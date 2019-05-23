package il.gov.pmo;

import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.search.IndexSearcher;
import org.apache.solr.common.SolrException;
import org.apache.solr.search.DelegatingCollector;
import org.apache.solr.search.ExtendedQueryBase;
import org.apache.solr.search.PostFilter;

import java.io.IOException;
import java.util.Set;

public class GroupLevelFilter extends ExtendedQueryBase implements PostFilter {

    private final Set<String> allowedGroups;
    private final String fName;
    private final char delimiter;

    public
    GroupLevelFilter(Set<String> groups, String fieldName, char delimiter) {
        setCache(false);
        this.allowedGroups = groups;
        this.fName = fieldName;
        this.delimiter = delimiter;
    }

    // TODO - can be tested only with solr core
    @Override
    public DelegatingCollector getFilterCollector(IndexSearcher searcher) {
        return new GroupLevelFilterCollector();
    }

    @Override
    public boolean equals(Object other) {
        return sameClassAs(other) &&
                fName.equals(((GroupLevelFilter) other).fName)
                && allowedGroups.equals(((GroupLevelFilter) other).allowedGroups);
    }

    @Override
    public int hashCode() {
        return classHash() ^ ((fName.hashCode()) * allowedGroups.hashCode());
    }

    private class GroupLevelFilterCollector extends DelegatingCollector {
        SortedSetDocValues fieldValues;

        @Override
        public void collect(int doc) throws IOException {
            boolean hasValue = fieldValues.advanceExact(doc);

            if (!hasValue) {
                throw new SolrException(SolrException.ErrorCode.SERVER_ERROR,
                        "No value was indexed for docID " + doc + " under field " + fName);
            }

            long ord = 0;
            while (ord != SortedSetDocValues.NO_MORE_ORDS) {
                String fieldValue = fieldValues.lookupOrd(ord).utf8ToString();
                if (allowedGroups.contains(fieldValue)) {
                    super.collect(doc);
                    break;
                }
                ord = fieldValues.nextOrd();
            }
        }

        @Override
        protected void doSetNextReader(LeafReaderContext context) throws IOException {
            fieldValues = DocValues.getSortedSet(context.reader(), fName);
            super.doSetNextReader(context);
        }
    }
}
