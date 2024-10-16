
package org.example;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.synonym.SynonymFilter;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.analysis.synonym.WordnetSynonymParser;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import java.io.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class QueryExpander extends Analyzer {

    // Main method to process individual queries
    public String expandQuery(String query) throws IOException, ParseException {
        Reader reader = new StringReader(query);
        TokenStreamComponents tsc = createComponents("", reader);
        TokenStream tokenStream = tsc.getTokenStream();

        // Collect the tokens into a list
        List<String> tokens = new ArrayList<>();
        CharTermAttribute charTermAttr = tokenStream.addAttribute(CharTermAttribute.class);

        tokenStream.reset();
        while (tokenStream.incrementToken()) {
            tokens.add(charTermAttr.toString());
        }
        tokenStream.end();
        tokenStream.close();

        // Join tokens to form the expanded query
        return String.join(" ", tokens);
    }

    protected static TokenStreamComponents createComponents(String fieldName, Reader reader) throws ParseException {
        Tokenizer source = new StandardTokenizer();
        source.setReader(reader);

        // Create a filter chain
        TokenStream filter = new LowerCaseFilter(source);

        // Load stop words
        EnglishAnalyzer analyzer = new EnglishAnalyzer();
        CharArraySet stopWords = analyzer.getStopwordSet();

        filter = new StopFilter(filter, stopWords);

        // Filter words based on length
        filter = new LengthFilter(filter, 2, 20);

        // Load synonym map
        SynonymMap mySynonymMap = null;
        try {
            mySynonymMap = buildSynonym();
        } catch (IOException e) {
            e.printStackTrace();
        }

        filter = new SynonymFilter(filter, mySynonymMap, false);

        // Apply Porter stemming
        filter = new PorterStemFilter(filter);

        return new TokenStreamComponents(source, filter);
    }

    private static SynonymMap buildSynonym() throws IOException, ParseException {
        File file = new File("wn/wn_s.pl"); // Ensure the path is correct
        InputStream stream = new FileInputStream(file);
        Reader rulesReader = new InputStreamReader(stream);

        SynonymMap.Builder parser = new WordnetSynonymParser(true, true, new StandardAnalyzer(CharArraySet.EMPTY_SET));
        ((WordnetSynonymParser) parser).parse(rulesReader);
        return parser.build();
    }

    public static String startExpansion(String query) throws IOException, ParseException {
        QueryExpander analyzer = new QueryExpander();
        String expandedQuery = analyzer.expandQuery(query);
        System.out.println("Expanded Query: " + expandedQuery);
        return expandedQuery;
    }

    @Override
    protected TokenStreamComponents createComponents(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

// Custom LengthFilter implementation
class LengthFilter extends TokenFilter {
    private final CharTermAttribute termAttr = addAttribute(CharTermAttribute.class);
    private final int minLength;
    private final int maxLength;
    private boolean hasNext = true;

    protected LengthFilter(TokenStream input, int minLength, int maxLength) {
        super(input);
        this.minLength = minLength;
        this.maxLength = maxLength;
    }

    @Override
    public final boolean incrementToken() throws IOException {
        while (hasNext && input.incrementToken()) {
            String term = termAttr.toString();
            if (term.length() >= minLength && term.length() <= maxLength) {
                return true; // Keep this token
            }
        }
        hasNext = false; // No more tokens to process
        return false;
    }
}
