package IRUtilities.company;

import org.rocksdb.RocksDBException;

import java.io.IOException;
import java.text.ParseException;
import java.util.Vector;

public class Main {
    public static void main(String args[]) throws RocksDBException, IOException, ParseException {
        DBReader dbReader = new DBReader("C:\\Users\\User\\Desktop\\COMP4321\\Phase 1\\COMP4471_Crawler\\dbLink",
                                         "C:\\Users\\User\\Desktop\\COMP4321\\Phase 1\\COMP4471_Crawler\\dbWord",
                                         "C:\\Users\\User\\Desktop\\COMP4321\\Phase 1\\COMP4471_Crawler\\dbWeight",
                                         "C:\\Users\\User\\Desktop\\COMP4321\\Phase 1\\COMP4471_Crawler\\dbTitle");
//        dbReader.saveLink("https://www.cse.ust.hk/");
//        dbReader.saveWords();
//        dbReader.saveDocumentWeight(50);
//        dbReader.saveTitle();
//        dbReader.getTitle("HKUST");
        dbReader.showResult("wang tao");
//        dbReader.getTermWeight(docs);
    }
}


