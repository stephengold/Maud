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
import jme3utilities.MyString;
import jme3utilities.nifty.GuiScreenController;
import jme3utilities.nifty.GuiWindowController;
import maud.Maud;
import maud.MaudUtil;
import maud.model.cgm.Cgm;
import maud.model.cgm.EditableCgm;
import maud.model.cgm.SelectedBone;

/**
 * The controller for the "Bone Tool" window in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class BoneTool extends GuiWindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(BoneTool.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController
     */
    BoneTool(GuiScreenController screenController) {
        super(screenController, "boneTool", false);
    }
    // *************************************************************************
    // GuiWindowController methods

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

        setStatusText("boneChildren", " " + childText);
        setButtonText("boneSelectChild", scButton);
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

        setStatusText("boneHasTrack", " " + hasTrackText);
    }

    /**
     * Update the index status and previous/next buttons.
     */
    private void updateIndex() {
        String indexText;
        String nButton, pButton;

        Cgm target = Maud.getModel().getTarget();
        int numBones = target.getSkeleton().countBones();
        if (target.getBone().isSelected()) {
            int selectedIndex = target.getBone().getIndex();
            indexText = MaudUtil.formatIndex(selectedIndex);
            indexText = String.format("%s of %d", indexText, numBones);
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

        setStatusText("boneIndex", indexText);
        setButtonText("boneNext", nButton);
        setButtonText("bonePrevious", pButton);
    }

    /**
     * Update the influence status.
     */
    private void updateInfluence() {
        String desc = "";

        SelectedBone bone = Maud.getModel().getTarget().getBone();
        if (bone.isSelected()) {
            boolean attachmentsNode = bone.influencesAttachmentsNode();
            boolean meshVertices = bone.influencesVertices();
            if (attachmentsNode) {
                if (meshVertices) {
                    desc = "attachments nodes and mesh vertices";
                } else {
                    desc = "attachments nodes only";
                }
            } else {
                if (meshVertices) {
                    desc = "mesh vertices only";
                } else {
                    desc = "no influence";
                }
            }
        }

        setStatusText("boneInfluence", " " + desc);
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

        setStatusText("boneName", " " + nameText);
        setButtonText("boneRename", rButton);
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
                int numRoots = target.getSkeleton().countRootBones();
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

        setStatusText("boneParent", " " + parentText);
        setButtonText("boneSelectParent", spButton);
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

        setButtonText("boneRotate", rButton);
        setButtonText("boneScale", sButton);
        setButtonText("boneTranslate", tButton);
    }
}
