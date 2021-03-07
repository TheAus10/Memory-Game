package aus10.games.memorygame;

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

import java.util.Timer;
import java.util.TimerTask;

public class HardMode extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "Hard";
    private final int           NUM_ROWS = 8;                                                       // constant number of rows in this level
    private final int           NUM_COLS = 6;                                                       // constant number of columns in this level
    private final int           NUM_CARDS = NUM_ROWS * NUM_COLS;                                    // constant total number of cards in the game
    private final int           NUM_PICS = NUM_CARDS / 2;                                           // constant number of images being used


    private ImageButton[][]     cardBacks = new ImageButton[NUM_ROWS][NUM_COLS];                    // 2d-array for each image button
    private int[][]             cardFronts = new int[NUM_ROWS][NUM_COLS];                           // 2d-array of ints that correspond to a picture, parallel to cardBacks
    private LinearLayout[]      rows = new LinearLayout[NUM_ROWS];                                  // the layout for each row of buttons, used for formatting


    private ImageButton[]       selectedCards = new ImageButton[2];                                 // array to keep track of the two cards that get selected by the user
    private int                 selectedCardCount = 0;                                              // count of the number of cards that have been selected
    private int                 cardsLeft = NUM_CARDS;                                              // decremented amount of cards left on the board


    private Dialog              victoryScreenDialog;                                                // Dialog variable
    private Timer               timer = new Timer();                                                // Timer variable
    private TimerTask           timerTask;                                                          // TimerTask variable
    private TextView            tvTimer;                                                            // TextView for the timer
    private double              time = 0.0;                                                         // keeps track of the elapsed time
    private boolean             timerRunning = false;                                               // keeps track of the state of the timer


    private GameHelper          gameHelper = new GameHelper();                                      // helper class object


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hard_mode);

        Button btnReset = findViewById(R.id.btnReset);                                              // reset button
        Button btnExit = findViewById(R.id.btnExit);                                                // exit  button
        final Button btnStart = findViewById(R.id.btnStart);                                        // start button

        // setting context for the victory screen dialog box
        victoryScreenDialog = new Dialog(this);
        
        // setting the timer text to 00:00 before the game is started
        tvTimer = findViewById(R.id.tvTimer);
        tvTimer.setText(gameHelper.formatTime(0,0));


        // assigning XML elements to arrays
        for(int i = 0; i < NUM_ROWS; i++)
        {
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

        // randomizing images within the array
        cardFronts = gameHelper.assignRandImages(NUM_CARDS, NUM_PICS, NUM_ROWS, NUM_COLS);

        // setting the on click listener for each image button
        for(int i = 0; i < NUM_ROWS; i++)
        {
            for(int j = 0; j < NUM_COLS; j++)
            {
                cardBacks[i][j].setOnClickListener(this);
            }
        }

        // setting the reset button to call the reset function
        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetGame();
            }
        });

        // setting the exit button to return to main
        btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        // setting the start button to start the game
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // formatting the buttons
                for(int i = 0; i < NUM_ROWS; i++)
                {
                    for(int j = 0; j < NUM_COLS; j++)
                    {
                        formatRows(rows[i], cardBacks[i][j]);
                    }
                }

                // hiding the start button to not affect the button layout
                btnStart.setVisibility(View.GONE);

                // making all rows visible
                for(int i = 0; i < NUM_ROWS; i++)
                {
                    rows[i].setVisibility(View.VISIBLE);
                }

                // starting the timer
                startStopTimer();
            }
        });
    }


    /**
     * onClick method for the cards
     * @param view - the card that was selected by the user
     */
    @Override
    public void onClick(View view) {


        // assigning the cards to an array of selected images to be checked later
        if (selectedCardCount == 0) {

            // showing image on the card front
            flipCards(view);

            // setting the card to first in array
            selectedCards[0] = ((ImageButton) view);

            // showing one card has been selected
            selectedCardCount = 1;
        }
        else {

            // resetting the count
            selectedCardCount = 0;

            // checking that 2nd button pressed is not the first one pressed again
            if(view.getId() != selectedCards[0].getId()) {

                // showing image on the card front
                flipCards(view);

                // setting the to second in array
                selectedCards[1] = ((ImageButton) view);

                // temporarily disabling buttons while being checked
                toggleButtonsEnabled();

                // pausing for 250 ms and then checking cards
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // checking that they are the same image
                        if (selectedCards[0].getTag() == selectedCards[1].getTag())
                        {
                            // hiding the cards
                            selectedCards[0].setVisibility(View.INVISIBLE);
                            selectedCards[1].setVisibility(View.INVISIBLE);
                            cardsLeft -= 2;

                            // checking for a win
                            if (cardsLeft < 2)
                            {
                                // stopping timer, saving the time, and showing victory screen
                                startStopTimer();
                                String totalTime = tvTimer.getText().toString();
                                showVictoryScreen(totalTime);
                            }
                        }
                        else
                        {
                            // resetting the cards to normal
                            flipCards(selectedCards[0]);
                            flipCards(selectedCards[1]);
                        }

                        // re-enabling buttons
                        toggleButtonsEnabled();
                    }
                }, 250);
            }
            else
            {
                // deselecting the card
                flipCards(view);
            }
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
     * starts the game over and restores the layout
     */
    public void resetGame() {

        // stopping timer if it is running
        if(timerRunning) startStopTimer();

        // resetting selected card count
        selectedCardCount = 0;

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
        cardsLeft = NUM_CARDS;

        // re-assigning images
        cardFronts = gameHelper.assignRandImages(NUM_CARDS, NUM_PICS, NUM_ROWS, NUM_COLS);

        // re-starting timer
        startStopTimer();
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
     * toggles the timer on and off
     */
    public void startStopTimer() {
        if(timerRunning){
            // stop the timer
            timerRunning = false;
            timerTask.cancel();
            time = 0.0;
        }
        else{
            // start the timer
            timerRunning = true;
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            time++;
                            tvTimer.setText(gameHelper.getTimeText(time));
                        }
                    });
                }
            };
            timer.scheduleAtFixedRate(timerTask, 0, 1000);
        }
    }


    /**
     * shows the victory screen popup
     * @param totalTime - the final time it took for the user to finish the level
     */
    public void showVictoryScreen(String totalTime) {
        victoryScreenDialog.setContentView(R.layout.victory_screen_popup);
        TextView tvTotalTime = victoryScreenDialog.findViewById(R.id.tvTotalTime);
        Button btnPlayAgain = victoryScreenDialog.findViewById(R.id.btnPlayAgain);
        Button btnHome = victoryScreenDialog.findViewById(R.id.btnHome);

        tvTotalTime.setText(totalTime);
        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // returning to MainActivity
                finish();
            }
        });

        btnPlayAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                victoryScreenDialog.dismiss();
                resetGame();
            }
        });

        victoryScreenDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        victoryScreenDialog.show();
    }


    /**
     * formats the width and height of the image buttons to be perfect squares
     * @param row - the linear layout for a given row
     * @param btn - the relative layout for a given button
     */
    public void formatRows(LinearLayout row, ImageButton btn) {

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) btn.getLayoutParams();

        int w = row.getWidth();

        params.height = (w / NUM_COLS);
        params.width = (w / NUM_COLS);

        btn.setLayoutParams(params);
    }
}