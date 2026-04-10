# 🤖 SMA RoboCup@Home — JADE

> Projet universitaire de Master — Systèmes Multi-Agents  
> Implémentation avec la plateforme **JADE (Java Agent DEvelopment Framework)**

---

## 📋 Présentation

Ce projet implémente un Système Multi-Agents pour **RoboCup@Home** en utilisant
la plateforme JADE, qui est conforme au standard **FIPA** (Foundation for
Intelligent Physical Agents).

### Différences clés avec une implémentation Python from-scratch

| Aspect | Python from-scratch | JADE |
|---|---|---|
| Language | Python 3 | Java 11+ |
| Messages | Dataclass custom | `ACLMessage` FIPA standard |
| Contract-Net | Codé manuellement | `ContractNetInitiator/Responder` |
| Threads | `threading.Thread` | Behaviours JADE (scheduler interne) |
| Annuaire | `Environment` custom | DF (Directory Facilitator) JADE |
| Monitoring | GUI custom Tkinter | RMA + Sniffer JADE intégrés |

---

## 🏗️ Architecture

```
sma_robocup_jade/
├── src/robocup/
│   ├── agents/
│   │   ├── HumanInteractionAgent.java   # AIH
│   │   ├── TaskCoordinatorAgent.java    # ACT + ContractNetInitiator
│   │   ├── NavigationAgent.java         # AN  + ContractNetResponder
│   │   ├── ManipulationAgent.java       # AM  + ContractNetResponder + file attente
│   │   └── SurveillanceAgent.java       # AS  + TickerBehaviour
│   ├── behaviours/
│   │   ├── TaskExecutionBehaviour.java  # Behaviour abstrait de base
│   │   └── SurveillanceBehaviour.java   # TickerBehaviour scan périodique
│   ├── models/
│   │   ├── Task.java                    # Modèle de tâche
│   │   ├── TaskStatus.java              # Enum des états
│   │   └── Room.java                    # Modèle de pièce
│   ├── gui/
│   │   └── DashboardGUI.java            # Interface Swing dark-mode
│   ├── utils/
│   │   ├── Config.java                  # Configuration centrale
│   │   └── PriorityCalculator.java      # Score pondéré
│   └── Main.java                        # Point d'entrée
├── lib/
│   └── jade.jar                         # ← À télécharger (voir ci-dessous)
├── build.xml                            # Script Apache Ant
└── README.md
```

---

## 🚀 Installation et lancement

### Prérequis
- **Java 11+** (JDK)
- **Apache Ant** (optionnel mais recommandé)

### 1. Télécharger JADE

Télécharger `jade.jar` depuis le site officiel :
```
https://jade.tilab.com/download/jade/
```
Placer le fichier dans le dossier `lib/` :
```
sma_robocup_jade/lib/jade.jar
```

### 2. Compiler

```bash
# Avec Ant (recommandé)
ant compile

# Ou manuellement
mkdir out
javac -cp "lib/jade.jar" -d out -encoding UTF-8 \
      $(find src -name "*.java")
```

### 3. Lancer

```bash
# Avec Ant
ant run

# Ou manuellement
java -cp "lib/jade.jar:out" robocup.Main

# Windows
java -cp "lib/jade.jar;out" robocup.Main
```

### 4. Lancer avec la GUI JADE (Sniffer, DF Browser, RMA)

```bash
ant run-jade-gui
```

Cela ouvre le **Remote Management Agent (RMA)** de JADE avec lequel tu peux :
- Voir tous les agents actifs
- Utiliser le **Sniffer** pour espionner les messages ACL
- Utiliser le **Introspector** pour voir les behaviours

---

## 🔬 Concepts JADE utilisés

### Behaviours

| Behaviour JADE | Utilisé par | Rôle |
|---|---|---|
| `CyclicBehaviour` | AIH, ACT, AM | Écoute permanente des messages |
| `OneShotBehaviour` | AN, AM | Exécution unique (navigation, manipulation) |
| `TickerBehaviour` | AS | Surveillance périodique (toutes les 10s) |
| `WakerBehaviour` | ACT | Réessai CFP après délai |
| `ContractNetInitiator` | ACT | Lance le protocole Contract-Net (CFP) |
| `ContractNetResponder` | AN, AM | Répond aux CFP (PROPOSE/REFUSE) |

### Messages ACL (FIPA)

```
Performatifs utilisés :
  REQUEST        → AIH → ACT (nouvelle tâche)
  CFP            → ACT → AN, AM (appel d'offres)
  PROPOSE        → AN/AM → ACT (offre)
  REFUSE         → AN/AM → ACT (incompatibilité)
  ACCEPT_PROPOSAL→ ACT → agent gagnant
  REJECT_PROPOSAL→ ACT → agents perdants
  CONFIRM        → AM/AN → ACT → AIH (succès)
  FAILURE        → AM/AN → ACT → AIH (échec)
  INFORM         → AS → AIH (anomalie détectée)
                   AN → ACT (position robot)
                   AN → GUI (animation)
```

### Protocole Contract-Net (FIPA standard)

```
ACT (Initiateur)                AN / AM (Répondeurs)
     │                               │
     ├──── CFP ──────────────────────►
     │                               │
     │◄─── PROPOSE (score) ──────────┤
     │◄─── PROPOSE (score) ──────────┤
     │                               │
     ├──── ACCEPT_PROPOSAL ──────────►  (gagnant)
     ├──── REJECT_PROPOSAL ──────────►  (perdants)
     │                               │
     │◄─── INFORM (résultat) ────────┤
```

---

## 🎯 Scénarios de test

Ajouter des tâches depuis la GUI ou modifier `Main.java` :

```java
// Exemple : soumettre une urgence au démarrage
Task urgence = new Task("urgence", "Chute détectée", "salle_de_bain", 10);
// Récupérer l'AIH via AgentController et appeler soumettreTache()
```

---

## 📊 Monitoring avec JADE

Une fois lancé avec `ant run-jade-gui`, tu peux :

1. **RMA** → voir les 5 agents et leur état
2. **Sniffer** → capturer tous les messages ACL échangés en temps réel
3. **DF Browser** → voir les services enregistrés
4. **Introspector** → inspecter les behaviours actifs de chaque agent

---

## Auteur

Projet universitaire Master Informatique — Systèmes Multi-Agents  
Plateforme : JADE 4.x — https://jade.tilab.com
