package aesziptest;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


public class Main {
    public static void main(String[] args) throws Exception {
        String currentDir = System.getProperty("user.dir");
        File resDir = new File(currentDir, "res");
        File backupZip = new File(resDir, "backup.zip");

        File encryptedFile = encryptFile(new File(resDir, "file"), new File(resDir, "file_encrypted"), "12345678");

        File[] files = {encryptedFile, new File(resDir, "metadata.json")};
        createZip(files, backupZip);

        File backupDir = new File(resDir, "backup_extracted");
        extractZip(backupZip, backupDir);

        decryptFile(new File(backupDir, "file_encrypted"), new File(backupDir, "file_decrypted"),"12345678");
    }

    private static void extractZip(File backupZip, File backupDir) throws IOException {
        if (backupDir.exists()) {
            backupDir.delete();
        }
        backupDir.mkdir();

        ZipInputStream zis = new ZipInputStream(new FileInputStream(backupZip));
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int count;
            while ((count = zis.read(buffer)) != -1) {
                baos.write(buffer, 0, count);
            }
            String filename = entry.getName();
            byte[] bytes = baos.toByteArray();

            File file = new File(backupDir, filename);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(bytes);
            fos.close();
        }
        zis.close();
    }

    private static void createZip(File[] files, File backupZip) throws IOException {

        if (backupZip.exists()) {
            backupZip.delete();
        }

        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(backupZip));
        zos.setLevel(ZipOutputStream.STORED);
        try {
            for (File file : files) {
                addZipEntry(zos, file);
            }
        } finally {
            zos.close();
        }
    }

    private static void addZipEntry(ZipOutputStream zos, File fileEntry) throws IOException {
        FileInputStream in = new FileInputStream(fileEntry);
        ZipEntry entry = new ZipEntry(fileEntry.getName());
        zos.putNextEntry(entry);

        byte[] buffer = new byte[1024];
        int length;
        while ((length = in.read(buffer)) > 0) {
            zos.write(buffer, 0, length);
        }
        in.close();
        zos.closeEntry();
    }

    public static File encryptFile(File file, File encryptedFile, String password) throws Exception {
        if (encryptedFile.exists()) {
            encryptedFile.delete();
        }

        FileInputStream in = new FileInputStream(file);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int count;
        while ((count = in.read(buffer)) != -1) {
            baos.write(buffer, 0, count);
        }
        in.close();
        byte[] bytes = baos.toByteArray();

        byte[] encryptedBytes = CryptoJSON.encrypt(bytes, password, "text/plain");
        FileOutputStream out = new FileOutputStream(encryptedFile);
        out.write(encryptedBytes);
        out.close();

        return encryptedFile;
    }

    private static void decryptFile(File encryptedFile, File decryptedFile, String password) throws Exception {
        FileInputStream in = new FileInputStream(encryptedFile);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int count;
        while ((count = in.read(buffer)) != -1) {
            baos.write(buffer, 0, count);
        }
        in.close();
        byte[] bytes = baos.toByteArray();

        byte[] decryptedBytes = CryptoJSON.decrypt(bytes, password, "text/plain");

        FileOutputStream out = new FileOutputStream(decryptedFile);
        out.write(decryptedBytes);
        out.close();
    }
}
