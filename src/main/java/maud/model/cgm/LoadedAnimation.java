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
     * dummy animation name to denote the bind pose (no real animation loaded)
     */
    final public static String bindPoseName = "( bind pose )";
    /**
     * dummy animation name to denote retargeted pose (no real animation loaded)
     */
    final public static String retargetedPoseName = "( retargeted pose )";
    // *************************************************************************
    // fields

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
        float neckTime = cgm.getPlay().getTime();
        assert neckTime > 0f : neckTime;

        Animation oldAnimation = getReal();
        float oldDuration = oldAnimation.getLength();
        float newDuration = oldDuration - neckTime;
        Animation newAnimation = new Animation(loadedName, newDuration);
        Track newSelectedTrack = null;
        TweenTransforms techniques = Maud.getModel().getTweenTransforms();
        Track oldSelectedTrack = cgm.getTrack().get();
        Track[] oldTracks = oldAnimation.getTracks();
        for (Track track : oldTracks) {
            Track newTrack;
            if (track instanceof BoneTrack || track instanceof SpatialTrack) {
                Transform neck = techniques.interpolate(neckTime, track,
                        oldDuration, null, null);
                newTrack = TrackEdit.behead(track, neckTime, neck, oldDuration);
            } else {
                newTrack = track.clone(); // TODO other track types
            }
            if (track == oldSelectedTrack) {
                newSelectedTrack = newTrack;
            }
            newAnimation.addTrack(newTrack);
        }

        String eventDescription = String.format(
                "behead the %s animation at t=%f",
                MyString.quote(loadedName), neckTime);
        editableCgm.replace(oldAnimation, newAnimation, eventDescription,
                newSelectedTrack);
        load(loadedName);
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
                float time = cgm.getPlay().getTime();
                float duration = getDuration();
                techniques.transform(track, time, duration, null, storeResult);
            }
        }

        return storeResult;
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
        float atTime = cgm.getPlay().getTime();
        assert atTime > 0f : atTime;

        float duration = getDuration();
        Animation newAnimation = new Animation(loadedName, duration);
        Track newSelectedTrack = null;
        Track oldSelectedTrack = cgm.getTrack().get();
        int numDeletions = 0;

        Animation realAnimation = getReal();
        Track[] tracks = realAnimation.getTracks();
        for (Track track : tracks) {
            Track newTrack;
            if (track instanceof BoneTrack || track instanceof SpatialTrack) {
                int keyframeIndex
                        = MyAnimation.findKeyframeIndex(track, atTime);
                if (keyframeIndex >= 1) {
                    newTrack = TrackEdit.deleteRange(track, keyframeIndex, 1);
                    ++numDeletions;
                } else {
                    newTrack = track.clone();
                }
            } else {
                newTrack = track.clone(); // TODO other track types
            }
            if (track == oldSelectedTrack) {
                newSelectedTrack = newTrack;
            }
            newAnimation.addTrack(newTrack);
        }

        if (numDeletions > 0) {
            String eventDescription = String.format(
                    "delete %d keyframes at t=%f from the %s animation",
                    numDeletions, atTime, MyString.quote(loadedName));
            editableCgm.replace(realAnimation, newAnimation, eventDescription,
                    newSelectedTrack);
        }
    }

    /**
     * Delete the selected track from the animation.
     */
    public void deleteTrack() {
        float duration = getDuration();
        Animation newAnimation = new Animation(loadedName, duration);

        SelectedTrack selectedTrack = cgm.getTrack();
        Animation oldAnimation = getReal();
        Track[] oldTracks = oldAnimation.getTracks();
        for (Track track : oldTracks) {
            if (track != selectedTrack.get()) {
                Track newTrack = track.clone();
                newAnimation.addTrack(newTrack);
            }
        }

        String trackName = selectedTrack.describe();
        String eventDescription = String.format(
                "delete the %s track from the %s animation",
                trackName, MyString.quote(loadedName));
        editableCgm.replace(oldAnimation, newAnimation, eventDescription, null);
    }

    /**
     * Describe the track for the indexed bone.
     *
     * @param boneIndex which bone (&ge;0)
     * @return a textual description if the track exists, otherwise null
     */
    public String describeBoneTrack(int boneIndex) {
        Validate.nonNegative(boneIndex, "bone index");

        String result = null;
        Animation realAnimation = getReal();
        if (realAnimation != null) {
            BoneTrack boneTrack
                    = MyAnimation.findBoneTrack(realAnimation, boneIndex);
            if (boneTrack != null) {
                SelectedAnimControl sac = cgm.getAnimControl();
                AnimControl animControl = sac.find();
                result = MyAnimation.describe(boneTrack, animControl);
            }
        }

        return result;
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
     * Access the spatial track (if any) for the specified target.
     *
     * @param target which spatial to find (unaffected)
     * @return the pre-existing instance, or null if none found
     */
    SpatialTrack findTrackForSpatial(Spatial target) {
        SpatialTrack result = null;
        AnimControl animControl = cgm.getAnimControl().find();
        Animation realAnimation = getReal();
        if (animControl != null && realAnimation != null) {
            result = MyAnimation.findSpatialTrack(animControl, realAnimation,
                    target);
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
     * Test whether the animation includes a track that exactly matches the
     * specified description.
     *
     * @param description from TrackItem.describe() (not null, not empty)
     * @return true if found, otherwise false
     */
    public boolean hasTrack(String description) {
        Validate.nonEmpty(description, "description");

        boolean result = false;
        List<TrackItem> items = listTracks();
        for (TrackItem item : items) { // TODO less brute force
            if (item.describe().equals(description)) {
                result = true;
                break;
            }
        }

        return result;
    }

    /**
     * Test whether the animation includes the specified track. TODO use
     * MyAnimation.findTrackIndex()
     *
     * @param track (may be null)
     * @return true if found, otherwise false
     */
    boolean hasTrack(Track track) {
        boolean result = false;
        Animation real = getReal();
        Track[] tracks = real.getTracks();
        for (Track t : tracks) {
            if (t == track) {
                result = true;
                break;
            }
        }

        return result;
    }

    /**
     * Test whether the animation includes a bone track for the indexed bone.
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
     * Insert a keyframe (or replace the existing keyframe) in each bone track
     * at the current animation time, to match the displayed pose.
     */
    public void insertKeyframes() {
        float atTime = cgm.getPlay().getTime();
        float duration = getDuration();
        Animation newAnimation = new Animation(loadedName, duration);
        Track newSelectedTrack = null;
        Track oldSelectedTrack = cgm.getTrack().get();

        Animation oldAnimation = getReal();
        Track[] oldTracks = oldAnimation.getTracks();
        for (Track track : oldTracks) { // TODO add more tracks?
            Track newTrack;
            if (track instanceof BoneTrack) {
                BoneTrack boneTrack = (BoneTrack) track;
                int boneIndex = boneTrack.getTargetBoneIndex();
                Pose pose = cgm.getPose().get();
                Transform user = pose.userTransform(boneIndex, null);
                int frameIndex
                        = MyAnimation.findKeyframeIndex(boneTrack, atTime);
                if (frameIndex == -1) {
                    newTrack
                            = TrackEdit.insertKeyframe(boneTrack, atTime, user);
                } else {
                    newTrack = TrackEdit.replaceKeyframe(boneTrack, frameIndex,
                            user);
                }
            } else {
                newTrack = track.clone();
            }
            if (track == oldSelectedTrack) {
                newSelectedTrack = newTrack;
            }
            newAnimation.addTrack(newTrack);
        }

        String eventDescription = String.format(
                "insert keyframes into the %s animation at t=%f",
                MyString.quote(loadedName), atTime);
        editableCgm.replace(oldAnimation, newAnimation, eventDescription,
                newSelectedTrack);
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
     * Test whether a real animation is loaded. TODO simplify
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
     * Enumerate all tracks in the animation.
     *
     * @return a new list of new track items
     */
    public List<TrackItem> listTracks() {
        List<TrackItem> result;
        Animation realAnimation = getReal();
        if (realAnimation != null) {
            SelectedAnimControl sac = cgm.getAnimControl();
            AnimControl animControl = sac.find();
            String controlName = sac.name();

            Track[] tracks = realAnimation.getTracks();
            result = new ArrayList<>(tracks.length);
            for (Track track : tracks) {
                TrackItem item = new TrackItem(loadedName, controlName,
                        animControl, track);
                result.add(item);
            }

        } else {
            result = new ArrayList<>(0);
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
            loadReal(name, playSpeed);
        }
    }

    /**
     * Load the bind pose.
     */
    public void loadBindPose() {
        loadedName = bindPoseName;
        cgm.getPlay().resetLimits();
        cgm.getPlay().setSpeed(0f);
        cgm.getPlay().setTime(0f);
        cgm.getTrack().select(null);

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
            cgm.getPlay().resetLimits();
            cgm.getPlay().setSpeed(0f);
            cgm.getPlay().setTime(0f);
            cgm.getTrack().select(null);
            cgm.getPose().setToAnimation();
            cgm.getPose().setFrozen(false);
        }
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
        Track newSelectedTrack = null;
        Track oldSelectedTrack = cgm.getTrack().get();

        Animation oldAnimation = getReal();
        Track[] oldTracks = oldAnimation.getTracks();
        for (Track track : oldTracks) {
            Track newTrack;
            if (track instanceof BoneTrack || track instanceof SpatialTrack) {
                newTrack = TrackEdit.reduce(track, factor);
            } else {
                newTrack = track.clone(); // TODO other track types
            }
            if (track == oldSelectedTrack) {
                newSelectedTrack = newTrack;
            }
            newAnimation.addTrack(newTrack);
        }

        String eventDescription = String.format(
                "thin the %s animation by %dx", MyString.quote(loadedName),
                factor);
        editableCgm.replace(oldAnimation, newAnimation, eventDescription,
                newSelectedTrack);
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
        Track newSelectedTrack = null;
        Track oldSelectedTrack = cgm.getTrack().get();

        Animation oldAnimation = getReal();
        Track[] oldTracks = oldAnimation.getTracks();
        for (Track track : oldTracks) {
            Track clone = track.clone();
            if (track == oldSelectedTrack) {
                newSelectedTrack = clone;
            }
            newAnimation.addTrack(clone);
        }

        String eventDescription = String.format("rename the %s animation to %s",
                MyString.quote(loadedName), MyString.quote(newName));
        editableCgm.replace(oldAnimation, newAnimation, eventDescription,
                newSelectedTrack);
        loadedName = newName;
    }

    /**
     * Reverse all bone/spatial tracks.
     */
    public void reverse() {
        float duration = getDuration();
        Animation newAnimation = new Animation(loadedName, duration);
        Track newSelectedTrack = null;
        Track oldSelectedTrack = cgm.getTrack().get();

        Animation oldAnimation = getReal();
        Track[] oldTracks = oldAnimation.getTracks();
        for (Track track : oldTracks) {
            Track newTrack;
            if (track instanceof BoneTrack || track instanceof SpatialTrack) {
                newTrack = TrackEdit.reverse(track);
            } else {
                newTrack = track.clone(); // TODO other track types
            }
            if (track == oldSelectedTrack) {
                newSelectedTrack = newTrack;
            }
            newAnimation.addTrack(newTrack);
        }

        String eventDescription = String.format("reverse the %s animation",
                MyString.quote(loadedName));
        editableCgm.replace(oldAnimation, newAnimation, eventDescription,
                newSelectedTrack);
        load(loadedName);
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
        Track newSelectedTrack = null;
        TweenTransforms techniques = Maud.getModel().getTweenTransforms();
        Track oldSelectedTrack = cgm.getTrack().get();

        Animation oldAnimation = getReal();
        Track[] oldTracks = oldAnimation.getTracks();
        for (Track track : oldTracks) {
            Track newTrack;
            if (track instanceof BoneTrack || track instanceof SpatialTrack) {
                newTrack = techniques.resampleAtRate(track, sampleRate,
                        duration);
            } else {
                newTrack = track.clone(); // TODO other track types
            }
            if (track == oldSelectedTrack) {
                newSelectedTrack = newTrack;
            }
            newAnimation.addTrack(newTrack);
        }

        String eventDescription = String.format(
                "resample the %s animation at %f FPS",
                MyString.quote(loadedName), sampleRate);
        editableCgm.replace(oldAnimation, newAnimation, eventDescription,
                newSelectedTrack);
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
        Track newSelectedTrack = null;
        TweenTransforms techniques = Maud.getModel().getTweenTransforms();
        Track oldSelectedTrack = cgm.getTrack().get();

        Animation oldAnimation = getReal();
        Track[] oldTracks = oldAnimation.getTracks();
        for (Track track : oldTracks) {
            Track newTrack;
            if (track instanceof BoneTrack || track instanceof SpatialTrack) {
                newTrack = techniques.resampleToNumber(track, numSamples,
                        duration);
            } else {
                newTrack = track.clone(); // TODO other track types
            }
            if (track == oldSelectedTrack) {
                newSelectedTrack = newTrack;
            }
            newAnimation.addTrack(newTrack);
        }

        String eventDescription = String.format(
                "resample the %s animation to %d keyframes",
                MyString.quote(loadedName), numSamples);
        editableCgm.replace(oldAnimation, newAnimation, eventDescription,
                newSelectedTrack);
    }

    /**
     * Select the named keyframe in the selected track.
     *
     * @param name name of the new selection (not null)
     */
    public void selectKeyframe(String name) {
        Validate.nonNull(name, "keyframe name");
        assert cgm.getTrack().isSelected();

        float newTime = Float.valueOf(name);
        // TODO validate
        cgm.getPlay().setTime(newTime);
    }

    /**
     * Alter which C-G model contains the animation. (Invoked only during
     * initialization and cloning.)
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
            Track newSelectedTrack = null;
            Track oldSelectedTrack = cgm.getTrack().get();
            Track[] oldTracks = oldAnimation.getTracks();
            for (Track track : oldTracks) {
                Track newTrack = TrackEdit.setDuration(track, newDuration);
                if (track == oldSelectedTrack) {
                    newSelectedTrack = newTrack;
                }
                newAnimation.addTrack(newTrack);
            }

            float factor;
            String verb;
            if (newDuration < oldDuration) {
                factor = oldDuration / newDuration;
                verb = "compress";
            } else {
                factor = newDuration / oldDuration;
                verb = "expand";
            }
            String eventDescription = String.format("%s the %s animation %fx",
                    verb, MyString.quote(loadedName), factor);
            editableCgm.replace(oldAnimation, newAnimation, eventDescription,
                    newSelectedTrack);
            load(loadedName);
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
            Track newSelectedTrack = null;
            Track oldSelectedTrack = cgm.getTrack().get();
            Track[] oldTracks = oldAnimation.getTracks();
            for (Track track : oldTracks) {
                Track newTrack;
                if (track instanceof BoneTrack
                        || track instanceof SpatialTrack) {
                    newTrack = TrackEdit.truncate(track, newDuration);
                } else {
                    newTrack = track.clone(); // TODO other track types
                }
                if (track == oldSelectedTrack) {
                    newSelectedTrack = newTrack;
                }
                newAnimation.addTrack(newTrack);
            }

            String verb = (newDuration < oldDuration) ? "truncate" : "extend";
            String name = oldAnimation.getName();
            String eventDescription = String.format(
                    "%s the %s animation to t=%f", verb, MyString.quote(name),
                    newDuration);
            editableCgm.replace(oldAnimation, newAnimation, eventDescription,
                    newSelectedTrack);
            load(loadedName);
        }
    }

    /**
     * Delete any optional track components that consist entirely of identities,
     * as well as any tracks for bones with no influence.
     */
    public void simplify() {
        BitSet influencers = null;
        SelectedSkeleton ss = cgm.getSkeleton();
        if (ss.isSelected()) {
            Skeleton skeleton = ss.find();
            Spatial subtree = ss.findSpatial();
            influencers = MaudUtil.addAllInfluencers(subtree, skeleton, null);
        }

        float duration = getDuration();
        Animation newAnimation = new Animation(loadedName, duration);
        Track newSelectedTrack = null;
        Track oldSelectedTrack = cgm.getTrack().get();

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
            if (track == oldSelectedTrack) {
                newSelectedTrack = newTrack;
            }
            if (newTrack != null) {
                newAnimation.addTrack(newTrack);
            }
        }

        String eventDescription = String.format("simplify the %s animation",
                MyString.quote(loadedName));
        editableCgm.replace(oldAnimation, newAnimation, eventDescription,
                newSelectedTrack);
    }

    /**
     * Delete everything after the current animation time and make that the end
     * of the animation.
     */
    public void truncate() {
        float endTime = cgm.getPlay().getTime();
        Animation newAnimation = new Animation(loadedName, endTime);
        Track newSelectedTrack = null;
        Track oldSelectedTrack = cgm.getTrack().get();

        Animation oldAnimation = getReal();
        Track[] oldTracks = oldAnimation.getTracks();
        for (Track track : oldTracks) {
            Track newTrack;
            if (track instanceof BoneTrack || track instanceof SpatialTrack) {
                newTrack = TrackEdit.truncate(track, endTime);
            } else {
                newTrack = track.clone(); // TODO other track types
            }
            if (track == oldSelectedTrack) {
                newSelectedTrack = newTrack;
            }
            newAnimation.addTrack(newTrack);
        }

        String eventDescription = String.format(
                "truncate the %s animation at t=%f", MyString.quote(loadedName),
                endTime);
        editableCgm.replace(oldAnimation, newAnimation, eventDescription,
                newSelectedTrack);
        load(loadedName);
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
        Track newSelectedTrack = null;
        Track oldSelectedTrack = cgm.getTrack().get();

        Track[] oldTracks = oldAnimation.getTracks();
        for (Track track : oldTracks) {
            Track newTrack;
            if (track instanceof BoneTrack || track instanceof SpatialTrack) {
                newTrack = TrackEdit.wrap(track, duration, endWeight);
            } else {
                newTrack = track.clone(); // TODO other track types
            }
            if (track == oldSelectedTrack) {
                newSelectedTrack = newTrack;
            }
            newAnimation.addTrack(newTrack);
        }

        String eventDescription = String.format(
                "wrap all tracks in the %s animation using end weight=%f",
                MyString.quote(loadedName), endWeight);
        editableCgm.replace(oldAnimation, newAnimation, eventDescription,
                newSelectedTrack);
    }
    // *************************************************************************
    // Cloneable methods

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
     * Load the named real animation (not bind/retargeted pose) at t=0 with the
     * specified playback speed.
     *
     * @param name which animation (not null)
     * @param newSpeed playback speed
     */
    private void loadReal(String name, float newSpeed) {
        assert name != null;
        assert !isReserved(name);

        if (!name.equals(loadedName)) {
            loadedName = name;
            cgm.getPlay().resetLimits();
            cgm.getPlay().setTime(0f);
            cgm.getTrack().select(null);
        }
        cgm.getPlay().setSpeed(newSpeed);

        boolean frozen = cgm.getPose().isFrozen();
        if (!frozen) {
            cgm.getPose().setToAnimation();
        }
    }
}
