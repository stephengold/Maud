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

import com.jme3.light.Light;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.nifty.GuiScreenController;
import maud.Maud;
import maud.MaudUtil;
import maud.model.cgm.Cgm;
import maud.model.cgm.SelectedLight;

/**
 * The controller for the "Light" tool in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class LightsTool extends Tool {
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
     * Instantiate an uninitialized tool.
     *
     * @param screenController the controller of the screen that will contain
     * the tool (not null)
     */
    LightsTool(GuiScreenController screenController) {
        super(screenController, "lights");
    }
    // *************************************************************************
    // Tool methods

    /**
     * Enumerate this tool's check boxes.
     *
     * @return a new list of names (unique id prefixes)
     */
    @Override
    protected List<String> listCheckBoxes() {
        List<String> result = super.listCheckBoxes();
        result.add("lightEnable");

        return result;
    }

    /**
     * Update the MVC model based on a check-box event.
     *
     * @param name the name (unique id prefix) of the check box
     * @param isChecked the new state of the check box (true&rarr;checked,
     * false&rarr;unchecked)
     */
    @Override
    public void onCheckBoxChanged(String name, boolean isChecked) {
        SelectedLight light = Maud.getModel().getTarget().getLight();
        switch (name) {
            case "lightEnable":
                light.setEnabled(isChecked);
                break;

            default:
                super.onCheckBoxChanged(name, isChecked);
        }
    }

    /**
     * Callback to update this tool prior to rendering. (Invoked once per render
     * pass while this tool is displayed.)
     */
    @Override
    protected void toolUpdate() {
        updateIndex();
        updateProperties();

        String deleteButton, renameButton, selectOwnerButton;
        String nameStatus, ownerStatus, typeStatus;

        SelectedLight light = Maud.getModel().getTarget().getLight();
        if (light.isSelected()) {
            boolean isEnabled = light.isEnabled();
            setChecked("lightEnable", isEnabled);

            deleteButton = "Delete";
            renameButton = "Rename";
            selectOwnerButton = "Select";
            String lightName = light.name();
            nameStatus = MyString.quote(lightName);
            String spatialName = light.ownerName();
            ownerStatus = MyString.quote(spatialName);
            typeStatus = light.getType();

        } else {
            disableCheckBox("lightEnable");

            deleteButton = "";
            renameButton = "";
            selectOwnerButton = "";
            nameStatus = "(no light selected)";
            ownerStatus = "(no light selected)";
            typeStatus = "(no light selected)";
        }

        setButtonText("lightDelete", deleteButton);
        setButtonText("lightRename", renameButton);
        setButtonText("lightSelectOwner", selectOwnerButton);
        setStatusText("lightName", " " + nameStatus);
        setStatusText("lightOwner", " " + ownerStatus);
        setStatusText("lightType", " " + typeStatus);
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
            indexStatus = MaudUtil.formatIndex(selectedIndex);
            indexStatus = String.format("%s of %d", indexStatus, numLights);
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

        setButtonText("lightNext", nextButton);
        setButtonText("lightPrevious", previousButton);
        setStatusText("lightIndex", indexStatus);
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

        setStatusText("lightColor", " " + colorStatus);
        setStatusText("lightDirection", " " + directionStatus);
        setStatusText("lightPosition", " " + positionStatus);
    }
}
