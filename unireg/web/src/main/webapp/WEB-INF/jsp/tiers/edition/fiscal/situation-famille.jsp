<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<!-- Debut Situation famille -->

<fieldset><legend><span><fmt:message key="label.situation.famille.fiscale" /></span></legend>

	<c:if test="${command.situationsFamilleEnErreurMessage != null}">
		<div class="flash-warning"><c:out value="${command.situationsFamilleEnErreurMessage}"/></div>
	</c:if>
	<c:if test="${command.situationsFamilleEnErreurMessage == null}">
		<table border="0">
			<tr><td>
				<a href="situation-famille.do?numero=<c:out value="${command.tiers.numero}"></c:out>" class="add" title="Ajouter situation famille">&nbsp;<fmt:message key="label.bouton.ajouter"/></a>
			</td></tr>
		</table>
		<jsp:include page="../../common/fiscal/situation-famille.jsp">
			<jsp:param name="page" value="edit"/>
		</jsp:include>
	</c:if>

</fieldset>
<script type="text/javascript" language="javascript1.3">
function annulerSituationFamille(idSituationFamille) {
	if(confirm('Voulez-vous vraiment annuler cette situation de famille ?')) {
		var form = F$('theForm');
		form.doPostBack("annulerSituationFamille", idSituationFamille);
 	}
}
</script>
<!-- Fin Situation famille -->