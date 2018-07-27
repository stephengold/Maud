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

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;

/**
 * Enumerate some types of user data.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public enum UserDataType {
    // *************************************************************************
    // values

    Boolean, Float, Integer, Long, String, Vector2f, Vector3f, Vector4f;
    // TODO array, bone, list, map, other savables

    // *************************************************************************
    // new methods exposed
    /**
     * Instantiate an object with this data type.
     *
     * @return a new instance (not null)
     */
    public Object create() {
        Object object;
        switch (this) {
            case Boolean:
                object = false;
                break;
            case Float:
                object = 0f;
                break;
            case Integer:
                object = 0;
                break;
            case Long:
                object = 0L;
                break;
            case String:
                object = "";
                break;
            case Vector2f:
                object = new Vector2f();
                break;
            case Vector3f:
                object = new Vector3f();
                break;
            case Vector4f:
                object = new Vector4f();
                break;
            default:
                throw new IllegalArgumentException();
        }

        return object;
    }
}
