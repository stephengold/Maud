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

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.WindowController;
import maud.Maud;
import maud.model.LoadedCGModel;
import maud.model.LoadedMapping;

/**
 * The controller for the "Bone Tool" window in Maud's "3D View" screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class BoneTool extends WindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            BoneTool.class.getName());
    // *************************************************************************
    // fields

    /**
     * smallest squared distance from the mouse pointer (&ge;0)
     */
    private float bestDSquared;
    /**
     * index of the axis whose tip is closest to the mouse pointer, or -1
     */
    private int bestAxisIndex;
    /**
     * index of the bone closest to the mouse pointer, or -1
     */
    private int bestBoneIndex;
    /**
     * CG model containing the feature closest to the mouse pointer, or null
     */
    private LoadedCGModel bestCgm;
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
     * Select the bone or axis tip whose screen coordinates are nearest to the
     * mouse pointer.
     */
    public void selectXY() {
        bestAxisIndex = -1;
        bestBoneIndex = -1;
        bestCgm = null;
        bestDSquared = Float.MAX_VALUE;

        if (Maud.model.source.isLoaded()) {
            selectBestInCgm(Maud.model.source);
        }
        selectBestInCgm(Maud.model.target);

        if (bestCgm != null) {
            if (bestAxisIndex >= 0) {
                boolean farSide = Maud.gui.tools.axes.isAxisReceding(bestCgm,
                        bestAxisIndex);
                Maud.model.axes.setDraggingAxis(bestAxisIndex, bestCgm,
                        farSide);
            }
            if (bestBoneIndex >= 0) {
                bestCgm.bone.select(bestBoneIndex);

                if (Maud.model.target.animation.isRetargetedPose()) {
                    /*
                     * Also select the mapped bone (if any).
                     */
                    LoadedMapping mapping = Maud.model.mapping;
                    if (bestCgm == Maud.model.source
                            && mapping.isSourceBoneMapped(bestBoneIndex)) {
                        Maud.model.mapping.selectFromSource();
                    } else if (bestCgm == Maud.model.target
                            && mapping.isTargetBoneMapped(bestBoneIndex)) {
                        Maud.model.mapping.selectFromTarget();
                    }
                }
            }
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
        if (Maud.model.target.bone.isSelected()) {
            if (Maud.model.target.animation.isRetargetedPose()) {
                String name = Maud.model.target.bone.getName();
                if (Maud.model.mapping.isBoneMapped(name)) {
                    hasTrackText = "mapped";
                } else {
                    hasTrackText = "unmapped";
                }
            } else if (Maud.model.target.bone.hasTrack()) {
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
     * Calculate the squared distance between the mouse pointer and the indexed
     * bone.
     *
     * @param cgm which CG model contains the bone (not null, unaffected)
     * @param boneIndex which bone in the CGM's selected skeleton (&ge;0)
     * @return squared distance in pixels (&ge;0)
     */
    private float boneDSquared(LoadedCGModel cgm, int boneIndex) {
        assert boneIndex >= 0 : boneIndex;

        Vector3f boneWorld = cgm.view.boneLocation(boneIndex);
        Vector3f boneScreen = cam.getScreenCoordinates(boneWorld);
        Vector2f boneXY = new Vector2f(boneScreen.x, boneScreen.y);
        Vector2f mouseXY = inputManager.getCursorPosition();
        float dSquared = mouseXY.distanceSquared(boneXY);

        return dSquared;
    }

    /**
     * Find the bone or axis tip in the specified CG model whose screen
     * coordinates are nearest to the mouse pointer. Assumes bestDSquared has
     * been initialized.
     *
     * @param cgm which CG model (not null)
     */
    private void selectBestInCgm(LoadedCGModel cgm) {
        int numBones = cgm.bones.countBones();
        for (int boneIndex = 0; boneIndex < numBones; boneIndex++) {
            float dSquared = boneDSquared(cgm, boneIndex);
            if (dSquared < bestDSquared) {
                bestDSquared = dSquared;
                bestAxisIndex = -1;
                bestBoneIndex = boneIndex;
                bestCgm = cgm;
            }
        }

        for (int axisIndex = 0; axisIndex < 3; axisIndex++) {
            float dSquared = tipDSquared(cgm, axisIndex);
            if (dSquared < bestDSquared) {
                bestDSquared = dSquared;
                bestAxisIndex = axisIndex;
                bestBoneIndex = -1;
                bestCgm = cgm;
            }
        }
    }

    /**
     * Calculate the squared distance between the mouse pointer and the indexed
     * axis tip.
     *
     * @param cgm which CG model (not null, unaffected)
     * @param axisIndex which axis in the CGM's axes control (&ge;0, &lt;3)
     * @return squared distance in pixels (&ge;0)
     */
    private float tipDSquared(LoadedCGModel cgm, int axisIndex) {
        assert cgm != null;
        assert axisIndex >= 0 : axisIndex;
        assert axisIndex < 3 : axisIndex;

        float dSquared = Float.MAX_VALUE;
        Vector3f tipWorld = Maud.gui.tools.axes.tipLocation(cgm, axisIndex);
        if (tipWorld != null) {
            Vector3f tipScreen = cam.getScreenCoordinates(tipWorld);
            Vector2f tipXY = new Vector2f(tipScreen.x, tipScreen.y);
            Vector2f mouseXY = inputManager.getCursorPosition();
            dSquared = mouseXY.distanceSquared(tipXY);
        }

        return dSquared;
    }

    /**
     * Update the children status and button.
     */
    private void updateChildren() {
        String childText, scButton;

        if (Maud.model.target.bone.isSelected()) {
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
        if (Maud.model.target.bone.isSelected()) {
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

        if (Maud.model.target.bone.isSelected()) {
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

        if (Maud.model.target.bone.isSelected()) {
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
