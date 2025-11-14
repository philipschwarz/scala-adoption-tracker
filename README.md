# Scala Adoption Tracker

A crowdsourced recorde of Scala adoption across companies and OSS projects.

## Adding or updating adopters

Each YAML file under `adopters/` should contain:

- `name` – company or project name
- `logoUrl` – absolute URL to a PNG/SVG logo
- `website` – canonical homepage
- `usage` – short explanation of how Scala is used
- `adoptionStatus` – one of `not planned`, `planned`, `partial`, `full`
- `sources` – list of links or short notes backing up the claim
- `size` – integer headcount (used for ordering)
- `category` – `product company`, `OSS project`, or `consulting company`

After editing YAML run `npm run generate:adopters` and commit both the data and the generated Markdown page. Every pull request also runs the generator via GitHub Actions to make sure the homepage stays in sync with the data.

## Setup

### Requirements

- Node.js 20+
- [Scala CLI](https://scala-cli.virtuslab.org/) 1.6+

### Dev Setup

```bash
npm install
npm run generate:adopters 
npm run start
```
