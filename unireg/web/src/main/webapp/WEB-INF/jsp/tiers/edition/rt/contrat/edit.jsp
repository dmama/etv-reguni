<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="provenance" value="${param.provenance}" />
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title"><fmt:message key="title.recapitulatif.rapport.prestation" /></tiles:put>
	<tiles:put name="fichierAide">
		<li>
			<a href="#" onClick="ouvrirAide('<c:url value='/docs/gestion-debiteurIS.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
		</li>
	</tiles:put>
	<tiles:put name="body">
	<form:form method="post" id="formRT">
		<unireg:nextRowClass reset="1"/>

		<table>
		<tr>
			<td id="td_tiers_gauche">
				<!-- Caracteristiques debiteur -->
				<jsp:include page="../../../../general/debiteur.jsp" >
					<jsp:param name="idDebiteur" value="${command.debiteur.numero}" />
				</jsp:include>
			</td>
			<td>
				<%-- Flèche du sens du rapport  --%>
				<table id="flecheSensRapport" cellpadding="0" cellspacing="0">
					<tr>
						<td style="width:1em;"/>
						<td id="flecheGauche" class="fleche_droite_bord_gauche iepngfix"/>
						<td id="flecheMilieu" class="fleche_milieu"><fmt:message key="label.fleche.debiteur.sourcier"/></td>
						<td id="flecheDroite" class="fleche_droite_bord_droit iepngfix"/>
						<td style="width:1em;"/>
					</tr>
				</table>
			</td>
			<td id="td_tiers_droite">
				<!-- Caracteristiques sourcier -->
				<jsp:include page="../../../../general/pp.jsp" >
					<jsp:param name="page" value="rt" />
					<jsp:param name="path" value="sourcier" />
				</jsp:include>
			</td>
		</tr>		
		<tr>
			<td colspan="3">
				<%-- Formulaire de saisie des détails du rapport --%>
				<jsp:include page="rt.jsp"/>
			</td>
		</tr>
		</table>

		<!-- Debut Boutons -->
		<c:if test="${provenance == 'sourcier'}">
			<input type="button" value="<fmt:message key="label.bouton.retour" />" onClick="retourRT(${command.sourcier.numero}, null);" />
		</c:if>
		<c:if test="${provenance == 'debiteur'}">
			<input type="button" value="<fmt:message key="label.bouton.retour" />" onClick="retourRT(null, ${command.debiteur.numero});" />
		</c:if>
		<input type="submit" value="<fmt:message key="label.bouton.sauver" />" />
		<!-- Fin Boutons -->
	</form:form>
	<script>
		function retourRT(numeroSrc, numeroDpi) {
			if(confirm('Voulez-vous quitter cette page sans sauver ?')) {
				if (numeroSrc != null) {
					document.location.href='list-debiteur.do?numeroSrc=' + numeroSrc ;
				}
				if (numeroDpi != null) {
					document.location.href='list-sourcier.do?numeroDpi=' + numeroDpi ;
				}
			}
		}
	</script>
	</tiles:put>
</tiles:insert>