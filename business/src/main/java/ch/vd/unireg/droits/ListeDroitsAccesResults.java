package ch.vd.unireg.droits;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseEnvoiDetaillee;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.JobResults;
import ch.vd.unireg.interfaces.service.host.Operateur;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.Niveau;
import ch.vd.unireg.type.TypeDroitAcces;

public class ListeDroitsAccesResults extends JobResults<Number, ListeDroitsAccesResults> {

//	private static final Logger LOGGER = LoggerFactory.getLogger(ListeDroitsAccesResults.class);

	public enum ErreurType {
		EXCEPTION(EXCEPTION_DESCRIPTION);

		private final String description;

		ErreurType(String description) {
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
		private final List<String> nomsRaisonsSociales = new ArrayList<>();
		private String[] adresseEnvoi;
		private final TypeDroitAcces type;
		private final Niveau niveau;
		private final Operateur operateur;

		public InfoDroitAcces(long noCtb, Integer oidGestion, AdresseEnvoiDetaillee adresseEnvoi, TypeDroitAcces type, Niveau niveau, Operateur operateur) {
			this.noCtb = noCtb;
			this.oidGestion = oidGestion;
			if (adresseEnvoi != null) {
				this.nomsRaisonsSociales.addAll(adresseEnvoi.getNomsPrenomsOuRaisonsSociales());
				this.adresseEnvoi = adresseEnvoi.getLignes();
			}
			this.type = type;
			this.niveau = niveau;
			this.operateur = operateur;
		}

		public long getNoCtb() {
			return noCtb;
		}

		public List<String> getNomsRaisonsSociales() {
			return nomsRaisonsSociales;
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
