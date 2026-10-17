import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
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
import javafx.scene.shape.Circle;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Line;
import javafx.scene.shape.Sphere;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.*;

public class Main extends Application {
    private double headX = 400;
    private double headY = 300;
    private double speed = 3;
    private int direction = 0; // 0=right, 1=down, 2=left, 3=up
    private double windowWidth = 800;
    private double windowHeight = 700;
    private final double HEAD_RADIUS = 18;
    private final double BODY_RADIUS = 17;
    private final double SEGMENT_SPACING = 16;
    private final double BOTTOM_OFFSET = 50;
    private final double LINE_MARGIN = 0;
    private Sphere head;
    private List<Sphere> bodyParts = new ArrayList<>();
    private List<Sphere> fruits = new ArrayList<>();
    private Random random = new Random();
    private int score = 0;
    private List<Sphere> specialItems = new ArrayList<>();
    private boolean specialItemVisible = false;
    private PauseTransition specialDespawnTimer;
    private Sphere lastSpecialItem = null;
    private Deque<double[]> positionHistory = new ArrayDeque<>();
    private Text scoreText;
    private Line topLine;
    private Line bottomLine;
    private long startTime;
    private boolean running = true;
    private Group gameElements;
    private AnimationTimer timer;
    private double villainX = 200;
    private double villainY = 200;
    private int villainDirection = 0;
    private List<Sphere> villainBodyParts = new ArrayList<>();
    private Deque<double[]> villainHistory = new ArrayDeque<>();
    private Sphere villainHead;
    private boolean villainAlive = true;
    private int framesSinceVillainTurn = 0;
    private static final int VILLAIN_TURN_COOLDOWN_FRAMES = 5;
    private PauseTransition villainRespawnTimer;
    private double villain2X = 400;
    private double villain2Y = 400;
    private int villain2Direction = 2;
    private List<Sphere> villain2BodyParts = new ArrayList<>();
    private Deque<double[]> villain2History = new ArrayDeque<>();
    private Sphere villain2Head;
    private boolean villain2Alive = true;
    private int framesSinceVillain2Turn = 0;
    private static final int VILLAIN2_TURN_COOLDOWN_FRAMES = 5;
    private PauseTransition villain2RespawnTimer;
    private double villain3X = 500;
    private double villain3Y = 500;
    private int villain3Direction = 3;
    private List<Sphere> villain3BodyParts = new ArrayList<>();
    private Deque<double[]> villain3History = new ArrayDeque<>();
    private Sphere villain3Head;
    private boolean villain3Alive = true;
    private int framesSinceVillain3Turn = 0;
    private static final int VILLAIN3_TURN_COOLDOWN_FRAMES = 5;
    private PauseTransition villain3RespawnTimer;
    private AudioClip eatSound;
    private MediaPlayer gameOverSound;
    private double villainSpeed = 3;
    private double villain2Speed = 3;
    private double villain3Speed = 3;
    private final double BASE_SPEED = 3;
    private final double SPEED_INCREMENT = 1;
    private int playerFruitCount = 0;
    private int villainFruitCount = 0;
    private int villain2FruitCount = 0;
    private int villain3FruitCount = 0;
    private Sphere goldenFruit;
    private boolean goldenFruitVisible = false;
    private final double GOLDEN_FRUIT_RADIUS = HEAD_RADIUS * 1.3;
    private final double GOLDEN_FRUIT_SPEED = 5.0;
    private final int GOLDEN_FRUIT_SHOW_DURATION = 5;
    private final int GOLDEN_FRUIT_APPEAR_INTERVAL = 10;
    private PauseTransition goldenFruitDisappearTimer;
    private PauseTransition goldenFruitRespawnTimer;
    private javafx.animation.ScaleTransition goldenPulse;
    private double previousPlayerSpeed = BASE_SPEED;
    private double previousVillainSpeed = BASE_SPEED;
    private double previousVillain2Speed = BASE_SPEED;
    private double previousVillain3Speed = BASE_SPEED;
    private PauseTransition playerGoldenBoostTimer;
    private PauseTransition villainGoldenBoostTimer;
    private PauseTransition villain2GoldenBoostTimer;
    private PauseTransition villain3GoldenBoostTimer;
    private enum SkinType { CLASSIC, NEON, RAINBOW, OCEAN, LIME, COSMIC }
    private static final String PREFS_NODE = "SnakeGame";
    private static final String PREF_LAST_SKIN = "lastSkin";
    private SkinType currentSkin = SkinType.CLASSIC;
    private final double RAINBOW_HUE_SPEED = 60;
    // Reusable materials for animated skins (avoids allocating a new PhongMaterial every frame)
    private final List<PhongMaterial> rainbowMats = new ArrayList<>();
    private final List<PhongMaterial> cosmicMats = new ArrayList<>();
    private final List<Color> COSMIC_PALETTE = List.of(
            Color.DARKVIOLET, Color.ROYALBLUE, Color.MEDIUMVIOLETRED, Color.WHITE
    );
    private final List<Color> NEON_PALETTE = List.of(
            Color.BLACK, Color.BROWN, Color.DARKBLUE, Color.DARKRED
    );
    private final List<Color> RAINBOW_PALETTE = List.of(
            Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.BLUE, Color.INDIGO, Color.VIOLET
    );
    private final List<Color> OCEAN_PALETTE = List.of(
            Color.DEEPSKYBLUE, Color.ROYALBLUE, Color.DARKSLATEBLUE,
            Color.LIGHTSKYBLUE, Color.MEDIUMBLUE, Color.CORNFLOWERBLUE
    );
    private final List<Color> VILLAIN_PALETTE = List.of(
            Color.DARKGREY, Color.GREY, Color.BLACK, Color.MAROON
    );
    private final List<Color> VILLAIN2_PALETTE = List.of(
            Color.RED, Color.CORAL, Color.FIREBRICK, Color.DEEPSKYBLUE
    );
    private final List<Color> VILLAIN3_PALETTE = List.of(
            Color.PERU, Color.BURLYWOOD, Color.CHOCOLATE, Color.CORNFLOWERBLUE
    );
    private final PhongMaterial classicMat = new PhongMaterial(Color.web("#2ECC71")); // fill
    private final PhongMaterial classicHeadMat = new PhongMaterial(Color.web("#2ECC71"));
    private final PhongMaterial neonMat = new PhongMaterial(Color.web("#111111"));
    private final PhongMaterial neonHeadMat = new PhongMaterial(Color.web("#111111"));
    private final javafx.scene.effect.Glow neonGlow = new javafx.scene.effect.Glow(0.9);
    private final PhongMaterial oceanMat       = new PhongMaterial(Color.LIGHTBLUE);
    private final PhongMaterial oceanHeadMat   = new PhongMaterial(Color.ROYALBLUE);
    private final PhongMaterial limeMat        = new PhongMaterial(Color.YELLOWGREEN);
    private final PhongMaterial limeHeadMat    = new PhongMaterial(Color.LIMEGREEN);
    private Button homeButton;
    private int highScore = 0;
    private Scene sceneRef;
    private Group headGroup;
    private AudioClip clickSound;
    private Button stopStartButton;
    //=======================================================  spawnSpecialItem  =======================================
    private void spawnSpecialItem(Group gameElements) {
        if (specialItemVisible) return;
        List<Sphere> candidates = new ArrayList<>(specialItems);
        candidates.remove(lastSpecialItem);
        if (candidates.isEmpty()) {
            candidates = new ArrayList<>(specialItems);
        }
        Sphere special = candidates.get(random.nextInt(candidates.size()));
        double minX = HEAD_RADIUS;
        double maxX = windowWidth - HEAD_RADIUS;
        double minY = topLine.getStartY() + HEAD_RADIUS;
        double maxY = bottomLine.getStartY() - HEAD_RADIUS - LINE_MARGIN;
        double x = minX + random.nextDouble() * (maxX - minX);
        double y = minY + random.nextDouble() * (maxY - minY);
        special.setTranslateX(x);
        special.setTranslateY(y);
        gameElements.getChildren().removeAll(specialItems);
        gameElements.getChildren().add(special);
        specialItemVisible = true;
        lastSpecialItem = special;
        specialDespawnTimer.playFromStart();
    }
    //=======================================================  spawnSparks  ============================================
    private void spawnSparks(Button btn) {
        Random rand = new Random();
        int sparkCount = 12;
        for (int i = 0; i < sparkCount; i++) {
            Circle spark = new Circle(2.5, Color.hsb(rand.nextInt(360), 1, 1));
            spark.setManaged(false);
            ((Pane) btn.getParent()).getChildren().add(spark);
            double centerX = btn.getLayoutX() + btn.getWidth() / 2;
            double centerY = btn.getLayoutY() + btn.getHeight() / 2;
            spark.setLayoutX(centerX);
            spark.setLayoutY(centerY);
            double angle = rand.nextDouble() * 2 * Math.PI;
            double distance = 30 + rand.nextDouble() * 25;
            double dx = Math.cos(angle) * distance;
            double dy = Math.sin(angle) * distance;
            TranslateTransition move = new TranslateTransition(Duration.millis(600), spark);
            move.setByX(dx);
            move.setByY(dy);
            FadeTransition fade = new FadeTransition(Duration.millis(600), spark);
            fade.setFromValue(1.0);
            fade.setToValue(0.0);
            ParallelTransition anim = new ParallelTransition(move, fade);
            anim.setOnFinished(e -> ((Pane) btn.getParent()).getChildren().remove(spark));
            anim.play();
        }
    }

    // ============================================   buildSkinSelectorOverlay   =======================================
    private Pane buildSkinSelectorOverlay(Runnable onStart) {
        loadLastSkin();
        var overlay = new StackPane();
        overlay.setPickOnBounds(true);
        overlay.setId("skin-overlay");
        final List<javafx.animation.Animation> overlayAnims = new ArrayList<>();
        final List<MediaPlayer> overlayMediaPlayers = new ArrayList<>();
        AudioClip clickSound = new AudioClip(getClass().getResource("/s1.wav").toExternalForm());
        AudioClip gameStartSound = new AudioClip( getClass().getResource("/gamestart.wav").toExternalForm());
        String videoPath = getClass().getResource("/bgvideo.mp4").toExternalForm();
        Media media = new Media(videoPath);
        MediaPlayer mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
        mediaPlayer.setAutoPlay(true);
        overlayMediaPlayers.add(mediaPlayer);
        MediaView videoView = new MediaView(mediaPlayer);
        videoView.setPreserveRatio(false);
        videoView.fitWidthProperty().bind(overlay.widthProperty());
        videoView.fitHeightProperty().bind(overlay.heightProperty());
        overlay.getChildren().add(0, videoView);
        var title = new Label("Snake Game");
        title.setStyle("-fx-text-fill: red; -fx-font-size: 80px; -fx-font-weight: bold;");
        VBox.setMargin(title, new Insets(-50, 0, 100, 0));
        ToggleGroup group = new ToggleGroup();
        var optClassic = new RadioButton("Classic");
        var optNeon    = new RadioButton("Neon");
        var optRainbow = new RadioButton("Rainbow");
        var optOcean = new RadioButton("Ocean");
        var optLime  = new RadioButton("Lime");
        var optCosmic = new RadioButton("Cosmic");
        for (RadioButton rb : new RadioButton[]{optClassic, optNeon, optRainbow, optOcean, optLime, optCosmic}) {
            rb.setPrefSize(160, 140);
            rb.setContentDisplay(ContentDisplay.CENTER);
            rb.setStyle(
                    "-fx-font-size: 26px;" +
                            "-fx-text-fill: white;" +
                            "-fx-background-color: transparent;" +
                            "-fx-padding: 0;"
            );
        }
        HBox skinsRow = new HBox(60, optClassic, optNeon, optRainbow, optOcean, optLime, optCosmic);
        skinsRow.setAlignment(Pos.CENTER);
        optClassic.setTextFill(Color.WHITE);
        optNeon.setTextFill(Color.WHITE);
        optRainbow.setTextFill(Color.WHITE);
        optOcean.setTextFill(Color.WHITE);
        optLime.setTextFill(Color.WHITE);
        optCosmic.setTextFill(Color.WHITE);
        optClassic.setToggleGroup(group);
        optNeon.setToggleGroup(group);
        optRainbow.setToggleGroup(group);
        optOcean.setToggleGroup(group);
        optLime.setToggleGroup(group);
        optCosmic.setToggleGroup(group);
        switch (currentSkin) {
            case CLASSIC -> group.selectToggle(optClassic);
            case NEON    -> group.selectToggle(optNeon);
            case RAINBOW -> group.selectToggle(optRainbow);
            case OCEAN   -> group.selectToggle(optOcean);
            case LIME    -> group.selectToggle(optLime);
            case COSMIC -> group.selectToggle(optCosmic);
        }
        HBox preview = new HBox(32);
        preview.setAlignment(Pos.CENTER);
        List<Circle> previewSegs = java.util.stream.IntStream.range(0, 7)
                .mapToObj(i -> new Circle(20, Color.web("#2ECC71")))
                .toList();
        preview.getChildren().addAll(previewSegs);
        group.selectedToggleProperty().addListener((o, oldT, newT) -> {
            clickSound.play();
            SkinType sel = newT == optClassic ? SkinType.CLASSIC
                    : (newT == optNeon ? SkinType.NEON
                    : (newT == optRainbow ? SkinType.RAINBOW
                    : (newT == optOcean ? SkinType.OCEAN
                    : (newT == optLime ? SkinType.LIME
                    : SkinType.COSMIC))));
            switch (sel) {
                case CLASSIC -> previewSegs.forEach(c -> c.setFill(Color.web("#2ECC71")));
                case NEON    -> previewSegs.forEach(c -> c.setFill(Color.web("#00FFFF")));
                case RAINBOW -> {
                    for (int i = 0; i < previewSegs.size(); i++) {
                        previewSegs.get(i).setFill(Color.hsb((i * 60) % 360, 1, 1));
                    }
                }
                case COSMIC -> {
                    for (int i = 0; i < previewSegs.size(); i++) {
                        Color c = switch (i % 4) {
                            case 0 -> Color.DARKVIOLET;
                            case 1 -> Color.ROYALBLUE;
                            case 2 -> Color.MEDIUMVIOLETRED;
                            default -> Color.WHITE; // sparkle
                        };
                        previewSegs.get(i).setFill(c);
                    }
                }
                case OCEAN   -> previewSegs.forEach(c -> c.setFill(Color.ROYALBLUE));
                case LIME    -> previewSegs.forEach(c -> c.setFill(Color.LIMEGREEN));
            }
        });
        Button play = new Button("Play");
        play.setPrefSize(100, 100);
        play.setStyle("-fx-background-radius: 70; -fx-font-size: 25px; -fx-font-weight: bold; -fx-background-color: #0000FF; -fx-text-fill: white;");
        play.setOnMouseEntered(e -> play.setStyle("-fx-background-radius: 70; -fx-font-size: 25px; -fx-font-weight: bold; -fx-background-color: #27ae60; -fx-text-fill: black;"));
        play.setOnMouseExited(e -> play.setStyle("-fx-background-radius: 70; -fx-font-size: 25px; -fx-font-weight: bold; -fx-background-color: #0000FF; -fx-text-fill: white;"));
        play.setOnAction(e -> {
            if (gameStartSound.isPlaying()) {
                gameStartSound.stop();
            }
            clickSound.play();
            spawnSparks(play);
            PauseTransition delay = new PauseTransition(Duration.seconds(1));
            delay.setOnFinished(ev -> {
                resetSnake();
                running = true;
                resumeGoldenFruitTimers();
                Toggle t = group.getSelectedToggle();
                currentSkin = (t == optClassic ? SkinType.CLASSIC
                        : (t == optNeon ? SkinType.NEON
                        : (t == optRainbow ? SkinType.RAINBOW
                        : (t == optOcean ? SkinType.OCEAN
                        : (t == optLime ? SkinType.LIME
                        : SkinType.COSMIC)))));
                saveLastSkin();
                onStart.run();
                ((Pane) overlay.getParent()).getChildren().remove(overlay);
            });
            delay.play();
        });
        overlay.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.LEFT || e.getCode() == KeyCode.A) {
                if (group.getSelectedToggle() == optLime) group.selectToggle(optOcean);
                else if (group.getSelectedToggle() == optOcean) group.selectToggle(optRainbow);
                else if (group.getSelectedToggle() == optRainbow) group.selectToggle(optNeon);
                else if (group.getSelectedToggle() == optNeon) group.selectToggle(optClassic);
            } else if (e.getCode() == KeyCode.RIGHT || e.getCode() == KeyCode.D) {
                if (group.getSelectedToggle() == optClassic) group.selectToggle(optNeon);
                else if (group.getSelectedToggle() == optNeon) group.selectToggle(optRainbow);
                else if (group.getSelectedToggle() == optRainbow) group.selectToggle(optOcean);
                else if (group.getSelectedToggle() == optOcean) group.selectToggle(optLime);
            } else if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) {
                play.fire();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                Platform.exit();
            }
        });
        Group starfield = new Group();
        for (int i = 0; i < 80; i++) {
            Circle star = new Circle(Math.random() * 1.5 + 0.5, Color.AQUA);
            star.setOpacity(Math.random() * 0.8);
            star.setTranslateX((Math.random() - 0.5) * 400);
            star.setTranslateY((Math.random() - 0.5) * 250);
            starfield.getChildren().add(star);
            FadeTransition twinkle = new FadeTransition(Duration.seconds(2 + Math.random() * 2), star);
            twinkle.setFromValue(0.2);
            twinkle.setToValue(1.0);
            twinkle.setCycleCount(Animation.INDEFINITE);
            twinkle.setAutoReverse(true);
            twinkle.play();
        }
        Group sparksGroup = new Group();
        int numSparks = 20;
        double radiusX = 160;
        double radiusY = 100;
        for (int i = 0; i < numSparks; i++) {
            Circle spark = new Circle(2, Color.AQUA);
            sparksGroup.getChildren().add(spark);
            Timeline orbit = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(spark.translateXProperty(), radiusX * Math.cos(2 * Math.PI * i / numSparks)),
                            new KeyValue(spark.translateYProperty(), radiusY * Math.sin(2 * Math.PI * i / numSparks)),
                            new KeyValue(spark.opacityProperty(), 0)
                    ),
                    new KeyFrame(Duration.seconds(0.5),
                            new KeyValue(spark.opacityProperty(), 1)
                    ),
                    new KeyFrame(Duration.seconds(3),
                            new KeyValue(spark.translateXProperty(), radiusX * Math.cos(2 * Math.PI * i / numSparks + 2 * Math.PI)),
                            new KeyValue(spark.translateYProperty(), radiusY * Math.sin(2 * Math.PI * i / numSparks + 2 * Math.PI)),
                            new KeyValue(spark.opacityProperty(), 0)
                    )
            );
            orbit.setCycleCount(Animation.INDEFINITE);
            orbit.play();
            overlayAnims.add(orbit);
        }
        Button backToGame = new Button("Back to Game");
        backToGame.setPrefSize(140, 50);
        backToGame.setFont(Font.font(18));
        final String backToGameBase =
                "-fx-background-radius: 15;" +
                        "-fx-background-insets: 0;" +
                        "-fx-font-size: 16px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: white;";
        backToGame.setStyle(backToGameBase + "-fx-background-color: #8B008B;");
        backToGame.setOnMouseEntered(e -> backToGame.setStyle(
                backToGameBase + "-fx-background-color: #9400D3;"));
        backToGame.setOnMouseExited(e -> backToGame.setStyle(
                backToGameBase + "-fx-background-color: #8B008B;"));
        backToGame.setOnMousePressed(e -> backToGame.setStyle(
                backToGameBase + "-fx-background-color: #4B0082;"));
        backToGame.setOnMouseReleased(e -> backToGame.setStyle(
                backToGameBase + (backToGame.isHover()
                        ? "-fx-background-color: #9400D3;"
                        : "-fx-background-color: #8B008B;")));
        backToGame.setOnAction(ev -> {
            clickSound.play();
            spawnSparks(backToGame);
            if (gameStartSound.isPlaying()) {
                gameStartSound.stop();
            }
            PauseTransition delay = new PauseTransition(Duration.seconds(1));
            delay.setOnFinished(e -> {
                running = true;
                timer.start();
                resumeGoldenFruitTimers();
                if (overlay.getParent() instanceof Pane p) {
                    p.getChildren().remove(overlay);
                }
            });
            delay.play();
        });
        VBox card = new VBox(40, title, skinsRow, preview, play);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(24));
        card.setStyle("-fx-background-color: rgba(20,20,20,0.9); -fx-background-radius: 16; -fx-padding: 24;");
        DropShadow glow = new DropShadow();
        glow.setColor(Color.CYAN);
        glow.setRadius(30);
        glow.setSpread(0.3);
        card.setEffect(glow);
        card.setOpacity(0);
        card.setScaleX(0.3);
        card.setScaleY(0.3);
        card.setRotate(-15);
        Timeline entrance = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(card.opacityProperty(), 0),
                        new KeyValue(card.scaleXProperty(), 0.3),
                        new KeyValue(card.scaleYProperty(), 0.3),
                        new KeyValue(card.rotateProperty(), -15)
                ),
                new KeyFrame(Duration.seconds(1),
                        new KeyValue(card.opacityProperty(), 1),
                        new KeyValue(card.scaleXProperty(), 1.05),
                        new KeyValue(card.scaleYProperty(), 1.05),
                        new KeyValue(card.rotateProperty(), 5)
                ),
                new KeyFrame(Duration.seconds(1.2),
                        new KeyValue(card.scaleXProperty(), 1),
                        new KeyValue(card.scaleYProperty(), 1),
                        new KeyValue(card.rotateProperty(), 0)
                )
        );
        gameStartSound.play();
        entrance.play();
        StackPane cardContainer = new StackPane(starfield, sparksGroup, card);
        overlay.getChildren().add(cardContainer);
        StackPane.setAlignment(cardContainer, Pos.CENTER);
        StackPane.setAlignment(backToGame, Pos.BOTTOM_LEFT);
        StackPane.setMargin(backToGame, new Insets(0, 0, 30, 30));
        overlay.getChildren().add(backToGame);
        overlay.sceneProperty().addListener((o, ov, nv) -> { if (nv != null) overlay.requestFocus(); });
        Button exitButton = new Button("Exit");
        exitButton.setPrefSize(140, 50); // wider rectangle
        final String exitBase =
                "-fx-background-radius: 15;" +
                        "-fx-background-insets: 0;" +
                        "-fx-font-size: 22px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: white;";
        exitButton.setStyle(exitBase + "-fx-background-color: #e74c3c;");
        exitButton.setOnMouseEntered(e -> exitButton.setStyle(
                exitBase + "-fx-background-color: #c0392b;"));
        exitButton.setOnMouseExited(e -> exitButton.setStyle(
                exitBase + "-fx-background-color: #e74c3c;"));
        exitButton.setOnMousePressed(e -> exitButton.setStyle(
                exitBase + "-fx-background-color: #a93226;"));
        exitButton.setOnMouseReleased(e -> exitButton.setStyle(
                exitBase + (exitButton.isHover()
                        ? "-fx-background-color: #c0392b;"
                        : "-fx-background-color: #e74c3c;")));
        exitButton.setOnAction(e -> {
            clickSound.play();
            spawnSparks(exitButton);
            PauseTransition delay = new PauseTransition(Duration.seconds(1));
            delay.setOnFinished(ev -> Platform.exit());
            delay.play();
        });
        StackPane.setAlignment(exitButton, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(exitButton, new Insets(0, 30, 30, 0));
        overlay.getChildren().add(exitButton);
        card.setStyle(
                "-fx-background-color: transparent;" + // solid teal
                        "-fx-background-radius: 16;" +
                        "-fx-padding: 24;"
        );
        overlay.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                for (javafx.animation.Animation a : overlayAnims) {
                    try { a.stop(); } catch (Throwable ignore) {}
                }
                for (MediaPlayer mp : overlayMediaPlayers) {
                    try { mp.stop(); mp.dispose(); } catch (Throwable ignore) {}
                }
                overlayAnims.clear();
                overlayMediaPlayers.clear();
            }
        });
        return overlay;
    }
    //==========================================================   start method   ======================================
    @Override
    public void start(Stage stage) {
        startTime = System.nanoTime();
        Canvas spaceCanvas = new Canvas(windowWidth, windowHeight);
        GraphicsContext gc = spaceCanvas.getGraphicsContext2D();
        final int numStars = 200;
        double[] x = new double[numStars];
        double[] y = new double[numStars];
        double[] starSpeed = new double[numStars];
        for (int i = 0; i < numStars; i++) {
            x[i] = Math.random() * windowWidth;
            y[i] = Math.random() * windowHeight;
            starSpeed[i] = 0.3 + Math.random() * 1.7;
        }
        eatSound = new AudioClip(getClass().getResource("/eat.wav").toExternalForm());
        Media gameOverMedia = new Media(getClass().getResource("/gameover.wav").toExternalForm());
        gameOverSound = new MediaPlayer(gameOverMedia);
        clickSound = new AudioClip(getClass().getResource("/s2.wav").toExternalForm());
        topLine = new Line(0, 50, windowWidth, 50);
        topLine.setStroke(Color.WHITE);
        topLine.setStrokeWidth(2);
        bottomLine = new Line(0, windowHeight - BOTTOM_OFFSET, windowWidth, windowHeight - BOTTOM_OFFSET);
        bottomLine.setStroke(Color.WHITE);
        bottomLine.setStrokeWidth(2);
        PhongMaterial headMaterial = new PhongMaterial(Color.BLACK);
        headMaterial.setSpecularColor(Color.PINK);
        head = new Sphere(HEAD_RADIUS);
        head.setMaterial(headMaterial);
        head.setTranslateZ(0);
        Sphere leftEye = new Sphere(6);
        leftEye.setMaterial(new PhongMaterial(Color.WHITE));
        leftEye.setTranslateX(-10);
        leftEye.setTranslateY(-10);
        leftEye.setTranslateZ(-HEAD_RADIUS);
        Sphere rightEye = new Sphere(6);
        rightEye.setMaterial(new PhongMaterial(Color.WHITE));
        rightEye.setTranslateX(10);
        rightEye.setTranslateY(-10);
        rightEye.setTranslateZ(-HEAD_RADIUS);
        Cylinder leftPupil = new Cylinder(1.5, 8);
        leftPupil.setMaterial(new PhongMaterial(Color.BLACK));
        leftPupil.setRotate(90);
        leftPupil.setTranslateX(-10);
        leftPupil.setTranslateY(-10);
        leftPupil.setTranslateZ(-HEAD_RADIUS - 3);
        Cylinder rightPupil = new Cylinder(1.5, 8);
        rightPupil.setMaterial(new PhongMaterial(Color.BLACK));
        rightPupil.setRotate(90);
        rightPupil.setTranslateX(10);
        rightPupil.setTranslateY(-10);
        rightPupil.setTranslateZ(-HEAD_RADIUS - 3);
        headGroup = new Group(head, leftEye, rightEye, leftPupil, rightPupil);
        PhongMaterial bodyMaterial = new PhongMaterial(Color.CHOCOLATE);
        bodyMaterial.setSpecularColor(Color.CHOCOLATE);
        for (int i = 0; i < 3; i++) {
            Sphere segment = new Sphere(BODY_RADIUS);
            segment.setMaterial(bodyMaterial);
            segment.setTranslateX(headX - (i + 1) * SEGMENT_SPACING);
            segment.setTranslateY(headY);
            segment.setTranslateZ(0);
            bodyParts.add(segment);
        }
        fruits.add(makeTexturedSphere("/images/apple.png"));
        fruits.add(makeTexturedSphere("/images/orange.png"));
        fruits.add(makeTexturedSphere("/images/pumpkin.png"));
        specialItems.add(makeTexturedSphere("/images/eagle.png"));
        specialItems.add(makeTexturedSphere("/images/rodent.png"));
        specialItems.add(makeTexturedSphere("/images/mongoose.png"));
        {
            classicMat.setSpecularColor(Color.web("#1E8449"));
            classicHeadMat.setSpecularColor(Color.web("#1E8449"));
            neonMat.setSpecularColor(Color.web("#00FFFF"));
            neonHeadMat.setSpecularColor(Color.web("#00FFFF"));
            oceanMat.setSpecularColor(Color.LIGHTSKYBLUE);
            oceanHeadMat.setSpecularColor(Color.DEEPSKYBLUE);
            limeMat.setSpecularColor(Color.web("#9ACD32"));   // YellowGreen-like shine
            limeHeadMat.setSpecularColor(Color.web("#32CD32")); // LimeGreen shine
        }
        specialDespawnTimer = new PauseTransition(Duration.seconds(3));
        specialDespawnTimer.setOnFinished(ev -> {
            if (specialItemVisible) {
                Group gameElements = (Group) head.getParent().getParent();
                gameElements.getChildren().removeAll(specialItems);
                specialItemVisible = false;
            }
        });
        Group gameElements = new Group(headGroup, topLine, bottomLine);
        gameElements.getChildren().addAll(bodyParts);
        PhongMaterial villainHeadMaterial = new PhongMaterial(Color.DARKSLATEGREY); // Head
        villainHeadMaterial.setSpecularColor(Color.ORANGE);
        villainHead = new Sphere(HEAD_RADIUS);
        villainHead.setMaterial(villainHeadMaterial);
        villainHead.setTranslateX(villainX);
        villainHead.setTranslateY(villainY);
        PhongMaterial villainBodyMaterial = new PhongMaterial(Color.CORNSILK);
        villainHeadMaterial.setSpecularColor(Color.CORNSILK);
        for (int i = 0; i < 3; i++) {
            Sphere seg = new Sphere(BODY_RADIUS);
            seg.setMaterial(villainBodyMaterial);
            seg.setTranslateX(villainX - (i + 1) * SEGMENT_SPACING);
            seg.setTranslateY(villainY);
            villainBodyParts.add(seg);
        }
        PhongMaterial villain2HeadMaterial = new PhongMaterial(Color.MAROON);
        villain2HeadMaterial.setSpecularColor(Color.GREEN);
        villain2Head = new Sphere(HEAD_RADIUS);
        villain2Head.setMaterial(villain2HeadMaterial);
        villain2Head.setTranslateX(villain2X);
        villain2Head.setTranslateY(villain2Y);
        PhongMaterial villain2BodyMaterial = new PhongMaterial(Color.TOMATO);
        villain2HeadMaterial.setSpecularColor(Color.TOMATO);
        for (int i = 0; i < 3; i++) {
            Sphere seg = new Sphere(BODY_RADIUS);
            seg.setMaterial(villain2BodyMaterial);
            seg.setTranslateX(villain2X - (i + 1) * SEGMENT_SPACING);
            seg.setTranslateY(villain2Y);
            villain2BodyParts.add(seg);
        }
        PhongMaterial villain3HeadMaterial = new PhongMaterial(Color.SADDLEBROWN);
        villain3HeadMaterial.setSpecularColor(Color.BLUE);
        villain3Head = new Sphere(HEAD_RADIUS);
        villain3Head.setMaterial(villain3HeadMaterial);
        villain3Head.setTranslateX(villain3X);
        villain3Head.setTranslateY(villain3Y);
        PhongMaterial villain3BodyMaterial = new PhongMaterial(Color.ROSYBROWN);
        villain3HeadMaterial.setSpecularColor(Color.ROSYBROWN);
        for (int i = 0; i < 3; i++) {
            Sphere seg = new Sphere(BODY_RADIUS);
            seg.setMaterial(villain3BodyMaterial);
            seg.setTranslateX(villain3X - (i + 1) * SEGMENT_SPACING);
            seg.setTranslateY(villain3Y);
            villain3BodyParts.add(seg);
        }
        homeButton = new Button("Home page");
        homeButton.setFont(Font.font(18));
        homeButton.setStyle("-fx-background-color: #FF69B4; -fx-text-fill: white;");
        homeButton.setPrefSize(120, 40);
        homeButton.setTranslateX(windowWidth - 125);
        homeButton.setTranslateY(4);
        homeButton.setOnAction(event -> {
            clickSound.play();
            running = false;
            timer.stop();
            StackPane root = (StackPane) sceneRef.getRoot();
            for (Iterator<Node> it = root.getChildren().iterator(); it.hasNext(); ) {
                Node n = it.next();
                if (n instanceof Pane && "skin-overlay".equals(n.getId())) {
                    it.remove();
                }
            }
            Pane overlay = buildSkinSelectorOverlay(() -> {
                applySkinToPlayerSnake();
                running = true;
                timer.start();
            });
            root.getChildren().add(overlay);
        });
        gameElements.getChildren().add(homeButton);
        gameElements.getChildren().addAll(villainHead);
        gameElements.getChildren().addAll(villainBodyParts);
        gameElements.getChildren().addAll(villain2Head);
        gameElements.getChildren().addAll(villain2BodyParts);
        gameElements.getChildren().addAll(villain3Head);
        gameElements.getChildren().addAll(villain3BodyParts);
        scoreText = new Text("Score: 0");
        scoreText.setFill(Color.web("#FFF8DC"));
        scoreText.setFont(Font.font(24));
        scoreText.setTranslateX(10);
        scoreText.setTranslateY(30);
        gameElements.getChildren().add(scoreText);
        Pane buttonPane = new Pane();
        buttonPane.setPrefHeight(50);
        buttonPane.setStyle("-fx-background-color: #1E90FF;");
        Button restartButton = new Button("Restart");
        restartButton.setPrefSize(100, 40);
        restartButton.setFont(Font.font(18));
        restartButton.setStyle("-fx-background-color: #006400; -fx-text-fill: white;");
        stopStartButton = new Button("Stop");
        stopStartButton.setPrefSize(100, 40);
        stopStartButton.setFont(Font.font(18));
        stopStartButton.setStyle("-fx-background-color: #FFD700; -fx-text-fill: black;");
        Button exitButton = new Button("Exit");
        exitButton.setPrefSize(100, 40);
        exitButton.setFont(Font.font(18));
        exitButton.setStyle("-fx-background-color: #800000; -fx-text-fill: MINTCREAM;");
        buttonPane.getChildren().addAll(restartButton, stopStartButton, exitButton);
        gameElements.getChildren().add(buttonPane);
        Runnable updateButtonPositions = () -> {
            buttonPane.setPrefWidth(windowWidth);
            restartButton.setLayoutX(10);
            stopStartButton.setLayoutX((windowWidth - stopStartButton.getWidth()) / 2);
            exitButton.setLayoutX(windowWidth - exitButton.getWidth() - 10);
            buttonPane.setTranslateY(windowHeight - 50);
        };
        Group content = new Group(spaceCanvas, gameElements);
        content.setManaged(false);
        StackPane layeredRoot = new StackPane(content);
        StackPane.setAlignment(content, Pos.TOP_LEFT);
        Scene scene = new Scene(layeredRoot, windowWidth, windowHeight, false);
        this.sceneRef = scene;
        running = false;
        Pane overlay = buildSkinSelectorOverlay(() -> {
            applySkinToPlayerSnake();
            running = true;
        });
        layeredRoot.getChildren().add(overlay);
        scene.widthProperty().addListener((obs, oldVal, newVal) -> {
            windowWidth = newVal.doubleValue();
            topLine.setEndX(windowWidth);
            bottomLine.setEndX(windowWidth);
            spaceCanvas.setWidth(windowWidth);
            updateButtonPositions.run();
            double maxX = windowWidth - HEAD_RADIUS;
            for (Sphere f : fruits) {
                if (gameElements.getChildren().contains(f) && f.getTranslateX() > maxX) {
                    f.setTranslateX(maxX);
                }
            }
            for (Sphere s : specialItems) {
                if (gameElements.getChildren().contains(s) && s.getTranslateX() > maxX) {
                    s.setTranslateX(maxX);
                }
            }
            if (goldenFruitVisible && goldenFruit.getTranslateX() > maxX) {
                goldenFruit.setTranslateX(maxX);
            }
        });
        scene.heightProperty().addListener((obs, oldVal, newVal) -> {
            windowHeight = newVal.doubleValue();
            bottomLine.setStartY(windowHeight - BOTTOM_OFFSET + LINE_MARGIN);
            bottomLine.setEndY(windowHeight - BOTTOM_OFFSET + LINE_MARGIN);
            spaceCanvas.setHeight(windowHeight);
            updateButtonPositions.run();
            double maxY = bottomLine.getStartY() - HEAD_RADIUS - LINE_MARGIN;
            for (Sphere f : fruits) {
                if (gameElements.getChildren().contains(f) && f.getTranslateY() > maxY) {
                    f.setTranslateY(maxY);
                }
            }
            for (Sphere s : specialItems) {
                if (gameElements.getChildren().contains(s) && s.getTranslateY() > maxY) {
                    s.setTranslateY(maxY);
                }
            }
            if (goldenFruitVisible && goldenFruit.getTranslateY() > maxY) {
                goldenFruit.setTranslateY(maxY);
            }
        });
        scene.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case W, UP -> direction = 3;    // Up
                case S, DOWN -> direction = 1;  // Down
                case A, LEFT -> direction = 2;  // Left
                case D, RIGHT -> direction = 0; // Right
            }
        });
        respawnRandomFruit(gameElements);
        setupGoldenFruit(gameElements);
        //========================================================  Animation Timer  ===================================
        // Game loop
        timer = new AnimationTimer() {
            private boolean waitingToRespawn = false;
            @Override
            public void handle(long now) {
                Stop[] stops = new Stop[] { new Stop(0, Color.BLACK), new Stop(1, Color.DARKBLUE) };
                LinearGradient lg = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, stops);
                gc.setFill(lg);
                gc.fillRect(0, 0, windowWidth, windowHeight);
                gc.setFill(Color.WHITE);
                for (int i = 0; i < numStars; i++) {
                    double size = 1 + Math.random() * 2; // twinkle
                    gc.fillOval(x[i], y[i], size, size);
                    y[i] += starSpeed[i];
                    if (y[i] > windowHeight) {
                        y[i] = 0;
                        x[i] = Math.random() * windowWidth;
                    }
                }
                if (Math.random() < 0.002) {
                    gc.setStroke(Color.LIGHTCYAN);
                    double sx = Math.random() * windowWidth;
                    double sy = Math.random() * windowHeight / 2;
                    gc.strokeLine(sx, sy, sx + 50, sy + 15);
                }
                if (!running) return;
                updateVillainAI(gameElements);
                spawnVillainTrail(gameElements);
                spawnVillainHeadGlow(gameElements, Color.MAROON);
                if (!running) return;
                updateVillain2AI(gameElements);
                spawnVillain2Trail(gameElements);
                spawnVillain2HeadGlow(gameElements, Color.BLUE);
                if (!running) return;
                updateVillain3AI(gameElements);
                spawnVillain3Trail(gameElements);
                spawnVillain3HeadGlow(gameElements, Color.LIGHTYELLOW);
                double elapsedSeconds = (now - startTime) / 1_000_000_000.0;
                if (currentSkin == SkinType.RAINBOW) {
                    updateRainbowColors(elapsedSeconds);
                }
                if (currentSkin == SkinType.COSMIC) {
                    spawnTrailParticles(gameElements, COSMIC_PALETTE, 4, 25);
                    spawnHeadGlow(gameElements, Color.WHITE);

                } else if (currentSkin == SkinType.NEON) {
                    spawnTrailParticles(gameElements, NEON_PALETTE, 3, 20);
                    spawnHeadGlow(gameElements, Color.DARKRED);

                } else if (currentSkin == SkinType.RAINBOW) {
                    spawnTrailParticles(gameElements, RAINBOW_PALETTE, 5, 25);
                    spawnRainbowHeadGlow(gameElements, elapsedSeconds);

                } else if (currentSkin == SkinType.OCEAN) {
                    spawnTrailParticles(gameElements, OCEAN_PALETTE, 4, 22);
                    spawnHeadGlow(gameElements, Color.DEEPSKYBLUE);
                }
                // ===============================
                // PLAYER SNAKE MOVEMENT
                // ===============================
                switch (direction) {
                    case 0 -> headX += speed;
                    case 1 -> headY += speed;
                    case 2 -> headX -= speed;
                    case 3 -> headY -= speed;
                }
                if (headX > windowWidth) {
                    headX = 0;
                    if (!positionHistory.isEmpty()) positionHistory.removeLast(); // remove bad pre-wrap entry
                    positionHistory.addLast(new double[]{headX, headY});
                }
                if (headX < 0) {
                    headX = windowWidth;
                    if (!positionHistory.isEmpty()) positionHistory.removeLast();
                    positionHistory.addLast(new double[]{headX, headY});
                }
                double topY = topLine.getStartY() + HEAD_RADIUS;
                double bottomY = bottomLine.getStartY() - HEAD_RADIUS - LINE_MARGIN;
                if (headY > bottomY) {
                    headY = topY;
                    if (!positionHistory.isEmpty()) positionHistory.removeLast();
                    positionHistory.addLast(new double[]{headX, headY});
                }
                if (headY < topY) {
                    headY = bottomY;
                    if (!positionHistory.isEmpty()) positionHistory.removeLast();
                    positionHistory.addLast(new double[]{headX, headY});
                }
                headGroup.setTranslateX(headX);
                headGroup.setTranslateY(headY);
                double angle;
                switch (direction) {
                    case 0 -> angle = 0;
                    case 1 -> angle = 90;
                    case 2 -> angle = 180;
                    case 3 -> angle = 270;
                    default -> angle = 0;
                }
                headGroup.setRotate(angle);
                positionHistory.addLast(new double[]{headX, headY});
                int stepFrames = Math.max(1, (int)(SEGMENT_SPACING / speed));
                int maxHistorySize = (bodyParts.size() + 1) * stepFrames + 50;
                while (positionHistory.size() > maxHistorySize) {
                    positionHistory.pollFirst();
                }
                int index = 1;
                for (Sphere segment : bodyParts) {
                    double[] pos = posAtFromTail(positionHistory, index * stepFrames,
                            new double[]{segment.getTranslateX(), segment.getTranslateY()});
                    segment.setTranslateX(pos[0]);
                    segment.setTranslateY(pos[1]);
                    double bobbing = 3 * Math.sin(elapsedSeconds * 4 + index);
                    segment.setTranslateZ(bobbing);
                    segment.setRotationAxis(Rotate.Z_AXIS);
                    segment.setRotate(angle);
                    if (index == bodyParts.size()) { // last segment only
                        double rippleAmplitude = 5 + bodyParts.size() * 0.5; // grows with more segments
                        double rippleFrequency = 5;
                        segment.setTranslateX(pos[0] + rippleAmplitude * Math.sin(elapsedSeconds * rippleFrequency));
                    }
                    index++;
                }
                // ===============================
                // SPECIAL ITEM HANDLING (PLAYER)
                // ===============================
                if (specialItemVisible) {
                    for (Sphere special : specialItems) {
                        if (!gameElements.getChildren().contains(special)) continue;
                        double dx = headX - special.getTranslateX();
                        double dy = headY - special.getTranslateY();
                        double distance = Math.sqrt(dx * dx + dy * dy);
                        if (distance < HEAD_RADIUS + HEAD_RADIUS * 0.9) {
                            shrinkSnake(bodyParts, gameElements);
                            gameElements.getChildren().remove(special);
                            specialItemVisible = false;
                            break;
                        }
                    }
                } else {
                    if (random.nextDouble() < 0.005) {
                        spawnSpecialItem(gameElements);
                    }
                }
                // ===============================
                // PLAYER FRUIT COLLISION
                // ===============================
                for (Sphere fruit : fruits) {
                    if (!gameElements.getChildren().contains(fruit)) continue;
                    double dx = headX - fruit.getTranslateX();
                    double dy = headY - fruit.getTranslateY();
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    if (distance < HEAD_RADIUS + HEAD_RADIUS * 0.9) {
                        score++;
                        scoreText.setText("Score: " + score);
                        playerFruitCount++;
                        if (playerFruitCount % 10 == 0) {
                            speed += SPEED_INCREMENT;
                            System.out.println("Player speed increased to: " + speed);
                        }
                        eatSound.play();
                        Sphere newSegment = createPlayerBodySegmentLikeTail();
                        Sphere lastSegment = bodyParts.get(bodyParts.size() - 1);
                        newSegment.setTranslateX(lastSegment.getTranslateX());
                        newSegment.setTranslateY(lastSegment.getTranslateY());
                        newSegment.setTranslateZ(lastSegment.getTranslateZ());
                        bodyParts.add(newSegment);
                        ((Group) head.getParent().getParent()).getChildren().add(newSegment);
                        gameElements.getChildren().remove(fruit);
                        PauseTransition pt = new PauseTransition(Duration.seconds(0.2));
                        pt.setOnFinished(ev -> {
                            respawnSingleFruit(gameElements, fruit);
                        });
                        pt.play();
                        break;
                    }
                }
                // ===============================
                // VILLAIN1 FRUIT COLLISION  (REPLACEMENT)
                // ===============================
                for (Sphere fruit : fruits) {
                    if (!gameElements.getChildren().contains(fruit)) continue;
                    double dx = villainX - fruit.getTranslateX();
                    double dy = villainY - fruit.getTranslateY();
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    if (distance < HEAD_RADIUS + HEAD_RADIUS * 0.9) {
                        villainFruitCount++;
                        if (villainFruitCount % 10 == 0) {
                            villainSpeed += SPEED_INCREMENT;
                            System.out.println("Villain speed increased to: " + villainSpeed);
                        }
                        eatSound.play();
                        PhongMaterial bodyMaterial = new PhongMaterial(Color.CORNSILK);
                        Sphere newSegment = new Sphere(BODY_RADIUS);
                        newSegment.setMaterial(bodyMaterial);
                        Sphere lastSeg = villainBodyParts.get(villainBodyParts.size() - 1);
                        newSegment.setTranslateX(lastSeg.getTranslateX());
                        newSegment.setTranslateY(lastSeg.getTranslateY());
                        newSegment.setTranslateZ(lastSeg.getTranslateZ());
                        villainBodyParts.add(newSegment);
                        ((Group) head.getParent().getParent()).getChildren().add(newSegment);
                        gameElements.getChildren().remove(fruit);
                        PauseTransition pt = new PauseTransition(Duration.seconds(0.2));
                        pt.setOnFinished(ev -> {
                            respawnSingleFruit(gameElements, fruit);
                        });
                        pt.play();
                        break;
                    }
                }
                // ===============================
                // VILLAIN2 FRUIT COLLISION
                // ===============================
                for (Sphere fruit : fruits) {
                    if (!gameElements.getChildren().contains(fruit)) continue;
                    double dx = villain2X - fruit.getTranslateX();
                    double dy = villain2Y - fruit.getTranslateY();
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    if (distance < HEAD_RADIUS + HEAD_RADIUS * 0.9) {
                        villain2FruitCount++;
                        if (villain2FruitCount % 10 == 0) {
                            villain2Speed += SPEED_INCREMENT;
                            System.out.println("Villain 2 speed increased to: " + villain2Speed);
                        }
                        eatSound.play();
                        PhongMaterial bodyMaterial = new PhongMaterial(Color.TOMATO);
                        Sphere newSegment = new Sphere(BODY_RADIUS);
                        newSegment.setMaterial(bodyMaterial);
                        Sphere lastSeg = villain2BodyParts.get(villain2BodyParts.size() - 1);
                        newSegment.setTranslateX(lastSeg.getTranslateX());
                        newSegment.setTranslateY(lastSeg.getTranslateY());
                        newSegment.setTranslateZ(lastSeg.getTranslateZ());
                        villain2BodyParts.add(newSegment);
                        ((Group) head.getParent().getParent()).getChildren().add(newSegment);
                        gameElements.getChildren().remove(fruit);
                        PauseTransition pt = new PauseTransition(Duration.seconds(0.2));
                        pt.setOnFinished(ev -> {
                            respawnSingleFruit(gameElements, fruit);
                        });
                        pt.play();
                        break;
                    }
                }
                // ===============================
                // VILLAIN3 FRUIT COLLISION
                // ===============================
                for (Sphere fruit : fruits) {
                    if (!gameElements.getChildren().contains(fruit)) continue;
                    double dx = villain3X - fruit.getTranslateX();
                    double dy = villain3Y - fruit.getTranslateY();
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    if (distance < HEAD_RADIUS + HEAD_RADIUS * 0.9) {
                        villain3FruitCount++;
                        if (villain3FruitCount % 10 == 0) {
                            villain3Speed += SPEED_INCREMENT;
                            System.out.println("Villain 3 speed increased to: " + villain3Speed);
                        }
                        eatSound.play();
                        PhongMaterial bodyMaterial = new PhongMaterial(Color.ROSYBROWN);
                        Sphere newSegment = new Sphere(BODY_RADIUS);
                        newSegment.setMaterial(bodyMaterial);
                        Sphere lastSeg = villain3BodyParts.get(villain3BodyParts.size() - 1);
                        newSegment.setTranslateX(lastSeg.getTranslateX());
                        newSegment.setTranslateY(lastSeg.getTranslateY());
                        newSegment.setTranslateZ(lastSeg.getTranslateZ());
                        villain3BodyParts.add(newSegment);
                        ((Group) head.getParent().getParent()).getChildren().add(newSegment);
                        gameElements.getChildren().remove(fruit);
                        PauseTransition pt = new PauseTransition(Duration.seconds(0.2));
                        pt.setOnFinished(ev -> {
                            respawnSingleFruit(gameElements, fruit);
                        });
                        pt.play();
                        break;
                    }
                }
                // ===============================
                // VILLAIN1 SPECIAL ITEM HANDLING
                // ===============================
                if (specialItemVisible) {
                    for (Sphere special : specialItems) {
                        if (!gameElements.getChildren().contains(special)) continue;
                        double dx = villainX - special.getTranslateX();
                        double dy = villainY - special.getTranslateY();
                        double distance = Math.sqrt(dx * dx + dy * dy);
                        if (distance < HEAD_RADIUS + HEAD_RADIUS * 0.9) {
                            shrinkSnake(villainBodyParts, gameElements);
                            gameElements.getChildren().remove(special);
                            specialItemVisible = false;
                            break;
                        }
                    }
                }
                // ===============================
                // VILLAIN2 SPECIAL ITEM HANDLING
                // ===============================
                if (specialItemVisible) {
                    for (Sphere special : specialItems) {
                        if (!gameElements.getChildren().contains(special)) continue;
                        double dx = villain2X - special.getTranslateX();
                        double dy = villain2Y - special.getTranslateY();
                        double distance = Math.sqrt(dx * dx + dy * dy);
                        if (distance < HEAD_RADIUS + HEAD_RADIUS * 0.9) {
                            shrinkSnake(villain2BodyParts, gameElements);
                            gameElements.getChildren().remove(special);
                            specialItemVisible = false;
                            break;
                        }
                    }
                }
                // ===============================
                // VILLAIN3 SPECIAL ITEM HANDLING
                // ===============================
                if (specialItemVisible) {
                    for (Sphere special : specialItems) {
                        if (!gameElements.getChildren().contains(special)) continue;
                        double dx = villain3X - special.getTranslateX();
                        double dy = villain3Y - special.getTranslateY();
                        double distance = Math.sqrt(dx * dx + dy * dy);
                        if (distance < HEAD_RADIUS + HEAD_RADIUS * 0.9) {
                            shrinkSnake(villain3BodyParts, gameElements);
                            gameElements.getChildren().remove(special);
                            specialItemVisible = false;
                            break;
                        }
                    }
                }
                // ===============================
                // GOLDEN FRUIT COLLISION (Player)
                //===============================
                if (goldenFruitVisible) {
                    double dx = headX - goldenFruit.getTranslateX();
                    double dy = headY - goldenFruit.getTranslateY();
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    if (distance < HEAD_RADIUS + GOLDEN_FRUIT_RADIUS) {
                        previousPlayerSpeed = speed;
                        speed = Math.max(GOLDEN_FRUIT_SPEED, previousPlayerSpeed + 2.0);
                        if (playerGoldenBoostTimer != null) playerGoldenBoostTimer.stop();
                        playerGoldenBoostTimer = new PauseTransition(Duration.seconds(10));
                        playerGoldenBoostTimer.setOnFinished(e -> speed = previousPlayerSpeed);
                        playerGoldenBoostTimer.play();
                        gameElements.getChildren().remove(goldenFruit);
                        goldenFruitVisible = false;
                    }
                }

                // ===============================
                // GOLDEN FRUIT COLLISION (Villain1)
                // ===============================
                if (goldenFruitVisible) {
                    double dx = villainX - goldenFruit.getTranslateX();
                    double dy = villainY - goldenFruit.getTranslateY();
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    if (distance < HEAD_RADIUS + GOLDEN_FRUIT_RADIUS) {
                        previousVillainSpeed = villainSpeed;
                        villainSpeed = Math.max(GOLDEN_FRUIT_SPEED, previousVillainSpeed + 2.0);
                        if (villainGoldenBoostTimer != null) villainGoldenBoostTimer.stop();
                        villainGoldenBoostTimer = new PauseTransition(Duration.seconds(10));
                        villainGoldenBoostTimer.setOnFinished(e -> villainSpeed = previousVillainSpeed);
                        villainGoldenBoostTimer.play();
                        gameElements.getChildren().remove(goldenFruit);
                        goldenFruitVisible = false;
                    }
                }

                // ===============================
                // GOLDEN FRUIT COLLISION (Villain2)
                // ===============================
                if (goldenFruitVisible) {
                    double dx = villain2X - goldenFruit.getTranslateX();
                    double dy = villain2Y - goldenFruit.getTranslateY();
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    if (distance < HEAD_RADIUS + GOLDEN_FRUIT_RADIUS) {
                        previousVillain2Speed = villain2Speed;
                        villain2Speed = Math.max(GOLDEN_FRUIT_SPEED, previousVillain2Speed + 2.0);
                        if (villain2GoldenBoostTimer != null) villain2GoldenBoostTimer.stop();
                        villain2GoldenBoostTimer = new PauseTransition(Duration.seconds(10));
                        villain2GoldenBoostTimer.setOnFinished(e -> villain2Speed = previousVillain2Speed);
                        villain2GoldenBoostTimer.play();
                        gameElements.getChildren().remove(goldenFruit);
                        goldenFruitVisible = false;
                    }
                }
                // ===============================
                // GOLDEN FRUIT COLLISION (Villain3)
                // ===============================
                if (goldenFruitVisible) {
                    double dx = villain3X - goldenFruit.getTranslateX();
                    double dy = villain3Y - goldenFruit.getTranslateY();
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    if (distance < HEAD_RADIUS + GOLDEN_FRUIT_RADIUS) {
                        previousVillain3Speed = villain3Speed;
                        villain3Speed = Math.max(GOLDEN_FRUIT_SPEED, previousVillain3Speed + 2.0);
                        if (villain3GoldenBoostTimer != null) villain3GoldenBoostTimer.stop();
                        villain3GoldenBoostTimer = new PauseTransition(Duration.seconds(10));
                        villain3GoldenBoostTimer.setOnFinished(e -> villain3Speed = previousVillain3Speed);
                        villain3GoldenBoostTimer.play();
                        gameElements.getChildren().remove(goldenFruit);
                        goldenFruitVisible = false;
                    }
                }
                // ===============================
                // COLLISION DETECTION (Player ↔ Villain1)
                // ===============================
                java.util.function.BiFunction<double[], double[], Double> dist = (a, b) -> {
                    double dx = a[0] - b[0];
                    double dy = a[1] - b[1];
                    return Math.sqrt(dx * dx + dy * dy);
                };
                double pX = headX,     pY = headY;
                double vX = villainX,  vY = villainY;
                if (villainAlive && dist.apply(new double[]{pX, pY}, new double[]{vX, vY}) <= (HEAD_RADIUS + HEAD_RADIUS)) {
                    killVillain();
                    gameOver(stage);
                    return;
                }
                for (Sphere seg : villainBodyParts) {
                    if (dist.apply(new double[]{pX, pY},
                            new double[]{seg.getTranslateX(), seg.getTranslateY()}) <= (HEAD_RADIUS + BODY_RADIUS)) {
                        gameOver(stage);
                        return;
                    }
                }
                if (villainAlive) {
                    for (Sphere seg : bodyParts) {
                        if (dist.apply(new double[]{vX, vY},
                                new double[]{seg.getTranslateX(), seg.getTranslateY()}) <= (HEAD_RADIUS + BODY_RADIUS)) {
                            killVillain();
                            return;
                        }
                    }
                }
                // ===============================
                // COLLISION DETECTION (Player ↔ Villain2)
                // ===============================
                java.util.function.BiFunction<double[], double[], Double> dist1 = (c, d) -> {
                    double dx = c[0] - d[0];
                    double dy = c[1] - d[1];
                    return Math.sqrt(dx * dx + dy * dy);
                };
                double rX = headX,     rY = headY;
                double uX = villain2X,  uY = villain2Y;
                if (villain2Alive && dist1.apply(new double[]{rX, rY}, new double[]{uX, uY}) <= (HEAD_RADIUS + HEAD_RADIUS)) {
                    killVillain2();
                    gameOver(stage);
                    return;
                }
                for (Sphere seg : villain2BodyParts) {
                    if (dist1.apply(new double[]{rX, rY},
                            new double[]{seg.getTranslateX(), seg.getTranslateY()}) <= (HEAD_RADIUS + BODY_RADIUS)) {
                        gameOver(stage);
                        return;
                    }
                }
                if (villain2Alive) {
                    for (Sphere seg : bodyParts) {
                        if (dist1.apply(new double[]{uX, uY},
                                new double[]{seg.getTranslateX(), seg.getTranslateY()}) <= (HEAD_RADIUS + BODY_RADIUS)) {
                            killVillain2();
                            return;
                        }
                    }
                }
                // ===============================
                // COLLISION DETECTION (Player ↔ Villain3)
                // ===============================
                java.util.function.BiFunction<double[], double[], Double> dist3 = (e, f) -> {
                    double dx = e[0] - f[0];
                    double dy = e[1] - f[1];
                    return Math.sqrt(dx * dx + dy * dy);
                };
                double sX = headX,     sY = headY;
                double wX = villain3X,  wY = villain3Y;
                if (villain3Alive && dist3.apply(new double[]{sX, sY}, new double[]{wX, wY}) <= (HEAD_RADIUS + HEAD_RADIUS)) {
                    killVillain3();
                    gameOver(stage);
                    return;
                }
                for (Sphere seg : villain3BodyParts) {
                    if (dist3.apply(new double[]{sX, sY},
                            new double[]{seg.getTranslateX(), seg.getTranslateY()}) <= (HEAD_RADIUS + BODY_RADIUS)) {
                        gameOver(stage);
                        return;
                    }
                }
                if (villain3Alive) {
                    for (Sphere seg : bodyParts) {
                        if (dist3.apply(new double[]{wX, wY},
                                new double[]{seg.getTranslateX(), seg.getTranslateY()}) <= (HEAD_RADIUS + BODY_RADIUS)) {
                            killVillain3();
                            return;
                        }
                    }
                }
                handleVillainCollision(gameElements, 1, 2);
                handleVillainCollision(gameElements, 1, 3);
                handleVillainCollision(gameElements, 2, 3);
            }
        };
        timer.start();

        stopStartButton.setOnAction(e -> {
            clickSound.play();
            running = !running;
            stopStartButton.setText(running ? "Stop" : "Start");
            if (running) {
                resumeGoldenFruitTimers();
                if (timer != null) timer.start();
            } else {
                pauseGoldenFruitTimers();
                if (timer != null) timer.stop();
            }
        });
        restartButton.setOnAction(e -> {
            restartGame();
        });
        exitButton.setOnAction(e -> {
            clickSound.play();
            stage.close();
        });
        stage.setScene(scene);
        stage.setTitle("3D Snake Game");
        try {
            stage.getIcons().add(new Image("/icon.png"));
        } catch (Exception ignored) {}
        stage.show();
        stage.widthProperty().addListener((obs, oldVal, newVal) -> {
            homeButton.setTranslateX(newVal.doubleValue() - 140);
        });
        updateButtonPositions.run();
    }
    //==================================================================   shrinkSnake   ===============================
    private void shrinkSnake(List<Sphere> bodyParts, Group gameElements) {
        if (bodyParts.size() > 1) {
            Sphere lastSegment = bodyParts.remove(bodyParts.size() - 1);
            javafx.animation.ScaleTransition scale = new javafx.animation.ScaleTransition(Duration.seconds(0.4), lastSegment);
            scale.setToX(0.1);
            scale.setToY(0.1);
            scale.setToZ(0.1);
            javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(Duration.seconds(0.4), lastSegment);
            fade.setToValue(0);
            javafx.animation.ParallelTransition anim = new javafx.animation.ParallelTransition(scale, fade);
            anim.setOnFinished(ev -> gameElements.getChildren().remove(lastSegment));
            anim.play();
        }
    }
    //===========================================================   loadLastSkin   =====================================
    private void loadLastSkin() {
        try {
            var prefs = java.util.prefs.Preferences.userRoot().node(PREFS_NODE);
            String s = prefs.get(PREF_LAST_SKIN, "CLASSIC");
            try { currentSkin = SkinType.valueOf(s); } catch (Exception ignore) { currentSkin = SkinType.CLASSIC; }
        } catch (Throwable ignore) {
            currentSkin = SkinType.CLASSIC;
        }
    }
    //===============================================================   saveLastSkin   =================================
    private void saveLastSkin() {
        try {
            var prefs = java.util.prefs.Preferences.userRoot().node(PREFS_NODE);
            prefs.put(PREF_LAST_SKIN, currentSkin.name());
        } catch (Throwable ignore) {}
    }
    //============================================================   applySkinToPlayerSnake   ==========================
    private void applySkinToPlayerSnake() {
        switch (currentSkin) {
            case CLASSIC -> {
                head.setMaterial(classicHeadMat);
                headGroup.setEffect(null);
                for (Sphere seg : bodyParts) seg.setMaterial(classicMat);
            }
            case NEON -> {
                head.setMaterial(neonHeadMat);
                headGroup.setEffect(neonGlow);
                for (Sphere seg : bodyParts) {
                    seg.setMaterial(neonMat);
                    seg.setEffect(neonGlow);
                }
            }
            case RAINBOW -> {
                headGroup.setEffect(null);
                updateRainbowColors(0);
            }
            case COSMIC -> {
                headGroup.setEffect(new Glow(0.7));
                updateCosmicColors(0);
            }
            case OCEAN -> {
                head.setMaterial(oceanHeadMat);
                headGroup.setEffect(null);
                for (Sphere seg : bodyParts) seg.setMaterial(oceanMat);
            }
            case LIME -> {
                head.setMaterial(limeHeadMat);
                headGroup.setEffect(null);
                for (Sphere seg : bodyParts) seg.setMaterial(limeMat);
            }
        }
    }
    //======================================================================   generateCosmicColor   ========================
    private Color generateCosmicColor() {
        return COSMIC_PALETTE.get(random.nextInt(COSMIC_PALETTE.size()));
    }
    //================================================================   spawnTrailParticles   =========================
    private void spawnTrailParticles(Group gameElements, List<Color> palette, int count, double spread) {
        for (int i = 0; i < count; i++) {
            Sphere seg = bodyParts.get(random.nextInt(bodyParts.size()));
            Color color = palette.get(random.nextInt(palette.size()));
            Circle star = new Circle(2 + Math.random() * 2, color);
            star.setTranslateX(seg.getTranslateX() + (Math.random() - 0.5) * spread);
            star.setTranslateY(seg.getTranslateY() + (Math.random() - 0.5) * spread);
            gameElements.getChildren().add(star);
            FadeTransition fade = new FadeTransition(Duration.seconds(1.2), star);
            fade.setFromValue(1.0);
            fade.setToValue(0.0);
            fade.setOnFinished(e -> gameElements.getChildren().remove(star));
            fade.play();
        }
    }
    //==============================================================   spawnVillain1Trail   ============================
    private void spawnVillainTrail(Group gameElements) {
        if (!villainAlive || villainBodyParts.isEmpty()) return;
        int particleCount = 3 + random.nextInt(2);
        for (int i = 0; i < particleCount; i++) {
            Sphere seg = villainBodyParts.get(random.nextInt(villainBodyParts.size()));
            Circle star = new Circle(2 + Math.random() * 2, VILLAIN_PALETTE.get(random.nextInt(VILLAIN_PALETTE.size())));
            star.setTranslateX(seg.getTranslateX() + (Math.random() - 0.5) * 20);
            star.setTranslateY(seg.getTranslateY() + (Math.random() - 0.5) * 20);
            gameElements.getChildren().add(star);
            FadeTransition fade = new FadeTransition(Duration.seconds(1.0 + Math.random() * 0.5), star);
            fade.setFromValue(1.0);
            fade.setToValue(0.0);
            fade.setOnFinished(e -> gameElements.getChildren().remove(star));
            fade.play();
        }
    }
    //==============================================================   spawnVillain2Trail   ============================
    private void spawnVillain2Trail(Group gameElements) {
        if (!villain2Alive || villain2BodyParts.isEmpty()) return;
        int particleCount = 3 + random.nextInt(2);
        for (int i = 0; i < particleCount; i++) {
            Sphere seg = villain2BodyParts.get(random.nextInt(villain2BodyParts.size()));
            Circle star = new Circle(2 + Math.random() * 2, VILLAIN2_PALETTE.get(random.nextInt(VILLAIN2_PALETTE.size())));
            star.setTranslateX(seg.getTranslateX() + (Math.random() - 0.5) * 20);
            star.setTranslateY(seg.getTranslateY() + (Math.random() - 0.5) * 20);
            gameElements.getChildren().add(star);
            FadeTransition fade = new FadeTransition(Duration.seconds(1.0 + Math.random() * 0.5), star);
            fade.setFromValue(1.0);
            fade.setToValue(0.0);
            fade.setOnFinished(e -> gameElements.getChildren().remove(star));
            fade.play();
        }
    }
    //==============================================================   spawnVillainTrail   =============================
    private void spawnVillain3Trail(Group gameElements) {
        if (!villain3Alive || villain3BodyParts.isEmpty()) return;
        int particleCount = 3 + random.nextInt(2);
        for (int i = 0; i < particleCount; i++) {
            Sphere seg = villain3BodyParts.get(random.nextInt(villain3BodyParts.size()));
            Circle star = new Circle(2 + Math.random() * 2, VILLAIN3_PALETTE.get(random.nextInt(VILLAIN3_PALETTE.size())));
            star.setTranslateX(seg.getTranslateX() + (Math.random() - 0.5) * 20);
            star.setTranslateY(seg.getTranslateY() + (Math.random() - 0.5) * 20);
            gameElements.getChildren().add(star);
            FadeTransition fade = new FadeTransition(Duration.seconds(1.0 + Math.random() * 0.5), star);
            fade.setFromValue(1.0);
            fade.setToValue(0.0);
            fade.setOnFinished(e -> gameElements.getChildren().remove(star));
            fade.play();
        }
    }
    //=====================================================================   spawnVillain1HeadGlow   ==================
    private void spawnVillainHeadGlow(Group gameElements, Color glowColor) {
        if (!villainAlive) return;
        Circle glow = new Circle(6, glowColor);
        glow.setTranslateX(villainHead.getTranslateX());
        glow.setTranslateY(villainHead.getTranslateY());
        gameElements.getChildren().add(glow);
        FadeTransition fade = new FadeTransition(Duration.seconds(0.6), glow);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setOnFinished(e -> gameElements.getChildren().remove(glow));
        fade.play();
    }
    //=====================================================================   spawnVillain2HeadGlow   ==================
    private void spawnVillain2HeadGlow(Group gameElements, Color glowColor) {
        if (!villain2Alive) return;
        Circle glow = new Circle(6, glowColor);
        glow.setTranslateX(villain2Head.getTranslateX());
        glow.setTranslateY(villain2Head.getTranslateY());
        gameElements.getChildren().add(glow);
        FadeTransition fade = new FadeTransition(Duration.seconds(0.6), glow);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setOnFinished(e -> gameElements.getChildren().remove(glow));
        fade.play();
    }
    //=====================================================================   spawnVillain3HeadGlow   ==================
    private void spawnVillain3HeadGlow(Group gameElements, Color glowColor) {
        if (!villain3Alive) return;
        Circle glow = new Circle(6, glowColor);
        glow.setTranslateX(villain3Head.getTranslateX());
        glow.setTranslateY(villain3Head.getTranslateY());
        gameElements.getChildren().add(glow);
        FadeTransition fade = new FadeTransition(Duration.seconds(0.6), glow);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setOnFinished(e -> gameElements.getChildren().remove(glow));
        fade.play();
    }
    //=====================================================================   spawnHeadGlow   ==========================
    private void spawnHeadGlow(Group gameElements, Color glowColor) {
        Circle star = new Circle(6, glowColor);
        star.setTranslateX(headGroup.getTranslateX());
        star.setTranslateY(headGroup.getTranslateY());
        gameElements.getChildren().add(star);
        FadeTransition fade = new FadeTransition(Duration.seconds(0.6), star);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setOnFinished(e -> gameElements.getChildren().remove(star));
        fade.play();
    }
    //=========================================================================   spawnRainbowHeadGlow   ===============
    private void spawnRainbowHeadGlow(Group gameElements, double elapsedSeconds) {
        for (int i = 0; i < RAINBOW_PALETTE.size(); i++) {
            Color color = RAINBOW_PALETTE.get(i);
            double angle = (elapsedSeconds * 120 + i * (360.0 / RAINBOW_PALETTE.size())) % 360;
            double radius = 25;
            double x = headGroup.getTranslateX() + Math.cos(Math.toRadians(angle)) * radius;
            double y = headGroup.getTranslateY() + Math.sin(Math.toRadians(angle)) * radius;
            Circle glow = new Circle(4, color);
            glow.setTranslateX(x);
            glow.setTranslateY(y);
            gameElements.getChildren().add(glow);
            FadeTransition fade = new FadeTransition(Duration.seconds(0.6), glow);
            fade.setFromValue(1.0);
            fade.setToValue(0.0);
            fade.setOnFinished(e -> gameElements.getChildren().remove(glow));
            fade.play();
        }
    }
    //==============================================================   updateRainbowColors   ======================
    private void updateRainbowColors(double elapsedSeconds) {
        double base = (elapsedSeconds * RAINBOW_HUE_SPEED) % 360.0;
        ensureMaterials(rainbowMats, bodyParts.size() + 1);
        PhongMaterial headMat = rainbowMats.get(0);
        headMat.setDiffuseColor(Color.hsb(base, 1.0, 1.0));
        head.setMaterial(headMat);
        for (int i = 0; i < bodyParts.size(); i++) {
            double h = (base + i * 18.0) % 360.0;
            Color c = Color.hsb(h, 1.0, 1.0);
            PhongMaterial m = rainbowMats.get(i + 1);
            m.setDiffuseColor(c);
            bodyParts.get(i).setMaterial(m);
        }
    }
    //==================================================================   ensureMaterials   ======================
    private void ensureMaterials(List<PhongMaterial> mats, int size) {
        while (mats.size() < size) {
            mats.add(new PhongMaterial());
        }
    }
    //===============================================================   updateCosmicColors   ===========================
    private void updateCosmicColors(double elapsedSeconds) {
        double hueShift = (elapsedSeconds * 40) % 360;
        ensureMaterials(cosmicMats, bodyParts.size() + 1);
        PhongMaterial headMat = cosmicMats.get(0);
        headMat.setDiffuseColor(Color.hsb((hueShift + 180) % 360, 0.8, 1.0));
        head.setMaterial(headMat);
        for (int i = 0; i < bodyParts.size(); i++) {
            double hue = (hueShift + (i + 1) * 45) % 360;
            Color cosmic = Color.hsb(hue, 0.8, 1.0);
            if (i % 5 == 0) cosmic = Color.WHITE;
            PhongMaterial m = cosmicMats.get(i + 1);
            m.setDiffuseColor(cosmic);
            bodyParts.get(i).setMaterial(m);
        }
    }
    //======================================================   createPlayerBodySegmentLikeTail   =======================
    private Sphere createPlayerBodySegmentLikeTail() {
        Sphere lastSegment = bodyParts.get(bodyParts.size() - 1);
        Sphere seg = new Sphere(BODY_RADIUS);

        switch (currentSkin) {
            case CLASSIC -> seg.setMaterial(classicMat);
            case NEON -> { seg.setMaterial(neonMat); seg.setEffect(neonGlow); }
            case RAINBOW -> {
                ensureMaterials(rainbowMats, bodyParts.size() + 2);
                seg.setMaterial(rainbowMats.get(bodyParts.size() + 1));
                seg.setEffect(null);
            }
            case OCEAN -> seg.setMaterial(oceanMat);
            case LIME  -> seg.setMaterial(limeMat);
            case COSMIC -> seg.setMaterial(new PhongMaterial(generateCosmicColor()));
        }
        seg.setTranslateX(lastSegment.getTranslateX());
        seg.setTranslateY(lastSegment.getTranslateY());
        seg.setTranslateZ(lastSegment.getTranslateZ());
        return seg;
    }
    //===================================================================   makeTexturedSphere   =======================
    private Sphere makeTexturedSphere(String imagePath) {
        Image img = new Image(getClass().getResource(imagePath).toExternalForm());
        PhongMaterial mat = new PhongMaterial();
        mat.setDiffuseMap(img);
        Sphere s = new Sphere(HEAD_RADIUS * 0.9);
        s.setMaterial(mat);
        s.setTranslateZ(0);
        return s;
    }
    //==============================================================   respawnRandomFruit   ============================
    private void respawnRandomFruit(Group gameElements) {
        gameElements.getChildren().removeAll(fruits);
        double minX = HEAD_RADIUS;
        double maxX = windowWidth - HEAD_RADIUS;
        double minY = topLine.getStartY() + HEAD_RADIUS;
        double maxY = bottomLine.getStartY() - HEAD_RADIUS - LINE_MARGIN;
        if (fruits.size() < 2) return; // safety guard
        int i1 = random.nextInt(fruits.size());
        int i2;
        do { i2 = random.nextInt(fruits.size()); } while (i2 == i1);
        Sphere f1 = fruits.get(i1);
        Sphere f2 = fruits.get(i2);
        double x1 = minX + random.nextDouble() * (maxX - minX);
        double y1 = minY + random.nextDouble() * (maxY - minY);
        double x2 = minX + random.nextDouble() * (maxX - minX);
        double y2 = minY + random.nextDouble() * (maxY - minY);
        int attempts = 0;
        while (Math.hypot(x1 - x2, y1 - y2) < HEAD_RADIUS * 3 && attempts++ < 60) {
            x2 = minX + random.nextDouble() * (maxX - minX);
            y2 = minY + random.nextDouble() * (maxY - minY);
        }
        f1.setTranslateX(x1);
        f1.setTranslateY(y1);
        f2.setTranslateX(x2);
        f2.setTranslateY(y2);
        if (!gameElements.getChildren().contains(f1)) gameElements.getChildren().add(f1);
        if (!gameElements.getChildren().contains(f2)) gameElements.getChildren().add(f2);
    }
    //==============================================================   respawnSingleFruit   ============================
    private void respawnSingleFruit(Group gameElements, Sphere fruit) {
        double minX = HEAD_RADIUS;
        double maxX = windowWidth - HEAD_RADIUS;
        double minY = topLine.getStartY() + HEAD_RADIUS;
        double maxY = bottomLine.getStartY() - HEAD_RADIUS - LINE_MARGIN;
        double x = minX + random.nextDouble() * (maxX - minX);
        double y = minY + random.nextDouble() * (maxY - minY);
        int attempts = 0;
        boolean ok;
        do {
            ok = true;
            for (Sphere f : fruits) {
                if (f == fruit) continue;
                if (!gameElements.getChildren().contains(f)) continue;
                double dx = x - f.getTranslateX();
                double dy = y - f.getTranslateY();
                if (Math.hypot(dx, dy) < HEAD_RADIUS * 3) { ok = false; break; }
            }
            if (!ok) {
                x = minX + random.nextDouble() * (maxX - minX);
                y = minY + random.nextDouble() * (maxY - minY);
            }
        } while (!ok && attempts++ < 50);
        fruit.setTranslateX(x);
        fruit.setTranslateY(y);
        if (!gameElements.getChildren().contains(fruit)) {
            gameElements.getChildren().add(fruit);
        }
    }
    //=====================================================  handleVillainCollision  ===================================
    private void handleVillainCollision(Group gameElements, int villainId1, int villainId2) {
        Sphere head1 = null, head2 = null;
        List<Sphere> body1 = null, body2 = null;
        double x1 = 0, y1 = 0, x2 = 0, y2 = 0;
        double speed1 = 0, speed2 = 0;
        switch (villainId1) {
            case 1 -> {
                head1 = villainHead;
                body1 = villainBodyParts;
                x1 = villainX;
                y1 = villainY;
                speed1 = villainSpeed;
            }
            case 2 -> {
                head1 = villain2Head;
                body1 = villain2BodyParts;
                x1 = villain2X;
                y1 = villain2Y;
                speed1 = villain2Speed;
            }
            case 3 -> {
                head1 = villain3Head;
                body1 = villain3BodyParts;
                x1 = villain3X;
                y1 = villain3Y;
                speed1 = villain3Speed;
            }
        }
        switch (villainId2) {
            case 1 -> {
                head2 = villainHead;
                body2 = villainBodyParts;
                x2 = villainX;
                y2 = villainY;
                speed2 = villainSpeed;
            }
            case 2 -> {
                head2 = villain2Head;
                body2 = villain2BodyParts;
                x2 = villain2X;
                y2 = villain2Y;
                speed2 = villain2Speed;
            }
            case 3 -> {
                head2 = villain3Head;
                body2 = villain3BodyParts;
                x2 = villain3X;
                y2 = villain3Y;
                speed2 = villain3Speed;
            }
        }
        if (head1 == null || head2 == null) return;
        double distance = Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
        if (distance < HEAD_RADIUS * 2) {
            disableVillain(villainId1, gameElements);
            disableVillain(villainId2, gameElements);
            return;
        }
        for (Sphere seg : body2) {
            double d = Math.sqrt((x1 - seg.getTranslateX()) * (x1 - seg.getTranslateX()) +
                    (y1 - seg.getTranslateY()) * (y1 - seg.getTranslateY()));
            if (d < HEAD_RADIUS + BODY_RADIUS) {
                disableVillain(villainId1, gameElements);
                return;
            }
        }
        for (Sphere seg : body1) {
            double d = Math.sqrt((x2 - seg.getTranslateX()) * (x2 - seg.getTranslateX()) +
                    (y2 - seg.getTranslateY()) * (y2 - seg.getTranslateY()));
            if (d < HEAD_RADIUS + BODY_RADIUS) {
                disableVillain(villainId2, gameElements);
                return;
            }
        }
    }
    //=================================================  disableVillain  ===============================================
private void disableVillain(int id, Group gameElements) {
    switch (id) {
        case 1 -> {
            villainAlive = false;
            gameElements.getChildren().removeAll(villainHead);
            gameElements.getChildren().removeAll(villainBodyParts);
            PauseTransition pt = new PauseTransition(Duration.seconds(3));
            pt.setOnFinished(ev -> respawnVillain(gameElements));
            pt.play();
        }
        case 2 -> {
            villain2Alive = false;
            gameElements.getChildren().removeAll(villain2Head);
            gameElements.getChildren().removeAll(villain2BodyParts);
            PauseTransition pt = new PauseTransition(Duration.seconds(3));
            pt.setOnFinished(ev -> respawnVillain2(gameElements));
            pt.play();
        }
        case 3 -> {
            villain3Alive = false;
            gameElements.getChildren().removeAll(villain3Head);
            gameElements.getChildren().removeAll(villain3BodyParts);
            PauseTransition pt = new PauseTransition(Duration.seconds(3));
            pt.setOnFinished(ev -> respawnVillain3(gameElements));
            pt.play();
        }
    }
}
    //==============================================================   setupGoldenFruit   ==============================
    private void setupGoldenFruit(Group gameElements) {
        Image goldenImg = new Image(getClass().getResource("/images/golden.png").toExternalForm());
        PhongMaterial goldenMat = new PhongMaterial();
        goldenMat.setDiffuseMap(goldenImg);
        goldenFruit = new Sphere(GOLDEN_FRUIT_RADIUS);
        goldenFruit.setMaterial(goldenMat);
        goldenFruit.setTranslateZ(0);
        goldenFruitDisappearTimer = new PauseTransition(Duration.seconds(GOLDEN_FRUIT_SHOW_DURATION));
        goldenFruitDisappearTimer.setOnFinished(e -> {
            if (goldenFruitVisible) {
                gameElements.getChildren().remove(goldenFruit);
                goldenFruitVisible = false;
            }
        });
        goldenFruitRespawnTimer = new PauseTransition(Duration.seconds(GOLDEN_FRUIT_APPEAR_INTERVAL));
        goldenFruitRespawnTimer.setOnFinished(e -> {
            spawnGoldenFruit(gameElements);
            goldenFruitRespawnTimer.playFromStart();
        });
        goldenFruitRespawnTimer.playFromStart();
    }
    //=====================================================================   pauseGoldenFruitTimers   =================
    private void pauseGoldenFruitTimers() {
        if (goldenFruitRespawnTimer != null) goldenFruitRespawnTimer.pause();
        if (goldenFruitDisappearTimer != null) goldenFruitDisappearTimer.pause();
        if (goldenPulse != null) goldenPulse.pause();
    }
    //====================================================================   resumeGoldenFruitTimers   =================
    private void resumeGoldenFruitTimers() {
        if (goldenFruitRespawnTimer != null) goldenFruitRespawnTimer.play();
        if (goldenFruitVisible && goldenFruitDisappearTimer != null) goldenFruitDisappearTimer.play();
        if (goldenPulse != null) goldenPulse.play();
    }
    //==================================================================   spawnGoldenFruit   ==========================
    private void spawnGoldenFruit(Group gameElements) {
        if (goldenFruit != null) {
            gameElements.getChildren().remove(goldenFruit);
        }
        double minX = HEAD_RADIUS;
        double maxX = windowWidth - HEAD_RADIUS;
        double minY = topLine.getStartY() + HEAD_RADIUS;
        double maxY = bottomLine.getStartY() - HEAD_RADIUS - LINE_MARGIN;
        double x = minX + random.nextDouble() * (maxX - minX);
        double y = minY + random.nextDouble() * (maxY - minY);
        goldenFruit.setTranslateX(x);
        goldenFruit.setTranslateY(y);
        if (!gameElements.getChildren().contains(goldenFruit)) {
            gameElements.getChildren().add(goldenFruit);
        }
        goldenFruitVisible = true;
        if (goldenPulse != null) goldenPulse.stop();
        goldenPulse = new javafx.animation.ScaleTransition(Duration.seconds(1), goldenFruit);
        goldenPulse.setFromX(1.0);
        goldenPulse.setFromY(1.0);
        goldenPulse.setToX(1.2);
        goldenPulse.setToY(1.2);
        goldenPulse.setCycleCount(javafx.animation.Animation.INDEFINITE);
        goldenPulse.setAutoReverse(true);
        goldenPulse.play();
        goldenFruitDisappearTimer.playFromStart();
    }
    //======================================================================   gameOver   ==============================
    private void gameOver(Stage stage) {
        if (gameOverSound != null) {
            gameOverSound.stop();
            gameOverSound.play();
        }
        running = false;
        if (timer != null) timer.stop();
        running = false;
        specialItemVisible = false;
        if (specialDespawnTimer != null) {
            specialDespawnTimer.stop();
        }
        pauseGoldenFruitTimers();
        Group gameElements = (Group) head.getParent().getParent();
        gameElements.getChildren().removeAll(specialItems);
        goldenFruitVisible = false;
        if (goldenFruit != null) {
            gameElements.getChildren().remove(goldenFruit);
        }
        boolean newHigh = score > highScore;
        if (newHigh) {
            highScore = score;
        }
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(newHigh ? "New High Score!" : "Game Over");
            alert.setHeaderText(null);
            String message = "Game Over!\nFinal Score: " + score;
            if (newHigh) {
                message = "🎉 NEW HIGH SCORE: " + score + "\n" + message;
            }
            alert.setContentText(message);
            ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.CANCEL_CLOSE);
            ButtonType restartButton = new ButtonType("Restart", ButtonBar.ButtonData.OK_DONE);
            alert.getButtonTypes().setAll(okButton, restartButton);
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.setPrefSize(400, 180);
            dialogPane.setStyle("-fx-background-color: black;");
            Label contentLabel = (Label) dialogPane.lookup(".content.label");
            if (contentLabel != null) {
                contentLabel.setStyle(
                        "-fx-text-fill: " + (newHigh ? "yellow" : "#FFFFFF") + "; " +
                                "-fx-font-size: 22px; " +
                                "-fx-font-weight: bold; " +
                                "-fx-text-alignment: center;"
                );
            }
            Button okBtn = (Button) dialogPane.lookupButton(okButton);
            okBtn.setStyle(
                    "-fx-background-color: gray; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-weight: bold; " +
                            "-fx-font-size: 14px;"
            );
            Button restartBtn = (Button) dialogPane.lookupButton(restartButton);
            restartBtn.setStyle(
                    "-fx-background-color: green; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-weight: bold; " +
                            "-fx-font-size: 14px;"
            );
            alert.showAndWait().ifPresent(response -> {
                if (response == restartButton) {
                    restartGame();
                }
            });
        });
    }
    //==============================================================   killVillain1   ==================================
    private void killVillain() {
        if (!villainAlive) return;
        villainAlive = false;
        Group rootGroup = (Group) head.getParent().getParent();
        if (villainRespawnTimer != null) {
            villainRespawnTimer.stop();
        }
        if (villainHead != null) rootGroup.getChildren().remove(villainHead);
        if (!villainBodyParts.isEmpty()) {
            rootGroup.getChildren().removeAll(villainBodyParts);
            villainBodyParts.clear();
        }
        villainRespawnTimer = new PauseTransition(Duration.seconds(3));
        villainRespawnTimer.setOnFinished(e -> respawnVillain(rootGroup));
        villainRespawnTimer.play();
    }
    //==============================================================   killVillain2   ==================================
    private void killVillain2() {
        if (!villain2Alive) return;
        villain2Alive = false;
        Group rootGroup = (Group) head.getParent().getParent();
        if (villain2RespawnTimer != null) {
            villain2RespawnTimer.stop();
        }
        if (villain2Head != null) rootGroup.getChildren().remove(villain2Head);
        if (!villain2BodyParts.isEmpty()) {
            rootGroup.getChildren().removeAll(villain2BodyParts);
            villain2BodyParts.clear();
        }
        villain2RespawnTimer = new PauseTransition(Duration.seconds(3));
        villain2RespawnTimer.setOnFinished(e -> respawnVillain2(rootGroup));
        villain2RespawnTimer.play();
    }
    //==============================================================   killVillain3   ==================================
    private void killVillain3() {
        if (!villain3Alive) return;
        villain3Alive = false;
        Group rootGroup = (Group) head.getParent().getParent();
        if (villain3RespawnTimer != null) {
            villain3RespawnTimer.stop();
        }
        if (villain3Head != null) rootGroup.getChildren().remove(villain3Head);
        if (!villain3BodyParts.isEmpty()) {
            rootGroup.getChildren().removeAll(villain3BodyParts);
            villain3BodyParts.clear();
        }
        villain3RespawnTimer = new PauseTransition(Duration.seconds(3));
        villain3RespawnTimer.setOnFinished(e -> respawnVillain3(rootGroup));
        villain3RespawnTimer.play();
    }
    //==============================================================   respawnVillain1   ===============================
    private void respawnVillain(Group rootGroup) {
        if (villainHead != null) rootGroup.getChildren().remove(villainHead);
        if (!villainBodyParts.isEmpty()) {
            rootGroup.getChildren().removeAll(villainBodyParts);
            villainBodyParts.clear();
        }
        villainAlive = true;
        villainX = windowWidth * 0.20;
        villainY = windowHeight * 0.40;
        villainDirection = 0;
        villainHistory.clear();
        villainHead = new Sphere(HEAD_RADIUS);
        villainHead.setMaterial(new PhongMaterial(Color.DARKSLATEGREY));
        villainHead.setTranslateX(villainX);
        villainHead.setTranslateY(villainY);
        rootGroup.getChildren().add(villainHead);
        PhongMaterial bodyMat = new PhongMaterial(Color.CORNSILK);
        for (int i = 0; i < 3; i++) {
            Sphere seg = new Sphere(BODY_RADIUS);
            seg.setMaterial(bodyMat);
            seg.setTranslateX(villainX - (i + 1) * SEGMENT_SPACING);
            seg.setTranslateY(villainY);
            villainBodyParts.add(seg);
        }
        rootGroup.getChildren().addAll(villainBodyParts);
    }
    //==============================================================   respawnVillain2   ===============================
    private void respawnVillain2(Group rootGroup) {
        if (villain2Head != null) rootGroup.getChildren().remove(villain2Head);
        if (!villain2BodyParts.isEmpty()) {
            rootGroup.getChildren().removeAll(villain2BodyParts);
            villain2BodyParts.clear();
        }
        villain2Alive = true;
        villain2X = windowWidth * 0.62;
        villain2Y = windowHeight * 0.72;
        villain2Direction = 2;
        villain2History.clear();
        villain2Head = new Sphere(HEAD_RADIUS);
        villain2Head.setMaterial(new PhongMaterial(Color.MAROON));
        villain2Head.setTranslateX(villain2X);
        villain2Head.setTranslateY(villain2Y);
        rootGroup.getChildren().add(villain2Head);
        PhongMaterial bodyMat = new PhongMaterial(Color.TOMATO);
        for (int i = 0; i < 3; i++) {
            Sphere seg = new Sphere(BODY_RADIUS);
            seg.setMaterial(bodyMat);
            seg.setTranslateX(villain2X - (i + 1) * SEGMENT_SPACING);
            seg.setTranslateY(villain2Y);
            villain2BodyParts.add(seg);
        }
        rootGroup.getChildren().addAll(villain2BodyParts);
    }
    //==============================================================   respawnVillain3   ===============================
    private void respawnVillain3(Group rootGroup) {
        if (villain3Head != null) rootGroup.getChildren().remove(villain3Head);
        if (!villain3BodyParts.isEmpty()) {
            rootGroup.getChildren().removeAll(villain3BodyParts);
            villain3BodyParts.clear();
        }
        villain3Alive = true;
        villain3X = windowWidth * 0.80;
        villain3Y = windowHeight * 0.28;
        villain3Direction = 3;
        villain3History.clear();
        villain3Head = new Sphere(HEAD_RADIUS);
        villain3Head.setMaterial(new PhongMaterial(Color.SADDLEBROWN));
        villain3Head.setTranslateX(villain3X);
        villain3Head.setTranslateY(villain3Y);
        rootGroup.getChildren().add(villain3Head);
        PhongMaterial bodyMat = new PhongMaterial(Color.ROSYBROWN);
        for (int i = 0; i < 3; i++) {
            Sphere seg = new Sphere(BODY_RADIUS);
            seg.setMaterial(bodyMat);
            seg.setTranslateX(villain3X - (i + 1) * SEGMENT_SPACING);
            seg.setTranslateY(villain3Y);
            villain3BodyParts.add(seg);
        }
        rootGroup.getChildren().addAll(villain3BodyParts);
    }
    //================================================================   resetSnake   ==================================
    private void resetSnake() {
        headX = windowWidth / 2;
        headY = windowHeight / 2;
        direction = 0;
        positionHistory.clear();
        playerFruitCount = 0;
        villainFruitCount = 0;
        villain2FruitCount = 0;
        villain3FruitCount = 0;
        speed = BASE_SPEED;
        villainSpeed = BASE_SPEED;
        villain2Speed = BASE_SPEED;
        villain3Speed = BASE_SPEED;
        if (playerGoldenBoostTimer != null) playerGoldenBoostTimer.stop();
        if (villainGoldenBoostTimer != null) villainGoldenBoostTimer.stop();
        if (villain2GoldenBoostTimer != null) villain2GoldenBoostTimer.stop();
        if (villain3GoldenBoostTimer != null) villain3GoldenBoostTimer.stop();
        goldenFruitVisible = false;
        ((Group) head.getParent().getParent()).getChildren().remove(goldenFruit);
        if (specialDespawnTimer != null) specialDespawnTimer.stop();
        specialItemVisible = false;
        Group rootGroup = (Group) head.getParent().getParent();
        rootGroup.getChildren().removeAll(fruits);
        rootGroup.getChildren().removeAll(specialItems);
        for (Sphere s : bodyParts) rootGroup.getChildren().remove(s);
        bodyParts.clear();
        PhongMaterial bodyMaterial = new PhongMaterial(Color.CHOCOLATE);
        bodyMaterial.setSpecularColor(Color.CHOCOLATE);
        for (int i = 0; i < 3; i++) {
            Sphere segment = new Sphere(BODY_RADIUS);
            segment.setMaterial(bodyMaterial);
            segment.setTranslateX(headX - (i + 1) * SEGMENT_SPACING);
            segment.setTranslateY(headY);
            bodyParts.add(segment);
        }
        rootGroup.getChildren().addAll(bodyParts);
        if (villainRespawnTimer != null) {
            villainRespawnTimer.stop();
        }
        if (villain2RespawnTimer != null) {
            villain2RespawnTimer.stop();
        }
        if (villain3RespawnTimer != null) {
            villain3RespawnTimer.stop();
        }
        if (villainHead != null) rootGroup.getChildren().remove(villainHead);
        if (!villainBodyParts.isEmpty()) {
            rootGroup.getChildren().removeAll(villainBodyParts);
            villainBodyParts.clear();
        }
        if (villain2Head != null) rootGroup.getChildren().remove(villain2Head);
        if (!villain2BodyParts.isEmpty()) {
            rootGroup.getChildren().removeAll(villain2BodyParts);
            villain2BodyParts.clear();
        }
        if (villain3Head != null) rootGroup.getChildren().remove(villain3Head);
        if (!villain3BodyParts.isEmpty()) {
            rootGroup.getChildren().removeAll(villain3BodyParts);
            villain3BodyParts.clear();
        }
        villainAlive = true;
        villainX = windowWidth * 0.20;
        villainY = windowHeight * 0.40;
        villainDirection = 0;
        villainHistory.clear();
        villain2Alive = true;
        villain2X = windowWidth * 0.62;
        villain2Y = windowHeight * 0.72;
        villain2Direction = 2;
        villain2History.clear();
        villain3Alive = true;
        villain3X = windowWidth * 0.80;
        villain3Y = windowHeight * 0.28;
        villain3Direction = 3;
        villain3History.clear();
        villainHead = new Sphere(HEAD_RADIUS);
        villainHead.setMaterial(new PhongMaterial(Color.DARKSLATEGREY));
        villainHead.setTranslateX(villainX);
        villainHead.setTranslateY(villainY);
        rootGroup.getChildren().add(villainHead);
        villain2Head = new Sphere(HEAD_RADIUS);
        villain2Head.setMaterial(new PhongMaterial(Color.MAROON));
        villain2Head.setTranslateX(villain2X);
        villain2Head.setTranslateY(villain2Y);
        rootGroup.getChildren().add(villain2Head);
        villain3Head = new Sphere(HEAD_RADIUS);
        villain3Head.setMaterial(new PhongMaterial(Color.SADDLEBROWN));
        villain3Head.setTranslateX(villain3X);
        villain3Head.setTranslateY(villain3Y);
        rootGroup.getChildren().add(villain3Head);
        PhongMaterial villainBodyMaterial = new PhongMaterial(Color.CORNSILK);
        for (int i = 0; i < 3; i++) {
            Sphere segment = new Sphere(BODY_RADIUS);
            segment.setMaterial(villainBodyMaterial);
            segment.setTranslateX(villainX - (i + 1) * SEGMENT_SPACING);
            segment.setTranslateY(villainY);
            villainBodyParts.add(segment);
        }
        rootGroup.getChildren().addAll(villainBodyParts);
        PhongMaterial villain2BodyMaterial = new PhongMaterial(Color.TOMATO);
        for (int i = 0; i < 3; i++) {
            Sphere segment = new Sphere(BODY_RADIUS);
            segment.setMaterial(villain2BodyMaterial);
            segment.setTranslateX(villain2X - (i + 1) * SEGMENT_SPACING);
            segment.setTranslateY(villain2Y);
            villain2BodyParts.add(segment);
        }
        rootGroup.getChildren().addAll(villain2BodyParts);
        PhongMaterial villain3BodyMaterial = new PhongMaterial(Color.ROSYBROWN);
        for (int i = 0; i < 3; i++) {
            Sphere segment = new Sphere(BODY_RADIUS);
            segment.setMaterial(villain3BodyMaterial);
            segment.setTranslateX(villain3X - (i + 1) * SEGMENT_SPACING);
            segment.setTranslateY(villain3Y);
            villain3BodyParts.add(segment);
        }
        rootGroup.getChildren().addAll(villain3BodyParts);
        respawnRandomFruit(rootGroup);
        score = 0;
        scoreText.setText("Score: " + score);
        lastSpecialItem = null;
        applySkinToPlayerSnake();
    }
    //===========================================================================   restartGame   ======================
    private void restartGame() {
        clickSound.play();
        resetSnake();
        running = true;
        stopStartButton.setText("Stop");
        resumeGoldenFruitTimers();
        if (timer != null) {
            timer.start();
        }
    }
    //====================================================================   dist2   ===================================
    private double dist2(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2, dy = y1 - y2;
        return dx*dx + dy*dy;
    }
    //================================================================   posAtFromTail   ================================
    /**
     * Walks a position history deque from the newest entry backwards and returns
     * the entry located {@code stepsBack} positions before the tail (0 = newest).
     * Replaces the previous O(history) stream().skip() lookup.
     */
    private double[] posAtFromTail(Deque<double[]> history, int stepsBack, double[] fallback) {
        if (stepsBack < 0) stepsBack = 0;
        Iterator<double[]> it = history.descendingIterator();
        double[] result = null;
        int i = 0;
        while (i <= stepsBack && it.hasNext()) {
            result = it.next();
            i++;
        }
        return result != null ? result : fallback;
    }
    //===================================================   wrapX   ====================================
    private double wrapX(double x) {
        double width = windowWidth - HEAD_RADIUS - LINE_MARGIN;
        double left = HEAD_RADIUS;
        double shifted = (x - left) % (width - left);
        if (shifted < 0) shifted += (width - left);
        return left + shifted;
    }
    //==============================================================   wrapY   ===================================
    private double wrapY(double y) {
        double top = topLine.getStartY() + HEAD_RADIUS;
        double bottom = bottomLine.getStartY() - HEAD_RADIUS - LINE_MARGIN;
        if (y > bottom) return top;
        if (y < top) return bottom;
        return y;
    }
    //====================================================================   pathSafeFromPlayer1   =====================
    private boolean pathSafeFromPlayer(int dir, int steps) {
        double x = villainX, y = villainY;
        double dangerR = HEAD_RADIUS + BODY_RADIUS * 0.95;
        double dangerR2 = dangerR * dangerR;
        for (int i = 0; i < steps; i++) {
            switch (dir) {
                case 0 -> x += villainSpeed;
                case 1 -> y += villainSpeed;
                case 2 -> x -= villainSpeed;
                case 3 -> y -= villainSpeed;
            }
            x = wrapX(x);
            y = wrapY(y);
            if (dist2(x, y, headX, headY) <= dangerR2) return false;
            for (Sphere seg : bodyParts) {
                if (dist2(x, y, seg.getTranslateX(), seg.getTranslateY()) <= dangerR2) return false;
            }
            if (dist2(x, y, villain2X, villain2Y) <= dangerR2) return false;
            for (Sphere seg : villain2BodyParts) {
                if (dist2(x, y, seg.getTranslateX(), seg.getTranslateY()) <= dangerR2) return false;
            }
            if (dist2(x, y, villain3X, villain3Y) <= dangerR2) return false;
            for (Sphere seg : villain3BodyParts) {
                if (dist2(x, y, seg.getTranslateX(), seg.getTranslateY()) <= dangerR2) return false;
            }
        }
        double px = headX, py = headY;
        switch (direction) {
            case 0 -> px += speed;
            case 1 -> py += speed;
            case 2 -> px -= speed;
            case 3 -> py -= speed;
        }
        px = wrapX(px);
        py = wrapY(py);
        double headOnR = HEAD_RADIUS * 2.0;
        if (dist2(x, y, px, py) <= headOnR * headOnR) return false;
        return true;
    }
    //====================================================================   pathSafeFromPlayer2   =====================
    private boolean pathSafeFromPlayer2(int dir2, int steps2) {
        double x1 = villain2X, y1 = villain2Y;
        double dangerR1 = HEAD_RADIUS + BODY_RADIUS * 0.95;
        double dangerR21 = dangerR1 * dangerR1;
        for (int i = 0; i < steps2; i++) {
            switch (dir2) {
                case 0 -> x1 += villain2Speed;
                case 1 -> y1 += villain2Speed;
                case 2 -> x1 -= villain2Speed;
                case 3 -> y1 -= villain2Speed;
            }
            x1 = wrapX(x1);
            y1 = wrapY(y1);
            if (dist2(x1, y1, headX, headY) <= dangerR21) return false;
            for (Sphere seg : bodyParts) {
                if (dist2(x1, y1, seg.getTranslateX(), seg.getTranslateY()) <= dangerR21) return false;
            }
            if (dist2(x1, y1, villainX, villainY) <= dangerR21) return false;
            for (Sphere seg : villainBodyParts) {
                if (dist2(x1, y1, seg.getTranslateX(), seg.getTranslateY()) <= dangerR21) return false;
            }
            if (dist2(x1, y1, villain3X, villain3Y) <= dangerR21) return false;
            for (Sphere seg : villain3BodyParts) {
                if (dist2(x1, y1, seg.getTranslateX(), seg.getTranslateY()) <= dangerR21) return false;
            }
        }
        double px1 = headX, py1 = headY;
        switch (direction) {
            case 0 -> px1 += speed;
            case 1 -> py1 += speed;
            case 2 -> px1 -= speed;
            case 3 -> py1 -= speed;
        }
        px1 = wrapX(px1);
        py1 = wrapY(py1);
        double headOnR = HEAD_RADIUS * 2.0;
        if (dist2(x1, y1, px1, py1) <= headOnR * headOnR) return false;
        return true;
    }
    //====================================================================   pathSafeFromPlayer3   =====================
    private boolean pathSafeFromPlayer3(int dir3, int steps3) {
        double x2 = villain3X, y2 = villain3Y;
        double dangerR2 = HEAD_RADIUS + BODY_RADIUS * 0.95;
        double dangerR22 = dangerR2 * dangerR2;
        for (int i = 0; i < steps3; i++) {
            switch (dir3) {
                case 0 -> x2 += villain3Speed;
                case 1 -> y2 += villain3Speed;
                case 2 -> x2 -= villain3Speed;
                case 3 -> y2 -= villain3Speed;
            }
            x2 = wrapX(x2);
            y2 = wrapY(y2);
            if (dist2(x2, y2, headX, headY) <= dangerR22) return false;
            for (Sphere seg : bodyParts) {
                if (dist2(x2, y2, seg.getTranslateX(), seg.getTranslateY()) <= dangerR22) return false;
            }
            if (dist2(x2, y2, villainX, villainY) <= dangerR22) return false;
            for (Sphere seg : villainBodyParts) {
                if (dist2(x2, y2, seg.getTranslateX(), seg.getTranslateY()) <= dangerR22) return false;
            }
            if (dist2(x2, y2, villain2X, villain2Y) <= dangerR22) return false;
            for (Sphere seg : villain2BodyParts) {
                if (dist2(x2, y2, seg.getTranslateX(), seg.getTranslateY()) <= dangerR22) return false;
            }
        }
        double px2 = headX, py2 = headY;
        switch (direction) {
            case 0 -> px2 += speed;
            case 1 -> py2 += speed;
            case 2 -> px2 -= speed;
            case 3 -> py2 -= speed;
        }
        px2 = wrapX(px2);
        py2 = wrapY(py2);
        double headOnR = HEAD_RADIUS * 2.0;
        if (dist2(x2, y2, px2, py2) <= headOnR * headOnR) return false;
        return true;
    }
    //===================================================================   updateVillainAI 1  =========================
    private void updateVillainAI(Group gameElements) {
        if (!villainAlive) return;
        Sphere targetFruit = null;
        if (goldenFruitVisible) {
            targetFruit = goldenFruit;
        } else {
            double best = Double.MAX_VALUE;
            for (Sphere fruit : fruits) {
                if (!gameElements.getChildren().contains(fruit)) continue;
                double dx = fruit.getTranslateX() - villainX;
                double dy = fruit.getTranslateY() - villainY;
                double d2 = dx * dx + dy * dy;
                if (d2 < best) {
                    best = d2;
                    targetFruit = fruit;
                }
            }
        }
        if (targetFruit == null) {
            int steps = Math.max(5, (int)Math.ceil(SEGMENT_SPACING / Math.max(1.0, villainSpeed)));
            if (!pathSafeFromPlayer(villainDirection, steps)) {
                for (int d = 0; d < 4; d++) {
                    if (pathSafeFromPlayer(d, steps)) {
                        villainDirection = d;
                        break;
                    }
                }
            }
        } else {
            int[] dirs = {0, 1, 2, 3};
            double[] scores = new double[4];
            for (int i = 0; i < 4; i++) {
                double tx = villainX, ty = villainY;
                switch (dirs[i]) {
                    case 0 -> tx += villainSpeed;
                    case 1 -> ty += villainSpeed;
                    case 2 -> tx -= villainSpeed;
                    case 3 -> ty -= villainSpeed;
                }
                tx = wrapX(tx);
                ty = wrapY(ty);
                double dx = targetFruit.getTranslateX() - tx;
                double dy = targetFruit.getTranslateY() - ty;
                scores[i] = Math.hypot(dx, dy);
                if (dirs[i] != villainDirection) scores[i] += 2.0;
            }
            int bestDir = -1;
            double bestScore = Double.MAX_VALUE;
            int steps = Math.max(6, (int)Math.ceil(SEGMENT_SPACING / Math.max(1.0, villainSpeed)) + 2);

            for (int i = 0; i < 4; i++) {
                for (int d = 0; d < 4; d++) {
                    if (scores[d] < bestScore && pathSafeFromPlayer(dirs[d], steps)) {
                        bestScore = scores[d];
                        bestDir = dirs[d];
                    }
                }
            }
            if (bestDir == -1) {
                double bestClear = -1;
                int fallback = villainDirection;
                for (int d = 0; d < 4; d++) {
                    double tx = villainX, ty = villainY;
                    switch (d) {
                        case 0 -> tx += villainSpeed;
                        case 1 -> ty += villainSpeed;
                        case 2 -> tx -= villainSpeed;
                        case 3 -> ty -= villainSpeed;
                    }
                    tx = wrapX(tx);
                    ty = wrapY(ty);
                    double minD2 = dist2(tx, ty, headX, headY);
                    for (Sphere seg : bodyParts) {
                        double d2 = dist2(tx, ty, seg.getTranslateX(), seg.getTranslateY());
                        if (d2 < minD2) minD2 = d2;
                    }
                    if (minD2 > bestClear) { bestClear = minD2; fallback = d; }
                }
                bestDir = fallback;
            }
            if (bestDir != villainDirection) {
                if (framesSinceVillainTurn >= VILLAIN_TURN_COOLDOWN_FRAMES || !pathSafeFromPlayer(villainDirection, 2)) {
                    villainDirection = bestDir;
                    framesSinceVillainTurn = 0;
                } else {
                    framesSinceVillainTurn++;
                }
            } else {
                framesSinceVillainTurn++;
            }
        }
        switch (villainDirection) {
            case 0 -> villainX += villainSpeed;
            case 1 -> villainY += villainSpeed;
            case 2 -> villainX -= villainSpeed;
            case 3 -> villainY -= villainSpeed;
        }
        double oldX = villainX;
        double oldY = villainY;
        villainX = wrapX(villainX);
        villainY = wrapY(villainY);
        if (villainX != oldX || villainY != oldY) {
            if (!villainHistory.isEmpty()) villainHistory.removeLast();
            villainHistory.addLast(new double[]{villainX, villainY});
        }
        villainHead.setTranslateX(villainX);
        villainHead.setTranslateY(villainY);
        villainHistory.addLast(new double[]{villainX, villainY});
        int stepFrames = Math.max(1, (int)(SEGMENT_SPACING / villainSpeed));
        int maxHistorySize = (villainBodyParts.size() + 1) * stepFrames + 50;
        while (villainHistory.size() > maxHistorySize) villainHistory.pollFirst();
        int index = 1;
        double elapsedSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
        for (Sphere segment : villainBodyParts) {
            double[] pos = posAtFromTail(villainHistory, index * stepFrames,
                    new double[]{segment.getTranslateX(), segment.getTranslateY()});
            segment.setTranslateX(pos[0]);
            segment.setTranslateY(pos[1]);
            if (index == villainBodyParts.size()) {
                double maxAmplitude = SEGMENT_SPACING / 2.0;
                double rippleAmplitude = Math.min(5 + villainBodyParts.size() * 0.2, maxAmplitude);
                double rippleFrequency = 5;
                segment.setTranslateX(pos[0] + rippleAmplitude * Math.sin(elapsedSeconds * rippleFrequency));
            }
            index++;
        }
    }
    //===================================================================  updateVillainAI 2  ==========================
    private void updateVillain2AI(Group gameElements) {
        if (!villain2Alive) return;
        Sphere targetFruit = null;
        if (goldenFruitVisible) {
            targetFruit = goldenFruit;
        } else {
            double best = Double.MAX_VALUE;
            for (Sphere fruit : fruits) {
                if (!gameElements.getChildren().contains(fruit)) continue;
                double dx = fruit.getTranslateX() - villain2X;
                double dy = fruit.getTranslateY() - villain2Y;
                double d2 = dx * dx + dy * dy;
                if (d2 < best) {
                    best = d2;
                    targetFruit = fruit;
                }
            }
        }
        if (targetFruit == null) {
            int steps = Math.max(5, (int)Math.ceil(SEGMENT_SPACING / Math.max(1.0, villain2Speed)));
            if (!pathSafeFromPlayer2(villain2Direction, steps)) {
                for (int d = 0; d < 4; d++) {
                    if (pathSafeFromPlayer2(d, steps)) {
                        villain2Direction = d;
                        break;
                    }
                }
            }
        } else {
            int[] dirs1 = {1, 0, 2, 3};
            double[] scores1 = new double[4];
            for (int i = 0; i < 4; i++) {
                double tx1 = villain2X, ty1 = villain2Y;
                switch (dirs1[i]) {
                    case 0 -> tx1 += villain2Speed;
                    case 1 -> ty1 += villain2Speed;
                    case 2 -> tx1 -= villain2Speed;
                    case 3 -> ty1 -= villain2Speed;
                }
                tx1 = wrapX(tx1);
                ty1 = wrapY(ty1);
                double dx = targetFruit.getTranslateX() - tx1;
                double dy = targetFruit.getTranslateY() - ty1;
                scores1[i] = Math.hypot(dx, dy);
                if (dirs1[i] != villain2Direction) scores1[i] += 2.0;
            }
            int bestDir1 = -1;
            double bestScore = Double.MAX_VALUE;
            int steps = Math.max(6, (int)Math.ceil(SEGMENT_SPACING / Math.max(1.0, villain2Speed)) + 2);

            for (int i = 0; i < 4; i++) {
                for (int d = 0; d < 4; d++) {
                    if (scores1[d] < bestScore && pathSafeFromPlayer2(dirs1[d], steps)) {
                        bestScore = scores1[d];
                        bestDir1 = dirs1[d];
                    }
                }
            }
            if (bestDir1 == -1) {
                double bestClear = -1;
                int fallback = villain2Direction;
                for (int d = 0; d < 4; d++) {
                    double tx1 = villain2X, ty1 = villain2Y;
                    switch (d) {
                        case 0 -> tx1 += villain2Speed;
                        case 1 -> ty1 += villain2Speed;
                        case 2 -> tx1 -= villain2Speed;
                        case 3 -> ty1 -= villain2Speed;
                    }
                    tx1 = wrapX(tx1);
                    ty1 = wrapY(ty1);

                    double minD2 = dist2(tx1, ty1, headX, headY);
                    for (Sphere seg : bodyParts) {
                        double d2 = dist2(tx1, ty1, seg.getTranslateX(), seg.getTranslateY());
                        if (d2 < minD2) minD2 = d2;
                    }
                    if (minD2 > bestClear) { bestClear = minD2; fallback = d; }
                }
                bestDir1 = fallback;
            }
            if (bestDir1 != villain2Direction) {
                if (framesSinceVillain2Turn >= VILLAIN2_TURN_COOLDOWN_FRAMES || !pathSafeFromPlayer2(villain2Direction, 2)) {
                    villain2Direction = bestDir1;
                    framesSinceVillain2Turn = 0;
                } else {
                    framesSinceVillain2Turn++;
                }
            } else {
                framesSinceVillain2Turn++;
            }
        }
        switch (villain2Direction) {
            case 0 -> villain2X += villain2Speed;
            case 1 -> villain2Y += villain2Speed;
            case 2 -> villain2X -= villain2Speed;
            case 3 -> villain2Y -= villain2Speed;
        }
        double oldX1 = villain2X;
        double oldY1 = villain2Y;
        villain2X = wrapX(villain2X);
        villain2Y = wrapY(villain2Y);
        if (villain2X != oldX1 || villain2Y != oldY1) {
            if (!villain2History.isEmpty()) villain2History.removeLast();
            villain2History.addLast(new double[]{villain2X, villain2Y});
        }
        villain2Head.setTranslateX(villain2X);
        villain2Head.setTranslateY(villain2Y);
        villain2History.addLast(new double[]{villain2X, villain2Y});
        int stepFrames = Math.max(1, (int)(SEGMENT_SPACING / villain2Speed));
        int maxHistorySize = (villain2BodyParts.size() + 1) * stepFrames + 50;
        while (villain2History.size() > maxHistorySize) villain2History.pollFirst();
        int index = 1;
        double elapsedSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
        for (Sphere segment : villain2BodyParts) {
            double[] pos = posAtFromTail(villain2History, index * stepFrames,
                    new double[]{segment.getTranslateX(), segment.getTranslateY()});
            segment.setTranslateX(pos[0]);
            segment.setTranslateY(pos[1]);
            if (index == villain2BodyParts.size()) {
                double maxAmplitude = SEGMENT_SPACING / 2.0;
                double rippleAmplitude = Math.min(5 + villain2BodyParts.size() * 0.2, maxAmplitude);
                double rippleFrequency = 5;
                segment.setTranslateX(pos[0] + rippleAmplitude * Math.sin(elapsedSeconds * rippleFrequency));
            }
            index++;
        }
    }
    //==================================================  updateVillainAI 3 ============================================
    private void updateVillain3AI(Group gameElements) {
        if (!villain3Alive) return;
        Sphere targetFruit = null;
        if (goldenFruitVisible) {
            targetFruit = goldenFruit;
        } else {
            double best = Double.MAX_VALUE;
            for (Sphere fruit : fruits) {
                if (!gameElements.getChildren().contains(fruit)) continue;
                double dx = fruit.getTranslateX() - villain3X;
                double dy = fruit.getTranslateY() - villain3Y;
                double d2 = dx * dx + dy * dy;
                if (d2 < best) {
                    best = d2;
                    targetFruit = fruit;
                }
            }
        }
        if (targetFruit == null) {
            int steps = Math.max(5, (int)Math.ceil(SEGMENT_SPACING / Math.max(1.0, villain3Speed)));
            if (!pathSafeFromPlayer3(villain3Direction, steps)) {
                for (int d = 0; d < 4; d++) {
                    if (pathSafeFromPlayer3(d, steps)) {
                        villain3Direction = d;
                        break;
                    }
                }
            }
        } else {
            int[] dirs2 = {3, 2, 1, 0};
            double[] scores2 = new double[4];
            for (int i = 0; i < 4; i++) {
                double tx2 = villain3X, ty2 = villain3Y;
                switch (dirs2[i]) {
                    case 0 -> tx2 += villain3Speed;
                    case 1 -> ty2 += villain3Speed;
                    case 2 -> tx2 -= villain3Speed;
                    case 3 -> ty2 -= villain3Speed;
                }
                tx2 = wrapX(tx2);
                ty2 = wrapY(ty2);
                double dx = targetFruit.getTranslateX() - tx2;
                double dy = targetFruit.getTranslateY() - ty2;
                scores2[i] = Math.hypot(dx, dy);
                if (dirs2[i] != villain3Direction) scores2[i] += 2.0;
            }
            int bestDir2 = -1;
            double bestScore2 = Double.MAX_VALUE;
            int steps = Math.max(6, (int)Math.ceil(SEGMENT_SPACING / Math.max(1.0, villain3Speed)) + 2);
            for (int i = 0; i < 4; i++) {
                for (int d = 0; d < 4; d++) {
                    if (scores2[d] < bestScore2 && pathSafeFromPlayer3(dirs2[d], steps)) {
                        bestScore2 = scores2[d];
                        bestDir2 = dirs2[d];
                    }
                }
            }
            if (bestDir2 == -1) {
                double bestClear = -1;
                int fallback = villain3Direction;
                for (int d = 0; d < 4; d++) {
                    double tx2 = villain3X, ty2 = villain3Y;
                    switch (d) {
                        case 0 -> tx2 += villain3Speed;
                        case 1 -> ty2 += villain3Speed;
                        case 2 -> tx2 -= villain3Speed;
                        case 3 -> ty2 -= villain3Speed;
                    }
                    tx2 = wrapX(tx2);
                    ty2 = wrapY(ty2);
                    double minD2 = dist2(tx2, ty2, headX, headY);
                    for (Sphere seg : bodyParts) {
                        double d2 = dist2(tx2, ty2, seg.getTranslateX(), seg.getTranslateY());
                        if (d2 < minD2) minD2 = d2;
                    }
                    if (minD2 > bestClear) { bestClear = minD2; fallback = d; }
                }
                bestDir2 = fallback;
            }
            if (bestDir2 != villain3Direction) {
                if (framesSinceVillain3Turn >= VILLAIN3_TURN_COOLDOWN_FRAMES || !pathSafeFromPlayer3(villain3Direction, 2)) {
                    villain3Direction = bestDir2;
                    framesSinceVillain3Turn = 0;
                } else {
                    framesSinceVillain3Turn++;
                }
            } else {
                framesSinceVillain3Turn++;
            }
        }
        switch (villain3Direction) {
            case 0 -> villain3X += villain3Speed;
            case 1 -> villain3Y += villain3Speed;
            case 2 -> villain3X -= villain3Speed;
            case 3 -> villain3Y -= villain3Speed;
        }
        double oldX2 = villain3X;
        double oldY2 = villain3Y;
        villain3X = wrapX(villain3X);
        villain3Y = wrapY(villain3Y);
        if (villain3X != oldX2 || villain3Y != oldY2) {
            if (!villain3History.isEmpty()) villain3History.removeLast();
            villain3History.addLast(new double[]{villain3X, villain3Y});
        }
        villain3Head.setTranslateX(villain3X);
        villain3Head.setTranslateY(villain3Y);
        villain3History.addLast(new double[]{villain3X, villain3Y});
        int stepFrames = Math.max(1, (int)(SEGMENT_SPACING / villain3Speed));
        int maxHistorySize = (villain3BodyParts.size() + 1) * stepFrames + 50;
        while (villain3History.size() > maxHistorySize) villain3History.pollFirst();
        int index = 1;
        double elapsedSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
        for (Sphere segment : villain3BodyParts) {
            double[] pos = posAtFromTail(villain3History, index * stepFrames,
                    new double[]{segment.getTranslateX(), segment.getTranslateY()});
            segment.setTranslateX(pos[0]);
            segment.setTranslateY(pos[1]);
            if (index == villain3BodyParts.size()) {
                double maxAmplitude = SEGMENT_SPACING / 2.0;
                double rippleAmplitude = Math.min(5 + villain3BodyParts.size() * 0.2, maxAmplitude);
                double rippleFrequency = 5;
                segment.setTranslateX(pos[0] + rippleAmplitude * Math.sin(elapsedSeconds * rippleFrequency));
            }
            index++;
        }
    }
    public static void main(String[] args) {
        launch();
    }
}