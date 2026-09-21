/**
 * Splits one stream of dictated (or typed, or pasted) text into shopping items.
 *
 * `knownNames` is the ingredient catalogue's names, used ONLY to find boundaries —
 * never to rewrite an item. Items stay free text exactly as the user produced them
 * (apart from trimming, whitespace collapse, leading-filler removal and capitalising
 * the first character). An empty `knownNames` is fully supported: the catalogue query
 * may be loading or failed.
 *
 * Pure. No I/O, no Date, no randomness. Same inputs always give the same output.
 */
export function splitDictation(text: string, knownNames: string[]): string[] {
  const preparedNames = knownNames.map((name) => name.trim()).filter((name) => name.length > 0);

  // Phase C prep: known names that would themselves be split by the Phase-D "and"/"&"
  // regex, longest-first (then lexicographic) so a longer name is protected before a
  // shorter one that could otherwise consume part of it.
  const andCandidates = preparedNames
    .filter((name) => AND_NAME_RE.test(name))
    .sort((a, b) => b.length - a.length || a.localeCompare(b));

  // Phase F prep: the run-split name index, built once for the whole call.
  const nameIndex = buildNameIndex(preparedNames);

  const collected: string[] = [];

  // Phase A — hard-separator split, protecting a '.' between two digits via a manual
  // left-to-right scan (no regex lookbehind — unsupported on older iOS Safari).
  const decimalProtected = protectDecimalPoints(text);
  const rawSegments = decimalProtected.split(/\r?\n|[,;.]/);

  for (const rawSegment of rawSegments) {
    const segment = rawSegment.split(DECIMAL_SENTINEL).join('.');

    // Phase B — normalise each segment before name protection so a name regex never
    // has to cope with double spaces.
    const normalized = segment.trim().replace(/\s+/g, ' ');
    if (normalized.length === 0) continue;

    // Phase C — protect known names containing a standalone "and" / "&".
    const { text: substituted, slots } = protectKnownNames(normalized, andCandidates);

    // Phase D — split on standalone "and" / "&".
    const rawPieces = substituted.split(AND_SPLIT_RE);

    for (const rawPiece of rawPieces) {
      // Phase E — restore placeholders, then clean.
      const restored = restorePlaceholders(rawPiece, slots);
      const cleanedPiece = stripLeadingFillers(restored.trim().replace(/\s+/g, ' '));
      if (cleanedPiece.length === 0) continue;

      // Phase F — separator-less run split, all-or-nothing, applied to this piece alone.
      const runItems = runSplit(cleanedPiece, nameIndex);

      for (const item of runItems) {
        // Phase G — final clean: trim, drop if empty, capitalise only the first character.
        const trimmed = item.trim();
        if (trimmed.length === 0) continue;
        collected.push(trimmed.charAt(0).toUpperCase() + trimmed.slice(1));
      }
    }
  }

  // Phase H — de-duplicate, keeping the first occurrence verbatim.
  const seen = new Set<string>();
  const result: string[] = [];
  for (const item of collected) {
    const key = itemKey(item);
    if (seen.has(key)) continue;
    seen.add(key);
    result.push(item);
  }
  return result;
}

/**
 * The case-insensitive identity of an item: `text.trim().replace(/\s+/g, ' ').toLowerCase()`.
 * Used for de-duplication inside `splitDictation` and as the key of the sheet's
 * excluded-item set. Both sides MUST use this one function.
 */
export function itemKey(text: string): string {
  return text.trim().replace(/\s+/g, ' ').toLowerCase();
}

// ---------------------------------------------------------------------------
// Phase A — decimal-point protection via a manual character scan.
// ---------------------------------------------------------------------------

// Private-use sentinel standing in for a '.' between two digits while the hard
// separators are split on.
const DECIMAL_SENTINEL = '';

function isAsciiDigit(ch: string | undefined): boolean {
  return ch !== undefined && ch >= '0' && ch <= '9';
}

function protectDecimalPoints(text: string): string {
  let result = '';
  for (let i = 0; i < text.length; i++) {
    const ch = text[i];
    if (ch === '.' && isAsciiDigit(text[i - 1]) && isAsciiDigit(text[i + 1])) {
      result += DECIMAL_SENTINEL;
    } else {
      result += ch;
    }
  }
  return result;
}

// ---------------------------------------------------------------------------
// Phase C — protecting known names that contain a standalone "and" / "&".
// ---------------------------------------------------------------------------

const AND_NAME_RE = /(?:^|\s)(?:and|&)(?=\s|$)/i;
const WORD_CHAR_RE = /\w/;

// Two private-use sentinels bracketing a slot index, e.g. "3".
const SLOT_OPEN = '';
const SLOT_CLOSE = '';
const SLOT_RE = /(\d+)/g;

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function protectKnownNames(
  segment: string,
  candidates: string[],
): { text: string; slots: string[] } {
  const slots: string[] = [];
  let text = segment;

  for (const name of candidates) {
    const escaped = escapeRegExp(name).replace(/\s+/g, '\\s+');
    const prefix = WORD_CHAR_RE.test(name.charAt(0)) ? '\\b' : '';
    const suffix = WORD_CHAR_RE.test(name.charAt(name.length - 1)) ? '\\b' : '';
    const regex = new RegExp(prefix + escaped + suffix, 'gi');
    text = text.replace(regex, (match) => {
      const index = slots.length;
      slots.push(match);
      return `${SLOT_OPEN}${index}${SLOT_CLOSE}`;
    });
  }

  return { text, slots };
}

function restorePlaceholders(text: string, slots: string[]): string {
  return text.replace(SLOT_RE, (_match, indexStr: string) => slots[Number(indexStr)]);
}

// ---------------------------------------------------------------------------
// Phase D — split on standalone "and" / "&".
// ---------------------------------------------------------------------------

const AND_SPLIT_RE = /(?:^|\s+)(?:and|&)(?:\s+|$)/gi;

// ---------------------------------------------------------------------------
// Phase E — leading-filler stripping.
// ---------------------------------------------------------------------------

// Words people say around an item that are not part of it. Stripped from the front of
// every piece, and skipped between items inside a separator-less run. The owner asked
// for these after trying the build — this list, not the original handoff, is the rule.
const FILLERS = new Set([
  'and',
  'also',
  'plus',
  '&',
  'some',
  'the',
  'then',
  'maybe',
  'please',
  'get',
  'buy',
  'grab',
  'um',
  'umm',
  'uh',
  'er',
  'oh',
  'ok',
  'okay',
]);

// Leading phrases, longest first so "i also need" wins over "i need". Apostrophes are
// compared straight: iOS dictation types a curly one.
const FILLER_PHRASES = [
  "don't forget the",
  'we also need',
  "don't forget",
  'i also need',
  "let's get",
  'we need',
  'we want',
  'i need',
  'i want',
];

function isFiller(token: string): boolean {
  return FILLERS.has(token.toLowerCase());
}

function stripLeadingFillers(text: string): string {
  let result = text;
  while (result.length > 0) {
    // Same length as `result`, so a phrase's length slices both.
    const lower = result.toLowerCase().replace(/\u2019/g, "'");
    const phrase = FILLER_PHRASES.find((p) => lower === p || lower.startsWith(p + ' '));
    if (phrase) {
      result = result.slice(phrase.length).trim();
      continue;
    }
    const spaceIndex = result.indexOf(' ');
    const firstWord = spaceIndex === -1 ? result : result.slice(0, spaceIndex);
    if (!isFiller(firstWord)) break;
    result = spaceIndex === -1 ? '' : result.slice(spaceIndex + 1).trim();
  }
  return result;
}

// ---------------------------------------------------------------------------
// Phase F — separator-less run split (all-or-nothing). Token-array comparison
// only; no regexes for name matching. Quantity-word checks use static regexes
// for digits/fractions, which is not the per-name `new RegExp` the performance
// contract forbids.
// ---------------------------------------------------------------------------

type NameTokens = string[];

const QUANTITY_WORDS = new Set([
  'a',
  'an',
  'one',
  'two',
  'three',
  'four',
  'five',
  'six',
  'seven',
  'eight',
  'nine',
  'ten',
  'eleven',
  'twelve',
  'dozen',
  'couple',
  'few',
  'several',
  'half',
  'pair',
  'of',
]);

const INTEGER_OR_DECIMAL_RE = /^\d+(?:\.\d+)?$/;
const FRACTION_RE = /^\d+\/\d+$/;
const UNICODE_FRACTION_RE = /^[½¼¾⅓⅔⅛]$/;

function isQuantityWord(token: string): boolean {
  const lower = token.toLowerCase();
  if (QUANTITY_WORDS.has(lower)) return true;
  if (INTEGER_OR_DECIMAL_RE.test(token)) return true;
  if (FRACTION_RE.test(token)) return true;
  if (UNICODE_FRACTION_RE.test(token)) return true;
  return false;
}

function tokensEquivalent(a: string, b: string): boolean {
  return a === b || a === b + 's' || a === b + 'es' || b === a + 's' || b === a + 'es';
}

function buildNameIndex(preparedNames: string[]): Map<string, NameTokens[]> {
  const index = new Map<string, NameTokens[]>();

  for (const name of preparedNames) {
    const tokens = name
      .toLowerCase()
      .split(/\s+/)
      .filter((token) => token.length > 0);
    if (tokens.length === 0) continue;

    const key = tokens[0];
    const bucket = index.get(key);
    if (bucket) {
      bucket.push(tokens);
    } else {
      index.set(key, [tokens]);
    }
  }

  for (const bucket of index.values()) {
    bucket.sort(compareCandidates);
  }

  return index;
}

function compareCandidates(a: NameTokens, b: NameTokens): number {
  if (b.length !== a.length) return b.length - a.length;
  const aJoined = a.join(' ');
  const bJoined = b.join(' ');
  if (bJoined.length !== aJoined.length) return bJoined.length - aJoined.length;
  return aJoined.localeCompare(bJoined);
}

function candidatesAt(index: Map<string, NameTokens[]>, token: string): NameTokens[] {
  const lower = token.toLowerCase();
  const keys = new Set<string>([lower, lower + 's', lower + 'es']);
  if (lower.endsWith('s')) keys.add(lower.slice(0, -1));
  if (lower.endsWith('es')) keys.add(lower.slice(0, -2));

  const seen = new Set<NameTokens>();
  const result: NameTokens[] = [];
  for (const key of keys) {
    const bucket = index.get(key);
    if (!bucket) continue;
    for (const candidate of bucket) {
      if (seen.has(candidate)) continue;
      seen.add(candidate);
      result.push(candidate);
    }
  }

  result.sort(compareCandidates);
  return result;
}

function candidateMatchesAt(tokens: string[], position: number, name: NameTokens): boolean {
  const k = name.length;
  if (position + k > tokens.length) return false;

  for (let j = 0; j < k - 1; j++) {
    if (tokens[position + j].toLowerCase() !== name[j]) return false;
  }

  const lastToken = tokens[position + k - 1].toLowerCase();
  return tokensEquivalent(lastToken, name[k - 1]);
}

function tokenize(piece: string): { tokens: string[]; starts: number[]; ends: number[] } {
  const tokens: string[] = [];
  const starts: number[] = [];
  const ends: number[] = [];
  let start = 0;

  for (let i = 0; i <= piece.length; i++) {
    if (i === piece.length || piece[i] === ' ') {
      if (i > start) {
        tokens.push(piece.slice(start, i));
        starts.push(start);
        ends.push(i);
      }
      start = i + 1;
    }
  }

  return { tokens, starts, ends };
}

function runSplit(piece: string, nameIndex: Map<string, NameTokens[]>): string[] {
  const { tokens, starts, ends } = tokenize(piece);
  if (tokens.length === 0) return [piece];

  const items: string[] = [];
  let i = 0;
  let pendingStart: number | null = null;

  while (i < tokens.length) {
    let matchLength: number | null = null;
    for (const candidate of candidatesAt(nameIndex, tokens[i])) {
      if (candidateMatchesAt(tokens, i, candidate)) {
        matchLength = candidate.length;
        break;
      }
    }

    if (matchLength !== null) {
      const start = pendingStart ?? i;
      items.push(piece.slice(starts[start], ends[i + matchLength - 1]));
      pendingStart = null;
      i += matchLength;
    } else if (isQuantityWord(tokens[i])) {
      if (pendingStart === null) pendingStart = i;
      i += 1;
    } else if (pendingStart === null && isFiller(tokens[i])) {
      // A filler between two items ("milk some eggs") belongs to neither.
      i += 1;
    } else {
      return [piece];
    }
  }

  if (pendingStart !== null) return [piece];
  return items;
}
