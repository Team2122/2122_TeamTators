import os

from miscellaneous.important_variables import *


def get_measurement(unit_of_measurement, amount):
    return unit_of_measurement / 100 * amount


def get_mouse_position():
    """returns: int[2] {mouse_left_edge, mouse_top_edge}; the mouse's position on the screen"""

    return [WINDOW.winfo_pointerx() - WINDOW.winfo_rootx(),
            WINDOW.winfo_pointery() - WINDOW.winfo_rooty()]

def get_lines(string):
    """returns: String[]; the lines contained within that string (each '/n' creates a new line). Every item in the list is a line"""

    current_line = ""
    lines = []
    enter = "\n"

    for ch in string:
        if ch == enter:
            lines.append(current_line)
            current_line = ""

        else:
            current_line += ch

    return lines + [current_line]  # The last line doesn't have an enter at the end, so adding that line here


# Conversions Functions
def pixels_to_meters(pixels):
    """returns: double; the meters value for the 'pixels'"""

    return pixels * PIXELS_TO_METERS_MULTIPLIER


def meters_to_pixels(meters):
    """returns: double; the pixels value for the 'meters'"""

    return METERS_TO_PIXELS_MULTIPLIER * meters


def truncate(number, decimal_places):
    """returns: number; the number to that many decimal places (it removes the other decimal places)"""

    # Getting the whole number with the decimals removed (to accuracy of decimal places) then making it go back
    # To the original decimal by dividing by 10^decimal_places
    return (number * pow(10, decimal_places) // 1) / pow(10, decimal_places)


def get_next_index(max_index, current_index):
    """returns: int; the next index after the 'current_index' and it does cycle 0 -> max_index -> 0 -> etc."""

    next_index = current_index + 1
    return next_index if next_index <= max_index else 0  # If the index is too big it should go back to 0


def get_previous_index(current_index):
    """returns: int; the previous index after the 'current_index' and it does cycle max_index -> 0 -> max_index -> etc."""

    return max(0, current_index - 1)  # The index should be at minimum 0


def swap_list_items(items, index1, index2):
    """Swaps the two indexes, so items[index1] = items[index2] and items[index2] = items[index1]"""

    temporary_item = items[index2]
    items[index2] = items[index1]
    items[index1] = temporary_item


def copy_list(items):
    """:returns: Object[]; The items that are at a new spot in memory"""

    return_value = []

    for item in items:
        return_value.append(item)

    return return_value


def get_index_of_range(range_lengths, number):
    """:returns: int; index of range"""

    index = -1
    start_time = 0

    for x in range(len(range_lengths)):
        end_time = start_time + range_lengths[x]

        if number >= start_time and number <= end_time:
            index = x

        start_time = end_time

    return index


def delete_file(file_path):
    """Deletes the file if the file exists"""

    if os.path.exists(file_path):
        os.remove(file_path)


def create_file(file_path):
    """Creates the file if the file does  not exist"""

    if os.path.exists(file_path):
        os.remove(file_path)

    file = open(file_path, "x")
    file.close()

def get_required_point_angle(required_point):
    """:returns: float; the angle of the required_point"""

    return float(required_point.get_command_parameter_values()[0])

def get_required_points(control_points):
    """:returns: ControlPoint[]; all the required points- ControlPoints with commands 'requiredPoint'"""

    return list(filter(lambda item: item.get_command_name() == "requiredPoint", control_points))
def get_control_points(control_points):
    """:returns: ControlPoint[]; all the control points- ControlPoints with a command that is not 'requiredPoint'"""

    return list(filter(lambda item: item.get_command_name() != "requiredPoint", control_points))

def get_dictionary_value(dictionary: dict, key, default_value):
    """:returns: Object; the value associated with that key if it exists otherwise it returns the default_value"""

    return default_value if not dictionary.__contains__(key) else dictionary[key]

def get_string_after(string, string_start):
    """:returns: str; the string after 'string_start'"""

    index = string.index(string_start)
    return string[index + 1:]

def get_string(string_list):
    """:returns: str; the string from all the string_list items"""

    return_value = ""

    for item in string_list:
        return_value += item

    return return_value