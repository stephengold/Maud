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
package maud.tool;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Mesh;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.math.MyQuaternion;
import jme3utilities.math.MyVector3f;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.WindowController;
import maud.Maud;
import maud.model.cgm.SelectedSpatial;

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
    final private static Logger logger
            = Logger.getLogger(SpatialTool.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController (not null)
     */
    SpatialTool(BasicScreenController screenController) {
        super(screenController, "spatialTool", false);
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

        updateChildren();
        updateMaterial();
        updateMesh();
        updateName();
        updateParent();
        updateShadows();
        updateTransform();
        updateTreePosition();
        updateType();

        Maud.gui.setIgnoreGuiChanges(false);
    }
    // *************************************************************************
    // private methods

    /**
     * Update the display of the spatial's children.
     */
    private void updateChildren() {
        String childrenText, scButton;

        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        if (spatial.isNode()) {
            int numChildren = spatial.countChildren();
            if (numChildren == 0) {
                childrenText = "none (a leaf node is selected)";
                scButton = "";
            } else if (numChildren == 1) {
                String childName = spatial.getChildName(0);
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
            childrenText = "none (a geometry is selected)";
            scButton = "";
        }

        Maud.gui.setStatusText("spatialChildren", " " + childrenText);
        Maud.gui.setButtonText("spatialSelectChild", scButton);
    }

    /**
     * Update the display of the spatial's material, if any.
     */
    private void updateMaterial() {
        String materialText;

        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        if (spatial.isGeometry()) {
            if (spatial.hasMaterial()) {
                String materialName = spatial.getMaterialName();
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

        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        if (spatial.isGeometry()) {
            if (spatial.hasMesh()) {
                if (spatial.hasAnimatedMesh()) {
                    meshText = "animated";
                } else {
                    meshText = "non-animated";
                }
                Mesh.Mode mode = spatial.getMeshMode();
                meshText += String.format(" %s", mode.toString());
                int numVertices = spatial.countVertices();
                meshText += String.format(", %d verts, ", numVertices);
                int numLevels = spatial.countLodLevels();
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
        String nameText;

        String name = Maud.getModel().getTarget().getSpatial().getName();
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

        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        if (spatial.isCgmRoot()) {
            parentText = "none (the model root is selected)";
            spButton = "";
        } else {
            String name = spatial.getParentName();
            if (name == null) {
                parentText = "null";
            } else {
                parentText = MyString.quote(name);
            }
            spButton = "Select";
        }

        Maud.gui.setStatusText("spatialParent", " " + parentText);
        Maud.gui.setButtonText("spatialSelectParent", spButton);
    }

    /**
     * Update the display of the spatial's shadow mode.
     */
    private void updateShadows() {
        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        RenderQueue.ShadowMode mode = spatial.getLocalShadowMode();
        String shadowsText = mode.toString();
        Maud.gui.setStatusText("spatialShadows", " " + shadowsText);
    }

    /**
     * Update the display of the spatial's position in the model's scene graph.
     */
    private void updateTreePosition() {
        String positionText;

        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        if (spatial.isCgmRoot()) {
            positionText = "model root";
        } else {
            positionText = spatial.toString();
        }

        Maud.gui.setStatusText("spatialTreePosition", positionText);
    }

    /**
     * Update the display of the spatial's transform.
     */
    private void updateTransform() {
        String transformText;

        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        if (spatial.isTransformIgnored()) {
            transformText = "Ignored";

        } else if (spatial.isAnimationTarget()) {
            transformText = "Controlled by a spatial track";

        } else if (spatial.isAttachmentsNode()) {
            transformText = "Controlled by a skeleton";

        } else {
            StringBuilder notes = new StringBuilder(20);
            Vector3f translation = spatial.localTranslation(null);
            if (!MyVector3f.isZero(translation)) {
                notes.append("Tra[ ");
                if (translation.x != 0f) {
                    notes.append('x');
                }
                if (translation.y != 0f) {
                    notes.append('y');
                }
                if (translation.z != 0f) {
                    notes.append('z');
                }
                notes.append(" ]");
            }
            Quaternion rotation = spatial.localRotation(null);
            if (!MyQuaternion.isRotationIdentity(rotation)) {
                if (notes.length() > 0) {
                    notes.append("  ");
                }
                notes.append("Rot");
            }
            Vector3f scale = spatial.localScale(null);
            if (!MyVector3f.isScaleIdentity(scale)) {
                if (notes.length() > 0) {
                    notes.append("  ");
                }
                notes.append("Sca[ ");
                if (scale.x != 1f) {
                    notes.append('x');
                }
                if (scale.y != 1f) {
                    notes.append('y');
                }
                if (scale.z != 1f) {
                    notes.append('z');
                }
                notes.append(" ]");
            }
            if (notes.length() == 0) {
                notes.append("Identity");
            }
            transformText = notes.toString();
        }

        Maud.gui.setStatusText("spatialTransform", " " + transformText);
    }

    /**
     * Update the display of the spatial's type.
     */
    private void updateType() {
        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        String typeText = spatial.describeType();
        Maud.gui.setStatusText("spatialType", typeText);
    }
}
