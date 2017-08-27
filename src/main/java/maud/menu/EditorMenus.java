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
import maud.model.EditableCgm;
import maud.model.EditableMap;
import maud.model.History;
import maud.model.LoadedAnimation;
import maud.model.LoadedCgm;
import maud.model.MiscStatus;
import maud.model.SelectedSpatial;
import maud.model.ViewMode;

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
    final private static Logger logger = Logger.getLogger(
            EditorMenus.class.getName());
    /**
     * magic filename used in "add locator" menus
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
     * Handle a "load (source)animation" action without arguments.
     *
     * @param cgm (not null)
     */
    public void loadAnimation(LoadedCgm cgm) {
        if (cgm.isLoaded()) {
            List<String> animationNames = cgm.listAnimationNames();
            Maud.gui.showMenus.showAnimationSubmenu(animationNames, cgm);
        }
    }

    /**
     * Handle a "load (source)animation" action with an argument.
     *
     * @param argument action argument (not null)
     * @param cgm which load slot (not null)
     */
    public void loadAnimation(String argument, LoadedCgm cgm) {
        if (cgm.hasAnimation(argument)
                || argument.equals(LoadedAnimation.bindPoseName)
                || argument.equals(LoadedAnimation.retargetedPoseName)) {
            cgm.getAnimation().load(argument);
        } else {
            /*
             * Treat the argument as an animation-name prefix.
             */
            List<String> animationNames;
            animationNames = cgm.listAnimationNames(argument);
            Maud.gui.showMenus.showAnimationSubmenu(animationNames, cgm);
        }
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
     * Handle a "select spatial" action with an argument.
     *
     * @param argument action argument (not null)
     * @param includeNodes true &rarr; include both nodes and geometries, false
     * &rarr; include geometries only
     */
    public void selectSpatial(String argument, boolean includeNodes) {
        LoadedCgm target = Maud.getModel().getTarget();
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
                handled = menuAnimation(remainder);
                break;
            case "Bone":
                handled = Maud.gui.boneMenus.menuBone(remainder);
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
        String addNewPrefix = "Add new" + menuPathSeparator;
        String editPrefix = "Edit" + menuPathSeparator;
        if (remainder.startsWith(addNewPrefix)) {
            String arg = MyString.remainder(remainder, addNewPrefix);
            handled = menuAnimationAddNew(arg);

        } else if (remainder.startsWith(editPrefix)) {
            String arg = MyString.remainder(remainder, editPrefix);
            handled = menuAnimationEdit(arg);

        } else {
            handled = true;
            EditableCgm target = Maud.getModel().getTarget();
            switch (remainder) {
                case "Add new":
                    Maud.gui.showMenus.addNewAnimation();
                    break;
                case "Delete":
                    Maud.gui.dialogs.deleteAnimation();
                    break;
                case "Edit":
                    Maud.gui.showMenus.editAnimation();
                    break;
                case "Load":
                    loadAnimation(target);
                    break;
                case "Load source":
                    loadAnimation(Maud.getModel().getSource());
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
                case "Tweening":
                    Maud.gui.tools.select("tweening");
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
     * Handle a "select menuItem" action from the "Animation -> Edit -> Change
     * duration" menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuAnimationChangeDuration(String remainder) {
        assert remainder != null;

        boolean handled = true;
        switch (remainder) {
            case "Proportional times":
                Maud.gui.dialogs.setDurationProportional();
                break;
            case "Same times":
                Maud.gui.dialogs.setDurationSame();
                break;
            default:
                handled = false;
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the "Animation -> Edit" menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuAnimationEdit(String remainder) {
        assert remainder != null;

        EditableCgm target = Maud.getModel().getTarget();
        LoadedAnimation animation = target.getAnimation();
        boolean handled;
        String changeDurationPrefix = "Change duration" + menuPathSeparator;
        if (remainder.startsWith(changeDurationPrefix)) {
            String arg = MyString.remainder(remainder, changeDurationPrefix);
            handled = menuAnimationChangeDuration(arg);

        } else {
            handled = true;
            switch (remainder) {
                case "Behead":
                    animation.behead();
                    break;
                case "Change duration":
                    Maud.gui.showMenus.changeDuration();
                    break;
                case "Delete keyframes":
                    animation.deleteKeyframes();
                    break;
                case "Insert keyframes":
                    animation.insertKeyframes();
                    break;
                case "Reduce all tracks":
                    Maud.gui.dialogs.reduceAnimation();
                    break;
                case "Resample all tracks":
                    Maud.gui.dialogs.resampleAnimation();
                    break;
                case "Truncate":
                    animation.truncate();
                    break;
                case "Wrap all tracks":
                    animation.wrapAllTracks();
                    break;
                default:
                    handled = false;
            }
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
        String sourcePrefix = "Source model" + EditorMenus.menuPathSeparator;
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
        assert remainder != null;

        boolean handled;
        String selectPrefix = "Select" + menuPathSeparator;
        if (remainder.startsWith(selectPrefix)) {
            String arg = MyString.remainder(remainder, selectPrefix);
            handled = menuKeyframeSelect(arg);

        } else {
            EditableCgm target = Maud.getModel().getTarget();
            handled = true;
            switch (remainder) {
                case "Delete":
                    target.getTrack().deleteSingleKeyframe();
                    break;
                case "Insert from pose":
                    target.getTrack().insertSingleKeyframe();
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
        assert remainder != null;

        LoadedCgm target = Maud.getModel().getTarget();
        boolean handled = true;
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
        assert remainder != null;

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
                Maud.gui.dialogs.saveMap();
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
        assert remainder != null;

        boolean handled = true;
        switch (remainder) {
            case "Add":
                Maud.getModel().getTarget().getSpatial().addRigidBodyControl();
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
                Maud.gui.showMenus.selectViewMode();
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
            case "Vertex":
                Maud.gui.tools.select("sceneVertex");
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
        String folderPrefix = "Asset folders" + menuPathSeparator;
        if (remainder.startsWith(folderPrefix)) {
            String selectArg = MyString.remainder(remainder, folderPrefix);
            handled = Maud.gui.buildMenus.menuAssetFolders(selectArg);

        } else {
            handled = true;
            MiscStatus status = Maud.getModel().getMisc();
            switch (remainder) {
                case "Asset folders":
                    Maud.gui.showMenus.assetFolders();
                    break;
                case "Diagnose loads":
                    status.setDiagnoseLoads(true);
                    break;
                case "Hotkeys":
                    Maud.gui.goBindScreen();
                    break;
                case "Start indices at 0":
                    status.setIndexBase(0);
                    break;
                case "Start indices at 1":
                    status.setIndexBase(1);
                    break;
                case "Stop diagnosing loads":
                    status.setDiagnoseLoads(false);
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
        assert remainder != null;

        boolean handled = true;
        String addPrefix = "Add" + menuPathSeparator;
        if (remainder.startsWith(addPrefix)) {
            String arg = MyString.remainder(remainder, addPrefix);
            handled = menuSgcAdd(arg);

        } else {
            switch (remainder) {
                case "Add":
                    Maud.gui.showMenus.addSgc();
                    break;
                case "Delete":
                    Maud.gui.dialogs.deleteSgc();
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
     * Handle a "select menuItem" action from the "Spatial -> Add control" menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuSgcAdd(String remainder) {
        boolean handled = false;

        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        switch (remainder) {
            case "Anim":
                spatial.addAnimControl();
                handled = true;
                break;

            case "RigidBody":
                spatial.addRigidBodyControl();
                handled = true;
                break;

            case "Skeleton":
                spatial.addSkeletonControl();
                handled = true;
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
        assert remainder != null;

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
        assert remainder != null;

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
        assert remainder != null;

        boolean handled = true;
        EditableCgm target = Maud.getModel().getTarget();
        switch (remainder) {
            case "Reduce":
                Maud.gui.dialogs.reduceTrack();
                break;
            case "Resample":
                Maud.gui.dialogs.resampleTrack();
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
        assert remainder != null;

        boolean handled;
        String selectPrefix = "Select" + menuPathSeparator;
        if (remainder.startsWith(selectPrefix)) {
            String arg = MyString.remainder(remainder, selectPrefix);
            handled = menuVertexSelect(arg);

        } else {
            handled = true;
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
        assert remainder != null;

        boolean handled = true;
        switch (remainder) {
            case "By index":
                Maud.gui.dialogs.selectVertex();
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

    /**
     * Handle a "select menuItem" action from the View menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuView(String remainder) {
        assert remainder != null;

        boolean handled = true;
        String modePrefix = "Mode" + menuPathSeparator;
        String scenesPrefix = "Scene options" + menuPathSeparator;
        String scoresPrefix = "Score options" + menuPathSeparator;
        if (remainder.startsWith(modePrefix)) {
            String arg = MyString.remainder(remainder, modePrefix);
            menuViewMode(arg);
            handled = true;

        } else if (remainder.startsWith(scenesPrefix)) {
            String arg = MyString.remainder(remainder, scenesPrefix);
            handled = menuSceneView(arg);

        } else if (remainder.startsWith(scoresPrefix)) {
            String arg = MyString.remainder(remainder, scoresPrefix);
            handled = menuScoreView(arg);

        } else {
            switch (remainder) {
                case "Mode":
                    Maud.gui.showMenus.selectViewMode();
                    break;
                case "Scene options":
                    Maud.gui.showMenus.sceneViewOptions();
                    break;
                case "Score options":
                    Maud.gui.showMenus.scoreViewOptions();
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
    private void menuViewMode(String remainder) {
        assert remainder != null;

        ViewMode viewMode = ViewMode.valueOf(remainder);
        Maud.getModel().getMisc().setViewMode(viewMode);
    }
}
