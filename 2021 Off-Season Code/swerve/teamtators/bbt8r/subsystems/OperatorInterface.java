package org.teamtators.bbt8r.subsystems;

import edu.wpi.first.wpilibj.Compressor;
import org.teamtators.bbt8r.SwerveDriveInputSupplier;
import org.teamtators.bbt8r.SwerveInputSupplier;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.config.helpers.CompressorConfig;
import org.teamtators.common.controllers.Controller;
import org.teamtators.common.controllers.LogitechF310;
import org.teamtators.common.scheduler.Subsystem;
import org.teamtators.common.tester.ManualTest;
import org.teamtators.common.tester.ManualTestGroup;
import org.teamtators.common.tester.components.CompressorTest;
import org.teamtators.common.util.JoystickModifiers;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class OperatorInterface extends Subsystem implements Configurable<OperatorInterface.Config>, Supplier <SwerveDriveInputSupplier.SwerveDriveInput>, SwerveInputSupplier {
    private LogitechF310 driverJoystick = new LogitechF310("driver");
    private LogitechF310 gunnerJoystick = new LogitechF310("gunner");
//    private ButtonBoardFingers gunnerJoystick = new ButtonBoardFingers("gunner");
//    private RawController gunnerSecondary = new RawController("gunnerSecondary");
    private List<Controller<?, ?>> controllers;
    private Config config;
    private Compressor compressor;

    public OperatorInterface() {
        super("Operator Interface");
    }

    public double getDriveLeft() { // Return the left joystick value and apply drive modifiers
        return -config.driverModifiers.apply(driverJoystick.getAxisValue(LogitechF310.Axis.LEFT_STICK_Y));
    }

    public double getDriveRight() { // Return the left joystick value and apply drive modifiers
        return -config.driverModifiers.apply(driverJoystick.getAxisValue(LogitechF310.Axis.RIGHT_STICK_Y));
    }

    public double getSwerveDriveLeft() { // Return the left joystick value and apply drive modifiers
        return -config.driverModifiers.apply(driverJoystick.getAxisValue(LogitechF310.Axis.LEFT_STICK_X));
    }

    public double[] getSwerveDriveRight() { // Return the left joystick value and apply drive modifiers
        return new double[]{config.driverModifiers.apply(driverJoystick.getAxisValue(LogitechF310.Axis.RIGHT_STICK_X)), -config.driverModifiers.apply(driverJoystick.getAxisValue(LogitechF310.Axis.RIGHT_STICK_Y))};
    }

    public double getLeftHorizontal() { // Return the left joystick value and apply drive modifiers
        return -config.driverModifiers.apply(driverJoystick.getAxisValue(LogitechF310.Axis.LEFT_STICK_X));
    }


    public double getTrigger(LogitechF310.Axis axis) { // Method for getting the axis value of a trigger
        return driverJoystick.getAxisValue(axis);
    }

    @Override
    public void configure(Config config) {
        super.configure();
        this.config = config;
        driverJoystick.configure(config.driverJoystick);
        gunnerJoystick.configure(config.gunnerJoystick);
//        gunnerSecondary.configure(config.gunnerSecondary);
        compressor = config.compressor.create();

        controllers = Arrays.asList(
                driverJoystick,
                gunnerJoystick
//                gunnerSecondary
        );
    }

    public LogitechF310 getDriverJoystick() { // Returns the object representing the drive controller
        return driverJoystick;
    }

    public List<Controller<?, ?>> getAllControllers() { // Returns a list of the controllers
        return controllers;
    }

    @Override
    public ManualTestGroup createManualTests() {
        ManualTestGroup group = super.createManualTests();
        group.addTest(new OITest());
        group.addTest(new CompressorTest(compressor));
        return group;
    }

    public void disableCompressor() { // Turn off the compressor
        compressor.setClosedLoopControl(false);
        compressor.stop();
    }

    public void toggleCompressor() { // Toggle the compessor
        compressor.setClosedLoopControl(!compressor.getClosedLoopControl());
    }

    public void toggleCompressorAndPrint() { // Toggle the compressor and print out that you have toggled it
        compressor.setClosedLoopControl(!compressor.getClosedLoopControl());
        logger.info("Compressor: {}", compressor.getClosedLoopControl());
    }

    public boolean isRightBumperPressed() { // true if the right bumper is pressed
        return driverJoystick.isButtonPressed(LogitechF310.Button.BUMPER_RIGHT);
    }

    public boolean isRightBumperHeld(){ // true of the right bumper is held
        return driverJoystick.isButtonDown(LogitechF310.Button.BUMPER_RIGHT);
    }


    public void enableCompressor() { // turns the compressor on
        compressor.setClosedLoopControl(true);
        compressor.start();
    }

    public LogitechF310 getGunnerJoystick() { // Returns the object representing the gunner controller
        return gunnerJoystick;
    }

    private SwerveDriveInputSupplier.SwerveDriveInput input = new SwerveDriveInputSupplier.SwerveDriveInput(new double[]{0,0} , 0);
    @Override
    public SwerveDriveInputSupplier.SwerveDriveInput get() { // Returns the value of the gunner joysticks after bring modified by the drive modifiers
//        double trigTotal = -driverJoystick.getAxisValue(LogitechF310.Axis.LEFT_TRIGGER) + driverJoystick.getAxisValue(LogitechF310.Axis.RIGHT_TRIGGER);
//        input.set(trigTotal + getDriveRight(), trigTotal + getDriveLeft());
        input.set(getSwerveDriveRight(), getDriveLeft());
        return input;
    }

    @Override
    public SwerveInputProxy.SwerveInput get(SwerveInputProxy.SwerveInput input) {
        double[] xy = getSwerveDriveRight();
        input.vector.setXY(xy[0], xy[1]);
        input.rotationScalar = getLeftHorizontal();
        return input;
    }

    public static class Config {
        public LogitechF310.Config driverJoystick;
        public LogitechF310.Config gunnerJoystick;
//        public ButtonBoardFingers.Config gunnerJoystick;
//        public RawController.Config gunnerSecondary;

        public JoystickModifiers driverModifiers;
        public CompressorConfig compressor;
    }

    private class OITest extends ManualTest {
        public OITest() {
            super("OITest");
        }

        @Override
        public void start() {
            printTestInstructions("Press A to get statuses");
        }

        @Override
        public void onButtonDown(LogitechF310.Button button) {
            printTestInfo("Tank: Left = {}, Right = {}", getDriveLeft(), getDriveRight());
            printTestInfo("Raw: Left = {}, Right = {}", driverJoystick.getAxisValue(LogitechF310.Axis.LEFT_STICK_Y), driverJoystick.getAxisValue(LogitechF310.Axis.RIGHT_STICK_Y));
            printTestInfo("Gunner: L = {}, R = {}", gunnerJoystick.getAxisValue(LogitechF310.Axis.LEFT_STICK_Y), gunnerJoystick.getAxisValue(LogitechF310.Axis.RIGHT_STICK_Y));
        }
    }
}

