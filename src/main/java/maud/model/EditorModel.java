/*
 Copyright (c) 2017-2018, Stephen Gold
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

import de.lessvoid.nifty.elements.Element;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.nifty.WindowController;
import jme3utilities.ui.ActionApplication;
import jme3utilities.wes.TweenRotations;
import jme3utilities.wes.TweenTransforms;
import jme3utilities.wes.TweenVectors;
import maud.Maud;
import maud.MaudUtil;
import maud.ScriptLoader;
import maud.action.ActionPrefix;
import maud.model.cgm.EditableCgm;
import maud.model.cgm.LoadedCgm;
import maud.model.option.AssetLocations;
import maud.model.option.MiscOptions;
import maud.model.option.ScoreOptions;
import maud.model.option.scene.SceneOptions;

/**
 * An MVC-model state of the editor screen in the Maud application. Includes all
 * state that's checkpointed.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class EditorModel {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(EditorModel.class.getName());
    // *************************************************************************
    // fields

    /**
     * global list of known asset locations
     */
    final private AssetLocations assetLocations;
    /**
     * load slot for the (editable) target (main) C-G model
     */
    final private EditableCgm targetCgmLoadSlot;
    /**
     * load slot for the skeleton map
     */
    final private EditableMap mapLoadSlot;
    /**
     * load slot for the (read-only) source C-G model
     */
    final private LoadedCgm sourceCgmLoadSlot;
    /**
     * miscellaneous global options
     */
    final private MiscOptions miscOptions;
    /**
     * options for "scene" views
     */
    final private SceneOptions sceneOptions;
    /**
     * options for "score" views
     */
    final private ScoreOptions scoreOptions;
    /**
     * global tweening techniques
     */
    final private TweenTransforms techniques;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an MVC model with the default settings.
     */
    public EditorModel() {
        assetLocations = new AssetLocations();
        targetCgmLoadSlot = new EditableCgm();
        mapLoadSlot = new EditableMap();
        sourceCgmLoadSlot = new LoadedCgm();
        miscOptions = new MiscOptions();
        sceneOptions = new SceneOptions();
        scoreOptions = new ScoreOptions();
        techniques = new TweenTransforms();
    }

    /**
     * Instantiate an MVC model with settings copied from another MVC-model
     * instance.
     *
     * @param other (not null)
     */
    EditorModel(EditorModel other) {
        try {
            assetLocations = other.getLocations().clone();
            targetCgmLoadSlot = other.getTarget().clone();
            mapLoadSlot = other.getMap().clone();
            sourceCgmLoadSlot = other.getSource().clone();
            miscOptions = other.getMisc().clone();
            sceneOptions = other.getScene().clone();
            scoreOptions = other.getScore().clone();
            techniques = other.getTweenTransforms().clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException();
        }
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Access the known asset locations.
     *
     * @return the pre-existing instance (not null)
     */
    public AssetLocations getLocations() {
        assert assetLocations != null;
        return assetLocations;
    }

    /**
     * Access the load slot for the skeleton map.
     *
     * @return the pre-existing instance (not null)
     */
    public EditableMap getMap() {
        assert mapLoadSlot != null;
        return mapLoadSlot;
    }

    /**
     * Access the miscellaneous global options.
     *
     * @return the pre-existing instance (not null)
     */
    public MiscOptions getMisc() {
        assert miscOptions != null;
        return miscOptions;
    }

    /**
     * Access the options for "scene" views.
     *
     * @return the pre-existing instance (not null)
     */
    public SceneOptions getScene() {
        assert sceneOptions != null;
        return sceneOptions;
    }

    /**
     * Access the options for "score" views.
     *
     * @return the pre-existing instance (not null)
     */
    public ScoreOptions getScore() {
        assert scoreOptions != null;
        return scoreOptions;
    }

    /**
     * Access the load slot for the (read-only) source C-G model.
     *
     * @return the pre-existing instance (not null)
     */
    public LoadedCgm getSource() {
        assert sourceCgmLoadSlot != null;
        return sourceCgmLoadSlot;
    }

    /**
     * Access the load slot for the (editable) target (main) C-G model.
     *
     * @return the pre-existing instance (not null)
     */
    public EditableCgm getTarget() {
        assert targetCgmLoadSlot != null;
        return targetCgmLoadSlot;
    }

    /**
     * Access the global tweening techniques.
     *
     * @return the pre-existing instance (not null)
     */
    public TweenTransforms getTweenTransforms() {
        assert techniques != null;
        return techniques;
    }

    /**
     * Callback invoked after restoring a checkpoint.
     */
    public void postMakeLive() {
        sourceCgmLoadSlot.getSceneView().postMakeLive();
        targetCgmLoadSlot.getSceneView().postMakeLive();
    }

    /**
     * Callback invoked before creating a checkpoint.
     */
    void preCheckpoint() {
        mapLoadSlot.preCheckpoint();
        targetCgmLoadSlot.preCheckpoint();
    }

    /**
     * Callback invoked before restoring a checkpoint.
     */
    public void preMakeLive() {
        sourceCgmLoadSlot.getSceneView().preMakeLive();
        targetCgmLoadSlot.getSceneView().preMakeLive();
    }

    /**
     * Update the startup script based on this MVC model.
     */
    public void updateStartupScript() {
        try {
            writeStartupScript(Maud.startupScriptAssetPath);
        } catch (IOException exception) {
            logger.log(Level.SEVERE,
                    "Output exception while writing startup script to {0}!",
                    MyString.quote(Maud.startupScriptAssetPath));
            throw new RuntimeException(exception);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Write a startup script.
     *
     * @param assetPath asset path to startup script (not null)
     */
    private void writeStartupScript(String assetPath) throws IOException {
        assert assetPath != null;

        logger.log(Level.INFO, "Updating startup script in asset {0}.",
                MyString.quote(assetPath));

        FileOutputStream stream = null;
        String filePath = ActionApplication.filePath(assetPath);
        try {
            File file = new File(filePath);
            File parentDirectory = file.getParentFile();
            if (parentDirectory != null && !parentDirectory.exists()) {
                boolean success = parentDirectory.mkdirs();
                if (!success) {
                    String parentPath = parentDirectory.getAbsolutePath();
                    parentPath = parentPath.replaceAll("\\\\", "/");
                    String msg = String.format(
                            "Unable to create folder %s for startup script",
                            MyString.quote(parentPath));
                    throw new IOException(msg);
                }
            }
            stream = new FileOutputStream(filePath);
            OutputStreamWriter writer
                    = new OutputStreamWriter(stream, ScriptLoader.charset);
            writeToScript(writer);

        } catch (IOException exception) {
            throw exception;

        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

    /**
     * Write an updated startup script to the specified writer.
     *
     * @param writer (not null)
     */
    private void writeToScript(Writer writer) throws IOException {
        String declareMaud = "var Maud = Java.type('maud.Maud');\n";
        writer.write(declareMaud);

        assetLocations.writeToScript(writer);
        miscOptions.writeToScript(writer);
        sceneOptions.writeToScript(writer);
        scoreOptions.writeToScript(writer);
        /*
         * tweening techniques
         */
        TweenVectors tweenTranslations = techniques.getTweenTranslations();
        String arg = tweenTranslations.toString();
        MaudUtil.writePerformAction(writer,
                ActionPrefix.selectTweenTranslations + arg);

        TweenRotations tweenRotations = techniques.getTweenRotations();
        arg = tweenRotations.toString();
        MaudUtil.writePerformAction(writer,
                ActionPrefix.selectTweenRotations + arg);

        TweenVectors tweenScales = techniques.getTweenScales();
        arg = tweenScales.toString();
        MaudUtil.writePerformAction(writer,
                ActionPrefix.selectTweenScales + arg);
        /*
         * Always load Jaime at startup.
         */
        MaudUtil.writePerformAction(writer,
                ActionPrefix.loadCgmNamed + "Jaime");
        /*
         * Select and position each selected tool.
         */
        for (WindowController tool : Maud.gui.listWindowControllers()) {
            if (tool.isEnabled()) {
                String toolId = tool.getId();
                assert toolId.endsWith("Tool");
                String name = MyString.removeSuffix(toolId, "Tool");
                Element element = tool.getElement();
                int x = element.getX();
                int y = element.getY();
                String action = String.format("%s%s %d %d",
                        ActionPrefix.selectToolAt, name, x, y);
                MaudUtil.writePerformAction(writer, action);
            }
        }

        writer.close();
    }
}
