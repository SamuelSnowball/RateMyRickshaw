# RateMyRickshaw Documentation Index

Welcome to the RateMyRickshaw documentation! This index will help you find the information you need.

## 📖 Documentation Files

### Getting Started
- **[PROJECT_SUMMARY.md](PROJECT_SUMMARY.md)** - Complete project overview and what's been created
- **[QUICKSTART.md](QUICKSTART.md)** - Fast setup guide for beginners (⭐ START HERE)
- **[README.md](README.md)** - Comprehensive project documentation

### Development & Deployment
- **[COMMANDS.md](COMMANDS.md)** - Quick reference for all common commands
- **[API_EXAMPLES.md](API_EXAMPLES.md)** - Sample requests, responses, and code examples
- **[TROUBLESHOOTING.md](TROUBLESHOOTING.md)** - Solutions to common issues

### Configuration
- **[.env.template](.env.template)** - Environment variables template

## 🚀 Quick Navigation

### I want to...

#### Deploy the application
1. Read: [QUICKSTART.md](QUICKSTART.md)
2. Run: `./deploy.sh`

#### Understand the project structure
- Read: [PROJECT_SUMMARY.md](PROJECT_SUMMARY.md)

#### Test the API
1. Reference: [API_EXAMPLES.md](API_EXAMPLES.md)
2. Run: `./scripts/test-api.sh`

#### Fix an issue
- Read: [TROUBLESHOOTING.md](TROUBLESHOOTING.md)

#### Find a specific command
- Read: [COMMANDS.md](COMMANDS.md)

#### Customize the application
- Read: [README.md](README.md) → Configuration section

## 📁 Project Structure

```
RateMyRickshaw/
├── 📚 Documentation
│   ├── README.md                    # Main documentation
│   ├── QUICKSTART.md               # Quick setup guide
│   ├── PROJECT_SUMMARY.md          # Project overview
│   ├── TROUBLESHOOTING.md         # Problem solutions
│   ├── COMMANDS.md                 # Command reference
│   ├── API_EXAMPLES.md            # API usage examples
│   └── INDEX.md                    # This file
│
├── ☕ Backend (Java/Quarkus)
│   ├── pom.xml                     # Maven dependencies
│   └── src/main/java/com/ratemyrickshaw/
│       ├── lambda/                 # Lambda handlers
│       ├── model/                  # Data models
│       └── service/                # Business logic
│
├── ⚛️ Frontend (React)
│   └── frontend/
│       ├── src/                    # React source code
│       └── public/                 # Static files
│
└── 🚀 Deployment Scripts
    ├── deploy.sh                   # Master deploy script
    └── scripts/                    # Individual deployment scripts
```

## 🎯 Common Tasks

### First Time Setup
```bash
# 1. Read the quickstart
cat QUICKSTART.md

# 2. Configure AWS
aws configure

# 3. Deploy everything
./deploy.sh
```

### Development Workflow
```bash
# Make code changes in src/main/java/...

# Test locally
./mvnw quarkus:dev

# Deploy updates
./build.sh
./scripts/deploy-lambda.sh
```

### Frontend Updates
```bash
# Make changes in frontend/src/...

# Test locally
cd frontend && npm start

# Deploy updates
cd .. && ./scripts/deploy-frontend.sh
```

### Debugging
```bash
# Check deployment status
./scripts/get-info.sh

# View logs
aws logs tail /aws/lambda/ratemyrickshaw-lambda --follow --region eu-west-2

# Test API
./scripts/test-api.sh
```

## 📚 Learning Path

### For Beginners
1. [QUICKSTART.md](QUICKSTART.md) - Setup and deployment
2. [API_EXAMPLES.md](API_EXAMPLES.md) - How to use the API
3. [README.md](README.md) - Understanding the architecture

### For Developers
1. [PROJECT_SUMMARY.md](PROJECT_SUMMARY.md) - Project structure
2. [COMMANDS.md](COMMANDS.md) - Development commands
3. Source code in `src/` - Implementation details

### For DevOps
1. [README.md](README.md) - Infrastructure overview
2. `scripts/` - Deployment scripts
3. [TROUBLESHOOTING.md](TROUBLESHOOTING.md) - Operational issues

## 🔍 Quick Reference

### Key Files

| File | Purpose |
|------|---------|
| `pom.xml` | Maven dependencies and build config |
| `src/main/java/com/ratemyrickshaw/lambda/RickshawAnalysisHandler.java` | Lambda entry point |
| `src/main/java/com/ratemyrickshaw/service/RekognitionService.java` | AWS Rekognition integration |
| `frontend/src/App.js` | React main component |
| `deploy.sh` | Full deployment automation |
| `scripts/deploy-lambda.sh` | Lambda deployment |
| `scripts/deploy-api-gateway.sh` | API Gateway setup |

### Key Commands

| Command | Purpose |
|---------|---------|
| `./deploy.sh` | Deploy entire application |
| `./build.sh` | Build Lambda function |
| `./mvnw quarkus:dev` | Run backend locally |
| `./scripts/test-api.sh` | Test deployed API |
| `./scripts/get-info.sh` | Show deployment info |

### Key URLs (After Deployment)

| Resource | URL Pattern |
|----------|-------------|
| API Endpoint | `https://[API_ID].execute-api.eu-west-2.amazonaws.com/prod` |
| Frontend | `http://ratemyrickshaw.s3-website.eu-west-2.amazonaws.com` |
| Lambda Console | `https://eu-west-2.console.aws.amazon.com/lambda/home?region=eu-west-2#/functions/ratemyrickshaw-lambda` |

## 💡 Tips

- **Stuck?** Check [TROUBLESHOOTING.md](TROUBLESHOOTING.md)
- **Need a command?** Check [COMMANDS.md](COMMANDS.md)
- **Want examples?** Check [API_EXAMPLES.md](API_EXAMPLES.md)
- **First time?** Start with [QUICKSTART.md](QUICKSTART.md)

## 🆘 Getting Help

1. Check [TROUBLESHOOTING.md](TROUBLESHOOTING.md) for common issues
2. Review CloudWatch Logs for errors
3. Run `./scripts/get-info.sh` to verify deployment
4. Check AWS Service Health: https://status.aws.amazon.com/

## 📝 Contributing

When adding new features:
1. Update relevant documentation
2. Add tests in `src/test/`
3. Update [API_EXAMPLES.md](API_EXAMPLES.md) if API changes
4. Test locally before deploying

## 🎉 Ready to Start?

```bash
# Quick start
cat QUICKSTART.md

# Full deployment
./deploy.sh

# Get deployment info
./scripts/get-info.sh
```

Happy coding! 🛺
