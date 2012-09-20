package ch.vd.uniregctb.taglibs;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.apache.commons.lang.StringUtils;

import ch.vd.uniregctb.supergra.EntityKey;
import ch.vd.uniregctb.supergra.EntityType;

/**
 * Tag jsp qui permet de récupérer divers éléments du service infrastructure par id.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class JspTagSuperGraLink extends BodyTagSupport {

	private static final long serialVersionUID = -8958197495549589352L;

	// private final Logger LOGGER = Logger.getLogger(JspTagInfra.class);

	private EntityType type;
	private Long id;
	private String collName;

	@Override
	public int doStartTag() throws JspException {

		final HttpServletRequest request = (HttpServletRequest) this.pageContext.getRequest();
		final String contextPath = request.getContextPath();

		final String body;
		if (StringUtils.isBlank(collName)) {
			body = "<a href=\"" + contextPath + "/supergra/entity.do?id=" + id + "&class=" + type + "\"/>" + type.getDisplayName() + " n°" + id + "</a>";
		}
		else {
			body = "<a href=\"" + contextPath + "/supergra/coll.do?id=" + id + "&class=" + type + "&name=" + collName + "\"/>" + type.getDisplayName() + " n°" + id + "</a>";
		}

		try {
			JspWriter out = pageContext.getOut();
			out.print(body);
			return SKIP_BODY;
		}
		catch (Exception ex) {
			throw new JspTagException(ex);
		}
	}

	public void setType(EntityType type) {
		this.type = type;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setKey(EntityKey key) {
		this.id = key.getId();
		this.type = key.getType();
	}

	public void setCollName(String collName) {
		this.collName = collName;
	}
}