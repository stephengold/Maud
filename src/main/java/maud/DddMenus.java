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
    // fields

    /**
     * builder for popup menus
     */
    final private static MenuBuilder builder = new MenuBuilder();
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
        boolean handled;
        int separatorBegin = menuPath.indexOf(menuSeparator);
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
     * Handle a "select bone" action with an argument.
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
                Maud.model.bone.selectFirstChild();
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
     * Build the Animation menu.
     */
    private void buildAnimationMenu() {
        builder.addTool("Tool");
        builder.add("Load");
        builder.addDialog("New from copy");
        builder.addDialog("New from pose");
        builder.addTool("New from retarget");
        if (!Maud.model.animation.isBindPoseLoaded()) {
            builder.add("Duration");
            builder.add("Tweening");
            builder.addDialog("Rename");
            builder.add("Delete");
        }
    }

    /**
     * Build the Bone menu.
     */
    private void buildBoneMenu() {
        builder.addTool("Tool");
        builder.add("Select");
        builder.addTool("Rotate");
        builder.addTool("Scale");
        builder.addTool("Translate");
        if (Maud.model.bone.isBoneSelected()) {
            builder.add("Attach prop");
            builder.addDialog("Rename");
        }
    }

    /**
     * Build the Bone -> Select menu.
     */
    private void buildBoneSelectMenu() {
        builder.add("By name");
        builder.add("By parent");
        builder.add("Root");

        int numTracks = Maud.model.animation.countBoneTracks();
        if (numTracks > 0) {
            builder.add("With track");
        }

        int numChildren = Maud.model.bone.countChildren();
        if (numChildren > 0) {
            builder.add("Child");
        }

        boolean isRoot = Maud.model.bone.isRootBone();
        if (!isRoot) {
            builder.add("Parent");
        }

        if (Maud.model.bone.isBoneSelected()) {
            builder.add("Next");
            builder.add("Previous");
        }
    }

    /**
     * Build the CGModel menu.
     */
    private void buildCGModelMenu() {
        builder.addTool("Tool");
        builder.add("Load named asset");
        builder.addDialog("Load asset path");
        builder.add("Load from file");
        builder.addDialog("Save as asset");
        builder.addDialog("Save as file");
    }

    /**
     * Build the Help menu.
     */
    private void buildHelpMenu() {
        builder.addDialog("About Maud");
        builder.addDialog("License");
        builder.add("Wiki");
        builder.add("Javadoc");
        builder.add("Source");
        builder.add("JME3 homepage");
    }

    /**
     * Build the Keyframe menu.
     */
    private void buildKeyframeMenu() {
        builder.addTool("Tool");
        builder.add("Select by time");
        builder.add("Select first");
        builder.add("Select previous");
        builder.add("Select next");
        builder.add("Select last");
        builder.add("Move");
        builder.add("Copy");
        builder.add("New from pose");
        builder.add("Delete");
    }

    /**
     * Build the Physics menu.
     */
    private void buildPhysicsMenu() {
        builder.addTool("Describe");
        builder.add("Add");
        builder.add("Mass");
        builder.add("Remove");
    }

    /**
     * Build the Settings menu.
     */
    private void buildSettingsMenu() {
        builder.add("Initial model");
        builder.add("Hotkeys");
        builder.add("Locale");
    }

    /**
     * Build the Spatial menu.
     */
    private void buildSpatialMenu() {
        builder.addTool("Tool");
        builder.add("Select by parent");
        builder.add("Select by name");
        builder.addTool("Rotate");
        builder.addTool("Scale");
        builder.addTool("Translate");
        builder.addTool("Material");
    }

    /**
     * Build the View menu.
     */
    private void buildViewMenu() {
        builder.addTool("Axes");
        builder.addTool("Camera");
        builder.addTool("Cursor");
        builder.addTool("Physics");
        builder.addTool("Platform");
        builder.addTool("Render");
        builder.addTool("Skeleton");
        builder.addTool("Sky");
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
            case "Bone -> Select":
                handled = menuBoneSelect(remainder);
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
                break;
            case "Platform":
                Maud.gui.platform.select();
                handled = true;
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
                Maud.gui.dialogs.copyAnimation();
                handled = true;
                break;

            case "New from pose":
                Maud.gui.dialogs.newPose();
                handled = true;
                break;

            case "New from retarget":
                Maud.gui.retarget.select();
                handled = true;
                break;

            case "Rename":
                Maud.gui.dialogs.renameAnimation();
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
        builder.reset();
        switch (menuName) {
            case "3DView":
                buildViewMenu();
                break;
            case "Animation":
                buildAnimationMenu();
                break;
            case "Bone":
                buildBoneMenu();
                break;
            case "CGModel":
                buildCGModelMenu();
                break;
            case "Help":
                buildHelpMenu();
                break;
            case "Keyframe":
                buildKeyframeMenu();
                break;
            case "Physics":
                buildPhysicsMenu();
                break;
            case "Settings":
                buildSettingsMenu();
                break;
            case "Spatial":
                buildSpatialMenu();
                break;
            default:
                return false;
        }
        if (builder.isEmpty()) {
            logger.log(Level.WARNING, "no items for the {0} menu",
                    MyString.quote(menuName));
        } else {
            String actionPrefix = DddInputMode.openMenuPrefix + menuName
                    + menuSeparator;
            String[] items = builder.copyItems();
            String[] icons = builder.copyIcons();
            Maud.gui.showPopupMenu(actionPrefix, items, icons);
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
        String selectPrefix = "Select" + menuSeparator;
        if (remainder.startsWith(selectPrefix)) {
            String selectArg = MyString.remainder(remainder, selectPrefix);
            handled = menuBoneSelect(selectArg);

        } else {
            switch (remainder) {
                case "Attach prop":
                    break;
                case "Rename":
                    Maud.gui.dialogs.renameBone();
                    handled = true;
                    break;
                case "Rotate":
                    Maud.gui.boneRotation.select();
                    handled = true;
                    break;
                case "Scale":
                    Maud.gui.boneScale.select();
                    handled = true;
                    break;
                case "Select":
                    selectBone();
                    handled = true;
                    break;
                case "Tool":
                    Maud.gui.bone.select();
                    handled = true;
                    break;
                case "Translate":
                    Maud.gui.boneTranslation.select();
                    handled = true;
            }
        }
        return handled;
    }

    /**
     * Handle actions from the Bone -> Select menu.
     *
     * @param remainder not-yet-parsed portion of the action string (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuBoneSelect(String remainder) {
        assert remainder != null;

        boolean handled = false;
        switch (remainder) {
            case "By name":
                selectBoneByName();
                handled = true;
                break;
            case "By parent":
                selectBoneByParent();
                handled = true;
                break;
            case "Child":
                selectBoneChild();
                handled = true;
                break;
            case "Next":
                Maud.model.bone.selectNext();
                handled = true;
                break;
            case "Parent":
                Maud.model.bone.selectParent();
                handled = true;
                break;
            case "Previous":
                Maud.model.bone.selectPrevious();
                handled = true;
                break;
            case "Root":
                selectRootBone();
                handled = true;
                break;
            case "With track":
                selectBoneWithTrack();
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
                Maud.gui.dialogs.loadModelAsset();
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
                Maud.gui.dialogs.aboutMaud();
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
            case "Rotate":
                Maud.gui.spatialRotation.select();
                handled = true;
                break;
            case "Scale":
                Maud.gui.spatialScale.select();
                handled = true;
                break;
            case "Tool":
                Maud.gui.spatial.select();
                handled = true;
                break;
            case "Translate":
                Maud.gui.spatialTranslation.select();
                handled = true;
        }

        return handled;
    }

    /**
     * Handle a "select bone" action without an argument.
     */
    private void selectBone() {
        String prefix = "open menu Bone -> Select -> ";
        builder.reset();
        buildBoneSelectMenu();
        String[] items = builder.copyItems();
        String[] icons = builder.copyIcons();
        Maud.gui.showPopupMenu(prefix, items, icons);
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

    /**
     * Handle a "select rootBone" action.
     */
    private void selectRootBone() {
        int numRoots = Maud.model.cgm.countRootBones();
        if (numRoots == 1) {
            Maud.model.bone.selectFirstRoot();
        } else if (numRoots > 1) {
            List<String> choices = Maud.model.cgm.listRootBoneNames();
            Maud.gui.showPopupMenu(DddInputMode.selectBonePrefix, choices);
        }
    }
}
