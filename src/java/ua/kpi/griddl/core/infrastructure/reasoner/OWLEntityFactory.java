package ua.kpi.griddl.core.infrastructure.reasoner;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import org.semanticweb.owlapi.vocab.XSDVocabulary;

/**
 *
 * @author Pospishniy Olexandr <pospishniy@kpi.in.ua>
 */
public class OWLEntityFactory implements OWLEntityChecker {

    private final OWLOntology ontology;
    private final OWLDataFactory factory;
    private Map<String, OWLClass> owlClassCache = new HashMap<String, OWLClass>(100);
    private Map<String, OWLObjectProperty> owlObjectPropertyCache = new HashMap<String, OWLObjectProperty>(100);
    private Map<String, OWLDataProperty> owlDataPropertyCache = new HashMap<String, OWLDataProperty>(100);
    private Map<String, OWLAnnotationProperty> owlAnnotationPropertyCache = new HashMap<String, OWLAnnotationProperty>(100);
    private Map<String, OWLNamedIndividual> owlIndividualCache = new HashMap<String, OWLNamedIndividual>(500);
    private Map<String, OWLDatatype> owlDatatypeCache = new HashMap<String, OWLDatatype>(50);

    public OWLEntityFactory(OWLOntology ontology, OWLDataFactory factory) {
        this.ontology = ontology;
        this.factory = factory;
        buildCache();
    }

    private void buildCache() {
        OWLClass thing = factory.getOWLThing();
        owlClassCache.put(render(thing.getIRI()), thing);
        OWLClass nothing = factory.getOWLNothing();
        owlClassCache.put(render(nothing.getIRI()), nothing);

        for (OWLClass cls : ontology.getClassesInSignature(true)) {
            addRendering(cls, owlClassCache);
        }
        for (OWLObjectProperty prop : ontology.getObjectPropertiesInSignature(true)) {
            addRendering(prop, owlObjectPropertyCache);
        }
        for (OWLDataProperty prop : ontology.getDataPropertiesInSignature(true)) {
            addRendering(prop, owlDataPropertyCache);
        }
        for (OWLIndividual ind : ontology.getIndividualsInSignature(true)) {
            if (!ind.isAnonymous()) {
                addRendering(ind.asOWLNamedIndividual(), owlIndividualCache);
            }
        }
        for (OWLAnnotationProperty prop : ontology.getAnnotationPropertiesInSignature()) {
            addRendering(prop, owlAnnotationPropertyCache);
        }

        for (IRI uri : OWLRDFVocabulary.BUILT_IN_ANNOTATION_PROPERTY_IRIS) {
            addRendering(factory.getOWLAnnotationProperty(uri), owlAnnotationPropertyCache);
        }

        for (OWLDatatype dt : getKnownDatatypes(ontology)) {
            addRendering(dt, owlDatatypeCache);
        }
    }

    private <T extends OWLEntity> void addRendering(T entity, Map<String, T> map) {
        String rendering = render(entity.getIRI());
        map.put(rendering, entity);
    }

    private String render(IRI iri) {
        try {
            String rendering = iri.getFragment();
            if (rendering == null) {
                String path = iri.toURI().getPath();
                if (path == null) {
                    return iri.toQuotedString();
                }
                return iri.toURI().getPath().substring(path.lastIndexOf("/") + 1);
            }
            return getEscapedRendering(rendering);
        } catch (Exception e) {
            return "<Error! " + e.getMessage() + ">";
        }
    }

    private static String getEscapedRendering(String originalRendering) {
        if (originalRendering.indexOf(' ') != -1 || originalRendering.indexOf('(') != -1 || originalRendering.indexOf(')') != -1) {
            return "'" + originalRendering + "'";
        } else {
            return originalRendering;
        }
    }

    private Set<OWLDatatype> getBuiltinDatatypes() {
        Set<OWLDatatype> datatypes = new HashSet<OWLDatatype>();

        datatypes.add(factory.getTopDatatype());
        for (XSDVocabulary dt : XSDVocabulary.values()) {
            datatypes.add(factory.getOWLDatatype(dt.getIRI()));
        }
        datatypes.add(factory.getOWLDatatype(OWLRDFVocabulary.RDF_XML_LITERAL.getIRI()));

        return datatypes;
    }

    private Set<OWLDatatype> getKnownDatatypes(OWLOntology ont) {
        Set<OWLDatatype> knownTypes = getBuiltinDatatypes();
        knownTypes.addAll(ont.getDatatypesInSignature());
        return knownTypes;
    }

    @Override
    public OWLClass getOWLClass(String name) {
        return owlClassCache.get(name);
    }

    @Override
    public OWLObjectProperty getOWLObjectProperty(String name) {
        return owlObjectPropertyCache.get(name);
    }

    @Override
    public OWLDataProperty getOWLDataProperty(String name) {
        return owlDataPropertyCache.get(name);
    }

    @Override
    public OWLNamedIndividual getOWLIndividual(String name) {
        return owlIndividualCache.get(name);
    }

    @Override
    public OWLDatatype getOWLDatatype(String name) {
        return owlDatatypeCache.get(name);
    }

    @Override
    public OWLAnnotationProperty getOWLAnnotationProperty(String name) {
        return owlAnnotationPropertyCache.get(name);
    }
}
