/*
 Copyright (c) 2017-2019, Stephen Gold
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

/**
 * Enumerate centering options for the scene-view camera in orbit mode.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public enum OrbitCenter {
    // *************************************************************************
    // values

    /**
     * orbit the 3-D cursor (default)
     */
    DddCursor,
    /**
     * orbit the world origin
     */
    Origin,
    /**
     * orbit the selected bone, or if none is selected, the 3-D cursor
     */
    SelectedBone,
    /**
     * orbit the selected vertex, or if none is selected, the 3-D cursor
     */
    SelectedVertex;
    // *************************************************************************
    // new methods exposed

    /**
     * Parse a value from a text string.
     *
     * @param string programmer-friendly name of an enum value
     * @return an enum value (not null)
     */
    public static OrbitCenter parse(String string) {
        for (OrbitCenter value : values()) {
            if (value.toString().equals(string)) {
                return value;
            }
        }
        throw new IllegalArgumentException(string);
    }
    // *************************************************************************
    // Enum methods

    /**
     * Read the programmer-friendly name of the enum value.
     *
     * @return name (not null)
     */
    @Override
    public String toString() {
        String result;

        switch (this) {
            case DddCursor:
                result = "3-D cursor";
                break;
            case Origin:
                result = "origin";
                break;
            case SelectedBone:
                result = "selected bone";
                break;
            case SelectedVertex:
                result = "selected vertex";
                break;
            default:
                throw new RuntimeException(name());
        }

        return result;
    }
}
