package ch.vd.unireg.supergra.validators;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.unireg.common.TiersNotFoundException;
import ch.vd.unireg.supergra.view.Pp2McView;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;

public class Pp2McValidator implements Validator {

	private TiersDAO tiersDAO;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return Pp2McView.class == clazz;
	}

	@Override
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	public void validate(Object target, Errors errors) {
		final Pp2McView view = (Pp2McView) target;

		// l'id du tiers n'est pas éditable par l'utilisateur, sauf hack
		final Tiers tiers = tiersDAO.get(view.getId());
		if (tiers == null) {
			throw new TiersNotFoundException(view.getId());
		}
		if (!(tiers instanceof PersonnePhysique)) {
			throw new IllegalArgumentException("Le tiers doit être une personne physique !");
		}

		// vérification de la validité des dates
		if (view.getDateDebut() == null) {
			// [SIFISC-18086] blindage en cas de mauvais format de saisie, pour éviter le double message d'erreur
			if (!errors.hasFieldErrors("dateDebut")) {
				errors.rejectValue("dateDebut", "error.date.ouverture.vide");
			}
		}
		else if (view.getDateFin() != null && view.getDateDebut().isAfter(view.getDateFin())) {
			errors.rejectValue("dateFin", "error.date.fermeture.anterieure");
		}

		// vérification du contribuable principal
		final Long idPrincipal = view.getIdPrincipal();
		// [SIFISC-18086] blindage en cas de mauvais format de saisie, pour éviter le double message d'erreur
		if (!errors.hasFieldErrors("idPrincipal")) {
			if (idPrincipal == null) {
				errors.rejectValue("idPrincipal", "error.numero.obligatoire");
			}
			else {
				final Tiers principal = tiersDAO.get(idPrincipal);
				if (principal == null) {
					errors.rejectValue("idPrincipal", "error.tiers.inexistant");
				}
				else if (!(principal instanceof PersonnePhysique)) {
					errors.rejectValue("idPrincipal", "error.tiers.doit.etre.personne.physique");
				}
			}
		}

		// vérification du contribuable secondaire
		final Long idSecondaire = view.getIdSecondaire();
		if (idSecondaire != null) {
			final Tiers secondaire = tiersDAO.get(idSecondaire);
			if (secondaire == null) {
				errors.rejectValue("idSecondaire", "error.tiers.inexistant");
			}
			else if (!(secondaire instanceof PersonnePhysique)) {
				errors.rejectValue("idSecondaire", "error.tiers.doit.etre.personne.physique");
			}
		}

		// détection des doublons d'identifiants donnés
		if (view.getIdPrincipal() != null && view.getIdPrincipal() == view.getId()) {
			errors.rejectValue("idPrincipal", "error.tiers.identique.source");
		}
		if (view.getIdSecondaire() != null && view.getIdSecondaire() == view.getId()) {
			errors.rejectValue("idSecondaire", "error.tiers.identique.source");
		}
		if (view.getIdSecondaire() != null && view.getIdSecondaire().equals(view.getIdPrincipal())) {
			errors.rejectValue("idSecondaire", "error.tiers.identique.principal");
		}
	}
}
