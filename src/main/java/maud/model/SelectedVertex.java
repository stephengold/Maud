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
package maud.model;

import com.jme3.math.Matrix4f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.logging.Logger;
import jme3utilities.MyMesh;
import jme3utilities.wes.Pose;

/**
 * The MVC model of the selected vertex in a loaded C-G model.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SelectedVertex implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * maximum number of bones that can influence any one vertex
     */
    final private static int maxBones = 4;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SelectedVertex.class.getName());
    // *************************************************************************
    // fields

    /**
     * index of the selected vertex, or -1 for none selected
     */
    private int selectedIndex = -1;
    /**
     * C-G model containing the vertex (set by {@link #setCgm(Cgm)}) TODO sort
     * fields
     */
    private Cgm cgm = null;
    // *************************************************************************
    // new methods exposed

    /**
     * Calculate the bind location of the selected vertex.
     *
     * @param storeResult (modified if not null)
     * @return location in model space (either storeResult or a new instance)
     */
    public Vector3f bindLocation(Vector3f storeResult) {
        if (storeResult == null) {
            storeResult = new Vector3f();
        }
        assert selectedIndex >= 0 : selectedIndex;

        Mesh mesh = cgm.getSpatial().getMesh();
        storeResult = MyMesh.vertexVector3f(mesh,
                VertexBuffer.Type.BindPosePosition, selectedIndex, storeResult);

        return storeResult;
    }

    /**
     * Read the bone indices of the vertex.
     *
     * @param storeResult (modified if not null)
     * @return array of indices (either storeResult or a new instance)
     */
    public int[] boneIndices(int[] storeResult) {
        if (storeResult == null) {
            storeResult = new int[maxBones];
        } else {
            assert storeResult.length >= maxBones : storeResult.length;
        }

        Buffer boneIndexBuffer = boneIndexBuffer();
        int maxNumWeights = cgm.getSpatial().getMaxNumWeights();
        for (int i = 0; i < maxNumWeights; i++) {
            int boneIndex = MyMesh.readIndex(boneIndexBuffer);
            storeResult[i] = boneIndex;
        }
        for (int i = maxNumWeights; i < maxBones; i++) {
            storeResult[i] = -1;
        }

        return storeResult;
    }

    /**
     *
     * @param storeResult
     * @return
     */
    public float[] boneWeights(float[] storeResult) {
        if (storeResult == null) {
            storeResult = new float[maxBones];
        } else {
            assert storeResult.length >= maxBones : storeResult.length;
        }

        FloatBuffer wBuffer = weightBuffer();
        int maxNumWeights = cgm.getSpatial().getMaxNumWeights();
        for (int i = 0; i < maxNumWeights; i++) {
            float weight = wBuffer.get();
            storeResult[i] = weight;
        }
        for (int i = maxNumWeights; i < maxBones; i++) {
            storeResult[i] = 0f;
        }

        return storeResult;
    }

    /**
     * Deselect the selected vertex, if any.
     */
    public void deselect() {
        selectedIndex = -1;
    }

    /**
     * Read the index of the selected vertex.
     *
     * @return the vertex index, or -1 if none selected
     */
    public int getIndex() {
        return selectedIndex;
    }

    /**
     * Count how many bones directly influence the selected vertex.
     *
     * @return count (&ge;0, &le;maxBones)
     */
    public int influence() {
        int result = 0;
        if (selectedIndex != -1) {
            FloatBuffer weightBuffer = weightBuffer();
            int maxNumWeights = cgm.getSpatial().getMaxNumWeights();
            for (int wIndex = 0; wIndex < maxNumWeights; wIndex++) {
                float weight = weightBuffer.get();
                if (weight != 0f) {
                    ++result;
                }
            }
        }

        assert result >= 0 : result;
        assert result <= maxBones : result;
        return result;
    }

    /**
     * Test whether a vertex is selected.
     *
     * @return true if selected, otherwise false
     */
    public boolean isSelected() {
        if (selectedIndex == -1) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Select a vertex by its index.
     *
     * @param newIndex which vertex to select, or -1 to deselect
     */
    public void select(int newIndex) {
        selectedIndex = newIndex;
    }

    /**
     * Select the next vertex (by index).
     */
    public void selectNext() {
        if (selectedIndex != -1) {
            ++selectedIndex;
            int numVertices = cgm.getSpatial().countVertices();
            if (selectedIndex >= numVertices) {
                selectedIndex = 0;
            }
        }
    }

    /**
     * Select the previous vertex (by index).
     */
    public void selectPrevious() {
        if (selectedIndex != -1) {
            --selectedIndex;
            if (selectedIndex < 0) {
                int numVertices = cgm.getSpatial().countVertices();
                selectedIndex = numVertices - 1;
            }
        }
    }

    /**
     * Alter which C-G model contains the vertex.
     *
     * @param newCgm (not null)
     */
    void setCgm(Cgm newCgm) {
        assert newCgm != null;
        assert newCgm.getVertex() == this;

        cgm = newCgm;
    }

    /**
     * Calculate the world location of the selected vertex.
     *
     * @param storeResult (modified if not null)
     * @return world coordinates (either storeResult or a new instance)
     */
    public Vector3f worldLocation(Vector3f storeResult) {
        Spatial selectedSpatial = cgm.getSceneView().selectedSpatial();
        Geometry selectedGeometry = (Geometry) selectedSpatial;
        Pose pose = cgm.getPose().get();
        Matrix4f[] matrices = pose.skin(null);
        storeResult = MyMesh.vertexWorldLocation(selectedGeometry,
                selectedIndex, matrices, storeResult);

        return storeResult;
    }
    // *************************************************************************
    // Object methods

    /**
     * Create a deep copy of this object.
     *
     * @return a new object, equivalent to this one
     * @throws CloneNotSupportedException if the superclass isn't cloneable
     */
    @Override
    public SelectedVertex clone() throws CloneNotSupportedException {
        SelectedVertex clone = (SelectedVertex) super.clone();
        return clone;
    }
    // *************************************************************************
    // private methods

    /**
     * Access the bone-index data for the selected vertex.
     *
     * @return a read-only buffer instance
     */
    private Buffer boneIndexBuffer() {
        assert selectedIndex >= 0 : selectedIndex;

        Mesh mesh = cgm.getSpatial().getMesh();
        VertexBuffer biBuf = mesh.getBuffer(VertexBuffer.Type.BoneIndex);
        Buffer boneIndexBuffer = biBuf.getDataReadOnly();
        boneIndexBuffer.position(maxBones * selectedIndex);

        return boneIndexBuffer;
    }

    /**
     * Access the weight data for the selected vertex.
     *
     * @return a read-only buffer instance
     */
    private FloatBuffer weightBuffer() {
        assert selectedIndex >= 0 : selectedIndex;

        Mesh mesh = cgm.getSpatial().getMesh();
        VertexBuffer wBuf = mesh.getBuffer(VertexBuffer.Type.BoneWeight);
        FloatBuffer weightBuffer = (FloatBuffer) wBuf.getDataReadOnly();
        weightBuffer.position(maxBones * selectedIndex);

        return weightBuffer;
    }
}
