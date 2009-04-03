/*
 * Copyright (c) 2009, Mason Green (zzzzrrr)
 * http://kenai.com/projects/haxmel
 * Based on code from Box2D, by Erin Catto: http://www.box2d.org
 * 
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * * Neither the name of the polygonal nor the names of its contributors may be
 *   used to endorse or promote products derived from this software without specific
 *   prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package render;

import opengl.GL;
import opengl.GLU;
import opengl.GLFW;

import phx.Vector;
import phx.World;
import phx.Body;
import phx.Shape;
import phx.Polygon;
import phx.Circle;

import melee.Melee;
import ships.Ship;
import utils.Util;

/// Color for drawing. Each value has the range [0,1].
typedef Color = {
    var r : Float;
    var g : Float;
    var b : Float;
}

class Render 
{

    static var MAX_CIRCLE_RES = 32;
	var zoom : Float;
	var viewCenter : Vector;
    var world : World;
    var screenSize : Vector;
    var settings : Settings;
    var ship1 : Ship;
    var ship2 : Ship;
    static public var running : Bool;
    
    public function new(melee : Melee, settings : Settings) {
        
        zoom = 40;
        running = true;
        this.settings = settings;
        world = melee.world;
        ship1 = melee.ship1;
        ship2 = melee.ship2;
        viewCenter = new Vector(10, 10);
        screenSize = new Vector(800, 600);
        
        // Open window
        var width : Int = cast(screenSize.x);
        var height : Int = cast(screenSize.y);
        var ok = GLFW.openWindow(Std.int(screenSize.x), Std.int(screenSize.y), 8,8,8, 8,8,0, GLFW.WINDOW );

        if(!ok) {
            throw "error loading window";
        }
        
        GLFW.setWindowTitle("OpenMelee");
        
        GLFW.setKeyFunction(function( a:Int, b:Int ) {
			trace("key: "+a+", "+b );
		});
        
        GLFW.setWindowCloseFunction( function() {
			trace("window close" );
			Render.running = false;
			return 1;
		});
    }
    
    public function update() {
        // Limit the fps
        GLFW.swapInterval(1);
        draw();
        GLFW.swapBuffers();
    }

    static function drawCircle( circ : Circle ) {
        var c = new Vector(circ.tC.x, circ.tC.y);
        var r = circ.r;
        var segs = 20;
        var coef = 2.0 * Math.PI / segs;
        var theta = circ.body.a;
        GL.begin(GL.LINE_STRIP); 
        {
            for (n in 0...segs+1) {
                var rads = n * coef;
                var x = r * Math.cos(rads + theta) + c.x;
                var y = r * Math.sin(rads + theta) + c.y;
                GL.vertex2(x, y);
            }
            GL.vertex2(c.x, c.y);
        } 
        GL.end();
        GL.loadIdentity();
        GL.flush();
	}

    function drawSolidCircle(center : Vector, radius : Float, axis : Vector, color : Color)
    {
        var k_segments = 25;
        var k_increment = 2.0 * Math.PI / k_segments;
        var theta = 0.0;
        GL.enable(GL.BLEND);
        GL.blendFunc(GL.SRC_ALPHA, GL.ONE_MINUS_SRC_ALPHA);
        GL.color4(0.5 * color.r, 0.5 * color.g, 0.5 * color.b, 0.5);
        GL.begin(GL.TRIANGLE_FAN);
        for (i in 0...k_segments) {
            var v = center.plus(new Vector(Math.cos(theta), Math.sin(theta)).mult(radius));
            GL.vertex2(v.x, v.y);
            theta += k_increment;
        }
        GL.end();
        GL.disable(GL.BLEND);

        theta = 0.0;
        GL.color4(color.r, color.g, color.b, 1.0);
        GL.begin(GL.LINE_LOOP);
        for (i in 0...k_segments) {
            var v = center.plus(new Vector(Math.cos(theta), Math.sin(theta)).mult(radius));
            GL.vertex2(v.x, v.y);
            theta += k_increment;
        }
        GL.end();

        var p = center.plus(axis.mult(radius));
        GL.begin(GL.LINES);
        GL.vertex2(center.x, center.y);
        GL.vertex2(p.x, p.y);
        GL.end();
    }

    function drawPolygon(glVerts : Array<Vector>, color : Color)
    {
        GL.color3(color.r, color.g, color.b);
        GL.begin(GL.LINE_LOOP);
        {
            for(v in glVerts) {
                GL.vertex2(v.x, v.y);
            }
        }
        GL.end();
    }

    function drawSolidPolygon(vertices : Array<Vector>, color : Color)
    {
        GL.enable(GL.BLEND);
        GL.blendFunc(GL.SRC_ALPHA, GL.ONE_MINUS_SRC_ALPHA);
        GL.color4(0.5 * color.r, 0.5 * color.g, 0.5 * color.b, 0.5);
        GL.begin(GL.TRIANGLE_FAN);
        {
            for(v in vertices) {
                GL.vertex2(v.x, v.y);
            }
        }
        GL.end();
        GL.disable(GL.BLEND);

        GL.color4(color.r, color.g, color.b, 1.0);
        GL.begin(GL.LINE_LOOP);
        {
            for(v in vertices) {
                GL.vertex2(v.x, v.y);
            }
        }
        GL.end();
    }
    
    function drawShape(s : Shape, color : Color) {
        
    }
   
    function draw() {
        
       if(ship2 != null) {
            var point1 = new Vector(ship1.rBody.x,ship1.rBody.y);
            var point2 = new Vector(ship2.rBody.x,ship2.rBody.y);
            var range = point1.minus(point2);
            zoom = Util.clamp(1000.0/range.length(), 2.0, 60.0);
            viewCenter = point1.minus(range.mult(0.5));
        } else {
             viewCenter = new Vector(ship1.rBody.x, ship1.rBody.y);
             zoom = 10;
        }
        
        GL.loadIdentity();
        GL.matrixMode(GL.PROJECTION);
        GL.loadIdentity();
        
        var left = -screenSize.x / zoom;
        var right = screenSize.x / zoom;
        var bottom = -screenSize.y / zoom;
        var top = screenSize.y / zoom;

        GLU.ortho2D(left, right, bottom, top);
        GL.translate(-viewCenter.x, -viewCenter.y, 0);
        GL.matrixMode(GL.MODELVIEW);
        GL.disable(GL.DEPTH_TEST);
        GL.loadIdentity();
        GL.clear(GL.COLOR_BUFFER_BIT);

        // Draw dynamic bodies
        for (b in world.bodies) {
            for (shape in b.shapes) {
                var s = shape;
                var color : Color;
                if (b.isStatic) {
                    color = {r: 0.5, g : 0.9, b : 0.5};
                    drawShape(s, color);
                } else {
                    color = {r : 0.9, g : 0.9, b : 0.9};
                    drawShape(s, color);
                }
                GL.loadIdentity();
                GL.flush();
            }
        }

        // Draw the world bounds
        /*
        var bp = world.broadPhase;
        var worldLower = bp.m_worldAABB.lowerBound;
        var worldUpper = bp.m_worldAABB.upperBound;
        var color : Color = {r : 0.3, g : 0.9, b : 0.9};        
        var vs = new Array();
        vs.push(new Vector(worldLower.x, worldLower.y));
        vs.push(new Vector(worldUpper.x, worldLower.y));
        vs.push(new Vector(worldUpper.x, worldUpper.y));
        vs.push(new Vector(worldLower.x, worldUpper.y));
        drawPolygon(vs, color);
        */
    }
}
