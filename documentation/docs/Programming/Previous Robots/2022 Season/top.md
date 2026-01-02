# Architecture Spec

## Introduction

The architecture spec describes the overall intended design of the robot. This includes designs for the various algorithms and how various components should interact. This document should be readable without having any understanding of code, and should not be dependent on any particular code implementation.

## Example Subsystem : VCD Dumper
The VCD Dumper is a debugging tool based on simulation tools used by digital hardware engineers. Essentially the VCD Dumper is designed to log values of important variables in a compressed file format. [1364 Verilog LRM](https://ieeexplore.ieee.org/document/1620780)

DRIs

 - Kareem (Programming Lead)

Interface: 
![Example image alt-text](images/VCDExample.png?raw=true)

## Ball Channel

![BallChannelStateDiagram](images/Ball_Channel_State_Diagram.png?raw=true)

DRIs

- Disha (Programming Lead)
- Alyssa (Programming)
- Austin (Mechanical Lead)

## Picker

![PickerStateDiagrams](images/PickerStates.png?raw=true)

DRIs

 - Sophia (Programming Lead)
 - Kavya (Programming)
 - Sanshubh (Programming)
 - Theron (Mechanical Lead)

## Shooter

![ShooterStateDiagram](images/ShooterStates.png?raw=true)

DRIs

 - Sophia (Programming Lead)
 - Kavya (Programming)
 - Sanshubh (Programming)
 - Cole (Mechanical Lead)

## Climber

DRIs

 - Disha (Programming Lead)
 - Grace (Mechanical Lead)

## Drive Train

![Swerve](images/Swerve.png?raw=true)

DRIs 

 - Ibrahim
 - Eddie

## Vision Rio

Upon being told to activate it will start running the validation code every time
doPeriodic() is called. It modifies its private members, which get methods
return; the get methods are the way for the other subsystems to get the values
they need.

### State Diagram
![VisionRioStateMachine](images/VisionRioStateDiagram.png?raw=true)

Red boxes are states, blue boxes are what happens in that state. The blue boxes
are connected to the state in which they describe. This State Diagram only
describes what happens if the robot is trying to aim at the target, which is why
it is only concerned about Retro Reflective Tape, the tape which is around the
target. The white cylinders are places where data is stored.

### Interactions Diagram
![VisionRioInteractionDiagram](images/VisionRioInteractionDiagram.png?raw=true)
This is an example of how all the code interacts. This specific example is for aiming at the target.

### Data In The Network Table
![NetworkTable](images/NetworkTable.png?raw=true)

This describes all the data that is in the Network Table. Vision Rio both uses
and modifies the data in the Network Table. The Network Table is the means by
which the Pi Side and the Rio Side communicate to each other.

DRIs

 - Ibrahim (Programming Lead)
 - Micah (Programming)
 - Matthew (Programming)
