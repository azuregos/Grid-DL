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
package ua.kpi.griddl.core.settings;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Pospishniy Olexandr <pospishniy@kpi.in.ua>
 */
public class SettingsManager {

    public static final String REPO_SERVER = "repository.server";
    public static final String REPO_USER = "repository.user";
    public static final String REPO_PATH = "repository.path";
    public static final String REASONER_CLASS = "reasoner.factory.class";
    public static final String REASONER_METHOD = "reasoner.factory.method";
    public static final String TBOX_FILE = "tbox.file";
    public static final String TBOX_VERSION = "tbox.version";
    public static final String TBOX_URI = "tbox.uri";
    public static final String GRID_LDAP_SERVER = "grid.ldap.server";
    public static final String GRID_LDAP_PORT = "grid.ldap.port";
    public static final String IMPORT_SITE = "import.site";
    public static final String IMPORT_SERVICE = "import.service";
    public static final String IMPORT_CE = "import.ce";
    public static final String IMPORT_VOVIEW = "import.voview";
    public static final String IMPORT_CLUSTER = "import.cluster";
    public static final String IMPORT_SUBCLUSTER = "import.subcluster";
    public static final String IMPORT_SE = "import.se";
    public static final String IMPORT_PERIOD = "import.period";
    public static final String WORKER_THREADS = "reasoner.worker.threads";
    private static SettingsManager instance;
    private Properties properties;

    private SettingsManager() {
        properties = new Properties();
        try {
            properties.load(SettingsManager.class.getClassLoader().getResourceAsStream("griddl.properties"));
        } catch (IOException ex) {
            Logger.getLogger(SettingsManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static class SingletonHolder {
        public static final SettingsManager INSTANCE = new SettingsManager();
    }

    public static SettingsManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public String getProperty(String name) {
        return properties.getProperty(name);
    }

    public boolean hasProperty(String name) {
        return properties.containsKey(name);
    }

    public boolean getBooleanProperty(String name) {
        return Boolean.parseBoolean(properties.getProperty(name));
    }
}
