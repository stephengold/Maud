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
package maud.menu;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.CollisionShape;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import jme3utilities.MyControl;
import jme3utilities.MyString;
import maud.Maud;
import maud.PhysicsUtil;
import maud.action.ActionPrefix;
import maud.dialog.EditorDialogs;
import maud.model.cgm.Cgm;
import maud.model.cgm.SelectedShape;
import maud.model.option.RigidBodyParameter;
import maud.model.option.ShapeParameter;

/**
 * Menus in Maud's editor screen that relate to physics shapes, objects, and
 * joints.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class PhysicsMenus {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(PhysicsMenus.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private PhysicsMenus() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Handle a "select menuItem" action from the Physics menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    static boolean menuPhysics(String remainder) {
        boolean handled = true;
        String addControlPrefix = "Add control" + EditorMenus.menuPathSeparator;
        if (remainder.startsWith(addControlPrefix)) {
            String arg = MyString.remainder(remainder, addControlPrefix);
            handled = menuPhysicsAddControl(arg);

        } else {
            Cgm target = Maud.getModel().getTarget();
            switch (remainder) {
                case "Add control":
                    addControl();
                    break;

                case "Compound shape":
                    target.getShape().addParent();
                    break;

                case "Joint tool":
                    Maud.gui.tools.select("joint");
                    break;

                case "Mass":
                    EditorDialogs.setPhysicsRbpValue(RigidBodyParameter.Mass);
                    break;

                case "Object tool":
                    Maud.gui.tools.select("object");
                    break;

                case "Select joint":
                    selectJoint(target);
                    break;

                case "Select object":
                    selectObject(target);
                    break;

                case "Select shape":
                    selectShape(target);
                    break;

                case "Shape tool":
                    Maud.gui.tools.select("shape");
                    break;

                default:
                    handled = false;
            }
        }

        return handled;
    }

    /**
     * Display a "select joint" menu.
     *
     * @param cgm which load slot (not null)
     */
    public static void selectJoint(Cgm cgm) {
        if (cgm.isLoaded()) {
            List<String> names = cgm.listJointNames("");
            if (!names.isEmpty()) {
                MenuBuilder builder = new MenuBuilder();
                for (String name : names) {
                    builder.add(name);
                }
                builder.show(ActionPrefix.selectJoint);
            }
        }
    }

    /**
     * Display a "select physics" menu to select a physics object.
     *
     * @param cgm which load slot (not null)
     */
    public static void selectObject(Cgm cgm) {
        if (cgm.isLoaded()) {
            List<String> names = cgm.listObjectNames("");
            if (!names.isEmpty()) {
                MenuBuilder builder = new MenuBuilder();
                for (String name : names) {
                    builder.add(name);
                }
                builder.show(ActionPrefix.selectPhysics);
            }
        }
    }

    /**
     * Display a "select physicsRbp" menu to select a rigid-body parameter.
     */
    public static void selectRbp() {
        MenuBuilder builder = new MenuBuilder();

        RigidBodyParameter selectedRbp = Maud.getModel().getMisc().getRbp();
        for (RigidBodyParameter rbp : RigidBodyParameter.values()) {
            if (!rbp.equals(selectedRbp)) {
                String name = rbp.toString();
                builder.add(name);
            }
        }

        builder.show(ActionPrefix.selectPhysicsRbp);
    }

    /**
     * Display a "select shape" menu.
     *
     * @param cgm which load slot (not null)
     */
    public static void selectShape(Cgm cgm) {
        if (cgm.isLoaded()) {
            List<String> names = cgm.listShapeNames("");
            if (!names.isEmpty()) {
                MenuBuilder builder = new MenuBuilder();
                for (String name : names) {
                    builder.add(name);
                }
                builder.show(ActionPrefix.selectShape);
            }
        }
    }

    /**
     * Handle a "select shapeChild" action without arguments.
     */
    public static void selectShapeChild() {
        Cgm target = Maud.getModel().getTarget();
        SelectedShape shape = target.getShape();
        int numChildren = shape.countChildren();
        if (numChildren == 1) {
            shape.selectFirstChild();
        } else if (numChildren > 1) {
            List<String> names = shape.listChildNames("");
            if (!names.isEmpty()) {
                MenuBuilder builder = new MenuBuilder();
                for (String name : names) {
                    builder.add(name);
                }
                builder.show(ActionPrefix.selectShape);
            }
        }
    }

    /**
     * Display a "select shapeParm" menu to select a shape parameter.
     */
    public static void selectShapeParameter() {
        MenuBuilder builder = new MenuBuilder();

        ShapeParameter selected = Maud.getModel().getMisc().getShapeParameter();
        for (ShapeParameter parm : ShapeParameter.values()) {
            if (!parm.equals(selected)) {
                String name = parm.toString();
                builder.add(name);
            }
        }

        builder.show(ActionPrefix.selectShapeParm);
    }

    /**
     * Handle a "select shapeUser" action without arguments.
     */
    public static void selectShapeUser() {
        Cgm target = Maud.getModel().getTarget();
        SelectedShape shape = target.getShape();
        Set<Long> userSet = shape.userSet();
        int numUsers = userSet.size();
        if (numUsers == 1) {
            Long[] ids = new Long[1];
            userSet.toArray(ids);
            long userId = ids[0];
            PhysicsSpace space = target.getSceneView().getPhysicsSpace();
            CollisionShape userShape = PhysicsUtil.findShape(userId, space);
            if (userShape != null) {
                shape.select(userId);
            } else {
                PhysicsCollisionObject userObject
                        = PhysicsUtil.findObject(userId, space);
                String name = MyControl.objectName(userObject);
                target.getObject().select(name);
                Maud.gui.tools.select("object");
            }
        }
    }

    /**
     * Display a menu of types of shapes that can be created.
     *
     * @param actionPrefix action prefix for the menu (not null, not empty)
     */
    static void showShapeTypeMenu(String actionPrefix) {
        assert actionPrefix != null;
        assert !actionPrefix.isEmpty();

        MenuBuilder builder = new MenuBuilder();
        for (PhysicsUtil.ShapeType shapeType : PhysicsUtil.ShapeType.values()) {
            String string = shapeType.toString();
            builder.add(string);
        }
        builder.show(actionPrefix);
    }
    // *************************************************************************
    // private methods

    /**
     * Display a "Physics -> Add control" menu.
     */
    private static void addControl() {
        MenuBuilder builder = new MenuBuilder();

        builder.addEdit("Ghost");
        builder.addEdit("RigidBody");

        builder.show("select menuItem Physics -> Add control -> ");
    }

    /**
     * Handle a "select menuItem" action from the "Physics -> Add control" menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private static boolean menuPhysicsAddControl(String remainder) {
        boolean handled = true;
        switch (remainder) {
            case "Ghost":
                showShapeTypeMenu(ActionPrefix.newGhostControl);
                break;

            case "RigidBody":
                showShapeTypeMenu(ActionPrefix.newRbc);
                break;

            default:
                handled = false;
        }

        return handled;
    }
}
