# Making an SD Card Backup at a Competition

*When the robot's code is modified during a competition, we back up the new code on an SD card.*

## Reimaging the Card
This should happen before the start of qualifying matches and before the start of playoff matches.

On a Tators laptop:

1. Use **balenaEtcher** to re-image the SD card. To find the SD card image on Windows:
    * Right click on the "roboRIO Imaging Tool" icon and select "Open file location" from the pop-up menu.
    * Browse to the "FRC Images" > "SD Images" directory in the same location as the "roboRIO_ImagingTool" application.
2. Use **roboRIO Team Number Setter** to set our team number (`2122`) on the SD card. See [https://docs.wpilib.org/en/stable/docs/software/wpilib-tools/roborio-team-number-setter/index.html](https://docs.wpilib.org/en/stable/docs/software/wpilib-tools/roborio-team-number-setter/index.html)

## Updating Code on an SD Card

This should happen **every time** a commit is made to the event branch for that day.

On a mobile phone:

1. Make sure the hotspot is enabled and allows external devices to join.

On a Tators laptop:

1. Join the hotspot.
2. Open a terminal window.
3. `git checkout [branch that has code changes]`
4. `git pull`
5. Leave the terminal window open. You will use it again below.

Back on the mobile phone:

1. You may disable the hotspot.

With roboRIO device:

1. Make sure the roboRIO is unplugged from its power source.
2. Connect a USB cable between Tators laptop and roboRIO.
3. Insert an imaged SD card.
4. Plug the roboRIO back into its power source.
5. To check if the USB connection is working, `ping 172.22.11.2`. The response should show packets being transmitted rather than a request timeout.

On the laptop:

1. In Visual Studio Code, deploy the robot code by choosing the "WPILib:Deploy robot code" menu item (Ctrl+Shift+P) or clicking the WPILib icon in the top, right corner.
2. Watch the progress of the deployment in the driver’s station dashboard.
3. When deployment is done, unplug the roboRIO from its power source.
4. Remove the SD card.
5. In the terminal window, `git log --max-count 1` (shows the current commit hash, after the word "commit" on the first line of output)

With SD card:

1. With a small piece of tape and a Sharpie, label the card with the first 4 characters of the commit hash of the code changes and the date/time the backup was made.
