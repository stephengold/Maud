/*
 Copyright (c) 2017-2018, Stephen Gold
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
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.ui.InputMode;
import maud.Maud;
import maud.dialog.EditorDialogs;
import maud.dialog.LicenseType;
import maud.model.EditorModel;
import maud.model.History;
import maud.model.cgm.Cgm;
import maud.model.cgm.EditableCgm;
import maud.model.cgm.SelectedTrack;
import maud.model.option.DisplaySettings;
import maud.view.EditorView;
import maud.view.ViewType;
import maud.view.scene.SceneDrag;
import maud.view.scene.SceneView;

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
    final private static Logger logger
            = Logger.getLogger(EditorInputMode.class.getName());
    /**
     * asset path to the cursor for this input mode
     */
    final private static String assetPath = "Textures/cursors/default.cur";
    // *************************************************************************
    // fields

    /**
     * analog listener for POV zoom in/out
     */
    final private ZoomListener zoomListener = new ZoomListener();
    // *************************************************************************
    // constructors

    /**
     * Instantiate a disabled, uninitialized mode.
     */
    public EditorInputMode() {
        super("editor");
    }
    // *************************************************************************
    // InputMode methods

    /**
     * Activate this input mode.
     */
    @Override
    public void activate() {
        super.activate();
        zoomListener.map();
    }

    /**
     * Deactivate this input mode.
     */
    @Override
    public void deactivate() {
        zoomListener.unmap();
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

    /**
     * Process an action from the GUI or keyboard.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param tpf time interval between updates (in seconds, &ge;0)
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
        String[] words = actionString.split(" ");
        String firstWord = words[0];
        if (ongoing) {
            switch (firstWord) {
                case "apply":
                    handled = applyAction(actionString);
                    break;

                case "delete":
                    handled = DeleteAction.process(actionString);
                    break;

                case "launch":
                    handled = launchAction(actionString);
                    break;

                case "load":
                    handled = LoadAction.process(actionString);
                    break;

                case "new":
                    handled = NewAction.process(actionString);
                    break;

                case "next":
                    handled = NextAction.process(actionString);
                    break;

                case "pick":
                    handled = PickAction.process(actionString);
                    break;

                case "previous":
                    handled = PreviousAction.process(actionString);
                    break;

                case "reduce":
                    handled = reduceAction(actionString);
                    break;

                case "rename":
                    handled = RenameAction.process(actionString);
                    break;

                case "resample":
                    handled = resampleAction(actionString);
                    break;

                case "reset":
                    handled = ResetAction.process(actionString);
                    break;

                case "save":
                    handled = saveAction(actionString);
                    break;

                case "select":
                    if (words.length > 1) {
                        char w1c0 = words[1].charAt(0);
                        if (w1c0 < 'o') {
                            handled = SelectANAction.process(actionString);
                        } else if (w1c0 < 't') {
                            handled = SelectOSAction.process(actionString);
                        } else {
                            handled = SelectTZAction.process(actionString);
                        }
                    }
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

        } else { // action not ongoing
            if ("pick".equals(firstWord)) {
                handled = PickAction.processNotOngoing(actionString);
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
    // private methods

    /**
     * Process an ongoing action that starts with the word "apply".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean applyAction(String actionString) {
        boolean handled = false;
        if (actionString.equals(Action.applyDisplaySettings)) {
            DisplaySettings.applyToDisplay();
            handled = true;
        }

        return handled;
    }

    /**
     * Process an ongoing action that starts with the word "launch".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean launchAction(String actionString) {
        boolean handled = false;
        if (actionString.equals(Action.launchProjectile)) {
            EditorView view = Maud.gui.mouseView();
            if (view instanceof SceneView) {
                SceneView sceneView = (SceneView) view;
                sceneView.getProjectile().launch();
            }
            handled = true;
        }

        return handled;
    }

    /**
     * Process an ongoing action that starts with the word "reduce".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean reduceAction(String actionString) {
        EditableCgm target = Maud.getModel().getTarget();

        boolean handled = false;
        if (actionString.equals(Action.reduceAnimation)) {
            EditorDialogs.reduceAnimation();
            handled = true;

        } else if (actionString.equals(Action.reduceTrack)) {
            EditorDialogs.reduceTrack();
            handled = true;

        } else if (actionString.startsWith(ActionPrefix.reduceAnimation)) {
            String f;
            f = MyString.remainder(actionString, ActionPrefix.reduceAnimation);
            int factor = Integer.parseInt(f);
            target.getAnimation().reduce(factor);
            handled = true;

        } else if (actionString.startsWith(ActionPrefix.reduceTrack)) {
            String f;
            f = MyString.remainder(actionString, ActionPrefix.reduceTrack);
            int factor = Integer.parseInt(f);
            target.getTrack().reduce(factor);
            handled = true;
        }

        return handled;
    }

    /**
     * Process an ongoing action that starts with the word "resample".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean resampleAction(String actionString) {
        EditableCgm target = Maud.getModel().getTarget();
        String arg;
        boolean handled = true;
        if (actionString.startsWith(ActionPrefix.resampleAnimationToNumber)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.resampleAnimationToNumber);
            int numSamples = Integer.parseInt(arg);
            target.getAnimation().resampleToNumber(numSamples);

        } else if (actionString.startsWith(
                ActionPrefix.resampleAnimationAtRate)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.resampleAnimationAtRate);
            float sampleRate = Float.parseFloat(arg);
            target.getAnimation().resampleAtRate(sampleRate);

        } else if (actionString.startsWith(
                ActionPrefix.resampleTrackToNumber)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.resampleTrackToNumber);
            int numSamples = Integer.parseInt(arg);
            target.getTrack().resampleToNumber(numSamples);

        } else if (actionString.startsWith(ActionPrefix.resampleTrackAtRate)) {
            String rateString = MyString.remainder(actionString,
                    ActionPrefix.resampleTrackAtRate);
            float sampleRate = Float.parseFloat(rateString);
            target.getTrack().resampleAtRate(sampleRate);

        } else {
            handled = false;
        }

        return handled;
    }

    /**
     * Process an ongoing action that starts with the word "save".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean saveAction(String actionString) {
        EditorModel model = Maud.getModel();
        String baseFilePath;
        boolean handled = true;

        if (actionString.equals(Action.saveDisplaySettings)) {
            DisplaySettings.save();

        } else if (actionString.startsWith(ActionPrefix.saveCgm)) {
            baseFilePath
                    = MyString.remainder(actionString, ActionPrefix.saveCgm);
            model.getTarget().writeToFile(baseFilePath);

        } else if (actionString.startsWith(ActionPrefix.saveCgmUnconfirmed)) {
            baseFilePath = MyString.remainder(actionString,
                    ActionPrefix.saveCgmUnconfirmed);
            EditorDialogs.confirmOverwrite(ActionPrefix.saveCgm, baseFilePath);

        } else if (actionString.startsWith(ActionPrefix.saveMap)) {
            baseFilePath
                    = MyString.remainder(actionString, ActionPrefix.saveMap);
            model.getMap().writeToFile(baseFilePath);

        } else if (actionString.startsWith(ActionPrefix.saveMapUnconfirmed)) {
            baseFilePath = MyString.remainder(actionString,
                    ActionPrefix.saveMapUnconfirmed);
            EditorDialogs.confirmOverwrite(ActionPrefix.saveMap, baseFilePath);

        } else {
            handled = false;
        }

        return handled;
    }

    /**
     * Process an ongoing action that starts with the word "toggle".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean toggleAction(String actionString) {
        boolean handled = true;

        EditorModel model = Maud.getModel();
        switch (actionString) {
            case Action.toggleBoundType:
                model.getTarget().toggleBoundType();
                break;

            case Action.toggleCursorColorIndex:
                model.getMisc().toggleColorIndex();
                break;

            case Action.toggleDegrees:
                model.getMisc().toggleAnglesInDegrees();
                break;

            case Action.toggleDragSide:
                SceneDrag.toggleSide();
                break;

            case Action.toggleFreezeTarget:
                model.getTarget().getPose().toggleFrozen();
                break;

            case Action.toggleIndexBase:
                model.getMisc().toggleIndexBase();
                break;

            case Action.toggleLoadOrientation:
                model.getMisc().toggleLoadOrientation();
                break;

            case Action.toggleMenuBar:
                model.getMisc().toggleMenuBarVisibility();
                break;

            case Action.toggleMovement:
                model.getScene().getCamera().toggleMovement();
                break;

            case Action.togglePause:
                model.getSource().getPlay().togglePaused();
                model.getTarget().getPlay().togglePaused();
                break;

            case Action.togglePauseSource:
                model.getSource().getPlay().togglePaused();
                break;

            case Action.togglePauseTarget:
                if (model.getTarget().getAnimation().isRetargetedPose()) {
                    model.getSource().getPlay().togglePaused();
                } else {
                    model.getTarget().getPlay().togglePaused();
                }
                break;

            case Action.toggleProjection:
                model.getScene().getCamera().toggleProjection();
                break;

            default:
                handled = false;
        }

        return handled;
    }

    /**
     * Process an ongoing action that starts with the word "view".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean viewAction(String actionString) {
        boolean handled = false;
        switch (actionString) {
            case Action.viewHorizontal:
                ViewType viewType = Maud.gui.mouseViewType();
                if (viewType == ViewType.Scene) {
                    Cgm cgm = Maud.gui.mouseCgm();
                    cgm.getScenePov().goHorizontal();
                }
                handled = true;
                break;
        }

        if (!handled && actionString.startsWith(ActionPrefix.viewLicense)) {
            String name;
            name = MyString.remainder(actionString, ActionPrefix.viewLicense);
            LicenseType licenseType = LicenseType.valueOf(name);
            EditorDialogs.license(licenseType);
            handled = true;
        }

        return handled;
    }

    /**
     * Process an ongoing action that starts with the word "warp".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean warpAction(String actionString) {
        boolean handled = false;
        switch (actionString) {
            case Action.warpCursor:
                EditorView mouseView = Maud.gui.mouseView();
                if (mouseView != null) {
                    mouseView.warpCursor();
                }
                handled = true;
                break;

            case Action.warpLastCheckpoint:
                History.redoAll();
                handled = true;
        }

        return handled;
    }

    /**
     * Process an ongoing action that starts with the word "wrap".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean wrapAction(String actionString) {
        boolean handled = true;

        Cgm target = Maud.getModel().getTarget();
        SelectedTrack track = target.getTrack();
        if (actionString.equals(Action.wrapTrack)) {
            if (track.endsWithKeyframe()) {
                EditorDialogs.wrapTrack();
            } else {
                track.wrap(0f);
            }

        } else if (actionString.startsWith(ActionPrefix.wrapAnimation)) {
            String weightString = MyString.remainder(actionString,
                    ActionPrefix.wrapAnimation);
            float endWeight = Float.parseFloat(weightString);
            target.getAnimation().wrapAllTracks(endWeight);

        } else if (actionString.startsWith(ActionPrefix.wrapTrack)) {
            String weightString
                    = MyString.remainder(actionString, ActionPrefix.wrapTrack);
            float endWeight = Float.parseFloat(weightString);
            track.wrap(endWeight);

        } else {
            handled = false;
        }

        return handled;
    }
}
