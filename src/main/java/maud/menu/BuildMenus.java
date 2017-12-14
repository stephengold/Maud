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
import maud.dialog.EditorDialogs;
import maud.model.EditorModel;
import maud.model.LoadedMap;
import maud.model.cgm.Cgm;
import maud.model.cgm.LoadedCgm;
import maud.model.cgm.SelectedBone;
import maud.model.option.ViewMode;

/**
 * Build menus in Maud's editor screen. TODO split off methods that don't need
 * the reusable builder
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
     * maximum number of items in a menu, derived from the minimum display
     * height of 480 pixels
     */
    final private static int maxItems = 19;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(BuildMenus.class.getName());
    /**
     * magic specifier for the default location in actions and menus
     */
    final private static String defaultLocation = "from classpath";
    /**
     * magic specifier for a source-identity map in actions and menus
     */
    final private static String identityForSource = "Identity for source";
    /**
     * magic specifier for a target-identity map in actions and menus
     */
    final private static String identityForTarget = "Identity for target";
    /**
     * magic name for an asset whose path is specified in a dialog box
     */
    final public static String otherName = "other";
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
     * @param loadedCgm load slot (not null)
     */
    public void loadCgmAsset(String args, LoadedCgm loadedCgm) {
        EditorModel model = Maud.getModel();
        String menuPrefix = null;
        if (loadedCgm == model.getSource()) {
            menuPrefix = ActionPrefix.loadSourceCgmAsset;
        } else if (loadedCgm == model.getTarget()) {
            menuPrefix = ActionPrefix.loadCgmAsset;
        } else {
            throw new IllegalArgumentException();
        }

        String indexString = args.split(" ")[0];
        String spec = model.getLocations().specForIndex(indexString);
        String assetPath = MyString.remainder(args, indexString + " ");

        if (spec == null || !spec.startsWith("file:///")) { // won't browse
            loadedCgm.loadAsset(spec, assetPath);
            return;
        }
        String rootPath = MyString.remainder(spec, "file:///");

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
                loadedCgm.loadAsset(spec, assetPath);
            } else if (!cgmEntries.isEmpty()) {
                ShowMenus.selectFile(cgmEntries,
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
                loadedCgm.loadAsset(spec, assetPath);

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
     * @param arg action argument (not null, not empty)
     * @param slot load slot (not null)
     */
    public void loadCgmLocator(String arg, LoadedCgm slot) {
        if (arg.equals(defaultLocation)) {
            buildTestDataMenu();
            if (slot == Maud.getModel().getSource()) {
                builder.show(ActionPrefix.loadSourceCgmNamed);
            } else if (slot == Maud.getModel().getTarget()) {
                builder.show(ActionPrefix.loadCgmNamed);
            } else {
                throw new IllegalArgumentException();
            }

        } else if (arg.startsWith("file://") || arg.endsWith(".jar")
                || arg.endsWith(".zip")) {
            String indexString;
            indexString = Maud.getModel().getLocations().indexForSpec(arg);
            String args = indexString + " /";
            loadCgmAsset(args, slot);

        } else {
            EditorDialogs.loadCgmAsset(arg, slot);
        }
    }

    /**
     * Display a "load map asset" action without arguments.
     */
    public void loadMapAsset() {
        buildLocatorMenu();
        if (Maud.getModel().getSource().getSkeleton().isSelected()) {
            builder.add(identityForSource);
        }
        if (Maud.getModel().getTarget().getSkeleton().isSelected()) {
            builder.add(identityForTarget);
        }
        builder.show(ActionPrefix.loadMapLocator);
    }

    /**
     * Handle a "load map asset" action with arguments.
     *
     * @param args action arguments (not null, not empty)
     */
    public void loadMapAsset(String args) {
        EditorModel model = Maud.getModel();
        String indexString = args.split(" ")[0];
        String spec = model.getLocations().specForIndex(indexString);
        String assetPath = MyString.remainder(args, indexString + " ");

        if (spec == null || !spec.startsWith("file:///")) { // won't browse
            model.getMap().loadAsset(spec, assetPath);
            return;
        }
        String rootPath = MyString.remainder(spec, "file:///");

        // TODO browse JAR/ZIP files to find maps
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
            model.getMap().loadAsset(spec, assetPath);

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

    /**
     * Handle a "load map locator" action.
     *
     * @param arg action argument (not null, not empty)
     */
    public void loadMapLocator(String arg) {
        EditorModel model = Maud.getModel();
        switch (arg) {
            case defaultLocation:
                buildClasspathMapMenu();
                builder.show(ActionPrefix.loadMapNamed);
                break;

            case identityForSource:
                Cgm source = model.getSource();
                model.getMap().loadIdentityFor(source);
                break;

            case identityForTarget:
                Cgm target = model.getTarget();
                model.getMap().loadIdentityFor(target);
                break;

            default:
                if (arg.startsWith("file://") || arg.endsWith(".jar")
                        || arg.endsWith(".zip")) {
                    String indexString;
                    indexString = model.getLocations().indexForSpec(arg);
                    String args = indexString + " /";
                    loadMapAsset(args);
                } else {
                    EditorDialogs.loadMapAsset(arg);
                }
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
     * Handle a "select menuItem" action from the "Settings -> Asset locations"
     * menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    boolean menuAssetLocations(String remainder) {
        assert remainder != null;

        boolean handled = false;
        switch (remainder) {
            case "Add":
                addAssetLocation();
                handled = true;
                break;

            case "Remove":
                ShowMenus.removeAssetLocation();
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
                PhysicsMenus.buildPhysicsMenu(builder);
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
     * Handle a "new assetLocation" action with an argument.
     *
     * @param argument action argument (not null)
     */
    public void newAssetLocation(String argument) {
        if (argument.endsWith(EditorMenus.addThis)) {
            String path = MyString.removeSuffix(argument, EditorMenus.addThis);
            Maud.getModel().getLocations().addFilesystem(path);

        } else if (argument.endsWith(".jar") || argument.endsWith(".zip")) {
            Maud.getModel().getLocations().addFilesystem(argument);

        } else { // open folder
            Map<String, File> folderMap = EditorMenus.folderMap(argument);
            buildFolderMenu(folderMap);

            File file = new File(argument);
            if (!file.isDirectory()) {
                file = file.getParentFile();
            }
            String folderPath = file.getAbsolutePath();
            String prefix = ActionPrefix.newAssetLocation + folderPath + "/";
            builder.show(prefix);
        }
    }

    /**
     * Handle a "select boneWithTrack" action.
     */
    void selectBoneWithTrack() {
        Cgm target = Maud.getModel().getTarget();
        List<String> boneNames = target.getAnimation().listTrackedBones();
        int numBoneTracks = boneNames.size();
        if (numBoneTracks == 1) {
            target.getBone().select(boneNames.get(0));
        } else if (numBoneTracks > 1) {
            ShowMenus.showBoneSubmenu(boneNames);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Display a "Settings -> Asset locations -> Add" menu.
     */
    private void addAssetLocation() {
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
        builder.show(ActionPrefix.newAssetLocation);
    }

    /**
     * Build an Animation menu.
     */
    private void buildAnimationMenu() {
        builder.addTool("Tool");
        Cgm target = Maud.getModel().getTarget();
        if (target.getAnimControl().isSelected()) {
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
        Cgm source = Maud.getModel().getSource();
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
        builder.addTool("Rotate");
        builder.addTool("Scale");
        builder.addTool("Translate");

        EditorModel model = Maud.getModel();
        SelectedBone selectedBone = model.getTarget().getBone();
        if (selectedBone.isSelected()) {
            boolean hasAttachments = selectedBone.hasAttachmentsNode();
            if (hasAttachments) {
                builder.addEdit("Delete attachments");
            } else {
                builder.addEdit("Attach node");
            }
            builder.add("Deselect");
            builder.addDialog("Rename");
        }
        if (model.getSource().isLoaded()) {
            builder.add("Select source"); // TODO submenu
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
     * Build a "Map -> Load -> defaultLocation" menu.
     */
    private void buildClasspathMapMenu() {
        builder.reset();

        builder.addJme("PuppetToMhGame"); // 51 mappings
        builder.addJme("PuppetToSinbad"); // 50 mappings
        builder.addJme("SinbadToJaime"); // 52 mappings
        builder.addJme("SinbadToMhGame"); // 49 mappings

        builder.addDialog(otherName);
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
        List<String> nameList = new ArrayList<>(nameSet);
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
        Cgm target = Maud.getModel().getTarget();
        if (target.getBone().hasTrack() && !target.getAnimation().isMoving()) {
            builder.add("Select");
            int frameIndex = target.getTrack().findKeyframeIndex();
            if (frameIndex == -1) {
                builder.addEdit("Insert from pose");
            } else {
                builder.addEdit("Replace with pose");
            }
            if (frameIndex > 0) {
                builder.addEdit("Delete selected");
                //builder.add("Move"); TODO
            }
            if (frameIndex > 1) {
                builder.addDialog("Delete previous");
            }
            int lastFrame = target.getTrack().countKeyframes() - 1;
            if (frameIndex != -1 && frameIndex < lastFrame) {
                builder.addDialog("Delete next");
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
        builder.add(defaultLocation);
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
     * Build a Settings menu.
     */
    private void buildSettingsMenu() {
        builder.addTool("Tool");
        builder.add("Asset locations");
        builder.addTool("Display-settings tool");
        builder.add("Hotkeys");
        //builder.add("Locale"); TODO
        builder.addTool("Tweening");
        builder.add("Update startup script");
    }

    /**
     * Build an SGC menu.
     */
    private void buildSgcMenu() {
        builder.addTool("Tool");
        builder.add("Select");
        builder.add("Add new");
        if (Maud.getModel().getTarget().getSgc().isSelected()) {
            builder.addEdit("Delete");
            builder.add("Deselect");
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
        Cgm target = Maud.getModel().getTarget();
        if (!target.getSpatial().isCgmRoot()) {
            builder.addEdit("Delete");
        }
        if (target.hasExtraSpatials()) {
            builder.addEdit("Delete extras");
        }
    }

    /**
     * Build a "... -> Load -> defaultLocation" menu.
     */
    private void buildTestDataMenu() {
        builder.reset();
        /*
         * Add items for C-G models included (on the classpath) with Maud.
         * If haveTestdata is true, also for C-G models in jme3-testdata.
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

        builder.addDialog(otherName);
    }

    /**
     * Build a Track menu.
     */
    private void buildTrackMenu() {
        builder.addTool("Tool");
        builder.add("Load animation");
        builder.add("Select bone");
        Cgm target = Maud.getModel().getTarget();
        SelectedBone bone = target.getBone();
        if (bone.hasTrack()) {
            builder.addEdit("Delete");
            builder.addDialog("Reduce");
            builder.addDialog("Resample at rate");
            builder.addDialog("Resample to number");
            builder.addEdit("Smooth");
            builder.addEdit("Translate for support");
            builder.addEdit("Translate for traction");
            if (target.getTrack().endsWithKeyframe()) {
                builder.addDialog("Wrap");
            } else {
                builder.addEdit("Wrap");
            }
        } else if (bone.isSelected() && target.getAnimation().isReal()) {
            builder.addEdit("Create");
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
