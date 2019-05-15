package il.gov.pmo;

import org.apache.lucene.search.Query;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.schema.FieldType;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;
import org.apache.solr.search.SyntaxError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.stream.Collectors;

public class GroupLevelQParserPlugin extends QParserPlugin {

    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static int MAX_PRE_FILTER_GROUPS = 80;

    public static final String NAME = "group_level_qparser";
    public static final String PRE_FILTER_GROUP_BOUND = "preFilterGroupBound";
    public static final String DELIMITER_PARAM = "delimiter";

    public static int getMaxPreFilterGroups() {
        return MAX_PRE_FILTER_GROUPS;
    }

    static void setMaxPreFilterGroups(int maxPreFilterGroups) {
        MAX_PRE_FILTER_GROUPS = maxPreFilterGroups;
    }

    @Override
    public void init( NamedList args ) {
        Object bound = args.get(PRE_FILTER_GROUP_BOUND);

        if(bound == null) {
            log.warn(PRE_FILTER_GROUP_BOUND + " was not supplied, using default value");
            return;
        }

        MAX_PRE_FILTER_GROUPS = tryParseParamInt(bound, PRE_FILTER_GROUP_BOUND);
    }

    private int tryParseParamInt(Object integer, String paramName) {
        try {
            return Integer.parseInt(integer.toString());
        } catch (NumberFormatException e) {
            throw new SolrException(SolrException.ErrorCode.SERVER_ERROR,
                    "could not parse specified " + paramName + ": " + integer.toString()
            );
        }
    }

    @Override
    public QParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
        return new QParser(qstr, localParams, params, req) {
            @Override
            public Query parse() throws SyntaxError {
                String fName = localParams.get(CommonParams.FIELD);
                if(fName == null) {
                    throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
                            CommonParams.FIELD + " param must be supplied");
                }

                String delimiter = localParams.get(DELIMITER_PARAM, ";");

                if(delimiter.length() > 1) {
                    throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
                            DELIMITER_PARAM + " param must be a char");
                }

                if(qstr.length() == 0) {
                    throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
                            "groups must be supplied");
                }

                FieldType ft = req.getSchema().getFieldType(fName);

                Set<String> allowedGroups = createUserGroupSet(qstr, delimiter);

                Iterator<String> allowedGroupsIter = allowedGroups.iterator();
                List<BytesRef> bytesRefs = new ArrayList<>(allowedGroups.size());
                BytesRefBuilder term = new BytesRefBuilder();

                for (int i = 0; i < allowedGroups.size(); i++) {
                    String stringVal = allowedGroupsIter.next();
                    //logic same as TermQParserPlugin
                    if (ft != null) {
                        ft.readableToIndexed(stringVal, term);
                    } else {
                        term.copyChars(stringVal);
                    }
                    bytesRefs.add(term.toBytesRef());
                }

                Object costParam = localParams.get(CommonParams.COST);
                int cost = costParam!=null? tryParseParamInt(costParam, CommonParams.COST): 0;

                return new GroupLevelFilter(cost, allowedGroups, fName, delimiter.charAt(0), bytesRefs);
            }

            private Set<String> createUserGroupSet(String userGroups, String delimiter) {
                return Arrays.stream(userGroups.split(delimiter))
                        .collect(Collectors.toSet());
            }
        };
    }
}
