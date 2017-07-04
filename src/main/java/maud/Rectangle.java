/*
 Copyright (c) 2017, Stephen Gold
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 * Neither the name of the copyright holder nor the names of its contributors
 may be used to endorse or promote products derived from this software without
 specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package maud;

import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;

/**
 * A axis-aligned rectangular mesh in the XY plane, extending in position from
 * (x1,y1,0) to (x2,y2,0) and in texture from (s1,t1) to (s2, t2) with normals
 * set to (0,0,zNorm).
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class Rectangle extends Mesh {
    // *************************************************************************
    // constructors

    /**
     * No-argument constructor for serialization purposes only. Do not use!
     */
    public Rectangle() {
    }

    /**
     * Create a rectangle with the specified parameters.
     *
     * @param s1 1st texture coordinate of the 1st and 2nd vertices
     * @param s2 1st texture coordinate of the 3rd and 4th vertices
     * @param t1 2nd texture coordinate of the 1st and 4th vertices
     * @param t2 2nd texture coordinate of the 2nd and 3rd vertices
     * @param x1 X coordinate of the 1st and 2nd vertices
     * @param x2 X coordinate of the 3rd and 4th vertices
     * @param y1 Y coordinate of the 1st and 4th vertices
     * @param y2 Y coordinate of the 2nd and 3rd vertices
     * @param zNorm sign of the Z component of the normal vector (must be +1 or
     * -1)
     */
    public Rectangle(float s1, float s2, float t1, float t2, float x1, float x2,
            float y1, float y2, float zNorm) {
        assert zNorm == -1f || zNorm == 1f : zNorm;

        setBuffer(Type.Position, 3, new float[]{
            x1, y1, 0f,
            x1, y2, 0f,
            x2, y2, 0f,
            x2, y1, 0f});

        setBuffer(Type.TexCoord, 2, new float[]{
            s1, t1,
            s1, t2,
            s2, t2,
            s2, t1});

        setBuffer(Type.Normal, 3, new float[]{
            0f, 0f, zNorm,
            0f, 0f, zNorm,
            0f, 0f, zNorm,
            0f, 0f, zNorm});

        if ((x2 - x1) * (y2 - y1) * zNorm > 0f) {
            setBuffer(Type.Index, 3, new short[]{
                0, 2, 1,
                0, 3, 2});
        } else {
            setBuffer(Type.Index, 3, new short[]{
                0, 1, 2,
                0, 2, 3});
        }

        updateBound();
        setStatic();
    }
}
