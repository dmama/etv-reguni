package ch.vd.uniregctb.fors;

import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.validation.Errors;

import ch.vd.uniregctb.common.TiersNotFoundException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.NatureTiers;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.validator.MotifsForHelper;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public abstract class AddForRevenuFortuneValidator extends AddForValidator {

	private HibernateTemplate hibernateTemplate;

	protected AddForRevenuFortuneValidator(ServiceInfrastructureService infraService, HibernateTemplate hibernateTemplate) {
		super(infraService);
		this.hibernateTemplate = hibernateTemplate;
	}

	@Override
	public void validate(Object target, Errors errors) {
		super.validate(target, errors);

		final AddForRevenuFortuneView view =(AddForRevenuFortuneView) target;

		final Tiers tiers = hibernateTemplate.get(Tiers.class, view.getTiersId());
		if (tiers == null) {
			throw new TiersNotFoundException(view.getTiersId());
		}

		// validation du motif de début
		if (view.getDateDebut() != null) {
			if (view.getMotifDebut() == null) {
				boolean allowEmptyMotif = view.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC || view.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS;
				if (allowEmptyMotif) {
					// [SIFISC-4065] On n'autorise les motifs d'ouverture vide que s'il n'y a pas de for principal non annulé existant avant le for que l'on veut créer maintenant
					allowEmptyMotif = tiers.getDernierForFiscalPrincipalAvant(view.getDateDebut()) == null;
				}

				if (!allowEmptyMotif) {
					errors.rejectValue("motifDebut", "error.motif.ouverture.vide");
				}
			}
			else {
				final NatureTiers natureTiers = tiers.getNatureTiers();
				final MotifsForHelper.TypeFor typeFor = new MotifsForHelper.TypeFor(natureTiers, GenreImpot.REVENU_FORTUNE, view.getMotifRattachement());

				if (!MotifsForHelper.getMotifsOuverture(typeFor).contains(view.getMotifDebut())) {
					errors.rejectValue("motifDebut", "error.motif.ouverture.invalide");
				}
			}
		}

		// validation du motif de fin
		if (view.getDateFin() != null) {
			if (view.getMotifFin() == null) {
				errors.rejectValue("motifFin", "error.motif.fermeture.vide");
			}
			else {
				final NatureTiers natureTiers = tiers.getNatureTiers();
				final MotifsForHelper.TypeFor typeFor = new MotifsForHelper.TypeFor(natureTiers, GenreImpot.REVENU_FORTUNE, view.getMotifRattachement());

				if (!MotifsForHelper.getMotifsFermeture(typeFor).contains(view.getMotifFin())) {
					errors.rejectValue("motifFin", "Motif fermeture invalide");
				}
			}
		}

		// le mode de rattachement
		if (view.getMotifRattachement() == null) {
			errors.rejectValue("motifRattachement", "error.motif.rattachement.vide");
		}
	}
}
