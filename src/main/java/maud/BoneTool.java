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

import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.WindowController;

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
     * Update this window and others after a change.
     */
    public void update() {
        String indexText, nameText;
        String parentText, childText;
        String rButton, spButton, scButton;

        int numBones = Maud.model.cgm.countBones();
        if (Maud.model.bone.isBoneSelected()) {
            int selectedIndex = Maud.model.bone.getIndex();
            indexText = String.format("#%d of %d", selectedIndex + 1, numBones);

            String name = Maud.model.bone.getName();
            nameText = MyString.quote(name);

            if (Maud.model.bone.isRootBone()) {
                List<String> roots = Maud.model.cgm.listRootBoneNames();
                int numRoots = roots.size();
                if (numRoots == 1) {
                    parentText = "none (the root bone)";
                } else {
                    parentText = String.format(
                            "none (one of %d root bones)", numRoots);
                }
                spButton = "";
            } else {
                String parentName = Maud.model.bone.getParentName();
                parentText = MyString.quote(parentName);
                spButton = "Select";
            }

            int numChildren = Maud.model.bone.countChildren();
            if (numChildren > 1) {
                childText = String.format("%d children", numChildren);
                scButton = "Select";
            } else if (numChildren == 1) {
                String childName = Maud.model.bone.getChildName(0);
                childText = MyString.quote(childName);
                scButton = "Select";
            } else {
                childText = "none";
                scButton = "";
            }

            rButton = "Rename";

        } else {
            if (numBones == 0) {
                indexText = "no bones";
            } else if (numBones == 1) {
                indexText = "one bone";
            } else {
                indexText = String.format("%d bones", numBones);
            }
            nameText = "(none selected)";
            parentText = "n/a";
            spButton = "";
            childText = "n/a";
            scButton = "";
            rButton = "";
        }

        Maud.gui.setStatusText("boneIndex", indexText);
        Maud.gui.setStatusText("boneName", " " + nameText);
        Maud.gui.setStatusText("boneParent", " " + parentText);
        Maud.gui.setStatusText("boneChildren", " " + childText);

        Maud.gui.setButtonLabel("boneRenameButton", rButton);
        Maud.gui.setButtonLabel("boneSelectParentButton", spButton);
        Maud.gui.setButtonLabel("boneSelectChildButton", scButton);

        Maud.gui.axes.update();
        Maud.gui.boneAngle.set();
        Maud.gui.boneOffset.set();
        Maud.gui.boneScale.set();
    }
}
