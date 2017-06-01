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
import maud.model.LoadedCGModel;

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
     * action prefix: remainder is the asset path to a CG model
     */
    final static String loadModelAssetPrefix = "load model asset ";
    final static String loadModelFilePrefix = "load model file ";
    /**
     * action prefix: remainder is the name of a CG model in jme3-testdata
     */
    final static String loadModelNamedPrefix = "load model named ";
    /**
     * action prefix: remainder is the name of a source animation
     */
    final static String loadSourceAnimationPrefix = "load sourceAnimation ";
    /**
     * action prefix: remainder is the asset path to a CG model
     */
    final static String loadSourceModelAssetPrefix = "load sourceModel asset ";
    final static String loadSourceModelFilePrefix = "load sourceModel file ";
    /**
     * action prefix: remainder is the name of a CG model in jme3-testdata
     */
    final static String loadSourceModelNamedPrefix = "load sourceModel named ";
    /**
     * action prefix: remainder is a menu path TODO "select menu "
     */
    final static String openMenuPrefix = "open menu ";
    /**
     * action prefix: remainder is a name for the new animation
     */
    final static String newPosePrefix = "new pose ";
    final static String reduceAnimationPrefix = "reduce animation ";
    final static String reduceTrackPrefix = "reduce track ";
    final static String renameAnimationPrefix = "rename animation ";
    final static String renameBonePrefix = "rename bone ";
    final static String retargetAnimationPrefix = "retarget animation ";
    final static String saveModelAssetPrefix = "save model asset ";
    final static String saveModelFilePrefix = "save model file ";
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
    final static String selectRetargetMapAssetPrefix = "select rma ";
    final static String selectRetargetSourceAnimationPrefix = "select rsa ";
    final static String selectRetargetSourceCgmAssetPrefix = "select rsca ";
    final static String selectSpatialChildPrefix = "select spatialChild ";
    /**
     * action prefix: remainder is the name of a spatial or a prefix thereof
     */
    final static String selectSpatialPrefix = "select spatial ";
    final static String selectToolPrefix = "select tool ";
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
                case "open":
                    handled = openAction(actionString);
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
        if (actionString.equals("delete animation")) {
            Maud.model.target.animation.delete();
            handled = true;
        } else if (actionString.equals("delete control")) {
            Maud.model.target.sgc.delete();
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
        if (actionString.startsWith(loadAnimationPrefix)) {
            String name = MyString.remainder(actionString, loadAnimationPrefix);
            Maud.model.target.animation.load(name);
            handled = true;

        } else if (actionString.startsWith(loadModelAssetPrefix)) {
            String path;
            path = MyString.remainder(actionString, loadModelAssetPrefix);
            Maud.model.target.loadModelAsset(path);
            handled = true;

        } else if (actionString.startsWith(loadModelFilePrefix)) {
            String path = MyString.remainder(actionString, loadModelFilePrefix);
            Maud.gui.menus.loadModelFile(path);
            handled = true;

        } else if (actionString.startsWith(loadModelNamedPrefix)) {
            String name;
            name = MyString.remainder(actionString, loadModelNamedPrefix);
            Maud.model.target.loadModelNamed(name);
            handled = true;

        } else if (actionString.startsWith(loadSourceAnimationPrefix)) {
            String name;
            name = MyString.remainder(actionString, loadSourceAnimationPrefix);
            Maud.model.source.animation.load(name);
            handled = true;

        } else if (actionString.startsWith(loadSourceModelAssetPrefix)) {
            String path;
            path = MyString.remainder(actionString, loadSourceModelAssetPrefix);
            Maud.model.source.loadModelAsset(path);
            handled = true;

        } else if (actionString.startsWith(loadSourceModelFilePrefix)) {
            String path;
            path = MyString.remainder(actionString, loadSourceModelFilePrefix);
            Maud.gui.menus.loadSourceModelFile(path);
            handled = true;

        } else if (actionString.startsWith(loadSourceModelNamedPrefix)) {
            String name;
            name = MyString.remainder(actionString, loadSourceModelNamedPrefix);
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
        } else if (actionString.equals("new pose")) {
            Maud.gui.dialogs.newPose();
            handled = true;
        } else if (actionString.startsWith(newPosePrefix)) {
            String name = MyString.remainder(actionString, newPosePrefix);
            Maud.model.target.animation.poseAndLoad(name);
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
            case "next sourceAnimation":
                Maud.model.source.animation.loadNext();
                handled = true;
        }

        return handled;
    }

    /**
     * Process an action that starts with "open".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean openAction(String actionString) {
        boolean handled = false;
        if (actionString.startsWith(openMenuPrefix)) {
            String menuPath = MyString.remainder(actionString, openMenuPrefix);
            handled = Maud.gui.menus.openMenu(menuPath);
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
            case "previous sourceAnimation":
                Maud.model.source.animation.loadPrevious();
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
        String newName;
        if (actionString.equals("rename animation")) {
            Maud.gui.dialogs.renameAnimation();
            handled = true;

        } else if (actionString.equals("rename bone")) {
            Maud.gui.dialogs.renameBone();
            handled = true;

        } else if (actionString.startsWith(renameAnimationPrefix)) {
            newName = MyString.remainder(actionString, renameAnimationPrefix);
            Maud.model.target.animation.rename(newName);
            handled = true;

        } else if (actionString.startsWith(renameBonePrefix)) {
            newName = MyString.remainder(actionString, renameBonePrefix);
            Maud.model.target.renameBone(newName);
            handled = true;
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
                ;
                handled = true;
                break;
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
            Maud.model.retarget.retargetAndLoad(name);
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
        if (actionString.startsWith(saveModelAssetPrefix)) {
            String path = MyString.remainder(actionString,
                    saveModelAssetPrefix);
            Maud.model.target.writeModelToAsset(path);
            handled = true;

        } else if (actionString.startsWith(saveModelFilePrefix)) {
            String path = MyString.remainder(actionString, saveModelFilePrefix);
            Maud.model.target.writeModelToFile(path);
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
        boolean handled = false;
        switch (actionString) {
            case "select boneChild":
                Maud.gui.menus.selectBoneChild();
                handled = true;
                break;
            case "select boneParent":
                Maud.model.target.bone.selectParent();
                handled = true;
                break;
            case "select boneXY":
                Maud.gui.tools.bone.selectXY();
                handled = true;
                break;
            case "select keyframeFirst":
                Maud.model.target.animation.selectKeyframeFirst();
                handled = true;
                break;
            case "select keyframeLast":
                Maud.model.target.animation.selectKeyframeLast();
                handled = true;
                break;
            case "select keyframeNext":
                Maud.model.target.animation.selectKeyframeNext();
                handled = true;
                break;
            case "select keyframePrevious":
                Maud.model.target.animation.selectKeyframePrevious();
                handled = true;
                break;
            case "select rma":
                Maud.gui.dialogs.selectRetargetMapAsset();
                handled = true;
                break;
            case "select rsa":
                Maud.gui.menus.selectRetargetSourceAnimation();
                handled = true;
                break;
            case "select rsca":
                Maud.gui.dialogs.selectRetargetSourceCgmAsset();
                handled = true;
                break;
            case "select spatialChild":
                Maud.gui.menus.selectSpatialChild();
                handled = true;
                break;
            case "select spatialParent":
                Maud.model.target.spatial.selectParent();
                handled = true;
        }

        if (!handled) {
            String arg;
            if (actionString.startsWith(selectBonePrefix)) {
                arg = MyString.remainder(actionString, selectBonePrefix);
                Maud.gui.menus.selectBone(arg);
                handled = true;

            } else if (actionString.startsWith(selectBoneChildPrefix)) {
                arg = MyString.remainder(actionString, selectBoneChildPrefix);
                Maud.gui.menus.selectBoneChild(arg);
                handled = true;

            } else if (actionString.startsWith(selectControlPrefix)) {
                arg = MyString.remainder(actionString, selectControlPrefix);
                Maud.model.target.sgc.select(arg);
                handled = true;

            } else if (actionString.startsWith(selectGeometryPrefix)) {
                arg = MyString.remainder(actionString, selectGeometryPrefix);
                Maud.gui.menus.selectSpatial(arg, false);
                handled = true;

            } else if (actionString.startsWith(
                    selectRetargetMapAssetPrefix)) {
                arg = MyString.remainder(actionString,
                        selectRetargetMapAssetPrefix);
                Maud.model.retarget.setMappingAssetPath(arg);
                handled = true;

            } else if (actionString.startsWith(
                    selectRetargetSourceAnimationPrefix)) {
                arg = MyString.remainder(actionString,
                        selectRetargetSourceAnimationPrefix);
                Maud.model.retarget.setSourceAnimationName(arg);
                handled = true;

            } else if (actionString.startsWith(
                    selectRetargetSourceCgmAssetPrefix)) {
                arg = MyString.remainder(actionString,
                        selectRetargetSourceCgmAssetPrefix);
                Maud.model.retarget.setSourceCgmAssetPath(arg);
                handled = true;

            } else if (actionString.startsWith(selectSpatialChildPrefix)) {
                arg = MyString.remainder(actionString,
                        selectSpatialChildPrefix);
                Maud.gui.selectSpatialChild(arg);
                handled = true;

            } else if (actionString.startsWith(selectSpatialPrefix)) {
                arg = MyString.remainder(actionString, selectSpatialPrefix);
                Maud.gui.menus.selectSpatial(arg, true);
                handled = true;
            }
        }

        if (!handled && actionString.startsWith(selectToolPrefix)) {
            String toolName = MyString.remainder(actionString,
                    selectToolPrefix);
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
            case "toggle pause":
                LoadedCGModel cgmToToggle = Maud.gui.selectCgm();
                cgmToToggle.animation.togglePaused();
                handled = true;
                break;
            case "toggle pause source":
                Maud.model.source.animation.togglePaused();
                handled = true;
                break;
            case "toggle pause target":
                Maud.model.target.animation.togglePaused();
                handled = true;
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
