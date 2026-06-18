import java.util.Scanner;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class Main {

    static class Job {
        int id;
        Process process;
        String command;
        long launchSequence; // Tracks true chronological order for + and - markers

        public Job(int id, Process process, String command, long launchSequence) {
            this.id = id;
            this.process = process;
            this.command = command;
            this.launchSequence = launchSequence;
        }
    }

    static List<Job> backgroundJobs = new ArrayList<>();
    static long seqCounter = 0; // Replaces nextJobId to track absolute launch order
    
    public static String[] parseInput(String input) {
        List<String> tokens = new ArrayList<>();
        StringBuilder currentToken = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inToken = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (c == '\\') {
                if (inSingleQuote) {
                    currentToken.append(c);
                    inToken = true;
                } else if (inDoubleQuote) {
                    if (i + 1 < input.length()) {
                        char next = input.charAt(i + 1);
                        if (next == '"' || next == '\\') {
                            currentToken.append(next);
                            i++;
                        } else {
                            currentToken.append(c);
                        }
                    } else {
                        currentToken.append(c);
                    }
                    inToken = true;
                } else {
                    if (i + 1 < input.length()) {
                        currentToken.append(input.charAt(i + 1));
                        i++;
                        inToken = true;
                    }
                }
            } else if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                inToken = true;
            } else if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                inToken = true;
            } else if (Character.isWhitespace(c) && !inSingleQuote && !inDoubleQuote) {
                if (inToken) {
                    tokens.add(currentToken.toString());
                    currentToken.setLength(0);
                    inToken = false;
                }
            } else {
                currentToken.append(c);
                inToken = true;
            }
        }
        
        if (inToken) {
            tokens.add(currentToken.toString());
        }
        
        return tokens.toArray(new String[0]);
    }

    public static void printOut(String text, String outFile, boolean append) throws Exception {
        if (outFile != null) {
            new File(outFile).getParentFile().mkdirs();
            if (append) {
                Files.writeString(Paths.get(outFile), text + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } else {
                Files.writeString(Paths.get(outFile), text + "\n", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
        } else {
            System.out.println(text);
        }
    }

    public static void checkBackgroundJobs(boolean isJobsCommand, String outFile, boolean appendOut) throws Exception {
        // Find the most recent (+) and second most recent (-) jobs based on true sequence
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
                    String output = String.format("[%d]%c  Running                 %s", job.id, marker, job.command);
                    printOut(output, outFile, appendOut);
                }
            } else {
                String doneCmd = job.command;
                if (doneCmd.endsWith(" &")) {
                    doneCmd = doneCmd.substring(0, doneCmd.length() - 2);
                } else if (doneCmd.endsWith("&")) {
                    doneCmd = doneCmd.substring(0, doneCmd.length() - 1);
                }
                String output = String.format("[%d]%c  Done                    %s", job.id, marker, doneCmd);
                if (isJobsCommand) {
                    printOut(output, outFile, appendOut);
                } else {
                    System.out.println(output);
                }
                toRemove.add(job);
            }
        }
        backgroundJobs.removeAll(toRemove);
    }

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        Path currentDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        
        while (true) {
            checkBackgroundJobs(false, null, false);
            
            System.out.print("$ ");
            String input = scanner.nextLine();
            
            if (input == null || input.trim().isEmpty()) continue;

            String[] rawTokens = parseInput(input.trim());
            if (rawTokens.length == 0) continue;

            String fullCommand = String.join(" ", rawTokens);
            boolean isBackground = false;
            
            if (rawTokens[rawTokens.length - 1].equals("&")) {
                isBackground = true;
                String[] newRawTokens = new String[rawTokens.length - 1];
                System.arraycopy(rawTokens, 0, newRawTokens, 0, rawTokens.length - 1);
                rawTokens = newRawTokens;
            }
            
            if (rawTokens.length == 0) continue;

            String outFile = null;
            String errFile = null;
            boolean appendOut = false;
            boolean appendErr = false;
            int opIdx = -1;
            
            for (int i = 0; i < rawTokens.length; i++) {
                if (rawTokens[i].equals(">") || rawTokens[i].equals("1>")) {
                    if (i + 1 < rawTokens.length) {
                        outFile = rawTokens[i + 1];
                        opIdx = i;
                    }
                    break;
                } else if (rawTokens[i].equals(">>") || rawTokens[i].equals("1>>")) {
                    if (i + 1 < rawTokens.length) {
                        outFile = rawTokens[i + 1];
                        appendOut = true;
                        opIdx = i;
                    }
                    break;
                } else if (rawTokens[i].equals("2>")) {
                    if (i + 1 < rawTokens.length) {
                        errFile = rawTokens[i + 1];
                        opIdx = i;
                    }
                    break;
                } else if (rawTokens[i].equals("2>>")) {
                    if (i + 1 < rawTokens.length) {
                        errFile = rawTokens[i + 1];
                        appendErr = true;
                        opIdx = i;
                    }
                    break;
                }
            }

            String[] tokens;
            if (opIdx != -1) {
                tokens = new String[opIdx];
                System.arraycopy(rawTokens, 0, tokens, 0, opIdx);
            } else {
                tokens = rawTokens;
            }

            if (tokens.length == 0) continue;
            String command = tokens[0];
            
            boolean isBuiltin = command.equals("exit") || command.equals("echo") || command.equals("pwd") || command.equals("cd") || command.equals("type") || command.equals("jobs");
            
            if (isBuiltin && errFile != null) {
                new File(errFile).getParentFile().mkdirs();
                if (appendErr) {
                    Files.writeString(Paths.get(errFile), "", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } else {
                    Files.writeString(Paths.get(errFile), "", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                }
            }
            
            if (command.equals("exit")) {
                break;
                
            } else if (command.equals("jobs")) {
                checkBackgroundJobs(true, outFile, appendOut);
                
            } else if (command.equals("echo")) {
                StringBuilder echoOutput = new StringBuilder();
                for (int i = 1; i < tokens.length; i++) {
                    echoOutput.append(tokens[i]);
                    if (i < tokens.length - 1) {
                        echoOutput.append(" ");
                    }
                }
                printOut(echoOutput.toString(), outFile, appendOut);
                
            } else if (command.equals("pwd")) {
                printOut(currentDir.toString(), outFile, appendOut);
                
            } else if (command.equals("cd")) {
                String target = tokens.length > 1 ? tokens[1] : "";
                
                if (target.equals("~")) {
                    currentDir = Paths.get(System.getenv("HOME"));
                } else {
                    Path newPath = currentDir.resolve(target).normalize();
                    if (new File(newPath.toString()).isDirectory()) {
                        currentDir = newPath;
                    } else {
                        System.out.println("cd: " + target + ": No such file or directory");
                    }
                }
                
            } else if (command.equals("type")) {
                String target = tokens.length > 1 ? tokens[1] : "";
                if (target.equals("echo") || target.equals("exit") || target.equals("type") || target.equals("pwd") || target.equals("cd") || target.equals("jobs")) {
                    printOut(target + " is a shell builtin", outFile, appendOut);
                } else {
                    String pathEnv = System.getenv("PATH");
                    boolean found = false;
                    if (pathEnv != null) {
                        String[] directories = pathEnv.split(":"); 
                        for (String dir : directories) {
                            File file = new File(dir, target);
                            if (file.exists() && file.canExecute()) {
                                printOut(target + " is " + file.getAbsolutePath(), outFile, appendOut);
                                found = true;
                                break; 
                            }
                        }
                    }
                    if (!found) {
                        printOut(target + ": not found", outFile, appendOut);
                    }
                }
                
            } else {
                try {
                    ProcessBuilder pb = new ProcessBuilder(tokens);
                    pb.directory(new File(currentDir.toString())); 
                    
                    pb.inheritIO();
                    
                    if (outFile != null) {
                        File out = new File(outFile);
                        out.getParentFile().mkdirs();
                        if (appendOut) {
                            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(out));
                        } else {
                            pb.redirectOutput(out);
                        }
                    }
                    
                    if (errFile != null) {
                        File err = new File(errFile);
                        err.getParentFile().mkdirs();
                        if (appendErr) {
                            pb.redirectError(ProcessBuilder.Redirect.appendTo(err));
                        } else {
                            pb.redirectError(err);
                        }
                    }
                    
                    Process process = pb.start();
                    
                    if (isBackground) {
                        // Find the smallest available ID
                        int newJobId = 1;
                        while (true) {
                            boolean found = false;
                            for (Job j : backgroundJobs) {
                                if (j.id == newJobId) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) break; // We found a gap!
                            newJobId++;
                        }
                        
                        System.out.println("[" + newJobId + "] " + process.pid());
                        backgroundJobs.add(new Job(newJobId, process, fullCommand, seqCounter++));
                        
                        // Keep the list sorted by ID so jobs command prints them in order!
                        backgroundJobs.sort((a, b) -> a.id - b.id);
                    } else {
                        process.waitFor();
                    }
                } catch (Exception e) {
                    System.out.println(command + ": command not found");
                }
            }
        }
    }
}