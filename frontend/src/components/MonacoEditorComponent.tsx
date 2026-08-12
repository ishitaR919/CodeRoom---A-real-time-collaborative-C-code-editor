'use client';

import React, { useRef, useEffect } from 'react';
import Editor, { OnMount } from '@monaco-editor/react';
import * as Y from 'yjs';
import { MonacoBinding } from 'y-monaco';

interface MonacoEditorProps {
  yText: Y.Text;
  defaultValue?: string;
}

export const MonacoEditorComponent: React.FC<MonacoEditorProps> = ({ yText, defaultValue }) => {
  const editorRef = useRef<any>(null);
  const bindingRef = useRef<MonacoBinding | null>(null);

  const handleEditorDidMount: OnMount = (editor, monaco) => {
    editorRef.current = editor;

    // Configure Monaco C++ settings
    monaco.editor.setTheme('vs-dark');

    const model = editor.getModel();
    if (model) {
      // Bind Yjs text to Monaco model
      const binding = new MonacoBinding(
        yText,
        model,
        new Set([editor]),
        null
      );
      bindingRef.current = binding;
    }
  };

  useEffect(() => {
    return () => {
      if (bindingRef.current) {
        bindingRef.current.destroy();
      }
    };
  }, []);

  return (
    <div className="h-full w-full relative bg-dark-900 overflow-hidden">
      <Editor
        height="100%"
        defaultLanguage="cpp"
        theme="vs-dark"
        options={{
          fontSize: 14,
          fontFamily: "'Fira Code', 'Cascadia Code', Consolas, monospace",
          minimap: { enabled: false },
          scrollBeyondLastLine: false,
          lineNumbers: 'on',
          roundedSelection: false,
          automaticLayout: true,
          tabSize: 4,
          padding: { top: 16, bottom: 16 },
        }}
        onMount={handleEditorDidMount}
      />
    </div>
  );
};
