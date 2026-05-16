package com.gerbarium.regions.client.screen.help;

import java.util.*;

public class HelpRegistry {
    private static final Map<HelpTopic, List<HelpSection>> REGISTRY = new EnumMap<>(HelpTopic.class);

    public static void register(HelpTopic topic, HelpSection... sections) {
        REGISTRY.put(topic, List.of(sections));
    }

    public static List<HelpSection> get(HelpTopic topic) {
        return REGISTRY.getOrDefault(topic, List.of());
    }

    public record HelpSection(String title, String content) {}

    public static void init() {
        register(HelpTopic.ZONE_RUNTIME_SETTINGS,
            section("Activation Range",
                "Radius in blocks within which a player must be for the zone to activate. " +
                "When a player enters this range, the zone becomes active and mob spawning begins. " +
                "Default: 96 blocks."),
            section("Deactivate After Seconds",
                "How many seconds the zone stays active after the last player leaves the activation range. " +
                "Set to 0 to deactivate immediately. Default: 45 seconds."),
            section("First Spawn Delay",
                "Delay in seconds before the first mob spawn occurs after zone activation. " +
                "Useful to prevent instant spawns when a player barely enters range. Default: 2 seconds."),
            section("Reactivation Cooldown",
                "Minimum seconds between zone activations. Prevents rapid on/off toggling. Default: 300 seconds."),
            section("Min Distance From Player",
                "Minimum distance in blocks that mobs can spawn from the nearest player. " +
                "Prevents spawning directly on top of players. Default: 24 blocks."),
            section("Max Distance From Player",
                "Maximum distance in blocks that mobs can spawn from the nearest player. " +
                "Must be greater than min distance. Default: 64 blocks."),
            section("Max Position Attempts",
                "How many times the system tries to find a valid spawn position before giving up. " +
                "Higher values may cause lag. Default: 64 attempts."),
            section("Require Loaded Chunk",
                "If ON, mobs will only spawn in chunks that are loaded. " +
                "If OFF, spawning may occur in unloaded chunks (not recommended)."),
            section("Respect Vanilla Spawn Rules",
                "If ON, mob spawning follows Minecraft vanilla rules (light level, biome, etc.). " +
                "If OFF, mobs can spawn anywhere within the zone regardless of vanilla conditions.")
        );

        register(HelpTopic.MOB_RULE_BASICS,
            section("Rule Name",
                "Display name for this mob rule. Used for identification in the zone details screen."),
            section("Entity",
                "Minecraft entity ID in format 'namespace:path'. Examples: 'minecraft:zombie', 'minecraft:skeleton'. " +
                "Use the 'Pick Entity' button to browse available entities."),
            section("Enabled",
                "If OFF, this rule is completely ignored and no mobs will spawn from it."),
            section("Spawn Type",
                "PACK: Spawns multiple mobs as a group. Suitable for normal enemies.\n" +
                "UNIQUE: Spawns a single special mob (boss). Tuned for rare, powerful enemies."),
            section("Boundary Mode",
                "Controls what happens when a mob leaves the zone boundary:\n" +
                "NONE - Mob can leave freely.\n" +
                "LEASH - Mob is returned if outside too long.\n" +
                "TELEPORT_BACK - Mob is instantly teleported back.\n" +
                "REMOVE_OUTSIDE - Mob is removed (use with caution).")
        );

        register(HelpTopic.MOB_RULE_SPAWN,
            section("Refill Mode",
                "ON_ACTIVATION - Spawns mobs when zone activates, up to max alive.\n" +
                "TIMED - Periodically spawns mobs on a timer. May be farmable!\n" +
                "AFTER_DEATH - Only respawns after all mobs from this rule die."),
            section("Max Alive",
                "Maximum number of mobs from this rule that can exist simultaneously. " +
                "For UNIQUE type, this is always 1."),
            section("Spawn Count",
                "How many mobs to spawn at once. For PACK type, this is the group size."),
            section("Respawn Seconds",
                "Cooldown in seconds between spawn attempts. Longer values reduce farmability."),
            section("Chance",
                "Probability (0.0 to 1.0) that a spawn attempt succeeds. 1.0 = always, 0.5 = 50%."),
            section("Spawn Trigger",
                "TIMER - Spawn on a regular interval.\n" +
                "AFTER_DEATH - Spawn only after previous mobs die.\n" +
                "ON_ACTIVATION - Spawn once when zone activates.\n" +
                "MANUAL - Only spawn via command."),
            section("Respawn After Death / Despawn",
                "Whether to trigger respawn when mobs die or despawn. Usually both are ON for continuous spawning.")
        );

        register(HelpTopic.MOB_RULE_PLACEMENT,
            section("Spawn Mode",
                "RANDOM_VALID_POSITION - Random valid location in zone.\n" +
                "CENTER - Spawn at exact zone center.\n" +
                "BOSS_ROOM - Spawn in largest open room (for bosses).\n" +
                "FIXED_POINT - Spawn at specific coordinates."),
            section("Fixed X/Y/Z",
                "Exact spawn coordinates. Only used when Spawn Mode is FIXED_POINT."),
            section("Position Attempts",
                "How many times to try finding a valid spawn position. Higher = more lag but better success rate."),
            section("Min Distance Between Spawns",
                "Minimum blocks between spawned mobs. Prevents clustering."),
            section("Allow Small Room",
                "If ON, mobs can spawn in small rooms. If OFF, requires larger open space."),
            section("Spread Spawns",
                "If ON, tries to spread mobs evenly across the zone. If OFF, may cluster."),
            section("Require Player Nearby",
                "If ON, only spawns when a player is within Player Activation Range."),
            section("Require Chunk Loaded",
                "If ON, only spawns in loaded chunks. Recommended for performance."),
            section("Allow Force Load",
                "If ON, can force-load chunks to spawn mobs. Use with caution on servers.")
        );

        register(HelpTopic.MOB_RULE_ADVANCED,
            section("Companions",
                "Additional mobs that spawn alongside the main entity. " +
                "Each companion has its own entity type, count, radius, and spawn chance."),
            section("Despawn When Zone Inactive",
                "If ON, removes all mobs from this rule when the zone deactivates."),
            section("Announce On Spawn",
                "If ON, broadcasts a server message when mobs spawn. Useful for boss events."),
            section("Boss Preset",
                "One-click preset for boss configuration: UNIQUE type, AFTER_DEATH refill, BOSS_ROOM spawn, " +
                "max alive = 1, teleport back boundary.")
        );

        register(HelpTopic.MOB_RULE_BOUNDARY,
            section("Boundary Mode",
                "Controls mob behavior when leaving zone bounds:\n" +
                "NONE - No restriction.\n" +
                "LEASH - Returns mob after Max Outside Seconds.\n" +
                "TELEPORT_BACK - Instantly teleports mob back inside.\n" +
                "REMOVE_OUTSIDE - Removes mob (does NOT count as death for respawn)."),
            section("Max Outside Seconds",
                "How long a mob can stay outside the zone before boundary action triggers. " +
                "Only used for LEASH mode. Default: 10 seconds."),
            section("Check Interval Ticks",
                "How often (in game ticks, 20 ticks = 1 second) to check mob position. " +
                "Lower = more responsive but more CPU usage. Minimum: 20 ticks."),
            section("Teleport Back",
                "If ON and mode is LEASH, teleports mob back instead of walking. Faster but may look jarring.")
        );

        register(HelpTopic.RESOURCE_RULE_BASICS,
            section("Rule ID",
                "Unique identifier for this resource rule. Auto-generated, read-only."),
            section("Name",
                "Display name for this rule. Used in lists and messages."),
            section("Enabled",
                "If OFF, this rule does not place any blocks."),
            section("Activation Mode",
                "REAL_TIME - Blocks are placed/updated continuously in real time.\n" +
                "WHILE_ZONE_ACTIVE - Only places blocks while the parent zone is active.")
        );

        register(HelpTopic.RESOURCE_RULE_BLOCKS,
            section("Target Blocks",
                "Block IDs that this rule can replace. Format: 'minecraft:stone'. " +
                "If Replace Mode is ONLY_TARGET_BLOCKS, these are the only blocks that will be replaced."),
            section("Resource Blocks",
                "Block IDs that this rule places. Each has a weight determining selection probability. " +
                "Higher weight = more likely to be chosen."),
            section("Replace Mode",
                "ONLY_TARGET_BLOCKS - Only replaces blocks in the Target Blocks list.\n" +
                "AIR_OR_REPLACEABLE - Can replace air and replaceable blocks (grass, etc.).\n" +
                "TARGET_BLOCKS_OR_AIR - Replaces target blocks or air.")
        );

        register(HelpTopic.RESOURCE_RULE_LIMITS,
            section("Max Active Blocks",
                "Maximum number of resource blocks that can exist at once. When reached, no more are placed until some are removed/mined."),
            section("Spawn Count",
                "How many blocks to place per spawn attempt."),
            section("Respawn Seconds",
                "Cooldown between block placement attempts."),
            section("Chance",
                "Probability (0.0-1.0) that a placement attempt succeeds."),
            section("Min Y / Max Y",
                "Vertical limits for block placement. Leave blank to use zone bounds."),
            section("Min Distance Between Resources",
                "Minimum blocks between placed resources. Prevents clustering."),
            section("Placement Mode",
                "RANDOM_SCATTER - Randomly scattered throughout the zone. (Currently the only option)")
        );

        register(HelpTopic.RESOURCE_RULE_SAFETY,
            section("Restore Delay Seconds",
                "How long after a block is mined before it can be restored. " +
                "Prevents instant reappearing which looks like a bug."),
            section("Max Position Attempts",
                "How many times to try finding a valid placement position."),
            section("Require Loaded Chunk",
                "Only place blocks in loaded chunks. Recommended for performance."),
            section("Respect Protected Blocks",
                "Do not replace blocks that are considered protected (bedrock, command blocks, etc.)."),
            section("Drop Original On Replace",
                "If ON, the replaced block drops its item. If OFF, it's silently removed."),
            section("Restore If Not Mined",
                "If ON, restores the original block if the resource block is removed by non-player means."),
            section("Prevent Player Placed Blocks",
                "Do not replace blocks that were placed by players. Preserves player builds."),
            section("Allow Block Entities",
                "If ON, can replace blocks with tile entities (chests, furnaces). Use with caution.")
        );

        register(HelpTopic.COMPANION_EDIT,
            section("Companion Name",
                "Display name for this companion. Used in lists and messages."),
            section("Entity",
                "Minecraft entity ID for the companion. Example: 'minecraft:zombie'."),
            section("Count",
                "How many of this companion spawn."),
            section("Radius",
                "Maximum distance in blocks from the main mob that companions can spawn."),
            section("Chance",
                "Probability (0.0-1.0) that companions spawn at all. 1.0 = always.")
        );
    }

    private static HelpSection section(String title, String content) {
        return new HelpSection(title, content);
    }
}
