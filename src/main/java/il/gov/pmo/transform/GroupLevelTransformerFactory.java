package il.gov.pmo.transform;

import il.gov.pmo.GroupLevelUtils;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.transform.DocTransformer;
import org.apache.solr.response.transform.TransformerFactory;

public class GroupLevelTransformerFactory extends TransformerFactory {

    private final static String allowedGroupsParam = "allowedGroups";
    private final static String delimiterParam = "delimiter";

    @Override
    public DocTransformer create(String field, SolrParams params, SolrQueryRequest req) {
        String fName = params.get(CommonParams.FIELD);

        if (fName == null) {
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
                    CommonParams.FIELD + " param must be supplied");
        }

        String allowedGroups = params.get(allowedGroupsParam);

        if (allowedGroups == null) {
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "parameter " + allowedGroupsParam + " must be supplied");
        }

        String[] splitGroups = allowedGroups.split(params.get(delimiterParam, ","));

        return new GroupLevelTransformer(fName
                , req.getSchema().getUniqueKeyField().getName(),
                GroupLevelUtils.objectListToObjectSet(splitGroups)
        );
    }
}
