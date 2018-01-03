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
package maud;

import com.jme3.light.Light;
import com.jme3.light.LightList;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.Validate;

/**
 * Light utility methods. All methods should be static.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class LightUtil {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(LightUtil.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private LightUtil() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Count all lights of the specified type in the specified subtree of a
     * scene graph. Note: recursive!
     *
     * @param <T> superclass of Light
     * @param subtree subtree to traverse (may be null, unaffected)
     * @param lightType superclass of Light to search for
     * @return number of lights controls found (&ge;0)
     */
    public static <T extends Light> int countLights(Spatial subtree,
            Class<T> lightType) {
        int result = 0;

        if (subtree != null) {
            LightList lights = subtree.getLocalLightList();
            int numLights = lights.size();
            for (int lightI = 0; lightI < numLights; lightI++) {
                Light light = lights.get(lightI);
                if (lightType.isAssignableFrom(light.getClass())) {
                    ++result;
                }
            }
        }

        if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                result += countLights(child, lightType);
            }
        }

        assert result >= 0 : result;
        return result;
    }

    /**
     * Describe the type of a light.
     *
     * @param light instance to describe (not null, unaffected)
     * @return description (not null)
     */
    public static String describeType(Light light) {
        String description = light.getClass().getSimpleName();
        if (description.endsWith("Light")) {
            description = MyString.removeSuffix(description, "Light");
        }

        return description;
    }

    /**
     * Find the index of the specified light in the specified spatial.
     *
     * @param light light to find (not null, unaffected)
     * @param owner where the light was added (not null, unaffected)
     * @return index (&ge;0) or -1 if not found
     */
    public static int findIndex(Light light, Spatial owner) {
        Validate.nonNull(light, "light");

        int result = -1;
        LightList lights = owner.getLocalLightList();
        int numLights = lights.size();
        for (int index = 0; index < numLights; index++) {
            Light indexedLight = lights.get(index);
            if (indexedLight == light) {
                result = index;
            }
        }

        return result;
    }

    /**
     * Find the 1st instance of a light with the specified name in the specified
     * subtree. Note: recursive!
     *
     * @param lightName light name to find (not null, unaffected)
     * @param subtree subtree to traverse (may be null, unaffected)
     * @return a pre-existing instance, or null if none found
     */
    public static Light findLight(String lightName, Spatial subtree) {
        Validate.nonNull(lightName, "light name");

        Light light = MySpatial.findLight(subtree, lightName);
        if (light != null) {
            return light;

        } else if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                light = findLight(lightName, child);
                if (light != null) {
                    return light;
                }
            }
        }

        return null;
    }

    /**
     * Find the spatial that owns the specified light in the specified subtree
     * of the scene graph. Note: recursive!
     *
     * @param light which light to search for (not null, unaffected)
     * @param subtree which subtree to search (not null, unaffected)
     * @return the pre-existing spatial, or null if none found
     */
    public static Spatial findOwner(Light light, Spatial subtree) {
        Validate.nonNull(light, "light");
        Validate.nonNull(subtree, "subtree");

        Spatial result = null;
        int lightIndex = findIndex(light, subtree);
        if (lightIndex != -1) {
            result = subtree;
        } else if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                result = findOwner(light, child);
                if (result != null) {
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Enumerate all lights of the specified type in the specified subtree of a
     * scene graph. Note: recursive!
     *
     * @param <T> superclass of Light
     * @param subtree (not null)
     * @param lightType superclass of Light to search for
     * @param storeResult (added to if not null)
     * @return an expanded list (either storeResult or a new instance)
     */
    @SuppressWarnings("unchecked")
    public static <T extends Light> List<T> listLights(Spatial subtree,
            Class<T> lightType, List<T> storeResult) {
        Validate.nonNull(subtree, "subtree");
        if (storeResult == null) {
            storeResult = new ArrayList<>(4);
        }

        LightList lights = subtree.getLocalLightList();
        int numLights = lights.size();
        for (int lightIndex = 0; lightIndex < numLights; lightIndex++) {
            T light = (T) lights.get(lightIndex);
            if (lightType.isAssignableFrom(light.getClass())
                    && !storeResult.contains(light)) {
                storeResult.add(light);
            }
        }

        if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                listLights(child, lightType, storeResult);
            }
        }

        return storeResult;
    }
}
