# Entity Matching System - Requirements

## Project Overview
Build an entity matching system that processes external forms (lender/trading participant data) and matches them against our LoanIQ system of record. The system should operate at "smart intern level" accuracy while handling real-world data quality issues.

## Business Context
- **Domain**: Syndicated loan trading participants using LSTA/LMA admin forms
- **System of Record**: LoanIQ SQL database (regulatory system of record when we're administrative agent)
- **Goal**: Determine if incoming entities exist in our system, flag discrepancies, or create new entries
- **Philosophy**: Live with messy data, don't assume upstream cleanup will happen
- **Processing Model**: Primarily individual submissions with occasional batch processing (testing batches ~100 forms)

## Document Processing Requirements

### Input Formats
- **Admin Details Forms (ADF)**: Primary trading participant data (always present)
  - Document types: PDF files (text-based, image-based/scanned, or mixed), Microsoft Word documents (.docx and legacy .doc formats)
  - Standard LSTA/LMA forms and custom/non-standard formats
  - Contact information section: Multiple contacts with email addresses for matching purposes
  - Wire/payment instructions: Skip entirely (multiple currencies, complex extraction, minimal matching value)
- **US Tax Forms**: Better source for legal names and EIN data (sometimes present)
  - Same document format challenges as ADFs
  - Higher quality legal name data
  - More reliable EIN/Tax ID information
- **Quality issues**: Manual corrections, handwritten annotations, OCR artifacts (same challenges across all input types)

### Cross-Source Validation
- **Legal name reconciliation**: Compare legal names across Admin Details Forms, tax forms, and LoanIQ records
- **EIN verification**: Tax forms provide more reliable EIN data for cross-validation
- **Email domain analysis**: Extract domains from ADF contact information (lender contacts only, filter third-party service providers)
- **Consistency checking**: Flag discrepancies between different input sources for same entity

### Document Processing Challenges
- **OCR complexity**: Scanned PDFs with varying quality across all document types
- **Manual annotations**: Handwritten corrections over printed forms
- **Layout variations**: Standard vs custom form layouts
- **Field extraction**: Must identify form fields even with non-standard labels
- **Contact information filtering**: Distinguish lender contacts from third-party service provider contacts

## Entity Identity Model

### Composite Identity Structure
Most entities (~80%) are **managed funds** with composite identity:
- **Legal Entity**: The underlying legal entity (pension fund, endowment, etc.)
- **Fund Manager**: The trading entity managing the funds (BlackRock, Blackstone, etc.)
- **Trading Identity** = Legal Entity + Fund Manager combination
- **Matching requirement**: BOTH legal entity AND fund manager must match for valid composite identity
- **Entity type detection**: Must infer standalone vs managed fund status from form data (not explicitly marked)

**Minority (~20%)** are **standalone entities** that trade directly without fund managers.

### LoanIQ Database Structure
#### Customer/Entity Records
- **Full Name**: Complete legal entity name (may omit corporate forms like Inc., LLC)
- **Short Name**: Abbreviated form with theoretical uniqueness requirements
  - **Practical reality**: Surface-level uniqueness only (duplicates exist due to punctuation differences)
  - **UI preference**: Displayed in most LoanIQ interfaces
  - **Semantic hints**: May contain fund manager indicators (FM suffix) or manager references for user convenience
- **Corporate hierarchy field repurposed**: `ultimate_parent` field stores fund manager (not actual corporate parent in our implementation)

#### Location Sub-Objects
- **Multiple locations per customer**: Some entities have sub-locations with separate identifiers
- **Location-level identifiers**: Each location may have its own MEI and/or tax ID
- **Granularity implications**: Same legal entity may appear at both customer and location levels

## Identifier Ecosystem

### Primary Identifiers (Priority Order)
1. **MEI (Member Entity Identifier)**
   - Length: 10 characters
   - Format: 2-character country code + 8 digits
   - Usage: Trading-specific identifier for Clearpar platform
   - Coverage: Almost all forms include MEI for both managed entity and fund manager
   - **Most reliable identifier for matching**
   - **Uniqueness**: Generally unique, with rare exceptions for trading account distinctions

2. **LEI (Legal Entity Identifier)**
   - Length: 20 characters
   - Usage: Legal entity identification
   - Coverage: Sometimes present
   - **Secondary validation identifier**

3. **EIN/Tax ID**
   - Format: Jurisdiction-specific
   - Usage: Tax identification
   - Coverage: More reliable in tax forms, sometimes present in Admin Details Forms
   - **Strong validation identifier when available**

4. **Debt Domain ID**
   - Usage: Present when entities described in Debt Domain external system
   - Coverage: Occasionally present
   - **Additional validation identifier when available**

5. **Email Domain Information**
   - Source: Contact information section of Admin Details Forms
   - Usage: Corporate relationship validation and entity identification
   - Coverage: Most forms include multiple contacts with email addresses
   - **Filtering requirement**: Exclude third-party service provider domains, focus on lender entity domains
   - **Weighting**: Higher value for fund manager matching than standalone entity matching

### Geographic Identifiers
- **Legal Address Country**: Incorporation jurisdiction (should align with MEI country code)
- **Tax Address Country**: Operational jurisdiction (may differ from legal address)
- **Cross-validation**: MEI country code vs address countries for consistency checks
- **Address details**: Street addresses, city, state/province, postal codes serve as helpers only
- **Formatting tolerance**: Expect significant variations in address formatting, abbreviations, punctuation

## Name Handling Complexity

### Legal Name Normalization
- **Corporate form variations**: Tax forms include full legal forms (Inc., LLC, Corp., Ltd., S.A., GmbH, etc.)
- **LoanIQ practice**: May omit corporate forms from full names
- **Normalization strategy**: Make corporate forms optional for matching comparisons
- **International forms**: Handle global corporate form equivalents
- **Significant vs insignificant differences**:
  - **Insignificant**: Corporate form presence/absence, variations, punctuation around forms
  - **Significant**: Core business name differences, substantive word changes

### DBA (Doing Business As) Processing
- **LoanIQ format**: "Legal Entity Name DBA Trade Name" (embedded in legal name field)
- **ADF structure**: Separate DBA field when present
- **Processing requirements**:
  - Parse embedded DBA from LoanIQ legal names
  - Cross-reference ADF DBA field with extracted DBA components
  - Validate consistency: ADF legal name + DBA field = LoanIQ combined name
  - Support multi-name matching against both legal name and DBA components

### Short Name Intelligence
- **Abbreviated consistency**: May be more effective for fuzzy matching than full names due to abbreviation patterns
- **Semantic parsing**: Extract fund manager hints to validate composite relationships
- **Duplicate detection**: Flag multiple "unique" records with only punctuation differences

## Data Quality Reality

### Universal Problems
- **Copy-paste degradation**: Data corrupts through each system in the chain
- **Microsoft Outlook effects**: "Smart" formatting (curly quotes, em dashes, special spaces)
- **Encoding issues**: Different character sets, Unicode vs ASCII corruption
- **Hidden characters**: Line breaks, tabs, non-breaking spaces embedded in names
- **Visual character confusion**: Similar-looking characters (0/O, rn/m, etc.)
- **Diacritic handling**: Mixed presence of accented characters

### Identifier Conflicts
- **Geographic mismatches**: MEI country codes vs legal/tax address countries
- **Cross-source conflicts**: Same entity with different identifiers across forms
- **Systematic data entry errors**: Must work with conflicting information
- **No assumption of data cleanup**: System must be conflict-tolerant

### Name Variations
- **Legal entity names**: Relatively stable and accurate across sources
- **Fund manager names**: Highly variable (abbreviations, nicknames, informal references)
- **Industry conventions**: "GSAM" vs "Goldman Sachs Asset Management"
- **Email domain corporate identity**: More consistent than text names for fund managers

## Comprehensive Discrepancy Detection

### Multi-Level Discrepancy Types
1. **External vs LoanIQ**: Traditional form-to-database discrepancies
2. **Cross-form inconsistencies**: Admin Details Form vs tax form conflicts within same submission
3. **LoanIQ internal duplicates**: Multiple records with surface-level unique short names representing same entity
4. **Geographic inconsistencies**: Country code misalignments across identifiers and addresses
5. **Corporate structure anomalies**: Subsidiary vs branch mapping inconsistencies

### Discrepancy Prioritization Framework
#### Critical Discrepancies (Immediate Review Required)
- **Identifier conflicts**: Different MEIs, EINs, LEIs, Debt Domain IDs for same entity
- **Country mismatches**: MEI country vs legal/tax address countries
- **Legal name conflicts**: Significant differences in legal names across sources (beyond corporate forms)
- **Duplicate detection**: Multiple LoanIQ records (customer or location level) matching same external identity
- **Email domain conflicts**: Lender contact domains inconsistent with entity identity

#### Minor Discrepancies (Display but Low Priority)
- **Address formatting**: Street address variations, abbreviation differences
- **Corporate form differences**: Inc. vs Corp., Ltd. vs Limited, international equivalents
- **Name formatting**: Punctuation, spacing, capitalization in non-legal names
- **Short name variations**: Different abbreviation styles
- **Contact information**: Third-party vs lender contact mixing

## Matching Strategy

### Rules-Based Approach
- **Implementation preference**: Rules over ML (faster to deploy, explainable, leverages domain knowledge)
- **Multi-source evidence**: Weight evidence from Admin Details Forms, tax forms, email domains, and multiple identifiers
- **Hierarchical confidence scoring**: Multiple evidence levels with clear reasoning
- **Conflict tolerance**: Weight evidence rather than require perfect matches
- **Asymmetric matching**: Strict on legal entity names, lenient on fund manager names

### Enhanced Matching Hierarchy
1. **Multi-identifier exact match**: MEI + EIN + Debt Domain ID + email domain alignment
2. **Dual identifier match**: Two exact identifiers + supporting evidence
3. **Tax form validation**: Legal name from tax form confirms Admin Details identity
4. **Email domain validation**: Lender contact domains align with entity identity (stronger weighting for fund managers)
5. **Single identifier + strong support**: One exact identifier + normalized name matching + geographic consistency
6. **Composite fuzzy matching**: Legal entity + fund manager name similarity with geographic cross-validation
7. **Cross-form consistency validation**: Admin Details aligns with tax form data across multiple attributes
8. **Location-level matching**: Match against LoanIQ customer locations when customer-level match fails
9. **Create new entity**: No acceptable matches found

### Confidence Levels
1. **High Confidence (95%+)**: Multiple exact identifier matches + cross-source validation + geographic consistency
2. **Medium-High (85-94%)**: One exact identifier + strong fuzzy matches + email domain support + geographic consistency
3. **Medium (70-84%)**: Strong fuzzy matches with multiple supporting evidence sources
4. **Review Queue (<70%)**: Uncertain matches requiring human review

### Weighting Strategy
- **MEI matching**: Highest weight (most reliable when present and correct)
- **Legal entity name**: High weight, strict matching requirements
- **Fund manager name**: Medium weight, lenient fuzzy matching acceptable
- **Email domain**: Higher weight for fund managers, medium weight for standalone entities
- **Geographic consistency**: Supporting evidence, inconsistencies reduce confidence
- **EIN/Tax ID**: High weight when present, strong validation value
- **LEI/Debt Domain ID**: Medium weight, good supporting evidence when available

## Edge Cases and Exceptions

### MEI Granularity Variations
- **Trading account distinctions**: Same fund manager may legitimately have multiple MEIs for different trading accounts
- **Corporate structure complexity**: Subsidiaries vs branches may be imprecisely mapped in LoanIQ
- **Location sub-objects**: LoanIQ customers may have multiple locations, each with separate MEI/tax ID
- **Granularity decisions**: Distinguish between legitimate sub-entities vs administrative duplicates

### System Implications for Edge Cases
- **Query strategy**: Search both customer-level and location-level records in LoanIQ
- **Exception detection**: Flag cases where multiple MEI matches exist for similar entities
- **Enhanced duplicate detection**: Distinguish legitimate granularity from problematic duplicates
- **Confidence adjustment**: Slight reduction when multiple potential matches exist at different granularity levels
- **Audit trail enhancement**: Document when matches involve location sub-objects vs primary customer records

## System Requirements

### Performance Targets
- **Accuracy benchmark**: "Smart intern level" performance (80%+ correct decisions)
- **Speed requirement**: Faster than manual processing
- **Explainability**: Clear reasoning for match decisions and discrepancy flagging
- **Coverage**: Handle all common document formats and form variations

### Functional Requirements
#### Input Processing
- **Document format support**: PDF (text/image/mixed), Word (.docx/.doc), standard and custom forms
- **OCR capability**: Handle scanned documents with varying quality
- **Field extraction**: Identify form fields even with non-standard labels
- **Multi-source coordination**: Process ADF + optional tax form together

#### Matching Engine
- **Composite identity handling**: Legal entity + fund manager matching for managed funds
- **Entity type inference**: Detect standalone vs managed fund from form data
- **Multi-level querying**: Search both customer and location records in LoanIQ
- **Real-time processing**: Individual submission processing (not just batch)

#### Output and Reporting
- **Side-by-side comparison**: All attributes from external forms vs LoanIQ records
- **Multi-source display**: ADF vs tax form vs LoanIQ data with discrepancy highlighting
- **Comprehensive discrepancy flagging**: All detected inconsistencies with priority classification
- **Confidence scoring**: Probability assessment with supporting evidence documentation
- **Audit trail**: Complete reasoning chain for match decisions
- **Duplicate alerts**: Flag potential LoanIQ internal duplicates discovered during matching

### Technical Constraints
- **No upstream data cleanup**: Work with existing messy data across all sources
- **Production deployment**: Must work with current data quality levels
- **LoanIQ integration**: Direct SQL database queries against existing schema
- **Document format flexibility**: Handle whatever formats arrive without preprocessing requirements
- **Multi-source processing**: Coordinate and validate data from multiple input sources
- **Performance scalability**: Support both individual processing and batch operations

### Integration Requirements
- **Database access**: SQL queries against LoanIQ customer and location tables
- **Document processing**: OCR and field extraction pipeline
- **UI integration**: Display matching results and discrepancies for human review
- **Audit logging**: Track all matching decisions and evidence sources

## Success Criteria
- **Match accuracy**: 80%+ correct decisions (comparable to smart intern performance)
- **Format coverage**: Process all common document types and form variations received
- **Processing speed**: Faster than manual review while maintaining accuracy
- **User confidence**: Clear, understandable match reasoning with comprehensive discrepancy detection
- **Discrepancy detection**: Flag all significant inconsistencies across sources and within LoanIQ
- **Cross-source validation**: Leverage multiple input sources for improved accuracy and validation
- **Edge case handling**: Properly recognize and handle known exceptions without false positives
- **Explainability**: Every match decision must be clearly documented and auditable