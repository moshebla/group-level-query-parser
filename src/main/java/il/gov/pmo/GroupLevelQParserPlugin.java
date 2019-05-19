package il.gov.pmo;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import static il.gov.pmo.GroupLevelUtils.tryParseParamInt;

public class GroupLevelQParserPlugin extends QParserPlugin {

    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

//    private static int MAX_PRE_FILTER_GROUPS = 80;
    private static final String PRE_FILTER_GROUP_BOUND = "preFilterGroupBound";
    private static final String DELIMITER_PARAM = "delimiter";

//    public static int getMaxPreFilterGroups() {
//        return MAX_PRE_FILTER_GROUPS;
//    }
//
//    static void setMaxPreFilterGroups(int maxPreFilterGroups) {
//        MAX_PRE_FILTER_GROUPS = maxPreFilterGroups;
//    }

    @Override
    public void init(NamedList args) {
        Object bound = args.get(PRE_FILTER_GROUP_BOUND);

        if (bound == null) {
            log.warn(PRE_FILTER_GROUP_BOUND + " was not supplied, using default value");
            return;
        }

        GroupLevelUtils.MAX_PRE_FILTER_GROUPS = tryParseParamInt(bound, PRE_FILTER_GROUP_BOUND);
    }

    @Override
    public QParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
        return new GroupLevelQParser(qstr, localParams, params, req, DELIMITER_PARAM);
    }
}
