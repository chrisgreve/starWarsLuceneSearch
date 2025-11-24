package org.jsonsearch.lucene;

import org.apache.lucene.search.*;
import org.json.simple.parser.ParseException;
import java.io.IOException;
import java.util.Scanner;

// Here we perform example tests on StarWards JSON files for indexing, querying, and searching
public class StarWarsTester {
    String indexDir = "src/test/index";
    String dataDir = "src/main/resources";
    Indexer indexer;
    TopDocs phoneticHits;
//    TopDocs basicHits;

    public static void main(String[] args) throws IOException, ParseException {
        StarWarsTester tester = new StarWarsTester();

        // Scanner for user input
        Scanner sc = new Scanner(System.in);

        // Indexing
        System.out.println("Would you like to index search files? (Yes/No)");
        if(sc.nextLine().equals("Yes")) {
            // ask user to define which files to look for
            System.out.println("Please enter filepath for search files (default= \"src/main/resources\")");
            String dataPath = sc.nextLine();
            tester.setDataDir(dataPath);
            // ask user to define where to store index for search purposes
            System.out.println("Please enter filepath to store index: (default= \"src/test/index\")");
            String indexPath = sc.nextLine();
            tester.setIndexDir(indexPath);
            // create index here
            tester.createIndex();
        }

        // Searching
        System.out.println("Please enter the phrase to search (e.g. \"hyper space\"): ");
        String phrase = sc.nextLine(); // search phrase
        System.out.println("Searching for phrase: \"" + phrase + "\" found in procedures...");
        TopDocs hits = tester.phoneticSearch(phrase);
        // here we can perform analysis on hits found

    }
    public void setDataDir(String path) {
         dataDir = path;
    }
    public void setIndexDir(String path) {
        indexDir = path;
    }

    // Calls Indexer to process JSON files from indexDir into lucene indexes in dataDir
    public void createIndex() throws IOException, ParseException {
        indexer = new Indexer(indexDir);
        int numIndexed;
        long startTime = System.currentTimeMillis();
        numIndexed = indexer.createIndex(dataDir, new JsonFileFilter());
        long endTime = System.currentTimeMillis();
        indexer.close();
        System.out.println(numIndexed + " docs indexed");
        System.out.println("Indexing took " + (endTime - startTime) + " ms");
    }

    // Creates query based on phrase and prints how many hits found; returns TopDocs found
    public TopDocs phoneticSearch(String phrase) throws IOException {
        PhoneticSearcher searcher = new PhoneticSearcher(indexDir);
        long startTime = System.currentTimeMillis();
        Query query = searcher.createBooleanQuery(phrase); // herre we can choose what type of Query to create
        phoneticHits = searcher.search(query);
        long endTime = System.currentTimeMillis();
        int numHits = (int) phoneticHits.totalHits.value();
        if(numHits < LuceneConstants.MIN_OCCUR) {
            System.out.println("No significant hits found (Min occur must be > " +  LuceneConstants.MIN_OCCUR + ")");
            return null;
        }
        System.out.println(phoneticHits.totalHits + " found. Time: " + (endTime - startTime) + " ms");
        return phoneticHits;
    }

    // For searching without phonetics
//        public TopDocs search(String phrase) throws IOException {
//            Searcher searcher = new Searcher(indexDir);
//            long startTime = System.currentTimeMillis();
//            System.out.println("Searching for phrase: " + phrase);
//            Query query = searcher.createWildCardPhraseQuery(phrase);
//            basicHits = searcher.search(query);
//            long endTime = System.currentTimeMillis();
//            int numHits = (int) basicHits.totalHits.value();
//            if(numHits < LuceneConstants.MIN_OCCUR) {
//                System.out.println("No significant hits found (Min occur must be > " +  LuceneConstants.MIN_OCCUR + ")");
//                return null;
//            }
//            System.out.println(basicHits.totalHits + " found. Time: " + (endTime - startTime) + " ms");
//            return basicHits;
//        }

}
