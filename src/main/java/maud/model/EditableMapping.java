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

import com.jme3.export.binary.BinaryExporter;
import com.jme3.math.Quaternion;
import com.jme3.scene.plugins.bvh.BoneMapping;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.Validate;
import maud.Maud;
import maud.Util;

/**
 * The loaded skeleton mapping in the Maud application.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class EditableMapping extends LoadedMapping {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            EditableMapping.class.getName());
    // *************************************************************************
    // fields

    /**
     * count of unsaved edits to the skeleton mapping (&ge;0)
     */
    private int editCount = 0;
    /**
     * name of the target bone whose twist is being edited, or null for none
     */
    private String editedTwist = null;
    // *************************************************************************
    // new methods exposed

    /**
     * Determine the default base path for writing the mapping to the
     * filesystem.
     *
     * @return base filesystem path, or "" if unknown (not null)
     */
    public String baseFilePathForWrite() {
        String result = "";
        String aPath = getAssetPath();
        if (!aPath.isEmpty()) {
            String folder = assetFolderForWrite();
            File file = new File(folder, aPath);
            result = file.getAbsolutePath();
            result = result.replaceAll("\\\\", "/");
        }

        return result;
    }

    /**
     * Cardinalize the effective twist of the selected bone mapping.
     */
    public void cardinalizeTwist() {
        BoneMapping boneMapping = selectedMapping();
        Quaternion twist = boneMapping.getTwist();
        Util.cardinalizeLocal(twist);
        setEditedTwist();
    }

    /**
     * Count unsaved edits.
     *
     * @return count (&ge;0)
     */
    public int countUnsavedEdits() {
        return editCount;
    }

    /**
     * Delete the selected bone mapping.
     */
    public void deleteBone() {
        BoneMapping boneMapping = selectedMapping();
        if (boneMapping != null) {
            mapping.removeMapping(boneMapping);
            setEdited("delete bone mapping");
        }
    }

    /**
     * Invert the skeleton mapping.
     */
    public void invert() {
        if (mapping.countMappings() > 0) {
            mapping = mapping.inverse();
            assetPath = null;
            setEdited("invert skeleton mapping");
        }
    }

    /**
     * Add a bone mapping for the selected source and target bones.
     */
    public void mapBones() {
        if (!isBoneMappingSelected() && Maud.model.source.bone.isSelected()
                && Maud.model.target.bone.isSelected()) {
            String sourceBoneName = Maud.model.source.bone.getName();
            String targetBoneName = Maud.model.target.bone.getName();
            /*
             * Remove any prior mappings involving those bones.
             */
            BoneMapping boneMapping = mapping.getForSource(sourceBoneName);
            if (boneMapping != null) {
                mapping.removeMapping(boneMapping);
            }
            boneMapping = mapping.get(targetBoneName);
            if (boneMapping != null) {
                mapping.removeMapping(boneMapping);
            }
            /*
             * Predict what the twist will be.
             */
            Quaternion twist = estimateTwist();
            mapping.map(targetBoneName, sourceBoneName, twist);

            String event = "map bone " + targetBoneName;
            setEdited(event);
        }
    }

    /**
     * Callback just before a checkpoint is created.
     */
    public void onCheckpoint() {
        /*
         * Potentially a new twist edit.
         */
        editedTwist = null;
    }

    /**
     * Callback after a bone in the target CG model is renamed.
     */
    void renameBone(String oldName, String newName) {
        if (isInvertingMap()) {
            mapping.renameSourceBone(oldName, newName);
        } else {
            mapping.renameTargetBone(oldName, newName);
        }
    }

    /**
     * Alter the effective twist of the selected bone mapping.
     *
     * @param newTwist (not null, unaffected)
     */
    public void setTwist(Quaternion newTwist) {
        BoneMapping boneMapping = selectedMapping();
        Quaternion twist = boneMapping.getTwist();
        if (isInvertingMap()) {
            Quaternion tmp = newTwist.inverse();
            twist.set(tmp);
        } else {
            twist.set(newTwist);
        }
        setEditedTwist();
    }

    /**
     * Snap the one axis angle of the effective twist.
     *
     * @param axisIndex which axis: 0&rarr;X, 1&rarr;Y, 2&rarr;3
     */
    public void snapTwist(int axisIndex) {
        Validate.inRange(axisIndex, "axis index", 0, 2);

        BoneMapping boneMapping = selectedMapping();
        Quaternion twist = boneMapping.getTwist();
        Util.snapLocal(twist, axisIndex);
        setEditedTwist();
    }

    /**
     * Unload the skeleton mapping.
     */
    public void unload() {
        mapping.clear();
        assetFolder = null;
        assetPath = null;
        setEdited("unload mapping");
    }
    // *************************************************************************
    // LoadedMapping methods

    /**
     * Unload the current mapping and load the specified asset.
     *
     * @param assetFolder file path to the asset root (not null, not empty)
     * @param assetPath path to the asset to load (not null, not empty)
     * @return true if successful, otherwise false
     */
    @Override
    public boolean loadAsset(String assetFolder, String assetPath) {
        Validate.nonEmpty(assetFolder, "asset folder");
        Validate.nonEmpty(assetPath, "asset path");

        boolean result = super.loadAsset(assetFolder, assetPath);

        if (result) {
            String eventDescription = String.format("load mapping %s %s",
                    MyString.quote(assetFolder), MyString.quote(assetPath));
            setPristine(eventDescription);
        }

        return result;
    }
    // *************************************************************************
    // Object methods

    /**
     * Create a deep copy of this object.
     *
     * @return a new object, equivalent to this one
     * @throws CloneNotSupportedException if superclass isn't cloneable
     */
    @Override
    public EditableMapping clone() throws CloneNotSupportedException {
        EditableMapping clone = (EditableMapping) super.clone();
        return clone;
    }
    // *************************************************************************
    // private methods

    /**
     * Determine the default asset folder for writing the mapping to the
     * filesystem.
     *
     * @return absolute filesystem path (not null, not empty)
     */
    private String assetFolderForWrite() {
        String result = assetFolder;
        if (result.isEmpty()) {
            File wa = new File("Written Assets");
            result = wa.getAbsolutePath();
            result = result.replaceAll("\\\\", "/");
        }

        return result;
    }

    /**
     * Predict what the twist should be for the selected bones.
     *
     * @return a new quaternion
     */
    private Quaternion estimateTwist() {
        Quaternion sourceMo = Maud.model.source.bone.modelOrientation(null);
        Quaternion targetMo = Maud.model.target.bone.modelOrientation(null);
        Quaternion invSourceMo = sourceMo.inverse();
        Quaternion twist = invSourceMo.mult(targetMo, null);
        Util.cardinalizeLocal(twist);

        return twist;
    }

    /**
     * Increment the count of unsaved edits and update the edit history.
     *
     * @param eventDescription description of causative event (not null)
     */
    private void setEdited(String eventDescription) {
        ++editCount;
        editedTwist = null;
        History.addEvent(eventDescription);
    }

    /**
     * If not a continuation of the previous edit, update the edit count and the
     * edit history.
     */
    private void setEditedTwist() {
        String newName = Maud.model.target.bone.getName();
        if (!newName.equals(editedTwist)) {
            ++editCount;
            editedTwist = newName;
            String event = String.format("set twist for %s", newName);
            History.addEvent(event);
        }
    }

    /**
     * Mark the mapping as pristine.
     *
     * @param eventDescription description of causative event (not null)
     */
    private void setPristine(String eventDescription) {
        editCount = 0;
        editedTwist = null;
        History.addEvent(eventDescription);
    }

    /**
     * Write this mapping to a file. TODO sort methods
     *
     * @param path file path (not null, not empty)
     * @return true if successful, otherwise false
     */
    public boolean writeToFile(String path) {
        Validate.nonEmpty(path, "path");

        File file = new File(path);
        BinaryExporter exporter = BinaryExporter.getInstance();

        boolean success = true;
        try {
            exporter.save(mapping, file);
        } catch (IOException exception) {
            success = false;
        }

        String filePath = file.getAbsolutePath();
        filePath = filePath.replaceAll("\\\\", "/");

        if (success) {
            String af = assetFolderForWrite();
            if (filePath.startsWith(af)) {
                assetFolder = af;
                assetPath = MyString.remainder(filePath, af);
            } else if (filePath.endsWith(assetPath) && !assetPath.isEmpty()) {
                int length = filePath.length() - assetPath.length();
                assetFolder = filePath.substring(0, length);
            } else {
                assetFolder = "";
                assetPath = "";
            }
            if (assetPath.startsWith("/")) {
                assetPath = MyString.remainder(assetPath, "/");
            }
            String eventDescription = "write mapping to " + filePath;
            setPristine(eventDescription);
            logger.log(Level.INFO, "Wrote mapping to file {0}",
                    MyString.quote(filePath));
        } else {
            logger.log(Level.SEVERE,
                    "I/O exception while writing mapping to file {0}",
                    MyString.quote(filePath));
        }

        return success;
    }
}
