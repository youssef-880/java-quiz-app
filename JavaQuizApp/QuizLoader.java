import java.io.*;
import java.util.*;

/**
 * Reads Quiz_file.txt and parses it into a map of section name -> question list.
 *
 * Expected format:
 *   Section A: Arrays Quiz (10 MCQs)
 *
 *   Question 1
 *   What is an array in Java?
 *   A. A collection of different data types
 *   B. A collection of elements of the same type
 *   C. A loop structure
 *   D. A conditional statement
 *   Correct Answer: B
 */
public class QuizLoader {

    // Returns a LinkedHashMap so sections stay in the order they appear in the file.
    public static Map<String, List<Question>> load(String filePath) throws IOException {

        Map<String, List<Question>> sections = new LinkedHashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            String         line;
            List<Question> currentList = null;
            String         qText       = null;
            String[]       opts        = new String[4];
            boolean        inQuestion  = false;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("Section A:")) {
                    currentList = new ArrayList<>();
                    sections.put("Arrays", currentList);
                    inQuestion = false;

                } else if (line.startsWith("Section B:")) {
                    currentList = new ArrayList<>();
                    sections.put("Loops", currentList);
                    inQuestion = false;

                } else if (line.startsWith("Section C:")) {
                    currentList = new ArrayList<>();
                    sections.put("Conditional Statements", currentList);
                    inQuestion = false;

                } else if (line.startsWith("Section D:")) {
                    currentList = new ArrayList<>();
                    sections.put("Functions / Methods", currentList);
                    inQuestion = false;

                } else if (line.startsWith("Section E:")) {
                    currentList = new ArrayList<>();
                    sections.put("Object-Oriented Programming Basics", currentList);
                    inQuestion = false;

                } else if (line.startsWith("Question ") && currentList != null) {
                    qText      = null;
                    opts       = new String[4];
                    inQuestion = true;

                } else if (inQuestion) {

                    if (line.startsWith("A.")) {
                        opts[0] = line.substring(2).trim();

                    } else if (line.startsWith("B.")) {
                        opts[1] = line.substring(2).trim();

                    } else if (line.startsWith("C.")) {
                        opts[2] = line.substring(2).trim();

                    } else if (line.startsWith("D.")) {
                        opts[3] = line.substring(2).trim();

                    } else if (line.startsWith("Correct Answer:")) {
                        String answer = line.replace("Correct Answer:", "").trim();
                        if (currentList != null && qText != null) {
                            currentList.add(new Question(qText, opts.clone(), answer));
                        }
                        inQuestion = false;

                    } else if (qText == null) {
                        // first non-option line after "Question N" is the question text
                        qText = line;
                    }
                }
            }
        }

        return sections;
    }
}
