# visual-tests

[BackstopJS](https://github.com/garris/BackstopJS) visual regression tests for the product list / home page (`/`). This is a separate npm project — it is **not** part of the `frontend` build.

## Why this page

It's the highest-traffic page (every visitor hits it, no login required), it has a genuinely responsive Bootstrap grid (1 column on mobile → 2 on tablet → 4 on desktop) that's exactly the kind of layout worth catching breakage in across viewports, and its content is deterministic: products are seeded consistently and each one's image comes from a fixed per-product seed (`picsum.photos/seed/mouse/...` etc.), so the same product always renders the same image. Pages like Cart/Checkout/Order History depend on live, per-run data (order IDs, timestamps) that would need masking to avoid constant false-positive diffs — this page doesn't have that problem.

## Prerequisites

- Node.js 18+ and npm
- A local Chrome/Chromium install
- The backend and frontend dev servers running

## Setup

```bash
npm install
```

Puppeteer normally downloads its own bundled Chromium on install. If that's blocked in your environment (some npm configs restrict install scripts) and `npm test` fails with a "Could not find Chromium" error, point Puppeteer at your local Chrome install instead — no config changes needed, this is a standard Puppeteer environment variable:

```bash
# Windows (PowerShell)
$env:PUPPETEER_EXECUTABLE_PATH = "C:\Program Files\Google\Chrome\Application\chrome.exe"

# macOS
export PUPPETEER_EXECUTABLE_PATH="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"

# Linux
export PUPPETEER_EXECUTABLE_PATH="/usr/bin/google-chrome"
```

## Running

```bash
cd backend && ./mvnw spring-boot:run     # in one terminal
cd frontend && npm run dev               # in another

cd visual-tests                          # in another
npm test
```

`npm test` compares the current page against the committed baseline in `backstop_data/bitmaps_reference/` and opens an HTML report (`backstop_data/html_report/index.html`) showing any diffs side by side.

## Updating the baseline

When a UI change is intentional, regenerate the reference images and commit them:

```bash
npm run reference
```

Or, after reviewing a `npm test` failure in the HTML report and confirming the new look is correct, approve it directly instead of regenerating from scratch:

```bash
npm run approve
```

Either way, the updated PNGs under `backstop_data/bitmaps_reference/` need to be committed — they're the source of truth future runs compare against, unlike everything else under `backstop_data/`, which is a regenerated, gitignored artifact.

## What's covered

One scenario ("Product List - Home Page") captured at three viewports chosen to land on each of the grid's Bootstrap breakpoints:

| Viewport | Width | Grid columns |
|----------|-------|--------------|
| mobile   | 400px | 1 |
| tablet   | 700px | 2 |
| desktop  | 1280px | 4 |

The scenario waits for `.card` to appear (`readySelector`) plus a 1s delay before capturing, so the async product fetch and images have time to load.

**Verified this actually catches regressions**: temporarily changed the "Add to cart" button from `btn-primary` to `btn-danger`, reran `npm test`, and got a clean failure (`0 Passed, 3 Failed`, ~8% content mismatch on every viewport) instead of a false pass. Reverted afterward.
