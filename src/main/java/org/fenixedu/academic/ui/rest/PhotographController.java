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
package org.fenixedu.academic.ui.rest;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.fenixedu.academic.domain.Photograph;
import org.fenixedu.academic.domain.photograph.PictureMode;
import org.fenixedu.bennu.core.domain.Avatar;
import org.fenixedu.bennu.core.domain.User;

@Path("/user/photo")
public class PhotographController {

    private static int MAX_PHOTO_SIZE = 512;
    private static int DEFAULT_PHOTO_SIZE = 100;

    @GET
    @Path("/{username:.+}")
    @Produces("image/*")
    public Response get(@PathParam("username") String username, @QueryParam(value = "s") Integer providedSize,
            @HeaderParam(value = "If-None-Match") String ifNoneMatch) throws IOException {
        int size = (providedSize == null
                || providedSize <= 0) ? DEFAULT_PHOTO_SIZE : (providedSize > MAX_PHOTO_SIZE ? MAX_PHOTO_SIZE : providedSize);

        User user = User.findByUsername(username);

        Optional<Photograph> personalPhoto = Optional.ofNullable(user).map(u -> u.getPerson())
                .filter(p -> p.isPhotoAvailableToCurrentUser()).map(p -> p.getPersonalPhoto());

        Response.ResponseBuilder responseBuilder;

        EntityTag entityTag =
                personalPhoto.map(p -> new EntityTag(p.getExternalId() + "-" + size, true)).orElse(new EntityTag("mm-av", true));

        if (!StringUtils.isBlank(ifNoneMatch)
                && entityTag.getValue().equals(ifNoneMatch.substring(3, ifNoneMatch.length() - 1))) {
            responseBuilder = Response.status(Response.Status.NOT_MODIFIED);
        } else {
            CacheControl cacheControl = new CacheControl();
            cacheControl.setMaxAge(1209600);

            // ChronoUnit.WEEKS was not supported so I used 14 days
            responseBuilder = Response.status(Response.Status.OK).expires(Date.from(Instant.now().plus(14, ChronoUnit.DAYS)))
                    .cacheControl(cacheControl);

            String mimeType = personalPhoto.map(p -> p.getOriginal().getPictureFileFormat().getMimeType()).orElse("image/png");

            byte[] entity = personalPhoto.map(p -> p.getCustomAvatar(size, size, PictureMode.ZOOM)).orElseGet(() -> {
                try (InputStream mysteryMan = PhotographController.class.getClassLoader()
                        .getResourceAsStream("META-INF/resources/img/mysteryman.png")) {
                    return Avatar.process(mysteryMan, mimeType, size);
                } catch (IOException e) {
                    return new byte[] {};
                }
            });

            responseBuilder.type(mimeType);
            responseBuilder.entity(entity);
            responseBuilder.tag(entityTag);
        }

        return responseBuilder.build();
    }

    @GET
    @Path(value = "{size}/{username:.+}")
    @Produces("image/*")
    public Response getWithSize(@PathParam("username") String username, @PathParam("size") Integer size,
            @HeaderParam(value = "If-None-Match") String ifNoneMatch) throws IOException {
        return get(username, size, ifNoneMatch);
    }

}
