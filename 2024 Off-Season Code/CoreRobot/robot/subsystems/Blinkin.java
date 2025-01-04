package frc.robot.subsystems;

import java.util.EnumSet;
import java.util.Optional;

import org.teamtators.tester.ManualTestGroup;
import org.teamtators.tester.components.BlinkinColorTest;
import org.teamtators.Util.Timer;
import org.teamtators.constants.BlinkinRawColors;
import org.littletonrobotics.junction.Logger;
import org.teamtators.Util.DeviceHealthManager;
import org.teamtators.Util.Subsystem;
import edu.wpi.first.hal.PWMJNI;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEvent;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.RobotBase;
import edu.wpi.first.wpilibj.motorcontrol.Spark;
import frc.robot.Robot;
import frc.robot.subsystems.kingRollers.KingRollers;
import frc.robot.subsystems.picker.Picker;
import frc.robot.subsystems.pivot.Pivot;
import frc.robot.subsystems.pivot.PivotConstants.PivotPositions;
import frc.robot.subsystems.upperNotePath.UpperNotePath;
import frc.robot.subsystems.upperNotePath.UpperNotePath.AmpStates;
import frc.robot.subsystems.upperNotePath.UpperNotePath.UpperNotePathStates;
import frc.robot.subsystems.vision.Vision;

public class Blinkin extends Subsystem {
    // both color 1 and color 2 are free
	enum BlinkinColor {
        /*** GENERAL ***/
        INIT(BlinkinRawColors.Fixed_BreathGray),
		IDLE(BlinkinRawColors.Solid_White),
        HELD(BlinkinRawColors.Solid_Gold),
        VISION_STALE(BlinkinRawColors.Fixed_BeatsPerMinuteLavaPalette),
        DEVICE_UNHEALTHY(BlinkinRawColors.Fixed_LarsonScannerRed),

        /*** PICKER ***/
        /** Picking: robot has no note */
        PICKING(BlinkinRawColors.Fixed_HeartbeatWhite),
        /** Sucking: robot has made contact with a note and is sucking it in */
        SUCKING(BlinkinRawColors.Fixed_StrobeGold),
        PANIC(BlinkinRawColors.Fixed_HeartbeatRed),

        /*** SHOOTING ***/
        SHOOTING_EMPTY(BlinkinRawColors.Fixed_StrobeRed),
        SHOOTING_NORMAL(BlinkinRawColors.Fixed_StrobeBlue),

        /*** AMPING ***/
        AMP_STOWING(BlinkinRawColors.Solid_Blue),
        AMP_DROPPING(BlinkinRawColors.Solid_Red),

        /*** TRAPPING ***/
        TRAP_NORMAL(BlinkinRawColors.Fixed_RainbowRainbowPalette),
        TRAP_EMPTY(BlinkinRawColors.Fixed_RainbowLavePalette),
        ;

        double sparkvalue;
        String prettyName;

        BlinkinColor(double sparkvalue) {
            this.sparkvalue = sparkvalue;
            String name = this.name().toLowerCase();
            this.prettyName = "";
            var cap = true;
            for (int i=0; i < name.length(); i++) {
                var chr = name.substring(i, i+1);
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

    private static final int ledID = 1; 

    private Spark spark;
	private NetworkTable blinkinTable;

    private Picker picker;
    private UpperNotePath unpa;
    private KingRollers king;
    private Vision vision;
    private Pivot pivot;

    public Blinkin() {
        this.spark = new Spark(ledID);

		blinkinTable = NetworkTableInstance.getDefault().getTable("Blinkin");
        blinkinTable.getEntry("Enabled").setBoolean(true);
        blinkinTable.addListener("Enabled", EnumSet.of(NetworkTableEvent.Kind.kValueRemote), (table, key, value) -> {
            if (value.valueData.value.isBoolean()) {
                enabled = value.valueData.value.getBoolean();
                System.out.println("[Blinkin] " + (enabled ? "Enabled" : "Disabled") + " from NetworkTables");
            } else {
                table.getEntry(key).setBoolean(enabled);
            }
        });
    }

    @Override
    public void configure() {
        this.unpa = Robot.getUpperNotePath();
        this.picker = Robot.getPicker();
        this.king = Robot.getKingRollers();
        this.vision = Robot.getVision();
        this.pivot = Robot.getPivot();
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
     * @param t Time
     * @param period Strobe duration; true for half, false for half
     * @return {@code true} if the strobe should be on
     */
    private boolean strobe(double t, double period) {
        return (t % period) < (period/2);
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
        BlinkinColor newColor = BlinkinColor.IDLE;
        boolean holdingAnything = king.noteSensed() || unpa.getDunkerSensor();
        if (firstFaultTimestamp.isEmpty() && !DeviceHealthManager.isCurrentlyHealthy()) {
            firstFaultTimestamp = Optional.of(Timer.getTimestamp());
        }

        double timeSinceFault = Timer.getTimestamp() - firstFaultTimestamp.orElse(0.);
        if (
          // blink for the first subsystem fault
            firstFaultTimestamp.isPresent() && timeSinceFault < 5
            && strobe(timeSinceFault, 1.0)
        ) {
            newColor = BlinkinColor.DEVICE_UNHEALTHY;
        } else if (
          // also blink for vision going out
            vision.anyCameraDisconnected()
            && strobe(Timer.getTimestamp(), 0.5)
        ) {
            newColor = BlinkinColor.VISION_STALE;
        } else {
            var pickerState = picker.getState();
            switch (pickerState) {
                case PICKING:
                    newColor = BlinkinColor.PICKING;
                    break;
                case PULLING: case LEAVING:
                    newColor = BlinkinColor.SUCKING;
                    break;
                case PANIC:
                    newColor = BlinkinColor.PANIC;
                    break;
                default:
                    var upperNotePathState = unpa.getState();
                    if (upperNotePathState == UpperNotePathStates.SHOOTING) {
                        newColor = holdingAnything ? BlinkinColor.SHOOTING_NORMAL : BlinkinColor.SHOOTING_EMPTY;
                    } else if (upperNotePathState == UpperNotePathStates.AMP) {
                        if (pivot.getDesiredPosition() == PivotPositions.AMP) {
                            newColor = BlinkinColor.AMP_DROPPING;
                        } else {
                            newColor = BlinkinColor.AMP_STOWING;
                        }
                    } else if (upperNotePathState == UpperNotePathStates.TRAP) {
                        newColor = holdingAnything ? BlinkinColor.TRAP_NORMAL : BlinkinColor.TRAP_EMPTY;
                    } else if (holdingAnything) {
                        newColor = BlinkinColor.HELD;
                    }
                    break;
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
        } else {
            // only do this in sim cuz its only useful there really
        }
    }

    @Override public void reset() {}
    @Override public boolean getHealth() { return true; }

	@Override
	public ManualTestGroup createManualTests() {
		return new ManualTestGroup(
            getName(),
            // new StateMachineTest<>("Color test", BlinkinColor.class, this, this::setColor),
            new BlinkinColorTest("All colors", spark)
        );
	}

}
