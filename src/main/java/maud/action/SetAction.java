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
package maud.action;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.shader.VarType;
import java.util.logging.Logger;
import jme3utilities.MyString;
import maud.Maud;
import maud.MaudUtil;
import maud.dialog.DimensionsDialog;
import maud.dialog.EditorDialogs;
import maud.menu.EnumMenus;
import maud.menu.ShowMenus;
import maud.model.EditorModel;
import maud.model.WhichCgm;
import maud.model.cgm.Cgm;
import maud.model.cgm.EditableCgm;
import maud.model.cgm.PlayOptions;
import maud.model.cgm.PlayTimes;
import maud.model.cgm.SelectedMatParam;
import maud.model.cgm.SelectedOverride;
import maud.model.option.Background;
import maud.model.option.DisplaySettings;
import maud.model.option.RigidBodyParameter;
import maud.model.option.ShapeParameter;
import maud.model.option.scene.SkeletonColors;

/**
 * Process actions that start with the word "set".
 *
 * @author Stephen Gold sgold@sonic.net
 */
class SetAction {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SetAction.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private SetAction() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Process an ongoing action that starts with the word "set".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    static boolean process(String actionString) {
        boolean handled = true;

        EditorModel model = Maud.getModel();
        EditableCgm target = model.getTarget();
        switch (actionString) {
            case Action.setBatchHint:
                EnumMenus.setBatchHint();
                break;

            case Action.setColorDepth:
                ShowMenus.setColorDepth();
                break;

            case Action.setCullHint:
                EnumMenus.setCullHint();
                break;

            case Action.setDimensions:
                if (DisplaySettings.isFullscreen()) {
                    ShowMenus.setDimensions();
                } else {
                    EditorDialogs.setDimensions();
                }
                break;

            case Action.setMatParamValue:
                SelectedMatParam matParam = target.getMatParam();
                if (matParam.isSelected()) {
                    String parameterName = matParam.getName();
                    VarType varType = matParam.getVarType();
                    Object oldValue = matParam.getValue();
                    boolean allowNull = false;
                    EditorDialogs.setMatParamValue(parameterName, varType,
                            oldValue, allowNull, ActionPrefix.setMatParamValue);
                }
                break;

            case Action.setMsaaFactor:
                ShowMenus.setMsaaFactor();
                break;

            case Action.setOverrideValue:
                SelectedOverride override = target.getOverride();
                if (override.isSelected()) {
                    String parameterName = override.getName();
                    VarType varType = override.getVarType();
                    Object oldValue = override.getValue();
                    boolean allowNull = true;
                    EditorDialogs.setMatParamValue(parameterName, varType,
                            oldValue, allowNull, ActionPrefix.setOverrideValue);
                }
                break;

            case Action.setPhysicsRbpValue:
                RigidBodyParameter rbp = model.getMisc().getRbp();
                EditorDialogs.setPhysicsRbpValue(rbp);
                break;

            case Action.setQueueBucket:
                EnumMenus.setQueueBucket();
                break;

            case Action.setRefreshRate:
                ShowMenus.setRefreshRate();
                break;

            case Action.setShadowMode:
                EnumMenus.setShadowMode();
                break;

            case Action.setShapeParmValue:
                ShapeParameter shapeParameter
                        = model.getMisc().getShapeParameter();
                EditorDialogs.setShapeParameterValue(shapeParameter);
                break;

            case Action.setSpatialAngleCardinal:
                target.getSpatial().cardinalizeRotation();
                break;

            case Action.setSpatialAngleSnapX:
                target.getSpatial().snapRotation(PhysicsSpace.AXIS_X);
                break;

            case Action.setSpatialAngleSnapY:
                target.getSpatial().snapRotation(PhysicsSpace.AXIS_Y);
                break;

            case Action.setSpatialAngleSnapZ:
                target.getSpatial().snapRotation(PhysicsSpace.AXIS_Z);
                break;

            case Action.setTimeLimitLower:
                Cgm cgm = Maud.gui.mouseCgm();
                if (cgm != null) {
                    PlayOptions play = cgm.getPlay();
                    float currentTime = play.getTime();
                    if (currentTime <= play.getUpperLimit()) {
                        play.setLowerLimit(currentTime);
                    }
                }
                break;

            case Action.setTimeLimitUpper:
                cgm = Maud.gui.mouseCgm();
                if (cgm != null) {
                    PlayOptions play = cgm.getPlay();
                    float currentTime = play.getTime();
                    if (currentTime >= play.getLowerLimit()) {
                        play.setUpperLimit(currentTime);
                    }
                }
                break;

            case Action.setTrackRotationAll:
                target.getTrack().setRotationAll();
                break;

            case Action.setTrackScaleAll:
                target.getTrack().setScaleAll();
                break;

            case Action.setTrackTranslationAll:
                target.getTrack().setTranslationAll();
                break;

            case Action.setTwistCardinal:
                model.getMap().cardinalizeTwist();
                break;

            case Action.setTwistSnapX:
                model.getMap().snapTwist(PhysicsSpace.AXIS_X);
                break;

            case Action.setTwistSnapY:
                model.getMap().snapTwist(PhysicsSpace.AXIS_Y);
                break;

            case Action.setTwistSnapZ:
                model.getMap().snapTwist(PhysicsSpace.AXIS_Z);
                break;

            case Action.setUserData:
                EditorDialogs.setUserData();
                break;

            default:
                handled = testForPrefixes(actionString);
        }

        return handled;
    }
    // *************************************************************************
    // private methods

    /**
     * Process an ongoing action that starts with the word "set" -- 2nd part:
     * test for prefixes.
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private static boolean testForPrefixes(String actionString) {
        boolean handled = true;

        EditorModel model = Maud.getModel();
        EditableCgm target = model.getTarget();
        String arg;
        if (actionString.startsWith(ActionPrefix.set3DCursorColor)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.set3DCursorColor);
            String[] args = arg.split(" ");
            if (args.length >= 2) {
                int index = Integer.valueOf(args[0]);
                String colorText = MyString.remainder(arg, args[0] + " ");
                ColorRGBA color = MaudUtil.parseColor(colorText);
                model.getScene().getCursor().setColor(index, color);
            } else {
                handled = false;
            }

        } else if (actionString.startsWith(ActionPrefix.set3DCursorCycleTime)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.set3DCursorCycleTime);
            float cycleTime = Float.valueOf(arg);
            model.getScene().getCursor().setCycleTime(cycleTime);

        } else if (actionString.startsWith(ActionPrefix.set3DCursorSize)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.set3DCursorSize);
            float size = Float.valueOf(arg);
            model.getScene().getCursor().setSize(size);

        } else if (actionString.startsWith(ActionPrefix.setAmbientLevel)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setAmbientLevel);
            float level = Float.valueOf(arg);
            model.getScene().getLights().setAmbientLevel(level);

        } else if (actionString.startsWith(ActionPrefix.setAxesLineWidth)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setAxesLineWidth);
            float width = Float.valueOf(arg);
            model.getScene().getAxes().setLineWidth(width);

        } else if (actionString.startsWith(ActionPrefix.setBackgroundColor)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setBackgroundColor);
            String[] args = arg.split(" ");
            if (args.length >= 2) {
                Background which = Background.valueOf(args[0]);
                String colorText = MyString.remainder(arg, args[0] + " ");
                ColorRGBA color = MaudUtil.parseColor(colorText);
                model.setBackgroundColor(which, color);
            } else {
                handled = false;
            }

        } else if (actionString.startsWith(ActionPrefix.setBoundsColor)) {
            arg = MyString.remainder(actionString, ActionPrefix.setBoundsColor);
            ColorRGBA color = MaudUtil.parseColor(arg);
            model.getScene().getBounds().setColor(color);

        } else if (actionString.startsWith(ActionPrefix.setBoundsLineWidth)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setBoundsLineWidth);
            float width = Float.valueOf(arg);
            model.getScene().getBounds().setLineWidth(width);

        } else if (actionString.startsWith(ActionPrefix.setCloudiness)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setCloudiness);
            float fraction = Float.valueOf(arg);
            model.getScene().getRender().setCloudiness(fraction);

        } else if (actionString.startsWith(ActionPrefix.setColorDepth)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setColorDepth);
            int bitsPerPixel = Integer.parseInt(arg);
            DisplaySettings.setColorDepth(bitsPerPixel);

        } else if (actionString.startsWith(ActionPrefix.setDimensions)) {
            arg = MyString.remainder(actionString, ActionPrefix.setDimensions);
            int[] wh = DimensionsDialog.parseDimensions(arg);
            if (wh == null) {
                handled = false;
            } else {
                DisplaySettings.setDimensions(wh[0], wh[1]);
            }

        } else if (actionString.startsWith(
                ActionPrefix.setDurationProportional)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setDurationProportional);
            float value = Float.parseFloat(arg);
            target.getAnimation().setDurationProportional(value);

        } else if (actionString.startsWith(ActionPrefix.setDurationSame)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setDurationSame);
            float value = Float.parseFloat(arg);
            target.getAnimation().setDurationSame(value);

        } else if (actionString.startsWith(ActionPrefix.setFrameTime)) {
            arg = MyString.remainder(actionString, ActionPrefix.setFrameTime);
            float value = Float.parseFloat(arg);
            target.getFrame().setTime(value);

        } else if (actionString.startsWith(ActionPrefix.setHour)) {
            arg = MyString.remainder(actionString, ActionPrefix.setHour);
            float hour = Float.valueOf(arg);
            model.getScene().getRender().setHour(hour);

        } else if (actionString.startsWith(ActionPrefix.setMainDirection)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setMainDirection);
            Vector3f direction = MaudUtil.parseVector3f(arg);
            model.getScene().getLights().setDirection(direction);

        } else if (actionString.startsWith(ActionPrefix.setMainLevel)) {
            arg = MyString.remainder(actionString, ActionPrefix.setMainLevel);
            float level = Float.valueOf(arg);
            model.getScene().getLights().setMainLevel(level);

        } else if (actionString.startsWith(ActionPrefix.setMapSize)) {
            arg = MyString.remainder(actionString, ActionPrefix.setMapSize);
            int mapSize = Integer.parseInt(arg);
            model.getScene().getRender().setShadowMapSize(mapSize);

        } else if (actionString.startsWith(ActionPrefix.setMatParamValue)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setMatParamValue);
            target.setMatParamValue(arg);

        } else if (actionString.startsWith(ActionPrefix.setMsaaFactor)) {
            arg = MyString.remainder(actionString, ActionPrefix.setMsaaFactor);
            int factor = 16;
            for (int f : new int[]{1, 2, 4, 6, 8}) {
                String aaDescription = MaudUtil.describeMsaaFactor(f);
                if (arg.equals(aaDescription)) {
                    factor = f;
                    break;
                }
            }
            DisplaySettings.setMsaaFactor(factor);

        } else if (actionString.startsWith(ActionPrefix.setNumSplits)) {
            arg = MyString.remainder(actionString, ActionPrefix.setNumSplits);
            int numSplits = Integer.parseInt(arg);
            model.getScene().getRender().setNumSplits(numSplits);

        } else if (actionString.startsWith(ActionPrefix.setOverrideValue)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setOverrideValue);
            target.setOverrideValue(arg);

        } else if (actionString.startsWith(ActionPrefix.setPhysicsRbpValue)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setPhysicsRbpValue);
            String[] args = arg.split(" ");
            if (args.length == 2) {
                RigidBodyParameter parm = RigidBodyParameter.valueOf(args[0]);
                float value = Float.parseFloat(args[1]);
                target.setRigidBodyParameter(parm, value);
            } else {
                handled = false;
            }

        } else if (actionString.startsWith(ActionPrefix.setPlatformDiameter)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setPlatformDiameter);
            String[] args = arg.split(" ");
            if (args.length == 2) {
                WhichCgm which = WhichCgm.valueOf(args[0]);
                float diameter = Float.parseFloat(args[1]);
                model.getScene().setPlatformDiameter(which, diameter);
            } else {
                handled = false;
            }

        } else if (actionString.startsWith(ActionPrefix.setRefreshRate)) {
            arg = MyString.remainder(actionString, ActionPrefix.setRefreshRate);
            int hertz = Integer.parseInt(arg);
            DisplaySettings.setRefreshRate(hertz);

        } else if (actionString.startsWith(ActionPrefix.setShapeParmValue)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setShapeParmValue);
            String[] args = arg.split(" ");
            if (args.length == 2) {
                ShapeParameter parm = ShapeParameter.valueOf(args[0]);
                float value = Float.parseFloat(args[1]);
                target.setShapeParameter(parm, value);
            } else {
                handled = false;
            }

        } else if (actionString.startsWith(ActionPrefix.setSkeletonColor)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setSkeletonColor);
            String[] args = arg.split(" ");
            if (args.length >= 2) {
                SkeletonColors use = SkeletonColors.valueOf(args[0]);
                String colorText = MyString.remainder(arg, args[0] + " ");
                ColorRGBA color = MaudUtil.parseColor(colorText);
                model.getScene().getSkeleton().setColor(use, color);
            } else {
                handled = false;
            }

        } else if (actionString.startsWith(ActionPrefix.setSkeletonLineWidth)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setSkeletonLineWidth);
            float width = Float.valueOf(arg);
            model.getScene().getSkeleton().setLineWidth(width);

        } else if (actionString.startsWith(ActionPrefix.setSkeletonPointSize)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setSkeletonPointSize);
            float pointSize = Float.valueOf(arg);
            model.getScene().getSkeleton().setPointSize(pointSize);

        } else if (actionString.startsWith(ActionPrefix.setSubmenuWarp)) {
            arg = MyString.remainder(actionString, ActionPrefix.setSubmenuWarp);
            String[] args = arg.split(" ");
            if (args.length == 2) {
                float x = Float.parseFloat(args[0]);
                float y = Float.parseFloat(args[1]);
                model.getMisc().setSubmenuWarp(x, y);
            } else {
                handled = false;
            }

        } else if (actionString.startsWith(ActionPrefix.setTime)) {
            arg = MyString.remainder(actionString, ActionPrefix.setTime);
            String[] args = arg.split(" ");
            if (args.length < 2) {
                handled = false;
            } else {
                WhichCgm whichCgm = WhichCgm.valueOf(args[0]);
                PlayTimes whichTime = PlayTimes.valueOf(args[1]);
                if (args.length == 2) {
                    EditorDialogs.setTime(whichCgm, whichTime);
                } else if (args.length == 3) {
                    float newValue = Float.parseFloat(args[2]);
                    Cgm cgm = model.getCgm(whichCgm);
                    cgm.getPlay().setTime(whichTime, newValue);
                } else {
                    handled = false;
                }
            }

        } else if (actionString.startsWith(ActionPrefix.setTimeToFrame)) {
            arg = MyString.remainder(actionString, ActionPrefix.setTimeToFrame);
            String[] args = arg.split(" ");
            if (args.length < 2) {
                handled = false;
            } else {
                WhichCgm whichCgm = WhichCgm.valueOf(args[0]);
                PlayTimes whichTime = PlayTimes.valueOf(args[1]);
                if (args.length == 2) {
                    EditorDialogs.setTimeToKeyframe(whichCgm, whichTime);
                } else if (args.length == 3) {
                    int indexBase = Maud.getModel().getMisc().getIndexBase();
                    int index = Integer.parseInt(args[2]) - indexBase;
                    Cgm cgm = model.getCgm(whichCgm);
                    float newValue = cgm.getTrack().keyframeTime(index);
                    cgm.getPlay().setTime(whichTime, newValue);
                } else {
                    handled = false;
                }
            }

        } else if (actionString.startsWith(ActionPrefix.setUserData)) {
            arg = MyString.remainder(actionString, ActionPrefix.setUserData);
            target.setUserData(arg);

        } else if (actionString.startsWith(ActionPrefix.setVertexColor)) {
            arg = MyString.remainder(actionString, ActionPrefix.setVertexColor);
            ColorRGBA color = MaudUtil.parseColor(arg);
            model.getScene().getVertex().setColor(color);

        } else if (actionString.startsWith(ActionPrefix.setVertexPointSize)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setVertexPointSize);
            float size = Float.parseFloat(arg);
            model.getScene().getVertex().setPointSize(size);

        } else if (actionString.startsWith(ActionPrefix.setXBoundary)) {
            arg = MyString.remainder(actionString, ActionPrefix.setXBoundary);
            float position = Float.valueOf(arg);
            model.getMisc().setXBoundary(position);

        } else {
            handled = false;
        }

        return handled;
    }
}
