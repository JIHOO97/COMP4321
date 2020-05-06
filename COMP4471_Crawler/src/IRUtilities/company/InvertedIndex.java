package IRUtilities.company;

import org.htmlparser.beans.StringBean;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.rocksdb.RocksDB;
import org.rocksdb.Options;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Timestamp;
import java.text.DateFormat;
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

    public void addEntry(String word, int x, int y) throws RocksDBException {
        // Add a "docX Y" entry for the key "word" into hashtable
        // ADD YOUR CODES HERE
        byte[] content = db.get(word.getBytes());
        if (content == null) {
            content = ("doc" + x + " " + y).getBytes();
        } else {
            content = (new String(content) + " doc" + x + " " + y).getBytes();
        }
        db.put(word.getBytes(), content);
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

    public void addLink(String link, Date lastModified) throws RocksDBException, ParseException {
        byte[] content = db.get(link.getBytes());
        SimpleDateFormat formatter = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US);
        if (content == null) {
            content = (lastModified.toString()).getBytes();
        } else {
            Date modifiedDate = formatter.parse(new String(content));
            Timestamp newModified = new Timestamp(lastModified.getTime());
            Timestamp savedModified = new Timestamp(modifiedDate.getTime());
            if (newModified.after(savedModified)) {
                Date modified = new Date(newModified.getTime());
                content = (modified.toString()).getBytes();
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

    public Vector<String> extractWords(String html) throws RocksDBException, IOException {
        Vector<String> result = new Vector<>();
        StopStem process = new StopStem("C:\\Users\\User\\Desktop\\COMP4321\\Phase 1\\COMP4471_Crawler\\stopwords.txt");
        StringBean bean = new StringBean();
        bean.setURL(html);
        bean.setLinks(false);
        String contents = bean.getStrings();
        StringTokenizer st = new StringTokenizer(contents);
        while (st.hasMoreTokens()) {
            result.add(st.nextToken());
        }
        while (st.hasMoreTokens()) {
            result.add(st.nextToken());
        }

        for(int j = 0; j < result.size(); ++j) {
            if(process.isStopWord(result.get(j))) {
                result.remove(j);
            }
        }

        for(int j = 0; j < result.size(); ++j) {
                String temp = process.stem(result.get(j));
                result.set(j, temp);
        }
        return result;
    }

    public String extractTitle(String html) throws IOException {
        Document doc = Jsoup.connect(String.valueOf(html)).get();
        String title = doc.title();
        return title;
    }
}