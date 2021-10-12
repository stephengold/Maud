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
package maud.tool;

import de.lessvoid.nifty.controls.ScrollPanel;
import de.lessvoid.nifty.controls.label.builder.LabelBuilder;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.screen.Screen;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.GuiScreenController;
import jme3utilities.nifty.Tool;
import maud.DescribeUtil;
import maud.Maud;
import maud.model.Checkpoint;
import maud.model.History;

/**
 * The controller for the "History" tool in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class HistoryTool extends Tool {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(HistoryTool.class.getName());
    // *************************************************************************
    // fields

    /**
     * true &rarr; scroll to current on next update, false &rarr; manual scroll
     */
    private boolean autoScrollFlag = false;
    /**
     * number of labels written to the content area
     */
    private int numLabelsWritten = 0;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized tool.
     *
     * @param screenController the controller of the screen that will contain
     * the tool (not null)
     */
    HistoryTool(GuiScreenController screenController) {
        super(screenController, "history");
    }
    // *************************************************************************
    // new methods exposed

    /**
     * scroll to "you are here" on next update
     */
    public void setAutoScroll() {
        autoScrollFlag = true;
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
        result.add("autoCheckpoint");

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
        switch (name) {
            case "autoCheckpoint":
                History.setAutoAdd(isChecked);
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
        String aButton = "";
        String rButton = "";
        int nextIndex = History.getNextIndex();
        int numCheckpoints = History.countCheckpoints();
        if (numCheckpoints > nextIndex) {
            aButton = "Redo all";
            rButton = "Redo";
        }
        setButtonText("historyRedoAll", aButton);
        setButtonText("historyRedo", rButton);

        setButtonText("historyClear", "Clear");

        int limit = Maud.getModel().getMisc().maxCheckpoints();
        String limitButton = Integer.toString(limit);
        setButtonText("historyLimit", limitButton);

        String uButton = "";
        boolean noneVulnerable = !History.hasVulnerable();
        if (nextIndex > 1 || noneVulnerable && nextIndex > 0) {
            uButton = "Undo";
        }
        setButtonText("historyUndo", uButton);
        /*
         * Add dynamic content to the scroll panel.
         */
        numLabelsWritten = 0;
        for (int cpIndex = 0; cpIndex < nextIndex; cpIndex++) {
            addCheckpoint(cpIndex, "#cfcf");
            /* green = secure for now */
        }
        List<String> events = History.listRecentEvents();
        for (String event : events) {
            addLabel(".. " + event, "#ffcf");
            /* yellow = vulnerable to redo */
        }
        Element urHere = addLabel(".. ( you are here )", "#ffff");
        for (int cpIndex = nextIndex; cpIndex < numCheckpoints; cpIndex++) {
            addCheckpoint(cpIndex, "#fccf");
            /* pink = vulnerable to add */
        }

        Element windowElement = getElement();
        windowElement.layoutElements();
        /*
         * Update the "automatic checkpoints" checkbox.
         */
        boolean autoAddFlag = History.isAutoAdd();
        setChecked("autoCheckpoint", autoAddFlag);

        if (autoScrollFlag) {
            autoScrollFlag = false;
            Element parent = urHere.getParent();
            int y = urHere.getY() - parent.getY();
            scrollTo(y);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Add a checkpoint to the scroll-panel content.
     */
    private void addCheckpoint(int cpIndex, String bgColor) {
        Checkpoint checkpoint = History.getCheckpoint(cpIndex);
        List<String> events = checkpoint.listEvents();
        for (String event : events) {
            addLabel(". " + event, bgColor);
        }

        String id = DescribeUtil.index(cpIndex);
        Date creationDate = checkpoint.copyTimestamp();
        DateFormat timestampFormatter = DateFormat.getTimeInstance();
        String creationTime = timestampFormatter.format(creationDate);
        String text = String.format("checkpoint%s added at %s", id,
                creationTime);
        addLabel(text, bgColor);
    }

    /**
     * Add a label to the scroll-panel content.
     *
     * @param text (not null)
     * @return the element of the newly-created label
     */
    private Element addLabel(final String text, final String bgColor) {
        BasicScreenController screenController = getScreenController();
        Screen screen = screenController.getScreen();
        Element content = screen.findElementById("historyDynamicContent");

        final String labelId = String.format("historyLine%d", numLabelsWritten);
        ++numLabelsWritten;
        LabelBuilder builder = new LabelBuilder() {
            {
                alignLeft();
                backgroundColor(bgColor);
                id(labelId);
                label(" " + text);
                width("330px");
            }
        };
        Element newLabel = builder.build(content);
        /*
         * Mark the label for removal before the next update.
         */
        getNifty().removeElement(screen, newLabel);

        return newLabel;
    }

    /**
     * Scroll to the specified position in the dynamic content.
     *
     * @param y vertical offset (in pixels downward from top edge)
     */
    private void scrollTo(int y) {
        Screen screen = Maud.gui.getScreen();
        ScrollPanel scpa = screen.findNiftyControl("historyScrollPanel",
                ScrollPanel.class);
        scpa.setVerticalPos(y);
        /*
         * Redo the layout.
         */
        Element windowElement = getElement();
        windowElement.layoutElements();
    }
}
