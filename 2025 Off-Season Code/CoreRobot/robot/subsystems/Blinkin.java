package frc.robot.subsystems;

import static edu.wpi.first.units.Units.Meters;

import edu.wpi.first.hal.PWMJNI;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEvent;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.RobotBase;
import edu.wpi.first.wpilibj.motorcontrol.Spark;
import frc.robot.Robot;
import frc.robot.constants.Constants;
import frc.robot.constants.FieldConstants;
import frc.robot.subsystems.affector.Affector;
import frc.robot.subsystems.chamberOfCorals.ChamberOfCorals;
import frc.robot.subsystems.climber.Climber;
import frc.robot.subsystems.climber.ClimberConstants.ClimberPositions;
import frc.robot.subsystems.coralPicker.CoralPicker;
import frc.robot.subsystems.coralPicker.CoralPickerConstants.CoralPickerPositions;
import frc.robot.subsystems.overwatch.Overwatch;
import frc.robot.subsystems.swerve.SwerveDrive;
import java.util.EnumSet;
import java.util.Optional;
import org.littletonrobotics.junction.Logger;
import org.teamtators.constants.BlinkinRawColors;
import org.teamtators.tester.ManualTestGroup;
import org.teamtators.tester.components.BlinkinColorTest;
import org.teamtators.util.DeviceHealthManager;
import org.teamtators.util.Subsystem;
import org.teamtators.util.Timer;

public class Blinkin extends Subsystem {
    // both color 1 and color 2 are free
    enum BlinkinColor {
        /*** GENERAL ***/
        INIT(BlinkinRawColors.Fixed_BreathGray),
        IDLE(BlinkinRawColors.Solid_White),
        DEVICE_UNHEALTHY(BlinkinRawColors.Fixed_LarsonScannerRed),

        /*** DEEP CLIMB ***/
        DEEP_CLIMB(BlinkinRawColors.Fixed_RainbowRainbowPalette),
        CLIMB_ALIGN_ZERO(BlinkinRawColors.Solid_Red),
        CLIMB_ALIGN_ONE(BlinkinRawColors.Solid_Yellow),
        CLIMB_ALIGN_TWO(BlinkinRawColors.Solid_Green),

        /*** BARGE PLACE ***/
        /** Placing into Barge - Too close */
        BARGE_PLACE_DANGER(BlinkinRawColors.Solid_Red),
        /** Placing into Barge - Too far */
        BARGE_PLACE_FAR(BlinkinRawColors.Solid_Yellow),
        /** Placing into Barge - Juuuust right */
        BARGE_PLACE_PERFECT(BlinkinRawColors.Solid_Green),

        /*** CORAL AFFECTOR ***/
        /** Picking: robot has no coral but is trying to grab one */
        CORAL_PICKING(BlinkinRawColors.Fixed_HeartbeatWhite),
        /** Placing */
        CORAL_PLACING(BlinkinRawColors.Fixed_LightChaseGray),

        /*** ALGINATOR ***/
        /** Picking: robot has no algae but is tring to grab one */
        ALGAE_PICKING(BlinkinRawColors.Fixed_HeartbeatBlue),
        /** Placing */
        ALGAE_PLACING(BlinkinRawColors.Fixed_LightChaseBlue),

        /*** HOLDING ***/
        HOLDING_ALGAE(BlinkinRawColors.Solid_Violet),
        HOLDING_CORAL(BlinkinRawColors.Solid_Lime),
        ALIGNED(BlinkinRawColors.Solid_Blue),
        ;

        double sparkvalue;
        String prettyName;

        BlinkinColor(double sparkvalue) {
            this.sparkvalue = sparkvalue;
            String name = this.name().toLowerCase();
            this.prettyName = "";
            var cap = true;
            for (int i = 0; i < name.length(); i++) {
                var chr = name.substring(i, i + 1);
                if (chr.equals("_")) {
                    this.prettyName += " ";
                    cap = true;
                } else if (cap) {
                    this.prettyName += chr.toUpperCase();
                    cap = false;
                } else {
                    this.prettyName += chr;
                }
            }
            System.out.println(this.prettyName);
        }
    }

    private static final String COLOR_BLACK = "#000000";
    private static final String COLOR_RED = "#ff0000";
    private static final String COLOR_YELLOW = "#ffff00";
    private static final String COLOR_GREEN = "#00ff00";

    private static final int ledID = 0;

    /** max mm to wall to use CORAL_PICKING_NEAR_WALL color */
    private static final int NEAR_WALL_MM = 40;

    private static final double BARGE_TO_EDGE = 0.4953 + Constants.BOT_WIDTH.in(Meters) / 2;
    private static final double BARGE_UNSAFE_ZONE = BARGE_TO_EDGE + Units.inchesToMeters(8);
    private static final double BARGE_FAR_ZONE = BARGE_TO_EDGE + Units.inchesToMeters(10);

    private Spark spark;
    private NetworkTable blinkinTable;

    private CoralPicker coralPicker;
    private ChamberOfCorals chamberOfCorals;
    private SwerveDrive swerve;
    private Overwatch overwatch;
    private Affector affector;
    private Climber climber;
    private boolean climbing = false;
    private boolean bargeDistanceOn = false;

    public Blinkin() {
        this.spark = new Spark(ledID);

        blinkinTable = NetworkTableInstance.getDefault().getTable("Blinkin");
        blinkinTable.getEntry("Enabled").setBoolean(true);
        blinkinTable.addListener(
                "Enabled",
                EnumSet.of(NetworkTableEvent.Kind.kValueRemote),
                (table, key, value) -> {
                    if (value.valueData.value.isBoolean()) {
                        enabled = value.valueData.value.getBoolean();
                        System.out.println(
                                "[Blinkin] " + (enabled ? "Enabled" : "Disabled") + " from NetworkTables");
                    } else {
                        table.getEntry(key).setBoolean(enabled);
                    }
                });

        Logger.recordOutput("Blinkin/ClimbSensorsColor", COLOR_BLACK);
    }

    @Override
    public void configure() {
        this.coralPicker = Robot.getInstance().coralPicker;
        this.chamberOfCorals = Robot.getInstance().chamberOfCorals;
        this.affector = Robot.getInstance().affector;
        this.overwatch = Robot.getInstance().overwatch;
        this.swerve = Robot.getInstance().swerve;
        this.climber = Robot.getInstance().climber;
    }

    public void setBargeDistanceOn(boolean on) {
        this.bargeDistanceOn = on;
    }

    public void setClimbing(boolean climbing) {
        this.climbing = climbing;
    }

    private boolean enabled = true;
    private BlinkinColor currentColor = BlinkinColor.INIT;
    private Optional<Double> firstFaultTimestamp = Optional.empty();

    private boolean recovering = false;

    public void recover() {
        System.out.println("[Blinkin] Recovery queued");
        recovering = true;
    }

    @Override
    public void log() {
        Logger.recordOutput("Blinkin/ColorName", currentColor.prettyName);
        Logger.recordOutput("Blinkin/ColorValue", currentColor.sparkvalue);
    }

    /**
     * Given a time, alternates between true and false periodically (starting with true)
     *
     * @param t Time
     * @param period Strobe duration; true for half, false for half
     * @return {@code true} if the strobe should be on
     */
    private boolean strobe(double t, double period) {
        return (t % period) < (period / 2);
    }

    @Override
    public void doPeriodic() {
        if (recovering) {
            recovering = false;
            System.out.println("[Blinkin] Recovery activated");
            // can't do this thru the Spark object - have to use JNI instead
            PWMJNI.setPulseTimeMicroseconds(spark.getPwmHandle(), 2125);
            return;
        }
        if (firstFaultTimestamp.isEmpty() && !DeviceHealthManager.isCurrentlyHealthy()) {
            firstFaultTimestamp = Optional.of(Timer.getTimestamp());
        }

        // //var algaeHeld = (
        //     .getCurrentState() == AffectorStates.PICKED_ALGAE
        // );
        // if (!algaeHeld) {
        //     bargeDistanceOn = false;
        // }

        BlinkinColor newColor = BlinkinColor.IDLE;

        double timeSinceFault = Timer.getTimestamp() - firstFaultTimestamp.orElse(0.);
        if (
        // blink for the first subsystem fault
        firstFaultTimestamp.isPresent() && timeSinceFault < 5 && strobe(timeSinceFault, 1.0)) {
            newColor = BlinkinColor.DEVICE_UNHEALTHY;
        } else if (climber.getDesiredPosition() == ClimberPositions.CLIMB) {
            newColor = BlinkinColor.DEEP_CLIMB;
        } else if (climbing) {
            newColor = BlinkinColor.CLIMB_ALIGN_ZERO;
        } else if (bargeDistanceOn) {
            var pose = swerve.getPose();
            double dist = Math.abs(pose.getX() - (FieldConstants.FIELD_LENGTH_METERS / 2));
            if (dist < BARGE_UNSAFE_ZONE) {
                newColor = BlinkinColor.BARGE_PLACE_DANGER;
            } else if (dist > BARGE_FAR_ZONE) {
                newColor = BlinkinColor.BARGE_PLACE_FAR;
            } else {
                newColor = BlinkinColor.BARGE_PLACE_PERFECT;
            }
            Logger.recordOutput(
                    "Blinkin/BargeDistanceColor",
                    switch (newColor) {
                        default -> COLOR_RED;
                        case BARGE_PLACE_FAR -> COLOR_YELLOW;
                        case BARGE_PLACE_PERFECT -> COLOR_GREEN;
                    });
        } else if (swerve.aligned.getAsBoolean()) {
            newColor = BlinkinColor.ALIGNED;
        } else if (coralPicker.getDesiredPosition() == CoralPickerPositions.DEPLOYED) {
            newColor = BlinkinColor.CORAL_PICKING;

        } else {
            switch (affector.getState()) {
                case PICKED_CORAL -> newColor = BlinkinColor.HOLDING_CORAL;
                case PICKED_ALGAE -> newColor = BlinkinColor.HOLDING_ALGAE;
                case PICKING_CORAL -> newColor = BlinkinColor.CORAL_PICKING;
                case PICKING_ALGAE -> newColor = BlinkinColor.ALGAE_PICKING;
                default -> newColor = BlinkinColor.IDLE;
            }
        }
        setColor(newColor);
    }

    private void setColor(BlinkinColor color) {
        currentColor = color;
        if (RobotBase.isReal()) {
            if (enabled) {
                this.spark.set(color.sparkvalue);
            } else {
                this.spark.set(BlinkinRawColors.Fixed_RainbowRainbowPalette);
            }
        }
    }

    @Override
    public boolean getHealth() {
        return true;
    }

    @Override
    public ManualTestGroup createManualTests() {
        return new ManualTestGroup(getName(), new BlinkinColorTest("All colors", spark));
    }
}
