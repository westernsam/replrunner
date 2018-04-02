# Repl Runner
Fixture to test repl style application in java, using just OutputStream and InputStream to drive the application.

## Echo Example

```
import java.io.*;

public class Echo implements Runnable {

    private final BufferedWriter out;
    private final BufferedReader in;

    public Echo(OutputStream out, InputStream in) {
        this.out = new BufferedWriter(new OutputStreamWriter(out));
        this.in = new BufferedReader(new InputStreamReader(in));
    }

    @Override
    public void run() {
        try {
            runSafe();
        } catch (IOException ie) {
            throw new RuntimeException(ie);
        }
    }

    private void runSafe() throws IOException {
        for (; ; ) {
            out.write("> "); out.flush();
            String line = in.readLine();

            if (line == null || line.equals("Q")) {
                break;
            }

            if (line.equals("")) {
                continue;
            }

            out.write(line);
            out.write("\n");
            out.flush();
        }
    }

    public static void main(String[] args) {
        new Echo(System.out, System.in).run();
    }
}
```

Can be tested like this:

```java
import github.westernsam.repltest.Echo;
import github.westernsam.repltest.ReplRunner;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

public class EchoTest {

    private static ReplRunner replRunner;

    @BeforeClass
    public static void setReplRunner() throws IOException {
        replRunner = new ReplRunner(">", true, Echo::new); //true 
        replRunner.start();
    }

    @Test
    public void testEcho() throws IOException {
        replRunner.enter("Hello",
                "Hello"
        );
    }

    @Test
    public void testEmptyString() throws IOException {
        replRunner.enter("",
                ""
        );
    }
}
```
