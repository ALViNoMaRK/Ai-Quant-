"""Display manager for OpenAI Agents SDK examples.

Handles formatted console output, panels, and streaming display for agent interactions.
"""

from rich.console import Console
from rich.markdown import Markdown
from rich.panel import Panel
from rich.theme import Theme
from rich.table import Table
import json


# Custom theme for event-based colors
custom_theme = Theme({
    "user": "green",
    "assistant": "cyan",
    "tool": "blue",
    "timestamp": "dim white",
})


class AgentDisplayManager:
    def __init__(self):
        self.console = Console(theme=custom_theme)

    def display_welcome(self, session_id: str):
        """Display welcome message with session info."""
        welcome_text = (
            f"Welcome to the Alpha Vantage Financial Agent! Ask questions about stocks, forex, and financial markets.\n"
            f"Session: {session_id}\n"
            f"Conversation history is automatically maintained across queries.\n"
            f"Type 'exit' or 'quit' to stop."
        )
        
        self.console.print(Panel(
            welcome_text,
            title="🤖 Alpha Vantage Financial Agent (with Session Memory)",
            border_style="assistant"
        ))

    def display_agent_config(self, agent_config):
        """Display agent configuration panel."""
        config_md = self._create_agent_config_md(agent_config)
        
        self.console.print(Panel(
            Markdown(config_md),
            title="Agent Info",
            border_style="user"
        ))

    def display_tool_execution(self, tool_name: str, arguments=None, call_id: str = None):
        """Display tool execution panel."""
        # Format arguments if provided as raw data
        if arguments is not None and not isinstance(arguments, str):
            try:
                args_text = f"```json\n{json.dumps(arguments, indent=2)}\n```"
            except:
                args_text = f"```\n{str(arguments)}\n```"
        elif isinstance(arguments, str) and not arguments.startswith('```'):
            # Handle raw JSON string arguments
            try:
                args = json.loads(arguments)
                args_text = f"```json\n{json.dumps(args, indent=2)}\n```"
            except:
                args_text = f"```\n{arguments}\n```"
        else:
            args_text = arguments or ""
        
        tool_md = self._create_tool_called_md(tool_name, args_text)
        
        title = "Tool Execution"
        if call_id:
            title = f"Tool Execution (ID: {call_id})"
        
        self.console.print(Panel(
            Markdown(tool_md),
            title=title,
            border_style="tool"
        ))

    def display_tool_result(self, output: str, call_id: str = None):
        """Display tool result panel."""
        # Truncate if too long
        if len(output) > 500:
            output_display = f"{output[:500]}..."
        else:
            output_display = output
            
        output_md = self._create_tool_output_md(output_display)
        
        title = "Tool Result"
        if call_id:
            title = f"Tool Result (ID: {call_id})"
        
        self.console.print(Panel(
            Markdown(output_md),
            title=title,
            border_style="tool"
        ))

    def display_agent_response(self, content):
        """Display agent response panel."""
        # Handle different content formats
        if isinstance(content, str):
            message_text = content
        elif isinstance(content, list):
            # Parse content array like in stream events
            for content_part in content:
                if hasattr(content_part, 'text'):
                    message_text = content_part.text
                    break
                elif isinstance(content_part, dict) and content_part.get('type') == 'output_text':
                    message_text = content_part.get('text', '')
                    break
            else:
                message_text = str(content)
        else:
            message_text = str(content)
        
        message_md = self._create_message_output_md(message_text)
        
        self.console.print(Panel(
            Markdown(message_md),
            title="Agent Response",
            border_style="assistant"
        ))

    def display_goodbye(self):
        """Display goodbye message."""
        self.console.print("Goodbye! 👋")

    def display_error(self, error: str):
        """Display error message."""
        self.console.print(f"Error: {error}")

    def display_info(self, message: str):
        """Display info message."""
        self.console.print(message)

    def display_sessions_table(self, sessions: list):
        """Display sessions in a formatted table."""
        table = Table(title="Previous Sessions")
        
        table.add_column("Session ID", no_wrap=True)
        table.add_column("Message Count", justify="center")
        table.add_column("First Message")
        table.add_column("Created At", no_wrap=True)
        table.add_column("Updated At", no_wrap=True)
        
        for session in sessions:
            table.add_row(
                session['session_id'],
                str(session['message_count']),
                session['first_message'],
                session.get('created_at', 'N/A'),
                session.get('updated_at', 'N/A')
            )
        
        self.console.print(table)

    def display_session_items(self, items: list):
        """Display conversation history items from a resumed session."""
        if not items:
            self.display_info("No conversation history found.")
            return
            
        self.console.print(Panel(
            f"Loaded {len(items)} messages from conversation history",
            title="📖 Session Resumed",
            border_style="assistant"
        ))
        
        # Display all conversation items using same methods as stream events
        for item in items:
            if item.get('role') == 'user':
                # Display user message
                self.console.print(Panel(
                    item.get('content', ''),
                    title="User Query",
                    border_style="user"
                ))
            elif item.get('type') == 'function_call':
                tool_name = item.get('name', 'Unknown Tool')
                arguments = item.get('arguments', '{}')
                call_id = item.get('call_id', '')
                self.display_tool_execution(tool_name, arguments, call_id)
                
            elif item.get('type') == 'function_call_output':
                output = item.get('output', '')
                call_id = item.get('call_id', '')
                self.display_tool_result(str(output), call_id)
                
            elif item.get('role') == 'assistant' and item.get('type') == 'message':
                content = item.get('content', [])
                self.display_agent_response(content)

    def _create_agent_config_md(self, agent_config) -> str:
        """Create markdown content for agent configuration."""
        return f"""**Name:** {agent_config.name}

**Model:** {agent_config.model}

**MCP Servers:** {len(agent_config.mcp_servers)} server(s)

**Instructions:** 
{agent_config.instructions}
"""

    def _create_tool_called_md(self, tool_name: str, args_text: str) -> str:
        """Create markdown content for tool execution."""
        return f"""**Tool:** {tool_name}

**Arguments:**
{args_text}
"""

    def _create_tool_output_md(self, output_display: str) -> str:
        """Create markdown content for tool output."""
        return f"""
{output_display}
"""

    def _create_message_output_md(self, message_text: str) -> str:
        """Create markdown content for final response."""
        return f"""{message_text}
"""