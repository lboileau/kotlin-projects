/**
 * SVG hair shapes for avatar rendering.
 * Head circle: cx=24, cy=14, r=11 → top of head is at y=3.
 * All hair must cover from at least y=2 down to the forehead line (~y=9-12).
 * Bottom edges curve to follow the forehead — high in center (~y=9), lower at temples (~y=12).
 */
export function AvatarHair({ style, color }: { style: string; color: string }) {
  // Base cap that fully covers the top of the head (y=2 to forehead)
  const baseCap = "M13,12 Q12,6 15,3 Q19,0 24,0 Q29,0 33,3 Q36,6 35,12 Q30,9 24,8 Q18,9 13,12 Z";

  switch (style) {
    case 'short':
      return (
        <g>
          <path d={baseCap} fill={color} />
        </g>
      );
    case 'long':
      return (
        <g>
          <path d="M13,12 Q12,6 15,2 Q19,-1 24,-1 Q29,-1 33,2 Q36,6 35,12 Q30,9 24,8 Q18,9 13,12 Z" fill={color} />
          <path d="M13,12 Q12,18 13,26" fill="none" stroke={color} strokeWidth="3" strokeLinecap="round" />
          <path d="M35,12 Q36,18 35,26" fill="none" stroke={color} strokeWidth="3" strokeLinecap="round" />
          <path d="M14,12 Q13,17 14,24" fill="none" stroke={color} strokeWidth="2" strokeLinecap="round" />
          <path d="M34,12 Q35,17 34,24" fill="none" stroke={color} strokeWidth="2" strokeLinecap="round" />
        </g>
      );
    case 'curly':
      return (
        <g>
          <path d={baseCap} fill={color} />
          <circle cx="13" cy="5" r="3.5" fill={color} />
          <circle cx="19" cy="1" r="3.5" fill={color} />
          <circle cx="29" cy="1" r="3.5" fill={color} />
          <circle cx="35" cy="5" r="3.5" fill={color} />
          <circle cx="11" cy="11" r="2.5" fill={color} />
          <circle cx="37" cy="11" r="2.5" fill={color} />
        </g>
      );
    case 'wavy':
      return (
        <g>
          <path d="M13,12 Q12,6 15,2 Q19,-1 24,-1 Q29,-1 33,2 Q36,6 35,12 Q30,9 24,8 Q18,9 13,12 Z" fill={color} />
          <path d="M13,12 Q11,16 13,22" fill="none" stroke={color} strokeWidth="2.5" strokeLinecap="round" />
          <path d="M35,12 Q37,16 35,22" fill="none" stroke={color} strokeWidth="2.5" strokeLinecap="round" />
          <path d="M16,3 Q20,0 25,1" fill="none" stroke={color} strokeWidth="3" strokeLinecap="round" />
        </g>
      );
    case 'buzz':
      return (
        <g>
          <path d="M14,11 Q13,6 16,3 Q20,1 24,1 Q28,1 32,3 Q35,6 34,11 Q30,9 24,8.5 Q18,9 14,11 Z" fill={color} />
        </g>
      );
    case 'mohawk':
      return (
        <g>
          {/* Tall center ridge — fully covers top */}
          <path d="M20,10 Q19,4 22,-2 Q24,-4 26,-2 Q29,4 28,10 Q26,8.5 24,8 Q22,8.5 20,10 Z" fill={color} />
          {/* Faint shaved sides covering scalp */}
          <path d="M14,11 Q13,6 16,3 Q19,1 20,10" fill={color} />
          <path d="M34,11 Q35,6 32,3 Q29,1 28,10" fill={color} />
        </g>
      );
    case 'ponytail':
      return (
        <g>
          <path d={baseCap} fill={color} />
          <circle cx="35" cy="6" r="2" fill={color} />
          <path d="M35,8 Q39,12 37,18 Q36,22 38,25" fill="none" stroke={color} strokeWidth="3" strokeLinecap="round" />
        </g>
      );
    case 'bun':
      return (
        <g>
          <path d={baseCap} fill={color} />
          <circle cx="24" cy="-2" r="5" fill={color} />
          <circle cx="24" cy="-2" r="3.5" fill={color} />
        </g>
      );
    default:
      return (
        <g>
          <path d={baseCap} fill={color} />
        </g>
      );
  }
}
