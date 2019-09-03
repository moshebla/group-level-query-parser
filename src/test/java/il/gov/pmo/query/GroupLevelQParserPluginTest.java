package il.gov.pmo.query;

import il.gov.pmo.GroupLevelUtils;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.SolrException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

/*
* integration tests:
* ==================
* tests the plugin from end to end taking in account all the involved components and test it against solr core
* */

public class GroupLevelQParserPluginTest extends SolrTestCaseJ4 {

    private static AtomicInteger idCounter = new AtomicInteger();


    @BeforeClass
    public static void beforeClass() throws Exception {
        initCore("solrconfig.xml", "schema-string-doc-vals.xml");

        deleteByQueryAndGetVersion("*:*", params());
        idCounter.set(0);

        indexSampleData();
    }

    @Before
    public void before() {
    }

    @Test
    public void groupLevelQParserPluginPreFilterTest() {

        // TODO - seems  to be a design problem - do we really want to let the client control the group level?
        //  like that one could bypass the client and fetch data out of his group level
        assertQ(req("q", "*:*"),
                "//*[@numFound='10']");

        assertQ(req("q", "*:*", "fq", "{!acl cache=false cost=-10 f=groups delimiter=','}a"),
                "//*[@numFound='10']");

        GroupLevelUtils.MAX_PRE_FILTER_GROUP_BOUND = 4;
        assertQ("cost should be fixed if out of the filter type range - 1000 to 99",
                req("q", "*:*", "fq", "{!acl cache=false cost=1000 f=groups delimiter=','}a,b,c"),
                "//*[@numFound='10']");
    }


    @Test
    public void groupLevelQParserPluginPreFilterNegativeTest() {

        assertQ("user's groups levels does not fit to the existing documents group levels",
                req("q", "*:*", "fq", "{!acl cache=false cost=65 f=groups delimiter=','}d"),
                "//*[@numFound='0']");
    }

    @Test
    public void groupLevelQParserPluginPostFilterTest() {

        GroupLevelUtils.MAX_PRE_FILTER_GROUP_BOUND = 1;

        assertQ("",
                req("q", "*:*", "fq", "{!acl cache=false cost=1000 f=groups delimiter=','}a"),
                "//*[@numFound='10']");

        assertQ("",
                req("q", "*:*", "fq", "{!acl cache=false cost=1000 f=groups delimiter=','}a,b,c"),
                "//*[@numFound='10']");

        assertQ("user's groups levels does not fit to the existing documents group levels",
                req("q", "*:*", "fq", "{!acl cache=false cost=1000 f=groups delimiter=','}d"),
                "//*[@numFound='0']");

        assertQ("cost should be fixed if out of the filter type range - 30 to 100",
                req("q", "*:*", "fq", "{!acl cache=false cost=30 f=groups delimiter=','}a,b,c"),
                "//*[@numFound='10']");
    }

    @Test
    public void groupLevelQParserPluginExceptionTest() {

        assertQEx("should fail because no groups were supplied",
                "groups must be supplied",
                req("q", "*:*", "fq", "{!acl cache=false cost=1000 f=groups delimiter=','}"),
                SolrException.ErrorCode.BAD_REQUEST);

    }

    private static void indexSampleData() throws Exception {
        String[] mockGroups = new String[]{"a", "b", "c"};

        for (int i = 0; i < 10; i++) {
            addAndGetVersion(sdoc("id", idCounter.incrementAndGet(), "groups", mockGroups),
                    params("wt", "json"));
        }
        assertU(commit());
        //JQ(req("q", "*:*"))
    }
}