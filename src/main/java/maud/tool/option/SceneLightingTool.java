/*
 Copyright (c) 2018-2020, Stephen Gold
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

import com.jme3.light.DirectionalLight;
import com.jme3.math.Vector3f;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.math.MyVector3f;
import jme3utilities.nifty.GuiScreenController;
import jme3utilities.nifty.SliderTransform;
import jme3utilities.nifty.Tool;
import maud.Maud;
import maud.model.EditorModel;
import maud.model.option.scene.LightsOptions;
import maud.model.option.scene.RenderOptions;
import maud.model.option.scene.SceneOptions;
import maud.view.scene.SceneView;

/**
 * The controller for the "Scene Lighting" tool in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SceneLightingTool extends Tool {
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
            = Logger.getLogger(SceneLightingTool.class.getName());
    /**
     * transform for the direction sliders
     */
    final private static SliderTransform directionSt = SliderTransform.Reversed;
    /**
     * transform for the color sliders
     */
    final private static SliderTransform levelSt = SliderTransform.None;
    /**
     * names of the coordinate axes
     */
    final private static String[] axisNames = {"x", "y", "z"};
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized tool.
     *
     * @param screenController the controller of the screen that will contain
     * the tool (not null)
     */
    public SceneLightingTool(GuiScreenController screenController) {
        super(screenController, "sceneLighting");
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
        result.add("sky2");

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
        for (int iAxis = 0; iAxis < numAxes; iAxis++) {
            String sliderName = axisNames[iAxis] + "Dir";
            result.add(sliderName);
        }
        result.add("ambientLevel");
        result.add("mainLevel");

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
            case "sky2":
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
        LightsOptions options = Maud.getModel().getScene().getLights();

        Vector3f direction = readVectorBank("Dir", directionSt, null);
        if (MyVector3f.isZero(direction)) {
            direction.set(0f, -1f, 0f);
        }
        options.setDirection(direction);

        float ambientLevel = readSlider("ambientLevel", levelSt);
        options.setAmbientLevel(ambientLevel);

        float mainLevel = readSlider("mainLevel", levelSt);
        options.setMainLevel(mainLevel);
    }

    /**
     * Update this tool prior to rendering. (Invoked once per frame while this
     * tool is displayed.)
     */
    @Override
    protected void toolUpdate() {
        EditorModel model = Maud.getModel();
        SceneOptions sceneOptions = model.getScene();
        boolean isSkySimulated = sceneOptions.getRender().isSkySimulated();
        setChecked("sky2", isSkySimulated);

        SceneView sceneView = model.getTarget().getSceneView();
        int count = sceneView.countAddedLightProbes();
        String countText;
        if (Maud.envCamIsBusy && count > 0) {
            countText = Integer.toString(count - 1) + "+";
        } else {
            countText = Integer.toString(count);
        }
        setStatusText("probeCount", countText);

        String addText;
        if (Maud.envCamIsBusy) {
            addText = "";
        } else {
            addText = "Add";
        }
        setButtonText("probeAdd", addText);

        if (count > 0) {
            setButtonText("probeDeleteAll", "Delete all");
        } else {
            setButtonText("probeDeleteAll", "");
        }

        LightsOptions lightsOptions = sceneOptions.getLights();
        Vector3f direction;
        if (isSkySimulated) {
            DirectionalLight main = sceneView.getMainLight();
            direction = main.getDirection(); // alias
        } else {
            direction = lightsOptions.direction(null);
        }

        for (int iAxis = 0; iAxis < numAxes; iAxis++) {
            String sliderName = axisNames[iAxis] + "Dir";
            setSliderEnabled(sliderName, !isSkySimulated);
            float value = direction.get(iAxis);
            setSlider(sliderName, directionSt, value);
            updateSliderStatus(sliderName, value, "");
        }

        float ambientLevel = lightsOptions.getAmbientLevel();
        setSlider("ambientLevel", levelSt, ambientLevel);
        updateSliderStatus("ambientLevel", ambientLevel, "");

        float mainLevel = lightsOptions.getMainLevel();
        setSlider("mainLevel", levelSt, mainLevel);
        updateSliderStatus("mainLevel", mainLevel, "");
    }
}
