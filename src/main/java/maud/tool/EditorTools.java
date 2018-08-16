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
import jme3utilities.MyString;
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

    final private AnimationTool animation;
    final private AxesTool axes;
    final private BackgroundTool background;
    final private BoneRotationTool boneRotation;
    final private BoneScaleTool boneScale;
    final private BoneTool bone;
    final private BoneTranslationTool boneTranslation;
    final private BoundsTool bounds;
    final private CameraTool camera;
    final private CgmTool cgm;

    final private CursorTool cursor;
    final private DisplaySettingsTool displaySettings;
    final private ExtractTool extract;
    final private HistoryTool history;
    final private JointTool joint;
    final private KeyframeTool keyframe;
    final private LightColorTool lightColor;
    final private LightDirectionTool lightDirection;
    final private LightPositionTool lightPosition;
    final private LightsTool lights;

    final private MappingTool mapping;
    final private MaterialTool material;
    final private MeshTool mesh;
    final private ObjectTool object;
    final private OverridesTool overrides;
    final private PhysicsTool physics;
    final private PlatformTool platform;
    final private RenderTool render;
    final private RetargetTool retarget;
    final private SceneLightingTool sceneLighting;
    final private SceneVertexTool sceneVertex;

    final private ScoreTool score;
    final private SettingsTool settings;
    final private SgcTool sgc;
    final private ShapeTool shape;
    final private SkeletonTool skeleton;
    final private SkyTool sky;
    final private SourceAnimationTool sourceAnimation;
    final private SpatialDetailsTool spatialDetails;
    final private SpatialRotationTool spatialRotation;
    final private SpatialScaleTool spatialScale;

    final private SpatialTool spatial;
    final private SpatialTranslationTool spatialTranslation;
    final private TrackTool track;
    final private TweeningTool tweening;
    final private TwistTool twist;
    final private UserDataTool userData;
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
        extract = new ExtractTool(screen);
        history = new HistoryTool(screen);
        joint = new JointTool(screen);
        keyframe = new KeyframeTool(screen);
        lightColor = new LightColorTool(screen);
        lightDirection = new LightDirectionTool(screen);
        lightPosition = new LightPositionTool(screen);
        lights = new LightsTool(screen);

        mapping = new MappingTool(screen);
        material = new MaterialTool(screen);
        mesh = new MeshTool(screen);
        object = new ObjectTool(screen);
        overrides = new OverridesTool(screen);
        physics = new PhysicsTool(screen);
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
        sky = new SkyTool(screen);
        sourceAnimation = new SourceAnimationTool(screen);
        spatialDetails = new SpatialDetailsTool(screen);
        spatialRotation = new SpatialRotationTool(screen);

        spatialScale = new SpatialScaleTool(screen);
        spatial = new SpatialTool(screen);
        spatialTranslation = new SpatialTranslationTool(screen);
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
                displaySettings, extract, history, keyframe, joint, lightColor,
                lightDirection, lightPosition, lights, mapping, material, mesh,
                object, overrides, physics, platform, render, retarget,
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
        if (tool == null) {
            String message = String.format("unimplemented feature (tool = %s)",
                    MyString.quote(toolName));
            Maud.getModel().getMisc().setStatusMessage(message);
        } else {
            tool.select();
        }
    }
}
