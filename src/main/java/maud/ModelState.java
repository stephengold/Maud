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

import com.jme3.animation.AnimControl;
import com.jme3.animation.Animation;
import com.jme3.animation.Bone;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.animation.Track;
import com.jme3.asset.AssetLoadException;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.asset.ModelKey;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
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
import jme3utilities.SimpleAppState;
import jme3utilities.ui.ActionApplication;

/**
 * A simple app state to manage the MVC model of the loaded CG model in the Maud
 * application.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class ModelState extends SimpleAppState {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            ModelState.class.getName());
    // *************************************************************************
    // fields

    /**
     * track unsaved changes to the cgModel
     */
    private boolean pristine = true;
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
    // constructors

    /**
     * Instantiate an uninitialized, enabled state.
     */
    ModelState() {
        super(true);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Add a pose animation to the model. The new animation has zero duration, a
     * single keyframe at t=0, and all the tracks are BoneTracks, set to the
     * current pose.
     *
     * @param animationName name for the new animation (not null, not empty)
     */
    void addPoseAnimation(String animationName) {
        assert animationName != null;
        assert !animationName.isEmpty();
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

        Animation pose = captureCurrentPose(animationName);
        control.addAnim(pose);
        pristine = false;
        Maud.gui.model.update();
    }

    /**
     * Delete the loaded animation. If successful, the caller must immediately
     * select a new animation.
     *
     * @return true if successful, otherwise false
     */
    boolean deleteAnimation() {
        if (Maud.gui.animation.isBindPoseLoaded()) {
            logger.log(Level.WARNING, "cannot delete bind pose");
            return false;
        }
        AnimControl animControl = getAnimControl();
        Animation animation = getLoadedAnimation();
        animControl.removeAnim(animation);
        pristine = false;
        Maud.gui.model.update();

        return true;
    }

    /**
     * Access the loaded animation.
     *
     * @return the pre-existing instance, or null if in bind pose
     */
    Animation getLoadedAnimation() {
        Animation result;
        if (Maud.gui.animation.isBindPoseLoaded()) {
            result = null;
        } else {
            String name = Maud.gui.animation.getName();
            result = getAnimation(name);
        }

        return result;
    }

    /**
     * Access the named animation.
     *
     * @param name (not null)
     * @return the pre-existing instance, or null if not found
     */
    Animation getAnimation(String name) {
        assert name != null;

        AnimControl animControl = getAnimControl();
        Animation animation = animControl.getAnim(name);

        return animation;
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
    String getAssetPath() {
        assert loadedModelAssetPath != null;
        return loadedModelAssetPath;
    }

    /**
     * Access the selected bone.
     *
     * @return the pre-existing instance, or null if none selected
     */
    Bone getBone() {
        Bone bone;
        if (!Maud.gui.bone.isBoneSelected()) {
            bone = null;
        } else {
            int index = Maud.gui.bone.getBoneIndex();
            bone = getSkeleton().getBone(index);
        }

        return bone;
    }

    int getBoneCount() {
        Skeleton skeleton = getSkeleton();
        int boneCount = skeleton.getBoneCount();

        return boneCount;
    }

    /**
     * Read the name of the selected bone.
     *
     * @return the name, or noBone if none selected (not null)
     */
    String getBoneName() {
        Bone bone = getBone();
        String name;
        if (bone == null) {
            name = DddGui.noBone;
        } else {
            name = bone.getName();
        }

        assert name != null;
        return name;
    }

    /**
     * Read the extension of the loaded model.
     *
     * @return extension (not null)
     */
    String getExtension() {
        assert loadedModelExtension != null;
        return loadedModelExtension;
    }

    /**
     * Read the filesystem path of the loaded model, less extension.
     *
     * @return path, or "" if not known (not null)
     */
    String getFilePath() {
        assert loadedModelFilePath != null;
        return loadedModelFilePath;
    }

    /**
     * Read the name of the loaded model.
     *
     * @return name, or "" if not known (not null)
     */
    String getName() {
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
    boolean hasBone(String name) {
        if (name.equals(DddGui.noBone)) {
            return true;
        }
        Bone bone = MySkeleton.getBone(rootSpatial, name);
        if (bone == null) {
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
    boolean hasTrack() {
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
     * @return true for a leaf bone, otherwise false
     */
    boolean isLeafBone(String boneName) {
        if (boneName.equals(DddGui.noBone)) {
            return false;
        }
        Bone bone = MySkeleton.getBone(rootSpatial, boneName);
        if (bone == null) {
            return false;
        }
        ArrayList<Bone> children = bone.getChildren();
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
    boolean isPristine() {
        return pristine;
    }

    /**
     * Enumerate all known animations and poses for the loaded model.
     *
     * @return a new collection
     */
    Collection<String> listAnimationNames() {
        Collection<String> names = MyAnimation.listAnimations(rootSpatial);
        names.add(DddGui.bindPoseName);

        return names;
    }

    /**
     * Enumerate all bones in the loaded model.
     *
     * @return a new list of names
     */
    List<String> listBoneNames() {
        List<String> boneNames = MySkeleton.listBones(rootSpatial);
        boneNames.remove("");
        boneNames.add(DddGui.noBone);

        return boneNames;
    }

    /**
     * Enumerate all bones in the loaded model having names that start with the
     * specified prefix.
     *
     * @return a new list of names
     */
    List<String> listBoneNames(String namePrefix) {
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
     * @return a new list of names
     */
    List<String> listChildBoneNames(String parentName) {
        Skeleton skeleton = getSkeleton();
        Bone parent = skeleton.getBone(parentName);
        List<Bone> children = parent.getChildren();
        List<String> boneNames = new ArrayList<>(children.size());
        for (Bone bone : children) {
            String name = bone.getName();
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
    List<String> listRootBoneNames() {
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
        if (Maud.gui.animation.isBindPoseLoaded()) {
            logger.log(Level.INFO, "No animation is selected.");
        } else if (!Maud.gui.bone.isBoneSelected()) {
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
     * @param assetPath (not null)
     * @return true if successful, otherwise false
     */
    boolean loadModelAsset(String assetPath) {
        assert assetPath != null;

        Spatial loaded = loadModelFromAsset(assetPath, false);
        if (loaded == null) {
            return false;
        }

        pristine = true;
        rootSpatial = loaded;

        Spatial viewClone = loadModelFromAsset(assetPath, true);
        assert viewClone != null;
        Maud.viewState.setModel(viewClone);
        Maud.gui.animation.loadBindPose();
        selectBone(DddGui.noBone);

        return true;
    }

    /**
     * Unload the current model, if any, and load the specified file.
     *
     * @param modelFile model file from which to load (not null)
     * @return true if successful, otherwise false
     */
    boolean loadModelFile(File modelFile) {
        assert modelFile != null;

        String canonicalPath;
        try {
            canonicalPath = modelFile.getCanonicalPath();
        } catch (IOException e) {
            return false;
        }

        Spatial loaded = loadModelFromFile(canonicalPath);
        if (loaded == null) {
            return false;
        }

        pristine = true;
        rootSpatial = loaded;

        Spatial viewClone = loadModelFromFile(canonicalPath);
        assert viewClone != null;
        Maud.viewState.setModel(viewClone);
        Maud.gui.animation.loadBindPose();
        selectBone(DddGui.noBone);
        Maud.gui.spatial.selectRootSpatial();

        return true;
    }

    /**
     * Unload the current model, if any, and load the named one.
     *
     * @param name name of model to load (not null)
     * @return true if successful, otherwise false
     */
    boolean loadModelNamed(String name) {
        assert isInitialized();
        assert name != null;

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
        } else {
            pristine = true;
            rootSpatial = loaded;

            Spatial viewClone = loadModelFromAsset(assetPath, true);
            assert viewClone != null;
            Maud.viewState.setModel(viewClone);

            modelName = name;
            Maud.gui.animation.loadBindPose();
            selectBone(DddGui.noBone);
            Maud.gui.spatial.selectRootSpatial();

            return true;
        }
    }

    /**
     * Rename the loaded animation.
     *
     * @param newName new name (not null)
     * @return true if successful, otherwise false
     */
    boolean renameAnimation(String newName) {
        assert newName != null;
        if (newName.equals(DddGui.bindPoseName)
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
        if (Maud.gui.animation.isBindPoseLoaded()) {
            logger.log(Level.WARNING,
                    "Rename failed: cannot rename bind pose.");
            return false;
        }

        Animation oldAnimation = getLoadedAnimation();
        float length = oldAnimation.getLength();
        Animation newAnimation = new Animation(newName, length);
        for (Track track : oldAnimation.getTracks()) {
            newAnimation.addTrack(track);
        }

        AnimControl animControl = getAnimControl();
        animControl.removeAnim(oldAnimation);
        animControl.addAnim(newAnimation);
        pristine = false;

        Maud.gui.animation.rename(newName);

        return true;
    }

    /**
     * Rename the selected bone.
     *
     * @param newName new name (not null)
     * @return true if successful, otherwise false
     */
    boolean renameBone(String newName) {
        assert newName != null;
        if (!Maud.gui.bone.isBoneSelected()) {
            logger.log(Level.WARNING, "Rename failed: no bone selected.",
                    MyString.quote(newName));
            return false;

        } else if (newName.equals(DddGui.noBone) || newName.isEmpty()) {
            logger.log(Level.WARNING, "Rename failed: {0} is a reserved name.",
                    MyString.quote(newName));
            return false;

        } else if (hasBone(newName)) {
            logger.log(Level.WARNING,
                    "Rename failed: a bone named {0} already exists.",
                    MyString.quote(newName));
            return false;
        }

        Bone bone = getBone();
        boolean success = MySkeleton.setName(bone, newName);
        pristine = false;
        Maud.gui.model.update();

        return success;
    }

    /**
     * Alter which bone is selected. TODO move to bone tool
     *
     * @param name bone name or noBone (not null)
     */
    void selectBone(String name) {
        assert name != null;
        if (!hasBone(name)) {
            logger.log(Level.WARNING, "Select failed: no bone named {0}.",
                    MyString.quote(name));
            return;
        }

        if (name.equals(DddGui.noBone)) {
            Maud.gui.bone.selectNoBone();
        } else {
            int boneIndex = MySkeleton.findBoneIndex(rootSpatial, name);
            Maud.gui.bone.selectBone(boneIndex);
        }
    }

    /**
     * Select the named keyframe in the selected bone track.
     *
     * @param name name of the new selection (not null)
     */
    void selectKeyframe(String name) {
        assert isTrackSelected();
        assert name != null;

        float newTime = Float.valueOf(name);
        // TODO validate
        Maud.gui.animation.setTime(newTime);
    }

    /**
     * Alter the user rotation of the selected bone.
     */
    void setBoneRotation(Quaternion rotation) {
        assert rotation != null;
        assert Maud.gui.bone.isBoneSelected();

        int boneIndex = Maud.gui.bone.getBoneIndex();
        Maud.gui.animation.setBoneRotation(boneIndex, rotation);
    }

    /**
     * Alter the user scale of the selected bone.
     */
    void setBoneScale(Vector3f scale) {
        assert scale != null;
        assert Maud.gui.bone.isBoneSelected();

        int boneIndex = Maud.gui.bone.getBoneIndex();
        Maud.gui.animation.setBoneScale(boneIndex, scale);
    }

    /**
     * Alter the user translation of the selected bone.
     */
    void setBoneTranslation(Vector3f translation) {
        assert translation != null;
        assert Maud.gui.bone.isBoneSelected();

        int boneIndex = Maud.gui.bone.getBoneIndex();
        Maud.gui.animation.setBoneTranslation(boneIndex, translation);
    }

    /**
     * Write the loaded model to an asset.
     *
     * @param baseAssetPath asset path without any extension (not null)
     */
    boolean writeModelToAsset(String baseAssetPath) {
        assert baseAssetPath != null;

        String baseFilePath = ActionApplication.filePath(baseAssetPath);
        boolean success = writeModelToFile(baseFilePath);
        if (success) {
            loadedModelAssetPath = baseAssetPath;
        }

        return success;
    }

    /**
     * Write the loaded model to a file.
     *
     * @param baseFilePath file path without any extension (not null)
     */
    boolean writeModelToFile(String baseFilePath) {
        assert baseFilePath != null;

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
            pristine = true;
            logger.log(Level.INFO, "Wrote model to file {0}",
                    MyString.quote(filePath));
        } else {
            logger.log(Level.SEVERE,
                    "I/O exception while writing model to file {0}",
                    MyString.quote(filePath));
        }

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

        Skeleton skeleton = getSkeleton();
        skeleton.updateWorldVectors();
        /*
         * Add a BoneTrack for each bone in the skeleton.
         */
        int numBones = skeleton.getBoneCount();
        for (int boneIndex = 0; boneIndex < numBones; boneIndex++) {
            Transform transform = Maud.gui.animation.copyBoneTransform(
                    boneIndex);
            Vector3f translation = transform.getTranslation();
            Quaternion rotation = transform.getRotation();
            Vector3f scale = transform.getScale();
            BoneTrack track = MyAnimation.createTrack(boneIndex, translation,
                    rotation, scale);
            result.addTrack(track);
        }

        return result;
    }

    /**
     * Find the track for the selected bone in the loaded animation.
     *
     * @return the pre-existing instance, or null if none
     */
    private BoneTrack findTrack() {
        if (!Maud.gui.bone.isBoneSelected()) {
            return null;
        }
        if (Maud.gui.animation.isBindPoseLoaded()) {
            return null;
        }

        Animation animation = getLoadedAnimation();
        int boneIndex = Maud.gui.bone.getBoneIndex();
        BoneTrack track = MyAnimation.findTrack(animation, boneIndex);

        return track;
    }

    /**
     * Test whether a bone track is selected.
     *
     * @return true if one is selected, false if none is selected
     */
    private boolean isTrackSelected() {
        if (Maud.gui.bone.isBoneSelected()) {
            if (Maud.gui.animation.isBindPoseLoaded()) {
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
        InputStream inputStream = null;
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
}
