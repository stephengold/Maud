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
package maud.model;

import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import maud.Maud;

/**
 * The MVC model of the selected bone in the Maud application.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SelectedBone {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            SelectedBone.class.getName());
    // *************************************************************************
    // fields

    /**
     * index of the selected bone, or null for none selected
     */
    private Integer selectedIndex = null;
    // *************************************************************************
    // new methods exposed

    /**
     * Count how many children the selected bone has.
     *
     * @return count (&ge;0)
     */
    public int countChildren() {
        Bone bone = getBone();
        int result;
        if (bone == null) {
            result = 0;
        } else {
            List<Bone> children = bone.getChildren();
            result = children.size();
        }

        assert result >= 0 : result;
        return result;
    }

    /**
     * Access the selected bone.
     *
     * @return the pre-existing instance, or null if none selected
     */
    Bone getBone() {
        Bone bone;
        if (selectedIndex == null) {
            bone = null;
        } else {
            Skeleton skeleton = Maud.model.getSkeleton();
            bone = skeleton.getBone(selectedIndex);
        }

        return bone;
    }

    /**
     * Read the index of the selected bone. Assumes a bone is selected.
     *
     * @return the bone index
     */
    public int getIndex() {
        int index = selectedIndex;
        return index;
    }

    /**
     * Read the name of an indexed child of the selected bone.
     *
     * @param childIndex which child (&ge;0)
     * @return name, or null if none
     */
    public String getChildName(int childIndex) {
        assert childIndex >= 0 : childIndex;

        Bone bone = getBone();
        String name;
        if (bone == null) {
            name = null;
        } else {
            List<Bone> children = bone.getChildren();
            Bone child = children.get(childIndex);
            if (child == null) {
                name = null;
            } else {
                name = child.getName();
            }
        }

        return name;
    }

    /**
     * Read the name of the selected bone.
     *
     * @return the name or noBone (not null)
     */
    public String getName() {
        String name;
        Bone bone = getBone();
        if (bone == null) {
            name = LoadedCGModel.noBone;
        } else {
            name = bone.getName();
        }

        return name;
    }

    /**
     * Read the name of the parent of the selected bone.
     *
     * @return name, or null if none
     */
    public String getParentName() {
        Bone bone = getBone();
        String name;
        if (bone == null) {
            name = null;
        } else {
            Bone parent = bone.getParent();
            if (parent == null) {
                name = null;
            } else {
                name = parent.getName();
            }
        }

        return name;
    }

    /**
     * Test whether a bone is selected.
     *
     * @return true if selected, otherwise false
     */
    public boolean isBoneSelected() {
        if (selectedIndex == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether the selected bone is a root bone.
     *
     * @return true if it's a root, otherwise false
     */
    public boolean isRootBone() {
        Bone bone = getBone();
        boolean result;
        if (bone == null) {
            result = false;
        } else {
            Bone parent = bone.getParent();
            result = (parent == null);
        }

        return result;
    }

    /**
     * Select the specified bone.
     *
     * @param bone which bone to select (not null)
     */
    void select(Bone bone) {
        assert bone != null;

        Skeleton skeleton = Maud.model.getSkeleton();
        int index = skeleton.getBoneIndex(bone);
        if (index != -1) {
            select(index);
        }
    }

    /**
     * Enumerate the names of all children of the selected bone.
     *
     * @return a new list
     */
    public List<String> listChildNames() {
        Bone bone = getBone();
        List<String> result;
        if (bone == null) {
            result = new ArrayList<>(0);
        } else {
            List<Bone> children = bone.getChildren();
            int numChildren = children.size();
            result = new ArrayList<>(numChildren);
            for (Bone child : children) {
                String name = child.getName();
                result.add(name);
            }
        }

        return result;
    }

    /**
     * Select a bone by its index.
     *
     * @param newIndex which bone to select
     */
    public void select(int newIndex) {
        selectedIndex = newIndex;
        Maud.gui.bone.update();
    }

    /**
     * Select a bone by its name.
     *
     * @param name bone name or noBone (not null)
     */
    public void select(String name) {
        if (name.equals(LoadedCGModel.noBone)) {
            selectNoBone();

        } else {
            Skeleton skeleton = Maud.model.getSkeleton();
            int index = skeleton.getBoneIndex(name);
            if (index == -1) {
                logger.log(Level.WARNING, "Select failed: no bone named {0}.",
                        MyString.quote(name));
            } else {
                select(index);
            }
        }
    }

    /**
     * Select (by index) a child of the selected bone.
     *
     * @param childIndex (&ge;0)
     */
    public void selectChild(int childIndex) {
        assert childIndex >= 0 : childIndex;

        Bone bone = getBone();
        if (bone != null) {
            List<Bone> children = bone.getChildren();
            Bone child = children.get(childIndex);
            if (child != null) {
                select(child);
            }
        }
    }

    /**
     * Deselect the selected bone, if any.
     */
    public void selectNoBone() {
        selectedIndex = null;
        Maud.gui.bone.update();
    }

    /**
     * Select the parent of the selected bone, if any.
     */
    public void selectParent() {
        Bone bone = getBone();
        if (bone != null) {
            Bone parent = bone.getParent();
            if (parent != null) {
                select(parent);
            }
        }
    }
}
