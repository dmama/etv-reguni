package ch.vd.uniregctb.migration.pm.regpm;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import java.io.Serializable;
import java.sql.Timestamp;

import org.hibernate.annotations.Columns;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import ch.vd.uniregctb.migration.pm.regpm.usertype.FixedCharUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.MutationTimestampUserType;

@MappedSuperclass
@TypeDefs({
		          @TypeDef(name = "Timestamp", typeClass = MutationTimestampUserType.class),
		          @TypeDef(name = "FixedChar", typeClass = FixedCharUserType.class)
         })
public abstract class RegpmEntity implements Serializable {

	private Timestamp lastMutationTimestamp;
	private String lastMutationOperator;

	@Columns(columns = {@Column(name = "DA_MUT"), @Column(name = "HR_MUT")})
	@Type(type = "Timestamp")
	public Timestamp getLastMutationTimestamp() {
		return lastMutationTimestamp;
	}

	public void setLastMutationTimestamp(Timestamp lastMutationTimestamp) {
		this.lastMutationTimestamp = lastMutationTimestamp;
	}

	@Column(name = "VS_MUT")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "8"))
	public String getLastMutationOperator() {
		return lastMutationOperator;
	}

	public void setLastMutationOperator(String lastMutationOperator) {
		this.lastMutationOperator = lastMutationOperator;
	}
}
