/*
 Copyright (c) 2017-2021, Stephen Gold
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
package maud.menu;

import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Spatial;
import java.util.logging.Logger;
import maud.Maud;
import maud.action.ActionPrefix;
import maud.dialog.EditorDialogs;
import maud.model.EditorModel;
import maud.model.cgm.CgmOutputFormat;
import maud.model.cgm.EditableCgm;
import maud.model.cgm.SelectedSpatial;
import maud.tool.EditorTools;

/**
 * Menus in Maud's editor screen that deal with computer-graphics models.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class CgmMenus {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(CgmMenus.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private CgmMenus() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Build a CGM menu.
     */
    static void buildCgmMenu(MenuBuilder builder) {
        builder.addTool("Tool");
        builder.addSubmenu("Load");
        builder.addDialog("Save");
        builder.addDialog("Export to XML");

        builder.addSubmenu("Load source model");
        EditorModel model = Maud.getModel();
        if (model.getSource().isLoaded()) {
            SelectedSpatial ss = model.getTarget().getSpatial();
            if (ss.isNode()) {
                builder.addEdit("Merge source model");
            }
            builder.add("Unload source model");
        }

        EditableCgm target = model.getTarget();
        int numSpatials = target.countSpatials(Spatial.class);
        int numInherits
                = target.countSpatials(Spatial.class, Spatial.CullHint.Inherit);
        assert numInherits <= numSpatials;
        boolean allInherit = (numInherits == numSpatials);
        if (!allInherit) {
            builder.addEdit("Reset cull hints to Inherit");
        }

        numInherits = target.countSpatials(Spatial.class,
                RenderQueue.Bucket.Inherit);
        assert numInherits <= numSpatials;
        allInherit = (numInherits == numSpatials);
        if (!allInherit) {
            builder.addEdit("Reset queue buckets to Inherit");
        }
    }

    /**
     * Handle a "load cgm" action without arguments.
     */
    public static void loadCgm() {
        MenuBuilder builder = EditorMenus.newLocationMenu();
        builder.show(ActionPrefix.loadCgmLocator);
    }

    /**
     * Handle a "load sourceCgm" action without arguments.
     */
    public static void loadSourceCgm() {
        MenuBuilder builder = EditorMenus.newLocationMenu();
        builder.show(ActionPrefix.loadSourceCgmLocator);
    }

    /**
     * Handle a "select menuItem" action from the CGM menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    static boolean menuCgm(String remainder) {
        EditorModel model = Maud.getModel();
        EditableCgm target = model.getTarget();

        String actionPrefix;
        boolean handled = true;
        switch (remainder) {
            case "Export to XML":
                actionPrefix = ActionPrefix.saveCgmUnconfirmed
                        + CgmOutputFormat.XML.toString() + " ";
                EditorDialogs.saveCgm("Export", actionPrefix);
                break;

            case "History":
                EditorTools.select("history");
                break;

            case "Load":
                loadCgm();
                break;

            case "Load source model":
                loadSourceCgm();
                break;

            case "Merge source model":
                target.getSpatial().attachClone();
                break;

            case "Reset cull hints to Inherit":
                target.setCullHintAll(Spatial.CullHint.Inherit);
                break;

            case "Reset queue buckets to Inherit":
                target.setQueueBucketAll(RenderQueue.Bucket.Inherit);
                break;

            case "Save":
                actionPrefix = ActionPrefix.saveCgmUnconfirmed
                        + CgmOutputFormat.J3O.toString() + " ";
                EditorDialogs.saveCgm("Save", actionPrefix);
                break;

            case "Tool":
                EditorTools.select("cgm");
                break;

            case "Unload source model":
                model.getSource().unload();
                break;

            default:
                handled = false;
        }

        return handled;
    }
}
