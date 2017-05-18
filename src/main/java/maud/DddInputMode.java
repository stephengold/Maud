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
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.ui.InputMode;

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
     * asset path to the cursor for this mode
     */
    final private static String assetPath = "Textures/cursors/default.cur";
    /*
     * action-string prefixes used by dialogs and popup menus
     */
    final static String copyAnimationPrefix = "copy animation ";
    final static String loadAnimationPrefix = "load animation ";
    final static String loadModelAssetPrefix = "load model asset ";
    final static String loadModelFilePrefix = "load model file ";
    final static String loadModelNamedPrefix = "load model named ";
    final static String openMenuPrefix = "open menu ";
    final static String newPosePrefix = "new pose ";
    final static String renameAnimationPrefix = "rename animation ";
    final static String renameBonePrefix = "rename bone ";
    final static String retargetAnimationPrefix = "retarget animation ";
    final static String saveModelAssetPrefix = "save model asset ";
    final static String saveModelFilePrefix = "save model file ";
    final static String selectBonePrefix = "select bone ";
    final static String selectBoneChildPrefix = "select boneChild ";
    final static String selectRetargetMapAssetPrefix = "select rma ";
    final static String selectRetargetSourceAnimationPrefix = "select rsa ";
    final static String selectRetargetSourceCgmAssetPrefix = "select rsca ";
    final static String selectSpatialChildPrefix = "select spatialChild ";
    final static String selectToolPrefix = "select tool ";
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
     * Process a "copy" action.
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean copyAction(String actionString) {
        boolean handled = false;
        if (actionString.startsWith(copyAnimationPrefix)) {
            String destName = MyString.remainder(actionString,
                    copyAnimationPrefix);
            Maud.model.animation.copyAndLoad(destName);
            handled = true;
        }

        return handled;
    }

    /**
     * Process a "load" action.
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean loadAction(String actionString) {
        boolean handled = false;
        if (actionString.startsWith(loadAnimationPrefix)) {
            String name = MyString.remainder(actionString, loadAnimationPrefix);
            Maud.model.animation.load(name);
            handled = true;

        } else if (actionString.startsWith(loadModelAssetPrefix)) {
            String path = MyString.remainder(actionString,
                    loadModelAssetPrefix);
            Maud.model.cgm.loadModelAsset(path);
            handled = true;

        } else if (actionString.startsWith(loadModelFilePrefix)) {
            String path = MyString.remainder(actionString, loadModelFilePrefix);
            Maud.gui.menus.loadModelFile(path);
            handled = true;

        } else if (actionString.startsWith(loadModelNamedPrefix)) {
            String name = MyString.remainder(actionString,
                    loadModelNamedPrefix);
            Maud.model.cgm.loadModelNamed(name);
            handled = true;
        }

        return handled;
    }

    /**
     * Process a "new" action.
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
            Maud.model.animation.poseAndLoad(name);
            handled = true;
        }

        return handled;
    }

    /**
     * Process a "next" action.
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean nextAction(String actionString) {
        boolean handled = false;
        if (actionString.equals("next bone")) {
            Maud.model.bone.selectNext();
            handled = true;

        } else if (actionString.equals("next checkpoint")) {
            History.redo();
            handled = true;
        }

        return handled;
    }

    /**
     * Process an "open" action.
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
     * Process a "previous" action.
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean previousAction(String actionString) {
        boolean handled = false;
        if (actionString.equals("previous bone")) {
            Maud.model.bone.selectPrevious();
            handled = true;

        } else if (actionString.equals("previous checkpoint")) {
            History.undo();
            handled = true;
        }

        return handled;
    }

    /**
     * Process a "rename" action.
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
            Maud.model.cgm.renameAnimation(newName);
            handled = true;

        } else if (actionString.startsWith(renameBonePrefix)) {
            newName = MyString.remainder(actionString, renameBonePrefix);
            Maud.model.cgm.renameBone(newName);
            handled = true;
        }

        return handled;
    }

    /**
     * Process a "reset" action.
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean resetAction(String actionString) {
        boolean handled = false;
        switch (actionString) {
            case "reset bone ang anim":
                Maud.gui.boneRotation.setToAnimation();
                handled = true;
                break;
            case "reset bone ang bind":
                Maud.gui.boneRotation.reset();
                handled = true;
                break;
            case "reset bone off anim":
                Maud.gui.boneTranslation.setToAnimation();
                handled = true;
                break;
            case "reset bone off bind":
                Maud.gui.boneTranslation.reset();
                handled = true;
                break;
            case "reset bone sca anim":
                Maud.gui.boneScale.setToAnimation();
                handled = true;
                break;
            case "reset bone sca bind":
                Maud.gui.boneScale.reset();
                handled = true;
                break;

            case "reset spatial rotation":
                Maud.gui.spatialRotation.reset();
                handled = true;
                break;
            case "reset spatial scale":
                Maud.gui.spatialScale.reset();
                handled = true;
                break;
            case "reset spatial translation":
                Maud.gui.spatialTranslation.reset();
                handled = true;
                break;
        }

        return handled;
    }

    /**
     * Process a "retarget" action.
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
     * Process a "save" action.
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean saveAction(String actionString) {
        boolean handled = false;
        if (actionString.startsWith(saveModelAssetPrefix)) {
            String path = MyString.remainder(actionString,
                    saveModelAssetPrefix);
            Maud.model.cgm.writeModelToAsset(path);
            handled = true;

        } else if (actionString.startsWith(saveModelFilePrefix)) {
            String path = MyString.remainder(actionString, saveModelFilePrefix);
            Maud.model.cgm.writeModelToFile(path);
            handled = true;
        }

        return handled;
    }

    /**
     * Process a "select" action.
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
                Maud.model.bone.selectParent();
                handled = true;
                break;
            case "select boneXY":
                Maud.gui.bone.selectXY();
                handled = true;
                break;
            case "select keyframeFirst":
                Maud.model.animation.selectKeyframeFirst();
                handled = true;
                break;
            case "select keyframeLast":
                Maud.model.animation.selectKeyframeLast();
                handled = true;
                break;
            case "select keyframeNext":
                Maud.model.animation.selectKeyframeNext();
                handled = true;
                break;
            case "select keyframePrevious":
                Maud.model.animation.selectKeyframePrevious();
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
                Maud.model.spatial.selectParent();
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
     * Process a "set" action.
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean setAction(String actionString) {
        boolean handled = false;
        switch (actionString) {
            case "set track rotation all":
                Maud.model.bone.setTrackRotationAll();
                handled = true;
                break;
            case "set track scale all":
                Maud.model.bone.setTrackScaleAll();
                handled = true;
                break;
            case "set track translation all":
                Maud.model.bone.setTrackTranslationAll();
                handled = true;
                break;
        }

        return handled;
    }

    /**
     * Process a "toggle" action.
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
                Maud.model.animation.togglePaused();
                handled = true;
        }

        return handled;
    }

    /**
     * Process a "view" action.
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
     * Process a "warp" action.
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean warpAction(String actionString) {
        boolean handled = false;
        switch (actionString) {
            case "warp cursor":
                Maud.gui.cursor.warpCursor();
                handled = true;
                break;
        }

        return handled;
    }
}
