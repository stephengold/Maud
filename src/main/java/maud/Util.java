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
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.blender.meshes.Face;
import com.jme3.scene.plugins.bvh.BVHAnimData;
import com.jme3.scene.plugins.bvh.BoneMapping;
import com.jme3.scene.plugins.bvh.SkeletonMapping;
import com.jme3.scene.plugins.ogre.MaterialLoader;
import com.jme3.scene.plugins.ogre.MeshLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyAnimation;
import jme3utilities.MyCamera;
import jme3utilities.MySkeleton;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;

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
     * Calculate the bone transform for the specified track and time, using
     * linear interpolation with no blending.
     *
     * @param track (not null, unaffected)
     * @param time animation time input
     * @param storeResult (modified if not null)
     * @return transform (either storeResult or a new instance)
     */
    public static Transform boneTransform(BoneTrack track, float time,
            Transform storeResult) {
        if (storeResult == null) {
            storeResult = new Transform();
        }
        float[] times = track.getTimes();
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

        } else if (time >= times[lastFrame]) {
            /*
             * Copy the transform of the last frame.
             */
            storeResult.setTranslation(translations[lastFrame]);
            storeResult.setRotation(rotations[lastFrame]);
            if (scales == null) {
                storeResult.setScale(scaleIdentity);
            } else {
                storeResult.setScale(scales[lastFrame]);
            }

        } else {
            /*
             * Interpolate between two successive frames.
             */
            interpolateTransform(time, times, translations, rotations, scales,
                    storeResult);
        }

        return storeResult;
    }

    /**
     * Find a cardinal quaternion similar to the specified input. A cardinal
     * quaternion is one for which the rotations angles on all three axes are
     * integer multiples of Pi/2 radians.
     *
     * @param input (not null, modified)
     */
    public static void cardinalizeLocal(Quaternion input) {
        Validate.nonNull(input, "input");

        MyMath.snapLocal(input, 0);
        MyMath.snapLocal(input, 1);
        MyMath.snapLocal(input, 2);
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
     * Test whether the specified vector contains more than one value.
     *
     * @param vector input (not null)
     * @return true if multiple values found, otherwise false
     */
    public static boolean distinct(float[] vector) {
        Validate.nonNull(vector, "vector");

        boolean result = false;
        if (vector.length > 1) {
            float first = vector[0];
            for (float value : vector) {
                if (value != first) {
                    result = true;
                    break;
                }
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
     * Interpolate linearly between keyframes of a bone track.
     *
     * @param time (in seconds)
     * @param times (not null, unaffected)
     * @param translations (not null, unaffected)
     * @param rotations (not null, unaffected)
     * @param scales (may be null, unaffected)
     * @param storeResult (modified if not null)
     * @return transform (either storeResult or a new instance)
     */
    public static Transform interpolateTransform(float time, float[] times,
            Vector3f[] translations, Quaternion[] rotations, Vector3f[] scales,
            Transform storeResult) {
        if (storeResult == null) {
            storeResult = new Transform();
        }

        int lastFrame = times.length - 1;
        int startFrame = -1;
        // TODO binary search
        for (int iFrame = 0; iFrame < lastFrame; iFrame++) {
            if (time >= times[iFrame] && time <= times[iFrame + 1]) {
                startFrame = iFrame;
                break;
            }
        }
        assert startFrame >= 0 : startFrame;
        int endFrame = startFrame + 1;
        float frameDuration = times[endFrame] - times[startFrame];
        assert frameDuration > 0f : frameDuration;
        float fraction = (time - times[startFrame]) / frameDuration;

        Vector3f translation = storeResult.getTranslation();
        translation.interpolateLocal(translations[startFrame],
                translations[endFrame], fraction);

        Quaternion rotation = storeResult.getRotation();
        rotation.set(rotations[startFrame]);
        rotation.nlerp(rotations[endFrame], fraction);

        if (scales == null) {
            storeResult.setScale(scaleIdentity);
        } else {
            Vector3f scale = storeResult.getScale();
            scale.interpolateLocal(scales[startFrame], scales[endFrame],
                    fraction);
        }

        return storeResult;
    }

    /**
     * Enumerate all view ports that contain the specified screen position.
     *
     * @param screenXY (in pixels, not null, unaffected)
     * @param renderManager (not null)
     *
     * @return a new list of pre-existing view ports
     */
    public static List<ViewPort> listViewPorts(RenderManager renderManager,
            Vector2f screenXY) {
        Validate.nonNull(screenXY, "screen xy");

        List<ViewPort> result = new ArrayList<>(4);

        List<ViewPort> preViews = renderManager.getPreViews();
        for (ViewPort preView : preViews) {
            if (MyCamera.contains(preView, screenXY)) {
                result.add(preView);
            }
        }

        List<ViewPort> mainViews = renderManager.getMainViews();
        for (ViewPort mainView : mainViews) {
            if (MyCamera.contains(mainView, screenXY)) {
                result.add(mainView);
            }
        }

        List<ViewPort> postViews = renderManager.getPostViews();
        for (ViewPort postView : postViews) {
            if (MyCamera.contains(postView, screenXY)) {
                result.add(postView);
            }
        }

        return result;
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
         * vertices with >4 weights, and unsupported pass directives.
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

        return loaded;
    }

    /**
     * Normalize a dataset to [0, 1]. If min=max, all data will be set to 0.5.
     *
     * @param data data to normalize (not null, modified)
     */
    public static void normalize(float[] data) {
        Validate.nonNull(data, "data");

        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;
        for (float value : data) {
            if (value < min) {
                min = value;
            }
            if (value > max) {
                max = value;
            }
        }
        normalize(data, min, max);
    }

    /**
     * Normalize a dataset to [0, 1] using the specified min and max. If
     * min=max, all data will be set to 0.5.
     *
     * @param data data to normalize (not null, modified)
     * @param min minimum value
     * @param max maximum value in dataset
     */
    public static void normalize(float[] data, float min, float max) {
        Validate.nonNull(data, "data");

        for (int i = 0; i < data.length; i++) {
            if (min == max) {
                data[i] = 0.5f;
            } else {
                data[i] = (data[i] - min) / (max - min);
            }
        }
    }

    /**
     * Copy a bone track, reducing the number of keyframes by the specified
     * factor.
     *
     * @param oldTrack (not null, unaffected)
     * @param factor reduction factor (&ge;2)
     * @return a new instance
     */
    public static BoneTrack reduce(BoneTrack oldTrack, int factor) {
        Validate.inRange(factor, "factor", 2, Integer.MAX_VALUE);

        Vector3f[] oldTranslations = oldTrack.getTranslations();
        Quaternion[] oldRotations = oldTrack.getRotations();
        Vector3f[] oldScales = oldTrack.getScales();
        float[] oldTimes = oldTrack.getKeyFrameTimes();
        int oldCount = oldTimes.length;
        assert oldCount > 0 : oldCount;

        int newCount = 1 + (oldCount - 1) / factor;
        Vector3f[] newTranslations = new Vector3f[newCount];
        Quaternion[] newRotations = new Quaternion[newCount];
        Vector3f[] newScales;
        if (oldScales == null) {
            newScales = null;
        } else {
            newScales = new Vector3f[newCount];
        }
        float[] newTimes = new float[newCount];

        for (int newIndex = 0; newIndex < newCount; newIndex++) {
            int oldIndex = newIndex * factor;
            newTranslations[newIndex] = oldTranslations[oldIndex].clone();
            newRotations[newIndex] = oldRotations[oldIndex].clone();
            newScales[newIndex] = oldScales[oldIndex].clone();
            newTimes[newIndex] = oldTimes[oldIndex];
        }

        int boneIndex = oldTrack.getTargetBoneIndex();
        BoneTrack result = new BoneTrack(boneIndex, newTimes, newTranslations,
                newRotations, newScales);

        return result;
    }

    /**
     * Re-target the specified animation from the specified source skeleton to
     * the specified target skeleton using the specified mapping.
     *
     * @param sourceAnimation which animation to re-target (not null,
     * unaffected)
     * @param sourceSkeleton (not null, unaffected)
     * @param targetSkeleton (not null, unaffected)
     * @param mapping which skeleton mapping to use (not null, unaffected)
     * @param animationName name for the resulting animation (not null)
     * @return a new animation
     */
    public static Animation retargetAnimation(Animation sourceAnimation,
            Skeleton sourceSkeleton, Skeleton targetSkeleton,
            SkeletonMapping mapping, String animationName) {
        Validate.nonNull(sourceAnimation, "source animation");
        Validate.nonNull(sourceSkeleton, "source skeleton");
        Validate.nonNull(targetSkeleton, "target skeleton");
        Validate.nonNull(mapping, "mapping");
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
            BoneMapping boneMapping = mapping.get(targetName);
            if (boneMapping != null) {
                String sourceName = boneMapping.getSourceName();
                int iSource = sourceSkeleton.getBoneIndex(sourceName);
                BoneTrack sourceTrack;
                sourceTrack = MyAnimation.findTrack(sourceAnimation, iSource);
                BoneTrack track = retargetTrack(sourceAnimation, sourceTrack,
                        sourceSkeleton, targetSkeleton, mapping, iTarget);
                result.addTrack(track);
            }
        }

        return result;
    }

    /**
     * Re-target the specified bone track from the specified source skeleton to
     * the specified target skeleton using the specified mapping.
     *
     * @param sourceAnimation the animation to re-target, or null for bind pose
     * @param sourceSkeleton (not null, unaffected)
     * @param sourceTrack input bone track (not null, unaffected)
     * @param targetSkeleton (not null, unaffected)
     * @param mapping which skeleton mapping to use (not null, unaffected)
     * @param targetBoneIndex index of the target bone (&ge;0)
     * @return a new bone track
     */
    public static BoneTrack retargetTrack(Animation sourceAnimation,
            BoneTrack sourceTrack, Skeleton sourceSkeleton,
            Skeleton targetSkeleton, SkeletonMapping mapping,
            int targetBoneIndex) {
        Validate.nonNull(sourceSkeleton, "source skeleton");
        Validate.nonNull(targetSkeleton, "target skeleton");
        Validate.nonNull(mapping, "mapping");
        Validate.nonNegative(targetBoneIndex, "target bone index");

        float[] times;
        int numKeyframes;
        if (sourceTrack == null) {
            numKeyframes = 1;
            times = new float[numKeyframes];
            times[0] = 0f;
        } else {
            times = sourceTrack.getTimes();
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
            targetPose.setToRetarget(sourcePose, mapping);

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
     * Copy a bone track, altering its duration and adjusting all its keyframes
     * proportionately.
     *
     * @param oldTrack (not null, unaffected)
     * @param newDuration new duration (in seconds, &ge;0)
     * @return a new instance
     */
    public static BoneTrack setDuration(BoneTrack oldTrack, float newDuration) {
        Validate.nonNegative(newDuration, "duration");

        BoneTrack result = oldTrack.clone();
        float[] newTimes = result.getKeyFrameTimes();

        float oldDuration = oldTrack.getLength();
        float[] oldTimes = oldTrack.getKeyFrameTimes();
        int numFrames = oldTimes.length;
        assert numFrames == 1 || oldDuration > 0f : numFrames;

        for (int frameIndex = 0; frameIndex < numFrames; frameIndex++) {
            float oldTime = oldTimes[frameIndex];
            assert oldTime <= oldDuration : oldTime;

            float newTime;
            if (oldDuration == 0f) {
                assert frameIndex == 0 : frameIndex;
                assert oldTime == 0f : oldTime;
                newTime = 0f;
            } else {
                newTime = newDuration * oldTime / oldDuration;
                newTime = FastMath.clamp(newTime, 0f, newDuration);
            }
            newTimes[frameIndex] = newTime;
        }

        return result;
    }
}
