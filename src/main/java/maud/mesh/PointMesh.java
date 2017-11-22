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

import com.jme3.math.Vector3f;
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
 * A point-mode mesh that visualizes a single point.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class PointMesh extends Mesh {
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
            = Logger.getLogger(PointMesh.class.getName());
    // *************************************************************************
    // fields

    /**
     * buffer to hold the mesh location of the point
     */
    final private FloatBuffer fPositions;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a mesh with the point at (0,0,0).
     */
    public PointMesh() {
        fPositions = BufferUtils.createFloatBuffer(numAxes);
        fPositions.clear();
        fPositions.put(0f);
        fPositions.put(0f);
        fPositions.put(0f);
        fPositions.flip(); // prepare for reading

        VertexBuffer vPositions = new VertexBuffer(Type.Position);
        vPositions.setupData(Usage.Stream, numAxes, Format.Float, fPositions);
        setBuffer(vPositions);

        setMode(Mode.Points);
        /*
         * Update the bounding volume.
         */
        updateBound();
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Alter the location of the point.
     *
     * @param newLocation coordinates in mesh space (not null, unaffected)
     */
    public void setLocation(Vector3f newLocation) {
        Validate.nonNull(newLocation, "new location");

        fPositions.clear();
        fPositions.put(newLocation.x);
        fPositions.put(newLocation.y);
        fPositions.put(newLocation.z);
        fPositions.flip(); // prepare for reading

        VertexBuffer positions = getBuffer(Type.Position);
        positions.updateData(fPositions);
        /*
         * Update the bounding volume.
         */
        updateBound();
    }
}
