# Alpha Vantage OpenAI Agent Example

A financial analysis agent that demonstrates how to use the OpenAI Agents SDK with the Alpha Vantage MCP server. This interactive CLI agent can answer questions about stocks, forex, and financial markets using real Alpha Vantage data.

![Demo](https://github.com/user-attachments/assets/3a6164ec-8d10-4ef5-b5ab-e45e0c76f105)

## Features

- Interactive chat interface with rich formatting
- Session management for conversation continuity
- Slash commands for session control
- Support for both HTTP and stdio MCP server connections
- Real-time tool execution display
- Configurable OpenAI models

## Prerequisites

- uv (Python package manager - install from [docs.astral.sh/uv](https://docs.astral.sh/uv/getting-started/installation/))
- Alpha Vantage API key (get one free at [alphavantage.co](https://www.alphavantage.co/support/#api-key))
- OpenAI API key

## Quick Setup

Navigate to the agent directory:
```bash
cd examples/agent
```

1. **Install dependencies:**
   ```bash
   uv sync
   ```

2. **Configure environment:**
   ```bash
   cp .env.example .env
   # Edit .env and add your OpenAI API key
   ```

3. **Configure MCP server:**
   ```bash
   cp mcp.json.example mcp.json
   # Edit mcp.json and replace YOUR_API_KEY with your Alpha Vantage API key
   ```

4. **Run the agent:**
   ```bash
   uv run main.py
   ```

## Configuration

### MCP Server Configuration

The `mcp.json` file supports two connection modes:

**HTTP Mode (Recommended):**
```json
{
  "servers": {
    "alphavantage": {
      "type": "http",
      "url": "https://mcp.alphavantage.co/mcp?apikey=YOUR_API_KEY"
    }
  }
}
```

**Local Stdio Mode:**
```json
{
  "servers": {
    "alphavantage": {
      "type": "stdio",
      "command": "uv",
      "args": ["run", "../../server/stdio_server.py", "YOUR_API_KEY"]
    }
  }
}
```

### Environment Variables

Create a `.env` file with:
```
OPENAI_API_KEY=your_openai_api_key_here
```

## Usage

### Basic Commands

```bash
# Start with default settings
uv run main.py

# Use specific OpenAI model
uv run main.py --model gpt-4o

# Resume previous session
uv run main.py --session-id your-session-id

# Enable verbose logging
uv run main.py --verbose
```

### Slash Commands

Once running, you can use these slash commands:

- `/help` - Show available commands
- `/clear` - Start a new session
- `/sessions` - List all available sessions
- `/resume` - Resume a specific session
- `/exit` or `/quit` - Exit the application

### Example Queries

Try asking the agent:

- "What's the current stock price of AAPL?"
- "Show me the daily price data for Tesla over the last week"
- "What are the top gainers in the market today?"
- "Compare the performance of MSFT vs GOOGL this month"
- "Get me intraday data for Bitcoin in USD"

## Architecture

The agent consists of several key components:

- **`main.py`** - Main application loop and CLI interface
- **`agent_display_manager.py`** - Rich console output formatting
- **`input_manager.py`** - User input handling with auto-completion
- **`session_manager.py`** - SQLite-based conversation persistence
- **`mcp_server_manager.py`** - MCP server connection management
- **`slash_commands.py`** - Interactive command processing

## Session Management

Sessions are automatically saved to a SQLite database (`sessions.db`) and include:

- Conversation history
- Agent state
- Tool execution results

Sessions persist across application restarts and can be resumed using the `--session-id` parameter.

## Development

### File Structure

```
examples/agent/
├── README.md                    # This file
├── main.py                      # Main application
├── pyproject.toml              # Project configuration
├── mcp.json.example            # MCP server config template
├── .env.example                # Environment variables template
├── agent_display_manager.py    # UI/display logic
├── input_manager.py            # Input handling
├── session_manager.py          # Session persistence
├── mcp_server_manager.py       # MCP connection management
└── slash_commands.py           # Interactive commands
```

### Adding Custom Tools

To extend the agent with additional MCP servers or tools:

1. Add server configuration to `mcp.json`
2. Update agent instructions in `main.py` if needed
3. The agent will automatically discover and use new tools

## Troubleshooting

**Connection Issues:**
- Verify your Alpha Vantage API key is correct
- Check internet connectivity for HTTP mode
- For stdio mode, ensure the server path is correct

**OpenAI API Issues:**
- Verify your OpenAI API key in `.env`
- Check you have sufficient API credits
- Try a different model if rate limited

**Session Issues:**
- Delete `sessions.db` to reset all sessions
- Use `--verbose` flag to see detailed logging

## License

This example is part of the Alpha Vantage MCP project and follows the same license terms.