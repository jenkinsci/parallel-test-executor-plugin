package org.jenkinsci.plugins.parallel_test_executor;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.FilePath;
import hudson.Util;
import hudson.console.ModelHyperlinkNote;
import hudson.model.AbstractBuild;
import hudson.model.InvisibleAction;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import jenkins.MasterToSlaveFileCallable;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Copy;

/**
 * Runs at the end of a triggered test sub-task and collects test reports back to the master.
 *
 * @author Kohsuke Kawaguchi
 */
class TestCollector extends InvisibleAction implements Serializable {
    private static final long serialVersionUID = -592264249944063364L;

    // none of this is meant to persist
    private transient AbstractBuild<?,?> collector;
    private transient ParallelTestExecutor testExecutor;
    @SuppressFBWarnings(value="SE_TRANSIENT_FIELD_NOT_RESTORED", justification="not needed after initial use")
    private transient int ordinal;

    public TestCollector(AbstractBuild<?, ?> collector, ParallelTestExecutor testExecutor, int ordinal) {
        this.testExecutor = testExecutor;
        assert collector!=null;
        this.collector = collector;
        this.ordinal = ordinal;
    }

    /**
     * Collects the test reports from the sub build to the master.
     */
    public void collect(AbstractBuild<?,?> build, TaskListener listener) {
        if (collector==null)    return; // must be deserialized. pretend as if this action doesn't exist.

        try {
            listener.getLogger().println("Collecting test reports for the master build: "+ ModelHyperlinkNote.encodeTo(collector));

            final FilePath src = build.getWorkspace();
            if (src==null)      return; // trying to be defensive in case of catastrophic build failure
            FilePath workspace = collector.getWorkspace();
            if (workspace == null) {
                return; // ditto
            }
            final FilePath dst = workspace.child("test-splits/reports/"+ordinal);
            dst.mkdirs();

            final String includes = testExecutor.getTestReportFiles();

            if (src.getChannel()==dst.getChannel()) {
                // fast case where a direct copy is possible
                // TODO: move this to the core. copyRecursiveTo + 'archive' semantics
                src.act(new MasterToSlaveFileCallable<Integer>() {
                    private static final long serialVersionUID = 1L;
                    public Integer invoke(File base, VirtualChannel channel) throws IOException {
                        if(!base.exists())  return 0;
                        assert dst.getChannel()==null;

                        try {
                            class CopyImpl extends Copy {
                                private int copySize;

                                public CopyImpl() {
                                    setProject(new org.apache.tools.ant.Project());
                                }

                                @Override
                                protected void doFileOperations() {
                                    copySize = super.fileCopyMap.size();
                                    super.doFileOperations();
                                }

                                public int getNumCopied() {
                                    return copySize;
                                }
                            }

                            CopyImpl copyTask = new CopyImpl();
                            copyTask.setTodir(new File(dst.getRemote()));
                            copyTask.addFileset(Util.createFileSet(base,includes));
                            copyTask.setOverwrite(true);
                            copyTask.setIncludeEmptyDirs(false);
                            copyTask.setPreserveLastModified(true); // this is the only change from stock 'copyRecursiveTo'

                            copyTask.execute();
                            return copyTask.getNumCopied();
                        } catch (BuildException e) {
                            throw new IOException("Failed to copy " + base + "/" + includes + " to " + dst, e);
                        }
                    }
                });
            } else
            if (src.getChannel()==null || dst.getChannel()==null) {
                // this uses tar, so the timestamp gets preserved
                src.copyRecursiveTo(includes, dst);
            } else {
                // copy via master
                File t = Util.createTempDir();
                FilePath tmp = new FilePath(t);
                try {
                    src.copyRecursiveTo(testExecutor.getTestReportFiles(), tmp);
                    tmp.copyRecursiveTo(dst);
                } finally {
                    Util.deleteRecursive(t);
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace(listener.error("Failed to aggregate test reports for "+collector.getFullDisplayName()));
        }
    }
}
