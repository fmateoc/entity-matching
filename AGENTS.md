# AGENTS.md

This document provides guidance for AI agents working on the Entity Matching System project.

## 1. Project Overview

The project is a Java-based entity matching system designed for the syndicated loan trading domain. Its primary goal is to automate the process of matching messy, real-world data from external forms (like Administrative Details Forms and tax forms) against the LoanIQ system of record.

The system is built to be resilient to data quality issues, such as OCR errors, manual corrections, and inconsistent formatting. It uses a rules-based approach, rather than machine learning, to provide explainable and maintainable matching logic.

### Key Architectural Features:

*   **Modular Design:** The application is divided into logical components:
    *   `extraction`: Handles data extraction from various document formats (PDF, Word).
    *   `detection`: Infers entity types (e.g., standalone vs. managed fund).
    *   `engine`: Contains the core matching rules and logic.
    *   `repository`: Manages database interactions with LoanIQ.
    *   `orchestrator`: Coordinates the overall workflow from document ingestion to matching.
*   **Technology Stack:**
    *   Java 17
    *   Gradle for dependency management and builds.
    *   Libraries for document processing (Apache PDFBox, POI, Tika) and OCR (Tess4J).
    *   PostgreSQL for the database (as per the schema in `build.gradle`).
*   **Domain-Specific Logic:** The system encodes significant domain knowledge, including:
    *   **Composite Identity:** Many entities are a combination of a "Legal Entity" and a "Fund Manager."
    *   **Asymmetric Matching:** Stricter matching rules for legal entity names and more lenient rules for fund manager names.
    *   **Identifier Hierarchy:** A prioritized list of identifiers (MEI, LEI, EIN, etc.) is used for matching.

## 2. Getting Started

This project uses the Gradle wrapper (`gradlew`) for all build and execution tasks.

### Building the Project

To compile the source code, run the tests, and build the JAR files, use the following command:

```bash
./gradlew build
```

This will create the application JAR and a fat/uber JAR (with all dependencies) in the `build/libs` directory.

### Running Tests

To run the full test suite, use:

```bash
./gradlew test
```

### Running the Application

The application can be run from the command line using the fat JAR. You need to provide database credentials and a command.

**Single Document Processing:**

```bash
java -jar build/libs/entity-matching-1.0.0-all.jar <db_url> <db_user> <db_pass> single <path_to_adf_file> [path_to_tax_form]
```

**Batch Processing:**

```bash
java -jar build/libs/entity-matching-1.0.0-all.jar <db_url> <db_user> <db_pass> batch <directory_with_documents>
```

Alternatively, you can use the `runBatch` Gradle task:

```bash
./gradlew runBatch -PdbUrl=<db_url> -PdbUser=<db_user> -PdbPass=<db_pass> -Pdirectory=<directory_with_documents>
```

### Creating a Distribution

To create a zip file containing the application JAR and other necessary files, run:

```bash
./gradlew createDistribution
```

The distribution zip will be created in `build/distributions`.

## 3. Development Workflow

Follow these guidelines when making changes to the codebase.

### Adding New Matching Rules

*   The core matching logic resides in the `com.loantrading.matching.engine` package.
*   When adding a new rule, consider if it's a general-purpose normalization rule or a specific matching strategy.
*   New normalization logic (e.g., for names, addresses) should be added to the appropriate normalizer class (e.g., `NameNormalizer`).
*   New matching strategies should be implemented as separate components within the engine.

### Adding New Tests

*   All new code should be accompanied by unit tests.
*   Tests are located in `src/test/java`.
*   Follow the existing package structure for your new tests. For example, a new class in `com.loantrading.matching.engine` should have its test in `src/test/java/com/loantrading/matching/engine`.
*   Use JUnit 5 for assertions.

### Coding Conventions

*   Follow standard Java coding conventions.
*   Maintain the existing architectural patterns. Do not introduce new frameworks or major dependencies without discussion.
*   Keep methods small and focused on a single responsibility.
*   Add Javadoc comments to new public methods.

## 4. Testing

A robust test suite is crucial for this project due to the complexity of the matching rules.

### Running Tests

To execute all unit tests, run the following command from the root of the project:

```bash
./gradlew test
```

The test results will be available in `build/reports/tests/test/index.html`.

### Test Coverage

*   Strive for high test coverage for any new code.
*   Pay special attention to edge cases and boundary conditions in your tests.
*   When fixing a bug, add a test case that reproduces the bug to prevent regressions.

## 5. Key Architectural Concepts

Understanding these concepts is essential for working on this project.

*   **Rules-Based Approach:** The system intentionally avoids machine learning in favor of a deterministic, rules-based engine. This makes the matching process explainable and easier to debug.
*   **Embrace Messiness:** The system is designed to work with real-world, imperfect data. Do not assume that data will be clean or consistent. The goal is to build a system that is tolerant of data quality issues.
*   **Composite Identity Model:** A significant portion of entities are "managed funds," which have a composite identity consisting of a **Legal Entity** and a **Fund Manager**. A match is only successful if both components are correctly identified.
*   **Asymmetric Fuzzy Matching:** The matching strategy is not uniform. It applies strict matching criteria for legal entity names, while using more lenient, fuzzy matching for fund manager names, which are known to be more variable.
*   **Hierarchical Evidence:** The system does not rely on a single data point for matching. Instead, it aggregates evidence from multiple sources (identifiers, names, email domains, etc.) and uses a confidence scoring mechanism to determine the quality of a match.
*   **Opportunistic Duplicate Detection:** While the primary goal is to match external entities, the system should also flag potential duplicate records within the LoanIQ database when they are discovered during the matching process.

## 6. Important Files

*   `build.gradle`: The Gradle build script. It defines all dependencies, plugins, and tasks for the project.
*   `system-requirements.md`: The detailed requirements document. This is the primary source of truth for the system's business logic and matching rules.
*   `foundation-prompt.md`: The original prompt used to generate the system's design. It provides insight into the high-level architectural goals.
*   `AGENTS.md`: This file. It provides guidance for AI agents working on the project.
