/*
 * This file is part of Dynamic Surroundings, licensed under the MIT License (MIT).
 *
 * Copyright (c) OreCruncher
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.blockartistry.DynSurround.registry;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.blockartistry.DynSurround.DSurround;
import org.blockartistry.DynSurround.ModOptions;
import org.blockartistry.DynSurround.client.sound.SoundEffect;
import org.blockartistry.DynSurround.client.sound.Sounds;
import org.blockartistry.DynSurround.data.xface.ItemConfig;
import org.blockartistry.DynSurround.data.xface.ModConfigurationFile;
import org.blockartistry.lib.ItemStackUtil;
import org.blockartistry.lib.MCHelper;
import org.blockartistry.lib.collections.IdentityHashSet;

import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class ItemRegistry extends Registry {

	private static final int SET_CAPACITY = 64;
	private static final int MAP_CAPACITY = 256;

	private final Set<Class<?>> swordItems = new IdentityHashSet<>(SET_CAPACITY);
	private final Set<Class<?>> axeItems = new IdentityHashSet<>(SET_CAPACITY);
	private final Set<Class<?>> bowItems = new IdentityHashSet<>(SET_CAPACITY);
	private final Set<Class<?>> toolItems = new IdentityHashSet<>(SET_CAPACITY);
	private final Set<Class<?>> shieldItems = new IdentityHashSet<>(SET_CAPACITY);
	private final Map<Item, ArmorClass> armorMap = new IdentityHashMap<>(SET_CAPACITY);
	private final Map<Item, SoundEffect> swings = new IdentityHashMap<>(MAP_CAPACITY);
	private final Map<Item, SoundEffect> uses = new IdentityHashMap<>(MAP_CAPACITY);
	private final Map<Item, SoundEffect> equips = new IdentityHashMap<>(MAP_CAPACITY);

	public ItemRegistry(@Nonnull final Side side) {
		super(side);
	}

	@Override
	public void configure(@Nonnull final ModConfigurationFile cfg) {
		this.register(cfg.itemConfig);
	}

	@Override
	public void init() {
		this.swordItems.clear();
		this.axeItems.clear();
		this.bowItems.clear();
		this.toolItems.clear();
		this.shieldItems.clear();
		this.armorMap.clear();

		this.swings.clear();
		this.uses.clear();
		this.equips.clear();
	}

	private boolean postProcess(@Nonnull final Set<Class<?>> itemSet, @Nonnull final Item item) {
		final Class<?> itemClass = item.getClass();

		// If the item is in the collection already, return
		if (itemSet.contains(itemClass))
			return true;

		// Need to iterate to see if an item is a sub-class of an existing
		// item in the list.
		for (final Class<?> clazz : itemSet) {
			if (clazz.isAssignableFrom(itemClass)) {
				itemSet.add(itemClass);
				return true;
			}
		}

		return false;
	}

	@Override
	public void initComplete() {

		// Post process item list looking for similar items
		final Iterator<Item> iterator = Item.REGISTRY.iterator();
		while (iterator.hasNext()) {
			final Item item = iterator.next();
			if (postProcess(this.swordItems, item))
				;
			else if (postProcess(this.axeItems, item))
				;
			else if (postProcess(this.toolItems, item))
				;
			else if (postProcess(this.shieldItems, item))
				;
			else
				postProcess(this.bowItems, item);

			SoundEffect se = this.getSwingSound(item);
			if (se != null)
				this.swings.put(item, se);

			se = this.getUseSound(item);
			if (se != null)
				this.uses.put(item, se);

			se = this.getEquipSound(item);
			if (se != null)
				this.equips.put(item, se);
		}

	}

	@Override
	public void fini() {

	}

	private void process(@Nonnull final List<String> classes, @Nonnull final Set<Class<?>> theList) {
		for (final String c : classes) {
			try {
				final Class<?> clazz = Class.forName(c, false, ItemRegistry.class.getClassLoader());
				theList.add(clazz);
			} catch (final ClassNotFoundException e) {
				DSurround.log().warn("Cannot locate class '%s' for ItemRegistry", c);
			}
		}
	}

	private void process(@Nonnull final List<String> itemList, @Nonnull final ArmorClass ac) {
		for (final String i : itemList) {
			final Item item = MCHelper.getItemByName(i);
			if (item != null)
				this.armorMap.put(item, ac);
		}
	}

	public void register(@Nonnull final ItemConfig config) {
		process(config.axeSound, this.axeItems);
		process(config.bowSound, this.bowItems);
		process(config.swordSound, this.swordItems);
		process(config.toolSound, this.toolItems);
		process(config.shieldSound, this.shieldItems);
		process(config.crystalArmor, ArmorClass.CRYSTAL);
		process(config.heavyArmor, ArmorClass.HEAVY);
		process(config.mediumArmor, ArmorClass.MEDIUM);
		process(config.lightArmor, ArmorClass.LIGHT);
	}

	public boolean isBow(@Nonnull final ItemStack stack) {
		return ItemStackUtil.isValidItemStack(stack) ? this.bowItems.contains(stack.getItem().getClass()) : false;
	}

	public boolean isShield(@Nonnull final ItemStack stack) {
		return ItemStackUtil.isValidItemStack(stack) ? this.shieldItems.contains(stack.getItem().getClass()) : false;
	}

	public ArmorClass getArmorClass(@Nonnull final ItemStack stack) {
		return ItemStackUtil.isValidItemStack(stack) ? getArmorClass(stack.getItem()) : ArmorClass.NONE;
	}

	protected ArmorClass getArmorClass(@Nonnull final Item item) {
		final ArmorClass result = this.armorMap.get(item);
		return result != null ? result : ArmorClass.NONE;
	}

	protected SoundEffect getSwingSound(@Nonnull final Item item) {
		final Class<?> itemClass = item.getClass();
		final SoundEffect sound;
		if (this.swordItems.contains(itemClass))
			sound = Sounds.SWORD_SWING;
		else if (this.axeItems.contains(itemClass))
			sound = Sounds.AXE_SWING;
		else if (this.toolItems.contains(itemClass))
			sound = Sounds.TOOL_SWING;
		else if (this.bowItems.contains(itemClass))
			sound = Sounds.TOOL_SWING;
		else if (this.shieldItems.contains(itemClass))
			sound = Sounds.TOOL_SWING;
		else {
			final ArmorClass armor = this.getArmorClass(item);
			switch (armor) {
			case LIGHT:
				sound = Sounds.LIGHT_ARMOR_EQUIP;
				break;
			case MEDIUM:
				sound = Sounds.MEDIUM_ARMOR_EQUIP;
				break;
			case HEAVY:
				sound = Sounds.HEAVY_ARMOR_EQUIP;
				break;
			case CRYSTAL:
				sound = Sounds.CRYSTAL_ARMOR_EQUIP;
				break;
			default:
				sound = null;
			}
		}

		return sound;
	}

	protected SoundEffect getUseSound(@Nonnull final Item item) {
		final Class<?> itemClass = item.getClass();
		final SoundEffect sound;

		if (this.bowItems.contains(itemClass))
			sound = Sounds.BOW_PULL;
		else if (this.shieldItems.contains(itemClass))
			sound = Sounds.SHIELD_USE;
		else {
			final ArmorClass armor = this.getArmorClass(item);
			switch (armor) {
			case LIGHT:
				sound = Sounds.LIGHT_ARMOR_EQUIP;
				break;
			case MEDIUM:
				sound = Sounds.MEDIUM_ARMOR_EQUIP;
				break;
			case HEAVY:
				sound = Sounds.HEAVY_ARMOR_EQUIP;
				break;
			case CRYSTAL:
				sound = Sounds.CRYSTAL_ARMOR_EQUIP;
				break;
			default:
				sound = null;
			}
		}

		return sound;
	}

	protected SoundEffect getEquipSound(@Nonnull final Item item) {
		final Class<?> itemClass = item.getClass();
		final SoundEffect sound;
		if (item instanceof ItemFood)
			sound = Sounds.FOOD_EQUIP;
		else if (this.swordItems.contains(itemClass))
			sound = ModOptions.sound.swordEquipAsTool ? Sounds.TOOL_EQUIP : Sounds.SWORD_EQUIP;
		else if (this.axeItems.contains(itemClass))
			sound = Sounds.AXE_EQUIP;
		else if (this.toolItems.contains(itemClass))
			sound = Sounds.TOOL_EQUIP;
		else if (this.bowItems.contains(itemClass))
			sound = Sounds.BOW_EQUIP;
		else if (this.shieldItems.contains(itemClass))
			sound = Sounds.SHIELD_EQUIP;
		else {
			final ArmorClass armor = this.getArmorClass(item);
			switch (armor) {
			case LIGHT:
				sound = Sounds.LIGHT_ARMOR_EQUIP;
				break;
			case MEDIUM:
				sound = Sounds.MEDIUM_ARMOR_EQUIP;
				break;
			case HEAVY:
				sound = Sounds.HEAVY_ARMOR_EQUIP;
				break;
			case CRYSTAL:
				sound = Sounds.CRYSTAL_ARMOR_EQUIP;
				break;
			default:
				sound = null;
			}
		}

		return sound;
	}

	@Nullable
	public SoundEffect getSwingSound(@Nonnull final ItemStack stack) {
		return ItemStackUtil.isValidItemStack(stack) ? this.swings.get(stack.getItem()) : null;
	}

	@Nullable
	public SoundEffect getUseSound(@Nonnull final ItemStack stack) {
		return ItemStackUtil.isValidItemStack(stack) ? this.uses.get(stack.getItem()) : null;
	}

	@Nullable
	public SoundEffect getEquipSound(@Nonnull final ItemStack stack) {
		if (ItemStackUtil.isValidItemStack(stack)) {
			final SoundEffect result = this.equips.get(stack.getItem());
			return result != null ? result : Sounds.UTILITY_EQUIP;
		}
		return null;
	}

}
