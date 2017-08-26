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

import com.jme3.app.state.AppStateManager;
import de.lessvoid.nifty.controls.SliderChangedEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.nifty.WindowController;
import maud.EditorScreen;
import maud.model.LoadedCgm;

/**
 * Tool windows in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class EditorTools {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            EditorTools.class.getName());
    // *************************************************************************
    // fields

    /**
     * controller for the "Animation Tool" window
     */
    final AnimationTool animation;
    /**
     * controller for the "Axes Tool" window
     */
    final public AxesTool axes;
    /**
     * controller for the "Background" window
     */
    final BackgroundTool background;
    /**
     * controller for the "Bone Rotation Tool" window
     */
    final BoneRotationTool boneRotation;
    /**
     * controller for the "Bone Scale Tool" window
     */
    final BoneScaleTool boneScale;
    /**
     * controller for the "Bone Tool" window
     */
    final BoneTool bone;
    /**
     * controller for the "Bone Translation Tool" window
     */
    final BoneTranslationTool boneTranslation;
    /**
     * controller for the "Bounds Tool" window
     */
    final BoundsTool bounds;
    /**
     * controller for the "Camera Tool" window
     */
    final public CameraTool camera;
    /**
     * controller for the "Model Tool" window
     */
    final CgmTool cgm;
    /**
     * controller for the "Cursor Tool" window
     */
    final CursorTool cursor;
    /**
     * controller for the "History Tool" window
     */
    final public HistoryTool history;
    /**
     * controller for the "Keyframe Tool" window
     */
    final KeyframeTool keyframe;
    /**
     * controller for the "Mapping Tool" window
     */
    final MappingTool mapping;
    /**
     * controller for the "Physics Tool" window
     */
    final PhysicsTool physics;
    /**
     * controller for the "Platform Tool" window
     */
    final PlatformTool platform;
    /**
     * controller for the "Render Tool" window
     */
    final RenderTool render;
    /**
     * controller for the "Retarget Tool" window
     */
    final RetargetTool retarget;
    /**
     * controller for the "Scene Vertex Tool" window
     */
    final SceneVertexTool sceneVertex;
    /**
     * controller for the "Score Tool" window
     */
    final ScoreTool score;
    /**
     * controller for the "Control Tool" window
     */
    final SgcTool sgc;
    /**
     * controller for the "Skeleton Color Tool" window
     */
    final SkeletonColorTool skeletonColor;
    /**
     * controller for the "Skeleton Tool" window
     */
    final SkeletonTool skeleton;
    /**
     * controller for the "Sky Tool" window
     */
    final SkyTool sky;
    /**
     * controller for the "Source Animation Tool" window
     */
    final SourceAnimationTool sourceAnimation;
    /**
     * controller for the "Spatial Details Tool" window
     */
    final SpatialDetailsTool spatialDetails;
    /**
     * controller for the "Spatial Rotation Tool" window
     */
    final SpatialRotationTool spatialRotation;
    /**
     * controller for the "Spatial Scale Tool" window
     */
    final SpatialScaleTool spatialScale;
    /**
     * controller for the "Spatial Tool" window
     */
    final SpatialTool spatial;
    /**
     * controller for the "Spatial Translation Tool" window
     */
    final SpatialTranslationTool spatialTranslation;
    /**
     * controller for the "Tweening Tool" window
     */
    final TweeningTool tweening;
    /**
     * controller for the "Twist Tool" window
     */
    final TwistTool twist;
    /**
     * controller for the "User Data Tool" window
     */
    final UserDataTool userData;
    /**
     * controller for the "Vertex Tool" window
     */
    final VertexTool vertex;
    // *************************************************************************
    // constructors

    /**
     * Instantiate the tools in the specified screen.
     *
     * @param screen the screen's controller (not null)
     */
    public EditorTools(EditorScreen screen) {
        Validate.nonNull(screen, "screen");

        animation = new AnimationTool(screen);
        axes = new AxesTool(screen);
        background = new BackgroundTool(screen);
        boneRotation = new BoneRotationTool(screen);
        boneScale = new BoneScaleTool(screen);
        bone = new BoneTool(screen);
        boneTranslation = new BoneTranslationTool(screen);
        bounds = new BoundsTool(screen);
        camera = new CameraTool(screen);
        cgm = new CgmTool(screen);
        cursor = new CursorTool(screen);
        history = new HistoryTool(screen);
        keyframe = new KeyframeTool(screen);
        mapping = new MappingTool(screen);
        physics = new PhysicsTool(screen);
        platform = new PlatformTool(screen);
        render = new RenderTool(screen);
        retarget = new RetargetTool(screen);
        sceneVertex = new SceneVertexTool(screen);
        score = new ScoreTool(screen);
        sgc = new SgcTool(screen);
        skeletonColor = new SkeletonColorTool(screen);
        skeleton = new SkeletonTool(screen);
        sourceAnimation = new SourceAnimationTool(screen);
        spatialDetails = new SpatialDetailsTool(screen);
        spatialRotation = new SpatialRotationTool(screen);
        spatialScale = new SpatialScaleTool(screen);
        spatial = new SpatialTool(screen);
        spatialTranslation = new SpatialTranslationTool(screen);
        sky = new SkyTool(screen);
        tweening = new TweeningTool(screen);
        twist = new TwistTool(screen);
        userData = new UserDataTool(screen);
        vertex = new VertexTool(screen);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Attach all window controllers to the specified state manager.
     *
     * @param stateManager (not null)
     */
    public void attachAll(AppStateManager stateManager) {
        stateManager.attach(cursor); // cursor before camera
        stateManager.attachAll(animation, axes, background, bone, boneRotation,
                boneScale, boneTranslation, bounds, camera, history,
                keyframe, mapping, cgm, physics, platform, render, retarget,
                sceneVertex, score, sgc, skeleton, skeletonColor, sky,
                sourceAnimation, spatial, spatialDetails, spatialRotation,
                spatialScale, spatialTranslation, tweening, twist, userData,
                vertex);
    }

    /**
     * Access the window controller for a named tool.
     *
     * @param toolName which tool to access (not null, not empty)
     * @return the pre-existing instance, or null of none
     */
    public WindowController getTool(String toolName) {
        Validate.nonEmpty(toolName, "tool name");

        WindowController controller = null;
        switch (toolName) {
            case "animation":
                controller = animation;
                break;
            case "axes":
                controller = axes;
                break;
            case "background":
                controller = background;
                break;
            case "bone":
                controller = bone;
                break;
            case "boneRotation":
                controller = boneRotation;
                break;
            case "boneScale":
                controller = boneScale;
                break;
            case "boneTranslation":
                controller = boneTranslation;
                break;
            case "bounds":
                controller = bounds;
                break;
            case "camera":
                controller = camera;
                break;
            case "cgm":
                controller = cgm;
                break;
            case "cursor":
                controller = cursor;
                break;
            case "history":
                controller = history;
                break;
            case "keyframe":
                controller = keyframe;
                break;
            case "map":
                controller = mapping;
                break;
            case "physics":
                controller = physics;
                break;
            case "platform":
                controller = platform;
                break;
            case "render":
                controller = render;
                break;
            case "retarget":
                controller = retarget;
                break;
            case "sceneVertex":
                controller = sceneVertex;
                break;
            case "score":
                controller = score;
                break;
            case "sgc":
                controller = sgc;
                break;
            case "skeleton":
                controller = skeleton;
                break;
            case "skeletonColor":
                controller = skeletonColor;
                break;
            case "sourceAnimation":
                controller = sourceAnimation;
                break;
            case "spatial":
                controller = spatial;
                break;
            case "spatialDetails":
                controller = spatialDetails;
                break;
            case "spatialRotation":
                controller = spatialRotation;
                break;
            case "spatialScale":
                controller = spatialScale;
                break;
            case "spatialTranslation":
                controller = spatialTranslation;
                break;
            case "sky":
                controller = sky;
                break;
            case "tweening":
                controller = tweening;
                break;
            case "twist":
                controller = twist;
                break;
            case "userData":
                controller = userData;
                break;
            case "vertex":
                controller = vertex;
        }

        return controller;
    }

    /**
     * Callback invoked after a slider changes.
     *
     * @param sliderId Nifty element id of the slider (not null)
     * @param event details of the event (not null, ignored)
     */
    public void onSliderChanged(final String sliderId,
            final SliderChangedEvent event) {
        Validate.nonNull(sliderId, "slider id");
        Validate.nonNull(event, "event");

        switch (sliderId) {
            case "speedSlider":
            case "timeSlider":
                animation.onSliderChanged();
                break;

            case "axesLineWidthSlider":
                axes.onSliderChanged();
                break;

            case "bgRSlider":
            case "bgGSlider":
            case "bgBSlider":
                background.onSliderChanged();
                break;

            case "xAngSlider":
            case "yAngSlider":
            case "zAngSlider":
                boneRotation.onSliderChanged();
                break;

            case "xScaSlider":
            case "yScaSlider":
            case "zScaSlider":
                boneScale.onSliderChanged();
                break;

            case "xOffSlider":
            case "yOffSlider":
            case "zOffSlider":
                boneTranslation.onSliderChanged();
                break;

            case "boundsRSlider":
            case "boundsGSlider":
            case "boundsBSlider":
            case "boundsLineWidthSlider":
                bounds.onSliderChanged();
                break;

            case "cursorRSlider":
            case "cursorGSlider":
            case "cursorBSlider":
                cursor.onSliderChanged();
                break;

            case "svRSlider":
            case "svGSlider":
            case "svBSlider":
            case "svPointSizeSlider":
                sceneVertex.onSliderChanged();
                break;

            case "skeletonLineWidthSlider":
            case "skeletonPointSizeSlider":
                skeleton.onSliderChanged();
                break;

            case "skeRSlider":
            case "skeGSlider":
            case "skeBSlider":
            case "btRSlider":
            case "btGSlider":
            case "btBSlider":
            case "bntRSlider":
            case "bntGSlider":
            case "bntBSlider":
                skeletonColor.onSliderChanged();
                break;

            case "sSpeedSlider":
            case "sourceTimeSlider":
                sourceAnimation.onSliderChanged();
                break;

            case "xSaSlider":
            case "ySaSlider":
            case "zSaSlider":
                spatialRotation.onSliderChanged();
                break;

            case "xSsSlider":
            case "ySsSlider":
            case "zSsSlider":
                spatialScale.onSliderChanged();
                break;

            case "offMasterSlider":
            case "soMasterSlider":
                // do nothing
                break;

            case "xSoSlider":
            case "ySoSlider":
            case "zSoSlider":
                spatialTranslation.onSliderChanged();
                break;

            case "xTwistSlider":
            case "yTwistSlider":
            case "zTwistSlider":
                twist.onSliderChanged();
                break;

            default:
                logger.log(Level.WARNING, "unknown slider with id={0}",
                        MyString.quote(sliderId));
        }
    }

    /**
     * Select the named tool.
     *
     * @param toolName which tool to select (not null, not empty)
     */
    public void select(String toolName) {
        Validate.nonEmpty(toolName, "tool name");

        WindowController controller = getTool(toolName);
        controller.select();
    }

    /**
     * Scene updates performed even when all tools are disabled. (Invoked once
     * per render pass for each CG model that's rendered to a scene view.)
     *
     * @param loadedCgm which CG model occupies the scene to update (not null)
     */
    public void updateScene(LoadedCgm loadedCgm) {
        Validate.nonNull(loadedCgm, "loaded model");
        assert loadedCgm.getSceneView().getCamera() != null;

        loadedCgm.getScenePov().updateCamera();

        axes.updateVisualizer(loadedCgm);
        bounds.updateVisualizer(loadedCgm);
        cursor.updateScene(loadedCgm);
        physics.updateVisualizer(loadedCgm);
        platform.updateScene(loadedCgm);
        render.updateShadowFilter(loadedCgm);
        sceneVertex.updateVisualizer(loadedCgm);
        skeleton.updateVisualizer(loadedCgm);
        skeletonColor.updateVisualizer(loadedCgm);
        sky.updateSkyControl(loadedCgm);
    }
}
