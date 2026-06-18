import java.util.Scanner;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        
        // Remember where our shell starts!
        // System.getProperty("user.dir") gets the folder where you launched the program.
        Path currentDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        
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
                
            } else if (command.equals("pwd")) {
                // Print Working Directory
                // print the variable we created at the top!
                System.out.println(currentDir.toString());
                
            } else if (command.equals("type")) {
                String target = tokens[1];
                //check buitlin
                if (target.equals("echo") || target.equals("exit") || target.equals("type") || target.equals("pwd")) {
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