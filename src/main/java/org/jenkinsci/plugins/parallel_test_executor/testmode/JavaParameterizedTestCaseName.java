/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jenkinsci.plugins.parallel_test_executor.testmode;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Descriptor;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * This mode works best with java projects.
 * <p>
 * Parallelize per java test case including parameters if present.
 * </p>
 * <p>
 * It is also able to estimate tests to run from the workspace content if no historical context could be found.
 * </p>
 */
public class JavaParameterizedTestCaseName extends JavaClassName {
  @DataBoundConstructor
  public JavaParameterizedTestCaseName() {
  }

  @Override 
  public boolean isSplitByCase() {
    return true;
  }

  @Override 
  public boolean useParameters() {
    return true;
  }

  @Extension
  @Symbol("javaParamTestCase")
  public static class DescriptorImpl extends Descriptor<TestMode> {
    @Override
    @NonNull
    public String getDisplayName() {
      return "By Java test cases with parameters";
    }
  }
}