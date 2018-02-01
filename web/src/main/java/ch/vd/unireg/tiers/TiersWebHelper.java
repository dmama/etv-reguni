package ch.vd.unireg.tiers;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.adresse.AdresseService;

public abstract class TiersWebHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(TiersWebHelper.class);

	/**
	 * Construit et retourne une string qui résume de manière compréhensible pour un humain un rapport entre deux tiers.
	 *
	 * @param rapport le rapport dont on veut obtenir un résumé
	 * @return un résumé du rapport; ou <b>null</b> s'il n'est pas possible de le créer pour une raison ou une autre.
	 */
	public static String getRapportEntreTiersTooltips(RapportEntreTiers rapport, AdresseService adresseService, TiersService tiersService) {

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
			return String.format("%s %s le tiers référent de %s", nomSujet, fermeOuAnnule ? "était" : "est", nomObjet);
		}
		else if (rapport instanceof RepresentationConventionnelle) {
			return String.format("%s %s représenté(e) par %s", nomSujet, fermeOuAnnule ? "était" : "est", nomObjet);
		}
		else if (rapport instanceof Curatelle) {
			return String.format("%s %s le curateur de %s", nomObjet, fermeOuAnnule ? "était" : "est", nomSujet);
		}
		else if (rapport instanceof ConseilLegal) {
			return String.format("%s %s le conseiller légal de %s", nomObjet, fermeOuAnnule ? "était" : "est", nomSujet);
		}
		else if (rapport instanceof Tutelle) {
			return String.format("%s %s le tuteur de %s", nomObjet, fermeOuAnnule ? "était" : "est", nomSujet);
		}
		else if (rapport instanceof AnnuleEtRemplace) {
			return String.format("%s (n°%d) %s %s (n°%d)", nomObjet, objetId, fermeOuAnnule ? "remplaçait" : "remplace", nomSujet, sujetId);
		}
		else if (rapport instanceof AppartenanceMenage) {
			return String.format("%s %s au ménage %s", nomSujet, fermeOuAnnule ? "appartenait" : "appartient", nomObjet);
		}
		else if (rapport instanceof RapportPrestationImposable) {
			return String.format("%s %s employé(e) par %s", nomSujet, fermeOuAnnule ? "était" : "est", nomObjet);
		}
		else if (rapport instanceof Parente) {
			return String.format("%s %s l'enfant de %s", nomSujet, fermeOuAnnule ? "était" : "est", nomObjet);
		}
		else if (rapport instanceof AssujettissementParSubstitution) {
			return String.format("L'assujettissement de %s se %s à celui de %s", nomObjet, fermeOuAnnule ? "substituait" : "substitue", nomSujet);
		}
		else if (rapport instanceof ActiviteEconomique) {
			if (((ActiviteEconomique) rapport).isPrincipal()) {
				return String.format("%s %s son activité économique principale au travers de l'établissement %s", nomSujet, fermeOuAnnule ? "exerçait" : "exerce", nomObjet);
			}
			else {
				return String.format("%s %s une activité économique au travers de l'établissement %s", nomSujet, fermeOuAnnule ? "exerçait" : "exerce", nomObjet);
			}
		}
		else if (rapport instanceof Mandat) {
			return String.format("%s %s un mandat à %s", nomSujet, fermeOuAnnule ? "confiait" : "confie", nomObjet);
		}
		else if (rapport instanceof FusionEntreprises) {
			return String.format("%s (n°%d) %s fusionné pour donner %s (n°%d)", nomSujet, sujetId, fermeOuAnnule ? "avait" : "a", nomObjet, objetId);
		}
		else if (rapport instanceof ScissionEntreprise) {
			return String.format("%s (n°%d) %s été scindée pour donner %s (n°%d)", nomSujet, sujetId, fermeOuAnnule ? "avait" : "a", nomObjet, objetId);
		}
		else if (rapport instanceof TransfertPatrimoine) {
			return String.format("%s (n°%d) %s transféré du patrimoine vers %s (n°%d)", nomSujet, sujetId, fermeOuAnnule ? "avait" : "a", nomObjet, objetId);
		}
		else if (rapport instanceof AdministrationEntreprise) {
			return String.format("L'entreprise '%s' %s administrée par %s", nomSujet, fermeOuAnnule ? "était" : "est", nomObjet);
		}
		else if (rapport instanceof SocieteDirection) {
			return String.format("%s %s propriétaire du fonds de placement %s", nomSujet, fermeOuAnnule ? "était" : "est", nomObjet);
		}
		else if (rapport instanceof Heritage) {
			return String.format("%s %s hérité de %s", nomSujet, fermeOuAnnule ? "avait" : "a", nomObjet);
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
