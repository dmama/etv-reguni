package ch.vd.uniregctb.json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.infrastructure.model.EnumTypeCollectivite;
import ch.vd.uniregctb.common.StringComparator;
import ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Localite;
import ch.vd.uniregctb.interfaces.model.OfficeImpot;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.model.Rue;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

/**
 * Contrôleur qui expose des données d'infrastructure dans un format Json (utilisé ensuite dans le mécanisme d'autocompletion).
 */
@Controller
public class AutoCompleteInfraController {

	protected final Logger LOGGER = Logger.getLogger(AutoCompleteInfraController.class);

	private ServiceInfrastructureService serviceInfrastructureService;

	private enum Category {
		RUE,
		LOCALITE,
		COMMUNE,
		COMMUNE_VD,
		COMMUNE_HC,
		ETAT,
		TERRITOIRE,
		COLLECTIVITE_ADMINISTRATIVE,
		JUSTICES_DE_PAIX,
		OFFICES_IMPOT
	}

	@SuppressWarnings({"UnusedDeclaration"})
	private static class Item {
		/**
		 * Chaîne de caractères utilisée dans le champ d'autocompletion
		 */
		private final String label;
		/**
		 * Chaîne de caractères utilisée dans la liste (dropdown) des valeurs disponibles
		 */
		private final String desc;
		/**
		 * Identifiant optionnel pouvant être affecté à un autre champ (généralement caché).
		 */
		private String id1;
		/**
		 * Second identifiant optionnel pouvant être affecté à un autre champ (généralement caché).
		 */
		private String id2;

		private Item(String label, String desc) {
			this.label = label;
			this.desc = desc;
		}

		private Item(String label, String desc, String id1) {
			this.label = label;
			this.desc = desc;
			this.id1 = id1;
		}

		private Item(String label, String desc, String id1, String id2) {
			this.label = label;
			this.desc = desc;
			this.id1 = id1;
			this.id2 = id2;
		}

		public String getLabel() {
			return label;
		}

		public String getDesc() {
			return desc;
		}

		public String getId1() {
			return id1;
		}

		public String getId2() {
			return id2;
		}
	}

	/**
	 * Retourne des données du service d'infrastructure sous forme JSON (voir http://blog.springsource.com/2010/01/25/ajax-simplifications-in-spring-3-0/)
	 *
	 * @param category   le catégorie de données désirée
	 * @param term       un critère de recherche des données
	 * @param numCommune le numéro Ofs d'une commune (optionel)
	 * @return le nombre d'immeubles du contribuable spécifié.
	 * @throws Exception en cas de problème
	 */
	@RequestMapping(value = "/autocomplete/infra.do", method = RequestMethod.GET)
	@ResponseBody
	public List<Item> infra(@RequestParam("category") String category, @RequestParam("term") String term, @RequestParam(value = "numCommune", required = false) Integer numCommune) throws Exception {

		final Set<Category> categories = parseCategories(category);

		// les urls sont envoyées en UTF-8 par jQuery mais interprétées en ISO-8859-1 par Tomcat
		final byte[] bytes = term.getBytes("ISO-8859-1");
		term = new String(bytes, "UTF-8");

		// on ignore les accents
		term = StringComparator.toLowerCaseWithoutAccent(term);

		final List<Item> list = new ArrayList<Item>();

		if (categories.contains(Category.RUE)) {
			if (numCommune == null) {
				list.add(new Item("#error: pas de localité renseignée", "#error: pas de localité renseignée"));
			}
			else {
				final List<Localite> localites = serviceInfrastructureService.getLocaliteByCommune(numCommune);
				final List<Rue> rues = serviceInfrastructureService.getRues(localites);
				final Map<Integer, Localite> mapLocalites = buildLocaliteMap(localites);
				if (rues != null) {
					for (Rue rue : rues) {
						if (StringComparator.toLowerCaseWithoutAccent(rue.getDesignationCourrier()).contains(term)) { // [UNIREG-3383] recherche dans toute la string
							final Localite localite = mapLocalites.get(rue.getNoLocalite());
							final String description;
							if (localite == null) {
								description = rue.getDesignationCourrier();
							}
							else {
								// [UNIREG-3293] on renseigne la localité entre parenthèses pour permettre de distinguer deux rues avec le même nom
								description = rue.getDesignationCourrier() + " (" + localite.getNPA() + " " + localite.getNomAbregeMinuscule() + ")";
							}
							list.add(new Item(rue.getDesignationCourrier(), description, String.valueOf(rue.getNoRue()), String.valueOf(rue.getNoLocalite())));
						}
					}
				}
			}
		}

		if (categories.contains(Category.LOCALITE)) {
			final List<Localite> localites = serviceInfrastructureService.getLocalites();
			if (localites != null) {
				for (Localite localite : localites) {
					if (StringComparator.toLowerCaseWithoutAccent(localite.getNomAbregeMinuscule()).startsWith(term) ||
							String.valueOf(localite.getNPA()).startsWith(term)) { // [UNIREG-3390] recherche par numéro de NPA
						final String description = localite.getNomAbregeMinuscule() + " (" + localite.getNPA() + ")";
						list.add(new Item(localite.getNomAbregeMinuscule(), description, String.valueOf(localite.getNoOrdre()), String.valueOf(localite.getNoCommune())));
					}
				}
			}
		}

		if (categories.contains(Category.COMMUNE)) {
			final List<Commune> communes = serviceInfrastructureService.getCommunes();
			if (communes != null) {
				for (Commune commune : communes) {
					if (StringComparator.toLowerCaseWithoutAccent(commune.getNomMinuscule()).startsWith(term)) {
						final String description = commune.getNomMinuscule() + " (" + commune.getNoOFSEtendu() + ")";
						list.add(new Item(commune.getNomMinuscule(), description, String.valueOf(commune.getNoOFSEtendu())));
					}
				}
			}
		}

		if (categories.contains(Category.COMMUNE_VD)) {
			final List<Commune> communes = serviceInfrastructureService.getListeFractionsCommunes();
			if (communes != null) {
				for (Commune commune : communes) {
					if (StringComparator.toLowerCaseWithoutAccent(commune.getNomMinuscule()).startsWith(term)) {
						final String description = commune.getNomMinuscule() + " (" + commune.getNoOFSEtendu() + ")";
						list.add(new Item(commune.getNomMinuscule(), description, String.valueOf(commune.getNoOFSEtendu())));
					}
				}
			}
		}

		if (categories.contains(Category.COMMUNE_HC)) {
			final List<Commune> communes = serviceInfrastructureService.getCommunesHorsCanton();
			if (communes != null) {
				for (Commune commune : communes) {
					if (StringComparator.toLowerCaseWithoutAccent(commune.getNomMinuscule()).startsWith(term)) {
						final String description = commune.getNomMinuscule() + " (" + commune.getNoOFSEtendu() + ")";
						list.add(new Item(commune.getNomMinuscule(), description, String.valueOf(commune.getNoOFSEtendu())));
					}
				}
			}
		}

		if (categories.contains(Category.ETAT) || categories.contains(Category.TERRITOIRE)) {
			final List<Pays> pays = serviceInfrastructureService.getPays();
			if (pays != null) {
				final boolean etatsOnly = !categories.contains(Category.TERRITOIRE);
				for (Pays p : pays) {
					if (p.isValide() && (!etatsOnly || p.isEtatSouverain())) { // [UNIREG-3338] on ne permet de sélectionner que les pays valides
						if (StringComparator.toLowerCaseWithoutAccent(p.getNomMinuscule()).startsWith(term)) {
							final String description = p.getNomMinuscule() + " (" + p.getNoOFS() + ")";
							list.add(new Item(p.getNomMinuscule(), description, String.valueOf(p.getNoOFS())));
						}
					}
				}
			}
		}

		if (categories.contains(Category.COLLECTIVITE_ADMINISTRATIVE)) {
			final List<CollectiviteAdministrative> colls = serviceInfrastructureService.getCollectivitesAdministratives(
					Arrays.asList(EnumTypeCollectivite.SIGLE_ACI, EnumTypeCollectivite.SIGLE_ACIA, EnumTypeCollectivite.SIGLE_ACIFD, EnumTypeCollectivite.SIGLE_ACIPP, EnumTypeCollectivite.SIGLE_CIR,
							EnumTypeCollectivite.SIGLE_S_ACI));
			if (colls != null) {
				for (CollectiviteAdministrative c : colls) {
					if (StringComparator.toLowerCaseWithoutAccent(c.getNomCourt()).startsWith(term)) {
						list.add(new Item(c.getNomCourt(), c.getNomCourt(), String.valueOf(c.getNoColAdm())));
					}
				}
			}
		}

		if (categories.contains(Category.JUSTICES_DE_PAIX)) {
			final List<CollectiviteAdministrative> colls = serviceInfrastructureService.getCollectivitesAdministratives(Arrays.asList(EnumTypeCollectivite.SIGLE_JPAIX));
			if (colls != null) {
				for (CollectiviteAdministrative c : colls) {
					final String nomComplet = String.format("%s %s", c.getNomComplet1(), c.getNomComplet2());
					if (StringComparator.toLowerCaseWithoutAccent(nomComplet).startsWith(term)) {
						list.add(new Item(nomComplet, nomComplet, String.valueOf(c.getNoColAdm())));
					}
				}
			}
		}

		if (categories.contains(Category.OFFICES_IMPOT)) {
			final List<OfficeImpot> offices = serviceInfrastructureService.getOfficesImpot();
			if (offices != null) {
				for (OfficeImpot oi : offices) {
					if (StringComparator.toLowerCaseWithoutAccent(oi.getNomCourt()).startsWith(term)) {
						list.add(new Item(oi.getNomCourt(), oi.getNomCourt(), String.valueOf(oi.getNoColAdm())));
					}
				}
			}
		}

		return list;
	}

	private Map<Integer, Localite> buildLocaliteMap(List<Localite> localites) {
		if (localites == null || localites.isEmpty()) {
			return Collections.emptyMap();
		}
		final HashMap<Integer, Localite> map = new HashMap<Integer, Localite>();
		for (Localite l : localites) {
			map.put(l.getNoOrdre(), l);
		}
		return map;
	}

	private static Set<Category> parseCategories(String category) {
		final Set<Category> categories = new HashSet<Category>();
		if ("rue".equalsIgnoreCase(category)) {
			categories.add(Category.RUE);
		}
		else if ("localite".equalsIgnoreCase(category)) {
			categories.add(Category.LOCALITE);
		}
		else if ("etat".equalsIgnoreCase(category)) {
			categories.add(Category.ETAT);
		}
		else if ("etatOuTerritoire".equalsIgnoreCase(category)) {
			categories.add(Category.ETAT);
			categories.add(Category.TERRITOIRE);
		}
		else if ("localiteOuPays".equalsIgnoreCase(category)) {
			categories.add(Category.LOCALITE);
			categories.add(Category.ETAT);
			categories.add(Category.TERRITOIRE);
		}
		else if ("communeOuPays".equalsIgnoreCase(category)) {
			categories.add(Category.COMMUNE);
			categories.add(Category.ETAT);
		}
		else if ("commune".equalsIgnoreCase(category)) {
			categories.add(Category.COMMUNE);
		}
		else if ("communeVD".equalsIgnoreCase(category)) {
			categories.add(Category.COMMUNE_VD);
		}
		else if ("communeHC".equalsIgnoreCase(category)) {
			categories.add(Category.COMMUNE_HC);
		}
		else if ("collectiviteAdministrative".equalsIgnoreCase(category)) {
			categories.add(Category.COLLECTIVITE_ADMINISTRATIVE);
		}
		else if ("justicePaix".equalsIgnoreCase(category)) {
			categories.add(Category.JUSTICES_DE_PAIX);
		}
		else if ("officeImpot".equalsIgnoreCase(category)) {
			categories.add(Category.OFFICES_IMPOT);
		}

		return categories;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceInfrastructureService(ServiceInfrastructureService serviceInfrastructureService) {
		this.serviceInfrastructureService = serviceInfrastructureService;
	}
}
