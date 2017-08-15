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

import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.WindowController;
import maud.Maud;
import maud.model.LoadedCgm;
import maud.model.LoadedMap;

/**
 * The controller for the "Retarget Tool" window in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class RetargetTool extends WindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            RetargetTool.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController
     */
    RetargetTool(BasicScreenController screenController) {
        super(screenController, "retargetTool", false);
    }
    // *************************************************************************
    // AppState methods

    /**
     * Callback to update this window prior to rendering. (Invoked once per
     * render pass.)
     *
     * @param elapsedTime time interval between render passes (in seconds,
     * &ge;0)
     */
    @Override
    public void update(float elapsedTime) {
        super.update(elapsedTime);

        String targetName = Maud.getModel().getTarget().getName();
        String targetDesc = MyString.quote(targetName);
        Maud.gui.setStatusText("targetName", " " + targetDesc);

        LoadedCgm source = Maud.getModel().getSource();
        String sButton, sourceDesc;
        if (!source.isLoaded()) {
            sourceDesc = "( none loaded )";
            sButton = "";

        } else {
            String sourceName = source.getName();
            sourceDesc = MyString.quote(sourceName);
            if (source.countAnimations() > 0) {
                sButton = "Load";
            } else {
                sButton = "";
            }
        }
        Maud.gui.setStatusText("sourceName", " " + sourceDesc);
        Maud.gui.setButtonLabel("selectSourceAnimationButton", sButton);

        int numBoneMappings = Maud.getModel().getMap().countMappings();
        String mappingDesc = Integer.toString(numBoneMappings);
        Maud.gui.setStatusText("mappingCount", mappingDesc);

        updateBottom();
        updateFeedback();
    }
    // *************************************************************************
    // private methods

    /**
     * Update the source animation.
     */
    private void updateBottom() {
        String sourceAnimDesc = "";

        LoadedCgm source = Maud.getModel().getSource();
        boolean real = source.animation.isReal();
        if (real) {
            String name = source.animation.getName();
            sourceAnimDesc = MyString.quote(name);
        }

        Maud.gui.setStatusText("sourceAnimation", " " + sourceAnimDesc);
    }

    /**
     * Update the feedback line and retarget button.
     */
    private void updateFeedback() {
        String feedback;
        String rButton = "";

        LoadedMap map = Maud.getModel().getMap();
        LoadedCgm source = Maud.getModel().getSource();
        LoadedCgm target = Maud.getModel().getTarget();
        if (!target.isAnimControlSelected()) {
            feedback = "select the target anim control";
        } else if (!source.isLoaded()) {
            feedback = "load the source model";
        } else if (!source.animation.isReal()) {
            feedback = "load the source animation";
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

        Maud.gui.setStatusText("retargetFeedback", feedback);
        Maud.gui.setButtonLabel("retargetButton", rButton);
    }
}
