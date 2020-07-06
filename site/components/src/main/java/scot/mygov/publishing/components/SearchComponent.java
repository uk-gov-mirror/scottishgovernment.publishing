package scot.mygov.publishing.components;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryResult;
import org.hippoecm.hst.content.beans.query.builder.Constraint;
import org.hippoecm.hst.content.beans.query.builder.HstQueryBuilder;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.util.SearchInputParsingUtils;
import org.onehippo.cms7.essentials.components.EssentialsListComponent;
import org.onehippo.cms7.essentials.components.info.EssentialsListComponentInfo;
import org.onehippo.cms7.essentials.components.paging.Pageable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.StringUtils.equalsIgnoreCase;
import static org.hippoecm.hst.content.beans.query.builder.ConstraintBuilder.and;
import static org.hippoecm.hst.content.beans.query.builder.ConstraintBuilder.constraint;
import static org.hippoecm.hst.content.beans.query.builder.ConstraintBuilder.or;

@ParametersInfo(type = EssentialsListComponentInfo.class)
public class SearchComponent extends EssentialsListComponent {

    private static String PRIMARY_TYPE = "jcr:primaryType";
    private static Collection<String> FIELD_NAMES = new ArrayList<>();

    private static final Logger LOG = LoggerFactory.getLogger(SearchComponent.class);

    @Override
    public void init(ServletContext servletContext, ComponentConfiguration componentConfig) {
        super.init(servletContext, componentConfig);
        Collections.addAll(FIELD_NAMES,
                "publishing:title",
                "publishing:summary",
                "publishing:content/hippostd:content",
                "publishing:tags"
        );
    }

    @Override
    public void doBeforeRender(final HstRequest request,
                               final HstResponse response) {

        super.doBeforeRender(request, response);

        Map<String, Set<String>> params = sanitiseParameterMap(request,
                request.getRequestContext().getServletRequest().getParameterMap());

        String relativeContentPath = request.getRequestContext().getResolvedSiteMapItem().getRelativeContentPath();
        request.setAttribute("relativeContentPath", relativeContentPath);

        request.setAttribute("parameters", params);
    }

    @Override
    protected <T extends EssentialsListComponentInfo>
    HstQuery buildQuery(final HstRequest request, final T paramInfo, final HippoBean scope) {

        final int pageSize = getPageSize(request, paramInfo);
        final int page = getCurrentPage(request);
        final int offset = (page - 1) * pageSize;

//      Sorts by jcr:score by default.
        HstQueryBuilder builder = HstQueryBuilder.create(request.getRequestContext().getSiteContentBaseBean());
        return builder.where(constraints(request, null))
                .limit(pageSize)
                .offset(offset)
                .build();
    }

    @Override
    protected <T extends EssentialsListComponentInfo>
    Pageable<HippoBean> executeQuery(final HstRequest request, final T paramInfo, final HstQuery query) throws QueryException {
        final int pageSize = getPageSize(request, paramInfo);
        final int page = getCurrentPage(request);

        try {
            LOG.info("query: {}", query.toString());
            final HstQueryResult execute = query.execute();

            return getPageableFactory().createPageable(
                    execute.getHippoBeans(),
                    execute.getTotalSize(),
                    pageSize,
                    page);
        } catch (Exception e) {
            LOG.error("query failed ", e);
            throw e;
        }
    }

    private Map<String, Set<String>> sanitiseParameterMap(HstRequest request, Map<String, String[]> parameterMap) {
        if (parameterMap == null) {
            return null;
        }

        Map<String, Set<String>> sanitisedMap = new HashMap();
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {

            if (equalsIgnoreCase("page", entry.getKey())) {
                continue;
            }

            Set<String> splitParameters = splitParameters(request, entry.getKey());
            sanitisedMap.put(entry.getKey(), splitParameters);
        }
        return sanitisedMap;
    }

    private Constraint constraints(HstRequest request, String searchField) {

        List<Constraint> constraints = new ArrayList<>();
        addTermConstraints(constraints, request);
        return and(constraints.toArray(new Constraint[] {}));
    }

    private void addTermConstraints(List<Constraint> constraints, HstRequest request) {
        String term = param(request, "q");
        String parsedTerm = SearchInputParsingUtils.parse(term, false);

        if (StringUtils.isBlank(term)) {
            return;
        }
        constraints.add(or(fieldConstraints(parsedTerm)));
    }


    private Constraint [] fieldConstraints(String term) {

        List<Constraint> constraints = FIELD_NAMES
                .stream()
                .map(field -> constraint(field).contains(term))
                .collect(toList());
        return constraints.toArray(new Constraint[constraints.size()]);
    }

    private String param(HstRequest request, String param) {
        HstRequestContext requestContext = request.getRequestContext();
        HttpServletRequest servletRequest = requestContext.getServletRequest();
        return servletRequest.getParameter(param);
    }

    private Set<String> splitParameters(HstRequest request, String parameter) {
        String parameters = param(request, parameter);
        if (parameters == null) {
            return Collections.emptySet();
        }
        String [] topicTitleArray = parameters.split("\\;");
        return new HashSet<>(asList(topicTitleArray));
    }
}
