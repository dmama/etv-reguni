package ch.vd.unireg.validation.registrefoncier;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.registrefoncier.AyantDroitRF;
import ch.vd.unireg.registrefoncier.CommunauteRF;
import ch.vd.unireg.registrefoncier.DroitProprieteRF;
import ch.vd.unireg.registrefoncier.GenrePropriete;
import ch.vd.unireg.registrefoncier.ImmeubleBeneficiaireRF;
import ch.vd.unireg.registrefoncier.RaisonAcquisitionRF;
import ch.vd.unireg.registrefoncier.TiersRF;

@SuppressWarnings("Duplicates")
public class DroitProprieteRFValidator extends DroitRFValidator<DroitProprieteRF> {

	private static final Set<GenrePropriete> GENRE_PROPRIETES_TIERS = EnumSet.of(GenrePropriete.INDIVIDUELLE, GenrePropriete.COPROPRIETE, GenrePropriete.COMMUNE);
	private static final Set<GenrePropriete> GENRE_PROPRIETES_IMMEUBLES = EnumSet.of(GenrePropriete.COPROPRIETE, GenrePropriete.FONDS_DOMINANT, GenrePropriete.PPE);

	@Override
	protected Class<DroitProprieteRF> getValidatedClass() {
		return DroitProprieteRF.class;
	}

	@Override
	protected String getEntityCategoryName() {
		return "Le droit de propriété RF";
	}

	@Override
	@NotNull
	public ValidationResults validate(DroitProprieteRF entity) {
		final ValidationResults results = super.validate(entity);

		// une entité annulée est toujours valide...
		if (entity.isAnnule()) {
			return results;
		}

		// [SIFISC-23894] la date de début métier et le motif de début doit correspondre à la première valeur dans l'historique des raisons d'acquisition
		final Set<RaisonAcquisitionRF> raisons = entity.getRaisonsAcquisition();
		if (raisons != null) {  // note: si la collection n'est pas renseignée, c'est qu'on est dans la première phase (save) : on attend la seconde phase (update) pour vérifier la cohérence.
			final RaisonAcquisitionRF premiereRaison = raisons.stream()
					.filter(AnnulableHelper::nonAnnule)
					.min(Comparator.naturalOrder())
					.orElse(null);

			if (premiereRaison == null) {
				if (entity.getDateDebutMetier() != null) {
					results.addWarning(String.format("%s %s possède une date de début métier renseignée (%s) alors qu'il n'y a pas de raison d'acquisition",
					                               getEntityCategoryName(),
					                               getEntityDisplayString(entity),
					                               RegDateHelper.dateToDisplayString(entity.getDateDebutMetier())));
				}
				if (StringUtils.isNotBlank(entity.getMotifDebut())) {
					results.addWarning(String.format("%s %s possède un motif de début métier renseigné (%s) alors qu'il n'y a pas de raison d'acquisition",
					                               getEntityCategoryName(),
					                               getEntityDisplayString(entity),
					                               entity.getMotifDebut()));
				}
			}
			else {
				if (entity.getDateDebutMetier() != premiereRaison.getDateAcquisition() || !Objects.equals(entity.getMotifDebut(), premiereRaison.getMotifAcquisition())) {
					// [SIFISC-24987] Il arrive dans certains cas qu'un droit de propriété évolue (même masterIdRF mais versionIdRF différent entre deux imports),
					//                à ce moment-là, la date et le motif de début ne sont pas forcément tirés de la première raison d'acquisition. Il n'est pas
					//                possible de déterminer ici (= il manque le context) la raison d'acquisition précise, alors on vérifie au minimum que la
					//                date et le motif de début sont bien tirés d'une des raisons d'acquisition.
					if (raisons.stream()
							.filter(AnnulableHelper::nonAnnule)
							.noneMatch(r -> entity.getDateDebutMetier() == r.getDateAcquisition() && Objects.equals(entity.getMotifDebut(), r.getMotifAcquisition()))) {
						results.addWarning(String.format("%s %s possède une date de début métier (%s) et un motif (%s) qui ne correspondent à aucune des raisons d'acquisition",
						                               getEntityCategoryName(),
						                               getEntityDisplayString(entity),
						                               RegDateHelper.dateToDisplayString(entity.getDateDebutMetier()),
						                               entity.getMotifDebut()));
					}
				}
			}
		}

		// [SIFISC-23895] les régimes de propriété dépendent du type de propriétaire
		final AyantDroitRF ayantDroit = entity.getAyantDroit();
		if (ayantDroit == null) {
			results.addError("Le droit masterIdRF=[" + entity.getMasterIdRF() + "] versionIdRF=[" + entity.getVersionIdRF()+ "] ne possède pas d'ayant-droit");
		}
		else {
			final Set<GenrePropriete> genreProprietesAutorises;
			if (ayantDroit instanceof TiersRF || ayantDroit instanceof CommunauteRF) {
				genreProprietesAutorises = GENRE_PROPRIETES_TIERS;
			}
			else if (ayantDroit instanceof ImmeubleBeneficiaireRF) {
				genreProprietesAutorises = GENRE_PROPRIETES_IMMEUBLES;
			}
			else {
				throw new IllegalArgumentException("Type d'ayant-droit inconnu = [" + ayantDroit.getClass().getSimpleName() + "]");
			}
			if (!genreProprietesAutorises.contains(entity.getRegime())) {
				results.addError("Le droit masterIdRF=[" + entity.getMasterIdRF() + "] versionIdRF=[" + entity.getVersionIdRF() + "] " +
						                 "sur le tiers RF (" + ayantDroit.getClass().getSimpleName() + ") idRF=[" + ayantDroit.getIdRF() + "] " +
						                 "possède un régime de propriété [" + entity.getRegime() + "] invalide");
			}
		}

		return results;
	}
}
