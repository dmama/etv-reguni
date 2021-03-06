package ch.vd.unireg.evenement.ide;

import java.util.Objects;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.audit.AuditManager;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.interfaces.entreprise.data.AnnonceIDEEnvoyee;
import ch.vd.unireg.interfaces.entreprise.data.NumeroIDE;
import ch.vd.unireg.interfaces.entreprise.data.StatutAnnonce;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.TiersService;

/**
 * @author Raphaël Marmier, 2016-10-06, <raphael.marmier@vd.ch>
 */
public class ReponseIDEProcessorImpl implements ReponseIDEProcessor {

	private TiersService tiersService;
	private ReferenceAnnonceIDEDAO referenceAnnonceIDEDAO;
	private AuditManager audit;

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setReferenceAnnonceIDEDAO(ReferenceAnnonceIDEDAO referenceAnnonceIDEDAO) {
		this.referenceAnnonceIDEDAO = referenceAnnonceIDEDAO;
	}

	public void setAudit(AuditManager audit) {
		this.audit = audit;
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
				if (etablissementPrincipal.getNumeroEtablissement() == null) {
					audit.info(annonceIDE.getNumero(), String.format("Numéro IDE %s assigné au tiers établissement n°%s principal non encore apparié.",
					                                                 annonceIDE.getNoIde().getValeur(),
					                                                 FormatNumeroHelper.numeroCTBToDisplay(etablissement.getNumero())));
					tiersService.setIdentifiantEntreprise(etablissementPrincipal, annonceIDE.getNoIde().getValeur());
				}
				if (entreprise.getNumeroEntreprise() == null) {
					audit.info(annonceIDE.getNumero(), String.format("Numéro IDE %s assigné au tiers entreprise n°%s non encore apparié.",
					                                                 annonceIDE.getNoIde().getValeur(),
					                                                 FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero())));
					tiersService.setIdentifiantEntreprise(entreprise, annonceIDE.getNoIde().getValeur());
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
				audit.warn(annonceIDE.getNumero(),
				           String.format("Refus de l'IDE pour cause de doublon: numéro IDE de remplacement %s. Vérifier si l'entreprise n°%s n'a pas été créé à double dans Unireg. ",
				                         noIdeRemplacant.getValeur(),
				                         FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero())));
			}
			/* Cas d'erreur */
			else {
				audit.warn(annonceIDE.getNumero(), String.format("Refus de l'IDE pour cause d'erreur. L'inscription ou la modification de l'entreprise n°%s a été rejetée.",
				                                                 FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero())
				           )
				);
			}
		}
		else if (statut == StatutAnnonce.REJET_RCENT) {
			audit.warn(annonceIDE.getNumero(), String.format("Rejet de demande d'annonce portant sur l'entreprise n°%s par RCEnt.",
			                                                 FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero())
			           )
			);
		}
		else {
			audit.warn(annonceIDE.getNumero(), String.format("L'annonce à l'IDE portant sur l'entreprise n°%s par RCEnt est passée à l'état %s. Aucune action à entreprendre.",
			                                                 FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()),
			                                                 statut
			           )
			);
		}
	}
}
