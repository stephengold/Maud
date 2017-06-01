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

import com.jme3.app.state.AppStateManager;
import de.lessvoid.nifty.controls.SliderChangedEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.nifty.WindowController;

/**
 * Tool windows in Maud's "3D View" screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class DddTools {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            DddTools.class.getName());
    // *************************************************************************
    // fields

    /*
     * controllers for tool windows
     */
    final AnimationTool animation;
    final AxesTool axes;
    final BoneRotationTool boneRotation;
    final BoneScaleTool boneScale;
    final BoneTool bone;
    final BoneTranslationTool boneTranslation;
    final CameraTool camera;
    final CullHintTool cullHint;
    final CursorTool cursor;
    final HistoryTool history;
    final KeyframeTool keyframe;
    final ModelTool model;
    final PlatformTool platform;
    final RenderTool render;
    final RetargetTool retarget;
    final SgcTool sgc;
    final ShadowModeTool shadowMode;
    final SkeletonColorTool skeletonColor;
    final SkeletonTool skeleton;
    final SourceAnimationTool sourceAnimation;
    final SpatialRotationTool spatialRotation;
    final SpatialScaleTool spatialScale;
    final SpatialTool spatial;
    final SpatialTranslationTool spatialTranslation;
    final SkyTool sky;
    // *************************************************************************
    // constructors

    /**
     * Instantiate the tools in the specified screen.
     *
     * @param screen the screen's controller (not null)
     */
    public DddTools(DddGui screen) {
        Validate.nonNull(screen, "screen");

        animation = new AnimationTool(screen);
        axes = new AxesTool(screen);
        boneRotation = new BoneRotationTool(screen);
        boneScale = new BoneScaleTool(screen);
        bone = new BoneTool(screen);
        boneTranslation = new BoneTranslationTool(screen);
        camera = new CameraTool(screen);
        cullHint = new CullHintTool(screen);
        cursor = new CursorTool(screen);
        history = new HistoryTool(screen);
        keyframe = new KeyframeTool(screen);
        model = new ModelTool(screen);
        platform = new PlatformTool(screen);
        render = new RenderTool(screen);
        retarget = new RetargetTool(screen);
        sgc = new SgcTool(screen);
        shadowMode = new ShadowModeTool(screen);
        skeletonColor = new SkeletonColorTool(screen);
        skeleton = new SkeletonTool(screen);
        sourceAnimation = new SourceAnimationTool(screen);
        spatialRotation = new SpatialRotationTool(screen);
        spatialScale = new SpatialScaleTool(screen);
        spatial = new SpatialTool(screen);
        spatialTranslation = new SpatialTranslationTool(screen);
        sky = new SkyTool(screen);
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
        stateManager.attachAll(animation, axes, bone, boneRotation, boneScale,
                boneTranslation, camera, cullHint, history, keyframe, model,
                platform, render, retarget, sgc, shadowMode, skeleton,
                skeletonColor, sky, sourceAnimation, spatial, spatialRotation,
                spatialScale, spatialTranslation);
    }

    /**
     * Access the window controller for a named tool.
     *
     * @param toolName which tool to access (not null, not empty)
     * @return true if the action is handled, otherwise false
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
            case "camera":
                controller = camera;
                break;
            case "control":
                controller = sgc;
                break;
            case "cullHint":
                controller = cullHint;
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
            case "model":
                controller = model;
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
            case "shadowMode":
                controller = shadowMode;
                break;
            case "skeleton":
                controller = skeleton;
                break;
            case "sourceAnimation":
                controller = sourceAnimation;
                break;
            case "spatial":
                controller = spatial;
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

            case "offMasterSlider":
            case "xOffSlider":
            case "yOffSlider":
            case "zOffSlider":
                boneTranslation.onSliderChanged();
                break;

            case "cursorRSlider":
            case "cursorGSlider":
            case "cursorBSlider":
                cursor.onSliderChanged();
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

            case "soMasterSlider":
            case "xSoSlider":
            case "ySoSlider":
            case "zSoSlider":
                spatialTranslation.onSliderChanged();
                break;

            default:
                logger.log(Level.WARNING, "unknown slider with id={0}",
                        MyString.quote(sliderId));
        }
    }
}
