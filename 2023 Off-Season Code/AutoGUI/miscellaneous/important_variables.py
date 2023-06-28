import json
from tkinter import Entry, Button, PhotoImage, Tk, OptionMenu, Menu, Frame, Label, Canvas, ttk

from miscellaneous.colors import *
import json

configuration_file_storer = json.load(open("configurations/configuration_storer.json"))
configuration_file_path = configuration_file_storer.get("currentConfigurationFile")
configuration_file = json.load(open(configuration_file_path))

SCREEN_LENGTH = configuration_file.get("screenLength")
SCREEN_HEIGHT = configuration_file.get("screenHeight")

BACKGROUND_COLOR = dark_gray

# Window
WINDOW = Tk()
WINDOW.configure(bg=BACKGROUND_COLOR)
WINDOW.title('Auto GUI')
WINDOW.geometry(f'{SCREEN_LENGTH}x{SCREEN_HEIGHT}')

# Field Image
FIELD_IMAGE_LENGTH = configuration_file.get("fieldImageLength")
FIELD_IMAGE_HEIGHT = configuration_file.get("fieldImageHeight")

# Fonts
FONT_NAME = "Arial"
MINISCULE_FONT = [FONT_NAME, configuration_file.get("minisculeFontSize")]
TINY_FONT = [FONT_NAME, configuration_file.get("tinyFontSize")]
SMALL_FONT = [FONT_NAME, configuration_file.get("smallFontSize")]
NORMAL_FONT = [FONT_NAME, configuration_file.get("normalFontSize")]
LARGE_FONT = [FONT_NAME, configuration_file.get("largeFontSize")]

# Constants
METERS_TO_PIXELS_MULTIPLIER = 153 / 2.5146
PIXELS_TO_METERS_MULTIPLIER = 2.5146 / 153

CENTER_OF_FIELD_HORIZONTAL_OFFSET = FIELD_IMAGE_LENGTH / 2 * PIXELS_TO_METERS_MULTIPLIER  # pixels -> meters
CENTER_OF_FIELD_VERTICAL_OFFSET = FIELD_IMAGE_HEIGHT / 2 * PIXELS_TO_METERS_MULTIPLIER    # pixels -> meters

field_image_path = configuration_file.get("fieldImagePath")
INPUT_FIELD_DECIMAL_ACCURACY = 3

# Distance is in meters and the reason this is needed is because control_points can not be too close to each other because
# If they are then the Auto Code will raise an Error
MINIMUM_DISTANCE_BETWEEN_WAY_POINTS = 0.07

JSON_MAX_INDENT = 2
POINT_ALTERABLE_FIELDS_IN_FRAME = 18

# Robot Angle Lines
ROBOT_ANGLE_LINE_LENGTH = SCREEN_HEIGHT * .1
ROBOT_ANGLE_LINE_COLOR = purple
ROBOT_ANGLE_LINE_WIDTH = 8


class Points:
    """Stores all the points, so they do not have to be continually passed into a function"""

    way_points = []
    control_points = []

    def set_points(self, way_points, control_points):
        """Sets the point lists that this class stores"""

        self.way_points = way_points
        self.control_points = control_points

points = Points()




