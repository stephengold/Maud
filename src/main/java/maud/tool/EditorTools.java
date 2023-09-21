/*
 Copyright (c) 2017-2020, Stephen Gold
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
import jme3utilities.nifty.Tool;
import maud.EditorScreen;
import maud.Maud;
import maud.tool.option.AxesTool;
import maud.tool.option.BackgroundTool;
import maud.tool.option.BoundsTool;
import maud.tool.option.CameraTool;
import maud.tool.option.CursorTool;
import maud.tool.option.DumpTool;
import maud.tool.option.PhysicsTool;
import maud.tool.option.PlatformTool;
import maud.tool.option.RenderTool;
import maud.tool.option.SceneLightingTool;
import maud.tool.option.ScoreTool;
import maud.tool.option.SettingsTool;
import maud.tool.option.SkeletonTool;
import maud.tool.option.SkyTool;
import maud.tool.option.TweeningTool;

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
    final private BoneMirrorTool boneMirror;
    final private BoneRotationTool boneRotation;
    final private BoneScaleTool boneScale;
    final private BoneTool bone;
    final private BoneTranslationTool boneTranslation;
    final private BoundsTool bounds;
    final private CameraTool camera;

    final private CgmTool cgm;
    final private CursorTool cursor;
    final private DumpTool dump;
    final private ExtractTool extract;
    final private ExtremeVertexTool extremeVertex;
    final private HistoryTool history;
    final private JointTool joint;
    final private KeyframeTool keyframe;
    final private LightColorTool lightColor;
    final private LightDirectionTool lightDirection;

    final private LightPositionTool lightPosition;
    final private LightsTool lights;
    final private LinkTool link;
    final private MappingTool mapping;
    final private MaterialTool material;
    final private MeshTool mesh;
    final private OverridesTool overrides;
    final private PcoTool pco;
    final private PhysicsTool physics;
    final private PlatformTool platform;

    final private RenderTool render;
    final private RetargetTool retarget;
    final private SceneLightingTool sceneLighting;
    final private ScoreTool score;
    final private SettingsTool settings;
    final private SgcTool sgc;
    final private ShapeTool shape;
    final private SkeletonTool skeleton;
    final private SkyTool sky;
    final private SourceAnimationTool sourceAnimation;

    final private SpatialBoundsTool spatialBounds;
    final private SpatialDetailsTool spatialDetails;
    final private SpatialRotationTool spatialRotation;
    final private SpatialScaleTool spatialScale;
    final private SpatialTool spatial;
    final private SpatialTranslationTool spatialTranslation;
    final private TextureTool texture;
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
     * @param screenController the screen's controller (not null)
     */
    public EditorTools(EditorScreen screenController) {
        Validate.nonNull(screenController, "screen controller");

        animation = new AnimationTool(screenController);
        axes = new AxesTool(screenController);
        background = new BackgroundTool(screenController);
        boneMirror = new BoneMirrorTool(screenController);
        boneRotation = new BoneRotationTool(screenController);
        boneScale = new BoneScaleTool(screenController);
        bone = new BoneTool(screenController);
        boneTranslation = new BoneTranslationTool(screenController);
        bounds = new BoundsTool(screenController);
        camera = new CameraTool(screenController);

        cgm = new CgmTool(screenController);
        cursor = new CursorTool(screenController);
        dump = new DumpTool(screenController);
        extract = new ExtractTool(screenController);
        extremeVertex = new ExtremeVertexTool(screenController);
        history = new HistoryTool(screenController);
        joint = new JointTool(screenController);
        keyframe = new KeyframeTool(screenController);
        lightColor = new LightColorTool(screenController);
        lightDirection = new LightDirectionTool(screenController);

        lightPosition = new LightPositionTool(screenController);
        lights = new LightsTool(screenController);
        link = new LinkTool(screenController);
        mapping = new MappingTool(screenController);
        material = new MaterialTool(screenController);
        mesh = new MeshTool(screenController);
        overrides = new OverridesTool(screenController);
        pco = new PcoTool(screenController);
        physics = new PhysicsTool(screenController);
        platform = new PlatformTool(screenController);

        render = new RenderTool(screenController);
        retarget = new RetargetTool(screenController);
        sceneLighting = new SceneLightingTool(screenController);
        score = new ScoreTool(screenController);
        settings = new SettingsTool(screenController);
        sgc = new SgcTool(screenController);
        shape = new ShapeTool(screenController);
        skeleton = new SkeletonTool(screenController);
        sky = new SkyTool(screenController);
        sourceAnimation = new SourceAnimationTool(screenController);

        spatialBounds = new SpatialBoundsTool(screenController);
        spatialDetails = new SpatialDetailsTool(screenController);
        spatialRotation = new SpatialRotationTool(screenController);
        spatialScale = new SpatialScaleTool(screenController);
        spatial = new SpatialTool(screenController);
        spatialTranslation = new SpatialTranslationTool(screenController);
        texture = new TextureTool(screenController);
        track = new TrackTool(screenController);
        tweening = new TweeningTool(screenController);
        twist = new TwistTool(screenController);

        userData = new UserDataTool(screenController);
        vertex = new VertexTool(screenController);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Attach all tool controllers to the specified state manager.
     *
     * @param stateManager (not null)
     */
    public void attachAll(AppStateManager stateManager) {
        stateManager.attachAll(animation, axes, background, bone, boneMirror,
                boneRotation, boneScale, boneTranslation, bounds, camera, cgm,
                cursor, dump, extract, extremeVertex, history, keyframe,
                joint, lightColor, lightDirection, lightPosition, lights, link,
                mapping, material, mesh, overrides, pco, physics, platform,
                render, retarget, sceneLighting, score, settings,
                sgc, shape, skeleton, sky, sourceAnimation, spatial,
                spatialBounds, spatialDetails, spatialRotation, spatialScale,
                spatialTranslation, texture, track, tweening, twist, userData,
                vertex);
    }

    /**
     * Select the named tool.
     *
     * @param toolName which tool to select (not null, not empty)
     */
    public static void select(String toolName) {
        Validate.nonEmpty(toolName, "tool name");

        Tool tool = Maud.gui.findTool(toolName);
        if (tool == null) {
            String message = String.format("unimplemented feature (tool = %s)",
                    MyString.quote(toolName));
            Maud.getModel().getMisc().setStatusMessage(message);
        } else {
            tool.select();
        }
    }
}
