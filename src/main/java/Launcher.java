/**
 * Standalone launcher for the packaged (jpackage) desktop build.
 * Java's launcher refuses to start an Application subclass when JavaFX jars are on the
 * classpath, so this small non-JavaFX entry point starts the game instead.
 */
public final class Launcher {
    private Launcher() {
    }

    public static void main(String[] args) {
        Main.main(args);
    }
}
