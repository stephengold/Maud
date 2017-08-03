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
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Spatial;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.ui.InputMode;
import maud.model.History;
import maud.model.LoadedCgm;

/**
 * Input mode for Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class EditorInputMode extends InputMode {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            EditorInputMode.class.getName());
    /**
     * local copy of {@link com.jme3.math.Quaternion#IDENTITY}
     */
    final private static Quaternion rotationIdentity = new Quaternion();
    /**
     * asset path to the cursor for this input mode
     */
    final private static String assetPath = "Textures/cursors/default.cur";
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
    EditorInputMode() {
        super("editor");
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
                    break;
                case "wrap":
                    handled = wrapAction(actionString);
            }

        } else if ("select screenXY".equals(actionString)) {
            Maud.model.axes.clearDragAxis();
            Maud.model.score.setDraggingGnomon(null);
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
     * Activate this input mode.
     */
    @Override
    public void activate() {
        super.activate();
        Maud.gui.tools.camera.mapButton();
    }

    /**
     * Deactivate this input mode.
     */
    @Override
    public void deactivate() {
        Maud.gui.tools.camera.unmapButton();
        super.deactivate();
    }

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
        if (actionString.startsWith(ActionPrefix.copyAnimation)) {
            String destName = MyString.remainder(actionString,
                    ActionPrefix.copyAnimation);
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
        boolean handled = true;
        switch (actionString) {
            case "delete animation":
                Maud.model.target.animation.delete();
                break;
            case "delete control":
                Maud.model.target.sgc.delete();
                break;
            case "delete mapping":
                Maud.model.map.deleteBone();
                break;
            case "delete singleKeyframe":
                Maud.model.target.track.deleteSingleKeyframe();
                break;
            case "delete userKey":
                Maud.model.misc.deleteUserKey();
                break;
            default:
                handled = false;
                if (actionString.startsWith(ActionPrefix.deleteAssetFolder)) {
                    String arg = MyString.remainder(actionString,
                            ActionPrefix.deleteAssetFolder);
                    Maud.model.folders.remove(arg);
                    handled = true;
                }
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
        boolean handled = true;
        String args, name, path;
        if (actionString.equals("load map asset")) {
            Maud.gui.menus.loadMapAsset();

        } else if (actionString.equals("load retargetedPose")) {
            Maud.model.target.animation.loadRetargetedPose();

        } else if (actionString.startsWith(ActionPrefix.loadAnimation)) {
            args = MyString.remainder(actionString, ActionPrefix.loadAnimation);
            Maud.gui.menus.loadAnimation(args, Maud.model.target);

        } else if (actionString.startsWith(ActionPrefix.loadCgmAsset)) {
            args = MyString.remainder(actionString, ActionPrefix.loadCgmAsset);
            Maud.gui.menus.loadCgmAsset(args, Maud.model.target);

        } else if (actionString.startsWith(ActionPrefix.loadCgmLocator)) {
            path = MyString.remainder(actionString,
                    ActionPrefix.loadCgmLocator);
            Maud.gui.menus.loadCgmLocator(path, Maud.model.target);

        } else if (actionString.startsWith(ActionPrefix.loadCgmNamed)) {
            name = MyString.remainder(actionString, ActionPrefix.loadCgmNamed);
            Maud.model.target.loadNamed(name);

        } else if (actionString.startsWith(ActionPrefix.loadMapAsset)) {
            path = MyString.remainder(actionString,
                    ActionPrefix.loadMapAsset);
            Maud.gui.menus.loadMapAsset(path);

        } else if (actionString.startsWith(ActionPrefix.loadMapLocator)) {
            path = MyString.remainder(actionString,
                    ActionPrefix.loadMapLocator);
            Maud.gui.menus.loadMapLocator(path);

        } else if (actionString.startsWith(ActionPrefix.loadMapNamed)) {
            name = MyString.remainder(actionString,
                    ActionPrefix.loadMapNamed);
            Maud.model.map.loadNamed(name);

        } else if (actionString.startsWith(ActionPrefix.loadSourceAnimation)) {
            args = MyString.remainder(actionString,
                    ActionPrefix.loadSourceAnimation);
            Maud.gui.menus.loadAnimation(args, Maud.model.source);

        } else if (actionString.startsWith(ActionPrefix.loadSourceCgmAsset)) {
            args = MyString.remainder(actionString,
                    ActionPrefix.loadSourceCgmAsset);
            Maud.gui.menus.loadCgmAsset(args, Maud.model.source);

        } else if (actionString.startsWith(ActionPrefix.loadSourceCgmLocator)) {
            path = MyString.remainder(actionString,
                    ActionPrefix.loadSourceCgmLocator);
            Maud.gui.menus.loadCgmLocator(path, Maud.model.source);

        } else if (actionString.startsWith(ActionPrefix.loadSourceCgmNamed)) {
            name = MyString.remainder(actionString,
                    ActionPrefix.loadSourceCgmNamed);
            Maud.model.source.loadNamed(name);

        } else {
            handled = false;
        }

        return handled;
    }

    /**
     * Process an action that starts with "new".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean newAction(String actionString) {
        boolean handled = true;
        switch (actionString) {
            case "new animation fromPose":
                Maud.gui.dialogs.newAnimationFromPose();
                break;
            case "new checkpoint":
                Maud.gui.addCheckpoint("user interface");
                break;
            case "new mapping":
                Maud.model.map.mapBones();
                break;
            case "new singleKeyframe":
                Maud.model.target.track.insertSingleKeyframe();
                break;
            case "new userKey":
                Maud.gui.menus.selectUserDataType();
                break;
            default:
                handled = newAction2(actionString);
        }

        return handled;
    }

    /**
     * Process an action that starts with "new" -- 2nd part: test prefixes.
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean newAction2(String actionString) {
        boolean handled = false;
        if (actionString.startsWith(ActionPrefix.newAssetFolder)) {
            String path = MyString.remainder(actionString,
                    ActionPrefix.newAssetFolder);
            Maud.gui.menus.newAssetFolder(path);
            handled = true;

        } else if (actionString.startsWith(ActionPrefix.newAnimationFromPose)) {
            String name = MyString.remainder(actionString,
                    ActionPrefix.newAnimationFromPose);
            Maud.model.target.animation.poseAndLoad(name);
            handled = true;

        } else if (actionString.startsWith(ActionPrefix.newUserKey)) {
            String args;
            args = MyString.remainder(actionString, ActionPrefix.newUserKey);
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
                Maud.model.map.selectNext();
                handled = true;
                break;
            case "next sourceAnimation":
                Maud.model.source.animation.loadNext();
                handled = true;
                break;
            case "next userData":
                Maud.model.misc.selectNextUserKey();
                handled = true;
                break;
            case "next viewMode":
                Maud.model.misc.selectNextViewMode();
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
                Maud.model.map.selectPrevious();
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

        } else if (actionString.startsWith(ActionPrefix.reduceAnimation)) {
            String f;
            f = MyString.remainder(actionString, ActionPrefix.reduceAnimation);
            int factor = Integer.parseInt(f);
            Maud.model.target.animation.reduce(factor);
            handled = true;

        } else if (actionString.startsWith(ActionPrefix.reduceTrack)) {
            String f;
            f = MyString.remainder(actionString, ActionPrefix.reduceTrack);
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
            if (actionString.startsWith(ActionPrefix.renameAnimation)) {
                newName = MyString.remainder(actionString,
                        ActionPrefix.renameAnimation);
                Maud.model.target.animation.rename(newName);
                handled = true;

            } else if (actionString.startsWith(ActionPrefix.renameBone)) {
                newName = MyString.remainder(actionString,
                        ActionPrefix.renameBone);
                Maud.model.target.renameBone(newName);
                handled = true;

            } else if (actionString.startsWith(ActionPrefix.renameSpatial)) {
                newName = MyString.remainder(actionString,
                        ActionPrefix.renameSpatial);
                Maud.model.target.renameSpatial(newName);
                handled = true;

            } else if (actionString.startsWith(ActionPrefix.renameUserKey)) {
                newName = MyString.remainder(actionString,
                        ActionPrefix.renameUserKey);
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
            case "reset bone selection":
                LoadedCgm cgm = Maud.gui.mouseCgm();
                cgm.bone.deselect();
                handled = true;
                break;

            case "reset spatial rotation":
                Maud.model.target.setSpatialRotation(rotationIdentity);
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
                Maud.model.map.setTwist(rotationIdentity);
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

        if (actionString.startsWith(ActionPrefix.retargetAnimation)) {
            String name = MyString.remainder(actionString,
                    ActionPrefix.retargetAnimation);
            Maud.model.map.retargetAndLoad(name);
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
        if (actionString.startsWith(ActionPrefix.saveCgm)) {
            String path;
            path = MyString.remainder(actionString, ActionPrefix.saveCgm);
            Maud.model.target.writeToFile(path);
            handled = true;

        } else if (actionString.startsWith(ActionPrefix.saveMap)) {
            String path;
            path = MyString.remainder(actionString, ActionPrefix.saveMap);
            Maud.model.map.writeToFile(path);
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
            case "select keyframeFirst":
                Maud.model.target.track.selectFirstKeyframe();
                break;
            case "select keyframeLast":
                Maud.model.target.track.selectLastKeyframe();
                break;
            case "select keyframeNext":
                Maud.model.target.track.selectNextKeyframe();
                break;
            case "select keyframePrevious":
                Maud.model.target.track.selectPreviousKeyframe();
                break;
            case "select mapSourceBone":
                Maud.model.map.selectFromSource();
                break;
            case "select mapTargetBone":
                Maud.model.map.selectFromTarget();
                break;
            case "select screenXY":
                Maud.gui.selectXY();
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
        if (actionString.startsWith(ActionPrefix.selectBone)) {
            arg = MyString.remainder(actionString, ActionPrefix.selectBone);
            Maud.gui.menus.selectBone(arg);

        } else if (actionString.startsWith(ActionPrefix.selectBoneChild)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.selectBoneChild);
            Maud.gui.menus.selectBoneChild(arg);

        } else if (actionString.startsWith(ActionPrefix.selectControl)) {
            arg = MyString.remainder(actionString, ActionPrefix.selectControl);
            Maud.model.target.sgc.select(arg);

        } else if (actionString.startsWith(ActionPrefix.selectGeometry)) {
            arg = MyString.remainder(actionString, ActionPrefix.selectGeometry);
            Maud.gui.menus.selectSpatial(arg, false);

        } else if (actionString.startsWith(ActionPrefix.selectSourceBone)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.selectSourceBone);
            Maud.gui.menus.selectSourceBone(arg);

        } else if (actionString.startsWith(ActionPrefix.selectSpatialChild)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.selectSpatialChild);
            Maud.gui.selectSpatialChild(arg);

        } else if (actionString.startsWith(ActionPrefix.selectSpatial)) {
            arg = MyString.remainder(actionString, ActionPrefix.selectSpatial);
            Maud.gui.menus.selectSpatial(arg, true);

        } else if (actionString.startsWith(ActionPrefix.selectUserKey)) {
            arg = MyString.remainder(actionString, ActionPrefix.selectUserKey);
            Maud.model.misc.selectUserKey(arg);

        } else {
            handled = false;
        }

        if (!handled && actionString.startsWith(ActionPrefix.selectMenuItem)) {
            String menuPath;
            menuPath = MyString.remainder(actionString,
                    ActionPrefix.selectMenuItem);
            handled = Maud.gui.menus.selectMenuItem(menuPath);
        }
        if (!handled && actionString.startsWith(ActionPrefix.selectTool)) {
            String toolName;
            toolName = MyString.remainder(actionString,
                    ActionPrefix.selectTool);
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
        boolean handled = true;
        switch (actionString) {
            case "set batchHint":
                Maud.gui.menus.setBatchHint();
                break;
            case "set cullHint":
                Maud.gui.menus.setCullHint();
                break;
            case "set queueBucket":
                Maud.gui.menus.setQueueBucket();
                break;
            case "set shadowMode":
                Maud.gui.menus.setShadowMode();
                break;
            case "set track rotation all":
                Maud.model.target.track.setTrackRotationAll();
                break;
            case "set track scale all":
                Maud.model.target.track.setTrackScaleAll();
                break;
            case "set track translation all":
                Maud.model.target.track.setTrackTranslationAll();
                break;
            case "set tweenScales":
                Maud.gui.menus.setTweenScales();
                break;
            case "set tweenTranslations":
                Maud.gui.menus.setTweenTranslations();
                break;
            case "set twist cardinal":
                Maud.model.map.cardinalizeTwist();
                break;
            case "set twist snapX":
                Maud.model.map.snapTwist(0);
                break;
            case "set twist snapY":
                Maud.model.map.snapTwist(1);
                break;
            case "set twist snapZ":
                Maud.model.map.snapTwist(2);
                break;
            case "set userData":
                Maud.gui.dialogs.setUserData();
                break;
            default:
                handled = setAction2(actionString);
        }

        return handled;
    }

    /**
     * Process an action that starts with "set" -- 2nd part: test prefixes.
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean setAction2(String actionString) {
        boolean handled = true;
        String arg;
        if (actionString.startsWith(ActionPrefix.setDuration)) {
            arg = MyString.remainder(actionString, ActionPrefix.setDuration);
            float value = Float.parseFloat(arg);
            Maud.model.target.animation.setDuration(value);
        } else if (actionString.startsWith(ActionPrefix.setBatchHint)) {
            arg = MyString.remainder(actionString, ActionPrefix.setBatchHint);
            Spatial.BatchHint value = Spatial.BatchHint.valueOf(arg);
            Maud.model.target.setBatchHint(value);
        } else if (actionString.startsWith(ActionPrefix.setCullHint)) {
            arg = MyString.remainder(actionString, ActionPrefix.setCullHint);
            Spatial.CullHint value = Spatial.CullHint.valueOf(arg);
            Maud.model.target.setCullHint(value);
        } else if (actionString.startsWith(ActionPrefix.setQueueBucket)) {
            arg = MyString.remainder(actionString, ActionPrefix.setQueueBucket);
            RenderQueue.Bucket value = RenderQueue.Bucket.valueOf(arg);
            Maud.model.target.setQueueBucket(value);
        } else if (actionString.startsWith(ActionPrefix.setShadowMode)) {
            arg = MyString.remainder(actionString, ActionPrefix.setShadowMode);
            RenderQueue.ShadowMode value;
            value = RenderQueue.ShadowMode.valueOf(arg);
            Maud.model.target.setShadowMode(value);
        } else if (actionString.startsWith(ActionPrefix.setTweenScales)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setTweenScales);
            VectorInterpolation value = VectorInterpolation.valueOf(arg);
            Maud.model.misc.setTweenScales(value);
        } else if (actionString.startsWith(ActionPrefix.setTweenTranslations)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setTweenTranslations);
            VectorInterpolation value = VectorInterpolation.valueOf(arg);
            Maud.model.misc.setTweenTranslations(value);
        } else if (actionString.startsWith(ActionPrefix.setUserData)) {
            arg = MyString.remainder(actionString, ActionPrefix.setUserData);
            Maud.model.target.setUserData(arg);
        } else {
            handled = false;
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
            case "toggle freeze target":
                Maud.model.target.pose.toggleFrozen();
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
                String viewMode = Maud.gui.mouseViewMode();
                if ("scene".equals(viewMode)) {
                    LoadedCgm loadedCgm = Maud.gui.mouseCgm();
                    loadedCgm.scenePov.goHorizontal();
                }
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
                Maud.gui.warpCursor();
                handled = true;
        }

        return handled;
    }

    /**
     * Process an action that starts with "wrap".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean wrapAction(String actionString) {
        boolean handled = false;
        switch (actionString) {
            case "wrap track":
                Maud.model.target.track.wrap();
                handled = true;
        }

        return handled;
    }
}
