from auto_components.movable_point import MovablePoint
from auto_components.input_field import InputField
from miscellaneous.important_variables import *


class ControlPoint(MovablePoint):
    """The points that dictate the path of the robot"""

    def __init__(self, left_edge, top_edge, click_function, button_number):
        """Initializes the object"""

        super().__init__(left_edge, top_edge, control_point_color, selected_control_point_color, click_function, button_number)

        self.vertical_velocity_field = InputField(WINDOW, SMALL_FONT, "0")
        self.horizontal_velocity_field = InputField(WINDOW, SMALL_FONT, "0")
        self.user_modifiable_fields += [self.horizontal_velocity_field, self.vertical_velocity_field]

        self.update_input_fields_belongs_to()

    def set_horizontal_velocity(self, value):
        self.horizontal_velocity_field.set_text(value)

    def set_vertical_velocity(self, value):
        self.vertical_velocity_field.set_text(value)

    def get_horizontal_velocity(self):
        return float(self.horizontal_velocity_field.get_text())

    def get_vertical_velocity(self):
        return float(self.vertical_velocity_field.get_text())