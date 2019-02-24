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
package maud;

/**
 * Enumerate the types of collision shapes that SelectedSpatial.makeShape()
 * knows how to make.
 */
public enum ShapeType {
    /**
     * BoxCollisionShape
     */
    Box,
    /**
     * CapsuleCollisionShape, height on longest axis
     */
    Capsule,
    /**
     * ConeCollisionShape, height on X axis
     */
    ConeX,
    /**
     * ConeCollisionShape, height on Y axis
     */
    ConeY,
    /**
     * ConeCollisionShape, height on Z axis
     */
    ConeZ,
    /**
     * CylinderCollisionShape, height on X axis
     */
    CylinderX,
    /**
     * CylinderCollisionShape, height on Y axis
     */
    CylinderY,
    /**
     * CylinderCollisionShape, height on Z axis
     */
    CylinderZ,
    /**
     * HullCollisionShape
     */
    Hull,
    /**
     * MultiSphere axis-aligned rounded box
     */
    MsBox,
    /**
     * MultiSphere capsule, height on longest axis
     */
    MsCapsule,
    /**
     * MultiSphere sphere
     */
    MsSphere,
    /**
     * SimplexCollisionShape
     */
    Simplex,
    /**
     * SphereCollisionShape
     */
    Sphere,
    /**
     * translated BoxCollisionShape
     */
    TransBox,
    /**
     * translated CapsuleCollisionShape, height on longest axis
     */
    TransCapsule,
    /**
     * translated ConeCollisionShape, height on X axis
     */
    TransConeX,
    /**
     * translated ConeCollisionShape, height on Y axis
     */
    TransConeY,
    /**
     * translated ConeCollisionShape, height on Z axis
     */
    TransConeZ,
    /**
     * translated CylinderCollisionShape, height on X axis
     */
    TransCylinderX,
    /**
     * translated CylinderCollisionShape, height on Y axis
     */
    TransCylinderY,
    /**
     * translated CylinderCollisionShape, height on Z axis
     */
    TransCylinderZ,
    /**
     * translated SimplexCollisionShape
     */
    TransSimplex;
}
