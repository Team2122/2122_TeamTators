from auto_components.grid import Grid
from auto_components.input_field import InputField
from miscellaneous.important_variables import *


class Frame:
    """A specific part of the screen, which holds its own components"""

    current_items = []
    dimensions = []
    default_field = None

    def __init__(self, left_edge, top_edge, length, height, default_field_text, field_color=red, text_color=white):
        """Initializes the object"""

        self.set_size(left_edge, top_edge, length, height)
        print(default_field_text)
        self.default_field = InputField(WINDOW, SMALL_FONT, default_field_text, text_color=text_color, background_color=field_color)

    def set_size(self, left_edge, top_edge, length, height):
        """Sets the size of the frame with the numbers provided"""

        self.dimensions = [left_edge, top_edge, length, height]

    def show_items(self, items, show_items_function):
        """Shows the items on the Frame and removes the items that were previously on the Frame"""

        # Hides the other items
        for item in self.current_items:
            item.place(x=0, y=0, width=0, height=0)

        self.remove_default_field()

        show_items_function()

        self.current_items = items

    def get_grid_show_items(self, rows, columns, items):
        """returns: function; a function that turns the items provided into a grid with the correct dimensions"""

        grid = Grid(self.dimensions, rows, columns)
        return lambda: grid.turn_into_grid(items, None, None)

    def get_default_show_items(self):
        """returns: function; the default save functions (shows a field that reads: 'Use Dropdown To Select Command With Parameters To Edit'"""

        return lambda: self.default_field.place(x=self.dimensions[0], y=self.dimensions[1], width=self.dimensions[2], height=self.dimensions[3])

    def default_show_items(self):
        """Shows the default items when nothing is on the frame"""

        show_items_function = self.get_default_show_items()
        self.show_items([self.default_field], show_items_function)

    def remove_default_field(self):
        """Removes the default field from the screen"""

        self.default_field.place(x=0, y=0, width=0, height=0)