package frc.robot.subsystems.overwatch;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import frc.robot.subsystems.overwatch.Graph.Node;
import frc.robot.subsystems.overwatch.Graph.RotationalDirection;
import java.util.EnumSet;
import org.littletonrobotics.junction.Logger;

public class GraphVisualizer {
    Translation2d[] safeWaypointNeighbors;
    Translation2d[] unsafeWaypointNeighbors;
    Translation2d[] allWaypoints;

    public GraphVisualizer() {
        safeWaypointNeighbors = Graph.nodeSetToTransArr(getAllNeighbors(Node.HOME));
        unsafeWaypointNeighbors = Graph.superstructurePosArrToTransArr(Graph.UNSAFE_ZONE);
        allWaypoints = Graph.nodeSetToTransArr(EnumSet.allOf(Node.class));
    }

    public void visualize(Node currentNode, Rotation2d pivotAngle, double liftHeight) {
        safeWaypointNeighbors = Graph.nodeSetToTransArr(getAllNeighbors(currentNode));
        Logger.recordOutput("Overwatch/Points/UnsafeNeighbors", unsafeWaypointNeighbors);
        Logger.recordOutput("Overwatch/Points/SafeNeighbors", safeWaypointNeighbors);
        Logger.recordOutput("Overwatch/Points/AllWaypoints", allWaypoints);
        Logger.recordOutput(
                "Overwatch/Points/Position", new Translation2d(pivotAngle.getRadians() * 20, liftHeight));
    }

    private EnumSet<Node> getAllNeighbors(Node node) {
        // EnumSet.copyOf() in order to prevent modifying the reference returned by getNeighbors
        var neighbors = EnumSet.copyOf(node.getNeighbors(RotationalDirection.CLOCKWISE));
        neighbors.addAll(node.getNeighbors(RotationalDirection.COUNTER_CLOCKWISE));
        return neighbors;
    }
}
