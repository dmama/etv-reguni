package ch.vd.uniregctb.efacture;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.core.io.ClassPathResource;

import ch.vd.registre.base.avs.AvsHelper;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.efacture.data.Demande;
import ch.vd.unireg.interfaces.efacture.data.DemandeAvecHisto;
import ch.vd.unireg.interfaces.efacture.data.DestinataireAvecHisto;
import ch.vd.unireg.interfaces.efacture.data.TypeAttenteDemande;
import ch.vd.unireg.interfaces.efacture.data.TypeEtatDemande;
import ch.vd.unireg.interfaces.efacture.data.TypeEtatDestinataire;
import ch.vd.unireg.interfaces.efacture.data.TypeRefusDemande;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeDocument;

public class EFactureEventHandlerImpl implements EFactureEventHandler {

	private final Logger LOGGER = Logger.getLogger(EFactureEventHandlerImpl.class);

	private EFactureService eFactureService;
	private TiersService tiersService;
	private AdresseService adresseService;

	public void seteFactureService(EFactureService eFactureService) {
		this.eFactureService = eFactureService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	@Override
	public void handle(Demande demande) throws Exception {
		if (demande.getAction() == Demande.Action.DESINSCRIPTION) {
			LOGGER.info(String.format("Reçu demande de désinscription e-Facture (%s) du contribuable %s au %s", demande.getIdDemande(),
			                          FormatNumeroHelper.numeroCTBToDisplay(demande.getCtbId()), RegDateHelper.dateToDisplayString(demande.getDateDemande())));
		}
		else {
			LOGGER.info(String.format("Reçu demande d'inscription e-Facture (%s) du contribuable %s/%s au %s", demande.getIdDemande(), FormatNumeroHelper.numeroCTBToDisplay(demande.getCtbId()),
			                          FormatNumeroHelper.formatNumAVS(demande.getNoAvs()), RegDateHelper.dateToDisplayString(demande.getDateDemande())));

			final DestinataireAvecHisto histo = eFactureService.getDestinataireAvecSonHistorique(demande.getCtbId());
			final Tiers tiers = tiersService.getTiers(demande.getCtbId());

			// [SIFISC-9362] Si la demande n'est pas dans l'état "VALIDATION_EN_COURS", on ne peut pas la traiter
			// et donc il faut l'ignorer (cas par exemple d'une demande qui aurait été annulée entre temps)
			if (isStillValid(histo, demande.getIdDemande())) {
				final TypeRefusDemande refus = check(demande, tiers, histo);
				if (refus != null) {
					eFactureService.refuserDemande(demande.getIdDemande(), false, refus.getDescription());
					LOGGER.info(String.format("Demande d'inscription refusée : %s", refus));
				}
				else {
					// valide l'etat du contribuable et envoye le courrier adéquat
					final TypeAttenteDemande etatFinal;
					final TypeDocument typeDocument;
					final String description;

					final boolean okSignature = (histo == null || histo.getDernierEtat().getType() != TypeEtatDestinataire.DESINSCRIT_SUSPENDU)
							&& EFactureHelper.valideEtatFiscalContribuablePourInscription(tiers);
					if (okSignature) {
						etatFinal = TypeAttenteDemande.EN_ATTENTE_SIGNATURE;
						typeDocument = TypeDocument.E_FACTURE_ATTENTE_SIGNATURE;
						description = etatFinal.getDescription();
					}
					else {
						etatFinal = TypeAttenteDemande.EN_ATTENTE_CONTACT;
						typeDocument = TypeDocument.E_FACTURE_ATTENTE_CONTACT;
						description = String.format("%s Assujettissement incohérent avec la e-facture.", etatFinal.getDescription());
					}

					final String archivageId = eFactureService.imprimerDocumentEfacture(demande.getCtbId(), typeDocument, demande.getDateDemande());
					eFactureService.notifieMiseEnAttenteInscription(demande.getIdDemande(), etatFinal, description, archivageId, false);
					LOGGER.info(String.format("Demande d'inscription passée à l'état %s", etatFinal));
				}
			}
			else {
				LOGGER.info("Demande d'inscription ignorée car n'est pas dans l'état 'validation en cours'");
			}
		}
	}

	private TypeRefusDemande check(Demande demande, Tiers tiers, DestinataireAvecHisto histo) throws EvenementEfactureException {

		// Check Numéro AVS à 13 chiffres
		if (!AvsHelper.isValidNouveauNumAVS(demande.getNoAvs())) {
			return TypeRefusDemande.NUMERO_AVS_INVALIDE;
		}
		// Check Date et heure de la demande
		if (demande.getDateDemande() == null) {
			return TypeRefusDemande.DATE_DEMANDE_ABSENTE;
		}

		// Vérification de l'absence d'éventuelles autres demandes en cours
		if (hasAutreDemandeEnAttente(histo, demande.getIdDemande())) {
			return TypeRefusDemande.AUTRE_DEMANDE_EN_COURS_DE_TRAITEMENT;
		}

		// récupération du tiers
		if (tiers == null || !(tiers instanceof MenageCommun || tiers instanceof PersonnePhysique)) {
			return TypeRefusDemande.NUMERO_CTB_INCOHERENT;
		}

		// si le tiers a un numéro AVS connu dans Unireg, il doit correspondre avec celui de la demande
		if (isNavsRenseignePourTiers(tiers)) {
			final long noAvs;
			try {
				noAvs = AvsHelper.stringToLong(demande.getNoAvs());
			}
			catch (IllegalArgumentException e) {
				return TypeRefusDemande.NUMERO_AVS_INVALIDE;
			}
			final TypeRefusDemande refusDemande = controlerNumeroAVS(noAvs, tiers);
			if (refusDemande != null) {
				return refusDemande;
			}
		}

		// vérifier la présence d'une adresse courrier
		try {
			if (adresseService.getAdresseFiscale(tiers, TypeAdresseFiscale.COURRIER, null, false) == null) {
				return TypeRefusDemande.ADRESSE_COURRIER_INEXISTANTE;
			}
		}
		catch (AdresseException e) {
			throw new EvenementEfactureException("Impossible de déterminer l'adresse du contribuable " + demande.getCtbId(), e);
		}

		// tout va bien, on a un candidat !
		return null;
	}

	private static boolean hasAutreDemandeEnAttente(DestinataireAvecHisto histo, String idDemande) {
		boolean hasSome = false;
		if (histo != null) {
			for (DemandeAvecHisto demande : histo.getHistoriqueDemandes()) {
				if (!idDemande.equals(demande.getIdDemande())) {
					hasSome = demande.isEnAttente();
					if (hasSome) {
						break;
					}
				}
			}
		}
		return hasSome;
	}

	private static boolean isStillValid(DestinataireAvecHisto histo, String idDemande) {
		boolean valid = false;
		if (histo != null) {
			// on retrouve la demande dans l'historique nouvellement chargé
			DemandeAvecHisto demande = null;
			for (DemandeAvecHisto candidate : histo.getHistoriqueDemandes()) {
				if (idDemande.equals(candidate.getIdDemande())) {
					demande = candidate;
					break;
				}
			}

			// trouvée !
			valid = demande != null && demande.getDernierEtat().getType() == TypeEtatDemande.VALIDATION_EN_COURS;
		}
		return valid;
	}

	/**
	 * Permet de tester les conditions suivante issues du JIRA-7326
	 * Navs renseigné pour le ctb personne physique
	 * Navs renseigné pour les deux membres d'un ctb ménage commun
	 */
	private boolean isNavsRenseignePourTiers(Tiers tiers) {
		if (tiers instanceof MenageCommun) {
			final MenageCommun menage = (MenageCommun) tiers;
			for (PersonnePhysique pp : tiersService.getPersonnesPhysiques(menage)) {
				final String avs = tiersService.getNumeroAssureSocial(pp);
				if (StringUtils.isBlank(avs) ) {
					return false;
				}
			}
		}
		else if (tiers instanceof PersonnePhysique) {
			final String avs = tiersService.getNumeroAssureSocial((PersonnePhysique) tiers);
			if (StringUtils.isBlank(avs)) {
				return false;
			}
		}
		return true;
	}

	private TypeRefusDemande controlerNumeroAVS(long noAvs, Tiers tiers) {
		if (tiers instanceof MenageCommun) {
			boolean found = false;
			final MenageCommun menage = (MenageCommun)tiers;
			for (PersonnePhysique pp : tiersService.getPersonnesPhysiques(menage)) {
				final String avs = tiersService.getNumeroAssureSocial(pp);
				if (noAvs == AvsHelper.stringToLong(avs)) {
					found = true;
					break;
				}
			}
			if (!found) {
				return TypeRefusDemande.NUMERO_AVS_CTB_INCOHERENT;
			}
		}
		else if (tiers instanceof PersonnePhysique) {
			final String avs = tiersService.getNumeroAssureSocial((PersonnePhysique) tiers);
			if (noAvs != AvsHelper.stringToLong(avs)) {
				return TypeRefusDemande.NUMERO_AVS_CTB_INCOHERENT;
			}
		}
		return null;
	}

	@Override
	public ClassPathResource getRequestXSD() {
		return new ClassPathResource("eVD-0025-1-0.xsd");
	}
}
