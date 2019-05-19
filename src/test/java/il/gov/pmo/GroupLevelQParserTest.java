package il.gov.pmo;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.junit.Test;
import org.apache.solr.common.params.SolrParams;


public class GroupLevelQParserTest extends SolrTestCaseJ4 {
    @Test
    public void testCalcCost() throws Exception {
        int cost = 1000;
        int expectedCost = 99;

        ModifiableSolrParams params = params("type","acl","cache","false","cost",String.valueOf(cost),"f","groups","delimiter",";","v","a,b,c");
        GroupLevelQParser groupLevelQParser = new GroupLevelQParser("", SolrParams.wrapDefaults(params, null), null, null);
       int actualCost = groupLevelQParser.calcCost(new String[]{"1", "2"});
        assertEquals(expectedCost, actualCost);
    }
}
