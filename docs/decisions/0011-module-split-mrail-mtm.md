# ADR 0011: Module Split — mrail and mtm as Independent Mods

Status: Accepted (rev. 2026-08-24)

Date: 2026-08-24

Revised: 2026-08-24 — `docs/roadmap/main/` was extracted into the new TransportMod repository (the re-created main-mod
project); the documentation indexes in this repository were updated accordingly.

Revised: 2026-08-24 — Removed the stale related-document link `docs/roadmap/main/README.md` below; the packed
main-mod material now lives in the TransportMod repository.

## Context

Railway transport overlaps with the Create mod's railway features; keeping both in one mod is not worth the conflict
cost. The project is therefore split into separate mods. A three-way split (mtm / mrail / a shared common mod) was
considered because container freight was expected to be shared content. However, containers as entities (not MC
entities) make empty-container transport and dispatch hard, so the container-as-entity design is cancelled. With
containers gone, no content overlaps between the two mods, and a shared common artifact is unnecessary. The two mods
are fully independent.

## Decision

1. `mrail` and `mtm` are two fully independent NeoForge mods. There is no dependency between them, no shared dependency,
   and no common/shared artifact or module.
2. Container-as-entity is canceled. Cross-mod freight exchange is not designed as shared registered content or shared
   types; each mod owns its own cargo structures.
3. This repository (TransportMod) is finalized as the railway mod project: mod id `mrail` (display name
   "Mistbeyond's Rail", `m` = Mistbeyond).
4. The main-mod (`mtm`) documentation and template material is packed into `docs/roadmap/main/` as a temporary packing
   area and will be extracted by the user into a separate repository.
5. Existing ADRs (0001–0010) stay in this repository under `docs/decisions/`; they are not packed with the main-mod
   material. The main-mod repository records its own decisions after extraction.
6. ADR numbering stays global and continues from 0011; the revision mechanism is unchanged.

## Consequences

- The `docs/` tree becomes rail-only after the `docs/roadmap/main/` packing area is extracted and removed (removal
  requires user confirmation).
- Code-side follow-up (by others, not this documentation change): the railway project skeleton is rebuilt with mod id
  `mrail`, its own package roots and mod metadata; the main mod is re-created by the user from the packed material.
- Naming is user-chosen: `mrail` (railway) and `mtm` (main mod, existing name kept).

## Related Documents

- `docs/architecture.md`
- `docs/roadmap.md`
- `docs/roadmap/rail.md`