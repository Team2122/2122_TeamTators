package org.teamtators.bbt8r.commands;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import org.teamtators.bbt8r.TatorRobot;
import org.teamtators.common.scheduler.*;

import java.util.ArrayList;
import java.util.Map;

public class HackerServer extends Command {

    // FORMAT

    // ST I1 I2 I3 (can continue) (separated with sepChar)

    // S: Side
    //     S: Server
    //     C: Client

    // T: Communication Type
    //     C: Command I1: Command Name I2: Argument 1 I3: Argument 2 (can continue) (In n > 1 Optional) -> Runs Command
    //     M: Message I1: Message I2: Query (I2 Optional)
    //     Q: Query I1: Question / Query -> Returns Message
    //     E: Error I1: Error Message
    //     S: Special

    private Scheduler scheduler;
    private CommandStore commandStore;

    private NetworkTableInstance ntInstance;
    private NetworkTable hackerTable;
    private NetworkTableEntry hackerMain;

    private ArrayList<NetworkTableEntry> hackerClients;

    private String sepChar;

    private boolean serverRunning = true;

    private boolean initialized = false;

    public HackerServer(TatorRobot robot) {
        super("HackerServer");
        scheduler = robot.getScheduler();
        commandStore = robot.getCommandStore();
    }

    @Override
    public void initialize() {
        super.initialize();
        if (!initialized) {
            logger.info("Hacker Server Initializing");

            ntInstance = NetworkTableInstance.getDefault();
            hackerTable = ntInstance.getTable(HackerUtil.hackerTableKey);
            hackerMain = hackerTable.getEntry(HackerUtil.hackerMainKey);

            hackerClients = new ArrayList<>();
            hackerMain.setDouble(0);

            sepChar = HackerUtil.sepChar;

            initialized = true;
        }

        serverRunning = true;
    }

    @Override
    public boolean step() {

//        if (Math.random() > .98) {
//            logger.info("Hacker Server Step");
//        }

        if (hackerMain.getDouble(hackerClients.size()) != hackerClients.size()) { // Adding Clients
            for (int i = hackerClients.size(); i < hackerMain.getDouble(-1); i++) {
                logger.info("Adding Client: " + i);
                hackerClients.add(i, hackerTable.getEntry(i + HackerUtil.hackerClientEntryExtension));
                hackerClients.get(i).setString("SS" + HackerUtil.CLIENT_REGISTERED);
            }
        }

        for (int i = 0; i < hackerClients.size(); i++) { // Handling Messages

            String response = hackerClients.get(i).getString("");

            if (response.equals("") || response.charAt(0) != 'C') { // No Response
                continue;
            }

            logger.info("Hacker Server Handling Input");

            hackerClients.get(i).setString("S" + handleInput(response));
        }

        return !serverRunning;
    }

    @Override
    public void finish(boolean interrupted) {
        super.finish(interrupted);

        logger.info("Hacker Server Exiting");

        for (int i = 0; i < hackerClients.size(); i++) {
            hackerClients.get(i).setString("SS" + sepChar + HackerUtil.CLIENT_SHUTDOWN);
        }

    }

    public String handleInput(String input) {

        logger.info("Hacker Server 1");

        ArrayList<String> arguments;
        try {
            arguments = HackerUtil.getArguments(input);
        } catch (Exception e) {
            return "E" + sepChar + "Failed to Parse Message";
        }

        logger.info("Hacker Server 2");

        if (arguments.size() == 0) { // No Input
            return "";
        }

        if (arguments.get(0).length() != 2) { // Not In The Right Format
            String message = "E" + sepChar + "Message Is Unreadable -> Formatting Error";
            message += "\n\t1: " + arguments.get(0);

            return message;
        }

        logger.info("Hacker Server Argument 1: " + arguments.get(1));

        char type = arguments.get(0).charAt(1);

        switch (type) {
            case 'C':
                return handleCommand(arguments);
            case 'M':
                return handleMessage(arguments);
            case 'Q':
                return handleQuery(arguments);
            case 'E':
                return handleError(arguments);
            case 'S':
                    return handleSpecial(arguments);
            default:
                return "E" + sepChar +"Parse Error: Not a Supported Type";
        }

    }

    public String handleCommand(ArrayList<String> arguments) {

        logger.info("Hacker Server Handing Command");

        if (scheduler.getRobotState() != RobotState.TELEOP) {
            return "E" + sepChar + "Not In TeleOp Mode";
        }

        if (arguments.size() < 2) {
            return "E" + sepChar + "No Command Parsed";
        }

        if (commandStore.getCommandNoException(arguments.get(1)) == null) {
            return "E" + sepChar + "Command Not Found";
        }

        if (scheduler.runningCommands.containsKey(arguments.get(1))) {
            scheduler.cancelCommand(arguments.get(1));
            return "M" + sepChar + "Command Cancelled";
        }

        if (arguments.size() == 2) {
            logger.info("Hacker Server Starting Command: " + arguments.get(1));
            scheduler.startCommand(commandStore.getCommand(arguments.get(1)));
            return "M" + sepChar + "Command Running";
        }

        return runCommand(arguments);
    }

    public String runCommand(ArrayList<String> arguments) { // Add Argument Support!

        if (arguments.get(1).equals("TurretToAngle")) {
//            TurretToAngle command;
//            try {
//                command = (TurretToAngle) commandStore.getCommand(arguments.get(1));
//            } catch (Exception e) {
//                return "E" + sepChar + "Type Mismatch, Could Not Cast Correctly";
//            }
//
//            try {
//                command.config.angle = Double.parseDouble(arguments.get(2));
//            } catch (Exception e) {
//                return "E" + sepChar + "Could Not Parse Argument 2, Double";
//            }
//
//            scheduler.startCommand(command);
//            return "M" + sepChar + "Command Configured and Running";
//        } else if (arguments.get(1).equals("ManualShot")) {
//            ManualShot command;
//            try {
//                command = (ManualShot) commandStore.getCommand(arguments.get(1));
//            } catch (Exception e) {
//                return "E" + sepChar + "Type Mismatch, Could Not Cast Correctly";
//            }
//
//            try {
//                command.config.speed = Double.parseDouble(arguments.get(2));
//            } catch (Exception e) {
//                return "E" + sepChar + "Could Not Parse Argument 2, Double";
//            }
//
//            scheduler.startCommand(command);
//            return "M" + sepChar + "Command Configured and Running";
//        } else if (arguments.get(1).equals("HoodExtend")) {
//            HoodExtend command;
//            try {
//                command = (HoodExtend) commandStore.getCommand(arguments.get(1));
//            } catch (Exception e) {
//                return "E" + sepChar + "Type Mismatch, Could Not Cast Correctly";
//            }
//
//            try {
//                command.config.percentExtension = Double.parseDouble(arguments.get(2));
//            } catch (Exception e) {
//                return "E" + sepChar + "Could Not Parse Argument 2, Double";
//            }
//
//            scheduler.startCommand(command);
//            return "M" + sepChar + "Command Configured and Running";
        }

        return "E" + sepChar + "Configuration of That Command Is Not Supported";
    }

    public String handleMessage(ArrayList<String> arguments) {

        if (arguments.size() != 2) {
            return "E" + sepChar + "Could Not Parse Message";
        }

        logger.info("Hacker Server: " + arguments.get(1));

        return "";
    }

    public String handleQuery(ArrayList<String> arguments) {
        return "";
    }

    public String handleError(ArrayList<String> arguments) {
        return "";
    }

    public String handleSpecial(ArrayList<String> arguments) {

        if (arguments.size() < 2) {
            return "E" + sepChar + "No Special Parsed";
        }

        if (arguments.get(1).equals(HackerUtil.SERVER_SHUTDOWN)) {
            serverRunning = false;
            return "M" + sepChar + "Server Shutting Down";
        }

        if (arguments.size() == 3) {
            if (arguments.get(1).equals(HackerUtil.ECHO)) {
                serverRunning = false;
                return "M" + sepChar + arguments.get(2);
            }
        }

        return "E" + sepChar + "Unable To Parse Special";
    }

    @Override
    public boolean isValidInState(RobotState robotState) {
        return RobotState.AUTONOMOUS != robotState;
    }


}
