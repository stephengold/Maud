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

import com.jme3.bounding.BoundingVolume;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Spatial;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.WindowController;
import maud.Maud;
import maud.model.cgm.SelectedSpatial;

/**
 * The controller for the "Spatial-Details Tool" window in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class SpatialDetailsTool extends WindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SpatialDetailsTool.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController (not null)
     */
    SpatialDetailsTool(BasicScreenController screenController) {
        super(screenController, "spatialDetailsTool", false);
    }
    // *************************************************************************
    // WindowController methods

    /**
     * Callback to update this window prior to rendering. (Invoked once per
     * render pass.)
     *
     * @param elapsedTime time interval between render passes (in seconds,
     * &ge;0)
     */
    @Override
    public void update(float elapsedTime) {
        super.update(elapsedTime);
        Maud.gui.setIgnoreGuiChanges(true);

        updateBatchHint();
        updateBound();
        updateBucket();
        updateCull();
        updateIgnoreTransform();
        updateInfluence();
        updateLights();
        updateName();
        updateOverrides();
        updateSgcs();
        updateShadows();
        updateUserData();

        Maud.gui.setIgnoreGuiChanges(false);
    }
    // *************************************************************************
    // private methods

    /**
     * Update the display of the spatial's batch hint.
     */
    private void updateBatchHint() {
        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        Spatial.BatchHint hint = spatial.getLocalBatchHint();
        String description = hint.toString();
        Maud.gui.setButtonText("spatialBatchHint", description);
    }

    /**
     * Update the spatial's bound-type status and toggle button.
     */
    private void updateBound() {
        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        BoundingVolume.Type type = spatial.getWorldBoundType();
        String typeText = "null";
        if (type != null) {
            typeText = type.toString();
        }
        Maud.gui.setStatusText("spatialBound", " " + typeText);

        String toggleText = "";
        if (spatial.isGeometry()) {
            toggleText = "Toggle";
        }
        Maud.gui.setButtonText("spatialBoundType", toggleText);
    }

    /**
     * Update the display of the spatial's render-queue bucket.
     */
    private void updateBucket() {
        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        RenderQueue.Bucket bucket = spatial.getLocalQueueBucket();
        String description = bucket.toString();
        Maud.gui.setButtonText("spatialBucket", description);
    }

    /**
     * Update the display of the spatial's cull hint.
     */
    private void updateCull() {
        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        Spatial.CullHint hint = spatial.getLocalCullHint();
        String description = hint.toString();
        Maud.gui.setButtonText("spatialCullHint", description);
    }

    /**
     * Update the "ignore transform" check box.
     */
    private void updateIgnoreTransform() {
        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        boolean flag = spatial.isTransformIgnored();
        Maud.gui.setChecked("spatialIgnoreTransform", flag);
    }

    /**
     * Update the count of mesh vertices influenced.
     */
    private void updateInfluence() {
        List<String> list = new ArrayList<>(3);

        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        int sgcCount = spatial.countSubtreeSgcs();
        if (sgcCount == 1) {
            String item = String.format(" one control");
            list.add(item);
        } else if (sgcCount > 1) {
            String item = String.format(" %d controls", sgcCount);
            list.add(item);
        }

        int dataCount = spatial.countSubtreeUserData();
        if (dataCount == 1) {
            String item = String.format(" one datum");
            list.add(item);
        } else if (dataCount > 1) {
            String item = String.format(" %d data", dataCount);
            list.add(item);
        }

        int vertexCount = spatial.countSubtreeVertices();
        if (vertexCount == 1) {
            String item = String.format(" one vertex");
            list.add(item);
        } else if (vertexCount > 1) {
            String item = String.format(" %d vertices", vertexCount);
            list.add(item);
        }

        String description;
        if (list.isEmpty()) {
            description = "none";
        } else {
            description = MyString.join(", ", list);
        }

        Maud.gui.setStatusText("spatialInfluence", " " + description);
    }

    /**
     * Update the count of local lights.
     */
    private void updateLights() {
        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        int numLights = spatial.countLights();
        String statusText = String.format(" %d", numLights);
        Maud.gui.setStatusText("spatialLights", statusText);
    }

    /**
     * Update the display of the spatial's name.
     */
    private void updateName() {
        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        String name = spatial.getName();
        String description = MyString.quote(name);
        Maud.gui.setStatusText("spatialName2", " " + description);
    }

    /**
     * Update the count of material-parameter overrides.
     */
    private void updateOverrides() {
        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        int numOverrides = spatial.countOverrides();
        String statusText = String.format(" %d", numOverrides);
        Maud.gui.setStatusText("spatialOverrides", statusText);
    }

    /**
     * Update the count of scene-graph controls.
     */
    private void updateSgcs() {
        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        int numSgcs = spatial.countSgcs();
        String statusText = String.format(" %d", numSgcs);
        Maud.gui.setStatusText("spatialControls", statusText);
    }

    /**
     * Update the display of the spatial's shadow mode.
     */
    private void updateShadows() {
        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        RenderQueue.ShadowMode mode = spatial.getLocalShadowMode();
        String description = mode.toString();
        Maud.gui.setButtonText("spatialShadows", description);
    }

    /**
     * Update the count of user data.
     */
    private void updateUserData() {
        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        int numKeys = spatial.countUserData();
        String statusText = String.format(" %d", numKeys);
        Maud.gui.setStatusText("spatialUserData", statusText);
    }
}
