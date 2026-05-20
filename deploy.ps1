# deploy.ps1 - Deploy budget-by-defo to AWS App Runner
# Usage: .\deploy.ps1 [-Region us-east-1]

param(
    [string]$Region = "us-east-1"
)

$ErrorActionPreference = "Stop"

$ECR_REPO = "budget-by-defo"
$AWS_REGION = $Region

function Write-Step($msg) { Write-Host "`n==> $msg" -ForegroundColor Cyan }
function Write-OK($msg)   { Write-Host "    OK: $msg" -ForegroundColor Green }
function Write-Fail($msg) { Write-Host "    FAIL: $msg" -ForegroundColor Red }

# ── 1. Resolve AWS account ID ──────────────────────────────────────────────
Write-Step "Resolving AWS account ID..."
$AWS_ACCOUNT_ID = aws sts get-caller-identity --query Account --output text
if (-not $AWS_ACCOUNT_ID) { Write-Fail "Could not resolve AWS account ID. Is the AWS CLI configured?"; exit 1 }
Write-OK "Account: $AWS_ACCOUNT_ID  |  Region: $AWS_REGION"

$ECR_URI = "$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com"
$IMAGE_URI = "$ECR_URI/${ECR_REPO}:latest"

# ── 2. Authenticate Docker with ECR ────────────────────────────────────────
Write-Step "Authenticating Docker with ECR..."
aws ecr get-login-password --region $AWS_REGION |
    docker login --username AWS --password-stdin $ECR_URI
Write-OK "Docker authenticated with ECR"

# ── 3. Build Docker image ───────────────────────────────────────────────────
Write-Step "Building Docker image..."
docker build -t $ECR_REPO .
Write-OK "Image built: $ECR_REPO"

# ── 4. Tag and push to ECR ─────────────────────────────────────────────────
Write-Step "Tagging and pushing image to ECR..."
docker tag "${ECR_REPO}:latest" $IMAGE_URI
docker push $IMAGE_URI
Write-OK "Pushed: $IMAGE_URI"

# ── 5. Trigger App Runner redeployment ─────────────────────────────────────
Write-Step "Triggering App Runner redeployment..."
$SERVICE_ARN = aws apprunner list-services --region $AWS_REGION `
    --query "ServiceSummaryList[?ServiceName=='$ECR_REPO'].ServiceArn" `
    --output text
if (-not $SERVICE_ARN) { Write-Fail "Could not find App Runner service '$ECR_REPO' in $AWS_REGION"; exit 1 }
aws apprunner start-deployment --service-arn $SERVICE_ARN --region $AWS_REGION | Out-Null
Write-OK "Deployment triggered for service: $SERVICE_ARN"

Write-Host "`n✅ Deployment complete! App Runner is rolling out the new version." -ForegroundColor Green
Write-Host "   Monitor: https://$AWS_REGION.console.aws.amazon.com/apprunner/home?region=$AWS_REGION#/services" -ForegroundColor DarkGray
