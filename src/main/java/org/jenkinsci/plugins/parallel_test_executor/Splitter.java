/*
 * The MIT License
 *
 * Copyright 2024 CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkinsci.plugins.parallel_test_executor;

import com.google.common.base.Predicate;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.FilePath;
import hudson.console.ModelHyperlinkNote;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.junit.ClassResult;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.TabulatedResult;
import hudson.tasks.test.TestResult;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.mixin.ChangeRequestSCMHead;
import static org.jenkinsci.plugins.parallel_test_executor.ParallelTestExecutor.NUMBER_OF_BUILDS_TO_SEARCH;
import static org.jenkinsci.plugins.parallel_test_executor.ParallelTestExecutor.RESULTS_OF_BUILDS_TO_CONSIDER;
import org.jenkinsci.plugins.parallel_test_executor.testmode.TestMode;
import org.jenkinsci.plugins.workflow.actions.LabelAction;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.graphanalysis.DepthFirstScanner;

class Splitter {

    private static final Logger LOGGER = Logger.getLogger(Splitter.class.getName());

    static List<InclusionExclusionPattern> findTestSplits(Parallelism parallelism, @CheckForNull TestMode inputTestMode, Run<?,?> build, TaskListener listener,
                                                          boolean generateInclusions,
                                                          @CheckForNull final String stageName, @CheckForNull FilePath workspace) throws InterruptedException {
        TestMode testMode = inputTestMode == null ? TestMode.getDefault() : inputTestMode;
        TestResult tr = findPreviousTestResult(build, listener);
        Map<String/*fully qualified class name*/, TestEntity> data = new TreeMap<>();
        if (tr != null) {
            Run<?,?> prevRun = tr.getRun();
            if (prevRun instanceof FlowExecutionOwner.Executable && stageName != null) {
                FlowExecutionOwner owner = ((FlowExecutionOwner.Executable)prevRun).asFlowExecutionOwner();
                if (owner != null) {
                    FlowExecution execution = owner.getOrNull();
                    if (execution != null) {
                        DepthFirstScanner scanner = new DepthFirstScanner();
                        FlowNode stageId = scanner.findFirstMatch(execution, new StageNamePredicate(stageName));
                        if (stageId != null) {
                            listener.getLogger().println("Found stage \"" + stageName + "\" in " + prevRun.getFullDisplayName());
                            tr = ((hudson.tasks.junit.TestResult) tr).getResultForPipelineBlock(stageId.getId());
                        } else {
                            listener.getLogger().println("No stage \"" + stageName + "\" found in " + prevRun.getFullDisplayName());
                        }
                    }
                }
            }
            collect(tr, data, testMode);
        } else {
            listener.getLogger().println("No record available, try to find test classes");
            data = testMode.estimate(workspace, listener);
            if(data.isEmpty()) {
                listener.getLogger().println("No test classes was found, so executing everything in one place");
                return Collections.singletonList(new InclusionExclusionPattern(Collections.<String>emptyList(), false));
            }
        }

        // sort in the descending order of the duration
        List<TestEntity> sorted = new ArrayList<>(data.values());
        Collections.sort(sorted);

        // degree of the parallelism. we need minimum 1
        final int n = Math.max(1, parallelism.calculate(sorted));

        List<ParallelTestExecutor.Knapsack> knapsacks = new ArrayList<>(n);
        for (int i = 0; i < n; i++)
            knapsacks.add(new ParallelTestExecutor.Knapsack());

        /*
            This packing problem is a NP-complete problem, so we solve
            this simply by a greedy algorithm. We pack heavier items first,
            and the result should be of roughly equal size
         */
        PriorityQueue<ParallelTestExecutor.Knapsack> q = new PriorityQueue<>(knapsacks);
        for (var testEntity : sorted) {
            ParallelTestExecutor.Knapsack k = q.poll();
            k.add(testEntity);
            q.add(k);
        }

        long total = 0, min = Long.MAX_VALUE, max = Long.MIN_VALUE;
        for (ParallelTestExecutor.Knapsack k : knapsacks) {
            total += k.total;
            max = Math.max(max, k.total);
            min = Math.min(min, k.total);
        }
        long average = total / n;
        long variance = 0;
        for (ParallelTestExecutor.Knapsack k : knapsacks) {
            variance += pow(k.total - average);
        }
        variance /= n;
        long stddev = (long) Math.sqrt(variance);
        listener.getLogger().printf("%d test %s (%dms) divided into %d sets. Min=%dms, Average=%dms, Max=%dms, stddev=%dms%n",
                data.size(), testMode.getWord(), total, n, min, average, max, stddev);

        List<InclusionExclusionPattern> r = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            ParallelTestExecutor.Knapsack k = knapsacks.get(i);
            boolean shouldIncludeElements = generateInclusions && i != 0;
            List<String> elements = sorted.stream().filter(testEntity -> shouldIncludeElements == (testEntity.knapsack == k))
                    .flatMap(testEntity -> testEntity.getElements().stream())
                    .collect(Collectors.toList());
            r.add(new InclusionExclusionPattern(elements, shouldIncludeElements));
        }
        return r;
    }

    private static long pow(long l) {
        return l * l;
    }

    /**
     * Visits the structure inside {@link hudson.tasks.test.TestResult}.
     */
    private static void collect(TestResult r, Map<String, TestEntity> data, TestMode testMode) {
        var queue = new ArrayDeque<TestResult>();
        queue.push(r);
        while (!queue.isEmpty()) {
            var current = queue.pop();
            if (current instanceof ClassResult) {
                var classResult = (ClassResult) current;
                LOGGER.log(Level.FINE, () -> "Retrieving test entities from " + classResult.getFullName());
                data.putAll(testMode.getTestEntitiesMap(classResult));
            } else if (current instanceof TabulatedResult) {
                LOGGER.log(Level.FINE, () -> "Considering children of " + current.getFullName());
                queue.addAll(((TabulatedResult) current).getChildren());
            } else {
                LOGGER.log(Level.FINE, () -> "Ignoring " + current.getFullName());
            }
        }
    }

    private static TestResult findPreviousTestResult(Run<?, ?> b, TaskListener listener) {
        Job<?, ?> project = b.getParent();
        // Look for test results starting with the previous build
        TestResult result = getTestResult(project, b.getPreviousBuild(), listener);
        if (result == null) {
            // Look for test results from the target branch builds if this is a change request.
            SCMHead head = SCMHead.HeadByItem.findHead(project);
            if (head instanceof ChangeRequestSCMHead) {
                SCMHead target = ((ChangeRequestSCMHead) head).getTarget();
                Item targetBranch = project.getParent().getItem(target.getName());
                if (targetBranch != null && targetBranch instanceof Job) {
                    result = getTestResult(project, ((Job<?, ?>) targetBranch).getLastBuild(), listener);
                }
            }
        }
        return result;
    }


    static TestResult getTestResult(Job<?, ?> originProject, Run<?, ?> b, TaskListener listener) {
        TestResult result = null;
        for (int i = 0; i < NUMBER_OF_BUILDS_TO_SEARCH; i++) {// limit the search to a small number to avoid loading too much
            if (b == null) break;
            if (RESULTS_OF_BUILDS_TO_CONSIDER.contains(b.getResult()) && !b.isBuilding()) {
                String hyperlink = ModelHyperlinkNote.encodeTo('/' + b.getUrl(), originProject != b.getParent() ? b.getFullDisplayName() : b.getDisplayName());
                try {
                    AbstractTestResultAction<?> tra = b.getAction(AbstractTestResultAction.class);
                    if (tra != null) {
                        Object o = tra.getResult();
                        if (o instanceof TestResult) {
                            TestResult tr = (TestResult) o;
                            if (tr.getTotalCount() == 0) {
                                listener.getLogger().printf("Build %s has no loadable test results (supposed count %d), skipping%n", hyperlink, tra.getTotalCount());
                            } else {
                                listener.getLogger().printf("Using build %s as reference%n", hyperlink);
                                result = tr;
                                break;
                            }
                        }
                    }
                } catch (RuntimeException e) {
                    e.printStackTrace(listener.error("Failed to load (corrupt?) build %s, skipping%n", hyperlink));
                }
            }
            b = b.getPreviousBuild();
        }
        return result;
    }

    private static class StageNamePredicate implements Predicate<FlowNode> {
        private final String stageName;
        StageNamePredicate(@NonNull String stageName) {
            this.stageName = stageName;
        }
        @Override
        public boolean apply(@Nullable FlowNode input) {
            if (input != null) {
                LabelAction labelAction = input.getPersistentAction(LabelAction.class);
                return labelAction != null && stageName.equals(labelAction.getDisplayName());
            }
            return false;
        }
    }

    private Splitter() {}

}
