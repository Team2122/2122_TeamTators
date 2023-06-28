import math

from miscellaneous.utility_functions import *

from miscellaneous.colors import control_point_color


def get_closest_path_point(left_edge, top_edge, path_points, control_point=None):
    """ returns: [left_edge, top_edge]; the closest point on the path to the control_point. IMPORTANT the unit for the
        left_edge, top_edge, and path_points must all be equal (meters, pixel, etc.)"""

    return path_points[get_closest_path_point_index(left_edge, top_edge, path_points, control_point)]

def get_closest_path_point_index(left_edge, top_edge, path_points, control_point=None, start_index=None, end_index=None):
    """returns: int; the index of the closest path point"""

    shortest_distance = float('inf')
    closest_point_index = 0

    if control_point is not None:
        control_point_start_index, control_point_end_index = get_control_point_path_indexes(control_point, path_points)

        start_index = control_point_start_index if start_index is None else start_index
        end_index = control_point_end_index if end_index is None else end_index

    start_index = 0 if start_index is None else start_index
    end_index = len(path_points) if end_index is None else end_index

    for i in range(start_index, end_index):
        current_distance = math.dist(path_points[i], (left_edge, top_edge))

        if current_distance < shortest_distance:
            shortest_distance = current_distance
            closest_point_index = i

    return closest_point_index

def get_control_point_path_indexes(control_point, path_points):
    """:returns: int[]; the path start and end index that the control point must be within"""

    return_values = None

    # For more information see the documentation for 'get_between_way_points()'
    between_way_points = control_point.get_between_way_points()

    if between_way_points.lower() == "auto":
        return_values = [0, len(path_points)]

    # Meaning that the user has chosen which way_point the control_point should be on
    elif not between_way_points.__contains__("-"):
        way_point_index = int(between_way_points) - 1
        way_point = points.way_points[way_point_index]
        return_values = [way_point.path_index, way_point.path_index + 1]

    # Meaning that it is number1-number2 (so it must be between two control points)
    else:
        way_point_indexes = between_way_points.split("-")
        start_way_point_index = int(way_point_indexes[0]) - 1
        end_way_point_index = int(way_point_indexes[1]) - 1

        start_way_point = points.way_points[start_way_point_index]
        end_way_point = points.way_points[end_way_point_index]

        return_values = [start_way_point.path_index, end_way_point.path_index]

    return return_values


def draw_path_lines(field_canvas, way_point_line_width, path_line_width):
    """Draws the path and the control_points which are connected to the path"""

    path_points = get_pixel_path_points()

    for j in range(len(path_points) - 1):
        start_point = path_points[j]
        end_point = path_points[j + 1]
        field_canvas.create_line(start_point, end_point, fill=way_point_color, width=path_line_width)

    for control_point in points.control_points:
        closest_point = get_closest_path_point(control_point.left_edge, control_point.top_edge, path_points, control_point)
        field_canvas.create_line((control_point.left_edge, control_point.top_edge), closest_point, fill=control_point_color, width=way_point_line_width)


def get_pixel_location(left_edge, top_edge):
    """returns: [left_edge, top_edge]; the pixel location that the GUI uses. This converts from the Swerve Code
    locations to the GUI code locations."""

    return [int(meters_to_pixels(left_edge)), int(meters_to_pixels(top_edge))]

# The reason this function has very similar code to get_meter_path_points() is because calling get_meter_path_points()
# And then getting the pixel points requires going through the same points twice, which is too slow
def get_pixel_path_points():
    """returns: [[x1, y1], [x2, y2]]; the points along the path (in pixels)"""

    path_points = []
    file = open("swerve_output.txt", "r")

    # The last line has no data as of now, so that line should be ignored (hence list[:-1])
    for line in get_lines(file.read()[:-1]):
        is_control_point_line = line.__contains__("Control Point")

        if is_control_point_line:
            continue

        point_coordinates = line.split(",")
        left_edge, top_edge = float(point_coordinates[0]), float(point_coordinates[1])
        left_edge, top_edge = get_gui_centric_coordinates(left_edge, top_edge)
        pixel_locations = get_pixel_location(left_edge, top_edge)

        if not path_points.__contains__(pixel_locations):
            path_points.append(pixel_locations)

    file.close()
    return path_points

def get_meter_path_points():
    """returns: [[x1, y1], [x2, y2]]; the points along the path (in meters)"""

    path_points = []
    file = open("swerve_output.txt", "r")

    for line in get_lines(file.read()[:-1]):
        is_control_point_line = line.__contains__("Control Point")

        # The lines that does not contain control points then it has the path points
        if not is_control_point_line:
            point_coordinates = line.split(",")
            left_edge, top_edge = float(point_coordinates[0]), float(point_coordinates[1])

            # Have to convert from the (0, 0) of the coordinates being the top left edge of the screen to the (0, 0) being the center of the hub
            # left_edge, top_edge = get_hub_centric_coordinates(left_edge, top_edge)

            path_points.append([left_edge, top_edge])

    file.close()

    return path_points

def get_hub_centric_coordinates(left_edge, top_edge):
    """ summary: The meter left_edge and top_edge (location user modifies) has (0, 0) at the center of the hub and the pixel location
        (GUI location) has (0,0) at the top left edge of the screen. When the pixel coordinates are converted to meters by a scalar multiplier,
        therefore, offsets must be subtracted to have (0, 0) once again be the center of the hub

        params:
            left_edge: double; the left_edge of a point that has (0, 0) at the top left edge of the screen instead of the center of the hub
            top_edge: double; the top_edge of a point that has (0, 0) at the top left edge of the screen instead of the center of the hub

        returns: Double[] {converted_left_edge, converted_top_edge}; the left_edge and top_edge that has the center of the hub be (0, 0)
    """

    left_edge -= CENTER_OF_FIELD_HORIZONTAL_OFFSET
    top_edge -= CENTER_OF_FIELD_VERTICAL_OFFSET

    top_edge *= -1  # Because of how the GUI is set up the number will be the negative version of the correct meter number

    return [left_edge, top_edge]

def get_gui_centric_coordinates(left_edge, top_edge):
    """ summary: The meter left_edge and top_edge (location user modifies) has (0, 0) at the center of the hub and the pixel location
        (GUI location) has (0,0) at the top left edge of the screen. When the pixel coordinates are converted to meters by a scalar multiplier,
        therefore, offsets must be added to have (0, 0) once again be the top left edge of the screen

        params:
            left_edge: double; the left_edge of a point that has (0, 0) at the center of the hub instead of the top left edge of the screen
            top_edge: double; the top_edge of a point that has (0, 0) at the center of the hub instead of the top left edge of the screen

        returns: Double[] {converted_left_edge, converted_top_edge}; the left_edge and top_edge that has the top left edge of the screen be (0, 0)
    """

    top_edge *= -1  # The numbers are multiplied by -1 to convert to hub_centric, so it must be multiplied by -1 to convert it to gui
    left_edge += CENTER_OF_FIELD_HORIZONTAL_OFFSET
    top_edge += CENTER_OF_FIELD_VERTICAL_OFFSET

    return [left_edge, top_edge]

def get_meter_location(left_edge, top_edge):
    """returns: [left_edge, top_edge]; the location that the AutoFollower.jar file needs to create the path - converts
    the values into meters and puts all the numbers based off of the base_left_edge and base_top_edge"""

    return [pixels_to_meters(left_edge), pixels_to_meters(top_edge)]


def get_way_point_path_indexes():
    """:returns: int[]; the path indexes of the way points"""

    return_value = []
    file = open("swerve_output.txt", "r")
    way_point_index = -1  # So the first way point index starts at 0 not 1

    for line in get_lines(file.read()[:-1]):
        is_control_point_line = line.__contains__("Control Point")

        # The lines that does not contain control points then it has the path points
        if is_control_point_line:
            amount_added = 1 if way_point_index == -1 else 0  # It starts at -1, which is not a valid index
            return_value.append(way_point_index + amount_added)

        else:
            way_point_index += 1

    file.close()

    return return_value


def update_way_point_information(required_points=None, way_points=None):
    """Updates the information that the way points need specifically which required point affects the angle of the robot"""

    path_points = get_meter_path_points()

    required_point_path_indexes = []
    required_point_index_to_angle = {}

    required_points = get_required_points(points.control_points) if required_points is None else required_points
    way_points = points.way_points if way_points is None else way_points

    for required_point in required_points:
        index = get_closest_path_point_index(required_point.get_field_left_edge(), required_point.get_field_top_edge(), path_points, required_point)
        required_point_path_indexes.append(index)
        required_point_index_to_angle[index] = get_required_point_angle(required_point)

    way_point_path_indexes = get_way_point_path_indexes()
    for x in range(len(points.way_points)):
        way_points[x].path_index = way_point_path_indexes[x]
        # way_points[x].path_index = x

    for way_point in way_points:
        last_angle = get_required_point_angle(required_points[0])
        next_angle = get_required_point_angle(required_points[1])

        last_required_point_affecting_way_point_path_index = float("-inf")
        next_required_point_affecting_way_point_path_index = float("inf")
        way_point_path_index = way_point.path_index

        for i in range(len(required_point_path_indexes)):
            required_point_path_index = required_point_path_indexes[i]

            if required_point_path_index <= way_point_path_index and required_point_path_index > last_required_point_affecting_way_point_path_index:
                last_required_point_affecting_way_point_path_index = required_point_path_index
                last_angle = get_required_point_angle(required_points[i])

            if required_point_path_index >= way_point_path_index and required_point_path_index < next_required_point_affecting_way_point_path_index:
                next_required_point_affecting_way_point_path_index = required_point_path_index
                next_angle = get_required_point_angle(required_points[i])

        angle = get_angle_at_point(way_point_path_index, last_required_point_affecting_way_point_path_index,
                                        next_required_point_affecting_way_point_path_index, last_angle, next_angle, path_points)

        way_point.set_angle_at_point(angle)

def get_angle_at_point(way_point_path_index, last_required_point_affecting_way_point_path_index,
                        next_required_point_affecting_way_point_path_index, last_angle, next_angle, path_points):
    """:returns: float; the angle at the way point (gotten from figuring out how much each required point angle affects the way point"""

    return_value = None

    # No proportion calculations need to be done because the required point is on the way point
    if last_required_point_affecting_way_point_path_index == next_required_point_affecting_way_point_path_index:
        return_value = last_angle

    else:
        last_required_point_distance_from_way_point = get_distance(last_required_point_affecting_way_point_path_index, way_point_path_index, path_points)
        next_required_point_distance_from_way_point = get_distance(way_point_path_index, next_required_point_affecting_way_point_path_index, path_points)

        total_distance = last_required_point_distance_from_way_point + next_required_point_distance_from_way_point
        last_required_point_proportion = 1 - (last_required_point_distance_from_way_point / total_distance)
        next_required_point_proportion = 1 - (next_required_point_distance_from_way_point / total_distance)

        last_angle = math.radians(last_angle)
        next_angle = math.radians(next_angle)

        return_value = last_required_point_proportion * last_angle + next_required_point_proportion * next_angle

    return return_value

def get_distance(first_index, last_index, path_points):
    """Gets the distance from all the lines of the path points"""

    current_distance = 0
    for x in range(first_index, last_index):
        point1 = path_points[x]
        point2 = path_points[x + 1]

        current_distance += math.dist(point1, point2)

    return current_distance

def get_last_way_point_number(left_edge, top_edge, path_points, start_index=0, end_index=None):
    """:returns: int; the number of the last way_point at those path left_edge and top_edge"""

    end_index = len(points.way_points) if end_index is None else end_index
    path_point_index = get_closest_path_point_index(left_edge, top_edge, path_points, start_index=start_index, end_index=end_index)
    return_value = 0
    closest_path_index = float("-inf")

    for x in range(len(points.way_points)):
        way_point = points.way_points[x]
        if way_point.path_index > closest_path_index and way_point.path_index <= path_point_index:
            return_value = x + 1
            closest_path_index = way_point.path_index

    return return_value

