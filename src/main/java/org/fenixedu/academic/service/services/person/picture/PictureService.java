package org.fenixedu.academic.service.services.person.picture;

import org.fenixedu.academic.domain.Photograph;
import org.fenixedu.academic.domain.photograph.Picture;
import org.fenixedu.academic.domain.photograph.PictureOriginal;
import org.fenixedu.academic.util.ContentType;

public interface PictureService {

    public void storePictureInStorage(Picture picture);

    public byte[] getOriginalPicture(Picture picture);

    public boolean isPictureCorrectlyStoredInDisk(Picture picture);

    public void assertStorageExistance();

    public boolean doesPictureDataContain100x100();

    public PictureOriginal createPictureForPhotograph(Photograph photograph, ContentType contentType, byte[] originalData,
            byte[] data100x100);
}
