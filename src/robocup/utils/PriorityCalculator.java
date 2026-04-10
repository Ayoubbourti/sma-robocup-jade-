package robocup.utils;

import robocup.models.Task;

/**
 * Calcule le score de priorité pondéré pour une tâche.
 *
 * Formule
 * -------
 * score = w_urgence  × norm(urgence)
 *       + w_distance × norm(1 / distance)
 *       + w_charge   × norm(1 / charge_agent)
 *       × 10  (mise à l'échelle [0, 10])
 */
public class PriorityCalculator {

    /**
     * Calcule le score de priorité d'une tâche.
     *
     * @param task         tâche à évaluer
     * @param pieceRobot   pièce actuelle du robot
     * @param chargeAgent  nombre de tâches en cours sur l'agent disponible
     * @return score [0.0 - 10.0]
     */
    public static double calculer(Task task, String pieceRobot, int chargeAgent) {

        // ── 1. Composante urgence ──────────────────
        double compUrgence = task.getUrgence() / 10.0;

        // Bonus d'âge : +0.2 max après 2 minutes d'attente
        double bonusAge = Math.min(task.getAgeMs() / 120000.0, 0.2);
        compUrgence = Math.min(compUrgence + bonusAge, 1.0);

        // ── 2. Composante distance ─────────────────
        double distance    = Config.getDistance(pieceRobot, task.getPieceCible());
        double compDistance = (distance == 0)
            ? 1.0
            : 1.0 / (1.0 + distance / 5.0);

        // ── 3. Composante charge agent ─────────────
        double compCharge = 1.0 / (1.0 + chargeAgent);

        // ── 4. Score final pondéré ─────────────────
        double score = (Config.POIDS_URGENCE  * compUrgence
                      + Config.POIDS_DISTANCE * compDistance
                      + Config.POIDS_CHARGE   * compCharge) * 10.0;

        return Math.round(score * 1000.0) / 1000.0;
    }

    /**
     * Sélectionne la meilleure offre Contract-Net (score le plus élevé).
     *
     * @param agentIds   tableau des identifiants agents
     * @param scores     tableau des scores correspondants
     * @return index de l'agent gagnant, -1 si vide
     */
    public static int meilleureOffre(String[] agentIds, double[] scores) {
        if (agentIds == null || agentIds.length == 0) return -1;
        int best = 0;
        for (int i = 1; i < scores.length; i++) {
            if (scores[i] > scores[best]) best = i;
        }
        return best;
    }
}
