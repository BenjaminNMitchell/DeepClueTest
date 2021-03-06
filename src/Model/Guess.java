package Model;

/**
 * Created by benji on 9/30/2016.
 * This object stores all the information for a guess in the bord game clue.
 *
 */
public class Guess {
    // a card of type "suspect"
    private Card person;

    // a card of type "room"
    private Card place;

    // a card of type "weapon"
    private Card thing;

    // Default Constructor for Model.Guess object.
    // Throws Model.GuessException if the cards generated by the string parameters do not match "suspect", "room", "weapon".
    public Guess(Card person, Card place, Card thing) throws GuessException, CardException {
        if (CardProperties.isSuspect(person) && CardProperties.isRoom(place) && CardProperties.isWeapon(thing)) {
            this.person = person.clone();
            this.place = place.clone();
            this.thing = thing.clone();
        } else
            throw new GuessException("Error cards are not in the right format");
    }

    // Returns deep copies of person, place, thing in a Model.Card array.
    public Card[] getCards() {
        Card[] cards = new Card[3];
        cards[0] = getPerson();
        cards[1] = getPlace();
        cards[2] = getThing();
        return cards;
    }

    // Returns a deep copy of the person attribute as a card.
    public Card getPerson() {
        return person.clone();
    }

    // Returns a deep copy of the place attribute as a card.
    public Card getPlace() {
        return place.clone();
    }

    // Returns a deep copy of the thing attribute as a card.
    public Card getThing() {
        return thing.clone();
    }

    // Creates and returns a String representation of this object in the form, "_ in the _ with the _"
    @Override
    public String toString() {
        return String.format("Guess: %s with the %s in the %s", person, thing, place);
    }

    // Overrides Objects equality method returns true if the supplied object is a card and its name matches this name.
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Guess) {
            Guess otherGuess = (Guess) obj;
            if (person.equals(otherGuess.getPerson()))
                return true;
            else
                return false;
        }
        else
            return false;
    }

    @Override
    public Guess clone() {
        try {
            Guess newGuess = new Guess(person, place, thing);
            return newGuess;
        } catch (ClueException e) {
            // unreachable do nothing
        }
        return null;
    }
}
