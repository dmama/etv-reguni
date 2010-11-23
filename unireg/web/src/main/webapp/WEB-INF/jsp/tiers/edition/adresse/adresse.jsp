<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut Adresse -->

<fieldset>

<legend><span>
<fmt:message key="label.adresse" /></span></legend>

	<c:if test="${command.adressesEnErreur == null}">
		<table border="0">
			<tr>
				<td>
					<unireg:raccourciAjouter link="adresse.do?numero=${command.tiers.numero}" tooltip="Ajouter Adresse" display="label.bouton.ajouter"/>
				</td>
			</tr>
		</table>
	</c:if>
	
	<jsp:include page="../../common/adresse/adresseFiscale.jsp">
		<jsp:param name="page" value="edit"/>
	</jsp:include>
	
</fieldset>
<script type="text/javascript" language="Javascript1.3">
function annulerAdresse(idAdresse) {
	if(confirm('Voulez-vous vraiment annuler cette adresse surchargée ?')) {
		var form = F$("theForm");
		form.doPostBack("annulerAdresse", idAdresse);
 	}
}	
</script>
<!-- Fin Adresse -->
		