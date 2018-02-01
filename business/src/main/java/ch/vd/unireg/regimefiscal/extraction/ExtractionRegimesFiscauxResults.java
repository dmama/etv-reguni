package ch.vd.unireg.regimefiscal.extraction;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.ExceptionUtils;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.common.AbstractJobResults;
import ch.vd.unireg.tiers.RegimeFiscal;

public class ExtractionRegimesFiscauxResults extends AbstractJobResults<Long, ExtractionRegimesFiscauxResults> {

	public final boolean avecHistorique;
	public final int nbThreads;
	public final RegDate dateTraitement;

	public static class Erreur implements Comparable<Erreur> {
		public final long idEntreprise;
		public final String message;

		public Erreur(long idEntreprise, String message) {
			this.idEntreprise = idEntreprise;
			this.message = message;
		}

		@Override
		public int compareTo(@NotNull ExtractionRegimesFiscauxResults.Erreur o) {
			return Long.compare(idEntreprise, o.idEntreprise);
		}
	}

	public static class SansRegimeFiscal implements Comparable<SansRegimeFiscal> {
		public final long idEntreprise;
		public final RegimeFiscal.Portee portee;
		public final String ide;
		public final String raisonSociale;
		public final FormeLegale formeLegale;

		public SansRegimeFiscal(long idEntreprise, RegimeFiscal.Portee portee, String ide, String raisonSociale, FormeLegale formleLegale) {
			this.idEntreprise = idEntreprise;
			this.portee = portee;
			this.ide = ide;
			this.raisonSociale = raisonSociale;
			formeLegale = formleLegale;
		}

		@Override
		public int compareTo(@NotNull ExtractionRegimesFiscauxResults.SansRegimeFiscal o) {
			int comparison = Long.compare(idEntreprise, o.idEntreprise);
			if (comparison == 0) {
				comparison = portee.compareTo(o.portee);
			}
			return comparison;
		}
	}

	public static class PlageRegimeFiscal implements Comparable<PlageRegimeFiscal>, DateRange {
		public final long idEntreprise;
		public final RegimeFiscal.Portee portee;
		public final String code;
		public final String libelle;
		public final RegDate dateDebut;
		public final RegDate dateFin;
		public final String ide;
		public final String raisonSociale;
		public final FormeLegale formeLegale;

		public PlageRegimeFiscal(long idEntreprise, RegimeFiscal.Portee portee, String code, String libelle, RegDate dateDebut, RegDate dateFin, String ide, String raisonSociale, FormeLegale formleLegale) {
			this.idEntreprise = idEntreprise;
			this.portee = portee;
			this.code = code;
			this.libelle = libelle;
			this.dateDebut = dateDebut;
			this.dateFin = dateFin;
			this.ide = ide;
			this.raisonSociale = raisonSociale;
			formeLegale = formleLegale;
		}

		@Override
		public RegDate getDateDebut() {
			return dateDebut;
		}

		@Override
		public RegDate getDateFin() {
			return dateFin;
		}

		@Override
		public int compareTo(@NotNull ExtractionRegimesFiscauxResults.PlageRegimeFiscal o) {
			int comparison = Long.compare(idEntreprise, o.idEntreprise);
			if (comparison == 0) {
				comparison = portee.compareTo(o.portee);
			}
			if (comparison == 0) {
				comparison = DateRangeComparator.compareRanges(this, o);
			}
			return comparison;
		}
	}

	private final List<Erreur> erreurs = new LinkedList<>();
	private final List<SansRegimeFiscal> sansRegimeFiscal = new LinkedList<>();
	private final List<PlageRegimeFiscal> plagesRegimeFiscal = new LinkedList<>();

	public ExtractionRegimesFiscauxResults(boolean avecHistorique, int nbThreads, RegDate dateTraitement) {
		this.avecHistorique = avecHistorique;
		this.nbThreads = nbThreads;
		this.dateTraitement = dateTraitement;
	}

	@Override
	public void addErrorException(Long idEntreprise, Exception e) {
		erreurs.add(new Erreur(idEntreprise, ExceptionUtils.extractCallStack(e)));
	}

	public void addIdentifiantInvalide(Long id) {
		erreurs.add(new Erreur(id, "L'identifiant ne correspond à aucune entreprise"));
	}

	public void addEntrepriseSansRegimeFiscal(Long id, RegimeFiscal.Portee portee, String ide, String raisonSociale, FormeLegale formleLegale) {
		sansRegimeFiscal.add(new SansRegimeFiscal(id, portee, ide, raisonSociale, formleLegale));
	}

	public void addRegimeFiscal(Long id, RegimeFiscal regime, TypeRegimeFiscal type, String ide, String raisonSociale, FormeLegale formleLegale) {
		plagesRegimeFiscal.add(new PlageRegimeFiscal(id, regime.getPortee(), regime.getCode(), type.getLibelle(), regime.getDateDebut(), regime.getDateFin(), ide, raisonSociale, formleLegale));
	}

	public void addRegimeFiscalInconnu(Long id, RegimeFiscal regime, String ide, String raisonSociale, FormeLegale formleLegale) {
		plagesRegimeFiscal.add(new PlageRegimeFiscal(id, regime.getPortee(), regime.getCode(), "????", regime.getDateDebut(), regime.getDateFin(), ide, raisonSociale, formleLegale));
	}

	@Override
	public void addAll(ExtractionRegimesFiscauxResults other) {
		erreurs.addAll(other.erreurs);
		sansRegimeFiscal.addAll(other.sansRegimeFiscal);
		plagesRegimeFiscal.addAll(other.plagesRegimeFiscal);
	}

	@Override
	public void end() {
		erreurs.sort(Comparator.naturalOrder());
		sansRegimeFiscal.sort(Comparator.naturalOrder());
		plagesRegimeFiscal.sort(Comparator.naturalOrder());
		super.end();
	}

	public List<Erreur> getErreurs() {
		return erreurs;
	}

	public List<SansRegimeFiscal> getSansRegimeFiscal() {
		return sansRegimeFiscal;
	}

	public List<PlageRegimeFiscal> getPlagesRegimeFiscal() {
		return plagesRegimeFiscal;
	}
}
