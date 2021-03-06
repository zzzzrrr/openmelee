#
# Copyright (c) 2009 Mason Green & Tom Novelli
#
# This file is part of OpenMelee.
#
# OpenMelee is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# any later version.
#
# OpenMelee is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with OpenMelee.  If not, see <http://www.gnu.org/licenses/>.
#
from physics import *
from engine import Game, init_gl, reset_zoom, draw_polygon

from actors.asteroid import Asteroid
from actors.planet import Planet
from actors.ships.kzerZa import KzerZa
from actors.ships.nemesis import Nemesis
#from history import History
from players.net import NetConn, NetPlayer
from utils import clamp
import config
import struct
from players.kbd import update_ship
from callbacks import ContactListener
from ai import AI

NUM_ASTEROIDS = 7

class Melee(Game):

    WINDOW_POSITION = "800,400"

    # Keep this at 60 - no not tie to FPS
    hz = 60
    TIME_STEP = 1.0 / hz
    ts_ms = TIME_STEP * 1000.0

    #Screen size
    screen_size = 800.0, 600.0

    button_change = False   # Buttons pressed/released _locally_ since update?

    # Space upper and lower bounds
    aabb = AABB()
    aabb.lower_bound = Vec2(-200, -200)
    aabb.upper_bound = Vec2(200, 200)

    # Collision callback dictionary
    contact_register = {}
    
    ##
    ## INITIALIZATION
    ##

    def __init__(self, REMOTE): 
        super(Melee, self).__init__(*self.screen_size)
        
        self.window_title = "OpenMelee 0.01"
        init_gl(*self.screen_size)

        # Registe callbacks
        self.register_kbd_callback(self.on_key_press)
        
        # Init network
        if REMOTE:
            self.net = NetConn(LOCAL, REMOTE, self)
            if LOCAL[1] == 8888:
                self.local_player, self.remote_player = 0,1
            else:
                self.local_player, self.remote_player = 1,0
            config.PLAYERS[self.remote_player] = NetPlayer
        else:
            self.net = None
      
        # Initialize physics engine
        gravity = Vec2(0, 0)
        width = 700
        height = 700
        self.world = World(-width/2, -height/2, width, height, gravity)
        
        # Register collision callbacks
        self.contacthub = ContactListener(self)
        
        # Create players/controllers
        self.players = list(cls() for cls in config.PLAYERS)
        
        # Create game objects (they add themselves to self.actors if necessary)
        self.actors = []
        self.me = Nemesis(self)
        self.enemy = KzerZa(self)
        self.planet = Planet(self)
        
        for i in range(NUM_ASTEROIDS):
            Asteroid(self)
            
        # Init AI
        self.enemy.ai = AI(self.enemy, self.me, self.actors)
        
        # Save state for rollback
        self.run_time = self.time
        #self.history = History(self)
        
        # Network handshake  # TODO move to NetConn and randomize master/slave
        if self.net:
            if self.local_player == 0:
                # Master
                self.net.handshake()
            else:
                # Slave
                state = self.net.wait_handshake()
                if state:
                    self.deserialize(state)
                else:
                    print "Network handshake failed or timed out"
                    return
        
        # Calculate radius
        for a in self.actors:
            a.calc_radius()
        
        #from utils import squirtle
        #self.svg = squirtle.SVG("data/butterfly.svg")
        #self.svg.init((0,0), 1)
        
        self.main_loop()
        
    ##
    ## EVENTS
    ##
        
    def on_key_press(self, key, state):
        #print "on_key_press(id=%s state=%s)" % (id, state)
        update_ship(self, key, state)

    def update(self):
 
        # Update states
        for a in self.actors:
            if a.check_death(): continue
            a.limit_velocity()
            a.update_ai()
            a.update_state()
            a.apply_gravity()
            
        # Update game mechanics
        self.world.step(self.TIME_STEP, 5, 2)
        
        # End of frame maintenance 
        for a in self.actors:
            if not self.in_bounds(a.body):
                self.boundary_violated(a.body)

        self.run_time += self.ts_ms
        #self.history.update(self.time)
    
    def render(self):
        zoom, center = self.calculate_view()
        reset_zoom(zoom, center, self.screen_size)
        
        cyan = 0.3, 0.9, 0.9
        ub = self.aabb.upper_bound
        lb = self.aabb.lower_bound 
        verts = [(lb.x, ub.y), (lb.x, lb.y), 
                 (ub.x, lb.y), (ub.x, ub.y)]
        draw_polygon(verts, cyan)
        
        for a in self.actors:
            a.debug_draw()
            
        #self.svg.render(0, 10, angle = 0)
        
    ##
    ## NETWORK
    ##
        
    def serialize(self):  # TODO move to NetConn
        lst = []
        for ship in self.actors:
            s = ship.body
            lst += (list(s.position)
                   +list(s.linear_velocity)
                   +[s.angle, s.angular_velocity]
                   )
        n = len(self.actors)
        return struct.pack("!B %df" % (n*6), n, *lst)

    def deserialize(self, state):
        nlen = 1  # Encoded length of 'n'
        n, = struct.unpack("!B", state[:nlen])
        if (len(state) - nlen) != (struct.calcsize("!6f") * n):
            print "deserialize(): PACKET SIZE MISMATCH -- PACKET DROPPED"
            return
        lst = struct.unpack("!%df" % (n*6), state[nlen:])
        for i,ship in enumerate(self.actors):
            s = ship.body
            p = lst[i*6 : (i+1)*6]
            s.position = Vec2(p[0], p[1])
            s.velocity = Vec2(p[2], p[3])
            s.angle = p[4]
            s.angular_velocity = p[5]
        
        
    ##
    ## PRIVATE HELPER METHODS
    ##

    def calculate_view(self):
        from utils import clamp, bounding_box

        # Zoom in on the ships (and planet?)
        #objects = self.actors[:2] 
        
        if not self.me.dead and not self.enemy.dead:
            p1 = self.me.body.position
            p2 = self.enemy.body.position
        else:
            p1 = Vec2(125, 125)
            p2 = Vec2(-125, -125)
            
        range = Vec2(p1.x - p2.x, p1.y - p2.y)
        zoom = clamp(1000/range.length, 2, 75)   
        # Calculate view center
        vcx = p1.x - range.x * 0.5
        vcy = p1.y - range.y * 0.5

        return zoom, (vcx, vcy)

    def in_bounds(self, body):
        ub = self.aabb.upper_bound
        lb = self.aabb.lower_bound
        
        if body.position.x > ub.x or body.position.x < lb.x:
            return False
        if body.position.y > ub.y or body.position.y < lb.y:
            return False
        return True
        
    def boundary_violated(self, body):
        ub = self.aabb.upper_bound
        lb = self.aabb.lower_bound
        pos = body.position
        
        if pos.x > ub.x:
            x = lb.x + 5
            body.position = Vec2(x, pos.y)
        elif pos.x < lb.x:
            x = ub.x - 5
            body.position = Vec2(x, pos.y)
        elif pos.y > ub.y:
            y = lb.y + 5
            body.position = Vec2(pos.x, y)
        elif pos.y < lb.y:
            y = ub.y - 5
            body.position = Vec2(pos.x, y)
            
    def collision_callback(self, shape1, shape2):
        key1 = hash(shape1)
        key2 = hash(shape2)

        actor1 = self.contact_register[key1]
        actor2 = self.contact_register[key2]
        actor1.apply_damage(actor2.damage)
        actor2.apply_damage(actor1.damage)
  