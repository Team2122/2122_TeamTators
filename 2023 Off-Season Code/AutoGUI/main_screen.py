import os
import tkinter
from copy import deepcopy

from auto_components.grid_items import GridItems
from auto_components.input_field import InputField
from auto_components.titled_input_field import TitledInputField
from auto_features.json_file_loader import json_file_loader
from auto_components.point_alterable_fields_frame import PointAlterableFieldsFrame
from auto_components.way_point import WayPoint

from auto_components.control_point import ControlPoint
from auto_features.json_file_writer import json_file_writer
from auto_features.path_creation import *

from tkinter import filedialog

from tkinter import *
from auto_components.grid import Grid

from miscellaneous.utility_functions import *
from miscellaneous.important_variables import *
from miscellaneous.popup_variables import commands_frame_saver, commands_main_frame


class MainScreen:
    font_size = 22

    # Toolbar
    draw_button = Button(WINDOW, compound=tkinter.CENTER, text="Draw", bg=pleasing_green, fg=white, font=TINY_FONT)
    update_points_button = Button(WINDOW, compound=tkinter.CENTER, text="Update Points", bg=pleasing_green, fg=white, font=TINY_FONT)
    clear_field_button = Button(WINDOW, compound=tkinter.CENTER, text="Clear Field", bg=pleasing_green, fg=white, font=TINY_FONT)
    reset_input_fields_button = Button(WINDOW, compound=tkinter.CENTER, text="Reset Fields", bg=pleasing_green, fg=white, font=TINY_FONT)
    toolbar_height = min(get_measurement(SCREEN_HEIGHT, 15), SCREEN_HEIGHT - FIELD_IMAGE_HEIGHT)
    toolbar_length = get_measurement(SCREEN_LENGTH, 25)
    toolbar_top_edge = SCREEN_HEIGHT - toolbar_height
    popup_windows = []

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
    point_bar_length = SCREEN_LENGTH - FIELD_IMAGE_LENGTH
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
    control_points = []
    way_points = []
    current_points_altered_class = WayPoint
    next_points_altered = {WayPoint: ControlPoint, ControlPoint: WayPoint}
    points_altered_to_point_list = {ControlPoint: control_points, WayPoint: way_points}
    points_altered_to_frame_button_color = {ControlPoint: control_point_color, WayPoint: way_point_color}
    points_altered_to_frame_name = {ControlPoint: "Control Point", WayPoint: "Way Point"}

    # Point Alterable Field Frames:
    way_point_alterable_fields_frame = PointAlterableFieldsFrame(way_points, ["Vx", "Vy", "x power"])
    control_point_alterable_fields_frame = PointAlterableFieldsFrame(control_points, ["Speed", "Between Way Point", "Command"])
    toggle_frame_button = Button(WINDOW, compound=tkinter.CENTER, text="Control Point", bg=control_point_color, fg=white, font=SMALL_FONT)
    control_points_input_fields = []
    way_points_input_fields = []
    points_altered_to_point_alterable_fields_frame = {ControlPoint: control_point_alterable_fields_frame, WayPoint: way_point_alterable_fields_frame}
    points_altered_to_frame_color = {ControlPoint: control_point_color, WayPoint: way_point_color}
    points_altered_to_points_input_fields = {ControlPoint: control_points_input_fields, WayPoint: way_points_input_fields}
    toggle_frame_button_height = get_measurement(SCREEN_HEIGHT, 4)
    point_alterable_fields_frames_height = point_action_bar_top_edge - toggle_frame_button_height # The Point frames should go down to the top of the Point action bar

    # Commands Frame Dimensions
    commands_frame_length = get_measurement(SCREEN_LENGTH, 35)

    # Initial And End Conditions Frame
    initial_conditions_tab_length = SCREEN_LENGTH - toolbar_length - commands_frame_length
    initial_conditions_tab_left_edge = toolbar_length + commands_frame_length
    initial_angle_field = TitledInputField(WINDOW, SMALL_FONT, "45", "Initial Angle", title_field_background_color=blue, title_field_text_color=white)
    initial_speed_field = TitledInputField(WINDOW, SMALL_FONT, "1", "InitialSpeed", title_field_background_color=blue, title_field_text_color=white)
    path_is_closed_field = TitledInputField(WINDOW, SMALL_FONT, "False", "Path Is Closed", title_field_background_color=blue, title_field_text_color=white)
    end_angle_field = TitledInputField(WINDOW, SMALL_FONT, "45", "End Angle", title_field_background_color=blue, title_field_text_color=white)
    placement_angle_field = TitledInputField(WINDOW, SMALL_FONT, "0", "Placement Angle", title_field_background_color=blue, title_field_text_color=white)

    # Field Image
    field_image = tkinter.PhotoImage(file=field_image_path)
    rendered_image = None
    field_image_bounds = [0, 0, SCREEN_LENGTH - point_bar_length, SCREEN_HEIGHT - toolbar_height]
    image_left_edge = FIELD_IMAGE_LENGTH / 2
    image_top_edge = FIELD_IMAGE_HEIGHT / 2

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
    path_line_width = 8
    way_point_line_width = 5

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
        self.reset_input_fields_button.configure(command=self.reset_all_point_input_fields)
        self.swap_points_button.configure(command=self.swap_points_function)

        commands_frame_saver.create_commands_frame(self.toolbar_length, self.toolbar_top_edge, self.commands_frame_length, self.toolbar_height)
        commands_main_frame.default_show_items()

        # The index should go down if the user is moving the point up and up if they are moving a point down
        WINDOW.bind("<Up>", lambda event: self.change_point_order(False))
        WINDOW.bind("<Down>", lambda event: self.change_point_order(True))

        self.set_button_colors()
        self.display_everything()

        points.set_points(self.way_points, self.control_points)

    def toggle_points_alterable_fields_frame(self):
        """Toggles the PointsAlterableFieldsFrame, so it switches to being able to edit ControlPoints and WayPoints"""

        self.unselect_input_fields()

        last_frame = self.points_altered_to_point_alterable_fields_frame.get(self.current_points_altered_class)
        last_frame.hide()

        self.current_points_altered_class = self.next_points_altered.get(self.current_points_altered_class)
        new_frame = self.points_altered_to_point_alterable_fields_frame.get(self.current_points_altered_class)
        new_frame.show()

        frame_name = self.points_altered_to_frame_name.get(self.current_points_altered_class)
        frame_button_color = self.points_altered_to_frame_button_color.get(self.current_points_altered_class)

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

        grid = Grid([0, self.toolbar_top_edge, self.toolbar_length, self.toolbar_height], 1, None)
        grid.turn_into_grid([self.draw_button, self.update_points_button,
                             self.clear_field_button, self.reset_input_fields_button], None, None)

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
        grid.turn_into_grid([self.way_point_alterable_fields_frame], None, None)
        grid.turn_into_grid([self.control_point_alterable_fields_frame], None, None)

        shown_point_alterable_fields_frame = self.points_altered_to_point_alterable_fields_frame.get(self.current_points_altered_class)
        other_points_altered = self.next_points_altered.get(self.current_points_altered_class)
        hidden_point_alterable_fields_frame = self.points_altered_to_point_alterable_fields_frame.get(other_points_altered)

        hidden_point_alterable_fields_frame.hide()
        # IMPORTANT: Must come after, so the title fields show correctly
        shown_point_alterable_fields_frame.show()

        self.toggle_frame_button.place(x=grid.left_edge, y=0, width=self.point_bar_length, height=self.toggle_frame_button_height)

        frame_name = self.points_altered_to_frame_name.get(self.current_points_altered_class)
        frame_button_color = self.points_altered_to_frame_button_color.get(self.current_points_altered_class)

        self.toggle_frame_button.configure(bg=frame_button_color, text=frame_name)

    def save_file_as(self, file_contents=None):
        """Saves a new file with the contents of the GUI"""

        file = filedialog.asksaveasfile(mode='w', defaultextension=".json")

        if file is not None:
            create_file("swerve_input.txt")
            create_file("swerve_output.txt")

            file_path = file.name

            last_slash_index = file_path.rindex("/")
            file_name = file_path[last_slash_index + 1:]
            start_all_json_contents = {
                "Name": file_name,
                "Closed": self.path_is_closed_field.get_text(),
                "InitialAngle": self.initial_angle_field.get_text(),
                "EndAngle": self.end_angle_field.get_text(),
                "InitialSpeed": self.initial_speed_field.get_text(),
                "offsetAngle": self.placement_angle_field.get_text()
            }

            initial_control_point, first_required_point, last_required_point = self.get_control_points_to_reflect_conditions()
            control_points = copy_list(self.control_points)

            control_points.append(initial_control_point)

            json_file_writer.write_postions_to_file()

            placement_angle = float(self.placement_angle_field.get_text())
            json_file_writer.write_file(file, self.way_points,
                                        control_points, start_all_json_contents, first_required_point,
                                        last_required_point, placement_angle)

            first_required_point.destroy()
            last_required_point.destroy()
            initial_control_point.destroy()

        delete_file("swerve_input.txt")
        delete_file("swerve_output.txt")

    def get_control_points_to_reflect_conditions(self):
        """:returns: {initial_control_point, first_required_point, last_required_point}; The updated control points
        that reflect what was entered in the conditions tab + the first and last required point"""

        last_way_point = self.way_points[len(self.way_points) - 1]
        # The first point on the path must have a way point, so the robot has the information to start the path
        initial_required_point = self.get_required_point_at_way_point(self.way_points[0], float(self.initial_angle_field.get_text()))
        last_required_point = self.get_required_point_at_way_point(last_way_point, float(self.end_angle_field.get_text()))

        initial_control_point: ControlPoint = self.get_required_point_at_way_point(self.way_points[0], 0)
        initial_control_point.set_command("None")
        initial_control_point.set_speed(float(self.initial_speed_field.get_text()))

        additional_control_points = [initial_control_point, initial_required_point, last_required_point]

        for control_point in additional_control_points:
            control_point.is_needed = False


        return [initial_control_point, initial_required_point, last_required_point]


    def get_required_point_at_way_point(self, way_point, angle):
        """returns: ControlPoint; a ControlPoint that is at the same position of the 'way_point' provided"""

        # None of these numbers matter because this ControlPointwon't be on the screen
        control_point = ControlPoint(0, 0, None, 0, is_on_screen=False)

        control_point.set_field_left_edge(way_point.get_field_left_edge())
        control_point.set_field_top_edge(way_point.get_field_top_edge())
        control_point.set_command("requiredPoint")
        control_point.set_command_parameter_values([angle])

        return control_point

    def display_everything(self):
        """Allows the user to be able to interact with the GUI after they have a file they can write to"""

        # Creating all the grids on the screen (the 'bars')
        self.create_bottom_bar()
        self.create_point_action_bar()
        self.create_point_alterable_fields_frames()
        self.create_switch_points_bar()
        self.create_initial_conditions_bar()

        # Creating the canvas that holds all the points and the field image
        canvas_length = SCREEN_LENGTH - self.point_bar_length
        canvas_height = SCREEN_HEIGHT - self.toolbar_height
        self.field_canvas = Canvas(master=WINDOW, width=canvas_length,
                                   height=canvas_height, bg=blue)

        self.field_canvas.create_image(self.image_left_edge, self.image_top_edge, image=self.field_image)
        self.field_canvas.place(x=0, y=0)

    def create_initial_conditions_bar(self):
        """Creates the bar for the conditions"""

        grid = Grid([self.initial_conditions_tab_left_edge, self.toolbar_top_edge, self.initial_conditions_tab_length, self.toolbar_height], 1, None)
        grid.turn_into_grid([self.initial_angle_field, self.initial_speed_field, self.end_angle_field, self.path_is_closed_field, self.placement_angle_field], None, None)

    def create_point(self, mouse_left_edge, mouse_top_edge):
        """Puts a new point onto the screen"""

        min_left_edge, min_top_edge, length, height = self.field_image_bounds
        max_left_edge = min_left_edge + length
        max_top_edge = min_top_edge + height

        is_within_horizontal_bounds = mouse_left_edge >= min_left_edge and mouse_left_edge <= max_left_edge
        is_within_vertical_bounds = mouse_top_edge >= min_top_edge and mouse_top_edge <= max_top_edge

        if is_within_horizontal_bounds and is_within_vertical_bounds:
            # Initializing the point
            point = self.current_points_altered_class(0, 0, self.point_click_function, len(self.points_list) + 1)

            point_left_edge = mouse_left_edge - point.base_length / 2
            point_top_edge = mouse_top_edge - point.base_height / 2

            point.place(x=point_left_edge, y=point_top_edge, width=point.base_length, height=point.base_height)

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
        """returns: MovablePoint[]; the points list that the point belongs to (WayPoint, ControlPoint, etc.)"""

        return self.control_points if self.control_points.__contains__(point) else self.way_points

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
            self.control_point_alterable_fields_frame.update()
            self.way_point_alterable_fields_frame.update()
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
            point.default_update_coordinates()

    def reset_point_input_fields(self, points):
        """Changes the input fields, so they reflect the points position on the screen"""

        for point in points:
            point.update_input_fields()

    def reset_all_point_input_fields(self):
        """Changes all the point input fields, so they reflect the points position on the screen"""
        self.reset_point_input_fields(self.control_points)
        self.reset_point_input_fields(self.way_points)

    def clear_field(self):
        """clears the entire field of points and the path"""

        for point in self.control_points + self.way_points:
            point.destroy()

        # So they don't reassigned to a new spot in memory messing up the pointer the frames have to the lists
        self.control_points[:] = []
        self.way_points[:] = []

        self.field_canvas.delete("all")
        self.field_canvas.create_image(self.image_left_edge, self.image_top_edge, image=self.field_image)

        # Updates the frames, so they contain the points data
        self.control_point_alterable_fields_frame.update()
        self.way_point_alterable_fields_frame.update()

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
        """Writes the data to the file, which calls AutoFollower.jar then it draws the points from AutoFollower.jar"""

        create_file("swerve_input.txt")
        create_file("swerve_output.txt")

        # So all the lines are deleted and the image is still on the canvas
        self.field_canvas.delete("all")
        self.field_canvas.create_image(self.image_left_edge, self.image_top_edge, image=self.field_image)

        json_file_writer.write_postions_to_file()
        self.update_point_information()

        draw_path_lines(self.field_canvas, self.way_point_line_width, self.path_line_width)
        self.draw_robot_angle_lines()

        delete_file("swerve_input.txt")
        delete_file("swerve_output.txt")

    def update_point_information(self):
        """Updates all the point information, so drawing the path lines will work correctly"""

        unused, first_required_point, last_required_point = self.get_control_points_to_reflect_conditions()
        required_points = get_required_points(self.control_points)
        required_points = [first_required_point] + required_points + [last_required_point]

        update_way_point_information(required_points)
        first_required_point.destroy()
        last_required_point.destroy()

    def draw_robot_angle_lines(self):
        """Draws the robot angle at each control point"""

        for way_point in self.way_points:
            angle = way_point.get_angle_at_point()
            point2_left_edge = way_point.get_left_edge() + math.cos(angle) * ROBOT_ANGLE_LINE_LENGTH

            y_distance = math.sin(angle) * ROBOT_ANGLE_LINE_LENGTH
            point2_top_edge = way_point.get_top_edge() - y_distance

            self.field_canvas.create_line((way_point.get_left_edge(), way_point.get_top_edge()), [point2_left_edge, point2_top_edge],
                                     fill=ROBOT_ANGLE_LINE_COLOR, width=ROBOT_ANGLE_LINE_WIDTH)

    # Loading in from files
    def load_file(self):
        """Loads a file onto the GUI"""

        file = filedialog.askopenfile(mode='r')

        if file is not None:
            json_contents = json.load(file)
            file.close()

            self.placement_angle_field.set_text(json_contents["offsetAngle"])
            self.initial_speed_field.set_text(json_contents["InitialSpeed"])
            self.end_angle_field.set_text(json_contents["EndAngle"])
            self.initial_angle_field.set_text(json_contents["InitialAngle"])

            self.update_points_to_reflect_loaded_file(json_contents)

    def update_points_to_reflect_loaded_file(self, json_contents):
        """So the GUI reflects what is in the file (has to be delayed because it takes a while for the GUI to update and load the file)"""

        self.clear_field()

        json_file_loader.set_all_points_to_reflect_json_file(self.way_points, self.control_points, json_contents, self.point_click_function)

        self.update_points(self.control_points)
        self.update_points(self.way_points)

        self.toggle_points_alterable_fields_frame()
        self.toggle_points_alterable_fields_frame()

    @property
    def points_list(self):
        """The current points being modified that belong to the PointAlterableFieldsFrame"""
        
        return self.points_altered_to_point_list.get(self.current_points_altered_class)

    @property
    def point_alterable_fields_frame(self):
        """The current PointAlterableFieldsFrame that is on the screen"""
        
        return self.points_altered_to_point_alterable_fields_frame.get(self.current_points_altered_class)

    @property
    def points_input_fields(self):
        """The points input fields that are currently on the screen that belong to the PointAlterableFieldsFrame"""
        
        return self.points_altered_to_points_input_fields.get(self.current_points_altered_class)

    @property
    def current_point_list(self):
        """The points list that is currently in use"""
        
        # First number in Point states signifies if it is the way points list being modified
        return self.points_altered_to_point_list.get(self.current_points_altered_class)



