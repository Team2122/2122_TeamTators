# Architecture Spec

## Introduction

The architecture spec describes the overall intended design of the 2025 alpha robot (tidaltator alpha-develop branch). This includes designs for the various algorithms and how various components should interact. This document should be readable without having any understanding of code, and should not be dependent on any particular code implementation.

## How to Read the Diagrams

The following diagrams can be understood through these example diagrams:

### State Machines

**State Machines** have a state, and can transition between states based on conditions or commands.

![Legend for State Machine Diagrams](diagrams/legend.png?raw=true)

### Sequences

**Sequence Diagrams** describe how different parts of the robot interact to preform a complicated action.

![Sequence Legend](sequences/example/annotated.png?raw=true)

## Design of Subsystems

### Coral Affector
The **Coral Affector** can grab and release coral.

![Coral Affector State Machine](diagrams/CoralAffector.png?raw=true)

### Drivetrain

The **Drivetrain** controls the swerve drive.

![Drive State Machine](diagrams/drive.png?raw=true)

### Pivot

The **Pivot** is the rotating arm on which the coral affector is mounted.

![Pivot State Machine](diagrams/pivot.png?raw=true)

### Algae Affector

The alpha bot doesn't have an algae affector, but this diagram was created
before we were clear on the alpha bot's design.

![Algae Affector State Machine](diagrams/algae_affector.png?raw=true)

## Sequences

### Pivot Sequence

The pivot sequence references a Lift subsystem, but the alpha bot has a fixed
tower rather than a moveable lift.

![Pivot Sequence](sequences/pivot+lift.png?raw=true)

### Algae Sequence
![Algae Affector Sequence](sequences/algaesequencediagram.png?raw=true)

