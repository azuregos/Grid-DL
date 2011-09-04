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
package ua.kpi.griddl.core.infrastructure.tbox;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import ua.kpi.griddl.core.settings.SettingsManager;

/**
 *
 * @author Pospishniy Olexandr <pospishniy@kpi.in.ua>
 */
public class TBoxManager {

    private static TBoxManager instance;

    private TBoxManager() {
    }

    private static class SingletonHolder {
        public static final TBoxManager INSTANCE = new TBoxManager();
    }

    public static TBoxManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public String getTBoxVersionStr() {
        return SettingsManager.getInstance().getProperty(SettingsManager.TBOX_VERSION);
    }

    public String getTBoxStr() {
        File tbox = new File(SettingsManager.getInstance().getProperty(SettingsManager.TBOX_FILE));
        if (!tbox.exists()) {
            Logger.getLogger(TBoxManager.class.getName()).log(Level.SEVERE, null, "No TBox file found at " + tbox.getAbsolutePath());
            return null;
        } else {
            try {
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(tbox));
                byte[] buffer = new byte[8192];
                int bytesRead = 0;
                StringBuilder fileContent = new StringBuilder();
                while ((bytesRead = bis.read(buffer)) != -1) {
                    fileContent.append(new String(buffer, 0, bytesRead));
                }
                return fileContent.toString();
            } catch (IOException ex) {
                Logger.getLogger(TBoxManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }
}
