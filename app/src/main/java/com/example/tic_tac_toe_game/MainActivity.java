package com.example.tic_tac_toe_game;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
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
    private final List<ObjectAnimator> activeAnimators = new ArrayList<>();
    private final int[][] winningCells = new int[3][2];
    private boolean gameEnded = false;

    private TextView statusText;
    private TextView modeDisplayText;
    private TextView playerXScoreText;
    private TextView playerOScoreText;
    private TextView drawScoreText;

    private final Handler aiHandler = new Handler();
    private Runnable aiRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupButtons();
        // Show mode selection at start
        setupGameMode();
    }

    private void initializeViews() {
        statusText = findViewById(R.id.statusText);
        modeDisplayText = findViewById(R.id.modeDisplayText);
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

        // Mode button allows changing game mode at any time
        Button modeButton = findViewById(R.id.modeButton);
        if (modeButton != null) {
            modeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    animateButton(v);
                    setupGameMode();
                }
            });
        }

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
                            if (modeDisplayText != null) modeDisplayText.setText("Mode: Play vs AI");
                        } else {
                            isAIEnabled = false;
                            if (modeDisplayText != null) modeDisplayText.setText("Mode: 2 Players");
                        }
                        // Reset everything when mode changes to keep scores relevant to the mode
                        playerXScore = 0;
                        playerOScore = 0;
                        drawScore = 0;
                        updateScoreDisplay();
                        resetGame();
                    }
                })
                .setCancelable(true)
                .show();
    }

    private void showDifficultyDialog() {
        if (!isAIEnabled) {
            Toast.makeText(this, "Enable 'Play vs AI' mode first", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select AI Difficulty")
                .setItems(new String[]{"Easy", "Medium", "Hard"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: difficulty = "Easy"; break;
                            case 1: difficulty = "Medium"; break;
                            case 2: difficulty = "Hard"; break;
                        }
                        Toast.makeText(MainActivity.this, "Difficulty set to " + difficulty, Toast.LENGTH_SHORT).show();
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
                isAITurn = !playerXTurn; // AI is turn O
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
        if (aiRunnable != null) aiHandler.removeCallbacks(aiRunnable);
        
        aiRunnable = new Runnable() {
            @Override
            public void run() {
                if (!gameEnded && isAIEnabled) {
                    int[] move = getBestMove();
                    onButtonClick(move[0], move[1]);
                }
            }
        };
        aiHandler.postDelayed(aiRunnable, 600);
    }

    private int[] getBestMove() {
        if (difficulty.equals("Easy")) return getRandomMove();
        if (difficulty.equals("Medium")) return (new Random().nextBoolean()) ? getMinimaxMove() : getRandomMove();
        return getMinimaxMove();
    }

    private int[] getRandomMove() {
        List<int[]> emptyCells = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (buttons[i][j].getText().toString().equals("")) {
                    emptyCells.add(new int[]{i, j});
                }
            }
        }
        if (emptyCells.isEmpty()) return new int[]{0,0};
        return emptyCells.get(new Random().nextInt(emptyCells.size()));
    }

    private int[] getMinimaxMove() {
        int bestScore = Integer.MIN_VALUE;
        int[] move = new int[]{-1, -1};
        String[][] board = getBoardState();

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j].equals("")) {
                    board[i][j] = "O";
                    int score = minimax(board, 0, false);
                    board[i][j] = "";
                    if (score > bestScore) {
                        bestScore = score;
                        move[0] = i;
                        move[1] = j;
                    }
                }
            }
        }
        return move;
    }

    private int minimax(String[][] board, int depth, boolean isMaximizing) {
        String result = checkWinner(board);
        if (result.equals("O")) return 10 - depth;
        if (result.equals("X")) return depth - 10;
        if (result.equals("draw")) return 0;

        if (isMaximizing) {
            int bestScore = Integer.MIN_VALUE;
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (board[i][j].equals("")) {
                        board[i][j] = "O";
                        bestScore = Math.max(bestScore, minimax(board, depth + 1, false));
                        board[i][j] = "";
                    }
                }
            }
            return bestScore;
        } else {
            int bestScore = Integer.MAX_VALUE;
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (board[i][j].equals("")) {
                        board[i][j] = "X";
                        bestScore = Math.min(bestScore, minimax(board, depth + 1, true));
                        board[i][j] = "";
                    }
                }
            }
            return bestScore;
        }
    }

    private String checkWinner(String[][] board) {
        for (int i = 0; i < 3; i++) {
            if (!board[i][0].equals("") && board[i][0].equals(board[i][1]) && board[i][0].equals(board[i][2])) return board[i][0];
            if (!board[0][i].equals("") && board[0][i].equals(board[1][i]) && board[0][i].equals(board[2][i])) return board[0][i];
        }
        if (!board[0][0].equals("") && board[0][0].equals(board[1][1]) && board[0][0].equals(board[2][2])) return board[0][0];
        if (!board[0][2].equals("") && board[0][2].equals(board[1][1]) && board[0][2].equals(board[2][0])) return board[0][2];

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j].equals("")) return "pending";
            }
        }
        return "draw";
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
            animateWinningCell(buttons[winningCells[i][0]][winningCells[i][1]]);
        }
    }

    private void animateWinningCell(View view) {
        ObjectAnimator colorAnim = ObjectAnimator.ofInt(view, "backgroundColor",
                Color.parseColor("#16213E"), Color.parseColor("#00FF00"), Color.parseColor("#16213E"));
        colorAnim.setDuration(800);
        colorAnim.setEvaluator(new ArgbEvaluator());
        colorAnim.setRepeatCount(ValueAnimator.INFINITE);
        colorAnim.setRepeatMode(ValueAnimator.REVERSE);
        colorAnim.start();
        activeAnimators.add(colorAnim);
    }

    private void animateCellClick(View view) {
        ScaleAnimation anim = new ScaleAnimation(1f, 1.2f, 1f, 1.2f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        anim.setDuration(200);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(1);
        view.startAnimation(anim);
    }

    private void animateButton(View view) {
        ScaleAnimation anim = new ScaleAnimation(1f, 0.9f, 1f, 0.9f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        anim.setDuration(100);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(1);
        view.startAnimation(anim);
    }

    private void showGameHistory() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Match Results History");

        if (gameHistory.isEmpty()) {
            builder.setMessage("No matches completed yet.");
        } else {
            StringBuilder historyText = new StringBuilder();
            for (String result : gameHistory) {
                historyText.append(result).append("\n");
            }
            builder.setMessage(historyText.toString());
        }

        builder.setPositiveButton("OK", null)
                .show();
    }

    private boolean checkForWin() {
        String[][] b = getBoardState();
        for (int i = 0; i < 3; i++) {
            if (!b[i][0].equals("") && b[i][0].equals(b[i][1]) && b[i][0].equals(b[i][2])) {
                setWinningCells(i, 0, i, 1, i, 2); return true;
            }
            if (!b[0][i].equals("") && b[0][i].equals(b[1][i]) && b[0][i].equals(b[2][i])) {
                setWinningCells(0, i, 1, i, 2, i); return true;
            }
        }
        if (!b[0][0].equals("") && b[0][0].equals(b[1][1]) && b[0][0].equals(b[2][2])) {
            setWinningCells(0, 0, 1, 1, 2, 2); return true;
        }
        if (!b[0][2].equals("") && b[0][2].equals(b[1][1]) && b[0][2].equals(b[2][0])) {
            setWinningCells(0, 2, 1, 1, 2, 0); return true;
        }
        return false;
    }

    private void setWinningCells(int r1, int c1, int r2, int c2, int r3, int c3) {
        winningCells[0][0] = r1; winningCells[0][1] = c1;
        winningCells[1][0] = r2; winningCells[1][1] = c2;
        winningCells[2][0] = r3; winningCells[2][1] = c3;
    }

    private void playerXWins() {
        playerXScore++;
        updateScoreDisplay();
        statusText.setText("🎉 Player X Wins! 🎉");
        gameHistory.add("Match " + (gameHistory.size() + 1) + ": Player X Won");
    }

    private void playerOWins() {
        playerOScore++;
        updateScoreDisplay();
        String winner = isAIEnabled ? "AI Won" : "Player O Won";
        statusText.setText("🎉 " + winner + "! 🎉");
        gameHistory.add("Match " + (gameHistory.size() + 1) + ": " + winner);
    }

    private void draw() {
        drawScore++;
        updateScoreDisplay();
        statusText.setText("🤝 It's a Draw! 🤝");
        gameHistory.add("Match " + (gameHistory.size() + 1) + ": Draw");
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
            statusText.setText(isAIEnabled ? "AI is thinking..." : "Player O's Turn");
        }
    }

    private void resetGame() {
        // Cancel pending AI moves
        if (aiRunnable != null) aiHandler.removeCallbacks(aiRunnable);

        // Stop all active winning animations
        for (ObjectAnimator animator : activeAnimators) {
            animator.cancel();
        }
        activeAnimators.clear();

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                buttons[i][j].setText("");
                buttons[i][j].setEnabled(true);
                buttons[i][j].clearAnimation();
                // Reset background color to default
                buttons[i][j].setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#16213E")));
                buttons[i][j].setBackgroundColor(Color.parseColor("#16213E"));
            }
        }

        roundCount = 0;
        playerXTurn = true;
        isAITurn = false;
        gameEnded = false;
        updateStatusText();
    }
}
