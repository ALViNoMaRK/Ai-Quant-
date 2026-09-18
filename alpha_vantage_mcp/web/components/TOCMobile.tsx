'use client';

import React, { useState, useRef, useEffect } from 'react';
import { TocItem } from '@/lib/toc';
import { ChevronDown, List } from 'lucide-react';
import { useTocNavigation } from '@/components/useTocNavigation';
import TocContent from '@/components/TocContent';

interface TOCMobileProps {
  tocItems: TocItem[];
}

export default function TOCMobile({ tocItems }: TOCMobileProps) {
  const [isExpanded, setIsExpanded] = useState(false);
  const { activeId, scrollToSection } = useTocNavigation(tocItems);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsExpanded(false);
      }
    }

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  if (tocItems.length === 0) {
    return null;
  }

  const handleItemClick = (id: string) => {
    scrollToSection(id);
    setIsExpanded(false); // Close mobile TOC after clicking
  };

  return (
    <div className="block sm:hidden">
      <div className="fixed bottom-4 right-2 z-50" ref={containerRef}>
        <button
          onClick={() => setIsExpanded(!isExpanded)}
className="rounded-full w-12 h-12 p-0 backdrop-blur-sm border-2 shadow-lg transition-colors flex items-center justify-center" style={{ backgroundColor: 'rgba(31, 31, 31, 0.9)', borderColor: 'rgba(74, 222, 128, 0.3)', color: 'white' }} onMouseEnter={(e) => e.currentTarget.style.backgroundColor = '#1f1f1f'} onMouseLeave={(e) => e.currentTarget.style.backgroundColor = 'rgba(31, 31, 31, 0.9)'}
        >
          <List className="w-5 h-5" />
        </button>
        
        {isExpanded && (
          <div className="absolute bottom-14 right-0 w-64 max-w-[calc(100vw-2rem)] max-h-[60vh] p-4 backdrop-blur-sm rounded-lg shadow-xl overflow-hidden flex flex-col" style={{ backgroundColor: 'rgba(31, 31, 31, 0.95)', border: '1px solid rgba(74, 222, 128, 0.3)' }}>
            <div className="flex items-center justify-between mb-3 flex-shrink-0">
              <h3 className="text-sm font-semibold" style={{ color: 'white' }}>
                Table of Contents
              </h3>
              <button
                onClick={() => setIsExpanded(false)}
className="transition-colors" style={{ color: '#9ca3af' }} onMouseEnter={(e) => e.currentTarget.style.color = 'white'} onMouseLeave={(e) => e.currentTarget.style.color = '#9ca3af'}
              >
                <ChevronDown className="w-4 h-4" />
              </button>
            </div>
            <div className="overflow-y-auto">
              <TocContent 
                tocItems={tocItems} 
                activeId={activeId} 
                onItemClick={handleItemClick} 
              />
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
