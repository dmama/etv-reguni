package ch.vd.uniregctb.webservices.tiers3.data.strategy;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.webservices.tiers3.Adresse;
import ch.vd.uniregctb.webservices.tiers3.AdresseAutreTiers;
import ch.vd.uniregctb.webservices.tiers3.BusinessExceptionCode;
import ch.vd.uniregctb.webservices.tiers3.ForFiscal;
import ch.vd.uniregctb.webservices.tiers3.Tiers;
import ch.vd.uniregctb.webservices.tiers3.TiersPart;
import ch.vd.uniregctb.webservices.tiers3.WebServiceException;
import ch.vd.uniregctb.webservices.tiers3.data.CompteBancaireBuilder;
import ch.vd.uniregctb.webservices.tiers3.data.DeclarationBuilder;
import ch.vd.uniregctb.webservices.tiers3.data.ForFiscalBuilder;
import ch.vd.uniregctb.webservices.tiers3.data.ForGestionBuilder;
import ch.vd.uniregctb.webservices.tiers3.data.RapportEntreTiersBuilder;
import ch.vd.uniregctb.webservices.tiers3.impl.Context;
import ch.vd.uniregctb.webservices.tiers3.impl.DataHelper;
import ch.vd.uniregctb.webservices.tiers3.impl.ExceptionHelper;
import ch.vd.uniregctb.webservices.tiers3.impl.ForFiscalComparator;

public abstract class TiersStrategy<T extends Tiers> {

	private static final Logger LOGGER = Logger.getLogger(TiersStrategy.class);

	/**
	 * Crée une nouvelle instance d'un tiers web à partir d'un tiers business.
	 *
	 * @param right   le tiers business
	 * @param parts   les parts à renseigner
	 * @param context le context de création
	 * @return un nouveau tiers
	 * @throws ch.vd.uniregctb.webservices.tiers3.WebServiceException en cas de problème
	 */
	public abstract T newFrom(ch.vd.uniregctb.tiers.Tiers right, @Nullable Set<TiersPart> parts, Context context) throws WebServiceException;

	/**
	 * Retourne une copie du tiers spécifié en copiant uniquement les parts spécifiées.
	 *
	 * @param tiers un tiers
	 * @param parts les parts à copier
	 * @return une nouvelle instance du tiers
	 */
	public abstract T clone(T tiers, @Nullable Set<TiersPart> parts);

	/**
	 * Ajoute les parts d'un tiers sur un autre.
	 *
	 * @param to    le tiers sur lequel les parts seront copiées
	 * @param from  le tiers à partir duquel les parts seront copiées
	 * @param parts les parts à copier
	 */
	public final void copyParts(T to, T from, @Nullable Set<TiersPart> parts) {
		copyParts(to, from, parts, CopyMode.ADDITIF);
	}

	protected void initBase(T to, ch.vd.uniregctb.tiers.Tiers from, Context context) throws WebServiceException {
		to.setNumero(from.getNumero());
		to.setComplementNom(from.getComplementNom());
		to.setDateAnnulation(DataHelper.coreToWeb(from.getAnnulationDate()));
		to.setPersonneContact(from.getPersonneContact());
		to.setNumeroTelPrive(from.getNumeroTelephonePrive());
		to.setNumeroTelProf(from.getNumeroTelephoneProfessionnel());
		to.setNumeroTelPortable(from.getNumeroTelephonePortable());
		to.setNumeroTelecopie(from.getNumeroTelecopie());
		to.setAdresseCourrierElectronique(from.getAdresseCourrierElectronique());
		to.setBlocageRemboursementAutomatique(DataHelper.coreToWeb(from.getBlocageRemboursementAutomatique()));
		to.setDebiteurInactif(from.isDebiteurInactif());
	}

	protected void copyBase(T to, T from) {
		to.setNumero(from.getNumero());
		to.setComplementNom(from.getComplementNom());
		to.setDateAnnulation(from.getDateAnnulation());
		to.setPersonneContact(from.getPersonneContact());
		to.setNumeroTelPrive(from.getNumeroTelPrive());
		to.setNumeroTelProf(from.getNumeroTelProf());
		to.setNumeroTelPortable(from.getNumeroTelPortable());
		to.setNumeroTelecopie(from.getNumeroTelecopie());
		to.setAdresseCourrierElectronique(from.getAdresseCourrierElectronique());
		to.setBlocageRemboursementAutomatique(from.isBlocageRemboursementAutomatique());
		to.setDebiteurInactif(from.isDebiteurInactif());
	}


	protected void initParts(T left, ch.vd.uniregctb.tiers.Tiers tiers, @Nullable Set<TiersPart> parts, Context context) throws WebServiceException {

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

	protected void copyParts(T to, T from, @Nullable Set<TiersPart> parts, CopyMode mode) {

		if (parts != null && parts.contains(TiersPart.COMPTES_BANCAIRES)) {
			copyColl(to.getComptesBancaires(), from.getComptesBancaires());
		}

		if (parts != null && parts.contains(TiersPart.ADRESSES)) {
			copyColl(to.getAdressesCourrier(), from.getAdressesCourrier());
			copyColl(to.getAdressesRepresentation(), from.getAdressesRepresentation());
			copyColl(to.getAdressesDomicile(), from.getAdressesDomicile());
			copyColl(to.getAdressesPoursuite(), from.getAdressesPoursuite());
			copyColl(to.getAdressesPoursuiteAutreTiers(), from.getAdressesPoursuiteAutreTiers());
		}

		if (parts != null && parts.contains(TiersPart.ADRESSES_ENVOI)) {
			to.setAdresseCourrierFormattee(from.getAdresseCourrierFormattee());
			to.setAdresseRepresentationFormattee(from.getAdresseRepresentationFormattee());
			to.setAdresseDomicileFormattee(from.getAdresseDomicileFormattee());
			to.setAdressePoursuiteFormattee(from.getAdressePoursuiteFormattee());
			to.setAdressePoursuiteAutreTiersFormattee(from.getAdressePoursuiteAutreTiersFormattee());
		}

		if (parts != null && parts.contains(TiersPart.RAPPORTS_ENTRE_TIERS)) {
			copyColl(to.getRapportsEntreTiers(), from.getRapportsEntreTiers());
		}

		if (parts != null && (parts.contains(TiersPart.FORS_FISCAUX) || parts.contains(TiersPart.FORS_FISCAUX_VIRTUELS))) {
			/**
			 * [UNIREG-2587] Les fors fiscaux non-virtuels et les fors fiscaux virtuels représentent deux ensembles qui se recoupent.
			 * Plus précisemment, les fors fiscaux non-virtuels sont entièrement contenus dans les fors fiscaux virtuels. En fonction
			 * du mode de copie, il est donc nécessaire de compléter ou de filtrer les fors fiscaux.
			 */
			if (mode == CopyMode.ADDITIF) {
				if (parts.contains(TiersPart.FORS_FISCAUX_VIRTUELS) || to.getForsFiscauxPrincipaux() == null || to.getForsFiscauxPrincipaux().isEmpty()) {
					copyColl(to.getForsFiscauxPrincipaux(), from.getForsFiscauxPrincipaux());
				}
			}
			else {
				Assert.isEqual(CopyMode.EXCLUSIF, mode);
				if (parts.contains(TiersPart.FORS_FISCAUX_VIRTUELS)) {
					copyColl(to.getForsFiscauxPrincipaux(), from.getForsFiscauxPrincipaux());
				}
				else {
					// supprime les éventuels fors virtuels s'ils ne sont pas demandés
					if (from.getForsFiscauxPrincipaux() != null && !from.getForsFiscauxPrincipaux().isEmpty()) {
						to.getForsFiscauxPrincipaux().clear();
						for (ForFiscal f : from.getForsFiscauxPrincipaux()) {
							if (!f.isVirtuel()) {
								to.getForsFiscauxPrincipaux().add(f);
							}
						}
					}
					else {
						to.getForsFiscauxPrincipaux().clear();
					}
				}

			}
			copyColl(to.getAutresForsFiscaux(), from.getAutresForsFiscaux());
		}

		if (parts != null && parts.contains(TiersPart.FORS_GESTION)) {
			copyColl(to.getForsGestions(), from.getForsGestions());
		}

		if (parts != null && parts.contains(TiersPart.DECLARATIONS)) {
			copyColl(to.getDeclarations(), from.getDeclarations());
		}
	}

	private static void initComptesBancaires(Tiers left, Context context, ch.vd.uniregctb.tiers.Tiers tiers) {
		final String numero = tiers.getNumeroCompteBancaire();
		if (numero != null && !"".equals(numero) && context.ibanValidator.isValidIban(numero)) {
			left.getComptesBancaires().add(CompteBancaireBuilder.newCompteBancaire(tiers, context));
		}
	}

	private static void initAdresses(Tiers tiers, ch.vd.uniregctb.tiers.Tiers right, final Context context) throws WebServiceException {
		ch.vd.uniregctb.adresse.AdressesFiscalesHisto adresses;
		try {
			adresses = context.adresseService.getAdressesFiscalHisto(right, false);
		}
		catch (ch.vd.uniregctb.adresse.AdresseException e) {
			LOGGER.error(e, e);
			throw ExceptionHelper.newBusinessException(e, BusinessExceptionCode.ADRESSES);
		}

		if (adresses != null) {
			final List<Adresse> adressesCourrier = DataHelper.coreToWeb(adresses.courrier, null, context.infraService);
			if (adressesCourrier != null) {
				tiers.getAdressesCourrier().addAll(adressesCourrier);
			}

			final List<Adresse> adressesRepresentation = DataHelper.coreToWeb(adresses.representation, null, context.infraService);
			if (adressesRepresentation != null) {
				tiers.getAdressesRepresentation().addAll(adressesRepresentation);
			}

			final List<Adresse> adressesDomicile = DataHelper.coreToWeb(adresses.domicile, null, context.infraService);
			if (adressesDomicile != null) {
				tiers.getAdressesDomicile().addAll(adressesDomicile);
			}

			final List<Adresse> adressesPoursuite = DataHelper.coreToWeb(adresses.poursuite, null, context.infraService);
			if (adressesPoursuite != null) {
				tiers.getAdressesPoursuite().addAll(adressesPoursuite);
			}

			final List<AdresseAutreTiers> adresseAutreTiers = DataHelper.coreToWebAT(adresses.poursuiteAutreTiers, null, context.infraService);
			if (adresseAutreTiers != null) {
				tiers.getAdressesPoursuiteAutreTiers().addAll(adresseAutreTiers);
			}
		}
	}

	private static void initAdressesEnvoi(Tiers left, ch.vd.uniregctb.tiers.Tiers tiers, Context context) throws WebServiceException {
		try {
			left.setAdresseCourrierFormattee(DataHelper.createAdresseFormattee(tiers, null, context, TypeAdresseFiscale.COURRIER));
			left.setAdresseRepresentationFormattee(DataHelper.createAdresseFormattee(tiers, null, context, TypeAdresseFiscale.REPRESENTATION));
			left.setAdresseDomicileFormattee(DataHelper.createAdresseFormattee(tiers, null, context, TypeAdresseFiscale.DOMICILE));
			left.setAdressePoursuiteFormattee(DataHelper.createAdresseFormattee(tiers, null, context, TypeAdresseFiscale.POURSUITE));
			left.setAdressePoursuiteAutreTiersFormattee(DataHelper.createAdresseFormatteeAT(tiers, null, context, TypeAdresseFiscale.POURSUITE_AUTRE_TIERS));
		}
		catch (AdresseException e) {
			LOGGER.error(e, e);
			throw ExceptionHelper.newBusinessException(e, BusinessExceptionCode.ADRESSES);
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

	private static void initForsFiscaux(Tiers tiers, ch.vd.uniregctb.tiers.Tiers right, final Set<TiersPart> parts, Context context) {

		// le calcul de ces dates nécessite d'accéder aux fors fiscaux, initialisé ici pour des raisons de performances.
		tiers.setDateDebutActivite(DataHelper.coreToWeb(right.getDateDebutActivite()));
		tiers.setDateFinActivite(DataHelper.coreToWeb(right.getDateFinActivite()));

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

	private static void initForsGestion(Tiers tiers, final ch.vd.uniregctb.tiers.Tiers right, Context context) {
		for (ch.vd.uniregctb.tiers.ForGestion forGestion : context.tiersService.getForsGestionHisto(right)) {
			tiers.getForsGestions().add(ForGestionBuilder.newForGestion(forGestion));
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

	protected static <T> void copyColl(List<T> toColl, List<T> fromColl) {
		if (toColl == fromColl) {
			throw new IllegalArgumentException("La même collection a été spécifiée comme entrée et sortie !");
		}
		toColl.clear();
		toColl.addAll(fromColl);
	}
}

