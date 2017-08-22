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
import maud.model.EditableCgm;
import maud.model.LoadedCgm;
import maud.model.SelectedBone;

/**
 * The controller for the "Bone Tool" window in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class BoneTool extends WindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            BoneTool.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController
     */
    BoneTool(BasicScreenController screenController) {
        super(screenController, "boneTool", false);
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

        updateChildren();
        updateHasTrack();
        updateIndex();
        updateInfluence();
        updateName();
        updateParent();
        updateTransformButtons();
    }
    // *************************************************************************
    // private methods

    /**
     * Update the children status and button.
     */
    private void updateChildren() {
        String childText, scButton;

        SelectedBone selectedBone = Maud.getModel().getTarget().getBone();
        if (selectedBone.isSelected()) {
            int numChildren = selectedBone.countChildren();
            if (numChildren > 1) {
                childText = String.format("%d children", numChildren);
                scButton = "Select";
            } else if (numChildren == 1) {
                String childName = selectedBone.getChildName(0);
                childText = MyString.quote(childName);
                scButton = "Select";
            } else {
                childText = "none";
                scButton = "";
            }

        } else {
            childText = "n/a";
            scButton = "";
        }

        Maud.gui.setStatusText("boneChildren", " " + childText);
        Maud.gui.setButtonLabel("boneSelectChildButton", scButton);
    }

    /**
     * Update the "has track" status.
     */
    private void updateHasTrack() {
        String hasTrackText = "";

        SelectedBone selectedBone = Maud.getModel().getTarget().getBone();
        if (selectedBone.isSelected()) {
            if (Maud.getModel().getTarget().getAnimation().isRetargetedPose()) {
                String name = selectedBone.getName();
                if (Maud.getModel().getMap().isBoneMapped(name)) {
                    hasTrackText = "mapped";
                } else {
                    hasTrackText = "unmapped";
                }
            } else if (selectedBone.hasTrack()) {
                hasTrackText = "has track";
            } else {
                hasTrackText = "no track";
            }
        }

        Maud.gui.setStatusText("boneHasTrack", " " + hasTrackText);
    }

    /**
     * Update the index status and previous/next buttons.
     */
    private void updateIndex() {
        String indexText;
        String nButton, pButton;

        LoadedCgm target = Maud.getModel().getTarget();
        int numBones = target.bones.countBones();
        if (target.getBone().isSelected()) {
            int selectedIndex = target.getBone().getIndex();
            indexText = String.format("#%d of %d", selectedIndex + 1, numBones);
            nButton = "+";
            pButton = "-";

        } else {
            if (numBones == 0) {
                indexText = "no bones";
            } else if (numBones == 1) {
                indexText = "one bone";
            } else {
                indexText = String.format("%d bones", numBones);
            }
            nButton = "";
            pButton = "";
        }

        Maud.gui.setStatusText("boneIndex", indexText);
        Maud.gui.setButtonLabel("boneNextButton", nButton);
        Maud.gui.setButtonLabel("bonePreviousButton", pButton);
    }

    /**
     * Update the influence status.
     */
    private void updateInfluence() {
        String desc = "";

        SelectedBone bone = Maud.getModel().getTarget().getBone();
        if (bone.isSelected()) {
            int influence = bone.influence();
            desc = String.format("influences %d vertices", influence);
        }
        Maud.gui.setStatusText("boneInfluence", desc);
    }

    /**
     * Update the name status and rename button.
     */
    private void updateName() {
        String nameText, rButton;

        SelectedBone bone = Maud.getModel().getTarget().getBone();
        if (bone.isSelected()) {
            String name = bone.getName();
            nameText = MyString.quote(name);
            rButton = "Rename";

        } else {
            nameText = "(none selected)";
            rButton = "";
        }

        Maud.gui.setStatusText("boneName", " " + nameText);
        Maud.gui.setButtonLabel("boneRenameButton", rButton);
    }

    /**
     * Update the parent status and button.
     */
    private void updateParent() {
        String parentText, spButton;

        EditableCgm target = Maud.getModel().getTarget();
        SelectedBone selectedBone = target.getBone();
        if (selectedBone.isSelected()) {
            if (selectedBone.isRootBone()) {
                int numRoots = target.bones.countRootBones();
                if (numRoots == 1) {
                    parentText = "none (the root)";
                } else {
                    parentText = String.format("none (one of %d roots)",
                            numRoots);
                }
                spButton = "";
            } else {
                String parentName = selectedBone.getParentName();
                parentText = MyString.quote(parentName);
                spButton = "Select";
            }

        } else {
            parentText = "n/a";
            spButton = "";
        }

        Maud.gui.setStatusText("boneParent", " " + parentText);
        Maud.gui.setButtonLabel("boneSelectParentButton", spButton);
    }

    /**
     * Update the transform buttons.
     */
    private void updateTransformButtons() {
        String rButton, sButton, tButton;

        if (Maud.getModel().getTarget().getBone().isSelected()) {
            rButton = "Rotate";
            sButton = "Scale";
            tButton = "Translate";
        } else {
            rButton = "";
            sButton = "";
            tButton = "";
        }

        Maud.gui.setButtonLabel("boneRotateButton", rButton);
        Maud.gui.setButtonLabel("boneScaleButton", sButton);
        Maud.gui.setButtonLabel("boneTranslateButton", tButton);
    }
}
