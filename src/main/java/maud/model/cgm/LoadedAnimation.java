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
package maud.model.cgm;

import com.jme3.animation.AnimControl;
import com.jme3.animation.Animation;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SpatialTrack;
import com.jme3.animation.Track;
import com.jme3.math.Transform;
import com.jme3.scene.Spatial;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyAnimation;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import jme3utilities.wes.Pose;
import jme3utilities.wes.TrackEdit;
import jme3utilities.wes.TweenTransforms;
import maud.Maud;
import maud.MaudUtil;
import maud.model.EditableMap;
import maud.model.EditorModel;

/**
 * The MVC model of the loaded animation in a C-G model. For loading purposes,
 * the bind pose and retargeted pose are treated as animations.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class LoadedAnimation implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(LoadedAnimation.class.getName());
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
     * true &rarr; root bones pinned to bind transform, false &rarr; free to
     * transform
     */
    private boolean pinnedFlag = false;
    /**
     * C-G model containing the animation (set by {@link #setCgm(Cgm)})
     */
    private Cgm cgm = null;
    /**
     * editable C-G model, if any, containing the animation (set by
     * {@link #setCgm(Cgm)})
     */
    private EditableCgm editableCgm;
    /**
     * current animation time for playback (seconds since start, &ge;0)
     */
    private float currentTime = 0f;
    /**
     * name of the loaded animation, bindPoseName, or retargetedPoseName
     */
    private String loadedName = null;
    // *************************************************************************
    // new methods exposed

    /**
     * Test whether any track ends with a keyframe.
     *
     * @return true if one or more tracks ends with a keyframe, otherwise false
     */
    public boolean anyTrackEndsWithKeyframe() {
        boolean result = false;
        Animation real = getReal();
        if (real != null) {
            float duration = getDuration();
            Track[] tracks = real.getTracks();
            for (Track track : tracks) {
                int endIndex = MyAnimation.findKeyframeIndex(track, duration);
                if (endIndex >= 0) {
                    result = true;
                }
            }
        }

        return result;
    }

    /**
     * Delete everything before the current animation time, and make that the
     * start of the animation.
     */
    public void behead() {
        if (currentTime <= 0f) {
            return;
        }
        Animation oldAnimation = getReal();
        float oldDuration = oldAnimation.getLength();
        float newDuration = oldDuration - currentTime;
        Animation newAnimation = new Animation(loadedName, newDuration);
        TweenTransforms techniques = Maud.getModel().getTweenTransforms();
        Track[] oldTracks = oldAnimation.getTracks();
        for (Track track : oldTracks) {
            Track newTrack;
            if (track instanceof BoneTrack || track instanceof SpatialTrack) {
                Transform neck = techniques.interpolate(currentTime, track,
                        oldDuration, null, null);
                newTrack = TrackEdit.behead(track, currentTime, neck,
                        oldDuration);
            } else {
                newTrack = track.clone(); // TODO other track types
            }
            newAnimation.addTrack(newTrack);
        }
        currentTime = 0f;
        editableCgm.replace(oldAnimation, newAnimation, "behead an animation");
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

        EditorModel model = Maud.getModel();
        Animation real = getReal();
        if (real == null) {
            if (isRetargetedPose()) {
                EditableMap map = model.getMap();
                map.boneTransform(boneIndex, storeResult);
            } else {
                storeResult.loadIdentity();
            }
        } else {
            BoneTrack track = MyAnimation.findBoneTrack(real, boneIndex);
            if (track == null) {
                storeResult.loadIdentity();
            } else {
                TweenTransforms techniques = model.getTweenTransforms();
                float duration = getDuration();
                techniques.transform(track, currentTime, duration, null,
                        storeResult);
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
        Animation realAnimation = getReal();
        int count = MyAnimation.countTracks(realAnimation, BoneTrack.class);

        assert count >= 0 : count;
        return count;
    }

    /**
     * Count the number of spatial tracks in the loaded animation.
     *
     * @return count (&ge;0)
     */
    public int countSpatialTracks() {
        Animation realAnimation = getReal();
        int count = MyAnimation.countTracks(realAnimation, SpatialTrack.class);

        assert count >= 0 : count;
        return count;
    }

    /**
     * Count the total number of tracks in the loaded animation.
     *
     * @return count (&ge;0)
     */
    public int countTracks() {
        int count = 0;
        Animation realAnimation = getReal();
        if (realAnimation != null) {
            Track[] tracks = realAnimation.getTracks();
            count = tracks.length;
        }

        assert count >= 0 : count;
        return count;
    }

    /**
     * Add an identity track for the selected bone.
     */
    public void createBoneTrack() {
        Animation realAnimation = getReal();
        Track[] tracks = realAnimation.getTracks();
        Track baseTrack = tracks[0]; // arbitrary choice
        float[] baseTimes = baseTrack.getKeyFrameTimes();
        int boneIndex = cgm.getBone().getIndex();
        Transform identity = new Transform();
        Track newTrack = MyAnimation.newBoneTrack(boneIndex, baseTimes,
                identity);

        String animationName = realAnimation.getName();
        String boneName = cgm.getBone().getName();
        String eventDescription = String.format(
                "add an identity track to %s for bone %s",
                MyString.quote(animationName), MyString.quote(boneName));
        editableCgm.addTrack(newTrack, eventDescription);
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
        Animation realAnimation = getReal();
        Track[] tracks = realAnimation.getTracks();
        for (Track track : tracks) {
            Track newTrack;
            if (track instanceof BoneTrack || track instanceof SpatialTrack) {
                int keyframeIndex
                        = MyAnimation.findKeyframeIndex(track, currentTime);
                if (keyframeIndex >= 1) {
                    newTrack = TrackEdit.deleteRange(track, keyframeIndex, 1);
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
            editableCgm.replace(realAnimation, newAnimation,
                    "delete keyframes from an animation");
        }
    }

    /**
     * Delete the selected bone track from the animation.
     */
    public void deleteTrack() {
        float duration = getDuration();
        Animation newAnimation = new Animation(loadedName, duration);

        Track selectedTrack = cgm.getTrack().find();
        Animation oldAnimation = getReal();
        Track[] oldTracks = oldAnimation.getTracks();
        for (Track track : oldTracks) {
            if (track != selectedTrack) {
                Track newTrack = track.clone();
                newAnimation.addTrack(newTrack);
            }
        }

        editableCgm.replace(oldAnimation, newAnimation,
                "delete a track from an animation");
    }

    /**
     * Find the index of the loaded animation.
     *
     * @return index, or -1 if bind pose/retargeted pose/not found
     */
    public int findIndex() {
        int index = -1;
        if (isReal()) {
            List<String> nameList
                    = cgm.getAnimControl().listRealAnimationsSorted();
            index = nameList.indexOf(loadedName);
        }

        return index;
    }

    /**
     * Find the keyframe with the latest time in the loaded animation.
     *
     * @return track time (in seconds, &ge;0)
     */
    public float findLatestKeyframe() {
        Animation realAnimation = getReal();
        float latest = MyAnimation.findLastKeyframe(realAnimation);

        assert latest >= 0f : latest;
        return latest;
    }

    /**
     * Find the indexed spatial track.
     *
     * @param spatialTrackIndex which spatial track (&ge;0)
     * @return the pre-existing instance, or null if not found
     */
    SpatialTrack findSpatialTrack(int spatialTrackIndex) {
        Validate.nonNegative(spatialTrackIndex, "spatial track index");

        SpatialTrack result = null;
        Animation realAnimation = getReal();
        if (realAnimation != null) {
            result = MaudUtil.findSpatialTrack(realAnimation,
                    spatialTrackIndex);
        }

        return result;
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
        Animation realAnimation = getReal();
        if (realAnimation != null) {
            result = MyAnimation.findBoneTrack(realAnimation, boneIndex);
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
        Animation realAnimation = getReal();
        if (realAnimation == null) {
            result = 0f;
        } else {
            result = realAnimation.getLength();
        }

        assert result >= 0f : result;
        return result;
    }

    /**
     * Read the name of the loaded animation.
     *
     * @return the name, or bindPoseName if in bind pose, or retargetedPoseName
     * if in retargeted pose (not null)
     */
    public String getName() {
        assert loadedName != null;
        return loadedName;
    }

    /**
     * Access the real animation.
     *
     * @return the pre-existing instance, or null if none or in bind/retargeted
     * pose
     */
    Animation getReal() {
        Animation result = null;
        if (isReal()) {
            result = cgm.getAnimControl().getAnimation(loadedName);
        }

        return result;
    }

    /**
     * Read the animation time for playback.
     *
     * @return seconds since start (&ge;0)
     */
    public float getTime() {
        assert currentTime >= 0f : currentTime;
        return currentTime;
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
        Animation realAnimation = getReal();
        if (realAnimation != null) {
            result = MyAnimation.hasTrackForBone(realAnimation, boneIndex);
        }

        return result;
    }

    /**
     * Access the spatial track (if any) for the specified spatial.
     *
     * @param spatial which spatial to find (unaffected)
     * @return the pre-existing instance, or null if none found
     */
    SpatialTrack findTrackForSpatial(Spatial spatial) {
        SpatialTrack result = null;
        AnimControl animControl = cgm.getAnimControl().find();
        Animation realAnimation = getReal();
        if (animControl != null && realAnimation != null) {
            result = MyAnimation.findSpatialTrack(animControl, realAnimation,
                    spatial);
        }

        return result;
    }

    /**
     * Insert a keyframe (or replace the existing keyframe) in each bone track
     * at the current animation time, to match the displayed pose.
     */
    public void insertKeyframes() {
        float duration = getDuration();
        Animation newAnimation = new Animation(loadedName, duration);
        Pose pose = cgm.getPose().get();

        Animation oldAnimation = getReal();
        Track[] oldTracks = oldAnimation.getTracks();
        for (Track track : oldTracks) { // TODO add more tracks?
            Track newTrack;
            if (track instanceof BoneTrack) {
                BoneTrack boneTrack = (BoneTrack) track;
                int boneIndex = boneTrack.getTargetBoneIndex();
                Transform user = pose.userTransform(boneIndex, null);
                int frameIndex
                        = MyAnimation.findKeyframeIndex(boneTrack, currentTime);
                if (frameIndex == -1) {
                    newTrack = TrackEdit.insertKeyframe(boneTrack, currentTime,
                            user);
                } else {
                    newTrack = TrackEdit.replaceKeyframe(boneTrack, frameIndex,
                            user);
                }
            } else {
                newTrack = track.clone();
            }
            newAnimation.addTrack(newTrack);
        }

        editableCgm.replace(oldAnimation, newAnimation,
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
     * Test whether the animation time is changing.
     *
     * @return true time is changing, false otherwise
     */
    public boolean isMoving() {
        boolean running;
        if (!cgm.isLoaded()) {
            running = false;
        } else if (cgm.getPlay().isPaused()) {
            running = false;
        } else if (cgm.getPlay().getSpeed() == 0f) {
            running = false;
        } else {
            running = true;
        }

        return running;
    }

    /**
     * Test whether the root bones are pinned to bind transform.
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
        boolean result;
        if (cgm.isLoaded()) {
            if (loadedName.equals(bindPoseName)) {
                result = false;
            } else if (loadedName.equals(retargetedPoseName)) {
                result = false;
            } else {
                result = true;
            }
        } else {
            result = false;
        }

        return result;
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
     * Enumerate all tracks in the loaded (real) animation.
     *
     * @return a list of new track items
     */
    List<TrackItem> listTracks() {
        assert isReal();

        SelectedAnimControl sac = cgm.getAnimControl();
        AnimControl animControl = sac.find();
        String controlName = sac.name();

        Animation realAnimation = getReal();
        Track[] tracks = realAnimation.getTracks();
        List<TrackItem> result = new ArrayList<>(tracks.length);
        for (Track track : tracks) {
            TrackItem item = new TrackItem(loadedName, controlName, animControl,
                    track);
            result.add(item);
        }

        return result;
    }

    /**
     * Enumerate bones that have tracks in the loaded animation.
     *
     * @return a new list of names, in arbitrary order
     */
    public List<String> listTrackedBones() {
        int numTracks = countTracks();
        List<String> result = new ArrayList<>(numTracks);
        Animation realAnimation = getReal();
        if (realAnimation != null) {
            AnimControl animControl = cgm.getAnimControl().find();
            Track[] tracks = realAnimation.getTracks();
            for (Track track : tracks) {
                if (track instanceof BoneTrack) {
                    BoneTrack boneTrack = (BoneTrack) track;
                    String name
                            = MyAnimation.getTargetName(boneTrack, animControl);
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
            loadBindPose();
        } else if (name.equals(retargetedPoseName)) {
            loadRetargetedPose();
        } else { // a real animation
            float duration = cgm.getAnimControl().getDuration(name);
            float playSpeed;
            if (duration == 0f) {
                /*
                 * The animation consists of a single frame: set speed to zero.
                 */
                playSpeed = 0f;
            } else {
                /*
                 * Start the animation at normal speed.
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
        cgm.getPlay().setSpeed(newSpeed);
        currentTime = 0f;

        boolean frozen = cgm.getPose().isFrozen();
        if (!frozen) {
            cgm.getPose().setToAnimation();
        }
    }

    /**
     * Load the bind pose.
     */
    public void loadBindPose() {
        loadedName = bindPoseName;
        cgm.getPlay().setSpeed(0f);
        currentTime = 0f;

        Skeleton skeleton = cgm.getSkeleton().find();
        cgm.getPose().resetToBind(skeleton);
    }

    /**
     * Load the next animation in name-sorted order.
     */
    public void loadNext() {
        if (cgm.isLoaded() && isReal()) {
            List<String> nameList
                    = cgm.getAnimControl().listRealAnimationsSorted();
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
        if (cgm.isLoaded() && isReal()) {
            List<String> nameList
                    = cgm.getAnimControl().listRealAnimationsSorted();
            int index = nameList.indexOf(loadedName);
            int numAnimations = nameList.size();
            int prevIndex = MyMath.modulo(index - 1, numAnimations);
            String prevName = nameList.get(prevIndex);
            load(prevName);
        }
    }

    /**
     * Load retargeted pose.
     */
    public void loadRetargetedPose() {
        if (Maud.getModel().getSource().isLoaded()
                && cgm.getSkeleton().isSelected()) {
            loadedName = retargetedPoseName;
            cgm.getPlay().setSpeed(0f);
            currentTime = 0f;
            cgm.getPose().setToAnimation();
            cgm.getPose().setFrozen(false);
        }
    }

    /**
     * Add a copy of the loaded animation to the C-G model.
     *
     * @param animationName name for the new animation (not null, not reserved,
     * not in use)
     */
    public void newCopy(String animationName) {
        Validate.nonEmpty(animationName, "animation name");
        assert !isReserved(animationName) : animationName;
        SelectedAnimControl sac = cgm.getAnimControl();
        assert !sac.hasRealAnimation(animationName) : animationName;

        Animation oldAnimation = getReal();
        float duration = getDuration();
        Animation copyAnim = new Animation(animationName, duration);
        if (oldAnimation != null) {
            Track[] oldTracks = oldAnimation.getTracks();
            for (Track track : oldTracks) {
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
     * Reduce all bone/spatial tracks in the loaded animation by the specified
     * factor.
     *
     * @param factor reduction factor (&ge;2)
     */
    public void reduce(int factor) {
        Validate.inRange(factor, "reduction factor", 2, Integer.MAX_VALUE);
        assert isReal();

        float duration = getDuration();
        Animation newAnimation = new Animation(loadedName, duration);

        Animation oldAnimation = getReal();
        Track[] oldTracks = oldAnimation.getTracks();
        for (Track track : oldTracks) {
            Track clone;
            if (track instanceof BoneTrack || track instanceof SpatialTrack) {
                clone = TrackEdit.reduce(track, factor);
            } else {
                clone = track.clone(); // TODO other track types
            }
            newAnimation.addTrack(clone);
        }

        editableCgm.replace(oldAnimation, newAnimation,
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
        SelectedAnimControl sac = cgm.getAnimControl();
        assert !sac.hasRealAnimation(newName) : newName;
        assert isReal();

        float duration = getDuration();
        Animation newAnimation = new Animation(newName, duration);

        Animation oldAnimation = getReal();
        Track[] oldTracks = oldAnimation.getTracks();
        for (Track track : oldTracks) {
            Track clone = track.clone();
            newAnimation.addTrack(clone);
        }

        loadedName = newName;
        editableCgm.replace(oldAnimation, newAnimation, "rename an animation");
    }

    /**
     * Reverse all bone/spatial tracks.
     */
    public void reverse() {
        float duration = getDuration();
        Animation newAnimation = new Animation(loadedName, duration);

        Animation oldAnimation = getReal();
        Track[] oldTracks = oldAnimation.getTracks();
        for (Track track : oldTracks) {
            Track newTrack;
            if (track instanceof BoneTrack || track instanceof SpatialTrack) {
                newTrack = TrackEdit.reverse(track);
            } else {
                newTrack = track.clone(); // TODO other track types
            }
            newAnimation.addTrack(newTrack);
        }

        String name = oldAnimation.getName();
        String description = String.format("reverse the %s animation",
                MyString.quote(name));
        editableCgm.replace(oldAnimation, newAnimation, description);
    }

    /**
     * Resample all bone/spatial tracks in the loaded animation at the specified
     * rate.
     *
     * @param sampleRate sample rate (in frames per second, &gt;0)
     */
    public void resampleAtRate(float sampleRate) {
        Validate.positive(sampleRate, "sample rate");
        assert isReal();

        float duration = getDuration();
        Animation newAnimation = new Animation(loadedName, duration);
        TweenTransforms techniques = Maud.getModel().getTweenTransforms();

        Animation oldAnimation = getReal();
        Track[] oldTracks = oldAnimation.getTracks();
        for (Track track : oldTracks) {
            Track clone;
            if (track instanceof BoneTrack || track instanceof SpatialTrack) {
                clone = techniques.resampleAtRate(track, sampleRate, duration);
            } else {
                clone = track.clone(); // TODO other track types
            }
            newAnimation.addTrack(clone);
        }

        String name = oldAnimation.getName();
        String description = String.format("resample the %s animation",
                MyString.quote(name));
        editableCgm.replace(oldAnimation, newAnimation, description);
    }

    /**
     * Resample all bone/spatial tracks in the loaded animation to the specified
     * number of samples.
     *
     * @param numSamples number of samples (&ge;2)
     */
    public void resampleToNumber(int numSamples) {
        Validate.inRange(numSamples, "number of samples", 2, Integer.MAX_VALUE);
        assert isReal();

        float duration = getDuration();
        assert duration > 0f : duration;
        Animation newAnimation = new Animation(loadedName, duration);
        TweenTransforms techniques = Maud.getModel().getTweenTransforms();

        Animation oldAnimation = getReal();
        Track[] oldTracks = oldAnimation.getTracks();
        for (Track track : oldTracks) {
            Track clone;
            if (track instanceof BoneTrack || track instanceof SpatialTrack) {
                clone = techniques.resampleToNumber(track, numSamples,
                        duration);
            } else {
                clone = track.clone(); // TODO other track types
            }
            newAnimation.addTrack(clone);
        }

        String name = oldAnimation.getName();
        String description = String.format("resample the %s animation",
                MyString.quote(name));
        editableCgm.replace(oldAnimation, newAnimation, description);
    }

    /**
     * Select the named keyframe in the selected bone track.
     *
     * @param name name of the new selection (not null)
     */
    public void selectKeyframe(String name) {
        Validate.nonNull(name, "keyframe name");
        assert cgm.getTrack().isTrackSelected();

        float newTime = Float.valueOf(name);
        // TODO validate
        setTime(newTime);
    }

    /**
     * Alter which C-G model contains the animation.
     *
     * @param newCgm (not null, aliases created)
     */
    void setCgm(Cgm newCgm) {
        assert newCgm != null;
        assert newCgm.getAnimation() == this;

        cgm = newCgm;
        if (newCgm instanceof EditableCgm) {
            editableCgm = (EditableCgm) newCgm;
        } else {
            editableCgm = null;
        }
    }

    /**
     * Expand or compress the loaded animation to give it the specified
     * duration.
     *
     * @param newDuration (in seconds, &ge;0)
     */
    public void setDurationProportional(float newDuration) {
        Validate.nonNegative(newDuration, "new duration");

        Animation oldAnimation = getReal();
        float oldDuration = oldAnimation.getLength();
        if (oldDuration != newDuration) {
            Animation newAnimation = new Animation(loadedName, newDuration);
            Track[] oldTracks = oldAnimation.getTracks();
            for (Track track : oldTracks) {
                Track newTrack = TrackEdit.setDuration(track, newDuration);
                newAnimation.addTrack(newTrack);
            }

            String verb = (newDuration < oldDuration)
                    ? "speed up" : "slow down";
            String name = oldAnimation.getName();
            String description = String.format("%s the %s animation", verb,
                    MyString.quote(name));
            editableCgm.replace(oldAnimation, newAnimation, description);
        }
    }

    /**
     * Truncate or extend the loaded animation to give it the specified
     * duration.
     *
     * @param newDuration (in seconds, &ge;0)
     */
    public void setDurationSame(float newDuration) {
        Validate.nonNegative(newDuration, "new duration");

        Animation oldAnimation = getReal();
        float oldDuration = oldAnimation.getLength();
        if (oldDuration != newDuration) {
            Animation newAnimation = new Animation(loadedName, newDuration);
            Track[] oldTracks = oldAnimation.getTracks();
            for (Track track : oldTracks) {
                Track newTrack;
                if (track instanceof BoneTrack
                        || track instanceof SpatialTrack) {
                    newTrack = TrackEdit.truncate(track, newDuration);
                } else {
                    newTrack = track.clone(); // TODO other track types
                }
                newAnimation.addTrack(newTrack);
            }

            String verb = (newDuration < oldDuration) ? "truncate" : "extend";
            String name = oldAnimation.getName();
            String description = String.format("%s the %s animation", verb,
                    MyString.quote(name));
            editableCgm.replace(oldAnimation, newAnimation, description);
        }
    }

    /**
     * Alter whether the root bones are pinned to bind transform.
     *
     * @param newSetting true &rarr; pinned, false &rarr; free to translate
     */
    public void setPinned(boolean newSetting) {
        pinnedFlag = newSetting;
    }

    /**
     * Alter the animation time. Update the pose unless it's frozen. Has no
     * effect in bind pose or if the loaded animation has zero duration.
     *
     * @param newTime seconds since start (&ge;0, &le;duration)
     */
    public void setTime(float newTime) {
        float duration = getDuration();
        Validate.inRange(newTime, "new time", 0f, duration);

        if (duration > 0f) {
            currentTime = newTime;
            boolean frozen = cgm.getPose().isFrozen();
            if (!frozen) {
                cgm.getPose().setToAnimation();
            }
        }
    }

    /**
     * Delete any optional track components that consist entirely of identities,
     * as well as any tracks for bones without influence.
     */
    public void simplify() {
        SelectedSkeleton ss = cgm.getSkeleton();
        Skeleton skeleton = ss.find();
        Spatial subtree = ss.findSpatial();
        BitSet influencers
                = MaudUtil.addAllInfluencers(subtree, skeleton, null);

        float duration = getDuration();
        Animation newAnimation = new Animation(loadedName, duration);

        Animation oldAnimation = getReal();
        Track[] oldTracks = oldAnimation.getTracks();
        for (Track track : oldTracks) {
            Track newTrack = null;
            if (track instanceof BoneTrack) {
                BoneTrack boneTrack = (BoneTrack) track;
                int boneIndex = boneTrack.getTargetBoneIndex();
                if (influencers.get(boneIndex)) {
                    newTrack = TrackEdit.simplify(track);
                }
            } else if (track instanceof SpatialTrack) {
                newTrack = TrackEdit.simplify(track);
            } else {
                newTrack = track.clone();
            }
            if (newTrack != null) {
                newAnimation.addTrack(newTrack);
            }
        }

        String name = oldAnimation.getName();
        String description = String.format("simplify the %s animation",
                MyString.quote(name));
        editableCgm.replace(oldAnimation, newAnimation, description);
    }

    /**
     * Delete everything after the current animation time and make that the end
     * of the animation.
     */
    public void truncate() {
        Animation newAnimation = new Animation(loadedName, currentTime);
        Animation oldAnimation = getReal();
        Track[] oldTracks = oldAnimation.getTracks();
        for (Track track : oldTracks) {
            Track newTrack;
            if (track instanceof BoneTrack || track instanceof SpatialTrack) {
                newTrack = TrackEdit.truncate(track, currentTime);
            } else {
                newTrack = track.clone(); // TODO other track types
            }
            newAnimation.addTrack(newTrack);
        }

        String name = oldAnimation.getName();
        String description = String.format("truncate the %s animation",
                MyString.quote(name));
        editableCgm.replace(oldAnimation, newAnimation, description);
    }

    /**
     * Alter each track's 1st keyframe and end-time keyframe so that they
     * precisely match. If a track doesn't end with a keyframe, append one.
     *
     * @param endWeight how much weight to give to pre-existing end-time
     * keyframes, if any exist (&ge;0, &le;1)
     */
    public void wrapAllTracks(float endWeight) {
        Animation oldAnimation = getReal();
        float duration = oldAnimation.getLength();
        Animation newAnimation = new Animation(loadedName, duration);
        Track[] oldTracks = oldAnimation.getTracks();
        for (Track track : oldTracks) {
            Track newTrack;
            if (track instanceof BoneTrack || track instanceof SpatialTrack) {
                newTrack = TrackEdit.wrap(track, duration, endWeight);
            } else {
                newTrack = track.clone(); // TODO other track types
            }
            newAnimation.addTrack(newTrack);
        }

        String name = oldAnimation.getName();
        String description = String.format(
                "wrap all tracks in the %s animation using end weight=%f",
                MyString.quote(name), endWeight);
        editableCgm.replace(oldAnimation, newAnimation, description);
    }
    // *************************************************************************
    // Object methods

    /**
     * Create a deep copy of this object.
     *
     * @return a new object, equivalent to this one
     * @throws CloneNotSupportedException if the superclass isn't cloneable
     */
    @Override
    public LoadedAnimation clone() throws CloneNotSupportedException {
        LoadedAnimation clone = (LoadedAnimation) super.clone();
        return clone;
    }
    // *************************************************************************
    // private methods

    /**
     * Add a new pose animation to the C-G model. The new animation has zero
     * duration, a single keyframe at t=0, and all the tracks are BoneTracks,
     * set to the current pose.
     *
     * @param animationName name for the new animation (not null, not reserved,
     * not in use)
     */
    private void newPose(String animationName) {
        assert animationName != null;
        assert !isReserved(animationName) : animationName;
        SelectedAnimControl sac = cgm.getAnimControl();
        assert !sac.hasRealAnimation(animationName) : animationName;

        Pose pose = cgm.getPose().get();
        Animation poseAnim = pose.capture(animationName);
        editableCgm.addAnimation(poseAnim);
    }
}
