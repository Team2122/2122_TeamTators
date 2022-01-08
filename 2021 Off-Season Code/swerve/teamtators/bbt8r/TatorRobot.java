package org.teamtators.bbt8r;

import SplineGenerator.Applied.*;
import SplineGenerator.Applied.LegacyVersions.OldVelocityController;
import SplineGenerator.Applied.SegmenterComplexVelocityController;
import SplineGenerator.GUI.BallVelocityDirectionController;
import SplineGenerator.Splines.PolynomicSpline;
import SplineGenerator.Splines.Spline;
import SplineGenerator.Util.*;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import org.teamtators.common.SG.SGControllerRegistry;
import org.teamtators.common.SubsystemsBase;
import org.teamtators.common.TatorRobotBase;
import org.teamtators.common.config.ConfigCommandStore;
import org.teamtators.common.scheduler.Command;
import org.teamtators.bbt8r.commands.CommandRegistrar;
import org.teamtators.bbt8r.subsystems.Subsystems;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TatorRobot extends TatorRobotBase {
    private Subsystems subsystems;
    private final CommandRegistrar registrar;
    private SendableChooser<String> autoChooser;

    private SGControllerRegistry controllerRegistry;

    public TatorRobot(String configDir) {
        super(configDir);
        registrar = new CommandRegistrar(this);
        subsystems = new Subsystems(this);
        controllerRegistry = new SGControllerRegistry();
        initializeControllers();

        getSubsystems().getInputProxy().setComputerInput(input -> {
            input.reset();
            return input;
        });

    }

    @Override
    public SubsystemsBase getSubsystemsBase() {
        return subsystems;
    }

    public Subsystems getSubsystems() {
        return subsystems;
    }

    public SGControllerRegistry getSGControllerRegistry() {
        return controllerRegistry;
    }

    public void initializeControllers() {
//        addBasicForwardPath();
        addFigurePath();
//        addBasicCurve();
//        addBasicishCurve();
    }

    public void addBasicishCurve() {
        PolynomicSpline spline = new PolynomicSpline(2);
        spline.addControlPoint(new DControlPoint(new DVector(0, 0), new DDirection(1, 1), new DDirection(0, 0)));
        spline.addControlPoint(new DControlPoint(new DVector(1, 1), new DDirection(1, 0)));
        spline.addControlPoint(new DControlPoint(new DVector(2, 0), new DDirection(1, 0)));
        spline.addControlPoint(new DControlPoint(new DVector(1, -1), new DDirection(1, 0)));
        spline.addControlPoint(new DControlPoint(new DVector(0, 0), new DDirection(1, 0)));
        spline.addControlPoint(new DControlPoint(new DVector(-1, 1), new DDirection(1, 0)));
        spline.addControlPoint(new DControlPoint(new DVector(-2, 0), new DDirection(1, 0)));
        spline.addControlPoint(new DControlPoint(new DVector(-1, -1), new DDirection(1, 0)));
        spline.addControlPoint(new DControlPoint(new DVector(0, 0), new DDirection(0, 0), new DDirection(0, 0)));
        spline.setPolynomicOrder(5);
        spline.closed = false;
        InterpolationInfo c1 = new InterpolationInfo();
        c1.interpolationType = Spline.InterpolationType.Linked;
        c1.endBehavior = Spline.EndBehavior.Hermite;
        spline.interpolationTypes.add(c1);
        InterpolationInfo c2 = new InterpolationInfo();
        c2.interpolationType = Spline.InterpolationType.Linked;
        c2.endBehavior = Spline.EndBehavior.Hermite;
        spline.interpolationTypes.add(c2);
        InterpolationInfo c3 = new InterpolationInfo();
        c3.interpolationType = Spline.InterpolationType.Linked;
        c3.endBehavior = Spline.EndBehavior.None;
        spline.interpolationTypes.add(c3);
        InterpolationInfo c4 = new InterpolationInfo();
        c4.interpolationType = Spline.InterpolationType.Linked;
        c4.endBehavior = Spline.EndBehavior.None;
        spline.interpolationTypes.add(c4);
        spline.generate();
        spline.takeNextDerivative();
        SplineVelocityController splineVelocityController = new SplineVelocityController(spline, 1.7D, 1.0D, 0.0D, 0.2D, 0.2D);
        ArcLengthConverter arcLengthConverter = new ArcLengthConverter(spline, 0.02D, splineVelocityController);
        arcLengthConverter.computeMovement();
        arcLengthConverter.addEndVelocity(0.0, .05);
        ArcLengthConverter.Controller controller = arcLengthConverter.getController();

        MotionController motionController = new MotionController(arcLengthConverter.getController(), controller, () -> new DPoint(subsystems.getSwerveDrive().getArcLength(),0));
        controllerRegistry.addController("eddie", motionController);
    }

    public void addBasicCurve() {
        PolynomicSpline spline = new PolynomicSpline(2);
        spline.addControlPoint(new DControlPoint(new DVector(0, 0), new DVector(5, 0), new DDirection(0, 0)));
        spline.addControlPoint(new DControlPoint(new DVector(4, 2)));
        spline.addControlPoint(new DControlPoint(new DVector(8, 0), new DDirection(1, 0), new DDirection(0, 0)));

        spline.setPolynomicOrder(5);
        spline.closed = false;

        InterpolationInfo c1 = new InterpolationInfo();
        c1.interpolationType = Spline.InterpolationType.Linked;
        c1.endBehavior = Spline.EndBehavior.Hermite;
        spline.interpolationTypes.add(c1);

        InterpolationInfo c2 = new InterpolationInfo();
        c2.interpolationType = Spline.InterpolationType.Linked;
        c2.endBehavior = Spline.EndBehavior.Hermite;
        spline.interpolationTypes.add(c2);

        InterpolationInfo c3 = new InterpolationInfo();
        c3.interpolationType = Spline.InterpolationType.Linked;
        c3.endBehavior = Spline.EndBehavior.None;
        spline.interpolationTypes.add(c3);

        InterpolationInfo c4 = new InterpolationInfo();
        c4.interpolationType = Spline.InterpolationType.Linked;
        c4.endBehavior = Spline.EndBehavior.None;
        spline.interpolationTypes.add(c4);

        spline.generate();
        spline.takeNextDerivative();

        Function<DVector, DVector> derivativeModifier = variable -> {
            variable.setMagnitude(10);
            return variable;
        };

        Function<DVector, DVector> distanceModifier = variable -> {
            variable.multiplyAll(35.001);
            return variable;
        };

        Segmenter segmenter = new Segmenter(spline, derivativeModifier, distanceModifier);
        segmenter.bounds = new Extrema(new DPoint(-4, -4), new DPoint(13, 5));
        segmenter.followerStep = .1;
        segmenter.onPathRadius = 1;

        ExecutorService executorService = Executors.newFixedThreadPool(4);
        Runnable[] runnables = segmenter.getRunnablePieces(4);
        Future<?>[] futures = new Future[runnables.length];

        for (int i = 0; i < runnables.length; i++) {
            futures[i] = executorService.submit(runnables[i]);
        }

        executorService.shutdown();

        boolean completed = false;
        while (!completed) {
            boolean allDone = true;
            for (int i = 0; i < futures.length; i++) {
                if (!futures[i].isDone()) {
                    allDone = false;
                }
            }
            completed = allDone;
        }

        Segmenter.Controller navigatorController = segmenter.getController();
        navigatorController.distFinishedThresh = .1;
        OldVelocityController velocityController = new SegmenterComplexVelocityController(navigatorController, 1.7, 1, 0, .2, .2);

        controllerRegistry.addController("BasicCurve", new MotionController(navigatorController, velocityController, subsystems.getSwerveDrive()::getPoint));
    }

    public void addBasicForwardPath() {
        PolynomicSpline spline = new PolynomicSpline(2);

        spline.addControlPoint(new DControlPoint(new DVector(0, 0), new DDirection(1, 0)));
        spline.addControlPoint(new DControlPoint(new DVector(10, 0), new DDirection(1, 0)));

        spline.closed = false;
        spline.setPolynomicOrder(3);

        InterpolationInfo c1 = new InterpolationInfo();
        c1.interpolationType = Spline.InterpolationType.Linked;
        c1.endBehavior = Spline.EndBehavior.Hermite;
        spline.interpolationTypes.add(c1);

        spline.generate();
        spline.takeNextDerivative();

        Function<DVector, DVector> derivativeModifier = variable -> {
            variable.setMagnitude(10);
            return variable;
        };

        Function<DVector, DVector> distanceModifier = variable -> {
            variable.multiplyAll(6);
            return variable;
        };

        Segmenter segmenter = new Segmenter(spline, derivativeModifier, distanceModifier);
        segmenter.bounds = new Extrema(new DPoint(-2, -2), new DPoint(12, 2));

        segmenter.computeGradient();

        Segmenter.Controller navigatorController = segmenter.getController();
        OldVelocityController velocityController = new SegmenterComplexVelocityController(navigatorController, 5000, 1000, 0, .4, .1);
        controllerRegistry.addController("BasicForward", new MotionController(navigatorController, velocityController, subsystems.getSwerveDrive()::getPoint));
    }

    private void addFigurePath() {
        PolynomicSpline spline = new PolynomicSpline(2);

        spline.addControlPoint(new DControlPoint(new DVector(0, 0), new DDirection(1, 1), new DDirection(0, 0)));
        spline.addControlPoint(new DControlPoint(new DVector(1, 1), new DDirection(1, 0)));
        spline.addControlPoint(new DControlPoint(new DVector(2, 0), new DDirection(1, 0)));
        spline.addControlPoint(new DControlPoint(new DVector(1, -1), new DDirection(1, 0)));
        spline.addControlPoint(new DControlPoint(new DVector(0, 0), new DDirection(1, 0)));
        spline.addControlPoint(new DControlPoint(new DVector(-1, 1), new DDirection(1, 0)));
        spline.addControlPoint(new DControlPoint(new DVector(-2, 0), new DDirection(1, 0)));
        spline.addControlPoint(new DControlPoint(new DVector(-1, -1), new DDirection(1, 0)));
        spline.addControlPoint(new DControlPoint(new DVector(0, 0), new DDirection(0, 0), new DDirection(0, 0)));

        spline.setPolynomicOrder(5);
        spline.closed = false;

        InterpolationInfo c1 = new InterpolationInfo();
        c1.interpolationType = Spline.InterpolationType.Linked;
        c1.endBehavior = Spline.EndBehavior.Hermite;
        spline.interpolationTypes.add(c1);

        InterpolationInfo c2 = new InterpolationInfo();
        c2.interpolationType = Spline.InterpolationType.Linked;
        c2.endBehavior = Spline.EndBehavior.Hermite;
        spline.interpolationTypes.add(c2);

        InterpolationInfo c3 = new InterpolationInfo();
        c3.interpolationType = Spline.InterpolationType.Linked;
        c3.endBehavior = Spline.EndBehavior.None;
        spline.interpolationTypes.add(c3);

        InterpolationInfo c4 = new InterpolationInfo();
        c4.interpolationType = Spline.InterpolationType.Linked;
        c4.endBehavior = Spline.EndBehavior.None;
        spline.interpolationTypes.add(c4);

        spline.generate();
        spline.takeNextDerivative();

        Function<DVector, DVector> derivativeModifier = variable -> {
            variable.setMagnitude(10);
            return variable;
        };

        Function<DVector, DVector> distanceModifier = variable -> {
            variable.multiplyAll(35.001);
            return variable;
        };

        Segmenter segmenter = new Segmenter(spline, derivativeModifier, distanceModifier);
        segmenter.bounds = new Extrema(new DPoint(-5, -5), new DPoint(5, 5));
        segmenter.followerStep = .05;
        segmenter.onPathRadius = .2;

        ExecutorService executorService = Executors.newFixedThreadPool(4);
        Runnable[] runnables = segmenter.getRunnablePieces(4);
        Future<?>[] futures = new Future[runnables.length];

        for (int i = 0; i < runnables.length; i++) {
            futures[i] = executorService.submit(runnables[i]);
        }

        executorService.shutdown();

        boolean completed = false;
        while (!completed) {
            boolean allDone = true;
            for (int i = 0; i < futures.length; i++) {
                if (!futures[i].isDone()) {
                    allDone = false;
                }
            }
            completed = allDone;
        }


        Segmenter.Controller navigatorController = segmenter.getController();
        navigatorController.distFinishedThresh = .1;
        OldVelocityController velocityController = new SegmenterComplexVelocityController(navigatorController, 1.7, 1, 0, .2, .2);

        BallVelocityDirectionController ball = new BallVelocityDirectionController(navigatorController, new DPoint(0, 0));
        ball.velocityController = velocityController;
        controllerRegistry.addController("FigureEight", new MotionController(navigatorController, velocityController, subsystems.getSwerveDrive()::getPoint));
    }

    @Override
    @SuppressWarnings("removal")
    protected void registerCommands(ConfigCommandStore commandStore) {
        super.registerCommands(commandStore);
        registrar.register(commandStore);

        autoChooser = new SendableChooser<>();
//        autoChooser.setDefaultOption("NoAuto", "NoAuto");
//        autoChooser.addOption("BasicStraight", "$BasicStraight");

        autoChooser.setDefaultOption("BasicStraight", "$BasicStraight");


        autoChooser.setSubsystem("TatorRobot");
        autoChooser.setName("Auto Chooser");
        SmartDashboard.putData(autoChooser);
    }

    @Override
    public Command getAutoCommand() {
        return commandStore.getCommand(autoChooser.getSelected());
    }
}
