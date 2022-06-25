package traben.entity_texture_features.mixin.entity.featureRenderers;

import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.feature.VillagerClothingFeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.ModelWithHat;
import net.minecraft.entity.LivingEntity;
import net.minecraft.village.VillagerDataContainer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(VillagerClothingFeatureRenderer.class)
public abstract class MixinVillagerClothingFeatureRenderer<T extends LivingEntity & VillagerDataContainer, M extends EntityModel<T> & ModelWithHat> extends FeatureRenderer<T, M> {

    public MixinVillagerClothingFeatureRenderer(FeatureRendererContext<T, M> context) {
        super(context);
    }
    //todo rewrite
//
//    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/LivingEntity;FFFFFF)V",
//            at = @At(value = "HEAD"))
//    private void etf$getEntity(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, T livingEntity, float f, float g, float h, float j, float k, float l, CallbackInfo ci) {
//        etf$villager = livingEntity;
//    }
//
//    T etf$villager = null;
//
//    @Inject(method = "findTexture",
//            at = @At(value = "RETURN"), cancellable = true)
//    private void etf$returnAlteredTexture(String keyType, Identifier keyId, CallbackInfoReturnable<Identifier> cir) {
//        if (etf$villager != null) {
//            cir.setReturnValue(
//                    switch (keyType) {
//                        //base villager uses  suffix1
//                        case "type" -> etf$returnAltered(cir.getReturnValue(), UUID_RANDOM_TEXTURE_SUFFIX_2, UUID_HAS_UPDATABLE_RANDOM_CASES_2);
//                        case "profession" -> etf$returnAltered(cir.getReturnValue(), UUID_RANDOM_TEXTURE_SUFFIX_3, UUID_HAS_UPDATABLE_RANDOM_CASES_3);
//                        case "profession_level" -> etf$returnAltered(cir.getReturnValue(), UUID_RANDOM_TEXTURE_SUFFIX_4, UUID_HAS_UPDATABLE_RANDOM_CASES_4);
//                        default -> cir.getReturnValue();
//                    });
//
//
//        }
//
//
//    }
//
//
//    private Identifier etf$returnAltered(Identifier vanillaTexture, Object2IntOpenHashMap<UUID> UUID_RandomSuffixMap, Object2BooleanOpenHashMap<UUID> UUID_HasUpdateables) {
//        UUID id = etf$villager.getUuid();
//        if (ETFConfigData.enableCustomTextures) {
//            if (!PATH_OPTIFINE_OR_JUST_RANDOM.containsKey(vanillaTexture.toString())) {
//                ETFUtils.processNewRandomTextureCandidate(vanillaTexture.toString());
//            } else if (PATH_USES_OPTIFINE_OLD_VANILLA_ETF_0123.containsKey(vanillaTexture.toString())) {
//                if (PATH_OPTIFINE_OR_JUST_RANDOM.getBoolean(vanillaTexture.toString())) {
//                    if (!UUID_RandomSuffixMap.containsKey(id)) {
//                        ETFUtils.testCases(vanillaTexture.toString(), id, etf$villager, false, UUID_RandomSuffixMap, UUID_HasUpdateables);
//                        //if all failed set to vanilla
//                        if (!UUID_RandomSuffixMap.containsKey(id)) {
//                            UUID_RandomSuffixMap.put(id, 0);
//                        }
//                        //UUID_entityAlreadyCalculated.add(id);
//                    }
//                    if (UUID_RandomSuffixMap.containsKey(id)) {
//                        if (UUID_RandomSuffixMap.getInt(id) != 0) {
//                            Identifier randomTexture = ETFUtils.returnOptifineOrVanillaIdentifier(vanillaTexture.toString(), UUID_RandomSuffixMap.getInt(id));
//                            if (!PATH_IS_EXISTING_FEATURE.containsKey(randomTexture.toString())) {
//                                PATH_IS_EXISTING_FEATURE.put(randomTexture.toString(), ETFUtils.isExistingNativeImageFile(randomTexture));
//                            }
//                            if (PATH_IS_EXISTING_FEATURE.getBoolean(randomTexture.toString())) {
//                                //can use random texture
//                                return randomTexture;
//                            }
//                        }
//                    }
//                } else {
//                    UUID_HasUpdateables.put(id, false);
//                    if (PATH_TOTAL_TRUE_RANDOM.getInt(vanillaTexture.toString()) > 0) {
//                        if (!UUID_RandomSuffixMap.containsKey(id)) {
//                            int randomReliable = Math.abs(id.hashCode());
//                            randomReliable %= PATH_TOTAL_TRUE_RANDOM.getInt(vanillaTexture.toString());
//                            randomReliable++;
//                            if (randomReliable == 1 && PATH_IGNORE_ONE_PNG.getBoolean(vanillaTexture.toString())) {
//                                randomReliable = 0;
//                            }
//                            UUID_RandomSuffixMap.put(id, randomReliable);
//                            //UUID_entityAlreadyCalculated.add(id);
//                        }
//                        if (UUID_RandomSuffixMap.getInt(id) == 0) {
//                            return ETFUtils.returnBlinkIdOrGiven(etf$villager, vanillaTexture.toString(), id);
//                        } else {
//                            return ETFUtils.returnBlinkIdOrGiven(etf$villager, ETFUtils.returnOptifineOrVanillaPath(vanillaTexture.toString(), UUID_RandomSuffixMap.getInt(id), ""), id);
//                        }
//                    } else {
//                        return ETFUtils.returnBlinkIdOrGiven(etf$villager, vanillaTexture.toString(), id);
//                    }
//                }
//            }
//        }
//        return vanillaTexture;
//    }
}
