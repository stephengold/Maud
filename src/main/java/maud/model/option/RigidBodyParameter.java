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
package maud.model.option;

import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;
import jme3utilities.Validate;

/**
 * Enumerate the readable parameters of rigid bodies. TODO move to Minie library
 *
 * @author Stephen Gold sgold@sonic.net
 */
public enum RigidBodyParameter {
    // *************************************************************************
    // values

    /**
     * angular damping rate
     */
    AngularDamping,
    /**
     * angular factor for the X axis
     */
    AngularFactorX,
    /**
     * angular factor for the Y axis
     */
    AngularFactorY,
    /**
     * angular factor for the Z axis
     */
    AngularFactorZ,
    /**
     * angular-motion sleep threshold
     */
    AngularSleep,
    /**
     * angular rate around the X axis
     */
    AngularVelocityX,
    /**
     * angular rate around the Y axis
     */
    AngularVelocityY,
    /**
     * angular rate around the Z axis
     */
    AngularVelocityZ,
    /**
     * radius of the sphere used for continuous collision detection (CCD)
     */
    CcdRadius,
    /**
     * continuous collision detection (CCD) motion threshold
     */
    CcdThreshold,
    /**
     * friction parameter
     */
    Friction,
    /**
     * X-component of the gravity acceleration vector
     */
    GravityX,
    /**
     * Y-component of the gravity acceleration vector
     */
    GravityY,
    /**
     * Z-component of the gravity acceleration vector
     */
    GravityZ,
    /**
     * linear damping rate
     */
    LinearDamping,
    /**
     * linear factor for the X axis
     */
    LinearFactorX,
    /**
     * linear factor for the Y axis
     */
    LinearFactorY,
    /**
     * linear factor for the Z axis
     */
    LinearFactorZ,
    /**
     * linear-motion sleep threshold
     */
    LinearSleep,
    /**
     * X-component of velocity
     */
    LinearVelocityX,
    /**
     * Y-component of velocity
     */
    LinearVelocityY,
    /**
     * Z-component of velocity
     */
    LinearVelocityZ,
    /**
     * X-component of the center location
     */
    LocationX,
    /**
     * Y-component of the center location
     */
    LocationY,
    /**
     * Z-component of the center location
     */
    LocationZ,
    /**
     * mass of the body (a float, zero indicates a static body)
     */
    Mass,
    /**
     * W-component of orientation
     */
    OrientationW,
    /**
     * X-component of orientation
     */
    OrientationX,
    /**
     * Y-component of orientation
     */
    OrientationY,
    /**
     * Z-component of orientation
     */
    OrientationZ,
    /**
     * restitution (bounciness)
     */
    Restitution;
    // *************************************************************************
    // new methods exposed

    /**
     * Test whether this parameter can be set to the specified value in the
     * specified body.
     *
     * @param rigidBody the body to alter (not null, modified)
     * @param value the new value
     * @return true if settable, otherwise false
     */
    public boolean canSet(PhysicsRigidBody rigidBody, float value) {
        switch (this) {
            case AngularDamping:
            case AngularFactorX:
            case AngularFactorY:
            case AngularFactorZ:
            case AngularSleep:
            case AngularVelocityX:
            case AngularVelocityY:
            case AngularVelocityZ:
            case CcdRadius:
            case CcdThreshold:
            case Friction:
            case GravityX:
            case GravityY:
            case GravityZ:
            case LinearDamping:
            case LinearFactorX:
            case LinearFactorY:
            case LinearFactorZ:
            case LinearSleep:
            case LinearVelocityX:
            case LinearVelocityY:
            case LinearVelocityZ:
            case LocationX:
            case LocationY:
            case LocationZ:
            case Mass:
            case Restitution:
                return true;

            case OrientationW:
            case OrientationX:
            case OrientationY:
            case OrientationZ:
                return false;

            default:
                throw new IllegalArgumentException(toString());
        }
    }

    /**
     * Determine a maximum value for this parameter.
     *
     * @return a maximum value, or Float.MAX_VALUE if there's no maximum
     */
    public float maxValue() {
        switch (this) {
            case AngularFactorX:
            case AngularFactorY:
            case AngularFactorZ:
            case AngularVelocityX:
            case AngularVelocityY:
            case AngularVelocityZ:
            case AngularSleep:
            case CcdRadius:
            case CcdThreshold:
            case Friction:
            case GravityX:
            case GravityY:
            case GravityZ:
            case LinearFactorX:
            case LinearFactorY:
            case LinearFactorZ:
            case LinearSleep:
            case LinearVelocityX:
            case LinearVelocityY:
            case LinearVelocityZ:
            case LocationX:
            case LocationY:
            case LocationZ:
            case Mass:
            case Restitution:
                return Float.MAX_VALUE;

            case AngularDamping:
            case LinearDamping:
            case OrientationW:
            case OrientationX:
            case OrientationY:
            case OrientationZ:
                return 1f;

            default:
                throw new IllegalArgumentException(toString());
        }
    }

    /**
     * Determine a minimum value for this parameter.
     *
     * @return a minimum value, or -Float.MAX_VALUE if there's no minimum
     */
    public float minValue() {
        switch (this) {
            case AngularDamping:
            case AngularFactorX:
            case AngularFactorY:
            case AngularFactorZ:
            case AngularSleep:
            case CcdRadius:
            case CcdThreshold:
            case Friction:
            case LinearDamping:
            case LinearFactorX:
            case LinearFactorY:
            case LinearFactorZ:
            case LinearSleep:
            case Mass:
            case Restitution:
                return 0f;

            case AngularVelocityX:
            case AngularVelocityY:
            case AngularVelocityZ:
            case GravityX:
            case GravityY:
            case GravityZ:
            case LinearVelocityX:
            case LinearVelocityY:
            case LinearVelocityZ:
            case LocationX:
            case LocationY:
            case LocationZ:
                return -Float.MAX_VALUE;

            case OrientationW:
            case OrientationX:
            case OrientationY:
            case OrientationZ:
                return -1f;

            default:
                throw new IllegalArgumentException(toString());
        }
    }

    /**
     * Read the parameter in the specified rigid body.
     *
     * @param rigidBody the body to read (not null, unaffected)
     * @return the parameter value
     */
    public float read(PhysicsRigidBody rigidBody) {
        Validate.nonNull(rigidBody, "rigid body");
        float result;
        switch (this) {
            case AngularDamping:
                result = rigidBody.getAngularDamping();
                break;
            case AngularFactorX:
                result = rigidBody.getAngularFactor(null).x;
                break;
            case AngularFactorY:
                result = rigidBody.getAngularFactor(null).y;
                break;
            case AngularFactorZ:
                result = rigidBody.getAngularFactor(null).z;
                break;
            case AngularSleep:
                result = rigidBody.getAngularSleepingThreshold();
                break;
            case AngularVelocityX:
                result = rigidBody.getAngularVelocity(null).x;
                break;
            case AngularVelocityY:
                result = rigidBody.getAngularVelocity(null).y;
                break;
            case AngularVelocityZ:
                result = rigidBody.getAngularVelocity(null).z;
                break;
            case CcdRadius:
                result = rigidBody.getCcdSweptSphereRadius();
                break;
            case CcdThreshold:
                result = rigidBody.getCcdMotionThreshold();
                break;
            case Friction:
                result = rigidBody.getFriction();
                break;
            case GravityX:
                result = rigidBody.getGravity(null).x;
                break;
            case GravityY:
                result = rigidBody.getGravity(null).y;
                break;
            case GravityZ:
                result = rigidBody.getGravity(null).z;
                break;
            case LinearDamping:
                result = rigidBody.getLinearDamping();
                break;
            case LinearFactorX:
                result = rigidBody.getLinearFactor(null).x;
                break;
            case LinearFactorY:
                result = rigidBody.getLinearFactor(null).y;
                break;
            case LinearFactorZ:
                result = rigidBody.getLinearFactor(null).z;
                break;
            case LinearSleep:
                result = rigidBody.getLinearSleepingThreshold();
                break;
            case LinearVelocityX:
                result = rigidBody.getLinearVelocity(null).x;
                break;
            case LinearVelocityY:
                result = rigidBody.getLinearVelocity(null).y;
                break;
            case LinearVelocityZ:
                result = rigidBody.getLinearVelocity(null).z;
                break;
            case LocationX:
                result = rigidBody.getPhysicsLocation(null).x;
                break;
            case LocationY:
                result = rigidBody.getPhysicsLocation(null).y;
                break;
            case LocationZ:
                result = rigidBody.getPhysicsLocation(null).z;
                break;
            case Mass:
                result = rigidBody.getMass();
                break;
            case OrientationW:
                result = rigidBody.getPhysicsRotation(null).getW();
                break;
            case OrientationX:
                result = rigidBody.getPhysicsRotation(null).getX();
                break;
            case OrientationY:
                result = rigidBody.getPhysicsRotation(null).getY();
                break;
            case OrientationZ:
                result = rigidBody.getPhysicsRotation(null).getZ();
                break;
            case Restitution:
                result = rigidBody.getRestitution();
                break;
            default:
                throw new IllegalArgumentException(toString());
        }

        return result;
    }

    /**
     * Alter the parameter in the specified rigid body.
     *
     * @param rigidBody the body to alter (not null, modified)
     * @param newValue the new parameter value
     */
    public void set(PhysicsRigidBody rigidBody, float newValue) {
        Validate.nonNull(rigidBody, "rigid body");
        assert canSet(rigidBody, newValue);

        Vector3f vector;
        switch (this) {
            case AngularDamping:
                rigidBody.setAngularDamping(newValue);
                break;

            case AngularFactorX:
                vector = rigidBody.getAngularFactor(null);
                vector.x = newValue;
                rigidBody.setAngularFactor(vector);
                break;

            case AngularFactorY:
                vector = rigidBody.getAngularFactor(null);
                vector.y = newValue;
                rigidBody.setAngularFactor(vector);
                break;

            case AngularFactorZ:
                vector = rigidBody.getAngularFactor(null);
                vector.z = newValue;
                rigidBody.setAngularFactor(vector);
                break;

            case AngularSleep:
                rigidBody.setAngularSleepingThreshold(newValue);
                break;

            case AngularVelocityX:
                vector = rigidBody.getAngularVelocity(null);
                vector.x = newValue;
                rigidBody.setAngularVelocity(vector);
                break;

            case AngularVelocityY:
                vector = rigidBody.getAngularVelocity(null);
                vector.y = newValue;
                rigidBody.setAngularVelocity(vector);
                break;

            case AngularVelocityZ:
                vector = rigidBody.getAngularVelocity(null);
                vector.z = newValue;
                rigidBody.setAngularVelocity(vector);
                break;

            case CcdRadius:
                rigidBody.setCcdSweptSphereRadius(newValue);
                break;

            case CcdThreshold:
                rigidBody.setCcdMotionThreshold(newValue);
                break;

            case Friction:
                rigidBody.setFriction(newValue);
                break;

            case GravityX:
                vector = rigidBody.getGravity(null);
                vector.x = newValue;
                rigidBody.setGravity(vector);
                break;

            case GravityY:
                vector = rigidBody.getGravity(null);
                vector.y = newValue;
                rigidBody.setGravity(vector);
                break;

            case GravityZ:
                vector = rigidBody.getGravity(null);
                vector.z = newValue;
                rigidBody.setGravity(vector);
                break;

            case LinearDamping:
                rigidBody.setLinearDamping(newValue);
                break;

            case LinearFactorX:
                vector = rigidBody.getLinearFactor(null);
                vector.x = newValue;
                rigidBody.setLinearFactor(vector);
                break;

            case LinearFactorY:
                vector = rigidBody.getLinearFactor(null);
                vector.y = newValue;
                rigidBody.setLinearFactor(vector);
                break;

            case LinearFactorZ:
                vector = rigidBody.getLinearFactor(null);
                vector.z = newValue;
                rigidBody.setLinearFactor(vector);
                break;

            case LinearSleep:
                rigidBody.setLinearSleepingThreshold(newValue);
                break;

            case LinearVelocityX:
                vector = rigidBody.getLinearVelocity(null);
                vector.x = newValue;
                rigidBody.setLinearVelocity(vector);
                break;

            case LinearVelocityY:
                vector = rigidBody.getLinearVelocity(null);
                vector.y = newValue;
                rigidBody.setLinearVelocity(vector);
                break;

            case LinearVelocityZ:
                vector = rigidBody.getLinearVelocity(null);
                vector.z = newValue;
                rigidBody.setLinearVelocity(vector);
                break;

            case LocationX:
                vector = rigidBody.getPhysicsLocation(null);
                vector.x = newValue;
                rigidBody.setPhysicsLocation(vector);
                break;

            case LocationY:
                vector = rigidBody.getPhysicsLocation(null);
                vector.y = newValue;
                rigidBody.setPhysicsLocation(vector);
                break;

            case LocationZ:
                vector = rigidBody.getPhysicsLocation(null);
                vector.z = newValue;
                rigidBody.setPhysicsLocation(vector);
                break;

            case Mass:
                rigidBody.setMass(newValue);
                break;

            case Restitution:
                rigidBody.setRestitution(newValue);
                break;

            default:
                throw new IllegalArgumentException(toString());
        }
    }
}
