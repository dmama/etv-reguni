<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="page" value="${param.page}" />
<c:if test="${page == 'visu' }">
	<c:set var="url" value="visu.do" />
	<c:set var="adresses" value="${command.historiqueAdresses}" />
</c:if>
<c:if test="${page == 'edit' }">
	<c:set var="url" value="edit.do" />
	<c:set var="adresses" value="${command.adressesFiscalesModifiables}" />
</c:if>

<c:if test="${not empty adresses}">	
<display:table name="${adresses}" id="adresse" pagesize="10" requestURI="${url}" class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
		<display:column  sortable ="true" titleKey="label.utilisationAdresse" class="usage">
			<fmt:message key="option.usage.${adresse.usage}" />
		</display:column>
		<display:column sortable ="true" titleKey="label.date.debut" sortProperty="dateDebut">
			<fmt:formatDate value="${adresse.dateDebut}" pattern="dd.MM.yyyy"/>
		</display:column>
		<display:column sortable ="true" titleKey="label.date.fin" sortProperty="dateFin">
			<fmt:formatDate value="${adresse.dateFin}" pattern="dd.MM.yyyy"/>
		</display:column>
		<display:column sortable="true" titleKey="label.adresse.complement">
			<c:out value="${adresse.complements}"/>
		</display:column>
		<display:column sortable ="true" titleKey="label.rueCasePostale">
			<c:out value="${adresse.rue}"/>
		</display:column>
		<display:column sortable ="true" titleKey="label.localite" >
			<c:out value="${adresse.localite}"/>
		</display:column>
		<display:column sortable ="true" titleKey="label.pays" >
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
			<c:if test="${adresse.default || adresse.source == 'CIVILE'}">(<fmt:message key="option.source.default.tag" />)</c:if>
		</display:column>
		<display:column style="action">
			<c:if test="${page == 'visu' }">
				<c:if test="${adresse.id != null}">
					<unireg:consulterLog entityNature="AdresseTiers" entityId="${adresse.id}"/>
				</c:if>
			</c:if>
			<c:if test="${page == 'edit' }">
				<c:if test="${!adresse.annule && adresse.source !='CIVILE' && adresse.id!= null && adresse.id!='' }">
					<c:if test="${((adresse.usage == 'COURRIER') && (command.allowedOnglet.ADR_C)) ||
					((adresse.usage == 'POURSUITE') && (command.allowedOnglet.ADR_P)) ||
					((adresse.usage == 'REPRESENTATION') && (command.allowedOnglet.ADR_B)) ||
					((adresse.usage == 'DOMICILE') && (command.allowedOnglet.ADR_D))}">
						<c:if test="${adresse.dateFin == null}">
							<unireg:raccourciModifier link="adresse-close.do?idAdresse=${adresse.id}" tooltip="Fermeture de l'adresse"/>
						</c:if>
						<unireg:raccourciAnnuler onClick="annulerAdresse(${adresse.id});" tooltip="Annulation de l'adresse"/>
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
	
	<display:table name="command.adressesEnErreur" id="adresseEnErreur" pagesize="10" class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
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
				<c:if test="${adresseEnErreur.id != null}">
					<unireg:consulterLog entityNature="AdresseTiers" entityId="${adresseEnErreur.id}"/>
				</c:if>
			</c:if>
			<c:if test="${page == 'edit' }">
				<c:if test="${!adresseEnErreur.annule && adresseEnErreur.source !='CIVILE' && adresseEnErreur.id!= null && adresseEnErreur.id!='' }">
					<c:if test="${((adresseEnErreur.usage == 'COURRIER') && (command.allowedOnglet.ADR_C)) ||
						((adresseEnErreur.usage == 'POURSUITE') && (command.allowedOnglet.ADR_P)) ||
						((adresseEnErreur.usage == 'REPRESENTATION') && (command.allowedOnglet.ADR_B)) ||
						((adresse.usage == 'DOMICILE') && (command.allowedOnglet.ADR_D))}">
						<unireg:raccourciModifier link="../adresses/adresse-close.do?idAdresse=${adresseEnErreur.id}" tooltip="Fermeture de l'adresse"/>
						<unireg:raccourciAnnuler onClick="annulerAdresse(${adresseEnErreur.id});" tooltip="Annulation de l'adresse"/>
					</c:if>
				</c:if>
			</c:if>
		</display:column>
		<display:setProperty name="paging.banner.all_items_found" value=""/>
		<display:setProperty name="paging.banner.one_item_found" value=""/>
	</display:table><br/>

	<fmt:message key="error.adresse.fiscale.correction" />
</c:if>
