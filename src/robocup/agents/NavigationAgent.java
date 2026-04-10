package robocup.agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import robocup.behaviours.TaskExecutionBehaviour;
import robocup.models.Room;
import robocup.models.Task;
import robocup.utils.Config;
import robocup.gui.DashboardGUI;

import java.util.Set;
import java.util.HashSet;

public class NavigationAgent extends Agent {

    private static final Set<String> COMPATIBLES = new HashSet<>();
    static {
        COMPATIBLES.add("livraison");
        COMPATIBLES.add("urgence");
        COMPATIBLES.add("guidage");
        COMPATIBLES.add("surveillance");
        COMPATIBLES.add("récupération");
    }

    private volatile boolean enDeplacement = false;
    private volatile String  pieceActuelle = "salon";
    private volatile double  robotX        = Config.ROBOT_INIT_X;
    private volatile double  robotY        = Config.ROBOT_INIT_Y;
    
    private DashboardGUI gui;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length > 0 && args[0] instanceof DashboardGUI) {
            gui = (DashboardGUI) args[0];
            System.out.println("[AN] Connecté à l'interface graphique");
        }
        
        System.out.printf("[%s] AN demarre - compatible: %s%n", getLocalName(), COMPATIBLES);

        // Mise à jour périodique de la position sur la carte
        addBehaviour(new TickerBehaviour(this, 300) {  // Toutes les 300ms
            @Override
            protected void onTick() {
                if (gui != null) {
                    gui.updateRobotPosition(robotX, robotY, pieceActuelle);
                }
            }
        });

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg == null) { block(); return; }
                dispatch(msg);
            }
        });
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
            System.out.printf("[%s] REJECT recu pour task=%s%n", getLocalName(), extraire(content, "task_id"));
        } else if (perf == ACLMessage.INFORM && content.startsWith("AGENT_LIBRE:")) {
            System.out.printf("[%s] AM libre - pret pour prochaine tache%n", getLocalName());
        }
    }

    private void traiterCFP(String content, ACLMessage cfp) {
    String taskId = extraire(content, "task_id");
    String typeTache = extraire(content, "type");
    String piece = extraire(content, "piece");

    System.out.printf("[%s] CFP recu: task=%s type=%s piece=%s%n", getLocalName(), taskId, typeTache, piece);

    if (!COMPATIBLES.contains(typeTache)) {
        System.out.printf("[%s] Type '%s' non compatible - pas d'offre%n", getLocalName(), typeTache);
        return;
    }

    double dispo = enDeplacement ? 0.0 : 1.0;
    double dist = Config.getDistance(pieceActuelle, piece);
    double score = dispo * 0.6 + (1.0 / (1.0 + dist / 5.0)) * 0.4;
    
    // CORRECTION: Augmenter le score pour la livraison (AN spécialiste du transport)
    if (typeTache.equals("livraison")) {
        score = score * 1.5;  // Bonus de 50%
        if (score > 1.0) score = 1.0;
        System.out.printf("[%s] [INFO] Livraison: score augmenté à %.3f (AN spécialiste)%n", 
                         getLocalName(), score);
    }
    // CORRECTION: Augmenter le score pour récupération
    if (typeTache.equals("récupération")) {
        score = score * 1.3;  // Bonus de 30%
        if (score > 1.0) score = 1.0;
        System.out.printf("[%s] [INFO] Récupération: score augmenté à %.3f%n", 
                         getLocalName(), score);
    }
    // CORRECTION: Score normal pour guidage
    if (typeTache.equals("guidage")) {
        score = score * 1.0;
        System.out.printf("[%s] [INFO] Guidage: score normal %.3f%n", 
                         getLocalName(), score);
    }

    System.out.printf("[%s] [OUT] PROPOSE task=%s score=%.3f (dispo=%.0f dist=%.0f)%n",
        getLocalName(), taskId, score, dispo, dist);

    ACLMessage propose = new ACLMessage(ACLMessage.PROPOSE);
    propose.addReceiver(new AID(Config.AGENT_ACT, AID.ISLOCALNAME));
    propose.setOntology(Config.ONTOLOGIE);
    propose.setContent(String.format("PROPOSE:task_id=%s;score=%.4f;agent_type=navigation", taskId, score));
    propose.setConversationId(cfp.getConversationId());
    send(propose);
}

    private void traiterAccept(String content) {
        String taskId = extraire(content, "task_id");
        String type = extraire(content, "type");
        String piece = extraire(content, "piece");
        int urgence;
        try { urgence = Integer.parseInt(extraire(content, "urgence")); }
        catch (Exception e) { urgence = 5; }
        String desc = extraire(content, "desc");

        System.out.printf("[%s] ACCEPT recu - navigation vers %s pour task=%s%n", getLocalName(), piece, taskId);

        Task task = new Task(type, desc.isEmpty() ? "Navigation " + type : desc, piece, urgence);
        addBehaviour(new NavigationBehaviour(this, task));
    }

    private class NavigationBehaviour extends TaskExecutionBehaviour {

        public NavigationBehaviour(Agent agent, Task task) {
            super(agent, task, Config.AGENT_ACT);
        }

        @Override
        protected void executerTache() throws Exception {
            enDeplacement = true;
            String dest = task.getPieceCible();
            Room room = Config.ROOMS.get(dest);

            if (room == null) {
                enDeplacement = false;
                throw new Exception("Piece inconnue: " + dest);
            }

            double targetX = room.getCenterX();
            double targetY = room.getCenterY();
            double dx = targetX - robotX;
            double dy = targetY - robotY;
            double distPx = Math.sqrt(dx*dx + dy*dy);
            int steps = Math.max(5, (int)(distPx / 12));

            log("Navigation vers " + dest + " (" + steps + " etapes)");

            for (int i = 1; i <= steps; i++) {
                robotX += dx / steps;
                robotY += dy / steps;
                
                // Mise à jour immédiate
                if (gui != null) {
                    gui.updateRobotPosition(robotX, robotY, dest);
                }

                pause(Config.NAV_STEP_DELAY_MS);
            }

            pieceActuelle = dest;
            enDeplacement = false;
            log("Arrive a " + dest);

            if (gui != null) {
                gui.updateRobotPosition(robotX, robotY, dest);
                gui.log("[AN] Arrivé à " + dest, "INFO");
            }

            ACLMessage posInform = new ACLMessage(ACLMessage.INFORM);
            posInform.addReceiver(new AID(Config.AGENT_ACT, AID.ISLOCALNAME));
            posInform.setOntology(Config.ONTOLOGIE);
            posInform.setContent("POSITION:piece=" + dest);
            myAgent.send(posInform);

            String type = task.getTypeTache();
            boolean needsManip = type.equals("livraision") || type.equals("livraison")
                || type.contains("cup") || type.equals("nettoyage");

            if (needsManip) {
                log("Delegation manipulation -> AM");
                ACLMessage deleg = new ACLMessage(ACLMessage.REQUEST);
                deleg.addReceiver(new AID(Config.AGENT_AM, AID.ISLOCALNAME));
                deleg.setOntology(Config.ONTOLOGIE);
                deleg.setContent("MANIPULER:"
                    + "task_id=" + task.getTaskId()
                    + ";type=" + task.getTypeTache()
                    + ";piece=" + task.getPieceCible()
                    + ";urgence=" + task.getUrgence()
                    + ";desc=" + task.getDescription());
                myAgent.send(deleg);
                return;
            }
        }
    }

    private static String extraire(String content, String key) {
        if (content == null) return "";
        for (String part : content.split(";")) {
            if (part.startsWith(key + "="))
                return part.substring(key.length() + 1);
        }
        return "";
    }

    public double[] getPosRobot() { return new double[]{robotX, robotY}; }
    public String getPieceActuelle() { return pieceActuelle; }
    public boolean isEnDeplacement() { return enDeplacement; }

    @Override
    protected void takeDown() {
        System.out.printf("[%s] AN arrete%n", getLocalName());
    }
}