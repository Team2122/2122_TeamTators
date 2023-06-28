class PopUpWindow:
    """How the outside code deals with PopUpWindows- MainPopUpWindow deals with actual tkinter code to make this class work though"""

    items = []
    commands_main_frame = None
    show_items_function = None

    def __init__(self, items, commands_main_frame, show_items_function):
        """Initializes the object"""

        self.items = items
        self.commands_main_frame = commands_main_frame
        self.show_items_function = show_items_function

    def show(self):
        """Puts the pop up window onto the screen"""

        self.commands_main_frame.show_items(self.items, self.show_items_function)


