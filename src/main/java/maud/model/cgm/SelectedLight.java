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

import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.LightProbe;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyLight;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.math.MyVector3f;
import maud.MaudUtil;

/**
 * The MVC model of the selected light in a C-G model.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SelectedLight implements JmeCloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * dummy index, used to indicate that no light is found/selected
     */
    final public static int noLightIndex = -1;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SelectedLight.class.getName());
    /**
     * dummy light name used to indicate that no light is selected
     */
    final public static String noLight = "( no light )";
    // *************************************************************************
    // fields

    /**
     * C-G model containing the light (set by {@link #setCgm(Cgm)})
     */
    private Cgm cgm = null;
    /**
     * current selection, or null if none
     */
    private Light selected = null;
    /**
     * editable C-G model, if any, containing the light (set by
     * {@link #setCgm(Cgm)})
     */
    private EditableCgm editableCgm = null;
    /**
     * spatial to which the selected light is added, or null if no light is
     * selected
     */
    private Spatial owner = null;
    // *************************************************************************
    // new methods exposed

    /**
     * Test whether the light's direction can be altered.
     *
     * @return true if directable, otherwise false
     */
    public boolean canDirect() {
        boolean result = false;
        if (isSelected() && cgm instanceof EditableCgm) {
            if (selected instanceof DirectionalLight
                    || selected instanceof SpotLight) {
                result = true;
            }
        }

        return result;
    }

    /**
     * Test whether the light's "position" (location) can be altered.
     *
     * @return true if positionable, otherwise false
     */
    public boolean canPosition() {
        boolean result = false;
        if (isSelected() && cgm instanceof EditableCgm) {
            if (selected instanceof LightProbe
                    || selected instanceof PointLight
                    || selected instanceof SpotLight) {
                result = true;
            }
        }

        return result;
    }

    /**
     * Cardinalize the light's direction.
     */
    public void cardinalizeDirection() {
        Vector3f dir = direction();
        MaudUtil.cardinalizeLocal(dir);
        setDirection(dir);
    }

    /**
     * Copy the light's color.
     *
     * @return a new instance, or null if no light is selected
     */
    public ColorRGBA color() {
        ColorRGBA result = null;
        if (isSelected()) {
            result = selected.getColor().clone();
        }

        return result;
    }

    /**
     * Add a copy of the selected light to the selected spatial.
     *
     * @param name a name for the new light (not null, not empty)
     */
    public void copySelected(String name) {
        Validate.nonEmpty(name, "name");
        assert !cgm.hasLight(name);

        Light newLight = selected.clone();
        newLight.setName(name);

        String description = String.format("copy selected light, set name=%s",
                MyString.quote(name));
        editableCgm.addLight(newLight, description);

        Spatial spatial = cgm.getSpatial().find();
        cgm.getLight().select(newLight, spatial);
    }

    /**
     * Delete the light.
     */
    public void delete() {
        if (isSelected() && editableCgm != null) {
            String type = getType();
            String name = selected.getName();
            String description = String.format("delete %s light named %s",
                    type, MyString.quote(name));
            editableCgm.replaceLight(null, description);
            selectNone();
        }
    }

    /**
     * Copy the light's direction, if any.
     *
     * @return a new unit vector, or null if the light is non-directional
     */
    public Vector3f direction() {
        Vector3f result = null;
        if (selected instanceof DirectionalLight) {
            DirectionalLight directional = (DirectionalLight) selected;
            result = directional.getDirection().clone();
        } else if (selected instanceof SpotLight) {
            SpotLight spot = (SpotLight) selected;
            result = spot.getDirection().clone();
        }

        assert result == null || result.isUnitVector();
        return result;
    }

    /**
     * Read the position index of the selected light in the C-G model.
     *
     * @return the index, or noLightIndex if no light is selected
     */
    public int findIndex() {
        int result = noLightIndex;
        if (isSelected()) {
            List<Light> lights = cgm.listLights(Light.class);
            result = lights.indexOf(selected);
            assert result != noLightIndex;
        }

        return result;
    }

    /**
     * Access the light.
     *
     * @return the pre-existing instance, or null if none selected
     */
    Light get() {
        return selected;
    }

    /**
     * Access the light's owner.
     *
     * @return the pre-existing instance, or null if no light is selected
     */
    Spatial getOwner() {
        return owner;
    }

    /**
     * Describe the light's type.
     *
     * @return abbreviated name for its class
     */
    public String getType() {
        String description = MyLight.describeType(selected);
        return description;
    }

    /**
     * Test whether the light is enabled.
     *
     * @return true if enabled, otherwise false
     */
    public boolean isEnabled() {
        boolean result = false;
        if (isSelected()) {
            result = selected.isEnabled();
        }

        return result;
    }

    /**
     * Test whether a light is selected.
     *
     * @return true if selected, otherwise false
     */
    public boolean isSelected() {
        boolean result = false;
        if (selected != null) {
            result = true;
        }

        return result;
    }

    /**
     * Determine the name of the selected light.
     *
     * @return a descriptive name, or noLight if none selected
     */
    public String name() {
        String name = noLight;
        if (isSelected()) {
            name = selected.getName();
        }

        return name;
    }

    /**
     * Read the name of the spatial that owns the light.
     *
     * @return name of the spatial, or "" if no light is selected
     */
    public String ownerName() {
        String result = "";
        if (isSelected()) {
            assert owner != null;
            result = owner.getName();
        }

        return result;
    }

    /**
     * Copy the light's "position" (location), if any.
     *
     * @return a new vector, or null if the light is non-positional
     */
    public Vector3f position() {
        Vector3f result = null;
        if (selected instanceof LightProbe) {
            LightProbe probe = (LightProbe) selected;
            result = probe.getPosition().clone();
        } else if (selected instanceof PointLight) {
            PointLight point = (PointLight) selected;
            result = point.getPosition().clone();
        } else if (selected instanceof SpotLight) {
            SpotLight spot = (SpotLight) selected;
            result = spot.getPosition().clone();
        }

        return result;
    }

    /**
     * After successfully loading a C-G model, deselect any previously selected
     * light.
     */
    void postLoad() {
        owner = null;
        selected = null;
    }

    /**
     * Rename the light.
     *
     * @param newName a new name for the selected light (not null)
     */
    public void rename(String newName) {
        Validate.nonNull(newName, "new name");

        if (isSelected() && editableCgm != null) {
            String oldName = selected.getName();
            Light newLight = selected.clone();
            newLight.setName(newName);
            String type = getType();
            String description = String.format("rename %s light %s to %s",
                    type, MyString.quote(oldName), MyString.quote(newName));
            editableCgm.replaceLight(newLight, description);
            select(newLight);
        }
    }

    /**
     * Reverse the light's direction.
     */
    public void reverseDirection() {
        Vector3f dir = direction();
        dir.negateLocal();
        setDirection(dir);
    }

    /**
     * Select the specified light.
     *
     * @param light which light to select (alias created), or null to deselect
     */
    void select(Light light) {
        if (light == null) {
            selectNone();
        } else {
            Spatial newOwner = cgm.findOwner(light);
            select(light, newOwner);
        }
    }

    /**
     * Select the specified light owned by the specified spatial.
     *
     * @param light light to select (not null, alias created)
     * @param spatial spatial that owns the light (not null, alias created)
     */
    void select(Light light, Spatial spatial) {
        assert light != null;
        assert spatial != null;
        assert MyLight.findIndex(light, spatial) != noLightIndex;

        selected = light;
        owner = spatial;
    }

    /**
     * Select a light by its name.
     *
     * @param name which light to select, or noLight to deselect (not null)
     */
    public void select(String name) {
        Validate.nonNull(name, "name");

        if (name.equals(noLight)) {
            selectNone();
        } else {
            List<String> names = cgm.listLightNames(Light.class);
            int index = names.indexOf(name);
            assert index != -1;
            List<Light> lights = cgm.listLights(Light.class);
            Light light = lights.get(index);
            select(light);
        }
    }

    /**
     * Select the next light (in cyclical index order).
     */
    public void selectNext() {
        if (isSelected()) {
            List<Light> lights = cgm.listLights(Light.class);
            int newIndex = findIndex() + 1;
            int numLights = lights.size();
            if (newIndex >= numLights) {
                newIndex = 0;
            }
            Light light = lights.get(newIndex);
            select(light);
        }
    }

    /**
     * Deselect the selected light, if any.
     */
    public void selectNone() {
        owner = null;
        selected = null;
    }

    /**
     * Select the previous light (in cyclical index order).
     */
    public void selectPrevious() {
        if (isSelected()) {
            List<Light> lights = cgm.listLights(Light.class);
            int newIndex = findIndex() - 1;
            if (newIndex < 0) {
                int numLights = lights.size();
                newIndex = numLights - 1;
            }
            Light light = lights.get(newIndex);
            select(light);
        }
    }

    /**
     * Alter which C-G model contains the light. (Invoked only during
     * initialization and cloning.)
     *
     * @param newCgm (not null, aliases created)
     */
    void setCgm(Cgm newCgm) {
        assert newCgm != null;
        assert newCgm.getLight() == this;

        cgm = newCgm;
        if (newCgm instanceof EditableCgm) {
            editableCgm = (EditableCgm) newCgm;
        } else {
            editableCgm = null;
        }
    }

    /**
     * Alter the color of the light.
     *
     * @param newColor (not null, unaffected)
     */
    public void setColor(ColorRGBA newColor) {
        Validate.nonNull(newColor, "new color");

        if (isSelected() && editableCgm != null) {
            Light newLight = selected.clone();
            newLight.setColor(newColor);
            editableCgm.replaceLightColor(newLight);
            select(newLight);
        }
    }

    /**
     * Alter the direction of the light.
     *
     * @param newDirection world direction (not null, not zero, unaffected)
     * @throws IllegalStateException if the selected light is not directable
     */
    public void setDirection(Vector3f newDirection) {
        Validate.nonZero(newDirection, "new direction");

        if (isSelected() && editableCgm != null) {
            Light newLight = selected.clone();
            Vector3f direction = newDirection.normalize();
            if (newLight instanceof DirectionalLight) {
                DirectionalLight directional = (DirectionalLight) newLight;
                directional.setDirection(direction);
            } else if (newLight instanceof SpotLight) {
                SpotLight spot = (SpotLight) newLight;
                spot.setDirection(direction);
            } else {
                throw new IllegalStateException();
            }
            editableCgm.replaceLightPosDir(newLight);
            select(newLight);
        }
    }

    /**
     * Alter whether the light is enabled.
     *
     * @param newSetting true&rarr;enable, false&rarr;disable
     */
    public void setEnabled(boolean newSetting) {
        if (isSelected() && editableCgm != null && isEnabled() != newSetting) {
            Light newLight = selected.clone();
            newLight.setEnabled(newSetting);
            String verb = newSetting ? "enabled" : "disabled";
            String type = getType();
            String name = selected.getName();
            String description = String.format("%s the %s light named %s",
                    verb, type, MyString.quote(name));
            editableCgm.replaceLight(newLight, description);
            select(newLight);
        }
    }

    /**
     * Alter the "position" (location) of the light.
     *
     * @param newPosition world coordinates (not null, unaffected)
     * @throws IllegalStateException if the light is not positionable
     */
    public void setPosition(Vector3f newPosition) {
        Validate.nonNull(newPosition, "new position");

        if (isSelected() && editableCgm != null) {
            Light newLight = selected.clone();
            if (newLight instanceof LightProbe) {
                LightProbe probe = (LightProbe) newLight;
                probe.setPosition(newPosition);
            } else if (newLight instanceof PointLight) {
                PointLight point = (PointLight) newLight;
                point.setPosition(newPosition);
            } else if (newLight instanceof SpotLight) {
                SpotLight spot = (SpotLight) newLight;
                spot.setPosition(newPosition);
            } else {
                throw new IllegalStateException();
            }
            editableCgm.replaceLightPosDir(newLight);
            select(newLight);
        }
    }

    /**
     * Calculate a transform for the light.
     *
     * @param storeResult (modified if not null)
     * @return world transform (either storeResult or a new instance)
     */
    public Transform transform(Transform storeResult) {
        if (storeResult == null) {
            storeResult = new Transform();
        } else {
            storeResult.loadIdentity();
        }

        Vector3f position = position();
        if (position != null) {
            storeResult.setTranslation(position);
        }
        Vector3f direction = direction();
        if (direction != null) {
            Vector3f axis2 = new Vector3f();
            Vector3f axis3 = new Vector3f();
            MyVector3f.generateBasis(direction, axis2, axis3);
            Quaternion orientation = storeResult.getRotation();
            orientation.fromAxes(direction, axis2, axis3);
        }

        return storeResult;
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
    public SelectedLight clone() throws CloneNotSupportedException {
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
        owner = cloner.clone(owner);
        selected = cloner.clone(selected);
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public SelectedLight jmeClone() {
        try {
            SelectedLight clone = (SelectedLight) super.clone();
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException(exception);
        }
    }
}
