from auto_components.grid import Grid


class GridItems:
    """Holds all the items that should be in a grid"""

    items = []

    def __init__(self, items):
        """Initializes the object"""

        self.items = items

    def place(self, **kwargs):
        """Places all the items at that location in a grid format"""

        grid = Grid([kwargs.get("x"), kwargs.get("y"), kwargs.get("width"), kwargs.get("height")], 1, None)
        grid.turn_into_grid(self.items, None, None)


