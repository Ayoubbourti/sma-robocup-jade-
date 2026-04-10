package robocup;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import robocup.gui.DashboardGUI;
import robocup.utils.Config;

public class Main {

    private static DashboardGUI gui;

    public static void main(String[] args) throws Exception {

        System.out.println("+======================================+");
        System.out.println("|   SMA RoboCup@Home - JADE v4.x       |");
        System.out.println("+======================================+");

        // ── 1. Ouvrir la GUI d'abord ───────────────
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            gui = new DashboardGUI();
            gui.log("[OK] Interface graphique démarrée", "SUCCESS");
        });

        // ── 2. Démarrer le Runtime JADE ─────────────
        Runtime rt = Runtime.instance();
        rt.setCloseVM(true);

        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.MAIN_HOST, "localhost");
        profile.setParameter(Profile.MAIN_PORT, "1099");

        AgentContainer container = rt.createMainContainer(profile);
        System.out.println("[Main] Container JADE démarré.");
        gui.log("[Main] Container JADE démarré", "INFO");

        // ── 3. Lancer les agents ────────────────────
        AgentController aihCtrl = container.createNewAgent(
            Config.AGENT_AIH,
            "robocup.agents.HumanInteractionAgent",
            new Object[]{gui});  // ← Passer la GUI à AIH
        aihCtrl.start();
        Thread.sleep(100);

        AgentController actCtrl = container.createNewAgent(
            Config.AGENT_ACT,
            "robocup.agents.TaskCoordinatorAgent",
            new Object[]{gui});  // ← Passer la GUI à ACT
        actCtrl.start();
        Thread.sleep(100);

        AgentController anCtrl = container.createNewAgent(
            Config.AGENT_AN,
            "robocup.agents.NavigationAgent",
            new Object[]{gui});  // ← Passer la GUI à AN
        anCtrl.start();
        Thread.sleep(100);

        AgentController amCtrl = container.createNewAgent(
            Config.AGENT_AM,
            "robocup.agents.ManipulationAgent",
            new Object[]{gui});  // ← Passer la GUI à AM
        amCtrl.start();
        Thread.sleep(100);

        AgentController asCtrl = container.createNewAgent(
            Config.AGENT_AS,
            "robocup.agents.SurveillanceAgent",
            new Object[]{gui});  // ← Passer la GUI à AS
        asCtrl.start();
        Thread.sleep(200);

        System.out.println("[Main] 5 agents démarrés.");
        gui.log("[OK] Système JADE démarré - 5 agents actifs", "SUCCESS");
        gui.log("Agents : AIH, ACT, AN, AM, AS", "INFO");
        gui.log("Protocole Contract-Net FIPA activé OK", "SUCCESS");
        gui.onSystemStarted();
    }
}