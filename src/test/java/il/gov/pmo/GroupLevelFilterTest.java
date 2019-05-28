package il.gov.pmo;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.util.BytesRef;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.SolrException;
import org.apache.solr.search.DelegatingCollector;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import il.gov.pmo.GroupLevelFilter.GroupLevelFilterCollector;
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
    private static Answer<Boolean> mockAdvanceExactAnswer;
    private static Answer<BytesRef> mockLookupOrdAnswer;

    @BeforeClass
    public static void beforeClass() {
        mockDocs.put(1, true);

        mockAdvanceExactAnswer = invocationOnMock -> {
            int docId = (int) invocationOnMock.getArguments()[0];
            return mockDocs.containsKey(docId);
        };

        mockLookupOrdAnswer = invocationOnMock -> {
            long ord = (long) invocationOnMock.getArguments()[0];
            return new BytesRef(Long.toString(ord));
        };

        mockIndexSearcher = Mockito.mock(IndexSearcher.class);
        mockFieldValues = Mockito.mock(SortedSetDocValues.class);
    }

    @Before
    public void setup() throws IOException {

        Mockito.doAnswer(mockAdvanceExactAnswer).when(mockFieldValues).advanceExact(Mockito.anyInt());

//        TODO - stopped here
//         Mockito.doAnswer((x)->{
//             return SortedSetDocValues.NO_MORE_ORDS;
//         }).when(mockFieldValues).nextOrd();
    }

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
        Mockito.doAnswer(mockLookupOrdAnswer(false)).when(mockFieldValues).lookupOrd(Mockito.anyLong());

        GroupLevelFilter filter = new GroupLevelFilter(GroupLevelUtils.objectListToObjectSet(groupsList), fName, GroupLevelUtils.GROUPS_DELIMITER.charAt(0));
        GroupLevelFilterCollector groupLevelFilterCollectorSpy = Mockito.spy((GroupLevelFilterCollector) filter.getFilterCollector(mockIndexSearcher));

        groupLevelFilterCollectorSpy.setFieldValues(mockFieldValues);

        LeafCollector mockLeafDelegate = Mockito.mock(LeafCollector.class);

        Whitebox.setInternalState(groupLevelFilterCollectorSpy, "leafDelegate", mockLeafDelegate);
        groupLevelFilterCollectorSpy.collect(1);
    }

    @Test
    public void collectDocExceptionTest() {
        GroupLevelFilter filter = new GroupLevelFilter(GroupLevelUtils.objectListToObjectSet(groupsList), fName, GroupLevelUtils.GROUPS_DELIMITER.charAt(0));
        GroupLevelFilterCollector groupLevelFilterCollector = (GroupLevelFilterCollector) filter.getFilterCollector(mockIndexSearcher);
        groupLevelFilterCollector.setFieldValues(mockFieldValues);

        int docId = 9;
        SolrException e = expectThrows(SolrException.class, () -> groupLevelFilterCollector.collect(docId));
        final String expectedMessage = "No value was indexed for docID " + docId + " under field " + fName;

        assertEquals(SolrException.ErrorCode.SERVER_ERROR.code, e.code());
        assertTrue(e.getMessage().contains(expectedMessage));

    }
}
