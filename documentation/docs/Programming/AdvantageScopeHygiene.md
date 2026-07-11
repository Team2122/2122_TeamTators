# Maintaining Good Hygiene in AdvantageScope and AdvantageKit

To avoid relying on knowledge being passed down from year to year regarding AdvantageScope and AdvantageKit logging, this page goes through the basic logs that should be implemented, what makes an additional log meaningful or not, and the best practices when creating logs.

## The Basics
### Subsystem Logs to Include

When programming subsystems, it is important to include AdvantageScope logs for important information, so if something goes wrong, the source of the problem is easy to find. But what, you ask, should be considered important?

In the main subsystem file:

The following should be kept in the `log()` method, which is called periodically in `Robot.java` like so:

```for (Subsystem subsystem : Subsystem.getSubsystemList()) {subsystem.log();}```

* <b>Automatically logging inputs</b>
    
    Rather than going through the tedious and difficult task of manually logging every single input we might need, processing the inputs automatically logs input values, such as voltage, from various sources in a subsystem. This is where the majority of subsystem logs come from, and is done like so:

    ```io.updateInputs(inputs);``` 
    ```Logger.processInputs("Subsystem", inputs);```

* <b>Logging the current command</b>
    
    This shows what input is coming into that subsystem. For example, this log for the Picker subsystem might display `MOVING` or `IN_POSITION`. This is important because if for example the subsystem didn't respond, we could check the logs and make sure that it received a command telling it to move out of it's original position. Below is an example of a log for the current command.

    ``` Logger.recordOutput( "Subsystem/CurrentCommand", getPossibleCommand().map(Command::getName).orElse("none"));```

* <b>Logging the  3D Pose (Optional)</b>

    In order to simulate the robot in 3D using AdvantageScope, each independently moving component needs it's own `Pose3d` log. (To put a robot into simulation, check out the [3D models in AdvantageScope docs](3dModels.md)) Below is an example of one of these logs.

    ```Logger.recordOutput("Simulation/SubsystemPose", new Pose3d[] {subsystemPose}); ```

### Other Logs to Include (AdvantageKit)

These logs show other data not necessarily related to individual subsystems, and are all stored in `robot.java` in the configureAkit() method:

#### Metadata

These logs contain the metadata, or data that describes the characteristics of other data, for our code. 

* ```Logger.recordMetadata("ProjectName", BuildConstants.MAVEN_NAME);```
> Name of the repository

* ```Logger.recordMetadata("BuildDate", BuildConstants.BUILD_DATE);```
> Time it was built/simulated

* ```Logger.recordMetadata("GitSha", BuildConstants.GIT_SHA);```
> A unique identifier for the current commit

* ```Logger.recordMetadata("GitDate", BuildConstants.GIT_DATE);```
> When last committed
    
* ```Logger.recordMetadata("GitBranch", BuildConstants.GIT_BRANCH);```
>What branch you are on currently

* ```Logger.recordMetadata("GitDirty",switch (BuildConstants.DIRTY) {case 0 -> "All changes committed";```
```case 1 -> "Uncommitted changes";default -> "Unknown";});```
> Displays whether or not all changes have been commited

#### Receiving Data

This changes based on whether or not we are replaying past inputs or displaying it live, since you don't need to create a new directory when looking at old logs. 

<b>For live data: </b>

* make a directory for the log

    ``` File logDir = new File(RobotBase.isReal() ? "/home/lvuser/logs" : "logs");```
    ```if (!logDir.exists()) {logDir.mkdirs();```

* Add data receivers

    ```Logger.addDataReceiver(new WPILOGWriter(logDir.toString()));```
    ```Logger.addDataReceiver(new NT4Publisher());```

* Power distribution logging
    This was done for the 2026 onseason to see the amount of voltage each subsystem was taking up, and just initializes the logging

    ```new PowerDistribution(1, ModuleType.kRev);```

<b> For replayed data: </b>

* Disable the periodic loop to go through the replay as fast as possible

    ```setUseTiming(false);```

* Find the replay log

    ```String logPath = LogFileUtil.findReplayLog();```
    
* Receive the data from the replay log

    ```      Logger.setReplaySource(new WPILOGReader(logPath));```

    ```Logger.addDataReceiver(new WPILOGWriter(LogFileUtil.addPathSuffix(logPath, "_replay"))); ```

## Other Meaningful Logs

In certain circumstances, the logs above don't include all the information we need, and we need to log other information as well. Although additional logs are indeed circumstantial, a determining factor of whether or not you <i> should </i> have a log is the question of "does the thing being logged determine whether or not an action happens?"

For example, when determining whether or not a robot can shoot when simulating the robot in the 2026 game, it checks to make sure it has game pieces in the first place, and those game pieces aren't visualized inside of the robot. As a result, it's important to have a log showing how many game pieces are inside of the hopper, so if the robot isn't shooting and there are game pieces, we would know there is a problem.

Things will inevitably go wrong over the course of developing a robot, and that's okay. But in order to fix those things, we need as much information we can about the components of the code that make our robot do what we want it to, so we can see where they fell short as fast as possible.

## Best Practices

* <b>Keep things organized</b>
    
    * With numerous subsystems working together to make our robot run, the number of logs in AdvantageScope accumulate fast. As a result, we want to make everything as easy to find as possible.

    * To do this, we can store our logs within dropdown menus corresponding to each subsystem or util. This is done like so, with the subsystem name acting as the label for the dropdown:
    ``` Logger.recordOutput("Subsystem/logName", logValue);```

* <b>Give meaningful names to your logs</b>
    
    When logging things manually, it is important to give meaningful names to your logs to ensure that others (and possibly even future you) know why you had that log in the first place. Below are a few tips to achieve this.

    * Avoid names that don't carry any meaning at all, like naming a log for a rotation point "bees".

    * Also try to avoid names that aren't descriptive enough, such as "PickerBoolean".

    Instead, opt for names that describe the purpose of a log. For example, instead of naming a boolean that checks if the picker was deployed "PickerBoolean", consider naming it something like "isDeployed", and storing it within the `Picker` dropdown like so:
    ```Logger.recordOutput("Picker/isDeployed", deployedCheck);```

* <b>Delete temporary logs</b>

    Don't keep logs in the code that are no longer relevant.

    While relevance is circumstantial, the following are a few examples of logs that might not be worth keeping

    * A log used in the debugging process, like a flag that returns whether or not something happened
    * Logs for something a previous version of the robot had, but no longer exists (i.e, if we switched from a turret to a fixed shooter, we would no longer need a turret angle log)




