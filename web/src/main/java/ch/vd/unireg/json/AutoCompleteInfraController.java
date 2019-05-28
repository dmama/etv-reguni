package ch.vd.unireg.json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.StringComparator;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.Localite;
import ch.vd.unireg.interfaces.infra.data.OfficeImpot;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.infra.data.Rue;
import ch.vd.unireg.interfaces.infra.data.TypeCollectivite;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;

/**
 * Contrôleur qui expose des données d'infrastructure dans un format Json (utilisé ensuite dans le mécanisme d'autocompletion).
 */
@Controller
public class AutoCompleteInfraController {

	private static final Logger LOGGER = LoggerFactory.getLogger(AutoCompleteInfraController.class);

	private ServiceInfrastructureService serviceInfrastructureService;

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
	public List<AutoCompleteItem> infra(@RequestParam("category") String category, @RequestParam("term") String term, @RequestParam(value = "numCommune", required = false) Integer numCommune) throws Exception {

		final Set<InfraCategory> categories = parseCategories(category);

		// les urls sont envoyées en UTF-8 par jQuery mais interprétées en ISO-8859-1 par Tomcat
		// --> depuis tomcat 8, ce n'est plus vrai, les urls sont bien interprétées en UTF-8 par Tomcat par défaut... (http://tomcat.apache.org/migration-8.html#URIEncoding)
		//term = EncodingFixHelper.fixFromIso(term);

		// on ignore les accents
		term = StringComparator.toLowerCaseWithoutAccent(term);

		final List<AutoCompleteItem> list = new ArrayList<>();

		if (categories.contains(InfraCategory.RUE)) {
			if (numCommune == null) {
				list.add(new AutoCompleteItem("#error: pas de localité renseignée", "#error: pas de localité renseignée"));
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
								description = rue.getDesignationCourrier() + " (" + localite.getNPA() + ' ' + localite.getNom() + ')';
							}
							list.add(new AutoCompleteItem(rue.getDesignationCourrier(), description, String.valueOf(rue.getNoRue()), String.valueOf(rue.getNoLocalite())));
						}
					}
				}
			}
		}

		if (categories.contains(InfraCategory.LOCALITE)) {
			final List<Localite> localites = serviceInfrastructureService.getLocalites();
			if (localites != null) {
				for (Localite localite : localites) {
					if (StringComparator.toLowerCaseWithoutAccent(localite.getNom()).startsWith(term) ||
							String.valueOf(localite.getNPA()).startsWith(term)) { // [UNIREG-3390] recherche par numéro de NPA
						final String description = localite.getNom() + " (" + localite.getNPA() + ')';
						list.add(new AutoCompleteItem(localite.getNom(), description, String.valueOf(localite.getNoOrdre()), String.valueOf(localite.getNoCommune())));
					}
				}
			}
		}

		if (categories.contains(InfraCategory.COMMUNE)) {
			final List<Commune> communes = serviceInfrastructureService.getCommunes();
			if (communes != null) {
				for (Commune commune : communes) {
					if (StringComparator.toLowerCaseWithoutAccent(commune.getNomOfficiel()).startsWith(term)) {
						final String description = commune.getNomOfficiel() + " (" + commune.getNoOFS() + ')';
						list.add(new AutoCompleteItem(commune.getNomOfficiel(), description, String.valueOf(commune.getNoOFS())));
					}
				}
			}
		}

		if (categories.contains(InfraCategory.COMMUNE_VD)) {
			final List<Commune> communes = serviceInfrastructureService.getCommunesVD();
			if (communes != null) {
				for (Commune commune : communes) {
					if (StringComparator.toLowerCaseWithoutAccent(commune.getNomOfficiel()).startsWith(term)) {
						final String description = commune.getNomOfficiel() + " (" + commune.getNoOFS() + ')';
						list.add(new AutoCompleteItem(commune.getNomOfficiel(), description, String.valueOf(commune.getNoOFS())));
					}
				}
			}
		}

		if (categories.contains(InfraCategory.COMMUNE_HC)) {
			final List<Commune> communes = serviceInfrastructureService.getCommunesHorsCanton();
			if (communes != null) {
				for (Commune commune : communes) {
					if (StringComparator.toLowerCaseWithoutAccent(commune.getNomOfficiel()).startsWith(term)) {
						final String description = commune.getNomOfficiel() + " (" + commune.getNoOFS() + ')';
						list.add(new AutoCompleteItem(commune.getNomOfficiel(), description, String.valueOf(commune.getNoOFS())));
					}
				}
			}
		}

		if (categories.contains(InfraCategory.ETAT) || categories.contains(InfraCategory.TERRITOIRE)) {
			final List<Pays> pays = serviceInfrastructureService.getPays();
			if (pays != null) {
				final boolean etatsOnly = !categories.contains(InfraCategory.TERRITOIRE);
				for (Pays p : pays) {
					// [UNIREG-3338] on ne permet de sélectionner que les pays valides
					// [SIFISC-8603] seuls les versions des pays valides MAINTENANT sont proposés
					if (p.isValidAt(RegDate.get()) && (!etatsOnly || p.isEtatSouverain())) {
						if (StringComparator.toLowerCaseWithoutAccent(p.getNomCourt()).startsWith(term)) {
							final String description = p.getNomCourt() + " (" + p.getNoOFS() + ')';
							list.add(new AutoCompleteItem(p.getNomCourt(), description, String.valueOf(p.getNoOFS())));
						}
					}
				}
			}
		}

		if (categories.contains(InfraCategory.COLLECTIVITE_ADMINISTRATIVE)) {
			final List<CollectiviteAdministrative> colls = serviceInfrastructureService.getCollectivitesAdministratives(
					Arrays.asList(TypeCollectivite.SIGLE_ACI, TypeCollectivite.SIGLE_ACIA, TypeCollectivite.SIGLE_ACIFD, TypeCollectivite.SIGLE_ACIPP, TypeCollectivite.SIGLE_CIR,
					              TypeCollectivite.SIGLE_S_ACI));
			if (colls != null) {
				for (CollectiviteAdministrative c : colls) {
					if (StringComparator.toLowerCaseWithoutAccent(c.getNomCourt()).startsWith(term)) {
						list.add(new AutoCompleteItem(c.getNomCourt(), c.getNomCourt(), String.valueOf(c.getNoColAdm())));
					}
				}
				list.sort(Comparator.comparing(AutoCompleteItem::getDesc));
			}
		}

		if (categories.contains(InfraCategory.JUSTICES_DE_PAIX)) {
			final List<CollectiviteAdministrative> colls = serviceInfrastructureService.getCollectivitesAdministratives(Collections.singletonList(TypeCollectivite.SIGLE_JPAIX));
			if (colls != null) {
				for (CollectiviteAdministrative c : colls) {
					final String nomComplet = Stream.of(c.getNomComplet1(), c.getNomComplet2()).filter(StringUtils::isNotEmpty).collect(Collectors.joining(" "));
					if (StringComparator.toLowerCaseWithoutAccent(nomComplet).startsWith(term)) {
						list.add(new AutoCompleteItem(nomComplet, nomComplet, String.valueOf(c.getNoColAdm())));
					}
				}
			}
		}

		if (categories.contains(InfraCategory.OFFICES_IMPOT)) {
			final List<OfficeImpot> offices = serviceInfrastructureService.getOfficesImpot();
			if (offices != null) {
				for (OfficeImpot oi : offices) {
					if (StringComparator.toLowerCaseWithoutAccent(oi.getNomCourt()).startsWith(term)) {
						list.add(new AutoCompleteItem(oi.getNomCourt(), oi.getNomCourt(), String.valueOf(oi.getNoColAdm())));
					}
				}
			}
		}

		return list;
	}

	private static Map<Integer, Localite> buildLocaliteMap(List<Localite> localites) {
		if (localites == null || localites.isEmpty()) {
			return Collections.emptyMap();
		}
		final Map<Integer, Localite> map = new HashMap<>();
		for (Localite l : localites) {
			map.put(l.getNoOrdre(), l);
		}
		return map;
	}

	private static Set<InfraCategory> parseCategories(String category) {
		final Set<InfraCategory> categories = EnumSet.noneOf(InfraCategory.class);
		if ("rue".equalsIgnoreCase(category)) {
			categories.add(InfraCategory.RUE);
		}
		else if ("localite".equalsIgnoreCase(category)) {
			categories.add(InfraCategory.LOCALITE);
		}
		else if ("etat".equalsIgnoreCase(category)) {
			categories.add(InfraCategory.ETAT);
		}
		else if ("etatOuTerritoire".equalsIgnoreCase(category)) {
			categories.add(InfraCategory.ETAT);
			categories.add(InfraCategory.TERRITOIRE);
		}
		else if ("localiteOuPays".equalsIgnoreCase(category)) {
			categories.add(InfraCategory.LOCALITE);
			categories.add(InfraCategory.ETAT);
			categories.add(InfraCategory.TERRITOIRE);
		}
		else if ("communeOuPays".equalsIgnoreCase(category)) {
			categories.add(InfraCategory.COMMUNE);
			categories.add(InfraCategory.ETAT);
		}
		else if ("commune".equalsIgnoreCase(category)) {
			categories.add(InfraCategory.COMMUNE);
		}
		else if ("communeVD".equalsIgnoreCase(category)) {
			categories.add(InfraCategory.COMMUNE_VD);
		}
		else if ("communeHC".equalsIgnoreCase(category)) {
			categories.add(InfraCategory.COMMUNE_HC);
		}
		else if ("collectiviteAdministrative".equalsIgnoreCase(category)) {
			categories.add(InfraCategory.COLLECTIVITE_ADMINISTRATIVE);
		}
		else if ("justicePaix".equalsIgnoreCase(category)) {
			categories.add(InfraCategory.JUSTICES_DE_PAIX);
		}
		else if ("officeImpot".equalsIgnoreCase(category)) {
			categories.add(InfraCategory.OFFICES_IMPOT);
		}

		return categories;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceInfrastructureService(ServiceInfrastructureService serviceInfrastructureService) {
		this.serviceInfrastructureService = serviceInfrastructureService;
	}
}
