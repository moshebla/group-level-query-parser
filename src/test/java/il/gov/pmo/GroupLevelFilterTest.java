package il.gov.pmo;

import com.google.common.collect.Sets;
import org.apache.solr.SolrTestCaseJ4;
import org.junit.Test;

import java.util.Set;

public class GroupLevelFilterTest extends SolrTestCaseJ4 {

    private static final String fieldName = "testField";
    private static final char delimiter = ',';

    @Test
    public void testPreFilter() throws Exception {
        Set<String> groups = Sets.newHashSet("a", "b");
        GroupLevelFilter filter = new GroupLevelFilter(5, groups, fieldName, delimiter);
        assertEquals(filter.getCost(), 5);

        // ensure low number of groups is still run as a pre-filter
        filter = new GroupLevelFilter(1500, groups, fieldName, delimiter);
        assertEquals(filter.getCost(), 99);
    }

    @Test
    public void testPostFilter() throws Exception {
        GroupLevelQParserPlugin.setMaxPreFilterGroups(5);
        Set<String> groups = Sets.newHashSet("a", "b", "c", "d", "e");
        GroupLevelFilter filter = new GroupLevelFilter(5, groups, fieldName, delimiter);
        assertEquals(filter.getCost(), 100);

        // ensure high number of groups is run as a post-filter
        filter = new GroupLevelFilter(1500, groups, fieldName, delimiter);
        assertEquals(filter.getCost(), 1500);
    }
}
