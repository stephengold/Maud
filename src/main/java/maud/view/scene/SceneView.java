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
package maud.view.scene;

import com.jme3.bounding.BoundingVolume;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.CollisionShape;
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
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.shader.VarType;
import com.jme3.util.clone.Cloner;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import jme3utilities.MyCamera;
import jme3utilities.MyLight;
import jme3utilities.MySpatial;
import jme3utilities.Validate;
import jme3utilities.debug.AxesVisualizer;
import jme3utilities.debug.SkeletonVisualizer;
import jme3utilities.math.MyVector3f;
import jme3utilities.minni.MyControlP;
import jme3utilities.minni.MyObject;
import maud.Maud;
import maud.PhysicsUtil;
import maud.model.cgm.Cgm;
import maud.model.cgm.SelectedMatParam;

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
    // *************************************************************************
    // constructors

    /**
     * Instantiate a new view.
     *
     * @param ownerCgm C-G model that will own this view (not null, aliases
     * created)
     * @param parentNode attachment point in the scene graph (not null, alias
     * created)
     * @param port1 initial view port, or null for none (alias created)
     * @param port2 view port to use after the screen is split (not null, alias
     * created)
     */
    public SceneView(Cgm ownerCgm, Node parentNode, ViewPort port1,
            ViewPort port2) {
        super(ownerCgm, parentNode, port1, port2);
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
     * Add a new material-parameter override to the selected spatial.
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
     * Add a copy of the specified physics control to the selected spatial and
     * the view's physics space.
     *
     * @param physicsControl (not null, unaffected)
     */
    public void addPhysicsControl(PhysicsControl physicsControl) {
        Validate.nonNull(physicsControl, "physics control");

        PhysicsControl copy = Cloner.deepClone(physicsControl);
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
        space.add(copy);
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

        SkeletonVisualizer skeletonVisualizer = getSkeletonVisualizer();
        Spatial controlled = null;
        if (skeletonVisualizer != null) {
            /*
             * Temporarily detach the skeleton visualizer from the scene.
             */
            controlled = skeletonVisualizer.getSpatial();
            controlled.removeControl(skeletonVisualizer);
        }
        /*
         * Attach the child to the scene.
         */
        Node parentNode = (Node) getCgmRoot();
        for (int childIndex : parentPosition) {
            Spatial parentSpatial = parentNode.getChild(childIndex);
            parentNode = (Node) parentSpatial;
        }
        parentNode.attachChild(clone);

        if (controlled != null) {
            /*
             * Re-attach the skeleton visualizer.
             */
            controlled.addControl(skeletonVisualizer);
        }
    }

    /**
     * Clear the selected material parameter.
     */
    public void deleteMatParam() {
        Material material = selectedMaterial();
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
     * Enumerate all collision objects in the physics space, excluding those
     * added by this view.
     *
     * @return a new map from ids to objects
     */
    public Map<Long, PhysicsCollisionObject> objectMap() {
        PhysicsSpace space = getPhysicsSpace();
        Map<Long, PhysicsCollisionObject> result = PhysicsUtil.objectMap(space);

        Set<Long> viewIds = listIds(null);
        for (long id : viewIds) {
            result.remove(id);
        }

        return result;
    }

    /**
     * Determine the name of the object associated with the indexed physics
     * control.
     *
     * @param treePosition tree position of the controlled spatial (not null,
     * unaffected)
     * @param pcPosition position among the physics controls added to the
     * controlled spatial (&ge;0)
     * @return name (not null, not empty)
     */
    public String objectName(List<Integer> treePosition, int pcPosition) {
        Validate.nonNull(treePosition, "tree position");
        Validate.nonNegative(pcPosition, "control position");

        PhysicsControl pc = findPhysicsControl(treePosition, pcPosition);
        PhysicsCollisionObject pco = (PhysicsCollisionObject) pc;
        String result = MyObject.objectName(pco);

        return result;
    }

    /**
     * Remove an existing physics control from the scene graph and the physics
     * space.
     *
     * @param treePosition tree position of the controlled spatial (not null,
     * unaffected)
     * @param pcPosition position among the physics controls added to the
     * controlled spatial (&ge;0)
     */
    public void removePhysicsControl(List<Integer> treePosition,
            int pcPosition) {
        Validate.nonNull(treePosition, "tree position");
        Validate.nonNegative(pcPosition, "control position");

        Spatial spatial = findSpatial(treePosition);
        PhysicsControl pc = PhysicsUtil.pcFromPosition(spatial, pcPosition);
        pc.setEnabled(false);
        spatial.removeControl(pc);
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
     * Alter whether the indexed physics control applies to its spatial's local
     * translation.
     *
     * @param treePosition tree position of the controlled spatial (not null,
     * unaffected)
     * @param pcPosition position among the physics controls added to the
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
     * Alter the cull hint of the selected spatial.
     *
     * @param newHint new value for cull hint (not null)
     */
    public void setCullHint(Spatial.CullHint newHint) {
        Validate.nonNull(newHint, "cull hint");

        Spatial spatial = selectedSpatial();
        spatial.setCullHint(newHint);
    }

    /**
     * Alter the depth-test setting of the selected material.
     *
     * @param newState true &rarr; enable test, false &rarr; disable it
     */
    public void setDepthTest(boolean newState) {
        Material material = selectedMaterial();
        RenderState modelState = material.getAdditionalRenderState();
        modelState.setDepthTest(newState);
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
     * Alter the value of the selected material parameter.
     *
     * @param newValue (may be null, alias created if not null)
     */
    public void setMatParamValue(Object newValue) {
        Material material = selectedMaterial();
        SelectedMatParam matParam = getCgm().getMatParam();
        String name = matParam.getName();
        VarType varType = matParam.getVarType();
        material.setParam(name, varType, newValue);
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
     * Alter the value of the selected material-parameter override.
     *
     * @param newValue (may be null, alias created if not null)
     */
    public void setOverrideValue(Object newValue) {
        Spatial spatial = selectedSpatial();
        MatParamOverride oldMpo = findSelectedMpo();
        spatial.removeMatParamOverride(oldMpo);

        String name = oldMpo.getName();
        VarType varType = oldMpo.getVarType();
        MatParamOverride newMpo = new MatParamOverride(varType, name, newValue);
        boolean enabled = oldMpo.isEnabled();
        newMpo.setEnabled(enabled);
        spatial.addMatParamOverride(newMpo);
    }

    /**
     * Alter whether the specified physics control is enabled.
     *
     * @param treePosition tree position of the controlled spatial (not null,
     * unaffected)
     * @param pcPosition position among the physics controls added to the
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
     * Alter the queue bucket of the selected spatial.
     *
     * @param newBucket new value for queue bucket (not null)
     */
    public void setQueueBucket(RenderQueue.Bucket newBucket) {
        Validate.nonNull(newBucket, "new bucket");

        Spatial spatial = selectedSpatial();
        spatial.setQueueBucket(newBucket);
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
     * Enumerate all collision shapes in the physics space, excluding those
     * added by this view.
     *
     * @return a new map from ids to shapes
     */
    public Map<Long, CollisionShape> shapeMap() {
        PhysicsSpace space = getPhysicsSpace();
        Map<Long, CollisionShape> map = PhysicsUtil.shapeMap(space);

        Set<Long> viewIds = listIds(null);
        for (long id : viewIds) {
            map.remove(id);
        }

        return map;
    }

    /**
     * Copy the world transform of the C-G model, based on an animated geometry
     * if possible.
     *
     * @param storeResult (modified if not null)
     * @return world transform (either storeResult or a new instance)
     */
    public Transform worldTransform(Transform storeResult) {
        if (storeResult == null) {
            storeResult = new Transform();
        }

        Spatial transformSpatial = findTransformSpatial();
        if (!MySpatial.isIgnoringTransforms(transformSpatial)) {
            Transform alias = transformSpatial.getWorldTransform();
            storeResult.set(alias);
        }

        return storeResult;
    }
    // *************************************************************************
    // JmeCloneable methods

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
    private Material selectedMaterial() {
        Spatial spatial = selectedSpatial();
        Geometry geometry = (Geometry) spatial;
        Material material = geometry.getMaterial();

        assert material != null;
        return material;
    }
}
