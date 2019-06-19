package il.gov.pmo;

import org.apache.lucene.index.SortedSetDocValues;
import org.apache.solr.common.SolrException;

import java.io.IOException;
import java.util.Set;

public interface GroupLevelLookUp {
    SortedSetDocValues getFieldValues();

    Set<String> getAllowedGroups();

    String getFieldName();

    default boolean isAllowed(int docid) throws IOException {
        boolean hasValue = getFieldValues().advanceExact(docid);

        if (!hasValue) {
            throw new SolrException(SolrException.ErrorCode.SERVER_ERROR,
                    "No value was indexed for docID " + docid + " under field " + getFieldName());
        }

        long ord = 0;
        while (ord != SortedSetDocValues.NO_MORE_ORDS) {
            String fieldValue = getFieldValues().lookupOrd(ord).utf8ToString();
            if (getAllowedGroups().contains(fieldValue)) {
                return true;
            }
            ord = getFieldValues().nextOrd();
        }
        return false;
    }
}
