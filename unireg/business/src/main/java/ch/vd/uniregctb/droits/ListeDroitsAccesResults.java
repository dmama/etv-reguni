package ch.vd.uniregctb.droits;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.securite.model.Operateur;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.JobResults;
import ch.vd.uniregctb.common.NomPrenom;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.Niveau;
import ch.vd.uniregctb.type.TypeDroitAcces;

public class ListeDroitsAccesResults extends JobResults<Number, ListeDroitsAccesResults> {

//	private static final Logger LOGGER = Logger.getLogger(ListeDroitsAccesResults.class);

	public static enum ErreurType {
		EXCEPTION(EXCEPTION_DESCRIPTION);

		private final String description;

		private ErreurType(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	public static class Erreur extends Info {
		public final ErreurType raison;

		public Erreur(Long noCtb, Integer officeImpotID, ErreurType raison, String details, String nomCtb) {
			super((noCtb == null ? 0 : noCtb), officeImpotID, details, nomCtb);
			this.raison = raison;
		}

		@Override
		public String getDescriptionRaison() {
			return raison.description;
		}
	}

	public static class InfoDroitAcces {

		private final long noCtb;
		private final Integer oidGestion;
		private final List<NomPrenom> nomsPrenoms = new ArrayList<>();
		private String[] adresseEnvoi;
		private final TypeDroitAcces type;
		private final Niveau niveau;
		private final Operateur operateur;

		public InfoDroitAcces(long noCtb, Integer oidGestion, AdresseEnvoiDetaillee adresseEnvoi, TypeDroitAcces type, Niveau niveau, Operateur operateur) {
			this.noCtb = noCtb;
			this.oidGestion = oidGestion;
			if (adresseEnvoi != null) {
				this.nomsPrenoms.addAll(adresseEnvoi.getNomsPrenoms());
				this.adresseEnvoi = adresseEnvoi.getLignes();
			}
			this.type = type;
			this.niveau = niveau;
			this.operateur = operateur;
		}

		public long getNoCtb() {
			return noCtb;
		}

		public List<NomPrenom> getNomsPrenoms() {
			return nomsPrenoms;
		}

		public String[] getAdresseEnvoi() {
			return adresseEnvoi;
		}

		public Integer getOidGestion() {
			return oidGestion;
		}

		public TypeDroitAcces getType() {
			return type;
		}

		public Niveau getNiveau() {
			return niveau;
		}

		public Operateur getOperateur() {
			return operateur;
		}
	}

	// Paramètres d'entrée
	public final RegDate dateValeur;

	// Données de processing
	public boolean interrompu;
	public final List<InfoDroitAcces> droitsAcces = new ArrayList<>();
	public final List<Erreur> erreurs = new ArrayList<>();

	public ListeDroitsAccesResults(RegDate dateValeur, TiersService tiersService, AdresseService adresseService) {
		super(tiersService, adresseService);
		this.dateValeur = dateValeur;
	}

	public RegDate getDateValeur() {
		return dateValeur;
	}

	public void setInterrompu(boolean interrompu) {
		this.interrompu = interrompu;
	}

	public boolean isInterrompu() {
		return interrompu;
	}

	public void addDroitAcces(long noCtb, Integer oidGestion, AdresseEnvoiDetaillee adresseEnvoi, TypeDroitAcces type, Niveau niveau, Operateur operateur) {
		this.droitsAcces.add(new InfoDroitAcces(noCtb, oidGestion, adresseEnvoi, type, niveau, operateur));
	}

	@Override
	public void addErrorException(Number idCtb, Exception e) {
		erreurs.add(new Erreur(idCtb.longValue(), null, ErreurType.EXCEPTION, e.getMessage(), getNom(idCtb.longValue())));
	}

	@Override
	public void addAll(ListeDroitsAccesResults right) {
		this.droitsAcces.addAll(right.droitsAcces);
		this.erreurs.addAll(right.erreurs);
	}
}
