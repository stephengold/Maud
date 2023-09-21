/*
 Copyright (c) 2019, Stephen Gold
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
import jme3utilities.minie.DumpFlags;
import jme3utilities.minie.PhysicsDumper;
import jme3utilities.nifty.GuiScreenController;
import jme3utilities.nifty.Tool;
import maud.Maud;

/**
 * The controller for the "Dump" tool in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class DumpTool extends Tool {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(DumpTool.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized tool.
     *
     * @param screenController the controller of the screen that will contain
     * the tool (not null)
     */
    public DumpTool(GuiScreenController screenController) {
        super(screenController, "dump");
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
        result.add("dumpBuckets");
        result.add("dumpCullHints");
        result.add("dumpJib");
        result.add("dumpJis");
        result.add("dumpMatParams");
        result.add("dumpMpos");
        result.add("dumpShadows");
        result.add("dumpTransforms");
        result.add("dumpUserData");

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
        PhysicsDumper dumper = Maud.getModel().getDumper();

        switch (name) {
            case "dumpBuckets":
                dumper.setDumpBucket(isChecked);
                break;
            case "dumpCullHints":
                dumper.setDumpCull(isChecked);
                break;
            case "dumpJib":
                dumper.setEnabled(DumpFlags.JointsInBodies, isChecked);
                break;
            case "dumpJis":
                dumper.setEnabled(DumpFlags.JointsInSpaces, isChecked);
                break;
            case "dumpMatParams":
                dumper.setDumpMatParam(isChecked);
                break;
            case "dumpMpos":
                dumper.setDumpOverride(isChecked);
                break;
            case "dumpShadows":
                dumper.setDumpShadow(isChecked);
                break;
            case "dumpTransforms":
                dumper.setDumpTransform(isChecked);
                break;
            case "dumpUserData":
                dumper.setDumpUser(isChecked);
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
        PhysicsDumper dumper = Maud.getModel().getDumper();

        boolean dumpBuckets = dumper.isDumpBucket();
        setChecked("dumpBuckets", dumpBuckets);

        boolean dumpCullHints = dumper.isDumpCull();
        setChecked("dumpCullHints", dumpCullHints);

        boolean dumpJib = dumper.isEnabled(DumpFlags.JointsInBodies);
        setChecked("dumpJib", dumpJib);

        boolean dumpJis = dumper.isEnabled(DumpFlags.JointsInSpaces);
        setChecked("dumpJis", dumpJis);

        boolean dumpMatParams = dumper.isDumpMatParam();
        setChecked("dumpMatParams", dumpMatParams);

        boolean dumpMpo = dumper.isDumpOverride();
        setChecked("dumpMpos", dumpMpo);

        boolean dumpShadows = dumper.isDumpShadow();
        setChecked("dumpShadows", dumpShadows);

        boolean dumpTransforms = dumper.isDumpTransform();
        setChecked("dumpTransforms", dumpTransforms);

        boolean dumpUserData = dumper.isDumpUser();
        setChecked("dumpUserData", dumpUserData);

        String indentIncrement = dumper.indentIncrement();
        int numSpaces = indentIncrement.length();
        String text = Integer.toString(numSpaces);
        setButtonText("dumpIndent", text);

        int maxChildren = dumper.maxChildren();
        if (maxChildren == Integer.MAX_VALUE) {
            text = "All";
        } else {
            text = Integer.toString(maxChildren);
        }
        setButtonText("dumpMaxChildren", text);
    }
}
