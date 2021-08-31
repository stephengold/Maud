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

import com.jme3.anim.TransformTrack;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Track;
import com.jme3.math.Transform;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.wes.Pose;
import jme3utilities.wes.TrackEdit;
import maud.DescribeUtil;
import maud.MaudUtil;

/**
 * The MVC model of the selected keyframe in a selected track.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SelectedFrame implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SelectedFrame.class.getName());
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
    // *************************************************************************
    // new methods exposed

    /**
     * Find the index of the keyframe (if any) at the current animation time.
     *
     * @return the keyframe's index, or -1 if no keyframe at that time
     */
    public int findIndex() {
        assert cgm.getTrack().isSelected();

        float time = cgm.getPlay().getTime();
        int frameIndex = cgm.getTrack().findKeyframeIndex(time);

        return frameIndex;
    }

    /**
     * Test whether a keyframe is selected.
     *
     * @return true if selected, otherwise false
     */
    public boolean isSelected() {
        boolean result = false;
        if (cgm.getTrack().isSelected()) {
            float time = cgm.getPlay().getTime();
            int frameIndex = cgm.getTrack().findKeyframeIndex(time);
            if (frameIndex != -1) {
                result = true;
            }
        }

        return result;
    }

    /**
     * Alter the keyframe, setting its Transform from the displayed Pose.
     */
    public void replace() {
        SelectedTrack sTrack = cgm.getTrack();
        assert sTrack.isBoneTrack();
        int frameIndex = findIndex();
        assert frameIndex >= 0 : frameIndex;

        Object oldSelected = sTrack.get();
        Object newSelected = null;
        TmpTracks.clear();
        Object[] oldTracks = cgm.getAnimation().getTracks();
        for (Object oldTrack : oldTracks) {
            Object newTrack;
            if (oldTrack == oldSelected) {
                Pose pose = cgm.getPose().get();
                int boneIndex = sTrack.targetBoneIndex();
                if (oldTrack instanceof BoneTrack) {
                    Transform user = pose.userTransform(boneIndex, null);
                    newTrack = TrackEdit.replaceKeyframe((Track) oldTrack,
                            frameIndex, user);
                } else {
                    Transform local = pose.localTransform(boneIndex, null);
                    newTrack = TrackEdit.replaceKeyframe(
                            (TransformTrack) oldTrack, frameIndex, local);
                }
                newSelected = newTrack;
            } else {
                newTrack = TrackEdit.cloneTrack(oldTrack);
            }
            TmpTracks.add(newTrack);
        }

        Object newAnim = cgm.getAnimation().newAnim();
        TmpTracks.addAllToAnim(newAnim);

        float time = cgm.getPlay().getTime();
        String trackName = cgm.getTrack().describe();
        String description = String.format(
                "replace keyframe at t=%f in track %s", time, trackName);
        Object oldAnim = cgm.getAnimation().getReal();
        editableCgm.replace(oldAnim, newAnim, description,
                newSelected);
    }

    /**
     * Select the indexed keyframe in the selected track.
     *
     * @param keyFrameIndex which keyframe (&ge;0)
     */
    public void select(int keyFrameIndex) {
        assert cgm.getTrack().isSelected();

        float time = cgm.getTrack().keyframeTime(keyFrameIndex);
        cgm.getPlay().setTime(time);
    }

    /**
     * Select the first keyframe in the selected track.
     */
    public void selectFirst() {
        cgm.getPlay().setTime(0f);
    }

    /**
     * Select the last keyframe in the selected track.
     */
    public void selectLast() {
        assert cgm.getTrack().isSelected();

        float time = cgm.getTrack().lastKeyframeTime();
        cgm.getPlay().setTime(time);
    }

    /**
     * Select the keyframe nearest to the current animation time.
     */
    public void selectNearest() {
        assert cgm.getTrack().isSelected();

        if (!isSelected()) {
            PlayOptions play = cgm.getPlay();
            float current = play.getTime();
            float next = nextKeyframeTime();
            float toNext = next - current;
            assert toNext >= 0f : toNext;
            float previous = previousKeyframeTime();
            float toPrevious = current - previous;
            assert toPrevious >= 0f : toPrevious;
            if (toPrevious < toNext) {
                play.setTime(previous);
            } else {
                play.setTime(next);
            }
        }
    }

    /**
     * Select the next keyframe in the track.
     */
    public void selectNext() {
        assert cgm.getTrack().isSelected();

        float time = nextKeyframeTime();
        if (time < Float.POSITIVE_INFINITY) {
            cgm.getPlay().setTime(time);
        }
    }

    /**
     * Select the previous keyframe in the track.
     */
    public void selectPrevious() {
        assert cgm.getTrack().isSelected();

        float time = previousKeyframeTime();
        if (time >= 0f) {
            cgm.getPlay().setTime(time);
        }
    }

    /**
     * Alter which C-G model contains the keyframe. (Invoked only during
     * initialization and cloning.)
     *
     * @param newCgm (not null, aliases created)
     */
    void setCgm(Cgm newCgm) {
        assert newCgm != null;
        assert newCgm.getFrame() == this;

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
    public void setTime(float newTime) {
        Validate.positive(newTime, "new time");
        SelectedTrack sTrack = cgm.getTrack();
        assert sTrack.isSelected();

        int frameIndex = findIndex();
        Object newSelected = null;
        Object oldSelected = sTrack.get();
        TmpTracks.clear();
        Object[] oldTracks = cgm.getAnimation().getTracks();
        for (Object oldTrack : oldTracks) {
            Object newTrack;
            if (oldTrack == oldSelected) {
                newTrack = MaudUtil.setFrameTime(oldTrack, frameIndex, newTime);
                assert newTrack != null;
                newSelected = newTrack;
            } else {
                newTrack = TrackEdit.cloneTrack(oldTrack);
            }
            TmpTracks.add(newTrack);
        }
        cgm.getPlay().setTime(newTime);

        Object newAnim = cgm.getAnimation().newAnim();
        TmpTracks.addAllToAnim(newAnim);

        String trackName = sTrack.describe();
        String eventDescription = String.format(
                "adjust the timing of frame%s in track %s",
                DescribeUtil.index(frameIndex), trackName);
        Object oldAnim = cgm.getAnimation().getReal();
        editableCgm.replace(oldAnim, newAnim, eventDescription,
                newSelected);
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
    public SelectedFrame clone() throws CloneNotSupportedException {
        SelectedFrame clone = (SelectedFrame) super.clone();
        return clone;
    }
    // *************************************************************************
    // private methods

    /**
     * Determine the time of the next keyframe in the selected track.
     *
     * @return animation time (&ge;0) or +Infinity if none found
     */
    private float nextKeyframeTime() {
        float result = Float.POSITIVE_INFINITY;
        float time = cgm.getPlay().getTime();
        Object selectedTrack = cgm.getTrack().get();
        float[] times = MaudUtil.getTrackTimes(selectedTrack);
        for (int iFrame = 0; iFrame < times.length; iFrame++) {
            if (times[iFrame] > time) {
                result = times[iFrame];
                break;
            }
        }

        return result;
    }

    /**
     * Determine the time of the previous keyframe in the selected track.
     *
     * @return animation time (&ge;0) or -Infinity if none found
     */
    private float previousKeyframeTime() {
        float result = Float.NEGATIVE_INFINITY;
        float time = cgm.getPlay().getTime();
        Object selectedTrack = cgm.getTrack().get();
        float[] times = MaudUtil.getTrackTimes(selectedTrack);
        for (int iFrame = times.length - 1; iFrame >= 0; iFrame--) {
            if (times[iFrame] < time) {
                result = times[iFrame];
                break;
            }
        }

        return result;
    }
}
