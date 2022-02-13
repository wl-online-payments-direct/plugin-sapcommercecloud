package com.worldline.direct.cache;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.hybris.platform.core.Registry;
import de.hybris.platform.store.BaseStoreModel;
import de.hybris.platform.store.services.BaseStoreService;

import org.springframework.cache.interceptor.KeyGenerator;

public class WorldlineCacheKeyGenerator implements KeyGenerator {
    private static WorldlineCacheKeyGenerator worldlineCacheKeyGenerator;
    private static BaseStoreService baseStoreService;

    @Override
    public Object generate(Object target, Method method, Object... params) {
        return generate(false, params);
    }

    public Object generate(boolean addBaseStore, Object... params) {
        List<Object> key = new ArrayList();
        if (addBaseStore) {
            addBaseStore(key);
        }
        this.addParams(key, params);
        return key;
    }

    public static Object generateKey(boolean addBaseStore, Object... params) {
        return getWorldlineCacheKeyGenerator().generate(addBaseStore, params);
    }


    private void addBaseStore(List<Object> key) {
        final BaseStoreModel currentBaseStore = getBaseStoreService().getCurrentBaseStore();
        key.add(currentBaseStore.getUid());
    }

    protected void addParams(List<Object> key, Object... params) {
        key.addAll(Arrays.asList(params));
    }

    private static WorldlineCacheKeyGenerator getWorldlineCacheKeyGenerator() {
        if (worldlineCacheKeyGenerator == null) {
            worldlineCacheKeyGenerator = (WorldlineCacheKeyGenerator) Registry.getApplicationContext().getBean("worldlineCacheKeyGenerator");
        }
        return worldlineCacheKeyGenerator;
    }

    public static BaseStoreService getBaseStoreService() {
        if (baseStoreService == null) {
            baseStoreService = (BaseStoreService) Registry.getApplicationContext().getBean("baseStoreService");
        }
        return baseStoreService;
    }
}
