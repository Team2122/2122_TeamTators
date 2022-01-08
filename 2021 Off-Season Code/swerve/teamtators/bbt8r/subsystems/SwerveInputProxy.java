package org.teamtators.bbt8r.subsystems;

import org.teamtators.bbt8r.SwerveInputSupplier;
import org.teamtators.bbt8r.TatorRobot;
import org.teamtators.bbt8r.staging.Vector;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.RobotState;
import org.teamtators.common.scheduler.RobotStateListener;
import org.teamtators.common.scheduler.Subsystem;

public class SwerveInputProxy extends Subsystem implements RobotStateListener, Configurable<SwerveInputProxy.Config> {

    public enum InputType {
        UserInput,
        ComputerInput,
    }

    private Config config;

    // Hardware
    private SwerveDrive swerveDrive;

    // ControlType
    private RobotState robotState = RobotState.DISABLED;
    private InputType inputType = InputType.UserInput;

    // Motion Controllers
    private SwerveInputSupplier userInput;
    private SwerveInputSupplier computerInput;
    private SwerveInput inputObject;

    // Constants
    private boolean field_centric_mode = true;
    private final double PI = Math.PI;

    public SwerveInputProxy(TatorRobot robot, SwerveDrive swerveDrive, OperatorInterface operatorInterface) {
        super("SwerveInputProxy");

//        this.swerveDrive = robot.getSubsystems().getSwerveDrive();
        this.swerveDrive = swerveDrive;

        robot.getScheduler().registerStateListener(this);

//        userInput = robot.getSubsystems().getOi();
        userInput = operatorInterface;
        inputObject = new SwerveInput();
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

    public SwerveInput handleInput(SwerveInput input) {
//        logger.info("Raw Input: " + input);
        switch (inputType) {
            case UserInput:
                return handleTeleopInput(input);
            case ComputerInput:
                return handleComputerInput(input);
            default:
                return input;
        }
    }

    public SwerveInput getOutput() {
        return handleInput(getInput(inputObject));
    }

    public void update() {
        // Output object
        SwerveInput output;

        // Check to see if we need to give control back over to the user
        if (robotState == RobotState.TELEOP && inputType == InputType.ComputerInput) {
            output = userInput.get(inputObject);
            if (output.hasInput()) {
                setInputType(InputType.UserInput);
            }
        }

        // Get output of suppliers
        output = getOutput();

//        logger.info("InputProxy Output: " + output);

        // Setting the Movement
        swerveDrive.updateModules(output);
    }

    public SwerveInput handleTeleopInput(SwerveInput swerveInput) {

//        System.out.println("Swerve Input: " + swerveInput.vector);

        swerveInput.vector.scale(config.maxVelocity);

        swerveInput.rotationScalar *= config.userRotationScalar;

        return swerveInput;
    }

    public SwerveInput handleComputerInput(SwerveInput swerveInput) {
        SwerveInput bob = handleTeleopInput(swerveInput);
        bob.vector.scale(.4);
//        bob.vector.scaleY(-1);

        return bob;
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

    @Override
    public void onEnterRobotState(RobotState state) {
        robotState = state;
    }

    public static class SwerveInput {

        public Vector vector;
        public double rotationScalar;

        public SwerveInput() {
            vector = new Vector();
            rotationScalar = 0;
        }

        public void reset() {
            vector.setXY(0, 0);
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

    @Override
    public void configure(Config config) {
        this.config = config;
    }

    public static class Config {
        public double maxVelocity = 5; // IPM   if you ever see this, so I was looking at the swerve modules Mark bought and they have a max velocity of 14.5 ft/sec on the neo version and 16.3 on the falcon version :)
        public double userRotationScalar;
    }

}
