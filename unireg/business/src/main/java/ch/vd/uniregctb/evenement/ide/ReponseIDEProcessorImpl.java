package ch.vd.uniregctb.evenement.ide;

import java.util.Collections;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEEnvoyee;
import ch.vd.unireg.interfaces.organisation.data.NumeroIDE;
import ch.vd.unireg.interfaces.organisation.data.StatutAnnonce;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.IdentificationEntreprise;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * @author Raphaël Marmier, 2016-10-06, <raphael.marmier@vd.ch>
 */
public class ReponseIDEProcessorImpl implements ReponseIDEProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(AnnonceIDEService.class);

	private TiersService tiersService;
	private AnnonceIDEService annonceIDEService;
	private ReferenceAnnonceIDEDAO referenceAnnonceIDEDAO;
	private ServiceIDEService serviceIDEService;
	private AdresseService adresseService;
	private ServiceInfrastructureService infraService;
	private ServiceOrganisationService serviceOrganisation;

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setAnnonceIDEService(AnnonceIDEService annonceIDEService) {
		this.annonceIDEService = annonceIDEService;
	}

	public void setReferenceAnnonceIDEDAO(ReferenceAnnonceIDEDAO referenceAnnonceIDEDAO) {
		this.referenceAnnonceIDEDAO = referenceAnnonceIDEDAO;
	}

	public void setServiceIDEService(ServiceIDEService serviceIDEService) {
		this.serviceIDEService = serviceIDEService;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setServiceInfra(ServiceInfrastructureService serviceInfra) {
		this.infraService = serviceInfra;
	}

	public void setServiceOrganisation(ServiceOrganisationService serviceOrganisation) {
		this.serviceOrganisation = serviceOrganisation;
	}

	@Override
	public void traiterReponseAnnonceIDE(AnnonceIDEEnvoyee annonceIDE) throws ReponseIDEProcessorException {

		final ReferenceAnnonceIDE referenceAnnonceIDE = referenceAnnonceIDEDAO.get(annonceIDE.getNumero());
		if (referenceAnnonceIDE == null) {
			throw new ReponseIDEProcessorException(
					String.format("Fatal: l'annonce à l'IDE numéro %d jointe à la réponse ne correspond à aucune annonce à l'IDE émise par Unireg!", annonceIDE.getNumero()));
		}
		final RegDate date = RegDateHelper.get(annonceIDE.getDateAnnonce());
		final Etablissement etablissement = referenceAnnonceIDE.getEtablissement();

		final StatutAnnonce statut = annonceIDE.getStatut().getStatut();
		if (statut == null) {
			throw new ReponseIDEProcessorException(
					String.format("Fatal: impossible de déterminer le statut de l'annonce à l'IDE numéro %d jointe à la réponse!", annonceIDE.getNumero()));
		}

		if (statut == StatutAnnonce.ACCEPTE_IDE || statut == StatutAnnonce.TRANSMIS) {
			Objects.requireNonNull(annonceIDE.getNoIde(),
			                       String.format("Fatal: l'annonce à l'IDE numéro %d jointe à la réponse est de statut %s mais ne contient pas le numéro IDE de l'entité concernée.",
			                                     annonceIDE.getNumero(), statut)
			);
			final Entreprise entreprise = tiersService.getEntreprise(etablissement, date);
			if (entreprise == null) {
				throw new ReponseIDEProcessorException(
						String.format("Fatal: impossible de retrouver l'entreprise visée par l'annonce à l'IDE numéro %d à la date de l'annonce %s.", annonceIDE.getNumero(),
						              DateHelper.dateTimeToDisplayString(annonceIDE.getDateAnnonce())));
			}
			final Etablissement etablissementPrincipal = tiersService.getEtablissementPrincipal(entreprise, date);
			if (etablissementPrincipal == null) {
				throw new ReponseIDEProcessorException(
						String.format("Fatal: impossible de retrouver l'établissement principal visée par l'annonce à l'IDE numéro %d à la date de l'annonce %s.", annonceIDE.getNumero(),
						              DateHelper.dateTimeToDisplayString(annonceIDE.getDateAnnonce())));
			}
			/*
				On est en train d'agir sur un établissement principal.
			 */
			if (etablissement.getNumero().equals(etablissementPrincipal.getNumero())) {

				if (etablissementPrincipal.getNumeroEtablissement() == null) {
					final IdentificationEntreprise identEtablissement = new IdentificationEntreprise(annonceIDE.getNoIde().getValeur());
					etablissementPrincipal.setIdentificationsEntreprise(Collections.singleton(identEtablissement));
					Audit.info(annonceIDE.getNumero(), String.format("Numéro IDE %s assigné au tiers n°%s établissement principal non encore apparié.",
					                                                 identEtablissement.getNumeroIde(),
					                                                 FormatNumeroHelper.numeroCTBToDisplay(etablissement.getNumero())));
				}
				if (entreprise.getNumeroEntreprise() == null) {
					final IdentificationEntreprise identEntreprise = new IdentificationEntreprise(annonceIDE.getNoIde().getValeur());
					entreprise.setIdentificationsEntreprise(Collections.singleton(identEntreprise));
					Audit.info(annonceIDE.getNumero(), String.format("Numéro IDE %s assigné au tiers n°%s entreprise non encore apparié.",
					                                                 identEntreprise.getNumeroIde(),
					                                                 FormatNumeroHelper.numeroCTBToDisplay(etablissement.getNumero())));
				}
			}
			/*
				On est en train d'agir sur un établissement secondaire. (non supporté par Unireg)
			 */
			else {
				throw new ReponseIDEProcessorException(
						String.format("Fatal: l'établissement n°%s sur lequel porte l'annonce n'est pas établissement principal à la date de l'annonce [%s].",
						              FormatNumeroHelper.numeroCTBToDisplay(etablissement.getNumero()),
						              DateHelper.dateTimeToDisplayString(annonceIDE.getDateAnnonce())));
			}
		}
		else if (statut == StatutAnnonce.REFUSE_IDE) {
			final NumeroIDE noIdeRemplacant = annonceIDE.getNoIdeRemplacant();
			/* Cas de dédoublonnage */
			if (noIdeRemplacant != null) {
				Audit.warn(annonceIDE.getNumero(),
				           String.format("Refus de l'IDE pour cause de doublon: numéro IDE de remplacement %s. Vérifier si le tiers n°%s n'a pas été créé à double dans Unireg. ",
				                         noIdeRemplacant.getValeur(),
				                         FormatNumeroHelper.numeroCTBToDisplay(etablissement.getNumero())));
			}
			/* Cas d'erreur */
			else {
				Audit.warn(annonceIDE.getNumero(), String.format("Refus de l'IDE pour cause d'erreur. La création ou modification sur l'établissement n°%s a été rejetée. (Voir les erreurs ci-dessus)",
				                                                 FormatNumeroHelper.numeroCTBToDisplay(etablissement.getNumero())
				           )
				);
			}
		}
		else if (statut == StatutAnnonce.REJET_RCENT) {
			Audit.warn(annonceIDE.getNumero(), String.format("Rejet de demande d'annonce portant sur l'établissement n°%s par RCEnt. (Voir les erreurs ci-dessus)",
			                                                 FormatNumeroHelper.numeroCTBToDisplay(etablissement.getNumero())
			           )
			);
		}
		else {
			Audit.warn(annonceIDE.getNumero(), String.format("L'annonce à l'IDE portant sur l'établissement n°%s par RCEnt est passée à l'état %s. Aucune action à entreprendre.",
			                                                 FormatNumeroHelper.numeroCTBToDisplay(etablissement.getNumero()),
			                                                 statut
			           )
			);
		}
	}
}
