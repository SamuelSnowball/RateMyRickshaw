#!/bin/bash

# Build script for RateMyRickshaw Lambda
echo "Building RateMyRickshaw Lambda..."

# Clean and package
mvn clean package

if [ $? -eq 0 ]; then
    echo "Build successful!"
    echo "Lambda deployment package: target/function.zip"
else
    echo "Build failed!"
    exit 1
fi
