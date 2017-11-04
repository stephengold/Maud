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
package maud.tool;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import de.lessvoid.nifty.controls.Slider;
import java.util.logging.Logger;
import jme3utilities.MyCamera;
import jme3utilities.Validate;
import jme3utilities.debug.AxesVisualizer;
import jme3utilities.math.MyMath;
import jme3utilities.math.MyVector3f;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.WindowController;
import jme3utilities.wes.Pose;
import maud.Maud;
import maud.model.EditableMap;
import maud.model.EditorModel;
import maud.model.cgm.Cgm;
import maud.model.cgm.EditableCgm;
import maud.model.option.scene.AxesMode;
import maud.model.option.scene.AxesOptions;
import maud.view.SceneDrag;
import maud.view.SceneView;

/**
 * The controller for the "Axes Tool" window in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class AxesTool extends WindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(AxesTool.class.getName());
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
     * While dragging an axis, update the orientation of the visualized object
     * in the MVC model.
     */
    public void dragAxis() {
        Cgm cgm = SceneDrag.getDragCgm();
        assert cgm.isLoaded();
        int axisIndex = SceneDrag.getDragAxis();
        boolean farSide = SceneDrag.isDraggingFarSide();

        AxesVisualizer visualizer = cgm.getSceneView().getAxesVisualizer();
        assert visualizer.isEnabled();
        Spatial axesSpatial = visualizer.getSpatial();
        /*
         * Calculate the old axis direction in local coordinates.
         */
        Vector3f oldDirection = MyVector3f.axisVector(axisIndex, 1f, null);
        assert oldDirection.isUnitVector() : oldDirection;
        /*
         * Calculate the new axis direction in local coordinates.
         */
        Camera camera = cgm.getSceneView().getCamera();
        Ray worldRay = MyCamera.mouseRay(camera, inputManager);
        Ray localRay = MyMath.localizeRay(worldRay, axesSpatial);
        float radius = visualizer.getAxisLength();
        Vector3f newDirection;
        newDirection = MyVector3f.lineMeetsSphere(localRay, radius, farSide);
        newDirection.divideLocal(radius);
        assert newDirection.isUnitVector() : newDirection;

        Vector3f cross = oldDirection.cross(newDirection);
        float crossNorm = cross.length();
        if (crossNorm > 0f) {
            rotateObject(cross, cgm);
        }
    }

    /**
     * Test whether the indexed axis points toward or away from the camera.
     *
     * @param cgm which C-G model (not null, unaffected)
     * @param axisIndex which axis: 0&rarr;X, 1&rarr;Y, 2&rarr;Z
     * @return true if pointing away, otherwise false
     */
    public boolean isAxisReceding(Cgm cgm, int axisIndex) {
        Validate.nonNull(cgm, "model");
        Validate.inRange(axisIndex, "axis index", 0, 2);

        AxesVisualizer visualizer = cgm.getSceneView().getAxesVisualizer();
        assert visualizer.isEnabled();
        Spatial axesSpatial = visualizer.getSpatial();
        /*
         * Calculate distances to the tip and tail of the axis arrow.
         */
        Vector3f tailLocation = axesSpatial.getWorldTranslation();
        Vector3f tipLocation = visualizer.tipLocation(axisIndex);
        Vector3f cameraLocation = cgm.getScenePov().cameraLocation(null);
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
        Maud.getModel().getScene().getAxes().setLineWidth(lineWidth);
    }

    /**
     * Calculate the tip location of the indexed axis for the specified C-G
     * model.
     *
     * @param cgm which C-G model (not null, unaffected)
     * @param axisIndex which axis in the C-G model's AxesControl (&ge;0, &lt;3)
     * @return a new vector (in world coordinates) or null if axis not displayed
     */
    public Vector3f tipLocation(Cgm cgm, int axisIndex) {
        Validate.nonNull(cgm, "loaded model");
        Validate.inRange(axisIndex, "axis index", 0, 2);

        Vector3f result = null;
        Transform transform = worldTransform(cgm);
        if (transform != null) {
            AxesVisualizer visualizer = cgm.getSceneView().getAxesVisualizer();
            result = visualizer.tipLocation(axisIndex);
        }

        return result;
    }

    /**
     * Update the C-G model's visualizer based on the MVC model.
     *
     * @param cgm which C-G model (not null)
     */
    void updateVisualizer(Cgm cgm) {
        AxesVisualizer axesControl = cgm.getSceneView().getAxesVisualizer();
        Transform transform = worldTransform(cgm);
        if (transform == null) {
            axesControl.setEnabled(false);
        } else {
            Node axesNode = (Node) axesControl.getSpatial();
            axesNode.setLocalTransform(transform);
            axesControl.setEnabled(true);

            Vector3f axesOrigin = transform.getTranslation();
            Vector3f cameraLocation = cgm.getScenePov().cameraLocation(null);
            float distance = axesOrigin.distance(cameraLocation);
            Vector3f scale = transform.getScale();
            float maxScale = MyMath.max(scale.x, scale.y, scale.z);
            assert maxScale > 0f : maxScale;
            float length = 0.2f * distance / maxScale;

            AxesOptions options = Maud.getModel().getScene().getAxes();
            boolean depthTestFlag = options.getDepthTestFlag();
            float lineWidth = options.getLineWidth();

            axesControl.setAxisLength(length);
            axesControl.setDepthTest(depthTestFlag);
            axesControl.setEnabled(true);
            axesControl.setLineWidth(lineWidth);
        }
    }
    // *************************************************************************
    // WindowController methods

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

        AxesOptions options = Maud.getModel().getScene().getAxes();
        boolean depthTestFlag = options.getDepthTestFlag();
        Maud.gui.setChecked("axesDepthTest", depthTestFlag);

        float lineWidth = options.getLineWidth();
        Slider slider = Maud.gui.getSlider("axesLineWidth");
        slider.setValue(lineWidth);

        updateLabels();
    }
    // *************************************************************************
    // private methods

    /**
     * Rotate the visualized bone using the specified quaternion.
     *
     * @param rotation quaternion (not null, norm=1)
     * @param cgm which C-G model (not null, unaffected)
     */
    private void rotateBone(Quaternion rotation, Cgm cgm) {
        int boneIndex = cgm.getBone().getIndex();
        assert boneIndex != -1;
        EditorModel model = Maud.getModel();
        EditableMap map = model.getMap();
        EditableCgm target = model.getTarget();
        Pose pose = cgm.getPose().get();
        Quaternion oldUserRotation = pose.userRotation(boneIndex, null);

        Quaternion newUserRotation = null;
        if (cgm.getBone().shouldEnableControls()) {
            /*
             * Apply the rotation to the selected bone in the displayed pose.
             */
            newUserRotation = oldUserRotation.mult(rotation);
            newUserRotation.normalizeLocal();
            pose.setRotation(boneIndex, newUserRotation);

        } else if (cgm == target
                && cgm.getAnimation().isRetargetedPose()
                && map.isBoneMappingSelected()) {
            /*
             * Apply the rotation to the target bone in the displayed pose.
             */
            newUserRotation = oldUserRotation.mult(rotation);
            pose.setRotation(boneIndex, newUserRotation);
        }

        if (newUserRotation != null && !cgm.getBone().shouldEnableControls()) {
            assert target.getAnimation().isRetargetedPose();
            assert map.isBoneMappingSelected();
            /*
             * Infer a new effective twist for the selected bone mapping.
             */
            Quaternion sourceMo;
            sourceMo = model.getSource().getBone().modelOrientation(null);
            Quaternion targetMo;
            targetMo = target.getBone().modelOrientation(null);
            Quaternion invSourceMo = sourceMo.inverse(); // TODO conjugate
            Quaternion newEffectiveTwist = invSourceMo.mult(targetMo);
            map.setTwist(newEffectiveTwist);
        }
    }

    /**
     * Rotate the visualized object using the specified cross product.
     *
     * @param cross cross product of two unit vectors (not null, length&gt;0)
     * @param cgm which C-G model (not null, unaffected)
     * @return the pre-existing instance
     */
    private void rotateObject(Vector3f cross, Cgm cgm) {
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
        AxesMode mode = Maud.getModel().getScene().getAxes().getMode();
        switch (mode) {
            case Bone:
                rotateBone(rotation, cgm);
                break;

            case Cgm:
                /*
                 * Apply the Y-axis rotation to the transform status.
                 */
                float yRotation = FastMath.asin(cross.y * crossNorm);
                cgm.getSceneView().getTransform().rotateY(yRotation);
                break;

            case Spatial:
                if (cgm instanceof EditableCgm) {
                    EditableCgm ecgm = (EditableCgm) cgm;
                    /*
                     * Apply the full rotation to the selected spatial.
                     */
                    Quaternion oldQ = ecgm.getSpatial().localRotation(null);
                    Quaternion newQ = oldQ.mult(rotation);
                    newQ.normalizeLocal();
                    ecgm.setSpatialRotation(newQ);
                }
                break;

            case World:
            // ignore attempts to drag the world axes
        }
    }

    /**
     * Update the status labels based on the MVC model.
     */
    private void updateLabels() {
        float lineWidth = Maud.getModel().getScene().getAxes().getLineWidth();
        lineWidth = Math.round(lineWidth);
        Maud.gui.updateSliderStatus("axesLineWidth", lineWidth, " pixels");
    }

    /**
     * Calculate the coordinate transform for the axes.
     *
     * @param cgm (not null)
     * @return a new instance (in world coordinates) or null to hide the axes
     */
    private Transform worldTransform(Cgm cgm) {
        Transform transform = null;
        SceneView sceneView = cgm.getSceneView();
        AxesMode mode = Maud.getModel().getScene().getAxes().getMode();
        switch (mode) {
            case Bone:
                if (cgm.getBone().isSelected()) {
                    transform = cgm.getBone().modelTransform(null);
                    Geometry ag = sceneView.findAnimatedGeometry();
                    Transform worldTransform = ag.getWorldTransform();
                    transform.combineWithParent(worldTransform);
                }
                break;

            case Cgm:
                if (cgm.isLoaded()) {
                    transform = sceneView.getTransform().worldTransform();
                }
                break;

            case None:
                break;

            case Spatial:
                if (cgm.isLoaded()) {
                    Spatial spatial = sceneView.selectedSpatial();
                    transform = spatial.getWorldTransform();
                }
                break;

            case World:
                transform = new Transform(); // identity
                break;

            default:
                throw new IllegalStateException();
        }

        return transform;
    }
}
