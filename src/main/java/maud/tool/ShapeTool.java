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
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.CollisionShape;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import jme3utilities.MyControl;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.WindowController;
import maud.Maud;
import maud.PhysicsUtil;
import maud.model.EditorModel;
import maud.model.cgm.Cgm;
import maud.model.cgm.SelectedShape;
import maud.model.option.ShapeParameter;

/**
 * The controller for the "Shape Tool" window in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class ShapeTool extends WindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(ShapeTool.class.getName());
    /**
     * names of the coordinate axes
     */
    final private static String[] axisNames = {"X", "Y", "Z"};
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController
     */
    ShapeTool(BasicScreenController screenController) {
        super(screenController, "shapeTool", false);
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
     * Update the children and select button.
     */
    private void updateChildren() {
        String childrenText = "";
        String scButton = "";

        Cgm target = Maud.getModel().getTarget();
        SelectedShape shape = target.getShape();
        if (shape.isSelected()) {
            String type = shape.getType();
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
                childrenText = "n/a";
            }
        }

        Maud.gui.setStatusText("shapeChildren", " " + childrenText);
        Maud.gui.setButtonLabel("shapeSelectChildButton", scButton);
    }

    /**
     * Update the index status and next/previous/select buttons.
     */
    private void updateIndex() {
        String indexText;
        String nButton = "", pButton = "", sButton = "";

        Cgm target = Maud.getModel().getTarget();
        int numShapes = target.getSceneView().shapeMap().size();
        if (numShapes > 0) {
            sButton = "Select";
        }

        SelectedShape shape = target.getShape();
        if (shape.isSelected()) {
            int selectedIndex = shape.index();
            int indexBase = Maud.getModel().getMisc().getIndexBase();
            indexText = String.format("#%d of %d", selectedIndex + indexBase,
                    numShapes);
            if (numShapes > 1) {
                nButton = "+";
                pButton = "-";
            }
        } else if (numShapes == 0) {
            indexText = "no shapes";
        } else if (numShapes == 1) {
            indexText = "one shape";
        } else {
            indexText = String.format("%d shapes", numShapes);
        }

        Maud.gui.setStatusText("shapeIndex", indexText);
        Maud.gui.setButtonLabel("shapeNextButton", nButton);
        Maud.gui.setButtonLabel("shapePreviousButton", pButton);
        Maud.gui.setButtonLabel("shapeSelectButton", sButton);
    }

    /**
     * Update the name status.
     */
    private void updateName() {
        String name;
        SelectedShape shape = Maud.getModel().getTarget().getShape();
        if (shape.isSelected()) {
            long id = shape.getId();
            name = Long.toHexString(id);
        } else {
            name = "(none selected)";
        }
        Maud.gui.setStatusText("shapeName", " " + name);
    }

    /**
     * Update the parameter buttons.
     */
    private void updateParameter() {
        EditorModel model = Maud.getModel();
        ShapeParameter parameter = model.getMisc().getShapeParameter();
        String name = parameter.toString();
        Maud.gui.setButtonLabel("shapeParmButton", name);

        SelectedShape shape = model.getTarget().getShape();
        float value = shape.getValue(parameter);
        String valueString = "";
        if (!Float.isNaN(value)) {
            valueString = Float.toString(value);
        }
        Maud.gui.setButtonLabel("shapeParmValueButton", valueString);
    }

    /**
     * Update the type and axis.
     */
    private void updateType() {
        String type = "";
        String axisName = "";

        Cgm target = Maud.getModel().getTarget();
        SelectedShape shape = target.getShape();
        if (shape.isSelected()) {
            type = shape.getType();
            int axisIndex = shape.getAxisIndex();
            if (axisIndex != -1) {
                axisName = axisNames[axisIndex];
            }
        }

        Maud.gui.setStatusText("shapeType", " " + type);
        Maud.gui.setStatusText("shapeAxis", axisName);
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
                Long[] ids = new Long[1];
                userSet.toArray(ids);
                long userId = ids[0];
                PhysicsSpace space = target.getSceneView().getPhysicsSpace();
                CollisionShape userShape = PhysicsUtil.findShape(userId, space);
                if (userShape != null) {
                    usersText = Long.toHexString(userId);
                    suButton = "Select";
                } else {
                    PhysicsCollisionObject userObject
                            = PhysicsUtil.findObject(userId, space);
                    usersText = MyControl.objectName(userObject);
                    suButton = "Select";
                }
            } else {
                usersText = String.format("%d users", numUsers);
            }
        }

        Maud.gui.setStatusText("shapeUsers", " " + usersText);
        Maud.gui.setButtonLabel("shapeSelectUserButton", suButton);
    }
}
