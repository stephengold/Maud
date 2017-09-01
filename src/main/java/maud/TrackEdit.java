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
import com.jme3.animation.Track;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.plugins.bvh.BoneMapping;
import com.jme3.scene.plugins.bvh.SkeletonMapping;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import jme3utilities.MyAnimation;
import static jme3utilities.MyAnimation.findKeyframeIndex;
import static jme3utilities.MyAnimation.findPreviousKeyframeIndex;
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
     * Copy a bone track, deleting everything before the specified time, and
     * making that the start of the animation.
     *
     * @param oldTrack (not null, unaffected)
     * @param neckTime cutoff time (in seconds, &gt;0)
     * @param neckTransform user transform of bone at the neck time (not null,
     * unaffected)
     * @param oldDuration (in seconds, &ge;neckTime)
     * @return a new instance
     */
    public static BoneTrack behead(BoneTrack oldTrack, float neckTime,
            Transform neckTransform, float oldDuration) {
        Validate.positive(neckTime, "neck time");

        float[] oldTimes = oldTrack.getKeyFrameTimes();
        Vector3f[] oldTranslations = oldTrack.getTranslations();
        Quaternion[] oldRotations = oldTrack.getRotations();
        Vector3f[] oldScales = oldTrack.getScales();
        int oldCount = oldTimes.length;

        int neckIndex;
        neckIndex = findPreviousKeyframeIndex(oldTrack, neckTime);
        int newCount = oldCount - neckIndex;
        Vector3f[] translations = new Vector3f[newCount];
        Quaternion[] rotations = new Quaternion[newCount];
        Vector3f[] scales = null;
        if (oldScales != null) {
            scales = new Vector3f[newCount];
        }
        float[] times = new float[newCount];

        Transform user = neckTransform.clone();
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
        BoneTrack result = MyAnimation.newBoneTrack(boneIndex, times,
                translations, rotations, scales);

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
     * Copy a bone track, inserting a keyframe at the specified time (which
     * mustn't already have a keyframe).
     *
     * @param oldTrack (not null, unaffected)
     * @param frameTime when to insert (&gt;0)
     * @param transform user transform to insert (not null, unaffected)
     * @return a new instance
     */
    public static BoneTrack insertKeyframe(BoneTrack oldTrack, float frameTime,
            Transform transform) {
        Validate.positive(frameTime, "keyframe time");
        assert findKeyframeIndex(oldTrack, frameTime) == -1;

        float[] oldTimes = oldTrack.getKeyFrameTimes();
        Vector3f[] oldTranslations = oldTrack.getTranslations();
        Quaternion[] oldRotations = oldTrack.getRotations();
        Vector3f[] oldScales = oldTrack.getScales();
        int oldCount = oldTimes.length;

        int newCount = oldCount + 1;
        Vector3f[] translations = new Vector3f[newCount];
        Quaternion[] rotations = new Quaternion[newCount];
        Vector3f[] scales = new Vector3f[newCount];
        float[] newTimes = new float[newCount];

        boolean added = false;
        for (int oldIndex = 0; oldIndex < oldCount; oldIndex++) {
            float time = oldTimes[oldIndex];
            int newIndex = oldIndex;
            if (time > frameTime) {
                if (!added) {
                    translations[newIndex] = transform.getTranslation().clone();
                    rotations[newIndex] = transform.getRotation().clone();
                    scales[newIndex] = transform.getScale().clone();
                    newTimes[newIndex] = frameTime;
                    added = true;
                }
                ++newIndex;
            }
            translations[newIndex] = oldTranslations[oldIndex].clone();
            rotations[newIndex] = oldRotations[oldIndex].clone();
            if (oldScales != null) {
                scales[newIndex] = oldScales[oldIndex].clone();
            } else {
                scales[newIndex] = new Vector3f(1f, 1f, 1f);
            }
            newTimes[newIndex] = oldTimes[oldIndex];
        }
        if (!added) {
            translations[oldCount] = transform.getTranslation().clone();
            rotations[oldCount] = transform.getRotation().clone();
            scales[oldCount] = transform.getScale().clone();
            newTimes[oldCount] = frameTime;
        }

        int boneIndex = oldTrack.getTargetBoneIndex();
        BoneTrack result = MyAnimation.newBoneTrack(boneIndex, newTimes, translations,
                rotations, scales);

        return result;
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
        Vector3f[] newScales = null;
        if (oldScales != null) {
            newScales = new Vector3f[newCount];
        }
        float[] newTimes = new float[newCount];

        for (int newIndex = 0; newIndex < newCount; newIndex++) {
            int oldIndex = newIndex * factor;
            newTranslations[newIndex] = oldTranslations[oldIndex].clone();
            newRotations[newIndex] = oldRotations[oldIndex].clone();
            if (oldScales != null) {
                newScales[newIndex] = oldScales[oldIndex].clone();
            }
            newTimes[newIndex] = oldTimes[oldIndex];
        }

        int boneIndex = oldTrack.getTargetBoneIndex();
        BoneTrack result = MyAnimation.newBoneTrack(boneIndex, newTimes, newTranslations,
                newRotations, newScales);

        return result;
    }

    /**
     * Remove repetitious keyframes from an animation.
     *
     * @param animation (not null)
     * @return number of tracks edited
     */
    public static int removeRepeats(Animation animation) {
        int numTracksEdited = 0;
        Track[] tracks = animation.getTracks();
        for (Track track : tracks) {
            if (track instanceof BoneTrack) {
                BoneTrack boneTrack = (BoneTrack) track;
                boolean removed = removeRepeats(boneTrack);
                if (removed) {
                    ++numTracksEdited;
                }
            } // TODO other track types
        }

        return numTracksEdited;
    }

    /**
     * Remove all repetitious keyframes from a bone track.
     *
     * @param boneTrack (not null)
     * @return true if 1 or more keyframes were removed, otherwise false
     */
    public static boolean removeRepeats(BoneTrack boneTrack) {
        float[] originalTimes = boneTrack.getKeyFrameTimes();
        /*
         * Count distinct keyframes.
         */
        float prevTime = Float.NEGATIVE_INFINITY;
        int numDistinct = 0;
        for (float time : originalTimes) {
            if (time != prevTime) {
                ++numDistinct;
            }
            prevTime = time;
        }

        int originalCount = originalTimes.length;
        if (numDistinct == originalCount) {
            return false;
        }
        Vector3f[] originalTranslations = boneTrack.getTranslations();
        Quaternion[] originalRotations = boneTrack.getRotations();
        Vector3f[] originalScales = boneTrack.getScales();
        /*
         * Allocate new arrays.
         */
        float[] newTimes = new float[numDistinct];
        Vector3f[] newTranslations = new Vector3f[numDistinct];
        Quaternion[] newRotations = new Quaternion[numDistinct];
        Vector3f[] newScales = null;
        if (originalScales != null) {
            newScales = new Vector3f[numDistinct];
        }
        /*
         * Copy all non-repeated keyframes.
         */
        prevTime = Float.NEGATIVE_INFINITY;
        int newIndex = 0;
        for (int oldIndex = 0; oldIndex < originalCount; oldIndex++) {
            float time = originalTimes[oldIndex];
            if (time != prevTime) {
                newTimes[newIndex] = originalTimes[oldIndex];
                newTranslations[newIndex] = originalTranslations[oldIndex];
                newRotations[newIndex] = originalRotations[oldIndex];
                if (newScales != null) {
                    newScales[newIndex] = originalScales[oldIndex];
                }
                ++newIndex;
            }
            prevTime = time;
        }

        if (newScales == null) {
            boneTrack.setKeyframes(newTimes, newTranslations, newRotations);
        } else {
            boneTrack.setKeyframes(newTimes, newTranslations, newRotations,
                    newScales);
        }

        return true;
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
        float[] newTimes = result.getKeyFrameTimes(); // an alias

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

    /**
     * Copy a bone track, smoothing it using the specified techniques.
     *
     * @param oldTrack (not null, unaffected)
     * @param width width of time window (&ge;0, &le;duration)
     * @param smoothTranslations technique for translations (not null)
     * @param smoothRotations technique for translations (not null)
     * @param smoothScales technique for scales (not null)
     * @param duration animation duration (in seconds, &ge;0)
     * @return a new instance
     */
    public static BoneTrack smooth(BoneTrack oldTrack, float width,
            SmoothVectors smoothTranslations, SmoothRotations smoothRotations,
            SmoothVectors smoothScales, float duration) {
        Validate.inRange(width, "width", 0f, duration);
        Validate.nonNegative(duration, "duration");

        float[] oldTimes = oldTrack.getKeyFrameTimes();
        Vector3f[] oldTranslations = oldTrack.getTranslations();
        Quaternion[] oldRotations = oldTrack.getRotations();
        Vector3f[] oldScales = oldTrack.getScales();

        int numFrames = oldTimes.length;
        float[] newTimes = new float[numFrames];
        for (int i = 0; i < numFrames; i++) {
            newTimes[i] = oldTimes[i];
        }

        Vector3f[] newTranslations = new Vector3f[numFrames];
        smoothTranslations.smooth(oldTimes, duration, oldTranslations, width,
                newTranslations);

        Quaternion[] newRotations = new Quaternion[numFrames];
        smoothRotations.smooth(oldTimes, duration, oldRotations, width,
                newRotations);

        Vector3f[] newScales = null;
        if (oldScales != null) {
            newScales = new Vector3f[numFrames];
            smoothScales.smooth(oldTimes, duration, oldScales, width,
                    newScales);
        }

        int boneIndex = oldTrack.getTargetBoneIndex();
        BoneTrack result = MyAnimation.newBoneTrack(boneIndex, newTimes,
                newTranslations, newRotations, newScales);

        return result;
    }

    /**
     * Copy a bone track, truncating it at the specified time.
     *
     * @param oldTrack (not null, unaffected)
     * @param endTime cutoff time (&ge;0)
     * @return a new instance
     */
    public static BoneTrack truncate(BoneTrack oldTrack, float endTime) {
        Validate.positive(endTime, "end time");

        float[] oldTimes = oldTrack.getKeyFrameTimes();
        Vector3f[] oldTranslations = oldTrack.getTranslations();
        Quaternion[] oldRotations = oldTrack.getRotations();
        Vector3f[] oldScales = oldTrack.getScales();

        int newCount = 1 + findPreviousKeyframeIndex(oldTrack, endTime);
        Vector3f[] translations = new Vector3f[newCount];
        Quaternion[] rotations = new Quaternion[newCount];
        Vector3f[] scales = null;
        if (oldScales != null) {
            scales = new Vector3f[newCount];
        }
        float[] times = new float[newCount];

        for (int frameIndex = 0; frameIndex < newCount; frameIndex++) {
            translations[frameIndex] = oldTranslations[frameIndex].clone();
            rotations[frameIndex] = oldRotations[frameIndex].clone();
            if (oldScales != null) {
                scales[frameIndex] = oldScales[frameIndex].clone();
            }
            times[frameIndex] = oldTimes[frameIndex];
        }

        int boneIndex = oldTrack.getTargetBoneIndex();
        BoneTrack result = MyAnimation.newBoneTrack(boneIndex, times, translations,
                rotations, scales);

        return result;
    }

    /**
     * Copy a bone track, altering its end-time keyframe to match its 1st
     * keyframe. If the track doesn't end with a keyframe, append one.
     *
     * @param oldTrack (not null, unaffected)
     * @param endTime when to insert (&gt;0)
     * @return a new instance
     */
    public static BoneTrack wrap(BoneTrack oldTrack, float endTime) {
        Validate.positive(endTime, "end time");

        float[] oldTimes = oldTrack.getKeyFrameTimes();
        Vector3f[] oldTranslations = oldTrack.getTranslations();
        Quaternion[] oldRotations = oldTrack.getRotations();
        Vector3f[] oldScales = oldTrack.getScales();
        int oldCount = oldTimes.length;

        int newCount;
        int endIndex = findKeyframeIndex(oldTrack, endTime);
        if (endIndex == -1) {
            endIndex = oldCount;
            newCount = oldCount + 1;
        } else {
            newCount = oldCount;
        }
        assert endIndex == newCount - 1;
        Vector3f[] translations = new Vector3f[newCount];
        Quaternion[] rotations = new Quaternion[newCount];
        Vector3f[] scales = null;
        if (oldScales != null) {
            scales = new Vector3f[newCount];
        }
        float[] newTimes = new float[newCount];

        for (int frameIndex = 0; frameIndex < endIndex; frameIndex++) {
            translations[frameIndex] = oldTranslations[frameIndex].clone();
            rotations[frameIndex] = oldRotations[frameIndex].clone();
            if (oldScales != null) {
                scales[frameIndex] = oldScales[frameIndex].clone();
            }
            newTimes[frameIndex] = oldTimes[frameIndex];
        }

        translations[endIndex] = oldTranslations[0].clone();
        rotations[endIndex] = oldRotations[0].clone();
        if (oldScales != null) {
            scales[endIndex] = oldScales[0].clone();
        }
        newTimes[endIndex] = endTime;

        int boneIndex = oldTrack.getTargetBoneIndex();
        BoneTrack result = MyAnimation.newBoneTrack(boneIndex, newTimes, translations,
                rotations, scales);

        return result;
    }

    /**
     * Repair all tracks in which the 1st keyframe isn't at time=0.
     *
     * @param animation (not null)
     * @return number of tracks edited (&ge;0)
     */
    public static int zeroFirst(Animation animation) {
        int numTracksEdited = 0;
        Track[] tracks = animation.getTracks();
        for (Track track : tracks) {
            float[] times = track.getKeyFrameTimes();
            if (times[0] != 0f) {
                times[0] = 0f;
                ++numTracksEdited;
            }
        }

        return numTracksEdited;
    }
}
