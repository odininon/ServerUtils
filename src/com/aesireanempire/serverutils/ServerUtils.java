package com.aesireanempire.serverutils;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ServerUtils
{

    public static void main(String[] args)
    {
        if (args[0].equals("-d"))
        {
            if (args.length != 3) { throw new RuntimeException(""); }

            try
            {
                createDiff(args[1], args[2]);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        if (args[0].equals("-x"))
        {
            if (args.length != 2) { throw new RuntimeException(""); }

            try
            {
                executeDiff(args[1]);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    private static void executeDiff(String zipName) throws IOException
    {
        ZipFile zipFile = null;

        try
        {
            zipFile = new ZipFile(zipName);

            ZipEntry entry = zipFile.getEntry("diff.txt");

            InputStream inputStream = zipFile.getInputStream(entry);

            readDiff(zipFile, inputStream);
            inputStream.close();
        }
        finally
        {
            if (zipFile != null) zipFile.close();
        }
    }

    private static void readDiff(ZipFile zipFile, InputStream inputStream) throws IOException
    {
        Scanner scanner = new Scanner(inputStream);

        while (scanner.hasNextLine())
        {
            String line = scanner.nextLine();
            if (line.startsWith("add"))
            {
                String fileName = line.substring(line.indexOf(":") + 2);
                ZipEntry entry = zipFile.getEntry(fileName);

                InputStream stream = zipFile.getInputStream(entry);

                writeFile(fileName, stream);
                stream.close();
            }

            if (line.startsWith("remove"))
            {
                String fileName = line.substring(line.indexOf(":") + 2);

                Files.delete(Paths.get(fileName));
            }

            if (line.startsWith("update"))
            {
                String fileName = line.substring(line.indexOf(":") + 2);

                String fileName1 = fileName.substring(0, fileName.length() / 2);
                String fileName2 = fileName.substring((fileName.length() + 1) / 2, fileName.length());

                try
                {
                    Files.delete(Paths.get(fileName1));
                }
                catch (IOException ignored)
                {

                }

                ZipEntry entry = zipFile.getEntry(fileName2);

                InputStream stream = zipFile.getInputStream(entry);

                writeFile(fileName2, stream);
                stream.close();
            }
        }
    }

    private static void writeFile(String fileName, InputStream stream) throws IOException, FileNotFoundException
    {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];

        for (int readNum; (readNum = stream.read(buf)) != -1;)
        {
            byteArrayOutputStream.write(buf, 0, readNum);
        }

        byte[] byteArray = byteArrayOutputStream.toByteArray();

        File file = new File(fileName);

        String substring = fileName.substring(0, fileName.lastIndexOf("/"));

        Files.createDirectories(Paths.get(substring));

        FileOutputStream fileOutputStream = new FileOutputStream(file);
        fileOutputStream.write(byteArray);
        fileOutputStream.flush();
        fileOutputStream.close();
    }

    private static void createDiff(String oldZip, String newZip) throws IOException
    {
        ZipFile zipFile1 = null;
        ZipFile zipFile2 = null;
        try
        {
            zipFile1 = new ZipFile(oldZip);
            zipFile2 = new ZipFile(newZip);

            List<ModFile> contents1 = getContents(zipFile1);
            List<ModFile> contents2 = getContents(zipFile2);

            String contents = compareContents(contents1, contents2);

            File file = new File("diff.txt");

            Files.deleteIfExists(file.toPath());

            BufferedWriter writer = new BufferedWriter(new FileWriter(file));

            writer.write(contents);
            writer.close();

        }
        finally
        {
            if (zipFile1 != null) zipFile1.close();
            if (zipFile2 != null) zipFile2.close();
        }
    }

    private static String compareContents(List<ModFile> oldContents, List<ModFile> newContents)
    {
        StringBuilder stringBuilder = new StringBuilder();

        for (ModFile file : newContents)
        {
            if (!includesFile(file, oldContents))
            {
                stringBuilder.append("add: " + file.getFileName());
                stringBuilder.append("\n");
            }
            else
            {
                ModFile oldFile = getOldFile(file, oldContents);

                stringBuilder.append("update: " + oldFile.getFileName() + " " + file.getFileName());
                stringBuilder.append("\n");
            }
        }

        for (ModFile file : oldContents)
        {
            if (!includesFile(file, newContents))
            {
                stringBuilder.append("remove: " + file.getFileName());
                stringBuilder.append("\n");
            }
        }

        return stringBuilder.toString();
    }

    private static ModFile getOldFile(ModFile file, List<ModFile> contents)
    {
        for (ModFile modFile : contents)
        {
            if (modFile.getFileName().equals(file.getFileName())) { return modFile; }

            if (!file.getModID().isEmpty())
            {
                if (modFile.getModID().equals(file.getModID())) { return modFile; }
            }

        }

        return null;
    }

    private static boolean includesFile(ModFile file, List<ModFile> contents)
    {
        ModFile oldFile = getOldFile(file, contents);
        return oldFile != null;
    }

    private static List<ModFile> getContents(ZipFile zip) throws IOException
    {
        ArrayList<ModFile> names = new ArrayList<ModFile>();
        Enumeration<? extends ZipEntry> entries = zip.entries();

        while (entries.hasMoreElements())
        {
            ZipEntry nextElement = entries.nextElement();
            if (!nextElement.isDirectory())
            {
                names.add(new ModFile(zip, nextElement));
            }
        }

        return names;
    }
}
