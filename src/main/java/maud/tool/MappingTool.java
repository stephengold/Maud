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
package maud.tool;

import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.WindowController;
import maud.Maud;
import maud.model.EditorModel;
import maud.model.LoadedMap;
import maud.model.cgm.Cgm;
import maud.model.cgm.SelectedBone;
import maud.model.cgm.SelectedSkeleton;

/**
 * The controller for the "Mapping Tool" window in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class MappingTool extends WindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(MappingTool.class.getName());
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
    // WindowController methods

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
        EditorModel model = Maud.getModel();
        boolean invertFlag = model.getMap().isInvertingMap();
        Maud.gui.setChecked("invertRma2", invertFlag);
        /*
         * the "Show retargeted pose" button
         */
        String mButton;
        if (model.getTarget().getAnimation().isRetargetedPose()
                || !model.getSource().isLoaded()
                || !model.getTarget().getSkeleton().isSelected()) {
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
        String assetPath = Maud.getModel().getMap().getAssetPath();
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
        int editCount = Maud.getModel().getMap().countUnsavedEdits();
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

        LoadedMap map = Maud.getModel().getMap();
        Cgm source = Maud.getModel().getSource();
        Cgm target = Maud.getModel().getTarget();
        if (!target.getSkeleton().isSelected()) {
            feedback = "select the target skeleton";
        } else if (!source.isLoaded()) {
            feedback = "load the source model";
        } else if (!source.getSkeleton().isSelected()) {
            feedback = "select the source skeleton";
        } else if (map.isEmpty()) {
            feedback = "no bone mappings - load map or add";
        } else {
            float matchesSource = map.matchesSource();
            float matchesTarget = map.matchesTarget();
            if (matchesTarget >= 0.9995f) {
                if (matchesSource >= 0.9995f) {
                    feedback = "";
                } else if (matchesSource < 0.0005f) {
                    feedback = "doesn't match the source skeleton";
                } else {
                    feedback = String.format(
                            "%.1f%% matches the source skeleton",
                            100f * matchesSource);
                }

            } else if (matchesSource >= 0.9995f) {
                feedback = String.format(
                        "%.1f%% matches the target skeleton",
                        100f * matchesTarget);
            } else if (matchesSource < 0.0005f && matchesTarget < 0.0005f) {
                feedback = "doesn't match either skeleton";
            } else {
                feedback = String.format(
                        "%.1f%% matches source, %.1f%% matches target",
                        100f * matchesSource, 100f * matchesTarget);
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

        LoadedMap map = Maud.getModel().getMap();
        int numBoneMappings = map.countMappings();
        if (map.isBoneMappingSelected()) {
            int index = map.findIndex();
            int indexBase = Maud.getModel().getMisc().getIndexBase();
            indexText = String.format("#%d of %d", index + indexBase,
                    numBoneMappings);
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

        if (Maud.getModel().getMap().isBoneMappingSelected()) {
            uButton = "Unmap";
        } else if (Maud.getModel().getSource().getBone().isSelected()
                && Maud.getModel().getTarget().getBone().isSelected()) {
            mButton = "Map";
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
        Cgm source = Maud.getModel().getSource();
        String sourceBoneDesc;
        if (source.getBone().isSelected()) {
            String sourceName = source.getBone().getName();
            sourceBoneDesc = MyString.quote(sourceName);
            String target = Maud.getModel().getMap().targetBoneName(sourceName);
            if (target != null) {
                sourceBoneDesc += String.format("  -> %s", target);
            }
        } else if (source.isLoaded()) {
            sourceBoneDesc = SelectedSkeleton.noBone;
        } else {
            sourceBoneDesc = "( no model )";
        }
        Maud.gui.setStatusText("sourceBone", " " + sourceBoneDesc);
        /*
         * select button
         */
        String sButton;
        if (source.isLoaded()) {
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

        SelectedBone bone = Maud.getModel().getTarget().getBone();
        if (bone.isSelected()) {
            String targetName = bone.getName();
            targetBoneDesc = MyString.quote(targetName);
            String source = Maud.getModel().getMap().sourceBoneName(targetName);
            if (source != null) {
                targetBoneDesc += String.format("  <- %s", source);
            }
        } else {
            targetBoneDesc = SelectedSkeleton.noBone;
        }

        Maud.gui.setStatusText("targetBone", " " + targetBoneDesc);
    }
}
