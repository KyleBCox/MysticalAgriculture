package com.blakebr0.mysticalagriculture.item;

import com.blakebr0.cucumber.helper.NBTHelper;
import com.blakebr0.cucumber.item.BaseItem;
import com.blakebr0.mysticalagriculture.lib.ModTooltips;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.Material;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.item.UseAction;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.IPlantable;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public class EssenceWateringCanItem extends BaseItem {
    private final int range;
    private final double chance;
    private final TextFormatting textColor;

    public EssenceWateringCanItem(int range, double chance, TextFormatting textColor, Function<Properties, Properties> properties) {
        super(properties.compose(p -> p.maxStackSize(1)));
        this.range = range;
        this.chance = chance;
        this.textColor = textColor;
    }

    @Override
    public void fillItemGroup(ItemGroup group, NonNullList<ItemStack> items) {
        if (this.isInGroup(group)) {
            ItemStack stack = new ItemStack(this);
            NBTHelper.setBoolean(stack, "Water", false);
            NBTHelper.setBoolean(stack, "Active", false);
            items.add(stack);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (selected && world.getGameTime() % 4 == 0) {
            if (NBTHelper.getBoolean(stack, "Active") && entity instanceof PlayerEntity) {
                PlayerEntity player = (PlayerEntity) entity;
                BlockRayTraceResult result = rayTrace(world, player, RayTraceContext.FluidMode.SOURCE_ONLY);
                if (result.getType() != RayTraceResult.Type.MISS)
                    this.doWater(stack, world, player, result.getPos(), result.getFace());
            }
        }
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.NONE;
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return NBTHelper.getBoolean(stack, "Active");
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (NBTHelper.getBoolean(stack, "Water")) {
            if (player.isCrouching())
                NBTHelper.flipBoolean(stack, "Active");

            return new ActionResult<>(ActionResultType.FAIL, stack);
        }

        BlockRayTraceResult trace = rayTrace(world, player, RayTraceContext.FluidMode.SOURCE_ONLY);
        if (trace.getType() != RayTraceResult.Type.BLOCK) {
            return new ActionResult<>(ActionResultType.FAIL, stack);
        }

        BlockPos pos = trace.getPos();
        Direction direction = trace.getFace();
        if (world.isBlockModifiable(player, pos) && player.canPlayerEdit(pos.offset(direction), direction, stack)) {
            BlockState state = world.getBlockState(pos);
            if (state.getMaterial() == Material.WATER) {
                NBTHelper.setBoolean(stack, "Water", true);
                return new ActionResult<>(ActionResultType.FAIL, stack);
            }
        }

        return new ActionResult<>(ActionResultType.FAIL, stack);
    }

    @Override
    public ActionResultType onItemUse(ItemUseContext context) {
        PlayerEntity player = context.getPlayer();
        if (player == null)
            return ActionResultType.FAIL;

        Hand hand = context.getHand();
        World world = context.getWorld();
        BlockPos pos = context.getPos();
        Direction direction = context.getFace();
        ItemStack stack = player.getHeldItem(hand);

        if (!player.canPlayerEdit(pos.offset(direction), direction, stack))
            return ActionResultType.FAIL;

        if (!NBTHelper.getBoolean(stack, "Water"))
            return ActionResultType.PASS;

        if (NBTHelper.getBoolean(stack, "Active"))
            return ActionResultType.FAIL;

        return this.doWater(stack, world, player, pos, direction);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void addInformation(ItemStack stack, World world, List<ITextComponent> tooltip, ITooltipFlag advanced) {
        if (NBTHelper.getBoolean(stack, "Water")) {
            tooltip.add(ModTooltips.FILLED.build());
        } else {
            tooltip.add(ModTooltips.EMPTY.build());
        }

        String rangeString = String.valueOf(this.range);
        ITextComponent rangeNumber = new StringTextComponent(rangeString + "x" + rangeString).mergeStyle(this.textColor);
        tooltip.add(ModTooltips.WATERING_CAN_AREA.args(rangeNumber).build());
    }

    private ActionResultType doWater(ItemStack stack, World world, PlayerEntity player, BlockPos pos, Direction direction) {
        if (player == null)
            return ActionResultType.FAIL;

        if (!player.canPlayerEdit(pos.offset(direction), direction, stack))
            return ActionResultType.FAIL;

        if (!NBTHelper.getBoolean(stack, "Water"))
            return ActionResultType.FAIL;

        int range = (this.range - 1) / 2;
        Stream<BlockPos> blocks = BlockPos.getAllInBox(pos.add(-range, -range, -range), pos.add(range, range, range));
        blocks.forEach(aoePos -> {
            BlockState aoeState = world.getBlockState(aoePos);
            if (aoeState.getBlock() instanceof FarmlandBlock) {
                int moisture = aoeState.get(FarmlandBlock.MOISTURE);
                if (moisture < 7) {
                    world.setBlockState(aoePos, aoeState.with(FarmlandBlock.MOISTURE, 7), 2);
                }
            }
        });

        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
                double d0 = pos.add(x, 0, z).getX() + world.getRandom().nextFloat();
                double d1 = pos.add(x, 0, z).getY() + 1.0D;
                double d2 = pos.add(x, 0, z).getZ() + world.getRandom().nextFloat();

                BlockState state = world.getBlockState(pos);
                if (state.isSolid() || state.getBlock() instanceof FarmlandBlock) {
                    d1 += 0.3D;
                }

                world.addParticle(ParticleTypes.RAIN, d0, d1, d2, 0.0D, 0.0D, 0.0D);
            }
        }

        if (!world.isRemote()) {
            if (Math.random() <= this.chance) {
                blocks = BlockPos.getAllInBox(pos.add(-range, -range, -range), pos.add(range, range, range));
                blocks.forEach(aoePos -> {
                    BlockState state = world.getBlockState(aoePos);
                    Block plantBlock = state.getBlock();
                    if (plantBlock instanceof IGrowable || plantBlock instanceof IPlantable || plantBlock == Blocks.MYCELIUM || plantBlock == Blocks.CHORUS_FLOWER) {
                        state.randomTick((ServerWorld) world, aoePos, random);
                        world.notifyBlockUpdate(aoePos, state, state, 2);
                    }
                });

                return ActionResultType.FAIL;
            }
        }

        return ActionResultType.FAIL;
    }
}
