import json


class CommandsRetriever:
    """ Loads all the commands, from commands.txt What is stored in the commands.txt file is as follows:
        command 'command_name' -> stored in the class's 'command_names' attribute
        parameter 'defaultValue' -> stored in the class's 'command_parameter_default_values' attribute
        parameter 'name' -> stored in the class's 'command_parameter_base_names' attribute; the parameters for the commands that is solely in commands.txt
        parameter 'validationArguments' -> stored in the class's 'command_parameter_validation_arguments' attribute; the restraints for each command's parameters (min value, max value, type, etc.)
        """

    command_names = []
    # Does not include the combined_command's arguments
    combined_command_parameter_names = {}
    command_parameter_base_names = {}  # The parameters for the commands that have not been combined (what is solely in commands.txt)
    command_parameter_default_values = {}
    command_parameter_validation_arguments = {}

    # NOTE: This dictionary is for implementation as far as the GUI is concerned. The Autonomous generation code (the other side that
    # is not the GUI) will have all the commands in commands.txt However, to make the GUI more usable some commands will be
    # automatically applied to make the GUI easier to use. The key in the dictionary is the command that is a combination of
    # commands and the value is the commands it is a combination of. Also, the order is the order the commands will be applied
    combined_commands = {
        "autoShoot": ["autoShoot", "lambdaLock", "unShoot"],
        "pickRight": ["pickRight", "requiredPoint"],
        "pickLeft": ["pickLeft", "requiredPoint"],
    }

    commands_json = json.load(open("commands.txt", "r"))

    def __init__(self):
        """Loads all the commands, so other parts of the code can access what is in commands.txt"""

        # Have to convert it to a list because '.keys(),' does not return a list
        self.command_names = list(self.commands_json.keys())

        # So if it is initialized multiple times the dictionary's don't grow too large from previous initializations
        self.combined_command_parameter_names = {}
        self.command_parameter_base_names = {}
        self.command_parameter_default_values = {}
        self.command_parameter_validation_arguments = {}

        for command_name in self.command_names:
            self.set_command_parameter_values(self.combined_command_parameter_names, command_name, "name", True)
            self.set_command_parameter_values(self.command_parameter_base_names, command_name, "name", False)
            self.set_command_parameter_values(self.command_parameter_default_values, command_name, "defaultValue", True)
            self.set_command_parameter_values(self.command_parameter_validation_arguments, command_name, "validationArguments", True)

    def set_command_parameter_values(self, dictionary, base_command_name, parameter_key, is_using_command_combination):
        """ summary: Sets the key and values for the dictionary provided; this is specifically for the parameters of commands-
            the things that are specific to each parameter like validationArguments, defaultValue, etc. This includes the
            command to all values and a command and parameter to the specific value

            params:
                dictionary: dict; the dictionary that stores the values that are specific to every parameter (name, defaultValue, etc)
                base_command_name: String; the name of the command
                parameter_key: String; the key in the key-value pairs in commands.txt that is wanted (name, defaultValue, etc.)
                is_using_command_combination: boolean; whether the parameters are solely the ones in commands.txt or includes the parameters once the commands are combined

            returns: None"""

        # Some commands are a combination of multiple commands and if that is the case the commands need to be gotten
        # From 'self.combined_commands' otherwise the base command name needs to be used
        all_command_names = [base_command_name]
        if self.combined_commands.__contains__(base_command_name) and is_using_command_combination:
            all_command_names = self.combined_commands.get(base_command_name)

        all_command_parameters = []

        for command_name in all_command_names:
            all_command_parameters += self.commands_json.get(command_name)

        all_command_parameter_values = []
        for command_parameter in all_command_parameters:
            command_parameter_value = command_parameter.get(parameter_key)
            command_parameter_name = command_parameter.get("name")
            all_command_parameter_values.append(command_parameter_value)
            dictionary_key = self.get_dictionary_key(base_command_name, command_parameter_name)
            dictionary[dictionary_key] = command_parameter_value

        dictionary[base_command_name] = all_command_parameter_values

    def get_dictionary_key(self, command_name, command_parameter_name):
        """returns: String; the key that the dictionary should have for that 'command_name' and 'command_parameter'"""

        return f"{command_name}-{command_parameter_name}"

    # GET METHODS
    def get_command_names(self):
        return self.command_names

    def get_command_parameter_validation_arguments(self, command_name, command_parameter_name):
        return self.get_command_parameter_values(self.command_parameter_validation_arguments, command_name, command_parameter_name)

    def get_combined_command_parameter_names(self, command_name):
        return self.combined_command_parameter_names.get(command_name)

    def get_command_parameter_base_names(self, command_name):
        return self.command_parameter_base_names.get(command_name)

    def get_combined_command_parameter_names(self, command_name):
        return self.combined_command_parameter_names.get(command_name)

    def get_command_parameter_default_value(self, command_name, command_parameter_name):
        return self.get_command_parameter_values(self.command_parameter_default_values, command_name, command_parameter_name)

    def get_command_parameter_default_values(self, command_name):
        return self.command_parameter_default_values.get(command_name)

    def get_command_parameter_values(self, dictionary, command_name, command_parameter_name):
        """returns: Object[] or Object; the value(s) for that 'command_name' and 'command_parameter_name' for that dictionary"""

        dictionary_key = self.get_dictionary_key(command_name, command_parameter_name)
        return dictionary.get(dictionary_key)


commands_retriever = CommandsRetriever()
