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
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;
            
            String[] rawTokens = parseInput(input);
            if (rawTokens.length == 0) continue;
            
            if (rawTokens[0].trim().equals("exit")) {
                System.exit(0);
            }

            boolean isBackground = false;
            if (rawTokens[rawTokens.length - 1].trim().equals("&")) {
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

    private static void executePipeline(List<List<String>> pipelineCommands, boolean isBackground, String fullCmd) throws Exception {
        if (pipelineCommands.size() == 2 && isBuiltin(pipelineCommands.get(0).get(0))) {
            List<String> leftCommand = pipelineCommands.get(0);
            List<String> rightCommand = pipelineCommands.get(1);

            String output = runBuiltinToString(leftCommand);

            ProcessBuilder pb = new ProcessBuilder(rightCommand);
            pb.directory(new File(System.getProperty("user.dir")));
            Process p = pb.start();

            p.getOutputStream().write(output.getBytes());
            p.getOutputStream().close();

            p.getInputStream().transferTo(System.out);
            p.waitFor();
            return;
        }

        if (pipelineCommands.size() == 2 && isBuiltin(pipelineCommands.get(1).get(0))) {
            List<String> leftCommand = pipelineCommands.get(0);
            List<String> rightCommand = pipelineCommands.get(1);

            ProcessBuilder pb = new ProcessBuilder(leftCommand);
            pb.directory(new File(System.getProperty("user.dir")));
            Process p = pb.start();
            p.waitFor(); 

            executeBuiltin(rightCommand.toArray(new String[0]), null, false);
            return;
        }

        List<ProcessBuilder> builders = new ArrayList<>();
        for (List<String> cmd : pipelineCommands) {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(new File(System.getProperty("user.dir")));
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            builders.add(pb);
        }

        List<Process> processes = ProcessBuilder.startPipeline(builders);
        Process lastProcess = processes.get(processes.size() - 1);
        lastProcess.getInputStream().transferTo(System.out);

        for (Process p : processes) {
            p.waitFor();
        }
    }

    private static String runBuiltinToString(List<String> tokens) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        PrintStream oldOut = System.out;
        System.setOut(ps);
        executeBuiltin(tokens.toArray(new String[0]), null, false);
        System.out.flush();
        System.setOut(oldOut);
        return baos.toString();
    }

    private static void executeSingle(List<String> tokens, boolean isBackground, String fullCmd) throws Exception {
        if (isBuiltin(tokens.get(0))) {
            executeBuiltin(tokens.toArray(new String[0]), null, false);
        } else {
            ProcessBuilder pb = new ProcessBuilder(tokens);
            pb.inheritIO();
            try {
                Process p = pb.start();
                if (!isBackground) p.waitFor();
            } catch (Exception e) {
                System.out.println(tokens.get(0) + ": command not found");
            }
        }
    }

    public static void executeBuiltin(String[] tokens, String outFile, boolean append) {
        String cmd = tokens[0].trim();
        if (cmd.equals("echo")) {
            System.out.println(String.join(" ", Arrays.copyOfRange(tokens, 1, tokens.length)));
        } else if (cmd.equals("pwd")) {
            System.out.println(Paths.get("").toAbsolutePath());
        } else if (cmd.equals("type")) {
            String target = tokens.length > 1 ? tokens[1].trim() : "";
            
            if (isBuiltin(target)) {
                System.out.println(target + " is a shell builtin");
            } else {
                String pathEnv = System.getenv("PATH");
                boolean found = false;
                if (pathEnv != null) {
                    for (String dir : pathEnv.split(":")) {
                        File file = new File(dir, target);
                        if (file.exists() && file.canExecute()) {
                            System.out.println(target + " is " + file.getAbsolutePath());
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) System.out.println(target + ": not found");
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
                if (sb.length() > 0) { tokens.add(sb.toString().trim()); sb.setLength(0); }
            } else sb.append(c);
        }
        if (sb.length() > 0) tokens.add(sb.toString().trim());
        return tokens.toArray(new String[0]);
    }

    public static void checkBackgroundJobs(boolean isJobsCommand, String outFile, boolean append) throws Exception { 
    }
    
    private static boolean isBuiltin(String cmd) { 
        if (cmd == null) return false;
        return Arrays.asList("echo", "type", "pwd", "cd", "jobs", "exit").contains(cmd.trim()); 
    }
    
    private static List<List<String>> splitByPipe(String[] tokens) {
        List<List<String>> p = new ArrayList<>(); 
        List<String> cur = new ArrayList<>();
        for(String t : tokens) { 
            if(t.trim().equals("|")) { 
                p.add(cur); 
                cur = new ArrayList<>(); 
            } else {
                cur.add(t.trim());
            } 
        }
        p.add(cur); 
        return p;
    }
}