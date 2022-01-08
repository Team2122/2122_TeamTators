package org.teamtators.bbt8r.commands;

import SplineGenerator.Applied.MotionController;
import SplineGenerator.Util.DVector;
import org.teamtators.bbt8r.SwerveInputSupplier;
import org.teamtators.bbt8r.TatorRobot;
import org.teamtators.bbt8r.subsystems.SwerveDrive;
import org.teamtators.bbt8r.subsystems.SwerveInputProxy;
import org.teamtators.common.SG.SGControllerRegistry;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;

public class SetDriveComputerInput extends Command implements Configurable<SetDriveComputerInput.Config> {

    private Config config;
    private SwerveInputProxy swerveInputProxy;
    private SGControllerRegistry controllerRegistry;

    public SetDriveComputerInput(TatorRobot robot) {
        super("SetDriveComputerInput");
        swerveInputProxy = robot.getSubsystems().getInputProxy();
        controllerRegistry = robot.getSGControllerRegistry();
    }

    @Override
    public boolean step() {

        logger.info("Setting Auto Stuff");

        MotionController controller = controllerRegistry.getController(config.controllerName);

        if (controller == null) {
            logger.error("Failed To Find Controller with Name: " + config.controllerName);
            return true;
        }

        SwerveInputSupplier supplier = input -> {
            controller.update();
            DVector motion;
//            if(!controller.isFinished()){
                motion = controller.getMotion();
//            } else {
//                motion = new DVector(0,0);
//                swerveInputProxy.stop();
//            }
            input.vector.setXY(motion.get(0), motion.get(1));
            input.rotationScalar = 0;
            logger.info(config.controllerName + " motion: " + motion);
            return input;
        };

        swerveInputProxy.setComputerInput(supplier);

        if (config.enterCompControlled) {
            swerveInputProxy.setInputType(SwerveInputProxy.InputType.ComputerInput);
        }

        return true;
    }

    @Override
    public void configure(Config config) {
        this.config = config;
    }

    public static class Config {
        public String controllerName;
        public boolean enterCompControlled = false;
    }
}
