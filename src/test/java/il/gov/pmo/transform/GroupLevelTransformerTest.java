package il.gov.pmo.transform;

import il.gov.pmo.GroupLevelUtils;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.BasicResultContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

public class GroupLevelTransformerTest extends SolrTestCaseJ4 {

    private static AtomicInteger idCounter = new AtomicInteger();

    @BeforeClass
    public static void beforeClass() throws Exception {
        initCore("solrconfig.xml", "schema-string-doc-vals.xml");

        String[] extraFieldsToIndex = new String[]{"description", "stam text lo meanyen", "classified", "only for allowed users"};
        indexSampleData(1, new String[]{"a", "b", "c"}, extraFieldsToIndex);
    }

    @AfterClass
    public static void cleanup() {
        assertU(delQ("*:*"));
        assertU(commit());
    }

    @Test
    public void groupLevelTransformerUserAllowedTest() throws Exception {
        String[] extraFieldsToIndex = new String[]{"description", "stam text lo meanyen", "classified", "only for allowed users"};
        indexSampleData(1, new String[]{"a"}, extraFieldsToIndex);

        // check specific field values
        assertQ("when user allowed all field should be present",
                req("q", "id:1", "fl", "*,[groups allowedGroups=a f=groups]"),
                "/response/result[@numFound=\"1\"]/doc/*[(@name=\"id\" and text()=\"1\")]",
                "/response/result/doc/*[(@name=\"classified\" and text()=\"only for allowed users\")]",
                "/response/result/doc/*[(@name=\"description\" and text()=\"stam text lo meanyen\")]",
                "/response/result/doc/*[@name=\"_version_\"]",
                "/response/result/doc/*[(@name=\"isAllowed\" and text()=\"true\")]");
    }

    @Test
    public void groupLevelTransformerUserNotAllowedTest() throws Exception {
        try (SolrQueryRequest solrQueryRequest = req("q", "id:1", "fl", "*,[groups allowedGroups=z f=groups]")) {
            // check specific field values
            assertQ("when user not allowed only public field supposed to be present",
                    solrQueryRequest,
                    "/response/result[@numFound=\"1\"]/doc/*[(@name=\"id\" and text()=\"1\")]",
                    "/response/result/doc/*[(@name=\"description\" and text()=\"stam text lo meanyen\")]",
                    "/response/result/doc/*[@name=\"_version_\"]",
                    "/response/result/doc/*[(@name=\"isAllowed\" and text()=\"false\")]");

            // check field existence
            BasicResultContext basicResultContext = (BasicResultContext) h.queryAndResponse("/select", solrQueryRequest).getResponse();
            Iterator<SolrDocument> docsStreamer = basicResultContext.getProcessedDocuments();
            while (docsStreamer.hasNext()) {
                SolrDocument solrDocument = docsStreamer.next();
                assertTrue("when user not allowed only public field supposed to be present", GroupLevelUtils.PUBLIC_FIELDS.containsAll(solrDocument.getFieldNames()));
            }
        }
    }

    private static void indexSampleData(int size) throws Exception {
        indexSampleData(size, new String[]{}, new String[]{});
    }

    private static void indexSampleData(int size, String[] groups, String[] extraField) throws Exception {

        for (int i = 0; i < size; i++) {
            SolrInputDocument sdoc = sdoc("id", idCounter.incrementAndGet());
            if (groups.length > 0) {
                sdoc.addField("groups", groups);
            }

            for (int index = 0; index + 1 < extraField.length; index += 2) {
                sdoc.addField(extraField[index], extraField[index + 1]);
            }

            addAndGetVersion(sdoc, params("wt", "json"));
        }
        assertU(commit());
    }
}
