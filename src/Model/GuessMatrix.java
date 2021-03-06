package Model;

import java.util.ArrayList;

/**
 * This class keeps track of the people who have answered yes to a specific guess.
 * It makes inferences about who owns what cards based on what information is known and inputted into it.
 */
public class GuessMatrix {
    // a 3 by 3 matrix 0 -> no info, 1 -> has the card, -1 -> cannot have the card, (Id Number) x (Card) where matrix[i][j] refers to the player in idNums[i] and  j = 0 -> "Suspect", 1-> "Room", 2 -> "Weapon"
    private int[][] matrix;
    private ArrayList<Integer> acceptedIDs; // The id's of everybody we know has one of the cards in this guess.
    private ArrayList<Integer> rejectedIDs; // The id's of everybody we know doesn't have one of the cards in this guess.
    private int solvedNum;                  // The number of 1's in this matrix (Cards we know a player has).
    private int strikeNum;                  // The number of -1's in this matrix (Cards we know a player does not have).
    private ArrayList<Card> solvedCards;    // Stores cards that we have deduced that a player must have
    private ArrayList<Card> cardsToStrike;  // Stores cards that we have deduced that a player cannot have
    private Guess guess;                    // The guess that this matrix is based on.
    private int playerNum;                  // The number of players in the game

    // Default Constructor for the GuessMatrix object
    public GuessMatrix(Guess guess, int playerNum) throws ClueException {
        this.playerNum = playerNum;
        this.guess = guess;
        matrix = new int[3][3];
        acceptedIDs = new ArrayList<>();
        rejectedIDs = new ArrayList<>();
        solvedCards = new ArrayList<>();
        cardsToStrike = new ArrayList<>();
        solvedNum = 0;
        strikeNum = 0;
    }

    // only adds a person to the idNums if they are not already in it. ie only a new person with that information
    public boolean[] addID(int idNum, int[] values) throws ClueException {
        for (int num : values)
            if (! (num == -1 || num == 0 || num == 1))
                throw new ClueException(String.format("Fatal Error: Attempting to add value %d to gm: %s other then -1, 0, or 1 ", num, toString()));
        int witnessNum = acceptedIDs.size();
        if (! isMember(idNum, acceptedIDs)) {
            acceptedIDs.add(idNum);
            for (int i = 0; i < values.length; i++) {
                if (values[i] == -2)
                    matrix[witnessNum][i] = -1;
                else
                    matrix[witnessNum][i] = values[i];
                if (values[i] == 1) {
                    updateColumnOne(i);
                    solvedNum++;
                }
            }
            return updateMatrix();
        }
        else
            return noChange();
    }

    // Returns a boolean[] that signifies no change in information
    private boolean[] noChange() {
        boolean[] noChange = new boolean[3];
        for (int i = 0; i < 3; i++)
            noChange[i] = false;
        return noChange;
    }

    // Adds the supplied idNum to the list of players that do not have any of the cards in this guess
    public void rejectID(int idNum) {
        if (! isMember(idNum, rejectedIDs))
            rejectedIDs.add(idNum);
    }

    // accessor for the total number of people that have given positive or negative information on this guess
    public int getTotalNumber() {
        return  acceptedIDs.size() + rejectedIDs.size();
    }

    /*
     * Only use this method when you know that the parameter card is a card in guess
     * Used when a card is found searches through and updates a column, when that card locaFrtion is determined.
     * Does not add the parameter card to the foundList because this function is only used when we know what a cards location is in the game object.
     * Invokes updateMatrix() which could add Cards to newFoundCards or strikeList.
     */
    public boolean[] updateCardHas(Card card) throws ClueException {
        int idNum = card.getPlayerID();
        String type = card.getType();
        int typeIndex = getIndex(type);
        if (typeIndex == 3)
            throw new ClueException(String.format("Invalid Type. Card: %s, Type: %s", card.getName(), type));
        else {
            for (int i = 0; i < acceptedIDs.size(); i++) {
                if (acceptedIDs.get(i) == idNum) {
                    solvedNum++;
                    if (matrix[i][typeIndex] == -1)
                        throw new ClueException(String.format("Error Trying to add card %s, to player: %d, in guess: %s, when value is -1", card.toString(), card.getPlayerID(), guess.toString()));
                    matrix[i][typeIndex] = 1;
                }
                else
                    setMinus1(i, typeIndex);
            }
        }
        return updateMatrix();
    }

    // strikes the card for the playerID that is contained in the card.
    public boolean[] updateCardStrike(Card card) throws ClueException {
        int playerID = card.getPlayerID();
        for (int i = 0; i < acceptedIDs.size(); i++)
            if (acceptedIDs.get(i) == playerID) {
                int columnIndex = getIndex(card.getType());
                if (matrix[i][columnIndex] == 1)
                    throw new ClueException(String.format("Fatal Error UCS is setting 1 to -1 in \n%s", toString()));
                matrix[i][columnIndex] = -1;

            }
        return updateMatrix();
    }

    // updates this matrix returns true if there are new inferred cards and false if not
    private boolean[] updateMatrix() throws ClueException {
        boolean[] flags = new boolean[3];
        int strikeTemp = strikeNum;
        int solvedTemp = solvedNum;
        int witnessNum = acceptedIDs.size();
        if (witnessNum == 1)
            updateMatrixOne();
        if (witnessNum == 2)
            updateMatrixTwo();
        if (witnessNum == 3)
            updateMatrixThree();
        flags[0] = ! (solvedTemp == solvedNum);
        flags[1] = ! (strikeTemp == strikeNum);
        flags[2] = getTotalNumber() == playerNum && acceptedIDs.size() < 3;
        return flags;

    }

    // updates this matrix when there is only one ID in this matrix
    private void updateMatrixOne() throws ClueException {
        int rowSum = sumRow(0);
        if (rowSum == -2)
            updateRowMinusTwo(0);
    }

    // Updates this matrix when there are two ID's in this matrix
    private void updateMatrixTwo() throws ClueException {
        int rowSum;
        for (int i = 0; i < 2; i++) {
            rowSum = sumRow(i);
            if (rowSum == -2)
                updateRowMinusTwo(i);
        }
    }

    // Updates this matrix when there are three ID's in this matrix
    private void updateMatrixThree() throws ClueException {
        boolean flag = true;
        int colSum, rowSum;
        while (flag) {
            flag = false;
            for (int i = 0; i < 3; i++) {
                colSum = sumColumn(i);
                rowSum = sumRow(i);
                if (colSum == -2) {
                    updateColumnMinusTwo(i);
                    flag = true;
                }
                if (colSum == 1) {
                    updateColumnOne(i);
                    flag = true;
                }
                if (rowSum == -2) {
                    updateRowMinusTwo(i);
                    flag = true;
                }
                if (rowSum == 1) {
                    updateRowOne(i);
                    flag = true;
                }
            }
        }
    }

    // This method is called when there is a 1 and two 0's in a row. It sets the two 0's to -1
    // This is a correct inference only when there are 3 ID's in the matrix
    private void updateRowOne(int rowIndex) throws ClueException {
        for (int i = 0; i < 3; i++)
            if (matrix[rowIndex][i] == 0)
                setMinus1(rowIndex, i);
    }

    // This method is called when there is a 1 and two 0's in a column. It sets the two 0's to -1
    // Note this is only valid when there are 3 ID's in this matrix.
    private void updateColumnOne(int columnIndex) throws ClueException {
        int counter = 0;
        for (int i = 0; i < acceptedIDs.size(); i++) {
            if (matrix[i][columnIndex] == 0)
                setMinus1(i, columnIndex);
            if (matrix[i][columnIndex] == 1)
                counter++;
        }
        if (counter != 1)
            throw new ClueException(String.format("Fatal Error two people have the same card\n%s", toString()));
    }

    // This method is called when a row contains two -1's and a 0. We know that the 0 must be a 1.
    // It sets that 0 to a 1
    private void updateRowMinusTwo(int rowIndex) throws ClueException {
        for (int i = 0; i < 3; i++)
            if (matrix[rowIndex][i] == 0)
                set1(rowIndex, i);

    }

    // This method is called when a column contains two -1's and a 0. We know that the 0 must be a 1
    // Note only a valid inference when there are 3 ID's in this matrix.
    private void updateColumnMinusTwo(int columnIndex) throws ClueException {
        for (int rowIndex = 0; rowIndex < 3; rowIndex++)
            if (matrix[rowIndex][columnIndex] == 0)
                set1(rowIndex, columnIndex);

    }

    // Returns the sum of the row in position rowIndex.
    private int sumRow(int rowIndex) throws ClueException {
        int aSum = 0;
        for (int i = 0; i < 3; i++) {
            aSum += matrix[rowIndex][i];
        }
        if (aSum == -3)
            throw new ClueException(String.format("Inconsistent information player %d, cannot have any of %s", acceptedIDs.get(rowIndex), guess.toString()));
        return aSum;
    }

    // Returns the sum of the column in position columnIndex.
    private int sumColumn(int columnIndex) throws ClueException {
        int aSum = 0;
        for (int i = 0; i < 3; i++)
            aSum += matrix[i][columnIndex];
        if (aSum == -3)
            throw new ClueException(String.format("Inconsistent information no player can have %s in %s", getCard(columnIndex), guess.toString()));
        return aSum;
    }

    // Returns the ith card in this guess
    private Card getCard(int i) throws ClueException {
        if (i == 0)
            return guess.getPerson();
        if (i == 1)
            return guess.getPlace();
        if (i == 2)
            return guess.getThing();
        else {
            throw new ClueException(String.format("Invalid card index %d in GuessMatrix: %s", i, guess));
        }
    }

    // Marks the card at the supplied position -1, and adds it to the cardToStrike list.
    private void setMinus1(int rowIndex, int columnIndex) throws ClueException {
        Card newStrike;
        matrix[rowIndex][columnIndex] = -1;
        newStrike = getCard(columnIndex);
        newStrike.setPlayerID(acceptedIDs.get(rowIndex));
        addToStrike(newStrike);
        strikeNum++;
    }

    // this method adds the supplied card the cardToStrike list provided it is not already in it.
    private void addToStrike(Card card) {
        boolean flag = true;
        for (Card c : cardsToStrike) {
            if (c.equals(card)) {
                flag = false;
                break;
            }
        }
        if (flag)
            cardsToStrike.add(card);
    }

    // This method sets the int in matrix as the supplied position to 1 and adds it to the solvedCards list.
    private void set1(int rowIndex, int columnIndex) throws ClueException {
        int original = matrix[rowIndex][columnIndex];
        Card newFound;
        if (original == 0) {
            newFound = getCard(columnIndex);
            newFound.setPlayerID(acceptedIDs.get(rowIndex));
            addToSolved(newFound);
        }
        else {
            if (original == -1)
                throw new ClueException(String.format("Fatal Error trying to set a -1 to a 1 player %d, card %s gm: \n %s", acceptedIDs.get(rowIndex), getCard(columnIndex)));
        }
        matrix[rowIndex][columnIndex] = 1;
        updateColumnOne(columnIndex);
        solvedNum++;
    }

    // This method adds the supplied card to solvedCards list.
    private void addToSolved(Card card) throws ClueException {
        boolean flag = true;
        for (Card c : solvedCards) {
            if (c.exactlyEquals(card)) {
                flag = false;
                if (c.getPlayerID() != card.getPlayerID())
                    throw new ClueException(String.format("Card %s is being added to ", c.getName()));
            }
        }
        if (flag)
            solvedCards.add(card);
    }

    // Returns the index associated with a type string.
    private int getIndex(String type) {
        if (type == "suspect")
            return 0;
        if (type == "room")
            return 1;
        if (type == "weapon")
            return 2;
        return 3;
    }

    // check for idNum membership for param idNum in acceptedIDs
    public boolean inAccepted(int idNum) {
        return isMember(idNum, acceptedIDs);
    }

    // check for idNum membership for param idNum in rejectedIDs
    public boolean inRejected(int idNum) {
        return isMember(idNum, rejectedIDs);
    }

    // Returns true if the supplied card is in this.guess
    boolean cardIsIn(Card target) {
        Card[] cards = guess.getCards();
        for (Card card : cards)
            if (card.equals(target))
                return true;
        return false;
    }

    // returns true if the player with ID: idNum has been entered in this guess matrix
    private boolean isMember(int idNum, ArrayList<Integer> list) {
        boolean isMember = false;
        for (int id : list)
            if (idNum == id)
                isMember = true;
        return isMember;
    }

    // Returns true if the matrix is solved ie 3 cards locations are solved and there are no solvedCards still to be cleared from solvedCards
    public boolean isSolved() {
        if (solvedNum == 3 && solvedCards.size() == 0)
            return true;
        else
            return false;
    }

    // Returns the contents of solvedCards and clears it
    public Card[] getSolvedArray() {
        Card[] cards = new Card[solvedCards.size()];
        cards = solvedCards.toArray(cards);
        solvedCards.clear();
        return cards;
    }

    // returns the Cards to be stricken as an array of type Card
    public Card[] getStrikeArray() {
        Card[] cards = new Card[cardsToStrike.size()];
        cards = cardsToStrike.toArray(cards);
        cardsToStrike.clear();
        return cards;
    }

    // Returns the guess in this GM
    public Guess getGuess() {
        return guess.clone();
    }

    // Returns the number of ID's accepted by this guessMatrix.
    public int getAcceptedNum() {
        return acceptedIDs.size();
    }

    // converts this GuessMatrix to a string
    public String toString() {
        String strRep = "Guess matrix for: " + guess + "\n";
        strRep += String.format("--- ID Number --- %11s --- %11s --- %11s ---\n", guess.getPerson(), guess.getPlace(), guess.getThing());
        for (int i = 0; i < acceptedIDs.size(); i++) {
            strRep += String.format("--- %9d --- %11d --- %11d --- %11d ---\n", acceptedIDs.get(i), matrix[i][0], matrix[i][1], matrix[i][2]);
        }
        strRep += String.format("SolvedNum = %d\n", solvedNum);
        return strRep;
    }
}