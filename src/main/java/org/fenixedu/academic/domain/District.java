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
package org.fenixedu.academic.domain;

import java.util.Optional;

import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.bennu.core.domain.Bennu;

//TODO: Refactor remaining object to use district instead of strings
public class District extends District_Base {

    private District() {
        super();
        setRootDomainObject(Bennu.getInstance());
    }

    public District(final String code, final String name) {
        this();
        init(code, name);
    }

    private void init(final String code, final String name) {
        checkParameters(code, name);

        super.setCode(code);
        super.setName(name);
    }

    private void checkParameters(final String code, final String name) {
        if (code == null) {
            throw new DomainException("error.org.fenixedu.academic.domain.District.code.cannot.be.null");
        }

        if (name == null) {
            throw new DomainException("error.org.fenixedu.academic.domain.District.name.cannot.be.null");
        }
    }

    public DistrictSubdivision getDistrictSubdivisionByName(final String name) {
        return getDistrictSubdivisionsSet().stream().filter(ds -> ds.getName().equals(name)).findFirst().orElse(null);
    }

    @Deprecated
    static public District readByCode(final String code) {
        return Bennu.getInstance().getDistrictsSet().stream().filter(district -> district.getCode().equals(code)).findFirst()
                .orElse(null);
    }

    @Deprecated
    static public District readByName(final String name) {
        return Bennu.getInstance().getDistrictsSet().stream().filter(district -> district.getName().equals(name)).findFirst()
                .orElse(null);
    }

    public static Optional<District> findByCode(final String code) {
        return Optional.ofNullable(code).flatMap(
                c -> Bennu.getInstance().getDistrictsSet().stream().filter(district -> c.equals(district.getCode())).findFirst());
    }

    public static Optional<District> findByName(final String name) {
        return Optional.ofNullable(name).flatMap(
                n -> Bennu.getInstance().getDistrictsSet().stream().filter(district -> n.equals(district.getName())).findFirst());
    }
}
