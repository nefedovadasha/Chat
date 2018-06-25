package ru.nefedovadasha.chat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    private static class Handler extends Thread{
        private Socket socket;

        private Handler(Socket socket){
            super();
            this.socket = socket;
        }

        @Override
        public void run() {
            ConsoleHelper.writeMessage("Connected with address "+socket.getRemoteSocketAddress());
            String userName = null;

            try (Connection connection = new Connection(socket)){
                userName = serverHandshake(connection);
                sendBroadcastMessage((new Message(MessageType.USER_ADDED,userName)));
                sendListOfUsers(connection,userName);
                serverMainLoop(connection,userName);
            }
            catch (IOException | ClassNotFoundException e){
                ConsoleHelper.writeMessage("Error exchange information with a remote server");
            }
            finally {
                if (userName!=null) {
                    connectionMap.remove(userName);
                    sendBroadcastMessage(new Message(MessageType.USER_REMOVED,userName));
                    ConsoleHelper.writeMessage("Connected with address "+socket.getRemoteSocketAddress()+" close");
                }
            }
        }

        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException{
           Message enterYouName = new Message(MessageType.NAME_REQUEST);
           Message nameAdopted = new Message(MessageType.NAME_ACCEPTED);
           Message answer;
           String nameUser;

           while (true) {
               connection.send(enterYouName);

               answer = connection.receive();


               if (answer.getType() != MessageType.USER_NAME) {
                   System.out.println("Incorrect type message");
                   continue;
               }

               nameUser = answer.getData();

               if (nameUser.isEmpty()) {
                   System.out.println("Incorrect name");
                   continue;
               }

               if (connectionMap.containsKey(nameUser)){
                   System.out.println("Such a user already exists");
                   continue;
               }

               connectionMap.put(nameUser,connection);
               connection.send(nameAdopted);
               break;
           }

           return nameUser;
        }

        private void sendListOfUsers(Connection connection, String userName) throws IOException{
            String nameAnotherUser;

            for (Map.Entry pair:connectionMap.entrySet()) {

                nameAnotherUser = (String)pair.getKey();

                if (!userName.equals(nameAnotherUser)) {
                    connection.send(new Message(MessageType.USER_ADDED, nameAnotherUser));
                }
            }
        }

        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException{
            Message messageClient;
            Message messageForAll;

            while(true){
                messageClient = connection.receive();
                if (messageClient.getType()==MessageType.TEXT){
                    messageForAll = new Message(MessageType.TEXT,userName+": "+messageClient.getData());
                    sendBroadcastMessage(messageForAll);
                }
                else{
                    ConsoleHelper.writeMessage("Error");
                }
            }
        }
    }

    public static void sendBroadcastMessage(Message message){
        Connection connection;
        for (Map.Entry pair:connectionMap.entrySet()) {
            try{
                connection = (Connection)pair.getValue();
                connection.send(message);

            }
            catch (IOException e){
                System.out.println("Can`t send message");
            }
        }
    }

    public static void main(String[] args) throws IOException {
        ConsoleHelper consoleHelper = new ConsoleHelper();
        consoleHelper.writeMessage("Enter port:");
        int port = consoleHelper.readInt();

        try (ServerSocket serverSocket = new ServerSocket(port)){

            System.out.println("Server is running");

            while(true){
                Socket socket = serverSocket.accept();
                Handler handler = new Handler(socket);
                handler.start();
            }
        }
        catch (Exception e){
        }
    }
}
