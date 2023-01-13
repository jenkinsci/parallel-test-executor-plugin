package org.jenkinsci.plugins.parallel_test_executor;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;

/**
 * Returns the latest build from the target branch if the current build is a pull request.
 */
@Extension(optional = true)
public class TargetBranchPreviousBuildFinder implements PreviousBuildFinder {
    private static final Logger LOGGER = Logger.getLogger(TargetBranchPreviousBuildFinder.class.getName());

    @Override
    public Run<?, ?> find(Run<?, ?> b, TaskListener listener) {
        try {
            EnvVars environment = b.getEnvironment(listener);
            String changeTarget = environment.get("CHANGE_TARGET");
            if (changeTarget != null) {
                Job<?, ?> project = b.getParent();
                ItemGroup itemGroup = project.getParent();
                if (itemGroup instanceof WorkflowMultiBranchProject) {
                    WorkflowMultiBranchProject multiBranchProject = (WorkflowMultiBranchProject) itemGroup;
                    WorkflowJob targetBranch = multiBranchProject.getItemByBranchName(changeTarget);
                    if (targetBranch != null) {
                        return targetBranch.getLastBuild();
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.log(Level.FINE, e, () -> "Could not look up environment variables for " + b);
        }
        return null;
    }
}
