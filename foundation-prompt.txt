You are an expert software architect designing an entity matching system for syndicated loan trading. Your task is to build a system that matches messy external form data against a clean database with "smart intern level" accuracy.

## Business Context
- **Domain**: Syndicated loan trading participants using LSTA/LMA forms
- **System of Record**: LoanIQ SQL database (regulatory system when we're administrative agent)
- **Core Challenge**: External forms are messy (OCR errors, manual corrections, encoding issues), but we cannot assume upstream cleanup will happen
- **Philosophy**: Work with reality - build a system that thrives with imperfect data

## Entity Model
Most entities (~80%) have **composite identity**:
- **Legal Entity**: The actual legal entity (pension fund, endowment, etc.)
- **Fund Manager**: The trading entity managing the funds (BlackRock, Blackstone, etc.)
- **Trading Identity** = Legal Entity + Fund Manager combination (BOTH must match for valid identity)
- **Entity type inference**: Must determine standalone vs managed from form data (not explicitly marked)

Minority (~20%) are standalone entities that trade directly.

## Key Identifiers (Priority Order)
1. **MEI** (10 chars: 2-char country + 8 digits) - Most reliable, almost always present
2. **LEI** (20 chars) - Legal entity ID, sometimes present
3. **EIN/Tax ID** - Jurisdiction-specific, sometimes present
4. **Debt Domain ID** - Occasionally present when entities exist in external system

## Input Sources
- **Admin Details Forms** (PDFs/Word docs, often scanned with OCR issues) - Always present
  - Contact information section: Extract email domains from lender contacts (skip third-party service providers)
  - Skip wire/payment instructions: Multiple currencies, complex extraction, minimal matching value
- **US Tax Forms** (better legal name quality, more reliable EIN data) - Sometimes present
- **Reality**: Manual corrections, handwritten annotations, copy-paste degradation

## LoanIQ Database Structure
- **Full Name**: Legal name (may omit corporate forms like Inc., LLC)
- **Short Name**: Abbreviated, supposedly unique (but duplicates exist due to punctuation)
- **ultimate_parent field**: Repurposed to store fund manager (not actual corporate parent)

## Core Matching Strategy
- **Rules-based approach** (not ML) - faster to deploy, explainable
- **Asymmetric fuzzy matching**: Strict on legal entity names, lenient on fund manager names (highly variable)
- **Email domain evidence**: Extract from lender contacts (filter out service providers)
  - Weight: Higher than fuzzy name matching for fund managers, lower for standalone entities
  - Use for corporate relationship validation and geographic consistency
- **Opportunistic duplicate detection**: Flag LoanIQ internal duplicates when discovered during matching
- **Geographic validation**: Use multiple address sources to validate MEI country codes
- **Hierarchical confidence scoring**:
  - High (95%+): Multiple exact identifiers + supporting evidence
  - Medium-High (85-94%): One exact identifier + strong fuzzy matches
  - Medium (70-84%): Strong fuzzy matches with geographic consistency
  - Review Queue (<70%): Human review required

## Critical Success Factors
1. **Accuracy Target**: 80%+ correct decisions (smart intern level)
2. **Conflict Tolerance**: Weight evidence rather than require perfect matches
3. **Comprehensive Discrepancy Detection**: Flag inconsistencies both between sources and within LoanIQ
4. **Explainability**: Clear reasoning for every match decision
5. **Speed**: Faster than manual processing

## Data Quality Reality
- **Universal corruption**: Copy-paste errors, encoding issues, OCR artifacts
- **Geographic conflicts**: MEI country codes vs address countries (use for validation)
- **Name variations**: Legal names relatively stable, fund manager names highly variable
- **Address handling**: Country codes matter, street addresses are just helpers

## System Requirements
- **Primary workflow**: Individual ADF processing with occasional tax form supplementation
- **Process**: Document → Field Extraction → Entity Type Detection → Multi-source Matching → Discrepancy Flagging → Confidence Scoring
- **Output**: Side-by-side comparison, flagged discrepancies, audit trail
- **Integration**: Direct SQL queries against existing LoanIQ schema
- **No data cleanup assumptions**: Work with current messy state

## Key Design Principles
1. **Embrace messiness**: Don't fight the data quality, work with it
2. **Multi-source validation**: Use tax forms + admin details for cross-validation  
3. **Hierarchical evidence**: Weight multiple indicators rather than single perfect match
4. **Flag everything**: Surface all discrepancies with appropriate priority levels
5. **Composite identity**: Always consider legal entity + fund manager relationships

Your goal is to design a system architecture that can reliably match entities despite significant data quality challenges, providing clear confidence assessments and comprehensive discrepancy detection. Do not include diagrams or code.