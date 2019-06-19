package il.gov.pmo.transform;

import il.gov.pmo.GroupLevelLookUp;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.ReaderUtil;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.response.transform.DocTransformer;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.search.SolrIndexSearcher;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class GroupLevelTransformer extends DocTransformer implements GroupLevelLookUp {

    private final String fieldName;
    private final String uniqFieldName;
    private final Set<String> allowedGroups;

    private SortedSetDocValues fieldValues;

    public GroupLevelTransformer(String fieldName, String uniqFieldName, Set<String> allowedGroups) {
        this.fieldName = fieldName;
        this.uniqFieldName = uniqFieldName;
        this.allowedGroups = allowedGroups;
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public void transform(SolrDocument doc, int docid) throws IOException {
        final SolrIndexSearcher searcher = context.getSearcher();
        final List<LeafReaderContext> leaves = searcher.getIndexReader().leaves();
        final int seg = ReaderUtil.subIndex(docid, leaves);
        final LeafReaderContext leafReaderContext = leaves.get(seg);
        fieldValues = DocValues.getSortedSet(leafReaderContext.reader(), getFieldName());
        SolrDocument newDoc = new SolrDocument();
        newDoc.setField(uniqFieldName, doc.getFieldValue(uniqFieldName));
        newDoc.setField("isAllowed", isAllowed(docid));
        doc = newDoc;
    }

    @Override
    public SortedSetDocValues getFieldValues() {
        return fieldValues;
    }

    @Override
    public Set<String> getAllowedGroups() {
        return allowedGroups;
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }
}
