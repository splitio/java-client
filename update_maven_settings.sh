#!/bin/bash

# Script to update Maven settings.xml with Central Repository credentials using xmlstarlet

# ANSI color codes
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check if xmlstarlet is installed
if ! command -v xmlstarlet &> /dev/null; then
    echo -e "${RED}Error: xmlstarlet is not installed.${NC}"
    echo "Please install xmlstarlet first:"
    echo "  macOS: brew install xmlstarlet"
    echo "  Debian/Ubuntu: sudo apt-get install xmlstarlet"
    echo "  RHEL/CentOS/Fedora: sudo yum install xmlstarlet"
    echo "Then run this script again."
    exit 1
fi

# Default values
DEFAULT_SETTINGS_PATH="$HOME/.m2/settings.xml"
DEFAULT_SERVER_ID="central"

echo -e "${BLUE}Maven Settings.xml Update Script${NC}"
echo "This script will update your Maven settings.xml with Central Repository credentials."
echo

# Ask for settings.xml path or use default
read -p "Path to settings.xml [$DEFAULT_SETTINGS_PATH]: " SETTINGS_PATH
SETTINGS_PATH=${SETTINGS_PATH:-$DEFAULT_SETTINGS_PATH}

# Variables to store existing values
EXISTING_USERNAME=""
EXISTING_PASSWORD=""

# Extract existing values if settings.xml exists
if [ -f "$SETTINGS_PATH" ] && command -v xmlstarlet &> /dev/null; then
    # Check if the file is valid XML
    if xmlstarlet val "$SETTINGS_PATH" &> /dev/null; then
        echo -e "${YELLOW}Reading existing settings from ${SETTINGS_PATH}...${NC}"
        
        # Extract existing server ID
        DEFAULT_SERVER_ID=$(xmlstarlet sel -t -v "/settings/servers/server[1]/id" "$SETTINGS_PATH" 2>/dev/null || echo "$DEFAULT_SERVER_ID")
        
        # Extract existing username and password for the server
        EXISTING_USERNAME=$(xmlstarlet sel -t -v "/settings/servers/server[id='$DEFAULT_SERVER_ID']/username" "$SETTINGS_PATH" 2>/dev/null || echo "")
        EXISTING_PASSWORD=$(xmlstarlet sel -t -v "/settings/servers/server[id='$DEFAULT_SERVER_ID']/password" "$SETTINGS_PATH" 2>/dev/null || echo "")
    fi
fi

# Ask for server ID or use default/existing
read -p "Server ID [$DEFAULT_SERVER_ID]: " SERVER_ID
SERVER_ID=${SERVER_ID:-$DEFAULT_SERVER_ID}

# Ask for username (show existing if available)
USERNAME_PROMPT="Username"
if [ -n "$EXISTING_USERNAME" ]; then
    USERNAME_PROMPT="Username (current: $EXISTING_USERNAME)"
fi
read -p "$USERNAME_PROMPT: " USERNAME
USERNAME=${USERNAME:-$EXISTING_USERNAME}

# Ask for password (indicate if existing)
PASSWORD_PROMPT="Password"
if [ -n "$EXISTING_PASSWORD" ]; then
    PASSWORD_PROMPT="Password (leave empty to keep current)"
fi
read -s -p "$PASSWORD_PROMPT: " PASSWORD
echo
# Only use existing password if the user didn't enter a new one
if [ -z "$PASSWORD" ] && [ -n "$EXISTING_PASSWORD" ]; then
    PASSWORD="$EXISTING_PASSWORD"
fi

# Create .m2 directory if it doesn't exist
M2_DIR=$(dirname "$SETTINGS_PATH")
mkdir -p "$M2_DIR"

# No GPG configuration needed

# Function to create a new settings.xml file
create_new_settings() {
    echo -e "${YELLOW}Creating new settings.xml file...${NC}"
    cat > "$SETTINGS_PATH" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <servers>
    <server>
      <id>$SERVER_ID</id>
      <username>$USERNAME</username>
      <password>$PASSWORD</password>
    </server>
  </servers>
</settings>
EOF
}

# Check if settings.xml exists
if [ -f "$SETTINGS_PATH" ]; then
    echo -e "${YELLOW}Existing settings.xml found. Backing up to ${SETTINGS_PATH}.bak${NC}"
    cp "$SETTINGS_PATH" "${SETTINGS_PATH}.bak"
    
    # Check if the file is valid XML
    if ! xmlstarlet val "$SETTINGS_PATH" &> /dev/null; then
        echo -e "${RED}Warning: The existing settings.xml is not valid XML.${NC}"
        read -p "Do you want to create a new settings.xml file? (y/n): " CREATE_NEW
        if [[ $CREATE_NEW =~ ^[Yy]$ ]]; then
            create_new_settings
        else
            echo -e "${RED}Exiting without making changes.${NC}"
            exit 1
        fi
    else
        # Check if servers element exists
        if ! xmlstarlet sel -t -v "/settings/servers" "$SETTINGS_PATH" &> /dev/null; then
            echo -e "${YELLOW}No servers section found. Adding servers section...${NC}"
            xmlstarlet ed --inplace \
                -s "/settings" -t elem -n "servers" \
                -s "/settings/servers" -t elem -n "server" \
                -s "/settings/servers/server" -t elem -n "id" -v "$SERVER_ID" \
                -s "/settings/servers/server" -t elem -n "username" -v "$USERNAME" \
                -s "/settings/servers/server" -t elem -n "password" -v "$PASSWORD" \
                "$SETTINGS_PATH"
        else
            # Check if server with this ID already exists
            if xmlstarlet sel -t -v "/settings/servers/server[id='$SERVER_ID']" "$SETTINGS_PATH" &> /dev/null; then
                echo -e "${YELLOW}Server with ID '$SERVER_ID' already exists. Updating credentials...${NC}"
                # Update existing server credentials
                xmlstarlet ed --inplace \
                    -u "/settings/servers/server[id='$SERVER_ID']/username" -v "$USERNAME" \
                    -u "/settings/servers/server[id='$SERVER_ID']/password" -v "$PASSWORD" \
                    "$SETTINGS_PATH"
            else
                echo -e "${YELLOW}Adding new server with ID '$SERVER_ID'...${NC}"
                # Add new server to existing servers section
                xmlstarlet ed --inplace \
                    -s "/settings/servers" -t elem -n "server" \
                    -s "/settings/servers/server[last()]" -t elem -n "id" -v "$SERVER_ID" \
                    -s "/settings/servers/server[last()]" -t elem -n "username" -v "$USERNAME" \
                    -s "/settings/servers/server[last()]" -t elem -n "password" -v "$PASSWORD" \
                    "$SETTINGS_PATH"
            fi
        fi
    fi
else
    create_new_settings
fi

# Make sure the file has the right permissions
chmod 600 "$SETTINGS_PATH"

echo -e "${GREEN}Maven settings.xml updated successfully at $SETTINGS_PATH${NC}"
echo -e "${GREEN}Server ID: $SERVER_ID${NC}"
echo -e "${GREEN}Username: $USERNAME${NC}"
echo -e "${GREEN}Password: ********${NC}"

