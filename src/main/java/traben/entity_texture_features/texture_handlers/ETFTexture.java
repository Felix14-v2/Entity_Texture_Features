package traben.entity_texture_features.texture_handlers;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import traben.entity_texture_features.utils.ETFUtils2;
import traben.entity_texture_features.utils.IrisCompat;

import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static traben.entity_texture_features.ETFClient.ETFConfigData;
import static traben.entity_texture_features.texture_handlers.ETFManager.EMISSIVE_SUFFIX_LIST;
import static traben.entity_texture_features.texture_handlers.ETFManager.ENTITY_BLINK_TIME;


//can either refer to a vanilla identifier or a variant
public class ETFTexture {


    //the vanilla texture this is associated with
    //might be itself
    //private final Identifier vanillaIdentifier;

    private final static String PATCH_NAMESPACE_PREFIX = "etf_patched_";
    //this variants id , might be vanilla
    public final Identifier thisIdentifier;
    private final Object2ReferenceOpenHashMap<Identifier, Identifier> FEATURE_TEXTURE_MAP = new Object2ReferenceOpenHashMap<>();
    private final int variantNumber;
    public TextureReturnState currentTextureState = TextureReturnState.NORMAL;
    //a variation of thisIdentifier but with emissive texture pixels removed for z-fighting solution
    private Identifier thisIdentifier_Patched = null;
    //the emissive version of this texture
    private Identifier emissiveIdentifier = null;
    private Identifier emissiveBlinkIdentifier = null;
    private Identifier emissiveBlink2Identifier = null;
    private Identifier blinkIdentifier = null;
    private Identifier blink2Identifier = null;
    private Identifier blinkIdentifier_Patched = null;
    private Identifier blink2Identifier_Patched = null;
    private Integer blinkLength = ETFConfigData.blinkLength;
    private Integer blinkFrequency = ETFConfigData.blinkFrequency;

    // private final TextureSource source;

    public ETFTexture(/*@NotNull Identifier vanillaIdentifier,*/ @NotNull Identifier variantIdentifier) {//,TextureSource source) {
        //this.vanillaIdentifier = vanillaIdentifier;
        //this.source = source;
        this.thisIdentifier = variantIdentifier;
        Pattern pattern = Pattern.compile("\\d+(?=\\.png)");
        Matcher matcher = pattern.matcher(variantIdentifier.getPath());
        if (matcher.find()) {
            this.variantNumber = Integer.parseInt(matcher.group());
        } else {
            this.variantNumber = 0;
        }
        setupBlinking();
        setupEmissives();

    }

    //alternative initiator for already known textures used for players
    public ETFTexture(@NotNull Identifier modifiedSkinIdentifier,
                      @Nullable Identifier blinkIdentifier,
                      @Nullable Identifier blink2Identifier,
                      @Nullable Identifier emissiveIdentifier,
                      @Nullable Identifier blinkEmissiveIdentifier,
                      @Nullable Identifier blink2EmissiveIdentifier) {

        //ALL input already tested and confirmed existing
        this.variantNumber = 0;
        this.thisIdentifier = modifiedSkinIdentifier;
        this.blinkIdentifier = blinkIdentifier;
        this.blink2Identifier = blink2Identifier;
        this.emissiveIdentifier = emissiveIdentifier;
        this.emissiveBlinkIdentifier = blinkEmissiveIdentifier;
        this.emissiveBlink2Identifier = blink2EmissiveIdentifier;
        //setupBlinking(); neither required
        //setupEmissives();
        createPatchedTextures();
    }

    //alternative initiator for already known textures used for MooShroom's mushrooms
    public ETFTexture(@NotNull Identifier modifiedSkinIdentifier,
                      @Nullable Identifier emissiveIdentifier) {

        //ALL input already tested and confirmed existing
        this.variantNumber = 0;
        this.thisIdentifier = modifiedSkinIdentifier;
        this.emissiveIdentifier = emissiveIdentifier;
        //setupBlinking(); neither required
        //setupEmissives();
        createPatchedTextures();
    }


    private void setupBlinking() {
        if (ETFConfigData.enableBlinking) {
            ResourceManager resourceManager = MinecraftClient.getInstance().getResourceManager();
            Optional<Resource> vanillaR1 = resourceManager.getResource(thisIdentifier);
            if (vanillaR1.isPresent()) {
                Identifier possibleBlinkIdentifier = ETFUtils2.replaceIdentifier(thisIdentifier, ".png", "_blink.png");
                Optional<Resource> blinkR1 = resourceManager.getResource(possibleBlinkIdentifier);
                if (blinkR1.isPresent()) {

                    String blink1PackName = blinkR1.get().getResourcePackName();
                    ObjectSet<String> packs = new ObjectOpenHashSet<>();
                    packs.add(blink1PackName);
                    packs.add(vanillaR1.get().getResourcePackName());
                    if (blink1PackName.equals(ETFUtils2.returnNameOfHighestPackFrom(packs))) {
                        //is higher or same pack
                        blinkIdentifier = possibleBlinkIdentifier;


                        Identifier possibleBlink2Identifier = ETFUtils2.replaceIdentifier(thisIdentifier, ".png", "_blink2.png");
                        Optional<Resource> blinkR2 = resourceManager.getResource(possibleBlink2Identifier);
                        if (blinkR2.isPresent()) {
                            String blink2PackName = blinkR2.get().getResourcePackName();
                            if (blink1PackName.equals(blink2PackName)) {
                                blink2Identifier = possibleBlink2Identifier;
                            }
                        }

                        //read possible blinking properties
                        Identifier propertyIdentifier = ETFUtils2.replaceIdentifier(possibleBlinkIdentifier, ".png", ".properties");
                        Properties blinkingProps = ETFUtils2.readAndReturnPropertiesElseNull(propertyIdentifier);
                        if (blinkingProps != null) {
                            Optional<Resource> propertyResource = resourceManager.getResource(propertyIdentifier);
                            if (propertyResource.isPresent()) {
                                String propertyResourcePackName = propertyResource.get().getResourcePackName();
                                packs.clear();
                                packs.add(propertyResourcePackName);
                                packs.add(blink1PackName);

                                if (propertyResourcePackName.equals(ETFUtils2.returnNameOfHighestPackFrom(packs))) {
                                    blinkLength = blinkingProps.containsKey("blinkLength") ?
                                            Integer.parseInt(blinkingProps.getProperty("blinkLength").replaceAll("\\D", "")) :
                                            ETFConfigData.blinkLength;
                                    blinkFrequency = blinkingProps.containsKey("blinkFrequency") ?
                                            Integer.parseInt(blinkingProps.getProperty("blinkFrequency").replaceAll("\\D", "")) :
                                            ETFConfigData.blinkFrequency;

                                }

                            }
                        }
                    }
                }
            }
        }
    }

    private void setupEmissives() {

        if (ETFConfigData.enableEmissiveTextures) {
            ResourceManager resourceManager = MinecraftClient.getInstance().getResourceManager();

            for (String possibleEmissiveSuffix :
                    EMISSIVE_SUFFIX_LIST) {
                Optional<Resource> vanillaR1 = resourceManager.getResource(thisIdentifier);
                if (vanillaR1.isPresent()) {
                    Identifier possibleEmissiveIdentifier = ETFUtils2.replaceIdentifier(thisIdentifier, ".png", possibleEmissiveSuffix + ".png");
                    Optional<Resource> emissiveR1 = resourceManager.getResource(possibleEmissiveIdentifier);
                    if (emissiveR1.isPresent()) {

                        String emissivePackName = emissiveR1.get().getResourcePackName();
                        ObjectSet<String> packs = new ObjectOpenHashSet<>();
                        packs.add(emissivePackName);
                        packs.add(vanillaR1.get().getResourcePackName());
                        if (emissivePackName.equals(ETFUtils2.returnNameOfHighestPackFrom(packs))) {
                            //is higher or same pack
                            emissiveIdentifier = possibleEmissiveIdentifier;
                            Identifier possibleEmissiveBlinkIdentifier = ETFUtils2.replaceIdentifier(thisIdentifier, ".png", "_blink" + possibleEmissiveSuffix + ".png");
                            Optional<Resource> emissiveBlinkR1 = resourceManager.getResource(possibleEmissiveBlinkIdentifier);
                            if (emissiveBlinkR1.isPresent()) {

                                String emissiveBlinkPackName = emissiveBlinkR1.get().getResourcePackName();
                                packs.clear();
                                packs.add(emissiveBlinkPackName);
                                packs.add(vanillaR1.get().getResourcePackName());
                                if (emissiveBlinkPackName.equals(ETFUtils2.returnNameOfHighestPackFrom(packs))) {
                                    //is higher or same pack
                                    emissiveBlinkIdentifier = possibleEmissiveBlinkIdentifier;
                                    Identifier possibleEmissiveBlink2Identifier = ETFUtils2.replaceIdentifier(thisIdentifier, ".png", "_blink2" + possibleEmissiveSuffix + ".png");
                                    Optional<Resource> emissiveBlink2R1 = resourceManager.getResource(possibleEmissiveBlink2Identifier);
                                    if (emissiveBlink2R1.isPresent()) {
                                        String emissiveBlink2PackName = emissiveBlink2R1.get().getResourcePackName();
                                        packs.clear();
                                        packs.add(emissiveBlink2PackName);
                                        packs.add(vanillaR1.get().getResourcePackName());
                                        if (emissiveBlink2PackName.equals(ETFUtils2.returnNameOfHighestPackFrom(packs))) {
                                            //is higher or same pack
                                            emissiveBlink2Identifier = possibleEmissiveBlink2Identifier;
                                        }
                                    }
                                }
                            }
                            //emissive found and is valid
                            break;
                        }
                    }
                }
            }
            if (isEmissive())
                createPatchedTextures();
        }
    }

    private void createPatchedTextures() {
        if (this.emissiveIdentifier != null) {
            //create patched texture
            NativeImage emissiveImage = ETFUtils2.getNativeImageElseNull(emissiveIdentifier);

            try {
                NativeImage originalCopyToPatch = returnPatchedVersionOrNull(ETFUtils2.getNativeImageElseNull(thisIdentifier), emissiveImage);
                //no errors here means it all , and we have a patched texture in originalCopyToPatch
                thisIdentifier_Patched = new Identifier(PATCH_NAMESPACE_PREFIX + thisIdentifier.getNamespace(), thisIdentifier.getPath());
                ETFUtils2.registerNativeImageToIdentifier(originalCopyToPatch, thisIdentifier_Patched);

                if (doesBlink() && emissiveBlinkIdentifier != null) {
                    NativeImage emissiveBlinkImage = ETFUtils2.getNativeImageElseNull(emissiveBlinkIdentifier);
                    NativeImage blinkCopyToPatch = returnPatchedVersionOrNull(ETFUtils2.getNativeImageElseNull(blinkIdentifier), emissiveBlinkImage);
                    //no errors here means it all worked, and we have a patched texture in
                    blinkIdentifier_Patched = new Identifier(PATCH_NAMESPACE_PREFIX + blinkIdentifier.getNamespace(), blinkIdentifier.getPath());
                    ETFUtils2.registerNativeImageToIdentifier(blinkCopyToPatch, blinkIdentifier_Patched);

                    if (doesBlink2() && emissiveBlink2Identifier != null) {
                        NativeImage emissiveBlink2Image = ETFUtils2.getNativeImageElseNull(emissiveBlink2Identifier);
                        NativeImage blink2CopyToPatch = returnPatchedVersionOrNull(ETFUtils2.getNativeImageElseNull(blink2Identifier), emissiveBlink2Image);
                        //no errors here means it all worked, and we have a patched texture in
                        blink2Identifier_Patched = new Identifier(PATCH_NAMESPACE_PREFIX + blink2Identifier.getNamespace(), blink2Identifier.getPath());
                        ETFUtils2.registerNativeImageToIdentifier(blink2CopyToPatch, blinkIdentifier_Patched);
                    }
                }
            } catch (Exception ignored) {
                //assert this just in case of crash in unexpected step after being set
                thisIdentifier_Patched = null;
                blinkIdentifier_Patched = null;
                blink2Identifier_Patched = null;
            }

        }

    }

    private @NotNull NativeImage returnPatchedVersionOrNull(NativeImage baseImage, NativeImage emissiveImage) throws IndexOutOfBoundsException {
        try {
            if (emissiveImage.getWidth() == baseImage.getWidth() && emissiveImage.getHeight() == baseImage.getHeight()) {
                //float widthMultipleEmissive  = originalCopy.getWidth()  / (float)emissive.getWidth();
                //float heightMultipleEmissive = originalCopy.getHeight() / (float)emissive.getHeight();

                for (int x = 0; x < baseImage.getWidth(); x++) {
                    for (int y = 0; y < baseImage.getHeight(); y++) {
                        //int newX = Math.min((int)(x*widthMultipleEmissive),originalCopy.getWidth()-1);
                        //int newY = Math.min((int)(y*heightMultipleEmissive),originalCopy.getHeight()-1);
                        if (emissiveImage.getOpacity(x, y) != 0) {
                            baseImage.setColor(x, y, 0);
                        }
                    }
                }
            }
            return baseImage;
        } catch (Exception e) {
            throw new IndexOutOfBoundsException("emissive texture is not correct size");
        }
    }

    @NotNull
    Identifier getFeatureTexture(Identifier vanillaFeatureTexture) {

        if (FEATURE_TEXTURE_MAP.containsKey(vanillaFeatureTexture)) {
            return FEATURE_TEXTURE_MAP.get(vanillaFeatureTexture);
        }
        //otherwise we need to find what it is and add to map
        ETFDirectory directory = ETFDirectory.getDirectoryOf(thisIdentifier);
        if (variantNumber != 0) {
            Identifier possibleFeatureVariantIdentifier =
                    ETFDirectory.getIdentifierAsDirectory(
                            ETFUtils2.replaceIdentifier(
                                    vanillaFeatureTexture,
                                    ".png",
                                    variantNumber + ".png")
                            , directory);
            Optional<Resource> possibleResource = MinecraftClient.getInstance().getResourceManager().getResource(possibleFeatureVariantIdentifier);
            if (possibleResource.isPresent()) {
                //feature variant exists so return
                FEATURE_TEXTURE_MAP.put(vanillaFeatureTexture, possibleFeatureVariantIdentifier);
                return possibleFeatureVariantIdentifier;
            }
        }
        //here we have no number and are likely vanilla texture or something went wrong in which case vanilla anyway
        //ETFUtils2.logWarn("getFeatureTexture() either vanilla or failed");
        ETFDirectory tryDirectory = ETFDirectory.getDirectoryOf(vanillaFeatureTexture);
        if (tryDirectory == directory || tryDirectory == ETFDirectory.VANILLA) {
            //if same directory as main texture or is vanilla texture use it
            Identifier tryDirectoryVariant = ETFDirectory.getIdentifierAsDirectory(vanillaFeatureTexture, tryDirectory);
            FEATURE_TEXTURE_MAP.put(vanillaFeatureTexture, tryDirectoryVariant);
            return tryDirectoryVariant;
        }
        //final fallback just use vanilla
        FEATURE_TEXTURE_MAP.put(vanillaFeatureTexture, vanillaFeatureTexture);
        return vanillaFeatureTexture;

    }

    @NotNull
    public Identifier getTextureIdentifier(LivingEntity entity) {
        return getTextureIdentifier(entity, false);
    }

    @NotNull
    public Identifier getTextureIdentifier(@Nullable LivingEntity entity, boolean forcePatchedTexture) {

        if (isPatched() && (forcePatchedTexture || (ETFConfigData.enableEmissiveTextures && IrisCompat.isShaderPackInUse()))) {
            //patched required
            currentTextureState = TextureReturnState.NORMAL_PATCHED;
            return getBlinkingIdentifier(entity);
        }
        currentTextureState = TextureReturnState.NORMAL;
        //regular required
        return getBlinkingIdentifier(entity);
    }

    @NotNull
    private Identifier getBlinkingIdentifier(@Nullable LivingEntity entity) {
        if (!doesBlink() || entity == null || !ETFConfigData.enableBlinking) {
            return identifierOfCurrentState();
        }

        //force eyes closed if asleep
        if (entity.getPose() == EntityPose.SLEEPING) {
            modifyTextureState(TextureReturnState.APPLY_BLINK);
            return identifierOfCurrentState();
        }
        //force eyes closed if blinded
        else if (entity.hasStatusEffect(StatusEffects.BLINDNESS)) {
            modifyTextureState(doesBlink2() ? TextureReturnState.APPLY_BLINK2 : TextureReturnState.APPLY_BLINK);
            return identifierOfCurrentState();
        } else {
            //do regular blinking
            if (entity.world != null) {
                UUID id = entity.getUuid();
                if (!ENTITY_BLINK_TIME.containsKey(id)) {
                    ENTITY_BLINK_TIME.put(id, entity.world.getTime() + blinkLength + 1);
                    return identifierOfCurrentState();
                }
                long nextBlink = ENTITY_BLINK_TIME.getLong(id);
                long currentTime = entity.world.getTime();

                if (currentTime >= nextBlink - blinkLength && currentTime <= nextBlink + blinkLength) {
                    if (doesBlink2()) {
                        if (currentTime >= nextBlink - (blinkLength / 3) && currentTime <= nextBlink + (blinkLength / 3)) {
                            modifyTextureState(TextureReturnState.APPLY_BLINK);
                            return identifierOfCurrentState();
                        }
                        modifyTextureState(TextureReturnState.APPLY_BLINK2);
                        return identifierOfCurrentState();
                    } else if (!(currentTime > nextBlink)) {
                        modifyTextureState(TextureReturnState.APPLY_BLINK);
                        return identifierOfCurrentState();
                    }
                } else if (currentTime > nextBlink + blinkLength) {
                    //calculate new next blink
                    ENTITY_BLINK_TIME.put(id, currentTime + entity.getRandom().nextInt(blinkFrequency) + 20);
                }
            }
        }
        return identifierOfCurrentState();
    }

    public boolean isEmissive() {
        return this.emissiveIdentifier != null;
    }

    public boolean isPatched() {
        return this.thisIdentifier_Patched != null;
    }

    public boolean doesBlink() {
        return this.blinkIdentifier != null;
    }


    public boolean doesBlink2() {
        return this.blink2Identifier != null;
    }

    @Override
    public String toString() {
        return "ETFTexture{texture=" + this.thisIdentifier.toString() +/*", vanilla="+this.vanillaIdentifier.toString()+*/", emissive=" + isEmissive() + ", patched=" + isPatched() + "}";
    }

    public void renderEmissive(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, ModelPart modelPart) {
        renderEmissive(matrixStack, vertexConsumerProvider, modelPart, ETFManager.getEmissiveMode());
    }

    public void renderEmissive(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, ModelPart modelPart, ETFManager.EmissiveRenderModes modeToUsePossiblyManuallyChosen) {
        VertexConsumer vertexC = getEmissiveVertexConsumer(vertexConsumerProvider, null, modeToUsePossiblyManuallyChosen);
        if (vertexC != null) {
            modelPart.render(matrixStack, vertexC, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV);
        }
    }

    public void renderEmissive(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, Model model) {
        renderEmissive(matrixStack, vertexConsumerProvider, model, ETFManager.getEmissiveMode());
    }

    public void renderEmissive(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, Model model, ETFManager.EmissiveRenderModes modeToUsePossiblyManuallyChosen) {
        VertexConsumer vertexC = getEmissiveVertexConsumer(vertexConsumerProvider, model, modeToUsePossiblyManuallyChosen);
        if (vertexC != null) {
            model.render(matrixStack, vertexC, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 1, 1, 1, 1);
        }
    }

    @Nullable
    public VertexConsumer getEmissiveVertexConsumer(VertexConsumerProvider vertexConsumerProvider, @Nullable Model model, ETFManager.EmissiveRenderModes modeToUsePossiblyManuallyChosen) {
        if (isEmissive()) {
            // block entity variants
            //removed in rework may return
//                if (irisDetected && ETFConfigData.fullBrightEmissives) {
//                    return vertexConsumerProvider.getBuffer(RenderLayer.getBeaconBeam(PATH_EMISSIVE_TEXTURE_IDENTIFIER.get(fileString), true));
//                } else {
//                    return vertexConsumerProvider.getBuffer(RenderLayer.getItemEntityTranslucentCull(PATH_EMISSIVE_TEXTURE_IDENTIFIER.get(fileString)));
//                }
            Identifier emissiveToUse = getEmissiveIdentifierOfCurrentState();
            if (emissiveToUse != null) {
                if (modeToUsePossiblyManuallyChosen == ETFManager.EmissiveRenderModes.BRIGHT) {
                    return vertexConsumerProvider.getBuffer(RenderLayer.getBeaconBeam(emissiveToUse, !IrisCompat.isShaderPackInUse()));
                } else {
                    if (model == null) {
                        return vertexConsumerProvider.getBuffer(RenderLayer.getEntityCutoutNoCull /*RenderLayer.getEntityTranslucent*/(emissiveToUse));
                    } else {
                        return vertexConsumerProvider.getBuffer(model.getLayer(emissiveToUse));
                    }
                }
            }
        }
        //return null for any fail
        return null;
    }

    private void modifyTextureState(TextureReturnState givenState) {
        switch (givenState) {
            case APPLY_BLINK ->
                    currentTextureState = currentTextureState == TextureReturnState.NORMAL_PATCHED ? TextureReturnState.BLINK_PATCHED : TextureReturnState.BLINK;
            case APPLY_BLINK2 -> currentTextureState = switch (currentTextureState) {
                case NORMAL_PATCHED, BLINK_PATCHED -> TextureReturnState.BLINK2_PATCHED;
                default -> TextureReturnState.BLINK2;
            };
            //shouldn't ever call but may need in future
//            case APPLY_PATCH -> currentTextureState= switch (currentTextureState){
//                    case BLINK ->  TextureReturnState.BLINK_PATCHED;
//                    case BLINK2 -> TextureReturnState.BLINK2_PATCHED;
//                    default -> TextureReturnState.NORMAL_PATCHED;
//                };
            //default -> {}
        }
    }

    @NotNull
    private Identifier identifierOfCurrentState() {
        return switch (currentTextureState) {
            case NORMAL -> thisIdentifier;
            case NORMAL_PATCHED -> thisIdentifier_Patched;
            case BLINK -> blinkIdentifier;
            case BLINK_PATCHED -> blinkIdentifier_Patched;
            case BLINK2 -> blink2Identifier;
            case BLINK2_PATCHED -> blink2Identifier_Patched;
            default ->
                //ETFUtils.logError("identifierOfCurrentState failed, it should not have, returning default");
                    thisIdentifier;
        };
    }

    @Nullable
    public Identifier getEmissiveIdentifierOfCurrentState() {
        return switch (currentTextureState) {
            case NORMAL, NORMAL_PATCHED -> emissiveIdentifier;
            case BLINK, BLINK_PATCHED -> emissiveBlinkIdentifier;
            case BLINK2, BLINK2_PATCHED -> emissiveBlink2Identifier;
            default ->
                //ETFUtils.logError("identifierOfCurrentState failed, it should not have, returning default");
                    thisIdentifier;
        };
    }

    public enum TextureReturnState {
        NORMAL,
        NORMAL_PATCHED,
        BLINK,
        BLINK_PATCHED,
        BLINK2,
        BLINK2_PATCHED,
        APPLY_PATCH,
        APPLY_BLINK,
        APPLY_BLINK2;


        @Override
        public String toString() {
            return switch (this) {
                case NORMAL -> "normal";
                case BLINK -> "blink";
                case BLINK2 -> "blink2";
                case NORMAL_PATCHED -> "normal_patched";
                case BLINK_PATCHED -> "blink_patched";
                case BLINK2_PATCHED -> "blink2_patched";
                case APPLY_BLINK -> "apply_blink";
                case APPLY_BLINK2 -> "apply_blink2";
                case APPLY_PATCH -> "apply_patch";
            };
        }
    }

}
