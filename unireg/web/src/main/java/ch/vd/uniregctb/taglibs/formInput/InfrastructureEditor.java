package ch.vd.uniregctb.taglibs.formInput;

import javax.servlet.jsp.JspException;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.servlet.tags.form.TagWriter;

import ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Localite;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.model.Rue;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.json.InfraCategory;

public class InfrastructureEditor implements Editor {

	static ServiceInfrastructureService infraService;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setService(ServiceInfrastructureService infraService) {
		InfrastructureEditor.infraService = infraService;
	}

	private EditorParams params;

	public InfrastructureEditor(EditorParams params) {
		this.params = params;
	}

	@Override
	public void generate(TagWriter tagWriter, String value) throws JspException {

		final InfraCategory category = (InfraCategory) params.getCategorie();

		final Long infraId;
		if (value == null) {
			infraId = null;
		}
		else {
			// on ignore tous les caractères non-numériques
			value = value.replaceAll("[^\\d]", "");
			infraId = parseLong(value);
		}

		if (category == null) {
			if (infraId == null) {
				// rien à faire
			}
			else {
				tagWriter.startTag("span");
				tagWriter.appendValue(infraId.toString());
				tagWriter.endTag();
			}
		}
		else if (params.isReadonly()) {
			if (infraId == null) {
				// rien à faire
			}
			else {
				tagWriter.startTag("span");
				tagWriter.appendValue(getInfraName(infraId, category));
				tagWriter.endTag();
			}
		}
		else {
			final String id = params.getId();
			if (StringUtils.isBlank(id)) {
				throw new IllegalArgumentException("L'id doit être renseigné.");
			}
			final String path = params.getPath();

			// le champ input visible
			tagWriter.startTag("input");
			tagWriter.writeAttribute("id", id);
			if (infraId != null) {
				tagWriter.writeAttribute("value", getInfraName(infraId, category));
			}
			tagWriter.endTag();

			// un champ caché qui contient la valeur utile
			tagWriter.startTag("input");
			tagWriter.writeAttribute("id", getHiddenId(id));
			tagWriter.writeAttribute("type", "hidden");
			if (StringUtils.isNotBlank(path)) {
				tagWriter.writeAttribute("name", path);
			}
			if (infraId != null) {
				tagWriter.writeAttribute("value", infraId.toString());
			}
			tagWriter.endTag();

			final String script;

			if (category == InfraCategory.LOCALITE) {
				// cas spécial pour la localité, on mémorise l'id de la commune pour pouvoir faire l'autocompletion sur les rues si nécessaire dans la même page

				if (!id.equals("localite")) {
					throw new IllegalArgumentException("L'id du champ d'input pour la catégorie LOCALITE doit être 'localite'.");
				}

				tagWriter.startTag("input");
				tagWriter.writeAttribute("id", "numCommune");
				tagWriter.writeAttribute("type", "hidden");
				if (infraId != null) {
					final String communeId = getCommuneId(infraId);
					if (communeId != null) {
						tagWriter.writeAttribute("value", communeId);
					}
				}
				tagWriter.endTag();

				script = "$(function() {\n" +
						"\tautocomplete_infra('" + category.getTag() + "', '#" + id + "', true, function(item) {\n" +
						"\t\t$('#" + getHiddenId(id) + "').val(item ? item.id1 : null);\n" +
						"\t\t$('#numCommune').val(item ? item.id2 : null);\n" +
						'\n' +
						"\t\t// à chaque changement de localité, on adapte l'autocompletion sur la rue en conséquence\n" +
						"\t\tautocomplete_infra('rue&numCommune=' + $('#numCommune').val(), '#rue', true, function(i) {\n" +
						"\t\t\tif (i) {\n" +
						"\t\t\t\t$('#_rueId').val(i.id1);\n" +
						"\t\t\t\t$('#_localiteId').val(i.id2);\n" +
						"\t\t\t}\n" +
						"\t\t\telse {\n" +
						"\t\t\t\t$('#_rueId').val(null);\n" +
						"\t\t\t}\n" +
						"\t\t});\n" +
						"\t});\n" +
						"});";
			}
			else if (category == InfraCategory.RUE) {
				// cas spécial pour les rues : l'autocompletion dépend de l'id de la commune choisir dans l'autocompletion de la localité

				if (!id.equals("rue")) {
					throw new IllegalArgumentException("L'id du champ d'input pour la catégorie RUE doit être 'rue'.");
				}
				
				script = "$(function() {\n" +
						"\tautocomplete_infra('rue&numCommune=' + $('#numCommune').val(), '#rue', true, function(i) {\n" +
						"\t\tif (i) {\n" +
						"\t\t\t$('#_rueId').val(i.id1);\n" +
						"\t\t\t$('#_localiteId').val(i.id2);\n" +
						"\t\t}\n" +
						"\t\telse {\n" +
						"\t\t\t$('#_rueId').val(null);\n" +
						"\t\t}\n" +
						"\t});\n" +
						"});";
			}
			else {
				// cas général
				script = "$(function() {\n" +
						"\tautocomplete_infra('" + category.getTag() + "', '#" + id + "', true, function(item) {\n" +
						"\t\t$('#" + getHiddenId(id) + "').val(item ? item.id1 : null);\n" +
						"\t});\n" +
						"});";
			}

			tagWriter.startTag("script");
			tagWriter.appendValue(script);
			tagWriter.endTag();
		}
	}

	private static String getCommuneId(long localiteId) {
		Localite localite = infraService.getLocaliteByONRP((int)localiteId);
		Integer communeId = (localite == null ? null : localite.getNoCommune());
		return communeId == null ? null : communeId.toString();
	}
	private static String getHiddenId(String id) {
		return '_' + id + "Id";
	}

	private static Long parseLong(String value) {
		try {
			return Long.parseLong(value);
		}
		catch (NumberFormatException e) {
			return null;
		}
	}

	private static String getInfraName(@NotNull Long id, @NotNull InfraCategory category) {
		switch (category) {
		case OFFICES_IMPOT:
		case JUSTICES_DE_PAIX:
		case COLLECTIVITE_ADMINISTRATIVE:
			final CollectiviteAdministrative coll = infraService.getCollectivite(id.intValue());
			return coll == null ? "?" : coll.getNomCourt();
		case COMMUNE:
		case COMMUNE_HC:
		case COMMUNE_VD:
			final Commune commune = infraService.getCommuneByNumeroOfsEtendu(id.intValue(), null);
			if (commune == null) {
				return "?";
			}
			else {
				if (commune.isVaudoise()) {
					return commune.getNomMinuscule();
				}
				else {
					return commune.getNomMinuscule() + " (" + commune.getSigleCanton() + ')';
				}
			}
		case ETAT:
		case TERRITOIRE:
			final Pays pays = infraService.getPays(id.intValue());
			return pays == null ? "?" : pays.getNomMinuscule();
		case LOCALITE:
			final Localite localite = infraService.getLocaliteByONRP(id.intValue());
			return localite == null ? "?" : localite.getNomAbregeMinuscule();
		case RUE:
			final Rue rue = infraService.getRueByNumero(id.intValue());
			return rue == null ? "?" : rue.getDesignationCourrier();
		default:
			throw new IllegalArgumentException("La catégorie d'infrastructure [" + category + "] est inconnue !");
		}
	}
}
