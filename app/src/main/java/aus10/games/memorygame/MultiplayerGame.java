package aus10.games.memorygame;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Timer;
import java.util.TimerTask;

public class MultiplayerGame extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "Multi";
    private final int           NUM_ROWS = 8;                                                       // constant number of rows in this level
    private final int           NUM_COLS = 6;                                                       // constant number of columns in this level
    private final int           NUM_CARDS = NUM_ROWS * NUM_COLS;                                    // constant total number of cards in the game
    private final int           NUM_PICS = NUM_CARDS / 2;                                           // constant number of images being used


    private TextView            tvCountdown;                                                        // opening countdown before game starts
    private TextView            tvScoreP1;                                                          // player 1 score text view
    private TextView            tvScoreP2;                                                          // player 2 score text view
    private int                 scoreP1 = 0;                                                        // player 1 score
    private int                 scoreP2 = 0;                                                        // player 2 score


    private ImageButton[][]     cardBacks = new ImageButton[NUM_ROWS][NUM_COLS];                    // 2d-array for each image button
    private int[][]             cardFronts = new int[NUM_ROWS][NUM_COLS];                           // 2d-array of ints that correspond to a picture, parallel to cardBacks
    private LinearLayout[]      rows = new LinearLayout[NUM_ROWS];                                  // the layout for each row of buttons, used for formatting


    private ImageButton[]       selectedCards = new ImageButton[2];                                 // array to keep track of the two cards that get selected by the user
    private ImageButton[]       opposingPlayerSelectedCards = new ImageButton[2];                   // array to keep track of the two cards that get selected by the opposing player
    private int                 selectedCardCount = 0;                                              // count of the number of cards that have been selected
    private int                 opposingPlayerSelectedCardCount = 0;                                // count of the number of cards that have been selected by the opposing player
    private int                 cardsLeft = NUM_CARDS;                                              // decremented amount of cards left on the board


    private Dialog              victoryScreenDialog;                                                // Dialog variable
    private Timer               timer = new Timer();                                                // Timer variable
    private TimerTask           timerTask;                                                          // TimerTask variable
    private double              time = 5.0;                                                         // keeps track of the elapsed time


    private boolean             isPlayerTurn = false;                                               // true if it is the players turn, false when it is not
    private boolean             isP1 = false;                                                       // true if player 1, false if player 2
    private String              gameCode;                                                           // the game code generated for the specific game
    private String              playerID;                                                           // player id (P1 or P2)
    private int                 moveCount = 0;                                                      // counter for number of moves


    private FirebaseDatabase    root;                                                               // root node of the firebase database
    private DatabaseReference   dbRef;                                                              // firebase database node being referenced depending on the situation


    private GameHelper          gameHelper = new GameHelper();                                      // helper class object

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multiplayer_game);

        // setting up score board elements
        tvScoreP1 = findViewById(R.id.tvScoreP1);
        tvScoreP2 = findViewById(R.id.tvScoreP2);
        tvScoreP1.setTextColor(getResources().getColor(R.color.yellow));
        tvScoreP2.setTextColor(getResources().getColor(R.color.black));

        // getting the player id number and the game code from the intent extras
        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            gameCode = extras.getString("GAME_CODE");
            String tempString = extras.getString("PLAYER_ID");
            assert tempString != null;
            if (tempString.equals("P1")) {
                isP1 = true;
                isPlayerTurn = true;
                playerID = "P1";
                Toast.makeText(this, "You are Player 1", Toast.LENGTH_LONG).show();
            }
            else {
                playerID = "P2";
                Toast.makeText(this, "You are Player 2", Toast.LENGTH_LONG).show();
            }
        }

        // getting to root/games in firebase
        root = FirebaseDatabase.getInstance();
        dbRef = root.getReference("games");

        // listening for the game being deleted from firebase and removing player
        dbRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()) {
                    // sending players back to MultiplayerMenu when game is ended manually by player
                    Toast.makeText(getApplicationContext(), "Game hass been ended.", Toast.LENGTH_LONG).show();
                    finish();
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        // setting up XML elements for count down text and exit button
        Button btnExit = findViewById(R.id.btnExit);                        // exit button that leaves game
        tvCountdown = findViewById(R.id.tvCountdown);

        // setting context for the victory screen dialog box
        victoryScreenDialog = new Dialog(this);

        // assigning XML elements to arrays
        for(int i = 0; i < NUM_ROWS; i++) {
            // assigning each linear layout that makes up a row to a spot in the array, so the buttons can be formatted
            String rowID = "row" + (i+1);
            int rowResID = getResources().getIdentifier(rowID, "id", getPackageName());
            rows[i] = findViewById(rowResID);

            for(int j = 0; j < NUM_COLS; j++)
            {
                // assigning each image button to a spot in the array and setting Tag to default (aka: 0)
                String btnID = "btn" + i + j;
                int btnResID = getResources().getIdentifier(btnID, "id", getPackageName());
                cardBacks[i][j] = findViewById(btnResID);
                cardBacks[i][j].setTag(0);
            }
        }

        // setting up game board. P1 generates a random board and writes to db, P2 copies the same game board from db
        if(isP1) {

            // generating random game board
            cardFronts = gameHelper.assignRandImages(NUM_CARDS, NUM_PICS, NUM_ROWS, NUM_COLS);

            // getting to root/games/[gameCode]/GameBoard in firebase
            String gamePath = "games/" + gameCode + "/GameBoard";
            root = FirebaseDatabase.getInstance();
            dbRef = root.getReference(gamePath);

            // writing game board to db
            for(int i = 0; i < NUM_ROWS; i++){
                for(int j = 0; j < NUM_COLS; j++){

                    String key = "" + i + j;
                    dbRef.child(key).setValue(cardFronts[i][j]);
                }
            }
        }
        else {
            retrieveGameBoard();
        }

        // setting the on click listener for each image button
        for(int i = 0; i < NUM_ROWS; i++) {
            for(int j = 0; j < NUM_COLS; j++)
            {
                cardBacks[i][j].setOnClickListener(this);
            }
        }

        // each player listens for moves made by other player
        listenForMoves();

        // starting the opening countdown
        openingTimer();

        // setting the exit button to return to main
        btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // getting to root/games/[gameCode] in firebase
                String gamePath = "games/" + gameCode;
                root = FirebaseDatabase.getInstance();
                dbRef = root.getReference(gamePath);

                // removing game when a player exits
                dbRef.removeValue();

                // returning to MultiplayerMenu
                finish();
            }
        });
    }


    /**
     * onClick method for the cards
     * @param view - the card that was selected by the user
     */
    @Override
    public void onClick(View view) {

        // checking if it is the players turn before processing their click
        if(isPlayerTurn) {

            // assigning the cards to an array of selected images to be checked later
            if (selectedCardCount == 0) {

                // showing image on the card front
                flipCards(view);

                // getting to root/games/[gameCode]/[playerID] in firebase
                String path = "games/" + gameCode + "/" + playerID;
                root = FirebaseDatabase.getInstance();
                dbRef = root.getReference(path);

                // adding the move to firebase
                dbRef.child(String.valueOf(++moveCount)).setValue(view.getId());

                // setting the card to first in array
                selectedCards[0] = ((ImageButton) view);

                // showing one card has been selected
                selectedCardCount = 1;
            } else {

                // resetting the count
                selectedCardCount = 0;

                // checking that 2nd button pressed is not the first one pressed again
                if (view.getId() != selectedCards[0].getId()) {

                    // showing image on the card front
                    flipCards(view);

                    // adding move into firebase db
                    dbRef.child(String.valueOf(++moveCount)).setValue(view.getId());

                    // setting the card to second in array
                    selectedCards[1] = ((ImageButton) view);

                    // temporarily disabling buttons while being checked
                    toggleButtonsEnabled();

                    // pausing for 250 ms and then checking cards
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // checking that they are the same image
                            if (selectedCards[0].getTag() == selectedCards[1].getTag()) {
                                // hiding the cards
                                selectedCards[0].setVisibility(View.INVISIBLE);
                                selectedCards[1].setVisibility(View.INVISIBLE);
                                cardsLeft -= 2;

                                // adding points to correct score count
                                if(isP1) {
                                    scoreP1++;
                                    String scoreBoard = "P1: " + scoreP1;
                                    tvScoreP1.setText(scoreBoard);
                                }
                                else {
                                    scoreP2++;
                                    String scoreBoard = "P2: " + scoreP2;
                                    tvScoreP2.setText(scoreBoard);
                                }

                                // checking for a win
                                if (cardsLeft < 2) {

                                    // stopping timer, saving the time, and showing victory screen
                                    showVictoryScreen(scoreP1, scoreP2);
                                }
                            } else {
                                // resetting the cards to normal
                                flipCards(selectedCards[0]);
                                flipCards(selectedCards[1]);
                            }

                            // re-enabling buttons
                            toggleButtonsEnabled();
                        }
                    }, 250);

                    // updating player scoreboard to reflect whose turn it is by making their name yellow
                    isPlayerTurn = false;
                    if(isP1) {
                        tvScoreP1.setTextColor(getResources().getColor(R.color.black));
                        tvScoreP2.setTextColor(getResources().getColor(R.color.yellow));
                    }
                    else {
                        tvScoreP1.setTextColor(getResources().getColor(R.color.yellow));
                        tvScoreP2.setTextColor(getResources().getColor(R.color.black));
                    }

                }
            }
        }
        else {
            // notifying the player they cannot move yet
            Toast.makeText(this, "Cannot move, not your turn", Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * checks if the card is showing the "back" or the "front" and flips to show the opposite
     * compares the spot in the parallel 2d-array to find which image needs to be shown
     * @param view - the image button that is selected
     */
    public void flipCards(View view) {

        String[] picNum = new String[NUM_PICS];                                                // Array of strings for all the names of the pictures

        // initializing array with the picture names
        for (int i = 0; i < NUM_PICS; i++)
        {
            picNum[i] = "pic" + (i+1);
        }


        // switches on the id of the view to know which button was clicked
        // checks if the selected card is showing the back or the front and flips to show the opposite
        switch (view.getId()) {
            case R.id.btn00:
                if (cardBacks[0][0].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[0][0]-1)], "mipmap", getPackageName());
                    cardBacks[0][0].setBackgroundResource(imgID);
                    cardBacks[0][0].setTag(cardFronts[0][0]);
                }
                else
                {
                    cardBacks[0][0].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[0][0].setTag(0);
                }
                break;

            case R.id.btn01:
                if (cardBacks[0][1].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[0][1]-1)], "mipmap", getPackageName());
                    cardBacks[0][1].setBackgroundResource(imgID);
                    cardBacks[0][1].setTag(cardFronts[0][1]);
                }
                else
                {
                    cardBacks[0][1].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[0][1].setTag(0);
                }
                break;

            case R.id.btn02:
                if (cardBacks[0][2].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[0][2]-1)], "mipmap", getPackageName());
                    cardBacks[0][2].setBackgroundResource(imgID);
                    cardBacks[0][2].setTag(cardFronts[0][2]);
                }
                else
                {
                    cardBacks[0][2].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[0][2].setTag(0);
                }
                break;

            case R.id.btn03:
                if (cardBacks[0][3].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[0][3]-1)], "mipmap", getPackageName());
                    cardBacks[0][3].setBackgroundResource(imgID);
                    cardBacks[0][3].setTag(cardFronts[0][3]);
                }
                else
                {
                    cardBacks[0][3].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[0][3].setTag(0);
                }
                break;

            case R.id.btn04:
                if (cardBacks[0][4].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[0][4]-1)], "mipmap", getPackageName());
                    cardBacks[0][4].setBackgroundResource(imgID);
                    cardBacks[0][4].setTag(cardFronts[0][4]);
                }
                else
                {
                    cardBacks[0][4].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[0][4].setTag(0);
                }
                break;

            case R.id.btn05:
                if (cardBacks[0][5].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[0][5]-1)], "mipmap", getPackageName());
                    cardBacks[0][5].setBackgroundResource(imgID);
                    cardBacks[0][5].setTag(cardFronts[0][5]);
                }
                else
                {
                    cardBacks[0][5].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[0][5].setTag(0);
                }
                break;

            case R.id.btn10:
                if (cardBacks[1][0].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[1][0]-1)], "mipmap", getPackageName());
                    cardBacks[1][0].setBackgroundResource(imgID);
                    cardBacks[1][0].setTag(cardFronts[1][0]);
                }
                else
                {
                    cardBacks[1][0].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[1][0].setTag(0);
                }
                break;
            case R.id.btn11:
                if (cardBacks[1][1].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[1][1]-1)], "mipmap", getPackageName());
                    cardBacks[1][1].setBackgroundResource(imgID);
                    cardBacks[1][1].setTag(cardFronts[1][1]);
                }
                else
                {
                    cardBacks[1][1].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[1][1].setTag(0);
                }
                break;

            case R.id.btn12:
                if (cardBacks[1][2].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[1][2]-1)], "mipmap", getPackageName());
                    cardBacks[1][2].setBackgroundResource(imgID);
                    cardBacks[1][2].setTag(cardFronts[1][2]);
                }
                else
                {
                    cardBacks[1][2].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[1][2].setTag(0);
                }
                break;

            case R.id.btn13:
                if (cardBacks[1][3].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[1][3]-1)], "mipmap", getPackageName());
                    cardBacks[1][3].setBackgroundResource(imgID);
                    cardBacks[1][3].setTag(cardFronts[1][3]);
                }
                else
                {
                    cardBacks[1][3].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[1][3].setTag(0);
                }
                break;

            case R.id.btn14:
                if (cardBacks[1][4].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[1][4]-1)], "mipmap", getPackageName());
                    cardBacks[1][4].setBackgroundResource(imgID);
                    cardBacks[1][4].setTag(cardFronts[1][4]);
                }
                else
                {
                    cardBacks[1][4].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[1][4].setTag(0);
                }
                break;

            case R.id.btn15:
                if (cardBacks[1][5].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[1][5]-1)], "mipmap", getPackageName());
                    cardBacks[1][5].setBackgroundResource(imgID);
                    cardBacks[1][5].setTag(cardFronts[1][5]);
                }
                else
                {
                    cardBacks[1][5].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[1][5].setTag(0);
                }
                break;

            case R.id.btn20:
                if (cardBacks[2][0].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[2][0]-1)], "mipmap", getPackageName());
                    cardBacks[2][0].setBackgroundResource(imgID);
                    cardBacks[2][0].setTag(cardFronts[2][0]);
                }
                else
                {
                    cardBacks[2][0].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[2][0].setTag(0);
                }
                break;

            case R.id.btn21:
                if (cardBacks[2][1].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[2][1]-1)], "mipmap", getPackageName());
                    cardBacks[2][1].setBackgroundResource(imgID);
                    cardBacks[2][1].setTag(cardFronts[2][1]);
                }
                else
                {
                    cardBacks[2][1].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[2][1].setTag(0);
                }
                break;

            case R.id.btn22:
                if (cardBacks[2][2].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[2][2]-1)], "mipmap", getPackageName());
                    cardBacks[2][2].setBackgroundResource(imgID);
                    cardBacks[2][2].setTag(cardFronts[2][2]);
                }
                else
                {
                    cardBacks[2][2].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[2][2].setTag(0);
                }
                break;

            case R.id.btn23:
                if (cardBacks[2][3].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[2][3]-1)], "mipmap", getPackageName());
                    cardBacks[2][3].setBackgroundResource(imgID);
                    cardBacks[2][3].setTag(cardFronts[2][3]);
                }
                else
                {
                    cardBacks[2][3].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[2][3].setTag(0);
                }
                break;

            case R.id.btn24:
                if (cardBacks[2][4].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[2][4]-1)], "mipmap", getPackageName());
                    cardBacks[2][4].setBackgroundResource(imgID);
                    cardBacks[2][4].setTag(cardFronts[2][4]);
                }
                else
                {
                    cardBacks[2][4].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[2][4].setTag(0);
                }
                break;

            case R.id.btn25:
                if (cardBacks[2][5].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[2][5]-1)], "mipmap", getPackageName());
                    cardBacks[2][5].setBackgroundResource(imgID);
                    cardBacks[2][5].setTag(cardFronts[2][5]);
                }
                else
                {
                    cardBacks[2][5].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[2][5].setTag(0);
                }
                break;

            case R.id.btn30:
                if (cardBacks[3][0].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[3][0]-1)], "mipmap", getPackageName());
                    cardBacks[3][0].setBackgroundResource(imgID);
                    cardBacks[3][0].setTag(cardFronts[3][0]);
                }
                else
                {
                    cardBacks[3][0].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[3][0].setTag(0);
                }
                break;

            case R.id.btn31:
                if (cardBacks[3][1].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[3][1]-1)], "mipmap", getPackageName());
                    cardBacks[3][1].setBackgroundResource(imgID);
                    cardBacks[3][1].setTag(cardFronts[3][1]);
                }
                else
                {
                    cardBacks[3][1].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[3][1].setTag(0);
                }
                break;

            case R.id.btn32:
                if (cardBacks[3][2].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[3][2]-1)], "mipmap", getPackageName());
                    cardBacks[3][2].setBackgroundResource(imgID);
                    cardBacks[3][2].setTag(cardFronts[3][2]);
                }
                else
                {
                    cardBacks[3][2].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[3][2].setTag(0);
                }
                break;

            case R.id.btn33:
                if (cardBacks[3][3].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[3][3]-1)], "mipmap", getPackageName());
                    cardBacks[3][3].setBackgroundResource(imgID);
                    cardBacks[3][3].setTag(cardFronts[3][3]);
                }
                else
                {
                    cardBacks[3][3].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[3][3].setTag(0);
                }
                break;

            case R.id.btn34:
                if (cardBacks[3][4].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[3][4]-1)], "mipmap", getPackageName());
                    cardBacks[3][4].setBackgroundResource(imgID);
                    cardBacks[3][4].setTag(cardFronts[3][4]);
                }
                else
                {
                    cardBacks[3][4].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[3][4].setTag(0);
                }
                break;

            case R.id.btn35:
                if (cardBacks[3][5].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[3][5]-1)], "mipmap", getPackageName());
                    cardBacks[3][5].setBackgroundResource(imgID);
                    cardBacks[3][5].setTag(cardFronts[3][5]);
                }
                else
                {
                    cardBacks[3][5].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[3][5].setTag(0);
                }
                break;

            case R.id.btn40:
                if (cardBacks[4][0].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[4][0]-1)], "mipmap", getPackageName());
                    cardBacks[4][0].setBackgroundResource(imgID);
                    cardBacks[4][0].setTag(cardFronts[4][0]);
                }
                else
                {
                    cardBacks[4][0].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[4][0].setTag(0);
                }
                break;

            case R.id.btn41:
                if (cardBacks[4][1].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[4][1]-1)], "mipmap", getPackageName());
                    cardBacks[4][1].setBackgroundResource(imgID);
                    cardBacks[4][1].setTag(cardFronts[4][1]);
                }
                else
                {
                    cardBacks[4][1].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[4][1].setTag(0);
                }
                break;

            case R.id.btn42:
                if (cardBacks[4][2].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[4][2]-1)], "mipmap", getPackageName());
                    cardBacks[4][2].setBackgroundResource(imgID);
                    cardBacks[4][2].setTag(cardFronts[4][2]);
                }
                else
                {
                    cardBacks[4][2].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[4][2].setTag(0);
                }
                break;

            case R.id.btn43:
                if (cardBacks[4][3].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[4][3]-1)], "mipmap", getPackageName());
                    cardBacks[4][3].setBackgroundResource(imgID);
                    cardBacks[4][3].setTag(cardFronts[4][3]);
                }
                else
                {
                    cardBacks[4][3].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[4][3].setTag(0);
                }
                break;

            case R.id.btn44:
                if (cardBacks[4][4].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[4][4]-1)], "mipmap", getPackageName());
                    cardBacks[4][4].setBackgroundResource(imgID);
                    cardBacks[4][4].setTag(cardFronts[4][4]);
                }
                else
                {
                    cardBacks[4][4].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[4][4].setTag(0);
                }
                break;

            case R.id.btn45:
                if (cardBacks[4][5].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[4][5]-1)], "mipmap", getPackageName());
                    cardBacks[4][5].setBackgroundResource(imgID);
                    cardBacks[4][5].setTag(cardFronts[4][5]);
                }
                else
                {
                    cardBacks[4][5].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[4][5].setTag(0);
                }
                break;

            case R.id.btn50:
                if (cardBacks[5][0].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[5][0]-1)], "mipmap", getPackageName());
                    cardBacks[5][0].setBackgroundResource(imgID);
                    cardBacks[5][0].setTag(cardFronts[5][0]);
                }
                else
                {
                    cardBacks[5][0].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[5][0].setTag(0);
                }
                break;

            case R.id.btn51:
                if (cardBacks[5][1].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[5][1]-1)], "mipmap", getPackageName());
                    cardBacks[5][1].setBackgroundResource(imgID);
                    cardBacks[5][1].setTag(cardFronts[5][1]);
                }
                else
                {
                    cardBacks[5][1].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[5][1].setTag(0);
                }
                break;

            case R.id.btn52:
                if (cardBacks[5][2].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[5][2]-1)], "mipmap", getPackageName());
                    cardBacks[5][2].setBackgroundResource(imgID);
                    cardBacks[5][2].setTag(cardFronts[5][2]);
                }
                else
                {
                    cardBacks[5][2].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[5][2].setTag(0);
                }
                break;

            case R.id.btn53:
                if (cardBacks[5][3].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[5][3]-1)], "mipmap", getPackageName());
                    cardBacks[5][3].setBackgroundResource(imgID);
                    cardBacks[5][3].setTag(cardFronts[5][3]);
                }
                else
                {
                    cardBacks[5][3].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[5][3].setTag(0);
                }
                break;

            case R.id.btn54:
                if (cardBacks[5][4].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[5][4]-1)], "mipmap", getPackageName());
                    cardBacks[5][4].setBackgroundResource(imgID);
                    cardBacks[5][4].setTag(cardFronts[5][4]);
                }
                else
                {
                    cardBacks[5][4].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[5][4].setTag(0);
                }
                break;

            case R.id.btn55:
                if (cardBacks[5][5].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[5][5]-1)], "mipmap", getPackageName());
                    cardBacks[5][5].setBackgroundResource(imgID);
                    cardBacks[5][5].setTag(cardFronts[5][5]);
                }
                else
                {
                    cardBacks[5][5].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[5][5].setTag(0);
                }
                break;

            case R.id.btn60:
                if (cardBacks[6][0].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[6][0]-1)], "mipmap", getPackageName());
                    cardBacks[6][0].setBackgroundResource(imgID);
                    cardBacks[6][0].setTag(cardFronts[6][0]);
                }
                else
                {
                    cardBacks[6][0].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[6][0].setTag(0);
                }
                break;

            case R.id.btn61:
                if (cardBacks[6][1].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[6][1]-1)], "mipmap", getPackageName());
                    cardBacks[6][1].setBackgroundResource(imgID);
                    cardBacks[6][1].setTag(cardFronts[6][1]);
                }
                else
                {
                    cardBacks[6][1].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[6][1].setTag(0);
                }
                break;

            case R.id.btn62:
                if (cardBacks[6][2].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[6][2]-1)], "mipmap", getPackageName());
                    cardBacks[6][2].setBackgroundResource(imgID);
                    cardBacks[6][2].setTag(cardFronts[6][2]);
                }
                else
                {
                    cardBacks[6][2].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[6][2].setTag(0);
                }
                break;

            case R.id.btn63:
                if (cardBacks[6][3].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[6][3]-1)], "mipmap", getPackageName());
                    cardBacks[6][3].setBackgroundResource(imgID);
                    cardBacks[6][3].setTag(cardFronts[6][3]);
                }
                else
                {
                    cardBacks[6][3].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[6][3].setTag(0);
                }
                break;

            case R.id.btn64:
                if (cardBacks[6][4].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[6][4]-1)], "mipmap", getPackageName());
                    cardBacks[6][4].setBackgroundResource(imgID);
                    cardBacks[6][4].setTag(cardFronts[6][4]);
                }
                else
                {
                    cardBacks[6][4].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[6][4].setTag(0);
                }
                break;

            case R.id.btn65:
                if (cardBacks[6][5].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[6][5]-1)], "mipmap", getPackageName());
                    cardBacks[6][5].setBackgroundResource(imgID);
                    cardBacks[6][5].setTag(cardFronts[6][5]);
                }
                else
                {
                    cardBacks[6][5].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[6][5].setTag(0);
                }
                break;

            case R.id.btn70:
                if (cardBacks[7][0].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[7][0]-1)], "mipmap", getPackageName());
                    cardBacks[7][0].setBackgroundResource(imgID);
                    cardBacks[7][0].setTag(cardFronts[7][0]);
                }
                else
                {
                    cardBacks[7][0].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[7][0].setTag(0);
                }
                break;

            case R.id.btn71:
                if (cardBacks[7][1].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[7][1]-1)], "mipmap", getPackageName());
                    cardBacks[7][1].setBackgroundResource(imgID);
                    cardBacks[7][1].setTag(cardFronts[7][1]);
                }
                else
                {
                    cardBacks[7][1].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[7][1].setTag(0);
                }
                break;

            case R.id.btn72:
                if (cardBacks[7][2].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[7][2]-1)], "mipmap", getPackageName());
                    cardBacks[7][2].setBackgroundResource(imgID);
                    cardBacks[7][2].setTag(cardFronts[7][2]);
                }
                else
                {
                    cardBacks[7][2].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[7][2].setTag(0);
                }
                break;

            case R.id.btn73:
                if (cardBacks[7][3].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[7][3]-1)], "mipmap", getPackageName());
                    cardBacks[7][3].setBackgroundResource(imgID);
                    cardBacks[7][3].setTag(cardFronts[7][3]);
                }
                else
                {
                    cardBacks[7][3].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[7][3].setTag(0);
                }
                break;

            case R.id.btn74:
                if (cardBacks[7][4].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[7][4]-1)], "mipmap", getPackageName());
                    cardBacks[7][4].setBackgroundResource(imgID);
                    cardBacks[7][4].setTag(cardFronts[7][4]);
                }
                else
                {
                    cardBacks[7][4].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[7][4].setTag(0);
                }
                break;

            case R.id.btn75:
                if (cardBacks[7][5].getTag().toString().equals("0")) {
                    // setting the image and the tag
                    int imgID = getResources().getIdentifier(picNum[(cardFronts[7][5]-1)], "mipmap", getPackageName());
                    cardBacks[7][5].setBackgroundResource(imgID);
                    cardBacks[7][5].setTag(cardFronts[7][5]);
                }
                else
                {
                    cardBacks[7][5].setBackgroundResource(R.mipmap.ic_launcher);
                    cardBacks[7][5].setTag(0);
                }
                break;
        }
    }


    /**
     * sets up a listener for the Game Board to assign the images in this game board to the same ones used in the other players
     */
    public void retrieveGameBoard() {

        // getting to root/games/[gameCode]/GameBoard in firebase
        String gamePath = "games/" + gameCode + "/GameBoard";
        root = FirebaseDatabase.getInstance();
        dbRef = root.getReference(gamePath);

        // listening for any children (aka id of the images set in the game board)
        dbRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if(snapshot.exists()) {

                    // getting the key (array indexes) of type string and converting it into a character array
                    char[] key = snapshot.getKey().toCharArray();

                    // assigning the value (image id) into the appropriate space in the parallel 2d-array and adjusting for ASCII values of numbers
                    long value = (long) snapshot.getValue();
                    cardFronts[key[0]-48][key[1]-48] = (int) value;
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if(snapshot.exists()) {

                    // getting the key (array indexes) of type string and converting it into a character array
                    char[] key = snapshot.getKey().toCharArray();

                    // assigning the value (image id) into the appropriate space in the parallel 2d-array and adjusting for ASCII values of numbers
                    long value = (long) snapshot.getValue();
                    cardFronts[key[0]-48][key[1]-48] = (int) value;
                }
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


    /**
     * sets up listeners for moves down by the opposing player
     */
    public void listenForMoves() {
        if(isP1){   // P1 will listen for moves done by P2


            // getting to root/games/[gameCode]/P2 in firebase
            String gamePath = "games/" + gameCode + "/P2";
            root = FirebaseDatabase.getInstance();
            dbRef = root.getReference(gamePath);

            // listening for any moves made by the other player to be added into the db
            dbRef.addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    if(snapshot.exists()) {

                        // getting the id of the image button pressed by other user
                        long value = (long) snapshot.getValue();
                        int selectedImageID = (int) value;

                        // finding the card selected by the other player and flipping it over on current users screen
                        for (int i = 0; i < NUM_ROWS; i++) {
                            for (int j = 0; j < NUM_COLS; j++) {
                                if(cardBacks[i][j].getId() == selectedImageID)
                                {
                                    handleReceivedMove(cardBacks[i][j]);
                                }
                            }
                        }
                    }
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    if(snapshot.exists()) {

                        // getting the id of the image button pressed by other user
                        long value = (long) snapshot.getValue();
                        int selectedImageID = (int) value;

                        // finding the card selected by the other player and flipping it over on current users screen
                        for (int i = 0; i < NUM_ROWS; i++) {
                            for (int j = 0; j < NUM_COLS; j++) {
                                if(cardBacks[i][j].getId() == selectedImageID)
                                {
                                    handleReceivedMove(cardBacks[i][j]);
                                }
                            }
                        }
                    }
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
        else {      // P2 will listen for moves done by P1

            // getting to root/games/[gameCode]/P1 in firebase
            String gamePath = "games/" + gameCode + "/P1";
            root = FirebaseDatabase.getInstance();
            dbRef = root.getReference(gamePath);


            // listening for any moves made by the other player to be added into the db
            dbRef.addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                    if(snapshot.exists()) {

                        // getting the id of the image button pressed by other user
                        long value = (long) snapshot.getValue();
                        int selectedImageID = (int) value;

                        // finding the card selected by the other player and flipping it over on current users screen
                        for (int i = 0; i < NUM_ROWS; i++) {
                            for (int j = 0; j < NUM_COLS; j++) {
                                if(cardBacks[i][j].getId() == selectedImageID)
                                {
                                    handleReceivedMove(cardBacks[i][j]);
                                }
                            }
                        }
                    }
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    if(snapshot.exists()) {

                        // getting the id of the image button pressed by other user
                        long value = (long) snapshot.getValue();
                        int selectedImageID = (int) value;

                        // finding the card selected by the other player and flipping it over on current users screen
                        for (int i = 0; i < NUM_ROWS; i++) {
                            for (int j = 0; j < NUM_COLS; j++) {
                                if(cardBacks[i][j].getId() == selectedImageID)
                                {
                                    handleReceivedMove(cardBacks[i][j]);
                                }
                            }
                        }
                    }
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
    }


    /**
     * starts the game over and restores the layout
     */
    public void resetGame() {

        // resetting layout
        for (int i = 0; i < NUM_ROWS; i++) {
            for (int j = 0; j < NUM_COLS; j++) {
                cardFronts[i][j] = 0;
                cardBacks[i][j].setVisibility(View.VISIBLE);
                if (!cardBacks[i][j].getTag().toString().equals("0"))
                {
                    flipCards(cardBacks[i][j]);
                }
            }
        }

        // resetting card count, scores, and colors
        cardsLeft = NUM_CARDS;
        moveCount = 0;
        scoreP1 = 0;
        scoreP2 = 0;
        String scoreBoard = "P1: " + scoreP1;
        tvScoreP1.setText(scoreBoard);
        scoreBoard = "P2: " + scoreP2;
        tvScoreP2.setText(scoreBoard);
        tvScoreP1.setTextColor(getResources().getColor(R.color.yellow));
        tvScoreP2.setTextColor(getResources().getColor(R.color.black));

        // repeating startup procedures for player 1 and 2
        if(isP1) {

            // generating random game board
            cardFronts = gameHelper.assignRandImages(NUM_CARDS, NUM_PICS, NUM_ROWS, NUM_COLS);

            // getting to root/games/[gameCode]/GameBoard in firebase
            String gamePath = "games/" + gameCode + "/GameBoard";
            root = FirebaseDatabase.getInstance();
            dbRef = root.getReference(gamePath);

            // writing game board to db
            for(int i = 0; i < NUM_ROWS; i++){
                for(int j = 0; j < NUM_COLS; j++){

                    String key = "" + i + j;
                    dbRef.child(key).setValue(cardFronts[i][j]);
                }
            }

            isPlayerTurn = true;
        }
        else {
            retrieveGameBoard();
            isPlayerTurn = false;
        }
    }


    /**
     * loops through all image buttons and toggles them to be enabled/disabled
     */
    public void toggleButtonsEnabled() {
        for(int i = 0; i < NUM_ROWS; i++){
            for(int j = 0; j < NUM_COLS; j++){

                // if the button is enabled, disable it and vice versa
                if(cardBacks[i][j].isEnabled()){
                    cardBacks[i][j].setEnabled(false);
                }
                else {
                    cardBacks[i][j].setEnabled(true);
                }
            }
        }
    }


    /**
     * starts a countdown at the start of the game
     */
    public void openingTimer() {

        timerTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        time--;
                        tvCountdown.setText(String.valueOf(time));

                        if(time == 0.0){
                            timerTask.cancel();
                            tvCountdown.setVisibility(View.GONE);

                            // formatting the buttons
                            for(int i = 0; i < NUM_ROWS; i++)
                            {
                                for(int j = 0; j < NUM_COLS; j++)
                                {
                                    gameHelper.formatRows(rows[i], cardBacks[i][j], NUM_COLS);
                                }
                            }

                            // making all rows visible
                            for(int i = 0; i < NUM_ROWS; i++)
                            {
                                rows[i].setVisibility(View.VISIBLE);
                            }
                        }
                    }
                });
            }
        };
        timer.scheduleAtFixedRate(timerTask, 0, 1000);

    }


    /**
     * shows the victory screen popup dialog
     * @param p1 - score of player 1
     * @param p2 - score of player 2
     */
    public void showVictoryScreen(int p1, int p2) {

        // setting up the victory screen popup XML elements
        victoryScreenDialog.setContentView(R.layout.victory_screen_popup);
        TextView tvScores = victoryScreenDialog.findViewById(R.id.tvScores);
        TextView tvTitle = victoryScreenDialog.findViewById((R.id.tvTitle));
        Button btnPlayAgain = victoryScreenDialog.findViewById(R.id.btnPlayAgain);
        Button btnHome = victoryScreenDialog.findViewById(R.id.btnHome);

        // setting score text
        String scores = "P1: " + p1 + "    P2: " + p2;
        tvScores.setText(scores);

        // checking which score is higher and setting title with winner
        String title;
        if(p1 > p2){
            title = "Player 1 Wins!";
            tvTitle.setText(title);
        }
        else if (p1 < p2) {
            title = "Player 2 Wins!";
            tvTitle.setText(title);
        }
        else {
            title = "It's a tie!";
            tvTitle.setText(title);
        }

        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // getting to root/games/[gameCode] in firebase
                String gamePath = "games/" + gameCode;
                root = FirebaseDatabase.getInstance();
                dbRef = root.getReference(gamePath);

                // removing game when a player exits
                dbRef.removeValue();

                // returning to MultiplayerMenu
                finish();
            }
        });

        btnPlayAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // restarting game
                victoryScreenDialog.dismiss();
                resetGame();

            }
        });

        victoryScreenDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        victoryScreenDialog.show();
    }


    /**
     * handles a move received from opposing player, similar to onClick listener for current player
     * @param view - the card selected by the opposing player
     */
    public void handleReceivedMove(View view) {
        // assigning the cards to an array of selected images to be checked later
        if (opposingPlayerSelectedCardCount == 0) {

            // showing image on the card front
            flipCards(view);

            // setting the card to first in array
            opposingPlayerSelectedCards[0] = ((ImageButton) view);

            // showing one card has been selected
            opposingPlayerSelectedCardCount = 1;
        }
        else {

            // resetting the count
            opposingPlayerSelectedCardCount = 0;

            // checking that 2nd button pressed is not the first one pressed again
            if(view.getId() != opposingPlayerSelectedCards[0].getId()) {

                // showing image on the card front
                flipCards(view);

                // setting the card to second in array
                opposingPlayerSelectedCards[1] = ((ImageButton) view);

                // pausing for 250 ms and then checking cards
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // checking that they are the same image
                        if (opposingPlayerSelectedCards[0].getTag() == opposingPlayerSelectedCards[1].getTag())
                        {
                            // hiding the cards
                            opposingPlayerSelectedCards[0].setVisibility(View.INVISIBLE);
                            opposingPlayerSelectedCards[1].setVisibility(View.INVISIBLE);
                            cardsLeft -= 2;

                            // adding points to correct score count
                            if(isP1) {
                                scoreP2++;
                                String scoreBoard = "P2: " + scoreP2;
                                tvScoreP2.setText(scoreBoard);
                            }
                            else {
                                scoreP1++;
                                String scoreBoard = "P1: " + scoreP1;
                                tvScoreP1.setText(scoreBoard);
                            }

                            // checking for a win
                            if (cardsLeft < 2)
                            {
                                showVictoryScreen(scoreP1, scoreP2);
                            }
                        }
                        else
                        {
                            // resetting the cards to normal
                            flipCards(opposingPlayerSelectedCards[0]);
                            flipCards(opposingPlayerSelectedCards[1]);
                        }
                    }
                }, 250);

                // updating player scoreboard to reflect whose turn it is by making their name yellow
                isPlayerTurn = true;
                if(isP1) {
                    tvScoreP1.setTextColor(getResources().getColor(R.color.yellow));
                    tvScoreP2.setTextColor(getResources().getColor(R.color.black));
                }
                else {
                    tvScoreP1.setTextColor(getResources().getColor(R.color.black));
                    tvScoreP2.setTextColor(getResources().getColor(R.color.yellow));
                }
            }
        }
    }
}