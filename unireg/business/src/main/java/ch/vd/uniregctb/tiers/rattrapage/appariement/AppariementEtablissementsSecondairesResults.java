package ch.vd.uniregctb.tiers.rattrapage.appariement;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.utils.ExceptionUtils;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.uniregctb.common.AbstractJobResults;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class AppariementEtablissementsSecondairesResults extends AbstractJobResults<Long, AppariementEtablissementsSecondairesResults> {

	public final int nbThreads;
	public final boolean simulation;
	public final List<Long> idsEntreprises;

	private boolean interrompu = false;
	private final List<AppariementEtablissement> appariements = new LinkedList<>();
	private final List<AppariementErreur> erreurs = new LinkedList<>();

	public static class AppariementInfo {
		public final long idEntreprise;
		public AppariementInfo(long idEntreprise) {
			this.idEntreprise = idEntreprise;
		}
	}

	public enum RaisonAppariement {
		SEULS_MEME_ENDROIT("Seul couple établissement/site à cet endroit compte tenu du flag actif/inactif."),
		IDE_MEME_ENDROIT("Etablissement et site partagent le même siège et le même numéro IDE.");

		private final String libelle;

		RaisonAppariement(String libelle) {
			this.libelle = libelle;
		}

		public String getLibelle() {
			return libelle;
		}
	}

	public static class AppariementEtablissement extends AppariementInfo implements Comparable<AppariementEtablissement> {

		public final long idEtablissement;
		public final long idSite;
		public final TypeAutoriteFiscale tafSiege;
		public final Integer ofsSiege;
		public final RaisonAppariement raison;

		public AppariementEtablissement(long idEntreprise, long idEtablissement, long idSite, TypeAutoriteFiscale tafSiege, Integer ofsSiege, RaisonAppariement raison) {
			super(idEntreprise);
			this.idEtablissement = idEtablissement;
			this.idSite = idSite;
			this.tafSiege = tafSiege;
			this.ofsSiege = ofsSiege;
			this.raison = raison;
		}

		@Override
		public int compareTo(@NotNull AppariementEtablissement o) {
			int comparison = Long.compare(idEntreprise, o.idEntreprise);
			if (comparison == 0) {
				comparison = Long.compare(idEtablissement, o.idEtablissement);
			}
			return comparison;
		}
	}

	public static class AppariementErreur extends AppariementInfo implements Comparable<AppariementErreur> {

		public final String stack;

		public AppariementErreur(long idEntreprise, Exception e) {
			super(idEntreprise);
			this.stack = ExceptionUtils.extractCallStack(e);
		}

		@Override
		public int compareTo(@NotNull AppariementErreur o) {
			return Long.compare(idEntreprise, o.idEntreprise);
		}
	}

	public AppariementEtablissementsSecondairesResults(int nbThreads, boolean simulation, List<Long> idsEntreprises) {
		this.nbThreads = nbThreads;
		this.simulation = simulation;
		this.idsEntreprises = CollectionsUtils.unmodifiableNeverNull(idsEntreprises);
	}

	@Override
	public void addErrorException(Long element, Exception e) {
		erreurs.add(new AppariementErreur(element, e));
	}

	@Override
	public void addAll(AppariementEtablissementsSecondairesResults right) {
		erreurs.addAll(right.erreurs);
		appariements.addAll(right.appariements);
	}

	@Override
	public void end() {
		Collections.sort(erreurs);
		Collections.sort(appariements);
		super.end();
	}

	public void addNouvelAppariementEtablissementCommune(Entreprise entreprise, Etablissement etablissement, SiteOrganisation site, TypeAutoriteFiscale tafSiege, Integer ofsSiege) {
		appariements.add(new AppariementEtablissement(entreprise.getNumero(), etablissement.getNumero(), site.getNumeroSite(), tafSiege, ofsSiege, RaisonAppariement.SEULS_MEME_ENDROIT));
	}

	public void addNouvelAppariementEtablissementIde(Entreprise entreprise, Etablissement etablissement, SiteOrganisation site, TypeAutoriteFiscale tafSiege, Integer ofsSiege) {
		appariements.add(new AppariementEtablissement(entreprise.getNumero(), etablissement.getNumero(), site.getNumeroSite(), tafSiege, ofsSiege, RaisonAppariement.IDE_MEME_ENDROIT));
	}

	public void setInterrupted(boolean interrompu) {
		this.interrompu = interrompu;
	}

	public boolean wasInterrupted() {
		return interrompu;
	}

	public List<AppariementEtablissement> getAppariements() {
		return appariements;
	}

	public List<AppariementErreur> getErreurs() {
		return erreurs;
	}
}
