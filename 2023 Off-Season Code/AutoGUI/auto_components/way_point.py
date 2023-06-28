from auto_components.drop_down_menu import DropDownMenu
from auto_components.movable_point import MovablePoint
from auto_components.input_field import InputField
from miscellaneous.important_variables import *


class WayPoint(MovablePoint):
    """The points that dictate the path of the robot"""

    angle_at_point = 0
    path_index = 0

    def __init__(self, left_edge, top_edge, click_function, button_number):
        """Initializes the object"""

        super().__init__(left_edge, top_edge, way_point_color, selected_way_point_color, click_function, button_number)

        self.vertical_velocity_field = InputField(WINDOW, SMALL_FONT, "1")
        self.horizontal_velocity_field = InputField(WINDOW, SMALL_FONT, "1")
        self.spline_order_drop_down = DropDownMenu(WINDOW, 0, ["1", "5"])
        self.user_modifiable_fields += [self.horizontal_velocity_field, self.vertical_velocity_field, self.spline_order_drop_down]

        self.update_input_fields_belongs_to()

    def set_horizontal_velocity(self, value):
        self.horizontal_velocity_field.set_text(value)

    def set_vertical_velocity(self, value):
        self.vertical_velocity_field.set_text(value)

    def set_spline_order(self, value):
        self.spline_order_drop_down.set_selected_item(value)

    def get_horizontal_velocity(self):
        return float(self.horizontal_velocity_field.get_text())

    def get_vertical_velocity(self):
        return float(self.vertical_velocity_field.get_text())

    def get_spline_order(self):
        return int(self.spline_order_drop_down.get_selected_item())
    def get_angle_at_point(self):
        """:returns: float; the angle of the robot at this point"""

        return self.angle_at_point

    def set_angle_at_point(self, angle):
        """Sets the angle of the robot at this point"""

        self.angle_at_point = angle