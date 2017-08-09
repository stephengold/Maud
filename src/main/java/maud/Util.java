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
import com.jme3.asset.AssetLoadException;
import com.jme3.asset.AssetManager;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.asset.ModelKey;
import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.bounding.BoundingVolume;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.PhysicsControl;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.jme3.scene.plugins.blender.meshes.Face;
import com.jme3.scene.plugins.bvh.BVHAnimData;
import com.jme3.scene.plugins.bvh.BoneMapping;
import com.jme3.scene.plugins.bvh.SkeletonMapping;
import com.jme3.scene.plugins.ogre.MaterialLoader;
import com.jme3.scene.plugins.ogre.MeshLoader;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import jme3utilities.MyAnimation;
import jme3utilities.MySkeleton;
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
     * Accumulate a linear combination of vectors.
     *
     * @param total sum of the scaled inputs so far (not null, updated)
     * @param input the vector to scale and add (not null, unaffected)
     * @param scale scale factor to apply to the input
     */
    public static void accumulateScaled(Vector3f total, Vector3f input,
            float scale) {
        Validate.nonNull(total, "total");
        Validate.nonNull(input, "input");

        total.x += input.x * scale;
        total.y += input.y * scale;
        total.z += input.z * scale;
    }

    /**
     * Copy a bone track, deleting everything before the specified time, and
     * making that the start of the animation.
     *
     * @param oldTrack (not null, unaffected)
     * @param neckTime cutoff time (in seconds, &gt;0)
     * @param oldDuration (in seconds, &ge;neckTime)
     * @return a new instance
     */
    public static BoneTrack behead(BoneTrack oldTrack, float neckTime,
            float oldDuration) {
        Validate.positive(neckTime, "neck time");

        float[] oldTimes = oldTrack.getKeyFrameTimes();
        Vector3f[] oldTranslations = oldTrack.getTranslations();
        Quaternion[] oldRotations = oldTrack.getRotations();
        Vector3f[] oldScales = oldTrack.getScales();
        int oldCount = oldTimes.length;

        int neckIndex;
        neckIndex = MyAnimation.findPreviousKeyframeIndex(oldTrack, neckTime);
        int newCount = oldCount - neckIndex;
        Vector3f[] translations = new Vector3f[newCount];
        Quaternion[] rotations = new Quaternion[newCount];
        Vector3f[] scales = null;
        if (oldScales != null) {
            scales = new Vector3f[newCount];
        }
        float[] times = new float[newCount];

        Transform user = Maud.model.misc.interpolate(neckTime, oldTimes,
                oldDuration, oldTranslations, oldRotations, oldScales, null);
        translations[0] = user.getTranslation();
        rotations[0] = user.getRotation();
        if (scales != null) {
            scales[0] = user.getScale();
        }
        times[0] = 0f;
        for (int newIndex = 1; newIndex < newCount; newIndex++) {
            int oldIndex = newIndex + neckIndex;
            translations[newIndex] = oldTranslations[oldIndex].clone();
            rotations[newIndex] = oldRotations[oldIndex].clone();
            if (scales != null) {
                scales[newIndex] = oldScales[oldIndex].clone();
            }
            times[newIndex] = oldTimes[oldIndex] - neckTime;
        }

        int boneIndex = oldTrack.getTargetBoneIndex();
        BoneTrack result = new BoneTrack(boneIndex, times, translations,
                rotations, scales);

        return result;
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
            Maud.model.misc.interpolate(time, times, duration, translations,
                    rotations, scales, storeResult);
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
     * Count how many vertices in the specified subtree of the scene graph are
     * directly influenced by the indexed bone. Note: recursive!
     *
     * @param subtree subtree to traverse (may be null)
     * @param boneIndex which bone (&ge;0)
     * @return count of vertices (&ge;0)
     */
    public static int directInfluence(Spatial subtree, int boneIndex) {
        Validate.nonNegative(boneIndex, "bone index");

        int result = 0;
        if (subtree instanceof Geometry) {
            Geometry geometry = (Geometry) subtree;
            Mesh mesh = geometry.getMesh();
            if (mesh.isAnimated()) {
                result = MySkeleton.numInfluenced(mesh, boneIndex);
            }

        } else if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                result += directInfluence(child, boneIndex);
            }
        }

        return result;
    }

    /**
     * Find the root bone in the specified skeleton that has the greatest
     * influence on the meshes in the specified subtree.
     *
     * @param subtree subtree to traverse (may be null)
     * @param skeleton skeleton (not null)
     * @return the pre-existing instance, or null if no root bone found
     */
    public static Bone dominantRootBone(Spatial subtree, Skeleton skeleton) {
        Bone result = null;
        Bone[] roots = skeleton.getRoots();
        if (roots.length == 1) {
            result = roots[0];

        } else if (subtree != null) {
            int maxInfluenced = -1;
            for (Bone rootBone : roots) {
                int boneIndex = skeleton.getBoneIndex(rootBone);
                int numInfluenced;
                numInfluenced = Util.influence(subtree, skeleton, boneIndex);
                if (numInfluenced > maxInfluenced) {
                    maxInfluenced = numInfluenced;
                    result = rootBone;
                }
            }
        }

        return result;
    }

    /**
     * Extract the 4th root of a double-precision value. This method is faster
     * than Math.pow(d, 0.25).
     *
     * @param dValue input 4th power to be extracted (&ge;0)
     * @return the positive 4th root of dValue (&ge;0)
     * @see java.lang.Math#cbrt(double)
     */
    public static double fourthRoot(double dValue) {
        Validate.nonNegative(dValue, "dValue");
        double sqrt = Math.sqrt(dValue);
        double result = Math.sqrt(sqrt);

        assert result >= 0.0 : result;
        return result;
    }

    /**
     * Count how many vertices in the specified subtree of the scene graph are
     * influenced by the indexed bone and its descendents. Note: recursive!
     *
     * @param subtree subtree to traverse (may be null)
     * @param skeleton skeleton (not null)
     * @param boneIndex which bone in the skeleton (&ge;0)
     * @return count of vertices (&ge;0)
     */
    public static int influence(Spatial subtree, Skeleton skeleton,
            int boneIndex) {
        Validate.nonNull(skeleton, "skeleton");
        Validate.nonNegative(boneIndex, "bone index");

        int result = directInfluence(subtree, boneIndex);

        Bone bone = skeleton.getBone(boneIndex);
        List<Bone> children = bone.getChildren();
        for (Bone child : children) {
            int childIndex = skeleton.getBoneIndex(child);
            result += influence(subtree, skeleton, childIndex);
        }

        return result;
    }

    /**
     * Interpolate between (or extrapolate from) 2 single-precision values using
     * linear (Lerp) *polation. Unlike
     * {@link com.jme3.math.FastMath#interpolateLinear(float, float, float)}, no
     * rounding error is introduced when y1==y2.
     *
     * @param t descaled parameter value (0&rarr;v0, 1&rarr;v1)
     * @param y1 function value at t=0
     * @param y2 function value at t=1
     * @return an interpolated function value
     */
    public static float lerp(float t, float y1, float y2) {
        float lerp;
        if (y1 == y2) {
            lerp = y1;
        } else {
            float u = 1f - t;
            lerp = u * y1 + t * y2;
        }

        return lerp;
    }

    /**
     * Interpolate between (or extrapolate from) 2 vectors using linear (Lerp)
     * *polation. Unlike
     * {@link com.jme3.math.FastMath#interpolateLinear(float, com.jme3.math.Vector3f, com.jme3.math.Vector3f, com.jme3.math.Vector3f)},
     * no rounding error is introduced when v1==v2.
     *
     * @param t descaled parameter value (0&rarr;v0, 1&rarr;v1)
     * @param v0 function value at t=0 (not null, unaffected, norm=1)
     * @param v1 function value at t=1 (not null, unaffected, norm=1)
     * @param storeResult (modified if not null)
     * @return an interpolated vector (either storeResult or a new instance)
     */
    public static Vector3f lerp(float t, Vector3f v0, Vector3f v1,
            Vector3f storeResult) {
        Validate.nonNull(v0, "v0");
        Validate.nonNull(v1, "v1");
        if (storeResult == null) {
            storeResult = new Vector3f();
        }

        storeResult.x = lerp(t, v0.x, v1.x);
        storeResult.y = lerp(t, v0.y, v1.y);
        storeResult.z = lerp(t, v0.z, v1.z);

        return storeResult;
    }

    /**
     * Load a BVH asset as a CG model without logging any warning/error
     * messages.
     *
     * @param assetManager asset manager
     * @param assetPath path to BVH asset
     * @return a new orphan spatial, or null if unsuccessful
     */
    public static Spatial loadBvhAsset(AssetManager assetManager,
            String assetPath) {
        if (assetManager == null || assetPath == null) {
            return null;
        }

        BVHAnimData loadedData;
        try {
            loadedData = (BVHAnimData) assetManager.loadAsset(assetPath);
        } catch (AssetNotFoundException | NoSuchElementException e) {
            return null;
        }

        Skeleton skeleton = loadedData.getSkeleton();
        SkeletonControl skeletonControl = new SkeletonControl(skeleton);

        AnimControl animControl = new AnimControl(skeleton);
        Animation anim = loadedData.getAnimation();
        animControl.addAnim(anim);

        Spatial result = new Node(assetPath);
        result.addControl(animControl);
        result.addControl(skeletonControl);

        return result;
    }

    /**
     * Load a CG model asset without logging any warning/error messages.
     *
     * @param assetManager asset manager
     * @param assetPath path to CG model asset
     * @return a new orphan spatial, or null if unsuccessful
     */
    public static Spatial loadCgmAsset(AssetManager assetManager,
            String assetPath) {
        if (assetManager == null || assetPath == null) {
            return null;
        }

        ModelKey key = new ModelKey(assetPath);
        /*
         * Temporarily hush warnings about failures to triangulate,
         * vertices with >4 weights, shapes that can't be scaled, and
         * unsupported pass directives.
         */
        Logger faceLogger = Logger.getLogger(Face.class.getName());
        Level faceLevel = faceLogger.getLevel();
        faceLogger.setLevel(Level.SEVERE);

        Logger meshLoaderLogger = Logger.getLogger(MeshLoader.class.getName());
        Level meshLoaderLevel = meshLoaderLogger.getLevel();
        meshLoaderLogger.setLevel(Level.SEVERE);

        Logger materialLoaderLogger = Logger.getLogger(
                MaterialLoader.class.getName());
        Level materialLoaderLevel = materialLoaderLogger.getLevel();
        materialLoaderLogger.setLevel(Level.SEVERE);

        Logger compoundCollisionShapeLogger = Logger.getLogger(
                CompoundCollisionShape.class.getName());
        Level compoundCollisionShapeLevel;
        compoundCollisionShapeLevel = compoundCollisionShapeLogger.getLevel();
        compoundCollisionShapeLogger.setLevel(Level.SEVERE);

        org.slf4j.Logger logger = LoggerFactory.getLogger("jme3_ext_xbuf.XbufLoader");
        ch.qos.logback.classic.Logger xbufLoaderLogger = (ch.qos.logback.classic.Logger) logger;
        ch.qos.logback.classic.Level xbufLoaderLevel = xbufLoaderLogger.getLevel();
        xbufLoaderLogger.setLevel(ch.qos.logback.classic.Level.ERROR);
        /*
         * Load the model.
         */
        Spatial loaded;
        try {
            loaded = assetManager.loadModel(key);
        } catch (AssetNotFoundException | AssetLoadException exception) {
            loaded = null;
        }
        /*
         * Restore logging levels.
         */
        faceLogger.setLevel(faceLevel);
        meshLoaderLogger.setLevel(meshLoaderLevel);
        materialLoaderLogger.setLevel(materialLoaderLevel);
        compoundCollisionShapeLogger.setLevel(compoundCollisionShapeLevel);
        xbufLoaderLogger.setLevel(xbufLoaderLevel);

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
     * Test whether two vectors are distinct, without distinguishing 0 from -0.
     *
     * @param v1 1st input vector (not null, unaffected)
     * @param v2 2nd input vector (not null, unaffected)
     * @return true if distinct, otherwise false
     */
    public static boolean ne(Vector3f v1, Vector3f v2) {
        Validate.nonNull(v1, "1st input vector");
        Validate.nonNull(v2, "2nd input vector");

        boolean result = v1.x != v2.x || v1.y != v2.y || v1.z != v2.z;
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
     * Interpolate between 2 unit quaternions using spherical linear (Slerp)
     * interpolation. This method is slower (but more accurate) than
     * {@link com.jme3.math.Quaternion#slerp(com.jme3.math.Quaternion, float)},
     * always produces a unit, and doesn't trash q1. The caller is responsible
     * for flipping the sign of q0 or q1 when it's appropriate to do so.
     *
     * @param t descaled parameter value (&ge;0, &le;1)
     * @param q0 function value at t=0 (not null, unaffected, norm=1)
     * @param q1 function value at t=1 (not null, unaffected, norm=1)
     * @param storeResult (modified if not null)
     * @return an interpolated unit quaternion (either storeResult or a new
     * instance)
     */
    public static Quaternion slerp(float t, Quaternion q0, Quaternion q1,
            Quaternion storeResult) {
        Validate.inRange(t, "t", 0f, 1f);
        MyQuaternion.validateUnit(q0, "q0", 0.0001f);
        MyQuaternion.validateUnit(q1, "q1", 0.0001f);
        if (storeResult == null) {
            storeResult = new Quaternion();
        }

        Quaternion q0inverse = MyQuaternion.conjugate(q0, null);
        Quaternion ratio = q0inverse.multLocal(q1);
        Quaternion power = MyQuaternion.pow(ratio, t, ratio);
        storeResult.set(q0);
        storeResult.multLocal(power);

        return storeResult;
    }

    /**
     * Interpolate between 4 unit quaternions using the Squad function. The
     * caller is responsible for flipping signs when it's appropriate to do so.
     *
     * @param t descaled parameter value (&ge;0, &le;1)
     * @param p function value at t=0 (not null, unaffected, norm=1)
     * @param a 1st control point (not null, unaffected, norm=1)
     * @param b 2nd control point (not null, unaffected, norm=1)
     * @param q function value at t=1 (not null, unaffected, norm=1)
     * @param storeResult (modified if not null)
     * @return interpolated unit quaternion (either storeResult or a new
     * instance)
     */
    public static Quaternion squad(float t, Quaternion p, Quaternion a,
            Quaternion b, Quaternion q, Quaternion storeResult) {
        Validate.inRange(t, "t", 0f, 1f);
        MyQuaternion.validateUnit(p, "p", 0.0001f);
        MyQuaternion.validateUnit(a, "a", 0.0001f);
        MyQuaternion.validateUnit(b, "b", 0.0001f);
        MyQuaternion.validateUnit(q, "q", 0.0001f);
        if (storeResult == null) {
            storeResult = new Quaternion();
        }

        Quaternion qSlerp = slerp(t, p, q, null);
        Quaternion aSlerp = slerp(t, a, b, null);
        slerp(2f * t * (1f - t), qSlerp, aSlerp, storeResult);

        return storeResult;
    }

    /**
     * Calculate Squad parameter "a" for a continuous 1st derivative at the
     * middle point of 3 specified control points.
     *
     * @param q0 previous control point (not null, unaffected, norm=1)
     * @param q1 current control point (not null, unaffected, norm=1)
     * @param q2 following control point (not null, unaffected, norm=1)
     * @param storeResult (modified if not null)
     * @return a unit quaternion for use as a Squad parameter (either
     * storeResult or a new instance)
     */
    public static Quaternion squadA(Quaternion q0, Quaternion q1,
            Quaternion q2, Quaternion storeResult) {
        MyQuaternion.validateUnit(q0, "q0", 0.0001f);
        MyQuaternion.validateUnit(q1, "q1", 0.0001f);
        MyQuaternion.validateUnit(q2, "q2", 0.0001f);
        if (storeResult == null) {
            storeResult = new Quaternion();
        }

        Quaternion q1c = MyQuaternion.conjugate(q1, null);
        Quaternion turn0 = q1c.mult(q0);
        Quaternion logTurn0 = MyQuaternion.log(turn0, turn0);
        Quaternion turn2 = q1c.mult(q2);
        Quaternion logTurn2 = MyQuaternion.log(turn2, turn2);
        Quaternion sum = logTurn2.addLocal(logTurn0);
        sum.multLocal(-0.25f);
        Quaternion exp = MyQuaternion.exp(sum, sum);
        storeResult.set(q1);
        storeResult.multLocal(exp);

        return storeResult;
    }

    /**
     * Enumerate all entries (in the specified JAR or ZIP) whose names begin
     * with the specified prefix.
     *
     * @param zipPath filesystem path to the JAR or ZIP (not null, not empty)
     * @param namePrefix (not null)
     * @return a new list of entry names
     */
    public static List<String> zipEntries(String zipPath, String namePrefix) {
        Validate.nonEmpty(zipPath, "zip path");
        Validate.nonNull(namePrefix, "name prefix");

        List<String> result = new ArrayList<>(90);
        try (FileInputStream fileIn = new FileInputStream(zipPath);
                ZipInputStream zipIn = new ZipInputStream(fileIn)) {
            for (ZipEntry entry = zipIn.getNextEntry();
                    entry != null;
                    entry = zipIn.getNextEntry()) {
                String entryName = "/" + entry.getName();
                if (entryName.startsWith(namePrefix)) {
                    result.add(entryName);
                }
            }
        } catch (IOException e) {
            // quit reading entries
        }

        return result;
    }
}
