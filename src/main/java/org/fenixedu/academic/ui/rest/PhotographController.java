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

    public static int MAX_PHOTO_SIZE = 1048576; //1M

    @GET
    @Path("/{username:.+}")
    @Produces("image/*")
    public Response get(@PathParam("username") String username, @QueryParam(value = "s") Integer size,
            @HeaderParam(value = "If-None-Match") String ifNoneMatch) throws IOException {

        if (size == null || size <= 0) {
            size = 100;
        }
        if (size > 512) {
            size = 512;
        }

        User user = User.findByUsername(username);

        if (user != null && user.getPerson() != null) {
            final Photograph personalPhoto =
                    user.getPerson().isPhotoAvailableToCurrentUser() ? user.getPerson().getPersonalPhoto() : null;

            if (personalPhoto != null) {
                return buildResponse(ifNoneMatch, personalPhoto.getOriginal().getPictureFileFormat().getMimeType(),
                        personalPhoto.getCustomAvatar(size, size, PictureMode.ZOOM),
                        new EntityTag(personalPhoto.getExternalId() + "-" + size, true));
            }
        }

        try (InputStream mysteryMan =
                PhotographController.class.getClassLoader().getResourceAsStream("META-INF/resources/img/mysteryman.png")) {
            String mimeType = "image/png";
            return buildResponse(ifNoneMatch, mimeType, Avatar.process(mysteryMan, mimeType, size), new EntityTag("mm-av", true));
        }
    }

    private Response buildResponse(String ifNoneMatch, String mimeType, byte[] entity, EntityTag entityTag) {
        Response.ResponseBuilder responseBuilder;

        if (!StringUtils.isBlank(ifNoneMatch)
                && entityTag.getValue().equals(ifNoneMatch.substring(3, ifNoneMatch.length() - 1))) {
            responseBuilder = Response.status(Response.Status.NOT_MODIFIED);
        } else {
            CacheControl cacheControl = new CacheControl();
            cacheControl.setMaxAge(1209600);

            // ChronoUnit.WEEKS was not supported so I used 14 days
            responseBuilder = Response.status(Response.Status.OK).expires(Date.from(Instant.now().plus(14, ChronoUnit.DAYS)))
                    .cacheControl(cacheControl);
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
