/*
 Copyright (c) 2017-2023, Stephen Gold
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

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyString;
import maud.Maud;
import maud.action.ActionPrefix;
import maud.dialog.EditorDialogs;
import maud.model.cgm.Cgm;
import maud.model.cgm.EditableCgm;
import maud.model.cgm.SelectedBone;
import maud.model.cgm.SelectedSpatial;
import maud.tool.EditorTools;

/**
 * Spatial menus in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
final public class SpatialMenus {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SpatialMenus.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private SpatialMenus() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Build a Spatial menu.
     *
     * @param builder the menu builder to use (not null, modified)
     */
    static void buildSpatialMenu(MenuBuilder builder) {
        builder.addTool("Tool");
        builder.addSubmenu("Select");
        builder.addSubmenu("Add new");

        builder.addTool("Details tool");
        builder.addTool("Lights tool");
        builder.addTool("Material tool");
        builder.addTool("Mesh tool");
        builder.addTool("Overrides tool");
        builder.addTool("Rotate tool");
        builder.addTool("Scale tool");
        builder.addTool("Translate tool");
        builder.addTool("User-Data tool");

        SelectedSpatial ss = Maud.getModel().getTarget().getSpatial();
        int treeLevel = ss.treeLevel();
        boolean isCgmRoot = (treeLevel == 0);
        int numChildren = ss.countChildren();
        if (!ss.isTransformIdentity() && numChildren > 0) {
            builder.addEdit("Apply transform to children");
        }
        if (ss.containsMeshes()) {
            builder.addEdit("Apply transform to meshes");
        }
        if (treeLevel > 1) {
            builder.addEdit("Boost");
        }
        if (numChildren > 0 && !isCgmRoot) {
            builder.addEdit("Boost children");
        }
        if (!isCgmRoot) {
            builder.addEdit("Delete");
        }
        if (numChildren > 0) {
            builder.addEdit("Delete children");
        }
        if (ss.hasMaterial()) {
            builder.addSubmenu("Edit material");
        }
        if (numChildren > 1) {
            builder.addDialog("Merge geometries");
        }
        if (ss.isNode() && !ss.listReparentItems().isEmpty()) {
            builder.addDialog("Reparent spatials");
        }
        if (ss.hasMesh() && !isCgmRoot) {
            builder.addEdit("Split geometry");
        }
    }

    /**
     * Display a "Spatial -&gt; Edit material" menu.
     */
    public static void editMaterial() {
        MenuBuilder builder = new MenuBuilder();

        Cgm target = Maud.getModel().getTarget();
        int uses = target.getSpatial().countMaterialUses();
        if (uses > 1) {
            builder.addEdit("Clone");
        }

        builder.addEdit("Replace with debug");
        builder.addEdit("Replace with lit");
        builder.addEdit("Replace with unshaded");

        builder.show("select menuItem Spatial -> Edit material -> ");
    }

    /**
     * Handle a "select menuItem" action from the Spatial menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    static boolean menuSpatial(String remainder) {
        boolean handled = true;
        String addPrefix = "Add new" + EditorMenus.menuPathSeparator;
        String edmatPrefix = "Edit material" + EditorMenus.menuPathSeparator;
        String selectPrefix = "Select" + EditorMenus.menuPathSeparator;
        if (remainder.startsWith(addPrefix)) {
            String arg = MyString.remainder(remainder, addPrefix);
            handled = menuSpatialAdd(arg);

        } else if (remainder.startsWith(edmatPrefix)) {
            String arg = MyString.remainder(remainder, edmatPrefix);
            handled = menuEditMaterial(arg);

        } else if (remainder.startsWith(selectPrefix)) {
            String arg = MyString.remainder(remainder, selectPrefix);
            handled = menuSpatialSelect(arg);

        } else {
            EditableCgm target = Maud.getModel().getTarget();
            SelectedSpatial ss = target.getSpatial();
            switch (remainder) {
                case "Add new":
                    addNew();
                    break;

                case "Apply transform to children":
                    ss.applyTransformToChildren();
                    break;

                case "Apply transform to meshes":
                    ss.applyTransformToMeshes();
                    break;

                case "Boost":
                    ss.boost();
                    break;

                case "Boost children":
                    ss.boostAllChildren();
                    break;

                case "Delete":
                    ss.delete();
                    break;

                case "Delete children":
                    target.deleteAllChildren();
                    break;

                case "Details tool":
                    EditorTools.select("spatialDetails");
                    break;

                case "Edit material":
                    editMaterial();
                    break;

                case "Lights tool":
                    EditorTools.select("lights");
                    break;

                case "Material tool":
                    EditorTools.select("material");
                    break;

                case "Merge geometries":
                    EditorDialogs.mergeGeometries(ActionPrefix.mergeGeometries);
                    break;

                case "Mesh tool":
                    EditorTools.select("mesh");
                    break;

                case "Overrides tool":
                    EditorTools.select("overrides");
                    break;

                case "Reparent spatials":
                    EditorDialogs.reparentSpatials();
                    break;

                case "Rotate tool":
                    EditorTools.select("spatialRotation");
                    break;

                case "Scale tool":
                    EditorTools.select("spatialScale");
                    break;

                case "Select":
                    select();
                    break;

                case "Split geometry":
                    target.copyAndSplitGeometry();
                    break;

                case "Tool":
                    EditorTools.select("spatial");
                    break;

                case "Translate tool":
                    EditorTools.select("spatialTranslation");
                    break;

                case "User-Data tool":
                    EditorTools.select("userData");
                    break;

                default:
                    handled = false;
            }
        }

        return handled;
    }

    /**
     * Handle a "select spatial" action with an argument.
     *
     * @param argument action argument (not null)
     * @param subset which kinds of spatials to include (not null)
     */
    public static void selectSpatial(String argument, WhichSpatials subset) {
        Cgm target = Maud.getModel().getTarget();
        if (target.hasSpatial(argument)) {
            target.getSpatial().select(argument);
        } else {
            // Treat the argument as a spatial-name prefix.
            List<String> names = target.listSpatialNames(argument, subset);
            showSpatialSubmenu(names, subset);
        }
    }

    /**
     * Handle a "select spatialChild" action.
     *
     * @param itemPrefix prefix for filtering menu items (not null)
     */
    public static void selectSpatialChild(String itemPrefix) {
        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        int numChildren = spatial.countChildren();
        if (numChildren == 1) {
            spatial.selectChild(0);

        } else if (numChildren > 1) {
            List<String> children = spatial.listNumberedChildren();
            List<String> choices
                    = MyString.addMatchPrefix(children, itemPrefix, null);
            MyString.reduce(choices, ShowMenus.maxItems);
            Collections.sort(choices);

            MenuBuilder builder = new MenuBuilder();
            for (String choice : choices) {
                int childIndex = children.indexOf(choice);
                if (childIndex >= 0) {
                    boolean isANode = spatial.isChildANode(childIndex);
                    if (isANode) {
                        builder.addNode(choice);
                    } else {
                        builder.addGeometry(choice);
                    }
                } else {
                    builder.addEllipsis(choice);
                }
            }
            builder.show(ActionPrefix.selectSpatialChild);
        }
    }

    /**
     * Handle a "select spatialSibling" action.
     *
     * @param itemPrefix prefix for filtering menu items (not null)
     */
    public static void selectSpatialSibling(String itemPrefix) {
        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        int numSiblings = spatial.countSiblings(); // count includes self
        assert numSiblings > 1 : numSiblings;

        List<String> siblings = spatial.listNumberedSiblings();
        List<String> choices
                = MyString.addMatchPrefix(siblings, itemPrefix, null);
        MyString.reduce(choices, ShowMenus.maxItems);
        Collections.sort(choices);

        MenuBuilder builder = new MenuBuilder();
        for (String choice : choices) {
            int siblingIndex = siblings.indexOf(choice);
            if (siblingIndex >= 0) {
                boolean isANode = spatial.isSiblingANode(siblingIndex);
                if (isANode) {
                    builder.addNode(choice);
                } else {
                    builder.addGeometry(choice);
                }
            } else {
                builder.addEllipsis(choice);
            }
        }
        builder.show(ActionPrefix.selectSpatialSibling);
    }
    // *************************************************************************
    // private methods

    /**
     * Display a "Spatial -> Add new" menu.
     */
    private static void addNew() {
        MenuBuilder builder = new MenuBuilder();

        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        if (spatial.isNode()) {
            builder.addDialog("Leaf node");
        }
        if (spatial.countChildren() > 1) {
            builder.addDialog("Merged geometry");
        }
        builder.addDialog("Parent");

        builder.show("select menuItem Spatial -> Add new -> ");
    }

    /**
     * Handle a "select menuItem" action from the "Spatial -> Edit material"
     * menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private static boolean menuEditMaterial(String remainder) {
        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        boolean handled = true;
        switch (remainder) {
            case "Clone":
                spatial.cloneMaterial();
                break;

            case "Replace with debug":
                spatial.applyDebugMaterial();
                break;

            case "Replace with lit":
                spatial.applyLitMaterial();
                break;

            case "Replace with unshaded":
                spatial.applyUnshadedMaterial();
                break;

            default:
                handled = false;
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the "Spatial -> Add new" menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private static boolean menuSpatialAdd(String remainder) {
        boolean handled = true;
        switch (remainder) {
            case "Leaf node":
                EditorDialogs.newNode(ActionPrefix.newLeafNode);
                break;

            case "Merged geometry":
                EditorDialogs.mergeGeometries(
                        ActionPrefix.newGeometryFromMerge);
                break;

            case "Parent":
                EditorDialogs.newNode(ActionPrefix.newParent);
                break;

            default:
                handled = false;
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the "Spatial -> Select" menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private static boolean menuSpatialSelect(String remainder) {
        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        boolean handled = true;
        switch (remainder) {
            case "Attachments node":
                selectAttachmentsNode();
                break;

            case "By name":
                selectSpatial("", WhichSpatials.All);
                break;

            case "Child":
                selectSpatialChild("");
                break;

            case "Geometry":
                selectSpatial("", WhichSpatials.Geometries);
                break;

            case "Parent":
                spatial.selectParent();
                break;

            case "Root":
                spatial.selectCgmRoot();
                break;

            default:
                handled = false;
        }

        return handled;
    }

    /**
     * Display a "Spatial -> Select" menu.
     */
    private static void select() {
        MenuBuilder builder = new MenuBuilder();

        Cgm target = Maud.getModel().getTarget();
        List<String> names = target.listSpatialNames("", WhichSpatials.All);
        if (!names.isEmpty()) {
            builder.addSubmenu("By name");
        }

        boolean isRootANode = target.isRootANode();
        if (isRootANode) {
            builder.addNode("Root");
        } else {
            builder.addGeometry("Root");
        }

        if (target.hasAttachmentsNode()) {
            builder.addNode("Attachments node");
        }

        names = target.listSpatialNames("", WhichSpatials.Geometries);
        if (!names.isEmpty()) {
            builder.addSubmenu("Geometry");
        }

        int numChildren = target.getSpatial().countChildren();
        if (numChildren == 1) {
            boolean isChildANode = target.getSpatial().isChildANode(0);
            if (isChildANode) {
                builder.addNode("Child");
            } else {
                builder.addGeometry("Child");
            }
        } else if (numChildren > 1) {
            builder.addSubmenu("Child");
        }

        boolean isRoot = target.getSpatial().isCgmRoot();
        if (!isRoot) {
            builder.addNode("Parent");
        }

        builder.show("select menuItem Spatial -> Select -> ");
    }

    /**
     * Handle the "Spatial -> Select -> Attachments node" menu item.
     */
    private static void selectAttachmentsNode() {
        Cgm target = Maud.getModel().getTarget();
        SelectedBone bone = target.getBone();
        if (bone.hasAttachmentsNode()) {
            SelectedSpatial spatial = target.getSpatial();
            spatial.selectAttachmentsNode();
        } else {
            selectSpatial("", WhichSpatials.AttachmentsNodes);
        }
    }

    /**
     * Display a submenu for selecting spatials by name using the "select
     * spatial" action prefix.
     *
     * @param nameList list of names from which to select (not null)
     * @param subset which kinds of spatials to include (not null)
     */
    private static void showSpatialSubmenu(List<String> nameList,
            WhichSpatials subset) {
        assert nameList != null;
        assert subset != null;

        MyString.reduce(nameList, ShowMenus.maxItems);
        Collections.sort(nameList);

        MenuBuilder builder = new MenuBuilder();
        Cgm target = Maud.getModel().getTarget();
        for (String name : nameList) {
            switch (subset) {
                case All:
                    break;

                case AttachmentsNodes:
                    if (!target.hasAttachmentsNode(name)) {
                        continue;
                    }
                    break;

                case Geometries:
                    if (!target.hasGeometry(name)) {
                        continue;
                    }
                    break;

                default:
                    throw new IllegalArgumentException("subset = " + subset);
            }

            if (target.hasGeometry(name)) {
                builder.addGeometry(name);
            } else if (target.hasNode(name)) {
                builder.addNode(name);
            } else {
                builder.addEllipsis(name);
            }
        }
        builder.show(ActionPrefix.selectSpatial + subset + " ");
    }
}
