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
    final private static Logger logger
            = Logger.getLogger(EditorTools.class.getName());
    // *************************************************************************
    // fields

    /**
     * controller for the "Animation Tool" window
     */
    final private AnimationTool animation;
    /**
     * controller for the "Axes Tool" window
     */
    final private AxesTool axes;
    /**
     * controller for the "Background" window
     */
    final private BackgroundTool background;
    /**
     * controller for the "Bone-Rotation Tool" window
     */
    final private BoneRotationTool boneRotation;
    /**
     * controller for the "Bone-Scale Tool" window
     */
    final private BoneScaleTool boneScale;
    /**
     * controller for the "Bone Tool" window
     */
    final private BoneTool bone;
    /**
     * controller for the "Bone-Translation Tool" window
     */
    final private BoneTranslationTool boneTranslation;
    /**
     * controller for the "Bounds Tool" window
     */
    final private BoundsTool bounds;
    /**
     * controller for the "Camera Tool" window
     */
    final private CameraTool camera;
    /**
     * controller for the "Model Tool" window
     */
    final private CgmTool cgm;
    /**
     * controller for the "Cursor Tool" window
     */
    final private CursorTool cursor;
    /**
     * controller for the "Display-Settings Tool" window
     */
    final private DisplaySettingsTool displaySettings;
    /**
     * controller for the "History Tool" window
     */
    final private HistoryTool history;
    /**
     * controller for the "Joint Tool" window
     */
    final private JointTool joint;
    /**
     * controller for the "Keyframe Tool" window
     */
    final private KeyframeTool keyframe;
    /**
     * controller for the "Mapping Tool" window
     */
    final private MappingTool mapping;
    /**
     * controller for the "Object Tool" window
     */
    final private ObjectTool object;
    /**
     * controller for the "Overrides Tool" window
     */
    final private OverridesTool overrides;
    /**
     * controller for the "Platform Tool" window
     */
    final private PlatformTool platform;
    /**
     * controller for the "Render Tool" window
     */
    final private RenderTool render;
    /**
     * controller for the "Retarget Tool" window
     */
    final private RetargetTool retarget;
    /**
     * controller for the "Scene Vertex Tool" window
     */
    final private SceneVertexTool sceneVertex;
    /**
     * controller for the "Score Tool" window
     */
    final private ScoreTool score;
    /**
     * controller for the "Settings Tool" window
     */
    final private SettingsTool settings;
    /**
     * controller for the "Control Tool" window
     */
    final private SgcTool sgc;
    /**
     * controller for the "Shape Tool" window
     */
    final private ShapeTool shape;
    /**
     * controller for the "Skeleton Color Tool" window
     */
    final private SkeletonColorTool skeletonColor;
    /**
     * controller for the "Skeleton Tool" window
     */
    final private SkeletonTool skeleton;
    /**
     * controller for the "Sky Tool" window
     */
    final private SkyTool sky;
    /**
     * controller for the "Source Animation Tool" window
     */
    final private SourceAnimationTool sourceAnimation;
    /**
     * controller for the "Spatial Details Tool" window
     */
    final private SpatialDetailsTool spatialDetails;
    /**
     * controller for the "Spatial-Rotation Tool" window
     */
    final private SpatialRotationTool spatialRotation;
    /**
     * controller for the "Spatial-Scale Tool" window
     */
    final private SpatialScaleTool spatialScale;
    /**
     * controller for the "Spatial Tool" window
     */
    final private SpatialTool spatial;
    /**
     * controller for the "Spatial-Translation Tool" window
     */
    final private SpatialTranslationTool spatialTranslation;
    /**
     * controller for the "Tweening Tool" window
     */
    final private TweeningTool tweening;
    /**
     * controller for the "Twist Tool" window
     */
    final private TwistTool twist;
    /**
     * controller for the "User-Data Tool" window
     */
    final private UserDataTool userData;
    /**
     * controller for the "Vertex Tool" window
     */
    final private VertexTool vertex;
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
        displaySettings = new DisplaySettingsTool(screen);
        history = new HistoryTool(screen);
        joint = new JointTool(screen);
        keyframe = new KeyframeTool(screen);
        mapping = new MappingTool(screen);
        object = new ObjectTool(screen);
        overrides = new OverridesTool(screen);
        platform = new PlatformTool(screen);
        render = new RenderTool(screen);
        retarget = new RetargetTool(screen);
        sceneVertex = new SceneVertexTool(screen);
        score = new ScoreTool(screen);
        settings = new SettingsTool(screen);
        sgc = new SgcTool(screen);
        shape = new ShapeTool(screen);
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
        stateManager.attachAll(animation, axes, background, bone, boneRotation,
                boneScale, boneTranslation, bounds, camera, cgm, cursor,
                displaySettings, history, keyframe, joint, mapping, object,
                overrides, platform, render, retarget, sceneVertex, score,
                settings, sgc, shape, skeleton, skeletonColor, sky,
                sourceAnimation, spatial, spatialDetails, spatialRotation,
                spatialScale, spatialTranslation, tweening,
                twist, userData, vertex);
    }

    /**
     * Access the window controller for a named tool.
     *
     * @param toolName which tool to access (not null, not empty)
     * @return the pre-existing instance, or null of none
     */
    public WindowController getTool(String toolName) {
        Validate.nonEmpty(toolName, "tool name");

        WindowController controller;
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
            case "displaySettings":
                controller = displaySettings;
                break;
            case "history":
                controller = history;
                break;
            case "joint":
                controller = joint;
                break;
            case "keyframe":
                controller = keyframe;
                break;
            case "map":
                controller = mapping;
                break;
            case "object":
                controller = object;
                break;
            case "overrides":
                controller = overrides;
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
            case "settings":
                controller = settings;
                break;
            case "sgc":
                controller = sgc;
                break;
            case "shape":
                controller = shape;
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
                break;
            default:
                String message = String.format("tool name = %s",
                        MyString.quote(toolName));
                throw new IllegalArgumentException(message);
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
        assert sliderId.endsWith("Slider");

        String prefix = MyString.removeSuffix(sliderId, "Slider");
        switch (prefix) {
            case "speed":
            case "time":
                animation.onSliderChanged();
                break;

            case "axesLineWidth":
                axes.onSliderChanged();
                break;

            case "sbgR":
            case "sbgG":
            case "sbgB":
            case "tbgR":
            case "tbgG":
            case "tbgB":
                background.onSliderChanged();
                break;

            case "xAng":
            case "yAng":
            case "zAng":
                boneRotation.onSliderChanged();
                break;

            case "scaMaster":
            case "xSca":
            case "ySca":
            case "zSca":
                boneScale.onSliderChanged();
                break;

            case "xOff":
            case "yOff":
            case "zOff":
                boneTranslation.onSliderChanged();
                break;

            case "boundsR":
            case "boundsG":
            case "boundsB":
            case "boundsLineWidth":
                bounds.onSliderChanged();
                break;

            case "cursorR":
            case "cursorG":
            case "cursorB":
                cursor.onSliderChanged();
                break;

            case "platformDiameter":
                platform.onSliderChanged();
                break;

            case "svR":
            case "svG":
            case "svB":
            case "svPointSize":
                sceneVertex.onSliderChanged();
                break;

            case "skeletonLineWidth":
            case "skeletonPointSize":
                skeleton.onSliderChanged();
                break;

            case "skeR":
            case "skeG":
            case "skeB":
            case "btR":
            case "btG":
            case "btB":
            case "bntR":
            case "bntG":
            case "bntB":
                skeletonColor.onSliderChanged();
                break;

            case "sSpeed":
            case "sourceTime":
                sourceAnimation.onSliderChanged();
                break;

            case "xSa":
            case "ySa":
            case "zSa":
                spatialRotation.onSliderChanged();
                break;

            case "ssMaster":
            case "xSs":
            case "ySs":
            case "zSs":
                spatialScale.onSliderChanged();
                break;

            case "offMaster":
            case "soMaster":
                // do nothing
                break;

            case "xSo":
            case "ySo":
            case "zSo":
                spatialTranslation.onSliderChanged();
                break;

            case "xTwist":
            case "yTwist":
            case "zTwist":
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
}
