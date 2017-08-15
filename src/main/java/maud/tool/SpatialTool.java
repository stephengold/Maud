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

import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.WindowController;
import maud.Maud;
import maud.model.LoadedCgm;

/**
 * The controller for the "Spatial Tool" window in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class SpatialTool extends WindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            SpatialTool.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController
     */
    SpatialTool(BasicScreenController screenController) {
        super(screenController, "spatialTool", false);
    }
    // *************************************************************************
    // AppState methods

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

        updateTreePosition();

        updateBatchHint();
        updateBucket();
        updateChildren();
        updateSgcs();
        updateHint();
        updateKeys();
        updateMaterial();
        updateMesh();
        updateName();
        updateParent();
        updateShadows();
        updateType();
    }
    // *************************************************************************
    // private methods

    /**
     * Update the display of the spatial's batch hint.
     */
    private void updateBatchHint() {
        Spatial.BatchHint hint;
        hint = Maud.getModel().target.spatial.getLocalBatchHint();
        String text = hint.toString();
        Maud.gui.setStatusText("spatialBatchHint", " " + text);
    }

    /**
     * Update the display of the spatial's render-queue bucket.
     */
    private void updateBucket() {
        RenderQueue.Bucket bucket;
        bucket = Maud.getModel().target.spatial.getLocalQueueBucket();
        String bucketText = bucket.toString();
        Maud.gui.setStatusText("spatialBucket", " " + bucketText);
    }

    /**
     * Update the display of the spatial's children.
     */
    private void updateChildren() {
        String childrenText, scButton;

        LoadedCgm target = Maud.getModel().target;
        if (target.spatial.isNode()) {
            int numChildren = target.spatial.countChildren();
            if (numChildren == 0) {
                childrenText = "none";
                scButton = "";
            } else if (numChildren == 1) {
                String childName = target.spatial.getChildName(0);
                if (childName == null) {
                    childrenText = "null";
                } else {
                    childrenText = MyString.quote(childName);
                }
                scButton = "Select";
            } else {
                childrenText = String.format("%d children", numChildren);
                scButton = "Select";
            }
        } else {
            childrenText = "n/a";
            scButton = "";
        }
        Maud.gui.setStatusText("spatialChildren", " " + childrenText);
        Maud.gui.setButtonLabel("spatialSelectChildButton", scButton);
    }

    /**
     * Update the display of the spatial's cull hint.
     */
    private void updateHint() {
        Spatial.CullHint hint;
        hint = Maud.getModel().target.spatial.getLocalCullHint();
        String hintText = hint.toString();
        Maud.gui.setStatusText("spatialHint", " " + hintText);
    }

    /**
     * Update the display of the spatial's user-data keys.
     */
    private void updateKeys() {
        int numKeys = Maud.getModel().target.spatial.countUserKeys();
        String keysText = String.format("%d", numKeys);
        Maud.gui.setStatusText("spatialKeys", " " + keysText);
    }

    /**
     * Update the display of the spatial's material, if any.
     */
    private void updateMaterial() {
        String materialText;
        LoadedCgm target = Maud.getModel().target;
        if (target.spatial.isGeometry()) {
            if (target.spatial.hasMaterial()) {
                String materialName = target.spatial.getMaterialName();
                if (materialName == null) {
                    materialText = "nameless";
                } else {
                    materialText = MyString.quote(materialName);
                }
            } else {
                materialText = "none";
            }
        } else {
            materialText = "n/a";
        }
        Maud.gui.setStatusText("spatialMaterial", " " + materialText);
    }

    /**
     * Update the display of the spatial's mesh, if any.
     */
    private void updateMesh() {
        String meshText;
        LoadedCgm target = Maud.getModel().target;
        if (target.spatial.isGeometry()) {
            if (target.spatial.hasMesh()) {
                if (target.spatial.hasAnimatedMesh()) {
                    meshText = "animated";
                } else {
                    meshText = "non-animated";
                }
                Mesh.Mode mode = target.spatial.getMeshMode();
                meshText += String.format(" %s", mode.toString());
                int numVertices = target.spatial.countVertices();
                meshText += String.format(", %d verts, ", numVertices);
                int numLevels = target.spatial.countLoDLevels();
                if (numLevels == 1) {
                    meshText += "one LoD";
                } else {
                    meshText += String.format("%d LoDs", numLevels);
                }
            } else {
                meshText = "none";
            }
        } else {
            meshText = "n/a";
        }
        Maud.gui.setStatusText("spatialMesh", " " + meshText);
    }

    /**
     * Update the display of the spatial's name.
     */
    private void updateName() {
        String name = Maud.getModel().target.spatial.getName();
        String nameText;
        if (name == null) {
            nameText = "null";
        } else {
            nameText = MyString.quote(name);
        }
        Maud.gui.setStatusText("spatialName", " " + nameText);
    }

    /**
     * Update the display of the spatial's parent.
     */
    private void updateParent() {
        String parentText, spButton;

        if (Maud.getModel().target.spatial.isCgmRoot()) {
            parentText = "none (the model root)";
            spButton = "";
        } else {
            String name = Maud.getModel().target.spatial.getParentName();
            if (name == null) {
                parentText = "null";
            } else {
                parentText = MyString.quote(name);
            }
            spButton = "Select";
        }

        Maud.gui.setStatusText("spatialParent", " " + parentText);
        Maud.gui.setButtonLabel("spatialSelectParentButton", spButton);
    }

    /**
     * Update the display of the spatial's SG controls.
     */
    private void updateSgcs() {
        int numControls = Maud.getModel().target.spatial.countSgcs();
        String controlsText = String.format("%d", numControls);
        Maud.gui.setStatusText("spatialControls", " " + controlsText);
    }

    /**
     * Update the display of the spatial's shadow mode.
     */
    private void updateShadows() {
        RenderQueue.ShadowMode mode;
        mode = Maud.getModel().target.spatial.getLocalShadowMode();
        String shadowsText = mode.toString();
        Maud.gui.setStatusText("spatialShadows", " " + shadowsText);
    }

    /**
     * Update the display of the spatial's position in the model's scene graph.
     */
    private void updateTreePosition() {
        String positionText;
        if (Maud.getModel().target.spatial.isCgmRoot()) {
            positionText = "model root";
        } else {
            positionText = Maud.getModel().target.spatial.toString();
        }

        Maud.gui.setStatusText("spatialTreePosition", positionText);
    }

    /**
     * Update the display of the spatial's type.
     */
    private void updateType() {
        String typeText = Maud.getModel().target.spatial.describeType();
        Maud.gui.setStatusText("spatialType", typeText);
    }
}
