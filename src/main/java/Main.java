import java.util.Scanner;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Main {
    
    public static String[] parseInput(String input) {
        List<String> tokens = new ArrayList<>();
        StringBuilder currentToken = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inToken = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (c == '\\' && !inSingleQuote && !inDoubleQuote) {
                if (i + 1 < input.length()) {
                    currentToken.append(input.charAt(i + 1));
                    i++;
                    inToken = true;
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

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        Path currentDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        
        while (true) {
            System.out.print("$ ");
            String input = scanner.nextLine();
            
            if (input == null || input.trim().isEmpty()) continue;

            String[] tokens = parseInput(input.trim());
            if (tokens.length == 0) continue;
            String command = tokens[0];
            
            if (command.equals("exit")) {
                break;
                
            } else if (command.equals("echo")) {
                StringBuilder echoOutput = new StringBuilder();
                for (int i = 1; i < tokens.length; i++) {
                    echoOutput.append(tokens[i]);
                    if (i < tokens.length - 1) {
                        echoOutput.append(" ");
                    }
                }
                System.out.println(echoOutput.toString());
                
            } else if (command.equals("pwd")) {
                System.out.println(currentDir.toString());
                
            } else if (command.equals("cd")) {
                String target = tokens.length > 1 ? tokens[1] : "";
                
                if (target.equals("~")) {
                    currentDir = Paths.get(System.getenv("HOME"));
                } else {
                    Path newPath = currentDir.resolve(target).normalize();
                    
                    if (new File(newPath.toString()).isDirectory()) {
                        currentDir = newPath;
                    } else {
                        System.out.println("cd: " + target + ": No such file or directory");
                    }
                }
                
            } else if (command.equals("type")) {
                String target = tokens.length > 1 ? tokens[1] : "";
                if (target.equals("echo") || target.equals("exit") || target.equals("type") || target.equals("pwd") || target.equals("cd")) {
                    System.out.println(target + " is a shell builtin");
                } else {
                    String pathEnv = System.getenv("PATH");
                    boolean found = false;
                    if (pathEnv != null) {
                        String[] directories = pathEnv.split(":"); 
                        for (String dir : directories) {
                            File file = new File(dir, target);
                            if (file.exists() && file.canExecute()) {
                                System.out.println(target + " is " + file.getAbsolutePath());
                                found = true;
                                break; 
                            }
                        }
                    }
                    if (!found) {
                        System.out.println(target + ": not found");
                    }
                }
                
            } else {
                try {
                    ProcessBuilder pb = new ProcessBuilder(tokens);
                    pb.directory(new File(currentDir.toString())); 
                    pb.inheritIO(); 
                    Process process = pb.start();
                    process.waitFor();
                } catch (Exception e) {
                    System.out.println(command + ": command not found");
                }
            }
        }
    }
}