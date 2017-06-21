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
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Spatial;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.controls.CheckBoxStateChangedEvent;
import de.lessvoid.nifty.controls.RadioButtonStateChangedEvent;
import de.lessvoid.nifty.controls.Slider;
import de.lessvoid.nifty.controls.SliderChangedEvent;
import de.lessvoid.nifty.screen.Screen;
import java.text.DateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import jme3utilities.nifty.GuiScreenController;
import jme3utilities.nifty.WindowController;
import jme3utilities.ui.InputMode;
import maud.model.Checkpoint;
import maud.model.History;
import maud.model.LoadedCGModel;
import maud.tools.DddTools;

/**
 * The screen controller for the GUI portion of Maud's "3D View" screen. The GUI
 * includes a menu bar, numerous tool windows, and a status bar.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class DddGui extends GuiScreenController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            DddGui.class.getName());
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
     * dialog boxes created by this screen
     */
    final DddDialogs dialogs = new DddDialogs();
    /**
     * input mode for this screen
     */
    final DddInputMode inputMode = new DddInputMode();
    /**
     * menus for this screen
     */
    final DddMenus menus = new DddMenus();
    /**
     * controllers for tool windows
     */
    final public DddTools tools = new DddTools(this);
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized, disabled display that will not be enabled
     * during initialization.
     */
    DddGui() {
        super("3D View", "Interface/Nifty/huds/3DView.xml", false);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Add a checkpoint and report details to the status line.
     *
     * @param source textual description of what triggered invocation (not null
     * or empty)
     */
    void addCheckpoint(String source) {
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

        if (ignoreGuiChanges || !hasStarted()) {
            return;
        }

        boolean isChecked = event.isChecked();

        switch (boxId) {
            case "3DCursorCheckBox":
                Maud.model.cursor.setVisible(isChecked);
                break;
            case "axesDepthTestCheckBox":
                Maud.model.axes.setDepthTestFlag(isChecked);
                break;
            case "boundsDepthTestCheckBox":
                Maud.model.bounds.setDepthTestFlag(isChecked);
                break;
            case "invertRmaCheckBox":
            case "invertRma2CheckBox":
                Maud.model.mapping.setInvertMap(isChecked);
                break;
            case "loopCheckBox":
                Maud.model.target.animation.setContinue(isChecked);
                break;
            case "loopSourceCheckBox":
                Maud.model.source.animation.setContinue(isChecked);
                break;
            case "pongCheckBox":
                Maud.model.target.animation.setReverse(isChecked);
                break;
            case "pongSourceCheckBox":
                Maud.model.source.animation.setReverse(isChecked);
                break;
            case "shadowsCheckBox":
                Maud.model.misc.setShadowsRendered(isChecked);
                break;
            case "skeletonCheckBox":
                Maud.model.skeleton.setVisible(isChecked);
                break;
            case "skyCheckBox":
                Maud.model.misc.setSkyRendered(isChecked);
                break;
            default:
                logger.log(Level.WARNING, "unknown check box with id={0}",
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
                Maud.model.axes.setMode("bone");
                break;
            case "modelAxesRadioButton":
                Maud.model.axes.setMode("model");
                break;
            case "spatialAxesRadioButton":
                Maud.model.axes.setMode("spatial");
                break;
            case "worldAxesRadioButton":
                Maud.model.axes.setMode("world");
                break;
            case "hideAxesRadioButton":
                Maud.model.axes.setMode("none");
                break;

            case "flyingRadioButton":
                Maud.model.camera.setMode("fly");
                break;
            case "orbitingRadioButton":
                Maud.model.camera.setMode("orbit");
                break;

            case "cullInheritRadioButton":
                Maud.model.target.setHint(Spatial.CullHint.Inherit);
                break;
            case "cullDynamicRadioButton":
                Maud.model.target.setHint(Spatial.CullHint.Dynamic);
                break;
            case "cullAlwaysRadioButton":
                Maud.model.target.setHint(Spatial.CullHint.Always);
                break;
            case "cullNeverRadioButton":
                Maud.model.target.setHint(Spatial.CullHint.Never);
                break;

            case "shadowOffRadioButton":
                Maud.model.target.setMode(RenderQueue.ShadowMode.Off);
                break;
            case "shadowCastRadioButton":
                Maud.model.target.setMode(RenderQueue.ShadowMode.Cast);
                break;
            case "shadowReceiveRadioButton":
                Maud.model.target.setMode(RenderQueue.ShadowMode.Receive);
                break;
            case "shadowCastAndReceiveRadioButton":
                Maud.model.target.setMode(RenderQueue.ShadowMode.CastAndReceive);
                break;
            case "shadowInheritRadioButton":
                Maud.model.target.setMode(RenderQueue.ShadowMode.Inherit);
                break;

            case "noPlatformRadioButton":
                Maud.model.misc.setPlatformMode("none");
                break;
            case "squarePlatformRadioButton":
                Maud.model.misc.setPlatformMode("square");
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
     * Read a bank of 3 sliders that control a rotation.
     *
     * @param prefix unique id prefix of the bank (not null)
     * @return rotation indicated by the sliders (new instance)
     */
    Quaternion readAngleBank(String prefix) {
        assert prefix != null;

        float x = readSlider("x" + prefix);
        float y = readSlider("y" + prefix);
        float z = readSlider("z" + prefix);
        Quaternion rotation = new Quaternion();
        rotation.fromAngles(x, y, z);

        return rotation;
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
     * Select a loaded CG model (source or target) based on the "sourceModel"
     * signal.
     *
     * @return a pre-existing instance (not null)
     */
    LoadedCGModel selectCgm() {
        LoadedCGModel cgm;
        if (signals.test(sourceModelSignalName)
                && Maud.model.source.isLoaded()) {
            cgm = Maud.model.source;
        } else {
            cgm = Maud.model.target;
        }

        assert cgm != null;
        return cgm;
    }

    /**
     * Handle a "select spatialChild" action with arguments.
     *
     * @param argument action argument (not null)
     */
    void selectSpatialChild(String argument) {
        String[] words = argument.split(" ");
        String firstWord = words[0];
        assert firstWord.startsWith("#") : firstWord;
        String numberText = firstWord.substring(1);
        int number = Integer.parseInt(numberText);
        Maud.model.target.spatial.selectChild(number - 1);
    }

    /**
     * Handle a "select tool" action.
     *
     * @param toolName which tool to select (not null)
     * @return true if the action is handled, otherwise false
     */
    boolean selectTool(String toolName) {
        WindowController controller = tools.getTool(toolName);
        if (controller == null) {
            return false;
        } else {
            controller.select();
            return true;
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

        Maud.model.target.loadModelNamed("Elephant");
    }

    /**
     * Callback to update this state prior to rendering. (Invoked once per
     * render pass.)
     *
     * @param tpf time interval between render passes (in seconds, &ge;0)
     */
    @Override
    public void update(float tpf) {
        super.update(tpf);

        if (!tools.getTool("camera").isInitialized()) {
            return;
        }

        tools.update();
        /*
         * Update animations even if the animation tool is disabled.
         */
        if (Maud.model.source.animation.isMoving()) {
            updateTrackTime(Maud.model.source, tpf);
        }
        if (Maud.model.source.isLoaded()) {
            Maud.model.source.view.updatePose();
        }
        if (Maud.model.target.animation.isMoving()) {
            updateTrackTime(Maud.model.target, tpf);
        } else if (Maud.model.target.animation.isMappedPose()) {
            Maud.model.target.pose.setToAnimation();
        }
        Maud.model.target.view.updatePose();
        /*
         * Rotate one of the views' CG models around its Y-axis.
         */
        LoadedCGModel cgmToRotate = selectCgm();
        if (signals.test(modelCCWSignalName)) {
            cgmToRotate.transform.rotateY(tpf);
        }
        if (signals.test(modelCWSignalName)) {
            cgmToRotate.transform.rotateY(-tpf);
        }
        Maud.model.source.view.updateTransform();
        Maud.model.target.view.updateTransform();
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
    private void updateTrackTime(LoadedCGModel loadedCgm, float tpf) {
        assert loadedCgm.animation.isMoving();

        float speed = loadedCgm.animation.getSpeed();
        float time = loadedCgm.animation.getTime();
        time += speed * tpf;

        boolean cont = loadedCgm.animation.willContinue();
        boolean reverse = loadedCgm.animation.willReverse();
        float duration = loadedCgm.animation.getDuration();
        if (cont && !reverse) {
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
