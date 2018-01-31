package ch.vd.uniregctb.evenement.organisation.interne.demenagement;

import java.util.List;

import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.Domicile;
import ch.vd.unireg.interfaces.organisation.data.EntreeJournalRC;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationSuiviCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.type.MotifFor;

/**
 * @author Raphaël Marmier, 2015-10-13
 */
public class DemenagementSansChangementDeTypeAutoriteFiscale extends Demenagement {

	public DemenagementSansChangementDeTypeAutoriteFiscale(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise,
	                                                       EvenementOrganisationContext context,
	                                                       EvenementOrganisationOptions options,
	                                                       Domicile siegeAvant,
	                                                       Domicile siegeApres) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options, siegeAvant, siegeApres);
	}

	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {

		RegDate dateDemenagement;

		// Si on est une organisation inscrite au RC, la date de déménagement correspond à la date de l'entrée au régistre journalier.
		if (getOrganisation().isInscriteAuRC(getDateEvt()) && !getOrganisation().isRadieeDuRC(getDateEvt())) {
			final SiteOrganisation sitePrincipal = getOrganisation().getSitePrincipal(getDateEvt()).getPayload();
			final List<EntreeJournalRC> entreesJournal = sitePrincipal.getDonneesRC().getEntreesJournalPourDatePublication(getDateEvt());
			if (entreesJournal.isEmpty()) {
				throw new EvenementOrganisationException(
						String.format("Entrée de journal au RC introuvable dans l'établissement principal (civil: %s). Impossible de traiter le déménagement.",
						              sitePrincipal.getNumeroSite()));
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
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		super.validateSpecific(erreurs, warnings, suivis);

		/*
		 Erreurs techniques fatale
		  */

		Assert.notNull(getSitePrincipalApres());

		// On doit avoir deux autorités fiscales
		Assert.notNull(getSiegeAvant());
		Assert.notNull(getSiegeApres());

		// Quelque conditions non valides
		Assert.isTrue(getSiegeAvant() != getSiegeApres(), "Pas un déménagement de siège, la commune n'a pas changé!");
	}
}
