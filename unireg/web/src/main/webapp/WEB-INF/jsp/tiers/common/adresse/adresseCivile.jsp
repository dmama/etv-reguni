<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="page" value="${param.page}" />
<c:set var="membre" value="${param.membre}" />
<c:if test="${page == 'visu' }">
	<c:set var="url" value="visu.do" />
	<c:choose>
		<c:when test="${membre == 'principal'}">
			<c:set var="adresses" value="${command.historiqueAdressesCiviles}" />
		</c:when>
		<c:when test="${membre == 'conjoint'}">
			<c:set var="adresses" value="${command.historiqueAdressesCivilesConjoint}" />
		</c:when>
	</c:choose>	
</c:if>
<c:if test="${not empty adresses}">	
<display:table name="${adresses}" id="adresse" pagesize="10" requestURI="${url}" class="display" decorator="ch.vd.uniregctb.decorator.TableAdresseCivileDecorator">
		<display:column  sortable ="true" titleKey="label.utilisationAdresse">
				<fmt:message key="option.usage.civil.${adresse.usageCivil}" />
		</display:column>
		<display:column sortable ="true" titleKey="label.date.debut" sortProperty="dateDebut">
				<fmt:formatDate value="${adresse.dateDebut}" pattern="dd.MM.yyyy"/>
		</display:column>
		<display:column sortable ="true" titleKey="label.date.fin" sortProperty="dateFin">
				<fmt:formatDate value="${adresse.dateFin}" pattern="dd.MM.yyyy"/>
		</display:column>
		<display:column sortable="true" titleKey="label.adresse.complement">
			${adresse.complements}
		</display:column>
		<display:column sortable ="true" titleKey="label.rueCasePostale">
				${adresse.rue}
		</display:column>
		<display:column sortable ="true" titleKey="label.localite">
				${adresse.localite}
		</display:column>
		<display:column sortable ="true" titleKey="label.pays">
				<c:if test="${adresse.paysOFS != null }">
					<unireg:infra entityId="${adresse.paysOFS}" entityType="pays" entityPropertyName="nomMinuscule"></unireg:infra>
				</c:if>
		</display:column>
		<display:column sortable ="true" titleKey="label.adresse.permanente" >
			<c:if test="${!adresse.annule}">	
				<c:if test="${adresse.id != null}">
					<input type="checkbox" <c:if test="${adresse.permanente}">checked</c:if> disabled="disabled" />
				</c:if>
			</c:if>
		</display:column>
		<display:column sortable ="true" titleKey="label.adresse.source">
				<fmt:message key="option.source.${adresse.source}" />
				<c:if test="${adresse.default}">(<fmt:message key="option.source.default.tag" />)</c:if>			
		</display:column>
		<display:column style="action">
			<c:if test="${page == 'visu' }">
				<c:if test="${adresse.id != null}">
					<unireg:raccourciConsulter link="../common/consult-log.do?height=200&width=800&nature=AdresseTiers&id=${adresse.id}&TB_iframe=true&modal=true" thickbox="true" tooltip="Edition des logs"/>
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
				<c:if test="${adresse.id != null}">
					<unireg:raccourciConsulter link="../common/consult-log.do?height=200&width=800&nature=AdresseTiers&id=${adresse.id}&TB_iframe=true&modal=true" thickbox="true" tooltip="Edition des logs"/>
				</c:if>
			</c:if>			
		</display:column>
		<display:setProperty name="paging.banner.all_items_found" value=""/>
		<display:setProperty name="paging.banner.one_item_found" value=""/>
	</display:table><br/>

	<fmt:message key="error.adresse.fiscale.correction" />
</c:if>