package il.gov.pmo;

import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermInSetQuery;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.BytesRef;
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
    private final List<BytesRef> terms;


    /**
     *
     * @param cost
     * @param groups
     * @param fieldName
     * @param delimiter
     * @param terms required when run as pre-filter, otherwise should be null
     */
    public GroupLevelFilter(int cost, Set<String> groups, String fieldName, char delimiter, List<BytesRef> terms) {
        setCost(groups.size() >= GroupLevelQParserPlugin.getMaxPreFilterGroups()? Math.max(cost, 100): Math.min(cost, 99));
        setCache(false);
        this.allowedGroups = groups;
        this.fName = fieldName;
        this.delimiter = delimiter;
        this.terms = terms;
    }

    public GroupLevelFilter(int cost, Set<String> groups, String fieldName, char delimiter) {
        this(cost, groups, fieldName, delimiter, null);
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

    @Override
    public Weight createWeight(IndexSearcher searcher, boolean needsScores, float boost) throws IOException {
        return new TermInSetQuery(fName, terms).createWeight(searcher, false, 1.0f);
    }

    private class GroupLevelFilterCollector extends DelegatingCollector {
        BinaryDocValues fieldVals;

        @Override
        public void collect(int doc) throws IOException {
            boolean hasValue = fieldVals.advanceExact(doc);

            if(!hasValue) {
                throw  new SolrException(SolrException.ErrorCode.SERVER_ERROR,
                        "No value was indexed for docID " + doc + " under field "+ fName);
            }

            final List<String> docGroups = StrUtils.splitSmart(fieldVals.binaryValue().utf8ToString(), delimiter);
            if(isAllowed(docGroups)) {
                super.collect(doc);
            }
        }

        @Override
        protected void doSetNextReader(LeafReaderContext context) throws IOException {
            fieldVals = DocValues.getSorted(context.reader(), fName);
            super.doSetNextReader(context);
        }

        private boolean isAllowed(List<String> docGroups) {
            return docGroups.stream().anyMatch(allowedGroups::contains);
        }
    }
}
