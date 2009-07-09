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

import org.villane.vecmath.{Vector2, Preamble}

// Based on Raimund Seidel's paper "A simple and fast incremental randomized
// algorithm for computing trapezoidal decompositions and for triangulating polygons"
class Triangulator(segments: Array[Segment]) {

  // TODO: Randomize segment list
  orderSegments
  
  // Initialize trapezoidal map and query structure
  val trapezoidalMap = new TrapezoidalMap
  val boundingBox = trapezoidalMap.boundingBox(segments)
  trapezoidalMap.add(boundingBox)
  val queryGraph = new QueryGraph(new Sink(boundingBox))
  
  var foo: ArrayList[Trapezoid] = null
  
  // Build the trapezoidal map and querey graph
  def process {
    for(s <- segments) {
      val trapezoids = queryGraph.followSegment(s)
      trapezoids.foreach(trapezoidalMap.remove)
      for(t <- trapezoids) {
        var tList: ArrayList[Trapezoid] = null
        val containsP = t.contains(s.p)
        val containsQ = t.contains(s.q)
        if(containsP && containsQ) {
          tList = trapezoidalMap.case1(t,s)
          foo = tList
          queryGraph.case1(t.sink, s, tList)
        } else if(containsP && !containsQ) {
          tList = trapezoidalMap.case2(t,s) 
          foo = tList
          queryGraph.case2(t.sink, s, tList)
        } else if(!containsP && !containsQ) {
          println("case3")
          tList = trapezoidalMap.case3(t, s)
          foo = tList
          queryGraph.case3(t.sink, s, tList)
        } else {
          tList = trapezoidalMap.case4(t, s)
          foo = tList
          queryGraph.case4(t.sink, s, tList)
        }
        tList.foreach(trapezoidalMap.add)
      }
    }
  }
  
  def trapezoids = {
    val traps = new ArrayList[Trapezoid]
    for(t <- trapezoidalMap.map.values) {
	  //if(t.top != boundingBox.top && t.bottom != boundingBox.bottom) {
	    traps += t
	  //}
    }
    traps
  }
  private def orderSegments {
    for(s <- segments) {
      // Point p must be to the left of point q
      if(s.p.x > s.q.x) {
        val tmp = s.p
        s.p = s.q
        s.q = tmp
      }
    }
      
  }
  
  
  
}
