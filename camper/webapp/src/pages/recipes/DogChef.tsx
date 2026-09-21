import './DogChef.css';

/**
 * The recipe import's waiting animation: a slightly dim dog in a chef's hat
 * peeking over a cookbook — held upside down — while ingredients float out of
 * it. Pure SVG + CSS keyframes (DogChef.css) — no images, no library — and
 * decorative: the status text beside it is what a screen reader hears.
 * Colours are Radix tokens only, so it follows the theme like everything else.
 *
 * How goofy he is was chosen by the owner between a tidy version (dot eyes,
 * straight hat, book the right way up) and "a cute dumb dumb" (eyes of two
 * sizes wandering separately, an ear stuck out sideways, a tiny hat, a long
 * swinging tongue, ducking right out of sight), which was a little too dumb:
 * this is the one in between. Two other scenes were tried and dropped
 * (sitting and turning pages; stirring a pot with the book in one paw).
 */
export function DogChef() {
  return (
    <svg className="dog-chef" viewBox="0 0 200 176" aria-hidden="true" focusable="false">
      {/* Everything that ducks behind the book and pops back up. */}
      <g className="dog-chef__peek">
        <g transform="translate(100 84)">
          <g className="dog-chef__head">
            {/* Animated wrappers: a CSS transform would replace an element's own transform attribute. */}
            <g className="dog-chef__ear-perk">
              <ellipse className="dog-chef__ear" cx="-36" cy="6" rx="11" ry="24" transform="rotate(18 -36 6)" />
            </g>
            <g className="dog-chef__ear-droop">
              <ellipse className="dog-chef__ear" cx="36" cy="6" rx="11" ry="24" transform="rotate(-18 36 6)" />
            </g>
            <ellipse className="dog-chef__fur" cx="0" cy="0" rx="36" ry="31" />
            <ellipse className="dog-chef__patch" cx="15" cy="-6" rx="12" ry="13" />

            {/* Reading along together, then cross-eyed for a moment (the __cross wrappers). */}
            <g className="dog-chef__eye">
              <circle className="dog-chef__eyeball" cx="-14" cy="-5" r="7.6" />
              <g className="dog-chef__cross dog-chef__cross--left">
                <g className="dog-chef__pupil">
                  <circle className="dog-chef__ink" cx="-14" cy="-5" r="3.3" />
                  <circle className="dog-chef__shine" cx="-13" cy="-6.2" r="1.05" />
                </g>
              </g>
            </g>
            <g className="dog-chef__eye">
              <circle className="dog-chef__eyeball" cx="14" cy="-5" r="7.6" />
              <g className="dog-chef__cross dog-chef__cross--right">
                <g className="dog-chef__pupil">
                  <circle className="dog-chef__ink" cx="14" cy="-5" r="3.3" />
                  <circle className="dog-chef__shine" cx="15" cy="-6.2" r="1.05" />
                </g>
              </g>
            </g>
            <g className="dog-chef__brow">
              <path className="dog-chef__line" d="M-22 -16 q7 -6 14 -1" />
            </g>
            <path className="dog-chef__line" d="M8 -16 q6 -4 12 0" />

            <ellipse className="dog-chef__muzzle" cx="0" cy="13" rx="17" ry="12" />
            <ellipse className="dog-chef__ink" cx="0" cy="7" rx="6" ry="4.3" />

            <g transform="translate(-6 -1) rotate(-11) scale(0.92)">
              <g className="dog-chef__hat">
                <circle className="dog-chef__hat-outline" cx="-15" cy="-44" r="12" />
                <circle className="dog-chef__hat-outline" cx="0" cy="-50" r="15" />
                <circle className="dog-chef__hat-outline" cx="15" cy="-44" r="12" />
                {/* The same puffs again without a stroke, to hide the outlines where they overlap. */}
                <circle className="dog-chef__hat-fill" cx="-15" cy="-44" r="11.2" />
                <circle className="dog-chef__hat-fill" cx="0" cy="-50" r="14.2" />
                <circle className="dog-chef__hat-fill" cx="15" cy="-44" r="11.2" />
                <rect className="dog-chef__hat-outline" x="-22" y="-37" width="44" height="11" rx="2.5" />
              </g>
            </g>
          </g>
        </g>
      </g>

      {/* Ingredients drift up from behind the book, one after another. */}
      <g className="dog-chef__floaty dog-chef__floaty--1">
        <circle className="dog-chef__tomato" cx="38" cy="90" r="7" />
        <path className="dog-chef__leaf" d="M33 84 q5 -5 10 0 q-5 3 -10 0 z" />
      </g>
      <g className="dog-chef__floaty dog-chef__floaty--2">
        <path className="dog-chef__carrot" d="M155 82 l12 4 l-16 14 z" />
        <path className="dog-chef__leaf" d="M161 83 q3 -8 9 -6 q-1 6 -7 8 z" />
      </g>
      <g className="dog-chef__floaty dog-chef__floaty--3">
        <ellipse className="dog-chef__egg" cx="50" cy="90" rx="6" ry="7.5" />
      </g>
      <g className="dog-chef__floaty dog-chef__floaty--4">
        <path className="dog-chef__leaf" d="M146 96 q2 -12 12 -12 q0 11 -12 12 z" />
      </g>

      <polygon className="dog-chef__page" points="24,92 100,100 176,92 178,96 100,104 22,96" />
      <polygon className="dog-chef__cover" points="22,96 100,104 100,172 22,164" />
      <polygon className="dog-chef__cover" points="178,96 100,104 100,172 178,164" />
      <rect className="dog-chef__spine" x="97" y="103" width="6" height="69" />
      {/* Upside down, as he is holding it. */}
      <g transform="translate(139 134) skewY(-5.9) rotate(180)">
        <rect className="dog-chef__label" x="-26" y="-15" width="52" height="30" rx="4" />
        <text className="dog-chef__label-text" x="0" y="0" textAnchor="middle">
          RECIPES
        </text>
        <line className="dog-chef__label-line" x1="-14" y1="7" x2="14" y2="7" />
      </g>
      <g className="dog-chef__pawprint" transform="translate(61 136) skewY(5.9) rotate(180)">
        <ellipse cx="0" cy="4" rx="9" ry="7.5" />
        <circle cx="-10" cy="-7" r="3.6" />
        <circle cx="0" cy="-11" r="3.6" />
        <circle cx="10" cy="-7" r="3.6" />
      </g>

      {/* The tongue hangs over the front of the book, so it is drawn after it. It follows the head, and is pulled in while he is down. */}
      <g className="dog-chef__tongue-peek">
        <g className="dog-chef__tongue-swing">
          <path className="dog-chef__tongue" d="M100 98 h10 v8 a5 5 0 0 1 -10 0 z" />
          <path className="dog-chef__tongue-line" d="M105 100 v6" />
        </g>
      </g>

      <ellipse className="dog-chef__paw" cx="60" cy="100" rx="10" ry="7" />
      <ellipse className="dog-chef__paw" cx="140" cy="100" rx="10" ry="7" />
    </svg>
  );
}
