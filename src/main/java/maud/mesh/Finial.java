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
package maud.mesh;

import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * A 2D, static, line-mode mesh in the XY plane, intended to be used as an end
 * cap for a staff. A finial can have up to 3 limbs (for translation, rotation,
 * and scale). A branch has either 3 or 4 crowsfeet, with each foot marking 1
 * end of a sparkline.
 * <p>
 * In local coordinates, a finial extends from -width to 0 in X and from -height
 * to 0 in Y.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class Finial extends Mesh {
    // *************************************************************************
    // constants and loggers

    /**
     * height per foot (in local units)
     */
    final public static double hpf = 0.1;
    /**
     * total width of a finial (in local units)
     */
    final public static double width = 0.05;
    /**
     * 1/3 of the total width (in local units)
     */
    final private static double otw = width / 3;
    /**
     * number of lines in a 3-foot limb
     */
    final private static int numLines3 = 10;
    /**
     * number of lines in a 4-foot limb
     */
    final private static int numLines4 = 14;
    /**
     * number of vertices in a 3-foot limb
     */
    final private static int numVertices3 = 15;
    /**
     * number of vertices in a 4-foot limb
     */
    final private static int numVertices4 = 22;
    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            Finial.class.getName());
    // *************************************************************************
    // fields

    /**
     * vertical space occupied by completed limbs (in local units, &ge;0)
     */
    private double height = 0.0;
    /**
     * half the vertical space each sparkline uses (in local units, &ge;0)
     */
    final private double shh;
    /**
     * buffer for position data, used during instantiation only
     */
    final private FloatBuffer floats;
    /**
     * number of vertices in the completed limbs
     */
    private int baseI = 0;
    /**
     * buffer for index data, used during instantiation only
     */
    final private ShortBuffer shorts;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a mesh with the specified components.
     *
     * @param tra if true, include a 3-foot limb for translations, else omit
     * @param rot if true, include a 4-foot limb for rotations, else omit
     * @param sca if true, include a 3-foot limb for scales, else omit
     * @param sparklineHeight vertical space used by a sparkline (in local
     * units, &ge;0)
     */
    public Finial(boolean tra, boolean rot, boolean sca,
            float sparklineHeight) {
        Validate.nonNegative(sparklineHeight, "sparkline height");

        shh = sparklineHeight / 2;

        int numVertices = 0;
        int numLines = 1;
        if (tra) {
            numVertices += numVertices3;
            numLines += numLines3;
        }
        if (rot) {
            numVertices += numVertices4;
            numLines += numLines4;
        }
        if (sca) {
            numVertices += numVertices3;
            numLines += numLines3;
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

        assert baseI == numVertices : numVertices;
        assert positions.getNumElements() == numVertices : numVertices;
        assert indices.getNumElements() == numLines : numLines;

        setMode(Mode.Lines);
        setStatic();
        updateBound();
    }

    /**
     * Instantiate a mesh with the specified components.
     *
     * @param tra if true, include a 3-foot limb for translations, else omit
     * @param rot if true, include a 4-foot limb for rotations, else omit
     * @param sca if true, include a 3-foot limb for scales, else omit
     */
    public Finial(boolean tra, boolean rot, boolean sca) {
        this(tra, rot, sca, (float) hpf);
    }
    // *************************************************************************
    // Object methods

    /**
     * Create a copy of this mesh.
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
     * @param tra if true, include a 3-foot limb for translations, else omit
     * @param rot if true, include a 4-foot limb for rotations, else omit
     * @param sca if true, include a 3-foot limb for scales, else omit
     */
    private void populate(boolean tra, boolean rot, boolean sca) {
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
         * connecting vertical bar, if needed
         */
        if (tra && sca) {
            shorts.put(sTra).put(sSca);
        } else if (tra && rot) {
            shorts.put(sTra).put(sRot);
        } else if (rot && sca) {
            shorts.put(sRot).put(sSca);
        } else {
            shorts.put(sTra).put(sTra); // no bar
        }

        floats.flip(); // prepare for reading
        shorts.flip(); // prepare for reading
    }

    /**
     * Add a 3-foot limb (for 3 sparklines) to the mesh.
     *
     * @return index of the connecting vertex
     */
    private short put3() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                putXY(0.0, i * hpf + j * shh);
            }
        }
        for (int i = 0; i < 3; i++) {
            putXY(otw, i * hpf + shh);
        }
        putXY(2 * otw, shh);
        putXY(2 * otw, 2 * hpf + shh);
        putXY(width, hpf + shh);
        /*
         * crowsfoot for 1st sparkline
         */
        putLine(0, 9);
        putLine(1, 12);
        putLine(2, 9);
        /*
         * crowsfoot for 2nd sparkline
         */
        putLine(3, 10);
        putLine(4, 14);
        putLine(5, 10);
        /*
         * crowsfoot for 3rd sparkline
         */
        putLine(6, 11);
        putLine(7, 13);
        putLine(8, 11);
        /*
         * connecting vertical bar
         */
        putLine(12, 13);

        int connectIndex = baseI + numLines4;
        height += 3 * hpf;
        baseI += numVertices3;

        return (short) connectIndex;
    }

    /**
     * Add a 4-foot limb (for 4 sparklines) to the mesh.
     *
     * @return index of the connecting vertex
     */
    private short put4() {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 3; j++) {
                putXY(0.0, i * hpf + j * shh);
            }
        }
        for (int i = 0; i < 4; i++) {
            putXY(otw, i * hpf + shh);
        }
        for (int i = 0; i < 4; i++) {
            putXY(2 * otw, i * hpf + shh);
        }
        putXY(2 * otw, 2 * hpf);
        putXY(width, 2 * hpf);
        /*
         * crowsfoot for 1st sparkline
         */
        putLine(0, 12);
        putLine(1, 16);
        putLine(2, 12);
        /*
         * crowsfoot for 2nd sparkline
         */
        putLine(3, 13);
        putLine(4, 17);
        putLine(5, 13);
        /*
         * crowsfoot for 3rd sparkline
         */
        putLine(6, 14);
        putLine(7, 18);
        putLine(8, 14);
        /*
         * crowsfoot for 4th sparkline
         */
        putLine(9, 15);
        putLine(10, 19);
        putLine(11, 15);
        /*
         * connecting T
         */
        putLine(16, 19);
        putLine(20, 21);

        int connectIndex = baseI + 21;
        height += 4 * hpf;
        baseI += numVertices4;

        return (short) connectIndex;
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
     * @param x negative x-offset of the position (&ge;0)
     * @param y negative y-offset of the position relative to baseY (&ge;0)
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
