from important_variables import *


def get_measurement(unit_of_measurement, amount):
    return unit_of_measurement / 100 * amount


def get_mouse_position():
    return [window.winfo_pointerx() - window.winfo_rootx(),
            window.winfo_pointery() - window.winfo_rooty()]

def get_lines(string):
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
    return pixels * pixels_to_meters_multiplier


def meters_to_pixels(meters):
    return meters_to_pixels_multiplier * meters


def truncate(number, decimal_places):
    # Getting the whole number with the decimals removed (to accuracy of decimal places) then making it go back
    # To the original decimal by dividing by 10^decimal_places
    return (number * pow(10, decimal_places) // 1) / pow(10, decimal_places)


def get_next_index(max_index, current_index):
    next_index = current_index + 1
    return next_index if next_index <= max_index else 0  # If the index is too big it should go back to 0


def get_previous_index(current_index):
    return max(0, current_index - 1)  # The index should be at minimum 0


def swap(items, index1, index2):
    temporary_item = items[index2]
    items[index2] = items[index1]
    items[index1] = temporary_item






