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
package maud.tool;

import java.util.logging.Logger;
import jme3utilities.nifty.GuiScreenController;
import maud.Maud;
import maud.model.cgm.Cgm;
import maud.model.cgm.EditableCgm;
import maud.model.cgm.SelectedBone;
import maud.model.cgm.SelectedTrack;

/**
 * The controller for the "Track" tool in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class TrackTool extends Tool {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(TrackTool.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized tool.
     *
     * @param screenController the controller of the screen that contains the
     * tool (not null)
     */
    TrackTool(GuiScreenController screenController) {
        super(screenController, "track");
    }
    // *************************************************************************
    // Tool methods

    /**
     * Callback to update this tool prior to rendering. (Invoked once per render
     * pass while the tool is displayed.)
     */
    @Override
    void toolUpdate() {
        updateDescription();
        updateFrames();
        updateTransforms();
    }
    // *************************************************************************
    // private methods

    /**
     * Update the track description.
     */
    private void updateDescription() {
        String descriptionStatus;
        String typeStatus = "";

        Cgm target = Maud.getModel().getTarget();
        SelectedBone bone = target.getBone();
        if (!target.getAnimation().isReal()) {
            descriptionStatus = "(load a real animation)";
        } else if (bone.hasTrack()) {
            String boneName = bone.getName();
            String animName = target.getAnimation().getName();
            descriptionStatus = String.format("%s in %s", boneName, animName);
            typeStatus = "bone";
        } else if (bone.isSelected()) {
            String boneName = bone.getName();
            descriptionStatus = String.format("none for %s", boneName);
        } else {
            descriptionStatus = "(select a bone)";
        }

        setStatusText("trackDescription", " " + descriptionStatus);
        setStatusText("trackType", typeStatus);
    }

    /**
     * Update the frame count.
     */
    private void updateFrames() {
        String framesStatus;

        EditableCgm target = Maud.getModel().getTarget();
        int numKeyframes = target.getTrack().countKeyframes();
        if (numKeyframes == 0) {
            if (target.getBone().hasTrack()) {
                framesStatus = "no keyframes";
            } else {
                framesStatus = "no track";
            }
        } else if (numKeyframes == 1) {
            framesStatus = "one keyframe";
        } else {
            framesStatus = String.format("%d keyframes", numKeyframes);
        }

        setStatusText("trackFrames", " " + framesStatus);
    }

    /**
     * Update transform information.
     */
    private void updateTransforms() {
        String translationStatus = "";
        String rotationStatus = "";
        String scaleStatus = "";

        String translationButton = "";
        String rotationButton = "";
        String scaleButton = "";

        Cgm target = Maud.getModel().getTarget();
        if (target.getBone().hasTrack()) {
            SelectedTrack track = target.getTrack();
            int numTranslations = track.countTranslations();
            translationStatus = Integer.toString(numTranslations);
            if (numTranslations > 0) {
                translationButton = "Set all to pose";
            }

            int numRotations = track.countRotations();
            rotationStatus = Integer.toString(numRotations);
            if (numRotations > 0) {
                rotationButton = "Set all to pose";
            }

            int numScales = track.countScales();
            scaleStatus = Integer.toString(numScales);
            if (numScales > 0) {
                scaleButton = "Set all to pose";
            }
        }

        setButtonText("translationsToPose", translationButton);
        setButtonText("rotationsToPose", rotationButton);
        setButtonText("scalesToPose", scaleButton);

        setStatusText("trackTranslationCount", translationStatus);
        setStatusText("trackRotationCount", rotationStatus);
        setStatusText("trackScaleCount", scaleStatus);
    }
}
