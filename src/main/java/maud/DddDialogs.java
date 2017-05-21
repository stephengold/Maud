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
package maud;

import com.jme3.asset.AssetManager;
import com.jme3.system.JmeVersion;
import de.lessvoid.nifty.Nifty;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyString;
import jme3utilities.debug.DebugVersion;
import jme3utilities.nifty.DialogController;
import jme3utilities.nifty.LibraryVersion;
import jme3utilities.sky.Constants;
import jme3utilities.ui.UiVersion;

/**
 * Dialog boxes created by Maud's "3D View" screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class DddDialogs {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            DddDialogs.class.getName());
    // *************************************************************************
    // fields

    /**
     * asset manager for loading assets (set by constructor}
     */
    final private AssetManager assetManager;
    /**
     * Nifty GUI instance (set by constructor}
     */
    final private Nifty nifty;
    // *************************************************************************
    // constructors

    /**
     * Instantiate with the specified asset manager and Nifty GUI instance.
     *
     * @param assetManager (not null)
     */
    DddDialogs(AssetManager assetManager, Nifty nifty) {
        this.assetManager = assetManager;
        this.nifty = nifty;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Display an "About Maud" dialog.
     */
    void aboutMaud() {
        String niftyVersion = nifty.getVersion();
        String text = "Maud, by Stephen Gold\n\nYou are c"
                + "urrently using Maud, a jMonkeyEngine application for edit"
                + "ing animated models.\n\nThe version you are using includes "
                + "the following libraries:";
        text += String.format("\n   jme3-core version %s",
                MyString.quote(JmeVersion.FULL_NAME));
        text += String.format("\n   nifty version %s",
                MyString.quote(niftyVersion));
        text += String.format("\n   jme3-utilities-heart version %s",
                MyString.quote(Misc.getVersionShort()));
        text += String.format("\n   SkyControl version %s",
                MyString.quote(Constants.getVersionShort()));
        text += String.format("\n   jme3-utilities-debug version %s",
                MyString.quote(DebugVersion.getVersionShort()));
        text += String.format("\n   jme3-utilities-ui version %s",
                MyString.quote(UiVersion.getVersionShort()));
        text += String.format("\n   jme3-utilities-nifty version %s\n\n",
                MyString.quote(LibraryVersion.getVersionShort()));

        Maud.gui.closeAllPopups();
        Maud.gui.showInfoDialog("About Maud", text);
    }

    /**
     * Display a "copy animation" dialog.
     */
    void copyAnimation() {
        String fromName = Maud.model.animation.getName();
        DialogController controller = new AnimationNameDialog("Copy");

        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter name for copied animation:",
                fromName, "", DddInputMode.copyAnimationPrefix, controller);
    }

    /**
     * Display a "load model asset" dialog.
     */
    void loadModelAsset() {
        String basePath = Maud.model.cgm.getAssetPath();
        String extension = Maud.model.cgm.getExtension();
        String assetPath = String.format("%s.%s", basePath, extension);
        List<String> modelExts = new ArrayList<>(4);
        modelExts.add(".blend");
        modelExts.add(".j3o");
        modelExts.add(".mesh.xml");
        AssetDialog controller = new AssetDialog("Load", modelExts,
                assetManager);

        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter asset path for model:", assetPath,
                "", DddInputMode.loadModelAssetPrefix, controller);
    }

    /**
     * Display a "new pose" dialog.
     */
    void newPose() {
        DialogController controller = new AnimationNameDialog("Create");

        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter a name for the new animation:",
                "pose", "", DddInputMode.newPosePrefix, controller);
    }

    /**
     * Display a "rename animation" dialog.
     */
    void renameAnimation() {
        if (!Maud.model.animation.isBindPoseLoaded()) {
            String oldName = Maud.model.animation.getName();
            DialogController controller = new AnimationNameDialog("Rename");

            Maud.gui.closeAllPopups();
            Maud.gui.showTextEntryDialog("Enter new name for the animation:",
                    oldName, "", DddInputMode.renameAnimationPrefix,
                    controller);
        }
    }

    /**
     * Display a "rename bone" dialog.
     */
    void renameBone() {
        if (Maud.model.bone.isBoneSelected()) {
            String oldName = Maud.model.bone.getName();
            DialogController controller = new BoneRenameDialog("Rename");

            Maud.gui.closeAllPopups();
            Maud.gui.showTextEntryDialog("Enter new name for the bone:",
                    oldName, "", DddInputMode.renameBonePrefix, controller);
        }
    }

    /**
     * Display a "retarget animation" dialog.
     */
    void retargetAnimation() {
        String oldName = Maud.model.retarget.getTargetAnimationName();
        if (oldName == null) {
            oldName = "";
        }
        DialogController controller = new AnimationNameDialog("Retarget");

        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter a name for the new animation:",
                oldName, "", DddInputMode.retargetAnimationPrefix, controller);
    }

    /**
     * Display a "select retarget map asset" dialog.
     */
    void selectRetargetMapAsset() {
        String assetPath = Maud.model.retarget.getMappingAssetPath();
        List<String> modelExts = new ArrayList<>(1);
        modelExts.add(".j3o");
        AssetDialog controller = new AssetDialog("Select", modelExts,
                assetManager);

        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter asset path for skeleton mapping:",
                assetPath, "", DddInputMode.selectRetargetMapAssetPrefix,
                controller);
    }

    /**
     * Display a "select retarget source cgm asset" dialog.
     */
    void selectRetargetSourceCgmAsset() {
        String assetPath = Maud.model.retarget.getSourceCgmAssetPath();
        List<String> modelExts = new ArrayList<>(4);
        modelExts.add(".blend");
        modelExts.add(".j3o");
        modelExts.add(".mesh.xml");
        AssetDialog controller = new AssetDialog("Select", modelExts,
                assetManager);

        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter asset path for source model:",
                assetPath, "", DddInputMode.selectRetargetSourceCgmAssetPrefix,
                controller);
    }

    /**
     * Display a "set duration" dialog.
     */
    void setDuration() {
        float oldDuration = Maud.model.animation.getDuration();
        String defaultText = Float.toString(oldDuration);

        float finalTime = Maud.model.animation.findLatestKeyframe();
        float min;
        if (finalTime > 0f) {
            min = 0.01f;
        } else {
            min = 0f;
        }
        DialogController controller = new FloatDialog("Set", min,
                Float.MAX_VALUE);

        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter new duration in seconds:",
                defaultText, "", DddInputMode.setDurationPrefix, controller);
    }
}
