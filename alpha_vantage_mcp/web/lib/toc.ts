export interface TocItem {
  id: string;
  text: string;
  level: 2 | 3;
}

export function generateSlug(text: string): string {
  const slug = text
    .toLowerCase()
    // Keep Unicode word characters (including Chinese), spaces, and hyphens
    // Remove only punctuation and special symbols, not Unicode letters/numbers
    .replace(/[^\p{L}\p{N}\s-]/gu, '') 
    .replace(/\s+/g, '-') // Replace spaces with hyphens
    .replace(/-+/g, '-') // Replace multiple hyphens with single
    .trim()
    .replace(/^-|-$/g, ''); // Remove leading/trailing hyphens
  
  // Return fallback if slug is empty
  return slug || 'heading';
}

export function extractTocFromMarkdown(content: string): TocItem[] {
  const lines = content.split('\n');
  const tocItems: TocItem[] = [];
  const usedIds = new Set<string>();
  let headingCounter = 1;

  // Helper function to ensure unique IDs
  const getUniqueId = (baseId: string): string => {
    let uniqueId = baseId;
    let counter = 1;
    
    // If baseId is the fallback 'heading', add counter immediately
    if (baseId === 'heading') {
      uniqueId = `heading-${headingCounter++}`;
    }
    
    // Ensure uniqueness
    while (usedIds.has(uniqueId)) {
      uniqueId = `${baseId}-${counter++}`;
    }
    
    usedIds.add(uniqueId);
    return uniqueId;
  };

  for (const line of lines) {
    const trimmedLine = line.trim();
    
    // Match h2 headings (## Title)
    const h2Match = trimmedLine.match(/^##\s+(.+)$/);
    if (h2Match) {
      const text = h2Match[1].trim();
      const baseId = generateSlug(text);
      const id = getUniqueId(baseId);
      const h2Item: TocItem = {
        id,
        text,
        level: 2
      };
      tocItems.push(h2Item);
    }
    
    // Match h3 headings (### Title)
    const h3Match = trimmedLine.match(/^###\s+(.+)$/);
    if (h3Match) {
      const text = h3Match[1].trim();
      const baseId = generateSlug(text);
      const id = getUniqueId(baseId);
      const h3Item: TocItem = {
        id,
        text,
        level: 3
      };
      tocItems.push(h3Item);
    }
  }

  return tocItems;
}
