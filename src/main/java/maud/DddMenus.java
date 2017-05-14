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
package maud;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyString;

/**
 * Menus in Maud's "3D View" screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class DddMenus {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            DddMenus.class.getName());
    /**
     * level separator in menu action strings
     */
    final private static String menuSeparator = " -> ";
    // *************************************************************************
    // new methods exposed

    /**
     * Handle a "load model file" action where the argument may be the name of a
     * folder/directory.
     *
     * @param filePath action argument (not null)
     */
    void loadModelFile(String filePath) {
        File file = new File(filePath);
        if (file.isDirectory()) {
            List<String> fileNames = listFileNames(filePath);
            String menuPrefix = DddInputMode.loadModelFilePrefix + filePath;
            if (!menuPrefix.endsWith("/")) {
                menuPrefix += "/";
            }
            Maud.gui.showPopupMenu(menuPrefix, fileNames);

        } else if (file.canRead()) {
            Maud.model.cgm.loadModelFile(file);
        }
    }

    /**
     * Handle an "open menu" action for this screen.
     *
     * @param menuPath menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    boolean openMenu(String menuPath) {
        int separatorBegin = menuPath.indexOf(menuSeparator);
        boolean handled;
        if (separatorBegin == -1) {
            handled = menuBar(menuPath);
        } else {
            int separatorEnd = separatorBegin + menuSeparator.length();
            String menuName = menuPath.substring(0, separatorBegin);
            String remainder = menuPath.substring(separatorEnd);
            handled = menu(menuName, remainder);
        }

        return handled;
    }

    /**
     * Handle a "select bone" action.
     *
     * @param argument action argument (not null)
     */
    void selectBone(String argument) {
        if (Maud.model.cgm.hasBone(argument)) {
            Maud.model.bone.select(argument);

        } else {
            /*
             * Treat the argument as a bone-name prefix.
             */
            List<String> boneNames = Maud.model.cgm.listBoneNames(argument);
            MyString.reduce(boneNames, 20);
            Collections.sort(boneNames);
            Maud.gui.showPopupMenu(DddInputMode.selectBonePrefix, boneNames);
        }
    }

    /**
     * Handle a "select boneChild" action with no argument.
     */
    void selectBoneChild() {
        if (Maud.model.bone.isBoneSelected()) {
            int numChildren = Maud.model.bone.countChildren();
            if (numChildren == 1) {
                Maud.model.bone.selectChild(0);
            } else if (numChildren > 1) {
                List<String> choices = Maud.model.bone.listChildNames();
                Maud.gui.showPopupMenu(DddInputMode.selectBonePrefix, choices);
            }
        }
    }

    /**
     * Handle a "select boneChild" action with an argument.
     *
     * @param argument action argument (not null)
     */
    void selectBoneChild(String argument) {
        if (argument.startsWith("!")) {
            String name = argument.substring(1);
            Maud.model.bone.select(name);
        } else {
            List<String> names = Maud.model.cgm.listChildBoneNames(argument);
            List<String> items = new ArrayList<>(names.size() + 1);
            items.add("!" + argument);
            for (String name : names) {
                if (Maud.model.cgm.isLeafBone(name)) {
                    items.add("!" + name);
                } else {
                    items.add(name);
                }
            }
            Maud.gui.showPopupMenu(DddInputMode.selectBoneChildPrefix, items);
        }
    }

    /**
     * Handle a "select boneWithTrack" action.
     */
    void selectBoneWithTrack() {
        List<String> bonesWithTrack = Maud.model.animation.listBonesWithTrack();
        int numBoneTracks = bonesWithTrack.size();
        if (numBoneTracks == 1) {
            Maud.model.bone.select(bonesWithTrack.get(0));
        } else if (numBoneTracks > 1) {
            Maud.gui.showPopupMenu(DddInputMode.selectBonePrefix,
                    bonesWithTrack);
        }
    }

    /**
     * Handle a "select spatialChild" action with no argument.
     */
    void selectSpatialChild() {
        int numChildren = Maud.model.spatial.countChildren();
        if (numChildren == 1) {
            Maud.model.spatial.selectChild(0);

        } else if (numChildren > 1) {
            List<String> choices = new ArrayList<>(numChildren);
            for (int i = 0; i < numChildren; i++) {
                String choice = String.format("#%d", i + 1);
                String name = Maud.model.spatial.getChildName(i);
                if (name != null) {
                    choice += " " + MyString.quote(name);
                }
                choices.add(choice);
            }
            Maud.gui.showPopupMenu(DddInputMode.selectSpatialChildPrefix,
                    choices);
        }
    }

    /**
     * Handle a "select rsa" action with no argument.
     */
    void selectRetargetSourceAnimation() {
        List<String> names = Maud.model.retarget.listAnimationNames();
        if (!names.isEmpty()) {
            Collections.sort(names);
            Maud.gui.showPopupMenu(
                    DddInputMode.selectRetargetSourceAnimationPrefix, names);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Enumerate items for the 3D View menu.
     *
     * @return a new list
     */
    private List<String> list3DViewMenuItems() {
        List<String> items = new ArrayList<>(9);
        items.add("Axes");
        items.add("Camera");
        items.add("Cursor");
        items.add("Physics");
        items.add("Platform");
        items.add("Render");
        items.add("Skeleton");
        items.add("Sky");

        return items;
    }

    /**
     * Enumerate items for the Animation menu.
     *
     * @return a new list
     */
    private List<String> listAnimationMenuItems() {
        List<String> items = new ArrayList<>(10);
        items.add("Tool");
        items.add("Load");
        items.add("New from copy");
        items.add("New from pose");
        items.add("New from retarget");
        if (!Maud.model.animation.isBindPoseLoaded()) {
            items.add("Duration");
            items.add("Tweening");
            items.add("Rename");
            items.add("Delete");
        }

        return items;
    }

    /**
     * Enumerate items for the Bone menu.
     *
     * @return a new list
     */
    private List<String> listBoneMenuItems() {
        List<String> items = new ArrayList<>(10);
        items.add("Tool");
        items.add("Angles");
        items.add("Offset");
        items.add("Scale");
        items.add("Select by parent");
        items.add("Select by name");
        if (Maud.model.animation.countBoneTracks() > 0) {
            items.add("Select with track");
        }
        if (Maud.model.bone.isBoneSelected()) {
            items.add("Attach prop");
            items.add("Rename");
        }

        return items;
    }

    /**
     * Enumerate items for the CGModel menu.
     *
     * @return a new list
     */
    private List<String> listCGModelMenuItems() {
        List<String> items = new ArrayList<>(6);
        items.add("Tool");
        items.add("Load named asset");
        items.add("Load asset path");
        items.add("Load from file");
        items.add("Save as asset");
        items.add("Save as file");

        return items;
    }

    /**
     * Enumerate the files in a directory/folder.
     *
     * @param path file path (not null)
     * @return a new list, or null if path is not a directory/folder
     */
    private List<String> listFileNames(String path) {
        assert path != null;

        File file = new File(path);
        File[] files = file.listFiles();
        if (files == null) {
            return null;
        }

        List<String> names = new ArrayList<>(files.length + 1);
        if (file.getParentFile() != null) {
            names.add("..");
        }
        for (int i = 0; i < files.length; i++) {
            String name = files[i].getName();
            names.add(name);
        }

        return names;
    }

    /**
     * Enumerate items for the Help menu.
     *
     * @return a new list
     */
    private List<String> listHelpMenuItems() {
        List<String> items = new ArrayList<>(6);
        items.add("About Maud");
        items.add("License");
        items.add("Wiki");
        items.add("Javadoc");
        items.add("Source");
        items.add("JME3 homepage");

        return items;
    }

    /**
     * Enumerate items for the Keyframe menu.
     *
     * @return a new list
     */
    private List<String> listKeyframeMenuItems() {
        List<String> items = new ArrayList<>(10);
        items.add("Tool");
        items.add("Select by time");
        items.add("Select first");
        items.add("Select previous");
        items.add("Select next");
        items.add("Select last");
        items.add("Move");
        items.add("Copy");
        items.add("New from pose");
        items.add("Delete");

        return items;
    }

    /**
     * Enumerate items for the Physics menu.
     *
     * @return a new list
     */
    private List<String> listPhysicsMenuItems() {
        List<String> items = new ArrayList<>(4);
        items.add("Describe");
        items.add("Add");
        items.add("Mass");
        items.add("Remove");

        return items;
    }

    /**
     * Enumerate items for the Settings menu.
     *
     * @return a new list
     */
    private List<String> listSettingsMenuItems() {
        List<String> items = new ArrayList<>(3);
        items.add("Initial model");
        items.add("Hotkeys");
        items.add("Locale");

        return items;
    }

    /**
     * Enumerate items for the Spatial menu.
     *
     * @return a new list
     */
    private List<String> listSpatialMenuItems() {
        List<String> items = new ArrayList<>(4);
        items.add("Tool");
        items.add("Select by parent");
        items.add("Select by name");
        items.add("Material");

        return items;
    }

    /**
     * Handle a menu action.
     *
     * @param menuName name of the menu (not null)
     * @param remainder not-yet-parsed portion of the action string (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menu(String menuName, String remainder) {
        assert menuName != null;
        assert remainder != null;

        boolean handled = false;
        switch (menuName) {
            case "3DView":
                handled = menu3DView(remainder);
                break;
            case "Animation":
                handled = menuAnimation(remainder);
                break;
            case "Bone":
                handled = menuBone(remainder);
                break;
            case "CGModel":
                handled = menuCGModel(remainder);
                break;
            case "Help":
                handled = menuHelp(remainder);
                break;
            case "Keyframe":
                handled = menuKeyframe(remainder);
                break;
            case "Physics":
                //handled = menuPhysics(remainder);
                break;
            case "Settings":
                handled = menuSettings(remainder);
                break;
            case "Spatial":
                handled = menuSpatial(remainder);
                break;
        }

        return handled;
    }

    /**
     * Handle actions from the 3D View menu.
     *
     * @param remainder not-yet-parsed portion of the action string (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menu3DView(String remainder) {
        assert remainder != null;

        boolean handled = false;
        switch (remainder) {
            case "Axes":
                Maud.gui.axes.select();
                handled = true;
                break;
            case "Camera":
                Maud.gui.camera.select();
                handled = true;
                break;
            case "Cursor":
                Maud.gui.cursor.select();
                handled = true;
                break;
            case "Physics":
            case "Platform":
                break;
            case "Render":
                Maud.gui.render.select();
                handled = true;
                break;
            case "Skeleton":
                Maud.gui.skeleton.select();
                handled = true;
                break;
            case "Sky":
                Maud.gui.sky.select();
                handled = true;
                break;
        }

        return handled;
    }

    /**
     * Handle actions from the Animation menu.
     *
     * @param remainder not-yet-parsed portion of the action string (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuAnimation(String remainder) {
        assert remainder != null;

        boolean handled = false;
        switch (remainder) {
            case "Delete":
                // TODO confirm first
                Maud.gui.animation.delete();
                handled = true;
                break;

            case "Duration":
                // TODO
                break;

            case "Load":
                Collection<String> animationNames;
                animationNames = Maud.model.cgm.listAnimationNames();
                Maud.gui.showPopupMenu(DddInputMode.loadAnimationPrefix,
                        animationNames);
                handled = true;
                break;

            case "New from copy":
                Maud.gui.copyAnimation();
                handled = true;
                break;

            case "New from pose":
                Maud.gui.newPose();
                handled = true;
                break;

            case "New from retarget":
                Maud.gui.retarget.select();
                handled = true;
                break;

            case "Rename":
                Maud.gui.renameAnimation();
                handled = true;
                break;

            case "Tool":
                Maud.gui.animation.select();
                handled = true;
                break;

            case "Tweening":
        }
        return handled;
    }

    /**
     * Handle an action from the menu bar.
     *
     * @param menuName name of the menu to open (not null)
     * @return true if handled, otherwise false
     */
    private boolean menuBar(String menuName) {
        assert menuName != null;
        /**
         * Dynamically generate the menu's item list.
         */
        List<String> menuItems;
        switch (menuName) {
            case "3DView":
                menuItems = list3DViewMenuItems();
                break;
            case "Animation":
                menuItems = listAnimationMenuItems();
                break;
            case "Bone":
                menuItems = listBoneMenuItems();
                break;
            case "CGModel":
                menuItems = listCGModelMenuItems();
                break;
            case "Help":
                menuItems = listHelpMenuItems();
                break;
            case "Keyframe":
                menuItems = listKeyframeMenuItems();
                break;
            case "Physics":
                menuItems = listPhysicsMenuItems();
                break;
            case "Settings":
                menuItems = listSettingsMenuItems();
                break;
            case "Spatial":
                menuItems = listSpatialMenuItems();
                break;
            default:
                return false;
        }
        if (menuItems.isEmpty()) {
            logger.log(Level.WARNING, "no items for the {0} menu",
                    MyString.quote(menuName));
        } else {
            String actionPrefix = DddInputMode.openMenuPrefix + menuName
                    + menuSeparator;
            Maud.gui.showPopupMenu(actionPrefix, menuItems);
        }
        return true;
    }

    /**
     * Handle actions from the Bone menu.
     *
     * @param remainder not-yet-parsed portion of the action string (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuBone(String remainder) {
        assert remainder != null;
        boolean handled = false;
        switch (remainder) {
            case "Angles":
                Maud.gui.boneAngle.select();
                handled = true;
                break;
            case "Attach prop":
                break;
            case "Offset":
                Maud.gui.boneOffset.select();
                handled = true;
                break;
            case "Rename":
                Maud.gui.renameBone();
                handled = true;
                break;
            case "Scale":
                Maud.gui.boneScale.select();
                handled = true;
                break;
            case "Select by name":
                selectBoneByName();
                handled = true;
                break;
            case "Select by parent":
                selectBoneByParent();
                handled = true;
                break;
            case "Select with track":
                selectBoneWithTrack();
                handled = true;
                break;
            case "Tool":
                Maud.gui.bone.select();
                handled = true;
        }
        return handled;
    }

    /**
     * Handle actions from the CGModel menu.
     *
     * @param remainder not-yet-parsed portion of the action string (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuCGModel(String remainder) {
        assert remainder != null;

        boolean handled = false;
        switch (remainder) {
            case "Load named asset":
                String[] modelNames = {
                    "Elephant", "Jaime", "Ninja", "Oto", "Sinbad"
                };
                Maud.gui.showPopupMenu(DddInputMode.loadModelNamedPrefix,
                        modelNames);
                handled = true;
                break;

            case "Load asset path":
                Maud.gui.loadModelAsset();
                handled = true;
                break;

            case "Load from file":
                List<String> fileNames = listFileNames("/");
                Maud.gui.showPopupMenu(DddInputMode.loadModelFilePrefix + "/",
                        fileNames);
                handled = true;
                break;

            case "Revert":
                // TODO
                break;

            case "Save as asset":
                String baseAssetPath = Maud.model.cgm.getAssetPath();
                Maud.gui.closeAllPopups();
                Maud.gui.showTextEntryDialog("Enter base asset path for model:",
                        baseAssetPath, "Save",
                        DddInputMode.saveModelAssetPrefix, null);
                handled = true;
                break;

            case "Save as file":
                String baseFilePath = Maud.model.cgm.getFilePath();
                Maud.gui.closeAllPopups();
                Maud.gui.showTextEntryDialog("Enter base file path for model:",
                        baseFilePath, "Save", DddInputMode.saveModelFilePrefix,
                        null);
                handled = true;
                break;

            case "Tool":
                Maud.gui.model.select();
                handled = true;
                break;
        }
        return handled;
    }

    /**
     * Handle actions from the Help menu.
     *
     * @param remainder not-yet-parsed portion of the action string (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuHelp(String remainder) {
        assert remainder != null;
        boolean handled = false;
        switch (remainder) {
            case "About Maud":
                Maud.gui.aboutMaud();
                handled = true;
                break;

            case "JME3 homepage":
                Misc.browseWeb("http://jmonkeyengine.org/");
                handled = true;
                break;

            case "License":
                File licenseFile = new File("LICENSE");
                Scanner scanner = null;
                try {
                    scanner = new Scanner(licenseFile).useDelimiter("\\Z");
                } catch (FileNotFoundException e) {
                }
                String text2;
                if (scanner == null) {
                    text2 = "Your software license is missing!";
                } else {
                    String contents = scanner.next();
                    scanner.close();
                    text2 = String.format(
                            "Here's your software license for Maud:\n%s\n",
                            contents);
                }
                Maud.gui.closeAllPopups();
                Maud.gui.showInfoDialog("License information", text2);
                handled = true;
                break;

            case "Source":
                Misc.browseWeb("https://github.com/stephengold/Maud");
                handled = true;
                break;
        }

        return handled;
    }

    /**
     * Handle actions from the Keyframe menu.
     *
     * @param remainder not-yet-parsed portion of the action string (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuKeyframe(String remainder) {
        assert remainder != null;
        boolean handled = false;
        switch (remainder) {
            case "Select first":
                Maud.model.animation.selectKeyframeFirst();
                handled = true;
                break;
            case "Select previous":
                Maud.model.animation.selectKeyframePrevious();
                handled = true;
                break;
            case "Select next":
                Maud.model.animation.selectKeyframeNext();
                handled = true;
                break;
            case "Select last":
                Maud.model.animation.selectKeyframeLast();
                handled = true;
                break;
            case "Tool":
                Maud.gui.keyframe.select();
                handled = true;
        }

        return handled;
    }

    /**
     * Handle actions from the Settings menu.
     *
     * @param remainder not-yet-parsed portion of the action string (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuSettings(String remainder) {
        assert remainder != null;
        boolean handled = false;
        switch (remainder) {
            case "Hotkeys":
                Maud.gui.closeAllPopups();
                Maud.bindScreen.activate(Maud.gui.inputMode);
                handled = true;
        }

        return handled;
    }

    /**
     * Handle actions from the Spatial menu.
     *
     * @param remainder not-yet-parsed portion of the action string (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuSpatial(String remainder) {
        assert remainder != null;
        boolean handled = false;
        switch (remainder) {
            case "Tool":
                Maud.gui.spatial.select();
                handled = true;
        }

        return handled;
    }

    /**
     * Select a bone by name, using submenus.
     */
    private void selectBoneByName() {
        List<String> boneNames = Maud.model.cgm.listBoneNames();
        MyString.reduce(boneNames, 20);
        Collections.sort(boneNames);
        Maud.gui.showPopupMenu(DddInputMode.selectBonePrefix, boneNames);
    }

    /**
     * Select a bone by parent, using submenus.
     */
    private void selectBoneByParent() {
        List<String> boneNames = Maud.model.cgm.listRootBoneNames();
        Maud.gui.showPopupMenu(DddInputMode.selectBoneChildPrefix, boneNames);
    }
}
