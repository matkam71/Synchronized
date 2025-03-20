package com.example.lista6;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

public class Watki extends Application {
    int n = 10;
    int m = 10;
    int max = Math.max(m, n);
    int k = 500;
    double p = 0.5;
    ColorThread watki[][] = new ColorThread[n][m];
    ReentrantLock locks[][] = new ReentrantLock[n][m];
    Random random = new Random();
    final Object stateLocker = new Object();

    @Override
    public void start(Stage primaryStage) {
        GridPane grid = new GridPane();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                locks[i][j] = new ReentrantLock();
                watki[i][j] = new ColorThread(i, j);
                grid.add(watki[i][j].getRectangle(), j, i);
                watki[i][j].start();
            }
        }
        Scene scene = new Scene(grid, (0.4 + 500 / max) * m, (0.4 + 500 / max) * n);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    final Object locker = new Object();

    private class ColorThread extends Thread {
        int x, y;
        Color color;
        boolean running = true;
        Rectangle rectangle;

        public ColorThread(int x, int y) {
            this.x = x;
            this.y = y;
            this.color = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256));
            this.rectangle = new Rectangle(500 / max, 500 / max);
            updateColor();
            this.rectangle.setOnMouseClicked(event -> {
                if (running) {
                    pauseThread();
                } else {
                    resumeThread();
                }
            });
        }

        @Override
        public void run() {
            try {
                while (true) {
                    Thread.sleep((long) ((0.5 + random.nextDouble()) * k));
                    if (!running) { // MyRectThread
                        synchronized (this) {
                            while (!running) {
                                wait();
                            }
                        }
                    }
                    synchronized (locker) {
                        synchronized (stateLocker) {
                            System.out.println("Start " + x + "," + y);
                            if (random.nextDouble() < p) {
                                changeColor(Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
                            } else {
                                Color neighbourColors[] = new Color[4];
                                int X[] = {1, -1, 0, 0};
                                int Y[] = {0, 0, 1, -1};
                                int ActiveNeighbours = 0;
                                for (int i = 0; i < 4; i++) {
                                    int neighbourX = (x + X[i] + n) % n;
                                    int neighbourY = (y + Y[i] + m) % m;
                                    synchronized (locks[neighbourX][neighbourY]) {
                                        if (locks[neighbourX][neighbourY].tryLock()) {
                                            neighbourColors[ActiveNeighbours] = watki[neighbourX][neighbourY].getColor();
                                            ActiveNeighbours++;
                                        }
                                        locks[neighbourX][neighbourY].unlock();
                                    }
                                }
                                if (ActiveNeighbours > 0) {
                                    changeColor(averageColor(neighbourColors, ActiveNeighbours));
                                }
                            }
                            updateColor();
                            System.out.println("End " + x + "," + y);
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private Color averageColor(Color neighbourColors[], int ActiveNeighbours) {
            double r = 0, g = 0, b = 0;
            for (int i = 0; i < ActiveNeighbours; i++) {
                r = r + neighbourColors[i].getRed();
                g = g + neighbourColors[i].getGreen();
                b = b + neighbourColors[i].getBlue();
            }
            r = r / ActiveNeighbours;
            g = g / ActiveNeighbours;
            b = b / ActiveNeighbours;
            return new Color(r, g, b, 1.0);
        }

        public void pauseThread() {
            synchronized (stateLocker) {
                running = false;
            }
        }

        public void resumeThread() {
            synchronized (stateLocker) {
                running = true;
                notify();
            }
        }

        public boolean isRunning() {
            synchronized (stateLocker) {
                return running;
            }
        }

        public Color getColor() {
            synchronized (locks[x][y]) {
                return color;
            }
        }

        public void changeColor(Color color) {
            synchronized (locks[x][y]) {
                this.color = color;
            }
        }

        private void updateColor() {
            Platform.runLater(() -> rectangle.setFill(color));
        }

        public Rectangle getRectangle() {
            return rectangle;
        }
    }
}
