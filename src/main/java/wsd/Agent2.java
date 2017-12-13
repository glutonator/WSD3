package wsd;

import app.CarsApplication;
import graphics.GUIApp;
import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import ontology.ParametersOntology;
import ontology.SignParameters;
import ontology.VehicleParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


public class Agent2 extends Agent {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    VehicleParameters myPArameters;
    VehicleParameters otherPArameters;
    //nowe
    SignParameters mySignParameters;
    //
    Ontology ontology = ParametersOntology.getInstance();

    private static final String AGENT_TYPE = "vehicle_agent";

    private HashMap<AID, VehicleParameters> otherCarsParams = new HashMap<>();
    private HashMap<AID, SignParameters> allSignsParams = new HashMap<>();

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args.length != 2)
            throw new IllegalStateException("Needs more arguments");
        Long speed = Long.parseLong(args[0].toString().split(":")[1]);
        Long MaxSpeed = Long.parseLong(args[1].toString().split(":")[1]);
        System.out.println("Utworzono Agenta: " + getName() + ", Predkosc: " + speed);
        getContentManager().registerLanguage(new SLCodec(), FIPANames.ContentLanguage.FIPA_SL0);
        getContentManager().registerOntology(ontology);

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(AGENT_TYPE);
        sd.setName(getName());
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        myPArameters = new VehicleParameters(speed, MaxSpeed);

        addBehaviour(new Receiver());
        //  addBehaviour(new CreateNewCar(this, 3000));
        addBehaviour(new UpdateParameters(this, 100));

        GUIApp.onSetup(getAID(), myPArameters.getX());
    }

    class CreateNewCar extends WakerBehaviour {

        public CreateNewCar(Agent a, int period) {
            super(a, period);
        }

        @Override
        protected void handleElapsedTimeout() {
            String[] args = {"speed:100"};
            try {
                AgentController ac = getContainerController().createNewAgent("SzybszyAgent", Agent2.class.getName(), args);
                ac.start();
            } catch (StaleProxyException e) {
                e.printStackTrace();
            }
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            AID receiver = new AID("SzybszyAgent", AID.ISLOCALNAME);
            msg.addReceiver(receiver);
            msg.setLanguage(FIPANames.ContentLanguage.FIPA_SL0);
            msg.setOntology(ParametersOntology.NAME);
            try {
                myAgent.getContentManager().fillContent(msg, new Action(receiver, myPArameters));
            } catch (Codec.CodecException | OntologyException e) {
                e.printStackTrace();
            }
            send(msg);

        }
    }


    class UpdateParameters extends TickerBehaviour {

        public UpdateParameters(Agent a, int period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            Boolean onLastLane = myPArameters.getX() == 2L;
            VehicleParameters przed = null;
            AID przedAID = null;
            Long przedNum = 9999999L;
            VehicleParameters przedObok = null;
            Long przedObokNum = 9999999L;
            VehicleParameters za = null;
            Long zaNum = 9999999L;
            VehicleParameters zaObok = null;
            Long zaObokNum = 9999999L;
            VehicleParameters obok = null;

            //tutaj for mo calej mapie znakow
            //interesuje nas znak o pozycji Y najblizszej naszej
            SignParameters defaultSign = new SignParameters(0L, 9999999L,9999999L );
            SignParameters closedSign=defaultSign;
            SignParameters closedprzedSign=defaultSign;
            Long diffminietySign = -9999999L;
            Long diffprzedSign = 9999999L;

            //przyjete zalozenie - zakresy znakow nie moga na siebie nachodzic
            for (Map.Entry<AID, SignParameters> entry : allSignsParams.entrySet()) {
                AID aid = entry.getKey();
                SignParameters param = entry.getValue();
                Long diff = param.getY_begin() - myPArameters.getY();

                if(diff<=0) {
                    //jesli mniejsze od 0 lub rowne 0 to znak jest juz miniety przez samochod = obowiazuje
                    //im wartosc bardziej blizsza 0 tym bardzieje aktualny znak - czyli potrzeba sprawdzać ktory znak ma najwikeszy diff
                    //ten bedzie obowiazywac
                    if(diff>diffminietySign) {
                        Long diffToEnd = param.getY_end() - myPArameters.getY();

                        if(diffToEnd>0) {
                            //jesli koniec znaku jest jeszcze nie miniety
                            diffminietySign = diff;
                            closedSign = param;
                        }
                        else {
                            //znak jest miniety nie obowiazuje
                            //closedSign bez zmian, domyslny znak wciaz obowiazuje
                        }
                    }
                }
                else {
                    //jesli wieksze od 0 to znak jest jeszcze nie miniety przez samochod = nie obowiazuje
                    //zostaje poprzedni
                    //trzeba zainicjowac jakims znakeim domyslnym na wypadek gdyby wszystkie znaki byly przed
                    if(diff<diffprzedSign) {
                        //jesli znak jest przed autem i blizej niz poprzedni zapisany to zapisz go
                        diffprzedSign=diff;
                        closedprzedSign = param;
                    }
                    else {
                        //nothing
                    }
                }

            }
            myPArameters.set_max_speed_of_sign(closedSign.getLimit_max_speed());
            System.out.println("dane znaku najblizszego za autem o nazwie  " + getName()+ "pamrametry:" +  closedSign.getY_begin() +"  "+closedSign.getY_end()+ "  "+closedSign.getLimit_max_speed());
            System.out.println("dane znaku najblizszego przed autem  " + getName() + "pamrametry:" + closedprzedSign.getY_begin() +"  "+closedprzedSign.getY_end()+ "  "+closedprzedSign.getLimit_max_speed());

            Boolean signCloseBy = false;
            Long minimal_distant = 100L;
            //test czy znak jest wystarczajaco blisko by zaczac zwalniac
            Long temp = closedprzedSign.getY_begin()-myPArameters.getY();
            if((temp <minimal_distant)&&(temp >0)) {
                //trzeba zaczac zwalniac
                signCloseBy=true;
            }
            else {
                //nie trzeba zwalniac
            }

                //TODO: znlesc najblizsze mnie samochody i zobaczyć czy mogę zmienić na pas pierwszy
            for (Map.Entry<AID, VehicleParameters> entry : otherCarsParams.entrySet()) {

                AID aid = entry.getKey();
                VehicleParameters param = entry.getValue();
                Long diff = param.getY() - myPArameters.getY();
                if (Objects.equals(param.getX(), myPArameters.getX())) {
                    if (diff < przedNum && diff >= 0) {
                        przedNum = diff;
                        przed = param;
                        przedAID = aid;
                    }
                    if (-diff < zaNum && diff < 0) {
                        zaNum = myPArameters.getY() - param.getY();
                        za = param;
                    }
                    if (diff == 0) {
                        log.error("Samochody w tym samym miejscu");
                    }
                } else {
                    if (diff < przedObokNum && diff >= 0) {
                        przedObokNum = diff;
                        przedObok = param;
                    }
                    if (-diff < zaObokNum && diff < 0) {
                        zaObokNum = -diff;
                        zaObok = param;
                    }
                    if (diff == 0) {
                        log.error("Dziwny przypadek");
                    }
                }
            }

            Boolean canChangeLane = false;

            if(przedObok == null){
                if(zaObok == null){
                    canChangeLane = true;
                }else if(myPArameters.getSpeed()>= zaObok.getSpeed()){
                    canChangeLane = myPArameters.getY() - zaObok.getY() - 2 * zaObok.getSpeed() >= 0;

                }else{
                    canChangeLane = myPArameters.getY() - zaObok.getY() - 3 * zaObok.getSpeed() >= 0;
                }
            }else{
                if(zaObok == null) {

                    if (myPArameters.getSpeed() < przedObok.getSpeed()) {
                        canChangeLane = przedObok.getY() - myPArameters.getY() - 2 * myPArameters.getSpeed() >= 0;

                    } else {
                        canChangeLane = przedObok.getY() - myPArameters.getY() - 3 * myPArameters.getSpeed() >= 0;
                    }
                }else{

                    if(
                            myPArameters.getY() - zaObok.getY() - 3 * zaObok.getSpeed() >= 0 &&
                                    przedObok.getY() - myPArameters.getY() - 3 * myPArameters.getSpeed() >= 0
                            ){
                        canChangeLane = true;
                    }
                }
            }



            Boolean canMoveOn = false;

            if(przed == null){
                canMoveOn = true;
            }else{
                if(przed.getSpeed() >= myPArameters.getSpeed()){
                    canMoveOn = true;
                }else if (przed.getY() - myPArameters.getY() - 3 * myPArameters.getSpeed() >= 0) {
                    canMoveOn = true;
                }
            }


            Long timeInterval = 10L;


            if (!onLastLane) {
                if(przed == null ||canMoveOn){
                    if (signCloseBy==true) {
                        if (myPArameters.getSpeed() >= closedprzedSign.getLimit_max_speed()) {
                            //jesli trzeba zwolinic bo aktualna predkosc jest wieksza od tej na zblizajacym sie znaku
                            myPArameters.addPercentageAcceleration(-10L);
                            myPArameters.updateSpeed();
                            myPArameters.updateY(timeInterval);
                        }
                        else {
                            //dostosowywanie predkosi gdy jest przed znakiem

                            myPArameters.setAcceleration(0L);
                            if (myPArameters.getSpeed() < closedprzedSign.getLimit_max_speed()) {
                                myPArameters.addPercentageAcceleration(10L);
                                myPArameters.updateSpeed();

                                //myPArameters.setSpeed(myPArameters.getMax_speed());
                                //myPArameters.setAcceleration(0L);
                                myPArameters.updateY(timeInterval);

                            }
                            else {
                                myPArameters.setAcceleration(0L);
                                myPArameters.updateY(timeInterval);
                            }
                            //myPArameters.updateY(timeInterval);
                        }
                    }
                    else {

                        if (myPArameters.getSpeed() >= myPArameters.getMax_speed()) {

                            myPArameters.setSpeed(myPArameters.getMax_speed());
                            myPArameters.setAcceleration(0L);
                            myPArameters.updateY(timeInterval);

                        } else {
                                myPArameters.addPercentageAcceleration(10L);
                                myPArameters.updateSpeed();
                                myPArameters.updateY(timeInterval);
                            }
                        }

                }else{

                    if(canChangeLane){

                        if(myPArameters.getSpeed()>=myPArameters.getMax_speed()){

                            myPArameters.setSpeed(myPArameters.getMax_speed());
                            myPArameters.setAcceleration(0L);

                        }else{

                            myPArameters.addPercentageAcceleration(10L);
                            myPArameters.updateSpeed();

                        }

                        myPArameters.setX(2L);
                        myPArameters.updateY(timeInterval);

                    }else{

                        if(Objects.equals(myPArameters.getSpeed(), przed.getSpeed())){

                            myPArameters.setAcceleration(0L);
                            myPArameters.updateY(timeInterval);

                        }else{

                            myPArameters.setPercentageAcceler(-20L);
                            myPArameters.updateSpeed();
                            myPArameters.updateY(timeInterval);

                        }

                    }

                }

            }else{

                //todo jezeli mam prosbe o zjechanie

                if(canChangeLane){

                    if(myPArameters.getSpeed()>=myPArameters.getMax_speed()){

                        myPArameters.setSpeed(myPArameters.getMax_speed());
                        myPArameters.setAcceleration(0L);
                        myPArameters.setX(1L);
                        myPArameters.updateY(timeInterval);

                    }else{

                        myPArameters.addPercentageAcceleration(10L);
                        myPArameters.updateSpeed();
                        myPArameters.setX(1L);
                        myPArameters.updateY(timeInterval);

                    }

                }else{

                    if(przed == null || canMoveOn){

                        if(myPArameters.getSpeed()>=myPArameters.getMax_speed()){

                            myPArameters.setSpeed(myPArameters.getMax_speed());
                            myPArameters.setAcceleration(0L);
                            myPArameters.updateY(timeInterval);

                        }else{

                            myPArameters.addPercentageAcceleration(10L);
                            myPArameters.updateSpeed();
                            myPArameters.updateY(timeInterval);

                        }

                    }else{

                        //sendMessageRequestChange pass
                        if(Objects.equals(myPArameters.getSpeed(), przed.getSpeed())){

                            myPArameters.setAcceleration(0L);
                            myPArameters.updateY(timeInterval);

                        }else{

                            myPArameters.setPercentageAcceler(-20L);
                            myPArameters.updateSpeed();
                            myPArameters.updateY(timeInterval);

                        }

                    }

                }

            }



            System.out.println("Parametrey dla: " + getName() + "\t to: Predkosc:  " + myPArameters.getSpeed() + ",\t X: " + myPArameters.getX() + ",\t Y: " + myPArameters.getY());
            SendParameters();

            if (myPArameters.getY() >= CarsApplication.MAX_Y) {
                doDelete();
                GUIApp.onDelete(getAID());
            }

            GUIApp.onUpdateParameters(getAID(), Agent2.this.myPArameters.getX(), Agent2.this.myPArameters.getY());
        }
    }


    class Receiver extends CyclicBehaviour {

        @Override
        public void action() {
            ACLMessage msg = receive();
            if (msg != null) {
                //System.out.println(msg.toString());
                try {
                    ContentElement element = myAgent.getContentManager().extractContent(msg);
                    Concept action = ((Action) element).getAction();
                    if (action instanceof VehicleParameters) {
                        VehicleParameters v = (VehicleParameters) action;
                        otherPArameters = v;
                        //System.out.println("Wartosci dla Agenta1: "+v.getMax_speed()+" "+v.getX()+" "+v.getSpeed());
                        otherCarsParams.put(msg.getSender(), v);
                    }
                    //nowe
                    if (action instanceof SignParameters) {
                        SignParameters v = (SignParameters) action;
                        mySignParameters = v;
                        //System.out.println("Wartosci dla Znaku: "+v.getY_begin()+" "+v.getY_end()+" "+v.getLimit_max_speed());
                        allSignsParams.put(msg.getSender(), v);
                    }
                } catch (Codec.CodecException | OntologyException e) {
                    e.printStackTrace();
                }

            } else {
                block();
            }
        }
    }

    @Override
    protected void takeDown() {
        super.takeDown();
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    private void SendParameters() {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        List<AID> receivers = CarsMap.getAllOtherCars(this, AGENT_TYPE);
        AID receiver = new AID("SzybszyAgent", AID.ISLOCALNAME);
        //msg.addReceiver(receiver);
        for (AID rec : receivers) {
            msg.addReceiver(rec);
        }

        msg.setLanguage(FIPANames.ContentLanguage.FIPA_SL0);
        msg.setOntology(ParametersOntology.NAME);
        try {
            getContentManager().fillContent(msg, new Action(receiver, myPArameters));
        } catch (Codec.CodecException | OntologyException e) {
            e.printStackTrace();
        }
        send(msg);

    }

}


