package il.gov.pmo;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.SolrException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;


public class GroupLevelQParserPluginTest extends SolrTestCaseJ4 {

    private static AtomicInteger idCounter = new AtomicInteger();


    @BeforeClass
    public static void beforeClass() throws Exception {
        initCore("solrconfig.xml", "schema-string-doc-vals.xml");
    }

    @Before
    public void before() throws Exception {
        deleteByQueryAndGetVersion("*:*", params());
        idCounter.set(0);
    }

    @Test
    public void GroupsQueryQParserPluginTest() throws Exception {
        indexSampleData();

        assertQ(req("q", "*:*"),
                "//*[@numFound='10']");

        assertQ(req("q", "*:*", "fq", "{!acl cache=false cost=1000 f=groups delimiter=','}a,b,c"),
                "//*[@numFound='10']");

        assertQ(req("q", "*:*", "fq", "{!acl cache=false cost=1000 f=groups delimiter=','}d"),
                "//*[@numFound='0']");
    }

    @Test
    public void GroupsQueryQParserPluginExceptionTest() throws Exception {
        indexSampleData();

        assertQEx("should fail because no groups were supplied",
                "groups must be supplied",
                req("q", "*:*", "fq", "{!acl cache=false cost=1000 f=groups delimiter=','}"),
                SolrException.ErrorCode.BAD_REQUEST);

    }

    private void indexSampleData() throws Exception {
        for(int i = 0; i < 10; i++) {
            addAndGetVersion(sdoc("id", idCounter.incrementAndGet(), "groups", "a,b,c"),
                    params("wt", "json"));
        }
        assertU(commit());
    }
}