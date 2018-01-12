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
import jme3utilities.nifty.GuiScreenController;
import jme3utilities.nifty.GuiWindowController;
import maud.Maud;
import maud.MaudUtil;
import maud.model.EditorModel;
import maud.model.cgm.Cgm;
import maud.model.cgm.SelectedObject;
import maud.model.option.RigidBodyParameter;

/**
 * The controller for the "Object Tool" window in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class ObjectTool extends GuiWindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(ObjectTool.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController
     */
    ObjectTool(GuiScreenController screenController) {
        super(screenController, "objectTool", false);
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

        updateIndex();
        updateName();
        updateRbp();
        updateShape();
    }
    // *************************************************************************
    // private methods

    /**
     * Update the index status and next/previous/select buttons.
     */
    private void updateIndex() {
        String indexText;
        String nButton = "", pButton = "", sButton = "";

        Cgm target = Maud.getModel().getTarget();
        int numObjects = target.getSceneView().objectMap().size();
        if (numObjects > 0) {
            sButton = "Select";
        }

        SelectedObject object = target.getObject();
        if (object.isSelected()) {
            int selectedIndex = object.index();
            indexText = MaudUtil.formatIndex(selectedIndex);
            indexText = String.format("%s of %d", indexText, numObjects);
            if (numObjects > 1) {
                nButton = "+";
                pButton = "-";
            }
        } else if (numObjects == 0) {
            indexText = "no objects";
        } else if (numObjects == 1) {
            indexText = "one object";
        } else {
            indexText = String.format("%d objects", numObjects);
        }

        setStatusText("physicsIndex", indexText);
        setButtonText("physicsNext", nButton);
        setButtonText("physicsPrevious", pButton);
        setButtonText("physicsSelectObject", sButton);
    }

    /**
     * Update the name status.
     */
    private void updateName() {
        String name;
        SelectedObject object = Maud.getModel().getTarget().getObject();
        if (object.isSelected()) {
            name = object.getName();
        } else {
            name = "(none selected)";
        }
        setStatusText("physicsName", " " + name);
    }

    /**
     * Update the rigid-body parameter buttons.
     */
    private void updateRbp() {
        EditorModel model = Maud.getModel();
        RigidBodyParameter rbp = model.getMisc().getRbp();
        String rbpName = rbp.toString();
        SelectedObject object = model.getTarget().getObject();
        String rbpValue = object.getRbpValue(rbp);

        setButtonText("physicsRbp", rbpName);
        setButtonText("physicsRbpValue", rbpValue);
    }

    /**
     * Update the shape status and select button.
     */
    private void updateShape() {
        String sButton, shape;

        SelectedObject object = Maud.getModel().getTarget().getObject();
        long id = object.getShapeId();
        if (id == -1L) {
            shape = "";
            sButton = "";
        } else {
            shape = object.describeShape();
            sButton = "Select";
        }

        setStatusText("physicsShape", " " + shape);
        setButtonText("physicsSelectShape", sButton);
    }
}
