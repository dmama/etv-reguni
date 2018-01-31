<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<unireg:nextRowClass reset="1"/>
<table>
	<tr class="<unireg:nextRowClass/>" >
		<td width="50%"><fmt:message key="label.numero.registre.habitant" />&nbsp;:</td>
		<td>
			<c:set var="bind" value="command.${param.path}" scope="request"/>
			<spring:bind path="${bind}">
				<c:out value="${status.value.numeroIndividuFormatte}"/>
				<authz:authorize ifAnyGranted="ROLE_MODIF_VD_ORD">
					<c:if test="${status.value.canceled}">
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
				</authz:authorize>
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
            <c:if test="${sexe != null}">
                <fmt:message key="option.sexe.${sexe}" />
            </c:if>
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
				<c:if test="${etatCivil != null}">
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
				<unireg:numAVS numeroAssureSocial="${status.value}"/>
			</spring:bind>
		</td>
	</tr>
	<tr class="<unireg:nextRowClass/>" >
		<td><fmt:message key="label.ancien.numero.avs" />&nbsp;:</td>
		<td>
			<c:set var="bind" value="command.${param.path}.ancienNumeroAVS" scope="request"/>
			<spring:bind path="${bind}" >		
				<unireg:ancienNumeroAVS ancienNumeroAVS="${status.value}"/>
			</spring:bind> 
		</td>
	</tr>
	<tr class="<unireg:nextRowClass/>" >
		<td><fmt:message key="label.origine" />&nbsp;:</td>
		<td>
			<c:set var="bind" value="command.${param.path}.origines" scope="request"/>
			<spring:bind path="${bind}" >
				<c:if test="${not empty status.value}">
					<c:set var="separator" value=""/>
					<c:forEach items="${status.value}" var="origin">
						<c:out value="${separator}${origin.nomCommuneAvecCanton}"/>
						<c:set var="separator" value="; "/>
					</c:forEach>
				</c:if>
			</spring:bind>
		</td>
	</tr>
	<tr class="<unireg:nextRowClass/>" >
		<td><fmt:message key="label.nationalites" />&nbsp;:</td>
		<td>
			<c:set var="bind" value="command.${param.path}.nationalites" scope="request"/>
			<spring:bind path="${bind}" >
				<c:if test="${not empty status.value}">
					<display:table 	name="${status.value}" id="row" pagesize="10">
						<display:column titleKey="label.pays" style="width: 33%">
							<c:out value="${row.pays}"/>
						</display:column>
						<display:column titleKey="label.date.annonce" style="width: 33%">
							<unireg:regdate regdate="${row.dateDebut}" />
						</display:column>
						<display:column titleKey="label.date.fin" style="width: 33%">
							<unireg:regdate regdate="${row.dateFin}" />
						</display:column>
						<display:setProperty name="paging.banner.all_items_found" value=""/>
						<display:setProperty name="paging.banner.one_item_found" value=""/>
					</display:table>
				</c:if>
			</spring:bind>
		</td>
	</tr>
	<tr class="<unireg:nextRowClass/>" >
		<td><fmt:message key="label.permis.travail" />&nbsp;:</td>
		<td>
			<c:set var="bind" value="command.${param.path}.permisView" scope="request"/>
			<spring:bind path="${bind}" >
				<c:if test="${not empty status.value}">
					<display:table 	name="${status.value}" id="row" pagesize="10" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
						<display:column titleKey="label.type" style="width: 33%">
							<fmt:message key="option.type.permis.${row.typePermis}"/>
						</display:column>
						<display:column titleKey="label.date.annonce" style="width: 33%">
							<unireg:regdate regdate="${row.dateDebutValidite}" />
						</display:column>
						<display:column titleKey="label.date.fin" style="width: 33%">
							<unireg:regdate regdate="${row.dateFinValidite}" />
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
    <unireg:setAuth var="autorisations" tiersId="${command.tiers.numero}"/>
    <c:if test="${param.pathTiers=='tiers' && autorisations.identificationEntreprise}">
        <table border="0">
            <tr>
                <td>
                    <unireg:raccourciModifier link="../civil/personnephysique/ide/edit.do?id=${command.tiers.numero}" tooltip="Modifier le numéro entreprise"
                                              display="label.bouton.modifier"/>
                </td>
            </tr>
        </table>
    </c:if>
    <jsp:include page="ide.jsp"/>
</fieldset>
