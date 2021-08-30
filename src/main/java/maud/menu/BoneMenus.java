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
package maud.menu;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyString;
import maud.Maud;
import maud.action.ActionPrefix;
import maud.dialog.EditorDialogs;
import static maud.menu.ShowMenus.maxItems;
import maud.model.EditorModel;
import maud.model.cgm.Cgm;
import maud.model.cgm.EditableCgm;
import maud.model.cgm.SelectedBone;
import maud.model.cgm.SelectedSkeleton;

/**
 * Bone menus in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class BoneMenus {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(BoneMenus.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private BoneMenus() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Build a Bone menu.
     */
    static void buildBoneMenu(MenuBuilder builder) {
        builder.addTool("Tool");

        EditorModel model = Maud.getModel();
        Cgm target = model.getTarget();
        SelectedSkeleton skeleton = target.getSkeleton();
        if (skeleton.countBones() > 0) {
            builder.addSubmenu("Select");
        }

        builder.addTool("Rotate");
        builder.addTool("Scale");
        builder.addTool("Translate");
        builder.addTool("Mirror");

        SelectedBone selectedBone = target.getBone();
        if (selectedBone.isSelected()) {
            boolean hasAttachments = selectedBone.hasAttachmentsNode();
            if (hasAttachments) {
                builder.addEdit("Delete attachments");
            } else {
                builder.addEdit("Attach node");
            }
            builder.add("Deselect");
            builder.addDialog("Rename");
        }
        if (model.getSource().getSkeleton().countBones() > 0) {
            builder.addSubmenu("Select source");
        }
    }

    /**
     * Handle a "select menuItem" action from the Bone menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    static boolean menuBone(String remainder) {
        assert remainder != null;

        boolean handled = true;
        String selectPrefix = "Select" + EditorMenus.menuPathSeparator;
        String ssPrefix = "Select source" + EditorMenus.menuPathSeparator;
        if (remainder.startsWith(selectPrefix)) {
            String selectArg = MyString.remainder(remainder, selectPrefix);
            handled = menuBoneSelect(selectArg);

        } else if (remainder.startsWith(ssPrefix)) {
            String selectArg = MyString.remainder(remainder, ssPrefix);
            handled = menuBoneSelectSource(selectArg);

        } else {
            EditableCgm target = Maud.getModel().getTarget();
            switch (remainder) {
                case "Attach node":
                    target.addAttachmentsNode();
                    break;

                case "Deselect":
                    target.getBone().deselect();
                    break;

                case "Delete attachments":
                    target.deleteAttachmentsNode();
                    break;

                case "Mirror":
                    Maud.gui.tools.select("boneMirror");
                    break;

                case "Rename":
                    EditorDialogs.renameBone();
                    break;

                case "Rotate":
                    Maud.gui.tools.select("boneRotation");
                    break;

                case "Scale":
                    Maud.gui.tools.select("boneScale");
                    break;

                case "Select":
                    selectBone();
                    break;

                case "Select source":
                    selectSourceBone();
                    break;

                case "Tool":
                    Maud.gui.tools.select("bone");
                    break;

                case "Translate":
                    Maud.gui.tools.select("boneTranslation");
                    break;

                default:
                    handled = false;
            }
        }

        return handled;
    }

    /**
     * Handle a "select bone" action without an argument.
     */
    public static void selectBone() {
        MenuBuilder builder = new MenuBuilder();

        builder.addSubmenu("By name");
        builder.addSubmenu("By parent");

        Cgm target = Maud.getModel().getTarget();
        int numRoots = target.getSkeleton().countRootBones();
        if (numRoots == 1) {
            builder.addBone("Root");
        } else if (numRoots > 1) {
            builder.addSubmenu("Root");
        }

        int numTracks = target.getAnimation().countBoneTracks();
        if (numTracks > 0) {
            builder.addSubmenu("With track");
        }

        int numAttachments = target.getSkeleton().listAttachedBones().size();
        if (numAttachments == 1) {
            builder.addBone("Attached");
        } else if (numAttachments > 1) {
            builder.addSubmenu("Attached");
        }

        String sourceBoneName = Maud.getModel().getSource().getBone().name();
        String boneName;
        boneName = Maud.getModel().getMap().targetBoneName(sourceBoneName);
        if (boneName != null && target.getSkeleton().hasBone(boneName)) {
            builder.addBone("Mapped");
        }

        SelectedBone bone = target.getBone();
        int numChildren = bone.countChildren();
        if (numChildren == 1) {
            builder.addBone("Child");
        } else if (numChildren > 1) {
            builder.addSubmenu("Child");
        }

        boolean isSelected = bone.isSelected();
        boolean isRoot = bone.isRootBone();
        if (isSelected && !isRoot) {
            builder.addBone("Parent");
        }
        if (isSelected) {
            builder.addBone("Next");
            builder.addBone("None");
            builder.addBone("Previous");
        }
        builder.addDialog("By index");

        builder.show("select menuItem Bone -> Select -> ");
    }

    /**
     * Handle a "select bone" action with an argument.
     *
     * @param argument action argument (not null)
     */
    public static void selectBone(String argument) {
        Cgm target = Maud.getModel().getTarget();
        SelectedSkeleton skeleton = target.getSkeleton();
        if (skeleton.hasBone(argument)) {
            target.getBone().select(argument);
        } else {
            /*
             * Treat the argument as a bone-name prefix.
             */
            List<String> boneNames = skeleton.listBoneNames(argument);
            showBoneSubmenu(boneNames);
        }
    }

    /**
     * Handle a "select boneChild" action without arguments.
     */
    public static void selectBoneChild() {
        SelectedBone bone = Maud.getModel().getTarget().getBone();
        if (bone.isSelected()) {
            int numChildren = bone.countChildren();
            if (numChildren == 1) {
                bone.selectFirstChild();
            } else if (numChildren > 1) {
                List<String> boneNames = bone.listChildNames();
                showBoneSubmenu(boneNames);
            }
        }
    }

    /**
     * Handle a "select boneChild" action with an argument.
     *
     * @param argument action argument (not null)
     */
    public static void selectBoneChild(String argument) {
        Cgm target = Maud.getModel().getTarget();
        if (argument.startsWith("!")) {
            String name = argument.substring(1);
            target.getBone().select(name);
        } else {
            SelectedSkeleton skeleton = target.getSkeleton();
            List<String> names = skeleton.listChildBoneNames(argument);

            MenuBuilder builder = new MenuBuilder();
            builder.addBone("!" + argument);
            for (String name : names) {
                if (skeleton.isLeafBone(name)) {
                    builder.addBone("!" + name);
                } else {
                    builder.add(name);
                }
            }
            builder.show(ActionPrefix.selectBoneChild);
        }
    }

    /**
     * Handle a "select (source)skeleton" action without an argument.
     *
     * @param cgm which load slot (not null)
     */
    public static void selectSkeleton(Cgm cgm) {
        if (cgm.isLoaded()) {
            MenuBuilder builder = new MenuBuilder();
            List<String> names = cgm.listSkeletonNames();
            for (String name : names) {
                builder.add(name);
            }

            if (cgm == Maud.getModel().getTarget()) {
                builder.show(ActionPrefix.selectSkeleton);
//            } else if (cgm == Maud.getModel().getSource()) {
//                builder.show(ActionPrefix.selectSourceSkeleton);
            } else {
                throw new IllegalArgumentException();
            }
        }
    }

    /**
     * Handle a "select sourceBone" action without an argument.
     */
    public static void selectSourceBone() {
        EditorModel model = Maud.getModel();
        if (model.getSource().isLoaded()) {
            MenuBuilder builder = new MenuBuilder();

            SelectedSkeleton sourceSkeleton = model.getSource().getSkeleton();
            int numRoots = sourceSkeleton.countRootBones();
            if (numRoots == 1) {
                builder.addBone("Root");
            } else if (numRoots > 1) {
                builder.addSubmenu("Root");
            }

            String targetBoneName = model.getTarget().getBone().name();
            String boneName = model.getMap().sourceBoneName(targetBoneName);
            if (boneName != null && sourceSkeleton.hasBone(boneName)) {
                builder.addBone("Mapped");
            }

            builder.show("select menuItem Bone -> Select source -> ");
        }
    }

    /**
     * Handle a "select sourceBone" action with an argument.
     *
     * @param argument action argument (not null)
     */
    public static void selectSourceBone(String argument) {
        Cgm source = Maud.getModel().getSource();
        SelectedSkeleton skeleton = source.getSkeleton();
        if (skeleton.hasBone(argument)) {
            source.getBone().select(argument);
        } else {
            /*
             * Treat the argument as a bone-name prefix.
             */
            List<String> boneNames = skeleton.listBoneNames(argument);
            showBoneSubmenu(boneNames);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Handle a "select menuItem" action from the "Bone -> Select" menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private static boolean menuBoneSelect(String remainder) {
        boolean handled = true;

        EditorModel model = Maud.getModel();
        SelectedBone selection = model.getTarget().getBone();
        switch (remainder) {
            case "Attached":
                selectAttachedBone();
                break;

            case "By index":
                EditorDialogs.selectBoneIndex();
                break;

            case "By name":
                selectBoneByName();
                break;

            case "By parent":
                selectBoneByParent();
                break;

            case "Child":
                selectBoneChild();
                break;

            case "Mapped":
                model.getMap().selectFromSource();
                break;

            case "Next":
                selection.selectNext();
                break;

            case "None":
                selection.deselect();
                break;

            case "Parent":
                selection.selectParent();
                break;

            case "Previous":
                selection.selectPrevious();
                break;

            case "Root":
                selectRootBone();
                break;

            case "With track":
                selectTrackedBone();
                break;

            default:
                handled = false;
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the "Bone -> Select source" menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private static boolean menuBoneSelectSource(String remainder) {
        assert remainder != null;

        boolean handled = false;
        switch (remainder) {
            case "Mapped":
                Maud.getModel().getMap().selectFromTarget();
                handled = true;
                break;
            case "Root":
                selectSourceRootBone();
                handled = true;
        }

        return handled;
    }

    /**
     * Select a bone with an attachments node, using submenus.
     */
    private static void selectAttachedBone() {
        Cgm target = Maud.getModel().getTarget();
        List<String> boneNames = target.getSkeleton().listAttachedBones();
        int numAttachmentNodes = boneNames.size();
        if (numAttachmentNodes == 1) {
            target.getBone().select(boneNames.get(0));
        } else if (numAttachmentNodes > 1) {
            showBoneSubmenu(boneNames);
        }
    }

    /**
     * Select a bone by name, using submenus if necessary.
     */
    private static void selectBoneByName() {
        Cgm target = Maud.getModel().getTarget();
        List<String> nameList = target.getSkeleton().listBoneNames();
        showBoneSubmenu(nameList);
    }

    /**
     * Select a bone by parent, using submenus.
     */
    private static void selectBoneByParent() {
        Cgm target = Maud.getModel().getTarget();
        List<String> boneNames = target.getSkeleton().listRootBoneNames();
        Maud.gui.showPopupMenu(ActionPrefix.selectBoneChild, boneNames);
    }

    /**
     * Handle a "select rootBone" action.
     */
    private static void selectRootBone() {
        Cgm target = Maud.getModel().getTarget();
        int numRoots = target.getSkeleton().countRootBones();
        if (numRoots == 1) {
            target.getBone().selectFirstRoot();
        } else if (numRoots > 1) {
            List<String> boneNames = target.getSkeleton().listRootBoneNames();
            showBoneSubmenu(boneNames);
        }
    }

    /**
     * Handle a "select sourceRootBone" action.
     */
    private static void selectSourceRootBone() {
        Cgm source = Maud.getModel().getSource();
        int numRoots = source.getSkeleton().countRootBones();
        if (numRoots == 1) {
            source.getBone().selectFirstRoot();
        } else if (numRoots > 1) {
            List<String> names = source.getSkeleton().listRootBoneNames();
            showSourceBoneSubmenu(names);
        }
    }

    /**
     * Select a tracked bone, using submenus.
     */
    private static void selectTrackedBone() {
        Cgm target = Maud.getModel().getTarget();
        List<String> boneNames = target.getAnimation().listTrackedBones();
        int numBoneTracks = boneNames.size();
        if (numBoneTracks == 1) {
            target.getBone().select(boneNames.get(0));
        } else if (numBoneTracks > 1) {
            showBoneSubmenu(boneNames);
        }
    }

    /**
     * Display a submenu for selecting a target bone by name using the "select
     * bone" action prefix.
     *
     * @param nameList list of names from which to select (not null)
     */
    private static void showBoneSubmenu(List<String> nameList) {
        assert nameList != null;

        MyString.reduce(nameList, maxItems);
        Collections.sort(nameList);

        MenuBuilder builder = new MenuBuilder();
        for (String name : nameList) {
            if (Maud.getModel().getTarget().getSkeleton().hasBone(name)) {
                builder.addBone(name);
            } else {
                builder.addEllipsis(name);
            }
        }
        builder.show(ActionPrefix.selectBone);
    }

    /**
     * Display a submenu for selecting a source bone by name using the "select
     * sourceBone" action prefix.
     *
     * @param nameList list of names from which to select (not null)
     */
    private static void showSourceBoneSubmenu(List<String> nameList) {
        assert nameList != null;

        MyString.reduce(nameList, maxItems);
        Collections.sort(nameList);

        MenuBuilder builder = new MenuBuilder();
        for (String name : nameList) {
            if (Maud.getModel().getSource().getSkeleton().hasBone(name)) {
                builder.addBone(name);
            } else {
                builder.addEllipsis(name);
            }
        }
        builder.show(ActionPrefix.selectSourceBone);
    }
}
