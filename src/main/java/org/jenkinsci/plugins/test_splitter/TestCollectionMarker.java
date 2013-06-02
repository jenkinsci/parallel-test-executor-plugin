package org.jenkinsci.plugins.test_splitter;

import hudson.FilePath;
import hudson.Util;
import hudson.console.ModelHyperlinkNote;
import hudson.model.AbstractBuild;
import hudson.model.InvisibleAction;
import hudson.model.TaskListener;

import java.io.File;
import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public class TestCollectionMarker extends InvisibleAction {
    // none of this is meant to persist
    private transient AbstractBuild<?,?> collector;
    private transient TestSplitter splitter;
    private transient int ordinal;

    public TestCollectionMarker(AbstractBuild<?, ?> collector, TestSplitter splitter, int ordinal) {
        this.splitter = splitter;
        assert collector!=null;
        this.collector = collector;
        this.ordinal = ordinal;
    }

    public void collect(AbstractBuild<?,?> build, TaskListener listener) {
        if (collector==null)    return; // must be deserialized. pretend as if this action doesn't exist.

        try {
            listener.getLogger().println("Collecting test reports for the master build: "+ ModelHyperlinkNote.encodeTo(collector));

            FilePath src = build.getWorkspace();
            FilePath dst = collector.getWorkspace().child("test-splits/reports/"+ordinal);
            dst.mkdirs();

            if (src.getChannel()==dst.getChannel() || src.getChannel()==null || dst.getChannel()==null) {
                // fast case where a direct copy is possible
                src.copyRecursiveTo(splitter.getTestReportFiles(), dst);
            } else {
                // copy via master
                File t = Util.createTempDir();
                FilePath tmp = new FilePath(t);
                try {
                    src.copyRecursiveTo(splitter.getTestReportFiles(), tmp);
                    tmp.copyRecursiveTo(dst);
                } finally {
                    Util.deleteRecursive(t);
                }
            }
        } catch (IOException e) {
            e.printStackTrace(listener.error("Failed to aggregate test reports for "+collector.getFullDisplayName()));
        } catch (InterruptedException e) {
            e.printStackTrace(listener.error("Failed to aggregate test reports for "+collector.getFullDisplayName()));
        }
    }
}
