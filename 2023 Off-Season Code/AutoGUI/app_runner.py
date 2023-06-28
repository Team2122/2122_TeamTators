from miscellaneous.important_variables import WINDOW

from main_screen import MainScreen
from miscellaneous.utility_functions import delete_file

MainScreen()

WINDOW.mainloop()  # running the loop that works as a trigger
delete_file("swerve_input.txt")
delete_file("swerve_output.txt")