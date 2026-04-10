package robocup.utils;

import robocup.models.Room;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuration centrale du projet SMA RoboCup@Home.
 * Contient toutes les constantes : pièces, distances, poids de priorité,
 * paramètres de simulation et identifiants des agents.
 */
public final class Config {

    private Config() {} // classe utilitaire non instanciable

    // ── Identifiants des agents JADE ──────────────
    public static final String AGENT_AIH  = "AgentInteractionHumaine";
    public static final String AGENT_ACT  = "AgentCoordinateurTaches";
    public static final String AGENT_AN   = "AgentNavigation";
    public static final String AGENT_AM   = "AgentManipulation";
    public static final String AGENT_AS   = "AgentSurveillance";

    // ── Ontologie ACL ─────────────────────────────
    public static final String ONTOLOGIE  = "robocup-home";
    public static final String LANGUE     = "fipa-sl0";

    // ── Pièces de la maison ───────────────────────
    public static final Map<String, Room> ROOMS = new LinkedHashMap<>();
    static {
        ROOMS.put("cuisine", new Room(
            "cuisine", "Cuisine",
            85, 65, 195, 130,
            "#FFF3E0",
            new String[]{"table", "réfrigérateur", "évier", "micro-ondes"}
        ));
        ROOMS.put("salon", new Room(
            "salon", "Salon",
            290, 65, 215, 130,
            "#E8F5E9",
            new String[]{"canapé", "télévision", "table_basse", "lampe"}
        ));
        ROOMS.put("chambre", new Room(
            "chambre", "Chambre",
            85, 205, 195, 110,
            "#E3F2FD",
            new String[]{"lit", "armoire", "bureau", "chaise"}
        ));
        ROOMS.put("salle_de_bain", new Room(
            "salle_de_bain", "Salle de Bain",
            290, 205, 215, 110,
            "#F3E5F5",
            new String[]{"baignoire", "lavabo", "toilettes", "miroir"}
        ));
    }

    // ── Distances entre pièces ────────────────────
    public static double getDistance(String from, String to) {
        if (from.equals(to)) return 0;
        double[][] dist = {
        //  cuisine  salon   chambre  sdb
            {0,      3,      4,       5},   // cuisine
            {3,      0,      5,       4},   // salon
            {4,      5,      0,       2},   // chambre
            {5,      4,      2,       0},   // salle_de_bain
        };
        String[] keys = {"cuisine","salon","chambre","salle_de_bain"};
        int i = indexOf(keys, from);
        int j = indexOf(keys, to);
        if (i < 0 || j < 0) return 3.0;
        return dist[i][j];
    }

    private static int indexOf(String[] arr, String val) {
        for (int i = 0; i < arr.length; i++)
            if (arr[i].equals(val)) return i;
        return -1;
    }

    // ── Poids de priorisation ─────────────────────
    public static final double POIDS_URGENCE  = 0.50;
    public static final double POIDS_DISTANCE = 0.30;
    public static final double POIDS_CHARGE   = 0.20;

    // ── Simulation ────────────────────────────────
    public static final long   CFP_TIMEOUT_MS       = 5000;   // Contract-Net timeout
    public static final int    MAX_RETRIES           = 3;
    public static final long   NAV_STEP_DELAY_MS     = 80;    // délai animation robot
    public static final double MANIPULATION_DUR_S    = 4.0;   // durée manipulation
    public static final long   SURVEILLANCE_INTERVAL = 10000; // scan toutes 10s
    public static final double PROBA_ANOMALIE        = 0.04;

    // ── Position initiale du robot ────────────────
    public static final int ROBOT_INIT_X = 250;
    public static final int ROBOT_INIT_Y = 175;

    // ── GUI ───────────────────────────────────────
    public static final int    CANVAS_W     = 520;
    public static final int    CANVAS_H     = 340;
    public static final String COLOR_BG     = "#0D1117";
    public static final String COLOR_PANEL  = "#161B22";
    public static final String COLOR_CARD   = "#21262D";
    public static final String COLOR_BLUE   = "#58A6FF";
    public static final String COLOR_GREEN  = "#3FB950";
    public static final String COLOR_ORANGE = "#F78166";
    public static final String COLOR_YELLOW = "#E3B341";
    public static final String COLOR_PURPLE = "#BC8CFF";
    public static final String COLOR_TEXT   = "#C9D1D9";
    public static final String COLOR_MUTED  = "#8B949E";
    public static final String COLOR_BORDER = "#30363D";
    public static final String COLOR_ROBOT  = "#58A6FF";
}
