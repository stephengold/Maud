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
package maud.tool;

import com.jme3.math.Quaternion;
import java.util.logging.Logger;
import jme3utilities.math.MyMath;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.SliderTransform;
import jme3utilities.nifty.WindowController;
import maud.Maud;
import maud.model.cgm.Cgm;
import maud.model.cgm.SelectedBone;

/**
 * The controller for the "Bone-Rotation Tool" window in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class BoneRotationTool extends WindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * number of coordinate axes
     */
    final private static int numAxes = 3;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(BoneRotationTool.class.getName());
    /**
     * transform for the axis sliders
     */
    final private static SliderTransform axisSt = SliderTransform.Reversed;
    /**
     * names of the coordinate axes
     */
    final private static String[] axisNames = {"x", "y", "z"};
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController the controller of the screen that contains the
     * window (not null)
     */
    BoneRotationTool(BasicScreenController screenController) {
        super(screenController, "boneRotationTool", false);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * If active, update the MVC model based on the sliders.
     */
    void onSliderChanged() {
        Cgm target = Maud.getModel().getTarget();
        if (target.getBone().shouldEnableControls()) {
            float[] angles = new float[numAxes];
            for (int iAxis = 0; iAxis < numAxes; iAxis++) {
                String sliderName = axisNames[iAxis] + "Ang";
                float value = Maud.gui.readSlider(sliderName, axisSt);
                angles[iAxis] = value;
            }
            Quaternion rot = new Quaternion();
            rot.fromAngles(angles);
            int boneIndex = target.getBone().getIndex();
            target.getPose().get().setRotation(boneIndex, rot);
        }
    }
    // *************************************************************************
    // WindowController methods

    /**
     * Callback to update this state prior to rendering. (Invoked once per
     * render pass.)
     *
     * @param tpf time interval between render passes (in seconds, &ge;0)
     */
    @Override
    public void update(float tpf) {
        super.update(tpf);
        Maud.gui.setIgnoreGuiChanges(true);

        String aButton = "";
        String bButton = "";
        SelectedBone bone = Maud.getModel().getTarget().getBone();
        if (bone.isSelected()) {
            setSlidersToPose();
            if (bone.shouldEnableControls()) {
                aButton = "Animation";
                bButton = "Bind pose";
                enableSliders();
            } else {
                disableSliders();
            }

        } else {
            clear();
            disableSliders();
        }
        Maud.gui.setButtonLabel("resetAngAnimButton", aButton);
        Maud.gui.setButtonLabel("resetAngBindButton", bButton);

        String dButton; // TODO remove?
        if (Maud.getModel().getMisc().getAnglesInDegrees()) {
            dButton = "radians";
        } else {
            dButton = "degrees";
        }
        Maud.gui.setButtonLabel("degreesButton", dButton);
        Maud.gui.setIgnoreGuiChanges(false);
    }
    // *************************************************************************
    // private methods

    /**
     * Zero all 3 sliders and clear their status labels.
     */
    private void clear() {
        for (int iAxis = 0; iAxis < numAxes; iAxis++) {
            String sliderName = axisNames[iAxis] + "Ang";
            Maud.gui.setSlider(sliderName, axisSt, 0f);
            Maud.gui.setStatusText(sliderName + "SliderStatus", "");
        }
    }

    /**
     * Disable all 3 sliders.
     */
    private void disableSliders() {
        for (int iAxis = 0; iAxis < numAxes; iAxis++) {
            String sliderName = axisNames[iAxis] + "Ang";
            Maud.gui.disableSlider(sliderName);
        }
    }

    /**
     * Enable all 3 sliders.
     */
    private void enableSliders() {
        for (int iAxis = 0; iAxis < numAxes; iAxis++) {
            String sliderName = axisNames[iAxis] + "Ang";
            Maud.gui.enableSlider(sliderName);
        }
    }

    /**
     * Set all 3 sliders (and their status labels) based on the displayed pose.
     */
    private void setSlidersToPose() {
        SelectedBone bone = Maud.getModel().getTarget().getBone();
        Quaternion rotation = bone.userRotation(null);
        float[] angles = rotation.toAngles(null);
        boolean degrees = Maud.getModel().getMisc().getAnglesInDegrees();

        for (int iAxis = 0; iAxis < numAxes; iAxis++) {
            String sliderName = axisNames[iAxis] + "Ang";
            float angle = angles[iAxis];
            Maud.gui.setSlider(sliderName, axisSt, angle);

            if (degrees) {
                angle = MyMath.toDegrees(angle);
                Maud.gui.updateSliderStatus(sliderName, angle, " deg");
            } else {
                Maud.gui.updateSliderStatus(sliderName, angle, " rad");
            }
        }
    }
}
