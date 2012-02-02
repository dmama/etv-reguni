package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.commons.lang.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.GentilIterator;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.evenement.externe.EtatEvenementExterne;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.uniregctb.stats.evenements.StatistiqueEvenementInfo;
import ch.vd.uniregctb.stats.evenements.StatsEvenementsCivilsEchResults;
import ch.vd.uniregctb.stats.evenements.StatsEvenementsCivilsRegPPResults;
import ch.vd.uniregctb.stats.evenements.StatsEvenementsExternesResults;
import ch.vd.uniregctb.stats.evenements.StatsEvenementsIdentificationContribuableResults;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

/**
 * Rapport des statistiques des événements reçus par Unireg
 */
public class PdfStatistiquesEvenementsRapport extends PdfRapport {

	/**
	 * Renvoie la représentation décimale d'un entier, ou de la valeur indiquée comme valeur par défaut si cet entier est <code>null</code>
	 * @param value valeur à représenter
	 * @param defaut valeur à utiliser si <code>value<code> est <code>null</code>
	 * @return représentation décimale de l'entier
	 */
	private static String toStringInt(Integer value, int defaut) {
		return Integer.toString(value != null ? value : defaut);
	}

	public void write(final StatsEvenementsCivilsRegPPResults civilsRegPP, final StatsEvenementsCivilsEchResults civilsEch,
	                  final StatsEvenementsExternesResults externes, final StatsEvenementsIdentificationContribuableResults identCtb,
	                  final RegDate dateReference, String nom, String description, final Date dateGeneration, OutputStream os,
	                  StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Statistiques des événements");

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, new PdfRapport.TableSimpleCallback() {
				@Override
				public void fillTable(PdfTableSimple table) throws DocumentException {
					table.addLigne("Evénements civils:", String.valueOf(civilsRegPP != null || civilsEch != null));
					table.addLigne("Evénements externes:", String.valueOf(externes != null));
					table.addLigne("Evénements identification:", String.valueOf(identCtb != null));
					table.addLigne("Date de référence:", RegDateHelper.dateToDisplayString(dateReference));
					table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
				}
			});
		}

		if (civilsRegPP != null) {

			addEntete1("Evénements civils (RegPP)");

			// Evénements civils : états
			{
				addTableSimple(2, new PdfRapport.TableSimpleCallback() {
					@Override
					public void fillTable(PdfTableSimple table) throws DocumentException {

						table.addLigne("Etat", "Total");
						table.setHeaderRows(1);

						final Map<EtatEvenementCivil, Integer> etats = civilsRegPP.getEtats();
						for (EtatEvenementCivil etat : EtatEvenementCivil.values()) {
							final Integer nombre = etats.get(etat);
							final String total = toStringInt(nombre, 0);
							table.addLigne(String.format("%s", etat.toString()), total);
						}
					}
				});
			}

			// événements civils : types en erreur
			{
				final String filename = "erreurs_evts_civils_regpp_par_type.csv";
				final String contenu = asCsvFile(civilsRegPP.getErreursParType(), null, dateReference, TypeEvenementCivil.class, filename, status);
				final String titre = "Erreurs des événements civils RegPP par type d'événement";
				final String listVide = "(aucune)";
				addListeDetaillee(writer, civilsRegPP.getErreursParType().size(), titre, listVide, filename, contenu);
			}

			// messages d'erreur regroupés par type
			{
				final String filename = "messages_erreurs_evts_civils_regpp_par_type.csv";
				final String contenu = buildStatsMessagesErreursRegPPParType(civilsRegPP.getToutesErreurs(), filename, status);
				final String titre = "Messages d'erreurs des événements civils RegPP par type d'événement";
				final String listVide = "(aucun)";
				addListeDetaillee(writer, civilsRegPP.getErreursParType().size(), titre, listVide, filename, contenu);
			}

			// toutes les erreurs
			{
				final String filename = "erreurs_evts_civils_regpp.csv";
				final String contenu = asCsvFile(civilsRegPP.getToutesErreurs(), filename, status);
				final String titre = "Erreurs des événements civils RegPP";
				final String listVide = "(aucune)";
				addListeDetaillee(writer, civilsRegPP.getToutesErreurs().size(), titre, listVide, filename, contenu);
			}
		}

		if (civilsEch != null) {

			addEntete1("Evénements civils (e-CH)");

			// Evénements civils : états
			{
				addTableSimple(3, new PdfRapport.TableSimpleCallback() {
					@Override
					public void fillTable(PdfTableSimple table) throws DocumentException {

						table.addLigne("Etat", "Total", "Depuis " + RegDateHelper.dateToDisplayString(dateReference));
						table.setHeaderRows(1);

						final Map<EtatEvenementCivil, Integer> etats = civilsEch.getEtats();
						final Map<EtatEvenementCivil, Integer> etatsNouveaux = civilsEch.getEtatsNouveaux();
						for (EtatEvenementCivil etat : EtatEvenementCivil.values()) {
							final Integer nombre = etats.get(etat);
							final Integer nombreNouveaux = etatsNouveaux.get(etat);

							final String total = toStringInt(nombre, 0);
							final String totalNouveaux = toStringInt(nombreNouveaux, 0);
							table.addLigne(String.format("%s", etat.toString()), total, totalNouveaux);
						}
					}
				});
			}

			// événements civils : types en erreur
			{
				final String filename = "erreurs_evts_civils_ech_par_type.csv";
				final String contenu = asCsvFile(civilsEch.getErreursParType(), civilsEch.getErreursParTypeNouveaux(), dateReference, filename, status);
				final String titre = "Erreurs des événements civils e-CH par type d'événement";
				final String listVide = "(aucune)";
				addListeDetaillee(writer, civilsEch.getErreursParType().size(), titre, listVide, filename, contenu);
			}

			// messages d'erreur regroupés par type
			{
				final String filename = "messages_erreurs_evts_civils_ech_par_type.csv";
				final String contenu = buildStatsMessagesErreursEchParType(civilsEch.getToutesErreurs(), filename, status);
				final String titre = "Messages d'erreurs des événements civils e-CH par type d'événement";
				final String listVide = "(aucun)";
				addListeDetaillee(writer, civilsEch.getErreursParType().size(), titre, listVide, filename, contenu);
			}

			// toutes les erreurs
			{
				final String filename = "erreurs_evts_civils_ech.csv";
				final String contenu = asCsvFile(civilsEch.getToutesErreurs(), filename, status);
				final String titre = "Erreurs des événements civils e-CH";
				final String listVide = "(aucune)";
				addListeDetaillee(writer, civilsEch.getToutesErreurs().size(), titre, listVide, filename, contenu);
			}

			// manipulations manuelles
			{
				final String filename = "manipulations_evts_civils_ech.csv";
				final String contenu = asCsvFile(civilsEch.getManipulationsManuelles(), filename, status);
				final String titre = String.format("Manipulations manuelles des événements civils e-CH depuis le %s", RegDateHelper.dateToDisplayString(dateReference));
				final String listVide = "(aucune)";
				addListeDetaillee(writer, civilsEch.getManipulationsManuelles().size(), titre, listVide, filename, contenu);
			}
		}

		// événements externes
		if (externes != null) {

			// événements externes : états
			addEntete1("Evénements externes");
			{
				addTableSimple(2, new PdfRapport.TableSimpleCallback() {
					@Override
					public void fillTable(PdfTableSimple table) throws DocumentException {

						table.addLigne("Etat", "Total");
						table.setHeaderRows(1);

						final Map<EtatEvenementExterne, Integer> etats = externes.getEtats();
						for (EtatEvenementExterne etat : EtatEvenementExterne.values()) {
							final Integer nombre = etats.get(etat);
							table.addLigne(etat.name(), toStringInt(nombre, 0));
						}
					}
				});
			}

			// événements externes : erreurs
			{
				final String filename = "erreurs_evts_externes.csv";
				final String contenu = asCsvFile(externes.getErreurs(), filename, status);
				final String titre = "Erreurs des événements externes";
				final String listVide = "(aucune)";
				addListeDetaillee(writer, externes.getErreurs().size(), titre, listVide, filename, contenu);
			}
		}

		// événements d'identification de contribuable
		if (identCtb != null) {

			addEntete1("Demandes d'identification de contribuable");

			// événements d'identification de contribuable : états
			{
				addTableSimple(new float[] {60f, 20f, 20f}, new PdfRapport.TableSimpleCallback() {
					@Override
					public void fillTable(PdfTableSimple table) throws DocumentException {

						table.addLigne("Etat", "Total", "Depuis " + RegDateHelper.dateToDisplayString(dateReference));
						table.setHeaderRows(1);

						final Map<IdentificationContribuable.Etat, Integer> etats = identCtb.getEtats();
						final Map<IdentificationContribuable.Etat, Integer> etatsNouveaux = identCtb.getEtatsNouveaux();
						for (IdentificationContribuable.Etat etat : IdentificationContribuable.Etat.values()) {
							final Integer nombre = etats.get(etat);
							final Integer nombreNouveaux = etatsNouveaux.get(etat);

							final String total = toStringInt(nombre, 0);
							final String totalNouveaux = toStringInt(nombreNouveaux, 0);
							table.addLigne(String.format("%s", etat.toString()), total, totalNouveaux);
						}
					}
				});
			}

			// événements d'identification de contribuable : restant à traiter
			{
				final String filename = "identification_ctb_a_traiter.csv";
				final String contenu = asCsvFile(identCtb.getATraiter(), filename, status);
				final String titre = "Evénements d'identification à traiter";
				final String listVide = "(aucun)";
				addListeDetaillee(writer, identCtb.getATraiter().size(), titre, listVide, filename, contenu);
			}
		}

		close();

		status.setMessage("Génération du rapport terminée.");
	}

	private static String asCsvLine(String[] elements, boolean withNewLine) {
		final StringBuilder b = new StringBuilder();
		for (int i = 0 ; i < elements.length ; ++ i) {
			if (i > 0) {
				b.append(COMMA);
			}
			final String string = escapeChars(elements[i]);
			final boolean needsQuote = string.contains("\n");
			if (needsQuote) {
				b.append('\"');
			}
			b.append(string);
			if (needsQuote) {
				b.append('\"');
			}
		}
		if (withNewLine) {
			b.append('\n');
		}
		return b.toString();
	}

	private static <T extends StatistiqueEvenementInfo> String asCsvFile(List<T> elements, String fileName, StatusManager statusManager) {
		String contenu = null;
		if (elements != null && !elements.isEmpty()) {
			final T first = elements.get(0);
			contenu = CsvHelper.asCsvFile(elements, fileName, statusManager, new CsvHelper.FileFiller<T>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append(asCsvLine(first.getNomsColonnes(), false));
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, T elt) {
					final String[] valeurs = elt.getValeursColonnes();
					b.append(asCsvLine(valeurs, false));
					return true;
				}
			});
		}
		return contenu;
	}

	private static <T extends Enum<T>> String asCsvFile(Map<T, Integer> tous, @Nullable Map<T, Integer> nouveaux, RegDate dateReference, Class<T> enumClass, String fileName, StatusManager statusManager) {
		String contenu = null;
		if (tous != null && !tous.isEmpty()) {
			final String message = String.format("Génération du fichier %s", fileName);
			statusManager.setMessage(message, 0);
			final StringBuilder b = new StringBuilder();

			// les noms des colonnes
			b.append("VALEUR").append(COMMA).append("TOTAL");
			if (nouveaux != null) {
				b.append(COMMA).append("NOUVEAUX_DEPUIS_").append(RegDateHelper.dateToDisplayString(dateReference));
			}
			b.append('\n');

			// les valeurs
			for (T mod : enumClass.getEnumConstants()) {
				final Integer total = tous.get(mod);
				final Integer marginal = nouveaux != null ? nouveaux.get(mod) : null;
				if ((total != null && total > 0) || (marginal != null && marginal > 0)) {
					b.append(mod).append(COMMA).append(toStringInt(total, 0));
					if (nouveaux != null) {
						b.append(COMMA).append(toStringInt(marginal, 0));
					}
					b.append('\n');
				}
			}
			contenu = b.toString();

			statusManager.setMessage(message, 100);
		}
		return contenu;
	}

	private static String asCsvFile(Map<Pair<TypeEvenementCivilEch, ActionEvenementCivilEch>, Integer> tous, @Nullable Map<Pair<TypeEvenementCivilEch, ActionEvenementCivilEch>, Integer> nouveaux, RegDate dateReference, String fileName, StatusManager statusManager) {
		String contenu = null;
		if (tous != null && !tous.isEmpty()) {
			final String message = String.format("Génération du fichier %s", fileName);
			statusManager.setMessage(message, 0);
			final StringBuilder b = new StringBuilder();

			// les noms des colonnes
			b.append("TYPE").append(COMMA).append("ACTION").append(COMMA).append("TOTAL");
			if (nouveaux != null) {
				b.append(COMMA).append("NOUVEAUX_DEPUIS_").append(RegDateHelper.dateToDisplayString(dateReference));
			}
			b.append('\n');

			// les valeurs
			for (TypeEvenementCivilEch type : TypeEvenementCivilEch.values()) {
				for (ActionEvenementCivilEch action : ActionEvenementCivilEch.values()) {
					final Pair<TypeEvenementCivilEch, ActionEvenementCivilEch> key = new Pair<TypeEvenementCivilEch, ActionEvenementCivilEch>(type, action);
					final Integer total = tous.get(key);
					final Integer marginal = nouveaux != null ? nouveaux.get(key) : null;
					if ((total != null && total > 0) || (marginal != null && marginal > 0)) {
						b.append(type).append(COMMA).append(action).append(COMMA).append(toStringInt(total, 0));
						if (nouveaux != null) {
							b.append(COMMA).append(toStringInt(marginal, 0));
						}
						b.append('\n');
					}
				}
			}
			contenu = b.toString();

			statusManager.setMessage(message, 100);
		}
		return contenu;
	}

	private static final class EvtCivilRegPPMsgTypeKey {

		public final String msg;
		public final TypeEvenementCivil type;

		public EvtCivilRegPPMsgTypeKey(String msg, TypeEvenementCivil type) {
			this.msg = msg.replaceAll("[0-9]+", "?");       // pour enlever toutes les différences sur des dates ou des numéros d'individu, de tiers...
			this.type = type;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final EvtCivilRegPPMsgTypeKey that = (EvtCivilRegPPMsgTypeKey) o;

			if (type != that.type) return false;
			if (msg != null ? !msg.equals(that.msg) : that.msg != null) return false;

			return true;
		}

		@Override
		public int hashCode() {
			int result = msg != null ? msg.hashCode() : 0;
			result = 31 * result + (type != null ? type.hashCode() : 0);
			return result;
		}
	}

	private String buildStatsMessagesErreursRegPPParType(List<StatsEvenementsCivilsRegPPResults.EvenementCivilEnErreurInfo> toutesErreurs, String fileName, StatusManager statusManager) {
		String contenu = null;
		if (toutesErreurs != null && !toutesErreurs.isEmpty()) {

			final String messageCalcul = String.format("Calcul des statistiques pour le fichier %s", fileName);
			statusManager.setMessage(messageCalcul, 0);

			// première partie : calcul des statistiques
			final Map<EvtCivilRegPPMsgTypeKey, MutableInt> map = new HashMap<EvtCivilRegPPMsgTypeKey, MutableInt>();
			final GentilIterator<StatsEvenementsCivilsRegPPResults.EvenementCivilEnErreurInfo> iter = new GentilIterator<StatsEvenementsCivilsRegPPResults.EvenementCivilEnErreurInfo>(toutesErreurs);
			while (iter.hasNext()) {
				if (iter.isAtNewPercent()) {
					statusManager.setMessage(messageCalcul, iter.getPercent());
				}

				final StatsEvenementsCivilsRegPPResults.EvenementCivilEnErreurInfo erreur = iter.next();
				if (erreur != null && erreur.etat == EtatEvenementCivil.EN_ERREUR) {
					final EvtCivilRegPPMsgTypeKey key = new EvtCivilRegPPMsgTypeKey(erreur.message, erreur.type);
					final MutableInt nb = map.get(key);
					if (nb == null) {
						map.put(key, new MutableInt(1));
					}
					else {
						nb.increment();
					}
				}
			}

			// tri des lignes dans l'ordre décroissant des nombres d'occurrence
			final List<Map.Entry<EvtCivilRegPPMsgTypeKey, MutableInt>> stats = new ArrayList<Map.Entry<EvtCivilRegPPMsgTypeKey, MutableInt>>(map.entrySet());
			Collections.sort(stats, new Comparator<Map.Entry<EvtCivilRegPPMsgTypeKey, MutableInt>>() {
				@Override
				public int compare(Map.Entry<EvtCivilRegPPMsgTypeKey, MutableInt> o1, Map.Entry<EvtCivilRegPPMsgTypeKey, MutableInt> o2) {
					return o2.getValue().intValue() - o1.getValue().intValue();
				}
			});

			// touche finale : remplissage de la chaîne de caractères qui finira dans le fichier CSV
			contenu = CsvHelper.asCsvFile(stats, fileName, statusManager, new CsvHelper.FileFiller<Map.Entry<EvtCivilRegPPMsgTypeKey, MutableInt>>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("MESSAGE").append(COMMA);
					b.append("TYPE_EVT").append(COMMA);
					b.append("COUNT");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, Map.Entry<EvtCivilRegPPMsgTypeKey, MutableInt> stat) {
					final EvtCivilRegPPMsgTypeKey key = stat.getKey();
					b.append(asCsvField(key.msg)).append(COMMA);
					b.append(key.type).append(COMMA);
					b.append(stat.getValue().intValue());
					return true;
				}
			});
		}
		return contenu;
	}
	
	private static class EvtCivilEchMsgTypeKey {
		public final String msg;
		public final TypeEvenementCivilEch type;
		public final ActionEvenementCivilEch action;

		private EvtCivilEchMsgTypeKey(String msg, TypeEvenementCivilEch type, ActionEvenementCivilEch action) {
			this.msg = msg.replaceAll("[0-9]+", "?");        // pour enlever toutes les différences sur des dates ou des numéros d'individu, de tiers...
			this.type = type;
			this.action = action;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final EvtCivilEchMsgTypeKey that = (EvtCivilEchMsgTypeKey) o;

			if (action != that.action) return false;
			if (msg != null ? !msg.equals(that.msg) : that.msg != null) return false;
			if (type != that.type) return false;

			return true;
		}

		@Override
		public int hashCode() {
			int result = msg != null ? msg.hashCode() : 0;
			result = 31 * result + (type != null ? type.hashCode() : 0);
			result = 31 * result + (action != null ? action.hashCode() : 0);
			return result;
		}
	}

	private String buildStatsMessagesErreursEchParType(List<StatsEvenementsCivilsEchResults.EvenementCivilEnErreurInfo> toutesErreurs, String fileName, StatusManager statusManager) {
		String contenu = null;
		if (toutesErreurs != null && !toutesErreurs.isEmpty()) {

			final String messageCalcul = String.format("Calcul des statistiques pour le fichier %s", fileName);
			statusManager.setMessage(messageCalcul, 0);

			// première partie : calcul des statistiques
			final Map<EvtCivilEchMsgTypeKey, MutableInt> map = new HashMap<EvtCivilEchMsgTypeKey, MutableInt>();
			final GentilIterator<StatsEvenementsCivilsEchResults.EvenementCivilEnErreurInfo> iter = new GentilIterator<StatsEvenementsCivilsEchResults.EvenementCivilEnErreurInfo>(toutesErreurs);
			while (iter.hasNext()) {
				if (iter.isAtNewPercent()) {
					statusManager.setMessage(messageCalcul, iter.getPercent());
				}

				final StatsEvenementsCivilsEchResults.EvenementCivilEnErreurInfo erreur = iter.next();
				if (erreur != null && erreur.etat == EtatEvenementCivil.EN_ERREUR) {
					final EvtCivilEchMsgTypeKey key = new EvtCivilEchMsgTypeKey(erreur.message, erreur.type, erreur.action);
					final MutableInt nb = map.get(key);
					if (nb == null) {
						map.put(key, new MutableInt(1));
					}
					else {
						nb.increment();
					}
				}
			}

			// tri des lignes dans l'ordre décroissant des nombres d'occurrence
			final List<Map.Entry<EvtCivilEchMsgTypeKey, MutableInt>> stats = new ArrayList<Map.Entry<EvtCivilEchMsgTypeKey, MutableInt>>(map.entrySet());
			Collections.sort(stats, new Comparator<Map.Entry<EvtCivilEchMsgTypeKey, MutableInt>>() {
				@Override
				public int compare(Map.Entry<EvtCivilEchMsgTypeKey, MutableInt> o1, Map.Entry<EvtCivilEchMsgTypeKey, MutableInt> o2) {
					return o2.getValue().intValue() - o1.getValue().intValue();
				}
			});

			// touche finale : remplissage de la chaîne de caractères qui finira dans le fichier CSV
			contenu = CsvHelper.asCsvFile(stats, fileName, statusManager, new CsvHelper.FileFiller<Map.Entry<EvtCivilEchMsgTypeKey, MutableInt>>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("MESSAGE").append(COMMA);
					b.append("TYPE_EVT").append(COMMA);
					b.append("ACTION").append(COMMA);
					b.append("COUNT");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, Map.Entry<EvtCivilEchMsgTypeKey, MutableInt> stat) {
					final EvtCivilEchMsgTypeKey key = stat.getKey();
					b.append(asCsvField(key.msg)).append(COMMA);
					b.append(key.type).append(COMMA);
					b.append(key.action).append(COMMA);
					b.append(stat.getValue().intValue());
					return true;
				}
			});
		}
		return contenu;
	}
}
