package IRUtilities.company;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// TODO: Fetch contents of the pages with given URL
// TODO: Extract all the hyperlinks
// TODO: Build file structure with parent/child relation
// TODO: Check the inverted index, whether the page needs to be fetched or not. (If not, check the modification date to fetch)
// Why use LinkedHashSet? What is a HashSet?

public class Crawler {
    static Vector<String> pages = new Vector<>();
    static Queue<String> queue = new LinkedList<>();
    static LinkedHashSet<String> marked = new LinkedHashSet<>();
    private Vector<Date> lastModificationDate = new Vector<>();


    public LinkedHashSet<String> getPageLinks(String URL, int numPage) {
        // list of web pages to be examined
        queue.add(URL);
        // set of examined web pages
        marked.add(URL);
        Document doc;

        // breadth first search crawl of web
        OUTER:
        while (!queue.isEmpty()) {

            String v = queue.remove();

            if (marked.size() < numPage) {
                try {
                    // fetch HTML code in the URL
                    doc = Jsoup.connect(v).get();
                    // fetch page of give URL
                    pages.add(doc.toString());
                    // Parse the HTML to extract links to other URLs
                    Elements questions = doc.select("a[href]");
                    for (Element link : questions) {
                        // if it starts with http
                        if ((link.attr("abs:href").startsWith("http"))) {
                            if (marked.size() == numPage)
                                continue OUTER;
                            else {
                                queue.add(link.attr("abs:href"));
                                marked.add(link.attr("abs:href"));
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
        return marked;
    }


    public Vector<Date> getLastModificationDate(LinkedHashSet<String> link) throws IOException, ParseException {
        Iterator<String> itr = link.iterator();
        Pattern pattern = Pattern.compile(".*<meta name=\"date[a-z.]*\" content=\"(.*?)\">.*");
        while(itr.hasNext()) {
            URL url = new URL(itr.next());
            URLConnection connection = url.openConnection();
            if (connection.getLastModified() != 0) {
                Date lastModified = new Date(connection.getLastModified());
                lastModificationDate.add(lastModified);
            } else {
                Matcher matcher = pattern.matcher(url.toString());
                if (matcher.find()) {
                    Date newDate = new SimpleDateFormat("dd-MM-yyyy").parse(matcher.group(1));
                    lastModificationDate.add(newDate);
                } else {
                    Date lastDate = new Date(connection.getDate());
                    lastModificationDate.add(lastDate);
                }
            }
        }
        return this.lastModificationDate;
    }
}

