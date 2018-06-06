package app;


import java.io.*;
import io.javalin.Javalin;
import io.javalin.embeddedserver.jetty.websocket.WsSession;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.String;
import org.eclipse.jetty.websocket.api.Session;
import org.json.JSONObject;
import app.util.HerokuUtil;
import static j2html.TagCreator.article;
import static j2html.TagCreator.attrs;
import static j2html.TagCreator.b;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;
import java.io.FileWriter;

public class Chat {

    private static Map<WsSession, String> userUsernameMap = new ConcurrentHashMap<>();
    private static int nextUserNumber = 1; // Assign to username for next connecting user
    private static LoginTime[] logins = new LoginTime[200];

    public static void main(String[] args) {
        for(int i = 0; i < logins.length; ++i){
            logins[i] = new LoginTime();
        }

        Javalin.create()
                .port(HerokuUtil.getHerokuAssignedPort())
                .enableStaticFiles("/public")
                .ws("/chat", ws -> {
                    // This goes off when a user connects
                    ws.onConnect(session -> {
                        //new LabelEx();
                        String username = "User" + nextUserNumber++;
                        userUsernameMap.put(session, username);
                        broadcastMessage("Server", (username + " joined the chat"));
                    });
                    // This goes off when a user disconnects
                    ws.onClose((session, status, message) -> {
                        String username = userUsernameMap.get(session);
                        userUsernameMap.remove(session);
                        broadcastMessage("Server", (username + " left the chat"));
                    });
                    // This goes off anytime a user sends information to the server, whether its
                    // login credentials, registration credentials, private message, or public message
                    ws.onMessage((session, message) -> {
                        // If the .js file appended ID:REG to the msg, that means it's sending the server
                        // a string in this format: "ID:REGusernameID:PWpassword"
                        if(message.contains("ID:REG")){
                            Integer flag = 0;
                            // throw out the ID:REG part
                            String msg = message.replace("ID:REG", "");
                            // split the message on ID:PW, so parts[0] is user and parts[1] is password
                            String parts[] = msg.split("ID:PW");
                            String user = parts[0];
                            // strip trailing spaces so "Molly" and "Molly  " can't be two different users
                            user.replaceAll("^\\s+|\\s+$", "");
                            String pass = parts[1];

                            // Search to make sure that username isnt already in database
                            try{
                                FileReader reader = new FileReader("accounts.txt");
                                BufferedReader br = new BufferedReader(reader);
                                String readLine;
                                while((readLine = br.readLine()) != null){
                                    String amsg = readLine.replace("ID:UN", "");
                                    String aParts[] = amsg.split("ID:PW");
                                    String aUser = aParts[0];
                                    if(aUser.equals(user)){
                                        // if its already in the database, tell the user its a no go
                                        String error = "Username taken. Please try again.";
                                        broadcastPM("Server", userUsernameMap.get(session), error);
                                        flag = 1;
                                    }
                                }
                                reader.close();
                            } catch(IOException e){
                                e.printStackTrace();
                            }
                            // if username wasn't taken already, write credentials to the file accounts.txt
                            if(flag == 0) {
                                String account = "ID:UN" + user + "ID:PW" + pass;
                                try {
                                    FileWriter writer = new FileWriter("accounts.txt", true);
                                    BufferedWriter bw = new BufferedWriter(writer);
                                    bw.write(account);
                                    bw.newLine();
                                    bw.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                // change their username to the one they registered as
                                String success = "Registration success! You are now logged in.";
                                broadcastPM("Server", userUsernameMap.get(session), success);
                                userUsernameMap.remove(session);
                                userUsernameMap.put(session, user);
                                fixList(userUsernameMap.get(session));
                            }

                            // If the .js file appended ID:LOGIN to the msg, that means it's sending the server
                            // a string in this format: "ID:LOGINusernameID:PWpassword"
                        } else if(message.contains("ID:LOGIN")) {
                            // all of this works the same as ID:REG
                            // There are three message type ids that come from ID:TYPE
                            //    0 - public message
                            //    1 - private message that you sent
                            //    2 - private message that you received

                            Integer logflag = 0;
                            String msg = message.replace("ID:LOGIN","");
                            String parts[] = msg.split("ID:PW");
                            String user = parts[0];
                            String pass = parts[1];

                            try{
                                FileReader logreader = new FileReader("accounts.txt");
                                BufferedReader logbr = new BufferedReader(logreader);
                                String logreadLine;
                                while((logreadLine = logbr.readLine()) != null) {
                                    String amsg = logreadLine.replace("ID:UN", "");
                                    String lParts[] = amsg.split("ID:PW");
                                    String lUser = lParts[0];
                                    String lPass = lParts[1];

                                    // if it finds a matching username and password as the one they used to login
                                    if(lUser.equals(user) && lPass.equals(pass)){
                                        // fixes their username and sends them a success message from the server
                                        userUsernameMap.remove(session);
                                        userUsernameMap.put(session, user);
                                        fixList(userUsernameMap.get(session));
                                        String logsuccess = "Successfully logged in as " + user;
                                        broadcastPM("Server", userUsernameMap.get(session), logsuccess);
                                        logflag = 1;
                                    }
                                }
                                logreader.close();
                            } catch(IOException e){
                                e.printStackTrace();
                            }
                        //    String Mh ="ID:SENDER" + sender + "ID:RECIP" + recip + "ID:MSG" + message + "ID:TYPE" + 2;
                            // send them their message history
                            if(logflag == 1){
                                String thisuser = userUsernameMap.get(session);
                                String thisfile = thisuser + ".txt";
                                try{
                                    FileReader Histreader = new FileReader(thisfile);
                                    BufferedReader bufreader = new BufferedReader(Histreader);
                                    String line;
                                    while((line = bufreader.readLine()) != null){
                                        String temp = line.replace("ID:SENDER", "");
                                        String parts1[] = temp.split("ID:RECIP");
                                        String Sender = parts1[0];
                                        System.out.println("Sender: " + Sender);
                                        String parts2[] = parts1[1].split("ID:MSG");
                                        String Recipient = parts2[0];
                                        System.out.println("Recipient: " + Recipient);
                                        String parts3[] = parts2[1].split("ID:TYPE");
                                        String mes = parts3[0];
                                        System.out.println("Message: " + mes);
                                        String Type = parts3[1];
                                        System.out.println("Type: " + Type);
                                        if(!(Sender.equals("Server") && Recipient.equals("ALL"))) {
                                            if (Type.equals("0")) {
                                                broadcastPMHistory(Sender, userUsernameMap.get(session), mes);
                                                //System.out.println("Are we getting here");
                                               // broadcastMessageHistory(Sender, mes);
                                            } else if (Type.equals("1") || Type.equals("2")) {
                                                broadcastPMHistory(Sender, Recipient, mes);
                                            }
                                        }
                                    }
                                    bufreader.close();
                                } catch(IOException e){
                                    e.printStackTrace();
                                }
                            }

                            // if it DOESNT find a matching user/pass, tells them they done goofed
                            if(logflag == 0) {
                                String logerror = "Invalid credentials. Please try again or register.";
                                broadcastPM("Server", userUsernameMap.get(session), logerror);
                            }
                        // if the .js file appended "ID:PM" to the message, that means its a private message
                        } else if(message.contains("ID:PM")) {
                            // first, write it to that person's message history file
                            /*String mh = message.replace("ID:PM", "ID:RECIP");
                            String sender = userUsernameMap.get(session);
                            String HistoryMSG = "ID:SENDER" + sender + mh;

                            */
                            // parses message same as ID:REG and ID:LOGIN
                            String mesg = message.replace("ID:PM", "");
                            String sendto = mesg;
                            String parts[] = sendto.split("ID:MSG");
                            sendto = parts[0];
                            String amsg = parts[1];
                            // calls the new function broadcastPM, and passes it the sender (session), recipient(sendto), and msg
                            broadcastPM(userUsernameMap.get(session), sendto, amsg);
                        } else {
                            // if the .js file didnt append any special IDs to the msg, its a regular public message
                            broadcastMessage(userUsernameMap.get(session), message);
                        }
                    });
                })
                .start();
    }

    // Function that updates the list after a user's username has been changed
    // goes through each online user (.forEach(session) and updates the list
    private static void fixList(String sender){
        userUsernameMap.keySet().stream().filter(Session::isOpen).forEach(session -> {
            try {
                session.send(
                        new JSONObject()
                                .put("userMessage", "")
                                .put("userlist", userUsernameMap.values()).toString()
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // Sends a message from one user to one user, along with a list of current usernames
    private static void broadcastPM(String sender, String recip, String message) {
       userUsernameMap.keySet().stream().filter(Session::isOpen).forEach(session -> {
           // userUsernameMap.get(session) is variable of type concurrentHashMap
           // when we add a user to the list of online users, we add it in the form: <WsSession session, String username>
           // we can use concurrentHashMap's .get() method : pass it a key, and it returns the value, i.e. the username for that session
           // So here, we're going through each session, or each person who is logged in, one by one
           String Recip = userUsernameMap.get(session);

                // if the name of the current user we're looking at matches the name of the message recipient
               if(Recip.equals(recip)) {
                   String Mh ="ID:SENDER" + sender + "ID:RECIP" + recip + "ID:MSG" + message + "ID:TYPE" + 2;
                   // send them the message
                   if(!recip.contains("User")) {
                       String Filename = sender + ".txt";
                       try {
                           FileWriter HistWriter = new FileWriter(Filename, true);
                           BufferedWriter Bw = new BufferedWriter(HistWriter);
                           Bw.write(Mh);
                           Bw.newLine();
                           Bw.close();
                       } catch (IOException e) {
                           e.printStackTrace();
                       }
                   }
                    try {
                        session.send(
                                new JSONObject()
                                        .put("userMessage", createHtmlMessageFromSenderPrivate(sender, message))
                                        .put("userlist", userUsernameMap.values()).toString()
                        );
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
               } else if(Recip.equals(sender)){
                   String mh ="ID:SENDER" + sender + "ID:RECIP" + recip + "ID:MSG" + message + "ID:TYPE" + 1;
                   // if the name of the current user we're looking at matches the name of the message sender
                   // send them a slightly different message with the new function createHtmlMessageFromSenderPM
                   // All this function does is make it say "You say to recipient: message" instead of "user says:"
                   if(!sender.contains("User")) {
                       String FileName = recip + ".txt";
                       try {
                           FileWriter HWriter = new FileWriter(FileName, true);
                           BufferedWriter bw = new BufferedWriter(HWriter);
                           bw.write(mh);
                           bw.newLine();
                           bw.close();
                       } catch (IOException e) {
                           e.printStackTrace();
                       }
                   }
                   try {
                       session.send(
                               new JSONObject()
                                       .put("userMessage", createHtmlMessageFromSenderPM(recip, message))
                                       .put("userlist", userUsernameMap.values()).toString()
                       );
                   } catch (Exception e) {
                       e.printStackTrace();
                   }
               }
               // If the current user we're looking at isn't the sender or the recipient of the private message,
               // we do nothing because we don't want the message getting sent to them
        });

    }


    // Sends a message from one user to all users, along with a list of current usernames
    private static void broadcastMessage(String sender, String message) {
        String MH = "ID:SENDER" + sender + "ID:RECIP" + "ALL" + "ID:MSG" + message + "ID:TYPE" + 0;
        userUsernameMap.keySet().stream().filter(Session::isOpen).forEach(session -> {
            String current = userUsernameMap.get(session);
            // save message to message history
            if(!current.contains("User")) {
                String filename = current + ".txt";
                try {
                    FileWriter HistoryWriter = new FileWriter(filename, true);
                    BufferedWriter BW = new BufferedWriter(HistoryWriter);
                    BW.write(MH);
                    BW.newLine();
                    BW.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                session.send(
                        new JSONObject()
                                .put("userMessage", createHtmlMessageFromSender(sender, message))
                                .put("userlist", userUsernameMap.values()).toString()
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static void broadcastMessageHistory(String Sender, String message) {
        userUsernameMap.keySet().stream().filter(Session::isOpen).forEach(session -> {
            try {
                session.send(
                        new JSONObject()
                                .put("userMessage", createHtmlMessageFromSender(Sender, message))
                                .put("userlist", userUsernameMap.values()).toString()
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static void broadcastPMHistory(String sender, String recip, String message) {
        userUsernameMap.keySet().stream().filter(Session::isOpen).forEach(session -> {
            String Recip = userUsernameMap.get(session);
            // if the name of the current user we're looking at matches the name of the message recipient
            if(Recip.equals(recip)) {
                try {
                    session.send(
                            new JSONObject()
                                    .put("userMessage", createHtmlMessageFromSenderPrivate(sender, message))
                                    .put("userlist", userUsernameMap.values()).toString()
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if(Recip.equals(sender)){
                try {
                    session.send(
                            new JSONObject()
                                    .put("userMessage", createHtmlMessageFromSenderPM(recip, message))
                                    .put("userlist", userUsernameMap.values()).toString()
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }

    // Builds a HTML element with a sender-name, a message, and a timestamp
    private static String createHtmlMessageFromSender(String sender, String message) {
        return article(
                b(sender + " says:"),
                span(attrs(".timestamp"), new SimpleDateFormat("HH:mm:ss").format(new Date())),
                p(message)
        ).render();
    }

    private static String createHtmlMessageFromSenderPM(String recip, String message) {
        return article(
                b("You say to " + recip + ":"),
                span(attrs(".timestamp"), new SimpleDateFormat("HH:mm:ss").format(new Date())),
                p(message)
        ).render();
    }

    private static String createHtmlMessageFromSenderPrivate(String sender, String message) {
        return article(
                b(sender + " says to you:"),
                span(attrs(".timestamp"), new SimpleDateFormat("HH:mm:ss").format(new Date())),
                p(message)
        ).render();
    }

}