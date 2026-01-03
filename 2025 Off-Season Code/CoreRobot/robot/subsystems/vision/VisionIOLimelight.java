package frc.robot.subsystems.vision;

import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.networktables.DoubleArrayPublisher;
import edu.wpi.first.networktables.DoubleArraySubscriber;
import edu.wpi.first.networktables.DoubleSubscriber;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.Timer;
import frc.robot.subsystems.vision.VisionConstants.PoseObservation;
import frc.robot.subsystems.vision.VisionConstants.PoseObservationType;
import frc.robot.subsystems.vision.VisionConstants.TargetObservation;
import java.util.LinkedList;
import java.util.List;

public class VisionIOLimelight implements VisionIO {
    private final DoubleArrayPublisher orientationPublisher;

    private final DoubleSubscriber latencySubscriber;
    private final DoubleSubscriber txSubscriber;
    private final DoubleSubscriber tySubscriber;
    private final DoubleArraySubscriber megatag1Subscriber;
    private final DoubleArraySubscriber megatag2Subscriber;

    private Rotation2d yaw;
    private Rotation2d yawRate;

    public VisionIOLimelight(String camName) {
        var table = NetworkTableInstance.getDefault().getTable(camName);

        orientationPublisher = table.getDoubleArrayTopic("robot_orientation_set").publish();

        latencySubscriber = table.getDoubleTopic("tl").subscribe(0.0);

        txSubscriber = table.getDoubleTopic("tx").subscribe(0.0);
        tySubscriber = table.getDoubleTopic("ty").subscribe(0.0);

        megatag1Subscriber = table.getDoubleArrayTopic("botpose_wpiblue").subscribe(new double[] {});
        megatag2Subscriber =
                table.getDoubleArrayTopic("botpose_orb_wpiblue").subscribe(new double[] {});

        yaw = new Rotation2d();
        yawRate = new Rotation2d();
    }

    public void updateInputs(VisionIOInputs inputs) {
        // Updates connection status based on if an update was seen in the last 250ms
        inputs.connected = (Timer.getTimestamp() - latencySubscriber.getAtomic().timestamp) < 250;

        // Update orientation for MegaTag 2
        orientationPublisher.accept(
                new double[] {yaw.getDegrees(), yawRate.getDegrees(), 0.0, 0.0, 0.0, 0.0});
        // new double[] {yaw.getDegrees(), 0.0, 0.0, 0.0, 0.0});
        NetworkTableInstance.getDefault()
                .flush(); // Recommended by Limelight but does increase network traffic
        // Read new pose observations from NetworkTables
        List<TargetObservation> targetObservations = new LinkedList<>();
        List<PoseObservation> poseObservations = new LinkedList<>();
        for (var rawSample : megatag1Subscriber.readQueue()) {

            if (rawSample.value.length == 0) continue;

            for (int i = 11; i + 5 < rawSample.value.length; i += 7) {
                targetObservations.add(
                        new TargetObservation(
                                rawSample.value[i + 5], // dist tag to robot
                                Rotation2d.fromDegrees(txSubscriber.get()), // tx
                                Rotation2d.fromDegrees(tySubscriber.get()), // ty
                                (int) rawSample.value[i])); // tag ID
            }
            poseObservations.add(
                    new PoseObservation(
                            // Timestamp, based on server timestamp of publish and latency
                            rawSample.timestamp * 1.0e-6 - rawSample.value[6] * 1.0e-3,

                            // 3D pose estimate
                            parsePose(rawSample.value),

                            // Ambiguity, using only the first tag because ambiguity isn't applicable for multitag
                            rawSample.value.length > 17 ? rawSample.value[17] : 0.0,

                            // Tag count
                            (int) rawSample.value[7],

                            // Average tag distance
                            rawSample.value[9],

                            // Observation type
                            PoseObservationType.MEGATAG1));
        }
        for (var rawSample : megatag2Subscriber.readQueue()) {
            if (rawSample.value.length == 0) continue;

            // recording target observations here would be duplicate
            // data since Megatag2 sees the same image Megatag1 does

            poseObservations.add(
                    new PoseObservation(
                            // Timestamp, based on server timestamp of publish and latency
                            rawSample.timestamp * 1.0e-6 - rawSample.value[6] * 1.0e-3,

                            // 3D pose estimate
                            parsePose(rawSample.value),

                            // Ambiguity, zeroed because the pose is already disambiguated
                            0.0,

                            // Tag count
                            (int) rawSample.value[7],

                            // Average tag distance
                            rawSample.value[9],

                            // Observation type
                            PoseObservationType.MEGATAG2));
        }

        // Save pose observations to inputs object
        inputs.poseObservations = new PoseObservation[poseObservations.size()];
        for (int i = 0; i < poseObservations.size(); i++) {
            inputs.poseObservations[i] = poseObservations.get(i);
        }

        // Save target observations to inputs object
        inputs.latestTargetObservations = new TargetObservation[targetObservations.size()];
        for (int i = 0; i < targetObservations.size(); i++) {
            inputs.latestTargetObservations[i] = targetObservations.get(i);
        }
    }

    /** Parses the 3D pose from a Limelight botpose array. */
    private static Pose3d parsePose(double[] rawLLArray) {
        return new Pose3d(
                rawLLArray[0],
                rawLLArray[1],
                rawLLArray[2],
                new Rotation3d(
                        Units.degreesToRadians(rawLLArray[3]),
                        Units.degreesToRadians(rawLLArray[4]),
                        Units.degreesToRadians(rawLLArray[5])));
    }

    /** Update orientation of the robot for MegaTag2 */
    @Override
    public void setOrientation(Rotation2d yaw, Rotation2d yawRate) {
        this.yaw = yaw;
        this.yawRate = yawRate;
    }
}
