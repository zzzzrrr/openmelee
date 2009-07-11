/* OpenMelee
 * Copyright (c) 2009, Mason Green
 * http://github.com/zzzzrrr/openmelee
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
 * * Neither the name of OpenMelee nor the names of its contributors may be
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
package org.openmelee.utils.geo

import collection.jcl.ArrayList
import scala.collection.mutable.Map

import org.villane.vecmath.Vector2

// See "Computational Geometry", 3rd edition, by Mark de Berg et al, Chapter 6.2

class TrapezoidalMap {

  // Trapezoid associated array
  val map: Map[Int, Trapezoid] = Map()
  // AABB margin
  var margin = 2f
    
  // Trapezoid that spans multiple parent trapezoids
  private var tCross: Trapezoid = null
  // Bottom segment that spans multiple trapezoids
  private var bCross: Segment = null
  
  // Add a trapezoid to the map
  def add(t: Trapezoid) {
    map + (t.hashCode -> t)
  }
  
  // Remove a trapezoid from the map
  def remove(t: Trapezoid) {
    map - t.hashCode
  }
  
  def reset {
    tCross = null
    bCross = null
  }

  // Case 1: segment completely enclosed by trapezoid
  //         break trapezoid into 4 smaller trapezoids
  def case1(t: Trapezoid, s: Segment) = {
    
    val trapezoids = new ArrayList[Trapezoid]
    trapezoids += new Trapezoid(t.leftPoint, s.p, t.top, t.bottom)
    trapezoids += new Trapezoid(s.p, s.q, t.top, s)
    trapezoids += new Trapezoid(s.p, s.q, s, t.bottom)
    trapezoids += new Trapezoid(s.q, t.rightPoint, t.top, t.bottom)
    
    trapezoids(0).update(t.upperLeft, t.lowerLeft, trapezoids(1), trapezoids(2))
    trapezoids(1).update(trapezoids(0), null, trapezoids(3), null)
    trapezoids(2).update(null, trapezoids(0), null, trapezoids(3))
    trapezoids(3).update(trapezoids(1), trapezoids(2), t.upperRight, t.lowerRight)
    
    t.updateNeighbors(trapezoids(0), trapezoids(0), trapezoids(3), trapezoids(3))
    trapezoids
  }

  // Case 2: Trapezoid contains point p, q lies outside
  //         break trapezoid into 3 smaller trapezoids
  def case2(t: Trapezoid, s: Segment) = {
    
    val trapezoids = new ArrayList[Trapezoid]
    trapezoids += new Trapezoid(t.leftPoint, s.p, t.top, t.bottom)
    trapezoids += new Trapezoid(s.p, t.rightPoint, t.top, s)
    trapezoids += new Trapezoid(s.p, t.rightPoint, s, t.bottom)
   
    s.above = trapezoids(1)
    
    trapezoids(0).update(t.upperLeft, t.lowerLeft, trapezoids(1), trapezoids(2))
    trapezoids(1).update(trapezoids(0), null, t.upperRight, null)
    trapezoids(2).update(null, trapezoids(0), null, t.lowerRight)
    
    bCross = t.bottom
    tCross = trapezoids(2)
    t.updateNeighbors(trapezoids(0), trapezoids(0), trapezoids(1), trapezoids(2))
    trapezoids
  }
  
  // Case 3: Trapezoid is bisected
  //         break trapezoid into 2 smaller trapezoids
  def case3(t: Trapezoid, s: Segment) = {
    
    val trapezoids = new ArrayList[Trapezoid]
    trapezoids += new Trapezoid(s.p, t.rightPoint, t.top, s)
    trapezoids += {if(bCross == t.bottom) tCross else new Trapezoid(s.p, t.rightPoint, s, t.bottom)}
    
    trapezoids(0).update(t.upperLeft, s.above, t.upperRight, t.lowerRight)
    
    if(s.above != null) s.above.lowerRight = trapezoids(0)
    s.above = trapezoids(0)
    
    if(bCross == t.bottom) {
      trapezoids(1).lowerRight = t.lowerRight
      trapezoids(1).rightPoint = t.rightPoint
    } else {
      trapezoids(1).update(null, t.lowerLeft, null, t.lowerRight)
      //t.lowerLeft.rightPoint = s.p
    }
    
    bCross = t.bottom
    tCross = trapezoids(1)
    
    t.updateNeighbors(trapezoids(0), trapezoids(1), trapezoids(0), trapezoids(1))
    trapezoids
  }
  
  // Case 4: Trapezoid contains point q, p lies outside
  //         break trapezoid into 3 smaller trapezoids
  def case4(t: Trapezoid, s: Segment)= {
    
    val trapezoids = new ArrayList[Trapezoid]
    trapezoids += new Trapezoid(t.leftPoint, s.q, t.top, s)
    trapezoids += {if(bCross == t.bottom) tCross else new Trapezoid(t.leftPoint, s.q, s, t.bottom)}
    trapezoids += new Trapezoid(s.q, t.rightPoint, t.top, t.bottom)
    
    trapezoids(0).update(t.upperLeft, s.above, trapezoids(2), null)
    
    if(s.above != null) s.above.lowerRight = trapezoids(0)
    
    if(bCross == t.bottom) {
      trapezoids(1).lowerRight = trapezoids(2)
      trapezoids(1).rightPoint = s.q
    } else {
      trapezoids(1).update(null, t.lowerLeft, null, trapezoids(2))
    }
    trapezoids(2).update(trapezoids(0), trapezoids(1), t.upperRight, t.lowerRight)
    
    t.updateNeighbors(trapezoids(0), trapezoids(1), trapezoids(2), trapezoids(2))
    trapezoids
  }
  
  // Create an AABB around segments
  def boundingBox(segments: Array[Segment]): Trapezoid = {
   
    var max = segments(0).p
    var min = segments(0).q

    for(s <- segments) {
      if(s.p.x > max.x) max = Vector2(s.p.x+margin, max.y)
      if(s.p.y > max.y) max = Vector2(max.x, s.p.y+margin)
      if(s.q.x > max.x) max = Vector2(s.q.x+margin, max.y)
      if(s.q.y > max.y) max = Vector2(max.x, s.q.y+margin)
      if(s.p.x < min.x) min = Vector2(s.p.x-margin, min.y)
      if(s.p.y < min.y) min = Vector2(min.x, s.p.y-margin)
      if(s.q.x < min.x) min = Vector2(s.q.x-margin, min.y)
      if(s.q.y < min.y) min = Vector2(min.x, s.q.y-margin)
    }

    val top = new Segment(Vector2(min.x, max.y), Vector2(max.x, max.y))
    val bottom = new Segment(Vector2(min.x, min.y), Vector2(max.x, min.y))
    val left = bottom.p
    val right = top.q
    
    return new Trapezoid(left, right, top, bottom)
  }
}
