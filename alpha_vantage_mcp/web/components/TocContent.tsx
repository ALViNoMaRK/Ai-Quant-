'use client';

import React, { useState } from 'react';
import { ChevronDown } from 'lucide-react';
import { TocItem } from '@/lib/toc';

interface TocContentProps {
  tocItems: TocItem[];
  activeId: string;
  onItemClick: (id: string) => void;
}

export default function TocContent({ tocItems, activeId, onItemClick }: TocContentProps) {
  const [expandedSections, setExpandedSections] = useState<Set<string>>(new Set());

  // Group items by their parent h2 sections
  const groupedItems = tocItems.reduce((acc, item, index) => {
    if (item.level === 2) {
      acc.push({ h2: item, h3s: [], h2Index: index });
    } else if (item.level === 3 && acc.length > 0) {
      acc[acc.length - 1].h3s.push(item);
    }
    return acc;
  }, [] as Array<{ h2: TocItem; h3s: TocItem[]; h2Index: number }>);

  const toggleSection = (h2Id: string) => {
    if (expandedSections.has(h2Id)) {
      // If clicking the currently expanded section, collapse it
      setExpandedSections(new Set());
    } else {
      // If clicking a different section, expand only that one
      setExpandedSections(new Set([h2Id]));
    }
  };

  return (
    <nav className="space-y-1">
      {groupedItems.map(({ h2, h3s, h2Index }) => (
        <div key={h2.id || `group-${h2Index}`}>
          <button
            onClick={() => {
              onItemClick(h2.id);
              if (h3s.length > 0) {
                toggleSection(h2.id);
              }
            }}
            className="flex items-center justify-between w-full text-left py-2 text-sm font-medium rounded-md transition-colors cursor-pointer"
            style={{
              backgroundColor: activeId === h2.id ? 'rgba(74, 222, 128, 0.2)' : 'transparent',
              color: activeId === h2.id ? '#42DCA3' : '#d1d5db',
              paddingLeft: '0.75rem',
              paddingRight: '0.75rem'
            }}
            onMouseEnter={(e) => {
              if (activeId !== h2.id) {
                e.currentTarget.style.backgroundColor = '#1f1f1f';
                e.currentTarget.style.color = 'white';
              }
            }}
            onMouseLeave={(e) => {
              if (activeId !== h2.id) {
                e.currentTarget.style.backgroundColor = 'transparent';
                e.currentTarget.style.color = '#d1d5db';
              }
            }}
          >
            <span>{h2.text}</span>
            {h3s.length > 0 && (
              <ChevronDown 
                className={`w-4 h-4 ml-2 transition-transform ${
                  expandedSections.has(h2.id) ? 'rotate-180' : ''
                }`}
              />
            )}
          </button>
          {h3s.length > 0 && expandedSections.has(h2.id) && (
            <div className="ml-4">
              {h3s.map((h3Item, h3Index) => (
                <button
                  key={h3Item.id || `h3-${h2Index}-${h3Index}`}
                  onClick={() => onItemClick(h3Item.id)}
                  className="block w-full text-left py-2 text-sm rounded-md transition-colors cursor-pointer"
                  style={{
                    backgroundColor: activeId === h3Item.id ? 'rgba(74, 222, 128, 0.2)' : 'transparent',
                    color: activeId === h3Item.id ? '#42DCA3' : '#9ca3af',
                    paddingLeft: '0.75rem',
                    paddingRight: '0.75rem'
                  }}
                  onMouseEnter={(e) => {
                    if (activeId !== h3Item.id) {
                      e.currentTarget.style.backgroundColor = '#1f1f1f';
                      e.currentTarget.style.color = 'white';
                    }
                  }}
                  onMouseLeave={(e) => {
                    if (activeId !== h3Item.id) {
                      e.currentTarget.style.backgroundColor = 'transparent';
                      e.currentTarget.style.color = '#9ca3af';
                    }
                  }}
                >
                  {h3Item.text}
                </button>
              ))}
            </div>
          )}
        </div>
      ))}
    </nav>
  );
}
