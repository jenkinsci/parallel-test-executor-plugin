package org.jenkinsci.plugins.test_splitter;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.AggregatedTestResultAction;
import hudson.tasks.test.TestResult;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.Stapler;

/**
 * {@link AggregatedTestResultAction} that collects test reports from triggered builds.
 *
 * @author Kohsuke Kawaguchi
 */
public class AggregatedTestResultActionImpl extends AggregatedTestResultAction {
    public AggregatedTestResultActionImpl(AbstractBuild owner) {
        super(owner);
    }

    @Override
    protected String getChildName(AbstractTestResultAction tr) {
        return tr.owner.getProject().getFullName();
    }

    @Override
    public AbstractBuild<?, ?> resolveChild(Child child) {
        return Jenkins.getInstance().getItemByFullName(child.name, AbstractProject.class).getBuildByNumber(child.build);
    }

    @Override
    protected void add(AbstractTestResultAction child) {
        super.add(child);
    }

    @Override
    public AbstractTestResultAction getChildReport(Child child) {
        return super.getChildReport(child);
    }

    @Override
    public String getTestResultPath(TestResult it) {
        return  Stapler.getCurrentRequest().getContextPath()+"/"+it.getOwner().getUrl()+getUrlName()/*TODO: is this correct?*/+ "/" + it.getRelativePathFrom(null);
    }
}
