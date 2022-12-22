package org.teamtators.sassitator;

import edu.wpi.first.networktables.EntryListenerFlags;
import edu.wpi.first.networktables.NetworkTableEntry;

import java.util.Scanner;

public class HackerClient extends NetworkTablesClient {

    private final String tableKey = "HackerServer";
    private final String numClientsKey = "numClients";
    private final String entryKey = "Client: ";

    public static void main(String... args) {
        new HackerClient();
    }

    public HackerClient() {
        long time = System.currentTimeMillis();

        // Connect to HackerServer
        NetworkTableEntry numClientsEntry = inst.getTable(tableKey).getEntry(numClientsKey);
        System.out.println("Connecting to HackerServer...");
        int currentNum;
        do {
            currentNum = Integer.parseInt(numClientsEntry.getString("-1"));
            if (((System.currentTimeMillis() - time) / 1000) > 2) {
                System.out.println("Connecting to HackerServer...");
                time = System.currentTimeMillis();
            }
        } while (currentNum == -1);

        System.out.println("Connected To HackerServer");

        // Registry Client
        currentNum += 1;
        numClientsEntry.setString("" + currentNum);

        // New Entry
        NetworkTableEntry newClientEntry = inst.getTable(tableKey).getEntry(entryKey + currentNum);
        newClientEntry.setString("");
        newClientEntry.addListener((entryNotification) -> {
            System.out.println("HS: " + newClientEntry.getString("Unable To Retrieve Message"));
        }, EntryListenerFlags.kUpdate | EntryListenerFlags.kImmediate);

        // Communicate With Server
        Scanner scanner = new Scanner(System.in);
        while(true) {
            newClientEntry.setString(scanner.nextLine());
        }

    }

}
