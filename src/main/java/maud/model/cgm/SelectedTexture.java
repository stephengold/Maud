/*
 Copyright (c) 2018-2021, Stephen Gold
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
package maud.model.cgm;

import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.asset.TextureKey;
import com.jme3.material.MatParam;
import com.jme3.material.MatParamOverride;
import com.jme3.material.Material;
import com.jme3.scene.Spatial;
import com.jme3.shader.VarType;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture3D;
import com.jme3.texture.TextureCubeMap;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Heart;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.nifty.WindowController;
import jme3utilities.ui.Locators;
import maud.DescribeUtil;
import maud.Maud;
import maud.MaudUtil;
import maud.model.History;

/**
 * The MVC model of the selected texture in a loaded C-G model.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SelectedTexture implements JmeCloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SelectedTexture.class.getName());
    // *************************************************************************
    // fields

    /**
     * C-G model containing the texture (set by {@link #setCgm(Cgm)})
     */
    private Cgm cgm;
    /**
     * editable C-G model, if any, containing the texture (set by
     * {@link #setCgm(Cgm)})
     */
    private EditableCgm editableCgm;
    /**
     * list of references being edited, or empty list if none
     */
    private List<MatParamRef> selectedRefs = new ArrayList<>(4);
    /**
     * most recent asset path (not null)
     */
    private String lastAssetPath = "Textures/untitled.jpg";
    /**
     * selected texture, or null if nulled out
     */
    private Texture selectedTexture = null;
    // *************************************************************************
    // new methods exposed

    /**
     * Read the anisotropy of the selected texture. (Key required.)
     *
     * @return the value (&ge;0)
     */
    public int anisotropy() {
        TextureKey key = getKey();
        int result = key.getAnisotropy();

        return result;
    }

    /**
     * Read the asset path of the selected texture. (Key required.)
     *
     * @return the path (not empty, not null)
     */
    public String assetPath() {
        TextureKey key = getKey();
        String result = key.getName();

        return result;
    }

    /**
     * Select the first selected reference and replace it with a clone.
     */
    public void cloneFirstRef() {
        assert isSelected();
        assert selectedTexture != null;

        MatParamRef firstRef = selectedRefs.get(0);
        Texture clone = Cloner.deepClone(selectedTexture);
        editableCgm.selectAndReplaceTexture(firstRef, clone, "clone texture");
    }

    /**
     * Count how many distinct non-null textures are in the loaded C-G model.
     *
     * @return the count (&ge;0)
     */
    public int countNonNulls() {
        List<Texture> nonNulls = listNonNullTextures();
        int count = nonNulls.size();

        return count;
    }

    /**
     * Count how many selectables (distinct non-null textures plus distinct
     * references to null textures) are in the loaded C-G model.
     *
     * @return count (&ge;0)
     */
    public int countSelectables() {
        List<Texture> nonNulls = listNonNullTextures();
        List<MatParamRef> nullRefs = listTextureRefs(null);
        int count = nonNulls.size() + nullRefs.size();

        return count;
    }

    /**
     * Count how many texture references are selected.
     *
     * @return count (&ge;0)
     */
    public int countSelectedRefs() {
        int numSelectedRefs = selectedRefs.size();
        return numSelectedRefs;
    }

    /**
     * Instantiate a new texture and assign it to all selected references.
     */
    public void create() {
        assert isSelected();

        Texture.Type typeHint = Texture.Type.TwoDimensional;
        MatParamRef firstRef = selectedRefs.get(0); // just a heuristic
        if (firstRef != null) {
            VarType type = firstRef.type();
            switch (type) {
                case Texture3D:
                    typeHint = Texture.Type.ThreeDimensional;
                    break;
                case TextureArray:
                    typeHint = Texture.Type.TwoDimensionalArray;
                    break;
                case TextureCubeMap:
                    typeHint = Texture.Type.CubeMap;
                    break;
                default:
            }
        }

        boolean flipY = false;
        TextureKey key = new TextureKey(lastAssetPath, flipY);
        key.setAnisotropy(0);
        key.setGenerateMips(true);
        key.setTextureTypeHint(typeHint);

        AssetManager assetManager = Locators.getAssetManager();
        Texture texture = assetManager.loadAsset(key);
        if (texture != selectedTexture) {
            editableCgm.replaceSelectedTexture(texture, "create texture");
            selectedTexture = texture;
        }
    }

    /**
     * Describe the selected texture.
     *
     * @return a textual description, or "" if none selected (not null)
     */
    public String describe() {
        String result = "";
        if (isSelected()) {
            result = DescribeUtil.texture(selectedTexture);
        }

        return result;
    }

    /**
     * Describe the first texture reference.
     *
     * @return a textual description (not null, not empty)
     */
    public String describeFirstRef() {
        MatParamRef firstRef = selectedRefs.get(0);
        String result = firstRef.toString();

        return result;
    }

    /**
     * Describe the image of the selected non-null texture.
     *
     * @return a textual description (not null, not empty)
     */
    public String describeImage() {
        Image image = selectedTexture.getImage();
        String result = image.toString();

        assert result != null;
        assert !result.isEmpty();
        return result;
    }

    /**
     * Deselect the specified texture reference.
     *
     * @param ref which reference to deselect (not null)
     */
    void deselectRef(MatParamRef ref) {
        assert ref != null;

        selectedRefs.remove(ref);
        if (selectedRefs.isEmpty()) {
            selectedTexture = null;
        }
    }

    /**
     * Deselect the selected texture and all references to it.
     */
    public void deselectAll() {
        selectedRefs.clear();
        selectedTexture = null;
    }

    /**
     * Determine the index of the selected texture among all selectable
     * textures, including nulls.
     *
     * @return the index, or -1 if none selected
     */
    public int findIndex() {
        int index = -1;
        if (isSelected()) {
            List<Texture> nonNulls = listNonNullTextures();
            if (selectedTexture == null) {
                assert selectedRefs.size() == 1;
                List<MatParamRef> nullRefs = listTextureRefs(null);
                index = nullRefs.indexOf(selectedRefs.get(0));
                assert index != -1;
                index += nonNulls.size();
            } else {
                index = nonNulls.indexOf(selectedTexture);
                assert index != -1;
            }
        }

        return index;
    }

    /**
     * Access the Texture itself.
     *
     * @return the pre-existing instance, or null if nulled out
     */
    public Texture get() {
        return selectedTexture;
    }

    /**
     * Access the key of the selected texture.
     *
     * @return the pre-existing instance, or null if none
     */
    TextureKey getKey() {
        TextureKey textureKey = null;
        if (selectedTexture != null) {
            AssetKey assetKey = selectedTexture.getKey();
            if (assetKey instanceof TextureKey) {
                textureKey = (TextureKey) assetKey;
            }
        }

        return textureKey;
    }

    /**
     * Test whether selected texture has an asset key.
     *
     * @return true if it has a key, otherwise false
     */
    public boolean hasKey() {
        TextureKey key = getKey();
        if (key == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether the texture has an image.
     *
     * @return true if it has an image, otherwise false
     */
    public boolean hasImage() {
        if (selectedTexture == null) {
            return false;
        } else if (selectedTexture.getImage() == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether the texture has an R axis.
     *
     * @return true if it has an R axis, otherwise false
     */
    public boolean hasRAxis() {
        if (selectedTexture instanceof Texture3D
                || selectedTexture instanceof TextureCubeMap) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Test whether the first selected reference can be set to null.
     *
     * @return true if nullable, otherwise false
     */
    public boolean isFirstNullable() {
        MatParamRef firstRef = selectedRefs.get(0);
        if (firstRef.isInMaterial()) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Read the flipY flag of the selected texture. (Key required.)
     *
     * @return true if flipped, otherwise false
     */
    public boolean isFlipY() {
        TextureKey key = getKey();
        boolean result = key.isFlipY();

        return result;
    }

    /**
     * Read the generateMips flag of the selected texture. (Key required.)
     *
     * @return false if flag is false, otherwise true
     */
    public boolean isGenerateMips() {
        TextureKey key = getKey();
        boolean result = key.isGenerateMips();

        return result;
    }

    /**
     * Test whether the selected texture is null.
     *
     * @return true if null, otherwise false
     */
    public boolean isNull() {
        if (selectedTexture == null) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Test whether a texture is selected.
     *
     * @return true if one is selected, false if none is selected
     */
    public boolean isSelected() {
        if (selectedRefs.isEmpty()) {
            assert selectedTexture == null;
            return false;
        } else {
            return true;
        }
    }

    /**
     * Determine the most recent asset path for a Texture.
     *
     * @return the path (not empty, not null)
     */
    public String lastAssetPath() {
        assert lastAssetPath != null;
        return lastAssetPath;
    }

    /**
     * Enumerate all selectables with the specified description prefix.
     *
     * @param prefix (not null)
     * @return a new list of texture- and MatParamRef descriptions
     */
    public List<String> listSelectables(String prefix) {
        List<Texture> nonNulls = listNonNullTextures();
        List<MatParamRef> nullRefs = listTextureRefs(null);
        int numSelectables = nonNulls.size() + nullRefs.size();
        List<String> result = new ArrayList<>(numSelectables);

        for (Texture texture : nonNulls) {
            String desc = DescribeUtil.texture(texture);
            if (desc.startsWith(prefix)) {
                result.add(desc);
            }
        }
        for (MatParamRef ref : nullRefs) {
            String desc = ref.toString();
            if (desc.startsWith(prefix)) {
                result.add(desc);
            }
        }

        return result;
    }

    /**
     * Read the magnification-filter mode of the selected non-null texture.
     *
     * @return the mode enum value (not null)
     */
    public Texture.MagFilter magFilter() {
        Texture.MagFilter result = selectedTexture.getMagFilter();

        assert result != null;
        return result;
    }

    /**
     * Read the minification-filter mode of the selected non-null texture.
     *
     * @return the mode enum value (not null)
     */
    public Texture.MinFilter minFilter() {
        Texture.MinFilter result = selectedTexture.getMinFilter();

        assert result != null;
        return result;
    }

    /**
     * Select the first selected reference and set it to null.
     */
    public void nullifyFirst() {
        assert isSelected();

        MatParamRef firstRef = selectedRefs.get(0);
        editableCgm.selectAndReplaceTexture(firstRef, null, "nullify texture");
    }

    /**
     * Update the selected texture after a major change to the C-G model,
     * deselecting any references that are no longer valid.
     */
    void postEdit() {
        List<MatParamRef> allRefs = listTextureRefs(selectedTexture);
        List<MatParamRef> oldRefs = new ArrayList<>(selectedRefs);

        selectedRefs.clear();
        for (MatParamRef ref : oldRefs) {
            if (allRefs.contains(ref)) {
                selectedRefs.add(ref);
            }
        }

        if (!isSelected()) {
            selectedTexture = null;
        } else {
            MatParamRef firstRef = selectedRefs.get(0);
            selectedTexture = (Texture) firstRef.getParameterValue();
        }
    }

    /**
     * Replace textures whose asset paths match the specified search pattern.
     *
     * @param match the CharSequence to search for (not null)
     * @param replacement the CharSequence to substitute for each match (not
     * null)
     */
    public void replaceMatchingTextures(CharSequence match,
            CharSequence replacement) {
        int replacementCount = 0;
        List<Texture> nonNullTextures = listNonNullTextures();

        History.autoAdd();
        for (Texture texture : nonNullTextures) {
            AssetKey assetKey = texture.getKey();
            if (assetKey instanceof TextureKey) {
                String oldPath = texture.getName();
                String newPath = oldPath.replace(match, replacement);
                if (!oldPath.equals(newPath)) {
                    TextureKey textureKey = (TextureKey) assetKey;
                    int anisotropy = textureKey.getAnisotropy();
                    boolean flipY = textureKey.isFlipY();
                    boolean generateMips = textureKey.isGenerateMips();
                    Texture.Type typeHint = textureKey.getTextureTypeHint();
                    Texture newTexture = null;
                    try {
                        newTexture = load(newPath, anisotropy, flipY,
                                generateMips, typeHint);
                    } catch (AssetNotFoundException exception) {
                        logger.log(Level.WARNING, "{0} not found, skipped.",
                                MyString.quote(newPath));
                    }
                    if (newTexture != null) {
                        List<MatParamRef> refs = listTextureRefs(texture);
                        for (MatParamRef reference : refs) {
                            reference.setValue(newTexture, cgm);
                            ++replacementCount;
                        }
                    }
                }
            }
        }

        String eventDescription = String.format("replace %d texture%s",
                replacementCount, (replacementCount == 1) ? "" : "s");
        editableCgm.getEditState().setEdited(eventDescription);
    }

    /**
     * Replace the specified texture reference with a new one.
     *
     * @param oldRef which reference to replace (not null)
     * @param newRef the new reference (not null)
     */
    void replaceRef(MatParamRef oldRef, MatParamRef newRef) {
        assert oldRef != null;
        assert newRef != null;

        if (selectedRefs.contains(oldRef)) {
            selectedRefs.remove(oldRef);
            selectedRefs.add(newRef);
        }
    }

    /**
     * Alter all references to the selected texture. Should be invoked only from
     * the EditableCgm class.
     *
     * @param newTexture (may be null only if exactly one reference is selected,
     * alias created)
     */
    void replaceTexture(Texture newTexture) {
        if (selectedRefs.size() != 1) {
            assert newTexture != null;
        }

        for (MatParamRef reference : selectedRefs) {
            reference.setValue(newTexture, cgm);
        }
        selectedTexture = newTexture;
    }

    /**
     * Select the specified texture reference, deselecting all others.
     *
     * @param reference the reference to select (not null)
     */
    public void select(MatParamRef reference) {
        Validate.nonNull(reference, "reference");

        selectedRefs.clear();
        selectedRefs.add(reference);

        Object value = reference.getParameterValue();
        selectedTexture = (Texture) value;

        if (selectedTexture != null) {
            AssetKey key = selectedTexture.getKey();
            if (key instanceof TextureKey) {
                lastAssetPath = key.getName();
            }
        }
    }

    /**
     * Select the described texture or null-texture reference.
     *
     * @param desc the description (not null, not empty)
     */
    public void select(String desc) {
        Validate.nonEmpty(desc, "description");

        List<Texture> textures = listNonNullTextures();
        for (Texture texture : textures) {
            if (DescribeUtil.texture(texture).equals(desc)) {
                selectAllRefs(texture);
                return;
            }
        }

        Spatial cgmRoot = cgm.getRootSpatial();
        MatParamRef ref = new MatParamRef(desc, cgmRoot);
        select(ref);
    }

    /**
     * Select all references to the specified non-null texture.
     *
     * @param texture the texture to select (not null)
     */
    void selectAllRefs(Texture texture) {
        Validate.nonNull(texture, "texture");

        selectedRefs = listTextureRefs(texture);
        selectedTexture = texture;

        AssetKey key = selectedTexture.getKey();
        if (key instanceof TextureKey) {
            lastAssetPath = key.getName();
        }
    }

    /**
     * Select the material parameter or M-P override of the first selected
     * reference.
     */
    public void selectFirstUser() {
        assert isSelected();

        WindowController tool;
        MatParamRef firstRef = selectedRefs.get(0);
        String parameterName = firstRef.parameterName();
        if (firstRef.isOverride()) {
            Spatial spatial = firstRef.getOverrideSpatial();
            cgm.getSpatial().select(spatial);
            cgm.getOverride().select(parameterName);
            tool = Maud.gui.findTool("overrides");
        } else {
            assert firstRef.isInMaterial();
            Material material = firstRef.getMaterial();
            cgm.getSpatial().selectMaterial(material);
            cgm.getMatParam().select(parameterName);
            tool = Maud.gui.findTool("material");
        }
        tool.select();
    }

    /**
     * Select the next selectable (in cyclical index order).
     */
    public void selectNext() {
        assert isSelected();

        int oldIndex = findIndex();
        assert oldIndex != -1;
        int newIndex = oldIndex + 1;
        int count = countSelectables();
        if (newIndex >= count) {
            newIndex = 0;
        }

        select(newIndex);
    }

    /**
     * Select the previous selectable (in cyclical index order).
     */
    public void selectPrevious() {
        assert isSelected();

        int oldIndex = findIndex();
        assert oldIndex != -1;
        int newIndex = oldIndex - 1;
        if (newIndex < 0) {
            int count = countSelectables();
            newIndex = count - 1;
        }

        select(newIndex);
    }

    /**
     * Alter the anisotropy of the selected texture. (Key required.)
     *
     * @param newValue the desired anisotropy (&ge;0)
     */
    public void setAnisotropy(int newValue) {
        Validate.nonNegative(newValue, "new value");
        assert hasKey();

        int oldValue = anisotropy();
        if (oldValue != newValue) {
            String assetPath = assetPath();
            boolean flipY = isFlipY();
            boolean generateMips = isGenerateMips();
            Texture.Type typeHint = typeHint();
            String eventDescription = String.format(
                    "change texture's anisotropy from %d to %d", oldValue,
                    newValue);
            set(assetPath, newValue, flipY, generateMips, typeHint,
                    eventDescription);
        }
    }

    /**
     * Alter the asset path of the selected texture. (Key required.)
     *
     * @param newPath the desired asset path (not null, not empty)
     */
    public void setAssetPath(String newPath) {
        Validate.nonEmpty(newPath, "new path");
        assert hasKey();

        String oldPath = assetPath();
        if (!newPath.equals(oldPath)) {
            int anisotropy = anisotropy();
            boolean flipY = isFlipY();
            boolean generateMips = isGenerateMips();
            Texture.Type typeHint = typeHint();
            set(newPath, anisotropy, flipY, generateMips, typeHint,
                    "change texture's asset");
        }
    }

    /**
     * Alter which C-G model contains the texture. (Invoked only during
     * initialization and cloning.)
     *
     * @param newCgm (not null, aliases created)
     */
    void setCgm(Cgm newCgm) {
        assert newCgm != null;
        assert newCgm.getTexture() == this;

        cgm = newCgm;
        if (newCgm instanceof EditableCgm) {
            editableCgm = (EditableCgm) newCgm;
        } else {
            editableCgm = null;
        }
    }

    /**
     * Alter the flipY flag of the selected texture. (Key required.)
     *
     * @param newSetting the desired setting (true&rarr;flip, false&rarr;don't
     * flip)
     */
    public void setFlipY(boolean newSetting) {
        assert hasKey();

        boolean oldSetting = isFlipY();
        if (oldSetting != newSetting) {
            int anisotropy = anisotropy();
            String assetPath = assetPath();
            boolean generateMips = isGenerateMips();
            Texture.Type typeHint = typeHint();
            String eventDescription = String.format(
                    "toggle texture's flipY to %s", newSetting);
            set(assetPath, anisotropy, newSetting, generateMips, typeHint,
                    eventDescription);
        }
    }

    /**
     * Alter the generateMips flag of the selected texture. (Key required.)
     *
     * @param newSetting the desired setting (true&rarr;generate MIP maps,
     * false&rarr;don't generate)
     */
    public void setGenerateMips(boolean newSetting) {
        assert hasKey();

        boolean oldSetting = isGenerateMips();
        if (oldSetting != newSetting) {
            int anisotropy = anisotropy();
            String assetPath = assetPath();
            boolean flipY = isFlipY();
            Texture.Type typeHint = typeHint();
            String eventDescription = String.format(
                    "toggle texture's generateMips to %s", newSetting);
            set(assetPath, anisotropy, flipY, newSetting, typeHint,
                    eventDescription);
        }
    }

    /**
     * Alter the magnification filter of the selected non-null texture.
     *
     * @param newFilter the desired filter (not null)
     */
    public void setMagFilter(Texture.MagFilter newFilter) {
        Validate.nonNull(newFilter, "new filter");
        assert !isNull();

        Texture.MagFilter oldFilter = magFilter();
        if (oldFilter != newFilter) {
            Texture clone = Cloner.deepClone(selectedTexture);
            clone.setMagFilter(newFilter);
            String eventDescription = String.format(
                    "change texture's magFilter from %s to %s", oldFilter,
                    newFilter);
            editableCgm.replaceSelectedTexture(clone, eventDescription);
        }
    }

    /**
     * Alter the minification filter of the selected non-null texture.
     *
     * @param newFilter the desired filter (not null)
     */
    public void setMinFilter(Texture.MinFilter newFilter) {
        Validate.nonNull(newFilter, "new filter");
        assert !isNull();

        Texture.MinFilter oldFilter = minFilter();
        if (oldFilter != newFilter) {
            Texture clone = Cloner.deepClone(selectedTexture);
            clone.setMinFilter(newFilter);
            String eventDescription = String.format(
                    "change texture's minFilter from %s to %s", oldFilter,
                    newFilter);
            editableCgm.replaceSelectedTexture(clone, eventDescription);
        }
    }

    /**
     * Alter the type hint of the selected texture. (Key required.)
     *
     * @param newHint the desired type hint (not null)
     */
    public void setTypeHint(Texture.Type newHint) {
        Validate.nonNull(newHint, "new hint");
        assert hasKey();

        Texture.Type oldHint = typeHint();
        if (newHint != oldHint) {
            String assetPath = assetPath();
            int anisotropy = anisotropy();
            boolean flipY = isFlipY();
            boolean generateMips = isGenerateMips();
            String eventDescription = String.format(
                    "change texture's typeHint from %s to %s", oldHint,
                    newHint);
            set(assetPath, anisotropy, flipY, generateMips, newHint,
                    eventDescription);
        }
    }

    /**
     * Alter one of the wrap modes of the selected non-null texture.
     *
     * @param axis the desired axis (not null)
     * @param newMode the desired mode (not null)
     */
    public void setWrapMode(Texture.WrapAxis axis, Texture.WrapMode newMode) {
        Validate.nonNull(axis, "axis");
        Validate.nonNull(newMode, "mode");
        assert !isNull();

        Texture.WrapMode oldMode = wrapMode(axis);
        if (oldMode != newMode) {
            Texture clone = Cloner.deepClone(selectedTexture);
            clone.setWrap(axis, newMode);
            String eventDescription = String.format(
                    "change texture's %s-axis wrapMode from %s to %s", axis,
                    oldMode, newMode);
            editableCgm.replaceSelectedTexture(clone, eventDescription);
        }
    }

    /**
     * Read the type hint of the selected texture. (Key required.)
     *
     * @return the texture type, or null if none
     */
    public Texture.Type typeHint() {
        Texture.Type result = null;
        TextureKey key = getKey();
        if (key != null) {
            result = key.getTextureTypeHint();
            assert result != null;
        }

        return result;
    }

    /**
     * Read the wrap mode of the selected texture for the specified axis.
     * (Non-null texture required.)
     *
     * @param axis which axis (not null)
     * @return an enum value (not null)
     */
    public Texture.WrapMode wrapMode(Texture.WrapAxis axis) {
        Texture.WrapMode result = selectedTexture.getWrap(axis);

        assert result != null;
        return result;
    }

    /**
     * Write the image to the specified asset path, then replace the selected
     * texture with a new texture keyed to that asset.
     *
     * @param assetPath asset path to the output file (not null, not empty)
     * @param flipY true&rarr;flip the Y coordinate, false&rarr;don't flip
     */
    public void writeImageToAsset(String assetPath, boolean flipY) {
        Validate.nonNull(assetPath, "asset path");

        int awtType = BufferedImage.TYPE_4BYTE_ABGR;
        String lowerCase = assetPath.toLowerCase();
        if (lowerCase.endsWith(".bmp")
                || lowerCase.endsWith(".jpg")
                || lowerCase.endsWith(".jpeg")) {
            awtType = BufferedImage.TYPE_3BYTE_BGR;
        }

        Image image = selectedTexture.getImage();
        RenderedImage renderedImage = MaudUtil.render(image, flipY, awtType);

        String filePath = Maud.filePath(assetPath);
        try {
            Heart.writeImage(filePath, renderedImage);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        TextureKey key = new TextureKey(assetPath, flipY);
        AssetManager assetManager = Locators.getAssetManager();
        Texture clone = assetManager.loadTexture(key);
        String description = "texture keyed to " + MyString.quote(assetPath);
        editableCgm.replaceSelectedTexture(clone, description);

        lastAssetPath = assetPath;
    }
    // *************************************************************************
    // JmeCloneable methods

    /**
     * Don't use this method; use a {@link com.jme3.util.clone.Cloner} instead.
     *
     * @return never
     * @throws CloneNotSupportedException always
     */
    @Override
    public SelectedTexture clone() throws CloneNotSupportedException {
        super.clone();
        throw new CloneNotSupportedException("use a cloner");
    }

    /**
     * Callback from {@link com.jme3.util.clone.Cloner} to convert this
     * shallow-cloned instance into a deep-cloned one, using the specified
     * cloner and original to resolve copied fields.
     *
     * @param cloner the cloner currently cloning this control (not null)
     * @param original the object from which this object was shallow-cloned
     * (unused)
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        selectedRefs = cloner.clone(selectedRefs);
        selectedTexture = cloner.clone(selectedTexture);
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public SelectedTexture jmeClone() {
        try {
            SelectedTexture clone = (SelectedTexture) super.clone();
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException(exception);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * If the specified parameter has a non-null texture value that's not
     * already in the specified collection, add it.
     *
     * @param collection (not null, modified)
     * @param param (not null, unaffected)
     */
    private static void addTexture(Collection<Texture> collection, MatParam param) {
        VarType varType = param.getVarType();
        switch (varType) {
            case Texture2D:
            case Texture3D:
            case TextureArray:
            case TextureCubeMap:
                Texture texture = (Texture) param.getValue();
                if (texture != null && !collection.contains(texture)) {
                    collection.add(texture);
                }
        }
    }

    /**
     * Enumerate all non-null texture instances in the C-G model.
     *
     * @return a new list (not null)
     */
    private List<Texture> listNonNullTextures() {
        Spatial cgmRoot = cgm.getRootSpatial();
        List<Texture> result = MySpatial.listTextures(cgmRoot, null);

        return result;
    }

    /**
     * Enumerate all references to the specified texture in the C-G model.
     *
     * @param texture the texture to find (may be null, unaffected)
     * @return a new list (not null)
     */
    private List<MatParamRef> listTextureRefs(Texture texture) {
        Spatial cgmRoot = cgm.getRootSpatial();

        List<MatParamRef> result = new ArrayList<>(20);
        if (texture != null) {
            List<Material> matList = MySpatial.listMaterials(cgmRoot, null);
            for (Material material : matList) {
                for (MatParam param : material.getParams()) {
                    if (referencesTexture(param, texture)) {
                        MatParamRef ref = new MatParamRef(param, material);
                        result.add(ref);
                    }
                }
            }
        }

        List<Spatial> spatList = MySpatial.listSpatials(cgmRoot);
        for (Spatial spatial : spatList) {
            for (MatParamOverride mpo : spatial.getLocalMatParamOverrides()) {
                if (referencesTexture(mpo, texture)) {
                    MatParamRef ref = new MatParamRef(mpo, spatial);
                    result.add(ref);
                }
            }
        }

        return result;
    }

    /**
     * Load a new Texture from an asset.
     *
     * @param assetPath (not null)
     * @param anisotropy (&ge;0)
     * @param flipY true&rarr;flip, false&rarr;don't flip
     * @param generateMips true&rarr;generate MIP maps, false&rarr;don't
     * generate
     * @param typeHint type hint (not null)
     * @return the loaded Texture, or null if failure
     */
    private static Texture load(String assetPath, int anisotropy, boolean flipY,
            boolean generateMips, Texture.Type typeHint) {
        assert assetPath != null;
        assert anisotropy >= 0 : anisotropy;
        assert typeHint != null;

        TextureKey key = new TextureKey(assetPath, flipY);
        key.setAnisotropy(anisotropy);
        key.setGenerateMips(generateMips);
        key.setTextureTypeHint(typeHint);

        Locators.save();
        Locators.registerDefault();
        List<String> specList = Maud.getModel().getLocations().listAll();
        Locators.register(specList);
        AssetManager assetManager = Locators.getAssetManager();
        Texture result = null;
        try {
            result = assetManager.loadTexture(key);
        } catch (ClassCastException exception) {
            logger.log(Level.WARNING, "Failed to load {0} as an image.",
                    MyString.quote(assetPath));
        }
        Locators.restore();

        return result;
    }

    /**
     * Test whether the specified parameter or override references the specified
     * texture.
     *
     * @param param the parameter or override to test (not null, unaffected)
     * @param texture the texture to find (may be null, unaffected)
     * @return true if referenced, otherwise false
     */
    private static boolean referencesTexture(MatParam param, Texture texture) {
        boolean result = false;

        VarType varType = param.getVarType();
        switch (varType) {
            case Texture2D:
            case Texture3D:
            case TextureArray:
            case TextureCubeMap:
                Object value = param.getValue();
                if (value == texture) {
                    result = true;
                }
        }

        return result;
    }

    /**
     * Select the indexed selectable.
     *
     * @param index the index of the selectable to select (&ge;0)
     */
    private void select(int index) {
        assert index >= 0 : index;

        List<Texture> nonNulls = listNonNullTextures();
        int numNonNulls = nonNulls.size();
        if (index < numNonNulls) {
            Texture texture = nonNulls.get(index);
            selectAllRefs(texture);
        } else {
            // Select a single reference to a null texture.
            int nullIndex = index - numNonNulls;
            List<MatParamRef> nullRefs = listTextureRefs(null);
            MatParamRef reference = nullRefs.get(nullIndex);
            select(reference);
        }
    }

    /**
     * Replace all selected references with a new texture, loaded from an asset.
     *
     * @param assetPath (not null)
     * @param anisotropy (&ge;0)
     * @param flipY true&rarr;flip, false&rarr;don't flip
     * @param generateMips true&rarr;generate MIP maps, false&rarr;don't
     * generate
     * @param typeHint type hint (not null)
     * @param eventDescription for the edit history (not null)
     */
    private void set(String assetPath, int anisotropy, boolean flipY,
            boolean generateMips, Texture.Type typeHint,
            String eventDescription) {
        assert anisotropy >= 0 : anisotropy;
        assert typeHint != null;
        assert eventDescription != null;

        Texture loaded = load(assetPath, anisotropy, flipY, generateMips,
                typeHint);
        if (loaded != null) {
            editableCgm.replaceSelectedTexture(loaded, eventDescription);
        }
    }
}
