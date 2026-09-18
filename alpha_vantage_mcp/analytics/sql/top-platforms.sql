-- Simple query to answer: which platform is used most for MCP tool calls?
-- Returns platforms ranked by total tool usage
SELECT 
    platform,
    COUNT(*) as total_tool_calls
FROM mcp_analytics.mcp_logs 
WHERE method = 'tools/call'
GROUP BY platform 
ORDER BY total_tool_calls DESC;