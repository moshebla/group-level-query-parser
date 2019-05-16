package il.gov.pmo;

import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.IndexSearcher;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.StrUtils;
import org.apache.solr.search.DelegatingCollector;
import org.apache.solr.search.ExtendedQueryBase;
import org.apache.solr.search.PostFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class GroupLevelFilter extends ExtendedQueryBase implements PostFilter {
    
    private final Set<String> allowedGroups;
    private final String fName;
    private final char delimiter;

    public GroupLevelFilter(int cost, Set<String> groups, String fieldName, char delimiter) {
        setCost(groups.size() >= GroupLevelQParserPlugin.getMaxPreFilterGroups()? Math.max(cost, 100): Math.min(cost, 99));
        setCache(false);
        this.allowedGroups = groups;
        this.fName = fieldName;
        this.delimiter = delimiter;
    }

    @Override
    public DelegatingCollector getFilterCollector(IndexSearcher searcher) {
        return new GroupLevelFilterCollector();
    }

    @Override
    public boolean equals(Object other) {
        return sameClassAs(other) &&
                fName.equals(((GroupLevelFilter)other).fName)
                && allowedGroups.equals(((GroupLevelFilter) other).allowedGroups);
    }

    @Override
    public int hashCode() {
        return classHash() ^ ((fName.hashCode()) * allowedGroups.hashCode());
    }

    private class GroupLevelFilterCollector extends DelegatingCollector {
        BinaryDocValues fieldValues;

        @Override
        public void collect(int doc) throws IOException {
            boolean hasValue = fieldValues.advanceExact(doc);

            if(!hasValue) {
                throw  new SolrException(SolrException.ErrorCode.SERVER_ERROR,
                        "No value was indexed for docID " + doc + " under field "+ fName);
            }

            final List<String> docGroups = StrUtils.splitSmart(fieldValues.binaryValue().utf8ToString(), delimiter);
            if(isAllowed(docGroups)) {
                super.collect(doc);
            }
        }

        @Override
        protected void doSetNextReader(LeafReaderContext context) throws IOException {
            fieldValues = DocValues.getSorted(context.reader(), fName);
            super.doSetNextReader(context);
        }

        private boolean isAllowed(List<String> docGroups) {
            return docGroups.stream().anyMatch(allowedGroups::contains);
        }
    }
}
