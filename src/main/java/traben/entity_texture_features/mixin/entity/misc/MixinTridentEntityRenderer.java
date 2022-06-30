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
import traben.entity_texture_features.texture_handlers.ETFManager;
import traben.entity_texture_features.texture_handlers.ETFTexture;
import traben.entity_texture_features.utils.ETFCacheKey;
import traben.entity_texture_features.utils.ETFUtils2;

import java.util.UUID;

import static traben.entity_texture_features.ETFClient.ETFConfigData;
import static traben.entity_texture_features.texture_handlers.ETFManager.ENTITY_TEXTURE_MAP;
import static traben.entity_texture_features.texture_handlers.ETFManager.UUID_TRIDENT_NAME;

@Mixin(TridentEntityRenderer.class)
public abstract class MixinTridentEntityRenderer implements SynchronousResourceReloader {
    @Shadow
    @Final
    private TridentEntityModel model;
    private ETFTexture thisETFTexture = null;

    @Inject(method = "render(Lnet/minecraft/entity/projectile/TridentEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/TridentEntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;IIFFFF)V", shift = At.Shift.AFTER))
    private void etf$changeEmissiveTexture(TridentEntity tridentEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        if (ETFConfigData.enableTridents) {

            if (thisETFTexture != null)
                thisETFTexture.renderEmissive(matrixStack, vertexConsumerProvider, this.model, ETFManager.EmissiveRenderModes.BRIGHT);


        }
    }

    @Redirect(method = "render(Lnet/minecraft/entity/projectile/TridentEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/TridentEntityRenderer;getTexture(Lnet/minecraft/entity/projectile/TridentEntity;)Lnet/minecraft/util/Identifier;"))
    private Identifier etf$returnTexture(TridentEntityRenderer instance, TridentEntity tridentEntity) {
        if (ETFConfigData.enableTridents) {
            UUID id = tridentEntity.getUuid();
            ETFCacheKey key = new ETFCacheKey(id, null);
            if (ENTITY_TEXTURE_MAP.containsKey(key)) {
                thisETFTexture = ENTITY_TEXTURE_MAP.get(key);
                if (thisETFTexture != null) {
                    return thisETFTexture.thisIdentifier;
                }
            } else {
                if (UUID_TRIDENT_NAME.get(id) != null) {
                    String path = TridentEntityModel.TEXTURE.toString();
                    String name = UUID_TRIDENT_NAME.get(id).toLowerCase().replaceAll("[^a-z\\d/_.-]", "");
                    Identifier possibleId = new Identifier(path.replace(".png", "_" + name + ".png"));
                    if (MinecraftClient.getInstance().getResourceManager().getResource(possibleId).isPresent()) {
                        Identifier emissive = ETFUtils2.replaceIdentifier(possibleId, ".png", "_e.png");
                        if (MinecraftClient.getInstance().getResourceManager().getResource(emissive).isEmpty()) {
                            emissive = null;
                        }
                        ENTITY_TEXTURE_MAP.put(key, new ETFTexture(possibleId, emissive));
                        return possibleId;
                    }
                }
            }
        }
        return instance.getTexture(tridentEntity);
    }
}


