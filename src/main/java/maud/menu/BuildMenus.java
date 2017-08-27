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
import maud.Maud;
import maud.action.ActionPrefix;
import maud.model.EditorModel;
import maud.model.LoadedCgm;
import maud.model.LoadedMap;
import maud.model.MiscStatus;
import maud.model.SelectedSkeleton;
import maud.model.ViewMode;

/**
 * Build menus in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class BuildMenus {
    // *************************************************************************
    // constants and loggers

    /**
     * true if jme3-testdata assets are available on the classpath
     */
    final private static boolean haveTestdata = false;
    /**
     * maximum number of items in a menu, determined by minimum screen height
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
     * Handle a "load cgm asset" action without arguments.
     */
    public void loadCgm() {
        buildLocatorMenu();
        builder.show(ActionPrefix.loadCgmLocator);
    }

    /**
     * Handle a "load (source)cgm asset" action with arguments.
     *
     * @param args action arguments (not null, not empty)
     * @param cgm (not null)
     */
    public void loadCgmAsset(String args, LoadedCgm cgm) {
        String menuPrefix = null;
        if (cgm == Maud.getModel().getSource()) {
            menuPrefix = ActionPrefix.loadSourceCgmAsset;
        } else if (cgm == Maud.getModel().getTarget()) {
            menuPrefix = ActionPrefix.loadCgmAsset;
        } else {
            throw new IllegalArgumentException();
        }

        String indexString = args.split(" ")[0];
        String rootPath;
        rootPath = Maud.getModel().getLocations().pathForIndex(indexString);
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
                Maud.gui.showMenus.selectFile(cgmEntries,
                        menuPrefix + indexString + " ");
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
    public void loadCgmLocator(String path, LoadedCgm cgm) {
        if (path.equals("From classpath")) {
            buildTestDataMenu();
            String menuPrefix = null;
            if (cgm == Maud.getModel().getSource()) {
                menuPrefix = ActionPrefix.loadSourceCgmNamed;
            } else if (cgm == Maud.getModel().getTarget()) {
                menuPrefix = ActionPrefix.loadCgmNamed;
            } else {
                throw new IllegalArgumentException();
            }
            builder.show(menuPrefix);

        } else {
            String indexString;
            indexString = Maud.getModel().getLocations().indexForPath(path);
            String args = indexString + " /";
            loadCgmAsset(args, cgm);
        }
    }

    /**
     * Display a "load map asset" action without arguments.
     */
    public void loadMapAsset() {
        buildLocatorMenu();
        if (Maud.getModel().getSource().getSkeleton().isSelected()) {
            builder.add("Identity for source");
        }
        if (Maud.getModel().getTarget().getSkeleton().isSelected()) {
            builder.add("Identity for target");
        }
        builder.show(ActionPrefix.loadMapLocator);
    }

    /**
     * Handle a "load map asset" action with arguments.
     *
     * @param args action arguments (not null, not empty)
     */
    public void loadMapAsset(String args) {
        String indexString = args.split(" ")[0];
        String rootPath;
        rootPath = Maud.getModel().getLocations().pathForIndex(indexString);
        String assetPath = MyString.remainder(args, indexString + " ");

        File file = new File(rootPath, assetPath);
        String menuPrefix = ActionPrefix.loadMapAsset;
        if (file.isDirectory()) {
            String folderPath = file.getAbsolutePath();
            buildFolderMenu(folderPath, "");
            menuPrefix += args;
            if (!menuPrefix.endsWith("/")) {
                menuPrefix += "/";
            }
            builder.show(menuPrefix);

        } else if (file.canRead()) {
            Maud.getModel().getMap().loadAsset(rootPath, assetPath);

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
     */
    public void loadMapLocator(String path) {
        switch (path) {
            case "From classpath":
                buildClasspathMapMenu();
                builder.show(ActionPrefix.loadMapNamed);
                break;

            case "Identity for source":
                Maud.getModel().getMap().loadIdentityForSource();
                break;

            case "Identity for target":
                Maud.getModel().getMap().loadIdentityForTarget();
                break;

            default:
                String indexString;
                indexString = Maud.getModel().getLocations().indexForPath(path);
                String args = indexString + " /";
                loadMapAsset(args);
        }
    }

    /**
     * Handle a "load sourceCgm asset" action without arguments.
     */
    public void loadSourceCgm() {
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
                List<String> pathList;
                pathList = Maud.getModel().getLocations().listAll();
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
            case "Track":
                buildTrackMenu();
                break;
            case "Vertex":
                buildVertexMenu();
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
                    + EditorMenus.menuPathSeparator;
            builder.show(actionPrefix);
        }

        return true;
    }

    /**
     * Handle a "new locator" action with an argument.
     *
     * @param argument action argument (not null)
     */
    public void newAssetFolder(String argument) {
        if (argument.endsWith(EditorMenus.addThis)) {
            String path = MyString.removeSuffix(argument, EditorMenus.addThis);
            Maud.getModel().getLocations().add(path);

        } else if (argument.endsWith(".jar") || argument.endsWith(".zip")) {
            Maud.getModel().getLocations().add(argument);

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
     * Handle a "select bone" action without an argument.
     */
    public void selectBone() {
        builder.reset();
        buildBoneSelectMenu();
        builder.show("select menuItem Bone -> Select -> ");
    }

    /**
     * Handle a "select boneWithTrack" action.
     */
    void selectBoneWithTrack() {
        LoadedCgm target = Maud.getModel().getTarget();
        List<String> boneNames = target.getAnimation().listBonesWithTrack();
        int numBoneTracks = boneNames.size();
        if (numBoneTracks == 1) {
            target.getBone().select(boneNames.get(0));
        } else if (numBoneTracks > 1) {
            Maud.gui.showMenus.showBoneSubmenu(boneNames);
        }
    }

    /**
     * Display a "Bone -> Select source" menu.
     */
    public void selectSourceBone() {
        if (Maud.getModel().getSource().isLoaded()) {
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
        LoadedCgm source = Maud.getModel().getSource();
        SelectedSkeleton skeleton = source.getSkeleton();
        if (skeleton.hasBone(argument)) {
            source.getBone().select(argument);
        } else {
            /*
             * Treat the argument as a bone-name prefix.
             */
            List<String> boneNames = skeleton.listBoneNames(argument);
            Maud.gui.showMenus.showBoneSubmenu(boneNames);
        }
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
        LoadedCgm target = Maud.getModel().getTarget();
        if (target.getSkeleton().countBones() > 0) {
            builder.add("Load");
            builder.add("Add new");
            //builder.add("Unload");
        }

        if (target.getAnimation().isReal()) {
            builder.add("Edit");
            builder.addDialog("Rename");
            builder.addDialog("Delete");
        }

        builder.add("Select AnimControl");
        
        builder.addTool("Source tool"); // TODO submenu
        LoadedCgm source = Maud.getModel().getSource();
        if (source.isLoaded() && source.getSkeleton().countBones() > 0) {
            builder.add("Load source");
        }
        builder.addTool("Tweening");
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
        if (Maud.getModel().getTarget().getBone().isSelected()) {
            //builder.add("Attach prop"); TODO
            builder.addDialog("Rename");
        }
        if (Maud.getModel().getSource().isLoaded()) {
            builder.add("Select source"); // TODO submenu
        }
    }

    /**
     * Build a "Bone -> Select" menu.
     */
    private void buildBoneSelectMenu() {
        LoadedCgm target = Maud.getModel().getTarget();
        builder.add("By name");
        int numBones = target.getSkeleton().countBones();
        if (numBones > 0) {
            builder.add("By parent");
        }

        int numRoots = target.getSkeleton().countRootBones();
        if (numRoots == 1) {
            builder.addBone("Root");
        } else if (numRoots > 1) {
            builder.add("Root");
        }

        int numTracks = target.getAnimation().countBoneTracks();
        if (numTracks > 0) {
            builder.add("With track");
        }

        String sourceBoneName = Maud.getModel().getSource().getBone().getName();
        String boneName;
        boneName = Maud.getModel().getMap().targetBoneName(sourceBoneName);
        if (boneName != null && target.getSkeleton().hasBone(boneName)) {
            builder.addBone("Mapped");
        }

        int numChildren = target.getBone().countChildren();
        if (numChildren == 1) {
            builder.addBone("Child");
        } else if (numChildren > 1) {
            builder.add("Child");
        }

        boolean isSelected = target.getBone().isSelected();
        boolean isRoot = target.getBone().isRootBone();
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
        builder.add("Clear");
    }

    /**
     * Build a Keyframe menu.
     */
    private void buildKeyframeMenu() {
        builder.addTool("Tool");
        LoadedCgm target = Maud.getModel().getTarget();
        if (target.getBone().hasTrack() && !target.getAnimation().isMoving()) {
            builder.add("Select");
            int frameIndex = target.getTrack().findKeyframeIndex();
            if (frameIndex == -1) {
                builder.addEdit("Insert from pose");
            }
            if (frameIndex > 0) {
                builder.addEdit("Delete");
                //builder.add("Move"); TODO
            }
        }
    }

    /**
     * Build a "CGM -> Load" or "CGM -> Source model -> Load" menu for selecting
     * an asset locator.
     */
    private void buildLocatorMenu() {
        builder.reset();
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
        builder.add("From classpath");
    }

    /**
     * Display a Map menu.
     */
    private void buildMapMenu() {
        builder.reset();
        builder.addTool("Tool");
        builder.addDialog("Load");
        LoadedMap map = Maud.getModel().getMap();
        if (!map.isEmpty()) {
            if (map.hasInvalidMappings()) {
                builder.addEdit("Delete invalid mappings");
            }
            builder.addEdit("Invert");
            builder.addEdit("Unload");
        }
        builder.addDialog("Save");
        builder.addTool("Twist tool");
    }

    /**
     * Build a Physics menu.
     */
    private void buildPhysicsMenu() {
        builder.addTool("Tool");
        builder.addEdit("Add");
        //builder.add("Mass"); TODO
        //builder.add("Remove"); TODO
    }

    /**
     * Build a Settings menu.
     */
    private void buildSettingsMenu() {
        builder.add("Asset folders");
        MiscStatus status = Maud.getModel().getMisc();
        boolean diagnoseLoads = status.getDiagnoseLoads();
        if (!diagnoseLoads) {
            builder.add("Diagnose loads");
        }
        //builder.add("Initial model"); TODO
        builder.add("Hotkeys");
        //builder.add("Locale"); TODO
        int indexBase = status.getIndexBase();
        if (indexBase == 0) {
            builder.add("Start indices at 1");
        } else {
            builder.add("Start indices at 0");
        }
        if (diagnoseLoads) {
            builder.add("Stop diagnosing loads");
        }
    }

    /**
     * Build an SGC menu.
     */
    private void buildSgcMenu() {
        builder.addTool("Tool");
        builder.add("Select");
        builder.add("Add");
        if (Maud.getModel().getTarget().getSgc().isSelected()) {
            //builder.add("Deselect"); TODO
            builder.addEdit("Delete");
        }
    }

    /**
     * Build a "Bone -> Select source" menu.
     */
    private void buildSourceBoneSelectMenu() {
        EditorModel model = Maud.getModel();
        int numRoots = model.getSource().getSkeleton().countRootBones();
        if (numRoots == 1) {
            builder.addBone("Root");
        } else if (numRoots > 1) {
            builder.add("Root");
        }

        String targetBoneName = model.getTarget().getBone().getName();
        String boneName = model.getMap().sourceBoneName(targetBoneName);
        if (boneName != null
                && model.getSource().getSkeleton().hasBone(boneName)) {
            builder.addBone("Mapped");
        }
    }

    /**
     * Build a Spatial menu.
     */
    private void buildSpatialMenu() {
        builder.addTool("Tool");
        builder.add("Select");
        builder.addTool("Details");
        //builder.addTool("Material"); TODO
        //builder.addTool("Mesh"); TODO
        builder.addTool("Rotate");
        builder.addTool("Scale");
        builder.addTool("Translate");
        //builder.addTool("User data tool");
        LoadedCgm target = Maud.getModel().getTarget();
        if (!target.getSpatial().isCgmRoot()) {
            builder.addEdit("Delete");
        }
        if (target.hasExtraSpatials()) {
            builder.addEdit("Delete extras");
        }
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
     * Build a Track menu.
     */
    private void buildTrackMenu() {
        builder.addTool("Tool");
        LoadedCgm target = Maud.getModel().getTarget();
        if (target.getBone().hasTrack()) {
            builder.addDialog("Reduce");
            builder.addDialog("Resample");
            builder.addEdit("Translate for support");
            builder.addEdit("Translate for traction");
            builder.addEdit("Wrap");
        }
    }

    /**
     * Build a Vertex menu.
     */
    private void buildVertexMenu() {
        builder.addTool("Tool");
        if (Maud.getModel().getTarget().getSpatial().countVertices() > 0) {
            builder.add("Select");
        }
        builder.add("Select geometry");
    }

    /**
     * Build a View menu.
     */
    private void buildViewMenu() {
        builder.add("Mode");
        ViewMode viewMode = Maud.getModel().getMisc().getViewMode();
        if (viewMode.equals(ViewMode.Scene)
                || viewMode.equals(ViewMode.Hybrid)) {
            builder.add("Scene options");
        }
        if (viewMode.equals(ViewMode.Score)
                || viewMode.equals(ViewMode.Hybrid)) {
            builder.add("Score options");
        }
    }
}
