from auto_features.commands_retriever import commands_retriever


class CommandParameterSaver:
    """Stores the values for the command's parameters"""
    
    # All the all_command_parameter_values; the command_name is the key and the command_parameter_values are the value
    all_command_parameter_values = {}

    # Stores only the singular command parameter value; key is command_name + command_parameter_name and the value is the command_parameter_value
    command_parameter_value = {}

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

        self.all_command_parameter_values[command_name] = command_parameter_values

        command_parameter_names = commands_retriever.get_combined_command_parameter_names(command_name)

        for x in range(len(command_parameter_values)):
            command_parameter_name = command_parameter_names[x]
            command_parameter_value = command_parameter_values[x]

            # TODO use validation arguments
            command_parameter_validation_arguments = commands_retriever.get_command_parameter_validation_arguments(command_name, command_parameter_name)

            dictionary_key = self.get_dictionary_key(command_name, command_parameter_name)
            self.command_parameter_value[dictionary_key] = command_parameter_value

    def get_command_parameter_value_is_valid(self, command_parameter_validation_arguments, command_parameter_value):
        """returns: boolean; whether the command_parameter_value is valid"""

        # TODO follow this model or do a better way
        return_value = True

        type_string_to_python_type_object = {"boolean": bool, "double": float, "int": int}

        for key in command_parameter_validation_arguments.keys():
            expected_value = command_parameter_validation_arguments.get(key)

            if key == "type" and isinstance(command_parameter_value, type_string_to_python_type_object.get(key)):
                return_value = False

            if key == "min" and command_parameter_value > min:
                return_value = False

        return return_value

    def get_command_parameter_values(self, command_name):
        """returns: String[]; the values of the command that is associated to that command_name"""

        return self.all_command_parameter_values.get(command_name)

    def get_command_parameter_value(self, command_name, parameter_name):
        """returns: Object; the value for that parameter"""

        dictionary_key = self.get_dictionary_key(command_name, parameter_name)
        return self.command_parameter_value.get(dictionary_key)
