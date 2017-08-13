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

import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Spatial;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyString;
import maud.model.LoadedAnimation;
import maud.model.LoadedCgm;
import maud.model.ViewMode;

/**
 * Build menus in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class BuildMenus {
    // *************************************************************************
    // constants and loggers

    /**
     * true if jme3-testdata assets are available
     */
    final private static boolean haveTestdata = false;
    /**
     * maximum number of items in a menu
     */
    final private static int maxItems = 19;
    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            BuildMenus.class.getName());
    // *************************************************************************
    // fields

    /**
     * reusable builder for popup menus
     */
    final private static MenuBuilder builder = new MenuBuilder();
    // *************************************************************************
    // new methods exposed

    /**
     * Display a "Animation -> Add new" menu.
     */
    void addNewAnimation() {
        builder.reset();
        builder.addDialog("Copy");
        builder.addDialog("Pose");
        builder.addTool("Retarget");
        builder.show("select menuItem Animation -> Add new -> ");
    }

    /**
     * Display a "Spatial -> Add control" menu.
     */
    void addSgc() {
        builder.reset();
        builder.add("Anim");
        builder.add("RigidBody");
        builder.add("Skeleton");
        builder.show("select menuItem Spatial -> Add control -> ");
    }

    /**
     * Display a "Settings -> Asset folders" menu.
     */
    void assetFolders() {
        builder.reset();
        builder.add("Add");
        if (Maud.model.getLocations().hasRemovable()) {
            builder.add("Remove");
        }
        builder.show("select menuItem Settings -> Asset folders -> ");
    }

    /**
     * Handle a "load cgm asset" action without arguments.
     */
    void loadCgm() {
        buildLocatorMenu();
        builder.show(ActionPrefix.loadCgmLocator);
    }

    /**
     * Handle a "load (source)cgm asset" action with arguments.
     *
     * @param args action arguments (not null, not empty)
     * @param cgm (not null)
     */
    void loadCgmAsset(String args, LoadedCgm cgm) {
        String menuPrefix = null;
        if (cgm == Maud.model.getSource()) {
            menuPrefix = ActionPrefix.loadSourceCgmAsset;
        } else if (cgm == Maud.model.target) {
            menuPrefix = ActionPrefix.loadCgmAsset;
        } else {
            throw new IllegalArgumentException();
        }

        String indexString = args.split(" ")[0];
        String rootPath = Maud.model.getLocations().pathForIndex(indexString);
        String assetPath = MyString.remainder(args, indexString + " ");

        if (rootPath.endsWith(".jar") || rootPath.endsWith(".zip")) {
            List<String> entryNames = Misc.listZipEntries(rootPath, assetPath);
            int numEntries = entryNames.size();
            List<String> cgmEntries = new ArrayList<>(numEntries);
            for (String entryName : entryNames) {
                if (MenuBuilder.hasCgmSuffix(entryName)) {
                    cgmEntries.add(entryName);
                }
            }
            if (cgmEntries.size() == 1 && cgmEntries.contains(assetPath)) {
                cgm.loadAsset(rootPath, assetPath);
            } else if (!cgmEntries.isEmpty()) {
                selectFile(cgmEntries, menuPrefix + indexString + " ");
            }

        } else { // not a JAR or ZIP
            File file = new File(rootPath, assetPath);
            if (file.isDirectory()) {
                String folderPath = file.getAbsolutePath();
                buildFolderMenu(folderPath, "");
                menuPrefix += args;
                if (!menuPrefix.endsWith("/")) {
                    menuPrefix += "/";
                }
                builder.show(menuPrefix);

            } else if (file.canRead()) {
                cgm.loadAsset(rootPath, assetPath);

            } else {
                /*
                 * Treat the pathname as a prefix.
                 */
                File parent = file.getParentFile();
                String parentPath = parent.getAbsolutePath();
                parentPath = parentPath.replaceAll("\\\\", "/");
                String prefix = file.getName();
                buildFolderMenu(parentPath, prefix);
                parentPath = MyString.remainder(parentPath, rootPath);
                menuPrefix += indexString + " " + parentPath;
                if (!menuPrefix.endsWith("/")) {
                    menuPrefix += "/";
                }
                builder.show(menuPrefix);
            }
        }
    }

    /**
     * Handle a "load (source)cgm locator" action with argument.
     *
     * @param path action argument (not null, not empty)
     * @param cgm (not null)
     */
    void loadCgmLocator(String path, LoadedCgm cgm) {
        if (path.equals("From classpath")) {
            buildTestDataMenu();
            String menuPrefix = null;
            if (cgm == Maud.model.getSource()) {
                menuPrefix = ActionPrefix.loadSourceCgmNamed;
            } else if (cgm == Maud.model.target) {
                menuPrefix = ActionPrefix.loadCgmNamed;
            } else {
                assert false;
            }
            builder.show(menuPrefix);

        } else {
            String indexString = Maud.model.getLocations().indexForPath(path);
            String args = indexString + " /";
            loadCgmAsset(args, cgm);
        }
    }

    /**
     * Display a "load map asset" action without arguments.
     */
    void loadMapAsset() {
        buildLocatorMenu();
        builder.show(ActionPrefix.loadMapLocator);
    }

    /**
     * Handle a "load map asset" action with arguments.
     *
     * @param args action arguments (not null, not empty)
     */
    void loadMapAsset(String args) {
        String menuPrefix = ActionPrefix.loadMapAsset;
        String indexString = args.split(" ")[0];
        String rootPath = Maud.model.getLocations().pathForIndex(indexString);
        String assetPath = MyString.remainder(args, indexString + " ");
        File file = new File(rootPath, assetPath);
        if (file.isDirectory()) {
            String folderPath = file.getAbsolutePath();
            buildFolderMenu(folderPath, "");
            menuPrefix += args;
            if (!menuPrefix.endsWith("/")) {
                menuPrefix += "/";
            }
            builder.show(menuPrefix);

        } else if (file.canRead()) {
            Maud.model.getMap().loadAsset(rootPath, assetPath);

        } else {
            /*
             * Treat the pathname as a prefix.
             */
            String folderName = file.getParent();
            String prefix = file.getName();
            buildFolderMenu(folderName, prefix);
            menuPrefix += folderName;
            if (!menuPrefix.endsWith("/")) {
                menuPrefix += "/";
            }
            builder.show(menuPrefix);
        }
    }

    /**
     * Handle a "load map locator" action.
     *
     * @param path action argument (not null, not empty)
     * @param cgm (not null)
     */
    void loadMapLocator(String path) {
        if (path.equals("From classpath")) {
            buildClasspathMapMenu();
            builder.show(ActionPrefix.loadMapNamed);

        } else {
            String indexString = Maud.model.getLocations().indexForPath(path);
            String args = indexString + " /";
            loadMapAsset(args);
        }
    }

    /**
     * Handle a "load sourceCgm asset" action without arguments.
     */
    void loadSourceCgm() {
        buildLocatorMenu();
        builder.show(ActionPrefix.loadSourceCgmLocator);
    }

    /**
     * Handle a "select menuItem" action from the "Settings -> Asset folders"
     * menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    boolean menuAssetFolders(String remainder) {
        assert remainder != null;

        boolean handled = false;
        switch (remainder) {
            case "Add":
                addAssetFolder();
                handled = true;
                break;
            case "Remove":
                builder.reset();
                List<String> pathList = Maud.model.getLocations().listAll();
                for (String path : pathList) {
                    builder.addFolder(path);
                }
                builder.show(ActionPrefix.deleteAssetFolder);
                handled = true;
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action for a top-level menu, typically from
     * the menu bar.
     *
     * @param menuName name of the menu to open (not null)
     * @return true if handled, otherwise false
     */
    boolean menuBar(String menuName) {
        assert menuName != null;
        /**
         * Dynamically generate the menu's item list.
         */
        builder.reset();
        switch (menuName) {
            case "Animation":
                buildAnimationMenu();
                break;
            case "Bone":
                buildBoneMenu();
                break;
            case "CGM":
                buildCgmMenu();
                break;
            case "Help":
                buildHelpMenu();
                break;
            case "History":
                buildHistoryMenu();
                break;
            case "Keyframe":
                buildKeyframeMenu();
                break;
            case "Map":
                buildMapMenu();
                break;
            case "Physics":
                buildPhysicsMenu();
                break;
            case "Settings":
                buildSettingsMenu();
                break;
            case "SGC":
                buildSgcMenu();
                break;
            case "Spatial":
                buildSpatialMenu();
                break;
            case "View":
                buildViewMenu();
                break;
            default:
                return false;
        }
        if (builder.isEmpty()) {
            logger.log(Level.WARNING, "no items for the {0} menu",
                    MyString.quote(menuName));
        } else {
            String actionPrefix = ActionPrefix.selectMenuItem + menuName
                    + EditorMenus.menuSeparator;
            builder.show(actionPrefix);
        }

        return true;
    }

    /**
     * Handle a "new locator" action with an argument.
     *
     * @param argument action argument (not null)
     */
    void newAssetFolder(String argument) {
        if (argument.endsWith(EditorMenus.addThis)) {
            String path = MyString.removeSuffix(argument, EditorMenus.addThis);
            Maud.model.getLocations().add(path);

        } else if (argument.endsWith(".jar") || argument.endsWith(".zip")) {
            Maud.model.getLocations().add(argument);

        } else {
            Map<String, File> folderMap = EditorMenus.folderMap(argument);
            buildFolderMenu(folderMap);

            File file = new File(argument);
            if (!file.isDirectory()) {
                file = file.getParentFile();
            }
            String folderPath = file.getAbsolutePath();
            String menuPrefix = ActionPrefix.newAssetFolder + folderPath + "/";
            builder.show(menuPrefix);
        }
    }

    /**
     * Display a "View -> Scene options" menu.
     */
    void sceneViewOptions() {
        builder.reset();
        builder.addTool("Axes");
        builder.addTool("Bounds");
        builder.addTool("Camera");
        builder.addTool("Cursor");
        builder.addTool("Physics");
        builder.addTool("Platform");
        builder.addTool("Render");
        builder.addTool("Skeleton");
        builder.addTool("Skeleton color");
        builder.addTool("Sky");
        builder.show("select menuItem View -> Scene options -> ");
    }

    /**
     * Display a "View -> Score options" menu.
     */
    void scoreViewOptions() {
        builder.reset();
        builder.addTool("Tool");
        builder.addTool("Background");
        builder.show("select menuItem View -> Score options -> ");
    }

    /**
     * Handle a "select (source)animControl" action without an argument.
     *
     * @param cgm which load slot (not null)
     */
    void selectAnimControl(LoadedCgm cgm) {
        builder.reset();
        List<String> names = cgm.listAnimControlNames();
        for (String name : names) {
            builder.add(name);
        }
        if (cgm == Maud.model.target) {
            builder.show("select animControl ");
        } else if (cgm == Maud.model.getSource()) {
            builder.show("select sourceAnimControl ");
        } else {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Handle a "select bone" action without an argument.
     */
    void selectBone() {
        builder.reset();
        buildBoneSelectMenu();
        builder.show("select menuItem Bone -> Select -> ");
    }

    /**
     * Handle a "select boneChild" action with an argument.
     *
     * @param argument action argument (not null)
     */
    void selectBoneChild(String argument) {
        if (argument.startsWith("!")) {
            String name = argument.substring(1);
            Maud.model.target.bone.select(name);
        } else {
            List<String> names;
            names = Maud.model.target.bones.listChildBoneNames(argument);

            builder.reset();
            builder.addBone("!" + argument);
            for (String name : names) {
                if (Maud.model.target.bones.isLeafBone(name)) {
                    builder.addBone("!" + name);
                } else {
                    builder.add(name);
                }
            }
            builder.show(ActionPrefix.selectBoneChild);
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
            showBoneSubmenu(boneNames);
        }
    }

    /**
     * Display a "Keyframe -> Select" menu.
     */
    void selectKeyframe() {
        builder.reset();
        builder.addTool("First");
        builder.addTool("Previous");
        builder.addTool("Next");
        builder.addTool("Last");
        builder.show("select menuItem Keyframe -> Select -> ");
    }

    /**
     * Display a "Spatial -> Select control" menu.
     */
    void selectSgc() {
        builder.reset();
        for (String name : Maud.model.target.spatial.listSgcNames()) {
            builder.add(name);
        }
        builder.add(LoadedCgm.noControl);
        builder.show(ActionPrefix.selectControl);
    }

    /**
     * Display a "Bone -> Select source" menu.
     */
    void selectSourceBone() {
        if (Maud.model.getSource().isLoaded()) {
            builder.reset();
            buildSourceBoneSelectMenu();
            builder.show("select menuItem Bone -> Select source -> ");
        }
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
            showBoneSubmenu(boneNames);
        }
    }

    /**
     * Display a "Spatial -> Select" menu.
     */
    void selectSpatial() {
        builder.reset();

        List<String> names = Maud.model.target.listSpatialNames("", true);
        if (!names.isEmpty()) {
            builder.add("By name");
        }

        boolean isRootANode = Maud.model.target.isRootANode();
        if (isRootANode) {
            builder.addNode("Root");
        } else {
            builder.addGeometry("Root");
        }

        names = Maud.model.target.listSpatialNames("", false);
        if (!names.isEmpty()) {
            builder.add("Geometry");
        }

        int numChildren = Maud.model.target.spatial.countChildren();
        if (numChildren == 1) {
            boolean isChildANode = Maud.model.target.spatial.isChildANode(0);
            if (isChildANode) {
                builder.addNode("Child");
            } else {
                builder.addGeometry("Child");
            }
        } else if (numChildren > 1) {
            builder.add("Child");
        }

        boolean isRoot = Maud.model.target.spatial.isCgmRoot();
        if (!isRoot) {
            builder.addNode("Parent");
        }

        builder.show("select menuItem Spatial -> Select -> ");
    }

    /**
     * Handle a "select spatialChild" action.
     *
     * @param itemPrefix prefix for filtering menu items (not null)
     */
    void selectSpatialChild(String itemPrefix) {
        int numChildren = Maud.model.target.spatial.countChildren();
        if (numChildren == 1) {
            Maud.model.target.spatial.selectChild(0);

        } else if (numChildren > 1) {
            List<String> children;
            children = Maud.model.target.spatial.listNumberedChildren();

            List<String> choices;
            choices = MyString.addMatchPrefix(children, itemPrefix, null);
            MyString.reduce(choices, maxItems);
            Collections.sort(choices);

            builder.reset();
            for (String choice : choices) {
                int childIndex = children.indexOf(choice);
                if (childIndex >= 0) {
                    boolean isANode = Maud.model.target.spatial.isChildANode(
                            childIndex);
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
     * Display a menu for selecting a user data type using the "new userKey "
     * action prefix.
     */
    void selectUserDataType() {
        builder.reset();
        builder.add("integer");
        builder.add("float");
        builder.add("boolean");
        builder.add("string");
        builder.add("long");
        // TODO savable, list, map, array
        builder.show(ActionPrefix.newUserKey);
    }

    /**
     * Display a menu for selecting a user key using the "select userKey "
     * action prefix.
     */
    void selectUserKey() {
        builder.reset();
        List<String> keyList = Maud.model.target.spatial.listUserKeys();
        String selectedKey = Maud.model.misc.getSelectedUserKey();
        for (String key : keyList) {
            if (!key.equals(selectedKey)) {
                builder.add(key);
            }
        }
        builder.show(ActionPrefix.selectUserKey);
    }

    /**
     * Handle a "select viewMode" action without an argument.
     */
    void selectViewMode() {
        builder.reset();
        ViewMode viewMode = Maud.model.misc.getViewMode();
        for (ViewMode mode : ViewMode.values()) {
            if (!mode.equals(viewMode)) {
                builder.add(mode.toString());
            }
        }
        builder.show("select menuItem View -> Mode -> ");
    }

    /**
     * Display a menu to set the batch hint of the current spatial using the
     * "set batchHint " action prefix.
     */
    void setBatchHint() {
        builder.reset();
        Spatial.BatchHint selectedHint;
        selectedHint = Maud.model.target.spatial.getLocalBatchHint();
        for (Spatial.BatchHint hint : Spatial.BatchHint.values()) {
            if (!hint.equals(selectedHint)) {
                String name = hint.toString();
                builder.add(name);
            }
        }
        builder.show(ActionPrefix.setBatchHint);
    }

    /**
     * Display a menu to set the cull hint of the current spatial using the "set
     * cullHint " action prefix.
     */
    void setCullHint() {
        builder.reset();
        Spatial.CullHint selectedHint;
        selectedHint = Maud.model.target.spatial.getLocalCullHint();
        for (Spatial.CullHint hint : Spatial.CullHint.values()) {
            if (!hint.equals(selectedHint)) {
                String name = hint.toString();
                builder.add(name);
            }
        }
        builder.show(ActionPrefix.setCullHint);
    }

    /**
     * Display a menu to set the render bucket of the current spatial using the
     * "set renderBucket " action prefix.
     */
    void setQueueBucket() {
        builder.reset();
        RenderQueue.Bucket selectedBucket;
        selectedBucket = Maud.model.target.spatial.getLocalQueueBucket();
        for (RenderQueue.Bucket bucket : RenderQueue.Bucket.values()) {
            if (!bucket.equals(selectedBucket)) {
                String name = bucket.toString();
                builder.add(name);
            }
        }
        builder.show(ActionPrefix.setQueueBucket);
    }

    /**
     * Display a menu to set the shadow mode of the current spatial using the
     * "set shadowMode " action prefix.
     */
    void setShadowMode() {
        builder.reset();
        RenderQueue.ShadowMode selectedMode;
        selectedMode = Maud.model.target.spatial.getLocalShadowMode();
        for (RenderQueue.ShadowMode mode : RenderQueue.ShadowMode.values()) {
            if (!mode.equals(selectedMode)) {
                String name = mode.toString();
                builder.add(name);
            }
        }
        builder.show(ActionPrefix.setShadowMode);
    }

    /**
     * Display a menu to set the rotation tweening mode using the "set
     * tweenRotations " action prefix.
     */
    void setTweenRotations() {
        builder.reset();
        TweenRotations selected = Maud.model.misc.getTweenRotations();
        for (TweenRotations t : TweenRotations.values()) {
            if (!t.equals(selected)) {
                String name = t.toString();
                builder.add(name);
            }
        }
        builder.show(ActionPrefix.setTweenRotations);
    }

    /**
     * Display a menu to set the scale tweening mode using the "set tweenScales
     * " action prefix.
     */
    void setTweenScales() {
        builder.reset();
        TweenVectors selected = Maud.model.misc.getTweenScales();
        for (TweenVectors t : TweenVectors.values()) {
            if (!t.equals(selected)) {
                String name = t.toString();
                builder.add(name);
            }
        }
        builder.show(ActionPrefix.setTweenScales);
    }

    /**
     * Display a menu to set the translation tweening mode using the "set
     * tweenTranslations " action prefix.
     */
    void setTweenTranslations() {
        builder.reset();
        TweenVectors selected = Maud.model.misc.getTweenTranslations();
        for (TweenVectors t : TweenVectors.values()) {
            if (!t.equals(selected)) {
                String name = t.toString();
                builder.add(name);
            }
        }
        builder.show(ActionPrefix.setTweenTranslations);
    }

    /**
     * Display a submenu for selecting a target animation by name using the
     * "select (source)animation" action prefix.
     *
     * @param nameList list of names from which to select (not null, modified)
     * @param cgm which load slot (not null)
     */
    void showAnimationSubmenu(List<String> nameList, LoadedCgm cgm) {
        assert nameList != null;
        assert cgm != null;

        String loadedAnimation = cgm.animation.getName();
        boolean success = nameList.remove(loadedAnimation);
        assert success;

        MyString.reduce(nameList, maxItems);
        Collections.sort(nameList);

        builder.reset();
        for (String name : nameList) {
            if (cgm.hasAnimation(name)) {
                builder.add(name); // TODO icon
            } else if (name.equals(LoadedAnimation.bindPoseName)) {
                builder.add(name);
            } else if (name.equals(LoadedAnimation.retargetedPoseName)) {
                builder.add(name);
            } else {
                builder.addEllipsis(name);
            }
        }

        if (cgm == Maud.model.target) {
            builder.show(ActionPrefix.loadAnimation);
        } else if (cgm == Maud.model.getSource()) {
            builder.show(ActionPrefix.loadSourceAnimation);
        } else {
            assert false;
        }
    }

    /**
     * Display a submenu for selecting a target bone by name using the "select
     * bone" action prefix.
     *
     * @param nameList list of names from which to select (not null)
     */
    void showBoneSubmenu(List<String> nameList) {
        assert nameList != null;

        MyString.reduce(nameList, maxItems);
        Collections.sort(nameList);

        builder.reset();
        for (String name : nameList) {
            if (Maud.model.target.bones.hasBone(name)) {
                builder.addBone(name);
            } else {
                builder.addEllipsis(name);
            }
        }
        builder.show(ActionPrefix.selectBone);
    }

    /**
     * Display a submenu for selecting a source bone by name using the "select
     * sourceBone" action prefix.
     *
     * @param nameList list of names from which to select (not null)
     */
    void showSourceBoneSubmenu(List<String> nameList) {
        assert nameList != null;

        MyString.reduce(nameList, maxItems);
        Collections.sort(nameList);

        builder.reset();
        for (String name : nameList) {
            if (Maud.model.getSource().bones.hasBone(name)) {
                builder.addBone(name);
            } else {
                builder.addEllipsis(name);
            }
        }
        builder.show(ActionPrefix.selectSourceBone);
    }

    /**
     * Display a submenu for selecting spatials by name using the "select
     * spatial" action prefix.
     *
     * @param nameList list of names from which to select (not null)
     * @param includeNodes true &rarr; both nodes and geometries, false &rarr;
     * geometries only
     */
    void showSpatialSubmenu(List<String> nameList,
            boolean includeNodes) {
        assert nameList != null;

        MyString.reduce(nameList, maxItems);
        Collections.sort(nameList);

        builder.reset();
        for (String name : nameList) {
            if (Maud.model.target.hasGeometry(name)) {
                builder.addGeometry(name);
            } else if (includeNodes && Maud.model.target.hasNode(name)) {
                builder.addNode(name);
            }
        }
        if (includeNodes) {
            builder.show(ActionPrefix.selectSpatial);
        } else {
            builder.show(ActionPrefix.selectGeometry);
        }
    }

    /**
     * Display a "CGM -> Source model" menu.
     */
    void sourceCgm() {
        builder.reset();
        builder.add("Load");
        if (Maud.model.getSource().isLoaded()) {
            builder.add("Unload");
        }
        builder.show("select menuItem CGM -> Source model -> ");
    }
    // *************************************************************************
    // private methods

    /**
     * Display an "Settings -> Asset folders -> Add" menu.
     */
    private void addAssetFolder() {
        Map<String, File> fileMap = Misc.driveMap();
        String workPath = System.getProperty("user.dir");
        File work = new File(workPath);
        if (work.isDirectory()) {
            String absoluteDirPath = work.getAbsolutePath();
            absoluteDirPath = absoluteDirPath.replaceAll("\\\\", "/");
            File oldFile = fileMap.put(absoluteDirPath, work);
            assert oldFile == null : oldFile;
        }
        buildFolderMenu(fileMap);
        builder.show(ActionPrefix.newAssetFolder);
    }

    /**
     * Build an Animation menu.
     */
    private void buildAnimationMenu() {
        builder.addTool("Tool");
        if (Maud.model.target.bones.countBones() > 0) {
            builder.add("Load");
            builder.add("Add new");
            //builder.add("Unload");
        }
        builder.addTool("Tweening");
        if (Maud.model.target.animation.isReal()) {
            builder.add("Behead");
            builder.addDialog("Change duration");
            builder.addDialog("Delete");
            builder.add("Delete keyframes");
            builder.add("Insert keyframes");
            builder.addDialog("Reduce");
            builder.addDialog("Rename");
            builder.add("Truncate");
            builder.add("Wrap all tracks");
        }
        builder.addTool("Source tool"); // TODO submenu
        if (Maud.model.getSource().isLoaded()
                && Maud.model.getSource().bones.countBones() > 0) {
            builder.add("Load source");
        }
    }

    /**
     * Build a Bone menu.
     */
    private void buildBoneMenu() {
        builder.addTool("Tool");
        builder.add("Select");
        //builder.add("Deselect"); TODO
        builder.addTool("Rotate");
        builder.addTool("Scale");
        builder.addTool("Translate");
        if (Maud.model.target.bone.isSelected()) {
            //builder.add("Attach prop"); TODO
            builder.addDialog("Rename");
        }
        if (Maud.model.getSource().isLoaded()) {
            builder.add("Select source"); // TODO submenu
        }
    }

    /**
     * Build a "Bone -> Select" menu.
     */
    private void buildBoneSelectMenu() {
        builder.add("By name");

        int numBones = Maud.model.target.bones.countBones();
        if (numBones > 0) {
            builder.add("By parent");
        }

        int numRoots = Maud.model.target.bones.countRootBones();
        if (numRoots == 1) {
            builder.addBone("Root");
        } else if (numRoots > 1) {
            builder.add("Root");
        }

        int numTracks = Maud.model.target.animation.countBoneTracks();
        if (numTracks > 0) {
            builder.add("With track");
        }

        String sourceBoneName = Maud.model.getSource().bone.getName();
        String boneName = Maud.model.getMap().targetBoneName(sourceBoneName);
        if (boneName != null && Maud.model.target.bones.hasBone(boneName)) {
            builder.addBone("Mapped");
        }

        int numChildren = Maud.model.target.bone.countChildren();
        if (numChildren == 1) {
            builder.addBone("Child");
        } else if (numChildren > 1) {
            builder.add("Child");
        }

        boolean isSelected = Maud.model.target.bone.isSelected();
        boolean isRoot = Maud.model.target.bone.isRootBone();
        if (isSelected && !isRoot) {
            builder.addBone("Parent");
        }
        if (isSelected) {
            builder.addBone("Next");
            builder.addBone("Previous");
        }
    }

    /**
     * Build a CGM menu.
     */
    private void buildCgmMenu() {
        builder.addTool("Tool");
        builder.add("Load");
        builder.addDialog("Save");
        //builder.add("Export"); TODO
        builder.add("Source model");
    }

    /**
     * Build a "Map -> Load -> From classpath" menu.
     */
    private void buildClasspathMapMenu() {
        builder.reset();

        builder.addJme("PuppetToMhGame"); // 51 mappings
        builder.addJme("PuppetToSinbad"); // 50 mappings
        builder.addJme("SinbadToJaime"); // 52 mappings
        builder.addJme("SinbadToMhGame"); // 49 mappings
    }

    /**
     * Build a menu of the files (and subdirectories/subfolders) in the
     * specified directory/folder.
     *
     * @param folderPath file path to the directory/folder (not null)
     * @param prefix required name prefix (not null)
     */
    private void buildFolderMenu(String folderPath, String prefix) {
        assert folderPath != null;
        assert prefix != null;

        File file = new File(folderPath);
        File[] files = file.listFiles();
        if (files == null) {
            builder.reset();
            return;
        }
        /*
         * Generate a map from file names (with the specified prefix)
         * to file objects.
         */
        Map<String, File> fileMap = new TreeMap<>();
        for (File f : files) {
            String name = f.getName();
            if (name.startsWith(prefix)) {
                File oldFile = fileMap.put(name, f);
                assert oldFile == null : oldFile;
            }
        }
        File parent = file.getParentFile();
        if (parent != null) {
            if ("..".startsWith(prefix)) {
                File oldFile = fileMap.put("..", parent);
                assert oldFile == null : oldFile;
            }
        }

        buildFolderMenu(fileMap);
    }

    /**
     * Build a menu of the files (and subdirectories/subfolders) in the
     * specified map.
     *
     * @param fileMap map of files to include (not null)
     */
    private void buildFolderMenu(Map<String, File> fileMap) {
        assert fileMap != null;
        /*
         * Generate a list of file names (and prefixes) to display in the menu.
         */
        Set<String> nameSet = fileMap.keySet();
        int numNames = nameSet.size();
        List<String> nameList = new ArrayList<>(numNames);
        nameList.addAll(nameSet);
        MyString.reduce(nameList, maxItems);
        Collections.sort(nameList);
        /*
         * Build the menu.
         */
        builder.reset();
        for (String name : nameList) {
            if (fileMap.containsKey(name)) {
                File f = fileMap.get(name);
                if (f.isDirectory()) {
                    builder.addFolder(name);
                } else {
                    builder.addFile(name);
                }
            } else {
                builder.addEllipsis(name);
            }
        }
    }

    /**
     * Build a Help menu.
     */
    private void buildHelpMenu() {
        builder.addDialog("About Maud");
        builder.addDialog("License");
        //builder.add("Wiki"); TODO
        //builder.add("Javadoc"); TODO
        builder.add("Source");
        builder.add("JME3 homepage");
    }

    /**
     * Build a History menu.
     */
    private void buildHistoryMenu() {
        builder.addTool("Tool");
        //builder.add("Clear"); TODO
    }

    /**
     * Build a Keyframe menu.
     */
    private void buildKeyframeMenu() {
        builder.addTool("Tool");
        if (Maud.model.target.bone.hasTrack()) {
            if (!Maud.model.target.animation.isMoving()) {
                builder.add("Select");
                int frameIndex = Maud.model.target.track.findKeyframeIndex();
                if (frameIndex == -1) {
                    builder.add("Insert from pose");
                }
                if (frameIndex > 0) {
                    builder.add("Delete");
                    //builder.add("Move"); TODO
                }
            }
            builder.addDialog("Reduce track");
            builder.add("Wrap track");
        }
    }

    /**
     * Build a "CGM -> Load" or "CGM -> Source model -> Load" menu for selecting
     * an asset locator.
     */
    private void buildLocatorMenu() {
        builder.reset();
        List<String> pathList = Maud.model.getLocations().listAll();
        for (String path : pathList) {
            if (path.endsWith(".jar")) {
                builder.addJar(path);
            } else if (path.endsWith(".zip")) {
                builder.addZip(path);
            } else {
                builder.addFolder(path);
            }
        }
        builder.add("From classpath");
    }

    /**
     * Display a Map menu.
     */
    private void buildMapMenu() {
        builder.reset();
        builder.addTool("Tool");
        builder.addDialog("Load");
        if (Maud.model.getMap().countMappings() > 0) {
            builder.add("Invert");
            builder.add("Unload");
        }
        builder.addDialog("Save");
        builder.addTool("Twist tool");
    }

    /**
     * Build a Physics menu.
     */
    private void buildPhysicsMenu() {
        builder.addTool("Tool");
        builder.add("Add");
        builder.add("Mass");
        builder.add("Remove");
    }

    /**
     * Build a Settings menu.
     */
    private void buildSettingsMenu() {
        builder.add("Asset folders");
        //builder.add("Initial model"); TODO
        builder.add("Hotkeys");
        //builder.add("Locale"); TODO
    }

    /**
     * Build an SGC menu.
     */
    private void buildSgcMenu() {
        builder.addTool("Tool");
        builder.add("Select");
        builder.add("Add");
        if (Maud.model.target.sgc.isSelected()) {
            //builder.add("Deselect"); TODO
            builder.add("Delete");
        }
    }

    /**
     * Build a "Bone -> Select source" menu.
     */
    private void buildSourceBoneSelectMenu() {
        int numRoots = Maud.model.getSource().bones.countRootBones();
        if (numRoots == 1) {
            builder.addBone("Root");
        } else if (numRoots > 1) {
            builder.add("Root");
        }

        String targetBoneName = Maud.model.target.bone.getName();
        String boneName = Maud.model.getMap().sourceBoneName(targetBoneName);
        if (boneName != null
                && Maud.model.getSource().bones.hasBone(boneName)) {
            builder.addBone("Mapped");
        }
    }

    /**
     * Build a Spatial menu.
     */
    private void buildSpatialMenu() {
        builder.addTool("Tool");
        builder.add("Select");
        builder.addTool("Rotate");
        builder.addTool("Scale");
        builder.addTool("Translate");
        if (!Maud.model.target.spatial.isCgmRoot()) {
            builder.addTool("Delete");
        }
        builder.addTool("User data tool");
        //builder.addTool("Material"); TODO
    }

    /**
     * Build a "... -> Load -> From classpath" menu.
     */
    private void buildTestDataMenu() {
        builder.reset();
        /*
         * Add items for CG models included with Maud.
         * If haveTestdata is true, also for CG models in jme3-testdata.
         *
         * animated models:
         */
        if (haveTestdata) {
            builder.addOgre("Elephant");
        }
        builder.addJme("Jaime");
        builder.addOgre("MhGame");
        if (haveTestdata) {
            builder.addOgre("Ninja");
            builder.addOgre("Oto");
        }
        builder.addXbuf("Puppet");
        builder.addOgre("Sinbad");
        /*
         * non-animated models:
         */
        if (haveTestdata) {
            builder.addJme("Boat");
            builder.addJme("Buggy");
            builder.addOgre("Ferrari");
            builder.addOgre("HoverTank");
            builder.addOgre("MonkeyHead");
            builder.addOgre("Sign Post");
            builder.addOgre("SpaceCraft");
        }
        builder.addOgre("Sword");
        if (haveTestdata) {
            builder.addGeometry("Teapot");
            builder.addOgre("Tree");
        }
    }

    /**
     * Build a View menu.
     */
    private void buildViewMenu() {
        builder.add("Mode");
        ViewMode viewMode = Maud.model.misc.getViewMode();
        if (viewMode.equals(ViewMode.Scene)
                || viewMode.equals(ViewMode.Hybrid)) {
            builder.add("Scene options");
        }
        if (viewMode.equals(ViewMode.Score)
                || viewMode.equals(ViewMode.Hybrid)) {
            builder.add("Score options");
        }
    }

    /**
     * Display a menu of files or zip entries.
     *
     * @param names the list of names (not null, unaffected)
     * @param actionPrefix common prefix of the menu's action strings (not null,
     * usually the final character will be a blank)
     */
    private void selectFile(List<String> names, String actionPrefix) {
        assert names != null;
        assert actionPrefix != null;

        builder.reset();
        builder.addFiles(names, maxItems);
        builder.show(actionPrefix);
    }
}
