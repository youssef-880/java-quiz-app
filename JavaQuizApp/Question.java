public class Question {

    private final String   text;
    private final String[] options;       // index 0=A, 1=B, 2=C, 3=D
    private final String   correctAnswer;

    public Question(String text, String[] options, String correctAnswer) {
        this.text          = text;
        this.options       = options;
        this.correctAnswer = correctAnswer;
    }

    public String   getText()          { return text; }
    public String[] getOptions()       { return options; }
    public String   getCorrectAnswer() { return correctAnswer; }
}
