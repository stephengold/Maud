# Maud

Maud is an editor for the animated 3-D models used by jMonkeyEngine.

<img height="150" src="https://github.com/stephengold/Maud/blob/master/src/main/resources/Textures/icons/Maud.png" alt="Maud logo"/>

Summary of features:
 + import models from Blender/Ogre/Wavefront and save to native J3O format
 + import animations from BVH
 + visualize animations, axes, bones, bounding boxes, skeletons, and physics objects
 + play animations forward/backward at various speeds and pause them
 + add new animations from poses or by altering existing animations
 + retarget animations from one model to another using skeleton maps
 + insert keyframes in animations and bone tracks
 + rename animations, bones, spatials, and user data
 + change animation durations
 + behead/truncate animations
 + reduce/wrap animations and bone tracks
 + delete animations, bone tracks, and keyframes
 + add scene-graph controls and user data to spatials
 + modify spatial transforms (translation, rotation, and scale)
 + modify spatial batch modes, cull hints, render queues, shadow modes, and user data
 + delete scene-graph controls and user data
 + review edit history and undo/redo edits
 + customize mouse-button assignments and keyboard shortcuts
 + complete Java source code provided under FreeBSD license

Maud was designed for a desktop environment with a wheel mouse.

Status as of July 2017: under development, will seek beta testers soon.

### Conventions

Maud's source code is compatible with both JDK 7 and JDK 8.

World coordinate system: the Y axis points upward (toward the zenith).

### History

Since April 2017, the Maud project has been hosted at
https://github.com/stephengold/Maud

Maud began as a demo application for the jme3-utilities-debug library,
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
    that doesn't already contain "Maud".
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

### Overview of the user interface

Maud's user interface is composed 3 screens: "Start", "3D View", and "Bind".
The "Start" screen loads first. It merely displays the logo while some
initialization takes place.  It should automatically give way to the 3D View
screen after a few seconds.

The 3D View screen is the main screen of Maud, where 3-D models are viewed
and edited.  There's a menu bar across the top.  The rest of the user-interface
is split into overlapping sub-windows called "tools".  At last count there
were 29 tools.

Selecting a tool makes it visible and moves it to the top for convenient use,
but the controls in the tool will work even when partly obscured.
You can move a tool around by dragging its title bar with the left mouse
button (LMB).
You can dismiss a tool by clicking the X in its upper right corner.
Dismissing a tool won't affect anything else, so it's always a safe move.

![wrench icon](https://github.com/stephengold/Maud/blob/master/src/main/resources/Textures/icons/tool.png)
![dialog icon](https://github.com/stephengold/Maud/blob/master/src/main/resources/Textures/icons/dialog.png)
![bone icon](https://github.com/stephengold/Maud/blob/master/src/main/resources/Textures/icons/bone.png)

Many menu items display icons to help describe what they do:
 + a wrench icon to select a tool
 + a dialog box icon to open a modal dialog box
 + a bone icon to select a bone
 + and so forth

Many menu items are numbered.  For instance, the first item in the menu bar is
labeled "1] View".  The numbers indicate keyboard shortcuts for navigating menus
and selecting items.  In other words, you can select the "View" menu by pressing
the "1" key on the main keyboard (but not the one on the numeric keypad).

Other standard keyboard shortcuts include:
 + "A" and "D" to rotate the target model left and right
 + "." to pause/restart loaded animation(s)
 + "Esc" to quit from Maud
 + "X" to create a checkpoint
 + "Z" to undo to the previous checkpoint
 + "Y" to redo to the next checkpoint
 + "F1" to switch to the Bind screen

Mouse-button assignments and keyboard shortcuts can be customized using the
Bind screen (or by editing the "Interface/bindings/3DView.properties" asset)
in which case shortcuts mentioned in this document might not work.

### Maud's camera

The mouse wheel and middle mouse button (MMB) control the camera (viewpoint).
Turn the wheel to move forward or back. Drag with MMB to turn the camera.

Beyond that, it gets complicated, since Maud's camera (viewpoint) operates in
2 movement modes and 2 projection modes.
The Camera Tool (selected via the View menu) can be used to change modes.

"Orbit mode" is the camera's default movement mode.
In orbit mode, the camera orbits the "3D cursor" -- typically visible
as a small, white, 6-pointed star at the center of the 3D View screen.
In orbit mode, turning the camera also changes its location, making it easy to
view models from different directions.

Move the 3D cursor to a new location by clicking LMB on an object in the scene.
The 3D cursor never attaches to any object, so moving or altering objects
in the scene won't affect the 3D cursor.
The Cursor Tool (selected via the View menu) can be used to alter the
appearance of the 3D cursor.

"Fly mode" is the camera's alternative movement mode.
In fly mode, the camera disregards the 3D cursor, allowing it to close in
on locations where the 3D cursor can't easily reach, such as the interior
of a model.

"Perspective mode" is the camera's default projection mode, and "parallel mode"
is its alternative projection mode.  In parallel mode, the mouse wheel
affects the scale of the projection without actually moving the camera.

Standard keyboard shortcuts affecting the camera:
 + "Numpad-1" to move (or rotate) to a horizontal view
 + "Numpad-5" to toggle between perspective and parallel projection modes

### Loading (or importing) model assets

Maud always has a model, called the "target", loaded into the scene.
At startup, Maud automatically loads the Jaime model (from the
jme3-testdata library) as the target.

In addition to the target, a second model, called the "source",
can be loaded into the scene.
The source can't be edited; only the target can be edited.
However, the source can be animated and viewed, and it can supply
animations for retargeting.

To rotate the source, press "A" or "D" while holding down a shift key.

The Model Tool (selected via the Model->Target menu) displays basic
information about the target model.

TODO more

## Next steps

External links:
  + May 2017 demo video:
    https://www.youtube.com/watch?v=fSjsbyBWlPk
  + June 2017 retargeted animation video:
    https://www.youtube.com/watch?v=yRjh1rAsipI

## Acknowledgments

Like most projects, Maud builds on the work of many who have gone before.

I therefore acknowledge the following software developers:
+ Rémy Bouquet (aka "nehon") for creating the BVH Retarget Project.
+ Paul Speed, for helpful insights that got me unstuck during debugging
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
  + the Nifty graphical user interface
  + Open Broadcaster Software
  + the RealWorld Cursor Editor
  + the WinMerge differencing and merging tool

I am grateful to JFrog and Github for providing free hosting for the
Maud Project and many other open-source projects.

I'm also grateful to my dear Holly, for keeping me sane.

If I've misattributed anything or left anyone out, please let me know so I can
correct the situation.