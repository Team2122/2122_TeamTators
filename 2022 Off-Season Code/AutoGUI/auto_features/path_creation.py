import math

from miscellaneous.utility_functions import *

from miscellaneous.colors import way_point_color


def get_closest_path_point(left_edge, top_edge, path_points):
    """ returns: [left_edge, top_edge]; the closest point on the path to the way_point. IMPORTANT the unit for the
        left_edge, top_edge, and path_points must all be equal (meters, pixel, etc.)"""

    closest_point = path_points[0]

    shortest_distance = float('inf')

    for i in range(len(path_points) - 1):
        current_distance = math.dist(path_points[i], (left_edge, top_edge))

        if current_distance < shortest_distance:
            closest_point = path_points[i]
            shortest_distance = current_distance

    return closest_point

def draw_path_lines(way_points, field_canvas, control_point_line_width, path_line_width):
    """Draws the path and the way_points which are connected to the path"""

    path_points = get_pixel_path_points()

    for j in range(len(path_points) - 1):
        start_point = path_points[j]
        end_point = path_points[j + 1]
        field_canvas.create_line(start_point, end_point, fill=control_point_color, width=path_line_width)

    for way_point in way_points:
        closest_point = get_closest_path_point(way_point.left_edge, way_point.top_edge, path_points)
        field_canvas.create_line((way_point.left_edge, way_point.top_edge), closest_point, fill=way_point_color, width=control_point_line_width)

def write_postions_to_file(control_points):
    """Writes the control points [x, y] to a file, so the JAR file can give all the points for the path"""

    file_data = ""

    for control_point in control_points:
        left_edge, top_edge = get_meter_location(control_point.get_left_edge(), control_point.get_top_edge())
        file_data += (f"{left_edge},{top_edge}," +
                      # The velocities are off by a negative number for the vertical_velocity and the Auto GUI and the Auto Follower
                      f"{control_point.get_horizontal_velocity()},{-1 * control_point.get_vertical_velocity()}\n")

    file = open("swerve_input.txt", "w+")
    file_data = file_data[0:-1]  # The last enter must be deleted
    file.write(file_data)
    file.close()

def get_meter_location(left_edge, top_edge):
    """returns: [left_edge, top_edge]; the location that the SwerveLib.jar file needs to create the path- converts
    the values into meters and puts all the numbers based off of the base_left_edge and base_top_edge"""

    return [pixels_to_meters(left_edge), pixels_to_meters(top_edge)]

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
        point_coordinates = line.split(",")
        left_edge, top_edge = float(point_coordinates[0]), float(point_coordinates[1])
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
        point_coordinates = line.split(",")
        left_edge, top_edge = float(point_coordinates[0]), float(point_coordinates[1])

        # Have to convert from the (0, 0) of the coordinates being the top left edge of the screen to the (0, 0) being the center of the hub
        left_edge, top_edge = get_hub_centric_coordinates(left_edge, top_edge)

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




