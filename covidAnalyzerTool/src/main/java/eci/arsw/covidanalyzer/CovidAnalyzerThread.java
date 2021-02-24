package eci.arsw.covidanalyzer;

import java.io.File;
import java.util.List;

    public class CovidAnalyzerThread extends Thread{
        private TestReader testReader;
        private List<File> reports;

        public CovidAnalyzerThread(List<File> rep) {
            reports = rep;
            testReader = new TestReader();
        }

        @Override
        public void run() {

            for(File r : reports)
            {
                List<Result> results = testReader.readResultsFromFile(r);
                for(Result result : results)
                {
                    synchronized(CovidAnalyzerTool.monitor){
                        if(CovidAnalyzerTool.pause){
                            try{
                                CovidAnalyzerTool.monitor.wait();
                            }catch (InterruptedException e){
                                e.printStackTrace();}
                        }
                    }
                    CovidAnalyzerTool.resultAnalyzer.addResult(result);
                }
                CovidAnalyzerTool.amountOfFilesProcessed.incrementAndGet();
            }
        }

        public synchronized void continuar(){
            CovidAnalyzerTool.monitor.notifyAll();
        }
}

