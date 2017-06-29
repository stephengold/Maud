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
package maud.tools;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import de.lessvoid.nifty.controls.Slider;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.debug.AxesControl;
import jme3utilities.math.MyMath;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.WindowController;
import maud.Maud;
import maud.Pose;
import maud.Util;
import maud.model.AxesStatus;
import maud.model.EditableCgm;
import maud.model.LoadedCGModel;

/**
 * The controller for the "Axes Tool" window in Maud's "3D View" screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class AxesTool extends WindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            AxesTool.class.getName());
    // *************************************************************************
    // fields

    /**
     * SG control to display coordinate axes for the source CG model
     */
    private AxesControl sourceAxesControl;
    /**
     * SG control to display coordinate axes for the target CG model
     */
    private AxesControl targetAxesControl;
    /**
     * SG node to translate and rotate the axes for the source CG model
     */
    final private Node sourceAxesNode = new Node("source axes node");
    /**
     * SG node to translate and rotate the axes for the target CG model
     */
    final private Node targetAxesNode = new Node("target axes node");
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController the controller of the screen that contains the
     * window (not null)
     */
    AxesTool(BasicScreenController screenController) {
        super(screenController, "axesTool", false);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Test whether the indexed axis points toward or away from the camera.
     *
     * @param cgm which CG model (not null, unaffected)
     * @param axisIndex which axis (&ge;0, &lt;2)
     * @return true if pointing away, otherwise false
     */
    boolean isAxisReceding(LoadedCGModel cgm, int axisIndex) {
        assert cgm != null;
        assert axisIndex >= 0 : axisIndex;
        assert axisIndex < 3 : axisIndex;

        AxesControl axesControl = findControl(cgm);
        assert axesControl.isEnabled();
        Spatial axesSpatial = axesControl.getSpatial();
        /*
         * Calculate distances to the tip and tail of the axis arrow.
         */
        Vector3f tailLocation = axesSpatial.getWorldTranslation();
        Vector3f tipLocation = axesControl.tipLocation(axisIndex);
        Vector3f cameraLocation = Maud.model.camera.copyLocation(null);
        float tailDS = cameraLocation.distanceSquared(tailLocation);
        float tipDS = cameraLocation.distanceSquared(tipLocation);

        if (tipDS > tailDS) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Update the MVC model based on the sliders.
     */
    void onSliderChanged() {
        float lineWidth = Maud.gui.readSlider("axesLineWidth");
        Maud.model.axes.setLineWidth(lineWidth);
    }

    /**
     * Calculate the tip location of the indexed axis for the specified CG
     * model.
     *
     * @param loadedCgm which CG model (not null, unaffected)
     * @param axisIndex which axis in the CGM's AxesControl (&ge;0, &lt;3)
     * @return a new vector (in world coordinates) or null if axis not displayed
     */
    Vector3f tipLocation(LoadedCGModel loadedCgm, int axisIndex) {
        Validate.nonNull(loadedCgm, "loaded model");
        Validate.inRange(axisIndex, "axis index", 0, 2);

        Vector3f result = null;
        Transform transform = worldTransform(loadedCgm);
        if (transform != null) {
            AxesControl axesControl = findControl(loadedCgm);
            result = axesControl.tipLocation(axisIndex);
        }

        return result;
    }

    /**
     * Updates performed even when the tool is disabled. (Invoked once per
     * render pass.)
     */
    void updateVisualizations() {
        if (Maud.model.axes.isDraggingAxis()) {
            dragAxis();
        }
        /*
         * Update the transform and AxesControl settings for each CG model.
         */
        updateAxes(Maud.model.source, sourceAxesControl, sourceAxesNode);
        updateAxes(Maud.model.target, targetAxesControl, targetAxesNode);
    }
    // *************************************************************************
    // AppState methods

    /**
     * Initialize this controller prior to its 1st update.
     *
     * @param stateManager (not null)
     * @param application application that owns the window (not null)
     */
    @Override
    public void initialize(AppStateManager stateManager,
            Application application) {
        super.initialize(stateManager, application);
        /*
         * Instantiate and add/attach the AxesControls/nodes.
         */
        sourceAxesControl = new AxesControl(assetManager, 1f, 1f);
        sourceAxesNode.addControl(sourceAxesControl);
        rootNode.attachChild(sourceAxesNode);

        targetAxesControl = new AxesControl(assetManager, 1f, 1f);
        targetAxesNode.addControl(targetAxesControl);
        rootNode.attachChild(targetAxesNode);
    }

    /**
     * Callback to update this window prior to rendering. (Invoked once per
     * render pass while the window is displayed.)
     *
     * @param elapsedTime time interval between render passes (in seconds,
     * &ge;0)
     */
    @Override
    public void update(float elapsedTime) {
        super.update(elapsedTime);

        boolean depthTestFlag = Maud.model.axes.getDepthTestFlag();
        Maud.gui.setChecked("axesDepthTest", depthTestFlag);

        float lineWidth = Maud.model.axes.getLineWidth();
        Slider slider = Maud.gui.getSlider("axesLineWidth");
        slider.setValue(lineWidth);

        updateLabels();
    }
    // *************************************************************************
    // private methods

    /**
     * While dragging an axis, update the orientation of the visualized object
     * in the MVC model.
     */
    private void dragAxis() {
        AxesStatus model = Maud.model.axes;
        LoadedCGModel cgm = model.getDragCgm();
        assert cgm.isLoaded();
        int axisIndex = model.getDragAxis();
        boolean farSide = model.isDraggingFarSide();

        AxesControl axesControl = findControl(cgm);
        assert axesControl.isEnabled();
        Spatial axesSpatial = axesControl.getSpatial();
        /*
         * Calculate the old axis direction in local coordinates.
         */
        Vector3f oldDirection = Util.axisVector(axisIndex, 1f, null);
        assert oldDirection.isUnitVector() : oldDirection;
        /*
         * Calculate the new axis direction in local coordinates.
         */
        Ray worldRay = Util.mouseRay(cam, inputManager);
        Ray localRay = Util.localizeRay(worldRay, axesSpatial);
        float radius = axesControl.getAxisLength();
        Vector3f newDirection = Util.lineMeetsSphere(localRay, radius, farSide);
        newDirection.divideLocal(radius);
        assert newDirection.isUnitVector() : newDirection;

        Vector3f cross = oldDirection.cross(newDirection);
        float crossNorm = cross.length();
        if (crossNorm > 0f) {
            rotateObject(cross, cgm);
        }
    }

    /**
     * Access the axes control for the specified CG model.
     *
     * @param cgm which CG model (not null, unaffected)
     * @return the pre-existing instance
     */
    private AxesControl findControl(LoadedCGModel cgm) {
        assert cgm != null;

        AxesControl result;
        if (cgm == Maud.model.source) {
            result = sourceAxesControl;
        } else if (cgm == Maud.model.target) {
            result = targetAxesControl;
        } else {
            throw new IllegalArgumentException();
        }

        return result;
    }

    /**
     * Rotate the visualized bone using the specified quaternion.
     *
     * @param rotation quaternion (not null, norm=1)
     * @param cgm which CG model (not null, unaffected)
     */
    private void rotateBone(Quaternion rotation, LoadedCGModel cgm) {
        int boneIndex = cgm.bone.getIndex();
        assert boneIndex != -1;
        Pose pose = cgm.pose.getPose();
        Quaternion oldUserRotation = pose.userRotation(boneIndex, null);

        Quaternion newUserRotation = null;
        if (cgm.bone.shouldEnableControls()) {
            /*
             * Apply the rotation to the selected bone in the displayed pose.
             */
            newUserRotation = oldUserRotation.mult(rotation);
            newUserRotation.normalizeLocal();
            pose.setRotation(boneIndex, newUserRotation);

        } else if (cgm == Maud.model.target
                && cgm.animation.isRetargetedPose()
                && Maud.model.mapping.isBoneMappingSelected()) {
            /*
             * Apply the rotation to the target bone in the displayed pose.
             */
            newUserRotation = oldUserRotation.mult(rotation);
            pose.setRotation(boneIndex, newUserRotation);
        }

        if (newUserRotation != null && !cgm.bone.shouldEnableControls()) {
            assert Maud.model.target.animation.isRetargetedPose();
            assert Maud.model.mapping.isBoneMappingSelected();
            /*
             * Infer a new effective twist for the selected bone mapping.
             */
            Quaternion sourceMo = Maud.model.source.bone.modelOrientation(null);
            Quaternion targetMo = Maud.model.target.bone.modelOrientation(null);
            Quaternion invSourceMo = sourceMo.inverse();
            Quaternion newEffectiveTwist = invSourceMo.mult(targetMo);
            Maud.model.mapping.setTwist(newEffectiveTwist);
        }
    }

    /**
     * Rotate the visualized object using the specified cross product.
     *
     * @param cross cross product of two unit vectors (not null, length&gt;0)
     * @param cgm which CG model (not null, unaffected)
     * @return the pre-existing instance
     */
    private void rotateObject(Vector3f cross, LoadedCGModel cgm) {
        /*
         * Convert the cross product to a rotation quaternion.
         */
        float crossNorm = cross.length();
        Vector3f rotationAxis = cross.divide(crossNorm);
        assert rotationAxis.isUnitVector() : rotationAxis;
        float rotationAngle = FastMath.asin(crossNorm);
        Quaternion rotation = new Quaternion();
        rotation.fromAngleNormalAxis(rotationAngle, rotationAxis);
        /*
         * Determine which MVC-model object the control is visualizing,
         * and apply the rotation to that object.
         */
        String mode = Maud.model.axes.getMode();
        switch (mode) {
            case "bone":
                rotateBone(rotation, cgm);
                break;

            case "model":
                /*
                 * Apply the Y-axis rotation to the transform status.
                 */
                float yRotation = FastMath.asin(cross.y * crossNorm);
                cgm.transform.rotateY(yRotation);
                break;

            case "spatial":
                if (cgm instanceof EditableCgm) {
                    EditableCgm ecgm = (EditableCgm) cgm;
                    /*
                     * Apply the full rotation to the selected spatial.
                     */
                    Quaternion oldQ = ecgm.spatial.localRotation(null);
                    Quaternion newQ = oldQ.mult(rotation);
                    newQ.normalizeLocal();
                    ecgm.setSpatialRotation(newQ);
                }
                break;

            case "world":
                // ignore attempts to drag the world axes
                break;

            default:
                throw new IllegalStateException();
        }
    }

    /**
     * Update the transform and AxesControl settings for the specified CG model.
     *
     * @param loadedCgm which CG model (not null)
     * @param axesControl (not null)
     * @param axesNode (not null)
     */
    private void updateAxes(LoadedCGModel loadedCgm, AxesControl axesControl,
            Node axesNode) {
        assert loadedCgm != null;
        assert axesControl != null;
        assert axesNode != null;

        Transform transform = worldTransform(loadedCgm);
        if (transform == null) {
            axesControl.setEnabled(false);
        } else {
            axesNode.setLocalTransform(transform);
            axesControl.setEnabled(true);

            Vector3f axesOrigin = transform.getTranslation();
            Vector3f cameraLocation = Maud.model.camera.copyLocation(null);
            float distance = axesOrigin.distance(cameraLocation);
            Vector3f scale = transform.getScale();
            float maxScale = MyMath.max(scale.x, scale.y, scale.z);
            float length = 0.2f * distance / maxScale;

            boolean depthTestFlag = Maud.model.axes.getDepthTestFlag();
            float lineWidth = Maud.model.axes.getLineWidth();

            axesControl.setAxisLength(length);
            axesControl.setDepthTest(depthTestFlag);
            axesControl.setEnabled(true);
            axesControl.setLineWidth(lineWidth);
        }
    }

    /**
     * Update the status labels based on the MVC model.
     */
    private void updateLabels() {
        float lineWidth = Maud.model.axes.getLineWidth();
        Maud.gui.updateSliderStatus("axesLineWidth", lineWidth, " pixels");
    }

    /**
     * Calculate the coordinate transform for the axes.
     *
     * @param loadedCgm (not null)
     * @return a new instance (in world coordinates) or null to hide the axes
     */
    private Transform worldTransform(LoadedCGModel loadedCgm) {
        Transform transform = null;
        String mode = Maud.model.axes.getMode();
        switch (mode) {
            case "bone":
                if (loadedCgm.bone.isSelected()) {
                    transform = loadedCgm.bone.modelTransform(null);
                    // TODO use animated spatial
                    Transform worldTransform;
                    worldTransform = loadedCgm.view.copyWorldTransform();
                    transform.combineWithParent(worldTransform);
                }
                break;

            case "model":
                if (loadedCgm.isLoaded()) {
                    transform = loadedCgm.transform.worldTransform();
                }
                break;

            case "none":
                break;

            case "spatial":
                if (loadedCgm.isLoaded()) {
                    Spatial spatial = loadedCgm.view.selectedSpatial();
                    transform = spatial.getWorldTransform();
                }
                break;

            case "world":
                if (loadedCgm == Maud.model.target) {
                    transform = new Transform(); // identity
                }
                break;

            default:
                throw new IllegalStateException();
        }

        return transform;
    }
}
