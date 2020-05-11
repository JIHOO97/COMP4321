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
      private Crawler crawler;
      private RocksDB dbl;
      private String html;
      private int pageNumbers;

    public DBReader(String dbPath1, String dbPath2, String dbPath3, String dbPath4) throws RocksDBException {
        this.dbLink = new InvertedIndex(dbPath1);
        this.dbWord = new InvertedIndex(dbPath2);
        this.dbWeight = new InvertedIndex(dbPath3);
        this.dbTitle = new InvertedIndex(dbPath4);
        this.crawler = new Crawler();
    }

    public void setLinkAndPN(String html, int pageNumbers) {
        this.html = html;
        this.pageNumbers = pageNumbers;
    }

    public LinkedHashSet<String> crawlLinks() {
        LinkedHashSet<String> links = this.crawler.getPageLinks(this.html, this.pageNumbers);
        return links;
    }

    public void saveLink() throws IOException, ParseException, RocksDBException {
        LinkedHashSet<String> links = crawlLinks();
        Vector<Date> lastModDate = this.crawler.getLastModificationDate(links);
        Iterator<String> itr = links.iterator();
        for (int i = 0; i < lastModDate.size(); ++i) {
            String html = itr.next();
            this.dbLink.addLink(html,lastModDate.get(i),i);
        }
    }

    public void saveWords() throws IOException, ParseException, RocksDBException {
        LinkedHashSet<String> links = crawlLinks();
        Iterator<String> itr = links.iterator(); int docNumber = 0;
        while (itr.hasNext()) {
            String html = itr.next();
            Vector<String> words = dbWord.extractWords(html);
            for (int i = 0; i < words.size(); ++i) {
                dbWord.addEntry(words.get(i),docNumber,i);
            }
            ++docNumber;
        }
    }

    public void saveTitle() throws IOException, RocksDBException {
        LinkedHashSet<String> links = crawlLinks();
        Iterator<String> itr = links.iterator(); int index = 0;
        while (itr.hasNext()) {
            String html = itr.next();
            String title = dbTitle.extractTitle(html);
            dbTitle.addTitle(title,index);
            index++;
        }
//        dbTitle.printAll();
    }

    public void saveDocumentWeight() throws IOException, RocksDBException {
        LinkedHashSet<String> links = crawlLinks();
        Vector<Integer> maxTF = getMaxTF();
        double documentDF, documentTF;
        Iterator<String> itr = links.iterator(); int index = 0;
        while(itr.hasNext()) {
            String doc = "doc" + index;
            String html = itr.next();
            HashMap<String,Integer> temp = getDocumentTF(html);
            // Some html has no content
            if (temp != null) {
                for (String i : temp.keySet()) {
                    // how many documents contain the string i
//                    System.out.println(i);
                    documentDF = getDocumentDF(i);
//                    if(documentDF == 0) System.out.println(html);
                    // frequency of the term i in the doc(index)
                    documentTF = temp.get(i);
                    double docWeight = documentTF * (Math.log(this.pageNumbers / documentDF) / Math.log(2)) / maxTF.get(index);
                    dbWeight.addWeight(doc, i, docWeight);
                }
            } else
                dbWeight.addWeight(doc,"",0);
            index++;
        }
    }

    public String getTitle(String query) {
        String result = "";
        query = query.toLowerCase();

        this.dbl = dbTitle.getDB();
        RocksIterator itr = this.dbl.newIterator();

        for(itr.seekToFirst(); itr.isValid(); itr.next()) {
            String lowerCaseTitle = new String(itr.key()).toLowerCase();
            if(lowerCaseTitle.contains(query)) {
                result += new String(itr.value()) + " ";
            }
        }

        return result;
    }



    // shows the result with respect to the given query
    public List<List<String>> showResult(String query) throws IOException, RocksDBException {
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
                    docs.add(new String(itr.value())); break;
                }
            }
        }

//        Vector<String> hi = dbWord.extractWords("https://www.chinadailyhk.com/articles/14/65/28/1541125674562.html?newsId=52460");
//        for(String i : hi) System.out.println(i);

//
//        for (int i = 0; i < docs.size(); i++) System.out.println(docs.get(i));
//
//        dbWord.printAll();

        List<String> docContainer1 = new ArrayList<>();

        List<String> documents = calculateCosSim(docs,Query);
        Vector<Integer> docIndex = new Vector<>();
        Pattern pattern = Pattern.compile("doc(.+)");
        for (String i : documents) {
            Matcher matcher = pattern.matcher(i);
            matcher.find();
            docIndex.add(Integer.parseInt(matcher.group(1)));
            docContainer1.add(matcher.group(0));
        }

        LinkedHashSet<String> links = crawlLinks();
        Vector<String> linksInVector = new Vector<>(links);
//        System.out.println(linksInVector.get(39));

        // documents with title
        String result = getTitle(query);
        StringTokenizer splitTitle = new StringTokenizer(result);
        Vector<Integer> docNumber = new Vector<>();
        while(splitTitle.hasMoreTokens()) {
            Matcher secMatcher = pattern.matcher(splitTitle.nextToken());
            secMatcher.find();
            docNumber.add(Integer.parseInt(secMatcher.group(1)));
            docContainer1.add(secMatcher.group(0));
        }

        LinkedHashSet<String> matchedLinks = new LinkedHashSet<>();
        for (Integer i : docNumber) matchedLinks.add(linksInVector.get(i));
        for (Integer i : docIndex) {
//            System.out.println(i);
            matchedLinks.add(linksInVector.get(i));
        }

//        dbWord.printAll();

        List<List<String>> finalResult = new ArrayList<>();

        Iterator<String> printItr = matchedLinks.iterator();
        int index = 0;

        while(printItr.hasNext()) {
            String html = printItr.next();
            List<String> tempResult = new ArrayList<>();
            // print title
            this.dbl = dbTitle.getDB();
            RocksIterator rockTitle = dbl.newIterator();
            for(rockTitle.seekToFirst(); rockTitle.isValid(); rockTitle.next()) {
                if(new String(rockTitle.value()).contains(docContainer1.get(index))) {
                    tempResult.add(new String(rockTitle.key())); index++; break;
                }
            }

            // print link
            tempResult.add(html);

            // print last modified date
            this.dbl = dbLink.getDB();
            RocksIterator rockLink = dbl.newIterator();
            for(rockLink.seekToFirst(); rockLink.isValid(); rockLink.next()) {
                if(new String(rockLink.key()).contains(html)) {
                    tempResult.add(new String(rockLink.value())); break;
                }
            }

            // print keywords and frequencies
            HashMap<String,Integer> tempKeyFreq = getDocumentTF(html);
            String keyFreq = "";
            for(String i : tempKeyFreq.keySet()) {
                keyFreq += i + " " + tempKeyFreq.get(i) + " ";
            }
            tempResult.add(keyFreq);

            // print parent links
            int parentIndex = linksInVector.indexOf(html);
            int childIndex = linksInVector.indexOf(html);
            String tempParentLink = ""; String tempChildLink = "";
            for(int j = parentIndex; j >= 0; j--) {
                tempParentLink += linksInVector.get(j) + " ";
            }
            for(int j = childIndex; j < linksInVector.size(); j++) {
                tempChildLink += linksInVector.get(j) + " ";
            }
            tempResult.add(tempParentLink);
            tempResult.add(tempChildLink);
            finalResult.add(tempResult);

            if (index == 49) break;
        }

        for(List<String> temp : finalResult) {
            for(String i : temp) {
                System.out.println(i);
            }
            System.out.println();
        }


        return finalResult;
    }


    public List<String> getDocumentList(String docList) {
        List<String> docs = new ArrayList<>();
        Pattern pattern = Pattern.compile("doc.+");
        StringTokenizer st = new StringTokenizer(docList);
        String compDoc = "";
        if (st.hasMoreTokens()) compDoc = st.nextToken();
        else return null;
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
        return docs;
    }

    // return maximum Term Frequencies of each documents
    public Vector<Integer> getMaxTF() throws IOException, RocksDBException {
        Vector<Integer> mostTF = new Vector<>();

        LinkedHashSet<String> links = crawlLinks();
        Iterator<String> itr = links.iterator();

        Vector<String> wordContainer;
        while (itr.hasNext()) {
            String link = itr.next();
            wordContainer = dbWord.extractWords(link);
            if (!wordContainer.isEmpty()) {
                Vector<Integer> counter = new Vector<>();
                HashSet<String> temp = new HashSet<>(wordContainer);
                for (String s : temp) {
                    counter.add(Collections.frequency(wordContainer, s));
                }
                mostTF.add(Collections.max(counter));
            } else
                mostTF.add(0);
        }
        return mostTF;
    }

    // Store the word and its frequency with given URL
    // Some contents of the pages can be null
    public HashMap getDocumentTF(String html) throws IOException, RocksDBException {
        Vector<String> result = dbWord.extractWords(html);
        if(!result.isEmpty()) {
            HashSet<String> temp = new HashSet<>(result);
            temp.remove("");
//        System.out.print(temp);
            HashMap<String, Integer> documentTF = new HashMap<>();
            for (String s : temp) documentTF.put(s, Collections.frequency(result, s));
            return documentTF;
        } else
            return null;
    }

    // how many documents contain this word
    public Integer getDocumentDF(String word) {
        // get the documents from inverted index correspond to the word
        String getInvInd = "";
        this.dbl = dbWord.getDB();
        RocksIterator itr = this.dbl.newIterator();
        for(itr.seekToFirst(); itr.isValid(); itr.next()) {
            if ((new String(itr.key()).equals(word))) {
                getInvInd = new String(itr.value()); break;
            }
        }

        if(getDocumentList(getInvInd) == null) {
            System.out.println(word + " 화이팅!!");
            return 0;
        }
        else
            return getDocumentList(getInvInd).size();
    }


    public List<String> calculateCosSim(Vector<String> queryDoc, Vector<String> query) throws IOException, RocksDBException {
        List<String> docs = new ArrayList<>();

        // get the documents that contains the query term
        for(int i = 0; i < queryDoc.size(); i++) {
            List<String> temp = getDocumentList(queryDoc.get(i));
            for(String j : temp) {
                if(!docs.contains(j))
                    docs.add(j);
            }
        }

//        for (String i : docs) System.out.println(i);
//        System.out.println();

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
        HashSet<String> temp = new HashSet<>(query);
        Vector<Integer> queryTF = new Vector<>();
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
//            System.out.println(en.getKey());
        }
//        System.out.println();


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
