package ch.vd.uniregctb.webservices.tiers3.data;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.webservices.tiers3.CategoriePersonnePhysique;
import ch.vd.uniregctb.webservices.tiers3.Contribuable;
import ch.vd.uniregctb.webservices.tiers3.Debiteur;
import ch.vd.uniregctb.webservices.tiers3.MenageCommun;
import ch.vd.uniregctb.webservices.tiers3.ModeCommunication;
import ch.vd.uniregctb.webservices.tiers3.PeriodeImposition;
import ch.vd.uniregctb.webservices.tiers3.PersonnePhysique;
import ch.vd.uniregctb.webservices.tiers3.Sexe;
import ch.vd.uniregctb.webservices.tiers3.Tiers;
import ch.vd.uniregctb.webservices.tiers3.TiersPart;
import ch.vd.uniregctb.webservices.tiers3.WebServiceException;
import ch.vd.uniregctb.webservices.tiers3.impl.BusinessHelper;
import ch.vd.uniregctb.webservices.tiers3.impl.Context;
import ch.vd.uniregctb.webservices.tiers3.impl.DataHelper;
import ch.vd.uniregctb.webservices.tiers3.impl.EnumHelper;
import ch.vd.uniregctb.webservices.tiers3.impl.ExceptionHelper;
import ch.vd.uniregctb.webservices.tiers3.impl.ForFiscalComparator;

public class TiersBuilder {

	private static final Logger LOGGER = Logger.getLogger(TiersBuilder.class);

	public static PersonnePhysique newPersonnePhysique(ch.vd.uniregctb.tiers.PersonnePhysique right, @Nullable Set<TiersPart> parts, Context context) throws WebServiceException {
		final PersonnePhysique left = new PersonnePhysique();
		fillPersonnePhysique(left, right, parts, context);
		return left;
	}

	public static MenageCommun newMenageCommun(ch.vd.uniregctb.tiers.MenageCommun right, Set<TiersPart> parts, Context context) throws WebServiceException {
		final MenageCommun left = new MenageCommun();
		fillMenageCommun(left, right, parts, context);
		return left;
	}

	public static Debiteur newDebiteur(DebiteurPrestationImposable right, Set<TiersPart> parts, Context context) throws WebServiceException {
		final Debiteur left = new Debiteur();
		fillDebiteur(left, right, parts, context);
		return left;
	}

	private static void fillTiers(Tiers left, ch.vd.uniregctb.tiers.Tiers tiers, Set<TiersPart> parts, Context context) throws WebServiceException {
		fillTiersBase(left, tiers);
		initTiersParts(left, tiers, parts, context);
	}

	private static void fillTiersBase(Tiers left, ch.vd.uniregctb.tiers.Tiers tiers) {
		left.setNumero(tiers.getNumero());
		left.setComplementNom(tiers.getComplementNom());
		left.setDateDebutActivite(DataHelper.coreToWeb(tiers.getDateDebutActivite()));
		left.setDateFinActivite(DataHelper.coreToWeb(tiers.getDateFinActivite()));
		left.setDateAnnulation(DataHelper.coreToWeb(tiers.getAnnulationDate()));
		left.setPersonneContact(tiers.getPersonneContact());
		left.setNumeroTelPrive(tiers.getNumeroTelephonePrive());
		left.setNumeroTelProf(tiers.getNumeroTelephoneProfessionnel());
		left.setNumeroTelPortable(tiers.getNumeroTelephonePortable());
		left.setNumeroTelecopie(tiers.getNumeroTelecopie());
		left.setAdresseCourrierElectronique(tiers.getAdresseCourrierElectronique());
		left.setBlocageRemboursementAutomatique(DataHelper.coreToWeb(tiers.getBlocageRemboursementAutomatique()));
		left.setIsDebiteurInactif(tiers.isDebiteurInactif());
	}

	private static void fillContribuable(Contribuable left, ch.vd.uniregctb.tiers.Contribuable right, Set<TiersPart> parts, Context context) throws WebServiceException {
		fillTiers(left, right, parts, context);
		initContribuableParts(left, right, parts, context);
	}

	private static void fillPersonnePhysique(PersonnePhysique left, ch.vd.uniregctb.tiers.PersonnePhysique personne, Set<TiersPart> parts, Context context) throws WebServiceException {
		fillContribuable(left, personne, parts, context);

		if (!personne.isHabitantVD()) {
			left.setNom(personne.getNom());
			left.setPrenom(personne.getPrenom());
			left.setDateNaissance(DataHelper.coreToWeb(personne.getDateNaissance()));
			left.setSexe(EnumHelper.coreToWeb(personne.getSexe()));
			left.setDateDeces(DataHelper.coreToWeb(personne.getDateDeces()));
			for (ch.vd.uniregctb.tiers.IdentificationPersonne ident : personne.getIdentificationsPersonnes()) {
				if (ident.getCategorieIdentifiant() == ch.vd.uniregctb.type.CategorieIdentifiant.CH_AHV_AVS) {
					left.setAncienNumeroAssureSocial(ident.getIdentifiant());
				}
			}
			left.setNouveauNumeroAssureSocial(personne.getNumeroAssureSocial());
			left.setDateArrivee(DataHelper.coreToWeb(personne.getDateDebutActivite()));
			left.setCategorie(EnumHelper.coreToWeb(personne.getCategorieEtranger()));
		}
		else {
			final ch.vd.uniregctb.interfaces.model.Individu individu = context.serviceCivilService.getIndividu(personne.getNumeroIndividu(), null, AttributeIndividu.PERMIS);

			if (individu == null) {
				final String message = String.format("Impossible de trouver l'individu n°%d pour l'habitant n°%d", personne
						.getNumeroIndividu(), personne.getNumero());
				LOGGER.error(message);
				throw ExceptionHelper.newBusinessException(message);
			}

			final ch.vd.uniregctb.interfaces.model.HistoriqueIndividu data = individu.getDernierHistoriqueIndividu();
			left.setNom(data.getNom());
			left.setPrenom(data.getPrenom());
			left.setDateNaissance(DataHelper.coreToWeb(individu.getDateNaissance()));
			left.setSexe((individu.isSexeMasculin() ? Sexe.MASCULIN : Sexe.FEMININ));
			left.setDateDeces(DataHelper.coreToWeb(personne.getDateDeces() == null ? individu.getDateDeces() : personne.getDateDeces()));
			left.setNouveauNumeroAssureSocial(individu.getNouveauNoAVS());
			left.setAncienNumeroAssureSocial(data.getNoAVS());
			left.setDateArrivee(DataHelper.coreToWeb(data.getDateDebutValidite()));

			final ch.vd.uniregctb.interfaces.model.Permis permis = individu.getPermisActif(null);
			if (permis == null) {
				left.setCategorie(CategoriePersonnePhysique.SUISSE);
			}
			else {
				left.setCategorie(EnumHelper.coreToWeb(permis.getTypePermis()));
			}
		}
	}

	private static void fillMenageCommun(MenageCommun left, ch.vd.uniregctb.tiers.MenageCommun right, Set<TiersPart> parts, Context context) throws WebServiceException {
		fillContribuable(left, right, parts, context);
		initMenageCommunParts(left, right, parts, context);
	}

	private static void fillDebiteur(Debiteur left, DebiteurPrestationImposable debiteur, Set<TiersPart> parts, Context context) throws WebServiceException {
		fillTiers(left, debiteur, parts, context);
		left.setRaisonSociale(BusinessHelper.getRaisonSociale(debiteur, null, context.adresseService));
		left.setCategorie(EnumHelper.coreToWeb(debiteur.getCategorieImpotSource()));
		left.setModeCommunication(EnumHelper.coreToWeb(debiteur.getModeCommunication()));
		left.setSansRappel(DataHelper.coreToWeb(debiteur.getSansRappel()));
		left.setSansListeRecapitulative(DataHelper.coreToWeb(debiteur.getSansListeRecapitulative()));
		left.setContribuableAssocie(debiteur.getContribuableId());
		if (left.getModeCommunication() == ModeCommunication.ELECTRONIQUE) {
			left.setLogicielId(debiteur.getLogicielId());
		}
		initDebiteurParts(left, debiteur, parts);
	}

	private static void initDebiteurParts(Debiteur left, DebiteurPrestationImposable right, Set<TiersPart> parts) {
		if (parts != null && parts.contains(TiersPart.PERIODICITES)) {
			initPeriodicites(left, right);
		}
	}

	private static void initPeriodicites(Debiteur left, DebiteurPrestationImposable right) {
		for (ch.vd.uniregctb.declaration.Periodicite periodicite : right.getPeriodicitesNonAnnules(true)) {
			left.getPeriodicites().add(PeriodiciteBuilder.newPeriodicite(periodicite));
		}
	}

	private static void initMenageCommunParts(MenageCommun left, ch.vd.uniregctb.tiers.MenageCommun right, Set<TiersPart> parts, Context context) throws WebServiceException {
		if (parts != null && parts.contains(TiersPart.COMPOSANTS_MENAGE)) {
			initComposants(left, right, context);
		}
	}

	private static void initComposants(MenageCommun left, ch.vd.uniregctb.tiers.MenageCommun menageCommun, Context context) throws WebServiceException {
		EnsembleTiersCouple ensemble = context.tiersService.getEnsembleTiersCouple(menageCommun, null);
		final ch.vd.uniregctb.tiers.PersonnePhysique principal = ensemble.getPrincipal();
		if (principal != null) {
			left.setContribuablePrincipal(newPersonnePhysique(principal, null, context));
		}

		final ch.vd.uniregctb.tiers.PersonnePhysique conjoint = ensemble.getConjoint();
		if (conjoint != null) {
			left.setContribuableSecondaire(newPersonnePhysique(conjoint, null, context));
		}
	}

	private static void initContribuableParts(Contribuable left, ch.vd.uniregctb.tiers.Contribuable right, Set<TiersPart> parts, Context context) throws WebServiceException {
		if (parts != null && parts.contains(TiersPart.SITUATIONS_FAMILLE)) {
			initSituationsFamille(left, right, context);
		}

		if (parts != null && (parts.contains(TiersPart.ASSUJETTISSEMENTS) || parts.contains(TiersPart.PERIODES_ASSUJETTISSEMENT))) {
			initAssujettissements(left, right, parts);
		}

		if (parts != null && parts.contains(TiersPart.PERIODES_IMPOSITION)) {
			initPeriodesImposition(left, right, context);
		}
	}

	private static void initAssujettissements(Contribuable left, ch.vd.uniregctb.tiers.Contribuable right, Set<TiersPart> parts) throws WebServiceException {
		/*
		 * Note: il est nécessaire de calculer l'assujettissement sur TOUTE la période de validité du contribuable pour obtenir un résultat
		 * correct avec le collate.
		 */
		final List<ch.vd.uniregctb.metier.assujettissement.Assujettissement> list;
		try {
			list = ch.vd.uniregctb.metier.assujettissement.Assujettissement.determine(right, null, true /* collate */);
		}
		catch (AssujettissementException e) {
			LOGGER.error(e, e);
			throw ExceptionHelper.newBusinessException(e);
		}

		if (list != null) {

			final boolean wantAssujettissements = parts.contains(TiersPart.ASSUJETTISSEMENTS);
			final boolean wantPeriodes = parts.contains(TiersPart.PERIODES_ASSUJETTISSEMENT);

			for (ch.vd.uniregctb.metier.assujettissement.Assujettissement a : list) {
				if (wantAssujettissements) {
					left.getAssujettissementsRole().add(AssujettissementBuilder.newAssujettissement(a));
				}
				if (wantPeriodes) {
					left.getPeriodesAssujettissementLIC().add(PeriodeAssujettissementBuilder.toLIC(a));
					left.getPeriodesAssujettissementLIFD().add(PeriodeAssujettissementBuilder.toLIFD(a));
				}
			}
		}
	}

	private static void initPeriodesImposition(Contribuable left, ch.vd.uniregctb.tiers.Contribuable contribuable, Context context)
			throws WebServiceException {

		// [UNIREG-913] On n'expose pas les périodes fiscales avant la première période définie dans les paramètres
		final int premierePeriodeFiscale = context.parametreService.getPremierePeriodeFiscale();
		final DateRangeHelper.Range range = new DateRangeHelper.Range(RegDate.get(premierePeriodeFiscale, 1, 1), null);

		final List<ch.vd.uniregctb.metier.assujettissement.PeriodeImposition> list;
		try {
			list = ch.vd.uniregctb.metier.assujettissement.PeriodeImposition.determine(contribuable, range);
		}
		catch (AssujettissementException e) {
			LOGGER.error(e, e);
			throw ExceptionHelper.newBusinessException(e);
		}
		if (list != null) {
			PeriodeImposition derniere = null;
			for (ch.vd.uniregctb.metier.assujettissement.PeriodeImposition p : list) {
				final PeriodeImposition periode = PeriodeImpositionBuilder.newPeriodeImposition(p);
				left.getPeriodesImposition().add(periode);
				derniere = periode;
			}
			// [UNIREG-910] la période d'imposition courante est laissée ouverte
			if (derniere != null && derniere.getDateFin() != null) {
				final RegDate aujourdhui = RegDate.get();
				final RegDate dateFin = DataHelper.webToCore(derniere.getDateFin());
				if (dateFin.isAfter(aujourdhui)) {
					derniere.setDateFin(null);
				}
			}
		}
	}

	private static void initSituationsFamille(Contribuable left, ch.vd.uniregctb.tiers.Contribuable contribuable, Context context) {

		final List<ch.vd.uniregctb.situationfamille.VueSituationFamille> situations = context.situationService.getVueHisto(contribuable);

		for (ch.vd.uniregctb.situationfamille.VueSituationFamille situation : situations) {
			left.getSituationsFamille().add(SituationFamilleBuilder.newSituationFamille(situation));
		}
	}

	private static void initTiersParts(Tiers left, ch.vd.uniregctb.tiers.Tiers tiers, Set<TiersPart> parts, Context context) throws WebServiceException {

		if (parts != null && parts.contains(TiersPart.COMPTES_BANCAIRES)) {
			initComptesBancaires(left, context, tiers);
		}

		if (parts != null && parts.contains(TiersPart.ADRESSES)) {
			initAdresses(left, tiers, context);
		}

		if (parts != null && parts.contains(TiersPart.ADRESSES_ENVOI)) {
			initAdressesEnvoi(left, tiers, context);
		}

		if (parts != null && parts.contains(TiersPart.RAPPORTS_ENTRE_TIERS)) {
			initRapports(left, tiers);
		}

		if (parts != null && (parts.contains(TiersPart.FORS_FISCAUX) || parts.contains(TiersPart.FORS_FISCAUX_VIRTUELS))) {
			initForsFiscaux(left, tiers, parts, context);
		}

		if (parts != null && parts.contains(TiersPart.FORS_GESTION)) {
			initForsGestion(left, tiers, context);
		}

		if (parts != null && parts.contains(TiersPart.DECLARATIONS)) {
			initDeclarations(left, tiers);
		}
	}

	private static void initComptesBancaires(Tiers left, Context context, ch.vd.uniregctb.tiers.Tiers tiers) {
		final String numero = tiers.getNumeroCompteBancaire();
		if (numero != null && !"".equals(numero) && context.ibanValidator.isValidIban(numero)) {
			left.getComptesBancaires().add(CompteBancaireBuilder.newCompteBancaire(tiers, context));
		}
	}

	private static void initAdressesEnvoi(Tiers left, ch.vd.uniregctb.tiers.Tiers tiers, Context context) throws WebServiceException {
		try {
			left.setAdresseEnvoi(DataHelper.createAdresseFormattee(tiers, null, context, TypeAdresseFiscale.COURRIER));
			left.setAdresseRepresentationFormattee(DataHelper.createAdresseFormattee(tiers, null, context, TypeAdresseFiscale.REPRESENTATION));
			left.setAdresseDomicileFormattee(DataHelper.createAdresseFormattee(tiers, null, context, TypeAdresseFiscale.DOMICILE));
			left.setAdressePoursuiteFormattee(DataHelper.createAdresseFormattee(tiers, null, context, TypeAdresseFiscale.POURSUITE));
			left.setAdressePoursuiteAutreTiersFormattee(DataHelper.createAdresseFormatteeAT(tiers, null, context, TypeAdresseFiscale.POURSUITE_AUTRE_TIERS));
		}
		catch (AdresseException e) {
			LOGGER.error(e, e);
			throw ExceptionHelper.newBusinessException(e);
		}
	}

	private static void initDeclarations(Tiers tiers, final ch.vd.uniregctb.tiers.Tiers right) {
		for (ch.vd.uniregctb.declaration.Declaration declaration : right.getDeclarationsSorted()) {
			if (declaration instanceof ch.vd.uniregctb.declaration.DeclarationImpotSource) {
				tiers.getDeclarations().add(DeclarationBuilder.newDeclarationImpotSource((ch.vd.uniregctb.declaration.DeclarationImpotSource) declaration));
			}
			else if (declaration instanceof ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire) {
				tiers.getDeclarations().add(DeclarationBuilder.newDeclarationImpotOrdinaire((ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire) declaration));
			}
		}
	}

	private static void initForsGestion(Tiers tiers, final ch.vd.uniregctb.tiers.Tiers right, Context context) {
		for (ch.vd.uniregctb.tiers.ForGestion forGestion : context.tiersService.getForsGestionHisto(right)) {
			tiers.getForsGestions().add(ForGestionBuilder.newForGestion(forGestion));
		}
	}

	private static void initForsFiscaux(Tiers tiers, ch.vd.uniregctb.tiers.Tiers right, final Set<TiersPart> parts, Context context) {
		for (ch.vd.uniregctb.tiers.ForFiscal forFiscal : right.getForsFiscauxSorted()) {
			if (forFiscal instanceof ch.vd.uniregctb.tiers.ForFiscalPrincipal
					|| forFiscal instanceof ch.vd.uniregctb.tiers.ForDebiteurPrestationImposable) {
				tiers.getForsFiscauxPrincipaux().add(ForFiscalBuilder.newForFiscalPrincipal(forFiscal, false));
			}
			else {
				tiers.getAutresForsFiscaux().add(ForFiscalBuilder.newForFiscal(forFiscal, false));
			}
		}

		// [UNIREG-1291] ajout des fors fiscaux virtuels
		if (parts.contains(TiersPart.FORS_FISCAUX_VIRTUELS)) {
			final List<ch.vd.uniregctb.tiers.ForFiscalPrincipal> forsVirtuels = DataHelper.getForsFiscauxVirtuels(right, context.tiersDAO);
			for (ch.vd.uniregctb.tiers.ForFiscalPrincipal forFiscal : forsVirtuels) {
				tiers.getForsFiscauxPrincipaux().add(ForFiscalBuilder.newForFiscalPrincipal(forFiscal, true));
			}
			Collections.sort(tiers.getForsFiscauxPrincipaux(), new ForFiscalComparator());
		}
	}

	private static void initRapports(Tiers tiers, final ch.vd.uniregctb.tiers.Tiers right) {
		// Ajoute les rapports dont le tiers est le sujet
		for (ch.vd.uniregctb.tiers.RapportEntreTiers rapport : right.getRapportsSujet()) {
			if (rapport instanceof ch.vd.uniregctb.tiers.ContactImpotSource) {
				continue;
			}

			tiers.getRapportsEntreTiers().add(RapportEntreTiersBuilder.newRapportEntreTiers(rapport, rapport.getObjetId()));
		}

		// Ajoute les rapports dont le tiers est l'objet
		for (ch.vd.uniregctb.tiers.RapportEntreTiers rapport : right.getRapportsObjet()) {
			if (rapport instanceof ch.vd.uniregctb.tiers.ContactImpotSource) {
				continue;
			}
			tiers.getRapportsEntreTiers().add(RapportEntreTiersBuilder.newRapportEntreTiers(rapport, rapport.getSujetId()));
		}
	}

	private static void initAdresses(Tiers tiers, ch.vd.uniregctb.tiers.Tiers right, final Context context) throws WebServiceException {
		ch.vd.uniregctb.adresse.AdressesFiscalesHisto adresses;
		try {
			adresses = context.adresseService.getAdressesFiscalHisto(right, false);
		}
		catch (ch.vd.uniregctb.adresse.AdresseException e) {
			LOGGER.error(e, e);
			throw ExceptionHelper.newBusinessException(e);
		}

		if (adresses != null) {
			tiers.getAdressesCourrier().addAll(DataHelper.coreToWeb(adresses.courrier, null, context.infraService));
			tiers.getAdressesRepresentation().addAll(DataHelper.coreToWeb(adresses.representation, null, context.infraService));
			tiers.getAdressesDomicile().addAll(DataHelper.coreToWeb(adresses.domicile, null, context.infraService));
			tiers.getAdressesPoursuite().addAll(DataHelper.coreToWeb(adresses.poursuite, null, context.infraService));
			tiers.getAdressesPoursuiteAutreTiers().addAll(DataHelper.coreToWebAT(adresses.poursuiteAutreTiers, null, context.infraService));
		}
	}

}
