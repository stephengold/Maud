/*
 Copyright (c) 2017-2022, Stephen Gold
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
package maud.model.cgm;

import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.asset.ModelKey;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.bvh.BVHAnimData;
import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Heart;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.ui.ActionApplication;
import jme3utilities.ui.Locators;
import maud.CheckLoaded;
import maud.LoadUtil;
import maud.Maud;
import maud.dialog.EditorDialogs;
import maud.menu.BuildMenus;
import maud.model.History;
import maud.model.option.ShowBones;

/**
 * MVC model for a computer-graphics (C-G) model load slot in the Maud
 * application: keeps track of where it was loaded from.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class LoadedCgm extends Cgm {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(LoadedCgm.class.getName());
    // *************************************************************************
    // fields

    /**
     * absolute filesystem path to the asset root used to load the C-G model, or
     * "" if unknown/remote, or null if none loaded
     */
    protected String assetRootPath = null;
    /**
     * asset path less extension, or "" if unknown, or null if none loaded
     */
    protected String baseAssetPath = null;
    /**
     * extension of the asset path, or "" if unknown, or null if none loaded
     */
    protected String extension = null;
    /**
     * name of the C-G model, or null if none loaded
     */
    protected String name = null;
    // *************************************************************************
    // new methods exposed

    /**
     * Determine the default base path for writing the C-G model to the
     * filesystem.
     *
     * @return absolute filesystem path less extension (not null, not empty)
     */
    public String baseFilePathForWrite() {
        String folder = assetFolderForWrite();
        String assetPath = getAssetPath();
        if (assetPath.isEmpty()) {
            assetPath = "Models/Untitled/Untitled";
        }
        File file = new File(folder, assetPath);
        String result = Heart.fixedPath(file);

        return result;
    }

    /**
     * Read the asset path of the loaded C-G model, less extension.
     *
     * @return base path, or "" if unknown (not null)
     */
    public String getAssetPath() {
        assert baseAssetPath != null;
        return baseAssetPath;
    }

    /**
     * Read the local filesystem path to the asset root used to load the C-G
     * model.
     *
     * @return absolute path, or "" if unknown/remote (not null)
     */
    public String getAssetRootPath() {
        assert assetRootPath != null;
        return assetRootPath;
    }

    /**
     * Read the extension of the loaded C-G model.
     *
     * @return extension (not null)
     */
    public String getExtension() {
        assert extension != null;
        return extension;
    }

    /**
     * Read the name of the loaded C-G model.
     *
     * @return name, or "" if not known (not null)
     */
    public String getName() {
        assert name != null;
        return name;
    }

    /**
     * Unload the loaded C-G model, if any, and load from the specified asset in
     * the specified location.
     *
     * @param spec URL specification, or null for the default location
     * @param assetPath path to the asset to load (not null, not empty)
     * @return true if successful, otherwise false
     */
    public boolean loadAsset(String spec, String assetPath) {
        Validate.nonEmpty(assetPath, "asset path");

        boolean useCache = false;
        boolean diagnose = Maud.getModel().getMisc().diagnoseLoads();

        Locators.save();
        Locators.unregisterAll();
        Locators.register(spec);
        Spatial loaded = loadFromAsset(assetPath, useCache, diagnose);
        Locators.restore();

        boolean success;
        if (loaded == null) {
            success = false;
        } else {
            if (spec == null || !spec.startsWith("file:///")) {
                assetRootPath = "";
            } else {
                String rootPath = MyString.remainder(spec, "file:///");
                assert !rootPath.isEmpty();
                assetRootPath = rootPath;
            }
            postLoad(loaded);
            success = true;
        }

        return success;
    }

    /**
     * Unload the current C-G model, if any, and load the named one from the
     * default location.
     *
     * @param cgmName which C-G model to load (not null, not empty)
     * @return true if successful, otherwise false
     */
    public boolean loadNamed(String cgmName) {
        Validate.nonEmpty(cgmName, "model name");

        String folderName = cgmName;
        String fileName;
        switch (cgmName) {
            case BuildMenus.otherName:
                EditorDialogs.loadCgmAsset(null, this);
                return true;

            case "Boat":
                fileName = "boat.j3o";
                break;
            case "Buggy":
                fileName = "Buggy.j3o";
                break;
            case "Elephant":
                fileName = "Elephant.mesh.xml";
                break;
            case "Ferrari":
                fileName = "Car.scene";
                break;
            case "HoverTank":
                fileName = "Tank2.mesh.xml";
                break;
            case "Jaime":
                fileName = "Jaime.j3o";
                break;
            case "MhGame":
                fileName = "MhGame.mesh.xml";
                break;
            case "MonkeyHead":
                fileName = "MonkeyHead.mesh.xml";
                break;
            case "Ninja":
                fileName = "Ninja.mesh.xml";
                break;
            case "Oto":
                fileName = "Oto.mesh.xml";
                break;
            case "Puppet":
                fileName = "Puppet.xbuf";
                break;
            case "Sign Post":
                fileName = "Sign Post.mesh.xml";
                break;
            case "Sword":
                fileName = "Sword.mesh.xml";
                folderName = "Sinbad";
                break;
            case "Sinbad":
                fileName = "Sinbad.mesh.xml";
                break;
            case "SpaceCraft":
                fileName = "Rocket.mesh.xml";
                break;
            case "Teapot":
                fileName = "Teapot.obj";
                break;
            case "Tree":
                fileName = "Tree.mesh.xml";
                break;

            default:
                String message = String.format("unknown model-asset name: %s",
                        MyString.quote(cgmName));
                throw new IllegalArgumentException(message);
        }

        String assetPath = String.format("Models/%s/%s", folderName, fileName);
        boolean useCache = false;
        boolean diagnose = Maud.getModel().getMisc().diagnoseLoads();

        Locators.save();
        Locators.useDefault();
        Spatial loaded = loadFromAsset(assetPath, useCache, diagnose);
        Locators.restore();

        if (loaded == null) {
            return false;
        } else {
            name = cgmName;
            postLoad(loaded);
            return true;
        }
    }
    // *************************************************************************
    // new protected methods

    /**
     * Invoked after successfully loading a C-G model.
     *
     * @param cgmRoot the newly loaded C-G model (not null)
     */
    protected void postLoad(Spatial cgmRoot) {
        assert cgmRoot != null;

        CheckLoaded.cgm(cgmRoot);
        rootSpatial = Heart.deepCopy(cgmRoot);
        getVertex().deselect();
        getSceneView().loadCgm(cgmRoot);
        updateSceneWireframe();
        /*
         * Reset the selected bone/light/sgc/spatial/texture
         * TODO other selections?
         */
        getBone().deselect();
        getLight().postLoad();
        getSgc().postLoad();
        getSpatial().postLoad();
        getTexture().deselectAll();
        /*
         * If there is only one real animation, load it;
         * otherwise load bind pose.
         */
        SelectedAnimControl sac = getAnimControl();
        if (sac.countRealAnimations() == 1) {
            List<String> names = sac.listRealAnimationsSorted();
            String animationName = names.get(0);
            getAnimation().load(animationName);
        } else {
            getAnimation().loadBindPose(true);
        }
        /*
         * Verify that the displayed pose has been initialized.
         */
        int boneCount = getSkeleton().countBones();
        int numTransforms = getPose().get().countBones();
        assert numTransforms == boneCount : numTransforms;
        /*
         * If there are no mesh vertices, show all bones;
         * otherwise show only the bones that actually influence vertices.
         */
        ShowBones showBonesInScene;
        if (MySpatial.countVertices(cgmRoot) == 0) {
            showBonesInScene = ShowBones.All;
        } else {
            showBonesInScene = ShowBones.Influencers;
        }
        Maud.getModel().getScene().getSkeleton().setShowBones(showBonesInScene);
    }
    // *************************************************************************
    // Cgm methods

    /**
     * Create a deep copy of this object.
     *
     * @return a new object, equivalent to this one
     * @throws CloneNotSupportedException if a field isn't cloneable
     */
    @Override
    public LoadedCgm clone() throws CloneNotSupportedException {
        LoadedCgm clone = (LoadedCgm) super.clone();
        return clone;
    }

    /**
     * Unload the C-G model.
     */
    @Override
    public void unload() {
        Cgm target = Maud.getModel().getTarget();
        assert this != target; // not allowed to unload target

        assetRootPath = null;
        baseAssetPath = null;
        extension = null;
        name = null;

        super.unload();
    }
    // *************************************************************************
    // new protected methods

    /**
     * Determine the default asset folder for writing the C-G model to the
     * filesystem.
     *
     * @return absolute filesystem path (not null, not empty)
     */
    protected String assetFolderForWrite() {
        String result = assetRootPath;
        if (result.isEmpty() || result.endsWith(".jar")
                || result.endsWith(".zip")) {
            result = ActionApplication.sandboxPath();
        }

        return result;
    }
    // *************************************************************************
    // private methods

    /**
     * Quietly load a C-G model asset from persistent storage without adding it
     * to the scene. If successful, set {@link #baseAssetPath}.
     *
     * @param assetPath (not null)
     * @param useCache true to look in the asset manager's cache, false to force
     * a fresh load from persistent storage
     * @param diagnose true&rarr;messages to console, false&rarr;no messages
     * @return an orphaned spatial, or null if the asset had errors
     */
    private Spatial loadFromAsset(
            String assetPath, boolean useCache, boolean diagnose) {
        AssetManager assetManager = Locators.getAssetManager();
        Locators.save();
        /*
         * Load the C-G model.
         */
        String ext;
        Spatial loaded;
        if (assetPath.endsWith(".bvh")) {
            AssetKey<BVHAnimData> key = new AssetKey<>(assetPath);
            ext = key.getExtension();
            if (!useCache) {
                /*
                 * Delete the key from the asset manager's cache in order
                 * to force a fresh load from persistent storage.
                 */
                assetManager.deleteFromCache(key);
            }
            loaded = LoadUtil.loadBvhAsset(assetManager, key, diagnose);

        } else {
            ModelKey key = new ModelKey(assetPath);
            ext = key.getExtension();
            if (!useCache) {
                /*
                 * Clear the asset manager's cache in order to force a fresh
                 * load from persistent storage, not only for the model but
                 * also for any assets it references.
                 */
                assetManager.clearCache();
            }
            Locators.registerDefault();
            List<String> specList = Maud.getModel().getLocations().listAll();
            Locators.register(specList);

            loaded = LoadUtil.loadCgmAsset(assetManager, key, diagnose);
        }

        if (loaded == null) {
            logger.log(Level.SEVERE, "Failed to load model from asset {0}",
                    MyString.quote(assetPath));
        } else {
            logger.log(Level.INFO, "Loaded model from asset {0}",
                    MyString.quote(assetPath));

            if (this == Maud.getModel().getTarget() && isLoaded()) {
                History.autoAdd();
            }
            extension = ext;
            int extLength = extension.length();
            if (extLength == 0) {
                baseAssetPath = assetPath;
            } else {
                int pathLength = assetPath.length() - extLength - 1;
                baseAssetPath = assetPath.substring(0, pathLength);
            }
            assetRootPath = Locators.getRootPath();
            name = loaded.getName();
        }

        Locators.restore();
        return loaded;
    }
}
