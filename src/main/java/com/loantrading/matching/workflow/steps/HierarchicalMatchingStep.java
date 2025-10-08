package com.loantrading.matching.workflow.steps;

import com.loantrading.matching.entity.ExtractedEntity;
import com.loantrading.matching.entity.MatchResult;
import com.loantrading.matching.entity.MatchedEntity;
import com.loantrading.matching.entity.ProcessingResult;
import com.loantrading.matching.framework.match.IMatchingStrategy;
import com.loantrading.matching.framework.persistence.IMatchRepository;
import com.loantrading.matching.framework.persistence.PersistedMatch;
import com.loantrading.matching.framework.workflow.IWorkflowStep;
import com.loantrading.matching.framework.workflow.MatchingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * A workflow step that handles hierarchical matching, specifically for a parent-child relationship.
 * It first resolves the parent entity, utilizing a persistent cache to avoid re-computation,
 * and then proceeds to match the child entity.
 */
public class HierarchicalMatchingStep implements IWorkflowStep {

    private static final Logger logger = LoggerFactory.getLogger(HierarchicalMatchingStep.class);
    public static final String PARENT_MATCH_RESULT = "parentMatchResult";

    private final IMatchRepository matchRepository;
    private final IMatchingStrategy<ExtractedEntity, MatchedEntity> parentMatchingStrategy;
    private final IMatchingStrategy<ExtractedEntity, MatchedEntity> childMatchingStrategy;
    private final String parentEntityType;
    private final double persistenceThreshold;

    public HierarchicalMatchingStep(
            IMatchRepository matchRepository,
            IMatchingStrategy<ExtractedEntity, MatchedEntity> parentMatchingStrategy,
            IMatchingStrategy<ExtractedEntity, MatchedEntity> childMatchingStrategy,
            String parentEntityType,
            double persistenceThreshold) {
        this.matchRepository = matchRepository;
        this.parentMatchingStrategy = parentMatchingStrategy;
        this.childMatchingStrategy = childMatchingStrategy;
        this.parentEntityType = parentEntityType;
        this.persistenceThreshold = persistenceThreshold;
    }

    @Override
    public void execute(MatchingContext context) {
        ProcessingResult result = context.getRequired(ContextKeys.PROCESSING_RESULT, ProcessingResult.class);
        ExtractedEntity masterEntity = context.getRequired(ContextKeys.MASTER_ENTITY, ExtractedEntity.class);

        // This step is only for entities with a fund manager (the parent)
        if (masterEntity.getFundManager() == null || masterEntity.getFundManager().isBlank()) {
            logger.debug("Skipping hierarchical matching as no parent entity (Fund Manager) is present.");
            return;
        }

        // 1. Resolve Parent Match
        MatchResult parentMatch = resolveParentMatch(masterEntity, context);
        if (parentMatch == null) {
            result.addAuditEntry("Could not resolve parent entity (Fund Manager). Skipping child match.");
            return;
        }
        context.put(PARENT_MATCH_RESULT, parentMatch);
        result.addAuditEntry(String.format("Resolved parent: %s (ID: %d) with score %.2f",
                parentMatch.getMatchedEntity().getFullName(), parentMatch.getMatchedEntity().getEntityId(), parentMatch.getScore()));


        // 2. Execute Child Match
        // The child strategy will use the parent match from the context to inform its logic.
        List<MatchResult> childMatches = childMatchingStrategy.match(masterEntity, null, context); // Candidates will be found by the strategy
        context.put(ContextKeys.MATCH_RESULTS, childMatches);
    }

    private MatchResult resolveParentMatch(ExtractedEntity masterEntity, MatchingContext context) {
        String parentIdentifier = masterEntity.getFundManager();
        ProcessingResult result = context.getRequired(ContextKeys.PROCESSING_RESULT, ProcessingResult.class);

        // Check persistent repository first
        Optional<Long> persistedParentId = matchRepository.findMatch(parentEntityType, parentIdentifier);
        if (persistedParentId.isPresent()) {
            result.addAuditEntry("Found persistent match for parent: " + parentIdentifier);
            // In a real scenario, we would need a way to look up the MatchedEntity by its ID.
            // For now, we create a synthetic result. This part needs a repository lookup method.
            MatchedEntity stubEntity = new MatchedEntity();
            stubEntity.setEntityId(persistedParentId.get());
            stubEntity.setFullName(parentIdentifier + " (Persisted)");
            MatchResult persistedResult = new MatchResult();
            persistedResult.setMatchedEntity(stubEntity);
            persistedResult.setScore(100.0);
            persistedResult.setMatchStrategy("PERSISTED_MATCH");
            return persistedResult;
        }

        // If not found in repo, run the matching strategy for the parent
        result.addAuditEntry("No persistent match found. Running parent matching strategy for: " + parentIdentifier);
        List<MatchResult> parentMatches = parentMatchingStrategy.match(masterEntity, null, context);

        if (parentMatches.isEmpty()) {
            return null;
        }

        MatchResult bestParentMatch = parentMatches.get(0);

        // If the match is high-confidence, persist it for future use.
        if (bestParentMatch.getScore() >= persistenceThreshold) {
            result.addAuditEntry(String.format("Persisting new high-confidence parent match. Score: %.2f", bestParentMatch.getScore()));
            PersistedMatch persistedMatch = new PersistedMatch(
                    parentEntityType,
                    parentIdentifier,
                    bestParentMatch.getMatchedEntity().getEntityId(),
                    bestParentMatch.getScore(),
                    bestParentMatch.getMatchStrategy()
            );
            matchRepository.saveMatch(persistedMatch);
        }

        return bestParentMatch;
    }
}