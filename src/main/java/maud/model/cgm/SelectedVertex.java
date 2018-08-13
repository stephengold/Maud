/*
 Copyright (c) 2017-2018, Stephen Gold
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
package maud.model.cgm;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
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
    final private static int maxWeights = 4;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SelectedVertex.class.getName());
    // *************************************************************************
    // fields

    /**
     * C-G model containing the vertex (set by {@link #setCgm(Cgm)})
     */
    private Cgm cgm = null;
    /**
     * index of the selected vertex in the mesh of the selected spatial, or -1
     * for none selected
     */
    private int selectedIndex = -1;
    // *************************************************************************
    // new methods exposed

    /**
     * Copy the bone indices of the vertex.
     *
     * @param storeResult (modified if not null)
     * @return array of indices (either storeResult or a new instance)
     */
    public int[] boneIndices(int[] storeResult) {
        assert selectedIndex >= 0 : selectedIndex;

        Mesh mesh = cgm.getSpatial().getMesh();
        storeResult = MyMesh.vertexBoneIndices(mesh, selectedIndex,
                storeResult);

        return storeResult;
    }

    /**
     * Copy the bone weights of the vertex.
     *
     * @param storeResult (modified if not null)
     * @return array of weights (either storeResult or a new instance)
     */
    public float[] boneWeights(float[] storeResult) {
        assert selectedIndex >= 0 : selectedIndex;

        Mesh mesh = cgm.getSpatial().getMesh();
        storeResult = MyMesh.vertexBoneWeights(mesh, selectedIndex,
                storeResult);

        return storeResult;
    }

    /**
     * Copy the color of the selected vertex.
     *
     * @param storeResult (modified if not null)
     * @return the color (either storeResult or a new instance)
     */
    public ColorRGBA color(ColorRGBA storeResult) {
        assert selectedIndex >= 0 : selectedIndex;

        Mesh mesh = cgm.getSpatial().getMesh();
        storeResult = MyMesh.vertexColor(mesh, selectedIndex, storeResult);

        return storeResult;
    }

    /**
     * Copy Vector2f data from the specified buffer for the selected vertex.
     *
     * @param bufferType which vertex buffer to read (8 legal values)
     * @param storeResult (modified if not null)
     * @return the data vector (either storeResult or a new instance)
     */
    public Vector2f copyVector2f(VertexBuffer.Type bufferType,
            Vector2f storeResult) {
        assert selectedIndex >= 0 : selectedIndex;

        Mesh mesh = cgm.getSpatial().getMesh();
        storeResult = MyMesh.vertexVector2f(mesh, bufferType, selectedIndex,
                storeResult);

        return storeResult;
    }

    /**
     * Copy Vector3f data for the selected vertex from the specified buffer.
     *
     * @param bufferType which vertex buffer to read (5 legal values)
     * @param storeResult (modified if not null)
     * @return the data vector (either storeResult or a new instance)
     */
    public Vector3f copyVector3f(VertexBuffer.Type bufferType,
            Vector3f storeResult) {
        assert selectedIndex >= 0 : selectedIndex;

        Mesh mesh = cgm.getSpatial().getMesh();
        if (bufferType == VertexBuffer.Type.Position) {
            Pose pose = cgm.getPose().get();
            Matrix4f[] skinningMatrices = pose.skin(null);
            storeResult = MyMesh.vertexLocation(mesh, selectedIndex,
                    skinningMatrices, storeResult);
        } else if (bufferType == VertexBuffer.Type.Normal) {
            Pose pose = cgm.getPose().get();
            Matrix4f[] skinningMatrices = pose.skin(null);
            storeResult = MyMesh.vertexNormal(mesh, selectedIndex,
                    skinningMatrices, storeResult);
        } else {
            storeResult = MyMesh.vertexVector3f(mesh, bufferType, selectedIndex,
                    storeResult);
        }

        return storeResult;
    }

    /**
     * Copy Vector4f data for the selected vertex from the specified buffer.
     *
     * @param bufferType which vertex buffer to read (5 legal values)
     * @param storeResult (modified if not null)
     * @return the data vector (either storeResult or a new instance)
     */
    public Vector4f copyVector4f(VertexBuffer.Type bufferType,
            Vector4f storeResult) {
        assert selectedIndex >= 0 : selectedIndex;

        Mesh mesh = cgm.getSpatial().getMesh();
        if (bufferType == VertexBuffer.Type.Tangent) {
            Pose pose = cgm.getPose().get();
            Matrix4f[] skinningMatrices = pose.skin(null);
            storeResult = MyMesh.vertexTangent(mesh, selectedIndex,
                    skinningMatrices, storeResult);
        } else {
            storeResult = MyMesh.vertexVector4f(mesh, bufferType, selectedIndex,
                    storeResult);
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
     * @return count (&ge;0, &le;maxWeights)
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
        assert result <= maxWeights : result;
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
     * @param newCgm (not null, alias created)
     */
    void setCgm(Cgm newCgm) {
        assert newCgm != null;
        assert newCgm.getVertex() == this;

        cgm = newCgm;
    }

    /**
     * Read the size of the selected vertex.
     *
     * @return the size (in pixels)
     */
    public float vertexSize() {
        assert selectedIndex >= 0 : selectedIndex;

        Mesh mesh = cgm.getSpatial().getMesh();
        float result = MyMesh.vertexSize(mesh, selectedIndex);

        return result;
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
     * Access the weight data for the selected vertex.
     *
     * @return a read-only buffer instance
     */
    private FloatBuffer weightBuffer() {
        assert selectedIndex >= 0 : selectedIndex;

        Mesh mesh = cgm.getSpatial().getMesh();
        VertexBuffer wBuf = mesh.getBuffer(VertexBuffer.Type.BoneWeight);
        FloatBuffer weightBuffer = (FloatBuffer) wBuf.getDataReadOnly();
        weightBuffer.position(maxWeights * selectedIndex);

        return weightBuffer;
    }
}
