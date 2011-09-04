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
package ua.kpi.griddl.core.infrastructure;

import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import ua.kpi.griddl.core.infrastructure.abox.ABoxManager;
import ua.kpi.griddl.core.infrastructure.abox.ImportAction;
import ua.kpi.griddl.core.infrastructure.reasoner.ReasonerManager;
import ua.kpi.griddl.core.infrastructure.repository.RepositoryException;
import ua.kpi.griddl.core.infrastructure.repository.RepositoryManager;
import ua.kpi.griddl.core.infrastructure.repository.hg.HgRepositoryManager;
import ua.kpi.griddl.core.settings.SettingsManager;

/**
 *
 * @author Pospishniy Olexandr <pospishniy@kpi.in.ua>
 */
public class ServerStartupManager {

    private RepositoryManager repositoryManager;

    public static enum ServerStatus {

        NONE("None"), INITIALIZING("Initializing"), READY("Ready"), FAILED("Failed");
        
        private final String label;

        private ServerStatus(String param) {
            this.label = param;
        }

        @Override
        public String toString() {
            return label;
        }
    };
    
    private static ServerStatus status = ServerStatus.NONE;

    public ServerStartupManager() {
        repositoryManager = HgRepositoryManager.getInstance();
    }

    void initializeRepository() throws StartupException {
        try {
            repositoryManager.prepareRepository();
        } catch (RepositoryException ex) {
            Logger.getLogger(ServerStartupManager.class.getName()).log(Level.SEVERE, "Repository initialization Failed");
            throw new StartupException(ex);
        }
    }

    void initializeGridDataImport() throws StartupException {
        ImportAction importAction = new ImportAction();
        importAction.run();
        if (ABoxManager.getInstance().getActiveABox() == null) {
            throw new StartupException("Grid data import initialization Failed");
        }
        Timer timer = new Timer("Abox builder", true);
        long period = Long.parseLong(SettingsManager.getInstance().getProperty(SettingsManager.IMPORT_PERIOD));
        timer.schedule(importAction, period, period);
    }

    void initializeReasoner() throws StartupException {
        try {
            String reasonerClass = SettingsManager.getInstance().getProperty(SettingsManager.REASONER_CLASS);
            String reasonerMethod = SettingsManager.getInstance().getProperty(SettingsManager.REASONER_METHOD);
            Class<?> rf = this.getClass().getClassLoader().loadClass(reasonerClass);
            OWLReasonerFactory factory = (OWLReasonerFactory) rf.getMethod(reasonerMethod, null).invoke(rf, null);
            ReasonerManager.getInstance().setReasonerFactory(factory);
            Logger.getLogger(ServerStartupManager.class.getName()).log(Level.INFO, "Initialized reasoner factory");
        } catch (Exception ex) {
            Logger.getLogger(ServerStartupManager.class.getName()).log(Level.SEVERE, "Failed to load OWL Reasoner class");
            throw new StartupException(ex);
        }
    }

    void initializeWorkerThreads() throws StartupException {
        int threads = Integer.parseInt(SettingsManager.getInstance().getProperty(SettingsManager.WORKER_THREADS));
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        ReasonerManager.getInstance().setExecutor(executor);
        Logger.getLogger(ServerStartupManager.class.getName()).log(Level.INFO, "Initialized {0} worker threads", threads);
    }

    public static ServerStatus getStatus() {
        return status;
    }

    public void setStatus(ServerStatus status) {
        ServerStartupManager.status = status;
    }
}
