package il.gov.pmo;

import org.apache.solr.common.SolrException;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class GroupLevelUtils {
    public static int MAX_PRE_FILTER_GROUP_BOUND = 80;
    public static int PRE_FILTER_COST_UPPER_BOUND = 99;
    public static int POST_FILTER_COST_LOWER_BOUND = 100;
    public static String GROUPS_DELIMITER = ",";

    public static int tryParseParamInt(Object integer, String paramName) {
        try {
            return Integer.parseInt(integer.toString());
        } catch (NumberFormatException e) {
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
                    "could not parse specified " + paramName + ": " + integer.toString()
            );
        }
    }

    public static Set<String> objectListToObjectSet(String[] objectList) {
        return Arrays.stream(objectList)
                .collect(Collectors.toSet());
    }
}
