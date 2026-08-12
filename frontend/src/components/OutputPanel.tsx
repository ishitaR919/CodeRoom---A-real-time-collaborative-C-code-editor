import React from 'react';
import { ExecutionResult } from '../types';
import { Terminal, Clock, CheckCircle2, AlertTriangle, XCircle, Loader2 } from 'lucide-react';

interface OutputPanelProps {
  result: ExecutionResult | null;
  isExecuting: boolean;
}

export const OutputPanel: React.FC<OutputPanelProps> = ({ result, isExecuting }) => {
  const getStatusBadge = () => {
    if (isExecuting) {
      return (
        <span className="flex items-center space-x-1.5 text-xs bg-yellow-500/20 text-yellow-300 border border-yellow-500/30 px-2.5 py-1 rounded-full animate-pulse">
          <Loader2 className="w-3.5 h-3.5 animate-spin" />
          <span>Running...</span>
        </span>
      );
    }

    if (!result) return null;

    switch (result.status) {
      case 'SUCCESS':
        return (
          <span className="flex items-center space-x-1.5 text-xs bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 px-2.5 py-1 rounded-full">
            <CheckCircle2 className="w-3.5 h-3.5" />
            <span>Success</span>
          </span>
        );
      case 'COMPILATION_ERROR':
        return (
          <span className="flex items-center space-x-1.5 text-xs bg-amber-500/20 text-amber-400 border border-amber-500/30 px-2.5 py-1 rounded-full">
            <AlertTriangle className="w-3.5 h-3.5" />
            <span>Compilation Error</span>
          </span>
        );
      case 'RUNTIME_ERROR':
        return (
          <span className="flex items-center space-x-1.5 text-xs bg-red-500/20 text-red-400 border border-red-500/30 px-2.5 py-1 rounded-full">
            <XCircle className="w-3.5 h-3.5" />
            <span>Runtime Error</span>
          </span>
        );
      case 'TIMEOUT':
        return (
          <span className="flex items-center space-x-1.5 text-xs bg-purple-500/20 text-purple-400 border border-purple-500/30 px-2.5 py-1 rounded-full">
            <Clock className="w-3.5 h-3.5" />
            <span>Execution Timed Out</span>
          </span>
        );
      default:
        return (
          <span className="flex items-center space-x-1.5 text-xs bg-red-500/20 text-red-400 border border-red-500/30 px-2.5 py-1 rounded-full">
            <XCircle className="w-3.5 h-3.5" />
            <span>System Error</span>
          </span>
        );
    }
  };

  return (
    <div className="h-full bg-dark-900 border-t border-dark-700 flex flex-col font-mono text-sm">
      {/* Panel Header */}
      <div className="bg-dark-800 px-4 py-2 border-b border-dark-700 flex items-center justify-between">
        <div className="flex items-center space-x-2 text-gray-300">
          <Terminal className="w-4 h-4 text-blue-400" />
          <span className="font-semibold uppercase tracking-wider text-xs">Output</span>
        </div>
        <div className="flex items-center space-x-3">
          {result && result.executionTimeMs > 0 && (
            <span className="text-xs text-gray-400 flex items-center space-x-1">
              <Clock className="w-3 h-3 text-gray-500" />
              <span>Execution time: {result.executionTimeMs} ms</span>
            </span>
          )}
          {getStatusBadge()}
        </div>
      </div>

      {/* Panel Body */}
      <div className="flex-1 p-4 overflow-auto bg-dark-900 text-gray-200">
        {isExecuting ? (
          <div className="flex items-center space-x-3 text-yellow-400 py-2">
            <Loader2 className="w-4 h-4 animate-spin" />
            <span>Compiling and executing C++ program inside Docker sandbox...</span>
          </div>
        ) : !result ? (
          <div className="text-gray-500 italic py-2">
            Click <strong className="text-blue-400">[ RUN ]</strong> to compile and execute main.cpp
          </div>
        ) : (
          <div className="space-y-3">
            {/* Standard Output */}
            {result.output && (
              <div className="space-y-1">
                <div className="text-xs text-gray-500 uppercase tracking-wider">Standard Output:</div>
                <pre className="bg-dark-800 p-3 rounded border border-dark-700 text-emerald-300 whitespace-pre-wrap font-mono">
                  {result.output}
                </pre>
              </div>
            )}

            {/* Error Output */}
            {result.errorOutput && (
              <div className="space-y-1">
                <div className="text-xs text-amber-500 uppercase tracking-wider">Compiler / Error Output:</div>
                <pre className="bg-dark-800 p-3 rounded border border-amber-950/50 text-amber-300 whitespace-pre-wrap font-mono">
                  {result.errorOutput}
                </pre>
              </div>
            )}

            {!result.output && !result.errorOutput && result.status === 'SUCCESS' && (
              <div className="text-gray-400 italic">Program executed successfully with no output.</div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};
