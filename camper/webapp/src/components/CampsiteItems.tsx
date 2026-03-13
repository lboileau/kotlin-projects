export function TentSVG() {
  return (
    <svg width="120" height="100" viewBox="0 0 120 100">
      {/* Tent body */}
      <polygon points="60,5 10,90 110,90" fill="#E8B4B8" stroke="#D4949A" strokeWidth="2" />
      {/* Tent shadow side */}
      <polygon points="60,5 60,90 110,90" fill="#D4949A" opacity="0.5" />
      {/* Door flap */}
      <path d="M48,90 Q55,55 60,50 Q65,55 72,90" fill="#C47A80" stroke="#B4646A" strokeWidth="1.5" />
      {/* Flag on top */}
      <line x1="60" y1="5" x2="60" y2="-8" stroke="#7A5230" strokeWidth="2" />
      <polygon points="60,-8 75,-3 60,2" fill="#F0E6B8" stroke="#E0D48A" strokeWidth="0.5" />
      {/* Ground */}
      <ellipse cx="60" cy="92" rx="55" ry="6" fill="rgba(0,0,0,0.15)" />
    </svg>
  );
}

export function EquipmentPileSVG() {
  return (
    <svg width="110" height="90" viewBox="0 0 110 90">
      {/* Backpack */}
      <rect x="35" y="15" width="35" height="45" rx="8" fill="#B5C9B3" stroke="#8FB08A" strokeWidth="2" />
      <rect x="40" y="20" width="25" height="15" rx="4" fill="#8FB08A" />
      {/* Backpack straps */}
      <path d="M38,25 Q35,45 38,55" fill="none" stroke="#5A7A56" strokeWidth="2.5" strokeLinecap="round" />
      <path d="M67,25 Q70,45 67,55" fill="none" stroke="#5A7A56" strokeWidth="2.5" strokeLinecap="round" />
      {/* Rope coil */}
      <circle cx="22" cy="55" r="14" fill="none" stroke="#D4BC94" strokeWidth="4" />
      <circle cx="22" cy="55" r="8" fill="none" stroke="#D4BC94" strokeWidth="3" />
      {/* Axe */}
      <line x1="80" y1="10" x2="95" y2="75" stroke="#7A5230" strokeWidth="3" strokeLinecap="round" />
      <path d="M88,12 Q100,8 95,20 Q90,16 88,12Z" fill="#8A8A9A" stroke="#6A6A7A" strokeWidth="1" />
      {/* Lantern */}
      <rect x="8" y="30" width="12" height="18" rx="3" fill="#F0E6B8" stroke="#E0D48A" strokeWidth="1.5" />
      <rect x="10" y="27" width="8" height="4" rx="2" fill="#7A5230" />
      <circle cx="14" cy="39" r="3" fill="#FFD166" opacity="0.6" />
      {/* Ground shadow */}
      <ellipse cx="55" cy="82" rx="50" ry="6" fill="rgba(0,0,0,0.12)" />
    </svg>
  );
}

export function KitchenSVG() {
  return (
    <svg width="110" height="100" viewBox="0 0 110 100">
      {/* Table/counter */}
      <rect x="10" y="50" width="90" height="8" rx="2" fill="#7A5230" stroke="#5A3A1E" strokeWidth="1" />
      {/* Table legs */}
      <rect x="15" y="58" width="5" height="25" fill="#5A3A1E" />
      <rect x="90" y="58" width="5" height="25" fill="#5A3A1E" />
      {/* Cooking pot */}
      <ellipse cx="55" cy="45" rx="22" ry="6" fill="#4A4A58" stroke="#3A3A48" strokeWidth="1.5" />
      <path d="M33,45 Q33,65 55,65 Q77,65 77,45" fill="#4A4A58" stroke="#3A3A48" strokeWidth="1.5" />
      {/* Pot handle */}
      <path d="M33,42 Q25,35 33,28" fill="none" stroke="#3A3A48" strokeWidth="2" strokeLinecap="round" />
      {/* Steam */}
      <path d="M45,30 Q43,20 47,12" fill="none" stroke="rgba(255,255,255,0.3)" strokeWidth="2" strokeLinecap="round" />
      <path d="M55,28 Q53,16 57,8" fill="none" stroke="rgba(255,255,255,0.25)" strokeWidth="2" strokeLinecap="round" />
      <path d="M65,30 Q63,20 67,14" fill="none" stroke="rgba(255,255,255,0.2)" strokeWidth="2" strokeLinecap="round" />
      {/* Cutting board + knife */}
      <rect x="82" y="40" width="18" height="12" rx="2" fill="#D4BC94" stroke="#A68B5B" strokeWidth="1" />
      <line x1="102" y1="38" x2="108" y2="48" stroke="#8A8A9A" strokeWidth="1.5" strokeLinecap="round" />
      {/* Ground shadow */}
      <ellipse cx="55" cy="88" rx="45" ry="5" fill="rgba(0,0,0,0.12)" />
    </svg>
  );
}

export function LogBookSVG() {
  return (
    <svg width="100" height="100" viewBox="0 0 100 100">
      {/* Stool legs */}
      <rect x="25" y="62" width="4" height="28" rx="1" fill="#5A3A1E" />
      <rect x="71" y="62" width="4" height="28" rx="1" fill="#5A3A1E" />
      {/* Stool crossbar */}
      <rect x="28" y="76" width="44" height="3" rx="1" fill="#5A3A1E" opacity="0.7" />
      {/* Stool seat */}
      <rect x="18" y="56" width="64" height="8" rx="3" fill="#7A5230" stroke="#5A3A1E" strokeWidth="1" />
      {/* Book body */}
      <rect x="24" y="28" width="52" height="32" rx="3" fill="#8B4513" stroke="#5A3210" strokeWidth="1.5" />
      {/* Book spine */}
      <rect x="48" y="26" width="4" height="34" rx="1" fill="#5A3210" />
      {/* Left page area */}
      <rect x="27" y="31" width="20" height="26" rx="1" fill="#FAF5EB" opacity="0.85" />
      {/* Right page area */}
      <rect x="53" y="31" width="20" height="26" rx="1" fill="#FAF5EB" opacity="0.85" />
      {/* Page lines (left) */}
      <line x1="30" y1="37" x2="44" y2="37" stroke="var(--tan-deep)" strokeWidth="0.6" opacity="0.4" />
      <line x1="30" y1="41" x2="43" y2="41" stroke="var(--tan-deep)" strokeWidth="0.6" opacity="0.35" />
      <line x1="30" y1="45" x2="44" y2="45" stroke="var(--tan-deep)" strokeWidth="0.6" opacity="0.3" />
      <line x1="30" y1="49" x2="42" y2="49" stroke="var(--tan-deep)" strokeWidth="0.6" opacity="0.25" />
      {/* Page lines (right) */}
      <line x1="56" y1="37" x2="70" y2="37" stroke="var(--tan-deep)" strokeWidth="0.6" opacity="0.4" />
      <line x1="56" y1="41" x2="69" y2="41" stroke="var(--tan-deep)" strokeWidth="0.6" opacity="0.35" />
      <line x1="56" y1="45" x2="70" y2="45" stroke="var(--tan-deep)" strokeWidth="0.6" opacity="0.3" />
      {/* Book corner decoration */}
      <circle cx="35" cy="34" r="1.5" fill="var(--ember)" opacity="0.6" />
      <circle cx="65" cy="34" r="1.5" fill="var(--ember)" opacity="0.6" />
      {/* Bookmark ribbon */}
      <path d="M62,28 L62,22 L65,25 L68,22 L68,28" fill="var(--rose-deep)" stroke="var(--rose-deep)" strokeWidth="0.5" />
      {/* Ground shadow */}
      <ellipse cx="50" cy="92" rx="35" ry="5" fill="rgba(0,0,0,0.12)" />
    </svg>
  );
}

export function MapTableSVG() {
  return (
    <svg width="120" height="100" viewBox="0 0 120 100">
      {/* Table */}
      <rect x="10" y="45" width="100" height="6" rx="2" fill="#7A5230" stroke="#5A3A1E" strokeWidth="1" />
      {/* Table legs */}
      <rect x="18" y="51" width="4" height="30" rx="1" fill="#5A3A1E" />
      <rect x="98" y="51" width="4" height="30" rx="1" fill="#5A3A1E" />
      {/* Map (rolled parchment look) */}
      <rect x="20" y="30" width="80" height="18" rx="2" fill="#FAF5EB" stroke="#D4BC94" strokeWidth="1.5" />
      {/* Map details - trails */}
      <path d="M30,36 Q45,42 60,38 Q75,34 90,40" fill="none" stroke="#D4949A" strokeWidth="1.2" strokeDasharray="3,2" />
      <path d="M35,40 Q50,35 70,42 Q85,45 95,38" fill="none" stroke="#8FB08A" strokeWidth="0.8" />
      {/* Map X marks */}
      <g transform="translate(45,36)" stroke="#E85D3A" strokeWidth="1.5" strokeLinecap="round">
        <line x1="-3" y1="-3" x2="3" y2="3" />
        <line x1="3" y1="-3" x2="-3" y2="3" />
      </g>
      <g transform="translate(75,39)" stroke="#E85D3A" strokeWidth="1.5" strokeLinecap="round">
        <line x1="-3" y1="-3" x2="3" y2="3" />
        <line x1="3" y1="-3" x2="-3" y2="3" />
      </g>
      {/* Compass rose */}
      <circle cx="85" cy="37" r="5" fill="none" stroke="#A68B5B" strokeWidth="0.8" />
      <line x1="85" y1="32" x2="85" y2="42" stroke="#A68B5B" strokeWidth="0.6" />
      <line x1="80" y1="37" x2="90" y2="37" stroke="#A68B5B" strokeWidth="0.6" />
      {/* Scroll curl left */}
      <ellipse cx="20" cy="39" rx="4" ry="10" fill="#EDE4D0" stroke="#D4BC94" strokeWidth="1" />
      {/* Scroll curl right */}
      <ellipse cx="100" cy="39" rx="4" ry="10" fill="#EDE4D0" stroke="#D4BC94" strokeWidth="1" />
      {/* Ground shadow */}
      <ellipse cx="60" cy="86" rx="48" ry="5" fill="rgba(0,0,0,0.12)" />
    </svg>
  );
}
