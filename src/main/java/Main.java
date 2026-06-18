import java.util.*;
import java.io.*;
import java.nio.file.*;

public class Main {
    static class Job {
        int id; Process process; String command; long launchSequence;
        public Job(int id, Process process, String command, long launchSequence) {
            this.id = id; this.process = process; this.command = command; this.launchSequence = launchSequence;
        }
    }

    static List<Job> backgroundJobs = new ArrayList<>();
    static long seqCounter = 0;

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            checkBackgroundJobs(false, null, false);
            System.out.print("$ ");
            String input = scanner.nextLine();
            if (input == null || input.trim().isEmpty()) continue;
            
            String[] rawTokens = parseInput(input.trim());
            boolean isBackground = false;
            if (rawTokens[rawTokens.length - 1].equals("&")) {
                isBackground = true;
                rawTokens = Arrays.copyOf(rawTokens, rawTokens.length - 1);
            }

            List<List<String>> pipeline = splitByPipe(rawTokens);
            if (pipeline.size() > 1) {
                executePipeline(pipeline, isBackground, String.join(" ", rawTokens));
            } else {
                executeSingle(pipeline.get(0), isBackground, String.join(" ", rawTokens));
            }
        }
    }

    private static void executePipeline(List<List<String>> pipeline, boolean isBackground, String fullCmd) throws Exception {
        Process lastProcess = null;
        InputStream currentIn = System.in;

        for (int i = 0; i < pipeline.size(); i++) {
            List<String> cmd = pipeline.get(i);
            boolean isLast = (i == pipeline.size() - 1);
            
            if (isBuiltin(cmd.get(0))) {
                // Simplified for brevity: pipe built-in output to next
                executeBuiltin(cmd.toArray(new String[0]), null, false);
            } else {
                ProcessBuilder pb = new ProcessBuilder(cmd);
                if (i != 0) pb.redirectInput(ProcessBuilder.Redirect.PIPE);
                if (!isLast) pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
                Process p = pb.start();
                if (lastProcess != null) lastProcess.getInputStream().transferTo(p.getOutputStream());
                lastProcess = p;
            }
        }
        if (!isBackground) lastProcess.waitFor();
    }

    private static void executeBuiltin(String[] tokens, String outFile, boolean append) throws Exception {
        String cmd = tokens[0];
        if (cmd.equals("echo")) {
            System.out.println(String.join(" ", Arrays.copyOfRange(tokens, 1, tokens.length)));
        } else if (cmd.equals("pwd")) {
            System.out.println(Paths.get("").toAbsolutePath());
        } else if (cmd.equals("type")) {
            String target = tokens[1];
            if (Arrays.asList("echo", "type", "pwd", "cd", "jobs").contains(target)) 
                System.out.println(target + " is a shell builtin");
            else System.out.println(target + ": not found");
        }
    }

    // Include your existing parseInput, checkBackgroundJobs, and helper methods here...
    private static boolean isBuiltin(String cmd) { return Arrays.asList("echo", "type", "pwd", "cd", "jobs").contains(cmd); }
    private static List<List<String>> splitByPipe(String[] tokens) {
        List<List<String>> p = new ArrayList<>();
        List<String> current = new ArrayList<>();
        for(String t : tokens) {
            if(t.equals("|")) { p.add(current); current = new ArrayList<>(); }
            else current.add(t);
        }
        p.add(current);
        return p;
    }
}