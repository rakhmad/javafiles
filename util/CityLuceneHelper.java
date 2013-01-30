package com.msci.moslem.util;

import com.msci.moslem.bean.CityBean;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.spatial.tier.DistanceQueryBuilder;
import org.apache.lucene.spatial.tier.projections.CartesianTierPlotter;
import org.apache.lucene.spatial.tier.projections.IProjector;
import org.apache.lucene.spatial.tier.projections.SinusoidalProjector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.NumericUtils;
import org.apache.lucene.util.Version;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class provides function for interact with Lucene. We use Lucene Spatial
 * for providing Proximity Search for a city.
 *
 * @author Rakhmad Azhari <r.azhari@samsung.com> Date: 1/4/13 Time: 1:51 PM
 * @version 0.0.1 Initial version for SpatialSearch. Already covered most of all
 *          spatial search. No configuration / local properties used.
 */
public class CityLuceneHelper {

    //    private IndexWriter indexWriter;
    private IndexSearcher indexSearcher;

    private static CityLuceneHelper instance;

    // TO-DO: Add configuration file (localconfiguration.properties)

    private String path;

    /**
     * Spatial Stuff. Must research more. MAGIC STARTS HERE.
     */
    private static final String LAT_FIELD = "lat";
    private static final String LON_FIELD = "lon";

    private static final String TIER_PREFIX_FIELD = "_localTier";

    private double maxMiles = 10;
    private double minMiles = 1;

    private IProjector projector = new SinusoidalProjector();
    private CartesianTierPlotter ctp0 = new CartesianTierPlotter(0, projector,
            TIER_PREFIX_FIELD);

    private int startTier = ctp0.bestFit(maxMiles);
    private int endTier = ctp0.bestFit(minMiles);

    private AtomicInteger activeWriter = new AtomicInteger(0),
            activeSearcher = new AtomicInteger(0);

    /**
     * END OF SPATIAL STUFF. MAGIC STOPS HERE.
     */

    /**
     * Private Constructor.
     */
    private CityLuceneHelper() {


    }

    /**
     * Factory method for CityLuceneHelper.
     *
     * @return object CityLuceneHelper
     */
    public static CityLuceneHelper getInstance() {
        if (instance == null)
            instance = new CityLuceneHelper();
        return instance;
    }

    public void setPath(String path) {
        this.path = path;

        // check apa directory ada ?
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }


    }

    /**
     * Preparing Index for Lucene.
     *
     * @return indexWriter object.
     */
    public IndexWriter getIndexWriter() throws IOException {

        Directory dir = FSDirectory.open(new File(path));
        Analyzer analyzer = new WhitespaceAnalyzer(Version.LUCENE_36);
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_36,
                analyzer);
        IndexWriter indexWriter = new IndexWriter(dir,
                config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND));
        return indexWriter;
    }

    /**
     * Clear Index for Lucene.
     *
     * @return indexWriter object.
     */
    public void clearIndex() throws IOException {
        IndexWriter indexWriter = getIndexWriter();
        indexWriter.deleteAll();
        indexWriter.commit();
        indexWriter.close();
        indexSearcher = null;
    }

    /**
     * Delete a city from Lucene's index.
     *
     * @param city CityBean object reference to delete.
     */
    public void removeFromIndex(CityBean city) throws IOException {
        Term term = new Term("id", Long.toString(city.getId()));
        IndexWriter indexWriter = getIndexWriter();
        indexWriter.deleteDocuments(term);
        indexWriter.commit();
        indexWriter.close();
        indexSearcher = null;
    }

    /**
     * Adding new Location to Lucene database. We store city's name, longitude
     * and latitude information into Lucene's index.
     *
     * @param city CityBean object to add into Lucene's index.
     */
    public void addToIndex(CityBean city) throws IOException {

        // remove city with same code !

        // Preparing Document object
        Document doc = new Document();

        // Add city information to index.
        doc.add(new Field("id", Long.toString(city.getId()), Field.Store.YES,
                Field.Index.NOT_ANALYZED));
        doc.add(new Field("json", new ObjectMapper().writeValueAsString(city),
                Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field("metafile", "doc", Field.Store.YES,
                Field.Index.NOT_ANALYZED));

        // Add Spatial Location starts here.
        doc.add(new Field(LAT_FIELD, NumericUtils.doubleToPrefixCoded(city
                .getLatitude()), Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field(LON_FIELD, NumericUtils.doubleToPrefixCoded(city
                .getLongitude()), Field.Store.YES, Field.Index.NOT_ANALYZED));

        /**
         * 07.01.2013 Still have no idea what this code block do. Must read some
         * reference more. First attempt, let's just pray it works. ^^v
         */
        for (int tier = startTier; tier <= endTier; tier++) {
            CartesianTierPlotter ctp = new CartesianTierPlotter(tier,
                    projector, TIER_PREFIX_FIELD);
            double boxId = ctp.getTierBoxId(city.getLatitude(),
                    city.getLongitude());
            doc.add(new Field(ctp.getTierFieldName(), NumericUtils
                    .doubleToPrefixCoded(boxId), Field.Store.YES,
                    Field.Index.NOT_ANALYZED_NO_NORMS));
        }

        IndexWriter indexWriter = getIndexWriter();

        try {
            activeWriter.incrementAndGet();
            // Adding Document to Lucene index.
            indexWriter.addDocument(doc);
        } finally {
            synchronized (this) {
                int write = activeWriter.decrementAndGet();
                if (write == 0) {
                    indexWriter.commit();
                    indexWriter.close();
                }
            }
        }

        indexSearcher = null;
    }

    public List<CityBean> getNearbyCities(double lat, double lon, double miles)
            throws IOException {

        if (indexSearcher == null) {
            Directory dir = FSDirectory.open(new File(path));

            indexSearcher = new IndexSearcher(IndexReader.open(dir));
        }

        /**
         * Build Query.
         */

        DistanceQueryBuilder queryBuilder = new DistanceQueryBuilder(lat, lon,
                miles, LAT_FIELD, LON_FIELD, TIER_PREFIX_FIELD, false,
                startTier, endTier);

        TermQuery tq = new TermQuery(new Term("metafile", "doc"));

        TopDocs hits;
        try {
            hits = indexSearcher.search(queryBuilder.getQuery(tq), 100);
        } finally {
            activeSearcher.decrementAndGet();
        }

        List<CityBean> cityList = new ArrayList<CityBean>();
        for (int i = 0; i < hits.scoreDocs.length; i++) {
            Document doc = indexSearcher.doc(hits.scoreDocs[i].doc);
            String json = doc.get("json");
            cityList.add(new ObjectMapper().readValue(json, CityBean.class));
        }
        return cityList;
    }

    public CityBean getNearbyCities(double lat, double lon) throws IOException {

        double MAX_DISTANCE = 20E3;
        double distance = 1;

        List<CityBean> cityList = new ArrayList<CityBean>();

        while (cityList.size() == 0 && distance < MAX_DISTANCE) {

            cityList = getNearbyCities(lat, lon, distance);
            distance = 2 * distance;
        }

        double currentDistance = 0;
        CityBean city = null;

        for (CityBean cityI : cityList) {
            double distanceI = distance(lat, lon, cityI);
            if (city == null) {
                city = cityI;
                currentDistance = distanceI;
            } else {
                if (distanceI < currentDistance) {
                    city = cityI;
                    currentDistance = distanceI;
                }
            }
        }

        return city;
    }

    public double distance(double lat, double lon, CityBean city) {
        double distX = Math.abs(city.getLongitude() - lon);
        double distY = Math.abs(city.getLatitude() - lat);
        return Math.sqrt((distX * distX) + (distY * distY));
    }
}
