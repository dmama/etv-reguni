package ch.vd.uniregctb.evenement.civil.interne.depart;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdressesCiviles;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class DepartSecondaire extends Depart {

	private final Adresse ancienneAdresse;
	private final Commune ancienneCommune;

	protected DepartSecondaire(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);

		final RegDate dateDepart = getDate();
		final AdressesCiviles anciennesAdresses = getAdresses(context, dateDepart);
		this.ancienneAdresse = anciennesAdresses.secondaire;
		this.ancienneCommune = getCommuneByAdresse(context, ancienneAdresse, dateDepart);
	}

	protected DepartSecondaire(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, Adresse adressePrincipale, Commune communePrincipale, Adresse ancienneAdresseSecondaire, Commune ancienneCommuneSecondaire, EvenementCivilContext context) {
		super(individu, conjoint, date, numeroOfsCommuneAnnonce, adressePrincipale, communePrincipale, context);
		this.ancienneAdresse = ancienneAdresseSecondaire;
		this.ancienneCommune = ancienneCommuneSecondaire;
	}

	@Override
	public void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		super.validateSpecific(erreurs, warnings);

		Audit.info(getNumeroEvenement(), "Validation du départ de résidence secondaire");
		validateCoherenceAdresse(ancienneAdresse, ancienneCommune, erreurs);
	}

	@Override
	protected void doHandleFermetureFors(PersonnePhysique pp, Contribuable ctb, RegDate dateFermeture, MotifFor motifFermeture) throws EvenementCivilException {
		Audit.info(getNumeroEvenement(), "Traitement du départ secondaire");
		handleDepartResidenceSecondaire(ctb, dateFermeture, motifFermeture);
	}

	/**
	 * Traite un depart d'une residence secondaire
	 *
	 * @param depart         un événement de départ
	 * @param contribuable
	 * @param dateFermeture
	 * @param motifFermeture
	 */
	private void handleDepartResidenceSecondaire(Contribuable contribuable, RegDate dateFermeture, MotifFor motifFermeture) {

		final ForFiscalPrincipal forPrincipal = contribuable.getForFiscalPrincipalAt(dateFermeture);
		// For principal est sur la commune de départ d'une résidence secondaire

		//TODO (BNM) attention si depart.getNumeroOfsCommuneAnnonce correspond à une commune avec des fractions
		//ce cas d'arrangement fiscal ne sera pas détecté, il faut mettre l'événement en erreur pour
		//traitement manuel

		if (forPrincipal != null && forPrincipal.getNumeroOfsAutoriteFiscale().equals(getNumeroOfsCommuneAnnonce())) {

			final ServiceInfrastructureService serviceInfra = context.getServiceInfra();
			Commune commune = null;
			final Adresse nouvelleAdresse = getNouvelleAdressePrincipale();
			final boolean estEnSuisse = serviceInfra.estEnSuisse(nouvelleAdresse);
			if (estEnSuisse) {
				commune = serviceInfra.getCommuneByAdresse(nouvelleAdresse, dateFermeture.getOneDayAfter());
			}

			final ForFiscalPrincipal ffp = contribuable.getForFiscalPrincipalAt(null);

			// [UNIREG-1921] si la commune du for principal ne change pas suite au départ secondaire, rien à faire!
			if (commune != null && ffp.getNumeroOfsAutoriteFiscale() == commune.getNoOFSEtendu() && commune.isVaudoise() && ffp.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				// rien à faire sur les fors...
			}
			else {
				// fermeture de l'ancien for principal à la date du départ
				getService().closeForFiscalPrincipal(ffp, dateFermeture, motifFermeture);

				// l'individu a sa residence principale en suisse
				if (estEnSuisse) {
					if (commune.isVaudoise()) {
						/*
						 * passage d'une commune vaudoise ouverte sur une résidence secondaire à une commune vaudoise ouverte sur la résidence
						 * principale : il s'agit donc d'un déménagement vaudois
						 */
						openForFiscalPrincipal(contribuable, dateFermeture.getOneDayAfter(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, commune.getNoOFS(), MotifRattachement.DOMICILE, MotifFor.DEMENAGEMENT_VD, ffp.getModeImposition(), false);
					}
					else {
						final ModeImposition modeImpostion = determineModeImpositionDepartHCHS(contribuable, dateFermeture, ffp);
						openForFiscalPrincipalHC(contribuable, dateFermeture.getOneDayAfter(), commune.getNoOFS(), modeImpostion, MotifFor.DEPART_HC);
					}
				}
				else if (ffp != null) {
					final ModeImposition modeImposition = determineModeImpositionDepartHCHS(contribuable, dateFermeture, ffp);
					openForFiscalPrincipalHS(contribuable, dateFermeture.getOneDayAfter(), nouvelleAdresse.getNoOfsPays(), modeImposition, MotifFor.DEPART_HS);
				}
			}
		}
	}
}
