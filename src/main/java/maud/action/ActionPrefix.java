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
 * Action-string prefixes for Maud's "editor" screen. Each prefix describes a
 * user-interface action requiring one or more (textual) arguments. By
 * convention, action prefixes end with a blank (' ').
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class ActionPrefix {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            ActionPrefix.class.getName());
    /**
     * argument is a name for the new animation
     */
    final public static String copyAnimation = "copy animation ";
    /**
     * argument is a filesystem path to an asset directory/folder/JAR/ZIP
     */
    final public static String deleteAssetFolder = "delete assetFolder ";
    /**
     * argument is the name of an animation in the target CG model
     */
    final public static String loadAnimation = "load animation ";
    /**
     * remainder is a location index followed by an asset path to a CG model
     */
    final public static String loadCgmAsset = "load cgm asset ";
    /**
     * argument is an asset directory/folder/JAR/ZIP or else "From classpath"
     */
    final public static String loadCgmLocator = "load cgm locator ";
    /**
     * argument is the name of a CG model on the classpath
     */
    final public static String loadCgmNamed = "load cgm named ";
    /**
     * argument is an location index followed by an asset path to a skeleton map
     */
    final public static String loadMapAsset = "load map asset ";
    /**
     * argument is an asset directory/folder/JAR/ZIP or else "From classpath" or
     * "Identity for source" or "Identity for target"
     */
    final public static String loadMapLocator = "load map locator ";
    /**
     * argument is the name of a map asset on the classpath
     */
    final public static String loadMapNamed = "load map named ";
    /**
     * argument is the name of an animation in the source CG model
     */
    final public static String loadSourceAnimation = "load sourceAnimation ";
    /**
     * argument is a location index followed by an asset path to a CG model
     */
    final public static String loadSourceCgmAsset = "load sourceCgm assetFolder ";
    /**
     * argument is a directory/folder/JAR/ZIP or else "From classpath"
     */
    final public static String loadSourceCgmLocator = "load sourceCgm locator ";
    /**
     * argument is the name of a CG model on the classpath
     */
    final public static String loadSourceCgmNamed = "load sourceCgm named ";
    /**
     * argument is a name for the new animation
     */
    final public static String newAnimationFromPose = "new animation fromPose ";
    /**
     * argument is a filesystem path to a folder/directory optionally with a
     * magic filename
     */
    final public static String newAssetFolder = "new assetFolder ";
    /**
     * arguments are the new type, key, and value
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
     * argument is the new name for the selected spatial
     */
    final public static String renameSpatial = "rename spatial ";
    /**
     * argument is the new name for the key
     */
    final public static String renameUserKey = "rename userKey ";
    /**
     * argument is the name for the new animation
     */
    final public static String retargetAnimation = "retarget animation ";
    /**
     * argument is a base file path
     */
    final public static String saveCgm = "save cgm ";
    /**
     * argument is a base file path
     */
    final public static String saveMap = "save map ";
    /**
     * arguments are a spatial name and a deduplication index
     */
    final static String selectAnimControl = "select animControl ";
    /**
     * argument is the name of a bone or a prefix thereof
     */
    final public static String selectBone = "select bone ";
    /**
     * argument is the name of a target bone, possibly preceded by "!"
     */
    final public static String selectBoneChild = "select boneChild ";
    /**
     * argument is an index or noControl
     */
    final public static String selectControl = "select control ";
    /**
     * argument is the name of a geometry or a prefix thereof
     */
    final public static String selectGeometry = "select geometry ";
    /**
     * argument is the menu path of a menu item
     */
    final public static String selectMenuItem = "select menuItem ";
    /**
     * arguments are a spatial name and a deduplication index
     */
    final static String selectSourceAnimControl = "select sourceAnimControl ";
    /**
     * argument is the name of a source bone or a prefix thereof
     */
    final public static String selectSourceBone = "select sourceBone ";
    /**
     * argument is the name of a spatial or a prefix thereof
     */
    final public static String selectSpatial = "select spatial ";
    /**
     * arguments are the quoted name of a spatial and a bracketed index
     */
    final public static String selectSpatialChild = "select spatialChild ";
    /**
     * argument is the name of a tool window
     */
    final static String selectTool = "select tool ";
    /**
     * argument is a pre-existing user key
     */
    final public static String selectUserKey = "select userKey ";
    /**
     * argument is the name of a batch hint
     */
    final public static String setBatchHint = "set batchHint ";
    /**
     * argument is the name of a cull hint
     */
    final public static String setCullHint = "set cullHint ";
    /**
     * argument is a duration in seconds
     */
    final public static String setDuration = "set duration ";
    /**
     * argument is the name of a queue bucket
     */
    final public static String setQueueBucket = "set queueBucket ";
    /**
     * argument is the name of a shadow mode
     */
    final public static String setShadowMode = "set shadowMode ";
    /**
     * argument is the name of a quaternion interpolation technique
     */
    final public static String setTweenRotations = "set tweenRotations ";
    /**
     * argument is the name of a vector interpolation technique
     */
    final public static String setTweenScales = "set tweenScales ";
    /**
     * argument is the name of a vector interpolation technique
     */
    final public static String setTweenTranslations = "set tweenTranslations ";
    /**
     * argument is the new value
     */
    final public static String setUserData = "set userData ";
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private ActionPrefix() {
    }
}
