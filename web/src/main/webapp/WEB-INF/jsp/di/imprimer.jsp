<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title"><fmt:message key="label.impression.di" /></tiles:put>
  	<tiles:put name="fichierAide"><li><a href="#" onClick="ouvrirAide('<c:url value="/docs/maj-di.pdf"/>');" title="AccessKey: a" accesskey="e">Aide</a></li></tiles:put>
	<tiles:put name="body">

	<unireg:nextRowClass reset="1"/>
	<unireg:bandeauTiers numero="${command.tiersId}" showAvatar="false" showValidation="false" showEvenementsCivils="false" showLinks="false" />

	<form:form name="theForm" method="post" action="imprimer.do">

		<form:errors cssClass="error"/>

		<input type="hidden" name="tiersId" value="${command.tiersId}"/>
		<input type="hidden" name="periodeFiscale" value="${command.periodeFiscale}"/>
		<input type="hidden" name="depuisTache" value="${command.depuisTache}"/>
		<input type="hidden" name="dateRetourProposeeCarDeclarationRetourneeAnnuleeExiste" value="${command.dateRetourProposeeCarDeclarationRetourneeAnnuleeExiste}"/>
		<input type="hidden" name="typeContribuable" value="${command.typeContribuable}"/>

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
									<jsp:param name="mandatory" value="true" />
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
									<jsp:param name="mandatory" value="true" />
								</jsp:include>
							</c:if>
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/>" >
						<td width="25%"><fmt:message key="label.type.declaration" />&nbsp;:</td>
						<td width="25%">
							<c:choose>
								<c:when test="${command.typeContribuable == 'PP'}">
									<%--@elvariable id="typesDeclarationImpot" type="java.util.Map<TypeDocument, String>"--%>
									<form:select path="typeDocument" items="${typesDeclarationImpot}" />
									<span class="mandatory">*</span>
								</c:when>
								<c:otherwise>
									<c:if test="${command.typeDocument != null}"><fmt:message key="option.type.document.${command.typeDocument}"/></c:if>
									<input type="hidden" name="typeDocument" value="${command.typeDocument}"/>
								</c:otherwise>
							</c:choose>
						</td>
						<td width="25%"><fmt:message key="label.date.delai.accorde" />&nbsp;:</td>
						<td width="25%">
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="delaiAccorde" />
								<jsp:param name="id" value="delaiAccorde" />
								<jsp:param name="mandatory" value="true" />
							</jsp:include>
						</td>
					</tr>
					<c:if test="${command.typeContribuable == 'PP' || (command.allowedQuittancement && command.dateRetour != null && command.dateRetourProposeeCarDeclarationRetourneeAnnuleeExiste)}">
						<tr class="<unireg:nextRowClass/>" >
							<c:choose>
								<c:when test="${command.typeContribuable == 'PP'}">
									<td width="25%"><fmt:message key="label.type.adresse.retour" />&nbsp;:</td>
									<td width="25%">
											<%--@elvariable id="typesAdresseRetour" type="java.util.Map<TypeAdresseRetour, String>"--%>
										<form:select path="typeAdresseRetour" items="${typesAdresseRetour}" />
										<span class="mandatory">*</span>
									</td>
								</c:when>
								<c:otherwise>
									<td width="25%">&nbsp;</td>
									<td width="25%">&nbsp;</td>
								</c:otherwise>
							</c:choose>
							<c:choose>
								<c:when test="${command.allowedQuittancement && command.dateRetour != null && command.dateRetourProposeeCarDeclarationRetourneeAnnuleeExiste}">
									<td width="25%"><fmt:message key="label.date.retour" />&nbsp;:</td>
									<td width="25%">
										<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
											<jsp:param name="path" value="dateRetour" />
											<jsp:param name="id" value="dateRetour" />
											<jsp:param name="inputFieldClass" value="flash" />
										</jsp:include>
									</td>
								</c:when>
								<c:otherwise>
									<td width="25%">&nbsp;</td>
									<td width="25%">&nbsp;</td>
								</c:otherwise>
							</c:choose>
						</tr>
					</c:if>
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

		<c:choose>
			<c:when test="${command.imprimable}">
				<input type="button" name="imprimerDI" value="<fmt:message key="label.bouton.imprimer"/>" onclick="return Page_ImprimerDI(this);"/>
			</c:when>
			<c:when test="${command.generableNonImprimable}">
				<input type="button" name="genererDI" value="<fmt:message key="label.bouton.sauver.sans.imprimer"/>" onclick="return Page_ImprimerDI(this);"/>
			</c:when>
			<c:otherwise>
				<input type="button" disabled="disabled" value="<fmt:message key="label.bouton.imprimer"/>"/>
			</c:otherwise>
		</c:choose>

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
			    Modifier.isModified = false; // du moment qu'on imprie la DI, les valeurs saisies ont été prises en compte
			    $(button).closest("form").submit();
			    button.disabled = true;
			    return true;
		    }

	</script>

	</tiles:put>
</tiles:insert>
