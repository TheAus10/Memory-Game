package aus10.games.memorygame;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;



public class MultiplayerMenu extends AppCompatActivity {

    Button btnCreate;                               // button for creating a room to use
    Button btnJoin;                                 // button for joining an existing room
    Button btnBack;                                 // back button to send user back to main menu
    Button btnCancel;                               // cancel button to send user back to multiplayer menu
    Button btnEnter;                                // enter button that checks game code
    EditText etCode;                                // edit text for game code input
    TextView tvCode;                                // text view for the game code
    TextView tvInstructions;                        // text view for instructions

    String gameCode;                                // holds the random game code
    String codeText;                                // text for code text view
    String instructions;                            // text for instructions text view

    FirebaseDatabase root;                          // root node for database
    DatabaseReference dbRef;                        // reference node for database

    boolean isP1 = false;                           // checks if the player is player 1 or not

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multiplayer_menu);

        btnCreate = findViewById(R.id.btnCreate);
        btnJoin   = findViewById(R.id.btnJoin);
        btnBack   = findViewById(R.id.btnBack);
        btnCancel = findViewById(R.id.btnCancel);
        btnEnter  = findViewById(R.id.btnEnter);
        etCode    = findViewById(R.id.etCode);
        tvCode    = findViewById(R.id.tvCode);
        tvInstructions = findViewById(R.id.tvInstructions);

        btnCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // setting up layout
                btnCreate.setVisibility(View.INVISIBLE);
                btnJoin.setVisibility(View.INVISIBLE);
                btnBack.setVisibility(View.INVISIBLE);
                btnCancel.setVisibility(View.VISIBLE);

                // generating a random 4 character game code
                gameCode = GameHelper.genGameCode();

                // getting to root/games in firebase
                root = FirebaseDatabase.getInstance();
                dbRef = root.getReference("games");

                // add a child to "games" with the new game code and add player 1 to that specific game
                dbRef.child(gameCode).child("P1").setValue(1);
                isP1 = true;

                // adding a GameBoard child to be used in MultiplayerGame
                dbRef.child(gameCode).child("GameBoard").setValue(0);

                // setting game code and instructions text
                codeText = "Game code:\n" + gameCode;
                instructions = "You will be player 1. Give this code to Player 2.";
                tvCode.setText(codeText);
                tvInstructions.setText(instructions);
                tvCode.setVisibility(View.VISIBLE);
                tvInstructions.setVisibility(View.VISIBLE);

                // listening for a new child (player) in "games/[gameCode]"
                dbRef.child(gameCode).addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                        // checking that the player added is player 2, not player 1
                        if(snapshot.getKey().equals("P2")) {

                            // notifying user of player 2 connection (even if unseen by user)
                            instructions = "Player 2 has connected";
                            tvInstructions.setText(instructions);
                            tvInstructions.setVisibility(View.VISIBLE);

                            // resetting layout
                            resetLayout();

                            // once player 2 has been added, sending user into game as player 1
                            Intent intent = new Intent(getApplicationContext(), MultiplayerGame.class);
                            intent.putExtra("PLAYER_ID", "P1");
                            intent.putExtra("GAME_CODE", gameCode);
                            startActivity(intent);
                        }
                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                    }

                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot snapshot) {

                    }

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        });


        btnJoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // setting up layout
                instructions = "Enter a game code";
                tvInstructions.setText(instructions);
                tvInstructions.setVisibility(View.VISIBLE);
                btnCreate.setVisibility(View.INVISIBLE);
                btnJoin.setVisibility(View.INVISIBLE);
                btnBack.setVisibility(View.INVISIBLE);
                btnCancel.setVisibility(View.VISIBLE);
                btnEnter.setVisibility(View.VISIBLE);
                etCode.setVisibility(View.VISIBLE);
            }
        });

        btnEnter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // getting the input game code
                gameCode = etCode.getText().toString();

                // getting to root/games in firebase
                root = FirebaseDatabase.getInstance();
                dbRef = root.getReference("games");

                // listening for data within firebase to see if the entered game code exists
                dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        // checking if the db has a game with the given code
                        if(snapshot.hasChild(gameCode)){

                            // telling user game has been found
                            instructions = "Game found!";
                            tvInstructions.setText(instructions);
                            tvInstructions.setVisibility(View.VISIBLE);

                            // adding to specific game within db
                            dbRef.child(gameCode).child("P2").setValue(2);

                            // resetting layout
                            resetLayout();

                            // sending player into game as player 2
                            Intent intent = new Intent(getApplicationContext(), MultiplayerGame.class);
                            intent.putExtra("PLAYER_ID", "P2");
                            intent.putExtra("GAME_CODE", gameCode);
                            startActivity(intent);

                        }
                        else {
                            // telling user the game could not be found
                            instructions = "No game found";
                            tvInstructions.setText(instructions);
                            tvInstructions.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                root = FirebaseDatabase.getInstance();
                dbRef = root.getReference("games");

                // removing the game if P1 cancels the game
                if(isP1) {
                    dbRef.child(gameCode).removeValue();
                }

                // resetting layout
                resetLayout();
            }
        });

    }

    /**
     * resets the layout to the original state when Menu activity is started
     */
    public void resetLayout(){
        // resetting layout
        btnCreate.setVisibility(View.VISIBLE);
        btnJoin.setVisibility(View.VISIBLE);
        btnBack.setVisibility(View.VISIBLE);
        btnCancel.setVisibility(View.INVISIBLE);
        btnEnter.setVisibility(View.INVISIBLE);
        tvCode.setVisibility(View.INVISIBLE);
        tvInstructions.setVisibility(View.INVISIBLE);
        etCode.setVisibility(View.INVISIBLE);
        etCode.getText().clear();
    }
}
