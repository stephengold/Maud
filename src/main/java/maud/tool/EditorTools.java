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
package maud.tool;

import com.jme3.app.state.AppStateManager;
import java.util.logging.Logger;
import jme3utilities.Validate;
import maud.EditorScreen;
import maud.Maud;

/**
 * Tools in Maud's editor screen.
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
     * controller for the AnimationTool
     */
    final private AnimationTool animation;
    /**
     * controller for the AxesTool
     */
    final private AxesTool axes;
    /**
     * controller for the BackgroundTool
     */
    final private BackgroundTool background;
    /**
     * controller for the BoneRotationTool
     */
    final private BoneRotationTool boneRotation;
    /**
     * controller for the BoneScaleTool
     */
    final private BoneScaleTool boneScale;
    /**
     * controller for the BoneTool
     */
    final private BoneTool bone;
    /**
     * controller for the BoneTranslationTool
     */
    final private BoneTranslationTool boneTranslation;
    /**
     * controller for the BoundsTool
     */
    final private BoundsTool bounds;
    /**
     * controller for the CameraTool
     */
    final private CameraTool camera;
    /**
     * controller for the CgmTool
     */
    final private CgmTool cgm;
    /**
     * controller for the CursorTool
     */
    final private CursorTool cursor;
    /**
     * controller for the DisplaySettingsTool
     */
    final private DisplaySettingsTool displaySettings;
    /**
     * controller for the HistoryTool
     */
    final private HistoryTool history;
    /**
     * controller for the JointTool
     */
    final private JointTool joint;
    /**
     * controller for the KeyframeTool
     */
    final private KeyframeTool keyframe;
    /**
     * controller for the LightsTool
     */
    final private LightsTool lights;
    /**
     * controller for the MappingTool
     */
    final private MappingTool mapping;
    /**
     * controller for the MaterialTool
     */
    final private MaterialTool material;
    /**
     * controller for the ObjectTool
     */
    final private ObjectTool object;
    /**
     * controller for the OverridesTool
     */
    final private OverridesTool overrides;
    /**
     * controller for the PlatformTool
     */
    final private PlatformTool platform;
    /**
     * controller for the RenderTool
     */
    final private RenderTool render;
    /**
     * controller for the RetargetTool
     */
    final private RetargetTool retarget;
    /**
     * controller for the SceneLightingTool
     */
    final private SceneLightingTool sceneLighting;
    /**
     * controller for the SceneVertexTool
     */
    final private SceneVertexTool sceneVertex;
    /**
     * controller for ScoreTool
     */
    final private ScoreTool score;
    /**
     * controller for the SettingsTool
     */
    final private SettingsTool settings;
    /**
     * controller for the SgcTool
     */
    final private SgcTool sgc;
    /**
     * controller for the ShapeTool
     */
    final private ShapeTool shape;
    /**
     * controller for SkeletonTool
     */
    final private SkeletonTool skeleton;
    /**
     * controller for the SkyTool
     */
    final private SkyTool sky;
    /**
     * controller for the SourceAnimationTool
     */
    final private SourceAnimationTool sourceAnimation;
    /**
     * controller for the SpatialDetailsTool
     */
    final private SpatialDetailsTool spatialDetails;
    /**
     * controller for the SpatialRotationTool
     */
    final private SpatialRotationTool spatialRotation;
    /**
     * controller for the SpatialScaleTool
     */
    final private SpatialScaleTool spatialScale;
    /**
     * controller for the SpatialTool
     */
    final private SpatialTool spatial;
    /**
     * controller for the SpatialTranslationTool
     */
    final private SpatialTranslationTool spatialTranslation;
    /**
     * controller for the TrackTool
     */
    final private TrackTool track;
    /**
     * controller for the TweeningTool
     */
    final private TweeningTool tweening;
    /**
     * controller for the TwistTool
     */
    final private TwistTool twist;
    /**
     * controller for the UserDataTool
     */
    final private UserDataTool userData;
    /**
     * controller for the VertexTool
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
        lights = new LightsTool(screen);
        mapping = new MappingTool(screen);
        material = new MaterialTool(screen);
        object = new ObjectTool(screen);
        overrides = new OverridesTool(screen);
        platform = new PlatformTool(screen);
        render = new RenderTool(screen);
        retarget = new RetargetTool(screen);
        sceneLighting = new SceneLightingTool(screen);
        sceneVertex = new SceneVertexTool(screen);
        score = new ScoreTool(screen);
        settings = new SettingsTool(screen);
        sgc = new SgcTool(screen);
        shape = new ShapeTool(screen);
        skeleton = new SkeletonTool(screen);
        sourceAnimation = new SourceAnimationTool(screen);
        spatialDetails = new SpatialDetailsTool(screen);
        spatialRotation = new SpatialRotationTool(screen);
        spatialScale = new SpatialScaleTool(screen);
        spatial = new SpatialTool(screen);
        spatialTranslation = new SpatialTranslationTool(screen);
        sky = new SkyTool(screen);
        track = new TrackTool(screen);
        tweening = new TweeningTool(screen);
        twist = new TwistTool(screen);
        userData = new UserDataTool(screen);
        vertex = new VertexTool(screen);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Attach all tool controllers to the specified state manager.
     *
     * @param stateManager (not null)
     */
    public void attachAll(AppStateManager stateManager) {
        stateManager.attachAll(animation, axes, background, bone, boneRotation,
                boneScale, boneTranslation, bounds, camera, cgm, cursor,
                displaySettings, history, keyframe, joint, lights, mapping,
                material, object, overrides, platform, render, retarget,
                sceneLighting, sceneVertex, score, settings, sgc, shape,
                skeleton, sky, sourceAnimation, spatial, spatialDetails,
                spatialRotation, spatialScale, spatialTranslation, track,
                tweening, twist, userData, vertex);
    }

    /**
     * Select the named tool.
     *
     * @param toolName which tool to select (not null, not empty)
     */
    public void select(String toolName) {
        Validate.nonEmpty(toolName, "tool name");

        Tool tool = Maud.gui.getTool(toolName);
        tool.select();
    }
}
