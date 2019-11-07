/*
 Copyright (c) 2018-2019, Stephen Gold
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
package maud.mesh;

import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A 2-D, static, fan-mode mesh which renders an axis-aligned lozenge (diamond
 * or kite shape) in the X-Y plane, centered on the origin.
 * <p>
 * In local space, the lozenge extends from (-x,0,0) to (+x,0,0) and from
 * (0,-y,0) to (0,+y,0) with normals set to (0,0,zNorm). In texture space, it
 * extends extends from (0,0.5) to (1,0.5) and from (0.5,0) to (0.5,1).
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class Lozenge extends Mesh {
    // *************************************************************************
    // constants and loggers

    /**
     * number of vertices per triangle
     */
    final private static int vpt = 3;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(Lozenge.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an axis-aligned lozenge with the specified parameters.
     *
     * @param x local X coordinate of the first vertex
     * @param y local X coordinate of the 2nd vertex
     * @param zNorm the Z component of the normal vector (must be +1 or -1)
     */
    public Lozenge(float x, float y, float zNorm) {
        if (zNorm != -1f && zNorm != 1f) {
            logger.log(Level.SEVERE, "zNorm={0}", zNorm);
            throw new IllegalArgumentException("zNorm must be +1 or -1.");
        }

        setMode(Mode.TriangleFan);

        setBuffer(Type.Position, 3, new float[]{
            x, 0f, 0f,
            0f, y, 0f,
            -x, 0f, 0f,
            0f, -y, 0f});

        setBuffer(Type.TexCoord, 2, new float[]{
            1f, 0.5f,
            0.5f, 1f,
            0f, 0.5f,
            0.5f, 0f});

        setBuffer(Type.Normal, 3, new float[]{
            0f, 0f, zNorm,
            0f, 0f, zNorm,
            0f, 0f, zNorm,
            0f, 0f, zNorm});

        if (zNorm > 0f) {
            setBuffer(Type.Index, vpt, new short[]{0, 1, 2, 3});
        } else {
            setBuffer(Type.Index, vpt, new short[]{0, 3, 2, 1});
        }

        updateBound();
        setStatic();
    }
}
