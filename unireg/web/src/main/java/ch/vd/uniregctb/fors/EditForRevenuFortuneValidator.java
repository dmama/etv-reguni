package ch.vd.uniregctb.fors;

import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.validation.Errors;

import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.tiers.ForFiscalRevenuFortune;
import ch.vd.uniregctb.tiers.NatureTiers;
import ch.vd.uniregctb.tiers.validator.MotifsForHelper;
import ch.vd.uniregctb.type.GenreImpot;

public abstract class EditForRevenuFortuneValidator extends EditForValidator {

	private HibernateTemplate hibernateTemplate;

	protected EditForRevenuFortuneValidator(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	@Override
	public void validate(Object target, Errors errors) {
		super.validate(target, errors);

		final EditForRevenuFortuneView view = (EditForRevenuFortuneView) target;

		final ForFiscalRevenuFortune ffrf = hibernateTemplate.get(ForFiscalRevenuFortune.class, view.getId());
		if (ffrf == null) {
			throw new ObjectNotFoundException("Impossible de trouver le for fiscal avec l'id=" + view.getId());
		}

		// validation du motif de debut
		if (view.getDateDebut() != null) {
			if (view.getMotifDebut() == null) {
				errors.rejectValue("motifDebut", "error.motif.ouverture.vide");
			}
			else {
				final NatureTiers natureTiers = ffrf.getTiers().getNatureTiers();
				final MotifsForHelper.TypeFor typeFor = new MotifsForHelper.TypeFor(natureTiers, GenreImpot.REVENU_FORTUNE, ffrf.getMotifRattachement());

				if (!MotifsForHelper.getMotifsOuverture(typeFor).contains(view.getMotifDebut())) {
					errors.rejectValue("motifDebut", "Motif ouverture invalide");
				}
			}
		}
		else if (view.getMotifDebut() != null) { // [SIFISC-7381] les dates et motifs doivent être renseignés tous les deux, ou nuls tous les deux
			errors.rejectValue("dateDebut", "error.date.ouverture.vide");
		}

		// validation du motif de fin
		if (view.getDateFin() != null) {
			if (view.getMotifFin() == null) {
				errors.rejectValue("motifFin", "error.motif.fermeture.vide");
			}
			else {
				final NatureTiers natureTiers = ffrf.getTiers().getNatureTiers();
				final MotifsForHelper.TypeFor typeFor = new MotifsForHelper.TypeFor(natureTiers, GenreImpot.REVENU_FORTUNE, ffrf.getMotifRattachement());

				if (!MotifsForHelper.getMotifsFermeture(typeFor).contains(view.getMotifFin())) {
					errors.rejectValue("motifFin", "Motif fermeture invalide");
				}
			}
		}
		else if (view.getMotifFin() != null) { // [SIFISC-7381] les dates et motifs doivent être renseignés tous les deux, ou nuls tous les deux
			errors.rejectValue("dateFin", "error.date.fermeture.vide");
		}
	}
}
