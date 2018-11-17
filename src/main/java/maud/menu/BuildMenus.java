/*
 Copyright (c) 2017-2018, Stephen Gold
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
import maud.model.cgm.SelectedTexture;

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
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(BuildMenus.class.getName());
    /**
     * magic name for classpath assets whose paths are entered via a dialog box
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
     * Display a "Settings -> Add asset location" menu.
     */
    void addAssetLocation() {
        Map<String, File> fileMap = Misc.driveMap();
        /*
         * Add working directory.
         */
        String workPath = System.getProperty("user.dir");
        File work = new File(workPath);
        if (work.isDirectory()) {
            String absoluteDirPath = work.getAbsolutePath();
            absoluteDirPath = absoluteDirPath.replaceAll("\\\\", "/");
            File oldFile = fileMap.put(absoluteDirPath, work);
            assert oldFile == null : oldFile;
        }
        /*
         * Add home directory.
         */
        String homePath = System.getProperty("user.home");
        File home = new File(homePath);
        if (home.isDirectory()) {
            String absoluteDirPath = home.getAbsolutePath();
            absoluteDirPath = absoluteDirPath.replaceAll("\\\\", "/");
            File oldFile = fileMap.put(absoluteDirPath, home);
            assert oldFile == null : oldFile;
        }

        buildFolderMenu(fileMap);
        builder.show(ActionPrefix.newAssetLocation);
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
                if (!parentPath.endsWith("/")) {
                    parentPath += "/";
                }
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
     * @param spec URL specification (not null, not empty)
     * @param slot load slot (not null)
     */
    public void loadCgmLocator(String spec, LoadedCgm slot) {
        if (spec.equals(EditorMenus.defaultLocation)) {
            buildClasspathCgmMenu();
            if (slot == Maud.getModel().getSource()) {
                builder.show(ActionPrefix.loadSourceCgmNamed);
            } else if (slot == Maud.getModel().getTarget()) {
                builder.show(ActionPrefix.loadCgmNamed);
            } else {
                throw new IllegalArgumentException();
            }

        } else if (spec.startsWith("file://") || spec.endsWith(".jar")
                || spec.endsWith(".zip")) {
            String indexString
                    = Maud.getModel().getLocations().indexForSpec(spec);
            String args = indexString + " /";
            loadCgmAsset(args, slot);

        } else {
            EditorDialogs.loadCgmAsset(spec, slot);
        }
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
     * @param spec URL specification or
     * defaultLocation/identityForSource/identityForTarget(not null, not empty)
     */
    public void loadMapLocator(String spec) {
        EditorModel model = Maud.getModel();
        switch (spec) {
            case EditorMenus.defaultLocation:
                buildClasspathMapMenu();
                builder.show(ActionPrefix.loadMapNamed);
                break;

            case EditorMenus.identityForSource:
                Cgm source = model.getSource();
                model.getMap().loadIdentityFor(source);
                break;

            case EditorMenus.identityForTarget:
                Cgm target = model.getTarget();
                model.getMap().loadIdentityFor(target);
                break;

            default:
                if (spec.startsWith("file://") || spec.endsWith(".jar")
                        || spec.endsWith(".zip")) {
                    String indexString;
                    indexString = model.getLocations().indexForSpec(spec);
                    String args = indexString + " /";
                    loadMapAsset(args);
                } else {
                    EditorDialogs.loadMapAsset(spec);
                }
        }
    }

    /**
     * Handle a "load texture asset" action with arguments.
     *
     * @param args action arguments (not null, not empty)
     */
    public void loadTextureAsset(String args) {
        String indexString = args.split(" ")[0];
        String assetPath = MyString.remainder(args, indexString + " ");
        EditorModel model = Maud.getModel();
        String spec = model.getLocations().specForIndex(indexString);
        SelectedTexture texture = model.getTarget().getTexture();

        if (spec == null || !spec.startsWith("file:///")) { // won't browse
            texture.setAssetPath(assetPath);
            return;
        }
        String rootPath = MyString.remainder(spec, "file:///");

        if (rootPath.endsWith(".jar") || rootPath.endsWith(".zip")) {
            List<String> entryNames = Misc.listZipEntries(rootPath, assetPath);
            int numEntries = entryNames.size();
            List<String> cgmEntries = new ArrayList<>(numEntries);
            for (String entryName : entryNames) {
                if (MenuBuilder.hasTextureSuffix(entryName)) {
                    cgmEntries.add(entryName);
                }
            }
            if (cgmEntries.size() == 1 && cgmEntries.contains(assetPath)) {
                texture.setAssetPath(assetPath);
            } else if (!cgmEntries.isEmpty()) {
                ShowMenus.selectFile(cgmEntries,
                        ActionPrefix.loadTextureAsset + indexString + " ");
            }

        } else { // not a JAR or ZIP
            File file = new File(rootPath, assetPath);
            if (file.isDirectory()) {
                String folderPath = file.getAbsolutePath();
                buildFolderMenu(folderPath, "");
                String menuPrefix = ActionPrefix.loadTextureAsset + args;
                if (!menuPrefix.endsWith("/")) {
                    menuPrefix += "/";
                }
                builder.show(menuPrefix);

            } else if (file.canRead()) {
                texture.setAssetPath(assetPath);

            } else {
                /*
                 * Treat the pathname as a prefix.
                 */
                File parent = file.getParentFile();
                String parentPath = parent.getAbsolutePath();
                parentPath = parentPath.replaceAll("\\\\", "/");
                if (!parentPath.endsWith("/")) {
                    parentPath += "/";
                }
                String prefix = file.getName();
                buildFolderMenu(parentPath, prefix);
                parentPath = MyString.remainder(parentPath, rootPath);
                String menuPrefix = ActionPrefix.loadTextureAsset
                        + indexString + " " + parentPath;
                if (!menuPrefix.endsWith("/")) {
                    menuPrefix += "/";
                }
                builder.show(menuPrefix);
            }
        }
    }

    /**
     * Handle a "load texture locator" action.
     *
     * @param spec URL specification or defaultLocation (not null, not empty)
     */
    public void loadTextureLocator(String spec) {
        if (spec.equals(EditorMenus.defaultLocation)) {
            buildClasspathTextureMenu();
            builder.show(ActionPrefix.loadTextureAsset + "-1 /");

        } else if (spec.startsWith("file://") || spec.endsWith(".jar")
                || spec.endsWith(".zip")) {
            String indexString
                    = Maud.getModel().getLocations().indexForSpec(spec);
            String args = indexString + " /";
            loadTextureAsset(args);
        }
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
                AnimationMenus.buildAnimationMenu(builder);
                break;

            case "Bone":
                BoneMenus.buildBoneMenu(builder);
                break;

            case "CGM":
                CgmMenus.buildCgmMenu(builder);
                break;

            case "Help":
                buildHelpMenu();
                break;

            case "History":
                buildHistoryMenu();
                break;

            case "Keyframe":
                KeyframeMenus.buildKeyframeMenu(builder);
                break;

            case "Map":
                buildMapMenu();
                break;

            case "Mesh":
                MeshMenus.buildMeshMenu(builder);
                break;

            case "Physics":
                PhysicsMenus.buildPhysicsMenu(builder);
                break;

            case "Settings":
                buildSettingsMenu();
                break;

            case "SGC":
                SgcMenus.buildSgcMenu(builder);
                break;

            case "Spatial":
                SpatialMenus.buildSpatialMenu(builder);
                break;

            case "Track":
                AnimationMenus.buildTrackMenu(builder);
                break;

            case "View":
                ViewMenus.buildViewMenu(builder);
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
    // *************************************************************************
    // private methods

    /**
     * Build a menu of models on the classpath.
     */
    private void buildClasspathCgmMenu() {
        builder.reset();
        /*
         * Add items for C-G models included (on the classpath) with Maud.
         * If haveTestdata, also add items for C-G models in jme3-testdata.
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
     * Build a menu of textures on the classpath.
     */
    private void buildClasspathTextureMenu() {
        builder.reset();
        /*
         * Add items for textures included (on the classpath) with Maud.
         * If haveTestdata, also add items for textures in jme3-testdata.
         */
        builder.addFile("Models/Jaime/NormalMap.png");
        builder.addFile("Models/Jaime/diffuseMap.jpg");
        builder.addFile("Models/MhGame/textures/brown_eye.png");
        builder.addFile("Models/MhGame/textures/eyebrow001.png");
        builder.addFile("Models/MhGame/textures/lit_matte.png");
        builder.addFile("Models/MhGame/textures/lit_standard_skin.png");
        builder.addFile("Models/MhGame/textures/male_worksuit01_ao.png");
        builder.addFile("Models/MhGame/textures/male_worksuit01_diffuse.png");
        builder.addFile("Models/MhGame/textures/male_worksuit01_normal.png");
        builder.addFile("Models/MhGame/textures/shoes03_diffuse.png");
        builder.addFile("Models/MhGame/textures/skinmat_eye.png");
        builder.addFile(
                "Models/MhGame/textures/young_lightskinned_male_diffuse2.png");
        builder.addFile("Models/Sinbad/sinbad_body.jpg");
        builder.addFile("Models/Sinbad/sinbad_clothes.jpg");
        builder.addFile("Models/Sinbad/sinbad_sword.jpg");
        builder.addFile("Textures/platform/bronze_ape.jpg");
        builder.addFile("Textures/platform/rock_11474.jpg");
        builder.addFile("Textures/platform/wood_0534.jpg");
        // TODO more classpath textures
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
        /*
         * Build a reduced menu.
         */
        MyString.reduce(nameList, ShowMenus.maxItems);
        Collections.sort(nameList);
        builder.reset();
        for (String name : nameList) {
            if (fileMap.containsKey(name)) {
                File f = fileMap.get(name);
                if (f.isDirectory()) {
                    builder.addFolder(name);
                } else {
                    builder.addFile(name);
                }
            } else { // prefix
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
        builder.addSubmenu("Add asset location");
        if (Maud.getModel().getLocations().hasRemovable()) {
            builder.addSubmenu("Remove asset location");
        }
        builder.addTool("Display-settings tool");
        builder.add("Hotkeys");
        //builder.add("Locale"); TODO
        builder.addSubmenu("Scene-view options");
        builder.addSubmenu("Score-view options");
        builder.addTool("Tweening tool");
        builder.add("Update startup script");
        if (Maud.isStartupScriptCustomized()) {
            builder.add("Revert startup script to default");
        }
    }
}
