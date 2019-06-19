package il.gov.pmo;

import org.apache.lucene.search.Query;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.query.FilterQuery;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.search.ExtendedQueryBase;
import org.apache.solr.search.QParser;
import org.junit.Before;
import org.junit.Test;
import org.apache.solr.common.params.SolrParams;
import org.mockito.Mockito;

/*
 * unit tests:
 * ===========
 * tests logic in GroupLevelQParser
 * */

public class GroupLevelQParserTest extends SolrTestCaseJ4 {

    private static final int DEFAULT_COST = 1000;
    private static String qstr = "a,b,c";
    private static String[] groupsList = qstr.split(GroupLevelUtils.GROUPS_DELIMITER);
    private static final String fName = "groups";
    private static ModifiableSolrParams params = params("type", "acl", "cache", "false", "cost", Integer.toString(DEFAULT_COST), "f", fName, "delimiter", ",", "v", "a,b,c");
    private static GroupLevelQParser groupLevelQParser = new GroupLevelQParser(qstr, SolrParams.wrapDefaults(params, null), null, null);
    private static SolrQueryRequest req;

    @Before
    public void setup() {

        GroupLevelUtils.MAX_PRE_FILTER_GROUP_BOUND = 3;

        // mockito setup
        IndexSchema mockSchema = Mockito.mock(IndexSchema.class);
        FieldType mockType = Mockito.mock(FieldType.class);
        SolrQueryRequest reqMock = Mockito.mock(LocalSolrQueryRequest.class);
        Mockito.doAnswer((x) -> x.getArgumentAt(0, String.class).equals(fName) ? null : mockType)
                .when(mockSchema).getFieldType(Mockito.anyString());
        Mockito.doReturn(mockSchema).when(reqMock).getSchema();
        req = reqMock;
    }

    @Test
    public void QParserFilterTypeDecisionTest() throws Exception {
        int cost = 1000;
        QParser qParser = getQParser(cost, GroupLevelUtils.GROUPS_DELIMITER);
        Query parsedQuery = qParser.parse();

        assertTrue("group length gte GroupLevelUtils.MAX_PRE_FILTER_GROUP_BOUND should produce Post-Filter"
                , parsedQuery instanceof GroupLevelFilter);
        assertEquals(cost, GroupLevelUtils.tryParseParamInt(qParser.getParam("cost"),"cost"));

        GroupLevelUtils.MAX_PRE_FILTER_GROUP_BOUND = 4;
        parsedQuery = qParser.parse();

        assertTrue("group length lt GroupLevelUtils.MAX_PRE_FILTER_GROUP_BOUND should produce Pre-Filter"
                , parsedQuery instanceof ExtendedQueryBase);
        assertEquals(GroupLevelUtils.PRE_FILTER_COST_UPPER_BOUND, GroupLevelUtils.tryParseParamInt(qParser.getParam("cost"), "cost"));
    }

    @Test
    public void QParserCostFixByFilterTypeTest() throws Exception {
        int cost = DEFAULT_COST;
        QParser qParser = getQParser(90);
        Query parsedQuery = qParser.parse();
        assertEquals("lowest cost value is " + GroupLevelUtils.POST_FILTER_COST_LOWER_BOUND + " when running Post-Filter",
                GroupLevelUtils.POST_FILTER_COST_LOWER_BOUND, ((GroupLevelFilter) parsedQuery).getCost());

        GroupLevelUtils.MAX_PRE_FILTER_GROUP_BOUND = 4;
        cost = 90;
        qParser = getQParser(cost);
        parsedQuery = qParser.parse();
        assertTrue("group length lt GroupLevelUtils.MAX_PRE_FILTER_GROUP_BOUND should produce Pre-Filter"
                , parsedQuery instanceof ExtendedQueryBase);
        assertEquals("cost should be between 0 and " + GroupLevelUtils.PRE_FILTER_COST_UPPER_BOUND + " when running Pre-Filter",
                cost, ((ExtendedQueryBase) parsedQuery).getCost());

        qParser = getQParser(1000);
        parsedQuery = qParser.parse();
        assertEquals("cost should be between 0 and " + GroupLevelUtils.PRE_FILTER_COST_UPPER_BOUND + " when running Pre-Filter",
                GroupLevelUtils.PRE_FILTER_COST_UPPER_BOUND, ((ExtendedQueryBase) parsedQuery).getCost());

        GroupLevelUtils.MAX_PRE_FILTER_GROUP_BOUND = 4;
        qParser = getQParser(-1);
        parsedQuery = qParser.parse();
        assertEquals("cost should be between 0 and " + GroupLevelUtils.PRE_FILTER_COST_UPPER_BOUND + " when running Pre-Filter",
                0, ((ExtendedQueryBase) parsedQuery).getCost());

        GroupLevelUtils.MAX_PRE_FILTER_GROUP_BOUND = 2;
        parsedQuery = qParser.parse();
        assertEquals("cost should be gte " + GroupLevelUtils.POST_FILTER_COST_LOWER_BOUND + " when running Post-Filter",
                GroupLevelUtils.POST_FILTER_COST_LOWER_BOUND, ((ExtendedQueryBase) parsedQuery).getCost());
    }

    @Test
    public void CalcCostTest() {
        int cost = DEFAULT_COST;
        int expectedCost = GroupLevelUtils.PRE_FILTER_COST_UPPER_BOUND;
        GroupLevelUtils.MAX_PRE_FILTER_GROUP_BOUND = 4;
        int actualCost = groupLevelQParser.calcCost(groupsList);
        assertEquals(expectedCost, actualCost);

        expectedCost = cost;
        GroupLevelUtils.MAX_PRE_FILTER_GROUP_BOUND = 2;
        actualCost = groupLevelQParser.calcCost(groupsList);
        assertEquals(expectedCost, actualCost);
    }

    @Test
    public void SetCostTest() throws Exception {
        int cost = DEFAULT_COST;
        QParser qParser = getQParser(cost);
        ExtendedQueryBase extendedQueryBase = (ExtendedQueryBase) qParser.getQuery();

        // should update both Query and QueryParser localParams
        groupLevelQParser.setCost(extendedQueryBase, cost);

        assertEquals("set cost should set the Query's cost filed",
                extendedQueryBase.getCost(),
                cost);
        assertEquals("set cost should reset the QueryParser's localParams",
                params("cost", Integer.toString(cost), "f", fName, "delimiter", GroupLevelUtils.GROUPS_DELIMITER).toString(),
                qParser.getLocalParams().toString());
    }

    @Test
    public void CreatePreFilterTest() {
        int cost = DEFAULT_COST;
        GroupLevelQParser qParser = (GroupLevelQParser) getQParser(cost);
        String[] splitGroups = qstr.split(GroupLevelUtils.GROUPS_DELIMITER);

        ExtendedQueryBase extendedQueryBase = qParser.createPreFilter(fName, splitGroups);

        assertTrue(extendedQueryBase instanceof FilterQuery);
        assertEquals(extendedQueryBase.toString(), "filter(groups:(" + String.join(" ", splitGroups) + "))");
    }

    // helpers

    private QParser getQParser(int cost) {
        return getQParser(cost, ",");
    }

    private QParser getQParser(int cost, String delimiter) {
        SolrParams localParams = params("cost", Integer.toString(cost), "f", fName, "delimiter", GroupLevelUtils.GROUPS_DELIMITER);
        req.setParams(params("q", "*:*", "fq", "{!acl " + solrParamsToQParserString(localParams) + " }" + qstr));
        return new GroupLevelQParser(qstr, localParams, req.getParams(), req);
    }

    private static String solrParamsToQParserString(SolrParams params) {
        StringBuilder stringBuilder = new StringBuilder();

        params.forEach(x -> stringBuilder
                .append(x.getKey())
                .append("=")
                .append(String.join(",", x.getValue()))
                .append(" ")
        );
        return stringBuilder.toString();
    }
}
