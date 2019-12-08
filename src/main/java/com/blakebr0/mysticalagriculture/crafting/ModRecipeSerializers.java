package com.blakebr0.mysticalagriculture.crafting;

import com.blakebr0.mysticalagriculture.MysticalAgriculture;
import com.blakebr0.mysticalagriculture.crafting.condition.CropEnabledCondition;
import com.blakebr0.mysticalagriculture.crafting.ingredient.HoeIngredient;
import com.blakebr0.mysticalagriculture.crafting.recipe.FarmlandTillRecipe;
import com.blakebr0.mysticalagriculture.crafting.recipe.InfusionRecipe;
import com.blakebr0.mysticalagriculture.crafting.recipe.ReprocessorRecipe;
import net.minecraft.item.HoeItem;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IIngredientSerializer;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.ArrayList;
import java.util.List;

public class ModRecipeSerializers {
    public static final IRecipeSerializer<FarmlandTillRecipe> CRAFTING_FARMLAND_TILL = new FarmlandTillRecipe.Serializer();
    public static final IRecipeSerializer<InfusionRecipe> INFUSION = new InfusionRecipe.Serializer();
    public static final IRecipeSerializer<ReprocessorRecipe> REPROCESSOR = new ReprocessorRecipe.Serializer();

    public static final IIngredientSerializer<HoeIngredient> HOE_INGREDIENT = new HoeIngredient.Serializer();

    @SubscribeEvent
    public void onRegisterSerializers(RegistryEvent.Register<IRecipeSerializer<?>> event) {
        IForgeRegistry<IRecipeSerializer<?>> registry = event.getRegistry();

        registry.register(CRAFTING_FARMLAND_TILL.setRegistryName(new ResourceLocation(MysticalAgriculture.MOD_ID, "farmland_till")));
        registry.register(INFUSION.setRegistryName(new ResourceLocation(MysticalAgriculture.MOD_ID, "infusion")));
        registry.register(REPROCESSOR.setRegistryName(new ResourceLocation(MysticalAgriculture.MOD_ID, "reprocessor")));

        CraftingHelper.register(CropEnabledCondition.Serializer.INSTANCE);

        CraftingHelper.register(new ResourceLocation(MysticalAgriculture.MOD_ID, "all_hoes"), HOE_INGREDIENT);
    }

    public static void onCommonSetup() {
        List<HoeItem> hoes = new ArrayList<>();
        ForgeRegistries.ITEMS.getValues().stream().filter(i -> i instanceof HoeItem).forEach(i -> hoes.add((HoeItem) i));
        HoeIngredient.ALL_HOES.addAll(hoes);
    }
}
