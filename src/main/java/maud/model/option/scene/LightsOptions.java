/*
 Copyright (c) 2018-2019, Stephen Gold
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
package maud.model.option.scene;

import com.jme3.math.Vector3f;
import java.io.IOException;
import java.io.Writer;
import java.util.logging.Logger;
import jme3utilities.Validate;
import maud.MaudUtil;
import maud.action.ActionPrefix;

/**
 * Options for lighting scene views with no sky simulation.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class LightsOptions implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(LightsOptions.class.getName());
    // *************************************************************************
    // fields

    /**
     * level of ambient light (&ge;0)
     */
    private float ambientLevel = 0.5f;
    /**
     * level of main (directional) light (&ge;0)
     */
    private float mainLevel = 1.1f;
    /**
     * direction of main (directional) light (unit vector)
     */
    private Vector3f direction
            = new Vector3f(0.7525587f, -0.6055314f, -0.25881884f);
    // *************************************************************************
    // new methods exposed

    /**
     * Copy the direction of the main (directional) light.
     *
     * @param storeResult (modified if not null)
     * @return a unit vector in world coordinates (either storeResult or a new
     * instance)
     */
    public Vector3f direction(Vector3f storeResult) {
        assert direction.isUnitVector();
        if (storeResult == null) {
            return direction.clone();
        } else {
            return storeResult.set(direction);
        }
    }

    /**
     * Read the level of the ambient light.
     *
     * @return level (&ge;0)
     */
    public float getAmbientLevel() {
        assert ambientLevel >= 0f : ambientLevel;
        return ambientLevel;
    }

    /**
     * Read the level of the main (directional) light.
     *
     * @return level (&ge;0)
     */
    public float getMainLevel() {
        assert mainLevel >= 0f : mainLevel;
        return mainLevel;
    }

    /**
     * Alter the level of the ambient light.
     *
     * @param newLevel (&ge;0)
     */
    public void setAmbientLevel(float newLevel) {
        Validate.nonNegative(newLevel, "new level");
        ambientLevel = newLevel;
    }

    /**
     * Alter the direction of the main (directional) light.
     *
     * @param newDirection (not null, not zero, unaffected)
     */
    public void setDirection(Vector3f newDirection) {
        Validate.nonZero(newDirection, "new direction");

        direction.set(newDirection);
        direction.normalizeLocal();
    }

    /**
     * Alter the level of the main (directional) light.
     *
     * @param newLevel (&ge;0)
     */
    public void setMainLevel(float newLevel) {
        Validate.nonNegative(newLevel, "new level");
        mainLevel = newLevel;
    }

    /**
     * Write the options to a script using the specified writer.
     *
     * @param writer (not null)
     * @throws java.io.IOException if an I/O error occurs while writing
     */
    void writeToScript(Writer writer) throws IOException {
        assert writer != null;

        String action = ActionPrefix.setAmbientLevel
                + Float.toString(ambientLevel);
        MaudUtil.writePerformAction(writer, action);

        action = ActionPrefix.setMainLevel + Float.toString(mainLevel);
        MaudUtil.writePerformAction(writer, action);

        action = ActionPrefix.setMainDirection + direction.toString();
        MaudUtil.writePerformAction(writer, action);
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
    public LightsOptions clone() throws CloneNotSupportedException {
        LightsOptions clone = (LightsOptions) super.clone();
        direction = direction.clone();

        return clone;
    }
}
