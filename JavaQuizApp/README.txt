======================================================
  Java Quiz Application
  Development of a GUI Based Java Quiz Application
======================================================

FILES INCLUDED
--------------
  Question.java         – Model class for a single MCQ
  QuizLoader.java       – Reads questions from Quiz_file.txt
  QuizApplication.java  – Main GUI application (run this)
  Quiz_file.txt         – Question bank (50 MCQs, 5 sections)

HOW TO COMPILE
--------------
Open a terminal / command prompt in the project folder, then run:

  javac Question.java QuizLoader.java QuizApplication.java

This produces .class files in the same folder.

HOW TO RUN
----------
  java QuizApplication

IMPORTANT: Quiz_file.txt must be in the SAME folder as the
           .class files when you run the program.

WHAT THE PROGRAM DOES
---------------------
1. Shows a main menu with 5 quiz topics:
     - Arrays
     - Loops
     - Conditional Statements
     - Functions / Methods
     - Object-Oriented Programming Basics

2. The user selects a topic and answers 10 MCQs one by one
   using radio buttons.

3. After all 10 questions, a result screen shows:
     - Final Score  (e.g. 8 / 10)
     - Correct Answers
     - Wrong Answers
     - Percentage

4. Results are also saved/appended to quiz_results.txt
   in the same folder.

REQUIREMENTS
------------
  Java 8 or later (any standard JDK/JRE)
  Java Swing (included in the JDK — no extra libraries needed)

======================================================
