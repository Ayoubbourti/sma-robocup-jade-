package robocup.gui;

import robocup.agents.HumanInteractionAgent;
import robocup.models.Task;
import robocup.utils.Config;
import robocup.models.Room;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Interface graphique principale - Java Swing.
 */
public class DashboardGUI extends JFrame {

    // ── Couleurs thème dark ───────────────────────
    private static final Color BG_DARK    = hex(Config.COLOR_BG);
    private static final Color BG_PANEL   = hex(Config.COLOR_PANEL);
    private static final Color BG_CARD    = hex(Config.COLOR_CARD);
    private static final Color COL_BLUE   = hex(Config.COLOR_BLUE);
    private static final Color COL_GREEN  = hex(Config.COLOR_GREEN);
    private static final Color COL_ORANGE = hex(Config.COLOR_ORANGE);
    private static final Color COL_YELLOW = hex(Config.COLOR_YELLOW);
    private static final Color COL_PURPLE = hex(Config.COLOR_PURPLE);
    private static final Color COL_TEXT   = hex(Config.COLOR_TEXT);
    private static final Color COL_MUTED  = Color.decode(Config.COLOR_MUTED);
    private static final Color COL_BORDER = hex(Config.COLOR_BORDER);

    // ── Composants ────────────────────────────────
    private JPanel        canvasPanel;
    private JTextPane     logPane;
    private DefaultTableModel tableModel;
    private JLabel[]      agentLabels;
    private JLabel        lblStatus, lblClock, lblRobot;
    private JLabel        lblCreees, lblReussies, lblEchouees, lblMessages;

    // Contrôles soumission
    private JComboBox<String> cbType, cbPiece;
    private JSlider           sliderUrgence;
    private JTextField        tfDesc;
    private JLabel            lblUrgVal;

    // Position robot
    private double robotX = Config.ROBOT_INIT_X;
    private double robotY = Config.ROBOT_INIT_Y;
    
    // ── STATISTIQUES ────────────────────────────────
    private int totalCrees = 0;
    private int totalReussies = 0;
    private int totalEchouees = 0;
    private int totalMessages = 0;

    public DashboardGUI() {
        super("SMA RoboCup@Home - JADE");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1360, 840);
        setMinimumSize(new Dimension(1100, 700));
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout());

        buildHeader();
        buildCenter();
        buildStatusBar();

        setLocationRelativeTo(null);
        setVisible(true);

        // Horloge
        javax.swing.Timer clock = new javax.swing.Timer(1000, e ->
            lblClock.setText(new SimpleDateFormat("HH:mm:ss").format(new Date())));
        clock.start();
    }

    // ==============================================
    // CONSTRUCTION
    // ==============================================

    private void buildHeader() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(BG_PANEL);
        bar.setPreferredSize(new Dimension(0, 50));
        bar.setBorder(BorderFactory.createMatteBorder(0,0,1,0, COL_BORDER));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 10));
        left.setBackground(BG_PANEL);
        left.add(label("[EXEC]  SMA RoboCup@Home - JADE", COL_BLUE,
                        new Font("Segoe UI", Font.BOLD, 13)));
        left.add(label("Simulateur Multi-Agents v1.0", COL_MUTED,
                        new Font("Segoe UI", Font.PLAIN, 9)));
        bar.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 10));
        right.setBackground(BG_PANEL);

        JButton btnStart = iconButton("> Démarrer", COL_GREEN);
        JButton btnStop  = iconButton("[stop] Arrêter",  COL_ORANGE);
        JButton btnClear = iconButton("[DELETE] Logs",    BG_CARD);
        btnStop.setEnabled(false);

        btnStart.addActionListener(e -> {
            btnStart.setEnabled(false);
            btnStop.setEnabled(true);
            lblStatus.setText("*  Système actif");
            lblStatus.setForeground(COL_GREEN);
            log(" Système démarré", "INFO");
        });
        btnStop.addActionListener(e -> {
            btnStop.setEnabled(false);
            btnStart.setEnabled(true);
            lblStatus.setText("*  Système arrêté");
            lblStatus.setForeground(COL_MUTED);
            log("[STOP] Système arrêté", "WARNING");
        });
        btnClear.addActionListener(e -> logPane.setText(""));

        right.add(btnStart);
        right.add(btnStop);
        right.add(btnClear);
        bar.add(right, BorderLayout.EAST);

        add(bar, BorderLayout.NORTH);
    }

    private void buildCenter() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerLocation(520);
        split.setDividerSize(5);
        split.setBackground(BG_DARK);
        split.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        split.setLeftComponent(buildLeft());
        split.setRightComponent(buildRight());
        add(split, BorderLayout.CENTER);
    }

    private JPanel buildLeft() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BG_DARK);

        p.add(buildCanvasCard());
        p.add(Box.createVerticalStrut(4));
        p.add(buildAgentsCard());

        return p;
    }

    private JPanel buildCanvasCard() {
        JPanel card = card("[HOUSE]  Plan de la maison");
        canvasPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawHouse((Graphics2D) g);
            }
        };
        canvasPanel.setBackground(Color.decode("#080C12"));
        canvasPanel.setPreferredSize(
            new Dimension(Config.CANVAS_W, Config.CANVAS_H));
        canvasPanel.setMaximumSize(
            new Dimension(Config.CANVAS_W, Config.CANVAS_H));
        card.add(canvasPanel, BorderLayout.CENTER);
        return card;
    }

    private void drawHouse(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(BG_PANEL);
        g.fillRect(75, 50, 415, 260);
        g.setColor(COL_BORDER);
        g.setStroke(new BasicStroke(2));
        g.drawRect(75, 50, 415, 260);
        g.setColor(COL_BORDER);
        g.setFont(new Font("Segoe UI", Font.BOLD, 10));
        g.drawString("COULOIR", 255, 188);

        String[] ids  = {"cuisine","salon","chambre","salle_de_bain"};
        Color[]  clrs = {
            Color.decode("#FFF3E0"), Color.decode("#E8F5E9"),
            Color.decode("#E3F2FD"), Color.decode("#F3E5F5")};

        for (int i = 0; i < ids.length; i++) {
            Room room = Config.ROOMS.get(ids[i]);
            int x = room.getCanvasX(), y = room.getCanvasY();
            int w = room.getCanvasW(), h = room.getCanvasH();
            g.setColor(clrs[i]);
            g.fillRect(x, y, w, h);
            g.setColor(COL_BORDER);
            g.setStroke(new BasicStroke(2));
            g.drawRect(x, y, w, h);
            g.setColor(Color.decode("#222222"));
            g.setFont(new Font("Segoe UI", Font.BOLD, 9));
            FontMetrics fm = g.getFontMetrics();
            String lbl = room.getLabel().toUpperCase();
            g.drawString(lbl, x + (w - fm.stringWidth(lbl)) / 2, y + 15);

            g.setColor(Color.decode(Config.COLOR_MUTED));
            g.setFont(new Font("Segoe UI", Font.PLAIN, 7));
            String[] objs = room.getObjects();
            for (int j = 0; j < Math.min(4, objs.length); j++) {
                int ox = x + 10 + (j % 2) * 80;
                int oy = y + 33 + (j / 2) * 25;
                g.fillOval(ox - 3, oy - 3, 6, 6);
                g.drawString(objs[j], ox + 7, oy + 3);
            }
        }

        int rx = (int) robotX, ry = (int) robotY, r = 12;
        g.setColor(Color.decode(Config.COLOR_ROBOT).darker());
        g.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT,
                     BasicStroke.JOIN_MITER, 1, new float[]{4,3}, 0));
        g.drawOval(rx-r-5, ry-r-5, (r+5)*2, (r+5)*2);
        g.setStroke(new BasicStroke(2));
        g.setColor(Color.decode(Config.COLOR_ROBOT));
        g.fillOval(rx-r, ry-r, r*2, r*2);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2));
        g.drawOval(rx-r, ry-r, r*2, r*2);
        g.setFont(new Font("Segoe UI", Font.BOLD, 10));
        g.drawString("R", rx - 4, ry + 4);
    }

    private JPanel buildAgentsCard() {
        JPanel card = card("[BOT]  État des Agents");
        JPanel row  = new JPanel(new GridLayout(1, 5, 4, 0));
        row.setBackground(BG_CARD);
        row.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        String[] labels = {"AIH", "ACT", "AN", "AM", "AS"};
        Color[]  colors = {COL_PURPLE, COL_BLUE, COL_GREEN,
                           COL_YELLOW, Color.decode("#FF6B9D")};
        agentLabels = new JLabel[labels.length];

        for (int i = 0; i < labels.length; i++) {
            JPanel f = agentCard(labels[i], colors[i]);
            agentLabels[i] = (JLabel) f.getClientProperty("etat");
            row.add(f);
        }
        card.add(row, BorderLayout.CENTER);
        return card;
    }

    private JPanel agentCard(String label, Color color) {
        JPanel f = new JPanel();
        f.setLayout(new BoxLayout(f, BoxLayout.Y_AXIS));
        f.setBackground(BG_CARD);
        f.setBorder(BorderFactory.createLineBorder(COL_BORDER));

        JPanel band = new JPanel();
        band.setBackground(COL_BORDER);
        band.setPreferredSize(new Dimension(0, 3));
        band.setMaximumSize(new Dimension(Integer.MAX_VALUE, 3));
        f.add(band);

        JLabel lbl = new JLabel(label, SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(color);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        f.add(lbl);

        JLabel dot = new JLabel("o", SwingConstants.CENTER);
        dot.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        dot.setForeground(COL_MUTED);
        dot.setAlignmentX(Component.CENTER_ALIGNMENT);
        f.add(dot);

        JLabel etat = new JLabel("Inactif", SwingConstants.CENTER);
        etat.setFont(new Font("Segoe UI", Font.PLAIN, 7));
        etat.setForeground(COL_MUTED);
        etat.setAlignmentX(Component.CENTER_ALIGNMENT);
        f.add(etat);
        f.add(Box.createVerticalStrut(5));

        f.putClientProperty("band",  band);
        f.putClientProperty("dot",   dot);
        f.putClientProperty("etat",  etat);
        f.putClientProperty("color", color);
        return f;
    }

    private JPanel buildRight() {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBackground(BG_DARK);

        p.add(buildControlesCard(), BorderLayout.NORTH);

        JSplitPane pv = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        pv.setDividerSize(5);
        pv.setBackground(BG_DARK);
        pv.setBorder(null);

        JSplitPane pv2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        pv2.setDividerSize(5);
        pv2.setBackground(BG_DARK);
        pv2.setBorder(null);
        pv2.setTopComponent(buildTachesCard());
        pv2.setBottomComponent(buildLogsCard());
        pv2.setDividerLocation(170);

        pv.setTopComponent(pv2);
        pv.setBottomComponent(buildStatsCard());
        pv.setDividerLocation(560);

        p.add(pv, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildControlesCard() {
        JPanel card = card("[CTRL]  Contrôles");

        JPanel g = new JPanel(new GridBagLayout());
        g.setBackground(BG_CARD);
        g.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 3, 3, 3);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx=0; gbc.gridy=0;
        g.add(labelMuted("Type :"), gbc);
        cbType = new JComboBox<>(new String[]{
            "livraison","nettoyage","urgence",
            "guidage","surveillance","récupération"});
        styleCombo(cbType);
        gbc.gridx=1;
        g.add(cbType, gbc);

        gbc.gridx=2;
        g.add(labelMuted("Pièce :"), gbc);
        cbPiece = new JComboBox<>(new String[]{
            "cuisine","salon","chambre","salle_de_bain"});
        styleCombo(cbPiece);
        gbc.gridx=3;
        g.add(cbPiece, gbc);

        gbc.gridx=0; gbc.gridy=1;
        g.add(labelMuted("Urgence :"), gbc);
        sliderUrgence = new JSlider(1, 10, 5);
        sliderUrgence.setBackground(BG_CARD);
        sliderUrgence.setForeground(COL_TEXT);
        sliderUrgence.setPreferredSize(new Dimension(160, 25));
        gbc.gridx=1;
        g.add(sliderUrgence, gbc);
        lblUrgVal = label("5", COL_YELLOW, new Font("Segoe UI", Font.BOLD, 11));
        sliderUrgence.addChangeListener(e ->
            lblUrgVal.setText(String.valueOf(sliderUrgence.getValue())));
        gbc.gridx=2;
        g.add(lblUrgVal, gbc);

        gbc.gridx=0; gbc.gridy=2;
        g.add(labelMuted("Desc. :"), gbc);
        tfDesc = new JTextField("Description de la tâche", 30);
        tfDesc.setBackground(BG_DARK);
        tfDesc.setForeground(COL_TEXT);
        tfDesc.setCaretColor(COL_TEXT);
        tfDesc.setBorder(BorderFactory.createEmptyBorder(3,6,3,6));
        tfDesc.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        gbc.gridx=1; gbc.gridwidth=3; gbc.fill=GridBagConstraints.HORIZONTAL;
        g.add(tfDesc, gbc);
        gbc.gridwidth=1; gbc.fill=GridBagConstraints.NONE;

        gbc.gridx=0; gbc.gridy=3; gbc.gridwidth=4;
        JPanel bf = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        bf.setBackground(BG_CARD);

        JButton btnSubmit = iconButton("+ Soumettre", COL_GREEN);
        JButton btnUrg    = iconButton("[URGENCE] URGENCE",  COL_ORANGE);

        btnSubmit.addActionListener(e -> soumettre());
        btnUrg.addActionListener(e -> urgence());

        bf.add(btnSubmit);
        bf.add(btnUrg);
        g.add(bf, gbc);

        card.add(g, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildTachesCard() {
        JPanel card = card("[TASK]  File des Tâches");
        String[] cols = {"ID","Type","Pièce","Urg.","Statut","Score","Agent"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(tableModel);
        table.setBackground(BG_CARD);
        table.setForeground(COL_TEXT);
        table.setGridColor(COL_BORDER);
        table.setFont(new Font("Consolas", Font.PLAIN, 9));
        table.getTableHeader().setBackground(BG_PANEL);
        table.getTableHeader().setForeground(COL_BLUE);
        table.setRowHeight(22);
        table.setShowGrid(true);
        
        // Colorer les lignes selon le statut
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String statut = (String) tableModel.getValueAt(row, 4);
                if (!isSelected) {
                    if ("TERMINEE".equals(statut)) {
                        c.setBackground(new Color(40, 60, 40));
                        c.setForeground(COL_GREEN);
                    } else if ("EN_COURS".equals(statut)) {
                        c.setBackground(new Color(60, 60, 30));
                        c.setForeground(COL_YELLOW);
                    } else if ("ASSIGNEE".equals(statut)) {
                        c.setBackground(new Color(30, 50, 70));
                        c.setForeground(COL_BLUE);
                    } else if ("ECHOUEE".equals(statut)) {
                        c.setBackground(new Color(70, 40, 40));
                        c.setForeground(COL_ORANGE);
                    } else {
                        c.setBackground(BG_CARD);
                        c.setForeground(COL_TEXT);
                    }
                }
                return c;
            }
        });
        
        int[] widths = {55, 90, 85, 40, 88, 55, 50};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        JScrollPane sp = new JScrollPane(table);
        sp.setBackground(BG_CARD);
        sp.getViewport().setBackground(BG_CARD);
        sp.setBorder(BorderFactory.createEmptyBorder());
        card.add(sp, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildLogsCard() {
        JPanel card = card("[LOG]  Journal des Événements");
        logPane = new JTextPane();
        logPane.setBackground(Color.decode("#070B10"));
        logPane.setForeground(COL_TEXT);
        logPane.setFont(new Font("Consolas", Font.PLAIN, 9));
        logPane.setEditable(false);
        logPane.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        JScrollPane sp = new JScrollPane(logPane);
        sp.setBackground(Color.decode("#070B10"));
        sp.getViewport().setBackground(Color.decode("#070B10"));
        sp.setBorder(BorderFactory.createEmptyBorder());
        card.add(sp, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildStatsCard() {
        JPanel card = card("[STATS]  Statistiques");
        JPanel row = new JPanel(new GridLayout(1, 4, 4, 0));
        row.setBackground(BG_CARD);
        row.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        lblCreees   = statLabel("0", "Créées",   COL_TEXT);
        lblReussies = statLabel("0", "Réussies", COL_GREEN);
        lblEchouees = statLabel("0", "Échouées", COL_ORANGE);
        lblMessages = statLabel("0", "Messages", COL_BLUE);

        for (JLabel l : new JLabel[]{lblCreees, lblReussies,
                                      lblEchouees, lblMessages}) {
            JPanel f = new JPanel();
            f.setLayout(new BoxLayout(f, BoxLayout.Y_AXIS));
            f.setBackground(BG_CARD);
            f.setBorder(BorderFactory.createLineBorder(COL_BORDER));
            f.add(l);
            row.add(f);
        }
        card.add(row, BorderLayout.CENTER);
        return card;
    }

    private JLabel statLabel(String val, String lbl, Color clr) {
        JLabel l = new JLabel(val + "\n" + lbl, SwingConstants.CENTER);
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        l.setForeground(clr);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        return l;
    }

    private void buildStatusBar() {
        JPanel sb = new JPanel(new BorderLayout());
        sb.setBackground(BG_PANEL);
        sb.setPreferredSize(new Dimension(0, 22));
        sb.setBorder(BorderFactory.createMatteBorder(1,0,0,0, COL_BORDER));

        lblStatus = label("*  Système arrêté", COL_MUTED,
                          new Font("Segoe UI", Font.PLAIN, 8));
        sb.add(lblStatus, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 2));
        right.setBackground(BG_PANEL);
        lblRobot = label("Robot : salon", COL_BLUE,
                         new Font("Segoe UI", Font.PLAIN, 8));
        lblClock = label("", COL_MUTED,
                         new Font("Segoe UI", Font.PLAIN, 8));
        right.add(lblRobot);
        right.add(lblClock);
        sb.add(right, BorderLayout.EAST);
        add(sb, BorderLayout.SOUTH);
    }

    // ==============================================
    // MISES À JOUR UI (thread-safe via SwingUtilities)
    // ==============================================

    public void onSystemStarted() {
        lblStatus.setText("*  Système actif - agents prêts");
        lblStatus.setForeground(COL_GREEN);
    }

    public void updateRobotPosition(double x, double y, String piece) {
        SwingUtilities.invokeLater(() -> {
            robotX = x;
            robotY = y;
            canvasPanel.repaint();
            String lbl = Config.ROOMS.containsKey(piece)
                ? Config.ROOMS.get(piece).getLabel() : piece;
            lblRobot.setText("Robot : " + lbl);
        });
    }

    /**
     * Ajoute ou met à jour une tâche dans le tableau
     * Gère tous les statuts: EN_ATTENTE, ASSIGNEE, EN_COURS, TERMINEE, ECHOUEE
     */
    public void addTaskRow(String id, String type, String piece,
                           int urgence, String statut,
                           double score, String agent) {
        SwingUtilities.invokeLater(() -> {
            boolean exists = false;
            int existingRow = -1;
            String oldStatut = "";
            
            // Chercher la ligne existante
            for (int r = 0; r < tableModel.getRowCount(); r++) {
                Object idValue = tableModel.getValueAt(r, 0);
                if (idValue != null && idValue.equals(id)) {
                    exists = true;
                    existingRow = r;
                    oldStatut = (String) tableModel.getValueAt(r, 4);
                    break;
                }
            }
            
            if (exists) {
                // Mise à jour de la ligne existante
                tableModel.setValueAt(type,   existingRow, 1);
                tableModel.setValueAt(piece,  existingRow, 2);
                tableModel.setValueAt(urgence, existingRow, 3);
                tableModel.setValueAt(statut, existingRow, 4);
                tableModel.setValueAt(String.format("%.1f", score), existingRow, 5);
                tableModel.setValueAt(agent,  existingRow, 6);
                
                // Mettre à jour les compteurs
                if (!oldStatut.equals("TERMINEE") && statut.equals("TERMINEE")) {
                    totalReussies++;
                    log("[STATS] Tâche " + id + " réussie !", "SUCCESS");
                }
                if (!oldStatut.equals("ECHOUEE") && statut.equals("ECHOUEE")) {
                    totalEchouees++;
                    log("[STATS] Tâche " + id + " échouée !", "ERROR");
                }
                if (!oldStatut.equals("EN_COURS") && statut.equals("EN_COURS")) {
                    log("[STATS] Tâche " + id + " en cours d'exécution...", "INFO");
                }
                
                System.out.println("[GUI] Tâche " + id + " : " + oldStatut + " → " + statut);
            } else {
                // Nouvelle tâche
                tableModel.insertRow(0, new Object[]{
                    id, type, piece, urgence, statut,
                    String.format("%.1f", score), agent});
                totalCrees++;
                System.out.println("[GUI] Tâche " + id + " : " + statut + " (nouvelle)");
            }
            
            updateStatsDisplay();
        });
    }

    public void log(String msg, String niveau) {
        SwingUtilities.invokeLater(() -> {
            totalMessages++;
            updateStatsDisplay();
            
            String time = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
            Color clr;
            if      ("WARNING".equals(niveau)) clr = COL_YELLOW;
            else if ("ERROR"  .equals(niveau)) clr = COL_ORANGE;
            else if ("SUCCESS".equals(niveau)) clr = COL_GREEN;
            else if ("DEBUG"  .equals(niveau)) clr = COL_MUTED;
            else                               clr = COL_TEXT;
            appendLog("[" + time + "] " + msg + "\n", clr);
        });
    }

    private void appendLog(String text, Color color) {
        try {
            javax.swing.text.StyledDocument doc = logPane.getStyledDocument();
            javax.swing.text.SimpleAttributeSet attrs =
                new javax.swing.text.SimpleAttributeSet();
            javax.swing.text.StyleConstants.setForeground(attrs, color);
            javax.swing.text.StyleConstants.setFontFamily(attrs, "Consolas");
            javax.swing.text.StyleConstants.setFontSize(attrs, 9);
            doc.insertString(doc.getLength(), text, attrs);
            logPane.setCaretPosition(doc.getLength());
        } catch (Exception e) { }
    }

    private void updateStatsDisplay() {
        lblCreees.setText(totalCrees + " Créées");
        lblReussies.setText(totalReussies + " Réussies");
        lblEchouees.setText(totalEchouees + " Échouées");
        lblMessages.setText(totalMessages + " Messages");
    }

    // ==============================================
    // ACTIONS BOUTONS
    // ==============================================

    private void soumettre() {
        if (!HumanInteractionAgent.isReady()) {
            JOptionPane.showMessageDialog(this,
                "Le système JADE démarre automatiquement.\nAttendez 1-2 secondes et réessayez.", "Patience...",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        String type   = (String) cbType.getSelectedItem();
        String piece  = (String) cbPiece.getSelectedItem();
        int    urgence = sliderUrgence.getValue();
        String desc   = tfDesc.getText().trim();
        if (desc.isEmpty()) desc = "Tâche " + type;

        Task task = new Task(type, desc, piece, urgence);
        HumanInteractionAgent.getInstance().soumettreTache(task);
        log("[IN] Tâche soumise : " + task, "INFO");
    }

    private void urgence() {
        if (!HumanInteractionAgent.isReady()) {
            JOptionPane.showMessageDialog(this,
                "Le système JADE démarre automatiquement.\nAttendez 1-2 secondes et réessayez.", "Patience...",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        String piece = (String) cbPiece.getSelectedItem();
        HumanInteractionAgent.getInstance().signalerUrgence(piece, "[URGENCE] Urgence dans " + piece);
    }

    // ==============================================
    // UTILITAIRES
    // ==============================================

    private static JPanel card(String titre) {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(Color.decode(Config.COLOR_BORDER));
        outer.setBorder(BorderFactory.createLineBorder(
            Color.decode(Config.COLOR_BORDER), 1));

        JPanel inner = new JPanel(new BorderLayout());
        inner.setBackground(BG_CARD);

        JLabel lbl = new JLabel(titre);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 9));
        lbl.setForeground(COL_BLUE);
        lbl.setBorder(BorderFactory.createEmptyBorder(5, 8, 2, 8));
        inner.add(lbl, BorderLayout.NORTH);
        inner.add(new JSeparator() {{
            setForeground(Color.decode(Config.COLOR_BORDER));
        }}, BorderLayout.CENTER);

        outer.add(inner, BorderLayout.CENTER);
        return inner;
    }

    private static JButton iconButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Segoe UI", Font.BOLD, 9));
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setMargin(new Insets(3, 11, 3, 11));
        return b;
    }

    private static JLabel label(String text, Color fg, Font font) {
        JLabel l = new JLabel(text);
        l.setForeground(fg);
        l.setFont(font);
        return l;
    }

    private static JLabel labelMuted(String text) {
        return label(text, COL_MUTED, new Font("Segoe UI", Font.PLAIN, 8));
    }

    private static void styleCombo(JComboBox<?> cb) {
        cb.setBackground(BG_DARK);
        cb.setForeground(COL_TEXT);
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 9));
    }

    private static Color hex(String hex) {
        try { return Color.decode(hex); }
        catch (Exception e) { return Color.GRAY; }
    }
}