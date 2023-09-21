/*
 Copyright (c) 2017-2023, Stephen Gold
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

import com.jme3.anim.AnimClip;
import com.jme3.anim.AnimComposer;
import com.jme3.anim.AnimTrack;
import com.jme3.anim.Armature;
import com.jme3.anim.Joint;
import com.jme3.anim.MorphTrack;
import com.jme3.anim.TransformTrack;
import com.jme3.anim.util.HasLocalTransform;
import com.jme3.animation.AnimControl;
import com.jme3.animation.Animation;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SpatialTrack;
import com.jme3.animation.Track;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.InfluenceUtil;
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
        Object[] tracks = getTracks();
        if (tracks != null) {
            float duration = duration();
            for (Object track : tracks) {
                int endIndex = MaudUtil.findKeyframeIndex(track, duration);
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

        Object newAnim;
        Object newSelectedTrack = null;

        Object oldAnim = getReal();
        Object oldSelectedTrack = cgm.getTrack().get();
        Object[] oldTracks = getTracks();
        if (oldAnim instanceof Animation) { // old animation system
            float oldDuration = duration();
            float newDuration = oldDuration - neckTime;
            Animation newAnimation = new Animation(loadedName, newDuration);
            TweenTransforms techniques = Maud.getModel().getTweenTransforms();
            for (Object trk : oldTracks) {
                Track oldTrack = (Track) trk;
                Track newTrack;
                if (trk instanceof BoneTrack || trk instanceof SpatialTrack) {
                    Transform neckTransform = techniques.interpolate(
                            neckTime, oldTrack, oldDuration, null, null);
                    newTrack = TrackEdit.behead(
                            oldTrack, neckTime, neckTransform, oldDuration);
                } else { // TODO other track types
                    newTrack = oldTrack.clone();
                }
                if (trk == oldSelectedTrack) {
                    newSelectedTrack = newTrack;
                }
                newAnimation.addTrack(newTrack);
            }
            newAnim = newAnimation;

        } else { // new animation system
            int numTracks = oldTracks.length;
            AnimTrack<?>[] newTracks = new AnimTrack<?>[numTracks];
            AnimClip newClip = new AnimClip(loadedName);
            for (int i = 0; i < numTracks; ++i) {
                AnimTrack<?> trk = (AnimTrack<?>) oldTracks[i];
                if (trk instanceof TransformTrack) {
                    TransformTrack oldTrack = (TransformTrack) trk;
                    Transform neckTransform = new Transform();
                    oldTrack.getDataAtTime(neckTime, neckTransform);
                    newTracks[i] = TrackEdit.behead(
                            oldTrack, neckTime, neckTransform);
                } else if (trk instanceof MorphTrack) {
                    MorphTrack oldTrack = (MorphTrack) trk;
                    int numTargets = oldTrack.getNbMorphTargets();
                    float[] neckWeights = new float[numTargets];
                    oldTrack.getDataAtTime(neckTime, neckWeights);
                    newTracks[i]
                            = TrackEdit.behead(oldTrack, neckTime, neckWeights);
                } else {
                    newTracks[i] = (AnimTrack<?>) TrackEdit.cloneTrack(trk);
                }
                if (trk == oldSelectedTrack) {
                    newSelectedTrack = newTracks[i];
                }
            }
            newClip.setTracks(newTracks);
            newAnim = newClip;
        }

        String eventDescription = String.format(
                "behead the %s animation at t=%f", MyString.quote(loadedName),
                neckTime);
        editableCgm.replace(
                oldAnim, newAnim, eventDescription, newSelectedTrack);
        load(loadedName);
    }

    /**
     * Determine the current user/animation transform of the indexed bone/joint.
     *
     * @param boneIndex the index of the subject bone/joint (&ge;0)
     * @param storeResult storage for the result (modified if not null)
     * @return transform (either storeResult or a new instance)
     */
    public Transform boneTransform(int boneIndex, Transform storeResult) {
        Transform result
                = (storeResult == null) ? new Transform() : storeResult;

        EditorModel model = Maud.getModel();
        Object oldAnim = getReal();
        if (oldAnim == null) {
            if (isRetargetedPose()) {
                EditableMap map = model.getMap();
                map.boneTransform(boneIndex, result);
            } else {
                result.loadIdentity();
            }

        } else if (oldAnim instanceof Animation) { // old animation system
            BoneTrack track
                    = MyAnimation.findBoneTrack((Animation) oldAnim, boneIndex);
            if (track == null) {
                result.loadIdentity();
            } else {
                TweenTransforms techniques = model.getTweenTransforms();
                float time = cgm.getPlay().getTime();
                float duration = duration();
                techniques.transform(track, time, duration, null, result);
            }

        } else { // new animation system
            TransformTrack track
                    = MyAnimation.findJointTrack((AnimClip) oldAnim, boneIndex);
            if (track == null) {
                result.loadIdentity();
            } else {
                double time = cgm.getPlay().getTime();
                Pose pose = cgm.getPose().get();
                Transform local = pose.bindTransform(boneIndex, null);
                track.getDataAtTime(time, local);
                pose.userForLocal(boneIndex, local, result);
            }
        }

        return result;
    }

    /**
     * Count the number of bone/joint tracks in the loaded animation.
     *
     * @return the number of tracks (&ge;0)
     */
    public int countBoneTracks() {
        int result = 0;
        Object realAnim = getReal();
        if (realAnim instanceof Animation) {
            result = MyAnimation.countTracks(
                    (Animation) realAnim, BoneTrack.class);

        } else if (realAnim instanceof AnimClip) {
            AnimTrack<?>[] tracks = ((AnimClip) realAnim).getTracks();
            for (AnimTrack<?> track : tracks) {
                if (MyAnimation.isJointTrack(track)) {
                    ++result;
                }
            }
        }

        assert result >= 0 : result;
        return result;
    }

    /**
     * Count the number of spatial tracks in the loaded animation.
     *
     * @return the number of tracks (&ge;0)
     */
    public int countSpatialTracks() {
        int result = 0;
        Object realAnim = getReal();
        if (realAnim instanceof Animation) {
            result = MyAnimation.countTracks(
                    (Animation) realAnim, SpatialTrack.class);

        } else if (realAnim instanceof AnimClip) {
            AnimTrack<?>[] tracks = ((AnimClip) realAnim).getTracks();
            for (AnimTrack<?> track : tracks) {
                if (track instanceof TransformTrack) {
                    HasLocalTransform target
                            = ((TransformTrack) track).getTarget();
                    if (target instanceof Spatial) {
                        ++result;
                    }
                }
            }
        }

        assert result >= 0 : result;
        return result;
    }

    /**
     * Count the total number of tracks in the loaded animation.
     *
     * @return the number of tracks (&ge;0)
     */
    public int countTracks() {
        int count = 0;
        Object[] tracks = getTracks();
        if (tracks != null) {
            count = tracks.length;
        }

        return count;
    }

    /**
     * Add an identity track for the selected Bone or Joint.
     */
    public void createBoneTrack() {
        Object[] tracks = getTracks();
        Object baseTrack = tracks[0]; // arbitrary choice
        float[] baseTimes = MaudUtil.getTrackTimes(baseTrack);
        Transform identity = new Transform();

        Object newTrack;
        Object realAnimation = getReal();
        if (realAnimation instanceof Animation) {
            int boneIndex = cgm.getBone().index();
            newTrack = MyAnimation.newBoneTrack(
                    boneIndex, baseTimes, identity);
        } else {
            Joint target = (Joint) cgm.getBone().get();
            int numKeyframes = baseTimes.length;
            Vector3f[] translations = new Vector3f[numKeyframes];
            Quaternion[] rotations = new Quaternion[numKeyframes];
            Vector3f[] scales = new Vector3f[numKeyframes];
            for (int i = 0; i < numKeyframes; ++i) {
                translations[i] = identity.getTranslation(); // alias
                rotations[i] = identity.getRotation(); // alias
                scales[i] = identity.getScale(); // alias
            }
            newTrack = new TransformTrack(
                    target, baseTimes, translations, rotations, scales);
        }

        String boneName = cgm.getBone().name();
        String eventDescription = String.format(
                "add an identity track to %s for bone %s",
                MyString.quote(loadedName), MyString.quote(boneName));
        editableCgm.addTrack(newTrack, eventDescription);
    }

    /**
     * Delete the loaded animation and (if successful) load bind pose.
     */
    public void delete() {
        if (isReal()) {
            editableCgm.deleteAnimation();
            loadBindPose(true);
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

        int numDeletions = 0;
        Object oldSelectedTrack = cgm.getTrack().get();
        Object newSelectedTrack = null;
        TmpTracks.clear();
        Object[] oldTracks = getTracks();
        for (Object track : oldTracks) {
            int keyframeIndex = MaudUtil.findKeyframeIndex(track, atTime);

            Object newTrack;
            if (track instanceof BoneTrack || track instanceof SpatialTrack) {
                if (keyframeIndex >= 1) {
                    newTrack = TrackEdit.deleteRange(
                            (Track) track, keyframeIndex, 1);
                    ++numDeletions;
                } else {
                    newTrack = TrackEdit.cloneTrack(track);
                }

            } else if (track instanceof TransformTrack && keyframeIndex >= 1) {
                newTrack = TrackEdit.deleteRange(
                        (TransformTrack) track, keyframeIndex, 1);
                ++numDeletions;

            } else { // TODO other track types
                newTrack = TrackEdit.cloneTrack(track);
            }

            if (track == oldSelectedTrack) {
                newSelectedTrack = newTrack;
            }
            TmpTracks.add(newTrack);
        }

        if (numDeletions > 0) {
            Object newAnim = newAnim();
            TmpTracks.addAllToAnim(newAnim);

            String eventDescription = String.format(
                    "delete %d keyframes at t=%f from the %s animation",
                    numDeletions, atTime, MyString.quote(loadedName));
            Object oldAnim = getReal();
            editableCgm.replace(
                    oldAnim, newAnim, eventDescription, newSelectedTrack);
        }
    }

    /**
     * Delete the selected track from the animation.
     */
    public void deleteTrack() {
        Object selectedTrack = cgm.getTrack().get();

        TmpTracks.clear();
        Object[] oldTracks = getTracks();
        for (Object oldTrack : oldTracks) {
            if (oldTrack != selectedTrack) {
                Object newTrack = TrackEdit.cloneTrack(oldTrack);
                TmpTracks.add(newTrack);
            }
        }

        Object newAnim = newAnim();
        TmpTracks.addAllToAnim(newAnim);

        String trackDesc = cgm.getTrack().describe();
        String eventDescription = String.format(
                "delete the %s track from the %s animation",
                trackDesc, MyString.quote(loadedName));
        Object oldAnim = getReal();
        editableCgm.replace(oldAnim, newAnim, eventDescription, null);
    }

    /**
     * Describe the track (if any) for the indexed Bone or Joint.
     *
     * @param boneIndex which Bone or Joint (&ge;0)
     * @return a textual description if a track exists, otherwise null
     */
    public String describeBoneTrack(int boneIndex) {
        Validate.nonNegative(boneIndex, "bone index");

        String result = null;
        SelectedAnimControl sac = cgm.getAnimControl();
        AbstractControl control = sac.find();
        Object realAnim = getReal();
        if (realAnim instanceof Animation) {
            BoneTrack boneTrack = MyAnimation.findBoneTrack(
                    (Animation) realAnim, boneIndex);
            if (boneTrack != null) {
                result = MyAnimation.describe(boneTrack, (AnimControl) control);
            }

        } else if (realAnim instanceof AnimClip) {
            TransformTrack transformTrack = MyAnimation.findJointTrack(
                    (AnimClip) realAnim, boneIndex);
            if (transformTrack != null) {
                result = MyAnimation.describe(transformTrack);
            }
        }

        return result;
    }

    /**
     * Read the duration of the loaded animation.
     *
     * @return the time (in seconds, &ge;0)
     */
    public float duration() {
        float result;
        Object realAnimation = getReal();
        if (realAnimation == null) {
            result = 0f;
        } else if (realAnimation instanceof AnimClip) {
            result = (float) ((AnimClip) realAnimation).getLength();
        } else {
            result = ((Animation) realAnimation).getLength();
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
     * @return the track time of the keyframe (in seconds, &ge;0)
     */
    public float findLatestKeyframe() {
        float result = 0f;
        Object[] tracks = getTracks();
        for (Object track : tracks) {
            float[] frameTimes = MaudUtil.getTrackTimes(track);
            for (float time : frameTimes) { // TODO assume ordered?
                if (time > result) {
                    result = time;
                }
            }
        }

        return result;
    }

    /**
     * Find the indexed spatial track.
     *
     * @param spatialTrackIndex which spatial track (&ge;0)
     * @return the pre-existing instance, or null if not found
     */
    Object findSpatialTrack(int spatialTrackIndex) {
        assert spatialTrackIndex >= 0 : spatialTrackIndex;

        Object result = null;
        Object realAnimation = getReal();
        if (realAnimation instanceof Animation) {
            result = MaudUtil.findSpatialTrack(
                    (Animation) realAnimation, spatialTrackIndex);

        } else if (realAnimation instanceof AnimClip) {
            int spatialTracksSeen = 0;
            AnimTrack<?>[] tracks = ((AnimClip) realAnimation).getTracks();
            for (AnimTrack<?> track : tracks) {
                if (track instanceof TransformTrack) {
                    HasLocalTransform target
                            = ((TransformTrack) track).getTarget();
                    if (target instanceof Spatial) {
                        if (spatialTracksSeen == spatialTrackIndex) {
                            result = track;
                        }
                        ++spatialTracksSeen;
                    }
                }
            }
        }

        return result;
    }

    /**
     * Find a track for the indexed target Bone/Joint.
     *
     * @param boneIndex the index of the target Bone or Joint (&ge;0)
     * @return the pre-existing BoneTrack or TransformTrack, or null if none
     * found
     */
    Object findTrackForBone(int boneIndex) {
        assert boneIndex >= 0 : boneIndex;

        Object result = null;
        Object realAnimation = getReal();
        if (realAnimation instanceof Animation) {
            result = MyAnimation.findBoneTrack(
                    (Animation) realAnimation, boneIndex);
        } else if (realAnimation instanceof AnimClip) {
            result = MyAnimation.findJointTrack(
                    (AnimClip) realAnimation, boneIndex);
        }

        return result;
    }

    /**
     * Find a track for the specified target Spatial.
     *
     * @param spat the target Spatial (unaffected)
     * @return the pre-existing MorphTrack, SpatialTrack, or TransformTrack, or
     * null if none found
     */
    Object findTrackForSpatial(Spatial spat) {
        Object result = null;
        Object control = cgm.getAnimControl().find();
        Object realAnim = getReal();
        if (control instanceof AnimControl && realAnim instanceof Animation) {
            result = MyAnimation.findSpatialTrack(
                    (AnimControl) control, (Animation) realAnim, spat);

        } else if (control instanceof AnimComposer
                && realAnim instanceof AnimClip) {
            AnimTrack<?>[] tracks = ((AnimClip) realAnim).getTracks();
            for (AnimTrack<?> track : tracks) {
                if (track instanceof MorphTrack) {
                    Geometry target = ((MorphTrack) track).getTarget();
                    if (target == spat) {
                        result = track;
                        break;
                    }
                } else if (track instanceof TransformTrack) {
                    HasLocalTransform target
                            = ((TransformTrack) track).getTarget();
                    if (target == spat) {
                        result = track;
                        break;
                    }
                }
            }
        }

        return result;
    }

    /**
     * Find the index of the specified track.
     *
     * @param track (may be null)
     * @return the track index (&ge;0) or -1 if not found
     */
    int findTrackIndex(Object track) {
        Object[] tracks = getTracks();
        int numTracks = tracks.length;
        for (int index = 0; index < numTracks; ++index) {
            if (track == tracks[index]) {
                return index;
            }
        }

        return -1;
    }

    /**
     * Access the real animation.
     *
     * @return the pre-existing Animation or AnimClip, or null if none or in
     * bind/retargeted pose
     */
    Object getReal() {
        Object result = null;
        if (isReal()) {
            result = cgm.getAnimControl().getAnimation(loadedName);
        }

        return result;
    }

    /**
     * Access the track array of the real animation.
     *
     * @return the pre-existing array of AnimTracks or Tracks, or null if none
     */
    Object[] getTracks() {
        Object[] tracks = null;
        Object realAnimation = getReal();
        if (realAnimation instanceof AnimClip) {
            tracks = ((AnimClip) realAnimation).getTracks();
        } else if (realAnimation instanceof Animation) {
            tracks = ((Animation) realAnimation).getTracks();
        }

        return tracks;
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
     * Test whether the animation includes a bone track for the indexed bone.
     *
     * @param boneIndex which bone (&ge;0)
     * @return true if a track exists, otherwise false
     */
    public boolean hasTrackForBone(int boneIndex) {
        Validate.nonNegative(boneIndex, "bone index");

        boolean result = false;
        Object realAnimation = getReal();
        if (realAnimation instanceof Animation) {
            result = MyAnimation.hasTrackForBone(
                    (Animation) realAnimation, boneIndex);
        } else if (realAnimation instanceof AnimClip) {
            result = MaudUtil.hasTrackForJoint(
                    (AnimClip) realAnimation, boneIndex);
        }

        return result;
    }

    /**
     * Insert a keyframe (or replace the existing keyframe) in each bone track
     * at the current animation time, to match the displayed pose.
     */
    public void insertKeyframes() {
        float atTime = cgm.getPlay().getTime();
        Pose pose = cgm.getPose().get();

        Object newSelectedTrack = null;
        Object oldSelectedTrack = cgm.getTrack().get();
        TmpTracks.clear();
        Object[] oldTracks = getTracks();
        for (Object oldTrack : oldTracks) { // TODO add more tracks?
            int frameIndex = MaudUtil.findKeyframeIndex(oldTrack, atTime);
            Object newTrack;
            if (oldTrack instanceof BoneTrack) {
                BoneTrack boneTrack = (BoneTrack) oldTrack;
                int boneIndex = boneTrack.getTargetBoneIndex();
                Transform user = pose.userTransform(boneIndex, null);
                if (frameIndex == -1) {
                    newTrack
                            = TrackEdit.insertKeyframe(boneTrack, atTime, user);
                } else {
                    newTrack = TrackEdit.replaceKeyframe(
                            boneTrack, frameIndex, user);
                }
            } else if (oldTrack instanceof TransformTrack) {
                TransformTrack transformTrack = (TransformTrack) oldTrack;
                HasLocalTransform target = transformTrack.getTarget();
                if (target instanceof Joint) {
                    int jointIndex = ((Joint) target).getId();
                    Transform local = pose.localTransform(jointIndex, null);
                    if (frameIndex == -1) {
                        newTrack = TrackEdit.insertKeyframe(
                                transformTrack, atTime, local);
                    } else {
                        newTrack = TrackEdit.replaceKeyframe(
                                transformTrack, frameIndex, local);
                    }
                } else {
                    newTrack = TrackEdit.cloneTrack(oldTrack);
                }
            } else {
                newTrack = TrackEdit.cloneTrack(oldTrack);
            }

            if (oldTrack == oldSelectedTrack) {
                newSelectedTrack = newTrack;
            }
            TmpTracks.add(newTrack);
        }

        Object newAnim = newAnim();
        TmpTracks.addAllToAnim(newAnim);

        String eventDescription = String.format(
                "insert keyframes into the %s animation at t=%f",
                MyString.quote(loadedName), atTime);
        Object oldAnim = getReal();
        editableCgm.replace(
                oldAnim, newAnim, eventDescription, newSelectedTrack);
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
        Object realAnimation = getReal();
        if (realAnimation != null) {
            AbstractControl control = cgm.getAnimControl().find();
            Object[] tracks = getTracks();
            for (Object track : tracks) {
                if (track instanceof BoneTrack) {
                    String name = MyAnimation.getTargetName(
                            (Track) track, (AnimControl) control);
                    result.add(name);
                } else if (track instanceof TransformTrack) {
                    // TODO
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
        Object realAnimation = getReal();
        if (realAnimation != null) {
            SelectedAnimControl sac = cgm.getAnimControl();
            AbstractControl animControl = sac.find();
            String controlName = sac.name();

            Object[] tracks = getTracks();
            result = new ArrayList<>(tracks.length);
            for (Object track : tracks) {
                TrackItem item = new TrackItem(
                        loadedName, controlName, animControl, track);
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
            loadBindPose(true);
        } else if (name.equals(retargetedPoseName)) {
            loadRetargetedPose();
        } else { // a real animation
            float duration = cgm.getAnimControl().getDuration(name);
            float playSpeed;
            if (duration == 0f) {
                // The animation consists of a single frame: set speed to zero.
                playSpeed = 0f;
            } else { // Start the animation at normal speed.
                playSpeed = 1f;
            }
            loadReal(name, playSpeed);
        }
    }

    /**
     * Load the bind pose.
     *
     * @param resetDisplayedPose true to reset, false to leave unchanged
     */
    public void loadBindPose(boolean resetDisplayedPose) {
        loadedName = bindPoseName;
        cgm.getPlay().resetLimits();
        cgm.getPlay().setSpeed(0f);
        cgm.getPlay().setTime(0f);
        cgm.getTrack().select(null);

        if (resetDisplayedPose) {
            Object skeleton = cgm.getSkeleton().find();
            cgm.getPose().resetToBind(skeleton);
        }
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
     * Read the name of the loaded animation.
     *
     * @return the name, or bindPoseName if in bind pose, or retargetedPoseName
     * if in retargeted pose (not null)
     */
    public String name() {
        assert loadedName != null;
        return loadedName;
    }

    /**
     * Create an empty animation with the same name and type as the loaded real
     * animation.
     *
     * @return a new Animation or AnimClip with no tracks
     */
    Object newAnim() {
        assert isReal();

        Object result;
        Object oldAnim = getReal();
        if (oldAnim instanceof AnimClip) {
            result = new AnimClip(loadedName);
        } else {
            float duration = ((Animation) oldAnim).getLength();
            result = new Animation(loadedName, duration);
        }

        return result;
    }

    /**
     * Reduce all tracks in the loaded animation by the specified factor.
     *
     * @param factor reduction factor (&ge;2)
     */
    public void reduce(int factor) {
        Validate.inRange(factor, "reduction factor", 2, Integer.MAX_VALUE);
        assert isReal();

        Object newSelectedTrack = null;
        Object oldSelectedTrack = cgm.getTrack().get();
        TmpTracks.clear();
        Object[] oldTracks = getTracks();
        for (Object oldTrack : oldTracks) {
            Object newTrack;
            if (oldTrack instanceof BoneTrack
                    || oldTrack instanceof SpatialTrack) {
                newTrack = TrackEdit.reduce((Track) oldTrack, factor);
            } else if (oldTrack instanceof TransformTrack) {
                newTrack = TrackEdit.reduce((TransformTrack) oldTrack, factor);
            } else { // TODO other track types
                newTrack = TrackEdit.cloneTrack(oldTrack);
            }

            if (oldTrack == oldSelectedTrack) {
                newSelectedTrack = newTrack;
            }
            TmpTracks.add(newTrack);
        }

        Object newAnim = newAnim();
        TmpTracks.addAllToAnim(newAnim);

        String eventDescription = String.format(
                "thin the %s animation by %dx", MyString.quote(loadedName),
                factor);
        Object oldAnim = getReal();
        editableCgm.replace(
                oldAnim, newAnim, eventDescription, newSelectedTrack);
    }

    /**
     * Rename the loaded animation.
     *
     * @param newName (not null, not reserved, not in use)
     */
    public void rename(String newName) {
        Validate.nonEmpty(newName, "new name");
        assert !MaudUtil.isReservedAnimationName(newName) : newName;
        SelectedAnimControl sac = cgm.getAnimControl();
        assert !sac.hasRealAnimation(newName) : newName;
        assert isReal();

        Object newAnim;
        Object oldAnim = getReal();
        if (oldAnim instanceof AnimClip) {
            newAnim = new AnimClip(newName);
        } else {
            assert oldAnim instanceof Animation;
            float duration = ((Animation) oldAnim).getLength();
            newAnim = new Animation(newName, duration);
        }

        Object oldSelectedTrack = cgm.getTrack().get();
        Object newSelectedTrack = null;
        TmpTracks.clear();
        Object[] oldTracks = getTracks();
        for (Object oldTrack : oldTracks) {
            Object newTrack = TrackEdit.cloneTrack(oldTrack);

            if (oldTrack == oldSelectedTrack) {
                newSelectedTrack = newTrack;
            }
            TmpTracks.add(newTrack);
        }
        TmpTracks.addAllToAnim(newAnim);

        String eventDescription = String.format("rename the %s animation to %s",
                MyString.quote(loadedName), MyString.quote(newName));
        editableCgm.replace(
                oldAnim, newAnim, eventDescription, newSelectedTrack);
        loadedName = newName;
    }

    /**
     * Reverse all bone/spatial tracks.
     */
    public void reverse() {
        Object newSelectedTrack = null;
        Object oldSelectedTrack = cgm.getTrack().get();
        TmpTracks.clear();
        Object[] oldTracks = getTracks();
        for (Object oldTrack : oldTracks) {
            Object newTrack;
            if (oldTrack instanceof BoneTrack
                    || oldTrack instanceof SpatialTrack) {
                newTrack = TrackEdit.reverse((Track) oldTrack);
            } else if (oldTrack instanceof AnimTrack) {
                newTrack = TrackEdit.reverse((AnimTrack<?>) oldTrack);
            } else { // TODO other track types
                newTrack = TrackEdit.cloneTrack(oldTrack);
            }

            if (oldTrack == oldSelectedTrack) {
                newSelectedTrack = newTrack;
            }
            TmpTracks.add(newTrack);
        }

        Object newAnim = newAnim();
        TmpTracks.addAllToAnim(newAnim);

        String eventDescription = String.format(
                "reverse the %s animation", MyString.quote(loadedName));
        Object oldAnim = getReal();
        editableCgm.replace(
                oldAnim, newAnim, eventDescription, newSelectedTrack);
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

        float duration = duration();
        Object newSelectedTrack = null;
        TweenTransforms techniques = Maud.getModel().getTweenTransforms();
        Object oldSelectedTrack = cgm.getTrack().get();
        TmpTracks.clear();
        Object[] oldTracks = getTracks();
        for (Object oldTrack : oldTracks) {
            Object newTrack;
            if (oldTrack instanceof BoneTrack
                    || oldTrack instanceof SpatialTrack) {
                newTrack = techniques.resampleAtRate(
                        (Track) oldTrack, sampleRate, duration);
            } else if (oldTrack instanceof TransformTrack) {
                newTrack = TrackEdit.resampleAtRate(
                        (TransformTrack) oldTrack, sampleRate, duration);
            } else { // TODO other track types
                newTrack = TrackEdit.cloneTrack(oldTrack);
            }

            if (oldTrack == oldSelectedTrack) {
                newSelectedTrack = newTrack;
            }
            TmpTracks.add(newTrack);
        }

        Object newAnim = newAnim();
        TmpTracks.addAllToAnim(newAnim);

        String eventDescription = String.format(
                "resample the %s animation at %f FPS",
                MyString.quote(loadedName), sampleRate);
        Object oldAnim = getReal();
        editableCgm.replace(
                oldAnim, newAnim, eventDescription, newSelectedTrack);
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

        float duration = duration();
        assert duration > 0f : duration;
        Object newSelectedTrack = null;
        TweenTransforms techniques = Maud.getModel().getTweenTransforms();
        Object oldSelectedTrack = cgm.getTrack().get();

        TmpTracks.clear();
        Object[] oldTracks = getTracks();
        for (Object oldTrack : oldTracks) {
            Object newTrack;
            if (oldTrack instanceof BoneTrack
                    || oldTrack instanceof SpatialTrack) {
                newTrack = techniques.resampleToNumber(
                        (Track) oldTrack, numSamples, duration);
            } else if (oldTrack instanceof TransformTrack) {
                newTrack = TrackEdit.resampleToNumber(
                        (TransformTrack) oldTrack, numSamples, duration);
            } else { // TODO other track types
                newTrack = TrackEdit.cloneTrack(oldTrack);
            }

            if (oldTrack == oldSelectedTrack) {
                newSelectedTrack = newTrack;
            }
            TmpTracks.add(newTrack);
        }

        Object newAnim = newAnim();
        TmpTracks.addAllToAnim(newAnim);

        String eventDescription = String.format(
                "resample the %s animation to %d keyframes",
                MyString.quote(loadedName), numSamples);
        Object oldAnim = getReal();
        editableCgm.replace(
                oldAnim, newAnim, eventDescription, newSelectedTrack);
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

        float oldDuration = duration();
        if (oldDuration == newDuration) {
            return;
        }

        Object newSelectedTrack = null;
        Object oldSelectedTrack = cgm.getTrack().get();
        TmpTracks.clear();
        Object[] oldTracks = getTracks();
        for (Object track : oldTracks) {
            Object newTrack
                    = TrackEdit.setDuration((Track) track, newDuration);
            if (track == oldSelectedTrack) {
                newSelectedTrack = newTrack;
            }
            TmpTracks.add(newTrack);
        }

        Object newAnim = newAnim(newDuration);
        TmpTracks.addAllToAnim(newAnim);

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
        Object oldAnim = getReal();
        editableCgm.replace(
                oldAnim, newAnim, eventDescription, newSelectedTrack);
        load(loadedName);
    }

    /**
     * Truncate or extend the loaded animation to give it the specified
     * duration.
     *
     * @param newDuration (in seconds, &ge;0)
     */
    public void setDurationSame(float newDuration) {
        Validate.nonNegative(newDuration, "new duration");

        float oldDuration = duration();
        if (oldDuration == newDuration) {
            return;
        }

        Object newSelectedTrack = null;
        Object oldSelectedTrack = cgm.getTrack().get();
        TmpTracks.clear();
        Object[] oldTracks = getTracks();
        for (Object oldTrack : oldTracks) {
            Object newTrack;
            if (oldTrack instanceof BoneTrack
                    || oldTrack instanceof SpatialTrack) {
                newTrack = TrackEdit.truncate((Track) oldTrack, newDuration);
            } else if (oldTrack instanceof MorphTrack) {
                MorphTrack morphTrack = (MorphTrack) oldTrack;
                int numTargets = morphTrack.getNbMorphTargets();
                float[] endWeights = new float[numTargets];
                morphTrack.getDataAtTime(newDuration, endWeights);
                newTrack = TrackEdit.truncate(
                        morphTrack, newDuration, endWeights);
            } else if (oldTrack instanceof TransformTrack) {
                TransformTrack transformTrack = (TransformTrack) oldTrack;
                Transform endTransform = new Transform();
                transformTrack.getDataAtTime(newDuration, endTransform);
                newTrack = TrackEdit.truncate(
                        transformTrack, newDuration, endTransform);
            } else { // TODO other track types
                newTrack = TrackEdit.cloneTrack(oldTrack);
            }

            if (oldTrack == oldSelectedTrack) {
                newSelectedTrack = newTrack;
            }
            TmpTracks.add(newTrack);
        }

        Object newAnim = newAnim(newDuration);
        TmpTracks.addAllToAnim(newAnim);

        String verb = (newDuration < oldDuration) ? "truncate" : "extend";
        String eventDescription = String.format(
                "%s the %s animation to t=%f", verb,
                MyString.quote(loadedName), newDuration);
        Object oldAnim = getReal();
        editableCgm.replace(
                oldAnim, newAnim, eventDescription, newSelectedTrack);
        load(loadedName);
    }

    /**
     * Delete any optional track components that consist entirely of identities,
     * as well as any tracks for bones with no influence.
     */
    public void simplify() {
        BitSet influencers = null;
        SelectedSkeleton ss = cgm.getSkeleton();
        if (ss.isSelected()) {
            Object skeleton = ss.find();
            Spatial subtree = ss.findSpatial();
            if (skeleton instanceof Armature) {
                influencers = InfluenceUtil.addAllInfluencers(
                        subtree, (Armature) skeleton);
            } else {
                influencers = InfluenceUtil.addAllInfluencers(
                        subtree, (Skeleton) skeleton);
            }
        }

        Object newSelectedTrack = null;
        Object oldSelectedTrack = cgm.getTrack().get();
        TmpTracks.clear();
        Object[] oldTracks = getTracks();
        for (Object track : oldTracks) {
            Object newTrack = null;
            if (track instanceof BoneTrack) {
                int boneIndex = ((BoneTrack) track).getTargetBoneIndex();
                if (influencers.get(boneIndex)) {
                    newTrack = TrackEdit.simplify((Track) track);
                }
            } else if (track instanceof SpatialTrack) {
                newTrack = TrackEdit.simplify((Track) track);
            } else if (track instanceof TransformTrack) {
                newTrack = TrackEdit.simplify((TransformTrack) track);
            } else {
                newTrack = TrackEdit.cloneTrack(track);
            }

            if (track == oldSelectedTrack) {
                newSelectedTrack = newTrack;
            }
            if (newTrack != null) {
                TmpTracks.add(newTrack);
            }
        }

        Object newAnim = newAnim();
        TmpTracks.addAllToAnim(newAnim);

        String eventDescription = String.format(
                "simplify the %s animation", MyString.quote(loadedName));
        Object oldAnim = getReal();
        editableCgm.replace(
                oldAnim, newAnim, eventDescription, newSelectedTrack);
    }

    /**
     * Delete everything after the current animation time and make that the end
     * of the animation.
     */
    public void truncate() {
        float endTime = cgm.getPlay().getTime();
        Object newSelectedTrack = null;
        Object oldSelectedTrack = cgm.getTrack().get();
        TmpTracks.clear();
        Object[] oldTracks = getTracks();
        for (Object oldTrack : oldTracks) {
            Object newTrack;
            if (oldTrack instanceof BoneTrack
                    || oldTrack instanceof SpatialTrack) {
                newTrack = TrackEdit.truncate((Track) oldTrack, endTime);
            } else if (oldTrack instanceof MorphTrack) {
                MorphTrack morphTrack = (MorphTrack) oldTrack;
                int numTargets = morphTrack.getNbMorphTargets();
                float[] endWeights = new float[numTargets];
                morphTrack.getDataAtTime(endTime, endWeights);
                newTrack = TrackEdit.truncate(
                        morphTrack, endTime, endWeights);
            } else if (oldTrack instanceof TransformTrack) {
                TransformTrack transformTrack = (TransformTrack) oldTrack;
                Transform endTransform = new Transform();
                transformTrack.getDataAtTime(endTime, endTransform);
                newTrack = TrackEdit.truncate(
                        transformTrack, endTime, endTransform);
            } else { // TODO other track types
                newTrack = TrackEdit.cloneTrack(oldTrack);
            }

            if (oldTrack == oldSelectedTrack) {
                newSelectedTrack = newTrack;
            }
            TmpTracks.add(newTrack);
        }

        Object newAnim = newAnim(endTime);
        TmpTracks.addAllToAnim(newAnim);

        String eventDescription = String.format(
                "truncate the %s animation at t=%f", MyString.quote(loadedName),
                endTime);
        Object oldAnim = getReal();
        editableCgm.replace(
                oldAnim, newAnim, eventDescription, newSelectedTrack);
        load(loadedName);
    }

    /**
     * Alter each track's first keyframe and end-time keyframe so that they
     * precisely match. If a track doesn't end with a keyframe, append one.
     *
     * @param endWeight how much weight to give to pre-existing end-time
     * keyframes, if any exist (&ge;0, &le;1)
     */
    public void wrapAllTracks(float endWeight) {
        float duration = duration();
        Object newSelectedTrack = null;
        Object oldSelectedTrack = cgm.getTrack().get();
        TmpTracks.clear();
        Object[] oldTracks = getTracks();
        for (Object oldTrack : oldTracks) {
            Object newTrack;
            if (oldTrack instanceof BoneTrack
                    || oldTrack instanceof SpatialTrack) {
                newTrack = TrackEdit.wrap(
                        (Track) oldTrack, duration, endWeight);
            } else if (oldTrack instanceof TransformTrack) {
                newTrack = TrackEdit.wrap(
                        (TransformTrack) oldTrack, duration, endWeight);
            } else { // TODO other track types
                newTrack = TrackEdit.cloneTrack(oldTrack);
            }

            if (oldTrack == oldSelectedTrack) {
                newSelectedTrack = newTrack;
            }
            TmpTracks.add(newTrack);
        }

        Object newAnim = newAnim();
        TmpTracks.addAllToAnim(newAnim);

        String eventDescription = String.format(
                "wrap all tracks in the %s animation using end weight=%f",
                MyString.quote(loadedName), endWeight);
        Object oldAnim = getReal();
        editableCgm.replace(
                oldAnim, newAnim, eventDescription, newSelectedTrack);
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
     * Load the named real animation (not bind/retargeted pose) at t=0 with the
     * specified playback speed.
     *
     * @param name which animation (not null)
     * @param newSpeed playback speed
     */
    private void loadReal(String name, float newSpeed) {
        assert name != null;
        assert !MaudUtil.isReservedAnimationName(name);

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

    /**
     * Create a new animation with the same name and type as the loaded real
     * animation, but a different duration.
     *
     * @param duration the desired duration (in seconds, &ge;0)
     * @return a new Animation or AnimClip with no tracks
     */
    private Object newAnim(float duration) {
        Object result;
        Object oldAnim = getReal();
        if (oldAnim instanceof AnimClip) {
            result = new AnimClip(loadedName);
        } else {
            assert oldAnim instanceof Animation;
            result = new Animation(loadedName, duration);
        }

        return result;
    }
}
