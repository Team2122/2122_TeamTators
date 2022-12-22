import tkinter
from math import ceil

from auto_components.input_field import InputField
from auto_components.pop_up_window import PopUpWindow
from auto_components.grid import Grid
from miscellaneous.utility_functions import get_measurement
from miscellaneous.important_variables import *


class CommandPopupWindow(PopUpWindow):
    """A PopUpWindow that is specifically for commands on the Auto Path"""

    grid_columns = 3
    length = 0
    height = 0
    title_field_height = 0
    buffer_between_titles_and_input_fields = 0
    save_button_height = 0

    title_fields = []
    input_fields = []
    names = []

    save_button = None
    command = lambda: ""

    def __init__(self, names, default_values, length, height, main_popup_window, font):
        """Initializes the object"""

        self.length, self.height = length, height
        self.title_fields = []
        self.input_fields = []
        self.names = names

        self.title_field_height = get_measurement(self.height, 10)
        self.buffer_between_titles_and_input_fields = get_measurement(self.height, 4)
        self.save_button_height = get_measurement(self.height, 10)

        for x in range(len(names)):
            self.title_fields.append(InputField(main_popup_window, font, names[x], False))
            self.input_fields.append(InputField(main_popup_window, font, default_values[x], True))

        self.save_button = Button(main_popup_window, compound=tkinter.CENTER, text="Save", bg=pleasing_green, fg=white, font=LARGE_FONT, command=self.handle_save_button_click)
        super().__init__(self.title_fields + self.input_fields + [self.save_button], main_popup_window, self.create_grids)

    def create_grids(self):
        """Creates the grids that defines the layout of the command popup window"""

        # If there aren't any items to show for a PopUpWindow, then it shouldn't show anything
        if len(self.names) != 0:
            number_of_grid_rows = ceil(len(self.names) / self.grid_columns)
            title_fields_total_height = number_of_grid_rows * self.title_field_height
            total_height_buffer = title_fields_total_height + self.buffer_between_titles_and_input_fields

            title_field_grid = Grid([0, 0, self.length, title_fields_total_height], None, self.grid_columns)
            input_field_grid = Grid([0, total_height_buffer, self.length, self.height - total_height_buffer - self.save_button_height], None, self.grid_columns)

            title_field_grid.turn_into_grid(self.title_fields, None, self.title_field_height)
            input_field_grid.turn_into_grid(self.input_fields, None, self.title_field_height)
            self.save_button.place(x=0, y=self.height - self.save_button_height, width=self.length, height=self.save_button_height)

    def set_input_fields_text(self, values):
        """Sets the text of the input fields (the command parameter values)"""

        for x in range(len(self.input_fields)):
            self.input_fields[x].set_text(values[x])

    def set_title_fields_text(self, values):
        """Sets the text of the text fields (the command parameter names)"""

        for x in range(len(self.title_fields)):
            self.title_fields[x].set_text(values[x])

    def get_input_field_values(self):
        """returns: String[]; the values of the input fields (command parameter values)"""

        return [input_field.get_text() for input_field in self.input_fields]

    def set_save_button_command(self, command):
        """Sets the function that is called when the save button is clicked"""

        self.command = command

    def handle_save_button_click(self):
        """Calls the function 'command' when the save button is clicked"""

        self.command()



