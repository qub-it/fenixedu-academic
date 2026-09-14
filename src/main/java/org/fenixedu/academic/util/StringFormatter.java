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
package org.fenixedu.academic.util;

import java.text.Collator;
import java.util.Comparator;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.commons.StringNormalizer;

/**
 * 
 * @author Carla Penedo (carla.penedo@ist.utl.pt)
 * @author Luis Cruz
 * @author Shezad Anavarali (shezad@ist.utl.pt)
 * 
 */
public class StringFormatter {

    public static final Comparator<String> NAME_COMPARATOR =
            Comparator.comparing(name -> name.replaceAll("\\s+", "_").toUpperCase(), Collator.getInstance());

    public static String normalize(String string) {
        String result = null;

        if (StringUtils.isNotBlank(string)) {
            String spacesReplacedString = removeDuplicateSpaces(string.trim());
            result = StringNormalizer.normalize(spacesReplacedString);
        }

        return result;
    }

    protected static String removeDuplicateSpaces(String string) {
        return string.replaceAll("\\s+", " ");
    }

}
