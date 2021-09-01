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
package maud.view.scene;

import com.jme3.bounding.BoundingVolume;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.animation.DynamicAnimControl;
import com.jme3.bullet.animation.PhysicsLink;
import com.jme3.bullet.animation.RangeOfMotion;
import com.jme3.bullet.control.PhysicsControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.input.InputManager;
import com.jme3.light.Light;
import com.jme3.material.MatParamOverride;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.Line;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.shader.VarType;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Heart;
import jme3utilities.MyCamera;
import jme3utilities.MyLight;
import jme3utilities.MySpatial;
import jme3utilities.Validate;
import jme3utilities.debug.AxesVisualizer;
import jme3utilities.math.MyVector3f;
import jme3utilities.minie.MyControlP;
import maud.Maud;
import maud.PhysicsUtil;
import maud.model.cgm.Cgm;
import maud.model.cgm.SelectedRagdoll;

/**
 * An editor view containing a 3-D visualization of a loaded C-G model.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SceneView extends SceneViewCore {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SceneView.class.getName());
    /**
     * local copy of {@link com.jme3.math.Transform#IDENTITY}
     */
    final private static Transform transformIdentity = new Transform();
    // *************************************************************************
    // constructors

    /**
     * Instantiate a new view.
     *
     * @param ownerCgm C-G model that will own this view (not null, aliases
     * created)
     * @param parentNode attachment point in the scene graph (not null, alias
     * created)
     * @param port1 initial main view port, or null for none (alias created)
     * @param port2 main view port to use after the screen is split (not null,
     * alias created)
     * @param overlayRoot root node of the overlay scene graph (not null, alias
     * created)
     */
    public SceneView(Cgm ownerCgm, Node parentNode, ViewPort port1,
            ViewPort port2, Node overlayRoot) {
        super(ownerCgm, parentNode, port1, port2, overlayRoot);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Add a clone of the specified light to the selected spatial.
     *
     * @param light the light to clone (not null, unaffected)
     */
    public void addLight(Light light) {
        Light cloneLight = light.clone();
        Spatial selectedSpatial = selectedSpatial();
        selectedSpatial.addLight(cloneLight);
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

        Spatial selectedSpatial = selectedSpatial();
        MatParamOverride newMpo
                = new MatParamOverride(varType, parameterName, null);
        selectedSpatial.addMatParamOverride(newMpo);
    }

    /**
     * Add a copy of the specified PhysicsControl to the selected Spatial and
     * also to the view's PhysicsSpace.
     *
     * @param physicsControl (not null, unaffected)
     * @return the new PhysicsControl for this view (not null)
     */
    public PhysicsControl addPhysicsControl(PhysicsControl physicsControl) {
        logger.log(Level.INFO, "");
        Validate.nonNull(physicsControl, "physics control");

        PhysicsControl copy = Heart.deepCopy(physicsControl);
        if (copy instanceof RigidBodyControl) {
            /*
             * Force kinematic mode for visualization purposes.
             */
            RigidBodyControl rbc = (RigidBodyControl) copy;
            rbc.setKinematic(true);
            rbc.setKinematicSpatial(true);
        }
        Spatial spatial = selectedSpatial();
        spatial.addControl(copy);

        PhysicsSpace space = getPhysicsSpace();
        copy.setPhysicsSpace(space);

        return copy;
    }

    /**
     * Apply the local Transform of the selected Spatial (and those of its
     * descendents) to each of its meshes.
     */
    public void applyTransform() {
        Spatial subtree = selectedSpatial();
        List<Spatial> spatials = MySpatial.listSpatials(subtree);
        for (Spatial spatial : spatials) {
            spatial.setLocalTransform(transformIdentity);
        }
        // TODO transform skinning data?
    }

    /**
     * Add an AttachmentLink for the named bone and the specified model to the
     * selected DynamicAnimControl.
     *
     * @param boneName the name of the bone to add (not null, not empty)
     * @param child spatial to clone (not null, unaffected)
     */
    public void attachBone(String boneName, Spatial child) {
        Validate.nonEmpty(boneName, "bone name");

        Spatial clone = child.clone();

        DynamicAnimControl dac = getSelectedRagdoll();
        Spatial controlledSpatial = dac.getSpatial();
        controlledSpatial.removeControl(dac);
        dac.attach(boneName, 1f, clone);
        controlledSpatial.addControl(dac);
    }

    /**
     * Attach a clone of the specified child spatial to the specified node in
     * the C-G model.
     *
     * @param parentPosition tree position of parent node (not null, unaffected)
     * @param child spatial to clone (not null, unaffected)
     */
    public void attachSpatial(List<Integer> parentPosition, Spatial child) {
        Validate.nonNull(parentPosition, "parent position");

        Spatial clone = child.clone();
        /*
         * Remove all scene-graph controls except those concerned with physics.
         * Enable those S-G controls and configure their physics spaces so that
         * the BulletDebugAppState will render their collision shapes.
         */
        MyControlP.removeNonPhysicsControls(clone);
        PhysicsSpace space = getPhysicsSpace();
        MyControlP.enablePhysicsControls(clone, space);
        /*
         * Attach the child to the scene.
         */
        Node parentNode = (Node) getCgmRoot();
        for (int childIndex : parentPosition) {
            Spatial parentSpatial = parentNode.getChild(childIndex);
            parentNode = (Node) parentSpatial;
        }
        parentNode.attachChild(clone);
    }

    /**
     * Calculate the length of the specified axis.
     *
     * @param axisIndex which axis: 0&rarr;X, 1&rarr;Y, 2&rarr;Z
     * @return length (in local units, &ge;0)
     */
    public float axisLength(int axisIndex) {
        Validate.inRange(axisIndex, "axis index", MyVector3f.firstAxis,
                MyVector3f.lastAxis);

        AxesVisualizer visualizer = getAxesVisualizer();
        Spatial spatial = visualizer.getSpatial();
        Vector3f tipWorld = visualizer.tipLocation(axisIndex);
        assert !MySpatial.isIgnoringTransforms(spatial);
        Vector3f tipLocal = spatial.worldToLocal(tipWorld, null);
        float length = tipLocal.length();

        assert length >= 0f : length;
        return length;
    }

    /**
     * Delete the selected buffer (which must be a mapped buffer).
     */
    public void deleteBuffer() {
        VertexBuffer buffer = findBuffer();
        VertexBuffer.Type type = buffer.getBufferType();

        Spatial spatial = selectedSpatial();
        Geometry geometry = (Geometry) spatial;
        Mesh mesh = geometry.getMesh();

        mesh.clearBuffer(type);
        if (type == VertexBuffer.Type.BoneIndex) {
            mesh.clearBuffer(VertexBuffer.Type.HWBoneIndex);
        }
    }

    /**
     * Clear the selected material parameter.
     */
    public void deleteMatParam() {
        Material material = getSelectedMaterial();
        String parameterName = getCgm().getMatParam().getName();
        material.clearParam(parameterName);
    }

    /**
     * Delete the selected material-parameter override.
     */
    public void deleteOverride() {
        Spatial spatial = selectedSpatial();
        MatParamOverride mpo = findSelectedMpo();
        spatial.removeMatParamOverride(mpo);
    }

    /**
     * Delete the selected spatial and its descendents, if any.
     */
    public void deleteSubtree() {
        Spatial spatial = selectedSpatial();
        MyControlP.disablePhysicsControls(spatial);
        boolean success = spatial.removeFromParent();
        assert success;
    }

    /**
     * Delete the specified spatial and its descendents, if any.
     *
     * @param treePosition tree position of spatial to delete (not null)
     */
    public void deleteSubtree(List<Integer> treePosition) {
        Validate.nonNull(treePosition, "tree position");

        Spatial spatial = getCgmRoot();
        for (int childPosition : treePosition) {
            Node node = (Node) spatial;
            spatial = node.getChild(childPosition);
        }

        MyControlP.disablePhysicsControls(spatial);
        boolean success = spatial.removeFromParent();
        assert success;
    }

    /**
     * When dragging an axis, update the subject in the MVC model.
     */
    public void dragAxis() {
        AxesVisualizer axesVisualizer = getAxesVisualizer();
        assert axesVisualizer.isEnabled();
        assert getCgm().isLoaded();

        Camera camera = getCamera();
        InputManager inputManager = Maud.getApplication().getInputManager();
        Line worldLine = MyCamera.mouseLine(camera, inputManager);
        SceneDrag.updateSubject(axesVisualizer, worldLine);
    }

    /**
     * Test whether the indexed axis points toward or away from the camera.
     *
     * @param axisIndex which axis: 0&rarr;X, 1&rarr;Y, 2&rarr;Z
     * @return true if pointing away, otherwise false
     */
    public boolean isAxisReceding(int axisIndex) {
        Validate.inRange(axisIndex, "axis index", MyVector3f.firstAxis,
                MyVector3f.lastAxis);

        AxesVisualizer axesVisualizer = getAxesVisualizer();
        assert axesVisualizer.isEnabled();
        Spatial axesSpatial = axesVisualizer.getSpatial();
        /*
         * Calculate distances to the tip and tail of the axis arrow.
         */
        assert !MySpatial.isIgnoringTransforms(axesSpatial);
        Vector3f tailLocation = axesSpatial.getWorldTranslation();
        Vector3f tipLocation = axesVisualizer.tipLocation(axisIndex);
        Vector3f cameraLocation = getPov().location(null);
        float tailDS = cameraLocation.distanceSquared(tailLocation);
        float tipDS = cameraLocation.distanceSquared(tipLocation);
        if (tipDS > tailDS) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Remove an existing physics control from the scene graph and the physics
     * space.
     *
     * @param treePosition tree position of the controlled spatial (not null,
     * unaffected)
     * @param pcPosition index among all physics controls added to the
     * controlled spatial (&ge;0)
     */
    public void removePhysicsControl(List<Integer> treePosition,
            int pcPosition) {
        logger.log(Level.INFO, "");
        Validate.nonNull(treePosition, "tree position");
        Validate.nonNegative(pcPosition, "control position");

        Spatial spatial = findSpatial(treePosition);
        PhysicsControl pc = PhysicsUtil.pcFromPosition(spatial, pcPosition);
        pc.setEnabled(false);
        boolean success = spatial.removeControl(pc);
        assert success;
    }

    /**
     * Rename the selected material-parameter override.
     *
     * @param newName (not null, not empty)
     */
    public void renameOverride(String newName) {
        Validate.nonEmpty(newName, "new name");

        Spatial spatial = selectedSpatial();
        MatParamOverride oldMpo = findSelectedMpo();

        Object value = oldMpo.getValue();
        VarType varType = oldMpo.getVarType();
        MatParamOverride newMpo = new MatParamOverride(varType, newName, value);
        boolean enabled = oldMpo.isEnabled();
        newMpo.setEnabled(enabled);

        spatial.addMatParamOverride(newMpo);
        spatial.removeMatParamOverride(oldMpo);
    }

    /**
     * Remove the named light, and optionally replace it with a clone of the
     * specified light.
     *
     * @param name the name of the light to remove/replace (not null)
     * @param light a replacement light (unaffected) if null, the existing light
     * is simply removed
     */
    public void replaceLight(String name, Light light) {
        Validate.nonNull(name, "name");

        Spatial cgmRoot = getCgmRoot();
        Light oldLight = MyLight.findLight(name, cgmRoot);
        assert oldLight != null;
        Spatial owner = MyLight.findOwner(oldLight, cgmRoot);
        owner.removeLight(oldLight);
        if (light != null) {
            Light cloneLight = light.clone();
            owner.addLight(cloneLight);
        }
    }

    /**
     * Alter the indexed float in the selected FloatBuffer.
     *
     * @param floatIndex which float to modify
     * @param newValue the desired value
     */
    public void putFloat(int floatIndex, float newValue) {
        Validate.nonNegative(floatIndex, "float index");

        VertexBuffer vertexBuffer = findBuffer();
        FloatBuffer floatBuffer = (FloatBuffer) vertexBuffer.getData();
        floatBuffer.put(floatIndex, newValue);
        vertexBuffer.updateData(floatBuffer);
    }

    /**
     * Alter whether the indexed physics control applies to its spatial's local
     * translation.
     *
     * @param treePosition tree position of the controlled spatial (not null,
     * unaffected)
     * @param pcPosition index among all physics controls added to the
     * controlled spatial (&ge;0)
     * @param newSetting true&rarr;apply to local, false&rarr;apply to world
     */
    public void setApplyPhysicsLocal(List<Integer> treePosition, int pcPosition,
            boolean newSetting) {
        Validate.nonNull(treePosition, "tree position");
        Validate.nonNegative(pcPosition, "control position");

        PhysicsControl pc = findPhysicsControl(treePosition, pcPosition);
        assert MyControlP.canApplyPhysicsLocal(pc);
        MyControlP.setApplyPhysicsLocal(pc, newSetting);
    }

    /**
     * Alter the instance span of the selected buffer.
     *
     * @param newSpan 0 &rarr; not instanced, 1 &rarr; each element goes with
     * one instance, etc.
     */
    public void setBufferInstanceSpan(int newSpan) {
        Validate.nonNegative(newSpan, "new span");

        VertexBuffer buffer = findBuffer();
        buffer.setInstanceSpan(newSpan);
    }

    /**
     * Alter the limit of the selected buffer.
     *
     * @param newLimit new value for limit (&ge;0)
     */
    public void setBufferLimit(int newLimit) {
        Validate.nonNegative(newLimit, "new limit");

        VertexBuffer buffer = findBuffer();
        Buffer data = buffer.getData();
        data.limit(newLimit);
    }

    /**
     * Alter the normalized flag of the selected buffer.
     *
     * @param newSetting true&rarr;normalized, false&rarr;not normalized
     */
    public void setBufferNormalized(boolean newSetting) {
        VertexBuffer buffer = findBuffer();
        buffer.setNormalized(newSetting);
    }

    /**
     * Alter the stride of the selected buffer.
     *
     * @param newStride new value for stride (&ge;0)
     */
    public void setBufferStride(int newStride) {
        Validate.nonNegative(newStride, "new stride");

        VertexBuffer buffer = findBuffer();
        buffer.setStride(newStride);
    }

    /**
     * Alter the usage of the selected buffer.
     *
     * @param newUsage new value for usage (not null)
     */
    public void setBufferUsage(VertexBuffer.Usage newUsage) {
        Validate.nonNull(newUsage, "new usage");

        VertexBuffer buffer = findBuffer();
        buffer.setUsage(newUsage);
    }

    /**
     * Alter the cull hint of the specified Spatial.
     *
     * @param treePosition tree position of the Spatial to modify (not null,
     * unaffected)
     * @param newHint new value for cull hint (not null)
     */
    public void setCullHint(List<Integer> treePosition,
            Spatial.CullHint newHint) {
        Validate.nonNull(treePosition, "tree position");
        Validate.nonNull(newHint, "cull hint");

        Spatial spatial = getCgmRoot();
        for (int childPosition : treePosition) {
            Node node = (Node) spatial;
            spatial = node.getChild(childPosition);
        }
        spatial.setCullHint(newHint);
    }

    /**
     * Alter the depth-test setting of the selected material.
     *
     * @param newState true &rarr; enable test, false &rarr; disable it
     */
    public void setDepthTest(boolean newState) {
        Material material = getSelectedMaterial();
        RenderState modelState = material.getAdditionalRenderState();
        modelState.setDepthTest(newState);
    }

    /**
     * Alter the face-cull mode of the selected material.
     *
     * @param newMode desired mode (not null)
     */
    public void setFaceCullMode(RenderState.FaceCullMode newMode) {
        Validate.nonNull(newMode, "new mode");

        Material material = getSelectedMaterial();
        RenderState modelState = material.getAdditionalRenderState();
        modelState.setFaceCullMode(newMode);
    }

    /**
     * Alter whether the selected geometry ignores its transform.
     *
     * @param newSetting true&rarr;ignore transform, false&rarr;apply transform
     */
    public void setIgnoreTransform(boolean newSetting) {
        Geometry geometry = (Geometry) selectedSpatial();
        geometry.setIgnoreTransform(newSetting);
    }

    /**
     * Alter the mass of the named physics link.
     *
     * @param linkName name of the link (not null, not empty)
     * @param mass (&gt;0)
     */
    public void setLinkMass(String linkName, float mass) {
        Validate.nonEmpty(linkName, "link name");

        DynamicAnimControl dac = getSelectedRagdoll();
        PhysicsLink link = dac.findLink(linkName);
        dac.setMass(link, mass);
    }

    /**
     * Apply the specified material to the selected spatial.
     *
     * @param modelMaterial MVC model's material (not null, unaffected)
     */
    public void setMaterial(Material modelMaterial) {
        assert modelMaterial != null;

        Spatial spatial = selectedSpatial();
        Geometry geometry = (Geometry) spatial;
        Material newMaterial = modelMaterial.clone();
        geometry.setMaterial(newMaterial);
    }

    /**
     * Apply the specified Mesh to the selected Geometry.
     *
     * @param modelMesh the MVC model's Mesh (not null, unaffected)
     */
    public void setMesh(Mesh modelMesh) {
        assert modelMesh != null;

        Spatial spatial = selectedSpatial();
        Geometry geometry = (Geometry) spatial;
        Mesh newMesh = modelMesh.deepClone();
        geometry.setMesh(newMesh);
    }

    /**
     * Alter the mode of the selected mesh.
     *
     * @param newMode new value for mode (not null)
     */
    public void setMeshMode(Mesh.Mode newMode) {
        Validate.nonNull(newMode, "new mode");

        Spatial spatial = selectedSpatial();
        Geometry geometry = (Geometry) spatial;
        Mesh mesh = geometry.getMesh();
        mesh.setMode(newMode);
    }

    /**
     * Alter the maximum number of weights per vertex in the selected mesh.
     *
     * @param newLimit new number (&ge;0, &le;4)
     */
    public void setMeshWeights(int newLimit) {
        Validate.inRange(newLimit, "new limit", 0, 4);

        Spatial spatial = selectedSpatial();
        Geometry geometry = (Geometry) spatial;
        Mesh mesh = geometry.getMesh();
        mesh.setMaxNumWeights(newLimit);
    }

    /**
     * Alter the model bound of the selected spatial.
     *
     * @param modelBound object for model bound (not null, unaffected)
     */
    public void setModelBound(BoundingVolume modelBound) {
        Validate.nonNull(modelBound, "model bound");

        Spatial spatial = selectedSpatial();
        BoundingVolume newBound = modelBound.clone();
        spatial.setModelBound(newBound);
    }

    /**
     * Alter whether the selected material-parameter override is enabled.
     *
     * @param newSetting true&rarr;enable, false&rarr;disable
     */
    public void setOverrideEnabled(boolean newSetting) {
        MatParamOverride mpo = findSelectedMpo();
        mpo.setEnabled(newSetting);
    }

    /**
     * Alter the value of the named M-P override in the specified spatial.
     *
     * @param treePosition the tree position of the spatial (not null,
     * unaffected)
     * @param parameterName the parameter name (not null, not empty)
     * @param varType type of parameter (not null)
     * @param viewValue the desired value (may be null, alias created)
     */
    public void setOverrideValue(List<Integer> treePosition, String parameterName,
            VarType varType, Object viewValue) {
        Validate.nonNull(treePosition, "treePosition");
        Validate.nonEmpty(parameterName, "parameter name");
        Validate.nonNull(varType, "var type");

        Spatial spatial = getCgmRoot();
        for (int childPosition : treePosition) {
            Node node = (Node) spatial;
            spatial = node.getChild(childPosition);
        }

        MatParamOverride oldMpo
                = MySpatial.findOverride(spatial, parameterName);
        assert oldMpo != null;

        spatial.removeMatParamOverride(oldMpo);

        MatParamOverride newMpo
                = new MatParamOverride(varType, parameterName, viewValue);
        boolean enabled = oldMpo.isEnabled();
        newMpo.setEnabled(enabled);
        spatial.addMatParamOverride(newMpo);
    }

    /**
     * Alter the value of the named material parameter in the specified spatial.
     *
     * @param treePosition the tree position of a spatial that uses the material
     * (not null, unaffected)
     * @param parameterName the parameter name (not null, not empty)
     * @param varType type of parameter (not null)
     * @param viewValue the desired value (not null, alias created)
     */
    public void setParamValue(List<Integer> treePosition, String parameterName,
            VarType varType, Object viewValue) {
        Validate.nonNull(treePosition, "treePosition");
        Validate.nonEmpty(parameterName, "parameter name");
        Validate.nonNull(varType, "var type");
        Validate.nonNull(viewValue, "view value");

        Spatial spatial = getCgmRoot();
        for (int childPosition : treePosition) {
            Node node = (Node) spatial;
            spatial = node.getChild(childPosition);
        }

        Geometry geometry = (Geometry) spatial;
        Material material = geometry.getMaterial();
        material.setParam(parameterName, varType, viewValue);
    }

    /**
     * Alter whether the specified physics control is enabled.
     *
     * @param treePosition tree position of the controlled spatial (not null,
     * unaffected)
     * @param pcPosition index among all physics controls added to the
     * controlled spatial (&ge;0)
     * @param newSetting true&rarr;enable, false&rarr;disable
     */
    public void setPhysicsControlEnabled(List<Integer> treePosition,
            int pcPosition, boolean newSetting) {
        Validate.nonNull(treePosition, "tree position");
        Validate.nonNegative(pcPosition, "control position");

        PhysicsControl pc = findPhysicsControl(treePosition, pcPosition);
        pc.setEnabled(newSetting);
    }

    /**
     * Alter the queue bucket of the specified Spatial.
     *
     * @param treePosition the tree position of the Spatial to modify (not null,
     * unaffected)
     * @param newBucket the new value for queue bucket (not null)
     */
    public void setQueueBucket(List<Integer> treePosition,
            RenderQueue.Bucket newBucket) {
        Validate.nonNull(treePosition, "tree position");
        Validate.nonNull(newBucket, "new bucket");

        Spatial spatial = getCgmRoot();
        for (int childPosition : treePosition) {
            Node node = (Node) spatial;
            spatial = node.getChild(childPosition);
        }

        spatial.setQueueBucket(newBucket);
    }

    /**
     * Alter the range of motion of the named linked bone.
     *
     * @param boneName the name of the linked bone (not null, not empty)
     * @param newRom the new range of motion (not null)
     */
    public void setRangeOfMotion(String boneName, RangeOfMotion newRom) {
        Validate.nonEmpty(boneName, "bone name");
        Validate.nonNull(newRom, "new range of motion");

        DynamicAnimControl dac = getSelectedRagdoll();
        dac.setJointLimits(boneName, newRom);
    }

    /**
     * Alter the shadow mode of the selected spatial.
     *
     * @param newMode new value for shadow mode (not null)
     */
    public void setShadowMode(RenderQueue.ShadowMode newMode) {
        Validate.nonNull(newMode, "shadow mode");

        Spatial spatial = selectedSpatial();
        spatial.setShadowMode(newMode);
    }

    /**
     * Alter the wireframe setting for the specified geometry.
     *
     * @param treePosition position of the geometry with respect to the C-G
     * model's root spatial (not null)
     * @param newSetting true&rarr;edges only, false&rarr;solid triangles
     */
    public void setWireframe(List<Integer> treePosition, boolean newSetting) {
        Validate.nonNull(treePosition, "tree position");

        Spatial spatial = getCgmRoot();
        for (int childPosition : treePosition) {
            Node node = (Node) spatial;
            spatial = node.getChild(childPosition);
        }
        Geometry geometry = (Geometry) spatial;
        Material material = geometry.getMaterial();
        RenderState renderState = material.getAdditionalRenderState();
        renderState.setWireframe(newSetting);
    }

    /**
     * Delete the AttachmentLink for the named bone.
     *
     * @param boneName name of the associated bone (not null, not empty)
     */
    public void unlinkAttachment(String boneName) {
        Validate.nonEmpty(boneName, "bone name");

        DynamicAnimControl dac = getSelectedRagdoll();
        dac.detach(boneName);
    }

    /**
     * Delete the BoneLink for the named bone.
     *
     * @param boneName name of the bone (not null, not empty)
     */
    public void unlinkBone(String boneName) {
        Validate.nonEmpty(boneName, "bone name");

        DynamicAnimControl dac = getSelectedRagdoll();
        dac.unlinkBone(boneName);
    }

    /**
     * Copy the world transform of the C-G model, based on an animated geometry
     * if possible.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return the world transform (either storeResult or a new instance)
     */
    public Transform worldTransform(Transform storeResult) {
        Transform result
                = (storeResult == null) ? new Transform() : storeResult;

        Spatial transformSpatial = findTransformSpatial();
        if (MySpatial.isIgnoringTransforms(transformSpatial)) {
            result.loadIdentity();
        } else {
            Transform alias = transformSpatial.getWorldTransform();
            result.set(alias);
        }

        return result;
    }
    // *************************************************************************
    // SceneViewCore methods

    /**
     * Don't use this method; use a {@link com.jme3.util.clone.Cloner} instead.
     *
     * @return never
     * @throws CloneNotSupportedException always
     */
    @Override
    public SceneView clone() throws CloneNotSupportedException {
        super.clone();
        throw new CloneNotSupportedException("use a cloner");
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public SceneView jmeClone() {
        return (SceneView) super.jmeClone();
    }
    // *************************************************************************
    // private methods

    /**
     * Find a physics control specified by positional indices.
     *
     * @param treePosition tree position of the controlled spatial (not null,
     * unaffected)
     * @param pcPosition position among the physics controls added to the
     * controlled spatial (&ge;0)
     * @return the pre-existing physics control
     */
    private PhysicsControl findPhysicsControl(List<Integer> treePosition,
            int pcPosition) {
        assert treePosition != null;
        assert pcPosition >= 0 : pcPosition;

        Spatial spatial = findSpatial(treePosition);
        PhysicsControl pc = PhysicsUtil.pcFromPosition(spatial, pcPosition);

        return pc;
    }

    /**
     * Access the selected material in this view's copy of its C-G model.
     *
     * @return the pre-existing material (not null)
     */
    private Material getSelectedMaterial() {
        Spatial spatial = selectedSpatial();
        Geometry geometry = (Geometry) spatial;
        Material material = geometry.getMaterial();

        assert material != null;
        return material;
    }

    /**
     * Access the selected ragdoll in this view's copy of its C-G model.
     *
     * @return the pre-existing control (not null)
     */
    private DynamicAnimControl getSelectedRagdoll() {
        SelectedRagdoll ragdoll = getCgm().getRagdoll();
        List<Integer> treePosition = ragdoll.treePosition();
        int pcPosition = ragdoll.pcPosition();
        PhysicsControl pc = findPhysicsControl(treePosition, pcPosition);
        DynamicAnimControl dac = (DynamicAnimControl) pc;

        assert dac != null;
        return dac;
    }
}
