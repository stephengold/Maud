/*
 Copyright (c) 2017-2018, Stephen Gold
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
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.PhysicsControl;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.light.Light;
import com.jme3.material.MatParam;
import com.jme3.material.MatParamOverride;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.UserData;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.control.Control;
import com.jme3.shader.VarType;
import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MySkeleton;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.math.MyVector3f;
import jme3utilities.minie.MyControlP;
import jme3utilities.nifty.dialog.VectorDialog;
import jme3utilities.ui.ActionApplication;
import jme3utilities.wes.TrackEdit;
import maud.Maud;
import maud.MaudUtil;
import maud.PhysicsUtil;
import maud.model.History;
import maud.model.option.RigidBodyParameter;
import maud.model.option.ShapeParameter;
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
     * indicates which model state is being edited continuously, either:
     * <ul>
     * <li> "lc" + light being recolored, or
     * <li> "lpd" + light being repositioned/redirected, or
     * <li> "pp" + physics collision object being repositioned, or
     * <li> "ss" + physics collision shape being resized, or
     * <li> "st" + tree position of the spatial being transformed, or
     * <li> "" for no continuous edits
     * </ul>
     */
    private String continousEditState = "";
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
        setEdited(description);
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

        String boneName = selectedBone.getName();
        String description
                = "add attachments node for " + MyString.quote(boneName);
        setEdited(description);

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
        setEdited(eventDescription);
    }

    /**
     * Add a new material-parameter override to the selected spatial.
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

        String description = String.format(
                "add new material-parameter override %s",
                MyString.quote(parameterName));
        setEdited(description);

        getOverride().selectParameter(parameterName);
    }

    /**
     * Add a newly-created S-G control to the selected spatial.
     *
     * @param newSgc (not null)
     * @param eventDescription a textual description of the event for the edit
     * history (not null, not empty)
     */
    void addSgc(Control newSgc, String eventDescription) {
        assert newSgc != null;

        History.autoAdd();
        Spatial selectedSpatial = getSpatial().find();
        if (newSgc instanceof PhysicsControl) {
            PhysicsControl physicsControl = (PhysicsControl) newSgc;
            SceneView sceneView = getSceneView();
            sceneView.addPhysicsControl(physicsControl);
        }
        selectedSpatial.addControl(newSgc);
        setEdited(eventDescription);
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
        setEdited(eventDescription);
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
        setEdited(description);
        getUserData().selectKey(key);
    }

    /**
     * Attach a child subtree to the specified parent node.
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
        setEdited(eventDescription);
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
        Animation anim = getAnimation().getReal();
        AnimControl animControl = getAnimControl().find();

        History.autoAdd();
        animControl.removeAnim(anim);
        // scene view not updated
        setEdited("delete animation");
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

        String boneName = selectedBone.getName();
        String description = "delete attachments node for "
                + MyString.quote(boneName);
        setEdited(description);

        assert !selectedBone.hasAttachmentsNode();
    }

    /**
     * Delete the selected buffer (which must be a mapped buffer) and deselect
     * it.
     */
    public void deleteBuffer() {
        SelectedBuffer buffer = getBuffer();
        assert buffer.isMapped();
        String description = "delete buffer " + buffer.describe();
        VertexBuffer.Type type = buffer.type();

        Spatial spatial = getSpatial().find();
        Geometry geometry = (Geometry) spatial;
        Mesh mesh = geometry.getMesh();

        History.autoAdd();
        getSceneView().deleteBuffer();
        mesh.clearBuffer(type);
        if (type == VertexBuffer.Type.BoneIndex) {
            mesh.clearBuffer(VertexBuffer.Type.HWBoneIndex);
        }
        setEdited(description);

        buffer.deselect();
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
            setEdited(description);
        }
    }

    /**
     * Delete the selected material parameter. The invoker is responsible for
     * deselecting the parameter.
     */
    void deleteMatParam() {
        Material material = getSpatial().getMaterial();
        String parameterName = getMatParam().getName();
        SceneView sceneView = getSceneView();

        History.autoAdd();
        material.clearParam(parameterName);
        sceneView.deleteMatParam();

        String description = String.format(
                "delete material parameter %s", MyString.quote(parameterName));
        setEdited(description);
    }

    /**
     * Delete the selected material-parameter override from the selected
     * spatial. The invoker is responsible for deselecting the override.
     */
    void deleteOverride() {
        Spatial spatial = getSpatial().find();
        MatParamOverride mpo = getOverride().find();
        String parameterName = mpo.getName();

        History.autoAdd();
        spatial.removeMatParamOverride(mpo);
        getSceneView().deleteOverride();

        String description = String.format(
                "delete material-parameter override %s",
                MyString.quote(parameterName));
        setEdited(description);
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
            List<Integer> treePosition = findSpatial(controlled);
            PhysicsControl pc = (PhysicsControl) selectedSgc;
            int pcPosition = PhysicsUtil.pcToPosition(controlled, pc);
            sceneView.removePhysicsControl(treePosition, pcPosition);
        }

        boolean success = controlled.removeControl(selectedSgc);
        assert success;
        setEdited("delete control");
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

        setEdited("delete subtree");
    }

    /**
     * Delete the selected user data from the selected spatial. The invoker is
     * responsible for deselecting the user data.
     */
    void deleteUserData() {
        Spatial selectedSpatial = getSpatial().find();
        String key = getUserData().getKey();

        History.autoAdd();
        selectedSpatial.setUserData(key, null);
        String description
                = String.format("delete user data %s", MyString.quote(key));
        setEdited(description);
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
        Boolean selectedSpatialFlag = false;
        Skeleton newSkeleton = getSkeleton().find(selectedSpatialFlag);
        if (newSkeleton != oldSkeleton) {
            getBone().deselect();
            getPose().resetToBind(newSkeleton);
            getSceneView().setSkeleton(newSkeleton, selectedSpatialFlag);
        }

        String eventDescription = String.format("insert parent %s",
                MyString.quote(newNodeName));
        setEdited(eventDescription);
    }

    /**
     * Callback before a checkpoint is created.
     */
    public void preCheckpoint() {
        /*
         * Potentially new continuous edits.
         */
        continousEditState = "";
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
     * Rename the selected material-parameter override.
     *
     * @param newName new parameter name (not null, not empty)
     */
    public void renameOverride(String newName) {
        Validate.nonEmpty(newName, "new name");

        Spatial spatial = getSpatial().find();
        SelectedOverride override = getOverride();
        MatParamOverride oldMpo = override.find();

        String oldName = oldMpo.getName();
        Object value = oldMpo.getValue();
        VarType varType = oldMpo.getVarType();
        MatParamOverride newMpo = new MatParamOverride(varType, newName, value);
        boolean enabled = oldMpo.isEnabled();
        newMpo.setEnabled(enabled);

        History.autoAdd();
        spatial.addMatParamOverride(newMpo);
        spatial.removeMatParamOverride(oldMpo);
        getSceneView().renameOverride(newName);

        String description = String.format(
                "rename material-parameter override %s to %s",
                MyString.quote(oldName), MyString.quote(newName));
        setEdited(description);

        override.selectParameter(newName);
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

        Spatial spatial = getSpatial().find();
        SelectedUserData datum = getUserData();
        String oldKey = datum.getKey();
        Object value = datum.getValue();

        History.autoAdd();
        spatial.setUserData(oldKey, null);
        spatial.setUserData(newKey, value);

        String description = String.format("rename user-data key %s to %s",
                MyString.quote(oldKey), MyString.quote(newKey));
        setEdited(description);

        getUserData().selectKey(newKey);
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
        float duration = getAnimation().getDuration();
        if (getPlay().getTime() > duration) {
            getPlay().setTime(duration); // keep animation time in range
        }
        setEdited(eventDescription);
        getTrack().select(newSelectedTrack);
    }

    /**
     * Update which physics collision shape is being resized without triggering
     * a history event.
     *
     * @param oldShape shape to replace (not null, unaffected)
     * @param newShape replacement shape (not null)
     */
    void replaceForResize(CollisionShape oldShape, CollisionShape newShape) {
        assert newShape != null;

        String oldState = "ss" + oldShape.toString();
        if (oldState.equals(continousEditState)) {
            String newState = "ss" + newShape.toString();
            continousEditState = newState;
        }
    }

    /**
     * Replace the specified physics collision shape with a completely different
     * shape, but only in objects, not in compound shapes.
     *
     * @param oldAnimation animation to replace (not null)
     * @param newAnimation replacement animation (not null)
     * @param eventDescription description for the edit history (not null, not
     * empty)
     */
    void replaceInObjects(CollisionShape oldShape, CollisionShape newShape,
            String eventDescription) {
        assert oldShape != null;
        assert newShape != null;
        assert eventDescription != null;
        assert !eventDescription.isEmpty();

        PhysicsSpace space = getSceneView().getPhysicsSpace();
        History.autoAdd();
        PhysicsUtil.replaceInObjects(space, oldShape, newShape);
        setEdited(eventDescription);
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
        setEdited(eventDescription);
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
        setEditedLightColor();
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
        setEditedLightPosDir();
    }

    /**
     * Resize the selected physics collision shape by the specified factors
     * without altering its scale. Has no effect on compound shapes. TODO
     * implement for compound shapes
     *
     * @param factors size factor to apply each local axis (not null,
     * unaffected)
     */
    public void resizeShape(Vector3f factors) {
        Validate.nonNull(factors, "factors");

        SelectedShape shape = getShape();
        if (!MyVector3f.isScaleIdentity(factors) && !shape.isCompound()) {
            Vector3f he = shape.halfExtents(null);
            he.multLocal(factors);
            shape.setHalfExtents(he);
            setEditedShapeSize();
        }
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

        Spatial modelSpatial = getSpatial().find();
        Spatial.BatchHint oldHint = modelSpatial.getLocalBatchHint();
        if (oldHint != newHint) {
            History.autoAdd();
            modelSpatial.setBatchHint(newHint);
            // scene view not updated
            String description = String.format(
                    "set batch hint of spatial to %s", newHint);
            setEdited(description);
        }
    }

    /**
     * Alter the instance span of the selected buffer.
     *
     * @param newSpan 0 &rarr; not instanced, 1 &rarr; each element goes with
     * one instance, etc.
     */
    public void setBufferInstanceSpan(int newSpan) {
        Validate.nonNegative(newSpan, "new span");

        VertexBuffer buffer = getBuffer().find();
        int oldSpan = buffer.getInstanceSpan();
        if (oldSpan != newSpan) {
            History.autoAdd();
            buffer.setInstanceSpan(newSpan);
            getSceneView().setBufferInstanceSpan(newSpan);
            String description = String.format(
                    "set instance span of buffer to %d", newSpan);
            setEdited(description);
        }
    }

    /**
     * Alter the limit of the selected buffer.
     *
     * @param newLimit (&ge;1)
     */
    public void setBufferLimit(int newLimit) {
        Validate.positive(newLimit, "new limit");

        VertexBuffer buffer = getBuffer().find();
        Buffer data = buffer.getData();
        int oldLimit = data.limit();
        if (oldLimit != newLimit) {
            History.autoAdd();
            data.limit(newLimit);
            getSceneView().setBufferLimit(newLimit);
            String description
                    = String.format("set limit of buffer to %d", newLimit);
            setEdited(description);
        }
    }

    /**
     * Alter the normalized flag of the selected buffer.
     *
     * @param newSetting true&rarr;normalized, false&rarr;not normalized
     */
    public void setBufferNormalized(boolean newSetting) {
        VertexBuffer buffer = getBuffer().find();
        boolean oldSetting = buffer.isNormalized();
        if (oldSetting != newSetting) {
            History.autoAdd();
            buffer.setNormalized(newSetting);
            getSceneView().setBufferNormalized(newSetting);
            String description = String.format(
                    "set normalized flag of buffer to %s", newSetting);
            setEdited(description);
        }
    }

    /**
     * Alter the stride of the selected buffer.
     *
     * @param newStride new value for stride (&ge;0)
     */
    public void setBufferStride(int newStride) {
        Validate.nonNegative(newStride, "new stride");

        VertexBuffer buffer = getBuffer().find();
        int oldStride = buffer.getStride();
        if (oldStride != newStride) {
            History.autoAdd();
            buffer.setStride(newStride);
            getSceneView().setBufferStride(newStride);
            String description = String.format(
                    "set stride of buffer to %d", newStride);
            setEdited(description);
        }
    }

    /**
     * Alter the usage of the selected buffer.
     *
     * @param newUsage new value for usage (not null)
     */
    public void setBufferUsage(VertexBuffer.Usage newUsage) {
        Validate.nonNull(newUsage, "new usage");

        VertexBuffer buffer = getBuffer().find();
        VertexBuffer.Usage oldUsage = buffer.getUsage();
        if (oldUsage != newUsage) {
            History.autoAdd();
            buffer.setUsage(newUsage);
            getSceneView().setBufferUsage(newUsage);
            String description = String.format(
                    "set usage of buffer to %s", newUsage);
            setEdited(description);
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
            setEdited(description);
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
                modelState.setDepthTest(newState);
                SceneView sceneView = getSceneView();
                sceneView.setDepthTest(newState);

                String description = String.format(
                        "set depth test flag of material to %s", newState);
                setEdited(description);
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
                modelState.setFaceCullMode(newMode);
                SceneView sceneView = getSceneView();
                sceneView.setFaceCullMode(newMode);

                String description = String.format(
                        "set face-cull mode of material to %s", newMode);
                setEdited(description);
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
                    setEdited("ignore transform");
                } else {
                    setEdited("stop ignoring transform");
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
        setEdited("replace keyframes");
    }

    /**
     * Alter the value of the selected material parameter.
     *
     * @param valueString string representation of the new value (not null)
     */
    public void setMatParamValue(String valueString) {
        Validate.nonNull(valueString, "value string");

        MatParam oldParam = getMatParam().find();
        Object modelValue = MaudUtil.parseMatParam(oldParam, valueString);
        Object viewValue = MaudUtil.parseMatParam(oldParam, valueString);
        String parameterName = oldParam.getName();
        Material material = getSpatial().getMaterial();

        History.autoAdd();
        VarType varType = oldParam.getVarType();
        material.setParam(parameterName, varType, modelValue);
        getSceneView().setMatParamValue(viewValue);

        String description = String.format(
                "alter value of material parameter %s",
                MyString.quote(parameterName));
        setEdited(description);
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
            setEdited(description);
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
            setEdited(description);
        }
    }

    /**
     * Alter whether the selected material-parameter override is enabled.
     *
     * @param newSetting true&rarr;enable, false&rarr;disable
     */
    public void setOverrideEnabled(boolean newSetting) {
        MatParamOverride mpo = getOverride().find();
        if (mpo != null) {
            boolean oldSetting = mpo.isEnabled();
            if (oldSetting != newSetting) {
                History.autoAdd();
                mpo.setEnabled(newSetting);
                getSceneView().setOverrideEnabled(newSetting);

                String verb = newSetting ? "enable" : "disable";
                String parameterName = mpo.getName();
                String description = String.format(
                        "%s material-parameter override %s",
                        verb, MyString.quote(parameterName));
                setEdited(description);
            }
        }
    }

    /**
     * Alter the value of the selected material-parameter override.
     *
     * @param valueString string representation of the new value (not null)
     */
    public void setOverrideValue(String valueString) {
        Validate.nonNull(valueString, "value string");

        MatParamOverride oldMpo = getOverride().find();
        Object modelValue = MaudUtil.parseMatParam(oldMpo, valueString);
        Object viewValue = MaudUtil.parseMatParam(oldMpo, valueString);
        String parameterName = oldMpo.getName();
        Spatial spatial = getSpatial().find();

        History.autoAdd();
        spatial.removeMatParamOverride(oldMpo);
        VarType varType = oldMpo.getVarType();
        MatParamOverride newMpo
                = new MatParamOverride(varType, parameterName, modelValue);
        spatial.addMatParamOverride(newMpo);
        getSceneView().setOverrideValue(viewValue);

        String description = String.format(
                "alter value of material-parameter override %s",
                MyString.quote(parameterName));
        setEdited(description);
    }

    /**
     * Relocate the selected physics object.
     *
     * @param newLocation (not null, unaffected)
     */
    public void setPhysicsLocation(Vector3f newLocation) {
        Validate.nonNull(newLocation, "new location");

        getObject().setLocation(newLocation);
        setEditedPhysicsPosition();
    }

    /**
     * Reorient the selected physics object.
     *
     * @param newOrientation (not null, unaffected)
     */
    public void setPhysicsOrientation(Quaternion newOrientation) {
        Validate.nonNull(newOrientation, "new orientation");

        getObject().setOrientation(newOrientation);
        setEditedPhysicsPosition();
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
            setEdited("change render-queue bucket");
        }
    }

    /**
     * Alter the specified parameter of the selected rigid body.
     *
     * @param parameter which parameter to alter (not null)
     * @param newValue new parameter value
     */
    public void setRigidBodyParameter(RigidBodyParameter parameter,
            float newValue) {
        Validate.nonNull(parameter, "parameter");

        SelectedObject selected = getObject();
        PhysicsCollisionObject pco = selected.find();
        if (pco instanceof PhysicsRigidBody) {
            History.autoAdd();
            selected.set(parameter, newValue);
            String eventDescription = String.format(
                    "set %s of rigid body to %f", parameter, newValue);
            setEdited(eventDescription);
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

        Spatial modelSpatial = getSpatial().find();
        RenderQueue.ShadowMode oldMode = modelSpatial.getLocalShadowMode();
        if (oldMode != newMode) {
            History.autoAdd();
            modelSpatial.setShadowMode(newMode);
            getSceneView().setShadowMode(newMode);
            String description = String.format(
                    "change spatial's shadow mode to %s", newMode);
            setEdited(description);
        }
    }

    /**
     * Alter the specified parameter of the selected physics collision shape.
     *
     * @param parameter which parameter to alter (not null)
     * @param newValue new parameter value
     */
    public void setShapeParameter(ShapeParameter parameter, float newValue) {
        Validate.nonNull(parameter, "parameter");

        SelectedShape shape = getShape();
        assert shape.canSet(parameter);
        float oldValue = shape.getValue(parameter);
        if (newValue != oldValue) {
            if (parameter.equals(ShapeParameter.Margin)) {
                History.autoAdd();
                shape.set(parameter, newValue);
                String description = String.format(
                        "change shape's margin to %f", newValue);
                setEdited(description);
            } else {
                shape.set(parameter, newValue);
                setEditedShapeSize();
            }
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

        Spatial selectedSpatial = getSpatial().find();
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

        Spatial selectedSpatial = getSpatial().find();
        selectedSpatial.setLocalTranslation(translation);
        setEditedSpatialTransform();
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
                modelState.setWireframe(newState);
                updateSceneWireframe();

                String description = String.format(
                        "set wireframe flag of material to %s", newState);
                setEdited(description);
            }
        }
    }

    /**
     * Alter the selected user datum.
     *
     * @param valueString string representation of the new value (not null)
     */
    public void setUserData(String valueString) {
        Validate.nonNull(valueString, "value string");

        SelectedUserData datum = getUserData();
        Object value = datum.getValue();
        Spatial spatial = getSpatial().find();
        String key = datum.getKey();

        History.autoAdd();
        if (value instanceof Boolean) {
            boolean newValue = Boolean.parseBoolean(valueString);
            spatial.setUserData(key, newValue);

        } else if (value instanceof Float) {
            float newValue = Float.parseFloat(valueString);
            spatial.setUserData(key, newValue);

        } else if (value instanceof Integer) {
            int newValue = Integer.parseInt(valueString);
            spatial.setUserData(key, newValue);

        } else if (value instanceof Long) {
            long newValue = Long.parseLong(valueString);
            spatial.setUserData(key, newValue);

        } else if (value instanceof String) {
            spatial.setUserData(key, valueString);

        } else if (value instanceof Vector2f
                || value instanceof Vector3f
                || value instanceof Vector4f) {
            Object newValue = VectorDialog.parseVector(valueString);
            spatial.setUserData(key, newValue);

        } else {        // TODO bone value
            throw new IllegalStateException();
        }

        String description = String.format("alter value of user datum %s",
                MyString.quote(key));
        setEdited(description);
    }

    /**
     * Toggle the bounds type of the selected geometry.
     */
    public void toggleBoundType() {
        SelectedSpatial ss = getSpatial();
        if (ss.isGeometry()) {
            History.autoAdd();
            ss.toggleBoundType();
            setEdited("alter bound type");
        }
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
            String description = "zeroed the time of the 1st keyframe in ";
            if (numTracksZfed == 1) {
                description += "one track";
            } else {
                description += String.format("%d tracks", numTracksZfed);
            }
            setEdited(description);
        }

        if (numTracksRred > 0) {
            String description = "removed repeat keyframe(s) from ";
            if (numTracksRred == 1) {
                description += "one track";
            } else {
                description += String.format("%d tracks", numTracksRred);
            }
            setEdited(description);
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
        continousEditState = "";
        History.addEvent(eventDescription);
    }

    /**
     * If not a continuation of the previous light-color edit, update the edit
     * count.
     */
    private void setEditedLightColor() {
        String lightName = getLight().name();
        String newState = "lc" + lightName;
        if (!newState.equals(continousEditState)) {
            History.autoAdd();
            ++editCount;
            continousEditState = newState;
            String description = String.format("recolor light named %s",
                    MyString.quote(lightName));
            History.addEvent(description);
        }
    }

    /**
     * If not a continuation of the previous light-position/direction edit,
     * update the edit count.
     */
    private void setEditedLightPosDir() {
        String lightName = getLight().name();
        String newState = "lpd" + lightName;
        if (!newState.equals(continousEditState)) {
            History.autoAdd();
            ++editCount;
            continousEditState = newState;
            String description = String.format(
                    "reposition and/or redirect light named %s",
                    MyString.quote(lightName));
            History.addEvent(description);
        }
    }

    /**
     * If not a continuation of the previous object-position edit, update the
     * edit count.
     */
    private void setEditedPhysicsPosition() {
        String newState = "pp" + getObject().getName();
        if (!newState.equals(continousEditState)) {
            History.autoAdd();
            ++editCount;
            continousEditState = newState;
            History.addEvent("reposition collision object");
        }
    }

    /**
     * If not a continuation of the previous shape-size edit, update the edit
     * count.
     */
    private void setEditedShapeSize() {
        String newState = "ss" + getShape().find().toString();
        if (!newState.equals(continousEditState)) {
            History.autoAdd();
            ++editCount;
            continousEditState = newState;
            History.addEvent("resize collision shape");
        }
    }

    /**
     * If not a continuation of the previous spatial-transform edit, update the
     * edit count.
     */
    private void setEditedSpatialTransform() {
        String newState = "st" + getSpatial().toString();
        if (!newState.equals(continousEditState)) {
            History.autoAdd();
            ++editCount;
            continousEditState = newState;
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
        continousEditState = "";
        History.addEvent(eventDescription);
    }
}
