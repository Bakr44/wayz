package com.example.wayz.Service;

import com.example.wayz.Model.MyFile;
import com.example.wayz.Model.User;
import com.example.wayz.Repository.AuthRepository;
import com.example.wayz.Repository.FileRepository;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileService {


    private final FileRepository fileRepository;
    private final AuthRepository userRepository;

    private final String SERVER_FILES_FOLDER = "C:/Users/isaud/IdeaProjects/System/src/main/resources/users_files/";

    //// record to put the file info and the file itself in one place I think it's more readable this way, plus we can return both from a function.
    public record FileInfoRecord(MediaType mediaType, byte[] data) {
    }

    public String uploadFile(MultipartFile file, Integer userId) throws IOException, RuntimeException {

        if (file.isEmpty()) throw new RuntimeException();


        ////// we'll make a directory for each user in our Server and store whatever we need to retrieve x file in the database, this way is way faster than storing the files in the db as BLOB.
        User user = userRepository.findUserById(userId);

        String fileLocation = SERVER_FILES_FOLDER + user.getId() + "/" + file.getOriginalFilename();

        Files.createDirectories(Paths.get(fileLocation));


        file.transferTo(new File(fileLocation));

        int fileSizeInMb = Math.toIntExact((file.getSize() >> 20)); //// converting bytes to mbs by bit shifting instead of dividing

        MyFile uploadFile = new MyFile();
        uploadFile.setFileName(file.getOriginalFilename());
        uploadFile.setFileType(file.getContentType());
        uploadFile.setSize(fileSizeInMb > 0 ? fileSizeInMb : 1); //// if file is less than 1mb it'll be 0 after shifting so we'll just put 1mb as an approximation in that case.
        uploadFile.setUser(user);

        fileRepository.save(uploadFile);

        return "File named " + file.getOriginalFilename() + " uploaded successfully !";
    }


    public void deleteUserFiles(Integer userId) throws IOException {

        FileUtils.deleteDirectory(new File(SERVER_FILES_FOLDER + userId));

    }

    public List<MyFile> getMyFiles(Integer userId) throws RuntimeException {

        User filesOwner = userRepository.findUserById(userId);

        List<MyFile> userFilesList = fileRepository.findAllByUser(filesOwner);

        if (userFilesList.isEmpty()) throw new RuntimeException();

        return userFilesList;
    }

    public List<MyFile> getMyFilesByType(Integer userId, String mediaType) throws RuntimeException {

        User filesOwner = userRepository.findUserById(userId);

        List<MyFile> userFilesList = fileRepository.getAllByFileTypeContainingIgnoreCaseAndUser(mediaType, filesOwner);

        if (userFilesList.isEmpty()) throw new RuntimeException();

        return userFilesList;
    }

    public FileInfoRecord downloadFileById(Integer userId, Integer fileID) throws IOException, RuntimeException {


        ///// doing this way allows us to prevent any unwanted access to any user's files since this token is generated and given everytime the user login to their account.
        User user = userRepository.findUserById(userId);


        MyFile downloadFile = fileRepository.findMyFileByIdAndUser(fileID, user);

        if (downloadFile == null) throw new RuntimeException();

        String downloadFilePath = SERVER_FILES_FOLDER + user.getId() + "/" + downloadFile.getFileName();

        byte[] file = Files.readAllBytes(new File(downloadFilePath).toPath());

        return new FileInfoRecord(MediaType.valueOf(downloadFile.getFileType()), file);

    }

    public FileInfoRecord downloadFileByName(Integer userId, String fileName) throws IOException, RuntimeException {


        User user = userRepository.findUserById(userId);

        MyFile downloadFile = fileRepository.findMyFileByFileNameAndUser(fileName, user);

        if (downloadFile == null) throw new RuntimeException();

        String downloadFilePath = SERVER_FILES_FOLDER + user.getId() + "/" + downloadFile.getFileName();

        byte[] file = Files.readAllBytes(new File(downloadFilePath).toPath());


        return new FileInfoRecord(MediaType.valueOf(downloadFile.getFileType()), file);

    }

    public List<MyFile> getMyFilesBiggerThan(Integer userId, Integer size) throws RuntimeException {

        ///// doing this way allows us to prevent any unwanted access to any user's files since this token is generated and given everytime the user login to their account.
        User filesOwner = userRepository.findUserById(userId);


        List<MyFile> userFilesList = fileRepository.findAllBySizeAfterAndUser(size, filesOwner);
        if (userFilesList.isEmpty()) throw new RuntimeException();

        return userFilesList;
    }

    public List<MyFile> getMyFilesLessThan(Integer userId, Integer size) throws RuntimeException {

        ///// doing this way allows us to prevent any unwanted access to any user's files since this token is generated and given everytime the user login to their account.
        User filesOwner = userRepository.findUserById(userId);

        List<MyFile> userFilesList = fileRepository.findAllBySizeBeforeAndUser(size, filesOwner);
        if (userFilesList.isEmpty()) throw new RuntimeException();

        return userFilesList;
    }

}
