from math import ceil

from auto_components.grid_items import GridItems
from auto_components.input_field import InputField
from auto_components.grid import Grid
from miscellaneous.important_variables import *


class PointAlterableFieldsFrame:
    """The frame that holds all the fields that the user can modify"""
    
    points_list = []
    user_modifiable_field_grids = []  # The grids that contain all the user alterable fields the user can modify
    all_fields = []  # The user alterable fields (not the grids containing them) and the title fields
    bounds_when_visible = []  # The bounds [x, y, length, height] of this object when it is visible
    bounds_when_invisible = [0, 0, 0, 0]  # The bounds [x, y, length, height] of this object when it is invisible
    current_bounds = []  # The current bounds [x, y, length, height] of this object
    title_field_grid_items = None
    names = ["Pt #", "X", "Y"]
    all_names = []
    title_fields = []

    def __init__(self, points_list, additional_names):
        """Initializes the object"""

        self.points_list = points_list
        self.bounds = []
        self.title_fields = []

        self.all_names = self.names + additional_names

        for name in self.all_names:
            self.title_fields += [InputField(WINDOW, TINY_FONT, name, False, background_color=black, text_color=white)]

        self.title_field_grid_items = GridItems(self.title_fields, GridItems.horizontal_grid)

    def place(self, **kwargs):
        """Places this object at the location provided in **kwargs (x, y, width, height)"""

        self.bounds_when_visible = [kwargs.get("x"), kwargs.get("y"), kwargs.get("width"), kwargs.get("height")]
    
    def set_up_for_turning_user_modifiable_fields_into_grids(self):
        """Sets up the gui component lists, so 'self.update()' can put the user_alterable_fields into grids"""
        
        self.user_modifiable_field_grids = []
        self.user_modifiable_field_grids.append(self.title_field_grid_items)
        self.all_fields = []

        for x in range(len(self.points_list)):
            point = self.points_list[x]

            point.set_order_position(x + 1)
            point_user_alterable_fields = point.point_user_alterable_fields()
            self.user_modifiable_field_grids.append(point_user_alterable_fields)
            self.all_fields += point_user_alterable_fields.items

    def update(self):
        """Updates this object so the user_alterable_fields are in accordance to the points on the screen"""

        self.set_up_for_turning_user_modifiable_fields_into_grids()
        self.turn_user_modifiable_fields_into_grids()
    
    def turn_user_modifiable_fields_into_grids(self):
        """Turns all the user_modifiable_fields into grids"""

        left_edge, top_edge, length, height = self.bounds

        for x in range(ceil(len(self.user_modifiable_field_grids) / POINT_ALTERABLE_FIELDS_IN_FRAME)):
            if x > 0:
                top_edge += height

            bounds = [left_edge, top_edge, length, height]
            start_index = x * POINT_ALTERABLE_FIELDS_IN_FRAME
            end_index = x * POINT_ALTERABLE_FIELDS_IN_FRAME + POINT_ALTERABLE_FIELDS_IN_FRAME + 1
            max_index = len(self.user_modifiable_field_grids)

            if end_index >= max_index:
                end_index = max_index

            grid = Grid(bounds, None, 1)
            # So they don't change size depending on how many objects there are
            max_height = grid.get_item_dimension(grid.height, POINT_ALTERABLE_FIELDS_IN_FRAME, None, grid.height_buffer)
            grid.turn_into_grid(self.user_modifiable_field_grids[start_index:end_index], None, max_height)
        
    def hide(self):
        """Makes this object invisible"""
        
        self.bounds = self.bounds_when_invisible
        self.update()

    def show(self):
        """Makes this object visible"""
        
        self.bounds = self.bounds_when_visible
        self.update()



