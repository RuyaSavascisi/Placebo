package dev.shadowsoffire.placebo.menu;

import java.util.function.Predicate;

import com.google.common.base.Predicates;

import dev.shadowsoffire.placebo.cap.InternalItemHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Extension of {@link SlotItemHandler} which takes a filter on what may enter the slot.
 */
public class FilteredSlot extends SlotItemHandler {

    protected final Predicate<ItemStack> filter;
    protected final int index;

    /**
     * Creates a new filtered slot
     *
     * @param handler The backing item handler
     * @param index   The slot index
     * @param x       The x coordinate
     * @param y       The y coordinate
     * @param filter  A filter controlling what items may be placed in the slot by a player
     */
    public FilteredSlot(InternalItemHandler handler, int index, int x, int y, Predicate<ItemStack> filter) {
        super(handler, index, x, y);
        this.filter = filter;
        this.index = index;
    }

    public FilteredSlot(InternalItemHandler handler, int index, int x, int y) {
        this(handler, index, x, y, Predicates.alwaysTrue());
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return this.filter.test(stack);
    }

    @Override
    public boolean mayPickup(Player playerIn) {
        return !((InternalItemHandler) this.getItemHandler()).extractItemInternal(this.index, 1, true).isEmpty();
    }

    @Override
    public ItemStack remove(int amount) {
        return ((InternalItemHandler) this.getItemHandler()).extractItemInternal(this.index, amount, false);
    }

}
