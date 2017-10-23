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
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.WindowController;
import maud.Maud;
import maud.model.Cgm;
import maud.model.SelectedJoint;

/**
 * The controller for the "Joint Tool" window in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class JointTool extends WindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(JointTool.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController
     */
    JointTool(BasicScreenController screenController) {
        super(screenController, "jointTool", false);
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

        updateDescription();
        updateIndex();
        updateName();
    }
    // *************************************************************************
    // private methods

    /**
     * Update the joint description.
     */
    private void updateDescription() {
        SelectedJoint joint = Maud.getModel().getTarget().getJoint();
        String type = joint.getType();
        Maud.gui.setStatusText("jointType", type);
    }

    /**
     * Update the index status and next/previous/select buttons.
     */
    private void updateIndex() {
        String indexText;
        String nButton = "", pButton = "", sButton = "";

        Cgm target = Maud.getModel().getTarget();
        int numJoints = target.countJoints();
        if (numJoints > 0) {
            sButton = "Select";
        }

        SelectedJoint joint = target.getJoint();
        if (joint.isSelected()) {
            int selectedIndex = joint.index();
            int indexBase = Maud.getModel().getMisc().getIndexBase();
            indexText = String.format("#%d of %d", selectedIndex + indexBase,
                    numJoints);
            if (numJoints > 1) {
                nButton = "+";
                pButton = "-";
            }
        } else if (numJoints == 0) {
            indexText = "no joints";
        } else if (numJoints == 1) {
            indexText = "one joint";
        } else {
            indexText = String.format("%d joints", numJoints);
        }

        Maud.gui.setStatusText("jointIndex", indexText);
        Maud.gui.setButtonLabel("jointNextButton", nButton);
        Maud.gui.setButtonLabel("jointPreviousButton", pButton);
        Maud.gui.setButtonLabel("jointSelectButton", sButton);
    }

    /**
     * Update the name status.
     */
    private void updateName() {
        String name;
        SelectedJoint joint = Maud.getModel().getTarget().getJoint();
        if (joint.isSelected()) {
            long id = joint.getId();
            name = Long.toHexString(id);
        } else {
            name = "(none selected)";
        }
        Maud.gui.setStatusText("jointName", " " + name);
    }
}
