package io.github.redstoneparadox.betterenchantmentboosting.mixin;

import io.github.redstoneparadox.betterenchantmentboosting.util.EnchantmentTableBooster;
import io.github.redstoneparadox.betterenchantmentboosting.util.SearchUtil;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import snownee.jade.addon.vanilla.TotalEnchantmentPowerProvider;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.overlay.DisplayHelper;

import java.util.List;

@Pseudo
@Mixin(value = TotalEnchantmentPowerProvider.class, remap = false )
public class TotalEnchantmentPowerProviderMixin {
    /**
     * @author MrGreenTea
     * @reason Enchantment power calculation is incorrect in vanilla Jade
     */
    @Overwrite
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        World level = accessor.getLevel();
        BlockPos pos = accessor.getPosition();

        List<BlockPos> bookshelfPositions = SearchUtil.search(level, pos);
        double power = EnchantmentTableBooster.getPower(level, bookshelfPositions);

        if (power > 0) {
            tooltip.add(Text.translatable("jade.ench_power", IThemeHelper.get().info(DisplayHelper.dfCommas.format(power))));
        }
    }
}
