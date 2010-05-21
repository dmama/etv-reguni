<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="page" value="${param.page}" />
<c:if test="${page == 'visu' }">
	<c:set var="url" value="visu.do" />
	<c:set var="adresses" value="${command.historiqueAdressesFiscales}" />
</c:if>
<c:if test="${page == 'edit' }">
	<c:set var="url" value="edit.do" />
	<c:set var="adresses" value="${command.adressesActives}" />
</c:if>

<c:if test="${not empty adresses}">	
<display:table name="${adresses}" id="adresseFiscale" pagesize="10" requestURI="${url}" class="display">
		<display:column  sortable ="true" titleKey="label.utilisationAdresse">
			<c:if test="${adresseFiscale.annule}"><strike></c:if>
				<fmt:message key="option.usage.${adresseFiscale.usage}" />
			<c:if test="${adresseFiscale.annule}"></strike></c:if>
		</display:column>
		<display:column sortable ="true" titleKey="label.date.debut" sortProperty="dateDebut">
			<c:if test="${adresseFiscale.annule}"><strike></c:if>
				<fmt:formatDate value="${adresseFiscale.dateDebut}" pattern="dd.MM.yyyy"/>
			<c:if test="${adresseFiscale.annule}"></strike></c:if>
		</display:column>
		<display:column sortable ="true" titleKey="label.date.fin" sortProperty="dateFin">
			<c:if test="${adresseFiscale.annule}"><strike></c:if>
				<fmt:formatDate value="${adresseFiscale.dateFin}" pattern="dd.MM.yyyy"/>
			<c:if test="${adresseFiscale.annule}"></strike></c:if>
		</display:column>
		<display:column sortable="true" titleKey="label.adresse.complement">
			<c:if test="${adresseFiscale.annule}"><strike></c:if>
			${adresseFiscale.complements}
			<c:if test="${adresseFiscale.annule}"></strike></c:if>
		</display:column>
		<display:column sortable ="true" titleKey="label.rueCasePostale">
			<c:if test="${adresseFiscale.annule}"><strike></c:if>
				${adresseFiscale.rue}
			<c:if test="${adresseFiscale.annule}"></strike></c:if>
		</display:column>
		<display:column sortable ="true" titleKey="label.localite" >
			<c:if test="${adresseFiscale.annule}"><strike></c:if>
				${adresseFiscale.localite}
			<c:if test="${adresseFiscale.annule}"></strike></c:if>
		</display:column>
		<display:column sortable ="true" titleKey="label.pays" >
			<c:if test="${adresseFiscale.annule}"><strike></c:if>
				<c:if test="${adresseFiscale.paysOFS != null }">
					<unireg:infra entityId="${adresseFiscale.paysOFS}" entityType="pays" entityPropertyName="nomMinuscule"></unireg:infra>
				</c:if>
			<c:if test="${adresseFiscale.annule}"></strike></c:if>
		</display:column>
		<display:column sortable ="true" titleKey="label.adresse.permanente" >
			<c:if test="${!adresseFiscale.annule}">
				<c:if test="${adresseFiscale.id != null}">
					<input type="checkbox" <c:if test="${adresseFiscale.permanente}">checked</c:if> disabled="disabled" />
				</c:if>
			</c:if>
		</display:column>
		<display:column sortable ="true" titleKey="label.adresse.source">
			<c:if test="${adresseFiscale.annule}"><strike></c:if>
				<fmt:message key="option.source.${adresseFiscale.source}" />
				<c:if test="${adresseFiscale.default}">(<fmt:message key="option.source.default.tag" />)</c:if>
			<c:if test="${adresseFiscale.annule}"></strike></c:if>
		</display:column>
		<display:column style="action">
			<c:if test="${page == 'visu' }">
				<c:if test="${adresseFiscale.id != null}">
					<unireg:raccourciConsulter link="../common/consult-log.do?height=200&width=800&nature=AdresseTiers&id=${adresse.id}&TB_iframe=true&modal=true" thickbox="true" tooltip="Edition des logs"/>
				</c:if>
			</c:if>
			<c:if test="${page == 'edit' }">
				<c:if test="${!adresseFiscale.annule && adresseFiscale.source !='CIVILE' && adresseFiscale.id!= null && adresseFiscale.id!='' }">
					<c:if test="${((adresseFiscale.usage == 'COURRIER') && (command.allowedOnglet.ADR_C)) ||
					((adresseFiscale.usage == 'POURSUITE') && (command.allowedOnglet.ADR_P)) ||
					((adresseFiscale.usage == 'REPRESENTATION') && (command.allowedOnglet.ADR_B)) ||
					((adresseFiscale.usage == 'DOMICILE') && (command.allowedOnglet.ADR_D))}">
						<!-- <a href="adresse.do?height=530&width=850&idAdresse=${adresseFiscale.id}&numero=<c:out value="${command.tiers.numero}"></c:out>&TB_iframe=true&modal=true" class="thickbox edit" title="Edition d'adresse">&nbsp;</a> -->
						<unireg:raccourciAnnuler onClick="javascript:annulerAdresse(${adresseFiscale.id});"/>
					</c:if>
				</c:if>
			</c:if>
		</display:column>
		<display:setProperty name="paging.banner.all_items_found" value=""/>
		<display:setProperty name="paging.banner.one_item_found" value=""/>

	</display:table>
</c:if>

<c:if test="${command.adressesEnErreur != null}">
	<span class="error"><fmt:message key="error.adresse.fiscale.entete" /></span><br/>
	<span class="error">=&gt;&nbsp;${command.adressesEnErreurMessage}</span><br/><br/>
		
	<fmt:message key="error.adresse.fiscale.source.erreur" /><br/>
	
	<display:table name="command.adressesEnErreur" id="adresseEnErreur" pagesize="10" class="display">
		<display:column  sortable ="true" titleKey="label.utilisationAdresse" class="error">
			<fmt:message key="option.usage.${adresseEnErreur.usage}" />
		</display:column>
		<display:column property="dateDebut" sortable ="true" titleKey="label.date.debut"  format="{0,date,dd.MM.yyyy}" class="error" />
		<display:column property="dateFin" sortable ="true" titleKey="label.date.fin"  format="{0,date,dd.MM.yyyy}" class="error" />
		<display:column sortable ="true" titleKey="label.adresse.source" class="error">
			<fmt:message key="option.source.${adresseEnErreur.source}" />
			<c:if test="${adresseEnErreur.default}">(<fmt:message key="option.source.default.tag" />)</c:if>
		</display:column>
		<display:column style="action">
			<c:if test="${page == 'visu' }">
				<c:if test="${adresseFiscale.id != null}">
					<unireg:raccourciConsulter link="../common/consult-log.do?height=200&width=800&nature=AdresseTiers&id=${adresseFiscale.id}&TB_iframe=true&modal=true" thickbox="true" tooltip="Edition des logs"/>
				</c:if>
			</c:if>
			<c:if test="${page == 'edit' }">
				<c:if test="${!adresseEnErreur.annule && adresseEnErreur.source !='CIVILE' && adresseEnErreur.id!= null && adresseEnErreur.id!='' }">
					<c:if test="${((adresseEnErreur.usage == 'COURRIER') && (command.allowedOnglet.ADR_C)) ||
						((adresseEnErreur.usage == 'POURSUITE') && (command.allowedOnglet.ADR_P)) ||
						((adresseEnErreur.usage == 'REPRESENTATION') && (command.allowedOnglet.ADR_B)) ||
						((adresseFiscale.usage == 'DOMICILE') && (command.allowedOnglet.ADR_D))}">
						<unireg:raccourciModifier link="adresse.do?height=530&width=850&idAdresse=${adresseEnErreur.id}&numero=${command.tiers.numero}&TB_iframe=true&modal=true" thickbox="true" tooltip="Edition d'adresse"/>
						<unireg:raccourciAnnuler onClick="javascript:annulerAdresse(${adresseEnErreur.id});"/>
					</c:if>
				</c:if>
			</c:if>
		</display:column>
		<display:setProperty name="paging.banner.all_items_found" value=""/>
		<display:setProperty name="paging.banner.one_item_found" value=""/>
	</display:table><br/>

	<fmt:message key="error.adresse.fiscale.correction" />
</c:if>