package ch.vd.uniregctb.indexer;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

import ch.vd.registre.base.utils.Assert;

/**
 * Cette entité est utilisée pour créer les tables pour l'indexer
 * Cette entité ne doot JAMAIS être utilisée en tant que telle
 * @author jec
 *
 */
@SuppressWarnings("unused")
@Entity
@Table(name = "LUCENE_IDX")
public class FakeIndexerTableCreator {

	@Id
	@Column(name="NAME_", length=50)
	private String name;

	@Column(name="VALUE_")
	@Type(type = "org.springframework.orm.hibernate3.support.BlobByteArrayType")
	private byte[] value;

	@Column(name="SIZE_")
	private Integer size;

	@Column(name="LF_")
	private Date lf;

	@Column(name="DELETED_")
	private Boolean deleted;

	public FakeIndexerTableCreator() {

		Assert.fail();
	}

}
