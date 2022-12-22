import math
import uuid

from auto_components.way_point import WayPoint
from miscellaneous.important_variables import *
from auto_features.path_creation import write_postions_to_file, get_meter_path_points, get_closest_path_point
from auto_features.commands_retriever import commands_retriever



class JSONFileWriter:
    """The class that writes all the files"""

    way_point_json_items = []
    way_point_left_edge = 0
    way_point_top_edge = 0

    def write_file(self, file_path, file, all_control_points, all_way_points):
        """"Writes the JSON file that the Auto Path code uses"""

        last_slash_index = file_path.rindex("/")
        file_name = file_path[last_slash_index + 1:]
        write_postions_to_file(all_control_points)

        all_json_contents = {"Name": file_name}

        # All the lists need an empty line after them and the last list does not need a comma
        # ALso have to call the make_*_accurate because the input fields don't always reflect what the GUI shows
        self.update_control_point_json(all_json_contents, all_control_points)

        self.update_required_points_json(all_json_contents, all_way_points)
        self.update_way_points_json(all_json_contents, all_way_points)
        json.dump(all_json_contents, file, indent=JSON_MAX_INDENT)

    def update_control_point_json(self, all_json_contents, control_points):
        """ Updates 'all_json_contents' so it contains the 'control_points' in it, so the file that is eventually written
            can contain all the information necessary for the Auto Path Follower"""

        # The contents of the control points list
        control_point_json_items = []

        # So all_json_contents can now contain all the control points (and modifications to 'control_point_json_items' will be tracked
        all_json_contents["ControlPoints"] = control_point_json_items

        for control_point in control_points:
            left_edge = control_point.get_field_left_edge()
            top_edge = control_point.get_field_top_edge()
            horizontal_velocity = control_point.get_horizontal_velocity()
            vertical_velocity = control_point.get_vertical_velocity()

            new_control_point_list_item = {
                "X": left_edge,
                "Y": top_edge,
                "Vx": horizontal_velocity,
                "Vy": vertical_velocity
            }

            control_point_json_items.append(new_control_point_list_item)

    def update_required_points_json(self, all_json_contents, all_way_points):
        """ Updates 'all_json_contents' so it contains the 'required_points' in it, so the file that is eventually written
            can contain all the information necessary for the Auto Path Follower"""

        # Only way points that are required are considered 'required points'
        required_points = list(filter(lambda item: item.get_command_name() == "requiredPoint", all_way_points))

        # The contents of the control points list
        required_points_json = []

        # So all_json_contents can now contain all the required points (and modifications to 'required_points_json' will be tracked
        all_json_contents["RequiredPoints"] = required_points_json

        for required_point in required_points:

            left_edge = required_point.get_field_left_edge()
            top_edge = required_point.get_field_top_edge()

            new_required_point_json = {
                "X": left_edge,
                "Y": top_edge
            }

            # Then the parameter values and names of each required must be tracked by required_points_json
            required_point_command_parameter_values = required_point.get_command_parameter_values()
            required_point_command_parameter_names = commands_retriever.get_combined_command_parameter_names(required_point.get_command_name())

            new_required_point_json_args = {}
            new_required_point_json["args"] = new_required_point_json_args

            for x in range(len(required_point_command_parameter_names)):
                command_parameter_name = required_point_command_parameter_names[x]
                command_parameter_values = required_point_command_parameter_values[x]

                new_required_point_json_args[command_parameter_name] = command_parameter_values

            required_points_json.append(new_required_point_json)

    def update_way_points_json(self, all_json_contents, all_way_points):
        """returns: String; the way_points JSON"""

        # Only way points that aren't required are considered 'way points'
        way_points = list(filter(lambda item: item.get_command_name() != "requiredPoint", all_way_points))

        path_points = get_meter_path_points()
        self.way_point_json_items = []
        all_json_contents["WayPoints"] = self.way_point_json_items

        for way_point in way_points:
            self.update_way_point_command_components_json(way_point, path_points)

    def update_way_point_command_components_json(self, way_point: WayPoint, path_points):
        """Updates the command components; some commands are a conglomerate of 2+ commands, so this provides that architecture"""

        base_command_name = way_point.get_command_name()
        command_component_names = []

        # If there are no components for that command, then the components should be 0
        if commands_retriever.combined_commands.get(base_command_name) is not None:
            # The first one should be ignored because that is the base command, which is already accounted for
            command_component_names = commands_retriever.combined_commands.get(base_command_name)[1:]

        self.way_point_left_edge, self.way_point_top_edge = way_point.get_field_left_edge(), way_point.get_field_top_edge()

        base_command_unique_identifier = str(uuid.uuid4())[:4]  # Only first 4 digits are needed for a unique value
        self.update_way_point_command_json(way_point, base_command_name, base_command_unique_identifier, True, 0)

        # Have to set these, so the next point will be the closest to the point of the base command
        self.way_point_left_edge, self.way_point_top_edge = get_closest_path_point(way_point.get_field_left_edge(), way_point.get_field_top_edge(), path_points)
        for x in range(len(command_component_names)):
            self.way_point_left_edge, self.way_point_top_edge = self.get_next_way_point_coordinates([self.way_point_left_edge, self.way_point_top_edge], path_points)

            self.update_way_point_command_json(way_point, command_component_names[x],
                                                         base_command_unique_identifier, False, x + 1)

    def update_way_point_command_json(self, way_point, base_command_name, base_command_unique_identifier, is_base_command, command_component_number):
        """ Updates the json for the base way point command or command component- The data for all of these will be in
            commands.txt"""

        new_way_point_json_item = {}
        current_command_parameter_names = commands_retriever.get_command_parameter_base_names(base_command_name)

        self.add_way_point_tracking_values(new_way_point_json_item, is_base_command, base_command_unique_identifier, command_component_number)

        self.add_keys_and_values_to_dictionary(["X", "Y", "Speed", "Command"],
                                               [self.way_point_left_edge, self.way_point_top_edge, way_point.get_speed(),
                                                base_command_name],
                                               new_way_point_json_item)

        current_command_parameter_values = self.get_command_parameters_values(current_command_parameter_names,
                                                                              way_point)
        new_way_point_json_item_args = {}
        new_way_point_json_item["args"] = new_way_point_json_item_args
        self.add_keys_and_values_to_dictionary(current_command_parameter_names, current_command_parameter_values,
                                               new_way_point_json_item_args)

        self.way_point_json_items.append(new_way_point_json_item)

    def add_way_point_tracking_values(self, new_way_point_json_item, is_base_command, base_command_unique_identifier, subcomponent_number):
        """ Adds the values that are used for tracking where components of a command belong to in 'new_way_point_json_item'
            the tracking values are 'name' and 'belongsTo'"""
        
        if is_base_command:
            new_way_point_json_item["name"] = base_command_unique_identifier
            new_way_point_json_item["belongsTo"] = "self"
        
        else:
            new_way_point_json_item["name"] = f"{base_command_unique_identifier} [{subcomponent_number}]"
            new_way_point_json_item["belongsTo"] = base_command_unique_identifier

    def get_command_parameters_values(self, current_parameter_names, way_point):
        command_parameters_values = []

        for current_parameter_name in current_parameter_names:
            parameter_value = way_point.get_command_parameter_value(current_parameter_name)
            command_parameters_values.append(parameter_value)

        return command_parameters_values


    def get_next_way_point_coordinates(self, previous_coordinates, path_points):
        """ returns: [left_edge, top_edge]; the coordinates of the next way point. This is important because if one GUI way point command
            is the combination of 2+ Auto way point commands then those 2+ points need to be spaced apart otherwise the Auto code
            raises an error"""

        return_value = []
        previous_coordinates_index = path_points.index(previous_coordinates)

        for x in range(previous_coordinates_index, len(path_points)):
            new_coordinates = path_points[x]
            distance = math.dist(previous_coordinates, new_coordinates)

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


json_file_writer = JSONFileWriter()
