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
import maud.Maud;
import maud.model.EditableCgm;
import maud.model.EditorModel;
import maud.model.History;
import maud.model.LoadedCgm;
import maud.view.SceneDrag;
import maud.view.ScoreDrag;
import maud.view.ViewType;

/**
 * Input mode for Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class EditorInputMode extends InputMode {
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
    public EditorInputMode() {
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
                    handled = LoadAction.process(actionString);
                    break;
                case "new":
                    handled = NewAction.process(actionString);
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
                    handled = SelectAction.process(actionString);
                    break;
                case "set":
                    handled = SetAction.process(actionString);
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
            SceneDrag.clearDragAxis();
            ScoreDrag.setDraggingGnomon(null);
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
            Maud.getModel().getTarget().animation.copyAndLoad(destName);
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
        EditableCgm target = Maud.getModel().getTarget();
        switch (actionString) {
            case "delete animation":
                target.animation.delete();
                break;
            case "delete control":
                target.sgc.delete();
                break;
            case "delete mapping":
                Maud.getModel().getMap().deleteBone();
                break;
            case "delete singleKeyframe":
                target.track.deleteSingleKeyframe();
                break;
            case "delete userKey":
                Maud.getModel().getMisc().deleteUserKey();
                break;
            default:
                handled = false;
                if (actionString.startsWith(ActionPrefix.deleteAssetFolder)) {
                    String arg = MyString.remainder(actionString,
                            ActionPrefix.deleteAssetFolder);
                    Maud.getModel().getLocations().remove(arg);
                    handled = true;
                }
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
        EditorModel model = Maud.getModel();
        EditableCgm target = model.getTarget();

        boolean handled = true;
        switch (actionString) {
            case "next animation":
                target.animation.loadNext();
                break;
            case "next animControl":
                target.nextAnimControl();
                break;
            case "next bone":
                target.bone.selectNext();
                break;
            case "next checkpoint":
                History.redo();
                break;
            case "next control":
                target.sgc.selectNext();
                break;
            case "next mapping":
                model.getMap().selectNext();
                break;
            case "next sourceAnimation":
                model.getSource().animation.loadNext();
                break;
            case "next sourceAnimControl":
                model.getSource().nextAnimControl();
                break;
            case "next userData":
                model.getMisc().selectNextUserKey();
                break;
            case "next viewMode":
                model.getMisc().selectNextViewMode();
                break;
            default:
                handled = false;
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
        EditorModel model = Maud.getModel();
        EditableCgm target = model.getTarget();

        boolean handled = true;
        switch (actionString) {
            case "previous animation":
                target.animation.loadPrevious();
                break;
            case "previous animControl":
                target.previousAnimControl();
                break;
            case "previous bone":
                target.bone.selectPrevious();
                break;
            case "previous checkpoint":
                History.undo();
                break;
            case "previous control":
                target.sgc.selectPrevious();
                break;
            case "previous mapping":
                model.getMap().selectPrevious();
                break;
            case "previous sourceAnimation":
                model.getSource().animation.loadPrevious();
                break;
            case "previous sourceAnimControl":
                model.getSource().previousAnimControl();
                break;
            case "previous userData":
                model.getMisc().selectPreviousUserKey();
                break;
            default:
                handled = false;
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
            Maud.getModel().getTarget().animation.reduce(factor);
            handled = true;

        } else if (actionString.startsWith(ActionPrefix.reduceTrack)) {
            String f;
            f = MyString.remainder(actionString, ActionPrefix.reduceTrack);
            int factor = Integer.parseInt(f);
            Maud.getModel().getTarget().track.reduceTrack(factor);
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
            EditableCgm target = Maud.getModel().getTarget();
            if (actionString.startsWith(ActionPrefix.renameAnimation)) {
                newName = MyString.remainder(actionString,
                        ActionPrefix.renameAnimation);
                target.animation.rename(newName);
                handled = true;

            } else if (actionString.startsWith(ActionPrefix.renameBone)) {
                newName = MyString.remainder(actionString,
                        ActionPrefix.renameBone);
                target.renameBone(newName);
                handled = true;

            } else if (actionString.startsWith(ActionPrefix.renameSpatial)) {
                newName = MyString.remainder(actionString,
                        ActionPrefix.renameSpatial);
                target.renameSpatial(newName);
                handled = true;

            } else if (actionString.startsWith(ActionPrefix.renameUserKey)) {
                newName = MyString.remainder(actionString,
                        ActionPrefix.renameUserKey);
                target.renameUserKey(newName);
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
        EditableCgm target = Maud.getModel().getTarget();
        switch (actionString) {
            case "reset bone ang anim":
                target.bone.setRotationToAnimation();
                handled = true;
                break;
            case "reset bone ang bind":
                target.bone.resetRotation();
                handled = true;
                break;
            case "reset bone off anim":
                target.bone.setTranslationToAnimation();
                handled = true;
                break;
            case "reset bone off bind":
                target.bone.resetTranslation();
                handled = true;
                break;
            case "reset bone sca anim":
                target.bone.setScaleToAnimation();
                handled = true;
                break;
            case "reset bone sca bind":
                target.bone.resetScale();
                handled = true;
                break;
            case "reset bone selection":
                LoadedCgm cgm = Maud.gui.mouseCgm();
                cgm.bone.deselect();
                handled = true;
                break;

            case "reset spatial rotation":
                target.setSpatialRotation(rotationIdentity);
                handled = true;
                break;
            case "reset spatial scale":
                target.setSpatialScale(scaleIdentity);
                handled = true;
                break;
            case "reset spatial translation":
                target.setSpatialTranslation(translateIdentity);
                handled = true;
                break;

            case "reset twist":
                Maud.getModel().getMap().setTwist(rotationIdentity);
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
            Maud.getModel().getMap().retargetAndLoad(name);
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
            Maud.getModel().getTarget().writeToFile(path);
            handled = true;

        } else if (actionString.startsWith(ActionPrefix.saveMap)) {
            String path;
            path = MyString.remainder(actionString, ActionPrefix.saveMap);
            Maud.getModel().getMap().writeToFile(path);
            handled = true;
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

        EditorModel model = Maud.getModel();
        switch (actionString) {
            case "toggle degrees":
                model.getMisc().toggleAnglesInDegrees();
                handled = true;
                break;
            case "toggle dragSide":
                SceneDrag.toggleDragSide();
                handled = true;
                break;
            case "toggle freeze target":
                model.getTarget().pose.toggleFrozen();
                handled = true;
                break;
            case "toggle pause":
                model.getSource().animation.togglePaused();
                model.getTarget().animation.togglePaused();
                handled = true;
                break;
            case "toggle pause source":
                model.getSource().animation.togglePaused();
                handled = true;
                break;
            case "toggle pause target":
                model.getTarget().animation.togglePaused();
                handled = true;
                break;
            case "toggle projection":
                model.getScene().getCamera().toggleProjection();
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
                ViewType viewType = Maud.gui.mouseViewType();
                if (viewType == ViewType.Scene) {
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
                Maud.getModel().getTarget().track.wrap();
                handled = true;
        }

        return handled;
    }
}
