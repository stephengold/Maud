/*
 Copyright (c) 2018-2020, Stephen Gold
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
    Box(true),
    /**
     * CapsuleCollisionShape, height on longest axis
     */
    Capsule(true),
    /**
     * compounded BoxCollisionShape
     */
    CompoundOfBoxes(false),
    /**
     * compounded HullCollisionShape
     */
    CompoundOfHulls(false),
    /**
     * compounded MeshCollisionShape
     */
    CompoundOfMeshes(false),
    /**
     * ConeCollisionShape, height on X axis
     */
    ConeX(true),
    /**
     * ConeCollisionShape, height on Y axis
     */
    ConeY(true),
    /**
     * ConeCollisionShape, height on Z axis
     */
    ConeZ(true),
    /**
     * CylinderCollisionShape, height on X axis
     */
    CylinderX(true),
    /**
     * CylinderCollisionShape, height on Y axis
     */
    CylinderY(true),
    /**
     * CylinderCollisionShape, height on Z axis
     */
    CylinderZ(true),
    /**
     * HullCollisionShape
     */
    Hull(true),
    /**
     * MultiSphere axis-aligned rounded box
     */
    MsBox(true),
    /**
     * MultiSphere capsule, height on longest axis
     */
    MsCapsule(true),
    /**
     * MultiSphere sphere
     */
    MsSphere(true),
    /**
     * SimplexCollisionShape
     */
    Simplex(true),
    /**
     * SphereCollisionShape
     */
    Sphere(true),
    /**
     * translated BoxCollisionShape
     */
    TransBox(false),
    /**
     * translated CapsuleCollisionShape, height on longest axis
     */
    TransCapsule(false),
    /**
     * translated ConeCollisionShape, height on X axis
     */
    TransConeX(false),
    /**
     * translated ConeCollisionShape, height on Y axis
     */
    TransConeY(false),
    /**
     * translated ConeCollisionShape, height on Z axis
     */
    TransConeZ(false),
    /**
     * translated CylinderCollisionShape, height on X axis
     */
    TransCylinderX(false),
    /**
     * translated CylinderCollisionShape, height on Y axis
     */
    TransCylinderY(false),
    /**
     * translated CylinderCollisionShape, height on Z axis
     */
    TransCylinderZ(false),
    /**
     * translated SimplexCollisionShape
     */
    TransSimplex(false);
    // *************************************************************************
    // fields

    /**
     * true&rarr;shape guaranteed convex, false&rarr;not guaranteed
     */
    final private boolean isConvex;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a ShapeType value.
     *
     * @param isConvex true&rarr;shape guaranteed convex, false&rarr;not
     * guaranteed
     */
    ShapeType(boolean isConvex) {
        this.isConvex = isConvex;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Test whether this shape is guaranteed convex or not.
     *
     * @return true&rarr; guaranteed convex, false&rarr;not guaranteed
     */
    public boolean isConvex() {
        return isConvex;
    }
}
