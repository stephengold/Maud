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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyString;
import maud.model.LoadedAnimation;
import maud.model.LoadedCgm;

/**
 * Menus in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class EditorMenus {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            EditorMenus.class.getName());
    /**
     * magic filename used in "add locator" menus
     */
    final static String addThis = "! add this folder";
    /**
     * level separator in menu action strings
     */
    final static String menuSeparator = " -> ";
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
     * Handle a "load (source)animation" action with an argument.
     *
     * @param argument action argument (not null)
     * @param cgm which load slot (not null)
     */
    void loadAnimation(String argument, LoadedCgm cgm) {
        if (cgm.hasAnimation(argument)
                || argument.equals(LoadedAnimation.bindPoseName)
                || argument.equals(LoadedAnimation.retargetedPoseName)) {
            cgm.animation.load(argument);
        } else {
            /*
             * Treat the argument as an animation-name prefix.
             */
            List<String> animationNames;
            animationNames = cgm.listAnimationNames(argument);
            Maud.gui.buildMenus.showAnimationSubmenu(animationNames, cgm);
        }
    }

    /**
     * Handle a "select bone" action with an argument.
     *
     * @param argument action argument (not null)
     */
    void selectBone(String argument) {
        if (Maud.model.target.bones.hasBone(argument)) {
            Maud.model.target.bone.select(argument);

        } else {
            /*
             * Treat the argument as a bone-name prefix.
             */
            List<String> boneNames;
            boneNames = Maud.model.target.bones.listBoneNames(argument);
            Maud.gui.buildMenus.showBoneSubmenu(boneNames);
        }
    }

    /**
     * Handle a "select boneChild" action with no argument.
     */
    void selectBoneChild() {
        if (Maud.model.target.bone.isSelected()) {
            int numChildren = Maud.model.target.bone.countChildren();
            if (numChildren == 1) {
                Maud.model.target.bone.selectFirstChild();
            } else if (numChildren > 1) {
                List<String> boneNames;
                boneNames = Maud.model.target.bone.listChildNames();
                Maud.gui.buildMenus.showBoneSubmenu(boneNames);
            }
        }
    }

    /**
     * Handle a "select boneWithTrack" action.
     */
    void selectBoneWithTrack() {
        List<String> boneNames;
        boneNames = Maud.model.target.animation.listBonesWithTrack();
        int numBoneTracks = boneNames.size();
        if (numBoneTracks == 1) {
            Maud.model.target.bone.select(boneNames.get(0));
        } else if (numBoneTracks > 1) {
            Maud.gui.buildMenus.showBoneSubmenu(boneNames);
        }
    }

    /**
     * Handle a "select menuItem" action for the editor screen.
     *
     * @param menuPath path to menu item (not null)
     * @return true if the action is handled, otherwise false
     */
    boolean selectMenuItem(String menuPath) {
        boolean handled;
        int separatorBegin = menuPath.indexOf(menuSeparator);
        if (separatorBegin == -1) {
            handled = Maud.gui.buildMenus.menuBar(menuPath);
        } else {
            int separatorEnd = separatorBegin + menuSeparator.length();
            String menuName = menuPath.substring(0, separatorBegin);
            String remainder = menuPath.substring(separatorEnd);
            handled = menu(menuName, remainder);
        }

        return handled;
    }

    /**
     * Handle a "select sourceBone" action with an argument.
     *
     * @param argument action argument (not null)
     */
    void selectSourceBone(String argument) {
        if (Maud.model.getSource().bones.hasBone(argument)) {
            Maud.model.getSource().bone.select(argument);
        } else {
            /*
             * Treat the argument as a bone-name prefix.
             */
            List<String> boneNames;
            boneNames = Maud.model.getSource().bones.listBoneNames(argument);
            Maud.gui.buildMenus.showBoneSubmenu(boneNames);
        }
    }

    /**
     * Handle a "select spatial" action with an argument.
     *
     * @param argument action argument (not null)
     */
    void selectSpatial(String argument, boolean includeNodes) {
        if (Maud.model.target.hasSpatial(argument)) {
            Maud.model.target.spatial.select(argument);

        } else {
            /*
             * Treat the argument as a spatial-name prefix.
             */
            List<String> names;
            names = Maud.model.target.listSpatialNames(argument, includeNodes);
            Maud.gui.buildMenus.showSpatialSubmenu(names, includeNodes);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Display a "Animation -> Load source" menu.
     */
    private void loadSourceAnimation() {
        if (Maud.model.getSource().isLoaded()) {
            List<String> animationNames;
            animationNames = Maud.model.getSource().listAnimationNames();
            Maud.gui.buildMenus.showAnimationSubmenu(animationNames,
                    Maud.model.getSource());
        }
    }

    /**
     * Handle a "select menuItem" action for a submenu.
     *
     * @param menuName name of the menu (not null)
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menu(String menuName, String remainder) {
        assert menuName != null;
        assert remainder != null;

        boolean handled = false;
        switch (menuName) {
            case "Animation":
                handled = menuAnimation(remainder);
                break;
            case "Bone":
                handled = menuBone(remainder);
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
            case "Spatial":
                handled = menuSpatial(remainder);
                break;
            case "View":
                handled = menuView(remainder);
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the Animation menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuAnimation(String remainder) {
        assert remainder != null;

        boolean handled;
        String addNewPrefix = "Add new" + menuSeparator;
        if (remainder.startsWith(addNewPrefix)) {
            String arg = MyString.remainder(remainder, addNewPrefix);
            handled = menuAnimationAddNew(arg);

        } else {
            handled = true;
            switch (remainder) {
                case "Add new":
                    Maud.gui.buildMenus.addNewAnimation();
                    break;
                case "Behead":
                    Maud.model.target.animation.behead();
                    break;
                case "Change duration":
                    Maud.gui.dialogs.setDuration();
                    break;
                case "Delete":
                    Maud.gui.dialogs.deleteAnimation();
                    break;
                case "Delete keyframes":
                    Maud.model.target.animation.deleteKeyframes();
                    break;
                case "Insert keyframes":
                    Maud.model.target.animation.insertKeyframes();
                    break;
                case "Load":
                    List<String> animationNames;
                    animationNames = Maud.model.target.listAnimationNames();
                    Maud.gui.buildMenus.showAnimationSubmenu(animationNames,
                            Maud.model.target);
                    break;
                case "Load source":
                    loadSourceAnimation();
                    break;
                case "Reduce":
                    Maud.gui.dialogs.reduceAnimation();
                    break;
                case "Rename":
                    Maud.gui.dialogs.renameAnimation();
                    break;
                case "Source tool":
                    Maud.gui.tools.select("sourceAnimation");
                    break;
                case "Tool":
                    Maud.gui.tools.select("animation");
                    break;
                case "Truncate":
                    Maud.model.target.animation.truncate();
                    break;
                case "Tweening":
                    Maud.gui.tools.select("tweening");
                    break;
                case "Wrap all tracks":
                    Maud.model.target.animation.wrapAllTracks();
                    break;
                default:
                    handled = false;
            }
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the "Animation -> Add new" menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuAnimationAddNew(String remainder) {
        assert remainder != null;

        boolean handled = true;
        switch (remainder) {
            case "Copy":
                Maud.gui.dialogs.copyAnimation();
                break;
            case "Pose":
                Maud.gui.dialogs.newAnimationFromPose();
                break;
            case "Retarget":
                Maud.gui.tools.select("retarget");
                break;
            default:
                handled = false;
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the Bone menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuBone(String remainder) {
        assert remainder != null;

        boolean handled = true;
        String selectPrefix = "Select" + menuSeparator;
        String selectSourcePrefix = "Select source" + menuSeparator;
        if (remainder.startsWith(selectPrefix)) {
            String selectArg = MyString.remainder(remainder, selectPrefix);
            handled = menuBoneSelect(selectArg);

        } else if (remainder.startsWith(selectSourcePrefix)) {
            String selectArg = MyString.remainder(remainder, selectSourcePrefix);
            handled = menuBoneSelectSource(selectArg);

        } else {
            switch (remainder) {
                case "Rename":
                    Maud.gui.dialogs.renameBone();
                    break;
                case "Rotate":
                    Maud.gui.tools.select("boneRotation");
                    break;
                case "Scale":
                    Maud.gui.tools.select("boneScale");
                    break;
                case "Select":
                    Maud.gui.buildMenus.selectBone();
                    break;
                case "Select source":
                    Maud.gui.buildMenus.selectSourceBone();
                    break;
                case "Tool":
                    Maud.gui.tools.select("bone");
                    break;
                case "Translate":
                    Maud.gui.tools.select("boneTranslation");
                    break;
                default:
                    handled = false;
            }
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the "Bone -> Select" menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
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
            case "Mapped":
                Maud.model.getMap().selectFromSource();
                handled = true;
                break;
            case "Next":
                Maud.model.target.bone.selectNext();
                handled = true;
                break;
            case "Parent":
                Maud.model.target.bone.selectParent();
                handled = true;
                break;
            case "Previous":
                Maud.model.target.bone.selectPrevious();
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
     * Handle a "select menuItem" action from the "Bone -> Select source" menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuBoneSelectSource(String remainder) {
        assert remainder != null;

        boolean handled = false;
        switch (remainder) {
            case "Mapped":
                Maud.model.getMap().selectFromTarget();
                handled = true;
                break;
            case "Root":
                selectSourceRootBone();
                handled = true;
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
        assert remainder != null;

        boolean handled;
        String sourcePrefix = "Source model" + EditorMenus.menuSeparator;
        if (remainder.startsWith(sourcePrefix)) {
            String selectArg = MyString.remainder(remainder, sourcePrefix);
            handled = menuSourceCgm(selectArg);

        } else {
            handled = true;
            switch (remainder) {
                case "History":
                    Maud.gui.tools.select("history");
                    break;
                case "Load":
                    Maud.gui.buildMenus.loadCgm();
                    break;
                case "Save":
                    Maud.gui.dialogs.saveCgm();
                    break;
                case "Source model":
                    Maud.gui.buildMenus.sourceCgm();
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
                Maud.gui.dialogs.license();
                handled = true;
                break;
            case "Source":
                Misc.browseWeb("https://github.com/stephengold/Maud");
                handled = true;
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
        assert remainder != null;

        boolean handled = true;
        switch (remainder) {
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
        assert remainder != null;

        boolean handled;
        String selectPrefix = "Select" + menuSeparator;
        if (remainder.startsWith(selectPrefix)) {
            String arg = MyString.remainder(remainder, selectPrefix);
            handled = menuKeyframeSelect(arg);

        } else {
            handled = true;
            switch (remainder) {
                case "Delete":
                    Maud.model.target.track.deleteSingleKeyframe();
                    break;
                case "Insert from pose":
                    Maud.model.target.track.insertSingleKeyframe();
                    break;
                case "Reduce track":
                    Maud.gui.dialogs.reduceTrack();
                    break;
                case "Select":
                    Maud.gui.buildMenus.selectKeyframe();
                    break;
                case "Tool":
                    Maud.gui.tools.select("keyframe");
                    break;
                case "Wrap track":
                    Maud.model.target.track.wrap();
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
        assert remainder != null;

        boolean handled = true;
        switch (remainder) {
            case "First":
                Maud.model.target.track.selectFirstKeyframe();
                break;
            case "Last":
                Maud.model.target.track.selectLastKeyframe();
                break;
            case "Next":
                Maud.model.target.track.selectNextKeyframe();
                break;
            case "Previous":
                Maud.model.target.track.selectPreviousKeyframe();
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
        assert remainder != null;

        boolean handled = false;
        switch (remainder) {
            case "Invert":
                Maud.model.getMap().invert();
                handled = true;
                break;
            case "Load":
                Maud.gui.buildMenus.loadMapAsset();
                handled = true;
                break;
            case "Save":
                Maud.gui.dialogs.saveMap();
                handled = true;
                break;
            case "Tool":
                Maud.gui.tools.select("map");
                handled = true;
                break;
            case "Twist tool":
                Maud.gui.tools.select("twist");
                handled = true;
                break;
            case "Unload":
                Maud.model.getMap().unload();
                handled = true;
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
        assert remainder != null;

        boolean handled = true;
        switch (remainder) {
            case "Add":
                Maud.model.target.spatial.addRigidBodyControl();
                break;
            case "Mass": // TODO
            case "Tool": // TODO
            case "Remove": // TODO
            default:
                handled = false;
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the "View -> Scenes" menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuSceneView(String remainder) {
        assert remainder != null;

        boolean handled = true;
        switch (remainder) {
            case "Axes":
                Maud.gui.tools.select("axes");
                break;
            case "Bounds":
                Maud.gui.tools.select("bounds");
                break;
            case "Camera":
                Maud.gui.tools.select("camera");
                break;
            case "Cursor":
                Maud.gui.tools.select("cursor");
                break;
            case "Mode":
                Maud.gui.buildMenus.selectViewMode();
                break;
            case "Physics":
                Maud.gui.tools.select("physics");
                break;
            case "Platform":
                Maud.gui.tools.select("platform");
                break;
            case "Render":
                Maud.gui.tools.select("render");
                break;
            case "Skeleton":
                Maud.gui.tools.select("skeleton");
                break;
            case "Skeleton color":
                Maud.gui.tools.select("skeletonColor");
                break;
            case "Sky":
                Maud.gui.tools.select("sky");
                break;
            default:
                handled = false;
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the "View -> Scores" menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuScoreView(String remainder) {
        assert remainder != null;

        boolean handled = true;
        switch (remainder) {
            case "Background":
                Maud.gui.tools.select("background");
                break;
            case "Tool":
                Maud.gui.tools.select("score");
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
        assert remainder != null;

        boolean handled;
        String folderPrefix = "Asset folders" + menuSeparator;
        if (remainder.startsWith(folderPrefix)) {
            String selectArg = MyString.remainder(remainder, folderPrefix);
            handled = Maud.gui.buildMenus.menuAssetFolders(selectArg);

        } else {
            handled = true;
            switch (remainder) {
                case "Asset folders":
                    Maud.gui.buildMenus.assetFolders();
                    break;
                case "Hotkeys":
                    Maud.gui.closeAllPopups();
                    Maud.bindScreen.activate(Maud.gui.inputMode);
                    break;
                default:
                    handled = false;
            }
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
        assert remainder != null;

        boolean handled = false;
        switch (remainder) {
            case "Load":
                Maud.gui.buildMenus.loadSourceCgm();
                handled = true;
                break;

            case "Unload":
                Maud.model.getSource().unload();
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
        assert remainder != null;

        boolean handled = true;
        String addControlPrefix = "Add control" + menuSeparator;
        String selectPrefix = "Select" + menuSeparator;
        if (remainder.startsWith(addControlPrefix)) {
            String arg = MyString.remainder(remainder, addControlPrefix);
            handled = menuSpatialAddControl(arg);

        } else if (remainder.startsWith(selectPrefix)) {
            String arg = MyString.remainder(remainder, selectPrefix);
            handled = menuSpatialSelect(arg);

        } else {
            switch (remainder) {
                case "Add control":
                    Maud.gui.buildMenus.addSgc();
                    break;
                case "Control tool":
                    Maud.gui.tools.select("sgc");
                    break;
                case "Delete control":
                    Maud.gui.dialogs.deleteSgc();
                    break;
                case "Delete":
                    Maud.model.target.spatial.delete();
                    break;
                case "Rotate":
                    Maud.gui.tools.select("spatialRotation");
                    break;
                case "Scale":
                    Maud.gui.tools.select("spatialScale");
                    break;
                case "Select":
                    Maud.gui.buildMenus.selectSpatial();
                    break;
                case "Select control":
                    Maud.gui.buildMenus.selectSgc();
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
     * Handle a "select menuItem" action from the "Spatial -> Add control" menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuSpatialAddControl(String remainder) {
        assert remainder != null;

        boolean handled = false;
        switch (remainder) {
            case "Anim":
                Maud.model.target.spatial.addAnimControl();
                handled = true;
                break;

            case "RigidBody":
                Maud.model.target.spatial.addRigidBodyControl();
                handled = true;
                break;

            case "Skeleton":
                Maud.model.target.spatial.addSkeletonControl();
                handled = true;
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
        assert remainder != null;

        boolean handled = true;
        switch (remainder) {
            case "By name":
                selectSpatial("", true);
                break;
            case "Child":
                Maud.gui.buildMenus.selectSpatialChild("");
                break;
            case "Geometry":
                selectSpatial("", false);
                break;
            case "Parent":
                Maud.model.target.spatial.selectParent();
                break;
            case "Root":
                Maud.model.target.spatial.selectModelRoot();
                break;
            default:
                handled = false;
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the View menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuView(String remainder) {
        assert remainder != null;

        boolean handled = true;
        String modePrefix = "Mode" + menuSeparator;
        String scenesPrefix = "Scene options" + menuSeparator;
        String scoresPrefix = "Score options" + menuSeparator;
        if (remainder.startsWith(modePrefix)) {
            String arg = MyString.remainder(remainder, modePrefix);
            handled = menuViewMode(arg);

        } else if (remainder.startsWith(scenesPrefix)) {
            String arg = MyString.remainder(remainder, scenesPrefix);
            handled = menuSceneView(arg);

        } else if (remainder.startsWith(scoresPrefix)) {
            String arg = MyString.remainder(remainder, scoresPrefix);
            handled = menuScoreView(arg);

        } else {
            switch (remainder) {
                case "Mode":
                    Maud.gui.buildMenus.selectViewMode();
                    break;
                case "Scene options":
                    Maud.gui.buildMenus.sceneViewOptions();
                    break;
                case "Score options":
                    Maud.gui.buildMenus.scoreViewOptions();
                    break;
                default:
                    handled = false;
            }
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the "View -> Mode" menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuViewMode(String remainder) {
        assert remainder != null;

        boolean handled = true;
        switch (remainder) {
            case "Hybrid":
                Maud.model.misc.setViewMode("hybrid");
                break;
            case "Scene":
                Maud.model.misc.setViewMode("scene");
                break;
            case "Score":
                Maud.model.misc.setViewMode("score");
                break;
            default:
                handled = false;
        }

        return handled;
    }

    /**
     * Select a bone by name, using submenus.
     */
    private void selectBoneByName() {
        List<String> nameList = Maud.model.target.bones.listBoneNames();
        Maud.gui.buildMenus.showBoneSubmenu(nameList);
    }

    /**
     * Select a bone by parent, using submenus.
     */
    private void selectBoneByParent() {
        List<String> boneNames = Maud.model.target.bones.listRootBoneNames();
        Maud.gui.showPopupMenu(ActionPrefix.selectBoneChild, boneNames);
    }

    /**
     * Handle a "select rootBone" action.
     */
    private void selectRootBone() {
        int numRoots = Maud.model.target.bones.countRootBones();
        if (numRoots == 1) {
            Maud.model.target.bone.selectFirstRoot();
        } else if (numRoots > 1) {
            List<String> boneNames;
            boneNames = Maud.model.target.bones.listRootBoneNames();
            Maud.gui.buildMenus.showBoneSubmenu(boneNames);
        }
    }

    /**
     * Handle a "select sourceRootBone" action.
     */
    private void selectSourceRootBone() {
        int numRoots = Maud.model.getSource().bones.countRootBones();
        if (numRoots == 1) {
            Maud.model.getSource().bone.selectFirstRoot();
        } else if (numRoots > 1) {
            List<String> names;
            names = Maud.model.getSource().bones.listRootBoneNames();
            Maud.gui.buildMenus.showSourceBoneSubmenu(names);
        }
    }
}
