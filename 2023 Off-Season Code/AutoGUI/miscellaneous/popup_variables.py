from auto_components.command_popup_window import CommandPopupWindow
from auto_components.main_popup_window import MainPopupWindow
from miscellaneous.important_variables import *
from auto_features.commands_retriever import commands_retriever
from auto_components.frame import Frame


class CommandsFrameSaver:
    """Saves all the frames and allows the retieval of frames"""

    command_popup_windows = {}
    commands_main_frame = Frame(0, 0, 0, 0, "Edit Commands With Parameters Using the Dropdown")

    def get_commands_main_frame(self):
        return self.commands_main_frame

    def create_commands_frame(self, left_edge, top_edge, length, height):
        """Creates all the commands frames"""

        # MainPopUpWindow
        self.commands_main_frame.set_size(left_edge, top_edge, length, height)

        # The data that stays constant between commands: length, height, commands_main_frame, font
        constant_command_data = [commands_main_frame, SMALL_FONT]

        for command_name in commands_retriever.get_command_names():
            command_parameter_names = commands_retriever.get_combined_command_parameter_names(command_name)
            command_parameter_default_values = commands_retriever.get_command_parameter_default_values(command_name)

            self.command_popup_windows[command_name] = CommandPopupWindow(command_parameter_names, command_parameter_default_values, *constant_command_data)

    def get_command_popup_window(self, command_name):
        """returns: CommandPopupWindow; the command popup window that is associated with that command_name"""

        return self.command_popup_windows.get(command_name)

commands_frame_saver = CommandsFrameSaver()

# The pointer to the object is initialized, then the object is modified when the MainScreen calls the method 'create_commands_frame'
commands_main_frame = commands_frame_saver.get_commands_main_frame()
