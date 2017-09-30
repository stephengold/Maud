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

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyString;
import maud.Maud;
import maud.dialog.EditorDialogs;
import maud.model.Cgm;
import maud.model.EditableCgm;
import maud.model.EditableMap;
import maud.model.History;
import maud.model.SelectedShape;
import maud.model.SelectedSpatial;

/**
 * Menus in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class EditorMenus {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(EditorMenus.class.getName());
    /**
     * magic filename used in "add locator" actions and menus
     */
    final static String addThis = "! add this folder";
    /**
     * level separator in menu paths
     */
    final static String menuPathSeparator = " -> ";
    // *************************************************************************
    // new methods exposed

    /**
     * Generate a map from subfolder names (with the specified path prefix) to
     * file objects.
     *
     * @param pathPrefix the file path prefix (not null)
     * @return a new map of subfolders
     */
    static Map<String, File> folderMap(String pathPrefix) {
        Map<String, File> result = new TreeMap<>();
        String namePrefix;
        File file = new File(pathPrefix);
        if (file.isDirectory()) {
            result.put(addThis, file);
            namePrefix = "";
        } else {
            namePrefix = file.getName();
            file = file.getParentFile();
            assert file.isDirectory();
        }

        File[] files = file.listFiles();
        for (File f : files) {
            String name = f.getName();
            if (name.startsWith(namePrefix)) {
                if (f.isDirectory() || name.endsWith(".jar")
                        || name.endsWith(".zip")) {
                    File oldFile = result.put(name, f);
                    assert oldFile == null : oldFile;
                }
            }
        }

        File parent = file.getParentFile();
        if (parent != null) {
            if ("..".startsWith(namePrefix)) {
                File oldFile = result.put("..", parent);
                assert oldFile == null : oldFile;
            }
        }

        return result;
    }

    /**
     * Handle a "select menuItem" action for the editor screen.
     *
     * @param menuPath path to menu item (not null)
     * @return true if the action is handled, otherwise false
     */
    public boolean selectMenuItem(String menuPath) {
        boolean handled;
        int separatorBegin = menuPath.indexOf(menuPathSeparator);
        if (separatorBegin == -1) {
            handled = Maud.gui.buildMenus.menuBar(menuPath);
        } else {
            int separatorEnd = separatorBegin + menuPathSeparator.length();
            String menuName = menuPath.substring(0, separatorBegin);
            String remainder = menuPath.substring(separatorEnd);
            handled = menu(menuName, remainder);
        }

        return handled;
    }

    /**
     * Handle a "select shapeChild" action without arguments.
     */
    public void selectShapeChild() {
        Cgm target = Maud.getModel().getTarget();
        SelectedShape shape = target.getShape();
        int numChildren = shape.countChildren();
        if (numChildren == 1) {
            shape.selectFirstChild();
        } else if (numChildren > 1) {
            Maud.gui.showMenus.selectShapeChild();
        }
    }

    /**
     * Handle a "select spatial" action with an argument.
     *
     * @param argument action argument (not null)
     * @param includeNodes true &rarr; include both nodes and geometries, false
     * &rarr; include geometries only
     */
    public void selectSpatial(String argument, boolean includeNodes) {
        Cgm target = Maud.getModel().getTarget();
        if (target.hasSpatial(argument)) {
            target.getSpatial().select(argument);

        } else {
            /*
             * Treat the argument as a spatial-name prefix.
             */
            List<String> names;
            names = target.listSpatialNames(argument, includeNodes);
            Maud.gui.showMenus.showSpatialSubmenu(names, includeNodes);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Handle a "select menuItem" action for a submenu.
     *
     * @param menuName name of the top-level menu (not null)
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menu(String menuName, String remainder) {
        assert menuName != null;
        assert remainder != null;

        boolean handled = false;
        switch (menuName) {
            case "Animation":
                handled = AnimationMenus.menuAnimation(remainder);
                break;
            case "Bone":
                handled = BoneMenus.menuBone(remainder);
                break;
            case "CGM":
                handled = menuCgm(remainder);
                break;
            case "Help":
                handled = menuHelp(remainder);
                break;
            case "History":
                handled = menuHistory(remainder);
                break;
            case "Keyframe":
                handled = menuKeyframe(remainder);
                break;
            case "Map":
                handled = menuMap(remainder);
                break;
            case "Physics":
                handled = menuPhysics(remainder);
                break;
            case "Settings":
                handled = menuSettings(remainder);
                break;
            case "SGC":
                handled = menuSgc(remainder);
                break;
            case "Spatial":
                handled = menuSpatial(remainder);
                break;
            case "Track":
                handled = menuTrack(remainder);
                break;
            case "Vertex":
                handled = menuVertex(remainder);
                break;
            case "View":
                handled = ViewMenus.menuView(remainder);
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the CGM menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuCgm(String remainder) {
        boolean handled = true;
        String sourcePrefix = "Source model" + EditorMenus.menuPathSeparator;
        if (remainder.startsWith(sourcePrefix)) {
            String selectArg = MyString.remainder(remainder, sourcePrefix);
            handled = menuSourceCgm(selectArg);

        } else {
            switch (remainder) {
                case "History":
                    Maud.gui.tools.select("history");
                    break;
                case "Load":
                    Maud.gui.buildMenus.loadCgm();
                    break;
                case "Save":
                    EditorDialogs.saveCgm();
                    break;
                case "Source model":
                    Maud.gui.showMenus.sourceCgm();
                    break;
                case "Tool":
                    Maud.gui.tools.select("cgm");
                    break;
                default:
                    handled = false;
            }
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the Help menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuHelp(String remainder) {
        boolean handled = true;
        switch (remainder) {
            case "About Maud":
                EditorDialogs.aboutMaud();
                break;
            case "JME3 homepage":
                Misc.browseWeb("http://jmonkeyengine.org/");
                break;
            case "License":
                Maud.gui.showMenus.viewLicense();
                break;
            case "Source":
                Misc.browseWeb("https://github.com/stephengold/Maud");
                break;
            default:
                handled = false;
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the History menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuHistory(String remainder) {
        boolean handled = true;
        switch (remainder) {
            case "Clear":
                History.clear();
                break;
            case "Tool":
                Maud.gui.tools.select("history");
                break;
            default:
                handled = false;
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the Keyframe menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuKeyframe(String remainder) {
        boolean handled = true;
        String selectPrefix = "Select" + menuPathSeparator;
        if (remainder.startsWith(selectPrefix)) {
            String arg = MyString.remainder(remainder, selectPrefix);
            handled = menuKeyframeSelect(arg);

        } else {
            EditableCgm target = Maud.getModel().getTarget();
            switch (remainder) {
                case "Delete next":
                    EditorDialogs.deleteNextKeyframes();
                    break;
                case "Delete previous":
                    EditorDialogs.deletePreviousKeyframes();
                    break;
                case "Delete selected":
                    target.getTrack().deleteSelectedKeyframe();
                    break;
                case "Insert from pose":
                    target.getTrack().insertKeyframe();
                    break;
                case "Replace with pose":
                    target.getTrack().replaceKeyframe();
                    break;
                case "Select":
                    Maud.gui.showMenus.selectKeyframe();
                    break;
                case "Tool":
                    Maud.gui.tools.select("keyframe");
                    break;
                default:
                    handled = false;
            }
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the "Keyframe -> Select" menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuKeyframeSelect(String remainder) {
        boolean handled = true;
        Cgm target = Maud.getModel().getTarget();
        switch (remainder) {
            case "First":
                target.getTrack().selectFirstKeyframe();
                break;
            case "Last":
                target.getTrack().selectLastKeyframe();
                break;
            case "Nearest":
                target.getTrack().selectNearestKeyframe();
                break;
            case "Next":
                target.getTrack().selectNextKeyframe();
                break;
            case "Previous":
                target.getTrack().selectPreviousKeyframe();
                break;
            default:
                handled = false;
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the Map menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuMap(String remainder) {
        boolean handled = true;
        EditableMap map = Maud.getModel().getMap();
        switch (remainder) {
            case "Delete invalid mappings":
                map.deleteInvalidMappings();
                break;
            case "Invert":
                map.invert();
                break;
            case "Load":
                Maud.gui.buildMenus.loadMapAsset();
                break;
            case "Save":
                EditorDialogs.saveMap();
                break;
            case "Tool":
                Maud.gui.tools.select("map");
                break;
            case "Twist tool":
                Maud.gui.tools.select("twist");
                break;
            case "Unload":
                map.unload();
                break;
            default:
                handled = false;
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the Physics menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuPhysics(String remainder) {
        boolean handled = true;
        String addPrefix = "Add new" + menuPathSeparator;
        if (remainder.startsWith(addPrefix)) {
            String arg = MyString.remainder(remainder, addPrefix);
            handled = menuPhysicsAdd(arg);

        } else {
            switch (remainder) {
                case "Joint Tool":
                    Maud.gui.tools.select("joint");
                    break;
                case "Mass":
                    EditorDialogs.setPhysicsMass();
                    break;
                case "Object Tool":
                    Maud.gui.tools.select("physics");
                    break;
                case "Shape Tool":
                    Maud.gui.tools.select("shape");
                    break;
                default:
                    handled = false;
            }
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the "Physics -> Add new" menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuPhysicsAdd(String remainder) {
        boolean handled = true;
        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        switch (remainder) {
            case "Ghost":
                spatial.addGhostControl();
                break;
            case "RigidBody":
                spatial.addRigidBodyControl();
                break;
            default:
                handled = false;
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the Settings menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuSettings(String remainder) {
        boolean handled = true;
        String folderPrefix = "Asset locations" + menuPathSeparator;
        if (remainder.startsWith(folderPrefix)) {
            String selectArg = MyString.remainder(remainder, folderPrefix);
            handled = Maud.gui.buildMenus.menuAssetLocations(selectArg);

        } else {
            switch (remainder) {
                case "Asset locations":
                    Maud.gui.showMenus.assetLocations();
                    break;
                case "Hotkeys":
                    Maud.gui.goBindScreen();
                    break;
                case "Tool":
                    Maud.gui.tools.select("settings");
                    break;
                default:
                    handled = false;
            }
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the SGC menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuSgc(String remainder) {
        boolean handled = true;
        String addPrefix = "Add new" + menuPathSeparator;
        if (remainder.startsWith(addPrefix)) {
            String arg = MyString.remainder(remainder, addPrefix);
            handled = menuSgcAdd(arg);

        } else {
            switch (remainder) {
                case "Add new":
                    Maud.gui.showMenus.addNewSgc();
                    break;
                case "Delete":
                    EditorDialogs.deleteSgc();
                    break;
                case "Select":
                    Maud.gui.showMenus.selectSgc();
                    break;
                case "Tool":
                    Maud.gui.tools.select("sgc");
                    break;
                default:
                    handled = false;
            }
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the "SGC -> Add new" menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuSgcAdd(String remainder) {
        boolean handled = true;
        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        switch (remainder) {
            case "Anim":
                spatial.addAnimControl();
                break;
            case "Ghost":
                spatial.addGhostControl();
                break;
            case "RigidBody":
                spatial.addRigidBodyControl();
                break;
            case "Skeleton":
                spatial.addSkeletonControl();
                break;
            default:
                handled = false;
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the "CGM -> Source model" menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuSourceCgm(String remainder) {
        boolean handled = false;
        switch (remainder) {
            case "Load":
                Maud.gui.buildMenus.loadSourceCgm();
                handled = true;
                break;

            case "Unload":
                Maud.getModel().getSource().unload();
                handled = true;
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the Spatial menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuSpatial(String remainder) {
        boolean handled = true;
        String selectPrefix = "Select" + menuPathSeparator;
        if (remainder.startsWith(selectPrefix)) {
            String arg = MyString.remainder(remainder, selectPrefix);
            handled = menuSpatialSelect(arg);

        } else {
            switch (remainder) {
                case "Delete":
                    Maud.getModel().getTarget().getSpatial().delete();
                    break;
                case "Delete extras":
                    Maud.getModel().getTarget().deleteExtraSpatials();
                    break;
                case "Details":
                    Maud.gui.tools.select("spatialDetails");
                    break;
                case "Rotate":
                    Maud.gui.tools.select("spatialRotation");
                    break;
                case "Scale":
                    Maud.gui.tools.select("spatialScale");
                    break;
                case "Select":
                    Maud.gui.showMenus.selectSpatial();
                    break;
                case "Tool":
                    Maud.gui.tools.select("spatial");
                    break;
                case "Translate":
                    Maud.gui.tools.select("spatialTranslation");
                    break;
                case "User data tool":
                    Maud.gui.tools.select("userData");
                    break;
                default:
                    handled = false;
            }
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the "Spatial -> Select" menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuSpatialSelect(String remainder) {
        boolean handled = true;
        switch (remainder) {
            case "By name":
                selectSpatial("", true);
                break;
            case "Child":
                Maud.gui.showMenus.selectSpatialChild("");
                break;
            case "Geometry":
                selectSpatial("", false);
                break;
            case "Parent":
                Maud.getModel().getTarget().getSpatial().selectParent();
                break;
            case "Root":
                Maud.getModel().getTarget().getSpatial().selectCgmRoot();
                break;
            default:
                handled = false;
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the Track menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuTrack(String remainder) {
        boolean handled = true;
        EditableCgm target = Maud.getModel().getTarget();
        switch (remainder) {
            case "Create":
                target.getAnimation().createBoneTrack();
                break;
            case "Delete":
                target.getAnimation().deleteTrack();
                break;
            case "Load animation":
                AnimationMenus.loadAnimation(target);
                break;
            case "Reduce":
                EditorDialogs.reduceTrack();
                break;
            case "Resample at rate":
                EditorDialogs.resampleTrack(true);
                break;
            case "Resample to number":
                EditorDialogs.resampleTrack(false);
                break;
            case "Select bone":
                Maud.gui.buildMenus.selectBone();
                break;
            case "Smooth":
                target.getTrack().smooth();
                break;
            case "Tool":
                Maud.gui.tools.select("keyframe"); // shared with Keyframe menu
                break;
            case "Translate for support":
                target.getTrack().translateForSupport();
                break;
            case "Translate for traction":
                target.getTrack().translateForTraction();
                break;
            case "Wrap":
                target.getTrack().wrap();
                break;
            default:
                handled = false;
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the Vertex menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuVertex(String remainder) {
        boolean handled = true;
        String selectPrefix = "Select" + menuPathSeparator;
        if (remainder.startsWith(selectPrefix)) {
            String arg = MyString.remainder(remainder, selectPrefix);
            handled = menuVertexSelect(arg);

        } else {
            switch (remainder) {
                case "Select":
                    Maud.gui.showMenus.selectVertex();
                    break;
                case "Select geometry":
                    selectSpatial("", false);
                    break;
                case "Tool":
                    Maud.gui.tools.select("vertex");
                    break;
                default:
                    handled = false;
            }
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the "Vertex -> Select" menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuVertexSelect(String remainder) {
        boolean handled = true;
        switch (remainder) {
            case "By index":
                EditorDialogs.selectVertex();
                break;
            //case "Extreme": TODO
            //case "Neighbor": TODO
            case "Next":
                Maud.getModel().getTarget().getVertex().selectNext();
                break;
            case "Previous":
                Maud.getModel().getTarget().getVertex().selectPrevious();
                break;
            default:
                handled = false;
        }

        return handled;
    }
}
