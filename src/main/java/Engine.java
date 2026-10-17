import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Box;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.text.Font;
import javafx.util.Duration;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

/**
 * Snake3D 2.0 upgrade layer ("Engine").
 * Lives beside the proven Main game core and adds:
 *   profile/progression persistence, cinematic intro, modern menu system,
 *   7 game modes, 6 arenas, extended collectibles, personalities-driven extra
 *   villain AI + bosses, HUD (minimap, power-ups, missions), pause/result screens.
 *
 * Engine is package-private and reaches Main's package-private fields directly.
 */
final class Engine {

    // ======================================================================
    //  SECTION 1 — PROFILE, STATISTICS, MISSIONS / ACHIEVEMENT DEFINITIONS
    // ======================================================================

    static final class GameProfile {
        int level = 1;
        int xp = 0;
        int coins = 0;
        String skin = "CLASSIC";
        String arena = "NEON";
        String quality = "MEDIUM";     // LOW / MEDIUM / HIGH
        boolean fullscreen = false;
        boolean shake = true;
        boolean skipIntro = false;
        double musicVol = 1.0;
        double sfxVol = 1.0;
        Set<String> unlockedSkins = new LinkedHashSet<>(List.of("CLASSIC", "LIME", "NEON", "OCEAN"));
        Map<String, Integer> bestScores = new HashMap<>();   // mode name -> best score
        Map<String, Integer> bestExtra = new HashMap<>();    // mode name -> tidal stat (waves, coins, boss wins)
        int totalFruit = 0;
        int totalGolden = 0;
        int totalTimeSec = 0;
        int maxLength = 0;
        int totalDeaths = 0;
        int bossWins = 0;
        int treasuresFound = 0;
        int villainKills = 0;
        List<String> achDone = new ArrayList<>();
        List<String> missionsDone = new ArrayList<>();

        int xpToNext() { return 200 * Math.max(1, level); }

        boolean addXp(int amount, List<String> toasts) {
            xp += Math.max(0, amount);
            boolean leveled = false;
            while (xp >= xpToNext()) {
                xp -= xpToNext();
                level++;
                coins += 50 * level;
                leveled = true;
                if (toasts != null) toasts.add("LEVEL UP! Level " + level + " — reward " + (50 * level) + " coins");
            }
            return leveled;
        }
    }

    static final class MissionDef {
        final String id;
        final String desc;
        final int target;
        MissionDef(String id, String desc, int target) { this.id = id; this.desc = desc; this.target = target; }
        int progress(GameProfile p) {
            switch (id) {
                case "m1": return p.totalFruit;
                case "m2": return p.totalTimeSec / 300;         // 5-minute units
                case "m3": return p.totalGolden;
                case "m4": return p.villainKills;
                case "m5": return p.maxLength;
                case "m6": return p.bossWins;
                case "m7": return p.treasuresFound;
                case "m8": return p.bestExtra.getOrDefault("WAVE", 0);
                case "m9": return p.bestScores.getOrDefault("TIME", 0);
                case "m10": return p.level - 1;
            }
            return 0;
        }
        int rewardCoins(GameProfile p) {
            switch (id) {
                case "m1": return 100;
                case "m2": return 40;
                case "m3": return 50;
                case "m4": return 200;
                case "m5": return 30;
                case "m6": return 250;
                case "m7": return 150;
                case "m8": return 300;
                case "m9": return 200;
                case "m10": return 60;
            }
            return 25;
        }
    }

    static final List<MissionDef> MISSIONS = List.of(
            new MissionDef("m1", "Eat 50 food items (lifetime)", 50),
            new MissionDef("m2", "Survive 5 minutes total", 5),
            new MissionDef("m3", "Collect 10 golden fruits", 10),
            new MissionDef("m4", "Defeat 15 villain snakes", 15),
            new MissionDef("m5", "Reach a body length of 20", 20),
            new MissionDef("m6", "Win 1 Boss Battle", 1),
            new MissionDef("m7", "Find 20 treasure coins", 20),
            new MissionDef("m8", "Survive 3 waves in Wave Survival", 3),
            new MissionDef("m9", "Score 40 in Time Challenge", 40),
            new MissionDef("m10", "Reach level 5", 5)
    );

    static final List<String[]> ACHIEVEMENTS = List.of(
            new String[] { "a1", "First Bite",  "eat your very first fruit" },
            new String[] { "a2", "Speed Demon", "eat 3 golden fruits" },
            new String[] { "a3", "Untouchable", "survive 3 minutes in one run" },
            new String[] { "a4", "Colossus",    "grow to 15 segments" },
            new String[] { "a5", "Snake Master","reach level 3" },
            new String[] { "a6", "Boss Slayer", "defeat the Venom Monarch" },
            new String[] { "a7", "Treasure Hunter", "gather a full treasure hoard" },
            new String[] { "a8", "Livestock",   "find a Legendary Star" },
            new String[] { "a9", "Snake Slayer","defeat 10 villain snakes" },
            new String[] { "a10","Predator",    "win Battle Arena" }
    );

    // ======================================================================
    //  SECTION 2 — SKINS / ARENAS / MODES / ITEM CATALOGS
    // ======================================================================

    static final class SkinDef {
        final String id, name, rarity;
        final int unlockLevel;
        final int price;
        final List<Color> body;
        final Color head, glow;
        final boolean trail;
        SkinDef(String id, String name, String rarity, int unlockLevel, int price,
                List<Color> body, Color head, Color glow, boolean trail) {
            this.id = id; this.name = name; this.rarity = rarity; this.unlockLevel = unlockLevel;
            this.price = price; this.body = body; this.head = head; this.glow = glow; this.trail = trail;
        }
        static final String COM = "Common";
    }

    static final List<SkinDef> SKINS = List.of(
        new SkinDef("CLASSIC", "Classic",  "Common",    0, 0,
                List.of(Color.web("#2ECC71")), Color.web("#2ECC71"), Color.web("#1E8449"), false),
        new SkinDef("LIME",    "Lime",     "Common",    0, 0,
                List.of(Color.YELLOWGREEN), Color.LIMEGREEN, Color.web("#32CD32"), false),
        new SkinDef("NEON",    "Neon",     "Rare",      0, 0,
                List.of(Color.web("#00FFFF"), Color.web("#00BFFF")), Color.web("#00FFFF"), Color.web("#00CCFF"), true),
        new SkinDef("OCEAN",   "Ocean",    "Common",    2, 150,
                List.of(Color.LIGHTBLUE, Color.ROYALBLUE, Color.DEEPSKYBLUE), Color.ROYALBLUE, Color.DEEPSKYBLUE, true),
        new SkinDef("FIRE",    "Fire",     "Rare",      2, 250,
                List.of(Color.web("#FF6B00"), Color.web("#FF2200"), Color.web("#FFAA00")), Color.web("#FFEE44"), Color.ORANGE, true),
        new SkinDef("ICE",     "Ice",      "Rare",      3, 350,
                List.of(Color.web("#B8F3FF"), Color.web("#64D8FF"), Color.web("#E8FBFF")), Color.web("#A9F5FF"), Color.web("#66DDFF"), true),
        new SkinDef("RAINBOW", "Rainbow",  "Epic",      3, 400,
                List.of(Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.BLUE, Color.VIOLET), Color.WHITE, Color.WHITE, true),
        new SkinDef("SHADOW",  "Shadow",   "Epic",      4, 500,
                List.of(Color.web("#150A1E"), Color.web("#33113F"), Color.web("#52175e")), Color.web("#7A1FA2"), Color.PURPLE, true),
        new SkinDef("COSMIC",  "Cosmic",   "Legendary", 5, 600,
                List.of(Color.DARKVIOLET, Color.ROYALBLUE, Color.MEDIUMVIOLETRED, Color.WHITE), Color.WHITE, Color.WHITE, true),
        new SkinDef("GOLD",    "Golden",   "Legendary", 6, 800,
                List.of(Color.web("#FFD700"), Color.web("#FFA400"), Color.web("#FFF1A0")), Color.web("#FFE870"), Color.GOLD, true)
    );

    private static SkinDef skinDef(String id) {
        for (SkinDef s : SKINS) if (s.id.equals(id)) return s;
        return SKINS.get(0);
    }

    enum Arena {
        NEON("Neon Cyber Arena", Color.web("#050014"), Color.web("#2A0B5E"), Color.web("#00E5FF")),
        JUNGLE("Tropical Jungle", Color.web("#04170a"), Color.web("#0B4D1A"), Color.web("#7CFD96")),
        DESERT("Desert Ruins", Color.web("#231204"), Color.web("#8A5A12"), Color.web("#FFC680")),
        FROZEN("Frozen World", Color.web("#08131f"), Color.web("#2C6C9E"), Color.web("#cEF6FF")),
        VOLCANIC("Volcanic Arena", Color.web("#1a0400"), Color.web("#902000"), Color.web("#FF7A3D")),
        COSMIC("Cosmic Arena", Color.web("#02010d"), Color.web("#311066"), Color.web("#C99CFF"));
        final String title;
        final Color skyTop, skyBottom, accent;
        Arena(String t, Color a, Color b, Color c) { title = t; skyTop = a; skyBottom = b; accent = c; }
        static Arena of(String id) {
            for (Arena a : values()) if (a.name().equalsIgnoreCase(id)) return a;
            return NEON;
        }
    }

    enum GameMode {
        CLASSIC("Classic Survival", "Eat fruit, grow huge and dodge three hostile AI snakes.", 0, 3, 0.0),
        ARENA("Battle Arena", "A crowded free-for-all. Extra hunters join the arena!", 2, 3, 0.6),
        BOSS("Boss Battle", "Face the colossal VENOM MONARCH for 90 seconds.", 1, 0, 0.35),
        TIME("Time Challenge", "Score as much as possible in 120 seconds.", 1, 2, 0.2),
        ENDLESS("Endless Growth", "The arena keeps getting harder. Survive and grow.", 2, 4, 0.4),
        TREASURE("Treasure Hunt", "Find 12 hidden treasure coins before the rivals.", 1, 3, 0.25),
        WAVE("Wave Survival", "Endless enemy waves — how long can you last?", 1, 2, 0.3);
        final String title, desc;
        final int startExtras, baseVillains;
        final double extraScale;
        GameMode(String t, String d, int se, int bv, double es) {
            title = t; desc = d; startExtras = se; baseVillains = bv; extraScale = es;
        }
    }

    enum ItemKind {
        FRUIT("Fruit", "+1 point"),
        GOLDEN("Golden Fruit", "Speed boost 10s"),
        COIN("Coin", "+5 coins"),
        GEM("Gem", "+10 coins"),
        CRYSTAL("Rare Crystal", "+25 XP"),
        SPEED_ORB("Speed Orb", "Speed up 6s"),
        SHIELD("Shield", "Invincible 6s"),
        MAGNET("Magnet", "Food magnet 8s"),
        DOUBLE("Double Score", "2x points 10s"),
        GROWTH("Growth Booster", "+3 length"),
        SHRINK("Shrink Potion", "-2 length"),
        HEART("Heart", "+2 length +XP"),
        MYSTERY("Mystery Box", "Random reward"),
        STAR("Legendary Star", "GRAND PRIZE"),
        BOMB("Venom Bomb", "DANGER! -5"),
        TRAP("Venom Trap", "DANGER! slows you");
        final String label, effect;
        ItemKind(String l, String e) { label = l; effect = e; }
    }

    /** One floating collectible in the arena. */
    static final class ItemDef {
        final ItemKind kind;
        final javafx.scene.Node node;
        final double pulse;
        ItemDef(ItemKind k, javafx.scene.Node n) {
            kind = k; node = n;
            pulse = Math.random() * 6.28;
        }
    }

    // ======================================================================
    //  SECTION 3 — SCALABLE VILLAIN (personality AI) + BOSS
    // ======================================================================

    static final class SVillain {
        double x, y, speed, dir = 0;
        int cooldown = 0;
        double size = 1.0;
        List<Color> palette;
        List<Sphere> body = new ArrayList<>();
        Sphere head;
        boolean alive = true, boss = false;
        String name = "Hunter";
        double chase = 0.35;      // move toward player head when moderate distance
        double greedy = 0.5;      // seek food aggressively
        double flee = 0.15;       // steer away when very close to player
        int hp = 1;
        int fruitsEaten = 0;
        double lungeCd = 0;
        boolean respawnPending = false;
        List<double[]> hist = new ArrayList<>();
        private final Group headGrp;

        SVillain(double sx, double sy, double spd, List<Color> pal, boolean isBoss, String n) {
            x = sx; y = sy; speed = spd; palette = pal; boss = isBoss; name = n;
            size = isBoss ? 1.9 : 0.8 + Math.random() * 0.35;
            if (isBoss) {
                chase = 0.9; greedy = 0.3; flee = 0.0;
                hp = 3;
            } else {
                double r = Math.random();
                if (r < 0.3)       { chase = 0.65; greedy = 0.15; flee = 0.10; name = "Stalker"; }   // aggressive
                else if (r < 0.55) { chase = 0.10; greedy = 0.85; flee = 0.10; name = "Scavenger"; } // opportunistic
                else if (r < 0.78) { chase = 0.20; greedy = 0.40; flee = 0.55; name = "Wanderer"; } // defensive
                else               { chase = 0.30; greedy = 0.45; flee = 0.15; speed *= 1.35; name = "Sprinter"; } // fast
            }
            double hr = Main.HEAD_RADIUS;
            head = new Sphere(hr * size);
            PhongMaterial hm = new PhongMaterial(palette.get(0));
            hm.setSpecularColor(palette.size() > 1 ? palette.get(1) : Color.WHITE);
            head.setMaterial(hm);
            Sphere e1 = new Sphere(3.2 * size * 1.6); e1.setTranslateX(-9 * size); e1.setTranslateY(-9 * size); e1.setTranslateZ(-hr * size);
            Sphere e2 = new Sphere(3.2 * size * 1.6); e2.setTranslateX(9 * size);  e2.setTranslateY(-9 * size); e2.setTranslateZ(-hr * size);
            PhongMaterial em = new PhongMaterial(Color.WHITE);
            em.setSpecularColor(Color.WHITE);
            e1.setMaterial(em); e2.setMaterial(em);
            Cylinder tongue = new Cylinder(1.6, hr * 1.1);
            tongue.setMaterial(new PhongMaterial(Color.web("#FF2D6F")));
            tongue.setTranslateY(2);
            tongue.setTranslateZ(-hr * size * 1.4);
            if (isBoss) {
                e1.setRadius(hr * 0.5); e2.setRadius(hr * 0.5);
                e1.setTranslateY(-hr * 0.6); e2.setTranslateY(-hr * 0.6);
                e1.setTranslateZ(-hr * 1.2); e2.setTranslateZ(-hr * 1.2);
                tongue.setRadius(3); tongue.setTranslateZ(-hr * 1.9);
            }
            headGrp = new Group(head, e1, e2, tongue);
            headGrp.setTranslateX(x);
            headGrp.setTranslateY(y);
            int segs = boss ? 11 : 5;
            for (int i = 0; i < segs; i++) body.add(null);
            rebuildBodyNow();
        }

        void rebuildBodyNow() {
            int pc = palette.size();
            for (int i = 0; i < body.size(); i++) {
                Sphere s = new Sphere(Main.BODY_RADIUS * size * (boss ? 1.0 - i * 0.03 : 1.0));
                s.setMaterial(new PhongMaterial(boss && i == 0 ? palette.get(0) : palette.get((i / 2 + 1) % pc)));
                body.set(i, s);
            }
        }

        void addTo(Group root) {
            if (!root.getChildren().contains(headGrp)) root.getChildren().add(headGrp);
            for (Sphere s : body) if (s != null && !root.getChildren().contains(s)) root.getChildren().add(s);
        }

        void removeFrom(Group root) {
            root.getChildren().remove(headGrp);
            for (Sphere s : body) if (s != null) root.getChildren().remove(s);
        }

        void place() {
            headGrp.setTranslateX(x);
            headGrp.setTranslateY(y);
        }
    }

    // ======================================================================
    //  SECTION 4 — RUN STATE
    // ======================================================================

    static final class RunState {
        GameMode mode = GameMode.CLASSIC;
        Arena arena = Arena.NEON;
        List<SVillain> extras = new ArrayList<>();
        List<ItemDef> items = new ArrayList<>();
        List<ItemDef> treasure = new ArrayList<>();     // Treasure Hunt coins (separate count)
        Map<String, Long> powerEnds = new LinkedHashMap<>();
        double arenaTime = 0;
        int wave = 1;
        double nextWaveIn = 0;
        double holdToWin = 0;
        int coinsRun = 0, gemsRun = 0, crystalsRun = 0, mysteryBoxes = 0;
        int starsRun = 0;
        int bossHits = 0;
        SVillain boss = null;
        boolean win = false;
        double lastItemSpawn = 0;
        int frame = 0;
        long survivalStart = 0;
        int enemiesOutscored = 0;
        boolean runActive = false;
        boolean paused = false;
        List<String> pendingToasts = new ArrayList<>();
        Random rnd = new Random();
        public double objective;
        double spawnClock = 0;
        int fruitRun = 0;
        int goldenRun = 0;
        double timeLeft = 0;
        double surviveCap = 0;
        int treasureCoins = 0;
        double nextItemIn = 5;
    }

    static RunState run;

    private static void newRun() {
        run = new RunState();
        run.mode = pendingMode;
        run.arena = Arena.of(profile.arena);
        run.survivalStart = System.currentTimeMillis();
    }

    // ======================================================================
    //  SECTION 5 — BOOT / PERSISTENCE
    // ======================================================================

    private static Main mt;
    private static boolean booted = false;
    private static MediaPlayer menuVideo;
    private static AudioClip sClick, sStart, sEat, sOver;

    private static GameProfile profile;

    static GameProfile profile() { return profile; }

    static void boot(Main ref, javafx.stage.Stage stage) {
        if (booted) return;
        booted = true;
        mt = ref;
        stageRef = stage;
        System.out.println("S3D: boot begin");
        loadProfile();
        if (isAndroid()) {
            profile.skipIntro = true;   // intro SubScene is not safe on the graal-android renderer
            System.out.println("S3D: android detected - intro skipped");
        }
        try { sClick = new AudioClip(mt.getClass().getResource("/s1.wav").toExternalForm()); } catch (Throwable ignore) { sClick = null; }
        try { sStart = new AudioClip(mt.getClass().getResource("/gamestart.wav").toExternalForm()); } catch (Throwable ignore) { sStart = null; }
        try { sEat = new AudioClip(mt.getClass().getResource("/eat.wav").toExternalForm()); } catch (Throwable ignore) { sEat = null; }
        try { sOver = new AudioClip(mt.getClass().getResource("/gameover.wav").toExternalForm()); } catch (Throwable ignore) { sOver = null; }
        try {
            saveLastSkinCompat();
        } catch (Throwable ignore) {}
        try { if (profile.fullscreen && stageRef != null) stageRef.setFullScreen(true); } catch (Throwable ignore) {}
        System.out.println("S3D: boot complete");
    }

    /** Detects the gluon graal-android executive (its javafx.platform property is set by the substrate runtime). */
    static boolean isAndroid() {
        try {
            String p = System.getProperty("javafx.platform", "");
            if (p.contains("android")) return true;
            String u = System.getProperty("java.vm.name", "").toLowerCase();
            return u.contains("substrate") || u.contains("graalvm") && System.getProperty("os.name", "").toLowerCase().contains("linux");
        } catch (Throwable t) {
            return false;
        }
    }

    /** Sync Main's legacy skin preference with profile skin id. */
    private static void saveLastSkinCompat() {
        try { mt.saveLastSkin(); } catch (Throwable ignore) {}
    }

    private static void playSfx(AudioClip clip, double vol) {
        if (clip == null) return;
        try { clip.setVolume(vol); clip.play(); } catch (Throwable ignore) {}
    }

    private static Image logoImg() {
        try { return new Image(mt.getClass().getResource("/icon.png").toExternalForm()); }
        catch (Throwable t) { return null; }
    }

    static File profileFile() {
        try {
            File home = new File(System.getProperty("user.home", "."));
            File dir = new File(home, ".snake3d");
            if (!dir.exists()) { /* try mkdir */ dir.mkdirs(); }
            return new File(dir, "profile.json");
        } catch (Throwable t) {
            return new File("profile.json");
        }
    }

    static void loadProfile() {
        profile = new GameProfile();
        File f = profileFile();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String ln;
            while ((ln = r.readLine()) != null) sb.append(ln);
            Map<String, Object> root = parseJson(sb.toString());
            if (root == null) return;
            profile.level = num(root.get("level"), 1);
            profile.xp = num(root.get("xp"), 0);
            profile.coins = num(root.get("coins"), 0);
            profile.skin = str(root.get("skin"), "CLASSIC");
            profile.arena = str(root.get("arena"), "NEON");
            profile.quality = str(root.get("quality"), "MEDIUM");
            profile.fullscreen = bool(root.get("fullscreen"), false);
            profile.shake = bool(root.get("shake"), true);
            profile.skipIntro = bool(root.get("skipIntro"), false);
            profile.musicVol = dbl(root.get("musicVol"), 1.0);
            profile.sfxVol = dbl(root.get("sfxVol"), 1.0);
            String unlocks = str(root.get("unlockedSkins"), "");
            if (!unlocks.isEmpty()) {
                profile.unlockedSkins.clear();
                for (String u : unlocks.split(",")) if (!u.isBlank()) profile.unlockedSkins.add(u.trim());
            }
            profile.unlockedSkins.add("CLASSIC");
            profile.unlockedSkins.add("LIME");
            profile.unlockedSkins.add("NEON");
            parseRanked(str(root.get("bestScores"), ""), profile.bestScores);
            parseRanked(str(root.get("bestExtra"), ""), profile.bestExtra);
            profile.totalFruit = num(root.get("totalFruit"), 0);
            profile.totalGolden = num(root.get("totalGolden"), 0);
            profile.totalTimeSec = num(root.get("totalTimeSec"), 0);
            profile.maxLength = num(root.get("maxLength"), 0);
            profile.totalDeaths = num(root.get("totalDeaths"), 0);
            profile.bossWins = num(root.get("bossWins"), 0);
            profile.treasuresFound = num(root.get("treasuresFound"), 0);
            profile.villainKills = num(root.get("villainKills"), 0);
            List<Object> ach = arr(root.get("achDone"));
            if (ach != null) for (Object o : ach) profile.achDone.add(String.valueOf(o));
            List<Object> mis = arr(root.get("missionsDone"));
            if (mis != null) for (Object o : mis) profile.missionsDone.add(String.valueOf(o));
        } catch (Throwable ignore) {
            // first launch: fresh profile
        }
    }

    static void saveProfile() {
        if (profile == null) return;
        try (PrintWriter w = new PrintWriter(new OutputStreamWriter(new FileOutputStream(profileFile()), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            sb.append("\"level\":").append(profile.level).append(", \"xp\":").append(profile.xp).append(", \"coins\":").append(profile.coins).append(",\n");
            sb.append("\"skin\":\"").append(esc(profile.skin)).append("\", \"arena\":\"").append(esc(profile.arena)).append("\", \"quality\":\"").append(esc(profile.quality)).append("\",\n");
            sb.append("\"fullscreen\":").append(profile.fullscreen).append(", \"shake\":").append(profile.shake).append(", \"skipIntro\":").append(profile.skipIntro).append(",\n");
            sb.append("\"musicVol\":").append(profile.musicVol).append(", \"sfxVol\":").append(profile.sfxVol).append(",\n");
            sb.append("\"unlockedSkins\":\"").append(esc(join(profile.unlockedSkins))).append("\",\n");
            sb.append("\"bestScores\":\"").append(esc(ranked(profile.bestScores))).append("\",\n");
            sb.append("\"bestExtra\":\"").append(esc(ranked(profile.bestExtra))).append("\",\n");
            sb.append("\"totalFruit\":").append(profile.totalFruit)
              .append(", \"totalGolden\":").append(profile.totalGolden)
              .append(", \"totalTimeSec\":").append(profile.totalTimeSec)
              .append(", \"maxLength\":").append(profile.maxLength)
              .append(", \"totalDeaths\":").append(profile.totalDeaths)
              .append(", \"bossWins\":").append(profile.bossWins)
              .append(", \"treasuresFound\":").append(profile.treasuresFound)
              .append(", \"villainKills\":").append(profile.villainKills).append(",\n");
            sb.append("\"achDone\":[").append(quoted(profile.achDone)).append("],\n");
            sb.append("\"missionsDone\":[").append(quoted(profile.missionsDone)).append("]\n");
            sb.append("}\n");
            w.print(sb.toString());
        } catch (Throwable ignore) {
        }
    }

    private static String join(Set<String> s) {
        StringBuilder sb = new StringBuilder();
        for (String x : s) sb.append(sb.length() == 0 ? "" : ",").append(x);
        return sb.toString();
    }

    private static String ranked(Map<String, Integer> m) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : m.entrySet())
            sb.append(sb.length() == 0 ? "" : ",").append(e.getKey()).append(":").append(e.getValue());
        return sb.toString();
    }

    private static void parseRanked(String raw, Map<String, Integer> target) {
        if (raw == null || raw.isEmpty()) return;
        for (String part : raw.split(",")) {
            String[] kv = part.split(":");
            if (kv.length == 2) {
                try { target.put(kv[0].trim(), Integer.parseInt(kv[1].trim())); } catch (Exception ignore) {}
            }
        }
    }

    private static String quoted(List<String> l) {
        StringBuilder sb = new StringBuilder();
        for (String s : l) sb.append(sb.length() == 0 ? "" : ",").append('"').append(esc(s)).append('"');
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseJson(String text) {
        try {
            Map<String, Object> root = new HashMap<>();
            int i = 0, n = text.length();
            while (i < n) {
                char c = text.charAt(i);
                if (c == ' ' || c == '\n' || c == '\r' || c == '\t') { i++; continue; }
                if (c == '}') break;
                if (c != '"') { i++; continue; }
                int close = text.indexOf('"', i + 1);
                if (close < 0) break;
                String key = text.substring(i + 1, close);
                i = close + 1;
                while (i < n && (text.charAt(i) == ' ' || text.charAt(i) == ':')) i++;
                if (i >= n) break;
                char v0 = text.charAt(i);
                if (v0 == '"') {
                    int c2 = text.indexOf('"', i + 1);
                    if (c2 < 0) break;
                    root.put(key, text.substring(i + 1, c2));
                    i = c2 + 1;
                } else if (v0 == '[') {
                    int c2 = text.indexOf(']', i + 1);
                    if (c2 < 0) break;
                    String inner = text.substring(i + 1, c2).trim();
                    List<Object> list = new ArrayList<>();
                    if (!inner.isEmpty()) {
                        for (String part : inner.split(",")) {
                            String p2 = part.trim();
                            if (p2.startsWith("\"") && p2.endsWith("\"") && p2.length() >= 2)
                                list.add(p2.substring(1, p2.length() - 1));
                            else if (!p2.isEmpty()) list.add(p2);
                        }
                    }
                    root.put(key, list);
                    i = c2 + 1;
                } else {
                    int c2 = i;
                    while (c2 < n && ",}\n".indexOf(text.charAt(c2)) < 0) c2++;
                    String tok = text.substring(i, c2).trim();
                    Integer iv = tryInt(tok);
                    if (iv != null) root.put(key, iv);
                    else root.put(key, tok);
                    i = c2;
                }
                while (i < n && text.charAt(i) == ' ') i++;
                if (i < n && text.charAt(i) == ',') i++;
            }
            return root;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Integer tryInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return null; }
    }

    private static int num(Object o, int def) {
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return def; }
    }

    private static double dbl(Object o, double def) {
        try { return Double.parseDouble(String.valueOf(o)); } catch (Exception e) { return def; }
    }

    private static String str(Object o, String def) {
        if (o == null) return def;
        String s = String.valueOf(o);
        return s.isEmpty() ? def : s;
    }

    private static boolean bool(Object o, boolean def) {
        String s = String.valueOf(o);
        if (s.equalsIgnoreCase("true")) return true;
        if (s.equalsIgnoreCase("false")) return false;
        return def;
    }

    private static List<Object> arr(Object o) {
        return o instanceof List ? (List<Object>) o : null;
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // ======================================================================
    //  SECTION 6 — CINEMATIC INTRO (live 3D snake, particles, skip)
    // ======================================================================

    private static StackPane currentOverlay;
    private static VBox currentCard;
    private static String activeScreen = "menu";
    static GameMode pendingMode = GameMode.CLASSIC;
    private static final List<Object> introAnims = new ArrayList<>();
    private static final List<MediaPlayer> introMedia = new ArrayList<>();
    private static boolean introDone = false;

    private static void stopStoppable(Object o) {
        try {
            if (o instanceof Animation a) a.stop();
            else if (o instanceof AnimationTimer t) t.stop();
        } catch (Throwable ignore) {}
    }

    private static void disposeMedia(MediaPlayer mp) {
        try { if (mp != null) { mp.stop(); mp.dispose(); } } catch (Throwable ignore) {}
    }

    static Pane buildIntro() {
        introDone = false;
        final StackPane overlay = new StackPane();
        overlay.setPickOnBounds(true);
        overlay.setId("screen-overlay");

        Pane backdrop = new Pane();
        backdrop.setBackground(new Background(new BackgroundFill(
                new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#050014")), new Stop(0.55, Color.web("#180633")), new Stop(1, Color.web("#05001a"))),
                CornerRadii.EMPTY, Insets.EMPTY)));
        overlay.getChildren().add(backdrop);

        // ---- particles ----
        Group stars = new Group();
        for (int i = 0; i < 70; i++) {
            Circle p = new Circle(1 + Math.random() * 2.2,
                    Math.random() < 0.6 ? Color.web("#7CF9FF") : Color.web("#C77DFF"));
            p.setOpacity(0.3 + Math.random() * 0.7);
            p.setTranslateX(Math.random() * 1000 - 500);
            p.setTranslateY(Math.random() * 560 - 280);
            stars.getChildren().add(p);
            FadeTransition ft = new FadeTransition(Duration.seconds(1.4 + Math.random() * 2.2), p);
            ft.setFromValue(0.12);
            ft.setToValue(1.0);
            ft.setAutoReverse(true);
            ft.setCycleCount(Animation.INDEFINITE);
            ft.play();
            introAnims.add(ft);
        }
        overlay.getChildren().add(stars);

        // ---- live 3D hero snake in a SubScene (desktop only) ----
        Group hero = new Group();
        boolean ok3d = false;
        boolean show3d = !isAndroid();
        PerspectiveCamera cam = new PerspectiveCamera(true);
        cam.setTranslateZ(-340);
        try {
            cam.getTransforms().add(new javafx.scene.transform.Rotate(-7, javafx.scene.transform.Rotate.X_AXIS));
        } catch (Throwable ignore) {}
        SubScene heroScene = null;
        if (show3d) {
            try {
                heroScene = new SubScene(hero, 900, 430, true, SceneAntialiasing.BALANCED);
                heroScene.setFill(Color.TRANSPARENT);
                heroScene.setCamera(cam);
                ok3d = true;
            } catch (Throwable t) {
                ok3d = false;
            }
        }
        final SubScene hs = heroScene;
        if (ok3d) {
            final PhongMaterial glowHead = new PhongMaterial(Color.web("#00E5FF"));
            glowHead.setSpecularColor(Color.WHITE);
            final Group[] segs = new Group[26];
            for (int i = 0; i < segs.length; i++) {
                double r = Math.max(7, (i == 0 ? 21 : 16) - i * 0.2);
                Sphere s = new Sphere(r);
                if (i == 0) {
                    s.setMaterial(glowHead);
                    Sphere e1 = new Sphere(5); e1.setMaterial(new PhongMaterial(Color.WHITE)); e1.setTranslateX(-8); e1.setTranslateY(-8); e1.setTranslateZ(-20);
                    Sphere e2 = new Sphere(5); e2.setMaterial(new PhongMaterial(Color.WHITE)); e2.setTranslateX(8);  e2.setTranslateY(-8); e2.setTranslateZ(-20);
                    Cylinder tongue = new Cylinder(1.6, 9);
                    tongue.setMaterial(new PhongMaterial(Color.web("#FF2D6F")));
                    tongue.setTranslateY(3); tongue.setTranslateZ(-22);
                    segs[i] = new Group(s, e1, e2, tongue);
                } else {
                    s.setMaterial(new PhongMaterial(Color.hsb(136 + i * 8, 0.85, 0.95)));
                    segs[i] = new Group(s);
                }
                hero.getChildren().add(segs[i]);
            }
            AnimationTimer heroTimer = new AnimationTimer() {
                long t0 = -1;
                @Override
                public void handle(long now) {
                    if (t0 < 0) t0 = now;
                    double t = (now - t0) / 1.0e9;
                    for (int i = 0; i < segs.length; i++) {
                        double phase = t * 2.3 - i * 0.55;
                        segs[i].setTranslateX(200 * Math.sin(phase * 0.42) - 320 + i * 26);
                        segs[i].setTranslateY(80 * Math.sin(phase));
                        segs[i].setTranslateZ(120 * Math.cos(phase * 0.7));
                    }
                    hero.setTranslateX(50 * Math.sin(t * 0.6));
                    hero.getTransforms().setAll(
                            new javafx.scene.transform.Rotate(12 * Math.sin(t * 0.35), javafx.scene.transform.Rotate.Y_AXIS));
                }
            };
            heroTimer.start();
            introAnims.add(heroTimer);
            overlay.getChildren().add(hs);
            StackPane.setAlignment(hs, Pos.CENTER);

            // tongue flick
            Timeline flick = new Timeline(
                new KeyFrame(Duration.seconds(0.0), new KeyValue(segs[0].getChildren().get(3).scaleXProperty(), 1)),
                new KeyFrame(Duration.seconds(0.35), new KeyValue(segs[0].getChildren().get(3).scaleXProperty(), 2.4)),
                new KeyFrame(Duration.seconds(0.6), new KeyValue(segs[0].getChildren().get(3).scaleXProperty(), 1)),
                new KeyFrame(Duration.seconds(1.15), new KeyValue(segs[0].getChildren().get(3).scaleXProperty(), 1))
            );
            flick.setCycleCount(Animation.INDEFINITE);
            flick.play();
            introAnims.add(flick);
        }

        // ---- logo + title + progress ----
        VBox center = new VBox(20);
        center.setAlignment(Pos.CENTER);
        Image icon = logoImg();
        if (icon != null) {
            ImageView lv = new ImageView(icon);
            lv.setFitWidth(140);
            lv.setPreserveRatio(true);
            try { lv.setEffect(new DropShadow(36, Color.web("#00E5FF"))); } catch (Throwable ignore) {}
            javafx.animation.RotateTransition rt = new javafx.animation.RotateTransition(Duration.seconds(8), lv);
            rt.setByAngle(360);
            rt.setCycleCount(Animation.INDEFINITE);
            rt.play();
            introAnims.add(rt);
            center.getChildren().add(lv);
        }
        Label title = new Label("S N A K E   3 D");
        title.setFont(Font.font(64));
        title.setTextFill(Color.WHITE);
        try { title.setEffect(new DropShadow(26, Color.web("#00E5FF"))); } catch (Throwable ignore) {}
        Label sub = new Label("U N L E A S H E D");
        sub.setFont(Font.font(20));
        sub.setTextFill(Color.web("#9BE7FF"));
        ProgressBar pb = new ProgressBar(0);
        pb.setPrefSize(330, 12);
        Label load = new Label("Loading arena resources...");
        load.setTextFill(Color.web("#BFF9FF"));
        Label skip = new Label("press any key / click anywhere to skip");
        skip.setTextFill(Color.web("#7986a8"));
        skip.setFont(Font.font(13));
        center.getChildren().addAll(title, sub, pb, load, skip);
        overlay.getChildren().add(center);
        StackPane.setAlignment(center, Pos.CENTER);

        Timeline loadAnim = new Timeline(new KeyFrame(Duration.seconds(6.5), new KeyValue(pb.progressProperty(), 1.0)));
        loadAnim.play();
        introAnims.add(loadAnim);

        final Runnable[] finish = new Runnable[1];
        finish[0] = () -> {
            if (introDone || skipActionPending) return;   // guard double-fires
            introDone = true;
            skipActionPending = false;
            playSfx(sStart, 0.9);
            FadeTransition fo = new FadeTransition(Duration.millis(550), overlay);
            fo.setToValue(0);
            fo.setOnFinished(e -> {
                for (Object a : introAnims) stopStoppable(a);
                introAnims.clear();
                for (MediaPlayer mp : introMedia) disposeMedia(mp);
                introMedia.clear();
                if (overlay.getParent() instanceof Pane p) p.getChildren().remove(overlay);
                showMenu();
            });
            fo.play();
        };
        Runnable doSkip = () -> {
            skipActionPending = true;
            finish[0].run();
        };
        overlay.setOnMouseClicked(e -> doSkip.run());
        overlay.sceneProperty().addListener((o, ov, nv) -> {
            if (nv != null) {
                nv.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, ev -> doSkip.run());
                overlay.requestFocus();
            }
        });
        loadAnim.setOnFinished(e -> { skipActionPending = false; finish[0].run(); });
        return overlay;
    }

    private static boolean skipActionPending;

    // ======================================================================
    //  SECTION 7 — MAIN MENU + SUB SCREENS
    // ======================================================================

    static void showMenu() {
        if (mt == null || mt.sceneRef == null) return;
        hideCurrentOverlay();
        hideHud();
        StackPane root = (StackPane) mt.sceneRef.getRoot();
        StackPane overlay = buildMenuSurface();
        root.getChildren().add(overlay);
        currentOverlay = overlay;
        activeScreen = "menu";
        currentCard = menuCardMain();
        addToCardHost(currentCard);
        softIn(currentCard);
    }

    private static void hideCurrentOverlay() {
        if (currentOverlay != null) {
            if (currentOverlay.getParent() instanceof Pane p) p.getChildren().remove(currentOverlay);
            currentOverlay = null;
        }
    }

    private static StackPane buildMenuSurface() {
        StackPane overlay = new StackPane();
        overlay.setPickOnBounds(true);
        overlay.setId("screen-overlay");

        Pane dark = new Pane();
        dark.setOpacity(0.34);
        boolean videoOk = false;
        if (!isAndroid()) {
            try {
                MediaPlayer mp = new MediaPlayer(new Media(mt.getClass().getResource("/bgvideo.mp4").toExternalForm()));
                mp.setCycleCount(MediaPlayer.INDEFINITE);
                mp.setVolume(profile.musicVol * 0.5);
                mp.setAutoPlay(true);
                introMedia.add(mp);
                menuVideo = mp;
                MediaView mv = new MediaView(mp);
                mv.setPreserveRatio(false);
                mv.fitWidthProperty().bind(overlay.widthProperty());
                mv.fitHeightProperty().bind(overlay.heightProperty());
                overlay.getChildren().add(mv);
                videoOk = true;
            } catch (Throwable ignore) {}
        }
        if (!videoOk) {
            dark.setOpacity(1);
            dark.setBackground(new Background(new BackgroundFill(
                    new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                            new Stop(0, Color.web("#12143a")), new Stop(1, Color.web("#2b0b45"))),
                    CornerRadii.EMPTY, Insets.EMPTY)));
        }
        overlay.getChildren().add(dark);

        VBox column = new VBox(22);
        column.setAlignment(Pos.TOP_CENTER);
        column.setPadding(new Insets(24, 20, 20, 20));

        // title row
        HBox titleRow = new HBox(16);
        titleRow.setAlignment(Pos.CENTER);
        Image icon = logoImg();
        if (icon != null) {
            ImageView lv = new ImageView(icon);
            lv.setFitWidth(78);
            lv.setPreserveRatio(true);
            try { lv.setEffect(new DropShadow(22, Color.web("#00E5FF"))); } catch (Throwable ignore) {}
            try {
                javafx.animation.RotateTransition rt = new javafx.animation.RotateTransition(Duration.seconds(9), lv);
                rt.setByAngle(360);
                rt.setCycleCount(Animation.INDEFINITE);
                rt.setAxis(javafx.scene.transform.Rotate.Y_AXIS);
                rt.play();
                introAnims.add(rt);
            } catch (Throwable ignore) {}
            titleRow.getChildren().add(lv);
        }
        Label title = new Label("SNAKE 3D");
        title.setFont(Font.font(74));
        title.setTextFill(Color.WHITE);
        try { title.setEffect(new DropShadow(30, Color.web("#00E5FF"))); } catch (Throwable ignore) {}
        javafx.animation.ScaleTransition ts = new javafx.animation.ScaleTransition(Duration.seconds(2.4), title);
        ts.setFromX(0.97);
        ts.setToX(1.05);
        ts.setCycleCount(Animation.INDEFINITE);
        ts.setAutoReverse(true);
        ts.play();
        introAnims.add(ts);
        titleRow.getChildren().add(title);

        HBox chip = profileChip();
        VBox cardHostPane = new VBox();     // game card swaps inside this
        cardHostPane.setAlignment(Pos.CENTER);
        cardHostPane.setMinWidth(430);
        cardHostPane.setId("card-host");
        cardHost = cardHostPane;

        Group sparks = new Group();
        for (int i = 0; i < 24; i++) {
            Circle p = new Circle(1.6 + Math.random() * 2, Color.web("#14F1D9"));
            p.setOpacity(0.2 + Math.random() * 0.5);
            p.setTranslateX(Math.random() * 760 - 380);
            p.setTranslateY(Math.random() * 420 - 200);
            sparks.getChildren().add(p);
            TranslateTransition tt = new TranslateTransition(Duration.seconds(6 + Math.random() * 6), p);
            tt.setByY(-140 - Math.random() * 150);
            tt.setCycleCount(Animation.INDEFINITE);
            tt.play();
            introAnims.add(tt);
        }

        StackPane stacker = new StackPane(sparks, cardHostPane);
        stacker.setMaxWidth(760);

        column.getChildren().addAll(titleRow, chip, stacker);
        overlay.getChildren().add(column);
        StackPane.setAlignment(column, Pos.TOP_CENTER);

        overlay.sceneProperty().addListener((o, ov, nv) -> {
            if (nv != null) {
                nv.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, ev -> {
                    if (ev.getCode() == KeyCode.ESCAPE && !"menu".equals(activeScreen)) {
                        activeScreen = "menu";
                        swapCard(menuCardMain());
                    }
                });
                try { overlay.requestFocus(); } catch (Throwable ignore) {}
            }
        });
        overlay.sceneProperty().addListener((o, ov, nv) -> {
            if (nv == null) {
                for (Object a : introAnims) stopStoppable(a);
                introAnims.clear();
                for (MediaPlayer mp : introMedia) disposeMedia(mp);
                introMedia.clear();
                menuVideo = null;
            }
        });
        return overlay;
    }

    private static VBox cardHost;

    private static void addToCardHost(Pane card) {
        if (cardHost != null) {
            cardHost.getChildren().setAll(card);
        }
    }

    private static void swapCard(Pane newCard) {
        if (cardHost == null || newCard == null) return;
        newCard.setOpacity(0);
        newCard.setTranslateY(24);
        cardHost.getChildren().setAll(newCard);
        currentCard = cvb(newCard);
        Timeline t = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(newCard.opacityProperty(), 0), new KeyValue(newCard.translateYProperty(), 24)),
                new KeyFrame(Duration.seconds(0.3), new KeyValue(newCard.opacityProperty(), 1), new KeyValue(newCard.translateYProperty(), 0))
        );
        t.play();
    }

    private static void softIn(Pane card) {
        if (card == null) return;
        Timeline t = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(card.opacityProperty(), 0), new KeyValue(card.scaleXProperty(), 0.9)),
                new KeyFrame(Duration.seconds(0.35), new KeyValue(card.opacityProperty(), 1), new KeyValue(card.scaleXProperty(), 1))
        );
        t.play();
    }

    private static VBox cvb(Pane p) {
        if (p instanceof VBox v) return v;
        VBox w = new VBox(p);
        w.setAlignment(Pos.CENTER);
        return w;
    }

    private static HBox profileChip() {
        HBox chip = new HBox(14);
        chip.setAlignment(Pos.CENTER);
        chip.setPadding(new Insets(8, 20, 8, 20));
        chip.setStyle("-fx-background-color: rgba(10, 28, 44, 0.74); -fx-background-radius: 18;" +
                "-fx-border-color: linear-gradient(to right, #00E5FF, #C77DFF); -fx-border-radius: 18; -fx-border-width: 1.4;");
        Label lvl = new Label("LVL " + profile.level);
        lvl.setTextFill(Color.web("#FFD166"));
        lvl.setFont(Font.font(17));
        ProgressBar xp = new ProgressBar(Math.min(1, (double) profile.xp / profile.xpToNext()));
        xp.setPrefSize(150, 10);
        Label coins = new Label("\u26A5 " + profile.coins);
        coins.setTextFill(Color.web("#9BE7FF"));
        coins.setFont(Font.font(16));
        Label skin = new Label("\u25C9 " + skinDef(profile.skin).name);
        skin.setTextFill(Color.WHITE);
        skin.setFont(Font.font(15));
        chip.getChildren().addAll(lvl, xp, coins, skin);
        chip.setUserData(xp);
        return chip;
    }

    private static void refreshChip() {
        if (currentOverlay == null) return;
        for (javafx.scene.Node n : currentOverlay.getChildren()) {
            if (n instanceof HBox h && h.getUserData() instanceof ProgressBar xp && !h.getChildren().isEmpty() && h.getChildren().get(0) instanceof Label) {
                xp.setProgress(Math.min(1, (double) profile.xp / profile.xpToNext()));
                Label first = (Label) h.getChildren().get(0);
                first.setText("LVL " + profile.level);
                Label coins = h.getChildren().get(2) instanceof Label ? (Label) h.getChildren().get(2) : null;
                if (coins != null) coins.setText("\u26A5 " + profile.coins);
            }
        }
    }

    private static String btnBase(String color) {
        return "-fx-background-color: " + color + ";"
                + "-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;"
                + "-fx-background-radius: 14; -fx-background-insets: 0; -fx-cursor: hand;"
                + "-fx-alignment: CENTER_LEFT; -fx-padding: 10 18 10 24;";
    }

    private static Button menuButton(String label, String color, Runnable action) {
        Button b = new Button(label);
        b.setPrefSize(330, 52);
        b.setMaxWidth(330);
        b.setStyle(btnBase(color));
        b.setOnMouseEntered(e -> {
            try { b.setEffect(new Glow(0.4)); } catch (Throwable ignore) {}
            b.setScaleX(1.045);
            b.setScaleY(1.045);
        });
        b.setOnMouseExited(e -> {
            try { b.setEffect(null); } catch (Throwable ignore) {}
            b.setScaleX(1);
            b.setScaleY(1);
        });
        b.setOnAction(e -> {
            playSfx(sClick, 0.8);
            if (action != null) action.run();
        });
        return b;
    }

    private static VBox menuCardMain() {
        activeScreen = "menu";
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(16));
        card.setMaxWidth(430);
        card.setStyle("-fx-background-color: rgba(6, 10, 26, 0.86); -fx-background-radius: 22;");
        card.getChildren().addAll(
                menuButton("\u25B6  PLAY", "#0e9b57", () -> startSelected(pendingMode)),
                menuButton("\u2694  GAME MODES", "#5b3cc4", () -> { activeScreen = "modes"; swapCard(screenModes()); }),
                menuButton("\uD83D\uDC0D  SNAKE COLLECTION", "#b01e63", () -> { activeScreen = "collection"; swapCard(screenCollection()); }),
                menuButton("\uD83C\uDCCF  MISSIONS", "#9a3eb3", () -> { activeScreen = "missions"; swapCard(screenMissions()); }),
                menuButton("\uD83C\uDFC6  ACHIEVEMENTS", "#c47a1f", () -> { activeScreen = "achievements"; swapCard(screenAchievements()); }),
                menuButton("\uD83D\uDCDF  LEADERBOARD", "#1f77c4", () -> { activeScreen = "leaderboard"; swapCard(screenLeaderboard()); }),
                menuButton("\u2699  SETTINGS", "#4a5568", () -> { activeScreen = "settings"; swapCard(screenSettings()); }),
                menuButton("\u2753  HOW TO PLAY", "#2f6f6f", () -> { activeScreen = "howto"; swapCard(screenHowTo()); }),
                menuButton("\u23FB  EXIT GAME", "#8a1f2f", () -> { saveProfile(); Platform.exit(); })
        );
        Label hint = new Label("WASD / arrows \u2022 SPACE or ESC pause \u2022 progress saves automatically");
        hint.setTextFill(Color.web("#7fa8c9"));
        hint.setFont(Font.font(12));
        hint.setWrapText(true);
        card.getChildren().add(hint);
        return card;
    }

    private static VBox titledCard(String title, String accent) {
        VBox card = new VBox(12);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(18));
        card.setMaxWidth(780);
        card.setPrefHeight(500);
        card.setStyle("-fx-background-color: rgba(6, 10, 26, 0.92); -fx-background-radius: 22;");
        HBox top = new HBox(140);
        top.setAlignment(Pos.CENTER_LEFT);
        Button back = new Button("\u2190  Back");
        back.setFont(Font.font(15));
        back.setStyle("-fx-background-color: " + accent + "; -fx-text-fill: #08140C; -fx-font-weight: bold;"
                + "-fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 4 14 4 14;");
        back.setOnAction(e -> {
            playSfx(sClick, 0.8);
            swapCard(menuCardMain());
        });
        Label h = new Label(title);
        h.setFont(Font.font(28));
        h.setTextFill(Color.WHITE);
        try { h.setEffect(new DropShadow(14, Color.web(accent))); } catch (Throwable ignore) {}
        top.getChildren().addAll(back, h);
        card.getChildren().add(top);
        return card;
    }

    private static ScrollPane scrollify(javafx.scene.Node content, double h) {
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setPrefSize(730, h);
        sp.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        return sp;
    }

    // ---------- SCREEN: GAME MODES ----------
    private static VBox screenModes() {
        VBox card = titledCard("GAME MODES", "#9b7dff");
        Label sub = new Label("Every run earns XP \u2022 best runs go to the leaderboard");
        sub.setTextFill(Color.web("#9BE7FF"));
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(14);
        int col = 0, row = 0;
        for (GameMode g : GameMode.values()) {
            grid.add(modeTile(g), col, row);
            col++;
            if (col == 2) { col = 0; row++; }
        }
        card.getChildren().addAll(sub, scrollify(grid, 360));
        return card;
    }

    private static VBox modeTile(GameMode g) {
        VBox mini = new VBox(6);
        mini.setPrefSize(340, 140);
        mini.setAlignment(Pos.CENTER_LEFT);
        mini.setPadding(new Insets(12));
        mini.setStyle("-fx-background-color: linear-gradient(to bottom right, rgba(24,36,80,0.95), rgba(10,14,34,0.95));"
                + "-fx-background-radius: 14; -fx-border-color: #3A4E9E; -fx-border-radius: 14; -fx-border-width: 1.4; -fx-cursor: hand;");
        Label t = new Label(g.title);
        t.setTextFill(Color.WHITE);
        t.setFont(Font.font(20));
        try { t.setEffect(new DropShadow(8, Color.web("#5B3CC4"))); } catch (Throwable ignore) {}
        Label d = new Label(g.desc);
        d.setTextFill(Color.web("#9BE7FF"));
        d.setWrapText(true);
        d.setFont(Font.font(13));
        Label best = new Label("Best: " + profile.bestScores.getOrDefault(g.name(), 0));
        best.setTextFill(Color.web("#FFD166"));
        best.setFont(Font.font(13));
        mini.getChildren().addAll(t, d, best);
        mini.setOnMouseEntered(e -> { mini.setScaleX(1.03); mini.setScaleY(1.03); });
        mini.setOnMouseExited(e -> { mini.setScaleX(1); mini.setScaleY(1); });
        mini.setOnMouseClicked(e -> startSelected(g));
        return mini;
    }

    // ---------- SCREEN: SNAKE COLLECTION ----------
    private static VBox screenCollection() {
        VBox card = titledCard("SNAKE COLLECTION", "#ff5fae");
        card.getChildren().add(scrollify(snakesGrid(), 360));
        return card;
    }

    private static GridPane snakesGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(14);
        grid.setAlignment(Pos.CENTER);
        int col = 0, row = 0;
        for (SkinDef s : SKINS) {
            grid.add(snakeTile(s), col, row);
            col++;
            if (col == 3) { col = 0; row++; }
        }
        return grid;
    }

    private static VBox snakeTile(SkinDef s) {
        boolean unlocked = profile.unlockedSkins.contains(s.id);
        boolean equipped = profile.skin.equals(s.id);
        VBox tile = new VBox(7);
        tile.setPrefSize(220, 190);
        tile.setAlignment(Pos.CENTER);
        tile.setPadding(new Insets(10));
        String rarityCol = switch (s.rarity) {
            case "Legendary" -> "#FFD166";
            case "Epic" -> "#B366FF";
            case "Rare" -> "#3FA7FF";
            default -> "#7CFC00";
        };
        tile.setStyle("-fx-background-color: linear-gradient(to bottom, rgba(20,28,54,0.95), rgba(8,10,26,0.98));"
                + "-fx-background-radius: 14; -fx-border-color: " + rarityCol + "; -fx-border-radius: 14; -fx-border-width: 2;");
        HBox preview = new HBox(5);
        preview.setAlignment(Pos.CENTER);
        for (int i = 0; i < 8; i++) preview.getChildren().add(new Circle(i == 0 ? 11 : 8, s.body.get(i % s.body.size())));
        Label name = new Label(s.name + (unlocked ? "" : "  (locked)"));
        name.setTextFill(Color.WHITE);
        name.setFont(Font.font(17));
        Label rar = new Label(s.rarity
                + (s.unlockLevel > 0 ? "  \u2022 LVL " + s.unlockLevel : "")
                + (s.price > 0 ? "  \u2022 " + s.price + " \u26A5" : ""));
        rar.setTextFill(Color.web(rarityCol));
        rar.setFont(Font.font(12));
        Button action;
        if (equipped) {
            action = miniBtn("EQUIPPED", "#37474f", null);
            action.setDisable(true);
        } else if (unlocked) {
            action = miniBtn("EQUIP", "#0e9b57", () -> {
                profile.skin = s.id;
                saveProfile();
                applySkinVisual();
                toast("Equipped " + s.name);
                refreshChip();
                swapCard(screenCollection());
            });
        } else if (profile.level >= s.unlockLevel && profile.coins >= s.price) {
            action = miniBtn("BUY " + s.price + " \u26A5", "#b8860b", () -> {
                profile.coins -= s.price;
                profile.unlockedSkins.add(s.id);
                saveProfile();
                toast("Unlocked " + s.name + "!");
                refreshChip();
                swapCard(screenCollection());
            });
        } else {
            Label lockNote = new Label("Reach LVL " + s.unlockLevel + " & " + s.price + " coins");
            lockNote.setTextFill(Color.web("#7fa8c9"));
            lockNote.setFont(Font.font(11));
            tile.getChildren().addAll(preview, name, rar);
            action = miniBtn("LOCKED", "#2d2d44", null);
            action.setDisable(true);
            tile.getChildren().add(action);
            tile.getChildren().add(lockNote);
            return tile;
        }
        tile.getChildren().addAll(preview, name, rar, action);
        return tile;
    }

    private static Button miniBtn(String label, String col, Runnable act) {
        Button b = new Button(label);
        b.setPrefSize(180, 32);
        b.setFont(Font.font(13));
        b.setStyle("-fx-background-color: " + col + "; -fx-text-fill: white; -fx-font-weight: bold;"
                + "-fx-background-radius: 9; -fx-cursor: hand;");
        if (act != null) {
            b.setOnAction(e -> {
                playSfx(sClick, 0.8);
                act.run();
            });
        }
        return b;
    }

    // ---------- SCREEN: MISSIONS ----------
    private static VBox screenMissions() {
        VBox card = titledCard("MISSIONS", "#C77DFF");
        VBox list = new VBox(9);
        for (MissionDef m : MISSIONS) {
            boolean done = profile.missionsDone.contains(m.id);
            int prog = m.progress(profile);
            int target = m.target;
            HBox row = new HBox(14);
            row.setPrefSize(700, 50);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(5, 12, 5, 12));
            row.setStyle("-fx-background-color: " + (done ? "#1c3a2a66" : "#111a3366")
                    + "; -fx-background-radius: 12; -fx-border-color: " + (done ? "#34d17b" : "#2a3a66")
                    + "; -fx-border-radius: 12;");
            Label icon = new Label(done ? "\u2714" : "\u25C9");
            icon.setFont(Font.font(20));
            VBox mid = new VBox(3);
            Label d = new Label(m.desc);
            d.setTextFill(Color.WHITE);
            d.setFont(Font.font(15));
            Label rw = new Label("Reward: " + m.rewardCoins(profile) + " coins");
            rw.setTextFill(Color.web("#FFD166"));
            rw.setFont(Font.font(12));
            mid.getChildren().addAll(d, rw);
            Label pr = new Label(Math.min(prog, target) + " / " + target);
            pr.setTextFill(done ? Color.web("#34d17b") : Color.web("#9BE7FF"));
            pr.setFont(Font.font(15));
            row.getChildren().addAll(icon, mid, pr);
            list.getChildren().add(row);
        }
        card.getChildren().add(scrollify(list, 360));
        return card;
    }

    // ---------- SCREEN: ACHIEVEMENTS ----------
    private static VBox screenAchievements() {
        VBox card = titledCard("ACHIEVEMENTS", "#ffc93c");
        VBox list = new VBox(9);
        for (String[] a : ACHIEVEMENTS) {
            boolean done = profile.achDone.contains(a[0]);
            HBox row = new HBox(14);
            row.setPrefSize(700, 44);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(5, 12, 5, 12));
            row.setStyle("-fx-background-color: " + (done ? "#234a1c66" : "#111a3366")
                    + "; -fx-background-radius: 12; -fx-border-color: " + (done ? "#ffc93c" : "#2a3a66")
                    + "; -fx-border-radius: 12;");
            Label icon = new Label(done ? "\u2605" : "\u25CB");
            icon.setFont(Font.font(20));
            VBox mid = new VBox(2);
            Label n = new Label(a[1]);
            n.setTextFill(Color.WHITE);
            n.setFont(Font.font(15));
            Label d = new Label(a[2]);
            d.setTextFill(Color.web("#9BE7FF"));
            d.setFont(Font.font(12));
            mid.getChildren().addAll(n, d);
            row.getChildren().addAll(icon, mid);
            list.getChildren().add(row);
        }
        card.getChildren().add(scrollify(list, 360));
        return card;
    }

    // ---------- SCREEN: LEADERBOARD ----------
    private static VBox screenLeaderboard() {
        VBox card = titledCard("LEADERBOARD \u2022 LOCAL HIGH SCORES", "#1f77c4");
        GridPane grid = new GridPane();
        grid.setHgap(40);
        grid.setVgap(12);
        grid.setAlignment(Pos.CENTER);
        int r = 0;
        for (GameMode g : GameMode.values()) {
            Label l1 = new Label(g.title);
            l1.setTextFill(Color.web("#9BE7FF"));
            l1.setFont(Font.font(15));
            int best = profile.bestScores.getOrDefault(g.name(), 0);
            int extra = profile.bestExtra.getOrDefault(g.name(), 0);
            String extraTxt = switch (g) {
                case WAVE -> "   \u2022  best wave: " + extra;
                case TREASURE -> "   \u2022  best coins: " + extra;
                case BOSS -> "   \u2022  wins: " + extra;
                default -> "";
            };
            Label l2 = new Label(best + " pts" + extraTxt);
            l2.setTextFill(Color.WHITE);
            l2.setFont(Font.font(17));
            grid.add(l1, 0, r);
            grid.add(l2, 1, r);
            r++;
        }
        card.getChildren().add(grid);
        return card;
    }

    // ---------- SCREEN: SETTINGS ----------
    private static VBox screenSettings() {
        VBox card = titledCard("SETTINGS", "#7FBFFF");
        GridPane g = new GridPane();
        g.setHgap(18);
        g.setVgap(14);
        g.setAlignment(Pos.CENTER);
        Label l1 = new Label("Graphics Quality");
        l1.setTextFill(Color.WHITE);
        ComboBox<String> cb = new ComboBox<>();
        cb.getItems().addAll("LOW", "MEDIUM", "HIGH");
        cb.setValue(profile.quality);
        cb.setOnAction(e -> {
            profile.quality = cb.getValue();
            saveProfile();
            toast("Graphics: " + profile.quality);
        });
        g.add(l1, 0, 0);
        g.add(cb, 1, 0);

        Label l2 = new Label("Arena");
        l2.setTextFill(Color.WHITE);
        ComboBox<String> arenaC = new ComboBox<>();
        for (Arena a : Arena.values()) arenaC.getItems().add(a.title);
        arenaC.setValue(Arena.of(profile.arena).title);
        arenaC.setOnAction(e -> {
            for (Arena a : Arena.values()) if (a.title.equals(arenaC.getValue())) profile.arena = a.name();
            saveProfile();
            if (run != null) run.arena = Arena.of(profile.arena);
            toast("Arena: " + Arena.of(profile.arena).title);
        });
        g.add(l2, 0, 1);
        g.add(arenaC, 1, 1);

        Label l3 = new Label("Fullscreen");
        l3.setTextFill(Color.WHITE);
        CheckBox fs = new CheckBox();
        fs.setSelected(profile.fullscreen);
        fs.setOnAction(e -> {
            profile.fullscreen = fs.isSelected();
            saveProfile();
            try {
                if (stageRef != null) stageRef.setFullScreen(profile.fullscreen);
            } catch (Throwable ignore) {}
        });
        g.add(l3, 0, 2);
        g.add(fs, 1, 2);

        Label l4 = new Label("Camera Shake");
        l4.setTextFill(Color.WHITE);
        CheckBox sh = new CheckBox();
        sh.setSelected(profile.shake);
        sh.setOnAction(e -> {
            profile.shake = sh.isSelected();
            saveProfile();
        });
        g.add(l4, 0, 3);
        g.add(sh, 1, 3);

        Label l5 = new Label("Skip intro next launch");
        l5.setTextFill(Color.WHITE);
        CheckBox si = new CheckBox();
        si.setSelected(profile.skipIntro);
        si.setOnAction(e -> {
            profile.skipIntro = si.isSelected();
            saveProfile();
        });
        g.add(l5, 0, 4);
        g.add(si, 1, 4);

        Label l6 = new Label("Music Volume");
        l6.setTextFill(Color.WHITE);
        Slider music = new Slider(0, 1, profile.musicVol);
        music.setPrefWidth(200);
        music.valueProperty().addListener((o, ov, nv) -> {
            profile.musicVol = nv.doubleValue();
            if (menuVideo != null) menuVideo.setVolume(profile.musicVol * 0.5);
            saveProfile();
        });
        g.add(l6, 0, 5);
        g.add(music, 1, 5);

        Label l7 = new Label("SFX Volume");
        l7.setTextFill(Color.WHITE);
        Slider sfx = new Slider(0, 1, profile.sfxVol);
        sfx.setPrefWidth(200);
        sfx.valueProperty().addListener((o, ov, nv) -> {
            profile.sfxVol = nv.doubleValue();
            saveProfile();
        });
        g.add(l7, 0, 6);
        g.add(sfx, 1, 6);

        card.getChildren().add(g);
        Label note = new Label("LOW quality reduces particles, trails and arena effects automatically \u2014 for a smooth 60 FPS on modest laptops.");
        note.setTextFill(Color.web("#7fa8c9"));
        note.setWrapText(true);
        note.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        card.getChildren().add(note);
        return card;
    }

    // ---------- SCREEN: HOW TO PLAY ----------
    private static VBox screenHowTo() {
        VBox card = titledCard("HOW TO PLAY", "#2f6f6f");
        VBox content = new VBox(9);
        content.setAlignment(Pos.CENTER);
        String[] lines = {
                "\u25B6 Eat fruit, grow long and survive. Score = points; every 10 fruits you speed up!",
                "\u25B6 WASD / arrow keys steer. On mobile use the on-screen pad.",
                "\u25B6 The arena wraps on every edge \u2014 hostile heads and bodies are the only kill.",
                "\u25B6 Golden fruit: whoever grabs it gains a 10s speed boost.",
                "\u25B6 Villain snakes want your fruit too \u2014 some chase, some flee, some sprint.",
                "\u25B6 Their bodies are hard walls: lure them into YOUR body to defeat them (+coins!).",
                "\u25B6 Shield protects one hit \u2022 Magnet pulls nearby food \u2022 Double Score doubles gains.",
                "\u25B6 Mystery Box = random gift. Beware Venom Bombs (-5) and Traps (slow-down).",
                "\u25B6 7 modes \u2022 6 arenas \u2022 10 collectible skins \u2022 missions \u2022 achievements.",
                "\u25B6 SPACE / ESC = pause \u2022 R (some modes) = restart prompt on the result screen.",
                "Good luck, snake master!"
        };
        boolean odd = false;
        for (String s : lines) {
            Label l = new Label(s);
            l.setTextFill(odd ? Color.web("#b7e5ff") : Color.web("#ffffff"));
            l.setWrapText(true);
            l.setFont(Font.font(14));
            l.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            content.getChildren().add(l);
            odd = !odd;
        }
        card.getChildren().add(content);
        return card;
    }

    // ======================================================================
    //  SECTION 8 — PAUSE + TOASTS
    // ======================================================================

    static void showPause() {
        if (mt == null || currentOverlay != null) return;
        if (mt.running) {
            mt.running = false;
            try { if (mt.timer != null) mt.timer.stop(); } catch (Throwable ignore) {}
            try { mt.pauseGoldenFruitTimers(); } catch (Throwable ignore) {}
            try { mt.stopStartButton.setText("Start"); } catch (Throwable ignore) {}
        }
        StackPane overlay = new StackPane();
        overlay.setPickOnBounds(true);
        overlay.setId("screen-overlay");
        overlay.setStyle("-fx-background-color: rgba(2, 6, 18, 0.72);");
        VBox card = new VBox(14);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(24));
        card.setMaxWidth(380);
        card.setStyle("-fx-background-color: rgba(6, 10, 26, 0.95); -fx-background-radius: 22;");
        Label t = new Label("PAUSED");
        t.setFont(Font.font(34));
        t.setTextFill(Color.WHITE);
        try { t.setEffect(new DropShadow(20, Color.web("#00E5FF"))); } catch (Throwable ignore) {}
        card.getChildren().add(t);
        Button resume = menuButton("\u25B6  RESUME", "#0e9b57", thisReflessResume());
        Button restart = menuButton("\u21BA  RESTART", "#b01e63", () -> {
            hideCurrentOverlay();
            startSelected(run == null ? GameMode.CLASSIC : run.mode);
        });
        Button settings = menuButton("\u2699  SETTINGS", "#4a5568", () -> {
            hideCurrentOverlay();
            StackPane root = (StackPane) mt.sceneRef.getRoot();
            StackPane ov2 = buildMenuSurface();
            root.getChildren().add(ov2);
            currentOverlay = ov2;
            activeScreen = "settings";
            currentCard = screenSettings();
            addToCardHost(currentCard);
            softIn(currentCard);
        });
        Button menu = menuButton("\u2302  MAIN MENU", "#8a1f2f", () -> {
            hideCurrentOverlay();
            showMenu();
        });
        card.getChildren().addAll(resume, restart, settings, menu);
        overlay.getChildren().add(card);
        StackPane root = (StackPane) mt.sceneRef.getRoot();
        root.getChildren().add(overlay);
        currentOverlay = overlay;
        activeScreen = "pause";
        overlay.sceneProperty().addListener((o, ov, nv) -> {
            if (nv != null) {
                nv.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, ev -> {
                    if (ev.getCode() == KeyCode.ESCAPE || ev.getCode() == KeyCode.SPACE) {
                        resumeFromPause();
                    }
                });
            }
        });
    }

    private static Runnable thisReflessResume() {
        return () -> resumeFromPause();
    }

    static void resumeFromPause() {
        hideCurrentOverlay();
        mt.running = true;
        try { if (mt.timer != null) mt.timer.start(); } catch (Throwable ignore) {}
        try { mt.resumeGoldenFruitTimers(); } catch (Throwable ignore) {}
        try { mt.stopStartButton.setText("Stop"); } catch (Throwable ignore) {}
        if (run != null) run.paused = false;
    }

    // ---------- toast system ----------
    private static Label toastLabel;
    private static Timeline toastTimer;

    static void toast(String msg) {
        if (mt == null) return;
        if (toastLabel == null) {
            StackPane root = (StackPane) mt.sceneRef.getRoot();
            toastLabel = new Label();
            toastLabel.setFont(Font.font(20));
            toastLabel.setTextFill(Color.WHITE);
            try { toastLabel.setEffect(new DropShadow(16, Color.web("#00E5FF"))); } catch (Throwable ignore) {}
            toastLabel.setMouseTransparent(true);
            root.getChildren().add(toastLabel);
            StackPane.setAlignment(toastLabel, Pos.TOP_CENTER);
            StackPane.setMargin(toastLabel, new Insets(140, 0, 0, 0));
        }
        Platform.runLater(() -> {
            toastLabel.setText(msg);
            toastLabel.setOpacity(1);
            if (toastTimer != null) toastTimer.stop();
            toastTimer = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(toastLabel.opacityProperty(), 1)),
                    new KeyFrame(Duration.seconds(2.2), new KeyValue(toastLabel.opacityProperty(), 0))
            );
            toastTimer.play();
        });
    }

    // ======================================================================
    //  SECTION 9 — HUD (score/level/xp/coins, power-ups, minimap, boss, pad)
    // ======================================================================

    private static Pane hudRoot;
    private static Label hudScoreL, hudHighL, hudLenL, hudObjectiveL, hudWaveL, hudBossL;
    private static ProgressBar hudXpBar;
    private static Pane hudPowers;

    static void attachHud(Main m) {
        if (hudRoot != null) {
            showHudNow();
            return;
        }
        hudRoot = new BorderPane();
        hudRoot.setMouseTransparent(false);
        hudRoot.setPickOnBounds(false);

        BorderPane bp = (BorderPane) hudRoot;
        // ---- top bar (score cluster + pause button)
        VBox topLeft = new VBox(2);
        topLeft.setPadding(new Insets(8, 0, 0, 12));
        hudScoreL = new Label("Score: 0");
        hudScoreL.setFont(Font.font(24));
        hudScoreL.setTextFill(Color.web("#FFF8DC"));
        hudHighL = new Label("Best: " + m.highScore);
        hudHighL.setFont(Font.font(15));
        hudHighL.setTextFill(Color.web("#BFF9FF"));
        hudLenL = new Label("Length: " + (m.bodyParts.size() + 1));
        hudLenL.setFont(Font.font(14));
        hudLenL.setTextFill(Color.web("#9BE7FF"));
        HBox hudRow = new HBox(12, hudScoreL, hudHighL, hudLenL);
        HBox lvlRow = new HBox(10);
        Label lvlL = new Label("LVL " + profile.level);
        lvlL.setTextFill(Color.web("#FFD166"));
        lvlL.setFont(Font.font(14));
        Label coins = new Label("\u26A5 " + profile.coins);
        coins.setTextFill(Color.web("#9BE7FF"));
        coins.setFont(Font.font(14));
        hudXpBar = new ProgressBar(Math.min(1, (double) profile.xp / profile.xpToNext()));
        hudXpBar.setPrefSize(140, 9);
        lvlRow.getChildren().addAll(lvlL, hudXpBar, coins);
        hudPowers = new HBox(8);
        topLeft.getChildren().addAll(hudRow, lvlRow, hudPowers);
        hudObjectiveL = new Label("");
        hudObjectiveL.setTextFill(Color.web("#C9FFF9"));
        hudObjectiveL.setFont(Font.font(15));
        hudWaveL = new Label("");
        hudWaveL.setTextFill(Color.web("#FFD166"));
        hudWaveL.setFont(Font.font(15));
        topLeft.getChildren().addAll(hudObjectiveL, hudWaveL);
        topLeft.setPickOnBounds(false);
        bp.setTop(topLeft);
        BorderPane.setAlignment(topLeft, Pos.TOP_LEFT);
        BorderPane.setMargin(topLeft, new Insets(6, 0, 0, 10));

        bp.setTop(topLeft);

        // boss HP (top center)
        hudBossL = new Label("");
        hudBossL.setFont(Font.font(20));
        hudBossL.setTextFill(Color.web("#FF6680"));
        hudBossL.setVisible(false);
        StackPane bossWrap = new StackPane(hudBossL);
        bossWrap.setMouseTransparent(true);
        bp.setCenter(bossWrap);
        BorderPane.setAlignment(bossWrap, Pos.TOP_CENTER);
        BorderPane.setMargin(bossWrap, new Insets(52, 0, 0, 0));

        // ---- right: pause + minimap
        VBox rightSide = new VBox(8);
        rightSide.setPadding(new Insets(8, 12, 0, 0));
        rightSide.setAlignment(Pos.TOP_RIGHT);
        Button pauseBtn = new Button("\u23F8");
        pauseBtn.setFont(Font.font(20));
        pauseBtn.setStyle("-fx-background-color: rgba(10,20,40,0.7); -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;");
        pauseBtn.setPrefSize(46, 40);
        pauseBtn.setOnAction(e -> { playSfx(sClick, 0.7); showPause(); });
        hudMap = new Canvas(120, 92);
        try { hudMap.setOpacity(0.75); } catch (Throwable ignore) {}
        VBox mapBox = new VBox(4, pauseBtn, hudMap);
        mapBox.setAlignment(Pos.TOP_RIGHT);
        rightSide.getChildren().add(mapBox);
        bp.setRight(rightSide);

        // ---- bottom-left: on-screen direction pad (touch + click)
        GridPane pad = new GridPane();
        pad.setHgap(6);
        pad.setVgap(6);
        pad.setStyle("-fx-background-color: rgba(0,0,0,0.01);");
        Button[] dirs = new Button[4];
        dirBtn(dirs, 0, pad, 1, 2, "\u27A1");   // right
        dirBtn(dirs, 1, pad, 2, 1, "\u2193");   // bottom
        dirBtn(dirs, 2, pad, 1, 0, "\u2B05");   // left
        dirBtn(dirs, 3, pad, 0, 1, "\u2191");   // top
        StackPane padWrap = new StackPane(pad);
        StackPane.setAlignment(padWrap, Pos.BOTTOM_LEFT);
        bp.setBottom(padWrap);
        BorderPane.setAlignment(padWrap, Pos.BOTTOM_LEFT);
        BorderPane.setMargin(padWrap, new Insets(0, 0, 56, 12));
        padWrap.setMouseTransparent(false);

        StackPane root = (StackPane) m.sceneRef.getRoot();
        root.getChildren().add(hudRoot);
        hudRoot.setOpacity(0.95);
        hudRoot.setVisible(false);
    }

    private static void showHudNow() {
        if (hudRoot == null) return;
        hudRoot.setVisible(true);
    }

    private static void hideHud() {
        if (hudRoot != null) hudRoot.setVisible(false);
    }

    private static void dirBtn(Button[] dirs, int idx, GridPane pad, int col, int row, String glyph) {
        Button b = new Button(glyph);
        b.setMinSize(58, 50);
        b.setFont(Font.font(18));
        b.setStyle("-fx-background-color: rgba(20, 32, 60, 0.55); -fx-text-fill: #BFF9FF;"
                + "-fx-background-radius: 12; -fx-cursor: hand; -fx-font-size: 18px;");
        b.setOpacity(0.75);
        b.setOnMouseEntered(e -> { b.setOpacity(1); });
        b.setOnMouseExited(e -> { b.setOpacity(0.75); });
        b.setOnAction(e -> { playSfx(sClick, 0.5); setPlayerDir(idx); });
        b.setUserData(idx);
        pad.add(b, col, row);
        dirs[idx] = b;
    }

    private static void setPlayerDir(int d) {
        if (mt == null) return;
        mt.direction = d;
    }

    // ======================================================================
    //  SECTION 10 — RESULT / GAME-OVER SCREEN
    // ======================================================================

    static void showResult(boolean victory, boolean newHigh) {
        if (mt == null || mt.sceneRef == null) return;
        hideCurrentOverlay();
        hideHud();
        long survSec = Math.max(0, (System.currentTimeMillis()
                - (run == null ? System.currentTimeMillis() : run.survivalStart)) / 1000);
        int score = mt.score;
        int length = mt.bodyParts.size() + 1;
        int waves = run == null ? 0 : run.wave;
        int coinsFound = run == null ? 0 : (run.coinsRun + run.gemsRun);
        int starsFound = run == null ? 0 : run.starsRun;

        GameProfile p = profile;
        if (run != null) {
            p.totalFruit += run.fruitRun;
            p.totalGolden += run.goldenRun;
        }
        int xpEarned = score + (int) Math.min(survSec / 5.0, 60) + waves * 10 + coinsFound * 2 + starsFound * 150;
        int coinsEarned = score / 2 + coinsFound * 5 + waves * 15 + (victory ? 75 : 0) + starsFound * 50;
        p.coins += coinsEarned;
        List<String> toasts = new ArrayList<>();
        p.addXp(xpEarned, toasts);
        p.totalTimeSec += (int) survSec;
        p.totalDeaths += victory ? 0 : 1;
        p.maxLength = Math.max(p.maxLength, length);

        String modeName = (run == null ? GameMode.CLASSIC : run.mode).name();
        int best = p.bestScores.getOrDefault(modeName, 0);
        if (score > best) {
            p.bestScores.put(modeName, score);
        }
        int extra = p.bestExtra.getOrDefault(modeName, 0);
        int extraNow = switch (modeName) {
            case "WAVE" -> waves;
            case "TREASURE" -> run == null ? 0 : run.treasureCoins;
            case "BOSS" -> p.bossWins;
            case "ENDLESS" -> length;
            default -> coinsFound;
        };
        if (extraNow > extra) {
            p.bestExtra.put(modeName, extraNow);
        }

        List<AchievementRow> newAch = updateAchievements(p, score, length, survSec, waves, starsFound, victory, modeName);
        List<MissionDoneRow> missDone = updateMissions(p);
        int coinsFromMissions = 0;
        for (MissionDoneRow r : missDone) coinsFromMissions += r.reward;
        p.coins += coinsFromMissions;

        saveProfile();
        refreshChip();

        StackPane overlay = new StackPane();
        overlay.setPickOnBounds(true);
        overlay.setId("screen-overlay");
        overlay.setStyle("-fx-background-color: rgba(2, 6, 18, 0.82);");
        VBox card = resultCard(victory, newHigh, score, length, survSec, xpEarned,
                coinsEarned + coinsFromMissions, best, toasts, newAch, missDone, modeName, waves, coinsFound);
        overlay.getChildren().add(card);
        StackPane.setAlignment(card, Pos.CENTER);
        StackPane root = (StackPane) mt.sceneRef.getRoot();
        root.getChildren().add(overlay);
        currentOverlay = overlay;
        softIn(card);
    }

    static final class AchievementRow {
        String name;
        AchievementRow(String n) { name = n; }
    }

    static final class MissionDoneRow {
        String name;
        int reward;
        MissionDoneRow(String n, int r) { name = n; reward = r; }
    }

    private static List<AchievementRow> updateAchievements(GameProfile p, int score, int length, long survSec, int waves,
                                                           int starsFound, boolean victory, String modeName) {
        List<AchievementRow> out = new ArrayList<>();
        checkAch(out, p, "a1", p.totalFruit >= 1);
        checkAch(out, p, "a2", p.totalGolden >= 3);
        checkAch(out, p, "a3", survSec >= 180);
        checkAch(out, p, "a4", p.maxLength >= 15);
        checkAch(out, p, "a5", p.level >= 3);
        checkAch(out, p, "a6", p.bossWins >= 1);
        checkAch(out, p, "a7", p.treasuresFound >= 12);
        checkAch(out, p, "a8", starsFound >= 1);
        checkAch(out, p, "a9", p.villainKills >= 10);
        checkAch(out, p, "a10", victory && "ARENA".equals(modeName));
        return out;
    }

    private static void checkAch(List<AchievementRow> out, GameProfile p, String id, boolean cond) {
        if (!cond) return;
        if (p.achDone.contains(id)) return;
        p.achDone.add(id);
        for (String[] a : ACHIEVEMENTS) if (a[0].equals(id)) out.add(new AchievementRow(a[1]));
    }

    private static List<MissionDoneRow> updateMissions(GameProfile p) {
        List<MissionDoneRow> out = new ArrayList<>();
        for (MissionDef m : MISSIONS) {
            if (!p.missionsDone.contains(m.id) && m.progress(p) >= m.target) {
                p.missionsDone.add(m.id);
                out.add(new MissionDoneRow(m.desc, m.rewardCoins(p)));
            }
        }
        return out;
    }

    // ======================================================================
    //  SECTION 11 — SKIN BRIDGE (legacy SkinType or custom-painted skins)
    // ======================================================================

    private static final Map<String, Main.SkinType> LEGACY_SKIN = Map.of(
            "CLASSIC", Main.SkinType.CLASSIC,
            "LIME", Main.SkinType.LIME,
            "NEON", Main.SkinType.NEON,
            "OCEAN", Main.SkinType.OCEAN,
            "RAINBOW", Main.SkinType.RAINBOW,
            "COSMIC", Main.SkinType.COSMIC);

    static void applySkinVisual() {
        if (mt == null || profile == null) return;
        Main.SkinType st = LEGACY_SKIN.get(profile.skin);
        if (st != null) {
            mt.currentSkin = st;
            mt.applySkinToPlayerSnake();
            return;
        }
        // custom-painted skins (FIRE / ICE / SHADOW / GOLD) — paint nodes directly
        try {
            SkinDef def = skinDef(profile.skin);
            if (mt.head != null) {
                PhongMaterial hm = new PhongMaterial(def.head);
                hm.setSpecularColor(def.glow);
                mt.head.setMaterial(hm);
            }
            int idx = 0;
            for (Sphere s : mt.bodyParts) {
                PhongMaterial bm = new PhongMaterial(def.body.get(idx % def.body.size()));
                bm.setSpecularColor(def.glow);
                s.setMaterial(bm);
                idx++;
            }
        } catch (Throwable ignore) {
        }
    }

    // ======================================================================
    //  SECTION 12 — RUN LIFECYCLE
    // ======================================================================

    private static javafx.stage.Stage stageRef;
    private static Canvas hudMap;
    private static Sphere shieldBubble;
    private static long lastNano = 0L;

    static void startSelected(GameMode mode) {
        if (mt == null) return;
        pendingMode = mode;
        newRun();
        run.runActive = true;
        playSfx(sStart, 0.9);
        hideCurrentOverlay();
        mt.resetSnake();                     // Engine.applyModeFromReset() hooks in via resetSnake
        try { mt.scoreText.setVisible(false); } catch (Throwable ignore) {}
        applySkinVisual();
        showHudNow();
        mt.running = true;
        try { if (mt.timer != null) mt.timer.start(); } catch (Throwable ignore) {}
        try { mt.stopStartButton.setText("Stop"); } catch (Throwable ignore) {}
        toast(mode.title + " — go!");
    }

    static void applyModeFromReset() {
        if (run == null || !run.runActive) return;
        // clear leftovers from previous run
        for (SVillain v : run.extras) {
            try { v.removeFrom(mt.gameElements); } catch (Throwable ignore) {}
        }
        run.extras.clear();
        for (ItemDef it : run.items) {
            try { mt.gameElements.getChildren().remove(it.node); } catch (Throwable ignore) {}
        }
        run.items.clear();
        run.powerEnds.clear();
        run.coinsRun = 0;
        run.gemsRun = 0;
        run.crystalsRun = 0;
        run.mysteryBoxes = 0;
        run.starsRun = 0;
        run.wave = 1;
        run.nextWaveIn = 10;
        run.nextItemIn = 6;
        run.timeLeft = 120;
        run.treasureCoins = 0;
        run.boss = null;
        run.enemiesOutscored = 0;
        run.spawnClock = 0;
        run.fruitRun = 0;
        run.goldenRun = 0;
        hideShield();
        // boss world adjustments per mode
        GameMode gm = run.mode;
        double lvlBoost = Math.min(0.4, (profile.level - 1) * 0.05);
        try {
            mt.villainSpeed += lvlBoost;
            mt.villain2Speed += lvlBoost;
            mt.villain3Speed += lvlBoost;
        } catch (Throwable ignore) {}
        switch (gm) {
            case ARENA: {
                spawnEnemyExtras(mt, 3, mt.villainSpeed * 1.15);
                toast("Battle Arena: 3 rival hunters joined!");
                break;
            }
            case BOSS: {
                spawnBoss(mt);
                toast("The VENOM MONARCH approaches...");
                break;
            }
            case WAVE: {
                run.nextWaveIn = 8;
                toast("Wave Survival started!");
                break;
            }
            case TIME: {
                run.timeLeft = 120;
                toast("Time Challenge: 120 seconds!");
                break;
            }
            case ENDLESS: {
                spawnEnemyExtras(mt, 2, mt.villainSpeed);
                toast("Endless Growth: the arena never rests.");
                break;
            }
            case TREASURE: {
                spawnTreasureSeed(mt);
                toast("Find the 12 hidden treasure coins!");
                break;
            }
            default: break;
        }
    }

    private static void spawnBoss(Main m) {
        try {
            List<Color> pal = List.of(Color.web("#4B0082"), Color.web("#9400D3"), Color.web("#DA70D6"));
            SVillain b = new SVillain(randX(m), randY(m), Math.max(1.4, m.villainSpeed * 0.75), pal, true, "VENOM MONARCH");
            b.lungeCd = 5.0;
            run.extras.add(b);
            b.addTo(m.gameElements);
            run.boss = b;
        } catch (Throwable ignore) {
        }
    }

    private static void spawnEnemyExtras(Main m, int n, double speed) {
        for (int i = 0; i < n && run.extras.size() < 9; i++) {
            try {
                SVillain v = new SVillain(randX(m), randY(m), Math.max(1.2, speed * (0.85 + Math.random() * 0.35)),
                        pickPalette(), false, "Hunter");
                run.extras.add(v);
                v.addTo(m.gameElements);
            } catch (Throwable ignore) {
            }
        }
    }

    private static void spawnTreasureSeed(Main m) {
        // a few starting coins so the hunt begins immediately
        try {
            for (int i = 0; i < 3; i++) {
                run.nextItemIn = 0.2; // forces an early roll via tick cadence
            }
        } catch (Throwable ignore) {
        }
    }

    private static final List<List<Color>> VILLAIN_PALETTES = List.of(
            List.of(Color.web("#3E1F47"), Color.web("#A3D5FF"), Color.web("#5D2A63")),
            List.of(Color.web("#1B3B36"), Color.web("#03DAC6"), Color.web("#0F5257")),
            List.of(Color.web("#3B1F2B"), Color.web("#FF5C8A"), Color.web("#6D1B36")),
            List.of(Color.web("#2F3E1C"), Color.web("#CDDC39"), Color.web("#4a5d23")),
            List.of(Color.web("#10002B"), Color.web("#7B2CBF"), Color.web("#3C096C")),
            List.of(Color.web("#3D2614"), Color.web("#E8A26A"), Color.web("#6F4518")),
            List.of(Color.web("#0B132B"), Color.web("#5BC0EB"), Color.web("#1C2541")));

    private static List<Color> pickPalette() {
        return VILLAIN_PALETTES.get(new Random().nextInt(VILLAIN_PALETTES.size()));
    }

    private static double randX(Main m) {
        return Main.HEAD_RADIUS * 3 + Math.random() * Math.max(50, m.windowWidth - Main.HEAD_RADIUS * 6);
    }

    private static double randY(Main m) {
        try {
            double top = m.topLine.getStartY() + Main.HEAD_RADIUS * 3 + Main.LINE_MARGIN;
            double bot = m.bottomLine.getStartY() - Main.HEAD_RADIUS * 3 - Main.LINE_MARGIN;
            if (bot - top < 40) return m.windowHeight / 2;
            return top + Math.random() * (bot - top);
        } catch (Throwable t) {
            return m.windowHeight * 0.5;
        }
    }

    private static void endRun(boolean victory) {
        if (run == null || !run.runActive) return;
        run.runActive = false;
        mt.running = false;
        try { if (mt.timer != null) mt.timer.stop(); } catch (Throwable ignore) {}
        try { mt.pauseGoldenFruitTimers(); } catch (Throwable ignore) {}
        playSfx(victory ? sStart : sOver, 1.0);
        boolean newHigh = mt.score > mt.highScore;
        if (newHigh) {
            mt.highScore = mt.score;
        }
        showResult(victory, newHigh);
    }

    static void onPlayerDeath(Main m, boolean newHigh) {
        if (run == null || !run.runActive) return;
        run.runActive = false;
        showResult(false, newHigh);
    }

    static void onVillainDown(Main m) {
        if (profile == null) return;
        profile.villainKills++;
        profile.coins += 12;
        if (run != null && run.runActive) run.enemiesOutscored++;
    }

    // ======================================================================
    //  SECTION 13 — SCORE / XP / COIN HOOKS (called from Main)
    // ======================================================================

    static int addFruitScore(Main m) {
        int mult = 1;
        if (run != null && run.runActive && run.powerEnds.containsKey("DOUBLE")) mult = 2;
        if (run != null && run.runActive) {
            run.fruitRun++;
            if (run.mode == GameMode.ENDLESS && run.fruitRun % 12 == 0) {
                try {
                    m.villainSpeed += 0.22;
                    m.villain2Speed += 0.22;
                    m.villain3Speed += 0.22;
                    if (run.extras.size() < 6 && run.fruitRun % 24 == 0) {
                        spawnEnemyExtras(m, 1, m.villainSpeed * 0.9);
                    }
                } catch (Throwable ignore) {}
            }
        }
        return mult;
    }

    static void onGoldenEaten(Main m) {
        if (run != null && run.runActive) {
            run.goldenRun++;
        }
        toast("GOLDEN FRUIT — SPEED BOOST!");
        playSfx(sEat, 0.9);
    }

    // ======================================================================
    //  SECTION 14 — PER-FRAME TICKS
    // ======================================================================

    static void tickGame(Main m) {
        if (m == null || run == null || !run.runActive || !m.running) return;
        long now = System.nanoTime();
        double dt = lastNano == 0L ? 1.0 / 60.0 : Math.min(0.1, (now - lastNano) / 1.0e9);
        lastNano = now;
        run.spawnClock += dt;
        run.objective += dt;

        switch (run.mode) {
            case TIME: {
                run.timeLeft -= dt;
                if (run.timeLeft <= 0) {
                    toast("TIME'S UP!");
                    endRun(true);
                    return;
                }
                break;
            }
            case WAVE: {
                run.nextWaveIn -= dt;
                if (run.nextWaveIn <= 0) {
                    int count = Math.min(8, run.wave + 1);
                    spawnEnemyExtras(m, count, m.villainSpeed * (0.85 + run.wave * 0.05));
                    run.wave++;
                    run.nextWaveIn = Math.max(12, 24 - run.wave * 1.2);
                    toast("WAVE " + run.wave + " INCOMING!");
                }
                break;
            }
            case TREASURE: {
                int target = 12;
                if (run.nextItemIn <= 0) {
                    spawnRandomItem(m);
                    run.nextItemIn = 4 + Math.random() * 4;
                } else {
                    run.nextItemIn -= dt;
                }
                if (run.treasureCoins >= target) {
                    profile.treasuresFound += run.treasureCoins;
                    endRun(true);
                    return;
                }
                break;
            }
            case BOSS: {
                if (run.spawnClock >= 90) {
                    endRun(true);
                    return;
                }
                break;
            }
            case ARENA: {
                if (mt.score >= 40) {
                    endRun(true);
                    return;
                }
                break;
            }
            default: break;
        }

        // spawn cadence for extra collectibles (all modes)
        if (run.mode != GameMode.TREASURE) {
            run.nextItemIn -= dt;
            if (run.nextItemIn <= 0 && run.items.size() < ("LOW".equals(profile.quality) ? 4 : 6)) {
                spawnRandomItem(m);
                run.nextItemIn = 7 + Math.random() * 7;
            }
        }

        // extras AI
        for (SVillain v : new ArrayList<>(run.extras)) {
            if (!v.alive) continue;
            steerVillain(v, m, dt);
            extraEatFruit(v, m);
        }

        // shield bubble follows head
        if (run.powerEnds.containsKey("SHIELD")) {
            ensureShieldBubble(m);
            try {
                shieldBubble.setTranslateX(m.headX);
                shieldBubble.setTranslateY(m.headY);
            } catch (Throwable ignore) {}
        }

        // power-up expiry
        expirePowerups();

        // generate objectives strings + minimap
        if (run.frame % 8 == 0) {
            refreshHud(m);
        }
        if (run.frame % 6 == 0) {
            drawMinimap(m);
        }
        run.frame++;
    }

    static void tickPost(Main m, javafx.stage.Stage stage) {
        if (m == null || run == null || !run.runActive || !m.running) return;
        checkExtraCollisions(m, stage);
        if (run == null || !run.runActive) return;
        magnetPull(m);
        checkItemPickup(m);
        if (run.frame % 2 == 0) {
            for (ItemDef it : new ArrayList<>(run.items)) {
                try {
                    if (it.node == null || !m.gameElements.getChildren().contains(it.node)) continue;
                    it.node.setRotate(it.node.getRotate() + 1.3);
                    it.node.setTranslateZ(Math.sin(run.frame / 14.0 + it.pulse) * 7.0);
                } catch (Throwable ignore) {}
            }
        }
    }

    private static void removeItem(Main m, ItemDef it) {
        try { m.gameElements.getChildren().remove(it.node); } catch (Throwable ignore) {}
        run.items.remove(it);
    }

    // ======================================================================
    //  SECTION 15 — EXTRA-VILLAIN AI
    // ======================================================================

    private static final double[][] DIRS = { {1, 0}, {0, 1}, {-1, 0}, {0, -1} };

    private static void steerVillain(SVillain v, Main m, double dt) {
        int best = (int) v.dir;
        double bestScore = -1e9;
        boolean shieldActive = run.powerEnds.containsKey("SHIELD");
        for (int d = 0; d < 4; d++) {
            double look = 40 + Math.random() * 60;
            double nx = v.x + DIRS[d][0] * look;
            double ny = v.y + DIRS[d][1] * look;
            double sc = 0;
            double fd = nearestFruitDist(nx, ny, m);
            sc += v.greedy * Math.max(0, 700 - fd) * 0.02;
            double pd;
            try { pd = Math.hypot(nx - m.headX, ny - m.headY); } catch (Throwable t) { pd = 500; }
            if (!shieldActive) {
                sc += v.chase * Math.max(0, 700 - pd) * 0.03;
            }
            if (v.flee > 0.02 && pd < 180) {
                sc -= v.flee * (180 - pd) * 0.1;
            }
            if (d == (int) v.dir) sc += 1.2;
            if (!pathClear(nx, ny, m, v)) sc -= 70;
            if (sc > bestScore) {
                bestScore = sc;
                best = d;
            }
        }
        v.dir = best;

        double sp = v.speed;
        if (v.boss) {
            v.lungeCd -= dt;
            if (v.lungeCd < -1.2) {
                v.lungeCd = 6.5;          // schedule next lunge
            }
            if (v.lungeCd < 0) {
                sp *= 2.1;                // lunge window active
            }
        }
        int d = (int) v.dir;
        v.x += DIRS[d][0] * sp;
        v.y += DIRS[d][1] * sp;
        wrapPoint(m, () -> v.x, (dx) -> v.x = dx, 0);
        wrapPoint(m, () -> v.y, (dy) -> v.y = dy, 1);
        advanceBody(v, m);
        v.place();
    }

    // functional wrapper to apply wrap on a coordinate without duplicating math
    private interface CoordGetter { double get(); }
    private interface CoordSetter { void set(double v); }

    private static void wrapPoint(Main m, CoordGetter getter, CoordSetter setter, int axis) {
        double val = getter.get();
        if (axis == 0) {
            double r = Main.HEAD_RADIUS;
            if (val > m.windowWidth - r * 2) val = r * 2;
            if (val < r * 2) val = m.windowWidth - r * 2;
        } else {
            try {
                double top = m.topLine.getStartY() + Main.HEAD_RADIUS * 1.6;
                double bot = m.bottomLine.getStartY() - Main.HEAD_RADIUS * 1.6;
                if (val > bot) val = top;
                if (val < top) val = bot;
            } catch (Throwable ignore) {}
        }
        setter.set(val);
    }

    private static void advanceBody(SVillain v, Main m) {
        v.hist.add(0, new double[] { v.x, v.y });
        while (v.hist.size() > 240) {
            v.hist.remove(v.hist.size() - 1);
        }
        double spacing = Math.max(4.0, Main.SEGMENT_SPACING);
        double frameStep = Math.max(2.0, spacing / Math.max(0.4, v.speed));
        for (int i = 0; i < v.body.size(); i++) {
            Sphere s = v.body.get(i);
            if (s == null) continue;
            int idx = (int) (2 + (i + 1) * frameStep);
            double[] p = idx < v.hist.size() ? v.hist.get(idx) : v.hist.get(v.hist.size() - 1);
            s.setTranslateX(p[0]);
            s.setTranslateY(p[1]);
        }
    }

    private static double nearestFruitDist(double x, double y, Main m) {
        double best = 800;
        try {
            for (Sphere f : m.fruits) {
                if (f == null || !m.gameElements.getChildren().contains(f)) continue;
                double d = Math.hypot(f.getTranslateX() - x, f.getTranslateY() - y);
                if (d < best) best = d;
            }
        } catch (Throwable ignore) {}
        try {
            if (m.goldenFruitVisible && m.goldenFruit != null) {
                double d = Math.hypot(m.goldenFruit.getTranslateX() - x, m.goldenFruit.getTranslateY() - y);
                if (d < best) best = d;
            }
        } catch (Throwable ignore) {}
        return best;
    }

    private static boolean pathClear(double x, double y, Main m, SVillain self) {
        double r = Main.HEAD_RADIUS * 1.4;
        for (Sphere s : m.bodyParts) {
            if (Math.abs(s.getTranslateX() - x) < r && Math.abs(s.getTranslateY() - y) < r) return false;
        }
        try {
            if (m.villainAlive && Math.hypot(m.villainX - x, m.villainY - y) < r) return false;
            if (m.villain2Alive && Math.hypot(m.villain2X - x, m.villain2Y - y) < r) return false;
            if (m.villain3Alive && Math.hypot(m.villain3X - x, m.villain3Y - y) < r) return false;
        } catch (Throwable ignore) {}
        try {
            if (blockedBody(m.villainBodyParts, x, y, r) || blockedBody(m.villain2BodyParts, x, y, r)
                    || blockedBody(m.villain3BodyParts, x, y, r)) return false;
        } catch (Throwable ignore) {}
        for (SVillain o : run.extras) {
            if (o == self || !o.alive) continue;
            if (Math.hypot(o.x - x, o.y - y) < r) return false;
            for (Sphere s : o.body) {
                if (s != null && Math.hypot(s.getTranslateX() - x, s.getTranslateY() - y) < r) return false;
            }
        }
        return true;
    }

    private static boolean blockedBody(List<Sphere> list, double x, double y, double r) {
        if (list == null) return false;
        for (Sphere s : list) {
            if (s == null) continue;
            if (Math.abs(s.getTranslateX() - x) < r && Math.abs(s.getTranslateY() - y) < r) return true;
        }
        return false;
    }

    private static void extraEatFruit(SVillain v, Main m) {
        try {
            for (Sphere f : m.fruits) {
                if (f == null || !m.gameElements.getChildren().contains(f)) continue;
                double dx = f.getTranslateX() - v.x;
                double dy = f.getTranslateY() - v.y;
                if (Math.hypot(dx, dy) < Main.HEAD_RADIUS * 1.8) {
                    m.gameElements.getChildren().remove(f);
                    v.fruitsEaten++;
                    PauseTransition pt = new PauseTransition(Duration.seconds(0.6));
                    pt.setOnFinished(e -> {
                        try { m.respawnSingleFruit(m.gameElements, f); } catch (Throwable ignore) {}
                    });
                    pt.play();
                    break;
                }
            }
        } catch (Throwable ignore) {
        }
    }

    private static void killExtra(SVillain v, Main m, int reward) {
        if (!v.alive) return;
        v.alive = false;
        try {
            v.removeFrom(m.gameElements);
        } catch (Throwable ignore) {}
        if (reward > 0) {
            profile.coins += reward;
            profile.villainKills++;
        }
        toast("Defeated " + v.name + "! +" + reward + " coins");
        if (run != null && run.runActive) run.enemiesOutscored++;
        PauseTransition pt = new PauseTransition(Duration.seconds(4));
        pt.setOnFinished(e -> respawnExtra(v, m));
        pt.play();
    }

    private static void respawnExtra(SVillain v, Main m) {
        if (run == null || !run.runActive || !run.extras.contains(v)) return;

        v.x = randX(m);
        v.y = randY(m);
        if (Math.hypot(v.x - m.headX, v.y - m.headY) < 240) {
            v.x = (v.x + m.windowWidth / 2.0) % m.windowWidth;
        }
        v.rebuildBodyNow();
        v.addTo(m.gameElements);
        v.alive = true;
        v.place();
    }

    private static void defeatBoss(Main m) {
        SVillain b = run.boss;
        if (b == null) return;
        try {
            b.removeFrom(m.gameElements);
        } catch (Throwable ignore) {}
        run.extras.remove(b);
        run.boss = null;
        profile.bossWins++;
        profile.coins += 60;
        toast("MONARCH DEFEATED!! +60 coins");
        endRun(true);
    }

    private static void checkExtraCollisions(Main m, javafx.stage.Stage stage) {
        double hr = Main.HEAD_RADIUS;
        boolean shield = run.powerEnds.containsKey("SHIELD");
        for (SVillain v : new ArrayList<>(run.extras)) {
            if (!v.alive) continue;
            // player head vs villain head
            if (Math.hypot(v.x - m.headX, v.y - m.headY) < hr * 2.0) {
                if (shield && !v.boss) {
                    run.powerEnds.remove("SHIELD");
                    hideShield();
                    killExtra(v, m, 5);
                    toast("SHIELD absorbed the hit!");
                } else {
                    m.gameOver(stage);
                    return;
                }
            } else {
                // player head vs villain body
                boolean died = false;
                for (Sphere s : v.body) {
                    if (s == null) continue;
                    if (Math.hypot(s.getTranslateX() - m.headX, s.getTranslateY() - m.headY) < hr * 1.6) {
                        if (shield && !v.boss) {
                            run.powerEnds.remove("SHIELD");
                            hideShield();
                            killExtra(v, m, 5);
                        } else {
                            m.gameOver(stage);
                        }
                        died = true;
                        break;
                    }
                }
                if (died) {
                    return;
                }
            }
            // villain head vs player body → villain dies (competition trap)
            if (run != null && run.runActive && v.alive) {
                for (Sphere ps : m.bodyParts) {
                    if (ps == null) continue;
                    if (Math.hypot(v.x - ps.getTranslateX(), v.y - ps.getTranslateY()) < hr * 1.8) {
                        if (v.boss) {
                            v.hp--;
                            if (v.hp <= 0) {
                                defeatBoss(m);
                            } else {
                                toast("MONARCH HP " + v.hp + "/3");
                            }
                        } else {
                            killExtra(v, m, 8);
                        }
                        break;
                    }
                }
            }
        }
    }

    // ======================================================================
    //  SECTION 16 — COLLECTIBLE SYSTEM
    // ======================================================================

    private static ItemKind rollKind() {
        int r = run.rnd.nextInt(100);
        if (run.mode == GameMode.TREASURE) {
            if (r < 55) return ItemKind.COIN;
            if (r < 65) return ItemKind.GEM;
            if (r < 76) return ItemKind.MYSTERY;
            if (r < 89) return ItemKind.BOMB;
            return ItemKind.TRAP;
        }
        if (r < 20) return ItemKind.COIN;
        if (r < 30) return ItemKind.GEM;
        if (r < 38) return ItemKind.CRYSTAL;
        if (r < 46) return ItemKind.SPEED_ORB;
        if (r < 55) return ItemKind.SHIELD;
        if (r < 63) return ItemKind.MAGNET;
        if (r < 69) return ItemKind.DOUBLE;
        if (r < 75) return ItemKind.GROWTH;
        if (r < 79) return ItemKind.SHRINK;
        if (r < 82) return ItemKind.HEART;
        if (r < 91) return ItemKind.MYSTERY;
        if (r < 94) return ItemKind.BOMB;
        if (r < 98) return ItemKind.TRAP;
        return ItemKind.STAR;
    }

    private static void spawnRandomItem(Main m) {
        try {
            ItemKind kind = rollKind();
            javafx.scene.Node node = itemNode(kind);
            double x = randX(m);
            double y = randY(m);
            node.setTranslateX(x);
            node.setTranslateY(y);
            node.setTranslateZ(0);
            m.gameElements.getChildren().add(node);
            run.items.add(new ItemDef(kind, node));
        } catch (Throwable ignore) {
        }
    }

    private static javafx.scene.Node itemNode(ItemKind k) {
        switch (k) {
            case COIN: {
                Cylinder c = new Cylinder(10, 4, 24);
                PhongMaterial pm = new PhongMaterial(Color.web("#FFD700"));
                pm.setSpecularColor(Color.WHITE);
                c.setMaterial(pm);
                try { c.getTransforms().add(new javafx.scene.transform.Rotate(90, javafx.scene.transform.Rotate.X_AXIS)); } catch (Throwable ignore) {}
                return c;
            }
            case GEM: {
                Box b = new Box(15, 15, 15);
                PhongMaterial pm = new PhongMaterial(Color.web("#00E5FF"));
                pm.setSpecularColor(Color.WHITE);
                b.setMaterial(pm);
                b.setRotate(45);
                return b;
            }
            case CRYSTAL: {
                Box b = new Box(10, 26, 10);
                PhongMaterial pm = new PhongMaterial(Color.web("#B266FF"));
                pm.setSpecularColor(Color.WHITE);
                b.setMaterial(pm);
                return b;
            }
            case SPEED_ORB: {
                Sphere s = new Sphere(11);
                s.setMaterial(new PhongMaterial(Color.ORANGE));
                return s;
            }
            case SHIELD: {
                Sphere inner = new Sphere(11);
                inner.setMaterial(new PhongMaterial(Color.web("#0E4BFF")));
                Sphere ring = new Sphere(15);
                PhongMaterial rm = new PhongMaterial(Color.web("#59F7FF"));
                ring.setMaterial(rm);
                ring.setOpacity(0.4);
                return new Group(inner, ring);
            }
            case MAGNET: {
                Sphere core = new Sphere(9);
                core.setMaterial(new PhongMaterial(Color.web("#C77DFF")));
                Sphere halo = new Sphere(14);
                PhongMaterial hm = new PhongMaterial(Color.web("#C77DFF"));
                halo.setMaterial(hm);
                halo.setOpacity(0.35);
                return new Group(core, halo);
            }
            case DOUBLE: {
                Box b = new Box(16, 16, 16);
                b.setMaterial(new PhongMaterial(Color.web("#39FF14")));
                b.setRotate(45);
                return b;
            }
            case GROWTH: {
                Sphere s = new Sphere(12);
                s.setMaterial(new PhongMaterial(Color.web("#66FF66")));
                return s;
            }
            case SHRINK: {
                Sphere s = new Sphere(10);
                s.setMaterial(new PhongMaterial(Color.web("#FF66CC")));
                return s;
            }
            case HEART: {
                Sphere s = new Sphere(11);
                s.setMaterial(new PhongMaterial(Color.web("#FF4D6D")));
                return s;
            }
            case MYSTERY: {
                Box b = new Box(16, 16, 16);
                PhongMaterial pm = new PhongMaterial(Color.web("#B8860B"));
                pm.setSpecularColor(Color.YELLOW);
                b.setMaterial(pm);
                return b;
            }
            case STAR: {
                Sphere core = new Sphere(10);
                PhongMaterial pm = new PhongMaterial(Color.WHITE);
                pm.setSpecularColor(Color.WHITE);
                core.setMaterial(pm);
                core.setOpacity(0.97);
                Sphere halo = new Sphere(17);
                halo.setMaterial(new PhongMaterial(Color.web("#F7BDFF")));
                halo.setOpacity(0.4);
                return new Group(core, halo);
            }
            case BOMB: {
                Sphere s = new Sphere(12);
                PhongMaterial pm = new PhongMaterial(Color.web("#8B0000"));
                pm.setSpecularColor(Color.RED);
                s.setMaterial(pm);
                return s;
            }
            case TRAP: {
                Box b = new Box(13, 7, 13);
                PhongMaterial pm = new PhongMaterial(Color.web("#66000D"));
                pm.setSpecularColor(Color.RED);
                b.setMaterial(pm);
                return b;
            }
            default: {
                Sphere s = new Sphere(10);
                s.setMaterial(new PhongMaterial(Color.LIME));
                return s;
            }
        }
    }

    private static void checkItemPickup(Main m) {
        try {
            double hr = Main.HEAD_RADIUS;
            for (ItemDef it : new ArrayList<>(run.items)) {
                if (it.node == null) continue;
                if (!m.gameElements.getChildren().contains(it.node)) continue;
                double dx = m.headX - it.node.getTranslateX();
                double dy = m.headY - it.node.getTranslateY();
                if (Math.hypot(dx, dy) < hr + 14) {
                    applyPickup(m, it);
                    removeItem(m, it);
                }
            }
        } catch (Throwable ignore) {
        }
    }

    private static void applyPickup(Main m, ItemDef it) {
        switch (it.kind) {
            case COIN: {
                profile.coins += 5;
                run.coinsRun++;
                if (run.mode == GameMode.TREASURE) {
                    run.treasureCoins++;
                    if (run.treasureCoins < 12) toast("Treasure " + run.treasureCoins + "/12");
                }
                toast("Coin! +5");
                playSfx(sEat, 0.7);
                break;
            }
            case GEM: {
                profile.coins += 10;
                run.gemsRun++;
                toast("Gem! +10 coins");
                playSfx(sEat, 0.8);
                break;
            }
            case CRYSTAL: {
                profile.addXp(25, null);
                run.crystalsRun++;
                toast("Rare Crystal: +25 XP");
                playSfx(sEat, 0.8);
                break;
            }
            case SPEED_ORB: {
                m.speed += 1.6;
                run.powerEnds.put("SPEED", System.currentTimeMillis() + 6000);
                Timeline revert = new Timeline(new KeyFrame(Duration.seconds(6), e -> {
                    try { m.speed = Math.max(3, m.speed - 1.6); } catch (Throwable ignore) {}
                }));
                revert.play();
                toast("Speed up!");
                playSfx(sStart, 0.6);
                break;
            }
            case SHIELD: {
                run.powerEnds.put("SHIELD", System.currentTimeMillis() + 6000);
                toast("SHIELD UP — 6s invincibility");
                playSfx(sStart, 0.6);
                break;
            }
            case MAGNET: {
                run.powerEnds.put("MAGNET", System.currentTimeMillis() + 8000);
                toast("MAGNET — food flies to you");
                playSfx(sStart, 0.6);
                break;
            }
            case DOUBLE: {
                run.powerEnds.put("DOUBLE", System.currentTimeMillis() + 10000);
                toast("DOUBLE SCORE x2");
                playSfx(sStart, 0.6);
                break;
            }
            case GROWTH: {
                addPlayerSegments(m, 3);
                toast("Growth Booster: +3");
                playSfx(sEat, 0.9);
                break;
            }
            case SHRINK: {
                try {
                    m.shrinkSnake(m.bodyParts, m.gameElements);
                    m.shrinkSnake(m.bodyParts, m.gameElements);
                } catch (Throwable ignore) {}
                toast("Shrink Potion: -2");
                playSfx(sEat, 0.9);
                break;
            }
            case HEART: {
                addPlayerSegments(m, 2);
                profile.addXp(15, null);
                toast("Heart: +2 length, +15 XP");
                playSfx(sEat, 0.9);
                break;
            }
            case MYSTERY: {
                run.mysteryBoxes++;
                int r = run.rnd.nextInt(100);
                if (r < 30) {
                    profile.coins += 10;
                    toast("Mystery Box: +10 coins!");
                } else if (r < 55) {
                    addPlayerSegments(m, 2);
                    toast("Mystery Box: growth surge!");
                } else if (r < 70) {
                    run.powerEnds.put("SHIELD", System.currentTimeMillis() + 6000);
                    toast("Mystery Box: SHIELD!");
                } else if (r < 85) {
                    run.powerEnds.put("DOUBLE", System.currentTimeMillis() + 10000);
                    toast("Mystery Box: DOUBLE SCORE!");
                } else if (r < 95) {
                    profile.coins += 25;
                    toast("Mystery Box: +25 coins!");
                } else {
                    m.score = Math.max(0, m.score - 5);
                    try { m.scoreText.setText("Score: " + m.score); } catch (Throwable ignore) {}
                    toast("Mystery Box was a trap! -5");
                }
                playSfx(sStart, 0.7);
                break;
            }
            case STAR: {
                profile.coins += 50;
                profile.addXp(150, null);
                run.starsRun++;
                toast("LEGENDARY STAR!! +50 coins, +150 XP");
                playSfx(sStart, 1.0);
                break;
            }
            case BOMB: {
                m.score = Math.max(0, m.score - 5);
                try { m.scoreText.setText("Score: " + m.score); } catch (Throwable ignore) {}
                toast("VENOM BOMB! -5");
                playSfx(sOver == null ? sEat : sClick, 0.9);
                break;
            }
            case TRAP: {
                final double before = m.speed;
                m.speed = Math.max(2.0, before - 1.4);
                Timeline revert = new Timeline(new KeyFrame(Duration.seconds(4), e -> {
                    try { m.speed = Math.min(6.0, m.speed + 1.4); } catch (Throwable ignore) {}
                }));
                revert.play();
                toast("VENOM TRAP! Slow-down");
                playSfx(sOver == null ? sEat : sClick, 0.9);
                break;
            }
            default: break;
        }
        saveProfile();
    }

    private static void addPlayerSegments(Main m, int n) {
        try {
            for (int i = 0; i < n; i++) {
                Sphere last = m.bodyParts.get(m.bodyParts.size() - 1);
                Sphere seg = m.createPlayerBodySegmentLikeTail();
                seg.setTranslateX(last.getTranslateX());
                seg.setTranslateY(last.getTranslateY());
                seg.setTranslateZ(last.getTranslateZ());
                m.bodyParts.add(seg);
                m.gameElements.getChildren().add(seg);
            }
        } catch (Throwable ignore) {
        }
    }

    private static void magnetPull(Main m) {
        if (run == null || !run.runActive || !run.powerEnds.containsKey("MAGNET")) return;
        try {
            for (Sphere f : m.fruits) {
                if (f == null || !m.gameElements.getChildren().contains(f)) continue;
                double dx = m.headX - f.getTranslateX();
                double dy = m.headY - f.getTranslateY();
                double d = Math.hypot(dx, dy);
                if (d < 240 && d > 14) {
                    f.setTranslateX(f.getTranslateX() + dx / d * 2.6);
                    f.setTranslateY(f.getTranslateY() + dy / d * 2.6);
                }
            }
            if (m.goldenFruitVisible && m.goldenFruit != null) {
                double dx = m.headX - m.goldenFruit.getTranslateX();
                double dy = m.headY - m.goldenFruit.getTranslateY();
                double d = Math.hypot(dx, dy);
                if (d < 260 && d > 14) {
                    m.goldenFruit.setTranslateX(m.goldenFruit.getTranslateX() + dx / d * 2.6);
                    m.goldenFruit.setTranslateY(m.goldenFruit.getTranslateY() + dy / d * 2.6);
                }
            }
        } catch (Throwable ignore) {
        }
    }

    private static void ensureShieldBubble(Main m) {
        try {
            if (shieldBubble == null) {
                shieldBubble = new Sphere(Main.HEAD_RADIUS * 1.75);
                shieldBubble.setMaterial(new PhongMaterial(Color.web("#59F7FF")));
                shieldBubble.setOpacity(0.35);
                m.gameElements.getChildren().add(shieldBubble);
            }
            shieldBubble.setVisible(true);
        } catch (Throwable ignore) {
        }
    }

    private static void hideShield() {
        try {
            if (shieldBubble != null) shieldBubble.setVisible(false);
        } catch (Throwable ignore) {}
    }

    private static void expirePowerups() {
        long now = System.currentTimeMillis();
        boolean changed = false;
        Iterator<Map.Entry<String, Long>> it = run.powerEnds.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> e = it.next();
            if (e.getValue() < now) {
                it.remove();
                changed = true;
                if ("SHIELD".equals(e.getKey())) {
                    hideShield();
                }
            }
        }
        if (changed) {
            rebuildPowerChips();
        }
    }

    // ======================================================================
    //  SECTION 17 — HUD TICK (objective text, powers, minimap)
    // ======================================================================

    private static void refreshHud(Main m) {
        try {
            hudScoreL.setText("Score: " + mt.score);
            hudLenL.setText("Length: " + (mt.bodyParts.size() + 1));
            if (hudXpBar != null) {
                hudXpBar.setProgress(Math.min(1, profile.xp / (double) profile.xpToNext()));
            }
            if (hudObjectiveL != null) hudObjectiveL.setText(objectiveText());
            if (hudWaveL != null) {
                if (run.mode == GameMode.BOSS) {
                    long left = Math.max(0, 90 - (long) run.spawnClock);
                    hudWaveL.setText("SURVIVE " + left + "s");
                } else if (run.mode == GameMode.TIME) {
                    hudWaveL.setText("");
                } else {
                    hudWaveL.setText("");
                }
            }
            if (hudBossL != null) {
                boolean boss = run.mode == GameMode.BOSS && run.boss != null && run.boss.alive;
                hudBossL.setVisible(boss);
                if (boss) {
                    hudBossL.setText(buildHpText(run.boss.hp, 3));
                }
            }
        } catch (Throwable ignore) {
        }
        rebuildPowerChips();
    }

    private static String buildHpText(int hp, int max) {
        StringBuilder sb = new StringBuilder("VENOM MONARCH  ");
        for (int i = 0; i < max; i++) sb.append(i < hp ? "<3 " : "x ");
        return sb.toString();
    }

    private static String objectiveText() {
        switch (run.mode) {
            case TIME: return "TIME  " + (int) Math.ceil(Math.max(0, run.timeLeft)) + "s";
            case TREASURE: return "TREASURE  " + run.treasureCoins + "/12";
            case ARENA: return "DOMINATION  " + mt.score + "/40";
            case BOSS: return "MONARCH HP " + (run.boss != null ? run.boss.hp : 0) + "/3";
            case ENDLESS: return "FRUITS  " + run.fruitRun;
            case WAVE: return "WAVE  " + run.wave;
            default: return "";
        }
    }

    private static void rebuildPowerChips() {
        try {
            if (hudPowers == null) return;
            hudPowers.getChildren().clear();
            for (Map.Entry<String, Long> e : run.powerEnds.entrySet()) {
                long left = (e.getValue() - System.currentTimeMillis()) / 1000;
                String name = switch (e.getKey()) {
                    case "SHIELD" -> "SHIELD";
                    case "MAGNET" -> "MAGNET";
                    case "DOUBLE" -> "X2";
                    case "SPEED" -> "SPEED";
                    default -> e.getKey();
                };
                Label chip = new Label(name + " " + left + "s");
                chip.setTextFill(Color.WHITE);
                chip.setFont(Font.font(12));
                String col = switch (e.getKey()) {
                    case "SHIELD" -> "#1E88E5";
                    case "MAGNET" -> "#8E24AA";
                    case "DOUBLE" -> "#2E7D32";
                    default -> "#EF6C00";
                };
                chip.setStyle("-fx-background-color: " + col + "; -fx-text-fill: white; -fx-padding: 2 8 2 8;"
                        + "-fx-background-radius: 8; -fx-font-weight: bold;");
                hudPowers.getChildren().add(chip);
            }
        } catch (Throwable ignore) {
        }
    }

    private static void drawMinimap(Main m) {
        try {
            if (hudMap == null) return;
            javafx.scene.canvas.GraphicsContext g = hudMap.getGraphicsContext2D();
            double w = hudMap.getWidth();
            double h = hudMap.getHeight();
            g.clearRect(0, 0, w, h);
            javafx.scene.paint.Color bgc = new javafx.scene.paint.Color(0.02, 0.05, 0.12, 0.72);
            g.setFill(bgc);
            g.fillRect(0, 0, w, h);
            double topY = m.topLine.getStartY();
            double botY = m.bottomLine.getStartY();
            double sh = Math.max(1, botY - topY);
            double sx = w / m.windowWidth;
            double sy = h / sh;
            g.setStroke(Color.web("#00E5FF"));
            g.setLineWidth(1);
            g.strokeRect(0.5, 0.5, w - 1, h - 1);
            // food dots
            g.setFill(Color.web("#7CFD96", 0.8));
            for (Sphere f : m.fruits) {
                if (!m.gameElements.getChildren().contains(f)) continue;
                g.fillOval(f.getTranslateX() * sx - 1.5, (f.getTranslateY() - topY) * sy - 1.5, 3, 3);
            }
            // villains
            g.setFill(Color.web("#E34444"));
            if (m.villainAlive) g.fillOval(m.villainX * sx - 2, (m.villainY - topY) * sy - 2, 4, 4);
            if (m.villain2Alive) g.fillOval(m.villain2X * sx - 2, (m.villain2Y - topY) * sy - 2, 4, 4);
            if (m.villain3Alive) g.fillOval(m.villain3X * sx - 2, (m.villain3Y - topY) * sy - 2, 4, 4);
            g.setFill(Color.web("#FF2222"));
            for (SVillain v : run.extras) {
                if (!v.alive) continue;
                double r = v.boss ? 4 : 2.5;
                g.setFill(v.boss ? Color.PURPLE : Color.web("#FF2222"));
                g.fillOval(v.x * sx - r, (v.y - topY) * sy - r, r * 2, r * 2);
            }
            if (m.goldenFruitVisible) {
                g.setFill(Color.GOLD);
                g.fillOval(m.goldenFruit.getTranslateX() * sx - 2.5, (m.goldenFruit.getTranslateY() - topY) * sy - 2.5, 5, 5);
            }
            g.setFill(Color.LIMEGREEN);
            g.fillOval(m.headX * sx - 3, (m.headY - topY) * sy - 3, 6, 6);
        } catch (Throwable ignore) {
        }
    }

    // ======================================================================
    //  SECTION 18 — INPUT / AKILL / OBJECTIVE END
    // ======================================================================

    static void onKey(KeyCode code) {
        try {
            if (code == KeyCode.ESCAPE || code == KeyCode.SPACE || code == KeyCode.P) {
                if (currentOverlay != null && "pause".equals(activeScreen)) {
                    resumeFromPause();
                    return;
                }
                if (currentOverlay == null) {
                    if (mt.running) {
                        showPause();
                    } else if (run != null && run.runActive) {
                        resumeFromPause();
                    }
                }
            }
        } catch (Throwable ignore) {
        }
    }

    // ======================================================================
    //  SECTION 19 — RESULT CARD BUILDER
    // ======================================================================

    private static VBox resultCard(boolean victory, boolean newHigh, int score, int length, long survSec,
                                   int xpEarned, int coinsEarned, int best, List<String> toasts,
                                   List<AchievementRow> newAch, List<MissionDoneRow> missDone,
                                   String modeName, int waves, int coinsFound) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(20));
        card.setMaxWidth(520);
        card.setPrefHeight(480);
        card.setStyle("-fx-background-color: rgba(6, 10, 26, 0.95); -fx-background-radius: 22;"
                + "-fx-border-color: linear-gradient(to right, #00E5FF, #C77DFF); -fx-border-radius: 22; -fx-border-width: 1.6;");

        String modeTitle = modeName;
        try { modeTitle = GameMode.valueOf(modeName).title; } catch (Throwable ignore) {}

        Label head = new Label(victory ? "VICTORY!" : newHigh ? "NEW HIGH SCORE!" : "GAME OVER");
        head.setFont(Font.font(40));
        head.setTextFill(victory ? Color.web("#39FF14") : (newHigh ? Color.web("#FFD166") : Color.web("#FF6680")));
        try {
            head.setEffect(new DropShadow(22, victory ? Color.web("#39FF14") : Color.web("#FF3366")));
        } catch (Throwable ignore) {}

        VBox stats = new VBox(6);
        stats.setAlignment(Pos.CENTER_LEFT);
        stats.setPadding(new Insets(6, 18, 6, 18));
        stats.setStyle("-fx-background-color: rgba(12, 18, 40, 0.6); -fx-background-radius: 14;");
        statRow(stats, "Mode", modeTitle);
        statRow(stats, "Final Score", String.valueOf(score));
        statRow(stats, "Best", String.valueOf(best));
        statRow(stats, "Snake Length", String.valueOf(length));
        statRow(stats, "Survived", survSec + "s");
        if (waves > 1) statRow(stats, "Waves", String.valueOf(waves));
        if (coinsFound > 0) statRow(stats, "Coins Collected", String.valueOf(coinsFound));
        statRow(stats, "XP Earned", "+" + xpEarned);
        statRow(stats, "Coins Earned", "+" + coinsEarned);

        VBox rewards = new VBox(6);
        rewards.setAlignment(Pos.CENTER_LEFT);
        for (String t : toasts) {
            Label l = new Label("* " + t);
            l.setTextFill(Color.web("#FFD166"));
            l.setFont(Font.font(14));
            rewards.getChildren().add(l);
        }
        for (MissionDoneRow r : missDone) {
            Label l = new Label("> Mission complete: " + r.name + " (+" + r.reward + ")");
            l.setTextFill(Color.web("#34d17b"));
            l.setFont(Font.font(13));
            rewards.getChildren().add(l);
        }
        for (AchievementRow r : newAch) {
            Label l = new Label("* Achievement unlocked: " + r.name);
            l.setTextFill(Color.web("#ffc93c"));
            l.setFont(Font.font(13));
            rewards.getChildren().add(l);
        }
        if (rewards.getChildren().isEmpty()) {
            Label l = new Label("Progress banked. Complete missions/achievements for more coins!");
            l.setTextFill(Color.web("#7fa8c9"));
            l.setFont(Font.font(12));
            l.setWrapText(true);
            rewards.getChildren().add(l);
        }

        GameMode gmTry = GameMode.CLASSIC;
        try { gmTry = GameMode.valueOf(modeName); } catch (Throwable ignore) {}
        final GameMode gm = gmTry;
        Button again = menuButton("PLAY AGAIN", "#0e9b57", () -> Engine.startSelected(gm));
        Button menu2 = menuButton("MAIN MENU", "#5b3cc4", () -> {
            hideCurrentOverlay();
            Engine.showMenu();
        });
        Button exit = menuButton("EXIT GAME", "#8a1f2f", () -> {
            saveProfile();
            Platform.exit();
        });

        card.getChildren().addAll(head, stats, rewards, again, menu2, exit);
        return card;
    }

    private static void statRow(VBox box, String k, String v) {
        HBox row = new HBox();
        row.setPrefWidth(460);
        Label a = new Label(k);
        a.setTextFill(Color.web("#9BE7FF"));
        a.setFont(Font.font(15));
        Label b = new Label(v);
        b.setTextFill(Color.WHITE);
        b.setFont(Font.font(15));
        var spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        row.getChildren().addAll(a, spacer, b);
        box.getChildren().add(row);
    }

    // ======================================================================
    //  SECTION 20 — ARENA BACKGROUND PAINTING (per-arena canvas scenery)
    // ======================================================================

    private static double[] driftA, driftB, driftC;
    private static boolean driftInit = false;

    private static void ensureDrift(int n) {
        if (driftInit && driftA.length >= n) return;
        driftA = new double[Math.max(1, n)];
        driftB = new double[Math.max(1, n)];
        driftC = new double[Math.max(1, n)];
        for (int i = 0; i < n; i++) {
            driftA[i] = Math.random() * 900;
            driftB[i] = Math.random() * 600;
            driftC[i] = Math.random() + 0.3;
        }
        driftInit = true;
    }

    static void paintBackground(GraphicsContext gc, Main m) {
        Arena a = (run != null && run.runActive) ? run.arena : Arena.NEON;
        double w = m.windowWidth;
        double h = m.windowHeight;
        LinearGradient lg = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, a.skyTop), new Stop(1, a.skyBottom));
        gc.setFill(lg);
        gc.fillRect(0, 0, w, h);
        if ("LOW".equals(profile.quality)) {
            return;   // performance mode: flat sky only
        }
        try {
            switch (a) {
                case NEON: neonScene(gc, m); break;
                case JUNGLE: jungleScene(gc, m); break;
                case DESERT: desertScene(gc, m); break;
                case FROZEN: frozenScene(gc, m); break;
                case VOLCANIC: volcanicScene(gc, m); break;
                case COSMIC: cosmicScene(gc, m); break;
                default: break;
            }
        } catch (Throwable ignore) {
        }
    }

    static Color starColor() {
        Arena a = (run != null && run.runActive) ? run.arena : Arena.NEON;
        switch (a) {
            case NEON: return Color.web("#9BE7FF");
            case JUNGLE: return Color.web("#DFFFDF");
            case DESERT: return Color.web("#FFE2B8");
            case FROZEN: return Color.web("#E4F7FF");
            case VOLCANIC: return Color.web("#FFD2A8");
            case COSMIC: return Color.web("#E8D5FF");
            default: return Color.WHITE;
        }
    }

    private static void neonScene(GraphicsContext gc, Main m) {
        double w = m.windowWidth, h = m.windowHeight;
        gc.setStroke(Color.web("#00E5FF", 0.06));
        gc.setLineWidth(1);
        for (int x = 0; x < w; x += 60) {
            gc.strokeLine(x, 0, x, h);
        }
        for (int y = 0; y < h; y += 60) {
            gc.strokeLine(0, y, w, y);
        }
        gc.setStroke(Color.web("#C77DFF", 0.25));
        gc.setLineWidth(2);
        gc.strokeLine(0, h - 60, w, h - 60);
    }

    private static void jungleScene(GraphicsContext gc, Main m) {
        double w = m.windowWidth, h = m.windowHeight;
        // leaf silhouettes on both edges
        gc.setFill(Color.web("#0B4D1A", 0.4));
        for (int i = 0; i < 8; i++) {
            double lx = (i % 2 == 0) ? 10 : w - 70;
            double ly = 60 + i * (h - 100) / 8.0;
            gc.fillOval(lx, ly, 62, 34);
        }
        ensureDrift(18);
        long t = System.nanoTime() / 100000000L;
        gc.setFill(Color.web("#7CFD96", 0.5));
        for (int i = 0; i < 18; i++) {
            double fx = (driftA[i] + t * (1 + i % 3)) % w;
            double fy = (driftB[i] + t * driftC[i]) % h;
            gc.fillOval(fx, fy, 3, 3);
        }
    }

    private static void desertScene(GraphicsContext gc, Main m) {
        double w = m.windowWidth, h = m.windowHeight;
        gc.setFill(Color.web("#8A5A12", 0.35));
        for (int i = 0; i < 3; i++) {
            double baseY = h - 60 + i * 22;
            gc.fillPolygon(
                    new double[] { 0, w * 0.25, w * 0.5, w * 0.75, w },
                    new double[] { h, baseY - 30, baseY, baseY - 34, h },
                    5);
        }
        // pyramid silhouettes
        gc.setFill(Color.web("#31210a", 0.5));
        long t = System.nanoTime() / 900000000L;
        double px = ((t % 3) * 90) + 80;
        gc.fillPolygon(new double[] { px, px + 70, px + 140 }, new double[] { h - 70, h - 150, h - 70 }, 3);
        ensureDrift(24);
        gc.setFill(Color.web("#FFC680", 0.35));
        for (int i = 0; i < 24; i++) {
            double fx = (driftA[i] - (System.nanoTime() / 90000000L * driftC[i] * 3)) % w;
            if (fx < 0) fx += w;
            gc.fillOval(fx, (driftB[i] % h), 4, 2);
        }
    }

    private static void frozenScene(GraphicsContext gc, Main m) {
        double w = m.windowWidth, h = m.windowHeight;
        // crystal triangles
        gc.setStroke(Color.web("#cEF6FF", 0.3));
        gc.setLineWidth(2);
        for (int i = 0; i < 6; i++) {
            double cx = 40 + i * (w - 80) / 6.0;
            gc.strokeLine(cx, h - 30, cx + 22, h - 78);
            gc.strokeLine(cx + 22, h - 78, cx + 44, h - 30);
        }
        ensureDrift(30);
        long t = System.nanoTime() / 100000000L;
        gc.setFill(Color.web("#E4F7FF", 0.7));
        for (int i = 0; i < 30; i++) {
            double sy = (driftB[i] + t * driftC[i]) % h;
            double sx = (driftA[i] + Math.sin(t * 0.1 + i) * 24) % w;
            gc.fillOval(sx, sy, 2.4, 2.4);
        }
    }

    private static void volcanicScene(GraphicsContext gc, Main m) {
        double w = m.windowWidth, h = m.windowHeight;
        // glow at the bottom
        gc.setFill(Color.web("#902000", 0.25));
        gc.fillOval(w * 0.2, h - 90, w * 0.6, 120);
        ensureDrift(34);
        long t = System.nanoTime() / 100000000L;
        for (int i = 0; i < 34; i++) {
            double ey = h - ((driftB[i] + t * driftC[i] * 3) % h);
            double ex = (driftA[i] + Math.sin(t * 0.14 + i * 1.7) * 30) % w;
            gc.setFill(Color.web("#FF7A3D", 0.75));
            gc.fillOval(ex, ey, 2.6, 3.4);
        }
    }

    private static void cosmicScene(GraphicsContext gc, Main m) {
        double w = m.windowWidth, h = m.windowHeight;
        long t = System.nanoTime() / 200000000L;
        for (int i = 0; i < 4; i++) {
            double cx = (i * 240 + (t % 60)) % w;
            double cy = (i * 170) % h;
            gc.setFill(Color.web("#C99CFF", 0.06));
            gc.fillOval(cx, cy, 220, 160);
            gc.setFill(Color.web("#311066", 0.1));
            gc.fillOval(cx + 40, cy + 30, 140, 100);
        }
        // planet
        gc.setFill(Color.web("#5B2A86", 0.8));
        gc.fillOval(w - 150, 60, 64, 64);
        gc.setFill(Color.web("#C77DFF", 0.4));
        gc.fillOval(w - 160, 82, 84, 18);
    }
}
