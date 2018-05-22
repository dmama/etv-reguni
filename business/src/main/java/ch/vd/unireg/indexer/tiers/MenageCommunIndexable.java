package ch.vd.unireg.indexer.tiers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.avatar.AvatarService;
import ch.vd.unireg.indexer.IndexerException;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.service.ServiceCivilService;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.IndividuNotFoundException;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.tiers.TiersService;

/**
 * Objet permettant d'indexer des contribuables ménage commun.
 *
 * @author Akram BEN AISSI <mailto:akram.ben-aissi@vd.ch>
 */
public class MenageCommunIndexable extends ContribuableImpositionPersonnesPhysiquesIndexable<MenageCommun> {

	// private final Logger LOGGER = LoggerFactory.getLogger(MenageCommunIndexable.class);

	private final PersonnePhysiqueIndexable ppIndexable1;
	private final PersonnePhysiqueIndexable ppIndexable2;

	public static final String SUB_TYPE = "menagecommun";

	public MenageCommunIndexable(AdresseService adresseService, TiersService tiersService, AssujettissementService assujettissementService, ServiceCivilService serviceCivil,
	                             ServiceInfrastructureService serviceInfra, AvatarService avatarService, MenageCommun menage) throws IndexerException {
		super(adresseService, tiersService, assujettissementService, serviceInfra, avatarService, menage);

		final EnsembleTiersCouple ensemble = extractEnsembleForIndexation(tiersService, menage);
		ppIndexable1 = getPPIndexable(adresseService, tiersService, assujettissementService, serviceCivil, serviceInfra, avatarService, ensemble.getPrincipal());
		if (ensemble.getConjoint() != null) {
			ppIndexable2 = getPPIndexable(adresseService, tiersService, assujettissementService, serviceCivil, serviceInfra, avatarService, ensemble.getConjoint());
		}
		else {
			ppIndexable2 = null;//marié seul => pas d'indexation du conjoint
		}
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
		if (!personnes.isEmpty()) {
			final Set<PersonnePhysique> nonAnnules = new HashSet<>(2);
			for (Map.Entry<PersonnePhysique, RapportEntreTiers> entry : personnes.entrySet()) {
				if (!entry.getValue().isAnnule()) {
					nonAnnules.add(entry.getKey());
				}
			}
			if (nonAnnules.size() > 2) {
				throw new IllegalArgumentException("Plus de deux personnes avec lien non-annulé dans le ménage " + menage.getNumero());
			}

			// s'il y a des liens non-annulés, on ne va pas chercher plus loin
			if (!nonAnnules.isEmpty()) {
				personnesPhysiques = nonAnnules;
			}
			else {
				// tous les liens sur ce ménage sont annulés
				final List<Map.Entry<PersonnePhysique, RapportEntreTiers>> aPrendre = new ArrayList<>(personnes.entrySet());
				if (aPrendre.size() > 1) {
					aPrendre.sort(new Comparator<Map.Entry<PersonnePhysique, RapportEntreTiers>>() {
						@Override
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
				personnesPhysiques = new HashSet<>(2);
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

	private static PersonnePhysiqueIndexable getPPIndexable(AdresseService adresseService, TiersService tiersService, AssujettissementService assujettissementService, ServiceCivilService serviceCivil, ServiceInfrastructureService serviceInfra,
	                                                        AvatarService avatarService, PersonnePhysique pp) {
		PersonnePhysiqueIndexable ppIndexable = null;
		if (pp != null && !pp.isHabitantVD()) {
			ppIndexable = new NonHabitantIndexable(adresseService, tiersService, assujettissementService, serviceInfra, avatarService, pp);
		}
		else if (pp != null) {
			final Individu ind = serviceCivil.getIndividu(pp.getNumeroIndividu(), null, AttributeIndividu.ADRESSES);
			if (ind == null) {
				throw new IndividuNotFoundException(pp);
			}
			ppIndexable = new HabitantIndexable(adresseService, tiersService, assujettissementService, serviceInfra, avatarService, pp, ind);
		}
		return ppIndexable;
	}

	@Override
	public String getSubType() {
		return SUB_TYPE;
	}

	@Override
	protected void fillBaseData(TiersIndexableData data) {
		super.fillBaseData(data);

		if (ppIndexable1 != null) {
			final TiersIndexableData ppData = (TiersIndexableData) ppIndexable1.getIndexableData();

			data.addNomRaison(ppData.getNomRaison());
			data.addAutresNom(ppData.getAutresNom());
			data.addDatesNaissance(ppData.getDatesNaissanceInscriptionRC()); // [UNIREG-2633] on pouvoir rechercher par dates de naissance sur les ménages communs
			data.addNavs11(ppData.getNavs11());
			data.addNavs13(ppData.getNavs13());
			data.setNom1(ppData.getNom1());
			data.setNavs13_1(ppData.getNavs13_1());
		}

		if (ppIndexable2 != null) {
			final TiersIndexableData ppData = (TiersIndexableData) ppIndexable2.getIndexableData();

			data.addNomRaison(ppData.getNomRaison());
			data.addAutresNom(ppData.getAutresNom());
			data.addDatesNaissance(ppData.getDatesNaissanceInscriptionRC()); // [UNIREG-2633] on pouvoir rechercher par dates de naissance sur les ménages communs
			data.addNavs11(ppData.getNavs11());
			data.addNavs13(ppData.getNavs13());
			data.setNom2(ppData.getNom1());
			data.setNavs13_2(ppData.getNavs13_1());
		}
	}
}
