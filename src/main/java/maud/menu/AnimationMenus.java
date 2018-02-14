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
package maud.menu;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import jme3utilities.MyString;
import maud.Maud;
import maud.action.ActionPrefix;
import maud.dialog.EditorDialogs;
import maud.model.EditorModel;
import maud.model.WhichCgm;
import maud.model.cgm.Cgm;
import maud.model.cgm.EditableCgm;
import maud.model.cgm.LoadedAnimation;
import maud.model.cgm.SelectedAnimControl;
import maud.model.cgm.TrackItem;

/**
 * Animation/track menus in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class AnimationMenus {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(AnimationMenus.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private AnimationMenus() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Display an "Animation -&gt; Add new" menu.
     */
    public static void addNewAnimation() {
        MenuBuilder builder = new MenuBuilder();

        EditorModel model = Maud.getModel();
        LoadedAnimation sourceAnimation = model.getSource().getAnimation();
        LoadedAnimation targetAnimation = model.getTarget().getAnimation();
        if (sourceAnimation.isReal() && targetAnimation.isReal()) {
            builder.addDialog("Prepend source animation");
            builder.addDialog("Append source animation");
        }
        builder.addDialog("Copy");
        builder.addTool("Extract sub-animation");
        builder.addDialog("Mix tracks");
        builder.addDialog("Pose");
        builder.addTool("Retarget source animation");

        builder.show("select menuItem Animation -> Add new -> ");
    }

    /**
     * Display an "Animation -&gt; Edit" menu (only for a real animation).
     */
    public static void editAnimation() {
        MenuBuilder builder = new MenuBuilder();

        Cgm target = Maud.getModel().getTarget();
        LoadedAnimation animation = target.getAnimation();
        float duration = animation.getDuration();
        if (duration > 0f) {
            builder.addEdit("Behead");
        }
        builder.addDialog("Change duration: proportional");
        builder.addDialog("Change duration: same times");
        if (duration > 0f) {
            builder.addEdit("Delete keyframes");
            builder.addEdit("Insert keyframes");
            builder.addDialog("Reduce all tracks");
            builder.addDialog("Resample all tracks at rate");
            builder.addDialog("Resample all tracks to number");
        }
        builder.addEdit("Reverse");
        builder.addEdit("Simplify");
        if (duration > 0f) {
            builder.addEdit("Truncate");
            if (animation.anyTrackEndsWithKeyframe()) {
                builder.addDialog("Wrap all tracks");
            } else {
                builder.addEdit("Wrap all tracks");
            }
        }

        builder.show("select menuItem Animation -> Edit -> ");
    }

    /**
     * Handle a "load (source)animation" action without arguments.
     *
     * @param cgm (not null)
     */
    public static void loadAnimation(Cgm cgm) {
        if (cgm.isLoaded()) {
            List<String> names = cgm.getAnimControl().listAnimationNames();
            showAnimationSubmenu(names, cgm);
        }
    }

    /**
     * Handle a "load (source)animation" action with an argument.
     *
     * @param argument action argument (not null)
     * @param cgm which load slot (not null)
     */
    public static void loadAnimation(String argument, Cgm cgm) {
        SelectedAnimControl sac = cgm.getAnimControl();
        if (sac.hasRealAnimation(argument)
                || argument.equals(LoadedAnimation.bindPoseName)
                || argument.equals(LoadedAnimation.retargetedPoseName)) {
            cgm.getAnimation().load(argument);
        } else {
            /*
             * Treat the argument as an animation-name prefix.
             */
            List<String> animationNames = sac.listAnimationNames(argument);
            showAnimationSubmenu(animationNames, cgm);
        }
    }

    /**
     * Handle a "select menuItem" action from the Animation menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    static boolean menuAnimation(String remainder) {
        boolean handled = true;
        String addNewPrefix = "Add new" + EditorMenus.menuPathSeparator;
        String editPrefix = "Edit" + EditorMenus.menuPathSeparator;
        if (remainder.startsWith(addNewPrefix)) {
            String arg = MyString.remainder(remainder, addNewPrefix);
            handled = menuAnimationAddNew(arg);

        } else if (remainder.startsWith(editPrefix)) {
            String arg = MyString.remainder(remainder, editPrefix);
            handled = menuAnimationEdit(arg);

        } else {
            EditableCgm target = Maud.getModel().getTarget();
            switch (remainder) {
                case "Add new":
                    addNewAnimation();
                    break;
                case "Delete":
                    EditorDialogs.deleteAnimation();
                    break;
                case "Edit":
                    editAnimation();
                    break;
                case "Load":
                    loadAnimation(target);
                    break;
                case "Load source":
                    loadAnimation(Maud.getModel().getSource());
                    break;
                case "Rename":
                    EditorDialogs.renameAnimation();
                    break;
                case "Select AnimControl":
                    selectAnimControl(target);
                    break;
                case "Source tool":
                    Maud.gui.tools.select("sourceAnimation");
                    break;
                case "Tool":
                    Maud.gui.tools.select("animation");
                    break;
                case "Tweening":
                    Maud.gui.tools.select("tweening");
                    break;
                default:
                    handled = false;
            }
        }

        return handled;
    }

    /**
     * Handle a "select (source)animControl" action without an argument.
     *
     * @param cgm which load slot (not null)
     */
    public static void selectAnimControl(Cgm cgm) {
        if (cgm.isLoaded()) {
            MenuBuilder builder = new MenuBuilder();
            List<String> names = cgm.listAnimControlNames();
            for (String name : names) {
                builder.add(name);
            }

            if (cgm == Maud.getModel().getTarget()) {
                builder.show(ActionPrefix.selectAnimControl);
            } else if (cgm == Maud.getModel().getSource()) {
                builder.show(ActionPrefix.selectSourceAnimControl);
            } else {
                throw new IllegalArgumentException();
            }
        }
    }

    /**
     * Handle a "select track" action without an argument.
     */
    public static void selectTrack() {
        LoadedAnimation animation = Maud.getModel().getTarget().getAnimation();
        assert animation.isReal();

        List<TrackItem> items = animation.listTracks();
        Set<String> descriptions = new TreeSet<>();
        for (TrackItem item : items) {
            String description = item.describe();
            boolean success = descriptions.add(description);
            assert success;
        }
        showTrackSubmenu(descriptions);
    }

    /**
     * Handle a "select track" action with an argument.
     *
     * @param argument action argument (not null)
     */
    public static void selectTrack(String argument) {
        Cgm target = Maud.getModel().getTarget();
        LoadedAnimation animation = target.getAnimation();
        if (animation.hasTrack(argument)) {
            target.getTrack().selectWithDescription(argument);
        } else {
            /*
             * Treat the argument as a track-description prefix.
             */
            List<TrackItem> items = animation.listTracks();
            Set<String> descriptions = new TreeSet<>();
            for (TrackItem item : items) {
                String description = item.describe();
                if (description.startsWith(argument)) {
                    boolean success = descriptions.add(description);
                    assert success;
                }
            }
            showTrackSubmenu(descriptions);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Handle a "select menuItem" action from the "Animation -> Add new" menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private static boolean menuAnimationAddNew(String remainder) {
        boolean handled = true;
        switch (remainder) {
            case "Append source animation":
                EditorDialogs.newAnimationFromChain(WhichCgm.Target,
                        WhichCgm.Source);
                break;

            case "Copy":
                Cgm target = Maud.getModel().getTarget();
                String originalName = target.getAnimation().getName();
                EditorDialogs.newAnimation(ActionPrefix.newAnimationFromCopy,
                        "Copy", originalName);
                break;

            case "Extract sub-animation":
                Maud.gui.tools.select("extract");
                break;

            case "Mix tracks":
                EditorDialogs.newAnimationFromMix();
                break;

            case "Pose":
                EditorDialogs.newAnimation(ActionPrefix.newAnimationFromPose,
                        "Create", "pose");
                break;

            case "Prepend source animation":
                EditorDialogs.newAnimationFromChain(WhichCgm.Source,
                        WhichCgm.Target);
                break;

            case "Retarget source animation":
                Maud.gui.tools.select("retarget");
                break;

            default:
                handled = false;
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the "Animation -> Edit" menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private static boolean menuAnimationEdit(String remainder) {
        boolean handled = true;
        EditableCgm target = Maud.getModel().getTarget();
        LoadedAnimation animation = target.getAnimation();
        switch (remainder) {
            case "Behead":
                animation.behead();
                break;

            case "Change duration: proportional":
                EditorDialogs.setDurationProportional();
                break;

            case "Change duration: same times":
                EditorDialogs.setDurationSame();
                break;

            case "Delete keyframes":
                animation.deleteKeyframes();
                break;

            case "Insert keyframes":
                animation.insertKeyframes();
                break;

            case "Reduce all tracks":
                EditorDialogs.reduceAnimation();
                break;

            case "Resample all tracks to number":
                EditorDialogs.resampleAnimation(false);
                break;

            case "Resample all tracks at rate":
                EditorDialogs.resampleAnimation(true);
                break;

            case "Reverse":
                animation.reverse();
                break;

            case "Simplify":
                animation.simplify();
                break;

            case "Truncate":
                animation.truncate();
                break;

            case "Wrap all tracks":
                if (animation.anyTrackEndsWithKeyframe()) {
                    EditorDialogs.wrapAnimation();
                } else {
                    animation.wrapAllTracks(0f);
                }
                break;

            default:
                handled = false;
        }

        return handled;
    }

    /**
     * Display a submenu for selecting an animation by name using the "select
     * (source)animation" action prefix.
     *
     * @param nameList list of names from which to select (not null, modified)
     * @param cgm which load slot (not null)
     */
    private static void showAnimationSubmenu(List<String> nameList, Cgm cgm) {
        assert nameList != null;
        assert cgm != null;

        String loadedAnimation = cgm.getAnimation().getName();
        boolean success = nameList.remove(loadedAnimation);
        assert success;

        MyString.reduce(nameList, ShowMenus.maxItems);
        Collections.sort(nameList);

        MenuBuilder builder = new MenuBuilder();
        for (String name : nameList) {
            if (cgm.getAnimControl().hasRealAnimation(name)) {
                builder.add(name); // TODO icon
            } else if (name.equals(LoadedAnimation.bindPoseName)) {
                builder.add(name);
            } else if (name.equals(LoadedAnimation.retargetedPoseName)) {
                builder.add(name);
            } else {
                builder.addEllipsis(name);
            }
        }

        if (cgm == Maud.getModel().getTarget()) {
            builder.show(ActionPrefix.loadAnimation);
        } else if (cgm == Maud.getModel().getSource()) {
            builder.show(ActionPrefix.loadSourceAnimation);
        } else {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Display a submenu for selecting a target track by name using the "select
     * track" action prefix.
     *
     * @param descriptions descriptions from which to select (not null)
     */
    private static void showTrackSubmenu(Set<String> descriptions) {
        assert descriptions != null;

        MyString.reduce(descriptions, ShowMenus.maxItems);

        MenuBuilder builder = new MenuBuilder();
        for (String desc : descriptions) {
            if (Maud.getModel().getTarget().getAnimation().hasTrack(desc)) {
                builder.add(desc);
            } else {
                builder.addEllipsis(desc);
            }
        }
        builder.show(ActionPrefix.selectTrack);
    }
}
