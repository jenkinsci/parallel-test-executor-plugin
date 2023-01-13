package org.jenkinsci.plugins.parallel_test_executor;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.mixin.ChangeRequestSCMHead;

/**
 * Returns the latest build from the target branch if the current build comes from a change request.
 */
@Extension(optional = true)
public class TargetBranchPreviousBuildFinder implements PreviousBuildFinder {
    @Override
    public Run<?, ?> find(Run<?, ?> b, TaskListener listener) {
        Job<?, ?> project = b.getParent();
        SCMHead head = SCMHead.HeadByItem.findHead(project);
        if (head instanceof ChangeRequestSCMHead) {
            SCMHead target = ((ChangeRequestSCMHead) head).getTarget();
            Item targetBranch = project.getParent().getItem(target.getName());
            if (targetBranch != null && targetBranch instanceof Job) {
                return ((Job<?, ?>) targetBranch).getLastBuild();
            }
        }
        return null;
    }
}
