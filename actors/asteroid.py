'''
Copyright 2009 Mason Green & Tom Novelli

This file is part of OpenMelee.

OpenMelee is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
any later version.

OpenMelee is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with OpenMelee.  If not, see <http://www.gnu.org/licenses/>.
'''
from random import randrange

import pymunk as pm
from pymunk import Vec2d

from actor import Actor
#from render import Color, draw_solid_circle
import pygame
from utils import transform

class Asteroid(Actor):

    ##
    ## INITIALIZATION
    ##

    def __init__(self, melee):
        Actor.__init__(self, melee)
        
        self.radius = 150
        self.offset1 = -self.radius, self.radius
        self.offset2 = self.radius, self.radius
        
        # Calculate mass and inertia
        mass = 0.5
        i1 = pm.moment_for_circle(mass, 0, self.radius, self.offset1)
        i2 = pm.moment_for_circle(mass, 0, self.radius, self.offset2)
        
        # Create body
        self.body = pm.Body(mass, i1+i2) 
        
        ub = melee.upperBound
        lb = melee.lowerBound
        x = randrange(lb.x, ub.x)
        y = randrange(lb.y, ub.y)
        self.body.position = x, y
        self.body.angular_velocity = 1.5
        
        # Create shapes
        self.circle1 = pm.Circle(self.body, self.radius, self.offset1)
        self.circle2 = pm.Circle(self.body, self.radius, self.offset2)
        
        # Add body and shapes to space
        melee.space.add(self.body, self.circle1, self.circle2)

        # Randomize velocity
        x = randrange(-15000.0, 15000.0)
        y = randrange(-15000.0, 15000.0)
        self.body.velocity = Vec2d(x, y)
    
    #def applyGravity(self):
    #    pass
        
    def draw(self, surface, view):
        center1 = self.body.position + self.circle1.center.cpvrotate(self.body.rotation_vector)
        center2 = self.body.position + self.circle2.center.cpvrotate(self.body.rotation_vector)
        if self.melee.backend == 'sdl':
            fill = 255,0,0
            r = transform.scale(self.radius)
            pygame.draw.circle(surface, fill, transform.to_sdl(center1), r)
            pygame.draw.circle(surface, fill, transform.to_sdl(center2), r)
        elif self.melee.backend == 'gl':
            fill = Color(1, 0, 0)
            draw_solid_circle(center1, self.radius, fill, fill) 
            draw_solid_circle(center2, self.radius, fill, fill)