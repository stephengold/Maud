/*
 Copyright (c) 2017-2019, Stephen Gold
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

import com.jme3.bullet.joints.JointEnd;
import java.util.logging.Logger;
import jme3utilities.nifty.GuiScreenController;
import jme3utilities.nifty.Tool;
import maud.DescribeUtil;
import maud.Maud;
import maud.model.cgm.Cgm;
import maud.model.cgm.SelectedJoint;

/**
 * The controller for the "Joint" tool in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class JointTool extends Tool {
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
     * Instantiate an uninitialized tool.
     *
     * @param screenController the controller of the screen that will contain
     * the tool (not null)
     */
    JointTool(GuiScreenController screenController) {
        super(screenController, "joint");
    }
    // *************************************************************************
    // Tool methods

    /**
     * Update this tool prior to rendering. (Invoked once per frame while this
     * tool is displayed.)
     */
    @Override
    protected void toolUpdate() {
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
        setStatusText("jointType", " " + type);

        String nameA = "", nameB = "";
        if (joint.isSelected()) {
            nameA = joint.endName(JointEnd.A);
            nameB = joint.endName(JointEnd.B);
        }
        setButtonText("jointBodyA", nameA);
        setButtonText("jointBodyB", nameB);
    }

    /**
     * Update the index status and next/previous/select buttons.
     */
    private void updateIndex() {
        String indexStatus;
        String nextButton = "", previousButton = "", selectButton = "";

        Cgm target = Maud.getModel().getTarget();
        int numJoints = target.getPhysics().countJoints();
        if (numJoints > 0) {
            selectButton = "Select joint";
        }

        SelectedJoint joint = target.getJoint();
        if (joint.isSelected()) {
            int selectedIndex = joint.index();
            indexStatus = DescribeUtil.index(selectedIndex, numJoints);
            if (numJoints > 1) {
                nextButton = "+";
                previousButton = "-";
            }
        } else if (numJoints == 0) {
            indexStatus = "no joints";
        } else if (numJoints == 1) {
            indexStatus = "one joint";
        } else {
            indexStatus = String.format("%d joints", numJoints);
        }

        setStatusText("jointIndex", indexStatus);
        setButtonText("jointNext", nextButton);
        setButtonText("jointPrevious", previousButton);
        setButtonText("jointSelect", selectButton);
    }

    /**
     * Update the name status.
     */
    private void updateName() {
        String name;
        SelectedJoint joint = Maud.getModel().getTarget().getJoint();
        if (joint.isSelected()) {
            name = joint.name();
        } else {
            name = "(none selected)";
        }
        setStatusText("jointName", " " + name);
    }
}
