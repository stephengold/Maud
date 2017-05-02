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
import com.jme3.animation.Track;
import com.jme3.asset.AssetLoadException;
import com.jme3.asset.AssetManager;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.asset.ModelKey;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.ogre.MeshLoader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyAnimation;
import jme3utilities.MySkeleton;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.ui.ActionApplication;
import maud.Maud;
import maud.Util;

/**
 * The MVC model of the loaded CG model in the Maud application: tracks all
 * edits made to the CG model.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class LoadedCGModel {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            LoadedCGModel.class.getName());
    /**
     * dummy bone name used to indicate that no bone is selected
     */
    final public static String noBone = "( no bone )";
    // *************************************************************************
    // fields

    /**
     * asset manager used to load CG models (set by
     * {@link #setAssetManager(com.jme3.asset.AssetManager)}
     */
    private AssetManager assetManager = null;
    /**
     * flag to track unsaved changes to the CG model
     */
    private boolean pristine = true;
    /**
     * which animation/pose is loaded
     */
    final public LoadedAnimation animation = new LoadedAnimation();
    /**
     * bone transforms of the displayed pose
     */
    final public Pose pose = new Pose();
    /**
     * which bone is selected
     */
    final public SelectedBone bone = new SelectedBone();
    /**
     * which bone is selected
     */
    final public SelectedSpatial spatial = new SelectedSpatial();
    /**
     * the root spatial in the MVC model's copy of the CG model
     */
    private Spatial rootSpatial = null;
    /**
     * asset path of the loaded model, less extension
     */
    private String loadedModelAssetPath = null;
    /**
     * extension of the loaded model
     */
    private String loadedModelExtension = null;
    /**
     * filesystem path of the loaded model, less extension
     */
    private String loadedModelFilePath = null;
    /**
     * name of the loaded model
     */
    private String modelName = null;
    // *************************************************************************
    // new methods exposed

    /**
     * Add a pose animation to the model. The new animation has zero duration, a
     * single keyframe at t=0, and all the tracks are BoneTracks, set to the
     * current pose.
     *
     * @param animationName name for the new animation (not null, not empty)
     */
    public void addPoseAnimation(String animationName) {
        Validate.nonEmpty(animationName, "animation name");
        /*
         * Check whether the name is in use.
         */
        AnimControl control = getAnimControl();
        Collection<String> names = control.getAnimationNames();
        if (names.contains(animationName)) {
            logger.log(Level.WARNING, "replacing existing animation {0}",
                    MyString.quote(animationName));
            Animation oldAnimation = control.getAnim(animationName);
            control.removeAnim(oldAnimation);
        }

        Animation poseAnim = captureCurrentPose(animationName);
        control.addAnim(poseAnim);
        setEdited();
    }

    /**
     * Add a copy of the loaded animation to the model.
     *
     * @param animationName name for the new animation (not null, not empty)
     */
    public void copyAnimation(String animationName) {
        Validate.nonEmpty(animationName, "animation name");
        /*
         * Check whether the name is in use.
         */
        AnimControl control = getAnimControl();
        Collection<String> names = control.getAnimationNames();
        if (names.contains(animationName)) {
            logger.log(Level.WARNING, "replacing existing animation {0}",
                    MyString.quote(animationName));
            Animation oldAnimation = control.getAnim(animationName);
            control.removeAnim(oldAnimation);
        }

        Animation loaded = animation.getLoadedAnimation();
        float duration = animation.getDuration();
        Animation copy = new Animation(animationName, duration);
        if (loaded != null) {
            Track[] loadedTracks = loaded.getTracks();
            for (Track track : loadedTracks) {
                Track clone = track.clone();
                copy.addTrack(clone);
            }
        }
        control.addAnim(copy);
        setEdited();
    }

    /**
     * Count the bones in the loaded model.
     *
     * @return count (&ge;0)
     */
    public int countBones() {
        Skeleton skeleton = getSkeleton();
        int count = skeleton.getBoneCount();

        assert count >= 0 : count;
        return count;
    }

    /**
     * Delete the loaded animation. If successful, the caller should immediately
     * load a different animation.
     *
     * @return true if successful, otherwise false
     */
    public boolean deleteAnimation() {
        if (animation.isBindPoseLoaded()) {
            logger.log(Level.WARNING, "cannot delete bind pose");
            return false;
        }
        AnimControl animControl = getAnimControl();
        Animation anim = animation.getLoadedAnimation();
        animControl.removeAnim(anim);
        setEdited();

        return true;
    }

    /**
     * Access the named animation.
     *
     * @param name (not null)
     * @return the pre-existing instance, or null if not found
     */
    Animation getAnimation(String name) {
        Validate.nonNull(name, "animation name");

        AnimControl animControl = getAnimControl();
        Animation anim = animControl.getAnim(name);

        return anim;
    }

    /**
     * Access the AnimControl of the loaded model.
     *
     * @return the pre-existing instance, or null if none
     */
    AnimControl getAnimControl() {
        AnimControl animControl = rootSpatial.getControl(AnimControl.class);
        if (animControl == null) {
            String message = String.format(
                    "expected model %s to have an AnimControl",
                    MyString.quote(modelName));
            throw new IllegalArgumentException(message);
            //TODO add a new control
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
            Animation anim = getAnimation(animationName);
            if (anim == null) {
                logger.log(Level.WARNING, "no bone named {0}",
                        MyString.quote(animationName));
                result = 0f;
            } else {
                result = anim.getLength();
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
     * Access the skeleton of the loaded model.
     *
     * @return the pre-existing instance (not null)
     */
    Skeleton getSkeleton() {
        Skeleton skeleton = MySkeleton.getSkeleton(rootSpatial);
        assert skeleton != null;
        return skeleton;
    }

    /**
     * Test whether the skeleton contains the named bone.
     *
     * @param name (not null)
     * @return true if found or noBone, otherwise false
     */
    public boolean hasBone(String name) {
        if (name.equals(noBone)) {
            return true;
        }
        Bone b = MySkeleton.getBone(rootSpatial, name);
        if (b == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether the selected bone has a BoneTrack.
     *
     * @return true if a bone is selected and it has a track, otherwise false
     */
    public boolean hasTrack() {
        BoneTrack track = findTrack();
        if (track == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether the named bone is a leaf bone, with no children.
     *
     * @param boneName which bone to test (not null)
     * @return true for a leaf bone, otherwise false
     */
    public boolean isLeafBone(String boneName) {
        if (boneName.equals(noBone)) {
            return false;
        }
        Bone b = MySkeleton.getBone(rootSpatial, boneName);
        if (b == null) {
            return false;
        }
        ArrayList<Bone> children = b.getChildren();
        if (children.isEmpty()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Test whether there are unsaved changes.
     *
     * @return true if no unsaved changes, false if there are some
     */
    public boolean isPristine() {
        return pristine;
    }

    /**
     * Enumerate all known animations and poses for the loaded model.
     *
     * @return a new collection of names
     */
    public Collection<String> listAnimationNames() {
        Collection<String> names = MyAnimation.listAnimations(rootSpatial);
        names.add(LoadedAnimation.bindPoseName);

        return names;
    }

    /**
     * Enumerate all bones in the loaded model.
     *
     * @return a new list of names
     */
    public List<String> listBoneNames() {
        List<String> boneNames = MySkeleton.listBones(rootSpatial);
        boneNames.remove("");
        boneNames.add(noBone);

        return boneNames;
    }

    /**
     * Enumerate all bones in the loaded model having names that start with the
     * specified prefix.
     *
     * @param namePrefix
     * @return a new list of names
     */
    public List<String> listBoneNames(String namePrefix) {
        List<String> boneNames = listBoneNames();
        for (String name : MyString.toArray(boneNames)) {
            if (!name.startsWith(namePrefix)) {
                boneNames.remove(name);
            }
        }

        return boneNames;
    }

    /**
     * Enumerate all children of the named bone.
     *
     * @param parentName
     * @return a new list of names
     */
    public List<String> listChildBoneNames(String parentName) {
        Skeleton skeleton = getSkeleton();
        Bone parent = skeleton.getBone(parentName);
        List<Bone> children = parent.getChildren();
        List<String> boneNames = new ArrayList<>(children.size());
        for (Bone b : children) {
            String name = b.getName();
            boneNames.add(name);
        }
        boneNames.remove("");

        return boneNames;
    }

    /**
     * Enumerate root bones in the loaded model.
     *
     * @return a new list of names
     */
    public List<String> listRootBoneNames() {
        Skeleton skeleton = getSkeleton();
        Bone[] roots = skeleton.getRoots();
        List<String> boneNames = new ArrayList<>(5);
        for (Bone b : roots) {
            String name = b.getName();
            boneNames.add(name);
        }
        boneNames.remove("");

        return boneNames;
    }

    /**
     * Enumerate all keyframes of the selected bone in the loaded animation.
     *
     * @return a new list, or null if no options
     */
    List<String> listKeyframes() {
        List<String> result = null;
        if (animation.isBindPoseLoaded()) {
            logger.log(Level.INFO, "No animation is selected.");
        } else if (!bone.isBoneSelected()) {
            logger.log(Level.INFO, "No bone is selected.");
        } else if (!isTrackSelected()) {
            logger.log(Level.INFO, "No track is selected.");
        } else {
            BoneTrack track = findTrack();
            float[] keyframes = track.getTimes();

            result = new ArrayList<>(20);
            for (float keyframe : keyframes) {
                String menuItem = String.format("%.3f", keyframe);
                result.add(menuItem);
            }
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
        }

        rootSpatial = loaded.clone();
        Maud.viewState.setModel(loaded);

        animation.loadBindPose();
        bone.selectNoBone();
        spatial.selectModelRoot();
        setPristine();
        Maud.gui.model.update();
        Maud.gui.skeleton.update();

        return true;
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
        }

        rootSpatial = loaded.clone();
        Maud.viewState.setModel(loaded);

        animation.loadBindPose();
        bone.selectNoBone();
        spatial.selectModelRoot();
        setPristine();
        Maud.gui.model.update();
        Maud.gui.skeleton.update();

        return true;
    }

    /**
     * Unload the current model, if any, and load the named one.
     *
     * @param name name of model to load (not null)
     * @return true if successful, otherwise false
     */
    public boolean loadModelNamed(String name) {
        String extension;
        if (name.equals("Jaime")) {
            extension = "j3o";
        } else {
            extension = "mesh.xml";
        }
        String assetPath = String.format("Models/%s/%s.%s",
                name, name, extension);

        Spatial loaded = loadModelFromAsset(assetPath, false);
        if (loaded == null) {
            return false;
        }

        rootSpatial = loaded.clone();
        Maud.viewState.setModel(loaded);

        modelName = name;

        animation.loadBindPose();
        bone.selectNoBone();
        spatial.selectModelRoot();
        setPristine();
        Maud.gui.model.update();
        Maud.gui.skeleton.update();

        return true;
    }

    /**
     * Rename the loaded animation.
     *
     * @param newName new name (not null)
     * @return true if successful, otherwise false
     */
    public boolean renameAnimation(String newName) {
        Validate.nonNull(newName, "animation name");

        if (newName.equals(LoadedAnimation.bindPoseName)
                || newName.isEmpty()) {
            logger.log(Level.WARNING, "Rename failed: {0} is a reserved name.",
                    MyString.quote(newName));
            return false;

        } else if (getAnimation(newName) != null) {
            logger.log(Level.WARNING,
                    "Rename failed: an animation named {0} already exists.",
                    MyString.quote(newName));
            return false;
        }
        if (animation.isBindPoseLoaded()) {
            logger.log(Level.WARNING,
                    "Rename failed: cannot rename bind pose.");
            return false;
        }

        Animation oldAnimation = animation.getLoadedAnimation();
        float length = oldAnimation.getLength();
        Animation newAnimation = new Animation(newName, length);
        for (Track track : oldAnimation.getTracks()) {
            newAnimation.addTrack(track);
        }

        AnimControl animControl = getAnimControl();
        animControl.removeAnim(oldAnimation);
        animControl.addAnim(newAnimation);
        setEdited();

        animation.rename(newName);

        return true;
    }

    /**
     * Rename the selected bone.
     *
     * @param newName new name (not null)
     * @return true if successful, otherwise false
     */
    public boolean renameBone(String newName) {
        Validate.nonNull(newName, "bone name");

        if (!bone.isBoneSelected()) {
            logger.log(Level.WARNING, "Rename failed: no bone selected.",
                    MyString.quote(newName));
            return false;

        } else if (newName.equals(noBone) || newName.isEmpty()) {
            logger.log(Level.WARNING, "Rename failed: {0} is a reserved name.",
                    MyString.quote(newName));
            return false;

        } else if (hasBone(newName)) {
            logger.log(Level.WARNING,
                    "Rename failed: a bone named {0} already exists.",
                    MyString.quote(newName));
            return false;
        }

        Bone selectedBone = bone.getBone();
        boolean success = MySkeleton.setName(selectedBone, newName);
        setEdited();
        Maud.gui.model.update();
        Maud.gui.bone.update();

        return success;
    }

    /**
     * Select the named keyframe in the selected bone track.
     *
     * @param name name of the new selection (not null)
     */
    public void selectKeyframe(String name) {
        Validate.nonNull(name, "keyframe name");
        assert isTrackSelected();

        float newTime = Float.valueOf(name);
        // TODO validate
        animation.setTime(newTime);
    }

    /**
     * Alter the asset manager used for loading models.
     *
     * @param newManager manager to use (not null)
     */
    public void setAssetManager(AssetManager newManager) {
        Validate.nonNull(newManager, "asset manager");
        assetManager = newManager;
    }

    /**
     * Alter the user rotation of the selected bone.
     *
     * @param rotation (not null, unaffected)
     */
    public void setBoneRotation(Quaternion rotation) {
        Validate.nonNull(rotation, "rotation");
        assert bone.isBoneSelected();

        int boneIndex = bone.getIndex();
        pose.setRotation(boneIndex, rotation);
    }

    /**
     * Alter the user scale of the selected bone.
     *
     * @param scale (not null, unaffected)
     */
    public void setBoneScale(Vector3f scale) {
        Validate.nonNull(scale, "scale");
        assert bone.isBoneSelected();

        int boneIndex = bone.getIndex();
        pose.setScale(boneIndex, scale);
    }

    /**
     * Alter the user translation of the selected bone.
     *
     * @param translation (not null, unaffected)
     */
    public void setBoneTranslation(Vector3f translation) {
        Validate.nonNull(translation, "translation");
        assert bone.isBoneSelected();

        int boneIndex = bone.getIndex();
        pose.setTranslation(boneIndex, translation);
    }

    /**
     * Alter the cull hint of the selected spatial.
     *
     * @param newHint new value for cull hint (not null)
     */
    public void setHint(Spatial.CullHint newHint) {
        Validate.nonNull(newHint, "cull hint");

        Spatial modelSpatial = spatial.findSpatial(rootSpatial);
        Spatial.CullHint oldHint = modelSpatial.getLocalCullHint();
        if (oldHint != newHint) {
            modelSpatial.setCullHint(newHint);
            setEdited();
            Maud.viewState.setHint(newHint);
            Maud.gui.spatial.update();
            Maud.gui.cullHint.update();
        }
    }

    /**
     * Alter the shadow mode of the selected spatial.
     *
     * @param newMode new value for shadow mode (not null)
     */
    public void setMode(RenderQueue.ShadowMode newMode) {
        Validate.nonNull(newMode, "shadow mode");

        Spatial modelSpatial = spatial.findSpatial(rootSpatial);
        RenderQueue.ShadowMode oldMode = modelSpatial.getLocalShadowMode();
        if (oldMode != newMode) {
            modelSpatial.setShadowMode(newMode);
            setEdited();
            Maud.viewState.setMode(newMode);
            Maud.gui.spatial.update();
            Maud.gui.shadowMode.update();
        }
    }

    /**
     * Write the loaded model to an asset.
     *
     * @param baseAssetPath asset path without any extension (not null)
     * @return true if successful, otherwise false
     */
    public boolean writeModelToAsset(String baseAssetPath) {
        Validate.nonNull(baseAssetPath, "asset path");

        String baseFilePath = ActionApplication.filePath(baseAssetPath);
        boolean success = writeModelToFile(baseFilePath);
        if (success) {
            loadedModelAssetPath = baseAssetPath;
            Maud.gui.model.update();
        }

        return success;
    }

    /**
     * Write the loaded model to a file.
     *
     * @param baseFilePath file path without any extension (not null)
     * @return true if successful, otherwise false
     */
    public boolean writeModelToFile(String baseFilePath) {
        Validate.nonNull(baseFilePath, "file path");

        String filePath = baseFilePath + ".j3o";
        File file = new File(filePath);
        BinaryExporter exporter = BinaryExporter.getInstance();

        boolean success = true;
        try {
            exporter.save(rootSpatial, file);
        } catch (IOException exception) {
            success = false;
        }
        if (success) {
            loadedModelAssetPath = "";
            loadedModelExtension = "j3o";
            loadedModelFilePath = baseFilePath;
            setPristine();
            logger.log(Level.INFO, "Wrote model to file {0}",
                    MyString.quote(filePath));
        } else {
            logger.log(Level.SEVERE,
                    "I/O exception while writing model to file {0}",
                    MyString.quote(filePath));
        }
        Maud.gui.model.update();

        return success;
    }
    // *************************************************************************
    // private methods

    /**
     * Capture the model's current pose as an animation. The new animation has a
     * zero duration, a single keyframe at t=0, and all its tracks are
     * BoneTracks.
     *
     * @parm animationName name for the new animation (not null)
     * @return a new instance
     */
    private Animation captureCurrentPose(String animationName) {
        assert animationName != null;
        assert !animationName.isEmpty();
        /*
         * Start with an empty animation.
         */
        float duration = 0f;
        Animation result = new Animation(animationName, duration);
        /*
         * Add a BoneTrack for each bone that's not in bind pose.
         */
        int numBones = countBones();
        Transform transform = new Transform();
        for (int boneIndex = 0; boneIndex < numBones; boneIndex++) {
            pose.copyTransform(boneIndex, transform);
            if (!Util.isIdentity(transform)) {
                Vector3f translation = transform.getTranslation();
                Quaternion rotation = transform.getRotation();
                Vector3f scale = transform.getScale();
                BoneTrack track = MyAnimation.createTrack(boneIndex,
                        translation, rotation, scale);
                result.addTrack(track);
            }
        }

        return result;
    }

    /**
     * Find the track for the selected bone in the loaded animation.
     *
     * @return the pre-existing instance, or null if none
     */
    private BoneTrack findTrack() {
        if (!bone.isBoneSelected()) {
            return null;
        }
        if (animation.isBindPoseLoaded()) {
            return null;
        }

        Animation anim = animation.getLoadedAnimation();
        int boneIndex = bone.getIndex();
        BoneTrack track = MyAnimation.findTrack(anim, boneIndex);

        return track;
    }

    /**
     * Test whether a bone track is selected.
     *
     * @return true if one is selected, false if none is selected
     */
    private boolean isTrackSelected() {
        if (bone.isBoneSelected()) {
            if (animation.isBindPoseLoaded()) {
                return false;
            }
            Track track = findTrack();
            if (track == null) {
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    /**
     * Quietly load a model asset from persistent storage without adding it to
     * the scene. If successful, set {@link #loadedModelAssetPath}.
     *
     * @param assetPath (not null)
     * @param useCache true to look in the asset manager's cache, false to force
     * a fresh load from persistent storage
     * @return an orphaned spatial, or null if the asset was not found
     */
    private Spatial loadModelFromAsset(String assetPath, boolean useCache) {
        ModelKey key = new ModelKey(assetPath);
        if (!useCache) {
            /*
             * Delete the model's key from the asset manager's cache, to force a
             * fresh load from persistent storage.
             */
            assetManager.deleteFromCache(key);
        }
        /*
         * Temporarily hush loader warnings about vertices with >4 weights.
         */
        Logger mlLogger = Logger.getLogger(MeshLoader.class.getName());
        Level oldLevel = mlLogger.getLevel();
        mlLogger.setLevel(Level.SEVERE);
        /*
         * Load the model.
         */
        Spatial loaded;
        try {
            loaded = assetManager.loadModel(key);
        } catch (AssetNotFoundException e) {
            loaded = null;
        }
        /*
         * Restore logging levels.
         */
        mlLogger.setLevel(oldLevel);

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
     * Mark the model as edited;
     */
    private void setEdited() {
        pristine = false;
        Maud.gui.model.update();
    }

    /**
     * Mark the model as pristine;
     */
    private void setPristine() {
        pristine = true;
        Maud.gui.model.update();
    }
}
