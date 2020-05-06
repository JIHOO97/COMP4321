package IRUtilities.company;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

public class StopStem {
    private IRUtilities.Porter porter;
    private java.util.HashSet stopWords;

    public boolean isStopWord(String str) {
        return stopWords.contains(str);
    }
    public int countStopWord() {
        return stopWords.size();
    }
    public StopStem(String str) {
        super();
        porter = new IRUtilities.Porter();
        stopWords = new java.util.HashSet();

        try {
            BufferedReader reader = new BufferedReader(new FileReader(str));
            String line;
            while ((line = reader.readLine()) != null) {
                stopWords.add(line);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String stem(String str) {
        return porter.stripAffixes(str);
    }

}
