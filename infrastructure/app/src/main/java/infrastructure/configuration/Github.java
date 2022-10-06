package infrastructure.configuration;

import com.pulumi.Context;
import com.pulumi.github.*;
import com.pulumi.github.inputs.BranchProtectionRequiredPullRequestReviewArgs;
import com.pulumi.github.inputs.BranchProtectionRequiredStatusCheckArgs;
import com.pulumi.github.inputs.GetOrganizationArgs;
import com.pulumi.resources.CustomResource;
import com.pulumi.resources.CustomResourceOptions;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class Github {

    private static final String ORGANIZATION_NAME = "dzik-darek-org";

    private static final String PROJECT_NAME = "infrastructure-example";

    private static final String REPOSITORY_NAME = "infrastructure-repository";

    private static final String BRANCH_DEFAULT_NAME = "main";

    private static final String BRANCH_DEFAULT_PROTECTION_NAME = "master-branch-protection";

    private static final String LABEL_CRITICAL_NAME = "Critical";

    private static final List REQUIRED_WORKFLOWS = List.of("Compile and test");

    public void configure(Context ctx) {

        // find existing organization by its name
//        final var organization = GithubFunctions.getOrganization(
//                GetOrganizationArgs.builder()
//                        .name(ORGANIZATION_NAME)
//                        .build()
//        );

        // declare repository resource
        final var repository = new Repository(REPOSITORY_NAME, // name of the resource for Pulumi
                RepositoryArgs.builder()
                        .name(REPOSITORY_NAME) // name of the repository
                        .autoInit(true)
                        .allowAutoMerge(false)
                        .allowAutoMerge(false)
                        .allowSquashMerge(true)
                        .allowRebaseMerge(false)
                        .deleteBranchOnMerge(true)
                        .hasDownloads(true)
                        .hasIssues(true)
                        .hasProjects(true)
                        .visibility("public")
                        .build(),
                CustomResourceOptions.builder() // third parameter of constructor are options common for all resources
                        //.protect(true)
                        .build()
        );

        // declare default branch
        final var branchDefault = new BranchDefault(BRANCH_DEFAULT_NAME,
                BranchDefaultArgs.builder()
                        .branch(BRANCH_DEFAULT_NAME)
                        .repository(repository.name()) // in which repository branch should be created
                        .build()
        );

        final var branchProtection = new BranchProtection(BRANCH_DEFAULT_PROTECTION_NAME,
                BranchProtectionArgs.builder()
                        .repositoryId(repository.nodeId()) // link branch protection to repository
                        .pattern(branchDefault.branch()) // pattern for names of branches, to which the protection should be apllied
                        .requireConversationResolution(true) // all discussion in pull request should be resolved before merge
                        .requiredPullRequestReviews(BranchProtectionRequiredPullRequestReviewArgs.builder()
                                .requiredApprovingReviewCount(1) // how many approvals required before merge
                                .build())
                        .requiredStatusChecks(BranchProtectionRequiredStatusCheckArgs.builder()
                                .contexts(REQUIRED_WORKFLOWS)
                                .build())
                        .build()
        );

        final var labelCritical = new IssueLabel(LABEL_CRITICAL_NAME,
                IssueLabelArgs.builder()
                        .repository(repository.name())
                        .name(LABEL_CRITICAL_NAME)
                        .color("FF0000") // color hex without hash
                        .build()
        );
    }

}
