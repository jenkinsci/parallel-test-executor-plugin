#!/bin/bash
mkdir -p hello/src/test/java/foo
mkdir -p goodbye/src/test/java/foo
for i in {00..99}; do
cat > hello/src/test/java/foo/Hello${i}Test.java << EOF
package foo;
import org.junit.Test;
import static org.junit.Assert.*;

public class Hello${i}Test {
    private static final int MULTIPLIER;
    static {
        String multiplier = System.getenv("MULTIPLIER");
        MULTIPLIER = multiplier != null ? Integer.parseInt(multiplier) : 1;
    }
    @Test public void one() {
        if (Math.random() < 0.015) {
            fail("oops");
        }
    }
    @Test public void two() {}
    @Test public void three() throws Exception {
        Thread.sleep(${i##0}0 * MULTIPLIER);
    }
    @Test public void four() throws Exception {
        Thread.sleep(1000 * MULTIPLIER);
    }
}
EOF
cat > goodbye/src/test/java/foo/Goodbye${i}Test.java << EOF
package foo;
import org.junit.Test;
import static org.junit.Assert.*;

public class Goodbye${i}Test {
    private static final int MULTIPLIER;
    static {
        String multiplier = System.getenv("MULTIPLIER");
        MULTIPLIER = multiplier != null ? Integer.parseInt(multiplier) : 1;
    }
    @Test public void one() {
        if (Math.random() < 0.015) {
            fail("oops");
        }
    }
    @Test public void two() {}
    @Test public void three() throws Exception {
        Thread.sleep(${i##0}0 * MULTIPLIER);
    }
    @Test public void four() throws Exception {
        Thread.sleep(1000 * MULTIPLIER);
    }
}
EOF
done
