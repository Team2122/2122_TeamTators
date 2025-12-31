# Architecture Spec

## Introduction

The architecture spec describes the overall intended design of the robot. This includes designs for the various algorithms and how various components should interact. This document should be readable without having any understanding of code, and should not be dependent on any particular code implementation.


## Chamber
![ChamberstatesDiagram](images/ChamberDiagram.png?raw=true)

DRIs

- Sophia (Programming Lead)
- Anant (Programming)
- Sanshubh (Mechanical Lead)


## Grab And Go
The GrabAndGo controlled the interactions among the other subsystems to automate the task of placing a game piece.
This subsystem can be thought of like an orchestrator with each other subsystem being an instrument. 
The individual subsystems do not know about how their actions affect the entire process. They follow the orchestrator who knows what the entire process is. 
Let's run through a quick, simplified example of how a piece went from being on the ground to being placed:

- The driver presses the pick button, so the GrabAndGo tells the picker to come down, which will put the game piece into the chamber.
- Once the chamber senses the game piece, the GrabAndGo tells the arm to go into a position that grabs the game piece. The claw will also be told to open and close at the right time to grab the game piece.
- The GrabAndGo tells the arm to go into the "transport" position. The transport position is the position that allowed the robot to drive and have the game piece secured.
- When the driver presses the place button, the GrabAndGo will tell the arm to go to the placing position. GrabAndGo then waits for the driver to press the release game piece button.
- Once the driver presses the release game piece button, the GrabAndGo tells the claw to drop the game piece onto the target. Then the GrabAndGo tells the arm to go back to the home position.


### State Diagram
For diagram simplicity we split the diagram into two parts: one for cube and one for cone. The states that share the same 
name are identical. The reason we did this was because of how many different arrows there would be within the diagram making 
it more confusing to look at.
![GrabAndGoStateDiagram](images/GrabAndGoStateDiagram.png?raw=true)

### Simplified Arm Positions Diagram
![ArmPositionsDiagram](images/SimplifiedArmPositions.png?raw=true)

DRIs

- Micah (Programming Lead)
- Zayah (Programming)

## Vision Rio
Upon being told to activate it will start running the validation code every time doPeriodic() is called. It modifies
its private members, which get methods return; the get methods are the way for the other subsystems to get the 
values they need.

### State Diagram
![VisionRioStateMachine](images/VisionRioStateDiagram.png?raw=true)

Red boxes are states, blue boxes are what happens in that state. The blue boxes are connected to the state in which they 
describe. This State Diagram only describes what happens if the robot is trying to aim at the target, which is why it is 
only concerned about Retro Reflective Tape, the tape which is around the target. The white cylinders are places where data
is stored.

### Interactions Diagram
![VisionRioInteractionDiagram](images/VisionRioInteractionDiagram.png?raw=true)
This is an example of how all the code interacts. This specific example is for aiming at the target.

### Data In The Network Table
![NetworkTable](images/NetworkTable.png?raw=true)

This describes all the data that is in the Network Table. Vision Rio both uses and modifies the data in the Network Table. 
The Network Table is the means by which the Pi Side and the Rio Side communicate to each other.

DRIs

 - Ibrahim (Programming Lead)
 - Micah (Programming)
 - Matthew (Programming)

## Arm Extension/Rotation

![ArmStateDiagram](images/ArmStateDiagram.png?raw=true)

DRIs

- Alyssa (Programming Lead)
- Cedar (Programming)
- Pierre (Programming)
