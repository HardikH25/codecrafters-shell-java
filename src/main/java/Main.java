import java.util.Scanner;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class Main {

    static class Job {
        int id;
        Process process;
        String command;

        public Job(int id, Process process, String command) {
            this.id = id;
            this.process = process;
            this.command = command;
        }
    }

    static List<Job> backgroundJobs = new ArrayList<>();
    static int nextJobId = 1;
    
    public static String[] parseInput(String input) {
        List<String> tokens = new ArrayList<>();
        StringBuilder currentToken = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inToken = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (c == '\\') {
                if (inSingleQuote) {
                    currentToken.append(c);
                    inToken = true;
                } else if (inDoubleQuote) {
                    if (i + 1 < input.length()) {
                        char next = input.charAt(i + 1);
                        if (next == '"' || next == '\\') {
                            currentToken.append(next);
                            i++;
                        } else {
                            currentToken.append(c);
                        }
                    } else {
                        currentToken.append(c);
                    }
                    inToken = true;
                } else {
                    if (i + 1 < input.length()) {
                        currentToken.append(input.charAt(i + 1));
                        i++;
                        inToken = true;
                    }
                }
            } else if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                inToken = true;
            } else if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                inToken = true;
            } else if (Character.isWhitespace(c) && !inSingleQuote && !inDoubleQuote) {
                if (inToken) {
                    tokens.add(currentToken.toString());
                    currentToken.setLength(0);
                    inToken = false;
                }
            } else {
                currentToken.append(c);
                inToken = true;
            }
        }
        
        if (inToken) {
            tokens.add(currentToken.toString());
        }
        
        return tokens.toArray(new String[0]);
    }

    public static void printOut(String text, String outFile, boolean append) throws Exception {
        if (outFile != null) {
            new File(outFile).getParentFile().mkdirs();
            if (append) {
                Files.writeString(Paths.get(outFile), text + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } else {
                Files.writeString(Paths.get(outFile), text + "\n", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
        } else {
            System.out.println(text);
        }
    }

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        Path currentDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        
        while (true) {
            System.out.print("$ ");
            String input = scanner.nextLine();
            
            if (input == null || input.trim().isEmpty()) continue;

            String[] rawTokens = parseInput(input.trim());
            if (rawTokens.length == 0) continue;

            String fullCommand = String.join(" ", rawTokens);
            boolean isBackground = false;
            
            if (rawTokens[rawTokens.length - 1].equals("&")) {
                isBackground = true;
                String[] newRawTokens = new String[rawTokens.length - 1];
                System.arraycopy(rawTokens, 0, newRawTokens, 0, rawTokens.length - 1);
                rawTokens = newRawTokens;
            }
            
            if (rawTokens.length == 0) continue;

            String outFile = null;
            String errFile = null;
            boolean appendOut = false;
            boolean appendErr = false;
            int opIdx = -1;
            
            for (int i = 0; i < rawTokens.length; i++) {
                if (rawTokens[i].equals(">") || rawTokens[i].equals("1>")) {
                    if (i + 1 < rawTokens.length) {
                        outFile = rawTokens[i + 1];
                        opIdx = i;
                    }
                    break;
                } else if (rawTokens[i].equals(">>") || rawTokens[i].equals("1>>")) {
                    if (i + 1 < rawTokens.length) {
                        outFile = rawTokens[i + 1];
                        appendOut = true;
                        opIdx = i;
                    }
                    break;
                } else if (rawTokens[i].equals("2>")) {
                    if (i + 1 < rawTokens.length) {
                        errFile = rawTokens[i + 1];
                        opIdx = i;
                    }
                    break;
                } else if (rawTokens[i].equals("2>>")) {
                    if (i + 1 < rawTokens.length) {
                        errFile = rawTokens[i + 1];
                        appendErr = true;
                        opIdx = i;
                    }
                    break;
                }
            }

            String[] tokens;
            if (opIdx != -1) {
                tokens = new String[opIdx];
                System.arraycopy(rawTokens, 0, tokens, 0, opIdx);
            } else {
                tokens = rawTokens;
            }

            if (tokens.length == 0) continue;
            String command = tokens[0];
            
            boolean isBuiltin = command.equals("exit") || command.equals("echo") || command.equals("pwd") || command.equals("cd") || command.equals("type") || command.equals("jobs");
            
            if (isBuiltin && errFile != null) {
                new File(errFile).getParentFile().mkdirs();
                if (appendErr) {
                    Files.writeString(Paths.get(errFile), "", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } else {
                    Files.writeString(Paths.get(errFile), "", StandardOpenOption.