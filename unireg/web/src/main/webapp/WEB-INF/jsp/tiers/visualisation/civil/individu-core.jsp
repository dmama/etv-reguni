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
		<td><fmt:message key="label.nom" />&nbsp;:</td>
		<td>
			<c:set var="bind" value="command.${param.path}.nom" scope="request"/>
			<spring:bind path="${bind}" >
				<c:out value="${status.value}"/>
			</spring:bind>
		</td>
	</tr>
	<tr class="<unireg:nextRowClass/>" >
		<td><fmt:message key="label.nom.naissance" />&nbsp;:</td>
		<td>
			<c:set var="bind" value="command.${param.path}.nomNaissance" scope="request"/>
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
		<td><fmt:message key="label.autres.prenoms" />&nbsp;:</td>
		<td>
			<c:set var="bind" value="command.${param.path}.autresPrenoms" scope="request"/>
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
			<fmt:message key="option.sexe.${sexe}" />
		</td>
	</tr>
	<tr class="<unireg:nextRowClass/>" >
		<td><fmt:message key="label.date.naissance" />&nbsp;:</td>
		<td>
			<c:set var="bind" value="command.${param.path}.dateNaissance" scope="request"/>
			<spring:bind path="${bind}" >		
				<c:out value="${status.value}"/>
			</spring:bind>
		</td>
	</tr>
	<tr class="<unireg:nextRowClass/>" >
			<td><fmt:message key="label.etat.civil" />&nbsp;:</td>
			<td>
				<c:set var="bind" value="command.${param.path}.etatCivil" scope="request"/>
				<spring:bind path="${bind}" >
					<c:set var="etatCivil" value="${status.value}"  scope="request"/>
				</spring:bind>
				<c:if test="${etatCivil != ''}">
					<fmt:message key="option.etat.civil.${etatCivil}"/>
				</c:if>
			</td>
	</tr>
	<tr class="<unireg:nextRowClass/>" >
		<td><fmt:message key="label.date.dernier.changement.etat.civil" />&nbsp;:</td>
		<td>
			<c:set var="bind" value="command.${param.path}.dateDernierChgtEtatCivil" scope="request"/>
			<spring:bind path="${bind}" >
				<c:out value="${status.value}" />
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
	<tr class="<unireg:nextRowClass/>" >
		<td><fmt:message key="label.ancien.numero.avs" />&nbsp;:</td>
		<td>
			<c:set var="bind" value="command.${param.path}.ancienNumeroAVS" scope="request"/>
			<spring:bind path="${bind}" >		
					<unireg:ancienNumeroAVS ancienNumeroAVS="${status.value}"></unireg:ancienNumeroAVS>
			</spring:bind> 
		</td>
	</tr>
	<tr class="<unireg:nextRowClass/>" >
		<td><fmt:message key="label.origine" />&nbsp;:</td>
		<td>
			<c:set var="bind" value="command.${param.path}.origine" scope="request"/>
			<spring:bind path="${bind}" >		
				<c:out value="${status.value}"/>
			</spring:bind>
		</td>
	</tr>
	<tr class="<unireg:nextRowClass/>" >
		<td><fmt:message key="label.nationalite" />&nbsp;:</td>
		<td>
			<c:set var="bind" value="command.${param.path}.nationalite" scope="request"/>
			<spring:bind path="${bind}" >		
				<c:out value="${status.value}"/>
			</spring:bind>
		</td>
	</tr>
	<tr class="<unireg:nextRowClass/>" >
		<td><fmt:message key="label.permis.travail" />&nbsp;:</td>
		<td>
			<c:set var="bind" value="command.${param.path}.permisView" scope="request"/>
			<spring:bind path="${bind}" >
				<c:if test="${not empty status.value}">
					<display:table 	name="${status.value}" id="row" pagesize="10">
						<display:column titleKey="label.type" >
							<c:if test="${row.annule}"><strike></c:if>
								<fmt:message key="option.type.permis.${row.typePermis}"/>
							<c:if test="${row.annule}"></strike></c:if>
						</display:column>
						<display:column titleKey="label.date.debut.validite.permis" >
							<c:if test="${row.annule}"><strike></c:if>
								<unireg:regdate regdate="${row.dateDebutValidite}" />
							<c:if test="${row.annule}"></strike></c:if>
						</display:column>
						<display:column titleKey="label.date.fin.validite.permis" >
							<c:if test="${row.annule}"><strike></c:if>
								<unireg:regdate regdate="${row.dateFinValidite}" />
							<c:if test="${row.annule}"></strike></c:if>
						</display:column>
						<display:setProperty name="paging.banner.all_items_found" value=""/>
						<display:setProperty name="paging.banner.one_item_found" value=""/>
					</display:table>
				</c:if>
			</spring:bind>
		</td>
	</tr>
	<tr class="<unireg:nextRowClass/>" >
		<td><fmt:message key="label.numero.registre.etranger" />&nbsp;:</td>
		<td>
			<c:set var="bind" value="command.${param.path}.numeroRCE" scope="request"/>
			<spring:bind path="${bind}" >		
				<c:out value="${status.value}"/>
			</spring:bind>
		</td>
	</tr>
</table>
