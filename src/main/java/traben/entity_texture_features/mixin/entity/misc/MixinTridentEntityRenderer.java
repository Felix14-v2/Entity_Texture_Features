package traben.entity_texture_features.mixin.entity.misc;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.TridentEntityRenderer;
import net.minecraft.client.render.entity.model.TridentEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

import static traben.entity_texture_features.ETFClient.ETFConfigData;
import static traben.entity_texture_features.ETFClient.UUID_TRIDENT_NAME;

@Mixin(TridentEntityRenderer.class)
public abstract class MixinTridentEntityRenderer implements SynchronousResourceReloader {
    @Shadow
    @Final
    private TridentEntityModel model;

    @Inject(method = "render(Lnet/minecraft/entity/projectile/TridentEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/TridentEntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;IIFFFF)V", shift = At.Shift.AFTER))
    private void etf$changeEmissiveTexture(TridentEntity tridentEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        if (ETFConfigData.enableTridents && ETFConfigData.enableEmissiveTextures) {
            UUID id = tridentEntity.getUuid();
            String path = TridentEntityModel.TEXTURE.toString();
            String name = UUID_TRIDENT_NAME.get(id) != null ? "_" + UUID_TRIDENT_NAME.get(id).toLowerCase().replaceAll("[^a-z\\d/_.-]", "") : "";
            String fileString = UUID_TRIDENT_NAME.get(id) != null ? path.replace(".png", "_" + name + ".png") : path;
            //todo rewrite ETFUtils.generalEmissiveRenderModel(matrixStack, vertexConsumerProvider, fileString, this.model);


        }
    }

    @Redirect(method = "render(Lnet/minecraft/entity/projectile/TridentEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/TridentEntityRenderer;getTexture(Lnet/minecraft/entity/projectile/TridentEntity;)Lnet/minecraft/util/Identifier;"))
    private Identifier etf$returnTexture(TridentEntityRenderer instance, TridentEntity tridentEntity) {
        if (ETFConfigData.enableTridents && ETFConfigData.enableCustomTextures) {
            UUID id = tridentEntity.getUuid();
            if (UUID_TRIDENT_NAME.get(id) != null) {
                String path = TridentEntityModel.TEXTURE.toString();
                String name = UUID_TRIDENT_NAME.get(id).toLowerCase().replaceAll("[^a-z\\d/_.-]", "");
                Identifier possibleId = new Identifier(path.replace(".png", "_" + name + ".png"));
                if (MinecraftClient.getInstance().getResourceManager().getResource(possibleId).isPresent()) {
                    return possibleId;
                }
            }

        }
        return instance.getTexture(tridentEntity);
    }
}


