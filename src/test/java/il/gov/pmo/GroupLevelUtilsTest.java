package il.gov.pmo;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.SolrException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

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

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void tryParseParamIntExceptionTest() {

        String testInt = "this is not an int";
        String paramName = "testParam";
        exceptionRule.expect(SolrException.class);
        exceptionRule.expectMessage("could not parse specified " + paramName + ": " + testInt);

        GroupLevelUtils.tryParseParamInt(testInt, paramName);
    }

    @Test
    public void objectListToObjectSetTest() {

        String[] testList = new String[]{UUID.randomUUID().toString()};
        Set testSet = GroupLevelUtils.objectListToObjectSet(testList);
        assertEquals(testList.length, testSet.size());
        assertEquals(testList[0], testSet.iterator().next());
    }
}
