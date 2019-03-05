package org.apache.commons.release.plugin.mojos;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.release.plugin.SharedFunctions;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.checkin.CheckInScmResult;
import org.apache.maven.scm.command.checkout.CheckOutScmResult;
import org.apache.maven.scm.command.remove.RemoveScmResult;
import org.apache.maven.scm.manager.BasicScmManager;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.provider.svn.repository.SvnScmProviderRepository;
import org.apache.maven.scm.provider.svn.svnexe.SvnExeScmProvider;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.SettingsDecrypter;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * This class checks out the dev distribution location, checkes whether anything exists in the
 * distribution location, and if it is non-empty it deletes all of the resources there.
 *
 * @author chtompki
 * @since 1.6
 */
@Mojo(name = "clean-staging",
        defaultPhase = LifecyclePhase.COMPILE,
        threadSafe = true,
        aggregator = true)
public class CommonsStagingCleanupMojo extends AbstractMojo {

    /**
     * The {@link MavenProject} object is essentially the context of the maven build at
     * a given time.
     */
    @Parameter(defaultValue = "${project}", required = true)
    private MavenProject project;

    /**
     * The main working directory for the plugin, namely <code>target/commons-release-plugin</code>, but
     * that assumes that we're using the default maven <code>${project.build.directory}</code>.
     */
    @Parameter(defaultValue = "${project.build.directory}/commons-release-plugin", property = "commons.outputDirectory")
    private File workingDirectory;

    /**
     * The location to which to checkout the dist subversion repository under our working directory, which
     * was given above. We then do an SVN delete on all of the directories in this repository.
     */
    @Parameter(defaultValue = "${project.build.directory}/commons-release-plugin/scm-cleanup",
            property = "commons.distCleanupDirectory")
    private File distCleanupDirectory;

    /**
     * A boolean that determines whether or not we actually commit the files up to the subversion repository.
     * If this is set to <code>true</code>, we do all but make the commits. We do checkout the repository in question
     * though.
     */
    @Parameter(property = "commons.release.dryRun", defaultValue = "false")
    private Boolean dryRun;

    /**
     * The url of the subversion repository to which we wish the artifacts to be staged. Typically this would need to
     * be of the form: <code>scm:svn:https://dist.apache.org/repos/dist/dev/commons/foo/version-RC#</code>. Note. that
     * the prefix to the substring <code>https</code> is a requirement.
     */
    @Parameter(defaultValue = "", property = "commons.distSvnStagingUrl")
    private String distSvnStagingUrl;

    /**
     * A parameter to generally avoid running unless it is specifically turned on by the consuming module.
     */
    @Parameter(defaultValue = "false", property = "commons.release.isDistModule")
    private Boolean isDistModule;

    /**
     * The ID of the server (specified in settings.xml) which should be used for dist authentication.
     * This will be used in preference to {@link #username}/{@link #password}.
     */
    @Parameter(property = "commons.distServer")
    private String distServer;

    /**
     * The username for the distribution subversion repository. This is typically your Apache id.
     */
    @Parameter(property = "user.name")
    private String username;

    /**
     * The password associated with {@link CommonsDistributionStagingMojo#username}.
     */
    @Parameter(property = "user.password")
    private String password;

    /**
     * Maven {@link Settings}.
     */
    @Parameter(defaultValue = "${settings}", readonly = true, required = true)
    private Settings settings;

    /**
     * Maven {@link SettingsDecrypter} component.
     */
    @Component
    private SettingsDecrypter settingsDecrypter;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!isDistModule) {
            getLog().info("This module is marked as a non distribution "
                    + "or assembly module, and the plugin will not run.");
            return;
        }
        if (StringUtils.isEmpty(distSvnStagingUrl)) {
            getLog().warn("commons.distSvnStagingUrl is not set, the commons-release-plugin will not run.");
            return;
        }
        if (!workingDirectory.exists()) {
            SharedFunctions.initDirectory(getLog(), workingDirectory);
        }
        try {
            ScmManager scmManager = new BasicScmManager();
            scmManager.setScmProvider("svn", new SvnExeScmProvider());
            ScmRepository repository = scmManager.makeScmRepository(distSvnStagingUrl);
            ScmProvider provider = scmManager.getProviderByRepository(repository);
            SvnScmProviderRepository providerRepository = (SvnScmProviderRepository) repository.getProviderRepository();
            SharedFunctions.setAuthentication(
                    providerRepository,
                    distServer,
                    settings,
                    settingsDecrypter,
                    username,
                    password
            );
            getLog().info("Checking out dist from: " + distSvnStagingUrl);
            ScmFileSet scmFileSet = new ScmFileSet(distCleanupDirectory);
            final CheckOutScmResult checkOutResult = provider.checkOut(repository, scmFileSet);
            if (!checkOutResult.isSuccess()) {
                throw new MojoExecutionException("Failed to checkout files from SCM: "
                        + checkOutResult.getProviderMessage() + " [" + checkOutResult.getCommandOutput() + "]");
            }
            List<File> filesToRemove = Arrays.asList(distCleanupDirectory.listFiles());
            ScmFileSet fileSet = new ScmFileSet(distCleanupDirectory, filesToRemove);
            RemoveScmResult removeScmResult = provider.remove(repository, fileSet, "Cleaning up staging area");
            if (!removeScmResult.isSuccess()) {
                throw new MojoFailureException("Failed to add files to SCM: " + removeScmResult.getProviderMessage()
                        + " [" + removeScmResult.getCommandOutput() + "]");
            }
            getLog().info("Cleaning distribution area for: " + project.getArtifactId());
            CheckInScmResult checkInResult = provider.checkIn(
                    repository,
                    fileSet,
                    "Cleaning distribution area for: " + project.getArtifactId()
            );
        } catch (ScmException e) {
            throw new MojoFailureException(e.getMessage());
        }

    }
}