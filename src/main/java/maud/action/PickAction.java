/*
 Copyright (c) 2018-2022, Stephen Gold
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
import maud.menu.ViewMenus;
import maud.view.Drag;
import maud.view.scene.SceneDrag;

/**
 * Process actions that start with the word "pick".
 *
 * @author Stephen Gold sgold@sonic.net
 */
final class PickAction {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(PickAction.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private PickAction() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Process an ongoing action that starts with the word "pick".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    static boolean process(String actionString) {
        boolean handled = true;

        switch (actionString) {
            case Action.pickAny:
                Maud.gui.pickAny();
                break;

            case Action.pickBone:
                Maud.gui.pickBone();
                break;

            case Action.pickGnomon:
                Maud.gui.pickGnomon();
                break;

            case Action.pickKeyframe:
                Maud.gui.pickKeyframe();
                break;

            case Action.pickVertex:
                Maud.gui.pickVertex();
                break;

            case Action.pickViewMenu:
                ViewMenus.pick();
                break;

            default:
                handled = false;
        }

        return handled;
    }

    /**
     * Process a non-ongoing action that starts with the word "pick".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    static boolean processNotOngoing(String actionString) {
        boolean handled = true;
        switch (actionString) {
            case Action.pickAny:
                Drag.stopDraggingBoundary();
                Drag.stopDraggingGnomon();
                SceneDrag.clear();
                break;

            case Action.pickBone:
            case Action.pickKeyframe:
            case Action.pickVertex:
                break;

            case Action.pickGnomon:
                Drag.stopDraggingGnomon();
                break;

            default:
                handled = false;
        }

        return handled;
    }
}
