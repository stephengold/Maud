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

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyString;
import jme3utilities.nifty.GuiScreenController;
import jme3utilities.nifty.Tool;
import maud.DescribeUtil;
import maud.Maud;
import maud.model.EditorModel;
import maud.model.cgm.Cgm;
import maud.model.cgm.SelectedShape;
import maud.model.option.ShapeParameter;

/**
 * The controller for the "Shape" tool in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class ShapeTool extends Tool {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(ShapeTool.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized tool.
     *
     * @param screenController the controller of the screen that will contain
     * the tool (not null)
     */
    ShapeTool(GuiScreenController screenController) {
        super(screenController, "shape");
    }
    // *************************************************************************
    // Tool methods

    /**
     * Update this tool prior to rendering. (Invoked once per frame while this
     * tool is displayed.)
     */
    @Override
    protected void toolUpdate() {
        updateChildren();
        updateIndex();
        updateName();
        updateParameter();
        updateType();
        updateUsers();
    }
    // *************************************************************************
    // private methods

    /**
     * Update the children and child-select button.
     */
    private void updateChildren() {
        String childrenText = "";
        String scButton = "";

        Cgm target = Maud.getModel().getTarget();
        SelectedShape shape = target.getShape();
        if (shape.isSelected()) {
            String type = shape.type();
            if (type.equals("Compound")) {
                int numChildren = shape.countChildren();
                if (numChildren == 0) {
                    childrenText = "none";
                } else if (numChildren == 1) {
                    List<String> children = shape.listChildNames("");
                    childrenText = children.get(0);
                    scButton = "Select";
                } else {
                    childrenText = String.format("%d children", numChildren);
                    scButton = "Select";
                }
            } else {
                childrenText = "(not applicable)";
            }
        }

        setStatusText("shapeChildren", " " + childrenText);
        setButtonText("shapeSelectChild", scButton);
    }

    /**
     * Update the index status and next/previous/select buttons.
     */
    private void updateIndex() {
        String indexStatus;
        String nextButton = "", previousButton = "", selectButton;

        Cgm target = Maud.getModel().getTarget();
        SelectedShape shape = target.getShape();
        boolean isSelected = shape.isSelected();
        int numShapes = target.getPhysics().countShapes();

        if (numShapes == 0) {
            selectButton = "";
        } else if (numShapes == 1 && isSelected) {
            selectButton = "Deselect shape";
        } else {
            selectButton = "Select shape";
        }

        if (isSelected) {
            int selectedIndex = shape.index();
            indexStatus = DescribeUtil.index(selectedIndex, numShapes);
            if (numShapes > 1) {
                nextButton = "+";
                previousButton = "-";
            }
        } else if (numShapes == 0) {
            indexStatus = "no shapes";
        } else if (numShapes == 1) {
            indexStatus = "one shape";
        } else {
            indexStatus = String.format("%d shapes", numShapes);
        }

        setStatusText("shapeIndex", indexStatus);
        setButtonText("shapeNext", nextButton);
        setButtonText("shapePrevious", previousButton);
        setButtonText("shapeSelect", selectButton);
    }

    /**
     * Update the name status.
     */
    private void updateName() {
        String name;
        SelectedShape shape = Maud.getModel().getTarget().getShape();
        if (shape.isSelected()) {
            name = shape.name();
        } else {
            name = "(none selected)";
        }
        setStatusText("shapeName", " " + name);
    }

    /**
     * Update the parameter buttons.
     */
    private void updateParameter() {
        EditorModel model = Maud.getModel();
        ShapeParameter parameter = model.getMisc().shapeParameter();
        String name = parameter.toString();
        setButtonText("shapeParm", name);

        SelectedShape shape = model.getTarget().getShape();
        float value = shape.value(parameter);
        String valueString = "";
        if (!Float.isNaN(value)) {
            valueString = Float.toString(value);
        }
        setButtonText("shapeParmValue", valueString);
    }

    /**
     * Update the type, axis, and vertex counts.
     */
    private void updateType() {
        String type = "";
        String axisName = "";
        String vertices = "";

        Cgm target = Maud.getModel().getTarget();
        SelectedShape shape = target.getShape();
        if (shape.isSelected()) {
            type = shape.type();
            int axisIndex = shape.mainAxisIndex();
            if (axisIndex != -1) {
                axisName = MyString.axisName(axisIndex);
            }

            int numVertices = shape.countGeneratorVertices();
            vertices = String.format("%d", numVertices);
        }

        setStatusText("shapeType", " " + type);
        setStatusText("shapeAxis", axisName);
        setStatusText("shapeVertices", " " + vertices);
    }

    /**
     * Update the users and select buttons.
     */
    private void updateUsers() {
        String usersText = "";
        String suButton = "";

        Cgm target = Maud.getModel().getTarget();
        SelectedShape shape = target.getShape();
        if (shape.isSelected()) {
            Set<Long> userSet = shape.userSet();
            int numUsers = userSet.size();
            if (numUsers == 0) {
                usersText = "unused";
            } else if (numUsers == 1) {
                long userId = Misc.first(userSet);
                usersText = target.getPhysics().name(userId);
                suButton = "Select user";
            } else {
                usersText = String.format("%d users", numUsers);
                suButton = "Select user";
            }
        }

        setStatusText("shapeUsers", " " + usersText);
        setButtonText("shapeSelectUser", suButton);
    }
}
