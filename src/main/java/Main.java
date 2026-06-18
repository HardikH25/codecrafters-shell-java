import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            System.out.print("$ ");
            String input = scanner.nextLine();

            String[] tokens = input.trim().split("\\s+");
            String command = tokens[0];
            
            if (command.equals("exit")) {
                break;
            } else if (input.startsWith("echo ")) {
                System.out.println(input.substring(5));
            } else {
                System.out.println(command + ": command not found");
            }
        }
    }
}