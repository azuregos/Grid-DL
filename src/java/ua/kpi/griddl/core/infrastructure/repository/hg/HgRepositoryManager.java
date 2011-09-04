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
package ua.kpi.griddl.core.infrastructure.repository.hg;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ua.kpi.griddl.core.infrastructure.repository.RepositoryException;
import ua.kpi.griddl.core.infrastructure.repository.RepositoryManager;
import ua.kpi.griddl.core.settings.SettingsManager;
import static ua.kpi.griddl.core.infrastructure.repository.hg.HgExec.*;

/**
 *
 * @author Pospishniy Olexandr <pospishniy@kpi.in.ua>
 */
public class HgRepositoryManager implements RepositoryManager {

    private static HgRepositoryManager instance;
    private static final Logger logger = Logger.getLogger(HgRepositoryManager.class.getName());
    private File repoDirectory;

    private HgRepositoryManager() {
        repoDirectory = new File(SettingsManager.getInstance().getProperty(SettingsManager.REPO_PATH));
    }

    private static class SingletonHolder {
        public static final HgRepositoryManager INSTANCE = new HgRepositoryManager();
    }

    public static RepositoryManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    @Override
    public void prepareRepository() throws RepositoryException {
        performVersionCheck(); //this will throw exception if mercurial is not availible

        if (repoDirectory.exists() && repoDirectory.isDirectory() && validHgRepo(repoDirectory)) {
            pullFromServer();
        } else {
            repoDirectory.mkdir();
            cloneFromServer();
        }
    }

    private boolean validHgRepo(File repoDirectory) throws RepositoryException {
        try {
            List<String> retval = exec(pack(HG_COMMAND, HG_SUMMARY_PARAM), repoDirectory);
            if (retval.size() == 1 && retval.get(0).matches("^abort.+hg not found.+$")) {
                return false;
            }
            return true;
        } catch (HgException ex) {
            throw new RepositoryException(ex);
        }
    }

    @Override
    public void updateRepository() throws RepositoryException {
        pullFromServer();
    }

    private void performVersionCheck() throws RepositoryException {
        try {
            List<String> retval = exec(pack(HG_COMMAND, HG_VERSION_PARAM), null);
            if (retval.size() > 0) {
                String firstLine = retval.get(0);
                if (firstLine.matches("^Mercurial.+version \\d.\\d.\\d.+$")) {
                    Matcher m = Pattern.compile("version \\d.\\d.\\d").matcher(firstLine);
                    m.find();
                    logger.log(Level.INFO, "Using Mercurial {0} as a repository tool.", firstLine.substring(m.start() + 8, m.end()));
                    return;
                }
            }
            throw new RepositoryException("Mercurial Distributed SCM was not detected! Please make sure \"hg\" command is availible.");
        } catch (HgException ex) {
            throw new RepositoryException(ex);
        }
    }

    private List<String> pack(String... parts) {
        ArrayList<String> cmd = new ArrayList<String>(parts.length);
        cmd.addAll(Arrays.asList(parts));
        return cmd;
    }

    private void cloneFromServer() throws RepositoryException {
        try {
            String serverURL = SettingsManager.getInstance().getProperty(SettingsManager.REPO_SERVER);
            List<String> retval = exec(pack(HG_COMMAND, HG_CLONE_PARAM, "--pull", serverURL, "."), repoDirectory);
            if (retval.size() == 1 && retval.get(0).matches("^abort.+Connection refused$")) {
                throw new RepositoryException("Could not connect to repository server : " + serverURL);
            }
            logger.log(Level.INFO, "Repository cloned from {0}", serverURL);
        } catch (HgException ex) {
            throw new RepositoryException(ex);
        }
    }

    private void pullFromServer() throws RepositoryException {
        try {
            String serverURL = SettingsManager.getInstance().getProperty(SettingsManager.REPO_SERVER);
            List<String> retval = exec(pack(HG_COMMAND, HG_PULL_PARAM, "--update", serverURL), repoDirectory);
            if (retval.size() == 1 && retval.get(0).matches("^abort.+Connection refused$")) {
                throw new RepositoryException("Could not connect to repository server : " + serverURL);
            }
            logger.log(Level.INFO, "Repository updated from {0}", serverURL);
        } catch (HgException ex) {
            throw new RepositoryException(ex);
        }
    }
}
