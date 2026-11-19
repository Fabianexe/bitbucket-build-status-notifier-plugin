package org.jenkinsci.plugins.bitbucket;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.Revision;
import hudson.plugins.git.util.Build;
import hudson.plugins.git.util.BuildData;
import java.util.List;
import org.eclipse.jgit.lib.ObjectId;
import org.jenkinsci.plugins.bitbucket.model.BitbucketBuildStatusResource;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.scribe.model.Verb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BitbucketBuildStatusHelperTest {
    private static final String COMMIT = "a83c709e9d514421ef614ef0a1117366c84c6304";

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void createsCloudApiResourcesForSupportedGitRemotes() throws Exception {
        String[] remotes = {
            "git@ssh.bitbucket.org:workspace/repository.git",
            "ssh://git@ssh.bitbucket.org/workspace/repository.git",
            "ssh://git@ssh.bitbucket.org:22/workspace/repository.git",
            "git@bitbucket.org:workspace/repository.git",
            "ssh://git@bitbucket.org/workspace/repository.git",
            "ssh://git@altssh.bitbucket.org:443/workspace/repository.git",
            "https://bitbucket.org/workspace/repository.git"
        };

        for (String remote : remotes) {
            List<BitbucketBuildStatusResource> resources =
                    BitbucketBuildStatusHelper.createBuildStatusResources(buildWithRemote(remote));

            assertEquals(remote, 1, resources.size());
            BitbucketBuildStatusResource resource = resources.get(0);
            assertEquals(remote, "workspace", resource.getOwner());
            assertEquals(remote, "repository", resource.getRepoSlug());
            assertEquals(remote, COMMIT, resource.getCommitId());
            assertEquals(remote, "https://api.bitbucket.org/2.0/repositories/workspace/repository/commit/"
                    + COMMIT + "/statuses/build", resource.generateUrl(Verb.POST));
        }
    }

    @Test
    public void skipsRepositoriesOutsideBitbucketCloud() throws Exception {
        String[] remotes = {
            "git@github.com:workspace/repository.git",
            "git@ssh.bitbucket.org.example.com:workspace/repository.git"
        };

        for (String remote : remotes) {
            assertTrue(remote, BitbucketBuildStatusHelper
                    .createBuildStatusResources(buildWithRemote(remote)).isEmpty());
        }
    }

    private FreeStyleBuild buildWithRemote(String remote) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.setScm(new GitSCM(remote));
        FreeStyleBuild build = project.createExecutable();
        BuildData buildData = new BuildData();
        buildData.saveBuild(new Build(new Revision(ObjectId.fromString(COMMIT)), build.getNumber(), Result.SUCCESS));
        build.addAction(buildData);
        return build;
    }
}
