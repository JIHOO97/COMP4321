package IRUtilities.company;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.*;
import org.htmlparser.beans.StringBean;
import org.htmlparser.util.ParserException;

import java.util.StringTokenizer;

import org.htmlparser.beans.LinkBean;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import java.net.URL;

import java.io.File;  // Import the File class
import java.io.FileWriter;   // Import the FileWriter class
import java.io.IOException;  // Import the IOException class to handle errors

//class InvertedIndex {
//    private RocksDB dbKey;
//    private RocksDB dbLink;
//    private Options options;
//
//    InvertedIndex(String dbPath, int i) throws RocksDBException {
//        // the Options class contains a set of configurable DB options
//        // that determines the behaviour of the database.
//        this.options = new Options();
//        this.options.setCreateIfMissing(true);
//
//        if(i == 0) {
//            // create and open the database
//            this.dbKey = RocksDB.open(options, dbPath);
//        } else {
//            this.dbLink = RocksDB.open(options, dbPath);
//        }
//    }
//
//    public void addWord(String doc, Vector<String> word1, int y) throws RocksDBException {
//        // Add a "docX Y" entry for the key "word" into hashtable
//        // ADD YOUR CODES HERE
//        byte[] content = dbKey.get(doc.getBytes());
//        if (content == null) {
//            content = (word1 + " " + y).getBytes();
//        } else {
//            content = (new String(content) + word1 + " " + y).getBytes();
//        }
//        dbKey.put(doc.getBytes(), content);
//    }
//
//    public void addLink(String doc, Vector<String> link1, int y) throws RocksDBException {
//        // Add a "docX Y" entry for the key "word" into hashtable
//        // ADD YOUR CODES HERE
//        byte[] content = dbLink.get(doc.getBytes());
//        if (content == null) {
//            content = (link1 + " " + y).getBytes();
//        } else {
//            content = (new String(content) + link1 + " " + y).getBytes();
//        }
//        dbLink.put(doc.getBytes(), content);
//    }
//
//
//    public void delEntry(String word) throws RocksDBException {
//        // Delete the word and its list from the hashtable
//        // ADD YOUR CODES HERE
//        dbKey.remove(word.getBytes());
//    }
//
//    public void printAllKey() throws RocksDBException {
//        // Print all the data in the hashtable
//        // ADD YOUR CODES HERE
//        RocksIterator iter = dbKey.newIterator();
//
//        for (iter.seekToFirst(); iter.isValid(); iter.next()) {
//            System.out.println(new String(iter.key()) + "=" + new String(iter.value()));
//        }
//    }
//
//    public void printAllLink() throws RocksDBException {
//        // Print all the data in the hashtable
//        // ADD YOUR CODES HERE
//        RocksIterator iter = dbLink.newIterator();
//
//        for (iter.seekToFirst(); iter.isValid(); iter.next()) {
//            System.out.println(new String(iter.key()) + "=" + new String(iter.value()));
//        }
//    }
//
//    public void printToFile(InvertedIndex test) throws IOException {
//        RocksIterator iter1 = dbKey.newIterator();
//        RocksIterator iter2 = test.dbLink.newIterator();
//
//        File myObj = new File("spider_result.txt");
//        if (myObj.createNewFile()) {
//            System.out.println("File created: " + myObj.getName());
//        } else {
//            System.out.println("File already exists.");
//        }
//        FileWriter myWriter = new FileWriter("spider_result.txt");
//        for(iter1.seekToFirst(), iter2.seekToFirst(); iter1.isValid() && iter2.isValid(); iter1.next(), iter2.next()) {
//            myWriter.write(new String(iter1.key()) + "\n");
//            myWriter.write(new String(iter1.value()) +"\n");
//            myWriter.write(new String(iter2.value()) +"\n");
//            myWriter.write("-------------------------------------------------------------------------\n\n");
//
//        }
//        myWriter.close();
//    }
//}


//public class Main {
//    private String url;
//
//    Main(String _url) {
//        url = _url;
//    }
//
//    public Vector<String> extractWords() throws ParserException {
//        // extract words in url and return them
//        // use StringTokenizer to tokenize the result from StringBean
//        // ADD YOUR CODES HERE
//        Vector<String> result = new Vector<String>();
//        StringBean bean = new StringBean();
//        bean.setURL(url);
//        bean.setLinks(false);
//        String contents = bean.getStrings();
//        StringTokenizer st = new StringTokenizer(contents);
//        while (st.hasMoreTokens()) {
//            result.add(st.nextToken());
//        }
//        return result;
//
//    }
//
//    public Vector<String> extractLinks() throws ParserException {
//        // extract links in url and return them
//        // ADD YOUR CODES HERE
//        Vector<String> result = new Vector<String>();
//        LinkBean bean = new LinkBean();
//        bean.setURL(url);
//        URL[] urls = bean.getLinks();
//        for (URL s : urls) {
//            result.add(s.toString());
//        }
//        return result;
//
//    }
//
//
//
//    public static void main(String[] args) {
//        try {
//            Main crawler = new Main("https://www.cse.ust.hk");
//
//            InvertedIndex wordIndex = new InvertedIndex("C:\\Users\\User\\Desktop\\COMP4321\\Phase 1\\COMP4471_Crawler\\dbWord", 0);
//            InvertedIndex linkIndex = new InvertedIndex("C:\\Users\\User\\Desktop\\COMP4321\\Phase 1\\COMP4471_Crawler\\dbLink", 1);
//
////            Vector<String> words = crawler.extractWords();
////
////            System.out.println("Words in " + crawler.url + ":");
////            for (int i = 0; i < words.size(); i++) {
////                System.out.print(words.get(i) + "\n");
////                System.out.println(words.get(i).getBytes());
////            }
////            System.out.println("\n\n");
//
//            Vector<String> links = crawler.extractLinks(); // only do 30 of them
//
//            for (int i = 0; i < 30 ;i++) {
//                String extractLink = links.get(i);
////                System.out.println("URL is " + extractLink);
//                Main miniCrawler = new Main(extractLink);
//
//                Vector<String> Words = miniCrawler.extractWords();
//                Vector<String> Links = miniCrawler.extractLinks();
//
//                StopStem process = new StopStem("C:\\Users\\User\\Desktop\\COMP4321\\Phase 1\\COMP4471_Crawler\\stopwords.txt");
//                for(int j = 0; j < Words.size(); ++j) {
//                    if(process.isStopWord(Words.get(j))){
//                        Words.remove(j);
//                    }
//                }
//
//                for(int j = 0; j < Words.size(); ++j) {
//                        String temp = process.stem(Words.get(j));
//                        Words.set(j, temp);
//                }
//
//                for(int j = 0; j < Words.size(); ++j) {
//                    if(process.isStopWord(Words.get(j))){
//                        Words.remove(j);
//                    }
//                }
//
//                wordIndex.addWord("doc" + Integer.toString(i+1), Words, 1);
//                linkIndex.addLink("doc" + Integer.toString(i+1), Links, 1);
//
//                for (int j = 0; j < Links.size(); j++) {
////                    System.out.println("Child link " + Links.get(j));
////                    index.addEntry(Links.get(j), i+1, j+1);
//                }
//
//            }
//
//            wordIndex.printToFile(linkIndex);
////            System.out.println("Links in " + crawler.url + ":");
////            for (int i = 0; i < links.size(); i++)
////                System.out.println(links.get(i));
////            System.out.println("");
//
//        } catch (ParserException | RocksDBException e) {
//            e.printStackTrace();
//        }
//        catch (IOException e) {
//            System.out.println("An error occurred.");
//            e.printStackTrace();
//        }
//    }
//}
public class Main {
    public static void main(String[] args) throws RocksDBException, IOException, ParseException {
        Crawler crawler = new Crawler();
        LinkedHashSet<String> linkExtractor = crawler.getPageLinks("https://www.cse.ust.hk/", 50);
        Vector<Date> lastModified = crawler.getLastModificationDate(linkExtractor);
        // a static method that loads the RocksDB C++ library.
        RocksDB.loadLibrary();

        InvertedIndex URL = new InvertedIndex("C:\\Users\\User\\Desktop\\COMP4321\\Phase 1\\COMP4471_Crawler\\dbLink");
        InvertedIndex indexer = new InvertedIndex("C:\\Users\\User\\Desktop\\COMP4321\\Phase 1\\COMP4471_Crawler\\dbWord");
        InvertedIndex title = new InvertedIndex("C:\\Users\\User\\Desktop\\COMP4321\\Phase 1\\COMP4471_Crawler\\dbTitle");

        Vector<String> tempWord;
        Iterator<String> itr = linkExtractor.iterator();
        Iterator<String> itr2 = linkExtractor.iterator();
        // insert words and title into inverted index
        int index = 0;
        while(itr.hasNext()) {
            index++;
            tempWord = indexer.extractWords(itr.next());
            title.addTitle(title.extractTitle(itr2.next()),index);
            for (int i = 0; i < tempWord.size(); ++i) {
                indexer.addEntry(tempWord.get(i),index,i+1);
            }
        }

        itr = linkExtractor.iterator();
        for (int i = 0; i < lastModified.size(); ++i) {
            URL.addLink(itr.next(),lastModified.get(i));
        }
        URL.printAll();
        System.out.println();
        indexer.printAll();
        System.out.println();
        title.printAll();
    }
}
