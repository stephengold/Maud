/*
 Copyright (c) 2017-2021, Stephen Gold
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
package maud;

import com.jme3.math.Transform;
import com.jme3.math.Triangle;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;
import java.util.logging.Logger;
import jme3utilities.MyMesh;
import jme3utilities.Validate;
import jme3utilities.math.MyBuffer;
import jme3utilities.math.MyVector3f;

/**
 * Utility methods for mesh manipulation. All methods should be static.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class MeshUtil {
    // *************************************************************************
    // constants and loggers

    /**
     * number of coordinate axes
     */
    final private static int numAxes = 3;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(MeshUtil.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private MeshUtil() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Generate mesh normals from positions. TODO move to Heart library
     *
     * @param mesh the input Mesh (not null)
     * @param algorithm which algorithm to use (not null)
     * @return a Mesh, possibly the input, although that's not likely
     */
    public static Mesh generateNormals(Mesh mesh, MeshNormals algorithm) {
        Validate.nonNull(mesh, "mesh");

        Mesh result = mesh;
        VertexBuffer position
                = result.getBuffer(VertexBuffer.Type.Position);
        if (position != null) {
            result = MeshUtil.generateNormals(result, VertexBuffer.Type.Normal,
                    VertexBuffer.Type.Position, algorithm);
        }

        VertexBuffer bindPosition
                = result.getBuffer(VertexBuffer.Type.BindPosePosition);
        if (bindPosition != null) {
            result = MeshUtil.generateNormals(result,
                    VertexBuffer.Type.BindPoseNormal,
                    VertexBuffer.Type.BindPosePosition, algorithm);
        }

        return result;
    }

    /**
     * Generate mesh normals from positions using the specified buffers. Any
     * pre-existing target buffer is discarded. TODO move to Heart library
     *
     * @param mesh the input Mesh (not null, unaffected)
     * @param normalBufferType (Normal or BindPoseNormal)
     * @param positionBufferType (Position or BindPosePosition)
     * @param algorithm which algorithm to use (not null)
     * @return a new Mesh
     */
    public static Mesh generateNormals(Mesh mesh,
            VertexBuffer.Type normalBufferType,
            VertexBuffer.Type positionBufferType, MeshNormals algorithm) {
        for (VertexBuffer inVertexBuffer : mesh.getBufferList()) {
            VertexBuffer.Type type = inVertexBuffer.getBufferType();
            if (type != VertexBuffer.Type.Index) {
                int numCperE = inVertexBuffer.getNumComponents();
                if (numCperE == 0) {
                    //??
                }
                assert numCperE >= 1 && numCperE <= 4 : "numCperE = " + numCperE + " type = " + type;
            }
        }

        Mesh result = mesh.deepClone();
        switch (algorithm) {
            case Facet: // NOTE: fail if expansion is needed in this case!
                generateFacetNormals(result, normalBufferType,
                        positionBufferType);
                break;

            case None:
                mesh.clearBuffer(normalBufferType);
                break;

            case Smooth:
                boolean expansion = MyMesh.hasIndices(mesh)
                        || (mesh.getMode() != Mesh.Mode.Triangles);
                if (!expansion) {
                    generateFacetNormals(result, normalBufferType,
                            positionBufferType);

                } else {
                    Mesh expandedMesh = MyMesh.expand(mesh);
                    generateFacetNormals(expandedMesh, normalBufferType,
                            positionBufferType);
                    /*
                     * Copy the vectors from the expanded target buffer
                     * to the clone's target buffer using the original indices.
                     * Most elements in the clone's target buffer
                     * will be written to more than once,
                     * but that shouldn't matter.
                     */
                    VertexBuffer expandedBuffer
                            = expandedMesh.getBuffer(normalBufferType);
                    FloatBuffer expanded = (FloatBuffer) expandedBuffer
                            .getDataReadOnly();

                    VertexBuffer targetBuffer
                            = result.getBuffer(normalBufferType);
                    FloatBuffer target = (FloatBuffer) targetBuffer.getData();

                    IndexBuffer indexList = mesh.getIndicesAsList();
                    int numIndices = indexList.size();
                    Vector3f tmpVector = new Vector3f();
                    for (int expandI = 0; expandI < numIndices; ++expandI) {
                        int originalI = indexList.get(expandI);
                        MyBuffer.get(expanded, numAxes * expandI, tmpVector);
                        MyBuffer.put(target, numAxes * originalI, tmpVector);
                    }
                }
                break;

            case Sphere:
                generateSphereNormals(result, normalBufferType,
                        positionBufferType);
                break;

            default:
                String message = "algorithm = " + algorithm;
                throw new IllegalArgumentException(message);
        }

        return result;
    }

    /**
     * Generate facet normals for a Triangles-mode Mesh without an index buffer.
     * Any pre-existing target buffer is discarded. TODO move to Heart library
     *
     * @param mesh the Mesh to modify (not null, mode=Triangles, not indexed)
     * @param normalBufferType the target buffer type (Normal or BindPoseNormal)
     * @param positionBufferType the source buffer type (Position or
     * BindPosePosition)
     */
    public static void generateFacetNormals(Mesh mesh,
            VertexBuffer.Type normalBufferType,
            VertexBuffer.Type positionBufferType) {
        assert mesh.getMode() == Mesh.Mode.Triangles : mesh.getMode();
        assert !MyMesh.hasIndices(mesh);

        FloatBuffer positionBuffer = mesh.getFloatBuffer(positionBufferType);
        int numFloats = positionBuffer.limit();

        FloatBuffer normalBuffer = BufferUtils.createFloatBuffer(numFloats);
        mesh.setBuffer(normalBufferType, numAxes, normalBuffer);

        Triangle triangle = new Triangle();
        Vector3f pos1 = new Vector3f();
        Vector3f pos2 = new Vector3f();
        Vector3f pos3 = new Vector3f();

        int numTriangles = numFloats / MyMesh.vpt / numAxes;
        for (int triIndex = 0; triIndex < numTriangles; ++triIndex) {
            int trianglePosition = triIndex * MyMesh.vpt * numAxes;
            MyBuffer.get(positionBuffer, trianglePosition, pos1);
            MyBuffer.get(positionBuffer, trianglePosition + numAxes, pos2);
            MyBuffer.get(positionBuffer, trianglePosition + 2 * numAxes, pos3);
            triangle.set(pos1, pos2, pos3);

            Vector3f normal = triangle.getNormal();
            for (int j = 0; j < MyMesh.vpt; ++j) {
                normalBuffer.put(normal.x);
                normalBuffer.put(normal.y);
                normalBuffer.put(normal.z);
            }
        }
        normalBuffer.flip();
    }

    /**
     * Generate sphere normals for Mesh. Any pre-existing target buffer is
     * discarded. TODO move to Heart library
     *
     * @param mesh the Mesh to modify (not null)
     * @param normalBufferType the target buffer type (Normal or BindPoseNormal)
     * @param positionBufferType the source buffer type (Position or
     * BindPosePosition)
     */
    public static void generateSphereNormals(Mesh mesh,
            VertexBuffer.Type normalBufferType,
            VertexBuffer.Type positionBufferType) {
        Validate.nonNull(mesh, "mesh");

        FloatBuffer positionBuffer = mesh.getFloatBuffer(positionBufferType);
        int numFloats = positionBuffer.limit();

        FloatBuffer normalBuffer = BufferUtils.clone(positionBuffer);
        mesh.setBuffer(normalBufferType, numAxes, normalBuffer);

        MyBuffer.normalize(normalBuffer, 0, numFloats);
        normalBuffer.limit(numFloats);
    }

    /**
     * Apply the specified coordinate transform to all data in the specified
     * VertexBuffer.
     *
     * @param mesh the subject mesh (not null)
     * @param bufferType which buffer to read (not null)
     * @param transform the Transform to apply (not null, unaffected)
     */
    public static void transformBuffer(Mesh mesh, VertexBuffer.Type bufferType,
            Transform transform) {
        Validate.nonNull(bufferType, "buffer type");
        Validate.nonNull(transform, "transform");

        VertexBuffer vertexBuffer = mesh.getBuffer(bufferType);
        if (vertexBuffer != null) {
            int count = mesh.getVertexCount();
            FloatBuffer floatBuffer = (FloatBuffer) vertexBuffer.getData();

            Vector3f tmpVector = new Vector3f();
            for (int vertexIndex = 0; vertexIndex < count; ++vertexIndex) {
                MyMesh.vertexVector3f(mesh, bufferType, vertexIndex, tmpVector);
                transform.transformVector(tmpVector, tmpVector);

                int floatIndex = MyVector3f.numAxes * vertexIndex;
                floatBuffer.put(floatIndex, tmpVector.x);
                floatBuffer.put(floatIndex + 1, tmpVector.y);
                floatBuffer.put(floatIndex + 2, tmpVector.z);
            }

            vertexBuffer.setUpdateNeeded();
        }
    }
}
