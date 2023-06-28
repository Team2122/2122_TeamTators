import tkinter

from auto_components.grid_items import GridItems
from auto_components.input_field import InputField
from auto_features.path_creation import get_hub_centric_coordinates
from miscellaneous.utility_functions import *


class MovablePoint(Button):
    """Any point that the user can move and modify the values of the fields (WayPoint, ControlPoint, etc.)"""
    
    # Miscellaneous
    color = None
    base_color = None
    selected_color = None
    user_modifiable_fields_grid = None
    order_position_field = None
    click_function = None
    left_edge = 0
    top_edge = 0
    order_position = 0
    left_edge_meters = 0
    top_edge_meters = 0

    # Alterable Data
    user_modifiable_fields = []
    left_edge_field = None
    top_edge_field = None
    order_position_field = None

    # Size
    base_length = get_measurement(SCREEN_LENGTH, .9)
    base_height = base_length

    is_needed = True

    def __init__(self, left_edge, top_edge, base_color, selected_color, click_function, button_number):
        """Initializes the object"""
        
        self.color, self.base_color = base_color, base_color
        self.selected_color = selected_color

        self.left_edge_field = InputField(WINDOW, SMALL_FONT, "x")
        self.top_edge_field = InputField(WINDOW, SMALL_FONT, "y")
        self.order_position_field = InputField(WINDOW, SMALL_FONT, "")
        self.user_modifiable_fields = [self.order_position_field, self.left_edge_field, self.top_edge_field]

        self.click_function = click_function

        super().__init__(WINDOW, bg=base_color, compound=tkinter.CENTER, command=lambda: click_function(self),
                         text=button_number, fg=white, font=MINISCULE_FONT)
        #
        # MAKE SURE THIS IS BEFORE PLACING
        #
        self.user_modifiable_fields_grid = GridItems(self.user_modifiable_fields, GridItems.horizontal_grid)

        self.place(x=left_edge, y=top_edge, width=self.base_length, height=self.base_height)

    def point_user_alterable_fields(self):
        """returns: GridItems; the grid containing all the user modifiable fields"""
        
        return self.user_modifiable_fields_grid

    def place(self, **kwargs):
        """Places the object at that location (x, y, width, height)"""

        self.left_edge = kwargs.get("x")
        self.top_edge = kwargs.get("y")
        self.update_input_fields()

        kwargs["x"] = int(self.left_edge)
        kwargs["y"] = int(self.top_edge)

        super().place(kwargs)

    def __str__(self):
        return str(id(self))

    def default_update_coordinates(self):
        """Moves the component to where the input fields are saying it should be"""

        field_top_edge = self.get_field_top_edge()
        field_left_edge = self.get_field_left_edge()

        left_edge_meters = self.get_field_left_edge()
        top_edge_meters = self.get_field_top_edge()

        top_edge_meters *= -1

        left_edge_meters += CENTER_OF_FIELD_HORIZONTAL_OFFSET
        top_edge_meters += CENTER_OF_FIELD_VERTICAL_OFFSET

        left_edge = meters_to_pixels(left_edge_meters)
        top_edge = meters_to_pixels(top_edge_meters)

        self.place(x=left_edge, y=top_edge, width=self.base_length, height=self.base_height)

        # So the coordinates do not change because of a rounding error
        self.set_field_top_edge(field_top_edge)
        self.set_field_left_edge(field_left_edge)

    def update_input_fields(self):
        """Updates the input fields, so they reflect where the component is on the screen"""

        left_edge = pixels_to_meters(self.left_edge)
        top_edge = pixels_to_meters(self.top_edge)

        left_edge, top_edge = get_hub_centric_coordinates(left_edge, top_edge)

        self.set_field_left_edge(truncate(left_edge, INPUT_FIELD_DECIMAL_ACCURACY))
        self.set_field_top_edge(truncate(top_edge, INPUT_FIELD_DECIMAL_ACCURACY))

    # The reason these methods are needed is because the input field's sometimes have false data
    def get_left_edge(self):
        return self.left_edge

    def get_top_edge(self):
        return self.top_edge
    
    def get_field_left_edge(self):
        return float(self.left_edge_field.get_text())

    def get_field_top_edge(self):
        return float(self.top_edge_field.get_text())
    
    def set_field_left_edge(self, value):
        self.left_edge_field.set_text(value)    
        
    def set_field_top_edge(self, value):
        self.top_edge_field.set_text(value)

    def set_order_position(self, order_position):
        """Sets the position of the point (point #1, point #2, etc.)"""

        self.configure(text=order_position)
        self.order_position_field.set_text(order_position)
        self.order_position = order_position

    def destroy(self):
        """Overrides tkinter's method: removes the button and all the components of the button from the screen"""

        for field in self.user_modifiable_fields:
            field.destroy()

        super().destroy()

    
    def get_order_position(self):
        """returns: int; the position of the point (point #1, point #2, etc.)"""
        
        return self.order_position

    def set_position_field_text(self, value):
        self.order_position_field.configure(text=value)

    def get_position_field_text(self):
        return self.order_position_field.get_text()

    def position_field_is_selected(self):
        return self.order_position_field.is_selected
    
    def get_input_fields(self):
        """returns: InputField; all the input fields that this point has"""
    
        return list(filter(lambda item: isinstance(item, InputField), self.user_modifiable_fields))

    def set_input_fields_command(self, command):
        """Makes all the input fields call the function 'command' when they are clicked"""
        
        for input_field in self.get_input_fields():
            input_field.set_command(command)

    def update_input_fields_belongs_to(self):
        """So the Main Screen can know what the input field's belong to (a WayPoint for example)"""

        for input_field in self.get_input_fields():
            input_field.set_belongs_to(self)

    def select(self):
        """Makes the point change to the selected color, so the user knows the point is selected"""

        self.configure(bg=self.selected_color)

    def unselect(self):
        """Makes the point go back to the base color, so the user knows the point is not selected"""

        self.configure(bg=self.base_color)

    def __str__(self):
        """FOR DEBUGGING"""

        return str(self.get_field_top_edge())

