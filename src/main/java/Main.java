import java.util.Scanner;
import java.io.File; // We need this to check if files exist

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
                    String pathEnv = System.getenv("PATH"); //list of folders
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
                System.out.println(command + ": command not found");
            }
        }
    }
}