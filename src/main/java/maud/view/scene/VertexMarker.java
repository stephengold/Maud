/*
 Copyright (c) 2019, Stephen Gold
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
package maud.view.scene;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import java.util.logging.Logger;
import jme3utilities.MyAsset;
import jme3utilities.math.MyColor;
import jme3utilities.mesh.PointMesh;
import jme3utilities.ui.Locators;
import maud.Maud;
import maud.model.cgm.Cgm;
import maud.model.cgm.SelectedVertex;
import maud.model.option.scene.DddCursorOptions;
import maud.model.option.scene.VertexOptions;

/**
 * The vertex marker in a SceneView.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class VertexMarker {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(VertexMarker.class.getName());
    /**
     * asset path to the texture for the marker
     */
    final private static String markerAssetPath
            = "Textures/shapes/ring.png";
    // *************************************************************************
    // fields

    /**
     * elapsed time in the current color cycle (&ge;0)
     */
    private double colorTime = 0.0;
    /**
     * marker geometry, or null if none
     */
    private Geometry geometry;
    /**
     * view that owns this marker (not null, set by constructor or
     * {@link #setView(SceneViewCore)})
     */
    private SceneViewCore view;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a new vertex marker.
     *
     * @param owner the view that will own this cursor (not null, alias created)
     */
    VertexMarker(SceneViewCore owner) {
        assert owner != null;
        view = owner;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Alter which view owns this marker. (Invoked only when restoring a
     * checkpoint.)
     *
     * @param newView (not null, alias created)
     */
    void setView(SceneViewCore newView) {
        assert newView != null;
        assert newView != view;
        assert newView.getVertex() == this;

        view = newView;
    }

    /**
     * Update this marker based on the MVC model.
     *
     * @param cgm which C-G model (not null)
     * @param tpf time interval between frames (in seconds, &ge;0)
     */
    void update(Cgm cgm, float tpf) {
        if (geometry == null) {
            geometry = createGeometry();
        }

        DddCursorOptions cursorOptions = Maud.getModel().getScene().getCursor();
        float cycleTime = cursorOptions.getCycleTime();
        colorTime = (colorTime + tpf) % cycleTime;

        SelectedVertex vertex = cgm.getVertex();
        if (vertex.isSelected()) {
            Vector3f worldLocation = vertex.worldLocation(null);
            geometry.setLocalTranslation(worldLocation);

            double t = Math.sin(Math.PI * colorTime / cycleTime);
            double t2 = t * t;
            float fraction = (float) (t2 * t2); // 4th power of sine
            ColorRGBA color0 = cursorOptions.copyColor(0, null);
            ColorRGBA color1 = cursorOptions.copyColor(1, null);
            ColorRGBA newColor
                    = MyColor.interpolateLinear(fraction, color0, color1);
            Material material = geometry.getMaterial();
            material.setColor("Color", newColor); // note: creates alias

            VertexOptions options = Maud.getModel().getScene().getVertex();
            float pointSize = options.getPointSize();
            AppSettings current = Maud.getApplication().getSettings();
            int msaaSamples = current.getSamples();
            if (msaaSamples == 16) { // work around JME issue #878
                pointSize *= 2f;
            }
            material.setFloat("PointSize", pointSize);

            geometry.setCullHint(Spatial.CullHint.Never);
        } else {
            geometry.setCullHint(Spatial.CullHint.Always);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Create a marker geometry.
     *
     * @return a new, orphaned spatial
     */
    private Geometry createGeometry() {
        AssetManager assetManager = Locators.getAssetManager();
        Material material = new Material(assetManager,
                "MatDefs/wireframe/multicolor2.j3md");
        Texture poseShape
                = MyAsset.loadTexture(assetManager, markerAssetPath, false);
        material.setTexture("PointShape", poseShape);
        RenderState rs = material.getAdditionalRenderState();
        rs.setBlendMode(RenderState.BlendMode.Alpha);
        rs.setDepthTest(false);

        Mesh mesh = new PointMesh();
        Geometry geom = new Geometry("vertex", mesh);
        geom.setMaterial(material);
        geom.setQueueBucket(RenderQueue.Bucket.Transparent);
        view.attachToOverlayRoot(geom);

        return geom;
    }
}
