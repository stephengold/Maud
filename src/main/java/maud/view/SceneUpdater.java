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
package maud.view;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.post.SceneProcessor;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;
import java.util.BitSet;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyAsset;
import jme3utilities.MySpatial;
import jme3utilities.debug.AxesVisualizer;
import jme3utilities.debug.BoundsVisualizer;
import jme3utilities.debug.SkeletonVisualizer;
import jme3utilities.sky.SkyControl;
import jme3utilities.sky.Updater;
import jme3utilities.ui.Locators;
import maud.EditorViewPorts;
import maud.Maud;
import maud.model.cgm.Cgm;
import maud.model.cgm.SelectedLight;
import maud.model.cgm.SelectedVertex;
import maud.model.option.ShowBones;
import maud.model.option.scene.AxesDragEffect;
import maud.model.option.scene.AxesOptions;
import maud.model.option.scene.AxesSubject;
import maud.model.option.scene.BoundsOptions;
import maud.model.option.scene.DddCursorOptions;
import maud.model.option.scene.RenderOptions;
import maud.model.option.scene.SkeletonOptions;
import maud.model.option.scene.VertexOptions;

/**
 * Utility methods for updating a scene view.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class SceneUpdater {
    // *************************************************************************
    // constants and loggers

    /**
     * local copy of {@link com.jme3.math.ColorRGBA#BlackNoAlpha}
     */
    final private static ColorRGBA invisibleColor
            = new ColorRGBA(0f, 0f, 0f, 0f);
    /**
     * multiplier for ambient light
     */
    final private static float ambientMultiplier = 1f;
    /**
     * multiplier for main light
     */
    final private static float mainMultiplier = 2f;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SceneUpdater.class.getName());
    /**
     * asset path to the C-G model for the 3-D cursor
     */
    final private static String cursorAssetPath
            = "Models/indicators/3d cursor/3d cursor.j3o";
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private SceneUpdater() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Calculate the coordinate transform for the displayed axes.
     *
     * @param cgm (not null)
     * @return a new instance (in world coordinates) or null to hide the axes
     */
    static Transform axesTransform(Cgm cgm) {
        Transform transform = null;
        SceneView sceneView = cgm.getSceneView();
        AxesSubject subject = Maud.getModel().getScene().getAxes().getSubject();
        switch (subject) {
            case Model:
                if (cgm.isLoaded()) {
                    transform = sceneView.getTransform().worldTransform();
                }
                break;

            case None:
                break;

            case SelectedBone:
                if (cgm.getBone().isSelected()) {
                    transform = cgm.getBone().modelTransform(null);
                    Spatial tsp = sceneView.findTransformSpatial();
                    if (!MySpatial.isIgnoringTransforms(tsp)) {
                        Transform worldTransform = tsp.getWorldTransform();
                        transform.combineWithParent(worldTransform);
                    }
                }
                break;

            case SelectedLight:
                if (cgm.getLight().isSelected()) {
                    transform = cgm.getLight().transform(null);
                }
                break;

            case SelectedObject:
                if (cgm.getObject().isSelected()) {
                    transform = cgm.getObject().transform(null);
                }
                break;

            case SelectedShape:
                if (cgm.getShape().isSelected()) {
                    transform = cgm.getShape().transform(null);
                }
                break;

            case SelectedSpatial:
                if (cgm.isLoaded()) {
                    Spatial spatial = sceneView.selectedSpatial();
                    if (MySpatial.isIgnoringTransforms(spatial)) {
                        transform = new Transform(); // identity
                    } else {
                        transform = spatial.getWorldTransform();
                    }
                }
                break;

            case World:
                transform = new Transform(); // identity
                break;

            default:
                throw new IllegalStateException();
        }

        return transform;
    }

    /**
     * Update a scene view prior to rendering. Invoked once per render pass on
     * each scene view.
     *
     * @param viewCgm which C-G model occupies the view (not null)
     */
    static void update(Cgm viewCgm) {
        assert viewCgm.getSceneView().getCamera() != null;

        viewCgm.getScenePov().updateCamera();
        updateAxes(viewCgm);
        updateBounds(viewCgm);
        updateCursor(viewCgm);
        updatePhysics(viewCgm);
        viewCgm.getSceneView().getPlatform().update();
        updateShadows(viewCgm);
        updateSkeleton(viewCgm);
        updateSky(viewCgm);
        updateVertex(viewCgm);
    }
    // *************************************************************************
    // private methods

    /**
     * Create a star-shaped 3-D cursor.
     *
     * @return a new, orphaned spatial
     */
    private static Geometry createCursor() {
        AssetManager assetManager = Locators.getAssetManager();
        Node node = (Node) assetManager.loadModel(cursorAssetPath);
        Node node2 = (Node) node.getChild(0);
        Node node3 = (Node) node2.getChild(0);
        Geometry result = (Geometry) node3.getChild(0);

        result.removeFromParent();

        Material material = MyAsset.createUnshadedMaterial(assetManager);
        result.setMaterial(material);

        return result;
    }

    /**
     * Update the axes visualizer based on the MVC model.
     *
     * @param cgm which C-G model (not null)
     */
    private static void updateAxes(Cgm cgm) {
        AxesVisualizer visualizer = cgm.getSceneView().getAxesVisualizer();
        Transform transform = axesTransform(cgm);
        if (transform == null) {
            visualizer.setEnabled(false);
        } else {
            Node axesNode = (Node) visualizer.getSpatial();
            axesNode.setLocalTransform(transform);
            visualizer.setEnabled(true);

            Vector3f axesOrigin = transform.getTranslation();
            Vector3f cameraLocation = cgm.getScenePov().cameraLocation(null);
            float distance = axesOrigin.distance(cameraLocation);
            float length = 0.2f * distance;
            visualizer.setAxisLength(length);

            AxesOptions options = Maud.getModel().getScene().getAxes();
            boolean depthTestFlag = options.getDepthTestFlag();
            visualizer.setDepthTest(depthTestFlag);

            float lineWidth = options.getLineWidth();
            visualizer.setLineWidth(lineWidth);

            int numAxes;
            AxesSubject subject = options.getSubject();
            AxesDragEffect effect = options.getDragEffect();
            SelectedLight selectedLight = cgm.getLight();
            if (subject.equals(AxesSubject.SelectedLight)
                    && selectedLight.canDirect()
                    && effect.equals(AxesDragEffect.Rotate)) {
                numAxes = 1;
            } else {
                numAxes = 3;
            }
            visualizer.setNumAxes(numAxes);
        }
    }

    /**
     * Update the bounds visualizer based on the MVC model.
     *
     * @param cgm which C-G model (not null)
     */
    private static void updateBounds(Cgm cgm) {
        SceneView sceneView = cgm.getSceneView();
        BoundsVisualizer visualizer = sceneView.getBoundsVisualizer();
        visualizer.setEnabled(true);

        BoundsOptions options = Maud.getModel().getScene().getBounds();
        ColorRGBA color = options.copyColor(null);
        visualizer.setColor(color);

        boolean depthTestFlag = options.getDepthTestFlag();
        visualizer.setDepthTest(depthTestFlag);

        float lineWidth = options.getLineWidth();
        visualizer.setLineWidth(lineWidth);

        Spatial selectedSpatial = sceneView.selectedSpatial();
        visualizer.setSubject(selectedSpatial);
    }

    /**
     * Update the 3-D cursor based on the MVC model.
     *
     * @param cgm which C-G model (not null)
     */
    private static void updateCursor(Cgm cgm) {
        SceneView sceneView = cgm.getSceneView();
        Geometry cursor = sceneView.getCursor();
        /*
         * visibility
         */
        boolean wasVisible = (cursor != null);
        DddCursorOptions options = Maud.getModel().getScene().getCursor();
        boolean visible = options.isVisible();
        if (wasVisible && !visible) {
            sceneView.setCursor(null);
            cursor = null;
        } else if (!wasVisible && visible) {
            cursor = createCursor();
            sceneView.setCursor(cursor);
        }

        if (cursor != null) {
            /*
             * color
             */
            ColorRGBA newColor = options.copyColor(null);
            Material material = cursor.getMaterial();
            material.setColor("Color", newColor); // note: creates alias
            /*
             * location
             */
            Vector3f newLocation = cgm.getScenePov().cursorLocation(null);
            MySpatial.setWorldLocation(cursor, newLocation);
            /*
             * scale
             */
            float newScale = cgm.getScenePov().worldScaleForCursor();
            if (newScale > 0f) {
                MySpatial.setWorldScale(cursor, newScale);
            }
        }
    }

    /**
     * Update the physics visualization based on the MVC model.
     *
     * @param cgm which C-G model (not null)
     */
    private static void updatePhysics(Cgm cgm) {
        SceneView sceneView = cgm.getSceneView();
        BulletAppState bulletAppState = sceneView.getBulletAppState();
        RenderOptions options = Maud.getModel().getScene().getRender();
        boolean enable = options.isPhysicsRendered();
        bulletAppState.setDebugEnabled(enable);
    }

    /**
     * Update the shadow renderer based on the MVC model.
     *
     * @param cgm which C-G model (not null)
     */
    private static void updateShadows(Cgm cgm) {
        SceneView view = cgm.getSceneView();
        ViewPort vp = view.getViewPort();
        if (vp != null && vp.isEnabled()) {
            DirectionalLightShadowRenderer dlsr = null;
            List<SceneProcessor> list = vp.getProcessors();
            for (SceneProcessor processor : list) {
                if (processor instanceof DirectionalLightShadowRenderer) {
                    dlsr = (DirectionalLightShadowRenderer) processor;
                    break;
                }
            }

            RenderOptions options = Maud.getModel().getScene().getRender();
            if (options.areShadowsRendered()) {
                if (dlsr == null) {
                    dlsr = EditorViewPorts.addShadows(vp);
                } else {
                    int newMaps = options.getNumSplits();
                    int oldMaps = dlsr.getNumShadowMaps();
                    int newSize = options.getShadowMapSize();
                    int oldSize = dlsr.getShadowMapSize();
                    if (newMaps != oldMaps || newSize != oldSize) {
                        vp.removeProcessor(dlsr);
                        dlsr = EditorViewPorts.addShadows(vp);
                    }
                }

                DirectionalLight mainLight = view.getMainLight();
                dlsr.setLight(mainLight);
                EdgeFilteringMode edgeFilter = options.getEdgeFilter();
                dlsr.setEdgeFilteringMode(edgeFilter);

            } else if (dlsr != null) {
                vp.removeProcessor(dlsr);
            }
        }
    }

    /**
     * Update the skeleton visualizer based on the MVC model.
     *
     * @param cgm which C-G model (not null)
     */
    private static void updateSkeleton(Cgm cgm) {
        SkeletonVisualizer visualizer;
        visualizer = cgm.getSceneView().getSkeletonVisualizer();
        if (visualizer == null) {
            return;
        }

        SkeletonOptions options = Maud.getModel().getScene().getSkeleton();
        ShowBones showBones = options.getShowBones();
        visualizer.setEnabled(showBones != ShowBones.None);

        float lineWidth = options.getLineWidth();
        visualizer.setLineWidth(lineWidth);

        float pointSize = options.getPointSize();
        visualizer.setPointSize(pointSize);

        ColorRGBA color = options.copyLinkColor(null);
        visualizer.setLineColor(color);

        color = options.copyTracklessColor(null); // TODO avoid extra garbage
        visualizer.setPointColor(color);

        BitSet showSet = cgm.getSkeleton().listShown(showBones, null);

        options.copyTrackedColor(color);
        int numBones = cgm.getSkeleton().countBones();
        for (int boneIndex = 0; boneIndex < numBones; boneIndex++) {
            if (showSet.get(boneIndex) == false) {
                visualizer.setPointColor(boneIndex, invisibleColor);
            } else if (cgm.getAnimation().isRetargetedPose()) {
                String name = cgm.getSkeleton().getBoneName(boneIndex);
                if (Maud.getModel().getMap().isBoneMapped(name)) {
                    visualizer.setPointColor(boneIndex, color);
                }
            } else if (cgm.getAnimation().hasTrackForBone(boneIndex)) {
                visualizer.setPointColor(boneIndex, color);
            } // else default to trackless/unmapped color
        }
    }

    /**
     * Update the sky based on the MVC model.
     *
     * @param cgm which C-G model (not null)
     */
    private static void updateSky(Cgm cgm) {
        SkyControl sky = cgm.getSceneView().getSkyControl();
        RenderOptions options = Maud.getModel().getScene().getRender();
        boolean enable = options.isSkyRendered();
        sky.setEnabled(enable);
        float cloudiness = options.getCloudiness();
        sky.setCloudiness(cloudiness);
        float hour = options.getHour();
        sky.getSunAndStars().setHour(hour);

        Updater updater = sky.getUpdater();
        updater.setAmbientMultiplier(ambientMultiplier);
        updater.setMainMultiplier(mainMultiplier);

        ColorRGBA backgroundColor = options.backgroundColor(null);
        ViewPort viewPort = cgm.getSceneView().getViewPort();
        viewPort.setBackgroundColor(backgroundColor);
    }

    /**
     * Update the vertex visualization based on the MVC model.
     *
     * @param cgm which C-G model (not null)
     */
    private static void updateVertex(Cgm cgm) {
        Spatial spatial = cgm.getSceneView().getVertexSpatial();

        SelectedVertex vertex = cgm.getVertex();
        if (vertex.isSelected()) {
            Vector3f worldLocation = vertex.worldLocation(null);
            spatial.setLocalTranslation(worldLocation);

            Geometry geometry = (Geometry) spatial;
            Material material = geometry.getMaterial();

            VertexOptions options = Maud.getModel().getScene().getVertex();
            ColorRGBA color = options.copyColor(null);
            material.setColor("Color", color);
            float pointSize = options.getPointSize();
            material.setFloat("PointSize", pointSize);

            spatial.setCullHint(Spatial.CullHint.Never);
        } else {
            spatial.setCullHint(Spatial.CullHint.Always);
        }
    }
}
