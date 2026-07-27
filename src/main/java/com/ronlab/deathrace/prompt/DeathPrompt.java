package com.ronlab.deathrace.prompt;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Dolphin;
import org.bukkit.entity.ElderGuardian;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.Hoglin;
import org.bukkit.entity.Husk;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Spider;
import org.bukkit.entity.Stray;
import org.bukkit.entity.Trident;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Enumeration of DeathRace death prompt objectives mapped to Bukkit damage contexts
 * with full datapack fidelity to Scommander/DeathRaceDatapack mechanics.
 */
@NullMarked
public enum DeathPrompt {
    CACTUS("cactus", "[player] was pricked to death", (player, event) ->
            event.getCause() == DamageCause.CONTACT),

    DROWNING("drowning", "[player] drowned", (player, event) ->
            event.getCause() == DamageCause.DROWNING),

    TNT_EXPLOSION("tnt_explosion", "[player] blew up", (player, event) ->
            event.getCause() == DamageCause.BLOCK_EXPLOSION),

    CREEPER_EXPLOSION("creeper_explosion", "[player] was blown up by Creeper", (player, event) ->
            event.getCause() == DamageCause.ENTITY_EXPLOSION && isDamagedByEntity(event, Creeper.class)),

    HIT_GROUND_TOO_HARD("hit_ground_too_hard", "[player] hit the ground too hard", (player, event) ->
            event.getCause() == DamageCause.FALL && player.getFallDistance() < 10.0f),

    INTENTIONAL_GAME_DESIGN("intentional_game_design", "[player] was killed by [Intentional Game Design]", (player, event) ->
            event.getCause() == DamageCause.BLOCK_EXPLOSION && player.getWorld().getEnvironment() != World.Environment.NORMAL),

    FELL_FROM_HIGH_PLACE("fell_from_high_place", "[player] fell from a high place", (player, event) ->
            event.getCause() == DamageCause.FALL && player.getFallDistance() >= 10.0f),

    FELL_OFF_LADDER("fell_off_ladder", "[player] fell off a ladder", (player, event) ->
            event.getCause() == DamageCause.FALL && isNearBlockType(player, Material.LADDER)),

    FELL_OFF_VINES("fell_off_vines", "[player] fell off some vines", (player, event) ->
            event.getCause() == DamageCause.FALL && isNearBlockType(player, Material.VINE)),

    IN_FLAMES("in_flames", "[player] went up in flames", (player, event) ->
            event.getCause() == DamageCause.FIRE || event.getCause() == DamageCause.CAMPFIRE),

    BURNED_TO_DEATH("burned_to_death", "[player] burned to death", (player, event) ->
            event.getCause() == DamageCause.FIRE_TICK),

    LAVA("lava", "[player] tried to swim in lava", (player, event) ->
            event.getCause() == DamageCause.LAVA),

    STRUNG_TO_DEATH("strung_to_death", "[player] was strung to death", (player, event) ->
            isNearBlockType(player, Material.COBWEB) || isDamagedByEntity(event, Spider.class)),

    STARVATION("starvation", "[player] starved to death", (player, event) ->
            event.getCause() == DamageCause.STARVATION),

    IMPALED_BY_MOB("impaled_by_mob", "[player] was impaled by [Mob]", (player, event) ->
            isDamagedByEntity(event, Trident.class) || isDamagedByEntity(event, Guardian.class) || isDamagedByEntity(event, ElderGuardian.class)),

    SUFFOCATION("suffocation", "[player] suffocated in a wall", (player, event) ->
            event.getCause() == DamageCause.SUFFOCATION),

    SHOT_BY_MOB("shot_by_mob", "[player] was shot by [Mob]", (player, event) ->
            event.getCause() == DamageCause.PROJECTILE && isDamagedByEntity(event, Skeleton.class)),

    FELL_OFF_WEEPING_VINES("fell_off_weeping_vines", "[player] fell off some weeping vines", (player, event) ->
            event.getCause() == DamageCause.FALL && isNearBlockType(player, Material.WEEPING_VINES, Material.WEEPING_VINES_PLANT)),

    FELL_OFF_TWISTING_VINES("fell_off_twisting_vines", "[player] fell off some twisting vines", (player, event) ->
            event.getCause() == DamageCause.FALL && isNearBlockType(player, Material.TWISTING_VINES, Material.TWISTING_VINES_PLANT)),

    FELL_OFF_SCAFFOLDING("fell_off_scaffolding", "[player] fell off scaffolding", (player, event) ->
            event.getCause() == DamageCause.FALL && isNearBlockType(player, Material.SCAFFOLDING)),

    BURNT_WHILST_FIGHTING_MOB("burnt_whilst_fighting_mob", "[player] was burnt to a crisp whilst fighting [Mob]", (player, event) ->
            (event.getCause() == DamageCause.FIRE || event.getCause() == DamageCause.FIRE_TICK) && hasRecentMobCombat(player)),

    FIRE_WHILST_FIGHTING_MOB("fire_whilst_fighting_mob", "[player] walked into fire whilst fighting [Mob]", (player, event) ->
            event.getCause() == DamageCause.FIRE && hasRecentMobCombat(player)),

    LAVA_ESCAPE_MOB("lava_escape_mob", "[player] tried to swim in lava to escape [Mob]", (player, event) ->
            event.getCause() == DamageCause.LAVA && hasRecentMobCombat(player)),

    ZOMBIE_SLAIN("zombie_slain", "[player] was slain by Zombie", (player, event) ->
            isDamagedByEntity(event, Zombie.class)),

    SPIDER_SLAIN("spider_slain", "[player] was slain by Spider", (player, event) ->
            isDamagedByEntity(event, Spider.class)),

    IRON_GOLEM_SLAIN("iron_golem_slain", "[player] was slain by Iron Golem", (player, event) ->
            isDamagedByEntity(event, IronGolem.class)),

    HOGLIN_SLAIN("hoglin_slain", "[player] was slain by Hoglin", (player, event) ->
            isDamagedByEntity(event, Hoglin.class)),

    HUSK_SLAIN("husk_slain", "[player] was slain by Husk", (player, event) ->
            isDamagedByEntity(event, Husk.class)),

    MAGMA_CUBE_SLAIN("magma_cube_slain", "[player] was slain by Magma Cube", (player, event) ->
            isDamagedByEntity(event, MagmaCube.class)),

    SKELETON_SLAIN("skeleton_slain", "[player] was slain by Skeleton", (player, event) ->
            isDamagedByEntity(event, Skeleton.class)),

    STRAY_SLAIN("stray_slain", "[player] was slain by Stray", (player, event) ->
            isDamagedByEntity(event, Stray.class)),

    WITHER_SKELETON_SLAIN("wither_skeleton_slain", "[player] was slain by Wither Skeleton", (player, event) ->
            isDamagedByEntity(event, WitherSkeleton.class)),

    WOLF_SLAIN("wolf_slain", "[player] was slain by Wolf", (player, event) ->
            isDamagedByEntity(event, Wolf.class)),

    DOLPHIN_SLAIN("dolphin_slain", "[player] was slain by Dolphin", (player, event) ->
            isDamagedByEntity(event, Dolphin.class)),

    FLOOR_WAS_LAVA("floor_was_lava", "[player] discovered the floor was lava", (player, event) ->
            event.getCause() == DamageCause.HOT_FLOOR),

    ANVIL("anvil", "[player] was squashed by a falling anvil", (player, event) ->
            event.getCause() == DamageCause.FALLING_BLOCK),

    SWEET_BERRY_BUSH("sweet_berry_bush", "[player] was pricked by a sweet berry bush", (player, event) ->
            event.getCause() == DamageCause.CONTACT),

    LIGHTNING("lightning", "[player] was struck by lightning", (player, event) ->
            event.getCause() == DamageCause.LIGHTNING),

    VOID("void", "[player] fell into the void", (player, event) ->
            event.getCause() == DamageCause.VOID),

    FREEZING("freezing", "[player] froze to death", (player, event) ->
            event.getCause() == DamageCause.FREEZE);

    @FunctionalInterface
    public interface DamageMatcher {
        boolean matches(Player player, EntityDamageEvent event);
    }

    private final String id;
    private final String description;
    private final DamageMatcher matcher;

    DeathPrompt(String id, String description, DamageMatcher matcher) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.description = Objects.requireNonNull(description, "description cannot be null");
        this.matcher = Objects.requireNonNull(matcher, "matcher cannot be null");
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public boolean matches(Player player, EntityDamageEvent event) {
        return matcher.matches(player, event);
    }

    private static boolean isDamagedByEntity(EntityDamageEvent event, Class<? extends Entity> entityClass) {
        if (event instanceof EntityDamageByEntityEvent damageByEntity) {
            Entity damager = damageByEntity.getDamager();
            if (entityClass.isInstance(damager)) {
                return true;
            }
            if (damager instanceof Projectile projectile && projectile.getShooter() != null) {
                return entityClass.isInstance(projectile.getShooter());
            }
        }
        return false;
    }

    private static boolean isNearBlockType(Player player, Material... materials) {
        Block feet = player.getLocation().getBlock();
        Block head = feet.getRelative(0, 1, 0);
        Block below = feet.getRelative(0, -1, 0);
        for (Material material : materials) {
            if (feet.getType() == material || head.getType() == material || below.getType() == material) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasRecentMobCombat(Player player) {
        EntityDamageEvent lastDamage = player.getLastDamageCause();
        if (lastDamage instanceof EntityDamageByEntityEvent damageByEntity) {
            Entity damager = damageByEntity.getDamager();
            return damager instanceof Mob || (damager instanceof Projectile proj && proj.getShooter() instanceof Mob);
        }
        return false;
    }

    private static final List<DeathPrompt> ALL_PROMPTS = List.copyOf(List.of(values()));

    public static DeathPrompt getRandomPrompt() {
        int index = ThreadLocalRandom.current().nextInt(ALL_PROMPTS.size());
        return ALL_PROMPTS.get(index);
    }

    public static List<DeathPrompt> getAllPrompts() {
        return ALL_PROMPTS;
    }
}
