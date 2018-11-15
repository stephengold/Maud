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
package maud.model.option;

import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;
import jme3utilities.Validate;

/**
 * Enumerate some parameters of rigid bodies.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public enum RigidBodyParameter {
    // *************************************************************************
    // values

    /**
     *
     */
    AngularDamping,
    /**
     *
     */
    AngularSleep,
    /**
     *
     */
    Friction,
    /**
     * X-component of the gravity acceleration vector (a float)
     */
    GravityX,
    /**
     * Y-component of the gravity acceleration vector (a float)
     */
    GravityY,
    /**
     * Z-component of the gravity acceleration vector (a float)
     */
    GravityZ,
    /**
     *
     */
    LinearDamping,
    /**
     *
     */
    LinearSleep,
    /**
     * mass of the body (a float, zero indicates a static body)
     */
    Mass,
    /**
     *
     */
    Restitution;
    // *************************************************************************
    // new methods exposed

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
            case AngularSleep:
                result = rigidBody.getAngularSleepingThreshold();
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
            case LinearSleep:
                result = rigidBody.getLinearSleepingThreshold();
                break;
            case Mass:
                result = rigidBody.getMass();
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
        Vector3f vector;

        switch (this) {
            case AngularDamping:
                rigidBody.setAngularDamping(newValue);
                break;

            case AngularSleep:
                rigidBody.setAngularSleepingThreshold(newValue);
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

            case LinearSleep:
                rigidBody.setLinearSleepingThreshold(newValue);
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
