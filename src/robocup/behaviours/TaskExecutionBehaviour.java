package robocup.behaviours;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import robocup.models.Task;
import robocup.utils.Config;

/**
 * Behaviour abstrait de base pour l'exécution d'une tâche.
 * Hérite de OneShotBehaviour JADE : exécuté une seule fois, dans le
 * thread de l'agent, sans bloquer les autres behaviours.
 *
 * Les sous-classes implémentent executerTache() avec la logique métier.
 */
public abstract class TaskExecutionBehaviour extends OneShotBehaviour {

    protected final Task   task;
    protected final String actAgentName;   // pour envoyer les confirmations

    public TaskExecutionBehaviour(Agent agent, Task task, String actAgentName) {
        super(agent);
        this.task         = task;
        this.actAgentName = actAgentName;
    }

    @Override
    public final void action() {
        try {
            task.demarrer();
            log("> Démarrage : " + task);

            executerTache();

            task.terminer();
            log("[OK] Terminée : " + task.getTaskId());
            envoyerConfirmation();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            signalerEchec("Interruption");
        } catch (Exception e) {
            signalerEchec(e.getMessage());
        }
    }

    /** Logique métier à implémenter dans chaque sous-classe. */
    protected abstract void executerTache() throws Exception;

    // ── Utilitaires ───────────────────────────────

    protected void log(String msg) {
        System.out.printf("[%s] %s%n", myAgent.getLocalName(), msg);
    }

    protected void pause(long ms) throws InterruptedException {
        Thread.sleep(ms);
    }

    protected void envoyerConfirmation() {
        ACLMessage confirm = new ACLMessage(ACLMessage.CONFIRM);
        confirm.addReceiver(
            new jade.core.AID(actAgentName, jade.core.AID.ISLOCALNAME));
        confirm.setOntology(Config.ONTOLOGIE);
        confirm.setContent("CONFIRM:task_id=" + task.getTaskId());
        myAgent.send(confirm);
    }

    protected void signalerEchec(String raison) {
        task.echouer();
        log("[FAIL] Échec [" + task.getTaskId() + "] : " + raison);
        ACLMessage failure = new ACLMessage(ACLMessage.FAILURE);
        failure.addReceiver(
            new jade.core.AID(actAgentName, jade.core.AID.ISLOCALNAME));
        failure.setOntology(Config.ONTOLOGIE);
        failure.setContent("FAILURE:task_id=" + task.getTaskId()
                           + ";raison=" + raison);
        myAgent.send(failure);
    }
}
