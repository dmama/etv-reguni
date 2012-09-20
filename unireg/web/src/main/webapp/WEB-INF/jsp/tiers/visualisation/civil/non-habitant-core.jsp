<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<unireg:nextRowClass reset="1"/>
<table>
	<tr class="<unireg:nextRowClass/>" >
		<td width="50%"><fmt:message key="label.numero.registre.habitant" />&nbsp;:</td>
		<td>
			<c:set var="bind" value="command.${param.path}.numeroIndividuFormatte" scope="request"/>
			<spring:bind path="${bind}" >
				<c:out value="${status.value}"></c:out>
			</spring:bind>
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
		<td><fmt:message key="label.prenom" />&nbsp;:</td>
		<td>
			<c:set var="bind" value="command.${param.path}.prenom" scope="request"/>
			<spring:bind path="${bind}" >
				<c:out value="${status.value}"/>
			</spring:bind>
		</td>
	</tr>
	
	<tr class="<unireg:nextRowClass/>" >
		<td><fmt:message key="label.nouveau.numero.avs" />&nbsp;:</td>
		<td>
			<c:set var="bind" value="command.${param.path}.numeroAssureSocial" scope="request"/>
			<spring:bind path="${bind}" >
					<unireg:numAVS numeroAssureSocial="${status.value}"></unireg:numAVS>	
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
						<unireg:ancienNumeroAVS ancienNumeroAVS="${identification.identifiant}"></unireg:ancienNumeroAVS>
				</c:if>
			</c:forEach>	
		</td>
	</tr>
	
	<tr class="<unireg:nextRowClass/>" >
		<td><fmt:message key="label.date.naissance" />&nbsp;:</td>
		<td>
			<c:set var="bind" value="command.${param.path}.dateNaissance" scope="request"/>
			<spring:bind path="${bind}" >
				<unireg:date date="${status.value}"></unireg:date>
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
		<td width="50%"><fmt:message key="label.date.deces"/>&nbsp;:</td>
		<td>
		<c:set var="bind" value="command.${param.path}.dateDeces" scope="request"/>
		<spring:bind path="${bind}" >
			<unireg:date date="${status.value}"></unireg:date>
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
	 <c:set var="bind" value="command.${param.path}.categorieEtranger" scope="request"/>
	 <spring:bind path="${bind}" >
		<c:set var="categorieEtranger" value="${status.value}"  scope="request"/>
	</spring:bind>
	
	<c:if test="${categorieEtranger != null}">
	<tr class="<unireg:nextRowClass/>" >
		<td><fmt:message key="label.categorie.etranger" />&nbsp;:</td>
		<td><fmt:message key="option.categorie.etranger.${categorieEtranger}"/></td>
	</tr>
	</c:if>

	<tr class="<unireg:nextRowClass/>" >
		<td width="50%"><fmt:message key="label.date.debut.validite.autorisation"/>&nbsp;:</td>
		<td>
		<c:set var="bind" value="command.${param.path}.dateDebutValiditeAutorisation" scope="request"/>
		<spring:bind path="${bind}" >
			<unireg:date date="${status.value}"></unireg:date>
		</spring:bind>                                       
	</tr>

	<tr class="<unireg:nextRowClass/>" >
	<td width="50%"><fmt:message key="label.pays.origine"/>&nbsp;:</td>
		<td>
			<!-- (msi/fde) on a pas trouvé mieux... -->
			<c:choose>
				<c:when test="${param.path == 'tiersPrincipal'}">
					<unireg:infra entityId="${command.tiersPrincipal.numeroOfsNationalite}" entityType="pays" entityPropertyName="nomMinuscule"></unireg:infra>
				</c:when>
				<c:when test="${param.path == 'tiersConjoint'}">
					<unireg:infra entityId="${command.tiersConjoint.numeroOfsNationalite}" entityType="pays" entityPropertyName="nomMinuscule"></unireg:infra>
				</c:when>
				<c:when test="${param.path == 'tiers'}">
					<unireg:infra entityId="${command.tiers.numeroOfsNationalite}" entityType="pays" entityPropertyName="nomMinuscule"></unireg:infra>
				</c:when>
			</c:choose>
		</td>
	</tr>

	<tr class="<unireg:nextRowClass/>" >
	<td width="50%"><fmt:message key="label.commune.origine"/>&nbsp;:</td>
		<td>
			<!-- (msi/fde) on a pas trouvé mieux... -->
			<c:choose>
				<c:when test="${param.path == 'tiersPrincipal'}">
					<unireg:commune ofs="${command.tiersPrincipal.numeroOfsCommuneOrigine}" displayProperty="nomMinuscule"/>
				</c:when>
				<c:when test="${param.path == 'tiersConjoint'}">
					<unireg:commune ofs="${command.tiersConjoint.numeroOfsCommuneOrigine}" displayProperty="nomMinuscule"/>
				</c:when>
				<c:when test="${param.path == 'tiers'}">
					<unireg:commune ofs="${command.tiers.numeroOfsCommuneOrigine}" displayProperty="nomMinuscule"/>
				</c:when>
			</c:choose>
		</td>
	</tr>

</table>

