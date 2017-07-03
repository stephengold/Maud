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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * The MVC model of asset folders known to Maud.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class AssetFolders implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            AssetFolders.class.getName());
    // *************************************************************************
    // fields

    /**
     * absolute filesystem paths to all asset folders (other than "Written
     * Assets") that are known to Maud
     */
    private List<String> folderPaths = new ArrayList<>(5);
    // *************************************************************************
    // new methods exposed

    /**
     * Add an asset folder, if it's not already known to Maud.
     *
     * @param path a filesystem path to the folder (not null, not empty)
     */
    public void add(String path) {
        Validate.nonEmpty(path, "folder path");

        File folder = new File(path);
        assert folder.isDirectory();
        String folderPath = folder.getAbsolutePath();
        folderPath = folderPath.replaceAll("\\\\", "/");
        assert folderPath != null;
        assert !folderPath.isEmpty();

        if (!folderPaths.contains(folderPath)) {
            folderPaths.add(folderPath);
        }
    }

    /**
     * Test whether there are any asset folders that can be removed.
     *
     * @return true if there is 1 or more, otherwise false
     */
    public boolean hasRemovable() {
        int count = folderPaths.size();
        if (count >= 1) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Find the file locator index for the specified asset folder.
     *
     * @param folderPath absolute path to the asset folder (not null, not empty)
     * @return a decimal string
     */
    public String indexForPath(String folderPath) {
        Validate.nonEmpty(folderPath, "folder path");

        int index = folderPaths.indexOf(folderPath);
        assert index != -1;
        String result = Integer.toString(index);

        return result;
    }

    /**
     * Enumerate all asset folders.
     *
     * @return a new list of absolute paths
     */
    public List<String> listAll() {
        int numPaths = folderPaths.size();
        List<String> result = new ArrayList<>(numPaths);
        result.addAll(folderPaths);

        return result;
    }

    /**
     * Read the path of the indexed asset folder.
     *
     * @param indexString a decimal string (not null, not empty)
     * @return absolute path to the folder (not null)
     */
    public String pathForIndex(String indexString) {
        Validate.nonEmpty(indexString, "index string");

        int index = Integer.parseInt(indexString);
        String result = folderPaths.get(index);

        return result;
    }

    /**
     * Remove the specified asset folder.
     *
     * @param folderPath absolute path to the asset folder (not null)
     */
    public void remove(String folderPath) {
        Validate.nonEmpty(folderPath, "folder path");

        if (folderPaths.contains(folderPath)) {
            folderPaths.remove(folderPath);
        } else {
            assert false;
        }
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
    public AssetFolders clone() throws CloneNotSupportedException {
        AssetFolders clone = (AssetFolders) super.clone();
        clone.folderPaths = new ArrayList<>(folderPaths);

        return clone;
    }
}
