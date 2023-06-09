package io.github.redstoneparadox.betterenchantmentboosting.util;

import io.github.redstoneparadox.betterenchantmentboosting.BetterEnchantmentBoosting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChiseledBookshelfBlock;
import net.minecraft.block.entity.ChiseledBookshelfBlockEntity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.collection.Weighting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.random.RandomGenerator;
import net.minecraft.world.World;
import org.quiltmc.qsl.block.content.registry.api.BlockContentRegistries;
import org.quiltmc.qsl.block.content.registry.api.enchanting.EnchantingBooster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class EnchantingUtil {
	private static final TagKey<Block> ENCHANTMENT_POWER_TRANSMITTER = TagKey.of(Registries.BLOCK.getKey(), new Identifier("minecraft:enchantment_power_transmitter"));

	public static List<BlockPos> search(World world, BlockPos origin) {
		int distance = BetterEnchantmentBoosting.CONFIG.bounds().distance();
		int height = BetterEnchantmentBoosting.CONFIG.bounds().height();
		int depth = BetterEnchantmentBoosting.CONFIG.bounds().depth();
		Box bounds = new Box(origin.add(-distance, depth, -distance), origin.add(distance, height, distance));
		SearchArea area = new SearchArea();

		area.setGrowthPredicate(state ->
				state.isIn(ENCHANTMENT_POWER_TRANSMITTER)
		);
		area.setSearchPredicate(state ->
				BlockContentRegistries.ENCHANTING_BOOSTERS.get(state.getBlock()).isPresent()
		);

		return area.search(world, origin, bounds);
	}

	public static double getPower(World world, List<BlockPos> boosterPositions) {
		double power = 0;

		for (BlockPos boosterPosition: boosterPositions) {
			BlockState boosterState = world.getBlockState(boosterPosition);
			Optional<EnchantingBooster> booster = BlockContentRegistries.ENCHANTING_BOOSTERS.get(boosterState.getBlock());

			if (booster.isPresent()) {
				power += booster.get().getEnchantingBoost(world, boosterState, boosterPosition);
			}
		}

		return power;
	}

	public static List<EnchantmentLevelEntry> generateEnchantments(ItemStack stack, int slot, int level, RandomGenerator random, int seed, World world, List<BlockPos> boosterPositions) {
		random.setSeed(seed + slot);
		List<EnchantmentLevelEntry> baseEntries = getEntriesFromInfluencers(stack, random, world, boosterPositions);
		List<EnchantmentLevelEntry> list = generateEnchantments(random, stack, level, false, baseEntries);
		if (stack.isOf(Items.BOOK) && list.size() > 1) {
			list.remove(random.nextInt(list.size()));
		}

		return list;
	}

	public static List<EnchantmentLevelEntry> getEntriesFromInfluencers(ItemStack stack, RandomGenerator random, World world, List<BlockPos> boosterPositions) {
		Map<Enchantment, Integer> map = new HashMap<>();
		List<EnchantmentLevelEntry> bonusEntries = new ArrayList<>();

		for (BlockPos boosterPosition: boosterPositions) {
			BlockState boosterState = world.getBlockState(boosterPosition);

			if (boosterState.getBlock() instanceof ChiseledBookshelfBlock) {
				ChiseledBookshelfBlockEntity blockEntity = (ChiseledBookshelfBlockEntity) world.getBlockEntity(boosterPosition);
				assert blockEntity != null;

				for (int i = 0; i < 6; i++) {
					ItemStack bookStack = blockEntity.getStack(i);

					if (bookStack.isOf(Items.ENCHANTED_BOOK)) {
						Map<Enchantment, Integer> bookEnchantmentMap = EnchantmentHelper.get(bookStack);
						List<Enchantment> possibleEnchantments = new ArrayList<>();

						for (Enchantment enchantment : bookEnchantmentMap.keySet()) {
							if (!enchantment.type.isAcceptableItem(stack.getItem()) || enchantment.isTreasure()) map.remove(enchantment);
							else possibleEnchantments.add(enchantment);
						}

						Enchantment enchantment = possibleEnchantments.get(random.nextInt(possibleEnchantments.size()));
						int level = bookEnchantmentMap.get(enchantment);

						if (!map.containsKey(enchantment)) map.put(enchantment, 0);

						int total = map.get(enchantment);
						map.put(enchantment, total + level);
					}
				}
			}
		}

		map.forEach((key, value) -> {
			int cost = 1;
			int remaining = value;

			while (remaining >= cost) {
				bonusEntries.add(new EnchantmentLevelEntry(key, 1));

				if (cost == 1) cost = 3;
				else cost += 3;
			}
		});

		return bonusEntries;
	}

	public static List<EnchantmentLevelEntry> generateEnchantments(RandomGenerator random, ItemStack stack, int level, boolean treasureAllowed, List<EnchantmentLevelEntry> entries) {
		Item item = stack.getItem();
		int enchantability = item.getEnchantability();
		if (enchantability > 0) {
			level += 1 + random.nextInt(enchantability / 4 + 1) + random.nextInt(enchantability / 4 + 1);
			float f = (random.nextFloat() + random.nextFloat() - 1.0F) * 0.15F;
			level = MathHelper.clamp(Math.round((float) level + (float) level * f), 1, Integer.MAX_VALUE);
			List<EnchantmentLevelEntry> possibleEntries = EnchantmentHelper.getPossibleEntries(level, stack, treasureAllowed);
			if (!possibleEntries.isEmpty()) {
				Weighting.getRandomItem(random, possibleEntries).ifPresent(entries::add);

				while (random.nextInt(50) <= level) {
					if (!entries.isEmpty()) {
						EnchantmentHelper.removeConflicts(possibleEntries, Util.getLast(entries));
					}

					if (possibleEntries.isEmpty()) {
						break;
					}

					Weighting.getRandomItem(random, possibleEntries).ifPresent(entries::add);
					level /= 2;
				}
			}

		}
		return entries;
	}
}
