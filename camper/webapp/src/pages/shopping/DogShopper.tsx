import "./DogShopper.css";

/**
 * The shopping list's "everything is bought" celebration: the import's dog
 * (same head, hat and colours as `DogChef`, so he is recognisably the same
 * dog), now beaming — eyes closed in a happy squint, tongue out — with a
 * paper grocery bag hugged in both paws, a baguette, a carrot and a bunch
 * of greens poking out of the top. He bobs on the spot, his ears flap and
 * his tail wags behind the bag. Pure SVG + CSS keyframes (DogShopper.css),
 * decorative: the "Everything is bought" text beside him is what a screen
 * reader hears.
 */
export function DogShopper() {
  return (
    <svg
      className="dog-shopper"
      viewBox="0 0 200 200"
      aria-hidden="true"
      focusable="false"
    >
      <g className="dog-shopper__bob">
        {/* Tail, behind everything, wagging. */}
        <g className="dog-shopper__tail">
          <path
            className="dog-shopper__tail-fur"
            d="M126 160 q34 -8 36 -40 q8 32 -22 54 z"
          />
        </g>

        {/* Body: a plump tummy reaching up under the chin and out past the
            bag, with the arms coming down its sides to the paws on the bag —
            so head, body, bag and tail read as one dog. */}
        <ellipse
          className="dog-shopper__fur"
          cx="100"
          cy="152"
          rx="38"
          ry="44"
        />
        <ellipse
          className="dog-shopper__belly"
          cx="100"
          cy="154"
          rx="24"
          ry="28"
        />
        <ellipse
          className="dog-shopper__arm"
          cx="66"
          cy="150"
          rx="7"
          ry="20"
          transform="rotate(-12 66 150)"
        />
        <ellipse
          className="dog-shopper__arm"
          cx="134"
          cy="150"
          rx="7"
          ry="20"
          transform="rotate(12 134 150)"
        />

        {/* Head. */}
        <g transform="translate(100 84)">
          <g className="dog-shopper__head">
            <g className="dog-shopper__ear-flap dog-shopper__ear-flap--left">
              <ellipse
                className="dog-shopper__ear"
                cx="-36"
                cy="6"
                rx="11"
                ry="24"
                transform="rotate(18 -36 6)"
              />
            </g>
            <g className="dog-shopper__ear-flap dog-shopper__ear-flap--right">
              <ellipse
                className="dog-shopper__ear"
                cx="36"
                cy="6"
                rx="11"
                ry="24"
                transform="rotate(-18 36 6)"
              />
            </g>
            <ellipse
              className="dog-shopper__fur"
              cx="0"
              cy="0"
              rx="36"
              ry="31"
            />
            <ellipse
              className="dog-shopper__patch"
              cx="15"
              cy="-6"
              rx="12"
              ry="13"
            />

            {/* Happy squint: two upturned arcs where the eyes were. */}
            <path className="dog-shopper__line" d="M-21 -4 q7 -9 14 0" />
            <path className="dog-shopper__line" d="M7 -4 q7 -9 14 0" />
            {/* Blush. */}
            <ellipse
              className="dog-shopper__blush"
              cx="-24"
              cy="8"
              rx="6"
              ry="3.5"
            />
            <ellipse
              className="dog-shopper__blush"
              cx="24"
              cy="8"
              rx="6"
              ry="3.5"
            />

            <ellipse
              className="dog-shopper__muzzle"
              cx="0"
              cy="13"
              rx="17"
              ry="12"
            />
            <ellipse
              className="dog-shopper__ink"
              cx="0"
              cy="7"
              rx="6"
              ry="4.3"
            />
            {/* A wide smile, and the tongue hanging out of it. */}
            <path className="dog-shopper__line" d="M-9 15 q9 9 18 0" />
            <g className="dog-shopper__tongue-swing">
              <path
                className="dog-shopper__tongue"
                d="M-3 19 h8 v7 a4 4 0 0 1 -8 0 z"
              />
              <path className="dog-shopper__tongue-line" d="M1 20 v5" />
            </g>

            <g transform="translate(-6 -1) rotate(-11) scale(0.92)">
              <g className="dog-shopper__hat">
                <circle
                  className="dog-shopper__hat-outline"
                  cx="-15"
                  cy="-44"
                  r="12"
                />
                <circle
                  className="dog-shopper__hat-outline"
                  cx="0"
                  cy="-50"
                  r="15"
                />
                <circle
                  className="dog-shopper__hat-outline"
                  cx="15"
                  cy="-44"
                  r="12"
                />
                <circle
                  className="dog-shopper__hat-fill"
                  cx="-15"
                  cy="-44"
                  r="11.2"
                />
                <circle
                  className="dog-shopper__hat-fill"
                  cx="0"
                  cy="-50"
                  r="14.2"
                />
                <circle
                  className="dog-shopper__hat-fill"
                  cx="15"
                  cy="-44"
                  r="11.2"
                />
                <rect
                  className="dog-shopper__hat-outline"
                  x="-22"
                  y="-37"
                  width="44"
                  height="11"
                  rx="2.5"
                />
              </g>
            </g>
          </g>
        </g>

        {/* The groceries and the bag sit low enough to leave the smile clear. */}
        <g transform="translate(0 14)">
          <g className="dog-shopper__groceries">
            <path
              className="dog-shopper__baguette"
              d="M74 128 q6 -26 14 -34 q6 4 4 12 q-6 10 -10 24 z"
            />
            <path
              className="dog-shopper__baguette-line"
              d="M83 104 l4 2 M80 112 l4 2 M77 120 l4 2"
            />
            <path
              className="dog-shopper__greens"
              d="M100 128 q-12 -16 -4 -30 q10 4 10 18 q10 -14 16 -4 q-4 12 -14 16 z"
            />
            <path
              className="dog-shopper__carrot"
              d="M118 130 l10 -26 l8 6 l-12 22 z"
            />
            <path
              className="dog-shopper__leaf"
              d="M128 104 q2 -10 10 -8 q-1 8 -8 10 z"
            />
          </g>

          {/* The paper bag, hugged in both paws. */}
          <path className="dog-shopper__bag" d="M66 128 h68 l-4 54 h-60 z" />
          <path
            className="dog-shopper__bag-fold"
            d="M66 128 h68 l-2 8 h-64 z"
          />
          <path className="dog-shopper__bag-line" d="M78 140 v36" />
          <ellipse
            className="dog-shopper__paw"
            cx="66"
            cy="152"
            rx="11"
            ry="8"
            transform="rotate(-12 66 152)"
          />
          <ellipse
            className="dog-shopper__paw"
            cx="134"
            cy="152"
            rx="11"
            ry="8"
            transform="rotate(12 134 152)"
          />
        </g>
      </g>

      {/* Little sparkles of a job done. */}
      <g className="dog-shopper__sparkle dog-shopper__sparkle--1">
        <path
          className="dog-shopper__sparkle-shape"
          d="M30 60 l2 5 l5 2 l-5 2 l-2 5 l-2 -5 l-5 -2 l5 -2 z"
        />
      </g>
      <g className="dog-shopper__sparkle dog-shopper__sparkle--2">
        <path
          className="dog-shopper__sparkle-shape"
          d="M170 48 l2 5 l5 2 l-5 2 l-2 5 l-2 -5 l-5 -2 l5 -2 z"
        />
      </g>
      <g className="dog-shopper__sparkle dog-shopper__sparkle--3">
        <path
          className="dog-shopper__sparkle-shape"
          d="M160 110 l1.5 4 l4 1.5 l-4 1.5 l-1.5 4 l-1.5 -4 l-4 -1.5 l4 -1.5 z"
        />
      </g>
    </svg>
  );
}
