package il.gov.pmo;

import org.apache.solr.SolrTestCaseJ4;
import org.junit.Test;

/*
 * unit tests:
 * ===========
 * tests logic in GroupLevelFilter
 * */

public class GroupLevelFilterTest extends SolrTestCaseJ4 {

    private static final String fName = "testField";

    @Test
    public void GroupLevelEqualityTest() {
        String groupsStr = "1,2";
        String[] groupsList = groupsStr.split(GroupLevelUtils.GROUPS_DELIMITER);
        GroupLevelFilter filterA = new GroupLevelFilter(GroupLevelUtils.objectListToObjectSet(groupsList), fName, GroupLevelUtils.GROUPS_DELIMITER.charAt(0));
        GroupLevelFilter filterB = new GroupLevelFilter(GroupLevelUtils.objectListToObjectSet(groupsList), fName, GroupLevelUtils.GROUPS_DELIMITER.charAt(0));
        assertEquals(filterA, filterB);

        GroupLevelFilter otherFilter = new GroupLevelFilter(GroupLevelUtils.objectListToObjectSet(groupsList), "otherFieldName", GroupLevelUtils.GROUPS_DELIMITER.charAt(0));
        assertNotEquals(filterA, otherFilter);
    }
}
