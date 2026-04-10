package robocup.models;

/**
 * États possibles d'une tâche dans le cycle de vie du SMA.
 */
public enum TaskStatus {
    EN_ATTENTE,    // créée, non assignée
    ASSIGNEE,      // affectée à un agent
    EN_COURS,      // en cours d'exécution
    TERMINEE,      // exécutée avec succès
    ECHOUEE,       // abandon après max tentatives
    ANNULEE        // annulée par l'opérateur
}
