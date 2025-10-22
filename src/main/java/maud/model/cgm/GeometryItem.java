/*
 Copyright (c) 2021-2025 Stephen Gold
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
package maud.model.cgm;

import com.jme3.light.LightList;
import com.jme3.material.MatParamOverride;
import com.jme3.material.Material;
import com.jme3.math.Transform;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyMesh;
import jme3utilities.MyString;
import maud.MaudUtil;

/**
 * Useful information about a particular Geometry.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class GeometryItem {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(GeometryItem.class.getName());
    // *************************************************************************
    // fields

    /**
     * Geometry in the MVC model
     */
    final private Geometry geometry;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a new item.
     *
     * @param geometry (not null, alias created)
     */
    GeometryItem(Geometry geometry) {
        assert geometry != null;
        this.geometry = geometry;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Access the Geometry itself.
     *
     * @return the pre-existing instance (not null)
     */
    Geometry getGeometry() {
        return geometry;
    }

    /**
     * Determine whether a GeometryItem is ready for merging: no S-G controls,
     * no user data, no local lights, no M-P overrides, and no LODs.
     *
     * @return "" if compatible, otherwise a non-null, non-empty message
     */
    public String mergeFeedback() {
        String name = geometry.getName();
        String quotedName = MyString.quote(name);

        int numSgcs = geometry.getNumControls();
        if (numSgcs != 0) {
            return quotedName + " has controls";
        }

        Collection<String> userDataKeys = geometry.getUserDataKeys();
        if (!userDataKeys.isEmpty()) {
            return quotedName + " has user data";
        }

        LightList lights = geometry.getLocalLightList();
        if (lights.size() != 0) {
            return quotedName + " has local lights";
        }

        List<MatParamOverride> mpos = geometry.getLocalMatParamOverrides();
        if (!mpos.isEmpty()) {
            return quotedName + " has overrides";
        }

        Mesh mesh = geometry.getMesh();
        int numLevels = mesh.getNumLodLevels();
        if (numLevels > 0) {
            return quotedName + " has LODs";
        }

        assert MaudUtil.isBare(geometry);
        return "";
    }

    /**
     * Determine whether two geometry items are compatible for merging: same
     * hints, same material, same transform, etcetera.
     *
     * @param item1 the item to test against (not null, unaffected)
     * @return "" if compatible, otherwise a non-null, non-empty message
     */
    public String mergeFeedback(GeometryItem item1) {
        Geometry geometry1 = item1.getGeometry();

        boolean ignoreTransform = geometry.isIgnoreTransform();
        boolean ignoreTransform1 = geometry1.isIgnoreTransform();
        if (ignoreTransform != ignoreTransform1) {
            return "ignore-transform flags differ";
        }

        Material material = geometry.getMaterial();
        Material material1 = geometry1.getMaterial();
        if (material != material1) {
            return "materials differ";
        }

        Spatial.BatchHint batchHint = geometry.getLocalBatchHint();
        Spatial.BatchHint batchHint1 = geometry1.getLocalBatchHint();
        if (batchHint != batchHint1) {
            return "batch hints differ";
        }

        Spatial.CullHint cullHint = geometry.getLocalCullHint();
        Spatial.CullHint cullHint1 = geometry1.getLocalCullHint();
        if (cullHint != cullHint1) {
            return "cull hints differ";
        }

        RenderQueue.Bucket renderBucket = geometry.getLocalQueueBucket();
        RenderQueue.Bucket renderBucket1 = geometry1.getLocalQueueBucket();
        if (renderBucket != renderBucket1) {
            return "render buckets differ";
        }

        RenderQueue.ShadowMode shadowMode = geometry.getLocalShadowMode();
        RenderQueue.ShadowMode shadowMode1 = geometry1.getLocalShadowMode();
        if (shadowMode != shadowMode1) {
            return "shadow modes differ";
        }

        Transform transform = geometry.getLocalTransform(); // alias
        Transform transform1 = geometry1.getLocalTransform(); // alias
        if (!transform.equals(transform1)) {
            return "transforms differ";
        }

        Mesh mesh = geometry.getMesh();
        Mesh mesh1 = geometry1.getMesh();

        Mesh.Mode primitive = MyMesh.expandedMode(mesh);
        Mesh.Mode primitive1 = MyMesh.expandedMode(mesh1);
        if (primitive != primitive1) {
            return "mesh primitives differ";
        }

        for (VertexBuffer.Type type : VertexBuffer.Type.values()) {
            if (type == VertexBuffer.Type.Index) {
                continue;
            }
            VertexBuffer vb = mesh.getBuffer(type);
            VertexBuffer vb1 = mesh1.getBuffer(type);
            if (vb == null && vb1 == null) {
                continue;
            }
            if (vb == null || vb1 == null) {
                return "different vertex buffers";
            }

            int numCperE = vb.getNumComponents();
            int numCperE1 = vb1.getNumComponents();
            if (numCperE != numCperE1) {
                return "components per element differ";
            }
        }

        return "";
    }
    // *************************************************************************
    // Object methods

    /**
     * Represent the item as a text string.
     *
     * @return a descriptive string of text (not null, not empty)
     */
    @Override
    public String toString() {
        String result = describe();
        return result;
    }
    // *************************************************************************
    // private methods

    /**
     * Describe the Geometry.
     *
     * @return a descriptive string of text (not null, not empty)
     */
    private String describe() {
        String name = geometry.getName();
        int hash = geometry.getMaterial().hashCode() & 0xFFFF;
        String description = String.format("%s (mat=%04x)", name, hash);

        assert description != null;
        assert !description.isEmpty();
        return description;
    }
}
