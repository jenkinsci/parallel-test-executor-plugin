#!/bin/bash
mkdir -p src/test/java/foo
for i in {00..99}; do
cat > src/test/java/foo/Hello${i}Test.java << EOF
package foo;
import org.junit.Test;
import static org.junit.Assert.*;

public class Hello${i}Test {
    @Test public void one() {
        if (Math.random() < 0.015) {
            fail("oops");
        }
    }
    @Test public void two() {}
    @Test public void three() throws Exception {
        Thread.sleep(${i##0}0);
    }
}
EOF
done
