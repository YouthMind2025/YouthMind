# CI/CD Setup for YouthMind

This document explains the CI/CD (Continuous Integration/Continuous Deployment) setup for the YouthMind project using GitHub Actions.

## Overview

Our CI/CD pipeline is designed to work with a branching model that uses `backend` and `frontend` as integration branches before merging into `main`.

## Branch Strategy

### Branch Types and Workflows

1. **`main`** - Production branch
   - Final, stable code.
   - Triggers deployment to the production environment (with manual approval).
   - Merged into from `backend` and `frontend` branches after they are stable.

2. **`backend`** - Backend integration branch
   - Stable integration point for all backend-related features.
   - Triggers full CI testing for the backend.
   - Triggers automatic deployment to the staging environment for the backend services.

3. **`frontend`** - Frontend integration branch
   - Stable integration point for all frontend-related features (iOS, Admin Panel).
   - Triggers full CI testing for the frontend components.
   - Can be configured to deploy to a staging/preview environment.

4. **Feature Branches** (e.g., `backend-harley`, `feature/new-login`)
   - Individual development branches for new features or bug fixes.
   - CI runs when a Pull Request is opened to `backend` or `frontend`.
   - Merged into `backend` or `frontend` upon completion.

## Workflow Files

All workflow files are located in the `.github/workflows/` directory.

### 1. `ci-backend.yml`
**Triggers**: Pushes or Pull Requests to `main` and `backend` branches.
**Purpose**: Run CI tests for the backend.

**What it does**:
- Sets up Java 17, runs Maven tests, builds the JAR, and performs a security scan.

### 2. `ci-ios.yml`
**Triggers**: Pushes or Pull Requests to `main` and `frontend` branches.
**Purpose**: Run CI tests for the iOS application.

**What it does**:
- Sets up Xcode, installs dependencies, runs tests on the iOS simulator, and archives the build.

### 3. `deploy-backend-staging.yml`
**Triggers**: Pushes to the `backend` branch.
**Purpose**: Deploy backend services to the staging environment.

**What it does**:
- Builds and pushes Docker images for the backend and AI services to your container registry.
- Deploys the new images to your Kubernetes staging cluster.

### 4. `deploy-production.yml`
**Triggers**: Pushes to the `main` branch.
**Purpose**: Deploy all services to the production environment.

**What it does**:
- Requires manual approval before running.
- Builds and deploys all containerized services (backend, AI).
- Runs smoke tests after deployment and sends a Slack notification.

### 5. `feature-branch-ci.yml`
**Triggers**: Pull Requests to `backend` and `frontend` branches.
**Purpose**: Lightweight testing for feature branches.

**What it does**:
- Runs quick tests and code quality checks to validate changes before they are merged into an integration branch.

## Setup Instructions

### 1. Repository Secrets

You need to configure these secrets in your GitHub repository:

Go to **Settings** → **Secrets and variables** → **Actions**

#### Required Secrets:
```
CONTAINER_REGISTRY          # Your container registry URL (e.g., ghcr.io/your-org)
REGISTRY_USERNAME           # Container registry username
REGISTRY_PASSWORD           # Container registry password/token
PRODUCTION_API_URL          # Production API health check URL
SLACK_WEBHOOK_URL           # Slack webhook for deployment notifications
SONAR_HOST_URL              # SonarQube server URL
SONAR_TOKEN                 # SonarQube authentication token
```

#### Optional Secrets:
```
KUBECONFIG                  # Base64 encoded kubeconfig for Kubernetes access
```

### 2. Environment Protection

Set up environment protection rules:

1. Go to **Settings** → **Environments**
2. Create `staging` and `production` environments
3. For `production` environment:
   - Enable "Required reviewers" (add team members who can approve)
   - Enable "Wait timer" (optional delay before deployment)
   - Enable "Deployment branches" (restrict to `main` branch)

### 3. Branch Protection Rules

Set up branch protection for `main` and `develop`:

1. Go to **Settings** → **Branches**
2. Add rule for `main`:
   - ✅ Require a pull request before merging
   - ✅ Require status checks to pass before merging
   - ✅ Require branches to be up to date before merging
   - ✅ Include administrators
3. Add rule for `develop`:
   - ✅ Require a pull request before merging
   - ✅ Require status checks to pass before merging

## How to Use

### Development Workflow

1.  **Create a feature branch** from `backend` or `frontend`:
    ```bash
    # For a backend feature
    git checkout -b feature/backend/new-api backend
    ```

2.  **Make changes and push**:
    ```bash
    git commit -m "feat: add new API endpoint"
    git push origin feature/backend/new-api
    ```

3.  **Create a Pull Request**:
    - Open a PR on GitHub from your feature branch to the appropriate integration branch (`backend` or `frontend`).
    - The `feature-branch-ci` workflow will run automatically.
    - Ensure all checks pass and get your code reviewed.

4.  **Merge to Integration Branch**:
    - Once the PR is approved, merge it.
    - If merged into `backend`, the `deploy-backend-staging` workflow will run, deploying your changes to the staging environment.

5.  **Merge to Main and Deploy to Production**:
    - When `backend` and `frontend` branches are stable and ready for release, merge them into `main`.
    - Merging to `main` will trigger the `deploy-production` workflow.
    - A designated person must approve the deployment in the GitHub Actions tab for it to proceed.

## Monitoring and Troubleshooting

### View Workflow Runs

1. Go to **Actions** tab in your repository
2. Click on any workflow to see detailed logs
3. Use the search/filter to find specific runs

### Common Issues

1. **Tests failing**:
   - Check the specific job logs
   - Look for dependency issues or test failures
   - Fix locally and push again

2. **Deployment failures**:
   - Check Kubernetes cluster status
   - Verify container registry credentials
   - Check environment secrets

3. **iOS build issues**:
   - Verify Xcode version compatibility
   - Check CocoaPods dependencies
   - Ensure all required certificates are available

### Manual Workflow Triggers

You can manually trigger workflows:

1. Go to **Actions** tab
2. Select the workflow you want to run
3. Click **Run workflow**
4. Select branch and any required inputs

## Best Practices

1. **Keep branches small**: Smaller PRs are easier to review and test
2. **Test locally first**: Run tests locally before pushing
3. **Use descriptive commit messages**: Helps with debugging and rollbacks
4. **Monitor deployments**: Check logs and health endpoints after deployment
5. **Use feature flags**: For risky changes, use feature flags to control rollout

## Customization

### Adding New Services

To add CI/CD for a new service:

1. Create a new workflow file (e.g., `ci-admin.yml`)
2. Follow the pattern of existing workflows
3. Add deployment steps to staging/production workflows
4. Update this documentation

### Modifying Triggers

To change when workflows run:

1. Edit the `on` section of the workflow file
2. Add/remove branches or paths as needed
3. Test the changes with a small commit

### Adding New Environments

To add a new environment (e.g., `testing`):

1. Create new workflow file or modify existing
2. Add environment configuration in GitHub
3. Update deployment scripts
4. Add environment-specific secrets

## Security Considerations

1. **Secrets management**: Never commit secrets to code
2. **Environment isolation**: Use separate environments for different stages
3. **Access control**: Limit who can approve production deployments
4. **Audit logging**: All deployments are logged in GitHub Actions
5. **Rollback capability**: Keep previous versions for quick rollback

## Support

For issues with CI/CD:

1. Check the Actions tab for error logs
2. Review this documentation
3. Check GitHub Actions documentation
4. Contact the DevOps team 