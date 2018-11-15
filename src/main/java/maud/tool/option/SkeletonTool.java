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
package maud.tool.option;

import com.jme3.math.ColorRGBA;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.nifty.GuiScreenController;
import jme3utilities.nifty.SliderTransform;
import maud.Maud;
import maud.model.option.ShowBones;
import maud.model.option.scene.SkeletonColors;
import maud.model.option.scene.SkeletonOptions;
import maud.tool.Tool;

/**
 * The controller for the "Skeleton" tool in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SkeletonTool extends Tool {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SkeletonTool.class.getName());
    /**
     * transform for the color sliders
     */
    final private static SliderTransform colorSt = SliderTransform.Reversed;
    /**
     * transform for the point-size sliders
     */
    final private static SliderTransform sizeSt = SliderTransform.None;
    /**
     * transform for the width slider
     */
    final private static SliderTransform widthSt = SliderTransform.None;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized tool.
     *
     * @param screenController the controller of the screen that will contain
     * the tool (not null)
     */
    public SkeletonTool(GuiScreenController screenController) {
        super(screenController, "skeleton");
    }
    // *************************************************************************
    // Tool methods

    /**
     * Enumerate this tool's sliders.
     *
     * @return a new list of names (unique id prefixes)
     */
    @Override
    protected List<String> listSliders() {
        List<String> result = super.listSliders();
        result.add("skeletonLineWidth");
        result.add("skeletonPointSize");
        result.add("skeR");
        result.add("skeG");
        result.add("skeB");

        return result;
    }

    /**
     * Update the MVC model based on the sliders.
     *
     * @param sliderName the name (unique id prefix) of the slider (not null)
     */
    @Override
    public void onSliderChanged(String sliderName) {
        SkeletonOptions options = Maud.getModel().getScene().getSkeleton();

        float lineWidth = readSlider("skeletonLineWidth", widthSt);
        options.setLineWidth(lineWidth);

        float pointSize = readSlider("skeletonPointSize", sizeSt);
        options.setPointSize(pointSize);

        ColorRGBA color = readColorBank("ske", colorSt, null);
        SkeletonColors use = options.getEditColor();
        options.setColor(use, color);
    }

    /**
     * Update this tool prior to rendering. (Invoked once per frame while this
     * tool is displayed.)
     */
    @Override
    protected void toolUpdate() {
        SkeletonOptions options = Maud.getModel().getScene().getSkeleton();

        ShowBones showBones = options.getShowBones();
        String showBonesStatus = showBones.toString();
        setButtonText("skeletonShowBones", showBonesStatus);

        float lineWidth = options.getLineWidth();
        setSlider("skeletonLineWidth", widthSt, lineWidth);
        lineWidth = Math.round(lineWidth);
        updateSliderStatus("skeletonLineWidth", lineWidth, " px");

        float pointSize = options.getPointSize();
        setSlider("skeletonPointSize", sizeSt, pointSize);
        pointSize = Math.round(pointSize);
        updateSliderStatus("skeletonPointSize", pointSize, " px");

        SkeletonColors editColor = options.getEditColor();
        String colorSelectButton = editColor.toString();
        setButtonText("skeletonColorSelect", colorSelectButton);

        ColorRGBA color = options.copyColor(editColor, null);
        setColorBank("ske", colorSt, color);
    }
}
