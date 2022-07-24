package traben.entity_texture_features.texture_handlers;

import it.unimi.dsi.fastutil.objects.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import traben.entity_texture_features.ETFClientCommon;
import traben.entity_texture_features.ETFVersionDifferenceHandler;
import traben.entity_texture_features.config.ETFConfig;
import traben.entity_texture_features.utils.ETFCacheKey;
import traben.entity_texture_features.utils.ETFLruCache;
import traben.entity_texture_features.utils.ETFUtils2;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static traben.entity_texture_features.ETFClientCommon.ETFConfigData;

// ETF re-write
//this class will ideally be where everything in vanilla interacts to get ETF stuff done
public abstract class ETFManager {

    /*
     * Storage reasoning
     *
     * for every storage map using an entity that cannot be stored in a fast-util primitive type
     * will utilise a cache that can clear contents after reaching certain sizes to prevent exceeding memory
     *
     * for every storage map keyed by a vanilla or confirmed existing texture they will remain as non clearing maps as they have an intrinsic upper size limit
     *
     *the rewrite relies heavily on minimizing processing time during play by
     *  setting up the textures once and then passing already calculated objects when required to speed up render time.
     * a big part of this is minimizing texture lookups and storing that info in fastUtil maps
     *
     */
    public static final ObjectOpenHashSet<String> EMISSIVE_SUFFIX_LIST = new ObjectOpenHashSet<>();
    public static final UUID ETF_GENERIC_UUID = UUID.nameUUIDFromBytes(("GENERIC").getBytes());
    //trident entities do not send item name data to clients when thrown, this is to keep that name in memory so custom tridents can at least display until reloading
    public static final Object2ReferenceOpenHashMap<UUID, String> UUID_TRIDENT_NAME = new Object2ReferenceOpenHashMap<>();
    public static final ETFLruCache<ETFCacheKey, ETFTexture> ENTITY_TEXTURE_MAP = new ETFLruCache<>();
    public static final ETFLruCache<UUID, ETFPlayerTexture> PLAYER_TEXTURE_MAP = new ETFLruCache<>();
    static final Object2LongOpenHashMap<UUID> ENTITY_BLINK_TIME = new Object2LongOpenHashMap<>();
    //if false variant 1 will need to use vanilla texture otherwise vanilla texture has an override in other directory
    private static final Object2BooleanOpenHashMap<Identifier> OPTIFINE_1_HAS_REPLACEMENT = new Object2BooleanOpenHashMap<>();
    static final ETFLruCache<ETFCacheKey, ObjectImmutableList<String>> ENTITY_SPAWN_CONDITIONS_CACHE = new ETFLruCache<>();
    //private static final Object2ReferenceOpenHashMap<@NotNull UUID, @NotNull ETFTexture> ENTITY_TEXTURE_MAP = new Object2ReferenceOpenHashMap<>();

    public static final Object2ObjectOpenHashMap<UUID, ETFCacheKey> UUID_TO_MOB_CACHE_KEY_MAP_FOR_FEATURE_USAGE = new Object2ObjectOpenHashMap<>();
    //this is a cache of all known ETFTexture versions of any existing resource-pack texture, used to prevent remaking objects
    private static final Object2ReferenceOpenHashMap<@NotNull Identifier, @Nullable ETFTexture> ETF_TEXTURE_CACHE = new Object2ReferenceOpenHashMap<>();
    //null means it is true random as in no properties
    private static final Object2ReferenceOpenHashMap<Identifier, @Nullable List<ETFTexturePropertyCase>> OPTIFINE_PROPERTY_CACHE = new Object2ReferenceOpenHashMap<>();
    private static final Object2BooleanOpenHashMap<UUID> ENTITY_IS_UPDATABLE = new Object2BooleanOpenHashMap<>();
    private static final ObjectOpenHashSet<UUID> ENTITY_UPDATE_QUEUE = new ObjectOpenHashSet<>();
    private static final ObjectOpenHashSet<UUID> ENTITY_DEBUG_QUEUE = new ObjectOpenHashSet<>();
    //contains the total number of variants for any given vanilla texture
    private static final Object2IntOpenHashMap<Identifier> TRUE_RANDOM_COUNT_CACHE = new Object2IntOpenHashMap<>();
    private static final ETFTexture ETF_ERROR_TEXTURE = getErrorETFTexture();
    private static final Object2LongOpenHashMap<UUID> LAST_PLAYER_CHECK_TIME = new Object2LongOpenHashMap<>();
    private static final Object2IntOpenHashMap<UUID> PLAYER_CHECK_COUNT = new Object2IntOpenHashMap<>();
    public static int mooshroomBrownCustomShroom = 0;
    //marks whether mooshroom mushroom overrides exist
    public static int mooshroomRedCustomShroom = 0;
    public static Boolean lecternHasCustomTexture = null;
    public final static Object2ObjectOpenHashMap<Identifier, ETFTexture> TEXTURE_MAP_TO_OPPOSITE_ELYTRA = new Object2ObjectOpenHashMap<>();
    public static ETFTexture redMooshroomAlt = null;
    public static ETFTexture brownMooshroomAlt = null;

    private static ETFTexture getErrorETFTexture() {
        ETFUtils2.registerNativeImageToIdentifier(ETFUtils2.emptyNativeImage(), new Identifier("etf:error.png"));
        ETFUtils2.logWarn("getErrorETFTexture() was called, investigate this if called too much");
        return new ETFTexture(new Identifier("etf:error.png"));//, ETFTexture.TextureSource.GENERIC_DEBUG);
    }

    public static void reset() {

        ETFClientCommon.etf$loadConfig();
        OPTIFINE_1_HAS_REPLACEMENT.clear();
        TEXTURE_MAP_TO_OPPOSITE_ELYTRA.clear();
        ETF_TEXTURE_CACHE.clear();
        ENTITY_TEXTURE_MAP.clearCache();
        //ENTITY_FEATURE_MAP.clear();
        ENTITY_SPAWN_CONDITIONS_CACHE.clearCache();
        OPTIFINE_PROPERTY_CACHE.clear();
        ENTITY_IS_UPDATABLE.clear();
        ENTITY_UPDATE_QUEUE.clear();
        ENTITY_DEBUG_QUEUE.clear();
        TRUE_RANDOM_COUNT_CACHE.clear();
        ENTITY_BLINK_TIME.clear();

        UUID_TO_MOB_CACHE_KEY_MAP_FOR_FEATURE_USAGE.clear();

        redMooshroomAlt = null;
        brownMooshroomAlt = null;
        mooshroomRedCustomShroom = 0;
        mooshroomBrownCustomShroom = 0;


        PLAYER_TEXTURE_MAP.clearCache();
        LAST_PLAYER_CHECK_TIME.clear();
        PLAYER_CHECK_COUNT.clear();

        ETFDirectory.clear();
        //reset emissive suffix
        EMISSIVE_SUFFIX_LIST.clear();
        try {
            List<Properties> props = new ArrayList<>();
            String[] paths = {"optifine/emissive.properties", "textures/emissive.properties", "etf/emissive.properties"};
            for (String path :
                    paths) {
                Properties prop = ETFUtils2.readAndReturnPropertiesElseNull(new Identifier(path));
                if (prop != null)
                    props.add(prop);
            }
            for (Properties prop :
                    props) {
                //not an optifine property that I know of but this has come up in a few packs, so I am supporting it
                if (prop.containsKey("entities.suffix.emissive")) {
                    if (prop.getProperty("entities.suffix.emissive") != null)
                        EMISSIVE_SUFFIX_LIST.add(prop.getProperty("entities.suffix.emissive"));
                }
                if (prop.containsKey("suffix.emissive")) {
                    if (prop.getProperty("suffix.emissive") != null)
                        EMISSIVE_SUFFIX_LIST.add(prop.getProperty("suffix.emissive"));
                }
            }
            if (ETFConfigData.alwaysCheckVanillaEmissiveSuffix) {
                EMISSIVE_SUFFIX_LIST.add("_e");
            }

            if (EMISSIVE_SUFFIX_LIST.size() == 0) {
                ETFUtils2.logMessage("no emissive suffixes found: default emissive suffix '_e' used");
                EMISSIVE_SUFFIX_LIST.add("_e");
            } else {
                ETFUtils2.logMessage("emissive suffixes loaded: " + EMISSIVE_SUFFIX_LIST);
            }
        } catch (Exception e) {
            ETFUtils2.logError("emissive suffixes could not be read: default emissive suffix '_e' used");
            EMISSIVE_SUFFIX_LIST.add("_e");
        }
    }

    public static void removeThisEntityDataFromAllStorage(ETFCacheKey ETFId) {
        ENTITY_TEXTURE_MAP.removeEntryOnly(ETFId);
        //ENTITY_FEATURE_MAP.clear();
        ENTITY_SPAWN_CONDITIONS_CACHE.removeEntryOnly(ETFId);


        UUID uuid = ETFId.getMobUUID();
        ENTITY_IS_UPDATABLE.removeBoolean(uuid);
        ENTITY_UPDATE_QUEUE.remove(uuid);
        ENTITY_DEBUG_QUEUE.remove(uuid);
        ENTITY_BLINK_TIME.removeLong(uuid);
        UUID_TO_MOB_CACHE_KEY_MAP_FOR_FEATURE_USAGE.remove(uuid);
    }

    public static void checkIfShouldTriggerUpdate(UUID id) {
        //type safe check as returns false if missing

        if (ENTITY_IS_UPDATABLE.getBoolean(id)
                && ETFConfigData.enableCustomTextures
                && ETFConfigData.textureUpdateFrequency_V2 != ETFConfig.UpdateFrequency.Never) {
            if (ENTITY_UPDATE_QUEUE.size() > 2000)
                ENTITY_UPDATE_QUEUE.clear();
            int delay = ETFConfigData.textureUpdateFrequency_V2.getDelay();
            long randomizer = delay * 20L;
            if (System.currentTimeMillis() % randomizer == Math.abs(id.hashCode()) % randomizer
            ) {
                //marks texture to update next render if a certain delay time is reached
                ENTITY_UPDATE_QUEUE.add(id);
            }
        }
    }

    public static void markEntityForDebugPrint(UUID id) {
        if (ETFConfigData.debugLoggingMode != ETFConfig.DebugLogMode.None) {
            ENTITY_DEBUG_QUEUE.add(id);
        }
    }

    @NotNull
    public static ETFTexture getETFDefaultTexture(Identifier vanillaIdentifier) {

        return getOrCreateETFTexture(vanillaIdentifier, vanillaIdentifier);
    }

    @NotNull
    public static <T extends Entity> ETFTexture getETFTexture(@NotNull Identifier vanillaIdentifier, @Nullable T entity, @NotNull TextureSource source) {
        if (entity == null) {
            //this should only purposefully call for features like armor or elytra that append to players and have no ETF customizing
            return getETFDefaultTexture(vanillaIdentifier);
        }
        UUID id = entity.getUuid();
        //use custom cache id this differentiates feature renderer calls here and makes the base feature still identifiable by uuid only when features are called
        ETFCacheKey cacheKey = new ETFCacheKey(id,vanillaIdentifier); //source == TextureSource.ENTITY_FEATURE ? vanillaIdentifier : null);
        if(source == TextureSource.ENTITY){
            //this is so feature renderers can find the 'base texture' of the mob to test it's variant if required
            UUID_TO_MOB_CACHE_KEY_MAP_FOR_FEATURE_USAGE.put(id,cacheKey);
        }

        //fastest in subsequent runs
        if (id == ETF_GENERIC_UUID || entity.getBlockPos().equals(Vec3i.ZERO)) {
            return getETFDefaultTexture(vanillaIdentifier);
        }
        if (ENTITY_TEXTURE_MAP.containsKey(cacheKey)) {
            ETFTexture quickReturn = ENTITY_TEXTURE_MAP.get(cacheKey);
            if (quickReturn == null) {
                ETFTexture vanillaETF = getETFDefaultTexture(vanillaIdentifier);
                ENTITY_TEXTURE_MAP.put(cacheKey, vanillaETF);
                quickReturn = vanillaETF;

            }
            if (source == TextureSource.ENTITY) {
                if (ENTITY_DEBUG_QUEUE.contains(id)) {
                    boolean inChat = ETFConfigData.debugLoggingMode == ETFConfig.DebugLogMode.Chat;
                    ETFUtils2.logMessage(quickReturn.toString(), inChat);
                    ETFUtils2.logMessage("entity cache size = " + ENTITY_TEXTURE_MAP.size() +
                            "\ntexture cache size = " + ETF_TEXTURE_CACHE.size() +
                            "\noriginal spawn state = " + ENTITY_SPAWN_CONDITIONS_CACHE.get(cacheKey) +
                            "\noptifine property key count = " + (OPTIFINE_PROPERTY_CACHE.containsKey(vanillaIdentifier) ? Objects.requireNonNullElse(OPTIFINE_PROPERTY_CACHE.get(vanillaIdentifier), new ArrayList<>()).size() : 0) +
                            "\ntrue random count = " + TRUE_RANDOM_COUNT_CACHE.getInt(vanillaIdentifier), inChat);

                    ENTITY_DEBUG_QUEUE.remove(id);
                }
                if (ENTITY_UPDATE_QUEUE.contains(id)) {
                    Identifier newVariantIdentifier = returnNewAlreadyConfirmedOptifineTexture(entity, vanillaIdentifier, true);
                    ENTITY_TEXTURE_MAP.put(cacheKey, Objects.requireNonNullElse(getOrCreateETFTexture(vanillaIdentifier, Objects.requireNonNullElse(newVariantIdentifier, vanillaIdentifier)), getETFDefaultTexture(vanillaIdentifier)));

                    ENTITY_UPDATE_QUEUE.remove(id);
                } else {
                    checkIfShouldTriggerUpdate(id);
                }
            }
            //this is where 99.99% of calls here will end only the very first call to this method by an entity goes further
            //the first call by any entity of a type will go the furthest and be the slowest as it triggers the initial setup, this makes all future calls by the same entity type faster
            //this is as close as possible to method start I can move this without losing update and debug functionality
            //this is the focal point of the rewrite where all the optimization is expected
            return quickReturn;
        }
        //need to create or find an ETFTexture object for entity and find or add to cache and entity map
        //firstly just going to check if this mob is some sort of gui element or not a real mod


        Identifier possibleIdentifier;
        if (source == TextureSource.ENTITY_FEATURE) {
            possibleIdentifier = getPossibleVariantIdentifierRedirectForFeatures(entity, vanillaIdentifier, source);
        } else {
            possibleIdentifier = getPossibleVariantIdentifier(entity, vanillaIdentifier, source);
        }

        ETFTexture foundTexture;
        foundTexture = Objects.requireNonNullElse(getOrCreateETFTexture(vanillaIdentifier, possibleIdentifier == null ? vanillaIdentifier : possibleIdentifier), getETFDefaultTexture(vanillaIdentifier));
        ENTITY_TEXTURE_MAP.put(cacheKey, foundTexture);
        return foundTexture;


    }

    @Nullable //when vanilla
    private static <T extends Entity> Identifier getPossibleVariantIdentifierRedirectForFeatures(T entity, Identifier vanillaIdentifier, TextureSource source) {
        Identifier regularReturnIdentifier = getPossibleVariantIdentifier(entity, vanillaIdentifier, source);
        if (regularReturnIdentifier == null || vanillaIdentifier.equals(regularReturnIdentifier)) {
            //random assignment either failed or returned texture1
            //as this is a feature we will also try one last time to match it to a possible variant of the base texture

            ETFCacheKey baseCacheId = UUID_TO_MOB_CACHE_KEY_MAP_FOR_FEATURE_USAGE.get(entity.getUuid()); //new ETFCacheKey(entity.getUuid(), null);

                if (baseCacheId != null && ENTITY_TEXTURE_MAP.containsKey(baseCacheId)) {
                    ETFTexture baseETFTexture = ENTITY_TEXTURE_MAP.get(baseCacheId);
                    if (baseETFTexture != null) {
                        return baseETFTexture.getFeatureTexture(vanillaIdentifier);
                    }
                }

        } else {
            return regularReturnIdentifier;
        }
        return null;
    }

    @Nullable //when vanilla
    private static <T extends Entity> Identifier getPossibleVariantIdentifier(T entity, Identifier vanillaIdentifier, TextureSource source) {

        if (ETFConfigData.enableCustomTextures) {
            //has this been checked before?
            if (TRUE_RANDOM_COUNT_CACHE.containsKey(vanillaIdentifier) || OPTIFINE_PROPERTY_CACHE.containsKey(vanillaIdentifier)) {
                //has optifine checked before?
                if (OPTIFINE_PROPERTY_CACHE.containsKey(vanillaIdentifier)) {
                    List<ETFTexturePropertyCase> optifineProperties = OPTIFINE_PROPERTY_CACHE.get(vanillaIdentifier);
                    if (optifineProperties != null) {
                        return returnNewAlreadyConfirmedOptifineTexture(entity, vanillaIdentifier, false, optifineProperties);
                    }
                }
                //has true random checked before?
                if (TRUE_RANDOM_COUNT_CACHE.containsKey(vanillaIdentifier) && source != TextureSource.ENTITY_FEATURE) {
                    int randomCount = TRUE_RANDOM_COUNT_CACHE.getInt(vanillaIdentifier);
                    if (randomCount != TRUE_RANDOM_COUNT_CACHE.defaultReturnValue()) {
                        return returnNewAlreadyConfirmedTrueRandomTexture(entity, vanillaIdentifier, randomCount);
                    }
                }
                //if we got here the texture is NOT random after having already checked before so return null
                return null;
            }


            //this is a new texture, we need to find what kind of random it is

            //if not null the below two represent the highest version of said files
            Identifier possibleProperty = ETFDirectory.getDirectoryVersionOf(ETFUtils2.replaceIdentifier(vanillaIdentifier, ".png", ".properties"));
            Identifier possible2PNG = ETFDirectory.getDirectoryVersionOf(ETFUtils2.replaceIdentifier(vanillaIdentifier, ".png", "2.png"));


            //if both null vanilla fallback as no randoms
            if (possible2PNG == null && possibleProperty == null) {
                //this will tell next call with this texture that these have been checked already
                OPTIFINE_PROPERTY_CACHE.put(vanillaIdentifier, null);
                return null;
            } else if (/*only*/possibleProperty == null) {
                if (source != TextureSource.ENTITY_FEATURE) {
                    newTrueRandomTextureFound(vanillaIdentifier, possible2PNG);
                    return returnNewAlreadyConfirmedTrueRandomTexture(entity, vanillaIdentifier);
                }
            } else if (/*only*/possible2PNG == null) {
                //optifine random confirmed
                newOptifineTextureFound(vanillaIdentifier, possibleProperty);
                return returnNewAlreadyConfirmedOptifineTexture(entity, vanillaIdentifier, false);
            } else {//neither null this will be annoying
                //if 2.png is higher it MUST be treated as true random confirmed
                ResourceManager resources = MinecraftClient.getInstance().getResourceManager();
                String p2pngPackName = resources.getResource(possible2PNG).isPresent() ? resources.getResource(possible2PNG).get().getResourcePackName() : null;
                String propertiesPackName = resources.getResource(possibleProperty).isPresent() ? resources.getResource(possibleProperty).get().getResourcePackName() : null;
                ObjectOpenHashSet<String> packs = new ObjectOpenHashSet<>();
                //if (p2pngPackName != null)
                packs.add(p2pngPackName);
                //if (propertiesPackName != null)
                packs.add(propertiesPackName);
                // System.out.println("debug6534="+p2pngPackName+","+propertiesPackName+","+ETFUtils2.returnNameOfHighestPackFrom(packs));
                if (propertiesPackName != null && propertiesPackName.equals(ETFUtils2.returnNameOfHighestPackFrom(packs))) {
                    newOptifineTextureFound(vanillaIdentifier, possibleProperty);
                    return returnNewAlreadyConfirmedOptifineTexture(entity, vanillaIdentifier, false);
                } else {
                    if (source != TextureSource.ENTITY_FEATURE) {
                        newTrueRandomTextureFound(vanillaIdentifier, possible2PNG);
                        return returnNewAlreadyConfirmedTrueRandomTexture(entity, vanillaIdentifier);
                    }
                }
            }
        }
        //marker to signify code has run before and is not random or true random
        OPTIFINE_PROPERTY_CACHE.put(vanillaIdentifier, null);
        //use vanilla as fallback
        return null;
    }

    private static void newOptifineTextureFound(Identifier vanillaIdentifier, Identifier properties) {

        try {
            Properties props = ETFUtils2.readAndReturnPropertiesElseNull(properties);

            if (props != null) {
                Set<String> propIds = props.stringPropertyNames();
                //set so only 1 of each
                Set<Integer> numbers = new HashSet<>();

                //get the numbers we are working with
                for (String str :
                        propIds) {
                    str = str.replaceAll("\\D", "");
                    if(!str.isEmpty()) {
                        try {
                            numbers.add(Integer.parseInt(str));
                        } catch (NumberFormatException e) {
                            ETFUtils2.logWarn("properties file number error in start count");
                        }
                    }
                }
                //sort from lowest to largest
                List<Integer> numbersList = new ArrayList<>(numbers);
                Collections.sort(numbersList);
                List<ETFTexturePropertyCase> allCasesForTexture = new ArrayList<>();
                for (Integer num :
                        numbersList) {
                    //System.out.println("constructed as "+num);
                    //loops through each known number in properties
                    //all case.1 ect should be processed here
                    Integer[] suffixes = {};
                    Integer[] weights = {};
                    String[] biomes = {};
                    Integer[] heights = {};
                    ArrayList<String> names = new ArrayList<>();
                    String[] professions = {};
                    String[] collarColours = {};
                    int baby = 0; // 0 1 2 - don't true false
                    int weather = 0; //0,1,2,3 - no clear rain thunder
                    String[] health = {};
                    Integer[] moon = {};
                    String[] daytime = {};
                    String[] blocks = {};
                    String[] teams = {};
                    Integer[] sizes = {};

                    if (props.containsKey("skins." + num) || props.containsKey("textures." + num)) {
                        String dataFromProps = props.containsKey("skins." + num) ? props.getProperty("skins." + num).strip() : props.getProperty("textures." + num).strip();
                        String[] skinData = dataFromProps.split("\s+");
                        ArrayList<Integer> suffixNumbers = new ArrayList<>();
                        for (String data :
                                skinData) {
                            //check if range
                            data = data.strip();
                            if (!data.replaceAll("\\D", "").isEmpty()) {
                                if (data.contains("-")) {
                                    suffixNumbers.addAll(Arrays.asList(ETFUtils2.getIntRange(data)));
                                } else {
                                    try {
                                        int tryNumber = Integer.parseInt(data.replaceAll("\\D", ""));
                                        suffixNumbers.add(tryNumber);
                                    }catch (NumberFormatException e){
                                        ETFUtils2.logWarn("properties files number error in skins / textures category");
                                    }
                                }
                            }
                        }
                        suffixes = suffixNumbers.toArray(new Integer[0]);
                    }
                    if (props.containsKey("weights." + num)) {
                        String dataFromProps = props.getProperty("weights." + num).trim();
                        String[] weightData = dataFromProps.split("\s+");
                        ArrayList<Integer> builder = new ArrayList<>();
                        for (String s :
                                weightData) {
                            s = s.trim();
                            if (!s.replaceAll("\\D", "").isEmpty()) {
                                try {
                                    int tryNumber = Integer.parseInt(s.replaceAll("\\D", ""));
                                    builder.add(tryNumber);
                                }catch (NumberFormatException e){
                                    ETFUtils2.logWarn("properties files number error in weights category");
                                }
                            }
                        }
                        weights = builder.toArray(new Integer[0]);
                    }
                    if (props.containsKey("biomes." + num)) {
                        String dataFromProps = props.getProperty("biomes." + num).strip();
                        String[] biomeList = dataFromProps.toLowerCase().split("\s+");

                        //strip out old format optifine biome names
                        //I could be way more in-depth and make these line up to all variants but this is legacy code
                        //only here for compat, pack makers need to fix these
                        if(biomeList.length > 0) {
                            for (int i = 0; i < biomeList.length; i++) {
                                String biome = biomeList[i].strip();
                                switch (biome) {
                                    case "Ocean" -> biomeList[i] = "ocean";
                                    case "Plains" -> biomeList[i] = "plains";
                                    case "ExtremeHills" -> biomeList[i] = "stony_peaks";
                                    case "Forest", "ForestHills" -> biomeList[i] = "forest";
                                    case "Taiga", "TaigaHills" -> biomeList[i] = "taiga";
                                    case "Swampland" -> biomeList[i] = "swamp";
                                    case "River" -> biomeList[i] = "river";
                                    case "Hell" -> biomeList[i] = "nether_wastes";
                                    case "Sky" -> biomeList[i] = "the_end";
                                    case "FrozenOcean" -> biomeList[i] = "frozen_ocean";
                                    case "FrozenRiver" -> biomeList[i] = "frozen_river";
                                    case "IcePlains" -> biomeList[i] = "snowy_plains";
                                    case "IceMountains" -> biomeList[i] = "snowy_slopes";
                                    case "MushroomIsland", "MushroomIslandShore" -> biomeList[i] = "mushroom_fields";
                                    case "Beach" -> biomeList[i] = "beach";
                                    case "DesertHills", "Desert" -> biomeList[i] = "desert";
                                    case "ExtremeHillsEdge" -> biomeList[i] = "meadow";
                                    case "Jungle", "JungleHills" -> biomeList[i] = "jungle";
                                }
                            }
                            biomes = biomeList;
                        }

                    }
                    //add legacy height support
                    if (!props.containsKey("heights." + num) && (props.containsKey("minHeight." + num) || props.containsKey("maxHeight." + num))) {
                        String min = "-64";
                        String max = "319";
                        if (props.containsKey("minHeight." + num)) {
                            min = props.getProperty("minHeight." + num).strip();
                        }
                        if (props.containsKey("maxHeight." + num)) {
                            max = props.getProperty("maxHeight." + num).strip();
                        }
                        props.put("heights." + num, min + "-" + max);
                    }
                    if (props.containsKey("heights." + num)) {
                        String dataFromProps = props.getProperty("heights." + num).trim();
                        String[] heightData = dataFromProps.split("\s+");
                        ArrayList<Integer> heightNumbers = new ArrayList<>();
                        for (String data :
                                heightData) {
                            data = data.replaceAll("\\(", "").replaceAll("\\)", "");
                            //check if range
                            data = data.trim();
                            if (!data.replaceAll("\\D", "").isEmpty()) {
                                if (data.contains("-")) {
                                    heightNumbers.addAll(Arrays.asList(ETFUtils2.getIntRange(data)));
                                } else {
                                    try {
                                        int tryNumber = Integer.parseInt(data.replaceAll("\\D", ""));
                                        heightNumbers.add(tryNumber);
                                    }catch (NumberFormatException e){
                                        ETFUtils2.logWarn("properties files number error in height category");
                                    }
                                }
                            }
                        }
                        heights = heightNumbers.toArray(new Integer[0]);
                    }

                    if (props.containsKey("names." + num)) {
                        String dataFromProps = props.getProperty("names." + num).trim();
                        if (dataFromProps.contains("regex:") || dataFromProps.contains("pattern:")) {
                            names.add(dataFromProps);
                        } else {
                            //names = dataFromProps.split("\s+");
                            //allow    "multiple names" among "other"
                            //List<String> list = new ArrayList<>();
                            //add the full line as the first name option to allow for simple multiple names
                            //in case someone just writes   names.1=john smith
                            //instead of                   names.1="john smith"
                            names.add(dataFromProps);

                            Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(dataFromProps);
                            while (m.find()) {
                                names.add(m.group(1).replace("\"", "").trim());
                            }
                            //names.addAll(list);
                        }
                    }
                    if (props.containsKey("name." + num)) {
                        String dataFromProps = props.getProperty("name." + num).trim();
                        names.add(dataFromProps);
                    }
                    if (props.containsKey("professions." + num)) {
                        professions = props.getProperty("professions." + num).trim().split("\s+");
                    }
                    if (props.containsKey("collarColors." + num) || props.containsKey("colors." + num)) {
                        collarColours = props.containsKey("collarColors." + num) ? props.getProperty("collarColors." + num).trim().split("\s+") : props.getProperty("colors." + num).trim().split("\s+");
                    }
                    if (props.containsKey("baby." + num)) {
                        String dataFromProps = props.getProperty("baby." + num).trim();
                        switch (dataFromProps) {
                            case "true" -> baby = 1;
                            case "false" -> baby = 2;
                        }
                    }
                    if (props.containsKey("weather." + num)) {
                        String dataFromProps = props.getProperty("weather." + num).trim();
                        switch (dataFromProps) {
                            case "clear" -> weather = 1;
                            case "rain" -> weather = 2;
                            case "thunder" -> weather = 3;
                        }
                    }
                    if (props.containsKey("health." + num)) {
                        health = props.getProperty("health." + num).trim().split("\s+");
                    }
                    if (props.containsKey("moonPhase." + num)) {
                        String dataFromProps = props.getProperty("moonPhase." + num).trim();
                        String[] moonData = dataFromProps.split("\s+");
                        ArrayList<Integer> moonNumbers = new ArrayList<>();
                        for (String data :
                                moonData) {
                            //check if range
                            data = data.trim();
                            if (!data.replaceAll("\\D", "").isEmpty()) {
                                if (data.contains("-")) {
                                    moonNumbers.addAll(Arrays.asList(ETFUtils2.getIntRange(data)));
                                } else {
                                    try {
                                        int tryNumber = Integer.parseInt(data.replaceAll("\\D", ""));
                                        moonNumbers.add(tryNumber);
                                    }catch (NumberFormatException e){
                                        ETFUtils2.logWarn("properties files number error in moon phase category");
                                    }
                                }
                            }
                        }
                        moon = moonNumbers.toArray(new Integer[0]);
                    }
                    if (props.containsKey("dayTime." + num)) {
                        daytime = props.getProperty("dayTime." + num).trim().split("\s+");
                    }
                    if (props.containsKey("blocks." + num)) {
                        blocks = props.getProperty("blocks." + num).trim().split("\s+");
                    } else if (props.containsKey("block." + num)) {
                        blocks = props.getProperty("block." + num).trim().split("\s+");
                    }
                    if (props.containsKey("teams." + num)) {
                        String teamData = props.getProperty("teams." + num).trim();
                        List<String> list = new ArrayList<>();
                        Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(teamData);
                        while (m.find()) {
                            list.add(m.group(1).replace("\"", ""));
                        }
                        teams = list.toArray(new String[0]);
                    } else if (props.containsKey("team." + num)) {
                        String teamData = props.getProperty("team." + num).trim();
                        List<String> list = new ArrayList<>();
                        Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(teamData);
                        while (m.find()) {
                            list.add(m.group(1).replace("\"", ""));
                        }
                        teams = list.toArray(new String[0]);
                    }

                    if (props.containsKey("sizes." + num)) {
                        String dataFromProps = props.getProperty("sizes." + num).trim();
                        String[] sizeData = dataFromProps.split("\s+");
                        ArrayList<Integer> sizeNumbers = new ArrayList<>();
                        for (String data :
                                sizeData) {
                            //check if range
                            data = data.trim();
                            if (!data.replaceAll("\\D", "").isEmpty()) {
                                if (data.contains("-")) {
                                    sizeNumbers.addAll(Arrays.asList(ETFUtils2.getIntRange(data)));
                                } else {

                                    try {
                                        int tryNumber = Integer.parseInt(data.replaceAll("\\D", ""));
                                        sizeNumbers.add(tryNumber);
                                    }catch (NumberFormatException e){
                                        ETFUtils2.logWarn("properties files number error in sizes category");
                                    }

                                }
                            }
                        }
                        sizes = sizeNumbers.toArray(new Integer[0]);
                    }

                    //array faster to use
                    //list easier to build
                    String[] namesArray = names.toArray(new String[0]);

                    if (suffixes.length != 0) {
                        allCasesForTexture.add(new ETFTexturePropertyCase(suffixes, weights, biomes, heights, namesArray, professions, collarColours, baby, weather, health, moon, daytime, blocks, teams, /*num,*/ sizes));
                    }
                }
                if (!allCasesForTexture.isEmpty()) {
                    //it all worked now just get the first texture called and everything is set for the next time the texture is called for fast processing
                    OPTIFINE_PROPERTY_CACHE.put(vanillaIdentifier, allCasesForTexture);
                    //return returnAlreadyConfirmedOptifineTexture(entity,vanillaIdentifier,allCasesForTexture);
                } else {
                    ETFUtils2.logMessage("Ignoring properties file that failed to load any cases @ " + vanillaIdentifier, false);
                    OPTIFINE_PROPERTY_CACHE.put(vanillaIdentifier, null);
                }
            } else {//properties file is null
                ETFUtils2.logMessage("Ignoring properties file that was null @ " + vanillaIdentifier, false);
                OPTIFINE_PROPERTY_CACHE.put(vanillaIdentifier, null);
            }
        } catch (Exception e) {
            ETFUtils2.logWarn("Ignoring properties file that caused Exception @ " + vanillaIdentifier + e, false);
            OPTIFINE_PROPERTY_CACHE.put(vanillaIdentifier, null);
        }

        //return null if properties failed to read/load/work or pass high school english
        //return null;
    }

    private static void newTrueRandomTextureFound(Identifier vanillaIdentifier, Identifier variant2PNG) {
        //here 2.png is confirmed to exist and has its directory already applied
        //I'm going to ignore 1.png that will be hardcoded as vanilla or optifine replaced
        ResourceManager resources = MinecraftClient.getInstance().getResourceManager();
        int totalTextureCount = 2;
        while (resources.getResource(ETFUtils2.replaceIdentifier(variant2PNG, "[0-9]+(?=\\.png)", String.valueOf((totalTextureCount + 1)))).isPresent()) {
            totalTextureCount++;
        }
        //here totalTextureCount == the confirmed last value of the random order
        //System.out.println("total true random was="+totalTextureCount);
        TRUE_RANDOM_COUNT_CACHE.put(vanillaIdentifier, totalTextureCount);

        //make sure to return first check
        //return returnAlreadyConfirmedTrueRandomTexture(entity,vanillaIdentifier,totalTextureCount);
        //can't return null as 2.png confirmed exists
    }

    @Nullable
    private static <T extends Entity> Identifier returnNewAlreadyConfirmedOptifineTexture(T entity, Identifier vanillaIdentifier, boolean isThisAnUpdate) {
        return returnNewAlreadyConfirmedOptifineTexture(entity, vanillaIdentifier, isThisAnUpdate, OPTIFINE_PROPERTY_CACHE.get(vanillaIdentifier));
    }

    @Nullable
    private static <T extends Entity> Identifier returnNewAlreadyConfirmedOptifineTexture(T entity, Identifier vanillaIdentifier, boolean isThisAnUpdate, List<ETFTexturePropertyCase> optifineProperties) {

        int variantNumber = testAndGetVariantNumberFromOptiFineCases(entity, isThisAnUpdate, optifineProperties);

        Identifier variantIdentifier = returnNewAlreadyNumberedRandomTexture(vanillaIdentifier, variantNumber);
        if (variantIdentifier == null) {
            return null;
        }
        //must test these exist
        if (ETF_TEXTURE_CACHE.containsKey(variantIdentifier)) {
            if (ETF_TEXTURE_CACHE.get(variantIdentifier) == null) {
                return null;
            }
            //then we know it exists
            return variantIdentifier;
        }
        Optional<Resource> variantResource = MinecraftClient.getInstance().getResourceManager().getResource(variantIdentifier);
        if (variantResource.isPresent()) {
            return variantIdentifier;
            //it will be added to cache for future checks later
        } else {
            ETF_TEXTURE_CACHE.put(variantIdentifier, null);
        }
        //ETFUtils.logError("texture assign has failed, vanilla texture has been used as fallback");

        return null;
    }

    private static <T extends Entity> int testAndGetVariantNumberFromOptiFineCases(T entity, boolean isThisAnUpdate, List<ETFTexturePropertyCase> optifineProperties) {
        try {
            for (ETFTexturePropertyCase property :
                    optifineProperties) {
                if (property.doesEntityMeetConditionsOfThisCase((LivingEntity) entity, isThisAnUpdate, ENTITY_IS_UPDATABLE)) {
                    return property.getAnEntityVariantSuffixFromThisCase(entity.getUuid());
                }
            }
        } catch (Exception e) {
            return 1;
        }

        //ETFUtils.logError("optifine property checks found no match using vanilla");
        return 1;
    }

    @NotNull
    private static <T extends Entity> Identifier returnNewAlreadyConfirmedTrueRandomTexture(T entity, Identifier vanillaIdentifier) {
        return returnNewAlreadyConfirmedTrueRandomTexture(entity, vanillaIdentifier, TRUE_RANDOM_COUNT_CACHE.getInt(vanillaIdentifier));
    }

    @NotNull
    private static <T extends Entity> Identifier returnNewAlreadyConfirmedTrueRandomTexture(T entity, Identifier vanillaIdentifier, int totalCount) {
        int randomReliable = Math.abs(entity.getUuid().hashCode());
        randomReliable %= totalCount;
        randomReliable++;
        //no need to test as they have already all been confirmed existing by code
        Identifier toReturn = returnNewAlreadyNumberedRandomTexture(vanillaIdentifier, randomReliable);
        return toReturn == null ? vanillaIdentifier : toReturn;
    }

    @Nullable
    private static Identifier returnNewAlreadyNumberedRandomTexture(Identifier vanillaIdentifier, int variantNumber) {
        //1.png logic not required as expected optifine behaviour is already present

        return ETFDirectory.getDirectoryVersionOf(ETFUtils2.replaceIdentifier(vanillaIdentifier, ".png", variantNumber + ".png"));
    }



    @NotNull
    private static ETFTexture getOrCreateETFTexture(Identifier vanillaIdentifier, Identifier variantIdentifier) {
        if (ETF_TEXTURE_CACHE.containsKey(variantIdentifier)) {
            //use cached ETFTexture
            ETFTexture cached = ETF_TEXTURE_CACHE.get(variantIdentifier);
            if (cached != null) {
                return cached;
            } else {
                ETFUtils2.logWarn("getOrCreateETFTexture found a null, this probably should not be happening");
                //texture doesn't exist
                cached = ETF_TEXTURE_CACHE.get(vanillaIdentifier);
                if (cached != null) {
                    return cached;
                }
            }
        } else {
            //create new ETFTexture and cache it
            ETFTexture foundTexture = new ETFTexture(variantIdentifier);
            ETF_TEXTURE_CACHE.put(variantIdentifier, foundTexture);
            return foundTexture;
        }
        ETFUtils2.logError("getOrCreateETFTexture and should not have");
        return ETF_ERROR_TEXTURE;
    }

    public static EmissiveRenderModes getEmissiveMode() {
        if (ETFConfigData.fullBrightEmissives) {
            return EmissiveRenderModes.BRIGHT;
        } else {
            return EmissiveRenderModes.DULL;
        }
    }

    @Nullable
    public static ETFPlayerTexture getPlayerTexture(PlayerEntity player) {
        UUID id = player.getUuid();
        if (PLAYER_TEXTURE_MAP.containsKey(id)) {
            return PLAYER_TEXTURE_MAP.get(id);
        } else {
            if (LAST_PLAYER_CHECK_TIME.containsKey(id)) {
                int attemptCount = PLAYER_CHECK_COUNT.getInt(id);
                if (attemptCount > 6) {
                    //no more checking always return null now
                    //player ahs no features it seems
                    LAST_PLAYER_CHECK_TIME.removeLong(id);
                    PLAYER_CHECK_COUNT.removeInt(id);
                    PLAYER_TEXTURE_MAP.put(id, null);
                    return null;
                }

                if (LAST_PLAYER_CHECK_TIME.getLong(id) + 3000 > System.currentTimeMillis()) {
                    //not time to check again
                    return null;
                }
                PLAYER_CHECK_COUNT.put(id, attemptCount + 1);
                //allowed to continue if time has passed and not exceeded attempt limit
            }
            LAST_PLAYER_CHECK_TIME.put(id, System.currentTimeMillis());
            ETFPlayerTexture etfPlayerTexture = new ETFPlayerTexture(player);
            PLAYER_TEXTURE_MAP.put(id, etfPlayerTexture);
            return etfPlayerTexture;
        }

    }

    public enum TextureSource {
        ENTITY,
        BLOCK_ENTITY,
        ENTITY_FEATURE
    }
    public enum EmissiveRenderModes {
        DULL,
        BRIGHT;

        public static EmissiveRenderModes blockEntityMode() {
            //iris has fixes for bright mode which is otherwise broken on block entities, does not require enabled shaders
            if (ETFVersionDifferenceHandler.isThisModLoaded("iris") && ETFConfigData.fullBrightEmissives) {
                return BRIGHT;
            } else {
                //todo investigate if block entities require a third enum for custom render mode
                return DULL;
            }
        }
    }

}
