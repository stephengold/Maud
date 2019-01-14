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

import com.jme3.shadow.EdgeFilteringMode;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.nifty.GuiScreenController;
import jme3utilities.nifty.SliderTransform;
import jme3utilities.nifty.Tool;
import maud.Maud;
import maud.model.EditorModel;
import maud.model.option.scene.RenderOptions;
import maud.model.option.scene.SceneOptions;
import maud.model.option.scene.TriangleMode;
import maud.view.scene.SceneView;

/**
 * The controller for the "Render" tool in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class RenderTool extends Tool {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(RenderTool.class.getName());
    /**
     * transform for the scale sliders
     */
    final private static SliderTransform scaleSt = SliderTransform.Log10;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized tool.
     *
     * @param screenController the controller of the screen that will contain
     * the tool (not null)
     */
    public RenderTool(GuiScreenController screenController) {
        super(screenController, "render");
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
        result.add("shadows");
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
        result.add("sourceScale");
        result.add("targetScale");

        return result;
    }

    /**
     * Update the MVC model based on a check-box event.
     *
     * @param name the name (unique id prefix) of the check box
     * @param isChecked the new state of the check box (true&rarr;checked,
     * false&rarr;unchecked)
     */
    @Override
    public void onCheckBoxChanged(String name, boolean isChecked) {
        SceneOptions options = Maud.getModel().getScene();
        switch (name) {
            case "shadows":
                options.getRender().setShadowsRendered(isChecked);
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
        EditorModel model = Maud.getModel();

        float sourceScale = readSlider("sourceScale", scaleSt);
        model.getSource().getSceneView().getTransform().setScale(sourceScale);

        float targetScale = readSlider("targetScale", scaleSt);
        model.getTarget().getSceneView().getTransform().setScale(targetScale);
    }

    /**
     * Update this tool prior to rendering. (Invoked once per frame while this
     * tool is displayed.)
     */
    @Override
    protected void toolUpdate() {
        EditorModel model = Maud.getModel();
        SceneOptions sceneOptions = model.getScene();
        RenderOptions options = sceneOptions.getRender();

        boolean shadowsFlag = options.areShadowsRendered();
        setChecked("shadows", shadowsFlag);

        TriangleMode mode = options.triangleMode();
        String modeName = mode.toString();
        setButtonText("triangles", modeName);

        EdgeFilteringMode edgeFilter = options.edgeFilter();
        modeName = edgeFilter.toString();
        setButtonText("edgeFilter", modeName);

        SceneView sourceView = model.getSource().getSceneView();
        float sourceScale = sourceView.getTransform().getScale();
        setSlider("sourceScale", scaleSt, sourceScale);
        updateSliderStatus("sourceScale", sourceScale, "x");

        SceneView targetView = model.getTarget().getSceneView();
        float targetScale = targetView.getTransform().getScale();
        setSlider("targetScale", scaleSt, targetScale);
        updateSliderStatus("targetScale", targetScale, "x");
    }
}
