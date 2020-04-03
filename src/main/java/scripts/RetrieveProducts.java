package scripts;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.opencsv.CSVWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class RetrieveProducts {

    private static Logger log = LogManager.getLogger(RetrieveProducts.class);

    public void start(String url, String pathForCSV) {
        ArrayListMultimap<String, String> productsMap = ArrayListMultimap.create();

        Set<String> allProductPages = Sets.newHashSet();
        int cont = 0;
        try {
            Document document = Jsoup.connect(url).get();
            List<String> homepageLinks = getUrls(document, ".menu-content a");

            if (homepageLinks != null) {
                int localCont = 0;
                for (String link : homepageLinks) {
                    localCont = localCont + 1;
                    Document doc = Jsoup.connect(link).get();
                    List<String> productsForCategory = getUrls(doc, "a.product-images__link");
                    allProductPages.addAll(productsForCategory);
                    cont = cont + productsForCategory.size();
                    try {
                        log.info("sleeping 10 seconds....");
                        Thread.sleep(10000);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                    if (localCont == 3) {
                        break;
                    }
                }
            }

            for (String productLink : allProductPages) {
                Document doc = Jsoup.connect(productLink).get();
                String productCode = retrieveProductCode(doc);
                if (productCode != null) {
                    productsMap.put(productCode, productLink);
                }
                try {
                    log.info("sleeping 1 second....");
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        } catch (IOException e) {
            log.error(e);
        }

        createCsv(productsMap, pathForCSV);

        log.warn("Total Products " + cont + " - Real: " + allProductPages.size());
    }


    private List<String> getUrls(Document document, String selector) {
        List<String> urls = Lists.newArrayList();

        Elements results = document.select(selector);
        if (results != null) {
            for (Element result : results) {
                String href = result.attr("href");
                log.info(href);
                urls.add(href);
            }
        }

        return urls;
    }

    public String retrieveProductCode(Document document) {
        Elements results = document.select("p.header span.value");
        if (results != null && results.first() != null) {
            return results.first().text();
        }
        return null;
    }

    public void createCsv(ArrayListMultimap<String, String> arrayListMultimap, String pathForCSV) {
        try {
            Writer writer = Files.newBufferedWriter(Paths.get(pathForCSV));

            CSVWriter csvWriter = new CSVWriter(writer,
                    CSVWriter.DEFAULT_SEPARATOR,
                    CSVWriter.NO_QUOTE_CHARACTER,
                    CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                    CSVWriter.DEFAULT_LINE_END);

            String[] headerRecord = {"Product Code", "Product Url"};
            csvWriter.writeNext(headerRecord);
            Map<String, Collection<String>> map = arrayListMultimap.asMap();
            Set<Map.Entry<String, Collection<String>>> entries = map.entrySet();
            Iterator<Map.Entry<String, Collection<String>>> iterator = entries.iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Collection<String>> elem = iterator.next();
                String productCode = elem.getKey();
                Collection<String> links = elem.getValue();
                for (String link : links) {
                    csvWriter.writeNext(new String[]{productCode, link});
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
