from auto_components.command_popup_window import CommandPopupWindow
from auto_components.main_popup_window import MainPopupWindow
from miscellaneous.important_variables import *
from auto_features.commands_retriever import commands_retriever


# MainPopUpWindow
main_popup_window = MainPopupWindow(WINDOW, SCREEN_LENGTH, SCREEN_HEIGHT)

# The data that stays constant between commands: length, height, main_popup_window, font
constant_command_data = [SCREEN_LENGTH, SCREEN_HEIGHT, main_popup_window, NORMAL_FONT]
command_popup_windows = {}

for command_name in commands_retriever.get_command_names():
    command_parameter_names = commands_retriever.get_combined_command_parameter_names(command_name)
    command_parameter_default_values = commands_retriever.get_command_parameter_default_values(command_name)

    command_popup_windows[command_name] = CommandPopupWindow(command_parameter_names, command_parameter_default_values, *constant_command_data)


def get_command_popup_window(command_name):
    """returns: CommandPopupWindow; the command popup window that is associated with that command_name"""

    return command_popup_windows.get(command_name)

