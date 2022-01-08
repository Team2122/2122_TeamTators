package org.teamtators.common;

import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import org.teamtators.common.scheduler.HackerUtil;

import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;

public class HackerClient implements Runnable {

    private final String server = "10.21.22.2";

    private Scanner scanner = new Scanner(System.in);
    private AtomicReference<String> userInput;

    private Thread inputHandler;

    private NetworkTableInstance inst;
    private NetworkTableEntry hackerInput;

    private int id;

    boolean running = true;

    String sepChar;

    public static void main(String[] args) {
        HackerClient hacker = new HackerClient();
    }

    public HackerClient() {
        System.out.println("Starting HackerClient");

        connectToNetworkTables();
        connectToHackerServer();

        configure();

        start();
    }

    public void connectToNetworkTables() {

        System.out.println("Connecting To NetworkTables Server: " + server);

        inst = NetworkTableInstance.getDefault();
        inst.setServer(server);
        inst.startClientTeam(2122);

        long startTime = System.currentTimeMillis();
        long timeout = 10 * 1000;

        while (true) {
            if (inst.isConnected()) {
                System.out.println("Successfully Connected To NetworkTables Server");
                break;
            } else if (System.currentTimeMillis() - startTime > timeout) {
                System.out.println("Connecting To NetworkTables Server Timed Out");
                System.exit(1);
            }
        }
    }

    public void connectToHackerServer() {

        System.out.println("Connecting To Hacker Server");

        hackerInput = inst.getTable(HackerUtil.hackerTableKey).getEntry(HackerUtil.hackerMainKey);
        id = (int) hackerInput.getDouble(-1);

        if (id == -1) {
            System.out.println("Error: Unable To Connect To Hacker Server -> Is The Server Initialized?");
            System.exit(1);
        }

        int numClients = id + 1;
        hackerInput.setDouble(numClients);

        hackerInput = inst.getTable(HackerUtil.hackerTableKey).getEntry(id + HackerUtil.hackerClientEntryExtension);

        long startTime = System.currentTimeMillis();
        double timeout = 5 * 1000;

        while (true) {
            if (hackerInput.getString("").equals("SS" + HackerUtil.CLIENT_REGISTERED)) {
                System.out.println("Successfully Registered To Hacker Server");
                hackerInput.setString("");
                break;
            } else if (System.currentTimeMillis() - startTime > timeout) {
                System.out.println("Registering With Hacker Server Timed Out");
                System.exit(1);
            }
        }

    }

    public void configure() {
        sepChar = HackerUtil.sepChar;

        userInput = new AtomicReference<>();
        userInput.set("");

        inputHandler = new Thread(this);
        inputHandler.start();
    }

    @Override
    public void run() {
        System.out.println("Launching Input Thread");

        String input;
        while (running) {
//            System.out.println("Ready For Input");

            if (scanner.hasNextLine()) {
                input = scanner.nextLine();
                userInput.set(input);
            }

        }
    }

    public void start() {

        String serverString;

        while (running) {
//            System.out.println("Running");
//            handleUserInput(userInput.get());

            serverString = hackerInput.getString("");

            if (serverString.equals("")) {
                handleUserInput(userInput.get());
            } else {
                if (serverString.charAt(0) == 'S') {
                    handleServerInput(serverString);
                }
            }

        }

    }

    public void handleUserInput(String input) {

        if (input.equals("")) {
            return;
        }

        hackerInput.setString(input);

        System.out.println("Handing User Input");

//        ArrayList<String> arguments = HackerUtil.getArguments(input);
//        System.out.println("Number of Arguments: " + arguments.size());
//        for (int i = 0; i < arguments.size(); i++) {
//            System.out.println(arguments.get(i));
//        }

        userInput.set("");
        System.out.println("Sending Input");
    }

    public void handleServerInput(String input) {
        System.out.println("Server: " + input);
        hackerInput.setString("");
    }

}


