package traben.entity_texture_features.utils;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import org.jetbrains.annotations.Nullable;
import traben.entity_texture_features.texture_handlers.ETFManager;

import static traben.entity_texture_features.ETFClient.ETFConfigData;

public class ETFLruCache<X, Y> {

    //cache with lru functionality
    final Object2ObjectLinkedOpenHashMap<X, Y> cache;
    final int capacity = 2048;

    public ETFLruCache() {
        this.cache = new Object2ObjectLinkedOpenHashMap<>(capacity);
        //this.capacity = capacity - 1;
    }

    public boolean containsKey(X key) {
        return this.cache.containsKey(key);
//        if (cache.containsKey(key)) {
//            cache.putAndMoveToFirst(key, cache.get(key));
//            return true;
//        } else {
//            return false;
//        }
    }

    @Nullable
    public Y get(X key) {
        return cache.getAndMoveToFirst(key);

    }

    public void put(X key, Y value) {
        if (cache.size() >= capacity * (ETFConfigData.advanced_IncreaseCacheSizeModifier > 1 ? ETFConfigData.advanced_IncreaseCacheSizeModifier : 1)  ) {
            X lastKey = cache.lastKey();
            if(lastKey instanceof ETFCacheKey ETFKey) {
                ETFManager.removeThisEntityDataFromAllStorage(ETFKey);
            }else{
                cache.remove(lastKey);
            }
        }
        cache.putAndMoveToFirst(key, value);
    }

    public void clearCache() {
        cache.clear();
    }


    public int size() {
        return cache.size();
    }

    public void removeEntryOnly(X key){
        cache.remove(key);
    }

    @Override
    public String toString() {
        return cache.toString();
    }
}
