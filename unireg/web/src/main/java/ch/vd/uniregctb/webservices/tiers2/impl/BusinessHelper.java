package ch.vd.uniregctb.webservices.tiers2.impl;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;

public class BusinessHelper {

	/**
	 * @return retourne la raison sociale du débiteur spécifié.
	 */
	public static String getRaisonSociale(final DebiteurPrestationImposable debiteur, RegDate date, AdresseService service) {

		if (date == null) {
			date = RegDate.get();
		}

		String raison = "";
		try {
			List<String> list = service.getNomCourrier(debiteur, date);
			if (!list.isEmpty()) {
				raison = list.get(0); // on ignore joyeusement une éventuelle deuxième ligne
			}
		}
		catch (AdressesResolutionException e) {
			// Si on a une exception on renvoie une raison sociale nulle
			raison = "";
		}
		return raison;
	}

//	public static String getRaisonSociale(final DebiteurPrestationImposable debiteur, TiersService tiersService, ServicePersonneMoraleService servicePM) {
//
//		final Contribuable contribuable = debiteur.getContribuable();
//
//		if (contribuable instanceof Habitant) {
//			final Habitant habitant = (Habitant) contribuable;
//			final Individu individu = tiersService.getServiceCivilService().getIndividu(habitant.getNumeroIndividu(), 2400);
//			final HistoriqueIndividu truc = individu.getDernierHistoriqueIndividu();
//			return truc.getPrenom() + " " + truc.getNom();
//		}
//		else if (contribuable instanceof NonHabitant) {
//			final NonHabitant nonHabitant = (NonHabitant) contribuable;
//			return nonHabitant.getPrenom() + " " + nonHabitant.getNom();
//		}
//		else if (contribuable instanceof Entreprise) {
//			final Entreprise entreprise = (Entreprise) contribuable;
//			final PersonneMorale pm = servicePM.getPersonneMorale(entreprise.getNumeroEntreprise());
//			return pm.getRaisonSociale();
//		}
//		else if (contribuable instanceof AutreCommunaute) {
//			final ch.vd.uniregctb.tiers.AutreCommunaute communaute = (ch.vd.uniregctb.tiers.AutreCommunaute) contribuable;
//			return communaute.getNom();
//		}
//		else if (contribuable instanceof CollectiviteAdministrative) {
//			final CollectiviteAdministrative collectivite = (CollectiviteAdministrative) contribuable;
//			final ch.vd.infrastructure.model.CollectiviteAdministrative ca;
//			try {
//				ca = tiersService.getServiceInfra().getCollectivite(collectivite.getNumeroCollectiviteAdministrative().intValue());
//			}
//			catch (InfrastructureException e) {
//				throw new RuntimeException(e);
//			}
//			return ca.getNomComplet1();
//		}
//		else {
//			throw new IllegalArgumentException("Type de contribuable inconnu = [" + contribuable.getClass().getSimpleName() + "]");
//		}
//	}
}
