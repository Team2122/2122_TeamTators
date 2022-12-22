package org.teamtators.sassitator;

import edu.wpi.first.networktables.NetworkTableInstance;

public class NetworkTablesClient {

    protected NetworkTableInstance inst;
    protected final String server = "10.21.22.2";
//    protected final String server = "localhost";

    public NetworkTablesClient() {
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

}
