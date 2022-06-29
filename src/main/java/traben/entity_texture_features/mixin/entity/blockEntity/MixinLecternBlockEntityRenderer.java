package traben.entity_texture_features.mixin.entity.blockEntity;

import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.LecternBlockEntityRenderer;
import net.minecraft.client.render.entity.model.BookModel;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import traben.entity_texture_features.texture_handlers.ETFManager;

import java.util.function.Function;

import static traben.entity_texture_features.ETFClient.ETFConfigData;
import static traben.entity_texture_features.texture_handlers.ETFManager.lecternHasCustomTexture;

@Mixin(LecternBlockEntityRenderer.class)
public abstract class MixinLecternBlockEntityRenderer implements BlockEntityRenderer<LecternBlockEntity> {

    private static final String LECTERN_BOOK_PATH = "minecraft:textures/entity/lectern_book.png";

    @Shadow
    @Final
    private BookModel book;
    private VertexConsumerProvider recentVert = null;

    @Inject(method = "render(Lnet/minecraft/block/entity/LecternBlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;II)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/BookModel;renderBook(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;IIFFFF)V",
                    shift = At.Shift.AFTER))
    private void etf$applyEmissiveBook(LecternBlockEntity lecternBlockEntity, float f, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, int j, CallbackInfo ci) {


        if (lecternHasCustomTexture == null)
            lecternHasCustomTexture = MinecraftClient.getInstance().getResourceManager().getResource(new Identifier(LECTERN_BOOK_PATH)).isPresent();

        String texture = (ETFConfigData.enableCustomTextures && lecternHasCustomTexture) ? LECTERN_BOOK_PATH : "minecraft:texture_handlers/entity/enchanting_table_book.png";//EnchantingTableBlockEntityRenderer.BOOK_TEXTURE.getTextureId().toString();

        VertexConsumer etf$vertex = ETFManager.getETFDefaultTexture(new Identifier(texture)).getEmissiveVertexConsumer(vertexConsumerProvider, null, ETFManager.EmissiveRenderModes.blockEntityMode());
        if (etf$vertex != null) {
            etf$redirectingEmissiveRender(matrixStack, etf$vertex, j);
        }
    }

    //so is not caught by other injects
    private void etf$redirectingEmissiveRender(MatrixStack matrixStack, VertexConsumer vertexConsumer, int overlay) {
        this.book.renderBook(matrixStack, vertexConsumer, LightmapTextureManager.MAX_LIGHT_COORDINATE, overlay, 1, 1, 1, 1);
    }

    @Inject(method = "render(Lnet/minecraft/block/entity/LecternBlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;II)V",
            at = @At(value = "HEAD"))
    private void etf$grabVertConsumer(LecternBlockEntity lecternBlockEntity, float f, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, int j, CallbackInfo ci) {
        recentVert = vertexConsumerProvider;
    }

    @Redirect(method = "render(Lnet/minecraft/block/entity/LecternBlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;II)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/SpriteIdentifier;getVertexConsumer(Lnet/minecraft/client/render/VertexConsumerProvider;Ljava/util/function/Function;)Lnet/minecraft/client/render/VertexConsumer;"))
    private VertexConsumer mixin(SpriteIdentifier instance, VertexConsumerProvider vertexConsumers, Function<Identifier, RenderLayer> layerFactory) {
        if (recentVert != null) {
            if (lecternHasCustomTexture == null) {
                lecternHasCustomTexture = MinecraftClient.getInstance().getResourceManager().getResource(new Identifier(LECTERN_BOOK_PATH)).isPresent();
            }
            if (lecternHasCustomTexture) {
                VertexConsumer vertCustom = recentVert.getBuffer(RenderLayer.getEntitySolid(new Identifier(LECTERN_BOOK_PATH)));
                if (vertCustom != null) {
                    return vertCustom;
                }
            }
        }
        return instance.getVertexConsumer(vertexConsumers, RenderLayer::getEntitySolid);
    }

}


