package IRUtilities.company;

import com.sun.istack.internal.NotNull;
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
    private Vector<String> words;

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
                if(!words.get(i).equals(""))
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
        System.out.println("now inserting document weight");
        LinkedHashSet<String> links = crawlLinks();
        Iterator<String> linkItr = links.iterator();
        Vector<Integer> maxTerms = getMaxTF();
        this.dbl = dbWord.getDB();
        int docIndex = 0;
        while(linkItr.hasNext()) {
            String doc = "doc" + docIndex;
            String currentDoc = linkItr.next();
            Vector<String> extractWords = dbWord.extractWords(currentDoc);
            Set<String> distinctWords = new HashSet<>(extractWords);
            distinctWords.remove("");
            Map<String,Integer> wordFreq = new HashMap<>();
            for(String i : distinctWords) wordFreq.put(i,Collections.frequency(extractWords,i));
            RocksIterator wordItr = dbl.newIterator();
            for(String i : wordFreq.keySet()) {
                String docList = "";
                for(wordItr.seekToFirst(); wordItr.isValid(); wordItr.next()) {
                    if(new String(wordItr.key()).equals(i)) {
                        docList = new String(wordItr.value()); break;
                    }
                }
                double tf = wordFreq.get(i); double df;
                if(getDocumentList(docList) == null) {
                    this.dbWeight.addWeight(doc,i,0);
                    System.out.println(doc + " " + i + " " + currentDoc);
                } else {
                    df = getDocumentList(docList).size();
                    double docWeight = tf * (Math.log(this.pageNumbers / df)) / maxTerms.get(docIndex);
                    this.dbWeight.addWeight(doc,i,docWeight);
                }
            }
            docIndex++;
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

    // function for stopword
    public Vector<String> applyStopWord(String query) {
        Vector<String> result = new Vector<>();

        StringTokenizer st = new StringTokenizer(query);
        while (st.hasMoreTokens()) {
            result.add(st.nextToken());
        }

        StopStem process = new StopStem("C:\\Users\\User\\Desktop\\COMP4321\\Phase 1\\COMP4471_Crawler\\stopwords.txt");
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



    // shows the result with respect to the given query
    public List<List<String>> showResult(String query) throws IOException, RocksDBException {
        // shows which documents and its position that contain the query
        // check if the query is phrase or not
        Vector<String> Query = new Vector<>();
        Vector<String> docs = new Vector<>();
        this.dbl = dbWord.getDB();
        String fakeQuery = "";
        boolean isPhrase = false;

        Pattern phrasePattern = Pattern.compile(".*(\"(.+)\").*");
        Matcher phraseMatcher = phrasePattern.matcher(query);
        if(phraseMatcher.find()) {
            isPhrase = true;
            String phrase = phraseMatcher.group(2);
            Vector<String> tempQuery = applyStopWord(phrase);
//            for(String i : tempQuery) System.out.println(i);
            RocksIterator itr = this.dbl.newIterator();
            List<String> storeDocs = new ArrayList<>();
            for (int i = 0; i < tempQuery.size(); ++i) {
                Query.add(tempQuery.get(i));
                for(itr.seekToFirst(); itr.isValid(); itr.next()) {
                    if ((new String(itr.key()).equals(tempQuery.get(i)))) {
//                        System.out.println(new String(itr.value()));
                        storeDocs.add(new String(itr.value()));
                        break;
                    }
                }
            }
            if(storeDocs.size() < tempQuery.size()) storeDocs.clear();
            // find phrase
            List<String> phraseDoc = new ArrayList<>();
            if(storeDocs.size() > 1) {
                for(int i = 0; i < storeDocs.size()-1; i++) {
                    String firstDocs = storeDocs.get(i);
                    String secDocs = storeDocs.get(i+1);
                    StringTokenizer st1 = new StringTokenizer(firstDocs);
                    StringTokenizer st2 = new StringTokenizer(secDocs);
                    List<String> comp1 = new ArrayList<>();
                    List<String> comp2 = new ArrayList<>();
                    while(st1.hasMoreTokens()) comp1.add(st1.nextToken());
                    while(st2.hasMoreTokens()) comp2.add(st2.nextToken());

                    for(int j = 0; j < comp1.size(); j+=2) {
                        String compDoc = comp1.get(j); int index, wordPos1, wordPos2;
                        if(comp2.contains(compDoc)) {
                            wordPos1 = Integer.parseInt(comp1.get(j+1));
                            index = comp2.indexOf(compDoc);
                            for(int k = index; k < comp2.size(); k+=2) {
                                if(!comp2.get(k).equals(compDoc)) break;
                                wordPos2 = Integer.parseInt(comp2.get(k+1));
                                if(wordPos1 == (wordPos2-1)) {
                                    phraseDoc.add(compDoc); break;
                                }
                            }
                        }
                    }
                }
            }
            for(String i : phraseDoc) {
//                System.out.println(i);
                if(!docs.contains(i)) docs.add(i);
            }
            fakeQuery = query.replace(phraseMatcher.group(1),"");
        }


        Vector<String> newQuery = new Vector<>();
        if(isPhrase) {
            Vector<String> tempQuery = applyStopWord(fakeQuery);
            for (String i : tempQuery) newQuery.add(i);
        } else {
            Vector<String> tempQuery = applyStopWord(query);
            for(String i : tempQuery) newQuery.add(i);
        }


//        for(String i : docs) System.out.println(i);

        Vector<String> newQueryDocs = new Vector<>();
        RocksIterator itr = this.dbl.newIterator();
        for (int i = 0; i < newQuery.size(); ++i) {
            for(itr.seekToFirst(); itr.isValid(); itr.next()) {
                if ((new String(itr.key()).equals(newQuery.get(i)))) {
                    newQueryDocs.add(new String(itr.value())); break;
                }
            }
        }

        for(int i = 0; i < newQueryDocs.size(); i++) {
            List<String> temp = getDocumentList(newQueryDocs.get(i));
            for(String j : temp) {
                if(!docs.contains(j))
                    docs.add(j);
            }
        }

        for(String i : newQuery) Query.add(i);

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
//        System.out.println(linksInVector.get(22));

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
            matchedLinks.add(linksInVector.get(i));
        }

//        for(String i : matchedLinks) System.out.println(i);


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
                String titleSplitter = new String(rockTitle.value());
                StringTokenizer st = new StringTokenizer(titleSplitter);
                List<String> tempTitle = new ArrayList<>();
                while(st.hasMoreTokens()) tempTitle.add(st.nextToken());
                if(tempTitle.contains(docContainer1.get(index))) {
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
            Vector<String> keyFreq = dbWord.extractWords(html);
            HashSet<String> tempKeyFreq = new HashSet<>(keyFreq);
            tempKeyFreq.remove("");
            Map<String,Integer> kfResult = new HashMap<>();
            for(String i : tempKeyFreq) kfResult.put(i,Collections.frequency(keyFreq,i));
            String kfResusltStr = "";
            for(String i : kfResult.keySet()) kfResusltStr += (i + " " + kfResult.get(i) + " ");
            tempResult.add(kfResusltStr);


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
        String compDoc;
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


    public List<String> calculateCosSim(List<String> queryDoc, Vector<String> query) throws IOException, RocksDBException {
        List<String> docs = queryDoc;


//        for(String i : queryDoc) System.out.println(i);


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
        }

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


}
