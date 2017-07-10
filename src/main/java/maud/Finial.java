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
 * Stephen Gold's name may not be used to endorse or promote products
 derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL STEPHEN GOLD BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package maud;

import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.logging.Logger;

/**
 * A 2D, line-mode mesh used as an end cap for a staff. In local coordinates it
 * extends from -width to 0 in X and from -height to 0 in Y.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class Finial extends Mesh {
    // *************************************************************************
    // constants and loggers

    /**
     * width (in local units, &ge;0)
     */
    final private static double width = 0.05;
    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            Finial.class.getName());
    // *************************************************************************
    // fields

    /**
     * how much vertical space the mesh occupies (in local units, &ge;0)
     */
    private double height = 0.0;
    /**
     * buffer for positions, used during instantiation only
     */
    final private FloatBuffer floats;
    /**
     * how many vertices have been generated, used during instantiation only
     */
    private int baseI = 0;
    /**
     * buffer for indices, used during instantiation only
     */
    final private ShortBuffer shorts;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a mesh with the specified components.
     *
     * @param tra if true, include 3 crowsfeet for translation offsets, else
     * omit
     * @param rot if true, include 4 crowsfeet for rotation elements, else omit
     * @param sca if true, include 3 crowsfeet for scale factors, else omit
     */
    public Finial(boolean tra, boolean rot, boolean sca) {
        int numVertices = 0;
        int numLines = 1;
        if (tra) {
            numVertices += 13;
            numLines += 14;
        }
        if (rot) {
            numVertices += 19;
            numLines += 15;
        }
        if (sca) {
            numVertices += 13;
            numLines += 14;
        }

        floats = BufferUtils.createFloatBuffer(3 * numVertices);
        VertexBuffer positions = new VertexBuffer(Type.Position);
        positions.setupData(Usage.Static, 3, Format.Float, floats);
        setBuffer(positions);

        shorts = BufferUtils.createShortBuffer(2 * numLines);
        VertexBuffer indices = new VertexBuffer(Type.Index);
        indices.setupData(Usage.Static, 2, Format.UnsignedShort, shorts);
        setBuffer(indices);

        populate(tra, rot, sca);

        setMode(Mode.Lines);
        setStatic();
        updateBound();
    }
    // *************************************************************************
    // Object methods

    /**
     * Create a shallow copy of this mesh.
     *
     * @return a new mesh, equivalent to this one
     */
    @Override
    public Finial clone() {
        Finial clone = (Finial) super.clone();
        return clone;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Read the height of the finial.
     *
     * @return height in local units (&ge;0)
     */
    public float getHeight() {
        assert height >= 0.0 : height;
        return (float) height;
    }
    // *************************************************************************
    // private methods

    /**
     * Populate the buffers.
     *
     * @param tra if true, include 3 crowsfeet for translations
     * @param rot if true, include 4 crowsfeet for rotations
     * @param sca if true, include 3 crowsfeet for scales
     */
    private void populate(boolean tra, boolean rot, boolean sca) {
        floats.clear(); // prepare for writing
        shorts.clear(); // prepare for writing

        short sTra = 0, sRot = 0, sSca = 0;
        if (tra) {
            sTra = put3();
        }
        if (rot) {
            sRot = put4();
        }
        if (sca) {
            sSca = put3();
        }
        /*
         * connecting vertical bar
         */
        if (tra && sca) {
            shorts.put(sTra).put(sSca);
        } else if (tra && rot) {
            shorts.put(sTra).put(sRot);
        } else {
            shorts.put(sRot).put(sSca);
        }

        floats.flip(); // prepare for reading
        shorts.flip(); // prepare for reading
    }

    /**
     * Add a 3-curve cap to the mesh.
     *
     * @return index of the connecting vertex
     */
    private short put3() {
        for (int i = 0; i < 7; i++) {
            putXY(0.0, 0.05 * i);
        }
        for (int i = 0; i < 3; i++) {
            putXY(width / 3.0, 0.05 + 0.1 * i);
        }
        putXY(width * 2.0 / 3.0, 0.05);
        putXY(width * 2.0 / 3.0, 0.25);
        putXY(width, 0.15);
        /*
         * crowsfoot for 1st curve
         */
        putLine(0, 7);
        putLine(1, 10);
        putLine(2, 7);
        /*
         * crowsfoot for 2nd curve
         */
        putLine(2, 8);
        putLine(3, 12);
        putLine(4, 8);
        /*
         * crowsfoot for 3rd curve
         */
        putLine(4, 9);
        putLine(5, 11);
        putLine(6, 9);
        /*
         * connecting vertical bar
         */
        putLine(10, 11);

        int result = baseI + 12;
        height += 0.3;
        baseI += 13;

        return (short) result;
    }

    /**
     * Add a 4-curve cap to the mesh.
     */
    private short put4() {
        for (int i = 0; i < 9; i++) {
            putXY(0, 0.05 * i);
        }
        for (int i = 0; i < 4; i++) {
            putXY(width / 3.0, 0.05 + 0.1 * i);
            putXY(width * 2.0 / 3.0, 0.05 + 0.1 * i);
        }
        putXY(width * 2.0 / 3.0, 0.2);
        putXY(width, 0.2);
        /*
         * crowsfoot for 1st curve
         */
        putLine(0, 9);
        putLine(1, 10);
        putLine(2, 9);
        /*
         * crowsfoot for 2nd curve
         */
        putLine(2, 11);
        putLine(3, 12);
        putLine(4, 11);
        /*
         * crowsfoot for 3rd curve
         */
        putLine(4, 13);
        putLine(5, 14);
        putLine(6, 13);
        /*
         * crowsfoot for 4th curve
         */
        putLine(6, 15);
        putLine(7, 16);
        putLine(8, 15);
        /*
         * connecting T
         */
        putLine(10, 16);
        putLine(17, 18);

        int result = baseI + 18;
        height += 0.4;
        baseI += 19;

        return (short) result;
    }

    /**
     * Add a line to the index buffer.
     *
     * @param index1 index of 1st endpoint relative to baseI (&ge;0)
     * @param index2 index of 2nd endpoint relative to baseI (&ge;0)
     */
    private void putLine(int index1, int index2) {
        assert index1 >= 0 : index1;
        assert index2 >= 0 : index1;

        short s1 = (short) (baseI + index1);
        shorts.put(s1);
        short s2 = (short) (baseI + index2);
        shorts.put(s2);
    }

    /**
     * Add a vertex to the position buffer.
     *
     * @param x x-offset of the position (&ge;0)
     * @param y y-offset of the position relative to baseY (&ge;0)
     */
    private void putXY(double x, double y) {
        assert x >= 0.0 : x;
        assert y >= 0.0 : y;

        float fx = (float) -x;
        floats.put(fx);
        float fy = (float) -(height + y);
        floats.put(fy);
        floats.put(0f);
    }
}
