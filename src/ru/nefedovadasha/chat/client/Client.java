package ru.nefedovadasha.chat.client;

import ru.nefedovadasha.chat.Connection;
import ru.nefedovadasha.chat.ConsoleHelper;
import ru.nefedovadasha.chat.Message;
import ru.nefedovadasha.chat.MessageType;

import java.io.IOException;
import java.net.Socket;

public class Client {
    protected Connection connection;
    private volatile boolean clientConnected = false;

    public void run(){
        SocketThread socketThread = getSocketThread();
        socketThread.setDaemon(true);
        socketThread.start();
        synchronized (this){
            try{
                wait();
            }
            catch (InterruptedException e){
                ConsoleHelper.writeMessage("Error waiting");
                return;
            }
        }

        if (clientConnected){
            ConsoleHelper.writeMessage("The connection is established. To exit enter 'exit'.");
        }
        else {
            ConsoleHelper.writeMessage("An error occurred during client.");
        }
        while (clientConnected){
            String text = ConsoleHelper.readString();
            if ("exit".equals(text)){
                break;
            }
            if (shouldSendTextFromConsole()){
                sendTextMessage(text);
            }
        }
    }

    protected String getServerAddress(){
        ConsoleHelper.writeMessage("Enter your IP-address:");
        return ConsoleHelper.readString();
    }

    protected int getServerPort(){
        ConsoleHelper.writeMessage("Enter your port:");
        return ConsoleHelper.readInt();
    }

    protected String getUserName(){
        ConsoleHelper.writeMessage("Enter your name:");
        return ConsoleHelper.readString();
    }

    protected boolean shouldSendTextFromConsole(){
        return true;
    }

    protected SocketThread getSocketThread(){
        return new SocketThread();
    }

    protected void sendTextMessage(String text){
        try {
            connection.send(new Message(MessageType.TEXT,text));
        } catch (IOException e) {
            ConsoleHelper.writeMessage("Error send message");
            clientConnected = false;
        }
    }
    public class SocketThread extends Thread{
        @Override
        public void run(){
            String address = getServerAddress();
            int port = getServerPort();
            try {
                Socket socket = new Socket(address,port);
                connection = new Connection(socket);
                clientHandshake();
                clientMainLoop();
            }
            catch (IOException | ClassNotFoundException e) {
                notifyConnectionStatusChanged(false);
            }

        }

        protected void processIncomingMessage(String message){
            ConsoleHelper.writeMessage(message);
        }

        protected void informAboutAddingNewUser(String userName){
            ConsoleHelper.writeMessage(userName + " joined the chat");
        }

        protected void informAboutDeletingNewUser(String userName){
            ConsoleHelper.writeMessage(userName + " left the chat");
        }

        protected void notifyConnectionStatusChanged(boolean clientConnected){
            Client.this.clientConnected = clientConnected;
            synchronized (Client.this){
                Client.this.notify();
            }
        }

        protected void clientHandshake() throws IOException, ClassNotFoundException{
            Message message;
            while (true){
                message = connection.receive();
                if (message.getType() == MessageType.NAME_REQUEST){
                    connection.send(new Message(MessageType.USER_NAME,getUserName()));
                }
                else if (message.getType() == MessageType.NAME_ACCEPTED){
                    notifyConnectionStatusChanged(true);
                    break;
                }
                else {
                   throw new  IOException("Unexpected MessageType");
                }
            }
        }

        protected void clientMainLoop() throws IOException, ClassNotFoundException{
            Message message;
            while (true) {
                message = connection.receive();
                if (message.getType() == MessageType.TEXT){
                    processIncomingMessage(message.getData());
                }
                else if (message.getType() == MessageType.USER_ADDED){
                    informAboutAddingNewUser(message.getData());
                }
                else if (message.getType() == MessageType.USER_REMOVED){
                    informAboutDeletingNewUser(message.getData());
                }
                else{
                    throw new IOException("Unexpected MessageType");
                }
            }
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }
}
