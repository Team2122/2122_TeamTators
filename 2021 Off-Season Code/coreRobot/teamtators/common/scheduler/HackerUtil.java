package org.teamtators.common.scheduler;

import java.util.ArrayList;

public final class HackerUtil {

    // Hacker Keys
    public static final String hackerTableKey = "HackerTable";
    public static final String hackerMainKey = "HackerMain";
    public static final String hackerClientEntryExtension = "_client";

    public static String sepChar = "~";

    // Specials
    public static final String CLIENT_SHUTDOWN = "CLIENT_SHUTDOWN";
    public static final String SERVER_SHUTDOWN = "SERVER_SHUTDOWN";
    public static final String CLIENT_REGISTERED = "CLIENT_REGISTERED";
    public static final String ECHO = "ECHO";

    private HackerUtil() {

    }

    public static ArrayList<String> getArguments(String input) { // If error put error message in 0th index of ArrayList

        ArrayList<String> list = new ArrayList<>();

        if (input.length() == 0) {
            return list;
        }

        int p1 = 0, p2;

        while (true) {

//            System.out.println("Here");

            p2 = input.indexOf(sepChar);

            if (p2 == -1) {
                list.add(input.substring(p1));
//                System.out.println("Exiting");
                break;
            }

            list.add(input.substring(p1, p2));

            input = input.replaceFirst(sepChar, "");
            p1 = p2;

        }

        return list;
    }

    public static String combine(ArrayList<String> arguments) {
        String string = "";

        if (arguments.size() == 0) {
            return string;
        }

        for (int i = 0; i < arguments.size() - 1; i++) {
            string += arguments.get(i) + sepChar;
        }

        string += arguments.get(arguments.size() - 1);

        return string;
    }



}
