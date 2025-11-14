# Specification: Scala Adoption Tracker Website

## Overview

Build a simple, maintainable website that lists **Scala adoption across companies and projects**, crowdsourced via GitHub.

Key goals:

- Data is stored as **YAML files** in a `/adopters` director
- A **generator script** produces the homepage Markdown file (`src/pages/index.md`).
- A **Docusaurus 3** site serves the website
- A **GitHub Action** regenerates `src/pages/index.md` on changes to `/adopters/**`.

The result: a public, easy-to-contribute “Scala Adoption Tracker” page.

---

## Tech Stack

- **Static site**: Docusaurus 3
- **Data storage**: YAML files (`/adopters/*.yaml`)
- **Generator script**: Scala using scala-cli
- **CI**: GitHub Actions

## Data model

Each company entry should capture the following data points

- Name
- Logo URL
- website url
- Usage description
- scala 3 adoption status (not planned, planned, partial, full)
- sources - list of links or other text to support the adoption status
- size of the company - so we can order by this
- category - product company, OSS project, consulting company


## Other requirements

- setup should be as simple as possible
- website should look reasonably well and clean but without much custom css. Unless its some kind fo docosaurus theme.\
- there should be a note somewhere about the purpose of the site - infortm the visitor what they are looking at
