import json
from tkinter import Entry, Button, PhotoImage, Tk, OptionMenu, Menu, Frame, Label, Canvas, ttk

from miscellaneous.colors import *

SCREEN_LENGTH = 1200
SCREEN_HEIGHT = 600

BACKGROUND_COLOR = dark_gray

# Window
WINDOW = Tk()
WINDOW.configure(bg=BACKGROUND_COLOR)
WINDOW.title('Auto GUI')
WINDOW.geometry(f'{SCREEN_LENGTH}x{SCREEN_HEIGHT}')

# Fonts
FONT_NAME = "Arial"
MINISCULE_FONT = [FONT_NAME, 5]
TINY_FONT = [FONT_NAME, 8]
SMALL_FONT = [FONT_NAME, 11]
NORMAL_FONT = [FONT_NAME, 22]
LARGE_FONT = [FONT_NAME, 27]

# Constants
METERS_TO_PIXELS_MULTIPLIER = 223.9/3.401822
PIXELS_TO_METERS_MULTIPLIER = 3.401822/223.9

CENTER_OF_FIELD_HORIZONTAL_OFFSET = 502 * PIXELS_TO_METERS_MULTIPLIER  # pixels -> meters
CENTER_OF_FIELD_VERTICAL_OFFSET = 283 * PIXELS_TO_METERS_MULTIPLIER    # pixels -> meters

INPUT_FIELD_DECIMAL_ACCURACY = 3

# Distance is in meters and the reason this is needed is because way_points can not be too close to each other because
# If they are then the Auto Code will raise an Error
MINIMUM_DISTANCE_BETWEEN_WAY_POINTS = .02

JSON_MAX_INDENT = 2
POINT_ALTERABLE_FIELDS_IN_FRAME = 18








