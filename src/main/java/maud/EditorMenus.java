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

/**
 * Menus in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class EditorMenus {
    // *************************************************************************
    // constants and loggers

    /**
     * maximum number of items in a menu
     */
    final private static int maxItems = 19;
    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            EditorMenus.class.getName());
    /**
     * magic filename used in "add locator" menus
     */
    final private static String addThis = "! add this folder";
    /**
     * level separator in menu action strings
     */
    final private static String menuSeparator = " -> ";
    // *************************************************************************
    // fields

    /**
     * reusable builder for popup menus
     */
    final private static MenuBuilder builder = new MenuBuilder();
    // *************************************************************************
    // new methods exposed

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
            showAnimationSubmenu(animationNames, cgm);
        }
    }

    /**
     * Handle a "load (source)cgm asset" action with arguments.
     *
     * @param args action arguments (not null, not empty)
     * @param cgm (not null)
     */
    void loadCgmAsset(String args, LoadedCgm cgm) {
        String menuPrefix = null;
        if (cgm == Maud.model.source) {
            menuPrefix = EditorInputMode.loadSourceCgmAssetPrefix;
        } else if (cgm == Maud.model.target) {
            menuPrefix = EditorInputMode.loadCgmAssetPrefix;
        } else {
            assert false;
        }

        String indexString = args.split(" ")[0];
        String rootPath = Maud.model.folders.pathForIndex(indexString);
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
            cgm.loadAsset(rootPath, assetPath);

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
     * Handle a "load (source)cgm locator" action with argument.
     *
     * @param path action argument (not null, not empty)
     * @param cgm (not null)
     */
    void loadCgmLocator(String path, LoadedCgm cgm) {
        if (path.equals("From classpath")) {
            buildTestDataMenu();
            String menuPrefix = null;
            if (cgm == Maud.model.source) {
                menuPrefix = EditorInputMode.loadSourceCgmNamedPrefix;
            } else if (cgm == Maud.model.target) {
                menuPrefix = EditorInputMode.loadCgmNamedPrefix;
            } else {
                assert false;
            }
            builder.show(menuPrefix);

        } else {
            String indexString = Maud.model.folders.indexForPath(path);
            String args = indexString + " " + "/";
            loadCgmAsset(args, cgm);
        }
    }

    /**
     * Display a "load mapping asset" action without arguments.
     */
    void loadMappingAsset() {
        buildLocatorMenu();
        builder.show(EditorInputMode.loadMappingLocatorPrefix);
    }

    /**
     * Handle a "load mapping asset" action with arguments.
     *
     * @param args action arguments (not null, not empty)
     */
    void loadMappingAsset(String args) {
        String menuPrefix = EditorInputMode.loadMappingAssetPrefix;
        String indexString = args.split(" ")[0];
        String rootPath = Maud.model.folders.pathForIndex(indexString);
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
            Maud.model.mapping.loadAsset(rootPath, assetPath);

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
     * Handle a "load mapping locator" action.
     *
     * @param path action argument (not null, not empty)
     * @param cgm (not null)
     */
    void loadMappingLocator(String path) {
        if (path.equals("From classpath")) {
            buildClasspathMappingMenu();
            builder.show(EditorInputMode.loadMappingNamedPrefix);

        } else {
            String indexString = Maud.model.folders.indexForPath(path);
            String args = indexString + " " + "/";
            loadMappingAsset(args);
        }
    }

    /**
     * Handle a "new locator" action with an argument.
     *
     * @param argument action argument (not null)
     */
    void newAssetFolder(String argument) {
        if (argument.endsWith(addThis)) {
            String path = MyString.removeSuffix(argument, addThis);
            Maud.model.folders.add(path);

        } else {
            Map<String, File> folderMap = folderMap(argument);
            buildFolderMenu(folderMap);

            File file = new File(argument);
            if (!file.isDirectory()) {
                file = file.getParentFile();
            }
            String folderPath = file.getAbsolutePath();
            String menuPrefix;
            menuPrefix = EditorInputMode.newAssetFolderPrefix + folderPath + "/";
            builder.show(menuPrefix);
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
            showBoneSubmenu(boneNames);
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
                showBoneSubmenu(boneNames);
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
            builder.show(EditorInputMode.selectBoneChildPrefix);
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
     * Handle a "select menuItem" action for the editor screen.
     *
     * @param menuPath path to menu item (not null)
     * @return true if the action is handled, otherwise false
     */
    boolean selectMenuItem(String menuPath) {
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
     * Handle a "select sourceBone" action with an argument.
     *
     * @param argument action argument (not null)
     */
    void selectSourceBone(String argument) {
        if (Maud.model.source.bones.hasBone(argument)) {
            Maud.model.source.bone.select(argument);
        } else {
            /*
             * Treat the argument as a bone-name prefix.
             */
            List<String> boneNames;
            boneNames = Maud.model.source.bones.listBoneNames(argument);
            showBoneSubmenu(boneNames);
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
            showSpatialSubmenu(names, includeNodes);
        }
    }

    /**
     * Handle a "select spatialChild" action with no argument.
     */
    void selectSpatialChild() {
        int numChildren = Maud.model.target.spatial.countChildren();
        if (numChildren == 1) {
            Maud.model.target.spatial.selectChild(0);

        } else if (numChildren > 1) {
            builder.reset();
            for (int childIndex = 0; childIndex < numChildren; childIndex++) {
                String choice = String.format("#%d", childIndex + 1);
                String name;
                name = Maud.model.target.spatial.getChildName(childIndex);
                if (name != null) {
                    choice += " " + MyString.quote(name);
                }
                boolean isANode;
                isANode = Maud.model.target.spatial.isChildANode(childIndex);
                if (isANode) {
                    builder.addNode(choice);
                } else {
                    builder.addGeometry(choice);
                }
            }
            builder.show(EditorInputMode.selectSpatialChildPrefix);
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
        builder.show(EditorInputMode.newUserKeyPrefix);
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
        builder.show(EditorInputMode.selectUserKeyPrefix);
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
        builder.show(EditorInputMode.setBatchHintPrefix);
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
        builder.show(EditorInputMode.setCullHintPrefix);
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
        builder.show(EditorInputMode.setQueueBucketPrefix);
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
        builder.show(EditorInputMode.setShadowModePrefix);
    }
    // *************************************************************************
    // private methods

    /**
     * Display a "Spatial -> Add control" menu.
     */
    private void addSgc() {
        builder.reset();
        builder.add("Anim");
        builder.add("RigidBody");
        builder.add("Skeleton");
        builder.show("select menuItem Spatial -> Add control -> ");
    }

    /**
     * Display a "CGM -> Asset folders" menu.
     */
    private void assetFolders() {
        builder.reset();
        builder.add("Add");
        if (Maud.model.folders.hasRemovable()) {
            builder.add("Remove");
        }
        builder.show("select menuItem CGM -> Asset folders -> ");
    }

    /**
     * Build an Animation menu.
     */
    private void buildAnimationMenu() {
        builder.addTool("Tool");
        builder.add("Load");
        builder.addDialog("New from pose");
        builder.addDialog("New from copy");
        builder.addTool("Source tool");
        if (Maud.model.source.isLoaded()) {
            builder.add("Load source");
        }
        builder.addTool("New from retarget");
        if (Maud.model.target.animation.isReal()) {
            builder.addDialog("Duration");
            builder.addDialog("Reduce");
            builder.addDialog("Rename");
            builder.add("Tweening");
            builder.addDialog("Delete");
        }
    }

    /**
     * Build a Bone menu.
     */
    private void buildBoneMenu() {
        builder.addTool("Tool");
        builder.add("Select");
        builder.addTool("Rotate");
        builder.addTool("Scale");
        builder.addTool("Translate");
        if (Maud.model.target.bone.isSelected()) {
            builder.add("Attach prop");
            builder.addDialog("Rename");
        }
        if (Maud.model.source.isLoaded()) {
            builder.add("Select source");
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

        String sourceBoneName = Maud.model.source.bone.getName();
        String boneName = Maud.model.mapping.targetBoneName(sourceBoneName);
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

        builder.add("Source model");
        builder.add("Skeleton mapping");
        builder.add("Asset folders");
        builder.addTool("History");
    }

    /**
     * Build a "CGM -> Skeleton mapping -> From classpath" menu.
     */
    private void buildClasspathMappingMenu() {
        builder.reset();

        builder.addJme("BallerinaToMhGame");
        builder.addJme("FlipToMhGame");
        builder.addJme("FlipToSinbad");
        builder.addJme("FooterToMhGame");
        builder.addJme("SinbadToJaime");
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
        builder.add("Wiki");
        builder.add("Javadoc");
        builder.add("Source");
        builder.add("JME3 homepage");
    }

    /**
     * Build a Keyframe menu.
     */
    private void buildKeyframeMenu() {
        builder.addTool("Tool");
        if (Maud.model.target.bone.hasTrack()) {
            builder.addDialog("Reduce");
            builder.add("Select by time");
            builder.add("Select first");
            builder.add("Select previous");
            builder.add("Select next");
            builder.add("Select last");
            builder.add("Move");
            builder.add("Copy");
            builder.add("New from pose");
            int frameIndex = Maud.model.target.track.findKeyframe();
            if (frameIndex > 0) {
                builder.add("Delete");
            }
        }
    }

    /**
     * Build a "CGM -> Load" or "CGM -> Source model -> Load" menu.
     */
    private void buildLocatorMenu() {
        builder.reset();
        List<String> pathList = Maud.model.folders.listAll();
        for (String path : pathList) {
            builder.addFolder(path);
        }
        builder.add("From classpath");
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
        builder.add("Initial model");
        builder.add("Hotkeys");
        builder.add("Locale");
    }

    /**
     * Build a "Bone -> Select source" menu.
     */
    private void buildSourceBoneSelectMenu() {
        int numRoots = Maud.model.source.bones.countRootBones();
        if (numRoots == 1) {
            builder.addBone("Root");
        } else if (numRoots > 1) {
            builder.add("Root");
        }

        String targetBoneName = Maud.model.target.bone.getName();
        String boneName = Maud.model.mapping.sourceBoneName(targetBoneName);
        if (boneName != null && Maud.model.source.bones.hasBone(boneName)) {
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
        builder.addTool("Control tool");
        builder.add("Select control");
        builder.add("Add control");
        if (Maud.model.target.sgc.isSelected()) {
            builder.add("Delete control");
        }
        builder.addTool("User data tool");
        builder.addTool("Material");
    }

    /**
     * Build a "... -> Load -> From classpath" menu.
     */
    private void buildTestDataMenu() {
        builder.reset();
        /*
         * Add items for the CG models in the jme3-testdata asset pack.
         *
         * animated models:
         */
        builder.addOgre("Elephant");
        builder.addJme("Jaime");
        builder.addOgre("Ninja");
        builder.addOgre("Oto");
        builder.addOgre("Sinbad");
        /*
         * non-animated models:
         */
        builder.addJme("Boat");
        builder.addJme("Buggy");
        builder.addOgre("Ferrari");
        builder.addOgre("HoverTank");
        builder.addOgre("MonkeyHead");
        builder.addOgre("Sign Post");
        builder.addOgre("SpaceCraft");
        builder.addGeometry("Teapot");
        builder.addOgre("Tree");
    }

    /**
     * Build a View menu.
     */
    private void buildViewMenu() {
        builder.add("Mode");
        String viewMode = Maud.model.misc.getViewMode();
        if (viewMode.equals("scene") || viewMode.equals("hybrid")) {
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
        }
        if (viewMode.equals("score") || viewMode.equals("hybrid")) {
            builder.addTool("Background");
            builder.addTool("Score");
        }
    }

    /**
     * Generate a map from drive paths (roots) to file objects.
     *
     * @return a new map of drives
     */
    private Map<String, File> driveMap() {
        Map<String, File> result = new TreeMap<>();

        File[] roots = File.listRoots();
        for (File root : roots) {
            if (root.isDirectory()) {
                String absoluteDirPath = root.getAbsolutePath();
                absoluteDirPath = absoluteDirPath.replaceAll("\\\\", "/");
                File oldFile = result.put(absoluteDirPath, root);
                assert oldFile == null : oldFile;
            }
        }

        return result;
    }

    /**
     * Generate a map from subfolder names (with the specified path prefix) to
     * file objects.
     *
     * @param pathPrefix the file path prefix (not null)
     * @return a new map of subfolders
     */
    private Map<String, File> folderMap(String pathPrefix) {
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
            if (f.isDirectory()) {
                String name = f.getName();
                if (name.startsWith(namePrefix)) {
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
     * Display a "Animation -> Load source" menu.
     */
    private void loadSourceAnimation() {
        if (Maud.model.source.isLoaded()) {
            List<String> animationNames;
            animationNames = Maud.model.source.listAnimationNames();
            showAnimationSubmenu(animationNames, Maud.model.source);
        }
    }

    /**
     * Display a "CGM -> Skeleton mapping" menu.
     */
    private void mapping() {
        builder.reset();
        builder.addTool("Tool");
        builder.addDialog("Load");
        if (Maud.model.mapping.countMappings() > 0) {
            builder.add("Invert");
            builder.add("Unload");
        }
        builder.addDialog("Save");
        builder.addTool("Twist tool");
        builder.show("select menuItem CGM -> Skeleton mapping -> ");
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

        boolean handled = true;
        switch (remainder) {
            case "Delete":
                Maud.gui.dialogs.deleteAnimation();
                break;
            case "Duration":
                Maud.gui.dialogs.setDuration();
                break;
            case "Load":
                List<String> animationNames;
                animationNames = Maud.model.target.listAnimationNames();
                showAnimationSubmenu(animationNames, Maud.model.target);
                break;
            case "Load source":
                loadSourceAnimation();
                break;
            case "New from copy":
                Maud.gui.dialogs.copyAnimation();
                break;
            case "New from pose":
                Maud.gui.dialogs.newPose();
                break;
            case "New from retarget":
                Maud.gui.tools.select("retarget");
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
            default:
                handled = false;
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the "CGM -> Asset folders" menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuAssetFolders(String remainder) {
        assert remainder != null;

        boolean handled = false;
        switch (remainder) {
            case "Add":
                Map<String, File> driveMap = driveMap();
                buildFolderMenu(driveMap);
                builder.show(EditorInputMode.newAssetFolderPrefix);
                handled = true;
                break;
            case "Remove":
                builder.reset();
                List<String> pathList = Maud.model.folders.listAll();
                for (String path : pathList) {
                    builder.addFolder(path);
                }
                builder.show(EditorInputMode.deleteAssetFolderPrefix);
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
    private boolean menuBar(String menuName) {
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
            String actionPrefix = EditorInputMode.selectMenuItemPrefix + menuName
                    + menuSeparator;
            builder.show(actionPrefix);
        }

        return true;
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
                    selectBone();
                    break;
                case "Select source":
                    selectSourceBone();
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
                Maud.model.mapping.selectFromSource();
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
                Maud.model.mapping.selectFromTarget();
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

        boolean handled = false;
        String folderPrefix = "Asset folders" + menuSeparator;
        String mappingPrefix = "Skeleton mapping" + menuSeparator;
        String sourcePrefix = "Source model" + menuSeparator;
        if (remainder.startsWith(folderPrefix)) {
            String selectArg = MyString.remainder(remainder, folderPrefix);
            handled = menuAssetFolders(selectArg);

        } else if (remainder.startsWith(mappingPrefix)) {
            String selectArg = MyString.remainder(remainder, mappingPrefix);
            handled = menuMapping(selectArg);

        } else if (remainder.startsWith(sourcePrefix)) {
            String selectArg = MyString.remainder(remainder, sourcePrefix);
            handled = menuSourceCgm(selectArg);

        } else {
            switch (remainder) {
                case "Asset folders":
                    assetFolders();
                    handled = true;
                    break;
                case "History":
                    Maud.gui.tools.select("history");
                    handled = true;
                    break;
                case "Load":
                    buildLocatorMenu();
                    builder.show(EditorInputMode.loadCgmLocatorPrefix);
                    handled = true;
                    break;
                case "Skeleton mapping":
                    mapping();
                    handled = true;
                    break;
                case "Save":
                    Maud.gui.dialogs.saveCgm();
                    handled = true;
                    break;
                case "Source model":
                    sourceCgm();
                    handled = true;
                    break;
                case "Tool":
                    Maud.gui.tools.select("cgm");
                    handled = true;
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
     * Handle a "select menuItem" action the Keyframe menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuKeyframe(String remainder) {
        assert remainder != null;

        boolean handled = true;
        switch (remainder) {
            case "Delete":
                Maud.model.target.animation.deleteSingleKeyframe();
                break;
            case "Reduce":
                Maud.gui.dialogs.reduceTrack();
                break;
            case "Select first":
                Maud.model.target.animation.selectKeyframeFirst();
                break;
            case "Select last":
                Maud.model.target.animation.selectKeyframeLast();
                break;
            case "Select next":
                Maud.model.target.animation.selectKeyframeNext();
                break;
            case "Select previous":
                Maud.model.target.animation.selectKeyframePrevious();
                break;
            case "Tool":
                Maud.gui.tools.select("keyframe");
                break;
            default:
                handled = false;
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the "CGM -> Skeleton mapping"
     * menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuMapping(String remainder) {
        assert remainder != null;

        boolean handled = false;
        switch (remainder) {
            case "Invert":
                Maud.model.mapping.invert();
                handled = true;
                break;
            case "Load":
                loadMappingAsset();
                handled = true;
                break;
            case "Save":
                Maud.gui.dialogs.saveMapping();
                handled = true;
                break;
            case "Tool":
                Maud.gui.tools.select("mapping");
                handled = true;
                break;
            case "Twist tool":
                Maud.gui.tools.select("twist");
                handled = true;
                break;
            case "Unload":
                Maud.model.mapping.unload();
                handled = true;
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
                buildLocatorMenu();
                builder.show(EditorInputMode.loadSourceCgmLocatorPrefix);
                handled = true;
                break;

            case "Unload":
                Maud.model.source.unload();
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
                    addSgc();
                    break;
                case "Control tool":
                    Maud.gui.tools.select("sgc");
                    break;
                case "Delete control":
                    Maud.gui.dialogs.deleteSgc();
                    break;
                case "Rotate":
                    Maud.gui.tools.select("spatialRotation");
                    break;
                case "Scale":
                    Maud.gui.tools.select("spatialScale");
                    break;
                case "Select":
                    selectSpatial();
                    break;
                case "Select control":
                    selectSgc();
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

        boolean handled = false;
        switch (remainder) {
            case "By name":
                selectSpatial("", true);
                handled = true;
                break;
            case "Child":
                selectSpatialChild();
                handled = true;
                break;
            case "Geometry":
                selectSpatial("", false);
                handled = true;
                break;
            case "Parent":
                Maud.model.target.spatial.selectParent();
                handled = true;
                break;
            case "Root":
                Maud.model.target.spatial.selectModelRoot();
                handled = true;
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
        if (remainder.startsWith(modePrefix)) {
            String arg = MyString.remainder(remainder, modePrefix);
            handled = menuViewMode(arg);

        } else {
            switch (remainder) {
                case "Axes":
                    Maud.gui.tools.select("axes");
                    break;
                case "Background":
                    Maud.gui.tools.select("background");
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
                    selectViewMode();
                    break;
                case "Physics":
                    handled = false;
                    break;
                case "Platform":
                    Maud.gui.tools.select("platform");
                    break;
                case "Render":
                    Maud.gui.tools.select("render");
                    break;
                case "Score":
                    Maud.gui.tools.select("score");
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
     * Handle a "select bone" action without an argument.
     */
    private void selectBone() {
        builder.reset();
        buildBoneSelectMenu();
        builder.show("select menuItem Bone -> Select -> ");
    }

    /**
     * Select a bone by name, using submenus.
     */
    private void selectBoneByName() {
        List<String> nameList = Maud.model.target.bones.listBoneNames();
        showBoneSubmenu(nameList);
    }

    /**
     * Select a bone by parent, using submenus.
     */
    private void selectBoneByParent() {
        List<String> boneNames = Maud.model.target.bones.listRootBoneNames();
        Maud.gui.showPopupMenu(EditorInputMode.selectBoneChildPrefix, boneNames);
    }

    /**
     * Handle a "select rootBone" action.
     */
    private void selectRootBone() {
        int numRoots = Maud.model.target.bones.countRootBones();
        if (numRoots == 1) {
            Maud.model.target.bone.selectFirstRoot();
        } else if (numRoots > 1) {
            List<String> boneNames = Maud.model.target.bones.listRootBoneNames();
            showBoneSubmenu(boneNames);
        }
    }

    /**
     * Display a "Spatial -> Select control" menu.
     */
    private void selectSgc() {
        builder.reset();
        for (String name : Maud.model.target.spatial.listSgcNames()) {
            builder.add(name);
        }
        builder.add(LoadedCgm.noControl);
        builder.show(EditorInputMode.selectControlPrefix);
    }

    /**
     * Display a "Bone -> Select source" menu.
     */
    private void selectSourceBone() {
        if (Maud.model.source.isLoaded()) {
            builder.reset();
            buildSourceBoneSelectMenu();
            builder.show("select menuItem Bone -> Select source -> ");
        }
    }

    /**
     * Handle a "select sourceRootBone" action.
     */
    private void selectSourceRootBone() {
        int numRoots = Maud.model.source.bones.countRootBones();
        if (numRoots == 1) {
            Maud.model.source.bone.selectFirstRoot();
        } else if (numRoots > 1) {
            List<String> names = Maud.model.source.bones.listRootBoneNames();
            showSourceBoneSubmenu(names);
        }
    }

    /**
     * Display a "Spatial -> Select" menu.
     */
    private void selectSpatial() {
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

        boolean isRoot = Maud.model.target.spatial.isModelRoot();
        if (!isRoot) {
            builder.addNode("Parent");
        }

        builder.show("select menuItem Spatial -> Select -> ");
    }

    /**
     * Handle a "select viewMode" action without an argument.
     */
    private void selectViewMode() {
        builder.reset();
        String viewMode = Maud.model.misc.getViewMode();
        if (!viewMode.equals("scene")) {
            builder.add("Scene");
        }
        if (!viewMode.equals("score")) {
            builder.add("Score");
        }
        if (!viewMode.equals("hybrid")) {
            builder.add("Hybrid");
        }
        builder.show("select menuItem View -> Mode -> ");
    }

    /**
     * Display a submenu for selecting a target animation by name using the
     * "select (source)animation" action prefix.
     *
     * @param nameList list of names from which to select (not null)
     * @param cgm which load slot (not null)
     */
    private void showAnimationSubmenu(List<String> nameList, LoadedCgm cgm) {
        assert nameList != null;
        assert cgm != null;

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
            builder.show(EditorInputMode.loadAnimationPrefix);
        } else if (cgm == Maud.model.source) {
            builder.show(EditorInputMode.loadSourceAnimationPrefix);
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
    private void showBoneSubmenu(List<String> nameList) {
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
        builder.show(EditorInputMode.selectBonePrefix);
    }

    /**
     * Display a submenu for selecting a source bone by name using the "select
     * sourceBone" action prefix.
     *
     * @param nameList list of names from which to select (not null)
     */
    private void showSourceBoneSubmenu(List<String> nameList) {
        assert nameList != null;

        MyString.reduce(nameList, maxItems);
        Collections.sort(nameList);

        builder.reset();
        for (String name : nameList) {
            if (Maud.model.source.bones.hasBone(name)) {
                builder.addBone(name);
            } else {
                builder.addEllipsis(name);
            }
        }
        builder.show(EditorInputMode.selectSourceBonePrefix);
    }

    /**
     * Display a submenu for selecting spatials by name using the "select
     * spatial" action prefix.
     *
     * @param nameList list of names from which to select (not null)
     * @param includeNodes true &rarr; both nodes and geometries, false &rarr;
     * geometries only
     */
    private void showSpatialSubmenu(List<String> nameList,
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
            } else {
                builder.add(name);
            }
        }
        if (includeNodes) {
            builder.show(EditorInputMode.selectSpatialPrefix);
        } else {
            builder.show(EditorInputMode.selectGeometryPrefix);
        }
    }

    /**
     * Display a "CGM -> Source model" menu.
     */
    private void sourceCgm() {
        builder.reset();

        builder.add("Load");
        if (Maud.model.source.isLoaded()) {
            builder.add("Unload");
        }

        builder.show("select menuItem CGM -> Source model -> ");
    }
}
