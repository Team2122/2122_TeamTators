from auto_components.command_popup_window import CommandPopupWindow
from auto_components.main_popup_window import MainPopupWindow
from important_variables import *

# MainPopUpWindow
main_popup_window = MainPopupWindow(window, screen_length, screen_height)

# command_names: Lambda Lock | Shoot | Pick | Goto | None | Print | Required

# The data that stays constant between commands: length, height, main_popup_window, font
constant_command_data = [screen_length, screen_height, main_popup_window, normal_font]
command_popup_windows = []

for x in range(len(command_names)):
    argument_names = command_argument_names[x]
    argument_default_values = command_argument_default_values[x]

    command_popup_windows.append(CommandPopupWindow(argument_names, argument_default_values, *constant_command_data))

def get_command_popup_window(command_name):
    index = command_names.index(command_name)

    return command_popup_windows[index]

