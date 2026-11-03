/**
 * Copyright © 2002 Instituto Superior Técnico
 *
 * This file is part of FenixEdu Academic.
 *
 * FenixEdu Academic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Academic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Academic.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.fenixedu.academic.domain.candidacy;

import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;

import com.google.common.base.Strings;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;

public class IngressionType extends IngressionType_Base {

	private IngressionType() {
		super();
		setRootDomainObject(Bennu.getInstance());
	}

	@Atomic(mode = TxMode.WRITE)
	public static IngressionType createIngressionType(String code, LocalizedString description) {

		IngressionType ingressionType = new IngressionType();
		ingressionType.setCode(code);
		ingressionType.setDescription(description);

		return ingressionType;
	}

	public String getLocalizedName() {
		return getLocalizedName(Locale.getDefault());
	}

	public String getLocalizedName(Locale locale) {
		return getCode() + " - " + getDescription().getContent(locale);
	}

	public static Collection<IngressionType> findAll() {
        return Bennu.getInstance().getIngressionTypesSet();
    }
	
	public static Optional<IngressionType> findIngressionTypeByCode(String name) {
		Predicate<? super IngressionType> matchesName = ingressionType -> name.equals(ingressionType.getCode());
		return Bennu.getInstance().getIngressionTypesSet().stream().filter(matchesName).findAny();
	}
	
	public static Stream<IngressionType> findAllByPredicate(Predicate<IngressionType> predicate) {
		return Bennu.getInstance().getIngressionTypesSet().stream().filter(predicate);
	}

	public static Optional<IngressionType> findByPredicate(Predicate<IngressionType> predicate) {
		return Bennu.getInstance().getIngressionTypesSet().stream().filter(predicate).findAny();
	}

	public void delete() {
		if (!getRegistrationSet().isEmpty()) {
			throw new DomainException("label.error.ingressionType.delete.hasRegistrationsAssociated");
		}
		setRootDomainObject(null);
		deleteDomainObject();
	}

	@Override
	public void setCode(String code) {
		
		if (Strings.isNullOrEmpty(code)) {
            throw new DomainException("error.ingressionType.code.cannot.be.null");
        }

        if (findAll().stream().anyMatch(it -> it != this && Objects.equals(it.getCode(), code))) {
            throw new DomainException("error.ingressionType.duplicated.code", code);
        }
		
		super.setCode(code);
	}
}
