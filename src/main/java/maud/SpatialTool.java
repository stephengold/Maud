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

import com.jme3.material.Material;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.terrain.geomipmap.TerrainQuad;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.nifty.BasicScreenController;

/**
 * The controller for the "Spatial Tool" window in Maud's "3D View" screen.
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
    // fields

    /**
     * the tree position of the selected spatial (not null)
     */
    final private List<Integer> treePosition = new ArrayList<>(3);
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
    // new methods exposed

    /**
     * Access the selected spatial.
     *
     * @return the pre-existing spatial (not null)
     */
    Spatial getSelectedSpatial() {
        Spatial result = Maud.model.getRootSpatial();
        for (int position : treePosition) {
            Node node = (Node) result;
            result = node.getChild(position);
        }

        assert result != null;
        return result;
    }

    /**
     * Select (by index) a child of the selected spatial and update this window.
     */
    void selectChildSpatial(int childIndex) {
        Spatial selectedSpatial = getSelectedSpatial();
        Node node = (Node) selectedSpatial;
        Spatial child = node.getChild(childIndex);
        if (child != null) {
            treePosition.add(childIndex);
            assert getSelectedSpatial() == child;
            update();
        }
    }

    /**
     * Select the parent of the selected spatial and update this window.
     */
    void selectParentSpatial() {
        Spatial selectedSpatial = getSelectedSpatial();
        Node parent = selectedSpatial.getParent();
        if (parent != null) {
            int last = treePosition.size() - 1;
            treePosition.remove(last);
            assert getSelectedSpatial() == parent;
            update();
        }
    }

    /**
     * Select the model's root spatial and update this window.
     */
    void selectRootSpatial() {
        treePosition.clear();
        update();
    }

    /**
     * Update the entire window after a change.
     */
    void update() {
        updateTreePosition();

        Spatial spatial = getSelectedSpatial();
        updateBucket(spatial);
        updateChildren(spatial);
        updateControls(spatial);
        updateHint(spatial);
        updateKeys(spatial);
        updateMaterial(spatial);
        updateMesh(spatial);
        updateName(spatial);
        updateParent(spatial);
        updateShadows(spatial);
        updateType(spatial);
    }
    // *************************************************************************
    // private methods

    /**
     * Update the information about the selected spatial's render-queue bucket.
     *
     * @param spatial the selected spatial (not null)
     */
    private void updateBucket(Spatial spatial) {
        RenderQueue.Bucket bucket = spatial.getLocalQueueBucket();
        String bucketText = bucket.toString();
        if (bucket == RenderQueue.Bucket.Inherit) {
            bucket = spatial.getQueueBucket();
            bucketText += String.format(": %s", bucket.toString());
        }
        Maud.gui.setStatusText("spatialBucket", " " + bucketText);
    }

    /**
     * Update the information about the selected spatial's children.
     *
     * @param spatial the selected spatial (not null)
     */
    private void updateChildren(Spatial spatial) {
        String childrenText, scButton;
        if (spatial instanceof Node) {
            Node node = (Node) spatial;
            List<Spatial> children = node.getChildren();
            int numChildren = children.size();
            if (numChildren > 1) {
                childrenText = String.format("%d children", numChildren);
                scButton = "Select";
            } else if (numChildren == 1) {
                String childName = children.get(0).getName();
                childrenText = MyString.quote(childName);
                scButton = "Select";
            } else {
                childrenText = "none";
                scButton = "";
            }
        } else {
            childrenText = "n/a";
            scButton = "";
        }
        Maud.gui.setStatusText("spatialChildren", " " + childrenText);
        Maud.gui.setButtonLabel("spatialSelectChildButton", scButton);
    }

    /**
     * Update the information about selected spatial's controls.
     *
     * @param spatial the selected spatial (not null)
     */
    private void updateControls(Spatial spatial) {
        int numControls = spatial.getNumControls();
        String controlsText = String.format("%d", numControls);
        Maud.gui.setStatusText("spatialControls", " " + controlsText);
    }

    /**
     * Update the information about selected spatial's cull hints.
     *
     * @param spatial the selected spatial (not null)
     */
    private void updateHint(Spatial spatial) {
        Spatial.CullHint hint = spatial.getLocalCullHint();
        String hintText = hint.toString();
        if (hint == Spatial.CullHint.Inherit) {
            hint = spatial.getCullHint();
            hintText += String.format(": %s", hint.toString());
        }
        Maud.gui.setStatusText("spatialHint", " " + hintText);
    }

    /**
     * Update the information about selected spatial's user data keys.
     *
     * @param spatial the selected spatial (not null)
     */
    private void updateKeys(Spatial spatial) {
        Collection<String> keys = spatial.getUserDataKeys();
        int numKeys = keys.size();
        String keysText = String.format("%d", numKeys);
        Maud.gui.setStatusText("spatialKeys", " " + keysText);
    }

    /**
     * Update the information about the selected spatial's material, if any.
     *
     * @param spatial the selected spatial (not null)
     */
    private void updateMaterial(Spatial spatial) {
        String materialText;
        if (spatial instanceof Geometry) {
            Geometry geometry = (Geometry) spatial;
            Material material = geometry.getMaterial();
            if (material == null) {
                materialText = "none";
            } else {
                String materialName = material.getName();
                if (materialName == null) {
                    materialText = "nameless";
                } else {
                    materialText = MyString.quote(materialName);
                }
            }
        } else {
            materialText = "n/a";
        }
        Maud.gui.setStatusText("spatialMaterial", " " + materialText);
    }

    /**
     * Update the information about the selected spatial's mesh, if any.
     *
     * @param spatial the selected spatial (not null)
     */
    private void updateMesh(Spatial spatial) {
        String meshText;
        if (spatial instanceof Geometry) {
            Geometry geometry = (Geometry) spatial;
            Mesh mesh = geometry.getMesh();
            if (mesh == null) {
                meshText = "none";
            } else {
                if (mesh.isAnimated()) {
                    meshText = "animated";
                } else {
                    meshText = "non-animated";
                }
                Mesh.Mode mode = mesh.getMode();
                meshText += String.format(" %s", mode.toString());
                int numVertices = mesh.getVertexCount();
                meshText += String.format(", %d verts, ", numVertices);
                int numLevels = mesh.getNumLodLevels();
                if (numLevels == 1) {
                    meshText += "one LoD";
                } else {
                    meshText += String.format("%d LoDs", numLevels);
                }
            }
        } else {
            meshText = "n/a";
        }
        Maud.gui.setStatusText("spatialMesh", " " + meshText);
    }

    /**
     * Update the information about the selected spatial's name.
     *
     * @param spatial the selected spatial (not null)
     */
    private void updateName(Spatial spatial) {
        String name = spatial.getName();
        String nameText;
        if (name == null) {
            nameText = "null";
        } else {
            nameText = MyString.quote(name);
        }
        Maud.gui.setStatusText("spatialName", " " + nameText);
    }

    /**
     * Update the information about the selected spatial's parent.
     *
     * @param spatial the selected spatial (not null)
     */
    private void updateParent(Spatial spatial) {
        Node parent = spatial.getParent();
        String parentText, spButton;
        if (parent == null) {
            parentText = "n/a";
            spButton = "";
        } else {
            String parentName = parent.getName();
            parentText = MyString.quote(parentName);
            spButton = "Select";
        }
        Maud.gui.setStatusText("spatialParent", " " + parentText);
        Maud.gui.setButtonLabel("spatialSelectParentButton", spButton);
    }

    /**
     * Update the information about the selected spatial's shadow mode.
     *
     * @param spatial the selected spatial (not null)
     */
    private void updateShadows(Spatial spatial) {
        RenderQueue.ShadowMode mode = spatial.getLocalShadowMode();
        String shadowsText = mode.toString();
        if (mode == RenderQueue.ShadowMode.Inherit) {
            mode = spatial.getShadowMode();
            shadowsText += String.format(": %s", mode.toString());
        }
        Maud.gui.setStatusText("spatialShadows", " " + shadowsText);
    }

    /**
     * Update the information about the selected spatial's position in the
     * model's scene graph.
     */
    private void updateTreePosition() {
        String positionText;
        if (treePosition.isEmpty()) {
            positionText = " model root";
        } else {
            positionText = treePosition.toString();
        }
        Maud.gui.setStatusText("spatialTreePosition", positionText);
    }

    /**
     * Update the information about the selected spatial's type.
     *
     * @param spatial the selected spatial (not null)
     */
    private void updateType(Spatial spatial) {
        String typeText;
        if (spatial instanceof TerrainQuad) {
            typeText = "TerQuad";
        } else if (spatial instanceof Node) {
            typeText = "Node";
        } else if (spatial instanceof Geometry) {
            typeText = "Geometry";
        } else {
            typeText = "unknown type";
        }
        Maud.gui.setStatusText("spatialType", typeText);
    }
}
