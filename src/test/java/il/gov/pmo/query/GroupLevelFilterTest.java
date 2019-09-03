package il.gov.pmo.query;

import il.gov.pmo.GroupLevelUtils;
import il.gov.pmo.query.GroupLevelFilter;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.util.BytesRef;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.SolrException;
import org.apache.solr.search.DelegatingCollector;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import il.gov.pmo.query.GroupLevelFilter.GroupLevelFilterCollector;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/*
 * unit tests:
 * ===========
 * tests logic in GroupLevelFilter
 * */

public class GroupLevelFilterTest extends SolrTestCaseJ4 {

    private static final String fName = "testField";
    private static String groupsStr = "1,2";
    private static String[] groupsList = groupsStr.split(GroupLevelUtils.GROUPS_DELIMITER);
    private static IndexSearcher mockIndexSearcher;
    private static SortedSetDocValues mockFieldValues;
    private static Map<Integer, Boolean> mockDocs = new HashMap<>();
//    private static Answer<Boolean> mockAdvanceExactAnswer;
//    private static Answer<BytesRef> mockLookupOrdAnswer;

    @BeforeClass
    public static void beforeClass() {

        mockDocs.put(1, true);

        mockIndexSearcher = Mockito.mock(IndexSearcher.class);
        mockFieldValues = Mockito.mock(SortedSetDocValues.class);
    }

    private Boolean mockAdvanceExactAnswer(InvocationOnMock invocationOnMock) {
        int docId = (int) invocationOnMock.getArguments()[0];
        return mockDocs.containsKey(docId);
    }

    @Before
    public void setup() throws IOException {

        Mockito.doAnswer(this::mockAdvanceExactAnswer).when(mockFieldValues).advanceExact(Mockito.anyInt());

        Mockito.doAnswer((x) -> SortedSetDocValues.NO_MORE_ORDS).when(mockFieldValues).nextOrd();
    }
//
//    private Answer<Boolean> mockAdvanceExactAnswer(InvocationOnMock invocationOnMock) {
//        int docId = (int) invocationOnMock.getArguments()[0];
//        return invocation -> mockDocs.containsKey(docId);
//    }

    private Answer mockLookupOrdAnswer(Boolean isExistsInGroup) {
        return invocation -> new BytesRef(isExistsInGroup ? "1" : Long.toString(SortedSetDocValues.NO_MORE_ORDS));
    }

    @Test
    public void GroupLevelEqualityTest() {

        GroupLevelFilter filterA = new GroupLevelFilter(GroupLevelUtils.objectListToObjectSet(groupsList), fName, GroupLevelUtils.GROUPS_DELIMITER.charAt(0));
        GroupLevelFilter filterB = new GroupLevelFilter(GroupLevelUtils.objectListToObjectSet(groupsList), fName, GroupLevelUtils.GROUPS_DELIMITER.charAt(0));
        assertEquals(filterA, filterB);

        GroupLevelFilter otherFilter = new GroupLevelFilter(GroupLevelUtils.objectListToObjectSet(groupsList), "otherFieldName", GroupLevelUtils.GROUPS_DELIMITER.charAt(0));
        assertNotEquals(filterA, otherFilter);
    }

    @Test
    public void collectDocTest() throws IOException {

        Mockito.doAnswer(mockLookupOrdAnswer(true)).when(mockFieldValues).lookupOrd(Mockito.anyLong());

        GroupLevelFilter filter = new GroupLevelFilter(GroupLevelUtils.objectListToObjectSet(groupsList), fName, GroupLevelUtils.GROUPS_DELIMITER.charAt(0));
        GroupLevelFilterCollector groupLevelFilterCollectorSpy = Mockito.spy((GroupLevelFilterCollector) filter.getFilterCollector(mockIndexSearcher));

        groupLevelFilterCollectorSpy.setFieldValues(mockFieldValues);

        LeafCollector mockLeafDelegate = Mockito.mock(LeafCollector.class);

        Whitebox.setInternalState(groupLevelFilterCollectorSpy, "leafDelegate", mockLeafDelegate);
        groupLevelFilterCollectorSpy.collect(1);

        Mockito.verify(groupLevelFilterCollectorSpy, Mockito.times(1)).collect(Mockito.anyInt());
        Mockito.verify((DelegatingCollector)groupLevelFilterCollectorSpy, Mockito.times(1)).collect(Mockito.anyInt());
    }

    @Ignore
    @Test
    public void collectDocNotAllowedGroupTest() throws IOException {

        Mockito.doAnswer(mockLookupOrdAnswer(false)).when(mockFieldValues).lookupOrd(Mockito.anyLong());

        GroupLevelFilter filter = new GroupLevelFilter(GroupLevelUtils.objectListToObjectSet(groupsList), fName, GroupLevelUtils.GROUPS_DELIMITER.charAt(0));
        GroupLevelFilterCollector groupLevelFilterCollectorSpy = Mockito.spy((GroupLevelFilterCollector) filter.getFilterCollector(mockIndexSearcher));

        groupLevelFilterCollectorSpy.setFieldValues(mockFieldValues);

        groupLevelFilterCollectorSpy.collect(1);

        Mockito.verify(groupLevelFilterCollectorSpy, Mockito.times(1)).collect(Mockito.anyInt());
        // TODO - again GroupLevelFilter.collect is is being called on verify instead of superclass DelegatingCollector - need a better way to test if group was not matched
        Mockito.verify((DelegatingCollector)groupLevelFilterCollectorSpy, Mockito.never()).collect(Mockito.anyInt());
    }

    @Test
    public void collectDocExceptionTest() {

        GroupLevelFilter filter = new GroupLevelFilter(GroupLevelUtils.objectListToObjectSet(groupsList), fName, GroupLevelUtils.GROUPS_DELIMITER.charAt(0));
        GroupLevelFilterCollector groupLevelFilterCollector = (GroupLevelFilterCollector) filter.getFilterCollector(mockIndexSearcher);
        groupLevelFilterCollector.setFieldValues(mockFieldValues);

        int doNotExistDocId = 9;
        SolrException e = expectThrows(SolrException.class, () -> groupLevelFilterCollector.collect(doNotExistDocId));
        final String expectedMessage = "No value was indexed for docID " + doNotExistDocId + " under field " + fName;

        assertEquals(SolrException.ErrorCode.SERVER_ERROR.code, e.code());
        assertTrue(e.getMessage().contains(expectedMessage));

    }
}
