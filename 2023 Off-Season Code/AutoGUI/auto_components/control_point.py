from auto_components.command_parameter_saver import CommandParameterSaver
from auto_components.movable_point import MovablePoint
from auto_components.drop_down_menu import DropDownMenu
from auto_components.input_field import InputField
from miscellaneous.popup_variables import *
from miscellaneous.important_variables import *
from auto_features.commands_retriever import commands_retriever


class ControlPoint(MovablePoint):
    """The points along the robot path that dictate the actions along the path"""
    
    command_parameter_saver = None
    adjusted_angle = None  # For the required points- they are different from the GUI angles (and they are in radians)

    def __init__(self, left_edge, top_edge, click_function, button_number, is_on_screen=True):
        """Initializes the object"""

        super().__init__(left_edge, top_edge, control_point_color, selected_control_point_color, click_function, button_number)

        self.speed_field = InputField(WINDOW, SMALL_FONT, 1.0)
        self.between_way_points_field = InputField(WINDOW, SMALL_FONT, "AUTO")
        self.command_drop_down_menu = DropDownMenu(WINDOW, 0, commands_retriever.get_command_names())

        self.user_modifiable_fields += [self.speed_field, self.between_way_points_field, self.command_drop_down_menu]
        self.set_command(commands_retriever.get_command_names()[0])  # So there is a value for current_command
        self.update_input_fields_belongs_to()

        self.command_parameter_saver = CommandParameterSaver()
        self.command_drop_down_menu.set_command(self.update_popup_window, [])

        # If the point is not on the screen, then it should be visible to the user (this point will be later destroyed),
        # But it still renders onto the screen if it is visible, which should not happen
        if not is_on_screen:
            self.place(x=0, y=0, width=0, height=0)

    def update_popup_window(self):
        """Updates the pop up window and updates the input field's values with the command_parameter_saver's values"""

        command_popup_window = commands_frame_saver.get_command_popup_window(self.get_command_name())
        command_popup_window.show()

        command_popup_window.set_input_fields_text(self.get_command_parameter_values())
        command_popup_window.set_save_button_command(self.save_command_parameter_values)

    def save_command_parameter_values(self):
        """Saves the values of the command parameters that were in the CommandPopupWindow"""

        command_popup_window = commands_frame_saver.get_command_popup_window(self.get_command_name())
        command_values = command_popup_window.get_input_field_values()
        self.command_parameter_saver.set_command_parameter_values(self.get_command_name(), command_values)

    def set_speed(self, speed):
        self.speed_field.set_text(speed)

    # Get commands
    def get_command_name(self):
        return self.command_drop_down_menu.get_selected_item()

    def set_command(self, value):
        self.command_drop_down_menu.set_selected_item(value)

    def get_speed(self):
        return float(self.speed_field.get_text())

    def get_between_way_points(self):
        """ :returns: str; what way_points the control point is between. There are three possible values:
            'AUTO': Let the GUI choose which point along the path the way point should be on
            'control_point_number1': The way point should be exactly on the control point of that number
            'control_point_number1'-'control_point_number2': The way point should be on the closes path point that is between those values"""

        return self.between_way_points_field.get_text()

    def set_between_way_points(self, value):
        self.between_way_points_field.set_text(value)

    def get_combined_command_parameter_names(self):
        """Calls the method of the same name in 'self.command_parameter_saver' and gives it self.get_command_name() as the parameter"""

        return commands_retriever.get_combined_command_parameter_names(self.get_command_name())

    def get_command_parameter_values(self):
        """Calls the method of the same name in 'self.command_parameter_saver' and gives it self.get_command_name() as the parameter"""

        return self.command_parameter_saver.get_command_parameter_values(self.get_command_name())

    def get_command_parameter_value(self, parameter_name):
        """Calls the method of the same name in 'self.command_parameter_saver' and gives it self.get_command_name() and 'parameter_name' as the parameters"""

        return self.command_parameter_saver.get_command_parameter_value(self.get_command_name(), parameter_name)

    def set_command_parameter_values(self, values):
        """Calls the method of the same name in 'self.command_parameter_saver' and gives it self.get_command_name() and 'values' as the parameters"""

        self.command_parameter_saver.set_command_parameter_values(self.get_command_name(), values)

