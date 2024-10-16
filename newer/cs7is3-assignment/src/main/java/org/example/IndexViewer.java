package org.example;

import org.apache.lucene.index.*;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

import org.apache.commons.math3.linear.*;
// this class is used to build the termsGlobal Set which is used in calcuating the cosine similairty
public class IndexViewer {
    public static Set<String> termsglobal = new HashSet<>();
    public static Map<String, Integer> getTermFrequencies(IndexReader reader, int docId) throws IOException {
        // Retrieve the term vectors for the specified document
        Fields termVectors = reader.getTermVectors(docId);
        Map<String, Integer> frequencies = new HashMap<>();

        // Check if term vectors are available for the document
        if (termVectors != null) {
            // Access the term vectors for the specific field "content"
            Terms terms = termVectors.terms("content"); // Specify the field name
            if (terms != null) {
                TermsEnum termsEnum = terms.iterator();
                BytesRef term;

                while ((term = termsEnum.next()) != null) {
                    String termText = term.utf8ToString(); // Convert BytesRef to String
                    long termFreq = termsEnum.totalTermFreq(); // Get the frequency of the term

                    // Add term and its frequency to the map
                    frequencies.put(termText, (int) termFreq);
                    termsglobal.add((termText));
                }

            }
        } else {
            //System.out.println("No term vectors available for docId " + docId);
        }

        return frequencies; // Return the map of term frequencies
    }
    static RealVector toRealVector(Map<String, Integer> map) {
        RealVector vector = new ArrayRealVector(termsglobal.size());
        int i = 0;
        // here, we have a Map that details the words in a document with its number of occurrences. The occurrences of each word is divided by the
        for (String term : termsglobal) {
            int value = map.containsKey(String.valueOf(term)) ? map.get(String.valueOf(term)) : 0;

            vector.setEntry(i++, value);
        }
        return (RealVector) vector.mapDivide(vector.getL1Norm());
    }
    public static void main(String[] args) {
        String indexPath = "index"; 
        int docId = 0; 

        try {
            // Open the index directory
            Directory dir = FSDirectory.open(Paths.get(indexPath));
            IndexReader reader = DirectoryReader.open(dir);

            // Retrieve term frequencies for the specified document ID
            Map<String, Integer> termFrequencies = getTermFrequencies(reader, docId);

            // Print the term frequencies
            for (Map.Entry<String, Integer> entry : termFrequencies.entrySet()) {
                System.out.println("Term: " + entry.getKey() + ", Frequency: " + entry.getValue());
            }

            reader.close(); // Close the index reader
        } catch (IOException e) {
            System.out.println("Caught an " + e.getClass() + " with message: " + e.getMessage());
        }
    }
}
