package robocup.behaviours;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.core.AID;
import robocup.utils.Config;
import robocup.agents.SurveillanceAgent;
import java.util.Random;

public class SurveillanceBehaviour extends TickerBehaviour {

    private static final String[] PIECES = {
        "cuisine", "salon", "chambre", "salle_de_bain"
    };

    private static final String[][] ANOMALIES = {
        {"odeur de brûlé", "robinet ouvert", "gaz détecté"},
        {"bruit inhabituel", "fenêtre ouverte", "chute d'objet"},
        {"personne immobile", "appel à l'aide", "chute du lit"},
        {"eau qui déborde", "chute détectée", "absence prolongée"},
    };

    private int pieceIdx = 0;
    private int nbScans = 0;
    private int nbAlertes = 0;
    private boolean active = true;
    private final Random rng = new Random();
    private final String aihName;

    public SurveillanceBehaviour(Agent agent, String aihName) {
        super(agent, Config.SURVEILLANCE_INTERVAL);
        this.aihName = aihName;
    }

    @Override
    protected void onTick() {
        if (!active) return;

        String piece = PIECES[pieceIdx % PIECES.length];
        pieceIdx++;
        nbScans++;

        System.out.printf("[%s] [SEARCH] Scan de %s...%n",
            myAgent.getLocalName(), piece);

        if (rng.nextDouble() < Config.PROBA_ANOMALIE) {
            int pi = indexOf(PIECES, piece);
            String[] anoms = ANOMALIES[pi >= 0 ? pi : 0];
            String anomalie = anoms[rng.nextInt(anoms.length)];
            declencherAlerte(piece, anomalie);
        }
    }

    private void declencherAlerte(String piece, String description) {
        nbAlertes++;
        System.out.printf("[%s] [WARN] ANOMALIE dans %s : %s%n",
            myAgent.getLocalName(), piece, description);

        // CORRECTION: Appeler la méthode de l'agent parent
        if (myAgent instanceof SurveillanceAgent) {
            ((SurveillanceAgent) myAgent).notifierAnomalie(piece, description, 8);
        } else {
            // Fallback: envoyer directement
            ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
            inform.addReceiver(new AID(aihName, AID.ISLOCALNAME));
            inform.setOntology(Config.ONTOLOGIE);
            inform.setContent(String.format("ANOMALIE:piece=%s;description=%s;urgence=8",
                piece, description));
            myAgent.send(inform);
        }
    }

    private static int indexOf(String[] arr, String val) {
        for (int i = 0; i < arr.length; i++)
            if (arr[i].equals(val)) return i;
        return -1;
    }

    public void setActive(boolean b) { this.active = b; }
    public int getNbScans() { return nbScans; }
    public int getNbAlertes() { return nbAlertes; }
}