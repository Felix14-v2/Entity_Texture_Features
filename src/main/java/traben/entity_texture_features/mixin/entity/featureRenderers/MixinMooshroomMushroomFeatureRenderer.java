package traben.entity_texture_features.mixin.entity.featureRenderers;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.*;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.MooshroomMushroomFeatureRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import traben.entity_texture_features.texture_handlers.ETFManager;
import traben.entity_texture_features.texture_handlers.ETFTexture;
import traben.entity_texture_features.utils.ETFUtils2;

import static traben.entity_texture_features.ETFClient.ETFConfigData;
import static traben.entity_texture_features.texture_handlers.ETFManager.EMISSIVE_SUFFIX_LIST;

@Mixin(MooshroomMushroomFeatureRenderer.class)
public abstract class MixinMooshroomMushroomFeatureRenderer {

    private static final Identifier RED_SHROOM = new Identifier("textures/entity/cow/red_mushroom.png");
    private static final Identifier BROWN_SHROOM = new Identifier("textures/entity/cow/brown_mushroom.png");
    private static final ModelPart[] shroomAsEntityModel = getModelData();
    private static Identifier redEmissive = null;
    private static Identifier brownEmissive = null;

    private static ModelPart[] getModelData() {
        Dilation dilation = new Dilation(0);
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        modelPartData.addChild("shroom1", ModelPartBuilder.create().uv(32, 16).cuboid(0, 0F, 8.0F, 16.0F, 16.0F, 0F, dilation), ModelTransform.NONE);
        modelPartData.addChild("shroom2", ModelPartBuilder.create().uv(32, 16).cuboid(8F, 0F, 0.0F, 0F, 16F, 16.0F, dilation), ModelTransform.NONE);
        ModelPart shroom1 = modelData.getRoot().getChild("shroom1").createPart(32, 16);
        ModelPart shroom2 = modelData.getRoot().getChild("shroom2").createPart(32, 16);
        return new ModelPart[]{shroom1, shroom2};
    }

    @Nullable
    private static Boolean returnRedTrueBrownFalseVanillaNull(BlockState mushroomState) {
        //enable custom mooshroom mushrooms
        if (ETFConfigData.enableCustomTextures) {
            if (mushroomState.isOf(Blocks.RED_MUSHROOM)) {
                switch (ETFManager.mooshroomRedCustomShroom) {
                    case 1:
                        return null;
                    case 2:
                        return true;
                    default: {
                        if (MinecraftClient.getInstance().getResourceManager().getResource(RED_SHROOM).isPresent()) {
                            ETFManager.mooshroomRedCustomShroom = 2;
                            return prepareMushroomTextures(true);
                        } else {
                            ETFManager.mooshroomRedCustomShroom = 1;
                        }
                    }
                }
            } else if (mushroomState.isOf(Blocks.BROWN_MUSHROOM)) {
                switch (ETFManager.mooshroomBrownCustomShroom) {
                    case 1:
                        return null;
                    case 2:
                        return false;
                    default: {
                        if (MinecraftClient.getInstance().getResourceManager().getResource(BROWN_SHROOM).isPresent()) {

                            ETFManager.mooshroomBrownCustomShroom = 2;
                            return prepareMushroomTextures(false);
                        } else {
                            ETFManager.mooshroomBrownCustomShroom = 1;
                        }
                    }
                }
            }

        }
        return null;
    }

    //return isRed if valid else return null
    private static Boolean prepareMushroomTextures(boolean isRed) {
        return prepareMushroomTextures(isRed, false);
    }

    private static Boolean prepareMushroomTextures(boolean isRed, boolean doingEmissive) {
        Identifier idOfOriginal = isRed ? RED_SHROOM : BROWN_SHROOM;
        String suffix = null;
        if (doingEmissive) {
            boolean found = false;
            for (String str :
                    EMISSIVE_SUFFIX_LIST) {
                Identifier test = new Identifier(idOfOriginal.toString().replace(".png", str + ".png"));
                //System.out.println("trying "+test.toString());
                if (MinecraftClient.getInstance().getResourceManager().getResource(test).isPresent()) {
                    suffix = str;
                    idOfOriginal = test;
                    found = true;
                    break;
                }
            }
            if (!found) {
                return null;
            }
        }
        //System.out.println("found="+suffix);
        NativeImage originalImagePreFlip = ETFUtils2.getNativeImageElseNull(idOfOriginal);

        if (originalImagePreFlip != null) {
            try {
                //flip vertically
                NativeImage flippedOriginalImage = ETFUtils2.emptyNativeImage(originalImagePreFlip.getWidth(), originalImagePreFlip.getHeight());
                for (int x = 0; x < flippedOriginalImage.getWidth(); x++) {
                    for (int y = 0; y < flippedOriginalImage.getHeight(); y++) {
                        flippedOriginalImage.setColor(x, y, originalImagePreFlip.getColor(x, originalImagePreFlip.getHeight() - 1 - y));
                    }
                }
                //mirror 2x wide texture for entity rendering
                NativeImage newImage = ETFUtils2.emptyNativeImage(flippedOriginalImage.getWidth() * 2, flippedOriginalImage.getHeight());
                for (int x = 0; x < newImage.getWidth(); x++) {
                    for (int y = 0; y < newImage.getHeight(); y++) {
                        if (x < flippedOriginalImage.getWidth()) {
                            newImage.setColor(x, y, flippedOriginalImage.getColor(x, y));
                        } else {
                            newImage.setColor(x, y, flippedOriginalImage.getColor(flippedOriginalImage.getWidth() - 1 - (x - flippedOriginalImage.getWidth()), y));
                        }
                    }
                }
                Identifier idOfNew = isRed ? new Identifier("etf", "red_shroom_alt.png") : new Identifier("etf", "brown_shroom_alt.png");
                if (doingEmissive && suffix != null) {
                    Identifier emissive = new Identifier(idOfNew.toString().replace(".png", suffix + ".png"));
                    ETFUtils2.registerNativeImageToIdentifier(newImage, emissive);
                    if (isRed) {
                        redEmissive = emissive;
                    } else {
                        brownEmissive = emissive;
                    }
                } else {
                    ETFUtils2.registerNativeImageToIdentifier(newImage, idOfNew);
                }
                //System.out.println("id="+idOfNew);


                //do a pass for the emissive texture if present return ignored
                if (!doingEmissive) {
                    prepareMushroomTextures(isRed, true);
                    if (isRed) {
                        ETFManager.redMooshroomAlt = new ETFTexture(idOfNew, redEmissive);
                    } else {
                        ETFManager.brownMooshroomAlt = new ETFTexture(idOfNew, brownEmissive);
                    }
                }
                return isRed;
            } catch (Exception e) {
                ETFUtils2.logError("Mooshroom custom mushroom texture could not be loaded. " + e);
            }
        }
        return null;
    }


    //rewritten as original didn't seem to work, I must have accidentally changed the vanilla mushroom texture when testing originally
    @Inject(method = "renderMushroom", at = @At(value = "HEAD"), cancellable = true)
    private void etf$injected(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, boolean renderAsModel, BlockState mushroomState, int overlay, BakedModel mushroomModel, CallbackInfo ci) {

        Boolean shroomType = returnRedTrueBrownFalseVanillaNull(mushroomState);
        if (shroomType != null) {
            ETFTexture thisTexture = shroomType ? ETFManager.redMooshroomAlt : ETFManager.brownMooshroomAlt;
            if (thisTexture != null) {
                for (ModelPart model :
                        shroomAsEntityModel) {
                    VertexConsumer texturedConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(thisTexture.thisIdentifier));
                    model.render(matrices, texturedConsumer, light, overlay, 1, 1, 1, 1);

                    thisTexture.renderEmissive(matrices, vertexConsumers, model);
                    //ETFUtils2.generalEmissiveRenderPart(matrices, vertexConsumers, shroomType ? RED_SHROOM_ALT : BROWN_SHROOM_ALT, model, false);

                }
                ci.cancel();
            }
        }
        //else continue to vanilla code
    }
}