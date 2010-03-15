
package ch.vd.uniregctb.webservices.batch;

import javax.xml.bind.annotation.XmlEnum;


/**
 * <p>Java class for jobStatut.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="jobStatut">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="JOB_INTERRUPTED"/>
 *     &lt;enumeration value="JOB_EXCEPTION"/>
 *     &lt;enumeration value="JOB_RUNNING"/>
 *     &lt;enumeration value="JOB_READY"/>
 *     &lt;enumeration value="JOB_OK"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlEnum
public enum JobStatut {

    JOB_EXCEPTION,
    JOB_INTERRUPTED,
    JOB_OK,
    JOB_READY,
    JOB_RUNNING;

    public String value() {
        return name();
    }

    public static JobStatut fromValue(String v) {
        return valueOf(v);
    }

}
