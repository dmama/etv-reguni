package ch.vd.uniregctb.efacture;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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

	protected static enum TypeRefusDemande {

		NUMERO_AVS_INVALIDE("Numéro AVS invalide."),
		AUTRE_DEMANDE_EN_COURS_DE_TRAITEMENT("Une autre demande est déjà en cours de traitement."),
		DATE_DEMANDE_ABSENTE("Date de la demande non renseignée."),
		NUMERO_CTB_INCOHERENT("Numéro de contribuable incohérent."),
		NUMERO_AVS_CTB_INCOHERENT("Numéro AVS incohérent avec le numéro de contribuable."),
		ADRESSE_COURRIER_INEXISTANTE("Aucune adresse courrier pour ce contribuable.");

		private final String description;

		private TypeRefusDemande(String description) {
			this.description = description;
		}

		public String getDescription() {
			return description;
		}
	}

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

					final String archivageId = eFactureService.imprimerDocumentEfacture(demande.getCtbId(), typeDocument, demande.getDateDemande(), demande.getNoAdherent(), null, null);
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

		// vérification du tiers et de la correspondance avec le navs
		final Set<Long> noAvsRegistre = findNavsDansRegistre(tiers);
		if (noAvsRegistre.isEmpty()) {
			return TypeRefusDemande.NUMERO_CTB_INCOHERENT;
		}

		// si le set contient "null", on ne va pas plus loin dans le test sur le numéro AVS puisqu'un au moins est inconnu dans le registre
		if (!noAvsRegistre.contains(null)) {
			final long noAvsDemande = AvsHelper.stringToLong(demande.getNoAvs());
			if (!noAvsRegistre.contains(noAvsDemande)) {
				return TypeRefusDemande.NUMERO_AVS_CTB_INCOHERENT;
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
	 * @param tiers tiers de référence
	 * @return l'ensemble de tous les numéros AVS des personnes physiques qui constituent ce tiers (<code>null</code> compris si au moins l'un de ces tiers n'a pas de numéro AVS connu)
	 */
	private Set<Long> findNavsDansRegistre(Tiers tiers) {
		final Set<Long> list;
		if (tiers instanceof PersonnePhysique) {
			final String avsStr = tiersService.getNumeroAssureSocial((PersonnePhysique) tiers);
			final Long noAvs = StringUtils.isBlank(avsStr) ? null : AvsHelper.stringToLong(avsStr);
			list = new HashSet<>(Arrays.asList(noAvs));
		}
		else if (tiers instanceof MenageCommun) {
			final Set<PersonnePhysique> pps = tiersService.getPersonnesPhysiques((MenageCommun) tiers);
			list = new HashSet<>(pps.size());
			for (PersonnePhysique pp : pps) {
				list.addAll(findNavsDansRegistre(pp));
			}
		}
		else {
			list = Collections.emptySet();
		}
		return list;
	}

	@Override
	public ClassPathResource getRequestXSD() {
		return new ClassPathResource("eVD-0025-1-1.xsd");
	}
}
