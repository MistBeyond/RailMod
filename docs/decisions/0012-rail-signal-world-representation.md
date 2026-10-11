# ADR 0012: Rail Signal World Representation — Independent Signal Block, Ghost Side, and Break-to-Remove

Status: Accepted

Date: 2026-08-28

## Context

The first signal implementation stored signal placements as track cell data only: a small placeholder post inside the
complex-cell mesh was the sole world visual, signals were removed through a non-standard sneak+use gesture on the item,
and the fixed signal-light template (`assets/mrail/models/block/block_signal.json`) had no rendering path at all. With
the template available, the world representation had to be decided. The user ruled (2026-08-28):

- the signal is an independent, breakable world block ("信号可以作为一个独立方块，这样就好拆了");
- the ghost preview's left/right side is toggled with shift+use, and already-placed signals are left untouched
  ("切换虚影左右侧，已经放下的不管他");
- the physical ERROR exclamation overlay is deferred until dedicated art exists ("暂缓物理感叹号").

The runtime contract keeps the signal placement data in the track cell (`TrackCellData` carries signal placement
information; a signal cell is a complex cell), and ADR 0006 mandates the baked-model route for complex-cell rendering.
The world block is the presentation of that data, not a second source of graph truth.

## Decision

- World form: a block signal is represented by its own block, `mrail:rail_block_signal`, standing in the World Grid
  cell adjacent to the guarded track cell — on the left or the right side of the facing direction. Left and right are
  defined looking along the facing direction; both sides are supported.
- The block state carries `facing` (the full 8-direction `GridDirection` of the signal), `side` (`LEFT`/`RIGHT`), and
  `aspect` (`RED`/`GREEN`). The guarded track cell is derived from position, facing, and side; no block entity is used.
- Rendering: the fixed template is mirrored element by element in a blockstate-driven custom unbaked model that renders
  through the same baked-model route as the track cells (ADR 0006 practice); the facing property rotates the model in
  45-degree steps, and the aspect property selects the lens materials — the lit lens is emitted light, the dark lens
  reuses the trim texture. The template file stays the fixed model source: it is referenced directly as the item model
  and is mirrored one-to-one in the rendering code (it is a fixed model, not a parametric family).
- Placement: right-clicking a track cell with the block signal item writes the signal placement into the track cell
  data and places the world block on the stack's ghost side, falling back to the opposite side when the preferred cell
  is blocked; if neither adjacent cell is free the placement is rolled back. The indication is resolved server-side at
  placement. Right-clicking a cell that already holds that block signal heals a missing world block instead.
- Ghost side: shift+use with the block signal item toggles the stack's ghost-preview side (`LEFT`/`RIGHT`,
  persisted and network-synchronized as an item data component); while aiming at a track cell, the ghost preview draws
  both candidate positions with the active side highlighted and a facing arrow. Already-placed signals are unaffected.
- Removal: breaking the signal block removes it and deletes the matching placement from the backing track cell; the
  block disappears automatically when its backing track cell is removed. The block signal item itself no longer removes
  signals (the path signal keeps the sneak+use removal because it has no world block).
- Aspect sync: the server pushes resolved RED/GREEN changes into the block's `aspect` property at most once per second
  through the vanilla block update path; a misconfigured signal renders as `RED` (see ADR 0010 revision).
- Legacy data-only block signal placements (from earlier saves) stay functional for the graph but have no world block;
  right-clicking their cell with the item re-creates the world block.

## Consequences

- `docs/roadmap/rail/sections.md` records the world form, the ghost side toggle, and break-to-removal in its confirmed
  directions, and drops the in-cell placeholder for block signals (the path signal keeps its placeholder post).
- ADR 0010 is revised: the physical exclamation overlay of the ERROR indicator is deferred; ERROR keeps behaving as
  `RED` and stays visible in the F3 debug overlay.
- The runtime contract needs no change: placement data still lives in the track cell, a signal cell is still a complex
  cell, and the ERROR-indicator MUST is unaffected (the F3 marker remains the indicator while the physical overlay is
  deferred).
- The signal block occupies its own block slot next to the track; parallel track one cell away blocks signal placement
  on that side. This is accepted adjacency behavior, not a new occupancy rule.
- Follow-up work: a dedicated path signal model (its world form will follow this ADR's pattern), and the physical ERROR
  overlay once dedicated art exists.

## Related Documents

- `docs/roadmap/rail/sections.md`
- `docs/roadmap/rail/runtime-contract.md`
- `docs/decisions/0006-complex-track-cell-rendering.md`
- `docs/decisions/0008-rail-signal-placement-conflict-and-error-state.md`
- `docs/decisions/0010-rail-signal-runtime-defaults.md`
