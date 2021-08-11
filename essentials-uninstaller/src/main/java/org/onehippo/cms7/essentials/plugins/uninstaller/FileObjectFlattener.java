package org.onehippo.cms7.essentials.plugins.uninstaller;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class FileObjectFlattener {

    private FileObject fileObject;

    public FileObjectFlattener(FileObject fileObject) {
        this.fileObject = fileObject;
    }

    public List<FileObject> getChildren() {
        try {
            return fileObject.isFolder() ? Arrays.asList(fileObject.getChildren()) : Collections.emptyList();
        } catch (FileSystemException e) {
        }
        return Collections.emptyList();
    }

    public FileObject getFileObject() {
        return fileObject;
    }

    public Stream<FileObjectFlattener> flattened() {
        return Stream.concat(
                Stream.of(this),
                getChildren().stream().map(fileObject -> new FileObjectFlattener(fileObject)).flatMap(FileObjectFlattener::flattened));
    }
}
