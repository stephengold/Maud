/*
 Copyright (c) 2017-2021, Stephen Gold
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
import jme3utilities.Heart;
import maud.Maud;
import maud.action.ActionPrefix;
import maud.dialog.EditorDialogs;
import maud.model.EditableMap;
import maud.model.History;
import maud.model.cgm.CgmOutputFormat;
import maud.model.cgm.SelectedTexture;
import maud.tool.EditorTools;

/**
 * Menus in Maud's editor screen (utility class).
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
     * magic specifier for the default asset location in actions and menus
     */
    final static String defaultLocation = "from classpath";
    /**
     * magic specifier for a source-identity map in actions and menus
     */
    final static String identityForSource = "Identity for source";
    /**
     * magic specifier for a target-identity map in actions and menus
     */
    final static String identityForTarget = "Identity for target";
    /**
     * level separator in menu paths
     */
    final static String menuPathSeparator = " -> ";
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private EditorMenus() {
    }
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
     * Display a "load map asset" action without arguments.
     */
    public static void loadMapAsset() {
        MenuBuilder builder = newLocationMenu();
        if (Maud.getModel().getSource().getSkeleton().isSelected()) {
            builder.add(identityForSource);
        }
        if (Maud.getModel().getTarget().getSkeleton().isSelected()) {
            builder.add(identityForTarget);
        }
        builder.show(ActionPrefix.loadMapLocator);
    }

    /**
     * Handle a "load texture" action without arguments.
     */
    public static void loadTexture() {
        SelectedTexture texture = Maud.getModel().getTarget().getTexture();
        if (texture.hasKey()) {
            MenuBuilder builder = newLocationMenu();
            builder.show(ActionPrefix.loadTextureLocator);

        } else if (texture.hasImage()) { // keyless texture: add a key
            EditorDialogs.saveTexture();
        }
    }

    /**
     * Create a menu for selecting an asset location.
     *
     * @return a new menu builder (not null)
     */
    static MenuBuilder newLocationMenu() {
        MenuBuilder builder = new MenuBuilder();
        List<String> pathList = Maud.getModel().getLocations().listAll();
        for (String path : pathList) {
            if (path.endsWith(".jar")) {
                builder.addJar(path);
            } else if (path.endsWith(".zip")) {
                builder.addZip(path);
            } else {
                builder.addFolder(path);
            }
        }
        builder.addSubmenu(defaultLocation);

        return builder;
    }

    /**
     * Handle a "select menuItem" action for the editor screen.
     *
     * @param menuPath path to menu item (not null)
     * @return true if the action is handled, otherwise false
     */
    public static boolean selectMenuItem(String menuPath) {
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
    // *************************************************************************
    // private methods

    /**
     * Handle a "select menuItem" action for a submenu.
     *
     * @param menuName name of the top-level menu (not null)
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private static boolean menu(String menuName, String remainder) {
        assert menuName != null;
        assert remainder != null;

        boolean handled;
        switch (menuName) {
            case "Animation":
                handled = AnimationMenus.menuAnimation(remainder);
                break;

            case "Bone":
                handled = BoneMenus.menuBone(remainder);
                break;

            case "CGM":
                handled = CgmMenus.menuCgm(remainder);
                break;

            case "Help":
                handled = menuHelp(remainder);
                break;

            case "History":
                handled = menuHistory(remainder);
                break;

            case "Keyframe":
                handled = KeyframeMenus.menuKeyframe(remainder);
                break;

            case "Map":
                handled = menuMap(remainder);
                break;

            case "Material":
                handled = menuMaterial(remainder);
                break;

            case "Mesh":
                handled = MeshMenus.menuMesh(remainder);
                break;

            case "Physics":
                handled = PhysicsMenus.menuPhysics(remainder);
                break;

            case "Settings":
                handled = menuSettings(remainder);
                break;

            case "SGC":
                handled = SgcMenus.menuSgc(remainder);
                break;

            case "Spatial":
                handled = SpatialMenus.menuSpatial(remainder);
                break;

            case "Track":
                handled = AnimationMenus.menuTrack(remainder);
                break;

            case "View":
                handled = ViewMenus.menuView(remainder);
                break;

            default:
                handled = false;
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the Help menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private static boolean menuHelp(String remainder) {
        boolean handled = true;
        switch (remainder) {
            case "About Maud":
                EditorDialogs.aboutMaud();
                break;
            case "JME3 homepage":
                Heart.browseWeb("https://jmonkeyengine.org/");
                break;
            case "License":
                EnumMenus.viewLicense();
                break;
            case "Source":
                Heart.browseWeb("https://github.com/stephengold/Maud");
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
    private static boolean menuHistory(String remainder) {
        boolean handled = true;
        switch (remainder) {
            case "Clear":
                History.clear();
                break;
            case "Tool":
                EditorTools.select("history");
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
    private static boolean menuMap(String remainder) {
        boolean handled = true;
        EditableMap map = Maud.getModel().getMap();
        String actionPrefix;
        switch (remainder) {
            case "Delete invalid mappings":
                map.deleteInvalidMappings();
                break;
            case "Export to XML":
                actionPrefix = ActionPrefix.saveMapUnconfirmed
                        + CgmOutputFormat.XML.toString() + " ";
                EditorDialogs.saveMap("Export", actionPrefix);
                break;
            case "Invert":
                map.invert();
                break;
            case "Load":
                loadMapAsset();
                break;
            case "Save":
                actionPrefix = ActionPrefix.saveMapUnconfirmed
                        + CgmOutputFormat.J3O.toString() + " ";
                EditorDialogs.saveMap("Save", actionPrefix);
                break;
            case "Tool":
                EditorTools.select("mapping");
                break;
            case "Twist tool":
                EditorTools.select("twist");
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
     * Handle a "select menuItem" action from the Material menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private static boolean menuMaterial(String remainder) {
        boolean handled = true;
        switch (remainder) {
            case "Select":
                SpatialMenus.selectSpatial("", WhichSpatials.Geometries);
                break;

            case "Select texture":
                ShowMenus.selectTexture();
                break;

            case "Deselect texture":
                Maud.getModel().getTarget().getTexture().deselectAll();
                break;

            case "Texture tool":
                EditorTools.select("texture");
                break;

            case "Tool":
                EditorTools.select("material");
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
    private static boolean menuSettings(String remainder) {
        boolean handled = true;
        switch (remainder) {
            case "Add asset location":
                BuildMenus.addAssetLocation();
                break;

            case "Display settings":
                Maud.gui.goDisplaySettingsScreen();
                break;

            case "Dump tool":
                EditorTools.select("dump");
                break;

            case "Hotkeys":
                Maud.gui.goBindScreen();
                break;

            case "Remove asset location":
                ShowMenus.removeAssetLocation();
                break;

            case "Revert startup script to default":
                Maud.revertStartupScript();
                break;

            case "Scene-view options":
                ViewMenus.sceneViewOptions();
                break;

            case "Score-view options":
                ViewMenus.scoreViewOptions();
                break;

            case "Tool":
                EditorTools.select("settings");
                break;

            case "Tweening tool":
                EditorTools.select("tweening");
                break;

            case "Update startup script":
                Maud.getModel().updateStartupScript();
                break;

            default:
                handled = false;
        }

        return handled;
    }
}
