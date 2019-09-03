package il.gov.pmo.query;

import il.gov.pmo.GroupLevelUtils;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.SolrException;
import org.junit.Test;

import java.util.Set;
import java.util.UUID;

/*
 * unit tests:
 * ===========
 * tests logic in GroupLevelUtils
 * */

public class GroupLevelUtilsTest extends SolrTestCaseJ4 {

    @Test
    public void tryParseParamIntTest() {

        int testInt = 1;
        String testIntString = Integer.toString(testInt);

        int parsedInt = GroupLevelUtils.tryParseParamInt(testIntString, "test");

        assertEquals(testInt, parsedInt);
    }

    @Test
    public void tryParseParamIntExceptionTest() {

        String testInt = "this is not an int";
        String paramName = "testParam";
        SolrException e = expectThrows(SolrException.class, () -> GroupLevelUtils.tryParseParamInt(testInt, paramName));
        final String expectedMessage = "could not parse specified " + paramName + ": " + testInt;
        assertEquals(SolrException.ErrorCode.BAD_REQUEST.code, e.code());
        assertTrue(e.getMessage().contains(expectedMessage));
    }

    @Test
    public void objectListToObjectSetTest() {

        String[] testList = new String[]{UUID.randomUUID().toString()};
        Set testSet = GroupLevelUtils.objectListToObjectSet(testList);
        assertEquals(testList.length, testSet.size());
        assertEquals(testList[0], testSet.iterator().next());
    }
}
