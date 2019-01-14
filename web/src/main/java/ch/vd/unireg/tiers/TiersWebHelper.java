package ch.vd.unireg.tiers;

import java.util.List;

import org.apache.commons.lang.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.message.MessageHelper;

public abstract class TiersWebHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(TiersWebHelper.class);

	/**
	 * Construit et retourne une string qui résume de manière compréhensible pour un humain un rapport entre deux tiers.
	 *
	 * @param rapport       le rapport dont on veut obtenir un résumé
	 * @param messageHelper messageHelper
	 * @return un résumé du rapport; ou <b>null</b> s'il n'est pas possible de le créer pour une raison ou une autre.
	 */
	public static String getRapportEntreTiersTooltips(RapportEntreTiers rapport, AdresseService adresseService, TiersService tiersService, MessageHelper messageHelper) {

		final Long sujetId = rapport.getSujetId();
		final Long objetId = rapport.getObjetId();
		final Tiers sujet = tiersService.getTiers(sujetId);
		final Tiers objet = tiersService.getTiers(objetId);

		final String nomSujet;
		final String nomObjet;
		try {
			nomSujet = getNomCourrierPlat(sujet, adresseService);
			nomObjet = getNomCourrierPlat(objet, adresseService);
		}
		catch (Exception e) {
			LOGGER.warn("Exception catchée pendant le calcul des tooltips", e);
			return null;
		}

		if (nomSujet == null || nomObjet == null) {
			return null;
		}

		final boolean fermeOuAnnule = rapport.isAnnule() || rapport.getDateFin() != null;

		if (rapport instanceof ContactImpotSource) {
			return messageHelper.getMessage("tooltip.rapport.entretiers.contactimpotsource", nomObjet, BooleanUtils.toInteger(fermeOuAnnule), nomSujet);
		}
		else if (rapport instanceof RepresentationConventionnelle) {
			return messageHelper.getMessage("tooltip.rapport.entretiers.representationconventionnelle", nomObjet, BooleanUtils.toInteger(fermeOuAnnule), nomSujet);
		}
		else if (rapport instanceof Curatelle) {
			return messageHelper.getMessage("tooltip.rapport.entretiers.curatelle", nomObjet, BooleanUtils.toInteger(fermeOuAnnule), nomSujet);
		}
		else if (rapport instanceof ConseilLegal) {
			return messageHelper.getMessage("tooltip.rapport.entretiers.conseillegal", nomObjet, BooleanUtils.toInteger(fermeOuAnnule), nomSujet);
		}
		else if (rapport instanceof Tutelle) {
			return messageHelper.getMessage("tooltip.rapport.entretiers.tutelle", nomObjet, BooleanUtils.toInteger(fermeOuAnnule), nomSujet);
		}
		else if (rapport instanceof AnnuleEtRemplace) {
			return messageHelper.getMessage("tooltip.rapport.entretiers.annuleetremplace", nomObjet, objetId, BooleanUtils.toInteger(fermeOuAnnule), nomSujet, sujetId);
		}
		else if (rapport instanceof AppartenanceMenage) {
			return messageHelper.getMessage("tooltip.rapport.entretiers.appartenancemenage", nomSujet, BooleanUtils.toInteger(fermeOuAnnule), nomObjet);
		}
		else if (rapport instanceof RapportPrestationImposable) {
			return messageHelper.getMessage("tooltip.rapport.entretiers.rapportprestationimposable", nomSujet, BooleanUtils.toInteger(fermeOuAnnule), nomObjet);
		}
		else if (rapport instanceof Parente) {
			return messageHelper.getMessage("tooltip.rapport.entretiers.parente", nomSujet, BooleanUtils.toInteger(fermeOuAnnule), nomObjet);
		}
		else if (rapport instanceof AssujettissementParSubstitution) {
			return messageHelper.getMessage("tooltip.rapport.entretiers.assujettissementparsubstitution", nomSujet, BooleanUtils.toInteger(fermeOuAnnule), nomObjet);
		}
		else if (rapport instanceof ActiviteEconomique) {
			return messageHelper.getMessage("tooltip.rapport.entretiers.activiteeconomique", nomSujet, BooleanUtils.toInteger(fermeOuAnnule), nomObjet, BooleanUtils.toInteger(((ActiviteEconomique) rapport).isPrincipal()));
		}
		else if (rapport instanceof Mandat) {
			return messageHelper.getMessage("tooltip.rapport.entretiers.mandat", nomSujet, BooleanUtils.toInteger(fermeOuAnnule), nomObjet);
		}
		else if (rapport instanceof FusionEntreprises) {
			return messageHelper.getMessage("tooltip.rapport.entretiers.fusionentreprises", nomSujet, sujetId, BooleanUtils.toInteger(fermeOuAnnule), nomObjet, objetId);
		}
		else if (rapport instanceof ScissionEntreprise) {
			return messageHelper.getMessage("tooltip.rapport.entretiers.scissionentreprise", nomSujet, sujetId, BooleanUtils.toInteger(fermeOuAnnule), nomObjet, objetId);
		}
		else if (rapport instanceof TransfertPatrimoine) {
			return messageHelper.getMessage("tooltip.rapport.entretiers.transfertpatrimoine", nomSujet, sujetId, BooleanUtils.toInteger(fermeOuAnnule), nomObjet, objetId);
		}
		else if (rapport instanceof AdministrationEntreprise) {
			return messageHelper.getMessage("tooltip.rapport.entretiers.administrationentreprise", nomSujet, BooleanUtils.toInteger(fermeOuAnnule), nomObjet);
		}
		else if (rapport instanceof SocieteDirection) {
			return messageHelper.getMessage("tooltip.rapport.entretiers.societedirection", nomSujet, BooleanUtils.toInteger(fermeOuAnnule), nomObjet);
		}
		else if (rapport instanceof Heritage) {
			return messageHelper.getMessage("tooltip.rapport.entretiers.heritage", nomSujet, BooleanUtils.toInteger(fermeOuAnnule), nomObjet);
		}
		else if (rapport instanceof LienAssociesEtSNC) {
			return messageHelper.getMessage("tooltip.rapport.entretiers.lienassociesetsnc", nomObjet, BooleanUtils.toInteger(fermeOuAnnule), nomSujet);
		}
		else {
			throw new IllegalArgumentException("Type de rapport-entre-tiers inconnu = [" + rapport.getClass() + ']');
		}
	}

	private static String getNomCourrierPlat(Tiers tiers, AdresseService adresseService) throws AdresseException {

		final List<String> noms = adresseService.getNomCourrier(tiers, null, false);
		if (noms == null || noms.isEmpty()) {
			return null;
		}

		if (noms.size() == 1) {
			return noms.get(0);
		}
		else {
			final StringBuilder b = new StringBuilder(noms.get(0));
			for (int i = 1; i < noms.size(); ++i) {
				b.append(" / ").append(noms.get(i));
			}
			return b.toString();
		}
	}
}
