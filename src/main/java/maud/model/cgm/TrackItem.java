/*
 Copyright (c) 2017-2021, Stephen Gold
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
import com.jme3.anim.TransformTrack;
import com.jme3.anim.util.HasLocalTransform;
import com.jme3.animation.AnimControl;
import com.jme3.animation.Animation;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.SpatialTrack;
import com.jme3.animation.Track;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import java.util.logging.Logger;
import jme3utilities.MyAnimation;
import jme3utilities.Validate;
import maud.MaudUtil;

/**
 * Useful information about a particular animation track.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TrackItem {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(TrackItem.class.getName());
    // *************************************************************************
    // fields

    /**
     * animation control that contains the track
     */
    final private AbstractControl animControl;
    /**
     * the track itself
     */
    final private Object track;
    /**
     * name of the animation that contains the track
     */
    final private String animationName;
    /**
     * constructed name of the anim control that contains the track
     */
    final private String animControlName;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a new item.
     */
    TrackItem(String animationName, String animControlName,
            AbstractControl control, Object track) {
        assert animControlName != null;
        assert control instanceof AnimComposer
                || control instanceof AnimControl;
        assert animationName != null;
        assert track instanceof AnimTrack || track instanceof Track;

        this.animationName = animationName;
        this.animControlName = animControlName;
        this.animControl = control;
        this.track = track;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Determine the duration of the animation.
     *
     * @return the pre-existing instance (not null)
     */
    double animationDuration() {
        double result;
        if (animControl instanceof AnimControl) {
            Animation animation
                    = ((AnimControl) animControl).getAnim(animationName);
            result = animation.getLength();
        } else {
            AnimClip clip
                    = ((AnimComposer) animControl).getAnimClip(animationName);
            result = clip.getLength();
        }

        return result;
    }

    /**
     * Describe the track within the context of its animation.
     *
     * @return a descriptive string of text (not null, not empty)
     */
    public String describe() {
        String description;
        if (animControl instanceof AnimControl) {
            description = MyAnimation.describe((Track) track,
                    (AnimControl) animControl);
            assert !description.isEmpty();
        } else {
            description = MaudUtil.describe((AnimTrack) track);
            assert !description.isEmpty();
        }

        return description;
    }

    /**
     * Access the anim control.
     *
     * @return the pre-existing instance (not null)
     */
    AbstractControl getAnimControl() {
        return animControl;
    }

    /**
     * Access the track itself.
     *
     * @return the pre-existing instance (not null)
     */
    Object getTrack() {
        return track;
    }

    /**
     * Test whether two tracks share the same target.
     *
     * @param otherItem item to compare (not null, unaffected)
     * @return true if same target, otherwise false
     */
    public boolean hasSameTargetAs(TrackItem otherItem) {
        Validate.nonNull(otherItem, "other item");

        boolean result;
        Object otherTrack = otherItem.getTrack();
        if (track instanceof BoneTrack && otherTrack instanceof BoneTrack) {
            int boneIndex = ((BoneTrack) track).getTargetBoneIndex();
            int otherBoneIndex = ((BoneTrack) otherTrack).getTargetBoneIndex();

            if (boneIndex == otherBoneIndex) {
                result = true;
            } else {
                result = false;
            }

        } else if (track instanceof TransformTrack
                && otherTrack instanceof TransformTrack) {
            HasLocalTransform target = ((TransformTrack) track).getTarget();
            HasLocalTransform otherTarget
                    = ((TransformTrack) otherTrack).getTarget();

            if (target == otherTarget) {
                result = true;
            } else {
                result = false;
            }

        } else if (track instanceof SpatialTrack
                && otherTrack instanceof SpatialTrack) {
            Spatial spatial = ((SpatialTrack) track).getTrackSpatial();
            if (spatial == null) {
                spatial = animControl.getSpatial();
            }

            AnimControl otherAnimControl
                    = (AnimControl) otherItem.getAnimControl();
            Spatial otherSpatial
                    = ((SpatialTrack) otherTrack).getTrackSpatial();
            if (otherSpatial == null) {
                otherSpatial = otherAnimControl.getSpatial();
            }

            if (spatial == otherSpatial) {
                result = true;
            } else {
                result = false;
            }

        } else {
            result = false; // TODO other track types
        }

        return result;
    }
    // *************************************************************************
    // Object methods

    /**
     * Represent the track as a text string.
     *
     * @return a descriptive string of text (not null, not empty)
     */
    @Override
    public String toString() {
        String description = describe();
        String result = String.format("%s/%s/%s", animControlName,
                animationName, description);

        return result;
    }
}
