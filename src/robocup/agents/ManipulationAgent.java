package robocup.agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import robocup.behaviours.TaskExecutionBehaviour;
import robocup.models.Task;
import robocup.utils.Config;
import robocup.gui.DashboardGUI;

import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

public class ManipulationAgent extends Agent {

    private static final Set<String> COMPATIBLES = new HashSet<>(
        Arrays.asList("livraison", "nettoyage", "récupération"));

    private static final Map<String, Double> DUREES = new HashMap<>();
    static {
        DUREES.put("livraison", 3.5);
        DUREES.put("nettoyage", 6.0);
        DUREES.put("récupération", 4.0);
    }

    private volatile boolean enManipulation = false;
    private final PriorityBlockingQueue<Task> fileAttente = new PriorityBlockingQueue<>(10,
        Comparator.comparingInt(Task::getUrgence).reversed());
    
    // Support GUI
    private DashboardGUI gui;

    @Override
    protected void setup() {
        // Récupérer la GUI
        Object[] args = getArguments();
        if (args != null && args.length > 0 && args[0] instanceof DashboardGUI) {
            gui = (DashboardGUI) args[0];
            System.out.println("[AM] Connecté à l'interface graphique");
        }
        
        System.out.printf("[%s] AM demarre - compatible: %s%n", getLocalName(), COMPATIBLES);

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg == null) { block(); return; }
                dispatch(msg);
            }
        });
    }

    private void logToGUI(String msg, String niveau) {
        if (gui != null) {
            gui.log(msg, niveau);
        }
    }

    private void dispatch(ACLMessage msg) {
        String content = msg.getContent();
        int perf = msg.getPerformative();
        if (content == null) return;

        if (perf == ACLMessage.CFP) {
            traiterCFP(content, msg);
        } else if (perf == ACLMessage.ACCEPT_PROPOSAL) {
            traiterAccept(content);
        } else if (perf == ACLMessage.REJECT_PROPOSAL) {
            System.out.printf("[%s] REJECT recu task=%s%n", getLocalName(), extraire(content, "task_id"));
        } else if (perf == ACLMessage.REQUEST && content.startsWith("MANIPULER:")) {
            traiterDelegation(content.substring("MANIPULER:".length()));
        }
    }

    private void traiterCFP(String content, ACLMessage cfp) {
    String taskId = extraire(content, "task_id");
    String typeTache = extraire(content, "type");

    System.out.printf("[%s] CFP recu: task=%s type=%s%n", getLocalName(), taskId, typeTache);

    if (!COMPATIBLES.contains(typeTache)) {
        System.out.printf("[%s] Type '%s' non compatible - pas d'offre%n", getLocalName(), typeTache);
        return;
    }

    double score = !enManipulation ? 1.0 : Math.max(0.05, 0.8 - fileAttente.size() * 0.1);
    
    // CORRECTION: Réduire le score pour la livraison (préférer AN)
    if (typeTache.equals("livraison")) {
        score = score * 0.6;  // Réduction de 40%
        System.out.printf("[%s] [INFO] Livraison: score réduit à %.3f (préférence navigation)%n", 
                         getLocalName(), score);
    }
    // CORRECTION: Réduire le score pour récupération
    if (typeTache.equals("récupération")) {
        score = score * 0.7;  // Réduction de 30%
        System.out.printf("[%s] [INFO] Récupération: score réduit à %.3f (préférence navigation)%n", 
                         getLocalName(), score);
    }
    // CORRECTION: Score normal pour nettoyage (AM spécialiste)
    if (typeTache.equals("nettoyage")) {
        score = score * 1.0;  // Pas de réduction
        System.out.printf("[%s] [INFO] Nettoyage: score normal %.3f (AM spécialiste)%n", 
                         getLocalName(), score);
    }

    System.out.printf("[%s] [OUT] PROPOSE task=%s score=%.3f (file=%d)%n",
        getLocalName(), taskId, score, fileAttente.size());

    ACLMessage propose = new ACLMessage(ACLMessage.PROPOSE);
    propose.addReceiver(new AID(Config.AGENT_ACT, AID.ISLOCALNAME));
    propose.setOntology(Config.ONTOLOGIE);
    propose.setContent(String.format("PROPOSE:task_id=%s;score=%.4f;agent_type=manipulation", taskId, score));
    propose.setConversationId(cfp.getConversationId());
    send(propose);
}

    private void traiterAccept(String content) {
        Task task = creerTache(content);
        if (task != null) {
            System.out.printf("[%s] ACCEPT recu task=%s%n", getLocalName(), task.getTaskId());
            logToGUI("[MANIP] Tâche " + task.getTaskId() + " acceptée", "INFO");
            enqueuer(task);
        }
    }

    private void traiterDelegation(String content) {
        Task task = creerTache(content);
        if (task != null) {
            System.out.printf("[%s] DELEGATION AN recu task=%s%n", getLocalName(), task.getTaskId());
            logToGUI("[MANIP] Délégation de AN pour " + task.getTaskId(), "INFO");
            enqueuer(task);
        }
    }

    private synchronized void enqueuer(Task task) {
        if (!enManipulation) {
            demarrer(task);
        } else {
            fileAttente.offer(task);
            System.out.printf("[%s] Tache [%s] en file (file=%d)%n", getLocalName(), task.getTaskId(), fileAttente.size());
            logToGUI("[MANIP] Tâche " + task.getTaskId() + " en file d'attente (position " + fileAttente.size() + ")", "INFO");
        }
    }

    private synchronized void traiterSuivante() {
        Task next = fileAttente.poll();
        if (next != null) {
            System.out.printf("[%s] Prochaine tache [%s] (restant=%d)%n", getLocalName(), next.getTaskId(), fileAttente.size());
            demarrer(next);
        } else {
            enManipulation = false;
        }
    }

    private void demarrer(Task task) {
        enManipulation = true;
        logToGUI("[MANIP] Début manipulation pour " + task.getTaskId(), "INFO");
        addBehaviour(new ManipulationBehaviour(this, task));
    }

    private class ManipulationBehaviour extends TaskExecutionBehaviour {

        public ManipulationBehaviour(Agent agent, Task task) {
            super(agent, task, Config.AGENT_ACT);
        }

        @Override
        protected void executerTache() throws Exception {
            String type = task.getTypeTache();
            double duree = DUREES.getOrDefault(type, 3.0);
            String[] etapes = getEtapes(type);
            int n = etapes.length;

            for (int i = 0; i < n; i++) {
                log("  -> [" + task.getTaskId() + "] " + etapes[i]);
                if (gui != null) {
                    gui.log("[MANIP] " + task.getTaskId() + ": " + etapes[i], "DEBUG");
                }
                pause((long) (duree * 1000 / n));
            }
        }

        @Override
        protected void envoyerConfirmation() {
            super.envoyerConfirmation();
            logToGUI("[MANIP] Fin manipulation pour " + task.getTaskId(), "SUCCESS");
            ACLMessage libre = new ACLMessage(ACLMessage.INFORM);
            libre.addReceiver(new AID(Config.AGENT_AN, AID.ISLOCALNAME));
            libre.setOntology(Config.ONTOLOGIE);
            libre.setContent("AGENT_LIBRE:task_id=" + task.getTaskId());
            myAgent.send(libre);
            traiterSuivante();
        }

        @Override
        protected void signalerEchec(String raison) {
            super.signalerEchec(raison);
            logToGUI("[MANIP] Échec pour " + task.getTaskId() + ": " + raison, "ERROR");
            traiterSuivante();
        }
    }

    private static String[] getEtapes(String type) {
        switch (type) {
            case "livraison":
                return new String[]{"Localisation objet...", "Prehension objet", "Transport vers cible", "Depot objet"};
            case "nettoyage":
                return new String[]{"Deploiement outils", "Nettoyage zone 1", "Nettoyage zone 2", "Verification proprete", "Rangement outils"};
            default:
                return new String[]{"Recherche objet...", "Saisie objet", "Securisation prise"};
        }
    }

    private static Task creerTache(String content) {
        try {
            String type = extraire(content, "type");
            String piece = extraire(content, "piece");
            String desc = extraire(content, "desc");
            int urgence;
            try { urgence = Integer.parseInt(extraire(content, "urgence")); }
            catch (Exception e) { urgence = 5; }
            if (type.isEmpty() || piece.isEmpty()) return null;
            return new Task(type, desc.isEmpty() ? "Tache " + type : desc, piece, urgence);
        } catch (Exception e) { return null; }
    }

    private static String extraire(String content, String key) {
        if (content == null) return "";
        for (String part : content.split(";")) {
            if (part.startsWith(key + "="))
                return part.substring(key.length() + 1);
        }
        return "";
    }

    @Override
    protected void takeDown() {
        System.out.printf("[%s] AM arrete%n", getLocalName());
    }
}