package SplineGenerator;

import SplineGenerator.Applied.LegacyVersions.OldVelocityController;
import SplineGenerator.Applied.SegmenterComplexVelocityController;
import SplineGenerator.Applied.Segmenter;
import SplineGenerator.GUI.BallVelocityDirectionController;
import SplineGenerator.GUI.KeyBoardListener;
import SplineGenerator.GUI.SplineDisplay;
import SplineGenerator.Splines.PolynomicSpline;
import SplineGenerator.Splines.Spline;
import SplineGenerator.Util.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class RobotCurveReplica {

    public static void main(String... args) {

        KeyBoardListener.initialize();

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
        System.out.println(spline.getDesmosEquations());


        Function<DVector, DVector> derivativeModifier = variable -> {
            variable.setMagnitude(10);
            return variable;
        };

        Function<DVector, DVector> distanceModifier = variable -> {
            variable.multiplyAll(35.001);
            return variable;
        };

        Segmenter segmenter = new Segmenter(spline, derivativeModifier, distanceModifier);
        segmenter.bounds = new Extrema(new DPoint(-2, -2), new DPoint(13, 5));
        segmenter.followerStep = .1;
        segmenter.onPathRadius = .4;

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

        SplineDisplay display = new SplineDisplay(spline, 0, 1, 1600, 700);
        display.displayables.add(ball);

        display.onGridDisplayables.add((point) ->  {
            return new DPosVector(point.clone(), segmenter.get(point.clone()).get(0));
        });

        ball.start();

        display.display();

        while (true) {
            display.repaint();
            if (navigatorController.isFinished()) {
                int cat = 12;
            }
        }
    }

}
