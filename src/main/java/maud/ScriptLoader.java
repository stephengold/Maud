/*
 Copyright (c) 2017-2023, Stephen Gold
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

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import jme3utilities.MyString;

/**
 * An asset loader for JavaScript assets.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class ScriptLoader implements AssetLoader {
    // *************************************************************************
    // constants

    /**
     * character set used to read scripts
     */
    final public static Charset charset = Charset.forName("UTF-8");
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(ScriptLoader.class.getName());
    // *************************************************************************
    // AssetLoader methods

    /**
     * Load and evaluate a script asset.
     *
     * @param assetInfo (not null)
     * @return the text, or null in case of an error
     */
    @Override
    public Object load(AssetInfo assetInfo) {
        // Open the asset as a stream and create a reader.
        InputStream stream = assetInfo.openStream();
        InputStreamReader reader = new InputStreamReader(stream, charset);

        ScriptEngine scriptEngine;
        scriptEngine = new ScriptEngineManager().getEngineByName("nashorn");

        // Evaluate the script.
        Object result = "";
        try {
            result = scriptEngine.eval(reader);
            if (result == null) {
                result = "";
            }
        } catch (ScriptException exception) {
            String keyString = assetInfo.getKey().toString();
            String message = String.format(
                    "load of JavaScript asset failed: key=%s",
                    MyString.quote(keyString));
            logger.log(Level.SEVERE, message, exception);
        }

        return result;
    }
}
