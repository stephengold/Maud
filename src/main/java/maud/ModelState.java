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
 * Stephen Gold's name may not be used to endorse or promote products
 derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL STEPHEN GOLD BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package maud;

import com.jme3.animation.AnimControl;
import com.jme3.animation.Animation;
import com.jme3.animation.Bone;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.animation.Track;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyAnimation;
import jme3utilities.MySkeleton;
import jme3utilities.MyString;
import jme3utilities.SimpleAppState;
import jme3utilities.math.MyMath;
import jme3utilities.ui.ActionApplication;

/**
 * Model state for the Maud application.
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
     * animation speed (0 &rarr; paused, 1 &rarr; normal speed)
     */
    private float animationSpeed = 0f;
    /**
     * animation time (in seconds, &ge;0)
     */
    private float animationTime = 0f;
    /**
     * user transforms for the current (temporary) pose
     */
    final private List<Transform> currentPose = new ArrayList<>(30);
    /**
     * the model's root spatial
     */
    private Spatial rootSpatial = null;
    /**
     * asset path of the loaded model, less extension
     */
    private String loadedModelAssetPath = null;
    /**
     * filesystem path of the loaded model, less extension
     */
    private String loadedModelFilePath = null;
    /**
     * name of the loaded model
     */
    private String modelName = null;
    /**
     * name of the selected animation or bindPoseName
     */
    private String selectedAnimationName = null;
    /**
     * name of the selected bone or noBone
     */
    private String selectedBoneName = null;
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
    }

    /**
     * Calculate rotation angles of the selected bone in the current pose.
     *
     * @param storeAngles (&ge;3 elements, modified unless null)
     */
    void boneAngles(float[] storeAngles) {
        int boneIndex = MySkeleton.findBoneIndex(rootSpatial, selectedBoneName);
        Transform transform = currentPose.get(boneIndex);
        Quaternion rotation = transform.getRotation(null);
        rotation.toAngles(storeAngles);
    }

    /**
     * Delete the loaded animation. The caller must immediately select a new
     * animation.
     */
    void deleteAnimation() {
        if (isBindPoseSelected()) {
            logger.log(Level.WARNING, "cannot delete bind pose");
            return;
        }
        AnimControl animControl = getAnimControl();
        Animation animation = getAnimation();
        animControl.removeAnim(animation);
    }

    /**
     * Access the loaded animation.
     *
     * @return the pre-existing instance, or null if none or in bind pose
     */
    Animation getAnimation() {
        if (isBindPoseSelected()) {
            return null;
        } else {
            Animation animation = getAnimation(selectedAnimationName);
            return animation;
        }
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
     * Read the name of the loaded animation.
     *
     * @return the name, or bindPoseName if in bind pose (not null)
     */
    String getAnimationName() {
        assert selectedAnimationName != null;
        return selectedAnimationName;
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
     * @return name (not null)
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
        if (!isBoneSelected()) {
            return null;
        } else {
            Bone bone = MySkeleton.getBone(rootSpatial, selectedBoneName);
            return bone;
        }
    }

    /**
     * Read the name of the selected bone.
     *
     * @return the name, or noBone if none selected (not null)
     */
    String getBoneName() {
        String result = selectedBoneName;
        assert result != null;
        return result;
    }

    /**
     * Read the duration of the loaded animation.
     *
     * @return time (in seconds, &ge;0)
     */
    float getDuration() {
        float result = getDuration(selectedAnimationName);
        assert result >= 0f : result;
        return result;
    }

    /**
     * Read the duration of a named animation.
     *
     * @param name
     * @return time (in seconds, &ge;0)
     */
    float getDuration(String name) {
        assert name != null;

        if (name.equals(HudState.bindPoseName)) {
            return 0f;
        } else {
            Animation animation = getAnimation(name);
            float result = animation.getLength();
            assert result >= 0f : result;
            return result;
        }
    }

    /**
     * Read the name of the loaded model.
     *
     * @return name (not null)
     */
    String getName() {
        assert modelName != null;
        return modelName;
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
     * Access the root spatial of the model.
     *
     * @return the pre-existing instance (not null)
     */
    Spatial getSpatial() {
        assert rootSpatial != null;
        return rootSpatial;
    }

    /**
     * Read the animation speed.
     *
     * @return relative speed (&ge;0, 1 &rarr; normal)
     */
    float getSpeed() {
        assert animationSpeed >= 0f : animationSpeed;
        return animationSpeed;
    }

    /**
     * Read the animation time.
     *
     * @return seconds since start (&ge;0)
     */
    float getTime() {
        assert animationTime >= 0f : animationTime;
        return animationTime;
    }

    /**
     * Test whether the skeleton contains the named bone.
     *
     * @param name (not null)
     * @return true if found or noBone, otherwise false
     */
    boolean hasBone(String name) {
        if (name.equals(HudState.noBone)) {
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
        BoneTrack track = getTrack();
        if (track == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether an animation is running.
     *
     * @return true if an animation is running, false otherwise
     */
    boolean isAnimationRunning() {
        if (animationSpeed == 0f) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether the bind pose is selected.
     *
     * @return true if it's selected, false if an animation is selected
     */
    boolean isBindPoseSelected() {
        if (selectedAnimationName.equals(HudState.bindPoseName)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Test whether a bone is selected.
     *
     * @return true if one is selected, false if none is selected
     */
    boolean isBoneSelected() {
        if (selectedBoneName.equals(HudState.noBone)) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * List the names of all known animations and poses for the loaded model.
     *
     * @return a new collection
     */
    Collection<String> listAnimationNames() {
        Collection<String> names = MyAnimation.listAnimations(rootSpatial);
        names.add(HudState.bindPoseName);

        return names;
    }

    /**
     * List the names of all bones in the loaded model.
     *
     * @return a new list of names
     */
    List<String> listBoneNames() {
        List<String> boneNames = MySkeleton.listBones(rootSpatial);
        boneNames.remove("");
        boneNames.add(HudState.noBone);

        return boneNames;
    }

    /**
     * List all bones in the loaded model whose names begin with the specified
     * prefix.
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
     * List all keyframes of the selected bone in the loaded animation.
     *
     * @return a new list, or null if no options
     */
    List<String> listKeyframes() {
        if (isBindPoseSelected()) {
            logger.log(Level.INFO, "No animation is selected.");
            return null;
        }
        if (!isBoneSelected()) {
            logger.log(Level.INFO, "No bone is selected.");
            return null;
        }
        if (!isTrackSelected()) {
            logger.log(Level.INFO, "No track is selected.");
            return null;
        }

        BoneTrack track = getTrack();
        float[] keyframes = track.getTimes();

        List<String> result = new ArrayList<>(20);
        for (float keyframe : keyframes) {
            String menuItem = String.format("%.3f", keyframe);
            result.add(menuItem);
        }

        return result;
    }

    /**
     * List all meshes in the loaded model.
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
     * Unload the current model, if any, and load a new one.
     *
     * @param name name of model to load (not null)
     * @return true if successful, otherwise false
     */
    boolean load(String name) {
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
            modelName = name;
            rootSpatial = loaded;

            Spatial viewClone = loadModelFromAsset(assetPath, true);
            assert viewClone != null;
            Maud.viewState.setModel(viewClone);

            loadBindPose();
            selectBone(HudState.noBone);

            return true;
        }
    }

    /**
     * Load an animation (not bind pose) and set the playback speed.
     *
     * @para name name of animation
     * @param speed (&ge;0)
     */
    void loadAnimation(String name, float speed) {
        assert name != null;
        assert !name.equals(HudState.bindPoseName);
        assert speed >= 0f : speed;

        selectedAnimationName = name;
        animationTime = 0f;
        animationSpeed = speed;
        poseSkeleton();
    }

    /**
     * Load the bind pose.
     */
    void loadBindPose() {
        selectedAnimationName = HudState.bindPoseName;
        animationTime = 0f;
        animationSpeed = 0f;

        Skeleton skeleton = getSkeleton();
        int boneCount = skeleton.getBoneCount();
        currentPose.clear();
        for (int boneIndex = 0; boneIndex < boneCount; boneIndex++) {
            Transform transform = new Transform();
            currentPose.add(transform);
        }

        Maud.viewState.poseSkeleton(currentPose);
    }

    /**
     * Pose the skeleton to current animation or pose.
     */
    void poseSkeleton() {
        Animation animation = getAnimation();
        Skeleton skeleton = getSkeleton();
        int boneCount = skeleton.getBoneCount();
        for (int boneIndex = 0; boneIndex < boneCount; boneIndex++) {
            Transform transform = currentPose.get(boneIndex);
            BoneTrack track = MyAnimation.findTrack(animation, boneIndex);
            if (track == null) {
                transform.loadIdentity();
            } else {
                Util.boneTransform(track, animationTime, transform);
            }
        }

        Maud.viewState.poseSkeleton(currentPose);
    }

    /**
     * Rename the loaded animation.
     *
     * @param newName new name (not null)
     * @return true if successful, otherwise false
     */
    boolean renameAnimation(String newName) {
        assert newName != null;
        if (newName.equals(HudState.bindPoseName)
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
        String oldName = getAnimationName();
        if (oldName.equals(HudState.bindPoseName)) {
            logger.log(Level.WARNING,
                    "Rename failed: cannot rename bind pose.");
            return false;
        }

        Animation oldAnimation = getAnimation();
        float length = oldAnimation.getLength();
        Animation newAnimation = new Animation(newName, length);
        for (Track track : oldAnimation.getTracks()) {
            newAnimation.addTrack(track);
        }

        AnimControl animControl = getAnimControl();
        animControl.removeAnim(oldAnimation);
        animControl.addAnim(newAnimation);

        selectedAnimationName = newName;

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
        if (newName.equals(HudState.noBone) || newName.isEmpty()) {
            logger.log(Level.WARNING, "Rename failed: {0} is a reserved name.",
                    MyString.quote(newName));
            return false;

        } else if (hasBone(newName)) {
            logger.log(Level.WARNING,
                    "Rename failed: a bone named {0} already exists.",
                    MyString.quote(newName));
            return false;
        }
        String oldName = getBoneName();
        if (oldName.equals(HudState.noBone)) {
            logger.log(Level.WARNING, "Rename failed: no bone selected.",
                    MyString.quote(newName));
            return false;

        }

        Bone bone = getBone();
        boolean success = MySkeleton.setName(bone, newName);
        selectedBoneName = newName;

        return success;
    }

    /**
     * Alter which bone is selected.
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

        selectedBoneName = name;
        /*
         * Update the view.
         */
        if (name.equals(HudState.noBone)) {
            Maud.viewState.selectNoBone();
        } else {
            int boneIndex = MySkeleton.findBoneIndex(rootSpatial, name);
            Maud.viewState.selectBone(boneIndex);
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
        animationTime = newTime;
    }

    /**
     * Alter the user rotation of the selected bone.
     */
    void setBoneRotation(Quaternion rotation) {
        assert rotation != null;
        assert isBoneSelected();

        int boneIndex = MySkeleton.findBoneIndex(rootSpatial, selectedBoneName);
        Transform boneTransform = currentPose.get(boneIndex);
        boneTransform.setRotation(rotation);

        Maud.viewState.poseSkeleton(currentPose);
    }

    /**
     * Alter the animation speed. No effect in bind pose or if the loaded
     * animation has zero duration.
     *
     * @param speed relative speed (&ge;0, 1 &rarr; normal)
     */
    void setSpeed(float speed) {
        assert speed >= 0f : speed;

        if (!isBindPoseSelected() && getDuration() > 0f) {
            animationSpeed = speed;
        }
    }

    /**
     * Write the loaded model to an asset.
     *
     * @param baseAssetPath asset path without any extension (not null)
     */
    boolean writeModelToAsset(String baseAssetPath) {
        assert baseAssetPath != null;

        String fullAssetPath = baseAssetPath + ".j3o";
        String filePath = ActionApplication.filePath(fullAssetPath);
        File file = new File(filePath);
        BinaryExporter exporter = BinaryExporter.getInstance();

        boolean success = true;
        try {
            exporter.save(rootSpatial, file);
        } catch (IOException exception) {
            success = false;
        }
        if (success) {
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
    // SimpleAppState methods

    /**
     * Callback to update this state prior to rendering. (Invoked once per
     * render pass.)
     *
     * @param elapsedTime time interval between render passes (in seconds,
     * &ge;0)
     */
    @Override
    public void update(float elapsedTime) {
        float duration = getDuration();
        if (duration != 0f && animationSpeed != 0f) {
            float newTime = animationTime + animationSpeed * elapsedTime;
            animationTime = MyMath.modulo(newTime, duration);
            poseSkeleton();
        }
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
            Transform transform = currentPose.get(boneIndex);
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
        if (!isBoneSelected()) {
            return null;
        }
        if (isBindPoseSelected()) {
            return null;
        }

        int boneIndex = MySkeleton.findBoneIndex(rootSpatial, selectedBoneName);
        Animation animation = getAnimation();
        BoneTrack track = MyAnimation.findTrack(animation, boneIndex);

        return track;
    }

    /**
     * Access the selected BoneTrack.
     *
     * @return the pre-existing instance, or null if no track is selected
     */
    private BoneTrack getTrack() {
        if (isBoneSelected()) {
            BoneTrack result = findTrack();
            return result;
        }
        assert !isTrackSelected();
        return null;
    }

    /**
     * Test whether a bone track is selected.
     *
     * @return true if one is selected, false if none is selected
     */
    private boolean isTrackSelected() {
        if (isBoneSelected()) {
            if (isBindPoseSelected()) {
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
            logger.log(Level.SEVERE, "Failed to find asset {0}",
                    MyString.quote(assetPath));
        } else {
            logger.log(Level.INFO, "Loaded model from asset {0}",
                    MyString.quote(assetPath));

            String extension = key.getExtension();
            int extLength = extension.length();
            if (extLength == 0) {
                loadedModelAssetPath = assetPath;
            } else {
                int pathLength = assetPath.length() - extLength - 1;
                loadedModelAssetPath = assetPath.substring(0, pathLength);
            }
        }

        return loaded;
    }

    /**
     * Replace the model in the scene.
     *
     * @param assetPath (not null)
     * @return the loaded spatial, or null if not found
     */
    private void setModel(Spatial loaded) {
    }
}
