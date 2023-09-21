/*
 Copyright (c) 2017-2019, Stephen Gold
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
package maud.tool.option;

import java.util.List;
import java.util.logging.Logger;
import jme3utilities.TimeOfDay;
import jme3utilities.nifty.GuiScreenController;
import jme3utilities.nifty.SliderTransform;
import jme3utilities.nifty.Tool;
import maud.Maud;
import maud.model.option.scene.RenderOptions;

/**
 * The controller for the "Sky" tool in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SkyTool extends Tool {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SkyTool.class.getName());
    /**
     * transform for the cloudiness slider
     */
    final private static SliderTransform cloudinessSt = SliderTransform.None;
    /**
     * transform for the hour slider
     */
    final private static SliderTransform hourSt = SliderTransform.None;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized tool.
     *
     * @param screenController the controller of the screen that will contain
     * the tool (not null)
     */
    public SkyTool(GuiScreenController screenController) {
        super(screenController, "sky");
    }
    // *************************************************************************
    // Tool methods

    /**
     * Enumerate this tool's check boxes.
     *
     * @return a new list of names (unique id prefixes)
     */
    @Override
    protected List<String> listCheckBoxes() {
        List<String> result = super.listCheckBoxes();
        result.add("sky");

        return result;
    }

    /**
     * Enumerate this tool's sliders.
     *
     * @return a new list of names (unique id prefixes)
     */
    @Override
    protected List<String> listSliders() {
        List<String> result = super.listSliders();
        result.add("cloudiness");
        result.add("hour");

        return result;
    }

    /**
     * Update the MVC model based on a check-box event.
     *
     * @param name the name (unique id prefix) of the checkbox
     * @param isChecked the new state of the checkbox (true&rarr;checked,
     * false&rarr;unchecked)
     */
    @Override
    public void onCheckBoxChanged(String name, boolean isChecked) {
        RenderOptions options = Maud.getModel().getScene().getRender();
        switch (name) {
            case "sky":
                options.setSkySimulated(isChecked);
                break;

            default:
                super.onCheckBoxChanged(name, isChecked);
        }
    }

    /**
     * Update the MVC model based on the sliders.
     *
     * @param sliderName the name (unique id prefix) of the slider (not null)
     */
    @Override
    public void onSliderChanged(String sliderName) {
        RenderOptions options = Maud.getModel().getScene().getRender();

        float cloudiness = readSlider("cloudiness", cloudinessSt);
        options.setCloudiness(cloudiness);

        float hour = readSlider("hour", hourSt);
        options.setHour(hour);
    }

    /**
     * Update this tool prior to rendering. (Invoked once per frame while this
     * tool is displayed.)
     */
    @Override
    protected void toolUpdate() {
        RenderOptions options = Maud.getModel().getScene().getRender();

        boolean isSkySimulated = options.isSkySimulated();
        setChecked("sky", isSkySimulated);

        float cloudiness = options.cloudiness();
        setSlider("cloudiness", cloudinessSt, cloudiness);
        updateSliderStatus("cloudiness", 100f * cloudiness, "%");

        float hour = options.hour();
        setSlider("hour", hourSt, hour);
        int second = Math.round(hour * 3_600);
        int minute = second / TimeOfDay.secondsPerMinute;
        int mm = minute % TimeOfDay.minutesPerHour;
        int hh = minute / TimeOfDay.minutesPerHour;
        String tod = String.format("solar time = %d:%02d", hh, mm);
        setStatusText("hourSliderStatus", tod);
    }
}
