package robocup.agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import robocup.behaviours.SurveillanceBehaviour;
import robocup.utils.Config;
import robocup.gui.DashboardGUI;

public class SurveillanceAgent extends Agent {

    private SurveillanceBehaviour survBehaviour;
    private DashboardGUI gui;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length > 0 && args[0] instanceof DashboardGUI) {
            gui = (DashboardGUI) args[0];
        }
        
        System.out.printf("[%s] [OK] AS démarré%n", getLocalName());

        survBehaviour = new SurveillanceBehaviour(this, Config.AGENT_AIH);
        addBehaviour(survBehaviour);

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg == null) { block(); return; }
                String content = msg.getContent();
                if (content == null) return;
                if (content.equals("SURVEILLANCE_ON")) {
                    survBehaviour.setActive(true);
                    if (gui != null) gui.log("[AS] Surveillance activée", "INFO");
                } else if (content.equals("SURVEILLANCE_OFF")) {
                    survBehaviour.setActive(false);
                    if (gui != null) gui.log("[AS] Surveillance désactivée", "INFO");
                } else if (content.equals("RAPPORT")) {
                    envoyerRapport(msg.getSender().getName());
                }
            }
        });
    }

    /**
     * Envoie une alerte d'anomalie à l'agent AIH
     */
    public void notifierAnomalie(String piece, String description, int urgence) {
        String logMsg = String.format("[ALERTE] Anomalie dans %s : %s", piece, description);
        System.out.printf("[%s] %s%n", getLocalName(), logMsg);
        
        if (gui != null) {
            gui.log(logMsg, "WARNING");
        }
        
        // ENVOYER LE MESSAGE À AIH (c'est ce qui manquait !)
        try {
            ACLMessage alert = new ACLMessage(ACLMessage.INFORM);
            alert.addReceiver(new AID(Config.AGENT_AIH, AID.ISLOCALNAME));
            alert.setOntology(Config.ONTOLOGIE);
            // Format correct pour AIH
            alert.setContent(String.format("ANOMALIE:piece=%s;description=%s;urgence=%d", 
                                           piece, description, urgence));
            send(alert);
            System.out.printf("[%s] Alerte envoyée à AIH: piece=%s, desc=%s%n", 
                             getLocalName(), piece, description);
        } catch (Exception e) {
            System.err.printf("[%s] Erreur envoi alerte: %s%n", getLocalName(), e.getMessage());
        }
    }

    private void envoyerRapport(String dest) {
        ACLMessage rapport = new ACLMessage(ACLMessage.INFORM);
        rapport.addReceiver(new jade.core.AID(dest, jade.core.AID.ISGUID));
        rapport.setOntology(Config.ONTOLOGIE);
        rapport.setContent(String.format("RAPPORT:scans=%d;alertes=%d",
            survBehaviour.getNbScans(), survBehaviour.getNbAlertes()));
        send(rapport);
    }

    @Override
    protected void takeDown() {
        System.out.printf("[%s] [STOP] AS arrêté (scans=%d, alertes=%d)%n",
            getLocalName(),
            survBehaviour != null ? survBehaviour.getNbScans() : 0,
            survBehaviour != null ? survBehaviour.getNbAlertes() : 0);
    }
}