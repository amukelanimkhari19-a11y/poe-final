import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * MessageManager.java
 * PROG POE Part 3 - keeps every message in parallel arrays and lets the user
 * search, delete and report on them. Stored messages are saved to / read from a JSON file.
 *
 * References:
 * JSON.org. n.d. Introducing JSON. Available at: https://www.json.org/json-en.html
 * [Accessed 14 November 2026].
 * Oracle. n.d. Class Files. Available at:
 * https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/nio/file/Files.html
 * [Accessed 14 November 2026].
 */
public class MessageManager {

    public static final String FLAG_SENT = "Sent";
    public static final String FLAG_STORED = "Stored";
    public static final String FLAG_DISREGARD = "Disregard";

    // Parallel arrays: the same index in each list describes the same message.
    private final List<String> messageIDs = new ArrayList<>();
    private final List<String> messageHashes = new ArrayList<>();
    private final List<String> recipients = new ArrayList<>();
    private final List<String> messageTexts = new ArrayList<>();
    private final List<String> flags = new ArrayList<>();

    // The three message arrays required by the brief.
    private final List<String> sentMessages = new ArrayList<>();
    private final List<String> disregardedMessages = new ArrayList<>();
    private final List<String> storedMessages = new ArrayList<>();

    /** Adds a message to the arrays; flag is FLAG_SENT, FLAG_STORED or FLAG_DISREGARD. */
    public void addMessage(Message message, String flag) {
        messageIDs.add(message.getMessageID());
        messageHashes.add(message.getMessageHash());
        recipients.add(message.getRecipientCell());
        messageTexts.add(message.getMessageText());
        flags.add(flag);
        categoryFor(flag).add(message.getMessageText());
    }

    private List<String> categoryFor(String flag) {
        if (flag.equals(FLAG_SENT)) {
            return sentMessages;
        }
        if (flag.equals(FLAG_STORED)) {
            return storedMessages;
        }
        return disregardedMessages;
    }

    public List<String> getSentMessages() {
        return new ArrayList<>(sentMessages);
    }

    public List<String> getDisregardedMessages() {
        return new ArrayList<>(disregardedMessages);
    }

    public List<String> getStoredMessages() {
        return new ArrayList<>(storedMessages);
    }

    public List<String> getMessageHashes() {
        return new ArrayList<>(messageHashes);
    }

    public List<String> getMessageIDs() {
        return new ArrayList<>(messageIDs);
    }

    /** Lists the sender and recipient of every stored message. */
    public String displaySendersAndRecipients(String sender) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < flags.size(); i++) {
            if (flags.get(i).equals(FLAG_STORED)) {
                out.append("Sender: ").append(sender)
                        .append(" | Recipient: ").append(recipients.get(i)).append("\n");
            }
        }
        return out.length() == 0 ? "No stored messages found." : out.toString();
    }

    /** The longest message in the stored messages array. */
    public String getLongestStoredMessage() {
        String longest = "";
        for (String text : storedMessages) {
            if (text.length() > longest.length()) {
                longest = text;
            }
        }
        return longest.isEmpty() ? "No stored messages found." : longest;
    }

    public String getMessageByID(String id) {
        int index = messageIDs.indexOf(id);
        return index < 0 ? "Message ID not found." : messageTexts.get(index);
    }

    /** Recipient and message for a message ID. */
    public String searchByMessageID(String id) {
        int index = messageIDs.indexOf(id);
        if (index < 0) {
            return "Message ID not found.";
        }
        return "Recipient: " + recipients.get(index) + "\nMessage: " + messageTexts.get(index);
    }

    /** All sent or stored messages for one recipient (disregarded messages are skipped). */
    public List<String> searchByRecipient(String recipient) {
        List<String> found = new ArrayList<>();
        for (int i = 0; i < recipients.size(); i++) {
            boolean sentOrStored = !flags.get(i).equals(FLAG_DISREGARD);
            if (sentOrStored && recipients.get(i).equals(recipient)) {
                found.add(messageTexts.get(i));
            }
        }
        return found;
    }

    /** Deletes the message with this hash from every array. */
    public String deleteByHash(String hash) {
        int index = messageHashes.indexOf(hash);
        if (index < 0) {
            return "Message hash not found.";
        }
        String text = messageTexts.get(index);
        categoryFor(flags.get(index)).remove(text);
        messageIDs.remove(index);
        messageHashes.remove(index);
        recipients.remove(index);
        messageTexts.remove(index);
        flags.remove(index);

        // The brief shows the closing quote straight after "time", so the final full stop is dropped.
        String shown = text.endsWith(".") ? text.substring(0, text.length() - 1) : text;
        return "Message: \"" + shown + "\" successfully deleted.";
    }

    /** Report with the full details of every message that has this flag. */
    public String generateReport(String flag) {
        StringBuilder report = new StringBuilder("=== " + flag + " Messages Report ===\n");
        int count = 0;
        for (int i = 0; i < flags.size(); i++) {
            if (flags.get(i).equals(flag)) {
                count++;
                report.append(count).append(". Message ID: ").append(messageIDs.get(i))
                        .append("\n   Message Hash: ").append(messageHashes.get(i))
                        .append("\n   Recipient: ").append(recipients.get(i))
                        .append("\n   Message: ").append(messageTexts.get(i)).append("\n");
            }
        }
        if (count == 0) {
            report.append("No messages.\n");
        }
        return report.toString();
    }

    // ----- JSON file -----

    /** Writes every stored message to the file as a JSON array. */
    public void saveStoredMessages(Path file) throws IOException {
        StringBuilder json = new StringBuilder("[\n");
        boolean first = true;
        for (int i = 0; i < flags.size(); i++) {
            if (!flags.get(i).equals(FLAG_STORED)) {
                continue;
            }
            if (!first) {
                json.append(",\n");
            }
            first = false;
            json.append("  {\"messageID\": \"").append(escapeJson(messageIDs.get(i))).append("\", ")
                    .append("\"messageHash\": \"").append(escapeJson(messageHashes.get(i))).append("\", ")
                    .append("\"recipient\": \"").append(escapeJson(recipients.get(i))).append("\", ")
                    .append("\"message\": \"").append(escapeJson(messageTexts.get(i))).append("\"}");
        }
        json.append("\n]\n");
        Files.writeString(file, json.toString(), StandardCharsets.UTF_8);
    }

    /** Reads the JSON file into the stored arrays; returns how many messages were added. */
    public int loadStoredMessages(Path file) throws IOException {
        if (!Files.exists(file)) {
            return 0;
        }
        String json = Files.readString(file, StandardCharsets.UTF_8);
        List<String[]> entries;
        try {
            entries = parseStoredMessages(json);
        } catch (StringIndexOutOfBoundsException e) {
            throw new IOException("The stored messages file is not valid JSON.", e);
        }
        int loaded = 0;
        for (String[] entry : entries) {
            if (messageIDs.contains(entry[0])) {
                continue; // already in the arrays
            }
            messageIDs.add(entry[0]);
            messageHashes.add(entry[1]);
            recipients.add(entry[2]);
            messageTexts.add(entry[3]);
            flags.add(FLAG_STORED);
            storedMessages.add(entry[3]);
            loaded++;
        }
        return loaded;
    }

    /** Each entry is {messageID, messageHash, recipient, message}. */
    private static List<String[]> parseStoredMessages(String json) {
        List<String[]> entries = new ArrayList<>();
        int position = 0;
        while (true) {
            int start = json.indexOf('{', position);
            if (start < 0) {
                break;
            }
            String[] entry = new String[4];
            int i = start + 1;
            while (json.charAt(i) != '}') {
                if (json.charAt(i) != '"') {
                    i++;
                    continue;
                }
                StringBuilder key = new StringBuilder();
                i = readJsonString(json, i, key);
                i = json.indexOf(':', i) + 1;
                while (Character.isWhitespace(json.charAt(i))) {
                    i++;
                }
                StringBuilder value = new StringBuilder();
                i = readJsonString(json, i, value);
                switch (key.toString()) {
                    case "messageID":
                        entry[0] = value.toString();
                        break;
                    case "messageHash":
                        entry[1] = value.toString();
                        break;
                    case "recipient":
                        entry[2] = value.toString();
                        break;
                    case "message":
                        entry[3] = value.toString();
                        break;
                    default:
                        break;
                }
            }
            entries.add(entry);
            position = i + 1;
        }
        return entries;
    }

    /** Reads a JSON string that opens at openQuote; returns the index just after the closing quote. */
    private static int readJsonString(String json, int openQuote, StringBuilder out) {
        int i = openQuote + 1;
        while (json.charAt(i) != '"') {
            char c = json.charAt(i);
            if (c == '\\') {
                i++;
                char escaped = json.charAt(i);
                if (escaped == 'n') {
                    out.append('\n');
                } else if (escaped == 't') {
                    out.append('\t');
                } else if (escaped == 'r') {
                    out.append('\r');
                } else {
                    out.append(escaped);
                }
            } else {
                out.append(c);
            }
            i++;
        }
        return i + 1;
    }

    private static String escapeJson(String text) {
        StringBuilder safe = new StringBuilder();
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"':
                    safe.append("\\\"");
                    break;
                case '\\':
                    safe.append("\\\\");
                    break;
                case '\n':
                    safe.append("\\n");
                    break;
                case '\r':
                    safe.append("\\r");
                    break;
                case '\t':
                    safe.append("\\t");
                    break;
                default:
                    safe.append(c);
            }
        }
        return safe.toString();
    }
}
