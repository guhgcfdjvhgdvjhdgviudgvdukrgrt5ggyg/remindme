#!/bin/bash
# Tum yahan token/update karke use kar lo
REPO_NAME="reminder"
USER_TOKEN="YOUR_GITHUB_TOKEN"
curl -X POST \
  -H "Authorization: token $USER_TOKEN" \
  -H "Accept: application/vnd.github.v3+json" \
  https://api.github.com/user/repos \
  -d "{\"name\":\"$REPO_NAME\",\"private\":false}"
