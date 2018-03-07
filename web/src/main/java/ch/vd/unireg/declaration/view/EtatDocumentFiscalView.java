package ch.vd.unireg.declaration.view;

import java.util.Date;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.MessageSource;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.Annulable;
import ch.vd.unireg.documentfiscal.DocumentFiscal;
import ch.vd.unireg.documentfiscal.EtatDocumentFiscal;
import ch.vd.unireg.documentfiscal.EtatDocumentFiscalAvecDateEnvoiCourrier;
import ch.vd.unireg.documentfiscal.EtatDocumentFiscalAvecDateEnvoiCourrierEtEmolument;
import ch.vd.unireg.documentfiscal.EtatDocumentFiscalAvecDocumentArchive;
import ch.vd.unireg.documentfiscal.SourceQuittancement;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;
import ch.vd.unireg.utils.WebContextUtils;

@SuppressWarnings("unused")
public class EtatDocumentFiscalView implements Comparable<EtatDocumentFiscalView>, Annulable {

	private final Long id;
	private final RegDate dateObtention;
	private final Date logCreationDate;
	private final boolean annule;
	private final TypeEtatDocumentFiscal etat;
	private final String etatMessage;

	/**
	 * La source de quittancement dans le cas ou #etat == "RETOURNE".
	 */
	private String source;
	private String sourceMessage;

	/**
	 * La date d'envoi de la sommation dans le cas ou #etat == 'SOMME' ou #etat == 'RAPPELE'
	 */
	private RegDate dateEnvoiCourrier;
	private String dateEnvoiCourrierMessage;

	/**
	 * L'url de visualisation externe du document (s'il y en a un, bien-sûr)
	 */
	private String urlVisualisationExterneDocument;

	public EtatDocumentFiscalView(@NotNull EtatDocumentFiscal etat, @NotNull ServiceInfrastructureService infraService, @NotNull MessageSource messageSource) {
		this.id = etat.getId();
		this.dateObtention = etat.getDateObtention();
		this.logCreationDate = etat.getLogCreationDate();
		this.annule = etat.isAnnule();
		this.etat = etat.getEtat();
		this.etatMessage = messageSource.getMessage("option.etat.avancement.f." + this.etat.name(), null, WebContextUtils.getDefaultLocale());

		if (etat instanceof SourceQuittancement) {
			this.source = ((SourceQuittancement) etat).getSource();
			if (this.source == null) {
				this.sourceMessage = messageSource.getMessage("option.source.quittancement.UNKNOWN", null, WebContextUtils.getDefaultLocale());
			}
			else {
				this.sourceMessage = messageSource.getMessage("option.source.quittancement." + this.source, null, WebContextUtils.getDefaultLocale());
			}
		}
		if (etat instanceof EtatDocumentFiscalAvecDateEnvoiCourrier) {
			this.dateEnvoiCourrier = ((EtatDocumentFiscalAvecDateEnvoiCourrier) etat).getDateEnvoiCourrier();
			final Integer emolument = getEmolument(etat);
			if (emolument != null) {
				this.dateEnvoiCourrierMessage = messageSource.getMessage("label.date.envoi.courrier.avec.emolument",
				                                                         new Object[]{RegDateHelper.dateToDisplayString(this.dateEnvoiCourrier), Integer.toString(emolument)},
				                                                         WebContextUtils.getDefaultLocale());
			}
			else {
				this.dateEnvoiCourrierMessage = messageSource.getMessage("label.date.envoi.courrier",
				                                                         new Object[]{RegDateHelper.dateToDisplayString(this.dateEnvoiCourrier)},
				                                                         WebContextUtils.getDefaultLocale());
			}
		}
		if (etat instanceof EtatDocumentFiscalAvecDocumentArchive) {
			final DocumentFiscal documentFiscal = etat.getDocumentFiscal();
			final Integer anneePeriode = documentFiscal.getAnneePeriodeFiscale();
			if (anneePeriode != null) {
				this.urlVisualisationExterneDocument = Optional.of((EtatDocumentFiscalAvecDocumentArchive) etat)
						.map(EtatDocumentFiscalAvecDocumentArchive::getCleDocument)
						.filter(StringUtils::isNotBlank)
						.map(cle -> infraService.getUrlVisualisationDocument(documentFiscal.getTiers().getNumero(), anneePeriode, cle))
						.orElse(null);
			}
		}
	}

	@Nullable
	private static Integer getEmolument(@Nullable EtatDocumentFiscal etat) {
		final Integer emolument;
		if (etat instanceof EtatDocumentFiscalAvecDateEnvoiCourrierEtEmolument) {
			emolument = ((EtatDocumentFiscalAvecDateEnvoiCourrierEtEmolument) etat).getEmolument();
		}
		else {
			emolument = null;
		}
		return emolument;
	}

	public Long getId() {
		return id;
	}

	public RegDate getDateObtention() {
		return dateObtention;
	}

	@Override
	public boolean isAnnule() {
		return annule;
	}

	public TypeEtatDocumentFiscal getEtat() {
		return etat;
	}

	public String getEtatMessage() {
		return etatMessage;
	}

	public String getSource() {
		return source;
	}

	public String getSourceMessage() {
		return sourceMessage;
	}

	public RegDate getDateEnvoiCourrier() {
		return dateEnvoiCourrier;
	}

	public String getDateEnvoiCourrierMessage() {
		return dateEnvoiCourrierMessage;
	}

	public String getUrlVisualisationExterneDocument() {
		return urlVisualisationExterneDocument;
	}

	@Override
	public int compareTo(@NotNull EtatDocumentFiscalView o) {
		if (this.dateObtention == o.dateObtention) {
			return -1 * logCreationDate.compareTo(o.logCreationDate);
		}
		else {
			// du plus récent au plus ancient
			return -1 * dateObtention.compareTo(o.dateObtention);
		}
	}
}
