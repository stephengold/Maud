/*
 Copyright (c) 2017-2022, Stephen Gold
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
import com.jme3.bullet.PhysicsSpace;
import com.jme3.cursors.plugins.JmeCursor;
import com.jme3.renderer.RenderManager;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.minie.PhysicsDumper;
import jme3utilities.ui.InputMode;
import maud.Maud;
import maud.dialog.EditorDialogs;
import maud.dialog.LicenseType;
import maud.dialog.ResampleType;
import maud.model.EditorModel;
import maud.model.History;
import maud.model.cgm.Cgm;
import maud.model.cgm.CgmOutputFormat;
import maud.model.cgm.CgmOutputSet;
import maud.model.cgm.EditableCgm;
import maud.model.cgm.SelectedTrack;
import maud.view.EditorView;
import maud.view.ViewType;
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
    final private static String cursorPath = "Textures/cursors/default.cur";
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
        ZoomListener.unmap();
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
     * Initialize this (disabled) mode prior to its first update.
     *
     * @param stateManager (not null)
     * @param application (not null)
     */
    @Override
    public void initialize(AppStateManager stateManager,
            Application application) {
        AssetManager am = application.getAssetManager();
        JmeCursor cursor = (JmeCursor) am.loadAsset(cursorPath);
        setCursor(cursor);

        super.initialize(stateManager, application);
        Action.addUnbound(this);
    }

    /**
     * Process an action from the GUI or keyboard.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param tpf time interval between frames (in seconds, &ge;0)
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

                case "dump":
                    handled = dumpAction(actionString);
                    break;

                case "launch":
                    handled = launchAction(actionString);
                    break;

                case "load":
                    handled = LoadAction.process(actionString);
                    break;

                case "merge":
                    handled = mergeAction(actionString);
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

                case "reparent":
                    handled = reparentAction(actionString);
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
                        if (w1c0 < 'f') {
                            handled = SelectAEAction.process(actionString);
                        } else if (w1c0 < 'o') {
                            handled = SelectFNAction.process(actionString);
                        } else if (w1c0 < 't') {
                            handled = SelectOSAction.process(actionString);
                        } else {
                            handled = SelectTZAction.process(actionString);
                        }
                    }
                    break;

                case "set":
                    if (words.length > 1) {
                        char w1c0 = words[1].charAt(0);
                        if (w1c0 < 'o') {
                            handled = SetANAction.process(actionString);
                        } else {
                            handled = SetOZAction.process(actionString);
                        }
                    }
                    break;

                case "setFlag":
                    handled = SetFlagAction.process(actionString);
                    break;

                case "toggle":
                    handled = ToggleAction.toggleAction(actionString);
                    break;

                case "view":
                    handled = viewAction(actionString);
                    break;

                case "warp":
                    handled = warpAction(actionString);
                    break;

                case "wrap":
                    handled = wrapAction(actionString);
                    break;

                default:
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
            getActionApplication().onAction(actionString, ongoing, tpf);
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
    private static boolean applyAction(String actionString) {
        Cgm target = Maud.getModel().getTarget();

        boolean handled = true;
        switch (actionString) {
            case Action.applySpatialTransform:
                target.getSpatial().applyTransformToMeshes();
                break;
            default:
                handled = false;
        }

        return handled;
    }

    /**
     * Process an ongoing action that starts with the word "dump".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean dumpAction(String actionString) {
        EditorModel model = Maud.getModel();
        PhysicsDumper dumper = model.getDumper();
        Cgm source = model.getSource();
        Cgm target = model.getTarget();
        Cgm mouseCgm = Maud.gui.mouseCgm();

        boolean handled = true;
        switch (actionString) {
            case Action.dumpAppStates:
                dumper.dump(stateManager);
                break;
            case Action.dumpMouseCgm:
                mouseCgm.dump(dumper);
                break;
            case Action.dumpPhysicsSpace:
                PhysicsSpace space = mouseCgm.getSceneView().getPhysicsSpace();
                dumper.dump(space);
                break;
            case Action.dumpRenderer:
                RenderManager rm = Maud.getApplication().getRenderManager();
                dumper.dump(rm);
                break;
            case Action.dumpSourceCgm:
                source.dump(dumper);
                break;
            case Action.dumpSourcePhysics:
                space = source.getSceneView().getPhysicsSpace();
                dumper.dump(space);
                break;
            case Action.dumpTargetCgm:
                target.dump(dumper);
                break;
            case Action.dumpTargetPhysics:
                space = target.getSceneView().getPhysicsSpace();
                dumper.dump(space);
                break;
            default:
                handled = false;
        }

        return handled;
    }

    /**
     * Process an ongoing action that starts with the word "launch".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private static boolean launchAction(String actionString) {
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
     * Process an ongoing action that starts with the word "merge".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action has been handled, otherwise false
     */
    private static boolean mergeAction(String actionString) {
        EditorModel model = Maud.getModel();
        EditableCgm target = model.getTarget();

        String args;
        boolean handled = false;
        if (actionString.startsWith(ActionPrefix.mergeGeometries)) {
            args = MyString.remainder(actionString,
                    ActionPrefix.mergeGeometries);
            if (args.contains(" ")) {
                String indices = args.split(" ")[0];
                String newGeometryName
                        = MyString.remainder(args, indices + " ");
                target.mergeGeometries(indices, newGeometryName);
            } else {
                EditorDialogs.newGeometry(actionString + " ");
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
    private static boolean reduceAction(String actionString) {
        EditableCgm target = Maud.getModel().getTarget();
        String arg;
        boolean handled = false;
        if (actionString.equals(Action.reduceAnimation)) {
            EditorDialogs.reduceAnimation();
            handled = true;

        } else if (actionString.equals(Action.reduceTrack)) {
            EditorDialogs.reduceTrack();
            handled = true;

        } else if (actionString.startsWith(ActionPrefix.reduceAnimation)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.reduceAnimation);
            int factor = Integer.parseInt(arg);
            target.getAnimation().reduce(factor);
            handled = true;

        } else if (actionString.startsWith(ActionPrefix.reduceTrack)) {
            arg = MyString.remainder(actionString, ActionPrefix.reduceTrack);
            int factor = Integer.parseInt(arg);
            target.getTrack().reduce(factor);
            handled = true;
        }

        return handled;
    }

    /**
     * Process an ongoing action that starts with the word "reparent".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private static boolean reparentAction(String actionString) {
        EditableCgm target = Maud.getModel().getTarget();
        String arg;
        boolean handled = false;
        if (actionString.startsWith(ActionPrefix.reparentSpatials)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.reparentSpatials);
            target.reparentSpatials(arg);
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
    private static boolean resampleAction(String actionString) {
        EditableCgm target = Maud.getModel().getTarget();
        String arg;
        boolean handled = false;
        if (actionString.startsWith(ActionPrefix.resampleAnimation)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.resampleAnimation);
            String[] args = arg.split(" ");
            if (args.length == 2) {
                ResampleType resample = ResampleType.valueOf(args[0]);
                switch (resample) {
                    case AtRate:
                        float sampleRate = Float.parseFloat(args[1]);
                        target.getAnimation().resampleAtRate(sampleRate);
                        handled = true;
                        break;
                    case ToNumber:
                        int numSamples = Integer.parseInt(args[1]);
                        target.getAnimation().resampleToNumber(numSamples);
                        handled = true;
                        break;
                    default:
                }
            }

        } else if (actionString.startsWith(ActionPrefix.resampleTrack)) {
            arg = MyString.remainder(actionString, ActionPrefix.resampleTrack);
            String[] args = arg.split(" ");
            if (args.length == 2) {
                ResampleType resample = ResampleType.valueOf(args[0]);
                switch (resample) {
                    case AtRate:
                        float sampleRate = Float.parseFloat(args[1]);
                        target.getTrack().resampleAtRate(sampleRate);
                        handled = true;
                        break;
                    case ToNumber:
                        int numSamples = Integer.parseInt(args[1]);
                        target.getTrack().resampleToNumber(numSamples);
                        handled = true;
                        break;
                    default:
                }
            }
        }

        return handled;
    }

    /**
     * Process an ongoing action that starts with the word "save".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private static boolean saveAction(String actionString) {
        EditorModel model = Maud.getModel();
        EditableCgm target = model.getTarget();
        CgmOutputFormat format;
        CgmOutputSet outputSet;
        String argList, baseFilePath, prefix;
        String[] args;
        boolean handled = true;

        if (actionString.startsWith(ActionPrefix.saveCgm)) {
            argList = MyString.remainder(actionString, ActionPrefix.saveCgm);
            if (argList.contains(" ")) {
                args = argList.split(" ");
                outputSet = CgmOutputSet.valueOf(args[0]);
                format = CgmOutputFormat.valueOf(args[1]);
                prefix = args[0] + " " + args[1] + " ";
                baseFilePath = MyString.remainder(argList, prefix);
                target.writeToFile(outputSet, format, baseFilePath);
            } else {
                handled = false;
            }

        } else if (actionString.startsWith(ActionPrefix.saveCgmUnconfirmed)) {
            argList = MyString.remainder(actionString,
                    ActionPrefix.saveCgmUnconfirmed);
            if (argList.contains(" ")) {
                args = argList.split(" ");
                outputSet = CgmOutputSet.valueOf(args[0]);
                format = CgmOutputFormat.valueOf(args[1]);
                prefix = args[0] + " " + args[1] + " ";
                baseFilePath = MyString.remainder(argList, prefix);
                EditorDialogs.confirmOverwrite(ActionPrefix.saveCgm, outputSet,
                        format, baseFilePath);
            } else {
                handled = false;
            }

        } else if (actionString.startsWith(ActionPrefix.saveMap)) {
            argList = MyString.remainder(actionString, ActionPrefix.saveMap);
            if (argList.contains(" ")) {
                args = argList.split(" ");
                format = CgmOutputFormat.valueOf(args[0]);
                baseFilePath = MyString.remainder(argList, args[0] + " ");
                model.getMap().writeToFile(format, baseFilePath);
            } else {
                handled = false;
            }

        } else if (actionString.startsWith(ActionPrefix.saveMapUnconfirmed)) {
            argList = MyString.remainder(actionString,
                    ActionPrefix.saveMapUnconfirmed);
            if (argList.contains(" ")) {
                args = argList.split(" ");
                format = CgmOutputFormat.valueOf(args[0]);
                baseFilePath = MyString.remainder(argList, args[0] + " ");
                EditorDialogs.confirmOverwrite(ActionPrefix.saveMap, null,
                        format, baseFilePath);
            } else {
                handled = false;
            }

        } else if (actionString.startsWith(ActionPrefix.saveTexture)) {
            argList = MyString.remainder(actionString,
                    ActionPrefix.saveTexture);
            if (argList.contains(" ")) {
                args = argList.split(" ");
                boolean flipY = Boolean.parseBoolean(args[0]);
                String assetPath = MyString.remainder(argList, args[0] + " ");
                target.getTexture().writeImageToAsset(assetPath, flipY);
            } else {
                handled = false;
            }

        } else {
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
    private static boolean viewAction(String actionString) {
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
            default:
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
    private static boolean warpAction(String actionString) {
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
                break;

            default:
        }

        return handled;
    }

    /**
     * Process an ongoing action that starts with the word "wrap".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private static boolean wrapAction(String actionString) {
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
