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
import com.jme3.math.Matrix3f;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
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
import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyMesh;
import jme3utilities.MySpatial;
import jme3utilities.Validate;
import jme3utilities.math.MyQuaternion;
import jme3utilities.wes.Pose;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for the Maud application. All methods should be static.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class Util {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(Util.class.getName());
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
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private Util() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Describe a anti-aliasing factor.
     *
     * @param numSamples samples per pixel (&ge;0, &le;16)
     * @return textual description
     */
    public static String aaDescription(int numSamples) {
        String aaDescription;
        if (numSamples <= 1) {
            aaDescription = "disabled";
        } else {
            aaDescription = String.format("%dx", numSamples);
        }

        return aaDescription;
    }

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
            if (mesh.isAnimated()) {
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
     * Cancel the attachments node (if any) of the specified bone. The invoker
     * is responsible for removing the node from the scene graph.
     *
     * @param bone which bone (not null, modified)
     */
    public static void cancelAttachments(Bone bone) {
        Class<?> boneClass = bone.getClass();
        Field attachNodeField;
        try {
            attachNodeField = boneClass.getDeclaredField("attachNode");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException();
        }
        attachNodeField.setAccessible(true);
        try {
            attachNodeField.set(bone, null);
        } catch (IllegalAccessException e) {
            throw new RuntimeException();
        }
    }

    /**
     * Find a cardinal quaternion similar to the specified input. A cardinal
     * quaternion is one for which the rotation angles on all 3 axes are integer
     * multiples of Pi/2 radians.
     *
     * @param input (not null, modified)
     */
    public static void cardinalizeLocal(Quaternion input) {
        Validate.nonNull(input, "input");

        MyQuaternion.snapLocal(input, 0);
        MyQuaternion.snapLocal(input, 1);
        MyQuaternion.snapLocal(input, 2);
    }

    /**
     * Test whether the specified subtree of a scene graph contains any of the
     * collected spatials.
     *
     * @param subtree subtree to traverse (may be null, unaffected)
     * @param collection spatials to find (not null, unaffected)
     * @return true if one of the collected spatials was found, otherwise false
     */
    public static boolean descendentsInclude(Spatial subtree,
            Collection<Spatial> collection) {
        boolean result;
        if (subtree == null) {
            result = false;
        } else if (collection.isEmpty()) {
            result = false;
        } else if (collection.contains(subtree)) {
            result = true;
        } else if (subtree instanceof Node) {
            Node node = (Node) subtree;
            result = false;
            for (Spatial spatial : collection) {
                result = spatial.hasAncestor(node);
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
     * Find the specified spatial in the specified subtree and optionally store
     * its tree position. Note: recursive!
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
        if (subtree != null) {
            if (subtree.equals(spatial)) {
                success = true;
                if (storePosition != null) {
                    storePosition.clear();
                }

            } else if (subtree instanceof Node) {
                Node node = (Node) subtree;
                List<Spatial> children = node.getChildren();
                int numChildren = children.size();
                for (int childI = 0; childI < numChildren; childI++) {
                    Spatial child = children.get(childI);
                    success = findPosition(spatial, child, storePosition);
                    if (success) {
                        if (storePosition != null) {
                            storePosition.add(0, childI);
                        }
                        break;
                    }
                }
            }
        }

        return success;
    }

    /**
     * Access the indexed SpatialTrack in the specified animation.
     *
     * @param animation which animation to search (not null, unaffected)
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
        Validate.nonNull(geometry, "geometry");
        Validate.nonNull(skinningMatrices, "skinning matrices");
        Validate.nonNull(storeLocation, "store location");

        int bestIndex = -1;
        float bestY = Float.POSITIVE_INFINITY;

        Vector3f meshLoc = new Vector3f();
        Vector3f worldLoc = new Vector3f();

        Mesh mesh = geometry.getMesh();
        int maxWeightsPerVertex = mesh.getMaxNumWeights();

        VertexBuffer posBuf;
        posBuf = mesh.getBuffer(VertexBuffer.Type.BindPosePosition);
        FloatBuffer posBuffer = (FloatBuffer) posBuf.getDataReadOnly();
        posBuffer.rewind();

        VertexBuffer wBuf = mesh.getBuffer(VertexBuffer.Type.BoneWeight);
        FloatBuffer weightBuffer = (FloatBuffer) wBuf.getDataReadOnly();
        weightBuffer.rewind();

        VertexBuffer biBuf = mesh.getBuffer(VertexBuffer.Type.BoneIndex);
        Buffer boneIndexBuffer = biBuf.getData();
        boneIndexBuffer.rewind();

        int numVertices = posBuffer.remaining() / 3;
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

            geometry.localToWorld(meshLoc, worldLoc);
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
     * Access the attachments node of the specified bone.
     *
     * @param bone which bone (not null, unaffected)
     * @return the pre-existing instance, or null if none
     */
    public static Node getAttachments(Bone bone) {
        Class<?> boneClass = bone.getClass();
        Field attachNodeField;
        try {
            attachNodeField = boneClass.getDeclaredField("attachNode");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException();
        }
        attachNodeField.setAccessible(true);
        Node result;
        try {
            result = (Node) attachNodeField.get(bone);
        } catch (IllegalAccessException e) {
            throw new RuntimeException();
        }

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
     * Test whether the specified spatial is an "extra" spatial.
     *
     * A spatial is "extra" iff neither it nor any of its descendents:
     * <li> is an attachments node
     * <li> has a scene-graph control
     * <li> has user data, or
     * <li> has mesh vertices.
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
                = Util.descendentsInclude(spatial, attachmentsNodes);
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
     * Enumerate all animated meshes in the specified subtree of a scene graph.
     * Note: recursive!
     *
     * @param subtree (not null, unaffected)
     * @param storeResult (added to if not null)
     * @return an expanded list (either storeResult or a new instance)
     */
    public static List<Mesh> listAnimatedMeshes(Spatial subtree,
            List<Mesh> storeResult) {
        Validate.nonNull(subtree, "subtree");
        if (storeResult == null) {
            storeResult = new ArrayList<>(10);
        }

        if (subtree instanceof Geometry) {
            Geometry geometry = (Geometry) subtree;
            Mesh mesh = geometry.getMesh();
            if (mesh.isAnimated()) {
                storeResult.add(mesh);
            }

        } else if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                listAnimatedMeshes(child, storeResult);
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
     * Map all attachments in the specified skeleton.
     *
     * @param skeleton (not null, unaffected)
     * @param storeResult (added to if not null)
     * @return an expanded map (either storeResult or a new instance)
     */
    public static Map<Bone, Spatial> mapAttachments(Skeleton skeleton,
            Map<Bone, Spatial> storeResult) {
        Validate.nonNull(skeleton, "skeleton");
        if (storeResult == null) {
            storeResult = new HashMap<>(4);
        }

        int numBones = skeleton.getBoneCount();
        for (int boneIndex = 0; boneIndex < numBones; boneIndex++) {
            Bone bone = skeleton.getBone(boneIndex);
            Node attachmentsNode = getAttachments(bone);
            if (attachmentsNode != null) {
                if (storeResult.containsKey(bone)) {
                    if (storeResult.get(bone) != attachmentsNode) {
                        throw new IllegalStateException();
                    }
                } else {
                    storeResult.put(bone, attachmentsNode);
                }
            }

        }

        return storeResult;
    }

    /**
     * Map all attachments nodes in the specified subtree of a scene graph.
     *
     * @param subtree (not null, unaffected)
     * @param storeResult (added to if not null)
     * @return an expanded map (either storeResult or a new instance)
     */
    public static Map<Bone, Spatial> mapAttachments(Spatial subtree,
            Map<Bone, Spatial> storeResult) {
        Validate.nonNull(subtree, "subtree");
        if (storeResult == null) {
            storeResult = new HashMap<>(4);
        }

        List<SkeletonControl> list
                = MySpatial.listControls(subtree, SkeletonControl.class, null);
        for (SkeletonControl control : list) {
            Skeleton skeleton = control.getSkeleton();
            Util.mapAttachments(skeleton, storeResult);
        }

        return storeResult;
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
        Validate.nonNull(pose, "pose");
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
}
