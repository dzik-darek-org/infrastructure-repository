# GitHub configuration & CI/CD example

This repository contains example of:  
* GitHub repository configuration within a GitHub Organization 
with use of [Pulumi][url-pulumi],  
* CI/CD setup with use of [GitHub Actions][url-github-actions-quickstart].  

### Table of contents

* [GitHub as project management tool](#github-as-project-management-tool)
  * [Organizations](#organizations)
  * [Projects](#projects)
  * [Pull requests & CI/CD](#pull-requests--cicd)
    * [Checks](#checks)
    * [Automation of GitHub Actions](#automation-of-github-actions)
* [Pulumi](#pulumi)
  * [Concept of Resources](#concept-of-resources) 
  * [Requirements](#requirements)
  * [Including Pulumi into project](#including-pulumi-into-project)
* [Configuring GitHub repository with Pulumi](#configuring-github-repository-with-pulumi)
  * [Example resources for GitHub](#example-resources-for-github)
    * [Repository](#repository)
    * [Default Branch](#default-branch)
    * [Branch Protection](#branch-protection)
    * [Labels for Issues](#labels-for-issues)
  * [Deployment](#deployment)
    * [Authentication at GitHub](#authentication-at-github)
    * [Resource deployment](#resources-deployment)
* [Known issues](#known-issues)
* [Links & sources](#links--sources)

## GitHub as project management tool

### Organizations 

GitHub allows to create [Organizations][url-github-organizations-docs] and gather project-related things, 
like issues, tasks, repositories, etc. in one place.  
![image-github-organization-members]

### Projects

[Projects][url-github-projects-docs] allow to track and manage issues. 
They deliver comfy ways to link issues with contents of repositories (like pull requests).
![image-github-project]

### Pull requests & CI/CD

PRs at GitHub have many functions making life easier, i.e. one can set templates for descriptions, 
notify contributors about awaiting reviews (so called CODEOWNERS file, for free available only in public repositories)
or link issues and automatically close them after merge.

![image-github-pr]

#### Checks

Pull requests at GitHub are fully integrated with [GitHub Actions][url-github-actions-quickstart] -
a tremendous tool for automation of builds and deployment.  
One can configure it to run tests after PR creation or pushes, deploy artifacts and so on.  
Free tier allows for 2000 minutes of running of CI/CD tools.

![image-github-checks]

#### Automation of GitHub Actions

**GitHub Actions** can be automated with configuration files written in YAML.  
Inside the repository one should create folder `.github/workflows` 
and put the configuration file inside `workflows` folder.  
Example configuration of a workflow (file `infrastucture-repository.yml`):  
```yaml
name: Compile and test # workflow name

on: # events trigerring workflow
  pull_request:
    branches:
      - main

jobs:
  compile-and-test: # job name
    name: Compile and test
    runs-on: ubuntu-latest # operating system of virtual machine, on which workflow should run
    steps:
      - name: Checkout project sources # step name
        uses: actions/checkout@v2 # github action to take
      - name: Setup Gradle
        uses: gradle/gradle-build-action@v2
      - name: Compile and test
        working-directory: ./infrastructure # location of project sources
        run: ./gradlew build test
```

## Pulumi

[Pulumi][url-pulumi] is an open-source tool allowing to set up the configuration for cloud services (mostly) 
within application's code (so called Infrastructure-as-Code).  
It supports multiple languages (i. e. Python, C#, Java) and cloud providers (AWS, Azure, Google Cloud).
Community creates own provider packages for other services and publishes them to [Pulumi Registry][url-pulumi-registry].  
In this case we are focusing on [GitHub provider][url-pulumi-registry-github].

**Using Pulumi with GitHub provider allows to configure GitHub repositories comfortably from the code 
instead of clicking on multiple tabs and pages in UI settings
and to keep single source of truth for the configuration.**

### Concept of Resources

In Pulumi's terminology a [Resource][url-pulumi-resources-docs] is a fundamental unit representing a computing instance, storage
or any entity from a provider's universe.

Simply declaring a resource in code causes its creation by **Pulumi Service**.

Each resource declaration follows pattern:
```java
new Resource("resource name", // name of the resource for Pulumi
        ResourceArgs.builder()
            .id(1) // property of the resource
            .build(),
        CustomResourceOptions.builder() // properties common for every resource
            .parent(anotherResource)
            .build()
);
```

[GitHub provider][url-pulumi-registry-github] for Pulumi delivers representations for various resources
(like repositories, branches) in form of corresponding classes.

Available resources are listed in [docs][url-pulumi-github-docs].
Unfortunately, not every resource is completely described,
because Java support for Pulumi is in the initial phase of development.  
Better descriptions are available in [documentation for Terraform][url-terraform-github-docs].

### Requirements

In order to work with [Pulumi][url-pulumi] one has to:  
* install [Pulumi CLI][url-pulumi-cli] - it is used to configure and manage the deployment state of resources,
* create [Pulumi Service][url-pulumi-service-docs] account - web application provided by developers of Pulumi,
it manages the deployment state of resources (**CLI** automatically uses the **Service**) 
and allows to browse the deployment state on dedicated web page.  
    Official **Pulumi Service** can be omitted if one decides to set up self-managed backend for the Service.

### Including Pulumi into project

Steps to begin using Pulumi and basic configuration:

1. Install [**Pulumi CLI**][url-pulumi-cli].
2. Create [**Pulumi Service**][url-pulumi-service-docs] account. 
3. Create **Pulumi Access Token** at **Pulumi Service**, it will be used to log in into **Pulumi CLI**, 
save the token locally (i. e. in [KeePassXC][url-keepassxc]), because it won't be discoverable after creation.  
   ![image-pulumi-security-token-creation]  
4. Log in into **Pulumi CLI** - in terminal use command `pulumi login` and then paste the access token.
5. Create a project - in this case a Java application with Gradle as build system.  
    ![image-gradle-init]  
6. Add dependencies to Pulumi packages - [core package in Java language][url-maven-pulumi-java] 
and [provider package][url-maven-pulumi-github] to `build.gradle.kts` file:  
    ```kotlin
    repositories {
        mavenCentral()
    }
    
    dependencies {
        implementation("com.pulumi:pulumi:(,1.0]")
        implementation("com.pulumi:github:5.0.0")
    }
    ```
7. Create [configuration file][url-pulumi-yaml-docs] `Pulumi.yaml` in root directory of project, fill it with basic configuration properties:  
   ```yaml
   name: infrastructure # name of the project
   description: A minimal Java Pulumi program with Gradle builds and configuration for GitHub project
   runtime: java # installed language runtime for the project
   ```
8. Create a **Stack** - it can be understood as a set of resources, it keeps theirs state and configuration 
and will be managed by **Pulumi Service** (deployed, updated or deleted).  
In **CLI** use command `pulumi stack`:  
![image-pulumi-stack-creation]  
State and contents of **Stack** will be replicated at **Pulumi Service** 
and deployment of resources (at any could or GitHub) will happen based on the **Stack**. 
      
    At this point **Stack** should be visible at **Pulumi Service**:  
    ![image-pulumi-service-stack-created]  
    Tag `github:owner` is visible, because GitHub account was used to sign up to the Service.
9. Invoke Pulumi in entry point of the app and declare resources:
   ```java
   public class App {
        public static void main(String[] args) {
            Pulumi.run(context -> {
                // declare resources here
            });
        }
    }
   ```

## Configuring GitHub repository with Pulumi

After setting up Pulumi like described above, one can begin to declare resources in code.

### Example resources for GitHub

#### Repository

Creation of Repository resource:  
```java
final var repository = new Repository(REPOSITORY_NAME,
        RepositoryArgs.builder()
                .name(REPOSITORY_NAME) // name of the repository
                .autoInit(true) // creates main branch and initial commit
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
        CustomResourceOptions.builder()
                .protect(true)
                .build()
);
```

#### Default Branch

Creation of default branch in repository created earlier:  
```java
final var branchDefault = new BranchDefault(BRANCH_DEFAULT_NAME,
        BranchDefaultArgs.builder()
                .branch(BRANCH_DEFAULT_NAME)
                .repository(repository.name()) // in which repository branch should be created
                .build()
);
```

#### Branch Protection

Setting the security options for repository and branches:  
```java
private static final List REQUIRED_WORKFLOWS = List.of("Compile and test");

final var branchProtection = new BranchProtection(BRANCH_DEFAULT_PROTECTION_NAME,
        BranchProtectionArgs.builder()
                .repositoryId(repository.nodeId()) // link branch protection to repository
                .pattern(branchDefault.branch()) // pattern for names of branches, to which the protection should be apllied
                .requireConversationResolution(true) // all discussion in pull request should be resolved before merge
                .requiredPullRequestReviews(BranchProtectionRequiredPullRequestReviewArgs.builder()
                        .requiredApprovingReviewCount(2) // how many approvals required before merge
                        .build())
                .requiredStatusChecks(BranchProtectionRequiredStatusCheckArgs.builder()
                        .contexts(REQUIRED_WORKFLOWS) // PRs mergin code to this branch will require given workflows to pass before merge
                        .build())
                .build()
);
```

#### Labels for Issues

Creation of labels, which can be attached to issues in project:  
```java
final var labelCritical = new IssueLabel(LABEL_CRITICAL_NAME,
        IssueLabelArgs.builder()
                .repository(repository.name())
                .name(LABEL_CRITICAL_NAME)
                .color("FF0000") // color hex without hash
                .build()
);
```

### Deployment

After setting up the configuration one can check, what will Pulumi do and which resources will be created.  
In order to do this, simply use command `pulumi preview` in terminal:  
![image-pulumi-preview]  

#### Authentication at GitHub

To let Pulumi know, for which user or organization the resources should be created,
one has to set `github:owner` configuration variable.  
It can be done in **Pulumi CLI** with use of command:  
`pulumi config set github:owner <owner-name>`

Every action taken at GitHub by Pulumi requires Pulumi to be authenticated and to have particular permissions.  
To authenticate Pulumi, one has to create [Personal Access Token][url-github-personal-access-token-docs] at GitHub
and then set variable `github:token` in **Pulumi CLI** with use of command:  
`pulumi config set github:token <token> --secret`  

![image-pulumi-config]

#### Resources deployment

Deployment can be initiated with use of command `pulumi up`:  

![image-pulumi-up]

At **Pulumi Service** following resources were created:

![image-pulumi-service-resources-created]

Initial state of repository at GitHub:

![image-github-created-repository]

## Known issues

Java support for Pulumi is at early stage of development,
many things can be configured and deployed with existing provider packages,
but it is still possible to encounter bugs.  

For example, creation of GitHub Project with Pulumi is not currently possible (08.10.2022),  
because resource `OrganizationProject` uses old GitHub REST API for managing projects 
and this feature was [disabled][url-stackoverflow-github-project-creation-issue] on the GitHub side 
(Project creation works through [GraphQL API][url-github-graphql-reference-create-project]).
Attempt to create this resource with `pulumi up` will return HTTP code 410.

## Links & sources

* Pulumi
  * [Pulumi main site][url-pulumi]
  * [Pulumi CLI documentation][url-pulumi-cli]
  * [GitHub provider for Pulumi documentation][url-pulumi-registry-github]
  * [Pulumi Registry (repository with providers)][url-pulumi-registry]
  * [description of concept of resources at Pulumi][url-pulumi-resources-docs]
  * [description of Pulumi Service][url-pulumi-service-docs]
  * [Pulumi configuration file documentation][url-pulumi-yaml-docs]
* Terraform
  * [GitHub provider for Terraform documentation][url-terraform-github-docs]
* GitHub
  * [GitHub Actions documentation][url-github-actions-quickstart]
  * [GitHub Organizations documentation][url-github-organizations-docs]
  * [GitHub Projects documentation][url-github-projects-docs]
  * [creation of personal access token at GitHub][url-github-personal-access-token-docs]
* KeePassXC
  * [KeePassXC password manager][url-keepassxc]
* Maven
  * [artifact Pulumi GitHub][url-maven-pulumi-github]
  * [artifact Pulumi Java][url-maven-pulumi-java]
* Stackoverflow
  * [Unable to create project in repository or organisation using GitHub REST API][url-stackoverflow-github-project-creation-issue]


[image-pulumi-config]: assets/images/pulumi_config.png
[image-pulumi-preview]: assets/images/pulumi_preview.png
[image-pulumi-security-token-creation]: assets/images/pulumi_security_token_creation.jpg
[image-pulumi-stack-creation]: assets/images/pulumi_stack_creation.png
[image-pulumi-service-resources-created]: assets/images/pulumi_service_resources_created.png
[image-pulumi-service-stack-created]: assets/images/pulumi_service_stack_created.png
[image-pulumi-up]: assets/images/pulumi_up.png

[image-github-checks]: assets/images/github_checks.png
[image-github-created-repository]: assets/images/github_created_repository.png
[image-github-pr]: assets/images/github_pr.png
[image-github-project]: assets/images/github_project.png
[image-github-organization-members]: assets/images/github_organization_members.png

[image-gradle-init]: assets/images/gradle_init.png

[url-pulumi]: https://www.pulumi.com/
[url-pulumi-cli]: https://www.pulumi.com/docs/reference/cli/
[url-pulumi-github-docs]: https://www.pulumi.com/registry/packages/github/api-docs/
[url-pulumi-registry]: https://www.pulumi.com/registry/
[url-pulumi-registry-github]: https://www.pulumi.com/registry/packages/github/
[url-pulumi-resources-docs]: https://www.pulumi.com/docs/intro/concepts/resources/
[url-pulumi-service-docs]: https://www.pulumi.com/docs/intro/pulumi-service/
[url-pulumi-yaml-docs]: https://www.pulumi.com/docs/reference/pulumi-yaml/

[url-terraform-github-docs]: https://registry.terraform.io/providers/integrations/github/latest/docs

[url-github-actions-quickstart]: https://docs.github.com/en/actions/quickstart
[url-github-organizations-docs]: https://docs.github.com/en/organizations/collaborating-with-groups-in-organizations/about-organizations
[url-github-personal-access-token-docs]: https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/creating-a-personal-access-token
[url-github-projects-docs]: https://docs.github.com/en/issues/planning-and-tracking-with-projects/learning-about-projects/about-projects
[url-github-graphql-reference-create-project]: https://docs.github.com/en/graphql/reference/mutations#createprojectv2

[url-keepassxc]: https://keepassxc.org/

[url-maven-pulumi-github]: https://search.maven.org/artifact/com.pulumi/github/5.0.0/jar
[url-maven-pulumi-java]: https://search.maven.org/artifact/com.pulumi/pulumi/0.6.0/jar

[url-stackoverflow-github-project-creation-issue]: https://stackoverflow.com/questions/73268885/unable-to-create-project-in-repository-or-organisation-using-github-rest-api