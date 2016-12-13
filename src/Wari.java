
import java.util.Arrays;
import java.util.Vector;
/**
 * wari game class that extends the AbstractGame class.
 * @version 1
 * @author John
 *
 */
public class Wari extends AbstractGame implements Cloneable
{
    public static final int TWENTY_FIVE = 25;
    private int hBeans;
    private int cBeans;
    private int[]wariBoard;



    /**
     * constructor.
     */
    public Wari()
    {
        hBeans = 0;
        cBeans = 0;
        wariBoard = new int [12];


        Arrays.fill(wariBoard, 4);

    }


    /**
     * displayStatus gameboard.
     */
    public void displayStatus()
    {
        System.out.println("Computer's side");
        System.out.println("|-----------------------|");
        System.out.println("| " + wariBoard[11] + " | " + wariBoard[10] + " | "
            + wariBoard[9] + " | " + wariBoard[8] + " | " + wariBoard[7]
                + " | " + wariBoard[6] + " |");
        System.out.println("-----------------------");
        System.out.println("| " + wariBoard[0] + " | " + wariBoard[1] + " | "
            + wariBoard[2] + " | " + wariBoard[3] + " | " + wariBoard[4]
                + " | " + wariBoard[5] + " |");
        System.out.println("-----------------------");
        System.out.println("Human's side");
        System.out.println();
        System.out.println("Computer's score: " + cBeans);
        System.out.println("Human's score   : " + hBeans);
        System.out.println();
        System.out.println(nextMover() + " it's your turn.");
    }
    /**
     * wari clone method.
     * @return answer.
     */
    protected Wari clone()
    {
        Wari answer;

        try
        {
            answer = (Wari) super.clone();

        }
        catch (Exception e)
        {
            throw new RuntimeException(
                "This class does not implement Cloneable.");
        }

        for (int p = 0; p < 12; p++)
        {
            answer.wariBoard[p] = this.wariBoard[p];
        }
        answer.hBeans = this.hBeans;
        answer.cBeans = this.cBeans;





        return answer;
    }

    /**
     * vector of move strings.
     * @return moves is the vector.
     */
    protected Vector<String> computeMoves()
    {
        Vector<String> moves = new Vector<String>();
        int i;

        if (nextMover() == AbstractGame.Player.human)
        {

            for (i = 0; i < 6; i++)
            {
                String s = String.valueOf(i);
                if (this.isLegal(s))
                {
                    moves.addElement(s);
                }
            }
        }



        if (nextMover() == AbstractGame.Player.computer)
        {
            for (int n = 6; n < 12; n++)
            {
                String t = String.valueOf(n);
                if (isLegal(t))
                {
                    moves.addElement(String.valueOf(t));
                }
            }



        }



        return moves;









    }



    /**
     * @param move is the move.
     */
    public void makeMove(String move)
    {
        int startPos = Integer.parseInt(move);
        int beans = 0;
        int index = startPos;

        beans = wariBoard[index];
        wariBoard[index] = 0;
        index = (index + 1) % 12;

        while (beans > 0)
        {
            if (index == startPos)
            {
                index = (index + 1) % 12;  
            }

            else if (index == 11)
            {
                index = (index + 1) % 12;
            }
            wariBoard[index]++; 
            beans--;
            index = (index + 1) % 12;
        }



        if (wariBoard[index] > 1 &&  wariBoard[index] < 4) 
        {
            if (startPos / 6 != index / 6)
            {

                for (int i = index; i >= index / 6 * 6 
                    && (wariBoard[i] > 1 || wariBoard[i] < 4); i--)
                {
                    if (startPos / 6 == 0)
                    {
                        hBeans += wariBoard[i];
                    }
                    else
                    {
                        cBeans += wariBoard[i];

                    }
                }
            }
        }
    }

    /**
     * @param move is move.
     * @return true if true.
     */
    public boolean isLegal(String move)
    {
        int index = Integer.parseInt(move);

        if (nextMover() == Player.computer && index > 11)
        {

            return false;
        }
        else if (nextMover() == Player.computer && index < 6)
        {
            return false;
        }

        else if (nextMover() == Player.computer && wariBoard[index] <= 0)
        {
            return false;
        }

        else if (nextMover() == Player.human && index > 5)
        {
            return false;
        }
        else if (nextMover() == Player.human && index < 0)
        {
            return false;
        }

        else if (nextMover() == Player.human && wariBoard[index] <= 0)
        {
            return false;
        }

        return true;

    }

    /**
     * 
     * @param args is a main args array thing.
     */
    public static void main(String args[])
    {
        Wari wari = new Wari();
        wari.displayStatus();
        wari.makeMove("11");
        wari.displayStatus();
        wari.makeMove("11");
        wari.displayStatus();
        wari.isLegal("11");


   

    }



    /**
     * @return evaluationNumber to determine if computer.
     */
    protected double evaluate()
    {
        double evaluationNumber;
        evaluationNumber = cBeans - hBeans;
        return evaluationNumber;

    }



    /**
     * @return true if game is over.
     */
    protected boolean isGameOver()
    {
        if (hBeans >= TWENTY_FIVE)
        {
            return true;
        }
        if (cBeans >= TWENTY_FIVE)
        {
            return true;
        }
        if (computeMoves().isEmpty())
        {
            return true;
        }
        return false;

    }
}