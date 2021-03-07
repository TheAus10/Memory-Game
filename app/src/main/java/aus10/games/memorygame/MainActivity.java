package aus10.games.memorygame;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    Button btnSingle;                           // button for single player mode
    Button btnMulti;                            // button for multiplayer mode
    TextView tvEasy;                            // text view for easy mode
    TextView tvMedium;                          // text view for medium mode
    TextView tvHard;                            // text view for hard mode
    TextView tvCancel;                          // text view for cancel; resets layout

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // assigning XML elements
        btnSingle = findViewById(R.id.btnSingle);
        btnMulti = findViewById(R.id.btnMulti);
        tvEasy = findViewById(R.id.tvEasy);
        tvMedium = findViewById(R.id.tvMedium);
        tvHard = findViewById(R.id.tvHard);
        tvCancel = findViewById(R.id.tvCancel);


        btnSingle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleVisibility();
            }
        });

        btnMulti.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openMultiplayerMenu();
            }
        });

        tvEasy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleVisibility();
                openEasyGame();
            }
        });

        tvMedium.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleVisibility();
                openMediumGame();
            }
        });

        tvHard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleVisibility();
                openHardGame();
            }
        });

        tvCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleVisibility();
            }
        });


    }

    /**
     * opens the EasyMode Activity
     */
    public void openEasyGame(){
        Intent intent = new Intent(this, EasyMode.class);
        startActivity(intent);
    }

    /**
     * opens the MediumMode Activity
     */
    public void openMediumGame(){
        Intent intent = new Intent(this, MediumMode.class);
        startActivity(intent);
    }

    /**
     * opens the HardMode Activity
     */
    public void openHardGame(){
        Intent intent = new Intent(this, HardMode.class);
        startActivity(intent);
    }

    /**
     * opens the MultiplayerMenu Activity
     */
    public void openMultiplayerMenu(){
        Intent intent = new Intent(this, MultiplayerMenu.class);
        startActivity(intent);
    }

    /**
     * toggles the views to either be visible or invisible
     */
    public void toggleVisibility(){
        // single player button
        if(btnSingle.getVisibility() == View.VISIBLE) btnSingle.setVisibility(View.INVISIBLE);
        else btnSingle.setVisibility(View.VISIBLE);

        // multiplayer button
        if(btnMulti.getVisibility() == View.VISIBLE) btnMulti.setVisibility(View.INVISIBLE);
        else btnMulti.setVisibility(View.VISIBLE);

        // "easy" text view
        if(tvEasy.getVisibility() == View.VISIBLE) tvEasy.setVisibility(View.INVISIBLE);
        else tvEasy.setVisibility(View.VISIBLE);

        // "medium" text view
        if(tvMedium.getVisibility() == View.VISIBLE) tvMedium.setVisibility(View.INVISIBLE);
        else tvMedium.setVisibility(View.VISIBLE);

        // "hard" text view
        if(tvHard.getVisibility() == View.VISIBLE) tvHard.setVisibility(View.INVISIBLE);
        else tvHard.setVisibility(View.VISIBLE);

        // "cancel" text view
        if(tvCancel.getVisibility() == View.VISIBLE) tvCancel.setVisibility(View.INVISIBLE);
        else tvCancel.setVisibility(View.VISIBLE);
    }
}