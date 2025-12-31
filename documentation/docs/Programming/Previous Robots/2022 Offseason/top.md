# Architecture Spec

## Introduction

The architecture spec describes the overall intended design of the robot. This includes designs for the various algorithms and how various components should interact. This document should be readable without having any understanding of code, and should not be dependent on any particular code implementation.

## Ball Channel

![BallChannel](diagrams/BallChannel.png?raw=true)

DRIs

- Mayahuel ( Programming Lead )
- Alyssa (Programming)

## Picker
![PickerStateDiagrams](diagrams/PickerState.png?raw=true)

DRIs

 - Matthew ( Programming Lead )
 - Austin ( Mechanical Lead )


## Shooter

<!-- this diagram does not exist, but this link present in the original repo's
documentation file. ![ShooterStateDiagram](diagrams/ShooterStates.png?raw=true)
-->

DRIs

- Ibrahim ( Programming Lead )

## Climber

![Climber](diagrams/ClimberDiagram.png?raw=true)

DRIs

- Sophia (Programming Lead)
- Grace (Mechanical Lead)

## Swerve Drive

![Swerve](diagrams/Swerve.png?raw=true)

DRIs

- Micah ( Programming Lead )
- Ibrahim ( Mentor )

### Swerve Drive Calibration Diagram
![SwerveCalibrationDiagram](diagrams/SwerveCalibrationDiagram.png?raw=true)

### Swerve Drive Interaction Diagram
![SwerveInteractionDiagram](diagrams/SwerveInteractionDiagram.png?raw=true)

### Swerve Drive State Machine Diagram
![SwerveStateMachineDiagram](diagrams/SwerveStateMachineDiagram.png?raw=true)

## Ball Counter

![BallCounter](diagrams/BallCounter.png?raw=true)

DRIs

- Matthew ( Programming Lead )

## Vision

Upon being told to activate it will start running the validation code every time
doPeriodic() is called. It modifies its private members, which get methods
return; the get methods are the way for the other subsystems to get the values
they need.

### Vision State Diagram

![VisionRioStateMachine](diagrams/VisionRioStateDiagram.png?raw=true)

Red boxes are states, blue boxes are what happens in that state. The blue boxes
are connected to the state in which they describe. This State Diagram only
describes what happens if the robot is trying to aim at the target, which is why
it is only concerned about Retro Reflective Tape, the tape which is around the
target. The white cylinders are places where data is stored.

### Subsytem Interactions Diagram

![VisionRioInteractionDiagram](diagrams/VisionRioInteractionDiagram.png?raw=true)

This is an example of how all the code interacts. This specific example is for aiming at the target.

### Network Table Data

![NetworkTable](diagrams/NetworkTable.png?raw=true)

This describes all the data that is in the Network Table. Vision Rio both uses
and modifies the data in the Network Table. The Network Table is the means by
which the Pi Side and the Rio Side communicate to each other.

DRIs

- Micah (Programming)
- Matthew (Programming)
