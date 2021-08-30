/*
 Copyright (c) 2017-2020, Stephen Gold
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
import com.jme3.anim.TransformTrack;
import com.jme3.anim.util.HasLocalTransform;
import com.jme3.animation.AnimControl;
import com.jme3.animation.Animation;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SpatialTrack;
import com.jme3.animation.Track;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import jme3utilities.MyAnimation;
import jme3utilities.MyMesh;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.math.MyArray;
import jme3utilities.math.MyQuaternion;
import jme3utilities.wes.Pose;
import jme3utilities.wes.SmoothRotations;
import jme3utilities.wes.SmoothVectors;
import jme3utilities.wes.TrackEdit;
import jme3utilities.wes.TweenTransforms;
import maud.Maud;
import maud.MaudUtil;
import maud.SupportUtil;
import maud.tool.EditorTools;

/**
 * The MVC model of the selected track in a loaded animation.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SelectedTrack implements JmeCloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SelectedTrack.class.getName());
    // *************************************************************************
    // fields

    /**
     * C-G model containing the track (set by {@link #setCgm(Cgm)})
     */
    private Cgm cgm;
    /**
     * editable C-G model, if any, containing the track (set by
     * {@link #setCgm(Cgm)})
     */
    private EditableCgm editableCgm;
    /**
     * selected track in the loaded Animation/AnimClip, or null if none selected
     */
    private Object selected;
    // *************************************************************************
    // new methods exposed

    /**
     * Count the number of keyframes in the track.
     *
     * @return the count (&ge;0)
     */
    public int countKeyframes() {
        int count = 0;
        if (selected != null) {
            float[] times = MaudUtil.getTrackTimes(selected);
            count = times.length;
        }

        return count;
    }

    /**
     * Count the number of distinct rotations, without distinguishing 0 from -0.
     *
     * @return count (&ge;0)
     */
    public int countRotations() {
        int count = 0;
        if (selected != null) {
            Quaternion[] rotations = MaudUtil.getTrackRotations(selected);
            if (rotations != null) {
                count = countNe(rotations);
            }
        }

        return count;
    }

    /**
     * Count the number of distinct scales, without distinguishing 0 from -0.
     *
     * @return count (&ge;0)
     */
    public int countScales() {
        int result = 0;
        if (selected != null) {
            Vector3f[] scales = MaudUtil.getTrackScales(selected);
            if (scales != null) {
                result = MyArray.countNe(scales);
            }
        }

        return result;
    }

    /**
     * Count the number of distinct translations, without distinguishing 0 from
     * -0.
     *
     * @return count (&ge;0)
     */
    public int countTranslations() {
        int result = 0;
        if (selected != null) {
            Vector3f[] translations = MaudUtil.getTrackTranslations(selected);
            if (translations != null) {
                result = MyArray.countNe(translations);
            }
        }

        return result;
    }

    /**
     * Delete the specified number of keyframes following the selected one.
     *
     * @param number number of keyframes to delete (&ge;1)
     */
    public void deleteNextKeyframes(int number) {
        Validate.positive(number, "number");
        assert selected != null;
        int frameIndex = cgm.getFrame().findIndex();
        assert frameIndex != -1;

        deleteRange(frameIndex + 1, number);
    }

    /**
     * Delete the specified number of keyframes preceding the selected one.
     *
     * @param number number of keyframes to delete (&ge;1)
     */
    public void deletePreviousKeyframes(int number) {
        Validate.positive(number, "number");
        assert selected != null;
        int frameIndex = cgm.getFrame().findIndex();
        assert frameIndex != -1;

        deleteRange(frameIndex - number, number);
    }

    /**
     * Delete the rotations from the selected track, which must be a
     * SpatialTrack or TransformTrack.
     */
    public void deleteRotations() {
        assert selected instanceof SpatialTrack;

        Object newSelected = null;

        TmpTracks.clear();
        Object[] oldTracks = cgm.getAnimation().getTracks();
        for (Object oldTrack : oldTracks) {
            Object newTrack;
            if (oldTrack == selected) {
                float[] times = MaudUtil.getTrackTimes(oldTrack);
                Vector3f[] translations = MaudUtil.getTrackTranslations(selected);
                Vector3f[] scales = MaudUtil.getTrackScales(selected);
                newTrack = MaudUtil.newTrack(oldTrack, times, translations,
                        null, scales);
                newSelected = newTrack;
            } else {
                newTrack = TrackEdit.cloneTrack(oldTrack);
            }
            TmpTracks.add(newTrack);
        }

        Object newAnim = cgm.getAnimation().newAnim();
        TmpTracks.addAllToAnim(newAnim);

        String trackName = describe();
        String eventDescription
                = String.format("delete rotations from track %s", trackName);
        Object oldAnim = cgm.getAnimation().getReal();
        editableCgm.replace(oldAnim, newAnim, eventDescription, newSelected);
    }

    /**
     * Delete the scales from the selected track.
     */
    public void deleteScales() {
        assert selected != null;

        Object newSelected = null;

        TmpTracks.clear();
        Object[] oldTracks = cgm.getAnimation().getTracks();
        for (Object oldTrack : oldTracks) {
            Object newTrack;
            if (oldTrack == selected) {
                float[] times = MaudUtil.getTrackTimes(oldTrack);
                Vector3f[] translations = MaudUtil.getTrackTranslations(selected);
                Quaternion[] rotations = MaudUtil.getTrackRotations(selected);
                newTrack = MaudUtil.newTrack(oldTrack, times, translations,
                        rotations, null);
                newSelected = newTrack;
            } else {
                newTrack = TrackEdit.cloneTrack(oldTrack);
            }
            TmpTracks.add(newTrack);
        }

        Object newAnim = cgm.getAnimation().newAnim();
        TmpTracks.addAllToAnim(newAnim);

        String trackName = describe();
        String eventDescription
                = String.format("delete scales from track %s", trackName);
        Object oldAnim = cgm.getAnimation().getReal();
        editableCgm.replace(oldAnim, newAnim, eventDescription, newSelected);
    }

    /**
     * Delete the selected keyframe, which mustn't be the first keyframe in the
     * track.
     */
    public void deleteSelectedKeyframe() {
        assert selected != null;
        int frameIndex = cgm.getFrame().findIndex();
        assert frameIndex > 0 : frameIndex;

        deleteRange(frameIndex, 1);
    }

    /**
     * Delete the translations from the selected track, which must be a
     * SpatialTrack or TransformTrack.
     */
    public void deleteTranslations() {
        assert selected instanceof SpatialTrack;

        Object newSelected = null;

        TmpTracks.clear();
        Object[] oldTracks = cgm.getAnimation().getTracks();
        for (Object oldTrack : oldTracks) {
            Object newTrack;
            if (oldTrack == selected) {
                float[] times = MaudUtil.getTrackTimes(oldTrack);
                Quaternion[] rotations = MaudUtil.getTrackRotations(selected);
                Vector3f[] scales = MaudUtil.getTrackScales(selected);
                newTrack = MaudUtil.newTrack(oldTrack, times, null,
                        rotations, scales);
                newSelected = newTrack;
            } else {
                newTrack = TrackEdit.cloneTrack(oldTrack);
            }
            TmpTracks.add(newTrack);
        }

        Object newAnim = cgm.getAnimation().newAnim();
        TmpTracks.addAllToAnim(newAnim);

        String trackName = describe();
        String eventDescription
                = String.format("delete translations from track %s", trackName);
        Object oldAnim = cgm.getAnimation().getReal();
        editableCgm.replace(oldAnim, newAnim, eventDescription, newSelected);
    }

    /**
     * Describe the track in the context of its animation.
     *
     * @return a textual description, or "" if none selected (not null)
     */
    public String describe() {
        String result = "";
        if (selected != null) {
            AbstractControl control = cgm.getAnimControl().find();
            assert control != null;
            if (control instanceof AnimComposer) {
                AnimTrack animTrack = (AnimTrack) selected;
                result = MaudUtil.describe(animTrack, (AnimComposer) control);
            } else {
                Track track = (Track) selected;
                result = MyAnimation.describe(track, (AnimControl) control);
            }
        }

        return result;
    }

    /**
     * Describe the track's target.
     *
     * @return description (not null)
     */
    public String describeTarget() {
        String description = "";
        if (selected instanceof BoneTrack) {
            int boneIndex = targetBoneIndex();
            assert boneIndex >= 0 : boneIndex;
            description = cgm.getSkeleton().getBoneName(boneIndex);
            description = MyString.quote(description);
        } else if (selected instanceof SpatialTrack) {
            SpatialTrack spatialTrack = (SpatialTrack) selected;
            Spatial spatial = spatialTrack.getTrackSpatial();
            description = spatial.getName();
            description = MyString.quote(description);
        }

        return description;
    }

    /**
     * Describe the type of track selected.
     *
     * @return description (not null)
     */
    public String describeType() {
        String description = "";
        if (selected != null) {
            description = selected.getClass().getSimpleName();
            if (description.endsWith("Track")) {
                description = MyString.removeSuffix(description, "Track");
            }
        }

        return description;
    }

    /**
     * Test whether the track ends with a keyframe.
     *
     * @return true if track ends with a keyframe, otherwise false
     */
    public boolean endsWithKeyframe() {
        float duration = cgm.getAnimation().duration();
        int endIndex = MaudUtil.findKeyframeIndex(selected, duration);
        if (endIndex >= 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Find the index of the keyframe (if any) at the specified time.
     *
     * @param time the animation time (in seconds, &ge;0)
     * @return the keyframe's index (&ge;0) or -1 if no keyframe at that time
     */
    public int findKeyframeIndex(float time) {
        Validate.nonNegative(time, "time");
        assert selected != null;

        int frameIndex = MaudUtil.findKeyframeIndex(selected, time);
        return frameIndex;
    }

    /**
     * Find the index of the first keyframe at or after the specified time.
     *
     * @param time the animation time (in seconds, &ge;0)
     * @return the keyframe's index (&ge;0) or -1 if no such keyframe
     */
    public int findNextKeyframeIndex(float time) {
        Validate.nonNegative(time, "time");
        assert selected != null;

        int nextIndex = findKeyframeIndex(time);
        if (nextIndex == -1) {
            int previous
                    = MaudUtil.findPreviousKeyframeIndex(selected, time);
            int lastIndex = countKeyframes() - 1;
            if (previous == lastIndex) {
                nextIndex = -1;
            } else {
                nextIndex = previous + 1;
            }
        }

        return nextIndex;
    }

    /**
     * Find the index of the last keyframe at or before the specified time.
     *
     * @param time the animation time (in seconds, &ge;0)
     * @return the keyframe's index (&ge;0)
     */
    public int findPreviousKeyframeIndex(float time) {
        Validate.nonNegative(time, "time");
        int frameIndex = MaudUtil.findPreviousKeyframeIndex(selected, time);
        return frameIndex;
    }

    /**
     * Access the selected track in the loaded animation.
     *
     * @return the pre-existing AnimTrack or Track, or null if none
     */
    Object get() {
        return selected;
    }

    /**
     * Determine the index of the selected track in the loaded animation.
     *
     * @return the track index, or -1 if none selected
     */
    public int index() {
        int index = -1;
        if (selected != null) {
            List<String> descriptions = listDescriptions();
            String desc = describe();
            index = descriptions.indexOf(desc);
        }

        return index;
    }

    /**
     * Based on the displayed pose, add a keyframe to the track at the current
     * animation time.
     */
    public void insertKeyframe() {
        float time = cgm.getPlay().getTime();
        assert time > 0f : time;
        float duration = cgm.getAnimation().duration();
        assert time <= duration : time;
        assert !cgm.getFrame().isSelected();

        Object newSelected = null;

        TmpTracks.clear();
        Object[] oldTracks = cgm.getAnimation().getTracks();
        for (Object oldTrack : oldTracks) {
            Object newTrack;
            if (oldTrack == selected) {
                if (selected instanceof BoneTrack) {
                    BoneTrack boneTrack = (BoneTrack) selected;
                    int boneIndex = boneTrack.getTargetBoneIndex();
                    Pose pose = cgm.getPose().get();
                    Transform user = pose.userTransform(boneIndex, null);
                    newTrack = TrackEdit.insertKeyframe(boneTrack, time, user);
                    newSelected = newTrack;
                } else {
                    TransformTrack transformTrack = (TransformTrack) selected;
                    Joint joint = (Joint) transformTrack.getTarget();
                    int jointIndex = joint.getId();
                    Pose pose = cgm.getPose().get();
                    Transform local = pose.localTransform(jointIndex, null);
                    newTrack = TrackEdit.insertKeyframe(transformTrack, time,
                            local);
                    newSelected = newTrack;
                }
            } else {
                newTrack = TrackEdit.cloneTrack(oldTrack);
            }
            TmpTracks.add(newTrack);
        } // TODO new bone tracks?

        Object newAnim = cgm.getAnimation().newAnim();
        TmpTracks.addAllToAnim(newAnim);

        String description = String.format("insert a keyframe at t=%f", time);
        Object oldAnim = cgm.getAnimation().getReal();
        editableCgm.replace(oldAnim, newAnim, description, newSelected);
    }

    /**
     * Test whether a bone track is selected.
     *
     * @return true if such a track is selected, false if none is selected
     */
    public boolean isBoneTrack() {
        boolean result = selected instanceof BoneTrack;
        return result;
    }

    /**
     * Test whether a track is selected.
     *
     * @return true if one is selected, false if none is selected
     */
    public boolean isSelected() {
        boolean result;
        if (selected == null) {
            result = false;
        } else {
            result = true;
        }

        return result;
    }

    /**
     * Generate an item for the selected track.
     *
     * @return a new item, or null if none selected
     */
    public TrackItem item() {
        String animName = cgm.getAnimation().name();
        String controlName = cgm.getAnimControl().name();
        AbstractControl control = cgm.getAnimControl().find();
        TrackItem item
                = new TrackItem(animName, controlName, control, selected);

        return item;
    }

    /**
     * Find the time of the indexed keyframe in the selected track.
     *
     * @param keyframeIndex which keyframe (&ge;0)
     * @return animation time (&ge;0)
     */
    public float keyframeTime(int keyframeIndex) {
        float[] times = MaudUtil.getTrackTimes(selected);
        float result = times[keyframeIndex];

        assert result >= 0f : result;
        return result;
    }

    /**
     * Find the time of the last keyframe in the selected track.
     *
     * @return animation time (&ge;0)
     */
    public float lastKeyframeTime() {
        float[] times = MaudUtil.getTrackTimes(selected);
        int lastIndex = times.length - 1;
        float result = times[lastIndex];

        return result;
    }

    /**
     * Enumerate all keyframes of the selected track.
     *
     * @return a new list of names, or null if no keyframes
     */
    public List<String> listKeyframes() {
        List<String> result = null;
        if (selected != null) {
            float[] keyframes = MaudUtil.getTrackTimes(selected);
            result = new ArrayList<>(keyframes.length);
            for (float keyframe : keyframes) {
                String menuItem = String.format("%.3f", keyframe);
                result.add(menuItem);
            }
        }

        return result;
    }

    /**
     * Thin the track's keyframes by the specified factor.
     *
     * @param factor reduction factor (&ge;2)
     */
    public void reduce(int factor) {
        Validate.inRange(factor, "reduction factor", 2, Integer.MAX_VALUE);
        assert selected != null;

        Object newSelected = null;

        TmpTracks.clear();
        Object[] oldTracks = cgm.getAnimation().getTracks();
        for (Object oldTrack : oldTracks) {
            Object newTrack;
            if (oldTrack == selected) {
                if (selected instanceof TransformTrack) {
                    newTrack = TrackEdit.reduce((TransformTrack) oldTrack,
                            factor);
                    newSelected = newTrack;
                } else {
                    newTrack = TrackEdit.reduce((Track) oldTrack, factor);
                    newSelected = newTrack;
                }
            } else {
                newTrack = TrackEdit.cloneTrack(oldTrack);
            }
            TmpTracks.add(newTrack);
        }

        Object newAnim = cgm.getAnimation().newAnim();
        TmpTracks.addAllToAnim(newAnim);

        String trackName = describe();
        String description = String.format(
                "thin the keyframes in track %s by %dx", trackName, factor);
        Object oldAnim = cgm.getAnimation().getReal();
        editableCgm.replace(oldAnim, newAnim, description, newSelected);
    }

    /**
     * Resample the track at the specified rate.
     *
     * @param sampleRate sample rate (in frames per second, &gt;0)
     */
    public void resampleAtRate(float sampleRate) {
        Validate.positive(sampleRate, "sample rate");
        assert selected != null;

        Object newSelected = null;

        TmpTracks.clear();
        Object[] oldTracks = cgm.getAnimation().getTracks();
        for (Object oldTrack : oldTracks) {
            Object newTrack;
            if (oldTrack == selected) {
                float duration = cgm.getAnimation().duration();
                if (selected instanceof Track) {
                    TweenTransforms technique
                            = Maud.getModel().getTweenTransforms();
                    newTrack = technique.resampleAtRate((Track) selected,
                            sampleRate, duration);
                    newSelected = newTrack;
                } else {
                    newTrack = TrackEdit.resampleAtRate(
                            (TransformTrack) selected, sampleRate, duration);
                    newSelected = newTrack;
                }
            } else {
                newTrack = TrackEdit.cloneTrack(oldTrack);
            }
            TmpTracks.add(newTrack);
        }

        Object newAnim = cgm.getAnimation().newAnim();
        TmpTracks.addAllToAnim(newAnim);

        String trackName = describe();
        String description = String.format("resample track %s at %f fps",
                trackName, sampleRate);
        Object oldAnim = cgm.getAnimation().getReal();
        editableCgm.replace(oldAnim, newAnim, description, newSelected);
    }

    /**
     * Resample the track to the specified number of samples.
     *
     * @param numSamples number of samples (&ge;2)
     */
    public void resampleToNumber(int numSamples) {
        Validate.inRange(numSamples, "number of samples", 2, Integer.MAX_VALUE);
        assert selected != null;

        Object newSelected = null;

        TmpTracks.clear();
        Object[] oldTracks = cgm.getAnimation().getTracks();
        for (Object oldTrack : oldTracks) {
            Object newTrack;
            if (oldTrack == selected) {
                float duration = cgm.getAnimation().duration();
                assert duration > 0f : duration;
                if (selected instanceof Track) {
                    TweenTransforms technique
                            = Maud.getModel().getTweenTransforms();
                    newTrack = technique.resampleToNumber((Track) selected,
                            numSamples, duration);
                    newSelected = newTrack;
                } else {
                    newTrack = TrackEdit.resampleToNumber(
                            (TransformTrack) selected, numSamples, duration);
                    newSelected = newTrack;
                }
            } else {
                newTrack = TrackEdit.cloneTrack(oldTrack);
            }
            TmpTracks.add(newTrack);
        }

        Object newAnim = cgm.getAnimation().newAnim();
        TmpTracks.addAllToAnim(newAnim);

        String trackName = describe();
        String description = String.format("resample track %s to %d samples",
                trackName, numSamples);
        Object oldAnim = cgm.getAnimation().getReal();
        editableCgm.replace(oldAnim, newAnim, description, newSelected);
    }

    /**
     * Select the specified track.
     *
     * @param track which track to select, or null to deselect
     */
    void select(Object track) {
        if (track != null) {
            assert cgm.getAnimation().findTrackIndex(track) != -1;
        }
        selected = track;
    }

    /**
     * Select the next track in the animation.
     */
    public void selectNext() {
        List<String> descriptions = listDescriptions();
        int numTracks = descriptions.size();
        String desc = describe();
        int index = descriptions.indexOf(desc);
        index = (index + 1) % numTracks;
        desc = descriptions.get(index);
        selectWithDescription(desc);
    }

    /**
     * Select the previous track in the animation.
     */
    public void selectPrevious() {
        List<String> descriptions = listDescriptions();
        String desc = describe();
        int index = descriptions.indexOf(desc) - 1;
        if (index < 0) {
            int numTracks = descriptions.size();
            index = numTracks - 1;
        }
        desc = descriptions.get(index);
        selectWithDescription(desc);
    }

    /**
     * Select the bone or spatial that is the track's target.
     */
    public void selectTarget() {
        if (selected instanceof BoneTrack) {
            BoneTrack boneTrack = (BoneTrack) selected;
            int boneIndex = boneTrack.getTargetBoneIndex();
            cgm.getBone().select(boneIndex);
            EditorTools.select("bone");
        } else if (selected instanceof SpatialTrack) {
            SpatialTrack spatialTrack = (SpatialTrack) selected;
            Spatial spatial = spatialTrack.getTrackSpatial();
            cgm.getSpatial().select(spatial);
            EditorTools.select("spatial");
        }
    }

    /**
     * Select the track with the specified description.
     *
     * @param description from {@link #describe()} (not null, not empty)
     */
    public void selectWithDescription(String description) {
        Validate.nonEmpty(description, "description");

        List<TrackItem> items = cgm.getAnimation().listTracks();
        for (TrackItem item : items) {
            if (item.describe().equals(description)) {
                Object track = item.getTrack();
                select(track);
                return;
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * Alter which C-G model contains the track. (Invoked only during
     * initialization and cloning.)
     *
     * @param newCgm (not null, aliases created)
     */
    void setCgm(Cgm newCgm) {
        assert newCgm != null;
        assert newCgm.getTrack() == this;

        cgm = newCgm;
        if (newCgm instanceof EditableCgm) {
            editableCgm = (EditableCgm) newCgm;
        } else {
            editableCgm = null;
        }
    }

    /**
     * Alter all rotations to match the displayed Pose. Requires a BoneTrack or
     * TransformTrack.
     */
    public void setRotationAll() {
        assert selected instanceof BoneTrack
                || selected instanceof TransformTrack;

        Pose pose = cgm.getPose().get();
        int boneIndex = targetBoneIndex();
        Quaternion poseRotation = pose.userRotation(boneIndex, null);

        float[] times = MaudUtil.getTrackTimes(selected);
        Vector3f[] translations = MaudUtil.getTrackTranslations(selected);
        Vector3f[] scales = MaudUtil.getTrackScales(selected);

        Quaternion[] rotations = new Quaternion[times.length];
        for (int i = 0; i < times.length; ++i) {
            rotations[i] = poseRotation.clone();
        }

        editableCgm.setKeyframes(times, translations, rotations, scales);
    }

    /**
     * Alter all scales to match the displayed Pose. Requires a BoneTrack or
     * TransformTrack.
     */
    public void setScaleAll() {
        assert selected instanceof BoneTrack
                || selected instanceof TransformTrack;

        Pose pose = cgm.getPose().get();
        int boneIndex = targetBoneIndex();
        Vector3f poseScale = pose.userScale(boneIndex, null);

        float[] times = MaudUtil.getTrackTimes(selected);
        Vector3f[] translations = MaudUtil.getTrackTranslations(selected);
        Quaternion[] rotations = MaudUtil.getTrackRotations(selected);

        Vector3f[] scales = new Vector3f[times.length];
        for (int i = 0; i < times.length; ++i) {
            scales[i] = poseScale.clone();
        }

        editableCgm.setKeyframes(times, translations, rotations, scales);
    }

    /**
     * Alter all translations to match the displayed Pose. Requires a BoneTrack
     * or TransformTrack.
     */
    public void setTranslationAll() {
        assert selected instanceof BoneTrack
                || selected instanceof TransformTrack;

        Pose pose = cgm.getPose().get();
        int boneIndex = targetBoneIndex();
        Vector3f poseTranslation = pose.userTranslation(boneIndex, null);

        float[] times = MaudUtil.getTrackTimes(selected);
        Quaternion[] rotations = MaudUtil.getTrackRotations(selected);
        Vector3f[] scales = MaudUtil.getTrackScales(selected);

        Vector3f[] translations = new Vector3f[times.length];
        for (int i = 0; i < times.length; ++i) {
            translations[i] = poseTranslation.clone();
        }

        editableCgm.setKeyframes(times, translations, rotations, scales);
    }

    /**
     * Smooth the track.
     */
    public void smooth() {
        assert selected != null;

        Object newSelected = null;
        float duration = cgm.getAnimation().duration();

        TmpTracks.clear();
        Object[] oldTracks = cgm.getAnimation().getTracks();
        for (Object oldTrack : oldTracks) {
            Object newTrack;
            if (oldTrack == selected) {
                if (selected instanceof Track) {
                    newTrack = TrackEdit.smooth((Track) selected, 0.2f,
                            SmoothVectors.LoopLerp, SmoothRotations.LoopNlerp,
                            SmoothVectors.LoopLerp, duration);
                    newSelected = newTrack;
                } else {
                    newTrack = TrackEdit.smooth((TransformTrack) selected, 0.2f,
                            SmoothVectors.LoopLerp, SmoothRotations.LoopNlerp,
                            SmoothVectors.LoopLerp, duration);
                    newSelected = newTrack;
                }
            } else {
                newTrack = TrackEdit.cloneTrack(oldTrack);
            }
            TmpTracks.add(newTrack);
        }

        Object newAnim = cgm.getAnimation().newAnim();
        TmpTracks.addAllToAnim(newAnim);

        String trackName = describe();
        String description = String.format("smooth track %s", trackName);
        Object oldAnim = cgm.getAnimation().getReal();
        editableCgm.replace(oldAnim, newAnim, description, newSelected);
    }

    /**
     * Determine the index of the target bone.
     *
     * @return the bone index, or noBoneIndex if none
     */
    public int targetBoneIndex() {
        int result = SelectedSkeleton.noBoneIndex;

        if (selected instanceof BoneTrack) {
            result = ((BoneTrack) selected).getTargetBoneIndex();

        } else if (selected instanceof TransformTrack) {
            HasLocalTransform target = ((TransformTrack) selected).getTarget();
            if (target instanceof Joint) {
                result = ((Joint) target).getId();
            }
        }

        return result;
    }

    /**
     * Translate the bone track to put the point of support at the same world
     * Y-coordinate as it is for bind pose.
     */
    public void translateForSupport() {
        assert selected instanceof BoneTrack;

        SelectedSkeleton selectedSkeleton = cgm.getSkeleton();
        Object skeleton = selectedSkeleton.find();
        assert skeleton != null;
        Spatial subtree = selectedSkeleton.findSpatial();

        int numBones = cgm.getSkeleton().countBones();
        Matrix4f[] skinningMatrices = new Matrix4f[numBones];
        Matrix4f identity = new Matrix4f();
        for (int boneIndex = 0; boneIndex < numBones; boneIndex++) {
            skinningMatrices[boneIndex] = identity;
        }

        Vector3f vertexLocation = new Vector3f();
        Geometry[] geometryRef = new Geometry[1];
        int vertexIndex = SupportUtil.findSupport(subtree, skinningMatrices,
                vertexLocation, geometryRef);
        assert vertexIndex != -1;

        float bindSupportY = vertexLocation.y;
        boolean success = translateForSupport(bindSupportY);
        if (!success) {
            String message = "track translation failed";
            Maud.getModel().getMisc().setStatusMessage(message);
        }
    }

    /**
     * Translate the bone track to simulate traction at the point of support.
     *
     * @return true if successful, otherwise false
     */
    public boolean translateForTraction() {
        BoneTrack boneTrack = (BoneTrack) selected;
        int boneIndex = boneTrack.getTargetBoneIndex();

        SelectedSkeleton selectedSkeleton = cgm.getSkeleton();
        Object skeleton = selectedSkeleton.find();
        Pose tempPose;
        if (skeleton instanceof Armature) {
            tempPose = new Pose((Armature) skeleton);
        } else {
            tempPose = new Pose((Skeleton) skeleton);
        }

        Spatial subtree = selectedSkeleton.findSpatial();
        int numBones = tempPose.countBones();

        Matrix4f[] skinningMatrices = new Matrix4f[numBones];
        Object oldAnim = cgm.getAnimation().getReal();
        Geometry[] previousGeometryRef = new Geometry[1];
        Vector3f previousWorld = new Vector3f();
        Vector3f world = new Vector3f();
        Vector3f w = new Vector3f();
        Matrix3f sensMat = new Matrix3f();
        /*
         * Calculate a new bone translation for each keyframe.
         */
        float[] times = MaudUtil.getTrackTimes(selected);
        Vector3f[] translations = MaudUtil.getTrackTranslations(selected);
        TweenTransforms technique = Maud.getModel().getTweenTransforms();
        int numKeyframes = times.length;
        int previousVertexIndex = -1;
        for (int frameIndex = 0; frameIndex < numKeyframes; frameIndex++) {
            float trackTime = times[frameIndex];
            if (oldAnim instanceof Animation) {
                tempPose.setToAnimation((Animation) oldAnim, trackTime,
                        technique);
            } else {
                tempPose.setToClip((AnimClip) oldAnim, trackTime);
            }
            tempPose.skin(skinningMatrices);

            if (previousVertexIndex == -1) {
                world.zero(); // no offset for first keyframe
            } else {
                MyMesh.vertexWorldLocation(previousGeometryRef[0],
                        previousVertexIndex, skinningMatrices, w);
                previousWorld.subtractLocal(w);
                world.addLocal(previousWorld);
                /*
                 * Convert the world offset to a bone offset.
                 */
                SupportUtil.sensitivity(boneIndex, previousGeometryRef[0],
                        previousVertexIndex, tempPose, sensMat);
                float determinant = sensMat.determinant();
                if (FastMath.abs(determinant) <= FastMath.FLT_EPSILON) {
                    return false;
                }
                sensMat.invertLocal();
                Vector3f boneOffset = sensMat.mult(world, null);
                /*
                 * Modify the keyframe's translation.
                 */
                Vector3f translation = translations[frameIndex];
                translations[frameIndex] = translation.add(boneOffset);
            }
            /*
             * Using the original skinning matrices, pick a vertex to serve as
             * a reference for the next frame.
             */
            previousVertexIndex = SupportUtil.findSupport(subtree,
                    skinningMatrices, previousWorld, previousGeometryRef);
            assert previousVertexIndex != -1;
            assert previousGeometryRef[0] != null;
        }
        /*
         * Construct a new animation using the modified translations.
         */
        Object newSelected = null;

        TmpTracks.clear();
        Object[] oldTracks = cgm.getAnimation().getTracks();
        for (Object oldTrack : oldTracks) {
            Object newTrack;
            if (oldTrack == selected) {
                Quaternion[] rotations = MaudUtil.getTrackRotations(selected);
                Vector3f[] scales = MaudUtil.getTrackScales(selected);
                newTrack = MyAnimation.newBoneTrack(boneIndex, times,
                        translations, rotations, scales);
                newSelected = newTrack;
            } else {
                newTrack = TrackEdit.cloneTrack(oldTrack);
            }
            TmpTracks.add(newTrack);
        }

        Object newAnim = cgm.getAnimation().newAnim();
        TmpTracks.addAllToAnim(newAnim);

        String trackName = describe();
        String description = String.format("translate track %s for traction",
                trackName);
        editableCgm.replace(oldAnim, newAnim, description, newSelected);

        return true;
    }

    /**
     * Alter the track's first keyframe and end-time keyframe so that they
     * precisely match. If the track doesn't end with a keyframe, append one.
     *
     * @param endWeight how much weight to give to the pre-existing end-time
     * keyframe, if one exists (&ge;0, &le;1)
     */
    public void wrap(float endWeight) {
        Validate.fraction(endWeight, "end weight");
        assert selected != null;

        Object newSelected = null;
        TmpTracks.clear();
        Object[] oldTracks = cgm.getAnimation().getTracks();
        for (Object oldTrack : oldTracks) {
            Object newTrack;
            if (oldTrack == selected) {
                float duration = cgm.getAnimation().duration();
                if (selected instanceof Track) {
                    newTrack = TrackEdit.wrap((Track) selected, duration,
                            endWeight);
                    newSelected = newTrack;
                } else {
                    newTrack = TrackEdit.wrap((TransformTrack) selected,
                            duration, endWeight);
                    newSelected = newTrack;
                }
            } else {
                newTrack = TrackEdit.cloneTrack(oldTrack);
            }
            TmpTracks.add(newTrack);
        }

        Object newAnim = cgm.getAnimation().newAnim();
        TmpTracks.addAllToAnim(newAnim);

        String trackName = describe();
        String description = String.format("wrap track %s using end weight=%f",
                trackName, endWeight);
        Object oldAnim = cgm.getAnimation().getReal();
        editableCgm.replace(oldAnim, newAnim, description, newSelected);
    }
    // *************************************************************************
    // JmeCloneable methods

    /**
     * Don't use this method; use a {@link com.jme3.util.clone.Cloner} instead.
     *
     * @return never
     * @throws CloneNotSupportedException always
     */
    @Override
    public SelectedTrack clone() throws CloneNotSupportedException {
        super.clone();
        throw new CloneNotSupportedException("use a cloner");
    }

    /**
     * Callback from {@link com.jme3.util.clone.Cloner} to convert this
     * shallow-cloned instance into a deep-cloned one, using the specified
     * cloner and original to resolve copied fields.
     *
     * @param cloner the cloner currently cloning this control (not null)
     * @param original the object from which this object was shallow-cloned
     * (unused)
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        selected = cloner.clone(selected);
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public SelectedTrack jmeClone() {
        try {
            SelectedTrack clone = (SelectedTrack) super.clone();
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException(exception);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Count the number of distinct quaternions, without distinguishing 0 from
     * -0. TODO move to the Heart Library
     *
     * @param quaternions (unaffected)
     * @return the count (&ge;0)
     */
    private static int countNe(Quaternion[] quaternions) {
        if (quaternions == null) {
            return 0;
        }

        Set<Quaternion> distinct = new HashSet<>(quaternions.length);
        for (Quaternion rot : quaternions) {
            Quaternion standard = MyQuaternion.standardize(rot, null);
            distinct.add(standard);
        }
        int result = distinct.size();

        return result;
    }

    /**
     * Delete a range of keyframes in the selected track.
     *
     * @param startIndex index of the first keyframe to delete (&gt;0)
     * @param number number of keyframes to delete (&gt;0)
     */
    private void deleteRange(int startIndex, int number) {
        assert startIndex > 0 : startIndex;
        assert number > 0 : number;
        assert selected != null;

        Object newSelected = null;

        TmpTracks.clear();
        Object[] oldTracks = cgm.getAnimation().getTracks();
        for (Object oldTrack : oldTracks) {
            Object newTrack;
            if (oldTrack == selected) {
                if (oldTrack instanceof TransformTrack) {
                    newTrack = TrackEdit.deleteRange((TransformTrack) selected,
                            startIndex, number);
                } else {
                    newTrack = TrackEdit.deleteRange((Track) selected,
                            startIndex, number);
                }
                newSelected = newTrack;
            } else {
                newTrack = TrackEdit.cloneTrack(oldTrack);
            }
            TmpTracks.add(newTrack);
        }

        Object newAnim = cgm.getAnimation().newAnim();
        TmpTracks.addAllToAnim(newAnim);

        String trackName = describe();
        String eventDescription;
        if (number == 1) {
            float[] times = MaudUtil.getTrackTimes(selected);
            float time = times[startIndex];
            eventDescription = String.format(
                    "delete keyframe at t=%f from track %s", time, trackName);
        } else {
            eventDescription = String.format(
                    "delete %d keyframes from track %s", number, trackName);
        }
        Object oldAnim = cgm.getAnimation().getReal();
        editableCgm.replace(oldAnim, newAnim, eventDescription, newSelected);
    }

    /**
     * Enumerate all tracks in the loaded animation, in sorted order.
     *
     * @return a new list of descriptions
     */
    private List<String> listDescriptions() {
        List<TrackItem> items = cgm.getAnimation().listTracks();
        int numTracks = items.size();
        List<String> result = new ArrayList<>(numTracks);
        for (TrackItem item : items) {
            String description = item.describe();
            result.add(description);
        }
        Collections.sort(result);

        return result;
    }

    /**
     * Translate the bone track to put the point of support at the specified
     * Y-coordinate.
     *
     * @param cgmY world Y-coordinate for support
     * @return true if successful, otherwise false
     */
    private boolean translateForSupport(float cgmY) {
        BoneTrack boneTrack = (BoneTrack) selected;
        int boneIndex = boneTrack.getTargetBoneIndex();

        SelectedSkeleton selectedSkeleton = cgm.getSkeleton();
        Object skeleton = selectedSkeleton.find();
        assert skeleton != null;
        Spatial subtree = selectedSkeleton.findSpatial();
        Pose tempPose;
        if (skeleton instanceof Armature) {
            tempPose = new Pose((Armature) skeleton);
        } else {
            tempPose = new Pose((Skeleton) skeleton);
        }
        int numBones = tempPose.countBones();

        Matrix4f[] skinningMatrices = new Matrix4f[numBones];
        Object oldAnim = cgm.getAnimation().getReal();
        Geometry[] geometryRef = new Geometry[1];
        Vector3f world = new Vector3f();
        Matrix3f sensMat = new Matrix3f();
        /*
         * Calculate a new bone translation for each keyframe.
         */
        float[] times = boneTrack.getKeyFrameTimes();
        Vector3f[] translations = boneTrack.getTranslations();
        TweenTransforms techniques = Maud.getModel().getTweenTransforms();
        int numKeyframes = times.length;
        for (int frameIndex = 0; frameIndex < numKeyframes; frameIndex++) {
            float trackTime = times[frameIndex];
            if (oldAnim instanceof Animation) {
                tempPose.setToAnimation((Animation) oldAnim, trackTime,
                        techniques);
            } else {
                tempPose.setToClip((AnimClip) oldAnim, trackTime);
            }
            tempPose.skin(skinningMatrices);
            int vertexIndex = SupportUtil.findSupport(subtree, skinningMatrices,
                    world, geometryRef);
            assert vertexIndex != -1;
            world.x = 0f;
            world.y = cgmY - world.y;
            world.z = 0f;
            /*
             * Convert the world offset to a bone offset.
             */
            Geometry geometry = geometryRef[0];
            SupportUtil.sensitivity(boneIndex, geometry, vertexIndex, tempPose,
                    sensMat);
            float det = sensMat.determinant();
            if (FastMath.abs(det) <= FastMath.FLT_EPSILON) {
                return false;
            }
            sensMat.invertLocal();
            Vector3f boneOffset = sensMat.mult(world, null);
            /*
             * Modify the keyframe's translation.
             */
            Vector3f translation = translations[frameIndex];
            translations[frameIndex] = translation.add(boneOffset);
        }
        /*
         * Construct a new animation using the modified translations.
         */
        Object newSelected = null;

        TmpTracks.clear();
        Object[] oldTracks = cgm.getAnimation().getTracks();
        for (Object oldTrack : oldTracks) {
            Object newTrack;
            if (oldTrack == boneTrack) {
                Quaternion[] rotations = boneTrack.getRotations();
                Vector3f[] scales = boneTrack.getScales();
                newTrack = MyAnimation.newBoneTrack(boneIndex, times,
                        translations, rotations, scales);
                newSelected = newTrack;
            } else {
                newTrack = TrackEdit.cloneTrack(oldTrack);
            }
            TmpTracks.add(newTrack);
        }

        Object newAnim = cgm.getAnimation().newAnim();
        TmpTracks.addAllToAnim(newAnim);

        String trackName = describe();
        String description = String.format("translate track %s for support",
                trackName);
        editableCgm.replace(oldAnim, newAnim, description, newSelected);

        return true;
    }
}
