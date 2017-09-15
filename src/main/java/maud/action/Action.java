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
package maud.action;

import java.util.logging.Logger;

/**
 * Action strings for Maud's "editor" screen. Each string describes a
 * user-interface action. Each string defined here should appear somewhere in
 * "editor.xml" and/or "editor.properties". By convention, action strings begin
 * with a verb in all lowercase and never end with a blank (' ').
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class Action {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            Action.class.getName());

    final public static String deleteAnimation = "delete animation";
    final static String deleteMapping = "delete mapping";
    final public static String deleteSgc = "delete sgc";
    final static String deleteSingleKeyframe = "delete singleKeyframe";
    final static String deleteUserKey = "delete userKey";

    final static String loadAnimation = "load animation";
    final static String loadCgm = "load cgm";
    final static String loadMapAsset = "load map asset";
    final static String loadRetargetedPose = "load retargetedPose";
    final static String loadSourceAnimation = "load sourceAnimation";
    final static String loadSourceCgm = "load sourceCgm";

    final static String nextAnimation = "next animation";
    final static String nextAnimControl = "next animControl";
    final static String nextBone = "next bone";
    final static String nextCheckpoint = "next checkpoint";
    final static String nextJoint = "next joint";
    final static String nextMapping = "next mapping";
    final static String nextPhysics = "next physics";
    final static String nextSgc = "next sgc";
    final static String nextShape = "next shape";
    final static String nextSourceAnimation = "next sourceAnimation";
    final static String nextSourceAnimControl = "next sourceAnimControl";
    final static String nextUserData = "next userData";
    final static String nextVertex = "next vertex";
    final static String nextViewMode = "next viewMode";

    final static String newAnimationFromPose = "new animation fromPose";
    final static String newCheckpoint = "new checkpoint";
    final static String newMapping = "new mapping";
    final static String newSgc = "new sgc";
    final static String newSingleKeyframe = "new singleKeyframe";
    final static String newUserKey = "new userKey";

    final static String previousAnimation = "previous animation";
    final static String previousAnimControl = "previous animControl";
    final static String previousBone = "previous bone";
    final static String previousCheckpoint = "previous checkpoint";
    final static String previousJoint = "previous joint";
    final static String previousMapping = "previous mapping";
    final static String previousPhysics = "previous physics";
    final static String previousSgc = "previous sgc";
    final static String previousShape = "previous shape";
    final static String previousSourceAnimation = "previous sourceAnimation";
    final static String previousSourceAnimControl = "previous sourceAnimControl";
    final static String previousUserData = "previous userData";
    final static String previousVertex = "previous vertex";
    final static String previousViewMode = "previous viewMode";

    final static String reduceAnimation = "reduce animation";
    final static String reduceTrack = "reduce track";

    final static String renameAnimation = "rename animation";
    final static String renameBone = "rename bone";
    final static String renameSpatial = "rename spatial";
    final static String renameUserKey = "rename userKey";

    final static String resetBoneAngleToAnimation = "reset bone ang anim";
    final static String resetBoneAngleToBind = "reset bone ang bind";
    final static String resetBoneOffsetToAnimation = "reset bone off anim";
    final static String resetBoneOffsetToBind = "reset bone off bind";
    final static String resetBoneScaleToAnimation = "reset bone sca anim";
    final static String resetBoneScaleToBind = "reset bone sca bind";
    final static String resetBoneSelection = "reset bone selection";
    final static String resetSpatialRotation = "reset spatial rotation";
    final static String resetSpatialScale = "reset spatial scale";
    final static String resetSpatialTranslation = "reset spatial translation";
    final static String resetTwist = "reset twist";
    final static String resetVertexSelection = "reset vertex selection";

    final static String retargetAnimation = "retarget animation";

    final static String selectAnimControl = "select animControl";
    final static String selectBone = "select bone";
    final static String selectBoneChild = "select boneChild";
    final static String selectBoneParent = "select boneParent";
    final static String selectJoint = "select joint";
    final static String selectKeyframeFirst = "select keyframeFirst";
    final static String selectKeyframeLast = "select keyframeLast";
    final static String selectKeyframeNearest = "select keyframeNearest";
    final static String selectKeyframeNext = "select keyframeNext";
    final static String selectKeyframePrevious = "select keyframePrevious";
    final static String selectMapSourceBone = "select mapSourceBone";
    final static String selectMapTargetBone = "select mapTargetBone";
    final static String selectOrbitCenter = "select orbitCenter";
    final static String selectPhysics = "select physics";
    final static String selectPhysicsShape = "select physicsShape";
    final static String selectScreenBone = "select screenBone";
    final static String selectScreenGnomon = "select screenGnomon";
    final static String selectScreenKeyframe = "select screenKeyframe";
    final static String selectScreenVertex = "select screenVertex";
    final static String selectScreenXY = "select screenXY";
    final static String selectSgc = "select sgc";
    final static String selectSgcObject = "select sgcObject";
    final static String selectShape = "select shape";
    final static String selectShapeChild = "select shapeChild";
    final static String selectSourceAnimControl = "select sourceAnimControl";
    final static String selectSourceBone = "select sourceBone";
    final static String selectSpatialChild = "select spatialChild";
    final static String selectSpatialParent = "select spatialParent";
    final static String selectUserKey = "select userKey";
    final static String selectVertex = "select vertex";

    final static String setBatchHint = "set batchHint";
    final static String setCullHint = "set cullHint";
    final static String setQueueBucket = "set queueBucket";
    final static String setSceneBones = "set sceneBones";
    final static String setScoreBonesNone = "set scoreBonesNone";
    final static String setScoreBonesWhen = "set scoreBonesWhen";
    final static String setShadowMode = "set shadowMode";
    final static String setTrackRotationAll = "set track rotation all";
    final static String setTrackScaleAll = "set track scale all";
    final static String setTrackTranslationAll = "set track translation all";
    final static String setTweenRotations = "set tweenRotations";
    final static String setTweenScales = "set tweenScales";
    final static String setTweenTranslations = "set tweenTranslations";
    final static String setTwistCardinal = "set twist cardinal";
    final static String setTwistSnapX = "set twist snapX";
    final static String setTwistSnapY = "set twist snapY";
    final static String setTwistSnapZ = "set twist snapZ";
    final static String setUserData = "set userData";

    final static String toggleDegrees = "toggle degrees";
    final static String toggleDragSide = "toggle dragSide";
    final static String toggleFreezeTarget = "toggle freeze target";
    final static String togglePause = "toggle pause";
    final static String togglePauseSource = "toggle pause source";
    final static String togglePauseTarget = "toggle pause target";
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
}
