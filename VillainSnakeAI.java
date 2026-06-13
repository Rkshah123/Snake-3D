import java.util.*;

public class VillainSnakeAI {
    static class Point {
        public int x, y;
        public Point parent;
        public Point(int x, int y, Point parent) {
            this.x = x;
            this.y = y;
            this.parent = parent;
        }
    }

    private static final int[][] DIRS = {{1,0},{-1,0},{0,1},{0,-1}}; // right, left, down, up

    public static Point getNextMove(Point villainHead, Point fruit, int rows, int cols, boolean[][] obstacles) {
        Queue<Point> q = new LinkedList<>();
        boolean[][] visited = new boolean[rows][cols];

        q.add(new Point(villainHead.x, villainHead.y, null));
        visited[villainHead.x][villainHead.y] = true;

        Point target = null;

        while (!q.isEmpty()) {
            Point curr = q.poll();
            if (curr.x == fruit.x && curr.y == fruit.y) {
                target = curr;
                break;
            }

            for (int[] d : DIRS) {
                int nx = curr.x + d[0], ny = curr.y + d[1];
                if (nx >= 0 && ny >= 0 && nx < cols && ny < rows &&
                        !visited[nx][ny] && !obstacles[ny][nx]) {
                    visited[nx][ny] = true;
                    q.add(new Point(nx, ny, curr));
                }
            }
        }

        if (target != null) {
            Point move = target;
            while (move.parent != null && move.parent.parent != null) {
                move = move.parent;
            }
            return move;
        }

        // Fallback: any safe move
        for (int[] d : DIRS) {
            int nx = villainHead.x + d[0], ny = villainHead.y + d[1];
            if (nx >= 0 && ny >= 0 && nx < cols && ny < rows && !obstacles[ny][nx]) {
                return new Point(nx, ny, null);
            }
        }

        return villainHead; // trapped
    }
}
