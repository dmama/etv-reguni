package ch.vd.uniregctb.indexer.tiers;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.tiers.*;

import java.util.*;

/**
 * Objet permettant d'indexer des contribuables ménage commun.
 *
 * @author Akram BEN AISSI <mailto:akram.ben-aissi@vd.ch>
 */
public class MenageCommunIndexable extends ContribuableIndexable {

	// private final Logger LOGGER = Logger.getLogger(MenageCommunIndexable.class);

	private final PersonnePhysiqueIndexable ppIndexable1;
	private final PersonnePhysiqueIndexable ppIndexable2;

	public static final String SUB_TYPE = "menagecommun";

	public MenageCommunIndexable(AdresseService adresseService, TiersService tiersService, ServiceCivilService serviceCivil, ServiceInfrastructureService serviceInfra, MenageCommun menage) throws IndexerException {
		super(adresseService, tiersService, serviceInfra, menage, new MenageCommunSubIndexable(tiersService, menage));

		final EnsembleTiersCouple ensemble = extractEnsembleForIndexation(tiersService, menage);
		ppIndexable1 = getPPIndexable(adresseService, tiersService, serviceCivil, serviceInfra, ensemble.getPrincipal());
		if (ensemble.getConjoint() != null) {
			ppIndexable2 = getPPIndexable(adresseService, tiersService, serviceCivil, serviceInfra, ensemble.getConjoint());
		}
		else ppIndexable2 = null;//marié seul => pas d'indexation du conjoint
	}

	/**
	 * Extrait l'ensemble tiers couple tel qu'il doit être indexé. Cet ensemble diffère de celui construit par le tiers service par le fait que les rapports entre tiers annulés sont quand même pris en
	 * compte (voir cas UNIREG-601).
	 */
	private static EnsembleTiersCouple extractEnsembleForIndexation(TiersService tiersService, MenageCommun menage) {

		// UNIREG-601, UNIREG-1619 : on demande TOUTES les personnes physiques impliquées dans ce ménage commun, y compris
		// celles dont les rapports ont été finalement annulés, mais comme l'indexation d'un ménage commun ne permet d'associer
		// que deux personnes physiques, on n'en prend que 2 au maximum (dans le cas de rattrapage de données, il est
		// tout à fait possible d'en avoir plus de 2, tant qu'au maximum 2 ne sont pas annulés...)

		// Après réflexion (la nuit porte conseil...), il est sans doute préférable de choisir l'algorithme suivant :
		// 1. si au moins un lien n'est pas annulé, on ne prend que les liens non-annulés
		// 2. s'ils sont tous annulés, alors on trie les liens et on en prend au maximum deux
		final Set<PersonnePhysique> personnesPhysiques;
		final Map<PersonnePhysique, RapportEntreTiers> personnes = tiersService.getToutesPersonnesPhysiquesImpliquees(menage);
		if (personnes != null && personnes.size() > 0) {
			final Set<PersonnePhysique> nonAnnules = new HashSet<PersonnePhysique>(2);
			for (Map.Entry<PersonnePhysique, RapportEntreTiers> entry : personnes.entrySet()) {
				if (!entry.getValue().isAnnule()) {
					nonAnnules.add(entry.getKey());
				}
			}
			Assert.isTrue(nonAnnules.size() <= 2, "Plus de deux personnes avec lien non-annulé dans le ménage " + menage.getNumero());

			// s'il y a des liens non-annulés, on ne va pas chercher plus loin
			if (nonAnnules.size() > 0) {
				personnesPhysiques = nonAnnules;
			}
			else {
				// tous les liens sur ce ménage sont annulés
				final List<Map.Entry<PersonnePhysique, RapportEntreTiers>> aPrendre = new ArrayList<Map.Entry<PersonnePhysique, RapportEntreTiers>>(personnes.entrySet());
				if (aPrendre.size() > 1) {
					Collections.sort(aPrendre, new Comparator<Map.Entry<PersonnePhysique, RapportEntreTiers>>() {
						public int compare(Map.Entry<PersonnePhysique, RapportEntreTiers> o1, Map.Entry<PersonnePhysique, RapportEntreTiers> o2) {
							final RapportEntreTiers rapport1 = o1.getValue();
							final RapportEntreTiers rapport2 = o2.getValue();
							int comparaison = MenageCommunIndexable.compare(rapport1.getAnnulationDate(), rapport2.getAnnulationDate(), false);
							if (comparaison == 0) {
								// à même date d'annulation, on regarde la date de fin du rapport
								comparaison = MenageCommunIndexable.compare(rapport1.getDateFin(), rapport2.getDateFin(), false);
								if (comparaison == 0) {
									// à même date de fin de rapport, on regarde la date de création du rapport
									comparaison = MenageCommunIndexable.compare(rapport1.getLogCreationDate(), rapport2.getLogCreationDate(), false);
								}
							}
							return comparaison;
						}
					});
				}
				personnesPhysiques = new HashSet<PersonnePhysique>(2);
				for (int i = aPrendre.size() - 1; i >= 0 && i >= aPrendre.size() - 2; --i) {
					personnesPhysiques.add(aPrendre.get(i).getKey());
				}
			}
		}
		else {
			personnesPhysiques = Collections.emptySet();
		}

		PersonnePhysique pp1 = null;
		PersonnePhysique pp2 = null;
		for (PersonnePhysique pp : personnesPhysiques) {
			if (pp1 == null) {
				pp1 = pp;
			}
			else {
				pp2 = pp;
			}
		}

		final PersonnePhysique principal = tiersService.getPrincipal(pp1, pp2);
		final PersonnePhysique conjoint = (principal == pp1 ? pp2 : pp1);
		return new EnsembleTiersCouple(menage, principal, conjoint);
	}

	/**
	 * si nullAvant est true, alors null sera toujours en tête, sinon null sera en fin de liste
	 */
	private static <T extends Comparable<T>> int compare(T o1, T o2, boolean nullAvant) {
		if (o1 == o2) {
			return 0;
		}
		else if (o1 == null) {
			return nullAvant ? -1 : 1;
		}
		else if (o2 == null) {
			return nullAvant ? 1 : -1;
		}
		else {
			return o1.compareTo(o2);
		}
	}

	private static PersonnePhysiqueIndexable getPPIndexable(AdresseService adresseService, TiersService tiersService, ServiceCivilService serviceCivil, ServiceInfrastructureService serviceInfra,
	                                                        PersonnePhysique pp) {
		PersonnePhysiqueIndexable ppIndexable = null;
		if (pp != null && !pp.isHabitant()) {
			ppIndexable = new NonHabitantIndexable(adresseService, tiersService, serviceInfra, pp);
		}
		else if (pp != null) {
			Individu ind = serviceCivil.getIndividu(pp.getNumeroIndividu(), DateHelper.getCurrentYear());
			ppIndexable = new HabitantIndexable(adresseService, tiersService, serviceInfra, pp, ind);
		}
		return ppIndexable;
	}

	/**
	 * @see ch.vd.uniregctb.indexer.Indexable#getSubType()
	 */
	public String getSubType() {
		return SUB_TYPE;
	}

	@Override
	public HashMap<String, String> getKeyValues() throws IndexerException {

		HashMap<String, String> values = super.getKeyValues();

		// HashMap<String, String> subValues = menageSubIndexable.getKeyValues();
		if (ppIndexable1 != null) {
			HashMap<String, String> hab1SubValues = ppIndexable1.getKeyValues();
			// Search
			// Hab1 - Ind1
			// addValueToMap(values, TiersSearchFields.NUMEROS, hab1SubValues, TiersSearchFields.NUMEROS);
			addValueToMap(values, TiersSearchFields.DATE_NAISSANCE, hab1SubValues, TiersSearchFields.DATE_NAISSANCE);
			addValueToMap(values, TiersSearchFields.NOM_RAISON, hab1SubValues, TiersSearchFields.NOM_RAISON);
			addValueToMap(values, TiersSearchFields.AUTRES_NOM, hab1SubValues, TiersSearchFields.AUTRES_NOM);
			addValueToMap(values, TiersSearchFields.NUMERO_ASSURE_SOCIAL, hab1SubValues, TiersSearchFields.NUMERO_ASSURE_SOCIAL);
			// Display
			values.put(TiersIndexedData.NOM1, hab1SubValues.get(TiersIndexedData.NOM1));
		}
		if (ppIndexable2 != null) {
			HashMap<String, String> hab2SubValues = ppIndexable2.getKeyValues();
			// Hab2 - Ind2
			// addValueToMap(values, TiersSearchFields.NUMEROS, hab2SubValues, TiersSearchFields.NUMEROS);
			addValueToMap(values, TiersSearchFields.DATE_NAISSANCE, hab2SubValues, TiersSearchFields.DATE_NAISSANCE);
			addValueToMap(values, TiersSearchFields.NOM_RAISON, hab2SubValues, TiersSearchFields.NOM_RAISON);
			addValueToMap(values, TiersSearchFields.AUTRES_NOM, hab2SubValues, TiersSearchFields.AUTRES_NOM);
			addValueToMap(values, TiersSearchFields.NUMERO_ASSURE_SOCIAL, hab2SubValues, TiersSearchFields.NUMERO_ASSURE_SOCIAL);
			// Display
			values.put(TiersIndexedData.NOM2, hab2SubValues.get(TiersIndexedData.NOM1));
		}

		// debugDumpValues(values);

		return values;
	}

}
