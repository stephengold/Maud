/*
 Copyright (c) 2017-2022, Stephen Gold
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
package maud;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import java.util.logging.Logger;
import jme3utilities.InitialState;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.ui.InputMode;

/**
 * The screen controller for Maud's "start" screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class StartScreen extends BasicScreenController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(StartScreen.class.getName());
    /**
     * Nifty ID for this screen
     */
    final private static String screenId = "start";
    /**
     * asset path to Nifty XML layout of this screen
     */
    final private static String xmlAssetPath
            = "Interface/Nifty/screens/start.xml";
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized, disabled screen that will be enabled during
     * initialization.
     */
    StartScreen() {
        super(screenId, xmlAssetPath, InitialState.Enabled);
    }
    // *************************************************************************
    // AppState methods

    /**
     * Initialize this controller prior to its first update.
     *
     * @param stateManager (not null)
     * @param application application that owns the screen (not null)
     */
    @Override
    public void initialize(AppStateManager stateManager,
            Application application) {
        if (isEnabled()) {
            throw new IllegalStateException("shouldn't be enabled yet");
        }
        InputMode defaultMode = InputMode.getActiveMode();
        setListener(defaultMode);
        super.initialize(stateManager, application);
        defaultMode.influence(this);
    }
}
