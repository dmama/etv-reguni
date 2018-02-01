package ch.vd.unireg.registrefoncier.dataimport.processor;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.common.HibernateDateRangeEntity;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
import ch.vd.unireg.registrefoncier.AyantDroitRF;
import ch.vd.unireg.registrefoncier.CommunauteRF;
import ch.vd.unireg.registrefoncier.DroitProprietePersonneRF;
import ch.vd.unireg.registrefoncier.DroitProprieteRF;
import ch.vd.unireg.registrefoncier.DroitRF;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.ModeleCommunauteRF;
import ch.vd.unireg.registrefoncier.RegistreFoncierService;
import ch.vd.unireg.registrefoncier.RegroupementCommunauteRF;

/**
 * Processeur qui recalcule l'état d'une communauté après des changements sur les droits.
 */
public class CommunauteRFProcessor {

	private final Function<Set<? extends AyantDroitRF>, ModeleCommunauteRF> modeleCommunauteProvider;
	private final Function<CommunauteRF, Long> communautePrincipalIdProvider;
	private final EvenementFiscalService evenementFiscalService;

	public CommunauteRFProcessor(@NotNull RegistreFoncierService registreFoncierService, @NotNull EvenementFiscalService evenementFiscalService) {
		this.modeleCommunauteProvider = registreFoncierService::findOrCreateModeleCommunaute;
		this.communautePrincipalIdProvider = registreFoncierService::getCommunauteCurrentPrincipalId;
		this.evenementFiscalService = evenementFiscalService;
	}

	public CommunauteRFProcessor(@NotNull Function<Set<? extends AyantDroitRF>, ModeleCommunauteRF> modeleCommunauteProvider,
	                             @NotNull Function<CommunauteRF, Long> communautePrincipalIdProvider,
	                             @NotNull EvenementFiscalService evenementFiscalService) {
		this.modeleCommunauteProvider = modeleCommunauteProvider;
		this.communautePrincipalIdProvider = communautePrincipalIdProvider;
		this.evenementFiscalService = evenementFiscalService;
	}

	/**
	 * Recalcule les regroupements sur toutes les communautés de l'immeuble spécifié.
	 *
	 * @param immeuble un immeuble
	 */
	public void processAll(@NotNull ImmeubleRF immeuble) {
		final Set<CommunauteRF> communautes = immeuble.getDroitsPropriete().stream()
				// on s'intéresse aussi aux droits annulés (car les communautés correspondantes ne le sont pas forcément) : .filter(AnnulableHelper::nonAnnule)
				.filter(DroitProprietePersonneRF.class::isInstance)
				.map(DroitProprietePersonneRF.class::cast)
				.map(DroitProprietePersonneRF::getCommunaute)
				.filter(Objects::nonNull)
				.filter(AnnulableHelper::nonAnnule)
				.collect(Collectors.toSet());
		communautes.forEach(this::process);
	}

	/**
	 * Recalcule les regroupements sur toutes les communautés de tous les immeubles de l'ayant-droit spécifié.
	 *
	 * @param ayantDroit un ayant-droit
	 */
	public void processAll(@NotNull AyantDroitRF ayantDroit) {
		ayantDroit.getDroitsPropriete().stream()
				.map(DroitProprieteRF::getImmeuble)
				.distinct()
				.forEach(this::processAll);
	}

	/**
	 * Recalcule les regroupements et met-à-jour la communauté si nécessaire.
	 *
	 * @param communaute une communauté
	 * @return <i>true</i> si la communauté a été modifiée; <i>false</i> autrement.
	 */
	public boolean process(@NotNull CommunauteRF communaute) {

		// on détermine le principal actuel
		final Long principalId = communautePrincipalIdProvider.apply(communaute);

		// les regroupements persistés
		final Set<RegroupementCommunauteRF> persistes = communaute.getRegroupements().stream()
				.filter(AnnulableHelper::nonAnnule)
				.collect(Collectors.toSet());

		// les regroupements théoriques (= la nouvelle référence)
		final Set<RegroupementCommunauteRF> theoriques = calculateRegroupements(communaute);

		// on enlève des collections tous les éléments égaux
		CollectionsUtils.removeCommonElements(persistes, theoriques, this::regroupementEquals);

		// on détermine les changements qui ne concernent que des regroupements à fermer
		final List<Pair<RegroupementCommunauteRF, RegroupementCommunauteRF>> fermes = CollectionsUtils.extractCommonElements(persistes, theoriques, this::regroupementEqualsSaufDateFinNouvellementFermee);

		// ce qui reste dans la collection 'persistes' est en trop, on l'annule
		persistes.forEach(r -> r.setAnnule(true));

		// on ferme les regroupements à fermer
		fermes.forEach(p -> fermeRegroupement(p.getFirst(), p.getSecond().getDateFin()));

		// ce qui reste dans la collection 'theoriques' manque, on l'ajoute
		theoriques.forEach(communaute::addRegroupement);

		// on détermine le nouveau principal
		final Long nouveauPrincipalId = communautePrincipalIdProvider.apply(communaute);

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

	private boolean regroupementEqualsSaufDateFinNouvellementFermee(@NotNull RegroupementCommunauteRF oldReg, @NotNull RegroupementCommunauteRF newReg) {
		return oldReg.getDateDebut() == newReg.getDateDebut() &&
				oldReg.getCommunaute() == newReg.getCommunaute() &&
				oldReg.getModele() == newReg.getModele() &&
				oldReg.getDateFin() == null &&
				newReg.getDateFin() != null;
	}

	/**
	 * Calcule les regroupements théoriques pour une communauté
	 *
	 * @param communaute une communauté
	 * @return ces regroupements théoriques
	 */
	@NotNull
	private Set<RegroupementCommunauteRF> calculateRegroupements(@NotNull CommunauteRF communaute) {

		final List<DateRange> droits = communaute.getMembres().stream()
				.filter(AnnulableHelper::nonAnnule)
				.map(DroitRF::getRangeMetier)
				.collect(Collectors.toList());

		if (droits.isEmpty()) {
			// pas de droit, pas de regroupement
			return Collections.emptySet();
		}

		// [SIFISC-27517] Dans le cas où un membre est remplacé par un autre membre au sein d'une communauté, on
		// recule de un jour la date de fin du membre remplacé de telle manière que les deux membres
		// ne se retrouvent pas en même temps dans la communauté
		//
		// Exemple : communauté n°189976085
		//
		//  D1:  12.07.1999 |------------------------------------------
		//
		//  D2:  12.07.1999 |----------------| 19.12.2008
		//
		//  D3:                   19.12.2008 |-------------------------
		//
		//  droits corrigés
		//
		//  D1:  12.07.1999 |------------------------------------------
		//
		//  D2:  12.07.1999 |---------------| 18.12.2008 (-1 jour)
		//
		//  D3:                   19.12.2008 |-------------------------

		final Set<RegDate> autresDatesDebut = droits.stream()
				.map(DateRange::getDateDebut)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		for (int i = 0; i < droits.size(); i++) {
			final DateRange droit = droits.get(i);
			final RegDate dateDebut = droit.getDateDebut();
			final RegDate dateFin = droit.getDateFin();
			if (dateFin != null && (dateDebut == null || dateFin.isAfter(dateDebut)) && autresDatesDebut.contains(dateFin)) {
				// le droit courant s'arrête le même jour qu'un autre droit qui débute
				// => on recule de un jour la date de fin
				final DateRange droitCorrige = new DateRangeHelper.Range(dateDebut, dateFin.getOneDayBefore());
				droits.set(i, droitCorrige);
			}
		}

		// On détermine les périodes où les droits des membres de la communauté sont constants
		//
		//  Exemple de droits sur une communauté :
		//
		//  D1:  02.05.2010 |------------------------------------------------------------------...
		//
		//  D2:  02.05.2010 |----------------| 17.05.2012
		//
		//  D3:  02.05.2010 |------------------| 18.05.2012
		//
		//  D4:       13.06.2010 |-------------------------------------------| 04.12.2013
		//
		//  D5:  02.05.2010 |------------------------------------------------------------------...
		//
		//  Périodes de composition constante des membres :
		//
		//  P1:  02.05.2010 |----| 12.06.2010
		//
		//  P2:  13.06.2010      |-----------| 17.05.2012
		//
		//  P2:                   18.05.2012 |-| 18.05.2012
		//
		//  P3:                     19.05.2012 |---------------------------------| 04.12.2014
		//
		//  P4:                                                       05.12.2014 |-------------...
		//
		final List<DateRange> periodesConstantes = DateRangeHelper.projectRanges(droits);

		final Set<RegroupementCommunauteRF> regroupements = new HashSet<>();

		// on boucle sur toutes les périodes
		for (final DateRange periode : periodesConstantes) {

			// on cherche les membres valides pour la période
			final Set<AyantDroitRF> membresValides = communaute.getMembres().stream()
					.filter(AnnulableHelper::nonAnnule)
					.filter(d -> d.getRangeMetier().isValidAt(periode.getDateFin()))
					.map(DroitProprieteRF::getAyantDroit)
					.collect(Collectors.toSet());

			if (!membresValides.isEmpty()) {
				// on demande le modèle de communauté correspond
				final ModeleCommunauteRF modele = modeleCommunauteProvider.apply(membresValides);

				// on créé le regroupement qui va bien
				RegroupementCommunauteRF regroupement = new RegroupementCommunauteRF();
				regroupement.setDateDebut(periode.getDateDebut());
				regroupement.setDateFin(periode.getDateFin());
				regroupement.setCommunaute(communaute);
				regroupement.setModele(modele);
				regroupements.add(regroupement);
			}
		}

		return regroupements;
	}
}
