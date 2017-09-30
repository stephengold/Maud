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
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.control.PhysicsControl;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.UserData;
import com.jme3.scene.control.Control;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyControl;
import jme3utilities.MySkeleton;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.ui.ActionApplication;
import jme3utilities.wes.TrackEdit;
import maud.Maud;
import maud.PhysicsUtil;
import maud.view.SceneView;

/**
 * MVC model for an editable computer-graphics (C-G) model in the Maud
 * application: keeps track of edits made to the loaded C-G model.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class EditableCgm extends LoadedCgm {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(EditableCgm.class.getName());
    // *************************************************************************
    // fields

    /**
     * count of unsaved edits (&ge;0)
     */
    private int editCount = 0;
    /**
     * tree position of the spatial whose transform is being edited, or "" for
     * none TODO use null for none
     */
    private String editedSpatialTransform = "";
    // *************************************************************************
    // new methods exposed

    /**
     * Add a new animation to the selected anim control.
     *
     * @param newAnimation (not null, name not in use)
     */
    void addAnimation(Animation newAnimation) {
        assert newAnimation != null;
        SelectedAnimControl sac = getAnimControl();
        String newAnimationName = newAnimation.getName();
        assert !sac.hasRealAnimation(newAnimationName);

        History.autoAdd();
        AnimControl control = sac.find();
        if (control == null) {
            SelectedSkeleton ss = getSkeleton();
            Skeleton skeleton = ss.find();
            assert skeleton != null;
            control = new AnimControl(skeleton);

            Spatial skeletonSpatial = ss.findSpatial();
            skeletonSpatial.addControl(control);
        }
        control.addAnim(newAnimation);
        String description;
        description = "add animation " + MyString.quote(newAnimationName);
        setEdited(description);
    }

    /**
     * Add a new S-G control to the selected spatial.
     *
     * @param newSgc (not null)
     */
    void addSgc(Control newSgc) {
        assert newSgc != null;

        History.autoAdd();
        Spatial selectedSpatial = getSpatial().underRoot(rootSpatial);
        if (newSgc instanceof PhysicsControl) {
            PhysicsControl physicsControl = (PhysicsControl) newSgc;
            SceneView sceneView = getSceneView();
            sceneView.addPhysicsControl(physicsControl);
        }
        selectedSpatial.addControl(newSgc);
        setEdited("add control");
    }

    /**
     * Add a track to the loaded animation.
     *
     * @param newTrack (not null, alias created)
     * @param eventDescription description of causative event (not null)
     */
    void addTrack(Track newTrack, String eventDescription) {
        assert newTrack != null;
        assert eventDescription != null;

        Animation animation = getAnimation().getAnimation();

        History.autoAdd();
        animation.addTrack(newTrack);
        setEdited(eventDescription);
    }

    /**
     * Add a user key to the selected spatial.
     *
     * @param type name of the data type ("boolean", "float", "integer", "long",
     * or "string")
     * @param key user key to create (not null)
     */
    public void addUserKey(String type, String key) {
        Validate.nonNull(key, "key");

        Object object = null;
        switch (type) {
            case "boolean":
                object = false;
                break;
            case "float":
                object = 0f;
                break;
            case "integer":
                object = 0;
                break;
            case "long":
                object = 0L;
                break;
            case "string":
                object = "";
                break;
            default:
                assert false;
        }
        byte objectType = UserData.getObjectType(object);
        UserData data = new UserData(objectType, object);
        Spatial selectedSpatial = getSpatial().underRoot(rootSpatial);

        History.autoAdd();
        selectedSpatial.setUserData(key, data);
        String description;
        description = String.format("add user key %s", MyString.quote(key));
        setEdited(description);
        getUserData().selectKey(key);
    }

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
        String result = file.getAbsolutePath();
        result = result.replaceAll("\\\\", "/");

        return result;
    }

    /**
     * Count unsaved edits.
     *
     * @return count (&ge;0)
     */
    public int countUnsavedEdits() {
        return editCount;
    }

    /**
     * Delete the loaded animation. The invoker is responsible for loading a
     * different animation.
     */
    void deleteAnimation() {
        Animation anim = getAnimation().getAnimation();
        AnimControl animControl = getAnimControl().find();

        History.autoAdd();
        animControl.removeAnim(anim);
        setEdited("delete animation");
    }

    /**
     * Delete all "extra" spatials, but not the root.
     */
    public void deleteExtraSpatials() {
        if (rootSpatial instanceof Node) {
            int oldNumSpatials = MySpatial.countSpatials(rootSpatial,
                    Spatial.class);
            Node rootNode = (Node) rootSpatial;

            History.autoAdd();
            deleteExtraSpatials(rootNode);
            getSpatial().selectCgmRoot();
            int newNumSpatials = MySpatial.countSpatials(rootSpatial,
                    Spatial.class);
            int numDeleted = oldNumSpatials - newNumSpatials;
            String description = String.format("delete %d extra spatial%s",
                    numDeleted, numDeleted == 1 ? "" : "s");
            setEdited(description);
        }
    }

    /**
     * Delete the selected S-G control. The invoker is responsible for
     * deselecting the S-G control.
     */
    void deleteSgc() {
        Spatial selectedSpatial = getSpatial().underRoot(rootSpatial);
        Control selectedSgc = getSgc().find();

        History.autoAdd();
        if (selectedSgc instanceof PhysicsControl) {
            PhysicsControl pc = (PhysicsControl) selectedSgc;
            int position = PhysicsUtil.pcToPosition(selectedSpatial, pc);
            SceneView sceneView = getSceneView();
            sceneView.removePhysicsControl(position);
        }
        boolean success = selectedSpatial.removeControl(selectedSgc);
        assert success;
        setEdited("delete control");
    }

    /**
     * Delete the selected spatial and its children, if any. The invoker is
     * responsible for deselecting the spatial.
     */
    void deleteSubtree() {
        Spatial selectedSpatial = getSpatial().underRoot(rootSpatial);
        Node parent = selectedSpatial.getParent();

        History.autoAdd();
        int position = parent.detachChild(selectedSpatial);
        assert position != -1;
        SceneView sceneView = getSceneView();
        sceneView.deleteSubtree();
        setEdited("delete spatial");
    }

    /**
     * Delete the selected user data from the selected spatial. The invoker is
     * responsible for deselecting the user data.
     */
    void deleteUserData() {
        Spatial selectedSpatial = getSpatial().underRoot(rootSpatial);
        String key = getUserData().getKey();

        History.autoAdd();
        selectedSpatial.setUserData(key, null);
        String description;
        description = String.format("delete user data %s", MyString.quote(key));
        setEdited(description);
    }

    /**
     * Callback before a checkpoint is created.
     */
    void preCheckpoint() {
        /*
         * Potentially a new spatial transform edit.
         */
        editedSpatialTransform = "";
    }

    /**
     * Rename the selected bone.
     *
     * @param newName new name (not null)
     * @return true if successful, otherwise false
     */
    public boolean renameBone(String newName) {
        Validate.nonNull(newName, "bone name");

        String oldName = getBone().getName();
        boolean success;
        if (!getBone().isSelected()) {
            logger.log(Level.WARNING, "Rename failed: no bone selected.",
                    MyString.quote(newName));
            success = false;

        } else if (newName.equals(SelectedSkeleton.noBone)
                || newName.isEmpty()) {
            logger.log(Level.WARNING, "Rename failed: {0} is a reserved name.",
                    MyString.quote(newName));
            success = false;

        } else if (getSkeleton().hasBone(newName)) {
            logger.log(Level.WARNING,
                    "Rename failed: a bone named {0} already exists.",
                    MyString.quote(newName));
            success = false;

        } else {
            Bone selectedBone = getBone().get();
            History.autoAdd();
            success = MySkeleton.setName(selectedBone, newName);
        }

        if (success) {
            Maud.getModel().getMap().renameBone(oldName, newName);
            setEdited("rename bone");
        }

        return success;
    }

    /**
     * Rename the selected spatial.
     *
     * @param newName new name (not null)
     * @return true if successful, otherwise false
     */
    public boolean renameSpatial(String newName) {
        Validate.nonNull(newName, "spatial name");

        boolean success;
        if (newName.isEmpty()) {
            logger.log(Level.WARNING, "Rename failed: {0} is a reserved name.",
                    MyString.quote(newName));
            success = false;

        } else if (hasSpatial(newName)) {
            logger.log(Level.WARNING,
                    "Rename failed: a spatial named {0} already exists.",
                    MyString.quote(newName));
            success = false;

        } else {
            Spatial selectedSpatial = getSpatial().find();

            History.autoAdd();
            selectedSpatial.setName(newName);
            success = true;
            setEdited("rename spatial");
        }

        return success;
    }

    /**
     * Rename the selected user-data key.
     *
     * @param newKey new key name (not null)
     */
    public void renameUserKey(String newKey) {
        Validate.nonNull(newKey, "new key");

        Spatial sp = getSpatial().find();
        String oldKey = getUserData().getKey();
        Object data = sp.getUserData(oldKey);

        History.autoAdd();
        sp.setUserData(oldKey, null);
        sp.setUserData(newKey, data);
        setEdited("rename user-data key");

        getUserData().selectKey(newKey);
    }

    /**
     * Replace the specified animation with a new one.
     *
     * @param oldAnimation (not null)
     * @param newAnimation (not null)
     * @param eventDescription description for the edit history (not null)
     */
    void replaceAnimation(Animation oldAnimation, Animation newAnimation,
            String eventDescription) {
        assert oldAnimation != null;
        assert newAnimation != null;
        assert eventDescription != null;

        AnimControl animControl = getAnimControl().find();

        History.autoAdd();
        animControl.removeAnim(oldAnimation);
        animControl.addAnim(newAnimation);
        LoadedAnimation loaded = getAnimation();
        float duration = loaded.getDuration();
        if (loaded.getTime() > duration) {
            loaded.setTime(duration); // keep track time in range
        }
        setEdited(eventDescription);
    }

    /**
     * Alter whether the selected S-G control applies to its spatial's local
     * translation.
     *
     * @param newSetting true&rarr;apply to local, false&rarr;apply to world
     */
    public void setApplyPhysicsLocal(boolean newSetting) {
        Control modelSgc = getSgc().find();
        if (MyControl.canApplyPhysicsLocal(modelSgc)) {
            boolean oldSetting = MyControl.isApplyPhysicsLocal(modelSgc);
            if (oldSetting != newSetting) {
                History.autoAdd();
                MyControl.setApplyPhysicsLocal(modelSgc, newSetting);

                SceneView sceneView = getSceneView();
                Spatial ss = getSpatial().underRoot(rootSpatial);
                PhysicsControl pc = (PhysicsControl) modelSgc;
                int position = PhysicsUtil.pcToPosition(ss, pc);
                sceneView.setApplyPhysicsLocal(position, newSetting);

                if (newSetting) {
                    setEdited("enable local physics");
                } else {
                    setEdited("disable local physics");
                }
            }
        }
    }

    /**
     * Alter the batch hint of the selected spatial.
     *
     * @param newHint new value for batch hint (not null)
     */
    public void setBatchHint(Spatial.BatchHint newHint) {
        Validate.nonNull(newHint, "batch hint");

        Spatial modelSpatial = getSpatial().underRoot(rootSpatial);
        Spatial.BatchHint oldHint = modelSpatial.getLocalBatchHint();
        if (oldHint != newHint) {
            History.autoAdd();
            modelSpatial.setBatchHint(newHint);
            // scene view not updated
            setEdited("change batch hint");
        }
    }

    /**
     * Alter the cull hint of the selected spatial.
     *
     * @param newHint new value for cull hint (not null)
     */
    public void setCullHint(Spatial.CullHint newHint) {
        Validate.nonNull(newHint, "cull hint");

        Spatial modelSpatial = getSpatial().underRoot(rootSpatial);
        Spatial.CullHint oldHint = modelSpatial.getLocalCullHint();
        if (oldHint != newHint) {
            History.autoAdd();
            modelSpatial.setCullHint(newHint);
            getSceneView().setCullHint(newHint);
            setEdited("change cull hint");
        }
    }

    /**
     * Alter whether the selected geometry ignores its transform.
     *
     * @param newSetting true&rarr;ignore transform, false&rarr;apply transform
     */
    public void setIgnoreTransform(boolean newSetting) {
        Spatial modelSpatial = getSpatial().underRoot(rootSpatial);
        if (modelSpatial instanceof Geometry) {
            Geometry geometry = (Geometry) modelSpatial;
            boolean oldSetting = geometry.isIgnoreTransform();
            if (oldSetting != newSetting) {
                History.autoAdd();
                geometry.setIgnoreTransform(newSetting);
                getSceneView().setIgnoreTransform(newSetting);
                if (newSetting) {
                    setEdited("ignore transform");
                } else {
                    setEdited("stop ignoring transform");
                }
            }
        }
    }

    /**
     * Alter all keyframes in the selected bone track.
     *
     * @param times array of keyframe times (not null, not empty)
     * @param translations array of keyframe translations (not null)
     * @param rotations array of keyframe rotations (not null)
     * @param scales array of keyframe scales (may be null)
     */
    void setKeyframes(float[] times, Vector3f[] translations,
            Quaternion[] rotations, Vector3f[] scales) {
        assert times != null;
        assert times.length > 0 : times.length;
        assert translations != null;
        assert rotations != null;

        BoneTrack boneTrack = getTrack().find();

        History.autoAdd();
        if (scales == null) {
            boneTrack.setKeyframes(times, translations, rotations);
        } else {
            boneTrack.setKeyframes(times, translations, rotations, scales);
        }
        setEdited("replace keyframes");
    }

    /**
     * Alter the mass of the selected rigid body.
     *
     * @param mass (&ge;0)
     */
    public void setMass(float mass) {
        Validate.nonNegative(mass, "mass");

        PhysicsCollisionObject object = getPhysics().find();
        if (object instanceof PhysicsRigidBody) {
            PhysicsRigidBody prb = (PhysicsRigidBody) object;

            History.autoAdd();
            prb.setMass(mass);
            String eventDescription = String.format("set mass to %f", mass);
            setEdited(eventDescription);
        }
    }

    /**
     * Alter the render-queue bucket of the selected spatial.
     *
     * @param newBucket new value for queue bucket (not null)
     */
    public void setQueueBucket(RenderQueue.Bucket newBucket) {
        Validate.nonNull(newBucket, "new bucket");

        Spatial modelSpatial = getSpatial().underRoot(rootSpatial);
        RenderQueue.Bucket oldBucket = modelSpatial.getLocalQueueBucket();
        if (oldBucket != newBucket) {
            History.autoAdd();
            modelSpatial.setQueueBucket(newBucket);
            getSceneView().setQueueBucket(newBucket);
            setEdited("change render-queue bucket");
        }
    }

    /**
     * Alter whether the selected S-G control is enabled.
     *
     * @param newSetting true&rarr;enable, false&rarr;disable
     */
    public void setSgcEnabled(boolean newSetting) {
        Control modelSgc = getSgc().find();
        if (MyControl.canDisable(modelSgc)) {
            boolean oldSetting = MyControl.isEnabled(modelSgc);
            if (oldSetting != newSetting) {
                History.autoAdd();
                MyControl.setEnabled(modelSgc, newSetting);
                if (modelSgc instanceof PhysicsControl) {
                    Spatial ss = getSpatial().underRoot(rootSpatial);
                    PhysicsControl pc = (PhysicsControl) modelSgc;
                    int position = PhysicsUtil.pcToPosition(ss, pc);

                    SceneView sceneView = getSceneView();
                    sceneView.setPhysicsControlEnabled(position, newSetting);
                }
                if (newSetting) {
                    setEdited("enable control");
                } else {
                    setEdited("disable control");
                }
            }
        }
    }

    /**
     * Alter the shadow mode of the selected spatial.
     *
     * @param newMode new value for shadow mode (not null)
     */
    public void setShadowMode(RenderQueue.ShadowMode newMode) {
        Validate.nonNull(newMode, "new mode");

        Spatial modelSpatial = getSpatial().underRoot(rootSpatial);
        RenderQueue.ShadowMode oldMode = modelSpatial.getLocalShadowMode();
        if (oldMode != newMode) {
            History.autoAdd();
            modelSpatial.setShadowMode(newMode);
            getSceneView().setMode(newMode);
            setEdited("change shadow mode");
        }
    }

    /**
     * Alter the local rotation of the selected spatial.
     *
     * @param rotation (not null, unaffected)
     */
    public void setSpatialRotation(Quaternion rotation) {
        Validate.nonNull(rotation, "rotation");

        Spatial selectedSpatial = getSpatial().underRoot(rootSpatial);
        selectedSpatial.setLocalRotation(rotation);
        setEditedSpatialTransform();
    }

    /**
     * Alter the local scale of the selected spatial.
     *
     * @param scale (not null, unaffected)
     */
    public void setSpatialScale(Vector3f scale) {
        Validate.nonNull(scale, "scale");
        Validate.positive(scale.x, "x scale");
        Validate.positive(scale.y, "y scale");
        Validate.positive(scale.z, "z scale");

        Spatial selectedSpatial = getSpatial().underRoot(rootSpatial);
        selectedSpatial.setLocalScale(scale);
        setEditedSpatialTransform();
    }

    /**
     * Alter the local translation of the selected spatial.
     *
     * @param translation (not null, unaffected)
     */
    public void setSpatialTranslation(Vector3f translation) {
        Validate.nonNull(translation, "translation");

        Spatial selectedSpatial = getSpatial().underRoot(rootSpatial);
        selectedSpatial.setLocalTranslation(translation);
        setEditedSpatialTransform();
    }

    /**
     * Alter the selected user data.
     *
     * @param valueString string representation of the new value (not null)
     */
    public void setUserData(String valueString) {
        Validate.nonNull(valueString, "value string");

        String key = getUserData().getKey();
        Spatial sp = getSpatial().find();
        Object data = getSpatial().getUserData(key);

        History.autoAdd();
        if (data instanceof Boolean) {
            boolean valueBoolean = Boolean.parseBoolean(valueString);
            sp.setUserData(key, valueBoolean);

        } else if (data instanceof Float) {
            float valueFloat = Float.parseFloat(valueString);
            sp.setUserData(key, valueFloat);

        } else if (data instanceof Integer) {
            int valueInteger = Integer.parseInt(valueString);
            sp.setUserData(key, valueInteger);

        } else if (data instanceof Long) {
            long valueLong = Long.parseLong(valueString);
            sp.setUserData(key, valueLong);

        } else if (data instanceof String) {
            sp.setUserData(key, valueString);
        }
        setEdited("alter user data");
    }

    /**
     * Write the C-G model to the filesystem at the specified base path.
     *
     * @param baseFilePath file path without any extension (not null, not empty)
     * @return true if successful, otherwise false
     */
    public boolean writeToFile(String baseFilePath) {
        Validate.nonEmpty(baseFilePath, "base file path");

        String filePath = baseFilePath + ".j3o";
        File file = new File(filePath);
        BinaryExporter exporter = BinaryExporter.getInstance();

        boolean success = true;
        try {
            exporter.save(rootSpatial, file);
        } catch (IOException exception) {
            success = false;
        }

        filePath = file.getAbsolutePath();
        filePath = filePath.replaceAll("\\\\", "/");

        if (success) {
            String af = assetFolderForWrite();
            if (baseFilePath.startsWith(af)) {
                assetRootPath = af;
                baseAssetPath = MyString.remainder(baseFilePath, af);
            } else if (baseFilePath.endsWith(baseAssetPath)
                    && !baseAssetPath.isEmpty()) {
                assetRootPath = MyString.removeSuffix(baseFilePath,
                        baseAssetPath);
            } else {
                assetRootPath = "";
                baseAssetPath = "";
            }
            if (baseAssetPath.startsWith("/")) {
                baseAssetPath = MyString.remainder(baseAssetPath, "/");
            }

            extension = "j3o";
            String eventDescription = "write model to " + filePath;
            setPristine(eventDescription);
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
    // LoadedCgm methods

    /**
     * Create a deep copy of this object.
     *
     * @return a new object, equivalent to this one
     * @throws CloneNotSupportedException if the superclass isn't cloneable
     */
    @Override
    public EditableCgm clone() throws CloneNotSupportedException {
        EditableCgm clone = (EditableCgm) super.clone();
        return clone;
    }

    /**
     * Invoked after successfully loading a C-G model.
     *
     * @param cgmRoot (not null)
     */
    @Override
    protected void postLoad(Spatial cgmRoot) {
        assert cgmRoot != null;

        String eventDescription = "load model named " + MyString.quote(name);
        setPristine(eventDescription);

        repair(cgmRoot);

        super.postLoad(cgmRoot);
    }
    // *************************************************************************
    // private methods

    /**
     * Determine the default asset folder for writing the C-G model to the
     * filesystem.
     *
     * @return absolute filesystem path (not null, not empty)
     */
    private String assetFolderForWrite() {
        String result = assetRootPath;
        if (result.isEmpty() || result.endsWith(".jar")
                || result.endsWith(".zip")) {
            result = ActionApplication.getWrittenAssetDirPath();
        }

        return result;
    }

    /**
     * Delete all "extra" spatials among a node's descendents. Note: recursive!
     *
     * @param subtree subtree to traverse (not null)
     */
    private void deleteExtraSpatials(Node subtree) {
        assert subtree != null;

        List<Spatial> childList = subtree.getChildren();
        int numChildren = childList.size();
        Spatial[] children = childList.toArray(new Spatial[numChildren]);
        for (Spatial child : children) {
            int numSgcs = MySpatial.countControls(child, Control.class);
            int numUserData = MySpatial.countUserData(child);
            int numVertices = MySpatial.countVertices(child);
            if (numSgcs == 0 && numUserData == 0 && numVertices == 0) {
                List<Integer> position = findSpatial(child);
                int index = subtree.detachChild(child);
                assert index != -1;
                getSceneView().deleteSubtree(position);
            }
        }

        for (Spatial child : subtree.getChildren()) {
            if (child instanceof Node) {
                deleteExtraSpatials((Node) child);
            }
        }
    }

    /**
     * Repair minor issues with a C-G model, such as repetitious keyframes and
     * tracks without a keyframe at t=0.
     *
     * @param cgmRoot model to correct (not null)
     */
    private void repair(Spatial cgmRoot) {
        int numTracksZfed = 0;
        int numTracksRred = 0;

        List<AnimControl> animControls;
        animControls = MySpatial.listControls(cgmRoot, AnimControl.class, null);
        for (AnimControl animControl : animControls) {
            Collection<String> names = animControl.getAnimationNames();
            for (String animationName : names) {
                Animation anim = animControl.getAnim(animationName);
                numTracksZfed += TrackEdit.zeroFirst(anim);
                numTracksRred += TrackEdit.removeRepeats(anim);
            }
        }

        if (numTracksZfed > 0) {
            String message = "zeroed the time of the 1st keyframe in ";
            if (numTracksZfed == 1) {
                message += "one track";
            } else {
                message += String.format("%d tracks", numTracksZfed);
            }
            setEdited(message);
        }

        if (numTracksRred > 0) {
            String message = "removed repeat keyframe(s) from ";
            if (numTracksRred == 1) {
                message += "one track";
            } else {
                message += String.format("%d tracks", numTracksRred);
            }
            setEdited(message);
        }
    }

    /**
     * Increment the count of unsaved edits.
     *
     * @param eventDescription description of causative event (not null)
     */
    private void setEdited(String eventDescription) {
        assert eventDescription != null;

        ++editCount;
        editedSpatialTransform = "";
        History.addEvent(eventDescription);
    }

    /**
     * If not a continuation of the previous edit, update the edit count.
     */
    private void setEditedSpatialTransform() {
        String newString = getSpatial().toString();
        if (!newString.equals(editedSpatialTransform)) {
            History.autoAdd();
            ++editCount;
            editedSpatialTransform = newString;
            History.addEvent("transform spatial");
        }
    }

    /**
     * Mark the C-G model as pristine.
     *
     * @param eventDescription description of causative event (not null)
     */
    private void setPristine(String eventDescription) {
        editCount = 0;
        editedSpatialTransform = "";
        History.addEvent(eventDescription);
    }
}
