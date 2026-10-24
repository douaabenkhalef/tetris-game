package com.tetris;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.Random;

public class TetrisGame extends Application {
    private static final int BOARD_WIDTH = 10;
    private static final int BOARD_HEIGHT = 20;
    private static final int CELL_SIZE = 30;
    
    private int[][] board = new int[BOARD_HEIGHT][BOARD_WIDTH];
    private Tetromino currentPiece;
    private Tetromino heldPiece = null;
    private Tetromino nextPiece;
    private boolean canHold = true;
    private int currentX, currentY;
    private int score = 0;
    private int level = 1;
    private int linesTotal = 0;
    private boolean gameRunning = true;
    private boolean gamePaused = false;
    private Random random = new Random();
    private long lastUpdate = 0;
    private long moveDelay;
    
    private Canvas gameCanvas;
    private Canvas previewCanvas;
    private Canvas holdCanvas;
    private Label scoreLabel;
    private Label levelLabel;
    private Label linesLabel;
    private Button pauseButton, restartButton, exitButton;
    private Button muteButton;
    private boolean muted = false;
    private Scene scene;
    
    private static final int[][][] TETROMINOS = {
        {{1, 1, 1, 1}},
        {{1, 1}, {1, 1}},
        {{0, 1, 0}, {1, 1, 1}},
        {{1, 0, 0}, {1, 1, 1}},
        {{0, 0, 1}, {1, 1, 1}},
        {{0, 1, 1}, {1, 1, 0}},
        {{1, 1, 0}, {0, 1, 1}}
    };
    
    private static final Color[] COLORS = {
        Color.TRANSPARENT,
        Color.CYAN, Color.YELLOW, Color.PURPLE,
        Color.ORANGE, Color.BLUE, Color.LIME, Color.RED
    };
    
    class Tetromino {
        int[][] shape;
        int colorIndex;
        
        Tetromino(int[][] shape, int colorIndex) {
            this.shape = shape;
            this.colorIndex = colorIndex;
        }
        
        Tetromino copy() {
            int[][] newShape = new int[shape.length][shape[0].length];
            for (int i = 0; i < shape.length; i++) {
                System.arraycopy(shape[i], 0, newShape[i], 0, shape[0].length);
            }
            return new Tetromino(newShape, colorIndex);
        }
        
        int getWidth() { return shape[0].length; }
        int getHeight() { return shape.length; }
    }
    
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Tetris Deluxe");
        
        BorderPane mainLayout = new BorderPane();
        mainLayout.setStyle("-fx-background-color: #0a0a1a;");
        
        gameCanvas = new Canvas(BOARD_WIDTH * CELL_SIZE, BOARD_HEIGHT * CELL_SIZE);
        gameCanvas.setStyle("-fx-border-color: #ff6b35; -fx-border-width: 3px;");
        previewCanvas = new Canvas(120, 120);
        holdCanvas = new Canvas(120, 120);
        
        VBox rightPanel = new VBox(15);
        rightPanel.setAlignment(Pos.TOP_CENTER);
        rightPanel.setPadding(new Insets(20));
        rightPanel.setStyle("-fx-background-color: #1a1a2e;");
        
        Label scoreTitle = new Label("🏆 SCORE");
        scoreTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        scoreTitle.setTextFill(Color.GOLD);
        scoreLabel = new Label("0");
        scoreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        scoreLabel.setTextFill(Color.YELLOW);
        
        Label levelTitle = new Label("⭐ LEVEL");
        levelTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        levelTitle.setTextFill(Color.AQUA);
        levelLabel = new Label("1");
        levelLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        levelLabel.setTextFill(Color.CYAN);
        
        Label linesTitle = new Label(" LINES");
        linesTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        linesTitle.setTextFill(Color.LIME);
        linesLabel = new Label("0");
        linesLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        linesLabel.setTextFill(Color.LIMEGREEN);
        
        Label holdTitle = new Label(" HOLD (C)");
        holdTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        holdTitle.setTextFill(Color.ORANGE);
        
        Label nextTitle = new Label(" NEXT");
        nextTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        nextTitle.setTextFill(Color.MAGENTA);
        
        pauseButton = new Button("⏸️ PAUSE");
        restartButton = new Button("🔄 RESTART");
        exitButton = new Button("❌ EXIT");
        muteButton = new Button("🔊 MUTE");
        
        String btnStyle = "-fx-background-color: linear-gradient(#ff6b35, #e94560); -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-min-width: 120px; -fx-padding: 10px; -fx-background-radius: 25px;";
        pauseButton.setStyle(btnStyle);
        restartButton.setStyle(btnStyle);
        exitButton.setStyle(btnStyle);
        muteButton.setStyle(btnStyle);
        
        pauseButton.setOnAction(e -> togglePause());
        restartButton.setOnAction(e -> restartGame());
        exitButton.setOnAction(e -> primaryStage.close());
        muteButton.setOnAction(e -> toggleMute());
        
        rightPanel.getChildren().addAll(
            scoreTitle, scoreLabel,
            levelTitle, levelLabel,
            linesTitle, linesLabel,
            holdTitle, holdCanvas,
            nextTitle, previewCanvas,
            pauseButton, restartButton, exitButton, muteButton
        );
        
        mainLayout.setCenter(gameCanvas);
        mainLayout.setRight(rightPanel);
        
        scene = new Scene(mainLayout);
        scene.setOnKeyPressed(this::handleKeyPress);
        scene.setOnMouseClicked(e -> scene.getRoot().requestFocus());
        
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
        
        scene.getRoot().requestFocus();
        initGame();
    }
    
    private void initGame() {
        nextPiece = getRandomPiece();
        spawnNewPiece();
        updateSpeed();
        startGameLoop();
    }
    
    private void startGameLoop() {
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!gameRunning || gamePaused) return;
                if (lastUpdate == 0) {
                    lastUpdate = now;
                    return;
                }
                if (now - lastUpdate >= moveDelay * 1_000_000) {
                    movePieceDown();
                    lastUpdate = now;
                }
                draw();
            }
        }.start();
    }
    
    private Tetromino getRandomPiece() {
        int type = random.nextInt(TETROMINOS.length);
        return new Tetromino(TETROMINOS[type], type + 1);
    }
    
    private void spawnNewPiece() {
        if (nextPiece == null) {
            nextPiece = getRandomPiece();
        }
        currentPiece = nextPiece.copy();
        nextPiece = getRandomPiece();
        currentX = BOARD_WIDTH / 2 - currentPiece.getWidth() / 2;
        currentY = 0;
        canHold = true;
        
        if (collision()) {
            gameRunning = false;
            pauseButton.setText("💀 GAME OVER");
        }
        draw();
    }
    
    private void updateSpeed() {
        long baseDelay = 500;
        moveDelay = Math.max(80, baseDelay - (level - 1) * 30);
    }
    
    private void movePieceDown() {
        if (!gameRunning) return;
        currentY++;
        if (collision()) {
            currentY--;
            mergePiece();
            clearLines();
            spawnNewPiece();
        }
        draw();
    }
    
    private void movePieceLeft() {
        if (!gameRunning || gamePaused) return;
        currentX--;
        if (collision()) currentX++;
        draw();
    }
    
    private void movePieceRight() {
        if (!gameRunning || gamePaused) return;
        currentX++;
        if (collision()) currentX--;
        draw();
    }
    
    private void rotatePiece() {
        if (!gameRunning || gamePaused) return;
        int[][] rotated = new int[currentPiece.getWidth()][currentPiece.getHeight()];
        for (int i = 0; i < currentPiece.getHeight(); i++) {
            for (int j = 0; j < currentPiece.getWidth(); j++) {
                rotated[j][currentPiece.getHeight() - 1 - i] = currentPiece.shape[i][j];
            }
        }
        
        int oldX = currentX;
        int oldY = currentY;
        int[][] oldShape = currentPiece.shape;
        currentPiece.shape = rotated;
        
        if (currentX + currentPiece.getWidth() > BOARD_WIDTH) {
            currentX = BOARD_WIDTH - currentPiece.getWidth();
        }
        if (currentX < 0) currentX = 0;
        if (currentY + currentPiece.getHeight() > BOARD_HEIGHT) {
            currentY = BOARD_HEIGHT - currentPiece.getHeight();
        }
        
        if (collision()) {
            currentPiece.shape = oldShape;
            currentX = oldX;
            currentY = oldY;
        }
        draw();
    }
    
    private void holdPiece() {
        if (!gameRunning || gamePaused || !canHold) return;
        
        if (heldPiece == null) {
            heldPiece = currentPiece.copy();
            spawnNewPiece();
        } else {
            Tetromino temp = heldPiece.copy();
            heldPiece = currentPiece.copy();
            currentPiece = temp;
            currentX = BOARD_WIDTH / 2 - currentPiece.getWidth() / 2;
            currentY = 0;
            if (collision()) {
                currentPiece = temp;
                heldPiece = null;
            }
        }
        canHold = false;
        draw();
    }
    
    private void hardDrop() {
        if (!gameRunning || gamePaused) return;
        while (!collision()) {
            currentY++;
        }
        currentY--;
        mergePiece();
        clearLines();
        spawnNewPiece();
        draw();
    }
    
    private boolean collision() {
        for (int i = 0; i < currentPiece.getHeight(); i++) {
            for (int j = 0; j < currentPiece.getWidth(); j++) {
                if (currentPiece.shape[i][j] != 0) {
                    int x = currentX + j;
                    int y = currentY + i;
                    if (x < 0 || x >= BOARD_WIDTH || y >= BOARD_HEIGHT) return true;
                    if (y >= 0 && board[y][x] != 0) return true;
                }
            }
        }
        return false;
    }
    
    private void mergePiece() {
        for (int i = 0; i < currentPiece.getHeight(); i++) {
            for (int j = 0; j < currentPiece.getWidth(); j++) {
                if (currentPiece.shape[i][j] != 0) {
                    int x = currentX + j;
                    int y = currentY + i;
                    if (y >= 0 && y < BOARD_HEIGHT && x >= 0 && x < BOARD_WIDTH) {
                        board[y][x] = currentPiece.colorIndex;
                    }
                }
            }
        }
    }
    
    private void clearLines() {
        int linesCleared = 0;
        for (int y = BOARD_HEIGHT - 1; y >= 0; y--) {
            boolean full = true;
            for (int x = 0; x < BOARD_WIDTH; x++) {
                if (board[y][x] == 0) {
                    full = false;
                    break;
                }
            }
            if (full) {
                for (int row = y; row > 0; row--) {
                    board[row] = board[row - 1].clone();
                }
                board[0] = new int[BOARD_WIDTH];
                y++;
                linesCleared++;
            }
        }
        
        if (linesCleared > 0) {
            linesTotal += linesCleared;
            int points = switch (linesCleared) {
                case 1 -> 100 * level;
                case 2 -> 300 * level;
                case 3 -> 600 * level;
                case 4 -> 1000 * level;
                default -> 0;
            };
            score += points;
            
            int newLevel = (linesTotal / 10) + 1;
            if (newLevel > level) {
                level = newLevel;
                updateSpeed();
            }
            
            scoreLabel.setText(String.valueOf(score));
            levelLabel.setText(String.valueOf(level));
            linesLabel.setText(String.valueOf(linesTotal));
        }
    }
    
    private void draw() {
        GraphicsContext gc = gameCanvas.getGraphicsContext2D();
        
        gc.setFill(Color.rgb(10, 10, 26));
        gc.fillRect(0, 0, BOARD_WIDTH * CELL_SIZE, BOARD_HEIGHT * CELL_SIZE);
        
        for (int y = 0; y < BOARD_HEIGHT; y++) {
            for (int x = 0; x < BOARD_WIDTH; x++) {
                if (board[y][x] != 0) {
                    gc.setFill(COLORS[board[y][x]]);
                    gc.fillRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE - 1, CELL_SIZE - 1);
                }
                gc.setStroke(Color.rgb(30, 30, 46));
                gc.strokeRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            }
        }
        
        if (currentPiece != null) {
            gc.setFill(COLORS[currentPiece.colorIndex]);
            for (int i = 0; i < currentPiece.getHeight(); i++) {
                for (int j = 0; j < currentPiece.getWidth(); j++) {
                    if (currentPiece.shape[i][j] != 0) {
                        int x = (currentX + j) * CELL_SIZE;
                        int y = (currentY + i) * CELL_SIZE;
                        gc.fillRect(x, y, CELL_SIZE - 1, CELL_SIZE - 1);
                    }
                }
            }
        }
        
        drawPreview(previewCanvas, nextPiece);
        drawPreview(holdCanvas, heldPiece);
        
        if (!gameRunning) {
            gc.setFill(Color.rgb(0, 0, 0, 0.8));
            gc.fillRect(0, 0, BOARD_WIDTH * CELL_SIZE, BOARD_HEIGHT * CELL_SIZE);
            gc.setFill(Color.RED);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 24));
            gc.fillText("GAME OVER", BOARD_WIDTH * CELL_SIZE / 4, BOARD_HEIGHT * CELL_SIZE / 2);
        } else if (gamePaused) {
            gc.setFill(Color.rgb(0, 0, 0, 0.8));
            gc.fillRect(0, 0, BOARD_WIDTH * CELL_SIZE, BOARD_HEIGHT * CELL_SIZE);
            gc.setFill(Color.YELLOW);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 24));
            gc.fillText("PAUSED", BOARD_WIDTH * CELL_SIZE / 3, BOARD_HEIGHT * CELL_SIZE / 2);
        }
    }
    
    private void drawPreview(Canvas canvas, Tetromino piece) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.setFill(Color.rgb(30, 30, 46));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        
        if (piece != null) {
            int cellSize = 20;
            int startX = (int)(canvas.getWidth() - piece.getWidth() * cellSize) / 2;
            int startY = (int)(canvas.getHeight() - piece.getHeight() * cellSize) / 2;
            
            gc.setFill(COLORS[piece.colorIndex]);
            for (int i = 0; i < piece.getHeight(); i++) {
                for (int j = 0; j < piece.getWidth(); j++) {
                    if (piece.shape[i][j] != 0) {
                        gc.fillRect(startX + j * cellSize, startY + i * cellSize, cellSize - 1, cellSize - 1);
                    }
                }
            }
        }
    }
    
    private void handleKeyPress(KeyEvent event) {
        KeyCode code = event.getCode();
        
        if (!gameRunning) return;
        
        switch (code) {
            case P -> togglePause();
            case C -> holdPiece();
            case LEFT -> movePieceLeft();
            case RIGHT -> movePieceRight();
            case DOWN -> movePieceDown();
            case UP -> rotatePiece();
            case SPACE -> hardDrop();
            default -> {}
        }
    }
    
    private void togglePause() {
        if (!gameRunning) return;
        gamePaused = !gamePaused;
        pauseButton.setText(gamePaused ? "▶️ RESUME" : "⏸️ PAUSE");
        draw();
        scene.getRoot().requestFocus();
    }
    
    private void restartGame() {
        board = new int[BOARD_HEIGHT][BOARD_WIDTH];
        score = 0;
        level = 1;
        linesTotal = 0;
        heldPiece = null;
        nextPiece = null;
        gameRunning = true;
        gamePaused = false;
        canHold = true;
        updateSpeed();
        scoreLabel.setText("0");
        levelLabel.setText("1");
        linesLabel.setText("0");
        pauseButton.setText("⏸️ PAUSE");
        initGame();
        draw();
        scene.getRoot().requestFocus();
    }
    
    private void toggleMute() {
        muted = !muted;
        muteButton.setText(muted ? "🔇 MUTE" : "🔊 MUTE");
        scene.getRoot().requestFocus();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
