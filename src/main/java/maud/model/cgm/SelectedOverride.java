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
package maud.model.cgm;

import com.jme3.material.MatParamOverride;
import com.jme3.scene.Spatial;
import com.jme3.shader.VarType;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;

/**
 * The MVC model of the selected material-parameter override in a loaded C-G
 * model.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SelectedOverride implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SelectedOverride.class.getName());
    /**
     * dummy parameter name used to indicate that no override is selected
     */
    final public static String noParam = "( no override )";
    // *************************************************************************
    // fields

    /**
     * C-G model containing the override (set by {@link #setCgm(Cgm)})
     */
    private Cgm cgm = null;
    /**
     * editable C-G model, if any, containing the override (set by
     * {@link #setCgm(Cgm)})
     */
    private EditableCgm editableCgm;
    /**
     * parameter name of the selected override in the selected spatial, or null
     * if none selected
     */
    private String selectedName = null;
    // *************************************************************************
    // new methods exposed

    /**
     * Delete (and deselect) the selected override and deselect its texture if
     * any.
     */
    public void delete() {
        if (isSelected() && editableCgm != null) {
            MatParamRef ref = makeRef();
            editableCgm.deleteOverride();
            selectedName = null;
            cgm.getTexture().deselectRef(ref);
        }
    }

    /**
     * Deselect the selected override, if any.
     */
    public void deselect() {
        selectedName = null;
    }

    /**
     * Access the selected override.
     *
     * @return the pre-existing instance, or null if none selected
     */
    MatParamOverride find() {
        MatParamOverride result = null;
        if (isSelected()) {
            result = find(selectedName);
        }

        return result;
    }

    /**
     * Access the named override.
     *
     * @param parameterName which override (not null)
     * @return the pre-existing instance, or null if none found
     */
    MatParamOverride find(String parameterName) {
        assert parameterName != null;

        MatParamOverride result = null;
        if (cgm.isLoaded()) {
            Spatial spatial = cgm.getSpatial().find();
            Collection<MatParamOverride> mpos
                    = spatial.getLocalMatParamOverrides();
            for (MatParamOverride mpo : mpos) {
                if (mpo.getName().equals(parameterName)) {
                    result = mpo;
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Find the index of the selected override (in name lexical order).
     *
     * @return index, or -1 if none selected
     */
    public int findNameIndex() {
        int index = -1;
        if (isSelected()) {
            List<String> nameList = cgm.getSpatial().listOverrideNames();
            index = nameList.indexOf(selectedName);
        }

        return index;
    }

    /**
     * Read the override's parameter value.
     *
     * @return the pre-existing object, or null if none
     */
    public Object getValue() {
        Object value = null;
        MatParamOverride mpo = find();
        if (mpo != null) {
            value = mpo.getValue();
        }

        return value;
    }

    /**
     * Test whether the override is enabled.
     *
     * @return true if enabled, otherwise false
     */
    public boolean isEnabled() {
        boolean result = false;
        if (isSelected()) {
            MatParamOverride mpo = find();
            result = mpo.isEnabled();
        }

        return result;
    }

    /**
     * Test whether an override is selected.
     *
     * @return true if selected, otherwise false
     */
    public boolean isSelected() {
        if (selectedName == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Generate a reference to the selected override.
     *
     * @return a new instance (not null)
     */
    public MatParamRef makeRef() {
        Spatial spatial = cgm.getSpatial().find();
        MatParamOverride mpo = find();
        MatParamRef result = new MatParamRef(mpo, spatial);

        return result;
    }

    /**
     * Read the override's parameter name.
     *
     * @return a parameter name, or null if none selected
     */
    public String parameterName() {
        return selectedName;
    }

    /**
     * Select the override with the specified parameter name.
     *
     * @param parameterName a parameter name (not null, not empty) or noParam
     */
    public void select(String parameterName) {
        Validate.nonEmpty(parameterName, "parameter name");

        if (parameterName.equals(noParam)) {
            deselect();
        } else {
            selectedName = parameterName;
        }
    }

    /**
     * Select the next override (in name lexical order).
     */
    public void selectNextName() {
        List<String> nameList = cgm.getSpatial().listOverrideNames();
        if (isSelected() && !nameList.isEmpty()) {
            int numNames = nameList.size();
            int index = nameList.indexOf(selectedName);
            int nextIndex = MyMath.modulo(index + 1, numNames);
            String nextName = nameList.get(nextIndex);
            select(nextName);
        }
    }

    /**
     * Select the previous override (in name lexical order).
     */
    public void selectPreviousName() {
        List<String> nameList = cgm.getSpatial().listOverrideNames();
        if (isSelected() && !nameList.isEmpty()) {
            int numNames = nameList.size();
            int index = nameList.indexOf(selectedName);
            int nextIndex = MyMath.modulo(index - 1, numNames);
            String previousName = nameList.get(nextIndex);
            select(previousName);
        }
    }

    /**
     * Alter which C-G model contains the override. (Invoked only during
     * initialization and cloning.)
     *
     * @param newCgm (not null, alias created)
     */
    void setCgm(Cgm newCgm) {
        assert newCgm != null;
        assert newCgm.getOverride() == this;

        cgm = newCgm;
        if (newCgm instanceof EditableCgm) {
            editableCgm = (EditableCgm) newCgm;
        } else {
            editableCgm = null;
        }
    }

    /**
     * Read the override's type.
     *
     * @return an enum value, or null if none selected
     */
    public VarType varType() {
        VarType varType = null;
        MatParamOverride mpo = find();
        if (mpo != null) {
            varType = mpo.getVarType();
        }

        return varType;
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
    public SelectedOverride clone() throws CloneNotSupportedException {
        SelectedOverride clone = (SelectedOverride) super.clone();
        return clone;
    }
}
