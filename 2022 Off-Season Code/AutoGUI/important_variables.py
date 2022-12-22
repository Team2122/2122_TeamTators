import json
from tkinter import Entry, Button, PhotoImage, Tk, OptionMenu, Menu, Frame, Label, Canvas, ttk

from colors import *

screen_length = 1200
screen_height = 600

background_color = dark_gray
# Window
window = Tk()
window.configure(bg=background_color)
window.title('Auto GUI')
window.geometry(f'{screen_length}x{screen_height}')

# Other stuff
pixelVirtual = PhotoImage(width=1, height=1)
font_name = "Arial"
small_font = [font_name, 11]
normal_font = [font_name, 22]
tiny_font = [font_name, 8]
miniscule_font = [font_name, 5]
large_font = [font_name, 27]

# Constants
meters_to_pixels_multiplier = 223.9/3.401822
pixels_to_meters_multiplier = 3.401822/223.9

center_of_field_horizontal_offset = 502 * pixels_to_meters_multiplier  # pixels -> meters
center_of_field_vertical_offset = 283 * pixels_to_meters_multiplier    # pixels -> meters

input_field_decimal_accuracy = 3

# Distance is in meters and the reason this is needed is because way_points can not be too close to each other because
# If they are then the Auto Code will raise an Error
min_distance_between_way_points = .05

# Commands
print(open("commands.txt", "r").read()[112:120])
commands_json_data = json.load(open("commands.txt", "r"))

command_names = []
# Does not include the combined_command's arguments
command_argument_names = []
command_argument_base_names = []  # Commands that are not combined
command_argument_default_values = []
command_argument_types = []

# NOTE: This dictionary is for implementation as far as the GUI is concerned. The Autonomous generation code (the other side that
# is not the GUI) will have all the commands in commands.txt However, to make the GUI more usable some commands will be
# automatically applied to make the GUI easier to use. The key in the dictionary is the command that is a combination of
# commands and the value is the commands it is a combination of. Also, the order is the order the commands will be applied
combined_commands = {
    "autoShoot": ["autoShoot", "lambdaLock", "unShoot"],
    "pickRight": ["pickRight", "requiredPoint"],
    "pickLeft": ["pickLeft", "requiredPoint"],
}

for command_name in commands_json_data.keys():
    command_names.append(command_name)

    argument_names = []
    argument_default_values = []
    command_arguments = []
    argument_types = []

    # The command names should be what it is a combination of if it is a combination of commands,
    # otherwise it should just be the normal command
    additional_command_names = combined_commands.get(command_name) if combined_commands.__contains__(command_name) else [command_name]

    for name in additional_command_names:
        command_arguments += commands_json_data.get(name)

    for command_argument in command_arguments:
        # If the command.txt file is not correct then it should be fixed
        if command_argument.get("name") is None or command_argument.get("defaultValue") is None:
            index = command_arguments.index(command_argument)
            raise ValueError(f"JSON list arg {additional_command_names[index]} does not have both 'name' and 'defaultValue'")

        argument_names.append(command_argument.get("name"))
        argument_default_values.append(command_argument.get("defaultValue"))
        argument_types.append(command_argument.get("type"))

    base_argument_names = []
    for command_argument in commands_json_data.get(command_name):
        base_argument_names.append(command_argument.get("name"))

    command_argument_base_names.append(base_argument_names)
    command_argument_names.append(argument_names)
    command_argument_default_values.append(argument_default_values)
    command_argument_types.append(argument_types)






