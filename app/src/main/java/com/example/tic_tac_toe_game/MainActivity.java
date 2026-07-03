package com.example.tic_tac_toe_game;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private final Button[][] buttons = new Button[3][3];
    private boolean playerXTurn = true;
    private boolean isAIEnabled = false;
    private boolean isAITurn = false;
    private int roundCount = 0;
    private int playerXScore = 0;
    private int playerOScore = 0;
    private int drawScore = 0;
    private String difficulty = "Hard";
    private final List<String> gameHistory = new ArrayList<>();
    private final int[][] winningCells = new int[3][2];
    private boolean gameEnded = false;

    private TextView statusText;
    private TextView playerXScoreText;
    private TextView playerOScoreText;
    private TextView drawScoreText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupButtons();
        setupGameMode();
    }

    private void initializeViews() {
        statusText = findViewById(R.id.statusText);
        playerXScoreText = findViewById(R.id.playerXScore);
        playerOScoreText = findViewById(R.id.playerOScore);
        drawScoreText = findViewById(R.id.drawScore);

        // Initialize button array
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                String buttonID = "button_" + i + j;
                int resID = getResources().getIdentifier(buttonID, "id", getPackageName());
                buttons[i][j] = findViewById(resID);
            }
        }

        Button resetButton = findViewById(R.id.resetButton);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateButton(v);
                resetGame();
            }
        });

        Button aiButton = findViewById(R.id.aiButton);
        aiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateButton(v);
                showDifficultyDialog();
            }
        });

        Button historyButton = findViewById(R.id.historyButton);
        historyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateButton(v);
                showGameHistory();
            }
        });
    }

    private void setupButtons() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                final int row = i;
                final int col = j;
                buttons[i][j].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!gameEnded && (!isAIEnabled || !isAITurn)) {
                            onButtonClick(row, col);
                        }
                    }
                });
            }
        }
    }

    private void setupGameMode() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Game Mode")
                .setItems(new String[]{"2 Players", "Play vs AI"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 1) {
                            isAIEnabled = true;
                            statusText.setText("You are X, AI is O");
                        } else {
                            isAIEnabled = false;
                            statusText.setText("Player X's Turn");
                        }
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void showDifficultyDialog() {
        if (!isAIEnabled) {
            Toast.makeText(this, "Select 'Play vs AI' mode first", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Difficulty")
                .setItems(new String[]{"Easy (Random)", "Medium (Mixed)", "Hard (Unbeatable)"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                difficulty = "Easy";
                                break;
                            case 1:
                                difficulty = "Medium";
                                break;
                            case 2:
                                difficulty = "Hard";
                                break;
                        }
                        Toast.makeText(MainActivity.this, "Difficulty: " + difficulty, Toast.LENGTH_SHORT).show();
                        resetGame();
                    }
                })
                .show();
    }

    private void onButtonClick(int row, int col) {
        if (!buttons[row][col].getText().toString().equals("")) {
            return;
        }

        String currentPlayer = playerXTurn ? "X" : "O";
        buttons[row][col].setText(currentPlayer);
        animateCellClick(buttons[row][col]);

        gameHistory.add("Player " + currentPlayer + " placed at (" + row + "," + col + ")");
        roundCount++;

        if (checkForWin()) {
            gameEnded = true;
            if (playerXTurn) {
                playerXWins();
            } else {
                playerOWins();
            }
            highlightWinningCells();
        } else if (roundCount == 9) {
            gameEnded = true;
            draw();
        } else {
            playerXTurn = !playerXTurn;
            if (isAIEnabled) {
                isAITurn = !isAITurn;
                updateStatusText();
                if (isAITurn) {
                    makeAIMove();
                }
            } else {
                updateStatusText();
            }
        }
    }

    private void makeAIMove() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!gameEnded) {
                    int[] bestMove = getBestMove();
                    onButtonClick(bestMove[0], bestMove[1]);
                }
            }
        }, 500);
    }

    private int[] getBestMove() {
        switch (difficulty) {
            case "Easy":
                return getRandomMove();
            case "Medium":
                return getMediumMove();
            case "Hard":
                return getMinimaxMove();
            default:
                return getRandomMove();
        }
    }

    private int[] getRandomMove() {
        List<int[]> availableMoves = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (buttons[i][j].getText().toString().equals("")) {
                    availableMoves.add(new int[]{i, j});
                }
            }
        }

        if (availableMoves.isEmpty()) return new int[]{0, 0};

        Random rand = new Random();
        return availableMoves.get(rand.nextInt(availableMoves.size()));
    }

    private int[] getMediumMove() {
        Random rand = new Random();
        if (rand.nextBoolean()) {
            return getMinimaxMove();
        } else {
            return getRandomMove();
        }
    }

    private int[] getMinimaxMove() {
        int bestScore = Integer.MIN_VALUE;
        int[] bestMove = new int[]{0, 0};
        String[][] board = getBoardState();

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j].equals("")) {
                    board[i][j] = "O";
                    int score = minimax(board, 0, false);
                    board[i][j] = "";
                    if (score > bestScore) {
                        bestScore = score;
                        bestMove[0] = i;
                        bestMove[1] = j;
                    }
                }
            }
        }
        return bestMove;
    }

    private int minimax(String[][] board, int depth, boolean isMaximizing) {
        String result = checkWinner(board);
        if (result.equals("O")) return 10 - depth;
        if (result.equals("X")) return depth - 10;
        if (result.equals("draw")) return 0;

        int bestScore;
        if (isMaximizing) {
            bestScore = Integer.MIN_VALUE;
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (board[i][j].equals("")) {
                        board[i][j] = "O";
                        int score = minimax(board, depth + 1, false);
                        board[i][j] = "";
                        bestScore = Math.max(score, bestScore);
                    }
                }
            }
        } else {
            bestScore = Integer.MAX_VALUE;
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (board[i][j].equals("")) {
                        board[i][j] = "X";
                        int score = minimax(board, depth + 1, true);
                        board[i][j] = "";
                        bestScore = Math.min(score, bestScore);
                    }
                }
            }
        }
        return bestScore;
    }

    private String checkWinner(String[][] board) {
        for (int i = 0; i < 3; i++) {
            if (board[i][0].equals(board[i][1]) && board[i][0].equals(board[i][2]) && !board[i][0].equals(""))
                return board[i][0];
            if (board[0][i].equals(board[1][i]) && board[0][i].equals(board[2][i]) && !board[0][i].equals(""))
                return board[0][i];
        }

        if (board[0][0].equals(board[1][1]) && board[0][0].equals(board[2][2]) && !board[0][0].equals(""))
            return board[0][0];
        if (board[0][2].equals(board[1][1]) && board[0][2].equals(board[2][0]) && !board[0][2].equals(""))
            return board[0][2];

        boolean isDraw = true;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j].equals("")) {
                    isDraw = false;
                    break;
                }
            }
        }

        return isDraw ? "draw" : "";
    }

    private String[][] getBoardState() {
        String[][] board = new String[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = buttons[i][j].getText().toString();
            }
        }
        return board;
    }

    private void highlightWinningCells() {
        for (int i = 0; i < 3; i++) {
            int row = winningCells[i][0];
            int col = winningCells[i][1];
            animateWinningCell(buttons[row][col]);
        }
    }

    private void animateWinningCell(View view) {
        ObjectAnimator colorAnim = ObjectAnimator.ofInt(view, "backgroundColor",
                Color.parseColor("#16213E"), Color.parseColor("#00FF00"), Color.parseColor("#16213E"));
        colorAnim.setDuration(1000);
        colorAnim.setEvaluator(new ArgbEvaluator());
        colorAnim.setRepeatCount(ValueAnimator.INFINITE);
        colorAnim.setRepeatMode(ValueAnimator.REVERSE);
        colorAnim.start();
    }

    private void animateCellClick(View view) {
        ScaleAnimation scaleAnim = new ScaleAnimation(
                1f, 1.2f, 1f, 1.2f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        scaleAnim.setDuration(200);
        scaleAnim.setRepeatMode(Animation.REVERSE);
        scaleAnim.setRepeatCount(1);
        view.startAnimation(scaleAnim);
    }

    private void animateButton(View view) {
        ScaleAnimation scaleAnim = new ScaleAnimation(
                1f, 0.9f, 1f, 0.9f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        scaleAnim.setDuration(100);
        scaleAnim.setRepeatMode(Animation.REVERSE);
        scaleAnim.setRepeatCount(1);
        view.startAnimation(scaleAnim);
    }

    private void showGameHistory() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Game History");

        if (gameHistory.isEmpty()) {
            builder.setMessage("No moves yet!");
        } else {
            StringBuilder history = new StringBuilder();
            for (String move : gameHistory) {
                history.append(move).append("\n");
            }
            builder.setMessage(history.toString());
        }

        builder.setPositiveButton("OK", null)
                .show();
    }

    private boolean checkForWin() {
        String[][] field = getBoardState();

        for (int i = 0; i < 3; i++) {
            if (field[i][0].equals(field[i][1]) &&
                    field[i][0].equals(field[i][2]) &&
                    !field[i][0].equals("")) {
                winningCells[0] = new int[]{i, 0};
                winningCells[1] = new int[]{i, 1};
                winningCells[2] = new int[]{i, 2};
                return true;
            }
        }

        for (int i = 0; i < 3; i++) {
            if (field[0][i].equals(field[1][i]) &&
                    field[0][i].equals(field[2][i]) &&
                    !field[0][i].equals("")) {
                winningCells[0] = new int[]{0, i};
                winningCells[1] = new int[]{1, i};
                winningCells[2] = new int[]{2, i};
                return true;
            }
        }

        if (field[0][0].equals(field[1][1]) &&
                field[0][0].equals(field[2][2]) &&
                !field[0][0].equals("")) {
            winningCells[0] = new int[]{0, 0};
            winningCells[1] = new int[]{1, 1};
            winningCells[2] = new int[]{2, 2};
            return true;
        }

        if (field[0][2].equals(field[1][1]) &&
                field[0][2].equals(field[2][0]) &&
                !field[0][2].equals("")) {
            winningCells[0] = new int[]{0, 2};
            winningCells[1] = new int[]{1, 1};
            winningCells[2] = new int[]{2, 0};
            return true;
        }

        return false;
    }

    private void playerXWins() {
        playerXScore++;
        updateScoreDisplay();
        statusText.setText("🎉 Player X Wins! 🎉");
        gameHistory.add("Player X wins!");
    }

    private void playerOWins() {
        playerOScore++;
        updateScoreDisplay();
        String winner = isAIEnabled ? "AI Wins!" : "Player O Wins!";
        statusText.setText("🎉 " + winner + " 🎉");
        gameHistory.add(winner);
    }

    private void draw() {
        drawScore++;
        updateScoreDisplay();
        statusText.setText("🤝 It's a Draw! 🤝");
        gameHistory.add("Game ended in a draw");
    }

    private void updateScoreDisplay() {
        playerXScoreText.setText("Player X: " + playerXScore);
        playerOScoreText.setText("Player O: " + playerOScore);
        drawScoreText.setText("Draws: " + drawScore);
    }

    private void updateStatusText() {
        if (playerXTurn) {
            statusText.setText(isAIEnabled ? "Your Turn (X)" : "Player X's Turn");
        } else {
            statusText.setText(isAIEnabled ? "AI Thinking..." : "Player O's Turn");
        }
    }

    private void resetGame() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                buttons[i][j].setText("");
                buttons[i][j].setEnabled(true);
                buttons[i][j].clearAnimation();
            }
        }

        roundCount = 0;
        playerXTurn = true;
        isAITurn = false;
        gameEnded = false;
        gameHistory.clear();
        updateStatusText();
    }
}