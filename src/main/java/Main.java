import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.nio.file.StandardOpenOption;

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
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;
            
            String[] rawTokens = parseInput(input);
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

    private static void executePipeline(List<List<String>> pipeline, boolean isBackground, String fullCmd) throws Exception {
        InputStream pipeIn = null;
        for (int i = 0; i < pipeline.size(); i++) {
            List<String> cmd = pipeline.get(i);
            boolean isLast = (i == pipeline.size() - 1);

            if (isBuiltin(cmd.get(0))) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(baos);
                PrintStream oldOut = System.out;
                System.setOut(ps);
                executeBuiltin(cmd.toArray(new String[0]), null, false);
                System.out.flush();
                System.setOut(oldOut);
                pipeIn = new ByteArrayInputStream(baos.toByteArray());
            } else {
                ProcessBuilder pb = new ProcessBuilder(cmd);
                if (pipeIn != null) pb.redirectInput(ProcessBuilder.Redirect.PIPE);
                if (!isLast) pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
                Process p = pb.start();
                if (pipeIn != null) { pipeIn.transferTo(p.getOutputStream()); p.getOutputStream().close(); }
                pipeIn = p.getInputStream();
                if (isLast) { p.getInputStream().transferTo(System.out); p.waitFor(); }
            }
        }
    }

    private static void executeSingle(List<String> tokens, boolean isBackground, String fullCmd) throws Exception {
        if (isBuiltin(tokens.get(0))) {
            executeBuiltin(tokens.toArray(new String[0]), null, false);
        } else {
            ProcessBuilder pb = new ProcessBuilder(tokens);
            pb.inheritIO();
            Process p = pb.start();
            if (!isBackground) p.waitFor();
            else { /* Logic for background job tracking ... */ }
        }
    }

    private static void executeBuiltin(String[] tokens, String outFile, boolean append) throws Exception {
        String cmd = tokens[0];
        if (cmd.equals("echo")) System.out.println(String.join(" ", Arrays.copyOfRange(tokens, 1, tokens.length)));
        else if (cmd.equals("pwd")) System.out.println(Paths.get("").toAbsolutePath());
        else if (cmd.equals("type")) {
            String target = tokens[1];
            if (Arrays.asList("echo", "type", "pwd", "cd", "jobs").contains(target)) 
                System.out.println(target + " is a shell builtin");
            else System.out.println(target + ": not found");
        }
    }

    public static String[] parseInput(String input) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inSingle = false, inDouble = false;
        for (char c : input.toCharArray()) {
            if (c == '\\' && !inSingle) { /* Handle escapes */ }
            else if (c == '\'' && !inDouble) inSingle = !inSingle;
            else if (c == '"' && !inSingle) inDouble = !inDouble;
            else if (Character.isWhitespace(c) && !inSingle && !inDouble) {
                if (sb.length() > 0) { tokens.add(sb.toString()); sb.setLength(0); }
            } else sb.append(c);
        }
        if (sb.length() > 0) tokens.add(sb.toString());
        return tokens.toArray(new String[0]);
    }

    public static void checkBackgroundJobs(boolean isJobsCommand, String outFile, boolean append) throws Exception { /* Logic to reap zombies */ }
    private static boolean isBuiltin(String cmd) { return Arrays.asList("echo", "type", "pwd", "cd", "jobs").contains(cmd); }
    private static List<List<String>> splitByPipe(String[] tokens) {
        List<List<String>> p = new ArrayList<>(); List<String> cur = new ArrayList<>();
        for(String t : tokens) { if(t.equals("|")) { p.add(cur); cur = new ArrayList<>(); } else cur.add(t); }
        p.add(cur); return p;
    }
}
