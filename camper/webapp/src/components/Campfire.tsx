import './Campfire.css';

export function Campfire() {
  return (
    <div className="campfire-container">
      {/* Ground glow */}
      <div className="campfire-ground-glow" />

      {/* Smoke */}
      <div className="campfire-smoke">
        {Array.from({ length: 4 }).map((_, i) => (
          <div
            key={i}
            className="smoke-particle"
            style={{
              animationDelay: `${i * 0.8}s`,
              left: `${45 + Math.random() * 10}%`,
            }}
          />
        ))}
      </div>

      {/* Flames */}
      <div className="campfire-flames">
        <div className="flame flame--outer" />
        <div className="flame flame--mid" />
        <div className="flame flame--inner" />
        <div className="flame flame--core" />
      </div>

      {/* Embers */}
      <div className="campfire-embers">
        {Array.from({ length: 8 }).map((_, i) => (
          <div
            key={i}
            className="ember"
            style={{
              animationDelay: `${i * 0.5}s`,
              animationDuration: `${1.5 + Math.random() * 1.5}s`,
              left: `${30 + Math.random() * 40}%`,
            }}
          />
        ))}
      </div>

      {/* Logs */}
      <div className="campfire-logs">
        <div className="log log--1" />
        <div className="log log--2" />
        <div className="log log--3" />
      </div>

      {/* Stones ring — elliptical for 3D perspective */}
      <div className="campfire-stones">
        {Array.from({ length: 10 }).map((_, i) => {
          const angle = (i / 10) * Math.PI * 2;
          const rx = 55;
          const ry = 28;
          const x = Math.cos(angle) * rx;
          const y = Math.sin(angle) * ry;
          return (
            <div
              key={i}
              className="stone"
              style={{
                transform: `translate(${x}px, ${y}px)`,
              }}
            />
          );
        })}
      </div>
    </div>
  );
}
