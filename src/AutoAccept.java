import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;

public class AutoAccept {

    private static final int CHECK_INTERVAL_MS = 500;
    private static final int CLICK_COOLDOWN_MS = 3000;

    // Minimum sampled green pixels (screen is sampled every 4px) to confirm button
    private static final int MIN_GREEN_PIXELS = 300;

    // The accept button bounding box must be at least this wide/tall (in real pixels)
    // — keeps small green UI dots from triggering a click
    private static final int MIN_BUTTON_WIDTH = 100;
    private static final int MIN_BUTTON_HEIGHT = 30;

    // HSB thresholds calibrated from actual Dota 2 accept screen pixels
    // Button green: H≈0.37, S≈0.46–0.51, V≈0.54–0.60
    private static final float HUE_MIN = 0.28f;
    private static final float HUE_MAX = 0.42f;
    private static final float SAT_MIN = 0.40f;
    private static final float BRI_MIN = 0.40f;

    public static void main(String[] args) throws Exception {
        Robot robot = new Robot();
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

        System.out.println("Dota 2 Auto-Accept running. Press Ctrl+C to stop.");
        System.out.println("Screen resolution: " + screen.width + "x" + screen.height);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> System.out.println("\nStopped.")));

        while (true) {
            BufferedImage capture = robot.createScreenCapture(new Rectangle(screen));
            Point pos = findAcceptButton(capture);

            if (pos != null) {
                click(robot, pos);
                Thread.sleep(CLICK_COOLDOWN_MS);
            } else {
                Thread.sleep(CHECK_INTERVAL_MS);
            }
        }
    }

    static Point findAcceptButton(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();

        // Only scan the center of the screen — the accept button never appears at edges
        int x0 = w / 4, x1 = w * 3 / 4;
        int y0 = h / 4, y1 = h * 3 / 4;

        int minX = Integer.MAX_VALUE, maxX = 0;
        int minY = Integer.MAX_VALUE, maxY = 0;
        int count = 0;

        // Sample every 4th pixel for performance (~16x speedup)
        for (int y = y0; y < y1; y += 4) {
            for (int x = x0; x < x1; x += 4) {
                if (isAcceptGreen(img.getRGB(x, y))) {
                    count++;
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }

        int buttonW = maxX - minX;
        int buttonH = maxY - minY;

        if (count >= MIN_GREEN_PIXELS && buttonW >= MIN_BUTTON_WIDTH && buttonH >= MIN_BUTTON_HEIGHT) {
            return new Point((minX + maxX) / 2, (minY + maxY) / 2);
        }
        return null;
    }

    static boolean isAcceptGreen(int rgb) {
        float[] hsb = new float[3];
        Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, hsb);
        return hsb[0] >= HUE_MIN && hsb[0] <= HUE_MAX
                && hsb[1] >= SAT_MIN
                && hsb[2] >= BRI_MIN;
    }

    static void click(Robot robot, Point pos) {
        robot.mouseMove(pos.x, pos.y);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        System.out.println("[+] Accepted match at (" + pos.x + ", " + pos.y + ")");
    }
}
