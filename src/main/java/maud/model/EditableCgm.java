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
import com.jme3.export.binary.BinaryExporter;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Spatial;
import com.jme3.scene.UserData;
import com.jme3.scene.control.Control;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyAnimation;
import jme3utilities.MySkeleton;
import jme3utilities.MyString;
import jme3utilities.Validate;
import maud.Maud;

/**
 * MVC model for an editable computer-graphics (CG) model in the Maud
 * application: keeps track of edits made to the loaded CG model.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class EditableCgm extends LoadedCgm {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            EditableCgm.class.getName());
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
     * Add a new animation to the CG model.
     *
     * @param newAnimation (not null, name not in use)
     */
    void addAnimation(Animation newAnimation) {
        assert newAnimation != null;
        assert !hasAnimation(newAnimation.getName());

        AnimControl control = getAnimControl();
        if (control == null) {
            Boolean selectedSpatialFlag = false;
            Skeleton skeleton = bones.findSkeleton(selectedSpatialFlag);
            assert skeleton != null;
            control = new AnimControl(skeleton);
            if (selectedSpatialFlag) {
                Spatial modelSpatial = spatial.modelSpatial();
                modelSpatial.addControl(control);
            } else {
                rootSpatial.addControl(control);
            }
        }
        control.addAnim(newAnimation);
        setEdited("add animation");
    }

    /**
     * Add a new SG control to the selected spatial.
     *
     * @param newSgc (not null)
     */
    void addSgc(Control newSgc) {
        assert newSgc != null;

        Spatial selectedSpatial = spatial.underRoot(rootSpatial);
        selectedSpatial.addControl(newSgc);
        setEdited("add control");
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
                object = new Long(0);
                break;
            case "string":
                object = "";
                break;
            default:
                assert false;
        }
        byte objectType = UserData.getObjectType(object);
        UserData data = new UserData(objectType, object);
        Spatial selectedSpatial = spatial.underRoot(rootSpatial);
        selectedSpatial.setUserData(key, data);
        setEdited("add user key");
        Maud.model.misc.selectUserKey(key);
    }

    /**
     * Determine the default base path for writing the CG model to the
     * filesystem.
     *
     * @return absolute filesystem path less extension, or "" if unknown (not
     * null)
     */
    public String baseFilePathForWrite() {
        String result = "";
        String assetPath = getAssetPath();
        if (!assetPath.isEmpty()) {
            String folder = assetFolderForWrite();
            File file = new File(folder, assetPath);
            result = file.getAbsolutePath();
            result = result.replaceAll("\\\\", "/");
        }

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
     * Delete the loaded animation.
     */
    void deleteAnimation() {
        Animation loadedAnimation = animation.getAnimation();
        AnimControl animControl = getAnimControl();
        animControl.removeAnim(loadedAnimation);
        setEdited("delete animation");
    }

    /**
     * Delete the selected control from the selected spatial.
     */
    void deleteControl() {
        Spatial selectedSpatial = spatial.underRoot(rootSpatial);
        Control selectedSgc = sgc.findSgc(rootSpatial);
        boolean success = selectedSpatial.removeControl(selectedSgc);
        assert success;
        setEdited("delete control");
    }

    /**
     * Delete the selected user key from the selected spatial.
     */
    void deleteUserKey() {
        Spatial selectedSpatial = spatial.underRoot(rootSpatial);
        String key = Maud.model.misc.getSelectedUserKey();
        selectedSpatial.setUserData(key, null);
        setEdited("delete user key");
    }

    /**
     * Callback just before a checkpoint is created.
     */
    public void onCheckpoint() {
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

        String oldName = bone.getName();
        boolean success;
        if (!bone.isSelected()) {
            logger.log(Level.WARNING, "Rename failed: no bone selected.",
                    MyString.quote(newName));
            success = false;

        } else if (newName.equals(SelectedSkeleton.noBone)
                || newName.isEmpty()) {
            logger.log(Level.WARNING, "Rename failed: {0} is a reserved name.",
                    MyString.quote(newName));
            success = false;

        } else if (bones.hasBone(newName)) {
            logger.log(Level.WARNING,
                    "Rename failed: a bone named {0} already exists.",
                    MyString.quote(newName));
            success = false;

        } else {
            Bone selectedBone = bone.getBone();
            success = MySkeleton.setName(selectedBone, newName);
        }

        if (success) {
            Maud.model.mapping.renameBone(oldName, newName);
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
            Spatial selectedSpatial = spatial.modelSpatial();
            selectedSpatial.setName(newName);
            success = true;
            setEdited("rename spatial");
        }

        return success;
    }

    /**
     * Rename the selected user key.
     *
     * @param newKey name for the new key (not null)
     */
    public void renameUserKey(String newKey) {
        Validate.nonNull(newKey, "new key");

        Spatial sp = spatial.modelSpatial();
        String oldKey = Maud.model.misc.getSelectedUserKey();
        Object data = sp.getUserData(oldKey);
        sp.setUserData(oldKey, null);
        sp.setUserData(newKey, data);
        setEdited("rename user key");
        Maud.model.misc.selectUserKey(newKey);
    }

    /**
     * Replace the specified animation with a new one.
     *
     * @param oldAnimation (not null)
     * @param newAnimation (not null)
     */
    void replaceAnimation(Animation oldAnimation, Animation newAnimation) {
        assert oldAnimation != null;
        assert newAnimation != null;

        AnimControl animControl = getAnimControl();
        animControl.removeAnim(oldAnimation);
        animControl.addAnim(newAnimation);
        setEdited("replace animation");
    }

    /**
     * Alter the batch hint of the selected spatial.
     *
     * @param newHint new value for batch hint (not null)
     */
    public void setBatchHint(Spatial.BatchHint newHint) {
        Validate.nonNull(newHint, "batch hint");

        Spatial modelSpatial = spatial.underRoot(rootSpatial);
        Spatial.BatchHint oldHint = modelSpatial.getLocalBatchHint();
        if (oldHint != newHint) {
            modelSpatial.setBatchHint(newHint);
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

        Spatial modelSpatial = spatial.underRoot(rootSpatial);
        Spatial.CullHint oldHint = modelSpatial.getLocalCullHint();
        if (oldHint != newHint) {
            modelSpatial.setCullHint(newHint);
            setEdited("change cull hint");
            getSceneView().setCullHint(newHint);
        }
    }

    /**
     * Alter the shadow mode of the selected spatial. TODO sort methods
     *
     * @param newMode new value for shadow mode (not null)
     */
    public void setShadowMode(RenderQueue.ShadowMode newMode) {
        Validate.nonNull(newMode, "shadow mode");

        Spatial modelSpatial = spatial.underRoot(rootSpatial);
        RenderQueue.ShadowMode oldMode = modelSpatial.getLocalShadowMode();
        if (oldMode != newMode) {
            modelSpatial.setShadowMode(newMode);
            setEdited("change shadow mode");
            getSceneView().setMode(newMode);
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

        BoneTrack boneTrack = track.findTrack();
        if (scales == null) {
            boneTrack.setKeyframes(times, translations, rotations);
        } else {
            boneTrack.setKeyframes(times, translations, rotations, scales);
        }
        setEdited("replace keyframes");
    }

    /**
     * Alter the queue bucket of the selected spatial.
     *
     * @param newBucket new value for queue bucket (not null)
     */
    public void setQueueBucket(RenderQueue.Bucket newBucket) {
        Validate.nonNull(newBucket, "queue bucket");

        Spatial modelSpatial = spatial.underRoot(rootSpatial);
        RenderQueue.Bucket oldBucket = modelSpatial.getLocalQueueBucket();
        if (oldBucket != newBucket) {
            modelSpatial.setQueueBucket(newBucket);
            setEdited("change queue bucket");
            getSceneView().setQueueBucket(newBucket);
        }
    }

    /**
     * Alter the local rotation of the selected spatial.
     *
     * @param rotation (not null, unaffected)
     */
    public void setSpatialRotation(Quaternion rotation) {
        Validate.nonNull(rotation, "rotation");

        Spatial selectedSpatial = spatial.underRoot(rootSpatial);
        selectedSpatial.setLocalRotation(rotation);
        getSceneView().setSpatialRotation(rotation);
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

        Spatial selectedSpatial = spatial.underRoot(rootSpatial);
        selectedSpatial.setLocalScale(scale);
        getSceneView().setSpatialScale(scale);
        setEditedSpatialTransform();
    }

    /**
     * Alter the local translation of the selected spatial.
     *
     * @param translation (not null, unaffected)
     */
    public void setSpatialTranslation(Vector3f translation) {
        Validate.nonNull(translation, "translation");

        Spatial selectedSpatial = spatial.underRoot(rootSpatial);
        selectedSpatial.setLocalTranslation(translation);
        getSceneView().setSpatialTranslation(translation);
        setEditedSpatialTransform();
    }

    /**
     * Write the CG model to the specified file.
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
                assetFolder = af;
                baseAssetPath = MyString.remainder(baseFilePath, af);
            } else if (baseFilePath.endsWith(baseAssetPath)
                    && !baseAssetPath.isEmpty()) {
                assetFolder = MyString.removeSuffix(baseFilePath,
                        baseAssetPath);
            } else {
                assetFolder = "";
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
    // protected methods

    /**
     * Invoked after successfully loading a CG model.
     *
     * @param cgmRoot (not null)
     */
    @Override
    protected void postLoad(Spatial cgmRoot) {
        assert cgmRoot != null;

        String eventDescription = "load model " + name;
        setPristine(eventDescription);

        repair(cgmRoot);

        super.postLoad(cgmRoot);
    }
    // *************************************************************************
    // Object methods

    /**
     * Create a deep copy of this object.
     *
     * @return a new object, equivalent to this one
     * @throws CloneNotSupportedException if superclass isn't cloneable
     */
    @Override
    public EditableCgm clone() throws CloneNotSupportedException {
        EditableCgm clone = (EditableCgm) super.clone();
        return clone;
    }
    // *************************************************************************
    // private methods

    /**
     * Determine the default asset folder for writing the CG model to the
     * filesystem.
     *
     * @return absolute filesystem path (not null, not empty)
     */
    private String assetFolderForWrite() {
        String result = assetFolder;
        if (result.isEmpty()) {
            File wa = new File("Written Assets");
            result = wa.getAbsolutePath();
            result = result.replaceAll("\\\\", "/");
        }

        return result;
    }

    /**
     * Repair minor issues with a CG model, such as repetitious keyframes.
     *
     * @param cgmRoot model to correct (not null)
     */
    private void repair(Spatial cgmRoot) {
        boolean madeRepairs = false;

        AnimControl animControl = cgmRoot.getControl(AnimControl.class);
        if (animControl == null) {
            return;
        }

        int numTracksEdited = 0;
        Collection<String> names = animControl.getAnimationNames();
        for (String animationName : names) {
            Animation anim = animControl.getAnim(animationName);
            numTracksEdited += MyAnimation.removeRepeats(anim);
        }
        if (numTracksEdited > 0) {
            String message = "removed repeat keyframe(s) from ";
            if (numTracksEdited == 1) {
                message += "one track";
            } else {
                message += String.format("%d tracks", numTracksEdited);
            }
            logger.warning(message);
            madeRepairs = true;
        }

        if (madeRepairs) {
            setEdited("repair model");
        }
    }

    /**
     * Increment the count of unsaved edits.
     *
     * @param eventDescription description of causative event (not null)
     */
    private void setEdited(String eventDescription) {
        ++editCount;
        editedSpatialTransform = "";
        History.addEvent(eventDescription);
    }

    /**
     * If not a continuation of the previous edit, update the edit count.
     */
    private void setEditedSpatialTransform() {
        String newString = spatial.toString();
        if (!newString.equals(editedSpatialTransform)) {
            ++editCount;
            editedSpatialTransform = newString;
            History.addEvent("transform spatial");
        }
    }

    /**
     * Mark the CG model as pristine.
     *
     * @param eventDescription description of causative event (not null)
     */
    private void setPristine(String eventDescription) {
        editCount = 0;
        editedSpatialTransform = "";
        History.addEvent(eventDescription);
    }
}
