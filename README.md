# Maud

Maud is an editor for jMonkeyEngine animated 3-D models.

Summary of Maud features:
 + import models from Blender or Ogre XML
 + view animations forward/backward at various speeds
 + create new animations from poses or by copying existing animations
 + retarget animations from one model to another
 + edit shadow modes and cull hints
 + rename animations and bones
 + undo and redo any edit
 + complete source code provided under FreeBSD license

![Maud logo](https://github.com/stephengold/Maud/blob/master/src/main/resources/Textures/icons/Maud.png "Maud")

Maud is designed for a desktop environment with a 3-button mouse.

### Conventions

Maud's source code is compatible with both JDK 7 and JDK 8.

### History

The Maud project is hosted at
https://github.com/stephengold/Maud

Maud began as a demo application for the jme3-utilities-debug library, which is
part of the jme3-utilities project at
https://github.com/stephengold/jme3-utilities

Maud includes code from the the BVH Retarget Project at
https://github.com/Nehon/bvhretarget

## How to install the SDK and the Maud project

### jMonkeyEngine3 (jME3) Software Development Kit (SDK)

Maud currently targets Version 3.1 of jMonkeyEngine.  You are welcome to
use the Engine without also using the SDK, but I use the SDK, and the following
installation instructions assume you will too.

The hardware and software requirements of the SDK are documented at
https://jmonkeyengine.github.io/wiki/jme3/requirements.html

 1. Download a jMonkeyEngine 3.1 SDK from https://github.com/jMonkeyEngine/sdk/releases
 2. Install the SDK, which includes:
   + the engine itself,
   + an integrated development environment (IDE) based on NetBeans,
   + various plugins, and
   + the Blender 3D application.
 3. To open the project in the SDK (or NetBeans), you will need the "Gradle
    Support" plugin.  Download and install it before proceeding.

### Source files

Clone the Maud repository using Git:
 1. Open the Clone wizard in the IDE:
   + Menu bar -> "Team" -> "Remote" -> "Clone..."
 2. For "Repository URL:" specify
    "https://github.com/stephengold/Maud.git" (without the quotes).
 3. Clear the "User:" and "Password:" text boxes.
 4. For "Clone into:" specify a writable folder (on a local filesystem)
    which doesn't already contain "Maud".
 5. Click on the "Next >" button.
 6. Make sure the "master" remote branch is checked.
 7. Click on the "Next >" button again.
 8. Make sure the Checkout Branch is set to "master".
 9. Make sure the "Scan for NetBeans Projects after Clone" box is checked.
10. Click on the "Finish" button.
11. When the "Clone Complete" dialog appears, click on the "Open Project..."
    button.

### Build the project

 1. In the "Projects" window, right-click on the "Maud" project to select it.
 2. Select "Build".

## Using Maud

Maud's user interface is composed 3 screens: "Start", "3D View", and "Bind".
The "Start" screen loads first. It merely displays the Maud logo while some
initialization takes place.  It should automatically give way to the 3D View
screen after a few seconds.

The 3D View screen is the main screen of Maud, where 3-D models are viewed
and edited.  There's a menu bar across the top.  The rest of the user-interface
is split into overlapping sub-windows called "tools".  At last count there
were 17 tools.

Selecting a tool makes it visible and moves it to the top for convenient use,
but the controls in the tool will work even when the tool is partly obscured.
You can move a tool around by dragging its title bar with the left mouse button.
You can dismiss a tool by clicking the X in its upper right corner.  Dismissing
a tool won't affect anything else, so it's always a safe move.

Many menu items are numbered.  For instance, the first item in the menu bar is
labeled "1] View".  The numbers indicate keyboard shortcuts for navigating menus
and selecting items.  In other words, you can select the "View" menu by pressing
the "1" key on the main keyboard (not the one on the numeric keypad).

Other useful keyboard shortcuts include:
 + "A" and "D" to rotate the model left and right
 + "." to pause/restart the loaded animation
 + "Esc" to quit from Maud
 + "Numpad-1" to get to a horizontal view
 + "X" to create a checkpoint
 + "Z" to undo to the previous checkpoint
 + "Y" to redo to the next checkpoint
 + "F1" to switch to the Bind screen

Keyboard shortcuts can be customized using the Bind screen (or by editing
the "Interface/bindings/3DView.properties" asset) in which case the shortcuts
listed above might not work.

By default, the camera orbits the "3D Cursor", usually visible as a small,
6-pointed star near the center of the 3D View screen.

TODO more

## Next steps

External links:
  + May 2017 demo video:
    https://www.youtube.com/watch?v=fSjsbyBWlPk

## Acknowledgments

Like most projects, Maud builds on the work of many who have gone before.

I therefore acknowledge the following software developers:
+ Rémy Bouquet (aka "nehon") for creating the BVH Retarget Project.
+ Paul Speed, for helpful insights which got me unstuck during debugging
+ the creators of (and contributors to) the following software:
  + Adobe Photoshop Elements
  + the Gradle build tool
  + the Blender 3D animation suite
  + the FindBugs source code analyzer
  + the Git revision control system
  + the Google Chrome web browser
  + the Java compiler, standard doclet, and runtime environment
  + jMonkeyEngine and the jME3 Software Development Kit
  + LWJGL, the Lightweight Java Game Library
  + Microsoft Windows
  + the NetBeans integrated development environment
  + the Nifty GUI
  + Open Broadcaster Software
  + the RealWorld Cursor Editor

I am grateful to JFrog and Github for providing free hosting for the
Maud Project and many other open-source projects.

I'm also grateful to my dear Holly, for keeping me sane.

If I've misattributed anything or left anyone out, please let me know so I can
correct the situation.