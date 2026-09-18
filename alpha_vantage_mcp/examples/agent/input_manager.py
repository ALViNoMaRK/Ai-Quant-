"""Input manager for handling user input with multi-line support."""

from typing import Tuple, List
from prompt_toolkit import prompt
from prompt_toolkit.completion import Completer, Completion
from prompt_toolkit.formatted_text import HTML
from rich.console import Console


class SlashCommandCompleter(Completer):
    """Custom completer for slash commands."""
    
    def __init__(self, commands: List[str]):
        self.commands = commands
    
    def get_completions(self, document, complete_event):
        text = document.text_before_cursor
        
        # Only show completions if text starts with '/'
        if text.startswith('/'):
            command_part = text[1:]  # Remove the '/'
            
            for command in self.commands:
                if command.startswith(command_part):
                    # Show the completion without the '/' prefix since it's already typed
                    yield Completion(
                        command[len(command_part):],
                        display=HTML(f'<b>/{command}</b>'),
                        start_position=0
                    )


class InputManager:
    def __init__(self, console: Console, slash_commands: List[str] = None):
        self.console = console
        self.eof_count = 0
        self.ctrl_c_count = 0
        self.slash_commands = slash_commands or [
            'help', 'sessions', 'clear', 'resume', 'exit', 'quit'
        ]
        self.completer = SlashCommandCompleter(self.slash_commands)

    def get_input(self) -> Tuple[str, bool, int]:
        """Get user input with support for multi-line input using EOF flags.

        Multi-line input starts with <<EOF and ends with EOF. For example:
        <<EOF
        line 1
        line 2
        EOF

        Returns:
            Tuple[str, bool, int]: A tuple containing:
                - The user input, either single line or multiple lines joined with newlines
                - A boolean indicating if the input was multi-line (True) or single-line (False)
                - The number of lines in the input
        
        Raises:
            EOFError: When Ctrl-D is pressed, allowing the caller to handle exit
        """
        try:
            # Get input using regular prompt with completion
            text = prompt('> ', in_thread=True, completer=self.completer)
            text = text.rstrip()
            
            # Reset EOF and Ctrl-C counts when user provides input
            self.eof_count = 0
            self.ctrl_c_count = 0

            # Check for multi-line input start flag
            if text == "<<EOF":
                lines = []
                while True:
                    try:
                        line = prompt('', in_thread=True, completer=self.completer)
                        if line == "EOF":
                            break
                        lines.extend(line.split("\n"))
                    except EOFError:
                        # Handle Ctrl-D during multi-line input
                        raise EOFError()
                    except KeyboardInterrupt:
                        # Handle Ctrl-C during multi-line input
                        raise KeyboardInterrupt()
                return ("\n".join(lines), True, len(lines))

            lines = text.split("\n")
            return (text, False, len(lines))
        except KeyboardInterrupt:
            self.ctrl_c_count += 1
            if self.ctrl_c_count == 1:
                self.console.print("(Press Ctrl-C again to exit)", style="dim")
                return ("", False, 0)
            else:
                # Second Ctrl-C, exit
                raise KeyboardInterrupt()
        except EOFError:
            self.eof_count += 1
            if self.eof_count == 1:
                self.console.print("(Press Ctrl-D again to exit)", style="dim")
                return ("", False, 0)
            else:
                # Second Ctrl-D, exit
                raise EOFError()

    def is_exit_command(self, text: str) -> bool:
        """Check if the input is an exit command.

        Args:
            text: The input text to check

        Returns:
            bool: True if the input is an exit command, False otherwise
        """
        return text.lower() in ['exit', 'quit', '/exit', '/quit']