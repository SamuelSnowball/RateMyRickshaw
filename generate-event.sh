#!/bin/bash
# Script to generate event.json with base64 encoded local image

IMAGE_PATH="src/main/resources/rickshaws/0.jpg"

if [ ! -f "$IMAGE_PATH" ]; then
    echo "Error: Image file not found at $IMAGE_PATH"
    exit 1
fi

echo "Generating event.json with base64 encoded image from $IMAGE_PATH..."

# Encode image to base64
if command -v base64 &> /dev/null; then
    BASE64_IMAGE=$(base64 -w 0 "$IMAGE_PATH" 2>/dev/null || base64 "$IMAGE_PATH" | tr -d '\n')
else
    echo "Error: base64 command not found"
    exit 1
fi

# Create event.json
cat > event.json << EOF
{
  "imageBase64": "$BASE64_IMAGE"
}
EOF

echo "event.json created successfully!"
echo "File size: $(wc -c < event.json) bytes"
echo ""
echo "To test with SAM CLI:"
echo "  sam local invoke RickshawAnalysisFunction -e event.json"
