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
package maud.menu;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyString;
import maud.Maud;
import maud.action.ActionPrefix;
import maud.dialog.EditorDialogs;
import maud.model.cgm.Cgm;
import maud.model.cgm.SelectedBone;
import maud.model.cgm.SelectedSpatial;

/**
 * Spatial menus in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SpatialMenus {
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
     * Handle a "select menuItem" action from the Spatial menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    static boolean menuSpatial(String remainder) {
        boolean handled = true;
        String addPrefix = "Add new" + EditorMenus.menuPathSeparator;
        String selectPrefix = "Select" + EditorMenus.menuPathSeparator;
        if (remainder.startsWith(addPrefix)) {
            String arg = MyString.remainder(remainder, addPrefix);
            handled = menuSpatialAdd(arg);

        } else if (remainder.startsWith(selectPrefix)) {
            String arg = MyString.remainder(remainder, selectPrefix);
            handled = menuSpatialSelect(arg);

        } else {
            switch (remainder) {
                case "Add new":
                    addNew();
                    break;

                case "Delete":
                    Maud.getModel().getTarget().getSpatial().delete();
                    break;

                case "Delete extras":
                    Maud.getModel().getTarget().deleteExtraSpatials();
                    break;

                case "Details":
                    Maud.gui.tools.select("spatialDetails");
                    break;

                case "Lights":
                    Maud.gui.tools.select("lights");
                    break;

                case "Material":
                    Maud.gui.tools.select("material");
                    break;

                case "Mesh":
                    Maud.gui.tools.select("mesh");
                    break;

                case "Overrides":
                    Maud.gui.tools.select("overrides");
                    break;

                case "Rotate":
                    Maud.gui.tools.select("spatialRotation");
                    break;

                case "Scale":
                    Maud.gui.tools.select("spatialScale");
                    break;

                case "Select":
                    select();
                    break;

                case "Tool":
                    Maud.gui.tools.select("spatial");
                    break;

                case "Translate":
                    Maud.gui.tools.select("spatialTranslation");
                    break;

                case "User-Data":
                    Maud.gui.tools.select("userData");
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
            /*
             * Treat the argument as a spatial-name prefix.
             */
            List<String> names = target.listSpatialNames(argument, subset);
            ShowMenus.showSpatialSubmenu(names, subset);
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
        builder.addDialog("Parent");

        builder.show("select menuItem Spatial -> Add new -> ");
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
}
