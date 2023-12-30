package frc.robot.subsystems.hardware;

import com.playingwithfusion.TimeOfFlight;
import com.revrobotics.CANSparkMax;
import com.revrobotics.SparkMaxPIDController;
import com.revrobotics.CANSparkMax.ControlType;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;

import common.Util.DigitalSensor;
import common.Util.Timer;
import common.Util.DigitalSensor.Type;
import edu.wpi.first.math.Matrix;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.numbers.N1;
import edu.wpi.first.math.numbers.N2;
import edu.wpi.first.wpilibj.AnalogInput;
import edu.wpi.first.wpilibj.DigitalInput;
import frc.robot.Robot;
import frc.robot.subsystems.Claw;
import frc.robot.RobotContainer.GamePieceTypes;

public class ClawHW {
    public final ClawMotor clawMotor;
    private final TimeOfFlight timeOfFlightSensor;
    private final DigitalSensor breakBeamSensor;
    private ClawSim clawSim;

    public ClawHW() {
        clawMotor = new ClawMotor();
        breakBeamSensor = new DigitalSensor(ClawConfig.breakBeamSensorID, Type.PNP); // make sure type is correct
        timeOfFlightSensor = new TimeOfFlight(ClawConfig.timeOfFlightSensorID);
        clawSim = new ClawSim();
    }

    public void updateSetpoint_(double motorSetpoint) {
        clawMotor.setSetpoint_(motorSetpoint);
        // clawSim.simMoterPower = motorSetpoint;
    }

    public boolean breakBeamTriggered() {
        if (Robot.isSimulation()) {
            return clawSim.getBannerSim();
        }
        return breakBeamSensor.get();
    }

    public TimeOfFlight getTimeOfFlightSensor() {
        return timeOfFlightSensor;
    }

    public void setPower(double power){
        clawMotor.motor.set(.5);   
    }

    public DigitalSensor getBreakBeamSensor() {
        return breakBeamSensor;
    }

    public CANSparkMax getClawMotor() {
        return clawMotor.motor;
    }

    public boolean getConeSensedCurrent() {
        // if (Robot.isSimulation()) {
        //     return clawSim.getConeThresholdSim();
        // }
        double current = clawMotor.motor.getOutputCurrent(); 
        return current > ClawConfig.kMotorConeThreshold;
    }

    public double getTimeOfFlightSensorDistance() {
        return timeOfFlightSensor.getRange() * ClawConfig.kTofConversion;
    }

    public double getPosition(){
        return clawMotor.motor.getEncoder().getPosition();
    }

    public double getCurrent(){
        return clawMotor.motor.getOutputCurrent();
    }
    // public Matrix<N2, N1> computeChangeOfStateEuler(Matrix<N2, N1> x, double
    // angleSetpoint,
    // double dt) {
    // return;
    // }

    class ClawMotor {
        private final CANSparkMax motor;

        public ClawMotor() {
            motor = new CANSparkMax(ClawConfig.clawMotorID, MotorType.kBrushless);
        }

        public void setSetpoint_(double motorPower) {
            motor.set(motorPower);
        }

    }

    public double getMotorSpeed() {
        return clawMotor.motor.get();
    }

    public void update_() {
        clawSim.runSim();
    }

    public void clearSim() {
        clawSim.clearSim();
    }

    public static class ClawSim {
        private double simMoterPower;
        private boolean simCone;
        private boolean simCube;
        private Timer simTimer;
        private int simArrInt;
        private GamePieceTypes[] pieces = { GamePieceTypes.CONE, GamePieceTypes.CUBE, GamePieceTypes.CONE,
                GamePieceTypes.CUBE, GamePieceTypes.CONE, GamePieceTypes.CUBE }; // 0 is cube, 1 is cone

        public ClawSim() {
            simMoterPower = 0;
            simCone = false;
            simCube = false;
            simArrInt = 0;
            simTimer = new Timer();
        }

        public void runSim() {
            if (simMoterPower > .1 && !simCube && !simCone) {
                if (!simTimer.isRunning()) {
                    simTimer.start();
                } else if (simTimer.get() > 5) {
                    if (pieces[simArrInt] == GamePieceTypes.CUBE) {
                        simCube = true;
                        simCone = false;
                    } else {
                        simCone = true;
                        simCube = false;
                    }
                    simArrInt++;
                }
            }
        }

        public void clearSim() {
            simTimer = new Timer();
            simCone = false;
            simCube = false;
        }

        public boolean getBannerSim() {
            return simCube;
        }

        public boolean getConeThresholdSim() {
            return simCone;
        }
    }

    // All of the following units are SI
    public static class ClawConfig {
        // public static final double wristMOI = 1;

        public static final double kPRot = .1;
        public static final double kIRot = 0;
        public static final double kDRot = 0;

        public static final PIDController rotationController = new PIDController(kPRot, kIRot, kDRot);
        public static final int clawMotorID = 60;
        public static final int timeOfFlightSensorID = 0;
        public static final int breakBeamSensorID = 10;

        public static final double kTimeOfFlightConeInThingThreshold = 485; // made up

        public static final double kSuckConeInSpeed = 1; 
        public static final double kSuckCubeInSpeed = -.6; 
        public static final double kSpitOutSpeed = -0.3; 
        public static final double kHoldConePower = 0.1;
        public static final double kHoldCubePower = -0.1;


        public static final double kMotorConeThreshold = 30;
        public static final double kTofConversion = 1;

    }
}
