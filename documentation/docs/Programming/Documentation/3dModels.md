# 3D Models in AdvantageScope

## Other documentation and downloads

**It is extremely important to look at these documents, especially during your first creation of an articulated component!**

The following are other pieces of documentation needed to create models and articulate components in AdvantageScope. A few things are also mentioned here, but that doesn't mean you should skip these!

* https://docs.advantagescope.org/more-features/custom-assets/

This is the main resource besides this very file you are in now that explains implementing 3D models in AdvantageScope, by the creators of AdvantageScope, team 6328.

**NOTE**: Do **NOT** just watch the video regarding 3D Robot models before creating your own model. Many things in the video are not explained, because you are supposed to **read the documentation** alongside it!

* https://github.wpilib.org/allwpilib/docs/release/java/edu/wpi/first/math/geometry/Pose3d.html

This is WPI-Lib documentaion regarding the different methods used in the movement of articulated components.


* https://dev.opencascade.org/project/cad-assistant

This is a link to CAD Assistant, a program made by Open Cascade, which is the middleman in between Onshape and VS Code.

## Where to get models

The models for our robot and its components are found in our shared Onshape library.

**NOTE**: In order to access this library, your account needs to be invited.

When you sign into Onshape, you should see this library if you were correctly invited and also accepted the invite:
![Sign in page with our frc library](../../images\OnshapeLibrary.png)

## Exporting Models from Onshape

Exporting models is a fairly simple process.

First, click on the model (typically in a folder labelled that robot's year)

Go to the main assembly for the part you are trying to import, specifically one of the files with a **cube containing a shaded section**. 

**right click the file in the tab at the bottom of your screen**, and then press Export.

![ExportLocation](../../images\OnshapeExport.png)

Export the file as a **STEP** file. You will need to make this .glb file in a second, but Onshape does not offer .glb exporting.

You do not really need to do anything else in the export menu apart from exporting to a .STEP file.

## Configuring and exporting in CAD Assistant

<!-- Note: This will need to be updated if we get a better CAD confiuring and exporting software, as CAD assistant is...bad. Blender seems a bit complex for this task, but it could be an option. Preferably, a tool availble across all OS would be best. -->

Once you have your file exported from Onshape, you can't quite put it into VS Code yet. Instead, you must download CAD Assistant, a program that lets you export to .glb files.

This program is also what we use for extra configuration of the models. (ie. Adding a non-moving component onto the base model <!--link to model.glb explanation-->) However, CAD Assistant is very limited in this aspect, making it hard to work with at times.

In most cases, you can simply open up your .STEP file, press the Save icon (floppy disk on left hand side), and press export as  a .glb file. At times (such as the example previously mentioned) though, you might need to move components around. 
![WhereSave](../../images\WhereSaveButton.png)
Location of the Save button

![WhereExport](../../images\WhereExport.png)
What to export as

**NOTE**: if you did indeed read the AdvantageScope documentation closely, you might have seen a mention of glTF files. These are **not** used in this process, so please do not export as a glTF. It will not work.

Once exported, **the file will not be able to be viewed when clicked in the VS code editor!** Do not fret, dear friend. This does not mean its broken, nor does it mean you should be using a glTF file instead. The VS code editor just cant read .glb files, since they are in bianary.

### Combining files in CAD Assistant

The base model for our robot should contain all parts that dont need to move seperately from the robot (For example, the driverbase does not have any parts that move, while the picker does)

This often means that you need to commbine other files into one another to put them on one model. To do this. Open up your current model.glb (see Naming Conventions)

Select the folder icon on the left side and click the button labelled "Add to Current Document" on the bottom tab of the page. **You must do this BEFORE opening up the file you are trying to combine your base model with.**

Click on the file you are trying to combine, and it should appear on your base model. If it isn't where it should be, see the tutorial below.


### Moving things around in CAD Assistant

This can be done with a .STEP file just exported or, a .glb file currently in the code.

**CAD Assistant was not exactly designed for preforming this task.** However, it is still possible. 

First, click any piece of the component you are trying to move. Press the button on the right-hand side that when hovered on reads **"Select Parent."**

You should now have the entire component you are trying to move selected. (Typically, this is from another file)