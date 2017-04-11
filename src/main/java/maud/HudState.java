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
 * Stephen Gold's name may not be used to endorse or promote products
 derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL STEPHEN GOLD BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package maud;

import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.input.controls.ActionListener;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.controls.RadioButtonStateChangedEvent;
import de.lessvoid.nifty.controls.Slider;
import de.lessvoid.nifty.controls.SliderChangedEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MySkeleton;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.debug.AxesControl;
import jme3utilities.debug.SkeletonDebugControl;
import jme3utilities.nifty.GuiScreenController;

/**
 * Controller for the HUD for the Maud application.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class HudState
        extends GuiScreenController
        implements ActionListener {
    // *************************************************************************
    // constants and loggers

    /**
     * number of coordinate axes
     */
    final private static int numAxes = 3;
    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            HudState.class.getName());
    /**
     * names of the coordinate axes
     */
    final private static String[] axisNames = {"x", "y", "z"};
    /**
     * names of loadable models in the jme3-testdata library
     */
    final private static String[] modelNames = {
        "Elephant", "Jaime", "Ninja", "Oto", "Sinbad"
    };
    /**
     * action prefix for the "rename animation" dialog
     */
    final private static String animationDialogPrefix = "rename animation ";
    /**
     * action prefix for the "load animation" popup menu
     */
    final private static String animationMenuPrefix = "load animation ";
    /**
     * dummy animation name used in menus and statuses (but never stored in
     * channel!) to indicate bind pose, that is, no animation loaded
     */
    final static String bindPoseName = "( bind pose )";
    /*
     * action prefix for the "rename bone" dialog
     */
    final private static String boneDialogPrefix = "rename bone ";
    /**
     * action prefix for the "select bone" popup menu
     */
    final private static String boneMenuPrefix = "select bone ";
    /**
     * action prefix for the "select keyframe" popup menu
     */
    final private static String keyframeMenuPrefix = "select keyframe ";
    /**
     * action prefix for the "load model" popup menu
     */
    final private static String modelMenuPrefix = "load model ";
    /**
     * dummy bone name used in menus and statuses to indicate no bone selected
     * (not null)
     */
    final static String noBone = "( no bone )";
    // *************************************************************************
    // fields

    /**
     * When true, bone controls should be enabled, along with the user-control
     * flags of all bones in the model's skeleton.
     */
    private boolean boneControlsEnabledFlag = false;
    /**
     * references to the bone-angle sliders, set by
     * {@link #initialize(com.jme3.app.state.AppStateManager, com.jme3.app.Application)}
     */
    final private Slider baSliders[] = new Slider[numAxes];
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized, disabled display that will be enabled
     * during initialization.
     */
    public HudState() {
        super("maud", "Interface/Nifty/huds/maud.xml", true);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Callback which Nifty invokes after the user selects a radio button.
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
            case "flyingRadioButton":
                Maud.cameraState.setOrbitMode(false);
                return;

            case "orbitingRadioButton":
                Maud.cameraState.setOrbitMode(true);
                return;
        }
        logger.log(Level.WARNING, "unknown radio button with id={0}",
                MyString.quote(buttonId));
    }

    /**
     * Callback which Nifty invokes after the user moves a slider.
     *
     * @param sliderId Nifty element id of the slider (not null)
     * @param event details of the event (not null, ignored)
     */
    @NiftyEventSubscriber(pattern = ".*Slider")
    public void onSliderChanged(final String sliderId,
            final SliderChangedEvent event) {
        Validate.nonNull(sliderId, "slider id");
        Validate.nonNull(event, "event");

        switch (sliderId) {
            case "xSlider":
            case "ySlider":
            case "zSlider":
                baSliderChanged();
        }
    }

    /**
     * Update the 3 bone-angle sliders to match the selected bone. If no bone is
     * selected, zero them.
     *
     * @param storeAngles array to store new angles (length=3 if not null)
     */
    void updateBaSlidersToBone(float[] storeAngles) {
        if (storeAngles == null) {
            storeAngles = new float[numAxes];
        }
        assert storeAngles.length == numAxes : storeAngles.length;

        if (Maud.modelState.isBoneSelected()) {
            Maud.modelState.boneAngles(storeAngles);
            for (int iAxis = 0; iAxis < numAxes; iAxis++) {
                baSliders[iAxis].setValue(storeAngles[iAxis]);
            }

        } else {
            /*
             * No bone selected: zero the sliders.
             */
            for (int iAxis = 0; iAxis < numAxes; iAxis++) {
                baSliders[iAxis].setValue(0f);
                storeAngles[iAxis] = 0f;
            }
        }
    }
    // *************************************************************************
    // ActionListener methods

    /**
     * Process a GUI action.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param tpf time interval between render passes (in seconds, &ge;0)
     */
    @Override
    public void onAction(String actionString, boolean ongoing, float tpf) {
        logger.log(Level.INFO, "Got action {0}", MyString.quote(actionString));

        if (ongoing) {
            switch (actionString) {
                case "add pose":
                    Maud.modelState.addPoseAnimation("new-animation");
                    loadAnimation("new-animation");
                    return;

                case "delete animation":
                    Maud.modelState.deleteAnimation();
                    loadAnimation(bindPoseName);
                    return;

                case "load animation":
                    Collection<String> animationNames = Maud.modelState.listAnimationNames();
                    showPopup(animationMenuPrefix, animationNames);
                    return;

                case "load model":
                    showPopup(modelMenuPrefix, modelNames);
                    return;

                case "rename animation":
                    String oldAnimName = Maud.modelState.getAnimationName();
                    showDialog("Enter new name for animation:", oldAnimName,
                            "Rename", animationDialogPrefix);
                    return;

                case "rename bone":
                    String oldBoneName = Maud.modelState.getBoneName();
                    showDialog("Enter new name for bone:", oldBoneName,
                            "Rename", boneDialogPrefix);
                    return;

                case "reset bone":
                    resetBone();
                    return;

                case "select bone":
                    List<String> boneNames = Maud.modelState.listBoneNames();
                    MyString.reduce(boneNames, 20);
                    Collections.sort(boneNames);
                    showPopup(boneMenuPrefix, boneNames);
                    return;

                case "select keyframe":
                    List<String> options = Maud.modelState.listKeyframes();
                    if (options != null) {
                        MyString.reduce(options, 20);
                        showPopup(keyframeMenuPrefix, options);
                    }
                    return;

                case "write model":
                    String assetPath = Maud.modelState.getAssetPath();
                    Maud.modelState.writeModelToAsset(assetPath);
                    return;
            }
            if (actionString.startsWith(animationDialogPrefix)) {
                int namePos = animationDialogPrefix.length();
                String newName = actionString.substring(namePos);
                boolean success = Maud.modelState.renameAnimation(newName);
                if (success) {
                    loadAnimation(newName);
                }
                return;

            } else if (actionString.startsWith(animationMenuPrefix)) {
                int namePos = animationMenuPrefix.length();
                String name = actionString.substring(namePos);
                loadAnimation(name);
                return;

            } else if (actionString.startsWith(boneDialogPrefix)) {
                int namePos = boneDialogPrefix.length();
                String newName = actionString.substring(namePos);
                boolean success = Maud.modelState.renameBone(newName);
                if (success) {
                    selectBone(newName);
                }
                return;

            } else if (actionString.startsWith(boneMenuPrefix)) {
                int namePos = boneMenuPrefix.length();
                String name = actionString.substring(namePos);

                if (!Maud.modelState.hasBone(name)) {
                    List<String> boneNames = Maud.modelState.listBoneNames(
                            name);
                    MyString.reduce(boneNames, 20);
                    Collections.sort(boneNames);
                    showPopup(boneMenuPrefix, boneNames);
                    return;
                }

                selectBone(name);
                return;

            } else if (actionString.startsWith(keyframeMenuPrefix)) {
                int namePos = keyframeMenuPrefix.length();
                String name = actionString.substring(namePos);

                List<String> options = Maud.modelState.listKeyframes();
                if (!options.contains(name)) {
                    MyString.matchPrefix(options, name);
                    MyString.reduce(options, 20);
                    showPopup(keyframeMenuPrefix, options);
                    return;
                }

                Maud.modelState.selectKeyframe(name);
                updateBaSlidersToBone(null); // ?
                return;

            } else if (actionString.startsWith(modelMenuPrefix)) {
                int namePos = modelMenuPrefix.length();
                String name = actionString.substring(namePos);
                loadModel(name);
                return;
            }
        }
        /*
         * Forward unhandled action to the application.
         */
        guiApplication.onAction(actionString, ongoing, tpf);
    }
    // *************************************************************************
    // GuiScreenController methods

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

        setListener(this);
        super.initialize(stateManager, application);

        for (int iAxis = 0; iAxis < numAxes; iAxis++) {
            String axisName = axisNames[iAxis];
            Slider slider = getSlider(axisName);
            baSliders[iAxis] = getSlider(axisName);
            slider.enable();
        }

        loadModel("Elephant");
    }

    /**
     * Callback to update this state prior to rendering. (Invoked once per
     * render pass.)
     *
     * @param tpf time interval between render passes (in seconds, &ge;0)
     */
    @Override
    public void update(float tpf) {
        /*
         * Camera mode
         */
        if (Maud.cameraState.isOrbitMode()) {
            setRadioButton("orbitingRadioButton");
        } else {
            setRadioButton("flyingRadioButton");
        }
        boolean enable = isChecked("3DCursor");
        Maud.cameraState.cursorSetEnabled(enable);
        /*
         * Model status
         */
        String status = Maud.modelState.getName();
        setStatusText("modelStatus", status);
        /*
         * Animation rename and delete buttons
         */
        if (Maud.modelState.isBindPoseSelected()) {
            setButtonLabel("renameAnimButton", "");
            setButtonLabel("deleteButton", "");
        } else {
            setButtonLabel("renameAnimButton", "Rename this animation");
            setButtonLabel("deleteButton", "Delete this animation");
        }
        /*
         * Track time
         */
        if (Maud.modelState.isBindPoseSelected()) {
            status = "n/a";
        } else {
            float trackTime = Maud.modelState.getTime();
            float duration = Maud.modelState.getDuration();
            status = String.format("%.3f / %.3f", trackTime, duration);
        }
        setStatusText("trackTime", status);
        /*
         * Animation speed slider and status
         */
        float speed = updateSlider("speed", "x");
        Maud.modelState.setSpeed(speed);
        /*
         * Select keyframe button
         */
        if (Maud.modelState.hasTrack()) {
            setButtonLabel("keyframeButton", "Select keyframe");
        } else {
            setButtonLabel("keyframeButton", "");
        }
        /*
         * Bone select and rename
         */
        if (Maud.modelState.isBoneSelected()) {
            setButtonLabel("selectBoneButton", "Select another bone");
            setButtonLabel("renameBoneButton", "Rename this bone");
        } else {
            setButtonLabel("selectBoneButton", "Select a bone");
            setButtonLabel("renameBoneButton", "");
        }
        /*
         * Bone-angle sliders, status, and reset button
         */
        updateBaSliders();
        /*
         * Debugging aids
         */
        updateDebugAids();
        /*
         * Lighting options
         */
        enable = isChecked("shadows");
        Maud.dlsf.setEnabled(enable);
    }
    // *************************************************************************
    // private methods

    /**
     * React to user adjusting a bone-angle slider.
     */
    private void baSliderChanged() {
        if (!boneControlsEnabledFlag) {
            /*
             * Ignore slider change events when the bone-angle sliders
             * are disabled.
             */
            return;
        }

        float angles[] = new float[numAxes];
        for (int iAxis = 0; iAxis < numAxes; iAxis++) {
            angles[iAxis] = baSliders[iAxis].getValue();
        }

        Vector3f translation = new Vector3f(0f, 0f, 0f);
        Quaternion rotation = new Quaternion();
        rotation.fromAngles(angles);
        Vector3f scale = new Vector3f(1f, 1f, 1f);
        Maud.modelState.setUserTransforms(translation, rotation, scale);
    }

    /**
     * Disable the 3 bone-angle sliders, reset button, and user control.
     */
    private void disableBoneControls() {
        Maud.modelState.setUserControl(false);
        if (!boneControlsEnabledFlag) {
            return;
        }
        logger.log(Level.INFO, "Disable the bone controls.");
        /*
         * Disable the sliders and reset button.
         */
        for (Slider slider : baSliders) {
            slider.disable();
        }
        setButtonLabel("resetButton", "");
        /*
         * Update the status.
         */
        boneControlsEnabledFlag = false;
    }

    /**
     * Enable the 3 bone-angle sliders, reset button, and user control.
     */
    private void enableBoneControls() {
        Maud.modelState.setUserControl(true);
        if (boneControlsEnabledFlag) {
            return;
        }
        logger.log(Level.INFO, "Enable the bone-angle sliders.");
        /*
         * Enable the sliders.
         */
        for (Slider slider : baSliders) {
            slider.enable();
        }
        setButtonLabel("resetButton", "Reset bone");
        /*
         * Update the status.
         */
        boneControlsEnabledFlag = true;
    }

    /**
     * Load the named pose or animation.
     *
     * @param name (not null)
     */
    private void loadAnimation(String name) {
        assert name != null;

        setStatusText("animationStatus", name);
        if (name.equals(bindPoseName)) {
            /*
             * Load bind pose.
             */
            Maud.modelState.loadBindPose();
            updateBaSlidersToBone(null);

        } else {
            if (Maud.modelState.getDuration(name) == 0f) {
                /*
                 * The animation consists of a single pose:
                 * Set speed to zero and load the pose under user control.
                 */
                Maud.modelState.loadAnimation(name, 0f);
                Maud.modelState.poseSkeleton();
                updateBaSlidersToBone(null);

            } else {
                /*
                 * Start the animation looping at normal speed.
                 */
                Maud.modelState.loadAnimation(name, 1f);
            }
        }
        String boneName = Maud.modelState.getBoneName();
        selectBone(boneName);
    }

    /**
     * Load the named model.
     *
     * @param name (not null)
     */
    private void loadModel(String name) {
        assert name != null;

        Maud.modelState.load(name);
        loadAnimation(bindPoseName);
        selectBone(noBone);
    }

    /**
     * Reset the selected bone.
     */
    private void resetBone() {
        if (!boneControlsEnabledFlag) {
            /*
             * Ignore reset events when the bone controls are disabled.
             */
            return;
        }

        for (int iAxis = 0; iAxis < numAxes; iAxis++) {
            baSliders[iAxis].setValue(0f);
        }
    }

    /**
     * Select the named bone or noBone.
     *
     * @param name (not null)
     */
    private void selectBone(String name) {
        Maud.modelState.selectBone(name);

        String status;
        if (Maud.modelState.hasTrack()) {
            status = String.format("+ %s", name); // bone with track
        } else {
            status = name;
        }
        setStatusText("boneStatus", status);

        String status2, status3, status4;
        Skeleton skeleton = Maud.modelState.getSkeleton();
        int numBones = skeleton.getBoneCount();
        if (Maud.modelState.isBoneSelected()) {
            Bone bone = Maud.modelState.getBone();

            Bone parent = bone.getParent();
            if (parent == null) {
                status2 = "a root bone";
            } else {
                String parentName = parent.getName();
                status2 = String.format("a child of %s",
                        MyString.quote(parentName));
            }

            List<Bone> children = bone.getChildren();
            int numChildren = children.size();
            if (numChildren > 1) {
                status3 = String.format("with %d children", numChildren);
            } else if (numChildren == 1) {
                String childName = children.get(0).getName();
                status3 = String.format("the parent of %s",
                        MyString.quote(childName));
            } else {
                status3 = "with no children";
            }

            int boneIndex = skeleton.getBoneIndex(bone.getName());
            int numInfluenced = 0;
            for (Mesh mesh : Maud.modelState.listMeshes()) {
                numInfluenced += MySkeleton.numInfluenced(mesh, boneIndex);
            }
            status4 = String.format("#%d of %d; vertices = %d",
                    boneIndex + 1, numBones, numInfluenced);

        } else {
            status2 = String.format("total bones = %d", numBones);
            int numRootBones = MySkeleton.numRootBones(skeleton);
            status3 = String.format("root bones = %d", numRootBones);
            int numLeafBones = MySkeleton.numLeafBones(skeleton);
            status4 = String.format("leaf bones = %d", numLeafBones);
        }
        setStatusText("boneStatus2", status2);
        setStatusText("boneStatus3", status3);
        setStatusText("boneStatus4", status4);

        updateBaSlidersToBone(null);
    }

    /**
     * Update the 3 bone-angle sliders, reset button, and status labels.
     */
    private void updateBaSliders() {
        if (!Maud.modelState.isBoneSelected()) {
            /*
             * No bone selected:
             * Zero sliders, clear status labels, and disable controls.
             */
            for (int iAxis = 0; iAxis < numAxes; iAxis++) {
                baSliders[iAxis].setValue(0f);

                String axisName = axisNames[iAxis];
                String statusName = axisName + "SliderStatus";
                setStatusText(statusName, "");
            }
            disableBoneControls();

        } else {
            float angles[] = new float[numAxes];
            if (Maud.modelState.isAnimationRunning()) {
                /*
                 * An animation is running:
                 * Update the sliders to match bone, disable controls.
                 */
                updateBaSlidersToBone(angles);
                disableBoneControls();

            } else {
                /*
                 * No animation is running:
                 * Read the sliders, enable controls.
                 */
                for (int iAxis = 0; iAxis < numAxes; iAxis++) {
                    angles[iAxis] = baSliders[iAxis].getValue();
                }
                enableBoneControls();
            }
            /*
             * Update the status labels to match the sliders.
             */
            for (int iAxis = 0; iAxis < numAxes; iAxis++) {
                String axisName = axisNames[iAxis];
                String statusName = axisName + "SliderStatus";
                String statusText = String.format(
                        "%s = %+4.2f", axisName, angles[iAxis]);
                setStatusText(statusName, statusText);
            }
        }
    }

    /**
     * Update a bank of 3 sliders that control a color.
     *
     * @param prefix unique id prefix of the bank (not null)
     * @return color indicated by the sliders (new instance)
     */
    private ColorRGBA updateColorBank(String prefix) {
        assert prefix != null;

        float r = updateSlider(prefix + "R", "");
        float g = updateSlider(prefix + "G", "");
        float b = updateSlider(prefix + "B", "");
        ColorRGBA color = new ColorRGBA(r, g, b, 1f);

        return color;
    }

    /**
     * Update the GUI controls for the debugging aids.
     */
    private void updateDebugAids() {
        /*
         * Bone AxesControl
         */
        AxesControl axesControl = Maud.modelState.getBoneAxesControl();
        float lineWidth = updateSlider("bacLineWidth", " pixels");
        float length = updateLogSlider("bacLength", 10f, " bone units");
        if (axesControl != null) {
            axesControl.setLineWidth(lineWidth);
            axesControl.setAxisLength(length);

            boolean enable = isChecked("bacEnable");
            axesControl.setEnabled(enable);
            enable = isChecked("bacDepthTest");
            axesControl.setDepthTest(enable);
        }
        /*
         * Global AxesControl
         */
        axesControl = rootNode.getControl(AxesControl.class);
        boolean enable = isChecked("gacEnable");
        axesControl.setEnabled(enable);
        enable = isChecked("gacDepthTest");
        axesControl.setDepthTest(enable);
        lineWidth = updateSlider("gacLineWidth", " pixels");
        axesControl.setLineWidth(lineWidth);
        length = updateSlider("gacLength", " world units");
        axesControl.setAxisLength(length);
        /*
         * SkeletonDebugControl
         */
        SkeletonDebugControl debugControl = Maud.modelState.getSkeletonDebugControl();
        enable = isChecked("skeletonDebug");
        debugControl.setEnabled(enable);
        ColorRGBA wireColor = updateColorBank("wire");
        debugControl.setColor(wireColor);
        lineWidth = updateSlider("sdcLineWidth", " pixels");
        debugControl.setLineWidth(lineWidth);
        float pointSize = updateSlider("pointSize", " pixels");
        if (debugControl.supportsPointSize()) {
            debugControl.setPointSize(pointSize);
        }
    }
}
