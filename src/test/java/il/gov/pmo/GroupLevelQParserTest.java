package il.gov.pmo;

import org.apache.lucene.search.Query;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.StrUtils;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.search.QParser;
import org.junit.Before;
import org.junit.Test;
import org.apache.solr.common.params.SolrParams;
import org.mockito.Mockito;

import java.util.StringJoiner;

public class GroupLevelQParserTest extends SolrTestCaseJ4 {

    private static int cost = 1000;
    private static String groupsStr = "1,2";
    private static String[] groupsList = groupsStr.split(GroupLevelUtils.GROUPS_DELIMITER);
    private static final String fName = "groups";
    private static ModifiableSolrParams params = params("type", "acl", "cache", "false", "cost", String.valueOf(cost), "f", fName, "delimiter", ",", "v", "a,b,c");
    private static GroupLevelQParser groupLevelQParser = new GroupLevelQParser(groupsStr, SolrParams.wrapDefaults(params, null), null, null);

    private static SolrQueryRequest req;

    @Before
    public void setup() throws Exception {
        GroupLevelUtils.MAX_PRE_FILTER_GROUP_BOUND = 2;

        IndexSchema mockSchema = Mockito.mock(IndexSchema.class);
        FieldType mockType = Mockito.mock(FieldType.class);
        SolrQueryRequest reqMock = Mockito.mock(LocalSolrQueryRequest.class);
        Mockito.doAnswer((x) -> x.getArgumentAt(0, String.class).equals(fName) ? mockType : null).when(mockSchema).getFieldType(Mockito.anyString());
        Mockito.doReturn(mockSchema).when(reqMock).getSchema();
        req = reqMock;
    }

    @Test
    public void testQParser() throws Exception {
        GroupLevelUtils.MAX_PRE_FILTER_GROUP_BOUND = 4;
        final String qstr = "a,b,c";
        final String cost = "1000";
        final String delimiter = ",";

        final SolrParams localParams = params("cost", cost, "f", fName, "delimiter", delimiter);
        req.setParams(params("q", "*:*", "fq", "{!acl " + solrParamsToQParserString(localParams) + " }" + qstr));
        QParser qParser = new GroupLevelQParser(qstr, localParams, req.getParams(), req);
        Query parsedQuery = qParser.parse();

        assertTrue("group length gte GroupLevelUtils.MAX_PRE_FILTER_GROUP_BOUND should produce post filter"
                , parsedQuery instanceof GroupLevelFilter);

        // assertEquals(qParser.getParam("cost"), cost);
        // assertEquals(qParser.getParam("delimiter"), delimiter);

    }

    @Test
    public void testCalcCost() throws Exception {

        int expectedCost = GroupLevelUtils.PRE_FILTER_UPPER_BOUND;
        GroupLevelUtils.MAX_PRE_FILTER_GROUP_BOUND = 3;

        int actualCost = groupLevelQParser.calcCost(groupsList);
        assertEquals(expectedCost, actualCost);

        expectedCost = cost;
        GroupLevelUtils.MAX_PRE_FILTER_GROUP_BOUND = 1;

        actualCost = groupLevelQParser.calcCost(groupsList);
        assertEquals(expectedCost, actualCost);
    }

    private static String solrParamsToQParserString(SolrParams params) {
        StringBuilder stringBuilder = new StringBuilder();
        params.forEach(x -> stringBuilder.append(x.getKey()).append("=")
                .append(String.join(",", x.getValue()))
                .append(" ")
        );
        return stringBuilder.toString();
    }
}
