package ch.vd.unireg.evenement.organisation.interne.demenagement;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.organisation.EvenementEntreprise;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseException;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.organisation.audit.EvenementEntrepriseErreurCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementEntrepriseSuiviCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementEntrepriseWarningCollector;
import ch.vd.unireg.interfaces.organisation.data.Domicile;
import ch.vd.unireg.interfaces.organisation.data.EntreeJournalRC;
import ch.vd.unireg.interfaces.organisation.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.organisation.data.EtablissementCivil;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.type.MotifFor;

/**
 * @author Raphaël Marmier, 2015-10-13
 */
public class DemenagementSansChangementDeTypeAutoriteFiscale extends Demenagement {

	public DemenagementSansChangementDeTypeAutoriteFiscale(EvenementEntreprise evenement, EntrepriseCivile entrepriseCivile, Entreprise entreprise,
	                                                       EvenementEntrepriseContext context,
	                                                       EvenementEntrepriseOptions options,
	                                                       Domicile siegeAvant,
	                                                       Domicile siegeApres) throws EvenementEntrepriseException {
		super(evenement, entrepriseCivile, entreprise, context, options, siegeAvant, siegeApres);
	}

	@Override
	public void doHandle(EvenementEntrepriseWarningCollector warnings, EvenementEntrepriseSuiviCollector suivis) throws EvenementEntrepriseException {

		RegDate dateDemenagement;

		// Si on est une entreprise inscrite au RC, la date de déménagement correspond à la date de l'entrée au régistre journalier.
		if (getEntrepriseCivile().isInscriteAuRC(getDateEvt()) && !getEntrepriseCivile().isRadieeDuRC(getDateEvt())) {
			final EtablissementCivil etablissementPrincipal = getEntrepriseCivile().getEtablissementPrincipal(getDateEvt()).getPayload();
			final List<EntreeJournalRC> entreesJournal = etablissementPrincipal.getDonneesRC().getEntreesJournalPourDatePublication(getDateEvt());
			if (entreesJournal.isEmpty()) {
				throw new EvenementEntrepriseException(
						String.format("Entrée de journal au RC introuvable dans l'établissement principal (civil: %s). Impossible de traiter le déménagement.",
						              etablissementPrincipal.getNumeroEtablissement()));
			}
			// On prend la première entrée qui vient car il devrait y en avoir qu'une seule. S'il devait vraiment y en avoir plusieurs, on considère qu'elles renverraient toutes vers le même jour.
			dateDemenagement = entreesJournal.iterator().next().getDate();
		} else {
			dateDemenagement = getDateEvt();
		}

		// Création & vérification de la surcharge corrective s'il y a lieu
		if (dateDemenagement.isBefore(getDateEvt())) {
			SurchargeCorrectiveRange surchargeCorrectiveRange = new SurchargeCorrectiveRange(dateDemenagement, getDateEvt().getOneDayBefore());
			verifieSurchargeAcceptable(dateDemenagement, surchargeCorrectiveRange);
			appliqueDonneesCivilesSurPeriode(getEntreprise(), surchargeCorrectiveRange, getDateEvt(), warnings, suivis);
		}

		final MotifFor motifFor = MotifFor.DEMENAGEMENT_VD;
		effectueChangementSiege(motifFor, dateDemenagement, warnings, suivis);
	}

	@Override
	protected void validateSpecific(EvenementEntrepriseErreurCollector erreurs, EvenementEntrepriseWarningCollector warnings, EvenementEntrepriseSuiviCollector suivis) throws EvenementEntrepriseException {
		super.validateSpecific(erreurs, warnings, suivis);

		// Erreurs techniques fatale
		// On doit avoir deux autorités fiscales
		if (getEtablissementCivilPrincipalApres() == null || getSiegeAvant() == null || getSiegeApres() == null) {
			throw new IllegalArgumentException();
		}

		// Quelque conditions non valides
		if (getSiegeAvant() == getSiegeApres()) {
			throw new IllegalArgumentException("Pas un déménagement de siège, la commune n'a pas changé!");
		}
	}
}
