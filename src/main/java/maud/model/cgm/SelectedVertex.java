/*
 Copyright (c) 2017-2020, Stephen Gold
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
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyMesh;
import jme3utilities.Validate;
import jme3utilities.math.MyVector3f;
import jme3utilities.wes.Pose;
import maud.Population;
import maud.view.scene.SceneUpdater;

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
     * @return an array of bone indices (either storeResult or a new instance,
     * not null)
     */
    public int[] boneIndices(int[] storeResult) {
        assert selectedIndex >= 0 : selectedIndex;
        int[] result
                = (storeResult == null) ? new int[maxWeights] : storeResult;

        Mesh mesh = cgm.getSpatial().getMesh();
        MyMesh.vertexBoneIndices(mesh, selectedIndex, result);

        return result;
    }

    /**
     * Copy the bone weights of the vertex.
     *
     * @param storeResult (modified if not null)
     * @return an array of weights (either storeResult or a new instance, not
     * null)
     */
    public float[] boneWeights(float[] storeResult) {
        assert selectedIndex >= 0 : selectedIndex;
        float[] result
                = (storeResult == null) ? new float[maxWeights] : storeResult;

        Mesh mesh = cgm.getSpatial().getMesh();
        MyMesh.vertexBoneWeights(mesh, selectedIndex, result);

        return result;
    }

    /**
     * Copy the color of the selected vertex.
     *
     * @param storeResult (modified if not null)
     * @return a color (either storeResult or a new instance, not null)
     */
    public ColorRGBA color(ColorRGBA storeResult) {
        assert selectedIndex >= 0 : selectedIndex;
        ColorRGBA result
                = (storeResult == null) ? new ColorRGBA() : storeResult;

        Mesh mesh = cgm.getSpatial().getMesh();
        MyMesh.vertexColor(mesh, selectedIndex, result);

        return result;
    }

    /**
     * Copy Vector2f data from the specified buffer for the selected vertex.
     *
     * @param bufferType which vertex buffer to read (8 legal values)
     * @param storeResult (modified if not null)
     * @return a data vector (either storeResult or a new instance)
     */
    public Vector2f copyVector2f(VertexBuffer.Type bufferType,
            Vector2f storeResult) {
        assert selectedIndex >= 0 : selectedIndex;
        Vector2f result = (storeResult == null) ? new Vector2f() : storeResult;

        Mesh mesh = cgm.getSpatial().getMesh();
        MyMesh.vertexVector2f(mesh, bufferType, selectedIndex, result);

        return result;
    }

    /**
     * Copy Vector3f data for the selected vertex from the specified buffer.
     *
     * @param bufferType which vertex buffer to read (5 legal values)
     * @param storeResult (modified if not null)
     * @return a data vector (either storeResult or a new instance, not null)
     */
    public Vector3f copyVector3f(VertexBuffer.Type bufferType,
            Vector3f storeResult) {
        assert selectedIndex >= 0 : selectedIndex;
        Vector3f result = (storeResult == null) ? new Vector3f() : storeResult;

        Mesh mesh = cgm.getSpatial().getMesh();
        if (bufferType == VertexBuffer.Type.Position) {
            Pose pose = cgm.getPose().get();
            Matrix4f[] skinningMatrices = pose.skin(null);
            MyMesh.vertexLocation(mesh, selectedIndex, skinningMatrices,
                    result);
        } else if (bufferType == VertexBuffer.Type.Normal) {
            Pose pose = cgm.getPose().get();
            Matrix4f[] skinningMatrices = pose.skin(null);
            MyMesh.vertexNormal(mesh, selectedIndex, skinningMatrices, result);
        } else {
            MyMesh.vertexVector3f(mesh, bufferType, selectedIndex, result);
        }

        return result;
    }

    /**
     * Copy Vector4f data for the selected vertex from the specified buffer.
     *
     * @param bufferType which vertex buffer to read (5 legal values)
     * @param storeResult (modified if not null)
     * @return a data vector (either storeResult or a new instance)
     */
    public Vector4f copyVector4f(VertexBuffer.Type bufferType,
            Vector4f storeResult) {
        assert selectedIndex >= 0 : selectedIndex;
        Vector4f result = (storeResult == null) ? new Vector4f() : storeResult;

        Mesh mesh = cgm.getSpatial().getMesh();
        if (bufferType == VertexBuffer.Type.Tangent) {
            Pose pose = cgm.getPose().get();
            Matrix4f[] skinningMatrices = pose.skin(null);
            MyMesh.vertexTangent(mesh, selectedIndex, skinningMatrices, result);
        } else {
            MyMesh.vertexVector4f(mesh, bufferType, selectedIndex, result);
        }

        return result;
    }

    /**
     * Deselect the selected vertex, if any.
     */
    public void deselect() {
        selectedIndex = -1;
    }

    /**
     * Read the indexed component of the vertex data from the selected
     * FloatBuffer.
     *
     * @param componentIndex (0-3)
     * @return the component value
     */
    public float floatComponent(int componentIndex) {
        SelectedBuffer buffer = cgm.getBuffer();
        int numComponentsPerElement = buffer.countComponents();
        int lastComponent = numComponentsPerElement - 1;
        Validate.inRange(componentIndex, "component index", 0, lastComponent);
        VertexBuffer.Format format = buffer.format();
        assert format == VertexBuffer.Format.Float : format;

        int floatIndex
                = selectedIndex * numComponentsPerElement + componentIndex;
        VertexBuffer vertexBuffer = buffer.find();
        FloatBuffer floatBuffer = (FloatBuffer) vertexBuffer.getDataReadOnly();
        float result = floatBuffer.get(floatIndex);

        return result;
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
     * Enumerate the closest vertices (in terms of bind position) in the
     * selected Mesh.
     *
     * @param maxNumber the maximum number of vertices to return (&gt;0)
     * @return a new list of vertex indices
     */
    public List<Integer> listNeighbors(int maxNumber) {
        Validate.positive(maxNumber, "max number");

        Population<Float, Integer> population = new Population<>(maxNumber);
        Mesh mesh = cgm.getSpatial().getMesh();
        Vector3f position = MyMesh.vertexVector3f(mesh, Type.BindPosePosition,
                selectedIndex, null);

        Vector3f tmpPosition = new Vector3f();
        int numVertices = mesh.getVertexCount();

        for (int vertexIndex = 0; vertexIndex < numVertices; ++vertexIndex) {
            if (vertexIndex != selectedIndex) {
                MyMesh.vertexVector3f(mesh, Type.BindPosePosition, vertexIndex,
                        tmpPosition);
                float squaredDistance = position.distanceSquared(tmpPosition);
                population.add(vertexIndex, -squaredDistance);
            }
        }

        List<Integer> result = population.listElements();
        return result;
    }

    /**
     * Select a vertex by its index.
     *
     * @param newIndex which vertex to select, or -1 to deselect
     */
    public void select(int newIndex) {
        Validate.inRange(newIndex, "new index", -1, Integer.MAX_VALUE);
        selectedIndex = newIndex;
    }

    /**
     * Select an extreme vertex.
     *
     * @param axesDirection the direction to maximize (in axes space, not null,
     * not zero)
     */
    public void selectExtreme(Vector3f axesDirection) {
        Validate.nonZero(axesDirection, "direction");

        Transform transform = SceneUpdater.axesTransform(cgm);
        Quaternion rotation = transform.getRotation();
        Vector3f worldDirection = rotation.mult(axesDirection, null);

        selectExtremeWorld(worldDirection);
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
     * Alter which C-G model contains the vertex. (Invoked only during
     * initialization and cloning.)
     *
     * @param newCgm (not null, alias created)
     */
    void setCgm(Cgm newCgm) {
        assert newCgm != null;
        assert newCgm.getVertex() == this;

        cgm = newCgm;
    }

    /**
     * Alter the indexed component of the vertex data in the selected
     * FloatBuffer.
     *
     * @param componentIndex which component to modify (0-3)
     * @param newValue the desired value
     */
    public void setComponent(int componentIndex, float newValue) {
        SelectedBuffer selectedBuffer = cgm.getBuffer();
        int numComponentsPerElement = selectedBuffer.countComponents();
        int lastComponent = numComponentsPerElement - 1;
        Validate.inRange(componentIndex, "component index", 0, lastComponent);

        VertexBuffer.Format format = selectedBuffer.format();
        assert format == VertexBuffer.Format.Float : format;

        int floatIndex
                = selectedIndex * numComponentsPerElement + componentIndex;
        selectedBuffer.putFloat(floatIndex, newValue);
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
        Vector3f result = (storeResult == null) ? new Vector3f() : storeResult;

        Spatial selectedSpatial = cgm.getSceneView().selectedSpatial();
        Geometry selectedGeometry = (Geometry) selectedSpatial;
        Pose pose = cgm.getPose().get();
        Matrix4f[] matrices = pose.skin(null);
        MyMesh.vertexWorldLocation(selectedGeometry, selectedIndex, matrices,
                result);

        return result;
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
     * Select an extreme vertex.
     *
     * @param worldDirection the direction to maximize (in world space, not
     * null, not zero)
     */
    private void selectExtremeWorld(Vector3f worldDirection) {
        assert worldDirection != null;
        assert !MyVector3f.isZero(worldDirection);

        Spatial selectedSpatial = cgm.getSceneView().selectedSpatial();
        Geometry selectedGeometry = (Geometry) selectedSpatial;
        Pose pose = cgm.getPose().get();
        Matrix4f[] skinningMatrices = pose.skin(null);

        double bestDot = Double.NEGATIVE_INFINITY;
        Vector3f tmpWorldLocation = new Vector3f();

        int numVertices = cgm.getSpatial().countVertices();
        for (int iVertex = 0; iVertex < numVertices; iVertex++) {
            MyMesh.vertexWorldLocation(selectedGeometry, iVertex,
                    skinningMatrices, tmpWorldLocation);
            double dot = MyVector3f.dot(tmpWorldLocation, worldDirection);
            if (dot >= bestDot) {
                bestDot = dot;
                select(iVertex);
            }
        }
    }

    /**
     * Access the weight data for the selected vertex.
     *
     * @return a positioned, read-only buffer instance
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
