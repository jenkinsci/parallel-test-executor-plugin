package org.jenkinsci.plugins.parallel_test_executor;

import com.google.common.collect.Lists;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.FileParameterValue;
import hudson.model.ParametersAction;
import hudson.model.TaskListener;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameterFactory;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameterFactoryDescriptor;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameters;
import hudson.plugins.parameterizedtrigger.FileBuildParameterFactory;
import hudson.util.IOException2;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Logger;

/**
 * Essentially a copy-paste of {@link hudson.plugins.parameterizedtrigger.BinaryFileParameterFactory} that takes a
 * list of mappings "name -> filePattern" to generate parameters.
 *
 * @author Vincent Latombe <vincent@latombe.net>
 */
public class MultipleBinaryFileParameterFactory extends AbstractBuildParameterFactory {
    public static class Tuple {
        public Tuple(String parameterName, String filePattern) {
            this.parameterName = parameterName;
            this.filePattern = filePattern;
        }
        public final String parameterName;
        public final String filePattern;
    }

    private final List<Tuple> parametersList;
    private final FileBuildParameterFactory.NoFilesFoundEnum noFilesFoundAction;

    @DataBoundConstructor
    public MultipleBinaryFileParameterFactory(List<Tuple> parametersList, FileBuildParameterFactory.NoFilesFoundEnum noFilesFoundAction) {
        this.parametersList = parametersList;
        this.noFilesFoundAction = noFilesFoundAction;
    }

    public MultipleBinaryFileParameterFactory(List<Tuple> parametersList) {
        this(parametersList, FileBuildParameterFactory.NoFilesFoundEnum.SKIP);
    }

    public FileBuildParameterFactory.NoFilesFoundEnum getNoFilesFoundAction() {
        return noFilesFoundAction;
    }

    @Override
    public List<AbstractBuildParameters> getParameters(AbstractBuild<?, ?> build, TaskListener listener) throws IOException, InterruptedException, AbstractBuildParameters.DontTriggerException {
        List<AbstractBuildParameters> result = Lists.newArrayList();
        int totalFiles = 0;
        for (final Tuple t : parametersList) {
            // save them into the master because FileParameterValue might need files after the slave workspace have disappeared/reused
            FilePath target = new FilePath(build.getRootDir()).child("parameter-files");
                int k = build.getWorkspace().copyRecursiveTo(t.filePattern, target);
                totalFiles += k;
                if (k > 0) {
                    for (final FilePath f : target.list(t.filePattern)) {
                        LOGGER.fine("Triggering build with " + f.getName());

                        result.add(new AbstractBuildParameters() {
                            @Override
                            public Action getAction(AbstractBuild<?, ?> build, TaskListener listener) throws IOException, InterruptedException, DontTriggerException {
                                assert f.getChannel() == null;    // we copied files locally. This file must be local to the master
                                FileParameterValue fv = new FileParameterValue(t.parameterName, new File(f.getRemote()), f.getName());
                                if ($setLocation != null) {
                                    try {
                                        $setLocation.invoke(fv, t.parameterName);
                                    } catch (IllegalAccessException e) {
                                        // be defensive as the core might change
                                    } catch (InvocationTargetException e) {
                                        // be defensive as the core might change
                                    }
                                }
                                return new ParametersAction(fv);
                            }
                        });
                    }
                }
        }
        if (totalFiles ==0) {
            noFilesFoundAction.failCheck(listener);
        }

        return result;
    }


    @Extension
    public static class DescriptorImpl extends AbstractBuildParameterFactoryDescriptor {
        @Override
        public String getDisplayName() {
            return "Multiple Binary Files (not meant to be used)";
        }
    }

    private static Method $setLocation;

    static {
        // work around NPE fixed in the core at 4a95cc6f9269108e607077dc9fd57f06e4c9af26
        try {
            $setLocation = FileParameterValue.class.getDeclaredMethod("setLocation",String.class);
            $setLocation.setAccessible(true);
        } catch (NoSuchMethodException e) {
            // ignore
        }
    }
    private static final Logger LOGGER = Logger.getLogger(MultipleBinaryFileParameterFactory.class.getName());
}
