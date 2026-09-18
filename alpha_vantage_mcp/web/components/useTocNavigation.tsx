'use client';

import { useState, useEffect } from 'react';
import { TocItem } from '@/lib/toc';

export function useTocNavigation(tocItems: TocItem[]) {
  const [activeId, setActiveId] = useState<string>('');

  // Handle scrolling to section
  const scrollToSection = (id: string) => {
    console.log(`click ${id}`);
    setActiveId(id);
    const element = document.getElementById(id);
    const header = document.querySelector('header');
    
    if (element && header) {
      const headerHeight = header.offsetHeight;
      const elementPosition = element.offsetTop;
      const offsetPosition = elementPosition - headerHeight - 16; // 16px buffer for better spacing
      
      window.scrollTo({
        top: offsetPosition,
        behavior: 'instant'
      });
    }
  };

  // Track which section is currently visible
  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            setActiveId(entry.target.id);
          }
        });
      },
      { 
        rootMargin: '-20% 0px -80% 0px',
        threshold: 0
      }
    );

    // Observe all headings
    tocItems.forEach((item) => {
      const element = document.getElementById(item.id);
      if (element) observer.observe(element);
    });

    return () => observer.disconnect();
  }, [tocItems]);

  return {
    activeId,
    scrollToSection
  };
}
