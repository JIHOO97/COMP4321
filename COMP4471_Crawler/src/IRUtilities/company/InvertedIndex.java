package IRUtilities.company;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.Connection;
import org.rocksdb.RocksDB;
import org.rocksdb.Options;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


public class InvertedIndex
{
    private RocksDB db;
    private Options options;

    InvertedIndex(String dbPath) throws RocksDBException {
        // the Options class contains a set of configurable DB options
        // that determines the behaviour of the database.
        this.options = new Options();
        this.options.setCreateIfMissing(true);

        // create and open the database
        this.db = RocksDB.open(options, dbPath);
    }

    public RocksDB getDB() {
        return this.db;
    }


    public void addEntry(String word, int x, int y) throws RocksDBException {
        // Add a "docX Y" entry for the key "word" into hashtable
        // ADD YOUR CODES HERE
        byte[] content = db.get(word.getBytes());
        if (content == null) {
            content = ("doc" + x + " " + y).getBytes();
        } else {
            content = (new String(content) + " doc" + x + " " + y).getBytes();
        }
        db.put(word.getBytes(),content);
    }

    public void addWeight(String doc, String word, double weight) throws RocksDBException {
        byte[] content = db.get(doc.getBytes());
        if (content == null) {
            content = (word + " " + weight).getBytes();
        }
        else
            content = (new String(content) + " " + word + " " + weight).getBytes();
        db.put(doc.getBytes(),content);
    }

    public void addTitle(String title, int x) throws RocksDBException {
        // put title and index of the documents
        byte[] content = db.get(title.getBytes());
        if (content == null) {
            content = ("doc" + x).getBytes();
        } else {
            content = (new String(content) + " doc" + x).getBytes();
        }
        db.put(title.getBytes(), content);
    }

    public void addLink(String link, Date lastModified, Integer pageSize, int docNumber) throws RocksDBException, ParseException, IOException {
        byte[] content = db.get(link.getBytes());
        SimpleDateFormat formatter = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US);
        Document doc = Jsoup.connect(link).get();
        Element isContentNull = doc.body();
        Vector<String> wordCount = new Vector<>();
        if(isContentNull != null) {
            String contentLink = isContentNull.text();
            StringTokenizer st = new StringTokenizer(contentLink);
            while(st.hasMoreTokens()) wordCount.add(st.nextToken());
        }
        int temp = pageSize.intValue();
        if(temp == 0) {
            for(int i = 0; i < wordCount.size(); i++) {
                temp += wordCount.get(i).length();
            }
        }
        if (content == null) {
            content = (lastModified.toString() + " " + temp).getBytes();
        } else {
            Date modifiedDate = formatter.parse(new String(content));
            Timestamp newModified = new Timestamp(lastModified.getTime());
            Timestamp savedModified = new Timestamp(modifiedDate.getTime());
            if (newModified.after(savedModified)) {
                Date modified = new Date(newModified.getTime());
                content = (modified.toString() + " " + temp).getBytes();
            }
        }
        db.put(link.getBytes(), content);
    }

    public void delEntry(String word) throws RocksDBException {
        // Delete the word and its list from the hashtable
        // ADD YOUR CODES HERE
        db.remove(word.getBytes());
    }
    public void printAll() throws RocksDBException {
        // Print all the data in the hashtable
        // ADD YOUR CODES HERE
        RocksIterator iter = db.newIterator();

        for(iter.seekToFirst(); iter.isValid(); iter.next()) {
            System.out.println(new String(iter.key()) + " = " + new String(iter.value()));
        }
    }

    public LinkedHashSet<String> getLinks() {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        RocksIterator itr = db.newIterator();

        for(itr.seekToFirst(); itr.isValid(); itr.next()) {
            result.add(new String(itr.key()));
        }

        return result;
    }


    public Vector<String> extractWords(String html) throws RocksDBException, IOException {
        Vector<String> result = new Vector<>();
        StopStem process = new StopStem("C:\\Users\\User\\Desktop\\COMP4321\\Phase 1\\COMP4471_Crawler\\stopwords.txt");
        try {
            Document doc = Jsoup.connect(html).get();
            Element isContentNull = doc.body();
            if (isContentNull != null) {
//                System.out.println(html);
                String content = doc.body().text();
                StringTokenizer st = new StringTokenizer(content);
                while (st.hasMoreTokens()) {
                    result.add(st.nextToken());
                }

                for (int j = 0; j < result.size(); ++j) {
                    if (process.isStopWord(result.get(j))) {
                        result.remove(j);
                    }
                }

                for (int j = 0; j < result.size(); ++j) {
                    String temp = process.stem(result.get(j));
                    result.set(j, temp);
                }
            }
            return result;
        } catch (IOException e) {
            System.out.println(e + " for this URL: " + html);
            return result;
        }
    }

    public String extractTitle(String html) throws IOException {
        try {
            Document doc = Jsoup.connect(String.valueOf(html)).get();
            String title = doc.title();
            return title;
        } catch (IOException e){
            System.out.println(e);
            return "";
        }
    }
}