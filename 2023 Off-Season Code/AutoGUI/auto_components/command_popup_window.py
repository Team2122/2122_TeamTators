import tkinter
from math import ceil

from auto_components.input_field import InputField
from auto_components.pop_up_window import PopUpWindow
from auto_components.grid import Grid
from auto_components.titled_input_field import TitledInputField
from miscellaneous.utility_functions import get_measurement
from miscellaneous.important_variables import *
from auto_components.frame import Frame


class CommandPopupWindow(PopUpWindow):
    """A PopUpWindow that is specifically for commands on the Auto Path"""

    grid_columns = 3
    length = 0
    height = 0
    title_field_height = 0
    buffer_between_titles_and_input_fields = 0
    save_button_height = 0
    titled_input_fields = []

    names = []
    grid = None

    save_button = None
    command = lambda: ""

    def __init__(self, names, default_values, commands_main_frame: Frame, font):
        """Initializes the object"""

        self.names = names
        self.save_button_height = get_measurement(self.height, 10)
        self.titled_input_fields = []

        for x in range(len(names)):
            self.titled_input_fields.append(TitledInputField(WINDOW, font, default_values[x], names[x]))

        self.save_button = Button(WINDOW, compound=tkinter.CENTER, text="Save", bg=pleasing_green, fg=white, font=SMALL_FONT, command=self.handle_save_button_click)

        # The usual show items function is the function that is used when there are names
        # (what is usually displayed, but if there are no names it does not make sense to have a giant save button)
        usual_show_items_functions = commands_main_frame.get_grid_show_items(1, None, self.titled_input_fields + [self.save_button])

        show_items_function = usual_show_items_functions if len(names) > 0 else commands_main_frame.get_default_show_items()
        super().__init__(self.titled_input_fields + [self.save_button], commands_main_frame, show_items_function)

    def set_input_fields_text(self, values):
        """Sets the text of the input fields (the command parameter values)"""

        for x in range(len(self.titled_input_fields)):
            self.titled_input_fields[x].set_text(values[x])

    def set_title_fields_text(self, values):
        """Sets the text of the text fields (the command parameter names)"""

        for x in range(len(self.titled_input_fields)):
            self.titled_input_fields[x].set_title(values[x])

    def get_input_field_values(self):
        """returns: String[]; the values of the input fields (command parameter values)"""

        return [input_field.get_text() for input_field in self.titled_input_fields]

    def set_save_button_command(self, command):
        """Sets the function that is called when the save button is clicked"""

        self.command = command

    def handle_save_button_click(self):
        """Calls the function 'command' when the save button is clicked"""

        self.command()



