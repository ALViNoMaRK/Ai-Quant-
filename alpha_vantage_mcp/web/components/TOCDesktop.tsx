'use client';

import React from 'react';
import { TocItem } from '@/lib/toc';
import { useTocNavigation } from '@/components/useTocNavigation';
import TocContent from '@/components/TocContent';

interface TOCDesktopProps {
  tocItems: TocItem[];
}

export default function TOCDesktop({ tocItems }: TOCDesktopProps) {
  const { activeId, scrollToSection } = useTocNavigation(tocItems);

  if (tocItems.length === 0) {
    return null;
  }

  return (
    <div className="hidden sm:block">
      <div 
        className="backdrop-blur-sm rounded-lg shadow-lg p-4 max-h-[60vh] overflow-y-auto scrollbar-hide" 
        style={{ 
          backgroundColor: 'rgba(31, 31, 31, 0.9)', 
          border: '1px solid rgba(74, 222, 128, 0.3)',
          scrollbarWidth: 'none',
          msOverflowStyle: 'none'
        }}
      >
        <h3 className="text-sm font-semibold mb-3" style={{ color: 'white' }}>
          Table of Contents
        </h3>
        <TocContent 
          tocItems={tocItems} 
          activeId={activeId} 
          onItemClick={scrollToSection} 
        />
      </div>
    </div>
  );
}
