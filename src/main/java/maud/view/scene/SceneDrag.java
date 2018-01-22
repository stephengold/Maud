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
package maud.view.scene;

import com.jme3.math.FastMath;
import com.jme3.math.Line;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.logging.Logger;
import jme3utilities.MySpatial;
import jme3utilities.Validate;
import jme3utilities.debug.AxesVisualizer;
import jme3utilities.math.MyVector3f;
import jme3utilities.wes.Pose;
import maud.Maud;
import maud.model.EditableMap;
import maud.model.EditorModel;
import maud.model.cgm.Cgm;
import maud.model.cgm.EditableCgm;
import maud.model.cgm.SelectedBone;
import maud.model.cgm.SelectedLight;
import maud.model.cgm.SelectedObject;
import maud.model.cgm.SelectedSkeleton;
import maud.model.option.scene.AxesDragEffect;
import maud.model.option.scene.AxesOptions;
import maud.model.option.scene.AxesSubject;

/**
 * Axis drag state for scene views. Never checkpointed.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SceneDrag {
    // *************************************************************************
    // constants and loggers

    /**
     * dummy axis index used to indicate that no axis is being dragged
     */
    final private static int noAxis = -1;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SceneDrag.class.getName());
    // *************************************************************************
    // fields

    /**
     * which direction the dragged axis is pointing (true &rarr; away from
     * camera, false &rarr; toward camera)
     */
    private static boolean dragFarSide;
    /**
     * which C-G model is being manipulated (true &rarr; source, false &rarr;
     * target)
     */
    private static boolean dragSourceCgm;
    /**
     * length of the axis arrow at previous update (in local units, &gt;0)
     */
    private static float previousLength;
    /**
     * index of the axis being dragged (&ge;0, &lt;numAxes) or noAxis for none
     */
    private static int dragAxisIndex = noAxis;
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private SceneDrag() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Terminate axis dragging.
     */
    public static void clear() {
        dragAxisIndex = noAxis;
        assert !isActive();
    }

    /**
     * Access the C-G model that's being manipulated.
     *
     * @return the pre-existing instance (not null)
     */
    public static Cgm getCgm() {
        assert isActive();

        EditorModel model = Maud.getModel();
        Cgm result;
        if (dragSourceCgm) {
            result = model.getSource();
        } else {
            result = model.getTarget();
        }

        return result;
    }

    /**
     * Test whether axis dragging is active.
     *
     * @return true if selected, otherwise false
     */
    public static boolean isActive() {
        if (dragAxisIndex == noAxis) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Start dragging the specified axis.
     *
     * @param axisIndex which axis to drag: 0&rarr;X, 1&rarr;Y, 2&rarr;Z
     * @param initialLength length of axis in local units (&gt;0)
     * @param cgm which C-G model (not null)
     * @param farSideFlag true &rarr; drag on the far side of the axis origin,
     * false to drag on near side
     */
    static public void start(int axisIndex, float initialLength, Cgm cgm,
            boolean farSideFlag) {
        Validate.inRange(axisIndex, "axis index", MyVector3f.firstAxis,
                MyVector3f.lastAxis);
        Validate.positive(initialLength, "initial length");
        Validate.nonNull(cgm, "model");

        dragAxisIndex = axisIndex;
        previousLength = initialLength;
        dragFarSide = farSideFlag;
        if (cgm == Maud.getModel().getSource()) {
            dragSourceCgm = true;
        } else {
            dragSourceCgm = false;
        }
        assert isActive();
    }

    /**
     * Toggle which direction the dragged axis is pointing.
     */
    public static void toggleSide() {
        dragFarSide = !dragFarSide;
    }

    /**
     * While dragging an axis, update the subject in the MVC model.
     *
     * @param visualizer (not null, unaffected)
     * @param worldLine (not null, unaffected)
     */
    static void updateSubject(AxesVisualizer visualizer, Line worldLine) {
        /*
         * Calculate the old axis direction in world coordinates.
         */
        Spatial spatial = visualizer.getSpatial();
        assert !MySpatial.isIgnoringTransforms(spatial);
        Vector3f axesOrigin = spatial.getWorldTranslation();
        Vector3f oldTipWorld = visualizer.tipLocation(dragAxisIndex);
        Vector3f oldDirWorld = oldTipWorld.subtract(axesOrigin);
        float oldLengthWorld = oldDirWorld.length();

        Line axisLine;
        Vector3f newTipWorld;
        AxesOptions options = Maud.getModel().getScene().getAxes();
        AxesDragEffect effect = options.getDragEffect();
        switch (effect) {
            case None:
                break;

            case Rotate:
                /*
                 * Calculate the new axis direction in world,
                 * then local coordinates.
                 */
                newTipWorld = MyVector3f.lineMeetsSphere(worldLine, axesOrigin,
                        oldLengthWorld, dragFarSide);
                Vector3f newDirWorld = newTipWorld.subtract(axesOrigin);
                Vector3f newDirLocal = MyVector3f.localizeDirection(newDirWorld,
                        spatial, null);

                Vector3f oldDirLocal = MyVector3f.localizeDirection(oldDirWorld,
                        spatial, null);
                Vector3f cross = oldDirLocal.cross(newDirLocal);
                if (!MyVector3f.isZero(cross)) {
                    rotate(cross, newDirWorld);
                }
                break;

            case ScaleAll:
            case ScaleAxis:
                /*
                 * Calculate the new axis length in local units.
                 */
                axisLine = new Line(axesOrigin, oldDirWorld);
                newTipWorld = MyVector3f.lineMeetsLine(axisLine, worldLine);
                if (newTipWorld != null) {
                    assert !MySpatial.isIgnoringTransforms(spatial);
                    Vector3f newTipLocal
                            = spatial.worldToLocal(newTipWorld, null);
                    float newLengthLocal = newTipLocal.length();

                    float scaleFactor
                            = FastMath.abs(newLengthLocal / previousLength);
                    if (scaleFactor > 0f) {
                        if (effect == AxesDragEffect.ScaleAll) {
                            scaleAll(scaleFactor);
                        } else {
                            scaleAxis(scaleFactor);
                        }
                        previousLength = newLengthLocal;
                    }
                }
                break;

            case Translate:
                /*
                 * Calculate the axis displacement in world coordinates.
                 */
                axisLine = new Line(axesOrigin, oldDirWorld);
                newTipWorld = MyVector3f.lineMeetsLine(axisLine, worldLine);
                if (newTipWorld != null) {
                    Vector3f displacement = newTipWorld.subtract(oldTipWorld);
                    translate(displacement);
                }
                break;

            default:
                throw new IllegalStateException();
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Access the editable C-G model that's being manipulated.
     *
     * @return the pre-existing instance (not null)
     */
    private static EditableCgm getEditableCgm() {
        assert isActive();

        EditableCgm result = null;
        if (!dragSourceCgm) {
            result = Maud.getModel().getTarget();
        }

        return result;
    }

    /**
     * Rotate the visualized bone using the specified quaternion.
     *
     * @param rotation quaternion (not null, norm=1)
     */
    private static void rotateBone(Quaternion rotation) {
        Cgm cgm = getCgm();
        SelectedBone bone = cgm.getBone();
        int boneIndex = bone.getIndex();
        assert boneIndex != -1;
        EditorModel model = Maud.getModel();
        EditableMap map = model.getMap();
        EditableCgm target = model.getTarget();
        Pose pose = cgm.getPose().get();
        Quaternion oldUserRotation = pose.userRotation(boneIndex, null);

        Quaternion newUserRotation = null;
        if (bone.shouldEnableControls()) {
            /*
             * Apply the rotation to the selected bone in the displayed pose.
             */
            newUserRotation = oldUserRotation.mult(rotation);
            newUserRotation.normalizeLocal();
            pose.setRotation(boneIndex, newUserRotation);

        } else if (cgm == target
                && cgm.getAnimation().isRetargetedPose()
                && map.isBoneMappingSelected()) {
            /*
             * Apply the rotation to the target bone in the displayed pose.
             */
            newUserRotation = oldUserRotation.mult(rotation);
            pose.setRotation(boneIndex, newUserRotation);
        }

        if (newUserRotation != null && !bone.shouldEnableControls()) {
            assert target.getAnimation().isRetargetedPose();
            assert map.isBoneMappingSelected();
            /*
             * Infer a new effective twist for the selected bone mapping.
             */
            Quaternion sourceMo
                    = model.getSource().getBone().modelOrientation(null);
            Quaternion targetMo
                    = target.getBone().modelOrientation(null);
            Quaternion invSourceMo = sourceMo.inverse();
            Quaternion newEffectiveTwist = invSourceMo.mult(targetMo);
            map.setTwist(newEffectiveTwist);
        }
    }

    /**
     * Rotate the subject using the specified cross product.
     *
     * @param cross cross product of two unit vectors in local coordinates (not
     * null, length&gt;0, length&le;1)
     * @param newDirWorld new axis direction in world coordinates (not null,
     * length&gt;0)
     */
    private static void rotate(Vector3f cross, Vector3f newDirWorld) {
        /*
         * Convert the cross product to a rotation quaternion.
         */
        double lengthSquared = MyVector3f.lengthSquared(cross);
        double dLength = Math.sqrt(lengthSquared);
        float fLength = (float) dLength;
        assert fLength > 0f : fLength;
        assert fLength <= 1f : fLength;
        Vector3f rotationAxis = cross.divide(fLength);
        float rotationAngle = (float) Math.asin(dLength);
        Quaternion rotation = new Quaternion();
        rotation.fromAngleNormalAxis(rotationAngle, rotationAxis);

        EditableCgm editableCgm = getEditableCgm();
        /*
         * Determine which MVC-model object the axes control is visualizing,
         * and rotate that object.
         */
        AxesSubject subject = Maud.getModel().getScene().getAxes().getSubject();
        switch (subject) {
            case Model:
                /*
                 * Apply the Y-axis rotation to the world transform.
                 */
                Cgm cgm = getCgm();
                CgmTransform cgmTransform = cgm.getSceneView().getTransform();
                float angle = FastMath.asin(cross.y * fLength);
                cgmTransform.rotateY(angle);
                break;

            case SelectedBone:
                rotateBone(rotation);
                break;

            case SelectedLight:
                if (dragAxisIndex == MyVector3f.xAxis) {
                    SelectedLight light = editableCgm.getLight();
                    if (light.canDirect()) {
                        /*
                         * Adjust the direction of the selected light.
                         */
                        light.setDirection(newDirWorld);
                    }
                }
                break;

            case SelectedObject:
                if (editableCgm != null) {
                    SelectedObject object = editableCgm.getObject();
                    if (object.canPosition()) {
                        /*
                         * Rotate the selected physics object.
                         */
                        Quaternion orientation = object.orientation(null);
                        orientation.multLocal(rotation);
                        orientation.normalizeLocal();
                        editableCgm.setPhysicsOrientation(orientation);
                    }
                }
                break;

            case SelectedShape:
                break;

            case SelectedSpatial:
                if (editableCgm != null) {
                    /*
                     * Rotate the selected spatial.
                     */
                    Quaternion localRotation
                            = editableCgm.getSpatial().localRotation(null);
                    localRotation.multLocal(rotation);
                    localRotation.normalizeLocal();
                    editableCgm.setSpatialRotation(localRotation);
                }
                break;

            case World: // ignore attempts to drag the world axes
                break;

            default:
                throw new IllegalStateException();
        }
    }

    /**
     * Scale/resize the subject uniformly on all 3 axes.
     *
     * @param factor scale factor (&gt;0, 1 &rarr; no effect)
     */
    private static void scaleAll(float factor) {
        assert factor > 0f : factor;

        Cgm cgm = getCgm();
        EditableCgm editableCgm = getEditableCgm();
        /*
         * Determine which MVC-model object the control is visualizing,
         * and scale that object.
         */
        AxesSubject subject = Maud.getModel().getScene().getAxes().getSubject();
        switch (subject) {
            case Model:
                /*
                 * Scale the world transform.
                 */
                CgmTransform cgmTransform = cgm.getSceneView().getTransform();
                cgmTransform.scale(factor);
                break;

            case SelectedBone:
                if (cgm.getBone().shouldEnableControls()) {
                    /*
                     * Scale the selected bone in the displayed pose.
                     */
                    Pose pose = cgm.getPose().get();
                    int boneIndex = cgm.getBone().getIndex();
                    Vector3f userScale = pose.userScale(boneIndex, null);
                    userScale.multLocal(factor);
                    pose.setScale(boneIndex, userScale);
                }
                break;

            case SelectedLight: // ignore attempts to scale lights
                break;

            case SelectedObject:
                /*
                 * Ignore attempts to scale the physics object directly
                 * -- the user should scale its geometry or resize its
                 * shape instead.
                 */
                break;

            case SelectedShape:
                if (editableCgm != null) {
                    Vector3f scale = new Vector3f(factor, factor, factor);
                    editableCgm.resizeShape(scale);
                }
                break;

            case SelectedSpatial:
                if (editableCgm != null) {
                    /*
                     * Scale the selected spatial.
                     */
                    Vector3f localScale = cgm.getSpatial().localScale(null);
                    localScale.multLocal(factor);
                    editableCgm.setSpatialScale(localScale);
                }
                break;

            case World: // ignore attempts to drag the world axes
                break;

            default:
                throw new IllegalStateException();
        }
    }

    /**
     * Scale/resize the subject on the dragged axis only.
     *
     * @param factor scale factor (&gt;0, 1 &rarr; no effect)
     */
    private static void scaleAxis(float factor) {
        assert factor > 0f : factor;

        Vector3f scale = MyVector3f.axisVector(dragAxisIndex, factor, null);
        scale.multLocal(factor - 1f);
        scale.addLocal(1f, 1f, 1f);

        Cgm cgm = getCgm();
        EditableCgm editableCgm = getEditableCgm();
        /*
         * Determine which MVC-model object the control is visualizing,
         * and scale that object.
         */
        AxesSubject subject = Maud.getModel().getScene().getAxes().getSubject();
        switch (subject) {
            case Model: // ignore attempts to scale a model axis
            case SelectedBone: // ignore attempts to scale bones
            case SelectedLight: // ignore attempts to scale lights
            case SelectedObject: // ignore attempts to scale objects
                break;

            case SelectedShape: // won't work on all shapes
                if (editableCgm != null) {
                    editableCgm.resizeShape(scale);
                }
                break;

            case SelectedSpatial:
                if (editableCgm != null) {
                    /*
                     * Scale the selected spatial.
                     */
                    Vector3f localScale = cgm.getSpatial().localScale(null);
                    localScale.multLocal(scale);
                    editableCgm.setSpatialScale(localScale);
                }
                break;

            case World: // ignore attempts to drag the world axes
                break;

            default:
                throw new IllegalStateException();
        }
    }

    /**
     * Translate the subject by the specified offset.
     *
     * @param offset (in world coordinates, not null, unaffected)
     */
    private static void translate(Vector3f offset) {
        EditableCgm editableCgm = getEditableCgm();
        Cgm cgm = getCgm();
        /*
         * Determine which MVC-model object the control is visualizing,
         * and translate that object.
         */
        AxesSubject subject = Maud.getModel().getScene().getAxes().getSubject();
        switch (subject) {
            case Model: // ignore attempts to translate the model root
                break;

            case SelectedBone:
                if (cgm.getBone().shouldEnableControls()) {
                    /*
                     * Factor out the animated geometry's rotation and
                     * scaling in this view.
                     */
                    Spatial spatial = cgm.getSceneView().selectedSpatial();
                    Geometry ag = MySpatial.findAnimatedGeometry(spatial);
                    Transform transform;
                    if (ag.isIgnoreTransform()) {
                        transform = new Transform(); // identity
                    } else {
                        transform = ag.getWorldTransform().clone();
                        transform.getTranslation().zero();
                    }
                    Vector3f meshOffset
                            = transform.transformInverseVector(offset, null);
                    /*
                     * Factor out the parent bone's rotation and scaling
                     * in the displayed pose.
                     */
                    SelectedBone bone = cgm.getBone();
                    int parentIndex = bone.parentIndex();
                    Pose pose = cgm.getPose().get();
                    Vector3f userOffset;
                    if (parentIndex == SelectedSkeleton.noBoneIndex) {
                        userOffset = meshOffset;
                    } else {
                        pose.modelTransform(parentIndex, transform);
                        transform.getTranslation().zero();
                        userOffset = transform.transformInverseVector(
                                meshOffset, null);
                    }
                    /*
                     * Translate the selected bone in the displayed pose.
                     */
                    int boneIndex = bone.getIndex();
                    Vector3f userTranslation
                            = pose.userTranslation(boneIndex, null);
                    userTranslation.addLocal(userOffset);
                    pose.setTranslation(boneIndex, userTranslation);
                }
                break;

            case SelectedLight:
                SelectedLight light = editableCgm.getLight();
                if (light.canPosition()) {
                    /*
                     * Translate the selected light.
                     */
                    Vector3f position = light.position();
                    position.addLocal(offset);
                    light.setPosition(position);
                }
                break;

            case SelectedObject:
                SelectedObject object = editableCgm.getObject();
                if (object.canPosition()) {
                    /*
                     * Translate the selected physics object.
                     */
                    Vector3f location = object.location(null);
                    location.addLocal(offset);
                    editableCgm.setPhysicsLocation(location);
                }
                break;

            case SelectedShape: // TODO
                break;

            case SelectedSpatial:
                if (editableCgm != null) {
                    /*
                     * Factor out parental rotation and scaling in this view.
                     */
                    Spatial spatial = cgm.getSceneView().selectedSpatial();
                    Node parent = spatial.getParent();
                    Transform transform = parent.getWorldTransform().clone();
                    transform.getTranslation().zero();
                    Vector3f localOffset
                            = transform.transformInverseVector(offset, null);
                    /*
                     * Translate the selected spatial.
                     */
                    Vector3f localTranslation
                            = editableCgm.getSpatial().localTranslation(null);
                    localTranslation.addLocal(localOffset);
                    editableCgm.setSpatialTranslation(localTranslation);
                }
                break;

            case World: // ignore attempts to drag the world axes
                break;

            default:
                throw new IllegalStateException();
        }
    }
}
