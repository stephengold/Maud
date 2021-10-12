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
package maud.dialog;

import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.shader.VarType;
import com.jme3.system.JmeVersion;
import de.lessvoid.nifty.Nifty;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.Heart;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.minie.MinieVersion;
import jme3utilities.nifty.LibraryVersion;
import jme3utilities.nifty.dialog.BooleanDialog;
import jme3utilities.nifty.dialog.DialogController;
import jme3utilities.nifty.dialog.FloatDialog;
import jme3utilities.nifty.dialog.IntegerDialog;
import jme3utilities.nifty.dialog.LongDialog;
import jme3utilities.nifty.dialog.MinimalDialog;
import jme3utilities.nifty.dialog.TextEntryDialog;
import jme3utilities.nifty.dialog.VectorDialog;
import jme3utilities.sky.Constants;
import jme3utilities.ui.Locators;
import jme3utilities.ui.UiVersion;
import jme3utilities.wes.WesVersion;
import maud.Maud;
import maud.action.Action;
import maud.action.ActionPrefix;
import maud.model.EditorModel;
import maud.model.WhichCgm;
import maud.model.cgm.Cgm;
import maud.model.cgm.CgmOutputFormat;
import maud.model.cgm.CgmOutputSet;
import maud.model.cgm.EditableCgm;
import maud.model.cgm.GeometryItem;
import maud.model.cgm.LoadedAnimation;
import maud.model.cgm.LoadedCgm;
import maud.model.cgm.MatParamRef;
import maud.model.cgm.PlayOptions;
import maud.model.cgm.PlayTimes;
import maud.model.cgm.SelectedBone;
import maud.model.cgm.SelectedBuffer;
import maud.model.cgm.SelectedFrame;
import maud.model.cgm.SelectedLight;
import maud.model.cgm.SelectedLink;
import maud.model.cgm.SelectedOverride;
import maud.model.cgm.SelectedPco;
import maud.model.cgm.SelectedShape;
import maud.model.cgm.SelectedSpatial;
import maud.model.cgm.SelectedTexture;
import maud.model.cgm.SelectedTrack;
import maud.model.cgm.SpatialItem;
import maud.model.cgm.TrackItem;
import maud.model.option.RigidBodyParameter;
import maud.model.option.RotationDisplayMode;
import maud.model.option.ShapeParameter;
import maud.tool.EditorTools;
import org.lwjgl.Sys;

/**
 * Dialog boxes created by Maud's "editor" screen (utility class).
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
    // new methods exposed TODO split off the text-entry dialog methods

    /**
     * Display an "About Maud" dialog.
     */
    public static void aboutMaud() {
        Nifty nifty = Maud.gui.getNifty();
        String niftyVersion = nifty.getVersion();
        String text = "Maud, by Stephen Gold\n\nYou are c"
                + "urrently using Maud, a jMonkeyEngine application for edit"
                + "ing animated models.\n\nThe version you are using "
                + "is designated v1.0.0-beta1 .\n\nIt "
                + "includes the following libraries:";
        text += String.format("%n   jme3-core version=%s hash=%s (BSD license)",
                MyString.quote(JmeVersion.FULL_NAME),
                JmeVersion.GIT_SHORT_HASH);
        text += String.format("%n   nifty version=%s (BSD license)",
                MyString.quote(niftyVersion));
        text += String.format("%n   Heart version=%s (BSD license)",
                MyString.quote(Heart.versionShort()));
        text += String.format("%n   Minie version=%s (BSD license)",
                MyString.quote(MinieVersion.versionShort()));
        text += String.format("%n   SkyControl version=%s (BSD license)",
                MyString.quote(Constants.versionShort()));
        text += String.format("%n   Wes version=%s (BSD license)",
                MyString.quote(WesVersion.versionShort()));
        text += String.format("%n   jme3-utilities-ui version=%s (BSD license)",
                MyString.quote(UiVersion.versionShort()));
        text += String.format(
                "%n   jme3-utilities-nifty version=%s (BSD license)",
                MyString.quote(LibraryVersion.versionShort()));

        text += String.format("%n   jme3-blender (BSD license)");
        text += String.format("%n   jme3-desktop (BSD license)");
        text += String.format("%n   jme3-effects (BSD license)");
        text += String.format("%n   jme3-lwjgl (BSD license)");
        text += String.format("%n   jme3-plugins (BSD license)");
        text += String.format("%n   jme3-terrain (BSD license)");

        text += String.format("%n   nifty-default-controls (BSD license)");
        text += String.format("%n   lwjgl version=%s (BSD license)",
                MyString.quote(Sys.getVersion()));

        text += String.format("%n   jme-ttf (%s)",
                "part FPL license, part BSD license");
        text += String.format("%n   sfntly (Apache license)");

        text += String.format("%n   jme3_xbuf_loader (public domain)");
        text += String.format("%n   jme3_xbuf_rt (public domain)");

        text += String.format("%n   logback-core (%s)",
                "Eclipse Public License v1.0");
        text += String.format("%n   logback-classic (%s)",
                "Eclipse Public License v1.0");

        text += String.format("%n%n");

        Maud.gui.closeAllPopups();
        Maud.gui.showInfoDialog("About Maud", text);
    }

    /**
     * Display a confirmation dialog for overwriting a file.
     *
     * @param actionPrefix the prefix to use in constructing the new action (not
     * null, not empty)
     * @param outputSet the CGM subset to write, or null if writing a map
     * @param format the output format (not null)
     * @param baseFilePath base file path (not null, not empty)
     */
    public static void confirmOverwrite(String actionPrefix,
            CgmOutputSet outputSet, CgmOutputFormat format,
            String baseFilePath) {
        Validate.nonEmpty(actionPrefix, "action prefix");
        Validate.nonNull(format, "format");
        Validate.nonEmpty(baseFilePath, "base file path");

        String action;
        if (outputSet == null) {
            action = actionPrefix + format + " " + baseFilePath;
        } else {
            action = actionPrefix + outputSet + " " + format
                    + " " + baseFilePath;
        }
        String filePath = format.extend(baseFilePath);
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
     * Display a "delete animation" dialog.
     */
    public static void deleteAnimation() {
        String name = Maud.getModel().getTarget().getAnimation().name();
        String message = String.format("Delete the %s animation?",
                MyString.quote(name));
        DialogController controller = new MinimalDialog();
        Maud.gui.closeAllPopups();
        Maud.gui.showConfirmDialog(message, "Delete", Action.deleteAnimation,
                controller);
    }

    /**
     * Display a "delete nextKeyframes" dialog.
     */
    public static void deleteNextKeyframes() {
        Cgm target = Maud.getModel().getTarget();
        int numFrames = target.getTrack().countKeyframes();
        int frameIndex = target.getFrame().findIndex();
        int max = numFrames - frameIndex - 2;
        IntegerDialog controller = new IntegerDialog("Delete", 1, max, false);
        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter the number of frames to delete:",
                "1", ActionPrefix.deleteNextKeyframes, controller);
    }

    /**
     * Display a "delete previousKeyframes" dialog.
     */
    public static void deletePreviousKeyframes() {
        SelectedFrame frame = Maud.getModel().getTarget().getFrame();
        int frameIndex = frame.findIndex();
        int max = frameIndex - 1;
        IntegerDialog controller = new IntegerDialog("Delete", 1, max, false);
        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter the number of frames to delete:",
                "1", ActionPrefix.deletePreviousKeyframes, controller);
    }

    /**
     * Display a "delete sgc" dialog.
     */
    public static void deleteSgc() {
        String name = Maud.getModel().getTarget().getSgc().name();
        String message
                = String.format("Delete the %s control?", MyString.quote(name));
        DialogController controller = new MinimalDialog();
        Maud.gui.closeAllPopups();
        Maud.gui.showConfirmDialog(message, "Delete", Action.deleteSgc,
                controller);
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
            bodyText = String.format("Your copy of the %s license is missing!",
                    licenseName);
        } else {
            bodyText = String.format(
                    "Here's your copy of the %s license:%n%n%s%n",
                    licenseName, licenseText);
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
        extList.add(".iqe");
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
     * Display a "new animation" dialog to enter a new animation name.
     *
     * @param actionPrefix action prefix (not null, not empty)
     * @param commitDescription commit description (not null, not empty)
     * @param defaultName default animation name (not null)
     */
    public static void newAnimation(String actionPrefix,
            String commitDescription, String defaultName) {
        Validate.nonEmpty(actionPrefix, "action prefix");
        Validate.nonEmpty(commitDescription, "commit description");
        Validate.nonNull(defaultName, "default name");

        DialogController controller
                = new AnimationNameDialog(commitDescription);
        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter a name for the new animation:",
                defaultName, actionPrefix, controller);
    }

    /**
     * Display a "new animation fromChain" dialog to enter the new animation
     * name.
     *
     * @param which1 which C-G model loaded the animation to go first (not null)
     * @param which2 which C-G model loaded the animation to go 2nd (not null)
     */
    public static void newAnimationFromChain(WhichCgm which1, WhichCgm which2) {
        Validate.nonNull(which1, "first animation's model");
        Validate.nonNull(which2, "2nd animation's model");

        EditorModel model = Maud.getModel();
        String animationName1 = model.getCgm(which1).getAnimation().name();
        String animationName2 = model.getCgm(which2).getAnimation().name();
        String defaultName = animationName1 + "," + animationName2;
        String actionPrefix = ActionPrefix.newAnimationFromChain + which1 + " "
                + which2 + " ";

        newAnimation(actionPrefix, "Chain", defaultName);
    }

    /**
     * Display a "new animation fromMix" dialog to select the tracks.
     */
    public static void newAnimationFromMix() {
        Cgm target = Maud.getModel().getTarget();
        List<TrackItem> items = target.listTrackItems();
        MixDialog controller = new MixDialog(items);

        String prompt = "Select tracks to include in the mix:";
        String prefix = ActionPrefix.newAnimationFromMix;
        Maud.gui.closeAllPopups();
        Maud.gui.showMultiSelectDialog(prompt, prefix, controller);
    }

    /**
     * Display a "new animation fromMix" dialog to enter the new animation name.
     *
     * @param actionPrefix (not null, not empty)
     */
    public static void newAnimationFromMix(String actionPrefix) {
        Validate.nonEmpty(actionPrefix, "action prefix");
        newAnimation(actionPrefix, "Create", "mix");
    }

    /**
     * Display a "new geometry" dialog to name a new Geometry.
     *
     * @param actionPrefix (not null, not empty)
     */
    public static void newGeometry(String actionPrefix) {
        Validate.nonEmpty(actionPrefix, "action prefix");

        DialogController controller = new SpatialNameDialog("Add geometry");
        String defaultName = "new geometry";

        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter a name for the new geometry:",
                defaultName, actionPrefix, controller);
    }

    /**
     * Display a dialog to select 2 or more geometries for a merge. TODO
     * re-order methods
     *
     * @param actionPrefix (not null, not empty)
     */
    public static void mergeGeometries(String actionPrefix) {
        Validate.nonEmpty(actionPrefix, "action prefix");

        SelectedSpatial ss = Maud.getModel().getTarget().getSpatial();
        List<GeometryItem> items = ss.listGeometryItems();
        MergeDialog controller = new MergeDialog(items);

        String prompt = "Select 2 or more geometries to include in the merge:";
        Maud.gui.closeAllPopups();
        Maud.gui.showMultiSelectDialog(prompt, actionPrefix, controller);
    }

    /**
     * Display a "new light" dialog to name a new light.
     *
     * @param actionPrefix (not null, not empty)
     */
    public static void newLight(String actionPrefix) {
        Validate.nonEmpty(actionPrefix, "action prefix");

        DialogController controller = new LightNameDialog("Add light");
        String defaultName = "new light";

        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter a name for the new light:",
                defaultName, actionPrefix, controller);
    }

    /**
     * Display a "new node" dialog to name a new scene-graph node.
     *
     * @param actionPrefix (not null, not empty)
     */
    public static void newNode(String actionPrefix) {
        Validate.nonEmpty(actionPrefix, "action prefix");

        DialogController controller = new SpatialNameDialog("Add node");
        String defaultName = "new node";

        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter a name for the new node:",
                defaultName, actionPrefix, controller);
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
            IntegerDialog controller = new IntegerDialog("Reduce", 2,
                    Integer.MAX_VALUE, false);

            Maud.gui.closeAllPopups();
            Maud.gui.showTextEntryDialog("Enter reduction factor:", "2",
                    ActionPrefix.reduceAnimation, controller);
        }
    }

    /**
     * Display a "reduce track" dialog to enter the reduction factor.
     */
    public static void reduceTrack() {
        SelectedTrack track = Maud.getModel().getTarget().getTrack();
        if (track.isSelected()) {
            IntegerDialog controller
                    = new IntegerDialog("Reduce", 2, Integer.MAX_VALUE, false);

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
            String oldName = animation.name();
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
            String oldName = bone.name();
            DialogController controller = new BoneRenameDialog("Rename");

            Maud.gui.closeAllPopups();
            Maud.gui.showTextEntryDialog("Enter new name for the bone:",
                    oldName, ActionPrefix.renameBone, controller);
        }
    }

    /**
     * Display a "rename light" dialog.
     */
    public static void renameLight() {
        SelectedLight light = Maud.getModel().getTarget().getLight();
        if (light.isSelected()) {
            String oldName = light.name();
            DialogController controller = new LightNameDialog("Rename");

            Maud.gui.closeAllPopups();
            Maud.gui.showTextEntryDialog("Enter a new name for the light:",
                    oldName, ActionPrefix.renameLight, controller);
        }
    }

    /**
     * Display a "rename material" dialog.
     */
    public static void renameMaterial() {
        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        if (spatial.hasMaterial()) {
            String oldName = spatial.getMaterialName();
            if (oldName == null) {
                oldName = "";
            }
            DialogController controller = new MaterialNameDialog("Rename");

            Maud.gui.closeAllPopups();
            Maud.gui.showTextEntryDialog("Enter a new name for the material:",
                    oldName, ActionPrefix.renameMaterial, controller);
        }
    }

    /**
     * Display a "rename override" dialog.
     */
    public static void renameOverride() {
        SelectedOverride override = Maud.getModel().getTarget().getOverride();
        if (override.isSelected()) {
            String oldName = override.parameterName();
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
        String oldName = target.getUserData().key();
        if (oldName != null) {
            DialogController controller = new UserKeyDialog("Rename");
            Maud.gui.closeAllPopups();
            Maud.gui.showTextEntryDialog("Enter a new key for the user data:",
                    oldName, ActionPrefix.renameUserKey, controller);
        }
    }

    /**
     * Display a "reparent spatials" dialog to select the spatials.
     */
    public static void reparentSpatials() {
        SelectedSpatial ss = Maud.getModel().getTarget().getSpatial();
        List<SpatialItem> allItems = ss.listReparentItems();
        Collections.sort(allItems);
        ReparentDialog controller = new ReparentDialog(allItems);

        String prompt = "Select spatials to reparent:";
        String prefix = ActionPrefix.reparentSpatials;
        Maud.gui.closeAllPopups();
        Maud.gui.showMultiSelectDialog(prompt, prefix, controller);
    }

    /**
     * Display a "rename matchingTextures" dialog.
     */
    public static void replaceTextures() {
        String promptMessage = "Enter match;replacement for texture assets:";
        String defaultPattern = "textures;textures";
        DialogController controller = new TextEntryDialog("Replace");

        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog(promptMessage, defaultPattern,
                ActionPrefix.renameMatchingTextures, controller);
    }

    /**
     * Display a "resample animation/track" dialog.
     *
     * @param actionPrefix an action prefix (not null)
     * @param type how to generate frame times (not null)
     */
    public static void resample(String actionPrefix, ResampleType type) {
        Validate.nonEmpty(actionPrefix, "action prefix");

        switch (type) {
            case AtRate:
                resampleAtRate(actionPrefix);
                break;

            case ToNumber:
                resampleToNumber(actionPrefix);
                break;

            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * Display a dialog to enter the base file path for saving or exporting the
     * target CGM.
     *
     * @param commitDescription label text for the commit button (not null, not
     * empty, should fit the button)
     * @param actionPrefix action prefix (not null, not empty)
     */
    public static void saveCgm(String commitDescription, String actionPrefix) {
        Validate.nonEmpty(commitDescription, "commit description");
        Validate.nonEmpty(actionPrefix, "action prefix");

        EditableCgm target = Maud.getModel().getTarget();
        String baseFilePath = target.baseFilePathForWrite();
        DialogController controller = new TextEntryDialog(commitDescription);
        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter base file path for model:",
                baseFilePath, actionPrefix, controller);
    }

    /**
     * Display a dialog to enter the base file path for saving or exporting the
     * SkeletonMap.
     *
     * @param commitDescription label text for the commit button (not null, not
     * empty, should fit the button)
     * @param actionPrefix action prefix (not null, not empty)
     */
    public static void saveMap(String commitDescription, String actionPrefix) {
        Validate.nonEmpty(commitDescription, "commit description");
        Validate.nonEmpty(actionPrefix, "action prefix");

        String baseFilePath = Maud.getModel().getMap().baseFilePathForWrite();
        DialogController controller = new TextEntryDialog(commitDescription);
        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter base file path for map:",
                baseFilePath, actionPrefix, controller);
    }

    /**
     * Display a dialog to enter the new asset path and flipY flag for saving
     * the selected Texture.
     */
    public static void saveTexture() {
        List<String> extList = new ArrayList<>(5);
        extList.add(".bmp");
        extList.add(".gif");
        extList.add(".jpeg");
        extList.add(".jpg");
        extList.add(".png");
        NewTextureDialog controller
                = new NewTextureDialog("Save", "flipY", extList);
        Maud.gui.closeAllPopups();

        SelectedTexture selTexture = Maud.getModel().getTarget().getTexture();
        String defaultAssetPath = selTexture.lastAssetPath();
        Maud.gui.showTextAndCheckDialog("Enter new asset path for texture:",
                defaultAssetPath, ActionPrefix.saveTexture, controller);
    }

    /**
     * Display a "select boneIndex " dialog to enter the bone index.
     */
    public static void selectBoneIndex() {
        EditorModel model = Maud.getModel();
        Cgm target = model.getTarget();
        int numBones = target.getSkeleton().countBones();
        int indexBase = model.getMisc().indexBase();
        DialogController controller = new IntegerDialog("Select", indexBase,
                numBones + indexBase - 1, false);

        int oldIndex = target.getBone().index();
        int defaultIndex = (oldIndex == -1) ? indexBase : oldIndex;
        String defaultText = Integer.toString(defaultIndex);

        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter the index of the bone:",
                defaultText, ActionPrefix.selectBoneIndex, controller);
    }

    /**
     * Display a "select keyframe " dialog to enter the keyframe index.
     */
    public static void selectKeyframe() {
        EditorModel model = Maud.getModel();
        Cgm target = model.getTarget();
        int numKeyframes = target.getTrack().countKeyframes();
        int indexBase = model.getMisc().indexBase();
        DialogController controller = new IntegerDialog("Select", indexBase,
                numKeyframes + indexBase - 1, false);

        int oldIndex = target.getVertex().getIndex();
        int defaultIndex = (oldIndex == -1) ? indexBase : oldIndex;
        String defaultText = Integer.toString(defaultIndex);

        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter the index of the keyframe:",
                defaultText, ActionPrefix.selectKeyframe, controller);
    }

    /**
     * Display a "select vertex " dialog to enter the vertex index.
     */
    public static void selectVertex() {
        EditorModel model = Maud.getModel();
        Cgm target = model.getTarget();
        int numVertices = target.getSpatial().countVertices();
        int indexBase = model.getMisc().indexBase();
        DialogController controller = new IntegerDialog("Select", indexBase,
                numVertices + indexBase - 1, false);

        int oldIndex = target.getVertex().getIndex();
        int defaultIndex = (oldIndex == -1) ? indexBase : oldIndex;
        String defaultText = Integer.toString(defaultIndex);

        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter the index of the vertex:",
                defaultText, ActionPrefix.selectVertex, controller);
    }

    /**
     * Display a "set anisotropy " dialog to enter the anisotropy of the
     * selected texture.
     */
    public static void setAnisotropy() {
        SelectedTexture texture = Maud.getModel().getTarget().getTexture();
        if (!texture.hasKey()) {
            return;
        }

        int oldValue = texture.anisotropy();
        String defaultText = Integer.toString(oldValue);

        DialogController controller = new IntegerDialog("Select", 0,
                Integer.MAX_VALUE, false);
        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter the anisotropy:", defaultText,
                ActionPrefix.setAnisotropy, controller);
    }

    /**
     * Display a "set linkMass " dialog to enter the new mass. TODO re-order
     * methods
     */
    public static void setLinkMass() {
        DialogController controller = new FloatDialog("Set", Float.MIN_VALUE,
                Float.MAX_VALUE, false);

        SelectedLink boneLink = Maud.getModel().getTarget().getLink();
        float oldMass = boneLink.mass();
        String defaultText = Float.toString(oldMass);

        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter the mass:", defaultText,
                ActionPrefix.setLinkMass, controller);
    }

    /**
     * Display a "set bufferInstanceSpan " dialog to enter the new span.
     */
    public static void setBufferInstanceSpan() {
        DialogController controller = new IntegerDialog("Set", 0,
                Integer.MAX_VALUE, false);

        SelectedBuffer buffer = Maud.getModel().getTarget().getBuffer();
        int oldSpan = buffer.instanceSpan();
        String defaultText = Integer.toString(oldSpan);

        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter the instance span:", defaultText,
                ActionPrefix.setBufferInstanceSpan, controller);
    }

    /**
     * Display a "set bufferLimit " dialog to enter the new buffer limit.
     */
    public static void setBufferLimit() {
        SelectedBuffer buffer = Maud.getModel().getTarget().getBuffer();
        int capacity = buffer.capacity();
        DialogController controller
                = new IntegerDialog("Set", 1, capacity, false);

        int oldLimit = buffer.limit();
        String defaultText = Integer.toString(oldLimit);

        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter the buffer limit:", defaultText,
                ActionPrefix.setBufferLimit, controller);
    }

    /**
     * Display a "set bufferStride " dialog to enter the new buffer stride.
     */
    public static void setBufferStride() {
        DialogController controller = new IntegerDialog("Set", 0, 999, false);

        SelectedBuffer buffer = Maud.getModel().getTarget().getBuffer();
        int oldStride = buffer.stride();
        String defaultText = Integer.toString(oldStride);

        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter the buffer stride in bytes:",
                defaultText, ActionPrefix.setBufferStride, controller);
    }

    /**
     * Display a "set dumpIndentSpaces" dialog.
     */
    public static void setDumpIndentSpaces() {
        String indentIncrement = Maud.getModel().getDumper().indentIncrement();
        String defaultText = Integer.toString(indentIncrement.length());

        DialogController controller = new IntegerDialog("Set", 0, 40, false);

        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter indent in spaces:",
                defaultText, ActionPrefix.setDumpIndentSpaces, controller);
    }

    /**
     * Display a "set dumpMaxChildren" dialog.
     */
    public static void setDumpMaxChildren() {
        int maxChildren = Maud.getModel().getDumper().maxChildren();
        String defaultText = Integer.toString(maxChildren);

        DialogController controller
                = new IntegerDialog("Set", 0, Integer.MAX_VALUE, false);

        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter max number of children:",
                defaultText, ActionPrefix.setDumpMaxChildren, controller);
    }

    /**
     * Display a "set duration proportional" dialog.
     */
    public static void setDurationProportional() {
        LoadedAnimation animation = Maud.getModel().getTarget().getAnimation();
        float oldDuration = animation.duration();
        String defaultText = Float.toString(oldDuration);

        float min;
        float finalTime = animation.findLatestKeyframe();
        if (finalTime > 0f) {
            min = 0.01f;
        } else {
            min = 0f;
        }
        DialogController controller
                = new FloatDialog("Set", min, Float.MAX_VALUE, false);

        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter new duration in seconds:",
                defaultText, ActionPrefix.setDurationProportional, controller);
    }

    /**
     * Display a "set duration same" dialog.
     */
    public static void setDurationSame() {
        LoadedAnimation animation = Maud.getModel().getTarget().getAnimation();
        float oldDuration = animation.duration();
        String defaultText = Float.toString(oldDuration);

        float finalTime = animation.findLatestKeyframe();
        DialogController controller
                = new FloatDialog("Extend", finalTime, Float.MAX_VALUE, false);

        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter new duration in seconds:",
                defaultText, ActionPrefix.setDurationSame, controller);
    }

    /**
     * Display a "set defaultMargin" dialog.
     */
    public static void setDefaultMargin() {
        float oldValue = CollisionShape.getDefaultMargin();
        String defaultValue = Float.toString(oldValue);

        FloatDialog controller = new FloatDialog("Set", Float.MIN_VALUE,
                Float.MAX_VALUE, false);
        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter margin for new physics shapes:",
                defaultValue, ActionPrefix.setDefaultMargin, controller);
    }

    /**
     * Display a "set frameTime" dialog.
     */
    public static void setFrameTime() {
        Cgm target = Maud.getModel().getTarget();
        SelectedFrame frame = target.getFrame();
        int frameIndex = frame.findIndex();
        assert frameIndex > 0 : frameIndex;
        SelectedTrack track = target.getTrack();
        assert track.isSelected();
        float minTime = track.keyframeTime(frameIndex - 1);

        int numFrames = track.countKeyframes();
        float maxTime;
        if (frameIndex < numFrames - 1) {
            maxTime = track.keyframeTime(frameIndex + 1);
        } else {
            maxTime = target.getAnimation().duration();
        }

        float delta = maxTime - minTime;
        assert delta > 0f : delta;
        maxTime -= delta / 100f;
        minTime += delta / 100f;

        float oldTime = target.getPlay().getTime();
        String defaultValue = Float.toString(oldTime);

        FloatDialog controller
                = new FloatDialog("Adjust", minTime, maxTime, false);
        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter new time for the frame in seconds:",
                defaultValue, ActionPrefix.setFrameTime, controller);
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
                if (allowNull) {
                    if (oldValue == null) {
                        defaultValue = "null";
                    } else {
                        boolean booleanValue = (boolean) oldValue;
                        defaultValue = Boolean.toString(booleanValue);
                    }
                    controller = new BooleanDialog("Set", allowNull);
                    promptMessage = "Enter new boolean value:";

                } else { // only 2 possible values, so toggle immediately
                    boolean newValue = !(boolean) oldValue;
                    String action = actionPrefix + newValue;
                    Maud.perform(action);
                    return;
                }
                break;

            case Float:
                if (oldValue == null) {
                    defaultValue = "0.0";
                } else {
                    float floatValue = (float) oldValue;
                    defaultValue = Float.toString(floatValue);
                }
                controller = new FloatDialog("Set", Float.NEGATIVE_INFINITY,
                        Float.POSITIVE_INFINITY, allowNull);
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
                controller = new IntegerDialog("Set", minValue, maxValue,
                        allowNull);
                promptMessage = "Enter new integer value:";
                break;

            case Texture2D:
            case Texture3D:
            case TextureArray:
            case TextureCubeMap:
                /*
                 * Select the texture in the texture tool.
                 */
                Cgm target = Maud.getModel().getTarget();
                MatParamRef ref;
                if (allowNull) {
                    ref = target.getOverride().makeRef();
                } else {
                    ref = target.getMatParam().makeRef();
                }
                target.getTexture().select(ref);
                EditorTools.select("texture");
                return;

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
     * Display a "set maxCheckpoints " dialog to enter the new limit.
     */
    public static void setMaxCheckpoints() {
        DialogController controller = new IntegerDialog("Set", 2,
                Integer.MAX_VALUE, false);

        int oldLimit = Maud.getModel().getMisc().maxCheckpoints();
        String defaultText = Integer.toString(oldLimit);

        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter the desired checkpoint limit:",
                defaultText, ActionPrefix.setMaxCheckpoints, controller);
    }

    /**
     * Display a "set physicsRbpValue" dialog to enter the parameter value.
     *
     * @param parameter which rigid-body parameter to alter (not null)
     */
    public static void setPhysicsRbpValue(RigidBodyParameter parameter) {
        Validate.nonNull(parameter, "parameter");

        SelectedPco object = Maud.getModel().getTarget().getPco();
        String parameterName = parameter.toString();
        if (object.canSet(parameterName)) {
            float minValue = parameter.minValue();
            float maxValue = parameter.maxValue();
            DialogController controller
                    = new FloatDialog("Set", minValue, maxValue, false);

            String prompt = String.format("Enter new %s:", parameterName);
            String defaultText = object.value(parameter);
            String prefix
                    = ActionPrefix.setPcoParmValue + parameterName + " ";
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
            float minValue = parameter.minValue();
            float maxValue = parameter.maxValue();
            DialogController controller
                    = new FloatDialog("Set", minValue, maxValue, false);

            String prompt = String.format("Enter new %s:", parameter);
            float defaultValue = shape.value(parameter);
            String defaultText = "";
            if (!Float.isNaN(defaultValue)) {
                defaultText = Float.toString(defaultValue);
            }
            String name = parameter.toString();
            String prefix = ActionPrefix.setShapeParmValue + name + " ";
            Maud.gui.closeAllPopups();
            Maud.gui.showTextEntryDialog(prompt, defaultText, prefix,
                    controller);
        }
    }

    /**
     * Display a "set time" dialog to enter a time in seconds. TODO re-order
     * methods
     *
     * @param whichCgm which CGM's options to modify (not null)
     * @param whichTime which time to modify in the options (not null)
     */
    public static void setTime(WhichCgm whichCgm, PlayTimes whichTime) {
        Cgm cgm = Maud.getModel().getCgm(whichCgm);
        PlayOptions options = cgm.getPlay();
        float lowerLimit = options.getLowerLimit();
        float upperLimit = options.getUpperLimit();
        if (cgm.getAnimation().isReal()) {
            float duration = cgm.getAnimation().duration();
            upperLimit = Math.min(upperLimit, duration);
        }

        float defaultValue, minValue, maxValue;
        switch (whichTime) {
            case LowerLimit:
                minValue = 0f;
                defaultValue = lowerLimit;
                maxValue = upperLimit;
                break;

            case UpperLimit:
                minValue = lowerLimit;
                defaultValue = upperLimit;
                maxValue = Float.MAX_VALUE;
                break;

            default:
                throw new IllegalArgumentException();
        }

        String defaultText = Float.toString(defaultValue);
        String actionPrefix = String.format("%s%s %s ", ActionPrefix.setTime,
                whichCgm, whichTime);
        FloatDialog controller
                = new FloatDialog("Set", minValue, maxValue, false);

        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter new time in seconds:", defaultText,
                actionPrefix, controller);
    }

    /**
     * Display a "set timeToFrame" dialog to enter a keyframe index.
     *
     * @param whichCgm which CGM's options to modify (not null)
     * @param whichTime which time to modify in the options (not null)
     */
    public static void setTimeToKeyframe(WhichCgm whichCgm,
            PlayTimes whichTime) {
        EditorModel model = Maud.getModel();
        Cgm cgm = model.getCgm(whichCgm);
        SelectedTrack track = cgm.getTrack();
        assert track.isSelected();

        PlayOptions options = cgm.getPlay();
        float lowerLimit = options.getLowerLimit();
        float upperLimit = options.getUpperLimit();
        float duration = cgm.getAnimation().duration();
        upperLimit = Math.min(upperLimit, duration);
        int numFrames = track.countKeyframes();

        int minIndex, maxIndex;
        switch (whichTime) {
            case LowerLimit:
                minIndex = 0;
                maxIndex = track.findPreviousKeyframeIndex(upperLimit);
                break;

            case UpperLimit:
                minIndex = track.findNextKeyframeIndex(lowerLimit);
                maxIndex = numFrames - 1;
                break;

            default:
                throw new IllegalArgumentException();
        }

        int indexBase = model.getMisc().indexBase();
        String defaultText = "";
        int defaultIndex = cgm.getFrame().findIndex();
        if (defaultIndex != -1) {
            defaultText = Float.toString(indexBase + defaultIndex);
        }
        String actionPrefix = String.format("%s%s %s ",
                ActionPrefix.setTimeToFrame, whichCgm, whichTime);
        IntegerDialog controller = new IntegerDialog("Set",
                indexBase + minIndex, indexBase + maxIndex, false);

        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter a keyframe index:", defaultText,
                actionPrefix, controller);
    }

    /**
     * Display a "set spatial rotation" dialog.
     */
    public static void setSpatialRotation() {
        Maud.gui.closeAllPopups();
        EditorModel model = Maud.getModel();
        EditableCgm target = model.getTarget();

        Quaternion q = target.getSpatial().localRotation(null);
        float[] tmpArray = new float[3];
        Vector3f oldValue = new Vector3f();
        RotationDisplayMode mode = model.getMisc().rotationDisplayMode();
        switch (mode) {
            case Degrees:
                q.toAngles(tmpArray);
                oldValue.set(tmpArray[0], tmpArray[1], tmpArray[2]);
                oldValue.multLocal(FastMath.RAD_TO_DEG);
                break;
            case Radians:
                q.toAngles(tmpArray);
                oldValue.set(tmpArray[0], tmpArray[1], tmpArray[2]);
                break;
            case QuatCoeff:
                oldValue.set(q.getX(), q.getY(), q.getZ());
                break;
            default:
                throw new IllegalStateException("mode = " + mode);
        }
        String defaultText = oldValue.toString();
        DialogController controller = new VectorDialog("Set", 3, false);
        Maud.gui.showTextEntryDialog("Enter new rotation vector:",
                defaultText, ActionPrefix.setSpatialRotation, controller);
    }

    /**
     * Display a "set spatial scale" dialog.
     */
    public static void setSpatialScale() {
        Maud.gui.closeAllPopups();
        EditableCgm target = Maud.getModel().getTarget();
        Vector3f oldValue = target.getSpatial().localScale(null);
        String defaultText = oldValue.toString();
        DialogController controller = new VectorDialog("Set", 3, false);
        Maud.gui.showTextEntryDialog("Enter new scale vector:",
                defaultText, ActionPrefix.setSpatialScale, controller);
    }

    /**
     * Display a "set spatial translation" dialog.
     */
    public static void setSpatialTranslation(String actionPrefix) {
        Maud.gui.closeAllPopups();
        EditableCgm target = Maud.getModel().getTarget();
        Vector3f oldValue = target.getSpatial().localTranslation(null);
        String defaultText = oldValue.toString();
        DialogController controller = new VectorDialog("Set", 3, false);
        Maud.gui.showTextEntryDialog("Enter new translation vector:",
                defaultText, actionPrefix, controller);
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
            target.getUserData().setValue(newText);

        } else if (value instanceof Float) {
            float oldValue = (float) value;
            controller = new FloatDialog("Set", Float.NEGATIVE_INFINITY,
                    Float.POSITIVE_INFINITY, false);
            defaultText = Float.toString(oldValue);
            Maud.gui.showTextEntryDialog("Enter new float value:", defaultText,
                    ActionPrefix.setUserData, controller);

        } else if (value instanceof Integer) {
            int oldValue = (int) value;
            controller = new IntegerDialog("Set", Integer.MIN_VALUE,
                    Integer.MAX_VALUE, false);
            defaultText = Integer.toString(oldValue);
            Maud.gui.showTextEntryDialog("Enter new integer value:",
                    defaultText, ActionPrefix.setUserData, controller);

        } else if (value instanceof Long) {
            long oldValue = (long) value;
            controller = new LongDialog("Set", Long.MIN_VALUE, Long.MAX_VALUE,
                    false);
            defaultText = Long.toString(oldValue);
            Maud.gui.showTextEntryDialog("Enter new long integer value:",
                    defaultText, ActionPrefix.setUserData, controller);

        } else if (value instanceof String) {
            controller = new TextEntryDialog("Set");
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

    /**
     * Display a "set vertexData" dialog.
     *
     * @param componentIndex (0-3)
     */
    public static void setVertexData(int componentIndex) {
        Validate.inRange(componentIndex, "component index", 0, 3);

        Maud.gui.closeAllPopups();
        EditableCgm target = Maud.getModel().getTarget();
        float oldValue = target.getVertex().floatComponent(componentIndex);
        DialogController controller = new FloatDialog("Set",
                Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, false);
        String defaultText = Float.toString(oldValue);
        String actionPrefix = ActionPrefix.setVertexData
                + Integer.toString(componentIndex) + " ";
        Maud.gui.showTextEntryDialog("Enter new float value:", defaultText,
                actionPrefix, controller);
    }

    /**
     * Display a "wrap animation" dialog.
     */
    public static void wrapAnimation() {
        FloatDialog controller
                = new FloatDialog("Wrap animation", 0f, 1f, false);
        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter weight for end-time keyframes:",
                "0", ActionPrefix.wrapAnimation, controller);
    }

    /**
     * Display a "wrap track" dialog.
     */
    public static void wrapTrack() {
        FloatDialog controller = new FloatDialog("Wrap track", 0f, 1f, false);
        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter weight for end-time keyframe:", "0",
                ActionPrefix.wrapTrack, controller);
    }
    // *************************************************************************
    // private methods

    /**
     * Display a "resample animation/track AtRate" dialog.
     *
     * @param actionPrefix an action prefix (not null)
     */
    private static void resampleAtRate(String actionPrefix) {
        assert actionPrefix != null;

        FloatDialog controller
                = new FloatDialog("Resample", 0.1f, 1000f, false);
        String prefix = actionPrefix + ResampleType.AtRate + " ";
        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter samples per second:", "10", prefix,
                controller);
    }

    /**
     * Display a "resample animation/track ToNumber" dialog.
     *
     * @param actionPrefix an action prefix (not null)
     */
    private static void resampleToNumber(String actionPrefix) {
        assert actionPrefix != null;

        IntegerDialog controller = new IntegerDialog("Resample", 2, 999, false);
        String prefix = actionPrefix + ResampleType.ToNumber + " ";
        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter number of samples:", "17", prefix,
                controller);
    }
}
