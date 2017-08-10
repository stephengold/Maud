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
package maud.model;

import com.jme3.animation.AnimControl;
import com.jme3.animation.Animation;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.animation.Track;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyAnimation;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import maud.Maud;
import maud.Pose;
import maud.RotationCurve;
import maud.TweenRotations;
import maud.TweenVectors;
import maud.Util;
import maud.VectorCurve;

/**
 * The MVC model of the loaded animation in the Maud application. For loading
 * purposes, the bind pose is treated as an animation. TODO rename
 * SelectedAnimation
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class LoadedAnimation implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            LoadedAnimation.class.getName());
    /**
     * dummy animation name to denote the bind pose (no animation loaded)
     */
    final public static String bindPoseName = "( bind pose )";
    /**
     * dummy animation name to denote a retargeted pose (no animation loaded)
     */
    final public static String retargetedPoseName = "( retargeted pose )";
    // *************************************************************************
    // fields

    /**
     * true &rarr; play continuously ("loop"), false &rarr; play once-through
     * and then pause
     */
    private boolean continueFlag = true;
    /**
     * true &rarr; explicitly paused, false &rarr; running perhaps at speed=0
     */
    private boolean pausedFlag = false;
    /**
     * true &rarr; root bones pinned to bindPos, false &rarr; free to translate
     * TODO move to DisplayedPose
     */
    private boolean pinnedFlag = true;
    /**
     * true &rarr; reverse playback direction ("pong") at limits, false &rarr;
     * wrap time at limits
     */
    private boolean reverseFlag = false;
    /**
     * editable CG model containing the animation, if any (set by
     * {@link #setCgm(LoadedCGModel)})
     */
    private EditableCgm editableCgm;
    /**
     * current animation time for playback (in seconds, &ge;0)
     */
    private float currentTime = 0f;
    /**
     * playback speed and direction when not paused (1 &rarr; forward at normal
     * speed)
     */
    private float speed = 1f;
    /**
     * loaded CG model containing the animation (set by
     * {@link #setCgm(LoadedCGModel)})
     */
    private LoadedCgm loadedCgm = null;
    /**
     * name of the loaded animation, bindPoseName, or retargetedPoseName
     */
    private String loadedName = null;
    // *************************************************************************
    // new methods exposed

    /**
     * Delete everything before the current animation time, and make that the
     * start of the animation.
     */
    public void behead() {
        if (currentTime == 0f) {
            return;
        }
        Animation loaded = getAnimation();
        float oldDuration = loaded.getLength();
        float newDuration = oldDuration - currentTime;
        Animation newAnimation = new Animation(loadedName, newDuration);
        Track[] loadedTracks = loaded.getTracks();
        for (Track track : loadedTracks) {
            Track newTrack;
            if (track instanceof BoneTrack) {
                BoneTrack boneTrack = (BoneTrack) track;
                newTrack = Util.behead(boneTrack, currentTime, oldDuration);
            } else {
                newTrack = track.clone(); // TODO other track types
            }
            newAnimation.addTrack(newTrack);
        }
        currentTime = 0f;
        editableCgm.replaceAnimation(loaded, newAnimation,
                "behead an animation");
    }

    /**
     * Calculate the current transform of the indexed bone.
     *
     * @param boneIndex which bone to calculate
     * @param storeResult (modified if not null)
     * @return transform (either storeResult or a new instance)
     */
    public Transform boneTransform(int boneIndex, Transform storeResult) {
        if (storeResult == null) {
            storeResult = new Transform();
        }

        Animation animation = getAnimation();
        if (animation == null) {
            if (isRetargetedPose()) {
                Maud.model.map.boneTransform(boneIndex, storeResult);
            } else {
                storeResult.loadIdentity();
            }
        } else {
            BoneTrack track = MyAnimation.findTrack(animation, boneIndex);
            if (track == null) {
                storeResult.loadIdentity();
            } else {
                float duration = getDuration();
                Util.boneTransform(track, currentTime, duration, storeResult);
            }
        }

        return storeResult;
    }

    /**
     * Duplicate the loaded animation and then load the new copy.
     *
     * @param newName name for the copy (not null)
     */
    public void copyAndLoad(String newName) {
        Validate.nonNull(newName, "new name");

        newCopy(newName);
        load(newName);
    }

    /**
     * Count the number of bone tracks in the loaded animation.
     *
     * @return count (&ge;0)
     */
    public int countBoneTracks() {
        int count = 0;
        Animation animation = getAnimation();
        if (animation != null) {
            Track[] tracks = animation.getTracks();
            for (Track track : tracks) {
                if (track instanceof BoneTrack) {
                    count++;
                }
            }
        }

        assert count >= 0 : count;
        return count;
    }

    /**
     * Count the total number of tracks in the loaded animation.
     *
     * @return count (&ge;0)
     */
    public int countTracks() {
        int count;
        Animation animation = getAnimation();
        if (animation == null) {
            count = 0;
        } else {
            Track[] tracks = animation.getTracks();
            count = tracks.length;
        }

        assert count >= 0 : count;
        return count;
    }

    /**
     * Delete the loaded animation and (if successful) load bind pose.
     */
    public void delete() {
        if (isReal()) {
            editableCgm.deleteAnimation();
            loadBindPose();
        } else if (isBindPose()) {
            logger.log(Level.WARNING, "cannot delete bind pose");
        } else {
            assert isRetargetedPose();
            logger.log(Level.WARNING, "cannot delete retargeted pose");
        }
    }

    /**
     * Delete all keyframes at the current animation time, which must be &gt;0.
     */
    public void deleteKeyframes() {
        if (currentTime <= 0f) {
            return;
        }
        float duration = getDuration();
        Animation newAnimation = new Animation(loadedName, duration);
        int numDeletions = 0;
        Animation loaded = getAnimation();
        Track[] loadedTracks = loaded.getTracks();
        for (Track track : loadedTracks) {
            Track newTrack;
            if (track instanceof BoneTrack) {
                BoneTrack boneTrack = (BoneTrack) track;
                int keyframeIndex;
                keyframeIndex = MyAnimation.findKeyframeIndex(boneTrack,
                        currentTime);
                if (keyframeIndex >= 1) {
                    newTrack = MyAnimation.deleteKeyframe(boneTrack,
                            keyframeIndex);
                    ++numDeletions;
                } else {
                    newTrack = track.clone();
                }
            } else {
                newTrack = track.clone(); // TODO other track types
            }
            newAnimation.addTrack(newTrack);
        }

        if (numDeletions > 0) {
            editableCgm.replaceAnimation(loaded, newAnimation,
                    "delete keyframes from an animation");
        }
    }

    /**
     * Find the keyframe with the latest time in the loaded animation.
     *
     * @return track time (in seconds, &ge;0)
     */
    public float findLatestKeyframe() {
        Animation loaded = getAnimation();
        float latest = MyAnimation.findLastKeyframe(loaded);

        assert latest >= 0f : latest;
        return latest;
    }

    /**
     * Access the loaded animation.
     *
     * @return the pre-existing instance, or null if none or in bind/retargeted
     * pose
     */
    Animation getAnimation() {
        Animation result;
        if (!loadedCgm.isLoaded() || !isReal()) {
            result = null;
        } else {
            result = loadedCgm.getAnimation(loadedName);
        }

        return result;
    }

    /**
     * Read the duration of the loaded animation.
     *
     * @return time (in seconds, &ge;0)
     */
    public float getDuration() {
        float result;
        Animation animation = getAnimation();
        if (animation == null) {
            result = 0f;
        } else {
            result = animation.getLength();
        }

        assert result >= 0f : result;
        return result;
    }

    /**
     * Find the index of the loaded animation.
     *
     * @return index, or -1 if bind pose/retargeted pose/not found
     */
    public int findIndex() {
        int index;
        if (isReal()) {
            List<String> nameList = loadedCgm.listAnimationsSorted();
            index = nameList.indexOf(loadedName);
        } else {
            index = -1;
        }

        return index;
    }

    /**
     * Find the track for the indexed bone.
     *
     * @param boneIndex which bone (&ge;0)
     * @return the pre-existing instance, or null if none
     */
    BoneTrack findTrackForBone(int boneIndex) {
        Validate.nonNegative(boneIndex, "bone index");

        BoneTrack result = null;
        Animation animation = getAnimation();
        if (animation != null) {
            result = MyAnimation.findTrack(animation, boneIndex);
        }

        return result;
    }

    /**
     * Read the name of the loaded animation.
     *
     * @return the name, or bindPoseName if in bind pose, or retargetedPoseName
     * if in retarget pose (not null)
     */
    public String getName() {
        assert loadedName != null;
        return loadedName;
    }

    /**
     * Read the animation speed.
     *
     * @return relative speed (1 &rarr; normal)
     */
    public float getSpeed() {
        return speed;
    }

    /**
     * Read the animation time.
     *
     * @return seconds since start (&ge;0)
     */
    public float getTime() {
        assert currentTime >= 0f : currentTime;
        return currentTime;
    }

    /**
     * Test whether the animation track for the indexed bone has scales.
     *
     * @param boneIndex which bone (&ge;0)
     * @return true if it has scales, otherwise false
     */
    public boolean hasScales(int boneIndex) {
        Validate.nonNegative(boneIndex, "bone index");

        boolean result = false;
        Animation animation = getAnimation();
        if (animation != null) {
            BoneTrack boneTrack = MyAnimation.findTrack(animation, boneIndex);
            if (boneTrack != null) {
                Vector3f[] scales = boneTrack.getScales();
                if (scales != null) {
                    return true;
                }
            }
        }

        return result;
    }

    /**
     * Test whether the loaded animation includes a bone track for the indexed
     * bone.
     *
     * @param boneIndex which bone (&ge;0)
     * @return true if a track exists, otherwise false
     */
    public boolean hasTrackForBone(int boneIndex) {
        Validate.nonNegative(boneIndex, "bone index");

        boolean result = false;
        Animation animation = getAnimation();
        if (animation != null) {
            result = MyAnimation.hasTrackForBone(animation, boneIndex);
        }

        return result;
    }

    /**
     * Insert a keyframe in each bone track at the current animation time, based
     * on the displayed pose.
     */
    public void insertKeyframes() {
        float duration = getDuration();
        Animation newAnimation = new Animation(loadedName, duration);
        Pose pose = loadedCgm.pose.getPose();

        Animation loaded = getAnimation();
        Track[] loadedTracks = loaded.getTracks();
        for (Track track : loadedTracks) { // TODO add more tracks
            Track newTrack;
            if (track instanceof BoneTrack) {
                BoneTrack boneTrack = (BoneTrack) track;
                int boneIndex = boneTrack.getTargetBoneIndex();
                Transform user = pose.userTransform(boneIndex, null);
                newTrack = MyAnimation.insertKeyframe(boneTrack, currentTime,
                        user);
            } else {
                newTrack = track.clone(); // TODO
            }
            newAnimation.addTrack(newTrack);
        }

        editableCgm.replaceAnimation(loaded, newAnimation,
                "insert keyframes into an animation");
    }

    /**
     * Test whether bind pose is loaded.
     *
     * @return true if it's loaded, false if a real animation or retargeted pose
     * is loaded
     */
    public boolean isBindPose() {
        if (loadedName.equals(bindPoseName)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Test whether retargeted pose is loaded.
     *
     * @return true if it's loaded, false if a real animation or bind pose is
     * loaded
     */
    public boolean isRetargetedPose() {
        if (loadedName.equals(retargetedPoseName)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Test whether the track time is changing.
     *
     * @return true time is changing, false otherwise
     */
    public boolean isMoving() {
        boolean running;
        if (!loadedCgm.isLoaded()) {
            running = false;
        } else if (pausedFlag) {
            running = false;
        } else if (speed == 0f) {
            running = false;
        } else {
            running = true;
        }

        return running;
    }

    /**
     * Test whether the loaded animation is explicitly paused.
     *
     * @return true if paused, false otherwise
     */
    public boolean isPaused() {
        return pausedFlag;
    }

    /**
     * Test whether the root bones are pinned to bindPos.
     *
     * @return true if pinned, false otherwise
     */
    public boolean isPinned() {
        return pinnedFlag;
    }

    /**
     * Test whether a real animation is loaded.
     *
     * @return true if one is loaded, false if bind/retargeted pose is loaded
     */
    public boolean isReal() {
        if (loadedName.equals(bindPoseName)) {
            return false;
        } else if (loadedName.equals(retargetedPoseName)) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether the specified animation name is reserved.
     *
     * @param name which name to test (not null)
     * @return true if reserved, otherwise false
     */
    public static boolean isReserved(String name) {
        boolean result;
        if (name.isEmpty()) {
            result = true;
        } else if (name.equals(bindPoseName)) {
            result = true;
        } else if (name.equals(retargetedPoseName)) {
            result = true;
        } else {
            result = false;
        }

        return result;
    }

    /**
     * List the indices of all bones with tracks in the loaded animation.
     *
     * @return a new list, in arbitrary order
     */
    public List<Integer> listBoneIndicesWithTracks() {
        int numTracks = countTracks();
        List<Integer> result = new ArrayList<>(numTracks);
        Animation animation = getAnimation();
        if (animation != null) {
            Track[] tracks = animation.getTracks();
            for (Track track : tracks) {
                if (track instanceof BoneTrack) {
                    BoneTrack boneTrack = (BoneTrack) track;
                    int index = boneTrack.getTargetBoneIndex();
                    result.add(index);
                }
            }
        }

        return result;
    }

    /**
     * List the names of all bones with tracks in the loaded animation.
     *
     * @return a new list, in arbitrary order
     */
    public List<String> listBonesWithTrack() {
        int numTracks = countTracks();
        List<String> result = new ArrayList<>(numTracks);
        Animation animation = getAnimation();
        if (animation != null) {
            AnimControl animControl = loadedCgm.getAnimControl();
            Track[] tracks = animation.getTracks();
            for (Track track : tracks) {
                if (track instanceof BoneTrack) {
                    BoneTrack boneTrack = (BoneTrack) track;
                    String name = MyAnimation.getTargetName(boneTrack,
                            animControl);
                    result.add(name);
                }
            }
        }

        return result;
    }

    /**
     * Load the named animation at t=0 with the default speed.
     *
     * @param name which animation (not null)
     */
    public void load(String name) {
        Validate.nonNull(name, "animation name");

        if (name.equals(bindPoseName)) {
            /*
             * Load bind pose.
             */
            loadBindPose();

        } else if (name.equals(retargetedPoseName)) {
            /*
             * Load retargeted pose.
             */
            loadRetargetedPose();

        } else {
            float duration = loadedCgm.getDuration(name);
            float playSpeed;
            if (duration == 0f) {
                /*
                 * The animation consists of a single pose: set speed to zero.
                 */
                playSpeed = 0f;
            } else {
                /*
                 * Start the animation loopLerp at normal speed.
                 */
                playSpeed = 1f;
            }
            loadAnimation(name, playSpeed);
        }
    }

    /**
     * Load the named real animation (not bind/retargeted pose) at t=0 with the
     * specified playback speed.
     *
     * @param name which animation (not null)
     * @param newSpeed playback speed
     */
    public void loadAnimation(String name, float newSpeed) {
        Validate.nonNull(name, "animation name");
        assert !isReserved(name);

        loadedName = name;
        speed = newSpeed;
        currentTime = 0f;

        loadedCgm.pose.setToAnimation();
        loadedCgm.pose.setFrozen(false);
    }

    /**
     * Load the bind pose.
     */
    public void loadBindPose() {
        loadedName = bindPoseName;
        speed = 0f;
        currentTime = 0f;

        Skeleton skeleton = loadedCgm.bones.findSkeleton();
        loadedCgm.pose.resetToBind(skeleton);
    }

    /**
     * Load retargeted pose.
     */
    public void loadRetargetedPose() {
        if (Maud.model.getSource().isLoaded()) {
            loadedName = retargetedPoseName;
            speed = 0f;
            currentTime = 0f;
            loadedCgm.pose.setToAnimation();
            loadedCgm.pose.setFrozen(false);
        }
    }

    /**
     * Load the next animation in name-sorted order.
     */
    public void loadNext() {
        if (loadedCgm.isLoaded() && isReal()) {
            List<String> nameList = loadedCgm.listAnimationsSorted();
            int index = nameList.indexOf(loadedName);
            int numAnimations = nameList.size();
            int nextIndex = MyMath.modulo(index + 1, numAnimations);
            String nextName = nameList.get(nextIndex);
            load(nextName);
        }
    }

    /**
     * Load the next animation in name-sorted order.
     */
    public void loadPrevious() {
        if (loadedCgm.isLoaded() && isReal()) {
            List<String> nameList = loadedCgm.listAnimationsSorted();
            int index = nameList.indexOf(loadedName);
            int numAnimations = nameList.size();
            int prevIndex = MyMath.modulo(index - 1, numAnimations);
            String prevName = nameList.get(prevIndex);
            load(prevName);
        }
    }

    /**
     * Add a copy of the loaded animation to the CG model.
     *
     * @param animationName name for the new animation (not null, not reserved,
     * not in use)
     */
    public void newCopy(String animationName) {
        Validate.nonEmpty(animationName, "animation name");
        assert !isReserved(animationName) : animationName;
        assert !loadedCgm.hasAnimation(animationName) : animationName;

        Animation loaded = getAnimation();
        float duration = getDuration();
        Animation copyAnim = new Animation(animationName, duration);
        if (loaded != null) {
            Track[] loadedTracks = loaded.getTracks();
            for (Track track : loadedTracks) {
                Track clone = track.clone();
                copyAnim.addTrack(clone);
            }
        }
        editableCgm.addAnimation(copyAnim);
    }

    /**
     * Add a pose animation and then load it.
     *
     * @param newName name for the new animation (not null)
     */
    public void poseAndLoad(String newName) {
        Validate.nonNull(newName, "new name");

        newPose(newName);
        load(newName);
    }

    /**
     * Reduce all bone tracks in the loaded animation by the specified factor.
     *
     * @param factor reduction factor (&ge;2)
     */
    public void reduce(int factor) {
        Validate.inRange(factor, "reduction factor", 2, Integer.MAX_VALUE);
        assert isReal();

        float duration = getDuration();
        Animation newAnimation = new Animation(loadedName, duration);

        Animation loaded = getAnimation();
        Track[] loadedTracks = loaded.getTracks();
        for (Track track : loadedTracks) {
            Track clone;
            if (track instanceof BoneTrack) {
                BoneTrack boneTrack = (BoneTrack) track;
                clone = MyAnimation.reduce(boneTrack, factor);
            } else {
                clone = track.clone();
            }
            newAnimation.addTrack(clone);
        }

        editableCgm.replaceAnimation(loaded, newAnimation,
                "thin the keyframes in an animation");
    }

    /**
     * Rename the loaded animation.
     *
     * @param newName (not null, not reserved, not in use)
     */
    public void rename(String newName) {
        Validate.nonEmpty(newName, "new name");
        assert !isReserved(newName) : newName;
        assert !loadedCgm.hasAnimation(newName) : newName;
        assert isReal();

        float duration = getDuration();
        Animation newAnimation = new Animation(newName, duration);

        Animation loaded = getAnimation();
        Track[] loadedTracks = loaded.getTracks();
        for (Track track : loadedTracks) {
            Track clone = track.clone();
            newAnimation.addTrack(clone);
        }

        loadedName = newName;
        editableCgm.replaceAnimation(loaded, newAnimation,
                "rename an animation");
    }

    /**
     * Select the named keyframe in the selected bone track.
     *
     * @param name name of the new selection (not null)
     */
    public void selectKeyframe(String name) {
        Validate.nonNull(name, "keyframe name");
        assert loadedCgm.track.isTrackSelected();

        float newTime = Float.valueOf(name);
        // TODO validate
        setTime(newTime);
    }

    /**
     * Alter which CG model contains the animation.
     *
     * @param newLoaded (not null)
     */
    void setCgm(LoadedCgm newLoaded) {
        assert newLoaded != null;

        loadedCgm = newLoaded;
        if (newLoaded instanceof EditableCgm) {
            editableCgm = (EditableCgm) newLoaded;
        } else {
            editableCgm = null;
        }
    }

    /**
     * Alter whether the loaded animation plays continuously.
     *
     * @param newSetting true &rarr; play continuously, false &rarr; play
     * once-through and then pause
     */
    public void setContinue(boolean newSetting) {
        continueFlag = newSetting;
    }

    /**
     * Expand or compress the loaded animation to give it the specified
     * duration.
     *
     * @param newDuration (in seconds, &ge;0)
     */
    public void setDuration(float newDuration) {
        Validate.nonNegative(newDuration, "new duration");

        Animation newAnimation = new Animation(loadedName, newDuration);
        Animation loaded = getAnimation();
        float oldDuration = loaded.getLength();
        Track[] loadedTracks = loaded.getTracks();
        for (Track track : loadedTracks) {
            Track newTrack;
            if (track instanceof BoneTrack) {
                BoneTrack boneTrack = (BoneTrack) track;
                newTrack = MyAnimation.setDuration(boneTrack, newDuration);
            } else {
                newTrack = track.clone(); // TODO other track types
            }
            newAnimation.addTrack(newTrack);
        }

        String eventDescription;
        if (newDuration > oldDuration) {
            eventDescription = "slow down an animation";
        } else {
            eventDescription = "speed up an animation";
        }
        editableCgm.replaceAnimation(loaded, newAnimation, eventDescription);
    }

    /**
     * Alter whether the loaded animation is explicitly paused.
     *
     * @param newSetting true &rarr; paused, false &rarr; running
     */
    public void setPaused(boolean newSetting) {
        pausedFlag = newSetting;
    }

    /**
     * Alter whether the root bones are pinned to bindPos.
     *
     * @param newSetting true &rarr; pinned, false &rarr; free to translate
     */
    public void setPinned(boolean newSetting) {
        pinnedFlag = newSetting;
    }

    /**
     * Alter whether the loaded animation will reverse direction when it reaches
     * a limit.
     *
     * @param newSetting true &rarr; reverse, false &rarr; wrap
     */
    public void setReverse(boolean newSetting) {
        reverseFlag = newSetting;
    }

    /**
     * Alter the playback speed and/or direction.
     *
     * @param newSpeed (1 &rarr; forward at normal speed)
     */
    public void setSpeed(float newSpeed) {
        speed = newSpeed;
    }

    /**
     * Alter the animation time. Update the pose unless it's frozen. Has no
     * effect in bind pose or if the loaded animation has zero duration.
     *
     * @param newTime seconds since start (&ge;0, &le;duration)
     */
    public void setTime(float newTime) {
        float duration = getDuration();
        Validate.inRange(newTime, "animation time", 0f, duration);

        if (duration > 0f) {
            currentTime = newTime;
            boolean frozen = loadedCgm.pose.isFrozen();
            if (!frozen) {
                loadedCgm.pose.setToAnimation();
            }
        }
    }

    /**
     * Toggle between paused and running.
     */
    public void togglePaused() {
        setPaused(!pausedFlag);
    }

    /**
     * Interpolate rotations from the track for the indexed bone to the parallel
     * arrays provided.
     *
     * @param ts sample times (not null, unaffected)
     * @param boneIndex which bone (&ge;0)
     * @param storeWs (not null, modified)
     * @param storeXs (not null, modified)
     * @param storeYs (not null, modified)
     * @param storeZs (not null, modified)
     */
    public void trackInterpolateRotations(float[] ts, int boneIndex,
            float[] storeWs, float[] storeXs,
            float[] storeYs, float[] storeZs) {
        Validate.nonNegative(boneIndex, "bone index");

        TweenRotations technique = Maud.model.misc.getTweenRotations();
        BoneTrack track = findTrackForBone(boneIndex);
        float[] times = track.getKeyFrameTimes();
        float duration = getDuration();
        Quaternion[] rotations = track.getRotations();
        RotationCurve parms = technique.precompute(times, duration, rotations);
        Quaternion tempQ = new Quaternion();

        for (int iSample = 0; iSample < ts.length; iSample++) {
            float time = ts[iSample];
            technique.interpolate(time, parms, tempQ);
            storeWs[iSample] = tempQ.getW();
            storeXs[iSample] = tempQ.getX();
            storeYs[iSample] = tempQ.getY();
            storeZs[iSample] = tempQ.getZ();
        }
    }

    /**
     * Copy the keyframe rotations from the track for the indexed bone to the
     * parallel arrays provided.
     *
     * @param boneIndex which bone (&ge;0)
     * @param storeWs (not null, modified)
     * @param storeXs (not null, modified)
     * @param storeYs (not null, modified)
     * @param storeZs (not null, modified)
     */
    public void trackRotations(int boneIndex, float[] storeWs, float[] storeXs,
            float[] storeYs, float[] storeZs) {
        Validate.nonNegative(boneIndex, "bone index");

        BoneTrack track = findTrackForBone(boneIndex);
        Quaternion[] rotations = track.getRotations();
        int numFrames = rotations.length;
        for (int i = 0; i < numFrames; i++) {
            storeWs[i] = rotations[i].getW();
            storeXs[i] = rotations[i].getX();
            storeYs[i] = rotations[i].getY();
            storeZs[i] = rotations[i].getZ();
        }
    }

    /**
     * Interpolate scales from the track for the indexed bone to the parallel
     * arrays provided.
     *
     * @param ts sample times (not null, unaffected)
     * @param boneIndex which bone (&ge;0)
     * @param storeXs (not null, modified)
     * @param storeYs (not null, modified)
     * @param storeZs (not null, modified)
     */
    public void trackInterpolateScales(float[] ts, int boneIndex,
            float[] storeXs, float[] storeYs, float[] storeZs) {
        Validate.nonNegative(boneIndex, "bone index");

        TweenVectors technique = Maud.model.misc.getTweenScales();
        BoneTrack track = findTrackForBone(boneIndex);
        float[] times = track.getKeyFrameTimes();
        float duration = getDuration();
        Vector3f[] scales = track.getScales();
        VectorCurve parms = technique.precompute(times, duration, scales);
        Vector3f tempV = new Vector3f();

        for (int iSample = 0; iSample < ts.length; iSample++) {
            float time = ts[iSample];
            technique.interpolate(time, parms, tempV);
            storeXs[iSample] = tempV.x;
            storeYs[iSample] = tempV.y;
            storeZs[iSample] = tempV.z;
        }
    }

    /**
     * Copy the keyframe scales from the track for the indexed bone to the
     * parallel arrays provided.
     *
     * @param boneIndex which bone (&ge;0)
     * @param storeXs (not null, modified)
     * @param storeYs (not null, modified)
     * @param storeZs (not null, modified)
     */
    public void trackScales(int boneIndex, float[] storeXs, float[] storeYs,
            float[] storeZs) {
        Validate.nonNegative(boneIndex, "bone index");

        BoneTrack track = findTrackForBone(boneIndex);
        Vector3f[] scales = track.getScales();
        int numFrames = scales.length;
        for (int i = 0; i < numFrames; i++) {
            storeXs[i] = scales[i].x;
            storeYs[i] = scales[i].y;
            storeZs[i] = scales[i].z;
        }
    }

    /**
     * Copy the keyframe times from the track for the indexed bone.
     *
     * @param boneIndex which bone (&ge;0)
     * @return a new array
     */
    public float[] trackTimes(int boneIndex) {
        Validate.nonNegative(boneIndex, "bone index");

        BoneTrack track = findTrackForBone(boneIndex);
        float[] times = track.getKeyFrameTimes();
        int numFrames = times.length;
        float[] result = new float[numFrames];
        System.arraycopy(times, 0, result, 0, numFrames);

        return result;
    }

    /**
     * Interpolate translations from the track for the indexed bone to the
     * parallel arrays provided.
     *
     * @param ts sample times (not null, unaffected)
     * @param boneIndex which bone (&ge;0)
     * @param storeXs (not null, modified)
     * @param storeYs (not null, modified)
     * @param storeZs (not null, modified)
     */
    public void trackInterpolateTranslations(float[] ts, int boneIndex,
            float[] storeXs, float[] storeYs, float[] storeZs) {
        Validate.nonNegative(boneIndex, "bone index");

        TweenVectors technique = Maud.model.misc.getTweenTranslations();
        BoneTrack track = findTrackForBone(boneIndex);
        float[] times = track.getKeyFrameTimes();
        float duration = getDuration();
        Vector3f[] translations = track.getTranslations();
        VectorCurve parms = technique.precompute(times, duration, translations);
        Vector3f tempV = new Vector3f();

        for (int iSample = 0; iSample < ts.length; iSample++) {
            float time = ts[iSample];
            technique.interpolate(time, parms, tempV);
            storeXs[iSample] = tempV.x;
            storeYs[iSample] = tempV.y;
            storeZs[iSample] = tempV.z;
        }
    }

    /**
     * Copy the keyframe translations from the track for the indexed bone to the
     * parallel arrays provided.
     *
     * @param boneIndex which bone (&ge;0)
     * @param storeXs (not null, modified)
     * @param storeYs (not null, modified)
     * @param storeZs (not null, modified)
     */
    public void trackTranslations(int boneIndex, float[] storeXs,
            float[] storeYs, float[] storeZs) {
        Validate.nonNegative(boneIndex, "bone index");

        BoneTrack track = findTrackForBone(boneIndex);
        Vector3f[] translations = track.getTranslations();
        int numFrames = translations.length;
        for (int i = 0; i < numFrames; i++) {
            storeXs[i] = translations[i].x;
            storeYs[i] = translations[i].y;
            storeZs[i] = translations[i].z;
        }
    }

    /**
     * Delete everything after the current animation time, and make that the end
     * of the animation.
     */
    public void truncate() {
        Animation newAnimation = new Animation(loadedName, currentTime);
        Animation loaded = getAnimation();
        Track[] loadedTracks = loaded.getTracks();
        for (Track track : loadedTracks) {
            Track newTrack;
            if (track instanceof BoneTrack) {
                BoneTrack boneTrack = (BoneTrack) track;
                newTrack = MyAnimation.truncate(boneTrack, currentTime);
            } else {
                newTrack = track.clone(); // TODO other track types
            }
            newAnimation.addTrack(newTrack);
        }

        editableCgm.replaceAnimation(loaded, newAnimation,
                "truncate an animation");
    }

    /**
     * Test whether the loaded animation will play continuously.
     *
     * @return true if continuous loop, false otherwise
     */
    public boolean willContinue() {
        return continueFlag;
    }

    /**
     * Test whether the loaded animation will reverse direction at limits.
     *
     * @return true if it will reverse, false otherwise
     */
    public boolean willReverse() {
        return reverseFlag;
    }

    /**
     * Set the final bone transform of each track to match its initial bone
     * transform.
     */
    public void wrapAllTracks() {
        Animation loaded = getAnimation();
        float duration = loaded.getLength();
        Animation newAnimation = new Animation(loadedName, duration);
        Track[] loadedTracks = loaded.getTracks();
        for (Track track : loadedTracks) {
            Track newTrack;
            if (track instanceof BoneTrack) {
                BoneTrack boneTrack = (BoneTrack) track;
                newTrack = MyAnimation.wrap(boneTrack, duration);
            } else {
                newTrack = track.clone(); // TODO other track types
            }
            newAnimation.addTrack(newTrack);
        }

        editableCgm.replaceAnimation(loaded, newAnimation,
                "wrap all tracks in animation");
    }
    // *************************************************************************
    // Object methods

    /**
     * Create a deep copy of this object.
     *
     * @return a new object, equivalent to this one
     * @throws CloneNotSupportedException if superclass isn't cloneable
     */
    @Override
    public LoadedAnimation clone() throws CloneNotSupportedException {
        LoadedAnimation clone = (LoadedAnimation) super.clone();
        return clone;
    }
    // *************************************************************************
    // private methods

    /**
     * Add a new pose animation to the CG model. The new animation has zero
     * duration, a single keyframe at t=0, and all the tracks are BoneTracks,
     * set to the current pose.
     *
     * @param animationName name for the new animation (not null, not reserved,
     * not in use)
     */
    private void newPose(String animationName) {
        assert animationName != null;
        assert !isReserved(animationName) : animationName;
        assert !loadedCgm.hasAnimation(animationName) : animationName;

        Animation poseAnim = loadedCgm.pose.getPose().capture(animationName);
        editableCgm.addAnimation(poseAnim);
    }
}
