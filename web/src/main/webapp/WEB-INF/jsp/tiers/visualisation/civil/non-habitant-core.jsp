<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<unireg:nextRowClass reset="1"/>
<table>
	<tr class="<unireg:nextRowClass/>" >
		<td width="50%"><fmt:message key="label.numero.registre.habitant" />&nbsp;:</td>
		<td>
			<c:set var="bind" value="command.${param.path}.numeroIndividuFormatte" scope="request"/>
			<spring:bind path="${bind}" >
				<c:out value="${status.value}"/>
			</spring:bind>
			<authz:authorize ifAnyGranted="ROLE_MODIF_VD_ORD">
				<c:set var="bindIndividu" value="command.${param.pathIndividu}" scope="request"/>
				<spring:bind path="${bindIndividu}">
					<c:if test="${status.value != null && status.value.canceled}">
						<span class="warn">
							<c:choose>
								<c:when test="${status.value.numeroIndividuRemplacant != null}">
									<fmt:message key="label.individu.annule.remplace">
										<fmt:param value="${status.value.numeroIndividuRemplacantFormatte}"/>
									</fmt:message>
								</c:when>
								<c:otherwise>
									<fmt:message key="label.individu.annule"/>
								</c:otherwise>
							</c:choose>
						</span>
					</c:if>
				</spring:bind>
			</authz:authorize>
		</td>
	</tr>

	<tr class="<unireg:nextRowClass/>" >
		<td width="50%"><fmt:message key="label.nom" />&nbsp;:</td>
		<td>
			<c:set var="bind" value="command.${param.path}.nom" scope="request"/>
			<spring:bind path="${bind}" >
				<c:out value="${status.value}"/>
			</spring:bind>
		</td>
	</tr>

	<tr class="<unireg:nextRowClass/>" >
		<td width="50%"><fmt:message key="label.nom.naissance" />&nbsp;:</td>
		<td>
			<c:set var="bind" value="command.${param.path}.nomNaissance" scope="request"/>
			<spring:bind path="${bind}" >
				<c:out value="${status.value}"/>
			</spring:bind>
		</td>
	</tr>

	<tr class="<unireg:nextRowClass/>" >
		<td><fmt:message key="label.prenom.usuel" />&nbsp;:</td>
		<td>
			<c:set var="bind" value="command.${param.path}.prenomUsuel" scope="request"/>
			<spring:bind path="${bind}" >
				<c:out value="${status.value}"/>
			</spring:bind>
		</td>
	</tr>

	<tr class="<unireg:nextRowClass/>" >
		<td><fmt:message key="label.prenoms" />&nbsp;:</td>
		<td>
			<c:set var="bind" value="command.${param.path}.tousPrenoms" scope="request"/>
			<spring:bind path="${bind}" >
				<c:out value="${status.value}"/>
			</spring:bind>
		</td>
	</tr>

	<tr class="<unireg:nextRowClass/>" >
		<td><fmt:message key="label.sexe" />&nbsp;:</td>
		<td>
			<c:set var="bind" value="command.${param.path}.sexe" scope="request"/>
			<spring:bind path="${bind}" >
				<c:set var="sexe" value="${status.value}"  scope="request"/>
			</spring:bind>
			<c:if test="${sexe != null }">
				<fmt:message key="option.sexe.${sexe}" />
			</c:if>
		</td>
	</tr>

	<tr class="<unireg:nextRowClass/>" >
		<td><fmt:message key="label.date.naissance" />&nbsp;:</td>
		<td>
			<c:set var="bind" value="command.${param.path}.dateNaissance" scope="request"/>
			<spring:bind path="${bind}" >
				<unireg:date date="${status.value}"/>
			</spring:bind>
		</td>
	</tr>

	<tr class="<unireg:nextRowClass/>" >
		<td width="50%"><fmt:message key="label.date.deces"/>&nbsp;:</td>
		<td>
			<c:set var="bind" value="command.${param.path}.dateDeces" scope="request"/>
			<spring:bind path="${bind}" >
				<unireg:date date="${status.value}"/>
			</spring:bind>
	</tr>

	<tr class="<unireg:nextRowClass/>" >
		<td><fmt:message key="label.nouveau.numero.avs" />&nbsp;:</td>
		<td>
			<c:set var="bind" value="command.${param.path}.numeroAssureSocial" scope="request"/>
			<spring:bind path="${bind}" >
					<unireg:numAVS numeroAssureSocial="${status.value}"/>
			</spring:bind>
		</td>
	</tr>

	<c:set var="bind" value="command.${param.path}.identificationsPersonnes" scope="request"/>
		<spring:bind path="${bind}" >
			<c:set var="identificationsPersonnes" value="${status.value}"  scope="request"/>
	</spring:bind> 
	<tr class="<unireg:nextRowClass/>" >
		<td><fmt:message key="label.ancien.numero.avs" />&nbsp;:</td>
		<td>
			<c:forEach var="identification" items="${identificationsPersonnes}">
				<c:if test="${identification.categorieIdentifiant == 'CH_AHV_AVS'}">
						<unireg:ancienNumeroAVS ancienNumeroAVS="${identification.identifiant}"/>
				</c:if>
			</c:forEach>	
		</td>
	</tr>

	<tr class="<unireg:nextRowClass/>" >
		<td width="50%"><fmt:message key="label.commune.origine"/>&nbsp;:</td>
		<td>
			<c:set var="bind" value="command.${param.path}.origine" scope="request"/>
			<spring:bind path="${bind}" >
				<c:if test="${status.value != null}">
					<c:out value="${status.value.libelleAvecCanton}"/>
				</c:if>
			</spring:bind>
		</td>
	</tr>

	<tr class="<unireg:nextRowClass/>" >
		<td width="50%"><fmt:message key="label.nationalite"/>&nbsp;:</td>
		<td>
			<!-- (msi/fde) on a pas trouvé mieux... -->
			<c:choose>
				<c:when test="${param.path == 'tiersPrincipal'}">
					<unireg:pays ofs="${command.tiersPrincipal.numeroOfsNationalite}" displayProperty="nomCourt"/>
				</c:when>
				<c:when test="${param.path == 'tiersConjoint'}">
					<unireg:pays ofs="${command.tiersConjoint.numeroOfsNationalite}" displayProperty="nomCourt"/>
				</c:when>
				<c:when test="${param.path == 'tiers'}">
					<unireg:pays ofs="${command.tiers.numeroOfsNationalite}" displayProperty="nomCourt"/>
				</c:when>
			</c:choose>
		</td>
	</tr>

	<c:set var="bind" value="command.${param.path}.categorieEtranger" scope="request"/>
	<spring:bind path="${bind}" >
		<c:set var="categorieEtranger" value="${status.value}"  scope="request"/>
	</spring:bind>
	<tr class="<unireg:nextRowClass/>" >
		<td><fmt:message key="label.categorie.etranger" />&nbsp;:</td>
		<td>
			<c:if test="${categorieEtranger != null}">
				<fmt:message key="option.categorie.etranger.${categorieEtranger}"/>
			</c:if>
		</td>
	</tr>

	<tr class="<unireg:nextRowClass/>" >
		<td width="50%"><fmt:message key="label.date.debut.validite.autorisation"/>&nbsp;:</td>
		<td>
			<c:set var="bind" value="command.${param.path}.dateDebutValiditeAutorisation" scope="request"/>
			<spring:bind path="${bind}" >
				<unireg:date date="${status.value}"/>
			</spring:bind>
	</tr>

	<c:set var="bind" value="command.${param.path}.identificationsPersonnes" scope="request"/>
		<spring:bind path="${bind}" >
			<c:set var="identificationsPersonnes" value="${status.value}"  scope="request"/>
	</spring:bind> 
	<tr class="<unireg:nextRowClass/>" >
		<td>
			<fmt:message key="label.numero.registre.etranger" />&nbsp;:
		</td>
		<td>
			<c:forEach var="identification" items="${identificationsPersonnes}">
				<c:if test="${identification.categorieIdentifiant == 'CH_ZAR_RCE'}">						
					${identification.identifiant}
				</c:if>
			</c:forEach>
		</td>
	</tr>

	<tr class="<unireg:nextRowClass/>" >
		<td width="50%"><fmt:message key="label.prenoms.pere"/>&nbsp;:</td>
		<td>
			<c:set var="bind" value="command.${param.path}.prenomsPere" scope="request"/>
			<spring:bind path="${bind}" >
				<c:out value="${status.value}"/>
			</spring:bind>
		</td>
	</tr>
	<tr class="<unireg:nextRowClass/>" >
		<td width="50%"><fmt:message key="label.nom.pere"/>&nbsp;:</td>
		<td>
			<c:set var="bind" value="command.${param.path}.nomPere" scope="request"/>
			<spring:bind path="${bind}" >
				<c:out value="${status.value}"/>
			</spring:bind>
		</td>
	</tr>
	<tr class="<unireg:nextRowClass/>" >
		<td width="50%"><fmt:message key="label.prenoms.mere"/>&nbsp;:</td>
		<td>
			<c:set var="bind" value="command.${param.path}.prenomsMere" scope="request"/>
			<spring:bind path="${bind}" >
				<c:out value="${status.value}"/>
			</spring:bind>
		</td>
	</tr>
	<tr class="<unireg:nextRowClass/>" >
		<td width="50%"><fmt:message key="label.nom.mere"/>&nbsp;:</td>
		<td>
			<c:set var="bind" value="command.${param.path}.nomMere" scope="request"/>
			<spring:bind path="${bind}" >
				<c:out value="${status.value}"/>
			</spring:bind>
		</td>
	</tr>

</table>
<fieldset class="information">
    <legend><span><fmt:message key="label.tiers.information.entreprise"/></span></legend>
	<%--@elvariable id="command" type="ch.vd.unireg.tiers.view.TiersVisuView"--%>
	<unireg:setAuth var="autorisations" tiersId="${command.tiers.numero}"/>
	<c:if test="${(command.natureTiers == 'Habitant' || command.natureTiers == 'NonHabitant') && autorisations.identificationEntreprise}">
        <table border="0">
            <tr>
                <td>
                    <unireg:raccourciModifier link="../civil/personnephysique/ide/edit.do?id=${command.tiers.numero}" tooltip="Modifier le numéro entreprise"
                                              display="label.bouton.modifier"/>
                </td>
            </tr>
        </table>
    </c:if>
	<jsp:include page="ide.jsp">
		<jsp:param name="path" value="${param.path}" />
	</jsp:include>
</fieldset>

