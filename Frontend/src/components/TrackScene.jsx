import React, { forwardRef, useImperativeHandle, useRef } from "react";
import { C, RADIUS } from "../theme.js";

/*
 * The world the race runs through.
 *
 * Sky, two skyline bands and the road surface all scroll left as the race
 * progresses, at different rates, so the strip reads as motion rather than a
 * dot sliding along a bar. Everything is driven by one 0..1 progress value —
 * the leading car's share of the run — and drawn as repeating SVG tiles so
 * there are no image assets to ship.
 */

const svgUrl = (svg) => `url("data:image/svg+xml,${encodeURIComponent(svg)}")`;

/* Distant towers: low contrast, barely moves, sits on the horizon. */
const FAR_SKYLINE = svgUrl(`
<svg xmlns='http://www.w3.org/2000/svg' width='560' height='130'>
  <g fill='#A6C9DC'>
    <rect x='8' y='58' width='46' height='72'/>
    <rect x='62' y='34' width='34' height='96'/>
    <rect x='104' y='70' width='58' height='60'/>
    <rect x='170' y='46' width='40' height='84'/>
    <rect x='222' y='64' width='30' height='66'/>
    <rect x='262' y='28' width='44' height='102'/>
    <rect x='316' y='74' width='52' height='56'/>
    <rect x='378' y='50' width='36' height='80'/>
    <rect x='424' y='66' width='48' height='64'/>
    <rect x='482' y='40' width='38' height='90'/>
  </g>
  <g fill='#C6E0EC'>
    <rect x='70' y='44' width='6' height='8'/><rect x='82' y='44' width='6' height='8'/>
    <rect x='70' y='60' width='6' height='8'/><rect x='82' y='60' width='6' height='8'/>
    <rect x='272' y='40' width='7' height='9'/><rect x='288' y='40' width='7' height='9'/>
    <rect x='272' y='58' width='7' height='9'/><rect x='288' y='58' width='7' height='9'/>
    <rect x='178' y='58' width='7' height='9'/><rect x='194' y='58' width='7' height='9'/>
    <rect x='490' y='54' width='7' height='9'/><rect x='504' y='54' width='7' height='9'/>
  </g>
</svg>`);

/* Nearer block: saturated, taller contrast, moves noticeably faster. */
const NEAR_SKYLINE = svgUrl(`
<svg xmlns='http://www.w3.org/2000/svg' width='420' height='120'>
  <g fill='#6FA7C0'>
    <rect x='0' y='72' width='54' height='48'/>
    <rect x='64' y='54' width='40' height='66'/>
    <rect x='118' y='80' width='66' height='40'/>
    <rect x='196' y='62' width='44' height='58'/>
    <rect x='252' y='84' width='58' height='36'/>
    <rect x='322' y='58' width='42' height='62'/>
    <rect x='374' y='78' width='40' height='42'/>
  </g>
  <g fill='#9FCADD'>
    <rect x='72' y='64' width='8' height='10'/><rect x='88' y='64' width='8' height='10'/>
    <rect x='72' y='82' width='8' height='10'/><rect x='88' y='82' width='8' height='10'/>
    <rect x='204' y='72' width='8' height='10'/><rect x='220' y='72' width='8' height='10'/>
    <rect x='330' y='68' width='8' height='10'/><rect x='346' y='68' width='8' height='10'/>
  </g>
  <g fill='#4E8AA4'>
    <rect x='158' y='44' width='4' height='40'/>
    <rect x='146' y='44' width='28' height='4'/>
    <rect x='149' y='52' width='22' height='3'/>
  </g>
</svg>`);

/* Roadside furniture at ground level — fastest layer, sells the speed. */
const ROADSIDE = svgUrl(`
<svg xmlns='http://www.w3.org/2000/svg' width='240' height='34'>
  <rect x='0' y='26' width='240' height='8' fill='#6FAE5C'/>
  <g fill='#84C36B'>
    <circle cx='34' cy='22' r='11'/><circle cx='46' cy='24' r='9'/>
    <circle cx='150' cy='23' r='10'/><circle cx='162' cy='25' r='8'/>
  </g>
  <g fill='#8C99A2'>
    <rect x='96' y='4' width='3' height='24'/>
    <rect x='90' y='4' width='15' height='3'/>
  </g>
</svg>`);

const SKY_H = 152;

/*
 * Driven imperatively during playback: the parent calls setProgress() on this
 * component's ref every animation frame and the parallax offsets are written
 * straight to the DOM, so a running race does not re-render the scene.
 */
const TrackScene = forwardRef(function TrackScene({ progress, running, children }, ref) {
  const p = Math.min(1, Math.max(0, progress || 0));
  /* Parallax: each layer travels a different distance over one full run. */
  const layers = [
    { img: FAR_SKYLINE, travel: 260, bottom: 0, height: 182, size: "784px 182px", opacity: 0.75 },
    { img: NEAR_SKYLINE, travel: 620, bottom: 0, height: 168, size: "588px 168px", opacity: 1 },
  ];
  const VERGE_TRAVEL = 1100;
  const DASH_TRAVEL = 1600;

  const skylineRefs = useRef([]);
  const vergeRef = useRef(null);
  const dashRef = useRef(null);

  useImperativeHandle(ref, () => ({
    setProgress(next) {
      const clamped = Math.min(1, Math.max(0, next || 0));
      skylineRefs.current.forEach((node, i) => {
        if (node) node.style.backgroundPositionX = `${-clamped * layers[i].travel}px`;
      });
      if (vergeRef.current) {
        vergeRef.current.style.backgroundPositionX = `${-clamped * VERGE_TRAVEL}px`;
      }
      if (dashRef.current) {
        dashRef.current.style.backgroundPositionX = `${-clamped * DASH_TRAVEL}px`;
      }
    },
  }), []);

  /* During the run the RAF drives every frame, so CSS must not also tween. */
  const glide = running ? "none" : "background-position-x .35s ease, transform .35s ease";

  return (
    <div style={{
      position: "relative",
      borderRadius: RADIUS,
      overflow: "hidden",
      border: `1px solid ${C.line}`,
      boxShadow: `inset 0 1px 0 #ffffff80`,
    }}>
      {/* Sky */}
      <div style={{
        position: "relative",
        height: SKY_H,
        background: `linear-gradient(180deg, ${C.skyTop} 0%, ${C.skyBottom} 100%)`,
        overflow: "hidden",
      }}>
        {/* Sun haze, fixed — it is far enough away not to slide. */}
        <div style={{
          position: "absolute", top: -44, right: 56, width: 132, height: 132,
          borderRadius: "50%", background: "#FFF4C2", opacity: 0.75, filter: "blur(2px)",
        }} />

        {layers.map((l, i) => (
          <div
            key={i}
            ref={(node) => { skylineRefs.current[i] = node; }}
            style={{
              position: "absolute", left: 0, right: 0, bottom: l.bottom, height: l.height,
              backgroundImage: l.img,
              backgroundRepeat: "repeat-x",
              backgroundSize: l.size,
              backgroundPositionX: `${-p * l.travel}px`,
              backgroundPositionY: "bottom",
              opacity: l.opacity,
              transition: glide,
            }}
          />
        ))}
      </div>

      {/* Roadside verge */}
      <div
        ref={vergeRef}
        style={{
          height: 46,
          backgroundImage: ROADSIDE,
          backgroundRepeat: "repeat-x",
          backgroundSize: "324px 46px",
          backgroundPositionX: `${-p * VERGE_TRAVEL}px`,
          backgroundColor: C.grass,
          transition: glide,
        }}
      />

      {/* Road surface, with the lanes laid on top of it */}
      <div style={{
        position: "relative",
        background: `linear-gradient(180deg, ${C.road} 0%, ${C.roadDark} 100%)`,
        padding: "20px 16px 18px",
      }}>
        {/* Scrolling lane dashes behind the cars */}
        <div
          aria-hidden
          ref={dashRef}
          style={{
            position: "absolute", left: 0, right: 0, top: "50%", height: 4, marginTop: -2,
            backgroundImage: `linear-gradient(90deg, ${C.roadLine} 0 46px, transparent 46px 92px)`,
            backgroundSize: "92px 4px",
            backgroundPositionX: `${-p * DASH_TRAVEL}px`,
            opacity: 0.38,
            transition: glide,
          }}
        />
        {children}
      </div>

      {/* Finish line */}
      <div aria-hidden style={{
        position: "absolute", top: SKY_H, right: 0, bottom: 0, width: 16,
        backgroundImage: `linear-gradient(45deg, ${C.text} 25%, transparent 25% 50%, ${C.text} 50% 75%, transparent 75%)`,
        backgroundSize: "16px 16px",
        backgroundColor: C.roadLine,
        opacity: 0.85,
      }} />
    </div>
  );
});

export default TrackScene;
