# Autonomous Documentation
## Basics of Autos
To create an auto you will need to use the existing commands to create a routine.
Every auto is typically represented with an `AutoRoutine` object. The commands these 
`AutoRoutines` call often use trajectories generated
### What is `AutoRoutines.java`?
The `AutoRoutines.java` file holds all of the differents autoroutines that we 
use for autos. For example, this is an autoroutine:  
```java
public static AutoRoutine sourceDeadLeftTest() {
        AutoRoutine routine = autoFactory.newRoutine("Dead Reckon from Source (left)(test)");
        AutoTrajectory start = routine.trajectory("dead_left_start");

        Command command =
                Commands.sequence(
                        start.resetOdometry(),
                        robot
                                .swerve
                                .alignTo(
                                        CoralBranch.NORTH_WEST_RIGHT.getCoralPlacePose(CoralPlaceHeights.CORAL_L4),
                                        3.2,
                                        1.0,
                                        Double.MAX_VALUE)
                                .alongWith(
                                        robot.overwatch.followSequence(List.of(Node.L4PREP), RotationType.SHORTEST)),
                        robot.overwatch.followSequence(
                                List.of(Node.L4PLACE),
                                RotationType.CLOCKWISE,
                                OverwatchConstants.SLOW_MOTION_PROFILE));
    return routine;
}
```
As you can see, autoroutines are just a sequence of commands. As long as you 
know how to use commands, you can create an autoroutine. You will also need to 
add your autoroutine into the autochooser.If we were the autoroutine `sourcedeadleftest`, 

we would add a line to the auto chooser which would look like this:
```java
autoChooser.addRoutine("Dead Reckoned Source (Left)(test)", AutoRoutines::sourceDeadLeftTest);
```
For a more in depth explanation, go to [Choreo Auto Factory Docs](https://choreo.autos/choreolib/auto-factory/).
### Writing a Command Sequence Auto
As you saw in the autoroutine above, most autos are command sequence autos. Because 
they are so common, you will want to know how to write one. There are a few 
basic steps. First you need to make a choreo path. For this, you need to go to the
choreo app and make a path. Making a path is pretty self explanatory. You plot points,
add restrictions, and generate. ![Creating a path in Choreo](../images/Choreo-basic-image.png)
 In every repo, there is a `choreo.chor` file which 
is where all of the paths are stored. Make sure you create your path here. Then 
you need to create a autoroutine in the `AutoRoutines.java` file. The process of 
creating a command sequence auto is just a sequence of commands. The example in 
the section above shows all the parts of a command sequence auto. If you can create 
a command sequence, then you can create a command sequence auto. Then the last step 
is to add your new autoroutine to the auto chooser. An example of the autochooser 
code is also in the section above. You need to add your auto to the autochooser 
by adding a line like this: 
```java
autoChooser.addRoutine("Dead Reckoned Source (Right)", AutoRoutines::sourceDeadRight);
```
You will include the name of your auto which you chose in the `AutoRoutines.java` file. 
After that, you have completed making a command sequence auto.
## State Machines in Auto
### Why State Machines in Auto are Useful
Command sequence autos are not the only type of autos. There is another type of 
auto. A path branching auto is able to detect where it succeded or failed in 
picking or placing. For example, in Reefscape, the path branching auto was able 
to identify if it missed the branch by checking if the coral was still in the affector,
and then go and reatempt the place instead of going back and trying to pick and 
getting stuck. We achieved this by using a state machine. In the offseason we 
had four states. We had a `PICKING`, `PLACING`, `PREPPING`, and `REPLACE`, state.
This allowed for us to detect if either the pick, handoff, or place had failed, 
and then reatempt it. 
### The Importance of Diagramming
Before you make a state machine you always need to make a diagram. it is not just 
that other people need to know what is the logic behind your state machine. You 
also need to diagram so that you are planning out the logic beforehand.
### How to use State Machines in Auto
To make a path branching auto, you will need to use a state machine. If you are 
going to make a state machine, you need to choose what states you need. What states
you need is going to be very year specific. Creating a state machine in auto is 
very similar to creating a state machine for a subsystem. Once you create a state
machine, for each state, you write a command sequence. For example, for a placing 
state from 2025, it would complete the placing sequence, then it would, check if
 it had a coral and decide which state to go from there. In a similar fashion, using
 your diagramming of your state machine, make a switch to decide what state your
 auto will go to. 