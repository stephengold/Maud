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
package maud.action;

import java.util.logging.Logger;

/**
 * Action-string prefixes for Maud's "editor" screen. Each prefix describes a
 * user-interface action requiring one or more (textual) arguments. By
 * convention, action prefixes end with a space (' ').
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class ActionPrefix {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(ActionPrefix.class.getName());
    /**
     * argument is a URL specification
     */
    final public static String deleteAssetLocationSpec
            = "delete assetLocationSpec ";
    /**
     * argument is a number of keyframes
     */
    final public static String deleteNextKeyframes = "delete nextKeyframes ";
    /**
     * argument is a number of keyframes
     */
    final public static String deletePreviousKeyframes
            = "delete previousKeyframes ";
    /**
     * argument is the name of an animation in the target C-G model
     */
    final public static String loadAnimation = "load animation ";
    /**
     * remainder is a location index followed by an asset path to a C-G model
     */
    final public static String loadCgmAsset = "load cgm asset ";
    /**
     * argument is a URL spec or defaultLocation
     */
    final public static String loadCgmLocator = "load cgm locator ";
    /**
     * argument is the name of a C-G model on the classpath or else otherName
     */
    final public static String loadCgmNamed = "load cgm named ";
    /**
     * argument is a location index followed by an asset path to a skeleton map
     */
    final public static String loadMapAsset = "load map asset ";
    /**
     * argument is URL spec or else defaultLocation or identityForSource or
     * identityForTarget
     */
    final public static String loadMapLocator = "load map locator ";
    /**
     * argument is the name of a map asset on the classpath
     */
    final public static String loadMapNamed = "load map named ";
    /**
     * argument is the name of an animation in the source C-G model
     */
    final public static String loadSourceAnimation = "load sourceAnimation ";
    /**
     * argument is a location index followed by an asset path to a C-G model
     */
    final public static String loadSourceCgmAsset = "load sourceCgm asset ";
    /**
     * argument is a URL spec or else defaultLocation
     */
    final public static String loadSourceCgmLocator = "load sourceCgm locator ";
    /**
     * argument is the name of a C-G model on the classpath
     */
    final public static String loadSourceCgmNamed = "load sourceCgm named ";
    /**
     * argument is a location index followed by an asset path to a texture
     */
    final public static String loadTextureAsset = "load texture asset ";
    /**
     * argument is a URL spec or else defaultLocation
     */
    final public static String loadTextureLocator = "load texture locator ";
    /**
     * arguments are 2 whichCgms and a name for the new animation
     */
    final public static String newAnimationFromChain
            = "new animation fromChain ";
    /**
     * argument is a name for the new animation
     */
    final public static String newAnimationFromCopy = "new animation fromCopy ";
    /**
     * argument is a name for the new animation
     */
    final public static String newAnimationFromExtract
            = "new animation fromExtract ";
    /**
     * arguments are a comma-separated list of decimal track indices and an
     * optional name for the new animation
     */
    final public static String newAnimationFromMix = "new animation fromMix ";
    /**
     * argument is a name for the new animation
     */
    final public static String newAnimationFromPose = "new animation fromPose ";
    /**
     * argument is a name for the new animation
     */
    final public static String newAnimationFromRetarget
            = "new animation fromRetarget ";
    /**
     * argument is a filesystem path to a directory/folder/JAR/ZIP
     */
    final public static String newAssetLocation = "new assetLocation ";
    /**
     * argument is a URL specification
     */
    final public static String newAssetLocationSpec = "new assetLocationSpec ";
    /**
     * argument is the name of a ShapeType
     */
    final public static String newGhostControl = "new ghostControl ";
    /**
     * argument is a name for the new S-G node
     */
    final public static String newLeafNode = "new leafNode ";
    /**
     * arguments are ("CopySelected" or the name of a Light.Type) and an
     * optional light name
     */
    final public static String newLight = "new light ";
    /**
     * argument is the name of a material parameter or a prefix thereof
     */
    final public static String newMatParam = "new matParam ";
    /**
     * argument is the name of a ShapeType
     */
    final public static String newMcc = "new mcc ";
    /**
     * arguments are the name of the VarType and an optional parameter name
     */
    final public static String newOverride = "new override ";
    /**
     * argument is a name for the new S-G node
     */
    final public static String newParent = "new parent ";
    /**
     * argument is the name of a ShapeType
     */
    final public static String newRbc = "new rbc ";
    /**
     * arguments are the new type and optional key
     */
    final public static String newUserKey = "new userKey ";
    /**
     * argument is a reduction factor
     */
    final public static String reduceAnimation = "reduce animation ";
    /**
     * argument is a reduction factor
     */
    final public static String reduceTrack = "reduce track ";
    /**
     * argument is the new name for the loaded animation
     */
    final public static String renameAnimation = "rename animation ";
    /**
     * argument is the new name for the selected bone
     */
    final public static String renameBone = "rename bone ";
    /**
     * argument is the new name for the light
     */
    final public static String renameLight = "rename light ";
    /**
     * argument is the new name for the material
     */
    final public static String renameMaterial = "rename material ";
    /**
     * argument is the new name for the material-parameter override
     */
    final public static String renameOverride = "rename override ";
    /**
     * argument is the new name for the selected spatial
     */
    final public static String renameSpatial = "rename spatial ";
    /**
     * argument is the new name for the key
     */
    final public static String renameUserKey = "rename userKey ";
    /**
     * arguments are a resample type and a decimal count/rate
     */
    final public static String resampleAnimation = "resample animation ";
    /**
     * arguments are a resample type and a decimal count/rate
     */
    final public static String resampleTrack = "resample track ";
    /**
     * arguments are an output-format type and a base file path
     */
    final public static String saveCgm = "save cgm ";
    /**
     * arguments are an output-format type and a base file path
     */
    final public static String saveCgmUnconfirmed = "save cgmUnconfirmed ";
    /**
     * arguments are an output-format type and a base file path
     */
    final public static String saveMap = "save map ";
    /**
     * arguments are an output-format type and a base file path
     */
    final public static String saveMapUnconfirmed = "save mapUnconfirmed ";
    /**
     * arguments are a spatial name and a deduplication index
     */
    final public static String selectAnimControl = "select animControl ";
    /**
     * argument is the name of a scene-view axis drag effect
     */
    final public static String selectAxesDragEffect = "select axesDragEffect ";
    /**
     * argument is the name of a scene-view axis subject
     */
    final public static String selectAxesSubject = "select axesSubject ";
    /**
     * argument is the name of a background
     */
    final public static String selectBackground = "select background ";
    /**
     * argument is the name of a batch hint
     */
    final public static String selectBatchHint = "select batchHint ";
    /**
     * argument is the name of a target bone or a prefix thereof
     */
    final public static String selectBone = "select bone ";
    /**
     * argument is the name of a target bone, possibly preceded by "!"
     */
    final public static String selectBoneChild = "select boneChild ";
    /**
     * argument is a decimal index (indexBase plus the bone index)
     */
    final public static String selectBoneIndex = "select boneIndex ";
    /**
     * argument is a buffer description or a prefix thereof
     */
    final public static String selectBuffer = "select buffer ";
    /**
     * argument is the name of a VertexBuffer.Usage
     */
    final public static String selectBufferUsage = "select bufferUsage ";
    /**
     * argument is the name of a cull hint
     */
    final public static String selectCullHint = "select cullHint ";
    /**
     * argument is 0 or 1 (index into the 2 cursor colors)
     */
    final public static String selectCursorColor = "select cursorColor ";
    /**
     * argument is the name of an EdgeFilteringMode value
     */
    final public static String selectEdgeFilter = "select edgeFilter ";
    /**
     * arguments are 3 float values
     */
    final public static String selectExtremeVertex = "select extremeVertex ";
    /**
     * argument is the name of a RenderState.FaceCullMode value
     */
    final public static String selectFaceCull = "select faceCull ";
    /**
     * argument is 0 or 1
     */
    final public static String selectIndexBase = "select indexBase ";
    /**
     * argument is the ID of a physics joint or a prefix thereof
     */
    final public static String selectJoint = "select joint ";
    /**
     * argument is the name of a JointEnd
     */
    final public static String selectJointBody = "select jointBody ";
    /**
     * argument is a decimal index (indexBase plus a keyframe index)
     */
    final public static String selectKeyframe = "select keyframe ";
    /**
     * argument is a light name or noLight
     */
    final public static String selectLight = "select light ";
    /**
     * argument is the name of a PhysicsLink
     */
    final public static String selectLink = "select link ";
    /**
     * argument is the name of a PhysicsLink, possibly preceded by "!"
     */
    final public static String selectLinkChild = "select linkChild ";
    /**
     * argument is the name of a ShapeHeuristic
     */
    final public static String selectLinkShape = "select linkShape ";
    /**
     * argument is the name of a coordinate axis
     */
    final public static String selectLinkToolAxis = "select linkToolAxis ";
    /**
     * argument is the name of a LoadBvhAxisOrder
     */
    final public static String selectLoadBvhAxisOrder
            = "select loadBvhAxisOrder ";
    /**
     * argument is the name of a pre-existing material parameter or a prefix
     * thereof
     */
    final public static String selectMatParam = "select matParam ";
    /**
     * argument is the menu path of a menu item
     */
    final public static String selectMenuItem = "select menuItem ";
    /**
     * argument is the name of a mesh mode
     */
    final public static String selectMeshMode = "select meshMode ";
    /**
     * argument is the name of a movement-mode enum value
     */
    final public static String selectMovement = "select movement ";
    /**
     * argument is the name of an orbit-center enum value
     */
    final public static String selectOrbitCenter = "select orbitCenter ";
    /**
     * argument is a pre-existing parameter name
     */
    final public static String selectOverride = "select override ";
    /**
     * argument is the name of a physics collision object or a prefix thereof
     */
    final public static String selectPco = "select pco ";
    /**
     * argument is the name of a RigidBodyParameter
     */
    final public static String selectPcoParm = "select pcoParm ";
    /**
     * argument is the name of a performance mode
     */
    final public static String selectPerformanceMode
            = "select performanceMode ";
    /**
     * argument is the name of a PlatformType
     */
    final public static String selectPlatformType = "select platformType ";
    /**
     * argument is the name of a projection-mode enum value
     */
    final public static String selectProjection = "select projection ";
    /**
     * argument is the name of a queue bucket
     */
    final public static String selectQueueBucket = "select queueBucket ";
    /**
     * argument is the name of a RotationDisplayMode value
     */
    final public static String selectRotationDisplay
            = "select rotationDisplay ";
    /**
     * argument is the name of a ShowBones value
     */
    final public static String selectSceneBones = "select sceneBones ";
    /**
     * argument is the name of a ShowBones value
     */
    final public static String selectScoreBonesNone = "select scoreBonesNone ";
    /**
     * argument is the name of a ShowBones value
     */
    final public static String selectScoreBonesWhen = "select scoreBonesWhen ";
    /**
     * argument is an SGC name or noControl
     */
    final public static String selectSgc = "select sgc ";
    /**
     * argument is the name of a shadow mode
     */
    final public static String selectShadowMode = "select shadowMode ";
    /**
     * argument is the name of a physics shape or a prefix thereof
     */
    final public static String selectShape = "select shape ";
    /**
     * argument is the name of a ShapeParameter
     */
    final public static String selectShapeParm = "select shapeParm ";
    /**
     * argument is the name of a physics shape or collision object or a prefix
     * thereof
     */
    final public static String selectShapeUser = "select shapeUser ";
    /**
     * argument is the name of a SkeletonColors
     */
    final public static String selectSkeletonColor = "select skeletonColor ";
    /**
     * arguments are a spatial name and a de-duplication index
     */
    final public static String selectSourceAnimControl
            = "select sourceAnimControl ";
    /**
     * argument is the name of a source bone or a prefix thereof
     */
    final public static String selectSourceBone = "select sourceBone ";
    /**
     * arguments are the name of a WhichSpatials value and an optional spatial
     * name or prefix thereof
     */
    final public static String selectSpatial = "select spatial ";
    /**
     * argument is a quoted name plus bracketed index, or a prefix thereof
     */
    final public static String selectSpatialChild = "select spatialChild ";
    /**
     * argument is the description of either a non-null texture or a
     * null-texture reference
     */
    final public static String selectTexture = "select texture ";
    /**
     * argument is the name of a mag-filter mode
     */
    final public static String selectTextureMag = "select textureMag ";
    /**
     * argument is the name of a min-filter mode
     */
    final public static String selectTextureMin = "select textureMin ";
    /**
     * argument is the description of a texture type
     */
    final public static String selectTextureType = "select textureType ";
    /**
     * arguments are the name of a texture axis and an optional name of a wrap
     * mode
     */
    final public static String selectTextureWrap = "select textureWrap ";
    /**
     * argument is the name of a tool window
     */
    final static String selectTool = "select tool ";
    /**
     * arguments are the name of a tool window and decimal x,y coordinates
     */
    final public static String selectToolAt = "select toolAt ";
    /**
     * argument is the description of a track item or a prefix thereof
     */
    final public static String selectTrack = "select track ";
    /**
     * argument is the name of a triangle mode
     */
    final public static String selectTriangleMode = "select triangleMode ";
    /**
     * argument is the name of a tween preset
     */
    final public static String selectTweenPreset = "select tweenPreset ";
    /**
     * argument is the name of a quaternion interpolation technique
     */
    final public static String selectTweenRotations = "select tweenRotations ";
    /**
     * argument is the name of a vector interpolation technique
     */
    final public static String selectTweenScales = "select tweenScales ";
    /**
     * argument is the name of a vector interpolation technique
     */
    final public static String selectTweenTranslations
            = "select tweenTranslations ";
    /**
     * argument is a pre-existing user key
     */
    final public static String selectUserKey = "select userKey ";
    /**
     * argument is a decimal index (indexBase plus a vertex index)
     */
    final public static String selectVertex = "select vertex ";
    /**
     * argument is a weight index (&ge;0, &lt;4)
     */
    final static String selectVertexBone = "select vertexBone ";
    /**
     * argument is the name of a view mode
     */
    final public static String selectViewMode = "select viewMode ";
    /**
     * arguments are a decimal index and a color
     */
    final public static String set3DCursorColor = "set 3DCursorColor ";
    /**
     * argument is a decimal interval in seconds
     */
    final public static String set3DCursorCycleTime = "set 3DCursorCycleTime ";
    /**
     * argument is a decimal float
     */
    final public static String set3DCursorSize = "set 3DCursorSize ";
    /**
     * argument is a decimal float
     */
    final public static String setAmbientLevel = "set ambientLevel ";
    /**
     * argument is a decimal integer
     */
    final public static String setAnisotropy = "set anisotropy ";
    /**
     * argument is a decimal width in pixels
     */
    final public static String setAxesLineWidth = "set axesLineWidth ";
    /**
     * arguments are the name of a Background and a color
     */
    final public static String setBackgroundColor = "set backgroundColor ";
    /**
     * argument is a color
     */
    final public static String setBoundsColor = "set boundsColor ";
    /**
     * argument is a decimal width in pixels
     */
    final public static String setBoundsLineWidth = "set boundsLineWidth ";
    /**
     * argument is a decimal number of instances
     */
    final public static String setBufferInstanceSpan
            = "set bufferInstanceSpan ";
    /**
     * argument is a decimal element index
     */
    final public static String setBufferLimit = "set bufferLimit ";
    /**
     * argument is a decimal number of bytes
     */
    final public static String setBufferStride = "set bufferStride ";
    /**
     * argument is a decimal fraction
     */
    final public static String setCloudiness = "set cloudiness ";
    /**
     * argument is a decimal distance in physics-space units
     */
    final public static String setDefaultMargin = "set defaultMargin ";
    /**
     * argument is a decimal number of spaces
     */
    final public static String setDumpIndentSpaces = "set dumpIndentSpaces ";
    /**
     * argument is a decimal number of children
     */
    final public static String setDumpMaxChildren = "set dumpMaxChildren ";
    /**
     * argument is a decimal duration in seconds
     */
    final public static String setDurationProportional
            = "set duration proportional ";
    /**
     * argument is a decimal duration in seconds
     */
    final public static String setDurationSame = "set duration same ";
    /**
     * argument is a decimal time in seconds
     */
    final public static String setFrameTime = "set frameTime ";
    /**
     * argument is a decimal float
     */
    final public static String setHour = "set hour ";
    /**
     * argument is a decimal mass
     */
    final public static String setLinkMass = "set linkMass ";
    /**
     * argument is a Vector3f value
     */
    final public static String setMainDirection = "set mainDirection ";
    /**
     * argument is a decimal float
     */
    final public static String setMainLevel = "set mainLevel ";
    /**
     * argument is a decimal size in bits
     */
    final public static String setMapSize = "set mapSize ";
    /**
     * argument is a material-parameter value
     */
    final static String setMatParamValue = "set matParamValue ";
    /**
     * argument is a decimal number of weights
     */
    final public static String setMeshWeights = "set meshWeights ";
    /**
     * argument is a decimal number of splits
     */
    final public static String setNumSplits = "set numSplits ";
    /**
     * argument is a material-parameter value or "null"
     */
    final static String setOverrideValue = "set overrideValue ";
    /**
     * arguments are the parameter name and a decimal value
     */
    final public static String setPcoParmValue = "set pcoParmValue ";
    /**
     * arguments are whichCgm and a decimal diameter in world units
     */
    final public static String setPlatformDiameter = "set platformDiameter ";
    /**
     * arguments are the parameter name and a decimal float
     */
    final public static String setShapeParmValue = "set shapeParmValue ";
    /**
     * arguments are the name of a SkeletonColors and a color
     */
    final public static String setSkeletonColor = "set skeletonColor ";
    /**
     * argument is a decimal width in pixels
     */
    final public static String setSkeletonLineWidth = "set skeletonLineWidth ";
    /**
     * argument is a decimal size in pixels
     */
    final public static String setSkeletonPointSize = "set skeletonPointSize ";
    /**
     * arguments are 2 decimal fractions
     */
    final public static String setSubmenuWarp = "set submenuWarp ";
    /**
     * arguments are whichCgm, a playTime, and an optional decimal float (time
     * in seconds)
     */
    final public static String setTime = "set time ";
    /**
     * arguments are whichCgm, a playTime, and an optional decimal integer
     * (indexBase plus a frame index)
     */
    final public static String setTimeToFrame = "set timeToFrame ";
    /**
     * argument is the new value
     */
    final public static String setUserData = "set userData ";
    /**
     * argument is a color
     */
    final public static String setVertexColor = "set vertexColor ";
    /**
     * arguments are a zero-base component index (0-3) and an optional decimal
     * value
     */
    final public static String setVertexData = "set vertexData ";
    /**
     * argument is a decimal size in pixels
     */
    final public static String setVertexPointSize = "set vertexPointSize ";
    /**
     * argument is a decimal fraction
     */
    final public static String setXBoundary = "set xBoundary ";
    /**
     * argument is a boolean value
     */
    final public static String sf3DCursorVisible = "setFlag 3DCursorVisible ";
    /**
     * argument is a boolean value
     */
    final public static String sfAxesDepthTest = "setFlag axesDepthTest ";
    /**
     * argument is a boolean value
     */
    final public static String sfBoundsDepthTest = "setFlag boundsDepthTest ";
    /**
     * argument is a boolean value
     */
    final public static String sfDiagnose = "setFlag diagnose ";
    /**
     * argument is a boolean value
     */
    final public static String sfLoadZUp = "setFlag loadZUp ";
    /**
     * argument is a boolean value
     */
    final public static String sfDumpBuckets = "setFlag dumpBuckets ";
    /**
     * argument is a boolean value
     */
    final public static String sfDumpCullHints = "setFlag dumpCullHints ";
    /**
     * argument is a boolean value
     */
    final public static String sfDumpJib = "setFlag dumpJib ";
    /**
     * argument is a boolean value
     */
    final public static String sfDumpJis = "setFlag dumpJis ";
    /**
     * argument is a boolean value
     */
    final public static String sfDumpMatParams = "setFlag dumpMatParams ";
    /**
     * argument is a boolean value
     */
    final public static String sfDumpMpo = "setFlag dumpMpo ";
    /**
     * argument is a boolean value
     */
    final public static String sfDumpShadows = "setFlag dumpShadows ";
    /**
     * argument is a boolean value
     */
    final public static String sfDumpTforms = "setFlag dumpTforms ";
    /**
     * argument is a boolean value
     */
    final public static String sfDumpUserData = "setFlag dumpUserData ";
    /**
     * argument is a boolean value
     */
    final public static String sfMenuBarVisible = "setFlag menuBarVisible ";
    /**
     * argument is a boolean value
     */
    final public static String sfPhysicsRendered = "setFlag physicsRendered ";
    /**
     * argument is a boolean value
     */
    final public static String sfShadowsRendered = "setFlag shadowsRendered ";
    /**
     * argument is a boolean value
     */
    final public static String sfShowRotations = "setFlag showRotations ";
    /**
     * argument is a boolean value
     */
    final public static String sfShowScales = "setFlag showScales ";
    /**
     * argument is a boolean value
     */
    final public static String sfShowTranslations = "setFlag showTranslations ";
    /**
     * argument is a boolean value
     */
    final public static String sfSkySimulated = "setFlag skySimulated ";
    /**
     * argument is a boolean value
     */
    final public static String sfTexturePreviewVisible
            = "setFlag texturePreviewVisible ";
    /**
     * argument is the name of a license type
     */
    final public static String viewLicense = "view license ";
    /**
     * argument is a decimal fraction (the weight to apply to any end-time
     * keyframes)
     */
    final public static String wrapAnimation = "wrap animation ";
    /**
     * argument is a decimal fraction (the weight to apply to any end-time
     * keyframe)
     */
    final public static String wrapTrack = "wrap track ";
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private ActionPrefix() {
    }
}
