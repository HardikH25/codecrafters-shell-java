import java.util.Scanner;
import java.io.File;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            System.out.print("$ ");
            String input = scanner.nextLine();
            
            if (input == null || input.trim().isEmpty()) continue;

            String[] tokens = input.trim().split("\\s+");
            String command = tokens[0];
            
            if (command.equals("exit")) {
                break;
                
            } else if (input.startsWith("echo ")) {
                System.out.println(input.substring(5));
                
            } else if (command.equals("type")) {
                String target = tokens[1];
                //builtin check
                if (target.equals("echo") || target.equals("exit") || target.equals("type")) {
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
                //run external program
                try {
                    // ProcessBuilder automatically takes the array of words (tokens)
                    // and asks the OS to run the first word as a program, passing the rest as arguments.
                    ProcessBuilder pb = new ProcessBuilder(tokens);
                    
                    // inheritIO() is CRUCIAL. It tells the OS: "Let this new program use my shell's screen to print its output."
                    pb.inheritIO(); 
                    
                    // Start the program and pause our shell until the program finishes
                    Process process = pb.start();
                    process.waitFor();
                    
                } catch (Exception e) {
                    // If the OS throws an error (e.g., it can't find the program), print not found
                    System.out.println(command + ": command not found");
                }
            }
        }
    }
}