/*
 Copyright (c) 2018, Stephen Gold
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
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.shader.VarType;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyString;
import maud.MaudUtil;
import maud.view.scene.SceneView;

/**
 * A locator for a material parameter or M-P override.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class MatParamRef implements JmeCloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(MatParamRef.class.getName());
    // *************************************************************************
    // fields

    /**
     * material that includes the target parameter, or null if target is an M-P
     * override
     */
    private Material matParamMaterial;
    /**
     * spatial whose local list includes the target M-P override, or null if
     * target is a material parameter
     */
    private Spatial overrideSpatial;
    /**
     * name of the target material parameter or M-P override (not null, not
     * empty)
     */
    final private String parameterName;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a reference to a parameter in a material.
     *
     * @param matParam the material parameter instance (not null)
     * @param material which material contains the parameter instance (not null,
     * alias created)
     */
    MatParamRef(MatParam matParam, Material material) {
        assert material != null;
        String name = matParam.getName();
        assert name != null;
        assert !name.isEmpty();
        assert matParam.getValue() != null;

        matParamMaterial = material;
        overrideSpatial = null;
        parameterName = name;
    }

    /**
     * Instantiate a reference to a material-parameter override.
     *
     * @param override the M-P override instance (not null)
     * @param spatial whose local list contains the override instance (not null,
     * alias created)
     */
    MatParamRef(MatParamOverride override, Spatial spatial) {
        assert spatial != null;
        String name = override.getName();
        assert name != null;
        assert !name.isEmpty();

        matParamMaterial = null;
        overrideSpatial = spatial;
        parameterName = name;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Access the material that references the parameter.
     *
     * @return the pre-existing material, or null if none
     */
    Material getMaterial() {
        return matParamMaterial;
    }

    /**
     * Access the spatial whose local list references the override.
     *
     * @return the pre-existing spatial, or null if none
     */
    Spatial getOverrideSpatial() {
        return overrideSpatial;
    }

    /**
     * Access the parameter value of the target.
     *
     * @return an object (may be null if target is an override)
     */
    Object getParameterValue() {
        MatParam target = getTarget();
        Object result = target.getValue();

        if (result == null) {
            assert isOverride();
        }
        return result;
    }

    /**
     * Access the target material parameter or M-P override.
     *
     * @return the pre-existing instance (not null)
     */
    MatParam getTarget() {
        MatParam target;
        if (isInMaterial()) {
            assert overrideSpatial == null;
            target = matParamMaterial.getParam(parameterName);
        } else {
            assert matParamMaterial == null;
            target = MaudUtil.findOverride(overrideSpatial, parameterName);
        }

        assert target != null;
        return target;
    }

    /**
     * Test whether the target is a parameter in a material.
     *
     * @return true if in material, otherwise false
     */
    boolean isInMaterial() {
        if (matParamMaterial == null) {
            assert overrideSpatial != null;
            return false;
        } else {
            assert overrideSpatial == null;
            return true;
        }
    }

    /**
     * Test whether the target is a material-parameter override in a spatial.
     *
     * @return true if override, otherwise false
     */
    boolean isOverride() {
        if (overrideSpatial == null) {
            assert matParamMaterial != null;
            return false;
        } else {
            assert matParamMaterial == null;
            return true;
        }
    }

    /**
     * Read the target's parameter name.
     *
     * @return name (not null, not empty)
     */
    String parameterName() {
        return parameterName;
    }

    /**
     * Alter the value of the target, both in the MVC model and the specified
     * scene view.
     *
     * @param desiredValue (may be null only for override, alias created)
     */
    void setValue(Object desiredValue, Cgm cgm) {
        if (desiredValue == null) {
            assert isOverride();
        }

        MatParam target = getTarget();
        target.setValue(desiredValue);

        Object viewValue = Misc.deepClone(desiredValue);
        SceneView sceneView = cgm.getSceneView();
        Spatial cgmRoot = cgm.getRootSpatial();
        VarType varType = target.getVarType();

        if (isInMaterial()) {
            matParamMaterial.setKey(null);
            List<Geometry> matSpatials
                    = MaudUtil.listUsers(cgmRoot, matParamMaterial, null);
            Spatial matSpatial = matSpatials.get(0);
            List<Integer> treePosition = cgm.findSpatial(matSpatial);
            sceneView.setParamValue(treePosition, parameterName, varType,
                    viewValue);
        } else {
            List<Integer> treePosition = cgm.findSpatial(overrideSpatial);
            sceneView.setOverrideValue(treePosition, parameterName, varType,
                    viewValue);
        }
    }

    /**
     * Read the type of the target.
     *
     * @return enum value (not null)
     */
    VarType type() {
        MatParam target = getTarget();
        VarType result = target.getVarType();

        assert result != null;
        return result;
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
    public MatParamRef clone() throws CloneNotSupportedException {
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
        matParamMaterial = cloner.clone(matParamMaterial);
        overrideSpatial = cloner.clone(overrideSpatial);
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public MatParamRef jmeClone() {
        try {
            MatParamRef clone = (MatParamRef) super.clone();
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException(exception);
        }
    }
    // *************************************************************************
    // Object methods

    /**
     * Test for equivalency with another reference.
     *
     * @param otherObject (may be null, unaffected)
     * @return true if the references are equivalent, otherwise false
     */
    @Override
    public boolean equals(Object otherObject) {
        boolean result;
        if (this == otherObject) {
            result = true;
        } else if (otherObject instanceof MatParamRef) {
            MatParamRef otherRef = (MatParamRef) otherObject;
            result = otherRef.getMaterial() == matParamMaterial
                    && otherRef.getOverrideSpatial() == overrideSpatial;
        } else {
            result = false;
        }

        return result;
    }

    /**
     * Generate the hash code for this reference.
     *
     * @return value for use in hashing
     */
    @Override
    public int hashCode() {
        int hash = 33;
        hash = 17 * hash + Objects.hashCode(matParamMaterial);
        hash = 17 * hash + Objects.hashCode(overrideSpatial);

        return hash;
    }

    /**
     * Describe this reference as a text string.
     *
     * @return a descriptive string of text (not null, not empty)
     */
    @Override
    public String toString() {
        String result;

        if (overrideSpatial != null) {
            String name = overrideSpatial.getName();
            result = MyString.quote(parameterName) + "override"
                    + MyString.quote(name);
        } else {
            String name = matParamMaterial.getName();
            result = MyString.quote(parameterName) + "param"
                    + MyString.quote(name);
        }

        return result;
    }
}
