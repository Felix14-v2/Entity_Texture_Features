package traben.entity_texture_features.texture_handlers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import traben.entity_texture_features.utils.ETFUtils2;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static traben.entity_texture_features.ETFClient.ETFConfigData;
import static traben.entity_texture_features.ETFClient.MOD_ID;
import static traben.entity_texture_features.texture_handlers.ETFManager.PLAYER_TEXTURE_MAP;


//this is effectively a pre-processor for a child ETFTexture
//this class will initialize a skin download and will then create any needed emissive / blinking textures
//so that an ETFTexture can be created and used for regular rendering
//additional features requiring further rendering will then be handled here also
public class ETFPlayerTexture {

    public static final String SKIN_NAMESPACE = "etf_skin";
    public static final UUID Dev = UUID.fromString("fd22e573-178c-415a-94fe-e476b328abfd");
    //public static final UUID Dev2 = UUID.fromString("bc2d6979-ddde-4452-8c7d-caefa4aceb01");
    public static final UUID Wife = UUID.fromString("cab7d2e2-519f-4b34-afbd-b65f4542b8a1");
    public Identifier baseEnchantIdentifier = null;
    public Identifier baseEnchantBlinkIdentifier = null;
    public Identifier baseEnchantBlink2Identifier = null;
    public Identifier etfCapeIdentifier = null;
    boolean hasEmissives = false;
    boolean hasEnchant = false;
    boolean hasVillagerNose = false;
    PlayerEntity player;
    private ETFCustomPlayerFeatureModel<PlayerEntity> customPlayerModel;
    private boolean isTextureReady = false;
    private boolean hasFeatures = false;
    private boolean hasVanillaCape = false;
    private NativeImage originalSkin;
    private NativeImage originalCape;
    private boolean allowThisETFBaseSkin = true;
    private Identifier coatIdentifier = null;
    private Identifier coatEmissiveIdentifier = null;
    private Identifier coatEnchantedIdentifier = null;
    private int[] enchantCapeBounds = null;
    private int[] emissiveCapeBounds = null;
    private Identifier etfCapeEmissiveIdentifier = null;
    private Identifier etfCapeEnchantedIdentifier = null;
    private boolean hasFatCoat = false;
    //provides emissive patching and blinking functionality
    //all ETFPlayerTexture needs to do is build those textures and register them before this ETFTexture is made, and it will auto locate and apply them
    private ETFTexture etfTextureOfFinalBaseSkin;


    ETFPlayerTexture(PlayerEntity player) {
        //initiate texture download as we need unprocessed texture from the skin server
        this.player = player;
        triggerSkinDownload();
    }

    @Nullable
    private static NativeImage returnMatchPixels(NativeImage baseSkin, int[] boundsToCheck) {
        return returnMatchPixels(baseSkin, boundsToCheck, null);
    }

    // returns a native image with only pixels that match those contained in the boundsToCheck region of baseSkin
    // if second is not null it will return an altered version of that instead of baseSkin
    // will also return null if there is nothing to check or no matching pixels
    @Nullable
    private static NativeImage returnMatchPixels(NativeImage baseSkin, int[] boundsToCheck, @SuppressWarnings("SameParameterValue") @Nullable NativeImage second) {
        if (baseSkin == null) return null;

        boolean secondImage = second != null;
        Set<Integer> matchColors = new HashSet<>();
        for (int x = boundsToCheck[0]; x <= boundsToCheck[2]; x++) {
            for (int y = boundsToCheck[1]; y <= boundsToCheck[3]; y++) {
                if (baseSkin.getOpacity(x, y) != 0) {
                    matchColors.add(baseSkin.getColor(x, y));
                }
            }
        }
        if (matchColors.size() == 0) {
            return null;
        } else {
            NativeImage texture = !secondImage ? new NativeImage(baseSkin.getWidth(), baseSkin.getHeight(), false) : new NativeImage(second.getWidth(), second.getHeight(), false);
            if (!secondImage) {
                texture.copyFrom(baseSkin);
            } else {
                texture.copyFrom(second);
            }
            for (int x = 0; x < texture.getWidth(); x++) {
                for (int y = 0; y < texture.getHeight(); y++) {
                    if (!matchColors.contains(baseSkin.getColor(x, y))) {
                        texture.setColor(x, y, 0);
                    }
                }
            }
            return returnNullIfEmptyImage(texture);
        }

    }

    @Nullable
    private static NativeImage returnNullIfEmptyImage(NativeImage imageToCheck) {
        boolean foundAPixel = false;
        upper:
        for (int x = 0; x < imageToCheck.getWidth(); x++) {
            for (int y = 0; y < imageToCheck.getHeight(); y++) {
                if (imageToCheck.getColor(x, y) != 0) {
                    foundAPixel = true;
                    break upper;
                }
            }
        }
        return foundAPixel ? imageToCheck : null;
    }


    private static NativeImage returnCustomTexturedCape(NativeImage skin) {
        NativeImage cape = ETFUtils2.emptyNativeImage(64, 32);
        NativeImage elytra = ETFUtils2.getNativeImageElseNull(new Identifier("textures/entity/elytra.png"));
        if (elytra == null || elytra.getWidth() != 64 || elytra.getHeight() != 32) {
            elytra = ETFUtils2.getNativeImageElseNull(new Identifier("etf:textures/capes/default_elytra.png"));
        }//not else
        if (elytra != null) {
            cape.copyFrom(elytra);
        }
        copyToPixels(skin, cape, getSkinPixelBounds("cape1"), 1, 1);
        copyToPixels(skin, cape, getSkinPixelBounds("cape1"), 12, 1);
        copyToPixels(skin, cape, getSkinPixelBounds("cape2"), 1, 5);
        copyToPixels(skin, cape, getSkinPixelBounds("cape2"), 12, 5);
        copyToPixels(skin, cape, getSkinPixelBounds("cape3"), 1, 9);
        copyToPixels(skin, cape, getSkinPixelBounds("cape3"), 12, 9);
        copyToPixels(skin, cape, getSkinPixelBounds("cape4"), 1, 13);
        copyToPixels(skin, cape, getSkinPixelBounds("cape4"), 12, 13);
        copyToPixels(skin, cape, getSkinPixelBounds("cape5.1"), 9, 1);
        copyToPixels(skin, cape, getSkinPixelBounds("cape5.1"), 20, 1);
        copyToPixels(skin, cape, getSkinPixelBounds("cape5.2"), 9, 5);
        copyToPixels(skin, cape, getSkinPixelBounds("cape5.2"), 20, 5);
        copyToPixels(skin, cape, getSkinPixelBounds("cape5.3"), 9, 9);
        copyToPixels(skin, cape, getSkinPixelBounds("cape5.3"), 20, 9);
        copyToPixels(skin, cape, getSkinPixelBounds("cape5.4"), 9, 13);
        copyToPixels(skin, cape, getSkinPixelBounds("cape5.4"), 20, 13);

        copyToPixels(cape, cape, getSkinPixelBounds("capeVertL"), 0, 1);
        copyToPixels(cape, cape, getSkinPixelBounds("capeVertR"), 11, 1);
        copyToPixels(cape, cape, getSkinPixelBounds("capeHorizL"), 1, 0);
        copyToPixels(cape, cape, getSkinPixelBounds("capeHorizR"), 11, 0);

        return cape;
    }

    private static int[] getSkinPixelBounds(String choiceKey) {
        return switch (choiceKey) {
            case "marker1" -> new int[]{56, 16, 63, 23};
            case "marker2" -> new int[]{56, 24, 63, 31};
            case "marker3" -> new int[]{56, 32, 63, 39};
            case "marker4" -> new int[]{56, 40, 63, 47};
            case "optimizedEyeSmall" -> new int[]{12, 16, 19, 16};
            case "optimizedEye2High" -> new int[]{12, 16, 19, 17};
            case "optimizedEye2High_second" -> new int[]{12, 18, 19, 19};
            case "optimizedEye4High" -> new int[]{12, 16, 19, 19};
            case "optimizedEye4High_second" -> new int[]{36, 16, 43, 19};
            case "face1" -> new int[]{0, 0, 7, 7};
            case "face2" -> new int[]{24, 0, 31, 7};
            case "face3" -> new int[]{32, 0, 39, 7};
            case "face4" -> new int[]{56, 0, 63, 7};
            case "cape1" -> new int[]{12, 32, 19, 35};
            case "cape2" -> new int[]{36, 32, 43, 35};
            case "cape3" -> new int[]{12, 48, 19, 51};
            case "cape4" -> new int[]{28, 48, 35, 51};
            case "cape5.1" -> new int[]{44, 48, 45, 51};
            case "cape5.2" -> new int[]{46, 48, 47, 51};
            case "cape5.3" -> new int[]{48, 48, 49, 51};
            case "cape5.4" -> new int[]{50, 48, 51, 51};
            case "capeVertL" -> new int[]{1, 1, 1, 16};
            case "capeVertR" -> new int[]{10, 1, 10, 16};
            case "capeHorizL" -> new int[]{1, 1, 10, 1};
            case "capeHorizR" -> new int[]{1, 16, 10, 16};
            default -> new int[]{0, 0, 0, 0};
        };
    }

    private static NativeImage returnOptimizedBlinkFace(NativeImage baseSkin, int[] eyeBounds, int eyeHeightFromTopDown) {
        return returnOptimizedBlinkFace(baseSkin, eyeBounds, eyeHeightFromTopDown, null);
    }

    private static NativeImage returnOptimizedBlinkFace(NativeImage baseSkin, int[] eyeBounds, int eyeHeightFromTopDown, int[] secondLayerBounds) {
        NativeImage texture = new NativeImage(64, 64, false);
        texture.copyFrom(baseSkin);
        //copy face
        copyToPixels(baseSkin, texture, eyeBounds, 8, 8 + (eyeHeightFromTopDown - 1));
        //copy face overlay
        if (secondLayerBounds != null) {
            copyToPixels(baseSkin, texture, secondLayerBounds, 40, 8 + (eyeHeightFromTopDown - 1));
        }
        return texture;
    }

    private static int countTransparentInBox(NativeImage img, int x1, int y1, int x2, int y2) {
        int counter = 0;
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                //ranges from  0 to 127  then wraps around negatively -127 to -1  totalling 0 to 255
                int i = img.getOpacity(x, y);
                if (i < 0) {
                    i += 256;
                }
                //adjusted to 0 to 256
                counter += i;

            }
        }
        return counter;
    }

    private static boolean isSkinNotTooTransparent(NativeImage skin) {
        if (ETFConfigData.skinFeaturesEnableFullTransparency) {
            return true;
        } else {
            int countTransparent = 0;
            //map of bottom skin layer
            countTransparent += countTransparentInBox(skin, 8, 0, 23, 15);
            countTransparent += countTransparentInBox(skin, 0, 20, 55, 31);
            countTransparent += countTransparentInBox(skin, 0, 8, 7, 15);
            countTransparent += countTransparentInBox(skin, 24, 8, 31, 15);
            countTransparent += countTransparentInBox(skin, 0, 16, 11, 19);
            countTransparent += countTransparentInBox(skin, 20, 16, 35, 19);
            countTransparent += countTransparentInBox(skin, 44, 16, 51, 19);
            countTransparent += countTransparentInBox(skin, 20, 48, 27, 51);
            countTransparent += countTransparentInBox(skin, 36, 48, 43, 51);
            countTransparent += countTransparentInBox(skin, 16, 52, 47, 63);
            //do not allow skins under 40% ish total opacity
            //1648 is total pixels that are not allowed transparent by vanilla
            int average = (countTransparent / 1648); // should be 0 to 256
            //System.out.println("average ="+average);
            return average >= 100;
        }
    }

    private static NativeImage getCoatTexture(NativeImage skin, int lengthOfCoat, boolean ignoreTopTexture) {

        NativeImage coat = new NativeImage(64, 64, false);
        coat.fillRect(0, 0, 64, 64, 0);

        //top
        if (!ignoreTopTexture) {
            copyToPixels(skin, coat, 4, 32, 7, 35 + lengthOfCoat, 20, 32);
            copyToPixels(skin, coat, 4, 48, 7, 51 + lengthOfCoat, 24, 32);
        }
        //sides
        copyToPixels(skin, coat, 0, 36, 7, 36 + lengthOfCoat, 16, 36);
        copyToPixels(skin, coat, 12, 36, 15, 36 + lengthOfCoat, 36, 36);
        copyToPixels(skin, coat, 4, 52, 15, 52 + lengthOfCoat, 24, 36);
//        //ENCHANT AND EMISSIVES
//        copyToPixels(skin, coat, 56, 16, 63, 47, 0, 0);
        return coat;

    }

    private static void copyToPixels(NativeImage source, NativeImage dest, int[] bounds, int copyToX, int CopyToY) {
        copyToPixels(source, dest, bounds[0], bounds[1], bounds[2], bounds[3], copyToX, CopyToY);
    }

    private static void copyToPixels(NativeImage source, NativeImage dest, int x1, int y1, int x2, int y2, int copyToX, int copyToY) {
        int copyToXRelative = copyToX - x1;
        int copyToYRelative = copyToY - y1;
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                dest.setColor(x + copyToXRelative, y + copyToYRelative, source.getColor(x, y));
            }
        }
    }

    private static void deletePixels(NativeImage source, int x1, int y1, int x2, int y2) {
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                source.setColor(x, y, 0);
            }
        }
    }

    private static int getSkinPixelColourToNumber(int color) {
        //            pink   cyan     red       green      brown    blue     orange     yellow
        //colours = -65281, -256, -16776961, -16711936, -16760705, -65536, -16744449, -14483457
        return switch (color) {
            case -65281 -> 1;
            case -256 -> 2;
            case -16776961 -> 3;
            case -16711936 -> 4;
            case -16760705 -> 5;
            case -65536 -> 6;
            case -16744449 -> 7;
            case -14483457 -> 8;
            case -12362096 -> 666; //villager nose color
            default -> 0;
        };
    }

    public static void printPlayerSkinCopyWithFeatureOverlay(NativeImage skinImage) {
        if (FabricLoader.getInstance().isModLoaded("fabric")) {
            Path outputDirectory = Path.of(FabricLoader.getInstance().getGameDir().toString(), "ETF_player_skin_printout.png");
            //NativeImage skinImage = ETFUtils.getNativeImageFromID(new Identifier(SKIN_NAMESPACE + playerID + ".png"));
            NativeImage skinFeatureImage = ETFUtils2.getNativeImageElseNull(new Identifier(MOD_ID, "textures/skin_feature_printout.png"));
            try {
                for (int x = 0; x < skinImage.getWidth(); x++) {
                    for (int y = 0; y < skinImage.getHeight(); y++) {
                        //noinspection ConstantConditions
                        if (skinFeatureImage.getColor(x, y) != 0) {
                            skinImage.setColor(x, y, skinFeatureImage.getColor(x, y));
                        }
                    }
                }
                skinImage.writeTo(outputDirectory);
                ETFUtils2.logMessage("Skin feature layout successfully applied to a copy of your skin and has been saved to the minecraft directory.", true);
            } catch (Exception e) {
                ETFUtils2.logMessage("Skin feature layout could not be applied to a copy of your skin and has not been saved. Error written to log.", true);
                ETFUtils2.logError(e.toString(), false);
            }

        } else {
            //requires fab api to read from mod resources
            ETFUtils2.logError("Fabric API required for example skin printout, cancelling.", true);
        }
    }

    public boolean hasCustomCape() {
        return etfCapeIdentifier != null;
    }

    @Nullable
    public Identifier getBaseTextureIdentifierOrNullForVanilla(PlayerEntity player) {
        this.player = player;//refresh player data
        if (allowThisETFBaseSkin && canUseFeaturesForThisPlayer() && etfTextureOfFinalBaseSkin != null) {
            return etfTextureOfFinalBaseSkin.getTextureIdentifier(player);
        }
        return null;
    }

    @Nullable
    public Identifier getBaseTextureEmissiveIdentifierOrNullForNone() {
        if (hasEmissives && canUseFeaturesForThisPlayer() && etfTextureOfFinalBaseSkin != null) {
            return etfTextureOfFinalBaseSkin.getEmissiveIdentifierOfCurrentState();
        }
        return null;
    }

    public void renderCapeAndFeatures(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, PlayerEntityModel<AbstractClientPlayerEntity> model) {
        if (canUseFeaturesForThisPlayer()) {
            if (etfCapeIdentifier != null) {
                VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(RenderLayer.getEntityTranslucent(etfCapeIdentifier));
                model.renderCape(matrixStack, vertexConsumer, light, OverlayTexture.DEFAULT_UV);

                if (etfCapeEmissiveIdentifier != null) {
                    VertexConsumer emissiveVert = vertexConsumerProvider.getBuffer(RenderLayer.getEntityTranslucent(etfCapeEmissiveIdentifier));
                    model.renderCape(matrixStack, emissiveVert, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV);
                }
                if (etfCapeEnchantedIdentifier != null) {
                    VertexConsumer enchantVert = ItemRenderer.getArmorGlintConsumer(vertexConsumerProvider, RenderLayer.getArmorCutoutNoCull(etfCapeEnchantedIdentifier), false, true);
                    model.renderCape(matrixStack, enchantVert, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV);
                }

            }
        }
    }

    public boolean canUseFeaturesForThisPlayer() {
        return isTextureReady
                && hasFeatures
                && (//not on enemy team or doesn't matter
                ETFConfigData.enableEnemyTeamPlayersSkinFeatures
                        || (player.isTeammate(MinecraftClient.getInstance().player)
                        || player.getScoreboardTeam() == null));
    }

    public void renderFeatures(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, PlayerEntityModel<PlayerEntity> model) {
        if (canUseFeaturesForThisPlayer()) {
            //villager nose
            if (hasVillagerNose) {
                customPlayerModel.nose.copyTransform(model.head);
                Identifier villager = new Identifier("textures/entity/villager/villager.png");
                VertexConsumer villagerVert = vertexConsumerProvider.getBuffer(RenderLayer.getEntitySolid(villager));
                customPlayerModel.nose.render(matrixStack, villagerVert, light, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
            }

            //coat features
            ItemStack armour = player.getInventory().getArmorStack(1);
            if (coatIdentifier != null &&
                    player.isPartVisible(PlayerModelPart.JACKET) &&
                    !(armour.isOf(Items.CHAINMAIL_LEGGINGS) ||
                            armour.isOf(Items.LEATHER_LEGGINGS) ||
                            armour.isOf(Items.DIAMOND_LEGGINGS) ||
                            armour.isOf(Items.GOLDEN_LEGGINGS) ||
                            armour.isOf(Items.IRON_LEGGINGS) ||
                            armour.isOf(Items.NETHERITE_LEGGINGS))
            ) {
                //String coat = ETFPlayerSkinUtils.SKIN_NAMESPACE + id + "_coat.png";

                if (hasFatCoat) {
                    customPlayerModel.fatJacket.copyTransform(model.jacket);
                } else {
                    customPlayerModel.jacket.copyTransform(model.jacket);
                }
                //perform texture features
                VertexConsumer coatVert = vertexConsumerProvider.getBuffer(RenderLayer.getEntityTranslucent(coatIdentifier));
                matrixStack.push();
                if (hasFatCoat) {
                    customPlayerModel.fatJacket.render(matrixStack, coatVert, light, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
                } else {
                    customPlayerModel.jacket.render(matrixStack, coatVert, light, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
                }
                if (coatEnchantedIdentifier != null) {
                    VertexConsumer enchantVert = ItemRenderer.getArmorGlintConsumer(vertexConsumerProvider, RenderLayer.getArmorCutoutNoCull(coatEnchantedIdentifier), false, true);
                    if (hasFatCoat) {
                        customPlayerModel.fatJacket.render(matrixStack, enchantVert, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 0.16F);
                    } else {
                        customPlayerModel.jacket.render(matrixStack, enchantVert, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 0.16F);
                    }
                }


                if (coatEmissiveIdentifier != null) {
                    VertexConsumer emissiveVert;// = vertexConsumerProvider.getBuffer(RenderLayer.getBeaconBeam(emissive, true));
                    if (ETFManager.getEmissiveMode() == ETFManager.EmissiveRenderModes.BRIGHT) {
                        emissiveVert = vertexConsumerProvider.getBuffer(RenderLayer.getBeaconBeam(coatEmissiveIdentifier, true));
                    } else {
                        emissiveVert = vertexConsumerProvider.getBuffer(RenderLayer.getEntityTranslucent(coatEmissiveIdentifier));
                    }

                    if (hasFatCoat) {
                        customPlayerModel.fatJacket.render(matrixStack, emissiveVert, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
                    } else {
                        customPlayerModel.jacket.render(matrixStack, emissiveVert, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
                    }
                }

                matrixStack.pop();
            }

            //perform texture features
            if (hasEnchant && baseEnchantIdentifier != null) {
                VertexConsumer enchantVert = ItemRenderer.getArmorGlintConsumer(vertexConsumerProvider, RenderLayer.getArmorCutoutNoCull(
                        switch (etfTextureOfFinalBaseSkin.currentTextureState) {
                            case BLINK, BLINK_PATCHED, APPLY_BLINK -> baseEnchantBlinkIdentifier;
                            case BLINK2, BLINK2_PATCHED, APPLY_BLINK2 -> baseEnchantBlink2Identifier;
                            default -> baseEnchantIdentifier;
                        }), false, true);
                model.render(matrixStack, enchantVert, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 0.16F);
            }
            if (hasEmissives && etfTextureOfFinalBaseSkin != null) {
                etfTextureOfFinalBaseSkin.renderEmissive(matrixStack, vertexConsumerProvider, model);
            }
        }
    }

    private void triggerSkinDownload() {
        UUID id = player.getUuid();
        try {
            @SuppressWarnings("ConstantConditions") //in a try for a reason
            PlayerListEntry playerListEntry = MinecraftClient.getInstance().getNetworkHandler().getPlayerListEntry(id);
            @SuppressWarnings("ConstantConditions") //in a try for a reason
            GameProfile gameProfile = playerListEntry.getProfile();
            Collection<Property> textureData = gameProfile.getProperties().get("textures");

            String skinUrl = "";
            String capeUrl = null;

            for (Property p :
                    textureData) {
                JsonObject props = JsonParser.parseString(new String(Base64.getDecoder().decode((p.getValue())))).getAsJsonObject();
                skinUrl = ((JsonObject) ((JsonObject) props.get("textures")).get("SKIN")).get("url").getAsString();
                try {
                    capeUrl = ((JsonObject) ((JsonObject) props.get("textures")).get("CAPE")).get("url").getAsString();
                    hasVanillaCape = true;
                } catch (Exception e) {
                    //
                }
                break;
            }

            String finalSkinUrl = skinUrl;
            CompletableFuture.runAsync(() -> {
                HttpURLConnection httpURLConnection;
                try {
                    httpURLConnection = (HttpURLConnection) (new URL(finalSkinUrl)).openConnection(MinecraftClient.getInstance().getNetworkProxy());
                    httpURLConnection.setDoInput(true);
                    httpURLConnection.setDoOutput(false);
                    httpURLConnection.connect();
                    if (httpURLConnection.getResponseCode() / 100 == 2) {
                        InputStream inputStream = httpURLConnection.getInputStream();

                        MinecraftClient.getInstance().execute(() -> {
                            try {
                                NativeImage one = NativeImage.read(inputStream);
                                this.receiveSkin(one);
                            } catch (Exception e) {
                                ETFUtils2.logError("triggerSkinDownload()2 failed for player: " + player.getName().getString() + " retrying again later, reason was: " + e);
                                this.skinFailed(false);
                            }


                        });
                    }
                } catch (Exception var6) {
                    ETFUtils2.logError("triggerSkinDownload()3 failed for player:" + player.getName().getString() + "retrying again later");
                    this.skinFailed(false);
                }

            }, Util.getMainWorkerExecutor());

            if (this.hasVanillaCape && capeUrl != null) {
                String finalCapeUrl = capeUrl;
                CompletableFuture.runAsync(() -> {
                    HttpURLConnection httpURLConnection;
                    try {
                        httpURLConnection = (HttpURLConnection) (new URL(finalCapeUrl)).openConnection(MinecraftClient.getInstance().getNetworkProxy());
                        httpURLConnection.setDoInput(true);
                        httpURLConnection.setDoOutput(false);
                        httpURLConnection.connect();
                        if (httpURLConnection.getResponseCode() / 100 == 2) {
                            InputStream inputStream = httpURLConnection.getInputStream();

                            MinecraftClient.getInstance().execute(() -> {
                                try {
                                    NativeImage one = NativeImage.read(inputStream);
                                    this.receiveCape(one);
                                } catch (Exception e) {
                                    ETFUtils2.logError("triggerSkinDownload()4 failed for player:" + player.getName().getString() + "retrying again later");
                                    this.skinFailed(false);
                                }


                            });
                        }
                    } catch (Exception var6) {
                        ETFUtils2.logError("triggerSkinDownload()5 failed for player:" + player.getName().getString() + "retrying again later");
                        this.skinFailed(false);
                    }

                }, Util.getMainWorkerExecutor());

            }

        } catch (Exception e) {
            ETFUtils2.logError("triggerSkinDownload() failed for player:" + player.getName().getString() + "retrying again later");
            skinFailed(false);
        }
    }

    private void initiateThirdPartyCapeDownload(String capeUrl) {
        CompletableFuture.runAsync(() -> {
            HttpURLConnection httpURLConnection;
            try {
                httpURLConnection = (HttpURLConnection) (new URL(capeUrl)).openConnection(MinecraftClient.getInstance().getNetworkProxy());
                httpURLConnection.setDoInput(true);
                httpURLConnection.setDoOutput(false);
                httpURLConnection.connect();
                if (httpURLConnection.getResponseCode() / 100 == 2) {
                    InputStream inputStream = httpURLConnection.getInputStream();

                    MinecraftClient.getInstance().execute(() -> {
                        try {
                            NativeImage one = NativeImage.read(inputStream);
                            this.receiveThirdPartyCape(one);
                        } catch (Exception e) {
                            ETFUtils2.logError("ThirdPartyCapeDownload failed for player:" + player.getName().getString() + "retrying again later");
                            //this.skinFailed(false);
                        }
                    });
                }
            } catch (Exception var6) {
                ETFUtils2.logError("ThirdPartyCapeDownload failed for player:" + player.getName().getString() + "retrying again later");
                //this.skinFailed(false);
            }
        }, Util.getMainWorkerExecutor());
    }

    public void receiveThirdPartyCape(@NotNull NativeImage capeImage) {
        //optifine resizes them for space cause expensive servers I guess
        etfCapeIdentifier = new Identifier(SKIN_NAMESPACE, player.getUuid() + "_cape_third_party.png");
        if (capeImage.getWidth() % capeImage.getHeight() != 0) {
            //resize optifine image
            int newWidth = 64;
            while (newWidth < capeImage.getWidth()) {
                newWidth = newWidth + newWidth;
            }
            int newHeight = newWidth / 2;
            NativeImage resizedImage = ETFUtils2.emptyNativeImage(newWidth, newHeight);
            for (int x = 0; x < capeImage.getWidth(); x++) {
                for (int y = 0; y < capeImage.getHeight(); y++) {
                    resizedImage.setColor(x, y, capeImage.getColor(x, y));
                }
            }
        }
        ETFUtils2.registerNativeImageToIdentifier(capeImage, etfCapeIdentifier);

        NativeImage checkCapeEmissive = returnMatchPixels(originalSkin, emissiveCapeBounds, capeImage);
        //UUID_PLAYER_HAS_EMISSIVE_CAPE.put(id, checkCape != null);
        if (checkCapeEmissive != null) {
            etfCapeEmissiveIdentifier = new Identifier(SKIN_NAMESPACE, player.getUuid() + "_cape_third_party_e.png");
            ETFUtils2.registerNativeImageToIdentifier(checkCapeEmissive, etfCapeEmissiveIdentifier);
        }
        NativeImage checkCapeEnchant = returnMatchPixels(originalSkin, enchantCapeBounds, capeImage);
        //UUID_PLAYER_HAS_EMISSIVE_CAPE.put(id, checkCape != null);
        if (checkCapeEnchant != null) {
            etfCapeEnchantedIdentifier = new Identifier(SKIN_NAMESPACE, player.getUuid() + "_cape_third_party_enchant.png");
            ETFUtils2.registerNativeImageToIdentifier(checkCapeEnchant, etfCapeEnchantedIdentifier);
        }
    }

    private void skinFailed(boolean preventFurtherChecks) {
        if (preventFurtherChecks) {
            PLAYER_TEXTURE_MAP.put(player.getUuid(), null);
        } else {
            PLAYER_TEXTURE_MAP.removeEntryOnly(player.getUuid());
        }
        //this object is now unreachable
    }

    public void receiveSkin(@NotNull NativeImage skinImage) {
        this.originalSkin = skinImage;
        if (!this.hasVanillaCape || this.originalCape != null) {
            onTexturesDownloaded();
        }


    }

    public void receiveCape(@NotNull NativeImage capeImage) {
        this.originalCape = capeImage;
        if (this.originalSkin != null) {
            onTexturesDownloaded();
        }
    }

    public void onTexturesDownloaded() {
        UUID id = player.getUuid();
        NativeImage modifiedCape;
        if (originalCape != null) {
            modifiedCape = ETFUtils2.emptyNativeImage(originalCape.getWidth(), originalCape.getHeight());
            modifiedCape.copyFrom(originalCape);
        } else {
            modifiedCape = null;
        }
        NativeImage modifiedSkin = ETFUtils2.emptyNativeImage(originalSkin.getWidth(), originalSkin.getHeight());
        modifiedSkin.copyFrom(originalSkin);

        if (ETFConfigData.skinFeaturesPrintETFReadySkin && MinecraftClient.getInstance().player != null && id.equals(MinecraftClient.getInstance().player.getUuid())) {
            ETFUtils2.logMessage("Skin feature layout is being applied to a copy of your skin please wait...", true);
            printPlayerSkinCopyWithFeatureOverlay(originalSkin);
            ETFConfigData.skinFeaturesPrintETFReadySkin = false;
            ETFUtils2.saveConfig();
        }
        if (originalSkin != null) {
            if (originalSkin.getColor(1, 16) == -16776961 &&
                    originalSkin.getColor(0, 16) == -16777089 &&
                    originalSkin.getColor(0, 17) == -16776961 &&
                    originalSkin.getColor(2, 16) == -16711936 &&
                    originalSkin.getColor(3, 16) == -16744704 &&
                    originalSkin.getColor(3, 17) == -16711936 &&
                    originalSkin.getColor(0, 18) == -65536 &&
                    originalSkin.getColor(0, 19) == -8454144 &&
                    originalSkin.getColor(1, 19) == -65536 &&
                    originalSkin.getColor(3, 18) == -1 &&
                    originalSkin.getColor(2, 19) == -1 &&
                    originalSkin.getColor(3, 18) == -1
            ) {
                customPlayerModel = new ETFCustomPlayerFeatureModel<>();

                hasFeatures = true;
                ETFUtils2.logMessage("Found Player {" + player.getName().getString() + "} with texture features in skin.", false);
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                //locate and convert choices to ints
                int[] choiceBoxChoices = {getSkinPixelColourToNumber(originalSkin.getColor(52, 16)),
                        getSkinPixelColourToNumber(originalSkin.getColor(52, 17)),
                        getSkinPixelColourToNumber(originalSkin.getColor(52, 18)),
                        getSkinPixelColourToNumber(originalSkin.getColor(52, 19)),
                        getSkinPixelColourToNumber(originalSkin.getColor(53, 16)),
                        getSkinPixelColourToNumber(originalSkin.getColor(53, 17))};
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                boolean noseUpper = (getSkinPixelColourToNumber(originalSkin.getColor(43, 13)) == 666 && getSkinPixelColourToNumber(originalSkin.getColor(44, 13)) == 666 &&
                        getSkinPixelColourToNumber(originalSkin.getColor(43, 14)) == 666 && getSkinPixelColourToNumber(originalSkin.getColor(44, 14)) == 666 &&
                        getSkinPixelColourToNumber(originalSkin.getColor(43, 15)) == 666 && getSkinPixelColourToNumber(originalSkin.getColor(44, 15)) == 666);
                boolean noseLower = (getSkinPixelColourToNumber(originalSkin.getColor(11, 13)) == 666 && getSkinPixelColourToNumber(originalSkin.getColor(12, 13)) == 666 &&
                        getSkinPixelColourToNumber(originalSkin.getColor(11, 14)) == 666 && getSkinPixelColourToNumber(originalSkin.getColor(12, 14)) == 666 &&
                        getSkinPixelColourToNumber(originalSkin.getColor(11, 15)) == 666 && getSkinPixelColourToNumber(originalSkin.getColor(12, 15)) == 666);
                hasVillagerNose = noseLower || noseUpper;
                if (noseUpper) {
                    deletePixels(modifiedSkin, 43, 13, 44, 15);
                }

                //check for coat bottom
                //pink to copy coat    light blue to remove from legs
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                NativeImage coatSkin = null;
                int controllerCoat = choiceBoxChoices[1];
                if (controllerCoat >= 1 && controllerCoat <= 8) {
                    int lengthOfCoat = choiceBoxChoices[2] - 1;
                    coatIdentifier = new Identifier(SKIN_NAMESPACE, id + "_coat.png");
                    coatSkin = getCoatTexture(originalSkin, lengthOfCoat, controllerCoat >= 5);
                    ETFUtils2.registerNativeImageToIdentifier(coatSkin, coatIdentifier);
                    //UUID_PLAYER_HAS_COAT.put(id, true);
                    if (controllerCoat == 2 || controllerCoat == 4 || controllerCoat == 6 || controllerCoat == 8) {
                        //delete original pixel from skin
                        deletePixels(modifiedSkin, 4, 32, 7, 35);
                        deletePixels(modifiedSkin, 4, 48, 7, 51);
                        deletePixels(modifiedSkin, 0, 36, 15, 36 + lengthOfCoat);
                        deletePixels(modifiedSkin, 0, 52, 15, 52 + lengthOfCoat);
                    }
                    //red or green make fat coat
                    hasFatCoat = controllerCoat == 3 || controllerCoat == 4 || controllerCoat == 7 || controllerCoat == 8;
                } else {
                    coatIdentifier = null;
                }
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                if (ETFConfigData.skinFeaturesEnableTransparency) {
                    if (isSkinNotTooTransparent(originalSkin)) {
                        allowThisETFBaseSkin = true;
                    } else {
                        ETFUtils2.logMessage("Skin was too transparent or had other problems", false);
                        allowThisETFBaseSkin = false;
                    }
                }
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                //create and register blink textures this will allow the ETFTexture to build these automatically
                NativeImage blinkSkinFile = null;
                NativeImage blinkSkinFile2 = null;
                Identifier blinkIdentifier = new Identifier(SKIN_NAMESPACE, id + "_blink.png");
                Identifier blink2Identifier = new Identifier(SKIN_NAMESPACE, id + "_blink2.png");

                int blinkChoice = choiceBoxChoices[0];
                if (blinkChoice >= 1 && blinkChoice <= 5) {

                    //check if lazy blink
                    if (blinkChoice <= 2) {
                        //blink 1 frame if either pink or blue optional
                        blinkSkinFile = returnOptimizedBlinkFace(originalSkin, getSkinPixelBounds("face1"), 1, getSkinPixelBounds("face3"));
                        ETFUtils2.registerNativeImageToIdentifier(blinkSkinFile, blinkIdentifier);

                        //blink is 2 frames with blue optional
                        if (blinkChoice == 2) {
                            blinkSkinFile2 = returnOptimizedBlinkFace(originalSkin, getSkinPixelBounds("face2"), 1, getSkinPixelBounds("face4"));
                            ETFUtils2.registerNativeImageToIdentifier(blinkSkinFile2, blink2Identifier);
                        }
                    } else {//optimized blink
                        int eyeHeightTopDown = choiceBoxChoices[3];
                        if (eyeHeightTopDown > 8 || eyeHeightTopDown < 1) {
                            eyeHeightTopDown = 1;
                        }
                        //optimized 1p high eyes
                        if (blinkChoice == 3) {
                            blinkSkinFile = returnOptimizedBlinkFace(originalSkin, getSkinPixelBounds("optimizedEyeSmall"), eyeHeightTopDown);

                            ETFUtils2.registerNativeImageToIdentifier(blinkSkinFile, blinkIdentifier);

                        } else if (blinkChoice == 4) {
                            blinkSkinFile = returnOptimizedBlinkFace(originalSkin, getSkinPixelBounds("optimizedEye2High"), eyeHeightTopDown);
                            blinkSkinFile2 = returnOptimizedBlinkFace(originalSkin, getSkinPixelBounds("optimizedEye2High_second"), eyeHeightTopDown);


                            ETFUtils2.registerNativeImageToIdentifier(blinkSkinFile, blinkIdentifier);
                            ETFUtils2.registerNativeImageToIdentifier(blinkSkinFile2, blink2Identifier);
                        } else /*if( blinkChoice == 5)*/ {
                            blinkSkinFile = returnOptimizedBlinkFace(originalSkin, getSkinPixelBounds("optimizedEye4High"), eyeHeightTopDown);
                            blinkSkinFile2 = returnOptimizedBlinkFace(originalSkin, getSkinPixelBounds("optimizedEye4High_second"), eyeHeightTopDown);
                            ETFUtils2.registerNativeImageToIdentifier(blinkSkinFile, blinkIdentifier);
                            ETFUtils2.registerNativeImageToIdentifier(blinkSkinFile2, blink2Identifier);
                        }
                    }
                }
                if (blinkSkinFile == null) blinkIdentifier = null;
                if (blinkSkinFile2 == null) blink2Identifier = null;

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

                //check for cape recolor
                int capeChoice1 = choiceBoxChoices[4];
                // custom cape data experiment
                // https://drive.google.com/uc?export=download&id=1rn1swLadqdMiLirz9Nrae0_VHFrTaJQe
                //downloadImageFromUrl(player, "https://drive.google.com/uc?export=download&id=1rn1swLadqdMiLirz9Nrae0_VHFrTaJQe", "etf$CAPE",null,true);
                if ((capeChoice1 >= 1 && capeChoice1 <= 3) || capeChoice1 == 666) {
                    switch (capeChoice1) {
                        case 1 -> //custom in skin
                                modifiedCape = returnCustomTexturedCape(originalSkin);
                        case 2 -> {
                            modifiedCape = null;
                            // minecraft capes mod
                            //https://minecraftcapes.net/profile/fd22e573178c415a94fee476b328abfd/cape/
                            initiateThirdPartyCapeDownload("https://minecraftcapes.net/profile/" + player.getUuidAsString().replace("-", "") + "/cape/");

                        }
                        case 3 -> {
                            modifiedCape = null;
                            //  https://optifine.net/capes/Benjamin.png
                            initiateThirdPartyCapeDownload("https://optifine.net/capes/" + player.getName().getString() + ".png");

                        }
                        case 666 ->
                                modifiedCape = ETFUtils2.getNativeImageElseNull(new Identifier("etf:textures/capes/error.png"));
                        default -> {
                            // cape = getNativeImageFromID(new Identifier("etf:capes/blank.png"));
                        }
                    }
                }
                if (modifiedCape != null) {
                    if ((capeChoice1 >= 1 && capeChoice1 <= 3) || capeChoice1 == 666) {//custom chosen
                        etfCapeIdentifier = new Identifier(SKIN_NAMESPACE, id + "_cape.png");
                        ETFUtils2.registerNativeImageToIdentifier(modifiedCape, etfCapeIdentifier);
                        //UUID_PLAYER_HAS_CUSTOM_CAPE.put(id, true);
                    }
                }
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                //check for marker choices
                //  1 = Emissives,  2 = Enchanted
                List<Integer> markerChoices = List.of(getSkinPixelColourToNumber(originalSkin.getColor(1, 17)),
                        getSkinPixelColourToNumber(originalSkin.getColor(1, 18)),
                        getSkinPixelColourToNumber(originalSkin.getColor(2, 17)),
                        getSkinPixelColourToNumber(originalSkin.getColor(2, 18)));
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                //emissives
                Identifier emissiveIdentifier = null;
                Identifier blinkEmissiveIdentifier = null;//new Identifier( SKIN_NAMESPACE , id + "_blink_e.png");
                Identifier blink2EmissiveIdentifier = null;//new Identifier( SKIN_NAMESPACE , id + "_blink2_e.png");
                hasEmissives = markerChoices.contains(1);
                if (hasEmissives) {
                    int[] boxChosenBounds = getSkinPixelBounds("marker" + (markerChoices.indexOf(1) + 1));
                    emissiveCapeBounds = boxChosenBounds;
                    NativeImage check = returnMatchPixels(originalSkin, boxChosenBounds);
                    if (check != null) {
                        emissiveIdentifier = new Identifier(SKIN_NAMESPACE, id + "_e.png");
                        ETFUtils2.registerNativeImageToIdentifier(check, emissiveIdentifier);
                        if (blinkSkinFile != null) {
                            NativeImage checkBlink = returnMatchPixels(blinkSkinFile, boxChosenBounds);
                            if (checkBlink != null) {
                                blinkEmissiveIdentifier = new Identifier(SKIN_NAMESPACE, id + "_blink_e.png");
                                ETFUtils2.registerNativeImageToIdentifier(checkBlink, blinkEmissiveIdentifier);
                            }
                            //registerNativeImageToIdentifier(Objects.requireNonNullElseGet(checkBlink, ETFUtils2::emptyNativeImage), SKIN_NAMESPACE + id + "_blink_e.png");
                        }
                        if (blinkSkinFile2 != null) {
                            NativeImage checkBlink = returnMatchPixels(blinkSkinFile2, boxChosenBounds);
                            if (checkBlink != null) {
                                blink2EmissiveIdentifier = new Identifier(SKIN_NAMESPACE, id + "_blink2_e.png");
                                ETFUtils2.registerNativeImageToIdentifier(checkBlink, blink2EmissiveIdentifier);
                            }
                            //registerNativeImageToIdentifier(Objects.requireNonNullElseGet(checkBlink, ETFUtils2::emptyNativeImage), SKIN_NAMESPACE + id + "_blink2_e.png");
                        }
                        if (coatSkin != null) {
                            NativeImage checkCoat = returnMatchPixels(originalSkin, boxChosenBounds, coatSkin);

                            //UUID_PLAYER_HAS_EMISSIVE_COAT.put(id, checkCoat != null);
                            if (checkCoat != null) {
                                coatEmissiveIdentifier = new Identifier(SKIN_NAMESPACE, id + "_coat_e.png");
                                ETFUtils2.registerNativeImageToIdentifier(checkCoat, coatEmissiveIdentifier);
                            }
                        }
                        if (modifiedCape != null) {

                            NativeImage checkCape = returnMatchPixels(originalSkin, boxChosenBounds, modifiedCape);
                            //UUID_PLAYER_HAS_EMISSIVE_CAPE.put(id, checkCape != null);
                            if (checkCape != null) {
                                etfCapeEmissiveIdentifier = new Identifier(SKIN_NAMESPACE, id + "_cape_e.png");
                                ETFUtils2.registerNativeImageToIdentifier(checkCape, coatEmissiveIdentifier);
                            }
                        }
                    } else {
                        hasEmissives = false;
                    }
                }
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                //enchant
                hasEnchant = markerChoices.contains(2);
                if (hasEnchant) {
                    int[] boxChosenBounds = getSkinPixelBounds("marker" + (markerChoices.indexOf(2) + 1));
                    enchantCapeBounds = boxChosenBounds;
                    NativeImage check = returnMatchPixels(originalSkin, boxChosenBounds);
                    if (check != null) {
                        baseEnchantIdentifier = new Identifier(SKIN_NAMESPACE, id + "_enchant.png");
                        ETFUtils2.registerNativeImageToIdentifier(check, baseEnchantIdentifier);
                        if (blinkSkinFile != null) {
                            NativeImage checkBlink = returnMatchPixels(blinkSkinFile, boxChosenBounds);
                            if (checkBlink != null) {
                                baseEnchantBlinkIdentifier = new Identifier(SKIN_NAMESPACE, id + "_blink_enchant.png");
                                ETFUtils2.registerNativeImageToIdentifier(checkBlink, baseEnchantBlinkIdentifier);
                            }
                            //registerNativeImageToIdentifier(Objects.requireNonNullElseGet(checkBlink, ETFUtils2::emptyNativeImage), SKIN_NAMESPACE + id + "_blink_e.png");
                        }
                        if (blinkSkinFile2 != null) {
                            NativeImage checkBlink = returnMatchPixels(blinkSkinFile2, boxChosenBounds);
                            if (checkBlink != null) {
                                baseEnchantBlink2Identifier = new Identifier(SKIN_NAMESPACE, id + "_blink2_enchant.png");
                                ETFUtils2.registerNativeImageToIdentifier(checkBlink, baseEnchantBlink2Identifier);
                            }
                            //registerNativeImageToIdentifier(Objects.requireNonNullElseGet(checkBlink, ETFUtils2::emptyNativeImage), SKIN_NAMESPACE + id + "_blink2_e.png");
                        }
                        if (coatSkin != null) {
                            NativeImage checkCoat = returnMatchPixels(originalSkin, boxChosenBounds, coatSkin);

                            //UUID_PLAYER_HAS_EMISSIVE_COAT.put(id, checkCoat != null);
                            if (checkCoat != null) {
                                coatEnchantedIdentifier = new Identifier(SKIN_NAMESPACE, id + "_coat_enchant.png");
                                ETFUtils2.registerNativeImageToIdentifier(checkCoat, coatEmissiveIdentifier);
                            }
                        }
                        if (modifiedCape != null) {

                            NativeImage checkCape = returnMatchPixels(originalSkin, boxChosenBounds, modifiedCape);
                            //UUID_PLAYER_HAS_EMISSIVE_CAPE.put(id, checkCape != null);
                            if (checkCape != null) {

                                etfCapeEnchantedIdentifier = new Identifier(SKIN_NAMESPACE, id + "_cape_enchant.png");
                                ETFUtils2.registerNativeImageToIdentifier(checkCape, coatEmissiveIdentifier);
                            }
                        }
                    } else {
                        hasEmissives = false;
                    }
                }

                Identifier modifiedSkinIdentifier = new Identifier(SKIN_NAMESPACE, id + ".png");
                ETFUtils2.registerNativeImageToIdentifier(modifiedSkin, modifiedSkinIdentifier);
                //create etf texture with player initiator
                etfTextureOfFinalBaseSkin = new ETFTexture(modifiedSkinIdentifier, blinkIdentifier, blink2Identifier, emissiveIdentifier, blinkEmissiveIdentifier, blink2EmissiveIdentifier);
            } else {
                skinFailed(true);
            }
        } else {
            skinFailed(true);
        }
        isTextureReady = true;
    }
}
