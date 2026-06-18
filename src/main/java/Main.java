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
            if (!scanner.hasNextLine()) break;
            String input = scanner.nextLine();
            if (input == null || input.trim().isEmpty()) continue;
            
            String[] rawTokens = parseInput(input.trim());
            boolean isBackground = false;
            if (rawTokens.length > 0 && rawTokens[rawTokens.length - 1].equals("&")) {
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

    public static String[] parseInput(String input) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inSingle = false, inDouble = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '\\' && !inSingle) {
                if (i + 1 < input.length()) sb.append(input.charAt(++i));
            } else if (c == '\'' && !inDouble) inSingle = !inSingle;
            else if (c == '"' && !inSingle) inDouble = !inDouble;
            else if (Character.isWhitespace(c) && !inSingle && !inDouble) {
                if (sb.length() > 0) { tokens.add(sb.toString()); sb.setLength(0); }
            } else sb.append(c);
        }
        if (sb.length() > 0) tokens.add(sb.toString());
        return tokens.toArray(new String[0]);
    }

    public static void checkBackgroundJobs(boolean isJobsCommand, String outFile, boolean append) throws Exception {
        // ... (Include your existing marker logic + reaping logic here)
    }

    private static void executeSingle(List<String> tokens, boolean isBackground, String fullCmd) throws Exception {
        // ... (Include your full logic for builtins + ProcessBuilder)
    }

    private static void executePipeline(List<List<String>> pipeline, boolean isBackground, String fullCmd) throws Exception {
        List<ProcessBuilder> builders = new ArrayList<>();
        for (List<String> cmd : pipeline) {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            builders.add(pb);
        }
        List<Process> procs = ProcessBuilder.startPipeline(builders);
        if (!isBackground) procs.get(procs.size() - 1).waitFor();
        // ... (Include job tracking for isBackground = true)
    }

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