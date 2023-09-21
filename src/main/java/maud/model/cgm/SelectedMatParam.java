/*
 Copyright (c) 2017-2022, Stephen Gold
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

import com.jme3.material.MatParam;
import com.jme3.material.MatParamOverride;
import com.jme3.material.Material;
import com.jme3.scene.Spatial;
import com.jme3.shader.VarType;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;

/**
 * The MVC model of the selected material parameter in a loaded C-G model.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SelectedMatParam implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SelectedMatParam.class.getName());
    // *************************************************************************
    // fields

    /**
     * C-G model containing the parameter (set by {@link #setCgm(Cgm)})
     */
    private Cgm cgm = null;
    /**
     * editable C-G model, if any, containing the parameter (set by
     * {@link #setCgm(Cgm)})
     */
    private EditableCgm editableCgm;
    /**
     * name of the selected parameter, or null if none selected
     */
    private String selectedName = null;
    // *************************************************************************
    // new methods exposed

    /**
     * Delete (and deselect) the selected parameter and deselect its texture if
     * any.
     */
    public void delete() {
        if (isSelected() && editableCgm != null) {
            MatParamRef ref = makeRef();
            editableCgm.deleteMatParam();
            selectedName = null;
            cgm.getTexture().deselectRef(ref);
        }
    }

    /**
     * Deselect the selected parameter, if any.
     */
    public void deselect() {
        selectedName = null;
    }

    /**
     * Access the selected parameter.
     *
     * @return the pre-existing instance, or null if none selected
     */
    MatParam find() {
        MatParam result = null;
        if (isSelected()) {
            result = find(selectedName);
        }

        return result;
    }

    /**
     * Access the named parameter in the selected material.
     *
     * @param parameterName which parameter (not null)
     * @return the pre-existing instance, or null if not found
     */
    MatParam find(String parameterName) {
        assert parameterName != null;

        MatParam result = null;
        if (cgm.isLoaded()) {
            Material material = cgm.getSpatial().getMaterial();
            result = material.getParam(parameterName);
            if (result != null) {
                assert result.getName() != null;
                assert result.getName().equals(parameterName);
            }
        }

        return result;
    }

    /**
     * Find the index of the selected parameter (in name-lexical order).
     *
     * @return index, or -1 if none selected
     */
    public int findNameIndex() {
        int index = -1;
        if (isSelected()) {
            SelectedSpatial spatial = cgm.getSpatial();
            List<String> nameList
                    = spatial.listMatParamNames("", WhichParams.Defined);
            index = nameList.indexOf(selectedName);
        }

        return index;
    }

    /**
     * Read the name of the selected parameter.
     *
     * @return a parameter name, or null if none selected
     */
    public String getName() {
        return selectedName;
    }

    /**
     * Read the parameter's value.
     *
     * @return the pre-existing object, or null if none
     */
    public Object getValue() {
        Object value = null;
        MatParam mp = find();
        if (mp != null) {
            value = mp.getValue();
        }

        return value;
    }

    /**
     * Read the parameter's type.
     *
     * @return an enum value, or null if none selected
     */
    public VarType getVarType() {
        VarType varType = null;
        MatParam mp = find();
        if (mp != null) {
            varType = mp.getVarType();
        }

        return varType;
    }

    /**
     * Test whether the parameter is overridden.
     *
     * @return true if overridden, otherwise false
     */
    public boolean isOverridden() {
        Spatial spatial = cgm.getSpatial().find();
        while (spatial != null) {
            List<MatParamOverride> mpos = spatial.getLocalMatParamOverrides();
            for (MatParamOverride mpo : mpos) {
                if (mpo.getName().equals(selectedName)
                        && mpo.isEnabled()
                        && mpo.getValue() != null) {
                    return true;
                }
            }
            spatial = spatial.getParent();
        }

        return false;
    }

    /**
     * Test whether a parameter is selected.
     *
     * @return true if selected, otherwise false
     */
    public boolean isSelected() {
        boolean result;
        if (selectedName == null) {
            result = false;
        } else {
            result = true;
        }

        return result;
    }

    /**
     * Generate a reference to the selected parameter.
     *
     * @return a new instance (not null)
     */
    public MatParamRef makeRef() {
        MatParam mp = find();
        Material material = cgm.getSpatial().getMaterial();
        MatParamRef result = new MatParamRef(mp, material);

        return result;
    }

    /**
     * Update the selected parameter after a new material is applied.
     *
     * @param newMaterial the material being applied (not null)
     */
    void postSetMaterial(Material newMaterial) {
        if (isSelected()) {
            MatParam result = newMaterial.getParam(selectedName);
            if (result == null) {
                deselect();
            }
        }
    }

    /**
     * Select the parameter with the specified name.
     *
     * @param parameterName a parameter name (not null, not empty)
     */
    public void select(String parameterName) {
        Validate.nonEmpty(parameterName, "parameter name");
        selectedName = parameterName;
    }

    /**
     * Select the next defined parameter (in name-lexical order).
     */
    public void selectNextName() {
        SelectedSpatial spatial = cgm.getSpatial();
        List<String> nameList
                = spatial.listMatParamNames("", WhichParams.Defined);
        if (isSelected() && !nameList.isEmpty()) {
            int numNames = nameList.size();
            int index = nameList.indexOf(selectedName);
            int nextIndex = MyMath.modulo(index + 1, numNames);
            String nextName = nameList.get(nextIndex);
            select(nextName);
        }
    }

    /**
     * Select the previous defined parameter (in name-lexical order).
     */
    public void selectPreviousName() {
        SelectedSpatial spatial = cgm.getSpatial();
        List<String> nameList
                = spatial.listMatParamNames("", WhichParams.Defined);
        if (isSelected() && !nameList.isEmpty()) {
            int numNames = nameList.size();
            int index = nameList.indexOf(selectedName);
            int nextIndex = MyMath.modulo(index - 1, numNames);
            String previousName = nameList.get(nextIndex);
            select(previousName);
        }
    }

    /**
     * Alter which C-G model contains the parameter. (Invoked only during
     * initialization and cloning.)
     *
     * @param newCgm (not null, alias created)
     */
    void setCgm(Cgm newCgm) {
        assert newCgm != null;
        assert newCgm.getMatParam() == this;

        cgm = newCgm;
        if (newCgm instanceof EditableCgm) {
            editableCgm = (EditableCgm) newCgm;
        } else {
            editableCgm = null;
        }
    }
    // *************************************************************************
    // Object methods

    /**
     * Create a deep copy of this object.
     *
     * @return a new object, equivalent to this one
     * @throws CloneNotSupportedException if the superclass isn't cloneable
     */
    @Override
    public SelectedMatParam clone() throws CloneNotSupportedException {
        SelectedMatParam clone = (SelectedMatParam) super.clone();
        return clone;
    }
}
