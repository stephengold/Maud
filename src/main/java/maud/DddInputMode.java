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

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.cursors.plugins.JmeCursor;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.ui.InputMode;
import maud.model.History;

/**
 * Input mode for Maud's "3D View" screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class DddInputMode extends InputMode {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            DddInputMode.class.getName());
    /**
     * local copy of {@link com.jme3.math.Quaternion#IDENTITY}
     */
    final private static Quaternion identityRotation = new Quaternion();
    /**
     * asset path to the cursor for this mode
     */
    final private static String assetPath = "Textures/cursors/default.cur";
    /**
     * action prefix: remainder is a name for the new animation
     */
    final static String copyAnimationPrefix = "copy animation ";
    /**
     * action prefix: remainder is the name of an animation
     */
    final static String loadAnimationPrefix = "load animation ";
    /**
     * action prefix: remainder is an asset path to a CG model
     */
    final static String loadCgmAssetPrefix = "load cgm asset ";
    /**
     * action prefix: remainder is a file path to a CG model
     */
    final static String loadCgmFilePrefix = "load cgm file ";
    /**
     * action prefix: remainder is the name of a CG model in jme3-testdata
     */
    final static String loadCgmNamedPrefix = "load cgm named ";
    /**
     * action prefix: remainder is an asset path to a skeleton mapping
     */
    final static String loadMappingAssetPrefix = "load mapping asset ";
    /**
     * action prefix: remainder is the name of a source animation
     */
    final static String loadSourceAnimationPrefix = "load sourceAnimation ";
    /**
     * action prefix: remainder is an asset path to a CG model
     */
    final static String loadSourceCgmAssetPrefix = "load sourceCgm asset ";
    /**
     * action prefix: remainder is a file path to a CG model
     */
    final static String loadSourceCgmFilePrefix = "load sourceCgm file ";
    /**
     * action prefix: remainder is the name of a CG model in jme3-testdata
     */
    final static String loadSourceCgmNamedPrefix = "load sourceCgm named ";
    /**
     * action prefix: remainder is a name for the new animation
     */
    final static String newPosePrefix = "new pose ";
    /**
     * action prefix: remainder consists of the new type, key, and value
     */
    final static String newUserKeyPrefix = "new userKey ";
    final static String reduceAnimationPrefix = "reduce animation ";
    final static String reduceTrackPrefix = "reduce track ";
    /**
     * action prefix: remainder is the new name for the loaded animation
     */
    final static String renameAnimationPrefix = "rename animation ";
    /**
     * action prefix: remainder is the new name for the selected bone
     */
    final static String renameBonePrefix = "rename bone ";
    /**
     * action prefix: remainder is the new name for the selected spatial
     */
    final static String renameSpatialPrefix = "rename spatial ";
    /**
     * action prefix: remainder is the new name for the key
     */
    final static String renameUserKeyPrefix = "rename userKey ";
    /**
     * action prefix: remainder is the name for the new animation
     */
    final static String retargetAnimationPrefix = "retarget animation ";
    /**
     * action prefix: remainder is a base asset path
     */
    final static String saveCgmAssetPrefix = "save cgm asset ";
    /**
     * action prefix: remainder is a base file path
     */
    final static String saveCgmFilePrefix = "save cgm file ";
    /**
     * action prefix: remainder is an asset path
     */
    final static String saveMappingAssetPrefix = "save mapping asset ";
    /**
     * action prefix: remainder is the name of a bone or a prefix thereof
     */
    final static String selectBonePrefix = "select bone ";
    final static String selectBoneChildPrefix = "select boneChild ";
    final static String selectControlPrefix = "select control ";
    /**
     * action prefix: remainder is the name of a geometry or a prefix thereof
     */
    final static String selectGeometryPrefix = "select geometry ";
    /**
     * action prefix: remainder is the menu path of a menu item
     */
    final static String selectMenuItemPrefix = "select menuItem ";
    /**
     * action prefix: remainder is the name of a source bone or a prefix thereof
     */
    final static String selectSourceBonePrefix = "select sourceBone ";
    final static String selectSpatialChildPrefix = "select spatialChild ";
    /**
     * action prefix: remainder is the name of a spatial or a prefix thereof
     */
    final static String selectSpatialPrefix = "select spatial ";
    /**
     * action prefix: remainder is the name of a tool window
     */
    final static String selectToolPrefix = "select tool ";
    /**
     * action prefix: remainder is a pre-existing user key
     */
    final static String selectUserKeyPrefix = "select userKey ";
    final static String setDurationPrefix = "set duration ";
    /**
     * local copy of {@link com.jme3.math.Vector3f#UNIT_XYZ}
     */
    final private static Vector3f scaleIdentity = new Vector3f(1f, 1f, 1f);
    /**
     * local copy of {@link com.jme3.math.Vector3f#ZERO}
     */
    final private static Vector3f translateIdentity = new Vector3f(0f, 0f, 0f);
    // *************************************************************************
    // constructors

    /**
     * Instantiate a disabled, uninitialized mode.
     */
    DddInputMode() {
        super("3D View");
    }
    // *************************************************************************
    // ActionListener methods

    /**
     * Process an action from the GUI or keyboard.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param tpf time interval between render passes (in seconds, &ge;0)
     */
    @Override
    public void onAction(String actionString, boolean ongoing, float tpf) {
        logger.log(Level.INFO, "Got action {0} ongoing={1}", new Object[]{
            MyString.quote(actionString), ongoing
        });
        /*
         * Parse the action string and attempt to handle the action.
         */
        boolean handled = false;
        if (ongoing) {
            String firstWord = actionString.split(" ")[0];
            switch (firstWord) {
                case "copy":
                    handled = copyAction(actionString);
                    break;
                case "delete":
                    handled = deleteAction(actionString);
                    break;
                case "load":
                    handled = loadAction(actionString);
                    break;
                case "new":
                    handled = newAction(actionString);
                    break;
                case "next":
                    handled = nextAction(actionString);
                    break;
                case "previous":
                    handled = previousAction(actionString);
                    break;
                case "reduce":
                    handled = reduceAction(actionString);
                    break;
                case "rename":
                    handled = renameAction(actionString);
                    break;
                case "reset":
                    handled = resetAction(actionString);
                    break;
                case "retarget":
                    handled = retargetAction(actionString);
                    break;
                case "save":
                    handled = saveAction(actionString);
                    break;
                case "select":
                    handled = selectAction(actionString);
                    break;
                case "set":
                    handled = setAction(actionString);
                    break;
                case "toggle":
                    handled = toggleAction(actionString);
                    break;
                case "view":
                    handled = viewAction(actionString);
                    break;
                case "warp":
                    handled = warpAction(actionString);
            }

        } else if ("select boneXY".equals(actionString)) {
            Maud.model.axes.clearDragAxis();
            handled = true;
        }

        if (!handled) {
            /*
             * Forward the unhandled action to the application.
             */
            actionApplication.onAction(actionString, ongoing, tpf);
        }
    }
    // *************************************************************************
    // InputMode methods

    /**
     * Hotkey bindings used if the configuration asset is missing.
     */
    @Override
    protected void defaultBindings() {
        // intentionally empty
    }

    /**
     * Initialize this (disabled) mode prior to its 1st update.
     *
     * @param stateManager (not null)
     * @param application (not null)
     */
    @Override
    public void initialize(AppStateManager stateManager,
            Application application) {
        AssetManager am = application.getAssetManager();
        JmeCursor cursor = (JmeCursor) am.loadAsset(assetPath);
        setCursor(cursor);

        super.initialize(stateManager, application);
    }
    // *************************************************************************
    // private methods

    /**
     * Process an action that starts with "copy ".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean copyAction(String actionString) {
        boolean handled = false;
        if (actionString.startsWith(copyAnimationPrefix)) {
            String destName = MyString.remainder(actionString,
                    copyAnimationPrefix);
            Maud.model.target.animation.copyAndLoad(destName);
            handled = true;
        }

        return handled;
    }

    /**
     * Process an action that starts with "delete ".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean deleteAction(String actionString) {
        boolean handled = false;
        switch (actionString) {
            case "delete animation":
                Maud.model.target.animation.delete();
                handled = true;
                break;
            case "delete control":
                Maud.model.target.sgc.delete();
                handled = true;
                break;
            case "delete mapping":
                Maud.model.mapping.deleteBone();
                handled = true;
                break;
            case "delete userKey":
                Maud.model.misc.deleteUserKey();
                handled = true;
        }

        return handled;
    }

    /**
     * Process an action that starts with "load ".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean loadAction(String actionString) {
        boolean handled = false;
        if (actionString.equals("load mapping asset")) {
            Maud.gui.dialogs.loadMappingAsset();
            handled = true;

        } else if (actionString.equals("load retargetedPose")) {
            Maud.model.target.animation.loadRetargetedPose();
            handled = true;

        } else if (actionString.startsWith(loadAnimationPrefix)) {
            String name = MyString.remainder(actionString, loadAnimationPrefix);
            Maud.model.target.animation.load(name);
            handled = true;

        } else if (actionString.startsWith(loadCgmAssetPrefix)) {
            String path;
            path = MyString.remainder(actionString, loadCgmAssetPrefix);
            Maud.model.target.loadModelAsset(path);
            handled = true;

        } else if (actionString.startsWith(loadCgmFilePrefix)) {
            String path = MyString.remainder(actionString, loadCgmFilePrefix);
            Maud.gui.menus.loadModelFile(path);
            handled = true;

        } else if (actionString.startsWith(loadCgmNamedPrefix)) {
            String name;
            name = MyString.remainder(actionString, loadCgmNamedPrefix);
            Maud.model.target.loadModelNamed(name);
            handled = true;

        } else if (actionString.startsWith(loadMappingAssetPrefix)) {
            String path;
            path = MyString.remainder(actionString, loadMappingAssetPrefix);
            Maud.model.mapping.loadMappingAsset(path);
            handled = true;

        } else if (actionString.startsWith(loadSourceAnimationPrefix)) {
            String name;
            name = MyString.remainder(actionString, loadSourceAnimationPrefix);
            Maud.model.source.animation.load(name);
            handled = true;

        } else if (actionString.startsWith(loadSourceCgmAssetPrefix)) {
            String path;
            path = MyString.remainder(actionString, loadSourceCgmAssetPrefix);
            Maud.model.source.loadModelAsset(path);
            handled = true;

        } else if (actionString.startsWith(loadSourceCgmFilePrefix)) {
            String path;
            path = MyString.remainder(actionString, loadSourceCgmFilePrefix);
            Maud.gui.menus.loadSourceModelFile(path);
            handled = true;

        } else if (actionString.startsWith(loadSourceCgmNamedPrefix)) {
            String name;
            name = MyString.remainder(actionString, loadSourceCgmNamedPrefix);
            Maud.model.source.loadModelNamed(name);
            handled = true;
        }

        return handled;
    }

    /**
     * Process an action that starts with "new ".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean newAction(String actionString) {
        boolean handled = false;
        if (actionString.equals("new checkpoint")) {
            Maud.gui.addCheckpoint("user interface");
            handled = true;
        } else if (actionString.equals("new mapping")) {
            Maud.model.mapping.mapBones();
            handled = true;
        } else if (actionString.equals("new pose")) {
            Maud.gui.dialogs.newPose();
            handled = true;
        } else if (actionString.equals("new userKey")) {
            Maud.gui.menus.selectUserDataType();
            handled = true;

        } else if (actionString.startsWith(newPosePrefix)) {
            String name = MyString.remainder(actionString, newPosePrefix);
            Maud.model.target.animation.poseAndLoad(name);
            handled = true;

        } else if (actionString.startsWith(newUserKeyPrefix)) {
            String args = MyString.remainder(actionString, newUserKeyPrefix);
            if (args.contains(" ")) {
                String type = args.split(" ")[0];
                String key = MyString.remainder(args, type + " ");
                Maud.model.target.addUserKey(type, key);
            } else {
                Maud.gui.dialogs.newUserKey(actionString + " ");
            }
            handled = true;
        }

        return handled;
    }

    /**
     * Process an action that starts with "next".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean nextAction(String actionString) {
        boolean handled = false;
        switch (actionString) {
            case "next animation":
                Maud.model.target.animation.loadNext();
                handled = true;
                break;
            case "next bone":
                Maud.model.target.bone.selectNext();
                handled = true;
                break;
            case "next checkpoint":
                History.redo();
                handled = true;
                break;
            case "next control":
                Maud.model.target.sgc.selectNext();
                handled = true;
                break;
            case "next mapping":
                Maud.model.mapping.selectNext();
                handled = true;
                break;
            case "next sourceAnimation":
                Maud.model.source.animation.loadNext();
                handled = true;
                break;
            case "next userData":
                Maud.model.misc.selectNextUserKey();
                handled = true;
        }

        return handled;
    }

    /**
     * Process an action that starts with "previous".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean previousAction(String actionString) {
        boolean handled = false;
        switch (actionString) {
            case "previous animation":
                Maud.model.target.animation.loadPrevious();
                handled = true;
                break;
            case "previous bone":
                Maud.model.target.bone.selectPrevious();
                handled = true;
                break;
            case "previous checkpoint":
                History.undo();
                handled = true;
                break;
            case "previous control":
                Maud.model.target.sgc.selectPrevious();
                handled = true;
                break;
            case "previous mapping":
                Maud.model.mapping.selectPrevious();
                handled = true;
                break;
            case "previous sourceAnimation":
                Maud.model.source.animation.loadPrevious();
                handled = true;
                break;
            case "previous userData":
                Maud.model.misc.selectPreviousUserKey();
                handled = true;
        }

        return handled;
    }

    /**
     * Process an action that starts with "reduce".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean reduceAction(String actionString) {
        boolean handled = false;
        if (actionString.equals("reduce animation")) {
            Maud.gui.dialogs.reduceAnimation();
            handled = true;

        } else if (actionString.equals("reduce track")) {
            Maud.gui.dialogs.reduceTrack();
            handled = true;

        } else if (actionString.startsWith(reduceAnimationPrefix)) {
            String f = MyString.remainder(actionString, reduceAnimationPrefix);
            int factor = Integer.parseInt(f);
            Maud.model.target.animation.reduce(factor);
            handled = true;

        } else if (actionString.startsWith(reduceTrackPrefix)) {
            String f = MyString.remainder(actionString, reduceTrackPrefix);
            int factor = Integer.parseInt(f);
            Maud.model.target.track.reduceTrack(factor);
            handled = true;
        }

        return handled;
    }

    /**
     * Process an action that starts with "rename".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean renameAction(String actionString) {
        boolean handled = false;
        switch (actionString) {
            case "rename animation":
                Maud.gui.dialogs.renameAnimation();
                handled = true;
                break;
            case "rename bone":
                Maud.gui.dialogs.renameBone();
                handled = true;
                break;
            case "rename spatial":
                Maud.gui.dialogs.renameSpatial();
                handled = true;
                break;
            case "rename userKey":
                Maud.gui.dialogs.renameUserKey();
                handled = true;
        }

        if (!handled) {
            String newName;
            if (actionString.startsWith(renameAnimationPrefix)) {
                newName = MyString.remainder(actionString,
                        renameAnimationPrefix);
                Maud.model.target.animation.rename(newName);
                handled = true;

            } else if (actionString.startsWith(renameBonePrefix)) {
                newName = MyString.remainder(actionString, renameBonePrefix);
                Maud.model.target.renameBone(newName);
                handled = true;

            } else if (actionString.startsWith(renameSpatialPrefix)) {
                newName = MyString.remainder(actionString, renameSpatialPrefix);
                Maud.model.target.renameSpatial(newName);
                handled = true;

            } else if (actionString.startsWith(renameUserKeyPrefix)) {
                newName = MyString.remainder(actionString, renameUserKeyPrefix);
                Maud.model.target.renameUserKey(newName);
                handled = true;
            }
        }

        return handled;
    }

    /**
     * Process an action that starts with "reset".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean resetAction(String actionString) {
        boolean handled = false;
        switch (actionString) {
            case "reset bone ang anim":
                Maud.model.target.bone.setRotationToAnimation();
                handled = true;
                break;
            case "reset bone ang bind":
                Maud.model.target.bone.resetRotation();
                handled = true;
                break;
            case "reset bone off anim":
                Maud.model.target.bone.setTranslationToAnimation();
                handled = true;
                break;
            case "reset bone off bind":
                Maud.model.target.bone.resetTranslation();
                handled = true;
                break;
            case "reset bone sca anim":
                Maud.model.target.bone.setScaleToAnimation();
                handled = true;
                break;
            case "reset bone sca bind":
                Maud.model.target.bone.resetScale();
                handled = true;
                break;

            case "reset spatial rotation":
                Maud.model.target.setSpatialRotation(identityRotation);
                handled = true;
                break;
            case "reset spatial scale":
                Maud.model.target.setSpatialScale(scaleIdentity);
                handled = true;
                break;
            case "reset spatial translation":
                Maud.model.target.setSpatialTranslation(translateIdentity);
                handled = true;
                break;

            case "reset twist":
                Maud.model.mapping.setTwist(identityRotation);
                handled = true;
        }

        return handled;
    }

    /**
     * Process an action that starts with "retarget".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean retargetAction(String actionString) {
        boolean handled = false;

        if (actionString.startsWith(retargetAnimationPrefix)) {
            String name = MyString.remainder(actionString,
                    retargetAnimationPrefix);
            Maud.model.mapping.retargetAndLoad(name);
            handled = true;

        } else if (actionString.equals("retarget animation")) {
            Maud.gui.dialogs.retargetAnimation();
            handled = true;
        }

        return handled;
    }

    /**
     * Process an action that starts with "save".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean saveAction(String actionString) {
        boolean handled = false;
        if (actionString.startsWith(saveCgmAssetPrefix)) {
            String path;
            path = MyString.remainder(actionString, saveCgmAssetPrefix);
            Maud.model.target.writeToAsset(path);
            handled = true;

        } else if (actionString.startsWith(saveCgmFilePrefix)) {
            String path = MyString.remainder(actionString, saveCgmFilePrefix);
            Maud.model.target.writeToFile(path);
            handled = true;

        } else if (actionString.startsWith(saveMappingAssetPrefix)) {
            String path;
            path = MyString.remainder(actionString, saveMappingAssetPrefix);
            Maud.model.mapping.writeToAsset(path);
            handled = true;
        }

        return handled;
    }

    /**
     * Process an action that starts with "select".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean selectAction(String actionString) {
        boolean handled = true;
        switch (actionString) {
            case "select boneChild":
                Maud.gui.menus.selectBoneChild();
                break;
            case "select boneParent":
                Maud.model.target.bone.selectParent();
                break;
            case "select boneXY":
                Maud.gui.tools.bone.selectXY();
                break;
            case "select keyframeFirst":
                Maud.model.target.animation.selectKeyframeFirst();
                break;
            case "select keyframeLast":
                Maud.model.target.animation.selectKeyframeLast();
                break;
            case "select keyframeNext":
                Maud.model.target.animation.selectKeyframeNext();
                break;
            case "select keyframePrevious":
                Maud.model.target.animation.selectKeyframePrevious();
                break;
            case "select mapSourceBone":
                Maud.model.mapping.selectFromSource();
                break;
            case "select mapTargetBone":
                Maud.model.mapping.selectFromTarget();
                break;
            case "select spatialChild":
                Maud.gui.menus.selectSpatialChild();
                break;
            case "select spatialParent":
                Maud.model.target.spatial.selectParent();
                break;
            case "select userKey":
                Maud.gui.menus.selectUserKey();
                break;
            default:
                handled = selectAction2(actionString);
        }

        return handled;
    }

    /**
     * Process an action that starts with "select" -- 2nd part: test prefixes.
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean selectAction2(String actionString) {
        boolean handled = true;
        String arg;
        if (actionString.startsWith(selectBonePrefix)) {
            arg = MyString.remainder(actionString, selectBonePrefix);
            Maud.gui.menus.selectBone(arg);

        } else if (actionString.startsWith(selectBoneChildPrefix)) {
            arg = MyString.remainder(actionString, selectBoneChildPrefix);
            Maud.gui.menus.selectBoneChild(arg);

        } else if (actionString.startsWith(selectControlPrefix)) {
            arg = MyString.remainder(actionString, selectControlPrefix);
            Maud.model.target.sgc.select(arg);

        } else if (actionString.startsWith(selectGeometryPrefix)) {
            arg = MyString.remainder(actionString, selectGeometryPrefix);
            Maud.gui.menus.selectSpatial(arg, false);

        } else if (actionString.startsWith(selectSourceBonePrefix)) {
            arg = MyString.remainder(actionString, selectSourceBonePrefix);
            Maud.gui.menus.selectSourceBone(arg);

        } else if (actionString.startsWith(selectSpatialChildPrefix)) {
            arg = MyString.remainder(actionString, selectSpatialChildPrefix);
            Maud.gui.selectSpatialChild(arg);

        } else if (actionString.startsWith(selectSpatialPrefix)) {
            arg = MyString.remainder(actionString, selectSpatialPrefix);
            Maud.gui.menus.selectSpatial(arg, true);

        } else if (actionString.startsWith(selectUserKeyPrefix)) {
            arg = MyString.remainder(actionString, selectUserKeyPrefix);
            Maud.model.misc.selectUserKey(arg);

        } else {
            handled = false;
        }

        if (!handled && actionString.startsWith(selectMenuItemPrefix)) {
            String menuPath;
            menuPath = MyString.remainder(actionString, selectMenuItemPrefix);
            handled = Maud.gui.menus.selectMenuItem(menuPath);
        }
        if (!handled && actionString.startsWith(selectToolPrefix)) {
            String toolName;
            toolName = MyString.remainder(actionString, selectToolPrefix);
            handled = Maud.gui.selectTool(toolName);
        }

        return handled;
    }

    /**
     * Process an action that starts with "set".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean setAction(String actionString) {
        boolean handled = false;
        switch (actionString) {
            case "set track rotation all":
                Maud.model.target.track.setTrackRotationAll();
                handled = true;
                break;
            case "set track scale all":
                Maud.model.target.track.setTrackScaleAll();
                handled = true;
                break;
            case "set track translation all":
                Maud.model.target.track.setTrackTranslationAll();
                handled = true;
                break;
            case "set userdata":
                Maud.gui.dialogs.setUserData();
                handled = true;
        }

        if (!handled) {
            String arg;
            if (actionString.startsWith(setDurationPrefix)) {
                arg = MyString.remainder(actionString, setDurationPrefix);
                float value = Float.parseFloat(arg);
                Maud.model.target.animation.setDuration(value);
                handled = true;
            }
        }

        return handled;
    }

    /**
     * Process an action that starts with "toggle".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean toggleAction(String actionString) {
        boolean handled = false;
        switch (actionString) {
            case "toggle degrees":
                Maud.model.misc.toggleAnglesInDegrees();
                handled = true;
                break;
            case "toggle dragSide":
                Maud.model.axes.toggleDragSide();
                handled = true;
                break;
            case "toggle pause":
                Maud.model.source.animation.togglePaused();
                Maud.model.target.animation.togglePaused();
                handled = true;
                break;
            case "toggle pause source":
                Maud.model.source.animation.togglePaused();
                handled = true;
                break;
            case "toggle pause target":
                Maud.model.target.animation.togglePaused();
                handled = true;
                break;
            case "toggle projection":
                Maud.model.camera.toggleProjection();
                handled = true;
                break;
        }

        return handled;
    }

    /**
     * Process an action that starts with "view".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean viewAction(String actionString) {
        boolean handled = false;
        switch (actionString) {
            case "view horizontal":
                Maud.model.camera.goHorizontal();
                handled = true;
                break;
        }

        return handled;
    }

    /**
     * Process an action that starts with "warp".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean warpAction(String actionString) {
        boolean handled = false;
        switch (actionString) {
            case "warp cursor":
                Maud.gui.tools.cursor.warpCursor();
                handled = true;
        }

        return handled;
    }
}
