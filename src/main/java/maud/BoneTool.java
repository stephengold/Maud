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
 * Stephen Gold's name may not be used to endorse or promote products
 derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL STEPHEN GOLD BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package maud;

import com.jme3.animation.Bone;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.nifty.BasicScreenController;

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
    // fields

    /**
     * index of the selected bone, or null
     */
    private Integer selectedIndex = null;
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
     * Deselect the selected bone and update this window.
     */
    void deselect() {
        selectedIndex = null;
        update();
    }

    /**
     * Read the index of the selected bone.
     */
    int getSelectedIndex() {
        int result = selectedIndex;
        return result;
    }

    /**
     * Test whether a bone is selected.
     */
    boolean isSelected() {
        if (selectedIndex == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Change what bone is selected and update this window.
     */
    void setSelectedIndex(int newIndex) {
        selectedIndex = newIndex;
        update();
    }

    /**
     * Update this window and others after a change.
     */
    void update() {
        String description1, description2;
        String parentDescription, childDescription;
        if (isSelected()) {
            int boneCount = Maud.model.getBoneCount();
            description1 = String.format("Bone #%d of %d:",
                    selectedIndex + 1, boneCount);

            Bone bone = Maud.model.getBone();
            String name = bone.getName();
            description2 = String.format("named %s", MyString.quote(name));

            Bone parent = bone.getParent();
            if (parent == null) {
                List<String> roots = Maud.model.listRootBoneNames();
                int numRoots = roots.size();
                if (numRoots == 1) {
                    parentDescription = "the root bone";
                } else {
                    parentDescription = String.format("one of %d root bones",
                            numRoots);
                }
            } else {
                String parentName = parent.getName();
                parentDescription = String.format("a child of %s",
                        MyString.quote(parentName));
            }

            List<Bone> children = bone.getChildren();
            int numChildren = children.size();
            if (numChildren > 1) {
                childDescription = String.format("with %d children",
                        numChildren);
            } else if (numChildren == 1) {
                String childName = children.get(0).getName();
                childDescription = String.format("the parent of %s",
                        MyString.quote(childName));
            } else {
                childDescription = "with no children";
            }

        } else {
            description1 = "No bone selected.";
            description2 = "";
            parentDescription = "";
            childDescription = "";
        }

        Maud.gui.setStatusText("boneDescription1", description1);
        Maud.gui.setStatusText("boneDescription2", description2);
        Maud.gui.setStatusText("boneParentDescription", parentDescription);
        Maud.gui.setStatusText("boneChildDescription", childDescription);

        Maud.gui.axes.update();
        Maud.gui.boneAngle.set();
        Maud.gui.boneOffset.set();
        Maud.gui.boneScale.set();
    }
}
