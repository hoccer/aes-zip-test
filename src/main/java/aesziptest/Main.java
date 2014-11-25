package aesziptest;

import de.idyl.winzipaes.AesZipFileDecrypter;
import de.idyl.winzipaes.AesZipFileEncrypter;
import de.idyl.winzipaes.impl.AESDecrypterBC;
import de.idyl.winzipaes.impl.AESEncrypterBC;
import de.idyl.winzipaes.impl.ExtZipEntry;

import java.io.*;
import java.util.zip.DataFormatException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


public class Main {
    public static void main(String[] args) throws IOException, DataFormatException {
        String currentDir = System.getProperty("user.dir");
        File resDir = new File(currentDir, "res");
        File backupZip = new File(resDir, "backup.zip");

        createEncryptedDataZip(resDir, new File(resDir, "data.zip"), "12345678");
        createZip(resDir, backupZip);
        extractZip(backupZip, resDir, "12345678");
    }

    private static void extractZip(File backupZip, File resDir, String password) throws IOException, DataFormatException {
        File backupDir = new File(resDir, "backup_extracted");
        if (backupDir.exists()) {
            backupDir.delete();
        }
        backupDir.mkdir();

        extractZip(backupZip, backupDir);
        extractAndDecryptZip(new File(backupDir, "data.zip"), backupDir, password);
    }

    private static void extractAndDecryptZip(File encrptedDataZip, File backupDir, String password) throws IOException, DataFormatException {
        AesZipFileDecrypter decrypter = new AesZipFileDecrypter(encrptedDataZip, new AESDecrypterBC());
        for (ExtZipEntry extZipEntry : decrypter.getEntryList()) {
            decrypter.extractEntry(extZipEntry, new File(backupDir, extZipEntry.getName().substring(extZipEntry.getName().lastIndexOf("/") + 1)), password);
        }
        decrypter.close();
        encrptedDataZip.delete();
    }

    private static void extractZip(File backupZip, File backupDir) throws IOException {
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

    private static void createZip(File resDir, File backupZip) throws IOException {

        if (backupZip.exists()) {
            backupZip.delete();
        }

        File metadata = new File(resDir, "metadata.json");
        File encryptedDataZip = new File(resDir, "data.zip");

        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(backupZip));
        zos.setLevel(ZipOutputStream.STORED);
        try {
            addZipEntry(zos, metadata);
            addZipEntry(zos, encryptedDataZip);
        } finally {
            zos.close();
        }

        encryptedDataZip.delete();
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

    public static void createEncryptedDataZip(File resDir, File encryptedDataZip, String password) throws IOException {
        if (encryptedDataZip.exists()) {
            encryptedDataZip.delete();
        }

        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isFile() && file.getName().startsWith("file");
            }
        };

        AesZipFileEncrypter encrypter = new AesZipFileEncrypter(encryptedDataZip, new AESEncrypterBC());
        for (File file : resDir.listFiles(filter)) {
            encrypter.add(file, password);
        }
        encrypter.close();
    }
}
