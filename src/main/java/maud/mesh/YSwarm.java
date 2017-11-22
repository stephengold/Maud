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

import com.jme3.math.ColorRGBA;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;
import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * A point-mode mesh that visualizes a swarm of colored points on the Y-axis,
 * such as a pose in a score view.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class YSwarm extends Mesh {
    // *************************************************************************
    // constants and loggers

    /**
     * number of axes in a vector
     */
    final private static int numAxes = 3;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(YSwarm.class.getName());
    // *************************************************************************
    // fields

    /**
     * buffer to hold the color of each point
     */
    final private FloatBuffer fColors;
    /**
     * buffer to hold the mesh location of each point
     */
    final private FloatBuffer fPositions;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a mesh with the specified maximum number of points.
     *
     * @param maxPoints maximum number of points to visualize (&gt;0)
     */
    public YSwarm(int maxPoints) {
        Validate.positive(maxPoints, "max points");

        fPositions = BufferUtils.createFloatBuffer(numAxes * maxPoints);
        VertexBuffer vPositions = new VertexBuffer(Type.Position);
        vPositions.setupData(Usage.Stream, numAxes, Format.Float, fPositions);
        setBuffer(vPositions);

        fColors = BufferUtils.createFloatBuffer(4 * maxPoints);
        VertexBuffer vColors = new VertexBuffer(Type.Color);
        vColors.setupData(Usage.Stream, 4, Format.Float, fColors);
        setBuffer(vColors);

        setMode(Mode.Points);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Add a point to the mesh.
     *
     * @param y Y-coordinate for the new point
     * @param color color for the new point (not null, unaffected)
     */
    public void add(float y, ColorRGBA color) {
        Validate.nonNull(color, "color");

        fPositions.put(0f);
        fPositions.put(y);
        fPositions.put(0f);

        fColors.put(color.r);
        fColors.put(color.g);
        fColors.put(color.b);
        fColors.put(color.a);
    }

    /**
     * Remove all points from the mesh and prepare to write new ones.
     */
    public void clear() {
        fPositions.clear();
        fColors.clear();
    }

    /**
     * Prepare the mesh for rendering.
     */
    public void flip() {
        fPositions.flip(); // prepare for reading
        VertexBuffer vPositions = getBuffer(Type.Position);
        vPositions.updateData(fPositions);

        fColors.flip(); // prepare for reading
        VertexBuffer vColors = getBuffer(Type.Color);
        vColors.updateData(fColors);
        /*
         * Update the bounding volume.
         */
        updateBound();
    }

    /**
     * Test whether the mesh contains points.
     *
     * @return true if it contains no points, else false
     */
    public boolean isEmpty() {
        int position = fColors.position();
        if (position == 0) {
            return true;
        } else {
            return false;
        }
    }
}
