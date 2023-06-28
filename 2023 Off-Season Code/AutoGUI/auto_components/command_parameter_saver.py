from auto_features.commands_retriever import commands_retriever
from tkinter import messagebox
from miscellaneous.popup_variables import commands_main_frame
import re


class CommandParameterSaver:
    """Stores the values for the command's parameters"""
    
    # All the all_command_parameter_values; the command_name is the key and the command_parameter_values are the value
    all_command_parameter_values = {}

    # Stores only the singular command parameter value; key is command_name + command_parameter_name and the value is the command_parameter_value
    command_parameter_value = {}

    type_string_to_python_type_object = {"boolean": bool, "double": float, "int": int}

    def __init__(self):
        """Stores all the default values for the commands"""
        
        self.command_parameter_value = {}
        self.all_command_parameter_values = {}
        
        for command_name in commands_retriever.get_command_names():
            command_parameter_default_values = commands_retriever.get_command_parameter_default_values(command_name)
            self.all_command_parameter_values[command_name] = command_parameter_default_values

            # So it populates the dictionary 'command_parameter_value'
            self.set_command_parameter_values(command_name, command_parameter_default_values)

    def get_dictionary_key(self, command_name, command_parameter_name):
        """returns: String; the key that the dictionary should have for that 'command_name' and 'command_parameter'"""

        return f"{command_name}-{command_parameter_name}"

    def set_command_parameter_values(self, command_name, command_parameter_values):
        """Sets the values for the command's parameters"""

        command_parameter_names = commands_retriever.get_combined_command_parameter_names(command_name)

        # The value in the expected type (angles should be a float, etc.)
        savable_command_parameter_values = []

        values_are_valid = True
        values_error_message = ""

        for x in range(len(command_parameter_values)):
            command_parameter_name = command_parameter_names[x]
            command_parameter_value = str(command_parameter_values[x])  # Strings are needed for some validation arguments

            command_parameter_validation_arguments = commands_retriever.get_command_parameter_validation_arguments(command_name, command_parameter_name)
            is_valid, error_message = self.get_command_parameter_value_is_valid(command_parameter_validation_arguments, command_parameter_value)

            # Only the error of the first invalid parameter should be displayed
            if not is_valid and values_are_valid:
                values_error_message = error_message
                values_are_valid = False

            if is_valid:
                parameter_java_type = command_parameter_validation_arguments.get("type")
                python_type = self.type_string_to_python_type_object.get(parameter_java_type)

                # Converts the parameter value (string) to a non string value
                savable_command_parameter_values.append(python_type(command_parameter_value))

        if values_are_valid:
            self.save_all_parameter_values(command_name, command_parameter_values)

        else:
            messagebox.showerror("ERROR", values_error_message)

    def save_all_parameter_values(self, command_name, command_parameter_values):
        """Saves all the parameter values"""

        self.all_command_parameter_values[command_name] = command_parameter_values
        command_parameter_names = commands_retriever.get_combined_command_parameter_names(command_name)

        for x in range(len(command_parameter_values)):
            command_parameter_name = command_parameter_names[x]
            command_parameter_value = command_parameter_values[x]

            dictionary_key = self.get_dictionary_key(command_name, command_parameter_name)
            self.command_parameter_value[dictionary_key] = command_parameter_value

    def get_command_parameter_value_is_valid(self, command_parameter_validation_arguments, command_parameter_value):
        """returns: [boolean, String] -> [is_valid, error_message]; whether the command_parameter_value is valid"""

        return_value = [True, ""]

        expected_type = command_parameter_validation_arguments.get("type")
        python_type = self.type_string_to_python_type_object.get(expected_type)

        if not self.is_correct_type(python_type, command_parameter_value):
            return_value = [False, f"Wanted type {expected_type}, but did not get that type"]

        for key in command_parameter_validation_arguments.keys():

            # The other validation arguments should not be checked if the type is wrong
            if not return_value[0]:
                break

            expected_value = command_parameter_validation_arguments.get(key)

            if key == "min" and float(command_parameter_value) < float(expected_value):
                return_value = [False, f"Value {command_parameter_value} is smaller than the minimum of {expected_value}"]

            if key == "max" and float(command_parameter_value) > float(expected_value):
                return_value = [False, f"Value {command_parameter_value} is bigger than the maximum of {expected_value}"]

            if key == "acceptableValues" and not expected_value.__contains__(command_parameter_value):
                return_value = [False, f"Value {command_parameter_value} is not in the list of valid values: {expected_value}"]

        return return_value

    def is_correct_type(self, expected_type, value):
        """returns: boolean; if the value is the expected type"""

        return_value = True

        if expected_type == int:
            # The string figures out if it only has numbers
            return_value = re.search("^[0-9]*$", value) is not None and len(value) != 0

        if expected_type == float:
            # The string is a regular expression which figure out if the value has a series of only numbers followed
            # By a decimal (not required) and then another series of only numbers (also the value can't be an empty string)
            return_value = re.search("^[0-9]*\.*[0-9]*$", value) is not None and len(value) != 0

        if expected_type == bool:
            acceptable_values = ["true", "false"]
            return_value = acceptable_values.__contains__(value)

        return return_value

    def get_command_parameter_values(self, command_name):
        """returns: String[]; the values of the command that is associated to that command_name"""

        # So changes to list once it is received, don't change what this class stores
        return list(self.all_command_parameter_values.get(command_name))

    def get_command_parameter_value(self, command_name, parameter_name):
        """returns: Object; the value for that parameter"""

        # So changes to list once it is received, don't change what this class stores
        dictionary_key = self.get_dictionary_key(command_name, parameter_name)
        return self.command_parameter_value.get(dictionary_key)
