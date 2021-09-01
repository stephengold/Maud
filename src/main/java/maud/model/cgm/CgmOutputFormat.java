/*
 Copyright (c) 2019-2021, Stephen Gold
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

import com.jme3.export.JmeExporter;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.export.xml.XMLExporter;

/**
 * Enumerate the file formats in which Maud can save/export CGMs.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public enum CgmOutputFormat {
    // *************************************************************************
    // values

    /**
     * JME's native binary format
     */
    J3O,
    /**
     * XML text format
     */
    XML;
    // *************************************************************************
    // new methods exposed

    /**
     * Instantiate a JmeExporter for this format.
     *
     * @return a new instance
     */
    public JmeExporter getExporter() {
        switch (this) {
            case J3O:
                return BinaryExporter.getInstance();
            case XML:
                return XMLExporter.getInstance();
            default:
                throw new IllegalStateException(this.toString());
        }
    }

    /**
     * Extend a base path for this format.
     *
     * @param basePath a file/asset path to extend (not null, not empty)
     * @return the resulting path (not null, not empty)
     */
    public String extend(String basePath) {
        String result = basePath + "." + extension();
        return result;
    }

    /**
     * Determine the standard file extension for this format, excluding the
     * separator dot.
     *
     * @return a text string (not null, not empty)
     */
    public String extension() {
        switch (this) {
            case J3O:
                return "j3o";
            case XML:
                return "xml";
            default:
                throw new IllegalStateException(this.toString());
        }
    }
}
