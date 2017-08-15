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
package maud.menu;

import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyString;
import maud.Maud;
import maud.action.ActionPrefix;
import maud.model.EditableCgm;
import maud.model.EditorModel;
import maud.model.LoadedCgm;

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
    final private static Logger logger = Logger.getLogger(
            BoneMenus.class.getName());
    // *************************************************************************
    // new methods exposed

    /**
     * Handle a "select menuItem" action from the Bone menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    boolean menuBone(String remainder) {
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
            switch (remainder) {
                case "Rename":
                    Maud.gui.dialogs.renameBone();
                    break;
                case "Rotate":
                    Maud.gui.tools.select("boneRotation");
                    break;
                case "Scale":
                    Maud.gui.tools.select("boneScale");
                    break;
                case "Select":
                    Maud.gui.buildMenus.selectBone();
                    break;
                case "Select source":
                    Maud.gui.buildMenus.selectSourceBone();
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
     * Handle a "select bone" action with an argument.
     *
     * @param argument action argument (not null)
     */
    public void selectBone(String argument) {
        LoadedCgm target = Maud.getModel().getTarget();
        if (target.bones.hasBone(argument)) {
            target.bone.select(argument);

        } else {
            /*
             * Treat the argument as a bone-name prefix.
             */
            List<String> boneNames = target.bones.listBoneNames(argument);
            Maud.gui.buildMenus.showBoneSubmenu(boneNames);
        }
    }

    /**
     * Handle a "select boneChild" action without arguments.
     */
    public void selectBoneChild() {
        LoadedCgm target = Maud.getModel().getTarget();
        if (target.bone.isSelected()) {
            int numChildren = target.bone.countChildren();
            if (numChildren == 1) {
                target.bone.selectFirstChild();
            } else if (numChildren > 1) {
                List<String> boneNames = target.bone.listChildNames();
                Maud.gui.buildMenus.showBoneSubmenu(boneNames);
            }
        }
    }

    /**
     * Handle a "select boneWithTrack" action.
     */
    void selectBoneWithTrack() {
        EditableCgm target = Maud.getModel().getTarget();
        List<String> boneNames = target.animation.listBonesWithTrack();
        int numBoneTracks = boneNames.size();
        if (numBoneTracks == 1) {
            target.bone.select(boneNames.get(0));
        } else if (numBoneTracks > 1) {
            Maud.gui.buildMenus.showBoneSubmenu(boneNames);
        }
    }

    /**
     * Handle a "select sourceBone" action with an argument.
     *
     * @param argument action argument (not null)
     */
    public void selectSourceBone(String argument) {
        LoadedCgm source = Maud.getModel().getSource();
        if (source.bones.hasBone(argument)) {
            source.bone.select(argument);
        } else {
            /*
             * Treat the argument as a bone-name prefix.
             */
            List<String> boneNames = source.bones.listBoneNames(argument);
            Maud.gui.buildMenus.showBoneSubmenu(boneNames);
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
    private boolean menuBoneSelect(String remainder) {
        boolean handled = true;

        EditorModel model = Maud.getModel();
        EditableCgm target = model.getTarget();
        switch (remainder) {
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
                target.bone.selectNext();
                break;
            case "Parent":
                target.bone.selectParent();
                break;
            case "Previous":
                target.bone.selectPrevious();
                break;
            case "Root":
                selectRootBone();
                break;
            case "With track":
                selectBoneWithTrack();
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
    private boolean menuBoneSelectSource(String remainder) {
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
     * Select a bone by name, using submenus.
     */
    private void selectBoneByName() {
        LoadedCgm target = Maud.getModel().getTarget();
        List<String> nameList = target.bones.listBoneNames();
        Maud.gui.buildMenus.showBoneSubmenu(nameList);
    }

    /**
     * Select a bone by parent, using submenus.
     */
    private void selectBoneByParent() {
        LoadedCgm target = Maud.getModel().getTarget();
        List<String> boneNames = target.bones.listRootBoneNames();
        Maud.gui.showPopupMenu(ActionPrefix.selectBoneChild, boneNames);
    }

    /**
     * Handle a "select rootBone" action.
     */
    private void selectRootBone() {
        LoadedCgm target = Maud.getModel().getTarget();
        int numRoots = target.bones.countRootBones();
        if (numRoots == 1) {
            target.bone.selectFirstRoot();
        } else if (numRoots > 1) {
            List<String> boneNames = target.bones.listRootBoneNames();
            Maud.gui.buildMenus.showBoneSubmenu(boneNames);
        }
    }

    /**
     * Handle a "select sourceRootBone" action.
     */
    private void selectSourceRootBone() {
        LoadedCgm source = Maud.getModel().getSource();
        int numRoots = source.bones.countRootBones();
        if (numRoots == 1) {
            source.bone.selectFirstRoot();
        } else if (numRoots > 1) {
            List<String> names = source.bones.listRootBoneNames();
            Maud.gui.buildMenus.showSourceBoneSubmenu(names);
        }
    }
}
