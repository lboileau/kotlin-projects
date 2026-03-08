import { useEffect, useRef, useMemo } from 'react';
import './ParallaxBackground.css';

interface Props {
  variant: 'night' | 'dusk' | 'campsite';
  timeOfDay?: 'day' | 'night';
}

function seededRandom(seed: number) {
  const x = Math.sin(seed * 9301 + 49297) * 49297;
  return x - Math.floor(x);
}

export function ParallaxBackground({ variant, timeOfDay }: Props) {
  const isDay = timeOfDay === 'day';
  const containerRef = useRef<HTMLDivElement>(null);

  const stars = useMemo(() =>
    Array.from({ length: 40 }).map((_, i) => ({
      left: seededRandom(i * 4) * 100,
      top: seededRandom(i * 4 + 1) * 60,
      delay: seededRandom(i * 4 + 2) * 4,
      duration: 2 + seededRandom(i * 4 + 3) * 3,
      size: 2 + seededRandom(i * 4 + 4) * 3,
    })), []);

  const leaves = useMemo(() =>
    Array.from({ length: 6 }).map((_, i) => ({
      left: 10 + seededRandom(i * 3 + 100) * 80,
      bottom: seededRandom(i * 3 + 101) * 30,
      delay: seededRandom(i * 3 + 102) * 8,
      duration: 8 + seededRandom(i * 3 + 103) * 6,
    })), []);

  useEffect(() => {
    const handleMouse = (e: MouseEvent) => {
      if (!containerRef.current) return;
      const x = (e.clientX / window.innerWidth - 0.5) * 2;
      const y = (e.clientY / window.innerHeight - 0.5) * 2;
      containerRef.current.style.setProperty('--mouse-x', String(x));
      containerRef.current.style.setProperty('--mouse-y', String(y));
    };
    window.addEventListener('mousemove', handleMouse);
    return () => window.removeEventListener('mousemove', handleMouse);
  }, []);

  const eyes = useMemo(() =>
    Array.from({ length: 6 }).map((_, i) => ({
      x: seededRandom(i * 5 + 200) < 0.5
        ? 3 + seededRandom(i * 5 + 201) * 14    // left side
        : 83 + seededRandom(i * 5 + 201) * 14,  // right side
      y: 10 + seededRandom(i * 5 + 202) * 32,
      size: 2 + seededRandom(i * 5 + 203) * 2,
      gap: 6 + seededRandom(i * 5 + 204) * 4,
      delay: seededRandom(i * 5 + 205) * 8,
      duration: 3 + seededRandom(i * 5 + 206) * 4,
      color: seededRandom(i * 5 + 207) < 0.5 ? '#AADD44' : '#FFCC33',
    })), []);

  if (variant === 'campsite') {
    return (
      <div ref={containerRef} className={`parallax-bg parallax-bg--campsite ${isDay ? 'campsite--day' : 'campsite--night'}`}>
        {/* Deep forest background */}
        <div className="campsite-darkness" />

        {/* Sky through canopy */}
        <div className="campsite-sky-sliver" />

        {/* Stars visible through canopy */}
        <div className="campsite-sky-objects" style={{ transform: `translate(calc(var(--mouse-x) * -2px), calc(var(--mouse-y) * -1px))` }}>
          {!isDay && stars.slice(0, 20).map((s, i) => (
            <div
              key={i}
              className="campsite-star"
              style={{
                left: `${20 + s.left * 0.6}%`,
                top: `${s.top * 0.7}%`,
                animationDelay: `${s.delay}s`,
                animationDuration: `${s.duration}s`,
                width: `${s.size * 0.7}px`,
                height: `${s.size * 0.7}px`,
              }}
            />
          ))}
        </div>

        {/* Far tree trunks + pine canopies — deep in the forest, faded */}
        <svg className="parallax-layer campsite-trunks-far" viewBox="0 0 1440 900" preserveAspectRatio="xMidYMin slice" style={{ transform: `translate(calc(var(--mouse-x) * -4px), calc(var(--mouse-y) * -2px))` }}>
          {/* Left deep forest — subtle bark lines */}
          <rect x="30" y="80" width="18" height="820" fill="#2A2A1E" />
          <line x1="39" y1="80" x2="38" y2="900" stroke="#242418" strokeWidth="0.8" />
          <polygon points="39,0 -10,180 88,180" fill="#1E2418" />
          <polygon points="39,60 5,160 73,160" fill="#202818" />

          <rect x="90" y="60" width="14" height="840" fill="#2C2C20" />
          <line x1="97" y1="60" x2="97" y2="900" stroke="#26261A" strokeWidth="0.6" />
          <polygon points="97,0 60,120 134,120" fill="#1C2218" />

          <rect x="170" y="50" width="20" height="850" fill="#282820" />
          <line x1="180" y1="50" x2="179" y2="900" stroke="#22221A" strokeWidth="0.8" />
          <polygon points="180,0 140,110 220,110" fill="#1E2418" />
          <polygon points="180,40 150,100 210,100" fill="#202818" />

          <rect x="260" y="100" width="12" height="800" fill="#2A2A1E" />
          <line x1="266" y1="100" x2="266" y2="900" stroke="#242418" strokeWidth="0.6" />
          <polygon points="266,30 240,160 292,160" fill="#1C2218" />

          <rect x="340" y="70" width="16" height="830" fill="#2C2C20" />
          <line x1="348" y1="70" x2="347" y2="900" stroke="#26261A" strokeWidth="0.7" />
          <polygon points="348,0 310,140 386,140" fill="#1E2418" />

          <rect x="420" y="90" width="10" height="810" fill="#282820" />
          <line x1="425" y1="90" x2="425" y2="900" stroke="#22221A" strokeWidth="0.5" />
          <polygon points="425,20 400,150 450,150" fill="#1C2218" />

          {/* Right deep forest — subtle bark lines */}
          <rect x="1020" y="90" width="10" height="810" fill="#282820" />
          <line x1="1025" y1="90" x2="1025" y2="900" stroke="#22221A" strokeWidth="0.5" />
          <polygon points="1025,20 1000,150 1050,150" fill="#1C2218" />

          <rect x="1100" y="70" width="16" height="830" fill="#2C2C20" />
          <line x1="1108" y1="70" x2="1107" y2="900" stroke="#26261A" strokeWidth="0.7" />
          <polygon points="1108,0 1070,140 1146,140" fill="#1E2418" />

          <rect x="1180" y="100" width="12" height="800" fill="#2A2A1E" />
          <line x1="1186" y1="100" x2="1186" y2="900" stroke="#242418" strokeWidth="0.6" />
          <polygon points="1186,30 1160,160 1212,160" fill="#1C2218" />

          <rect x="1260" y="50" width="20" height="850" fill="#282820" />
          <line x1="1270" y1="50" x2="1269" y2="900" stroke="#22221A" strokeWidth="0.8" />
          <polygon points="1270,0 1230,110 1310,110" fill="#1E2418" />
          <polygon points="1270,40 1240,100 1300,100" fill="#202818" />

          <rect x="1350" y="60" width="14" height="840" fill="#2C2C20" />
          <line x1="1357" y1="60" x2="1357" y2="900" stroke="#26261A" strokeWidth="0.6" />
          <polygon points="1357,0 1320,120 1394,120" fill="#1C2218" />

          <rect x="1400" y="80" width="18" height="820" fill="#2A2A1E" />
          <line x1="1409" y1="80" x2="1408" y2="900" stroke="#242418" strokeWidth="0.8" />
          <polygon points="1409,0 1360,180 1458,180" fill="#1E2418" />
          <polygon points="1409,60 1375,160 1443,160" fill="#202818" />
        </svg>

        {/* Mid tree trunks + pine canopies — medium depth */}
        <svg className="parallax-layer campsite-trunks-mid" viewBox="0 0 1440 900" preserveAspectRatio="xMidYMin slice" style={{ transform: `translate(calc(var(--mouse-x) * -10px), calc(var(--mouse-y) * -4px))` }}>
          {/* Left */}
          <rect x="10" y="60" width="32" height="840" fill="#222018" />
          <line x1="10" y1="60" x2="10" y2="900" stroke="#2E2C1E" strokeWidth="1" />
          <line x1="26" y1="60" x2="26" y2="900" stroke="#181610" strokeWidth="2" />
          <line x1="38" y1="60" x2="38" y2="900" stroke="#2A281C" strokeWidth="0.7" />
          <ellipse cx="26" cy="400" rx="5" ry="8" fill="#1C1A10" stroke="#2A281C" strokeWidth="0.5" />
          <polygon points="26,0 -20,130 72,130" fill="#1A2416" />
          <polygon points="26,30 -8,110 60,110" fill="#1C2618" />
          <polygon points="26,55 2,95 50,95" fill="#1A2416" />

          <rect x="80" y="40" width="24" height="860" fill="#242218" />
          <line x1="86" y1="40" x2="86" y2="900" stroke="#2C2A1C" strokeWidth="0.8" />
          <line x1="92" y1="40" x2="92" y2="900" stroke="#181610" strokeWidth="1.5" />
          <line x1="99" y1="40" x2="100" y2="900" stroke="#2A281C" strokeWidth="0.6" />
          <ellipse cx="92" cy="550" rx="4" ry="7" fill="#1E1C12" stroke="#2A281C" strokeWidth="0.5" />
          {/* Branch stub */}
          <path d="M104,320 Q112,316 116,310" fill="none" stroke="#242218" strokeWidth="2" strokeLinecap="round" />
          <polygon points="92,0 50,100 134,100" fill="#182218" />
          <polygon points="92,25 58,85 126,85" fill="#1A2618" />

          <rect x="150" y="30" width="28" height="870" fill="#201E14" />
          <line x1="156" y1="30" x2="156" y2="900" stroke="#2A281C" strokeWidth="0.8" />
          <line x1="164" y1="30" x2="163" y2="900" stroke="#181610" strokeWidth="1.5" />
          <line x1="172" y1="30" x2="173" y2="900" stroke="#2A281C" strokeWidth="0.6" />
          <ellipse cx="164" cy="350" rx="5" ry="8" fill="#1A1810" stroke="#28261C" strokeWidth="0.5" />
          <ellipse cx="164" cy="680" rx="4" ry="6" fill="#1A1810" stroke="#28261C" strokeWidth="0.5" />
          <polygon points="164,0 120,80 208,80" fill="#1A2416" />
          <polygon points="164,20 130,70 198,70" fill="#1C2618" />

          <rect x="230" y="50" width="20" height="850" fill="#222018" />
          <line x1="240" y1="50" x2="239" y2="900" stroke="#181610" strokeWidth="1.2" />
          <ellipse cx="240" cy="480" rx="3" ry="5" fill="#1C1A10" stroke="#28261C" strokeWidth="0.5" />
          <polygon points="240,0 205,100 275,100" fill="#182218" />

          <rect x="310" y="40" width="22" height="860" fill="#242218" />
          <line x1="316" y1="40" x2="316" y2="900" stroke="#2C2A1C" strokeWidth="0.7" />
          <line x1="321" y1="40" x2="322" y2="900" stroke="#181610" strokeWidth="1.3" />
          <path d="M310,500 Q302,496 298,490" fill="none" stroke="#242218" strokeWidth="1.8" strokeLinecap="round" />
          <polygon points="321,0 280,90 362,90" fill="#1A2416" />

          <rect x="380" y="70" width="16" height="830" fill="#201E14" />
          <line x1="388" y1="70" x2="387" y2="900" stroke="#181610" strokeWidth="1" />
          <ellipse cx="388" cy="420" rx="3" ry="5" fill="#1A1810" stroke="#28261C" strokeWidth="0.5" />
          <polygon points="388,10 358,120 418,120" fill="#182218" />

          {/* Right */}
          <rect x="1044" y="70" width="16" height="830" fill="#201E14" />
          <line x1="1052" y1="70" x2="1053" y2="900" stroke="#181610" strokeWidth="1" />
          <ellipse cx="1052" cy="440" rx="3" ry="5" fill="#1A1810" stroke="#28261C" strokeWidth="0.5" />
          <polygon points="1052,10 1022,120 1082,120" fill="#182218" />

          <rect x="1108" y="40" width="22" height="860" fill="#242218" />
          <line x1="1114" y1="40" x2="1114" y2="900" stroke="#2C2A1C" strokeWidth="0.7" />
          <line x1="1119" y1="40" x2="1120" y2="900" stroke="#181610" strokeWidth="1.3" />
          <path d="M1130,480 Q1138,476 1142,470" fill="none" stroke="#242218" strokeWidth="1.8" strokeLinecap="round" />
          <polygon points="1119,0 1078,90 1160,90" fill="#1A2416" />

          <rect x="1190" y="50" width="20" height="850" fill="#222018" />
          <line x1="1200" y1="50" x2="1201" y2="900" stroke="#181610" strokeWidth="1.2" />
          <ellipse cx="1200" cy="500" rx="3" ry="5" fill="#1C1A10" stroke="#28261C" strokeWidth="0.5" />
          <polygon points="1200,0 1165,100 1235,100" fill="#182218" />

          <rect x="1262" y="30" width="28" height="870" fill="#201E14" />
          <line x1="1268" y1="30" x2="1268" y2="900" stroke="#2A281C" strokeWidth="0.8" />
          <line x1="1276" y1="30" x2="1277" y2="900" stroke="#181610" strokeWidth="1.5" />
          <line x1="1284" y1="30" x2="1284" y2="900" stroke="#2A281C" strokeWidth="0.6" />
          <ellipse cx="1276" cy="380" rx="5" ry="8" fill="#1A1810" stroke="#28261C" strokeWidth="0.5" />
          <ellipse cx="1276" cy="700" rx="4" ry="6" fill="#1A1810" stroke="#28261C" strokeWidth="0.5" />
          <polygon points="1276,0 1232,80 1320,80" fill="#1A2416" />
          <polygon points="1276,20 1242,70 1310,70" fill="#1C2618" />

          <rect x="1336" y="40" width="24" height="860" fill="#242218" />
          <line x1="1342" y1="40" x2="1342" y2="900" stroke="#2C2A1C" strokeWidth="0.8" />
          <line x1="1348" y1="40" x2="1348" y2="900" stroke="#181610" strokeWidth="1.5" />
          <line x1="1355" y1="40" x2="1356" y2="900" stroke="#2A281C" strokeWidth="0.6" />
          <ellipse cx="1348" cy="530" rx="4" ry="7" fill="#1E1C12" stroke="#2A281C" strokeWidth="0.5" />
          <polygon points="1348,0 1306,100 1390,100" fill="#182218" />
          <polygon points="1348,25 1314,85 1382,85" fill="#1A2618" />

          <rect x="1398" y="60" width="32" height="840" fill="#222018" />
          <line x1="1404" y1="60" x2="1404" y2="900" stroke="#2A281C" strokeWidth="0.7" />
          <line x1="1414" y1="60" x2="1414" y2="900" stroke="#181610" strokeWidth="2" />
          <line x1="1430" y1="60" x2="1430" y2="900" stroke="#2E2C1E" strokeWidth="1" />
          <ellipse cx="1414" cy="420" rx="5" ry="8" fill="#1C1A10" stroke="#2A281C" strokeWidth="0.5" />
          <polygon points="1414,0 1368,130 1460,130" fill="#1A2416" />
          <polygon points="1414,30 1380,110 1448,110" fill="#1C2618" />
          <polygon points="1414,55 1390,95 1438,95" fill="#1A2416" />
        </svg>

        {/* Near tree trunks — foreground, sharp, dark */}
        <svg className="parallax-layer campsite-trunks-near" viewBox="0 0 1440 900" preserveAspectRatio="xMidYMin slice" style={{ transform: `translate(calc(var(--mouse-x) * -20px), calc(var(--mouse-y) * -6px))` }}>
          {/* Left foreground — full detail */}
          <rect x="-10" y="0" width="50" height="900" fill="#1C1A10" />
          <line x1="0" y1="0" x2="-1" y2="900" stroke="#242218" strokeWidth="0.6" />
          <line x1="5" y1="0" x2="5" y2="900" stroke="#2A2820" strokeWidth="1" />
          <line x1="15" y1="0" x2="14" y2="900" stroke="#242218" strokeWidth="0.5" />
          <line x1="20" y1="0" x2="18" y2="900" stroke="#141208" strokeWidth="3" />
          <line x1="30" y1="0" x2="31" y2="900" stroke="#242218" strokeWidth="0.6" />
          <line x1="35" y1="0" x2="36" y2="900" stroke="#2A2820" strokeWidth="0.5" />
          {/* Knots */}
          <ellipse cx="20" cy="200" rx="6" ry="8" fill="#141208" stroke="#2A2820" strokeWidth="0.5" />
          <ellipse cx="12" cy="300" rx="8" ry="12" fill="#141208" stroke="#2A2820" strokeWidth="0.5" />
          <ellipse cx="25" cy="500" rx="5" ry="7" fill="#161410" stroke="#282620" strokeWidth="0.5" />
          <ellipse cx="20" cy="600" rx="6" ry="9" fill="#141208" stroke="#2A2820" strokeWidth="0.5" />
          <ellipse cx="30" cy="750" rx="4" ry="6" fill="#161410" stroke="#282620" strokeWidth="0.5" />
          {/* Branch stubs */}
          <path d="M40,250 Q50,244 56,236" fill="none" stroke="#1C1A10" strokeWidth="3" strokeLinecap="round" />
          <path d="M40,650 Q48,646 52,640" fill="none" stroke="#1C1A10" strokeWidth="2.5" strokeLinecap="round" />
          {/* Bark cracks */}
          <path d="M8,150 Q12,180 6,210" fill="none" stroke="#282620" strokeWidth="0.4" />
          <path d="M28,400 Q32,440 26,470" fill="none" stroke="#282620" strokeWidth="0.4" />

          <rect x="70" y="0" width="38" height="900" fill="#1E1C12" />
          <line x1="74" y1="0" x2="74" y2="900" stroke="#262418" strokeWidth="0.6" />
          <line x1="78" y1="0" x2="78" y2="900" stroke="#282618" strokeWidth="1" />
          <line x1="89" y1="0" x2="90" y2="900" stroke="#121008" strokeWidth="2" />
          <line x1="100" y1="0" x2="101" y2="900" stroke="#262418" strokeWidth="0.7" />
          {/* Knots */}
          <ellipse cx="89" cy="250" rx="5" ry="7" fill="#141210" stroke="#282618" strokeWidth="0.5" />
          <ellipse cx="89" cy="450" rx="7" ry="10" fill="#121008" stroke="#282618" strokeWidth="0.5" />
          <ellipse cx="82" cy="650" rx="4" ry="6" fill="#141210" stroke="#282618" strokeWidth="0.5" />
          {/* Branch stub */}
          <path d="M70,380 Q60,374 56,366" fill="none" stroke="#1E1C12" strokeWidth="2.5" strokeLinecap="round" />
          {/* Bark cracks */}
          <path d="M95,180 Q98,220 92,260" fill="none" stroke="#282620" strokeWidth="0.4" />
          <path d="M80,550 Q84,580 78,610" fill="none" stroke="#282620" strokeWidth="0.4" />

          <rect x="160" y="0" width="30" height="900" fill="#1E1C12" />
          <line x1="168" y1="0" x2="167" y2="900" stroke="#262418" strokeWidth="0.7" />
          <line x1="175" y1="0" x2="176" y2="900" stroke="#141210" strokeWidth="1.5" />
          <line x1="184" y1="0" x2="185" y2="900" stroke="#262418" strokeWidth="0.5" />
          <ellipse cx="175" cy="350" rx="5" ry="8" fill="#141210" stroke="#282618" strokeWidth="0.5" />
          <ellipse cx="175" cy="700" rx="4" ry="6" fill="#141210" stroke="#282618" strokeWidth="0.5" />
          <path d="M190,450 Q198,446 202,440" fill="none" stroke="#1E1C12" strokeWidth="2" strokeLinecap="round" />

          <rect x="240" y="0" width="18" height="900" fill="#1C1A10" />
          <line x1="249" y1="0" x2="248" y2="900" stroke="#141210" strokeWidth="1.2" />
          <ellipse cx="249" cy="400" rx="3" ry="5" fill="#161410" stroke="#262418" strokeWidth="0.5" />
          <ellipse cx="249" cy="680" rx="3" ry="4" fill="#161410" stroke="#262418" strokeWidth="0.5" />

          {/* Right foreground — full detail */}
          <rect x="1182" y="0" width="18" height="900" fill="#1C1A10" />
          <line x1="1191" y1="0" x2="1192" y2="900" stroke="#141210" strokeWidth="1.2" />
          <ellipse cx="1191" cy="380" rx="3" ry="5" fill="#161410" stroke="#262418" strokeWidth="0.5" />
          <ellipse cx="1191" cy="660" rx="3" ry="4" fill="#161410" stroke="#262418" strokeWidth="0.5" />

          <rect x="1250" y="0" width="30" height="900" fill="#1E1C12" />
          <line x1="1258" y1="0" x2="1257" y2="900" stroke="#262418" strokeWidth="0.7" />
          <line x1="1265" y1="0" x2="1266" y2="900" stroke="#141210" strokeWidth="1.5" />
          <line x1="1274" y1="0" x2="1275" y2="900" stroke="#262418" strokeWidth="0.5" />
          <ellipse cx="1265" cy="320" rx="5" ry="8" fill="#141210" stroke="#282618" strokeWidth="0.5" />
          <ellipse cx="1265" cy="680" rx="4" ry="6" fill="#141210" stroke="#282618" strokeWidth="0.5" />
          <path d="M1250,430 Q1242,426 1238,420" fill="none" stroke="#1E1C12" strokeWidth="2" strokeLinecap="round" />

          <rect x="1332" y="0" width="38" height="900" fill="#1E1C12" />
          <line x1="1336" y1="0" x2="1336" y2="900" stroke="#262418" strokeWidth="0.6" />
          <line x1="1340" y1="0" x2="1340" y2="900" stroke="#282618" strokeWidth="1" />
          <line x1="1351" y1="0" x2="1352" y2="900" stroke="#121008" strokeWidth="2" />
          <line x1="1362" y1="0" x2="1363" y2="900" stroke="#262418" strokeWidth="0.7" />
          <ellipse cx="1351" cy="220" rx="5" ry="7" fill="#141210" stroke="#282618" strokeWidth="0.5" />
          <ellipse cx="1351" cy="350" rx="7" ry="10" fill="#121008" stroke="#282618" strokeWidth="0.5" />
          <ellipse cx="1344" cy="600" rx="4" ry="6" fill="#141210" stroke="#282618" strokeWidth="0.5" />
          <path d="M1370,360 Q1378,356 1382,348" fill="none" stroke="#1E1C12" strokeWidth="2.5" strokeLinecap="round" />
          <path d="M1357,530 Q1362,570 1354,600" fill="none" stroke="#282620" strokeWidth="0.4" />

          <rect x="1400" y="0" width="50" height="900" fill="#1C1A10" />
          <line x1="1403" y1="0" x2="1402" y2="900" stroke="#242218" strokeWidth="0.6" />
          <line x1="1405" y1="0" x2="1405" y2="900" stroke="#2A2820" strokeWidth="0.5" />
          <line x1="1413" y1="0" x2="1412" y2="900" stroke="#242218" strokeWidth="0.5" />
          <line x1="1420" y1="0" x2="1422" y2="900" stroke="#141208" strokeWidth="3" />
          <line x1="1432" y1="0" x2="1433" y2="900" stroke="#242218" strokeWidth="0.6" />
          <line x1="1435" y1="0" x2="1435" y2="900" stroke="#2A2820" strokeWidth="1" />
          <ellipse cx="1420" cy="180" rx="6" ry="8" fill="#141208" stroke="#2A2820" strokeWidth="0.5" />
          <ellipse cx="1420" cy="280" rx="8" ry="12" fill="#141208" stroke="#2A2820" strokeWidth="0.5" />
          <ellipse cx="1428" cy="480" rx="5" ry="7" fill="#161410" stroke="#282620" strokeWidth="0.5" />
          <ellipse cx="1420" cy="550" rx="6" ry="9" fill="#141208" stroke="#2A2820" strokeWidth="0.5" />
          <ellipse cx="1412" cy="720" rx="4" ry="6" fill="#161410" stroke="#282620" strokeWidth="0.5" />
          <path d="M1400,230 Q1392,224 1388,216" fill="none" stroke="#1C1A10" strokeWidth="3" strokeLinecap="round" />
          <path d="M1400,620 Q1394,616 1390,610" fill="none" stroke="#1C1A10" strokeWidth="2.5" strokeLinecap="round" />
          <path d="M1430,160 Q1434,200 1426,230" fill="none" stroke="#282620" strokeWidth="0.4" />
          <path d="M1410,420 Q1414,460 1408,490" fill="none" stroke="#282620" strokeWidth="0.4" />
        </svg>

        {/* Glowing eyes in the darkness */}
        <div className="campsite-eyes" style={{ transform: `translate(calc(var(--mouse-x) * -12px), calc(var(--mouse-y) * -5px))` }}>
          {eyes.map((e, i) => (
            <div
              key={i}
              className="forest-eyes"
              style={{
                left: `${e.x}%`,
                top: `${e.y}%`,
                animationDelay: `${e.delay}s`,
                animationDuration: `${e.duration}s`,
              }}
            >
              <div className="forest-eye" style={{ width: e.size, height: e.size * 0.6, background: e.color, boxShadow: `0 0 ${e.size * 3}px ${e.color}` }} />
              <div className="forest-eye" style={{ width: e.size, height: e.size * 0.6, background: e.color, boxShadow: `0 0 ${e.size * 3}px ${e.color}`, marginLeft: e.gap }} />
            </div>
          ))}
        </div>

        {/* Forest floor — dark earth */}
        <div className="parallax-layer campsite-floor" />

        {/* Ground detail — sharp SVG rocks, roots, twigs */}
        <svg className="campsite-ground-detail" viewBox="0 0 1440 450" preserveAspectRatio="none">
          {/* Top zone — exposed roots and rocks */}
          <path d="M0,20 Q40,15 80,22 Q120,28 160,18" fill="none" stroke="#2A2820" strokeWidth="2.5" />
          <path d="M0,22 Q30,18 60,24" fill="none" stroke="#2E2C22" strokeWidth="1.5" />
          <path d="M1280,30 Q1320,24 1360,32 Q1400,38 1440,28" fill="none" stroke="#2A2820" strokeWidth="2.5" />
          <path d="M1350,32 Q1390,26 1440,34" fill="none" stroke="#2E2C22" strokeWidth="1.5" />
          <polygon points="200,40 208,34 218,38 215,46 205,48" fill="#1E1C16" stroke="#2A2820" strokeWidth="0.8" />
          <polygon points="520,25 526,20 534,23 532,30 522,31" fill="#1C1A14" stroke="#282620" strokeWidth="0.8" />
          <polygon points="850,45 860,38 872,42 868,52 854,54" fill="#1E1C16" stroke="#2A2820" strokeWidth="0.8" />
          <polygon points="1100,30 1106,24 1114,28 1110,36 1102,37" fill="#1C1A14" stroke="#282620" strokeWidth="0.8" />

          {/* Mid zone — more rocks, twigs, roots */}
          <path d="M100,140 Q160,132 220,145 Q280,150 340,138" fill="none" stroke="#2A2820" strokeWidth="2" />
          <path d="M900,160 Q960,152 1020,165 Q1080,170 1140,155" fill="none" stroke="#2E2C22" strokeWidth="2" />
          <polygon points="380,150 386,144 392,148 390,156 382,157" fill="#1A1816" stroke="#262420" strokeWidth="0.8" />
          <polygon points="680,130 688,124 696,128 693,136 682,137" fill="#1E1C16" stroke="#2A2820" strokeWidth="0.8" />
          <polygon points="1000,170 1004,164 1012,168 1010,176 1002,177" fill="#1C1A14" stroke="#262420" strokeWidth="0.8" />
          <polygon points="150,180 158,172 168,178 164,188 154,190" fill="#1E1C16" stroke="#2A2820" strokeWidth="0.8" />
          <line x1="450" y1="145" x2="490" y2="140" stroke="#2A2820" strokeWidth="1.5" strokeLinecap="round" />
          <line x1="750" y1="160" x2="780" y2="155" stroke="#2C2A1E" strokeWidth="1" strokeLinecap="round" />
          <line x1="1200" y1="135" x2="1230" y2="130" stroke="#2C2A1E" strokeWidth="1" strokeLinecap="round" />
          <g opacity="0.7">
            <line x1="300" y1="170" x2="308" y2="166" stroke="#1E2E16" strokeWidth="0.8" />
            <line x1="302" y1="172" x2="310" y2="170" stroke="#1E2E16" strokeWidth="0.8" />
            <line x1="304" y1="168" x2="312" y2="166" stroke="#1E2E16" strokeWidth="0.8" />
          </g>

          {/* Lower-mid zone */}
          <path d="M0,250 Q100,244 200,255 Q300,260 400,248" fill="none" stroke="#2A2820" strokeWidth="2" />
          <path d="M1040,260 Q1140,252 1240,265 Q1340,270 1440,258" fill="none" stroke="#2E2C22" strokeWidth="2" />
          <polygon points="560,240 568,234 578,238 574,248 564,250" fill="#1E1C16" stroke="#2A2820" strokeWidth="0.8" />
          <polygon points="820,270 826,264 834,268 830,276 822,278" fill="#1C1A14" stroke="#282620" strokeWidth="0.8" />
          <polygon points="260,280 268,274 276,278 273,286 262,287" fill="#1A1816" stroke="#262420" strokeWidth="0.8" />
          <line x1="150" y1="260" x2="195" y2="256" stroke="#2C2A1E" strokeWidth="1.5" strokeLinecap="round" />
          <line x1="650" y1="250" x2="690" y2="245" stroke="#2A2820" strokeWidth="1.5" strokeLinecap="round" />
          <line x1="1050" y1="275" x2="1085" y2="270" stroke="#2A2820" strokeWidth="1.5" strokeLinecap="round" />
          <g opacity="0.6">
            <line x1="950" y1="260" x2="958" y2="256" stroke="#1E2E16" strokeWidth="0.8" />
            <line x1="952" y1="262" x2="960" y2="260" stroke="#1E2E16" strokeWidth="0.8" />
            <line x1="948" y1="258" x2="956" y2="256" stroke="#1E2E16" strokeWidth="0.8" />
          </g>

          {/* Bottom zone */}
          <path d="M200,360 Q300,354 400,365 Q500,370 600,358" fill="none" stroke="#2A2820" strokeWidth="2" />
          <path d="M800,380 Q900,372 1000,385 Q1100,390 1200,378" fill="none" stroke="#2E2C22" strokeWidth="2" />
          <polygon points="100,370 108,364 118,368 114,378 104,380" fill="#1E1C16" stroke="#2A2820" strokeWidth="0.8" />
          <polygon points="440,390 446,384 454,388 450,396 442,398" fill="#1C1A14" stroke="#282620" strokeWidth="0.8" />
          <polygon points="720,350 728,344 738,348 734,358 724,360" fill="#1A1816" stroke="#262420" strokeWidth="0.8" />
          <polygon points="1300,375 1308,368 1318,374 1314,384 1304,386" fill="#1E1C16" stroke="#2A2820" strokeWidth="0.8" />
          <line x1="300" y1="400" x2="345" y2="396" stroke="#2C2A1E" strokeWidth="1.5" strokeLinecap="round" />
          <line x1="550" y1="380" x2="590" y2="375" stroke="#2A2820" strokeWidth="1" strokeLinecap="round" />
          <line x1="1100" y1="410" x2="1135" y2="405" stroke="#2A2820" strokeWidth="1.5" strokeLinecap="round" />
          <g opacity="0.65">
            <line x1="600" y1="420" x2="608" y2="416" stroke="#1E2E16" strokeWidth="0.8" />
            <line x1="602" y1="422" x2="610" y2="420" stroke="#1E2E16" strokeWidth="0.8" />
            <line x1="598" y1="418" x2="606" y2="416" stroke="#1E2E16" strokeWidth="0.8" />
          </g>

          {/* Subtle ground variation lines spanning full height */}
          <path d="M0,10 Q200,8 400,12 Q600,16 800,10 Q1000,6 1200,14 Q1400,10 1440,12" fill="none" stroke="#201E16" strokeWidth="1" opacity="0.7" />
          <path d="M0,120 Q300,115 600,125 Q900,130 1200,118 Q1350,114 1440,120" fill="none" stroke="#1E1C14" strokeWidth="0.8" opacity="0.5" />
          <path d="M0,230 Q250,225 500,235 Q750,240 1000,228 Q1250,224 1440,232" fill="none" stroke="#201E16" strokeWidth="0.8" opacity="0.5" />
          <path d="M0,340 Q200,335 400,345 Q600,350 800,338 Q1000,334 1200,342 Q1400,338 1440,340" fill="none" stroke="#1E1C14" strokeWidth="0.8" opacity="0.4" />
        </svg>

        {/* Mist layer */}
        <div className="campsite-mist" />
      </div>
    );
  }

  return (
    <div ref={containerRef} className={`parallax-bg parallax-bg--${variant}`}>
      {/* Stars layer */}
      <div className="parallax-layer parallax-layer--stars">
        {stars.map((s, i) => (
          <div
            key={i}
            className="star"
            style={{
              left: `${s.left}%`,
              top: `${s.top}%`,
              animationDelay: `${s.delay}s`,
              animationDuration: `${s.duration}s`,
              width: `${s.size}px`,
              height: `${s.size}px`,
            }}
          />
        ))}
      </div>

      {/* Far mountains */}
      <svg className="parallax-layer parallax-layer--mountains-far" viewBox="0 0 1440 400" preserveAspectRatio="none">
        <path d="M0,400 L0,280 Q120,180 240,250 Q360,160 480,220 Q600,120 720,200 Q840,100 960,180 Q1080,80 1200,160 Q1320,120 1440,200 L1440,400 Z" />
      </svg>

      {/* Near mountains */}
      <svg className="parallax-layer parallax-layer--mountains-near" viewBox="0 0 1440 400" preserveAspectRatio="none">
        <path d="M0,400 L0,300 Q100,220 200,280 Q320,180 440,260 Q560,160 680,240 Q800,140 920,230 Q1040,170 1160,250 Q1280,200 1440,280 L1440,400 Z" />
      </svg>

      {/* Treeline */}
      <svg className="parallax-layer parallax-layer--trees" viewBox="0 0 1440 300" preserveAspectRatio="none">
        <path d="M0,300 L0,180 Q30,140 60,170 Q80,120 100,160 Q130,100 160,150 Q180,110 200,155 Q230,80 260,140 Q290,100 320,150 Q350,70 380,130 Q410,90 440,145 Q470,60 500,130 Q530,80 560,140 Q590,50 620,120 Q650,70 680,135 Q710,40 740,110 Q770,60 800,130 Q830,50 860,120 Q890,70 920,140 Q950,40 980,120 Q1010,70 1040,135 Q1070,50 1100,125 Q1130,80 1160,140 Q1190,60 1220,130 Q1250,90 1280,145 Q1310,70 1340,130 Q1370,100 1400,150 Q1420,120 1440,160 L1440,300 Z" />
      </svg>

      {/* Ground */}
      <div className="parallax-layer parallax-layer--ground" />

      {/* Floating leaves */}
      {leaves.map((l, i) => (
        <div
          key={i}
          className="floating-leaf"
          style={{
            left: `${l.left}%`,
            bottom: `${l.bottom}%`,
            animationDelay: `${l.delay}s`,
            animationDuration: `${l.duration}s`,
          }}
        >
          <svg width="16" height="16" viewBox="0 0 16 16">
            <path d="M8,0 Q12,4 14,8 Q12,12 8,16 Q4,12 2,8 Q4,4 8,0Z" fill="var(--sage)" opacity="0.6" />
          </svg>
        </div>
      ))}
    </div>
  );
}
