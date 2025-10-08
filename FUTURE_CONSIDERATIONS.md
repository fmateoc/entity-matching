# Future Architectural Considerations for Identity Resolution

This document outlines advanced challenges related to entity identity that the current system is designed to accommodate in the future, but does not yet fully implement. The core architecture, particularly the separation of concerns between workflow steps and the `IMatchRepository` interface, provides the foundation for addressing these complexities.

## 1. Evolving Entity Identity

Real-world entities are not static. Their identifiers and names can change over time, even while their core identity remains the same. The current `DbMatchRepository` implementation uses a simple, static mapping between a source identifier and a target ID. A more robust, future implementation will need to handle the following scenarios:

*   **Name Changes**: A fund manager or corporate entity may officially change its legal name. The system should be able to recognize that "Old Company Name Inc." and "New Company Name LLC" refer to the same canonical entity. This may require maintaining a history of names associated with a single entity ID.
*   **Identifier Rollovers**: In the loan domain, facilities can be "rolled over" in a cashless transaction, resulting in a new CUSIP for what is functionally the same underlying facility. The identity resolution system must be able to link the old CUSIP to the new one to preserve continuity.
*   **Asynchronous Updates**: Name and identifier changes may not be synchronized across all systems (e.g., LoanIQ and the external party). The matching logic must be resilient to these temporary inconsistencies, potentially by relying on a combination of other stable identifiers or metadata.

## 2. The Concept of a Canonical Entity

To solve the evolving identity problem, the system will need to embrace the concept of a **canonical entity**. This is a single, abstract record that represents the "true" real-world entity, independent of its various representations in different systems or at different points in time.

The `IMatchRepository` interface is the key to this. Its responsibility can be evolved from a simple key-value lookup to a more sophisticated **Identity Resolution Service**. When asked for a match, this service would:

1.  Take a set of identifiers (name, CUSIP, EIN, etc.).
2.  Use a set of configurable rules to resolve these identifiers to a single, canonical entity ID.
3.  This resolution process could involve querying historical alias tables, rollover mapping tables, and other metadata.

## 3. Implementation Path

The current architecture allows for an iterative approach to implementing these features:

1.  **Enhance the Database Schema**: The `persisted_matches` table can be augmented with tables for `entity_aliases` (to track historical names) and `identifier_links` (to track CUSIP rollovers).
2.  **Create an `AdvancedMatchRepository`**: A new class implementing `IMatchRepository` can be developed to encapsulate the more complex resolution logic.
3.  **Update Workflow Configuration**: The `EntityMatchingWorkflow` can be configured to use the new `AdvancedMatchRepository` instead of the simple `DbMatchRepository` with no other changes to the workflow steps themselves.

By separating the "what" (the workflow) from the "how" (the identity resolution logic in the repository), the system remains flexible and capable of evolving to meet these complex, real-world challenges.