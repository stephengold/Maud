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
import maud.Maud;
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
    // new methods exposed

    /**
     * Count the number of keyframes in the selected bone track.
     *
     * @return count (&ge;0)
     */
    public int countKeyframes() {
        int count;
        BoneTrack track = Maud.model.track.findTrack();
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
        if (track == null) {
            return 0;
        } else {
            Vector3f[] offsets = track.getTranslations();
            count = MyVector3f.countDistinct(offsets);
        }

        return count;
    }

    /**
     * Find the index of the keyframe (if any) in the selected track at the
     * current track time.
     *
     * @return keyframe index, or -1 if no keyframe
     */
    public int findKeyframe() {
        BoneTrack track = Maud.model.track.findTrack();
        float[] times = track.getTimes();

        int frameIndex = -1;
        for (int iFrame = 0; iFrame < times.length; iFrame++) {
            if (Maud.model.animation.getTime() == times[iFrame]) {
                frameIndex = iFrame;
                break;
            }
        }

        return frameIndex;
    }

    /**
     * Find the track for the selected bone in the loaded animation.
     *
     * @return the pre-existing instance, or null if none
     */
    BoneTrack findTrack() {
        if (!Maud.model.bone.isBoneSelected()) {
            return null;
        }
        if (Maud.model.animation.isBindPoseLoaded()) {
            return null;
        }

        Animation anim = Maud.model.animation.getLoadedAnimation();
        int boneIndex = Maud.model.bone.getIndex();
        BoneTrack track = MyAnimation.findTrack(anim, boneIndex);

        return track;
    }

    /**
     * Test whether a bone track is selected.
     *
     * @return true if one is selected, false if none is selected
     */
    public boolean isTrackSelected() {
        if (Maud.model.bone.isBoneSelected()) {
            if (Maud.model.animation.isBindPoseLoaded()) {
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
     * Find the time of the last keyframe in the selected bone track.
     *
     * @return animation time (&ge;0)
     */
    public float lastKeyframeTime() {
        BoneTrack track = Maud.model.track.findTrack();
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
        if (Maud.model.animation.isBindPoseLoaded()) {
            logger.log(Level.INFO, "No animation is selected.");
        } else if (!Maud.model.bone.isBoneSelected()) {
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
        assert Maud.model.bone.hasTrack();

        float duration = Maud.model.animation.getDuration();
        String name = Maud.model.animation.getName();
        Animation newAnimation = new Animation(name, duration);

        Animation loaded = Maud.model.animation.getLoadedAnimation();
        Track selectedTrack = Maud.model.track.findTrack();
        Track[] loadedTracks = loaded.getTracks();
        for (Track track : loadedTracks) {
            Track clone;
            if (track == selectedTrack) {
                BoneTrack boneTrack = (BoneTrack) track;
                clone = Util.reduce(boneTrack, factor);
            } else {
                clone = track.clone();
            }
            newAnimation.addTrack(clone);
        }

        Maud.model.cgm.replaceAnimation(loaded, newAnimation);
    }

    /**
     * Alter all rotations in the selected track to match the displayed pose.
     */
    public void setTrackRotationAll() {
        BoneTrack track = findTrack();
        if (track != null) {
            int boneIndex = Maud.model.bone.getIndex();
            Transform poseTransform = Maud.model.pose.copyTransform(boneIndex,
                    null);
            Quaternion poseRotation = poseTransform.getRotation();

            float[] times = track.getTimes();
            Vector3f[] translations = track.getTranslations();
            Quaternion[] rotations = track.getRotations();
            for (Quaternion rotation : rotations) {
                rotation.set(poseRotation);
            }
            Vector3f[] scales = track.getScales();
            Maud.model.cgm.setKeyframes(times, translations, rotations, scales);
        }
    }

    /**
     * Alter all scales in the selected track to match the displayed pose.
     */
    public void setTrackScaleAll() {
        BoneTrack track = findTrack();
        if (track != null) {
            int boneIndex = Maud.model.bone.getIndex();
            Transform poseTransform = Maud.model.pose.copyTransform(boneIndex,
                    null);
            Vector3f poseScale = poseTransform.getScale();

            float[] times = track.getTimes();
            Vector3f[] translations = track.getTranslations();
            Quaternion[] rotations = track.getRotations();
            Vector3f[] scales = track.getScales();
            if (scales != null) {
                for (Vector3f scale : scales) {
                    scale.set(poseScale);
                }
                Maud.model.cgm.setKeyframes(times, translations, rotations, scales);
            }
        }
    }

    /**
     * Alter all translations in the selected track to match the displayed pose.
     */
    public void setTrackTranslationAll() {
        BoneTrack track = findTrack();
        if (track != null) {
            int boneIndex = Maud.model.bone.getIndex();
            Transform poseTransform = Maud.model.pose.copyTransform(boneIndex,
                    null);
            Vector3f poseTranslation = poseTransform.getTranslation();

            float[] times = track.getTimes();
            Vector3f[] translations = track.getTranslations();
            for (Vector3f translation : translations) {
                translation.set(poseTranslation);
            }
            Quaternion[] rotations = track.getRotations();
            Vector3f[] scales = track.getScales();
            Maud.model.cgm.setKeyframes(times, translations, rotations, scales);
        }
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
    public Object clone() throws CloneNotSupportedException {
        SelectedTrack clone = (SelectedTrack) super.clone();
        return clone;
    }
}
