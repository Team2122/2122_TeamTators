from auto_components.way_point import WayPoint
from auto_components.control_point import ControlPoint
from miscellaneous.important_variables import *
from auto_features.commands_retriever import commands_retriever
from miscellaneous.utility_functions import *


class JSONFileLoader:
    """Loads the JSON files the JSONFileWriter writes, so the Auto GUI application can save progress"""

    way_points_json = None
    required_points_json = None
    control_points_json = None
    point_click_function = None

    def load_json_file(self, json_contents):
        """Loads the JSON file, so the way_points, required_points, and control_points can be set"""

        # How the GUI and Auto Follower name points is different, which is why these names are different
        self.way_points_json = json_contents["ControlPoints"]
        self.required_points_json = json_contents["RequiredPoints"]
        self.control_points_json = json_contents["WayPoints"]

    def set_all_points_to_reflect_json_file(self, way_points, control_points, json_contents, point_click_function):
        """Updates all points on the screen (way_points, required_points, and control_points) to reflect what is in the JSON file"""

        self.load_json_file(json_contents)
        self.point_click_function = point_click_function

        self.set_way_points_to_reflect_json_file(way_points)
        self.set_control_points_to_reflect_json_file(control_points)

    def set_way_points_to_reflect_json_file(self, way_points):
        """Updates the control points list to reflect what is in the JSON file"""

        for x in range(len(self.way_points_json)):
            # Adding the control point to control points
            current_way_point = WayPoint(0, 0, self.point_click_function, x)
            way_points.append(current_way_point)

            # Updating the control point's attributes, so it reflects the JSON file
            current_way_point_json = self.way_points_json[x]
            self.set_point_position(current_way_point, current_way_point_json)
            current_way_point.set_vertical_velocity(float(current_way_point_json["Vy"]))
            current_way_point.set_horizontal_velocity(float(current_way_point_json["Vx"]))
            current_way_point.set_spline_order(current_way_point_json["order"])

    def set_point_position(self, point, point_json):
        """Sets the point's position to reflect the JSON for the point"""

        point.set_field_left_edge(float(point_json["X"]))
        point.set_field_top_edge(float(point_json["Y"]))

    def set_control_points_to_reflect_json_file(self, control_points):
        """ Control Points and Required Points are under the same umbrella of 'Control Points,' so it will update only
            the way points list to reflect what is in the JSON file, which has 'ControlPoints' and 'RequiredPoints'"""

        current_control_point_number = 0
        control_point_number = 1

        while control_point_number < len(self.control_points_json):
            current_control_point_json = self.control_points_json[current_control_point_number]
            if current_control_point_json["isNeeded"]:
                control_point = ControlPoint(0, 0, self.point_click_function, control_point_number)
                control_points.append(control_point)
                self.set_point_position(control_point, current_control_point_json)

                control_point_command_name = current_control_point_json["Command"]
                self.set_control_point_command_to_reflect_json_file(control_point, current_control_point_json, current_control_point_number)
                number_of_additional_command_components = self.get_number_of_additional_control_point_command_components(control_point_command_name)

                between_way_points_value = get_dictionary_value(current_control_point_json, "betweenWayPoints", "AUTO")
                control_point.set_between_way_points(between_way_points_value)
                current_control_point_number += 1 + number_of_additional_command_components

            control_point_number += 1

        for x in range(len(self.required_points_json)):
            current_control_point_json = self.required_points_json[x]

            # Determines if this point is specific to the GUI or the Auto Follower. If it belongs to the Auto Follower, then
            # That point should not be added
            if current_control_point_json["isNeeded"]:
                current_control_point = ControlPoint(0, 0, self.point_click_function, control_point_number + 1)
                control_points.append(current_control_point)

                between_way_points_value = get_dictionary_value(current_control_point_json, "betweenWayPoints", "AUTO")
                current_control_point.set_between_way_points(between_way_points_value)

                self.set_point_position(current_control_point, current_control_point_json)
                current_control_point.set_command("requiredPoint")
                current_control_point.set_command_parameter_values([current_control_point_json["args"]["angle-GUI"]])

    def set_control_point_command_to_reflect_json_file(self, control_point, current_control_point_json, current_control_point_number):
        """Sets the command of the way point to reflect what is in the json file (it will also combine the command components)"""

        control_point_command_name = current_control_point_json["Command"]
        control_point.set_command(control_point_command_name)

        current_command_parameters = current_control_point_json["args"]
        current_command_parameter_values = list(current_command_parameters.values())

        number_of_additional_command_components = self.get_number_of_additional_control_point_command_components(control_point_command_name)

        # Adding one to get to the next indexes past the original index
        for j in range(current_control_point_number + 1, current_control_point_number + number_of_additional_command_components + 1):
            control_point_component_data = self.control_points_json[j]
            component_parameters = control_point_component_data["args"]
            component_command_parameter_values = list(component_parameters.values())
            current_command_parameter_values += component_command_parameter_values

        control_point.set_command_parameter_values(current_command_parameter_values)

    def get_number_of_additional_control_point_command_components(self, control_point_command_name):
        """returns: int; the number of components a command has"""

        # If it is not a combined command it has no more additional components, but if it is a combined command it will
        number_of_additional_command_components = 0
        if commands_retriever.combined_commands.__contains__(control_point_command_name):
            # One of a command's components is itself, so one must be subtracted
            number_of_additional_command_components = len(commands_retriever.combined_commands.get(control_point_command_name)) - 1

        return number_of_additional_command_components


json_file_loader = JSONFileLoader()

