package ch.vd.uniregctb.taglibs;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.OfficeImpot;
import ch.vd.uniregctb.admin.GestionJob;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamBoolean;
import ch.vd.uniregctb.scheduler.JobParamCommune;
import ch.vd.uniregctb.scheduler.JobParamEnum;
import ch.vd.uniregctb.scheduler.JobParamFile;
import ch.vd.uniregctb.scheduler.JobParamOfficeImpot;
import ch.vd.uniregctb.scheduler.JobParamRegDate;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;

/**
 * Tag jsp permettant d'afficher le nom d'un batch et le formulaire permettant de saisir les paramètres et de démarrer le batch.
 */
public class JspTagBatchForm extends BodyTagSupport {

	private static final int NB_COLS_PARAMS = 2;

	private static final long serialVersionUID = 5881995361738706324L;

	private GestionJob job;

	@Override
	public int doStartTag() throws JspException {
		try {
			final HttpServletRequest request = (HttpServletRequest) this.pageContext.getRequest();
			final JspWriter out = pageContext.getOut();
			out.print(buildHtlm(request, job));
			return SKIP_BODY;
		}
		catch (Exception ex) {
			throw new JspTagException(ex);
		}
	}

	public static String buildHtlm(HttpServletRequest request, GestionJob job) {

		if (job == null) {
			return "";
		}

		final StringBuilder b = new StringBuilder();
		b.append("<form method=\"post\" enctype=\"multipart/form-data\" id=\"").append(job.getName()).append("\" name=\"").append(job.getName()).append("\">\n");
		{
			final String image = request.getContextPath() + "/images/plus.gif";

			// L'entête du formulaire
			b.append("<a href=\"#\" onclick=\"javascript:Batch_toggleExpand(this, '").append(job.getName()).append("'); this.blur(); return false;\">");
			b.append("<img src=\"").append(image).append("\" align=\"top\" id=\"IMG_").append(job.getName()).append("\" />");
			b.append(StringEscapeUtils.escapeHtml(job.getDescription()));
			b.append("</a>");
			b.append("<div id=\"ARGS_").append(job.getName()).append("\" style=\"display:none\">\n");

			// La liste des paramètres + le bouton démarrer
			final List<JobParam> paramDefs = job.getParameterDefintions();
			b.append("<table border=\"0\" class=\"arguments\">\n");
			int displayedCount = 0;
			for (int i = 0; i < paramDefs.size(); ++i) {
				final JobParam p = paramDefs.get(i);
				if (!p.isEnabled()) {
					continue;
				}
				if (i % NB_COLS_PARAMS == 0) {
					if (i > 0) {
						b.append("</tr>\n");
					}
					b.append("<tr>");
				}
				b.append("<td nowrap0=\"nowrap\" width=\"25%\" align=\"right\">").append(StringEscapeUtils.escapeHtml(p.getDescription())).append("</td>");
				b.append("<td nowrap0=\"nowrap\" width=\"25%\">").append(renderParam(job, p, request)).append("</td>");
				displayedCount++;
			}
			if (displayedCount == 0) {
				b.append("<tr><td>(ce batch ne possède pas de paramètre)</td></tr>");
			}
			if (SecurityProvider.isAnyGranted(Role.ADMIN, Role.TESTER)) {

				// [SIFISC-145] doit-on pouvoir lancer ce batch directement depuis l'IHM ?
				if (job.getJobDefinition().isWebStartable()) {
					b.append("<tr><td class=\"command\" colspan=\"4\" align=\"right\"><input id=\"start").append(job.getName()).append(
							"\" type=\"button\" value=\"Démarrer le batch\" onclick=\"Batch.start('").append(job.getName())
							.append("');\"/></td></tr>");
				}
				else {
					b.append("<tr><td colspan=\"4\" align=\"right\"><i>Ce batch ne peut être lancé depuis l'IHM</i></td></tr>");
				}
			}
			else {
				b.append("<tr><td colspan=\"4\" align=\"right\"><i>Droits insuffisants pour lancer le batch</i></td></tr>");
			}
			b.append("</tr>\n");
			b.append("</table>\n");

			b.append("</div>\n");
		}
		b.append("</form>\n");

		return b.toString();
	}

	private static String renderParam(GestionJob job, JobParam param, HttpServletRequest request) {

		if (param.getType() instanceof JobParamEnum) {
			return renderEnumParam(job, param);
		}
		if (param.getType() instanceof JobParamRegDate) {
			return renderRegDateParam(job, param, request);
		}
		else if (param.getType() instanceof JobParamFile) {
			return renderFile(job, param);
		}
		else if (param.getType() instanceof JobParamBoolean) {
			return renderBooleanParam(job, param);
		}
		else if (param.getType() instanceof JobParamCommune) {
			return renderCommuneParam(job, param);
		}
		else if (param.getType() instanceof JobParamOfficeImpot) {
			return renderOIDParam(job, param);
		}
		else {
			return renderTextParam(job, param);
		}
	}

	private static String getBatchParamNameInForm(JobParam param) {
		final StringBuilder b = new StringBuilder();
		b.append("startParams[").append(param.getName()).append(']');
		return b.toString();
	}

	private static String getBatchParamAnchorInForm(JobParam param) {
		final StringBuilder b = new StringBuilder();
		b.append("startParams_").append(param.getName()).append("_Anchor");
		return b.toString();
	}

	private static String getBatchParamId(GestionJob job, JobParam param) {
		final StringBuilder b = new StringBuilder();
		b.append(job.getName()).append('_').append(param.getName());
		return b.toString();
	}

	private static String renderTextParam(GestionJob job, JobParam param) {

		final Object defaultBalue = job.getJobDefinition().getDefaultWebValue(param.getName());

		final StringBuilder b = new StringBuilder();
		b.append("<input name=\"").append(getBatchParamNameInForm(param)).append("\" type=\"text\" size=\"12\"");
		if (defaultBalue != null) {
			b.append(" value=\"").append(defaultBalue).append('\"');
		}
		b.append("/>");
		return b.toString();
	}

	private static String renderBooleanParam(GestionJob job, JobParam param) {

		final Boolean defaultValue = (Boolean) job.getJobDefinition().getDefaultWebValue(param.getName());

		final StringBuilder b = new StringBuilder();
		b.append("<select name=\"").append(getBatchParamNameInForm(param)).append("\">\n");
		b.append("<option value=\"true\"");
		if (defaultValue == Boolean.TRUE) {
			b.append(" selected=\"selected\"");
		}
		b.append(">true</option>\n");
		b.append("<option value=\"false\"");
		if (defaultValue == Boolean.FALSE) {
			b.append(" selected=\"selected\"");
		}
		b.append(">false</option>\n");
		b.append("</select>");
		return b.toString();
	}

	private static String renderEnumParam(GestionJob job, JobParam param) {

		final Enum<?> defaultValue = (Enum<?>) job.getJobDefinition().getDefaultWebValue(param.getName());

		final StringBuilder b = new StringBuilder();
		b.append("<select name=\"").append(getBatchParamNameInForm(param)).append("\">\n");

		final JobParamEnum type = (JobParamEnum) param.getType();
		final Enum<?>[] enums = (Enum<?>[]) type.getConcreteClass().getEnumConstants();
		if (!param.isMandatory()) {
			b.append("<option/>\n");
		}
		for (Enum<?> e : enums) {
			b.append("<option value=\"").append(e.toString()).append('\"');
			if (defaultValue == e) {
				b.append(" selected=\"selected\"");
			}
			b.append('>').append(e.toString()).append("</option>\n");
		}
		b.append("</select>");
		return b.toString();
	}

	private static String renderRegDateParam(GestionJob job, JobParam param, HttpServletRequest request) {

		final RegDate defaultValue = (RegDate) job.getJobDefinition().getDefaultWebValue(param.getName());

		final StringBuilder b = new StringBuilder();
		final String name = getBatchParamNameInForm(param);
		final String anchor = getBatchParamAnchorInForm(param);
		final String id = getBatchParamId(job, param);

		// input
		b.append("<input  type=\"text\" name=\"").append(name).append('\"');
		if (defaultValue != null) {
			b.append(" value=\"").append(RegDateHelper.dateToDisplayString(defaultValue)).append('\"');
		}
		b.append(" id=\"").append(id).append("\" size=\"10\" maxlength =\"10\" class=\"date\" />\n");

		// calendar
		b.append("<script>\n");
		b.append("    $(function() {\n");
		b.append("        $('#").append(id).append("').datepicker({\n");
		b.append("            showOn: \"button\",\n");
		b.append("            showAnim: '',\n");
		b.append("            yearRange: '1900:+10',\n");
		b.append("            buttonImage: \"").append(request.getContextPath()).append("/css/x/calendar_off.gif\",\n");
		b.append("            buttonImageOnly: true,\n");
		b.append("            changeMonth: true,\n");
		b.append("            changeYear: true\n");
		b.append("        });\n");
		b.append("    });\n");
		b.append("</script>\n");

		return b.toString();
	}

	private static String renderFile(GestionJob job, JobParam param) {

		final Object defaultBalue = job.getJobDefinition().getDefaultWebValue(param.getName());

		final StringBuilder b = new StringBuilder();
		b.append("<input name=\"").append(getBatchParamNameInForm(param)).append("\" type=\"file\" size=\"20\"");
		if (defaultBalue != null) {
			b.append(" value=\"").append(defaultBalue).append('\"');
		}
		b.append("/>");
		return b.toString();
	}

	private static String renderCommuneParam(GestionJob job, JobParam param) {

		final String jobName = job.getName();
		final Commune defaultCommune = (Commune) job.getJobDefinition().getDefaultWebValue(param.getName());
		final String defaultNomCommune = (defaultCommune == null ? "" : defaultCommune.getNomMinuscule());
		final String defaultNoOfsCommune = (defaultCommune == null ? "" : String.valueOf(defaultCommune.getNoOFSEtendu()));

		final String idInputNomCommune = jobName + "NomCommune";
		final String nameInputNoOfs = getBatchParamNameInForm(param);
		final String idInputNoOfs = getBatchParamId(job, param);
		final String idDivAutoComplete = idInputNomCommune + "_autoComplete";
		final String nameMethodOnChange = jobName + "Commune_onChange";

		final StringBuilder b = new StringBuilder();

		// champ de saisie visible à l'utilisateur
		b.append("<input id=\"").append(idInputNomCommune).append("\" name=\"").append(idInputNomCommune).append("\" type=\"text\" value=\"").append(defaultNomCommune).append("\" size=\"25\" />\n");

		// champ contenant le numéro Ofs invisible à l'utilisateur
		b.append("<input id=\"").append(idInputNoOfs).append("\" name=\"").append(nameInputNoOfs).append("\" type=\"hidden\" value=\"").append(defaultNoOfsCommune).append("\"/>\n");

		// la catégorie à donner à la méthode Autocomplete.infra dépend du type de commune que l'on accepte
		final JobParamCommune type = (JobParamCommune) param.getType();
		final String categorie;
		switch (type.getType()) {
			case COMMUNE_VD:
				categorie = "communeVD";
				break;
			case COMMUNE_HC:
				categorie = "communeHC";
				break;
			case COMMUNE_CH:
				categorie = "commune";
				break;
			default:
				throw new RuntimeException("Catégorie de commune inconnue... : " + type.getType());
		}

		// système de recherche ajax de l'OID
		b.append("<script>\n");
		b.append("    $(function() {\n");
		b.append("        Autocomplete.infra('").append(categorie).append("', '#").append(idInputNomCommune).append("', true, function(item) {\n");
		b.append("            if (item) {\n");
		b.append("                $('#").append(idInputNoOfs).append("').val(item.id1);\n");
		b.append("                $('#").append(idInputNomCommune).append("').removeClass('error');\n");
		b.append("            }\n");
		b.append("            else {\n");
		b.append("                $('#").append(idInputNomCommune).append("').addClass('error');\n"); // [SIFISC-832] la valeur saisie est inconnue : on le signal en changeant la couleur du champs
		b.append("                $('#").append(idInputNoOfs).append("').val(null);\n");
		b.append("            }\n");
		b.append("        });\n");
		b.append("    });\n");
		b.append("</script>");

		return b.toString();
	}

	private static String renderOIDParam(GestionJob job, JobParam param) {
		final String jobName = job.getName();
		final OfficeImpot defaultOID = (OfficeImpot) job.getJobDefinition().getDefaultWebValue(param.getName());
		final String defaultNomOID = (defaultOID == null ? "" : defaultOID.getNomCourt());
		final String defaultNoColAdm = (defaultOID == null ? "" : String.valueOf(defaultOID.getNoColAdm()));

		final String idInputNomOID = jobName + "NomOID";
		final String nameInputNoColAdm = getBatchParamNameInForm(param);
		final String idInputNoColAdm = getBatchParamId(job, param);
		final String idDivAutoComplete = idInputNomOID + "_autoComplete";
		final String nameMethodOnChange = idInputNomOID + "_onChange";

		final StringBuilder b = new StringBuilder();

		// champ de saisie visible à l'utilisateur
		b.append("<input id=\"").append(idInputNomOID).append("\" name=\"").append(idInputNomOID).append("\" type=\"text\" value=\"").append(defaultNomOID).append("\" size=\"25\" />\n");

		// champ contenant le numéro invisible à l'utilisateur
		b.append("<input id=\"").append(idInputNoColAdm).append("\" name=\"").append(nameInputNoColAdm).append("\" type=\"hidden\" value=\"").append(defaultNoColAdm).append("\" />\n");

		// système de recherche ajax de l'OID
		b.append("<script>\n");
		b.append("    $(function() {\n");
		b.append("        Autocomplete.infra('officeImpot', '#").append(idInputNomOID).append("', true, function(item) {\n");
		b.append("            if (item) {\n");
		b.append("                $('#").append(idInputNoColAdm).append("').val(item.id1);\n");
		b.append("                $('#").append(idInputNomOID).append("').removeClass('error');\n");
		b.append("            }\n");
		b.append("            else {\n");
		b.append("                $('#").append(idInputNomOID).append("').addClass('error');\n"); // [SIFISC-832] la valeur saisie est inconnue : on le signal en changeant la couleur du champs
		b.append("                $('#").append(idInputNoColAdm).append("').val(null);\n");
		b.append("            }\n");
		b.append("        });\n");
		b.append("    });\n");
		b.append("</script>");
		
		return b.toString();
	}

	public void setJob(GestionJob job) {
		this.job = job;
	}
}
