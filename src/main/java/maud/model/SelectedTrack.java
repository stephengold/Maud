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

import com.jme3.animation.Animation;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Track;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyAnimation;
import jme3utilities.Validate;
import jme3utilities.math.MyVector3f;
import maud.Pose;
import maud.Util;

/**
 * The MVC model of the selected track in the Maud application.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SelectedTrack implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            SelectedTrack.class.getName());
    // *************************************************************************
    // fields

    /**
     * editable CG model containing the track, if any (set by
     * {@link #setCgm(LoadedCGModel)})
     */
    private EditableCgm editableCgm;
    /**
     * loaded CG model containing the track (set by
     * {@link #setCgm(LoadedCGModel)})
     */
    private LoadedCgm loadedCgm;
    // *************************************************************************
    // new methods exposed

    /**
     * Count the number of keyframes in the selected bone track.
     *
     * @return count (&ge;0)
     */
    public int countKeyframes() {
        int count;
        BoneTrack track = findTrack();
        if (track == null) {
            count = 0;
        } else {
            float[] times = track.getTimes();
            count = times.length;
        }

        assert count >= 0 : count;
        return count;
    }

    /**
     * Count the number of distinct rotations in the selected track.
     *
     * @return count (&ge;0)
     */
    public int countRotations() {
        int count;
        BoneTrack track = findTrack();
        if (track == null) {
            count = 0;
        } else {
            Quaternion[] rotations = track.getRotations();
            Set<Quaternion> distinct = new HashSet<>(rotations.length);
            for (Quaternion rot : rotations) {
                distinct.add(rot);
            }
            count = distinct.size();
        }

        return count;
    }

    /**
     * Count the number of distinct scales in the selected track.
     *
     * @return count (&ge;0)
     */
    public int countScales() {
        int count;
        BoneTrack track = findTrack();
        if (track == null) {
            count = 0;
        } else {
            Vector3f[] scales = track.getScales();
            if (scales == null) {
                count = 0;
            } else {
                count = MyVector3f.countDistinct(scales);
            }
        }

        return count;
    }

    /**
     * Count the number of distinct translations in the selected track.
     *
     * @return count (&ge;0)
     */
    public int countTranslations() {
        int count = 0;
        BoneTrack track = findTrack();
        if (track != null) {
            Vector3f[] offsets = track.getTranslations();
            count = MyVector3f.countDistinct(offsets);
        }

        return count;
    }

    /**
     * Delete the selected keyframe, which mustn't be the 1st keyframe in the
     * track.
     */
    public void deleteSingleKeyframe() {
        if (!isTrackSelected()) {
            return;
        }
        int frameIndex = findKeyframeIndex();
        if (frameIndex < 1) {
            return;
        }
        int boneIndex = loadedCgm.bone.getIndex();
        assert boneIndex >= 0 : boneIndex;

        Animation newAnimation = newAnimation();
        BoneTrack selectedTrack = findTrack();
        Animation oldAnimation = loadedCgm.animation.getAnimation();
        Track[] oldTracks = oldAnimation.getTracks();
        for (Track track : oldTracks) {
            Track clone;
            if (track == selectedTrack) {
                clone = MyAnimation.deleteKeyframe(selectedTrack, frameIndex);
            } else {
                clone = track.clone();
            }
            newAnimation.addTrack(clone);
        }

        editableCgm.replaceAnimation(oldAnimation, newAnimation,
                "delete single keyframe");
    }

    /**
     * Find the index of the keyframe (if any) in the selected track at the
     * current track time.
     *
     * @return keyframe index, or -1 if no keyframe
     */
    public int findKeyframeIndex() {
        BoneTrack track = findTrack();
        float time = loadedCgm.animation.getTime();
        int frameIndex = Util.findKeyframeIndex(track, time);

        return frameIndex;
    }

    /**
     * Find the track for the selected bone in the loaded animation.
     *
     * @return the pre-existing instance, or null if none
     */
    BoneTrack findTrack() {
        if (!loadedCgm.bone.isSelected()) {
            return null;
        } else if (!loadedCgm.animation.isReal()) {
            return null;
        }

        Animation anim = loadedCgm.animation.getAnimation();
        int boneIndex = loadedCgm.bone.getIndex();
        BoneTrack track = MyAnimation.findTrack(anim, boneIndex);

        return track;
    }

    /**
     * Using the current pose, add a keyframe to the track at the current
     * animation time.
     */
    public void insertSingleKeyframe() {
        if (!isTrackSelected()) {
            return;
        }
        int frameIndex = findKeyframeIndex();
        if (frameIndex != -1) {
            return;
        }
        float time = loadedCgm.animation.getTime();
        assert time > 0f : time;
        float duration = loadedCgm.animation.getDuration();
        assert time <= duration : time;

        Animation newAnimation = newAnimation();
        BoneTrack selectedTrack = findTrack();
        Animation oldAnimation = loadedCgm.animation.getAnimation();
        Track[] oldTracks = oldAnimation.getTracks();
        for (Track track : oldTracks) {
            Track clone;
            if (track == selectedTrack) {
                Pose pose = loadedCgm.pose.getPose();
                int boneIndex = selectedTrack.getTargetBoneIndex();
                Transform user = pose.userTransform(boneIndex, null);
                clone = Util.addKeyframe(selectedTrack, time, user);
            } else {
                clone = track.clone();
            }
            newAnimation.addTrack(clone);
        }

        editableCgm.replaceAnimation(oldAnimation, newAnimation,
                "insert single keyframe");
    }

    /**
     * Test whether a bone track is selected.
     *
     * @return true if one is selected, false if none is selected
     */
    public boolean isTrackSelected() {
        if (loadedCgm.bone.isSelected()) {
            if (!loadedCgm.animation.isReal()) {
                return false;
            }
            Track track = findTrack();
            if (track == null) {
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    /**
     * Find the time of the indexed keyframe in the selected bone track.
     *
     * @param keyframeIndex which keyframe (&ge;0)
     * @return animation time (&ge;0)
     */
    public float keyframeTime(int keyframeIndex) {
        BoneTrack track = findTrack();
        float[] times = track.getTimes();
        float result = times[keyframeIndex];

        assert result >= 0f : result;
        return result;
    }

    /**
     * Find the time of the last keyframe in the selected bone track.
     *
     * @return animation time (&ge;0)
     */
    public float lastKeyframeTime() {
        BoneTrack track = findTrack();
        float[] times = track.getTimes();
        int lastIndex = times.length - 1;
        float result = times[lastIndex];

        return result;
    }

    /**
     * Enumerate all keyframes of the selected bone in the loaded animation.
     *
     * @return a new list, or null if no options
     */
    public List<String> listKeyframes() {
        List<String> result = null;
        if (!loadedCgm.animation.isReal()) {
            logger.log(Level.INFO, "No animation is selected.");
        } else if (!loadedCgm.bone.isSelected()) {
            logger.log(Level.INFO, "No bone is selected.");
        } else if (!isTrackSelected()) {
            logger.log(Level.INFO, "No track is selected.");
        } else {
            BoneTrack track = findTrack();
            float[] keyframes = track.getTimes();

            result = new ArrayList<>(20);
            for (float keyframe : keyframes) {
                String menuItem = String.format("%.3f", keyframe);
                result.add(menuItem);
            }
        }

        return result;
    }

    /**
     * Reduce the selected bone track by the specified factor.
     *
     * @param factor reduction factor (&ge;2)
     */
    public void reduceTrack(int factor) {
        Validate.inRange(factor, "reduction factor", 2, Integer.MAX_VALUE);
        assert loadedCgm.bone.hasTrack();

        Animation newAnimation = newAnimation();
        BoneTrack selectedTrack = findTrack();
        Animation oldAnimation = loadedCgm.animation.getAnimation();
        Track[] oldTracks = oldAnimation.getTracks();
        for (Track track : oldTracks) {
            Track clone;
            if (track == selectedTrack) {
                clone = MyAnimation.reduce(selectedTrack, factor);
            } else {
                clone = track.clone();
            }
            newAnimation.addTrack(clone);
        }

        editableCgm.replaceAnimation(oldAnimation, newAnimation,
                "thin keyframes in a single bone track");
    }

    /**
     * Select the 1st keyframe in the track.
     */
    public void selectFirstKeyframe() {
        BoneTrack track = findTrack();
        if (track != null) {
            float[] times = track.getTimes();
            float t = times[0];
            loadedCgm.animation.setTime(t);
        }
    }

    /**
     * Select the last keyframe in the track.
     */
    public void selectLastKeyframe() {
        BoneTrack track = findTrack();
        if (track != null) {
            float[] times = track.getTimes();
            int lastIndex = times.length - 1;
            float t = times[lastIndex];
            loadedCgm.animation.setTime(t);
        }
    }

    /**
     * Select the next keyframe in the track.
     */
    public void selectNextKeyframe() {
        BoneTrack track = findTrack();
        if (track != null) {
            float time = loadedCgm.animation.getTime();
            float[] times = track.getTimes();
            for (int iFrame = 0; iFrame < times.length; iFrame++) {
                if (times[iFrame] > time) {
                    loadedCgm.animation.setTime(times[iFrame]);
                    break;
                }
            }
        }
    }

    /**
     * Select the previous keyframe in the track.
     */
    public void selectPreviousKeyframe() {
        BoneTrack track = findTrack();
        if (track != null) {
            float time = loadedCgm.animation.getTime();
            float[] times = track.getTimes();
            for (int iFrame = times.length - 1; iFrame >= 0; iFrame--) {
                if (times[iFrame] < time) {
                    loadedCgm.animation.setTime(times[iFrame]);
                    break;
                }
            }
        }
    }

    /**
     * Alter which CG model contains the track.
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
     * Alter all rotations in the selected track to match the displayed pose.
     */
    public void setTrackRotationAll() {
        BoneTrack track = findTrack();
        if (track != null) {
            Quaternion poseRotation = loadedCgm.bone.userRotation(null);

            float[] times = track.getTimes();
            Vector3f[] translations = track.getTranslations();
            Quaternion[] rotations = track.getRotations();
            Vector3f[] scales = track.getScales();

            for (Quaternion rotation : rotations) {
                rotation.set(poseRotation);
            }
            editableCgm.setKeyframes(times, translations, rotations, scales);
        }
    }

    /**
     * Alter all scales in the selected track to match the displayed pose.
     */
    public void setTrackScaleAll() {
        BoneTrack track = findTrack();
        if (track != null) {
            Vector3f poseScale = loadedCgm.bone.userScale(null);

            float[] times = track.getTimes();
            Vector3f[] translations = track.getTranslations();
            Quaternion[] rotations = track.getRotations();
            Vector3f[] scales = track.getScales();

            if (scales == null) {
                int numKeyframes = times.length;
                scales = new Vector3f[numKeyframes];
            }
            for (Vector3f scale : scales) {
                scale.set(poseScale);
            }
            editableCgm.setKeyframes(times, translations, rotations, scales);
        }
    }

    /**
     * Alter all translations in the selected track to match the displayed pose.
     */
    public void setTrackTranslationAll() {
        BoneTrack track = findTrack();
        if (track != null) {
            Vector3f poseTranslation = loadedCgm.bone.userTranslation(null);

            float[] times = track.getTimes();
            Vector3f[] translations = track.getTranslations();
            Quaternion[] rotations = track.getRotations();
            Vector3f[] scales = track.getScales();

            for (Vector3f translation : translations) {
                translation.set(poseTranslation);
            }
            editableCgm.setKeyframes(times, translations, rotations, scales);
        }
    }

    /**
     * Alter the track's end-time keyframe to match the 1st keyframe. If the
     * track doesn't end with a keyframe, append one.
     */
    public void wrap() {
        if (!isTrackSelected()) {
            return;
        }

        Animation newAnimation = newAnimation();
        BoneTrack selectedTrack = findTrack();
        Animation oldAnimation = loadedCgm.animation.getAnimation();
        Track[] oldTracks = oldAnimation.getTracks();
        for (Track track : oldTracks) {
            Track clone;
            if (track == selectedTrack) {
                float endTime = loadedCgm.animation.getDuration();
                clone = Util.wrap(selectedTrack, endTime);
            } else {
                clone = track.clone();
            }
            newAnimation.addTrack(clone);
        }

        editableCgm.replaceAnimation(oldAnimation, newAnimation, "wrap track");
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
    public SelectedTrack clone() throws CloneNotSupportedException {
        SelectedTrack clone = (SelectedTrack) super.clone();
        return clone;
    }
    // *************************************************************************
    // private methods

    /**
     * Create an empty animation with the same name and duration as the selected
     * animation.
     *
     * @return a new instance
     */
    private Animation newAnimation() {
        float duration = loadedCgm.animation.getDuration();
        String name = loadedCgm.animation.getName();
        Animation result = new Animation(name, duration);

        return result;
    }
}
