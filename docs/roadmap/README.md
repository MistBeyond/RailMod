# Roadmap Docs Overview and Content Ownership

This document is the **index of the `roadmap/` directory**: the overview of what each direct entry contains and the
authority for what content belongs in each of them. Read it before adding or moving documentation. In short, this README
is the **metadata of this documentation subtree**.

## Purpose

`roadmap/` holds the per-mode detail documents behind `docs/roadmap.md`: the railway mode (`rail`) detail and binding
contracts of this project. The implementation order itself lives in `docs/roadmap.md`; this subtree only holds the
detail that each
entry references.

## Overview

| Path        | Purpose                                                               |
|-------------|-----------------------------------------------------------------------|
| `README.md` | This document: directory overview and content ownership rules         |
| `rail.md`   | Railway mode overview and index; roadmap node 1                       |
| `rail/`     | Railway contracts and implementation notes; index in `rail/README.md` |

## Content Ownership

| Document    | Belongs here                                                                   | Does not belong here                                                                   |
|-------------|--------------------------------------------------------------------------------|----------------------------------------------------------------------------------------|
| `rail.md`   | Railway overview, design summary, links to the `rail/` contracts               | Contract detail (→ `rail/`), implementation order (→ `docs/roadmap.md`)                |
| `rail/`     | Railway binding contracts and implementation notes (owned by `rail/README.md`) | Global rules                                                                           |

## Ownership Rules

1. This directory mirrors the nodes in `docs/roadmap.md`; node status and implementation order live there — do not
   duplicate them here.
2. Each mode document carries that mode's gameplay and implementation detail only; binding contracts for a mode live in
   that mode's own subdirectory (e.g. `rail/`), and accepted user rulings live in `docs/decisions/`.
3. Binding contracts win over concept notes on conflict; see the document hierarchy in the document-editing-rules skill.
4. `rail/` content is owned by `rail/README.md`; this README indexes direct entries only.
5. Terminology stays consistent with the rest of the docs (e.g. `World Grid`, simple/complex cells, baked-model route,
   `TrackPlacement`).