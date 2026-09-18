-- Top Alpha Vantage MCP tools by usage
-- Shows which tools are called most frequently
SELECT 
    tool_name,
    COUNT(*) as total_calls
FROM mcp_analytics.mcp_logs 
WHERE method = 'tools/call'
GROUP BY tool_name 
ORDER BY total_calls DESC;