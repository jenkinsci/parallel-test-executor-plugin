package org.jenkinsci.plugins.parallel_test_executor;

import hudson.Extension;
import jenkins.branch.BranchBuildStrategy;
import jenkins.branch.BranchBuildStrategyDescriptor;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMSource;

public class DontBuildBranchBuildStrategy extends BranchBuildStrategy {

  @Override
  public boolean isAutomaticBuild(SCMSource source, SCMHead head) {
    // never ever build automatically
    return false;
  }

  @Extension
  public static class DescriptorImpl extends BranchBuildStrategyDescriptor { }
}
