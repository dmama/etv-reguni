package ch.vd.uniregctb.migration.pm.log;

import java.util.function.BinaryOperator;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.StringRenderer;
import ch.vd.uniregctb.migration.pm.engine.collector.EntityLinkCollector;
import ch.vd.uniregctb.migration.pm.regpm.NumeroIDE;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Les différents champs loggués pour tous les éléments
 */
public enum LoggedElementAttribute {

	//
	// un message explicatif
	//

	NIVEAU(LogLevel.class, Enum::name, (e1, e2) -> LogLevel.values()[Math.max(e1.ordinal(), e2.ordinal())]),
	MESSAGE(String.class, s -> s, (s1, s2) -> String.format("%s // %s", s1, s2)),

	//
	// spécifiques aux entreprises
	//

	ENTREPRISE_ID(Number.class, Object::toString, LoggedElementHelper.<Number>exceptionThrowing()),
	ENTREPRISE_FLAG_ACTIF(Boolean.class, b -> b ? "Active" : "Inactive", LoggedElementHelper.<Boolean>exceptionThrowing()),
	ENTREPRISE_NO_IDE(NumeroIDE.class, no -> String.format("%s%d", no.getCategorie(), no.getNumero()), LoggedElementHelper.<NumeroIDE>exceptionThrowing()),
	ENTREPRISE_ID_CANTONAL(Number.class, Object::toString, LoggedElementHelper.<Number>exceptionThrowing()),

	//
	// spécifiques aux établissements
	//

	ETABLISSEMENT_ID(Number.class, Object::toString, LoggedElementHelper.<Number>exceptionThrowing()),
	ETABLISSEMENT_ID_UNIREG(Number.class, Object::toString, LoggedElementHelper.<Number>exceptionThrowing()),
	ETABLISSEMENT_ENTREPRISE_ID(Number.class, Object::toString, LoggedElementHelper.<Number>exceptionThrowing()),
	ETABLISSEMENT_INDIVIDU_ID(Number.class, Object::toString, LoggedElementHelper.<Number>exceptionThrowing()),
	ETABLISSEMENT_NO_IDE(NumeroIDE.class, no -> String.format("%s%d", no.getCategorie(), no.getNumero()), LoggedElementHelper.<NumeroIDE>exceptionThrowing()),
	ETABLISSEMENT_ID_CANTONAL(Number.class, Object::toString, LoggedElementHelper.<Number>exceptionThrowing()),

	//
	// spécifiques aux individus
	//

	INDIVIDU_ID(Number.class, Object::toString, LoggedElementHelper.<Number>exceptionThrowing()),
	INDIVIDU_ID_UNIREG(Number.class, Object::toString, LoggedElementHelper.<Number>exceptionThrowing()),
	INDIVIDU_NOM(String.class, s -> s, LoggedElementHelper.<String>exceptionThrowing()),
	INDIVIDU_PRENOM(String.class, s -> s, LoggedElementHelper.<String>exceptionThrowing()),
	INDIVIDU_SEXE(Sexe.class, Enum::name, LoggedElementHelper.<Sexe>exceptionThrowing()),
	INDIVIDU_DATE_NAISSANCE(RegDate.class, RegDateHelper::dateToDashString, LoggedElementHelper.<RegDate>exceptionThrowing()),

	//
	// spécifiques aux adresses
	//

	ADRESSE_RUE(String.class, s -> s, LoggedElementHelper.<String>exceptionThrowing()),
	ADRESSE_NO_POLICE(String.class, s -> s, LoggedElementHelper.<String>exceptionThrowing()),
	ADRESSE_LIEU(String.class, s -> s, LoggedElementHelper.<String>exceptionThrowing()),
	ADRESSE_LOCALITE(String.class, s -> s, LoggedElementHelper.<String>exceptionThrowing()),
	ADRESSE_PAYS(Number.class, Object::toString, LoggedElementHelper.<Number>exceptionThrowing()),

	//
	// spécifiques aux rapports entre tiers
	//

	RET_TYPE(EntityLinkCollector.LinkType.class, Enum::name, LoggedElementHelper.<EntityLinkCollector.LinkType>exceptionThrowing()),
	RET_DATE_DEBUT(RegDate.class, RegDateHelper::dateToDashString, LoggedElementHelper.<RegDate>exceptionThrowing()),
	RET_DATE_FIN(RegDate.class, RegDateHelper::dateToDashString, LoggedElementHelper.<RegDate>exceptionThrowing()),
	RET_SRC_ENTREPRISE_ID(Number.class, Object::toString, LoggedElementHelper.<Number>exceptionThrowing()),
	RET_SRC_ETABLISSEMENT_ID(Number.class, Object::toString, LoggedElementHelper.<Number>exceptionThrowing()),
	RET_SRC_INDIVIDU_ID(Number.class, Object::toString, LoggedElementHelper.<Number>exceptionThrowing()),
	RET_SRC_UNIREG_ID(Number.class, Object::toString, LoggedElementHelper.<Number>exceptionThrowing()),
	RET_DEST_ENTREPRISE_ID(Number.class, Object::toString, LoggedElementHelper.<Number>exceptionThrowing()),
	RET_DEST_ETABLISSEMENT_ID(Number.class, Object::toString, LoggedElementHelper.<Number>exceptionThrowing()),
	RET_DEST_INDIVIDU_ID(Number.class, Object::toString, LoggedElementHelper.<Number>exceptionThrowing()),
	RET_DEST_UNIREG_ID(Number.class, Object::toString, LoggedElementHelper.<Number>exceptionThrowing()),

	//
	// utilisables dans les diverses listes de contrôle
	//

	RAISON_SOCIALE(String.class, s -> s, LoggedElementHelper.<String>exceptionThrowing()),
	NOM_COMMUNE(String.class, s -> s, LoggedElementHelper.<String>exceptionThrowing()),
	TYPE_AUTORITE_FISCALE(TypeAutoriteFiscale.class, Enum::name, LoggedElementHelper.<TypeAutoriteFiscale>exceptionThrowing()),
	NO_OFS(Number.class, Object::toString, LoggedElementHelper.<Number>exceptionThrowing()),
	DATE_DEBUT_FOR(RegDate.class, RegDateHelper::dateToDashString, LoggedElementHelper.<RegDate>exceptionThrowing()),
	DATE_FIN_FOR(RegDate.class, RegDateHelper::dateToDashString, LoggedElementHelper.<RegDate>exceptionThrowing()),
	DATE_FIN_ASSUJETTISSEMENT(RegDate.class, RegDateHelper::dateToDashString, LoggedElementHelper.<RegDate>exceptionThrowing()),
	MOTIF_RATTACHEMENT(MotifRattachement.class, Enum::name, LoggedElementHelper.<MotifRattachement>exceptionThrowing()),
	TYPE_ENTITE(Class.class, Class::getSimpleName, LoggedElementHelper.<Class>exceptionThrowing()),

	;

	/**
	 * Metadonnée associée à la valeur énumérée
	 * @param <T> type de la donnée associée à la valeur énumérée
	 */
	private static class AttributeMetaData<T> {

		private final Class<T> attributeClazz;
		private final StringRenderer<? super T> renderer;
		private final BinaryOperator<T> merger;

		public AttributeMetaData(Class<T> attributeClazz, StringRenderer<? super T> renderer, BinaryOperator<T> merger) {
			this.attributeClazz = attributeClazz;
			this.renderer = renderer;
			this.merger = merger;
		}
	}

	private final AttributeMetaData<?> metaData;

	<T> LoggedElementAttribute(Class<T> valueClazz, StringRenderer<? super T> renderer, BinaryOperator<T> merger) {
		this.metaData = new AttributeMetaData<>(valueClazz, renderer, merger);
	}

	@NotNull
	public <T> Class<T> getValueClass() {
		//noinspection unchecked
		return ((AttributeMetaData<T>) metaData).attributeClazz;
	}

	@NotNull
	public <T> StringRenderer<? super T> getValueRenderer() {
		//noinspection unchecked
		return ((AttributeMetaData<T>) metaData).renderer;
	}

	@NotNull
	public <T> BinaryOperator<T> getValueMerger() {
		//noinspection unchecked
		return ((AttributeMetaData<T>) metaData).merger;
	}
}
