'use client'

import { useState, useMemo, useEffect, useRef } from 'react';
import Editor, { loader } from '@monaco-editor/react';
import React from 'react';
import * as ReactDOM from 'react-dom/client';
import * as Recharts from 'recharts';
import * as Babel from '@babel/standalone';
import useSWR from 'swr';

// Create a map of all available imports
const availableImports = {
  // React core
  React,
  // React hooks
  useState,
  useMemo, 
  useEffect,
  useRef,
  // Data fetching
  useSWR,
  // Recharts components
  ...Recharts,
};

export function ArtifactsView() {
  const STORAGE_KEY = 'artifactsView.code';
  const VIEW_MODE_KEY = 'artifactsView.viewMode';
  const [viewMode, setViewMode] = useState<'code' | 'preview' | 'split'>('split');
  const [isHydrated, setIsHydrated] = useState(false);
  const [codeType, setCodeType] = useState<'react' | 'html'>('react');
  
  const defaultCode = `// Welcome to the React Artifacts Editor!
// You can use import statements - they'll be automatically handled
// Available imports:
// - React hooks: useState, useEffect, useMemo, useRef
// - React Router: useNavigate, useLocation, useParams
// - Data fetching: useSWR
// - All Recharts components and utilities (LineChart, BarChart, PieChart, AreaChart, RadarChart, 
//   ScatterChart, ComposedChart, Treemap, Sankey, Funnel, XAxis, YAxis, ZAxis, CartesianGrid, 
//   Tooltip, Legend, Cell, LabelList, ReferenceLine, ReferenceArea, Brush, etc.)
// Export your component as default at the end

// Import Recharts components
import { ResponsiveContainer, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, PieChart, Pie, Cell } from 'recharts';

const MyComponent = () => {
  const [count, setCount] = useState(0);
  const [text, setText] = useState('Hello, World!');
  
  const data = [
    { name: 'Jan', value: 400 },
    { name: 'Feb', value: 300 },
    { name: 'Mar', value: 600 },
    { name: 'Apr', value: 800 },
    { name: 'May', value: 500 }
  ];
  
  const colors = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#8884D8'];
  
  return (
    <div className="p-8 max-w-4xl mx-auto">
      <h1 className="text-3xl font-bold mb-6 text-gray-800">
        Interactive React Component
      </h1>
      
      <div className="space-y-6">
        {/* Interactive Counter */}
        <div className="bg-white p-6 rounded-lg shadow-md">
          <h2 className="text-xl font-semibold mb-4">Counter Example</h2>
          <div className="flex items-center gap-4">
            <button 
              onClick={() => setCount(count - 1)}
              className="px-4 py-2 bg-red-500 text-white rounded hover:bg-red-600"
            >
              -
            </button>
            <span className="text-2xl font-bold">{count}</span>
            <button 
              onClick={() => setCount(count + 1)}
              className="px-4 py-2 bg-green-500 text-white rounded hover:bg-green-600"
            >
              +
            </button>
          </div>
        </div>
        
        {/* Text Input */}
        <div className="bg-white p-6 rounded-lg shadow-md">
          <h2 className="text-xl font-semibold mb-4">Text Input</h2>
          <input
            type="text"
            value={text}
            onChange={(e) => setText(e.target.value)}
            className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            placeholder="Type something..."
          />
          <p className="mt-2 text-gray-600">You typed: {text}</p>
        </div>
        
        {/* Chart Example */}
        <div className="bg-white p-6 rounded-lg shadow-md">
          <h2 className="text-xl font-semibold mb-4">Chart Visualization</h2>
          <ResponsiveContainer width="100%" height={200}>
            <BarChart data={data}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="name" />
              <YAxis />
              <Tooltip />
              <Bar dataKey="value" fill="#8884d8" />
            </BarChart>
          </ResponsiveContainer>
        </div>
        
        {/* Pie Chart */}
        <div className="bg-white p-6 rounded-lg shadow-md">
          <h2 className="text-xl font-semibold mb-4">Pie Chart</h2>
          <ResponsiveContainer width="100%" height={300}>
            <PieChart>
              <Pie
                data={data}
                cx="50%"
                cy="50%"
                labelLine={false}
                label={({name, value}) => \`\${name}: \${value}\`}
                outerRadius={80}
                fill="#8884d8"
                dataKey="value"
              >
                {data.map((entry, index) => (
                  <Cell key={\`cell-\${index}\`} fill={colors[index % colors.length]} />
                ))}
              </Pie>
              <Tooltip />
            </PieChart>
          </ResponsiveContainer>
        </div>
      </div>
    </div>
  );
};

export default MyComponent;`;

  const [code, setCode] = useState(defaultCode);

  const [error, setError] = useState<string | null>(null);
  const previewRef = useRef<HTMLDivElement>(null);
  const rootRef = useRef<ReactDOM.Root | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Hydration effect - load from localStorage after component mounts
  useEffect(() => {
    setIsHydrated(true);
    try {
      const savedMode = localStorage.getItem(VIEW_MODE_KEY);
      if (savedMode === 'code' || savedMode === 'preview' || savedMode === 'split') {
        setViewMode(savedMode);
      }
      
      const savedCode = localStorage.getItem(STORAGE_KEY);
      if (savedCode) {
        setCode(savedCode);
      }
    } catch (e) {
      console.error('Failed to load from localStorage:', e);
    }
  }, []);

  // Detect code type when code changes
  useEffect(() => {
    const trimmedCode = code.trim().toLowerCase();
    if (trimmedCode.startsWith('<!doctype html') || trimmedCode.startsWith('<html')) {
      setCodeType('html');
    } else {
      setCodeType('react');
    }
  }, [code]);

  // Save code to localStorage whenever it changes (only after hydration)
  useEffect(() => {
    if (!isHydrated) return;
    try {
      localStorage.setItem(STORAGE_KEY, code);
    } catch (e) {
      console.error('Failed to save code to localStorage:', e);
    }
  }, [code, isHydrated]);

  // Extract React compilation logic into a reusable function
  const compileReactCode = (sourceCode: string): { success: boolean; component?: any; error?: string } => {
    try {
      // Parse import statements to determine which dependencies are needed
      const importRegex = /import\s+(?:{([^}]+)}|(\w+)|\*\s+as\s+(\w+))\s+from\s+['"]([^'"]+)['"]/gm;
      const usedImports = new Set<string>();
      
      let match;
      while ((match = importRegex.exec(sourceCode)) !== null) {
        if (match[1]) {
          // Named imports like { useState, useEffect }
          const namedImports = match[1].split(',').map(s => s.trim());
          namedImports.forEach(imp => {
            const cleanName = imp.split(' as ')[0].trim();
            usedImports.add(cleanName);
          });
        } else if (match[2]) {
          // Default import like React
          usedImports.add(match[2]);
        }
      }

      // Also check for identifiers used in the code but not explicitly imported
      const codeWithoutComments = sourceCode
        .replace(/\/\*[\s\S]*?\*\//g, '') // Remove block comments
        .replace(/\/\/.*/g, ''); // Remove line comments
      
      // Check for commonly used but not imported identifiers
      Object.keys(availableImports).forEach(key => {
        const regex = new RegExp(`\\b${key}\\b`, 'g');
        if (regex.test(codeWithoutComments)) {
          usedImports.add(key);
        }
      });

      // Remove import statements since all dependencies are provided globally
      const codeWithoutImports = sourceCode
        .replace(/^import\s+.*?from\s+['"].*?['"];?\s*$/gm, '')
        .replace(/^import\s+['"].*?['"];?\s*$/gm, '')
        .trim();

      // Transform JSX to JavaScript
      let transformedCode: string;
      try {
        const transformResult = Babel.transform(codeWithoutImports, {
          presets: ['react', 'typescript'],
          plugins: [],
          parserOpts: {
            // Enable TypeScript parsing - handles arrow function parameters without types
            plugins: ['jsx', 'typescript'],
            errorRecovery: true
          }
        });
        
        if (!transformResult || !transformResult.code) {
          throw new Error('Failed to transform code');
        }
        
        transformedCode = transformResult.code;
      } catch (babelError: any) {
        // Fallback: try without typescript preset if it fails
        try {
          const fallbackResult = Babel.transform(codeWithoutImports, {
            presets: ['react'],
            plugins: [],
            parserOpts: {
              plugins: ['jsx'],
              errorRecovery: true
            }
          });
          
          if (!fallbackResult || !fallbackResult.code) {
            throw babelError;
          }
          
          transformedCode = fallbackResult.code;
        } catch (finalError) {
          throw babelError; // Throw original error if fallback also fails
        }
      }

      // Replace export default with a variable assignment
      const modifiedCode = transformedCode.replace(/export\s+default\s+(\w+);?/g, 'Component = $1;');

      // Build parameters and arguments dynamically
      const params: string[] = [];
      const args: any[] = [];
      
      // Always include React
      params.push('React');
      args.push(React);
      
      // Add all detected imports
      usedImports.forEach(importName => {
        if (importName !== 'React' && availableImports[importName as keyof typeof availableImports]) {
          params.push(importName);
          args.push(availableImports[importName as keyof typeof availableImports]);
        }
      });

      // Create function body
      const functionBody = `
        let Component = null;
        try {
          ${modifiedCode}
          return Component;
        } catch (error) {
          throw error;
        }
      `;

      // Create a function that returns the component
      const func = new Function(...params, functionBody);
      
      // Execute the function with all the dependencies and get the component
      const Component = func(...args);

      return { success: true, component: Component };
    } catch (err: any) {
      return { success: false, error: err.toString() };
    }
  };

  // Transform and render the code
  useEffect(() => {
    if (!previewRef.current || (viewMode !== 'preview' && viewMode !== 'split')) return;

    // Clean up previous root if it exists - use setTimeout to avoid sync unmount warning
    if (rootRef.current) {
      const currentRoot = rootRef.current;
      rootRef.current = null;
      setTimeout(() => {
        currentRoot.unmount();
      }, 0);
    }

    // Handle HTML code
    if (codeType === 'html') {
      try {
        // Create an iframe for HTML content
        const iframe = document.createElement('iframe');
        iframe.className = 'w-full h-full border-0';
        iframe.sandbox = 'allow-scripts allow-same-origin allow-forms allow-modals';
        
        previewRef.current.innerHTML = '';
        previewRef.current.appendChild(iframe);
        
        // Write HTML content to iframe
        const iframeDoc = iframe.contentDocument || iframe.contentWindow?.document;
        if (iframeDoc) {
          iframeDoc.open();
          iframeDoc.write(code);
          iframeDoc.close();
        }
        
        setError(null);
      } catch (err: any) {
        setError(err.toString());
        if (previewRef.current) {
          previewRef.current.innerHTML = `
            <div class="p-4 bg-red-50 border border-red-200 rounded-lg m-4">
              <h3 class="text-red-800 font-semibold mb-2">HTML Error</h3>
              <pre class="text-red-600 text-sm whitespace-pre-wrap">${err.toString()}</pre>
            </div>
          `;
        }
      }
      return;
    }

    // Handle React/JSX code
    const result = compileReactCode(code);
    
    if (result.success && result.component) {
      try {
        // Clear the preview container
        if (previewRef.current) {
          previewRef.current.innerHTML = '';
        }
        
        // Add Tailwind CSS script to the preview container
        const tailwindScript = document.createElement('script');
        tailwindScript.src = 'https://cdn.tailwindcss.com';
        tailwindScript.onload = () => {
          // Create a container for the rendered component after Tailwind loads
          const container = document.createElement('div');
          container.className = 'w-full h-full';
          if (previewRef.current) {
            previewRef.current.appendChild(container);
          }

          // Create root and render
          rootRef.current = ReactDOM.createRoot(container);
          
          // Render the component
          if (rootRef.current) {
            rootRef.current.render(React.createElement(result.component));
          }
        };
        
        // Append the script to head if not already present
        if (!document.querySelector('script[src="https://cdn.tailwindcss.com"]')) {
          document.head.appendChild(tailwindScript);
        } else {
          // If Tailwind is already loaded, render immediately
          const container = document.createElement('div');
          container.className = 'w-full h-full';
          if (previewRef.current) {
            previewRef.current.appendChild(container);
          }

          // Create root and render
          rootRef.current = ReactDOM.createRoot(container);
          
          // Render the component
          if (rootRef.current) {
            rootRef.current.render(React.createElement(result.component));
          }
        }

        setError(null);
      } catch (err: any) {
        setError(err.toString());
        // Display runtime error with Tailwind styles
        if (previewRef.current) {
          // Ensure Tailwind is loaded for error display
          const renderError = () => {
            const container = document.createElement('div');
            container.className = 'w-full h-full';
            if (previewRef.current) {
              previewRef.current.innerHTML = '';
              previewRef.current.appendChild(container);
            }
            
            rootRef.current = ReactDOM.createRoot(container);
            rootRef.current.render(
              <div className="p-4 bg-red-50 border border-red-200 rounded-lg m-4">
                <h3 className="text-red-800 font-semibold mb-2">Runtime Error</h3>
                <pre className="text-red-600 text-sm whitespace-pre-wrap">{err.toString()}</pre>
              </div>
            );
          };
          
          if (!document.querySelector('script[src="https://cdn.tailwindcss.com"]')) {
            const tailwindScript = document.createElement('script');
            tailwindScript.src = 'https://cdn.tailwindcss.com';
            tailwindScript.onload = renderError;
            document.head.appendChild(tailwindScript);
          } else {
            renderError();
          }
        }
      }
    } else {
      setError(result.error || 'Unknown error');
      
      // Display compilation error in preview with Tailwind styles
      if (previewRef.current) {
        const renderError = () => {
          const container = document.createElement('div');
          container.className = 'w-full h-full';
          if (previewRef.current) {
            previewRef.current.innerHTML = '';
            previewRef.current.appendChild(container);
          }
          
          rootRef.current = ReactDOM.createRoot(container);
          rootRef.current.render(
            <div className="p-4 bg-red-50 border border-red-200 rounded-lg m-4">
              <h3 className="text-red-800 font-semibold mb-2">Compilation Error</h3>
              <pre className="text-red-600 text-sm whitespace-pre-wrap">{result.error}</pre>
              <div className="mt-4 text-sm text-gray-600">
                <p className="font-semibold">Tips:</p>
                <ul className="list-disc list-inside mt-1">
                  <li>Import statements are automatically handled</li>
                  <li>Export your component as default (export default MyComponent)</li>
                  <li>Use proper JSX syntax</li>
                  <li>Available: React hooks, React Router, useSWR, all Recharts components</li>
                  <li>Tailwind CSS classes are fully supported</li>
                </ul>
              </div>
            </div>
          );
        };
        
        if (!document.querySelector('script[src="https://cdn.tailwindcss.com"]')) {
          const tailwindScript = document.createElement('script');
          tailwindScript.src = 'https://cdn.tailwindcss.com';
          tailwindScript.onload = renderError;
          document.head.appendChild(tailwindScript);
        } else {
          renderError();
        }
      }
    }
  }, [code, viewMode, codeType]);

  const handleEditorChange = (value: string | undefined) => {
    if (value !== undefined) {
      setCode(value);
    }
  };

  const handleFileUpload = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file && (file.name.endsWith('.tsx') || file.name.endsWith('.html') || file.name.endsWith('.htm'))) {
      const reader = new FileReader();
      reader.onload = (e) => {
        const content = e.target?.result as string;
        setCode(content);
      };
      reader.readAsText(file);
    } else if (file) {
      alert('Please select a .tsx or .html file');
    }
    // Reset input value to allow uploading the same file again
    if (event.target) {
      event.target.value = '';
    }
  };

  const handleUploadClick = () => {
    fileInputRef.current?.click();
  };

  const handleResetToDefault = () => {
    if (confirm('Are you sure you want to reset to the default code? This will overwrite your current code.')) {
      setCode(defaultCode);
    }
  };

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      if (rootRef.current) {
        const currentRoot = rootRef.current;
        setTimeout(() => {
          currentRoot.unmount();
        }, 0);
      }
    };
  }, []);

  // Save view mode to localStorage whenever it changes (only after hydration)
  useEffect(() => {
    if (!isHydrated) return;
    try {
      localStorage.setItem(VIEW_MODE_KEY, viewMode);
    } catch (e) {
      console.error('Failed to save view mode to localStorage:', e);
    }
  }, [viewMode, isHydrated]);

  // Keyboard shortcut for toggling view
  useEffect(() => {
    const handleKeyPress = (e: KeyboardEvent) => {
      // Cmd/Ctrl + E to cycle through code, split, and preview
      if ((e.metaKey || e.ctrlKey) && e.key === 'e') {
        e.preventDefault();
        setViewMode(prev => {
          if (prev === 'code') return 'split';
          if (prev === 'split') return 'preview';
          return 'code';
        });
      }
    };

    window.addEventListener('keydown', handleKeyPress);
    return () => window.removeEventListener('keydown', handleKeyPress);
  }, []);

  return (
    <div className="flex flex-col h-screen" style={{ backgroundColor: 'rgb(45, 45, 45)' }}>
      {/* Header with toggle buttons */}
      <div className="h-12 border-b flex items-center justify-between px-4 shadow-sm" style={{ backgroundColor: '#1f1f1f', borderColor: 'rgba(66, 220, 163, 0.3)' }}>
        <div className="flex items-center gap-3">
          {/* Toggle Button Group */}
          <div className="flex rounded-lg p-1" style={{ backgroundColor: 'rgba(66, 220, 163, 0.1)' }}>
            <button
              onClick={() => setViewMode('code')}
              className={`px-4 py-1.5 text-sm font-medium rounded-md transition-all ${
                viewMode === 'code'
                  ? 'shadow-sm'
                  : 'hover:bg-opacity-20'
              }`}
              style={{
                backgroundColor: viewMode === 'code' ? '#42DCA3' : 'transparent',
                color: viewMode === 'code' ? '#1f1f1f' : '#42DCA3'
              }}
            >
              <span className="flex items-center gap-2">
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4" />
                </svg>
                Code
              </span>
            </button>
            <button
              onClick={() => setViewMode('split')}
              className={`px-4 py-1.5 text-sm font-medium rounded-md transition-all ${
                viewMode === 'split'
                  ? 'shadow-sm'
                  : 'hover:bg-opacity-20'
              }`}
              style={{
                backgroundColor: viewMode === 'split' ? '#42DCA3' : 'transparent',
                color: viewMode === 'split' ? '#1f1f1f' : '#42DCA3'
              }}
            >
              <span className="flex items-center gap-2">
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 3H5a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-6m0 0V5a2 2 0 00-2-2h-6m8 8h-8m8 0L12 12" />
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 3v18" />
                </svg>
                Split
              </span>
            </button>
            <button
              onClick={() => setViewMode('preview')}
              className={`px-4 py-1.5 text-sm font-medium rounded-md transition-all ${
                viewMode === 'preview'
                  ? 'shadow-sm'
                  : 'hover:bg-opacity-20'
              }`}
              style={{
                backgroundColor: viewMode === 'preview' ? '#42DCA3' : 'transparent',
                color: viewMode === 'preview' ? '#1f1f1f' : '#42DCA3'
              }}
            >
              <span className="flex items-center gap-2">
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                </svg>
                Preview
              </span>
            </button>
          </div>

          {/* Action buttons - show when in code or split view */}
          {(viewMode === 'code' || viewMode === 'split') && (
            <>
              <button
                onClick={handleUploadClick}
                className="px-3 py-1.5 text-sm rounded-md transition-colors"
                style={{ backgroundColor: '#42DCA3', color: '#1f1f1f' }}
                onMouseEnter={(e) => e.currentTarget.style.backgroundColor = 'rgba(66, 220, 163, 0.9)'}
                onMouseLeave={(e) => e.currentTarget.style.backgroundColor = '#42DCA3'}
              >
                Upload File
              </button>
              <button
                onClick={handleResetToDefault}
                className="px-3 py-1.5 text-sm rounded-md transition-colors"
                style={{ backgroundColor: 'rgba(255, 255, 255, 0.1)', color: '#42DCA3', border: '1px solid #42DCA3' }}
                onMouseEnter={(e) => e.currentTarget.style.backgroundColor = 'rgba(66, 220, 163, 0.1)'}
                onMouseLeave={(e) => e.currentTarget.style.backgroundColor = 'rgba(255, 255, 255, 0.1)'}
              >
                Reset to Default
              </button>
              <input
                ref={fileInputRef}
                type="file"
                accept=".tsx,.html,.htm"
                onChange={handleFileUpload}
                className="hidden"
              />
            </>
          )}
        </div>
        
        <div className="flex items-center gap-3">
          {error && (viewMode === 'preview' || viewMode === 'split') && (
            <span className="text-xs font-medium" style={{ color: '#ff6b6b' }}>Error in code</span>
          )}
          <div className="text-xs" style={{ color: '#42DCA3' }}>
            {viewMode === 'code' ? `${codeType === 'html' ? 'HTML' : 'JSX'} • Auto-saved` : 
             viewMode === 'split' ? `Split View • ${codeType === 'html' ? 'HTML' : 'JSX'}` : 
             'Live Preview'}
          </div>
          <div className="text-xs" style={{ color: 'rgba(255, 255, 255, 0.6)' }}>
            Press ⌘E to cycle views
          </div>
        </div>
      </div>

      {/* Main Content Area */}
      <div className="flex-1 min-h-0 bg-white">
        {/* Split View */}
        {viewMode === 'split' && (
          <div className="flex h-full">
            {/* Code Editor Panel (Left) */}
            <div className="w-1/2 h-full border-r border-gray-200">
              <Editor
                height="100%"
                defaultLanguage={codeType === 'html' ? 'html' : 'javascript'}
                language={codeType === 'html' ? 'html' : 'javascript'}
                value={code}
                onChange={handleEditorChange}
                options={{
                  minimap: { enabled: false },
                  fontSize: 14,
                  wordWrap: 'on',
                  automaticLayout: true,
                  scrollBeyondLastLine: false,
                  theme: 'vs-light',
                  tabSize: 2,
                }}
              />
            </div>
            {/* Preview Panel (Right) */}
            <div className="w-1/2 h-full overflow-auto bg-gray-50">
              <div ref={previewRef} className="w-full h-full" />
            </div>
          </div>
        )}

        {/* Code Only View */}
        {viewMode === 'code' && (
          <Editor
            height="100%"
            defaultLanguage={codeType === 'html' ? 'html' : 'javascript'}
            language={codeType === 'html' ? 'html' : 'javascript'}
            value={code}
            onChange={handleEditorChange}
            options={{
              minimap: { enabled: false },
              fontSize: 14,
              wordWrap: 'on',
              automaticLayout: true,
              scrollBeyondLastLine: false,
              theme: 'vs-light',
              tabSize: 2,
            }}
          />
        )}

        {/* Preview Only View */}
        {viewMode === 'preview' && (
          <div className="w-full h-full overflow-auto bg-gray-50">
            <div ref={previewRef} className="w-full h-full" />
          </div>
        )}
      </div>
    </div>
  );
}