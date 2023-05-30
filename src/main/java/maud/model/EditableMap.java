/*
 Copyright (c) 2017-2023, Stephen Gold
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

import com.jme3.export.JmeExporter;
import com.jme3.math.Quaternion;
import com.jme3.scene.plugins.bvh.BoneMapping;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Heart;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.math.MyQuaternion;
import jme3utilities.math.MyVector3f;
import jme3utilities.ui.ActionApplication;
import maud.Maud;
import maud.model.cgm.Cgm;
import maud.model.cgm.CgmOutputFormat;
import maud.model.cgm.SelectedSkeleton;

/**
 * The loaded skeleton map in the Maud application, with editing features.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class EditableMap extends LoadedMap {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(EditableMap.class.getName());
    // *************************************************************************
    // fields

    /**
     * count of unsaved edits and continuous-edit state
     */
    private EditState editState = new EditState();
    // *************************************************************************
    // new methods exposed

    /**
     * Determine the default base path for writing the map to the filesystem.
     *
     * @return absolute filesystem path less extension (not null, not empty)
     */
    public String baseFilePathForWrite() {
        String folder = assetFolderForWrite();
        String assetPath = getAssetPath();
        if (assetPath.isEmpty()) {
            assetPath = "SkeletonMaps/Untitled";
        }
        File file = new File(folder, assetPath);
        String result = Heart.fixedPath(file);

        return result;
    }

    /**
     * Cardinalize the effective twist of the selected bone mapping.
     */
    public void cardinalizeTwist() {
        BoneMapping boneMapping = selectedMapping();
        Quaternion twist = boneMapping.getTwist();
        MyQuaternion.cardinalizeLocal(twist);

        String targetBoneName = boneMapping.getTargetName();
        editState.setEditedTwist(targetBoneName);
    }

    /**
     * Delete the selected bone mapping.
     */
    public void deleteBoneMapping() {
        BoneMapping boneMapping = selectedMapping();
        if (boneMapping != null) {
            History.autoAdd();
            map.removeMapping(boneMapping);
            editState.setEdited("delete bone mapping");
        }
    }

    /**
     * Delete all invalid bone mappings.
     */
    public void deleteInvalidMappings() {
        if (hasInvalidMappings()) {
            Cgm source = Maud.getModel().getSource();
            Cgm target = Maud.getModel().getTarget();
            SelectedSkeleton skeleton;
            if (isInvertingMap()) {
                skeleton = source.getSkeleton();
            } else {
                skeleton = target.getSkeleton();
            }
            int numDeleted = 0;

            History.autoAdd();
            if (skeleton.isSelected()) {
                for (String targetBoneName : map.listTargetBones()) {
                    if (!skeleton.hasBone(targetBoneName)) {
                        BoneMapping mapping = map.get(targetBoneName);
                        map.removeMapping(mapping);
                        ++numDeleted;
                    }
                }
            }
            if (isInvertingMap()) {
                skeleton = target.getSkeleton();
            } else {
                skeleton = source.getSkeleton();
            }
            if (skeleton.isSelected()) {
                for (String sourceBoneName : map.listSourceBones()) {
                    if (!skeleton.hasBone(sourceBoneName)) {
                        BoneMapping mapping = map.getForSource(sourceBoneName);
                        map.removeMapping(mapping);
                        ++numDeleted;
                    }
                }
            }

            String description = String.format(
                    "delete %d invalid bone mapping%s", numDeleted,
                    numDeleted == 1 ? "" : "s");
            editState.setEdited(description);
        }
    }

    /**
     * Access the edit state for this map.
     *
     * @return the pre-existing instance (not null)
     */
    public EditState getEditState() {
        assert editState != null;
        return editState;
    }

    /**
     * Replace the map with its own inverse.
     */
    public void invert() {
        if (map.countMappings() > 0) {
            History.autoAdd();
            map = map.inverse();
            baseAssetPath = "";
            editState.setEdited("invert the skeleton map");
        }
    }

    /**
     * Replace the map with an identity map for the specified C-G model.
     *
     * @param cgm which C-G model (not null, unaffected)
     */
    public void loadIdentityFor(Cgm cgm) {
        SelectedSkeleton skeleton = cgm.getSkeleton();
        List<String> boneNames = skeleton.listBoneNamesRaw();

        History.autoAdd();
        map.clear();
        for (String name : boneNames) {
            map.map(name, name);
        }

        assetRootPath = "";
        baseAssetPath = "";
        int numBones = boneNames.size();
        String event = String.format("load an identity map with %d bone%s",
                numBones, numBones == 1 ? "" : "s");
        editState.setEdited(event);
    }

    /**
     * Add a bone mapping for the selected source and target bones.
     */
    public void mapBones() {
        Cgm source = Maud.getModel().getSource();
        Cgm target = Maud.getModel().getTarget();
        if (!isBoneMappingSelected()
                && source.getBone().isSelected()
                && target.getBone().isSelected()) {
            History.autoAdd();
            String sourceBoneName = source.getBone().name();
            String targetBoneName = target.getBone().name();

            // Remove any prior mappings involving those bones.
            BoneMapping boneMapping = map.getForSource(sourceBoneName);
            if (boneMapping != null) {
                map.removeMapping(boneMapping);
            }
            boneMapping = map.get(targetBoneName);
            if (boneMapping != null) {
                map.removeMapping(boneMapping);
            }

            // Predict what the twist will be.
            Quaternion twist = estimateTwist();
            map.map(targetBoneName, sourceBoneName, twist);

            String event = "map bone " + targetBoneName;
            editState.setEdited(event);
        }
    }

    /**
     * Callback after a bone in the target C-G model is renamed.
     *
     * @param oldName former name of bone (not null)
     * @param newName new name of bone (not null, not empty)
     */
    public void renameBone(String oldName, String newName) {
        Validate.nonNull(oldName, "old name");
        Validate.nonEmpty(newName, "new name");

        if (isInvertingMap()) {
            map.renameSourceBone(oldName, newName);
        } else {
            map.renameTargetBone(oldName, newName);
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

        String targetBoneName = boneMapping.getTargetName();
        editState.setEditedTwist(targetBoneName);
    }

    /**
     * Snap one axis-angle of the effective twist.
     *
     * @param axisIndex which axis: 0&rarr;X, 1&rarr;Y, 2&rarr;Z
     */
    public void snapTwist(int axisIndex) {
        Validate.inRange(axisIndex, "axis index", MyVector3f.firstAxis,
                MyVector3f.lastAxis);

        BoneMapping boneMapping = selectedMapping();
        Quaternion twist = boneMapping.getTwist();
        MyQuaternion.snapLocal(twist, axisIndex);

        String targetBoneName = boneMapping.getTargetName();
        editState.setEditedTwist(targetBoneName);
    }

    /**
     * Unload the map.
     */
    public void unload() {
        History.autoAdd();
        map.clear();
        assetRootPath = "";
        baseAssetPath = "";
        editState.setEdited("unload map");
    }

    /**
     * Write the map to the filesystem, in the specified format, at the
     * specified base path. TODO refactor code shared with EditableCgm
     *
     * @param baseFilePath file path without any extension (not null, not empty)
     * @param format the output format (not null)
     * @return true if successful, otherwise false
     */
    public boolean writeToFile(CgmOutputFormat format, String baseFilePath) {
        Validate.nonNull(format, "format");
        Validate.nonEmpty(baseFilePath, "base file path");

        String filePath = format.extend(baseFilePath);
        File file = new File(filePath);
        /*
         * create the parent folder (see JME issue #1011)
         */
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        filePath = Heart.fixedPath(file);
        JmeExporter exporter = format.getExporter();
        boolean success = true;
        try {
            exporter.save(map, file);
        } catch (IOException exception) {
            System.err.println(exception);
            success = false;
        }

        String quotedPath = MyString.quote(filePath);
        if (success) {
            logger.log(Level.INFO, "Wrote map to file {0}", quotedPath);
        } else {
            logger.log(Level.SEVERE,
                    "I/O exception while writing map to file {0}", quotedPath);
        }

        if (success) {
            boolean maudCanLoadIt = (format == CgmOutputFormat.J3O);
            String af = assetFolderForWrite();
            String eventDescription = "write map to " + filePath;
            if (maudCanLoadIt && baseFilePath.startsWith(af)) {
                /*
                 * The map was successfully written to the "Written Assets"
                 * folder in a format that Maud can load, so update the
                 * origin information and mark as pristine.
                 */
                assetRootPath = af;
                baseAssetPath = MyString.remainder(baseFilePath, af);
                /*
                 * In asset paths, a leading slash is always redundant.
                 */
                if (baseAssetPath.startsWith("/")) {
                    baseAssetPath = MyString.remainder(baseAssetPath, "/");
                }
                editState.setPristine(eventDescription);

            } else if (maudCanLoadIt && baseFilePath.endsWith(baseAssetPath)
                    && !baseAssetPath.isEmpty()) {
                /*
                 * The map was successfully written to another part of the
                 * filesystem in a format that Maud can load, so update the
                 * origin information and mark as pristine.
                 */
                assetRootPath = MyString.removeSuffix(baseFilePath,
                        baseAssetPath);
                editState.setPristine(eventDescription);

            } else {
                /*
                 * Don't update the origin information, don't mark as pristine.
                 */
                History.addEvent(eventDescription);
            }
        }

        return success;
    }
    // *************************************************************************
    // LoadedMap methods

    /**
     * Create a deep copy of this object.
     *
     * @return a new object, equivalent to this one
     * @throws CloneNotSupportedException if the superclass isn't cloneable
     */
    @Override
    public EditableMap clone() throws CloneNotSupportedException {
        EditableMap clone = (EditableMap) super.clone();
        clone.editState = editState.clone();
        return clone;
    }

    /**
     * Unload the current map and load the specified asset.
     *
     * @param spec URL specification, or null for the default location
     * @param assetPath path to the asset to load (not null, not empty)
     * @return true if successful, otherwise false
     */
    @Override
    public boolean loadAsset(String spec, String assetPath) {
        Validate.nonEmpty(assetPath, "asset path");

        boolean success = super.loadAsset(spec, assetPath);
        if (success) {
            String eventDescription = String.format("load map %s %s",
                    MyString.quote(spec), MyString.quote(assetPath));
            editState.setPristine(eventDescription);
        }

        return success;
    }

    /**
     * Unload the current map and load the named one from the classpath.
     *
     * @param mapName which map to load (not null, not empty)
     * @return true if successful, otherwise false
     */
    @Override
    public boolean loadNamed(String mapName) {
        Validate.nonEmpty(mapName, "map name");

        boolean success = super.loadNamed(mapName);
        if (success) {
            String eventDescription = String.format("load map named %s",
                    MyString.quote(mapName));
            editState.setPristine(eventDescription);
        }

        return success;
    }
    // *************************************************************************
    // private methods

    /**
     * Determine the default asset folder for writing the map to the filesystem.
     *
     * @return absolute filesystem path (not null, not empty)
     */
    private String assetFolderForWrite() {
        String result = assetRootPath;
        if (result.isEmpty() || result.endsWith(".jar")
                || result.endsWith(".zip")) {
            result = ActionApplication.sandboxPath();
        }

        return result;
    }

    /**
     * Predict what the twist should be for the selected mapping.
     *
     * @return a new quaternion
     */
    private static Quaternion estimateTwist() {
        EditorModel model = Maud.getModel();
        Quaternion sourceMo
                = model.getSource().getBone().modelOrientation(null);
        Quaternion targetMo
                = model.getTarget().getBone().modelOrientation(null);
        Quaternion invSourceMo = sourceMo.inverse();
        Quaternion twist = invSourceMo.mult(targetMo, null);
        MyQuaternion.cardinalizeLocal(twist);

        return twist;
    }
}
