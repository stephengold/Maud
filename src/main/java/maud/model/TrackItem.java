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
import com.jme3.animation.SpatialTrack;
import com.jme3.animation.Track;
import com.jme3.scene.Spatial;
import java.util.logging.Logger;
import jme3utilities.MyAnimation;
import jme3utilities.Validate;

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
     * anim control that contains the track
     */
    final private AnimControl animControl;
    /**
     * name of the animation that contains the track
     */
    final private String animationName;
    /**
     * constructed name of the anim control that contains the track
     */
    final private String animControlName;
    /**
     * the track itself
     */
    final private Track track;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a new instance.
     */
    TrackItem(String animationName, String animControlName,
            AnimControl animControl, Track track) {
        assert animControlName != null;
        assert animControl != null;
        assert animationName != null;
        assert track != null;

        this.animationName = animationName;
        this.animControlName = animControlName;
        this.animControl = animControl;
        this.track = track;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Access the animation.
     *
     * @return the pre-existing instance (not null)
     */
    Animation getAnimation() {
        Animation result = animControl.getAnim(animationName);
        assert result != null;
        return result;
    }

    /**
     * Access the name of the animation.
     *
     * @return text string (not null)
     */
    public String getAnimationName() {
        assert animationName != null;
        return animationName;
    }

    /**
     * Access the anim control.
     *
     * @return the pre-existing instance
     */
    AnimControl getAnimControl() {
        assert animControl != null;
        return animControl;
    }

    /**
     * Access the name of the anim control.
     *
     * @return text string (not null)
     */
    public String getAnimControlName() {
        assert animControlName != null;
        return animControlName;
    }

    /**
     * Access the track itself.
     *
     * @return the pre-existing instance (not null)
     */
    Track getTrack() {
        assert track != null;
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
        Track otherTrack = otherItem.getTrack();
        if (track instanceof BoneTrack && otherTrack instanceof BoneTrack) {
            BoneTrack boneTrack = (BoneTrack) track;
            int boneIndex = boneTrack.getTargetBoneIndex();
            BoneTrack otherBoneTrack = (BoneTrack) otherTrack;
            int otherBoneIndex = otherBoneTrack.getTargetBoneIndex();

            if (boneIndex == otherBoneIndex) {
                result = true;
            } else {
                result = false;
            }

        } else if (track instanceof SpatialTrack
                && otherTrack instanceof SpatialTrack) {
            //SpatialTrack spatialTrack = (SpatialTrack) track;
            Spatial //spatial = spatialTrack.getTrackSpatial(); //TODO JME 3.2
                    //if (spatial == null) {
                    spatial = animControl.getSpatial();
            //}

            AnimControl otherAnimControl = otherItem.getAnimControl();
            //SpatialTrack otherSpatialTrack = (SpatialTrack) otherTrack;
            Spatial //otherSpatial = otherSpatialTrack.getTrackSpatial(); //TODO JME 3.2
                    //if (otherSpatial == null) {
                    otherSpatial = otherAnimControl.getSpatial();
            //}

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
     * @return descriptive string of text (not null)
     */
    @Override
    public String toString() {
        String desc = MyAnimation.describe(track, animControl);
        String result = String.format("%s/%s/%s", animControlName,
                animationName, desc);

        return result;
    }

}
