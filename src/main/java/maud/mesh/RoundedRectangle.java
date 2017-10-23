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
package maud.mesh;

import com.jme3.math.FastMath;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * A 2D, static, fan-mode mesh which renders an axis-aligned rounded rectangle
 * in the XY plane.
 * <p>
 * In local space, X extends from x1 to x2 and Y extends from y1 to y2, with
 * normals set to (0,0,zNorm). In texture space, X and Y extend from 0 to 1.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class RoundedRectangle extends Mesh {
    // *************************************************************************
    // constants and loggers

    /**
     * number of edges per arc
     */
    final private static int epa = 4;
    /**
     * number of vertices per triangle
     */
    final private static int vpt = 3;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(RoundedRectangle.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an axis-aligned, rounded unit square with a corner radius of
     * 0.1, right-handed normals, and X and Y extending from 0 to 1.
     */
    public RoundedRectangle() {
        this(0f, 1f, 0f, 1f, 0.1f, 1f);
    }

    /**
     * Instantiate an axis-aligned rounded rectangle with the specified corner
     * radius.
     *
     * @param x1 local X coordinate of the left edge
     * @param x2 local X coordinate of the right edge
     * @param y1 local Y coordinate of the bottom edge
     * @param y2 local Y coordinate of the top edge
     * @param cornerRadius radius of the corner arcs (&ge;0)
     * @param zNorm the Z component of the normal vector (must be +1 or -1)
     */
    public RoundedRectangle(float x1, float x2, float y1, float y2,
            float cornerRadius, float zNorm) {
        Validate.nonNegative(cornerRadius, "corner radius");
        assert x2 - x1 > 2f * cornerRadius : cornerRadius;
        assert y2 - y1 > 2f * cornerRadius : cornerRadius;
        if (zNorm != -1f && zNorm != 1f) {
            logger.log(Level.SEVERE, "zNorm={0}", zNorm);
            throw new IllegalArgumentException("zNorm must be +1 or -1.");
        }

        setMode(Mode.TriangleFan);

        int numVertices = 4 * epa + 5;
        int numIndices = numVertices + 1;
        short[] indices = new short[numIndices];
        float[] normals = new float[3 * numVertices];
        float[] positions = new float[3 * numVertices];
        float[] texCoords = new float[2 * numVertices];

        positions[0] = (x1 + x2) / 2;
        positions[1] = (y1 + y2) / 2;

        float centerX, centerY;
        int vIndex = 1;

        centerX = x1 + cornerRadius;
        centerY = y1 + cornerRadius;
        for (int edge = 0; edge <= epa; edge++) {
            float theta = FastMath.HALF_PI * edge / epa;
            float sin = FastMath.sin(theta);
            float cos = FastMath.cos(theta);
            positions[3 * vIndex] = centerX - cornerRadius * cos;
            positions[3 * vIndex + 1] = centerY - cornerRadius * sin;
            vIndex++;
        }

        centerX = x2 - cornerRadius;
        for (int edge = 0; edge <= epa; edge++) {
            float theta = FastMath.HALF_PI * edge / epa;
            float sin = FastMath.sin(theta);
            float cos = FastMath.cos(theta);
            positions[3 * vIndex] = centerX + cornerRadius * sin;
            positions[3 * vIndex + 1] = centerY - cornerRadius * cos;
            vIndex++;
        }

        centerY = y2 - cornerRadius;
        for (int edge = 0; edge <= epa; edge++) {
            float theta = FastMath.HALF_PI * edge / epa;
            float sin = FastMath.sin(theta);
            float cos = FastMath.cos(theta);
            positions[3 * vIndex] = centerX + cornerRadius * cos;
            positions[3 * vIndex + 1] = centerY + cornerRadius * sin;
            vIndex++;
        }

        centerX = x1 + cornerRadius;
        for (int edge = 0; edge <= epa; edge++) {
            float theta = FastMath.HALF_PI * edge / epa;
            float sin = FastMath.sin(theta);
            float cos = FastMath.cos(theta);
            positions[3 * vIndex] = centerX - cornerRadius * sin;
            positions[3 * vIndex + 1] = centerY + cornerRadius * cos;
            vIndex++;
        }
        assert vIndex == numVertices : vIndex;

        for (int vi = 0; vi < numVertices; vi++) {
            indices[vi] = (short) vi;
            normals[3 * vi] = 0f;
            normals[3 * vi + 1] = 0f;
            normals[3 * vi + 2] = zNorm;
            positions[3 * vi + 2] = 0f;
            float x = positions[3 * vi];
            float y = positions[3 * vi + 1];
            texCoords[2 * vi] = (x - x1) / (x2 - x1);
            texCoords[2 * vi + 1] = (y - y1) / (y2 - y1);
        }
        indices[numVertices] = 1;

        setBuffer(Type.Normal, 3, normals);
        setBuffer(Type.Index, vpt, indices);
        setBuffer(Type.Position, 3, positions);
        setBuffer(Type.TexCoord, 2, texCoords);

        updateBound();
        setStatic();
    }
}
