package org.example;

import org.apache.commons.math3.linear.RealVector;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

//import static jdk.internal.org.jline.utils.AttributedStringBuilder.append;
import static org.example.IndexViewer.*;

public class QuerySearcher {


    public static void searchQuery(String queryString, String qno, int actual) throws IOException {

        Map<Integer, Double> topFiveDocs = new HashMap<>();
System.out.println("actual here= "+actual+" query=  "+queryString);
        List<Map.Entry<Integer, Double>> scoreList = new ArrayList<>();

        String filePath = "output/custom_similarity.txt";
        File outputFile = new File(filePath);
        if (outputFile.exists() && actual ==0) {
            // Delete the file
            outputFile.delete();
            System.out.println("Existing file deleted.");
            return;
        }

    
        outputFile.createNewFile();
        System.out.println("New file created.");

        for (int i = 0; i <= 1399; i++) {
            termsglobal.clear();
            String indexPath = "new_index";
            int docId = i;
            Map<String, Integer> queryFrequencies = getQueryFrequencies(queryString);
            RealVector v1  ;
            RealVector v2 ;
            try {
                // Open the index directory
                Directory dir = FSDirectory.open(Paths.get(indexPath));
                IndexReader reader = DirectoryReader.open(dir);

                // Retrieve term frequencies for the specified document ID
                Map<String, Integer> termFrequencies = getTermFrequencies(reader, docId);



                v1 = toRealVector(queryFrequencies);
                v2 = toRealVector(termFrequencies);

                // Calculate the cosine similarity score
                double cosineSimilarityScore = getCosineSimilarity(v1, v2);


                Set<String> commonKeys = new HashSet<>(termFrequencies.keySet());  // Make a copy of keys1
                commonKeys.retainAll(queryFrequencies.keySet());  // Retains only the common elements with keys2


                // Store the score and document ID in the list
                scoreList.add(new AbstractMap.SimpleEntry<>(docId+1, cosineSimilarityScore));
                reader.close(); // Close the index reader
            } catch (IOException e) {
                System.out.println("Caught an " + e.getClass() + " with message: " + e.getMessage());
            }
        }
        scoreList.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        // Get the top five scores and their document IDs
        for (int j = 0; j < Math.min(5, scoreList.size()); j++) {
            Map.Entry<Integer, Double> entry = scoreList.get(j);
            topFiveDocs.put(entry.getKey(), entry.getValue());
        }
        StringBuilder output = new StringBuilder();


        for (int i = 2; i < 52 && i < scoreList.size(); i++) {
            //int qnum = actual+1;
            output.append(actual + " ") //query number
                    .append("0 ") //0
                    .append(scoreList.get(i).getKey()).append(" ") // document ID
                    .append(i-1+" ")//rank
                    .append(scoreList.get(i).getValue()+ " ")//score
                    .append("EXP")
                    .append("\n");
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, true))) {
            writer.write(output.toString());

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    static double getCosineSimilarity(RealVector v1, RealVector v2) {
        return (v1.dotProduct(v2)) / (v1.getNorm() * v2.getNorm());
    }

    public static Map<String,Integer> getQueryFrequencies(String query)
    {
        Map<String, Integer> frequencies = new HashMap<>();


        String[] splitTerms = query.split("\\s+"); // Split by whitespace

        for (String term : splitTerms) {
            term = term.toLowerCase(); // Normalize to lowercase (optional)

            // Update term frequency in the map
            frequencies.put(term, frequencies.getOrDefault(term, 0) + 1);

            // Add the term to the HashSet
            termsglobal.add(term);
        }

        return frequencies; // Return the map of term frequencies
    }
}
