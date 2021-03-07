package aus10.games.memorygame;

import android.widget.ImageButton;
import android.widget.LinearLayout;

import java.util.Random;

/**
 * A class of functions used by each game mode
 */
public class GameHelper {


    /**
     * randomly assigns images to spots in the 2d-array, ensuring each image gets used exactly twice
     * @param numCards - number of cards in the game mode
     * @param numPics - number of pics in the game mode
     * @param numRows - number of rows in the game mode
     * @param numCols - number of pics in the game mode
     * @return a 2d array of integers that correspond to an image id in the game mode class
     */
    public int[][] assignRandImages(int numCards, int numPics, int numRows, int numCols) {

        int[] imageCount = new int[numPics];
        int[][] imageNum = new int[numRows][numCols];         // 2d-array of ints that correspond to a picture
        int numCardsSet = 0;                                  // number of cards that have been set thus far
        Random rand = new Random();                           // new random variable

        // setting imageCount array to all 0s
        for(int i = 0; i < numPics; i++) {
            imageCount[i] = 0;
        }

        // loop until all cards are set
        while(numCardsSet < numCards) {

            // loop for all images
            for(int x = 0; x < numPics; x++) {

                // making sure count is reset before entering loop
                imageCount[x] = 0;

                // loop twice for each image
                while(imageCount[x] < 2) {

                    // generates random numbers for bounds of array
                    int i = rand.nextInt(numRows);
                    int j = rand.nextInt(numCols);

                    // checks that the random spot is still the default before setting a new one
                    if (imageNum[i][j] == 0) {

                        // since 0 is default, setting values in imageNum to x+1
                        imageNum[i][j] = (x+1);
                        imageCount[x]++;
                        numCardsSet++;
                    }
                }
            }
        }

        return imageNum;
    }


    /**
     * takes the time in milliseconds and converts it to minutes and seconds
     * @return - returns the time as a string from the formatTime function
     */
    public String getTimeText(double time) {

        int roundedTime = (int) Math.round(time);
        int seconds = ((roundedTime % 86400) % 3600) % 60;
        int minutes = ((roundedTime % 86400) % 3600) / 60;

        return formatTime(minutes, seconds);
    }


    /**
     * formats the given minutes and seconds into a string
     * @param min - the number of minutes
     * @param sec - the number of seconds
     * @return - the minutes and seconds in a string
     */
    public String formatTime(int min, int sec) {
        return String.format("%02d",min) + " : " + String.format("%02d",sec);
    }


    /**
     * generates a random alpha-numeric game code
     * @return the final created string
     */
    static String genGameCode() {
        String gameCodeCharacters = "ABCDEFGHJKLMNPQRSTUVWXYZ" + "123456789";

        StringBuilder builder = new StringBuilder(4);       // holds the final created string

        // loops for each letter and generates a random index
        // to pull a character from and adds to final string
        for(int i = 0; i < 4; i++){
            int index = (int) (gameCodeCharacters.length() * Math.random());
            builder.append(gameCodeCharacters.charAt(index));
        }

        return builder.toString();
    }


    /**
     * formats the width and height of the image buttons to be perfect squares
     * @param row - the linear layout for a given row
     * @param btn - the relative layout for a given button
     * @param numCols - number of columns in the game mode
     */
    public void formatRows(LinearLayout row, ImageButton btn, int numCols) {

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) btn.getLayoutParams();

        int w = row.getWidth();

        params.height = (w / numCols);
        params.width = (w / numCols);

        btn.setLayoutParams(params);
    }

}
