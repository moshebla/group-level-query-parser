package il.gov.pmo;

import org.apache.lucene.search.Query;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.junit.Test;
import org.apache.solr.common.params.SolrParams;


public class GroupLevelQParserTest extends SolrTestCaseJ4 {

    static private int cost = 1000;
    static private String groupsStr = "1,2";
    static private String[] groupsList = groupsStr.split(GroupLevelUtils.GROUPS_DELIMITER);
    static private ModifiableSolrParams params = params("type", "acl", "cache", "false", "cost", String.valueOf(cost), "f", "groups", "delimiter", ",", "v", "a,b,c");
    static private GroupLevelQParser groupLevelQParser = new GroupLevelQParser(groupsStr, SolrParams.wrapDefaults(params, null), null, null);

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
}
