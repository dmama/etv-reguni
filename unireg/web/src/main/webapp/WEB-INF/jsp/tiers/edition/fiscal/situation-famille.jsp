<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<!-- Debut Situation famille -->

<fieldset><legend><span><fmt:message key="label.situation.famille.fiscale" /></span></legend>
	<table border="0">
		<tr>
			<td>
				<a href="situation-famille.do?numero=<c:out value="${command.tiers.numero}"></c:out>&height=250&width=900&index=&TB_iframe=true&modal=true" 
				class="add thickbox" title="Ajouter situation famille">&nbsp;<fmt:message key="label.bouton.ajouter" /></a>
			</td>
		</tr>
	</table>
	<jsp:include page="../../common/fiscal/situation-famille.jsp">
		<jsp:param name="page" value="edit"/>
	</jsp:include>

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