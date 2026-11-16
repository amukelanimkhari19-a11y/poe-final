import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

/**
 * Main.java
 * Console entry point: registration, login, then the QuickChat menu.
 * No GUI / JOptionPane is used, per the assignment's requirements.
 */
public class Main {

    private static final Path STORED_FILE = Path.of("messages.json");

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        Login account = register(scanner);
        if (login(scanner, account)) {
            runQuickChat(scanner, account);
        }
        scanner.close();
    }

    private static Login register(Scanner scanner) {
        System.out.println("=== Registration ===");
        String firstName = ask(scanner, "First name: ");
        String lastName = ask(scanner, "Last name: ");
        String username = ask(scanner, "Username (must contain '_', max 5 characters): ");
        String password = ask(scanner, "Password (8+ chars, capital letter, number, special char): ");
        String cellNumber = ask(scanner, "Cell number (e.g. +27838968976): ");

        Login account = new Login(firstName, lastName, username, password, cellNumber);
        System.out.println(account.registerUser());
        System.out.println(account.registerCellPhoneNumber());
        return account;
    }

    private static boolean login(Scanner scanner, Login account) {
        System.out.println("\n=== Login ===");
        String username = ask(scanner, "Username: ");
        String password = ask(scanner, "Password: ");
        boolean success = account.loginUser(username, password);
        System.out.println(account.returnLoginStatus(success));
        return success;
    }

    private static void runQuickChat(Scanner scanner, Login account) {
        System.out.println("\nWelcome to QuickChat.");
        MessageManager manager = new MessageManager();
        loadStoredMessages(manager);

        int numMessages = askForNumber(scanner, "How many messages would you like to enter? ");
        int messagesCreated = 0;

        boolean running = true;
        while (running) {
            System.out.println("\n1) Send Messages");
            System.out.println("2) Show recently sent messages");
            System.out.println("3) Quit");
            System.out.println("4) Stored Messages");
            String option = ask(scanner, "Choose an option: ").trim();

            switch (option) {
                case "1":
                    messagesCreated = sendMessages(scanner, manager, messagesCreated, numMessages);
                    break;
                case "2":
                    System.out.println("Coming Soon.");
                    break;
                case "3":
                    running = false;
                    System.out.println("Total messages sent: " + Message.returnTotalMessagess());
                    break;
                case "4":
                    storedMessagesMenu(scanner, manager, account);
                    break;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    /** Lets the user enter messages until the number they chose at the start has been reached. */
    private static int sendMessages(Scanner scanner, MessageManager manager, int created, int total) {
        if (created >= total) {
            System.out.println("You have already entered all " + total + " messages.");
            return created;
        }
        for (; created < total; created++) {
            Message message = readMessage(scanner, created);
            message.createMessageHash();
            System.out.println(message.reportMessageID());

            String choice = askChoice(scanner);
            System.out.println(message.sentMessage(choice));

            if (choice.equals("send")) {
                manager.addMessage(message, MessageManager.FLAG_SENT);
                System.out.println(message.printMessageDetails());
            } else if (choice.equals("store")) {
                manager.addMessage(message, MessageManager.FLAG_STORED);
                saveStoredMessages(manager);
            } else {
                manager.addMessage(message, MessageManager.FLAG_DISREGARD);
            }
        }
        System.out.println("\nAll " + total + " messages have been entered.");
        System.out.println("Total messages sent: " + Message.returnTotalMessagess());
        return created;
    }

    /** Keeps asking until the recipient and the message length are both valid. */
    private static Message readMessage(Scanner scanner, int number) {
        while (true) {
            String recipient = ask(scanner, "Recipient cell number (e.g. +27718693002): ");
            String text = ask(scanner, "Message (max 250 characters): ");
            Message message = new Message(number, recipient, text);

            System.out.println(message.checkRecipientCell());
            System.out.println(message.checkMessageLength());
            if (message.isRecipientValid() && message.isMessageLengthValid()) {
                return message;
            }
        }
    }

    private static String askChoice(Scanner scanner) {
        while (true) {
            String choice = ask(scanner, "Send, Store, or Disregard this message? ").trim().toLowerCase();
            if (choice.equals("send") || choice.equals("store") || choice.equals("disregard")) {
                return choice;
            }
            System.out.println("Please type send, store or disregard.");
        }
    }

    private static void storedMessagesMenu(Scanner scanner, MessageManager manager, Login account) {
        String sender = account.getFirstName() + " " + account.getLastName();
        boolean inMenu = true;
        while (inMenu) {
            System.out.println("\n=== Stored Messages ===");
            System.out.println("a) Display the sender and recipient of all stored messages");
            System.out.println("b) Display the longest stored message");
            System.out.println("c) Search for a message ID");
            System.out.println("d) Search for all messages stored for a recipient");
            System.out.println("e) Delete a message using its hash");
            System.out.println("f) Display a report of all stored messages");
            System.out.println("g) Back to the main menu");
            String option = ask(scanner, "Choose an option: ").trim().toLowerCase();

            switch (option) {
                case "a":
                    System.out.print(manager.displaySendersAndRecipients(sender));
                    break;
                case "b":
                    System.out.println("Longest stored message: " + manager.getLongestStoredMessage());
                    break;
                case "c":
                    System.out.println(manager.searchByMessageID(ask(scanner, "Message ID: ").trim()));
                    break;
                case "d":
                    printMessagesForRecipient(manager, ask(scanner, "Recipient cell number: ").trim());
                    break;
                case "e":
                    System.out.println(manager.deleteByHash(ask(scanner, "Message hash: ").trim()));
                    saveStoredMessages(manager);
                    break;
                case "f":
                    System.out.print(manager.generateReport(MessageManager.FLAG_STORED));
                    break;
                case "g":
                    inMenu = false;
                    break;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    private static void printMessagesForRecipient(MessageManager manager, String recipient) {
        List<String> found = manager.searchByRecipient(recipient);
        if (found.isEmpty()) {
            System.out.println("No messages found for " + recipient + ".");
            return;
        }
        for (String text : found) {
            System.out.println("- " + text);
        }
    }

    /** Reads messages.json (if it exists) into the stored array and shows them to the user. */
    private static void loadStoredMessages(MessageManager manager) {
        try {
            int loaded = manager.loadStoredMessages(STORED_FILE);
            if (loaded > 0) {
                System.out.println("\nLoaded " + loaded + " stored message(s) from " + STORED_FILE + ":");
                System.out.print(manager.generateReport(MessageManager.FLAG_STORED));
            }
        } catch (IOException e) {
            System.out.println("Could not read stored messages: " + e.getMessage());
        }
    }

    private static void saveStoredMessages(MessageManager manager) {
        try {
            manager.saveStoredMessages(STORED_FILE);
        } catch (IOException e) {
            System.out.println("Could not save stored messages: " + e.getMessage());
        }
    }

    private static String ask(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }

    private static int askForNumber(Scanner scanner, String prompt) {
        while (true) {
            try {
                int number = Integer.parseInt(ask(scanner, prompt).trim());
                if (number > 0) {
                    return number;
                }
            } catch (NumberFormatException e) {
                // fall through and ask again
            }
            System.out.println("Please enter a whole number greater than 0.");
        }
    }
}
