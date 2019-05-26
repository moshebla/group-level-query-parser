package il.gov.pmo;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermInSetQuery;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.query.FilterQuery;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.schema.FieldType;
import org.apache.solr.search.ExtendedQueryBase;
import org.apache.solr.search.QParser;
import org.apache.solr.search.SyntaxError;

import java.util.ArrayList;
import java.util.List;

public class GroupLevelQParser extends QParser {

    private static final String DELIMITER_PARAM = "delimiter";

    /**
     * Constructor for the QParser
     *
     * @param qstr        The part of the query string specific to this parser
     * @param localParams The set of parameters that are specific to this QParser.  See http://wiki.apache.org/solr/LocalParams
     * @param params      The rest of the {@link SolrParams}
     * @param req         The original {@link SolrQueryRequest}.
     */
    public GroupLevelQParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
        super(qstr, localParams, params, req);
    }

    @Override
    public Query parse() throws SyntaxError {
        String fName = localParams.get(CommonParams.FIELD);
        if (fName == null) {
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
                    CommonParams.FIELD + " param must be supplied");
        }

        String delimiter = localParams.get(DELIMITER_PARAM, GroupLevelUtils.GROUPS_DELIMITER);

        if (delimiter.length() > 1) {
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
                    DELIMITER_PARAM + " param must be a char");
        }

        if (qstr.length() == 0) {
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
                    "groups must be supplied");
        }

        String[] splitGroups = qstr.split(delimiter);
        int cost = calcCost(splitGroups);

        // pre-filter or post-filter
        ExtendedQueryBase q = cost >= GroupLevelUtils.POST_FILTER_COST_LOWER_BOUND ? new GroupLevelFilter(GroupLevelUtils.objectListToObjectSet(splitGroups),
                fName, delimiter.charAt(0)) : createPreFilter(fName, splitGroups);

        setCost(q, cost);
        return q;
    }

    void setCost(ExtendedQueryBase q, int cost) {
        localParams = ModifiableSolrParams.of(localParams).set(CommonParams.COST, cost);
        q.setCost(cost);
    }

    int calcCost(String[] splitGroups) {
        Object costParam = localParams.get(CommonParams.COST);

        int cost = 0;
        if (costParam != null) {
            int parsedCost = GroupLevelUtils.tryParseParamInt(costParam, CommonParams.COST);
            if (parsedCost > 0)
                cost = parsedCost;
        }
        // ensure user does not set a large or query as a pre filter
        return splitGroups.length >= GroupLevelUtils.MAX_PRE_FILTER_GROUP_BOUND ? Math.max(cost, GroupLevelUtils.POST_FILTER_COST_LOWER_BOUND) : Math.min(cost, GroupLevelUtils.PRE_FILTER_COST_UPPER_BOUND);
    }

    ExtendedQueryBase createPreFilter(String fName, String[] splitGroups) {
        FieldType ft = req.getSchema().getFieldType(fName);
        List<BytesRef> bytesRefs = new ArrayList<>(splitGroups.length);
        BytesRefBuilder term = new BytesRefBuilder();

        for (String group : splitGroups) {
            // same logic as TermQParserPlugin - take into account the type
            if (ft != null) {
                ft.readableToIndexed(group, term);
            } else {
                term.copyChars(group);
            }
            bytesRefs.add(term.toBytesRef());
        }

        return new FilterQuery(new TermInSetQuery(fName, bytesRefs));
    }
}
