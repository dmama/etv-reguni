package ch.vd.uniregctb.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.orm.hibernate3.HibernateTemplate;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersSearcher;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.tiers.Contribuable;

/**
 * Classe de base des containers de résultats pour les rapports d'exécution des batchs
 */
public abstract class JobResults<E, R extends JobResults> implements BatchResults<E, R> {

	public static final String EXCEPTION_DESCRIPTION = "Une exception est apparue pendant le traitement du contribuable, veuillez en informer le chef de projet Unireg";

	/**
	 * Classe de base des informations dumpées dans les rapports d'exécution
	 */
	public static abstract class Info {
		public final long noCtb;
		public final Integer officeImpotID;
		public final String details;
		public final String nomCtb;

		public Info(long noCtb, Integer officeImpotID, String details) {
			this.noCtb = noCtb;
			this.officeImpotID = officeImpotID;
			this.details = details;
			this.nomCtb = getNom(noCtb);
		}

		public abstract String getDescriptionRaison();
	}

	private static AdresseService adresseService;
	private static GlobalTiersSearcher tiersSearcher;
	private static HibernateTemplate hibernateTemplate;

	public void setAdresseService(AdresseService adresseService) {
		JobResults.adresseService = adresseService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTiersSearcher(GlobalTiersSearcher tiersSearcher) {
		JobResults.tiersSearcher = tiersSearcher;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		JobResults.hibernateTemplate = hibernateTemplate;
	}

	/**
	 * Retourne le nom et le prénom du contribuable spécifié. S'il s'agit d'un contribuable ménage commun et que les deux parties sont connues, la liste retournée contient les deux noms des parties.
	 *
	 * @param noCtb le numéro de contribuable
	 * @return une liste avec 1 nom (majorité des cas) ou 2 noms (contribuables ménage commun)
	 */
	private static List<String> getNoms(long noCtb) {

		List<String> noms;

		// on essaie en premier de récupérer l'info de l'indexeur (= plus rapide)
		final TiersIndexedData data = tiersSearcher.get(noCtb);

		if (data == null) {
			// rien trouvé dans l'indexeur => on se rabat sur le service
			final Contribuable tiers = (Contribuable) hibernateTemplate.get(Contribuable.class, noCtb);
			if (tiers == null) {
				noms = Collections.emptyList();
			}
			else {
				try {
					noms = adresseService.getNomCourrier(tiers, null, false);
				}
				catch (Exception e) {
					noms = new ArrayList<String>(1);
					noms.add(e.getMessage()); // rien de mieux à faire ici
				}
			}
		}
		else {
			// données de l'indexeur
			noms = new ArrayList<String>(2);
			noms.add(data.getNom1().trim());
			final String nom2 = data.getNom2();
			if (nom2 != null && !nom2.trim().equals("")) {
				noms.add(nom2.trim());
			}
		}

		return noms;
	}

	/**
	 * Retourne le nom et prénom du contribuable (ou des deux parties dans le cas d'un ménage commun)
	 *
	 * @param noCtb le numéro de contribuable
	 * @return le nom et le prénom du contribuable.
	 */
	private static String getNom(long noCtb) {

		String nom;

		final List<String> noms = getNoms(noCtb);
		if (noms.size() == 1) { // 90% des cas
			nom = noms.get(0);
		}
		else {
			nom = null;
			for (int i = 0; i < noms.size(); ++i) {
				if (i == 0) {
					nom = noms.get(0);
				}
				else {
					nom += (" & " + noms.get(i));
				}
			}
		}

		return (nom == null ? "" : nom.trim());
	}

	/**
	 * Heure de démarrage du job (à la milliseconde près).
	 */
	public final long startTime;

	/**
	 * Heure d'arrêt du job (à la milliseconde près).
	 */
	public long endTime = 0;

	public JobResults() {
		this.startTime = System.currentTimeMillis();
	}

	public void end() {
		this.endTime = System.currentTimeMillis();
	}
}
