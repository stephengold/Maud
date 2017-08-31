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

import com.jme3.animation.Animation;
import com.jme3.animation.Bone;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.plugins.bvh.BoneMapping;
import com.jme3.scene.plugins.bvh.SkeletonMapping;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import jme3utilities.MyAnimation;
import jme3utilities.Validate;

/**
 * Utility methods for track/animation editing. All methods should be static.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TrackEdit {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            TrackEdit.class.getName());
    /**
     * local copy of {@link com.jme3.math.Vector3f#UNIT_XYZ}
     */
    final private static Vector3f scaleIdentity = new Vector3f(1f, 1f, 1f);
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private TrackEdit() {
    }
    // *************************************************************************
    // new methods exposed

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
     * Copy a bone track, deleting the indexed range of keyframes (which mustn't
     * include the 1st keyframe).
     *
     * @param oldTrack (not null, unaffected)
     * @param startIndex 1st keyframe to delete (&gt;0, &le;lastIndex)
     * @param deleteCount number of keyframes to delete (&gt;0, &lt;lastIndex)
     * @return a new instance
     */
    public static BoneTrack deleteRange(BoneTrack oldTrack, int startIndex,
            int deleteCount) {
        float[] oldTimes = oldTrack.getKeyFrameTimes();
        int oldCount = oldTimes.length;
        int lastIndex = oldCount - 1;
        Validate.inRange(startIndex, "start index", 1, lastIndex);
        Validate.inRange(deleteCount, "delete count", 1, lastIndex);
        float endIndex = startIndex + deleteCount - 1;
        Validate.inRange(endIndex, "end index", 1, lastIndex);

        Vector3f[] oldTranslations = oldTrack.getTranslations();
        Quaternion[] oldRotations = oldTrack.getRotations();
        Vector3f[] oldScales = oldTrack.getScales();

        int newCount = oldCount - deleteCount;
        Vector3f[] newTranslations = new Vector3f[newCount];
        Quaternion[] newRotations = new Quaternion[newCount];
        Vector3f[] newScales = null;
        if (oldScales != null) {
            newScales = new Vector3f[newCount];
        }
        float[] newTimes = new float[newCount];

        for (int newIndex = 0; newIndex < newCount; newIndex++) {
            int oldIndex;
            if (newIndex < startIndex) {
                oldIndex = newIndex;
            } else {
                oldIndex = newIndex + deleteCount;
            }
            newTranslations[newIndex] = oldTranslations[oldIndex].clone();
            newRotations[newIndex] = oldRotations[oldIndex].clone();
            if (oldScales != null) {
                newScales[newIndex] = oldScales[oldIndex].clone();
            }
            newTimes[newIndex] = oldTimes[oldIndex];
        }

        int boneIndex = oldTrack.getTargetBoneIndex();
        BoneTrack result = MyAnimation.newBoneTrack(boneIndex, newTimes,
                newTranslations, newRotations, newScales);

        return result;
    }

    /**
     * Create a BoneTrack in which all keyframes have the same transform.
     *
     * @param boneIndex which bone (&ge;0)
     * @param frameTimes (not null, unaffected)
     * @param transform (not null, unaffected)
     * @return a new instance
     */
    public static BoneTrack newBoneTrack(int boneIndex, float[] frameTimes,
            Transform transform) {
        Validate.nonNegative(boneIndex, "bone index");

        int numFrames = frameTimes.length;
        float[] times = new float[numFrames];
        Vector3f[] translations = new Vector3f[numFrames];
        Quaternion[] rotations = new Quaternion[numFrames];
        Vector3f[] scales = new Vector3f[numFrames];
        transform = transform.clone();

        for (int frameIndex = 0; frameIndex < numFrames; frameIndex++) {
            times[frameIndex] = frameTimes[frameIndex];
            translations[frameIndex] = transform.getTranslation();
            rotations[frameIndex] = transform.getRotation();
            scales[frameIndex] = transform.getScale();
        }
        BoneTrack result = MyAnimation.newBoneTrack(boneIndex, times,
                translations, rotations, scales);

        return result;
    }

    /**
     * Copy a bone track, setting the indexed keyframe to the specified
     * transform.
     *
     * @param oldTrack (not null, unaffected)
     * @param frameIndex which keyframe (&ge;0, &lt;numFrames)
     * @param transform user transform (not null, unaffected?)
     * @return a new instance
     */
    public static BoneTrack replaceKeyframe(BoneTrack oldTrack, int frameIndex,
            Transform transform) {
        float[] oldTimes = oldTrack.getKeyFrameTimes();
        int frameCount = oldTimes.length;
        Validate.inRange(frameIndex, "keyframe index", 0, frameCount - 1);
        Validate.nonNull(transform, "transform");

        Vector3f[] oldTranslations = oldTrack.getTranslations();
        Quaternion[] oldRotations = oldTrack.getRotations();
        Vector3f[] oldScales = oldTrack.getScales();

        Vector3f[] newTranslations = new Vector3f[frameCount];
        Quaternion[] newRotations = new Quaternion[frameCount];
        Vector3f[] newScales = new Vector3f[frameCount];
        float[] newTimes = new float[frameCount];

        for (int frameI = 0; frameI < frameCount; frameI++) {
            newTimes[frameI] = oldTimes[frameI];
            if (frameI == frameIndex) {
                newTranslations[frameI] = transform.getTranslation();
                newRotations[frameI] = transform.getRotation();
                newScales[frameI] = transform.getScale();
            } else {
                newTranslations[frameI] = oldTranslations[frameI].clone();
                newRotations[frameI] = oldRotations[frameI].clone();
                if (oldScales == null) {
                    newScales[frameI] = new Vector3f(1f, 1f, 1f);
                } else {
                    newScales[frameI] = oldScales[frameI].clone();
                }
            }
        }

        int boneIndex = oldTrack.getTargetBoneIndex();
        BoneTrack result = MyAnimation.newBoneTrack(boneIndex, newTimes,
                newTranslations, newRotations, newScales);

        return result;
    }

    /**
     * Copy a bone track, resampling at the specified times.
     *
     * @param oldTrack (not null, unaffected)
     * @param newTimes sample times (not null, alias created)
     * @param duration animation duration (in seconds, &ge;0)
     * @return a new instance
     */
    public static BoneTrack resample(BoneTrack oldTrack, float[] newTimes,
            float duration) {
        Validate.nonNegative(duration, "duration");

        int boneIndex = oldTrack.getTargetBoneIndex();
        int numSamples = newTimes.length;
        Vector3f[] newTranslations = new Vector3f[numSamples];
        Quaternion[] newRotations = new Quaternion[numSamples];
        Vector3f[] newScales = null;
        Vector3f[] oldScales = oldTrack.getScales();
        if (oldScales != null) {
            newScales = new Vector3f[numSamples];
        }

        for (int frameIndex = 0; frameIndex < numSamples; frameIndex++) {
            float time = newTimes[frameIndex];
            Transform boneTransform;
            boneTransform = boneTransform(oldTrack, time, duration, null);
            newTranslations[frameIndex] = boneTransform.getTranslation();
            newRotations[frameIndex] = boneTransform.getRotation();
            if (oldScales != null) {
                newScales[frameIndex] = boneTransform.getScale();
            }
        }

        BoneTrack result = MyAnimation.newBoneTrack(boneIndex, newTimes,
                newTranslations, newRotations, newScales);

        return result;
    }

    /**
     * Copy a bone track, resampling it at the specified rate.
     *
     * @param oldTrack (not null, unaffected)
     * @param sampleRate sample rate (in frames per second, &gt;0)
     * @param duration animation duration (in seconds, &ge;0)
     * @return a new instance
     */
    public static BoneTrack resampleAtRate(BoneTrack oldTrack, float sampleRate,
            float duration) {
        Validate.positive(sampleRate, "sample rate");
        Validate.nonNegative(duration, "duration");

        int numSamples = 1 + (int) Math.floor(duration * sampleRate);
        float[] newTimes = new float[numSamples];
        for (int frameIndex = 0; frameIndex < numSamples; frameIndex++) {
            float time = frameIndex / sampleRate;
            if (time > duration) {
                time = duration;
            }
            newTimes[frameIndex] = time;
        }
        BoneTrack result = resample(oldTrack, newTimes, duration);

        return result;
    }

    /**
     * Copy a bone track, resampling to the specified number of samples.
     *
     * @param oldTrack (not null, unaffected)
     * @param numSamples number of samples (&ge;2)
     * @param duration animation duration (in seconds, &gt;0)
     * @return a new instance
     */
    public static BoneTrack resampleToNumber(BoneTrack oldTrack, int numSamples,
            float duration) {
        Validate.inRange(numSamples, "number of samples", 2, Integer.MAX_VALUE);
        Validate.positive(duration, "duration");

        float[] newTimes = new float[numSamples];
        for (int frameIndex = 0; frameIndex < numSamples; frameIndex++) {
            float time;
            if (frameIndex == numSamples - 1) {
                time = duration;
            } else {
                time = (frameIndex * duration) / (numSamples - 1);
            }
            newTimes[frameIndex] = time;
        }
        BoneTrack result = resample(oldTrack, newTimes, duration);

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
        Validate.nonNull(sourceSkeleton, "source skeleton");
        Validate.nonNull(map, "map");
        Validate.nonNull(animationName, "animation name");
        /*
         * Start with an empty animation.
         */
        float duration = sourceAnimation.getLength();
        Animation result = new Animation(animationName, duration);

        Map<Float, Pose> cache = new TreeMap<>();
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
                        sourceSkeleton, targetSkeleton, map, iTarget, cache);
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
     * @param cache previously calculated poses (not null, added to)
     * @return a new bone track
     */
    public static BoneTrack retargetTrack(Animation sourceAnimation,
            BoneTrack sourceTrack, Skeleton sourceSkeleton,
            Skeleton targetSkeleton, SkeletonMapping map,
            int targetBoneIndex, Map<Float, Pose> cache) {
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

        for (int frameIndex = 0; frameIndex < numKeyframes; frameIndex++) {
            float trackTime = times[frameIndex];
            Pose targetPose = cache.get(trackTime);
            if (targetPose == null) {
                targetPose = new Pose(targetSkeleton);
                sourcePose.setToAnimation(sourceAnimation, trackTime);
                targetPose.setToRetarget(sourcePose, map);
                cache.put(trackTime, targetPose);
            }
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
}
