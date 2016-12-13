import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A JUnit test class for the Wari game.
 * 
 * @author Mitch Parry
 * @version 2013-09-18
 * 
 */
public class WariTest
{
    public static final String BOARD =
        "Computer side \n"
            + "-------------------------------------\n"
            + "| a11 | a12 | a13 | a14 | a15 | a16 |\n"
            + "-------------------------------------\n"
            + "| a21 | a22 | a23 | a24 | a25 | a26 |\n"
            + "-------------------------------------\n"
            + "Human side";
    public static Vector<String> humanMoves = new Vector<String>(6);
    public static Vector<String> computerMoves = new Vector<String>(6);
    final static int FIRST_6_BITS = 0x3F;
    final static double EPS = 1e-9;
    ByteArrayOutputStream outContent;
    PrintStream stdout;
    Wari game;

    /**
     * Helper method for learning the moves based on the display output.
     * 
     * @param player
     *            The human or computer side of the board.
     * @param move
     *            The current move.
     * @return The index of the bin selected by this move.
     */
    private static int findMove(AbstractGame.Player player, String move)
    {
        Wari game = new Wari();
        if (player == AbstractGame.Player.computer)
        {
            // make the human play first.
            game.simulateMove(humanMoves.get(0));
        }
        game.simulateMove(move);
        GameState state = getGameState(game);
        int[] board = state.getBoard();
        for (int i = 0; i < 6; i++)
        {
            if (player == AbstractGame.Player.human)
            {
                if (board[6 + i] == 0)
                {
                    return i;
                }
            }
            else if (board[i] == 0)
            {
                {
                    return i;
                }
            }
        }
        throw new IllegalArgumentException("Failed to learn " + player
            + " move \"" + move + "\".\nIt does not set any of the elements "
            + (player == AbstractGame.Player.human ? "a2" : "a1")
            + "* on the board to zero:\n" + BOARD + "\n");
    }

    /**
     * Get the vector of move strings for the player. Index 0 corresponds to the
     * first column of the game board.
     * 
     * {@literal
     * |---|---|---|---|---|---|
     * | 0 | 1 | 2 | 3 | 4 | 5 |<-- Computer
     * |---|---|---|---|---|---|
     * | 0 | 1 | 2 | 3 | 4 | 5 |<-- Human
     * |---|---|---|---|---|---|
     * }
     * 
     * @param player
     *            Whether this is for the human or computer.
     * @return The vector of strings that make the play in the corresponding
     *         column.
     */
    public static Vector<String> findMoves(AbstractGame.Player player)
    {
        Wari game = new Wari();
        Vector<String> someMoves = game.computeMoves();
        Vector<String> moves = new Vector<String>(6);
        moves.setSize(6);
        if (player == AbstractGame.Player.computer)
        {
            // make the human play first
            game.simulateMove(someMoves.get(0));
        }
        someMoves = game.computeMoves();
        if (someMoves.size() != 6)
        {
            throw new IllegalArgumentException("Failed to learn moves: "
                + "computeMoves must return 6 moves for human's or computer's"
                + " first move.");
        }

        // determine what each move does.
        int finished = 0;
        for (int j = 0; j < someMoves.size(); j++)
        {

            int i = findMove(player, someMoves.get(j));
            moves.set(i, someMoves.get(j));
            finished |= 0x1 << i;
        }
        if ((finished & FIRST_6_BITS) != FIRST_6_BITS)
        {
            throw new IllegalArgumentException("Failed to learn moves. Some "
                + "bins are unplayable.\nLearned human moves:\n" + moves);
        }
        return moves;
    }

    /**
     * Runs before all tests in this class.
     */
    @BeforeClass
    public static void beforeClass()
    {
        PrintStream stdout = System.out;
        humanMoves = findMoves(AbstractGame.Player.human);
        computerMoves = findMoves(AbstractGame.Player.computer);

        System.setOut(stdout);
        System.out.println(computerMoves);
        System.out.println(humanMoves);
    }

    /**
     * Runs before each test case.
     */
    @Before
    public void before()
    {
        // Capture output for comparison
        outContent = new ByteArrayOutputStream();
        stdout = System.out;
        System.setOut(new PrintStream(outContent));
        game = new Wari();
    }

    /**
     * Runs after each test case.
     */
    @After
    public void after()
    {
        System.setOut(stdout);
    }

    /**
     * Reads the board and score from the scanner.
     * 
     * @param scan
     *            The input scanner.
     * @param state
     *            The output state.
     */
    private static void updateBoardAndScore(Scanner scan, GameState state)
    {
        while (scan.hasNext())
        {
            if (scan.hasNextInt())
            {
                state.assignNext(scan.nextInt());
            }
            else
            {
                scan.next();
            }
        }
        if (state.getNumAssigned() < 14)
        {
            scan.close();
            throw new IllegalArgumentException(
                "Wari.displayStatus should print at least "
                    + "14 isolated integers.");
        }
        if (state.getNumAssigned() > 14)
        {
            System.err.println("Warning: found more than 14 isolated "
                + "integers printed by displayStatus.  "
                + "Ignoring additional integers.");
        }
    }

    /**
     * Gets an array that indicates which moves are legal.
     * 
     * @param game
     *            The game.
     * @return Which of the board entries are legal moves.
     */
    private static boolean[] getIsLegal(Wari game)
    {
        boolean[] isLegal = new boolean[6];
        for (int i = 0; i < 6; i++)
        {
            if (game.nextMover() == AbstractGame.Player.computer
                && computerMoves.size() == 6)
            {
                isLegal[i] = game.isLegal(computerMoves.get(i));
            }
            else if (humanMoves.size() == 6)
            {
                isLegal[i] = game.isLegal(humanMoves.get(i));
            }
            else
            {
                isLegal[i] = false;
            }
        }
        return isLegal;
    }

    /**
     * Gets the current game state from the display.
     * 
     * @param game
     *            The game of wari.
     * @return The game state containing the board and score.
     */
    public static GameState getGameState(Wari game)
    {
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));

        game.displayStatus();

        String output = outContent.toString();
        Scanner scan = new Scanner(output);
        GameState state = new GameState();
        updateBoardAndScore(scan, state);
        scan.close();

        state.setIsGameOver(game.isGameOver());
        state.setIsLegal(getIsLegal(game));
        state.setLegalMoves(game.computeMoves());
        state.setEvaluate(game.evaluate());
        return state;
    }

    /**
     * A helper class to hold current game state.
     * 
     * @author Mitch Parry
     * @version 2013-09-18
     * 
     */
    public static class GameState
    {
        private int[] board = new int[12];
        private int humanScore;
        private int computerScore;
        private int numAssigned = 0;
        private boolean gameOver;
        private boolean[] isLegal = new boolean[6];
        private Vector<String> legalMoves;
        private double evaluate;

        /**
         * Sets the next value of the game state to i.
         * 
         * @param i
         *            The value to assign.
         */
        public void assignNext(int i)
        {
            if (numAssigned < 12)
            {
                board[numAssigned++] = i;
            }
            else if (numAssigned == 12)
            {
                computerScore = i;
                numAssigned++;
            }
            else if (numAssigned == 13)
            {
                humanScore = i;
                numAssigned++;
            }
            else
            {
                numAssigned++;
            }
        }

        /**
         * @return the number of items assigned.
         */
        public int getNumAssigned()
        {
            return numAssigned;
        }

        /**
         * @return a copy of the board.
         */
        public int[] getBoard()
        {
            return board.clone();
        }

        /**
         * @return the human score.
         */
        public int getHumanScore()
        {
            return humanScore;
        }

        /**
         * @return the computer score.
         */
        public int getComputerScore()
        {
            return computerScore;
        }

        /**
         * @param gameOver
         *            Sets isGameOver.
         */
        public void setIsGameOver(boolean gameOver)
        {
            this.gameOver = gameOver;
        }

        /**
         * @return whether or not the game is over.
         */
        public boolean getIsGameOver()
        {
            return gameOver;
        }

        /**
         * @param isLegal
         *            Sets isLegal.
         */
        public void setIsLegal(boolean[] isLegal)
        {
            if (isLegal.length != 6)
            {
                throw new IllegalArgumentException(
                    "setIsLegal: input should have length = 6: "
                        + isLegal.length);
            }
            this.isLegal = isLegal.clone();
        }

        /**
         * @return whether or not the move is legal.
         */
        public boolean[] getIsLegal()
        {
            return isLegal.clone();
        }

        /**
         * @param legalMoves
         *            Sets legalMoves.
         */
        public void setLegalMoves(Vector<String> legalMoves)
        {
            this.legalMoves = new Vector<String>(legalMoves);
            Collections.sort((List<String>) this.legalMoves);
        }

        /**
         * @return the list of legal moves.
         */
        public Vector<String> getLegalMoves()
        {
            return new Vector<String>(legalMoves);
        }

        /**
         * @param evaluate
         *            Sets evaluate.
         */
        public void setEvaluate(double evaluate)
        {
            this.evaluate = evaluate;
        }

        /**
         * @return whether or not the game is over.
         */
        public double getEvaluate()
        {
            return evaluate;
        }

        /**
         * Compares this state to the parameter and returns true if the two have
         * the same fields.
         * 
         * @param obj
         *            The state to compare against.
         * @return True if this state is the same.
         */
        public boolean equals(Object obj)
        {
            if (obj instanceof WariTest.GameState)
            {
                GameState state = (GameState) obj;
                return Arrays.equals(board, state.board)
                    && humanScore == state.humanScore
                    && computerScore == state.computerScore
                    && gameOver == state.gameOver
                    && Arrays.equals(getIsLegal(), state.getIsLegal())
                    && legalMoves.equals(state.legalMoves)
                    && Math.abs(Math.signum(evaluate)
                        - Math.signum(state.evaluate)) < EPS;
            }
            return false;
        }

        /**
         * ToString method.
         * 
         * @return The string representation of the game state.
         */
        public String toString()
        {
            String s = "";
            for (int i = 0; i < 6; i++)
            {
                s += String.format("%3d", board[i]);
            }
            s += "\n";
            for (int i = 6; i < 12; i++)
            {
                s += String.format("%3d", board[i]);
            }
            s += String.format("\nComputer: %d\n", computerScore);
            s += String.format("Human: %d\n", humanScore);
            s += "Game over: " + gameOver + "\n";
            s += "isLegal[] = ";
            for (int i = 0; i < 6; i++)
            {
                s += isLegal[i];
            }
            s += "\ncomputeMoves: " + legalMoves + "\n";
            s += "Sign(evaluate()): " + Math.signum(evaluate);
            return s;
        }

    }

    /**
     * Read a vector of integers from the scanner.
     * 
     * @param scan
     *            The scanner
     * @return A vector of integers.
     */
    private int[] readIntArray(Scanner scan)
    {
        if (!scan.hasNextLine())
        {
            return null;
        }
        String line = scan.nextLine();
        line = line.replaceAll("\\[", "");
        line = line.replaceAll("\\]", "");
        String[] tokens = line.split(", ");
        Vector<Integer> v = new Vector<Integer>();
        for (String s : tokens)
        {
            if (s.length() > 0)
            {
                v.add(Integer.parseInt(s));
            }
        }
        int[] ret = new int[v.size()];
        int k = 0;
        for (int i : v)
        {
            ret[k++] = i;
        }
        return ret;
    }

    /**
     * Helper method to trim the 12 element array from the test cases to the 6
     * element array needed here.
     * 
     * @param scan
     *            The input scanner.
     * @param player
     *            The current player.
     * @return The array of 6 True/False values.
     */
    private boolean[] readIsLegalArray(Scanner scan, AbstractGame.Player player)
    {
        boolean[] isLegal12 = readBooleanArray(scan);
        boolean[] isLegal = new boolean[6];
        for (int i = 0; i < 6; i++)
        {
            isLegal[i] = player == AbstractGame.Player.human ? isLegal12[i + 6]
                : isLegal12[i];
        }
        return isLegal;
    }

    /**
     * Read a vector of booleans from the scanner.
     * 
     * @param scan
     *            The scanner
     * @return A vector of booleans.
     */
    private boolean[] readBooleanArray(Scanner scan)
    {
        if (!scan.hasNextLine())
        {
            return null;
        }
        String line = scan.nextLine();
        line = line.replaceAll("\\[", "");
        line = line.replaceAll("\\]", "");
        String[] tokens = line.split(", ");
        Vector<Boolean> v = new Vector<Boolean>();
        for (String s : tokens)
        {
            if (s.length() > 0)
            {
                v.add(Boolean.parseBoolean(s));
            }
        }
        boolean[] ret = new boolean[v.size()];
        int k = 0;
        for (boolean b : v)
        {
            ret[k++] = b;
        }
        return ret;
    }

    /**
     * Reads an integer from the scanner.
     * 
     * @param scan
     *            The scanner.
     * @return the integer.
     */
    private int readInt(Scanner scan)
    {
        return Integer.parseInt(scan.nextLine().trim());
    }

    /**
     * Reads a boolean from the scanner.
     * 
     * @param scan
     *            the scanner.
     * @return the boolean.
     */
    private Boolean readBoolean(Scanner scan)
    {
        return Boolean.parseBoolean(scan.nextLine().trim());
    }

    /**
     * Reads a double from the scanner.
     * 
     * @param scan
     *            the scanner.
     * @return the double.
     */
    private Double readDouble(Scanner scan)
    {
        return Double.parseDouble(scan.nextLine().trim());
    }

    /**
     * Reads the game state from a scanner.
     * 
     * @param scan
     *            The scanner.
     * @param moves
     *            the sequence of moves.
     * @return The game state.
     */
    private GameState readGameState(Scanner scan, int[] moves)
    {
        AbstractGame.Player player = moves.length % 2 == 0
            ? AbstractGame.Player.human : AbstractGame.Player.computer;
        GameState state = new GameState();
        int[] board = readIntArray(scan);
        for (int i : board)
        {
            state.assignNext(i);
        }
        state.assignNext(readInt(scan));
        state.assignNext(readInt(scan));
        state.setIsGameOver(readBoolean(scan));
        state.setIsLegal(readIsLegalArray(scan, player));
        int[] legalMoves = readIntArray(scan);
        Vector<String> legal = new Vector<String>();
        for (int i : legalMoves)
        {
            if (i < 6)
            {
                legal.add(humanMoves.get(i));
            }
            else
            {
                legal.add(computerMoves.get(11 - i));
            }
        }
        state.setLegalMoves(legal);
        state.setEvaluate(readDouble(scan));
        return state;
    }

    /**
     * Simulates a game with the provided moves and returns it.
     * 
     * @param moves
     *            The moves.
     * @return The game.
     */
    private Wari simulateGame(int[] moves)
    {
        Wari game = new Wari();
        return simulateGame(moves, game);
    }

    /**
     * Simulates a game with the provided moves and returns it.
     * 
     * @param moves
     *            The moves.
     * @param game
     *            The game already in progress.
     * @return The game.
     */
    private Wari simulateGame(int[] moves, Wari game)
    {
        boolean human = true;
        for (int move : moves)
        {
            if (human)
            {
                game.simulateMove(humanMoves.get(move));
            }
            else
            {
                game.simulateMove(computerMoves.get(move));
            }
            human = !human;
        }
        return game;
    }

    /**
     * Simulates the game using the provided moves and returns the final game
     * state.
     * 
     * @param moves
     *            The moves.
     * @return The game state.
     */
    private GameState simulateGameState(int[] moves)
    {
        Wari game = simulateGame(moves);
        GameState state = getGameState(game);
        return state;
    }

    /**
     * Prints helpful information for the student to debug the game state.
     * 
     * @param prev
     *            The previous game state
     * @param correct
     *            The correct new game state
     * @param actual
     *            The student's game state
     * @param moves
     *            The moves to get to this point.
     * @return A string representation of the test failure.
     */
    private String printState(GameState prev, GameState correct,
        GameState actual, int[] moves)
    {
        String s = "Game state is incorrect.\n";
        if (prev == null)
        {
            s += "after the following moves:\n" + Arrays.toString(moves) + "\n";
        }
        s += "Previous state:\n" + prev + "\n";
        if (moves.length > 0)
        {
            if (moves.length % 2 == 1)
            {
                // human
                s += "Human moves at index "
                    + humanMoves.get(moves[moves.length - 1])
                    + " from left\n\n";
            }
            else
            {
                // computer
                s += "Computer moves at index "
                    + computerMoves.get(moves[moves.length - 1])
                    + " from left\n\n";
            }
        }
        s += "Correct state:\n" + correct + "\n";
        s += "Your state:\n" + actual + "\n";
        return s;
    }

    /**
     * Tests whether the states match.
     * 
     * @param correctState
     *            The correct game state.
     * @param state
     *            The actual game state.
     * @param moves
     *            The moves made to get to this state.
     * @param previousState
     *            the correct previous state.
     */
    private void testState(GameState correctState, GameState state,
        int[] moves, GameState previousState)
    {
        String message = printState(previousState, correctState, state, moves);
        assertArrayEquals("Error: board.\n" + message, correctState.getBoard(),
            state.getBoard());
        assertEquals("Error: computer score\n" + message,
            correctState.getComputerScore(), state.getComputerScore());
        assertEquals("Error: human score\n" + message,
            correctState.getHumanScore(), state.getHumanScore());
        assertTrue("Error: gameOver()\n" + message,
            correctState.getIsGameOver() == state.getIsGameOver());
        if (!correctState.getIsGameOver())
        {
            // only test the legal moves if the game is not over.
            // only test the moves from the current player (not the opponent)
            // because the moves may have the same name.
            assertTrue(
                "Error: isLegal\n" + message,
                Arrays.equals(correctState.getIsLegal(),
                    state.getIsLegal()));
            assertTrue("Error: computeMoves()\n" + message,
                correctState.getLegalMoves().equals(state.getLegalMoves()));
        }
        assertEquals("Error: evaluate()\n" + message,
            Math.signum(correctState.getEvaluate()),
            Math.signum(state.getEvaluate()), EPS);
    }

    /**
     * Returns the logical 'or' between first and last 6 booleans.
     * 
     * @param b
     *            The input array of booleans
     * @return A half-size array with logical ors.
     */
    /*    private static boolean[] computeOr(boolean[] b)
        {
            boolean[] ret = new boolean[6];
            for (int i = 0; i < 6; i++)
            {
                ret[i] = b[i] || b[i + 6];
            }
            return ret;
        }
     */
    /**
     * Tests the clone method.
     */
    @Test
    public void testClone()
    {
        int[] moves = {
            2, 1, 0, 0, 1, 2, 2, 4, 5, 0, 4, 3, 2, 4, 3, 5, 4, 5, 0, 3, 1, 0
        };
        Wari game1 = simulateGame(moves);
        Wari game2 = game1.clone();

        GameState state1 = getGameState(game1);
        GameState state2 = getGameState(game2);

        assertFalse("game and game.clone() reference the same object.",
            game1 == game2);
        assertTrue("Wari.clone: game does not equal game.clone()",
            state1.equals(state2));

        int[] moves2 = {
            1, 5, 5, 1, 5, 2, 1, 5, 0, 3, 3, 1, 0, 5, 5, 4, 5, 3, 3, 1, 2, 1
        };
        game1 = simulateGame(moves2, game1);
        state1 = getGameState(game1);

        assertFalse("Wari.clone: changing the clone changes the original.",
            state1.equals(state2));

        game2 = simulateGame(moves2, game2);
        state2 = getGameState(game2);

        assertTrue("Wari.clone: after cloning the games do not "
            + "progress in the same way.", state1.equals(state2));

    }

    /**
     * Runs the AbstractGame repeatPlay method.
     */
    @Test
    public void testRepeatPlay()
    {
        final int MAX_MOVES = 10000;
        String data = "";
        for (int i = 0; i < MAX_MOVES; i++)
        {
            data += humanMoves.get(i % 6) + "\n";
        }
        InputStream stdin = System.in;
        try
        {
            System.setIn(new ByteArrayInputStream(data.getBytes()));
            AbstractGame.repeatPlay("Wari", 4);
        }
        catch (Exception e)
        {
            System.err.println("No problem, just testing that it runs:\n"
                + e.getMessage());
        }
        finally
        {
            System.setIn(stdin);
        }

        Wari game = new Wari();
        game.displayMessage("hello");
        game.winning();
        game.movesCompleted();

    }

    /**
     * Gives points for display.
     */
    @Test
    public void testDisplay()
    {
        simulateGameState(new int[] {});
        System.out.println("In order to get this far, your display must work.");

    }

    /**
     * Test after zero moves.
     * 
     * @throws FileNotFoundException
     *             if the test file does not exist.
     */
    @Test
    public void test0() throws FileNotFoundException
    {
        Scanner scan = new Scanner(new File("testCases0.txt"));
        int[] moves = readIntArray(scan);
        GameState correctState = readGameState(scan, moves);
        scan.close();

        GameState state = simulateGameState(moves);
        testState(correctState, state, moves, null);
    }

    /**
     * Test after 1 move.
     * 
     * @throws FileNotFoundException
     *             if the test cases file is not found.
     */
    @Test
    public void test1() throws FileNotFoundException
    {
        Scanner scan = new Scanner(new File("testCases1.txt"));
        while (scan.hasNextLine())
        {
            int[] moves = readIntArray(scan);
            GameState correctState = readGameState(scan, moves);
    
            GameState state = simulateGameState(moves);
            testState(correctState, state, moves, null);
        }
        scan.close();
    }

    /**
     * Test after 2 moves.
     * 
     * @throws FileNotFoundException
     *             if the test cases file is not found.
     */
    @Test
    public void test2() throws FileNotFoundException
    {
        Scanner scan = new Scanner(new File("testCases2.txt"));
        while (scan.hasNextLine())
        {
            int[] moves = readIntArray(scan);
            GameState correctState = readGameState(scan, moves);

            GameState state = simulateGameState(moves);
            testState(correctState, state, moves, null);
        }
        scan.close();
    }

    /**
     * Test after 3 moves.
     * 
     * @throws FileNotFoundException
     *             if the test cases file is not found.
     */
    @Test
    public void test3() throws FileNotFoundException
    {
        Scanner scan = new Scanner(new File("testCases3.txt"));
        while (scan.hasNextLine())
        {
            int[] moves = readIntArray(scan);
            GameState correctState = readGameState(scan, moves);

            GameState state = simulateGameState(moves);
            testState(correctState, state, moves, null);
        }
        scan.close();
    }

    /**
     * Test after 3 moves.
     * 
     * @throws FileNotFoundException
     *             if the test cases file is not found.
     */
    @Test
    public void test4() throws FileNotFoundException
    {
        Scanner scan = new Scanner(new File("testCases4.txt"));
        while (scan.hasNextLine())
        {
            int[] moves = readIntArray(scan);
            GameState correctState = readGameState(scan, moves);

            GameState state = simulateGameState(moves);
            testState(correctState, state, moves, null);
        }
        scan.close();
    }

    /**
     * Test after M random moves.
     * 
     * @throws FileNotFoundException
     *             if the test cases file is not found.
     */
    @Test
    public void testM() throws FileNotFoundException
    {
        for (int i = 0; i < 5; i++)
        {
            Scanner scan = new Scanner(new File("testCasesM" + i + ".txt"));
            GameState previousState = null;
            while (scan.hasNextLine())
            {
                int[] moves = readIntArray(scan);
                GameState correctState = readGameState(scan, moves);

                GameState state = simulateGameState(moves);
                testState(correctState, state, moves, previousState);
                previousState = state;
            }
            scan.close();
        }

    }

}
