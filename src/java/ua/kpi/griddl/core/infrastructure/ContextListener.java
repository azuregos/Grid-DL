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

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import static ua.kpi.griddl.core.infrastructure.ServerStartupManager.ServerStatus;

/**
 *
 * @author Pospishniy Olexandr <pospishniy@kpi.in.ua>
 */
public class ContextListener implements ServletContextListener {

    private final Logger logger = Logger.getLogger(ContextListener.class.getName());
    private ServerStartupManager startupManager;

    public ContextListener() {
        startupManager = new ServerStartupManager();
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.log(Level.INFO, "<GRID-DL Initialization>");
        try {
            startupManager.setStatus(ServerStatus.INITIALIZING);
            startupManager.initializeRepository();
            startupManager.initializeGridDataImport();
            startupManager.initializeReasoner();
            startupManager.initializeWorkerThreads();
            startupManager.setStatus(ServerStatus.READY);
        } catch (StartupException ex) {
            logger.log(Level.SEVERE, "Startup initialization failed. Please check configuration", ex);
            startupManager.setStatus(ServerStatus.FAILED);
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "Unexpected error during initialization phase.", t);
            startupManager.setStatus(ServerStatus.FAILED);
        }
        logger.log(Level.INFO, "</GRID-DL Initialization>");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        logger.log(Level.INFO, "== GRID-DL Shutdown ==");
    }
}
