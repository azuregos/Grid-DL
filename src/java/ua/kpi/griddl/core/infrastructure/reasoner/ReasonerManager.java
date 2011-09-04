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
package ua.kpi.griddl.core.infrastructure.reasoner;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import ua.kpi.griddl.core.infrastructure.cache.CacheManager;
import ua.kpi.griddl.ws.WSUserQuery;

/**
 *
 * @author Pospishniy Olexandr <pospishniy@kpi.in.ua>
 */
public class ReasonerManager {

    private LinkedHashMap<Long, QueryTask> taskMap;
    private ExecutorService executor;
    private OWLReasonerFactory reasonerFactory;

    private ReasonerManager() {
        taskMap = new LinkedHashMap<Long, QueryTask>(25);
    }

    private static class SingletonHolder {

        public static final ReasonerManager INSTANCE = new ReasonerManager();
    }

    public static ReasonerManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    public void setReasonerFactory(OWLReasonerFactory reasonerFactory) {
        this.reasonerFactory = reasonerFactory;
    }

    public OWLReasonerFactory getReasonerFactory() {
        return reasonerFactory;
    }

    public Long submitQuery(WSUserQuery userQuery) {
        QueryTask queryTask = CacheManager.getInstance().get(userQuery);
        if (queryTask != null) {
            return queryTask.getId();
        } else {
            Long id = System.currentTimeMillis();
            queryTask = new QueryTask(id, userQuery.getQuery(), userQuery.getOntologies());
            taskMap.put(id, queryTask);
            executor.submit(queryTask);
            queryTask.setSubmitTime(new Date());
            queryTask.setStatus(QueryTask.TaskStatus.QUEUED);
            return id;
        }
    }

    public QueryTask getQueryTask(Long id) {
        return taskMap.get(id);
    }

    public void deleteTask(Long id) {
        taskMap.remove(id);
    }

    public Collection<QueryTask> listQueryTasks() {
        return taskMap.values();
    }
}
