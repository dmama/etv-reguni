<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title"><fmt:message key="label.impression.di" /></tiles:put>
  	<tiles:put name="fichierAide"><a href="#" onClick="ouvrirAide('<c:url value="/docs/maj-di.pdf"/>');" title="AccessKey: a" accesskey="e">Aide</a></tiles:put>
	<tiles:put name="body">

	<unireg:nextRowClass reset="1"/>
	<unireg:bandeauTiers numero="${command.tiersId}" showAvatar="false" showValidation="false" showEvenementsCivils="false" showLinks="false" />

	<form:form name="theForm" method="post" action="imprimer.do">

		<form:errors cssClass="error"/>

		<input type="hidden" name="tiersId" value="${command.tiersId}"/>
		<input type="hidden" name="periodeFiscale" value="${command.periodeFiscale}"/>
		<input type="hidden" name="depuisTache" value="${command.depuisTache}"/>
		<input type="hidden" name="dateRetourProposeeCarDeclarationRetourneeAnnuleeExiste" value="${command.dateRetourProposeeCarDeclarationRetourneeAnnuleeExiste}"/>

		<c:if test="${command.periodeFiscale != null}">

			<!-- Debut di -->
			<fieldset class="information">
				<legend><span><fmt:message key="label.caracteristiques.di" /></span></legend>

				<c:if test="${command.allowedQuittancement && command.dateRetour != null && command.dateRetourProposeeCarDeclarationRetourneeAnnuleeExiste}">
					<div class="flash"><fmt:message key="label.di.proposition.retour.car.annulee.existe"/></div>
				</c:if>

				<table border="0">
					<tr class="<unireg:nextRowClass/>" >
						<td width="25%"><fmt:message key="label.periode.fiscale" />&nbsp;:</td>
						<td width="25%">
							${command.periodeFiscale}</td>
						<td width="25%"></td>
						<td width="25%"></td>
					</tr>
					<tr class="<unireg:nextRowClass/>" >
						<td width="25%"><fmt:message key="label.date.debut.periode.imposition" />&nbsp;:</td>
						<td width="25%">
							<c:if test="${not command.ouverte}">
								<input type="hidden" name="dateDebutPeriodeImposition" value="<unireg:regdate regdate="${command.dateDebutPeriodeImposition}"/>"/>
								<unireg:regdate regdate="${command.dateDebutPeriodeImposition}"/>
							</c:if>
							<c:if test="${command.ouverte}">
								<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
									<jsp:param name="path" value="dateDebutPeriodeImposition" />
									<jsp:param name="id" value="dateDebutPeriodeImposition" />
								</jsp:include>
							</c:if>
						</td>
						<td width="25%"><fmt:message key="label.date.fin.periode.imposition" />&nbsp;:</td>
						<td width="25%">
							<c:if test="${not command.ouverte}">
								<input type="hidden" name="dateFinPeriodeImposition" value="<unireg:regdate regdate="${command.dateFinPeriodeImposition}"/>"/>
								<unireg:regdate regdate="${command.dateFinPeriodeImposition}"/>
							</c:if>
							<c:if test="${command.ouverte}">
								<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
									<jsp:param name="path" value="dateFinPeriodeImposition" />
									<jsp:param name="id" value="dateFinPeriodeImposition" />
								</jsp:include>
							</c:if>
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/>" >
						<td width="25%"><fmt:message key="label.type.declaration" />&nbsp;:</td>
						<td width="25%">
							<%--@elvariable id="typesDeclarationImpot" type="java.util.Map<TypeDocument, String>"--%>
							<form:select path="typeDocument" items="${typesDeclarationImpot}" />
						</td>
						<td width="25%"><fmt:message key="label.date.delai.accorde" />&nbsp;:</td>
						<td width="25%">
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="delaiAccorde" />
								<jsp:param name="id" value="delaiAccorde" />
							</jsp:include>
							<span style="color:red;">*</span>
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/>" >
						<td width="25%"><fmt:message key="label.type.adresse.retour" />&nbsp;:</td>
						<td width="25%">
							<%--@elvariable id="typesAdresseRetour" type="java.util.Map<TypeAdresseRetour, String>"--%>
							<form:select path="typeAdresseRetour" items="${typesAdresseRetour}" />
						</td>
						<c:if test="${command.allowedQuittancement && command.dateRetour != null && command.dateRetourProposeeCarDeclarationRetourneeAnnuleeExiste}">
							<td width="25%"><fmt:message key="label.date.retour" />&nbsp;:</td>
							<td width="25%">
								<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
									<jsp:param name="path" value="dateRetour" />
									<jsp:param name="id" value="dateRetour" />
									<jsp:param name="inputFieldClass" value="flash" />
								</jsp:include>
							</td>
						</c:if>
						<c:if test="${!command.allowedQuittancement || command.dateRetour == null || !command.dateRetourProposeeCarDeclarationRetourneeAnnuleeExiste}">
							<td width="25%">&nbsp;</td>
							<td width="25%">&nbsp;</td>
						</c:if>
					</tr>
				</table>

			</fieldset>
			<!-- Fin di -->
		</c:if>

		<!-- Debut Boutons -->
		<c:if test="${not command.depuisTache}">
			<input type="button" name="retourDI" value="<fmt:message key="label.bouton.retour" />" onclick="Page_RetourEdition(${command.tiersId});" />
		</c:if>
		<c:if test="${command.depuisTache}">
			<unireg:buttonTo name="Retour" action="/tache/list.do" method="get" />
		</c:if>

		<input type="button" name="imprimerDI" <c:if test="${!command.imprimable}">disabled="disabled"</c:if>
		       value="<fmt:message key="label.bouton.imprimer" />" onclick="return Page_ImprimerDI(this);" />
		<!-- Fin Boutons -->

	</form:form>

	<script type="text/javascript">
			/**
			 * Initialisation de l'observeur du flag 'modifier'
			 */
			Modifier.attachObserver( "theForm", false);
			Modifier.messageSaveSubmitConfirmation = 'Voulez-vous vraiment sauver cette déclaration d\'impôt ?';
			Modifier.messageOverConfirmation = "Voulez-vous vraiment quitter cette page sans sauver ?";

			function Page_RetourEdition(numero) {
				<c:if test="${command.imprimable == true}">
					if (Modifier.isModified) {
						if (!confirm(Modifier.messageOverConfirmation)) {
							return false;
						}
					}
				</c:if>
				document.location.href='<c:url value="/di/list.do"/>?tiersId=' + numero ;
				return true;
			}
			
		 	function Page_ImprimerDI(button) {
				 $('span.error').hide(); // on cache d'éventuelles erreurs datant d'un ancien submit
				 $(button).closest("form").submit();
				 button.disabled = true;
				return true;
		 	}
	</script>

	</tiles:put>
</tiles:insert>
