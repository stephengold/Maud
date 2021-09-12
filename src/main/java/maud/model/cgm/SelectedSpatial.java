/*
 Copyright (c) 2017-2021, Stephen Gold
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

import com.jme3.anim.AnimComposer;
import com.jme3.anim.Armature;
import com.jme3.anim.SkinningControl;
import com.jme3.animation.AnimControl;
import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.bounding.BoundingVolume;
import com.jme3.bullet.animation.DynamicAnimControl;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.ConvexShape;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.GhostControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.LightList;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.material.MatParam;
import com.jme3.material.MatParamOverride;
import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.Control;
import com.jme3.util.SafeArrayList;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import jme3utilities.MeshNormals;
import jme3utilities.MyAsset;
import jme3utilities.MyMesh;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import jme3utilities.math.MyQuaternion;
import jme3utilities.math.MyVector3f;
import jme3utilities.ui.Locators;
import maud.Maud;
import maud.MaudUtil;
import maud.MeshUtil;
import maud.PhysicsUtil;
import maud.ShapeType;
import maud.model.EditState;
import maud.model.History;
import maud.tool.EditorTools;
import maud.view.scene.SceneView;

/**
 * The MVC model of the selected spatial in a loaded C-G model.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SelectedSpatial implements JmeCloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * local copy of {@link com.jme3.math.ColorRGBA#White}
     */
    final private static ColorRGBA white = new ColorRGBA(1f, 1f, 1f, 1f);
    /**
     * dummy buffer index, used to indicate that no buffer is selected
     */
    final public static int noBufferIndex = -1;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SelectedSpatial.class.getName());
    /**
     * dummy buffer description, used to indicate that no buffer is selected
     */
    final public static String noBuffer = "( no buffer )";
    /**
     * local copy of {@link com.jme3.math.Transform#IDENTITY}
     */
    final private static Transform transformIdentity = new Transform();
    // *************************************************************************
    // fields

    /**
     * C-G model containing the spatial (set by {@link #setCgm(Cgm)})
     */
    private Cgm cgm = null;
    /**
     * editable C-G model, if any, containing the spatial (set by
     * {@link #setCgm(Cgm)})
     */
    private EditableCgm editableCgm = null;
    /**
     * tree position of the spatial (not null)
     */
    private List<Integer> treePosition = new ArrayList<>(3);
    /**
     * most recent selection
     */
    private Spatial last = null;
    // *************************************************************************
    // new methods exposed TODO split off SelectedMesh, selectedNode, etcetera

    /**
     * Add an AnimComposer or AnimControl to the spatial and select the new
     * control.
     */
    public void addAnimControl() {
        AbstractControl newSgc;
        Object skeleton = cgm.getSkeleton().find();
        if (skeleton instanceof Armature) {
            newSgc = new SkinningControl((Armature) skeleton);
            editableCgm.addSgc(newSgc, "add a SkinningControl");
        } else {
            newSgc = new AnimControl((Skeleton) skeleton);
            editableCgm.addSgc(newSgc, "add an AnimControl");
        }

        editableCgm.getSgc().select(newSgc);
    }

    /**
     * Add a BetterCharacterControl to the Spatial and select the new Control.
     */
    public void addBetterCharacterControl() {
        float radius = 1f; // TODO base dimensions on the model
        float height = 3f;
        float mass = 1f;
        BetterCharacterControl bcc
                = new BetterCharacterControl(radius, height, mass);

        editableCgm.addSgc(bcc, "add a BetterCharacterControl");
        editableCgm.getSgc().select(bcc);
    }

    /**
     * Add a CharacterControl to the Spatial and select the new Control.
     *
     * @param shapeType desired type of shape (not null)
     */
    public void addCharacterControl(ShapeType shapeType) {
        Validate.nonNull(shapeType, "shape type");

        Spatial subtree = find();
        ConvexShape shape
                = (ConvexShape) PhysicsUtil.makeShape(shapeType, subtree);
        CharacterControl cc = new CharacterControl(shape, 1f);

        editableCgm.addSgc(cc, "add a CharacterControl");
        Spatial modelSpatial = find();
        editableCgm.getSgc().select(cc, modelSpatial);
    }

    /**
     * Add a GhostControl to the Spatial and select the new Control.
     *
     * @param shapeType desired type of shape (not null)
     */
    public void addGhostControl(ShapeType shapeType) {
        Validate.nonNull(shapeType, "shape type");

        Spatial subtree = find();
        CollisionShape shape = PhysicsUtil.makeShape(shapeType, subtree);
        GhostControl ghostControl = new GhostControl(shape);
        ghostControl.setApplyScale(true);

        editableCgm.addSgc(ghostControl, "add a GhostControl");
        Spatial modelSpatial = find();
        editableCgm.getSgc().select(ghostControl, modelSpatial);
    }

    /**
     * Add a Light to the Spatial and select the new Light.
     *
     * @param type (not null)
     * @param name a name for the new light (not null, not empty)
     */
    public void addLight(Light.Type type, String name) {
        Validate.nonEmpty(name, "name");
        assert !cgm.hasLight(name);

        Light newLight;
        switch (type) {
            case Ambient:
                newLight = new AmbientLight();
                break;

            case Directional:
                newLight = new DirectionalLight();
                break;

            case Point:
                newLight = new PointLight();
                break;

            case Spot:
                newLight = new SpotLight();
                break;

            case Probe: // TODO
            default:
                throw new IllegalArgumentException();
        }

        newLight.setName(name);

        String description = String.format("add %s light named %s",
                type.toString(), MyString.quote(name));
        editableCgm.addLight(newLight, description);

        Spatial spatial = find();
        editableCgm.getLight().select(newLight, spatial);
    }

    /**
     * Add a DynamicAnimControl to the Spatial and select the new Control.
     */
    public void addRagdollControl() {
        /*
         * Make sure the correct skeleton is selected, so that
         * SelectedSkeleton.postSelect() won't cause SceneView
         * to create new S-G controls.
         */
        Spatial spatial = find();
        SkeletonControl skeletonControl
                = spatial.getControl(SkeletonControl.class);
        editableCgm.getSgc().select(skeletonControl, spatial);

        DynamicAnimControl dac = new DynamicAnimControl();
        editableCgm.addSgc(dac, "add a DynamicAnimControl");
        editableCgm.getSgc().select(dac, spatial);
    }

    /**
     * Add a RigidBodyControl to the spatial and select the new control.
     *
     * @param shapeType desired type of shape (not null)
     */
    public void addRigidBodyControl(ShapeType shapeType) {
        Validate.nonNull(shapeType, "shape type");

        Spatial subtree = find();
        CollisionShape shape = PhysicsUtil.makeShape(shapeType, subtree);
        float mass = 1f;
        RigidBodyControl rbc = new RigidBodyControl(shape, mass);
        rbc.setApplyScale(true);
        rbc.setKinematic(true);
        // TODO why is the default kinematic=false but kinematicSpatial=true?

        editableCgm.addSgc(rbc, "add a RigidBodyControl");
        Spatial modelSpatial = find();
        editableCgm.getSgc().select(rbc, modelSpatial);
    }

    /**
     * Add a SkeletonControl to the selected spatial and select the new control.
     */
    public void addSkeletonControl() {
        Object skeleton = cgm.getSkeleton().find();
        if (skeleton == null) {
            Spatial spatial = find();
            int numBones = MySpatial.countMeshBones(spatial);
            // TODO option for a SkinningControl
            Bone[] bones = new Bone[numBones];
            for (int boneIndex = 0; boneIndex < numBones; boneIndex++) {
                String boneName = String.format("bone%d", boneIndex);
                bones[boneIndex] = new Bone(boneName);
            }
            skeleton = new Skeleton(bones);
        }

        AbstractControl newSgc;
        if (skeleton instanceof Armature) {
            newSgc = new SkinningControl((Armature) skeleton);
            editableCgm.addSgc(newSgc, "add a SkinningControl");

        } else {
            newSgc = new SkeletonControl((Skeleton) skeleton);
            editableCgm.addSgc(newSgc, "add a SkeletonControl");
        }

        editableCgm.getSgc().select(newSgc);
    }

    /**
     * Apply normal-debugging material to the selected Geometry.
     */
    public void applyDebugMaterial() {
        AssetManager am = Locators.getAssetManager();
        Material newMaterial
                = new Material(am, "Common/MatDefs/Misc/ShowNormals.j3md");
        editableCgm.setMaterial(newMaterial, "apply debug material");
    }

    /**
     * Apply a lit material to the selected geometry.
     */
    public void applyLitMaterial() {
        AssetManager am = Locators.getAssetManager();
        Material newMaterial = MyAsset.createShinyMaterial(am, white);
        editableCgm.setMaterial(newMaterial, "apply lit material");
    }

    /**
     * Apply the local Transform of the selected Node to each of its children.
     */
    public void applyTransformToChildren() {
        Node node = (Node) find();
        Transform nodeTransform = node.getLocalTransform(); // alias
        assert !MyMath.isIdentity(nodeTransform);
        List<Spatial> children = node.getChildren();
        assert !children.isEmpty();

        History.autoAdd();

        Transform tmpTransform = new Transform(); // TODO garbage
        for (Spatial child : children) {
            tmpTransform.set(child.getLocalTransform());
            tmpTransform.combineWithParent(nodeTransform);
            child.setLocalTransform(tmpTransform);
        }
        node.setLocalTransform(transformIdentity);

        EditState editState = editableCgm.getEditState();
        editState.setEdited("apply node transform to children");
    }

    /**
     * Apply the local Transform of the selected Spatial (and those of its
     * descendents) to all of their meshes.
     */
    public void applyTransformToMeshes() {
        Spatial subtree = find();
        Node parent = subtree.getParent();
        Transform wip;
        if (parent == null) {
            wip = new Transform();
        } else {
            Transform parentInWorld = parent.getWorldTransform();
            wip = parentInWorld.invert();
        }
        List<Geometry> geometries
                = MySpatial.listSpatials(subtree, Geometry.class, null);
        SceneView sceneView = cgm.getSceneView();

        History.autoAdd();

        for (Geometry geometry : geometries) {
            Mesh mesh = geometry.getMesh(); // TODO check for instancing
            Transform gInWorld = geometry.getWorldTransform().clone();
            Transform gInParent = gInWorld.combineWithParent(wip);

            MeshUtil.transformBuffer(mesh, VertexBuffer.Type.BindPosePosition,
                    gInParent);
            MeshUtil.transformBuffer(mesh, VertexBuffer.Type.Position,
                    gInParent);

            Quaternion gInPRot = gInParent.getRotation();
            MyMesh.rotateBuffer(mesh, VertexBuffer.Type.BindPoseNormal,
                    gInPRot);
            MyMesh.rotateBuffer(mesh, VertexBuffer.Type.Normal, gInPRot);
            // TODO binormal?

            mesh.updateBound();

            List<Integer> position = cgm.findSpatial(geometry);
            sceneView.setMesh(position, mesh);
        }

        List<Spatial> spatials = MySpatial.listSpatials(subtree);
        for (Spatial spatial : spatials) {
            spatial.setLocalTransform(transformIdentity);
        }
        sceneView.applyTransform();

        editableCgm.getEditState().setEdited(
                "apply spatial transforms to meshes");
    }

    /**
     * Apply an unshaded material to the selected geometry.
     */
    public void applyUnshadedMaterial() {
        AssetManager am = Locators.getAssetManager();
        Material newMaterial = MyAsset.createUnshadedMaterial(am);
        editableCgm.setMaterial(newMaterial, "apply unshaded material");
    }

    /**
     * Attach a clone of the source C-G model to the selected scene-graph node.
     */
    public void attachClone() {
        assert cgm == Maud.getModel().getTarget();

        LoadedCgm sourceCgm = Maud.getModel().getSource();
        Node parentNode = (Node) find();
        assert sourceCgm.isLoaded();
        Spatial cgmRoot = sourceCgm.getRootSpatial();
        String name = sourceCgm.getName();

        Spatial clone = cgmRoot.clone();
        String description
                = String.format("merge model %s", MyString.quote(name));
        editableCgm.attachSpatial(parentNode, clone, description);
    }

    /**
     * Attach a leaf (empty) node to the selected scene-graph node.
     *
     * @param leafNodeName a name for the new node (not null, not empty)
     */
    public void attachLeafNode(String leafNodeName) {
        Validate.nonEmpty(leafNodeName, "leaf-node name");
        assert cgm == Maud.getModel().getTarget();

        Node parentNode = (Node) find();
        Node leafNode = new Node(leafNodeName);
        String description = String.format("attach leaf node %s",
                MyString.quote(leafNodeName));
        editableCgm.attachSpatial(parentNode, leafNode, description);
    }

    /**
     * Reattach the selected Spatial one level higher (closer to the root) in
     * the scene graph.
     */
    public void boost() {
        int treeLevel = treePosition.size();
        assert treeLevel > 1 : treeLevel;
        Spatial spatial = find();
        String name = spatial.getName();
        Node parent = spatial.getParent();
        Node target = parent.getParent();

        String eventDescription = "boost spatial " + MyString.quote(name);
        editableCgm.moveSpatials(target, eventDescription, spatial);
        /*
         * Keep the same spatial selected.
         */
        treePosition = cgm.findSpatial(spatial);
        assert treePosition != null;
    }

    /**
     * Reattach all children of the selected Spatial one level higher (closer to
     * the root) in the scene graph.
     */
    public void boostAllChildren() {
        int treeLevel = treePosition.size();
        assert treeLevel > 0 : treeLevel;

        Node node = (Node) find();
        String name = node.getName();
        Node target = node.getParent();
        List<Spatial> childList = node.getChildren();
        int numChildren = childList.size();
        Spatial[] children = childList.toArray(new Spatial[numChildren]);
        String eventDescription = String.format("boost %d children of %s",
                numChildren, MyString.quote(name));
        editableCgm.moveSpatials(target, eventDescription, children);
        assert find() == node;
    }

    /**
     * Cardinalize the local rotation.
     */
    public void cardinalizeRotation() {
        Quaternion localRotation = localRotation(null);
        MyQuaternion.cardinalizeLocal(localRotation);
        editableCgm.setSpatialRotation(localRotation);
    }

    /**
     * Clone the material and apply it to the selected spatial, in order to
     * eliminate any sharing of the material.
     */
    public void cloneMaterial() {
        Material oldMaterial = getMaterial();
        Material clone = oldMaterial.clone();
        clone.setKey(null);
        editableCgm.setMaterial(clone, "clone material");
    }

    /**
     * Test whether the spatial's subtree contains any meshes.
     *
     * @return true if it contains meshes, otherwise false
     */
    public boolean containsMeshes() {
        Spatial subtree = find();
        boolean result = MaudUtil.containsMeshes(subtree);

        return result;
    }

    /**
     * Copy the material's additional render state.
     *
     * @return a new instance
     */
    public RenderState copyAdditionalRenderState() {
        Material material = getMaterial();
        RenderState result = material.getAdditionalRenderState();

        return result.clone();
    }

    /**
     * Copy the spatial's world bound or mesh bound.
     *
     * @param meshFlag true for the mesh bound, false for the world bound
     * @return a new instance, or null if the bound is null
     */
    public BoundingVolume copyBound(boolean meshFlag) {
        Spatial spatial = find();

        BoundingVolume result;
        if (meshFlag) {
            if (spatial instanceof Geometry) {
                Geometry geom = (Geometry) spatial;
                Mesh mesh = geom.getMesh();
                result = mesh.getBound();
            } else {
                result = null;
            }
        } else {
            result = spatial.getWorldBound();
        }

        if (result != null) {
            result = result.clone();
        }
        return result;
    }

    /**
     * Count how many children are attached to the spatial.
     *
     * @return count (&ge;0) or 0 if the spatial is not a scene-graph node
     */
    public int countChildren() {
        Spatial parent = find();

        int result;
        if (parent instanceof Node) {
            Node node = (Node) parent;
            result = node.getQuantity();
        } else {
            result = 0;
        }

        assert result >= 0 : result;
        return result;
    }

    /**
     * Count how many elements are in the spatial's mesh.
     *
     * @return count (&ge;0)
     */
    public int countElements() {
        Mesh mesh = getMesh();
        int result;
        if (mesh == null) {
            result = 0;
        } else {
            result = mesh.getTriangleCount();
        }

        assert result >= 0 : result;
        return result;
    }

    /**
     * Count how many local lights the spatial has.
     *
     * @return count (&ge;0)
     */
    public int countLights() {
        Spatial spatial = find();

        LightList list = spatial.getLocalLightList();
        int result = list.size();

        assert result >= 0 : result;
        return result;
    }

    /**
     * Count how many levels of detail are in the mesh.
     *
     * @return count (&ge;0)
     */
    public int countLodLevels() {
        Mesh mesh = getMesh();
        int result;
        if (mesh == null) {
            result = 0;
        } else {
            result = mesh.getNumLodLevels();
        }

        assert result >= 0 : result;
        return result;
    }

    /**
     * Count uses of the selected material.
     *
     * @return count (&ge;0)
     */
    public int countMaterialUses() {
        Spatial cgmRoot = cgm.getRootSpatial();
        Material material = getMaterial();
        int result = MySpatial.countUses(cgmRoot, material);

        assert result >= 0 : result;
        return result;
    }

    /**
     * Count how many local material-parameter overrides the spatial has.
     *
     * @return count (&ge;0)
     */
    public int countOverrides() {
        Spatial spatial = find();
        List<MatParamOverride> list = spatial.getLocalMatParamOverrides();
        int result = list.size();

        return result;
    }

    /**
     * Count how many scene-graph controls are added directly to the spatial.
     *
     * @return count (&ge;0)
     */
    public int countSgcs() {
        Spatial spatial = find();
        int result = spatial.getNumControls();

        assert result >= 0 : result;
        return result;
    }

    /**
     * Count how many siblings the Spatial has, including itself.
     *
     * @return the count (&ge;1)
     */
    public int countSiblings() {
        Spatial spatial = find();
        Node parent = spatial.getParent();

        int result;
        if (parent == null) { // the C-G model root is selected
            result = 1;
        } else {
            result = parent.getQuantity();
        }

        assert result >= 1 : result;
        return result;
    }

    /**
     * Count how many scene-graph controls are contained in the spatial's
     * subtree.
     *
     * @return count (&ge;0)
     */
    public int countSubtreeSgcs() {
        Spatial spatial = find();
        int result = MySpatial.countControls(spatial, Control.class);

        assert result >= 0 : result;
        return result;
    }

    /**
     * Count how many user data are contained in the spatial's subtree.
     *
     * @return count (&ge;0)
     */
    public int countSubtreeUserData() {
        Spatial spatial = find();
        int result = MySpatial.countUserData(spatial);

        assert result >= 0 : result;
        return result;
    }

    /**
     * Count how many vertices are influenced by the spatial.
     *
     * @return count (&ge;0)
     */
    public int countSubtreeVertices() {
        Spatial spatial = find();
        int result = MySpatial.countVertices(spatial);

        assert result >= 0 : result;
        return result;
    }

    /**
     * Count the user data of the spatial.
     *
     * @return count (&ge;0)
     */
    public int countUserData() {
        Spatial spatial = find();
        Collection<String> keys = spatial.getUserDataKeys();
        int result = keys.size();

        return result;
    }

    /**
     * Count how many vertices are in the spatial's mesh.
     *
     * @return count (&ge;0)
     */
    public int countVertices() {
        Mesh mesh = getMesh();
        int result;
        if (mesh == null) {
            result = 0;
        } else {
            result = mesh.getVertexCount();
        }

        assert result >= 0 : result;
        return result;
    }

    /**
     * If the spatial has a parent, delete the spatial and select its parent.
     */
    public void delete() {
        Spatial selectedSpatial = find();
        Node parent = selectedSpatial.getParent();
        if (parent != null) {
            /*
             * If the selected S-G control will be deleted, deselect it.
             */
            SelectedSgc sgc = cgm.getSgc();
            Spatial controlled = sgc.getControlled();
            if (subtreeContains(controlled)) {
                sgc.selectNone();
            }

            editableCgm.deleteSubtree();
            /*
             * Select the parent node.
             */
            int lastLevel = treePosition.size() - 1;
            treePosition.remove(lastLevel);
            assert find() == parent;
            postSelect();
        }
    }

    /**
     * Describe the spatial's type, i.e. "Geometry".
     *
     * @return a textual description (not null)
     */
    public String describeType() {
        Spatial spatial = find();
        String typeText = spatial.getClass().getSimpleName();

        return typeText;
    }

    /**
     * Access the selected spatial in the MVC model.
     *
     * @return the pre-existing instance (not null)
     */
    Spatial find() {
        Spatial modelRoot = cgm.getRootSpatial();
        Spatial result = underRoot(modelRoot);

        assert result != null;
        return result;
    }

    /**
     * Access the indexed buffer.
     *
     * @param index which buffer in the list (&ge;0)
     * @return the pre-existing instance, or null if not found
     */
    VertexBuffer findBuffer(int index) {
        assert index >= 0 : index;

        VertexBuffer result = null;
        List<String> bufferDescs = listBufferDescs("");
        int numBuffers = bufferDescs.size();
        if (index < numBuffers) {
            String desc = bufferDescs.get(index);
            result = findBuffer(desc);
        }

        return result;
    }

    /**
     * Determine the index of the selected Geometry among all geometries in the
     * C-G model.
     *
     * @return the index, or -1 if the selected spatial is not a Geometry
     */
    public int findGeometryIndex() {
        Spatial spatial = find();
        Spatial cgmRoot = cgm.getRootSpatial();
        List<Geometry> geomList
                = MySpatial.listSpatials(cgmRoot, Geometry.class, null);
        int result = geomList.indexOf(spatial);

        return result;
    }

    /**
     * Generate mesh normals from positions.
     *
     * @param algorithm which algorithm to use (not null)
     */
    public void generateNormals(MeshNormals algorithm) {
        Validate.nonNull(algorithm, "algorithm");

        Mesh oldMesh = getMesh();
        Mesh newMesh = MeshUtil.generateNormals(oldMesh, algorithm);
        String message = "generate mesh normals for " + algorithm;
        editableCgm.setMesh(newMesh, message);
    }

    /**
     * Read the name of an indexed child of the selected spatial.
     *
     * @param childIndex which child (&ge;0)
     * @return name, or null if none
     */
    public String getChildName(int childIndex) {
        assert childIndex >= 0 : childIndex;

        String result = null;
        Spatial child = modelChild(childIndex);
        if (child != null) {
            result = child.getName();
        }

        return result;
    }

    /**
     * Read the local batch hint of the selected spatial.
     *
     * @return an enum value (not null)
     */
    public Spatial.BatchHint getLocalBatchHint() {
        Spatial spatial = find();
        Spatial.BatchHint result = spatial.getLocalBatchHint();

        assert result != null;
        return result;
    }

    /**
     * Read the local cull hint of the spatial.
     *
     * @return an enum value (not null)
     */
    public Spatial.CullHint getLocalCullHint() {
        Spatial spatial = find();
        Spatial.CullHint result = spatial.getLocalCullHint();

        assert result != null;
        return result;
    }

    /**
     * Read the local render bucket of the spatial.
     *
     * @return an enum value (not null)
     */
    public RenderQueue.Bucket getLocalQueueBucket() {
        Spatial spatial = find();
        RenderQueue.Bucket result = spatial.getLocalQueueBucket();

        assert result != null;
        return result;
    }

    /**
     * Read the local shadow mode of the spatial.
     *
     * @return an enum value (not null)
     */
    public RenderQueue.ShadowMode getLocalShadowMode() {
        Spatial spatial = find();
        RenderQueue.ShadowMode result = spatial.getLocalShadowMode();

        assert result != null;
        return result;
    }

    /**
     * Access the spatial's material.
     *
     * @return the pre-existing instance, or null if none
     */
    Material getMaterial() {
        Material material = null;
        Spatial spatial = find();
        if (spatial instanceof Geometry) {
            Geometry geometry = (Geometry) spatial;
            material = geometry.getMaterial();
        }

        return material;
    }

    /**
     * Read the name of the material definition.
     *
     * @return name (not null) or "" if none
     */
    public String getMaterialDefName() {
        String name = null;
        Material material = getMaterial();
        if (material != null) {
            MaterialDef def = material.getMaterialDef();
            if (def != null) {
                name = def.getName();
            }
        }

        return name;
    }

    /**
     * Read the name of the material.
     *
     * @return name, or null if no material
     */
    public String getMaterialName() {
        String result = null;
        Material material = getMaterial();
        if (material != null) {
            result = material.getName();
        }

        return result;
    }

    /**
     * Read the maximum number of weights per vertex in the mesh.
     *
     * @return count (&ge;0, &le;4)
     */
    public int getMaxNumWeights() {
        Mesh mesh = getMesh();
        int maxNumWeights = mesh.getMaxNumWeights();

        assert maxNumWeights >= 0 : maxNumWeights;
        assert maxNumWeights <= 4 : maxNumWeights;
        return maxNumWeights;
    }

    /**
     * Access the mesh.
     *
     * @return the pre-existing instance, or null if none
     */
    Mesh getMesh() {
        Mesh result = null;
        Spatial spatial = find();
        if (spatial instanceof Geometry) {
            Geometry geometry = (Geometry) spatial;
            result = geometry.getMesh();
        }

        return result;
    }

    /**
     * Read the mode of the mesh.
     *
     * @return an enum value, or null if none
     */
    public Mesh.Mode getMeshMode() {
        Mesh.Mode result = null;
        Mesh mesh = getMesh();
        if (mesh != null) {
            result = mesh.getMode();
        }

        return result;
    }

    /**
     * Read the name of the spatial.
     *
     * @return name, or null if none
     */
    public String getName() {
        Spatial spatial = find();
        String result = spatial.getName();

        return result;
    }

    /**
     * Read the name of the parent of the selected spatial.
     *
     * @return name, or null if none
     */
    public String getParentName() {
        Spatial spatial = find();
        Spatial parent = spatial.getParent();
        String result;
        if (parent == null) {
            result = null;
        } else {
            result = parent.getName();
        }

        return result;
    }

    /**
     * Determine the type of the spatial's world bound.
     *
     * @return an enum value, or null if bound not set
     */
    public BoundingVolume.Type getWorldBoundType() {
        Spatial spatial = find();
        BoundingVolume bound = spatial.getWorldBound();
        BoundingVolume.Type result = null;
        if (bound != null) {
            result = bound.getType();
        }

        return result;
    }

    /**
     * Test whether the mesh is animated.
     *
     * @return true if it has an animated mesh, otherwise false
     */
    public boolean hasAnimatedMesh() {
        Mesh mesh = getMesh();
        if (mesh == null) {
            return false;
        } else {
            return MyMesh.isAnimated(mesh);
        }
    }

    /**
     * Test whether the Spatial has an indexed mesh.
     *
     * @return true if it has an indexed mesh, otherwise false
     */
    public boolean hasIndexedMesh() {
        Mesh mesh = getMesh();
        if (mesh == null) {
            return false;
        } else {
            return MyMesh.hasIndices(mesh);
        }
    }

    /**
     * Test whether the spatial has a material.
     *
     * @return true if it has a material, otherwise false
     */
    public boolean hasMaterial() {
        Material material = getMaterial();
        if (material == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether the spatial is a Geometry with a mesh.
     *
     * @return true if it has a mesh, otherwise false
     */
    public boolean hasMesh() {
        Mesh mesh = getMesh();
        if (mesh == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether the named material-parameter override exists in the spatial.
     *
     * @param parameterName the parameter name to search for (not null)
     * @return true if it exists, otherwise false
     */
    public boolean hasOverride(String parameterName) {
        Validate.nonNull(parameterName, "parameter name");

        MatParamOverride mpo = cgm.getOverride().find(parameterName);
        if (mpo == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether the spatial has both a SkeletonControl and an AnimControl or
     * both a SkinningControl and an AnimComposer.
     *
     * @return true if it has both, otherwise false
     */
    public boolean hasSkeletonControls() {
        Spatial spatial = find();

        AnimControl animControl = spatial.getControl(AnimControl.class);
        SkeletonControl skelControl = spatial.getControl(SkeletonControl.class);
        if (animControl != null && skelControl != null) {
            return true;
        }

        AnimComposer animComposer = spatial.getControl(AnimComposer.class);
        SkinningControl skinControl = spatial.getControl(SkinningControl.class);
        if (animComposer != null && skinControl != null) {
            return true;
        }

        return false;
    }

    /**
     * Test whether the specified user key exists in the spatial.
     *
     * @param key the key to search for (not null)
     * @return true if it exists, otherwise false
     */
    public boolean hasUserKey(String key) {
        Validate.nonNull(key, "key");

        Spatial spatial = find();
        Collection<String> keys = spatial.getUserDataKeys();
        boolean result = keys.contains(key);

        return result;
    }

    /**
     * Test whether the spatial has a vertex buffer of the specified type.
     *
     * @param bufferType which type of vertex buffer to test for (not null)
     * @return true if the buffer is set, otherwise false
     */
    public boolean hasVertexBuffer(VertexBuffer.Type bufferType) {
        Validate.nonNull(bufferType, "buffer type");

        boolean result = false;
        Mesh mesh = getMesh();
        if (mesh != null) {
            VertexBuffer buffer = mesh.getBuffer(bufferType);
            if (buffer != null) {
                result = true;
            }
        }

        return result;
    }

    /**
     * Test whether the spatial is the target of a spatial track in the loaded
     * animation.
     *
     * @return true if it's a target, otherwise false
     */
    public boolean isAnimationTarget() {
        Spatial spatial = find();
        Object track = cgm.getAnimation().findTrackForSpatial(spatial);
        if (track == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether the spatial is an attachments node.
     *
     * @return true if it's an attachments node, otherwise false
     */
    public boolean isAttachmentsNode() {
        boolean result = false;
        Spatial spatial = find();
        if (spatial instanceof Node) {
            Map<Bone, Spatial> map = cgm.mapAttachments();
            if (map.containsValue(spatial)) {
                result = true;
            }
        }

        return result;
    }

    /**
     * Test whether the spatial is the root of the loaded C-G model.
     *
     * @return true if it's the root, otherwise false
     */
    public boolean isCgmRoot() {
        boolean result = treePosition.isEmpty();
        return result;
    }

    /**
     * Test whether the indexed child of the selected spatial is a node.
     *
     * @param childIndex which child (&ge;0)
     * @return true if it's a node, otherwise false
     */
    public boolean isChildANode(int childIndex) {
        Validate.nonNegative(childIndex, "child index");

        boolean result = false;
        Spatial spatial = find();
        if (spatial instanceof Node) {
            Node node = (Node) spatial;
            Spatial child = node.getChild(childIndex);
            result = child instanceof Node;
        }

        return result;
    }

    /**
     * Test whether the spatial is a geometry.
     *
     * @return true if it's a geometry, otherwise false
     */
    public boolean isGeometry() {
        Spatial spatial = find();
        boolean result = spatial instanceof Geometry;

        return result;
    }

    /**
     * Test whether the spatial is a node.
     *
     * @return true if it's a node, otherwise false
     */
    public boolean isNode() {
        Spatial spatial = find();
        boolean result = spatial instanceof Node;

        return result;
    }

    /**
     * Test whether the spatial's local transform is an identity.
     *
     * @return true if identity transform, otherwise false
     */
    public boolean isTransformIdentity() {
        Spatial spatial = find();
        Transform localTransform = spatial.getLocalTransform();
        boolean result = MyMath.isIdentity(localTransform);

        return result;
    }

    /**
     * Test whether the spatial is a geometry with ignoreTransform set.
     *
     * @return true if ignoring its transform, otherwise false
     */
    public boolean isTransformIgnored() {
        Spatial spatial = find();
        boolean result = MySpatial.isIgnoringTransforms(spatial);

        return result;
    }

    /**
     * Enumerate data-bearing vertex buffers in the mesh whose descriptions have
     * the specified prefix.
     *
     * @param descPrefix (not null)
     * @return a new list of vertex buffer descriptions, in standard order, with
     * LoDs at the end
     */
    public List<String> listBufferDescs(String descPrefix) {
        List<String> result = new ArrayList<>(20);

        Mesh mesh = getMesh();
        if (mesh != null) {
            SafeArrayList<VertexBuffer> buffers = mesh.getBufferList();
            for (VertexBuffer buffer : buffers) {
                String desc = buffer.getBufferType().toString();
                if (desc.startsWith(descPrefix) && buffer.getData() != null) {
                    result.add(desc);
                }
            }

            int numLodLevels = mesh.getNumLodLevels();
            for (int iLevel = 0; iLevel < numLodLevels; iLevel++) {
                String desc = "LoD" + Integer.toString(iLevel);
                VertexBuffer buffer = mesh.getLodLevel(iLevel);
                if (desc.startsWith(descPrefix) && buffer.getData() != null) {
                    result.add(desc);
                }
            }
        }

        return result;
    }

    /**
     * Enumerate all geometries among the children of the selected Node.
     *
     * @return a new list of new items
     */
    public List<GeometryItem> listGeometryItems() {
        List<GeometryItem> result = new ArrayList<>(10);

        Spatial parent = find();
        if (parent instanceof Node) {
            Node node = (Node) parent;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                if (child instanceof Geometry) {
                    Geometry geometry = (Geometry) child;
                    GeometryItem item = new GeometryItem(geometry);
                    result.add(item);
                }
            }
        }

        return result;
    }

    /**
     * Enumerate all spatials that are not the selected Node, nor its ancestor,
     * nor among its immediate children.
     *
     * @return a new list of new items
     */
    public List<SpatialItem> listReparentItems() {
        List<SpatialItem> result = new ArrayList<>(50);

        Spatial selectedSpatial = find();
        if (selectedSpatial instanceof Node) {
            Node parent = (Node) selectedSpatial;
            Spatial rootCgm = cgm.getRootSpatial();
            List<Spatial> spatials = MySpatial.listSpatials(rootCgm);
            for (Spatial spatial : spatials) {
                if (spatial != selectedSpatial
                        && spatial.getParent() != selectedSpatial) {
                    if (spatial instanceof Node
                            && parent.hasAncestor((Node) spatial)) {
                        continue;
                    }
                    SpatialItem item = new SpatialItem(spatial);
                    result.add(item);
                }
            }
        }

        return result;
    }

    /**
     * Enumerate all parameters in the material whose names have the specified
     * prefix.
     *
     * @param namePrefix (not null)
     * @param whichParams which parameters to include in the list (not null)
     * @return a new list of parameter names, sorted lexicographically
     */
    public List<String> listMatParamNames(String namePrefix,
            WhichParams whichParams) {
        List<String> result = new ArrayList<>(20);

        Material material = getMaterial();
        if (material != null) {
            Collection<MatParam> definedParams = material.getParams();

            switch (whichParams) {
                case Defined:
                    for (MatParam param : definedParams) {
                        String name = param.getName();
                        if (name.startsWith(namePrefix)) {
                            result.add(name);
                        }
                    }
                    break;

                case Undefined:
                    MaterialDef def = material.getMaterialDef();
                    Collection<MatParam> allParams = def.getMaterialParams();
                    for (MatParam param : allParams) {
                        String name = param.getName();
                        if (name.startsWith(namePrefix)) {
                            result.add(name);
                        }
                    }
                    for (MatParam param : definedParams) {
                        String name = param.getName();
                        result.remove(name);
                    }
                    break;

                default:
                    throw new IllegalArgumentException(whichParams.toString());
            }

            Collections.sort(result);
        }

        return result;
    }

    /**
     * Enumerate all children of the spatial, numbering them to prevent
     * duplication.
     *
     * @return a new list of numbered names ordered by index
     */
    public List<String> listNumberedChildren() {
        int numChildren = countChildren();
        List<String> result = new ArrayList<>(numChildren);
        for (int childIndex = 0; childIndex < numChildren; childIndex++) {
            String name = getChildName(childIndex);
            String choice = String.format("%s [%d]", MyString.quote(name),
                    childIndex);
            result.add(choice);
        }

        return result;
    }

    /**
     * Enumerate the spatial's local material-parameter overrides.
     *
     * @return a new list of parameter names, sorted lexicographically
     */
    public List<String> listOverrideNames() {
        Spatial spatial = find();
        Collection<MatParamOverride> mpos = spatial.getLocalMatParamOverrides();
        int numMpos = mpos.size();
        List<String> result = new ArrayList<>(numMpos);
        for (MatParamOverride mpo : mpos) {
            String name = mpo.getName();
            result.add(name);
        }
        Collections.sort(result);

        return result;
    }

    /**
     * Enumerate the keys of the all the spatial's user data.
     *
     * @return a new list, sorted lexicographically
     */
    public List<String> listUserKeys() {
        Spatial spatial = find();
        Collection<String> keys = spatial.getUserDataKeys();
        List<String> result = new ArrayList<>(keys);
        Collections.sort(result);

        return result;
    }

    /**
     * Copy the local rotation of the spatial.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return local rotation (either storeResult or a new instance)
     */
    public Quaternion localRotation(Quaternion storeResult) {
        Quaternion result
                = (storeResult == null) ? new Quaternion() : storeResult;

        Spatial spatial = find();
        Quaternion rotation = spatial.getLocalRotation();
        result.set(rotation);

        return result;
    }

    /**
     * Copy the local scale of the spatial.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return local scale vector (either storeResult or a new instance)
     */
    public Vector3f localScale(Vector3f storeResult) {
        Vector3f result = (storeResult == null) ? new Vector3f() : storeResult;

        Spatial spatial = find();
        Vector3f scale = spatial.getLocalScale();
        result.set(scale);

        return result;
    }

    /**
     * Copy the local translation of the spatial.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return local translation vector (either storeResult or a new instance)
     */
    public Vector3f localTranslation(Vector3f storeResult) {
        Vector3f result = (storeResult == null) ? new Vector3f() : storeResult;

        Spatial spatial = find();
        Vector3f translation = spatial.getLocalTranslation();
        result.set(translation);

        return result;
    }

    /**
     * After successfully loading a C-G model, select the root of the model.
     */
    void postLoad() {
        selectCgmRoot();
    }

    /**
     * Select the specified tree position.
     *
     * @param pos which position (not null, unaffected)
     */
    public void select(List<Integer> pos) {
        Validate.nonNull(pos, "pos");

        treePosition.clear();
        treePosition.addAll(pos);
        postSelect();
    }

    /**
     * Select the specified spatial.
     *
     * @param newSpatial (not null)
     */
    void select(Spatial newSpatial) {
        assert newSpatial != null;

        List<Integer> position = cgm.findSpatial(newSpatial);
        assert position != null;
        treePosition = position;
        assert find() == newSpatial;
        postSelect();
    }

    /**
     * Select the named spatial.
     *
     * @param name (not null, not empty)
     */
    public void select(String name) {
        Validate.nonEmpty(name, "spatial name");

        List<Integer> position = cgm.findSpatialNamed(name);
        assert position != null;
        treePosition = position;
        assert find().getName().equals(name);
        postSelect();
    }

    /**
     * Select the attachments node of the selected bone.
     */
    public void selectAttachmentsNode() {
        Object bone = cgm.getBone().get();
        if (bone != null) {
            Node attachmentsNode = MaudUtil.getBoneAttachments(bone);
            if (attachmentsNode != null) {
                select(attachmentsNode);
            }
        }
    }

    /**
     * Select (by index) a child of the selected spatial.
     *
     * @param childIndex (&ge;0)
     */
    public void selectChild(int childIndex) {
        Validate.nonNegative(childIndex, "child index");

        Spatial child = modelChild(childIndex);
        if (child != null) {
            treePosition.add(childIndex);
            assert find() == child;
            postSelect();
        }
    }

    /**
     * Select the C-G model's root spatial.
     */
    public void selectCgmRoot() {
        treePosition.clear();
        assert find() == cgm.getRootSpatial();
        postSelect();
    }

    /**
     * Select the controlled spatial of the selected S-G control.
     */
    public void selectControlled() {
        Spatial controlled = cgm.getSgc().getControlled();
        if (controlled != null) {
            select(controlled);
        }
    }

    /**
     * Select the spatial that owns the selected light.
     */
    public void selectLightOwner() {
        Spatial owner = cgm.getLight().getOwner();
        if (owner != null) {
            select(owner);
            EditorTools.select("spatial");
        }
    }

    /**
     * Select a geometry that uses the specified material.
     *
     * @param material (not null)
     */
    void selectMaterial(Material material) {
        assert material != null;

        Spatial rootSpatial = cgm.getRootSpatial();
        List<Geometry> geoms
                = MySpatial.listMaterialUsers(rootSpatial, material, null);
        Geometry geometry = geoms.get(0);

        List<Integer> position = cgm.findSpatial(geometry);
        assert position != null;
        treePosition = position;
        postSelect();
    }

    /**
     * Select the next Geometry (in cyclical index order).
     */
    public void selectNextGeometry() {
        Spatial spatial = find();
        assert spatial instanceof Geometry;

        Spatial cgmRoot = cgm.getRootSpatial();
        List<Geometry> geomList
                = MySpatial.listSpatials(cgmRoot, Geometry.class, null);
        int oldIndex = geomList.indexOf(spatial);
        assert oldIndex != -1;
        int newIndex = oldIndex + 1;
        if (newIndex >= geomList.size()) {
            newIndex = 0;
        }

        Spatial newSpatial = geomList.get(newIndex);
        select(newSpatial);
    }

    /**
     * Select the parent of the selected spatial.
     */
    public void selectParent() {
        Spatial selectedSpatial = find();
        Node parent = selectedSpatial.getParent();
        if (parent != null) {
            int lastLevel = treePosition.size() - 1;
            treePosition.remove(lastLevel);
            assert find() == parent;
            postSelect();
        }
    }

    /**
     * Select the previous Geometry (in cyclical index order).
     */
    public void selectPreviousGeometry() {
        Spatial spatial = find();
        assert spatial instanceof Geometry;

        Spatial cgmRoot = cgm.getRootSpatial();
        List<Geometry> geomList
                = MySpatial.listSpatials(cgmRoot, Geometry.class, null);
        int oldIndex = geomList.indexOf(spatial);
        assert oldIndex != -1;
        int newIndex = oldIndex - 1;
        if (newIndex < 0) {
            newIndex = geomList.size() - 1;
        }

        Spatial newSpatial = geomList.get(newIndex);
        select(newSpatial);
    }

    /**
     * Select the indexed child of the spatial's parent.
     *
     * @param siblingIndex the position within the spatial's siblings (&ge;0)
     */
    public void selectSibling(int siblingIndex) {
        Validate.nonNegative(siblingIndex, "sibling index");

        Spatial spatial = find();
        Node parent = spatial.getParent();
        if (parent != null) { // not the C-G model root
            Spatial sibling = parent.getChild(siblingIndex);
            select(sibling);
        }
    }

    /**
     * Alter which C-G model contains the spatial. (Invoked only during
     * initialization and cloning.)
     *
     * @param newCgm (not null, aliases created)
     */
    void setCgm(Cgm newCgm) {
        assert newCgm != null;
        assert newCgm.getSpatial() == this;

        cgm = newCgm;
        if (newCgm instanceof EditableCgm) {
            editableCgm = (EditableCgm) newCgm;
        } else {
            editableCgm = null;
        }
    }

    /**
     * Determine the index of the Spatial among its parent's children.
     *
     * @return the index (&ge;0)
     */
    public int siblingIndex() {
        Spatial spatial = find();
        Node parent = spatial.getParent();

        int result;
        if (parent == null) { // the C-G model root is selected
            result = 0;
        } else {
            result = parent.getChildIndex(spatial);
        }

        assert result >= 0 : result;
        return result;
    }

    /**
     * Snap one axis-angle of the spatial's local rotation.
     *
     * @param axisIndex which axis: 0&rarr;X, 1&rarr;Y, 2&rarr;Z
     */
    public void snapRotation(int axisIndex) {
        Validate.inRange(axisIndex, "axis index", MyVector3f.firstAxis,
                MyVector3f.lastAxis);

        Quaternion localRotation = localRotation(null);
        MyQuaternion.snapLocal(localRotation, axisIndex);
        editableCgm.setSpatialRotation(localRotation);
    }

    /**
     * Test whether the selected subtree contains the specified spatial.
     *
     * @param input spatial to find (may be null, unaffected)
     * @return true if one the collected spatial was found, otherwise false
     */
    boolean subtreeContains(Spatial input) {
        boolean result;
        if (input == null) {
            result = false;
        } else {
            Spatial selectedSpatial = find();
            if (input == selectedSpatial) {
                result = true;
            } else if (input instanceof Node) {
                Node selectedNode = (Node) selectedSpatial;
                result = input.hasAncestor(selectedNode);
            } else {
                result = false;
            }
        }

        return result;
    }

    /**
     * Translate the selected Node and compensate by translating all its
     * children in the opposite direction.
     *
     * @param localOffset the displacement vector (in the parent's local
     * coordinates, not null, unaffected)
     */
    public void translateSmartNode(Vector3f localOffset) {
        Validate.nonNull(localOffset, "offset");

        Node node = (Node) find();
        List<Spatial> children = node.getChildren();
        int numChildren = children.size();
        Transform[] childTransforms = new Transform[numChildren];
        for (int i = 0; i < numChildren; ++i) {
            Spatial child = children.get(i);
            childTransforms[i] = child.getWorldTransform();
        }
        node.setLocalTranslation(localOffset);
        for (int i = 0; i < numChildren; ++i) {
            Spatial child = children.get(i);
            MySpatial.setWorldTransform(child, childTransforms[i]);
        }

        String positionString = node.toString();
        editableCgm.getEditState().setEditedSmartNodeTransform(positionString);
    }

    /**
     * Toggle the type of the geometry's model bound.
     */
    void toggleBoundType() {
        BoundingVolume.Type oldType = getWorldBoundType();
        BoundingVolume newBound = null;
        if (oldType == BoundingVolume.Type.AABB) {
            newBound = new BoundingSphere();
        } else if (oldType == BoundingVolume.Type.Sphere) {
            newBound = new BoundingBox();
        }

        Mesh mesh = getMesh();
        mesh.setBound(newBound);

        Spatial spatial = find();
        spatial.updateModelBound();

        Spatial modelRoot = cgm.getRootSpatial();
        modelRoot.updateGeometricState();

        SceneView sceneView = cgm.getSceneView();
        sceneView.setModelBound(newBound);
    }

    /**
     * If the selected Spatial has an indexed Mesh, expand it the Mesh ensure
     * that no vertex data are re-used. If it has a non-indexed Mesh, compress
     * the Mesh by introducing an index buffer.
     */
    public void toggleIndexedMesh() {
        Mesh mesh = getMesh();
        if (mesh != null) {
            boolean isIndexed = MyMesh.hasIndices(mesh);
            if (isIndexed) {
                Mesh expandedMesh = MyMesh.expand(mesh);
                editableCgm.setMesh(expandedMesh, "expand an indexed mesh");
            } else {
                Mesh indexedMesh = MyMesh.addIndices(mesh);
                editableCgm.setMesh(indexedMesh, "add indices to a mesh");
            }
        }
    }

    /**
     * Determine the depth of the selected spatial in the C-G model.
     *
     * @return the level (&ge;0, 0 &rarr; model root)
     */
    public int treeLevel() {
        int result = treePosition.size();
        return result;
    }

    /**
     * Access the selected spatial in the specified C-G model.
     *
     * @param cgmRoot root of the C-G model (not null)
     * @return the pre-existing instance (not null)
     */
    public Spatial underRoot(Spatial cgmRoot) {
        Validate.nonNull(cgmRoot, "root spatial");

        Spatial result = cgmRoot;
        for (int childPosition : treePosition) {
            Node node = (Node) result;
            result = node.getChild(childPosition);
        }

        assert result != null;
        return result;
    }
    // *************************************************************************
    // JmeCloneable methods

    /**
     * Callback from {@link com.jme3.util.clone.Cloner} to convert this
     * shallow-cloned instance into a deep-cloned one, using the specified
     * cloner and original to resolve copied fields.
     *
     * @param cloner the cloner currently cloning this control (not null)
     * @param original the instance from which this instance was shallow-cloned
     * (unused)
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        last = cloner.clone(last);
        treePosition = new ArrayList<>(treePosition);
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public SelectedSpatial jmeClone() {
        try {
            SelectedSpatial clone = (SelectedSpatial) super.clone();
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException(exception);
        }
    }
    // *************************************************************************
    // Object methods

    /**
     * Don't use this method; use a {@link com.jme3.util.clone.Cloner} instead.
     *
     * @return never
     * @throws CloneNotSupportedException always
     */
    @Override
    public SelectedSpatial clone() throws CloneNotSupportedException {
        super.clone();
        throw new CloneNotSupportedException("use a cloner");
    }

    /**
     * Represent the selected spatial as a text string. TODO starting index?
     *
     * @return descriptive string of text (not null)
     */
    @Override
    public String toString() {
        return treePosition.toString();
    }
    // *************************************************************************
    // private methods

    /**
     * Test whether the specified Spatial is a Geometry whose Material defines
     * the selected parameter.
     */
    private boolean definesSelectedMatParam(Spatial newSpatial) {
        boolean result = false;
        if (newSpatial instanceof Geometry) {
            Material material = ((Geometry) newSpatial).getMaterial();
            if (material != null) {
                String parameterName = cgm.getMatParam().getName();
                if (material.getParam(parameterName) != null) {
                    result = true;
                }
            }
        }

        return result;
    }

    /**
     * Access the described buffer.
     *
     * @param description which buffer (not null)
     * @return the pre-existing instance, or null if not found
     */
    private VertexBuffer findBuffer(String description) {
        assert description != null;

        VertexBuffer result = null;
        if (hasMesh() && !noBuffer.equals(description)) {
            Mesh mesh = cgm.getSpatial().getMesh();
            if (description.startsWith("LoD")) {
                String lodText = MyString.removeSuffix(description, "LoD");
                int level = Integer.parseInt(lodText);
                result = mesh.getLodLevel(level);
            } else {
                VertexBuffer.Type type = VertexBuffer.Type.valueOf(description);
                result = mesh.getBuffer(type);
            }
        }

        return result;
    }

    /**
     * Access (by index) a child of the selected spatial in the MVC model.
     *
     * @param childIndex which child (&ge;0)
     * @return the pre-existing instance, or null if none
     */
    private Spatial modelChild(int childIndex) {
        assert childIndex >= 0 : childIndex;

        Spatial parent = find();
        Spatial child;
        if (parent instanceof Node) {
            Node node = (Node) parent;
            child = node.getChild(childIndex);
        } else {
            child = null;
        }

        return child;
    }

    /**
     * Invoked after selecting a spatial.
     */
    private void postSelect() {
        Spatial found = find();
        if (found != last) {
            cgm.getBuffer().deselect();
            if (!definesSelectedMatParam(found)) {
                cgm.getMatParam().deselect();
            }
            cgm.getOverride().deselect();
            cgm.getUserData().deselect();
            cgm.getVertex().deselect();
            last = found;
        }
    }
}
