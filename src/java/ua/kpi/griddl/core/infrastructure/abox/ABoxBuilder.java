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

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

/**
 *
 * @author Pospishniy Olexandr <pospishniy@kpi.in.ua>
 */
public class ABoxBuilder {

    private String glueOntologyPath;
    private String ontologyIRI;
    private OWLOntologyManager manager;
    private OWLDataFactory factory;
    private OWLOntology ontology;
    private OWLOntology glueOntology;
    private PrefixManager gluePrefix;
    private PrefixManager currentPrefix;

    public ABoxBuilder(String glueOntologyPath, String ontologyIRI) {
        this.glueOntologyPath = glueOntologyPath;
        this.ontologyIRI = ontologyIRI;
        this.manager = OWLManager.createOWLOntologyManager();
        this.factory = manager.getOWLDataFactory();
    }

    public void createNewEmptyOntology() {
        try {
            ontology = manager.createOntology(IRI.create(ontologyIRI));
            glueOntology = manager.loadOntologyFromOntologyDocument(new File(glueOntologyPath));
            OWLImportsDeclaration glueImport = factory.getOWLImportsDeclaration(glueOntology.getOntologyID().getOntologyIRI());
            manager.applyChange(new AddImport(ontology, glueImport));
            gluePrefix = new DefaultPrefixManager(glueOntology.getOntologyID().getOntologyIRI().toString() + "#");
            currentPrefix = new DefaultPrefixManager(ontology.getOntologyID().getOntologyIRI().toString() + "#");
        } catch (Exception ex) {
            Logger.getLogger(ABoxBuilder.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public OWLOntology getOntology() {
        return ontology;
    }

    public void saveCEs(NamingEnumeration<SearchResult> namingEnumeration) throws NamingException {
        if (namingEnumeration != null) {
            int i = 1;
            while (namingEnumeration.hasMore()) {

                SearchResult result = namingEnumeration.next();
                Attributes attributes = result.getAttributes();

                OWLClass ceClass = factory.getOWLClass("ComputingElement", gluePrefix);
                String ceName = attributes.get("GlueCEUniqueID").get().toString();
                ceName = ceName.replaceAll(":", "").replaceAll("/", "");

                OWLNamedIndividual ce = factory.getOWLNamedIndividual(ceName, currentPrefix);

                if (!ontology.containsIndividualInSignature(ce.getIRI())) {

                    manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(ceClass, ce));
                    manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasUniqueID", gluePrefix), ce, attributes.get("GlueCEUniqueID").get().toString()));
                    if (attributes.get("GlueCEName") != null) {
                        manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasName", gluePrefix), ce, attributes.get("GlueCEName").get().toString()));
                    } else {
                        manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasName", gluePrefix), ce, ""));
                    }
                    if (attributes.get("GlueInformationServiceURL") != null && !attributes.get("GlueInformationServiceURL").get().toString().equals("none")) {
                        OWLLiteral isURL = factory.getOWLLiteral(attributes.get("GlueInformationServiceURL").get().toString(), OWL2Datatype.XSD_ANY_URI);
                        manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasInformationServiceURL", gluePrefix), ce, isURL));
                    }

                    if (attributes.get("GlueCECapability") != null) {
                        NamingEnumeration ne = attributes.get("GlueCECapability").getAll();
                        while (ne.hasMore()) {
                            manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasCapability", gluePrefix), ce, ne.next().toString()));
                        }
                    }

                    if (attributes.get("GlueServiceAccessControlBaseRule") != null) {
                        NamingEnumeration ne = attributes.get("GlueServiceAccessControlBaseRule").getAll();
                        while (ne.hasMore()) {
                            String[] ACBR = ne.next().toString().split(":");
                            if (ACBR.length == 2) {
                                OWLAnonymousIndividual individual = factory.getOWLAnonymousIndividual();
                                manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(factory.getOWLClass("AccessControlBaseRule", gluePrefix), individual));
                                manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasPrefix", gluePrefix), individual, ACBR[0]));
                                manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasSCN", gluePrefix), individual, ACBR[1]));
                                manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(factory.getOWLObjectProperty("hasAccessControlBaseRule", gluePrefix), ce, individual));
                            } else {
                                //GlueServiceAccessControlBaseRule is wrong!
                            }
                        }
                    }

                    if (attributes.get("GlueCEImplementationName") != null && attributes.get("GlueCEImplementationVersion") != null) {
                        OWLAnonymousIndividual individual = factory.getOWLAnonymousIndividual();
                        manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(factory.getOWLClass("Implementation", gluePrefix), individual));
                        manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasImplementationName", gluePrefix), individual, attributes.get("GlueCEImplementationName").get().toString()));
                        manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasImplementationVersion", gluePrefix), individual, attributes.get("GlueCEImplementationVersion").get().toString()));
                        manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(factory.getOWLObjectProperty("hasImplementation", gluePrefix), ce, individual));
                    }
                }



                //OWLNamedIndividual ceInfo = factory.getOWLNamedIndividual("CEINfo." + ceName, currentPrefix);
                OWLAnonymousIndividual ceInfo = factory.getOWLAnonymousIndividual();
                manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(factory.getOWLClass("CEInfo", gluePrefix), ceInfo));
                manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(factory.getOWLObjectProperty("hasInfo", gluePrefix), ce, ceInfo));

                if (attributes.get("GlueCEInfoLRMSType") != null) {
                    OWLNamedIndividual lrmsTypeInd = factory.getOWLNamedIndividual(attributes.get("GlueCEInfoLRMSType").get().toString(), currentPrefix);
                    if (!ontology.containsIndividualInSignature(lrmsTypeInd.getIRI())) {
                        OWLClass srvTypeClass = factory.getOWLClass("LRMSType", gluePrefix);
                        manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(srvTypeClass, lrmsTypeInd));
                    }
                    manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(factory.getOWLObjectProperty("hasLRMSType", gluePrefix), ceInfo, lrmsTypeInd));
                }
                //manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasLRMSVersion", prefix), ceInfo, attributes.get("GlueCEInfoGRAMVersion").get().toString()));
                //manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasLRMSVersion", prefix), ceInfo, attributes.get("GlueCEInfoTotalCPUs").get().toString()));
                manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasLRMSVersion", gluePrefix), ceInfo, attributes.get("GlueCEInfoLRMSVersion").get().toString()));
                manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasHostName", gluePrefix), ceInfo, attributes.get("GlueCEInfoHostName").get().toString()));
                OWLLiteral gateport = factory.getOWLLiteral(attributes.get("GlueCEInfoGateKeeperPort").get().toString(), OWL2Datatype.XSD_INTEGER);
                manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasGateKeeperPort", gluePrefix), ceInfo, gateport));
                manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasContactString", gluePrefix), ceInfo, attributes.get("GlueCEInfoContactString").get().toString()));
                manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasJobManager", gluePrefix), ceInfo, attributes.get("GlueCEInfoJobManager").get().toString()));
                if (attributes.get("hasApplicationDir") != null) {
                    manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasApplicationDir", gluePrefix), ceInfo, attributes.get("GlueCEInfoApplicationDir").get().toString()));
                }
                if (attributes.get("hasDataDir") != null) {
                    manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasDataDir", gluePrefix), ceInfo, attributes.get("GlueCEInfoDataDir").get().toString()));
                }
                if (attributes.get("hasDefaultSE") != null) {
                    manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasDefaultSE", gluePrefix), ceInfo, attributes.get("GlueCEInfoDefaultSE").get().toString()));
                }

                //OWLNamedIndividual ceState = factory.getOWLNamedIndividual("CEState." + ceName, currentPrefix);
                OWLAnonymousIndividual ceState = factory.getOWLAnonymousIndividual();
                manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(factory.getOWLClass("CEState", gluePrefix), ceState));
                manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(factory.getOWLObjectProperty("hasState", gluePrefix), ce, ceState));

                if (attributes.get("GlueCEStateEstimatedResponseTime") != null) {
                    OWLLiteral literal = factory.getOWLLiteral(attributes.get("GlueCEStateEstimatedResponseTime").get().toString(), OWL2Datatype.XSD_INTEGER);
                    manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasEstimatedResponseTime", gluePrefix), ceState, literal));
                }

                if (attributes.get("GlueCEStateWorstResponseTime") != null) {
                    OWLLiteral literal = factory.getOWLLiteral(attributes.get("GlueCEStateWorstResponseTime").get().toString(), OWL2Datatype.XSD_INTEGER);
                    manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasWorstResponseTime", gluePrefix), ceState, literal));
                }

                if (attributes.get("GlueCEStateRunningJobs") != null) {
                    OWLLiteral literal = factory.getOWLLiteral(attributes.get("GlueCEStateRunningJobs").get().toString(), OWL2Datatype.XSD_INTEGER);
                    manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasRunningJobs", gluePrefix), ceState, literal));
                }

                if (attributes.get("GlueCEStateWaitingJobs") != null) {
                    OWLLiteral literal = factory.getOWLLiteral(attributes.get("GlueCEStateWaitingJobs").get().toString(), OWL2Datatype.XSD_INTEGER);
                    manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasWaitingJobs", gluePrefix), ceState, literal));
                }

                if (attributes.get("GlueCEStateTotalJobs") != null) {
                    OWLLiteral literal = factory.getOWLLiteral(attributes.get("GlueCEStateTotalJobs").get().toString(), OWL2Datatype.XSD_INTEGER);
                    manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasGateTotalJobs", gluePrefix), ceState, literal));
                }

//                if (attributes.get("GlueCEStateFreeCPUs") != null) {
//                    OWLLiteral literal = factory.getOWLLiteral(attributes.get("GlueCEStateFreeCPUs").get().toString(), OWL2Datatype.XSD_INTEGER);
//                    manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasStateFreeCPUs", prefix), ceState, gateport));
//                }

                if (attributes.get("GlueCEStateFreeJobSlots") != null) {
                    OWLLiteral literal = factory.getOWLLiteral(attributes.get("GlueCEStateFreeJobSlots").get().toString(), OWL2Datatype.XSD_INTEGER);
                    manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasFreeJobSlots", gluePrefix), ceState, literal));
                }


                if (attributes.get("GlueCEStateStatus") != null) {
                    OWLNamedIndividual ceStatusInd = factory.getOWLNamedIndividual(attributes.get("GlueCEStateStatus").get().toString(), currentPrefix);
                    if (!ontology.containsIndividualInSignature(ceStatusInd.getIRI())) {
                        OWLClass ceStatusClass = factory.getOWLClass("CEStatus", gluePrefix);
                        manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(ceStatusClass, ceStatusInd));
                    }
                    manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(factory.getOWLObjectProperty("hasStatus", gluePrefix), ceState, ceStatusInd));
                }


                //OWLNamedIndividual cePolicy = factory.getOWLNamedIndividual("CEPolicy." + ceName, currentPrefix);
                OWLAnonymousIndividual cePolicy = factory.getOWLAnonymousIndividual();
                manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(factory.getOWLClass("CEPolicy", gluePrefix), cePolicy));
                manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(factory.getOWLObjectProperty("hasPolicy", gluePrefix), ce, cePolicy));

                if (attributes.get("GlueCEPolicyAssignedJobSlots") != null) {
                    OWLLiteral literal = factory.getOWLLiteral(attributes.get("GlueCEPolicyAssignedJobSlots").get().toString(), OWL2Datatype.XSD_INTEGER);
                    manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasAssignedJobSlots", gluePrefix), cePolicy, literal));
                }
                if (attributes.get("GlueCEPolicyMaxCPUTime") != null) {
                    OWLLiteral literal = factory.getOWLLiteral(attributes.get("GlueCEPolicyMaxCPUTime").get().toString(), OWL2Datatype.XSD_INTEGER);
                    manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasMaxCPUTime", gluePrefix), cePolicy, literal));
                }
                if (attributes.get("GlueCEPolicyMaxObtainableCPUTime") != null) {
                    OWLLiteral literal = factory.getOWLLiteral(attributes.get("GlueCEPolicyMaxObtainableCPUTime").get().toString(), OWL2Datatype.XSD_INTEGER);
                    manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasMaxObtainableCPUTime", gluePrefix), cePolicy, literal));
                }
                if (attributes.get("GlueCEPolicyMaxObtainableWallClockTime") != null) {
                    OWLLiteral literal = factory.getOWLLiteral(attributes.get("GlueCEPolicyMaxObtainableWallClockTime").get().toString(), OWL2Datatype.XSD_INTEGER);
                    manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasMaxObtainableWallClockTime", gluePrefix), cePolicy, literal));
                }
                if (attributes.get("GlueCEPolicyMaxRunningJobs") != null) {
                    OWLLiteral literal = factory.getOWLLiteral(attributes.get("GlueCEPolicyMaxRunningJobs").get().toString(), OWL2Datatype.XSD_INTEGER);
                    manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasMaxRunningJobs", gluePrefix), cePolicy, literal));
                }
                if (attributes.get("GlueCEPolicyMaxSlotsPerJob") != null) {
                    OWLLiteral literal = factory.getOWLLiteral(attributes.get("GlueCEPolicyMaxSlotsPerJob").get().toString(), OWL2Datatype.XSD_INTEGER);
                    manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasMaxSlotsPerJob", gluePrefix), cePolicy, literal));
                }
                if (attributes.get("GlueCEPolicyMaxTotalJobs") != null) {
                    OWLLiteral literal = factory.getOWLLiteral(attributes.get("GlueCEPolicyMaxTotalJobs").get().toString(), OWL2Datatype.XSD_INTEGER);
                    manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasMaxTotalJobs", gluePrefix), cePolicy, literal));
                }
                if (attributes.get("GlueCEPolicyMaxWaitingJobs") != null) {
                    OWLLiteral literal = factory.getOWLLiteral(attributes.get("GlueCEPolicyMaxWaitingJobs").get().toString(), OWL2Datatype.XSD_INTEGER);
                    manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasMaxWaitingJobs", gluePrefix), cePolicy, literal));
                }
                if (attributes.get("GlueCEPolicyMaxWallClockTime") != null) {
                    OWLLiteral literal = factory.getOWLLiteral(attributes.get("GlueCEPolicyMaxWallClockTime").get().toString(), OWL2Datatype.XSD_INTEGER);
                    manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasMaxWallClockTime", gluePrefix), cePolicy, literal));
                }
                if (attributes.get("GlueCEPolicyPreemption") != null) {
                    OWLLiteral literal = factory.getOWLLiteral(attributes.get("GlueCEPolicyPreemption").get().toString(), OWL2Datatype.XSD_INTEGER);
                    manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasPreemption", gluePrefix), cePolicy, literal));
                }
                if (attributes.get("GlueCEPolicyPriority") != null) {
                    OWLLiteral literal = factory.getOWLLiteral(attributes.get("GlueCEPolicyPriority").get().toString(), OWL2Datatype.XSD_INTEGER);
                    manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasPriority", gluePrefix), cePolicy, literal));
                }

                if (attributes.get("GlueCEAccessControlBaseRule") != null) {
                    NamingEnumeration ne = attributes.get("GlueCEAccessControlBaseRule").getAll();
                    while (ne.hasMore()) {
                        String[] ACBR = ne.next().toString().split(":");
                        if (ACBR.length == 2) {
                            OWLAnonymousIndividual individual = factory.getOWLAnonymousIndividual();
                            manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(factory.getOWLClass("AccessControlBaseRule", gluePrefix), individual));
                            manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasPrefix", gluePrefix), individual, ACBR[0]));
                            manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasSCN", gluePrefix), individual, ACBR[1]));
                            manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(factory.getOWLObjectProperty("hasAccessControlBaseRule", gluePrefix), ce, individual));
                        } else {
                            //GlueServiceAccessControlBaseRule is wrong!
                        }
                    }
                }

                //TODO bind to Cluster
            }
        }
    }

    public void saveServices(NamingEnumeration<SearchResult> namingEnumeration) throws NamingException {
        if (namingEnumeration != null) {
            int i = 1;
            while (namingEnumeration.hasMore()) {
                SearchResult result = namingEnumeration.next();
                Attributes attributes = result.getAttributes();

                OWLClass serviceClass = factory.getOWLClass("Service", gluePrefix);
                String serviceName = attributes.get("GlueServiceUniqueID").get().toString();
                serviceName = serviceName.replaceAll(":", "").replaceAll("/", "");

                OWLNamedIndividual service = factory.getOWLNamedIndividual(serviceName, currentPrefix);

                if (!ontology.containsIndividualInSignature(service.getIRI())) {

                    manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(serviceClass, service));
                    manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasUniqueID", gluePrefix), service, attributes.get("GlueServiceUniqueID").get().toString()));
                    if (attributes.get("GlueServiceName") != null) {
                        manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasName", gluePrefix), service, attributes.get("GlueServiceName").get().toString()));
                    } else {
                        manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasName", gluePrefix), service, ""));
                    }

                    if (attributes.get("GlueServiceVersion") != null && !attributes.get("GlueServiceVersion").get().toString().equals("none")) {
                        manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasVersion", gluePrefix), service, attributes.get("GlueServiceVersion").get().toString()));
                    }
                    if (attributes.get("GlueServiceEndpoint") != null && !attributes.get("GlueServiceEndpoint").get().toString().equals("none")) {
                        OWLLiteral endpoint = factory.getOWLLiteral(attributes.get("GlueServiceEndpoint").get().toString());
                        manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasEndpoint", gluePrefix), service, endpoint));
                    }
                    if (attributes.get("GlueServiceWSDL") != null && !attributes.get("GlueServiceWSDL").get().toString().equals("none")) {
                        OWLLiteral wsdl = factory.getOWLLiteral(attributes.get("GlueServiceWSDL").get().toString());
                        manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasWSDL", gluePrefix), service, wsdl));
                    }
                    if (attributes.get("GlueServiceSemantics") != null && !attributes.get("GlueServiceSemantics").get().toString().equals("none")) {
                        OWLLiteral semantics = factory.getOWLLiteral(attributes.get("GlueServiceSemantics").get().toString());
                        manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasSemantics", gluePrefix), service, semantics));
                    }
                    //TODO MayBe convert to date format after all;
                    if (attributes.get("GlueServiceStartTime") != null) {
                        OWLLiteral startTime = factory.getOWLLiteral(attributes.get("GlueServiceStartTime").get().toString());
                        manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasStartTime", gluePrefix), service, startTime));
                    }

                    if (attributes.get("GlueServiceType") != null) {
                        OWLNamedIndividual srvTypeInd = factory.getOWLNamedIndividual(attributes.get("GlueServiceType").get().toString(), currentPrefix);
                        if (!ontology.containsIndividualInSignature(srvTypeInd.getIRI())) {
                            OWLClass srvTypeClass = factory.getOWLClass("ServiceType", gluePrefix);
                            OWLNamedIndividual srvType = factory.getOWLNamedIndividual(attributes.get("GlueServiceType").get().toString(), currentPrefix);
                            manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(srvTypeClass, srvType));
                            manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(factory.getOWLObjectProperty("hasType", gluePrefix), service, srvType));
                        } else {
                            manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(factory.getOWLObjectProperty("hasType", gluePrefix), service, srvTypeInd));
                        }
                    }

                    if (attributes.get("GlueServiceStatus") != null) {
                        String status = attributes.get("GlueServiceStatus").get().toString();
                        status = status.replaceAll(" ", "_");
                        OWLNamedIndividual srvStatusInd = factory.getOWLNamedIndividual(status, currentPrefix);
                        if (!ontology.containsIndividualInSignature(srvStatusInd.getIRI())) {
                            OWLClass srvStatusClass = factory.getOWLClass("ServiceStatus", gluePrefix);
                            OWLNamedIndividual srvType = factory.getOWLNamedIndividual(status, currentPrefix);
                            manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(srvStatusClass, srvType));
                            manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(factory.getOWLObjectProperty("hasStatus", gluePrefix), service, srvType));
                        } else {
                            manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(factory.getOWLObjectProperty("hasStatus", gluePrefix), service, srvStatusInd));
                        }
                    }

                    if (attributes.get("GlueServiceStatusInfo") != null) {
                        OWLLiteral statusInfo = factory.getOWLLiteral(attributes.get("GlueServiceStatusInfo").get().toString());
                        manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasStatusInfo", gluePrefix), service, statusInfo));
                    }

                    if (attributes.get("GlueServiceOwner") != null) {
                        NamingEnumeration ne = attributes.get("GlueServiceOwner").getAll();
                        while (ne.hasMore()) {
                            String ownerName = ne.next().toString();
                            ownerName = ownerName.replaceAll(" ", "_");
                            ownerName = ownerName.replaceAll(":", "_");
                            OWLNamedIndividual owner = factory.getOWLNamedIndividual(ownerName, currentPrefix);
                            if (!ontology.containsIndividualInSignature(owner.getIRI())) {
                                OWLClass subjectClass = factory.getOWLClass("Subject", gluePrefix);
                                manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(subjectClass, owner));
                            }
                            manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(factory.getOWLObjectProperty("hasOwner", gluePrefix), service, owner));
                        }
                    }

                    if (attributes.get("GlueServiceAccessControlBaseRule") != null) {
                        NamingEnumeration ne = attributes.get("GlueServiceAccessControlBaseRule").getAll();
                        while (ne.hasMore()) {
                            String[] ACBR = ne.next().toString().split(":");
                            if (ACBR.length == 2) {
                                OWLAnonymousIndividual individual = factory.getOWLAnonymousIndividual();
                                manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(factory.getOWLClass("AccessControlBaseRule", gluePrefix), individual));
                                manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasPrefix", gluePrefix), individual, ACBR[0]));
                                manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasSCN", gluePrefix), individual, ACBR[1]));
                                manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(factory.getOWLObjectProperty("hasAccessControlBaseRule", gluePrefix), service, individual));
                            } else {
                                // GlueServiceAccessControlBaseRule is wrong!
                            }
                        }
                    }

                    if (attributes.get("GlueForeignKey") != null) {
                        NamingEnumeration ne = attributes.get("GlueForeignKey").getAll();
                        while (ne.hasMore()) {
                            String[] FK = ne.next().toString().split("=");
                            if (FK.length == 2) {
                                OWLNamedIndividual reference = factory.getOWLNamedIndividual(FK[1].replaceAll(":", "").replaceAll("/", ""), currentPrefix);
                                if (ontology.containsIndividualInSignature(reference.getIRI())) {
                                    if (FK[0].equals("GlueSiteUniqueID")) {
                                        manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(factory.getOWLObjectProperty("partOf", gluePrefix), service, reference));
                                    } else if (FK[0].equals("GlueServiceUniqueID")) {
                                        manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(factory.getOWLObjectProperty("inRelationshipWith", gluePrefix), service, reference));
                                    }
                                }
                            } else {
                                //GlueForeignKey is wrong!
                            }
                        }
                    }

                }
            }
        }
    }

    public void saveVOViews(NamingEnumeration<SearchResult> namingEnumeration) throws NamingException {
        if (namingEnumeration != null) {
            int i = 1;
            while (namingEnumeration.hasMore()) {
                SearchResult result = namingEnumeration.next();
                Attributes attributes = result.getAttributes();

                OWLClass vovClass = factory.getOWLClass("VOView", gluePrefix);

                String[] key = attributes.get("GlueChunkKey").get().toString().split("=");
                String ceID = null;
                if (key.length == 2 && key[0].equals("GlueCEUniqueID")) {
                    ceID = key[1];
                    ceID = ceID.replaceAll(":", "").replaceAll("/", "");
                }

                OWLNamedIndividual CE = factory.getOWLNamedIndividual(ceID, currentPrefix);

                if (ontology.containsIndividualInSignature(CE.getIRI())) {
                    String voViewLocalID = attributes.get("GlueVOViewLocalID").get().toString();
                    voViewLocalID = voViewLocalID.replaceAll("/", ".");
                    OWLNamedIndividual voview = factory.getOWLNamedIndividual(ceID + "-" + voViewLocalID, currentPrefix);

                    manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(vovClass, voview));
                    manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(factory.getOWLObjectProperty("hasVOView", gluePrefix), CE, voview));

                    if (attributes.get("GlueCECapability") != null) {
                        NamingEnumeration ne = attributes.get("GlueCECapability").getAll();
                        while (ne.hasMore()) {
                            manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasCapability", gluePrefix), voview, ne.next().toString()));
                        }
                    }

                    manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasLocalID", gluePrefix), voview, voViewLocalID));

                    //OWLNamedIndividual ceInfo = factory.getOWLNamedIndividual("CEINfo." + ceID + "-" + voViewLocalID, currentPrefix);
                    OWLAnonymousIndividual ceInfo = factory.getOWLAnonymousIndividual();
                    manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(factory.getOWLClass("CEInfo", gluePrefix), ceInfo));
                    manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(factory.getOWLObjectProperty("hasInfo", gluePrefix), voview, ceInfo));

                    if (attributes.get("GlueCEInfoApplicationDir") != null) {
                        manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasApplicationDir", gluePrefix), ceInfo, attributes.get("GlueCEInfoApplicationDir").get().toString()));
                    }
                    if (attributes.get("GlueCEInfoDataDir") != null) {
                        manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasDataDir", gluePrefix), ceInfo, attributes.get("GlueCEInfoDataDir").get().toString()));
                    }
                    if (attributes.get("GlueCEInfoDefaultSE") != null) {
                        manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasDefaultSE", gluePrefix), ceInfo, attributes.get("GlueCEInfoDefaultSE").get().toString()));
                    }

                    //OWLNamedIndividual ceState = factory.getOWLNamedIndividual("CEState." + ceID + "-" + voViewLocalID, currentPrefix);
                    OWLAnonymousIndividual ceState = factory.getOWLAnonymousIndividual();
                    manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(factory.getOWLClass("CEState", gluePrefix), ceState));
                    manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(factory.getOWLObjectProperty("hasState", gluePrefix), voview, ceState));

                    if (attributes.get("GlueCEStateEstimatedResponseTime") != null) {
                        OWLLiteral literal = factory.getOWLLiteral(attributes.get("GlueCEStateEstimatedResponseTime").get().toString(), OWL2Datatype.XSD_INTEGER);
                        manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasEstimatedResponseTime", gluePrefix), ceState, literal));
                    }

                    if (attributes.get("GlueCEStateWorstResponseTime") != null) {
                        OWLLiteral literal = factory.getOWLLiteral(attributes.get("GlueCEStateWorstResponseTime").get().toString(), OWL2Datatype.XSD_INTEGER);
                        manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasWorstResponseTime", gluePrefix), ceState, literal));
                    }

                    if (attributes.get("GlueCEStateRunningJobs") != null) {
                        OWLLiteral literal = factory.getOWLLiteral(attributes.get("GlueCEStateRunningJobs").get().toString(), OWL2Datatype.XSD_INTEGER);
                        manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasRunningJobs", gluePrefix), ceState, literal));
                    }

                    if (attributes.get("GlueCEStateWaitingJobs") != null) {
                        OWLLiteral literal = factory.getOWLLiteral(attributes.get("GlueCEStateWaitingJobs").get().toString(), OWL2Datatype.XSD_INTEGER);
                        manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasWaitingJobs", gluePrefix), ceState, literal));
                    }

                    if (attributes.get("GlueCEStateTotalJobs") != null) {
                        OWLLiteral literal = factory.getOWLLiteral(attributes.get("GlueCEStateTotalJobs").get().toString(), OWL2Datatype.XSD_INTEGER);
                        manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasGateTotalJobs", gluePrefix), ceState, literal));
                    }

//                if (attributes.get("GlueCEStateFreeCPUs") != null) {
//                    OWLLiteral literal = factory.getOWLLiteral(attributes.get("GlueCEStateFreeCPUs").get().toString(), OWL2Datatype.XSD_INTEGER);
//                    manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasStateFreeCPUs", prefix), ceState, gateport));
//                }

                    if (attributes.get("GlueCEStateFreeJobSlots") != null) {
                        OWLLiteral literal = factory.getOWLLiteral(attributes.get("GlueCEStateFreeJobSlots").get().toString(), OWL2Datatype.XSD_INTEGER);
                        manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasFreeJobSlots", gluePrefix), ceState, literal));
                    }


                    if (attributes.get("GlueCEStateStatus") != null) {
                        OWLNamedIndividual lrmsTypeInd = factory.getOWLNamedIndividual(attributes.get("GlueCEStateStatus").get().toString(), currentPrefix);
                        if (!ontology.containsIndividualInSignature(lrmsTypeInd.getIRI())) {
                            OWLClass srvTypeClass = factory.getOWLClass("CEStatus", gluePrefix);
                            manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(srvTypeClass, lrmsTypeInd));
                        }
                        manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(factory.getOWLObjectProperty("hasStatus", gluePrefix), ceState, lrmsTypeInd));
                    }


                    //OWLNamedIndividual cePolicy = factory.getOWLNamedIndividual("CEPolicy." + ceID + "-" + voViewLocalID, currentPrefix);
                    OWLAnonymousIndividual cePolicy = factory.getOWLAnonymousIndividual();
                    manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(factory.getOWLClass("CEPolicy", gluePrefix), cePolicy));
                    manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(factory.getOWLObjectProperty("hasPolicy", gluePrefix), voview, cePolicy));

                    if (attributes.get("GlueCEPolicyAssignedJobSlots") != null) {
                        OWLLiteral literal = factory.getOWLLiteral(attributes.get("GlueCEPolicyAssignedJobSlots").get().toString(), OWL2Datatype.XSD_INTEGER);
                        manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasAssignedJobSlots", gluePrefix), cePolicy, literal));
                    }
                    if (attributes.get("GlueCEPolicyMaxCPUTime") != null) {
                        OWLLiteral literal = factory.getOWLLiteral(attributes.get("GlueCEPolicyMaxCPUTime").get().toString(), OWL2Datatype.XSD_INTEGER);
                        manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasMaxCPUTime", gluePrefix), cePolicy, literal));
                    }
                    if (attributes.get("GlueCEPolicyMaxObtainableCPUTime") != null) {
                        OWLLiteral literal = factory.getOWLLiteral(attributes.get("GlueCEPolicyMaxObtainableCPUTime").get().toString(), OWL2Datatype.XSD_INTEGER);
                        manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasMaxObtainableCPUTime", gluePrefix), cePolicy, literal));
                    }
                    if (attributes.get("GlueCEPolicyMaxObtainableWallClockTime") != null) {
                        OWLLiteral literal = factory.getOWLLiteral(attributes.get("GlueCEPolicyMaxObtainableWallClockTime").get().toString(), OWL2Datatype.XSD_INTEGER);
                        manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasMaxObtainableWallClockTime", gluePrefix), cePolicy, literal));
                    }
                    if (attributes.get("GlueCEPolicyMaxRunningJobs") != null) {
                        OWLLiteral literal = factory.getOWLLiteral(attributes.get("GlueCEPolicyMaxRunningJobs").get().toString(), OWL2Datatype.XSD_INTEGER);
                        manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasMaxRunningJobs", gluePrefix), cePolicy, literal));
                    }
                    if (attributes.get("GlueCEPolicyMaxSlotsPerJob") != null) {
                        OWLLiteral literal = factory.getOWLLiteral(attributes.get("GlueCEPolicyMaxSlotsPerJob").get().toString(), OWL2Datatype.XSD_INTEGER);
                        manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasMaxSlotsPerJob", gluePrefix), cePolicy, literal));
                    }
                    if (attributes.get("GlueCEPolicyMaxTotalJobs") != null) {
                        OWLLiteral literal = factory.getOWLLiteral(attributes.get("GlueCEPolicyMaxTotalJobs").get().toString(), OWL2Datatype.XSD_INTEGER);
                        manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasMaxTotalJobs", gluePrefix), cePolicy, literal));
                    }
                    if (attributes.get("GlueCEPolicyMaxWaitingJobs") != null) {
                        OWLLiteral literal = factory.getOWLLiteral(attributes.get("GlueCEPolicyMaxWaitingJobs").get().toString(), OWL2Datatype.XSD_INTEGER);
                        manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasMaxWaitingJobs", gluePrefix), cePolicy, literal));
                    }
                    if (attributes.get("GlueCEPolicyMaxWallClockTime") != null) {
                        OWLLiteral literal = factory.getOWLLiteral(attributes.get("GlueCEPolicyMaxWallClockTime").get().toString(), OWL2Datatype.XSD_INTEGER);
                        manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasMaxWallClockTime", gluePrefix), cePolicy, literal));
                    }
                    if (attributes.get("GlueCEPolicyPreemption") != null) {
                        OWLLiteral literal = factory.getOWLLiteral(attributes.get("GlueCEPolicyPreemption").get().toString(), OWL2Datatype.XSD_INTEGER);
                        manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasPreemption", gluePrefix), cePolicy, literal));
                    }
                    if (attributes.get("GlueCEPolicyPriority") != null) {
                        OWLLiteral literal = factory.getOWLLiteral(attributes.get("GlueCEPolicyPriority").get().toString(), OWL2Datatype.XSD_INTEGER);
                        manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasPriority", gluePrefix), cePolicy, literal));
                    }

                    if (attributes.get("GlueCEAccessControlBaseRule") != null) {
                        NamingEnumeration ne = attributes.get("GlueCEAccessControlBaseRule").getAll();
                        while (ne.hasMore()) {
                            String[] ACBR = ne.next().toString().split(":");
                            if (ACBR.length == 2) {
                                OWLAnonymousIndividual individual = factory.getOWLAnonymousIndividual();
                                manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(factory.getOWLClass("AccessControlBaseRule", gluePrefix), individual));
                                manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasPrefix", gluePrefix), individual, ACBR[0]));
                                manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasSCN", gluePrefix), individual, ACBR[1]));
                                manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(factory.getOWLObjectProperty("hasAccessControlBaseRule", gluePrefix), voview, individual));
                            } else {
                                //GlueServiceAccessControlBaseRule is wrong!
                            }
                        }
                    }

                }
            }
        }
    }

    public void saveClusters(NamingEnumeration<SearchResult> namingEnumeration) throws NamingException {
        if (namingEnumeration != null) {
            int i = 1;
            while (namingEnumeration.hasMore()) {
                SearchResult result = namingEnumeration.next();
                Attributes attributes = result.getAttributes();

                OWLClass clusterClass = factory.getOWLClass("Cluster", gluePrefix);
                String name = attributes.get("GlueClusterUniqueID").get().toString() + "-cluster";
                OWLNamedIndividual cluster = factory.getOWLNamedIndividual(name, currentPrefix);

                manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(clusterClass, cluster));

                if (attributes.get("GlueClusterUniqueID") != null) {
                    manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasUniqueID", gluePrefix), cluster, attributes.get("GlueClusterUniqueID").get().toString()));
                }
                if (attributes.get("GlueClusterName") != null) {
                    manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasName", gluePrefix), cluster, attributes.get("GlueClusterName").get().toString()));
                }
                if (attributes.get("GlueClusterTmpDir") != null) {
                    manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasTmpDir", gluePrefix), cluster, attributes.get("GlueClusterTmpDir").get().toString()));
                }
                if (attributes.get("GlueClusterWNTmpDir") != null) {
                    manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasWNTmpDir", gluePrefix), cluster, attributes.get("GlueClusterWNTmpDir").get().toString()));
                }

                if (attributes.get("GlueForeignKey") != null) {
                    NamingEnumeration ne = attributes.get("GlueForeignKey").getAll();
                    while (ne.hasMore()) {
                        String[] FK = ne.next().toString().split("=");
                        if (FK.length == 2) {
                            OWLNamedIndividual reference = factory.getOWLNamedIndividual(FK[1].replaceAll(":", "").replaceAll("/", ""), currentPrefix);
                            if (ontology.containsIndividualInSignature(reference.getIRI())) {
                                if (FK[0].equals("GlueCEUniqueID")) {
                                    manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(factory.getOWLObjectProperty("contains", gluePrefix), cluster, reference));
                                } else if (FK[0].equals("GlueSiteUniqueID")) {
                                    manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(factory.getOWLObjectProperty("partOf", gluePrefix), cluster, reference));
                                }
                            }
                        } else {
                            //GlueForeignKey is wrong!
                        }
                    }
                }

            }
        }
    }

    public void saveSubClusters(NamingEnumeration<SearchResult> namingEnumeration) throws NamingException {
        if (namingEnumeration != null) {
            int i = 1;
            while (namingEnumeration.hasMore()) {
                SearchResult result = namingEnumeration.next();
                Attributes attributes = result.getAttributes();

                OWLClass clusterClass = factory.getOWLClass("SubCluster", gluePrefix);
                String name = attributes.get("GlueSubClusterUniqueID").get().toString() + "-subCluster";
                name = name.replaceAll(" ", "_");
                OWLNamedIndividual subCluster = factory.getOWLNamedIndividual(name, currentPrefix);

                manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(clusterClass, subCluster));

                if (attributes.get("GlueSubClusterName") != null) {
                    manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasName", gluePrefix), subCluster, attributes.get("GlueSubClusterName").get().toString()));
                }
                if (attributes.get("GlueSubClusterLogicalCPUs") != null) {
                    OWLLiteral literal = factory.getOWLLiteral(attributes.get("GlueSubClusterLogicalCPUs").get().toString(), OWL2Datatype.XSD_INTEGER);
                    manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasLogicalCPUs", gluePrefix), subCluster, literal));
                }
                if (attributes.get("GlueSubClusterPhysicalCPUs") != null) {
                    OWLLiteral literal = factory.getOWLLiteral(attributes.get("GlueSubClusterPhysicalCPUs").get().toString(), OWL2Datatype.XSD_INTEGER);
                    manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasPhysicalCPUs", gluePrefix), subCluster, literal));
                }
                if (attributes.get("GlueSubClusterTmpDir") != null) {
                    manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasTmpDir", gluePrefix), subCluster, attributes.get("GlueSubClusterTmpDir").get().toString()));
                }
                if (attributes.get("GlueSubClusterWNTmpDir") != null) {
                    manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasWNTmpDir", gluePrefix), subCluster, attributes.get("GlueSubClusterWNTmpDir").get().toString()));
                }

                String[] key = attributes.get("GlueChunkKey").get().toString().split("=");
                OWLNamedIndividual cluster = null;
                if (key.length == 2 && key[0].equals("GlueClusterUniqueID")) {
                    cluster = factory.getOWLNamedIndividual(key[1] + "-cluster", currentPrefix);
                }

                if (ontology.containsIndividualInSignature(cluster.getIRI())) {
                    manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(factory.getOWLObjectProperty("partOf", gluePrefix), subCluster, cluster));
                }


                // EXTRACT HOST INFO

                OWLClass hostClass = factory.getOWLClass("Host", gluePrefix);
                //String hostName = attributes.get("GlueSubClusterUniqueID").get().toString() + "-host";
                //hostName = hostName.replaceAll(" ", "_");
                //OWLNamedIndividual host = factory.getOWLNamedIndividual(hostName, currentPrefix);
                OWLAnonymousIndividual host = factory.getOWLAnonymousIndividual();

                manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(hostClass, host));
                manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(factory.getOWLObjectProperty("describedBy", gluePrefix), subCluster, host));

                NamingEnumeration ne = attributes.get("objectClass").getAll();
                while (ne.hasMore()) {
                    String val = ne.next().toString();
                    if (val.equals("GlueHost")) {
                        manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasUniqueID", gluePrefix), host, attributes.get("GlueHostUniqueId").get().toString()));
                        manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasName", gluePrefix), host, attributes.get("GlueHostName").get().toString()));
                    }
                    if (val.equals("GlueHostArchitecture")) {
                        OWLAnonymousIndividual arch = factory.getOWLAnonymousIndividual();
                        manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(factory.getOWLClass("Architecture", gluePrefix), arch));
                        if (attributes.get("GlueHostArchitecturePlatformType") != null) {
                            manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasPlatformType", gluePrefix), arch, attributes.get("GlueHostArchitecturePlatformType").get().toString()));
                        }
                        if (attributes.get("GlueHostArchitectureSMPSize") != null) {
                            OWLLiteral value = factory.getOWLLiteral(attributes.get("GlueHostArchitectureSMPSize").get().toString(), OWL2Datatype.XSD_INTEGER);
                            manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasSMPSize", gluePrefix), arch, value));
                        }
                        manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(factory.getOWLObjectProperty("describedBy", gluePrefix), host, arch));
                    }
                    if (val.equals("GlueHostProcessor")) {
                        OWLAnonymousIndividual proc = factory.getOWLAnonymousIndividual();
                        manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(factory.getOWLClass("Processor", gluePrefix), proc));
                        manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(factory.getOWLObjectProperty("describedBy", gluePrefix), host, proc));

                        if (attributes.get("GlueHostProcessorVendor") != null) {
                            manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasVendor", gluePrefix), proc, attributes.get("GlueHostProcessorVendor").get().toString()));
                        }
                        if (attributes.get("GlueHostProcessorModel") != null) {
                            manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasModel", gluePrefix), proc, attributes.get("GlueHostProcessorModel").get().toString()));
                        }
                        if (attributes.get("GlueHostProcessorVersion") != null) {
                            manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasVersion", gluePrefix), proc, attributes.get("GlueHostProcessorVersion").get().toString()));
                        }
                        if (attributes.get("GlueHostProcessorInstructionSet") != null) {
                            manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasInstructionSet", gluePrefix), proc, attributes.get("GlueHostProcessorInstructionSet").get().toString()));
                        }
                        if (attributes.get("GlueHostProcessorOtherProcessorDescription") != null) {
                            manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasDescription", gluePrefix), proc, attributes.get("GlueHostProcessorOtherProcessorDescription").get().toString()));
                        }
                        if (attributes.get("GlueHostProcessorClockSpeed") != null) {
                            OWLLiteral value = factory.getOWLLiteral(attributes.get("GlueHostProcessorClockSpeed").get().toString(), OWL2Datatype.XSD_INTEGER);
                            manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasClockSpeed", gluePrefix), proc, value));
                        }
                        if (attributes.get("GlueHostProcessorCacheL1") != null) {
                            OWLLiteral value = factory.getOWLLiteral(attributes.get("GlueHostProcessorCacheL1").get().toString(), OWL2Datatype.XSD_INTEGER);
                            manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasCacheL1", gluePrefix), proc, value));
                        }
                        if (attributes.get("GlueHostProcessorCacheL1I") != null) {
                            OWLLiteral value = factory.getOWLLiteral(attributes.get("GlueHostProcessorCacheL1I").get().toString(), OWL2Datatype.XSD_INTEGER);
                            manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasCacheL1I", gluePrefix), proc, value));
                        }
                        if (attributes.get("GlueHostProcessorCacheL1D") != null) {
                            OWLLiteral value = factory.getOWLLiteral(attributes.get("GlueHostProcessorCacheL1D").get().toString(), OWL2Datatype.XSD_INTEGER);
                            manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasCacheL1D", gluePrefix), proc, value));
                        }
                        if (attributes.get("GlueHostProcessorCacheL2") != null) {
                            OWLLiteral value = factory.getOWLLiteral(attributes.get("GlueHostProcessorCacheL2").get().toString(), OWL2Datatype.XSD_INTEGER);
                            manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasCacheL2", gluePrefix), proc, value));
                        }

                    }
                    if (val.equals("GlueHostApplicationSoftware")) {
                        OWLAnonymousIndividual app = factory.getOWLAnonymousIndividual();
                        manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(factory.getOWLClass("ApplicationSoftware", gluePrefix), app));
                        manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(factory.getOWLObjectProperty("describedBy", gluePrefix), host, app));

                        if (attributes.get("GlueHostApplicationSoftwareRunTimeEnvironment") != null) {
                            NamingEnumeration next = attributes.get("GlueHostApplicationSoftwareRunTimeEnvironment").getAll();
                            while (next.hasMore()) {
                                manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasRunTimeEnvironment", gluePrefix), app, next.next().toString()));
                            }
                        }
                    }
                    if (val.equals("GlueHostMainMemory")) {
                        OWLAnonymousIndividual mem = factory.getOWLAnonymousIndividual();
                        manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(factory.getOWLClass("MainMemory", gluePrefix), mem));
                        manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(factory.getOWLObjectProperty("describedBy", gluePrefix), host, mem));

                        if (attributes.get("GlueHostMainMemoryRAMSize") != null) {
                            OWLLiteral value = factory.getOWLLiteral(attributes.get("GlueHostMainMemoryRAMSize").get().toString(), OWL2Datatype.XSD_INTEGER);
                            manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasRAMSize", gluePrefix), mem, value));
                        }
                        if (attributes.get("GlueHostMainMemoryRAMAvailable") != null) {
                            OWLLiteral value = factory.getOWLLiteral(attributes.get("GlueHostMainMemoryRAMAvailable").get().toString(), OWL2Datatype.XSD_INTEGER);
                            manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasRAMAvailable", gluePrefix), mem, value));
                        }
                        if (attributes.get("GlueHostMainMemoryVirtualSize") != null) {
                            OWLLiteral value = factory.getOWLLiteral(attributes.get("GlueHostMainMemoryVirtualSize").get().toString(), OWL2Datatype.XSD_INTEGER);
                            manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasVirtualSize", gluePrefix), mem, value));
                        }
                        if (attributes.get("GlueHostMainMemoryVirtualAvailable") != null) {
                            OWLLiteral value = factory.getOWLLiteral(attributes.get("GlueHostMainMemoryVirtualAvailable").get().toString(), OWL2Datatype.XSD_INTEGER);
                            manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasVirtualAvailable", gluePrefix), mem, value));
                        }
                    }
                    if (val.equals("GlueHostBenchmark")) {
                        OWLAnonymousIndividual bechmark = factory.getOWLAnonymousIndividual();
                        manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(factory.getOWLClass("Benchmark", gluePrefix), bechmark));
                        manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(factory.getOWLObjectProperty("describedBy", gluePrefix), host, bechmark));

                        if (attributes.get("GlueHostBenchmarkSI00") != null) {
                            OWLLiteral value = factory.getOWLLiteral(attributes.get("GlueHostBenchmarkSI00").get().toString(), OWL2Datatype.XSD_INTEGER);
                            manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasSI00", gluePrefix), bechmark, value));
                        }
                        if (attributes.get("GlueHostBenchmarkSF00") != null) {
                            OWLLiteral value = factory.getOWLLiteral(attributes.get("GlueHostBenchmarkSF00").get().toString(), OWL2Datatype.XSD_INTEGER);
                            manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasSF00", gluePrefix), bechmark, value));
                        }
                    }
                    if (val.equals("GlueHostNetworkAdapter")) {
                        OWLAnonymousIndividual net = factory.getOWLAnonymousIndividual();
                        manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(factory.getOWLClass("NetworkAdapter", gluePrefix), net));
                        manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(factory.getOWLObjectProperty("describedBy", gluePrefix), host, net));

                        if (attributes.get("GlueHostNetworkAdapterName") != null) {
                            OWLLiteral value = factory.getOWLLiteral(attributes.get("GlueHostNetworkAdapterName").get().toString(), OWL2Datatype.XSD_STRING);
                            manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasName", gluePrefix), net, value));
                        }

                        if (attributes.get("GlueHostNetworkAdapterIPAddress") != null) {
                            OWLLiteral value = factory.getOWLLiteral(attributes.get("GlueHostNetworkAdapterIPAddress").get().toString(), OWL2Datatype.XSD_STRING);
                            manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasIPAddress", gluePrefix), net, value));
                        }

                        if (attributes.get("GlueHostNetworkAdapterMTU") != null) {
                            OWLLiteral value = factory.getOWLLiteral(attributes.get("GlueHostNetworkAdapterMTU").get().toString(), OWL2Datatype.XSD_INTEGER);
                            manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasMTU", gluePrefix), net, value));
                        }

                        if (attributes.get("GlueHostNetworkAdapterOutboundIP") != null) {
                            boolean value = Boolean.parseBoolean(attributes.get("GlueHostNetworkAdapterOutboundIP").get().toString());
                            manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasOutboundIP", gluePrefix), net, value));
                        }

                        if (attributes.get("GlueHostNetworkAdapterInboundIP") != null) {
                            boolean value = Boolean.parseBoolean(attributes.get("GlueHostNetworkAdapterInboundIP").get().toString());
                            manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasInboundIP", gluePrefix), net, value));
                        }
                    }
                    if (val.equals("GlueHostProcessorLoad")) {
                        //TODO - not used interface CERN
                    }
                    if (val.equals("GlueHostSMPLoad")) {
                        //TODO - not used interface CERN
                    }
                    if (val.equals("GlueHostOperatingSystem")) {
                        OWLAnonymousIndividual os = factory.getOWLAnonymousIndividual();
                        manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(factory.getOWLClass("OperatingSystem", gluePrefix), os));
                        manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(factory.getOWLObjectProperty("describedBy", gluePrefix), host, os));

                        if (attributes.get("GlueHostOperatingSystemOSName") != null) {
                            manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasName", gluePrefix), os, attributes.get("GlueHostOperatingSystemOSName").get().toString()));
                        }
                        if (attributes.get("GlueHostOperatingSystemOSRelease") != null) {
                            manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasRelease", gluePrefix), os, attributes.get("GlueHostOperatingSystemOSRelease").get().toString()));
                        }
                        if (attributes.get("GlueHostOperatingSystemOSVersion") != null) {
                            manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasVersion", gluePrefix), os, attributes.get("GlueHostOperatingSystemOSVersion").get().toString()));
                        }
                    }
                    if (val.equals("GlueHostLocalFileSystem")) {
                        //TODO - not used interface CERN
                    }
                    if (val.equals("GlueHostRemoteFileSystem")) {
                        //TODO - not used interface CERN
                    }
                    if (val.equals("GlueHostStorageDevice")) {
                        //TODO - not used interface CERN
                    }
                    if (val.equals("GlueHostFile")) {
                        //TODO - not used interface CERN
                    }
                }

            }
        }
    }

    public void saveSEs(NamingEnumeration<SearchResult> namingEnumeration) throws NamingException {
        if (namingEnumeration != null) {
            int i = 1;
            while (namingEnumeration.hasMore()) {
                SearchResult result = namingEnumeration.next();
                Attributes attributes = result.getAttributes();

                OWLClass seClass = factory.getOWLClass("StorageElement", gluePrefix);
                String name = attributes.get("GlueSEUniqueID").get().toString();
                name = name.replaceAll(":", "").replaceAll("/", "");
                OWLNamedIndividual se = factory.getOWLNamedIndividual(name, currentPrefix);

                if (!ontology.containsIndividualInSignature(se.getIRI())) {

                    manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(seClass, se));
                    manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasUniqueID", gluePrefix), se, attributes.get("GlueSEUniqueID").get().toString()));

                    if (attributes.get("GlueSEArchitecture") != null) {
                        OWLNamedIndividual seArchInd = factory.getOWLNamedIndividual(attributes.get("GlueSEArchitecture").get().toString(), currentPrefix);
                        if (!ontology.containsIndividualInSignature(seArchInd.getIRI())) {
                            OWLClass seArchClass = factory.getOWLClass("SEArchitecture", gluePrefix);
                            manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(seArchClass, seArchInd));
                        }
                        manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(factory.getOWLObjectProperty("hasArchitecture", gluePrefix), se, seArchInd));
                    }

                    if (attributes.get("GlueSEImplementationName") != null && attributes.get("GlueSEImplementationVersion") != null) {
                        OWLAnonymousIndividual individual = factory.getOWLAnonymousIndividual();
                        manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(factory.getOWLClass("Implementation", gluePrefix), individual));
                        manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasImplementationName", gluePrefix), individual, attributes.get("GlueSEImplementationName").get().toString()));
                        manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasImplementationVersion", gluePrefix), individual, attributes.get("GlueSEImplementationVersion").get().toString()));
                        manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(factory.getOWLObjectProperty("hasImplementation", gluePrefix), se, individual));
                    }

                    if (attributes.get("GlueSEName") != null) {
                        manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasName", gluePrefix), se, attributes.get("GlueSEName").get().toString()));
                    }

                    if (attributes.get("GlueSETotalOnlineSize") != null) {
                        String temp = attributes.get("GlueSETotalOnlineSize").get().toString();
                        try {
                            int val = Integer.parseInt(temp);
                            manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasTotalOnlineSize", gluePrefix), se, val));
                        } catch (NumberFormatException ex) {
                        }
                    }
                    if (attributes.get("GlueSEUsedOnlineSize") != null) {
                        String temp = attributes.get("GlueSEUsedOnlineSize").get().toString();
                        try {
                            int val = Integer.parseInt(temp);
                            manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasUsedOnlineSize", gluePrefix), se, val));
                        } catch (NumberFormatException ex) {
                        }
                    }
                    if (attributes.get("GlueSETotalNearlineSize") != null) {
                        String temp = attributes.get("GlueSETotalNearlineSize").get().toString();
                        try {
                            int val = Integer.parseInt(temp);
                            manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasTotalNearlineSize", gluePrefix), se, val));
                        } catch (NumberFormatException ex) {
                        }
                    }
                    if (attributes.get("GlueSEUsedNearlineSize") != null) {
                        String temp = attributes.get("GlueSEUsedNearlineSize").get().toString();
                        try {
                            int val = Integer.parseInt(temp);
                            manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasUsedNearlineSize", gluePrefix), se, val));
                        } catch (NumberFormatException ex) {
                        }
                    }
                    if (attributes.get("GlueInformationServiceURL") != null) {
                        manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasInformationServiceURL", gluePrefix), se, attributes.get("GlueInformationServiceURL").get().toString()));
                    }

                    if (attributes.get("GlueSEStatus") != null) {
                        String value = attributes.get("GlueSEStatus").get().toString();
                        value = value.replaceAll(" ", "_");
                        OWLNamedIndividual seStatusInd = factory.getOWLNamedIndividual(value, currentPrefix);
                        if (!ontology.containsIndividualInSignature(seStatusInd.getIRI())) {
                            OWLClass seStatusClass = factory.getOWLClass("SEStatus", gluePrefix);
                            manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(seStatusClass, seStatusInd));
                        }
                        manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(factory.getOWLObjectProperty("hasStatus", gluePrefix), se, seStatusInd));
                    }

                    if (attributes.get("GlueForeignKey") != null) {
                        NamingEnumeration ne = attributes.get("GlueForeignKey").getAll();
                        while (ne.hasMore()) {
                            String[] FK = ne.next().toString().split("=");
                            if (FK.length == 2) {
                                OWLNamedIndividual reference = factory.getOWLNamedIndividual(FK[1].replaceAll(":", "").replaceAll("/", ""), currentPrefix);
                                if (ontology.containsIndividualInSignature(reference.getIRI())) {
                                    if (FK[0].equals("GlueSiteUniqueID")) {
                                        manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(factory.getOWLObjectProperty("partOf", gluePrefix), se, reference));
                                    }
                                }
                            } else {
                                //GlueForeignKey is wrong!
                            }
                        }
                    }
                }
            }
        }
    }

    public void saveSites(NamingEnumeration<SearchResult> namingEnumeration) throws NamingException {
        if (namingEnumeration != null) {
            int i = 1;
            while (namingEnumeration.hasMore()) {
                SearchResult result = namingEnumeration.next();
                Attributes attributes = result.getAttributes();

                OWLClass siteClass = factory.getOWLClass("Site", gluePrefix);
                OWLNamedIndividual site = factory.getOWLNamedIndividual(attributes.get("GlueSiteUniqueID").get().toString(), currentPrefix);

                if (!ontology.containsIndividualInSignature(site.getIRI())) {

                    manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(siteClass, site));
                    manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasUniqueID", gluePrefix), site, attributes.get("GlueSiteUniqueID").get().toString()));
                    manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasName", gluePrefix), site, attributes.get("GlueSiteName").get().toString()));
                    manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasWeb", gluePrefix), site, attributes.get("GlueSiteWeb").get().toString()));
                    manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasDescription", gluePrefix), site, attributes.get("GlueSiteDescription").get().toString()));

                    if (attributes.get("GlueSiteOtherInfo") != null) {
                        NamingEnumeration ne = attributes.get("GlueSiteOtherInfo").getAll();
                        while (ne.hasMore()) {
                            manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasOtherInfo", gluePrefix), site, ne.next().toString()));
                        }
                    }

                    //TODO SPONSOR!!

                    OWLAnonymousIndividual loc = factory.getOWLAnonymousIndividual();
                    manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(factory.getOWLClass("Location", gluePrefix), loc));
                    manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasName", gluePrefix), loc, attributes.get("GlueSiteLocation").get().toString()));
                    if (attributes.get("GlueSiteLatitude") != null) {
//                    OWLLiteral latitude = factory.getOWLLiteral(attributes.get("GlueSiteLatitude").get().toString(), OWL2Datatype.XSD_FLOAT);
                        OWLLiteral latitude = factory.getOWLLiteral(attributes.get("GlueSiteLatitude").get().toString(), OWL2Datatype.XSD_STRING);
                        manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasLatitude", gluePrefix), loc, latitude));
                    }
                    if (attributes.get("GlueSiteLongitude") != null) {
//                    OWLLiteral longitude = factory.getOWLLiteral(attributes.get("GlueSiteLongitude").get().toString(), OWL2Datatype.XSD_FLOAT);
                        OWLLiteral longitude = factory.getOWLLiteral(attributes.get("GlueSiteLongitude").get().toString(), OWL2Datatype.XSD_STRING);
                        manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasLongitude", gluePrefix), loc, longitude));
                    }
                    manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(factory.getOWLObjectProperty("hasLocation", gluePrefix), site, loc));
                    if (attributes.get("GlueSiteEmailContact") != null) {
                        OWLAnonymousIndividual individual = factory.getOWLAnonymousIndividual();
                        manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(factory.getOWLClass("EmailContact", gluePrefix), individual));
                        manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasName", gluePrefix), individual, attributes.get("GlueSiteEmailContact").get().toString()));
                        manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(factory.getOWLObjectProperty("hasContact", gluePrefix), site, individual));
                    }
                    if (attributes.get("GlueSiteSecurityContact") != null) {
                        OWLAnonymousIndividual individual = factory.getOWLAnonymousIndividual();
                        manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(factory.getOWLClass("SecurityContact", gluePrefix), individual));
                        manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasName", gluePrefix), individual, attributes.get("GlueSiteSecurityContact").get().toString()));
                        manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(factory.getOWLObjectProperty("hasContact", gluePrefix), site, individual));
                    }
                    if (attributes.get("GlueSiteSysAdminContact") != null) {
                        OWLAnonymousIndividual individual = factory.getOWLAnonymousIndividual();
                        manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(factory.getOWLClass("SysAdminContact", gluePrefix), individual));
                        manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasName", gluePrefix), individual, attributes.get("GlueSiteSysAdminContact").get().toString()));
                        manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(factory.getOWLObjectProperty("hasContact", gluePrefix), site, individual));
                    }
                    if (attributes.get("GlueSiteUserSupportContact") != null) {
                        OWLAnonymousIndividual individual = factory.getOWLAnonymousIndividual();
                        manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(factory.getOWLClass("UserSupportContact", gluePrefix), individual));
                        manager.addAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("hasName", gluePrefix), individual, attributes.get("GlueSiteUserSupportContact").get().toString()));
                        manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(factory.getOWLObjectProperty("hasContact", gluePrefix), site, individual));
                    }
                }
            }
        }
    }
}
