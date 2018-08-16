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
package maud;

import com.jme3.animation.AnimControl;
import com.jme3.animation.Animation;
import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.animation.SpatialTrack;
import com.jme3.animation.Track;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.asset.ModelKey;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix3f;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Transform;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.control.Control;
import com.jme3.scene.plugins.blender.meshes.Face;
import com.jme3.scene.plugins.bvh.BVHAnimData;
import com.jme3.scene.plugins.bvh.SkeletonMapping;
import com.jme3.scene.plugins.ogre.MaterialLoader;
import com.jme3.scene.plugins.ogre.MeshLoader;
import com.jme3.shader.VarType;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.Texture3D;
import com.jme3.texture.TextureCubeMap;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import com.jme3.util.clone.Cloner;
import java.io.IOException;
import java.io.Writer;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jme3utilities.MyMesh;
import jme3utilities.MySpatial;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import jme3utilities.math.MyVector3f;
import jme3utilities.nifty.dialog.VectorDialog;
import jme3utilities.wes.Pose;
import maud.model.option.RotationDisplayMode;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for the Maud application. All methods should be static.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class MaudUtil {
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
            = Logger.getLogger(MaudUtil.class.getName());
    /**
     * pattern for matching a color
     */
    final private static Pattern colorPattern = Pattern.compile(
            "Color\\[\\s*([^,]+),\\s*([^,]+),\\s*([^,]+),\\s*(\\S+)\\s*]");
    /**
     * pattern for matching the word "null"
     */
    final private static Pattern nullPattern = Pattern.compile("\\s*null\\s*");
    /**
     * pattern for matching a Vector3f
     */
    final private static Pattern vector3fPattern = Pattern.compile(
            "\\(\\s*([^,]+),\\s+([^,]+),\\s+(\\S+)\\s*\\)");
    /**
     * local copy of {@link com.jme3.math.Transform#IDENTITY}
     */
    final private static Transform transformIdentity = new Transform();
    /**
     * local copy of {@link com.jme3.math.Vector3f#UNIT_X}
     */
    final private static Vector3f xAxis = new Vector3f(1f, 0f, 0f);
    /**
     * local copy of {@link com.jme3.math.Vector3f#UNIT_Y}
     */
    final private static Vector3f yAxis = new Vector3f(0f, 1f, 0f);
    /**
     * local copy of {@link com.jme3.math.Vector3f#UNIT_Z}
     */
    final private static Vector3f zAxis = new Vector3f(0f, 0f, 1f);
    /**
     * array of cardinal axes
     */
    final private static Vector3f cardinalAxes[] = {
        new Vector3f(1f, 0f, 0f),
        new Vector3f(0f, 1f, 0f),
        new Vector3f(0f, 0f, 1f),
        new Vector3f(-1f, 0f, 0f),
        new Vector3f(0f, -1f, 0f),
        new Vector3f(0f, 0f, -1f)
    };
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private MaudUtil() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Add indices to the result for bones that influence (directly or
     * indirectly) vertices in the specified subtree of the scene graph. Note:
     * recursive!
     *
     * @param subtree subtree to traverse (may be null, unaffected)
     * @param skeleton skeleton (not null, unaffected)
     * @param storeResult (modified if not null)
     * @return the set of bones with influence (either storeResult or a new
     * instance)
     */
    public static BitSet addAllInfluencers(Spatial subtree, Skeleton skeleton,
            BitSet storeResult) {
        int numBones = skeleton.getBoneCount();
        if (storeResult == null) {
            storeResult = new BitSet(numBones);
        }

        addDirectInfluencers(subtree, storeResult);

        for (int boneIndex = 0; boneIndex < numBones; boneIndex++) {
            if (storeResult.get(boneIndex)) {
                Bone bone = skeleton.getBone(boneIndex);
                for (Bone parent = bone.getParent();
                        parent != null;
                        parent = parent.getParent()) {
                    int parentIndex = skeleton.getBoneIndex(parent);
                    storeResult.set(parentIndex);
                }
            }
        }

        return storeResult;
    }

    /**
     * Add indices to the result for bones that directly influence vertices in
     * the specified mesh.
     *
     * @param mesh animated mesh to analyze (not null, unaffected)
     * @param storeResult (modified if not null)
     * @return the set of bones with influence (either storeResult or a new
     * instance)
     */
    public static BitSet addDirectInfluencers(Mesh mesh, BitSet storeResult) {
        if (storeResult == null) {
            storeResult = new BitSet(120);
        }

        int maxWeightsPerVert = mesh.getMaxNumWeights();
        if (maxWeightsPerVert <= 0) {
            maxWeightsPerVert = 1;
        }
        assert maxWeightsPerVert > 0 : maxWeightsPerVert;
        assert maxWeightsPerVert <= 4 : maxWeightsPerVert;

        VertexBuffer biBuf = mesh.getBuffer(VertexBuffer.Type.BoneIndex);
        Buffer boneIndexBuffer = biBuf.getDataReadOnly();
        boneIndexBuffer.rewind();
        int numBoneIndices = boneIndexBuffer.remaining();
        assert numBoneIndices % 4 == 0 : numBoneIndices;
        int numVertices = boneIndexBuffer.remaining() / 4;

        VertexBuffer wBuf = mesh.getBuffer(VertexBuffer.Type.BoneWeight);
        FloatBuffer weightBuffer = (FloatBuffer) wBuf.getDataReadOnly();
        weightBuffer.rewind();
        int numWeights = weightBuffer.remaining();
        assert numWeights == numVertices * 4 : numWeights;

        for (int vIndex = 0; vIndex < numVertices; vIndex++) {
            for (int wIndex = 0; wIndex < 4; wIndex++) {
                float weight = weightBuffer.get();
                int boneIndex = MyMesh.readIndex(boneIndexBuffer);
                if (wIndex < maxWeightsPerVert && weight > 0f) {
                    storeResult.set(boneIndex);
                }
            }
        }

        return storeResult;
    }

    /**
     * Add indices to the result for bones that directly influence vertices in
     * the specified subtree of the scene graph. Note: recursive!
     *
     * @param subtree subtree to traverse (may be null, unaffected)
     * @param storeResult (modified if not null)
     * @return the set of bones with influence (either storeResult or a new
     * instance)
     */
    public static BitSet addDirectInfluencers(Spatial subtree,
            BitSet storeResult) {
        if (storeResult == null) {
            storeResult = new BitSet(120);
        }

        if (subtree instanceof Geometry) {
            Geometry geometry = (Geometry) subtree;
            Mesh mesh = geometry.getMesh();
            if (isAnimated(mesh)) {
                addDirectInfluencers(mesh, storeResult);
            }

        } else if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                addDirectInfluencers(child, storeResult);
            }
        }

        return storeResult;
    }

    /**
     * Find the cardinal direction most similar to the specified input. TODO use
     * heart library
     *
     * @param input (not null, modified)
     */
    public static void cardinalizeLocal(Vector3f input) {
        input.normalizeLocal();
        /*
         * Generate each of the 6 cardinal directions.
         */
        Vector3f bestCardinalDirection = new Vector3f();
        float bestDot = -2f;
        for (Vector3f x : cardinalAxes) {
            /*
             * Measure the similarity of the 2 directions
             * using their dot product.
             */
            float dot = x.dot(input);
            if (dot > bestDot) {
                bestDot = dot;
                bestCardinalDirection.set(x);
            }
        }

        input.set(bestCardinalDirection);
    }

    /**
     * Count all uses of the specified material in the specified subtree of a
     * scene graph. Note: recursive! TODO use heart library
     *
     * @param subtree (may be null, unaffected)
     * @param material (unaffected)
     * @return the use count (&ge;0)
     */
    public static int countUses(Spatial subtree, Material material) {
        int count = 0;

        if (subtree instanceof Geometry) {
            Geometry geometry = (Geometry) subtree;
            Material mat = geometry.getMaterial();
            if (mat == material) {
                ++count;
            }

        } else if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                count += countUses(child, material);
            }
        }

        return count;
    }

    /**
     * Create a deep copy of the specified object. TODO use heart library
     *
     * @param object input (unaffected)
     * @return a new object, equivalent to the input
     */
    public static Object deepClone(Object object) {
        Object clone;
        if (object instanceof Boolean || object instanceof Enum) {
            clone = object;
        } else if (object instanceof Double) {
            clone = (double) object;
        } else if (object instanceof Float) {
            clone = (float) object;
        } else if (object instanceof Integer) {
            clone = (int) object;
        } else if (object instanceof Long) {
            clone = (long) object;
        } else if (object instanceof Short) {
            clone = (short) object;
        } else {
            clone = Cloner.deepClone(object);
        }

        return clone;
    }

    /**
     * Generate an arbitrary non-null value for a material parameter.
     *
     * @param varType (not null)
     * @param parameterName name of the parameter (not null, not empty)
     * @return a new instance (not null)
     */
    public static Object defaultValue(VarType varType, String parameterName) {
        Validate.nonEmpty(parameterName, "parameter name");

        Object result;
        Image image;
        Texture texture;
        switch (varType) {
            case Boolean:
                result = false;
                break;
            case Float:
                result = 0f;
                break;
            case FloatArray:
                result = new float[]{0f};
                break;
            case Int:
                result = 1; // PreShadow.vert crashes if NumberOfBones < 1
                break;
            case IntArray:
                result = new int[]{0};
                break;
            case Matrix3:
                result = new Matrix3f();
                break;
            case Matrix3Array:
                result = new Matrix3f[]{new Matrix3f()};
                break;
            case Matrix4:
                result = new Matrix4f();
                break;
            case Matrix4Array:
                result = new Matrix4f[]{new Matrix4f()};
                break;
            case Texture2D:
                image = new Image();
                image.setFormat(Image.Format.BGRA8);
                texture = new Texture2D();
                texture.setImage(image);
                result = texture;
                break;
            case Texture3D:
                result = new Texture3D();
                break;
            case TextureCubeMap:
                ArrayList<ByteBuffer> data = new ArrayList<>(6);
                for (int i = 0; i < 6; i++) {
                    ByteBuffer buffer = BufferUtils.createByteBuffer(32);
                    data.add(buffer);
                }
                image = new Image(Image.Format.BGRA8, 1, 1, 1, data,
                        ColorSpace.Linear);
                texture = new TextureCubeMap();
                texture.setImage(image);
                result = texture;
                break;
            case Vector2:
                result = new Vector2f();
                break;
            case Vector2Array:
                result = new Vector2f[]{new Vector2f()};
                break;
            case Vector3:
                result = new Vector3f();
                break;
            case Vector3Array:
                result = new Vector3f[]{new Vector3f()};
                break;
            case Vector4:
                result = new Vector4f();
                break;
            case Vector4Array:
                result = new Vector4f[]{new Vector4f()};
                break;
            default:// TODO handle TextureArray, TextureBuffer
                logger.log(Level.SEVERE, "varType={0}", varType);
                throw new IllegalArgumentException(varType.toString());
        }

        assert result != null;
        return result;
    }

    /**
     * Describe a pair of display dimensions.
     *
     * @param width width in pixels (&gt;0)
     * @param height height in pixels (&gt;0)
     * @return a textual description (not null, not empty)
     */
    public static String describeDimensions(int width, int height) {
        Validate.positive(width, "width");
        Validate.positive(height, "height");

        String description = String.format("%d x %d", width, height);
        return description;
    }

    /**
     * Describe an MSAA sampling factor.
     *
     * @param factor samples per pixel (&ge;0, &le;16)
     * @return a textual description (not null, not empty)
     */
    public static String describeMsaaFactor(int factor) {
        String description;
        if (factor <= 1) {
            description = "disabled";
        } else {
            description = String.format("%dx", factor);
        }

        return description;
    }

    /**
     * Calculate the slider positions and status values to display the specified
     * rotation in the specified display mode.
     *
     * @param rotation (not null, possibly modified)
     * @param mode (not null)
     * @param storeValues (not null, length=3, modified)
     * @param storePositions (not null, length=3, modified)
     * @return unit suffix (not null)
     */
    public static String displayRotation(Quaternion rotation,
            RotationDisplayMode mode, float[] storeValues,
            float[] storePositions) {
        Validate.nonNull(rotation, "rotation");
        Validate.nonNull(storePositions, "slider positions");
        int numSliders = storePositions.length;
        Validate.inRange(numSliders, "number of sliders", numAxes, numAxes);
        Validate.nonNull(storeValues, "status values");
        int numStatuses = storeValues.length;
        Validate.inRange(numStatuses, "number of statuses", numAxes, numAxes);

        String unitSuffix;
        switch (mode) {
            case Degrees:
                rotation.toAngles(storePositions);
                for (int iAxis = 0; iAxis < numAxes; iAxis++) {
                    storeValues[iAxis]
                            = MyMath.toDegrees(storePositions[iAxis]);
                }
                unitSuffix = " deg";
                break;

            case QuatCoeff:
                if (rotation.getW() < 0f) {
                    rotation.negate();
                }
                storeValues[0] = rotation.getX();
                storeValues[1] = rotation.getY();
                storeValues[2] = rotation.getZ();
                storePositions[0] = 3.142f * storeValues[0];
                storePositions[1] = 3.142f * storeValues[1];
                storePositions[2] = 1.571f * storeValues[2];
                unitSuffix = "";
                break;

            case Radians:
                rotation.toAngles(storePositions);
                System.arraycopy(storePositions, 0, storeValues, 0, numAxes);
                unitSuffix = " rad";
                break;

            default:
                logger.log(Level.SEVERE, "mode={0}", mode);
                throw new IllegalArgumentException(
                        "invalid rotation display mode");
        }

        return unitSuffix;
    }

    /**
     * For the specified camera ray, find the nearest collision involving a
     * triangle facing the camera.
     *
     * @param subtree collidable subtree of the scene graph (not null,
     * unaffected)
     * @param ray camera ray (not null, unaffected)
     * @return collision result, or null of no collision with a triangle facing
     * the camera
     */
    public static CollisionResult findCollision(Spatial subtree, Ray ray) {
        Validate.nonNull(subtree, "subtree");
        Validate.nonNull(ray, "ray");

        MySpatial.prepareForCollide(subtree);
        CollisionResults results = new CollisionResults();
        subtree.collideWith(ray, results);
        /*
         * Collision results are sorted by increasing distance from the camera,
         * so the first result is also the nearest one.
         */
        for (int resultIndex = 0; resultIndex < results.size(); resultIndex++) {
            /*
             * Calculate the offset from the camera to the point of contact.
             */
            CollisionResult result = results.getCollision(resultIndex);
            Vector3f contactPoint = result.getContactPoint();
            Vector3f offset = contactPoint.subtract(ray.origin);
            /*
             * If the dot product of the normal with the offset is negative,
             * then the triangle faces the camera.
             */
            Vector3f normal = result.getContactNormal();
            float dotProduct = offset.dot(normal);
            if (dotProduct < 0f) {
                return result;
            }
        }

        return null;
    }

    /**
     * Find the specified spatial in the specified subtree and optionally store
     * its tree position.
     *
     * @param spatial spatial to find (not null, unaffected)
     * @param subtree which subtree to search (may be null, unaffected)
     * @param storePosition tree position of the spatial (modified if found and
     * not null)
     * @return true if found, otherwise false
     */
    public static boolean findPosition(Spatial spatial, Spatial subtree,
            List<Integer> storePosition) {
        Validate.nonNull(spatial, "spatial");

        boolean success = false;
        if (spatial == subtree) {
            success = true;
            if (storePosition != null) {
                storePosition.clear();
            }

        } else if (subtree instanceof Node) {
            Node subtreeNode = (Node) subtree;
            if (spatial.hasAncestor(subtreeNode)) {
                success = true;
                if (storePosition != null) {
                    storePosition.clear();
                    /*
                     * Climb to the subtree's root, adding indices to the list.
                     */
                    Spatial climber = spatial;
                    while (climber != subtree) {
                        Node parent = climber.getParent();
                        int index = parent.getChildIndex(climber);
                        assert index >= 0 : index;
                        storePosition.add(index);
                        climber = parent;
                    }
                    Collections.reverse(storePosition);
                }
            }
        }

        return success;
    }

    /**
     * Find a spatial with the specified name in the specified subtree. Note:
     * recursive!
     *
     * @param name what name to search for (not null, not empty)
     * @param subtree which subtree to search (may be null, unaffected)
     * @param storePosition tree position of the spatial (modified if found and
     * not null)
     * @return the pre-existing spatial, or null if not found
     */
    public static Spatial findSpatialNamed(String name, Spatial subtree,
            List<Integer> storePosition) {
        Spatial result = null;
        if (subtree != null) {
            String spatialName = subtree.getName();
            if (spatialName != null && spatialName.equals(name)) {
                result = subtree;
                if (storePosition != null) {
                    storePosition.clear();
                }

            } else if (subtree instanceof Node) {
                Node node = (Node) subtree;
                List<Spatial> children = node.getChildren();
                int numChildren = children.size();
                for (int childI = 0; childI < numChildren; childI++) {
                    Spatial child = children.get(childI);
                    result = findSpatialNamed(name, child, storePosition);
                    if (result != null) {
                        if (storePosition != null) {
                            storePosition.add(0, childI);
                        }
                        break;
                    }
                }
            }
        }

        return result;
    }

    /**
     * Access the indexed SpatialTrack in the specified animation.
     *
     * @param animation which animation to search (not null, alias created)
     * @param spatialTrackIndex which spatial track (&ge;0)
     * @return the pre-existing instance, or null if not found
     */
    public static SpatialTrack findSpatialTrack(Animation animation,
            int spatialTrackIndex) {
        Validate.nonNegative(spatialTrackIndex, "spatial track index");

        Track[] tracks = animation.getTracks();
        int spatialTracksSeen = 0;
        for (Track track : tracks) {
            if (track instanceof SpatialTrack) {
                SpatialTrack spatialTrack = (SpatialTrack) track;
                if (spatialTracksSeen == spatialTrackIndex) {
                    return spatialTrack;
                }
                ++spatialTracksSeen;
            }
        }

        return null;
    }

    /**
     * Find the point of vertical support (minimum Y coordinate) for the
     * specified geometry transformed by the specified skinning matrices.
     *
     * @param geometry (not null)
     * @param skinningMatrices (not null, unaffected)
     * @param storeLocation point in world coordinates (not null, modified)
     * @return index of vertex in the geometry's mesh (&ge;0) or -1 if none
     * found
     */
    public static int findSupport(Geometry geometry,
            Matrix4f[] skinningMatrices, Vector3f storeLocation) {
        Validate.nonNull(skinningMatrices, "skinning matrices");
        Validate.nonNull(storeLocation, "store location");

        int bestIndex = -1;
        float bestY = Float.POSITIVE_INFINITY;

        Vector3f meshLoc = new Vector3f();
        Vector3f worldLoc = new Vector3f();

        Mesh mesh = geometry.getMesh();
        int maxWeightsPerVertex = mesh.getMaxNumWeights();

        VertexBuffer posBuf
                = mesh.getBuffer(VertexBuffer.Type.BindPosePosition);
        FloatBuffer posBuffer = (FloatBuffer) posBuf.getDataReadOnly();
        posBuffer.rewind();

        VertexBuffer wBuf = mesh.getBuffer(VertexBuffer.Type.BoneWeight);
        FloatBuffer weightBuffer = (FloatBuffer) wBuf.getDataReadOnly();
        weightBuffer.rewind();

        VertexBuffer biBuf = mesh.getBuffer(VertexBuffer.Type.BoneIndex);
        Buffer boneIndexBuffer = biBuf.getData();
        boneIndexBuffer.rewind();

        int numVertices = posBuffer.remaining() / MyVector3f.numAxes;
        for (int vertexIndex = 0; vertexIndex < numVertices; vertexIndex++) {
            float bx = posBuffer.get(); // bind position
            float by = posBuffer.get();
            float bz = posBuffer.get();

            meshLoc.zero();
            for (int wIndex = 0; wIndex < maxWeightsPerVertex; wIndex++) {
                float weight = weightBuffer.get();
                int boneIndex = MyMesh.readIndex(boneIndexBuffer);
                if (weight != 0f) {
                    Matrix4f s = skinningMatrices[boneIndex];
                    float xOff = s.m00 * bx + s.m01 * by + s.m02 * bz + s.m03;
                    float yOff = s.m10 * bx + s.m11 * by + s.m12 * bz + s.m13;
                    float zOff = s.m20 * bx + s.m21 * by + s.m22 * bz + s.m23;
                    meshLoc.x += weight * xOff;
                    meshLoc.y += weight * yOff;
                    meshLoc.z += weight * zOff;
                }
            }

            if (geometry.isIgnoreTransform()) {
                worldLoc.set(meshLoc);
            } else {
                geometry.localToWorld(meshLoc, worldLoc);
            }
            if (worldLoc.y < bestY) {
                bestIndex = vertexIndex;
                bestY = worldLoc.y;
                storeLocation.set(worldLoc);
            }

            for (int wIndex = maxWeightsPerVertex; wIndex < 4; wIndex++) {
                weightBuffer.get();
                MyMesh.readIndex(boneIndexBuffer);
            }
        }

        return bestIndex;
    }

    /**
     * Find the point of vertical support (minimum Y coordinate) for the meshes
     * in the specified subtree, each transformed by the specified skinning
     * matrices.
     *
     * @param subtree (may be null)
     * @param skinningMatrices (not null, unaffected)
     * @param storeLocation point in world coordinates (not null, modified)
     * @param storeGeometry (not null, modified)
     * @return index of vertex in storeGeometry's mesh (&ge;0) or -1 if none
     * found
     */
    public static int findSupport(Spatial subtree, Matrix4f[] skinningMatrices,
            Vector3f storeLocation, Geometry[] storeGeometry) {
        Validate.nonNull(skinningMatrices, "skinning matrices");
        Validate.nonNull(storeLocation, "store location");
        Validate.nonNull(storeGeometry, "store geometry");
        assert storeGeometry.length == 1 : storeGeometry.length;

        int bestIndex = -1;
        storeGeometry[0] = null;
        float bestY = Float.POSITIVE_INFINITY;
        Vector3f tmpLocation = new Vector3f();

        if (subtree instanceof Geometry) {
            Geometry geometry = (Geometry) subtree;
            int index = findSupport(geometry, skinningMatrices, tmpLocation);
            if (tmpLocation.y < bestY) {
                bestIndex = index;
                storeGeometry[0] = geometry;
                storeLocation.set(tmpLocation);
            }

        } else if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            Geometry[] tmpGeometry = new Geometry[1];
            for (Spatial child : children) {
                int index = findSupport(child, skinningMatrices, tmpLocation,
                        tmpGeometry);
                if (tmpLocation.y < bestY) {
                    bestIndex = index;
                    bestY = tmpLocation.y;
                    storeGeometry[0] = tmpGeometry[0];
                    storeLocation.set(tmpLocation);
                }
            }
        }

        return bestIndex;
    }

    /**
     * Format an index value for the current index base.
     *
     * @param index zero-base index value (&ge;0)
     * @return formatted text string (not null, not empty)
     */
    public static String formatIndex(int index) {
        Validate.nonNegative(index, "index");

        String result;
        int indexBase = Maud.getModel().getMisc().getIndexBase();
        if (indexBase == 0) {
            result = String.format("[%d]", index);
        } else if (indexBase == 1) {
            result = String.format("#%d", index + 1);
        } else {
            throw new IllegalStateException();
        }

        assert result != null;
        assert !result.isEmpty();
        return result;
    }

    /**
     * Calculate half extents for a symmetrical bounding box aligned with the
     * local axes of the specified scene-graph subtree.
     *
     * @param subtree (not null, unaffected)
     * @return a new vector
     */
    public static Vector3f halfExtents(Spatial subtree) {
        Spatial clone = subtree.clone(false);
        clone.setLocalTransform(transformIdentity);
        Vector3f[] minMax = MySpatial.findMinMaxCoords(clone);
        float heX = Math.max(-minMax[0].x, minMax[1].x);
        float heY = Math.max(-minMax[0].y, minMax[1].y);
        float heZ = Math.max(-minMax[0].z, minMax[1].z);
        Vector3f result = new Vector3f(heX, heY, heZ);

        assert MyVector3f.isAllNonNegative(result);
        return result;
    }

    /**
     * Test whether there are any "extra" spatials in the specified subtree.
     * Note: recursive!
     *
     * @param subtree subtree to traverse (may be null, unaffected)
     * @param attachmentsNodes collection of attachments nodes (not null,
     * unaffected)
     * @return true if any found, otherwise false
     */
    public static boolean hasExtraSpatials(Spatial subtree,
            Collection<Spatial> attachmentsNodes) {
        Validate.nonNull(attachmentsNodes, "attachments nodes");

        boolean result;
        if (subtree != null && isExtra(subtree, attachmentsNodes)) {
            result = true;

        } else if (subtree instanceof Node) {
            result = false;
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                result = hasExtraSpatials(child, attachmentsNodes);
                if (result) {
                    break;
                }
            }

        } else {
            result = false;
        }

        return result;
    }

    /**
     * Test whether the specified mesh is animated. Unlike mesh.isAnimated()
     * this method checks for bone weights and ignores HW buffers.
     *
     * @param mesh which mesh to test (not null, unaffected)
     * @return true if animated, otherwise false
     */
    public static boolean isAnimated(Mesh mesh) {
        VertexBuffer indices = mesh.getBuffer(VertexBuffer.Type.BoneIndex);
        boolean hasIndices = indices != null;
        VertexBuffer weights = mesh.getBuffer(VertexBuffer.Type.BoneWeight);
        boolean hasWeights = weights != null;
        boolean result = hasIndices && hasWeights;

        return result;
    }

    /**
     * Test whether the specified spatial is an "extra" spatial.
     *
     * A spatial is "extra" iff neither it nor any of its descendents:
     * <ul>
     * <li> is an attachments node
     * <li> has a scene-graph control
     * <li> has user data, or
     * <li> has mesh vertices.
     * </ul>
     *
     * @param spatial spatial to test (not null, unaffected)
     * @param attachmentsNodes collection of attachments nodes (not null,
     * unaffected)
     * @return true if extra, otherwise false
     */
    public static boolean isExtra(Spatial spatial,
            Collection<Spatial> attachmentsNodes) {
        Validate.nonNull(spatial, "spatial");
        Validate.nonNull(attachmentsNodes, "attachments nodes");

        boolean hasAttachments
                = MySpatial.subtreeContainsAny(spatial, attachmentsNodes);
        int numSgcs = MySpatial.countControls(spatial, Control.class);
        int numUserData = MySpatial.countUserData(spatial);
        int numVertices = MySpatial.countVertices(spatial);

        boolean result;
        if (!hasAttachments && numSgcs == 0 && numUserData == 0
                && numVertices == 0) {
            result = true;
        } else {
            result = false;
        }

        return result;
    }

    /**
     * Enumerate all materials in the specified subtree of a scene graph. Note:
     * recursive! TODO use heart library
     *
     * @param subtree (may be null, aliases created)
     * @param storeResult (added to if not null)
     * @return an expanded list (either storeResult or a new instance)
     */
    public static List<Material> listMaterials(Spatial subtree,
            List<Material> storeResult) {
        if (storeResult == null) {
            storeResult = new ArrayList<>(10);
        }

        if (subtree instanceof Geometry) {
            Geometry geometry = (Geometry) subtree;
            Material material = geometry.getMaterial();
            if (!storeResult.contains(material)) {
                storeResult.add(material);
            }

        } else if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                listMaterials(child, storeResult);
            }
        }

        return storeResult;
    }

    /**
     * Enumerate all meshes in the specified subtree of a scene graph. Note:
     * recursive! TODO use heart library
     *
     * @param subtree (may be null, aliases created)
     * @param storeResult (added to if not null)
     * @return an expanded list (either storeResult or a new instance)
     */
    public static List<Mesh> listMeshes(Spatial subtree,
            List<Mesh> storeResult) {
        if (storeResult == null) {
            storeResult = new ArrayList<>(10);
        }

        if (subtree instanceof Geometry) {
            Geometry geometry = (Geometry) subtree;
            Mesh mesh = geometry.getMesh();
            if (!storeResult.contains(mesh)) {
                storeResult.add(mesh);
            }

        } else if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                listMeshes(child, storeResult);
            }
        }

        return storeResult;
    }

    /**
     * Load a BVH asset as a C-G model.
     *
     * @param assetManager asset manager
     * @param key key for BVH asset
     * @param diagnose true&rarr;messages to console, false&rarr;no messages
     * @return a new orphan spatial, or null if unsuccessful
     */
    public static Spatial loadBvhAsset(AssetManager assetManager,
            AssetKey<BVHAnimData> key, boolean diagnose) {
        if (assetManager == null || key == null) {
            return null;
        }

        BVHAnimData loadedData;
        try {
            loadedData = assetManager.loadAsset(key);
        } catch (RuntimeException exception) {
            if (diagnose) {
                exception.printStackTrace();
            }
            return null;
        }

        Skeleton skeleton = loadedData.getSkeleton();
        SkeletonControl skeletonControl = new SkeletonControl(skeleton);

        AnimControl animControl = new AnimControl(skeleton);
        Animation anim = loadedData.getAnimation();
        animControl.addAnim(anim);

        String name = key.getName();
        Spatial result = new Node(name);
        result.addControl(animControl);
        result.addControl(skeletonControl);

        return result;
    }

    /**
     * Load a C-G model asset.
     *
     * @param assetManager asset manager
     * @param key asset key for the C-G model
     * @param diagnose true&rarr;messages to console, false&rarr;no messages
     * @return a new orphan spatial, or null if unsuccessful
     */
    public static Spatial loadCgmAsset(AssetManager assetManager,
            ModelKey key, boolean diagnose) {
        if (assetManager == null || key == null) {
            return null;
        }

        Logger faceLogger = Logger.getLogger(Face.class.getName());
        Level faceLevel = faceLogger.getLevel();

        Logger meshLoaderLogger = Logger.getLogger(MeshLoader.class.getName());
        Level meshLoaderLevel = meshLoaderLogger.getLevel();

        Logger materialLoaderLogger
                = Logger.getLogger(MaterialLoader.class.getName());
        Level materialLoaderLevel = materialLoaderLogger.getLevel();

        Logger compoundCollisionShapeLogger
                = Logger.getLogger(CompoundCollisionShape.class.getName());
        Level compoundCollisionShapeLevel
                = compoundCollisionShapeLogger.getLevel();

        org.slf4j.Logger slfLogger;
        slfLogger = LoggerFactory.getLogger("jme3_ext_xbuf.XbufLoader");
        ch.qos.logback.classic.Logger xbufLoaderLogger;
        xbufLoaderLogger = (ch.qos.logback.classic.Logger) slfLogger;
        ch.qos.logback.classic.Level xbufLoaderLevel;
        xbufLoaderLevel = xbufLoaderLogger.getLevel();

        if (!diagnose) {
            /*
             * Temporarily hush warnings about failures to triangulate,
             * vertices with >4 weights, shapes that can't be scaled, and
             * unsupported pass directives.
             */
            faceLogger.setLevel(Level.SEVERE);
            meshLoaderLogger.setLevel(Level.SEVERE);
            materialLoaderLogger.setLevel(Level.SEVERE);
            compoundCollisionShapeLogger.setLevel(Level.SEVERE);
            xbufLoaderLogger.setLevel(ch.qos.logback.classic.Level.ERROR);
        }
        /*
         * Load the model.
         */
        Spatial loaded;
        try {
            loaded = assetManager.loadModel(key);
        } catch (RuntimeException exception) {
            if (diagnose) {
                exception.printStackTrace();
            }
            loaded = null;
        }
        if (!diagnose) {
            /*
             * Restore logging levels.
             */
            faceLogger.setLevel(faceLevel);
            meshLoaderLogger.setLevel(meshLoaderLevel);
            materialLoaderLogger.setLevel(materialLoaderLevel);
            compoundCollisionShapeLogger.setLevel(compoundCollisionShapeLevel);
            xbufLoaderLogger.setLevel(xbufLoaderLevel);
        }

        return loaded;
    }

    /**
     * Load a J3O asset as a skeleton map without logging any warning/error
     * messages.
     *
     * @param assetManager asset manager
     * @param key key for skeleton map asset
     * @param diagnose true&rarr;messages to console, false&rarr;no messages
     * @return a skeleton map, or null if unsuccessful
     */
    public static SkeletonMapping loadMapAsset(AssetManager assetManager,
            AssetKey<SkeletonMapping> key, boolean diagnose) {
        if (assetManager == null || key == null) {
            return null;
        }

        SkeletonMapping loaded;
        try {
            loaded = assetManager.loadAsset(key);
        } catch (RuntimeException exception) {
            if (diagnose) {
                exception.printStackTrace();
            }
            loaded = null;
        }

        return loaded;
    }

    /**
     * Parse a color from the specified text string. TODO use heart library
     *
     * @param textString input text (not null, not empty)
     * @return a new color instance, or null if text is invalid
     */
    public static ColorRGBA parseColor(String textString) {
        Validate.nonEmpty(textString, "text string");

        ColorRGBA result = null;
        Matcher matcher = colorPattern.matcher(textString);
        boolean valid = matcher.matches();
        if (valid) {
            String rText = matcher.group(1);
            float r = Float.parseFloat(rText);
            String gText = matcher.group(2);
            float g = Float.parseFloat(gText);
            String bText = matcher.group(3);
            float b = Float.parseFloat(bText);
            String aText = matcher.group(4);
            float a = Float.parseFloat(aText);
            result = new ColorRGBA(r, g, b, a);
        }

        return result;
    }

    /**
     * Parse a material parameter from the specified text string.
     *
     * @param oldParameter old parameter (not null, unaffected)
     * @param textString input text (not null, not empty)
     * @return a new object or null
     */
    public static Object parseMatParam(MatParam oldParameter,
            String textString) {
        Validate.nonNull(oldParameter, "old parameter");
        Validate.nonEmpty(textString, "text string");

        String lcText = textString.toLowerCase(Locale.ROOT);
        Matcher matcher = nullPattern.matcher(lcText);
        Object result = null;
        if (!matcher.matches()) {
            VarType varType = oldParameter.getVarType();
            switch (varType) {
                case Boolean:
                    result = Boolean.parseBoolean(lcText);
                    break;

                case Float:
                    result = Float.parseFloat(lcText);
                    break;

                case Int:
                    result = Integer.parseInt(lcText);
                    break;

                case Vector2:
                case Vector3:
                    result = VectorDialog.parseVector(lcText);
                    break;

                case Vector4:
                    result = VectorDialog.parseVector(lcText);
                    Vector4f v = (Vector4f) result;
                    Object oldValue = oldParameter.getValue();
                    if (oldValue instanceof Quaternion) {
                        result = new Quaternion(v.x, v.y, v.z, v.w);
                    } else if (!(oldValue instanceof Vector4f)) {
                        /*
                         * best guess for oldValue == null
                         * If we guess wrong, there's a delayed cast exception.
                         */
                        result = new ColorRGBA(v.x, v.y, v.z, v.w);
                    }
                    break;

                default: // TODO more types
                    throw new IllegalArgumentException();
            }
        }

        return result;
    }

    /**
     * Parse a Vector3f from the specified text string. TODO use heart library
     *
     * @param textString input text (not null, not empty)
     * @return a new vector, or null if the text is invalid
     */
    public static Vector3f parseVector3f(String textString) {
        Validate.nonEmpty(textString, "text string");

        Vector3f result = null;
        Matcher matcher = vector3fPattern.matcher(textString);
        boolean valid = matcher.matches();
        if (valid) {
            String xText = matcher.group(1);
            float x = Float.parseFloat(xText);
            String yText = matcher.group(2);
            float y = Float.parseFloat(yText);
            String zText = matcher.group(3);
            float z = Float.parseFloat(zText);
            result = new Vector3f(x, y, z);
        }

        return result;
    }

    /**
     * Calculate the sensitivity of the indexed vertex to translations of the
     * indexed bone in the specified pose.
     *
     * @param boneIndex which bone to translate (&ge;0)
     * @param geometry (not null)
     * @param vertexIndex index into the geometry's vertices (&ge;0)
     * @param pose (not null, unaffected)
     * @param storeResult (modified if not null)
     * @return sensitivity matrix (either storeResult or a new instance)
     */
    public static Matrix3f sensitivity(int boneIndex, Geometry geometry,
            int vertexIndex, Pose pose, Matrix3f storeResult) {
        Validate.nonNull(geometry, "geometry");
        if (storeResult == null) {
            storeResult = new Matrix3f();
        }

        Vector3f testWorld = new Vector3f();
        Vector3f baseWorld = new Vector3f();
        int numBones = pose.countBones();
        Matrix4f[] matrices = new Matrix4f[numBones];
        Pose testPose = pose.clone();

        pose.userTranslation(boneIndex, testWorld);
        testPose.skin(matrices);
        MyMesh.vertexWorldLocation(geometry, vertexIndex, matrices, baseWorld);

        pose.userTranslation(boneIndex, testWorld);
        testWorld.addLocal(xAxis);
        testPose.setTranslation(boneIndex, testWorld);
        testPose.skin(matrices);
        MyMesh.vertexWorldLocation(geometry, vertexIndex, matrices, testWorld);
        testWorld.subtractLocal(baseWorld);
        storeResult.setColumn(0, testWorld);

        pose.userTranslation(boneIndex, testWorld);
        testWorld.addLocal(yAxis);
        testPose.setTranslation(boneIndex, testWorld);
        testPose.skin(matrices);
        MyMesh.vertexWorldLocation(geometry, vertexIndex, matrices, testWorld);
        testWorld.subtractLocal(baseWorld);
        storeResult.setColumn(1, testWorld);

        pose.userTranslation(boneIndex, testWorld);
        testWorld.addLocal(zAxis);
        testPose.setTranslation(boneIndex, testWorld);
        testPose.skin(matrices);
        MyMesh.vertexWorldLocation(geometry, vertexIndex, matrices, testWorld);
        testWorld.subtractLocal(baseWorld);
        storeResult.setColumn(2, testWorld);

        return storeResult;
    }

    /**
     * Calculate the rotation specified by a bank of sliders when the rotation
     * display mode is QuatCoeff.
     *
     * @param sliderPositions (not null, length = 3)
     * @param storeResult (modified if not null)
     * @return rotation (either storeResult or a new quaternion)
     */
    public static Quaternion setFromSliders(float[] sliderPositions,
            Quaternion storeResult) {
        int numSliders = sliderPositions.length;
        Validate.inRange(numSliders, "numSliders", 3, 3);
        Quaternion result;
        if (storeResult == null) {
            result = new Quaternion();
        } else {
            result = storeResult;
        }

        double x = sliderPositions[0] / 3.142;
        double y = sliderPositions[1] / 3.142;
        double z = sliderPositions[2] / 1.571;
        double ssq = x * x + y * y + z * z;
        if (ssq > 1.0) {
            result.set((float) x, (float) y, (float) z, 0f);
            result.normalizeLocal();
        } else {
            double w = Math.sqrt(1.0 - ssq);
            result.set((float) x, (float) y, (float) z, (float) w);
        }

        return result;
    }

    /**
     * Write an editor action to the specified writer.
     *
     * @param writer (not null)
     * @param actionString (not null)
     * @throws java.io.IOException if an I/O error occurs while writing
     */
    public static void writePerformAction(Writer writer, String actionString)
            throws IOException {
        Validate.nonNull(actionString, "action string");

        writer.write("Maud.perform('");
        writer.write(actionString);
        writer.write("');\n");
    }
}
