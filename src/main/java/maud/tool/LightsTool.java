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

import com.jme3.light.Light;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.WindowController;
import maud.Maud;
import maud.model.cgm.Cgm;
import maud.model.cgm.SelectedLight;

/**
 * The controller for the "Light Tool" window in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class LightsTool extends WindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(LightsTool.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController
     */
    LightsTool(BasicScreenController screenController) {
        super(screenController, "lightsTool", false);
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
        updateProperties();

        SelectedLight light = Maud.getModel().getTarget().getLight();
        boolean isEnabled = light.isEnabled();
        Maud.gui.setChecked("lightEnable", isEnabled);

        String deleteButton, renameButton, selectOwnerButton;
        String nameStatus, ownerStatus, typeStatus;

        if (light.isSelected()) {
            deleteButton = "Delete";
            renameButton = "Rename";
            selectOwnerButton = "Select";
            String lightName = light.name();
            nameStatus = MyString.quote(lightName);
            String spatialName = light.ownerName();
            ownerStatus = MyString.quote(spatialName);
            typeStatus = light.getType();
        } else {
            deleteButton = "";
            renameButton = "";
            selectOwnerButton = "";
            nameStatus = "(no light selected)";
            ownerStatus = "(no light selected)";
            typeStatus = "(no light selected)";
        }

        Maud.gui.setButtonText("lightDelete", deleteButton);
        Maud.gui.setButtonText("lightRename", renameButton);
        Maud.gui.setButtonText("lightSelectOwner", selectOwnerButton);
        Maud.gui.setStatusText("lightName", " " + nameStatus);
        Maud.gui.setStatusText("lightOwner", " " + ownerStatus);
        Maud.gui.setStatusText("lightType", " " + typeStatus);
    }
    // *************************************************************************
    // private methods

    /**
     * Update the index status and previous/next-button texts.
     */
    private void updateIndex() {
        String nextButton, previousButton, indexStatus;

        Cgm target = Maud.getModel().getTarget();
        int numLights = target.countLights(Light.class);
        SelectedLight light = target.getLight();
        if (light.isSelected()) {
            nextButton = "+";
            previousButton = "-";
            int selectedIndex = light.findIndex();
            int indexBase = Maud.getModel().getMisc().getIndexBase();
            indexStatus = String.format("#%d of %d", selectedIndex + indexBase,
                    numLights);

        } else {
            nextButton = "";
            previousButton = "";
            if (numLights == 0) {
                indexStatus = "no lights";
            } else if (numLights == 1) {
                indexStatus = "one light";
            } else {
                indexStatus = String.format("%d lights", numLights);
            }
        }

        Maud.gui.setButtonText("lightNext", nextButton);
        Maud.gui.setButtonText("lightPrevious", previousButton);
        Maud.gui.setStatusText("lightIndex", indexStatus);
    }

    /**
     * Update the color/direction/position status.
     */
    private void updateProperties() {
        String colorStatus, directionStatus, positionStatus;

        SelectedLight light = Maud.getModel().getTarget().getLight();
        if (light.isSelected()) {
            ColorRGBA color = light.color();
            colorStatus = String.format("%.3f %.3f %.3f",
                    color.r, color.g, color.b);

            Vector3f direction = light.direction();
            if (direction == null) {
                directionStatus = "(not applicable)";
            } else {
                directionStatus = String.format("%.3f %.3f %.3f",
                        direction.x, direction.y, direction.z);
            }

            Vector3f position = light.position();
            if (position == null) {
                positionStatus = "(not applicable)";
            } else {
                positionStatus = String.format("%.3f %.3f %.3f",
                        position.x, position.y, position.z);
            }
        } else {
            colorStatus = "(no light selected)";
            directionStatus = "(no light selected)";
            positionStatus = "(no light selected)";
        }

        Maud.gui.setStatusText("lightColor", " " + colorStatus);
        Maud.gui.setStatusText("lightDirection", " " + directionStatus);
        Maud.gui.setStatusText("lightPosition", " " + positionStatus);
    }
}
