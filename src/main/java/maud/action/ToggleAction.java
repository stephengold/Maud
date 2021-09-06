/*
 Copyright (c) 2017-2021, Stephen Gold
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
package maud.action;

import java.util.logging.Logger;
import maud.Maud;
import maud.model.EditorModel;
import maud.model.cgm.Cgm;
import maud.model.cgm.EditableCgm;
import maud.model.option.MiscOptions;
import maud.model.option.scene.SceneOptions;
import maud.view.scene.SceneDrag;

/**
 * Process actions that start with the word "toggle".
 *
 * @author Stephen Gold sgold@sonic.net
 */
class ToggleAction {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(ToggleAction.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private ToggleAction() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Process an ongoing action that starts with the word "toggle".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    static boolean toggleAction(String actionString) {
        boolean handled = true;

        EditorModel model = Maud.getModel();
        MiscOptions miscOptions = model.getMisc();
        SceneOptions sceneOptions = model.getScene();
        Cgm source = model.getSource();
        EditableCgm target = model.getTarget();

        switch (actionString) {
            case Action.toggleBoundType:
                target.toggleBoundType();
                break;

            case Action.toggleCursorColorIndex:
                miscOptions.toggleColorIndex();
                break;

            case Action.toggleDragSide:
                SceneDrag.toggleSide();
                break;

            case Action.toggleFreezeTarget:
                target.getPose().toggleFrozen();
                break;

            case Action.toggleIndexBase:
                miscOptions.toggleIndexBase();
                break;

            case Action.toggleLoadOrientation:
                miscOptions.toggleLoadOrientation();
                break;

            case Action.toggleMenuBar:
                miscOptions.toggleMenuBarVisibility();
                break;

            case Action.toggleMovement:
                sceneOptions.getCamera().toggleMovement();
                break;

            case Action.togglePause:
                source.getPlay().togglePaused();
                target.getPlay().togglePaused();
                break;

            case Action.togglePauseSource:
                source.getPlay().togglePaused();
                break;

            case Action.togglePauseTarget:
                if (target.getAnimation().isRetargetedPose()) {
                    source.getPlay().togglePaused();
                } else {
                    target.getPlay().togglePaused();
                }
                break;

            case Action.togglePhysicsDebug:
                sceneOptions.getRender().togglePhysicsRendered();
                break;

            case Action.toggleProjection:
                sceneOptions.getCamera().toggleProjection();
                break;

            default:
                handled = false;
        }

        return handled;
    }
}
