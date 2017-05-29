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
package maud.model;

import com.jme3.animation.AnimControl;
import com.jme3.animation.Animation;
import com.jme3.animation.Bone;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.animation.Track;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetLoadException;
import com.jme3.asset.AssetManager;
import com.jme3.asset.ModelKey;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.jme3.scene.plugins.ogre.MeshLoader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.Validate;
import maud.CgmView;
import maud.Maud;
import maud.Util;

/**
 * MVC model for a loaded computer-graphics (CG) model in the Maud application:
 * encapsulates the CG model's tree of spatials, keeps track of where the CG
 * model was loaded from, and provides access to related MVC model state: the
 * loaded animation and the selected spatial/control/skeleton/pose/bone/etc.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class LoadedCGModel implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            LoadedCGModel.class.getName());
    /**
     * dummy control name used to indicate that no control is selected
     */
    final public static String noControl = "( no control )";
    // *************************************************************************
    // fields

    /**
     * visualization of the CG model (set by {@link #setView(maud.CgmView)})
     */
    public CgmView view = null;
    /**
     * the loaded animation for the CG model
     */
    public LoadedAnimation animation = new LoadedAnimation();
    /**
     * bone transforms of the displayed pose
     */
    public Pose pose = new Pose();
    /**
     * which bone in selected in the CG model
     */
    public SelectedBone bone = new SelectedBone();
    /**
     * which SG control is selected in the CG model
     */
    public SelectedSgc sgc = new SelectedSgc();
    /**
     * which skeleton is selected in the CG model
     */
    public SelectedSkeleton bones = new SelectedSkeleton();
    /**
     * which spatial is selected in the CG model
     */
    public SelectedSpatial spatial = new SelectedSpatial();
    /**
     * which track is selected in the CG model
     */
    public SelectedTrack track = new SelectedTrack();
    /**
     * the root spatial in the MVC model's copy of the CG model
     */
    protected Spatial rootSpatial = null;
    /**
     * asset path of the CG model, less extension
     */
    protected String loadedModelAssetPath = null;
    /**
     * extension of the CG model
     */
    protected String loadedModelExtension = null;
    /**
     * filesystem path of the loaded model, less extension
     */
    protected String loadedModelFilePath = null;
    /**
     * name of the loaded model
     */
    protected String modelName = null;
    /**
     * world transform of the visualization
     */
    public TransformStatus transform = new TransformStatus();
    // *************************************************************************
    // constructors

    /**
     * Instantiate with no model loaded.
     */
    public LoadedCGModel() {
        animation.setCgm(this);
        bone.setCgm(this);
        bones.setCgm(this);
        pose.setCgm(this);
        sgc.setCgm(this);
        spatial.setCgm(this);
        track.setCgm(this);
        transform.setCgm(this);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Copy the local transform of the selected spatial.
     *
     * @param storeResult (modified if not null)
     * @return transform (either storeResult or a new instance)
     */
    public Transform copySpatialTransform(Transform storeResult) {
        if (storeResult == null) {
            storeResult = new Transform();
        }

        Spatial selectedSpatial = spatial.findSpatial(rootSpatial);
        Transform transform = selectedSpatial.getLocalTransform();
        storeResult.set(transform);

        return storeResult;
    }

    /**
     * Count the animations.
     *
     * @return count (&ge;0)
     */
    public int countAnimations() {
        AnimControl animControl = getAnimControl();
        int count;
        if (animControl == null) {
            count = 0;
        } else {
            Collection<String> names = animControl.getAnimationNames();
            count = names.size();
        }

        assert count >= 0 : count;
        return count;
    }

    /**
     * Find a spatial with the specified name.
     *
     * @param name what name to search for (not null, not empty)
     * @return a new tree-position instance, or null if not found
     */
    List<Integer> findSpatialNamed(String name) {
        List<Integer> treePosition = new ArrayList<>(4);
        Spatial spatial = findSpatialNamed(name, rootSpatial, treePosition);
        if (spatial == null) {
            treePosition = null;
        }

        return treePosition;
    }

    /**
     * Access the named animation.
     *
     * @param name (not null)
     * @return the pre-existing instance, or null if not found
     */
    Animation getAnimation(String name) {
        Validate.nonNull(name, "animation name");

        Animation animation;
        AnimControl animControl = getAnimControl();
        if (animControl == null) {
            animation = null;
        } else {
            animation = animControl.getAnim(name);
        }

        return animation;
    }

    /**
     * Access the selected AnimControl.
     *
     * @return the pre-existing instance, or null if none
     */
    AnimControl getAnimControl() {
        AnimControl animControl;
        Control selectedSgc = sgc.findSgc();
        if (selectedSgc instanceof AnimControl) {
            animControl = (AnimControl) selectedSgc;
        } else {
            animControl = rootSpatial.getControl(AnimControl.class);
        }

        return animControl;
    }

    /**
     * Read the asset path of the loaded model, less extension.
     *
     * @return path, or "" if not known (not null)
     */
    public String getAssetPath() {
        assert loadedModelAssetPath != null;
        return loadedModelAssetPath;
    }

    /**
     * Read the duration of the named animation.
     *
     * @param animationName (not null)
     * @return duration (in seconds, &ge;0)
     */
    public float getDuration(String animationName) {
        Validate.nonNull(animationName, "animation name");

        float result;
        if (animationName.equals(LoadedAnimation.bindPoseName)) {
            result = 0f;
        } else {
            Animation animation = getAnimation(animationName);
            if (animation == null) {
                logger.log(Level.WARNING, "no animation named {0}",
                        MyString.quote(animationName));
                result = 0f;
            } else {
                result = animation.getLength();
            }
        }

        assert result >= 0f : result;
        return result;
    }

    /**
     * Read the extension of the loaded model.
     *
     * @return extension (not null)
     */
    public String getExtension() {
        assert loadedModelExtension != null;
        return loadedModelExtension;
    }

    /**
     * Read the filesystem path of the loaded model, less extension.
     *
     * @return path, or "" if not known (not null)
     */
    public String getFilePath() {
        assert loadedModelFilePath != null;
        return loadedModelFilePath;
    }

    /**
     * Read the name of the loaded model.
     *
     * @return name, or "" if not known (not null)
     */
    public String getName() {
        assert modelName != null;
        return modelName;
    }

    /**
     * Access the root spatial of the loaded model.
     *
     * @return the pre-existing instance (not null)
     */
    Spatial getRootSpatial() {
        assert rootSpatial != null;
        return rootSpatial;
    }

    /**
     * Test whether the animation controller contains the named animation.
     *
     * @param name (not null)
     * @return true if found or bindPose, otherwise false
     */
    public boolean hasAnimation(String name) {
        Validate.nonNull(name, "name");

        Animation animation = getAnimation(name);
        if (animation == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether the CG model contains the named geometry.
     *
     * @param name (not null)
     * @return true if found, otherwise false
     */
    public boolean hasGeometry(String name) {
        Spatial spatial = findSpatialNamed(name, rootSpatial, null);
        boolean result = spatial instanceof Geometry;

        return result;
    }

    /**
     * Test whether the CG model contains the named node.
     *
     * @param name (not null)
     * @return true if found, otherwise false
     */
    public boolean hasNode(String name) {
        Spatial spatial = findSpatialNamed(name, rootSpatial, null);
        boolean result = spatial instanceof Node;

        return result;
    }

    /**
     * Test whether the CG model contains the named spatial.
     *
     * @param name (not null)
     * @return true if found, otherwise false
     */
    public boolean hasSpatial(String name) {
        Spatial spatial = findSpatialNamed(name, rootSpatial, null);
        if (spatial == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether a CG model is loaded.
     *
     * @return true if loaded, otherwise false
     */
    public boolean isLoaded() {
        if (rootSpatial == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether the model root is a node.
     *
     * @return true for a node, false for a geometry
     */
    public boolean isRootANode() {
        boolean result = rootSpatial instanceof Node;
        return result;
    }

    /**
     * Enumerate all known animations and poses for the loaded model.
     *
     * @return a new collection of names
     */
    public Collection<String> listAnimationNames() {
        Collection<String> names = listAnimationsSorted();
        names.add(LoadedAnimation.bindPoseName);

        return names;
    }

    /**
     * Generate a sorted list of animation names, not including bindPose.
     *
     * @return a new list
     */
    List<String> listAnimationsSorted() {
        List<String> result;
        AnimControl animControl = getAnimControl();
        if (animControl == null) {
            result = new ArrayList<>(0);
        } else {
            Collection<String> names = animControl.getAnimationNames();
            int numNames = names.size();
            result = new ArrayList<>(numNames);
            result.addAll(names);
            Collections.sort(result);
        }

        return result;
    }

    /**
     * Enumerate all meshes in the loaded model.
     *
     * @return a new list
     */
    List<Mesh> listMeshes() {
        List<Mesh> result = new ArrayList<>(8);

        Node node = (Node) rootSpatial;
        for (Spatial child : node.getChildren()) {
            if (child instanceof Geometry) {
                Geometry geometry = (Geometry) child;
                Mesh mesh = geometry.getMesh();
                result.add(mesh);
            }
        }

        return result;
    }

    /**
     * Enumerate named spatials in the loaded model whose names begin with the
     * specified prefix.
     *
     * @param prefix which name prefix (not null, may be empty)
     * @param includeNodes true &rarr; both nodes and geometries, false &rarr;
     * geometries only
     * @return a new list of names
     */
    public List<String> listSpatialNames(String prefix, boolean includeNodes) {
        List<String> result;
        result = listSpatialNames(rootSpatial, prefix, includeNodes);

        return result;
    }

    /**
     * Unload the current model, if any, and load the specified asset.
     *
     * @param assetPath path to the model asset to load (not null)
     * @return true if successful, otherwise false
     */
    public boolean loadModelAsset(String assetPath) {
        Validate.nonNull(assetPath, "asset path");

        Spatial loaded = loadModelFromAsset(assetPath, false);
        if (loaded == null) {
            return false;
        } else {
            postLoad(loaded);
            return true;
        }
    }

    /**
     * Unload the current model, if any, and load the specified file.
     *
     * @param filePath path to the model file to load (not null)
     * @return true if successful, otherwise false
     */
    public boolean loadModelFile(File filePath) {
        String canonicalPath;
        try {
            canonicalPath = filePath.getCanonicalPath();
        } catch (IOException e) {
            return false;
        }

        Spatial loaded = loadModelFromFile(canonicalPath);
        if (loaded == null) {
            return false;
        } else {
            postLoad(loaded);
            return true;
        }
    }

    /**
     * Unload the current model, if any, and load the named one from the
     * jme3-testdata asset pack.
     *
     * @param name which model to load (not null)
     * @return true if successful, otherwise false
     */
    public boolean loadModelNamed(String name) {
        String fileName;
        switch (name) {
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
            case "MonkeyHead":
                fileName = "MonkeyHead.mesh.xml";
                break;
            case "Ninja":
                fileName = "Ninja.mesh.xml";
                break;
            case "Oto":
                fileName = "Oto.mesh.xml";
                break;
            case "Sign Post":
                fileName = "Sign Post.mesh.xml";
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
                String message = String.format("unknown asset name: %s",
                        MyString.quote(name));
                throw new IllegalArgumentException(message);
        }

        String assetPath = String.format("Models/%s/%s", name, fileName);
        Spatial loaded = loadModelFromAsset(assetPath, false);
        if (loaded == null) {
            return false;
        } else {
            this.modelName = name;
            postLoad(loaded);
            return true;
        }
    }

    /**
     * Alter the MVC view for the CG model.
     *
     * @param newView (not null)
     */
    public void setView(CgmView newView) {
        Validate.nonNull(newView, "new view");
        view = newView;
    }
    // *************************************************************************
    // protected methods

    /**
     * Invoked after successfully loading a CG model.
     *
     * @param modelRoot (not null)
     */
    protected void postLoad(Spatial modelRoot) {
        assert modelRoot != null;

        validateModel(modelRoot);
        rootSpatial = modelRoot.clone();
        view.loadModel(modelRoot);
        /*
         * Reset the selected bone/spatial and also the loaded animation.
         */
        bone.selectNoBone();
        spatial.selectModelRoot();
        animation.loadBindPose();
    }
    // *************************************************************************
    // Object methods

    /**
     * Create a copy of this object.
     *
     * @return a new object, equivalent to this one
     * @throws CloneNotSupportedException if a field isn't cloneable
     */
    @Override
    public LoadedCGModel clone() throws CloneNotSupportedException {
        LoadedCGModel clone = (LoadedCGModel) super.clone();

        if (rootSpatial == null) {
            clone.rootSpatial = null;
        } else {
            clone.rootSpatial = rootSpatial.clone();
        }

        if (view == null) {
            clone.view = null;
        } else {
            clone.view = view.createCopy(clone);
        }

        clone.animation = animation.clone();
        clone.bone = bone.clone();
        clone.bones = bones.clone();
        clone.pose = pose.clone();
        clone.sgc = sgc.clone();
        clone.spatial = spatial.clone();
        clone.track = track.clone();
        clone.transform = transform.clone();

        clone.animation.setCgm(clone);
        clone.bone.setCgm(clone);
        clone.bones.setCgm(clone);
        clone.pose.setCgm(clone);
        clone.sgc.setCgm(clone);
        clone.spatial.setCgm(clone);
        clone.track.setCgm(clone);
        clone.transform.setCgm(clone);

        return clone;
    }
    // *************************************************************************
    // private methods

    /**
     * Find a spatial with the specified name in the specified subtree. Note:
     * recursive!
     *
     * @param name what name to search for (not null, not empty)
     * @param subtree which subtree to search (may be null, unaffected)
     * @param storePosition tree position of the spatial (modified if found and
     * not null)
     * @return the pre-existing spatial, or null if not found
     */
    private Spatial findSpatialNamed(String name, Spatial subtree,
            List<Integer> storePosition) {
        Spatial result = null;
        if (subtree != null) {
            String spatialName = subtree.getName();
            if (spatialName != null && spatialName.equals(name)) {
                result = subtree;
                if (storePosition != null) {
                    storePosition.clear();
                }

            } else if (subtree instanceof Node) {
                Node node = (Node) subtree;
                List<Spatial> children = node.getChildren();
                int numChildren = children.size();
                for (int childI = 0; childI < numChildren; childI++) {
                    Spatial child = children.get(childI);
                    result = findSpatialNamed(name, child, storePosition);
                    if (result != null) {
                        if (storePosition != null) {
                            storePosition.add(0, childI);
                        }
                        break;
                    }
                }
            }
        }

        return result;
    }

    /**
     * Enumerate named spatials in the specified subtree whose names begin with
     * the specified prefix. Note: recursive!
     *
     * @param subtree which subtree to traverse (may be null, unaffected)
     * @param prefix which name prefix (not null, may be empty)
     * @param includeNodes true &rarr; both nodes and geometries, false &rarr;
     * geometries only
     * @return a new list of names
     */
    private List<String> listSpatialNames(Spatial subtree, String prefix,
            boolean includeNodes) {
        List<String> names = new ArrayList<>(5);
        if (subtree != null) {
            String name = subtree.getName();
            if (name != null && !name.isEmpty() && name.startsWith(prefix)) {
                if (includeNodes || subtree instanceof Geometry) {
                    names.add(name);
                }
            }

            if (subtree instanceof Node) {
                Node node = (Node) subtree;
                List<Spatial> children = node.getChildren();
                for (Spatial child : children) {
                    List<String> childNames;
                    childNames = listSpatialNames(child, prefix, includeNodes);
                    names.addAll(childNames);
                }
            }
        }

        return names;
    }

    /**
     * Quietly load a CG model asset from persistent storage without adding it
     * to the scene. If successful, set {@link #loadedModelAssetPath}.
     *
     * @param assetPath (not null)
     * @param useCache true to look in the asset manager's cache, false to force
     * a fresh load from persistent storage
     * @return an orphaned spatial, or null if the asset was not found
     */
    private Spatial loadModelFromAsset(String assetPath, boolean useCache) {
        SimpleApplication application = Maud.getApplication();
        AssetManager assetManager = application.getAssetManager();

        ModelKey key = new ModelKey(assetPath);
        if (!useCache) {
            /*
             * Delete the model's key from the asset manager's cache, to force a
             * fresh load from persistent storage.
             */
            assetManager.deleteFromCache(key);
        }
        /*
         * Load the model quietly.
         */
        Spatial loaded = Util.loadCgmQuietly(assetManager, assetPath);
        if (loaded == null) {
            logger.log(Level.SEVERE, "Failed to load model from asset {0}",
                    MyString.quote(assetPath));
        } else {
            logger.log(Level.INFO, "Loaded model from asset {0}",
                    MyString.quote(assetPath));

            loadedModelExtension = key.getExtension();
            int extLength = loadedModelExtension.length();
            if (extLength == 0) {
                loadedModelAssetPath = assetPath;
            } else {
                int pathLength = assetPath.length() - extLength - 1;
                loadedModelAssetPath = assetPath.substring(0, pathLength);
            }
            loadedModelFilePath = "";
            modelName = loaded.getName();
        }

        return loaded;
    }

    /**
     * Quietly load a model file without adding it to the scene. If successful,
     * set {@link #loadedModelFilePath}.
     *
     * @param filePath (not null)
     * @return an orphaned spatial, or null if an error occurred
     */
    private Spatial loadModelFromFile(String filePath) {
        ModelKey key = new ModelKey(filePath);
        InputStream inputStream;
        try {
            inputStream = new FileInputStream(filePath);
        } catch (FileNotFoundException e) {
            return null;
        }
        /*
         * Temporarily hush loader warnings about vertices with >4 weights.
         */
        Logger mlLogger = Logger.getLogger(MeshLoader.class.getName());
        Level oldLevel = mlLogger.getLevel();
        mlLogger.setLevel(Level.SEVERE);

        Spatial loaded;
        SimpleApplication application = Maud.getApplication();
        AssetManager assetManager = application.getAssetManager();
        try {
            loaded = assetManager.loadAssetFromStream(key, inputStream);
        } catch (AssetLoadException e) {
            loaded = null;
        }
        /*
         * Restore logging levels.
         */
        mlLogger.setLevel(oldLevel);

        if (loaded == null) {
            logger.log(Level.SEVERE, "Failed to load model from file {0}",
                    MyString.quote(filePath));
        } else {
            logger.log(Level.INFO, "Loaded model from file {0}",
                    MyString.quote(filePath));

            loadedModelAssetPath = "";
            loadedModelExtension = key.getExtension();
            int extLength = loadedModelExtension.length();
            if (extLength == 0) {
                loadedModelFilePath = filePath;
            } else {
                int pathLength = filePath.length() - extLength - 1;
                loadedModelFilePath = filePath.substring(0, pathLength);
            }
            modelName = loaded.getName();
        }

        return loaded;
    }

    /**
     * Test for issues with a BoneTrack.
     *
     * @param boneTrack (not null)
     * @param numBones (&gt;0, &le;255)
     * @param numFrames (&gt;0)
     * @return false if issues found, otherwise true
     */
    private boolean validateBoneTrack(BoneTrack boneTrack, int numBones,
            int numFrames) {
        assert numBones > 0 : numBones;
        assert numBones <= 255 : numBones;
        assert numFrames > 0 : numFrames;

        int targetBoneIndex = boneTrack.getTargetBoneIndex();
        if (targetBoneIndex >= numBones) {
            logger.warning("track for non-existant bone");
            return false;
        }
        Vector3f[] translations = boneTrack.getTranslations();
        if (translations == null) {
            logger.warning("bone track lacks translation data");
            return false;
        }
        int numTranslations = translations.length;
        if (numTranslations != numFrames) {
            logger.warning("translation data have wrong length");
            return false;
        }
        Quaternion[] rotations = boneTrack.getRotations();
        if (rotations == null) {
            logger.warning("bone track lacks rotation data");
            return false;
        }
        int numRotations = rotations.length;
        if (numRotations != numFrames) {
            logger.warning("rotation data have wrong length");
            return false;
        }
        Vector3f[] scales = boneTrack.getScales();
        if (scales == null) { // JME3 allows this
            logger.warning("bone track lacks scale data");
            return false;
        }
        int numScales = scales.length;
        if (numScales != numFrames) {
            logger.warning("scale data have wrong length");
            return false;
        }

        return true;
    }

    /**
     * Test for issues with a CG model.
     *
     * @param modelRoot (not null)
     * @return false if issues found, otherwise true
     */
    private boolean validateModel(Spatial modelRoot) {
        assert modelRoot != null;

        SkeletonControl skeletonControl = modelRoot.getControl(
                SkeletonControl.class);
        if (skeletonControl == null) {
            logger.warning("lacks a skeleton control");
            return false;
        }
        Skeleton skeleton = skeletonControl.getSkeleton();
        if (skeleton == null) {
            logger.warning("lacks a skeleton");
            return false;
        }
        int numBones = skeleton.getBoneCount();  // TODO each skeleton
        if (numBones > 255) {
            logger.warning("too many bones");
            return false;
        }
        if (numBones < 0) {
            logger.warning("bone count is negative");
            return false;
        }
        Set<String> nameSet = new TreeSet<>();
        for (int boneIndex = 0; boneIndex < numBones; boneIndex++) {
            Bone bone = skeleton.getBone(boneIndex);
            if (!bones.validateBone(bone, nameSet)) {
                return false;
            }
        }
        AnimControl animControl = modelRoot.getControl(AnimControl.class);
        if (animControl == null) {
            logger.warning("model lacks an animation control");
            return false;
        }
        Collection<String> animNames = animControl.getAnimationNames();
        if (animNames.isEmpty()) {
            logger.warning("model has no animations");
            return false;
        }
        nameSet.clear();
        for (String name : animNames) {
            if (name == null) {
                logger.warning("animation name is null");
                return false;
            }
            if (name.length() == 0) {
                logger.warning("animation name is empty");
                return false;
            }
            if (name.equals(LoadedAnimation.bindPoseName)) {
                logger.warning("animation has reserved name");
                return false;
            }
            if (nameSet.contains(name)) {
                logger.warning("duplicate animation name");
                return false;
            }
            nameSet.add(name);
            Animation anim = animControl.getAnim(name);
            if (anim == null) {
                logger.warning("animation is null");
                return false;
            }
            float duration = anim.getLength();
            if (duration < 0f) {
                logger.warning("animation has negative length");
                return false;
            }
            Track[] tracks = anim.getTracks();
            if (tracks == null) {
                logger.warning("animation has no track data");
                return false;
            }
            int numTracks = tracks.length;
            if (numTracks == 0) {
                logger.warning("animation has no tracks");
                return false;
            }
            for (Track track : tracks) {
                float[] times = track.getKeyFrameTimes();
                if (times == null) {
                    logger.warning("track has no keyframe data");
                    return false;
                }
                int numFrames = times.length;
                if (numFrames <= 0) {
                    logger.warning("track has no keyframes");
                    return false;
                }
                if (times[0] != 0f) {
                    logger.warning("first keyframe not at t=0");
                    return false;
                }
                float prev = -1f;
                for (int frameIndex = 0; frameIndex < numFrames; frameIndex++) {
                    float time = times[frameIndex];
                    if (time < prev) {
                        logger.warning("keyframes out of order");
                        return false;
                    } else if (time == prev) {
                        logger.log(Level.WARNING,
                                "multiple keyframes for t={0} in {1}",
                                new Object[]{time, MyString.quote(name)});
                    } else if (time > duration) {
                        logger.warning("keyframe past end of animation");
                        return false;
                    }
                    prev = time;
                }
                if (track instanceof BoneTrack) {
                    BoneTrack boneTrack = (BoneTrack) track;
                    if (!validateBoneTrack(boneTrack, numBones, numFrames)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }
}
