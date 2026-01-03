package frc.robot.subsystems.overwatch;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.trajectory.TrapezoidProfile;
import edu.wpi.first.math.util.Units;
import frc.robot.subsystems.overwatch.OverwatchConstants.OverwatchPos;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class Graph {
    public enum Node implements OverwatchPos {
        HOME(-87, 21.5),
        CLIMB(57, 0),
        CORAL_HOLDING(-87, 35),
        L1TRANSPORT(-87, 26.2),
        PICK_SAFETY(-87, 40),
        PICK_ALGAE_SAFETY(-87, 51),
        CORAL_PICK(-87, 19.75),
        MIDPOINT_TO_HOME(-28, 30.60),
        L1PREP(-36, 26.2),
        L1POSTPLACE(-87, 26.2),
        L2PREP(65, 5.35),
        L2PLACE(7, 5.35),
        L3PREPREP(50, 22.35),
        L3PREP(65, 22.35),
        L3PLACE(3.6, L3PREP.liftHeightInches - 2),
        L3POSTPLACE(-87, L3PLACE.liftHeightInches + 12),
        TRANSPORT_SAFETY(65, Node.L3PREP.liftHeightInches),
        L4PREP(TRANSPORT_SAFETY.pivotAngle.getDegrees(), 51.16),
        L4FAST_PREPREP(L4PREP.pivotAngle().getDegrees(), CORAL_HOLDING.liftHeightInches() + 3),
        L4PLACE(-16, L4PREP.liftHeightInches),
        L4POSTPLACE(-87, 57),
        ALGAE_BARGE_PLACEMENT(125, 60),
        POST_BARGE(-82, PICK_SAFETY.liftHeightInches),
        ALGAE_PROCESSOR_PLACEMENT(-27, 16.5),
        ALGAE_PRE_GROUND_PICK(-87, 31.5),
        ALGAE_GROUND_PICK(-32.3, 10.83),
        ALGAE_REEF_HIGH(0, 37),
        ALGAE_REEF_LOW(0, 20.5),
        ALGAE_HOLDING(90, 12),
        ALGAE_HOLDING_SAFE(90, 40),
        DODGE_PICKER_PEAK(-100, 37),
        DODGE_PICKER_SIDE(-85, 37),
        DODGE_UNSAFE_HILL_SIDE(-160, 23),
        DODGE_UNSAFE_HILL_PEAK(-130, 40),
        ALGAE_AFTER_PICKED_HIGH(
                ALGAE_REEF_HIGH.pivotAngle.getDegrees(), ALGAE_REEF_HIGH.liftHeightInches + 4),
        ALGAE_AFTER_PICKED_LOW(0, ALGAE_REEF_LOW.liftHeightInches + 4),
        FIRST_DEST(90, 0);

        // to be amended to during initialization (see static block below)
        // initializing now messes up the jvm since Node isn't a class yet, has to stay null for now
        private EnumSet<Node> safeClockwiseNeighbors;
        private EnumSet<Node> safeCounterClockwiseNeighbors;
        private EnumSet<Node> unsafeNeighborOverrides;
        private final double liftHeightInches;
        private final Rotation2d pivotAngle;

        Node(double pivotAngleDegrees, double liftHeightInches) {
            var angleRads = Units.degreesToRadians(pivotAngleDegrees);
            this.pivotAngle = Rotation2d.fromRadians(MathUtil.angleModulus(angleRads));
            this.liftHeightInches = liftHeightInches;
        }

        @Override
        public double liftHeightInches() {
            return liftHeightInches;
        }

        @Override
        public Rotation2d pivotAngle() {
            return pivotAngle;
        }

        static {
            // add all neighbors whose edge doesn't intersect the unsafe zone
            for (Node node : Node.values()) {
                // force override which nodes can be travelled to from where
                // e.g. CORAL_PICK can only be accessed from HOME
                node.unsafeNeighborOverrides = EnumSet.noneOf(Node.class);
                if (node != Node.HOME) node.unsafeNeighborOverrides.add(Node.CORAL_PICK);
                // if (node != Node.L2PREPREP) node.unsafeNeighborOverrides.add(Node.L2PREP);
                // if (node != Node.L3PREPREP) node.unsafeNeighborOverrides.add(Node.L3PREP);
                // if (node != Node.L4PREPREP) node.unsafeNeighborOverrides.add(Node.L4PREP);

                node.safeClockwiseNeighbors =
                        getSafeNeighbors(node, RotationalDirection.CLOCKWISE, node.unsafeNeighborOverrides);
                node.safeCounterClockwiseNeighbors =
                        getSafeNeighbors(
                                node, RotationalDirection.COUNTER_CLOCKWISE, node.unsafeNeighborOverrides);
            }
        }

        /**
         * Return all nodes that are safe to travel directly to, given that you are travelling directly
         * from this node.
         */
        public EnumSet<Node> getNeighbors(RotationalDirection dir) {
            return switch (dir) {
                case CLOCKWISE -> safeClockwiseNeighbors;
                case COUNTER_CLOCKWISE -> safeCounterClockwiseNeighbors;
            };
        }

        /** Return the amount of time it takes to travel between this node and the specified node. */
        public double travelTime(Node node, RotationalDirection dir) {
            return Graph.travelTime(this, node, dir);
        }

        public String toString() {
            return name() + " " + string();
        }
    }

    /**
     * Data type used for representing a query between the starting and ending nodes on a path. Exists
     * just to make the type of the cache a little easier to read
     */
    public static record PathQuery(OverwatchPos startNode, Node goalNode, RotationalDirection dir) {}

    public enum RotationalDirection {
        CLOCKWISE,
        COUNTER_CLOCKWISE,
        ;
    }

    private static Map<PathQuery, List<OverwatchPos>> aStarCache = new HashMap<>();

    /** Return the amount of time it takes to travel between the start node and the goal node. */
    public static double travelTime(
            OverwatchPos startNode, OverwatchPos goalNode_, RotationalDirection dir) {
        OverwatchPos goalNode = repositionNode(startNode, goalNode_, dir);
        double nodeToNodeDist = startNode.distanceFrom(goalNode);
        var initialState = new TrapezoidProfile.State();
        var finalState = new TrapezoidProfile.State(nodeToNodeDist, 0.0);
        OverwatchConstants.MOTION_PROFILE.calculate(0.02, initialState, finalState);

        return OverwatchConstants.MOTION_PROFILE.totalTime();
    }

    /** Returns the gscore for the final node in the provided set */
    private static double cumulativeGScore(List<OverwatchPos> nodes, RotationalDirection dir) {
        double gscore = 0.0;

        for (int i = 0; i < nodes.size() - 1; i++) {
            var node = nodes.get(i);
            gscore += travelTime(node, nodes.get(i + 1), dir);
        }
        return gscore;
    }

    private static double calculateHScore(
            OverwatchPos from, OverwatchPos goalNode, RotationalDirection dir) {
        return travelTime(from, goalNode, dir);
    }

    /**
     * Calculate the time-optimal path from this node to the supplied node using A* to traverse the
     * graph
     *
     * <p>The G-score is the cost (time) of travelling directly from one node to another. The H-score
     * is the heuristic (in this case, time from a node to the goal node) associated with a node. The
     * F-score is the sum of the G and H scores, and is used to decide which node to go to. A* will
     * continuously pathfind by travelling to each node with the smallest F-score until it reaches the
     * goal
     */
    public static Optional<List<OverwatchPos>> pathfind(
            OverwatchPos startNode, Node goalNode, RotationalDirection dir) {
        PathQuery pathQuery = new PathQuery(startNode, goalNode, dir);
        if (aStarCache.containsKey(pathQuery)) {
            return Optional.of(aStarCache.get(pathQuery));
        }

        if (goalNode.equals(startNode)) {
            return Optional.of(List.of());
        }

        var openList = new ArrayList<OverwatchPos>();
        openList.add(startNode);
        var closedList = new ArrayList<OverwatchPos>();
        Map<OverwatchPos, OverwatchPos> cameFrom = new HashMap<>();
        Map<OverwatchPos, Double> gscore = new HashMap<>();
        Map<OverwatchPos, Double> hscore = new HashMap<>();

        gscore.put(startNode, 0.0);
        hscore.put(startNode, calculateHScore(startNode, goalNode, dir));

        OverwatchPos current = startNode;
        int panic = 0;
        while (!openList.isEmpty()) {
            // System.out.println(
            //         current
            //                 + " open "
            //                 + Arrays.toString(openList.toArray())
            //                 + " closed "
            //                 + Arrays.toString(closedList.toArray()));
            if (panic++ > 999) {
                System.err.println(
                        "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\nAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\nAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\nAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\nAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA PATHFIND LOOPED A LOT");
                return Optional.empty();
            }
            current = openList.get(0);
            for (OverwatchPos node : openList) {
                Double currentFScore =
                        gscore.getOrDefault(current, Double.MAX_VALUE)
                                + hscore.getOrDefault(current, Double.MAX_VALUE);
                Double otherFScore =
                        gscore.getOrDefault(node, Double.MAX_VALUE)
                                + hscore.getOrDefault(node, Double.MAX_VALUE);
                if (otherFScore < currentFScore) {
                    current = node;
                }
            }

            openList.remove(current);
            closedList.add(current);

            EnumSet<Node> neighbors;
            if (current.getClass() == Node.class) {
                neighbors = ((Node) current).getNeighbors(dir);
                // System.out.println(
                //         ((Node) current).toString() + " neighbors: " +
                // Arrays.toString(neighbors.toArray()));
            } else {
                neighbors = getSafeNeighbors(current, dir);
                // System.out.println(
                //         current.string() + "(not node) neighbors: " +
                // Arrays.toString(neighbors.toArray()));
            }

            for (OverwatchPos neighbor : neighbors) {
                if (neighbor == goalNode) {
                    cameFrom.put(goalNode, current);
                    var ret = reconstructPath(cameFrom, goalNode);
                    aStarCache.put(pathQuery, ret);
                    return Optional.of(ret);
                }

                if (closedList.contains(neighbor)) {
                    continue;
                }

                var tentativeGScore = cumulativeGScore(reconstructPath(cameFrom, neighbor), dir);
                var tentativeHScore = calculateHScore(neighbor, goalNode, dir);
                var tentativeFScore = tentativeGScore + tentativeHScore;
                var currentFScore =
                        gscore.getOrDefault(neighbor, Double.MAX_VALUE)
                                + hscore.getOrDefault(neighbor, Double.MAX_VALUE);

                if ((openList.contains(neighbor) || closedList.contains(neighbor))
                        && tentativeFScore > currentFScore) {
                    continue;
                } else {
                    if (!openList.contains(neighbor)) {
                        openList.add(neighbor);
                    }
                    gscore.put(neighbor, tentativeGScore);
                    hscore.put(neighbor, tentativeHScore);
                    cameFrom.put(neighbor, current);
                }
            }

            closedList.add(current);
        }

        return Optional.empty();
    }

    private static List<OverwatchPos> reconstructPath(
            Map<OverwatchPos, OverwatchPos> cameFrom, OverwatchPos lastNode) {
        Deque<OverwatchPos> totalPath = new ArrayDeque<>();
        totalPath.add(lastNode);
        var current = lastNode;
        while (cameFrom.containsKey(current)) {
            current = cameFrom.get(current);
            totalPath.addFirst(current);
        }
        return totalPath.stream().toList();
    }

    /**
     * an array of vertices on the state graph of the superstructure defining the area on that graph
     * that is unsafe for the superstructure to travel in.
     *
     * <p>It is expected that these vertices are sorted by pivot angle in ascending order.
     */
    public static final OverwatchPos[] UNSAFE_ZONE;

    static {
        ArrayList<OverwatchPos> unsafeZone = new ArrayList<>();
        for (var pos :
                new OverwatchPos[] {
                    OverwatchPos.of(Rotation2d.fromDegrees(-144.72), 19.917),
                    OverwatchPos.of(Rotation2d.fromDegrees(-136.08), 23.9),
                    OverwatchPos.of(Rotation2d.fromDegrees(-131.76), 28.1),
                    OverwatchPos.of(Rotation2d.fromDegrees(-129.24), 31.6),
                    OverwatchPos.of(Rotation2d.fromDegrees(-122.76), 34.54),
                    OverwatchPos.of(Rotation2d.fromDegrees(-115.2), 35.5),
                    OverwatchPos.of(Rotation2d.fromDegrees(-90.18), 35.5),
                    OverwatchPos.of(Node.CORAL_PICK.pivotAngle, Node.CORAL_PICK.liftHeightInches - 2),
                    OverwatchPos.of(Rotation2d.fromDegrees(-81.36), 23.2),
                    OverwatchPos.of(Rotation2d.fromDegrees(-61.2), 20.95),
                    OverwatchPos.of(Rotation2d.fromDegrees(-43.2), 15.36),
                    OverwatchPos.of(Rotation2d.fromDegrees(-32.4), 11.25),
                    OverwatchPos.of(Rotation2d.fromDegrees(-19.8), 6.04),
                    OverwatchPos.of(Rotation2d.fromDegrees(-7.2), -0.1),
                    OverwatchPos.of(Rotation2d.fromDegrees(124.56), -0.1),
                    OverwatchPos.of(Rotation2d.fromDegrees(135.), 3.5),
                    OverwatchPos.of(Rotation2d.fromDegrees(155.88), 7.1),
                    OverwatchPos.of(Rotation2d.fromDegrees(180.0), 12),
                }) {
            unsafeZone.add(pos);
        }

        // unsafeZone gets mutated a lot, nice to have a copy of the original
        var unsafeZoneReference = unsafeZone.toArray(OverwatchPos[]::new);

        // wrap the unsafe zone around the periodic boundary
        for (var pos : unsafeZoneReference) {
            unsafeZone.add(
                    OverwatchPos.of(
                            // can't do .plus() because that'll clamp the rotation between -pi and pi
                            Rotation2d.fromRadians(pos.pivotAngle().getRadians() + 2 * Math.PI),
                            pos.liftHeightInches()));
        }

        for (int i = unsafeZoneReference.length - 1; i >= 0; i--) {
            var pos = unsafeZoneReference[i];
            unsafeZone.add(
                    0,
                    OverwatchPos.of(
                            Rotation2d.fromRadians(pos.pivotAngle().getRadians() - 2 * Math.PI),
                            pos.liftHeightInches()));
        }

        UNSAFE_ZONE = unsafeZone.toArray(OverwatchPos[]::new);
    }

    public static Translation2d nodeToTranslation2d(Node node) {
        return new Translation2d(node.pivotAngle().getRadians(), node.liftHeightInches());
    }

    public static Translation2d[] nodeSetToTransArr(EnumSet<Node> nodes) {
        Translation2d[] ret = new Translation2d[nodes.size()];
        for (int i = 0; i < nodes.size(); i++) {
            List<Node> nodesList = nodes.stream().collect(Collectors.toList());
            OverwatchPos pos = nodesList.get(i);
            ret[i] = new Translation2d(pos.pivotAngle().getRadians() * 20, pos.liftHeightInches());
        }
        return ret;
    }

    public static Translation2d[] superstructurePosArrToTransArr(OverwatchPos[] positions) {
        Translation2d[] ret = new Translation2d[positions.length];
        for (int i = 0; i < positions.length; i++) {
            var pos = positions[i];
            ret[i] = new Translation2d(pos.pivotAngle().getRadians() * 20, pos.liftHeightInches());
        }
        return ret;
    }

    /**
     * Return an {@link OverwatchPos} that is shifted 2*pi to the left or right if such a
     * transformation is needed in order to be able to draw a straight line from base to node while
     * maintaining the direction specified.
     */
    public static OverwatchPos repositionNode(
            OverwatchPos base, OverwatchPos node, RotationalDirection dir) {
        double nodeAngle = node.pivotAngle().getRadians();
        double baseAngle = base.pivotAngle().getRadians();

        OverwatchPos ret = node;

        if (dir == RotationalDirection.COUNTER_CLOCKWISE && baseAngle > nodeAngle) {
            ret =
                    OverwatchPos.of(Rotation2d.fromRadians(nodeAngle + 2 * Math.PI), node.liftHeightInches());
        }

        if (dir == RotationalDirection.CLOCKWISE && baseAngle < nodeAngle) {
            ret =
                    OverwatchPos.of(Rotation2d.fromRadians(nodeAngle - 2 * Math.PI), node.liftHeightInches());
        }

        return ret;
    }

    private static EnumSet<Node> getSafeNeighbors(OverwatchPos pos, RotationalDirection dir) {
        return getSafeNeighbors(pos, dir, EnumSet.noneOf(Node.class));
    }

    private static EnumSet<Node> getSafeNeighbors(
            OverwatchPos pos, RotationalDirection dir, EnumSet<Node> unsafeNodes) {
        var ret = EnumSet.noneOf(Node.class);

        // add all neighbors whose edge doesn't intersect the unsafe zone
        for (Node unmodifiedNode : Node.values()) {

            OverwatchPos node = repositionNode(pos, unmodifiedNode, dir);

            boolean doIntersect = false;
            for (int i = 0; i < UNSAFE_ZONE.length - 1; i++) {
                OverwatchPos[][] segments = {
                    {pos, node},
                    {UNSAFE_ZONE[i], UNSAFE_ZONE[i + 1]}
                };

                if (unsafeNodes.contains(node) || doIntersect(segments) || pos == unmodifiedNode) {
                    doIntersect = true;
                    break;
                }
            }

            if (!doIntersect) {
                ret.add(unmodifiedNode);
            }
        }

        return ret;
    }

    /* https://www.geeksforgeeks.org/dsa/check-if-two-given-line-segments-intersect/ */
    // function to check if point q lies on line segment 'pr'
    private static boolean onSegment(OverwatchPos p, OverwatchPos q, OverwatchPos r) {
        double px = p.pivotAngle().getRadians();
        double py = p.liftHeightInches();

        double qx = q.pivotAngle().getRadians();
        double qy = q.liftHeightInches();

        double rx = r.pivotAngle().getRadians();
        double ry = r.liftHeightInches();

        return (qx <= Math.max(px, rx)
                && qx >= Math.min(px, rx)
                && qy <= Math.max(py, ry)
                && qy >= Math.min(py, ry));
    }

    // function to find orientation of ordered triplet (p, q, r)
    // 0 --> p, q and r are collinear
    // 1 --> Clockwise
    // 2 --> Counterclockwise
    public static int orientation(OverwatchPos p, OverwatchPos q, OverwatchPos r) {
        double px = p.pivotAngle().getRadians();
        double py = p.liftHeightInches();

        double qx = q.pivotAngle().getRadians();
        double qy = q.liftHeightInches();

        double rx = r.pivotAngle().getRadians();
        double ry = r.liftHeightInches();

        double val = (qy - py) * (rx - qx) - (qx - px) * (ry - qy);

        if (val == 0) return 0;

        return (val > 0) ? 1 : 2;
    }

    // function to check if two line segments intersect
    public static boolean doIntersect(OverwatchPos[][] points) {

        int o1 = orientation(points[0][0], points[0][1], points[1][0]);
        int o2 = orientation(points[0][0], points[0][1], points[1][1]);
        int o3 = orientation(points[1][0], points[1][1], points[0][0]);
        int o4 = orientation(points[1][0], points[1][1], points[0][1]);

        return (o1 == 0 && onSegment(points[0][0], points[1][0], points[0][1]))
                || (o2 == 0 && onSegment(points[0][0], points[1][1], points[0][1]))
                || (o3 == 0 && onSegment(points[1][0], points[0][0], points[1][1]))
                || (o4 == 0 && onSegment(points[1][0], points[0][1], points[1][1]))
                || (o1 != o2 && o3 != o4);
    }
}
