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
package maud.menu;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.CollisionShape;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.minie.MyObject;
import maud.DescribeUtil;
import maud.Maud;
import maud.PhysicsUtil;
import maud.ShapeType;
import maud.action.ActionPrefix;
import maud.dialog.EditorDialogs;
import maud.model.cgm.Cgm;
import maud.model.cgm.SelectedBone;
import maud.model.cgm.SelectedLink;
import maud.model.cgm.SelectedRagdoll;
import maud.model.cgm.SelectedShape;
import maud.model.cgm.SelectedSpatial;
import maud.model.option.RigidBodyParameter;
import maud.model.option.ShapeParameter;
import maud.view.scene.SceneView;

/**
 * Menus in Maud's editor screen that relate to physics shapes, objects, links,
 * and joints.
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
     * Build a Physics menu.
     *
     * @param builder the menu builder to use (not null, modified)
     */
    static void buildPhysicsMenu(MenuBuilder builder) {
        builder.addSubmenu("Add control");
        builder.addTool("Shape tool");

        Cgm target = Maud.getModel().getTarget();
        SceneView sceneView = target.getSceneView();
        int numShapes = sceneView.shapeMap().size();
        if (numShapes == 1) {
            builder.add("Select shape");
        } else if (numShapes > 1) {
            builder.addSubmenu("Select shape");
        }

        SelectedShape shape = target.getShape();
        boolean isSelected = shape.isSelected();
        boolean isCompound = shape.isCompound();
        if (isSelected && !isCompound) {
            builder.addEdit("Compound shape");
        }

        builder.addTool("Collision-object tool");

        int numObjects = sceneView.objectMap().size();
        if (numObjects > 0) {
            builder.addSubmenu("Select object");
        }

        if (target.getObject().hasMass()) {
            builder.addDialog("Mass");
        }

        builder.addTool("Joint tool");

        PhysicsSpace space = sceneView.getPhysicsSpace();
        int numJoints = space.countJoints();
        if (numJoints > 0) {
            builder.addSubmenu("Select joint");
        }

        builder.addTool("Link tool");

        int numLinks = target.getRagdoll().countLinks();
        if (numLinks == 1) {
            builder.add("Select link");
        } else if (numLinks > 1) {
            builder.addSubmenu("Select link");
        }

        SelectedBone bone = target.getBone();
        if (bone.isSelected() && !bone.isLinked()) {
            builder.addEdit("Link selected bone");
        }
    }

    /**
     * Handle a "select menuItem" action from the Physics menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    static boolean menuPhysics(String remainder) {
        boolean handled = true;
        String addControlPrefix = "Add control" + EditorMenus.menuPathSeparator;
        String selectLinkPrefix = "Select link" + EditorMenus.menuPathSeparator;
        if (remainder.startsWith(addControlPrefix)) {
            String arg = MyString.remainder(remainder, addControlPrefix);
            handled = menuPhysicsAddControl(arg);
        } else if (remainder.startsWith(selectLinkPrefix)) {
            String arg = MyString.remainder(remainder, selectLinkPrefix);
            handled = menuPhysicsSelectLink(arg);
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

                case "Link selected bone":
                    target.getLink().createBoneLink();
                    break;

                case "Link tool":
                    Maud.gui.tools.select("link");
                    break;

                case "Mass":
                    EditorDialogs.setPhysicsRbpValue(RigidBodyParameter.Mass);
                    break;

                case "Collision-object tool":
                    Maud.gui.tools.select("object");
                    break;

                case "Select joint":
                    selectJoint(target);
                    break;

                case "Select link":
                    selectLink();
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
     * Handle a "select link" action without arguments.
     */
    public static void selectLink() {
        Cgm target = Maud.getModel().getTarget();
        SelectedRagdoll selectedRagdoll = target.getRagdoll();
        SelectedLink selectedLink = target.getLink();
        boolean isSelected = selectedLink.isSelected();

        if (selectedRagdoll.countLinks() == 1) {
            if (isSelected) {
                selectedLink.selectNone();
            } else {
                List<String> linkNames = selectedRagdoll.listLinkNames("");
                assert linkNames.size() == 1 : linkNames.size();
                String linkName = linkNames.get(0);
                selectedLink.select(linkName);
            }
            return;
        }

        MenuBuilder builder = new MenuBuilder();

        builder.addSubmenu("By name");
        builder.addSubmenu("By parent");

        int numChildren = selectedLink.countChildren();
        if (numChildren == 1) {
            builder.add("Child");
        } else if (numChildren > 1) {
            builder.addSubmenu("Child");
        }

        if (isSelected) {
            builder.add("Parent");
            builder.add("Next");
            builder.add("None");
            builder.add("Previous");
        }

        builder.show("select menuItem Physics -> Select link -> ");
    }

    /**
     * Handle a "select linkChild" action without arguments.
     */
    public static void selectLinkChild() {
        SelectedLink link = Maud.getModel().getTarget().getLink();
        if (link.countChildren() == 1) {
            List<String> linkNames = link.childNames();
            assert linkNames.size() == 1 : linkNames.size();
            String linkName = linkNames.get(0);
            link.select(linkName);
            return;
        }

        MenuBuilder builder = new MenuBuilder();
        List<String> childNames = link.childNames();

        for (String childName : childNames) {
            builder.add(childName);
        }

        builder.show(ActionPrefix.selectLink);
    }

    /**
     * Handle a "select linkChild" action with an argument.
     *
     * @param argument action argument (not null)
     */
    public static void selectLinkChild(String argument) {
        Cgm target = Maud.getModel().getTarget();
        if (argument.startsWith("!")) {
            String name = argument.substring(1);
            target.getLink().select(name);
        } else {
            SelectedRagdoll ragdoll = target.getRagdoll();
            List<String> linkNames = ragdoll.listChildLinkNames(argument);

            MenuBuilder builder = new MenuBuilder();
            builder.add("!" + argument);
            for (String linkName : linkNames) {
                if (ragdoll.isLeafLink(linkName)) {
                    builder.add("!" + linkName);
                } else {
                    builder.add(linkName);
                }
            }
            builder.show(ActionPrefix.selectLinkChild);
        }
    }

    /**
     * Handle a "select linkToolAxis" action without arguments.
     */
    public static void selectLinkToolAxis() {
        int selectedAxis = Maud.getModel().getMisc().linkToolAxis();
        MenuBuilder builder = new MenuBuilder();

        for (int axisIndex = PhysicsSpace.AXIS_X;
                axisIndex <= PhysicsSpace.AXIS_Z;
                ++axisIndex) {
            if (axisIndex != selectedAxis) {
                String axisName = DescribeUtil.axisName(axisIndex);
                builder.add(axisName);
            }
        }

        builder.show(ActionPrefix.selectLinkToolAxis);
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
     *
     * @param namePrefix prefix for RBP filtering (not null)
     */
    public static void selectRbp(String namePrefix) {
        Validate.nonNull(namePrefix, "name prefix");

        Set<String> matchingNames = new TreeSet<>();
        RigidBodyParameter selectedRbp
                = Maud.getModel().getMisc().rbParameter();
        for (RigidBodyParameter rbp : RigidBodyParameter.values()) {
            if (!rbp.equals(selectedRbp)) {
                String name = rbp.toString();
                if (name.startsWith(namePrefix)) {
                    matchingNames.add(name);
                }
            }
        }

        List<String> reducedList = new ArrayList<>(matchingNames);
        MyString.reduce(reducedList, ShowMenus.maxItems);
        Collections.sort(reducedList);

        MenuBuilder builder = new MenuBuilder();
        for (String name : reducedList) {
            if (matchingNames.contains(name)) {
                builder.add(name);
            } else {
                builder.addEllipsis(name);
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
            List<String> names = cgm.listShapes("");
            int numShapes = names.size();
            SelectedShape selectedShape = cgm.getShape();
            boolean isSelected = selectedShape.isSelected();

            if (numShapes == 1) {
                if (isSelected) {
                    selectedShape.selectNone();
                } else {
                    String name = names.get(0);
                    selectedShape.select(name);
                }
                return;
            }

            if (numShapes > 1) {
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

        ShapeParameter selected = Maud.getModel().getMisc().shapeParameter();
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
                String name = MyObject.objectName(userObject);
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
        for (ShapeType shapeType : ShapeType.values()) {
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

        SelectedSpatial ss = Maud.getModel().getTarget().getSpatial();
        if (ss.hasSkeletonControls()) {
            builder.addEdit("DynamicAnim");
        }
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
            case "DynamicAnim":
                Maud.getModel().getTarget().getSpatial().addRagdollControl();
                break;

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

    /**
     * Handle a "select menuItem" action from the "Physics -> Select link" menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private static boolean menuPhysicsSelectLink(String remainder) {
        boolean handled = true;

        SelectedLink selectedLink = Maud.getModel().getTarget().getLink();
        switch (remainder) {
            case "By name":
                selectLinkByName();
                break;

            case "By parent":
                selectLinkByParent();
                break;

            case "Child":
                selectLinkChild();
                break;

            case "Next":
                selectedLink.selectNext();
                break;

            case "None":
                selectedLink.selectNone();
                break;

            case "Parent":
                selectedLink.selectParent();
                break;

            case "Previous":
                selectedLink.selectPrevious();
                break;

            default:
                handled = false;
        }

        return handled;
    }

    /**
     * Select a link by name, using submenus if necessary.
     */
    private static void selectLinkByName() {
        SelectedRagdoll ragdoll = Maud.getModel().getTarget().getRagdoll();
        List<String> nameList = ragdoll.listLinkNames("");
        showLinkSubmenu(nameList);
    }

    /**
     * Select a link by parent, using submenus.
     */
    private static void selectLinkByParent() {
        List<String> linkNames = new ArrayList<>(1);
        linkNames.add("Torso:");
        Maud.gui.showPopupMenu(ActionPrefix.selectLinkChild, linkNames);
    }

    /**
     * Display a submenu for selecting a physics link by name using the "select
     * link " action prefix.
     *
     * @param nameList list of names from which to select (not null)
     */
    private static void showLinkSubmenu(List<String> nameList) {
        assert nameList != null;

        MyString.reduce(nameList, ShowMenus.maxItems);
        Collections.sort(nameList);

        MenuBuilder builder = new MenuBuilder();
        for (String linkName : nameList) {
            if (Maud.getModel().getTarget().getRagdoll().hasLink(linkName)) {
                builder.add(linkName);
            } else {
                builder.addEllipsis(linkName);
            }
        }
        builder.show(ActionPrefix.selectLink);
    }
}
