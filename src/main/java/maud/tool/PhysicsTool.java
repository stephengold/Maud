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

import com.jme3.bullet.PhysicsSpace;
import java.util.logging.Logger;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.WindowController;
import maud.Maud;
import maud.PhysicsUtil;
import maud.model.Cgm;
import maud.model.SelectedPhysics;

/**
 * The controller for the "Physics Tool" window in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class PhysicsTool extends WindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(PhysicsTool.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController
     */
    PhysicsTool(BasicScreenController screenController) {
        super(screenController, "physicsTool", false);
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

        updateIndex();
        updateName();
        updateShape();

        SelectedPhysics physics = Maud.getModel().getTarget().getPhysics();
        String mass = physics.getMass();
        Maud.gui.setButtonLabel("physicsMassButton", mass);
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
        PhysicsSpace space = target.getSceneView().getPhysicsSpace();
        int numObjects = PhysicsUtil.countObjects(space);
        if (numObjects > 0) {
            sButton = "Select";
        }

        SelectedPhysics physics = target.getPhysics();
        if (physics.isSelected()) {
            int selectedIndex = physics.index();
            int indexBase = Maud.getModel().getMisc().getIndexBase();
            indexText = String.format("#%d of %d", selectedIndex + indexBase,
                    numObjects);
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

        Maud.gui.setStatusText("physicsIndex", indexText);
        Maud.gui.setButtonLabel("physicsNextButton", nButton);
        Maud.gui.setButtonLabel("physicsPreviousButton", pButton);
        Maud.gui.setButtonLabel("physicsSelectObjectButton", sButton);
    }

    /**
     * Update the name status.
     */
    private void updateName() {
        String name;
        SelectedPhysics physics = Maud.getModel().getTarget().getPhysics();
        if (physics.isSelected()) {
            name = physics.getName();
        } else {
            name = "(none selected)";
        }
        Maud.gui.setStatusText("physicsName", " " + name);
    }

    /**
     * Update the shape status and select button.
     */
    private void updateShape() {
        String sButton, shape;

        SelectedPhysics physics = Maud.getModel().getTarget().getPhysics();
        long id = physics.getShapeId();
        if (id == -1L) {
            shape = "";
            sButton = "";
        } else {
            shape = Long.toHexString(id);
            sButton = "Select";
        }

        Maud.gui.setStatusText("physicsShape", " " + shape);
        Maud.gui.setButtonLabel("physicsSelectShapeButton", sButton);
    }
}
