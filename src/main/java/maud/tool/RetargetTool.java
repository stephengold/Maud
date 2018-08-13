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

import com.jme3.animation.AnimControl;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.nifty.GuiScreenController;
import maud.Maud;
import maud.model.EditorModel;
import maud.model.LoadedMap;
import maud.model.cgm.Cgm;
import maud.model.cgm.LoadedAnimation;
import maud.model.cgm.LoadedCgm;
import maud.model.cgm.SelectedAnimControl;

/**
 * The controller for the "Retarget" tool in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class RetargetTool extends Tool {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(RetargetTool.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized tool.
     *
     * @param screenController the controller of the screen that contains the
     * tool (not null)
     */
    RetargetTool(GuiScreenController screenController) {
        super(screenController, "retarget");
    }
    // *************************************************************************
    // Tool methods

    /**
     * Callback to update this tool prior to rendering. (Invoked once per render
     * pass while the tool is displayed.)
     */
    @Override
    protected void toolUpdate() {
        EditorModel model = Maud.getModel();
        LoadedCgm target = model.getTarget();
        String targetCgmName = target.getName();
        String targetCgmText = MyString.quote(targetCgmName);
        setButtonText("retargetTargetCgm", targetCgmText);

        String targetAnimControlText = "";
        SelectedAnimControl targetAnimControl = target.getAnimControl();
        String targetAnimControlName = targetAnimControl.name();
        if (targetAnimControlName != null) {
            targetAnimControlText = targetAnimControlName;
        } else {
            int numAnimControls = target.countSgcs(AnimControl.class);
            if (numAnimControls > 0) {
                targetAnimControlText = "( none selected )";
            }
        }
        setButtonText("retargetTargetAnimControl", targetAnimControlText);

        String sourceCgmText;
        LoadedCgm source = model.getSource();
        if (source.isLoaded()) {
            String name = source.getName();
            sourceCgmText = MyString.quote(name);
        } else {
            sourceCgmText = "( none loaded )";
        }
        setButtonText("retargetSourceCgm", sourceCgmText);

        String sourceAnimControlText = "";
        SelectedAnimControl sourceAnimControl = source.getAnimControl();
        if (source.isLoaded()) {
            String name = sourceAnimControl.name();
            if (name != null) {
                sourceAnimControlText = name;
            } else {
                int numSourceAnimControls = source.countSgcs(AnimControl.class);
                if (numSourceAnimControls > 0) {
                    sourceAnimControlText = "( none selected )";
                }
            }
        }
        setButtonText("retargetSourceAnimControl", sourceAnimControlText);

        String animationText = "";
        if (source.isLoaded() && sourceAnimControl.isSelected()) {
            LoadedAnimation animation = source.getAnimation();
            animationText = animation.getName();
        }
        setButtonText("retargetAnimation", animationText);

        int numBoneMappings = Maud.getModel().getMap().countMappings();
        String mappingDesc = Integer.toString(numBoneMappings);
        setStatusText("mappingCount", mappingDesc);

        updateFeedback();
    }
    // *************************************************************************
    // private methods

    /**
     * Update the feedback line and retarget button.
     */
    private void updateFeedback() {
        String feedback;
        String rButton = "";

        LoadedMap map = Maud.getModel().getMap();
        Cgm source = Maud.getModel().getSource();
        Cgm target = Maud.getModel().getTarget();
        if (!target.getAnimControl().isSelected()) {
            feedback = "select a target anim control";
        } else if (!source.isLoaded()) {
            feedback = "load a source model";
        } else if (!source.getAnimation().isReal()) {
            feedback = "load a source animation";
        } else if (map.isEmpty()) {
            feedback = "no bone mappings";
        } else {
            float matchesSource = map.matchesSource();
            float matchesTarget = map.matchesTarget();
            if (matchesTarget >= 0.9995f) {
                if (matchesSource >= 0.9995f) {
                    feedback = "";
                    rButton = "Retarget";
                } else {
                    feedback = "map doesn't match the source skeleton";
                }
            } else if (matchesSource < 0.9995f) {
                feedback = "map doesn't match either skeleton";
            } else {
                feedback = "map doesn't match the target skeleton";
            }
        }

        setStatusText("retargetFeedback", feedback);
        setButtonText("retarget", rButton);
    }
}
