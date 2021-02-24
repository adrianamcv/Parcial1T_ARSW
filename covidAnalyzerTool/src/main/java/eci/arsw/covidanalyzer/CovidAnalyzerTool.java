package eci.arsw.covidanalyzer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A Camel Application
 */
public class CovidAnalyzerTool {
    public static ResultAnalyzer resultAnalyzer;
    private TestReader testReader;
    private int amountOfFilesTotal;
    public static AtomicInteger amountOfFilesProcessed;
    public static Object monitor= new Object();
    public static boolean pause = false;
    private int numThreads=5;
    private static ArrayList<CovidAnalyzerThread> threads;
    private List<File> resultFiles;

    public CovidAnalyzerTool() {
        resultAnalyzer = new ResultAnalyzer();
        testReader = new TestReader();
        amountOfFilesProcessed = new AtomicInteger();
    }

    public void processResultData() {
        amountOfFilesProcessed.set(0);
        resultFiles = getResultFileList();
        amountOfFilesTotal = resultFiles.size();
        int ini = 0;
        int fin = numThreads;
        int totalT = amountOfFilesTotal/numThreads;
        threads =new ArrayList<CovidAnalyzerThread>();
        for(int i=0 ;i< numThreads;i++){
            if(i+1 == numThreads && fin<amountOfFilesTotal){
                fin=amountOfFilesTotal;
            }
            CovidAnalyzerThread thread = new CovidAnalyzerThread(resultFiles.subList(ini,fin));
            ini = fin;
            fin = fin + totalT;
            thread.start();
            threads.add(thread);
        }

        for (File resultFile : resultFiles) {
            List<Result> results = testReader.readResultsFromFile(resultFile);
            for (Result result : results) {
                resultAnalyzer.addResult(result);
            }
            amountOfFilesProcessed.incrementAndGet();
        }
    }

    private List<File> getResultFileList() {
        List<File> csvFiles = new ArrayList<>();
        try (Stream<Path> csvFilePaths = Files.walk(Paths.get("src/main/resources/")).filter(path -> path.getFileName().toString().endsWith(".csv"))) {
            csvFiles = csvFilePaths.map(Path::toFile).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return csvFiles;
    }


    public Set<Result> getPositivePeople() {
        return resultAnalyzer.listOfPositivePeople();
    }

    /**
     * A main() so we can easily run these routing rules in our IDE
     */
    public static void main(String[] args) throws Exception {
        CovidAnalyzerTool covidAnalyzerTool = new CovidAnalyzerTool();
        //Thread processingThread = new Thread(() -> covidAnalyzerTool.processResultData());
        //processingThread.start();
        covidAnalyzerTool.processResultData();
        while (true) {
            Scanner scanner = new Scanner(System.in);
            String line = scanner.nextLine();
            if (line.contains("exit")){
                System.exit(0);
            if(line.isEmpty()){
            }if(pause=false){
                pause=true;}
            if(pause=true){
                pause=false;}

            for (CovidAnalyzerThread t : threads) {
                    t.continuar();
            }
            String message = "Processed %d out of %d files.\nFound %d positive people:\n%s";
            Set<Result> positivePeople = covidAnalyzerTool.getPositivePeople();
            String affectedPeople = positivePeople.stream().map(Result::toString).reduce("", (s1, s2) -> s1 + "\n" + s2);
            message = String.format(message, covidAnalyzerTool.amountOfFilesProcessed.get(), covidAnalyzerTool.amountOfFilesTotal, positivePeople.size(), affectedPeople);
            System.out.println(message);
        }
    }
}}

