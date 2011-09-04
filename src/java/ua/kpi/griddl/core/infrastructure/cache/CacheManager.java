/*
 * Copyright 2011 NTUU "KPI".
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ua.kpi.griddl.core.infrastructure.cache;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import ua.kpi.griddl.core.infrastructure.reasoner.QueryTask;
import ua.kpi.griddl.ws.WSUserQuery;

/**
 *
 * @author alexander
 */
public class CacheManager {

    private Map<String, SoftReference<QueryTask>> cacheMap = new HashMap<String, SoftReference<QueryTask>>();

    private static class SingletonHolder {

        public static final CacheManager INSTANCE = new CacheManager();
    }

    public static CacheManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public void put(QueryTask queryTask) {
        cacheMap.put(makeKey(queryTask), new SoftReference<QueryTask>(queryTask));
    }

    public QueryTask get(WSUserQuery userQuery) {
        SoftReference<QueryTask> ref = cacheMap.get(makeKey(userQuery));
        return ref != null ? ref.get() : null;
    }

    public void reset() {
        cacheMap = new HashMap<String, SoftReference<QueryTask>>();
    }

    private String makeKey(QueryTask queryTask) {
        StringBuilder key = new StringBuilder();
        key.append(queryTask.getQuery());
        if (queryTask.getOntologies() != null) {
            key.append("@");
            Iterator<String> iterator = queryTask.getOntologies().iterator();
            while (iterator.hasNext()) {
                key.append(iterator.next());
                if (iterator.hasNext()) {
                    key.append(",");
                }
            }
        }
        return key.toString();
    }

    private String makeKey(WSUserQuery userQuery) {
        StringBuilder key = new StringBuilder();
        key.append(userQuery.getQuery());
        if (userQuery.getOntologies() != null) {
            key.append("@");
            Iterator<String> iterator = userQuery.getOntologies().iterator();
            while (iterator.hasNext()) {
                key.append(iterator.next());
                if (iterator.hasNext()) {
                    key.append(",");
                }
            }
        }
        return key.toString();
    }
}
