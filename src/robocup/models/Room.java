package robocup.models;

/**
 * Représente une pièce de la maison avec ses caractéristiques
 * et sa position sur le canvas de visualisation.
 */
public class Room {

    private final String id;
    private final String label;
    private final int    canvasX;
    private final int    canvasY;
    private final int    canvasW;
    private final int    canvasH;
    private final String color;      // couleur de fond (hex)
    private final String[] objects;  // objets présents

    public Room(String id, String label,
                int canvasX, int canvasY, int canvasW, int canvasH,
                String color, String[] objects) {
        this.id       = id;
        this.label    = label;
        this.canvasX  = canvasX;
        this.canvasY  = canvasY;
        this.canvasW  = canvasW;
        this.canvasH  = canvasH;
        this.color    = color;
        this.objects  = objects;
    }

    /** Centre X sur le canvas (pour le déplacement du robot). */
    public int getCenterX() { return canvasX + canvasW / 2; }

    /** Centre Y sur le canvas. */
    public int getCenterY() { return canvasY + canvasH / 2; }

    // ── Getters ───────────────────────────────────
    public String   getId()      { return id; }
    public String   getLabel()   { return label; }
    public int      getCanvasX() { return canvasX; }
    public int      getCanvasY() { return canvasY; }
    public int      getCanvasW() { return canvasW; }
    public int      getCanvasH() { return canvasH; }
    public String   getColor()   { return color; }
    public String[] getObjects() { return objects; }
}
