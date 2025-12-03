package com.example.tp1_mathquiz;

import android.animation.ObjectAnimator;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tp1_mathquiz.R;

import java.util.ArrayList;
import java.util.Random;

/**
 * MainActivity - Application MathQuiz
 *
 * Application de quiz mathématique avec :
 * - Génération aléatoire de nombres
 * - Opérations arithmétiques (+, -, ×)
 * - Système de score avec sauvegarde
 * - Niveaux de difficulté (Facile/Moyen/Difficile)
 * - Historique des 5 dernières opérations
 * - Animations et feedback visuel
 *
 * @author ABOU-EL KASEM
 * @version 1.0
 */
public class MainActivity extends AppCompatActivity {

    // ==================== CONSTANTES ====================

    /** Clé pour la sauvegarde du score dans SharedPreferences */
    private static final String PREFS_NAME = "MathQuizPrefs";
    private static final String KEY_SCORE = "score";

    /** Nombre maximum d'éléments dans l'historique */
    private static final int MAX_HISTORY_SIZE = 5;

    /** Points gagnés par bonne réponse */
    private static final int POINTS_PER_CORRECT = 10;

    // ==================== ENUM POUR LES NIVEAUX ====================

    /**
     * Énumération des niveaux de difficulté
     * Chaque niveau définit les bornes min/max pour la génération de nombres
     */
    private enum Difficulty {
        EASY(11, 99),      // Facile : 11-99
        MEDIUM(111, 999),  // Moyen : 111-999
        HARD(1111, 9999);  // Difficile : 1111-9999

        final int min;
        final int max;

        Difficulty(int min, int max) {
            this.min = min;
            this.max = max;
        }
    }

    // ==================== VARIABLES D'INSTANCE ====================

    // Composants UI
    private TextView tvNumber1, tvNumber2, tvResult, tvOperationSymbol, tvScore, tvHistory;
    private Button btnAdd, btnSubtract, btnMultiply, btnGenerate, btnResetScore;
    private Spinner spinnerDifficulty;

    // Variables métier
    private int number1, number2;              // Les deux nombres affichés
    private int currentScore = 0;              // Score actuel
    private Difficulty currentDifficulty = Difficulty.MEDIUM;  // Niveau par défaut
    private ArrayList<String> history;         // Historique des opérations
    private Random random;                     // Générateur aléatoire
    private SharedPreferences prefs;           // Sauvegarde persistante

    // ==================== CYCLE DE VIE ====================

    /**
     * Initialisation de l'activité
     * Configuration de tous les composants et listeners
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialisation des composants
        initializeComponents();

        // Chargement du score sauvegardé
        loadScore();

        // Configuration des événements
        setupListeners();

        // Génération du premier exercice
        generateNewExercise();
    }

    /**
     * Sauvegarde du score avant la destruction de l'activité
     */
    @Override
    protected void onPause() {
        super.onPause();
        saveScore();
    }

    // ==================== INITIALISATION ====================

    /**
     * Initialise tous les composants UI et objets métier
     */
    private void initializeComponents() {
        // Récupération des vues
        tvNumber1 = findViewById(R.id.tvNumber1);
        tvNumber2 = findViewById(R.id.tvNumber2);
        tvResult = findViewById(R.id.tvResult);
        tvOperationSymbol = findViewById(R.id.tvOperationSymbol);
        tvScore = findViewById(R.id.tvScore);
        tvHistory = findViewById(R.id.tvHistory);

        btnAdd = findViewById(R.id.btnAdd);
        btnSubtract = findViewById(R.id.btnSubtract);
        btnMultiply = findViewById(R.id.btnMultiply);
        btnGenerate = findViewById(R.id.btnGenerate);
        btnResetScore = findViewById(R.id.btnResetScore);

        spinnerDifficulty = findViewById(R.id.spinnerDifficulty);

        // Initialisation des objets métier
        random = new Random();
        history = new ArrayList<>();
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Configuration du Spinner de difficulté
        setupDifficultySpinner();
    }

    /**
     * Configure le Spinner pour la sélection du niveau de difficulté
     */
    private void setupDifficultySpinner() {
        String[] difficulties = {
                getString(R.string.difficulty_easy),
                getString(R.string.difficulty_medium),
                getString(R.string.difficulty_hard)
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                difficulties
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDifficulty.setAdapter(adapter);
        spinnerDifficulty.setSelection(1); // Moyen par défaut
    }

    /**
     * Configure tous les listeners d'événements
     */
    private void setupListeners() {
        // Boutons d'opérations
        btnAdd.setOnClickListener(v -> performOperation('+'));
        btnSubtract.setOnClickListener(v -> performOperation('-'));
        btnMultiply.setOnClickListener(v -> performOperation('×'));

        // Bouton de génération
        btnGenerate.setOnClickListener(v -> {
            generateNewExercise();
            showToast(getString(R.string.toast_new_exercise));
        });

        // Bouton de réinitialisation du score
        btnResetScore.setOnClickListener(v -> resetScore());

        // Changement de difficulté
        spinnerDifficulty.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentDifficulty = Difficulty.values()[position];
                generateNewExercise();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // ==================== LOGIQUE MÉTIER ====================

    /**
     * Génère deux nouveaux nombres aléatoires selon le niveau de difficulté
     */
    private void generateNewExercise() {
        number1 = generateRandomNumber(currentDifficulty);
        number2 = generateRandomNumber(currentDifficulty);

        // Mise à jour de l'interface
        tvNumber1.setText(String.valueOf(number1));
        tvNumber2.setText(String.valueOf(number2));
        tvOperationSymbol.setText("?");
        tvResult.setText(getString(R.string.tv_result_placeholder));

        // Animation d'apparition des nombres
        animateView(tvNumber1);
        animateView(tvNumber2);
    }

    /**
     * Génère un nombre aléatoire dans les bornes du niveau de difficulté
     *
     * @param difficulty Le niveau de difficulté
     * @return Un nombre aléatoire
     */
    private int generateRandomNumber(Difficulty difficulty) {
        return random.nextInt(difficulty.max - difficulty.min + 1) + difficulty.min;
    }

    /**
     * Effectue l'opération mathématique sélectionnée
     *
     * @param operation Le symbole de l'opération (+, -, ×)
     */
    private void performOperation(char operation) {
        int result = 0;
        String operationStr = "";

        // Calcul selon l'opération
        switch (operation) {
            case '+':
                result = number1 + number2;
                operationStr = "+";
                break;
            case '-':
                result = number1 - number2;
                operationStr = "−";
                break;
            case '×':
                result = number1 * number2;
                operationStr = "×";
                break;
        }

        // Mise à jour de l'interface
        tvOperationSymbol.setText(String.valueOf(operation));
        tvResult.setText(getString(R.string.result_format, result));

        // Animation du résultat
        animateView(tvResult);

        // Mise à jour du score (simulation d'une bonne réponse automatique)
        updateScore(POINTS_PER_CORRECT);

        // Ajout à l'historique
        addToHistory(number1, operationStr, number2, result);

        // Feedback visuel positif
        flashResultBackground(true);
    }

    /**
     * Ajoute une opération à l'historique
     * Limite l'historique à MAX_HISTORY_SIZE éléments
     *
     * @param num1 Premier nombre
     * @param op Opération
     * @param num2 Deuxième nombre
     * @param result Résultat
     */
    private void addToHistory(int num1, String op, int num2, int result) {
        String entry = String.format("%d %s %d = %d", num1, op, num2, result);

        // Ajout en début de liste
        history.add(0, entry);

        // Limitation à 5 éléments
        if (history.size() > MAX_HISTORY_SIZE) {
            history.remove(MAX_HISTORY_SIZE);
        }

        // Mise à jour de l'affichage
        updateHistoryDisplay();
    }

    /**
     * Met à jour l'affichage de l'historique
     */
    private void updateHistoryDisplay() {
        if (history.isEmpty()) {
            tvHistory.setText(getString(R.string.history_empty));
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < history.size(); i++) {
                sb.append((i + 1)).append(". ").append(history.get(i));
                if (i < history.size() - 1) {
                    sb.append("\n");
                }
            }
            tvHistory.setText(sb.toString());
        }
    }

    // ==================== GESTION DU SCORE ====================

    /**
     * Met à jour le score et l'affiche
     *
     * @param points Points à ajouter
     */
    private void updateScore(int points) {
        currentScore += points;
        tvScore.setText(String.valueOf(currentScore));
        animateView(tvScore);
    }

    /**
     * Réinitialise le score à zéro
     */
    private void resetScore() {
        currentScore = 0;
        tvScore.setText("0");
        history.clear();
        updateHistoryDisplay();
        saveScore();
        showToast(getString(R.string.toast_score_reset));
    }

    /**
     * Charge le score depuis SharedPreferences
     */
    private void loadScore() {
        currentScore = prefs.getInt(KEY_SCORE, 0);
        tvScore.setText(String.valueOf(currentScore));
    }

    /**
     * Sauvegarde le score dans SharedPreferences
     */
    private void saveScore() {
        prefs.edit().putInt(KEY_SCORE, currentScore).apply();
    }

    // ==================== ANIMATIONS & UI ====================

    /**
     * Anime une vue avec un effet de rebond
     *
     * @param view La vue à animer
     */
    private void animateView(View view) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0.8f, 1.1f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0.8f, 1.1f, 1f);

        scaleX.setDuration(400);
        scaleY.setDuration(400);
        scaleX.setInterpolator(new BounceInterpolator());
        scaleY.setInterpolator(new BounceInterpolator());

        scaleX.start();
        scaleY.start();
    }

    /**
     * Flash de couleur sur le résultat (vert pour correct)
     *
     * @param isCorrect True si la réponse est correcte
     */
    private void flashResultBackground(boolean isCorrect) {
        int color = isCorrect ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336");
        tvResult.setTextColor(color);

        // Retour à la couleur normale après 500ms
        tvResult.postDelayed(() -> {
            tvResult.setTextColor(getResources().getColor(R.color.text_primary));
        }, 500);
    }

    /**
     * Affiche un Toast court
     *
     * @param message Le message à afficher
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}