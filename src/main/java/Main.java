import java.util.Scanner;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
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
                System.out.println(currentDir.toString());
                
            } else if (command.equals("cd")) {
                String target = tokens[1];
                
                // handle the Home Directory
                if (target.equals("~")) {
                    currentDir = Paths.get(System.getenv("HOME"));
                } else {
                    // Handle absolute and relative paths
                    Path newPath = currentDir.resolve(target).normalize();
                    
                    if (new File(newPath.toString()).isDirectory()) {
                        currentDir = newPath;
                    } else {
                        System.out.println("cd: " + target + ": No such file or directory");
                    }
                }
                
            } else if (command.equals("type")) {
                String target = tokens[1];
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
                } catch (Exception e)
            }
        }
    }
}