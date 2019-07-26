/*
 Copyright (c) 2017-2019, Stephen Gold
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
package maud.action;

import java.util.logging.Logger;
import jme3utilities.ui.InputMode;

/**
 * Action strings for Maud's "editor" screen. Each string describes a
 * user-interface action. Each string defined here should appear somewhere in
 * "editor.xml" and/or "editor.properties". By convention, action strings begin
 * with a verb in all lowercase and never end with a space (' ').
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class Action {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(Action.class.getName());

    final static String applySpatialTransform = "apply spatialTransform";
    /**
     * delete the selected animation
     */
    final public static String deleteAnimation = "delete animation";
    final static String deleteBuffer = "delete buffer";
    final static String deleteLight = "delete light";
    final static String deleteLink = "delete link";
    final static String deleteMapping = "delete mapping";
    final static String deleteMatParam = "delete matParam";
    final static String deleteOverride = "delete override";
    /**
     * delete the selected S-G control
     */
    final public static String deleteSgc = "delete sgc";
    final static String deleteSingleKeyframe = "delete singleKeyframe";
    final static String deleteTrackRotations = "delete trackRotations";
    final static String deleteTrackScales = "delete trackScales";
    final static String deleteTrackTranslations = "delete trackTranslations";
    final static String deleteUserKey = "delete userKey";

    final static String dumpAppStates = "dump appStates";
    final static String dumpMouseCgm = "dump mouseCgm";
    final static String dumpPhysicsSpace = "dump physicsSpace";
    final static String dumpRenderer = "dump renderer";
    final static String dumpSourceCgm = "dump sourceCgm";
    final static String dumpSourcePhysics = "dump sourcePhysics";
    final static String dumpTargetCgm = "dump targetCgm";
    final static String dumpTargetPhysics = "dump targetPhysics";

    final static String launchProjectile = "launch projectile";

    final static String loadAnimation = "load animation";
    final static String loadCgm = "load cgm";
    final static String loadMapAsset = "load map asset";
    final static String loadRetargetedPose = "load retargetedPose";
    final static String loadSourceAnimation = "load sourceAnimation";
    final static String loadSourceCgm = "load sourceCgm";
    final static String loadTexture = "load texture";

    final static String newAnimation = "new animation";
    final static String newAnimationFromExtract = "new animation fromExtract";
    final static String newAnimationFromPose = "new animation fromPose";
    final static String newAnimationFromRetarget
            = "new animation fromRetarget";
    final static String newAttachmentLink = "new attachmentLink";
    final static String newBoneLink = "new boneLink";
    final static String newCheckpoint = "new checkpoint";
    final static String newLight = "new light";
    final static String newMapping = "new mapping";
    final static String newMatParam = "new matParam";
    final static String newOverride = "new override";
    final static String newSgc = "new sgc";
    final static String newSingleKeyframe = "new singleKeyframe";
    final static String newTexture = "new texture";
    final static String newUserKey = "new userKey";

    final static String nextAnimation = "next animation";
    final static String nextAnimControl = "next animControl";
    final static String nextBone = "next bone";
    final static String nextBuffer = "next buffer";
    final static String nextCheckpoint = "next checkpoint";
    final static String nextJoint = "next joint";
    final static String nextLight = "next light";
    final static String nextLink = "next link";
    final static String nextMapping = "next mapping";
    final static String nextMatParam = "next matParam";
    final static String nextOverride = "next override";
    final static String nextPco = "next pco";
    final static String nextPerformanceMode = "next performanceMode";
    final static String nextRotationDisplay = "next rotationDisplay";
    final static String nextSgc = "next sgc";
    final static String nextShape = "next shape";
    final static String nextSourceAnimation = "next sourceAnimation";
    final static String nextSourceAnimControl = "next sourceAnimControl";
    final static String nextTexture = "next texture";
    final static String nextTrack = "next track";
    final static String nextUserData = "next userData";
    final static String nextVertex = "next vertex";
    final static String nextViewMode = "next viewMode";

    final static String pickAny = "pick any";
    final static String pickBone = "pick bone";
    final static String pickGnomon = "pick gnomon";
    final static String pickKeyframe = "pick keyframe";
    final static String pickVertex = "pick vertex";
    final static String pickViewMenu = "pick viewMenu";

    final static String previousAnimation = "previous animation";
    final static String previousAnimControl = "previous animControl";
    final static String previousBone = "previous bone";
    final static String previousBuffer = "previous buffer";
    final static String previousCheckpoint = "previous checkpoint";
    final static String previousJoint = "previous joint";
    final static String previousLight = "previous light";
    final static String previousLink = "previous link";
    final static String previousMapping = "previous mapping";
    final static String previousMatParam = "previous matParam";
    final static String previousOverride = "previous override";
    final static String previousPco = "previous pco";
    final static String previousSgc = "previous sgc";
    final static String previousShape = "previous shape";
    final static String previousSourceAnimation = "previous sourceAnimation";
    final static String previousSourceAnimControl
            = "previous sourceAnimControl";
    final static String previousTexture = "previous texture";
    final static String previousTrack = "previous track";
    final static String previousUserData = "previous userData";
    final static String previousVertex = "previous vertex";
    final static String previousViewMode = "previous viewMode";

    final static String reduceAnimation = "reduce animation";
    final static String reduceTrack = "reduce track";

    final static String renameAnimation = "rename animation";
    final static String renameBone = "rename bone";
    final static String renameLight = "rename light";
    final static String renameMaterial = "rename material";
    final static String renameOverride = "rename override";
    final static String renameSpatial = "rename spatial";
    final static String renameUserKey = "rename userKey";

    final static String resetBoneAngleToAnimation = "reset bone ang anim";
    final static String resetBoneAngleToBind = "reset bone ang bind";
    final static String resetBoneOffsetToAnimation = "reset bone off anim";
    final static String resetBoneOffsetToBind = "reset bone off bind";
    final static String resetBoneScaleToAnimation = "reset bone sca anim";
    final static String resetBoneScaleToBind = "reset bone sca bind";
    final static String resetBoneSelection = "reset bone selection";
    final static String resetLightColor = "reset light color";
    final static String resetLightDir = "reset light dir";
    final static String resetLightPos = "reset light pos";
    final static String resetSpatialRotation = "reset spatial rotation";
    final static String resetSpatialScale = "reset spatial scale";
    final static String resetSpatialTranslation = "reset spatial translation";
    final static String resetTwist = "reset twist";
    final static String resetVertexSelection = "reset vertex selection";

    final static String selectAnimationEditMenu = "select animationEditMenu";
    final static String selectAnimControl = "select animControl";
    final static String selectAxesDragEffect = "select axesDragEffect";
    final static String selectAxesSubject = "select axesSubject";
    final static String selectBackground = "select background";
    final static String selectBatchHint = "select batchHint";
    final static String selectBone = "select bone";
    final static String selectBoneChild = "select boneChild";
    final static String selectBoneParent = "select boneParent";
    final static String selectBoneTrack = "select boneTrack";
    final static String selectBuffer = "select buffer";
    final static String selectBufferUsage = "select bufferUsage";
    final static String selectCullHint = "select cullHint";
    final static String selectEdgeFilter = "select edgeFilter";
    final static String selectFaceCull = "select faceCull";
    final static String selectJoint = "select joint";
    final static String selectKeyframeFirst = "select keyframeFirst";
    final static String selectKeyframeLast = "select keyframeLast";
    final static String selectKeyframeNearest = "select keyframeNearest";
    final static String selectKeyframeNext = "select keyframeNext";
    final static String selectKeyframePrevious = "select keyframePrevious";
    final static String selectLight = "select light";
    final static String selectLightOwner = "select lightOwner";
    final static String selectLink = "select link";
    final static String selectLinkChild = "select linkChild";
    final static String selectLinkedBone = "select linkedBone";
    final static String selectLinkedJoint = "select linkedJoint";
    final static String selectLinkedPco = "select linkedPco";
    final static String selectLinkParent = "select linkParent";
    final static String selectLinkShape = "select linkShape";
    final static String selectLinkToolAxis = "select linkToolAxis";
    final static String selectLoadBvhAxisOrder = "select loadBvhAxisOrder";
    final static String selectMapSourceBone = "select mapSourceBone";
    final static String selectMapTargetBone = "select mapTargetBone";
    final static String selectMaterialEditMenu = "select materialEditMenu";
    final static String selectMatParam = "select matParam";
    final static String selectMeshMode = "select meshMode";
    final static String selectOrbitCenter = "select orbitCenter";
    final static String selectOverride = "select override";
    final static String selectPco = "select pco";
    final static String selectPcoParm = "select pcoParm";
    final static String selectPcoShape = "select pcoShape";
    final static String selectPlatformType = "select platformType";
    final static String selectQueueBucket = "select queueBucket";
    final static String selectSceneBones = "select sceneBones";
    final static String selectScoreBonesNone = "select scoreBonesNone";
    final static String selectScoreBonesWhen = "select scoreBonesWhen";
    final static String selectSgc = "select sgc";
    final static String selectSgcPco = "select sgcPco";
    final static String selectSgcSpatial = "select sgcSpatial";
    final static String selectShadowMode = "select shadowMode";
    final static String selectShape = "select shape";
    final static String selectShapeChild = "select shapeChild";
    final static String selectShapeParm = "select shapeParm";
    final static String selectShapeUser = "select shapeUser";
    final static String selectSkeletonColor = "select skeletonColor";
    final static String selectSourceAnimControl = "select sourceAnimControl";
    final static String selectSourceBone = "select sourceBone";
    final static String selectSpatialChild = "select spatialChild";
    final static String selectSpatialParent = "select spatialParent";
    final static String selectTexture = "select texture";
    final static String selectTextureMag = "select textureMag";
    final static String selectTextureMin = "select textureMin";
    final static String selectTextureType = "select textureType";
    final static String selectTextureUser = "select textureUser";
    final static String selectTrack = "select track";
    final static String selectTrackTarget = "select trackTarget";
    final static String selectTriangleMode = "select triangleMode";
    final static String selectTweenRotations = "select tweenRotations";
    final static String selectTweenScales = "select tweenScales";
    final static String selectTweenTranslations = "select tweenTranslations";
    final static String selectUserKey = "select userKey";
    final static String selectVertex = "select vertex";

    final static String setAnisotropy = "set anisotropy";
    final static String setBufferInstanceSpan = "set bufferInstanceSpan";
    final static String setBufferLimit = "set bufferLimit";
    final static String setBufferStride = "set bufferStride";
    final static String setDumpIndentSpaces = "set dumpIndentSpaces";
    final static String setDumpMaxChildren = "set dumpMaxChildren";
    final static String setLightDirCardinal = "set lightDir cardinal";
    final static String setLightDirReverse = "set lightDir reverse";
    final static String setLinkMass = "set linkMass";
    final static String setMatParamValue = "set matParamValue";
    final static String setMeshWeights = "set meshWeights";
    final static String setOverrideValue = "set overrideValue";
    final static String setPcoParmValue = "set pcoParmValue";
    final static String setShapeParmValue = "set shapeParmValue";
    final static String setSpatialAngleCardinal = "set spatialAngle cardinal";
    final static String setSpatialAngleSnapX = "set spatialAngle snapX";
    final static String setSpatialAngleSnapY = "set spatialAngle snapY";
    final static String setSpatialAngleSnapZ = "set spatialAngle snapZ";
    final static String setTextureClone = "set texture clone";
    final static String setTextureNull = "set texture null";
    final static String setTimeLimitLower = "set timeLimit lower";
    final static String setTimeLimitUpper = "set timeLimit upper";
    final static String setTrackRotationAll = "set track rotation all";
    final static String setTrackScaleAll = "set track scale all";
    final static String setTrackTranslationAll = "set track translation all";
    final static String setTwistCardinal = "set twist cardinal";
    final static String setTwistSnapX = "set twist snapX";
    final static String setTwistSnapY = "set twist snapY";
    final static String setTwistSnapZ = "set twist snapZ";
    final static String setUserData = "set userData";

    final static String toggleBoundType = "toggle boundType";
    final static String toggleCursorColorIndex = "toggle cursorColorIndex";
    final static String toggleDragSide = "toggle dragSide";
    final static String toggleFreezeTarget = "toggle freeze target";
    final static String toggleIndexBase = "toggle indexBase";
    final static String toggleLoadOrientation = "toggle loadOrientation";
    final static String toggleMenuBar = "toggle menuBar";
    final static String toggleMovement = "toggle movement";
    final static String togglePause = "toggle pause";
    final static String togglePauseSource = "toggle pause source";
    final static String togglePauseTarget = "toggle pause target";
    final static String togglePhysicsDebug = "toggle physics debug";
    final static String toggleProjection = "toggle projection";

    final static String viewHorizontal = "view horizontal";

    final static String warpCursor = "warp cursor";
    final static String warpLastCheckpoint = "warp lastCheckpoint";

    final static String wrapTrack = "wrap track";
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private Action() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Add actions to the specified InputMode without binding them. TODO some of
     * these may cause crashes
     *
     * @param m the InputMode to modify (not null)
     */
    public static void addUnbound(InputMode m) {
        m.addActionName(deleteAnimation);
        m.addActionName(deleteBuffer);
        m.addActionName(deleteLight);
        m.addActionName(deleteLink);
        m.addActionName(deleteMapping);
        m.addActionName(deleteMatParam);
        m.addActionName(deleteOverride);
        m.addActionName(deleteSgc);
        m.addActionName(deleteSingleKeyframe);
        m.addActionName(deleteTrackRotations);
        m.addActionName(deleteTrackScales);
        m.addActionName(deleteTrackTranslations);
        m.addActionName(deleteUserKey);

        m.addActionName(launchProjectile);

        m.addActionName(loadAnimation);
        m.addActionName(loadCgm);
        m.addActionName(loadMapAsset);
        m.addActionName(loadRetargetedPose);
        m.addActionName(loadSourceAnimation);
        m.addActionName(loadSourceCgm);
        m.addActionName(loadTexture);

        m.addActionName(newAnimation);
        m.addActionName(newAnimationFromExtract);
        m.addActionName(newAnimationFromPose);
        m.addActionName(newAnimationFromRetarget);
        m.addActionName(newAttachmentLink);
        m.addActionName(newBoneLink);
        m.addActionName(newCheckpoint);
        m.addActionName(newLight);
        m.addActionName(newMapping);
        m.addActionName(newMatParam);
        m.addActionName(newOverride);
        m.addActionName(newSgc);
        m.addActionName(newSingleKeyframe);
        m.addActionName(newTexture);
        m.addActionName(newUserKey);

        m.addActionName(nextAnimation);
        m.addActionName(nextAnimControl);
        m.addActionName(nextBone);
        m.addActionName(nextBuffer);
        m.addActionName(nextCheckpoint);
        m.addActionName(nextJoint);
        m.addActionName(nextLight);
        m.addActionName(nextLink);
        m.addActionName(nextMapping);
        m.addActionName(nextMatParam);
        m.addActionName(nextOverride);
        m.addActionName(nextPco);
        m.addActionName(nextPerformanceMode);
        m.addActionName(nextRotationDisplay);
        m.addActionName(nextSgc);
        m.addActionName(nextShape);
        m.addActionName(nextSourceAnimation);
        m.addActionName(nextSourceAnimControl);
        m.addActionName(nextTexture);
        m.addActionName(nextTrack);
        m.addActionName(nextUserData);
        m.addActionName(nextVertex);
        m.addActionName(nextViewMode);

        m.addActionName(pickAny);
        m.addActionName(pickBone);
        m.addActionName(pickGnomon);
        m.addActionName(pickKeyframe);
        m.addActionName(pickVertex);
        m.addActionName(pickViewMenu);

        m.addActionName(previousAnimation);
        m.addActionName(previousAnimControl);
        m.addActionName(previousBone);
        m.addActionName(previousBuffer);
        m.addActionName(previousCheckpoint);
        m.addActionName(previousJoint);
        m.addActionName(previousLight);
        m.addActionName(previousLink);
        m.addActionName(previousMapping);
        m.addActionName(previousMatParam);
        m.addActionName(previousOverride);
        m.addActionName(previousPco);
        m.addActionName(previousSgc);
        m.addActionName(previousShape);
        m.addActionName(previousSourceAnimation);
        m.addActionName(previousSourceAnimControl);
        m.addActionName(previousTexture);
        m.addActionName(previousTrack);
        m.addActionName(previousUserData);
        m.addActionName(previousVertex);
        m.addActionName(previousViewMode);

        m.addActionName(reduceAnimation);
        m.addActionName(reduceTrack);

        m.addActionName(renameAnimation);
        m.addActionName(renameBone);
        m.addActionName(renameLight);
        m.addActionName(renameMaterial);
        m.addActionName(renameOverride);
        m.addActionName(renameSpatial);
        m.addActionName(renameUserKey);

        m.addActionName(resetBoneAngleToAnimation);
        m.addActionName(resetBoneAngleToBind);
        m.addActionName(resetBoneOffsetToAnimation);
        m.addActionName(resetBoneOffsetToBind);
        m.addActionName(resetBoneScaleToAnimation);
        m.addActionName(resetBoneScaleToBind);
        m.addActionName(resetBoneSelection);
        m.addActionName(resetLightColor);
        m.addActionName(resetLightDir);
        m.addActionName(resetLightPos);
        m.addActionName(resetSpatialRotation);
        m.addActionName(resetSpatialScale);
        m.addActionName(resetSpatialTranslation);
        m.addActionName(resetTwist);
        m.addActionName(resetVertexSelection);

        m.addActionName(selectAnimationEditMenu);
        m.addActionName(selectAnimControl);
        m.addActionName(selectAxesDragEffect);
        m.addActionName(selectAxesSubject);
        m.addActionName(selectBackground);
        m.addActionName(selectBatchHint);
        m.addActionName(selectBone);
        m.addActionName(selectBoneChild);
        m.addActionName(selectBoneParent);
        m.addActionName(selectBoneTrack);
        m.addActionName(selectBuffer);
        m.addActionName(selectBufferUsage);
        m.addActionName(selectCullHint);
        m.addActionName(selectEdgeFilter);
        m.addActionName(selectFaceCull);
        m.addActionName(selectJoint);
        m.addActionName(selectKeyframeFirst);
        m.addActionName(selectKeyframeLast);
        m.addActionName(selectKeyframeNearest);
        m.addActionName(selectKeyframeNext);
        m.addActionName(selectKeyframePrevious);
        m.addActionName(selectLight);
        m.addActionName(selectLightOwner);
        m.addActionName(selectLink);
        m.addActionName(selectLinkChild);
        m.addActionName(selectLinkedBone);
        m.addActionName(selectLinkedJoint);
        m.addActionName(selectLinkedPco);
        m.addActionName(selectLinkParent);
        m.addActionName(selectLinkToolAxis);
        m.addActionName(selectLoadBvhAxisOrder);
        m.addActionName(selectMapSourceBone);
        m.addActionName(selectMapTargetBone);
        m.addActionName(selectMaterialEditMenu);
        m.addActionName(selectMatParam);
        m.addActionName(selectMeshMode);
        m.addActionName(selectOrbitCenter);
        m.addActionName(selectOverride);
        m.addActionName(selectPco);
        m.addActionName(selectPcoParm);
        m.addActionName(selectPcoShape);
        m.addActionName(selectPlatformType);
        m.addActionName(selectQueueBucket);
        m.addActionName(selectSceneBones);
        m.addActionName(selectScoreBonesNone);
        m.addActionName(selectScoreBonesWhen);
        m.addActionName(selectSgc);
        m.addActionName(selectSgcPco);
        m.addActionName(selectSgcSpatial);
        m.addActionName(selectShadowMode);
        m.addActionName(selectShape);
        m.addActionName(selectShapeChild);
        m.addActionName(selectShapeParm);
        m.addActionName(selectShapeUser);
        m.addActionName(selectSkeletonColor);
        m.addActionName(selectSourceAnimControl);
        m.addActionName(selectSourceBone);
        m.addActionName(selectSpatialChild);
        m.addActionName(selectSpatialParent);
        m.addActionName(selectTextureMag);
        m.addActionName(selectTextureMin);
        m.addActionName(selectTextureType);
        m.addActionName(selectTextureUser);
        m.addActionName(selectTrack);
        m.addActionName(selectTrackTarget);
        m.addActionName(selectTriangleMode);
        m.addActionName(selectTweenRotations);
        m.addActionName(selectTweenScales);
        m.addActionName(selectTweenTranslations);
        m.addActionName(selectUserKey);
        m.addActionName(selectVertex);

        m.addActionName(setAnisotropy);
        m.addActionName(setBufferInstanceSpan);
        m.addActionName(setBufferLimit);
        m.addActionName(setBufferStride);
        m.addActionName(setLightDirCardinal);
        m.addActionName(setLightDirReverse);
        m.addActionName(setLinkMass);
        m.addActionName(setMatParamValue);
        m.addActionName(setMeshWeights);
        m.addActionName(setOverrideValue);
        m.addActionName(setPcoParmValue);
        m.addActionName(setShapeParmValue);
        m.addActionName(setSpatialAngleCardinal);
        m.addActionName(setSpatialAngleSnapX);
        m.addActionName(setSpatialAngleSnapY);
        m.addActionName(setSpatialAngleSnapZ);
        m.addActionName(setTextureClone);
        m.addActionName(setTextureNull);
        m.addActionName(setTimeLimitLower);
        m.addActionName(setTimeLimitUpper);
        m.addActionName(setTrackRotationAll);
        m.addActionName(setTrackScaleAll);
        m.addActionName(setTrackTranslationAll);
        m.addActionName(setTwistCardinal);
        m.addActionName(setTwistSnapX);
        m.addActionName(setTwistSnapY);
        m.addActionName(setTwistSnapZ);
        m.addActionName(setUserData);

        m.addActionName(toggleBoundType);
        m.addActionName(toggleCursorColorIndex);
        m.addActionName(toggleDragSide);
        m.addActionName(toggleFreezeTarget);
        m.addActionName(toggleIndexBase);
        m.addActionName(toggleLoadOrientation);
        m.addActionName(toggleMenuBar);
        m.addActionName(toggleMovement);
        m.addActionName(togglePause);
        m.addActionName(togglePauseSource);
        m.addActionName(togglePauseTarget);
        m.addActionName(togglePhysicsDebug);
        m.addActionName(toggleProjection);

        m.addActionName(viewHorizontal);

        m.addActionName(warpCursor);
        m.addActionName(warpLastCheckpoint);

        m.addActionName(wrapTrack);
    }
}
