/*
 Copyright (c) 2017-2022, Stephen Gold
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

import com.jme3.math.Triangle;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.util.BufferUtils;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import jme3utilities.Element;
import jme3utilities.MeshNormals;
import jme3utilities.MyMesh;
import jme3utilities.Validate;
import jme3utilities.math.DistinctVectorValues;
import jme3utilities.math.IntPair;
import jme3utilities.math.MyBuffer;
import jme3utilities.math.MyMath;

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
     * Count how many meshes in the specified screen-graph subtree include
     * collision data.
     *
     * @param subtree the root of the subtree to analyze (may be null,
     * unaffected)
     * @return the number found (&ge;0)
     */
    public static int countCollisionTrees(Spatial subtree) {
        Validate.nonNull(subtree, "subtree");

        int result = 0;
        List<Mesh> meshes = MyMesh.listMeshes(subtree, null);
        for (Mesh mesh : meshes) {
            if (MyMesh.getCollisionTree(mesh) != null) {
                ++result;
            }
        }

        assert result >= 0 : result;
        return result;
    }

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
            result = generateNormals(result, VertexBuffer.Type.Normal,
                    VertexBuffer.Type.Position, algorithm);
        }

        VertexBuffer bindPosition
                = result.getBuffer(VertexBuffer.Type.BindPosePosition);
        if (bindPosition != null) {
            result = generateNormals(result,
                    VertexBuffer.Type.BindPoseNormal,
                    VertexBuffer.Type.BindPosePosition, algorithm);
        }

        return result;
    }

    /**
     * Generate mesh normals from positions using the specified buffers. Any
     * pre-existing target buffer is discarded. TODO move to Heart library
     *
     * @param mesh the input Mesh (not null, has triangles, unaffected)
     * @param normalBufferType (Normal or BindPoseNormal)
     * @param positionBufferType (Position or BindPosePosition)
     * @param algorithm which algorithm to use (not null)
     * @return a new Mesh
     */
    public static Mesh generateNormals(Mesh mesh,
            VertexBuffer.Type normalBufferType,
            VertexBuffer.Type positionBufferType, MeshNormals algorithm) {
        Validate.nonNull(mesh, "mesh");
        Validate.require(MyMesh.hasTriangles(mesh), "triangles in the mesh");
        Validate.require(normalBufferType == VertexBuffer.Type.Normal
                || normalBufferType == VertexBuffer.Type.BindPoseNormal,
                "normalBufferType = Normal or BindPoseNormal");
        Validate.require(positionBufferType == VertexBuffer.Type.Position
                || positionBufferType == VertexBuffer.Type.BindPosePosition,
                "positionBufferType = Position or BindPosePosition");
        Validate.nonNull(algorithm, "algorithm");

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
     * Maximally partition the specified Mesh into disjoint sub-meshes, based
     * vertex positions compared using the specified tolerance.
     *
     * @param mesh the input mesh (not null, no LODs)
     * @param positionType the type of the VertexBuffer to analyze (not null,
     * typically Position or BindPosition)
     * @param tolerance the minimum distance for distinct vertex positions (in
     * mesh units, &ge;0)
     * @return a new array of new meshes without index buffers
     */
    public static Mesh[] partition(Mesh mesh, VertexBuffer.Type positionType,
            float tolerance) {
        Validate.nonNull(positionType, "position type");
        Validate.nonNegative(tolerance, "tolerance");
        Validate.require(mesh.getNumLodLevels() == 0, "no LODs");

        int numVertices = mesh.getVertexCount();
        FloatBuffer buffer = mesh.getFloatBuffer(positionType);
        /*
         * Assign an ID to each distinct vertex position and
         * enumerate all pairs of IDs that are adjacent (joined by edges).
         */
        int startPosition = 0;
        int endPosition = numAxes * numVertices;
        DistinctVectorValues distinctPositions;
        if (tolerance == 0f) {
            distinctPositions = new DistinctVectorValues(buffer, startPosition,
                    endPosition);
        } else {
            distinctPositions = new DistinctVectorValues(buffer, startPosition,
                    endPosition, tolerance);
        }
        Set<IntPair> adjacentPairs = listAdjacentPairs(mesh, distinctPositions);
        /*
         * Assign each distinct position to a sub-mesh,
         * based on the adjacency data.
         */
        int[] vvid2Submesh = partitionIds(distinctPositions, adjacentPairs);
        int numSubmeshes = MyMath.maxInt(vvid2Submesh) + 1;
        if (numSubmeshes < 0) {
            numSubmeshes = 0;
        }
        /*
         * Construct the submeshes.
         */
        IndexBuffer indexList = mesh.getIndicesAsList();
        int numIndices = indexList.size();
        Mesh.Mode expandedMode = MyMesh.expandedMode(mesh);
        Mesh[] result = new Mesh[numSubmeshes];
        for (int submesh = 0; submesh < numSubmeshes; ++submesh) {
            /*
             * Determine the number of vertices for the submesh.
             */
            int numOutputVertices = 0;
            for (int ii = 0; ii < numIndices; ++ii) {
                int vertexIndex = indexList.get(ii);
                int vvid = distinctPositions.findVvid(vertexIndex);
                if (vvid2Submesh[vvid] == submesh) {
                    ++numOutputVertices;
                }
            }
            assert numOutputVertices > 0;
            /*
             * Instantiate the output mesh.
             */
            Mesh outputMesh = new Mesh();
            result[submesh] = outputMesh;
            outputMesh.setMode(expandedMode);

            for (VertexBuffer vb : mesh.getBufferList()) {
                VertexBuffer.Type type = vb.getBufferType();
                if (type == VertexBuffer.Type.Index) {
                    continue;
                }
                /*
                 * Create the VertexBuffer for output.
                 */
                int numCperE = vb.getNumComponents();
                numCperE = MyMath.clamp(numCperE, 1, 4); // to avoid an IAE
                VertexBuffer.Format format = vb.getFormat();
                if (format == null) {
                    format = VertexBuffer.Format.Float; // to avoid an NPE
                }
                Buffer outputBuffer = VertexBuffer.createBuffer(format,
                        numCperE, numOutputVertices);
                outputMesh.setBuffer(type, numCperE, format, outputBuffer);
                VertexBuffer outputVB = outputMesh.getBuffer(type);
                /*
                 * Perform an element-by-element copy from the input buffer
                 * to the output buffer.
                 */
                int outVI = 0;
                for (int ii = 0; ii < numIndices; ++ii) {
                    int vertexIndex = indexList.get(ii);
                    int vvid = distinctPositions.findVvid(vertexIndex);
                    if (vvid2Submesh[vvid] == submesh) {
                        if (vb.getNumElements() > 0) {
                            Element.copy(vb, vertexIndex, outputVB, outVI);
                        }
                        ++outVI;
                    }
                }
                assert outVI == numOutputVertices;
                int end = outputBuffer.capacity();
                outputBuffer.position(end);
                outputBuffer.flip();
            }

            outputMesh.updateBound();
            outputMesh.updateCounts();

            int mnw = mesh.getMaxNumWeights();
            outputMesh.setMaxNumWeights(mnw);
        }

        return result;
    }
    // *************************************************************************
    // private methods

    /**
     * Assign position IDs to existing sub-meshes based on adjacency to IDs that
     * have previously been assigned.
     *
     * @param id2Submesh map from position IDs to submeshes (not null, modified)
     * @param adjacentPairs list of adjacent ID pairs (not null, unaffected)
     * @param oldNumUnassigned the count of unassigned IDs
     * @return the new count of unassigned IDs
     */
    private static int assignIds(int[] id2Submesh, Set<IntPair> adjacentPairs,
            int oldNumUnassigned) {
        int result = oldNumUnassigned;
        /*
         * Iterate until further progress is impossible.
         */
        boolean progress;
        do {
            progress = false;
            /*
             * Assign position IDs to existing submeshes based on adjacency.
             */
            for (IntPair pair : adjacentPairs) {
                int idA = pair.smaller();
                int idB = pair.larger();

                int submeshA = id2Submesh[idA];
                int submeshB = id2Submesh[idB];
                if (submeshA == -1 && submeshB != -1) { // B assigned but not A
                    id2Submesh[idA] = submeshB;
                    --result;
                    progress = true;

                } else if (submeshA != -1 && submeshB == -1) { // A assigned but not B
                    id2Submesh[idB] = submeshA;
                    --result;
                    progress = true;
                }
            }
        } while (progress && result > 0);

        return result;
    }

    /**
     * Enumerate all pairs of distinct vertex positions that are adjacent (part
     * of the same edge or triangle) in the specified Mesh.
     *
     * @param mesh the Mesh to analyze (not null, unaffected)
     * @param distinctPositions distinct positions in the Mesh (not null)
     * @return a new set of ID pairs
     */
    private static Set<IntPair> listAdjacentPairs(Mesh mesh,
            DistinctVectorValues distinctPositions) {
        IndexBuffer indexList = mesh.getIndicesAsList();
        int numIndices = indexList.size();
        Mesh.Mode expandedMode = MyMesh.expandedMode(mesh);

        Set<IntPair> result = new HashSet<>(numIndices);
        if (MyMesh.hasTriangles(mesh)) {
            int numTriangles = numIndices / MyMesh.vpt;
            assert numTriangles * MyMesh.vpt == numIndices : numIndices;
            for (int triangleI = 0; triangleI < numTriangles; ++triangleI) {
                int startPosition = MyMesh.vpt * triangleI;
                int vertexI0 = indexList.get(startPosition);
                int vertexI1 = indexList.get(startPosition + 1);
                int vertexI2 = indexList.get(startPosition + 2);
                int vvid0 = distinctPositions.findVvid(vertexI0);
                int vvid1 = distinctPositions.findVvid(vertexI1);
                int vvid2 = distinctPositions.findVvid(vertexI2);
                if (vvid0 != vvid1) {
                    result.add(new IntPair(vvid0, vvid1));
                }
                if (vvid1 != vvid2) {
                    result.add(new IntPair(vvid1, vvid2));
                }
                if (vvid2 != vvid0) {
                    result.add(new IntPair(vvid2, vvid0));
                }
            }

        } else if (expandedMode == Mesh.Mode.Lines) {
            int numEdges = numIndices / MyMesh.vpe;
            assert numEdges * MyMesh.vpe == numIndices : numIndices;
            // assert fails on LineLoop meshes due to JME issue #1603
            for (int edgeI = 0; edgeI < numEdges; ++edgeI) {
                int startPosition = MyMesh.vpe * edgeI;
                int vertexI0 = indexList.get(startPosition);
                int vertexI1 = indexList.get(startPosition + 1); // IndexOutOfBoundsException on LineLoop meshes due to JME issue #1603
                int vvid0 = distinctPositions.findVvid(vertexI0);
                int vvid1 = distinctPositions.findVvid(vertexI1);
                if (vvid0 != vvid1) {
                    result.add(new IntPair(vvid0, vvid1));
                }
            }
        }

        return result;
    }

    /**
     * Assign each distinct vertex position to a sub-mesh, based on the
     * specified adjacency data.
     *
     * @param distinctPositions an analysis of distinct vertex positions (not
     * null)
     * @param adjacentPairs adjacency data for distinct vertex positions (not
     * null, unaffected)
     * @return a new array of sub-mesh indices, one for each distinct position
     */
    private static int[] partitionIds(DistinctVectorValues distinctPositions,
            Set<IntPair> adjacentPairs) {
        int numDistinctPositions = distinctPositions.countDistinct();
        int[] result = new int[numDistinctPositions];
        Arrays.fill(result, -1);

        int numUnassigned = numDistinctPositions;
        int numSubmeshes = 0;
        while (numUnassigned > 0) {
            /*
             * Allocate a new submesh for the first unassigned position.
             */
            for (int idA = 0; idA < numDistinctPositions; ++idA) {
                int submeshA = result[idA];
                if (submeshA == -1) { // A isn't assigned yet
                    submeshA = numSubmeshes;
                    ++numSubmeshes;

                    result[idA] = submeshA;
                    --numUnassigned;
                    break;
                }
            }
            /*
             * Assign IDs to the new submesh until closure is achieved.
             */
            numUnassigned = assignIds(result, adjacentPairs, numUnassigned);
        }

        return result;
    }
}
