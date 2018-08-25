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
import maud.DescribeUtil;
import maud.Maud;
import maud.model.cgm.Cgm;
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
     * @param screenController the controller of the screen that will contain
     * the tool (not null)
     */
    TrackTool(GuiScreenController screenController) {
        super(screenController, "track");
    }
    // *************************************************************************
    // Tool methods

    /**
     * Callback to update this tool prior to rendering. (Invoked once per render
     * pass while this tool is displayed.)
     */
    @Override
    protected void toolUpdate() {
        boolean isReal = Maud.getModel().getTarget().getAnimation().isReal();
        String selectButton = isReal ? "Select track" : "";
        setButtonText("selectTrack", selectButton);

        updateDescription();
        updateFrames();
        updateIndex();
        updateTransforms();
    }
    // *************************************************************************
    // private methods

    /**
     * Update the track's description.
     */
    private void updateDescription() {
        SelectedTrack track = Maud.getModel().getTarget().getTrack();

        String targetStatus = track.describeTarget();
        setStatusText("trackTarget", " " + targetStatus);

        String typeStatus = track.describeType();
        setStatusText("trackType", typeStatus);

        String selectButton = "";
        if (typeStatus.equals("Bone")) {
            selectButton = "Select bone";
        } else if (typeStatus.equals("Spatial")) {
            selectButton = "Select spatial";
        }
        setButtonText("selectTrackTarget", selectButton);
    }

    /**
     * Update the frame count.
     */
    private void updateFrames() {
        String framesStatus = "";

        SelectedTrack track = Maud.getModel().getTarget().getTrack();
        if (track.isSelected()) {
            int numKeyframes = track.countKeyframes();
            framesStatus = Integer.toString(numKeyframes);
        }

        setStatusText("trackFrames", " " + framesStatus);
    }

    /**
     * Update the index status and previous/next buttons.
     */
    private void updateIndex() {
        String indexStatus;
        String nextButton, previousButton;

        Cgm target = Maud.getModel().getTarget();
        int numTracks = target.getAnimation().countTracks();
        if (target.getTrack().isSelected()) {
            int selectedIndex = target.getTrack().index();
            indexStatus = DescribeUtil.index(selectedIndex, numTracks);
            nextButton = "+";
            previousButton = "-";
        } else {
            if (numTracks == 0) {
                indexStatus = "no tracks";
            } else if (numTracks == 1) {
                indexStatus = "one track";
            } else {
                indexStatus = String.format("%d tracks", numTracks);
            }
            nextButton = "";
            previousButton = "";
        }

        setStatusText("trackIndex", indexStatus);
        setButtonText("trackNext", nextButton);
        setButtonText("trackPrevious", previousButton);
    }

    /**
     * Update transform information.
     */
    private void updateTransforms() {
        String translationStatus = "";
        String setTranslationsButton = "";
        String deleteTranslationsButton = "";

        String rotationStatus = "";
        String setRotationsButton = "";
        String deleteRotationsButton = "";

        String scaleStatus = "";
        String setScalesButton = "";
        String deleteScalesButton = "";

        SelectedTrack track = Maud.getModel().getTarget().getTrack();
        if (track.isSelected()) {
            int numTranslations = track.countTranslations();
            translationStatus = Integer.toString(numTranslations);

            int numRotations = track.countRotations();
            rotationStatus = Integer.toString(numRotations);

            int numScales = track.countScales();
            scaleStatus = Integer.toString(numScales);

            if (track.isBoneTrack()) {
                if (numTranslations > 0) {
                    setTranslationsButton = "Set all to pose";
                }
                if (numRotations > 0) {
                    setRotationsButton = "Set all to pose";
                }
                if (numScales > 0) {
                    setScalesButton = "Set all to pose";
                }
            } else {
                if (numTranslations > 0) {
                    deleteTranslationsButton = "Delete";
                }
                if (numRotations > 0) {
                    deleteRotationsButton = "Delete";
                }
            }
            if (numScales > 0) {
                deleteScalesButton = "Delete";
            }
        }

        setStatusText("trackTranslationCount", translationStatus);
        setButtonText("translationsToPose", setTranslationsButton);
        setButtonText("deleteTrackTranslations", deleteTranslationsButton);

        setStatusText("trackRotationCount", rotationStatus);
        setButtonText("rotationsToPose", setRotationsButton);
        setButtonText("deleteTrackRotations", deleteRotationsButton);

        setStatusText("trackScaleCount", scaleStatus);
        setButtonText("scalesToPose", setScalesButton);
        setButtonText("deleteTrackScales", deleteScalesButton);
    }
}
