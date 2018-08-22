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
package maud.view.scene;

import com.jme3.bullet.BulletAppState;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.system.AppSettings;
import java.util.BitSet;
import java.util.logging.Logger;
import jme3utilities.MySpatial;
import jme3utilities.debug.AxesVisualizer;
import jme3utilities.debug.BoundsVisualizer;
import jme3utilities.debug.SkeletonVisualizer;
import jme3utilities.sky.SkyControl;
import jme3utilities.sky.Updater;
import maud.EditorViewPorts;
import maud.Maud;
import maud.model.EditorModel;
import maud.model.cgm.Cgm;
import maud.model.cgm.SelectedLight;
import maud.model.cgm.SelectedVertex;
import maud.model.option.ShowBones;
import maud.model.option.scene.AxesDragEffect;
import maud.model.option.scene.AxesOptions;
import maud.model.option.scene.AxesSubject;
import maud.model.option.scene.BoundsOptions;
import maud.model.option.scene.LightsOptions;
import maud.model.option.scene.RenderOptions;
import maud.model.option.scene.SceneOptions;
import maud.model.option.scene.SkeletonColors;
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
     * @param updateInterval time interval between updates (in seconds, &ge;0)
     */
    static void update(Cgm viewCgm, float updateInterval) {
        SceneView sceneView = viewCgm.getSceneView();
        assert sceneView.getCamera() != null;

        viewCgm.getScenePov().update(updateInterval);
        updateAxes(viewCgm);
        updateBounds(sceneView);
        sceneView.getCursor().update(viewCgm, updateInterval);
        updatePhysics(viewCgm);
        sceneView.getPlatform().update();
        updateShadows(viewCgm);
        updateSkeleton(viewCgm);
        updateSky(viewCgm);
        updateVertex(viewCgm);
    }
    // *************************************************************************
    // private methods

    /**
     * Update the specified view's ambient light based on the MVC model.
     *
     * @param sceneView which view (not null)
     */
    private static void updateAmbientLight(SceneView sceneView) {
        SceneOptions options = Maud.getModel().getScene();
        assert !options.getRender().isSkySimulated();

        float ambientLevel = options.getLights().getAmbientLevel();
        ColorRGBA ambientColor = new ColorRGBA(ambientLevel, ambientLevel,
                ambientLevel, 1f);

        AmbientLight ambient = sceneView.getAmbientLight();
        ambient.setColor(ambientColor);
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
            Vector3f cameraLocation = cgm.getScenePov().location(null);
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
     * Update the specified C-G model's background color based on the MVC model.
     *
     * @param cgm which C-G model (not null)
     */
    private static void updateBackground(Cgm cgm) {
        EditorModel model = Maud.getModel();
        RenderOptions renderOptions = model.getScene().getRender();
        assert !renderOptions.isSkySimulated();

        ColorRGBA backgroundColor;
        if (cgm == model.getSource()) {
            backgroundColor = renderOptions.sourceBackgroundColor(null);
        } else {
            assert cgm == model.getTarget();
            backgroundColor = renderOptions.targetBackgroundColor(null);
        }

        ViewPort viewPort = cgm.getSceneView().getViewPort();
        viewPort.setBackgroundColor(backgroundColor);
    }

    /**
     * Update the bounds visualizer based on the MVC model.
     *
     * @param sceneView which view (not null)
     */
    private static void updateBounds(SceneView sceneView) {
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
     * Update the specified view's main (directional) light based on the MVC
     * model.
     *
     * @param sceneView which view (not null)
     */
    private static void updateMainLight(SceneView sceneView) {
        SceneOptions options = Maud.getModel().getScene();
        assert !options.getRender().isSkySimulated();

        LightsOptions lightsOptions = options.getLights();
        float mainLevel = lightsOptions.getMainLevel();
        ColorRGBA mainColor
                = new ColorRGBA(mainLevel, mainLevel, mainLevel, 1f);

        DirectionalLight main = sceneView.getMainLight();
        main.setColor(mainColor);
        Vector3f direction = lightsOptions.direction(null);
        main.setDirection(direction);
    }

    /**
     * Update the physics visualization based on the MVC model.
     *
     * @param cgm which C-G model (not null)
     */
    private static void updatePhysics(Cgm cgm) {
        SceneView sceneView = cgm.getSceneView();
        BulletAppState bulletAppState = sceneView.getBulletAppState();
        RenderOptions renderOptions = Maud.getModel().getScene().getRender();
        boolean enable = renderOptions.isPhysicsRendered();
        bulletAppState.setDebugEnabled(enable);
    }

    /**
     * Update the specified view's shadow intensity based on the MVC model.
     *
     * @param sceneView which view (not null)
     */
    private static void updateShadowIntensity(SceneView sceneView) {
        SceneOptions options = Maud.getModel().getScene();
        assert !options.getRender().isSkySimulated();

        DirectionalLightShadowRenderer dlsr = sceneView.getShadowRenderer();
        if (dlsr != null) {
            LightsOptions lights = options.getLights();
            float ambientLevel = lights.getAmbientLevel();
            float mainLevel = lights.getMainLevel();
            float totalLevel = mainLevel + ambientLevel;
            float shadowIntensity;
            if (totalLevel == 0f) {
                shadowIntensity = 0f;
            } else {
                shadowIntensity = FastMath.saturate(mainLevel / totalLevel);
            }
            dlsr.setShadowIntensity(shadowIntensity);
        }
    }

    /**
     * Update specified C-G model's shadow renderer based on the MVC model. The
     * shadow intensity is updated elsewhere.
     *
     * @param cgm which C-G model (not null)
     */
    private static void updateShadows(Cgm cgm) {
        SceneView sceneView = cgm.getSceneView();
        ViewPort viewPort = sceneView.getViewPort();
        if (viewPort != null && viewPort.isEnabled()) {
            DirectionalLightShadowRenderer dlsr = sceneView.getShadowRenderer();
            Updater skyUpdater = sceneView.getSkyControl().getUpdater();

            SceneOptions sceneOptions = Maud.getModel().getScene();
            RenderOptions renderOptions = sceneOptions.getRender();
            if (renderOptions.areShadowsRendered()) {
                if (dlsr == null) {
                    dlsr = EditorViewPorts.addShadows(viewPort);
                    skyUpdater.addShadowRenderer(dlsr);
                } else {
                    int newMaps = renderOptions.getNumSplits();
                    int oldMaps = dlsr.getNumShadowMaps();
                    int newSize = renderOptions.getShadowMapSize();
                    int oldSize = dlsr.getShadowMapSize();
                    if (newMaps != oldMaps || newSize != oldSize) {
                        viewPort.removeProcessor(dlsr);
                        skyUpdater.removeShadowRenderer(dlsr);

                        dlsr = EditorViewPorts.addShadows(viewPort);
                        skyUpdater.addShadowRenderer(dlsr);
                    }
                }

                DirectionalLight mainLight = sceneView.getMainLight();
                dlsr.setLight(mainLight);
                EdgeFilteringMode edgeFilter = renderOptions.getEdgeFilter();
                dlsr.setEdgeFilteringMode(edgeFilter);

            } else if (dlsr != null) {
                viewPort.removeProcessor(dlsr);
                skyUpdater.removeShadowRenderer(dlsr);
            }
        }
    }

    /**
     * Update the skeleton visualizer based on the MVC model.
     *
     * @param cgm which C-G model (not null)
     */
    private static void updateSkeleton(Cgm cgm) {
        SkeletonVisualizer visualizer
                = cgm.getSceneView().getSkeletonVisualizer();

        SkeletonOptions options = Maud.getModel().getScene().getSkeleton();
        ShowBones showBones = options.getShowBones();
        visualizer.setEnabled(showBones != ShowBones.None);
        int selectedBoneIndex = cgm.getBone().getIndex();
        BitSet showSet = cgm.getSkeleton().listShown(showBones,
                selectedBoneIndex, null);

        float lineWidth = options.getLineWidth();
        visualizer.setLineWidth(lineWidth);

        float pointSize = options.getPointSize();
        AppSettings current = Maud.getSettings();
        int msaaSamples = current.getSamples();
        if (msaaSamples == 16) { // work around JME issue #878
            pointSize *= 2f;
        }
        visualizer.setHeadSize(pointSize);

        ColorRGBA color = options.copyColor(SkeletonColors.Links, null);
        visualizer.setLineColor(color);

        options.copyColor(SkeletonColors.IdleBones, color);
        visualizer.setHeadColor(color);

        ColorRGBA forTracked
                = options.copyColor(SkeletonColors.TrackedBones, null);
        ColorRGBA forMapped
                = options.copyColor(SkeletonColors.MappedBones, null);
        int numBones = cgm.getSkeleton().countBones();
        for (int boneIndex = 0; boneIndex < numBones; boneIndex++) {
            if (showSet.get(boneIndex) == false) {
                visualizer.setHeadColor(boneIndex, invisibleColor);
            } else if (cgm.getAnimation().isRetargetedPose()) {
                String name = cgm.getSkeleton().getBoneName(boneIndex);
                if (Maud.getModel().getMap().isBoneMapped(name)) {
                    visualizer.setHeadColor(boneIndex, forMapped);
                }
            } else if (cgm.getAnimation().hasTrackForBone(boneIndex)) {
                visualizer.setHeadColor(boneIndex, forTracked);
            } // else default to trackless/unmapped color
        }
    }

    /**
     * Update the specified C-G model's sky, background, lights, and shadow
     * intensity based on the MVC model.
     *
     * @param cgm which C-G model (not null)
     */
    private static void updateSky(Cgm cgm) {
        RenderOptions renderOptions = Maud.getModel().getScene().getRender();
        boolean skySimulated = renderOptions.isSkySimulated();

        SceneView sceneView = cgm.getSceneView();
        SkyControl sky = sceneView.getSkyControl();
        sky.setEnabled(skySimulated);

        if (skySimulated) {
            float cloudiness = renderOptions.getCloudiness();
            sky.setCloudiness(cloudiness);

            float hour = renderOptions.getHour();
            sky.getSunAndStars().setHour(hour);

            Updater updater = sky.getUpdater();
            updater.setAmbientMultiplier(ambientMultiplier);
            updater.setMainMultiplier(mainMultiplier);

        } else {
            updateAmbientLight(sceneView);
            updateBackground(cgm);
            updateMainLight(sceneView);
            updateShadowIntensity(sceneView);
        }
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
            AppSettings current = Maud.getSettings();
            int msaaSamples = current.getSamples();
            if (msaaSamples == 16) { // work around JME issue #878
                pointSize *= 2f;
            }
            material.setFloat("PointSize", pointSize);

            spatial.setCullHint(Spatial.CullHint.Never);
        } else {
            spatial.setCullHint(Spatial.CullHint.Always);
        }
    }
}
