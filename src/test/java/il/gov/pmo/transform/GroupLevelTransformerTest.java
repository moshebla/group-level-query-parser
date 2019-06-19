package il.gov.pmo.transform;

import org.apache.solr.SolrTestCaseJ4;
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

        indexSampleData();
    }

    @After
    public void cleanup() {
        assertU(delQ("*:*"));
        assertU(commit());
    }

    @Ignore
    @Test
    public void groupLevelTransformer() {

        // TODO - stoped here
        assertQ(req("q", "id:1", "fl", "*,[groups allowedGroups=z f=groups]"),
                "//*[@isAllowed='false']");
    }

    private static void indexSampleData() throws Exception {
        String[] mockGroups = new String[]{"a", "b", "c"};

        for (int i = 0; i < 10; i++) {
            addAndGetVersion(sdoc("id", idCounter.incrementAndGet(), "groups", mockGroups, "classified","only for allowed users"),
                    params("wt", "json"));
        }
        assertU(commit());
        //JQ(req("q", "*:*"));
    }
}
