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
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
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
import jme3utilities.math.MyQuaternion;
import jme3utilities.math.MyVector3f;
import jme3utilities.wes.Pose;
import jme3utilities.wes.SmoothRotations;
import jme3utilities.wes.SmoothVectors;
import jme3utilities.wes.TrackEdit;
import jme3utilities.wes.TweenTransforms;
import maud.Maud;
import maud.MaudUtil;

/**
 * The MVC model of the selected track and the selected keyframe in a loaded
 * animation. TODO split off selected keyframe
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
     * selected track in the loaded animation, or null if none selected
     */
    private Track selected;
    // *************************************************************************
    // new methods exposed

    /**
     * Count the number of keyframes in the track.
     *
     * @return count (&ge;0)
     */
    public int countKeyframes() {
        int count = 0;
        if (selected != null) {
            float[] times = selected.getKeyFrameTimes();
            count = times.length;
        }

        assert count >= 0 : count;
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
            Quaternion[] rotations = MyAnimation.getRotations(selected);
            if (rotations != null) {
                Set<Quaternion> distinct = new HashSet<>(rotations.length);
                for (Quaternion rot : rotations) {
                    Quaternion standard = MyQuaternion.standardize(rot, null);
                    distinct.add(standard);
                }
                count = distinct.size();
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
        int count = 0;
        if (selected != null) {
            Vector3f[] scales = MyAnimation.getScales(selected);
            if (scales != null) {
                count = MyVector3f.countNe(scales);
            }
        }

        return count;
    }

    /**
     * Count the number of distinct translations, without distinguishing 0 from
     * -0.
     *
     * @return count (&ge;0)
     */
    public int countTranslations() {
        int count = 0;
        if (selected != null) {
            Vector3f[] translations = MyAnimation.getTranslations(selected);
            if (translations != null) {
                count = MyVector3f.countNe(translations);
            }
        }

        return count;
    }

    /**
     * Delete the specified number of keyframes following the selected one.
     *
     * @param number number of keyframes to delete (&ge;1)
     */
    public void deleteNextKeyframes(int number) {
        Validate.positive(number, "number");
        assert selected != null;
        int frameIndex = findKeyframeIndex();
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
        int frameIndex = findKeyframeIndex();
        assert frameIndex != -1;

        deleteRange(frameIndex - number, number);
    }

    /**
     * Delete the selected keyframe, which mustn't be the 1st keyframe in the
     * track.
     */
    public void deleteSelectedKeyframe() {
        assert selected != null;
        int frameIndex = findKeyframeIndex();
        assert frameIndex > 0 : frameIndex;

        deleteRange(frameIndex, 1);
    }

    /**
     * Describe the track in the context of its animation.
     *
     * @return a textual description, or "" if none selected (not null)
     */
    public String describe() {
        String result = "";
        if (selected != null) {
            AnimControl animControl = cgm.getAnimControl().find();
            assert animControl != null;
            result = MyAnimation.describe(selected, animControl);
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
        assert selected != null;

        float duration = cgm.getAnimation().getDuration();
        int endIndex = MyAnimation.findKeyframeIndex(selected, duration);
        boolean result;
        if (endIndex >= 0) {
            result = true;
        } else {
            result = false;
        }

        return result;
    }

    /**
     * Access the selected track in the loaded animation.
     *
     * @return the pre-existing instance, or null if none
     */
    Track find() {
        return selected;
    }

    /**
     * Find the index of the keyframe (if any) at the current animation time.
     *
     * @return the keyframe's index, or -1 if no keyframe at that time
     */
    public int findKeyframeIndex() {
        assert selected != null;

        float time = cgm.getAnimation().getTime();
        int frameIndex = findKeyframeIndex(time);

        return frameIndex;
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

        int frameIndex = MyAnimation.findKeyframeIndex(selected, time);
        return frameIndex;
    }

    /**
     * Find the index of the 1st keyframe at or after the specified time.
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
                    = MyAnimation.findPreviousKeyframeIndex(selected, time);
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
        int frameIndex = MyAnimation.findPreviousKeyframeIndex(selected, time);
        return frameIndex;
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
        assert selected instanceof BoneTrack;
        float time = cgm.getAnimation().getTime();
        assert time > 0f : time;
        float duration = cgm.getAnimation().getDuration();
        assert time <= duration : time;
        int frameIndex = findKeyframeIndex();
        assert frameIndex == -1 : frameIndex;

        Animation newAnimation = newAnimation();
        Track newSelected = null;
        Animation oldAnimation = cgm.getAnimation().getReal();
        Track[] oldTracks = oldAnimation.getTracks();
        for (Track track : oldTracks) {
            Track clone;
            if (track == selected) {
                BoneTrack boneTrack = (BoneTrack) track;
                int boneIndex = boneTrack.getTargetBoneIndex();
                Pose pose = cgm.getPose().get();
                Transform user = pose.userTransform(boneIndex, null);
                clone = TrackEdit.insertKeyframe(selected, time, user);
                newSelected = clone;
            } else {
                clone = track.clone();
            }
            newAnimation.addTrack(clone);
        } // TODO new bone tracks?

        String description = String.format("insert a keyframe at t=%f", time);
        editableCgm.replace(oldAnimation, newAnimation, description,
                newSelected);
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
        TrackItem item = null;
        if (selected != null) {
            String animationName = cgm.getAnimation().getName();
            String animControlName = cgm.getAnimControl().name();
            AnimControl animControl = cgm.getAnimControl().find();
            item = new TrackItem(animationName, animControlName, animControl,
                    selected);
        }

        return item;
    }

    /**
     * Find the time of the indexed keyframe in the selected track.
     *
     * @param keyframeIndex which keyframe (&ge;0)
     * @return animation time (&ge;0)
     */
    public float keyframeTime(int keyframeIndex) {
        float[] times = selected.getKeyFrameTimes();
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
        float[] times = selected.getKeyFrameTimes();
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
            float[] keyframes = selected.getKeyFrameTimes();
            result = new ArrayList<>(keyframes.length);
            for (float keyframe : keyframes) {
                String menuItem = String.format("%.3f", keyframe);
                result.add(menuItem);
            }
        }

        return result;
    }

    /**
     * Reduce the track's keyframes by the specified factor.
     *
     * @param factor reduction factor (&ge;2)
     */
    public void reduce(int factor) {
        Validate.inRange(factor, "reduction factor", 2, Integer.MAX_VALUE);
        assert selected != null;

        Animation newAnimation = newAnimation();
        Track newSelected = null;
        Animation oldAnimation = cgm.getAnimation().getReal();
        Track[] oldTracks = oldAnimation.getTracks();
        for (Track track : oldTracks) {
            Track clone;
            if (track == selected) {
                clone = TrackEdit.reduce(track, factor);
                newSelected = clone;
            } else {
                clone = track.clone();
            }
            newAnimation.addTrack(clone);
        }

        String trackName = describe();
        String description = String.format(
                "thin the keyframes in track %s by %dx", trackName, factor);
        editableCgm.replace(oldAnimation, newAnimation, description,
                newSelected);
    }

    /**
     * Replace the keyframe at the current animation time.
     */
    public void replaceKeyframe() {
        assert selected instanceof BoneTrack;
        int frameIndex = findKeyframeIndex();
        assert frameIndex >= 0 : frameIndex;

        Animation newAnimation = newAnimation();
        Track newSelected = null;
        Animation oldAnimation = cgm.getAnimation().getReal();
        Track[] oldTracks = oldAnimation.getTracks();
        for (Track track : oldTracks) {
            Track clone;
            if (track == selected) {
                BoneTrack boneTrack = (BoneTrack) track;
                Pose pose = cgm.getPose().get();
                int boneIndex = boneTrack.getTargetBoneIndex();
                Transform user = pose.userTransform(boneIndex, null);
                clone = TrackEdit.replaceKeyframe(boneTrack, frameIndex, user);
                newSelected = clone;
            } else {
                clone = track.clone();
            }
            newAnimation.addTrack(clone);
        }

        float time = cgm.getAnimation().getTime();
        String trackName = describe();
        String description = String.format(
                "replace keyframe at t=%f in track %s", time, trackName);
        editableCgm.replace(oldAnimation, newAnimation, description,
                newSelected);
    }

    /**
     * Resample the track at the specified rate.
     *
     * @param sampleRate sample rate (in frames per second, &gt;0)
     */
    public void resampleAtRate(float sampleRate) {
        Validate.positive(sampleRate, "sample rate");
        assert selected != null;

        Animation newAnimation = newAnimation();
        Track newSelected = null;
        Animation oldAnimation = cgm.getAnimation().getReal();

        Track[] oldTracks = oldAnimation.getTracks();
        for (Track track : oldTracks) {
            Track clone;
            if (track == selected) {
                TweenTransforms technique
                        = Maud.getModel().getTweenTransforms();
                float duration = oldAnimation.getLength();
                clone = technique.resampleAtRate(selected, sampleRate,
                        duration);
                newSelected = clone;
            } else {
                clone = track.clone();
            }
            newAnimation.addTrack(clone);
        }

        String trackName = describe();
        String description = String.format("resample track %s at %f fps",
                trackName, sampleRate);
        editableCgm.replace(oldAnimation, newAnimation, description,
                newSelected);
    }

    /**
     * Resample the track to the specified number of samples.
     *
     * @param numSamples number of samples (&ge;2)
     */
    public void resampleToNumber(int numSamples) {
        Validate.inRange(numSamples, "number of samples", 2, Integer.MAX_VALUE);
        assert selected != null;

        Animation newAnimation = newAnimation();
        Track newSelected = null;
        Animation oldAnimation = cgm.getAnimation().getReal();

        Track[] oldTracks = oldAnimation.getTracks();
        for (Track track : oldTracks) {
            Track clone;
            if (track == selected) {
                TweenTransforms technique
                        = Maud.getModel().getTweenTransforms();
                float duration = oldAnimation.getLength();
                assert duration > 0f : duration;
                clone = technique.resampleToNumber(selected, numSamples,
                        duration);
                newSelected = clone;
            } else {
                clone = track.clone();
            }
            newAnimation.addTrack(clone);
        }

        String trackName = describe();
        String description = String.format("resample track %s to %d samples",
                trackName, numSamples);
        editableCgm.replace(oldAnimation, newAnimation, description,
                newSelected);
    }

    /**
     * Select the specified track.
     *
     * @param track which track to select, or null to deselect
     */
    void select(Track track) {
        if (track != null) {
            assert cgm.getAnimation().hasTrack(track);
        }
        selected = track;
    }

    /**
     * Select the 1st keyframe in the track.
     */
    public void selectFirstKeyframe() {
        float[] times = selected.getKeyFrameTimes();
        float t = times[0];
        cgm.getAnimation().setTime(t);
    }

    /**
     * Select the last keyframe in the track.
     */
    public void selectLastKeyframe() {
        if (selected != null) {
            float t = lastKeyframeTime();
            cgm.getAnimation().setTime(t);
        }
    }

    /**
     * Select the nearest keyframe in the track.
     */
    public void selectNearestKeyframe() {
        if (selected != null) {
            LoadedAnimation animation = cgm.getAnimation();
            float time = animation.getTime();
            int frameIndex = MyAnimation.findKeyframeIndex(selected, time);
            if (frameIndex == -1) {
                float next = nextKeyframeTime();
                float toNext = next - time;
                assert toNext >= 0f : toNext;
                float previous = previousKeyframeTime();
                float toPrevious = time - previous;
                assert toPrevious >= 0f : toPrevious;
                if (toPrevious < toNext) {
                    animation.setTime(previous);
                } else {
                    animation.setTime(next);
                }
            }
        }
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
     * Select the next keyframe in the track.
     */
    public void selectNextKeyframe() {
        if (selected != null) {
            float time = nextKeyframeTime();
            if (time < Float.POSITIVE_INFINITY) {
                cgm.getAnimation().setTime(time);
            }
        }
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
     * Select the previous keyframe in the track.
     */
    public void selectPreviousKeyframe() {
        if (selected != null) {
            float time = previousKeyframeTime();
            if (time >= 0f) {
                cgm.getAnimation().setTime(time);
            }
        }
    }

    /**
     * Select the bone or spatial that is the track's target.
     */
    public void selectTarget() {
        if (selected instanceof BoneTrack) {
            BoneTrack boneTrack = (BoneTrack) selected;
            int boneIndex = boneTrack.getTargetBoneIndex();
            cgm.getBone().select(boneIndex);
            Maud.gui.tools.select("bone");
        } else if (selected instanceof SpatialTrack) {
            SpatialTrack spatialTrack = (SpatialTrack) selected;
            Spatial spatial = spatialTrack.getTrackSpatial();
            cgm.getSpatial().select(spatial);
            Maud.gui.tools.select("spatial");
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
                Track track = item.getTrack();
                select(track);
                return;
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * Alter which C-G model contains the track.
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
     * Adjust the timing of the selected frame. TODO use
     * EditableCgm.setKeyframes()
     *
     * @param newTime new time for the frame (in seconds, &gt;0)
     */
    public void setFrameTime(float newTime) {
        Validate.positive(newTime, "new time");
        assert selected != null;

        Animation newAnimation = newAnimation();

        int frameIndex = findKeyframeIndex();
        Track newSelected = null;
        Animation oldAnimation = cgm.getAnimation().getReal();
        Track[] oldTracks = oldAnimation.getTracks();
        for (Track track : oldTracks) {
            Track clone;
            if (track == selected) {
                clone = TrackEdit.setFrameTime(selected, frameIndex, newTime);
                assert clone != null;
                newSelected = clone;
            } else {
                clone = track.clone();
            }
            newAnimation.addTrack(clone);
        }
        cgm.getAnimation().setTime(newTime);

        String trackName = describe();
        String eventDescription = String.format(
                "adjust the timing of frame%s in track %s",
                MaudUtil.formatIndex(frameIndex), trackName);
        editableCgm.replace(oldAnimation, newAnimation, eventDescription,
                newSelected);
    }

    /**
     * Alter all rotations to match the displayed pose. Requires a bone track.
     */
    public void setRotationAll() {
        assert selected instanceof BoneTrack;

        Pose pose = cgm.getPose().get();
        int boneIndex = targetBoneIndex();
        Quaternion poseRotation = pose.userRotation(boneIndex, null);

        float[] times = selected.getKeyFrameTimes();
        Vector3f[] translations = MyAnimation.getTranslations(selected);
        Vector3f[] scales = MyAnimation.getScales(selected);

        Quaternion[] rotations = new Quaternion[times.length];
        for (int i = 0; i < times.length; i++) {
            rotations[i] = poseRotation.clone();
        }
        editableCgm.setKeyframes(times, translations, rotations, scales);
    }

    /**
     * Alter all scales to match the displayed pose. Requires a bone track.
     */
    public void setScaleAll() {
        assert selected instanceof BoneTrack;

        Pose pose = cgm.getPose().get();
        int boneIndex = targetBoneIndex();
        Vector3f poseScale = pose.userScale(boneIndex, null);

        float[] times = selected.getKeyFrameTimes();
        Vector3f[] translations = MyAnimation.getTranslations(selected);
        Quaternion[] rotations = MyAnimation.getRotations(selected);

        Vector3f[] scales = new Vector3f[times.length];
        for (int i = 0; i < times.length; i++) {
            scales[i] = poseScale.clone();
        }
        editableCgm.setKeyframes(times, translations, rotations, scales);
    }

    /**
     * Alter all translations to match the displayed pose. Requires a bone
     * track.
     */
    public void setTranslationAll() {
        assert selected instanceof BoneTrack;

        Pose pose = cgm.getPose().get();
        int boneIndex = targetBoneIndex();
        Vector3f poseTranslation = pose.userTranslation(boneIndex, null);

        float[] times = selected.getKeyFrameTimes();
        Quaternion[] rotations = MyAnimation.getRotations(selected);
        Vector3f[] scales = MyAnimation.getScales(selected);

        Vector3f[] translations = new Vector3f[times.length];
        for (int i = 0; i < times.length; i++) {
            translations[i] = poseTranslation.clone();
        }
        editableCgm.setKeyframes(times, translations, rotations, scales);
    }

    /**
     * Smooth the track.
     */
    public void smooth() {
        assert selected != null;

        Animation newAnimation = newAnimation();
        Track newSelected = null;
        Animation oldAnimation = cgm.getAnimation().getReal();
        float duration = oldAnimation.getLength();
        Track[] oldTracks = oldAnimation.getTracks();
        for (Track track : oldTracks) {
            Track clone;
            if (track == selected) {
                clone = TrackEdit.smooth(selected, 0.2f,
                        SmoothVectors.LoopLerp, SmoothRotations.LoopNlerp,
                        SmoothVectors.LoopLerp, duration);
                newSelected = clone;
            } else {
                clone = track.clone();
            }
            newAnimation.addTrack(clone);
        }

        String trackName = describe();
        String description = String.format("smooth track %s", trackName);
        editableCgm.replace(oldAnimation, newAnimation, description,
                newSelected);
    }

    /**
     * Determine the index of the target bone.
     *
     * @return the bone index, or noBoneIndex if none
     */
    public int targetBoneIndex() {
        int result = SelectedSkeleton.noBoneIndex;
        if (selected instanceof BoneTrack) {
            BoneTrack boneTrack = (BoneTrack) selected;
            result = boneTrack.getTargetBoneIndex();
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
        Skeleton skeleton = selectedSkeleton.find();
        assert skeleton != null;
        Spatial subtree = selectedSkeleton.findSpatial();

        int numBones = skeleton.getBoneCount();
        Matrix4f[] skinningMatrices = new Matrix4f[numBones];
        Matrix4f identity = new Matrix4f();
        for (int boneIndex = 0; boneIndex < numBones; boneIndex++) {
            skinningMatrices[boneIndex] = identity;
        }

        Vector3f vertexLocation = new Vector3f();
        Geometry[] geometryRef = new Geometry[1];
        int vertexIndex = MaudUtil.findSupport(subtree, skinningMatrices,
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
        Skeleton skeleton = selectedSkeleton.find();
        assert skeleton != null;
        Spatial subtree = selectedSkeleton.findSpatial();
        Pose tempPose = new Pose(skeleton);
        int numBones = tempPose.countBones();

        Matrix4f[] skinningMatrices = new Matrix4f[numBones];
        Animation oldAnimation = cgm.getAnimation().getReal();
        Geometry[] previousGeometryRef = new Geometry[1];
        Vector3f previousWorld = new Vector3f();
        Vector3f world = new Vector3f();
        Vector3f w = new Vector3f();
        Matrix3f sensMat = new Matrix3f();
        /*
         * Calculate a new bone translation for each keyframe.
         */
        float[] times = selected.getKeyFrameTimes();
        Vector3f[] translations = MyAnimation.getTranslations(selected);
        TweenTransforms technique = Maud.getModel().getTweenTransforms();
        int numKeyframes = times.length;
        int previousVertexIndex = -1;
        for (int frameIndex = 0; frameIndex < numKeyframes; frameIndex++) {
            float trackTime = times[frameIndex];
            tempPose.setToAnimation(oldAnimation, trackTime, technique);
            tempPose.skin(skinningMatrices);

            if (previousVertexIndex == -1) {
                world.zero(); // no offset for 1st keyframe
            } else {
                MyMesh.vertexWorldLocation(previousGeometryRef[0],
                        previousVertexIndex, skinningMatrices, w);
                previousWorld.subtractLocal(w);
                world.addLocal(previousWorld);
                /*
                 * Convert the world offset to a bone offset.
                 */
                MaudUtil.sensitivity(boneIndex, previousGeometryRef[0],
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
            previousVertexIndex = MaudUtil.findSupport(subtree,
                    skinningMatrices, previousWorld, previousGeometryRef);
            assert previousVertexIndex != -1;
            assert previousGeometryRef[0] != null;
        }
        /*
         * Construct a new animation using the modified translations.
         */
        Animation newAnimation = newAnimation();
        Track newSelected = null;
        Track[] oldTracks = oldAnimation.getTracks();
        for (Track oldTrack : oldTracks) {
            Track clone;
            if (oldTrack == selected) {
                Quaternion[] rotations = MyAnimation.getRotations(selected);
                Vector3f[] scales = MyAnimation.getScales(selected);
                clone = MyAnimation.newBoneTrack(boneIndex, times, translations,
                        rotations, scales);
                newSelected = clone;
            } else {
                clone = oldTrack.clone();
            }
            newAnimation.addTrack(clone);
        }

        String trackName = describe();
        String description = String.format("translate track %s for traction",
                trackName);
        editableCgm.replace(oldAnimation, newAnimation, description,
                newSelected);

        return true;
    }

    /**
     * Alter the track's 1st keyframe and end-time keyframe so that they
     * precisely match. If the track doesn't end with a keyframe, append one.
     *
     * @param endWeight how much weight to give to the pre-existing end-time
     * keyframe, if one exists (&ge;0, &le;1)
     */
    public void wrap(float endWeight) {
        Validate.fraction(endWeight, "end weight");
        assert selected != null;

        Animation newAnimation = newAnimation();
        Track newSelected = null;
        Animation oldAnimation = cgm.getAnimation().getReal();
        Track[] oldTracks = oldAnimation.getTracks();
        for (Track track : oldTracks) {
            Track clone;
            if (track == selected) {
                float duration = cgm.getAnimation().getDuration();
                clone = TrackEdit.wrap(selected, duration, endWeight);
                newSelected = clone;
            } else {
                clone = track.clone();
            }
            newAnimation.addTrack(clone);
        }

        String trackName = describe();
        String description = String.format("wrap track %s using end weight=%f",
                trackName, endWeight);
        editableCgm.replace(oldAnimation, newAnimation, description,
                newSelected);
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
     * Delete a range of keyframes in the selected track.
     *
     * @param startIndex index of 1st keyframe to delete (&gt;0)
     * @param number number of keyframes to delete (&gt;0)
     */
    private void deleteRange(int startIndex, int number) {
        assert startIndex > 0 : startIndex;
        assert number > 0 : number;
        assert selected != null;

        Animation newAnimation = newAnimation();
        Track newSelected = null;
        Animation oldAnimation = cgm.getAnimation().getReal();
        Track[] oldTracks = oldAnimation.getTracks();
        for (Track track : oldTracks) {
            Track clone;
            if (track == selected) {
                clone = TrackEdit.deleteRange(selected, startIndex, number);
                newSelected = clone;
            } else {
                clone = track.clone();
            }
            newAnimation.addTrack(clone);
        }

        String trackName = describe();
        String eventDescription;
        if (number == 1) {
            float[] times = selected.getKeyFrameTimes();
            float time = times[startIndex];
            eventDescription = String.format(
                    "delete keyframe at t=%f from track %s", time, trackName);
        } else {
            eventDescription = String.format(
                    "delete %d keyframes from track %s", number, trackName);
        }
        editableCgm.replace(oldAnimation, newAnimation, eventDescription,
                newSelected);
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
     * Create an empty animation with the same name and duration as the selected
     * animation.
     *
     * @return a new instance with no tracks
     */
    private Animation newAnimation() {
        float duration = cgm.getAnimation().getDuration();
        String name = cgm.getAnimation().getName();
        Animation result = new Animation(name, duration);

        return result;
    }

    /**
     * Find the time of the next keyframe in the selected track.
     *
     * @return animation time (&ge;0) or +Infinity if none found
     */
    private float nextKeyframeTime() {
        float result = Float.POSITIVE_INFINITY;
        float time = cgm.getAnimation().getTime();
        float[] times = selected.getKeyFrameTimes();
        for (int iFrame = 0; iFrame < times.length; iFrame++) {
            if (times[iFrame] > time) {
                result = times[iFrame];
                break;
            }
        }

        return result;
    }

    /**
     * Find the time of the previous keyframe in the selected track.
     *
     * @return animation time (&ge;0) or -Infinity if none found
     */
    private float previousKeyframeTime() {
        float result = Float.NEGATIVE_INFINITY;
        float time = cgm.getAnimation().getTime();
        float[] times = selected.getKeyFrameTimes();
        for (int iFrame = times.length - 1; iFrame >= 0; iFrame--) {
            if (times[iFrame] < time) {
                result = times[iFrame];
                break;
            }
        }

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
        Skeleton skeleton = selectedSkeleton.find();
        assert skeleton != null;
        Spatial subtree = selectedSkeleton.findSpatial();
        Pose tempPose = new Pose(skeleton);
        int numBones = tempPose.countBones();

        Matrix4f[] skinningMatrices = new Matrix4f[numBones];
        Animation oldAnimation = cgm.getAnimation().getReal();
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
            tempPose.setToAnimation(oldAnimation, trackTime, techniques);
            tempPose.skin(skinningMatrices);
            int vertexIndex = MaudUtil.findSupport(subtree, skinningMatrices,
                    world, geometryRef);
            assert vertexIndex != -1;
            world.x = 0f;
            world.y = cgmY - world.y;
            world.z = 0f;
            /*
             * Convert the world offset to a bone offset.
             */
            Geometry geometry = geometryRef[0];
            MaudUtil.sensitivity(boneIndex, geometry, vertexIndex, tempPose,
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
        Animation newAnimation = newAnimation();
        Track newSelected = null;
        Track[] oldTracks = oldAnimation.getTracks();
        for (Track oldTrack : oldTracks) {
            Track clone;
            if (oldTrack == boneTrack) {
                Quaternion[] rotations = boneTrack.getRotations();
                Vector3f[] scales = boneTrack.getScales();
                clone = MyAnimation.newBoneTrack(boneIndex, times, translations,
                        rotations, scales);
                newSelected = clone;
            } else {
                clone = oldTrack.clone();
            }
            newAnimation.addTrack(clone);
        }

        String trackName = describe();
        String description = String.format("translate track %s for support",
                trackName);
        editableCgm.replace(oldAnimation, newAnimation, description,
                newSelected);

        return true;
    }
}
