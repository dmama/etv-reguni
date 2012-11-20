package ch.vd.uniregctb.fors;

import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.validation.Errors;

import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.TiersNotFoundException;
import ch.vd.uniregctb.tiers.ForFiscalRevenuFortune;
import ch.vd.uniregctb.tiers.NatureTiers;
import ch.vd.uniregctb.tiers.Tiers;
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

		final Tiers tiers = hibernateTemplate.get(Tiers.class, view.getTiersId());
		if (tiers == null) {
			throw new TiersNotFoundException(view.getTiersId());
		}

		// validation du motif de fin
		if (view.getDateFin() != null) {
			if (view.getMotifFin() == null) {
				errors.rejectValue("motifFin", "error.motif.fermeture.vide");
			}
			else {
				final ForFiscalRevenuFortune ffrf = hibernateTemplate.get(ForFiscalRevenuFortune.class, view.getId());
				if (ffrf == null) {
					throw new ObjectNotFoundException("Impossible de trouver le for fiscal avec l'id=" + view.getId());
				}

				final NatureTiers natureTiers = tiers.getNatureTiers();
				final MotifsForHelper.TypeFor typeFor = new MotifsForHelper.TypeFor(natureTiers, GenreImpot.REVENU_FORTUNE, ffrf.getMotifRattachement());

				if (!MotifsForHelper.getMotifsFermeture(typeFor).contains(view.getMotifFin())) {
					errors.rejectValue("motifFin", "Motif fermeture invalide");
				}
			}
		}
	}
}
