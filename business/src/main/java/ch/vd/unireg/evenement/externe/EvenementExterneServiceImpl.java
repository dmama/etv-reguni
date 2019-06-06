package ch.vd.unireg.evenement.externe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.data.CivilDataEventNotifier;
import ch.vd.unireg.data.FiscalDataEventNotifier;
import ch.vd.unireg.declaration.DeclarationImpotSource;
import ch.vd.unireg.declaration.EtatDeclaration;
import ch.vd.unireg.declaration.EtatDeclarationRetournee;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;

public class EvenementExterneServiceImpl implements EvenementExterneService {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementExterneServiceImpl.class);

	private EvenementExterneDAO evenementExterneDAO;
	private TiersDAO tiersDAO;

	private CivilDataEventNotifier civilDataEventNotifier;
	private FiscalDataEventNotifier fiscalDataEventNotifier;

	public void setEvenementExterneDAO(EvenementExterneDAO evenementExterneDAO) {
		this.evenementExterneDAO = evenementExterneDAO;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setCivilDataEventNotifier(CivilDataEventNotifier civilDataEventNotifier) {
		this.civilDataEventNotifier = civilDataEventNotifier;
	}

	public void setFiscalDataEventNotifier(FiscalDataEventNotifier fiscalDataEventNotifier) {
		this.fiscalDataEventNotifier = fiscalDataEventNotifier;
	}

	@Override
	public void onEvent(EvenementExterne event) throws EvenementExterneException {

		if (evenementExterneDAO.existe(event.getBusinessId())) {
			LOGGER.warn("Le message avec le business id=[" + event.getBusinessId() + "] existe déjà en base: il est ignoré.");
			return;
		}

		if (event instanceof QuittanceLR) {
			onEvenementLR((QuittanceLR) event);
		}
		else {
			throw new EvenementExterneException("Type d'événement inconnu = " + event.getClass());
		}
	}

	@Override
	public boolean retraiterEvenementExterne(EvenementExterne event) {
		if (event instanceof QuittanceLR) {
			return traiterEvenementLR((QuittanceLR) event);
		}
		else {
			throw new RuntimeException("Type d'événement inconnu = " + event.getClass());
		}
	}

	/**
	 * Traitement d'un événement traitant du quittancement (ou du dé-quittancement) d'une LR ; l'état de l'événement est mis à "TRAITE"
	 * si le traitement ne lève aucune exception
	 * @param event événement à traiter
	 * @return <code>true</code> si l'événement a été traité, <code>false</code> s'il est parti/resté en erreur
	 */
	private boolean traiterEvenementLR(QuittanceLR event) {
		boolean result = true;
		try {
			quittancementLr(event);
			event.setEtat(EtatEvenementExterne.TRAITE);
			event.setErrorMessage(null);
		}
		catch (EvenementExterneException e) {
			event.setEtat(EtatEvenementExterne.ERREUR);
			event.setErrorMessage(e.getMessage());
			result = false;
		}
		return result;
	}

	/**
	 * Appelé à la réception de l'événement de quittancement
	 * @param event événement de quittancement (ou d'annulation de quittancement) nouvellement reçu
	 */
	private void onEvenementLR(QuittanceLR event) {

		final Long tiersId = event.getTiersId();

		// Première chose à faire : sauver le message pour allouer un id technique.
		event.setEtat(EtatEvenementExterne.NON_TRAITE);
		event = (QuittanceLR) evenementExterneDAO.save(event);

		final Tiers tiers = tiersDAO.get(tiersId);
		if (tiers == null) {
			event.setEtat(EtatEvenementExterne.ERREUR);
			event.setErrorMessage(String.format("Tiers n'existe pas : %d", tiersId));
		}
		else {
			event.setTiers(tiers);

			// travail de traitement proprement dit
			traiterEvenementLR(event);
		}
	}

	private DeclarationImpotSource validate(QuittanceLR quittance) throws EvenementExterneException {
		DeclarationImpotSource declarationImpotSource = null;
		// La période de décompte a été ouverte et n’a pas été annulée.
		final Tiers tiers = quittance.getTiers();
		for (DeclarationImpotSource candidate : tiers.getDeclarationsTriees(DeclarationImpotSource.class, false)) {
			if (candidate.getDateDebut() == quittance.getDateDebut()) {
				declarationImpotSource = candidate;
				break;
			}
		}
		if (declarationImpotSource == null) {
			throw new EvenementExterneException(String.format("Aucune correspondance avec une déclaration impôt source de ce débiteur: %d", tiers.getNumero()));
		}

		// Au moins la date de retour ou l’annulation du retour doit être renseignée et elles ne peuvent être présentes simultanément.
		if (quittance.getType() == TypeQuittance.QUITTANCEMENT) {
			if (quittance.getDateEvenement() == null) {
				throw new EvenementExterneException("Pour un quittancement la date de retour est requise.");
			}
			// Si la date de retour est renseignée, elle ne se situe pas dans le futur et le retour n’a pas encore été enregistré.
			//Transformation en RegDate pour n'avoir à comparer que les jours.
			final RegDate regDateEvenement = RegDateHelper.get(quittance.getDateEvenement());
			if (RegDateHelper.isAfter(regDateEvenement, RegDate.get(), NullDateBehavior.LATEST)) {
				throw new EvenementExterneException(String.format("La date de retour (%s) ne peut se situer dans le futur", RegDateHelper.dateToDisplayString(regDateEvenement)));
			}
		}
		else if (quittance.getType() == TypeQuittance.ANNULATION) {
			//Changement de la xsd: une date d'evenement doit être presente pour l'annulation egalement
			if (quittance.getDateEvenement() == null) {
				throw new EvenementExterneException("Pour une annulation la date de retour est requise.");
			}
			final EtatDeclaration etatDeclaration = declarationImpotSource.getDernierEtatDeclarationOfType(TypeEtatDocumentFiscal.RETOURNE);
			// S’il s’agit d’une annulation du retour, le retour a déjà été enregistré.
			if (etatDeclaration == null) {
				throw new EvenementExterneException(String.format("La déclaration impôt source sélectionnée (tiers=%d, période=%s) ne contient pas de retour à annuler.",
				                                                  tiers.getNumero(), DateRangeHelper.toDisplayString(declarationImpotSource)));
			}
		}

		return declarationImpotSource;
	}

	private void quittancementLr(QuittanceLR quittance) throws EvenementExterneException {
		final DeclarationImpotSource declarationImpotSource = validate(quittance);
		// En l’absence d’erreur, l’application met à jour la liste récapitulative du débiteur de l’impôt à la source
		// correspondant à la période de décompte :
		// - Si la date de retour est renseignée, elle en y insère la date de retour.
		// - S’il s’agit d’une annulation du retour, elle efface la date de retour.
		if (quittance.getType() == TypeQuittance.QUITTANCEMENT) {

			// on annule les éventuels quittancements antérieurs si nécessaire
			for (EtatDeclaration etat : declarationImpotSource.getEtatsDeclaration()) {
				if (!etat.isAnnule() && etat.getEtat() == TypeEtatDocumentFiscal.RETOURNE) {
					etat.setAnnule(true);
				}
			}

			// on crée le nouvel état "retourné"
			final EtatDeclaration etatDeclaration = new EtatDeclarationRetournee();
			etatDeclaration.setDateObtention(RegDateHelper.get(quittance.getDateEvenement()));
			etatDeclaration.setAnnule(false);
			declarationImpotSource.addEtat(etatDeclaration);
		}
		else if (quittance.getType() == TypeQuittance.ANNULATION) {
			final EtatDeclaration etatDeclaration = declarationImpotSource.getDernierEtatDeclarationOfType(TypeEtatDocumentFiscal.RETOURNE);
			etatDeclaration.setAnnule(true);
		}
		else {
			throw new RuntimeException("Type d'événement de quittancement de LR invalide : " + quittance.getType());
		}

		// [UNIREG-1947] ne pas oublier d'invalider le cache du tiers!
		fiscalDataEventNotifier.notifyTiersChange(declarationImpotSource.getTiers().getNumero());
	}

}
