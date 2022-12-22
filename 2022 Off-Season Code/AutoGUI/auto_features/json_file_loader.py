from auto_components.control_point import ControlPoint
from auto_components.way_point import WayPoint
from miscellaneous.important_variables import *
from auto_features.commands_retriever import commands_retriever


class JSONFileLoader:
    """Loads the JSON files the JSONFileWriter writes, so the Auto GUI application can save progress"""

    control_points_json = None
    required_points_json = None
    way_points_json = None
    point_click_function = None

    def load_json_file(self, json_file):
        """Loads the JSON file, so the control_points, required_points, and way_points can be set"""

        json_contents = json.load(json_file)
        self.control_points_json = json_contents["ControlPoints"]
        self.required_points_json = json_contents["RequiredPoints"]
        self.way_points_json = json_contents["WayPoints"]

    def set_all_points_to_reflect_json_file(self, control_points, way_points, json_file, point_click_function):
        """Updates all points on the screen (control_points, required_points, and way_points) to reflect what is in the JSON file"""

        self.load_json_file(json_file)
        self.point_click_function = point_click_function

        self.set_control_points_to_reflect_json_file(control_points)
        self.set_way_points_to_reflect_json_file(way_points)

    def set_control_points_to_reflect_json_file(self, control_points):
        """Updates the control points list to reflect what is in the JSON file"""

        for x in range(len(self.control_points_json)):
            # Adding the control point to control points
            current_control_point = ControlPoint(0, 0, self.point_click_function, x)
            control_points.append(current_control_point)

            # Updating the control point's attributes, so it reflects the JSON file
            current_control_point_json = self.control_points_json[x]
            self.set_point_position(current_control_point, current_control_point_json)
            current_control_point.set_vertical_velocity(float(current_control_point_json["Vy"]))
            current_control_point.set_horizontal_velocity(float(current_control_point_json["Vx"]))

    def set_point_position(self, point, point_json):
        """Sets the point's position to reflect the JSON for the point"""

        point.set_field_left_edge(float(point_json["X"]))
        point.set_field_top_edge(float(point_json["Y"]))

    def set_way_points_to_reflect_json_file(self, way_points):
        """ Way Points and Required Points are under the same umbrella of 'Way Points,' so it will update only
            the way points list to reflect what is in the JSON file, which has 'WayPoints' and 'RequiredPoints'"""

        current_way_point_number = 0
        way_point_number = 1

        while current_way_point_number < len(self.way_points_json):
            current_way_point_json = self.way_points_json[current_way_point_number]
            way_point = WayPoint(0, 0, self.point_click_function, way_point_number)
            way_points.append(way_point)
            self.set_point_position(way_point, current_way_point_json)

            way_point_command_name = current_way_point_json["Command"]
            self.set_way_point_command_to_reflect_json_file(way_point, current_way_point_json, current_way_point_number)
            number_of_additional_command_components = self.get_number_of_additional_way_point_command_components(way_point_command_name)

            current_way_point_number += 1 + number_of_additional_command_components
            way_point_number += 1

        for x in range(len(self.required_points_json)):
            current_way_point = WayPoint(0, 0, self.point_click_function, way_point_number + 1)
            way_points.append(current_way_point)

            current_way_point_json = self.required_points_json[x]
            self.set_point_position(current_way_point, current_way_point_json)
            current_way_point.set_command("requiredPoint")

    def set_way_point_command_to_reflect_json_file(self, way_point, current_way_point_json, current_way_point_number):
        """Sets the command of the way point to reflect what is in the json file (it will also combine the command components)"""

        way_point_command_name = current_way_point_json["Command"]
        way_point.set_command(way_point_command_name)

        current_command_parameters = current_way_point_json["args"]
        current_command_parameter_values = list(current_command_parameters.values())

        number_of_additional_command_components = self.get_number_of_additional_way_point_command_components(way_point_command_name)

        # Adding one to get to the next indexes past the original index
        for j in range(current_way_point_number + 1, current_way_point_number + number_of_additional_command_components + 1):
            way_point_component_data = self.way_points_json[j]
            component_parameters = way_point_component_data["args"]
            component_command_parameter_values = list(component_parameters.values())
            current_command_parameter_values += component_command_parameter_values

        way_point.set_command_parameter_values(current_command_parameter_values)

    def get_number_of_additional_way_point_command_components(self, way_point_command_name):
        """returns: int; the number of components a command has"""

        # If it is not a combined command it has no more additional components, but if it is a combined command it will
        number_of_additional_command_components = 0
        if commands_retriever.combined_commands.__contains__(way_point_command_name):
            # One of a command's components is itself, so one must be subtracted
            number_of_additional_command_components = len(commands_retriever.combined_commands.get(way_point_command_name)) - 1

        return number_of_additional_command_components


json_file_loader = JSONFileLoader()

