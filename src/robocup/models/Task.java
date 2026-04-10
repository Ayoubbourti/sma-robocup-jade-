package robocup.models;

import java.io.Serializable;
import java.util.UUID;

/**
 * Représente une tâche dans le SMA RoboCup@Home.
 * Sérialisable pour transmission dans les messages JADE (ACLMessage content).
 */
public class Task implements Serializable {

    private static final long serialVersionUID = 1L;

    // ── Identifiants ─────────────────────────────
    private final String taskId;
    private final String typeTache;       // livraison, nettoyage, urgence...
    private final String description;
    private final String pieceCible;      // cuisine, salon, chambre, salle_de_bain
    private final String objetCible;      // peut être null
    private final int    urgence;         // 1 (basse) -> 10 (critique)

    // ── État dynamique ────────────────────────────
    private TaskStatus statut;
    private String     agentAssigne;
    private double     scorePriorite;
    private int        nbTentatives;
    private long       timestampCreation;
    private long       timestampDebut;
    private long       timestampFin;

    // ── Constructeur principal ────────────────────
    public Task(String typeTache, String description,
                String pieceCible, int urgence, String objetCible) {
        this.taskId            = UUID.randomUUID().toString().substring(0, 8);
        this.typeTache         = typeTache;
        this.description       = description;
        this.pieceCible        = pieceCible;
        this.urgence           = Math.max(1, Math.min(10, urgence));
        this.objetCible        = objetCible;
        this.statut            = TaskStatus.EN_ATTENTE;
        this.timestampCreation = System.currentTimeMillis();
        this.nbTentatives      = 0;
        this.scorePriorite     = 0.0;
    }

    public Task(String typeTache, String description,
                String pieceCible, int urgence) {
        this(typeTache, description, pieceCible, urgence, null);
    }

    // ── Transitions d'état ────────────────────────

    public void assigner(String agentId) {
        this.agentAssigne = agentId;
        this.statut       = TaskStatus.ASSIGNEE;
    }

    public void demarrer() {
        this.statut         = TaskStatus.EN_COURS;
        this.timestampDebut = System.currentTimeMillis();
    }

    public void terminer() {
        this.statut        = TaskStatus.TERMINEE;
        this.timestampFin  = System.currentTimeMillis();
    }

    public void echouer() {
        this.nbTentatives++;
        this.statut       = TaskStatus.ECHOUEE;
        this.timestampFin = System.currentTimeMillis();
    }

    public void reinitialiser() {
        this.statut       = TaskStatus.EN_ATTENTE;
        this.agentAssigne = null;
        this.timestampDebut = 0;
    }

    // ── Propriétés calculées ──────────────────────

    public boolean estActive() {
        return statut == TaskStatus.ASSIGNEE
            || statut == TaskStatus.EN_COURS;
    }

    public boolean estTerminale() {
        return statut == TaskStatus.TERMINEE
            || statut == TaskStatus.ECHOUEE
            || statut == TaskStatus.ANNULEE;
    }

    public long getAgeMs() {
        return System.currentTimeMillis() - timestampCreation;
    }

    public double getDureeExecution() {
        if (timestampDebut > 0 && timestampFin > 0)
            return (timestampFin - timestampDebut) / 1000.0;
        return -1;
    }

    // ── Sérialisation simple (pour les messages ACL) ──
    public String toAclString() {
        return String.format(
            "id=%s;type=%s;piece=%s;urgence=%d;statut=%s;score=%.2f;agent=%s",
            taskId, typeTache, pieceCible, urgence,
            statut.name(),
            scorePriorite,
            agentAssigne != null ? agentAssigne : "none"
        );
    }

    @Override
    public String toString() {
        return String.format("Task[%s | %s | %s | urg=%d | %s]",
            taskId, typeTache, pieceCible, urgence, statut.name());
    }

    // ── Getters ───────────────────────────────────

    public String  getTaskId()          { return taskId; }
    public String  getTypeTache()       { return typeTache; }
    public String  getDescription()     { return description; }
    public String  getPieceCible()      { return pieceCible; }
    public String  getObjetCible()      { return objetCible; }
    public int     getUrgence()         { return urgence; }
    public TaskStatus getStatut()       { return statut; }
    public String  getAgentAssigne()    { return agentAssigne; }
    public double  getScorePriorite()   { return scorePriorite; }
    public int     getNbTentatives()    { return nbTentatives; }
    public long    getTimestampCreation(){ return timestampCreation; }

    // ── Setters ───────────────────────────────────

    public void setStatut(TaskStatus s)        { this.statut = s; }
    public void setAgentAssigne(String a)      { this.agentAssigne = a; }
    public void setScorePriorite(double s)     { this.scorePriorite = s; }
}
