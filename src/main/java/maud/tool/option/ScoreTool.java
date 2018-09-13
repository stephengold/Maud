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

import java.util.List;
import java.util.logging.Logger;
import jme3utilities.nifty.GuiScreenController;
import maud.Maud;
import maud.model.option.ScoreOptions;
import maud.model.option.ShowBones;
import maud.tool.Tool;

/**
 * The controller for the "Score" tool in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class ScoreTool extends Tool {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(ScoreTool.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized tool.
     *
     * @param screenController the controller of the screen that will contain
     * the tool (not null)
     */
    public ScoreTool(GuiScreenController screenController) {
        super(screenController, "score");
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
        result.add("scoreTranslations");
        result.add("scoreRotations");
        result.add("scoreScales");

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
        ScoreOptions options = Maud.getModel().getScore();
        switch (name) {
            case "scoreRotations":
                options.setShowRotations(isChecked);
                break;

            case "scoreScales":
                options.setShowScales(isChecked);
                break;

            case "scoreTranslations":
                options.setShowTranslations(isChecked);
                break;

            default:
                super.onCheckBoxChanged(name, isChecked);
        }
    }

    /**
     * Update this tool prior to rendering. (Invoked once per frame while this
     * tool is displayed.)
     */
    @Override
    protected void toolUpdate() {
        ScoreOptions scoreOptions = Maud.getModel().getScore();

        boolean translations = scoreOptions.showsTranslations();
        setChecked("scoreTranslations", translations);

        boolean rotations = scoreOptions.showsRotations();
        setChecked("scoreRotations", rotations);

        boolean scales = scoreOptions.showsScales();
        setChecked("scoreScales", scales);

        ShowBones showNoneSelected = scoreOptions.getShowNoneSelected();
        String noneButton = showNoneSelected.toString();
        setButtonText("scoreShowNoneSelected", noneButton);

        ShowBones showWhenSelected = scoreOptions.getShowWhenSelected();
        String whenButton = showWhenSelected.toString();
        setButtonText("scoreShowWhenSelected", whenButton);
    }
}
