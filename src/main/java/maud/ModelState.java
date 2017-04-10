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

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.Animation;
import com.jme3.animation.Bone;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.LoopMode;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.animation.Track;
import com.jme3.asset.ModelKey;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.ogre.MeshLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyAnimation;
import jme3utilities.MySkeleton;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.SimpleAppState;
import jme3utilities.debug.AxesControl;
import jme3utilities.debug.SkeletonDebugControl;

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
    /**
     * local copy of {@link com.jme3.math.Transform#IDENTITY}
     */
    final private static Transform identityTransform = new Transform();
    /**
     * local copy of {@link com.jme3.math.Vector3f#UNIT_XYZ}
     */
    final private static Vector3f identityScale = new Vector3f(1f, 1f, 1f);
    /**
     * local copy of {@link com.jme3.math.Vector3f#ZERO}
     */
    final private static Vector3f identityTranslate = new Vector3f(0f, 0f, 0f);
    // *************************************************************************
    // fields

    /**
     * animation channel (Can be null only if no model selected.
     * getAnimationName() == null means bind pose is selected.)
     */
    private AnimChannel channel = null;
    /**
     * true if the loaded model has user control enabled, otherwise false
     */
    private boolean userControlFlag = false;
    /**
     * inverse local rotation of each bone in original bind pose, indexed by
     * boneIndex
     */
    final private List<Quaternion> inverseBindPose = new ArrayList<>(30);
    /**
     * attachments node for the selected bone, or null if none selected
     */
    private Node attachmentsNode = null;
    /**
     * the skeleton debug control (set by #load())
     */
    private SkeletonDebugControl skeletonDebugControl = null;
    /**
     * the model's spatial (set by #load())
     */
    private Spatial spatial = null;
    /**
     * name of the loaded model (set by #load())
     */
    private String loadedModelName = null;
    /**
     * name of the selected bone or noBone (not null)
     */
    private String selectedBoneName = HudState.noBone;
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
     * Add a pose animation to the loaded model and select the new animation.
     * The new animation has zero duration, a single keyframe at t=0, and all
     * the tracks are BoneTracks, set to the current pose.
     *
     * @param animationName name for the new animation (not null , not empty)
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
     * Delete the loaded animation.
     */
    void deleteAnimation() {
        if (isBindPoseSelected()) {
            logger.log(Level.WARNING, "Cannot delete bind pose.");
            return;
        }
        AnimControl animControl = getAnimControl();
        String animationName = channel.getAnimationName();
        Animation animation = animControl.getAnim(animationName);
        animControl.removeAnim(animation);

        loadBindPose();
    }

    /**
     * Calculate the rotation angles of the selected bone.
     *
     * @param storeAngles (&ge;3 elements, modified)
     */
    void boneAngles(float[] storeAngles) {
        int boneIndex = MySkeleton.findBoneIndex(spatial, selectedBoneName);
        Quaternion rotation = boneRotation(boneIndex);
        rotation.toAngles(storeAngles);
    }

    /**
     * Access the loaded animation.
     *
     * @return the pre-existing instance, or null if none or in bind pose
     */
    Animation getAnimation() {
        if (isBindPoseSelected()) {
            return null;
        }
        String animationName = channel.getAnimationName();
        Animation animation = getAnimation(animationName);

        return animation;
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
     * Access the selected bone.
     *
     * @return the pre-existing instance, or null if none selected
     */
    Bone getBone() {
        if (!isBoneSelected()) {
            return null;
        }
        Bone bone = MySkeleton.getBone(spatial, selectedBoneName);

        return bone;
    }

    /**
     * Access the AxesControl for the selected bone.
     *
     * @return the pre-existing instance, or null if no bone selected
     */
    AxesControl getBoneAxesControl() {
        if (attachmentsNode == null) {
            return null;
        }
        AxesControl result = attachmentsNode.getControl(AxesControl.class);
        assert result != null;

        return result;
    }

    /**
     * Read the name of the loaded animation.
     *
     * @return the name, or bindPoseName if in bind pose (not null)
     */
    String getAnimationName() {
        String result;
        if (isBindPoseSelected()) {
            result = HudState.bindPoseName;
        } else {
            result = channel.getAnimationName();
        }

        assert result != null;
        return result;
    }

    /**
     * Access the AnimControl of the loaded model.
     *
     * @return the pre-existing instance, or null if none
     */
    AnimControl getAnimControl() {
        AnimControl animControl = spatial.getControl(AnimControl.class);
        if (animControl == null) {
            String message = String.format(
                    "expected model %s to have an AnimControl",
                    MyString.quote(loadedModelName));
            throw new IllegalArgumentException(message);
        }

        return animControl;
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
        Animation animation = getAnimation();
        float result = animation.getLength();

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

        Animation animation = getAnimation(name);
        float result = animation.getLength();

        assert result >= 0f : result;
        return result;
    }

    /**
     * Read the name of the loaded model.
     *
     * @return name (not null)
     */
    String getName() {
        assert loadedModelName != null;
        return loadedModelName;
    }

    /**
     * Access the skeleton of the loaded model.
     *
     * @return the pre-existing instance (not null)
     */
    Skeleton getSkeleton() {
        Skeleton skeleton = MySkeleton.getSkeleton(spatial);

        assert skeleton != null;
        return skeleton;
    }

    /**
     * Access the skeleton debug control.
     *
     * @return the pre-existing instance (not null)
     */
    SkeletonDebugControl getSkeletonDebugControl() {
        assert skeletonDebugControl != null;
        return skeletonDebugControl;
    }

    /**
     * Access the spatial which represents the model in the scene.
     *
     * @return the pre-existing instance (not null)
     */
    Spatial getSpatial() {
        assert spatial != null;
        return spatial;
    }

    /**
     * Read the animation speed.
     *
     * @return relative speed (&ge;0, 1 &rarr; normal)
     */
    float getSpeed() {
        if (isBindPoseSelected()) {
            return 0f;
        }
        float result = channel.getSpeed();

        assert result >= 0f : result;
        return result;
    }

    /**
     * Read the animation time.
     *
     * @return seconds since start (&ge;0)
     */
    float getTime() {
        float result;
        if (getDuration() == 0f) {
            result = 0f;
        } else {
            result = channel.getTime();
        }

        assert result >= 0f : result;
        return result;
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
        Bone bone = MySkeleton.getBone(spatial, name);
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
        if (getSpeed() == 0f) {
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
        if (channel == null) {
            return true;
        }
        String animationName = channel.getAnimationName();
        if (animationName == null) {
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
     * Test whether user control is enabled.
     *
     * @return true if enabled, false if disabled
     */
    boolean isUserControl() {
        return userControlFlag;
    }

    /**
     * List the names of all known animations and poses for the loaded model.
     *
     * @return a new collection
     */
    Collection<String> listAnimationNames() {
        Collection<String> names = MyAnimation.listAnimations(spatial);
        names.add(HudState.bindPoseName);

        return names;
    }

    /**
     * List the names of all bones in the loaded model.
     *
     * @return a new list
     */
    List<String> listBoneNames() {
        List<String> boneNames = MySkeleton.listBones(spatial);
        boneNames.remove("");
        boneNames.add(HudState.noBone);

        return boneNames;
    }

    /**
     * List all bones in the loaded model whose names begin with the specified
     * prefix.
     *
     * @return a new list
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
        if (channel == null || channel.getAnimationName() == null) {
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

        Node node = (Node) spatial;
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
        /*
         * Temporarily hush loader warnings about vertices with >4 weights.
         */
        Logger mlLogger = Logger.getLogger(MeshLoader.class.getName());
        Level save = mlLogger.getLevel();
        mlLogger.setLevel(Level.SEVERE);

        String assetPath;
        if (name.equals("Jaime")) {
            assetPath = "Models/Jaime/Jaime.j3o";
        } else {
            assetPath = String.format("Models/%s/%s.mesh.xml", name, name);
        }
        /*
         * Load the model from its asset, bypassing the asset manager's cache.
         */
        ModelKey key = new ModelKey(assetPath);
        assetManager.deleteFromCache(key);
        Spatial loaded = assetManager.loadModel(key);

        mlLogger.setLevel(save);
        /*
         * Detach the old spatial (if any) from the scene.
         */
        if (spatial != null) {
            rootNode.detachChild(spatial);
            channel = null;
        }

        loadedModelName = name;
        spatial = loaded;
        userControlFlag = false;
        MySkeleton.setUserControl(spatial, userControlFlag);
        spatial.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        rootNode.attachChild(spatial);
        saveInverseBindPose();
        /*
         * Apply an identity transform to every child spatial of the model.
         * This hack enables accurate bone attachments on models with
         * locally transformed geometries (such as Jaime).  (The attachments bug
         * should be fixed in jME 3.2.)
         */
        if (spatial instanceof Node) {
            Node node = (Node) spatial;
            for (Spatial child : node.getChildren()) {
                child.setLocalTransform(identityTransform);
            }
        }
        /*
         * Scale and translate the model so its bind pose is 1 world-unit
         * tall, with its base resting on the XZ plane.
         */
        float maxY = MySpatial.getMaxY(spatial);
        float minY = MySpatial.getMinY(spatial);
        assert maxY > minY : maxY; // no 2D models!
        float worldScale = 1f / (maxY - minY);
        MySpatial.setWorldScale(spatial, worldScale);
        Vector3f worldLocation = new Vector3f(0f, -minY * worldScale, 0f);
        MySpatial.setWorldLocation(spatial, worldLocation);
        /*
         * Add a skeleton debug control to the spatial.
         */
        skeletonDebugControl = new SkeletonDebugControl(assetManager);
        spatial.addControl(skeletonDebugControl);

        loadBindPose();
        selectBone(HudState.noBone);

        return true;
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

        channel.setAnim(name, 0f);
        if (speed > 0f) {
            channel.setLoopMode(LoopMode.Loop);
        }
        channel.setSpeed(speed);
    }

    /**
     * Load the bind pose.
     */
    void loadBindPose() {
        if (channel == null) {
            AnimControl control = getAnimControl();
            channel = control.createChannel();
        }
        channel.reset(false);
        resetSkeleton();
    }

    /**
     * Load an animation pose under user control.
     */
    void poseSkeleton() {
        /*
         * Save user-control status and enable it.
         */
        boolean savedStatus = userControlFlag;
        setUserControl(true);
        /*
         * Copy bone rotations from pose to skeleton.
         */
        Animation animation = getAnimation();
        Skeleton skeleton = getSkeleton();
        int boneCount = skeleton.getBoneCount();
        for (int boneIndex = 0; boneIndex < boneCount; boneIndex++) {
            BoneTrack track = MyAnimation.findTrack(animation, boneIndex);

            Vector3f translation, scale;
            Quaternion rotation;
            if (track != null) {
                Vector3f[] translations = track.getTranslations();
                assert translations.length == 1 : translations.length;
                translation = translations[0];
                Quaternion[] rotations = track.getRotations();
                assert rotations.length == 1 : rotations.length;
                rotation = rotations[0];
                Vector3f[] scales = track.getScales();
                assert scales.length == 1 : scales.length;
                scale = scales[0];
            } else {
                translation = new Vector3f(0f, 0f, 0f);
                rotation = new Quaternion();
                scale = new Vector3f(0f, 0f, 0f);
            }

            Bone bone = skeleton.getBone(boneIndex);
            bone.setUserTransforms(translation, rotation, scale);
        }
        /*
         * Restore prior user-control status.
         */
        setUserControl(savedStatus);
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

        channel.setAnim(newName, 0f);

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

        return success;
    }

    /**
     * Rotate the model around +Y by the specified angle.
     *
     * @param angle in radians
     */
    void rotateY(float angle) {
        spatial.rotate(0f, angle, 0f);
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
        updateAttachmentsNode();
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
        channel.setTime(newTime);
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
            channel.setSpeed(speed);
        }
    }

    /**
     * Alter the user-control setting of the loaded model.
     *
     * @param newSetting true &rarr; user transforms, false &rarr; animation
     */
    void setUserControl(boolean newSetting) {
        if (newSetting == userControlFlag) {
            return;
        }
        MySkeleton.setUserControl(spatial, newSetting);
        userControlFlag = newSetting;
    }

    /**
     * Alter the user transforms of the selected bone.
     *
     * @param translation (not null)
     * @param rotation (not null)
     * @param scale (not null)
     */
    void setUserTransforms(Vector3f translation, Quaternion rotation,
            Vector3f scale) {
        assert translation != null;
        assert rotation != null;
        assert scale != null;

        Bone bone = getBone();
        bone.setUserTransforms(translation, rotation, scale);
    }
    // *************************************************************************
    // private methods

    /**
     * Calculate the rotation of a bone (local rotation minus bind rotation).
     *
     * @param boneIndex (&ge;0)
     * @return a new instance
     */
    private Quaternion boneRotation(int boneIndex) {
        assert boneIndex >= 0 : boneIndex;

        Skeleton skeleton = getSkeleton();
        Bone bone = skeleton.getBone(boneIndex);
        Quaternion localRotation = bone.getLocalRotation().clone();

        Quaternion invBind = inverseBindPose.get(boneIndex);
        Quaternion rotation = invBind.mult(localRotation);

        return rotation;
    }

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
            Vector3f translation = identityTranslate;
            Quaternion rotation = boneRotation(boneIndex);
            Vector3f scale = identityScale;
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
        if (channel == null) {
            return null;
        }
        if (isBindPoseSelected()) {
            return null;
        }

        int boneIndex = MySkeleton.findBoneIndex(spatial, selectedBoneName);
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
     * Reset the skeleton to its bind pose.
     */
    private void resetSkeleton() {
        Skeleton skeleton = getSkeleton();

        if (userControlFlag) {
            /*
             * Skeleton.reset() is ineffective with user mode enabled,
             * so load bind pose under user control.
             */
            int boneCount = skeleton.getBoneCount();
            for (int boneIndex = 0; boneIndex < boneCount; boneIndex++) {
                Bone bone = skeleton.getBone(boneIndex);
                Vector3f translation = new Vector3f(0f, 0f, 0f);
                Quaternion rotation = new Quaternion();
                Vector3f scale = new Vector3f(1f, 1f, 1f);
                bone.setUserTransforms(translation, rotation, scale);
            }

        } else {
            skeleton.reset();
        }
    }

    /**
     * Save (in memory) a copy of all inverse local rotations for the bind pose.
     */
    private void saveInverseBindPose() {
        resetSkeleton();
        inverseBindPose.clear();

        Skeleton skeleton = getSkeleton();
        int boneCount = skeleton.getBoneCount();
        for (int boneIndex = 0; boneIndex < boneCount; boneIndex++) {
            Bone bone = skeleton.getBone(boneIndex);
            /*
             * Save the bone's local orientation (relative to its parent,
             * or if it's a root bone, relative to the model).
             */
            Quaternion local = bone.getLocalRotation();
            Quaternion inverse = local.inverse();
            inverseBindPose.add(inverse);
        }
    }

    /**
     * Update the attachments node.
     */
    private void updateAttachmentsNode() {
        Node newNode = null;
        if (isBoneSelected()) {
            SkeletonControl control = spatial.getControl(SkeletonControl.class);
            if (control == null) {
                throw new IllegalArgumentException(
                        "expected the spatial to have an SkeletonControl");
            }
            newNode = control.getAttachmentsNode(selectedBoneName);
        }
        if (newNode != attachmentsNode) {
            if (attachmentsNode != null) {
                attachmentsNode.removeControl(AxesControl.class);
            }
            if (newNode != null) {
                AxesControl axesControl = new AxesControl(assetManager, 1f, 1f);
                newNode.addControl(axesControl);
            }
            attachmentsNode = newNode;
        }
    }
}
