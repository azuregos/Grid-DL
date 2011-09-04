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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxEditorParser;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import ua.kpi.griddl.core.infrastructure.abox.ABoxManager;
import ua.kpi.griddl.core.infrastructure.cache.CacheManager;
import ua.kpi.griddl.core.settings.SettingsManager;
import ua.kpi.griddl.ws.WSQueryTask;

/**
 *
 * @author Pospishniy Olexandr <pospishniy@kpi.in.ua>
 */
public class QueryTask implements Runnable {

    private Logger logger;

    public static enum TaskStatus {

        SUBMITED("Submited"), QUEUED("Queued"), RUNNING("Running"), DONE("Done"), FAILED("Failed");
        private final String status;

        private TaskStatus(String status) {
            this.status = status;
        }

        @Override
        public String toString() {
            return status;
        }
    };
    private Long id;
    private int progress;
    private Date submitTime;
    private String query;
    private List<String> ontologies;
    private TaskStatus status;
    private Set<OWLNamedIndividual> resultSet;
    private ByteArrayOutputStream log;

    public QueryTask(Long id, String query, List<String> ontologies) {
        this.id = id;
        this.query = query;
        this.status = TaskStatus.SUBMITED;
        this.ontologies = ontologies;
        this.log = new ByteArrayOutputStream();
        this.logger = Logger.getLogger("QueryTask ID:" + id);
        this.logger.setUseParentHandlers(false);
        this.logger.addHandler(new StreamHandler(log, new SimpleFormatter()));
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public Date getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(Date submitTime) {
        this.submitTime = submitTime;
    }

    public String getTaskLog() {
        return log.toString();
    }

    public Set<OWLNamedIndividual> getResultSet() {
        return resultSet;
    }

    public List<String> getOntologies() {
        return ontologies;
    }

    public void setOntologies(List<String> ontologies) {
        this.ontologies = ontologies;
    }

    @Override
    public void run() {
        try {
            logger.log(Level.INFO, "Query Task started");
            status = TaskStatus.RUNNING;

            //Prepare
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLDataFactory factory = manager.getOWLDataFactory();
            logger.log(Level.INFO, "OWLDataFactory prepared");

            //Load ontology with system Tbox
            OWLOntology aBoxOntology = ABoxManager.getInstance().getActiveABox();
            if (ontologies != null) {
                File repoRoot = new File(SettingsManager.getInstance().getProperty(SettingsManager.REPO_PATH));
                manager.setSilentMissingImportsHandling(true);
                for (String ontologyPath : ontologies) {
                    OWLOntology suffix = manager.loadOntologyFromOntologyDocument(new File(repoRoot, ontologyPath));
                    manager.addAxioms(aBoxOntology, suffix.getAxioms());
                }
            }
            logger.log(Level.INFO, "Ontology assambled");

            //Start reasoner
            logger.log(Level.INFO, "Initializing reasoner");
            OWLReasoner reasoner = ReasonerManager.getInstance().
                    getReasonerFactory().createNonBufferingReasoner(aBoxOntology);
            logger.log(Level.INFO, "Reasoner ready");

            //Parse query
            logger.log(Level.INFO, "Start reasoning");
            flushLogger();
            ManchesterOWLSyntaxEditorParser parser = new ManchesterOWLSyntaxEditorParser(factory, query);
            parser.setOWLEntityChecker(new OWLEntityFactory(aBoxOntology, factory));
            OWLClassExpression parseClassExpression = parser.parseClassExpression();
            logger.log(Level.INFO, "Reasoning completed");

            //Perform reasoning
            resultSet = reasoner.getInstances(parseClassExpression, false).getFlattened();
            logger.log(Level.INFO, "Result saved");
            
            CacheManager.getInstance().put(this);
            logger.log(Level.INFO, "Cache updated");
            
            status = TaskStatus.DONE;
            logger.log(Level.INFO, "Query Task finished");
        } catch (Throwable e) {
            status = TaskStatus.FAILED;
            logger.log(Level.SEVERE, "QueryTask failed!", e);
        } finally {
            for (Handler handler : logger.getHandlers()) {
                handler.close();
            }
        }
    }

    private void flushLogger() {
        for (Handler handler : logger.getHandlers()) {
            handler.flush();
        }
    }

    public WSQueryTask toWSQueryTask() {
        WSQueryTask wsqt = new WSQueryTask();
        wsqt.setId(id);
        wsqt.setOntologies(ontologies);
        wsqt.setQuery(query);
        wsqt.setStatus(status.toString());
        wsqt.setSubmitTime(submitTime);
        if (resultSet != null) {
            List<String> rs = new ArrayList<String>(resultSet.size());
            for (OWLNamedIndividual individual : resultSet) {
                rs.add(individual.toString());
            }
            wsqt.setResultSet(rs);
        }
        return wsqt;
    }
}
