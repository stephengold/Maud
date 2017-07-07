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

import com.jme3.system.JmeVersion;
import de.lessvoid.nifty.Nifty;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyString;
import jme3utilities.debug.DebugVersion;
import jme3utilities.nifty.DialogController;
import jme3utilities.nifty.FloatDialog;
import jme3utilities.nifty.IntegerDialog;
import jme3utilities.nifty.LibraryVersion;
import jme3utilities.sky.Constants;
import jme3utilities.ui.UiVersion;

/**
 * Dialog boxes created by Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class EditorDialogs {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            EditorDialogs.class.getName());
    // *************************************************************************
    // new methods exposed

    /**
     * Display an "About Maud" dialog.
     */
    void aboutMaud() {
        Nifty nifty = Maud.gui.getNifty();
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
        String fromName = Maud.model.target.animation.getName();
        DialogController controller = new AnimationNameDialog("Copy");

        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter name for copied animation:",
                fromName, EditorInputMode.copyAnimationPrefix, controller);
    }

    /**
     * Display a "delete animation" dialog.
     */
    void deleteAnimation() {
        String name = Maud.model.target.animation.getName();
        String message = String.format("Delete the %s animation?",
                MyString.quote(name));
        Maud.gui.closeAllPopups();
        Maud.gui.showConfirmDialog(message, "Delete", "delete animation", null);
    }

    /**
     * Display a "delete control" dialog.
     */
    void deleteSgc() {
        String name = Maud.model.target.sgc.getName();
        String message;
        message = String.format("Delete the %s control?", MyString.quote(name));
        Maud.gui.closeAllPopups();
        Maud.gui.showConfirmDialog(message, "Delete", "delete control", null);
    }

    /**
     * Display a License infobox.
     *
     * @param actionPrefix for the dialog (not null)
     */
    void license() {
        File licenseFile = new File("LICENSE");
        Scanner scanner = null;
        try {
            scanner = new Scanner(licenseFile).useDelimiter("\\Z");
        } catch (FileNotFoundException e) {
        }
        String text2;
        if (scanner == null) {
            text2 = "Your software license is missing!";
        } else {
            String contents = scanner.next();
            scanner.close();
            text2 = String.format(
                    "Here's your software license for Maud:\n%s\n",
                    contents);
        }
        Maud.gui.closeAllPopups();
        Maud.gui.showInfoDialog("License information", text2);
    }

    /**
     * Display a "new pose" dialog.
     */
    void newPose() {
        DialogController controller = new AnimationNameDialog("Create");
        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter a name for the new animation:",
                "pose", EditorInputMode.newPosePrefix, controller);
    }

    /**
     * Display a "new userKey" dialog.
     */
    void newUserKey(String actionString) {
        DialogController controller = new UserKeyDialog("Create");
        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter a key for the new user data:",
                "key", actionString, controller);
    }

    /**
     * Display a "reduce animation" dialog.
     */
    void reduceAnimation() {
        if (Maud.model.target.animation.isReal()) {
            IntegerDialog controller;
            controller = new IntegerDialog("Reduce", 2, Integer.MAX_VALUE);

            Maud.gui.closeAllPopups();
            Maud.gui.showTextEntryDialog("Enter reduction factor:", "2",
                    EditorInputMode.reduceAnimationPrefix, controller);
        }
    }

    /**
     * Display a "reduce track" dialog.
     */
    void reduceTrack() {
        if (Maud.model.target.bone.hasTrack()) {
            IntegerDialog controller;
            controller = new IntegerDialog("Reduce", 2, Integer.MAX_VALUE);

            Maud.gui.closeAllPopups();
            Maud.gui.showTextEntryDialog("Enter reduction factor:", "2",
                    EditorInputMode.reduceTrackPrefix, controller);
        }
    }

    /**
     * Display a "rename animation" dialog.
     */
    void renameAnimation() {
        if (Maud.model.target.animation.isReal()) {
            String oldName = Maud.model.target.animation.getName();
            DialogController controller = new AnimationNameDialog("Rename");

            Maud.gui.closeAllPopups();
            Maud.gui.showTextEntryDialog("Enter new name for the animation:",
                    oldName, EditorInputMode.renameAnimationPrefix,
                    controller);
        }
    }

    /**
     * Display a "rename bone" dialog.
     */
    void renameBone() {
        if (Maud.model.target.bone.isSelected()) {
            String oldName = Maud.model.target.bone.getName();
            DialogController controller = new BoneRenameDialog("Rename");

            Maud.gui.closeAllPopups();
            Maud.gui.showTextEntryDialog("Enter new name for the bone:",
                    oldName, EditorInputMode.renameBonePrefix, controller);
        }
    }

    /**
     * Display a "rename spatial" dialog.
     */
    void renameSpatial() {
        String oldName = Maud.model.target.spatial.getName();
        DialogController controller = new SpatialNameDialog("Rename");
        String prompt;
        if (Maud.model.target.spatial.isNode()) {
            prompt = "Enter new name for the node:";
        } else {
            prompt = "Enter new name for the geometry:";
        }

        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog(prompt, oldName,
                EditorInputMode.renameSpatialPrefix, controller);
    }

    /**
     * Display a "rename userKey" dialog.
     */
    void renameUserKey() {
        String oldName = Maud.model.misc.getSelectedUserKey();
        if (oldName != null) {
            DialogController controller = new UserKeyDialog("Rename");
            Maud.gui.closeAllPopups();
            Maud.gui.showTextEntryDialog("Enter a new key for the user data:",
                    oldName, EditorInputMode.renameUserKeyPrefix, controller);
        }
    }

    /**
     * Display a "save cgm" dialog.
     */
    void saveCgm() {
        String baseFilePath = Maud.model.target.baseFilePathForWrite();
        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter base file path for model:",
                baseFilePath, "Save", EditorInputMode.saveCgmPrefix, null);
    }

    /**
     * Display a "save mapping" dialog.
     */
    void saveMapping() {
        String baseFilePath = Maud.model.mapping.baseFilePathForWrite();
        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter base file path for mapping:",
                baseFilePath, "Save", EditorInputMode.saveMappingPrefix, null);
    }

    /**
     * Display a "retarget animation" dialog.
     */
    void retargetAnimation() {
        String oldName = Maud.model.source.animation.getName();
        if (oldName == null) {
            oldName = "";
        }
        DialogController controller = new AnimationNameDialog("Retarget");

        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter a name for the new animation:",
                oldName, EditorInputMode.retargetAnimationPrefix, controller);
    }

    /**
     * Display a "set duration" dialog.
     */
    void setDuration() {
        float oldDuration = Maud.model.target.animation.getDuration();
        String defaultText = Float.toString(oldDuration);

        float finalTime = Maud.model.target.animation.findLatestKeyframe();
        float min;
        if (finalTime > 0f) {
            min = 0.01f;
        } else {
            min = 0f;
        }
        DialogController controller;
        controller = new FloatDialog("Set", min, Float.MAX_VALUE);

        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter new duration in seconds:",
                defaultText, EditorInputMode.setDurationPrefix, controller);
    }

    /**
     * Display a "set userData" dialog.
     */
    void setUserData() {
        // TODO
    }
}
