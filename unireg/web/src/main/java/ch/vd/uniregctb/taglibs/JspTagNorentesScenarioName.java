package ch.vd.uniregctb.taglibs;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

import ch.vd.uniregctb.norentes.common.NorentesFactory;
import ch.vd.uniregctb.norentes.common.NorentesManager;
import ch.vd.uniregctb.norentes.common.NorentesScenario;

/**
 * Tag jsp qui retourne le nom du scénario Norentes courant, ou chaîne vide s'il n'y a pas de scénario ou si l'application n'est pas en mode
 * Norentes.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class JspTagNorentesScenarioName extends BodyTagSupport {

	private static final long serialVersionUID = 6328360306856497698L;

	// private final Logger LOGGER = Logger.getLogger(JspTagInfra.class);

	@Override
	public int doStartTag() throws JspException {

		String name = "";

		final NorentesManager norentesManager = NorentesFactory.getNorentesManager();
		if (norentesManager.isActif()) {
			NorentesScenario scenario = norentesManager.getCurrentScenario();
			if (scenario != null) {
				name = scenario.getDescription();
			}
		}

		try {
			pageContext.getOut().print(name);
		}
		catch (IOException e) {
			throw new JspException(e);
		}

		return SKIP_BODY;
	}
}
