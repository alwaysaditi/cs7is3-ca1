package org.example;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BM25Searcher {
    public static void search(String query, int index) throws IOException {
        String filePath = "output/bm25_similarity.txt";
        File outputFile = new File(filePath);
        if (outputFile.exists() && index == 0) {
            // Delete the file
            outputFile.delete();
            System.out.println("Existing file deleted.");
            return;
        }

   
        outputFile.createNewFile();
        System.out.println("New file created.");

        if (index == 0) return;
        String indexPath = "new_index";

        Analyzer analyzer = LuceneIndexer.analyzer; //  custom analyzer

        try {
            // Open the index
            Directory dir = FSDirectory.open(Paths.get(indexPath));
            IndexReader reader = DirectoryReader.open(dir);
            IndexSearcher searcher = new IndexSearcher(reader);
            System.out.println(query);

           
            searcher.setSimilarity(new BM25Similarity());

            // Loop through the 225 preprocessed queries

            // Parse the query string using the analyzer
            MultiFieldQueryParser parser = new MultiFieldQueryParser(new String[]{"title", "author", "bibliography", "content"}, analyzer);
            Query queryFinal = parser.parse(QueryParser.escape(query));

            // Execute the search (adjust the number of top documents as necessary)
            TopDocs topDocs = searcher.search(queryFinal, 50); // Retrieves top 10 documents

            StringBuilder output = new StringBuilder();


            Map<Integer, Double> docScores = new HashMap<>();

            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                //Document doc = searcher.doc(scoreDoc.doc);
                docScores.put(scoreDoc.doc, (double) scoreDoc.score); // Store the score with DocID

            }

            // Normalize scores
            int r = 1;
            Map<Integer, Double> normalizedScores = normalizeScores(docScores);
            List<Map.Entry<Integer, Double>> sortedEntries = new ArrayList<>(normalizedScores.entrySet());
            sortedEntries.sort((entry1, entry2) -> Double.compare(entry2.getValue(), entry1.getValue()));

                            for (Map.Entry<Integer, Double> entry : sortedEntries) {
                    output.append(index+" ")
                            .append("0 ")
                            .append(entry.getKey()+" ")
                            .append(r+ " ")
                            .append(entry.getValue()+" ")
                            .append("EXP")
                            .append("\n");
                    r++;
                }


            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, true))) {
                writer.write(output.toString());

            } catch (IOException e) {
                e.printStackTrace();
            }



           
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Map<Integer, Double> normalizeScores(Map<Integer, Double> docScores) {
        double minScore = Double.MAX_VALUE;
        double maxScore = Double.MIN_VALUE;

        // Find min and max scores
        for (double score : docScores.values()) {
            if (score < minScore) {
                minScore = score;
            }
            if (score > maxScore) {
                maxScore = score;
            }
        }

        // Normalize scores
        Map<Integer, Double> normalizedScores = new HashMap<>();
        for (Map.Entry<Integer, Double> entry : docScores.entrySet()) {
            double normalizedScore = (entry.getValue() - minScore) / (maxScore - minScore);
            normalizedScores.put(entry.getKey(), normalizedScore);
        }

        return normalizedScores;
    }
}

