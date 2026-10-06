package dev.redycrosshair.smoke;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

final class GameplayChecks {
    private static final int TARGET = 0xFFDA2741;
    private static final int CRITICAL = 0xFF297CE8;
    private static final String[] CONFIG_NAMES = {
        "Enabled", "Rgb", "CritColorEnabled", "CritRgb", "UseIndicatorStyle",
        "IndicatorCustomColor", "IndicatorCornersOnly", "DisableBlending", "DisableBlendingOnlyWhileRedy"
    };
    private final Minecraft client;
    private final Class<?> config;
    private final Method attackableTarget;
    private final Method canCriticalHit;
    private final Method extractCrosshair;

    private GameplayChecks(Minecraft client) throws ReflectiveOperationException {
        this.client = client;
        this.config = Class.forName("dev.redycrosshair.RedyCrosshairConfig");
        Class<?> targeting = Class.forName("dev.redycrosshair.RedyCrosshairTargeting");
        this.attackableTarget = targeting.getMethod("attackableTarget", Minecraft.class);
        this.canCriticalHit = targeting.getMethod("canCriticalHit", Minecraft.class, Entity.class);
        this.extractCrosshair = Hud.class.getDeclaredMethod("extractCrosshair", GuiGraphicsExtractor.class, DeltaTracker.class);
        this.extractCrosshair.setAccessible(true);
    }

    static void run(Minecraft client) throws Exception {
        if (!client.isSameThread() || client.player == null || client.level == null) {
            throw new IllegalStateException("Gameplay checks require the client thread and a loaded world");
        }
        new GameplayChecks(client).runCases();
    }

    private void runCases() throws Exception {
        var player = this.client.player;
        Object[] savedConfig = new Object[CONFIG_NAMES.length];
        for (int i = 0; i < CONFIG_NAMES.length; i++) {
            String name = CONFIG_NAMES[i];
            savedConfig[i] = this.config.getMethod(Character.toLowerCase(name.charAt(0)) + name.substring(1)).invoke(null);
        }
        Entity savedTarget = this.client.crosshairPickEntity;
        Vec3 savedPosition = player.position();
        double savedFallDistance = player.fallDistance;
        boolean savedGround = player.onGround();
        boolean savedSprint = player.isSprinting();
        ItemStack savedHand = player.getMainHandItem();
        CameraType savedCamera = this.client.options.getCameraType();
        AttackIndicatorStatus savedIndicator = this.client.options.attackIndicator().get();
        MobEffectInstance blindness = player.getEffect(MobEffects.BLINDNESS);
        MobEffectInstance savedBlindness = blindness == null ? null : new MobEffectInstance(blindness);
        Field cooldown = field(LivingEntity.class, "attackStrengthTicker");
        Field water = field(Entity.class, "wasTouchingWater");
        Field cachedBlock = field(Entity.class, "inBlockState");
        Field lastClimbable = field(LivingEntity.class, "lastClimbablePos");
        Field boardingCooldown = field(Entity.class, "boardingCooldown");
        Field handsBusy = field(LocalPlayer.class, "handsBusy");
        int savedCooldown = cooldown.getInt(player);
        boolean savedWater = water.getBoolean(player);
        Object savedCachedBlock = cachedBlock.get(player);
        Object savedLastClimbable = lastClimbable.get(player);
        int savedBoardingCooldown = boardingCooldown.getInt(player);
        boolean savedHandsBusy = handsBusy.getBoolean(player);
        var savedPose = player.getPose();
        PlayerInfo info = this.client.getConnection().getPlayerInfo(player.getUUID());
        require(info != null, "Missing local PlayerInfo");
        Method setMode = PlayerInfo.class.getDeclaredMethod("setGameMode", GameType.class);
        setMode.setAccessible(true);
        GameType savedMode = info.getGameMode();
        BlockPos probe = player.blockPosition().above(4);
        BlockState savedBlock = this.client.level.getBlockState(probe);
        require(!player.isPassenger(), "Test-world player unexpectedly mounted");
        Zombie zombie = new Zombie(this.client.level);
        ItemEntity droppedItem = new ItemEntity(this.client.level, probe.getX(), probe.getY(), probe.getZ(), new ItemStack(Items.STICK));
        Interaction interaction = new Interaction(EntityTypes.INTERACTION, this.client.level);
        try {
            set("Enabled", true);
            set("Rgb", TARGET);
            set("CritRgb", CRITICAL);
            set("CritColorEnabled", false);
            set("UseIndicatorStyle", false);
            set("IndicatorCustomColor", false);
            set("IndicatorCornersOnly", false);
            set("DisableBlending", true);
            set("DisableBlendingOnlyWhileRedy", true);
            setMode.invoke(info, GameType.SURVIVAL);
            this.client.options.setCameraType(CameraType.FIRST_PERSON);
            this.client.options.attackIndicator().set(AttackIndicatorStatus.OFF);
            player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            player.setPos(probe.getX() + 0.5, probe.getY(), probe.getZ() + 0.5);
            this.client.level.setBlock(probe, Blocks.AIR.defaultBlockState(), 2);
            cachedBlock.set(player, null);
            player.removeEffect(MobEffects.BLINDNESS);
            water.setBoolean(player, false);
            player.setSprinting(false);
            player.setOnGround(false);
            player.fallDistance = 1.0;
            cooldown.setInt(player, 100);

            target(null, null, "no-target");
            target(zombie, zombie, "living-target");
            render("target-tint", RenderPipelines.GUI_TEXTURED, TARGET);
            zombie.setHealth(0.0F);
            target(zombie, null, "dead-target");
            render("dead-white", RenderPipelines.CROSSHAIR, -1);
            zombie.setHealth(zombie.getMaxHealth());
            require(!droppedItem.isAttackable(), "ItemEntity fixture must be unattackable");
            target(droppedItem, null, "unattackable-target");
            require(interaction.skipAttackInteraction(player), "Interaction fixture must reject attacks");
            target(interaction, null, "skipped-interaction");
            target(zombie, zombie, "living-restored");
            set("Enabled", false);
            target(zombie, null, "disabled-target");
            render("disabled-white", RenderPipelines.CROSSHAIR, -1);
            set("Enabled", true);
            setMode.invoke(info, GameType.SPECTATOR);
            require(player.isSpectator(), "PlayerInfo did not make real player a spectator");
            target(zombie, null, "spectator-target");
            setMode.invoke(info, GameType.SURVIVAL);
            target(zombie, zombie, "survival-restored");
            System.out.println("REDY_GAMEPLAY_TARGET_OK cases=none,living,dead,unattackable,skip-interaction,disabled,spectator");

            critical(zombie, true, "falling-ready");
            critical(droppedItem, false, "nonliving");
            critical(null, false, "null-target");
            cooldown.setInt(player, 0);
            critical(zombie, false, "cooldown");
            cooldown.setInt(player, 100);
            player.fallDistance = 0.0;
            critical(zombie, false, "not-falling");
            player.fallDistance = 1.0;
            player.setOnGround(true);
            critical(zombie, false, "grounded");
            player.setOnGround(false);
            this.client.level.setBlock(probe, Blocks.LADDER.defaultBlockState(), 2);
            cachedBlock.set(player, null);
            require(player.onClimbable(), "Real ladder fixture is not climbable");
            critical(zombie, false, "climbing");
            this.client.level.setBlock(probe, Blocks.AIR.defaultBlockState(), 2);
            cachedBlock.set(player, null);
            water.setBoolean(player, true);
            critical(zombie, false, "water");
            water.setBoolean(player, false);
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200));
            require(player.hasEffect(MobEffects.BLINDNESS), "Blindness fixture was not applied");
            critical(zombie, false, "blindness");
            player.removeEffect(MobEffects.BLINDNESS);
            require(player.startRiding(zombie, true, true), "Real mount fixture was rejected");
            critical(zombie, false, "passenger");
            player.stopRiding();
            player.setPos(probe.getX() + 0.5, probe.getY(), probe.getZ() + 0.5);
            player.setSprinting(true);
            critical(zombie, false, "sprinting");
            player.setSprinting(false);
            critical(zombie, true, "prerequisites-restored");
            System.out.println("REDY_GAMEPLAY_CRIT_OK cases=ready,nonliving,null,cooldown,fall-distance,ground,ladder,water,blindness,passenger,sprint");

            render("crit-disabled-target-precedence", RenderPipelines.GUI_TEXTURED, TARGET);
            set("CritColorEnabled", true);
            render("critical-precedence", RenderPipelines.GUI_TEXTURED, CRITICAL);
            player.setSprinting(true);
            render("sprint-target-precedence", RenderPipelines.GUI_TEXTURED, TARGET);
            player.setSprinting(false);
            ItemStack spear = new ItemStack(Items.WOODEN_SPEAR);
            require(spear.has(DataComponents.PIERCING_WEAPON), "Spear fixture lacks PIERCING_WEAPON");
            player.setItemSlot(EquipmentSlot.MAINHAND, spear);
            render("piercing-target-precedence", RenderPipelines.GUI_TEXTURED, TARGET);
            player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            set("CritColorEnabled", false);
            set("UseIndicatorStyle", true);
            render("indicator-white", RenderPipelines.GUI_TEXTURED, -1, -1);
            set("IndicatorCustomColor", true);
            render("indicator-custom", RenderPipelines.GUI_TEXTURED, TARGET, TARGET);
            set("IndicatorCornersOnly", true);
            render("indicator-corners-only", RenderPipelines.GUI_TEXTURED, -1, TARGET);
            set("CritColorEnabled", true);
            render("indicator-critical-corners", RenderPipelines.GUI_TEXTURED, -1, CRITICAL);
            set("IndicatorCustomColor", false);
            set("IndicatorCornersOnly", false);
            render("indicator-critical-overrides-white", RenderPipelines.GUI_TEXTURED, CRITICAL, CRITICAL);
            set("Enabled", false);
            render("disabled-indicator", RenderPipelines.CROSSHAIR, -1);
            set("Enabled", true);
            set("CritColorEnabled", false);
            set("UseIndicatorStyle", false);
            set("DisableBlending", false);
            render("blending-enabled-target", RenderPipelines.CROSSHAIR, TARGET);
            set("DisableBlending", true);
            this.client.crosshairPickEntity = null;
            render("blending-inactive", RenderPipelines.CROSSHAIR, -1);
            set("DisableBlendingOnlyWhileRedy", false);
            render("blending-disabled-always", RenderPipelines.GUI_TEXTURED, -1);
            set("Enabled", false);
            render("disabled-overrides-global-blending", RenderPipelines.CROSSHAIR, -1);
            System.out.println("REDY_RENDER_EXTRACT_OK cases=tint,crit-disabled,critical,sprint,piercing,indicator-white,indicator-custom,corners-only,critical-corners,critical-white-override,disabled,disabled-indicator,blend-active,blend-inactive,blend-always,disabled-global-blend");
        } finally {
            player.stopRiding();
            this.client.level.setBlock(probe, savedBlock, 2);
            player.setPos(savedPosition);
            player.setPose(savedPose);
            player.setItemSlot(EquipmentSlot.MAINHAND, savedHand);
            player.setSprinting(savedSprint);
            player.setOnGround(savedGround);
            player.fallDistance = savedFallDistance;
            cooldown.setInt(player, savedCooldown);
            water.setBoolean(player, savedWater);
            cachedBlock.set(player, savedCachedBlock);
            lastClimbable.set(player, savedLastClimbable);
            boardingCooldown.setInt(player, savedBoardingCooldown);
            handsBusy.setBoolean(player, savedHandsBusy);
            player.removeEffect(MobEffects.BLINDNESS);
            if (savedBlindness != null) {
                player.addEffect(savedBlindness);
            }
            setMode.invoke(info, savedMode);
            this.client.crosshairPickEntity = savedTarget;
            this.client.options.setCameraType(savedCamera);
            this.client.options.attackIndicator().set(savedIndicator);
            for (int i = 0; i < CONFIG_NAMES.length; i++) {
                set(CONFIG_NAMES[i], savedConfig[i]);
            }
        }
    }

    private void target(Entity selected, Entity expected, String name) throws ReflectiveOperationException {
        this.client.crosshairPickEntity = selected;
        require(this.attackableTarget.invoke(null, this.client) == expected, "Targeting case failed: " + name);
    }

    private void critical(Entity target, boolean expected, String name) throws ReflectiveOperationException {
        require((boolean)this.canCriticalHit.invoke(null, this.client, target) == expected, "Critical case failed: " + name);
    }

    private void render(String name, RenderPipeline pipeline, int... colors) throws ReflectiveOperationException {
        GuiRenderState state = new GuiRenderState();
        GuiGraphicsExtractor graphics = new GuiGraphicsExtractor(this.client, state, 0, 0);
        this.extractCrosshair.invoke(this.client.gui.hud, graphics, DeltaTracker.ZERO);
        List<BlitRenderState> blits = new ArrayList<>();
        state.forEachElement(element -> {
            require(element instanceof BlitRenderState, "Unexpected crosshair state: " + element.getClass().getName());
            blits.add((BlitRenderState)element);
        }, GuiRenderState.TraverseRange.ALL);
        require(blits.size() == colors.length, name + ": expected " + colors.length + " blits, got " + blits.size());
        for (int i = 0; i < colors.length; i++) {
            BlitRenderState blit = blits.get(i);
            Identifier spriteId = i == 0 ? Identifier.withDefaultNamespace("hud/crosshair")
                : Identifier.fromNamespaceAndPath("redycrosshair", "crosshair_indicator");
            TextureAtlasSprite sprite = this.client.getAtlasManager().getAtlasOrThrow(AtlasIds.GUI).getSprite(spriteId);
            require(sprite.contents().name().equals(spriteId), name + ": missing sprite " + spriteId);
            require(blit.pipeline() == pipeline, name + ": wrong RenderPearl pipeline");
            require(blit.color() == colors[i], name + ": wrong tint " + Integer.toHexString(blit.color()));
            require(blit.x0() == (graphics.guiWidth() - 15) / 2 && blit.y0() == (graphics.guiHeight() - 15) / 2
                && blit.x1() - blit.x0() == 15 && blit.y1() - blit.y0() == 15, name + ": wrong crosshair bounds");
            require(blit.u0() == sprite.getU0() && blit.u1() == sprite.getU1()
                && blit.v0() == sprite.getV0() && blit.v1() == sprite.getV1(), name + ": wrong atlas sprite");
        }
    }

    private void set(String name, Object value) throws ReflectiveOperationException {
        this.config.getMethod("set" + name, value instanceof Boolean ? boolean.class : int.class).invoke(null, value);
    }

    private static Field field(Class<?> owner, String name) throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
