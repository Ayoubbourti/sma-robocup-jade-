package robocup.agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import robocup.models.Task;
import robocup.utils.Config;
import robocup.utils.PriorityCalculator;
import robocup.gui.DashboardGUI;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TaskCoordinatorAgent extends Agent {

    private final Map<String, Task> tachesActives = new ConcurrentHashMap<>();
    private final Map<String, Map<String,Double>> offresRecues = new ConcurrentHashMap<>();
    private final Map<String, Long> deadlines = new ConcurrentHashMap<>();
    private final Map<String, Integer> charges = new ConcurrentHashMap<>();
    private volatile String pieceRobot = "salon";
    
    // Support GUI
    private DashboardGUI gui;

    @Override
    protected void setup() {
        // Récupérer la GUI
        Object[] args = getArguments();
        if (args != null && args.length > 0 && args[0] instanceof DashboardGUI) {
            gui = (DashboardGUI) args[0];
            logToGUI("[ACT] Connecté à l'interface graphique", "INFO");
        }
        
        System.out.printf("[%s] ACT demarre%n", getLocalName());
        charges.put(Config.AGENT_AN, 0);
        charges.put(Config.AGENT_AM, 0);

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg == null) { block(); return; }
                dispatch(msg);
            }
        });

        addBehaviour(new TickerBehaviour(this, 500) {
            @Override
            protected void onTick() {
                verifierTimeouts();
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
        String sender = msg.getSender().getLocalName();
        if (content == null) return;

        if (perf == ACLMessage.REQUEST && content.startsWith("NOUVELLE_TACHE:")) {
            recevoirTache(content.substring("NOUVELLE_TACHE:".length()));
        } else if (perf == ACLMessage.PROPOSE) {
            recevoirOffre(content, sender);
        } else if (perf == ACLMessage.CONFIRM) {
            traiterConfirmation(extraire(content, "task_id"));
        } else if (perf == ACLMessage.FAILURE) {
            traiterEchec(extraire(content, "task_id"), extraire(content, "raison"), sender);
        } else if (perf == ACLMessage.INFORM && content.startsWith("POSITION:")) {
            pieceRobot = extraire(content, "piece");
        }
    }

    private void recevoirTache(String aclStr) {
        if (aclStr.startsWith("NOUVELLE_TACHE:")) {
            aclStr = aclStr.substring("NOUVELLE_TACHE:".length());
        }
        
        String type = extraire(aclStr, "type");
        String piece = extraire(aclStr, "piece");
        String desc = extraire(aclStr, "desc");
        int urgence;
        try { urgence = Integer.parseInt(extraire(aclStr, "urgence")); }
        catch (Exception e) { urgence = 5; }

        if (type.isEmpty() || piece.isEmpty()) {
            System.out.printf("[%s] [WARN] Tache invalide : %s%n", getLocalName(), aclStr);
            return;
        }

        Task task = new Task(type, desc.isEmpty() ? "Tache " + type : desc, piece, urgence);

        int chargeMin = charges.values().stream().mapToInt(Integer::intValue).min().orElse(0);
        double score = PriorityCalculator.calculer(task, pieceRobot, chargeMin);
        task.setScorePriorite(score);
        tachesActives.put(task.getTaskId(), task);

        System.out.printf("[%s] [TASK] Recu [%s] type=%s piece=%s score=%.2f%n",
            getLocalName(), task.getTaskId(), type, piece, score);
        
        // Ajouter à la GUI
        if (gui != null) {
            gui.addTaskRow(task.getTaskId(), type, piece, urgence, "EN_ATTENTE", score, "-");
            logToGUI("[TASK] Nouvelle tâche: " + task.getTaskId() + " (" + type + " - " + piece + ")", "INFO");
        }
        
        lancerCFP(task);
    }

    private void lancerCFP(Task task) {
        offresRecues.put(task.getTaskId(), new ConcurrentHashMap<>());
        deadlines.put(task.getTaskId(), System.currentTimeMillis() + Config.CFP_TIMEOUT_MS);

        String content = String.format("task_id=%s;type=%s;piece=%s;urgence=%d",
            task.getTaskId(), task.getTypeTache(), task.getPieceCible(), task.getUrgence());

        for (String agentName : new String[]{Config.AGENT_AN, Config.AGENT_AM}) {
            ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
            cfp.addReceiver(new AID(agentName, AID.ISLOCALNAME));
            cfp.setOntology(Config.ONTOLOGIE);
            cfp.setContent(content);
            cfp.setConversationId("cfp-" + task.getTaskId());
            send(cfp);
        }

        System.out.printf("[%s] [CFP] CFP envoye a AN+AM pour [%s] type=%s%n",
            getLocalName(), task.getTaskId(), task.getTypeTache());
    }

    private void recevoirOffre(String content, String sender) {
        if (content.startsWith("PROPOSE:")) {
            content = content.substring("PROPOSE:".length());
        }
        
        String taskId = extraire(content, "task_id");
        double score;
        try { score = Double.parseDouble(extraire(content, "score")); }
        catch (Exception e) { score = 0.0; }

        Map<String, Double> offres = offresRecues.get(taskId);
        if (offres == null) {
            System.out.printf("[%s] [WARN] PROPOSE orphelin task=%s de %s%n", getLocalName(), taskId, sender);
            return;
        }

        offres.put(sender, score);
        System.out.printf("[%s] [MSG] Offre de %s : score=%.3f pour [%s] (%d/2)%n",
            getLocalName(), sender, score, taskId, offres.size());

        if (offres.size() >= 2) {
            attribuer(taskId);
        }
    }

    private void verifierTimeouts() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Long> e : deadlines.entrySet()) {
            if (now > e.getValue()) {
                String taskId = e.getKey();
                deadlines.remove(taskId);
                Map<String, Double> offres = offresRecues.get(taskId);
                if (offres != null) {
                    if (!offres.isEmpty()) {
                        System.out.printf("[%s] [CFP] Timeout - attribution avec %d offre(s)%n", getLocalName(), offres.size());
                        attribuer(taskId);
                    } else {
                        Task task = tachesActives.get(taskId);
                        if (task != null && task.getNbTentatives() < Config.MAX_RETRIES) {
                            task.echouer();
                            task.reinitialiser();
                            System.out.printf("[%s] [WARN] Aucune offre pour [%s] - reessai %d/%d%n",
                                getLocalName(), taskId, task.getNbTentatives(), Config.MAX_RETRIES);
                            lancerCFP(task);
                        } else if (task != null) {
                            abandonner(taskId);
                        }
                    }
                }
            }
        }
    }

    private void attribuer(String taskId) {
        deadlines.remove(taskId);
        Map<String, Double> offres = offresRecues.remove(taskId);
        Task task = tachesActives.get(taskId);
        if (task == null || offres == null || offres.isEmpty()) return;

        String bestAgent = null;
        double bestScore = -1;
        for (Map.Entry<String, Double> e : offres.entrySet()) {
            if (e.getValue() > bestScore) {
                bestScore = e.getValue();
                bestAgent = e.getKey();
            }
        }

        if (bestAgent == null) { abandonner(taskId); return; }

        System.out.printf("[%s] [WIN] Agent selectionne : %s (score=%.3f) -> [%s]%n",
            getLocalName(), bestAgent, bestScore, taskId);

        task.assigner(bestAgent);
        charges.merge(bestAgent, 1, Integer::sum);
        
        // Mettre à jour la GUI
        if (gui != null) {
            gui.addTaskRow(taskId, task.getTypeTache(), task.getPieceCible(), task.getUrgence(), 
                          "ASSIGNEE", bestScore, bestAgent);
            logToGUI("[TASK] Tâche " + taskId + " assignée à " + bestAgent, "INFO");
        }

        String acceptContent = "ACCEPT:task_id=" + taskId
            + ";type=" + task.getTypeTache()
            + ";piece=" + task.getPieceCible()
            + ";urgence=" + task.getUrgence()
            + ";desc=" + task.getDescription();

        ACLMessage accept = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
        accept.addReceiver(new AID(bestAgent, AID.ISLOCALNAME));
        accept.setOntology(Config.ONTOLOGIE);
        accept.setContent(acceptContent);
        accept.setConversationId("cfp-" + taskId);
        send(accept);

        for (String agent : offres.keySet()) {
            if (!agent.equals(bestAgent)) {
                ACLMessage reject = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                reject.addReceiver(new AID(agent, AID.ISLOCALNAME));
                reject.setOntology(Config.ONTOLOGIE);
                reject.setContent("REJECT:task_id=" + taskId);
                reject.setConversationId("cfp-" + taskId);
                send(reject);
            }
        }
    }

    private void traiterConfirmation(String taskId) {
        Task task = tachesActives.remove(taskId);
        if (task == null) return;
        String agent = task.getAgentAssigne();
        if (agent != null) charges.merge(agent, -1, (a, b) -> Math.max(0, a + b));

        System.out.printf("[%s] [OK] Tache [%s] terminee avec succes%n", getLocalName(), taskId);
        
        if (gui != null) {
            gui.addTaskRow(taskId, task.getTypeTache(), task.getPieceCible(), task.getUrgence(), 
                          "TERMINEE", task.getScorePriorite(), agent);
            logToGUI("[TASK] Tâche " + taskId + " terminée avec succès", "SUCCESS");
        }

        ACLMessage confirm = new ACLMessage(ACLMessage.CONFIRM);
        confirm.addReceiver(new AID(Config.AGENT_AIH, AID.ISLOCALNAME));
        confirm.setOntology(Config.ONTOLOGIE);
        confirm.setContent("CONFIRM:task_id=" + taskId);
        send(confirm);
    }

    private void traiterEchec(String taskId, String raison, String agentName) {
        Task task = tachesActives.get(taskId);
        if (task == null) return;
        charges.merge(agentName, -1, (a, b) -> Math.max(0, a + b));

        System.out.printf("[%s] [CRASH] Echec [%s] par %s : %s (tentative %d/%d)%n",
            getLocalName(), taskId, agentName, raison, task.getNbTentatives(), Config.MAX_RETRIES);
        
        if (gui != null) {
            logToGUI("[TASK] Tâche " + taskId + " a échoué: " + raison, "ERROR");
        }

        if (task.getNbTentatives() < Config.MAX_RETRIES) {
            task.reinitialiser();
            System.out.printf("[%s] [RETRY] Reallocation [%s]%n", getLocalName(), taskId);
            lancerCFP(task);
        } else {
            abandonner(taskId);
        }
    }

    private void abandonner(String taskId) {
        tachesActives.remove(taskId);
        offresRecues.remove(taskId);
        deadlines.remove(taskId);
        System.out.printf("[%s] [DELETE] Tache [%s] abandonnee (max retries)%n", getLocalName(), taskId);
        
        if (gui != null) {
            logToGUI("[TASK] Tâche " + taskId + " abandonnée (max retries)", "ERROR");
        }

        ACLMessage failure = new ACLMessage(ACLMessage.FAILURE);
        failure.addReceiver(new AID(Config.AGENT_AIH, AID.ISLOCALNAME));
        failure.setOntology(Config.ONTOLOGIE);
        failure.setContent("FAILURE:task_id=" + taskId + ";raison=max_retries");
        send(failure);
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
        System.out.printf("[%s] ACT arrete%n", getLocalName());
    }
}