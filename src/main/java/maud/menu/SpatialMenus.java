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
package maud.menu;

import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyString;
import maud.Maud;
import maud.model.cgm.Cgm;
import maud.model.cgm.SelectedSpatial;

/**
 * Spatial menus in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SpatialMenus {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SpatialMenus.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private SpatialMenus() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Handle a "select menuItem" action from the Spatial menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    static boolean menuSpatial(String remainder) {
        boolean handled = true;
        String selectPrefix = "Select" + EditorMenus.menuPathSeparator;
        if (remainder.startsWith(selectPrefix)) {
            String arg = MyString.remainder(remainder, selectPrefix);
            handled = menuSpatialSelect(arg);

        } else {
            switch (remainder) {
                case "Delete":
                    Maud.getModel().getTarget().getSpatial().delete();
                    break;

                case "Delete extras":
                    Maud.getModel().getTarget().deleteExtraSpatials();
                    break;

                case "Details":
                    Maud.gui.tools.select("spatialDetails");
                    break;

                case "Material":
                    Maud.gui.tools.select("material");
                    break;

                case "Rotate":
                    Maud.gui.tools.select("spatialRotation");
                    break;

                case "Scale":
                    Maud.gui.tools.select("spatialScale");
                    break;

                case "Select":
                    ShowMenus.selectSpatial();
                    break;

                case "Tool":
                    Maud.gui.tools.select("spatial");
                    break;

                case "Translate":
                    Maud.gui.tools.select("spatialTranslation");
                    break;

                case "User data tool":
                    Maud.gui.tools.select("userData");
                    break;

                default:
                    handled = false;
            }
        }

        return handled;
    }

    /**
     * Handle a "select spatial" action with an argument.
     *
     * @param argument action argument (not null)
     * @param includeNodes true &rarr; include both nodes and geometries, false
     * &rarr; include geometries only
     */
    public static void selectSpatial(String argument, boolean includeNodes) {
        Cgm target = Maud.getModel().getTarget();
        if (target.hasSpatial(argument)) {
            target.getSpatial().select(argument);

        } else {
            /*
             * Treat the argument as a spatial-name prefix.
             */
            List<String> names
                    = target.listSpatialNames(argument, includeNodes);
            ShowMenus.showSpatialSubmenu(names, includeNodes);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Handle a "select menuItem" action from the "Spatial -> Select" menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private static boolean menuSpatialSelect(String remainder) {
        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        boolean handled = true;
        switch (remainder) {
            case "Attachments node":
                spatial.selectAttachmentsNode();
                break;

            case "By name":
                selectSpatial("", true);
                break;

            case "Child":
                ShowMenus.selectSpatialChild("");
                break;

            case "Geometry":
                selectSpatial("", false);
                break;

            case "Parent":
                spatial.selectParent();
                break;

            case "Root":
                spatial.selectCgmRoot();
                break;

            default:
                handled = false;
        }

        return handled;
    }
}
