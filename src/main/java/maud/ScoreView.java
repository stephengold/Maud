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

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Line;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.util.logging.Logger;
import jme3utilities.MyAsset;
import jme3utilities.Rectangle;
import jme3utilities.Validate;
import maud.model.LoadedCgm;

/**
 * A 2D visualization of a loaded animation in a view port.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class ScoreView implements JmeCloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * end-cap mesh for bone tracks without scaling
     */
    final private static Finial finialNoScales = new Finial(true, true, false);
    /**
     * end-cap mesh for bone tracks with scaling
     */
    final private static Finial finialWithScales = new Finial(true, true, true);
    /**
     * Z-coordinate for lines
     */
    final private static float z = -10f;
    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            ScoreView.class.getName());
    // *************************************************************************
    // fields

    /**
     * background color for this view
     */
    private final ColorRGBA backgroundColor = new ColorRGBA(
            0.84f, 0.84f, 0.72f, 1f);
    /**
     * height of the score (in world units, &gt;0)
     */
    private float height = 1f;
    /**
     * CG model that owns this view (not null)
     */
    private LoadedCgm cgm;
    /**
     * material for borders
     */
    private Material border = null;
    /**
     * visualization subtree
     */
    private Node visuals = new Node("score view");
    /**
     * view port used when the screen is not split, or null for none
     */
    private ViewPort viewPort1 = null;
    /**
     * view port used when the screen is split (not null)
     */
    final private ViewPort viewPort2;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a new visualization.
     *
     * @param loadedCgm loaded CG model that will own this view (not null, alias
     * created)
     * @param port1 initial view port, or null for none (alias created)
     * @param port2 view port to use after the screen is split (not null, alias
     * created)
     */
    public ScoreView(LoadedCgm loadedCgm, ViewPort port1, ViewPort port2) {
        Validate.nonNull(loadedCgm, "loaded model");
        Validate.nonNull(port2, "view port2");

        cgm = loadedCgm;
        viewPort1 = port1;
        viewPort2 = port2;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Access the camera used to render the score.
     *
     * @return a pre-existing instance, or null if none
     */
    public Camera getCamera() {
        Camera result = null;

        ViewPort viewPort = getViewPort();
        if (viewPort != null) {
            result = viewPort.getCamera();
        }

        return result;
    }

    /**
     * Read the height of the score.
     *
     * @return height (in world units, &gt;0)
     */
    public float getHeight() {
        assert height > 0f : height;
        return height;
    }

    /**
     * Access the view port being used to render the score.
     *
     * @return a pre-existing, enabled view port, or null if none
     */
    public ViewPort getViewPort() {
        ViewPort result;
        if (Maud.model.source.isLoaded()) {
            result = viewPort2;
        } else {
            result = viewPort1;
        }

        return result;
    }

    /**
     * Alter which loaded CG model corresponds with this view. (Invoked after
     * cloning.)
     *
     * @param loadedCgm (not null)
     */
    public void setCgm(LoadedCgm loadedCgm) {
        Validate.nonNull(loadedCgm, "loaded model");
        cgm = loadedCgm;
    }

    /**
     * Update prior to rendering. (Invoked once per render pass on each
     * instance.)
     */
    void update() {
        ViewPort viewPort = getViewPort();
        if (viewPort != null && viewPort.isEnabled()) {
            assert cgm.isLoaded();
            viewPort.setBackgroundColor(backgroundColor);

            Spatial parentSpatial = viewPort.getScenes().get(0);
            Node parent = (Node) parentSpatial;
            parent.detachAllChildren();

            if (border == null) {
                Maud application = Maud.getApplication();
                AssetManager assetManager = application.getAssetManager();
                ColorRGBA black = new ColorRGBA(0f, 0f, 0f, 1f);
                border = MyAsset.createWireframeMaterial(assetManager, black);
            }

            height = 0f;
            int numBones = cgm.bones.countBones();
            for (int boneIndex = 0; boneIndex < numBones; boneIndex++) {
                float staffHeight = 0f;
                if (cgm.animation.hasTrackForBone(boneIndex)) {
                    Finial finial;
                    if (cgm.animation.hasScales(boneIndex)) {
                        finial = finialWithScales;
                    } else {
                        finial = finialNoScales;
                    }
                    staffHeight = finial.getHeight();

                    String name;
                    Geometry geom;

                    name = String.format("left finial%d", boneIndex);
                    geom = new Geometry(name, finial);
                    geom.setLocalTranslation(0f, -height, z);
                    geom.setMaterial(border);
                    parent.attachChild(geom);

                    name = String.format("right finial%d", boneIndex);
                    geom = new Geometry(name, finial);
                    geom.setLocalTranslation(1f, -height, z);
                    geom.setLocalScale(-1f, 1f, 1f);
                    geom.setMaterial(border);
                    parent.attachChild(geom);

                    Rectangle staff = new Rectangle(0f, 1f, 0f, 1f,
                            0f, 1f, 0f, -staffHeight, 1f);
                    name = String.format("staff%d", boneIndex);
                    geom = new Geometry(name, staff);
                    geom.setLocalTranslation(0f, -height, z);
                    geom.setMaterial(border);
                    parent.attachChild(geom);
                }

                height += staffHeight;
            }
            height += 0.2f;

            Geometry gnomon = makeGnomon();
            parent.attachChild(gnomon);
        }
    }
    // *************************************************************************
    // JmeCloner methods

    /**
     * Convert this shallow-cloned instance into a deep-cloned one, using the
     * specified cloner and original to resolve copied fields.
     *
     * @param cloner the cloner currently cloning this control (not null)
     * @param original the control from which this control was shallow-cloned
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        visuals = cloner.clone(visuals);
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public ScoreView jmeClone() {
        try {
            ScoreView clone = (ScoreView) super.clone();
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException(exception);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Create a time indicator.
     *
     * @return a new orphaned geometry
     */
    private Geometry makeGnomon() {
        float time = cgm.animation.getTime();
        float duration = cgm.animation.getDuration();
        float x = 0f;
        if (duration > 0f) {
            x = time / duration;
        }
        Vector3f start = new Vector3f(x, 0.3f, 0f);
        Vector3f end = new Vector3f(x, -height, 0f);
        Line line = new Line(start, end);

        Geometry result = new Geometry("gnomon", line);
        result.setLocalTranslation(0f, 0f, z);
        result.setMaterial(border);

        return result;
    }
}
