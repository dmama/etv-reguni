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
					<a href="adresse.do?numero=<c:out value="${command.tiers.numero}"></c:out>&height=530&width=850&index=&TB_iframe=true&modal=true" 
					class="add thickbox" title="Ajouter Adresse">&nbsp;<fmt:message key="label.bouton.ajouter" /></a>
				</td>
			</tr>
		</table>
	</c:if>
	
	<jsp:include page="../../common/adresse/adresse.jsp">
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
		