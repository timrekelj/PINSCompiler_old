package tests;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import ArgPar.Annotation.ParsableCommand;
import ArgPar.Annotation.ParsableOption;
import ArgPar.Exception.ParseException;
import ArgPar.Parser.ArgumentParser;
import compiler.lexer.Lexer;

@ParsableCommand(commandName = "TEST", description = "Tester for the PINS compiler")
public class Test {

    @ParsableOption(name = "--folder")
    public String folder = "";

    /**
     * Get phase that we want to test from args
     */
    public static Test parse(String[] args) {
        try {
            var parser = new ArgumentParser<Test>(Test.class);
            return parser.parse(args);
        } catch (ParseException __) {
            System.exit(2);
            return null;
        }
    }

    public static void main(String[] args) {

        
        var cli = Test.parse(args);
        
        // get number of test files
        String[] tests = getTestNames(cli.folder);

        // delete all .err files inside folder
        deleteErrFiles(cli.folder);

        // run tests
        runTest(tests, cli.folder);
    }

    /**
     * Counts the number of all files inside test folder
     * @param folder folder name from args
     * @return names of test files
     */
    private static String[] getTestNames(String folder) {
        String[] allFiles = new File("src/tests/" + folder).list();
        var tests = new ArrayList<String>();

        for (String file : allFiles) {
            if (file.endsWith(".pins23")) {
                tests.add(file.replace(".pins23", ""));
            }
        }

        return tests.toArray(String[]::new);
    }

    /**
     * Deletes all .err files inside folder
     * Minimizes the confusion when comparing files
     * @param folder folder name from args
     */
    private static void deleteErrFiles(String folder) {
        // delete all .err files inside folder
        File deleteDir = new File("src/tests/" + folder);
        File[] listOfFiles = deleteDir.listFiles();
        for (File file : listOfFiles) {
            if (file.isFile() && file.getName().endsWith(".err")) {
                file.delete();
            }
        }
    }

    /**
     * Runs all tests
     * @param tests names of tests in folder (without extensions)
     * @param folder folder name from args
     */
    private static void runTest(String[] tests, String folder) {
        String output = "";
        String expected = "";
        double countOK = 0;

        for (int i = 0; i < tests.length; i++) {
            output = getOutput(tests[i], folder);
            expected = readFile("src/tests/" + folder + "/" + tests[i] + ".out");
            System.out.print("Test " + tests[i] + ": ");
            if (output.equals(expected)) {
                countOK++;
                System.out.println("OK");
            } else {
                System.out.println("FAIL");
                System.out.println("Compare files: src/tests/" + folder + "/" + tests[i] + ".out and src/tests/" + folder + "/" + tests[i] + ".err");
                writeErrFile(output, tests[i], folder);
            }
        }

        // print ration
        System.out.println("Passed: " + (int)countOK + "/" + tests.length + "; " + String.format("%.2f", countOK / tests.length * 100) + "%");
    }

    /**
     * Gets program output for a given test
     * @param test name of the test
     * @param folder folder name from args
     * @return output produced by the program
     */
    private static String getOutput(String test, String folder) {
        String input = readFile("src/tests/" + folder + "/" + test + ".pins23");
        var symbols = new Lexer(input).scan();
        StringBuilder output = new StringBuilder();
        for (var symbol : symbols) {
            output.append(symbol.toString());
            output.append("\n");
        }
        return output.toString();
    }

    /**
     * Reads file and returns its content as a string
     * @param path path to the file
     * @return content of the file
     */
    private static String readFile(String path) {
        StringBuilder text = new StringBuilder();
        try {
            Scanner sc = new Scanner(new File(path));
            while (sc.hasNextLine()) {
                text.append(sc.nextLine());
                text.append("\n");
            }
            sc.close();
        } catch (Exception e) {
            System.out.println("Error while reading file!\nFilename: " + path);
            System.exit(1);
        }
        return text.toString();
    }

    /**
     * Writes output to a file.
     * Only happens for test that failed.
     * @param output output produced by the program
     * @param test name of the test
     * @param folder folder name from args
     */
    private static void writeErrFile(String output, String test, String folder) {
        try {
            File file = new File("src/tests/" + folder + "/" + test + ".err");
            file.createNewFile();
            FileWriter writer = new FileWriter(file);
            writer.write(output.strip());
            writer.close();
        } catch (IOException e) {
            System.out.println("Error while writing file!\nFilename: " + String.format("%02d", test) + ".err");
            System.exit(1);
        }
    }

}
