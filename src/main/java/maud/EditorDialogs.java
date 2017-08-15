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
import maud.action.ActionPrefix;
import maud.dialog.AnimationNameDialog;
import maud.dialog.BoneRenameDialog;
import maud.dialog.LongDialog;
import maud.dialog.SpatialNameDialog;
import maud.dialog.TextEntryDialog;
import maud.dialog.UserKeyDialog;
import maud.model.EditableCgm;
import maud.model.LoadedAnimation;
import maud.model.SelectedBone;
import maud.model.SelectedSpatial;

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
    final private static Logger logger = Logger.getLogger(
            EditorDialogs.class.getName());
    // *************************************************************************
    // new methods exposed

    /**
     * Display an "About Maud" dialog.
     */
    public void aboutMaud() {
        Nifty nifty = Maud.gui.getNifty();
        String niftyVersion = nifty.getVersion();
        String text = "Maud, by Stephen Gold\n\nYou are c"
                + "urrently using Maud, a jMonkeyEngine application for edit"
                + "ing animated models.\n\nThe version you are using includes "
                + "the following libraries:";
        text += String.format("%n   jme3-core version %s",
                MyString.quote(JmeVersion.FULL_NAME));
        text += String.format("%n   nifty version %s",
                MyString.quote(niftyVersion));
        text += String.format("%n   jme3-utilities-heart version %s",
                MyString.quote(Misc.getVersionShort()));
        text += String.format("%n   SkyControl version %s",
                MyString.quote(Constants.getVersionShort()));
        text += String.format("%n   jme3-utilities-debug version %s",
                MyString.quote(DebugVersion.getVersionShort()));
        text += String.format("%n   jme3-utilities-ui version %s",
                MyString.quote(UiVersion.getVersionShort()));
        text += String.format("%n   jme3-utilities-nifty version %s%n%n",
                MyString.quote(LibraryVersion.getVersionShort()));

        Maud.gui.closeAllPopups();
        Maud.gui.showInfoDialog("About Maud", text);
    }

    /**
     * Display a "copy animation" dialog.
     */
    public void copyAnimation() {
        String fromName = Maud.getModel().getTarget().animation.getName();
        DialogController controller = new AnimationNameDialog("Copy");

        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter name for copied animation:",
                fromName, ActionPrefix.copyAnimation, controller);
    }

    /**
     * Display a "delete animation" dialog.
     */
    public void deleteAnimation() {
        String name = Maud.getModel().getTarget().animation.getName();
        String message = String.format("Delete the %s animation?",
                MyString.quote(name));
        Maud.gui.closeAllPopups();
        Maud.gui.showConfirmDialog(message, "Delete", "delete animation", null);
    }

    /**
     * Display a "delete control" dialog.
     */
    public void deleteSgc() {
        String name = Maud.getModel().getTarget().sgc.getName();
        String message;
        message = String.format("Delete the %s control?", MyString.quote(name));
        Maud.gui.closeAllPopups();
        Maud.gui.showConfirmDialog(message, "Delete", "delete control", null);
    }

    /**
     * Display a License infobox.
     */
    public void license() {
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
                    "Here's your software license for Maud:%n%s%n",
                    contents);
        }
        Maud.gui.closeAllPopups();
        Maud.gui.showInfoDialog("License information", text2);
    }

    /**
     * Display a "new animation fromPose" dialog.
     */
    public void newAnimationFromPose() {
        DialogController controller = new AnimationNameDialog("Create");
        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter a name for the new animation:",
                "pose", ActionPrefix.newAnimationFromPose, controller);
    }

    /**
     * Display a "new userKey" dialog.
     *
     * @param actionString (not null)
     */
    public void newUserKey(String actionString) {
        DialogController controller = new UserKeyDialog("Create");
        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter a key for the new user data:",
                "key", actionString, controller);
    }

    /**
     * Display a "reduce animation" dialog.
     */
    public void reduceAnimation() {
        if (Maud.getModel().getTarget().animation.isReal()) {
            IntegerDialog controller;
            controller = new IntegerDialog("Reduce", 2, Integer.MAX_VALUE);

            Maud.gui.closeAllPopups();
            Maud.gui.showTextEntryDialog("Enter reduction factor:", "2",
                    ActionPrefix.reduceAnimation, controller);
        }
    }

    /**
     * Display a "reduce track" dialog.
     */
    public void reduceTrack() {
        if (Maud.getModel().getTarget().bone.hasTrack()) {
            IntegerDialog controller;
            controller = new IntegerDialog("Reduce", 2, Integer.MAX_VALUE);

            Maud.gui.closeAllPopups();
            Maud.gui.showTextEntryDialog("Enter reduction factor:", "2",
                    ActionPrefix.reduceTrack, controller);
        }
    }

    /**
     * Display a "rename animation" dialog.
     */
    public void renameAnimation() {
        LoadedAnimation animation = Maud.getModel().getTarget().animation;
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
    public void renameBone() {
        SelectedBone bone = Maud.getModel().getTarget().bone;
        if (bone.isSelected()) {
            String oldName = bone.getName();
            DialogController controller = new BoneRenameDialog("Rename");

            Maud.gui.closeAllPopups();
            Maud.gui.showTextEntryDialog("Enter new name for the bone:",
                    oldName, ActionPrefix.renameBone, controller);
        }
    }

    /**
     * Display a "rename spatial" dialog.
     */
    public void renameSpatial() {
        SelectedSpatial spatial = Maud.getModel().getTarget().spatial;
        String oldName = spatial.getName();
        DialogController controller = new SpatialNameDialog("Rename");
        String prompt;
        if (spatial.isNode()) {
            prompt = "Enter new name for the node:";
        } else {
            prompt = "Enter new name for the geometry:";
        }

        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog(prompt, oldName,
                ActionPrefix.renameSpatial, controller);
    }

    /**
     * Display a "rename userKey" dialog.
     */
    public void renameUserKey() {
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
     * Display a "save cgm" dialog.
     */
    public void saveCgm() {
        EditableCgm target = Maud.getModel().getTarget();
        String baseFilePath = target.baseFilePathForWrite();
        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter base file path for model:",
                baseFilePath, "Save", ActionPrefix.saveCgm, null);
    }

    /**
     * Display a "save map" dialog.
     */
    public void saveMap() {
        String baseFilePath = Maud.getModel().getMap().baseFilePathForWrite();
        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter base file path for map:",
                baseFilePath, "Save", ActionPrefix.saveMap, null);
    }

    /**
     * Display a "retarget animation" dialog.
     */
    public void retargetAnimation() {
        String oldName = Maud.getModel().getSource().animation.getName();
        DialogController controller = new AnimationNameDialog("Retarget");

        Maud.gui.closeAllPopups();
        Maud.gui.showTextEntryDialog("Enter a name for the new animation:",
                oldName, ActionPrefix.retargetAnimation, controller);
    }

    /**
     * Display a "set duration" dialog.
     */
    public void setDuration() {
        LoadedAnimation animation = Maud.getModel().getTarget().animation;
        float oldDuration = animation.getDuration();
        String defaultText = Float.toString(oldDuration);

        float finalTime = animation.findLatestKeyframe();
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
                defaultText, ActionPrefix.setDuration, controller);
    }

    /**
     * Display a "set userData" dialog.
     */
    public void setUserData() {
        EditableCgm target = Maud.getModel().getTarget();
        String key = target.getUserData().getKey();
        Object data = target.spatial.getUserData(key);
        if (data instanceof Boolean) {
            boolean oldValue = (boolean) data;
            String newValue = Boolean.toString(!oldValue); // toggle value
            target.setUserData(newValue);

        } else if (data instanceof Float) {
            DialogController controller = new FloatDialog("Set",
                    Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
            float oldValue = (float) data;
            String stringData = Float.toString(oldValue);
            Maud.gui.showTextEntryDialog("Enter new float value:", stringData,
                    ActionPrefix.setUserData, controller);

        } else if (data instanceof Integer) {
            DialogController controller = new IntegerDialog("Set",
                    Integer.MIN_VALUE, Integer.MAX_VALUE);
            int oldValue = (int) data;
            String stringData = Integer.toString(oldValue);
            Maud.gui.showTextEntryDialog("Enter new integer value:", stringData,
                    ActionPrefix.setUserData, controller);

        } else if (data instanceof Long) {
            DialogController controller = new LongDialog("Set",
                    Long.MIN_VALUE, Long.MAX_VALUE);
            long oldValue = (long) data;
            String stringData = Long.toString(oldValue);
            Maud.gui.showTextEntryDialog("Enter new long integer value:",
                    stringData, ActionPrefix.setUserData, controller);

        } else if (data instanceof String) {
            DialogController controller = new TextEntryDialog();
            String oldValue = (String) data;
            Maud.gui.closeAllPopups();
            Maud.gui.showTextEntryDialog("Enter new string value:", oldValue,
                    "Set", ActionPrefix.setUserData, controller);
        }
    }
}
