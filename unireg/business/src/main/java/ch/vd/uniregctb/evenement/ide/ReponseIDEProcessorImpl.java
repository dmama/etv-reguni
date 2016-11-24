package ch.vd.uniregctb.evenement.ide;

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
import ch.vd.uniregctb.tiers.TiersException;
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
		final Entreprise entreprise = tiersService.getEntreprise(etablissement, date);

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
				try {
					if (etablissementPrincipal.getNumeroEtablissement() == null) {
						Audit.info(annonceIDE.getNumero(), String.format("Numéro IDE %s assigné au tiers établissement n°%s principal non encore apparié.",
						                                                 annonceIDE.getNoIde().getValeur(),
						                                                 FormatNumeroHelper.numeroCTBToDisplay(etablissement.getNumero())));
						tiersService.setIdentifiantEntreprise(etablissementPrincipal, annonceIDE.getNoIde().getValeur());
					}
					if (entreprise.getNumeroEntreprise() == null) {
						Audit.info(annonceIDE.getNumero(), String.format("Numéro IDE %s assigné au tiers entreprise n°%s non encore apparié.",
						                                                 annonceIDE.getNoIde().getValeur(),
						                                                 FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero())));
						tiersService.setIdentifiantEntreprise(entreprise, annonceIDE.getNoIde().getValeur());
					}
				}
				catch (TiersException e) {
					throw new ReponseIDEProcessorException("Impossible d'assigner le numéro IDE à l'entreprise: " + e.getMessage(), e);
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
				           String.format("Refus de l'IDE pour cause de doublon: numéro IDE de remplacement %s. Vérifier si l'entreprise n°%s n'a pas été créé à double dans Unireg. ",
				                         noIdeRemplacant.getValeur(),
				                         FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero())));
			}
			/* Cas d'erreur */
			else {
				Audit.warn(annonceIDE.getNumero(), String.format("Refus de l'IDE pour cause d'erreur. L'inscription ou la modification de l'entreprise n°%s a été rejetée.",
				                                                 FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero())
				           )
				);
			}
		}
		else if (statut == StatutAnnonce.REJET_RCENT) {
			Audit.warn(annonceIDE.getNumero(), String.format("Rejet de demande d'annonce portant sur l'entreprise n°%s par RCEnt.",
			                                                 FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero())
			           )
			);
		}
		else {
			Audit.warn(annonceIDE.getNumero(), String.format("L'annonce à l'IDE portant sur l'entreprise n°%s par RCEnt est passée à l'état %s. Aucune action à entreprendre.",
			                                                 FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()),
			                                                 statut
			           )
			);
		}
	}
}
