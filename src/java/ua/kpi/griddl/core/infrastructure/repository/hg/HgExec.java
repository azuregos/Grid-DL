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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Pospishniy Olexandr <pospishniy@kpi.in.ua>
 */
public class HgExec {

    public static final String HG_COMMAND = "hg";
    public static final String HG_VERSION_PARAM = "version";
    public static final String HG_SUMMARY_PARAM = "summary";
    public static final String HG_CLONE_PARAM = "clone";
    public static final String HG_PULL_PARAM = "pull";
    
    private static final Logger logger = Logger.getLogger(HgExec.class.getName());

    public static List<String> exec(final List<String> commandLine, File workdir) throws HgException {
        final ProcessBuilder pb = new ProcessBuilder(commandLine);
        pb.directory(workdir);
        try {
            Callable<List<String>> callable = new Callable<List<String>>() {

                @Override
                public List<String> call() throws HgException {
                    return execProcess(pb);
                }
            };
            return callable.call();
        } catch (HgException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.log(Level.WARNING, null, ex);
            return null;
        }
    }

    private static List<String> execProcess(ProcessBuilder pb) throws HgException {
        final List<String> list = new ArrayList<String>();
        BufferedReader input = null;
        BufferedReader error = null;
        Process proc = null;
        try {
            proc = pb.start();

            input = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            error = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
            final BufferedReader errorReader = error;
            final LinkedList<String> errorOutput = new LinkedList<String>();
            final BufferedReader inputReader = input;
            final LinkedList<String> inputOutput = new LinkedList<String>();
            Thread errorThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        String line;
                        while ((line = errorReader.readLine()) != null) {
                            errorOutput.add(line);
                        }
                    } catch (IOException ex) {
                        // not interested
                    }
                }
            });
            errorThread.start();
            Thread inputThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        String line;
                        while ((line = inputReader.readLine()) != null) {
                            inputOutput.add(line);
                        }
                    } catch (IOException ex) {
                        // not interested
                    }
                }
            });
            inputThread.start();
            try {
                inputThread.join();
                errorThread.join();
            } catch (InterruptedException ex) {
                logger.log(Level.FINE, "execEnv():  process interrupted {0}", ex); // NOI18N
                // We get here is we try to cancel so kill the process
                if (proc != null) {
                    proc.destroy();
                }
                throw new HgException.HgCommandCanceledException("MSG_COMMAND_CANCELLED"); //NOI18N
            }
            list.addAll(inputOutput); // appending output
            input.close();
            input = null;
            list.addAll(errorOutput); // appending error output
            error.close();
            error = null;
            try {
                proc.waitFor();
                // By convention we assume that 255 (or -1) is a serious error.
                // For instance, the command line could be too long.
                if (proc.exitValue() == 255) {
                    logger.log(Level.FINE, "execEnv():  process returned 255"); // NOI18N
                    if (list.isEmpty()) {
                        throw new HgException.HgTooLongArgListException("MSG_UNABLE_EXECUTE_COMMAND");
                    }
                }
            } catch (InterruptedException e) {
                logger.log(Level.FINE, "execEnv():  process interrupted {0}", e); // NOI18N
            }
        } catch (InterruptedIOException e) {
            // We get here is we try to cancel so kill the process
            logger.log(Level.FINE, "execEnv():  execEnv(): InterruptedIOException {0}", e); // NOI18N
            if (proc != null) {
                proc.destroy();
            }
            throw new HgException("MSG_COMMAND_CANCELLED");
        } catch (IOException e) {
            // Hg does not seem to be returning error status != 0
            // even when it fails when for instance adding an already tracked file to
            // the repository - we will have to examine the output in the context of the
            // calling func and raise exceptions there if needed

            throw new HgException(e.getMessage());
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ioex) {
                    //Just ignore. Closing streams.
                }
                input = null;
            }
            if (error != null) {
                try {
                    error.close();
                } catch (IOException ioex) {
                    //Just ignore. Closing streams.
                }
            }
        }
        return list;
    }
}
