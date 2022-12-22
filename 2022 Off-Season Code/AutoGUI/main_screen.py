import os
import tkinter

from auto_components.input_field import InputField
from auto_features.json_file_loader import json_file_loader
from auto_components.point_alterable_fields_frame import PointAlterableFieldsFrame
from auto_components.control_point import ControlPoint

from auto_components.way_point import WayPoint
from auto_features.json_file_writer import json_file_writer
from auto_features.path_creation import *

from tkinter import filedialog

from tkinter import *
from auto_components.grid import Grid

from miscellaneous.utility_functions import *


class MainScreen:
    font_size = 22

    # Toolbar
    draw_button = Button(WINDOW, compound=tkinter.CENTER, text="Draw", bg=pleasing_green, fg=white, font=NORMAL_FONT)
    update_points_button = Button(WINDOW, compound=tkinter.CENTER, text="Update Points", bg=pleasing_green, fg=white, font=NORMAL_FONT)
    clear_field_button = Button(WINDOW, compound=tkinter.CENTER, text="Clear Field", bg=pleasing_green, fg=white, font=NORMAL_FONT)
    toolbar_height = get_measurement(SCREEN_HEIGHT, 5)

    # Switching Points Bar
    switching_points_bar_height = get_measurement(SCREEN_HEIGHT, 5)
    selected_point_field = InputField(WINDOW, SMALL_FONT, "1", True)
    switched_point_field = InputField(WINDOW, SMALL_FONT, "2", True)
    swap_points_button = Button(WINDOW, compound=tkinter.CENTER, text="Swap", bg=pleasing_green, fg=white, font=SMALL_FONT)
    switching_points_bar_top_edge = SCREEN_HEIGHT - toolbar_height - switching_points_bar_height

    # Point Action Bar
    delete_button = Button(WINDOW, compound=tkinter.CENTER, text="Delete", bg=pleasing_green, fg=white, font=SMALL_FONT)
    move_button = Button(WINDOW, compound=tkinter.CENTER, text="Move", bg=pleasing_green, fg=white, font=SMALL_FONT)
    add_button = Button(WINDOW, compound=tkinter.CENTER, text="Add", bg=pleasing_green, fg=white, font=SMALL_FONT)
    point_bar_length = get_measurement(SCREEN_LENGTH, 20)
    point_action_bar_height = get_measurement(SCREEN_HEIGHT, 5)
    point_action_bar_top_edge = SCREEN_HEIGHT - toolbar_height - switching_points_bar_height - point_action_bar_height
    point_action_bar_buttons = [delete_button, move_button, add_button]

    # Miscellaneous
    file_name = ""
    selected_point = None
    selected_input_field = None

    # File Menu
    menu = Menu(WINDOW)
    file_menu = Menu(menu)
    menu.add_cascade(label='File', menu=file_menu)

    # Point Info:
    way_points = []
    control_points = []
    current_points_altered = ControlPoint
    next_points_altered = {ControlPoint: WayPoint, WayPoint: ControlPoint}
    points_altered_to_point_list = {WayPoint: way_points, ControlPoint: control_points}
    points_altered_to_frame_button_color = {WayPoint: way_point_color, ControlPoint: control_point_color}
    points_altered_to_frame_name = {WayPoint: "Way Point", ControlPoint: "Control Point"}

    # Point Alterable Field Frames:
    control_point_alterable_fields_frame = PointAlterableFieldsFrame(control_points, ["Vx", "Vy"])
    way_point_alterable_fields_frame = PointAlterableFieldsFrame(way_points, ["Speed", "Command", "x power"])
    toggle_frame_button = Button(WINDOW, compound=tkinter.CENTER, text="Way Point", bg=way_point_color, fg=white, font=SMALL_FONT)
    way_points_input_fields = []
    control_points_input_fields = []
    points_altered_to_point_alterable_fields_frame = {WayPoint: way_point_alterable_fields_frame, ControlPoint: control_point_alterable_fields_frame}
    points_altered_to_frame_color = {WayPoint: way_point_color, ControlPoint: control_point_color}
    points_altered_to_points_input_fields = {WayPoint: way_points_input_fields, ControlPoint: control_points_input_fields}
    toggle_frame_button_height = get_measurement(SCREEN_HEIGHT, 4)
    point_alterable_fields_frames_height = point_action_bar_top_edge - toggle_frame_button_height # The Point frames should go down to the top of the Point action bar

    # Field Image
    field_image = tkinter.PhotoImage(file="images/field_image.png")
    rendered_image = None
    field_image_bounds = [0, 0, SCREEN_LENGTH - point_bar_length, SCREEN_HEIGHT - toolbar_height]
    image_left_edge = 500
    image_top_edge = 280

    # States
    class States:
        DELETION = "DELETION"
        MOVING = "MOVING"
        ADD = "ADD"
        INIT = "INIT"

    point_editing_state = States.ADD
    point_editing_state_to_point_button = {States.DELETION: delete_button, States.MOVING: move_button, States.ADD: add_button}

    field_canvas = None

    # Path Drawing
    path_line_width = 12
    control_point_line_width = 5

    def __init__(self):
        """Used for setting up the entire GUI"""

        self.create_file_menu()

        WINDOW.bind("<Button-1>", lambda e: self.run_mouse_click(e))

        self.delete_button.configure(command=lambda: self.change_point_editing_state(self.States.DELETION, self.States.ADD))
        self.move_button.configure(command=lambda: self.change_point_editing_state(self.States.MOVING, self.States.INIT))
        self.add_button.configure(command=lambda: self.change_point_editing_state(self.States.ADD, self.States.DELETION))
        self.toggle_frame_button.configure(command=self.toggle_points_alterable_fields_frame)
        self.update_points_button.configure(command=self.update_points)
        self.draw_button.configure(command=self.draw_path)
        self.clear_field_button.configure(command=self.clear_field)
        self.swap_points_button.configure(command=self.swap_points_function)

        # The index should go down if the user is moving the point up and up if they are moving a point down
        WINDOW.bind("<Up>", lambda event: self.change_point_order(False))
        WINDOW.bind("<Down>", lambda event: self.change_point_order(True))

        self.set_button_colors()
        self.display_everything()

    def toggle_points_alterable_fields_frame(self):
        """Toggles the PointsAlterableFieldsFrame, so it switches to being able to edit WayPoints and ControlPoints"""

        self.unselect_input_fields()

        last_frame = self.points_altered_to_point_alterable_fields_frame.get(self.current_points_altered)
        last_frame.hide()

        self.current_points_altered = self.next_points_altered.get(self.current_points_altered)
        new_frame = self.points_altered_to_point_alterable_fields_frame.get(self.current_points_altered)
        new_frame.show()

        frame_name = self.points_altered_to_frame_name.get(self.current_points_altered)
        frame_button_color = self.points_altered_to_frame_button_color.get(self.current_points_altered)

        self.toggle_frame_button.configure(bg=frame_button_color, text=frame_name)

    def change_point_editing_state(self, point_editing_state, state_after_double_click):
        """Changes the point editing state, so it can switch between adding, moving, deleting, and doing nothing with points"""

        # If the button is a toggle then it should toggle between INIT (doing nothing) and that point_editing_state
        if point_editing_state == self.point_editing_state:
            self.point_editing_state = state_after_double_click

        else:
            self.point_editing_state = point_editing_state

        self.set_button_colors()

    def create_file_menu(self):
        """Creates the file menu system that allows the user to navigate between loading and saving files"""

        self.file_menu.add_command(label="Load File", command=self.load_file)
        self.file_menu.add_command(label="Save File As", command=self.save_file_as)
        WINDOW.configure(menu=self.menu)

    def create_bottom_bar(self):
        """Creates the button bar at the bottom of the screen (updating points, draw button, etc.)"""

        grid = Grid([0, SCREEN_HEIGHT - self.toolbar_height, SCREEN_LENGTH, self.toolbar_height], 1, None)
        grid.turn_into_grid([self.draw_button, self.update_points_button,
                             self.clear_field_button], None, None)

    def create_switch_points_bar(self):
        """Creates the bar that allows you to switch points around"""

        grid = Grid([SCREEN_LENGTH - self.point_bar_length, self.switching_points_bar_top_edge, self.point_bar_length, self.switching_points_bar_height], 1, None)
        grid.turn_into_grid([self.selected_point_field, self.switched_point_field, self.swap_points_button], None, None)

    def create_point_action_bar(self):
        """Creates the bar that allows you to be able to add, delete, and move points"""

        grid = Grid([SCREEN_LENGTH - self.point_bar_length, self.point_action_bar_top_edge, self.point_bar_length, self.point_action_bar_height], 1, None)
        grid.turn_into_grid([self.add_button, self.delete_button, self.move_button], None, None)

    def create_point_alterable_fields_frames(self):
        """Creates the bar that allows you to be able to modify the field's attributes like X, Y, Command, etc."""

        grid = Grid([SCREEN_LENGTH - self.point_bar_length, self.toggle_frame_button_height, self.point_bar_length, self.point_alterable_fields_frames_height], None, 1)

        # So they have the same dimensions
        grid.turn_into_grid([self.control_point_alterable_fields_frame], None, None)
        grid.turn_into_grid([self.way_point_alterable_fields_frame], None, None)

        shown_point_alterable_fields_frame = self.points_altered_to_point_alterable_fields_frame.get(self.current_points_altered)
        other_points_altered = self.next_points_altered.get(self.current_points_altered)
        hidden_point_alterable_fields_frame = self.points_altered_to_point_alterable_fields_frame.get(other_points_altered)

        hidden_point_alterable_fields_frame.hide()
        # IMPORTANT: Must come after, so the title fields show correctly
        shown_point_alterable_fields_frame.show()

        self.toggle_frame_button.place(x=grid.left_edge, y=0, width=self.point_bar_length, height=self.toggle_frame_button_height)

        frame_name = self.points_altered_to_frame_name.get(self.current_points_altered)
        frame_button_color = self.points_altered_to_frame_button_color.get(self.current_points_altered)

        self.toggle_frame_button.configure(bg=frame_button_color, text=frame_name)

    def save_file_as(self, file_contents=None):
        """Saves a new file with the contents of the GUI"""

        file = filedialog.asksaveasfile(mode='w', defaultextension=".txt")

        if file is not None:
            file_path = file.name

            # So the input fields are guaranteed to reflect what is on the field
            self.reset_point_input_fields(self.way_points)
            self.reset_point_input_fields(self.control_points)

            json_file_writer.write_file(file_path, file, self.control_points, self.way_points)

    def display_everything(self):
        """Allows the user to be able to interact with the GUI after they have a file they can write to"""

        # Creating all the grids on the screen (the 'bars')
        self.create_bottom_bar()
        self.create_point_action_bar()
        self.create_point_alterable_fields_frames()
        self.create_switch_points_bar()

        # Creating the canvas that holds all the points and the field image
        canvas_length = SCREEN_LENGTH - self.point_bar_length
        canvas_height = SCREEN_HEIGHT - self.toolbar_height
        self.field_canvas = Canvas(master=WINDOW, width=canvas_length,
                                   height=canvas_height, bg=blue)

        self.field_canvas.create_image(self.image_left_edge, self.image_top_edge, image=self.field_image)
        self.field_canvas.place(x=0, y=0)

    def create_point(self, mouse_left_edge, mouse_top_edge):
        """Puts a new point onto the screen"""

        min_left_edge, min_top_edge, length, height = self.field_image_bounds
        max_left_edge = min_left_edge + length
        max_top_edge = min_top_edge + height

        is_within_horizontal_bounds = mouse_left_edge >= min_left_edge and mouse_left_edge <= max_left_edge
        is_within_vertical_bounds = mouse_top_edge >= min_top_edge and mouse_top_edge <= max_top_edge

        if is_within_horizontal_bounds and is_within_vertical_bounds:
            point = self.current_points_altered(mouse_left_edge, mouse_top_edge, self.point_click_function, len(self.points_list) + 1)
            point.set_input_fields_command(self.handle_input_field_click)

            self.points_list.append(point)
            self.point_alterable_fields_frame.update()
            point.set_order_position(len(self.points_list))
            self.update_input_fields()

    def unselect_input_fields(self):
        """Makes all the points become unselected"""

        for input_field in self.points_input_fields:
            input_field.set_is_selected(False)

        for point in self.points_list:
            point.unselect()

        self.selected_input_field = None

    def update_input_fields(self):
        """So when a point is either added or deleted all the fields are recalculated to reflect the points"""

        # So there are no more input fields; then all the input fields can be populated
        # Creating a new variable, so the names don't conflict with the function name
        points_input_fields = self.points_input_fields
        points_input_fields[:] = []

        for point in self.points_list:
            points_input_fields += point.get_input_fields()

    def handle_input_field_click(self, selected_input_field):
        """Makes the input field become selected and the point that input field belongs to selected (all others are unselected)"""

        self.unselect_input_fields()

        # Once all the input field's are unselected then make the 'selected_input_field' selected
        selected_input_field.set_is_selected(True)
        selected_input_field.get_belongs_to().select()
        self.selected_input_field = selected_input_field

    # Click Functions
    def swap_points_function(self):
        """Swaps the points"""

        # Indexes and the point numbers are of a difference of 1
        point_index = int(self.selected_point_field.get_text()) - 1
        new_index = int(self.switched_point_field.get_text()) - 1

        # Swaps the 'backend' position of the points
        swap_list_items(self.points_list, point_index, new_index)
        self.point_alterable_fields_frame.update()

    def get_points_list(self, point):
        """returns: MovablePoint[]; the points list that the point belongs to (ControlPoint, WayPoint, etc.)"""

        return self.way_points if self.way_points.__contains__(point) else self.control_points

    def get_index_of_point(self, point, points_list):
        """returns: int; the index of the point within the points list gotten from get_points_list()"""

        return points_list.index(point)

    def point_click_function(self, point):
        """ Runs different things depending on what point_editing_state the GUI is in when the point was clicked:
            ADD: Adds a point
            MOVING: Selects a point
            DELETION: Deletes a point
        """

        points_list = self.get_points_list(point)
        index_of_point = self.get_index_of_point(point, points_list)

        if self.point_editing_state == self.States.DELETION:
            del points_list[index_of_point]
            self.way_point_alterable_fields_frame.update()
            self.control_point_alterable_fields_frame.update()
            point.destroy()

            self.update_input_fields()

        if self.point_editing_state == self.States.MOVING:
            self.selected_point = point
            self.selected_point.select()

    def set_button_colors(self):
        """Sets the colors of the add, move, delete buttons; called upon point_editing_state change"""

        for button in self.point_action_bar_buttons:
            button.configure(bg=pleasing_green)

        point_button = self.point_editing_state_to_point_button.get(self.point_editing_state)

        # If the point_editing_state is in INIT then there will be no point button causing an error
        if point_button is not None:
            point_button.configure(bg=dark_green)

    def run_mouse_click(self, event):
        """Creates a point if the point_editing_state is ADD and moves a point if the point_editing_state is MOVE and a point is selected"""

        mouse_left_edge, mouse_top_edge = get_mouse_position()

        if self.point_editing_state == self.States.ADD:
            self.create_point(mouse_left_edge, mouse_top_edge)

        if self.point_editing_state == self.States.MOVING and self.selected_point is not None:
            self.selected_point.place(x=mouse_left_edge, y=mouse_top_edge)
            self.selected_point.unselect()
            self.selected_point = None

    def update_points(self, points_list=None):
        """Updates the points, so they reflect what the input field's have"""

        points_list = points_list or self.current_point_list

        for point in points_list:
            top_edge = -1 * float(point.get_field_top_edge())  # By default it inverses, so it has to be uninversed
            point.set_field_top_edge(top_edge)

            point.default_update_coordinates()

    def reset_point_input_fields(self, points):
        """Updates the input fields, so they reflect the points position on the screen"""

        for point in points:
            point.update_input_fields()

    def clear_field(self):
        """clears the entire field of points and the path"""

        for point in self.way_points + self.control_points:
            point.destroy()

        # So they don't reassigned to a new spot in memory messing up the pointer the frames have to the lists
        self.way_points[:] = []
        self.control_points[:] = []

        self.field_canvas.delete("all")
        self.field_canvas.create_image(self.image_left_edge, self.image_top_edge, image=self.field_image)

        # Updates the frames, so they contain the points data
        self.way_point_alterable_fields_frame.update()
        self.control_point_alterable_fields_frame.update()

    def change_point_order(self, is_up):
        """Moves the order of the currently selected point (1 -> 2)"""

        if self.selected_input_field is not None:
            point = self.selected_input_field.get_belongs_to()
            point_index, new_index = self.get_point_indexes(point, is_up)

            # Swaps the 'backend' position of the points
            swap_list_items(self.points_list, point_index, new_index)
            self.point_alterable_fields_frame.update()

    def get_point_indexes(self, selected_point, is_up):
        """returns the new index of the point"""

        point_index = selected_point.get_order_position() - 1

        next_index = get_next_index(len(self.points_list) - 1, point_index)
        previous_index = get_previous_index(point_index)
        new_index = next_index if is_up else previous_index

        return [point_index, new_index]

    def draw_path(self):
        """Writes the data to the file, which calls SwerveLib.jar then it draws the points from SwerveLib.jar"""

        # So all the lines are deleted and the image is still on the canvas
        self.field_canvas.delete("all")
        self.field_canvas.create_image(self.image_left_edge, self.image_top_edge, image=self.field_image)

        self.reset_point_input_fields(self.way_points)
        self.reset_point_input_fields(self.control_points)
        write_postions_to_file(self.control_points)

        os.system("java -jar SwerveLib.jar swerve_input.txt swerve_output.txt")
        draw_path_lines(self.way_points, self.field_canvas, self.control_point_line_width, self.path_line_width)

    # Loading in from files
    def load_file(self):
        """Loads a file onto the GUI"""

        file = filedialog.askopenfile(mode='r')

        if file is not None:
            self.update_points_to_reflect_loaded_file(file)

    def update_points_to_reflect_loaded_file(self, json_file):
        """So the GUI reflects what is in the file (has to be delayed because it takes a while for the GUI to update and load the file)"""

        self.clear_field()

        json_file_loader.set_all_points_to_reflect_json_file(self.control_points, self.way_points, json_file, self.point_click_function)

        self.update_points(self.way_points)
        self.update_points(self.control_points)

        self.toggle_points_alterable_fields_frame()
        self.toggle_points_alterable_fields_frame()

    @property
    def points_list(self):
        """The current points being modified that belong to the PointAlterableFieldsFrame"""
        
        return self.points_altered_to_point_list.get(self.current_points_altered)


    @property
    def point_alterable_fields_frame(self):
        """The current PointAlterableFieldsFrame that is on the screen"""
        
        return self.points_altered_to_point_alterable_fields_frame.get(self.current_points_altered)



    @property
    def points_input_fields(self):
        """The points input fields that are currently on the screen that belong to the PointAlterableFieldsFrame"""
        
        return self.points_altered_to_points_input_fields.get(self.current_points_altered)

    @property
    def current_point_list(self):
        """The points list that is currently in use"""
        
        # First number in Point states signifies if it is the way points list being modified
        return self.points_altered_to_point_list.get(self.current_points_altered)



