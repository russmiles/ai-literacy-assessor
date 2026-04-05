#!/usr/bin/env bash
# run.sh — Start the ALCI Assessor without Docker.
#
# Usage:
#   ./run.sh                          CLI mode (interactive terminal)
#   ./run.sh web                      Web mode (chat UI on http://localhost:8080)
#   ./run.sh web openai               Web mode with dual-provider cost optimisation
#
# Prerequisites:
#   - JDK 21+
#   - Maven 3.9+
#   - ANTHROPIC_API_KEY environment variable set
#   - OPENAI_API_KEY environment variable set (optional, for dual-provider mode)

set -euo pipefail

# --- Validate prerequisites ---

if [ -z "${ANTHROPIC_API_KEY:-}" ]; then
  echo "Error: ANTHROPIC_API_KEY is not set." >&2
  echo "" >&2
  echo "  export ANTHROPIC_API_KEY=\"your-key\"" >&2
  echo "  ./run.sh" >&2
  exit 1
fi

if ! command -v mvn &>/dev/null; then
  echo "Error: Maven is not installed. Install Maven 3.9+ and try again." >&2
  exit 1
fi

if ! java -version 2>&1 | grep -q "version \"21"; then
  echo "Warning: JDK 21 recommended. Current version:" >&2
  java -version 2>&1 | head -1 >&2
fi

# --- Parse arguments ---

PROFILES=""

for arg in "$@"; do
  case "$arg" in
    web)
      PROFILES="${PROFILES:+$PROFILES,}web"
      ;;
    openai)
      if [ -z "${OPENAI_API_KEY:-}" ]; then
        echo "Error: OPENAI_API_KEY is not set (required for openai mode)." >&2
        echo "" >&2
        echo "  export OPENAI_API_KEY=\"your-key\"" >&2
        echo "  ./run.sh openai" >&2
        exit 1
      fi
      PROFILES="${PROFILES:+$PROFILES,}openai"
      ;;
    --help|-h)
      echo "ALCI Assessor — AI Literacy assessment chatbot"
      echo ""
      echo "Usage:"
      echo "  ./run.sh                CLI mode (interactive terminal)"
      echo "  ./run.sh web            Web mode (http://localhost:8080)"
      echo "  ./run.sh openai         CLI mode with dual-provider (Claude + OpenAI)"
      echo "  ./run.sh web openai     Web mode with dual-provider"
      echo ""
      echo "Environment variables:"
      echo "  ANTHROPIC_API_KEY       Required. Anthropic API key."
      echo "  OPENAI_API_KEY          Optional. OpenAI key for cost-optimised routing."
      echo "  ASSESSOR_OUTPUT_DIR     Optional. Output directory (default: ./assessments)"
      exit 0
      ;;
    *)
      echo "Unknown argument: $arg (try ./run.sh --help)" >&2
      exit 1
      ;;
  esac
done

# --- Build and run ---

echo "Building ALCI Assessor..."
mvn -B package -DskipTests -q

if [ -n "$PROFILES" ]; then
  echo "Starting with profiles: $PROFILES"
  SPRING_PROFILES_ACTIVE="$PROFILES" java -jar target/alci-assessor-0.1.0-SNAPSHOT.jar
else
  echo "Starting in CLI mode..."
  java -jar target/alci-assessor-0.1.0-SNAPSHOT.jar
fi
