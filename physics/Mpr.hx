/*
 * Copyright (c) 2009, Mason Green 
 * http://github.com/zzzzrrr/haxmel
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
package mpr2d;

import Vector2D;
import System;

const SIMPLEX_EPSILON = 0.01f;

Vector insidePortal(Vector v1, Vector v2)
{
    // Perp-dot product
    float dir = v1.x * v2.y - v1.y * v2.x;

    if (dir > EPSILON) return Vector(v1.x-v2.x, v1.y-v2.y).rotateLeft90;
    else return Vector(v1.x-v2.x, v1.y-v2.y).rotateRight90;
}

Vector outsidePortal(Vector v1, Vector v2)
{
    // Perp-dot product
    float dir = v1.x * v2.y - v1.y * v2.x;

    if (dir < EPSILON) return Vector(v1.x-v2.x, v1.y-v2.y).rotateLeft90;
    else return Vector(v1.x-v2.x, v1.y-v2.y).rotateRight90;
}

bool originInTriangle(Vector a, Vector b, Vector c)
{
    Vector ab = b - a;
	Vector bc = c - b;
	Vector ca = a - c;

    float pab = (-a).cross(ab);
    float pbc = (-b).cross(bc);
	bool sameSign = (((pab > 0) - (pab < 0)) == ((pbc > 0) - (pbc < 0)));
    if (!sameSign) return false;

	float pca = (-c).cross(ca);
	sameSign = (((pab > 0) - (pab < 0)) == ((pca > 0) - (pca < 0)));
    if (!sameSign) return false;

    return true;
}

bool intersectPortal(Vector v0, Vector v1, Vector v2)
{

    Vector a = Vector(0,0);
    Vector b = v0;
    Vector c = v1;
    Vector d = v2;

    float a1 = (a.x - d.x) * (b.y - d.y) - (a.y - d.y) * (b.x - d.x);
    float a2 = (a.x - c.x) * (b.y - c.y) - (a.y - c.y) * (b.x - c.x);

    if (a1 != 0.0f && a2 != 0.0f && a1*a2 < 0.0f)
    {
        float a3 = (c.x - a.x) * (d.y - a.y) - (c.y - a.y) * (d.x - a.x);
        float a4 = a3 + a2 - a1;
        if (a3 != 0.0f && a4 != 0.0f && a3*a4 < 0.0f) return true;
    }

    // Segments not intersecting (or collinear)
    return false;
}

bool collideAndFindPoint(RigidBody shape1, RigidBody shape2, inout Vector returnNormal, inout Vector point1, inout Vector point2, inout Vector[] sAB, inout Vector[] sA, inout Vector[] sB)
{

    // Phase one: Portal discovery

    // v0 = center of Minkowski sum
    Vector v01 = shape1.getCenter;
    Vector v02 = shape2.getCenter;
    Vector v0 = v02 - v01;

    // Avoid case where centers overlap -- any direction is fine in this case
    if (v0.isZero()) v0 = Vector(0.00001f, 0);

    // v1 = support in direction of origin
    Vector n = -v0;
    Vector v11 = shape1.support(-n);
    Vector v12 = shape2.support(n);
    Vector v1 = v12 - v11;

    sA ~= v11;
    sB ~= v12;

    // origin outside v1 support plane ==> miss
    if (v1 * n <= 0) return false;

    // Find a candidate portal
    n = outsidePortal(v1,v0);
    Vector v21 = shape1.support(-n);
    Vector v22 = shape2.support(n);
    Vector v2 = v22 - v21;

    if(sA[length-1] != v21) sA ~= v21;
    if(sB[length-1] != v22) sB ~= v22;

    // origin outside v2 support plane ==> miss
    if (v2 * n <= 0) return false;

    // Phase two: Portal refinement
    int maxIterations;
    while (1)
    {
        // Find normal direction

        if(!intersectPortal(v0,v2,v1))
        {
            n = insidePortal(v2,v1);
        }
        else
        {
            // Origin ray crosses the portal
            n = outsidePortal(v2,v1);
        }

        // Obtain the next support point
        Vector v31 = shape1.support(-n);
        Vector v32 = shape2.support(n);
        Vector v3 = v32 - v31;

        if(sA[length-1] != v21) sA ~= v31;
        if(sB[length-1] != v22) sB ~= v32;

        if (v3 * n <= 0)
        {
            Vector ab = v3-v2;
            float t = -(v2*ab)/(ab*ab);
            returnNormal = (v2 + (t * ab));
            return false;
        }

        // Portal lies on the outside edge of the Minkowski Hull.
        // Return contact information
        if((v3-v2)*n <= SIMPLEX_EPSILON || ++maxIterations > 10)
        {

            Vector ab = v2-v1;
            float t = -v1*ab;

            if (t <= 0.0f)
            {
                t   = 0.0f;
                returnNormal = v1;
            }
            else
            {
                float denom = ab*ab;
                if (t >= denom)
                {
                    returnNormal = v2;
                    t   = 1.0f;
                }
                else
                {
                    t  /= denom;
                    returnNormal = v1 + t * ab;
                }
            }

            float s = 1 - t;

            point1 = s * v11 + t * v21;
            point2 = s * v12 + t * v22;

            sAB ~= v0;
            sAB ~= v1;
            sAB ~= v2;
            return true;
        }

        // If origin is inside (v1,v0,v3), refine portal
        if (originInTriangle(v0,v1,v3))
        {
            v2 = v3;
            v21 = v31;
            v22 = v32;
            continue;
        }
        // If origin is inside (v3,v0,v2), refine portal
        else if (originInTriangle(v0,v2,v3))
        {
            v1=v3;
            v11 = v31;
            v12 = v32;
            continue;
        }

        return false;
    }
    // This should never happpen.....
    throw "mpr error";
}