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

import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * A 2D, line-mode mesh used to plot a function defined by samples. In local
 * coordinates, the mesh extends from 0 to 1 in X and from 0 to height in Y.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class Sparkline extends Mesh {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            Sparkline.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate a sparkline for the specified data.
     *
     * @param numVertices number of values to use (&ge;0)
     * @param xs X values, in ascending order, normalized to the range [0, 1]
     * (not null, unaffected, length&ge;numVertices)
     * @param ys Y values, normalized to the range [0, 1] (not null, unaffected,
     * length&ge;numVertices)
     * @param height desired height of the mesh (in local units, &ge;0)
     * @param mode mode for the mesh (Mode.Lines, Mode.LineStrip, or
     * Mode.Points)
     */
    public Sparkline(int numVertices, float[] xs, float ys[], float height,
            Mode mode) {
        Validate.nonNegative(numVertices, "num vertices");
        Validate.nonNull(xs, "x values");
        Validate.nonNull(ys, "y values");
        Validate.nonNegative(height, "height");
        assert mode == Mode.Lines || mode == Mode.LineStrip
                || mode == Mode.Points : mode;
        assert xs.length >= numVertices : xs.length;
        assert ys.length >= numVertices : ys.length;

        float[] positions = new float[3 * numVertices];
        for (int i = 0; i < numVertices; i++) {
            positions[3 * i] = xs[i];
            positions[3 * i + 1] = ys[i] * height;
            positions[3 * i + 2] = 0f;
        }
        setBuffer(Type.Position, 3, positions);

        if (mode == Mode.Lines) {
            int numLines = numVertices - 1;
            short[] indices = new short[2 * numLines];
            for (int i = 0; i < numLines; i++) {
                indices[2 * i] = (short) i;
                indices[2 * i + 1] = (short) (i + 1);
            }
            setBuffer(Type.Index, 2, indices);

        } else if (mode == Mode.LineStrip) {
            short[] indices = new short[numVertices];
            for (int i = 0; i < numVertices; i++) {
                indices[i] = (short) i;
            }
            setBuffer(Type.Index, 2, indices);
        }

        setMode(mode);
        setStatic();
        updateBound();
    }
    // *************************************************************************
    // Mesh methods

    /**
     * Create a copy of this mesh.
     *
     * @return a new mesh, equivalent to this one
     */
    @Override
    public Sparkline clone() {
        Sparkline clone = (Sparkline) super.clone();
        return clone;
    }
}
