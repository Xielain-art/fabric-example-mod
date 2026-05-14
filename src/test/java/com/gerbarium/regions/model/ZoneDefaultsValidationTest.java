package com.gerbarium.regions.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ZoneDefaultsValidationTest {
    @Test
    void validateMobRuleRejectsZeroBudgetValues() {
        MobRule rule = MobRule.packDefaults("mob-1", "minecraft:zombie");
        rule.maxAlive = 0;
        rule.spawnCount = 0;

        assertThrows(IllegalArgumentException.class, () -> ZoneDefaults.validateMobRule(rule));
    }

    @Test
    void validateResourceRuleRejectsZeroBudgetValues() {
        ResourceRule rule = new ResourceRule();
        rule.id = "resource-1";
        rule.name = "Resource One";
        rule.maxActiveBlocks = 0;
        rule.spawnCount = 0;
        rule.resourceBlocks.add(new WeightedBlock("minecraft:diamond_ore", 1));

        assertThrows(IllegalArgumentException.class, () -> ZoneDefaults.validateResourceRule(rule));
    }

    @Test
    void validateResourceRuleAllowsAirPlacementWithoutTargets() {
        ResourceRule rule = new ResourceRule();
        rule.id = "resource-1";
        rule.name = "Resource One";
        rule.maxActiveBlocks = 1;
        rule.spawnCount = 1;
        rule.replaceMode = ReplaceMode.AIR_OR_REPLACEABLE;
        rule.resourceBlocks.add(new WeightedBlock("minecraft:diamond_ore", 1));

        assertDoesNotThrow(() -> ZoneDefaults.validateResourceRule(rule));
    }
}
