class PopUpWindow:
    """How the outside code deals with PopUpWindows- MainPopUpWindow deals with actual tkinter code to make this class work though"""

    items = []
    main_popup_window = None
    show_items_function = None

    def __init__(self, items, main_popup_window, show_items_function):
        """Initializes the object"""

        self.items = items
        self.main_popup_window = main_popup_window
        self.show_items_function = show_items_function

    def show(self):
        """Puts the pop up window onto the screen"""

        self.main_popup_window.show_items(self.items, self.show_items_function)


