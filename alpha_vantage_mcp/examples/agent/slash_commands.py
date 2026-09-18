"""Slash command handling for the Alpha Vantage Financial Agent application.

This module contains all slash command functionality separated from the main application logic.
"""

from agents import SQLiteSession
from agent_display_manager import AgentDisplayManager
from session_manager import list_sessions, get_session_database_path, generate_session_id, find_session_by_prefix


def handle_slash_command(command: str, display_manager: AgentDisplayManager) -> str:
    """Handle slash commands during interactive session.
    
    Args:
        command: The command string (including the slash)
        display_manager: Display manager for output
        
    Returns:
        str: 'clear' if /clear command was used, 'resume:{session_id}' if /resume was used,
             'handled' if other command was handled, or 'unhandled' if not a recognized command
    """
    command = command.strip()
    command_lower = command.lower()
    
    if command_lower == "/sessions":
        list_sessions()
        return 'handled'
    elif command_lower == "/clear":
        display_manager.display_info("🔄 Starting new session...")
        return 'clear'
    elif command_lower.startswith("/resume"):
        parts = command.split()
        if len(parts) == 1:
            display_manager.display_info("💡 Usage: /resume {session_id}")
            return 'handled'
        elif len(parts) == 2:
            session_prefix = parts[1]
            matching_session = find_session_by_prefix(session_prefix)
            
            if matching_session:
                display_manager.display_info(f"🔍 Found session: {matching_session}")
                return f'resume:{matching_session}'
            else:
                display_manager.display_error(f"No session found matching prefix: {session_prefix}")
                return 'handled'
        else:
            display_manager.display_error("Invalid /resume command. Usage: /resume {session_id}")
            return 'handled'
    elif command_lower == "/help":
        display_manager.display_info("💡 Available Commands:")
        display_manager.console.print("  /sessions - List all previous sessions")
        display_manager.console.print("  /resume {session_id} - Resume a specific session")
        display_manager.console.print("  /clear    - Start a new session")
        display_manager.console.print("  /help     - Show this help message")
        display_manager.console.print("  exit      - Exit the application")
        display_manager.console.print("  quit      - Exit the application")
        return 'handled'
    
    return 'unhandled'


async def process_slash_command(user_input: str, display_manager: AgentDisplayManager) -> tuple[str, SQLiteSession]:
    """Process slash commands and handle session management.
    
    Args:
        user_input: The user input string
        display_manager: Display manager for output
        
    Returns:
        tuple: (new_session_id, new_session) or (None, None) if command was handled but no session change
    """
    command_result = handle_slash_command(user_input, display_manager)
    
    if command_result == 'clear':
        # Generate new session ID and create new session
        new_session_id = generate_session_id()
        new_session = SQLiteSession(new_session_id, get_session_database_path())
        display_manager.display_welcome(new_session_id)
        return new_session_id, new_session
        
    elif command_result.startswith('resume:'):
        # Extract session ID and resume session
        resume_session_id = command_result.split(':', 1)[1]
        try:
            # Create new session with the specified ID
            new_session = SQLiteSession(resume_session_id, get_session_database_path())
            
            # Load and display conversation history
            items = await new_session.get_items()
            display_manager.display_session_items(items)
            
            return resume_session_id, new_session
        except Exception as e:
            display_manager.display_error(f"Failed to resume session {resume_session_id}: {str(e)}")
            return None, None
            
    elif command_result in ['handled', 'unhandled']:
        if command_result == 'unhandled':
            display_manager.display_error(f"Unknown command: {user_input}. Type /help for available commands.")
        return None, None
    
    return None, None