/*
 Copyright (c) 2018, Stephen Gold
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

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.nifty.GuiScreenController;
import jme3utilities.nifty.GuiWindowController;
import maud.EditorScreen;

/**
 * A controller for a tool window in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
abstract public class Tool extends GuiWindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(Tool.class.getName());
    // *************************************************************************
    // fields

    /**
     * the name (unique id prefix) of this tool
     */
    final private String toolName;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController the controller of the screen that will contain
     * the tool (not null)
     * @param name the name (unique id prefix) of this tool (not null)
     */
    Tool(GuiScreenController screenController, String name) {
        super(screenController, name + "Tool", false);
        toolName = name;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Update the MVC model based on a check-box event.
     *
     * @param name the name (unique id prefix) of the check box
     * @param isChecked the new state of the check box (true&rarr;checked,
     * false&rarr;unchecked)
     */
    public void onCheckBoxChanged(String name, boolean isChecked) {
        logger.log(Level.WARNING,
                "unexpected check-box change ignored, name={0}",
                MyString.quote(name));
    }

    /**
     * Update the MVC model based on this tool's sliders, if any.
     */
    public void onSliderChanged() {
        logger.warning("unexpected slider change ignored");
    }
    // *************************************************************************
    // new protected methods

    /**
     * Enumerate this tool's check boxes.
     *
     * @return a new list of names (unique id prefixes)
     */
    protected List<String> listCheckBoxes() {
        List<String> result = new ArrayList<>(5);
        return result;
    }

    /**
     * Enumerate this tool's sliders.
     *
     * @return a new list of names (unique id prefixes)
     */
    protected List<String> listSliders() {
        List<String> result = new ArrayList<>(5);
        return result;
    }

    /**
     * Disable or enable all of this tool's sliders.
     *
     * @param newState true&rarr;enable the sliders, false&rarr;disable them
     */
    protected void setSlidersEnabled(boolean newState) {
        List<String> list = listSliders();
        for (String sliderName : list) {
            setSliderEnabled(sliderName, newState);
        }
    }

    /**
     * Callback to update this tool prior to rendering. (Invoked once per render
     * pass while this tool is displayed.)
     */
    abstract protected void toolUpdate();
    // *************************************************************************
    // GuiWindowController methods

    /**
     * Initialize this controller prior to its 1st update.
     *
     * @param stateManager (not null)
     * @param application application which owns this tool (not null)
     */
    @Override
    public void initialize(AppStateManager stateManager,
            Application application) {
        super.initialize(stateManager, application);

        EditorScreen screen = (EditorScreen) getScreenController();
        screen.mapTool(toolName, this);

        List<String> checkBoxNames = listCheckBoxes();
        for (String name : checkBoxNames) {
            screen.mapCheckBox(name, this);
        }

        List<String> sliderNames = listSliders();
        for (String name : sliderNames) {
            screen.mapSlider(name, this);
        }
    }

    /**
     * Callback to update this tool prior to rendering. (Invoked once per render
     * pass while this tool is displayed.)
     *
     * @param updateInterval time interval between updates (in seconds, &ge;0)
     */
    @Override
    final public void update(float updateInterval) {
        super.update(updateInterval);

        EditorScreen screen = (EditorScreen) getScreenController();
        screen.setIgnoreGuiChanges(true);
        toolUpdate();
        screen.setIgnoreGuiChanges(false);
    }
}
