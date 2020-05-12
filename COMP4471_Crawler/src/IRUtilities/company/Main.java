package IRUtilities.company;

import org.jsoup.nodes.Element;
import org.rocksdb.RocksDBException;
import org.jsoup.nodes.Document;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.text.ParseException;


public class Main {

    public static void main(String args[]) throws RocksDBException, IOException, ParseException {
        DBReader dbReader = new DBReader("C:\\Users\\User\\Desktop\\COMP4321\\Phase 1\\COMP4471_Crawler\\dbLink",
                                         "C:\\Users\\User\\Desktop\\COMP4321\\Phase 1\\COMP4471_Crawler\\dbWord",
                                         "C:\\Users\\User\\Desktop\\COMP4321\\Phase 1\\COMP4471_Crawler\\dbWeight",
                                         "C:\\Users\\User\\Desktop\\COMP4321\\Phase 1\\COMP4471_Crawler\\dbTitle");

        dbReader.setLinkAndPN("https://www.cse.ust.hk/", 50);


//        Document doc = Jsoup.connect("https://www.cse.ust.hk/").get();
////        String content1 = doc.body().text();
//        Element content = doc.body();
//        String content1 = content.text();
//        System.out.println(content1);
        dbReader.saveLink();
        dbReader.saveWords();
        dbReader.saveDocumentWeight();
        dbReader.saveTitle();
        dbReader.showResult("requirement for computer science");
////        dbReader.getTermWeight(docs);
    }
}


