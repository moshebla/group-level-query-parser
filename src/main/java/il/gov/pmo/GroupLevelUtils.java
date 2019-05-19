package il.gov.pmo;

import org.apache.solr.common.SolrException;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

class GroupLevelUtils {
    static int MAX_PRE_FILTER_GROUPS = 80;
    static int PREFILTER_UPPER_BOUND = 99;
    static int POSTFILTER_UPPER_BOUND = 100;

    static int tryParseParamInt(Object integer, String paramName) {
        try {
            return Integer.parseInt(integer.toString());
        } catch (NumberFormatException e) {
            throw new SolrException(SolrException.ErrorCode.SERVER_ERROR,
                    "could not parse specified " + paramName + ": " + integer.toString()
            );
        }
    }

    static Set<String> objectListToObjectSet(String[] objectList) {
        return Arrays.stream(objectList)
                .collect(Collectors.toSet());
    }
}
