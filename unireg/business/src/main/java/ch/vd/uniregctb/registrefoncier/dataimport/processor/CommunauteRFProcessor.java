package ch.vd.uniregctb.registrefoncier.dataimport.processor;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.HibernateDateRangeEntity;
import ch.vd.uniregctb.common.ProgrammingException;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRFMembreInfo;
import ch.vd.uniregctb.registrefoncier.DroitProprieteRF;
import ch.vd.uniregctb.registrefoncier.DroitRF;
import ch.vd.uniregctb.registrefoncier.ModeleCommunauteRF;
import ch.vd.uniregctb.registrefoncier.RegistreFoncierService;
import ch.vd.uniregctb.registrefoncier.RegroupementCommunauteRF;

/**
 * Processeur qui recalcule l'état d'une communauté après des changements sur les droits.
 */
public class CommunauteRFProcessor {

	private final Function<Set<? extends AyantDroitRF>, ModeleCommunauteRF> modeleCommunauteProvider;
	private final Function<CommunauteRF, CommunauteRFMembreInfo> communauteMembreInfoProvider;
	private final EvenementFiscalService evenementFiscalService;

	public CommunauteRFProcessor(@NotNull RegistreFoncierService registreFoncierService, @NotNull EvenementFiscalService evenementFiscalService) {
		this.modeleCommunauteProvider = registreFoncierService::findOrCreateModeleCommunaute;
		this.communauteMembreInfoProvider = registreFoncierService::getCommunauteMembreInfo;
		this.evenementFiscalService = evenementFiscalService;
	}

	public CommunauteRFProcessor(@NotNull Function<Set<? extends AyantDroitRF>, ModeleCommunauteRF> modeleCommunauteProvider,
	                             @NotNull Function<CommunauteRF, CommunauteRFMembreInfo> communauteMembreInfoProvider,
	                             @NotNull EvenementFiscalService evenementFiscalService) {
		this.modeleCommunauteProvider = modeleCommunauteProvider;
		this.communauteMembreInfoProvider = communauteMembreInfoProvider;
		this.evenementFiscalService = evenementFiscalService;
	}

	/**
	 * Recalcule les regroupements et met-à-jour la communauté si nécessaire.
	 *
	 * @param communaute une communauté
	 * @return <i>true</i> si la communauté a été modifiée; <i>false</i> autrement.
	 */
	public boolean process(@NotNull CommunauteRF communaute) {

		// on détermine le principal actuel
		final Long principalId = communauteMembreInfoProvider.apply(communaute).getCtbIds().stream().findFirst().orElse(null);

		// les regroupements persistés
		final Set<RegroupementCommunauteRF> persistes = communaute.getRegroupements().stream()
				.filter(AnnulableHelper::nonAnnule)
				.collect(Collectors.toSet());

		// les regroupements théoriques (= la nouvelle référence)
		final Set<RegroupementCommunauteRF> theoriques = calculateRegroupements(communaute);

		// on enlève des collections tous les éléments égaux
		CollectionsUtils.removeCommonElements(persistes, theoriques, this::regroupementEquals);

		// on détermine les changements qui ne concernent que des regroupements à fermer
		final List<Pair<RegroupementCommunauteRF, RegroupementCommunauteRF>> fermes = CollectionsUtils.extractCommonElements(persistes, theoriques, this::regroupementEqualsSaufDateFin);

		// ce qui reste dans la collection 'persistes' est en trop, on l'annule
		persistes.forEach(r -> r.setAnnule(true));

		// on ferme les regroupements à fermer
		fermes.forEach(p -> fermeRegroupement(p.getFirst(), p.getSecond().getDateFin()));

		// ce qui reste dans la collection 'theoriques' manque, on l'ajoute
		theoriques.forEach(communaute::addRegroupement);

		// on détermine le nouveau principal
		final Long nouveauPrincipalId = communauteMembreInfoProvider.apply(communaute).getCtbIds().stream().findFirst().orElse(null);

		// si le principal de communauté a changé, on publie un événement correspondant
		if (!Objects.equals(principalId, nouveauPrincipalId)) {
			final RegDate dateDebut = communaute.getRegroupements().stream()
					.filter(r -> r.isValidAt(null))
					.map(RegroupementCommunauteRF::getModele)
					.map(ModeleCommunauteRF::getPrincipaux)
					.filter(Objects::nonNull)
					.flatMap(Collection::stream)
					.filter(p -> p.isValidAt(null))
					.findFirst()
					.map(HibernateDateRangeEntity::getDateDebut)
					.orElse(null);  // s'il n'y a pas d'élection explicite du principal, on retourne une date nulle car l'algorithme de tri des membres est trop compliqué pour déterminer une date métier
			evenementFiscalService.publierModificationPrincipalCommunaute(dateDebut, communaute);
		}

		return !persistes.isEmpty() || !theoriques.isEmpty();
	}

	private void fermeRegroupement(@NotNull RegroupementCommunauteRF regroupement, RegDate dateFin) {
		if (dateFin == null) {
			throw new IllegalArgumentException("La date de fin est nulle");
		}
		regroupement.setDateFin(dateFin);
	}

	private boolean regroupementEquals(@NotNull RegroupementCommunauteRF r1, @NotNull RegroupementCommunauteRF r2) {
		return r1.getDateDebut() == r2.getDateDebut() &&
				r1.getDateFin() == r2.getDateFin() &&
				r1.getCommunaute() == r2.getCommunaute() &&
				r1.getModele() == r2.getModele();
	}

	private boolean regroupementEqualsSaufDateFin(@NotNull RegroupementCommunauteRF r1, @NotNull RegroupementCommunauteRF r2) {
		return r1.getDateDebut() == r2.getDateDebut() &&
				r1.getCommunaute() == r2.getCommunaute() &&
				r1.getModele() == r2.getModele();
	}

	/**
	 * Calcule les regroupements théoriques pour une communauté
	 *
	 * @param communaute une communauté
	 * @return ces regroupements théoriques
	 */
	@NotNull
	private Set<RegroupementCommunauteRF> calculateRegroupements(@NotNull CommunauteRF communaute) {

		final Set<DroitProprieteRF> droits = communaute.getMembres().stream()
				.filter(AnnulableHelper::nonAnnule)
				.collect(Collectors.toSet());

		if (droits.isEmpty()) {
			// pas de droit, pas de regroupement
			return Collections.emptySet();
		}

		// on recherche la date de début métier de la communauté (normalement, tous les droits d'une communauté
		// sont créés en même temps lors de sa création : ils possèdent donc tous la même date de début métier.
		// Cependant, il arrive que des oublis ou des erreurs de saisie provoquent des incohérences, dans ce cas,
		// on prend la date métier la plus petite)
		RegDate dateDebutMetier = RegDateHelper.getLateDate();
		for (DroitProprieteRF droit : droits) {
			if (NullDateBehavior.EARLIEST.compare(droit.getDateDebutMetier(), dateDebutMetier) < 0) {
				dateDebutMetier = droit.getDateDebutMetier();
			}
		}

		// on recherche toutes les dates de fin des droits des membres de la communauté (à l'inverse de leurs
		// création, les droits des membres d'une communauté peuvent s'arrêter à des dates différentes)
		final List<RegDate> datesFinMetier = droits.stream()
				.map(DroitRF::getDateFinMetier)
				.distinct()
				.sorted(NullDateBehavior.LATEST::compare)
				.collect(Collectors.toList());

		//
		//  Exemple de droits sur une communauté :
		//
		//  D1:  02.05.2010 |------------------------------------------------------------------...
		//
		//  D2:  02.05.2010 |---------| 17.05.2012
		//
		//  D3:  02.05.2010 |----------| 18.05.2012
		//
		//  D4:    13.05.2010 |-------------------------------------------| 04.12.2013
		//
		//  D5:  02.05.2010 |------------------------------------------------------------------...
		//
		//  Périodes de composition constante des membres :
		//
		//  P1:  02.05.2010 |---------| 17.05.2012
		//
		//  P2:            18.05.2012 |-| 18.05.2012
		//
		//  P3:              19.05.2012 |---------------------------------| 04.12.2014
		//
		//  P4:                                                05.12.2014 |--------------------...
		//

		if (datesFinMetier.isEmpty()) {
			// si on arrive là, c'est que l'algo est foireux
			throw new ProgrammingException("Aucune date de fin trouvée");
		}

		final Set<RegroupementCommunauteRF> regroupements = new HashSet<>();

		// on boucle sur toutes les périodes où la composition des membres de la communauté est constante
		RegDate dateDebut = dateDebutMetier;
		for (final RegDate dateFin : datesFinMetier) {

			// on cherche les membres valides pour la période
			final Set<AyantDroitRF> membresValides = communaute.getMembres().stream()
					.filter(AnnulableHelper::nonAnnule)
					.filter(d -> d.getRangeMetier().isValidAt(dateFin))
					.map(DroitProprieteRF::getAyantDroit)
					.collect(Collectors.toSet());

			if (!membresValides.isEmpty()) {
				// on demande le modèle de communauté correspond
				final ModeleCommunauteRF modele = modeleCommunauteProvider.apply(membresValides);

				// on créé le regroupement qui va bien
				RegroupementCommunauteRF regroupement = new RegroupementCommunauteRF();
				regroupement.setDateDebut(dateDebut);
				regroupement.setDateFin(dateFin);
				regroupement.setCommunaute(communaute);
				regroupement.setModele(modele);
				regroupements.add(regroupement);
			}

			// période suivante
			dateDebut = (dateFin == null ? null : dateFin.getOneDayAfter());
		}

		return regroupements;
	}
}
