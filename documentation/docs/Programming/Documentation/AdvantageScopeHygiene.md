# Maintaining Good Hygiene in AdvantageScope and AdvantageKit

To avoid relying on knowledge being passed down from year to year regarding AdvantageScope and AdvantageKit logging, this page goes through the best practices when creating logs.

### Subsystem Logs to include

When programming subsystems, it is important to include AdvantageScope logs for important information, so if something goes wrong, the source of the problem is easy to find. But what, you ask, should be considered important?

In `Subsystem.java`:

The following should be kept in the `log()` method, which is called periodically in `Robot.java` like so:

```for (Subsystem subsystem : Subsystem.getSubsystemList()) {subsystem.log();}```

* Automatically logging inputs
    
    Rather than going through the tedious and difficult task of manually logging every single input we might need, processing the inputs automatically logs input values, such as voltage, from various sources in a subsystem. This is where the majority of subsystem logs come from, and is done like so:

    ```io.updateInputs(inputs);``` 
    ```Logger.processInputs("Subsystem", inputs);```

* Logging the current command
    
    This shows what input is coming into that subsystem. For example, this log for the Picker subsystem might display `MOVING` or `IN_POSITION`. This is important because if for example the subsystem didn't respond, we could check the logs and make sure that it received a command telling it to move out of it's original position. Below is an example of a log for the current command.

    ``` Logger.recordOutput( "Subsystem/CurrentCommand", getPossibleCommand().map(Command::getName).orElse("none"));```
* Logging the  3D Pose (Optional)

    In order to simulate the robot in 3D using AdvantageScope, each independently moving component needs it's own `Pose3d` log. (To put a robot into simulation, check out the [3D models in AdvantageScope docs](3dModels.md)) Below is an example of one of these logs.

    ```Logger.recordOutput("Simulation/SubsystemPose", new Pose3d[] {subsystemPose}); ```

### Other logs to include (AdvantageKit)

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

* ```Logger.recordMetadata("GitDirty",switch (BuildConstants.DIRTY) {case 0 -> "All changes committed";case 1 -> "Uncommitted changes";default -> "Unknown";});```
> Displays whether or not all changes have been commited

#### Receiving data

This changes based on whether or not we are replaying past inputs or displaying it live, since you don't need to create a new directory when looking at old logs. 

For live data:
* make a directory for the log

    ``` File logDir = new File(RobotBase.isReal() ? "/home/lvuser/logs" : "logs");if (!logDir.exists()) {logDir.mkdirs();} ```

* Add data receivers

    ```Logger.addDataReceiver(new WPILOGWriter(logDir.toString()));```
    ```Logger.addDataReceiver(new NT4Publisher());```

* Power distribution logging
    This was done for the 2026 onseason to see the amount of voltage each subsystem was taking up, and just initializes the logging

    ```new PowerDistribution(1, ModuleType.kRev);```

For replayed data:

* Disable the periodic loop to go through the replay as fast as possible

    ```setUseTiming(false);```

* Find the replay log

    ```String logPath = LogFileUtil.findReplayLog();```
* Receive the data from the replay log

    ```      Logger.setReplaySource(new WPILOGReader(logPath));```

    ```Logger.addDataReceiver(new WPILOGWriter(LogFileUtil.addPathSuffix(logPath, "_replay"))); ```





### Best practices

* Keep things organized
    
    * With numerous subsystems working together to make our robot run, the number of logs in AdvantageScope accumulate fast. As a result, we want to make everything as easy to find as possible.

    * To do this, we can store our logs within dropdown menus corresponding to each subsystem or util. This is done like so, with the subsystem name acting as the label for the dropdown:

    ``` Logger.recordOutput("Subsystem/logName", logValue);```

<br>

* Give meaningful names to your logs
    
    When logging things manually, it is important to give meaningful names to your logs to ensure that others (and possibly even future you) know why you had that log in the first place. Below are a few tips to achieve this. 
    * Avoid names that don't carry any meaning at all, like naming a log for a rotation point "bees".
    * Also try to avoid names that aren't descriptive enough, such as "PickerBoolean".

    Instead, opt for names that describe the purpose of a log. For example, instead of naming a boolean that checks if the picker was deployed "PickerBoolean", consider naming it something like "isDeployed", and storing it within the `Picker` dropdown like so:

    ```Logger.recordOutput("Picker/isDeployed", deployedCheck);```



