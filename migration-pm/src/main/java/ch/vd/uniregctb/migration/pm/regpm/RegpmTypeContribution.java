package ch.vd.uniregctb.migration.pm.regpm;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import ch.vd.uniregctb.migration.pm.regpm.usertype.CodeCollectiviteUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.FixedCharUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.ObjectImpotUserType;

@Entity
@Table(name = "TY_CONTRIBUTION")
@TypeDefs({
		          @TypeDef(name = "FixedChar", typeClass = FixedCharUserType.class),
		          @TypeDef(name = "CodeCollectivite", typeClass = CodeCollectiviteUserType.class),
		          @TypeDef(name = "ObjectImpot", typeClass = ObjectImpotUserType.class)
          })
public class RegpmTypeContribution extends RegpmEntity implements WithLongId {

	private Long id;
	private String codeContribution;
	private RegpmCodeCollectivite codeCollectivite;
	private String designationAbregee;
	private String designationLongue1;
	private String designationLongue2;
	private String genreContribution;
	private RegpmObjectImpot objectImpot;

	@Id
	@Column(name = "NO_TECHNIQUE")
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "CO_CONTRIBUTION")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "5"))
	public String getCodeContribution() {
		return codeContribution;
	}

	public void setCodeContribution(String codeContribution) {
		this.codeContribution = codeContribution;
	}

	@Column(name = "CO_COLLECTIVITE")
	@Type(type = "CodeCollectivite")
	public RegpmCodeCollectivite getCodeCollectivite() {
		return codeCollectivite;
	}

	public void setCodeCollectivite(RegpmCodeCollectivite codeCollectivite) {
		this.codeCollectivite = codeCollectivite;
	}

	@Column(name = "DESIGN_ABREGEE")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "25"))
	public String getDesignationAbregee() {
		return designationAbregee;
	}

	public void setDesignationAbregee(String designationAbregee) {
		this.designationAbregee = designationAbregee;
	}

	@Column(name = "DESIGN_LONGUE_1")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "40"))
	public String getDesignationLongue1() {
		return designationLongue1;
	}

	public void setDesignationLongue1(String designationLongue1) {
		this.designationLongue1 = designationLongue1;
	}

	@Column(name = "DESIGN_LONGUE_2")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "40"))
	public String getDesignationLongue2() {
		return designationLongue2;
	}

	public void setDesignationLongue2(String designationLongue2) {
		this.designationLongue2 = designationLongue2;
	}

	@Column(name = "GENRE_CONTRIBUTION")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "1"))
	public String getGenreContribution() {
		return genreContribution;
	}

	public void setGenreContribution(String genreContribution) {
		this.genreContribution = genreContribution;
	}

	@Column(name = "FK_OBJIMPCO")
	@Type(type = "ObjectImpot")
	public RegpmObjectImpot getObjectImpot() {
		return objectImpot;
	}

	public void setObjectImpot(RegpmObjectImpot objectImpot) {
		this.objectImpot = objectImpot;
	}
}
