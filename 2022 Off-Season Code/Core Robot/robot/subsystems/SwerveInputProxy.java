package frc.robot.subsystems;

import com.kauailabs.navx.frc.AHRS;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.robot.Robot;
import frc.robot.RobotContainer;
import frc.robot.constants.SwerveConstants;
import org.teamtators.Util.GeneralPIDController;
import org.teamtators.Util.SwerveGyro;
import org.teamtators.Util.SwerveInputSupplier;
import org.teamtators.Util.Vector;
import org.teamtators.sassitator.RobotStateListener;
import org.teamtators.sassitator.Subsystem;

public class SwerveInputProxy extends Subsystem implements RobotStateListener {

    public enum InputType {
        UserInput,
        ComputerInput,
    }

    public enum CentricType {
        FieldCentric,
        HubCentric,
        RightPickerCentric,
        Harvesting,
        ComputerHubCentric
    }

    public enum ComputerCentric {
        FieldCentric,
        HubCentric
    }

    // Hardware
    private final SwerveDrive swerveDrive;
    private final Vector currentPos = new Vector();
    private final RobotContainer robotContainer;
    private final String vision;
    private boolean climberMode = false;

    // ControlType
    private InputType inputType = InputType.UserInput;
    private CentricType centricType = CentricType.FieldCentric;
    private ComputerCentric computerCentric = ComputerCentric.FieldCentric;

    // Motion Controllers
    private final SwerveInputSupplier userInput;
    private SwerveInputSupplier computerInput;
    private final SwerveInput inputObject;
    private double currentDistance;
    private boolean meOverride = false;
    private final SwerveGyro gyro;
    private final GeneralPIDController anglePid = new GeneralPIDController(.05  , 0, 0, 0, 1);
    private Robot.EnumRobotState robotState;
    private final boolean harvestFinish = false;
    private final boolean tempHarvest = false;

    // Constants
    private final double PI = Math.PI;

    public SwerveInputProxy(RobotContainer robot, SwerveDrive swerveDrive, OperatorInterface operatorInterface) {
        super(robot);
        robotContainer = robot;
        vision = getVision();
        this.swerveDrive = swerveDrive;

        userInput = operatorInterface;
        inputObject = new SwerveInput();
        gyro = swerveDrive.getGyro();

        //Shuffleboard stuff:
        putDataOnShuffleboard();
    }

    @Override
    public void doPeriodic() {
        update();
        putDataOnShuffleboard();
    }

    public void putDataOnShuffleboard() {
        SmartDashboard.putString("Centric Type", "" + getCentricType());
        SmartDashboard.putString("Input Type", "" + getInputType());
    }

    public SwerveInput getInput(SwerveInput input) {
        switch (inputType) {
            case UserInput:
                return userInput.get(input);
            case ComputerInput:
                return computerInput.get(input);
            default:
                return new SwerveInput();
        }
    }

    public void stop() {
        swerveDrive.stop();
    }

    public boolean harvestFinish() {
        return harvestFinish;
    }

    public void reset() {
        inputType = InputType.UserInput;
        centricType = CentricType.FieldCentric;
    }

    public SwerveInput handleInput(SwerveInput input) {
        switch (inputType) {
            case UserInput:
                switch (centricType) {
                    case FieldCentric:
                        if (getVisionState() != getEnabledTurret() && !getArizonaControls()) {
                            enableVisionTurret();
                        }
                        return handleFieldCentricInput(input);
                    case HubCentric:
                        if (getVisionState() != getEnabledTurret()) {
                            enableVisionTurret();
                        }
                        return handleHubCentricInput(input);
                    case Harvesting:
                        if (getVisionState() != getEnabledTurret()) {
                            enableVisionTurret();
                        }
                        return handleHarvestMode(input);
                    case RightPickerCentric:
                        return handleRightPickerCentricInput(input);
                    case ComputerHubCentric:
                        return handleComputerHubCentric(input);
                }
            case ComputerInput:
                switch (computerCentric) {
                    case FieldCentric:
                        return handleComputerInput(input);
                    case HubCentric:
                        if (getVisionState() != getEnabledTurret()) {
                            enableVisionTurret();
                        }
                        return handleComputerHubCentric(input);
                }
            default:
                return input;
        }
    }

    public SwerveInput getOutput() {
        return handleInput(getInput(inputObject));
    }

    public void setOverride(boolean override) {
        this.meOverride = override;
    }

    public void update() {
        // Output object
        SwerveInput output;

        // Check to see if we need to give control back over to the user
        anglePid.setkP(.05 * (1 + .4 * swerveDrive.getAverageVector().getMagnitude()));

        if (robotState == Robot.EnumRobotState.Teleop && inputType == InputType.ComputerInput) {
            output = userInput.get(inputObject);
            if (output.hasInput()) {
                setInputType(InputType.UserInput);
            }
        }

        // Get output of suppliers
        output = getOutput();

        // Setting the Movement
        swerveDrive.updateModules(output);
    }

    public SwerveInput handleComputerInput(SwerveInput swerveInput) {
        if (gyro.isConnected()) {
            swerveInput.vector.setTheta((swerveInput.vector.getTheta()) + swerveDrive.getRotation());
            swerveInput.vector.scale(1);
            if (meOverride) {
                swerveInput.override = true;
            }
            return swerveInput;
        }
        swerveInput.vector.setXY(0, 0);
        swerveInput.rotationScalar = 0;
        return swerveInput;
    }

    public SwerveInput handleFieldCentricInput(SwerveInput swerveInput) {
        if (swerveInput.vector.getMagnitude() > 1.0) { // Cannot move faster than max vel, accounts for square nature of joysticks
            swerveInput.vector.setMagnitude(1.0);
        }
        if(robotContainer.getDriverController().getRightBumper()){
            swerveInput.vector.scale(SwerveConstants.SwerveInputProxy.climberMode);
        }else if(climberMode){
            swerveInput.vector.scale(SwerveConstants.SwerveInputProxy.turboModeVelocity);
        }

        else {
            swerveInput.vector.scale(SwerveConstants.SwerveInputProxy.maxVelocity);
        }
        swerveInput.vector.setTheta(swerveInput.vector.getTheta() + swerveDrive.getRotation());
        swerveInput.rotationScalar *= SwerveConstants.SwerveInputProxy.userRotationScalar;
        swerveInput.override = false;
        return swerveInput;
    }

    // Theron's new Hub-centric
    public SwerveInput handleHubCentricInput(SwerveInput swerveInput) {
        swerveInput.vector.scale(SwerveConstants.SwerveInputProxy.maxVelocity);
        swerveInput.vector.setTheta(swerveInput.vector.getTheta() + swerveDrive.getRotation());
        swerveInput.rotationScalar *= SwerveConstants.SwerveInputProxy.userRotationScalar;
        swerveInput.override = false;
        anglePid.reset();
        if (getNumberOfVisionObjects() > 0 && swerveInput.rotationScalar == 0 &&
                getMainShooterState() != getMainShooterShooting() &&
                getBallChannelState() != getBallChannelRapidFire()) {
            anglePid.setCurrentState(getVisionDeltaAngle());
            anglePid.setSetPoint(0);
            swerveInput.rotationScalar = anglePid.getOutput();
        } else {
            if (swerveInput.rotationScalar == 0) {
                swerveInput.rotationScalar = 0;
            }
        }
        return (swerveInput);
    }

    //
    // METHOD SHOULD ONLY BE USED FOR HARVEST MODE !!!!!
    //
    public SwerveInput handleComputerHubCentric(SwerveInput swerveInput) {
        swerveInput.vector.scale(SwerveConstants.SwerveInputProxy.maxVelocity);
        swerveInput.vector.setTheta(swerveInput.vector.getTheta() + swerveDrive.getRotation());
        swerveInput.rotationScalar *= SwerveConstants.SwerveInputProxy.computerRotationScalar;
        swerveInput.override = false;
        anglePid.reset();
        if (getNumberOfVisionObjects() > 0 &&
                getMainShooterState() != getMainShooterShooting() &&
                getBallChannelState() != getBallChannelRapidFire()) {
            anglePid.setCurrentState(getVisionDeltaAngle());
            anglePid.setSetPoint(0);
            swerveInput.rotationScalar = anglePid.getOutput();
        }else {
            swerveInput.rotationScalar = 0;
        }
        anglePid.debug();
        return (swerveInput);
    }

    public SwerveInput handleRightPickerCentricInput(SwerveInput swerveInput) {
        if (swerveInput.vector.getMagnitude() > 1.0) { // Cannot move faster than max vel, accounts for square nature of joysticks
            swerveInput.vector.setMagnitude(1.0);
        }
        swerveInput.vector.scale(SwerveConstants.SwerveInputProxy.maxVelocity);
        swerveInput.vector.setTheta(swerveInput.vector.getTheta() - (Math.PI / 2));
        swerveInput.rotationScalar *= SwerveConstants.SwerveInputProxy.computerRotationScalar;
        return swerveInput;
    }

    public SwerveInput handleHarvestMode(SwerveInput swerveInput) {
        swerveInput.vector.setXY(0, 0);
        swerveInput.rotationScalar = 0;
        return handleComputerHubCentric(swerveInput);
    }

    public void setCentricType(CentricType newType) {
        centricType = newType;
    }

    public void setComputerCentric(ComputerCentric computerCentric) {
        this.computerCentric = computerCentric;
    }

    public CentricType getCentricType() {
        return centricType;
    }

    public ComputerCentric getComputerCentric() {
        return computerCentric;
    }

    public void setComputerInput(SwerveInputSupplier computerInput) {
        this.computerInput = computerInput;
    }

    public void setInputType(InputType controlType) {
        this.inputType = controlType;
    }

    public InputType getInputType() {
        return inputType;
    }

    public SwerveInputSupplier getComputerInputSupplier() {
        return computerInput;
    }

    @Override
    public void onEnterRobotState(Robot.EnumRobotState state) {
        robotState = state;
    }

    public static class SwerveInput {

        public Vector vector;
        public double rotationScalar;
        public Vector point;

        public boolean override = false;

        public SwerveInput() {
            vector = new Vector();
            rotationScalar = 0;
            point = new Vector();
        }

        public void reset() {
            vector.setXY(0, 0);
            point.setXY(0, 0);
            rotationScalar = 0;
        }

        public boolean hasInput() {
            return rotationScalar != 0 || vector.getX() != 0 || vector.getY() != 0;
        }

        @Override
        public String toString() {
            return "SwerveInput{" +
                    "vector=" + vector +
                    ", rotationScalar=" + rotationScalar +
                    '}';
        }
    }

    public void setClimberMode(boolean climberMode) {
        this.climberMode = climberMode;
    }

    public boolean isClimberMode() {
        return climberMode;
    }

    //    DUMMY METHODS
    private String getVision() {
        return "";
    }
    private String getVisionState() {
        return "";
    }
    private String getEnabledTurret() {
        return "";
    }
    private String enableVisionTurret() {
        return "";
    }
    private int getNumberOfVisionObjects() {
        return 0;
    }
    private String getMainShooterState() {
        return "";
    }
    private String getMainShooterShooting() {
        return "";
    }
    private String getBallChannelState() {
        return "";
    }
    private String getBallChannelRapidFire() {
        return "";
    }
    private int getVisionDeltaAngle() {
        return 0;
    }
    private boolean getArizonaControls() {
        return false;
    }
}
