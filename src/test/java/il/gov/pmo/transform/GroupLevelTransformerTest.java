package il.gov.pmo.transform;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.io.Tuple;
import org.apache.solr.common.SolrInputDocument;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class GroupLevelTransformerTest extends SolrTestCaseJ4 {

    private static AtomicInteger idCounter = new AtomicInteger();

    @BeforeClass
    public static void beforeClass() throws Exception {
        initCore("solrconfig.xml", "schema-string-doc-vals.xml");
    }

    @After
    public void cleanup() {
        assertU(delQ("*:*"));
        assertU(commit());
    }

    @Ignore
    @Test
    public void groupLevelTransformer() throws Exception {
        indexSampleData(10, new String[]{"a", "b", "c"}, new String[]{"description", "stam text lo meanyen", "classified", "only for allowed users"});
        assertQ(req("q", "id:1", "fl", "*,[groups allowedGroups=z f=groups]"),
                "//*[@name=\"isAllowed\" and text()=\"false\"]");

//        indexSampleData(10, new String[]{"a", "b", "c"}, new String[]{"classified", "only for allowed users"});
//        assertQ(req("q", "id:1", "fl", "*,[groups allowedGroups=z f=groups]"),
//                "//*[@name=\"isAllowed\" and text()=\"false\"]");
//        cleanup();
//        indexSampleData(7);
//        assertQ(req("q", "*:*"),
//                "//*[@numFound='7']");
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
        //JQ(req("q", "*:*"));
    }
}
