package org.jenkinsci.plugins.parallel_test_executor;

import hudson.Extension;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.branch.Branch;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.mixin.ChangeRequestSCMHead;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;

/**
 * Returns the latest build from the target branch if the current build is a pull request.
 */
@Extension(optional = true)
public class TargetBranchPreviousBuildFinder implements PreviousBuildFinder {
    @Override
    public Run<?, ?> find(Run<?, ?> b, TaskListener listener) {
        Job<?, ?> project = b.getParent();
        ItemGroup itemGroup = project.getParent();
        if (itemGroup instanceof WorkflowMultiBranchProject) {
            WorkflowMultiBranchProject multiBranchProject = (WorkflowMultiBranchProject) itemGroup;
            Branch branch = multiBranchProject.getProjectFactory().getBranch((WorkflowJob) project);
            SCMHead head = branch.getHead();
            if (head instanceof ChangeRequestSCMHead) {
                SCMHead target = ((ChangeRequestSCMHead) head).getTarget();
                String targetName = target.getName();
                WorkflowJob targetBranch = multiBranchProject.getItemByBranchName(targetName);
                if (targetBranch != null) {
                    return targetBranch.getLastBuild();
                }
            }
        }
        return null;
    }
}
