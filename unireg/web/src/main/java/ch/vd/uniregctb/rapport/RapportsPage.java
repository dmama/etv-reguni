package ch.vd.uniregctb.rapport;

import java.util.List;
import java.util.Map;

import org.springframework.context.MessageSource;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.tiers.ActiviteEconomique;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Heritage;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RepresentationConventionnelle;
import ch.vd.uniregctb.tiers.RepresentationLegale;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.TiersWebHelper;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;
import ch.vd.uniregctb.utils.WebContextUtils;

@SuppressWarnings("UnusedDeclaration")
public class RapportsPage {

	public static class RapportView {

		private Long id;
		private String dateDebut;
		private String dateFin;
		private boolean annule;
		private String type;
		private Long numeroAutreTiers;
		private List<String> nomCourrier;
		private String toolTipMessage;

		// -- uniquement pour RepresentationConventionnelle --
		private Boolean extensionExecutionForcee;

		// -- uniquement pour la Representation légale
		private Long autoriteTutelaireId;
		private String nomAutoriteTutelaire;

		// -- uniquement pour les héritiers
		private Boolean principalCommunaute;

		public RapportView(RapportEntreTiers rapport, SensRapportEntreTiers sens, TiersService tiersService, AdresseService adresseService, MessageSource messageSource) {
			this.id = rapport.getId();
			this.dateDebut = RegDateHelper.dateToDisplayString(rapport.getDateDebut());
			this.dateFin = RegDateHelper.dateToDisplayString(rapport.getDateFin());
			this.annule = rapport.isAnnule();
			this.type = initType(rapport, sens, messageSource);
			this.numeroAutreTiers = (sens == SensRapportEntreTiers.SUJET ? rapport.getObjetId() : rapport.getSujetId());
			this.nomCourrier = ch.vd.uniregctb.rapport.view.RapportView.buildNomCourrier(getTiers(this.numeroAutreTiers, tiersService), adresseService);

			if (rapport instanceof RepresentationConventionnelle) {
				final RepresentationConventionnelle rc = (RepresentationConventionnelle) rapport;
				this.extensionExecutionForcee = rc.getExtensionExecutionForcee();
			}

			if (rapport instanceof RepresentationLegale) {
				final RepresentationLegale rl = (RepresentationLegale) rapport;
				this.autoriteTutelaireId = rl.getAutoriteTutelaireId();
				this.nomAutoriteTutelaire = initNomAutoriteTutelaire(this.autoriteTutelaireId, tiersService);
			}

			if (rapport instanceof Heritage) {
				final Heritage heritage = (Heritage) rapport;
				this.principalCommunaute = heritage.getPrincipalCommunaute();
			}

			this.toolTipMessage = TiersWebHelper.getRapportEntreTiersTooltips(rapport, adresseService, tiersService);
		}

		private static String initNomAutoriteTutelaire(Long autoriteTutelaireId, TiersService tiersService) {
			String nom = null;
			if (autoriteTutelaireId != null) {
				final Tiers autoriteTutelaire = tiersService.getTiers(autoriteTutelaireId);
				if (autoriteTutelaire instanceof CollectiviteAdministrative) {
					nom = tiersService.getNomCollectiviteAdministrative(((CollectiviteAdministrative) autoriteTutelaire).getNumeroCollectiviteAdministrative());
				}
			}
			return nom;
		}

		private static Tiers getTiers(Long numeroTiers, TiersService tiersService) {
			return numeroTiers != null ? tiersService.getTiers(numeroTiers) : null;
		}

		private static String initType(RapportEntreTiers rapport, SensRapportEntreTiers sens, MessageSource messageSource) {
			final String code;
			final TypeRapportEntreTiers type = rapport.getType();
			if (type == TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE && sens == SensRapportEntreTiers.SUJET && ((ActiviteEconomique) rapport).isPrincipal()) {
				code = "option.rapport.entre.tiers." + sens.name() + "." + type.name() + ".principal";
			}
			else {
				code = "option.rapport.entre.tiers." + sens.name() + "." + type.name();
			}
			return messageSource.getMessage(code, null, WebContextUtils.getDefaultLocale());
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getDateDebut() {
			return dateDebut;
		}

		public void setDateDebut(String dateDebut) {
			this.dateDebut = dateDebut;
		}

		public String getDateFin() {
			return dateFin;
		}

		public void setDateFin(String dateFin) {
			this.dateFin = dateFin;
		}

		public boolean isAnnule() {
			return annule;
		}

		public void setAnnule(boolean annule) {
			this.annule = annule;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public Long getNumeroAutreTiers() {
			return numeroAutreTiers;
		}

		public void setNumeroAutreTiers(Long numeroAutreTiers) {
			this.numeroAutreTiers = numeroAutreTiers;
		}

		public List<String> getNomCourrier() {
			return nomCourrier;
		}

		public void setNomCourrier(List<String> nomCourrier) {
			this.nomCourrier = nomCourrier;
		}

		public String getToolTipMessage() {
			return toolTipMessage;
		}

		public void setToolTipMessage(String toolTipMessage) {
			this.toolTipMessage = toolTipMessage;
		}

		public Boolean getExtensionExecutionForcee() {
			return extensionExecutionForcee;
		}

		public void setExtensionExecutionForcee(Boolean extensionExecutionForcee) {
			this.extensionExecutionForcee = extensionExecutionForcee;
		}

		public Long getAutoriteTutelaireId() {
			return autoriteTutelaireId;
		}

		public void setAutoriteTutelaireId(Long autoriteTutelaireId) {
			this.autoriteTutelaireId = autoriteTutelaireId;
		}

		public String getNomAutoriteTutelaire() {
			return nomAutoriteTutelaire;
		}

		public void setNomAutoriteTutelaire(String nomAutoriteTutelaire) {
			this.nomAutoriteTutelaire = nomAutoriteTutelaire;
		}

		public Boolean getPrincipalCommunaute() {
			return principalCommunaute;
		}
	}

	private Long tiersId;
	private List<RapportView> rapports;
	private boolean showHisto;
	private TypeRapportEntreTiers typeRapport;
	private Map<TypeRapportEntreTiers,String> typesRapportEntreTiers;
	private int page;
	private int totalCount;
	private String sortField;
	private String sortOrder;

	public RapportsPage(Long tiersId, List<RapportView> rapports, boolean showHisto, TypeRapportEntreTiers typeRapport, Map<TypeRapportEntreTiers, String> typesRapportEntreTiers, int page,
	                    int totalCount, String sortField, String sortOrder) {
		this.tiersId = tiersId;
		this.rapports = rapports;
		this.showHisto = showHisto;
		this.typeRapport = typeRapport;
		this.typesRapportEntreTiers = typesRapportEntreTiers;
		this.page = page;
		this.totalCount = totalCount;
		this.sortField = sortField;
		this.sortOrder = sortOrder;
	}

	public Long getTiersId() {
		return tiersId;
	}

	public void setTiersId(Long tiersId) {
		this.tiersId = tiersId;
	}

	public List<RapportView> getRapports() {
		return rapports;
	}

	public void setRapports(List<RapportView> rapports) {
		this.rapports = rapports;
	}

	public boolean isShowHisto() {
		return showHisto;
	}

	public void setShowHisto(boolean showHisto) {
		this.showHisto = showHisto;
	}

	public TypeRapportEntreTiers getTypeRapport() {
		return typeRapport;
	}

	public void setTypeRapport(TypeRapportEntreTiers typeRapport) {
		this.typeRapport = typeRapport;
	}

	public Map<TypeRapportEntreTiers, String> getTypesRapportEntreTiers() {
		return typesRapportEntreTiers;
	}

	public void setTypesRapportEntreTiers(Map<TypeRapportEntreTiers, String> typesRapportEntreTiers) {
		this.typesRapportEntreTiers = typesRapportEntreTiers;
	}

	public int getPage() {
		return page;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public int getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(int totalCount) {
		this.totalCount = totalCount;
	}

	public String getSortField() {
		return sortField;
	}

	public void setSortField(String sortField) {
		this.sortField = sortField;
	}

	public String getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(String sortOrder) {
		this.sortOrder = sortOrder;
	}
}
