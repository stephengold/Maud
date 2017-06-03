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
 * Stephen Gold's name may not be used to endorse or promote products
 derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL STEPHEN GOLD BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package maud;

import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.bounding.BoundingVolume;
import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.WireBox;
import com.jme3.util.clone.Cloner;
import java.util.logging.Logger;
import jme3utilities.MyAsset;
import jme3utilities.MySpatial;
import jme3utilities.SubtreeControl;
import jme3utilities.Validate;

/**
 * Subtree control to visualize the bounds of a spatial.
 * <p>
 * The controlled spatial must be a node, but the subject (visualized spatial)
 * may be a geometry.
 * <p>
 * The control is disabled by default. When enabled, it attaches a node and a
 * geometry to the controlled node.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class BoundsVisualizer extends SubtreeControl {
    // *************************************************************************
    // constants and loggers

    /**
     * true &rarr; enabled, false &rarr; disabled.
     *
     * The test provides depth cues, but might hide portions of the
     * visualization.
     */
    private boolean depthTest = false;
    /**
     * default color for lines (blue)
     */
    final private static ColorRGBA defaultLineColor = new ColorRGBA(
            0f, 0f, 1f, 1f);
    /**
     * default width for lines (in pixels)
     */
    final private static float defaultLineWidth = 2f;
    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            BoundsVisualizer.class.getName());
    // *************************************************************************
    // fields

    /**
     * line width (in pixels)
     */
    private float lineWidth;
    /**
     * material for lines/box
     */
    private Material lineMaterial;
    /**
     * the spatial whose bounds are being visualized, or null for none
     */
    private Spatial subject = null;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a disabled control.
     *
     * @param assetManager for loading material definitions (not null)
     */
    public BoundsVisualizer(AssetManager assetManager) {
        super();
        Validate.nonNull(assetManager, "asset manager");

        lineMaterial = MyAsset.createWireframeMaterial(
                assetManager, defaultLineColor);
        lineMaterial.getAdditionalRenderState().setDepthTest(false);
        setLineWidth(defaultLineWidth);

        assert !isEnabled();
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Copy the color of the lines.
     *
     * @return a new instance
     */
    public ColorRGBA copyLineColor() {
        MatParam parameter = lineMaterial.getParam("Color");
        ColorRGBA color = (ColorRGBA) parameter.getValue();

        return color.clone();
    }

    /**
     * Read the depth test setting.
     *
     * @return true if the test is enabled, otherwise false
     */
    public boolean getDepthTest() {
        return depthTest;
    }

    /**
     * Read the line width of the visualization.
     *
     * @return width (in pixels, &ge;1)
     */
    public float getLineWidth() {
        float result = lineMaterial.getAdditionalRenderState().getLineWidth();
        assert result >= 1f : result;
        return result;
    }

    /**
     * Alter the color of all lines.
     *
     * @param newColor (not null, unaffected)
     */
    public void setColor(ColorRGBA newColor) {
        Validate.nonNull(newColor, "new color");
        lineMaterial.setColor("Color", newColor.clone());
    }

    /**
     * Alter the depth test setting. The test provides depth cues, but might
     * hide portions of the visualization.
     *
     * @param newSetting true to enable test, false to disable it
     */
    final public void setDepthTest(boolean newSetting) {
        if (depthTest != newSetting) {
            Geometry box = (Geometry) subtree.getChild(0);
            Material material = box.getMaterial();
            RenderState state = material.getAdditionalRenderState();
            state.setDepthTest(newSetting);

            depthTest = newSetting;
        }
    }

    /**
     * Alter the line width of the visualization.
     *
     * @param width (in pixels, values &lt;1 hide the lines)
     */
    final public void setLineWidth(float width) {
        lineWidth = width;

        if (subtree != null) {
            Geometry box = (Geometry) subtree.getChild(0);
            if (lineWidth < 1f) {
                box.setCullHint(Spatial.CullHint.Always);
            } else {
                box.setCullHint(Spatial.CullHint.Inherit);
                lineMaterial.getAdditionalRenderState().setLineWidth(lineWidth);
            }
        }
    }

    /**
     * Alter the spatial being visualized.
     *
     * @param newSubject which spatial to visualize (may be null, alias created)
     */
    public void setSubject(Spatial newSubject) {
        subject = newSubject;

        if (subtree != null) {
            subtree.detachAllChildren();

            String namePrefix = "";
            if (spatial != null) {
                namePrefix = spatial.getName() + " ";
            }
            String boxName = namePrefix + "box";
            WireBox boxMesh = new WireBox();
            Geometry box = new Geometry(boxName, boxMesh);
            box.setMaterial(lineMaterial);
            subtree.attachChild(box);
        }
    }
    // *************************************************************************
    // AbstractControl methods

    /**
     * Callback invoked when the spatial's geometric state is about to be
     * updated, once per frame while attached and enabled.
     *
     * @param updateInterval time interval between updates (in seconds, &ge;0)
     */
    @Override
    protected void controlUpdate(float updateInterval) {
        super.controlUpdate(updateInterval);

        if (subject != null) {
            BoundingVolume bound = subject.getWorldBound();
            if (bound instanceof BoundingBox) {
                BoundingBox boundingBox = (BoundingBox) bound;

                Vector3f center = boundingBox.getCenter();
                Geometry box = (Geometry) subtree.getChild(0);
                MySpatial.setWorldLocation(box, center);

                float xExtent = boundingBox.getXExtent();
                float yExtent = boundingBox.getYExtent();
                float zExtent = boundingBox.getZExtent();
                WireBox boxMesh = (WireBox) box.getMesh();
                boxMesh.updatePositions(xExtent, yExtent, zExtent);

            } else if (bound instanceof BoundingSphere) {
                BoundingSphere boundingSphere = (BoundingSphere) bound;
                Vector3f center = boundingSphere.getCenter();
                float radius = boundingSphere.getRadius();
                // TODO visualize the sphere
                assert radius >= 0f : radius;
                assert center != null;
                throw new UnsupportedOperationException();
            }
        }
    }

    /**
     * Alter the visibility of the visualization.
     *
     * @param newState if true, reveal the visualization; if false, hide it
     */
    @Override
    public void setEnabled(boolean newState) {
        if (newState && subtree == null) {
            /*
             * Before enabling this control for the first time,
             * create the subtree.
             */
            String nodeName = spatial.getName() + " bounds";
            subtree = new Node(nodeName);
            subtree.setQueueBucket(RenderQueue.Bucket.Transparent);
            subtree.setShadowMode(RenderQueue.ShadowMode.Off);

            setSubject(null);
            setLineWidth(lineWidth);
        }

        super.setEnabled(newState);
    }

    /**
     * Alter which node is controlled.
     *
     * @param newNode the node to control (or null)
     */
    @Override
    public void setSpatial(Spatial newNode) {
        super.setSpatial(newNode);
        setSubject(null);
    }
    // *************************************************************************
    // JmeCloneable methods

    /**
     * Convert this shallow-cloned control into a deep-cloned one, using the
     * specified cloner and original to resolve copied fields.
     *
     * @param cloner the cloner currently cloning this control
     * @param original the control from which this control was shallow-cloned
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        super.cloneFields(cloner, original);

        lineMaterial = cloner.clone(lineMaterial);
        subject = cloner.clone(subject);
    }
    // *************************************************************************
    // Object methods

    /**
     * Create a shallow copy of this control.
     *
     * @return a new control, equivalent to this one
     * @throws CloneNotSupportedException if superclass isn't cloneable
     */
    @Override
    public BoundsVisualizer clone() throws CloneNotSupportedException {
        BoundsVisualizer clone = (BoundsVisualizer) super.clone();
        return clone;
    }
}
