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
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.controls.CheckBoxStateChangedEvent;
import de.lessvoid.nifty.controls.RadioButtonStateChangedEvent;
import de.lessvoid.nifty.controls.Slider;
import de.lessvoid.nifty.controls.SliderChangedEvent;
import de.lessvoid.nifty.screen.Screen;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyCamera;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import jme3utilities.nifty.GuiScreenController;
import jme3utilities.nifty.WindowController;
import jme3utilities.ui.InputMode;
import maud.action.EditorInputMode;
import maud.menu.BoneMenus;
import maud.menu.BuildMenus;
import maud.menu.EditorMenus;
import maud.model.Checkpoint;
import maud.model.History;
import maud.model.LoadedCgm;
import maud.model.Pov;
import maud.tool.EditorTools;
import maud.view.EditorView;
import maud.view.SceneDrag;
import maud.view.ScoreDrag;
import maud.view.ViewType;

/**
 * The screen controller for Maud's editor screen. The GUI includes a menu bar,
 * numerous tool windows, and a status bar.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class EditorScreen extends GuiScreenController {
    // *************************************************************************
    // constants and loggers

    /**
     * largest squared distance for bone/axis selection (in pixels squared)
     */
    final private static float dSquaredThreshold = 1000f;
    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            EditorScreen.class.getName());
    /**
     * name of the signal that rotates the model counter-clockwise around +Y
     */
    final private static String modelCCWSignalName = "modelLeft";
    /**
     * name of the signal that rotates the model clockwise around +Y
     */
    final private static String modelCWSignalName = "modelRight";
    /**
     * name of the signal that diverts rotation from target to source
     */
    final public static String sourceModelSignalName = "sourceModel";
    // *************************************************************************
    // fields

    /**
     * flag that causes this controller to temporarily ignore change events from
     * GUI controls during an update
     */
    private boolean ignoreGuiChanges = false;
    /**
     * bone menus for this screen
     */
    final public BoneMenus boneMenus = new BoneMenus();
    /**
     * build menus for this screen
     */
    final public BuildMenus buildMenus = new BuildMenus();
    /**
     * dialog boxes created by this screen
     */
    final public EditorDialogs dialogs = new EditorDialogs();
    /**
     * input mode for this screen
     */
    final EditorInputMode inputMode = new EditorInputMode();
    /**
     * menus for this screen
     */
    final public EditorMenus menus = new EditorMenus();
    /**
     * controllers for tool windows
     */
    final public EditorTools tools = new EditorTools(this);
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized, disabled display that will not be enabled
     * during initialization.
     */
    EditorScreen() {
        super("editor", "Interface/Nifty/huds/editor.xml", false);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Add a checkpoint and report details to the status line.
     *
     * @param source textual description of what triggered invocation (not null
     * or empty)
     */
    public void addCheckpoint(String source) {
        Validate.nonEmpty(source, "source");

        int checkpointIndex = History.addCheckpoint();
        Checkpoint checkpoint = History.getCheckpoint(checkpointIndex);
        Date creationDate = checkpoint.copyTimestamp();
        String creationTime = DateFormat.getTimeInstance().format(creationDate);

        tools.history.setAutoScroll();
        String message = String.format("added checkpoint[%d] from %s at %s",
                checkpointIndex, source, creationTime);
        setStatus(message);
    }

    /**
     * Activate "Bind" screen.
     */
    public void goBindScreen() {
        closeAllPopups();
        Maud.bindScreen.activate(inputMode);
    }

    /**
     * Select a loaded CG model (source or target) based on the screen position
     * of the mouse pointer.
     *
     * @return a pre-existing instance, or null if none applies
     */
    public LoadedCgm mouseCgm() {
        LoadedCgm source = Maud.getModel().getSource();
        LoadedCgm target = Maud.getModel().target;
        ViewPort sScene = source.getSceneView().getViewPort();
        ViewPort sScore = source.getScoreView().getViewPort();
        ViewPort tScene = target.getSceneView().getViewPort();
        ViewPort tScore = target.getScoreView().getViewPort();

        Vector2f screenXY = inputManager.getCursorPosition();
        List<ViewPort> viewPorts;
        viewPorts = MyCamera.listViewPorts(renderManager, screenXY);
        LoadedCgm cgm = null;
        for (ViewPort vp : viewPorts) {
            if (vp.isEnabled()) {
                if (vp == sScene || vp == sScore) {
                    cgm = source;
                    break;
                } else if (vp == tScene || vp == tScore) {
                    cgm = target;
                    break;
                }
            }
        }

        return cgm;
    }

    /**
     * Select a POV based on the screen position of the mouse pointer.
     *
     * @return the pre-existing instance, or null if none applies
     */
    public Pov mousePov() {
        LoadedCgm source = Maud.getModel().getSource();
        LoadedCgm target = Maud.getModel().target;
        EditorView sScene = source.getSceneView();
        EditorView sScore = source.getScoreView();
        EditorView tScene = target.getSceneView();
        EditorView tScore = target.getScoreView();

        Vector2f screenXY = inputManager.getCursorPosition();
        List<ViewPort> viewPorts;
        viewPorts = MyCamera.listViewPorts(renderManager, screenXY);
        Pov result = null;
        for (ViewPort vp : viewPorts) {
            if (vp.isEnabled()) {
                if (vp == sScene.getViewPort()) {
                    result = source.scenePov;
                    break;
                } else if (vp == sScore.getViewPort()) {
                    result = source.scorePov;
                    break;
                } else if (vp == tScene.getViewPort()) {
                    result = target.scenePov;
                    break;
                } else if (vp == tScore.getViewPort()) {
                    result = target.scorePov;
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Select a view based on the screen position of the mouse pointer.
     *
     * @return the pre-existing instance, or null if none applies
     */
    public EditorView mouseView() {
        LoadedCgm source = Maud.getModel().getSource();
        LoadedCgm target = Maud.getModel().target;
        EditorView sScene = source.getSceneView();
        EditorView sScore = source.getScoreView();
        EditorView tScene = target.getSceneView();
        EditorView tScore = target.getScoreView();

        Vector2f screenXY = inputManager.getCursorPosition();
        List<ViewPort> viewPorts;
        viewPorts = MyCamera.listViewPorts(renderManager, screenXY);
        EditorView result = null;
        for (ViewPort vp : viewPorts) {
            if (vp.isEnabled()) {
                if (vp == sScene.getViewPort()) {
                    result = sScene;
                    break;
                } else if (vp == sScore.getViewPort()) {
                    result = sScore;
                    break;
                } else if (vp == tScene.getViewPort()) {
                    result = tScene;
                    break;
                } else if (vp == tScore.getViewPort()) {
                    result = tScore;
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Select a view type based on the screen position of the mouse pointer.
     *
     * @return an enum value, or null if neither applies
     */
    public ViewType mouseViewType() {
        ViewType result = null;
        EditorView view = mouseView();
        if (view != null) {
            result = view.getType();
        }

        return result;
    }

    /**
     * Callback handler that Nifty invokes after a check box changes.
     *
     * @param boxId Nifty element id of the check box (not null)
     * @param event details of the event (not null)
     */
    @NiftyEventSubscriber(pattern = ".*CheckBox")
    public void onCheckBoxChanged(final String boxId,
            final CheckBoxStateChangedEvent event) {
        Validate.nonNull(boxId, "check box id");
        Validate.nonNull(event, "event");
        assert boxId.endsWith("CheckBox");

        if (ignoreGuiChanges || !hasStarted()) {
            return;
        }

        boolean isChecked = event.isChecked();
        String prefix = MyString.removeSuffix(boxId, "CheckBox");
        switch (prefix) {
            case "3DCursor":
                Maud.getModel().cursor.setVisible(isChecked);
                break;
            case "axesDepthTest":
                Maud.getModel().axes.setDepthTestFlag(isChecked);
                break;
            case "boundsDepthTest":
                Maud.getModel().bounds.setDepthTestFlag(isChecked);
                break;
            case "freeze":
                Maud.getModel().target.pose.setFrozen(isChecked);
                break;
            case "invertRma":
            case "invertRma2":
                Maud.getModel().getMap().setInvertMap(isChecked);
                break;
            case "loop":
                Maud.getModel().target.animation.setContinue(isChecked);
                break;
            case "loopSource":
                Maud.getModel().getSource().animation.setContinue(isChecked);
                break;
            case "physics":
                Maud.getModel().scene.setPhysicsRendered(isChecked);
                break;
            case "pin":
                Maud.getModel().target.animation.setPinned(isChecked);
                break;
            case "pinSource":
                Maud.getModel().getSource().animation.setPinned(isChecked);
                break;
            case "pong":
                Maud.getModel().target.animation.setReverse(isChecked);
                break;
            case "pongSource":
                Maud.getModel().getSource().animation.setReverse(isChecked);
                break;
            case "scoreRotations":
                Maud.getModel().getScore().setShowRotations(isChecked);
                break;
            case "scoreScales":
                Maud.getModel().getScore().setShowScales(isChecked);
                break;
            case "scoreTranslations":
                Maud.getModel().getScore().setShowTranslations(isChecked);
                break;
            case "shadows":
                Maud.getModel().scene.setShadowsRendered(isChecked);
                break;
            case "skeleton":
                Maud.getModel().skeleton.setVisible(isChecked);
                break;
            case "sky":
                Maud.getModel().scene.setSkyRendered(isChecked);
                break;
            default:
                logger.log(Level.WARNING, "check box with unknown id={0}",
                        MyString.quote(boxId));
        }
    }

    /**
     * Callback handler that Nifty invokes after a radio button changes.
     *
     * @param buttonId Nifty element id of the radio button (not null)
     * @param event details of the event (not null)
     */
    @NiftyEventSubscriber(pattern = ".*RadioButton")
    public void onRadioButtonChanged(final String buttonId,
            final RadioButtonStateChangedEvent event) {
        Validate.nonNull(buttonId, "button id");
        Validate.nonNull(event, "event");

        if (ignoreGuiChanges || !hasStarted() || !event.isSelected()) {
            return;
        }

        switch (buttonId) {
            case "boneAxesRadioButton":
                Maud.getModel().axes.setMode("bone");
                break;
            case "modelAxesRadioButton":
                Maud.getModel().axes.setMode("model");
                break;
            case "spatialAxesRadioButton":
                Maud.getModel().axes.setMode("spatial");
                break;
            case "worldAxesRadioButton":
                Maud.getModel().axes.setMode("world");
                break;
            case "hideAxesRadioButton":
                Maud.getModel().axes.setMode("none");
                break;

            case "flyRadioButton":
                Maud.getModel().camera.setMode("fly");
                break;
            case "orbitRadioButton":
                Maud.getModel().camera.setMode("orbit");
                break;
            case "perspectiveRadioButton":
                Maud.getModel().camera.setMode("perspective");
                break;
            case "parallelRadioButton":
                Maud.getModel().camera.setMode("parallel");
                break;

            case "scoreNoneAllRadioButton":
                Maud.getModel().getScore().setShowNoneSelected("all");
                break;
            case "scoreNoneNoneRadioButton":
                Maud.getModel().getScore().setShowNoneSelected("none");
                break;
            case "scoreNoneRootsRadioButton":
                Maud.getModel().getScore().setShowNoneSelected("roots");
                break;
            case "scoreNoneTrackedRadioButton":
                Maud.getModel().getScore().setShowNoneSelected("tracked");
                break;

            case "scoreWhenAllRadioButton":
                Maud.getModel().getScore().setShowWhenSelected("all");
                break;
            case "scoreWhenAncestorsRadioButton":
                Maud.getModel().getScore().setShowWhenSelected("ancestors");
                break;
            case "scoreWhenFamilyRadioButton":
                Maud.getModel().getScore().setShowWhenSelected("family");
                break;
            case "scoreWhenSelectedRadioButton":
                Maud.getModel().getScore().setShowWhenSelected("selected");
                break;

            case "noPlatformRadioButton":
                Maud.getModel().scene.setPlatformMode("none");
                break;
            case "squarePlatformRadioButton":
                Maud.getModel().scene.setPlatformMode("square");
                break;

            default:
                logger.log(Level.WARNING, "unknown radio button with id={0}",
                        MyString.quote(buttonId));
        }
    }

    /**
     * Callback handler that Nifty invokes after a slider changes.
     *
     * @param sliderId Nifty element id of the slider (not null)
     * @param event details of the event (not null, ignored)
     */
    @NiftyEventSubscriber(pattern = ".*Slider")
    public void onSliderChanged(final String sliderId,
            final SliderChangedEvent event) {
        Validate.nonNull(sliderId, "slider id");
        Validate.nonNull(event, "event");

        if (ignoreGuiChanges || !hasStarted()) {
            return;
        }

        tools.onSliderChanged(sliderId, event);
    }

    /**
     * Read a bank of 3 sliders that control a color.
     *
     * @param prefix unique id prefix of the bank (not null)
     * @return color indicated by the sliders (new instance)
     */
    public ColorRGBA readColorBank(String prefix) {
        assert prefix != null;

        float r = readSlider(prefix + "R");
        float g = readSlider(prefix + "G");
        float b = readSlider(prefix + "B");
        ColorRGBA color = new ColorRGBA(r, g, b, 1f);

        return color;
    }

    /**
     * Read a bank of 3 sliders that control a vector.
     *
     * @param infix unique id infix of the bank (not null)
     * @return vector indicated by the sliders (new instance)
     */
    public Vector3f readVectorBank(String infix) {
        assert infix != null;

        float x = readSlider("x" + infix);
        float y = readSlider("y" + infix);
        float z = readSlider("z" + infix);
        Vector3f vector = new Vector3f(x, y, z);

        return vector;
    }

    /**
     * Handle a "select spatialChild" action with an argument.
     *
     * @param argument action argument (not null)
     */
    public void selectSpatialChild(String argument) {
        List<String> children;
        children = Maud.getModel().target.spatial.listNumberedChildren();
        int childIndex = children.indexOf(argument);
        if (childIndex >= 0) {
            Maud.getModel().target.spatial.selectChild(childIndex);
        } else {
            Maud.gui.buildMenus.selectSpatialChild(argument);
        }
    }

    /**
     * Handle a "select tool" action.
     *
     * @param toolName which tool to select (not null)
     * @return true if the action is handled, otherwise false
     */
    public boolean selectTool(String toolName) {
        WindowController controller = tools.getTool(toolName);
        if (controller == null) {
            return false;
        } else {
            controller.select();
            return true;
        }
    }

    /**
     * Select an axis, bone, gnomon, or keyframe based on the screen coordinates
     * of the mouse pointer.
     */
    public void selectXY() {
        LoadedCgm mouseCgm = Maud.gui.mouseCgm();
        EditorView mouseView = Maud.gui.mouseView();
        if (mouseCgm != null && mouseView != null) {
            Vector2f mouseXY = inputManager.getCursorPosition();
            Selection selection = new Selection(mouseXY, dSquaredThreshold);
            mouseView.considerAll(selection);
            selection.select();
        }
    }

    /**
     * Set a bank of 3 sliders that control a color.
     *
     * @param prefix unique id prefix of the bank (not null)
     * @param color (not null, unaffected)
     */
    public void setColorBank(String prefix, ColorRGBA color) {
        assert prefix != null;

        Slider slider = Maud.gui.getSlider(prefix + "R");
        slider.setValue(color.r);
        Maud.gui.updateSliderStatus(prefix + "R", color.r, "");

        slider = Maud.gui.getSlider(prefix + "G");
        slider.setValue(color.g);
        Maud.gui.updateSliderStatus(prefix + "G", color.g, "");

        slider = Maud.gui.getSlider(prefix + "B");
        slider.setValue(color.b);
        Maud.gui.updateSliderStatus(prefix + "B", color.b, "");
    }

    /**
     * Alter the "ignore GUI changes" flag.
     *
     * @param newSetting true &rarr; ignore events, false &rarr; invoke callback
     * handlers for events
     */
    public void setIgnoreGuiChanges(boolean newSetting) {
        ignoreGuiChanges = newSetting;
    }

    /**
     * Update the status bar.
     *
     * @param message what to display (not null)
     */
    void setStatus(String message) {
        assert message != null;
        setStatusText("messageLabel", message);
    }

    /**
     * Update a bank of 3 sliders that control a color.
     *
     * @param prefix unique id prefix of the bank (not null)
     * @return color indicated by the sliders (new instance)
     */
    ColorRGBA updateColorBank(String prefix) {
        assert prefix != null;

        float r = updateSlider(prefix + "R", "");
        float g = updateSlider(prefix + "G", "");
        float b = updateSlider(prefix + "B", "");
        ColorRGBA color = new ColorRGBA(r, g, b, 1f);

        return color;
    }

    /**
     * Attempt to warp a cursor to the screen coordinates of the mouse pointer.
     */
    public void warpCursor() {
        EditorView mouseView = Maud.gui.mouseView();
        if (mouseView != null) {
            mouseView.warpCursor();
        }
    }
    // *************************************************************************
    // AppState methods

    /**
     * Initialize this controller prior to its 1st update.
     *
     * @param stateManager (not null)
     * @param application application that owns this screen (not null)
     */
    @Override
    public void initialize(AppStateManager stateManager,
            Application application) {
        if (isEnabled()) {
            throw new IllegalStateException("shouldn't be enabled yet");
        }

        InputMode.getActiveMode().setEnabled(false);
        inputMode.setEnabled(true);
        inputMode.influence(this);
        setListener(inputMode);

        super.initialize(stateManager, application);

        Maud.getModel().target.loadNamed("Jaime");
    }

    /**
     * Callback to update the editor screen prior to rendering. (Invoked once
     * per render pass.)
     *
     * @param tpf time interval between render passes (in seconds, &ge;0)
     */
    @Override
    public void update(float tpf) {
        super.update(tpf);

        if (!tools.getTool("camera").isInitialized()) {
            return;
        }
        LoadedCgm source = Maud.getModel().getSource();
        LoadedCgm target = Maud.getModel().target;
        /*
         * Update animations even when the animation tool is disabled.
         */
        if (source.animation.isMoving()) {
            updateTrackTime(source, tpf);
        }
        if (target.animation.isMoving()) {
            updateTrackTime(target, tpf);
        } else if (target.animation.isRetargetedPose()) {
            target.pose.setToAnimation();
        }
        /*
         * Configure view ports based on the MVC model.
         */
        Maud application = Maud.getApplication();
        application.updateViewPorts();

        ViewType viewType = mouseViewType();
        if (viewType == ViewType.Scene) {
            /*
             * Based on mouse pointer position, select a loaded CG model
             * to rotate around its Y-axis.
             */
            LoadedCgm cgmToRotate = mouseCgm();
            if (cgmToRotate != null) {
                if (signals.test(modelCCWSignalName)) {
                    cgmToRotate.transform.rotateY(tpf);
                }
                if (signals.test(modelCWSignalName)) {
                    cgmToRotate.transform.rotateY(-tpf);
                }
            }

            if (SceneDrag.isDraggingAxis()) {
                Maud.gui.tools.axes.dragAxis();
            }

        } else if (viewType == ViewType.Score) {
            LoadedCgm cgm = ScoreDrag.getDraggingGnomonCgm();
            if (cgm != null) {
                Camera camera = cgm.getScoreView().getCamera();
                Vector2f mouseXY = inputManager.getCursorPosition();
                Vector3f world = camera.getWorldCoordinates(mouseXY, 0f);
                float worldX = FastMath.clamp(world.x, 0f, 1f);
                float duration = cgm.animation.getDuration();
                float newTime = worldX * duration;
                cgm.animation.setTime(newTime);
            }
        }
        tools.camera.updatePov();
        /*
         * Update the views.
         */
        source.getSceneView().update(null);
        target.getSceneView().update(null);
        source.getScoreView().update(source);
        target.getScoreView().update(target);
    }
    // *************************************************************************
    // ScreenController methods

    /**
     * A callback which Nifty invokes the 1st time the screen is displayed.
     *
     * @param nifty (not null)
     * @param screen (not null)
     */
    @Override
    public void bind(Nifty nifty, Screen screen) {
        super.bind(nifty, screen);

        Maud maud = Maud.getApplication();
        maud.startup2();
    }
    // *************************************************************************
    // private methods

    /**
     * Update the track time.
     *
     * @param loadedCgm (not null)
     * @param tpf time interval between render passes (in seconds, &ge;0)
     */
    private void updateTrackTime(LoadedCgm loadedCgm, float tpf) {
        assert loadedCgm.animation.isMoving();

        float speed = loadedCgm.animation.getSpeed();
        float time = loadedCgm.animation.getTime();
        time += speed * tpf;

        boolean cont = loadedCgm.animation.willContinue();
        boolean reverse = loadedCgm.animation.willReverse();
        float duration = loadedCgm.animation.getDuration();
        if (duration == 0f) {
            time = 0f;
        } else if (cont && !reverse) {
            time = MyMath.modulo(time, duration); // wrap
        } else {
            float freeTime = time;
            time = FastMath.clamp(time, 0f, duration);
            if (time != freeTime) { // reached a limit
                if (reverse) {
                    loadedCgm.animation.setSpeed(-speed); // pong
                } else {
                    time = duration - time; // wrap
                }
                loadedCgm.animation.setPaused(!cont);
            }
        }
        loadedCgm.animation.setTime(time);
    }
}
