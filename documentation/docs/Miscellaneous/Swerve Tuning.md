Swerve wheels consist of two parts: the **azimuth** controls where the wheel points, while the **drive** actually runs the wheels. Both parts need to be well-tuned for a robot to drive well (though it's not hard to get azimuth well-tuned).

## Step 1: Put the robot on blocks
For tuning the azimuths, the robot needs to be off the ground. Get some blocks (there's a bin with plastic blocks of various shapes somewhere in the shop that works nicely, other kinds of blocks will also work) and use them to get the robot off the ground. Make sure the wheels won't hit or grip anything when rotating + driving.

## Step 2: Align the Azimuths
The azimuths need to be tuned such that, if the drive motors are driving in a *positive* direction and the azimuth motors are at an angle of **0**, the robot should drive forwards.  

Before tuning, we want to deploy Phoenix's **temporary diagnostic server**. It will do absolutely nothing, which is great when you don't want the motors to move while you're tuning them.

1. If there isn't any robot code, Phoenix will readily offer to deploy the diagnostic server in the Devices menu. Click the RUN TEMPORARY DIAGNOSTICS button.  

<img src="../../images/PhoenixTunerX-RunTempDiag-DevicesMenu.png" style="height: 6em">

2. If there is other robot code, or if the button otherwise dosen't show up, go to the Settings menu from the hamburger menu in the upper left.
3. Expand the "FRC Advanced" section and click "Run Temporary Diagnostic Server".

After the temporary diagnostic server is running, do the following for each wheel:

1. Find the Cancoder for that wheel in Phoenix Tuner X. Make sure you've got the right one by using the Blink button.
2. Go into the **Config** tab and set the Magnet Offset to 0. Use the "Apply and flash configs" button (<img src="../../images/PhoenixTunerX-FlashConfigButton.png" style="height: 1em" title="It looks like it's downloading something onto a motor">) to save the config.
3. Turn the wheel so that it's aligned forward-backward (don't worry about if it's inverted yet). The shop has a swerve alignment tool; use it if possible.
4. Go to the **Self Test** tab and click Refresh; this should load a page. Copy the Absolute Position value (only the number), flip the sign (positive to negative and vice versa), and paste it into the Magnet Offset field in the config.
5. Flash the config. Make sure the offset is correct by refreshing the Self Test and checking that the Absolute Position is now near-zero (within ±0.01 is good).
6. Find the drive motor for the current wheel.
7. Go to the Control panel and set the control mode to VoltageOut (below the big red DISABLED button), and set the Output to a very small value (just below 0.5V is good).
8. Enable the robot in Test mode.
9. Click the DISABLED button to enable Phoenix control and run the wheel. If the wheel would push the robot forward (it would push the ground backwards), great! Disable the motor control and skip this step. If not:
    1. Disable motor control, then go back to the Cancoder and go to the Config tab.
    2. If the magnet offset is negative, add 0.5, else subtract 0.5. Flash the config.
10. Make sure to save the magnet offset in code if it's saved there.
11. Swerve wheels are generally not perfectly symmetrical. Check for something that's on one side of the wheel only, and use that information to get the wheel angle right the first time for the rest of the wheels.

After tuning all the azimuths, verify by going either into Manual Tests and using the `SwerveTuningTest` to drive the wheels, or going into Teleop and just driving in that mode.

## Step 3: Drive Motor Tuning
Tuning the drive motors ensures that the bot drives at the correct speed.

1. Get the bot off blocks and on the ground. The tuning will be very different if the wheels aren't pushing a bot.
2. Make sure that the `SwerveTuningTest` manual test is available in the swerve subsystem.
3. Go into Test mode and select the `SwerveTuningTest` test.
4. Open up Shuffleboard or similar tool, and look under the `quick/SwerveTuning` path for [PID values](https://docs.wpilib.org/en/stable/docs/software/advanced-controls/introduction/index.html).
5. First, tune **S**.
    1. Start it at a small guess value (e.g. 0.5V).
    2. Increase S if the bot doesn't move, decrease S if it does.
    3. Repeat until S is just barely *not* enough to move the robot.
6. Next, tune **V**.
    1. You'll need to look at the `DesiredSpeed` and `ActualSpeed` values under the `quick/SwerveTuning` table, preferrably as a graph; open AdvantageScope, connect to the robot (Ctrl-K), and put those two values into a graph.
    2. Start V at a small guess value (0.01).
    2. Increase V if the bot goes too slow, decrease V if it's too fast. Make sure to test moving around as well as rotating.
    3. Repeat until the bot drives *roughly* the speed it's being told to. Don't worry about getting this extremely accurate; that's what P and D are for.
7. Now, tune **P**.
    1. Find some sort of weight to add to the robot, e.g. a stack of cones. Place it on the robot.
    2. Start P at the same value as V.
    3. Increase to increase snappiness, decrease if the wheels make scary noises due to jittering. Make sure to test moving around as well as rotating.
    4. Repeat until it's low enough to consistently avoid scary noises.
    5. Test that the robot still moves well with the weight removed.
8. Finally, tune **D**.
    1. TODO figure out how to use D properly; it seems to just make the wheels jitter when I try to use it. (For future refrence I started it at 1/100th and it instantly caused jitter.)
9. Remeber to save P/D/S/V values to code.

## Step 4: Azimuth Tuning
The azimuth motors are usually just so overpowered that it's very easy to get it very close. If the azimuths don't appear to be poorly tuned, you probably don't need to retune them.

1. Go back into that `SwerveTuningTest` test and hit Back/Select to swap to azimuth mode.
2. Replace the P/D/S/V values with the ones currently in code for the azimuths. (which probably just has P set.)
3. If the azimuths aren't turning very fast, increase P. If they're going crazy, decrease P.
4. Repeat until it gets to its setpoint fairly quickly (say, under 0.3s)
5. Remeber to save P/D/S/V values to code.
