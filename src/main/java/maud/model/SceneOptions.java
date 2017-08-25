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

import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * Display options applicable to "scene" views in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SceneOptions implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            SceneOptions.class.getName());
    // *************************************************************************
    // fields

    /**
     * configuration of coordinate axes visualization(s)
     */
    private AxesOptions axes = new AxesOptions();
    /**
     * true if physics objects are visualized, otherwise false
     */
    private boolean physicsRendered = true;
    /**
     * shadows (true &rarr; rendered, false &rarr; not rendered)
     */
    private boolean shadowsRendered = true;
    /**
     * sky background (true &rarr; rendered, false &rarr; not rendered)
     */
    private boolean skyRendered = true;
    /**
     * configuration of the bounds visualization(s)
     */
    private BoundsOptions bounds = new BoundsOptions();
    /**
     * configuration of the camera(s)
     */
    private CameraStatus camera = new CameraStatus();
    /**
     * configuration of the 3D cursor(s)
     */
    private CursorStatus cursor = new CursorStatus();
    /**
     * diameter of the platform in scene views (in world units, &gt;0)
     */
    private float platformDiameter = 1f;
    /**
     * type of platform in scene views (not null)
     */
    private PlatformType platformType = PlatformType.Square;
    /**
     * configuration of the skeleton visualization(s)
     */
    private SkeletonOptions skeleton = new SkeletonOptions();
    /**
     * configuration of the vertex visualization(s)
     */
    private VertexOptions vertex = new VertexOptions();
    // *************************************************************************
    // new methods exposed

    /**
     * Test whether shadows are rendered in scene views.
     *
     * @return true if rendered, otherwise false
     */
    public boolean areShadowsRendered() {
        return shadowsRendered;
    }

    /**
     * Access the configuration of coordinate axes visualization(s).
     *
     * @return the pre-existing instance
     */
    public AxesOptions getAxes() {
        assert axes != null;
        return axes;
    }

    /**
     * Access the configuration of bounds visualization(s).
     *
     * @return the pre-existing instance
     */
    public BoundsOptions getBounds() {
        assert bounds != null;
        return bounds;
    }

    /**
     * Access the configuration of the camera(s).
     *
     * @return the pre-existing instance
     */
    public CameraStatus getCamera() {
        assert camera != null;
        return camera;
    }

    /**
     * Access the configuration of the 3D cursor(s).
     *
     * @return the pre-existing instance
     */
    public CursorStatus getCursor() {
        assert cursor != null;
        return cursor;
    }

    /**
     * Read the diameter of the platform in scene views.
     *
     * @return diameter (in world units, &gt;0)
     */
    public float getPlatformDiameter() {
        assert platformDiameter > 0f : platformDiameter;
        return platformDiameter;
    }

    /**
     * Read the type of platform included in scene views.
     *
     * @return enum (not null)
     */
    public PlatformType getPlatformType() {
        assert platformType != null;
        return platformType;
    }

    /**
     * Access the configuration of the skeleton visualization(s).
     *
     * @return the pre-existing instance
     */
    public SkeletonOptions getSkeleton() {
        assert skeleton != null;
        return skeleton;
    }

    /**
     * Access the configuration of the vertex visualization(s).
     *
     * @return the pre-existing instance
     */
    public VertexOptions getVertex() {
        assert vertex != null;
        return vertex;
    }

    /**
     * Test whether physics objects are visualized.
     *
     * @return true if visualized, otherwise false
     */
    public boolean isPhysicsRendered() {
        return physicsRendered;
    }

    /**
     * Test whether the sky background is rendered in scene views.
     *
     * @return true if rendered, otherwise false
     */
    public boolean isSkyRendered() {
        return skyRendered;
    }

    /**
     * Alter whether physics objects are visualized.
     *
     * @param newSetting true to visualize, false to hide
     */
    public void setPhysicsRendered(boolean newSetting) {
        physicsRendered = newSetting;
    }

    /**
     * Alter the diameter of the platform in scene views.
     *
     * @param diameter (in world units, &gt;0)
     */
    public void setPlatformDiameter(float diameter) {
        Validate.positive(diameter, "diameter");
        platformDiameter = diameter;
    }

    /**
     * Alter the type of platform included in scene views.
     *
     * @param newType enum
     */
    public void setPlatformType(PlatformType newType) {
        Validate.nonNull(newType, "new type");
        platformType = newType;
    }

    /**
     * Alter the rendering of shadows in scene views.
     *
     * @param newState true &rarr; rendered, false &rarr; not rendered
     */
    public void setShadowsRendered(boolean newState) {
        shadowsRendered = newState;
    }

    /**
     * Alter the rendering of the sky background in scene views.
     *
     * @param newState true &rarr; rendered, false &rarr; not rendered
     */
    public void setSkyRendered(boolean newState) {
        skyRendered = newState;
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
    public SceneOptions clone() throws CloneNotSupportedException {
        SceneOptions clone = (SceneOptions) super.clone();
        axes = axes.clone();
        bounds = bounds.clone();
        camera = camera.clone();
        cursor = cursor.clone();
        skeleton = skeleton.clone();
        vertex = vertex.clone();

        return clone;
    }
}
