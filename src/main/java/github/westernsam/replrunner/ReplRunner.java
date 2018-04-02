package github.westernsam.repltest;

import java.io.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import static java.lang.String.join;
import static org.junit.Assert.assertEquals;


public class ReplRunner implements Closeable {

    private String prompt;

    static class Constants {
        static String newLine = System.getProperty("line.separator");
    }

    private final InputStream testInput;
    private final OutputStream testOutput;
    private final Runnable repl;

    private final boolean verbose;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final Thread main = Thread.currentThread();

    public ReplRunner(String prompt, boolean verbose, BiFunction<OutputStream, InputStream, Runnable> mkRepl) throws IOException {
        this.prompt = prompt;
        this.verbose = verbose;
        PipedInputStream testInput = new PipedInputStream();
        PipedOutputStream testOutput = new PipedOutputStream();

        this.repl = mkRepl.apply(teeWithSysOut(new PipedOutputStream(testInput)), new PipedInputStream(testOutput));
        this.testInput = testInput;
        this.testOutput = teeWithSysOut(testOutput);
    }

    public ReplRunner start() throws IOException {
        executorService.submit(() -> {
            try {
                repl.run();
            } catch (Throwable e) {
                e.printStackTrace();
                main.interrupt();
                throw new RuntimeException(e);
            }
        });
        consumeOutput();
        return this;
    }

    public ReplRunner enter(String command, String expectedHead, String... expectedRest) throws IOException {
        write(command);
        List<String> elements = new LinkedList<>(Arrays.asList(expectedRest));
        elements.add(0, expectedHead);

        assertEquals(join(Constants.newLine, elements), consumeOutput());

        return this;
    }

    public ReplRunner enter(String command) throws IOException {
        write(command);
        consumeOutput();
        return this;
    }

    private ReplRunner write(String command) throws IOException {
        BufferedWriter buffered = new BufferedWriter(new OutputStreamWriter(testOutput));
        buffered.write(command + Constants.newLine);
        buffered.flush();
        return this;
    }

    private String consumeOutput() throws IOException {
        Reader reader = new InputStreamReader(testInput);
        char[] in = new char[1024];

        int length = prompt.length() - 1;

        for (int i = 0; i < length; i++) {
            in[i] = (char) reader.read();
        }

        int j;
        //wait for prompt
        for (j = length; ; j++) {
            in[j] = (char) reader.read();
            if (prompt.equals(new String(in, j - length, length + 1)))
                break;
        }
        return new String(in, 0, j - length).trim();
    }

    @Override
    public void close() throws IOException {
        write(null); //Ctrl^D

        try {
            executorService.awaitTermination(1, TimeUnit.SECONDS);
            executorService.shutdownNow();
            testInput.close();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private OutputStream teeWithSysOut(OutputStream main) {
        return new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                main.write(b);
                if (verbose)
                    System.out.write(b);
            }
        };
    }
}
