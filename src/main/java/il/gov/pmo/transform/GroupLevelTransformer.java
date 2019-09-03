package il.gov.pmo.transform;

import il.gov.pmo.GroupLevelLookUp;
import il.gov.pmo.GroupLevelUtils;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.ReaderUtil;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.response.transform.DocTransformer;
import org.apache.solr.search.SolrIndexSearcher;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class GroupLevelTransformer extends DocTransformer implements GroupLevelLookUp {

    private final String fieldName;
    private final String uniqueFieldName;
    private final Set<String> allowedGroups;

    private SortedSetDocValues fieldValues;

    GroupLevelTransformer(String fieldName, String uniqueFieldName, Set<String> allowedGroups) {
        this.fieldName = fieldName;
        this.uniqueFieldName = uniqueFieldName;
        this.allowedGroups = allowedGroups;
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public void transform(SolrDocument doc, int docid) throws IOException {
        SolrDocument tempDoc = new SolrDocument();
        tempDoc.putAll(doc);

        final SolrIndexSearcher searcher = context.getSearcher();
        final List<LeafReaderContext> leaves = searcher.getIndexReader().leaves();
        final int seg = ReaderUtil.subIndex(docid, leaves);
        final LeafReaderContext leafReaderContext = leaves.get(seg);
        fieldValues = DocValues.getSortedSet(leafReaderContext.reader(), getFieldName());

        boolean isAllowed = isAllowed(docid);
        if (!isAllowed) {
            doc.clear();
            for (String fieldName : tempDoc.getFieldNames()) {
                if(GroupLevelUtils.PUBLIC_FIELDS.contains(fieldName)){
                    doc.setField(fieldName, tempDoc.getFieldValue(fieldName));
                }
            }
        }

        doc.setField("isAllowed", isAllowed);
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
