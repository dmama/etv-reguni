<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<script type="text/javascript" language="Javascript" src="<c:url value="/js/dialog.js"/>"></script>

<!-- Debut Fiscal -->
<c:if test="${command.allowedOnglet.FISCAL}">
	<table border="0">
		<tr>
			<td>
				<unireg:raccourciModifier link="../fiscal/edit.do?id=${command.tiers.numero}" tooltip="Modifier la partie fiscale" display="label.bouton.modifier"/>
			</td>

			<td id="timeline" style="display:none" align="right">
				<a href="<c:url value="/tiers/timeline.do?id=" /><c:out value="${command.tiers.numero}" />" >Vue chronologique</a>
			</td>
		</tr>
	</table>
	
	<script type="text/javascript">
	    // on n'affiche le lien vers la timeline que dans l'environnement de développement
		if (is_dev_env()) {
			var td = document.getElementById('timeline');
			td.style.display = '';
		}
	</script>
</c:if>	
<c:choose>
	<c:when test="${command.natureTiers == 'DebiteurPrestationImposable'}">
		<jsp:include page="debiteur.jsp"/>
		<c:if test="${command.allowedOnglet.FISCAL}">
			<table border="0">
				<tr><td>
					<unireg:raccourciModifier link="../fiscal/edit-for-debiteur.do?id=${command.tiers.numero}" tooltip="Modifier les fors du débiteur" display="label.bouton.modifier"/>
				</td></tr>
			</table>
		</c:if>
		
		<jsp:include page="for-debiteur.jsp"/>
	</c:when>
	<c:when test="${command.natureTiers != 'DebiteurPrestationImposable'}">
		<jsp:include page="for.jsp"/>	
		<jsp:include page="situation-famille.jsp"/>	
	</c:when>
</c:choose>

<!-- Fin Fiscal -->
