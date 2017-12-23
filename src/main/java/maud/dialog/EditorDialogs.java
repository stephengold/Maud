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
package maud.dialog;

import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.shader.VarType;
import com.jme3.system.JmeVersion;
import de.lessvoid.nifty.Nifty;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.debug.DebugVersion;
import jme3utilities.nifty.LibraryVersion;
import jme3utilities.nifty.dialog.DialogController;
import jme3utilities.nifty.dialog.FloatDialog;
import jme3utilities.nifty.dialog.IntegerDialog;
import jme3utilities.nifty.dialog.LongDialog;
import jme3utilities.nifty.dialog.TextEntryDialog;
import jme3utilities.sky.Constants;
import jme3utilities.ui.Locators;
import jme3utilities.ui.UiVersion;
import jme3utilities.wes.WesVersion;
import maud.Maud;
import maud.action.Action;
import maud.action.ActionPrefix;
import maud.model.EditorModel;
import maud.model.cgm.Cgm;
import maud.model.cgm.EditableCgm;
import maud.model.cgm.LoadedAnimation;
import maud.model.cgm.LoadedCgm;
import maud.model.cgm.SelectedBone;
import maud.model.cgm.SelectedObject;
import maud.model.cgm.SelectedOverride;
import maud.model.cgm.SelectedShape;
import maud.model.cgm.SelectedSpatial;
import maud.model.cgm.SelectedTrack;
import maud.model.cgm.TrackItem;
import maud.model.option.RigidBodyParameter;
import maud.model.option.ShapeParameter;

/**
 * Dialog boxes created by Maud's "editor" screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class EditorDialogs {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(EditorDialogs.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private EditorDialogs() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Display an "About Maud" dialog.
     */
    public static void aboutMaud() {
        Nifty nifty = Maud.gui.getNifty();
        String niftyVersion = nifty.getVersion();
        String text = "Maud, by Stephen Gold\n\nYou are c"
                + "urrently using Maud, a jMonkeyEngine application for edit"
                + "ing animated models.\n\nThe version you are using "
                + "includes the following libraries:";
        text += String.format("%n   jme3-core version %s",
                MyString.quote(JmeVersion.FULL_NAME));
        text += String.format("%n   nifty version %s",
                MyString.quote(niftyVersion));
        text += String.format("%n   jme3-utilities-heart version %s",
                MyString.quote(Misc.getVersionShort()));
        text += String.format("%n   SkyControl version %s",
                MyString.quote(Constants.getVersionShort()));
        text += String.format("%n   Wes version %s",
                MyString.quote(WesVersion.getVersionShort()));
        text += String.format("%n   jme3-utilities-debug version %s",
                MyString.quote(DebugVersion.getVersionShort()));
        text += String.format("%n   jme3-utilities-ui version %s",
                MyString.quote(UiVersion.getVersionShort()));
        text += String.format("%n   jme3-utilities-nifty version %s",
                MyString.quote(LibraryVersion.getVersionShort()));
        text += String.format("%n   jme3-blender");
        text += String.format("%n   jme3-bullet");
        text += String.format("%n   jme3-bullet-native");
        text += String.format("%n   jme3-desktop");
        text += String.format("%n   jme3-lwjgl");

        text += String.format("%n   jme3_xbuf_loader");
        text += String.format("%n   logback-classic%n%n");

        Maud.gui.closeAllPopups();
        Maud.gui.showInfoDialog("About Maud", text);
    }

    /**
     * Display a confirmation dialog for overwriting a file.
     *
     * @param prefix action prefix (not null, not empty)
     * @param baseFilePath base file path (not null, not empty)
     */
    public static void confirmOverwrite(String prefix, String baseFilePath) {
        Validate.nonEmpty(prefix, "prefix");
        Validate.nonEmpty(baseFilePath, "base file path");

        String action = prefix + baseFilePath;
        String filePath = baseFilePath + ".j3o";
        File file = new File(filePath);
        if (file.exists()) {
            String message
                    = String.format("Overwrite %s?", MyString.quote(filePath));
            OverwriteDialog controller = new OverwriteDialog();
            Maud.gui.showConfirmDialog(message, "", action, controller);
        } else {
            Maud.perform(action);
        }
    }

    /**
     * Display a "copy animation" dialog.
     */
    public static void copyAnimation() {
        String fromName = Maud.getModel().getTarget().getAnimation().getName();
        DialogController controller = new AnimationNameDialog("Copy");

        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter name for copied animation:",
                fromName, ActionPrefix.copyAnimation, controller);
    }

    /**
     * Display a "delete animation" dialog.
     */
    public static void deleteAnimation() {
        String name = Maud.getModel().getTarget().getAnimation().getName();
        String message = String.format("Delete the %s animation?",
                MyString.quote(name));
        Maud.gui.closeAllPopups();
        Maud.gui.showConfirmDialog(message, "Delete", Action.deleteAnimation,
                null);
    }

    /**
     * Display a "delete sgc" dialog.
     */
    public static void deleteSgc() {
        String name = Maud.getModel().getTarget().getSgc().name();
        String message
                = String.format("Delete the %s control?", MyString.quote(name));
        Maud.gui.closeAllPopups();
        Maud.gui.showConfirmDialog(message, "Delete", Action.deleteSgc, null);
    }

    /**
     * Display a "delete nextKeyframes" dialog.
     */
    public static void deleteNextKeyframes() {
        SelectedTrack track = Maud.getModel().getTarget().getTrack();
        int numFrames = track.countKeyframes();
        int frameIndex = track.findKeyframeIndex();
        int max = numFrames - frameIndex - 2;
        IntegerDialog controller = new IntegerDialog("Delete", 1, max);
        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter number of keyframes:", "1",
                ActionPrefix.deleteNextKeyframes, controller);
    }

    /**
     * Display a "delete previousKeyframes" dialog.
     */
    public static void deletePreviousKeyframes() {
        SelectedTrack track = Maud.getModel().getTarget().getTrack();
        int frameIndex = track.findKeyframeIndex();
        int max = frameIndex - 1;
        IntegerDialog controller = new IntegerDialog("Delete", 1, max);
        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter number of keyframes:", "1",
                ActionPrefix.deletePreviousKeyframes, controller);
    }

    /**
     * Display a License infobox.
     *
     * @param licenseType which license to display (not null)
     */
    public static void license(LicenseType licenseType) {
        Validate.nonNull(licenseType, "license");

        String licenseName = licenseType.name();
        String assetPath = String.format("Licenses/%s.txt", licenseName);
        AssetKey<String> key = new AssetKey<>(assetPath);
        AssetManager assetManager = Locators.getAssetManager();

        Locators.save();
        Locators.useDefault();
        String licenseText = assetManager.loadAsset(key);
        Locators.restore();

        String bodyText;
        if (licenseText == null) {
            bodyText = String.format("Your %s license is missing!",
                    licenseName);
        } else {
            bodyText = String.format(
                    "Here's your %s license:%n%n%s%n", licenseName, licenseText);
        }

        Maud.gui.closeAllPopups();
        Maud.gui.showInfoDialog("License information", bodyText);
    }

    /**
     * Display a "load (source)cgm asset " dialog for the specified location.
     *
     * @param spec URL specification, or null for the default location
     * @param slot load slot (not null)
     */
    public static void loadCgmAsset(String spec, LoadedCgm slot) {
        EditorModel model = Maud.getModel();
        String actionPrefix;
        if (slot == model.getTarget()) {
            actionPrefix = ActionPrefix.loadCgmAsset;
        } else if (slot == model.getSource()) {
            actionPrefix = ActionPrefix.loadSourceCgmAsset;
        } else {
            throw new IllegalArgumentException();
        }
        String indexString = model.getLocations().indexForSpec(spec);
        String dialogPrefix = actionPrefix + indexString + " /";

        List<String> extList = new ArrayList<>(6);
        extList.add(".blend");
        extList.add(".j3o");
        extList.add(".mesh.xml");
        extList.add(".obj");
        extList.add(".scene");
        extList.add(".xbuf");
        AssetDialog controller = new AssetDialog("Select", spec, extList);

        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter model asset path:", "Models/",
                "Load", dialogPrefix, controller);
    }

    /**
     * Display a "load map asset " dialog for the specified location.
     *
     * @param spec URL specification, or null for the default location
     */
    public static void loadMapAsset(String spec) {
        EditorModel model = Maud.getModel();
        String indexString = model.getLocations().indexForSpec(spec);
        String dialogPrefix = ActionPrefix.loadMapAsset + indexString + " /";

        List<String> extList = new ArrayList<>(1);
        extList.add(".j3o");
        AssetDialog controller = new AssetDialog("Select", spec, extList);

        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter map asset path:", "SkeletonMaps/",
                "Load", dialogPrefix, controller);
    }

    /**
     * Display a "new animation fromMix" dialog to select the tracks.
     */
    public static void newAnimationFromMix() {
        Cgm target = Maud.getModel().getTarget();
        List<TrackItem> items = target.listTrackItems();
        int numTracks = items.size();
        List<String> options = new ArrayList<>(numTracks);
        for (TrackItem item : items) {
            String description = item.toString();
            options.add(description);
        }

        String prompt = "Select tracks to include in the mix:";
        String commitLabel = "Next";
        String prefix = ActionPrefix.newAnimationFromMix;
        MixDialog controller = new MixDialog(items);

        Maud.gui.closeAllPopups();
        Maud.gui.showMultiSelectDialog(prompt, options, commitLabel, prefix,
                controller);
    }

    /**
     * Display a "new animation fromMix" dialog to enter the animation name.
     *
     * @param actionPrefix (not null, not empty)
     */
    public static void newAnimationFromMix(String actionPrefix) {
        Validate.nonEmpty(actionPrefix, "action prefix");

        String prompt = "Enter a name for the new animation:";
        String defaultName = "mix";
        DialogController controller = new AnimationNameDialog("Create");

        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog(prompt, defaultName, actionPrefix,
                controller);
    }

    /**
     * Display a "new animation fromPose" dialog to enter the animation name.
     */
    public static void newAnimationFromPose() {
        DialogController controller = new AnimationNameDialog("Create");
        String defaultName = "pose";

        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter a name for the new animation:",
                defaultName, ActionPrefix.newAnimationFromPose, controller);
    }

    /**
     * Display a "new override" dialog to name a new material-parameter
     * override.
     *
     * @param actionPrefix (not null, not empty)
     */
    public static void newOverride(String actionPrefix) {
        Validate.nonEmpty(actionPrefix, "action prefix");

        DialogController controller = new OverrideNameDialog("Add");
        String defaultName = "ParameterName";

        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog(
                "Enter a parameter name for the new override:", defaultName,
                actionPrefix, controller);
    }

    /**
     * Display a "new userKey" dialog to name a new user-data key.
     *
     * @param actionPrefix (not null, not empty)
     */
    public static void newUserKey(String actionPrefix) {
        Validate.nonEmpty(actionPrefix, "action prefix");

        DialogController controller = new UserKeyDialog("Add");
        String defaultKey = "key";

        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter a key for the new user data:",
                defaultKey, actionPrefix, controller);
    }

    /**
     * Display a "reduce animation" dialog to enter the reduction factor.
     */
    public static void reduceAnimation() {
        if (Maud.getModel().getTarget().getAnimation().isReal()) {
            IntegerDialog controller;
            controller = new IntegerDialog("Reduce", 2, Integer.MAX_VALUE);

            Maud.gui.closeAllPopups();
            Maud.gui.showTextEntryDialog("Enter reduction factor:", "2",
                    ActionPrefix.reduceAnimation, controller);
        }
    }

    /**
     * Display a "reduce track" dialog to enter the reduction factor.
     */
    public static void reduceTrack() {
        SelectedBone bone = Maud.getModel().getTarget().getBone();
        if (bone.hasTrack()) {
            IntegerDialog controller
                    = new IntegerDialog("Reduce", 2, Integer.MAX_VALUE);

            Maud.gui.closeAllPopups();
            Maud.gui.showTextEntryDialog("Enter reduction factor:", "2",
                    ActionPrefix.reduceTrack, controller);
        }
    }

    /**
     * Display a "rename animation" dialog.
     */
    public static void renameAnimation() {
        LoadedAnimation animation = Maud.getModel().getTarget().getAnimation();
        if (animation.isReal()) {
            String oldName = animation.getName();
            DialogController controller = new AnimationNameDialog("Rename");

            Maud.gui.closeAllPopups();
            Maud.gui.showTextEntryDialog("Enter new name for the animation:",
                    oldName, ActionPrefix.renameAnimation, controller);
        }
    }

    /**
     * Display a "rename bone" dialog.
     */
    public static void renameBone() {
        SelectedBone bone = Maud.getModel().getTarget().getBone();
        if (bone.isSelected()) {
            String oldName = bone.getName();
            DialogController controller = new BoneRenameDialog("Rename");

            Maud.gui.closeAllPopups();
            Maud.gui.showTextEntryDialog("Enter new name for the bone:",
                    oldName, ActionPrefix.renameBone, controller);
        }
    }

    /**
     * Display a "rename override" dialog.
     */
    public static void renameOverride() {
        SelectedOverride override = Maud.getModel().getTarget().getOverride();
        if (override.isSelected()) {
            String oldName = override.getName();
            DialogController controller = new OverrideNameDialog("Rename");

            Maud.gui.closeAllPopups();
            Maud.gui.showTextEntryDialog(
                    "Enter a new name for the parameter override:",
                    oldName, ActionPrefix.renameOverride, controller);
        }
    }

    /**
     * Display a "rename spatial" dialog to enter the new name.
     */
    public static void renameSpatial() {
        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();

        String defaultName = spatial.getName();
        if (defaultName == null) {
            defaultName = "new name";
        }

        DialogController controller = new SpatialNameDialog("Rename");
        String prompt;
        if (spatial.isNode()) {
            prompt = "Enter new name for the node:";
        } else {
            prompt = "Enter new name for the geometry:";
        }

        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog(prompt, defaultName,
                ActionPrefix.renameSpatial, controller);
    }

    /**
     * Display a "rename userKey" dialog to enter the new key.
     */
    public static void renameUserKey() {
        EditableCgm target = Maud.getModel().getTarget();
        String oldName = target.getUserData().getKey();
        if (oldName != null) {
            DialogController controller = new UserKeyDialog("Rename");
            Maud.gui.closeAllPopups();
            Maud.gui.showTextEntryDialog("Enter a new key for the user data:",
                    oldName, ActionPrefix.renameUserKey, controller);
        }
    }

    /**
     * Display a "resample animation" dialog.
     *
     * @param rateFlag true&rarr;per second, false&rarr;number of samples
     */
    public static void resampleAnimation(boolean rateFlag) {
        LoadedAnimation animation = Maud.getModel().getTarget().getAnimation();
        if (animation.isReal()) {
            if (rateFlag) {
                resampleRate(ActionPrefix.resampleAnimationAtRate);
            } else {
                resampleCount(ActionPrefix.resampleAnimationToNumber);
            }
        }
    }

    /**
     * Display a "resample track" dialog.
     *
     * @param rateFlag true&rarr;per second, false&rarr;number of samples
     */
    public static void resampleTrack(boolean rateFlag) {
        SelectedBone bone = Maud.getModel().getTarget().getBone();
        if (bone.hasTrack()) {
            if (rateFlag) {
                resampleRate(ActionPrefix.resampleTrackAtRate);
            } else {
                resampleCount(ActionPrefix.resampleTrackToNumber);
            }
        }
    }

    /**
     * Display a "retarget animation" dialog to enter the new name.
     */
    public static void retargetAnimation() {
        String oldName = Maud.getModel().getSource().getAnimation().getName();
        DialogController controller = new AnimationNameDialog("Retarget");

        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter a name for the new animation:",
                oldName, ActionPrefix.retargetAnimation, controller);
    }

    /**
     * Display a "save cgmUnconfirmed" dialog to enter the base file path.
     */
    public static void saveCgm() {
        EditableCgm target = Maud.getModel().getTarget();
        String baseFilePath = target.baseFilePathForWrite();
        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter base file path for model:",
                baseFilePath, "Save", ActionPrefix.saveCgmUnconfirmed, null);
    }

    /**
     * Display a "save mapUnconfirmed" dialog to enter the base file path.
     */
    public static void saveMap() {
        String baseFilePath = Maud.getModel().getMap().baseFilePathForWrite();
        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter base file path for map:",
                baseFilePath, "Save", ActionPrefix.saveMapUnconfirmed, null);
    }

    /**
     * Display a "select vertex " dialog to enter the vertex index.
     */
    public static void selectVertex() {
        Cgm target = Maud.getModel().getTarget();
        int numVertices = target.getSpatial().countVertices();
        int indexBase = Maud.getModel().getMisc().getIndexBase();
        DialogController controller = new IntegerDialog("Select", indexBase,
                numVertices + indexBase - 1);

        int oldIndex = target.getVertex().getIndex();
        int defaultIndex = (oldIndex == -1) ? indexBase : oldIndex;
        String defaultText = Integer.toString(defaultIndex);

        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter the index of the vertex:",
                defaultText, ActionPrefix.selectVertex, controller);
    }

    /**
     * Display a "set duration proportional" dialog.
     */
    public static void setDurationProportional() {
        LoadedAnimation animation = Maud.getModel().getTarget().getAnimation();
        float oldDuration = animation.getDuration();
        String defaultText = Float.toString(oldDuration);

        float min;
        float finalTime = animation.findLatestKeyframe();
        if (finalTime > 0f) {
            min = 0.01f;
        } else {
            min = 0f;
        }
        DialogController controller
                = new FloatDialog("Set", min, Float.MAX_VALUE);

        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter new duration in seconds:",
                defaultText, ActionPrefix.setDurationProportional, controller);
    }

    /**
     * Display a "set duration same" dialog.
     */
    public static void setDurationSame() {
        LoadedAnimation animation = Maud.getModel().getTarget().getAnimation();
        float oldDuration = animation.getDuration();
        String defaultText = Float.toString(oldDuration);

        float finalTime = animation.findLatestKeyframe();
        DialogController controller
                = new FloatDialog("Extend", finalTime, Float.MAX_VALUE);

        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter new duration in seconds:",
                defaultText, ActionPrefix.setDurationSame, controller);
    }

    /**
     * Display a dialog to alter the value of a material parameter or
     * material-parameter override.
     *
     * @param parameterName name of the parameter to alter (not null)
     * @param varType (not null)
     * @param oldValue (unaffected)
     * @param allowNull if true, "null" will be an allowed value
     * @param actionPrefix (not null, not empty)
     */
    public static void setMatParamValue(String parameterName, VarType varType,
            Object oldValue, boolean allowNull, String actionPrefix) {
        Validate.nonEmpty(actionPrefix, "action prefix");
        Validate.nonNull(parameterName, "parameter name");
        Validate.nonNull(varType, "var type");

        DialogController controller;
        String defaultValue, promptMessage;
        switch (varType) {
            case Boolean:
                if (oldValue == null) {
                    defaultValue = "null";
                } else {
                    boolean booleanValue = (boolean) oldValue;
                    defaultValue = Boolean.toString(booleanValue);
                }
                controller = new BooleanDialog("Set", allowNull);
                promptMessage = "Enter new boolean value:";
                break;

            case Float:
                if (oldValue == null) {
                    defaultValue = "0.0";
                } else {
                    float floatValue = (float) oldValue;
                    defaultValue = Float.toString(floatValue);
                }
                controller = new FloatDialog("Set", Float.NEGATIVE_INFINITY,
                        Float.POSITIVE_INFINITY);
                promptMessage = "Enter new float value:";
                break;

            case Int:
                if (oldValue == null) {
                    defaultValue = "0";
                } else {
                    int intValue = (int) oldValue;
                    defaultValue = Integer.toString(intValue);
                }
                int minValue = Integer.MIN_VALUE;
                int maxValue = Integer.MAX_VALUE;
                if (parameterName.equals("NumberOfBones")) {
                    /*
                     * PreShadow.vert crashes if NumberOfBones < 1.
                     */
                    minValue = 1;
                    /*
                     * Lighting.frag crashes if NumberOfBones > 250.
                     */
                    maxValue = 250;
                }
                controller = new IntegerDialog("Set", minValue, maxValue);
                promptMessage = "Enter new integer value:";
                break;

            case Vector2:
                if (oldValue == null) {
                    defaultValue = "null";
                } else {
                    defaultValue = oldValue.toString();
                }
                controller = new VectorDialog("Set", 2, allowNull);
                promptMessage = "Enter new vector2 value:";
                break;

            case Vector3:
                if (oldValue == null) {
                    defaultValue = "null";
                } else {
                    defaultValue = oldValue.toString();
                }
                controller = new VectorDialog("Set", 3, allowNull);
                promptMessage = "Enter new vector3 value:";
                break;

            case Vector4:
                if (oldValue == null) {
                    defaultValue = "null";
                } else if (oldValue instanceof ColorRGBA) {
                    ColorRGBA color = (ColorRGBA) oldValue;
                    defaultValue = color.r + " " + color.g + " " + color.b
                            + " " + color.a;
                } else if (oldValue instanceof Quaternion) {
                    Quaternion q = (Quaternion) oldValue;
                    defaultValue = q.getX() + " " + q.getY() + " " + q.getZ()
                            + " " + q.getW();
                } else if (oldValue instanceof Vector4f) {
                    Vector4f vector = (Vector4f) oldValue;
                    defaultValue = vector.x + " " + vector.y + " " + vector.z
                            + " " + vector.w;
                } else {
                    throw new IllegalArgumentException();
                }
                controller = new VectorDialog("Set", 4, allowNull);
                promptMessage = "Enter new vector4 value:";
                break;

            // TODO handle more types
            default:
                return;
        }

        Maud.gui.showTextEntryDialog(promptMessage, defaultValue, actionPrefix,
                controller);
    }

    /**
     * Display a "set physicsRbpValue" dialog to enter the parameter value.
     *
     * @param parameter which rigid-body parameter to alter (not null)
     */
    public static void setPhysicsRbpValue(RigidBodyParameter parameter) {
        Validate.nonNull(parameter, "parameter");

        SelectedObject object = Maud.getModel().getTarget().getObject();
        if (object.isSelected()) {
            String defaultText = object.getRbpValue(parameter);
            DialogController controller;
            switch (parameter) {
                case GravityX:
                case GravityY:
                case GravityZ:
                    controller = new FloatDialog("Set", -Float.MAX_VALUE,
                            Float.MAX_VALUE);
                    break;
                default:
                    controller = new FloatDialog("Set", 0f, Float.MAX_VALUE);
            }
            String name = parameter.toString();
            String prompt = String.format("Enter new %s:", name);
            String prefix = ActionPrefix.setPhysicsRbpValue + name + " ";

            Maud.gui.closeAllPopups();
            Maud.gui.showTextEntryDialog(prompt, defaultText, prefix,
                    controller);
        }
    }

    /**
     * Display a "set shapeParmValue" dialog to enter the parameter value.
     *
     * @param parameter which shape parameter to alter (not null)
     */
    public static void setShapeParameterValue(ShapeParameter parameter) {
        Validate.nonNull(parameter, "parameter");

        SelectedShape shape = Maud.getModel().getTarget().getShape();
        if (shape.canSet(parameter)) {
            float defaultValue = shape.getValue(parameter);
            String defaultText = "";
            if (!Float.isNaN(defaultValue)) {
                defaultText = Float.toString(defaultValue);
            }
            DialogController controller
                    = new FloatDialog("Set", 0f, Float.MAX_VALUE);
            String name = parameter.toString();
            String prompt = String.format("Enter new %s:", name);
            String prefix = ActionPrefix.setShapeParmValue + name + " ";

            Maud.gui.closeAllPopups();
            Maud.gui.showTextEntryDialog(prompt, defaultText, prefix,
                    controller);
        }
    }

    /**
     * Display a "set userData" dialog.
     */
    public static void setUserData() {
        Maud.gui.closeAllPopups();
        EditableCgm target = Maud.getModel().getTarget();
        Object value = target.getUserData().getValue();
        DialogController controller;
        String defaultText;
        if (value instanceof Boolean) {
            boolean oldValue = (boolean) value;
            String newText = Boolean.toString(!oldValue); // toggle value
            target.setUserData(newText);

        } else if (value instanceof Float) {
            float oldValue = (float) value;
            controller = new FloatDialog("Set", Float.NEGATIVE_INFINITY,
                    Float.POSITIVE_INFINITY);
            defaultText = Float.toString(oldValue);
            Maud.gui.showTextEntryDialog("Enter new float value:", defaultText,
                    ActionPrefix.setUserData, controller);

        } else if (value instanceof Integer) {
            int oldValue = (int) value;
            controller = new IntegerDialog("Set", Integer.MIN_VALUE,
                    Integer.MAX_VALUE);
            defaultText = Integer.toString(oldValue);
            Maud.gui.showTextEntryDialog("Enter new integer value:",
                    defaultText, ActionPrefix.setUserData, controller);

        } else if (value instanceof Long) {
            long oldValue = (long) value;
            controller = new LongDialog("Set", Long.MIN_VALUE, Long.MAX_VALUE);
            defaultText = Long.toString(oldValue);
            Maud.gui.showTextEntryDialog("Enter new long integer value:",
                    defaultText, ActionPrefix.setUserData, controller);

        } else if (value instanceof String) {
            controller = new TextEntryDialog();
            defaultText = (String) value;
            Maud.gui.showTextEntryDialog("Enter new string value:", defaultText,
                    "Set", ActionPrefix.setUserData, controller);

        } else if (value instanceof Vector2f) {
            Vector2f oldValue = (Vector2f) value;
            controller = new VectorDialog("Set", 2, false);
            defaultText = oldValue.toString();
            Maud.gui.showTextEntryDialog("Enter new Vector2f value:",
                    defaultText, ActionPrefix.setUserData, controller);

        } else if (value instanceof Vector3f) {
            Vector3f oldValue = (Vector3f) value;
            controller = new VectorDialog("Set", 3, false);
            defaultText = oldValue.toString();
            Maud.gui.showTextEntryDialog("Enter new Vector3f value:",
                    defaultText, ActionPrefix.setUserData, controller);

        } else if (value instanceof Vector4f) {
            Vector4f oldValue = (Vector4f) value;
            controller = new VectorDialog("Set", 4, false);
            defaultText = oldValue.toString();
            Maud.gui.showTextEntryDialog("Enter new Vector4f value:",
                    defaultText, ActionPrefix.setUserData, controller);
        }
        // TODO bone data
    }
    // *************************************************************************
    // private methods

    /**
     * Display a "resample animation/track count" dialog.
     *
     * @param actionPrefix action prefix (not null)
     */
    private static void resampleCount(String actionPrefix) {
        LoadedAnimation animation = Maud.getModel().getTarget().getAnimation();
        if (animation.getDuration() > 0f) {
            IntegerDialog controller = new IntegerDialog("Resample", 2, 999);
            Maud.gui.closeAllPopups();
            Maud.gui.showTextEntryDialog("Enter number of samples:", "17",
                    actionPrefix, controller);
        }
    }

    /**
     * Display a "resample animation/track rate" dialog.
     *
     * @param actionPrefix action prefix (not null)
     */
    private static void resampleRate(String actionPrefix) {
        FloatDialog controller = new FloatDialog("Resample", 0.1f, 1000f);
        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter samples per second:", "10",
                actionPrefix, controller);
    }

    /**
     * Display a "wrap animation" dialog.
     */
    public static void wrapAnimation() {
        FloatDialog controller = new FloatDialog("Wrap animation", 0f, 1f);
        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter weight for end-time keyframes:", "0",
                ActionPrefix.wrapAnimation, controller);
    }

    /**
     * Display a "wrap track" dialog.
     */
    public static void wrapTrack() {
        FloatDialog controller = new FloatDialog("Wrap track", 0f, 1f);
        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter weight for end-time keyframe:", "0",
                ActionPrefix.wrapTrack, controller);
    }
}
