package robocup.agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import robocup.models.Task;
import robocup.utils.Config;
import robocup.gui.DashboardGUI;

public class HumanInteractionAgent extends Agent {

    private static volatile HumanInteractionAgent INSTANCE = null;
    private DashboardGUI gui;
    private int nbRequetes = 0;

    public static HumanInteractionAgent getInstance() {
        return INSTANCE;
    }

    public static boolean isReady() {
        return INSTANCE != null;
    }

    @Override
    protected void setup() {
        INSTANCE = this;
        
        Object[] args = getArguments();
        if (args != null && args.length > 0 && args[0] instanceof DashboardGUI) {
            gui = (DashboardGUI) args[0];
            logToGUI("[AIH] Connecté à l'interface graphique", "INFO");
        }
        
        System.out.printf("[%s] [OK] AIH démarré%n", getLocalName());
        logToGUI("[AIH] Agent démarré", "SUCCESS");

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg == null) {
                    block();
                    return;
                }
                traiterMessage(msg);
            }
        });

        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof Task) {
                    soumettreTache((Task) arg);
                }
            }
        }
    }

    private void logToGUI(String msg, String niveau) {
        if (gui != null) {
            gui.log(msg, niveau);
        }
    }

    public void soumettreTache(Task task) {
        nbRequetes++;
        String logMsg = String.format("[IN] Requête [%s] : %s (urgence=%d)",
            task.getTaskId(), task.getDescription(), task.getUrgence());
        System.out.printf("[%s] %s%n", getLocalName(), logMsg);
        logToGUI(logMsg, "INFO");

        ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
        request.addReceiver(new AID(Config.AGENT_ACT, AID.ISLOCALNAME));
        request.setOntology(Config.ONTOLOGIE);
        request.setLanguage(Config.LANGUE);
        String aclContent = task.toAclString()
            + ";desc=" + task.getDescription().replace(";", "_");
        request.setContent("NOUVELLE_TACHE:" + aclContent);
        request.setReplyWith("req-" + task.getTaskId());
        send(request);
    }

    public void signalerUrgence(String piece, String description) {
        String logMsg = String.format("[URGENCE] URGENCE dans %s !", piece);
        System.out.printf("[%s] %s%n", getLocalName(), logMsg);
        logToGUI(logMsg, "WARNING");
        Task t = new Task("urgence", description, piece, 10);
        soumettreTache(t);
    }

    private void traiterMessage(ACLMessage msg) {
        String content = msg.getContent();
        if (content == null) return;

        int perf = msg.getPerformative();

        if (perf == ACLMessage.PROPOSE
                || perf == ACLMessage.CFP
                || perf == ACLMessage.ACCEPT_PROPOSAL
                || perf == ACLMessage.REJECT_PROPOSAL
                || perf == ACLMessage.REFUSE) {
            return;
        }

        switch (perf) {
            case ACLMessage.CONFIRM:
                System.out.printf("[%s] [OK] Tache confirmee : %s%n", getLocalName(), content);
                logToGUI("[CONFIRM] " + content, "SUCCESS");
                break;

            case ACLMessage.FAILURE:
                System.out.printf("[%s] [FAIL] Echec recu : %s%n", getLocalName(), content);
                logToGUI("[FAIL] " + content, "ERROR");
                break;

            case ACLMessage.INFORM:
                traiterInform(content, msg.getSender().getLocalName());
                break;

            default:
                System.out.printf("[%s] [WARN] Performatif inattendu : %d%n", getLocalName(), perf);
        }
    }

    private void traiterInform(String content, String expediteur) {
        System.out.printf("[%s] [DEBUG] INFORM recu: '%s'%n", getLocalName(), content);
        
        if (content.contains("ANOMALIE")) {
            // Nettoyer le préfixe
            String cleanContent = content;
            if (cleanContent.startsWith("ANOMALIE:")) {
                cleanContent = cleanContent.substring("ANOMALIE:".length());
            }
            
            String piece = extraire(cleanContent, "piece");
            String description = extraire(cleanContent, "description");
            int urgence = 8;
            
            String urgStr = extraire(cleanContent, "urgence");
            if (!urgStr.isEmpty()) {
                try { urgence = Integer.parseInt(urgStr); } catch(Exception e) {}
            }
            
            System.out.printf("[%s] [DEBUG] piece='%s', desc='%s', urg=%d%n", 
                             getLocalName(), piece, description, urgence);
            
            if (piece != null && !piece.isEmpty()) {
                Task t = new Task("urgence",
                    "[AS] " + description + " dans " + piece,
                    piece, urgence);
                soumettreTache(t);
            } else {
                System.out.printf("[%s] [WARN] Alerte ignorée: pièce manquante (piece='%s')%n", 
                                 getLocalName(), piece);
            }
        } else {
            System.out.printf("[%s] [INFO] INFORM de %s : %s%n", getLocalName(), expediteur, content);
            logToGUI("[INFO] INFORM de " + expediteur + " : " + content, "INFO");
        }
    }

    // Version corrigée de extraire - plus robuste
    private static String extraire(String content, String key) {
        if (content == null || content.isEmpty()) return "";
        
        String searchKey = key + "=";
        int start = content.indexOf(searchKey);
        if (start == -1) return "";
        
        start += searchKey.length();
        int end = content.indexOf(";", start);
        if (end == -1) {
            end = content.length();
        }
        
        return content.substring(start, end).trim();
    }

    @Override
    protected void takeDown() {
        INSTANCE = null;
        String logMsg = String.format("[STOP] AIH arrêté (%d requêtes traitées)", nbRequetes);
        System.out.printf("[%s] %s%n", getLocalName(), logMsg);
        logToGUI(logMsg, "INFO");
    }
}