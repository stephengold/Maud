/*
 Copyright (c) 2017-2020, Stephen Gold
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

import com.jme3.animation.AnimControl;
import com.jme3.animation.Animation;
import com.jme3.animation.Bone;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.animation.SpatialTrack;
import com.jme3.animation.Track;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.animation.AttachmentLink;
import com.jme3.bullet.animation.BoneLink;
import com.jme3.bullet.animation.DynamicAnimControl;
import com.jme3.bullet.animation.PhysicsLink;
import com.jme3.bullet.animation.RangeOfMotion;
import com.jme3.bullet.control.PhysicsControl;
import com.jme3.export.JmeExporter;
import com.jme3.light.Light;
import com.jme3.material.MatParam;
import com.jme3.material.MatParamOverride;
import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.material.RenderState;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.UserData;
import com.jme3.scene.control.Control;
import com.jme3.shader.VarType;
import com.jme3.texture.Texture;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Heart;
import jme3utilities.MyControl;
import jme3utilities.MySkeleton;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.NameGenerator;
import jme3utilities.Validate;
import jme3utilities.minie.MyControlP;
import jme3utilities.wes.AnimationEdit;
import maud.Maud;
import maud.MaudUtil;
import maud.ParseUtil;
import maud.PhysicsUtil;
import maud.model.EditState;
import maud.model.History;
import maud.view.scene.SceneView;

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
     * number of coordinate axes
     */
    final private static int numAxes = 3;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(EditableCgm.class.getName());
    // *************************************************************************
    // fields

    /**
     * count of unsaved edits plus the continuous-edit state
     */
    private EditState editState = new EditState();
    // *************************************************************************
    // new methods exposed

    /**
     * Add a new animation to the selected anim control. TODO add
     * eventDescription argument
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
        String description
                = "add animation " + MyString.quote(newAnimationName);
        editState.setEdited(description);
    }

    /**
     * Add an attachments node for the selected bone.
     */
    public void addAttachmentsNode() {
        SelectedBone selectedBone = getBone();
        assert !selectedBone.hasAttachmentsNode();

        History.autoAdd();
        Node newNode = selectedBone.createAttachments();

        Node parent = newNode.getParent();
        List<Integer> parentPosition = findSpatial(parent);
        getSceneView().attachSpatial(parentPosition, newNode);

        String boneName = selectedBone.name();
        String description
                = "add attachments node for " + MyString.quote(boneName);
        editState.setEdited(description);

        assert selectedBone.hasAttachmentsNode();
    }

    /**
     * Add a newly-created light to the selected spatial.
     *
     * @param newLight the light to add (not null, alias created)
     * @param eventDescription a textual description of the event for the edit
     * history (not null, not empty)
     */
    void addLight(Light newLight, String eventDescription) {
        assert newLight != null;
        assert eventDescription != null;
        assert !eventDescription.isEmpty();

        History.autoAdd();
        Spatial selectedSpatial = getSpatial().find();
        selectedSpatial.addLight(newLight);
        getSceneView().addLight(newLight);
        editState.setEdited(eventDescription);
    }

    /**
     * Add a new parameter to the selected material and select that parameter.
     *
     * @param parameterName a name for the parameter (not null, not empty)
     */
    public void addMatParam(String parameterName) {
        Validate.nonEmpty(parameterName, "parameter name");

        Material material = getSpatial().getMaterial();
        MaterialDef def = material.getMaterialDef();
        MatParam matDefParam = def.getMaterialParam(parameterName);
        VarType varType = matDefParam.getVarType();
        Object defaultValue = matDefParam.getValue();
        Spatial matSpatial = getSpatial().find();
        List<Integer> treePosition = findSpatial(matSpatial);

        Object value;
        if (defaultValue == null) {
            value = MaudUtil.defaultValue(varType, parameterName);
        } else {
            value = Heart.deepCopy(defaultValue);
        }

        History.autoAdd();
        material.setKey(null);
        material.setParam(parameterName, varType, value);
        getSceneView().setParamValue(treePosition, parameterName, varType,
                value);

        String description = String.format("add material parameter %s",
                MyString.quote(parameterName));
        editState.setEdited(description);

        getMatParam().select(parameterName);
    }

    /**
     * Add a new, null-valued material-parameter override to the selected
     * spatial.
     *
     * @param varType the variable type (not null)
     * @param parameterName a name for the parameter (not null)
     */
    public void addOverride(VarType varType, String parameterName) {
        Validate.nonNull(varType, "variable type");
        Validate.nonNull(parameterName, "parameter name");

        Spatial selectedSpatial = getSpatial().find();
        MatParamOverride newMpo
                = new MatParamOverride(varType, parameterName, null);

        History.autoAdd();
        selectedSpatial.addMatParamOverride(newMpo);
        getSceneView().addOverride(varType, parameterName);

        String description = String.format("add material-parameter override %s",
                MyString.quote(parameterName));
        editState.setEdited(description);

        getOverride().select(parameterName);
    }

    /**
     * Add a newly-created S-G control to the selected spatial.
     *
     * @param newSgc the SGC in the MVC model (not null, alias created)
     * @param eventDescription a textual description of the event for the edit
     * history (not null, not empty)
     */
    void addSgc(Control newSgc, String eventDescription) {
        assert newSgc != null;

        History.autoAdd();
        Spatial controlledSpatial = getSpatial().find();
        controlledSpatial.addControl(newSgc);
        if (newSgc instanceof PhysicsControl) {
            /*
             * Notify CgmPhysics about the addition.
             */
            PhysicsControl physicsControl = (PhysicsControl) newSgc;
            getPhysics().addPhysicsControl(physicsControl);
        }
        assert MyControl.findIndex(newSgc, controlledSpatial)
                != SelectedSgc.noSgcIndex;
        assert getSpatial().find() == controlledSpatial;

        editState.setEdited(eventDescription);
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

        Animation animation = getAnimation().getReal();

        History.autoAdd();
        animation.addTrack(newTrack);
        editState.setEdited(eventDescription);
    }

    /**
     * Add a new user key to the selected spatial.
     *
     * @param dataType the data type (not null)
     * @param key user key to create (not null)
     */
    public void addUserKey(UserDataType dataType, String key) {
        Validate.nonNull(dataType, "data type");
        Validate.nonNull(key, "key");

        Object object = dataType.create();
        byte objectType = UserData.getObjectType(object);
        UserData data = new UserData(objectType, object);
        Spatial selectedSpatial = getSpatial().find();

        History.autoAdd();
        selectedSpatial.setUserData(key, data);

        String description
                = String.format("add user key %s", MyString.quote(key));
        editState.setEdited(description);
        getUserData().selectKey(key);
    }

    /**
     * Add an AttachmentLink for the named bone and specified model to the
     * selected ragdoll.
     *
     * @param boneName (not null, not empty)
     * @param child (not null, unaffected)
     */
    void attachBone(String boneName, Spatial child) {
        Validate.nonEmpty(boneName, "bone name");

        SelectedRagdoll ragdoll = getRagdoll();
        DynamicAnimControl dac = ragdoll.find();

        History.autoAdd();
        Spatial saveSpatial = ragdoll.setSpatial(null);
        dac.attach(boneName, 1f, child);
        ragdoll.setSpatial(saveSpatial);
        getSceneView().attachBone(boneName, child);
        String description = "attach model to bone " + MyString.quote(boneName);
        editState.setEdited(description);
    }

    /**
     * Attach the specified child subtree to the specified parent node.
     *
     * @param parent (not null)
     * @param child (not null)
     * @param eventDescription description of causative event (not null)
     */
    void attachSpatial(Node parent, Spatial child, String eventDescription) {
        assert parent != null;
        assert child != null;
        assert eventDescription != null;

        SceneView sceneView = getSceneView();
        List<Integer> parentPosition = findSpatial(parent);

        History.autoAdd();
        sceneView.attachSpatial(parentPosition, child);
        parent.attachChild(child);
        editState.setEdited(eventDescription);
    }

    /**
     * Delete the loaded animation. The invoker is responsible for loading a
     * different animation.
     */
    void deleteAnimation() {
        Animation anim = getAnimation().getReal();
        AnimControl animControl = getAnimControl().find();

        History.autoAdd();
        animControl.removeAnim(anim);
        // scene view not updated
        editState.setEdited("delete animation");
    }

    /**
     * Delete the attachments node for the selected bone.
     */
    public void deleteAttachmentsNode() {
        SelectedBone selectedBone = getBone();
        assert selectedBone.hasAttachmentsNode();

        History.autoAdd();
        Bone bone = selectedBone.get();
        Node node = MySkeleton.getAttachments(bone);

        // check for deletion of the selected spatial
        SelectedSpatial selectedSpatial = getSpatial();
        Spatial spatial = selectedSpatial.find();
        if (spatial == node || spatial.hasAncestor(node)) {
            selectedSpatial.selectCgmRoot();
        }

        List<Integer> nodePosition = findSpatial(node);

        MySkeleton.cancelAttachments(bone);
        boolean success = node.removeFromParent();
        assert success;
        getSceneView().deleteSubtree(nodePosition);

        String boneName = selectedBone.name();
        String description = "delete attachments node for "
                + MyString.quote(boneName);
        editState.setEdited(description);

        assert !selectedBone.hasAttachmentsNode();
    }

    /**
     * Delete all "extra" spatials in the model, but not the root.
     */
    public void deleteExtraSpatials() {
        if (rootSpatial instanceof Node) {
            History.autoAdd();
            int oldNumSpatials = MySpatial.countSpatials(rootSpatial,
                    Spatial.class);

            Node rootNode = (Node) rootSpatial;
            Map<Bone, Spatial> map = mapAttachments();
            deleteExtraSpatials(rootNode, map.values());

            getSpatial().selectCgmRoot();
            int newNumSpatials
                    = MySpatial.countSpatials(rootSpatial, Spatial.class);
            int numDeleted = oldNumSpatials - newNumSpatials;
            String description = String.format("delete %d extra spatial%s",
                    numDeleted, numDeleted == 1 ? "" : "s");
            editState.setEdited(description);
        }
    }

    /**
     * Delete the selected material parameter. The invoker is responsible for
     * deselecting the parameter and its texture if any.
     */
    void deleteMatParam() {
        Material material = getSpatial().getMaterial();
        String parameterName = getMatParam().getName();
        SceneView sceneView = getSceneView();

        History.autoAdd();
        material.setKey(null);
        material.clearParam(parameterName);
        sceneView.deleteMatParam();

        String description = String.format(
                "delete material parameter %s", MyString.quote(parameterName));
        editState.setEdited(description);
    }

    /**
     * Delete the selected S-G control. The invoker is responsible for
     * deselecting the control.
     */
    void deleteSgc() {
        Spatial controlled = getSgc().getControlled();
        Control selectedSgc = getSgc().get();
        SceneView sceneView = getSceneView();

        History.autoAdd();
        if (selectedSgc instanceof SkeletonControl) {
            SkeletonControl skeletonControl = (SkeletonControl) selectedSgc;
            Skeleton skeleton = skeletonControl.getSkeleton();
            Map<Bone, Spatial> map = MySkeleton.mapAttachments(skeleton, null);
            for (Bone bone : map.keySet()) {
                Node attachmentsNode = MySkeleton.getAttachments(bone);
                List<Integer> nodePosition = findSpatial(attachmentsNode);

                MySkeleton.cancelAttachments(bone);
                /*
                 * Detach the attachments node from its parent.
                 */
                boolean success = attachmentsNode.removeFromParent();
                assert success;
                /*
                 * Sychronize with the scene view.
                 */
                sceneView.deleteSubtree(nodePosition);
            }

        } else if (selectedSgc instanceof PhysicsControl) {
            /*
             * Notify CgmPhysics about the removal.
             */
            PhysicsControl pc = (PhysicsControl) selectedSgc;
            getPhysics().removePhysicsControl(pc);
            /*
             * Sychronize with the scene view.
             */
            List<Integer> treePosition = findSpatial(controlled);
            int pcPosition = PhysicsUtil.pcToPosition(controlled, pc);
            sceneView.removePhysicsControl(treePosition, pcPosition);
        }

        boolean success = controlled.removeControl(selectedSgc);
        assert success;
        editState.setEdited("delete control");
    }

    /**
     * Delete the selected spatial and its descendents, if any. The invoker is
     * responsible for updating selections.
     */
    void deleteSubtree() {
        SelectedSpatial ss = getSpatial();
        assert !ss.isCgmRoot();

        History.autoAdd();
        Spatial subtree = ss.find();
        /*
         * Cancel all attachments nodes in the subtree.
         */
        if (subtree instanceof Node) {
            Node subtreeNode = (Node) subtree;
            Map<Bone, Spatial> map = mapAttachments();
            for (Entry<Bone, Spatial> mapEntry : map.entrySet()) {
                Spatial spatial = mapEntry.getValue();
                if (spatial == subtree || spatial.hasAncestor(subtreeNode)) {
                    Bone bone = mapEntry.getKey();
                    MySkeleton.cancelAttachments(bone);
                }
            }
        }
        /*
         * Detach the subtree from its parent.
         */
        boolean success = subtree.removeFromParent();
        assert success;
        /*
         * Sychronize the scene view.
         */
        SceneView sceneView = getSceneView();
        sceneView.deleteSubtree();

        editState.setEdited("delete subtree");
    }

    /**
     * Access the edit state for this C-G model.
     *
     * @return the pre-existing instance (not null)
     */
    public EditState getEditState() {
        assert editState != null;
        return editState;
    }

    /**
     * Insert a new node into the scene graph to parent the selected spatial.
     *
     * @param newNodeName a name for the new node (not null, not empty)
     */
    public void insertParent(String newNodeName) {
        Validate.nonEmpty(newNodeName, "new node name");

        SceneView sceneView = getSceneView();
        Spatial selectedSpatial = getSpatial().find();
        Node oldParent = selectedSpatial.getParent();
        Node newNode = new Node(newNodeName);

        History.autoAdd();
        sceneView.insertParent(newNodeName);

        Skeleton oldSkeleton = getSkeleton().find();
        if (oldParent != null) {
            int position = oldParent.detachChild(selectedSpatial);
            assert position != -1;
            oldParent.attachChild(newNode);
        } else {
            rootSpatial = newNode;
        }
        newNode.attachChild(selectedSpatial);
        /*
         * Make sure the selected spatial doesn't change.
         */
        getSpatial().select(selectedSpatial);
        /*
         * Check whether the selected skeleton has changed.
         */
        boolean[] selectedSgc = {false};
        Skeleton newSkeleton = getSkeleton().find(selectedSgc);
        if (newSkeleton != oldSkeleton) {
            getBone().deselect();
            getPose().resetToBind(newSkeleton);
            getSceneView().setSkeleton(newSkeleton, selectedSgc[0]);
        }

        String eventDescription = String.format("insert parent %s",
                MyString.quote(newNodeName));
        editState.setEdited(eventDescription);
    }

    /**
     * Add a BoneLink for the named bone to the selected ragdoll.
     *
     * @param boneName (not null, not empty)
     * @return true if successful, otherwise false
     */
    public boolean linkBone(String boneName) {
        Validate.nonEmpty(boneName, "bone name");

        SelectedRagdoll ragdoll = getRagdoll();
        DynamicAnimControl dac = ragdoll.find();

        History.autoAdd();
        Spatial saveSpatial = ragdoll.setSpatial(null);
        dac.link(boneName, 1f, new RangeOfMotion());
        try {
            ragdoll.setSpatial(saveSpatial);
        } catch (IllegalArgumentException e) {
            ragdoll.setSpatial(null);
            dac.unlinkBone(boneName);
            ragdoll.setSpatial(saveSpatial);
            String description = "failed link bone " + MyString.quote(boneName);
            editState.setEdited(description);
            return false;
        }
        String description = "link bone " + MyString.quote(boneName);
        editState.setEdited(description);
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

        String oldName = getBone().name();
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
            editState.setEdited("rename bone");
        }

        return success;
    }

    /**
     * Rename the selected material.
     *
     * @param newName new material name (may be null or duplicate)
     */
    public void renameMaterial(String newName) {
        Material material = getSpatial().getMaterial();
        String oldName = material.getName();
        if (oldName != newName && !oldName.equals(newName)) {
            History.autoAdd();
            material.setKey(null);
            material.setName(newName);
            // scene view not updated

            String description = String.format("rename material %s to %s",
                    MyString.quote(oldName), MyString.quote(newName));
            editState.setEdited(description);
        }
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
            editState.setEdited("rename spatial");
        }

        return success;
    }

    /**
     * Replace the specified animation with a new one.
     *
     * @param oldAnimation animation to replace (not null)
     * @param newAnimation replacement animation (not null)
     * @param eventDescription description for the edit history (not null)
     * @param newSelectedTrack replacement selected track (may be null)
     */
    void replace(Animation oldAnimation, Animation newAnimation,
            String eventDescription, Track newSelectedTrack) {
        assert oldAnimation != null;
        assert newAnimation != null;
        assert eventDescription != null;

        AnimControl animControl = getAnimControl().find();

        History.autoAdd();
        animControl.removeAnim(oldAnimation);
        animControl.addAnim(newAnimation);
        float duration = getAnimation().duration();
        if (getPlay().getTime() > duration) {
            getPlay().setTime(duration); // keep animation time in range
        }
        editState.setEdited(eventDescription);
        getTrack().select(newSelectedTrack);
    }

    /**
     * Remove the selected light, and optionally replace it with the specified
     * light. The invoker is responsible for updating the selection.
     *
     * @param newLight the light to add (alias created) if null, the existing
     * light is simply removed
     * @param eventDescription a textual description of the event for the edit
     * history (not null, not empty)
     */
    void replaceLight(Light newLight, String eventDescription) {
        assert eventDescription != null;
        assert !eventDescription.isEmpty();

        SelectedLight selectedLight = getLight();
        Spatial owner = selectedLight.getOwner();
        Light oldLight = selectedLight.get();
        String oldName = oldLight.getName();

        History.autoAdd();
        owner.removeLight(oldLight);
        if (newLight != null) {
            owner.addLight(newLight);
        }
        getSceneView().replaceLight(oldName, newLight);
        editState.setEdited(eventDescription);
    }

    /**
     * Remove the selected light and replace it the specified light, which
     * differs only in color.
     *
     * @param newLight the light to add (not null, alias created)
     */
    void replaceLightColor(Light newLight) {
        assert newLight != null;

        SelectedLight selectedLight = getLight();
        Spatial owner = selectedLight.getOwner();
        Light oldLight = selectedLight.get();
        String oldName = oldLight.getName();

        owner.removeLight(oldLight);
        owner.addLight(newLight);
        getSceneView().replaceLight(oldName, newLight);
        selectedLight.select(newLight);
        editState.setEditedLightColor(oldName);
    }

    /**
     * Remove the selected light and replace it the specified light, which
     * differs only in position and/or direction.
     *
     * @param newLight the light to add (not null, alias created)
     */
    void replaceLightPosDir(Light newLight) {
        assert newLight != null;

        SelectedLight selectedLight = getLight();
        Spatial owner = selectedLight.getOwner();
        Light oldLight = selectedLight.get();
        String oldName = oldLight.getName();

        owner.removeLight(oldLight);
        owner.addLight(newLight);
        getSceneView().replaceLight(oldName, newLight);
        selectedLight.select(newLight);
        editState.setEditedLightPosDir(oldName);
    }

    /**
     * Replace the selected texture references with the specified texture.
     *
     * @param newTexture the replacement texture (not null, aliases created)
     * @param eventDescription for the edit history (not null)
     */
    void replaceSelectedTexture(Texture newTexture, String eventDescription) {
        assert newTexture != null;
        assert eventDescription != null;

        History.autoAdd();
        getTexture().replaceTexture(newTexture);
        editState.setEdited(eventDescription);
    }

    /**
     * Select the specified texture reference and replace it with the specified
     * texture.
     *
     * @param ref the texture reference to select/replace (not null)
     * @param newTexture the replacement texture (may be null, aliases created)
     * @param eventDescription for the edit history (not null)
     */
    void selectAndReplaceTexture(MatParamRef ref, Texture newTexture,
            String eventDescription) {
        assert ref != null;
        assert eventDescription != null;

        SelectedTexture texture = getTexture();
        History.autoAdd();
        texture.select(ref);
        texture.replaceTexture(newTexture);
        editState.setEdited(eventDescription);
    }

    /**
     * Alter whether the selected S-G control applies to its spatial's local
     * translation.
     *
     * @param newSetting true&rarr;apply to local, false&rarr;apply to world
     */
    public void setApplyPhysicsLocal(boolean newSetting) {
        Control modelSgc = getSgc().get();
        if (MyControlP.canApplyPhysicsLocal(modelSgc)) {
            boolean oldSetting = MyControlP.isApplyPhysicsLocal(modelSgc);
            if (oldSetting != newSetting) {
                History.autoAdd();
                MyControlP.setApplyPhysicsLocal(modelSgc, newSetting);

                Spatial controlled = getSgc().getControlled();
                List<Integer> treePosition = findSpatial(controlled);
                PhysicsControl pc = (PhysicsControl) modelSgc;
                int pcPosition = PhysicsUtil.pcToPosition(controlled, pc);
                SceneView sceneView = getSceneView();
                sceneView.setApplyPhysicsLocal(treePosition, pcPosition,
                        newSetting);

                if (newSetting) {
                    editState.setEdited("enable local physics");
                } else {
                    editState.setEdited("disable local physics");
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

        Spatial modelSpatial = getSpatial().find();
        Spatial.BatchHint oldHint = modelSpatial.getLocalBatchHint();
        if (oldHint != newHint) {
            History.autoAdd();
            modelSpatial.setBatchHint(newHint);
            // scene view not updated
            String description = String.format(
                    "set batch hint of spatial to %s", newHint);
            editState.setEdited(description);
        }
    }

    /**
     * Alter the cull hint of the selected spatial.
     *
     * @param newHint new value for cull hint (not null)
     */
    public void setCullHint(Spatial.CullHint newHint) {
        Validate.nonNull(newHint, "cull hint");

        Spatial modelSpatial = getSpatial().find();
        Spatial.CullHint oldHint = modelSpatial.getLocalCullHint();
        if (oldHint != newHint) {
            History.autoAdd();
            modelSpatial.setCullHint(newHint);
            getSceneView().setCullHint(newHint);
            String description = String.format(
                    "set cull hint of spatial to %s", newHint);
            editState.setEdited(description);
        }
    }

    /**
     * Alter the depth-test setting of the selected material.
     *
     * @param newState true &rarr; enable test, false &rarr; disable it
     */
    public void setDepthTest(boolean newState) {
        Material material = getSpatial().getMaterial();
        if (material != null) {
            RenderState modelState = material.getAdditionalRenderState();
            if (modelState.isDepthTest() != newState) {
                History.autoAdd();
                material.setKey(null);
                modelState.setDepthTest(newState);
                SceneView sceneView = getSceneView();
                sceneView.setDepthTest(newState);

                String description = String.format(
                        "set depth test flag of material to %s", newState);
                editState.setEdited(description);
            }
        }
    }

    /**
     * Alter the face-cull mode of the selected material.
     *
     * @param newMode desired mode (not null)
     */
    public void setFaceCullMode(RenderState.FaceCullMode newMode) {
        Validate.nonNull(newMode, "new mode");

        Material material = getSpatial().getMaterial();
        if (material != null) {
            RenderState modelState = material.getAdditionalRenderState();
            if (modelState.getFaceCullMode() != newMode) {
                History.autoAdd();
                material.setKey(null);
                modelState.setFaceCullMode(newMode);
                SceneView sceneView = getSceneView();
                sceneView.setFaceCullMode(newMode);

                String description = String.format(
                        "set face-cull mode of material to %s", newMode);
                editState.setEdited(description);
            }
        }
    }

    /**
     * Alter whether the selected geometry ignores its transform.
     *
     * @param newSetting true&rarr;ignore transform, false&rarr;apply transform
     */
    public void setIgnoreTransform(boolean newSetting) {
        Spatial modelSpatial = getSpatial().find();
        if (modelSpatial instanceof Geometry) {
            Geometry geometry = (Geometry) modelSpatial;
            boolean oldSetting = geometry.isIgnoreTransform();
            if (oldSetting != newSetting) {
                History.autoAdd();
                geometry.setIgnoreTransform(newSetting);
                getSceneView().setIgnoreTransform(newSetting);
                if (newSetting) {
                    editState.setEdited("ignore transform");
                } else {
                    editState.setEdited("stop ignoring transform");
                }
            }
        }
    }

    /**
     * Alter all keyframes in the selected track. TODO description arg
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

        Track track = getTrack().get();

        History.autoAdd();
        if (track instanceof BoneTrack) {
            BoneTrack boneTrack = (BoneTrack) track;
            boneTrack.setKeyframes(times, translations, rotations, scales);
        } else {
            SpatialTrack spatialTrack = (SpatialTrack) track;
            spatialTrack.setKeyframes(times, translations, rotations, scales);
        }
        editState.setEdited("replace keyframes");
    }

    /**
     * Alter the range of motion of one axis of the selected PhysicsLink.
     *
     * @param axisIndex which axis to alter (0&rarr;X, 1&rarr;Y, 2&rarr;Z)
     * @param maxAngle the desired maximum rotation angle (in radians)
     * @param minAngle the desired minimum rotation angle (in radians)
     */
    public void setLinkAxisLimits(int axisIndex, float maxAngle,
            float minAngle) {
        Validate.inRange(axisIndex, "axis index", PhysicsSpace.AXIS_X,
                PhysicsSpace.AXIS_Z);
        Validate.inRange(maxAngle, "maximum angle", minAngle, FastMath.PI);
        Validate.inRange(minAngle, "minimum angle", -FastMath.PI, maxAngle);

        SelectedLink selectedLink = getLink();

        RangeOfMotion oldRom = selectedLink.getRangeOfMotion();
        float max[] = new float[numAxes];
        float min[] = new float[numAxes];
        for (int axis = 0; axis < numAxes; ++axis) {
            if (axis == axisIndex) {
                max[axis] = maxAngle;
                min[axis] = minAngle;
            } else {
                max[axis] = oldRom.getMaxRotation(axis);
                min[axis] = oldRom.getMinRotation(axis);
            }
        }
        RangeOfMotion newRom = new RangeOfMotion(
                max[PhysicsSpace.AXIS_X], min[PhysicsSpace.AXIS_X],
                max[PhysicsSpace.AXIS_Y], min[PhysicsSpace.AXIS_Y],
                max[PhysicsSpace.AXIS_Z], min[PhysicsSpace.AXIS_Z]);

        DynamicAnimControl dac = getRagdoll().find();
        String boneName = selectedLink.boneName();
        dac.setJointLimits(boneName, newRom);
        getSceneView().setRangeOfMotion(boneName, newRom);

        String linkName = selectedLink.name();
        editState.setEditedRangeOfMotion(linkName);
    }

    /**
     * Alter the mass of the selected physics link.
     *
     * @param mass the desired mass (&gt;0)
     */
    public void setLinkMass(float mass) {
        Validate.positive(mass, "mass");

        PhysicsLink link = getLink().find();
        String linkName = link.name();
        DynamicAnimControl dac = getRagdoll().find();
        String description = String.format("set %s mass to %f",
                MyString.quote(linkName), mass);

        History.autoAdd();
        dac.setMass(link, mass);
        getSceneView().setLinkMass(linkName, mass);
        editState.setEdited(description);
    }

    /**
     * Apply the specified material to the selected spatial.
     *
     * @param newMaterial replacement material (not null)
     * @param eventDescription description for the edit history (not null)
     */
    void setMaterial(Material newMaterial, String eventDescription) {
        assert newMaterial != null;
        assert eventDescription != null;

        Spatial spatial = getSpatial().find();

        History.autoAdd();
        spatial.setMaterial(newMaterial);
        getSceneView().setMaterial(newMaterial);
        getMatParam().postSetMaterial(newMaterial);
        getTexture().postEdit();
        editState.setEdited(eventDescription);
    }

    /**
     * Alter the value of the selected material parameter.
     *
     * @param valueString string representation of the new value (not null)
     */
    public void setMatParamValue(String valueString) {
        Validate.nonNull(valueString, "value string");

        MatParam oldParam = getMatParam().find();
        VarType varType = oldParam.getVarType();
        String parameterName = oldParam.getName();
        Object modelValue = ParseUtil.parseMatParam(oldParam, valueString);
        Object viewValue = ParseUtil.parseMatParam(oldParam, valueString);

        Spatial matSpatial = getSpatial().find();
        List<Integer> treePosition = findSpatial(matSpatial);
        Material material = getSpatial().getMaterial();

        History.autoAdd();
        material.setKey(null);
        material.setParam(parameterName, varType, modelValue);
        getSceneView().setParamValue(treePosition, parameterName, varType,
                viewValue);

        String description = String.format(
                "alter value of material parameter %s",
                MyString.quote(parameterName));
        editState.setEdited(description);
    }

    /**
     * Alter the mode of the selected mesh.
     *
     * @param newMode new value for mode (not null, not Hybrid)
     */
    public void setMeshMode(Mesh.Mode newMode) {
        Validate.nonNull(newMode, "new mode");
        assert newMode != Mesh.Mode.Hybrid;

        Mesh mesh = getSpatial().getMesh();
        if (mesh.getMode() != newMode) {
            History.autoAdd();
            mesh.setMode(newMode);
            SceneView sceneView = getSceneView();
            sceneView.setMeshMode(newMode);

            String description = String.format("set mode of mesh to %s",
                    newMode);
            editState.setEdited(description);
        }
    }

    /**
     * Alter the maximum number of weights per vertex in the selected mesh.
     *
     * @param newLimit new number (&ge;1, &le;4)
     */
    public void setMeshWeights(int newLimit) {
        Validate.inRange(newLimit, "new limit", 1, 4);

        Mesh mesh = getSpatial().getMesh();
        int oldLimit = mesh.getMaxNumWeights();
        if (oldLimit != newLimit) {
            History.autoAdd();
            mesh.setMaxNumWeights(newLimit);
            getSceneView().setMeshWeights(newLimit);
            String description = String.format(
                    "set max weights of mesh to %d", newLimit);
            editState.setEdited(description);
        }
    }

    /**
     * Alter the render-queue bucket of the selected spatial.
     *
     * @param newBucket new value for queue bucket (not null)
     */
    public void setQueueBucket(RenderQueue.Bucket newBucket) {
        Validate.nonNull(newBucket, "new bucket");

        Spatial modelSpatial = getSpatial().find();
        RenderQueue.Bucket oldBucket = modelSpatial.getLocalQueueBucket();
        if (oldBucket != newBucket) {
            History.autoAdd();
            modelSpatial.setQueueBucket(newBucket);
            getSceneView().setQueueBucket(newBucket);
            editState.setEdited("change render-queue bucket"); // TODO details
        }
    }

    /**
     * Alter whether the selected S-G control is enabled.
     *
     * @param newSetting true&rarr;enable, false&rarr;disable
     */
    public void setSgcEnabled(boolean newSetting) {
        Control modelSgc = getSgc().get();
        if (MyControlP.canDisable(modelSgc)) {
            boolean oldSetting = MyControlP.isEnabled(modelSgc);
            if (oldSetting != newSetting) {
                History.autoAdd();
                MyControlP.setEnabled(modelSgc, newSetting);
                if (modelSgc instanceof PhysicsControl) {
                    Spatial controlled = getSgc().getControlled();
                    List<Integer> treePosition = findSpatial(controlled);
                    PhysicsControl pc = (PhysicsControl) modelSgc;
                    int pcPosition = PhysicsUtil.pcToPosition(controlled, pc);

                    SceneView sceneView = getSceneView();
                    sceneView.setPhysicsControlEnabled(treePosition, pcPosition,
                            newSetting);
                }
                if (newSetting) {
                    editState.setEdited("enable control");
                } else {
                    editState.setEdited("disable control");
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

        Spatial modelSpatial = getSpatial().find();
        RenderQueue.ShadowMode oldMode = modelSpatial.getLocalShadowMode();
        if (oldMode != newMode) {
            History.autoAdd();
            modelSpatial.setShadowMode(newMode);
            getSceneView().setShadowMode(newMode);
            String description = String.format(
                    "change spatial's shadow mode to %s", newMode);
            editState.setEdited(description);
        }
    }

    /**
     * Alter the local rotation of the selected spatial.
     *
     * @param rotation (not null, unaffected)
     */
    public void setSpatialRotation(Quaternion rotation) {
        Validate.nonNull(rotation, "rotation");

        Spatial selectedSpatial = getSpatial().find();
        selectedSpatial.setLocalRotation(rotation);
        String spatialPosition = selectedSpatial.toString();
        editState.setEditedSpatialTransform(spatialPosition);
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

        Spatial selectedSpatial = getSpatial().find();
        selectedSpatial.setLocalScale(scale);
        String position = selectedSpatial.toString();
        editState.setEditedSpatialTransform(position);
    }

    /**
     * Alter the local translation of the selected spatial.
     *
     * @param translation (not null, unaffected)
     */
    public void setSpatialTranslation(Vector3f translation) {
        Validate.nonNull(translation, "translation");

        Spatial selectedSpatial = getSpatial().find();
        selectedSpatial.setLocalTranslation(translation);
        String position = selectedSpatial.toString();
        editState.setEditedSpatialTransform(position);
    }

    /**
     * Alter the wireframe setting for the selected material.
     *
     * @param newState true &rarr; render edges only, false &rarr; fill
     * triangles
     */
    public void setWireframe(boolean newState) {
        Material material = getSpatial().getMaterial();
        if (material != null) {
            RenderState modelState = material.getAdditionalRenderState();
            if (modelState.isWireframe() != newState) {
                History.autoAdd();
                material.setKey(null);
                modelState.setWireframe(newState);
                updateSceneWireframe();

                String description = String.format(
                        "set wireframe flag of material to %s", newState);
                editState.setEdited(description);
            }
        }
    }

    /**
     * Toggle the bounds type of the selected geometry.
     */
    public void toggleBoundType() {
        SelectedSpatial ss = getSpatial();
        if (ss.isGeometry()) {
            History.autoAdd();
            ss.toggleBoundType();
            editState.setEdited("alter bound type");
        }
    }

    /**
     * Delete the selected attachment/bone link. The invoker is responsible for
     * de-selecting the link, if it was selected.
     */
    void unlink(PhysicsLink link) {
        assert link instanceof BoneLink || link instanceof AttachmentLink;

        SelectedRagdoll ragdoll = getRagdoll();
        DynamicAnimControl dac = ragdoll.find();
        String boneName = link.boneName();

        History.autoAdd();
        if (link instanceof BoneLink) {
            dac.unlinkBone(boneName);
            getSceneView().unlinkBone(boneName);
        } else {
            dac.detach(boneName);
            getSceneView().unlinkAttachment(boneName);
        }
        String description = "unlink " + link.name();
        editState.setEdited(description);
    }

    /**
     * Write the C-G model to the filesystem, in the specified format, at the
     * specified base path.
     *
     * @param baseFilePath file path without any extension (not null, not empty)
     * @param format the output format (not null)
     * @return true if successful, otherwise false
     */
    public boolean writeToFile(OutputFormats format, String baseFilePath) {
        Validate.nonNull(format, "format");
        Validate.nonEmpty(baseFilePath, "base file path");

        String filePath = format.extend(baseFilePath);
        File file = new File(filePath);
        /*
         * create the parent folder (see JME issue #1011)
         */
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        filePath = Heart.fixedPath(file);
        JmeExporter exporter = format.getExporter();
        boolean success = true;
        try {
            exporter.save(rootSpatial, file);
        } catch (IOException exception) {
            System.err.println(exception);
            success = false;
        }

        String quotedPath = MyString.quote(filePath);
        if (success) {
            logger.log(Level.INFO, "Wrote model to file {0}", quotedPath);
        } else {
            logger.log(Level.SEVERE,
                    "I/O exception while writing model to file {0}",
                    quotedPath);
        }

        if (success) {
            boolean maudCanLoadIt = (format == OutputFormats.J3O);
            String af = assetFolderForWrite();
            String eventDescription = "write model to " + filePath;
            if (maudCanLoadIt && baseFilePath.startsWith(af)) {
                /*
                 * The CGM was successfully written to the "Written Assets"
                 * folder in a format that Maud can load, so update the
                 * origin information and mark as pristine.
                 */
                assetRootPath = af;
                baseAssetPath = MyString.remainder(baseFilePath, af);
                /*
                 * In asset paths, a leading slash is always redundant.
                 */
                if (baseAssetPath.startsWith("/")) {
                    baseAssetPath = MyString.remainder(baseAssetPath, "/");
                }
                extension = format.extension();
                editState.setPristine(eventDescription);

            } else if (maudCanLoadIt && baseFilePath.endsWith(baseAssetPath)
                    && !baseAssetPath.isEmpty()) {
                /*
                 * The CGM was successfully written to another part of the
                 * filesystem in a format that Maud can load, so update the
                 * origin information and mark as pristine.
                 */
                assetRootPath = MyString.removeSuffix(baseFilePath,
                        baseAssetPath);
                extension = format.extension();
                editState.setPristine(eventDescription);

            } else {
                /*
                 * Don't update the origin information, don't mark as pristine.
                 */
                History.addEvent(eventDescription);
            }
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
        clone.editState = editState.clone();
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
        editState.setPristine(eventDescription);
        /*
         * Repair any minor issues with the loaded C-G model.
         */
        repairSpatials(cgmRoot);
        repairTracks(cgmRoot);

        super.postLoad(cgmRoot);
    }
    // *************************************************************************
    // private methods

    /**
     * Delete all "extra" spatials among a node's descendents. Note: recursive!
     *
     * @param subtree subtree to traverse (not null)
     * @param attachmentsNodes collection of attachments nodes (not null,
     * unaffected)
     */
    private void deleteExtraSpatials(Node subtree,
            Collection<Spatial> attachmentsNodes) {
        assert subtree != null;
        assert attachmentsNodes != null;

        List<Spatial> childList = subtree.getChildren();
        int numChildren = childList.size();
        Spatial[] children = childList.toArray(new Spatial[numChildren]);
        for (Spatial child : children) {
            if (MaudUtil.isExtra(child, attachmentsNodes)) {
                List<Integer> position = findSpatial(child);
                int index = subtree.detachChild(child);
                assert index != -1;
                getSceneView().deleteSubtree(position);
            }
        }

        for (Spatial child : subtree.getChildren()) {
            if (child instanceof Node) {
                deleteExtraSpatials((Node) child, attachmentsNodes);
            }
        }
    }

    /**
     * Repair problems with spatials in a newly-loaded model, including null
     * names, empty names, and duplicate names.
     *
     * @param cgmRoot the C-G model to repair (not null)
     */
    private void repairSpatials(Spatial cgmRoot) {
        int numRenamed = 0;

        NameGenerator generate = new NameGenerator();
        Set<String> namesUsed = new TreeSet<>();
        List<Spatial> spatials
                = MySpatial.listSpatials(cgmRoot);
        for (Spatial spatial : spatials) {
            String spatialName = spatial.getName();
            boolean renameIt = false;

            if (spatialName == null) {
                spatialName = generate.unique("repairNullName");
                renameIt = true;
            } else if (spatialName.isEmpty()) {
                spatialName = generate.unique("repairEmptyName");
                renameIt = true;
            }

            while (namesUsed.contains(spatialName)) {
                spatialName = generate.unique("repairDuplicateName");
                renameIt = true;
            }

            if (renameIt) {
                spatial.setName(spatialName);
                ++numRenamed;
            }

            namesUsed.add(spatialName);
        }

        if (numRenamed > 0) {
            String description = "renamed ";
            if (numRenamed == 1) {
                description += "one spatial";
            } else {
                description += String.format("%d spatials", numRenamed);
            }
            editState.setEdited(description);
        }
    }

    /**
     * Repair problems with animation tracks in a newly-loaded model, including
     * first keyframe not at t=0 and repetitious keyframes.
     *
     * @param cgmRoot the C-G model to repair (not null)
     */
    private void repairTracks(Spatial cgmRoot) {
        int numTracksZfed = 0;
        int numTracksRred = 0;
        int numTracksNqed = 0;

        List<AnimControl> animControls
                = MySpatial.listControls(cgmRoot, AnimControl.class, null);
        for (AnimControl animControl : animControls) {
            Collection<String> names = animControl.getAnimationNames();
            for (String animationName : names) {
                Animation anim = animControl.getAnim(animationName);
                numTracksZfed += AnimationEdit.zeroFirst(anim);
                numTracksRred += AnimationEdit.removeRepeats(anim);
                numTracksNqed
                        += AnimationEdit.normalizeQuaternions(anim, 0.00005f);
            }
        }

        if (numTracksZfed > 0) {
            String description = "zeroed the time of the first keyframe in ";
            if (numTracksZfed == 1) {
                description += "one track";
            } else {
                description += String.format("%d tracks", numTracksZfed);
            }
            editState.setEdited(description);
        }

        if (numTracksRred > 0) {
            String description = "removed repeat keyframe(s) from ";
            if (numTracksRred == 1) {
                description += "one track";
            } else {
                description += String.format("%d tracks", numTracksRred);
            }
            editState.setEdited(description);
        }

        if (numTracksNqed > 0) {
            String description = "normalized quaternion(s) in ";
            if (numTracksNqed == 1) {
                description += "one track";
            } else {
                description += String.format("%d tracks", numTracksNqed);
            }
            editState.setEdited(description);
        }
    }
}
