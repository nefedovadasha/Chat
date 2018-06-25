package ru.nefedovadasha.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ConsoleHelper {
    private static BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    public static void writeMessage(String message){
        System.out.println(message);
    }

    public static String readString(){
        String line;
        while (true){
            try {
                line = reader.readLine();
                break;
            }
            catch (IOException e){
                System.out.println("An error occurred during text input. Try again.");
            }
        }
        return line;
    }

    public static int readInt(){
        int number;
        while (true){
            try{
                number = Integer.parseInt(readString());
                break;
            }
            catch (NumberFormatException e){
                System.out.println("An error occurred during number input. Try again.");
            }
        }
        return number;
    }
}
