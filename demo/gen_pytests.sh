#!/bin/bash
mkdir -p src/test/foo
cat > src/test/foo/test_Hello.py << EOF
import pytest
import time
import random

class TestHello:
EOF

for i in {00..99}; do
cat >> src/test/foo/test_Hello.py << EOF

    def test_${i}(self):
        x = random.random()
        time.sleep(x)
        if x < 0.015:
            pytest.fail("oops")
EOF
done

