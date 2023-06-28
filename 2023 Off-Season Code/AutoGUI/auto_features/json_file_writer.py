import math
import uuid

from auto_components.control_point import ControlPoint
from miscellaneous.important_variables import *
from auto_features.path_creation import *
from auto_features.commands_retriever import commands_retriever
from miscellaneous.utility_functions import truncate

class JSONFileWriter:
    """The class that writes all the files"""

    control_point_json_items = []
    control_point_left_edge = 0
    control_point_top_edge = 0
    last_angle = 0
    way_point_to_current_angle = {}
    way_point_path_indexes_length = []
    way_point_to_angle = {}
    required_point_index_to_angle = {}
    path_points = []

    def write_file(self, file, all_way_points, all_control_points,
                   start_all_json_contents, first_required_point, last_required_point, placement_angle):
        """"Writes the JSON file that the Auto Path code uses"""

        self.last_angle = math.radians(placement_angle)  # So it is converted to radians
        all_json_contents = start_all_json_contents

        required_points = get_required_points(all_control_points)
        required_points = [first_required_point] + required_points
        required_points.append(last_required_point)

        self.path_points = get_meter_path_points()

        update_way_point_information(required_points, all_way_points)

        # All the lists need an empty line after them and the last list does not need a comma
        # ALso have to call the make_*_accurate because the input fields don't always reflect what the GUI shows
        self.update_way_point_json(all_json_contents, all_way_points)

        self.update_required_points_json(all_json_contents, required_points)

        self.update_control_points_json(all_json_contents, all_control_points)

        json.dump(all_json_contents, file, indent=JSON_MAX_INDENT)

    def update_way_point_json(self, all_json_contents, way_points):
        """ Updates 'all_json_contents' so it contains the 'way_points' in it, so the file that is eventually written
            can contain all the information necessary for the Auto Path Follower"""

        # The contents of the control points list
        way_point_json_items = []

        # So all_json_contents can now contain all the control points (and modifications to 'way_point_json_items' will be tracked
        # How the GUI and Auto Follower name points is different, which is why these names are different
        all_json_contents["ControlPoints"] = way_point_json_items

        for way_point in way_points:
            left_edge = way_point.get_field_left_edge()
            top_edge = way_point.get_field_top_edge()
            horizontal_velocity, vertical_velocity = self.get_angle_corrected_velocities(way_point)

            spline_order = way_point.get_spline_order()

            new_way_point_list_item = {
                "X": left_edge,
                "Y": top_edge,
                "Vx": horizontal_velocity,
                "Vy": vertical_velocity,
                "order": spline_order
            }

            way_point_json_items.append(new_way_point_list_item)

    def get_angle_corrected_velocities(self, way_point):
        """ :returns: double[2] {horizontal_velocity, vertical_velocity}; the velocities corrected for the angle. This
            is needed because the angle of the robot changes the field centric velocities"""

        field_centric_horizontal_velocity = way_point.get_horizontal_velocity()
        field_centric_vertical_velocity = way_point.get_vertical_velocity()

        field_centric_vector_angle = math.atan2(field_centric_vertical_velocity, field_centric_horizontal_velocity)
        vector_magnitude = math.sqrt(field_centric_vertical_velocity ** 2 + field_centric_horizontal_velocity ** 2)

        robot_current_angle = way_point.get_angle_at_point()
        robot_centric_vector_angle = field_centric_vector_angle + robot_current_angle

        return [
            vector_magnitude * math.cos(robot_centric_vector_angle),
            vector_magnitude * math.sin(robot_centric_vector_angle)
        ]

    def update_required_points_json(self, all_json_contents, required_points):
        """ Updates 'all_json_contents' so it contains the 'required_points' in it, so the file that is eventually written
            can contain all the information necessary for the Auto Path Follower"""

        # The contents of the control points list
        required_points_json = []

        # So all_json_contents can now contain all the required points (and modifications to 'required_points_json' will be tracked
        all_json_contents["RequiredPoints"] = required_points_json

        for required_point in required_points:

            left_edge = required_point.get_field_left_edge()
            top_edge = required_point.get_field_top_edge()

            new_required_point_json = {
                "X": left_edge,
                "Y": top_edge,
                "betweenWayPoints": required_point.get_between_way_points()
            }

            # Then the parameter values and names of each required must be tracked by required_points_json
            required_point_command_parameter_values = required_point.get_command_parameter_values()
            required_point_command_parameter_names = commands_retriever.get_combined_command_parameter_names(required_point.get_command_name())

            self.update_angle_key_and_value_pairs(required_point_command_parameter_names, required_point_command_parameter_values)
            new_required_point_json_args = {}
            new_required_point_json["args"] = new_required_point_json_args

            new_required_point_json["isNeeded"] = required_point.is_needed

            self.add_keys_and_values_to_dictionary(required_point_command_parameter_names, required_point_command_parameter_values, new_required_point_json_args)

            required_points_json.append(new_required_point_json)

    def update_control_points_json(self, all_json_contents, all_control_points):
        """returns: String; the control_points JSON"""

        control_points = get_control_points(all_control_points)

        self.control_point_json_items = []

        # How the GUI and Auto Follower define points is different, which is why the names are different here
        all_json_contents["WayPoints"] = self.control_point_json_items

        additional_control_point = None

        if len(control_points) < 2:
            first_control_point_coordinates = get_closest_path_point(control_points[0].get_field_left_edge(),
                                                                     control_points[0].get_field_top_edge(), self.path_points)

            left_edge, top_edge = self.get_next_control_point_coordinates(first_control_point_coordinates)
            additional_control_point = ControlPoint(left_edge, top_edge, None, None, False)

            additional_control_point.set_field_left_edge(left_edge)
            additional_control_point.set_field_top_edge(top_edge)
            additional_control_point.set_speed(control_points[0].get_speed())
            additional_control_point.set_command("None")
            additional_control_point.is_needed = False

            control_points.append(additional_control_point)

        for control_point in control_points:
            self.update_control_point_command_components_json(control_point)

        if additional_control_point is not None:
            additional_control_point.destroy()

    def update_control_point_command_components_json(self, control_point: ControlPoint):
        """Updates the command components; some commands are a conglomerate of 2+ commands, so this provides that architecture"""

        base_command_name = control_point.get_command_name()
        command_component_names = []

        # If there are no components for that command, then the components should be 0
        if commands_retriever.combined_commands.get(base_command_name) is not None:
            # The first one should be ignored because that is the base command, which is already accounted for
            command_component_names = commands_retriever.combined_commands.get(base_command_name)[1:]

        self.control_point_left_edge, self.control_point_top_edge = control_point.get_field_left_edge(), control_point.get_field_top_edge()

        base_command_unique_identifier = str(uuid.uuid4())[:4]  # Only first 4 digits are needed for a unique value
        self.update_control_point_command_json(control_point, base_command_name, base_command_unique_identifier, True, 0, control_point.is_needed)

        # Have to set these, so the next point will be the closest to the point of the base command
        self.control_point_left_edge, self.control_point_top_edge = get_closest_path_point(control_point.get_field_left_edge(), control_point.get_field_top_edge(), self.path_points)
        for x in range(len(command_component_names)):
            self.control_point_left_edge, self.control_point_top_edge = self.get_next_control_point_coordinates([self.control_point_left_edge, self.control_point_top_edge])

            self.update_control_point_command_json(control_point, command_component_names[x],
                                                         base_command_unique_identifier, False, x + 1, control_point.is_needed)

    def update_control_point_command_json(self, control_point, base_command_name, base_command_unique_identifier, is_base_command, command_component_number, is_needed):
        """ Updates the json for the base way point command or command component- The data for all of these will be in
            commands.json"""

        new_control_point_json_item = {}
        current_command_parameter_names = commands_retriever.get_command_parameter_base_names(base_command_name)

        self.add_control_point_tracking_values(new_control_point_json_item, is_base_command, base_command_unique_identifier, command_component_number)
        between_way_points_value = control_point.get_between_way_points()

        manipulation_possible = not between_way_points_value.__contains__("-") and between_way_points_value.lower() != "auto"

        # If it is a single value then a dash should be added
        if manipulation_possible and not is_base_command:
            between_way_points_value = f"{between_way_points_value}-{int(between_way_points_value) + 1}"

        self.add_keys_and_values_to_dictionary(["X", "Y", "Speed", "Command", "betweenWayPoints"],
                                               [self.control_point_left_edge, self.control_point_top_edge, control_point.get_speed(),
                                                base_command_name, between_way_points_value],
                                               new_control_point_json_item)

        current_command_parameter_values = self.get_command_parameters_values(current_command_parameter_names,
                                                                              control_point)
        new_control_point_json_item_args = {}
        new_control_point_json_item["args"] = new_control_point_json_item_args
        self.add_keys_and_values_to_dictionary(current_command_parameter_names, current_command_parameter_values,
                                               new_control_point_json_item_args)

        self.control_point_json_items.append(new_control_point_json_item)
        new_control_point_json_item["isNeeded"] = is_needed

    def get_between_way_points_value(self, control_point, is_base_command, start_index):
        """:returns: str; the between way points value for this control point"""

        return_value = control_point.get_between_way_points()

        # If it is the base command, it should be what the user typed, but if it isn't it should be calculated
        if not is_base_command:
            way_point_start_value = get_last_way_point_number(self.control_point_left_edge, self.control_point_top_edge, self.path_points, start_index)
            way_point_end_value = get_last_way_point_number(self.control_point_left_edge, self.control_point_top_edge, self.path_points, start_index)
            way_point_end_value = way_point_end_value if way_point_start_value != way_point_end_value else way_point_start_value + 1
            return_value = f"{way_point_start_value}-{way_point_end_value}"

        return return_value

    def add_control_point_tracking_values(self, new_control_point_json_item, is_base_command, base_command_unique_identifier, subcomponent_number):
        """ Adds the values that are used for tracking where components of a command belong to in 'new_control_point_json_item'
            the tracking values are 'name' and 'belongsTo'"""
        
        if is_base_command:
            new_control_point_json_item["name"] = base_command_unique_identifier
            new_control_point_json_item["belongsTo"] = "self"
        
        else:
            new_control_point_json_item["name"] = f"{base_command_unique_identifier} [{subcomponent_number}]"
            new_control_point_json_item["belongsTo"] = base_command_unique_identifier

    def get_command_parameters_values(self, current_parameter_names, control_point):
        """:returns: str[]; the values of the command parameters for the 'control_point'"""

        command_parameters_values = []

        for current_parameter_name in current_parameter_names:
            parameter_value = control_point.get_command_parameter_value(current_parameter_name)
            command_parameters_values.append(parameter_value)

        return command_parameters_values

    def get_next_control_point_coordinates(self, previous_coordinates):
        """ returns: [left_edge, top_edge]; the coordinates of the next way point. This is important because if one GUI way point command
            is the combination of 2+ Auto way point commands then those 2+ points need to be spaced apart otherwise the Auto code
            raises an error"""

        return_value = []
        previous_coordinates_index = self.path_points.index(previous_coordinates)
        distance = 0

        for x in range(previous_coordinates_index, len(self.path_points)):
            new_coordinates = self.path_points[x]
            distance += math.dist(previous_coordinates, new_coordinates)

            if distance >= MINIMUM_DISTANCE_BETWEEN_WAY_POINTS:
                return_value = new_coordinates
                break

        # If it could not find coordinates that are not far enough from the previous way point then a way point can't be there
        # Because the Auto code will raise an Error if way points are too close to each other
        if len(return_value) == 0:
            raise ValueError("It is not possible to have a way point there because it will be too close to other way points")

        return return_value

    def add_keys_and_values_to_dictionary(self, keys, values, dictionary):
        """Adds the key value pairs to the dictionary"""

        for x in range(len(keys)):
            dictionary_key = keys[x]
            dictionary_value = values[x]

            dictionary[dictionary_key] = dictionary_value

    def update_angle_key_and_value_pairs(self, keys, values):
        """Updates all the key value pairs that are angles: the key has 'angle' in its name"""

        for x in range(len(keys)):
            dictionary_key = keys[x]
            dictionary_value = values[x]

            if dictionary_key.__contains__("angle") or dictionary_key.__contains__("Angle"):
                current_radian_angle = float(dictionary_value) * math.pi / 180
                delta_angle = current_radian_angle - self.last_angle

                values[x] = delta_angle
                keys.append(f"{dictionary_key}-GUI")
                values.append(dictionary_value)

                # So the GUI angle (field centric) can be stored also.
                self.last_angle = current_radian_angle
    def write_postions_to_file(self):
        """Writes the control points [x, y] to a file, so the JAR file can give all the points for the path"""

        all_json_contents = {}
        test_file = open("test.txt", "w+")

        self.update_way_point_json(all_json_contents, points.way_points)
        json.dump(all_json_contents, open("swerve_input.txt", "w+"), indent=JSON_MAX_INDENT)

        for x in range(len(points.way_points)):
            test_file.write(f"Control Point # {x + 1}: [ {points.way_points[x].get_left_edge()}, {points.way_points[x].get_top_edge()} ]")

        test_file.close()



        os.system("java -jar AutoFollower.jar swerve_input.txt swerve_output.txt")
        swerve_output_reading = open("swerve_output.txt", "r")
        file_current_contents = get_string(swerve_output_reading.read()[:-1])  # The last enter must be deleted
        swerve_output = open("swerve_output.txt", "w+")

        # The file does not have the last Control Point number
        swerve_output.write(file_current_contents + "\n" + f"Control Point: {len(points.way_points) - 1}")


json_file_writer = JSONFileWriter()