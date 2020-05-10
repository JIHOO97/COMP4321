package IRUtilities.company;

import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class DBReader {
      private InvertedIndex dbLink;
      private InvertedIndex dbWord;
      private InvertedIndex dbWeight;
      private InvertedIndex dbTitle;
      private RocksDB dbl;

    DBReader(String dbPath1, String dbPath2, String dbPath3, String dbPath4) throws RocksDBException {
        this.dbLink = new InvertedIndex(dbPath1);
        this.dbWord = new InvertedIndex(dbPath2);
        this.dbWeight = new InvertedIndex(dbPath3);
        this.dbTitle = new InvertedIndex(dbPath4);
    }

    public void saveLink(String URL) throws IOException, ParseException, RocksDBException {
        Crawler crawler = new Crawler();
        LinkedHashSet<String> links = crawler.getPageLinks(URL, 50);
        Vector<Date> lastModDate = crawler.getLastModificationDate(links);
        Iterator<String> itr = links.iterator();
        for (int i = 0; i < lastModDate.size(); ++i) {
            this.dbLink.addLink(itr.next(),lastModDate.get(i));
        }
    }

    public void saveWords() throws IOException, ParseException, RocksDBException {
        LinkedHashSet<String> links = dbLink.getLinks();
        Iterator<String> itr = links.iterator(); int index = 0;
        while (itr.hasNext()) {
            String html = itr.next();
            Vector<String> words = dbWord.extractWords(html);
            for (int i = 0; i < words.size(); ++i) {
                dbWord.addEntry(words.get(i),index,i);
            }
            ++index;
        }
    }

    public void saveTitle() throws IOException, RocksDBException {
        LinkedHashSet<String> links = dbLink.getLinks();
        Iterator<String> itr = links.iterator(); int index = 0;
        while (itr.hasNext()) {
            String title = dbTitle.extractTitle(itr.next());
            dbTitle.addTitle(title,index);
            index++;
        }
        dbTitle.printAll();
    }

    public void saveDocumentWeight(int maxDF) throws IOException, RocksDBException {
        LinkedHashSet<String> links = dbLink.getLinks();
        Vector<Integer> maxTF = getMaxTF();
        double documentDF, documentTF;
        Iterator<String> itr = links.iterator(); int index = 0;
        while(itr.hasNext()) {
            String doc = "doc" + index;
            HashMap<String,Integer> temp = getDocumentTF(itr.next());
            for (String i : temp.keySet()) {
                documentDF = getDocumentDF(i);
                documentTF = temp.get(i);
                double docWeight = documentTF * (Math.log(maxDF/documentDF)/Math.log(2)) / maxTF.get(index);
                dbWeight.addWeight(doc,i,docWeight);
            }
            index++;
        }
//        dbWeight.printAll();
    }

    public String getTitle(String query) {
        String title = "";
        this.dbl = dbTitle.getDB();
        RocksIterator itr = this.dbl.newIterator();

        for(itr.seekToFirst(); itr.isValid(); itr.next()) {
            if(new String(itr.key()).contains(query)) {
                title = new String(itr.key());
                break;
            }
        }

//        System.out.println(title);

        return title;
    }



    // shows the result with respect to the given query
    public Vector<String> showResult(String query) throws IOException, RocksDBException {
        // shows which documents and its position that contain the query
        Vector<String> Query = new Vector<>();
        StringTokenizer st = new StringTokenizer(query);
        while (st.hasMoreTokens()) {
            Query.add(st.nextToken());
        }

        StopStem process = new StopStem("C:\\Users\\User\\Desktop\\COMP4321\\Phase 1\\COMP4471_Crawler\\stopwords.txt");
        for(int j = 0; j < Query.size(); ++j) {
            if(process.isStopWord(Query.get(j))) {
                Query.remove(j);
            }
        }
        for(int j = 0; j < Query.size(); ++j) {
            String temp = process.stem(Query.get(j));
            Query.set(j, temp);
        }

        Vector<String> docs = new Vector<>();
        this.dbl = dbWord.getDB();
        RocksIterator itr = this.dbl.newIterator();
        for (int i = 0; i < Query.size(); ++i) {
            for(itr.seekToFirst(); itr.isValid(); itr.next()) {
                if ((new String(itr.key()).equals(Query.get(i)))) {
                    docs.add(new String(itr.value()));
                }
            }
        }
        for (int i = 0; i < docs.size(); i++) System.out.println(docs.get(i));

        dbLink.printAll();

//        getTermWeight(docs);

        List<String> documents = calculateCosSim(docs,Query);
        Vector<Integer> docIndex = new Vector<>();
        Pattern pattern = Pattern.compile("doc(.+)");
        for (String i : documents) {
            Matcher matcher = pattern.matcher(i);
            matcher.find();
            docIndex.add(Integer.parseInt(matcher.group(1)));
        }
        LinkedHashSet<String> links = dbLink.getLinks();
        Vector<String> linksInVector = new Vector<>(links);
        Vector<String> linksInOrder = new Vector<>();
        for (Integer i : docIndex) linksInOrder.add(linksInVector.get(i));
        for (String i : linksInOrder) System.out.println(i);

        return docs;
    }

    // return maximum Term Frequencies of each documents
    public Vector<Integer> getMaxTF() throws IOException, RocksDBException {
        Vector<Integer> mostTF = new Vector<>();
        LinkedHashSet<String> links = dbLink.getLinks();
        Iterator<String> itr = links.iterator();
        Vector<String> wordContainer;
        while (itr.hasNext()) {
            String link = itr.next();
//            System.out.println(link);
            wordContainer = dbWord.extractWords(link);
            Vector<Integer> counter = new Vector<>();
            HashSet<String> temp = new HashSet<>(wordContainer);
            for (String s : temp) {
                counter.add(Collections.frequency(wordContainer, s));
            }
            mostTF.add(Collections.max(counter));
        }
        return mostTF;
    }

    public HashMap getDocumentTF(String html) throws IOException, RocksDBException {
        Vector<String> result = dbWord.extractWords(html);
        HashSet<String> temp = new HashSet<>(result);
        temp.remove("");
//        System.out.print(temp);
        HashMap<String, Integer> documentTF = new HashMap<>();
        for (String s : temp) documentTF.put(s,Collections.frequency(result, s));
        return documentTF;
    }

    public Integer getDocumentDF(String word) {
        // get the documents from inverted index correspond to the word
        String getInvInd = ""; int result = 1;
        this.dbl = dbWord.getDB();
        RocksIterator itr = this.dbl.newIterator();
        for(itr.seekToFirst(); itr.isValid(); itr.next()) {
            if ((new String(itr.key()).equals(word))) {
                getInvInd = new String(itr.value());
            }
        }
        Pattern pattern = Pattern.compile("doc.+");
        StringTokenizer st = new StringTokenizer(getInvInd);
        String compDoc = "";
        if (st.hasMoreTokens()) {
            compDoc = st.nextToken();
        }
        while (st.hasMoreTokens()) {
            String test = st.nextToken();
            Matcher matcher = pattern.matcher(test);
            if (matcher.find()) {
                if (!matcher.group(0).equals(compDoc)) {
                    ++result;
                    compDoc = matcher.group(0);
                }
            }
        }
        return result;
    }

    public List<String> calculateCosSim(Vector<String> queryDoc, Vector<String> query) throws IOException, RocksDBException {
        Vector<String> docs = new Vector<>();

        // get the documents that contains the query term
        Pattern pattern = Pattern.compile("doc.+");
        for(int i = 0; i < queryDoc.size(); i++) {
            StringTokenizer st = new StringTokenizer(queryDoc.get(i));
            String compDoc = st.nextToken();
            docs.add(compDoc);
            while (st.hasMoreTokens()) {
                String test = st.nextToken();
                Matcher matcher = pattern.matcher(test);
                if (matcher.find()) {
                    if (!matcher.group(0).equals(compDoc)) {
                        compDoc = matcher.group(0);
                        docs.add(compDoc);
                    }
                }
            }
        }

//        for (String i : docs) System.out.println(i);

//        for (int i = 0; i < docs.size(); i++) {
//            System.out.println(docs.get(i));
//        }

        // calculate the sum of the document weight
        HashMap<String,Double> sum = new HashMap<>();
        this.dbl = dbWeight.getDB();
        RocksIterator itr = this.dbl.newIterator();
        for (int i = 0; i < docs.size(); i++) {
            for (itr.seekToFirst(); itr.isValid(); itr.next()) {
                if (new String(itr.key()).equals(docs.get(i))) {
                    StringTokenizer st1 = new StringTokenizer(new String(itr.value()));
                    int getScores = 1; double scores = 0;
                    while(st1.hasMoreTokens()) {
                        if (getScores % 2 == 0) {
                            scores += Math.pow(Double.parseDouble(st1.nextToken()),2);
                        } else {
                            st1.nextToken();
                        }
                        getScores++;
                    }
                    sum.put(new String(itr.key()),Math.sqrt(scores));
                }
            }
        }
//        for(String i : sum.keySet()) {
//            System.out.println("doc number: " + i + "\t" + "weight: " + sum.get(i));
//        }

        // calculate the inner product of document and query
        RocksIterator itr1 = this.dbl.newIterator();
        HashMap<String,Double> innerProduct = new HashMap<>();

        for (int i = 0; i < docs.size(); i++) {
            for (itr1.seekToFirst(); itr1.isValid(); itr1.next()) {
                if ((new String(itr1.key())).equals(docs.get(i))) {
//                    System.out.println(new String(itr1.value()));
                    StringTokenizer splitQueryDoc = new StringTokenizer(new String(itr1.value()));
                    Vector<String> temp = new Vector<>();
                    while (splitQueryDoc.hasMoreTokens()) {
                        temp.add(splitQueryDoc.nextToken());
                    }
                    double weight = 0;
                    for (String queryStr : query) {
                        if (temp.contains(queryStr)) {
                            weight += Double.parseDouble(temp.get(temp.indexOf(queryStr)+1));
                        }
                    }
                    innerProduct.put(docs.get(i),weight);
                }
            }
        }

//        for (String i : innerProduct.keySet()) {
//            System.out.println("doc: " + i + "\t" + "weight: " + innerProduct.get(i));
//        }

        // calculate query weights
        Vector<Integer> queryTF = new Vector<>();
        HashSet<String> temp = new HashSet<>(query);
        for (String i : temp) queryTF.add(Collections.frequency(query, i));

        double queryProduct = 0;
        for (Integer i : queryTF) queryProduct += (Math.pow(i,2));
        queryProduct = Math.sqrt(queryProduct);

//        for (int i = 0; i < queryTF.size(); i++) {
//            System.out.println(queryProduct);
//        }

        // calculate the cosine similarity
        HashMap<String,Double> cosSim = new HashMap<>();
        double documentLen, queryLen, result;
        for (String i : innerProduct.keySet()) {
            documentLen = sum.get(i);
            queryLen = queryProduct;
            result = innerProduct.get(i)/(documentLen*queryLen);
            cosSim.put(i,result);
        }

//        for (String i : cosSim.keySet()) {
//            System.out.println("doc " + i + "\t" + "cosine similarity: " + cosSim.get(i));
//        }

        HashMap<String, Double> sortedCosSim = sortByValue(cosSim);
        List<String> returnDocs = new ArrayList<>();
        for (Map.Entry<String, Double> en : sortedCosSim.entrySet()) {
            returnDocs.add(en.getKey());
        }

//        for (String i : returnDocs) System.out.println(i);

        return returnDocs;

    }

    public static HashMap<String, Double> sortByValue(HashMap<String, Double> hm)
    {
        // Create a list from elements of HashMap
        List<Map.Entry<String, Double> > list =
                new LinkedList<Map.Entry<String, Double> >(hm.entrySet());

        // Sort the list
        Collections.sort(list, new Comparator<Map.Entry<String, Double> >() {
            public int compare(Map.Entry<String, Double> o1,
                               Map.Entry<String, Double> o2)
            {
                return (o2.getValue()).compareTo(o1.getValue());
            }
        });

        // put data from sorted list to hashmap
        HashMap<String, Double> temp = new LinkedHashMap<String, Double>();
        for (Map.Entry<String, Double> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }

    public void isPhrase(String word, int x, int y) {
        this.dbl = dbWord.getDB();
        // save all the document number and the position of the number
        HashMap<String,String> docs = new HashMap<>();
        RocksIterator itr = this.dbl.newIterator();
        for(itr.seekToFirst(); itr.isValid(); itr.next()) docs.put(new String(itr.key()), new String(itr.value()));

        // compare if it is a phrase

    }

}
