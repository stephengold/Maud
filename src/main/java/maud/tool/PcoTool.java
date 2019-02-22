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

import java.util.logging.Logger;
import jme3utilities.nifty.GuiScreenController;
import jme3utilities.nifty.Tool;
import maud.DescribeUtil;
import maud.Maud;
import maud.model.EditorModel;
import maud.model.cgm.Cgm;
import maud.model.cgm.SelectedPco;
import maud.model.option.RigidBodyParameter;

/**
 * The controller for the "Collision-Object" tool in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class PcoTool extends Tool {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(PcoTool.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized tool.
     *
     * @param screenController the controller of the screen that will contain
     * the tool (not null)
     */
    PcoTool(GuiScreenController screenController) {
        super(screenController, "pco");
    }
    // *************************************************************************
    // Tool methods

    /**
     * Update this tool prior to rendering. (Invoked once per frame while this
     * tool is displayed.)
     */
    @Override
    protected void toolUpdate() {
        updateIndex();
        updateName();
        updateParameter();
        updateShape();
    }
    // *************************************************************************
    // private methods

    /**
     * Update the index status and next/previous/select buttons.
     */
    private void updateIndex() {
        String indexStatus;
        String nextButton = "", previousButton = "", selectButton = "";

        Cgm target = Maud.getModel().getTarget();
        int numPcos = target.getPhysics().countPcos();
        if (numPcos > 0) {
            selectButton = "Select";
        }

        SelectedPco pco = target.getPco();
        if (pco.isSelected()) {
            int selectedIndex = pco.index();
            indexStatus = DescribeUtil.index(selectedIndex, numPcos);
            if (numPcos > 1) {
                nextButton = "+";
                previousButton = "-";
            }
        } else if (numPcos == 0) {
            indexStatus = "no objects";
        } else if (numPcos == 1) {
            indexStatus = "one object";
        } else {
            indexStatus = String.format("%d objects", numPcos);
        }

        setStatusText("pcoIndex", indexStatus);
        setButtonText("pcoNext", nextButton);
        setButtonText("pcoPrevious", previousButton);
        setButtonText("pcoSelect", selectButton);
        // TODO more parameters: user object, collision group,
        // collideWith groups, debug mesh resolution/normals,
        // contact-response flag, static flag
    }

    /**
     * Update the name status.
     */
    private void updateName() {
        String name;
        SelectedPco object = Maud.getModel().getTarget().getPco();
        if (object.isSelected()) {
            name = object.name();
        } else {
            name = "(none selected)";
        }
        setStatusText("pcoName", " " + name);
    }

    /**
     * Update the parameter buttons.
     */
    private void updateParameter() {
        EditorModel model = Maud.getModel();
        RigidBodyParameter rbp = model.getMisc().rbParameter();
        String name = rbp.toString();
        SelectedPco pco = model.getTarget().getPco();
        String value = pco.value(rbp);

        setButtonText("pcoParm", name);
        setButtonText("pcoParmValue", value);
    }

    /**
     * Update the shape status and select button.
     */
    private void updateShape() {
        String sButton, shape;

        SelectedPco pco = Maud.getModel().getTarget().getPco();
        if (pco.isSelected()) {
            shape = pco.shapeName();
            sButton = "Select";
        } else {
            shape = "";
            sButton = "";
        }

        setStatusText("pcoShape", " " + shape);
        setButtonText("pcoSelectShape", sButton);
    }
}
