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
package maud;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.FastMath;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import de.lessvoid.nifty.controls.Slider;
import java.util.logging.Logger;
import jme3utilities.debug.AxesControl;
import jme3utilities.math.MyMath;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.WindowController;
import maud.model.AxesStatus;

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
     * scene-graph control to display coordinate axes
     */
    private AxesControl axesControl;
    /**
     * scene-graph node to translate and rotate the axes
     */
    final private Node axesNode = new Node("axes node");
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
     * Update the MVC model based on the sliders.
     */
    void onSliderChanged() {
        float value = Maud.gui.readSlider("axesLength");
        float axesLength = FastMath.pow(10f, value);
        Maud.model.axes.setLength(axesLength);

        float lineWidth = Maud.gui.readSlider("axesLineWidth");
        Maud.model.axes.setLineWidth(lineWidth);
    }

    /**
     * Update the node transform and AxesControl settings.
     */
    void updateAxesControl() {
        Transform transform = worldTransform();
        if (transform == null) {
            axesControl.setEnabled(false);
        } else {
            axesNode.setLocalTransform(transform);
            axesControl.setEnabled(true);

            float length;
            if (Maud.model.axes.isAutoSizing()) {
                Vector3f axesOrigin = transform.getTranslation();
                Vector3f cameraLocation = Maud.model.camera.copyLocation(null);
                float distance = axesOrigin.distance(cameraLocation);
                Vector3f scale = transform.getScale();
                float maxScale = MyMath.max(scale.x, scale.y, scale.z);
                length = 0.2f * distance / maxScale;
                Maud.model.axes.setLength(length);
            } else {
                length = Maud.model.axes.getLength();
            }
            boolean depthTestFlag = Maud.model.axes.getDepthTestFlag();
            float lineWidth = Maud.model.axes.getLineWidth();

            axesControl.setAxisLength(length);
            axesControl.setDepthTest(depthTestFlag);
            axesControl.setEnabled(true);
            axesControl.setLineWidth(lineWidth);
        }
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
         * Instantiate and add/attach the AxesControl and the node.
         */
        axesControl = new AxesControl(assetManager, 1f, 1f);
        axesNode.addControl(axesControl);
        rootNode.attachChild(axesNode);
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

        boolean isAutoSizing = Maud.model.axes.isAutoSizing();
        Maud.gui.setChecked("axesAuto", isAutoSizing);
        Slider slider = Maud.gui.getSlider("axesLength");
        slider.setEnabled(!isAutoSizing);

        float axesLength = Maud.model.axes.getLength();
        float value = FastMath.log(axesLength, 10f);
        slider.setValue(value);

        boolean depthTestFlag = Maud.model.axes.getDepthTestFlag();
        Maud.gui.setChecked("axesDepthTest", depthTestFlag);

        float lineWidth = Maud.model.axes.getLineWidth();
        slider = Maud.gui.getSlider("axesLineWidth");
        slider.setValue(lineWidth);

        updateLabels();
    }
    // *************************************************************************
    // private methods

    /**
     * Update the status labels based on the MVC model.
     */
    private void updateLabels() {
        AxesStatus model = Maud.model.axes;

        String mode = model.getMode();
        String units;
        switch (mode) {
            case "bone":
                if (Maud.model.target.bone.isBoneSelected()) {
                    units = " bone units";
                } else {
                    units = " units";
                }
                break;
            case "model":
                units = " model units";
                break;
            case "none":
                units = " units";
                break;
            case "spatial":
                units = " local units";
                break;
            case "world":
                units = " world units";
                break;
            default:
                throw new IllegalStateException();
        }

        float axesLength = model.getLength();
        Maud.gui.updateSliderStatus("axesLength", axesLength, units);

        float lineWidth = model.getLineWidth();
        Maud.gui.updateSliderStatus("axesLineWidth", lineWidth, " pixels");
    }

    /**
     * Calculate the coordinate transform for the axes.
     *
     * @return a new instance (in world coordinates) or null to hide the axes
     */
    private Transform worldTransform() {
        Transform transform;
        String mode = Maud.model.axes.getMode();
        switch (mode) {
            case "bone":
                if (Maud.model.target.bone.isBoneSelected()) {
                    int boneIndex = Maud.model.target.bone.getIndex();
                    transform = Maud.model.target.pose.modelTransform(boneIndex,
                            null);
                    Geometry ag = Maud.viewState.findAnimatedGeometry();
                    Transform agTransform = ag.getWorldTransform();
                    transform.combineWithParent(agTransform);
                } else {
                    transform = null;
                }
                break;

            case "model":
                Spatial cgm = Maud.viewState.getSpatial();
                transform = cgm.getWorldTransform();
                break;

            case "none":
                transform = null;
                break;

            case "spatial":
                cgm = Maud.viewState.getSpatial();
                Spatial spatial = Maud.model.target.spatial.findSpatial(cgm);
                transform = spatial.getWorldTransform();
                break;

            case "world":
                transform = new Transform(); // identity
                break;

            default:
                throw new IllegalArgumentException();
        }

        return transform;
    }
}
