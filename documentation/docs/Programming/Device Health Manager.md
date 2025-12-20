The **device health manager** is a system for logging when things go wrong in subsystems, e.g. when a motor/sensor gets disconnected or has an error.

## How it works

Each subsystem must implement `getHealth()`:
```java
@Override
public boolean getHealth() {
    // should return `true` if the system is fine and `false` if something is disconnected/not working
    return isEverythingFine;
}
```

The `DeviceHealthManager` will go through each subsystem and log if a system reports itself as unhealthy. `Robot.java` should call `DeviceHealthManager.logHealth()` every tick and `DeviceHealthManager.printHealth(currentControlMode)` when going into Disabled from another state.

There are two fault states it keeps:

- **Current** faults only record what's unhealthy right *now*; if a system recovers the current fault for it will disappear
- **Sticky** faults will persist even if the system recovers; useful for knowing if a system went down at all during a match.

## Where it logs
The `DeviceHealthManager` will log the status of motors and sensors in the driver station's console when the robot disables.
```
===== ROBOT HEALTH REPORT =====
>>>>>>> From Teleop
Current faults: none
Sticky faults: Pivot, Affector
===============================
```

It will also continuously log the faults to NetworkTables under `AdvantageKit/Health/`:

- `Health/Momentary/Healthy` will be true if nothing is currently having issues.
- `Health/Sticky/Healthy` will be true if nothing has had issues at all during the match.
- `Health/Momentary/Faults` and `Health/Sticky/Faults` will log exactly what systems have/had issues, or "nothing" if no issues are/were detected.
