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
package ua.kpi.griddl.core.infrastructure.abox;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.TimerTask;
import ua.kpi.griddl.core.infrastructure.cache.CacheManager;
import ua.kpi.griddl.core.settings.SettingsManager;
import static ua.kpi.griddl.core.settings.SettingsManager.*;

/**
 *
 * @author Pospishniy Olexandr <pospishniy@kpi.in.ua>
 */
public class ImportAction extends TimerTask {

    private ABoxManager aBoxManager;
    private LDAPManager ldapManager;
    private ABoxBuilder boxBuilder;
    private final SettingsManager settingsManager = SettingsManager.getInstance();

    public ImportAction() {
        aBoxManager = ABoxManager.getInstance();
    }

    @Override
    public void run() {
        Logger.getLogger(ImportAction.class.getName()).log(Level.INFO, "ABox construction...");
        String ldapServer = settingsManager.getProperty(GRID_LDAP_SERVER);
        String ldapPort = settingsManager.getProperty(GRID_LDAP_PORT);
        String tboxFile = settingsManager.getProperty(TBOX_FILE);
        String tboxURI = settingsManager.getProperty(TBOX_URI);
        boxBuilder = new ABoxBuilder(tboxFile, tboxURI);
        try {
            ldapManager = new LDAPManager(ldapServer, ldapPort);
            boxBuilder.createNewEmptyOntology();
            if (settingsManager.getBooleanProperty(IMPORT_SITE)) {
                boxBuilder.saveSites(ldapManager.retriveSites());
            }
            if (settingsManager.getBooleanProperty(IMPORT_SERVICE)) {
                boxBuilder.saveServices(ldapManager.retriveServices());
            }
            if (settingsManager.getBooleanProperty(IMPORT_CE)) {
                boxBuilder.saveCEs(ldapManager.retriveCEs());
            }
            if (settingsManager.getBooleanProperty(IMPORT_VOVIEW)) {
                boxBuilder.saveVOViews(ldapManager.retriveVOViews());
            }
            if (settingsManager.getBooleanProperty(IMPORT_CLUSTER)) {
                boxBuilder.saveClusters(ldapManager.retriveClusters());
            }
            if (settingsManager.getBooleanProperty(IMPORT_SUBCLUSTER)) {
                boxBuilder.saveSubClusters(ldapManager.retriveSubClusters());
            }
            if (settingsManager.getBooleanProperty(IMPORT_SE)) {
                boxBuilder.saveSEs(ldapManager.retriveSEs());
            }
            aBoxManager.setActiveABox(boxBuilder.getOntology());
            CacheManager.getInstance().reset();
            Logger.getLogger(ImportAction.class.getName()).log(Level.INFO, "ABox constructed. {0} axioms.", boxBuilder.getOntology().getAxiomCount());
        } catch (Exception ex) {
            Logger.getLogger(ImportAction.class.getName()).log(Level.SEVERE, "ABox construction failed! Reasoning could not be done.", ex);
            aBoxManager.setActiveABox(null);
        } finally {
            ldapManager.close();
        }
    }
}
