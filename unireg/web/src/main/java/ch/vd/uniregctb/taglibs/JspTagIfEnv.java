package ch.vd.uniregctb.taglibs;

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;

import ch.vd.uniregctb.utils.UniregModeHelper;

/**
 * Ce tag permet d'inclure conditionnellement un fragment de code JSP en fonction de l'environnement de l'application
 */
public class JspTagIfEnv extends TagSupport {

	private static final long serialVersionUID = -5832005163589177589L;

	private boolean devel;
	private boolean hudson;
	private boolean integration;
	private boolean integrationPo;
	private boolean standalone;
	private boolean validation;
	private boolean preProduction;
	private boolean production;
	private boolean postProduction;

	@Override
	public int doStartTag() throws JspTagException {
		final String environnement = UniregModeHelper.getEnvironnement();
		if (("Developpement".equals(environnement) && devel)
				|| ("Hudson".equals(environnement) && hudson)
				|| ("Integration".equals(environnement) && integration)
				|| ("Integration-Post-Production".equals(environnement) && integrationPo)
				|| ("Standalone".equals(environnement) && standalone)
				|| ("Validation".equals(environnement) && validation)
				|| ("Pre-Production".equals(environnement) && preProduction)
				|| ("Production".equals(environnement) && production)
				|| ("Post-Production".equals(environnement) && postProduction)) {
			return Tag.EVAL_BODY_INCLUDE;
		}

		return Tag.SKIP_BODY;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDevel(boolean devel) {
		this.devel = devel;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setHudson(boolean hudson) {
		this.hudson = hudson;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setIntegration(boolean integration) {
		this.integration = integration;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setIntegrationPo(boolean integrationPo) {
		this.integrationPo = integrationPo;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setStandalone(boolean standalone) {
		this.standalone = standalone;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setValidation(boolean validation) {
		this.validation = validation;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setPreProduction(boolean preProduction) {
		this.preProduction = preProduction;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setProduction(boolean production) {
		this.production = production;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setPostProduction(boolean postProduction) {
		this.postProduction = postProduction;
	}
}
