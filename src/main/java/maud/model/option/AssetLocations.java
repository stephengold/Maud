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
package maud.model.option;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.Validate;
import maud.MaudUtil;
import maud.action.ActionPrefix;

/**
 * The MVC model of asset locations known to Maud.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class AssetLocations implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(AssetLocations.class.getName());
    // *************************************************************************
    // fields

    /**
     * URL specifications of all asset locations (other than the default
     * location) that are known to Maud
     */
    private List<String> knownSpecs = new ArrayList<>(6);
    // *************************************************************************
    // new methods exposed

    /**
     * Add a filesystem asset location, if it's not already known to Maud.
     *
     * @param path a filesystem path to the directory/folder/JAR/ZIP (not null,
     * not empty)
     */
    public void addFilesystem(String path) {
        Validate.nonEmpty(path, "path");

        File file = new File(path);
        String absolutePath = Misc.fixedPath(file);

        String spec = "file:///" + absolutePath;
        if (file.isDirectory()) {
            spec += "/";
        }
        addSpec(spec);
    }

    /**
     * Add an asset location, if it's not already known to Maud.
     *
     * @param spec a URL specification (not null, not empty)
     */
    public void addSpec(String spec) {
        Validate.nonEmpty(spec, "spec");

        if (!knownSpecs.contains(spec)) {
            knownSpecs.add(spec);
        }
    }

    /**
     * Test whether any asset location can be removed.
     *
     * @return true if there is one or more, otherwise false
     */
    public boolean hasRemovable() {
        int count = knownSpecs.size();
        if (count >= 1) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Find the index of the specified asset location.
     *
     * @param spec a URL specification, or null for the default location
     * @return index encoded as decimal string
     */
    public String indexForSpec(String spec) {
        String result;
        if (spec == null) {
            result = "-1";
        } else {
            int index = knownSpecs.indexOf(spec);
            assert index != -1;
            result = Integer.toString(index);
        }

        return result;
    }

    /**
     * Enumerate all asset locations (other than the default location) that are
     * known to Maud.
     *
     * @return a new list of URL specifications
     */
    public List<String> listAll() {
        List<String> result = new ArrayList<>(knownSpecs);
        return result;
    }

    /**
     * Read the spec of the indexed asset location.
     *
     * @param indexString a decimal string (not null, not empty)
     * @return URL specification, or null for the default location
     */
    public String specForIndex(String indexString) {
        Validate.nonEmpty(indexString, "index string");

        String result;
        int index = Integer.parseInt(indexString);
        if (index == -1) {
            result = null;
        } else {
            result = knownSpecs.get(index);
        }

        return result;
    }

    /**
     * Remove the specified asset location.
     *
     * @param spec a URL specification (not null, not empty)
     */
    public void remove(String spec) {
        Validate.nonEmpty(spec, "spec");

        if (knownSpecs.contains(spec)) {
            knownSpecs.remove(spec);
        } else {
            assert false;
        }
    }

    /**
     * Write the locations to a script using the specified writer.
     *
     * @param writer (not null)
     * @throws java.io.IOException if an I/O error occurs while writing
     */
    public void writeToScript(Writer writer) throws IOException {
        Validate.nonNull(writer, "writer");

        for (String spec : knownSpecs) {
            String action = ActionPrefix.newAssetLocationSpec + spec;
            MaudUtil.writePerformAction(writer, action);
        }
    }
    // *************************************************************************
    // Object methods

    /**
     * Create a deep copy of this object.
     *
     * @return a new object, equivalent to this one
     * @throws CloneNotSupportedException if the superclass isn't cloneable
     */
    @Override
    public AssetLocations clone() throws CloneNotSupportedException {
        AssetLocations clone = (AssetLocations) super.clone();
        clone.knownSpecs = new ArrayList<>(knownSpecs);

        return clone;
    }
}
