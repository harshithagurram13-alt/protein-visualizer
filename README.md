# ProteinViz v2 · Molecular Structure Explorer

> An AI-powered protein structure visualization tool with interactive 3D rendering, protein-protein interaction networks, AI-generated summaries, and mutation analysis.

![ProteinViz](https://img.shields.io/badge/version-2.0-39ffc8?style=flat-square) ![License](https://img.shields.io/badge/license-MIT-5b8dff?style=flat-square) ![Java](https://img.shields.io/badge/Java-17-ff5e8a?style=flat-square) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-6db33f?style=flat-square)

---

## What It Does

ProteinViz is a browser-based tool for exploring protein structures from the RCSB Protein Data Bank. It works entirely from a single HTML file — no backend required for core features. The optional Java/Spring Boot backend adds caching and server-side API calls.

**Core features:**

- **3D Structure Viewer** — interactive molecular rendering via 3Dmol.js with cartoon, stick, sphere, surface, line, and cross representations
- **AI Protein Summary** — uses Claude (Anthropic) to generate function summaries, disease relevance, pathway involvement, and drug targets
- **Mutation Analyzer** — highlights mutated residues in 3D, compares wild-type vs mutant amino acid properties, predicts pathogenicity using BLOSUM62 and Claude AI
- **PPI Network** — protein-protein interaction graphs via STRING database with D3.js force-directed layout
- **RCSB Search** — full-text search across the entire PDB
- **Metadata Panel** — resolution, organism, release date, authors, atom/residue counts

---

## Project Structure

```
proteinviz/
├── frontend/
│   └── index.html          # Entire frontend — single self-contained file
├── backend/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/proteinviz/
│       │   ├── controller/
│       │   │   └── ProteinController.java
│       │   ├── service/
│       │   │   ├── ProteinService.java      # RCSB API calls + caching
│       │   │   ├── AiSummaryService.java    # Claude API integration
│       │   │   └── MutationService.java     # BLOSUM62 + variant DB + Claude
│       │   └── model/
│       │       ├── ProteinInfo.java
│       │       ├── SearchResult.java
│       │       └── MutationRequest.java
│       └── resources/
│           └── application.properties
└── README.md
```

---

## Quick Start — Frontend Only (Recommended)

No installation needed. Just open the file in a browser.

```bash
# Clone the repo
git clone https://github.com/yourusername/proteinviz.git
cd proteinviz

# Open directly in browser
open frontend/index.html

# Or serve with any static server
npx serve frontend/
python3 -m http.server --directory frontend/ 5500
```

All three previously broken features now work without any backend:

| Feature | How it works |
|---|---|
| Metadata | Fetches directly from `data.rcsb.org` in the browser |
| Search | Calls `search.rcsb.org` directly in the browser |
| AI Summary | Calls Anthropic API directly from the browser (needs your API key) |

---

## Running the Backend (Optional)

The backend adds server-side caching and is useful if you want to avoid browser CORS issues or add rate limiting.

### Prerequisites

- Java 17+
- Maven 3.8+
- An Anthropic API key (for AI features)

### Setup

```bash
cd backend

# Set your Anthropic API key
export ANTHROPIC_API_KEY=sk-ant-api03-your-key-here

# Run
mvn spring-boot:run
```

The backend starts on `http://localhost:8080`.

To point the frontend at the backend, add this near the top of the `<script>` block in `index.html`:

```js
const BACKEND = 'http://localhost:8080';
```

Then prefix all `/api/...` calls with `BACKEND`. The frontend already tries `/api/...` first and falls back to direct API calls if the backend is unreachable.

### Backend Endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/protein/{pdbId}` | Fetch protein metadata |
| `GET` | `/api/search?q=&limit=` | Search RCSB PDB |
| `GET` | `/api/featured` | Get featured protein list |
| `GET` | `/api/protein/{pdbId}/summary` | AI-generated protein summary |
| `POST` | `/api/mutation/analyze` | Analyze a point mutation |
| `GET` | `/api/protein/{pdbId}/sequence` | Fetch canonical sequence |
| `GET` | `/api/health` | Health check |

---

## AI Features Setup

Both AI features (AI Summary and Mutation Analyzer) call the Anthropic Claude API directly from your browser. Your API key never touches any third-party server.

1. Get a free API key at [console.anthropic.com](https://console.anthropic.com)
2. Open the **AI** tab in ProteinViz
3. Paste your key into the API key field
4. Click **Analyze** on any loaded protein

The key is stored in `sessionStorage` — it's cleared when you close the browser tab.

**Without an API key**, the Mutation Analyzer still works using BLOSUM62 substitution scoring, and the AI Summary panel will prompt you to add a key.

---

## Using Each Feature

### Explore Tab
- Type any PDB ID (e.g. `4HHB`, `6LU7`) and click **Load**
- Click any featured protein chip to load it instantly
- Metadata (organism, resolution, method, authors) loads automatically from RCSB

### Search Tab
- Type a protein name or keyword (e.g. `hemoglobin`, `p53`, `kinase`)
- Results come from the full RCSB PDB — over 200,000 structures
- Click any result to load it in the 3D viewer

### PPI Tab
- Enter a gene symbol (e.g. `TP53`, `BRCA1`, `EGFR`)
- Choose organism, number of nodes, and confidence score threshold
- Click **Map** to render the interaction network
- Click any node to re-query that protein
- Click **Expand** for the full-screen interactive view with zoom/pan/drag

### AI Tab
- Load a protein first, then click **Analyze**
- Returns: biological function, disease associations, pathway involvement, drug targets, and key residues
- Clicking a residue badge highlights it in the 3D viewer

### Mutate Tab
- Load a protein, then fill in: Wild-Type AA, Mutant AA, Position, Chain
- Or use the amino acid quick-pick buttons
- Click **Analyze Mutation** to:
  - Highlight the mutated residue in the 3D viewer (pink sphere + label)
  - Compare amino acid physicochemical properties
  - Get BLOSUM62 conservation score
  - Get Claude AI prediction of pathogenicity
- Click **Focus Mutation Site** to zoom the 3D viewer to the residue

### Style Tab
- Switch between Cartoon, Stick, Sphere, Surface, Line, Cross
- Change color scheme: Spectrum, Chain, Residue type, Teal, White
- Change background: Dark, Navy, Light

---

## External APIs Used

All called directly from the browser — no proxy needed.

| API | Purpose | Docs |
|---|---|---|
| `data.rcsb.org` | Protein metadata | [rcsb.org/docs](https://data.rcsb.org/redoc/index.html) |
| `search.rcsb.org` | Full-text PDB search | [search docs](https://search.rcsb.org) |
| `files.rcsb.org` | PDB structure files (via 3Dmol.js) | — |
| `string-db.org` | Protein interaction networks | [string-db.org](https://string-db.org/cgi/help) |
| `api.anthropic.com` | Claude AI summaries & mutation analysis | [docs.anthropic.com](https://docs.anthropic.com) |

---

## Tech Stack

| Layer | Technology |
|---|---|
| 3D rendering | [3Dmol.js](https://3dmol.org) |
| Network graphs | [D3.js v7](https://d3js.org) |
| Fonts | Syne, Syne Mono, DM Sans (Google Fonts) |
| AI | Claude claude-sonnet-4-6 (Anthropic) |
| Backend | Spring Boot 3.2, Java 17 |
| Caching | Caffeine |
| Build | Maven |

---

## Environment Variables

| Variable | Required | Description |
|---|---|---|
| `ANTHROPIC_API_KEY` | For backend AI | Your Anthropic API key (`sk-ant-...`) |

For frontend-only usage, the API key is entered in the UI instead.

---

## Known Limitations

- Surface representation can be slow for large structures (>10,000 atoms) — switch to Cartoon for better performance
- The STRING PPI API has a rate limit; if Map fails, wait a few seconds and retry
- AI summary quality depends on how well-studied the protein is; obscure structures may get lower-confidence results
- Mutation position uses PDB numbering, which may differ from UniProt canonical numbering for some structures

---

## License

MIT — free to use, modify, and distribute.

---

## Acknowledgements

- [RCSB Protein Data Bank](https://www.rcsb.org) for open structural data
- [3Dmol.js](https://3dmol.org) (University of Pittsburgh) for WebGL molecular rendering
- [STRING Database](https://string-db.org) for protein interaction data
- [Anthropic](https://www.anthropic.com) for the Claude API
