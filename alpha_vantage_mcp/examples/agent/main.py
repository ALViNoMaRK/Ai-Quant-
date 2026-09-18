"""Minimal OpenAI Agents SDK example using local Alpha Vantage MCP server via stdio.

This is a simple example that demonstrates basic usage of the OpenAI Agents SDK
with the Alpha Vantage MCP server running locally via stdio transport.
"""

import asyncio
import click
import sys
import io
import contextlib
from agents import Agent, Runner, SQLiteSession
from dotenv import load_dotenv
from loguru import logger
from input_manager import InputManager
from agent_display_manager import AgentDisplayManager
from session_manager import get_session_database_path, generate_session_id, find_session_by_prefix
from slash_commands import process_slash_command
from mcp_server_manager import MCPServerManager

load_dotenv()

async def handle_stream_events(result_streaming, display_manager: AgentDisplayManager):
    """Handle streaming events from the agent."""
    async for event in result_streaming.stream_events():
        
        if event.type == "agent_updated_stream_event":
            agent_config = event.new_agent
            display_manager.display_agent_config(agent_config)
            
        elif event.type == "run_item_stream_event":
            if hasattr(event, 'item'):
                item = event.item
                
                if event.name == "tool_called":
                    if hasattr(item, 'raw_item') and hasattr(item.raw_item, 'name'):
                        tool_name = item.raw_item.name
                        arguments = getattr(item.raw_item, 'arguments', None)
                        call_id = getattr(item.raw_item, 'call_id', None)
                        display_manager.display_tool_execution(tool_name, arguments, call_id)
                        
                elif event.name == "tool_output":
                    if hasattr(item, 'output'):
                        output_str = str(item.output)
                        call_id = getattr(item, 'raw_item', {}).get('call_id', None) if hasattr(item, 'raw_item') else None
                        display_manager.display_tool_result(output_str, call_id)
                        
                elif event.name == "message_output_created":
                    if hasattr(item, 'raw_item') and hasattr(item.raw_item, 'content'):
                        display_manager.display_agent_response(item.raw_item.content)

async def main_agent(original_session_id=None, model='gpt-4.1-mini', verbose=False):
    """Run an interactive financial query session using the Alpha Vantage MCP server."""
    
    # Generate session ID if not provided
    if not original_session_id:
        session_id = generate_session_id()
    else:
        # If session_id was provided, try to match it as a prefix (like /resume command)
        matching_session = find_session_by_prefix(original_session_id)
        
        if matching_session:
            session_id = matching_session
            # We'll show the match info later in display logic
        else:
            # If no match found, keep the original session_id (might be a new session with that ID)
            session_id = original_session_id
    
    # Configure logger based on verbose flag
    if not verbose:
        logger.disable("mcp_server_manager")
    
    # Initialize MCP server manager (loads from mcp.json configuration)
    mcp_manager = MCPServerManager("mcp.json")
    
    # Initialize display manager and input manager
    display_manager = AgentDisplayManager()
    input_manager = InputManager(display_manager.console)
    
    # Create session for conversation memory
    session = SQLiteSession(session_id, get_session_database_path())
    
    async with mcp_manager:
        # Get initialized servers
        servers = mcp_manager.get_servers()
        
        # Create a simple agent
        agent = Agent(
            name="Alpha Vantage Financial Agent",
            instructions="You are a financial analyst. Use Alpha Vantage data to answer questions about stocks, forex, and other financial markets.",
            model=model,
            mcp_servers=servers
        )
        
        # Welcome message (only for new sessions)
        if not original_session_id:
            display_manager.display_welcome(session_id)
        else:
            # Show prefix match info if session ID was matched
            if original_session_id != session_id:
                display_manager.display_info(f"🔍 Found session: {session_id}")
            
            # Load and display conversation history for resumed session
            try:
                items = await session.get_items()
                if items:
                    display_manager.display_session_items(items)
                else:
                    display_manager.display_welcome(session_id)  # No history, show welcome
            except Exception as e:
                display_manager.display_error(f"Failed to load session history: {str(e)}")
                display_manager.display_welcome(session_id)  # Fallback to welcome
        
        # Interactive loop
        while True:
            try:
                # Get user input
                user_input, _, _ = input_manager.get_input()
                
                # Check for exit command (works for both regular and slash commands)
                if input_manager.is_exit_command(user_input):
                    display_manager.display_goodbye()
                    break
                
                if not user_input.strip():
                    continue
                
                # Check for slash commands
                if user_input.startswith('/'):
                    new_session_id, new_session = await process_slash_command(user_input, display_manager)
                    if new_session_id and new_session:
                        session_id = new_session_id
                        session = new_session
                    continue
                
                # Run the agent with user query and stream events
                result_streaming = Runner.run_streamed(agent, user_input, session=session)
                await handle_stream_events(result_streaming, display_manager)
                
            except EOFError:
                display_manager.display_goodbye()
                break
            except KeyboardInterrupt:
                display_manager.display_goodbye()
                break
            except Exception as e:
                display_manager.display_error(str(e))
                continue

@click.command()
@click.option('--session-id', default=None, help='Use specific session ID (defaults to new session)')
@click.option('--model', default='gpt-4.1-mini', help='OpenAI model to use')
@click.option('--verbose', '-v', is_flag=True, help='Enable verbose logging')
def main_cli(session_id, model, verbose):
    """Alpha Vantage Financial Agent with Alpha Vantage MCP server."""
    asyncio.run(main_agent(original_session_id=session_id, model=model, verbose=verbose))

if __name__ == "__main__":
    main_cli()