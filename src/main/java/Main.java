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
            checkBackgroundJobs(false);
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
                executePipeline(pipeline, isBackground, input);
            } else {
                executeSingle(pipeline.get(0), isBackground, input);
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

            executeBuiltin(rightCommand.toArray(new String[0]));
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

        if (!isBackground) {
            lastProcess.getInputStream().transferTo(System.out);
            for (Process p : processes) {
                p.waitFor();
            }
        } else {
            addBackgroundJob(lastProcess, fullCmd);
        }
    }

    private static String runBuiltinToString(List<String> tokens) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        PrintStream oldOut = System.out;
        System.setOut(ps);
        executeBuiltin(tokens.toArray(new String[0]));
        System.out.flush();
        System.setOut(oldOut);
        return baos.toString();
    }

    private static void executeSingle(List<String> tokens, boolean isBackground, String fullCmd) throws Exception {
        if (isBuiltin(tokens.get(0))) {
            executeBuiltin(tokens.toArray(new String[0]));
        } else {
            ProcessBuilder pb = new ProcessBuilder(tokens);
            pb.inheritIO();
            try {
                Process p = pb.start();
                if (!isBackground) {
                    p.waitFor();
                } else {
                    addBackgroundJob(p, fullCmd);
                }
            } catch (Exception e) {
                System.out.println(tokens.get(0) + ": command not found");
            }
        }
    }
    
    private static void addBackgroundJob(Process p, String fullCmd) {
        int newJobId = 1;
        while (true) {
            boolean found = false;
            for (Job j : backgroundJobs) {
                if (j.id == newJobId) {
                    found = true;
                    break;
                }
            }
            if (!found) break; 
            newJobId++;
        }
        
        System.out.println("[" + newJobId + "] " + p.pid());
        backgroundJobs.add(new Job(newJobId, p, fullCmd, seqCounter++));
        backgroundJobs.sort((a, b) -> a.id - b.id);
    }

    public static void executeBuiltin(String[] tokens) {
        String cmd = tokens[0].trim();
        if (cmd.equals("echo")) {
            System.out.println(String.join(" ", Arrays.copyOfRange(tokens, 1, tokens.length)));
        } else if (cmd.equals("pwd")) {
            System.out.println(Paths.get("").toAbsolutePath());
        } else if (cmd.equals("jobs")) {
            checkBackgroundJobs(true);
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

    public static void checkBackgroundJobs(boolean isJobsCommand) { 
        Job plusJob = null;
        Job minusJob = null;
        long maxSeq = -1;
        long secondMaxSeq = -1;

        for (Job j : backgroundJobs) {
            if (j.launchSequence > maxSeq) {
                secondMaxSeq = maxSeq;
                minusJob = plusJob;
                maxSeq = j.launchSequence;
                plusJob = j;
            } else if (j.launchSequence > secondMaxSeq) {
                secondMaxSeq = j.launchSequence;
                minusJob = j;
            }
        }

        List<Job> toRemove = new ArrayList<>();
        for (int i = 0; i < backgroundJobs.size(); i++) {
            Job job = backgroundJobs.get(i);
            char marker = ' ';
            if (job == plusJob) marker = '+';
            else if (job == minusJob) marker = '-';
            
            if (job.process.isAlive()) {
                if (isJobsCommand) {
                    System.out.printf("[%d]%c  Running                 %s\n", job.id, marker, job.command);
                }
            } else {
                String doneCmd = job.command;
                if (doneCmd.endsWith(" &")) {
                    doneCmd = doneCmd.substring(0, doneCmd.length() - 2);
                } else if (doneCmd.endsWith("&")) {
                    doneCmd = doneCmd.substring(0, doneCmd.length() - 1);
                }
                
                System.out.printf("[%d]%c  Done                    %s\n", job.id, marker, doneCmd);
                toRemove.add(job);
            }
        }
        backgroundJobs.removeAll(toRemove);
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