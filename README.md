ProteinViz v2 · Molecular Structure Explorer

An AI-powered protein structure visualization tool with interactive 3D rendering, protein-protein interaction networks, AI-generated summaries, and mutation analysis.

> What It Does
ProteinViz is a browser-based tool for exploring protein structures from the RCSB Protein Data Bank. It works entirely from a single HTML file — no backend required for core features. The optional Java/Spring Boot backend adds caching and server-side API calls.
Core features:

3D Structure Viewer — interactive molecular rendering via 3Dmol.js with cartoon, stick, sphere, surface, line, and cross representations
AI Protein Summary — uses Claude (Anthropic) to generate function summaries, disease relevance, pathway involvement, and drug targets
Mutation Analyzer — highlights mutated residues in 3D, compares wild-type vs mutant amino acid properties, predicts pathogenicity using BLOSUM62 and Claude AI
PPI Network — protein-protein interaction graphs via STRING database with D3.js force-directed layout
RCSB Search — full-text search across the entire PDB
Metadata Panel — resolution, organism, release date, authors, atom/residue counts

> Project Structure

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

> Using Each Feature

1. Explore Tab

Type any PDB ID (e.g. 4HHB, 6LU7) and click Load
Click any featured protein chip to load it instantly
Metadata (organism, resolution, method, authors) loads automatically from RCSB

2. Search Tab

Type a protein name or keyword (e.g. hemoglobin, p53, kinase)
Results come from the full RCSB PDB — over 200,000 structures
Click any result to load it in the 3D viewer

3. PPI Tab

Enter a gene symbol (e.g. TP53, BRCA1, EGFR)
Choose organism, number of nodes, and confidence score threshold
Click Map to render the interaction network
Click any node to re-query that protein
Click Expand for the full-screen interactive view with zoom/pan/drag

4. AI Tab

Load a protein first, then click Analyze
Returns: biological function, disease associations, pathway involvement, drug targets, and key residues
Clicking a residue badge highlights it in the 3D viewer

5. Mutate Tab

Load a protein, then fill in: Wild-Type AA, Mutant AA, Position, Chain
Or use the amino acid quick-pick buttons
Click Analyze Mutation to:

Highlight the mutated residue in the 3D viewer (pink sphere + label)
Compare amino acid physicochemical properties
Get BLOSUM62 conservation score
Get Claude AI prediction of pathogenicity

Click Focus Mutation Site to zoom the 3D viewer to the residue

6. Style Tab

Switch between Cartoon, Stick, Sphere, Surface, Line, Cross
Change color scheme: Spectrum, Chain, Residue type, Teal, White
Change background: Dark, Navy, Light

> External APIs Used
All called directly from the browser — no proxy needed.

1. data.rcsb.org for Protein metadata
2. search.rcsb.org for Full-text PDB search
3. files.rcsb.org PDB for structure files (via 3Dmol.js)
4. string-db.org Protein for interaction networks
5. api.anthropic.com for Claude AI summaries & mutation analysis

> Tech Stack

layer               technology 
3D rendering         3Dmol.js
Network             graphsD3.js v7
Fonts               Syne, Syne Mono, DM Sans (Google Fonts)
AI                  Claude claude-sonnet-4-6 (Anthropic)
Backend             Spring Boot 3.2, Java 17 
Caching             Caffeine
Build               Maven

> Known Limitations

Surface representation can be slow for large structures (>10,000 atoms) — switch to Cartoon for better performance
The STRING PPI API has a rate limit; if Map fails, wait a few seconds and retry
AI summary quality depends on how well-studied the protein is; obscure structures may get lower-confidence results
Mutation position uses PDB numbering, which may differ from UniProt canonical numbering for some structures

> Acknowledgements

RCSB Protein Data Bank for open structural data
3Dmol.js (University of Pittsburgh) for WebGL molecular rendering
STRING Database for protein interaction data
Anthropic for the Claude API
