"""Session management utilities for the Alpha Vantage Financial Agent."""

import os
import sqlite3
import uuid
import click


def get_session_database_path():
    """Get the path to the session database."""
    return "sessions.db"


def generate_session_id():
    """Generate a unique session ID based on timestamp."""
    return uuid.uuid4().hex[:6]


def get_sessions_list():
    """Get list of all previous agent sessions as dictionaries."""
    # Use the correct database path
    db_path = get_session_database_path()
    
    if not os.path.exists(db_path):
        return []
    
    sessions_list = []
    
    try:
        # Connect to session database
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        
        # Query the agent_sessions table
        cursor.execute("SELECT DISTINCT session_id, created_at, updated_at FROM agent_sessions ORDER BY updated_at DESC;")
        sessions = cursor.fetchall()
        
        for session_id, created_at, updated_at in sessions:
            # Get message count for this session from agent_messages table (only user messages)
            cursor.execute("SELECT COUNT(*) FROM agent_messages WHERE session_id = ? AND JSON_EXTRACT(message_data, '$.role') = 'user';", (session_id,))
            message_count = cursor.fetchone()[0]
            
            # Get first user message from this session
            cursor.execute("""
                SELECT message_data FROM agent_messages 
                WHERE session_id = ? AND JSON_EXTRACT(message_data, '$.role') = 'user'
                ORDER BY created_at ASC 
                LIMIT 1;
            """, (session_id,))
            first_message_result = cursor.fetchone()
            
            first_message = ""
            if first_message_result:
                import json
                try:
                    message_data = json.loads(first_message_result[0])
                    first_message = message_data.get('content', '')[:100] + ('...' if len(message_data.get('content', '')) > 100 else '')
                except json.JSONDecodeError:
                    first_message = "Unable to parse message"
            else:
                first_message = "No user messages"
            
            sessions_list.append({
                'session_id': session_id,
                'message_count': message_count,
                'first_message': first_message,
                'created_at': created_at,
                'updated_at': updated_at
            })
        
        conn.close()
        
    except Exception as e:
        click.echo(f"Error reading session database: {e}")
        return []
    
    return sessions_list


def find_session_by_prefix(session_prefix: str) -> str:
    """Find first session that starts with the given prefix.
    
    Args:
        session_prefix: The prefix to match against session IDs
        
    Returns:
        str: The full session ID if found, None otherwise
    """
    sessions = get_sessions_list()
    for session in sessions:
        if session['session_id'].startswith(session_prefix):
            return session['session_id']
    return None


def list_sessions():
    """List all previous agent sessions."""
    from agent_display_manager import AgentDisplayManager
    
    sessions = get_sessions_list()
    
    if not sessions:
        click.echo("No previous sessions found.")
        return
    
    display_manager = AgentDisplayManager()
    display_manager.display_sessions_table(sessions)