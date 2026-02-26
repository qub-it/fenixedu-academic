package org.fenixedu.academic.service.services.person.picture;

import org.fenixedu.academic.domain.photograph.Picture;

public interface PictureService {

    public void storePictureInStorage(Picture picture);

    public byte[] getOriginalPicture(Picture picture);

    public boolean isPictureCorrectlyStoredInDisk(Picture picture);

    public void assertStorageExistance();

    public boolean doesPictureDataContain100x100();
}
