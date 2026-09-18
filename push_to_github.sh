#!/usr/bin/env bash
# ==============================================================================
# GitHub Direct Push Script for Institutional Stock Intelligence Platform
# ==============================================================================
# Usage:
#   ./push_to_github.sh <GITHUB_USERNAME> <GITHUB_REPO_NAME> <PERSONAL_ACCESS_TOKEN>
# Or run interactively:
#   ./push_to_github.sh
# ==============================================================================

set -e

echo "========================================================"
echo " Institutional Stock Intelligence — Direct GitHub Push"
echo "========================================================"

USERNAME="$1"
REPO="$2"
TOKEN="$3"

if [ -z "$USERNAME" ]; then
    read -p "Enter your GitHub Username: " USERNAME
fi

if [ -z "$REPO" ]; then
    read -p "Enter your GitHub Repository Name (e.g. stock-intelligence): " REPO
fi

if [ -z "$TOKEN" ]; then
    echo ""
    echo "To get a GitHub Personal Access Token (classic):"
    echo "  1. Go to https://github.com/settings/tokens"
    echo "  2. Generate new token (classic)"
    echo "  3. Select scope: 'repo' (Full control of private repositories)"
    echo "  4. Copy and paste the token below (it will be hidden):"
    read -s -p "Enter your GitHub Personal Access Token (ghp_...): " TOKEN
    echo ""
fi

if [ -z "$USERNAME" ] || [ -z "$REPO" ] || [ -z "$TOKEN" ]; then
    echo "Error: Username, repository name, and access token are all required."
    exit 1
fi

echo ""
echo "Configuring Git..."
git config --global user.name "$USERNAME"
git config --global user.email "$USERNAME@users.noreply.github.com"
git config --global init.defaultBranch main

if [ ! -d ".git" ]; then
    echo "Initializing local git repository..."
    git init
fi

echo "Staging files..."
git add .

echo "Committing files..."
git commit -m "feat: complete institutional stock intelligence platform" || echo "Working tree clean, proceeding..."

git branch -M main

REMOTE_URL="https://${USERNAME}:${TOKEN}@github.com/${USERNAME}/${REPO}.git"

echo "Setting remote origin..."
if git remote get-url origin >/dev/null 2>&1; then
    git remote set-url origin "$REMOTE_URL"
else
    git remote add origin "$REMOTE_URL"
fi

echo "Pushing to GitHub (main)..."
git push -u origin main --force

# Remove credentials from git remote config for safety
git remote set-url origin "https://github.com/${USERNAME}/${REPO}.git"

echo ""
echo "========================================================"
echo " SUCCESS! All project files pushed to:"
echo " https://github.com/${USERNAME}/${REPO}"
echo "========================================================"
