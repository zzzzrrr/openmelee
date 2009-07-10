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
import scala.collection.mutable.{Set, Map}

import org.villane.vecmath.{Vector2, Preamble}

// Based on Raimund Seidel's paper "A simple and fast incremental randomized
// algorithm for computing trapezoidal decompositions and for triangulating polygons"
class Triangulator(segments: Array[Segment]) {

  // Trapezoid decomposition list
  var trapezoids : ArrayList[Trapezoid] = null
  // Triangle decomposition list
  // var triangles: ArrayList[Triangle] = null
    
  // Build the trapezoidal map and query graph
  def process {
    for(s <- segments) {
      val traps = queryGraph.followSegment(s)
      // Remove trapezoids from trapezoidal Map
      traps.foreach(trapezoidalMap.remove)
      for(t <- traps) {
        var tList: ArrayList[Trapezoid] = null
        val containsP = t.contains(s.p)
        val containsQ = t.contains(s.q)
        if(containsP && containsQ) {
          // Case 1
          tList = trapezoidalMap.case1(t,s)
          queryGraph.case1(t.sink, s, tList)
        } else if(containsP && !containsQ) {
          // Case 2
          tList = trapezoidalMap.case2(t,s) 
          queryGraph.case2(t.sink, s, tList)
        } else if(!containsP && !containsQ) {
          // Case 3
          tList = trapezoidalMap.case3(t, s)
          queryGraph.case3(t.sink, s, tList)
        } else {
          // Case 4
          tList = trapezoidalMap.case4(t, s)
          queryGraph.case4(t.sink, s, tList)
        }
        // Add new trapezoids to the trapezoidal map
        tList.foreach(trapezoidalMap.add)
      }
      trapezoidalMap reset
    }
    trapezoids = trim
    monotonePolygons
    xMonoPoly.foreach(println)
  }
  
  def allTrapezoids = trapezoidalMap.map.values
  
  // Initialize trapezoidal map and query structure
  private val trapezoidalMap = new TrapezoidalMap
  private val boundingBox = trapezoidalMap.boundingBox(segments)
  private val queryGraph = new QueryGraph(new Sink(boundingBox))
  
  val xMonoPoly = new ArrayList[Set[Vector2]]
                                        
  orderSegments
  
  // Build a list of x-monotone polygons 
  private def monotonePolygons {
    
    val map: Map[Int, Set[Vector2]] = Map()
    
    for(s <- segments) {
       map + (s.hashCode -> Set.empty[Vector2])
    }
    
   for(t <- trapezoids) {
     val top = map(t.top.hashCode)
     top += t.rightPoint; top += t.leftPoint                    
     val bottom = map(t.bottom.hashCode)     
     bottom += t.rightPoint; bottom += t.leftPoint
   }
   
   for(m <- map.keys) {
     if(map(m).size > 2) xMonoPoly += map(m)
   }
   
  }
  
  // Partition the x-monotone polygons into triangles o(n)
  // See "Computational Geometry", 3rd edition, by Mark de Berg et al, page 57
  private def triangulateMonotonePolygon {
    
  }
  
  // Trim off the extraneous trapezoids surrounding the polygon
  private def trim = {
    val traps = new ArrayList[Trapezoid]
    // Mark outside trapezoids
    for(t <- trapezoidalMap.map.values) {
	  if(t.top == boundingBox.top || t.bottom == boundingBox.bottom) {
	    t.outside = true
	    t.markNeighbors
	  }
    }
    // Collect interior trapezoids
    for(t <- trapezoidalMap.map.values) if(!t.outside) traps += t
    traps
  }
 
  // TODO: Randomize segment list
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
