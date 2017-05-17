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
package maud.model;

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
import maud.Maud;

/**
 * The MVC model of the selected spatial in the Maud application.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SelectedSpatial implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            SelectedSpatial.class.getName());
    // *************************************************************************
    // fields

    /**
     * tree position of the selected spatial (not null)
     */
    private List<Integer> treePosition = new ArrayList<>(3);
    // *************************************************************************
    // new methods exposed

    /**
     * Enumerate all user data keys of the selected spatial.
     *
     * @return a new array
     */
    public String[] copyUserDataKeys() {
        Spatial spatial = modelSpatial();
        Collection<String> keys = spatial.getUserDataKeys();
        int numKeys = keys.size();
        String[] result = new String[numKeys];
        keys.toArray(result);

        return result;
    }

    /**
     * Count how many children are attached to the selected spatial.
     *
     * @return count (&ge;0) or 0 if the spatial is not a node
     */
    public int countChildren() {
        Spatial parent = modelSpatial();

        int result;
        if (parent instanceof Node) {
            Node node = (Node) parent;
            List<Spatial> children = node.getChildren();
            result = children.size();
        } else {
            result = 0;
        }

        assert result >= 0 : result;
        return result;
    }

    /**
     * Count how many controls are added to the selected spatial.
     *
     * @return count (&ge;0)
     */
    public int countControls() {
        Spatial spatial = modelSpatial();
        int result = spatial.getNumControls();

        assert result >= 0 : result;
        return result;
    }

    /**
     * Count how many levels of detail are in the selected spatial's mesh.
     *
     * @return count (&ge;0)
     */
    public int countLoDLevels() {
        Mesh mesh = mesh();
        int result;
        if (mesh == null) {
            result = 0;
        } else {
            result = mesh.getNumLodLevels();
        }

        assert result >= 0 : result;
        return result;
    }

    /**
     * Count how many vertices are in the selected spatial's mesh.
     *
     * @return count (&ge;0)
     */
    public int countVertices() {
        Mesh mesh = mesh();
        int result;
        if (mesh == null) {
            result = 0;
        } else {
            result = mesh.getVertexCount();
        }

        assert result >= 0 : result;
        return result;
    }

    /**
     * Describe the type of the selected spatial.
     *
     * @return textual description (not null)
     */
    public String describeType() {
        Spatial spatial = modelSpatial();

        String typeText;
        if (spatial instanceof Geometry) {
            typeText = "Geometry";
        } else if (spatial instanceof TerrainQuad) {
            typeText = "TerQuad";
        } else if (spatial instanceof Node) {
            typeText = "Node";
        } else {
            typeText = "unknown";
        }

        return typeText;
    }

    /**
     * Access the selected spatial in a CG model.
     *
     * @param modelRoot root of the CG model (not null)
     * @return the pre-existing instance (not null)
     */
    public Spatial findSpatial(Spatial modelRoot) {
        Spatial result = modelRoot;
        for (int childPosition : treePosition) {
            Node node = (Node) result;
            result = node.getChild(childPosition);
        }

        assert result != null;
        return result;
    }

    /**
     * Read the name of an indexed child of the selected spatial.
     *
     * @param childIndex which child (&ge;0)
     * @return name, or null if none
     */
    public String getChildName(int childIndex) {
        assert childIndex >= 0 : childIndex;

        Spatial child = modelChild(childIndex);
        String result;
        if (child == null) {
            result = null;
        } else {
            result = child.getName();
        }

        return result;
    }

    /**
     * Read the effective cull hint of the selected spatial.
     *
     * @return hint (not null)
     */
    public Spatial.CullHint getCullHint() {
        Spatial spatial = modelSpatial();
        Spatial.CullHint result = spatial.getCullHint();

        assert result != null;
        return result;
    }

    /**
     * Read the local cull hint of the selected spatial.
     *
     * @return hint (not null)
     */
    public Spatial.CullHint getLocalCullHint() {
        Spatial spatial = modelSpatial();
        Spatial.CullHint result = spatial.getLocalCullHint();

        assert result != null;
        return result;
    }

    /**
     * Read the local render bucket of the selected spatial.
     *
     * @return bucket (not null)
     */
    public RenderQueue.Bucket getLocalQueueBucket() {
        Spatial spatial = modelSpatial();
        RenderQueue.Bucket result = spatial.getLocalQueueBucket();

        assert result != null;
        return result;
    }

    /**
     * Read the local shadow mode of the selected spatial.
     *
     * @return mode (not null)
     */
    public RenderQueue.ShadowMode getLocalShadowMode() {
        Spatial spatial = modelSpatial();
        RenderQueue.ShadowMode result = spatial.getLocalShadowMode();

        assert result != null;
        return result;
    }

    /**
     * Read the name of the selected spatial's material.
     *
     * @return name, or null if none
     */
    public String getMaterialName() {
        Material material = material();
        String result;
        if (material == null) {
            result = null;
        } else {
            result = material.getName();
        }

        return result;
    }

    /**
     * Read the mode of the selected spatial's mesh.
     *
     * @return the mode of the mesh, or null if none
     */
    public Mesh.Mode getMeshMode() {
        Mesh mesh = mesh();
        Mesh.Mode result;
        if (mesh == null) {
            result = null;
        } else {
            result = mesh.getMode();
        }

        return result;
    }

    /**
     * Read the name of the selected spatial.
     *
     * @return name, or null if none
     */
    public String getName() {
        Spatial spatial = modelSpatial();
        String result;
        if (spatial == null) {
            result = null;
        } else {
            result = spatial.getName();
        }

        return result;
    }

    /**
     * Read the name of the parent of the selected spatial.
     *
     * @return name, or null if none
     */
    public String getParentName() {
        Spatial spatial = modelSpatial();
        Spatial parent = spatial.getParent();
        String result;
        if (parent == null) {
            result = null;
        } else {
            result = parent.getName();
        }

        return result;
    }

    /**
     * Read the effective render bucket of the selected spatial.
     *
     * @return bucket (not null)
     */
    public RenderQueue.Bucket getQueueBucket() {
        Spatial spatial = modelSpatial();
        RenderQueue.Bucket result = spatial.getQueueBucket();

        assert result != null;
        return result;
    }

    /**
     * Read the effective shadow mode of the selected spatial.
     *
     * @return mode (not null)
     */
    public RenderQueue.ShadowMode getShadowMode() {
        Spatial spatial = modelSpatial();
        RenderQueue.ShadowMode result = spatial.getShadowMode();

        assert result != null;
        return result;
    }

    /**
     * Test whether the selected spatial has an animated mesh.
     *
     * @return true if it has an animated mesh, otherwise false
     */
    public boolean hasAnimatedMesh() {
        Mesh mesh = mesh();
        if (mesh == null) {
            return false;
        } else {
            return mesh.isAnimated();
        }
    }

    /**
     * Test whether the selected spatial has a material.
     *
     * @return true if it has a material, otherwise false
     */
    public boolean hasMaterial() {
        Material material = material();
        if (material == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether the selected spatial has a mesh.
     *
     * @return true if it has a mesh, otherwise false
     */
    public boolean hasMesh() {
        Mesh mesh = mesh();
        if (mesh == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether the selected spatial is a geometry.
     *
     * @return true if it's a geometry, otherwise false
     */
    public boolean isGeometry() {
        Spatial spatial = modelSpatial();
        if (spatial instanceof Geometry) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Test whether the selected spatial is the root of the CG model.
     *
     * @return true if it's the root, otherwise false
     */
    public boolean isModelRoot() {
        if (treePosition.isEmpty()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Test whether the selected spatial is a node.
     *
     * @return true if it's a node, otherwise false
     */
    public boolean isNode() {
        Spatial spatial = modelSpatial();
        if (spatial instanceof Node) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Select (by index) a child of the selected spatial.
     *
     * @param childIndex (&ge;0)
     */
    public void selectChild(int childIndex) {
        assert childIndex >= 0 : childIndex;

        Spatial child = modelChild(childIndex);
        if (child != null) {
            treePosition.add(childIndex);
            assert modelSpatial() == child;
        }
    }

    /**
     * Select the CG model's root spatial.
     */
    public void selectModelRoot() {
        treePosition.clear();
        assert modelSpatial() == Maud.model.cgm.getRootSpatial();
    }

    /**
     * Select the parent of the selected spatial.
     */
    public void selectParent() {
        Spatial selectedSpatial = modelSpatial();
        Node parent = selectedSpatial.getParent();
        if (parent != null) {
            int last = treePosition.size() - 1;
            treePosition.remove(last);
            assert modelSpatial() == parent;
        }
    }
    // TODO setters for CullHint, QueueBucket, and ShadowMode
    // *************************************************************************
    // Object methods

    /**
     * Create a deep copy of this object.
     *
     * @return a new object, equivalent to this one
     * @throws CloneNotSupportedException if superclass isn't cloneable
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        SelectedSpatial clone = (SelectedSpatial) super.clone();

        int numLevels = treePosition.size();
        clone.treePosition = new ArrayList<>(numLevels);
        for (Integer ci : treePosition) {
            clone.treePosition.add(ci);
        }

        return clone;
    }

    /**
     * Represent the selected spatial as a text string.
     *
     * @return descriptive string of text (not null)
     */
    @Override
    public String toString() {
        return treePosition.toString();
    }
    // *************************************************************************
    // private methods

    /**
     * Access the material of the selected spatial.
     *
     * @return the pre-existing instance, or null if none
     */
    private Material material() {
        Material result;
        Spatial spatial = modelSpatial();
        if (spatial instanceof Geometry) {
            Geometry geometry = (Geometry) spatial;
            result = geometry.getMaterial();
        } else {
            result = null;
        }

        return result;
    }

    /**
     * Access the mesh of the selected spatial.
     *
     * @return the pre-existing instance, or null if none
     */
    private Mesh mesh() {
        Mesh result;
        Spatial spatial = modelSpatial();
        if (spatial instanceof Geometry) {
            Geometry geometry = (Geometry) spatial;
            result = geometry.getMesh();
        } else {
            result = null;
        }

        return result;
    }

    /**
     * Access (by index) a child of the selected spatial in the MVC model.
     *
     * @param childIndex which child (&ge;0)
     * @return the pre-existing instance, or null if none
     */
    private Spatial modelChild(int childIndex) {
        assert childIndex >= 0 : childIndex;

        Spatial parent = modelSpatial();
        Spatial child;
        if (parent instanceof Node) {
            Node node = (Node) parent;
            child = node.getChild(childIndex);
        } else {
            child = null;
        }

        return child;
    }

    /**
     * Access the selected spatial in the MVC model.
     *
     * @return the pre-existing instance
     */
    private Spatial modelSpatial() {
        Spatial modelRoot = Maud.model.cgm.getRootSpatial();
        Spatial result = findSpatial(modelRoot);

        assert result != null;
        return result;
    }
}
