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

import com.jme3.bullet.animation.RangeOfMotion;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.math.MyMath;
import jme3utilities.nifty.GuiScreenController;
import jme3utilities.nifty.SliderTransform;
import maud.DescribeUtil;
import maud.Maud;
import maud.model.EditorModel;
import maud.model.cgm.Cgm;
import maud.model.cgm.EditableCgm;
import maud.model.cgm.SelectedBone;
import maud.model.cgm.SelectedLink;
import maud.model.cgm.SelectedRagdoll;
import maud.model.option.MiscOptions;
import maud.model.option.RotationDisplayMode;

/**
 * The controller for the Link tool in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class LinkTool extends Tool {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(LinkTool.class.getName());
    /**
     * transform for an angle slider
     */
    final private static SliderTransform angleSt = SliderTransform.None;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized tool.
     *
     * @param screenController the controller of the screen that will contain
     * the tool (not null)
     */
    LinkTool(GuiScreenController screenController) {
        super(screenController, "link");
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
        result.add("maxLinkAngle");
        result.add("minLinkAngle");

        return result;
    }

    /**
     * Update the MVC model based on the sliders.
     *
     * @param sliderName the name (unique id prefix) of the slider (not null)
     */
    @Override
    public void onSliderChanged(String sliderName) {
        EditorModel model = Maud.getModel();
        int axisIndex = model.getMisc().linkToolAxis();
        float maxAngle = readSlider("maxLinkAngle", angleSt);
        float minAngle = readSlider("minLinkAngle", angleSt);

        if (maxAngle < minAngle) {
            if (sliderName.equals("maxLinkAngle")) {
                minAngle = maxAngle;
            } else {
                maxAngle = minAngle;
            }
        }

        EditableCgm target = model.getTarget();
        target.setLinkAxisLimits(axisIndex, maxAngle, minAngle);
    }

    /**
     * Callback to update this tool prior to rendering. (Invoked once per frame
     * while this tool is displayed.)
     */
    @Override
    protected void toolUpdate() {
        updateChildren();
        updateIndex();
        updateName();
        updateParent();
        updateRangeOfMotion();

        String addButton = "";
        String jointButton = "";
        String massButton = "";
        String objectButton = "";
        String selectButton = "";

        Cgm target = Maud.getModel().getTarget();
        SelectedBone selectedBone = target.getBone();
        SelectedLink selectedLink = target.getLink();
        SelectedRagdoll selectedRagdoll = target.getRagdoll();

        if (selectedRagdoll.isSelected() && selectedBone.isSelected()
                && !selectedBone.isLinked()) {
            addButton = "Link bone";
        }

        if (selectedLink.isSelected()) {
            if (selectedBone.isLinked()) {
                String boneName = selectedBone.getName();
                String linkName = selectedLink.name();
                if (!boneName.equals(linkName)) {
                    selectButton = "Select linked bone";
                }
            }
            String jointName = selectedLink.jointName();
            if (jointName != null) {
                jointButton = jointName;
            }
            float mass = selectedLink.mass();
            massButton = Float.toString(mass);
            objectButton = selectedLink.objectName();
        }

        setButtonText("linkAddBoneLink", addButton);
        setButtonText("linkMass", massButton);
        setButtonText("linkSelectBone", selectButton);
        setButtonText("linkSelectJoint", jointButton);
        setButtonText("linkSelectObject", objectButton);
    }
    // *************************************************************************
    // private methods

    /**
     * Update the children button.
     */
    private void updateChildren() {
        String button = "";

        SelectedLink selectedLink = Maud.getModel().getTarget().getLink();
        if (selectedLink.isSelected()) {
            int numChildren = selectedLink.countChildren();
            if (numChildren > 1) {
                button = String.format("%d children", numChildren);
            } else if (numChildren == 1) {
                String childName = selectedLink.childNames().get(0);
                button = MyString.quote(childName);
            }
        }

        setButtonText("linkSelectChild", button);
    }

    /**
     * Update the index status and previous/next/select buttons.
     */
    private void updateIndex() {
        String indexStatus;
        String nextButton, previousButton, selectButton;

        Cgm target = Maud.getModel().getTarget();
        int numLinks = target.getRagdoll().countLinks();
        SelectedLink selectedLink = target.getLink();
        boolean isSelected = selectedLink.isSelected();
        if (isSelected) {
            int selectedIndex = selectedLink.findIndex();
            indexStatus = DescribeUtil.index(selectedIndex, numLinks);
            nextButton = "+";
            previousButton = "-";
        } else {
            if (numLinks == 0) {
                indexStatus = "no links";
            } else if (numLinks == 1) {
                indexStatus = "one link";
            } else {
                indexStatus = String.format("%d links", numLinks);
            }
            nextButton = "";
            previousButton = "";
        }

        if (numLinks == 0) {
            selectButton = "";
        } else if (numLinks == 1 && isSelected) {
            selectButton = "Deselect link";
        } else {
            selectButton = "Select link";
        }

        setStatusText("linkIndex", indexStatus);
        setButtonText("linkNext", nextButton);
        setButtonText("linkPrevious", previousButton);
        setButtonText("linkSelect", selectButton);
    }

    /**
     * Update the name status.
     */
    private void updateName() {
        String nameStatus;

        SelectedLink selectedLink = Maud.getModel().getTarget().getLink();
        if (selectedLink.isSelected()) {
            String name = selectedLink.name();
            nameStatus = MyString.quote(name);
        } else {
            nameStatus = "(none selected)";
        }

        setStatusText("linkName", " " + nameStatus);
    }

    /**
     * Update the parent status and button.
     */
    private void updateParent() {
        String button = "";

        SelectedLink selectedLink = Maud.getModel().getTarget().getLink();
        if (selectedLink.isSelected()) {
            String parentName = selectedLink.nameParent();
            if (parentName != null) {
                button = MyString.quote(parentName);
            }
        }

        setButtonText("linkSelectParent", button);
    }

    /**
     * Update the range of motion.
     */
    private void updateRangeOfMotion() {
        EditorModel model = Maud.getModel();
        MiscOptions misc = model.getMisc();
        int axisIndex = misc.linkToolAxis();

        String axisName = DescribeUtil.axisName(axisIndex);
        setButtonText("linkAxis", axisName);

        RotationDisplayMode rdm = misc.rotationDisplayMode();
        String modeName = rdm.toString();
        setButtonText("rotationMode4", modeName);

        String status;
        SelectedLink selectedLink = model.getTarget().getLink();
        if (selectedLink.isBoneLink()) {
            RangeOfMotion rom = selectedLink.getRangeOfMotion();
            float maxAngle = rom.getMaxRotation(axisIndex);
            float minAngle = rom.getMinRotation(axisIndex);
            switch (rdm) {
                case Degrees:
                    float min = MyMath.toDegrees(minAngle);
                    float max = MyMath.toDegrees(maxAngle);
                    status = String.format("%+.0f to %+.0f degrees", min, max);
                    break;
                case Radians:
                    status = String.format("%+.2f to %+.2f radians", minAngle,
                            maxAngle);
                    break;
                default:
                    status = "";
            }
            setSlider("maxLinkAngle", angleSt, maxAngle);
            setSlider("minLinkAngle", angleSt, minAngle);
            setSliderEnabled("maxLinkAngle", true);
            setSliderEnabled("minLinkAngle", true);
        } else {
            status = "";
            setSliderEnabled("maxLinkAngle", false);
            setSliderEnabled("minLinkAngle", false);
        }
        setStatusText("linkAngleStatus", status);
    }
}
