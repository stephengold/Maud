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
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.animation.Track;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.asset.ModelKey;
import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.bounding.BoundingVolume;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.PhysicsControl;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.Matrix3f;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.control.Control;
import com.jme3.scene.plugins.blender.meshes.Face;
import com.jme3.scene.plugins.bvh.BVHAnimData;
import com.jme3.scene.plugins.bvh.BoneMapping;
import com.jme3.scene.plugins.bvh.SkeletonMapping;
import com.jme3.scene.plugins.ogre.MaterialLoader;
import com.jme3.scene.plugins.ogre.MeshLoader;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyAnimation;
import jme3utilities.Validate;
import jme3utilities.math.MyQuaternion;
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
     * local copy of {@link com.jme3.math.Vector3f#UNIT_XYZ}
     */
    final private static Vector3f scaleIdentity = new Vector3f(1f, 1f, 1f);
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
        assert maxWeightsPerVert > 0 : maxWeightsPerVert;
        assert maxWeightsPerVert <= 4 : maxWeightsPerVert;

        VertexBuffer biBuf = mesh.getBuffer(VertexBuffer.Type.BoneIndex);
        ByteBuffer boneIndexBuffer = (ByteBuffer) biBuf.getDataReadOnly();
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
                byte bIndex = boneIndexBuffer.get();
                if (wIndex < maxWeightsPerVert && weight > 0f) {
                    int boneIndex = 0xff & bIndex;
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
     * Calculate the bone transform for the specified track and time, using the
     * current techniques.
     *
     * @param track input (not null, unaffected)
     * @param time animation time input (in seconds)
     * @param duration (in seconds)
     * @param storeResult (modified if not null)
     * @return a transform (either storeResult or a new instance)
     */
    public static Transform boneTransform(BoneTrack track, float time,
            float duration, Transform storeResult) {
        if (storeResult == null) {
            storeResult = new Transform();
        }
        float[] times = track.getKeyFrameTimes();
        int lastFrame = times.length - 1;
        assert lastFrame >= 0 : lastFrame;

        Vector3f[] translations = track.getTranslations();
        Quaternion[] rotations = track.getRotations();
        Vector3f[] scales = track.getScales();

        if (time <= 0f || lastFrame == 0) {
            /*
             * Copy the transform of the first frame.
             */
            storeResult.setTranslation(translations[0]);
            storeResult.setRotation(rotations[0]);
            if (scales == null) {
                storeResult.setScale(scaleIdentity);
            } else {
                storeResult.setScale(scales[0]);
            }

        } else {
            /*
             * Interpolate between frames.
             */
            Maud.getModel().getMisc().interpolate(time, times, duration,
                    translations, rotations, scales, storeResult);
        }

        return storeResult;
    }

    /**
     * Find a cardinal quaternion similar to the specified input. A cardinal
     * quaternion is one for which the rotations angles on all 3 axes are
     * integer multiples of Pi/2 radians.
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
     * Count how many scene-graph controls are contained in the specified
     * subtree. Note: recursive!
     *
     * @param subtree subtree to traverse (may be null)
     * @return count of vertices (&ge;0)
     */
    public static int countSgcs(Spatial subtree) {
        int result = 0;
        if (subtree != null) {
            result += subtree.getNumControls();
        }
        if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                result += countSgcs(child);
            }
        }

        assert result >= 0 : result;
        return result;
    }

    /**
     * Count how many spatials are contained in the specified subtree. Note:
     * recursive!
     *
     * @param subtree subtree to traverse (may be null)
     * @return count of spatials (&ge;0)
     */
    public static int countSpatials(Spatial subtree) {
        int result = 0;
        if (subtree != null) {
            ++result;
        }
        if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                result += countSpatials(child);
            }
        }

        assert result >= 0 : result;
        return result;
    }

    /**
     * Count how many user data are contained in the specified subtree. Note:
     * recursive!
     *
     * @param subtree subtree to traverse (may be null)
     * @return count of vertices (&ge;0)
     */
    public static int countUserData(Spatial subtree) {
        int result = 0;
        if (subtree != null) {
            Collection<String> keys = subtree.getUserDataKeys();
            result += keys.size();
        }
        if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                result += countUserData(child);
            }
        }

        assert result >= 0 : result;
        return result;
    }

    /**
     * Count how many mesh vertices are contained in the specified subtree.
     * Note: recursive!
     *
     * @param subtree subtree to traverse (may be null)
     * @return count of vertices (&ge;0)
     */
    public static int countVertices(Spatial subtree) {
        int result = 0;
        if (subtree instanceof Geometry) {
            Geometry geometry = (Geometry) subtree;
            Mesh mesh = geometry.getMesh();
            result = mesh.getVertexCount();

        } else if (subtree instanceof Node) {
            Node node = (Node) subtree;
            Collection<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                result += countVertices(child);
            }
        }

        assert result >= 0 : result;
        return result;
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
        ByteBuffer boneIndexBuffer = (ByteBuffer) biBuf.getData();
        boneIndexBuffer.rewind();

        int numVertices = posBuffer.remaining() / 3;
        for (int vertexIndex = 0; vertexIndex < numVertices; vertexIndex++) {
            float bx = posBuffer.get(); // bind position
            float by = posBuffer.get();
            float bz = posBuffer.get();

            meshLoc.zero();
            for (int wIndex = 0; wIndex < maxWeightsPerVertex; wIndex++) {
                float weight = weightBuffer.get();
                int boneIndex = 0xff & boneIndexBuffer.get();
                if (weight != 0f) {
                    Matrix4f s = skinningMatrices[boneIndex];
                    meshLoc.x += weight
                            * (s.m00 * bx + s.m01 * by + s.m02 * bz + s.m03);
                    meshLoc.y += weight
                            * (s.m10 * bx + s.m11 * by + s.m12 * bz + s.m13);
                    meshLoc.z += weight
                            * (s.m20 * bx + s.m21 * by + s.m22 * bz + s.m23);
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
                boneIndexBuffer.get();
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
     * Test whether there are any "extra" spatials in the specified subtree.
     * Note: recursive!
     *
     * @param subtree subtree to traverse (may be null)
     * @return true if any found, otherwise false
     */
    public static boolean hasExtraSpatials(Spatial subtree) {
        if (countSgcs(subtree) == 0
                && countUserData(subtree) == 0
                && countVertices(subtree) == 0) {
            return true;
        }

        if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                boolean hasExtras = hasExtraSpatials(child);
                if (hasExtras) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Join a list of texts using separators, ignoring any nulls. Note that Java
     * 8 provides
     * {@link java.lang.String#join(java.lang.CharSequence, java.lang.Iterable)}.
     *
     * @param list texts to join (not null, unaffected, may contain nulls)
     * @param separator string (not null)
     * @return joined string
     */
    public static String join(CharSequence separator, Iterable<String> list) {
        Validate.nonNull(separator, "separator");
        Validate.nonNull(list, "list");

        StringBuilder result = new StringBuilder(80);
        for (String item : list) {
            if (item != null) {
                if (result.length() > 0) {
                    /*
                     * Append a separator.
                     */
                    result.append(separator);
                }
                result.append(item);
            }
        }

        return result.toString();
    }

    /**
     * Load a BVH asset as a CG model without logging any warning/error
     * messages.
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
     * Load a CG model asset without logging any warning/error messages.
     *
     * @param assetManager asset manager
     * @param key key for CG model asset
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

        Logger materialLoaderLogger = Logger.getLogger(
                MaterialLoader.class.getName());
        Level materialLoaderLevel = materialLoaderLogger.getLevel();

        Logger compoundCollisionShapeLogger = Logger.getLogger(
                CompoundCollisionShape.class.getName());
        Level compoundCollisionShapeLevel;
        compoundCollisionShapeLevel = compoundCollisionShapeLogger.getLevel();

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
     * Create a collision shape suitable for the specified spatial.
     *
     * @param spatial (not null)
     * @return a new instance
     */
    public static CollisionShape makeShape(Spatial spatial) {
        CollisionShape childShape;
        BoundingVolume bound = spatial.getWorldBound();
        if (bound instanceof BoundingBox) {
            BoundingBox boundingBox = (BoundingBox) bound;
            float xHalfExtent = boundingBox.getXExtent();
            float yHalfExtent = boundingBox.getYExtent();
            float zHalfExtent = boundingBox.getZExtent();
            // TODO consider other possible axes for the capsule
            float radius = Math.max(xHalfExtent, zHalfExtent);
            if (yHalfExtent > radius) {
                float height = 2 * (yHalfExtent - radius);
                childShape = new CapsuleCollisionShape(radius, height);
            } else {
                childShape = new SphereCollisionShape(yHalfExtent);
            }
        } else if (bound instanceof BoundingSphere) {
            BoundingSphere boundingSphere = (BoundingSphere) bound;
            float radius = boundingSphere.getRadius();
            childShape = new SphereCollisionShape(radius);
        } else {
            throw new IllegalStateException();
        }
        CompoundCollisionShape result = new CompoundCollisionShape();
        Vector3f location = bound.getCenter();
        Vector3f translation = spatial.getWorldTranslation();
        location.subtractLocal(translation);
        // TODO account for rotation
        result.addChildShape(childShape, location);

        return result;
    }

    /**
     * Find the SGC in the specified position among physics controls in the
     * specified spatial.
     *
     * @param spatial which spatial to scan (not null)
     * @param position position index (&ge;0)
     * @return the pre-existing physics control instance (not null)
     */
    public static PhysicsControl pcFromPosition(Spatial spatial, int position) {
        Validate.nonNegative(position, "position");

        int numSgcs = spatial.getNumControls();
        int pcCount = 0;
        for (int controlIndex = 0; controlIndex < numSgcs; controlIndex++) {
            Control control = spatial.getControl(controlIndex);
            if (control instanceof PhysicsControl) {
                if (pcCount == position) {
                    return (PhysicsControl) control;
                }
                ++pcCount;
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * Calculate the position of the specified SGC among the physics controls in
     * the specified spatial.
     *
     * @param spatial which spatial to scan (not null)
     * @param pc (a control added to that spatial)
     * @return position index (&ge;0)
     */
    public static int pcToPosition(Spatial spatial, PhysicsControl pc) {
        int numSgcs = spatial.getNumControls();
        int result = 0;
        for (int controlIndex = 0; controlIndex < numSgcs; controlIndex++) {
            Control control = spatial.getControl(controlIndex);
            if (control instanceof PhysicsControl) {
                if (control == pc) {
                    return result;
                }
                ++result;
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * Clear all cached collision data from the specified subtree of the scene
     * graph and force a bound refresh. Note: recursive!
     *
     * @param subtree where to search (may be null)
     */
    public static void prepareForCollide(Spatial subtree) {
        if (subtree instanceof Geometry) {
            Geometry geometry = (Geometry) subtree;
            geometry.updateModelBound();
            Mesh mesh = geometry.getMesh();
            mesh.clearCollisionData();

        } else if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                prepareForCollide(child);
            }
        }
    }

    /**
     * Copy a bone track, resampling it at the specified rate.
     *
     * @param oldTrack (not null, unaffected)
     * @param sampleRate sample rate (in frames per second, &gt;0)
     * @param duration animation duration (in seconds, &ge;0)
     * @return a new instance
     */
    public static Track resample(BoneTrack oldTrack, float sampleRate,
            float duration) {
        Validate.positive(sampleRate, "sample rate");
        Validate.nonNegative(duration, "duration");

        int boneIndex = oldTrack.getTargetBoneIndex();
        Vector3f[] oldScales = oldTrack.getScales();
        int newCount = 1 + (int) Math.floor(duration * sampleRate);
        Vector3f[] newTranslations = new Vector3f[newCount];
        Quaternion[] newRotations = new Quaternion[newCount];
        Vector3f[] newScales = null;
        if (oldScales != null) {
            newScales = new Vector3f[newCount];
        }
        float[] newTimes = new float[newCount];

        for (int frameIndex = 0; frameIndex < newCount; frameIndex++) {
            float time = frameIndex / sampleRate;

            Transform boneTransform;
            boneTransform = boneTransform(oldTrack, time, duration, null);
            newTranslations[frameIndex] = boneTransform.getTranslation();
            newRotations[frameIndex] = boneTransform.getRotation();
            if (oldScales != null) {
                newScales[frameIndex] = boneTransform.getScale();
            }
            newTimes[frameIndex] = time;
        }

        BoneTrack result = MyAnimation.newBoneTrack(boneIndex, newTimes,
                newTranslations, newRotations, newScales);

        return result;

    }

    /**
     * Re-target the specified animation from the specified source skeleton to
     * the specified target skeleton using the specified map.
     *
     * @param sourceAnimation which animation to re-target (not null,
     * unaffected)
     * @param sourceSkeleton (not null, unaffected)
     * @param targetSkeleton (not null, unaffected)
     * @param map which skeleton map to use (not null, unaffected)
     * @param animationName name for the resulting animation (not null)
     * @return a new animation
     */
    public static Animation retargetAnimation(Animation sourceAnimation,
            Skeleton sourceSkeleton, Skeleton targetSkeleton,
            SkeletonMapping map, String animationName) {
        Validate.nonNull(sourceAnimation, "source animation");
        Validate.nonNull(sourceSkeleton, "source skeleton");
        Validate.nonNull(targetSkeleton, "target skeleton");
        Validate.nonNull(map, "map");
        Validate.nonNull(animationName, "animation name");
        /*
         * Start with an empty animation.
         */
        float duration = sourceAnimation.getLength();
        Animation result = new Animation(animationName, duration);
        /*
         * Add a bone track for each target bone that's mapped.
         */
        int numTargetBones = targetSkeleton.getBoneCount();
        for (int iTarget = 0; iTarget < numTargetBones; iTarget++) {
            Bone targetBone = targetSkeleton.getBone(iTarget);
            String targetName = targetBone.getName();
            BoneMapping boneMapping = map.get(targetName);
            if (boneMapping != null) {
                String sourceName = boneMapping.getSourceName();
                int iSource = sourceSkeleton.getBoneIndex(sourceName);
                BoneTrack sourceTrack;
                sourceTrack = MyAnimation.findTrack(sourceAnimation, iSource);
                BoneTrack track = retargetTrack(sourceAnimation, sourceTrack,
                        sourceSkeleton, targetSkeleton, map, iTarget);
                result.addTrack(track);
            }
        }

        return result;
    }

    /**
     * Re-target the specified bone track from the specified source skeleton to
     * the specified target skeleton using the specified map.
     *
     * @param sourceAnimation the animation to re-target, or null for bind pose
     * @param sourceSkeleton (not null, unaffected)
     * @param sourceTrack input bone track (not null, unaffected)
     * @param targetSkeleton (not null, unaffected)
     * @param map which skeleton map to use (not null, unaffected)
     * @param targetBoneIndex index of the target bone (&ge;0)
     * @return a new bone track
     */
    public static BoneTrack retargetTrack(Animation sourceAnimation,
            BoneTrack sourceTrack, Skeleton sourceSkeleton,
            Skeleton targetSkeleton, SkeletonMapping map,
            int targetBoneIndex) {
        Validate.nonNull(sourceSkeleton, "source skeleton");
        Validate.nonNull(targetSkeleton, "target skeleton");
        Validate.nonNull(map, "map");
        Validate.nonNegative(targetBoneIndex, "target bone index");

        float[] times;
        int numKeyframes;
        if (sourceTrack == null) {
            numKeyframes = 1;
            times = new float[numKeyframes];
            times[0] = 0f;
        } else {
            times = sourceTrack.getKeyFrameTimes();
            numKeyframes = times.length;
        }
        Vector3f[] translations = new Vector3f[numKeyframes];
        Quaternion[] rotations = new Quaternion[numKeyframes];
        Vector3f[] scales = new Vector3f[numKeyframes];
        Pose sourcePose = new Pose(sourceSkeleton);
        Pose targetPose = new Pose(targetSkeleton);

        for (int frameIndex = 0; frameIndex < numKeyframes; frameIndex++) {
            float trackTime = times[frameIndex];
            sourcePose.setToAnimation(sourceAnimation, trackTime);
            targetPose.setToRetarget(sourcePose, map);

            Transform userTransform;
            userTransform = targetPose.userTransform(targetBoneIndex, null);
            translations[frameIndex] = userTransform.getTranslation();
            rotations[frameIndex] = userTransform.getRotation();
            scales[frameIndex] = userTransform.getScale();
        }

        BoneTrack result = new BoneTrack(targetBoneIndex, times, translations,
                rotations, scales);

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
        vertexWorldLocation(geometry, vertexIndex, matrices, baseWorld);

        pose.userTranslation(boneIndex, testWorld);
        testWorld.addLocal(xAxis);
        testPose.setTranslation(boneIndex, testWorld);
        testPose.skin(matrices);
        vertexWorldLocation(geometry, vertexIndex, matrices, testWorld);
        testWorld.subtractLocal(baseWorld);
        storeResult.setColumn(0, testWorld);

        pose.userTranslation(boneIndex, testWorld);
        testWorld.addLocal(yAxis);
        testPose.setTranslation(boneIndex, testWorld);
        testPose.skin(matrices);
        vertexWorldLocation(geometry, vertexIndex, matrices, testWorld);
        testWorld.subtractLocal(baseWorld);
        storeResult.setColumn(1, testWorld);

        pose.userTranslation(boneIndex, testWorld);
        testWorld.addLocal(zAxis);
        testPose.setTranslation(boneIndex, testWorld);
        testPose.skin(matrices);
        vertexWorldLocation(geometry, vertexIndex, matrices, testWorld);
        testWorld.subtractLocal(baseWorld);
        storeResult.setColumn(2, testWorld);

        return storeResult;
    }

    /**
     * Alter the wireframe render setting on all materials in the specified
     * subtree. Note: recursive!
     *
     * @param subtree (may be null)
     * @param newSetting true&rarr;enable wireframe mode, false&rarr;disable it
     */
    public static void setWireframe(Spatial subtree, boolean newSetting) {
        if (subtree instanceof Geometry) {
            Geometry geometry = (Geometry) subtree;
            Material material = geometry.getMaterial();
            RenderState rs = material.getAdditionalRenderState();
            rs.setWireframe(newSetting);

        } else if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                setWireframe(child, newSetting);
            }
        }
    }

    /**
     * Read a data vector for the indexed vertex in a mesh.
     *
     * @param mesh subject mesh (not null)
     * @param bufferType which buffer to read (6 legal values)
     * @param vertexIndex index into the mesh's vertices (&ge;0)
     * @param storeResult (modified if not null)
     * @return mesh coordinates (either storeResult or a new instance)
     */
    public static Vector3f vertexMeshVector3f(Mesh mesh,
            VertexBuffer.Type bufferType, int vertexIndex,
            Vector3f storeResult) {
        Validate.nonNull(mesh, "mesh");
        assert bufferType == VertexBuffer.Type.BindPoseNormal
                || bufferType == VertexBuffer.Type.BindPosePosition
                || bufferType == VertexBuffer.Type.BindPoseTangent
                || bufferType == VertexBuffer.Type.Normal
                || bufferType == VertexBuffer.Type.Position
                || bufferType == VertexBuffer.Type.Tangent : bufferType;
        Validate.nonNegative(vertexIndex, "vertex index");
        if (storeResult == null) {
            storeResult = new Vector3f();
        }

        VertexBuffer vertexBuffer = mesh.getBuffer(bufferType);
        FloatBuffer floatBuffer = (FloatBuffer) vertexBuffer.getDataReadOnly();
        floatBuffer.position(3 * vertexIndex);
        storeResult.x = floatBuffer.get();
        storeResult.y = floatBuffer.get();
        storeResult.z = floatBuffer.get();

        return storeResult;
    }

    /**
     * Calculate the location of the indexed vertex in mesh space using the
     * skinning matrices provided.
     *
     * @param mesh subject mesh (not null)
     * @param vertexIndex index into the mesh's vertices (&ge;0)
     * @param skinningMatrices (not null, unaffected)
     * @param storeResult (modified if not null)
     * @return mesh coordinates (either storeResult or a new instance)
     */
    public static Vector3f vertexMeshLocation(Mesh mesh, int vertexIndex,
            Matrix4f[] skinningMatrices, Vector3f storeResult) {
        Validate.nonNull(mesh, "mesh");
        Validate.nonNegative(vertexIndex, "vertex index");
        Validate.nonNull(skinningMatrices, "skinning matrices");
        if (storeResult == null) {
            storeResult = new Vector3f();
        }

        Vector3f b = vertexMeshVector3f(mesh,
                VertexBuffer.Type.BindPosePosition, vertexIndex, null);

        VertexBuffer wBuf = mesh.getBuffer(VertexBuffer.Type.BoneWeight);
        FloatBuffer weightBuffer = (FloatBuffer) wBuf.getDataReadOnly();
        weightBuffer.position(4 * vertexIndex);

        VertexBuffer biBuf = mesh.getBuffer(VertexBuffer.Type.BoneIndex);
        ByteBuffer boneIndexBuffer = (ByteBuffer) biBuf.getDataReadOnly();
        boneIndexBuffer.position(4 * vertexIndex);

        storeResult.zero();
        int maxWeightsPerVertex = mesh.getMaxNumWeights();
        for (int wIndex = 0; wIndex < maxWeightsPerVertex; wIndex++) {
            float weight = weightBuffer.get();
            int boneIndex = 0xff & boneIndexBuffer.get();
            if (weight != 0f) {
                Matrix4f s = skinningMatrices[boneIndex];
                storeResult.x += weight
                        * (s.m00 * b.x + s.m01 * b.y + s.m02 * b.z + s.m03);
                storeResult.y += weight
                        * (s.m10 * b.x + s.m11 * b.y + s.m12 * b.z + s.m13);
                storeResult.z += weight
                        * (s.m20 * b.x + s.m21 * b.y + s.m22 * b.z + s.m23);
            }
        }

        return storeResult;
    }

    /**
     * Calculate the location of the indexed vertex in world space using the
     * skinning matrices provided.
     *
     * @param geometry an animated geometry (not null)
     * @param vertexIndex index into the geometry's vertices (&ge;0)
     * @param skinningMatrices (not null, unaffected)
     * @param storeResult (modified if not null)
     * @return world coordinates (either storeResult or a new instance)
     */
    public static Vector3f vertexWorldLocation(Geometry geometry,
            int vertexIndex, Matrix4f[] skinningMatrices,
            Vector3f storeResult) {
        Validate.nonNull(geometry, "geometry");
        Validate.nonNegative(vertexIndex, "vertex index");
        Validate.nonNull(skinningMatrices, "skinning matrices");

        Mesh mesh = geometry.getMesh();

        Vector3f meshLocation = new Vector3f();
        vertexMeshLocation(mesh, vertexIndex, skinningMatrices, meshLocation);
        storeResult = geometry.localToWorld(meshLocation, storeResult);

        return storeResult;
    }
}
