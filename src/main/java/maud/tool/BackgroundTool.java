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
package maud.tool;

import com.jme3.math.ColorRGBA;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.nifty.GuiScreenController;
import jme3utilities.nifty.SliderTransform;
import maud.Maud;
import maud.model.EditorModel;
import maud.model.option.Background;
import maud.model.option.ScoreOptions;
import maud.model.option.scene.RenderOptions;

/**
 * The controller for the "Background" tool in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class BackgroundTool extends Tool {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(BackgroundTool.class.getName());
    /**
     * transform for the color sliders
     */
    final private static SliderTransform colorSt = SliderTransform.Reversed;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized tool.
     *
     * @param screenController the controller of the screen that will contain
     * the tool (not null)
     */
    BackgroundTool(GuiScreenController screenController) {
        super(screenController, "background");
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
        result.add("bgR");
        result.add("bgG");
        result.add("bgB");

        return result;
    }

    /**
     * Update the MVC model based on the sliders.
     */
    @Override
    public void onSliderChanged() {
        EditorModel editorModel = Maud.getModel();
        RenderOptions forScenes = editorModel.getScene().getRender();
        ScoreOptions forScores = editorModel.getScore();

        ColorRGBA color = readColorBank("bg", colorSt, null);
        Background background = Maud.getModel().getMisc().getBackground();
        switch (background) {
            case SourceScenesWithNoSky:
                forScenes.setSourceBackgroundColor(color);
                break;

            case SourceScores:
                forScores.setSourceBackgroundColor(color);
                break;

            case TargetScenesWithNoSky:
                forScenes.setTargetBackgroundColor(color);
                break;

            case TargetScores:
                forScores.setTargetBackgroundColor(color);
                break;

            default:
                throw new IllegalStateException();
        }
    }

    /**
     * Callback to update this tool prior to rendering. (Invoked once per render
     * pass while this tool is displayed.)
     */
    @Override
    protected void toolUpdate() {
        EditorModel editorModel = Maud.getModel();
        RenderOptions forScenes = editorModel.getScene().getRender();
        ScoreOptions forScores = editorModel.getScore();

        ColorRGBA color;
        Background background = Maud.getModel().getMisc().getBackground();
        switch (background) {
            case SourceScenesWithNoSky:
                color = forScenes.sourceBackgroundColor(null);
                break;

            case SourceScores:
                color = forScores.sourceBackgroundColor(null);
                break;

            case TargetScenesWithNoSky:
                color = forScenes.targetBackgroundColor(null);
                break;

            case TargetScores:
                color = forScores.targetBackgroundColor(null);
                break;

            default:
                throw new IllegalStateException();
        }
        setColorBank("bg", colorSt, color);

        String buttonText = background.toString();
        setButtonText("bgSelect", buttonText);
    }
}
