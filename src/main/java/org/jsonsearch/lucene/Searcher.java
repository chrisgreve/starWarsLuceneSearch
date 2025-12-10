package org.jsonsearch.lucene;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.sandbox.search.PhraseWildcardQuery;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;

import java.io.Closeable;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

// This is the main searcher class that look for results both exact and similar in phonetics
public class Searcher implements Closeable {
    private final IndexSearcher indexSearcher;
    private final DirectoryReader reader;

    public Searcher(String indexDirectoryPath) throws IOException {
        this.reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexDirectoryPath)));
        this.indexSearcher = new IndexSearcher(reader);
    }

    public TopDocs search(Query query) throws IOException {
        return indexSearcher.search(query, LuceneConstants.MAX_SEARCH);
    }

    public Document getDocument(ScoreDoc scoreDoc) throws IOException {
        return indexSearcher.storedFields().document(scoreDoc.doc);
    }

    // This method returns an ordered set of bookmark tag IDs where hits are found; TODO: we can work on ranking their relevance
    public LinkedHashMap<String, Double> getBookmarks(TopDocs hits) throws IOException {
        LinkedHashMap<String, Double> bookmarkCounts = new LinkedHashMap<>(); // linked hash map helps rank bookmarks by score
        Set<Integer> uniqueDocs = new LinkedHashSet<>();
        for (ScoreDoc scoreDoc : hits.scoreDocs) {
            int docID = scoreDoc.doc;
            uniqueDocs.add(docID);
//            System.out.print("Score: " + scoreDoc.score + " Doc ID:" + docID);
            Document document = this.getDocument(scoreDoc);
            String proc_id = document.get(LuceneConstants.BOOKMARK_TAG);
            String start_speech = document.get(LuceneConstants.START);
            String end_speech = document.get(LuceneConstants.END);
            if (proc_id != null) {
                bookmarkCounts.put(proc_id, bookmarkCounts.getOrDefault(proc_id, (double) 0) + (double) scoreDoc.score);
            }
            System.out.print(" From time " + start_speech + " to " + end_speech + ": ");
//            displayTokenUsingStandardAnalyzer(document.get(LuceneConstants.CONTENTS));
            System.out.println(document.get(LuceneConstants.CONTENTS));
        }

        System.out.println(" Significant docs have IDs of:" + uniqueDocs);
        return bookmarkCounts;
    }


    // This method analyzes a given query term to get its phonetic form (first token)
    private String getPhoneticTerm(String queryTerm) {
        String phoneticTerm = "";
        try (MyPhoneticAnalyzer analyzer = new MyPhoneticAnalyzer();
             TokenStream stream = analyzer.tokenStream(LuceneConstants.CONTENTS, new StringReader(queryTerm))) {
            CharTermAttribute charTermAttr = stream.addAttribute(CharTermAttribute.class);
            stream.reset();
            if (stream.incrementToken()) {
                phoneticTerm = charTermAttr.toString();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return phoneticTerm;
    }

    // This method creates what we are mainly using for searches right now
    public BooleanQuery createBooleanQuery(String phrase, boolean isPhoneticSearch) throws IOException {
        BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();

        // Create Option Queries to add to Boolean query
        Query phraseQuery;
        if (!isPhoneticSearch) {
            phraseQuery = createExactPhraseQuery(phrase);
        } else {
            phraseQuery = createPhoneticPhraseQuery(phrase);
        }

        // check for fuzzy
        if(phrase.contains("~")){
            Query fuzzyQuery = createFuzzyQuery(phrase);
            booleanQueryBuilder.add(fuzzyQuery, BooleanClause.Occur.MUST);
        }

        // check for wildcards
        if(phrase.contains("*") || phrase.contains("?")) {
            Query wildcardPhraseQuery = createPhraseWildcardQuery(phrase);
            booleanQueryBuilder.add(wildcardPhraseQuery, BooleanClause.Occur.MUST);

        }

        // Add Queries to BooleanQuery using SHOULD = OR logic (phrase should match)
        booleanQueryBuilder.add(phraseQuery, BooleanClause.Occur.SHOULD);



        return booleanQueryBuilder.build(); // return combined fuzzy and wildcard query
    }

//  Below we implement methods to create different types of queries used in boolean query for refining searches

    public TermQuery createBasicQuery(String phrase) {
        Term t = new Term(LuceneConstants.CONTENTS, phrase);
        return new TermQuery(t);
    }

    public Query createExactPhraseQuery(String phrase) throws IOException {
        PhraseQuery.Builder builder = new PhraseQuery.Builder();
        try (StandardAnalyzer standardAnalyzer = new StandardAnalyzer();
             TokenStream tokenStream = standardAnalyzer.tokenStream(
                     LuceneConstants.CONTENTS, new StringReader(phrase))) {
            CharTermAttribute term = tokenStream.addAttribute(CharTermAttribute.class);
            tokenStream.reset();
            while (tokenStream.incrementToken()) {
                builder.add(new Term(LuceneConstants.CONTENTS, term.toString()));
            }
        }
        builder.setSlop(LuceneConstants.PHRASE_QUERY_SLOP); // default to 2
        return createBoostQuery(builder.build()); // this returns a PhraseQuery instance
    }

    public PhraseQuery createPhoneticPhraseQuery(String phrase) throws IOException {
        PhraseQuery.Builder builder = new PhraseQuery.Builder();
        try (MyPhoneticAnalyzer analyzer = new MyPhoneticAnalyzer();
             TokenStream tokenStream = analyzer.tokenStream(
                     LuceneConstants.CONTENTS, new StringReader(phrase))) {
            CharTermAttribute term = tokenStream.addAttribute(CharTermAttribute.class);
            tokenStream.reset();
            while (tokenStream.incrementToken()) {
                // tokens produced are already phonetic representations
                builder.add(new Term(LuceneConstants.CONTENTS, term.toString()));
            }
        }
        builder.setSlop(LuceneConstants.PHRASE_QUERY_SLOP);
        return builder.build(); // return PhraseQuery instance
    }

    public Query createFuzzyQuery(String phrase) {
        Term fuzzyTerm = new Term(LuceneConstants.CONTENTS, phrase);
        return createBoostQuery(new FuzzyQuery(fuzzyTerm, 2));
    }

    public WildcardQuery createWildcardQuery(String phrase) {
        Term wildcardTerm = new Term(LuceneConstants.CONTENTS, phrase + "*");

        return new WildcardQuery(wildcardTerm);
    }

    public Query createPhraseWildcardQuery(String phrase) throws IOException {
        PhraseWildcardQuery.Builder builder = new PhraseWildcardQuery.Builder(LuceneConstants.CONTENTS, 2);
        String[] words = phrase.split("\\s+"); // split words by one/more whitespace
        for(String word : words) {
            if(word.equals("*") || word.equals("~")) {
               String phraseWithoutWildcard = phrase.replace("*", "").replace("?", "");
               return createExactPhraseQuery(phraseWithoutWildcard); // create phrase query if a term in phrase is a wildcard
            }
            if (word.contains("*") || word.contains("?")) { // If the term contains a wildcard, create a MultiTermQuery
                // e.g. PrefixQuery used here for terms starting with a prefix; might need a full WildcardQuery depending on placement of '*'
                MultiTermQuery multiTermQuery = new WildcardQuery(new Term(LuceneConstants.CONTENTS, word)); // Simplified
                System.out.println(word.replace("*", "").replace("?", ""));
                builder.addMultiTerm(multiTermQuery);
            }
            else {
                // For a regular term, add it directly
                builder.addTerm(new Term(LuceneConstants.CONTENTS, word));
            }
        }
        return createBoostQuery(builder.build());
    }

    public PrefixQuery createPrefixQuery(String phrase) {
        Term t = new Term(LuceneConstants.CONTENTS, phrase);
        return new PrefixQuery(t);
    }

    // Helps boost certain queries to have better score than others
    public Query createBoostQuery(Query query) {
        return new BoostQuery(query, 100.0f);
    }

    // Helper method to print a separator line
    public static void printSeparator(char character, int length) {
        for (int i = 0; i < length; i++) {
            System.out.print(character);
        }
        System.out.println();
    }


    @Override
    public void close() throws IOException {
        reader.close();
    }
}
