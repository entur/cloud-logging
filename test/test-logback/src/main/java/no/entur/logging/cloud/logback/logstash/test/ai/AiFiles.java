package no.entur.logging.cloud.logback.logstash.test.ai;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The AI files hold whole request and response bodies and are often in a shared temporary directory, so they are created readable by
 * the owner only (where the file system has POSIX permissions), and a file that is a symbolic link or belongs to another user is refused.
 */
public final class AiFiles {

    private static final Set<PosixFilePermission> OWNER_ONLY = EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);

    private AiFiles() {
    }

    /** See {@link #createOwnerOnly(Path, Predicate)}. */
    public static void createOwnerOnly(Path path) throws IOException {
        createOwnerOnly(path, AiFiles::isOwnedByThisUser);
    }

    /**
     * Creates the file readable by the owner only, or makes an existing file of this user so.
     *
     * @throws IOException if the file is a symbolic link, belongs to another user (it could be readable by others) or cannot be
     *                     created (then with a cause)
     */
    static void createOwnerOnly(Path path, Predicate<Path> ownedByThisUser) throws IOException {
        boolean posix = path.getFileSystem().supportedFileAttributeViews().contains("posix");
        try {
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            if (posix) {
                Files.createFile(path, PosixFilePermissions.asFileAttribute(OWNER_ONLY));
            } else {
                Files.createFile(path);
            }
            return;
        } catch (FileAlreadyExistsException e) {
            // also if it is a symbolic link, even one to a missing file
        } catch (IOException | RuntimeException e) {
            throw new IOException("Unable to create " + path, e);
        }
        if (Files.isSymbolicLink(path)) {
            throw new IOException("Not writing to " + path + ": it is a symbolic link");
        }
        if (posix && !ownedByThisUser.test(path)) {
            throw new IOException("Not writing to " + path + ": it belongs to another user");
        }
        if (posix && Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            try {
                Files.setPosixFilePermissions(path, OWNER_ONLY);
            } catch (IOException | RuntimeException e) {
                // not the owner
            }
        }
    }

    /** True if the file belongs to the user running this JVM, or if that cannot be told. */
    static boolean isOwnedByThisUser(Path path) {
        try {
            UserPrincipal owner = Files.getOwner(path, LinkOption.NOFOLLOW_LINKS);
            UserPrincipal user = path.getFileSystem().getUserPrincipalLookupService().lookupPrincipalByName(System.getProperty("user.name"));
            return owner.equals(user);
        } catch (IOException | RuntimeException e) {
            return true;
        }
    }
}
