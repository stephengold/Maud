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
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.system.JmeVersion;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.controls.CheckBoxStateChangedEvent;
import de.lessvoid.nifty.controls.RadioButtonStateChangedEvent;
import de.lessvoid.nifty.controls.SliderChangedEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.debug.DebugVersion;
import jme3utilities.math.MyMath;
import jme3utilities.nifty.GuiScreenController;
import jme3utilities.nifty.LibraryVersion;
import jme3utilities.nifty.WindowController;
import jme3utilities.ui.InputMode;
import jme3utilities.ui.UiVersion;

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
     * level separator in menu action strings
     */
    final private static String menuSeparator = " -> ";
    /**
     * name of signal that rotates the model counter-clockwise around +Y
     */
    final private static String modelCCWSignalName = "modelLeft";
    /**
     * name of signal that rotates the model clockwise around +Y
     */
    final private static String modelCWSignalName = "modelRight";
    // *************************************************************************
    // fields

    /**
     * input mode for the "3D View" screen
     */
    final DddMode inputMode = new DddMode();
    /*
     * controllers for tool windows
     */
    final public AnimationTool animation = new AnimationTool(this);
    final AxesTool axes = new AxesTool(this);
    final public BoneTool bone = new BoneTool(this);
    final BoneAngleTool boneAngle = new BoneAngleTool(this);
    final BoneOffsetTool boneOffset = new BoneOffsetTool(this);
    final BoneScaleTool boneScale = new BoneScaleTool(this);
    final CameraTool camera = new CameraTool(this);
    final CursorTool cursor = new CursorTool(this);
    final public ModelTool model = new ModelTool(this);
    final RenderTool render = new RenderTool(this);
    final public ShadowModeTool shadowMode = new ShadowModeTool(this);
    final public SkeletonTool skeleton = new SkeletonTool(this);
    final public SpatialTool spatial = new SpatialTool(this);
    final SkyTool sky = new SkyTool(this);
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized, disabled display that will be enabled
     * during initialization.
     */
    DddGui() {
        super("3D View", "Interface/Nifty/huds/3DView.xml", true);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Handle a "load model file" action where the argument may be the name of a
     * folder/directory.
     *
     * @param filePath action argument (not null)
     */
    void loadModelFile(String filePath) {
        File file = new File(filePath);
        if (file.isDirectory()) {
            List<String> fileNames = listFileNames(filePath);
            String menuPrefix = DddMode.loadModelFilePrefix + filePath;
            if (!menuPrefix.endsWith("/")) {
                menuPrefix += "/";
            }
            showPopupMenu(menuPrefix, fileNames);

        } else if (file.canRead()) {
            Maud.model.loadModelFile(file);
        }
    }

    /**
     * Callback that Nifty invokes after a check box changes.
     *
     * @param boxId Nifty element id of the check box (not null)
     * @param event details of the event (not null)
     */
    @NiftyEventSubscriber(pattern = ".*CheckBox")
    public void onCheckBoxChanged(final String boxId,
            final CheckBoxStateChangedEvent event) {
        Validate.nonNull(boxId, "check box id");
        Validate.nonNull(event, "event");

        if (!hasStarted()) {
            return;
        }

        switch (boxId) {
            case "axesDepthTestCheckBox":
                axes.update();
                break;
            case "3DCursorCheckBox":
                cursor.update();
                break;
            case "shadowsCheckBox":
                render.update();
                break;
            case "skeletonCheckBox":
                skeleton.update();
                break;
            case "skyCheckBox":
                sky.update();
                break;
        }
    }

    /**
     * Callback that Nifty invokes after a radio button changes.
     *
     * @param buttonId Nifty element id of the radio button (not null)
     * @param event details of the event (not null)
     */
    @NiftyEventSubscriber(pattern = ".*RadioButton")
    public void onRadioButtonChanged(final String buttonId,
            final RadioButtonStateChangedEvent event) {
        Validate.nonNull(buttonId, "button id");
        Validate.nonNull(event, "event");

        if (!hasStarted()) {
            return;
        }

        switch (buttonId) {
            case "boneAxesRadioButton":
                axes.setMode("bone");
                break;
            case "modelAxesRadioButton":
                axes.setMode("model");
                break;
            case "worldAxesRadioButton":
                axes.setMode("world");
                break;
            case "hideAxesRadioButton":
                axes.setMode("none");
                break;

            case "flyingRadioButton":
                camera.setMode("fly");
                break;
            case "orbitingRadioButton":
                camera.setMode("orbit");
                break;

            case "shadowOffRadioButton":
                Maud.model.setMode(RenderQueue.ShadowMode.Off);
                break;
            case "shadowCastRadioButton":
                Maud.model.setMode(RenderQueue.ShadowMode.Cast);
                break;
            case "shadowReceiveRadioButton":
                Maud.model.setMode(RenderQueue.ShadowMode.Receive);
                break;
            case "shadowCastAndReceiveRadioButton":
                Maud.model.setMode(RenderQueue.ShadowMode.CastAndReceive);
                break;
            case "shadowInheritRadioButton":
                Maud.model.setMode(RenderQueue.ShadowMode.Inherit);
                break;

            default:
                logger.log(Level.WARNING, "unknown radio button with id={0}",
                        MyString.quote(buttonId));
        }
    }

    /**
     * Callback that Nifty invokes after a slider changes.
     *
     * @param sliderId Nifty element id of the slider (not null)
     * @param event details of the event (not null, ignored)
     */
    @NiftyEventSubscriber(pattern = ".*Slider")
    public void onSliderChanged(final String sliderId,
            final SliderChangedEvent event) {
        Validate.nonNull(sliderId, "slider id");
        Validate.nonNull(event, "event");

        if (!hasStarted()) {
            return;
        }

        switch (sliderId) {
            case "speedSlider":
            case "timeSlider":
                animation.update();
                break;
            case "axesLineWidthSlider":
            case "axesLengthSlider":
                axes.update();
                break;
            case "cursorRSlider":
            case "cursorGSlider":
            case "cursorBSlider":
                cursor.update();
                break;
            case "skeletonLineWidthSlider":
            case "skeletonPointSizeSlider":
            case "skeRSlider":
            case "skeGSlider":
            case "skeBSlider":
                skeleton.update();
        }
    }

    /**
     * Handle an "open menu" action for this screen.
     *
     * @param menuPath menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    boolean openMenu(String menuPath) {
        int separatorBegin = menuPath.indexOf(menuSeparator);
        boolean handled;
        if (separatorBegin == -1) {
            handled = menuBar(menuPath);
        } else {
            int separatorEnd = separatorBegin + menuSeparator.length();
            String menuName = menuPath.substring(0, separatorBegin);
            String remainder = menuPath.substring(separatorEnd);
            handled = menu(menuName, remainder);
        }

        return handled;
    }

    /**
     * Handle a "rename bone" action with no argument.
     */
    void renameBone() {
        if (Maud.model.bone.isBoneSelected()) {
            closeAllPopups();
            String oldName = Maud.model.bone.getName();
            showTextEntryDialog("Enter new name for bone:", oldName, "Rename",
                    DddMode.renameBonePrefix);
        }
    }

    /**
     * Handle a "select bone" action.
     *
     * @param argument action argument (not null)
     */
    void selectBone(String argument) {
        if (Maud.model.hasBone(argument)) {
            Maud.model.bone.select(argument);

        } else {
            /*
             * Treat the argument as a bone-name prefix.
             */
            List<String> boneNames = Maud.model.listBoneNames(argument);
            MyString.reduce(boneNames, 20);
            Collections.sort(boneNames);
            showPopupMenu(DddMode.selectBonePrefix, boneNames);
        }
    }

    /**
     * Handle a "select boneChild" action with no argument.
     */
    void selectBoneChild() {
        if (Maud.model.bone.isBoneSelected()) {
            int numChildren = Maud.model.bone.countChildren();
            if (numChildren == 1) {
                Maud.model.bone.selectChild(0);
            } else if (numChildren > 1) {
                List<String> choices = Maud.model.bone.listChildNames();
                showPopupMenu(DddMode.selectBonePrefix, choices);
            }
        }
    }

    /**
     * Handle a "select boneChild" action.
     *
     * @param argument action argument (not null)
     */
    void selectBoneChild(String argument) {
        if (argument.startsWith("!")) {
            String name = argument.substring(1);
            Maud.model.bone.select(name);
        } else {
            List<String> names = Maud.model.listChildBoneNames(argument);
            List<String> items = new ArrayList<>(names.size() + 1);
            items.add("!" + argument);
            for (String name : names) {
                if (Maud.model.isLeafBone(name)) {
                    items.add("!" + name);
                } else {
                    items.add(name);
                }
            }
            showPopupMenu(DddMode.selectBoneChildPrefix, items);
        }
    }

    /**
     * Handle a "select spatialChild" action with no argument.
     */
    void selectSpatialChild() {
        int numChildren = Maud.model.spatial.countChildren();
        if (numChildren == 1) {
            Maud.model.spatial.selectChild(0);

        } else if (numChildren > 1) {
            List<String> choices = new ArrayList<>(numChildren);
            for (int i = 0; i < numChildren; i++) {
                String choice = String.format("#%d", i + 1);
                String name = Maud.model.spatial.getChildName(i);
                if (name != null) {
                    choice += " " + MyString.quote(name);
                }
                choices.add(choice);
            }
            showPopupMenu(DddMode.selectSpatialChildPrefix, choices);
        }
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
        Maud.model.spatial.selectChild(number - 1);
    }

    /**
     * Handle a "select spatialParent" action.
     */
    void selectSpatialParent() {
        Maud.model.spatial.selectParent();
    }

    /**
     * Handle a "select tool" action.
     *
     * @param toolName which tool to select (not null)
     * @return true if the action is handled, otherwise false
     */
    boolean selectTool(String toolName) {
        WindowController controller = null;
        switch (toolName) {
            case "animation":
                controller = animation;
                break;
            case "axes":
                controller = axes;
                break;
            case "bone":
                controller = bone;
                break;
            case "boneAngle":
                controller = boneAngle;
                break;
            case "boneOffset":
                controller = boneOffset;
                break;
            case "boneScale":
                controller = boneScale;
                break;
            case "camera":
                controller = camera;
                break;
            case "cursor":
                controller = cursor;
                break;
            case "model":
                controller = model;
                break;
            case "render":
                controller = render;
                break;
            case "shadowMode":
                controller = shadowMode;
                break;
            case "skeleton":
                controller = skeleton;
                break;
            case "spatial":
                controller = spatial;
                break;
            case "sky":
                controller = sky;
                break;
        }
        if (controller == null) {
            return false;
        } else {
            controller.select();
            return true;
        }
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
     * @param application application which owns this screen (not null)
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

        cam.setLocation(new Vector3f(-2.4f, 1f, 1.6f));
        cam.setRotation(new Quaternion(0.006f, 0.86884f, -0.01049f, 0.49493f));

        signals.add(modelCCWSignalName);
        signals.add(modelCWSignalName);

        Maud.model.loadModelNamed("Elephant");
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
        /*
         * Update animation even if the animation tool is disabled.
         */
        float duration = Maud.model.animation.getDuration();
        float speed = Maud.model.animation.getSpeed();
        if (duration != 0f && speed != 0f) {
            float time = Maud.model.animation.getTime();
            time += speed * tpf;
            time = MyMath.modulo(time, duration);
            Maud.model.animation.setTime(time);
        }
        /*
         * Rotate the view's CG model around the Y-axis.
         */
        if (signals.test(modelCCWSignalName)) {
            Maud.viewState.rotateY(tpf);
        }
        if (signals.test(modelCWSignalName)) {
            Maud.viewState.rotateY(-tpf);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Handle an "About Maud" action.
     */
    private void aboutMaud() {
        String niftyVersion = nifty.getVersion();
        String text = "Maud, by Stephen Gold\n\nYou are c"
                + "urrently using Maud, a jMonkeyEngine application for edit"
                + "ing animated models.\n\nThe version you are using includes "
                + "the following libraries:";
        text += String.format("\n   jme3-core version %s",
                MyString.quote(JmeVersion.FULL_NAME));
        text += String.format("\n   nifty version %s",
                MyString.quote(niftyVersion));
        text += String.format("\n   SkyControl version %s",
                MyString.quote(Misc.getVersionShort()));
        text += String.format("\n   jme3-utilities-debug version %s",
                MyString.quote(DebugVersion.getVersionShort()));
        text += String.format("\n   jme3-utilities-ui version %s",
                MyString.quote(UiVersion.getVersionShort()));
        text += String.format("\n   jme3-utilities-nifty version %s\n\n",
                MyString.quote(LibraryVersion.getVersionShort()));
        closeAllPopups();
        showInfoDialog("About Maud", text);
    }

    /**
     * Enumerate items for the 3D View menu.
     *
     * @return a new list
     */
    private List<String> list3DViewMenuItems() {
        List<String> items = new ArrayList<>(9);
        items.add("Axes");
        items.add("Camera");
        items.add("Cursor");
        items.add("Physics");
        items.add("Platform");
        items.add("Render");
        items.add("Skeleton");
        items.add("Sky");

        return items;
    }

    /**
     * Enumerate items for the Animation menu.
     *
     * @return a new list
     */
    private List<String> listAnimationMenuItems() {
        List<String> items = new ArrayList<>(10);
        items.add("Tool");
        items.add("Load");
        items.add("New from copy");
        items.add("New from BVH");
        items.add("New from pose");
        if (!Maud.model.animation.isBindPoseLoaded()) {
            items.add("Duration");
            items.add("Tweening");
            items.add("Rename");
            items.add("Delete");
        }

        return items;
    }

    /**
     * Enumerate items for the Bone menu.
     *
     * @return a new list
     */
    private List<String> listBoneMenuItems() {
        List<String> items = new ArrayList<>(10);
        items.add("Tool");
        items.add("Angles");
        items.add("Offset");
        items.add("Scale");
        items.add("Select by parent");
        items.add("Select by name");
        items.add("Select by pointing");
        items.add("Describe skeleton");
        if (Maud.model.bone.isBoneSelected()) {
            items.add("Attach prop");
            items.add("Rename");
        }

        return items;
    }

    /**
     * Enumerate items for the CGModel menu.
     *
     * @return a new list
     */
    private List<String> listCGModelMenuItems() {
        List<String> items = new ArrayList<>(6);
        items.add("Tool");
        items.add("Load named asset");
        items.add("Load asset path");
        items.add("Load from file");
        items.add("Save as asset");
        items.add("Save as file");

        return items;
    }

    /**
     * Enumerate the files in a directory/folder.
     *
     * @param path file path (not null)
     * @return a new list, or null if path is not a directory/folder
     */
    private List<String> listFileNames(String path) {
        assert path != null;

        File file = new File(path);
        File[] files = file.listFiles();
        if (files == null) {
            return null;
        }

        List<String> names = new ArrayList<>(files.length + 1);
        if (file.getParentFile() != null) {
            names.add("..");
        }
        for (int i = 0; i < files.length; i++) {
            String name = files[i].getName();
            names.add(name);
        }

        return names;
    }

    /**
     * Enumerate items for the Help menu.
     *
     * @return a new list
     */
    private List<String> listHelpMenuItems() {
        List<String> items = new ArrayList<>(6);
        items.add("About Maud");
        items.add("License");
        items.add("Wiki");
        items.add("Javadoc");
        items.add("Source");
        items.add("JME3 homepage");

        return items;
    }

    /**
     * Enumerate items for the Keyframe menu.
     *
     * @return a new list
     */
    private List<String> listKeyframeMenuItems() {
        List<String> items = new ArrayList<>(10);
        items.add("Describe");
        items.add("Select by time");
        items.add("Select first");
        items.add("Select previous");
        items.add("Select next");
        items.add("Select last");
        items.add("Move");
        items.add("Copy");
        items.add("New from pose");
        items.add("Delete");

        return items;
    }

    /**
     * Enumerate items for the Physics menu.
     *
     * @return a new list
     */
    private List<String> listPhysicsMenuItems() {
        List<String> items = new ArrayList<>(4);
        items.add("Describe");
        items.add("Add");
        items.add("Mass");
        items.add("Remove");

        return items;
    }

    /**
     * Enumerate items for the Settings menu.
     *
     * @return a new list
     */
    private List<String> listSettingsMenuItems() {
        List<String> items = new ArrayList<>(3);
        items.add("Initial model");
        items.add("Hotkeys");
        items.add("Locale");

        return items;
    }

    /**
     * Enumerate items for the Spatial menu.
     *
     * @return a new list
     */
    private List<String> listSpatialMenuItems() {
        List<String> items = new ArrayList<>(4);
        items.add("Tool");
        items.add("Select by parent");
        items.add("Select by name");
        items.add("Material");

        return items;
    }

    /**
     * Handle a menu action.
     *
     * @param menuName name of the menu (not null)
     * @param remainder not-yet-parsed portion of the action string (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menu(String menuName, String remainder) {
        assert menuName != null;
        assert remainder != null;

        boolean handled = false;
        switch (menuName) {
            case "3DView":
                handled = menu3DView(remainder);
                break;
            case "Animation":
                handled = menuAnimation(remainder);
                break;
            case "Bone":
                handled = menuBone(remainder);
                break;
            case "CGModel":
                handled = menuCGModel(remainder);
                break;
            case "Help":
                handled = menuHelp(remainder);
                break;
            case "Keyframe":
                //handled = menuKeyframe(remainder);
                break;
            case "Physics":
                //handled = menuPhysics(remainder);
                break;
            case "Settings":
                handled = menuSettings(remainder);
                break;
            case "Spatial":
                handled = menuSpatial(remainder);
                break;
        }

        return handled;
    }

    /**
     * Handle actions from the 3D View menu.
     *
     * @param remainder not-yet-parsed portion of the action string (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menu3DView(String remainder) {
        assert remainder != null;

        boolean handled = false;
        switch (remainder) {
            case "Axes":
                axes.select();
                handled = true;
                break;
            case "Camera":
                camera.select();
                handled = true;
                break;
            case "Cursor":
                cursor.select();
                handled = true;
                break;
            case "Physics":
            case "Platform":
                break;
            case "Render":
                render.select();
                handled = true;
                break;
            case "Skeleton":
                skeleton.select();
                handled = true;
                break;
            case "Sky":
                sky.select();
                handled = true;
                break;
        }

        return handled;
    }

    /**
     * Handle actions from the Animation menu.
     *
     * @param remainder not-yet-parsed portion of the action string (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuAnimation(String remainder) {
        assert remainder != null;

        boolean handled = false;
        switch (remainder) {
            case "Delete":
                animation.delete();
                handled = true;
                break;

            case "Duration":
                break;

            case "Load":
                Collection<String> animationNames;
                animationNames = Maud.model.listAnimationNames();
                showPopupMenu(DddMode.loadAnimationPrefix, animationNames);
                handled = true;
                break;

            case "New from BVH":
                break;

            case "New from copy":
                String fromName = Maud.model.animation.getName();
                String toName = String.format("Copy of %s", fromName);
                closeAllPopups();
                showTextEntryDialog("Enter name for new animation:",
                        toName, "Copy", DddMode.copyAnimationPrefix);
                handled = true;
                break;

            case "New from pose":
                String newName = "new-animation"; // TODO
                Maud.model.addPoseAnimation(newName);
                Maud.model.animation.load(newName);
                handled = true;
                break;

            case "Rename":
                closeAllPopups();
                String oldName = Maud.model.animation.getName();
                showTextEntryDialog("Enter new name for animation:",
                        oldName, "Rename", DddMode.renameAnimationPrefix);
                handled = true;
                break;

            case "Tool":
                animation.select();
                handled = true;
                break;

            case "Tweening":
        }
        return handled;
    }

    /**
     * Handle an action from the menu bar.
     *
     * @param menuName name of the menu to open (not null)
     * @return true if handled, otherwise false
     */
    private boolean menuBar(String menuName) {
        assert menuName != null;
        /**
         * Dynamically generate the menu's item list.
         */
        List<String> menuItems;
        switch (menuName) {
            case "3DView":
                menuItems = list3DViewMenuItems();
                break;
            case "Animation":
                menuItems = listAnimationMenuItems();
                break;
            case "Bone":
                menuItems = listBoneMenuItems();
                break;
            case "CGModel":
                menuItems = listCGModelMenuItems();
                break;
            case "Help":
                menuItems = listHelpMenuItems();
                break;
            case "Keyframe":
                menuItems = listKeyframeMenuItems();
                break;
            case "Physics":
                menuItems = listPhysicsMenuItems();
                break;
            case "Settings":
                menuItems = listSettingsMenuItems();
                break;
            case "Spatial":
                menuItems = listSpatialMenuItems();
                break;
            default:
                return false;
        }
        if (menuItems.isEmpty()) {
            logger.log(Level.WARNING, "no items for the {0} menu",
                    MyString.quote(menuName));
        } else {
            String actionPrefix = DddMode.openMenuPrefix + menuName
                    + menuSeparator;
            showPopupMenu(actionPrefix, menuItems);
        }
        return true;
    }

    /**
     * Handle actions from the Bone menu.
     *
     * @param remainder not-yet-parsed portion of the action string (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuBone(String remainder) {
        assert remainder != null;
        boolean handled = false;
        switch (remainder) {
            case "Angles":
                boneAngle.select();
                handled = true;
                break;
            case "Attach prop":
            case "Describe skeleton":
                break;
            case "Offset":
                boneOffset.select();
                handled = true;
                break;
            case "Rename":
                renameBone();
                handled = true;
                break;
            case "Scale":
                boneScale.select();
                handled = true;
                break;
            case "Select by name":
                selectBoneByName();
                handled = true;
                break;
            case "Select by parent":
                selectBoneByParent();
                handled = true;
                break;
            case "Select by pointing":
                break;
            case "Tool":
                bone.select();
                handled = true;
        }
        return handled;
    }

    /**
     * Handle actions from the CGModel menu.
     *
     * @param remainder not-yet-parsed portion of the action string (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuCGModel(String remainder) {
        assert remainder != null;

        boolean handled = false;
        switch (remainder) {
            case "Load named asset":
                String[] modelNames = {
                    "Elephant", "Jaime", "Ninja", "Oto", "Sinbad"
                };
                showPopupMenu(DddMode.loadModelNamedPrefix, modelNames);
                handled = true;
                break;

            case "Load asset path":
                String basePath = Maud.model.getAssetPath();
                String extension = Maud.model.getExtension();
                String assetPath = String.format("%s.%s", basePath, extension);
                closeAllPopups();
                showTextEntryDialog("Enter base asset path for model:",
                        assetPath, "Load", DddMode.loadModelAssetPrefix);
                handled = true;
                break;

            case "Load from file":
                List<String> fileNames = listFileNames("/");
                showPopupMenu(DddMode.loadModelFilePrefix + "/", fileNames);
                handled = true;
                break;

            case "Revert":
                // TODO
                break;

            case "Save as asset":
                String baseAssetPath = Maud.model.getAssetPath();
                closeAllPopups();
                showTextEntryDialog("Enter base asset path for model:",
                        baseAssetPath, "Save", DddMode.saveModelAssetPrefix);
                handled = true;
                break;

            case "Save as file":
                String baseFilePath = Maud.model.getFilePath();
                closeAllPopups();
                showTextEntryDialog("Enter base file path for model:",
                        baseFilePath, "Save", DddMode.saveModelFilePrefix);
                handled = true;
                break;

            case "Tool":
                model.select();
                handled = true;
                break;
        }
        return handled;
    }

    /**
     * Handle actions from the Help menu.
     *
     * @param remainder not-yet-parsed portion of the action string (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuHelp(String remainder) {
        assert remainder != null;
        boolean handled = false;
        switch (remainder) {
            case "About Maud":
                aboutMaud();
                handled = true;
                break;

            case "JME3 homepage":
                Util.browseWeb("http://jmonkeyengine.org/");
                handled = true;
                break;

            case "License":
                File licenseFile = new File("LICENSE");
                Scanner scanner = null;
                try {
                    scanner = new Scanner(licenseFile).useDelimiter("\\Z");
                } catch (FileNotFoundException e) {
                }
                String text2;
                if (scanner == null) {
                    text2 = "Your software license is missing!";
                } else {
                    String contents = scanner.next();
                    scanner.close();
                    text2 = String.format(
                            "Here's your software license for Maud:\n%s\n",
                            contents);
                }
                closeAllPopups();
                showInfoDialog("License information", text2);
                handled = true;
                break;

            case "Source":
                Util.browseWeb("https://github.com/stephengold/Maud");
                handled = true;
                break;
        }

        return handled;
    }

    /**
     * Handle actions from the Settings menu.
     *
     * @param remainder not-yet-parsed portion of the action string (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuSettings(String remainder) {
        assert remainder != null;
        boolean handled = false;
        switch (remainder) {
            case "Hotkeys":
                closeAllPopups();
                Maud.bindScreen.activate(inputMode);
                handled = true;
        }

        return handled;
    }

    /**
     * Handle actions from the Spatial menu.
     *
     * @param remainder not-yet-parsed portion of the action string (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuSpatial(String remainder) {
        assert remainder != null;
        boolean handled = false;
        switch (remainder) {
            case "Tool":
                spatial.select();
                handled = true;
        }

        return handled;
    }

    /**
     * Select a bone by name, using submenus.
     */
    private void selectBoneByName() {
        List<String> boneNames = Maud.model.listBoneNames();
        MyString.reduce(boneNames, 20);
        Collections.sort(boneNames);
        showPopupMenu(DddMode.selectBonePrefix, boneNames);
    }

    /**
     * Select a bone by parent, using submenus.
     */
    private void selectBoneByParent() {
        List<String> boneNames = Maud.model.listRootBoneNames();
        showPopupMenu(DddMode.selectBoneChildPrefix, boneNames);
    }
}
