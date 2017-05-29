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
import jme3utilities.ui.ActionApplication;
import maud.History;

/**
 * MVC model for an editable computer-graphics (CG) model in the Maud
 * application: keeps track of edits made to the loaded CG model.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class EditableCgm extends LoadedCGModel {
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
     * count of unsaved edits to the CG model (&ge;0)
     */
    private int editCount = 0;
    /**
     * tree position of the spatial whose transform is being edited, or "" for
     * none
     */
    private String editedSpatialTransform = "";
    // *************************************************************************
    // new methods exposed

    /**
     * Add a new animation to the model.
     *
     * @param newAnimation (not null, name not in use)
     */
    void addAnimation(Animation newAnimation) {
        assert newAnimation != null;
        assert !hasAnimation(newAnimation.getName());

        AnimControl control = getAnimControl();
        if (control == null) {
            Skeleton skeleton = bones.getSkeleton();
            control = new AnimControl(skeleton);
            rootSpatial.addControl(control);
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

        Spatial selectedSpatial = spatial.findSpatial(rootSpatial);
        selectedSpatial.addControl(newSgc);
        setEdited("add control");
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
        Animation loadedAnimation = animation.getLoadedAnimation();
        AnimControl animControl = getAnimControl();
        animControl.removeAnim(loadedAnimation);
        setEdited("delete animation");
    }

    /**
     * Delete the selected control from the selected spatial.
     */
    void deleteControl() {
        Spatial selectedSpatial = spatial.findSpatial(rootSpatial);
        Control selectedSgc = sgc.findSgc(rootSpatial);
        boolean success = selectedSpatial.removeControl(selectedSgc);
        assert success;
        setEdited("delete control");
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

        boolean success;
        if (!bone.isBoneSelected()) {
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
            setEdited("rename bone");
        }

        return success;
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
            setEdited("change cull hint");
            view.setHint(newHint);
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
            setEdited("change shadow mode");
            view.setMode(newMode);
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
        boneTrack.setKeyframes(times, translations, rotations, scales);
        setEdited("replace keyframes");
    }

    /**
     * Alter the local rotation of the selected spatial.
     *
     * @param rotation (not null, unaffected)
     */
    public void setSpatialRotation(Quaternion rotation) {
        Validate.nonNull(rotation, "rotation");

        Spatial selectedSpatial = spatial.findSpatial(rootSpatial);
        selectedSpatial.setLocalRotation(rotation);
        view.setSpatialRotation(rotation);
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

        Spatial selectedSpatial = spatial.findSpatial(rootSpatial);
        selectedSpatial.setLocalScale(scale);
        view.setSpatialScale(scale);
        setEditedSpatialTransform();
    }

    /**
     * Alter the local translation of the selected spatial.
     *
     * @param translation (not null, unaffected)
     */
    public void setSpatialTranslation(Vector3f translation) {
        Validate.nonNull(translation, "translation");

        Spatial selectedSpatial = spatial.findSpatial(rootSpatial);
        selectedSpatial.setLocalTranslation(translation);
        view.setSpatialTranslation(translation);
        setEditedSpatialTransform();
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
            String eventDescription = "write " + baseFilePath;
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
     * @param modelRoot (not null)
     */
    @Override
    protected void postLoad(Spatial modelRoot) {
        assert modelRoot != null;

        String eventDescription = "load " + modelName;
        setPristine(eventDescription);

        repairModel(modelRoot);

        super.postLoad(modelRoot);
    }
    // *************************************************************************
    // Object methods

    /**
     * Create a copy of this object.
     *
     * @return a new object, equivalent to this one
     * @throws CloneNotSupportedException if superclass isn't cloneable
     */
    @Override
    public EditableCgm clone() throws CloneNotSupportedException {
        EditableCgm clone = (EditableCgm) super.clone();
        return clone;
    }

    /**
     * Repair minor issues with a CG model, such as repetitious keyframes.
     *
     * @param modelRoot model to correct (not null)
     */
    private void repairModel(Spatial modelRoot) {
        boolean madeRepairs = false;

        AnimControl animControl = modelRoot.getControl(AnimControl.class);
        if (animControl == null) {
            return;
        }

        int numTracksEdited = 0;
        Collection<String> names = animControl.getAnimationNames();
        for (String animationName : names) {
            Animation animation = animControl.getAnim(animationName);
            numTracksEdited += MyAnimation.removeRepeats(animation);
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
    // *************************************************************************
    // private methods

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
     * Mark the model as pristine.
     *
     * @param eventDescription description of causative event (not null)
     */
    private void setPristine(String eventDescription) {
        editCount = 0;
        editedSpatialTransform = "";
        History.addEvent(eventDescription);
    }
}
