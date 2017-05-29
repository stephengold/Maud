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
package maud;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.WindowController;
import maud.model.LoadedCGModel;

/**
 * The controller for the "Bone Tool" window in Maud's "3D View" screen.
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
    // new methods exposed

    /**
     * Select the bone with screen coordinates nearest to the mouse pointer.
     */
    void selectXY() {
        float bestDSquared = Float.MAX_VALUE;
        int bestBoneIndex = -1;
        LoadedCGModel bestCgm = null;

        if (Maud.model.source.isLoaded()) {
            int numBones = Maud.model.source.bones.countBones();
            for (int boneIndex = 0; boneIndex < numBones; boneIndex++) {
                float dSquared = distanceSquared(Maud.model.source, boneIndex);
                if (dSquared < bestDSquared) {
                    bestDSquared = dSquared;
                    bestBoneIndex = boneIndex;
                    bestCgm = Maud.model.source;
                }
            }
        }

        int numBones = Maud.model.target.bones.countBones();
        for (int boneIndex = 0; boneIndex < numBones; boneIndex++) {
            float dSquared = distanceSquared(Maud.model.target, boneIndex);
            if (dSquared < bestDSquared) {
                bestDSquared = dSquared;
                bestBoneIndex = boneIndex;
                bestCgm = Maud.model.target;
            }
        }

        if (bestCgm != null) {
            bestCgm.bone.select(bestBoneIndex);
        }
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

        String hasTrackText, rButton, sButton, tButton;
        if (Maud.model.target.bone.isBoneSelected()) {
            if (Maud.model.target.bone.hasTrack()) {
                hasTrackText = "has track";
            } else {
                hasTrackText = "no track";
            }
            rButton = "Rotate";
            sButton = "Scale";
            tButton = "Translate";
        } else {
            hasTrackText = "";
            rButton = "";
            sButton = "";
            tButton = "";
        }
        Maud.gui.setStatusText("boneHasTrack", " " + hasTrackText);
        Maud.gui.setButtonLabel("boneRotateButton", rButton);
        Maud.gui.setButtonLabel("boneScaleButton", sButton);
        Maud.gui.setButtonLabel("boneTranslateButton", tButton);

        updateChildren();
        updateIndex();
        updateName();
        updateParent();
    }
    // *************************************************************************
    // private methods

    /**
     * Calculate the squared distance between the mouse pointer and the
     * specified bone.
     *
     * @param loadedCgm which CG model (not null)
     * @param boneIndex (&ge;0)
     * @return squared distance in screen units (&ge;0)
     */
    private float distanceSquared(LoadedCGModel loadedCgm, int boneIndex) {
        Vector3f boneWorld = loadedCgm.view.boneLocation(boneIndex);
        Vector3f boneScreen = cam.getScreenCoordinates(boneWorld);
        Vector2f boneXY = new Vector2f(boneScreen.x, boneScreen.y);
        Vector2f mouseXY = inputManager.getCursorPosition();
        float dSquared = mouseXY.distanceSquared(boneXY);

        return dSquared;
    }

    /**
     * Update the children status and button.
     */
    private void updateChildren() {
        String childText, scButton;

        if (Maud.model.target.bone.isBoneSelected()) {
            int numChildren = Maud.model.target.bone.countChildren();
            if (numChildren > 1) {
                childText = String.format("%d children", numChildren);
                scButton = "Select";
            } else if (numChildren == 1) {
                String childName = Maud.model.target.bone.getChildName(0);
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
     * Update the index status and previous/next buttons.
     */
    private void updateIndex() {
        String indexText;
        String nButton, pButton;

        int numBones = Maud.model.target.bones.countBones();
        if (Maud.model.target.bone.isBoneSelected()) {
            int selectedIndex = Maud.model.target.bone.getIndex();
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
     * Update the name status and rename button.
     */
    private void updateName() {
        String nameText, rButton;

        if (Maud.model.target.bone.isBoneSelected()) {
            String name = Maud.model.target.bone.getName();
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

        if (Maud.model.target.bone.isBoneSelected()) {
            if (Maud.model.target.bone.isRootBone()) {
                int numRoots = Maud.model.target.bones.countRootBones();
                if (numRoots == 1) {
                    parentText = "none (the root)";
                } else {
                    parentText = String.format("none (one of %d roots)",
                            numRoots);
                }
                spButton = "";
            } else {
                String parentName = Maud.model.target.bone.getParentName();
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
}
