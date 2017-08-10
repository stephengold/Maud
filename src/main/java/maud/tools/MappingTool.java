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
package maud.tools;

import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.WindowController;
import maud.Maud;
import maud.model.SelectedSkeleton;

/**
 * The controller for the "Mapping Tool" window in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class MappingTool extends WindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            MappingTool.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController
     */
    MappingTool(BasicScreenController screenController) {
        super(screenController, "mappingTool", false);
    }
    // *************************************************************************
    // AppState methods

    /**
     * Callback to update this window prior to rendering. (Invoked once per
     * render pass.)
     *
     * @param elapsedTime time interval between render passes (in seconds,
     * &ge;0)
     */
    @Override
    public void update(float elapsedTime) {
        super.update(elapsedTime);

        updateAsset();
        updateFeedback();
        updateIndex();
        updateSelected();
        updateSource();
        updateTarget();
        /*
         * the "use inverse" checkbox
         */
        boolean invertFlag = Maud.model.getMap().isInvertingMap();
        Maud.gui.setChecked("invertRma2", invertFlag);
        /*
         * the "retargeted pose" button
         */
        String mButton;
        if (Maud.model.target.animation.isRetargetedPose()
                || !Maud.model.getSource().isLoaded()) {
            mButton = "";
        } else {
            mButton = "Show retargeted pose";
        }
        Maud.gui.setButtonLabel("loadRetargetedPose", mButton);
    }
    // *************************************************************************
    // private methods

    /**
     * Update the asset status.
     */
    private void updateAsset() {
        /*
         * asset-path status
         */
        String assetPath = Maud.model.getMap().getAssetPath();
        String assetDesc;
        if (assetPath.isEmpty()) {
            assetDesc = "unknown";
        } else {
            assetDesc = MyString.quote(assetPath);
        }
        Maud.gui.setStatusText("mapAssetPath", " " + assetDesc);
        /*
         * pristine/edited status
         */
        String pristineDesc;
        int editCount = Maud.model.getMap().countUnsavedEdits();
        if (editCount == 0) {
            pristineDesc = "pristine";
        } else if (editCount == 1) {
            pristineDesc = "one edit";
        } else {
            pristineDesc = String.format("%d edits", editCount);
        }
        Maud.gui.setStatusText("mapPristine", pristineDesc);
    }

    /**
     * Update the feedback line.
     */
    private void updateFeedback() {
        String feedback;
        boolean sourceIsLoaded = Maud.model.getSource().isLoaded();
        if (Maud.model.getMap().matchesTarget()) {
            if (sourceIsLoaded) {
                if (Maud.model.getMap().matchesSource()) {
                    feedback = "";
                } else {
                    feedback = "doesn't match the source skeleton";
                }
            } else {
                feedback = "load the source model";
            }
        } else {
            if (!sourceIsLoaded || Maud.model.getMap().matchesSource()) {
                feedback = "doesn't match the target skeleton";
            } else {
                feedback = "doesn't match either skeleton";
            }
        }
        Maud.gui.setStatusText("mappingFeedback", feedback);
    }

    /**
     * Update the index status and previous/next buttons.
     */
    private void updateIndex() {
        String indexText;
        String nButton, pButton;

        int numBoneMappings = Maud.model.getMap().countMappings();
        if (Maud.model.getMap().isBoneMappingSelected()) {
            int index = Maud.model.getMap().findIndex();
            indexText = String.format("#%d of %d", index + 1, numBoneMappings);
            nButton = "+";
            pButton = "-";

        } else {
            if (numBoneMappings == 0) {
                indexText = "no mappings";
            } else if (numBoneMappings == 1) {
                indexText = "one mapping";
            } else {
                indexText = String.format("%d mappings", numBoneMappings);
            }
            nButton = "";
            pButton = "";
        }

        Maud.gui.setStatusText("mappingIndex", indexText);
        Maud.gui.setButtonLabel("mappingNextButton", nButton);
        Maud.gui.setButtonLabel("mappingPreviousButton", pButton);
    }

    /**
     * Update map/unmap buttons.
     */
    private void updateSelected() {
        String mButton = "";
        String uButton = "";
        if (Maud.model.getMap().isBoneMappingSelected()) {
            uButton = "Unmap";
        } else {
            if (Maud.model.getSource().bone.isSelected()
                    && Maud.model.target.bone.isSelected()) {
                mButton = "Map";
            }
        }
        Maud.gui.setButtonLabel("addMappingButton", mButton);
        Maud.gui.setButtonLabel("deleteMappingButton", uButton);
    }

    /**
     * Update the source-bone description and select button.
     */
    private void updateSource() {
        /*
         * description
         */
        String sourceBoneDesc;
        if (Maud.model.getSource().bone.isSelected()) {
            String sourceName = Maud.model.getSource().bone.getName();
            sourceBoneDesc = MyString.quote(sourceName);
            String target = Maud.model.getMap().targetBoneName(sourceName);
            if (target != null) {
                sourceBoneDesc += String.format("  -> %s", target);
            }
        } else if (Maud.model.getSource().isLoaded()) {
            sourceBoneDesc = SelectedSkeleton.noBone;
        } else {
            sourceBoneDesc = "( no model )";
        }
        Maud.gui.setStatusText("sourceBone", " " + sourceBoneDesc);
        /*
         * select button
         */
        String sButton;
        if (Maud.model.getSource().isLoaded()) {
            sButton = "Select";
        } else {
            sButton = "";
        }
        Maud.gui.setButtonLabel("selectSourceBone", sButton);
    }

    /**
     * Update the target-bone description.
     */
    private void updateTarget() {
        String targetBoneDesc;
        if (Maud.model.target.bone.isSelected()) {
            String targetName = Maud.model.target.bone.getName();
            targetBoneDesc = MyString.quote(targetName);
            String source = Maud.model.getMap().sourceBoneName(targetName);
            if (source != null) {
                targetBoneDesc += String.format("  <- %s", source);
            }
        } else {
            targetBoneDesc = SelectedSkeleton.noBone;
        }
        Maud.gui.setStatusText("targetBone", " " + targetBoneDesc);
    }
}
