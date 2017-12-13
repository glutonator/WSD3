package ontology;

import jade.content.onto.BasicOntology;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.schema.ConceptSchema;
import jade.content.schema.ObjectSchema;
import jade.content.schema.PrimitiveSchema;



public class ParametersOntology extends Ontology {

    public static final String VEHICLEPARAMETERS = "VehicleParameters";
    public static final String X = "x";
    public static final String Y = "y";
    public static final String SPEED = "speed";
    public static final String MAX_SPEED = "max_speed";
    public static final String ACCELERATION = "acceleration";

    //nowe
    public static final String SIGNPARAMETERS = "SignParameters";
    public static final String X_SIGN = "x";
    public static final String Y_BEGIN = "y_begin";
    public static final String Y_END = "y_end";
    public static final String LIMIT_MAX_SPEED = "limit_max_speed";


    public static final String NAME = "parameters-ontology";

    public static Ontology instance = new ParametersOntology();

    public ParametersOntology() {
        super(NAME, BasicOntology.getInstance());
        try {

            add(new ConceptSchema(VEHICLEPARAMETERS), VehicleParameters.class);

            ConceptSchema cs = (ConceptSchema)getSchema(VEHICLEPARAMETERS);
            cs.add(X, (PrimitiveSchema)getSchema(BasicOntology.INTEGER));
            cs.add(Y, (PrimitiveSchema)getSchema(BasicOntology.INTEGER), ObjectSchema.OPTIONAL);
            cs.add(SPEED, (PrimitiveSchema)getSchema(BasicOntology.INTEGER));
            cs.add(MAX_SPEED, (PrimitiveSchema)getSchema(BasicOntology.INTEGER));
            cs.add(ACCELERATION, (PrimitiveSchema)getSchema(BasicOntology.INTEGER));

            //nowe
            //czemu sa one opcjonalne??
            add(new ConceptSchema(SIGNPARAMETERS), SignParameters.class);
            ConceptSchema cs2 = (ConceptSchema)getSchema(SIGNPARAMETERS);
            cs2.add(X_SIGN, (PrimitiveSchema)getSchema(BasicOntology.INTEGER));
            cs2.add(Y_BEGIN, (PrimitiveSchema)getSchema(BasicOntology.INTEGER), ObjectSchema.OPTIONAL);
            cs2.add(Y_END, (PrimitiveSchema)getSchema(BasicOntology.INTEGER), ObjectSchema.OPTIONAL);
            cs2.add(LIMIT_MAX_SPEED, (PrimitiveSchema)getSchema(BasicOntology.INTEGER));
        }
        catch(OntologyException oe) {
            oe.printStackTrace();
        }

    }

    public static Ontology getInstance() {
        return instance;
    }


}
