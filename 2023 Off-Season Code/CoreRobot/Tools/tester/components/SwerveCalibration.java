//package teamtators.Tools.tester.components;
//
//import teamtators.Controllers.XBOXController;
//import teamtators.Tools.tester.ManualTest;
//
//import javax.swing.plaf.nimbus.State;
//import java.util.Arrays;
//import java.util.Scanner;
//
//public class SwerveCalibration extends ManualTest {
//    private SwerveModule[] swerveModules;
//    private int currentWheelBeingUsed = 0;
//
//    private Double[] offsets = new Double[4];
//
//    private enum States {
//        GETTING_OFFSETS,
//        RUNNING_WHEEL_FOR_OFFSET,
//        RUNNING_A_WHEEL,
//        WAIT_TO_RUN_WHEELS,
//        IS_DONE_RUNNING,
//        WAIT_TO_START_CALIBRATION,
//    }
//
//    private States currentState = States.WAIT_TO_START_CALIBRATION;
//
//    public SwerveCalibration(String name, SwerveModule[] swerveModules) {
//        super("Swerve Calibration");
//        this.swerveModules = swerveModules;
//    }
//
//    public void start() {
//        logger.info("Flip the robot on its side and make sure to rest it on a sturdy part of the robot"+
//                "(elevate it using a battery or something else sturdy). Then, when I tell you to, use the swerve calibration tool" +
//                "-a sheet that is around 4 feet long and has two wheel cutouts- to align each of the wheels. You will align the wheel" +
//                "by putting the tool onto the wheel and making the tool parallel to the robot. Since there are four wheels you will have to do this four times" +
//                "Also I will start spinning wheels for the calibration process, so once you are ready hit the 'RIGHT_TRIGGER' button");
//    }
//
//    private void updateOffset() {
//        double encoderValue = swerveModules[currentWheelBeingUsed].getAbsoluteEncoder().get();
//        offsets[currentWheelBeingUsed] = encoderValue;
//
//        currentWheelBeingUsed++;
//        stopPreviousModule();
//
//        if (getAllWheelsHaveBeenRun()) {
//            // Once we are done with updating the offsets the user should be notified what to do next
//            logger.info("Press X to start running the wheels");
//            currentWheelBeingUsed = 0;
//            currentState = States.WAIT_TO_RUN_WHEELS;
//        }
//
//        else {
//            runModule(currentWheelBeingUsed);
//        }
//
//    }
//
//    private void saveData() {
//        String fullOffsetString = "public static double[] SWERVE_MODULE_OFFSETS = { ";
//        for (var i = 0; i < offsets.length; i++) {
//            fullOffsetString += offsets[i] * -1; // Have to multiply by -1 to make sure the offset is correct
//
//            // The last item should not have a comma after it
//            if (i < offsets.length - 1) {
//                fullOffsetString += ", ";
//            }
//        }
//        fullOffsetString += "};";
//
//        String originalLine = "public static double[] SWERVE_MODULE_OFFSETS = {number1, number2, number3, number4};";
//        logger.info("Replace the line that looks like: " + originalLine + " with the line below. The line will be in the SwerveConstants.java file (under frc/robot/constants) \n\n" + fullOffsetString);
//
//    }
//    private void updateWheelRotationDirection(boolean isMovingForwards) {
//        if (!isMovingForwards) {
//            offsets[currentWheelBeingUsed] += .5; // Inverts the module
//        }
//        currentWheelBeingUsed++;
//
//        if (getAllWheelsHaveBeenRun()) {
//            saveData();
//            currentState = States.IS_DONE_RUNNING;
//        }
//
//        else {
//            runModule(currentWheelBeingUsed);
//            logger.info("Which direction is Wheel " + currentWheelBeingUsed + " rotating? Hit 'Left Bumper' for forwards and 'Right Bumper' for backwards.");
//        }
//
//        stopPreviousModule();
//    }
//
//    private void displayWheelRotationDirectionInfo() {
//        logger.info("Now I will run all the wheels in a sequential order and I will ask you to which direction the wheel is rotating." +
//                "I will ask if it is Clockwise or Counter-Clockwise (it is clockwise if your hand is being pushed towards the front of the robot). " +
//                "Once you tell me that I will start running the next wheel until all wheels are done.");
//    }
//
//    @Override
//    public void onButtonDown(XBOXController.Button button) {
//        switch(button) {
//            case kA:
//                if (currentState == States.GETTING_OFFSETS) {
//                    updateOffset();
//                }
//
//                else if (currentState == States.RUNNING_WHEEL_FOR_OFFSET) {
//                    currentState = States.GETTING_OFFSETS;
//                    stopModule(currentWheelBeingUsed);
//                    logger.info("Once you are done aligning the wheel hit the 'A' button");
//                }
//                break;
//            case kBUMPER_LEFT:
//                if (currentState == States.RUNNING_A_WHEEL) {
//                    updateWheelRotationDirection(true);
//                }
//                break;
//
//            case kBUMPER_RIGHT:
//                if (currentState == States.RUNNING_A_WHEEL) {
//                    updateWheelRotationDirection(false);
//                }
//                break;
//
//            case kX:
//                if (currentState == States.WAIT_TO_RUN_WHEELS) {
//                    currentState = States.RUNNING_A_WHEEL;
//                    displayWheelRotationDirectionInfo();
//                    runModule(0);
//                }
//                break;
//
//            case kTRIGGER_RIGHT:
//                if (currentState == States.WAIT_TO_START_CALIBRATION) {
//                    startCalibration();
//                }
//                break;
//        }
//
//    }
//
//    private void startCalibration() {
//        logger.info("I am going to spin the wheel you have to align and once you know which one you have to align hit 'A'");
//        runModule(0);
//        currentState = States.RUNNING_WHEEL_FOR_OFFSET;
//    }
//
//    private void runModule(int moduleNumber) {
//        swerveModules[moduleNumber].setMovementMotor(.1);
//    }
//
//    private void stopModule(int moduleNumber) {
//        if (moduleNumber >= 0 && moduleNumber <= 3) {
//            swerveModules[moduleNumber].setMovementMotor(0);
//        }
//    }
//
//    private boolean getAllWheelsHaveBeenRun() {
//        return currentWheelBeingUsed > 3;
//    }
//
//    private void stopPreviousModule() {
//        int previousWheelBeingUsed = currentWheelBeingUsed - 1;
//        stopModule(previousWheelBeingUsed);
//    }
//}
