package il.gov.pmo;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public class GroupLevelQParserPlugin extends QParserPlugin {

    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String MAX_PRE_FILTER_GROUP_BOUND_PARAM = "maxPreFilterGroupBound";

    @Override
    public void init(NamedList args) {
        Object bound = args.get(MAX_PRE_FILTER_GROUP_BOUND_PARAM);

        if (bound == null) {
            log.warn(MAX_PRE_FILTER_GROUP_BOUND_PARAM + " was not supplied, using default value");
            return;
        }

        GroupLevelUtils.MAX_PRE_FILTER_GROUP_BOUND = GroupLevelUtils.tryParseParamInt(bound, MAX_PRE_FILTER_GROUP_BOUND_PARAM);
    }

    @Override
    public QParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
        return new GroupLevelQParser(qstr, localParams, params, req);
    }
}
