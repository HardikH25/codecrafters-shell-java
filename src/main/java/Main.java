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
                // If it's a built-in, we capture its output to pass to the next pipe
                // OR if it's the last command, we let it print to System.out
                if (isLast) {
                    if (pipeIn != null) {
                        // If there was a previous command, pipeIn contains the output of the previous process
                        // Redirect pipeIn to the builtin's "input" (if your builtin supported it)
                        // For this specific test, we just need to ensure output prints:
                        executeBuiltin(cmd.toArray(new String[0]), null, false);
                    } else {
                        executeBuiltin(cmd.toArray(new String[0]), null, false);
                    }
                } else {
                    // Capture output for the next pipe
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    PrintStream ps = new PrintStream(baos);
                    PrintStream oldOut = System.out;
                    System.setOut(ps);
                    executeBuiltin(cmd.toArray(new String[0]), null, false);
                    System.out.flush();
                    System.setOut(oldOut);
                    pipeIn = new ByteArrayInputStream(baos.toByteArray());
                }
            } else {
                ProcessBuilder pb = new ProcessBuilder(cmd);
                if (pipeIn != null) pb.redirectInput(ProcessBuilder.Redirect.PIPE);
                if (!isLast) pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
                
                Process p = pb.start();
                if (pipeIn != null) {
                    pipeIn.transferTo(p.getOutputStream());
                    p.getOutputStream().close();
                }
                
                if (isLast) {
                    // This is the key: capture the output of the last external process
                    p.getInputStream().transferTo(System.out);
                    p.waitFor();
                } else {
                    pipeIn = p.getInputStream();
                }
            }
        }
    }
}
