package com.example.tp1_mathquiz;

import android.animation.ObjectAnimator;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    // ==================== VARIABLES ====================
    private TextView tvNumber1, tvNumber2, tvResult, tvOperationSymbol, tvScore, tvHistory;
    private EditText etUserGuess;
    private Button btnAdd, btnSubtract, btnMultiply, btnGenerate, btnResetScore, btnValidate;
    private Spinner spinnerDifficulty;

    private int number1, number2;
    private int currentScore = 0;

    // NEW: We need to remember which operation the user selected
    private Character selectedOperation = null; // null means no operation selected yet

    // Constants
    private static final String PREFS_NAME = "MathQuizPrefs";
    private static final String KEY_SCORE = "score";
    private static final int MAX_HISTORY_SIZE = 5;

    // Data structures
    private ArrayList<String> history;
    private Random random;
    private SharedPreferences prefs;

    // Difficulty Levels
    private enum Difficulty {
        EASY(11, 99),
        MEDIUM(111, 999),
        HARD(1111, 9999);
        final int min, max;
        Difficulty(int min, int max) { this.min = min; this.max = max; }
    }
    private Difficulty currentDifficulty = Difficulty.MEDIUM;

    // ==================== LIFECYCLE ====================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeComponents();
        loadScore();
        setupListeners();
        generateNewExercise();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveScore();
    }

    // ==================== SETUP ====================
    private void initializeComponents() {
        tvNumber1 = findViewById(R.id.tvNumber1);
        tvNumber2 = findViewById(R.id.tvNumber2);
        tvResult = findViewById(R.id.tvResult);
        tvOperationSymbol = findViewById(R.id.tvOperationSymbol);
        tvScore = findViewById(R.id.tvScore);
        tvHistory = findViewById(R.id.tvHistory);
        etUserGuess = findViewById(R.id.etUserGuess);

        btnAdd = findViewById(R.id.btnAdd);
        btnSubtract = findViewById(R.id.btnSubtract);
        btnMultiply = findViewById(R.id.btnMultiply);
        btnGenerate = findViewById(R.id.btnGenerate);
        btnResetScore = findViewById(R.id.btnResetScore);
        btnValidate = findViewById(R.id.btnValidate);

        spinnerDifficulty = findViewById(R.id.spinnerDifficulty);

        random = new Random();
        history = new ArrayList<>();
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        setupDifficultySpinner();
    }

    private void setupDifficultySpinner() {
        String[] difficulties = {
                getString(R.string.difficulty_easy),
                getString(R.string.difficulty_medium),
                getString(R.string.difficulty_hard)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, difficulties);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDifficulty.setAdapter(adapter);
        spinnerDifficulty.setSelection(1);
    }

    private void setupListeners() {
        // STEP 1: Select Operation
        // These buttons ONLY set the operation, they do not check the answer yet.
        btnAdd.setOnClickListener(v -> selectOperation('+'));
        btnSubtract.setOnClickListener(v -> selectOperation('-'));
        btnMultiply.setOnClickListener(v -> selectOperation('×'));

        // STEP 2: Validate Answer
        btnValidate.setOnClickListener(v -> checkAnswer());

        btnGenerate.setOnClickListener(v -> {
            generateNewExercise();
            showToast(getString(R.string.toast_new_exercise));
        });

        btnResetScore.setOnClickListener(v -> resetScore());

        spinnerDifficulty.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentDifficulty = Difficulty.values()[position];
                generateNewExercise();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // ==================== CORE LOGIC ====================
    private void generateNewExercise() {
        number1 = random.nextInt(currentDifficulty.max - currentDifficulty.min + 1) + currentDifficulty.min;
        number2 = random.nextInt(currentDifficulty.max - currentDifficulty.min + 1) + currentDifficulty.min;

        tvNumber1.setText(String.valueOf(number1));
        tvNumber2.setText(String.valueOf(number2));

        // RESET State
        selectedOperation = null; // No operation selected
        tvOperationSymbol.setText("?");
        tvResult.setText(getString(R.string.tv_result_placeholder));
        tvResult.setTextColor(ContextCompat.getColor(this, R.color.text_primary));

        // CLEAR input
        etUserGuess.setText("");
        etUserGuess.setError(null);

        animateView(tvNumber1);
        animateView(tvNumber2);
    }

    // Called when user clicks +, -, or x
    private void selectOperation(char operation) {
        selectedOperation = operation;
        tvOperationSymbol.setText(String.valueOf(operation));

        // Optional: clear any previous error if they change operation
        if (tvResult.getCurrentTextColor() == ContextCompat.getColor(this, R.color.wrong_red)) {
            tvResult.setText(getString(R.string.tv_result_placeholder));
            tvResult.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        }
    }

    // Called when user clicks the Checkmark button
    private void checkAnswer() {
        // 1. Did they select an operation?
        if (selectedOperation == null) {
            showToast("Veuillez d'abord sélectionner une opération (+, -, ×)");
            return;
        }

        // 2. Did they enter a guess?
        String userGuessStr = etUserGuess.getText().toString();
        if (userGuessStr.isEmpty()) {
            showToast("Veuillez entrer votre réponse !");
            etUserGuess.setError("Réponse requise");
            return;
        }

        // 3. Calculate based on the SELECTED operation
        int realResult = 0;
        String operationStr = "";

        switch (selectedOperation) {
            case '+': realResult = number1 + number2; operationStr = "+"; break;
            case '-': realResult = number1 - number2; operationStr = "-"; break;
            case '×': realResult = number1 * number2; operationStr = "×"; break;
        }

        // 4. Validate
        try {
            int userGuess = Integer.parseInt(userGuessStr);
            if (userGuess == realResult) {
                // Correct
                updateScore(10);
                showToast("Bravo ! Bonne réponse !");
                tvResult.setTextColor(ContextCompat.getColor(this, R.color.correct_green));
                tvResult.setText("Correct ! (" + realResult + ")");
            } else {
                // Wrong
                showToast("Raté !");
                tvResult.setTextColor(ContextCompat.getColor(this, R.color.wrong_red));
                tvResult.setText("Raté ! Réponse : " + realResult);
            }
        } catch (NumberFormatException e) {
            showToast("Format invalide");
            return;
        }

        etUserGuess.setError(null);
        animateView(tvResult);
        addToHistory(number1, operationStr, number2, realResult);
    }

    // ==================== UTILS ====================
    private void updateScore(int points) {
        currentScore += points;
        tvScore.setText(String.valueOf(currentScore));
        animateView(tvScore);
    }

    private void addToHistory(int n1, String op, int n2, int res) {
        String entry = String.format("%d %s %d = %d", n1, op, n2, res);
        history.add(0, entry);
        if (history.size() > MAX_HISTORY_SIZE) history.remove(MAX_HISTORY_SIZE);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < history.size(); i++) {
            sb.append(i + 1).append(". ").append(history.get(i)).append("\n");
        }
        tvHistory.setText(sb.toString().trim());
    }

    private void resetScore() {
        currentScore = 0;
        tvScore.setText("0");
        history.clear();
        tvHistory.setText(getString(R.string.history_empty));
        saveScore();
        showToast(getString(R.string.toast_score_reset));

        // Also reset current game
        generateNewExercise();
    }

    private void saveScore() {
        prefs.edit().putInt(KEY_SCORE, currentScore).apply();
    }

    private void loadScore() {
        currentScore = prefs.getInt(KEY_SCORE, 0);
        tvScore.setText(String.valueOf(currentScore));
    }

    private void animateView(View view) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0.8f, 1.1f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0.8f, 1.1f, 1f);
        scaleX.setDuration(400); scaleY.setDuration(400);
        scaleX.setInterpolator(new BounceInterpolator()); scaleY.setInterpolator(new BounceInterpolator());
        scaleX.start(); scaleY.start();
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}