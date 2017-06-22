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
import maud.Maud;
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
     * Update the MVC model based on the sliders.
     */
    void onSliderChanged() {
        float lineWidth = Maud.gui.readSlider("axesLineWidth");
        Maud.model.axes.setLineWidth(lineWidth);
    }

    /**
     * Update the transform and AxesControl settings for both CG models.
     */
    void updateVisualizations() {
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
     * Update the transform and AxesControl settings for a CG model.
     *
     * @param loadedCgm (not null)
     * @param axesControl (not null)
     * @param axesNode (not null)
     */
    private void updateAxes(LoadedCGModel loadedCgm,
            AxesControl axesControl, Node axesNode) {
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
        Transform transform;
        String mode = Maud.model.axes.getMode();
        switch (mode) {
            case "bone":
                if (loadedCgm.bone.isSelected()) {
                    int boneIndex = loadedCgm.bone.getIndex();
                    transform = loadedCgm.pose.modelTransform(boneIndex, null);
                    Geometry ag = loadedCgm.view.findAnimatedGeometry();
                    Transform agTransform = ag.getWorldTransform();
                    transform.combineWithParent(agTransform);
                } else {
                    transform = null;
                }
                break;

            case "model":
                if (loadedCgm.isLoaded()) {
                    Spatial cgm = loadedCgm.view.getCgmRoot();
                    transform = cgm.getWorldTransform();
                } else {
                    transform = null;
                }
                break;

            case "none":
                transform = null;
                break;

            case "spatial":
                if (loadedCgm.isLoaded()) {
                    Spatial spatial = loadedCgm.view.selectedSpatial();
                    transform = spatial.getWorldTransform();
                } else {
                    transform = null;
                }
                break;

            case "world":
                if (loadedCgm == Maud.model.target) {
                    transform = new Transform(); // identity
                } else {
                    transform = null;
                }
                break;

            default:
                throw new IllegalArgumentException();
        }

        return transform;
    }
}
